package com.example.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
    onReminderTimeChange: (Int, Int) -> Unit,
    onThemeChange: (String) -> Unit,
    onThemeColorPresetChange: (String) -> Unit,
    onCustomThemeColorsChange: (String, String) -> Unit,
    onGradientColorsChange: (String, String) -> Unit,
    onCustomTextColorChange: (String) -> Unit,
    onBackgroundAppearanceChange: (Float, Float) -> Unit,
    onFontAppearanceChange: (String, Float) -> Unit,
    onSpeechRateChange: (Float) -> Unit,
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
    onAiPromptChange: (String) -> Unit,
    onResetAiPrompt: () -> Unit,
    onAiImagePromptChange: (String) -> Unit,
    onResetAiImagePrompt: () -> Unit,
    onSaveApiKey: (String) -> Unit,
    onClearApiKey: (String) -> Unit,
    onRefreshAiModels: () -> Unit,
    onTestAiConnection: () -> Unit,
    onExportBackup: (Uri) -> Unit,
    onExportCsv: (Uri) -> Unit,
    onImportBackup: (Uri) -> Unit,
    onClearAllData: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var updateStatus by remember { mutableStateOf("尚未檢查更新") }
    var availableRelease by remember { mutableStateOf<GitHubRelease?>(null) }
    var downloadedApk by remember { mutableStateOf<File?>(null) }
    var updateBusy by remember { mutableStateOf(false) }
    var apiKeyInput by remember(settings.aiProvider) { mutableStateOf("") }
    var customModelInput by remember(settings.aiProvider) { mutableStateOf(false) }
    var promptInput by remember(settings.aiWordPrompt) { mutableStateOf(settings.aiWordPrompt) }
    var imagePromptInput by remember(settings.aiImagePrompt) {
        mutableStateOf(settings.aiImagePrompt)
    }
    var customPrimaryInput by remember(settings.customPrimaryColor) {
        mutableStateOf(settings.customPrimaryColor)
    }
    var customSecondaryInput by remember(settings.customSecondaryColor) {
        mutableStateOf(settings.customSecondaryColor)
    }
    var gradientStartInput by remember(settings.gradientStartColor) {
        mutableStateOf(settings.gradientStartColor)
    }
    var gradientEndInput by remember(settings.gradientEndColor) {
        mutableStateOf(settings.gradientEndColor)
    }
    var customTextColorInput by remember(settings.customTextColor) {
        mutableStateOf(settings.customTextColor)
    }
    var brightnessInput by remember(settings.backgroundBrightness) {
        mutableStateOf(settings.backgroundBrightness)
    }
    var opacityInput by remember(settings.backgroundOpacity) {
        mutableStateOf(settings.backgroundOpacity)
    }
    var fontScaleInput by remember(settings.fontScale) {
        mutableStateOf(settings.fontScale)
    }
    var showClearDataDialog by remember { mutableStateOf(false) }
    val installPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        downloadedApk?.takeIf(File::exists)?.let { apk ->
            when (GitHubUpdateManager.launchInstaller(context, apk)) {
                InstallLaunchResult.InstallerOpened -> updateStatus = "已開啟系統安裝畫面"
                is InstallLaunchResult.PermissionRequired -> updateStatus = "請允許安裝此來源後再按一次安裝"
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

    fun checkForUpdates() {
        if (updateBusy) return
        updateBusy = true
        updateStatus = "正在檢查 GitHub 最新版本…"
        coroutineScope.launch {
            when (val result = GitHubUpdateManager.checkForUpdate()) {
                is UpdateCheckResult.Available -> {
                    availableRelease = result.release
                    updateStatus = "發現 Vocab ${result.release.version}"
                }
                UpdateCheckResult.UpToDate -> {
                    availableRelease = null
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

    fun downloadAndInstall(release: GitHubRelease) {
        if (updateBusy) return
        updateBusy = true
        updateStatus = "準備下載 Vocab ${release.version}…"
        coroutineScope.launch {
            GitHubUpdateManager.downloadApk(
                context = context,
                release = release,
                wifiOnly = settings.wifiOnlyUpdates,
                onProgress = { progress -> updateStatus = "正在下載… $progress%" }
            ).onSuccess { apk ->
                downloadedApk = apk
                when (val launch = GitHubUpdateManager.launchInstaller(context, apk)) {
                    InstallLaunchResult.InstallerOpened -> updateStatus = "已開啟系統安裝畫面"
                    is InstallLaunchResult.PermissionRequired -> {
                        updateStatus = "請先允許 Vocab 安裝更新"
                        installPermissionLauncher.launch(launch.intent)
                    }
                }
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

    Scaffold(
        topBar = {
            TopAppBar(
                expandedHeight = 48.dp,
                title = { Text("設定", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SettingsSection("外觀") {
                    Text("主題配色", fontWeight = FontWeight.Bold)
                    val themePresets = listOf(
                        "BLACK" to "黑色",
                        "WHITE" to "白色",
                        "DEEP_BLUE" to "深藍",
                        "LIGHT_BLUE" to "淺藍",
                        "TEAL" to "藍綠",
                        "GREEN" to "綠色",
                        "PINK" to "粉色",
                        "RED" to "紅色",
                        "PURPLE" to "紫色",
                        "ORANGE" to "橘色",
                        "CUSTOM" to "自訂"
                    )
                    SettingDropdown(
                        label = "主題",
                        value = settings.themeColorPreset,
                        options = themePresets.map { it.first },
                        optionText = { value ->
                            themePresets.firstOrNull { it.first == value }?.second ?: value
                        },
                        onSelected = onThemeColorPresetChange
                    )
                    if (settings.themeColorPreset == "CUSTOM") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ColorPickerButton(
                                label = "主色",
                                colorHex = customPrimaryInput,
                                onColorSelected = { selected ->
                                    customPrimaryInput = selected
                                    onCustomThemeColorsChange(selected, customSecondaryInput)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            ColorPickerButton(
                                label = "輔助色",
                                colorHex = customSecondaryInput,
                                onColorSelected = { selected ->
                                    customSecondaryInput = selected
                                    onCustomThemeColorsChange(customPrimaryInput, selected)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    SettingSwitch("使用漸層背景", settings.gradientEnabled) {
                        onBooleanChange("gradientEnabled", it)
                    }
                    if (settings.gradientEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ColorPickerButton(
                                label = "漸層起點",
                                colorHex = gradientStartInput,
                                onColorSelected = { selected ->
                                    gradientStartInput = selected
                                    onGradientColorsChange(selected, gradientEndInput)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            ColorPickerButton(
                                label = "漸層終點",
                                colorHex = gradientEndInput,
                                onColorSelected = { selected ->
                                    gradientEndInput = selected
                                    onGradientColorsChange(gradientStartInput, selected)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Text("背景亮度 ${(brightnessInput * 100).toInt()}%")
                    Slider(
                        value = brightnessInput,
                        onValueChange = { brightnessInput = it },
                        onValueChangeFinished = {
                            onBackgroundAppearanceChange(brightnessInput, opacityInput)
                        },
                        valueRange = 0.55f..1.35f
                    )
                    Text("背景透明度 ${(opacityInput * 100).toInt()}%")
                    Slider(
                        value = opacityInput,
                        onValueChange = { opacityInput = it },
                        onValueChangeFinished = {
                            onBackgroundAppearanceChange(brightnessInput, opacityInput)
                        },
                        valueRange = 0.55f..1f
                    )
                    SettingDropdown(
                        label = "字體",
                        value = settings.fontFamily,
                        options = listOf("DEFAULT", "SERIF", "MONOSPACE"),
                        optionText = {
                            when (it) {
                                "SERIF" -> "襯線字體"
                                "MONOSPACE" -> "等寬字體"
                                else -> "系統字體"
                            }
                        },
                        onSelected = { onFontAppearanceChange(it, settings.fontScale) }
                    )
                    Text("字體大小 ${(fontScaleInput * 100).toInt()}%")
                    Slider(
                        value = fontScaleInput,
                        onValueChange = { fontScaleInput = it },
                        onValueChangeFinished = {
                            onFontAppearanceChange(settings.fontFamily, fontScaleInput)
                        },
                        valueRange = 0.85f..1.3f
                    )
                    SettingSwitch("自訂字體顏色", settings.customTextColorEnabled) {
                        onBooleanChange("customTextColorEnabled", it)
                    }
                    if (settings.customTextColorEnabled) {
                        ColorPickerButton(
                            label = "字體顏色",
                            colorHex = customTextColorInput,
                            onColorSelected = { selected ->
                                customTextColorInput = selected
                                onCustomTextColorChange(selected)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "對比不足時會自動改用清楚的預設文字色",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
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
            item {
                SettingsSection("AI 補齊", initiallyExpanded = false) {
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
            item {
                SettingsSection("AI 連線", initiallyExpanded = false) {
                    val provider = AiProvider.from(settings.aiProvider)
                    val selectedProviderConfigured =
                        aiApiProfiles.any { it.provider == provider }
                    SettingDropdown(
                        label = "AI 供應商",
                        value = provider.name,
                        options = AiProvider.entries.map { it.name },
                        optionText = { AiProvider.from(it).displayName },
                        onSelected = {
                            customModelInput = false
                            onAiProviderChange(it)
                        }
                    )
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
                        Button(onClick = onTestAiConnection, enabled = selectedProviderConfigured && settings.aiModel.isNotBlank()) {
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
            item {
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
            item {
                SettingsSection("發音與提醒") {
                    SettingSwitch("翻卡後自動朗讀", settings.autoSpeak) { onBooleanChange("autoSpeak", it) }
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
                    Button(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    onReminderTimeChange(hour, minute)
                                },
                                settings.reminderHour,
                                settings.reminderMinute,
                                true
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "提醒時間 ${"%02d:%02d".format(settings.reminderHour, settings.reminderMinute)}"
                        )
                    }
                }
            }
            item {
                SettingsSection("更新", initiallyExpanded = false) {
                    SettingSwitch("自動檢查更新", settings.autoCheckUpdates) {
                        onBooleanChange("autoCheckUpdates", it)
                    }
                    SettingSwitch("僅使用 Wi-Fi 更新", settings.wifiOnlyUpdates) {
                        onBooleanChange("wifiOnlyUpdates", it)
                    }
                    Text("目前版本 ${BuildConfig.VERSION_NAME}（${BuildConfig.VERSION_CODE}）")
                    Text(updateStatus, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(
                        onClick = ::checkForUpdates,
                        enabled = !updateBusy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("檢查更新")
                    }
                    availableRelease?.let { release ->
                        Button(
                            onClick = { downloadAndInstall(release) },
                            enabled = !updateBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("下載並安裝 Vocab ${release.version}")
                        }
                        Text(
                            "下載後會開啟 Android 系統安裝畫面，需由你確認安裝。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                SettingsSection("資料", initiallyExpanded = false) {
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

@Composable
private fun ColorPickerButton(
    label: String,
    colorHex: String,
    onColorSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by rememberSaveable(label) { mutableStateOf(false) }
    val currentColor = remember(colorHex) { parseColorOrDefault(colorHex) }

    Card(
        modifier = modifier.clickable { showPicker = true },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier.size(30.dp),
                shape = CircleShape,
                color = currentColor
            ) {}
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text("選擇", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
    }

    if (showPicker) {
        val initialHsv = remember(colorHex, showPicker) { colorHexToHsv(colorHex) }
        var hue by remember(colorHex, showPicker) { mutableStateOf(initialHsv[0]) }
        var saturation by remember(colorHex, showPicker) { mutableStateOf(initialHsv[1]) }
        var value by remember(colorHex, showPicker) { mutableStateOf(initialHsv[2]) }
        val previewHex = hsvToHex(hue, saturation, value)
        val quickColors = remember {
            listOf(
                "#111827", "#FFFFFF", "#173B63", "#5EA9E8", "#168A8A", "#3F8C69",
                "#E78BB4", "#D9534F", "#845EC2", "#E98B3A", "#F2C94C", "#8D6E63"
            )
        }

        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("選擇$label") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = parseColorOrDefault(previewHex)
                    ) {}
                    Text("常用調色盤", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(quickColors) { hex ->
                            Surface(
                                onClick = {
                                    val hsv = colorHexToHsv(hex)
                                    hue = hsv[0]
                                    saturation = hsv[1]
                                    value = hsv[2]
                                },
                                modifier = Modifier.size(34.dp),
                                shape = CircleShape,
                                color = parseColorOrDefault(hex)
                            ) {}
                        }
                    }
                    Text("色相 ${hue.toInt()}°", style = MaterialTheme.typography.labelMedium)
                    Slider(value = hue, onValueChange = { hue = it }, valueRange = 0f..360f)
                    Text("鮮豔度 ${(saturation * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                    Slider(value = saturation, onValueChange = { saturation = it }, valueRange = 0f..1f)
                    Text("亮度 ${(value * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                    Slider(value = value, onValueChange = { value = it }, valueRange = 0f..1f)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onColorSelected(previewHex)
                        showPicker = false
                    }
                ) { Text("套用") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("取消") }
            }
        )
    }
}

private fun colorHexToHsv(colorHex: String): FloatArray {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(
        runCatching { android.graphics.Color.parseColor(colorHex) }
            .getOrDefault(android.graphics.Color.DKGRAY),
        hsv
    )
    return hsv
}

private fun hsvToHex(hue: Float, saturation: Float, value: Float): String {
    val color = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))
    return String.format("#%06X", color and 0xFFFFFF)
}

private fun parseColorOrDefault(colorHex: String): Color = Color(
    runCatching { android.graphics.Color.parseColor(colorHex) }
        .getOrDefault(android.graphics.Color.DKGRAY)
)

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
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by rememberSaveable(title) { mutableStateOf(initiallyExpanded) }
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "收合" else "展開",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (expanded) content()
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
