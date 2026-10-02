package com.example.ui.screens
import com.example.ui.components.ModernButton as Button
import com.example.ui.components.ModernOutlinedButton as OutlinedButton
import com.example.ui.components.ModernCard as Card
import com.example.ui.components.ModernListRow
import com.example.ui.components.ModernTextField as OutlinedTextField
import com.example.ui.components.ModernAlertDialog as AlertDialog

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.data.assistant.AssistantMessage
import com.example.data.entity.Deck
import com.example.data.entity.GrammarWritingIssue
import com.example.data.entity.GrammarWritingRecord
import com.example.data.entity.GrammarWritingScanResult
import com.example.ui.components.CalmEmptyState
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReadingLibraryScreen(
    decks: List<Deck>,
    articleMessages: List<AssistantMessage>,
    onGenerate: (Deck) -> Unit,
    onGenerateTopic: (String) -> Unit,
    onImport: (String) -> Unit,
    onRecognizeSources: suspend (List<Uri>) -> GrammarWritingScanResult,
    onOpenArticle: (AssistantMessage) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var chooseDeck by remember { mutableStateOf(false) }
    var showGenerate by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf("") }
    var importBusy by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf<String?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    fun recognize(uris: List<Uri>) {
        if (uris.isEmpty()) return
        scope.launch {
            importBusy = true
            importError = null
            runCatching { onRecognizeSources(uris) }
                .onSuccess { importText = it.recognizedText }
                .onFailure { importError = it.message ?: "無法辨識文章" }
            importBusy = false
        }
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents(), ::recognize)
    val files = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments(), ::recognize)
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) pendingCameraUri?.let { recognize(listOf(it)) } }
    val scanner = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            runCatching { GmsDocumentScanningResult.fromActivityResultIntent(result.data)?.pages?.map { it.imageUri }.orEmpty() }
                .onSuccess(::recognize).onFailure { importError = "無法讀取掃描結果，請改用相片" }
        }
    }
    fun takePhoto() {
        val file = File(context.cacheDir, "reading_${System.currentTimeMillis()}.jpg")
        pendingCameraUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        camera.launch(pendingCameraUri!!)
    }
    fun scanDocument() {
        val activity = context.readingActivity() ?: return
        val options = GmsDocumentScannerOptions.Builder().setGalleryImportAllowed(true).setPageLimit(10)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL).build()
        runCatching { GmsDocumentScanning.getClient(options).getStartScanIntent(activity) }
            .onFailure { importError = it.message ?: "此裝置不支援掃描服務" }
            .getOrNull()?.addOnSuccessListener { scanner.launch(IntentSenderRequest.Builder(it).build()) }
            ?.addOnFailureListener { importError = it.message ?: "無法開啟掃描器" }
    }
    if (chooseDeck) AlertDialog(
        onDismissRequest = { chooseDeck = false },
        title = { Text("用哪個資料夾產生文章？") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(decks) { deck ->
                    Surface(
                        onClick = { chooseDeck = false; onGenerate(deck) },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(deck.name, fontWeight = FontWeight.Bold)
                            Text(deck.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = { chooseDeck = false }) { Text("取消") } }
    )
    if (showGenerate) AlertDialog(
        onDismissRequest = { showGenerate = false },
        title = { Text("AI 產生文章") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("可以從單字資料夾產生，也可以直接輸入想閱讀的主題。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = { showGenerate = false; chooseDeck = true }, enabled = decks.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Folder, null); Spacer(Modifier.width(6.dp)); Text("從單字資料夾產生")
                }
                OutlinedTextField(topic, { topic = it }, label = { Text("自訂主題、程度或文章類型") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(enabled = topic.isNotBlank(), onClick = { val value = topic.trim(); topic = ""; showGenerate = false; onGenerateTopic(value) }) { Text("依主題產生") } },
        dismissButton = { TextButton(onClick = { showGenerate = false }) { Text("取消") } }
    )
    if (showImport) AlertDialog(
        onDismissRequest = { showImport = false },
        title = { Text("匯入文章") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("選擇匯入方式，辨識後可先校對文字，再建立互動文章。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    FilledTonalIconButton(enabled = !importBusy, onClick = ::takePhoto) { Icon(Icons.Default.CameraAlt, "拍照") }
                    FilledTonalIconButton(enabled = !importBusy, onClick = ::scanDocument) { Icon(Icons.Default.DocumentScanner, "掃描") }
                    FilledTonalIconButton(enabled = !importBusy, onClick = { gallery.launch("image/*") }) { Icon(Icons.Default.PhotoLibrary, "相片") }
                    FilledTonalIconButton(enabled = !importBusy, onClick = { files.launch(arrayOf("image/*", "application/pdf", "text/plain", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")) }) { Icon(Icons.Default.FileOpen, "檔案") }
                }
                if (importBusy) Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(22.dp)); Spacer(Modifier.width(8.dp)); Text("正在辨識文章…") }
                importError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                OutlinedTextField(importText, { importText = it }, minLines = 6, label = { Text("文章或網址") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(enabled = importText.isNotBlank() && !importBusy, onClick = { val value = importText.trim(); showImport = false; importText = ""; onImport(value) }) { Text("建立文章") } },
        dismissButton = { TextButton(onClick = { showImport = false }) { Text("取消") } }
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("文章閱讀", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("把正在讀的文章放在最前面，點文字即可查詢與建立單字卡。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { showGenerate = true }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.AutoAwesome, null); Text("AI 產生") }
                OutlinedButton(onClick = { showImport = true }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.FileOpen, null); Text("匯入文章") }
            }
        }
        item { Text("我的文章", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        if (articleMessages.isEmpty()) item { CalmEmptyState(Icons.Default.Article, "還沒有文章", "可由單字資料夾產生文章，或匯入自己的內容。") }
        itemsIndexed(articleMessages, key = { _, item -> item.id }) { index, message ->
ModernListRow(articleTitle(message), "${dateText(message.createdAt)} · " + message.content.replace('\n', ' ').take(100), Icons.Default.Article, { onOpenArticle(message) })
        }
    }
}

@Composable
fun WritingLibraryScreen(
    records: List<GrammarWritingRecord>,
    onNew: () -> Unit,
    onImport: () -> Unit,
    onOpen: (GrammarWritingRecord) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("作文練習", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("保留原稿、修改版本與逐項說明，讓每次修改都能變成弱點練習。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onNew, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Add, null); Text("自由寫作") }
                OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f)) { Icon(Icons.Default.CameraAlt, null); Text("匯入作文") }
            }
        }
        item { Text("作文紀錄", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        if (records.isEmpty()) item { CalmEmptyState(Icons.Default.EditNote, "還沒有作文", "新增作文或拍照匯入，AI 批改前會先讓你確認辨識文字。") }
        itemsIndexed(records, key = { _, item -> item.id }) { index, record ->
ModernListRow(record.title, "${record.course} · ${issueCount(record)} 項批改 · ${dateText(record.updatedAt)}", Icons.Default.EditNote, { onOpen(record) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritingRecordDetailScreen(
    record: GrammarWritingRecord,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var tab by rememberSaveable(record.id) { mutableIntStateOf(0) }
    var confirmDelete by remember { mutableStateOf(false) }
    val issues = remember(record.issuesJson) { writingIssues(record.issuesJson) }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("刪除作文紀錄？") },
        text = { Text("「${record.title}」的原稿、修正版與完整批改結果都會刪除，且無法復原。") },
        confirmButton = { Button(onClick = { confirmDelete = false; onDelete() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("刪除") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } }
    )
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(record.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                actions = {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "繼續修改") }
                    IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, "刪除") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .72f))) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(record.course, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(dateText(record.updatedAt), style = MaterialTheme.typography.labelMedium)
                        }
                        Text("${record.originalText.split(Regex("\\s+")).count { it.isNotBlank() }} 字 · ${issues.size} 項批改", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("批改總覽", "原始作文", "修改後作文", "逐項批改")) { label ->
                        val index = listOf("批改總覽", "原始作文", "修改後作文", "逐項批改").indexOf(label)
                        FilterChip(selected = tab == index, onClick = { tab = index }, label = { Text(label) })
                    }
                }
            }
            when (tab) {
                0 -> {
                    item { Text("批改摘要", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                    item {
                        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("共發現 ${issues.size} 項可改進內容。", style = MaterialTheme.typography.titleMedium)
                                issues.groupingBy { it.title.ifBlank { "其他" } }.eachCount().forEach { (title, count) -> Text("$title · $count 項") }
                            }
                        }
                    }
                }
                1 -> item { WritingTextCard("原始作文", record.originalText) }
                2 -> item { WritingTextCard("修改後作文", record.revisedText) }
                else -> if (issues.isEmpty()) item { CalmEmptyState(Icons.Default.CheckCircle, "沒有批改項目", "這篇作文沒有保存逐項修改資料。") }
                else items(issues) { issue ->
                    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            AssistChip(onClick = {}, label = { Text(issue.title.ifBlank { "修改建議" }) })
                            Text("原文", style = MaterialTheme.typography.labelMedium); Text(issue.originalSentence.ifBlank { issue.originalText }, color = MaterialTheme.colorScheme.error)
                            Text("建議", style = MaterialTheme.typography.labelMedium); Text(issue.correctedSentence.ifBlank { issue.correctedText }, color = MaterialTheme.colorScheme.primary)
                            if (issue.explanation.isNotBlank()) Text(issue.explanation)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WritingTextCard(title: String, text: String) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private fun writingIssues(raw: String): List<GrammarWritingIssue> = runCatching {
    val array = JSONArray(raw)
    buildList {
        repeat(array.length()) { index ->
            val item = array.optJSONObject(index) ?: return@repeat
            add(GrammarWritingIssue(
                ruleKey = item.optString("ruleKey"), title = item.optString("title"),
                originalSentence = item.optString("originalSentence"), correctedSentence = item.optString("correctedSentence"),
                originalText = item.optString("originalText"), correctedText = item.optString("correctedText"),
                explanation = item.optString("explanation")
            ))
        }
    }
}.getOrDefault(emptyList())

private fun articleTitle(message: AssistantMessage): String = message.content.lineSequence()
    .firstOrNull { it.isNotBlank() }
    ?.removePrefix("#")?.trim()?.take(70)
    ?.ifBlank { null }
    ?: "互動閱讀文章"

private fun issueCount(record: GrammarWritingRecord): Int = runCatching {
    org.json.JSONArray(record.issuesJson).length()
}.getOrDefault(0)

private fun dateText(time: Long): String = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(time))

private tailrec fun Context.readingActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.readingActivity()
    else -> null
}
