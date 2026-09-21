package com.example.util

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

data class GitHubRelease(
    val version: String,
    val name: String,
    val apkUrl: String,
    val htmlUrl: String
)

sealed interface UpdateCheckResult {
    data class Available(val release: GitHubRelease) : UpdateCheckResult
    data object UpToDate : UpdateCheckResult
    data class Failed(val message: String) : UpdateCheckResult
}

sealed interface InstallLaunchResult {
    data object InstallerOpened : InstallLaunchResult
    data class PermissionRequired(val intent: Intent) : InstallLaunchResult
}

object GitHubUpdateManager {
    private const val REPOSITORY = BuildConfig.GITHUB_REPOSITORY
    private const val API_URL = "https://api.github.com/repos/$REPOSITORY/releases/latest"

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun checkForUpdate(): UpdateCheckResult = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(API_URL)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "Vocab-Android/${BuildConfig.VERSION_NAME}")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.code == 404) return@withContext UpdateCheckResult.UpToDate
                if (!response.isSuccessful) error("GitHub 回應 ${response.code}")
                val root = JSONObject(response.body?.string().orEmpty())
                val version = root.getString("tag_name").removePrefix("v")
                val assets = root.getJSONArray("assets")
                var apkUrl = ""
                for (index in 0 until assets.length()) {
                    val asset = assets.getJSONObject(index)
                    if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.getString("browser_download_url")
                        break
                    }
                }
                if (apkUrl.isBlank()) error("最新版本沒有可安裝的 APK")
                val release = GitHubRelease(
                    version = version,
                    name = root.optString("name", "Vocab v$version"),
                    apkUrl = apkUrl,
                    htmlUrl = root.optString("html_url")
                )
                if (isNewer(version, BuildConfig.VERSION_NAME)) {
                    UpdateCheckResult.Available(release)
                } else {
                    UpdateCheckResult.UpToDate
                }
            }
        }.getOrElse { UpdateCheckResult.Failed(it.message ?: "無法檢查更新") }
    }

    suspend fun downloadApk(
        context: Context,
        release: GitHubRelease,
        wifiOnly: Boolean,
        onProgress: (Int) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            if (wifiOnly && !isOnWifi(context)) error("目前不是 Wi-Fi 連線")
            val request = Request.Builder()
                .url(release.apkUrl)
                .header("User-Agent", "Vocab-Android/${BuildConfig.VERSION_NAME}")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("下載失敗（${response.code}）")
                val body = response.body ?: error("下載內容是空的")
                val updateDir = File(context.cacheDir, "updates").apply { mkdirs() }
                val target = File(updateDir, "Vocab-${release.version}.apk")
                val total = body.contentLength()
                body.byteStream().use { input ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var copied = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            copied += read
                            if (total > 0) onProgress(((copied * 100) / total).toInt())
                        }
                    }
                }
                if (target.length() == 0L) error("下載的 APK 無效")
                target
            }
        }
    }

    fun launchInstaller(context: Context, apk: File): InstallLaunchResult {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            return InstallLaunchResult.PermissionRequired(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                )
            )
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apk
        )
        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
        return InstallLaunchResult.InstallerOpened
    }

    private fun isOnWifi(context: Context): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun isNewer(remote: String, local: String): Boolean {
        val remoteParts = remote.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
        val localParts = local.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
        val size = maxOf(remoteParts.size, localParts.size)
        for (index in 0 until size) {
            val remotePart = remoteParts.getOrElse(index) { 0 }
            val localPart = localParts.getOrElse(index) { 0 }
            if (remotePart != localPart) return remotePart > localPart
        }
        return false
    }
}
