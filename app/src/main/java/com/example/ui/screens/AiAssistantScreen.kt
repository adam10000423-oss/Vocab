package com.example.ui.screens

import android.net.Uri
import android.app.Activity
import android.content.Intent
import android.content.Context
import android.content.ContextWrapper
import android.content.ClipData
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import com.example.data.assistant.AssistantConversation
import com.example.data.assistant.AssistantMessage
import com.example.data.assistant.AssistantPendingAction
import com.example.viewmodel.AssistantSendOutcome
import com.example.data.entity.Deck
import com.example.data.entity.Flashcard
import com.example.ui.components.rememberResponsiveLayout
import org.json.JSONArray
import org.json.JSONObject
import androidx.core.content.FileProvider
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File
import kotlinx.coroutines.delay
import coil.compose.AsyncImage
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiAssistantScreen(
    conversations: List<AssistantConversation>,
    currentConversationId: String?,
    messages: List<AssistantMessage>,
    busy: Boolean,
    status: String?,
    sendOutcome: AssistantSendOutcome?,
    pendingAction: AssistantPendingAction?,
    undoAction: AssistantPendingAction?,
    apiConfigured: Boolean,
    contextDeckName: String?,
    decks: List<Deck>,
    cards: List<Flashcard>,
    selectedDeckIds: Set<Long>,
    onScopeChange: (Set<Long>) -> Unit,
    onSend: (String, List<Uri>) -> Unit,
    onStop: () -> Unit,
    onQuizCompleted: (String, Int, Int) -> Unit,
    onQuizRestart: (String) -> Unit,
    onQuizProgress: (String, Int, Int, Int, Map<Int, Int>) -> Unit,
    onSpeak: (String) -> Unit,
    onStopSpeaking: () -> Unit,
    onPronunciationResult: (Long, Int) -> Unit,
    onReadingMistake: (Long) -> Unit,
    onCopyReadingCard: (Flashcard, Long, (Boolean) -> Unit) -> Unit,
    onCreateFolderAndCopyReadingCard: (String, String, String, String, Flashcard, (Boolean) -> Unit) -> Unit,
    onConfirmAction: () -> Unit,
    onCancelAction: () -> Unit,
    onUndoAction: () -> Unit,
    onNewConversation: (Boolean) -> Unit,
    onSelectConversation: (String) -> Unit,
    onDeleteConversation: (String) -> Unit,
    onClearChats: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val responsive = rememberResponsiveLayout()
    val density = LocalDensity.current
    val useLandscapeInputOverlay = responsive.isLandscape &&
        WindowInsets.ime.getBottom(density) > 0
    var input by remember { mutableStateOf("") }
    val context = LocalContext.current
    var attachments by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var showAttachmentSources by remember { mutableStateOf(false) }
    var attachmentError by remember { mutableStateOf<String?>(null) }
    var previewImageUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { selected ->
        selected.forEach { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
        attachments = (attachments + selected).distinct().take(5)
    }
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { selected -> attachments = (attachments + selected).distinct().take(5) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { saved ->
        val uri = pendingCameraUri
        pendingCameraUri = null
        if (saved && uri != null) attachments = (attachments + uri).distinct().take(5)
    }
    val scannerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val pages = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                ?.pages.orEmpty().map { it.imageUri }
            attachments = (attachments + pages).distinct().take(5)
        }
    }
    val scannerOptions = remember {
        GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(5)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
    }
    var showHistory by remember { mutableStateOf(false) }
    var showClearConfirmation by remember { mutableStateOf(false) }
    var showScopeSelector by remember { mutableStateOf(false) }
    var draftScope by remember(selectedDeckIds, showScopeSelector) {
        mutableStateOf(selectedDeckIds)
    }
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val currentConversation = conversations.firstOrNull { it.id == currentConversationId }
    var activeQuizMessageId by remember { mutableStateOf<String?>(null) }
    var activeArticleMessageId by remember { mutableStateOf<String?>(null) }
    var knownQuizMessageIds by remember {
        mutableStateOf<Set<String>>(messages.filter { it.kind == "QUIZ" }.mapTo(hashSetOf()) { it.id })
    }

    fun submit() {
        val value = input.trim()
        if ((value.isBlank() && attachments.isEmpty()) || busy) return
        val files = attachments
        input = ""
        onSend(value, files)
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    LaunchedEffect(sendOutcome?.id) {
        val outcome = sendOutcome ?: return@LaunchedEffect
        if (outcome.succeeded) {
            attachments = emptyList()
        } else {
            attachmentError = outcome.errorMessage ?: "附件無法讀取或傳送，請確認格式後重試"
        }
    }

    LaunchedEffect(messages.size, pendingAction, status, busy) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    LaunchedEffect(messages) {
        val currentQuizIds = messages.filter { it.kind == "QUIZ" }.mapTo(linkedSetOf()) { it.id }
        val newQuizId = currentQuizIds.lastOrNull { it !in knownQuizMessageIds }
        if (newQuizId != null) activeQuizMessageId = newQuizId
        knownQuizMessageIds = currentQuizIds
    }

    attachmentError?.let { error ->
        AlertDialog(
            onDismissRequest = { attachmentError = null },
            title = { Text("無法加入附件") },
            text = { Text(error) },
            confirmButton = { TextButton(onClick = { attachmentError = null }) { Text("確定") } }
        )
    }

    previewImageUri?.let { uri ->
        AssistantImagePreviewDialog(
            uri = uri,
            onCopy = { copyAssistantImage(context, uri) },
            onDismiss = { previewImageUri = null }
        )
    }

    if (showAttachmentSources) {
        AlertDialog(
            onDismissRequest = { showAttachmentSources = false },
            title = { Text("加入附件") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AttachmentSourceButton(Icons.Default.PhotoLibrary, "照片") {
                        showAttachmentSources = false
                        photoPicker.launch("image/*")
                    }
                    AttachmentSourceButton(Icons.Default.CameraAlt, "拍攝") {
                        showAttachmentSources = false
                        runCatching {
                            val file = File.createTempFile("ai_chat_camera_", ".jpg", context.cacheDir)
                            FileProvider.getUriForFile(
                                context, "${context.packageName}.fileprovider", file
                            )
                        }.onSuccess { uri ->
                            pendingCameraUri = uri
                            cameraLauncher.launch(uri)
                        }.onFailure { attachmentError = it.localizedMessage ?: "無法開啟相機" }
                    }
                    AttachmentSourceButton(Icons.Default.DocumentScanner, "掃描") {
                        showAttachmentSources = false
                        val activity = context.findAssistantActivity()
                        if (activity == null) {
                            attachmentError = "此裝置無法啟動文件掃描"
                        } else {
                            GmsDocumentScanning.getClient(scannerOptions)
                                .getStartScanIntent(activity)
                                .addOnSuccessListener { sender ->
                                    scannerLauncher.launch(IntentSenderRequest.Builder(sender).build())
                                }
                                .addOnFailureListener {
                                    attachmentError = it.localizedMessage ?: "請確認 Google Play 服務與網路"
                                }
                        }
                    }
                    AttachmentSourceButton(Icons.Default.InsertDriveFile, "檔案") {
                        showAttachmentSources = false
                        filePicker.launch(
                            arrayOf(
                                "application/pdf", "text/*",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                            )
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAttachmentSources = false }) { Text("取消") }
            }
        )
    }

    val activeQuizMessage = activeQuizMessageId?.let { id -> messages.firstOrNull { it.id == id } }
    if (activeQuizMessage != null) {
        AssistantQuizScreen(
            message = activeQuizMessage,
            onCompleted = onQuizCompleted,
            onRestart = onQuizRestart,
            onProgress = onQuizProgress,
            onBack = { activeQuizMessageId = null }
        )
        return
    }
    val activeArticleMessage = activeArticleMessageId?.let { id -> messages.firstOrNull { it.id == id } }
    if (activeArticleMessage != null) {
        InteractiveReadingScreen(
            message = activeArticleMessage,
            cards = cards,
            decks = decks,
            onSpeak = onSpeak,
            onStopSpeaking = onStopSpeaking,
            onPronunciationResult = onPronunciationResult,
            onReadingMistake = onReadingMistake,
            onCopyCardToDeck = onCopyReadingCard,
            onCreateFolderAndCopyCard = onCreateFolderAndCopyReadingCard,
            onBack = { activeArticleMessageId = null }
        )
        return
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("清空全部聊天？") },
            text = { Text("所有已保存的對話都會刪除；已經套用到單字庫的操作不會被撤銷。") },
            confirmButton = {
                TextButton(onClick = {
                    onClearChats()
                    showClearConfirmation = false
                }) { Text("確認清空", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) { Text("取消") }
            }
        )
    }

    if (showScopeSelector) {
        val allSelected = decks.isNotEmpty() && draftScope.containsAll(decks.map { it.id })
        AlertDialog(
            onDismissRequest = { showScopeSelector = false },
            title = { Text("AI 可編輯的資料夾") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "AI 仍可讀取整個單字庫並回答其他知識；這裡只限制它能修改哪些資料夾。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        onClick = {
                            draftScope = if (allSelected) emptySet() else decks.map { it.id }.toSet()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = allSelected,
                                onCheckedChange = {
                                    draftScope = if (it) decks.map { deck -> deck.id }.toSet() else emptySet()
                                }
                            )
                            Text("全部資料夾 (${decks.size})", fontWeight = FontWeight.Bold)
                        }
                    }
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 340.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(decks, key = { it.id }) { deck ->
                            Surface(
                                onClick = {
                                    draftScope = if (deck.id in draftScope) {
                                        draftScope - deck.id
                                    } else {
                                        draftScope + deck.id
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = deck.id in draftScope,
                                        onCheckedChange = { checked ->
                                            draftScope = if (checked) draftScope + deck.id else draftScope - deck.id
                                        }
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(deck.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(
                                            deck.category,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onScopeChange(draftScope)
                        showScopeSelector = false
                    },
                    enabled = draftScope.isNotEmpty() || decks.isEmpty()
                ) { Text("套用") }
            },
            dismissButton = {
                TextButton(onClick = { showScopeSelector = false }) { Text("取消") }
            }
        )
    }

    if (showHistory) {
        AlertDialog(
            onDismissRequest = { showHistory = false },
            title = { Text("對話紀錄") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                onNewConversation(false)
                                showHistory = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Text("新對話")
                        }
                        OutlinedButton(
                            onClick = {
                                onNewConversation(true)
                                showHistory = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.VisibilityOff, contentDescription = null)
                            Text("無痕")
                        }
                    }
                    LazyColumn(
                        modifier = Modifier.heightIn(max = responsive.dialogContentMaxHeight),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(conversations, key = { it.id }) { conversation ->
                            Surface(
                                onClick = {
                                    onSelectConversation(conversation.id)
                                    showHistory = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (conversation.id == currentConversationId) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        conversation.title,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    IconButton(onClick = { onDeleteConversation(conversation.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "刪除對話")
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistory = false }) { Text("完成") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showHistory = false
                    showClearConfirmation = true
                }) { Text("清空全部", color = MaterialTheme.colorScheme.error) }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (!useLandscapeInputOverlay) TopAppBar(
                title = {
                    Column {
                        Text("Vocab AI", fontWeight = FontWeight.Bold)
                        Text(
                            when {
                                currentConversation?.incognito == true -> "無痕對話・離開後不保存"
                                selectedDeckIds.size == decks.size && decks.isNotEmpty() -> "可編輯全部 ${decks.size} 個資料夾"
                                selectedDeckIds.size > 1 -> "可編輯 ${selectedDeckIds.size} 個資料夾"
                                contextDeckName != null -> "可編輯：$contextDeckName"
                                else -> "可讀取全部內容；尚未指定編輯範圍"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { onNewConversation(false) }) {
                        Icon(Icons.Default.Add, contentDescription = "新對話")
                    }
                    IconButton(onClick = { showHistory = true }) {
                        Icon(Icons.Default.History, contentDescription = "對話紀錄")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        // The activity window already resizes for the software keyboard. Applying
        // imePadding here a second time pushes the composer up by another full
        // keyboard height and leaves a large empty area underneath it.
        modifier = modifier,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Top
        )
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp)
        ) {
            if (!useLandscapeInputOverlay) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (messages.isEmpty()) {
                        item {
                            AssistantWelcome(
                                apiConfigured = apiConfigured,
                                onSuggestion = { onSend(it, emptyList()) },
                                onOpenSettings = onOpenSettings
                            )
                        }
                    }
                    itemsIndexed(messages, key = { index, message -> "${message.id}:$index" }) { _, message ->
                        AssistantMessageBubble(
                            message = message,
                            onQuizCompleted = onQuizCompleted,
                            onQuizRestart = onQuizRestart,
                            onOpenQuiz = { activeQuizMessageId = message.id },
                            onOpenArticle = { activeArticleMessageId = message.id },
                            onPreviewImage = { previewImageUri = it }
                        )
                    }
                    if (busy) {
                        item {
                            AssistantTypingIndicator(status)
                        }
                    }
                    pendingAction?.let { action ->
                        item {
                            AssistantActionCard(action, onConfirmAction, onCancelAction)
                        }
                    }
                    undoAction?.let { action ->
                        item {
                            OutlinedButton(onClick = onUndoAction, enabled = !busy) {
                                Icon(Icons.Default.History, contentDescription = null)
                                Text(action.title)
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (useLandscapeInputOverlay) Modifier
                        else Modifier.imePadding()
                    )
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (attachments.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        attachments.forEach { uri ->
                            AssistantPendingAttachment(
                                uri = uri,
                                onPreview = { previewImageUri = uri },
                                onRemove = { attachments = attachments - uri }
                            )
                        }
                    }
                }
                if (!useLandscapeInputOverlay) Surface(
                    onClick = { showScopeSelector = true },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.heightIn(min = 36.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(
                            when {
                                decks.isEmpty() -> "目前沒有資料夾"
                                selectedDeckIds.size == decks.size -> "可編輯：全部資料夾 (${decks.size})"
                                selectedDeckIds.size == 1 -> "可編輯：${decks.firstOrNull { it.id in selectedDeckIds }?.name ?: "1 個資料夾"}"
                                else -> "可編輯：已選 ${selectedDeckIds.size} 個資料夾"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "選擇資料夾")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { showAttachmentSources = true },
                        enabled = !busy
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = "附加圖片或文件")
                    }
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it.take(4_000) },
                        placeholder = { Text("告訴 AI 你想做什麼…") },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        shape = RoundedCornerShape(18.dp),
                        minLines = 1,
                        maxLines = 2,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { submit() }),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 56.dp, max = if (responsive.isLargeText) 120.dp else 84.dp)
                            .testTag("assistant_input")
                    )
                    Surface(
                        onClick = { if (busy) onStop() else submit() },
                        enabled = busy || input.isNotBlank() || attachments.isNotEmpty(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (busy) {
                                Icon(Icons.Default.Stop, contentDescription = "停止 AI 回覆")
                            } else {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "傳送")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AssistantWelcome(
    apiConfigured: Boolean,
    onSuggestion: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(32.dp))
            Text("想整理或練習哪些單字？", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "AI 會先顯示正在讀取的資料與預計操作；修改內容前一定會讓你確認。",
                style = MaterialTheme.typography.bodyMedium
            )
            if (!apiConfigured) {
                Button(onClick = onOpenSettings) { Text("設定 AI API") }
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "依照字母排序這個資料夾",
                        "找出容易混淆的單字",
                        "根據這個資料夾寫一篇文章",
                        "製作五題互動測驗"
                    ).forEach { suggestion ->
                        OutlinedButton(onClick = { onSuggestion(suggestion) }) {
                            Text(suggestion, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentSourceButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.size(8.dp))
        Text(label, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun AssistantPendingAttachment(
    uri: Uri,
    onPreview: () -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val image = remember(uri) { isAssistantImage(context, uri) }
    if (!image) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Row(
                modifier = Modifier.padding(start = 8.dp, end = 2.dp, top = 3.dp, bottom = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    uri.lastPathSegment?.substringAfterLast('/')?.take(22) ?: "附件",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "移除附件", modifier = Modifier.size(16.dp))
                }
            }
        }
        return
    }
    Box(modifier = Modifier.size(76.dp)) {
        Surface(
            onClick = onPreview,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxSize()
        ) {
            AsyncImage(
                model = uri,
                contentDescription = "預覽圖片",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Surface(
            onClick = onRemove,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.TopEnd).size(26.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Close, contentDescription = "移除圖片", modifier = Modifier.size(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AssistantSentAttachments(
    uris: List<Uri>,
    onPreviewImage: (Uri) -> Unit
) {
    val context = LocalContext.current
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        uris.forEach { uri ->
            if (isAssistantImage(context, uri)) {
                Surface(
                    onClick = { onPreviewImage(uri) },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(92.dp)
                ) {
                    AsyncImage(
                        model = uri,
                        contentDescription = "訊息圖片",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.InsertDriveFile, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.size(5.dp))
                        Text(
                            uri.lastPathSegment?.substringAfterLast('/')?.take(24) ?: "附件",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AssistantImagePreviewDialog(
    uri: Uri,
    onCopy: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AsyncImage(
                    model = uri,
                    contentDescription = "放大圖片",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp, max = 620.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onCopy) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Text("複製圖片")
                    }
                    TextButton(onClick = onDismiss) { Text("關閉") }
                }
            }
        }
    }
}

private fun isAssistantImage(context: Context, uri: Uri): Boolean {
    val mime = runCatching { context.contentResolver.getType(uri) }.getOrNull().orEmpty()
    if (mime.startsWith("image/", ignoreCase = true)) return true
    val path = uri.lastPathSegment.orEmpty().lowercase()
    return listOf(".jpg", ".jpeg", ".png", ".webp", ".gif", ".heic", ".heif")
        .any(path::endsWith)
}

private fun copyAssistantImage(context: Context, uri: Uri) {
    runCatching {
        val copyUri = if (uri.scheme == "file") {
            val file = uri.path?.let(::File) ?: error("圖片位置無效")
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } else uri
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(ClipData.newUri(context.contentResolver, "Vocab 圖片", copyUri))
        Toast.makeText(context, "已複製圖片", Toast.LENGTH_SHORT).show()
    }.onFailure {
        Toast.makeText(context, "無法複製圖片", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun AssistantMessageBubble(
    message: AssistantMessage,
    onQuizCompleted: (String, Int, Int) -> Unit,
    onQuizRestart: (String) -> Unit,
    onOpenQuiz: () -> Unit,
    onOpenArticle: () -> Unit,
    onPreviewImage: (Uri) -> Unit
) {
    val user = message.role == "USER"
    val clipboard = LocalClipboardManager.current
    val displayContent = remember(message.content) { readableAssistantContent(message.content) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (user) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (user) 18.dp else 5.dp,
                bottomEnd = if (user) 5.dp else 18.dp
            ),
            color = if (user) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            contentColor = if (user) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(
                when {
                    user -> 0.84f
                    message.kind == "QUIZ" -> 0.62f
                    else -> 0.94f
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(13.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (message.attachmentUris.isNotEmpty()) {
                    AssistantSentAttachments(
                        uris = message.attachmentUris.map(Uri::parse),
                        onPreviewImage = onPreviewImage
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!user && message.kind != "TEXT") {
                        Text(
                            when (message.kind) {
                                "ARTICLE" -> "AI 文章"
                                "CONFUSABLES" -> "易混淆單字"
                                "QUIZ" -> "互動測驗"
                                else -> "操作紀錄"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    if (displayContent.isNotBlank()) {
                        IconButton(
                            onClick = { clipboard.setText(AnnotatedString(displayContent)) },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "複製訊息",
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
                if (displayContent.isNotBlank() && message.kind != "QUIZ") {
                    if (user) Text(displayContent) else RichAssistantContent(displayContent)
                }
                if (message.kind == "QUIZ" && message.payload.isNotBlank()) {
                    AssistantQuizSummary(
                        payload = message.payload,
                        onOpenQuiz = onOpenQuiz,
                        onRestart = {
                            onQuizRestart(message.id)
                            onOpenQuiz()
                        }
                    )
                }
                if (message.kind == "ARTICLE") {
                    Button(onClick = onOpenArticle, modifier = Modifier.fillMaxWidth()) {
                        Text("開始互動閱讀")
                    }
                }
            }
        }
    }
}

private enum class AssistantBlockType {
    HEADING_1, HEADING_2, HEADING_3, BULLET, NUMBERED, MATH, TEXT, SPACE
}

private data class AssistantRichBlock(
    val type: AssistantBlockType,
    val text: String
)

@Composable
private fun RichAssistantContent(content: String) {
    val blocks = remember(content) { parseAssistantRichBlocks(content) }
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        blocks.forEach { block ->
            when (block.type) {
                AssistantBlockType.SPACE -> Spacer(Modifier.height(3.dp))
                AssistantBlockType.HEADING_1 -> Text(
                    richInlineText(block.text, MaterialTheme.colorScheme.primary),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                AssistantBlockType.HEADING_2 -> Text(
                    richInlineText(block.text, MaterialTheme.colorScheme.primary),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                AssistantBlockType.HEADING_3 -> Text(
                    richInlineText(block.text, MaterialTheme.colorScheme.primary),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                AssistantBlockType.BULLET,
                AssistantBlockType.NUMBERED -> Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        if (block.type == AssistantBlockType.BULLET) "•" else block.text.substringBefore(' '),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        richInlineText(
                            if (block.type == AssistantBlockType.NUMBERED) block.text.substringAfter(' ') else block.text,
                            MaterialTheme.colorScheme.primary
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
                AssistantBlockType.MATH -> Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = latexToReadableMath(block.text),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
                AssistantBlockType.TEXT -> Text(
                    richInlineText(block.text, MaterialTheme.colorScheme.primary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AssistantTypingIndicator(detail: String?) {
    var visibleDots by remember { mutableIntStateOf(1) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(320)
            visibleDots = visibleDots % 3 + 1
        }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Surface(
            shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 5.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(3) { index ->
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(
                                alpha = if (index < visibleDots) 1f else 0.25f
                            ),
                            modifier = Modifier.size(7.dp)
                        ) {}
                    }
                }
                Text(
                    detail?.takeIf { it.isNotBlank() } ?: "AI 正在輸入",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun parseAssistantRichBlocks(content: String): List<AssistantRichBlock> {
    val blocks = mutableListOf<AssistantRichBlock>()
    val mathBuffer = StringBuilder()
    var inMathBlock = false
    content.replace("\r\n", "\n").lines().forEach { original ->
        val line = original.trim()
        if (inMathBlock) {
            if (line.endsWith("$$")) {
                mathBuffer.append(line.removeSuffix("$$"))
                blocks += AssistantRichBlock(AssistantBlockType.MATH, mathBuffer.toString().trim())
                mathBuffer.clear()
                inMathBlock = false
            } else {
                if (mathBuffer.isNotEmpty()) mathBuffer.append(' ')
                mathBuffer.append(line)
            }
            return@forEach
        }
        when {
            line.isBlank() -> blocks += AssistantRichBlock(AssistantBlockType.SPACE, "")
            line.startsWith("$$") && !line.removePrefix("$$").contains("$$") -> {
                inMathBlock = true
                mathBuffer.append(line.removePrefix("$$"))
            }
            line.startsWith("$$") && line.endsWith("$$") -> blocks += AssistantRichBlock(
                AssistantBlockType.MATH,
                line.removePrefix("$$").removeSuffix("$$")
            )
            line.startsWith("\\[") && line.endsWith("\\]") -> blocks += AssistantRichBlock(
                AssistantBlockType.MATH,
                line.removePrefix("\\[").removeSuffix("\\]")
            )
            line.startsWith("### ") -> blocks += AssistantRichBlock(AssistantBlockType.HEADING_3, line.removePrefix("### "))
            line.startsWith("## ") -> blocks += AssistantRichBlock(AssistantBlockType.HEADING_2, line.removePrefix("## "))
            line.startsWith("# ") -> blocks += AssistantRichBlock(AssistantBlockType.HEADING_1, line.removePrefix("# "))
            line.startsWith("- ") || line.startsWith("* ") -> blocks += AssistantRichBlock(AssistantBlockType.BULLET, line.drop(2))
            line.matches(Regex("^\\d+[.)]\\s+.*")) -> blocks += AssistantRichBlock(AssistantBlockType.NUMBERED, line)
            line.length > 48 && line.contains('=') && (line.contains('$') || line.contains('\\')) -> blocks += AssistantRichBlock(
                AssistantBlockType.MATH,
                line.trim('$')
            )
            else -> blocks += AssistantRichBlock(AssistantBlockType.TEXT, line)
        }
    }
    if (mathBuffer.isNotBlank()) blocks += AssistantRichBlock(AssistantBlockType.MATH, mathBuffer.toString())
    return blocks
}

private fun richInlineText(value: String, mathColor: Color): AnnotatedString = buildAnnotatedString {
    var index = 0
    while (index < value.length) {
        when {
            value.startsWith("**", index) -> {
                val end = value.indexOf("**", index + 2)
                if (end > index) {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(value.substring(index + 2, end))
                    pop()
                    index = end + 2
                } else append(value[index++])
            }
            value[index] == '$' -> {
                val end = value.indexOf('$', index + 1)
                if (end > index) {
                    pushStyle(
                        SpanStyle(
                            color = mathColor,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    append(latexToReadableMath(value.substring(index + 1, end)))
                    pop()
                    index = end + 1
                } else append(value[index++])
            }
            else -> {
                val nextBold = value.indexOf("**", index).takeIf { it >= 0 } ?: value.length
                val nextMath = value.indexOf('$', index).takeIf { it >= 0 } ?: value.length
                val end = minOf(nextBold, nextMath)
                append(value.substring(index, end))
                index = end
            }
        }
    }
}

private fun latexToReadableMath(source: String): String {
    var value = source.trim()
    val replacements = linkedMapOf(
        "\\alpha" to "α", "\\beta" to "β", "\\gamma" to "γ", "\\delta" to "δ",
        "\\theta" to "θ", "\\lambda" to "λ", "\\mu" to "μ", "\\pi" to "π",
        "\\sigma" to "σ", "\\phi" to "φ", "\\omega" to "ω", "\\Delta" to "Δ",
        "\\oplus" to "⊕", "\\ominus" to "⊖", "\\times" to "×", "\\cdot" to "·",
        "\\pm" to "±", "\\rightarrow" to "→", "\\leftarrow" to "←", "\\ge" to "≥",
        "\\le" to "≤", "\\neq" to "≠", "\\infty" to "∞", "\\approx" to "≈"
    )
    replacements.forEach { (latex, symbol) -> value = value.replace(latex, symbol) }
    value = Regex("\\\\sqrt\\{([^{}]+)}").replace(value) { "√(${it.groupValues[1]})" }
    value = Regex("\\\\frac\\{([^{}]+)}\\{([^{}]+)}").replace(value) {
        "(${it.groupValues[1]})/(${it.groupValues[2]})"
    }
    value = Regex("\\\\text\\{([^{}]+)}").replace(value) { it.groupValues[1] }
    value = Regex("\\^\\{([^{}]+)}").replace(value) { toSuperscript(it.groupValues[1]) }
    value = Regex("_\\{([^{}]+)}").replace(value) { toSubscript(it.groupValues[1]) }
    value = Regex("\\^(.)").replace(value) { toSuperscript(it.groupValues[1]) }
    value = Regex("_(.)").replace(value) { toSubscript(it.groupValues[1]) }
    return value.replace("\\,", " ").replace("\\;", " ").replace("{", "").replace("}", "")
}

private fun toSuperscript(value: String): String = value.map { character ->
    mapOf(
        '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴', '5' to '⁵',
        '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹', '+' to '⁺', '-' to '⁻',
        'n' to 'ⁿ', 'i' to 'ⁱ', '(' to '⁽', ')' to '⁾'
    )[character] ?: character
}.joinToString("")

private fun toSubscript(value: String): String = value.map { character ->
    mapOf(
        '0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃', '4' to '₄', '5' to '₅',
        '6' to '₆', '7' to '₇', '8' to '₈', '9' to '₉', '+' to '₊', '-' to '₋',
        '(' to '₍', ')' to '₎'
    )[character] ?: character
}.joinToString("")

private fun readableAssistantContent(value: String): String {
    val text = value.trim()
    if (!text.startsWith("[") && !text.startsWith("{")) return text
    return runCatching {
        val items = if (text.startsWith("[")) JSONArray(text)
        else JSONArray().put(JSONObject(text))
        buildList {
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                val words = item.optJSONArray("words")?.let { array ->
                    buildList {
                        for (wordIndex in 0 until array.length()) {
                            array.optString(wordIndex).trim()
                                .takeIf { it.isNotBlank() }
                                ?.let(::add)
                        }
                    }
                }.orEmpty()
                val title = words.joinToString("／").ifBlank { item.optString("title").trim() }
                val explanation = item.optString("explanation").trim()
                    .ifBlank { item.optString("analysis").trim() }
                listOf(title, explanation)
                    .filter { it.isNotBlank() }
                    .joinToString("\n")
                    .takeIf { it.isNotBlank() }
                    ?.let(::add)
            }
        }.joinToString("\n\n").ifBlank { text }
    }.getOrDefault(text)
}

@Composable
private fun AssistantQuizSummary(
    payload: String,
    onOpenQuiz: () -> Unit,
    onRestart: () -> Unit
) {
    val root = remember(payload) {
        runCatching { JSONObject(payload) }.getOrElse {
            JSONObject()
                .put("questions", runCatching { JSONArray(payload) }.getOrDefault(JSONArray()))
                .put("completed", false)
        }
    }
    val total = root.optJSONArray("questions")?.length() ?: 0
    val completed = root.optBoolean("completed")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (completed) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("測驗完成・${root.optInt("latestCorrect")}/${root.optInt("latestTotal")}", fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onOpenQuiz) { Text("查看結果與錯題") }
            TextButton(onClick = onRestart) { Text("重新測驗") }
        } else {
            Text("AI 測驗・共 $total 題", style = MaterialTheme.typography.bodySmall)
            Button(onClick = onOpenQuiz, modifier = Modifier.fillMaxWidth()) { Text("進入測驗") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssistantQuizScreen(
    message: AssistantMessage,
    onCompleted: (String, Int, Int) -> Unit,
    onRestart: (String) -> Unit,
    onProgress: (String, Int, Int, Int, Map<Int, Int>) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AI 測驗", fontWeight = FontWeight.Bold)
                        Text("完成後會保存到對話紀錄", style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回 AI 對話")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    AssistantInlineQuiz(
                        messageId = message.id,
                        payload = message.payload,
                        onCompleted = onCompleted,
                        onRestart = onRestart,
                        onProgress = onProgress
                    )
                }
            }
        }
    }
}

@Composable
private fun AssistantActionCard(
    action: AssistantPendingAction,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Text(action.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
            Text(action.description, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("取消") }
                Button(onClick = onConfirm, modifier = Modifier.weight(1f)) { Text("確認執行") }
            }
        }
    }
}

@Composable
private fun AssistantInlineQuiz(
    messageId: String,
    payload: String,
    onCompleted: (String, Int, Int) -> Unit,
    onRestart: (String) -> Unit,
    onProgress: (String, Int, Int, Int, Map<Int, Int>) -> Unit
) {
    val root = remember(payload) {
        runCatching { JSONObject(payload) }.getOrElse {
            JSONObject()
                .put("questions", runCatching { JSONArray(payload) }.getOrDefault(JSONArray()))
                .put("completed", false)
                .put("attempts", JSONArray())
        }
    }
    val questions = root.optJSONArray("questions") ?: return
    val completed = root.optBoolean("completed")
    if (questions.length() == 0) return
    val completedAt = root.optLong("completedAt")
    val stateKey = "$messageId:$completed:$completedAt"
    val savedWrongAnswers = remember(payload) {
        buildMap {
            val array = root.optJSONArray("wrongAnswers") ?: JSONArray()
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                put(item.optInt("questionIndex"), item.optInt("selectedIndex"))
            }
        }
    }
    var questionIndex by remember(stateKey) {
        mutableIntStateOf(root.optInt("currentIndex", 0).coerceIn(0, questions.length() - 1))
    }
    var selectedIndex by remember(stateKey, questionIndex) {
        mutableStateOf(root.optInt("selectedIndex", -1).takeIf { it >= 0 })
    }
    var correctCount by remember(stateKey) { mutableIntStateOf(root.optInt("correctCount", 0)) }
    var wrongAnswers by remember(stateKey) { mutableStateOf(savedWrongAnswers) }
    var showWrongAnswers by remember(stateKey) { mutableStateOf(false) }
    if (completed) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("測驗完成", fontWeight = FontWeight.Bold)
            }
            Text(
                "答對 ${root.optInt("latestCorrect")}/${root.optInt("latestTotal")} 題",
                style = MaterialTheme.typography.bodyMedium
            )
            if (completedAt > 0L) {
                Text(
                    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(completedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (savedWrongAnswers.isNotEmpty()) {
                    OutlinedButton(onClick = { showWrongAnswers = !showWrongAnswers }) {
                        Text(if (showWrongAnswers) "收起錯題" else "查看錯題")
                    }
                }
                OutlinedButton(onClick = { onRestart(messageId) }) {
                    Text("重新測驗")
                }
            }
            if (showWrongAnswers) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(savedWrongAnswers.entries.toList(), key = { it.key }) { wrong ->
                        val wrongQuestion = questions.optJSONObject(wrong.key) ?: return@items
                        val wrongOptions = wrongQuestion.optJSONArray("options") ?: return@items
                        val selectedText = wrongOptions.optJSONObject(wrong.value)?.optString("text").orEmpty()
                        val correctText = buildList {
                            for (optionIndex in 0 until wrongOptions.length()) {
                                val option = wrongOptions.optJSONObject(optionIndex) ?: continue
                                if (option.optBoolean("correct")) add(option.optString("text"))
                            }
                        }.joinToString("／")
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("第 ${wrong.key + 1} 題　${wrongQuestion.optString("prompt")}", fontWeight = FontWeight.Bold)
                                Text("你的答案：$selectedText", color = MaterialTheme.colorScheme.error)
                                Text("正確答案：$correctText")
                                wrongQuestion.optString("explanation").takeIf { it.isNotBlank() }?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
        return
    }
    val question = questions.optJSONObject(questionIndex) ?: return
    val options = question.optJSONArray("options") ?: return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("${questionIndex + 1} / ${questions.length()}　${question.optString("prompt")}", fontWeight = FontWeight.Bold)
        for (index in 0 until options.length()) {
            val option = options.optJSONObject(index) ?: continue
            val selected = selectedIndex == index
            val correct = option.optBoolean("correct")
            OutlinedButton(
                onClick = {
                    if (selectedIndex == null) {
                        selectedIndex = index
                        val updatedCorrectCount = correctCount + if (correct) 1 else 0
                        val updatedWrongAnswers = if (correct) {
                            wrongAnswers - questionIndex
                        } else {
                            wrongAnswers + (questionIndex to index)
                        }
                        correctCount = updatedCorrectCount
                        wrongAnswers = updatedWrongAnswers
                        onProgress(
                            messageId,
                            questionIndex,
                            index,
                            updatedCorrectCount,
                            updatedWrongAnswers
                        )
                    }
                },
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = when {
                        selectedIndex != null && correct -> MaterialTheme.colorScheme.primaryContainer
                        selected && !correct -> MaterialTheme.colorScheme.errorContainer
                        else -> Color.Transparent
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(option.optString("text"), modifier = Modifier.fillMaxWidth())
            }
        }
        selectedIndex?.let {
            Text(question.optString("explanation"), style = MaterialTheme.typography.bodySmall)
            Button(
                onClick = {
                    if (questionIndex == questions.length() - 1) {
                        onCompleted(messageId, correctCount, questions.length())
                    } else {
                        val nextIndex = questionIndex + 1
                        questionIndex = nextIndex
                        selectedIndex = null
                        onProgress(messageId, nextIndex, -1, correctCount, wrongAnswers)
                    }
                },
                modifier = Modifier.align(Alignment.End)
            ) { Text(if (questionIndex == questions.length() - 1) "完成" else "下一題") }
        }
    }
}

private tailrec fun Context.findAssistantActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findAssistantActivity()
    else -> null
}
