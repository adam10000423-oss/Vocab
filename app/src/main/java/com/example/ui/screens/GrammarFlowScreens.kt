package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.data.entity.GrammarDraft
import com.example.data.entity.GrammarQuestion
import com.example.data.entity.GrammarQuizResult
import com.example.data.entity.GrammarPattern
import com.example.data.entity.GrammarExample
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrammarImportScreen(
    onBack: () -> Unit,
    onManual: () -> Unit,
    onAiCreate: () -> Unit,
    onRecognize: suspend (List<Uri>, String) -> List<GrammarDraft>,
    onRecognizeText: suspend (String, String) -> GrammarDraft,
    onImportShare: suspend (Uri) -> List<GrammarDraft>,
    onPreview: (List<GrammarDraft>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var showPasteDialog by remember { mutableStateOf(false) }
    var pastedText by remember { mutableStateOf("") }
    fun process(uris: List<Uri>) {
        if (uris.isEmpty()) return
        scope.launch {
            busy = true; message = null
            runCatching { onRecognize(uris, "ENRICH") }
                .onSuccess(onPreview)
                .onFailure { message = it.message ?: "無法辨識內容" }
            busy = false
        }
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents(), ::process)
    val files = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments(), ::process)
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) pendingCameraUri?.let { process(listOf(it)) } }
    val scanner = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) runCatching { GmsDocumentScanningResult.fromActivityResultIntent(result.data)?.pages?.map { it.imageUri }.orEmpty() }
            .onSuccess(::process).onFailure { message = "無法讀取掃描結果，請重新掃描或改用相片" }
    }
    val shareFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch { busy = true; message = null; runCatching { onImportShare(uri) }.onSuccess(onPreview).onFailure { message = it.message ?: "無法讀取分享檔" }; busy = false }
    }
    fun launchCamera() {
        val file = File(context.cacheDir, "grammar_import_${System.currentTimeMillis()}.jpg")
        pendingCameraUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        camera.launch(pendingCameraUri!!)
    }
    fun launchScanner() {
        val activity = context.grammarActivity() ?: return
        val options = GmsDocumentScannerOptions.Builder().setGalleryImportAllowed(true).setPageLimit(10)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL).build()
        runCatching { GmsDocumentScanning.getClient(options).getStartScanIntent(activity) }
            .onFailure { message = it.message ?: "此裝置不支援掃描服務" }
            .getOrNull()
            ?.addOnSuccessListener { scanner.launch(IntentSenderRequest.Builder(it).build()) }
            ?.addOnFailureListener { message = it.message ?: "無法開啟掃描器" }
    }
    if (showPasteDialog) AlertDialog(
        onDismissRequest = { if (!busy) showPasteDialog = false }, title = { Text("貼上文法內容") },
        text = { OutlinedTextField(pastedText, { pastedText = it }, label = { Text("教材文字") }, minLines = 7) },
        confirmButton = { Button(enabled = pastedText.isNotBlank() && !busy, onClick = { scope.launch { busy = true; message = null; runCatching { onRecognizeText(pastedText, "ENRICH") }.onSuccess { showPasteDialog = false; onPreview(listOf(it)) }.onFailure { message = it.message ?: "無法整理文字" }; busy = false } }) { Text("建立預覽") } },
        dismissButton = { TextButton(onClick = { showPasteDialog = false }) { Text("取消") } }
    )
    Scaffold(
        topBar = { TopAppBar(title = { Text("匯入文法") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }) }
    ) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GrammarSourceButton(Icons.Default.EditNote, "手動輸入", onManual, Modifier.weight(1f))
                    GrammarSourceButton(Icons.Default.AutoAwesome, "AI 建立", onAiCreate, Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(onClick = ::launchCamera, modifier = Modifier.size(54.dp)) { Icon(Icons.Default.CameraAlt, "拍照") }
                    FilledTonalIconButton(onClick = ::launchScanner, modifier = Modifier.size(54.dp)) { Icon(Icons.Default.DocumentScanner, "掃描") }
                    FilledTonalIconButton(onClick = { gallery.launch("image/*") }, modifier = Modifier.size(54.dp)) { Icon(Icons.Default.PhotoLibrary, "圖片") }
                    GrammarSourceButton(Icons.Default.FileOpen, "檔案", { files.launch(arrayOf("image/*", "application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "text/plain")) }, Modifier.weight(1f))
            } }
            item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GrammarSourceButton(Icons.Default.ContentPaste, "貼上文字", { showPasteDialog = true }, Modifier.weight(1f))
                GrammarSourceButton(Icons.Default.Share, ".vocabshare", { shareFile.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) }, Modifier.weight(1f))
            } }
            if (busy) item { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(24.dp)); Spacer(Modifier.width(12.dp)); Text("AI 正在辨識、整理並補齊文法…") } } }
            message?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        }
    }
}

@Composable
private fun GrammarSourceButton(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledTonalButton(onClick = onClick, shape = RoundedCornerShape(14.dp), modifier = modifier.height(54.dp)) { Icon(icon, null); Spacer(Modifier.width(7.dp)); Text(text, maxLines = 1) }
}

@Composable
private fun GrammarDropdownAnchor(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.heightIn(min = 64.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, maxLines = 1)
                Text(value, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
    }
}

@Composable
private fun RecognitionOption(value: String, selected: String, title: String, subtitle: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = value == selected, onClick = { onSelect(value) })
        Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrammarNoteDetailScreen(
    note: com.example.data.entity.GrammarNote,
    patterns: List<GrammarPattern>,
    examples: List<GrammarExample>,
    questionCount: Int,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onFavorite: () -> Unit,
    onSpeak: (String) -> Unit,
    onLearn: () -> Unit,
    onQuiz: () -> Unit
) {
    var showMistakes by remember { mutableStateOf(false) }
    var showComparison by remember { mutableStateOf(false) }
    var showWhy by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("刪除文法筆記？") }, text = { Text("筆記、題目與弱點紀錄會一併刪除，且無法復原。") }, confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("刪除", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } })
    Scaffold(
        topBar = { TopAppBar(title = { Text(note.title, maxLines = 1, overflow = TextOverflow.Ellipsis) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }, actions = { IconButton(onClick = onFavorite) { Icon(if (note.favorite) Icons.Default.Star else Icons.Default.StarBorder, "收藏") }; IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "編輯") }; IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, "刪除") } }) },
        bottomBar = { Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) { OutlinedButton(onClick = onLearn, modifier = Modifier.weight(1f)) { Text("開始學習") }; Button(onClick = onQuiz, enabled = questionCount > 0, modifier = Modifier.weight(1f)) { Text("開始測驗") } } }
    ) { padding -> LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { AssistChip(onClick = {}, label = { Text(note.course) }); note.tags.split(',', '、').firstOrNull { it.isNotBlank() }?.let { AssistChip(onClick = {}, label = { Text(it.trim()) }) }; Text("熟練度 ${note.masteryPercent}%", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterVertically)) } }
        if (note.summary.isNotBlank()) item { GrammarPanel("核心概念") { Text(note.summary) } }
        if (patterns.isNotEmpty()) itemsIndexed(patterns, key = { _, pattern -> pattern.id }) { index, pattern ->
            GrammarPanel(pattern.title.ifBlank { "句型 ${index + 1}" }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(pattern.formula, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("${pattern.masteryPercent}%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (pattern.meaning.isNotBlank()) Text(pattern.meaning)
                if (pattern.usage.isNotBlank()) Text("使用時機\n${pattern.usage}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                examples.filter { it.grammarPatternId == pattern.id }.forEach { example ->
                    HorizontalDivider()
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text(example.sentence)
                            if (example.translation.isNotBlank()) Text(example.translation, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { onSpeak(listOf(example.sentence, example.translation).filter(String::isNotBlank).joinToString("。")) }) { Icon(Icons.Default.VolumeUp, "播放例句") }
                    }
                }
                if (pattern.notes.isNotBlank()) Text(pattern.notes, style = MaterialTheme.typography.bodySmall)
            }
        } else {
            if (note.structure.isNotBlank()) item { GrammarPanel("句型") { Text(note.structure) } }
            if (note.exampleSentence.isNotBlank()) item { GrammarPanel("例句") { Text(note.exampleSentence); if (note.exampleTranslation.isNotBlank()) Text(note.exampleTranslation, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        }
        if (note.commonMistakes.isNotBlank()) item { ExpandableGrammarPanel("常見錯誤", showMistakes, { showMistakes = !showMistakes }, note.commonMistakes) }
        if (note.comparison.isNotBlank()) item { ExpandableGrammarPanel("容易混淆", showComparison, { showComparison = !showComparison }, note.comparison) }
        item { Spacer(Modifier.height(70.dp)) }
    } }
}

@Composable private fun GrammarPanel(title: String, content: @Composable ColumnScope.() -> Unit) { Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(title, fontWeight = FontWeight.Bold); content() } } }
@Composable private fun ExpandableGrammarPanel(title: String, expanded: Boolean, onToggle: () -> Unit, text: String) { Card(onClick = onToggle, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Row { Text(title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, if (expanded) "收起" else "展開") }; if (expanded) Text(text) } } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrammarImportPreviewScreen(
    initialDrafts: List<GrammarDraft>, courses: List<String>, onBack: () -> Unit,
    onSave: (List<GrammarDraft>, String, String) -> Unit
) {
    val drafts = remember(initialDrafts) { mutableStateListOf<GrammarDraft>().apply { addAll(initialDrafts) } }
    val selected = remember(initialDrafts) { mutableStateListOf<Boolean>().apply { repeat(initialDrafts.size) { add(true) } } }
    var course by remember { mutableStateOf(courses.firstOrNull().orEmpty()) }
    var courseMenu by remember { mutableStateOf(false) }
    var duplicateMode by remember { mutableStateOf("OVERWRITE") }
    var duplicateMenu by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var editTitle by remember { mutableStateOf("") }
    var editSummary by remember { mutableStateOf("") }
    val selectedDrafts = drafts.filterIndexed { i, _ -> selected.getOrElse(i) { false } }
    editingIndex?.let { index -> AlertDialog(
        onDismissRequest = { editingIndex = null }, title = { Text("編輯匯入內容") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { OutlinedTextField(editTitle, { editTitle = it }, label = { Text("文法名稱") }); OutlinedTextField(editSummary, { editSummary = it }, label = { Text("核心概念") }, minLines = 3) } },
        confirmButton = { Button(enabled = editTitle.isNotBlank(), onClick = { drafts[index] = drafts[index].copy(title = editTitle.trim(), summary = editSummary); editingIndex = null }) { Text("套用") } },
        dismissButton = { TextButton(onClick = { editingIndex = null }) { Text("取消") } }
    ) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("匯入預覽") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }) },
        bottomBar = { Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) { OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("重新辨識") }; Button(enabled = selectedDrafts.isNotEmpty(), onClick = { onSave(selectedDrafts, course, duplicateMode) }, modifier = Modifier.weight(1f)) { Text("儲存 ${selectedDrafts.size} 篇") } } }
    ) { padding -> LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ExposedDropdownMenuBox(expanded = courseMenu, onExpandedChange = { courseMenu = it }, modifier = Modifier.weight(1f)) {
                GrammarDropdownAnchor("課程", course, Modifier.menuAnchor().fillMaxWidth())
                ExposedDropdownMenu(expanded = courseMenu, onDismissRequest = { courseMenu = false }) { courses.forEach { DropdownMenuItem(text = { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) }, onClick = { course = it; courseMenu = false }) } }
            }
            ExposedDropdownMenuBox(expanded = duplicateMenu, onExpandedChange = { duplicateMenu = it }, modifier = Modifier.weight(1f)) {
                GrammarDropdownAnchor("重複處理", when (duplicateMode) { "SKIP" -> "略過同名文法"; "COPY" -> "另存新筆記"; else -> "覆蓋同名文法" }, Modifier.menuAnchor().fillMaxWidth())
                ExposedDropdownMenu(expanded = duplicateMenu, onDismissRequest = { duplicateMenu = false }) { listOf("OVERWRITE" to "覆蓋同名文法", "SKIP" to "略過同名文法", "COPY" to "另存新筆記").forEach { (v, label) -> DropdownMenuItem(text = { Text(label) }, onClick = { duplicateMode = v; duplicateMenu = false }) } }
            }
        } }
        itemsIndexed(drafts) { index, draft -> Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Checkbox(selected[index], { selected[index] = it }); Column(Modifier.weight(1f)) { Text(draft.title.ifBlank { "未命名文法" }, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("${draft.patterns.size} 組句型 · ${draft.patterns.sumOf { it.examples.size }} 個例句 · ${draft.questions.size} 題", color = MaterialTheme.colorScheme.onSurfaceVariant); AssistChip(onClick = {}, label = { Text("AI 已整理") }) }; IconButton(onClick = { editingIndex = index; editTitle = draft.title; editSummary = draft.summary }) { Icon(Icons.Default.Edit, "編輯") } } } }
        item { Spacer(Modifier.height(70.dp)) }
    } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrammarQuizResultScreen(noteTitle: String, result: GrammarQuizResult, questions: List<GrammarQuestion>, onBack: () -> Unit, onPracticeWrong: () -> Unit, onRetryAll: () -> Unit) {
    val wrong = questions.filter { it.id in result.wrongQuestionIds }.sortedByDescending { result.wrongCounts[it.id] ?: 0 }
    var showWrongDetails by remember { mutableStateOf(false) }
    Scaffold(topBar = { TopAppBar(title = { Text("測驗完成") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.Close, "關閉") } }) }) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("本次正確率", color = MaterialTheme.colorScheme.onSurfaceVariant); Text("${result.accuracyPercent}%", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold); Text("首次答對 ${result.firstTryCorrect}／${result.totalQuestions} · 共作答 ${result.totalAttempts} 次") } } }
            item { Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("熟練度變化", fontWeight = FontWeight.Bold); Row { Text(noteTitle, modifier = Modifier.weight(1f)); Text("${result.masteryBefore}% → ${result.masteryAfter}%", fontWeight = FontWeight.Bold) }; LinearProgressIndicator(progress = { result.masteryAfter / 100f }, modifier = Modifier.fillMaxWidth()); Text("依首次答對與本次錯題計算。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
            if (wrong.isNotEmpty()) item { Text("需要加強", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            if (showWrongDetails) itemsIndexed(wrong) { _, q -> Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(q.prompt.replace("{{answer}}", "____"), maxLines = 2); Text("正確答案：${q.answer}"); Text("錯 ${result.wrongCounts[q.id] ?: 1} 次", color = MaterialTheme.colorScheme.error) } } } }
            item { if (wrong.isNotEmpty()) { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { OutlinedButton(onClick = { showWrongDetails = !showWrongDetails }, modifier = Modifier.weight(1f)) { Text(if (showWrongDetails) "收起錯題" else "查看錯題") }; Button(onClick = onPracticeWrong, modifier = Modifier.weight(1f)) { Text("弱點練習") } } } else Button(onClick = onRetryAll, modifier = Modifier.fillMaxWidth()) { Text("再次測驗") } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrammarQuizFlowScreen(
    noteId: Long,
    noteTitle: String,
    masteryBefore: Int,
    questions: List<GrammarQuestion>,
    onAnswer: (GrammarQuestion, Boolean, Boolean) -> Unit,
    onBack: () -> Unit,
    onComplete: (GrammarQuizResult) -> Unit
) {
    val original = remember(noteId, questions) { questions.sortedBy { it.sortOrder } }
    var queue by remember(noteId, questions) { mutableStateOf(original) }
    var index by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf("") }
    var input by remember { mutableStateOf("") }
    var checked by remember { mutableStateOf(false) }
    var correct by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf(false) }
    var totalAttempts by remember { mutableIntStateOf(0) }
    var firstTryCorrect by remember { mutableIntStateOf(0) }
    val attempted = remember { mutableStateListOf<Long>() }
    val wrongCounts = remember { mutableStateMapOf<Long, Int>() }
    val wrongQueue = remember { mutableStateListOf<GrammarQuestion>() }
    val question = queue.getOrNull(index)
    fun clearResponse() { selected = ""; input = ""; checked = false; correct = false; hint = false }
    fun finish() {
        val penalty = wrongCounts.values.sum() * 3
        val reward = firstTryCorrect * 5
        onComplete(GrammarQuizResult(noteId, original.size, firstTryCorrect, totalAttempts, wrongCounts.keys.toList(), wrongCounts.toMap(), masteryBefore, (masteryBefore + reward - penalty).coerceIn(0, 100)))
    }
    Scaffold(
        topBar = { TopAppBar(title = { Text("文法測驗") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.Close, "退出") } }, actions = { Text("${(index + 1).coerceAtMost(queue.size)}／${queue.size}", modifier = Modifier.padding(end = 16.dp)) }) },
        bottomBar = { if (question != null) Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(enabled = !checked, onClick = { hint = true }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Lightbulb, null); Spacer(Modifier.width(5.dp)); Text("提示") }
            val choices = question.options.split(',', '、', '\n').map(String::trim).filter(String::isNotBlank)
            val response = if (choices.isNotEmpty()) selected else input
            Button(enabled = response.isNotBlank(), onClick = {
                if (!checked) {
                    val accepted = (question.acceptedAnswers.split(',', '、') + question.answer).map { it.trim().lowercase() }.filter(String::isNotBlank).toSet()
                    correct = response.trim().lowercase() in accepted
                    checked = true; totalAttempts++; onAnswer(question, correct, hint)
                    if (question.id !in attempted) { attempted += question.id; if (correct) firstTryCorrect++ }
                    if (!correct) { wrongCounts[question.id] = (wrongCounts[question.id] ?: 0) + 1; if (wrongQueue.none { it.id == question.id }) wrongQueue += question }
                } else if (!correct) clearResponse()
                else if (index < queue.lastIndex) { index++; clearResponse() }
                else if (wrongQueue.isNotEmpty()) { queue = wrongQueue.toList(); wrongQueue.clear(); index = 0; clearResponse() }
                else finish()
            }, modifier = Modifier.weight(1f)) { Text(if (!checked) "確認答案" else if (correct) "下一題" else "再答一次") }
        } }
    ) { padding ->
        if (question == null) Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { Text("這篇文法尚未建立練習題") }
        else {
            val choices = question.options.split(',', '、', '\n').map(String::trim).filter(String::isNotBlank).distinct().take(4)
            Column(Modifier.padding(padding).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LinearProgressIndicator(progress = { (index + 1f) / queue.size.coerceAtLeast(1) }, modifier = Modifier.fillMaxWidth())
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { AssistChip(onClick = {}, label = { Text(if (choices.isEmpty()) "填空" else "選擇") }); Text(question.prompt.replace("{{answer}}", "____"), style = MaterialTheme.typography.titleLarge); if (question.translation.isNotBlank()) Text(question.translation, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                if (choices.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    choices.forEach { choice ->
                        FilterChip(
                            selected = selected == choice,
                            enabled = !checked,
                            onClick = { selected = choice },
                            label = { Text(choice, modifier = Modifier.padding(vertical = 5.dp), style = MaterialTheme.typography.titleMedium) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                else OutlinedTextField(input, { input = it }, enabled = !checked, label = { Text("輸入完整答案") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (hint && !checked) Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) { Row(Modifier.padding(14.dp)) { Icon(Icons.Default.Lightbulb, null); Spacer(Modifier.width(8.dp)); Text(question.explanation.ifBlank { "想想這個時間或語境需要使用哪一種文法形式。" }) } }
                if (checked) Card(colors = CardDefaults.cardColors(containerColor = if (correct) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer)) { Column(Modifier.fillMaxWidth().padding(14.dp)) { Text(if (correct) "答對了" else "答錯了", fontWeight = FontWeight.Bold); if (!correct) Text("正確答案：${question.answer}"); if (question.explanation.isNotBlank()) Text(question.explanation) } }
            }
        }
    }
}

private tailrec fun Context.grammarActivity(): Activity? = when (this) { is Activity -> this; is ContextWrapper -> baseContext.grammarActivity(); else -> null }
