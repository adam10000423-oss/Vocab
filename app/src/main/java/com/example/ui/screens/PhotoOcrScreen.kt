package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.activity.result.IntentSenderRequest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.entity.Deck
import com.example.data.api.GeminiService
import com.example.util.AiImagePreprocessor
import com.example.util.DocumentParser
import com.example.util.OcrCardCandidate
import com.example.util.OcrWordParser
import com.example.ui.components.rememberResponsiveLayout
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PhotoOcrScreen(
    decks: List<Deck>,
    candidates: List<OcrCardCandidate>,
    initialDeckId: Long? = null,
    onParseText: (String, Long) -> Unit,
    onImportCandidates: (List<OcrCardCandidate>, Long) -> Unit,
    onBack: () -> Unit,
    aiAvailable: Boolean = false,
    pdfPageLimit: Int = 20,
    modifier: Modifier = Modifier
) {
    val responsive = rememberResponsiveLayout()
    val context = LocalContext.current
    var rawInputText by remember { mutableStateOf("") }
    var selectedDeckId by remember(decks, initialDeckId) {
        mutableStateOf(
            initialDeckId?.takeIf { requestedId -> decks.any { it.id == requestedId } }
                ?: decks.firstOrNull()?.id
        )
    }
    var isOcrScanning by remember { mutableStateOf(false) }
    var isScannerLaunching by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var isAiEnriching by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val localCandidates = remember(candidates) {
        mutableStateListOf<OcrCardCandidate>().apply { addAll(candidates) }
    }

    LaunchedEffect(decks) {
        if (decks.none { it.id == selectedDeckId }) {
            selectedDeckId = decks.firstOrNull()?.id
        }
    }

    val processScannedUri: (Uri, String) -> Unit = { uri, source ->
        isOcrScanning = true
        coroutineScope.launch {
            runCatching { DocumentParser.parseDocumentToCards(context, uri, pdfPageLimit) }
                .onSuccess { (text, parsed) ->
                    isOcrScanning = false
                    rawInputText = text
                    localCandidates.clear()
                    localCandidates.addAll(parsed)
                    snackbarHostState.showSnackbar(
                        when {
                            text.isBlank() -> "${source}未讀取到文字，請換一張更清晰的影像"
                            parsed.isEmpty() -> "${source}已讀到文字，但沒有找到可匯入的英文單字"
                            else -> "${source}完成，找到 ${parsed.size} 張單字卡"
                        }
                    )
                }
                .onFailure {
                    isOcrScanning = false
                    snackbarHostState.showSnackbar("${source}失敗：${it.message ?: "無法讀取內容"}")
                }
        }
    }

    val processImageUris: (List<Uri>, String) -> Unit = processImages@ { uris, source ->
        if (!aiAvailable) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("請先在設定中啟用個人 AI API 並選擇支援圖片的模型")
            }
            return@processImages
        }
        val selectedUris = uris.take(pdfPageLimit.coerceIn(1, 50))
        if (selectedUris.isEmpty()) return@processImages

        isOcrScanning = true
        coroutineScope.launch {
            runCatching {
                selectedUris
                    .chunked(4)
                    .flatMap { uriChunk ->
                        val payloads = uriChunk.map { uri ->
                            AiImagePreprocessor.prepare(context, uri)
                        }
                        GeminiService.analyzeImages(payloads)
                    }
                    .distinctBy { it.word.trim().lowercase() }
            }.onSuccess { parsed ->
                isOcrScanning = false
                rawInputText = ""
                localCandidates.clear()
                localCandidates.addAll(parsed)
                snackbarHostState.showSnackbar(
                    if (parsed.isEmpty()) {
                        "${source}完成，但 AI 沒有找到可匯入的英文單字"
                    } else {
                        "${source}完成，AI 找到 ${parsed.size} 張單字卡"
                    }
                )
            }.onFailure { error ->
                isOcrScanning = false
                snackbarHostState.showSnackbar(
                    "${source}失敗：${error.localizedMessage ?: "多模態 AI 無法讀取圖片"}"
                )
            }
        }
    }

    val documentScannerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        isScannerLaunching = false
        if (activityResult.resultCode == Activity.RESULT_OK) {
            val result = GmsDocumentScanningResult.fromActivityResultIntent(activityResult.data)
            val pageUris = result?.pages.orEmpty().map { it.imageUri }
            if (pageUris.isNotEmpty()) {
                processImageUris(pageUris, "文件掃描")
            } else {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("文件掃描沒有回傳可傳送給 AI 的頁面圖片")
                }
            }
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("已取消文件掃描")
            }
        }
    }

    val scannerOptions = remember(pdfPageLimit) {
        GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(pdfPageLimit.coerceIn(1, 50))
            .setResultFormats(
                GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                GmsDocumentScannerOptions.RESULT_FORMAT_PDF
            )
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
    }

    // Full-resolution camera capture. TakePicturePreview only returns a thumbnail
    // and is not reliable enough for a full vocabulary page.
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { saved ->
        val capturedUri = pendingCameraUri
        pendingCameraUri = null
        if (saved && capturedUri != null) {
            processImageUris(listOf(capturedUri), "拍照辨識")
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("已取消拍照")
            }
        }
    }

    // Photo Picker Launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { processImageUris(listOf(it), "相片辨識") }
    }
    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val mimeType = context.contentResolver.getType(it).orEmpty()
            val uriText = it.toString().lowercase()
            val isImage = mimeType.startsWith("image/") ||
                listOf(".jpg", ".jpeg", ".png", ".webp", ".gif", ".heic", ".heif")
                    .any(uriText::endsWith)
            if (isImage) {
                processImageUris(listOf(it), "圖片辨識")
            } else {
                processScannedUri(it, "檔案分析")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                expandedHeight = 48.dp,
                title = { Text("掃描匯入", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(top = 6.dp)
                .padding(responsive.horizontalPadding),
            verticalArrangement = Arrangement.spacedBy(if (responsive.isConstrained) 8.dp else 16.dp)
        ) {
            // Instructions Banner
            if (!responsive.isLandscape) Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "圖片會直接交給多模態 AI；確認結果後才會匯入。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (decks.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "尚未建立資料夾；可以先辨識，但無法匯入。",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("匯入到", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(decks, key = { it.id }) { deck ->
                            FilterChip(
                                selected = selectedDeckId == deck.id,
                                onClick = { selectedDeckId = deck.id },
                                label = { Text(deck.name) }
                            )
                        }
                    }
                }
            }

            if (!aiAvailable) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "拍照與相片辨識需要先在設定中啟用個人 AI API。",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            val sourceButtonsEnabled = !isOcrScanning && !isScannerLaunching

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        runCatching {
                            val photoFile = File.createTempFile(
                                "vocab_capture_",
                                ".jpg",
                                context.cacheDir
                            )
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                photoFile
                            )
                            pendingCameraUri = uri
                            cameraLauncher.launch(uri)
                        }.onFailure { error ->
                            pendingCameraUri = null
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    "無法開啟相機：${error.localizedMessage ?: "請確認裝置有可用的相機應用程式"}"
                                )
                            }
                        }
                    },
                    enabled = sourceButtonsEnabled && aiAvailable,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("take_photo_ocr_button")
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("拍照", fontWeight = FontWeight.Bold)
                }

                FilledTonalButton(
                    onClick = {
                        val activity = context.findActivity()
                        if (activity == null) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("此裝置無法啟動文件掃描")
                            }
                        } else {
                            isScannerLaunching = true
                            GmsDocumentScanning.getClient(scannerOptions)
                                .getStartScanIntent(activity)
                                .addOnSuccessListener { sender ->
                                    documentScannerLauncher.launch(IntentSenderRequest.Builder(sender).build())
                                }
                                .addOnFailureListener { error ->
                                    isScannerLaunching = false
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            "文件掃描無法啟動：${error.localizedMessage ?: "請確認 Google Play 服務與網路"}"
                                        )
                                    }
                                }
                        }
                    },
                    enabled = sourceButtonsEnabled && aiAvailable,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("document_scanner_button")
                ) {
                    Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("掃文件", fontWeight = FontWeight.Bold)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = { photoPickerLauncher.launch("image/*") },
                    enabled = sourceButtonsEnabled && aiAvailable,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("select_photo_ocr_button")
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("相片", fontWeight = FontWeight.Bold)
                }

                FilledTonalButton(
                    onClick = {
                        docPickerLauncher.launch(
                            arrayOf(
                                "application/pdf",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                "text/plain",
                                "text/csv",
                                "text/tab-separated-values",
                                "application/json",
                                "image/*"
                            )
                        )
                    },
                    enabled = sourceButtonsEnabled,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("select_file_doc_button")
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("檔案", fontWeight = FontWeight.Bold)
                }
            }

            if (isOcrScanning || isScannerLaunching) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        Text(
                            if (isScannerLaunching) "正在準備文件掃描…" else "多模態 AI 正在辨識圖片…",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Raw Text Input / OCR Result Area
            OutlinedTextField(
                value = rawInputText,
                onValueChange = { rawInputText = it },
                label = { Text("辨識文字") },
                placeholder = { Text("可拍照辨識，或直接貼上文字") },
                trailingIcon = {
                    IconButton(
                        onClick = { onParseText(rawInputText, selectedDeckId ?: 0L) },
                        enabled = rawInputText.isNotBlank() && !isOcrScanning
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "解析文字")
                    }
                },
                maxLines = 4,
                enabled = !isOcrScanning,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ocr_text_input")
            )

            // Extracted Candidates Table List
            if (localCandidates.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "找到 ${localCandidates.size} 張",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(1f)
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // AI Auto Fill Button
                            if (aiAvailable) Button(
                                onClick = {
                                    if (!isAiEnriching) {
                                        isAiEnriching = true
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("AI 正在分析並補齊單字資訊")
                                            val currentList = localCandidates.toList()
                                            runCatching {
                                                val aiEnriched =
                                                    com.example.data.api.GeminiService.batchEnrichCandidates(currentList)
                                                OcrWordParser.enrichCandidatesWithDictionary(aiEnriched)
                                            }.onSuccess { finalEnriched ->
                                                localCandidates.clear()
                                                localCandidates.addAll(finalEnriched)
                                                val changed = finalEnriched != currentList
                                                val serviceError =
                                                    com.example.data.api.GeminiService.lastError.value
                                                snackbarHostState.showSnackbar(
                                                    when {
                                                        serviceError != null ->
                                                            "AI 補齊失敗：$serviceError"
                                                        changed -> "AI 補齊完成，請確認內容"
                                                        else -> "AI 沒有回傳可套用的新資料"
                                                    }
                                                )
                                            }.onFailure { error ->
                                                snackbarHostState.showSnackbar(
                                                    "AI 補齊失敗：${error.localizedMessage ?: "請檢查 API 設定"}"
                                                )
                                            }
                                            isAiEnriching = false
                                        }
                                    }
                                },
                                enabled = !isAiEnriching,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.testTag("ai_batch_enrich_button")
                            ) {
                                if (isAiEnriching) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.onSecondary,
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(if (isAiEnriching) "處理中…" else "AI 補齊", fontWeight = FontWeight.Bold)
                            }

                            // Batch Import Button
                            Button(
                                onClick = {
                                    selectedDeckId?.let { deckId ->
                                        onImportCandidates(localCandidates.filter { it.isSelected }, deckId)
                                    }
                                },
                                enabled = selectedDeckId != null && localCandidates.any { it.isSelected },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.testTag("batch_import_button")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("匯入")
                            }
                        }
                    }
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(localCandidates) { index, candidate ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = candidate.isSelected,
                                    onCheckedChange = { checked ->
                                        localCandidates[index] = candidate.copy(isSelected = checked)
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = candidate.word,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (candidate.phonetic.isNotBlank()) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = candidate.phonetic,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                        if (candidate.partOfSpeech.isNotBlank()) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = candidate.partOfSpeech,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = candidate.definition.ifBlank { "待補齊" },
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                    )
                                    if (candidate.exampleSentence.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "例: ${candidate.exampleSentence}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    }
                                    if (candidate.exampleTranslation.isNotBlank()) {
                                        Text(
                                            text = candidate.exampleTranslation,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { localCandidates.removeAt(index) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "移除此項",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (!isOcrScanning && !isScannerLaunching) {
                com.example.ui.components.CalmEmptyState(
                    icon = Icons.Default.DocumentScanner,
                    title = "等待辨識內容",
                    message = "拍照、掃描文件，或選擇現有檔案。",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
