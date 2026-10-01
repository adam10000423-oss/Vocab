package com.example.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import com.example.data.settings.AppSettings
import com.example.data.settings.ReminderTime
import com.example.data.api.AiProvider
import com.example.data.api.AiApiProfile
import com.example.util.TtsVoiceOption
import com.example.util.GitHubRelease
import com.example.util.GitHubUpdateManager
import com.example.util.InstallLaunchResult
import com.example.util.UpdateCheckResult
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onBooleanChange: (String, Boolean) -> Unit,
    onIntChange: (String, Int) -> Unit,
    onReminderTimesChange: (List<ReminderTime>) -> Unit,
    onThemeChange: (String) -> Unit,
    onSpeechRateChange: (Float) -> Unit,
    onDictionarySourceChange: (String) -> Unit,
    ttsVoices: List<TtsVoiceOption>,
    onTtsVoiceStyleChange: (String) -> Unit,
    onTtsVoiceNameChange: (String) -> Unit,
    onPreviewTtsVoice: () -> Unit,
    dataMessage: String?,
    apiKeyConfigured: Boolean,
    aiApiProfiles: List<AiApiProfile>,
    availableAiModels: List<String>,
    aiConnectionStatus: String?,
    onAiProviderChange: (String) -> Unit,
    onAiModelChange: (String) -> Unit,
    onAiChatStyleChange: (String) -> Unit,
    onAiPromptChange: (String) -> Unit,
    onResetAiPrompt: () -> Unit,
    onAiImagePromptChange: (String) -> Unit,
    onResetAiImagePrompt: () -> Unit,
    onSaveApiKey: (String) -> Unit,
    onClearApiKey: (String) -> Unit,
    onRefreshAiModels: () -> Unit,
    onTestAiConnection: (String?) -> Unit,
    onExportBackup: (Uri) -> Unit,
    onExportCsv: (Uri) -> Unit,
    onImportBackup: (Uri) -> Unit,
    onClearAllData: () -> Unit,
    onBack: () -> Unit
) {
    var settingsPage by rememberSaveable { mutableStateOf("設定") }
    androidx.activity.compose.BackHandler(enabled = settingsPage != "設定") { settingsPage = "設定" }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var updateStatus by remember { mutableStateOf("尚未檢查更新") }
    var availableRelease by remember { mutableStateOf<GitHubRelease?>(null) }
    var downloadedApk by remember { mutableStateOf<File?>(null) }
    var updateBusy by remember { mutableStateOf(false) }
    var apiKeyInput by remember(settings.aiProvider) { mutableStateOf("") }
    var customModelInput by remember(settings.aiProvider) { mutableStateOf(false) }
    var showAiTutorial by remember { mutableStateOf(false) }
    var promptInput by remember(settings.aiWordPrompt) { mutableStateOf(settings.aiWordPrompt) }
    var imagePromptInput by remember(settings.aiImagePrompt) {
        mutableStateOf(settings.aiImagePrompt)
    }
    var showClearDataDialog by remember { mutableStateOf(false) }
    val installPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        downloadedApk?.takeIf(File::exists)?.let { apk ->
            when (val launch = GitHubUpdateManager.launchInstaller(context, apk)) {
                InstallLaunchResult.InstallerOpened -> updateStatus = "已開啟系統安裝畫面"
                is InstallLaunchResult.PermissionRequired -> updateStatus = "請允許安裝此來源後再按一次安裝"
                is InstallLaunchResult.Failed -> updateStatus = "安裝失敗：${launch.message}"
            }
        }
    }
    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { it?.let(onExportBackup) }
    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { it?.let(onExportCsv) }
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { it?.let(onImportBackup) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> onBooleanChange("remindersEnabled", granted) }
    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val granted = AndroidSettings.canDrawOverlays(context)
        onBooleanChange("ttsFloatingOverlay", granted)
        if (granted) {
            onBooleanChange("ttsContinueInBackground", true)
        } else {
            Toast.makeText(context, "未取得浮動視窗權限，仍可單獨使用背景朗讀", Toast.LENGTH_LONG).show()
        }
    }

    fun openReminderPicker(index: Int? = null) {
        val current = index?.let { settings.reminderTimes.getOrNull(it) }
        TimePickerDialog(
            context,
            { _, hour, minute ->
                val picked = ReminderTime(hour, minute)
                val updated = if (index == null) {
                    settings.reminderTimes + picked
                } else {
                    settings.reminderTimes.mapIndexed { itemIndex, time ->
                        if (itemIndex == index) picked else time
                    }
                }
                onReminderTimesChange(updated.distinct().sorted())
            },
            current?.hour ?: 20,
            current?.minute ?: 0,
            true
        ).show()
    }

    fun checkForUpdates() {
        if (updateBusy) return
        updateBusy = true
        updateStatus = "正在檢查 GitHub 最新版本…"
        coroutineScope.launch {
            when (val result = GitHubUpdateManager.checkForUpdate()) {
                is UpdateCheckResult.Available -> {
                    availableRelease = result.release
                    downloadedApk = GitHubUpdateManager.findDownloadedApk(context, result.release.version)
                    updateStatus = if (downloadedApk != null) {
                        "Vocab ${result.release.version} 已下載，可直接再次安裝"
                    } else {
                        "發現 Vocab ${result.release.version}"
                    }
                }
                UpdateCheckResult.UpToDate -> {
                    availableRelease = null
                    downloadedApk = null
                    GitHubUpdateManager.cleanupInstalledDownloads(context)
                    updateStatus = "目前已是最新版本"
                }
                is UpdateCheckResult.Failed -> {
                    availableRelease = null
                    updateStatus = result.message
                }
            }
            updateBusy = false
        }
    }

    fun openInstaller(apk: File) {
        when (val launch = GitHubUpdateManager.launchInstaller(context, apk)) {
            InstallLaunchResult.InstallerOpened -> updateStatus = "已開啟系統安裝畫面；若未完成，可回來再次安裝"
            is InstallLaunchResult.PermissionRequired -> {
                updateStatus = "請先允許 Vocab 安裝更新"
                installPermissionLauncher.launch(launch.intent)
            }
            is InstallLaunchResult.Failed -> updateStatus = "安裝失敗：${launch.message}"
        }
    }

    fun downloadAndInstall(release: GitHubRelease, forceDownload: Boolean = false) {
        if (updateBusy) return
        updateBusy = true
        updateStatus = if (forceDownload) {
            "正在重新下載 Vocab ${release.version}…"
        } else {
            "準備下載 Vocab ${release.version}…"
        }
        coroutineScope.launch {
            GitHubUpdateManager.downloadApk(
                context = context,
                release = release,
                wifiOnly = settings.wifiOnlyUpdates,
                forceDownload = forceDownload,
                onProgress = { progress -> updateStatus = "正在下載… $progress%" }
            ).onSuccess { apk ->
                downloadedApk = apk
                openInstaller(apk)
            }.onFailure {
                updateStatus = it.message ?: "下載更新失敗"
            }
            updateBusy = false
        }
    }

    LaunchedEffect(settings.autoCheckUpdates) {
        if (settings.autoCheckUpdates && updateStatus == "尚未檢查更新") checkForUpdates()
    }

    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("清空所有資料？") },
            text = { Text("會刪除所有單字卡、資料夾、學習紀錄、設定及本機 API Key，並回到首次設定。此動作無法復原。") },
            confirmButton = {
                TextButton(onClick = {
                    showClearDataDialog = false
                    onClearAllData()
                }) { Text("確定全部清空", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) { Text("取消") }
            }
        )
    }

    if (showAiTutorial) {
        AlertDialog(
            onDismissRequest = { showAiTutorial = false },
            title = { Text("AI 連線教學", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text("Google Gemini", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("1. 登入 Google AI Studio。\n2. 建立或選擇專案並產生 API Key。\n3. 回到 Vocab，選擇 Google Gemini、貼上 Key、選模型後儲存。\n4. 在該 Key 卡片按「測試這組」。")
                        OutlinedButton(
                            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://ai.google.dev/gemini-api/docs/api-key"))) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, null)
                            Text("Gemini 官方教學")
                        }
                        Button(
                            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("前往取得 Gemini API Key") }
                    }
                    item {
                        Text("OpenRouter", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("1. 登入 OpenRouter。\n2. 到 API Keys 建立 Key。\n3. 回到 Vocab，選擇 OpenRouter、貼上 Key。\n4. 更新模型清單並選擇模型，再測試該 Key。")
                        OutlinedButton(
                            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://openrouter.ai/docs/quickstart"))) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, null)
                            Text("OpenRouter 官方教學")
                        }
                        Button(
                            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://openrouter.ai/settings/keys"))) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("前往取得 OpenRouter API Key") }
                    }
                    item {
                        Text(
                            "安全提醒：API Key 只貼在 Vocab 的設定欄位，不要傳到聊天室、截圖、GitHub 或分享網址。",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAiTutorial = false }) { Text("完成") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                expandedHeight = 48.dp,
                title = { Text(settingsPage, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { if (settingsPage == "設定") onBack() else settingsPage = "設定" }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(top = 6.dp).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (settingsPage == "設定") {
                items(listOf(
                    "外觀" to "跟隨系統、亮色與暗色",
                    "學習" to "每日目標與複習",
                    "AI" to "連線、模型與補齊",
                    "掃描與文件" to "教材辨識與匯入",
                    "單字查詢" to "辭典與翻譯來源",
                    "發音與提醒" to "朗讀、自動播放與通知",
                    "更新" to "版本與下載",
                    "資料" to "備份、匯出與還原"
                )) { (title, subtitle) ->
                    Surface(
                        onClick = { settingsPage = title },
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(title, style = MaterialTheme.typography.titleMedium)
                                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("›", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            if (settingsPage == "外觀") item {
                SettingsSection("主題") {
                    SettingDropdown(
                        label = "顯示模式",
                        value = settings.themeMode,
                        options = listOf("SYSTEM", "LIGHT", "DARK"),
                        optionText = { when (it) { "LIGHT" -> "亮色"; "DARK" -> "暗色"; else -> "跟隨系統" } },
                        onSelected = onThemeChange
                    )
                }
            }
            if (settingsPage == "學習") item {
                SettingsSection("學習") {
                    NumberSetting("每日學習目標", settings.dailyGoalCards, 5, 5..100, suffix = " 張") {
                        onIntChange("dailyGoalCards", it)
                    }
                    NumberSetting("每日新卡上限", settings.dailyNewCardLimit, 5, 1..100) {
                        onIntChange("dailyNewCardLimit", it)
                    }
                    NumberSetting("每日複習上限", settings.dailyReviewLimit, 10, 10..500) {
                        onIntChange("dailyReviewLimit", it)
                    }
                    SettingSwitch("測驗錯題加入複習", settings.gameMistakesToReview) {
                        onBooleanChange("gameMistakesToReview", it)
                    }
                }
            }
            if (settingsPage == "AI") item {
                SettingsSection("AI 補齊") {
                    SettingSwitch("啟用 AI 補齊", settings.aiEnabled) { onBooleanChange("aiEnabled", it) }
                    SettingSwitch("AI 結果套用前確認", settings.aiRequiresConfirmation) {
                        onBooleanChange("aiRequiresConfirmation", it)
                    }
                    SettingSwitch("使用我自己的 API Key", settings.usePersonalAiApi) {
                        onBooleanChange("usePersonalAiApi", it)
                    }
                    Text(
                        if (settings.usePersonalAiApi) {
                            "${AiProvider.from(settings.aiProvider).displayName} 直連"
                        } else {
                            "Vocab Gateway（未部署時無法使用）"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = promptInput,
                        onValueChange = { promptInput = it.take(4_000) },
                        label = { Text("自訂 Prompt") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onAiPromptChange(promptInput) },
                            enabled = promptInput.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("儲存 Prompt")
                        }
                        TextButton(
                            onClick = {
                                promptInput = com.example.data.api.AiPromptDefaults.WORD_DETAILS
                                onResetAiPrompt()
                            }
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null)
                            Text("重設")
                        }
                    }
                }
            }
            if (settingsPage == "AI") item {
                SettingsSection("AI 連線") {
                    val provider = AiProvider.from(settings.aiProvider)
                    val selectedProviderConfigured =
                        aiApiProfiles.any { it.provider == provider }
                    SettingDropdown(
                        label = "AI 供應商",
                        value = provider.name,
                        options = listOf(
                            AiProvider.GEMINI,
                            AiProvider.OPENROUTER,
                            AiProvider.OPENAI,
                            AiProvider.ANTHROPIC
                        ).map { it.name },
                        optionText = { AiProvider.from(it).displayName },
                        onSelected = {
                            customModelInput = false
                            onAiProviderChange(it)
                        }
                    )
                    OutlinedButton(
                        onClick = { showAiTutorial = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("查看 AI 連線教學") }
                    Button(
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(provider.keyUrl))
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                        Text("取得 API Key")
                    }
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text(if (selectedProviderConfigured) "貼上新的 API Key" else "貼上 API Key") },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onSaveApiKey(apiKeyInput) },
                            enabled = apiKeyInput.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) { Text(if (selectedProviderConfigured) "加入另一把 Key" else "加入 Key") }
                    }
                    val modelOptions = remember(provider, availableAiModels) {
                        (provider.recommendedModels + availableAiModels)
                            .filter(String::isNotBlank)
                            .distinct()
                    }
                    SettingDropdown(
                        label = "AI 模型",
                        value = settings.aiModel,
                        options = modelOptions + CUSTOM_MODEL_OPTION,
                        optionText = {
                            if (it == CUSTOM_MODEL_OPTION) "自訂模型 ID…" else it
                        },
                        onSelected = {
                            if (it == CUSTOM_MODEL_OPTION) {
                                customModelInput = true
                            } else {
                                customModelInput = false
                                onAiModelChange(it)
                            }
                        }
                    )
                    SettingDropdown(
                        label = "聊天風格",
                        value = settings.aiChatStyle,
                        options = listOf("NORMAL", "RELAXED", "STRICT"),
                        optionText = {
                            when (it) {
                                "RELAXED" -> "輕鬆"
                                "STRICT" -> "嚴謹"
                                else -> "一般"
                            }
                        },
                        onSelected = onAiChatStyleChange
                    )
                    Text(
                        when (settings.aiChatStyle) {
                            "RELAXED" -> "語氣自然輕鬆，也能聊英文學習以外的日常話題。"
                            "STRICT" -> "只處理英文學習、單字庫與相關知識；不相關話題會禮貌拒絕，回答也會更審慎。"
                            else -> "一般助理語氣，兼顧清楚、自然與實用性。"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (customModelInput || settings.aiModel !in modelOptions) {
                        OutlinedTextField(
                            value = settings.aiModel,
                            onValueChange = onAiModelChange,
                            label = { Text("自訂模型 ID") },
                            supportingText = { Text("僅在清單沒有需要的模型時使用") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onRefreshAiModels, enabled = selectedProviderConfigured) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Text("更新模型")
                        }
                        Button(onClick = { onTestAiConnection(null) }, enabled = selectedProviderConfigured && settings.aiModel.isNotBlank()) {
                            Text("測試連線")
                        }
                    }
                    aiConnectionStatus?.let {
                        Text(it, color = MaterialTheme.colorScheme.primary)
                    }
                    if (aiApiProfiles.isNotEmpty()) {
                        Text("已儲存的 API Key", fontWeight = FontWeight.Bold)
                        aiApiProfiles.forEachIndexed { index, profile ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "${index + 1}. ${profile.provider.displayName}",
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(profile.model, style = MaterialTheme.typography.bodySmall)
                                        }
                                        IconButton(onClick = { onClearApiKey(profile.credentialId) }) {
                                            Icon(Icons.Default.DeleteForever, contentDescription = "移除此 API Key")
                                        }
                                    }
                                    OutlinedTextField(
                                        value = profile.maskedKey,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("API Key（已加密）") },
                                        visualTransformation = PasswordVisualTransformation(),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Button(
                                        onClick = { onTestAiConnection(profile.credentialId) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("測試這組 ${profile.provider.displayName} API")
                                    }
                                }
                            }
                        }
                    }
                    Text(
                        "同一供應商的 Key 會輪流使用；全部無法使用時才切換下一個供應商。Key 只會加密保存在此裝置，不會放進備份。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (settingsPage == "掃描與文件") item {
                SettingsSection("掃描與文件") {
                    Text(
                        "拍照、相片與掃描頁面會直接送到目前選擇的多模態 AI 模型。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = imagePromptInput,
                        onValueChange = { imagePromptInput = it.take(4_000) },
                        label = { Text("圖片辨識 Prompt") },
                        minLines = 5,
                        maxLines = 10,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onAiImagePromptChange(imagePromptInput) },
                            enabled = imagePromptInput.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("儲存 Prompt")
                        }
                        TextButton(
                            onClick = {
                                imagePromptInput =
                                    com.example.data.api.AiPromptDefaults.IMAGE_VOCABULARY_EXTRACTION
                                onResetAiImagePrompt()
                            }
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null)
                            Text("重設")
                        }
                    }
                    SettingSwitch("匯入前預覽", settings.ocrPreviewBeforeImport) {
                        onBooleanChange("ocrPreviewBeforeImport", it)
                    }
                    NumberSetting("PDF 頁數上限", settings.pdfPageLimit, 1, 1..50) {
                        onIntChange("pdfPageLimit", it)
                    }
                }
            }
            if (settingsPage == "單字查詢") item {
                SettingsSection("單字查詢") {
                    SettingDropdown(
                        label = "線上字典來源",
                        value = settings.dictionarySource,
                        options = listOf("AUTO", "GOOGLE", "CAMBRIDGE", "WIKTIONARY", "FREE"),
                        optionText = {
                            when (it) {
                                "GOOGLE" -> "Google 翻譯"
                                "CAMBRIDGE" -> "劍橋英漢辭典"
                                "WIKTIONARY" -> "維基詞典"
                                "FREE" -> "Free Dictionary（英文解釋）"
                                else -> "自動選擇（依序切換可用來源）"
                            }
                        },
                        onSelected = onDictionarySourceChange
                    )
                    Text(
                        "自動模式遇到 429、逾時或查無結果時，會依序嘗試劍橋、維基詞典、Free Dictionary 與備用翻譯；中文查詢會優先翻成英文。網頁來源若改版，可能暫時無法解析。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (settingsPage == "發音與提醒") item {
                SettingsSection("發音與提醒") {
                    SettingSwitch("翻卡後自動朗讀", settings.autoSpeak) { onBooleanChange("autoSpeak", it) }
                    SettingSwitch("離開 App 後繼續自動朗讀", settings.ttsContinueInBackground) {
                        onBooleanChange("ttsContinueInBackground", it)
                        if (!it) onBooleanChange("ttsFloatingOverlay", false)
                    }
                    SettingSwitch("背景朗讀時顯示浮動視窗", settings.ttsFloatingOverlay) { enabled ->
                        if (!enabled) {
                            onBooleanChange("ttsFloatingOverlay", false)
                        } else if (AndroidSettings.canDrawOverlays(context)) {
                            onBooleanChange("ttsContinueInBackground", true)
                            onBooleanChange("ttsFloatingOverlay", true)
                        } else {
                            overlayPermissionLauncher.launch(
                                Intent(
                                    AndroidSettings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                            )
                        }
                    }
                    Text(
                        "背景朗讀會以低干擾通知維持播放；浮動視窗只在離開 Vocab 時出現，正面顯示英文、背面顯示中文。首次啟用需授予顯示在其他應用程式上層的權限。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SettingDropdown(
                        label = "聲音風格",
                        value = settings.ttsVoiceStyle,
                        options = listOf("NATURAL", "FEMALE", "MALE"),
                        optionText = {
                            when (it) {
                                "FEMALE" -> "女聲風格（較明亮）"
                                "MALE" -> "男聲風格（較低沉）"
                                else -> "自然原聲"
                            }
                        },
                        onSelected = onTtsVoiceStyleChange
                    )
                    val voiceNames = remember(ttsVoices) {
                        listOf(AUTO_TTS_VOICE) + ttsVoices.map { it.name }
                    }
                    SettingDropdown(
                        label = "英文語音",
                        value = settings.ttsVoiceName.ifBlank { AUTO_TTS_VOICE },
                        options = voiceNames,
                        optionText = { name ->
                            if (name == AUTO_TTS_VOICE) {
                                "自動選擇最佳英文語音"
                            } else {
                                ttsVoices.firstOrNull { it.name == name }?.let {
                                    "${it.label}（${it.name}）"
                                } ?: name
                            }
                        },
                        onSelected = {
                            onTtsVoiceNameChange(if (it == AUTO_TTS_VOICE) "" else it)
                        }
                    )
                    Text(
                        "實際可用聲音由手機的語音引擎決定。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("學習卡正面自動朗讀", fontWeight = FontWeight.Bold)
                    Text(
                        "自動播放會等正面內容全部念完才翻到背面。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SettingSwitch("英文單字", settings.ttsFrontReadWord) { onBooleanChange("ttsFrontReadWord", it) }
                    SettingSwitch("拼音（逐字母拼讀）", settings.ttsFrontSpellWord) { onBooleanChange("ttsFrontSpellWord", it) }
                    SettingSwitch("詞性", settings.ttsFrontReadPartOfSpeech) { onBooleanChange("ttsFrontReadPartOfSpeech", it) }
                    NumberSetting("正面朗讀次數", settings.ttsFrontRepetitions, 1, 1..3) {
                        onIntChange("ttsFrontRepetitions", it)
                    }
                    Text("學習卡背面自動朗讀", fontWeight = FontWeight.Bold)
                    Text(
                        "需先開啟「翻卡後自動朗讀」；下列內容會依順序組成一組播放。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SettingSwitch("英文單字", settings.ttsReadWord) { onBooleanChange("ttsReadWord", it) }
                    SettingSwitch("拼音（逐字母拼讀）", settings.ttsSpellWord) { onBooleanChange("ttsSpellWord", it) }
                    SettingSwitch("中文解釋", settings.ttsReadDefinition) { onBooleanChange("ttsReadDefinition", it) }
                    SettingSwitch("音標", settings.ttsReadPhonetic) { onBooleanChange("ttsReadPhonetic", it) }
                    SettingSwitch("詞性", settings.ttsReadPartOfSpeech) { onBooleanChange("ttsReadPartOfSpeech", it) }
                    SettingSwitch("英文例句", settings.ttsReadExample) { onBooleanChange("ttsReadExample", it) }
                    SettingSwitch("中文例句", settings.ttsReadExampleTranslation) { onBooleanChange("ttsReadExampleTranslation", it) }
                    NumberSetting("每組朗讀次數", settings.ttsGroupRepetitions, 1, 1..3) {
                        onIntChange("ttsGroupRepetitions", it)
                    }
                    Text("朗讀速度 ${"%.1f".format(settings.speechRate)} 倍")
                    Slider(
                        value = settings.speechRate,
                        onValueChange = onSpeechRateChange,
                        valueRange = 0.5f..1.5f,
                        steps = 9
                    )
                    Button(onClick = onPreviewTtsVoice, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                        Text(" 試聽目前聲音")
                    }
                    SettingSwitch("每日學習提醒", settings.remindersEnabled) {
                        if (
                            it &&
                            Build.VERSION.SDK_INT >= 33 &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            onBooleanChange("remindersEnabled", it)
                        }
                    }
                    settings.reminderTimes.forEachIndexed { index, reminder ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    reminder.displayText(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { openReminderPicker(index) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "編輯 ${reminder.displayText()}")
                                }
                                IconButton(
                                    onClick = {
                                        onReminderTimesChange(settings.reminderTimes.filterIndexed { itemIndex, _ -> itemIndex != index })
                                    },
                                    enabled = settings.reminderTimes.size > 1
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "刪除 ${reminder.displayText()}")
                                }
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = { openReminderPicker() },
                        enabled = settings.reminderTimes.size < 12,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("新增提醒時間")
                    }
                }
            }
            if (settingsPage == "更新") item {
                SettingsSection("更新") {
                    SettingSwitch("自動檢查更新", settings.autoCheckUpdates) {
                        onBooleanChange("autoCheckUpdates", it)
                    }
                    SettingSwitch("僅使用 Wi-Fi 更新", settings.wifiOnlyUpdates) {
                        onBooleanChange("wifiOnlyUpdates", it)
                    }
                    Text("目前版本 ${BuildConfig.VERSION_NAME}")
                    Text(updateStatus, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(
                        onClick = ::checkForUpdates,
                        enabled = !updateBusy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("檢查更新")
                    }
                    availableRelease?.let { release ->
                        val cachedApk = downloadedApk?.takeIf(File::exists)
                        if (cachedApk == null) {
                            Button(
                                onClick = { downloadAndInstall(release) },
                                enabled = !updateBusy,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("下載並安裝 Vocab ${release.version}") }
                        } else {
                            Button(
                                onClick = { openInstaller(cachedApk) },
                                enabled = !updateBusy,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("再次安裝 Vocab ${release.version}") }
                            OutlinedButton(
                                onClick = { downloadAndInstall(release, forceDownload = true) },
                                enabled = !updateBusy,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("重新下載安裝檔") }
                        }
                        if (release.notes.isNotBlank()) {
                            Text("更新日誌", fontWeight = FontWeight.Bold)
                            Text(
                                release.notes
                                    .replace(Regex("(?m)^#{1,6}\\s*"), "")
                                    .replace("**", "")
                                    .take(4_000),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            "安裝取消或失敗時會保留這份 APK，可直接再次安裝；重新下載或成功升級後會自動清除舊檔。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (settingsPage == "資料") item {
                SettingsSection("資料") {
                    Text("單字與學習資料預設只儲存在本機。")
                    Button(
                        onClick = { backupLauncher.launch("vocab-backup.json") },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("建立完整備份") }
                    Button(
                        onClick = { restoreLauncher.launch(arrayOf("application/json")) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("還原備份") }
                    Button(
                        onClick = { csvLauncher.launch("vocab-cards.csv") },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("匯出 CSV") }
                    Button(
                        onClick = { showClearDataDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null)
                        Text(" 清空所有資料並從 0 開始")
                    }
                    dataMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                    Spacer(Modifier.padding(bottom = 12.dp))
                }
            }
        }
    }
}

private const val CUSTOM_MODEL_OPTION = "__custom_model__"
private const val AUTO_TTS_VOICE = "__auto_tts_voice__"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingDropdown(
    label: String,
    value: String,
    options: List<String>,
    optionText: (String) -> String = { it },
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = optionText(value),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.distinct().forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionText(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

            }
            content()
        }
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun NumberSetting(
    label: String,
    value: Int,
    step: Int,
    range: IntRange,
    suffix: String = "",
    onChange: (Int) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        IconButton(onClick = { onChange((value - step).coerceIn(range)) }) {
            Icon(Icons.Default.Remove, contentDescription = "減少$label")
        }
        Text("$value$suffix", fontWeight = FontWeight.Bold)
        IconButton(onClick = { onChange((value + step).coerceIn(range)) }) {
            Icon(Icons.Default.Add, contentDescription = "增加$label")
        }
    }
}
