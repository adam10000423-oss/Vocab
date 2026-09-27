package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.data.entity.*
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun GrammarDashboardScreen(
    notes: List<GrammarNote>,
    weaknesses: List<GrammarWeakness>,
    onAdd: () -> Unit,
    onImport: () -> Unit,
    onOpen: (GrammarNote) -> Unit,
    onLearn: (GrammarNote) -> Unit,
    onQuiz: (GrammarNote) -> Unit,
    onWritingCheck: () -> Unit,
    onSeeAllNotes: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val now = System.currentTimeMillis()
    val due = notes.filter { it.nextReviewAt <= now }
    val recentNotes = remember(notes) { notes.sortedByDescending { it.updatedAt }.take(4) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Hero Progress Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("今日文法複習", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("待複習 ${due.size} 個文法", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Text("共 ${notes.size} 篇筆記", color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    }
                }
            }
        }

        item {
            Button(
                enabled = notes.isNotEmpty(),
                onClick = { (due.firstOrNull() ?: notes.firstOrNull())?.let(onLearn) },
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(
                    if (due.isNotEmpty()) "開始複習（${due.size}）" else "開始文法學習",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Keep the same calm two-column action layout used by the word dashboard.
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(onClick = onAdd, shape = RoundedCornerShape(18.dp), modifier = Modifier.weight(1f).height(54.dp)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(5.dp)); Text("新增文法") }
                    FilledTonalButton(onClick = onImport, shape = RoundedCornerShape(18.dp), modifier = Modifier.weight(1f).height(54.dp)) { Icon(Icons.Default.DocumentScanner, null); Spacer(Modifier.width(5.dp)); Text("匯入") }
                }
                OutlinedButton(onClick = onWritingCheck, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().height(54.dp)) { Icon(Icons.Default.Spellcheck, null); Spacer(Modifier.width(5.dp)); Text("寫作檢查") }
            }
        }

        // Weakness Summary Alert
        if (weaknesses.isNotEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("累積寫作弱點 ${weaknesses.size} 項", fontWeight = FontWeight.Bold)
                            Text(weaknesses.take(2).joinToString("、") { it.title }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        TextButton(onClick = onWritingCheck) {
                            Text("寫作練習")
                        }
                    }
                }
            }
        }

        // Recent Notes Header & List Preview
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("最近研讀筆記", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = onSeeAllNotes) {
                    Text("查看全部 (${notes.size}) 篇 ➔")
                }
            }
        }

        if (recentNotes.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                    Column(
                        Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Book, null, tint = MaterialTheme.colorScheme.primary)
                        Text("尚未建立任何文法筆記", fontWeight = FontWeight.Bold)
                        Text("點擊「新增文法」或上傳教材照片，AI 會為您自動整理筆記與練習題。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = onAdd) { Text("建立第一篇文法筆記") }
                    }
                }
            }
        } else {
            items(recentNotes, key = { it.id }) { note ->
                GrammarNoteCard(note = note, now = now, onOpen = onOpen)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrammarNotesScreen(
    notes: List<GrammarNote>,
    onAdd: () -> Unit,
    onImport: () -> Unit,
    onReorderNotes: (List<Long>) -> Unit,
    onOpen: (GrammarNote) -> Unit,
    modifier: Modifier = Modifier
) {
    val now = System.currentTimeMillis()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val courses = remember(notes) { notes.map { it.course }.filter { it.isNotBlank() }.distinct() }
    val grammarTypes = remember(notes) { notes.map { it.title }.filter { it.isNotBlank() }.distinct() }
    var selectedCourse by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedType by rememberSaveable { mutableStateOf<String?>(null) }
    var courseMenu by remember { mutableStateOf(false) }
    var typeMenu by remember { mutableStateOf(false) }

    val filteredNotes = remember(notes, searchQuery, selectedCourse, selectedType) {
        notes.filter { note ->
            val matchCourse = selectedCourse == null || note.course.equals(selectedCourse, ignoreCase = true)
            val matchType = selectedType == null || note.title.equals(selectedType, ignoreCase = true)
            val matchQuery = searchQuery.isBlank() ||
                note.title.contains(searchQuery, ignoreCase = true) ||
                note.summary.contains(searchQuery, ignoreCase = true) ||
                note.structure.contains(searchQuery, ignoreCase = true) ||
                note.course.contains(searchQuery, ignoreCase = true)
            matchCourse && matchType && matchQuery
        }
    }
    val reorderEnabled = searchQuery.isBlank() && selectedType == null
    val sourceNoteIds = if (reorderEnabled) filteredNotes.map { it.id } else emptyList()
    var displayedNoteIds by remember(sourceNoteIds) { mutableStateOf(sourceNoteIds) }
    val displayedNotes = if (reorderEnabled) {
        displayedNoteIds.mapNotNull { id -> filteredNotes.firstOrNull { it.id == id } }
    } else filteredNotes
    val noteListState = rememberLazyListState()
    var noteOrderChanged by remember { mutableStateOf(false) }
    val noteReorderState = rememberReorderableLazyListState(noteListState) { from, to ->
        // Search, filters and action buttons occupy the first three positions.
        if (reorderEnabled && displayedNoteIds.isNotEmpty()) {
            val fromIndex = (from.index - 3).coerceIn(displayedNoteIds.indices)
            val toIndex = (to.index - 3).coerceIn(displayedNoteIds.indices)
            if (fromIndex != toIndex) {
                displayedNoteIds = displayedNoteIds.toMutableList().apply {
                    add(toIndex, removeAt(fromIndex))
                }
                noteOrderChanged = true
            }
        }
    }

    LaunchedEffect(noteReorderState.isAnyItemDragging) {
        if (!noteReorderState.isAnyItemDragging && noteOrderChanged) {
            onReorderNotes(displayedNoteIds)
            noteOrderChanged = false
        }
    }

    LazyColumn(
        state = noteListState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("搜尋文法或句型", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "清除")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Filters use the same compact two-dropdown structure as folder management.
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ExposedDropdownMenuBox(expanded = courseMenu, onExpandedChange = { courseMenu = it }, modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = selectedCourse ?: "全部課程", onValueChange = {}, readOnly = true,
                        label = { Text("課程") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(courseMenu) },
                        singleLine = true, modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = courseMenu, onDismissRequest = { courseMenu = false }) {
                        DropdownMenuItem(text = { Text("全部課程") }, onClick = { selectedCourse = null; courseMenu = false })
                        courses.forEach { course -> DropdownMenuItem(text = { Text(course) }, onClick = { selectedCourse = course; courseMenu = false }) }
                    }
                }
                ExposedDropdownMenuBox(expanded = typeMenu, onExpandedChange = { typeMenu = it }, modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = selectedType ?: "全部類型", onValueChange = {}, readOnly = true,
                        label = { Text("文法類型") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeMenu) },
                        singleLine = true, modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                        DropdownMenuItem(text = { Text("全部類型") }, onClick = { selectedType = null; typeMenu = false })
                        grammarTypes.forEach { type -> DropdownMenuItem(text = { Text(type, maxLines = 1, overflow = TextOverflow.Ellipsis) }, onClick = { selectedType = type; typeMenu = false }) }
                    }
                }
            }
        }

        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onAdd, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("新增") }
            FilledTonalButton(onClick = onImport, modifier = Modifier.weight(1f)) { Icon(Icons.Default.DocumentScanner, null); Spacer(Modifier.width(6.dp)); Text("匯入") }
        } }

        if (displayedNotes.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                    Column(
                        Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.MenuBook, null, tint = MaterialTheme.colorScheme.primary)
                        Text("未找到符合條件的文法筆記", fontWeight = FontWeight.Bold)
                        Button(onClick = onAdd) { Text("建立新筆記") }
                    }
                }
            }
        } else {
            itemsIndexed(displayedNotes, key = { _, note -> note.id }) { _, note ->
                ReorderableItem(state = noteReorderState, key = note.id) {
                    GrammarNoteCard(
                        note = note,
                        now = now,
                        onOpen = onOpen,
                        showReorder = reorderEnabled,
                        dragHandleModifier = Modifier.draggableHandle()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrammarStudyHubScreen(
    notes: List<GrammarNote>,
    onLearn: (GrammarNote) -> Unit,
    onQuiz: (GrammarNote) -> Unit,
    onOpen: (GrammarNote) -> Unit,
    modifier: Modifier = Modifier
) {
    val now = System.currentTimeMillis()
    val courses = remember(notes) { notes.map { it.course }.filter(String::isNotBlank).distinct() }
    val grammarTypes = remember(notes) { notes.map { it.title }.filter(String::isNotBlank).distinct() }
    var selectedCourse by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedType by rememberSaveable { mutableStateOf<String?>(null) }
    var courseMenu by remember { mutableStateOf(false) }
    var typeMenu by remember { mutableStateOf(false) }
    val filtered = remember(notes, selectedCourse, selectedType) { notes.filter {
        (selectedCourse == null || it.course == selectedCourse) && (selectedType == null || it.title == selectedType)
    } }
    val due = filtered.filter { it.nextReviewAt <= now }
    val nextNote = due.firstOrNull() ?: filtered.firstOrNull()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GrammarFilterMenu("課程", selectedCourse ?: "全部課程", courses, courseMenu, { courseMenu = it }, { selectedCourse = it }, Modifier.weight(1f))
                GrammarFilterMenu("文法類型", selectedType ?: "全部類型", grammarTypes, typeMenu, { typeMenu = it }, { selectedType = it }, Modifier.weight(1f))
            }
        }
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Style, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("文法學習", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text("一次只專注一個概念、句型或例句，進度會自動保存。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(
                        enabled = nextNote != null,
                        onClick = { nextNote?.let(onLearn) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (due.isNotEmpty()) "複習全部 (${due.size})" else "學習全部")
                    }
                }
            }
        }

        item {
            Text("按筆記學習與演練", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (filtered.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                    Column(
                        Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Style, null, tint = MaterialTheme.colorScheme.primary)
                        Text("還沒有可學習的文法筆記", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            items(filtered, key = { it.id }) { note ->
                Card(
                    onClick = { onOpen(note) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        GrammarNoteCardBody(note = note, now = now)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onLearn(note) }, modifier = Modifier.weight(1f)) {
                                Text("學習")
                            }
                            Button(onClick = { onQuiz(note) }, modifier = Modifier.weight(1f)) {
                                Text("測驗")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GrammarNoteCard(
    note: GrammarNote,
    now: Long,
    onOpen: (GrammarNote) -> Unit,
    showReorder: Boolean = false,
    dragHandleModifier: Modifier = Modifier
) {
    Card(onClick = { onOpen(note) }, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { GrammarNoteCardBody(note = note, now = now) }
                if (showReorder) {
                    IconButton(onClick = {}, modifier = dragHandleModifier.size(40.dp)) {
                        Icon(Icons.Default.DragHandle, contentDescription = "拖曳調整文法順序")
                    }
                }
            }
        }
    }
}

@Composable
private fun GrammarNoteCardBody(note: GrammarNote, now: Long) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(note.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(note.structure.ifBlank { note.summary }.ifBlank { "尚未填寫句型" }, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(if (note.nextReviewAt <= now) "待複習" else "${note.masteryPercent}%", color = MaterialTheme.colorScheme.primary)
    }
    LinearProgressIndicator(progress = { note.masteryPercent / 100f }, modifier = Modifier.fillMaxWidth())
    AssistChip(onClick = {}, label = { Text(note.course, maxLines = 1) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GrammarFilterMenu(
    label: String,
    value: String,
    options: List<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = onExpandedChange, modifier = modifier) {
        OutlinedTextField(
            value = value, onValueChange = {}, readOnly = true, label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, singleLine = true,
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            DropdownMenuItem(text = { Text("全部$label") }, onClick = { onSelect(null); onExpandedChange(false) })
            options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); onExpandedChange(false) }) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GrammarEditableMenu(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    onAddRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.filter(String::isNotBlank).distinct().forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    onClick = { onSelect(option); expanded = false }
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("新增$label…") },
                leadingIcon = { Icon(Icons.Default.Add, null) },
                onClick = { expanded = false; onAddRequested() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrammarEditorScreen(note: GrammarNote?, questions: List<GrammarQuestion>, patterns: List<GrammarPattern>, examples: List<GrammarExample>, courses: List<String>, categories: List<String> = emptyList(), levels: List<String> = emptyList(), initialShowAiDialog: Boolean = false, onGenerateAi: suspend (String) -> GrammarDraft, onScanImages: suspend (List<Uri>) -> GrammarDraft, onSave: (GrammarNote, List<GrammarQuestionDraft>, List<GrammarPatternDraft>) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current; val scope = rememberCoroutineScope()
    var course by rememberSaveable(note?.id) { mutableStateOf(note?.course ?: courses.firstOrNull().orEmpty()) }; var title by rememberSaveable(note?.id) { mutableStateOf(note?.title.orEmpty()) }
    val linkedCourses = remember(courses) { (listOf("通用") + courses).filter(String::isNotBlank).distinct() }
    var summary by rememberSaveable(note?.id) { mutableStateOf(note?.summary.orEmpty()) }; var structure by rememberSaveable(note?.id) { mutableStateOf(note?.structure.orEmpty()) }; var usage by rememberSaveable(note?.id) { mutableStateOf(note?.usage.orEmpty()) }
    var example by rememberSaveable(note?.id) { mutableStateOf(note?.exampleSentence.orEmpty()) }; var translation by rememberSaveable(note?.id) { mutableStateOf(note?.exampleTranslation.orEmpty()) }; var mistakes by rememberSaveable(note?.id) { mutableStateOf(note?.commonMistakes.orEmpty()) }; var comparison by rememberSaveable(note?.id) { mutableStateOf(note?.comparison.orEmpty()) }; var tags by rememberSaveable(note?.id) { mutableStateOf(note?.tags.orEmpty()) }
    var category by rememberSaveable(note?.id) { mutableStateOf(note?.category ?: "其他") }
    var level by rememberSaveable(note?.id) { mutableStateOf(note?.level ?: "未分級") }
    val categoryOptions = remember(categories, note?.category) { (listOf("時態", "句型", "介系詞", "連接詞", "語態", "比較", "其他") + categories + note?.category.orEmpty()).filter(String::isNotBlank).distinct() }
    val levelOptions = remember(levels, note?.level) { (listOf("初級", "中級", "中高級", "高級", "未分級") + levels + note?.level.orEmpty()).filter(String::isNotBlank).distinct() }
    var customField by remember { mutableStateOf<String?>(null) }
    var customValue by remember { mutableStateOf("") }
    val patternDrafts = remember(note?.id, patterns, examples) { mutableStateListOf<GrammarPatternDraft>().apply {
        addAll(patterns.map { pattern -> GrammarPatternDraft(
            title = pattern.title, formula = pattern.formula, meaning = pattern.meaning,
            usage = pattern.usage, notes = pattern.notes,
            examples = examples.filter { it.grammarPatternId == pattern.id }.map { GrammarExampleDraft(it.sentence, it.translation, it.highlightedText) }
        ) })
        if (isEmpty() && (note?.structure?.isNotBlank() == true || note?.exampleSentence?.isNotBlank() == true)) add(
            GrammarPatternDraft(title = "主要句型", formula = note.structure, meaning = note.summary, usage = note.usage,
                examples = if (note.exampleSentence.isBlank()) emptyList() else listOf(GrammarExampleDraft(note.exampleSentence, note.exampleTranslation)))
        )
    } }
    val drafts = remember(note?.id, questions, patterns) { mutableStateListOf<GrammarQuestionDraft>().apply { addAll(questions.map { q -> GrammarQuestionDraft(
        id = q.id, grammarPatternIndex = patterns.indexOfFirst { it.id == q.grammarPatternId }, type = q.type,
        prompt = q.prompt, translation = q.translation, answer = q.answer, acceptedAnswers = q.acceptedAnswers,
        options = q.options, explanation = q.explanation, difficulty = q.difficulty, sourceType = q.sourceType
    ) }); if (isEmpty() && note?.questionTemplate?.isNotBlank() == true) add(GrammarQuestionDraft(prompt = note.questionTemplate, translation = note.exampleTranslation, answer = note.answer, acceptedAnswers = note.acceptedAnswers, options = note.options, explanation = note.explanation)) } }
    var aiPrompt by rememberSaveable { mutableStateOf("") }; var showAiDialog by remember { mutableStateOf(initialShowAiDialog) }; var busy by remember { mutableStateOf(false) }; var aiApplied by rememberSaveable(note?.id) { mutableStateOf(false) }; var error by remember { mutableStateOf<String?>(null) }; var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var editorSection by rememberSaveable(note?.id) { mutableIntStateOf(0) }
    var horizontalDrag by remember { mutableFloatStateOf(0f) }
    fun applyDraft(d: GrammarDraft, onlyBlank: Boolean = false) {
        aiApplied = true
        if (!onlyBlank || title.isBlank()) title=d.title
        if (!onlyBlank || category.isBlank() || category == "其他") category=d.category
        if (!onlyBlank || level.isBlank() || level == "未分級") level=d.level
        if (!onlyBlank || summary.isBlank()) summary=d.summary
        if (!onlyBlank || structure.isBlank()) structure=d.structure
        if (!onlyBlank || usage.isBlank()) usage=d.usage
        if (!onlyBlank || example.isBlank()) example=d.exampleSentence
        if (!onlyBlank || translation.isBlank()) translation=d.exampleTranslation
        if (!onlyBlank || mistakes.isBlank()) mistakes=d.commonMistakes
        if (!onlyBlank || comparison.isBlank()) comparison=d.comparison
        if (!onlyBlank || tags.isBlank()) tags=d.tags
        val generatedPatterns=d.patterns.ifEmpty { if (d.structure.isBlank()) emptyList() else listOf(GrammarPatternDraft("主要句型", d.structure, d.summary, d.usage, examples = if (d.exampleSentence.isBlank()) emptyList() else listOf(GrammarExampleDraft(d.exampleSentence, d.exampleTranslation)))) }
        if (!onlyBlank || patternDrafts.isEmpty()) { patternDrafts.clear(); patternDrafts.addAll(generatedPatterns) }
        val generatedQuestions=d.questions.ifEmpty { if (d.questionTemplate.isBlank()) emptyList() else listOf(GrammarQuestionDraft(prompt=d.questionTemplate, translation=d.exampleTranslation, answer=d.answer, acceptedAnswers=d.acceptedAnswers, options=d.options, explanation=d.explanation, sourceType="AI")) }
        if (!onlyBlank || drafts.isEmpty()) { drafts.clear(); drafts.addAll(generatedQuestions) }
    }
    fun importImages(uris: List<Uri>) { if (uris.isEmpty()) return; scope.launch { busy=true; error=null; runCatching { onScanImages(uris) }.onSuccess(::applyDraft).onFailure { error=it.message ?: "辨識失敗" }; busy=false } }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { importImages(it) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { if (it) pendingCameraUri?.let { u -> importImages(listOf(u)) } }
    val scanner = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { r -> if (r.resultCode == Activity.RESULT_OK) importImages(GmsDocumentScanningResult.fromActivityResultIntent(r.data)?.pages?.map { it.imageUri }.orEmpty()) }
    fun launchCamera() { val f=File(context.cacheDir,"grammar_${System.currentTimeMillis()}.jpg"); pendingCameraUri=FileProvider.getUriForFile(context,"${context.packageName}.fileprovider",f); camera.launch(pendingCameraUri!!) }
    fun launchScanner() { val activity=context.findActivity() ?: return; val o=GmsDocumentScannerOptions.Builder().setGalleryImportAllowed(true).setPageLimit(10).setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG).setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL).build(); GmsDocumentScanning.getClient(o).getStartScanIntent(activity).addOnSuccessListener { scanner.launch(IntentSenderRequest.Builder(it).build()) }.addOnFailureListener { error=it.message ?: "無法開啟掃描器" } }
    if (customField != null) AlertDialog(
        onDismissRequest = { customField = null },
        title = { Text("新增${customField}") },
        text = { OutlinedTextField(customValue, { customValue = it }, singleLine = true, label = { Text(customField.orEmpty()) }) },
        confirmButton = { Button(enabled = customValue.isNotBlank(), onClick = {
            when (customField) { "課程" -> course = customValue.trim(); "分類" -> category = customValue.trim(); "程度" -> level = customValue.trim() }
            customValue = ""; customField = null
        }) { Text("套用") } },
        dismissButton = { TextButton(onClick = { customField = null }) { Text("取消") } }
    )
    if (showAiDialog) AlertDialog(onDismissRequest={if(!busy)showAiDialog=false},title={Text("AI 建立或補齊文法")},text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){Text("輸入主題可建立完整內容；已有內容時也能只補齊空白欄位。");OutlinedTextField(aiPrompt,{aiPrompt=it},label={Text("文法主題與程度")},minLines=3);error?.let{Text(it,color=MaterialTheme.colorScheme.error)}}},confirmButton={Column(horizontalAlignment=Alignment.End){Button(enabled=aiPrompt.isNotBlank()&&!busy,onClick={scope.launch{busy=true;error=null;runCatching{onGenerateAi(aiPrompt)}.onSuccess{applyDraft(it,false);showAiDialog=false}.onFailure{error=it.message?:"AI 產生失敗"};busy=false}}){Text(if(busy)"處理中…" else "依主題生成")};TextButton(enabled=aiPrompt.isNotBlank()&&!busy,onClick={scope.launch{busy=true;error=null;runCatching{onGenerateAi(aiPrompt)}.onSuccess{applyDraft(it,true);showAiDialog=false}.onFailure{error=it.message?:"AI 補齊失敗"};busy=false}}){Text("只補齊空白")}}},dismissButton={TextButton(enabled=!busy,onClick={showAiDialog=false}){Text("取消")}})
    Scaffold(topBar={TopAppBar(title={Text(if(note==null)"新增文法" else "編輯文法")},navigationIcon={IconButton(onClick=onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"返回")}},actions={IconButton(onClick={showAiDialog=true}){Icon(Icons.Default.AutoAwesome,"AI 建立")}})},bottomBar={Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)){OutlinedButton(onClick=onBack,modifier=Modifier.weight(1f)){Text("取消")};Button(enabled=title.isNotBlank()&&!busy,onClick={val q=drafts.firstOrNull();val firstPattern=patternDrafts.firstOrNull();val firstExample=firstPattern?.examples?.firstOrNull();onSave((note?:GrammarNote(title=title)).copy(course=course.trim().ifBlank{"通用"},title=title.trim(),category=category,level=level,summary=summary,structure=firstPattern?.formula.orEmpty(),usage=firstPattern?.usage.orEmpty(),exampleSentence=firstExample?.sentence.orEmpty(),exampleTranslation=firstExample?.translation.orEmpty(),commonMistakes=mistakes,comparison=comparison,tags=tags,sourceType=if(note==null&&aiApplied)"AI" else note?.sourceType?:"MANUAL",questionTemplate=q?.prompt.orEmpty(),answer=q?.answer.orEmpty(),acceptedAnswers=q?.acceptedAnswers.orEmpty(),options=q?.options.orEmpty(),explanation=q?.explanation.orEmpty()),drafts.toList(),patternDrafts.toList())},modifier=Modifier.weight(1f)){Text("儲存")}}}) { p ->
        Column(Modifier.padding(p).pointerInput(editorSection){
            detectHorizontalDragGestures(
                onDragStart={ horizontalDrag=0f },
                onHorizontalDrag={ _, amount -> horizontalDrag += amount },
                onDragEnd={ if(abs(horizontalDrag)>100f) editorSection=(editorSection + if(horizontalDrag<0) 1 else -1).coerceIn(0,3) }
            )
        }.verticalScroll(rememberScrollState()).padding(horizontal=16.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
            TabRow(selectedTabIndex=editorSection,containerColor=MaterialTheme.colorScheme.surface.copy(alpha=0.92f),divider={}){
                listOf("基本","句型","題目","補充").forEachIndexed{index,label->Tab(selected=editorSection==index,onClick={editorSection=index},text={Text(label,maxLines=1)})}
            }
            when(editorSection){
                0->{
                    Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
                        FilledTonalIconButton(enabled=!busy,onClick=::launchCamera){Icon(Icons.Default.CameraAlt,"拍照")}
                        FilledTonalIconButton(enabled=!busy,onClick=::launchScanner){Icon(Icons.Default.DocumentScanner,"掃描")}
                        FilledTonalIconButton(enabled=!busy,onClick={gallery.launch("image/*")}){Icon(Icons.Default.PhotoLibrary,"圖片")}
                    }
                    if(busy)Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)){Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically){CircularProgressIndicator(Modifier.size(22.dp));Spacer(Modifier.width(10.dp));Text("AI 正在辨識並整理文法…")}}
                    error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
                    GrammarEditableMenu("課程",course.ifBlank{"通用"},linkedCourses,{course=it},{customField="課程"})
                    OutlinedTextField(title,{title=it},label={Text("文法類型名稱 *")},singleLine=true,modifier=Modifier.fillMaxWidth())
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        GrammarEditableMenu("分類",category,categoryOptions,{category=it},{customField="分類"},Modifier.weight(1f))
                        GrammarEditableMenu("程度",level,levelOptions,{level=it},{customField="程度"},Modifier.weight(1f))
                    }
                    OutlinedTextField(summary,{summary=it},label={Text("核心概念")},minLines=3,modifier=Modifier.fillMaxWidth())
                    OutlinedTextField(tags,{tags=it},label={Text("標籤（逗號分隔）")},singleLine=true,modifier=Modifier.fillMaxWidth())
                }
                1->{
                    Row(verticalAlignment=Alignment.CenterVertically){Text("句型與例句",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));IconButton(onClick={patternDrafts.add(GrammarPatternDraft(title="句型 ${patternDrafts.size+1}"))}){Icon(Icons.Default.AddCircle,"新增句型")}}
                    patternDrafts.forEachIndexed { index, pattern -> GrammarPatternEditor(index, pattern, { patternDrafts[index]=it }, { patternDrafts.removeAt(index) }) }
                    if(patternDrafts.isEmpty()) OutlinedButton(onClick={patternDrafts.add(GrammarPatternDraft(title="主要句型"))},modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.Add,null);Spacer(Modifier.width(6.dp));Text("新增第一組句型")}
                }
                2->{
                    Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("練習題",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);Text("每題可連結到一組句型。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};IconButton(onClick={drafts.add(GrammarQuestionDraft())}){Icon(Icons.Default.AddCircle,"新增題目")}}
                    drafts.forEachIndexed{i,d->GrammarQuestionEditor(i,d,patternDrafts,{drafts[i]=it},{drafts.removeAt(i)})}
                    if(drafts.isEmpty())OutlinedButton(onClick={drafts.add(GrammarQuestionDraft())},modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.Add,null);Spacer(Modifier.width(6.dp));Text("新增第一題")}
                }
                else->{
                    OutlinedTextField(mistakes,{mistakes=it},label={Text("常見錯誤")},minLines=4,modifier=Modifier.fillMaxWidth())
                    OutlinedTextField(comparison,{comparison=it},label={Text("容易混淆的文法")},minLines=4,modifier=Modifier.fillMaxWidth())
                }
            }
            Spacer(Modifier.height(72.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GrammarQuestionEditor(
    index: Int,
    value: GrammarQuestionDraft,
    patterns: List<GrammarPatternDraft>,
    onChange: (GrammarQuestionDraft) -> Unit,
    onDelete: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    var expanded by rememberSaveable(index) { mutableStateOf(index == 0) }
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("第 ${index + 1} 題 · ${if (value.options.isBlank()) "填空" else "選擇"}", fontWeight = FontWeight.Bold)
                    Text(
                        value.prompt.ifBlank { "尚未輸入題目" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { expanded = !expanded }) { Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, if (expanded) "收起" else "展開") }
                IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, "刪除這題") }
            }
            if (expanded) {
                if (patterns.isNotEmpty()) ExposedDropdownMenuBox(expanded = menu, onExpandedChange = { menu = it }) {
                    OutlinedTextField(
                        value = patterns.getOrNull(value.grammarPatternIndex)?.title ?: "整篇文法",
                        onValueChange = {}, readOnly = true, label = { Text("對應句型") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(menu) },
                        singleLine = true, modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("整篇文法") }, onClick = { onChange(value.copy(grammarPatternIndex = -1)); menu = false })
                        patterns.forEachIndexed { i, pattern ->
                            DropdownMenuItem(text = { Text(pattern.title.ifBlank { "句型 ${i + 1}" }, maxLines = 1, overflow = TextOverflow.Ellipsis) }, onClick = { onChange(value.copy(grammarPatternIndex = i)); menu = false })
                        }
                    }
                }
                OutlinedTextField(value.prompt, { onChange(value.copy(prompt = it)) }, label = { Text("題目句型") }, supportingText = { Text("例：She {{answer}} here yesterday.") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value.translation, { onChange(value.copy(translation = it)) }, label = { Text("中文提示") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value.answer, { onChange(value.copy(answer = it)) }, label = { Text("正確答案") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value.acceptedAnswers, { onChange(value.copy(acceptedAnswers = it)) }, label = { Text("其他可接受答案（逗號分隔）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value.options, { onChange(value.copy(options = it)) }, label = { Text("選項（留空即為填空題）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value.explanation, { onChange(value.copy(explanation = it)) }, label = { Text("答案說明") }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun GrammarPatternEditor(
    index: Int,
    value: GrammarPatternDraft,
    onChange: (GrammarPatternDraft) -> Unit,
    onDelete: () -> Unit
) {
    var expanded by rememberSaveable(index) { mutableStateOf(index == 0) }
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(value.title.ifBlank { "句型 ${index + 1}" }, fontWeight = FontWeight.Bold)
                    Text("${value.examples.size} 個例句", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { expanded = !expanded }) { Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, if (expanded) "收起" else "展開") }
                IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, "刪除句型") }
            }
            if (expanded) {
                OutlinedTextField(value.title, { onChange(value.copy(title = it)) }, label = { Text("句型名稱") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value.formula, { onChange(value.copy(formula = it)) }, label = { Text("公式") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value.meaning, { onChange(value.copy(meaning = it)) }, label = { Text("意思") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value.usage, { onChange(value.copy(usage = it)) }, label = { Text("使用時機") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                value.examples.forEachIndexed { exampleIndex, example ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("例句 ${exampleIndex + 1}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                                IconButton(onClick = { onChange(value.copy(examples = value.examples.toMutableList().also { it.removeAt(exampleIndex) })) }) { Icon(Icons.Default.Close, "刪除例句") }
                            }
                            OutlinedTextField(example.sentence, { sentence -> onChange(value.copy(examples = value.examples.toMutableList().also { it[exampleIndex] = example.copy(sentence = sentence) })) }, label = { Text("英文") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(example.translation, { translation -> onChange(value.copy(examples = value.examples.toMutableList().also { it[exampleIndex] = example.copy(translation = translation) })) }, label = { Text("中文") }, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
                TextButton(onClick = { onChange(value.copy(examples = value.examples + GrammarExampleDraft())) }) {
                    Icon(Icons.Default.Add, null); Spacer(Modifier.width(4.dp)); Text("新增例句")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class) @Composable fun GrammarDetailScreen(note:GrammarNote,questionCount:Int,onBack:()->Unit,onEdit:()->Unit,onDelete:()->Unit,onFavorite:()->Unit,onLearn:()->Unit,onQuiz:()->Unit){var confirm by remember{mutableStateOf(false)};if(confirm)AlertDialog(onDismissRequest={confirm=false},title={Text("刪除文法筆記？")},text={Text("筆記、題目與弱點紀錄會一併刪除，且無法復原。")},confirmButton={TextButton(onClick={confirm=false;onDelete()}){Text("刪除",color=MaterialTheme.colorScheme.error)}},dismissButton={TextButton(onClick={confirm=false}){Text("取消")}});Scaffold(topBar={TopAppBar(title={Text(note.title,maxLines=1,overflow=TextOverflow.Ellipsis)},navigationIcon={IconButton(onClick=onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"返回")}},actions={IconButton(onClick=onFavorite){Icon(if(note.favorite)Icons.Default.Star else Icons.Default.StarBorder,"收藏")};IconButton(onClick=onEdit){Icon(Icons.Default.Edit,"編輯")};IconButton(onClick={confirm=true}){Icon(Icons.Default.Delete,"刪除")}})},bottomBar={Row(Modifier.fillMaxWidth().padding(12.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)){OutlinedButton(onClick=onLearn,modifier=Modifier.weight(1f)){Text("開始學習")};Button(onClick=onQuiz,enabled=questionCount>0,modifier=Modifier.weight(1f)){Text("測驗（$questionCount）")}}}){p->LazyColumn(Modifier.padding(p),contentPadding=PaddingValues(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){AssistChip(onClick={},label={Text(note.course)});AssistChip(onClick={},label={Text("熟練度 ${note.masteryPercent}%")});AssistChip(onClick={},label={Text("$questionCount 題")})}};grammarSection("核心概念",note.summary);grammarSection("句型結構",note.structure);grammarSection("使用時機",note.usage);if(note.exampleSentence.isNotBlank())item{Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceContainer)){Column(Modifier.padding(16.dp)){Text("例句",fontWeight=FontWeight.Bold);Text(note.exampleSentence);if(note.exampleTranslation.isNotBlank())Text(note.exampleTranslation,color=MaterialTheme.colorScheme.onSurfaceVariant)}}};grammarSection("常見錯誤",note.commonMistakes);grammarSection("容易混淆",note.comparison);item{Spacer(Modifier.height(64.dp))}}}}
private fun androidx.compose.foundation.lazy.LazyListScope.grammarSection(title:String,content:String){if(content.isBlank())return;item{Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceContainer)){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(title,fontWeight=FontWeight.Bold);content.lines().filter(String::isNotBlank).forEach{Text(it)}}}}}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrammarLearnScreen(
    note: GrammarNote,
    patterns: List<GrammarPattern>,
    examples: List<GrammarExample>,
    onProgress: (Int) -> Unit,
    onRatePattern: (Long, Boolean) -> Unit,
    onBack: () -> Unit,
    onQuiz: () -> Unit
) {
    data class LearnPage(val title: String, val primary: String, val secondary: String = "", val patternId: Long = 0)
    val pages = remember(note, patterns, examples) {
        buildList {
            add(LearnPage("核心概念", note.summary.ifBlank { "尚未填寫核心概念" }, listOf(note.category, note.level).filter { it.isNotBlank() }.joinToString(" · ")))
            if (patterns.isEmpty()) {
                if (note.structure.isNotBlank()) add(LearnPage("主要句型", note.structure, note.usage))
                if (note.exampleSentence.isNotBlank()) add(LearnPage("例句", note.exampleSentence, note.exampleTranslation))
            } else patterns.forEachIndexed { index, pattern ->
                add(LearnPage(pattern.title.ifBlank { "句型 ${index + 1}" }, pattern.formula, listOf(pattern.meaning, pattern.usage).filter { it.isNotBlank() }.joinToString("\n\n"), pattern.id))
                examples.filter { it.grammarPatternId == pattern.id }.forEach { example ->
                    add(LearnPage("例句", example.sentence, example.translation, pattern.id))
                }
            }
            if (note.commonMistakes.isNotBlank()) add(LearnPage("常見錯誤", note.commonMistakes))
            if (note.comparison.isNotBlank()) add(LearnPage("容易混淆", note.comparison))
            add(LearnPage("學習完成", "你已看完這篇文法。可以前往測驗，或返回學習區稍後再複習。"))
        }
    }
    var step by rememberSaveable(note.id, pages.size) { mutableIntStateOf(note.studyStep.coerceIn(0, pages.lastIndex)) }
    val page = pages[step]
    BackHandler { onProgress(step); onBack() }
    Scaffold(
        topBar = { TopAppBar(title = { Text(note.title, maxLines = 1, overflow = TextOverflow.Ellipsis) }, navigationIcon = { IconButton(onClick = { onProgress(step); onBack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }, actions = { Text("${step + 1}／${pages.size}", modifier = Modifier.padding(end = 16.dp)) }) },
        bottomBar = { Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedIconButton(enabled = step > 0, onClick = { step-- }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "上一個") }
            if (page.patternId > 0) {
                OutlinedButton(onClick = { onRatePattern(page.patternId, false); if (step < pages.lastIndex) { step++; onProgress(step) } }, modifier = Modifier.weight(1f)) { Text("還不熟") }
                Button(onClick = { onRatePattern(page.patternId, true); if (step < pages.lastIndex) { step++; onProgress(step) } }, modifier = Modifier.weight(1f)) { Text("看懂了") }
            } else {
                Button(onClick = { if (step < pages.lastIndex) { step++; onProgress(step) } else onQuiz() }, modifier = Modifier.weight(1f)) { Text(if (step < pages.lastIndex) "下一個" else "開始測驗") }
            }
        } }
    ) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 16.dp, vertical = 8.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LinearProgressIndicator(progress = { (step + 1f) / pages.size.coerceAtLeast(1) }, modifier = Modifier.fillMaxWidth())
            Text(page.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(page.primary, style = if (page.title.startsWith("句型")) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge)
                    if (page.secondary.isNotBlank()) { HorizontalDivider(); Text(page.secondary, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class) @Composable fun GrammarQuizScreen(note:GrammarNote,questions:List<GrammarQuestion>,onAnswer:(Boolean,Boolean)->Unit,onBack:()->Unit,onRetry:()->Unit){val initial=remember(note.id,questions){questions.sortedBy{it.sortOrder}};var queue by remember(note.id,questions){mutableStateOf(initial)};var index by rememberSaveable(note.id){mutableIntStateOf(0)};var selected by rememberSaveable(note.id){mutableStateOf("")};var input by rememberSaveable(note.id){mutableStateOf("")};var checked by rememberSaveable(note.id){mutableStateOf(false)};var correct by rememberSaveable(note.id){mutableStateOf(false)};var completed by rememberSaveable(note.id){mutableStateOf(false)};var round by rememberSaveable(note.id){mutableIntStateOf(1)};val wrong=remember(note.id){mutableStateListOf<GrammarQuestion>()};val q=queue.getOrNull(index);fun clear(){selected="";input="";checked=false;correct=false};fun restart(){queue=initial;index=0;wrong.clear();completed=false;round=1;clear();onRetry()};Scaffold(topBar={TopAppBar(title={Text(if(completed)"測驗完成" else "${note.title}測驗")},navigationIcon={IconButton(onClick=onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"返回")}})}){p->Column(Modifier.padding(p).verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){if(completed){Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer),modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)){Icon(Icons.Default.CheckCircle,null,tint=MaterialTheme.colorScheme.primary);Text("測驗完成",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("所有題目都已答對")}};Button(onClick=::restart,modifier=Modifier.fillMaxWidth()){Text("再次測驗")};OutlinedButton(onClick=onBack,modifier=Modifier.fillMaxWidth()){Text("返回文法")}}else if(q==null)Text("這篇文法還沒有練習題。")else{val choices=q.options.split(',','、','\n').map(String::trim).filter(String::isNotBlank).distinct().take(4);val response=if(choices.isNotEmpty())selected else input;val accepted=(q.acceptedAnswers.split(',','、')+q.answer).map{it.trim().lowercase()}.filter(String::isNotBlank).toSet();Text(if(round==1)"第 ${index+1}／${queue.size} 題" else "錯題練習 · 第 ${index+1}／${queue.size} 題",color=MaterialTheme.colorScheme.primary);LinearProgressIndicator(progress={(index+1f)/queue.size.coerceAtLeast(1)},modifier=Modifier.fillMaxWidth());Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceContainer),modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text(q.prompt.replace("{{answer}}","____"),style=MaterialTheme.typography.titleLarge);if(q.translation.isNotBlank())Text(q.translation,color=MaterialTheme.colorScheme.onSurfaceVariant)}};if(choices.isNotEmpty())choices.forEach{c->FilterChip(selected=selected==c,enabled=!checked,onClick={selected=c},label={Text(c)},modifier=Modifier.fillMaxWidth())}else OutlinedTextField(input,{input=it},enabled=!checked,label={Text("輸入完整答案")},singleLine=true,modifier=Modifier.fillMaxWidth());if(checked)Card(colors=CardDefaults.cardColors(containerColor=if(correct)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer)){Column(Modifier.fillMaxWidth().padding(16.dp)){Text(if(correct)"答對了" else "答錯了",fontWeight=FontWeight.Bold);if(!correct)Text("正確答案：${q.answer}");if(q.explanation.isNotBlank())Text(q.explanation)}};Button(enabled=response.isNotBlank(),onClick={if(!checked){correct=response.trim().lowercase() in accepted;checked=true;onAnswer(correct,false);if(!correct&&wrong.none{it.id==q.id})wrong.add(q)}else if(!correct)clear()else if(index<queue.lastIndex){index++;clear()}else if(wrong.isNotEmpty()){queue=wrong.toList();wrong.clear();index=0;round++;clear()}else completed=true},modifier=Modifier.fillMaxWidth()){Text(if(!checked)"確認答案" else if(!correct)"再答一次" else if(index==queue.lastIndex&&wrong.isEmpty())"完成" else "下一題")}}}}}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrammarWritingCheckScreen(
    weaknesses: List<GrammarWeakness>,
    onCheck: suspend (String) -> List<GrammarWritingIssue>,
    onAccept: (GrammarWritingIssue) -> Unit,
    onRecognizeSources: (suspend (List<Uri>) -> GrammarWritingScanResult)? = null,
    onBack: () -> Unit = {},
    showTopBar: Boolean = true
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var text by rememberSaveable { mutableStateOf("") }
    var originalText by rememberSaveable { mutableStateOf("") }
    var issues by remember { mutableStateOf<List<GrammarWritingIssue>>(emptyList()) }
    val accepted = remember { mutableStateListOf<String>() }
    val ignored = remember { mutableStateListOf<String>() }
    var showRevised by rememberSaveable { mutableStateOf(false) }
    var busyLabel by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var uncertainParts by remember { mutableStateOf<List<String>>(emptyList()) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    fun recognize(uris: List<Uri>) {
        val recognizer = onRecognizeSources ?: return
        if (uris.isEmpty()) return
        scope.launch {
            busyLabel = "正在辨識作文…"; error = null
            runCatching { recognizer(uris) }
                .onSuccess { result ->
                    originalText = result.recognizedText
                    text = result.recognizedText
                    uncertainParts = result.uncertainParts
                    issues = emptyList()
                }
                .onFailure { error = it.message ?: "無法辨識作文" }
            busyLabel = null
        }
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents(), ::recognize)
    val files = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments(), ::recognize)
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) pendingCameraUri?.let { recognize(listOf(it)) }
    }
    val scanner = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            recognize(GmsDocumentScanningResult.fromActivityResultIntent(result.data)?.pages?.map { it.imageUri }.orEmpty())
        }
    }
    fun takePhoto() {
        val file = File(context.cacheDir, "grammar_writing_${System.currentTimeMillis()}.jpg")
        pendingCameraUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        camera.launch(pendingCameraUri!!)
    }
    fun scanDocument() {
        val activity = context.findActivity() ?: return
        val options = GmsDocumentScannerOptions.Builder().setGalleryImportAllowed(true).setPageLimit(10)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL).build()
        GmsDocumentScanning.getClient(options).getStartScanIntent(activity)
            .addOnSuccessListener { scanner.launch(IntentSenderRequest.Builder(it).build()) }
            .addOnFailureListener { error = it.message ?: "無法開啟掃描器" }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (showTopBar) TopAppBar(
                title = { Text("寫作檢查") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    enabled = text.isNotBlank() && busyLabel == null,
                    onClick = {
                        scope.launch {
                            busyLabel = "正在檢查文法與用字…"; error = null
                            runCatching { onCheck(text) }.onSuccess { issues = it }
                                .onFailure { error = it.message ?: "檢查失敗" }
                            busyLabel = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp)
                ) { Icon(Icons.Default.Spellcheck, null); Spacer(Modifier.width(6.dp)); Text("開始檢查") }
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("輸入作文，或拍照／掃描後先校對辨識文字，再交給 AI 檢查。原文不會被直接覆蓋。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (onRecognizeSources != null) item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalIconButton(onClick = ::takePhoto) { Icon(Icons.Default.CameraAlt, "拍照") }
                    FilledTonalIconButton(onClick = ::scanDocument) { Icon(Icons.Default.DocumentScanner, "掃描") }
                    FilledTonalIconButton(onClick = { gallery.launch("image/*") }) { Icon(Icons.Default.PhotoLibrary, "相片") }
                    FilledTonalIconButton(onClick = { files.launch(arrayOf("image/*", "application/pdf", "text/plain", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")) }) { Icon(Icons.Default.FileOpen, "檔案") }
                }
            }
            busyLabel?.let { label -> item { Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(22.dp)); Spacer(Modifier.width(10.dp)); Text(label) } } }
            if (uncertainParts.isNotEmpty()) item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text("有 ${uncertainParts.size} 處辨識不確定，請先對照原圖校正。", Modifier.padding(14.dp))
                }
            }
            item {
                OutlinedTextField(
                    value = text, onValueChange = { text = it },
                    label = { Text(if (originalText.isBlank()) "英文作文" else "辨識文字（請先校對）") },
                    minLines = 8, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)
                )
            }
            error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            if (issues.isEmpty() && busyLabel == null && text.isNotBlank()) item {
                Text("檢查結果會以逐句對照顯示；只有你確認的項目才會建立弱點練習。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (issues.isNotEmpty()) item {
                val grammarCount = issues.count { it.ruleKey.contains("grammar", true) || it.title.contains("文法") || it.title.contains("時態") }
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f))) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("檢查完成", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("共 ${issues.size} 項建議 · 文法 ${grammarCount} · 其他 ${issues.size - grammarCount}")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = !showRevised, onClick = { showRevised = false }, label = { Text("原文") })
                            FilterChip(selected = showRevised, onClick = { showRevised = true }, label = { Text("目前修正版") })
                        }
                        if (showRevised) Text(text, style = MaterialTheme.typography.bodyMedium)
                        Text("AI 建議仍應由你確認；只有按下套用的內容會修改文字並建立弱點。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            items(issues.filter { "${it.ruleKey}|${it.originalSentence}" !in ignored }, key = { "${it.ruleKey}|${it.originalSentence}" }) { issue ->
                val key = "${issue.ruleKey}|${issue.originalSentence}"
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = {}, label = { Text(issue.title) })
                        Text("原文", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(issue.originalSentence, color = MaterialTheme.colorScheme.error)
                        Text("建議", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(issue.correctedSentence, color = MaterialTheme.colorScheme.primary)
                        Text(issue.explanation)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { ignored.add(key) }, modifier = Modifier.weight(1f)) { Text("略過") }
                            Button(
                                enabled = key !in accepted,
                                onClick = {
                                    text = text.replaceFirst(issue.originalSentence, issue.correctedSentence)
                                    accepted.add(key); onAccept(issue)
                                }, modifier = Modifier.weight(1f)
                            ) { Text(if (key in accepted) "已套用" else "套用並練習") }
                        }
                    }
                }
            }
            if (weaknesses.isNotEmpty()) item { Text("已累積 ${weaknesses.size} 項寫作弱點", color = MaterialTheme.colorScheme.primary) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrammarQuizHubScreen(
    notes: List<GrammarNote>,
    questions: List<GrammarQuestion>,
    weaknesses: List<GrammarWeakness>,
    onStartTopicQuiz: (String, List<GrammarQuestion>) -> Unit,
    onGenerateAiTopicQuiz: (String) -> Unit,
    onWritingCheck: suspend (String) -> List<GrammarWritingIssue>,
    onAcceptWritingIssue: (GrammarWritingIssue) -> Unit,
    onOpenWritingCheck: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val courses = remember(notes) { notes.map { it.course }.filter(String::isNotBlank).distinct() }
    val grammarTypes = remember(notes) { notes.map { it.title }.filter(String::isNotBlank).distinct() }
    var selectedCourse by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedType by rememberSaveable { mutableStateOf<String?>(null) }
    var courseMenu by remember { mutableStateOf(false) }
    var typeMenu by remember { mutableStateOf(false) }
    val selectedNotes = notes.filter { (selectedCourse == null || it.course == selectedCourse) && (selectedType == null || it.title == selectedType) }
    val selectedIds = selectedNotes.map { it.id }.toSet()
    val selectedQuestions = questions.filter { it.grammarNoteId in selectedIds }
    LazyColumn(
        modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GrammarFilterMenu("課程", selectedCourse ?: "全部課程", courses, courseMenu, { courseMenu = it }, { selectedCourse = it }, Modifier.weight(1f))
            GrammarFilterMenu("文法類型", selectedType ?: "全部類型", grammarTypes, typeMenu, { typeMenu = it }, { selectedType = it }, Modifier.weight(1f))
        } }
        item {
            Button(
                enabled = selectedQuestions.isNotEmpty(),
                onClick = { onStartTopicQuiz("混合測驗", selectedQuestions) }, modifier = Modifier.fillMaxWidth()
            ) { Icon(Icons.Default.Quiz, null); Spacer(Modifier.width(6.dp)); Text("測驗全部 (${selectedQuestions.size})") }
        }
        item { Text("選擇文法類型", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        if (selectedQuestions.isEmpty()) item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("目前範圍還沒有測驗題", fontWeight = FontWeight.Bold)
                    TextButton(onClick = { onGenerateAiTopicQuiz(selectedType ?: selectedCourse ?: "綜合文法") }) { Icon(Icons.Default.AutoAwesome, null); Spacer(Modifier.width(4.dp)); Text("AI 建立題目") }
                }
            }
        }
        items(selectedNotes.filter { note -> selectedQuestions.any { it.grammarNoteId == note.id } }, key = { it.id }) { note ->
            val noteQuestions = selectedQuestions.filter { it.grammarNoteId == note.id }
            Card(onClick = { onStartTopicQuiz(note.title, noteQuestions) }, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MenuBook, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) { Text(note.title, fontWeight = FontWeight.Bold); Text("${note.category} · ${noteQuestions.size} 個句子／題目", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Icon(Icons.Default.ChevronRight, "開始")
                }
            }
        }
        item {
            Card(onClick = onOpenWritingCheck, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Spellcheck, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) { Text("寫作檢查", fontWeight = FontWeight.Bold); Text("輸入、拍照或掃描作文", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    if (weaknesses.isNotEmpty()) Text("${weaknesses.size} 個弱點", color = MaterialTheme.colorScheme.error)
                    Icon(Icons.Default.ChevronRight, "開啟")
                }
            }
        }
    }
}

private tailrec fun Context.findActivity():Activity?=when(this){is Activity->this;is ContextWrapper->baseContext.findActivity();else->null}
