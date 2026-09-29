package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.entity.GrammarLibrary
import com.example.data.entity.GrammarNote
import com.example.ui.components.AddFolderDialog
import com.example.ui.components.CalmEmptyState
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun GrammarLibrariesScreen(
    libraries: List<GrammarLibrary>,
    notes: List<GrammarNote>,
    sharedCourses: List<String> = emptyList(),
    highlightedAction: String,
    onAdd: (String, String, String, String) -> Unit,
    onUpdate: (GrammarLibrary) -> Unit,
    onDelete: (GrammarLibrary) -> Unit,
    onReorder: (List<Long>) -> Unit,
    onManage: (GrammarLibrary) -> Unit,
    onLearn: (GrammarLibrary) -> Unit,
    onQuiz: (GrammarLibrary) -> Unit
) {
    var selectedCourse by rememberSaveable(highlightedAction) { mutableStateOf<String?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<GrammarLibrary?>(null) }
    var deleting by remember { mutableStateOf<GrammarLibrary?>(null) }
    val courses = remember(libraries, sharedCourses) { (sharedCourses + libraries.map { it.course }).filter(String::isNotBlank).distinct() }
    val filtered = libraries.filter { selectedCourse == null || it.course == selectedCourse }
    val listState = rememberLazyListState()
    var visibleIds by remember(filtered) { mutableStateOf(filtered.map { it.id }) }
    var changed by remember { mutableStateOf(false) }
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        if (visibleIds.isNotEmpty()) {
            val fromIndex = (from.index - 1).coerceIn(visibleIds.indices)
            val toIndex = (to.index - 1).coerceIn(visibleIds.indices)
            if (fromIndex != toIndex) {
                visibleIds = visibleIds.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
                changed = true
            }
        }
    }
    LaunchedEffect(reorderState.isAnyItemDragging) {
        if (!reorderState.isAnyItemDragging && changed) {
            onReorder(visibleIds)
            changed = false
        }
    }
    if (showAdd) AddFolderDialog(
        existingCourses = courses,
        defaultCourse = selectedCourse,
        itemLabel = "文法庫",
        onDismiss = { showAdd = false },
        onConfirm = { course, name, description, color -> showAdd = false; onAdd(course, name, description, color) }
    )
    editing?.let { library ->
        GrammarLibraryEditDialog(library, courses, { editing = null }) {
            editing = null
            onUpdate(it)
        }
    }
    deleting?.let { library ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("刪除文法庫「${library.name}」？") },
            text = { Text("其中的文法、句型與題目會一併刪除，且無法復原。") },
            confirmButton = { Button(onClick = { deleting = null; onDelete(library) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("確定刪除") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("取消") } }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (highlightedAction == "manage") {
                FloatingActionButton(
                    onClick = { showAdd = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape
                ) { Icon(Icons.Default.Add, "新增文法庫") }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("選擇課程", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { CourseChip("全部文法庫 (${libraries.size})", selectedCourse == null) { selectedCourse = null } }
                    items(courses) { course -> CourseChip("$course (${libraries.count { it.course == course }})", selectedCourse == course) { selectedCourse = course } }
                }
                if (highlightedAction == "manage" && libraries.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("拖曳卡片右上方把手可調整整體順序。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (filtered.isEmpty()) item { CalmEmptyState(Icons.Default.MenuBook, "還沒有文法庫", "按右下角＋建立文法庫。") }
            itemsIndexed(visibleIds, key = { _, id -> id }) { _, id ->
                val library = filtered.firstOrNull { it.id == id } ?: return@itemsIndexed
                ReorderableItem(reorderState, key = id) { dragging ->
                    val libraryNotes = notes.filter { it.course == library.course && it.folder == library.name }
                    val total = libraryNotes.size
                    val mastered = libraryNotes.count { it.masteryPercent >= 80 }
                    val due = libraryNotes.count { it.nextReviewAt <= System.currentTimeMillis() && it.masteryPercent < 80 }
                    val color = runCatching { Color(android.graphics.Color.parseColor(library.colorHex)) }.getOrElse { MaterialTheme.colorScheme.primary }
                    when (highlightedAction) {
                        "learn" -> GrammarLearningLibraryCard(library, total, mastered, due, color, { onManage(library) }, { onLearn(library) }, { onQuiz(library) })
                        "quiz" -> GrammarQuizLibraryCard(library, total, color, { onManage(library) }, { onLearn(library) }, { onQuiz(library) })
                        else -> GrammarManageLibraryCard(
                            library, total, mastered, due, color, dragging,
                            { editing = library }, { deleting = library }, { onManage(library) }, { onLearn(library) }, { onQuiz(library) },
                            Modifier.draggableHandle()
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable private fun CourseChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(20.dp), color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant) {
        Text(
            text,
            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            maxLines = 1
        )
    }
}

@Composable private fun Stat(label: String, value: Int) = Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
    Text(value.toString(), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
}

@Composable private fun LibraryAction(text: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    if (selected) Button(onClick = onClick, enabled = enabled, modifier = modifier, shape = RoundedCornerShape(12.dp)) { Text(text, fontWeight = FontWeight.Bold) }
    else OutlinedButton(onClick = onClick, enabled = enabled, modifier = modifier, shape = RoundedCornerShape(12.dp)) { Text(text, fontWeight = FontWeight.Bold) }
}

@Composable
private fun GrammarManageLibraryCard(
    library: GrammarLibrary,
    total: Int,
    mastered: Int,
    due: Int,
    color: Color,
    dragging: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onManage: () -> Unit,
    onLearn: () -> Unit,
    onQuiz: () -> Unit,
    dragModifier: Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (dragging) .8f else .5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(16.dp).clip(CircleShape).background(color))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(library.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(library.course, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                    IconButton(onClick = {}, modifier = dragModifier.size(36.dp)) {
                        Icon(Icons.Default.DragHandle, "拖曳排序", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, "編輯文法庫", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, "刪除文法庫", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }
            if (library.description.isNotBlank()) {
                Text(library.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Stat("總文法", total)
                Stat("已精通", mastered)
                Stat("待複習", due)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                LibraryAction("管理", true, true, onManage, Modifier.weight(1f))
                LibraryAction("學習", false, total > 0, onLearn, Modifier.weight(1f))
                LibraryAction("測驗", false, total > 0, onQuiz, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun GrammarLearningLibraryCard(
    library: GrammarLibrary,
    total: Int,
    mastered: Int,
    due: Int,
    color: Color,
    onManage: () -> Unit,
    onLearn: () -> Unit,
    onQuiz: () -> Unit
) {
    val progress = if (total > 0) mastered.toFloat() / total else 0f
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(14.dp).clip(CircleShape).background(color))
                    Spacer(Modifier.width(8.dp))
                    Text(library.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Surface(shape = RoundedCornerShape(10.dp), color = if (due > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer) {
                    Text(
                        when {
                            total == 0 -> "沒有文法"
                            due > 0 -> "待複習 $due"
                            mastered == total -> "已完成"
                            else -> "無到期"
                        },
                        Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (due > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("精通進度", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text("${(progress * 100).toInt()}% ($mastered/$total)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = color,
                    trackColor = MaterialTheme.colorScheme.surface
                )
            }
            Spacer(Modifier.height(2.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LibraryAction("管理", false, true, onManage, Modifier.weight(1f))
                LibraryAction("學習", true, total > 0, onLearn, Modifier.weight(1f))
                LibraryAction("測驗", false, total > 0, onQuiz, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun GrammarQuizLibraryCard(
    library: GrammarLibrary,
    total: Int,
    color: Color,
    onManage: () -> Unit,
    onLearn: () -> Unit,
    onQuiz: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(14.dp).clip(CircleShape).background(color))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(library.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(library.course, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text("$total 個", Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                LibraryAction("管理", false, true, onManage, Modifier.weight(1f))
                LibraryAction("學習", false, total > 0, onLearn, Modifier.weight(1f))
                LibraryAction("測驗", true, total > 0, onQuiz, Modifier.weight(1.2f))
            }
        }
    }
}

@Composable
private fun GrammarLibraryEditDialog(
    library: GrammarLibrary,
    courses: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (GrammarLibrary) -> Unit
) {
    var name by remember { mutableStateOf(library.name) }
    var description by remember { mutableStateOf(library.description) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("編輯文法庫") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("課程：${library.course}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(name, { name = it }, label = { Text("名稱") }, singleLine = true)
            OutlinedTextField(description, { description = it }, label = { Text("說明（選填）") }, singleLine = true)
        } },
        confirmButton = { Button(onClick = { onConfirm(library.copy(name = name.trim(), description = description.trim())) }, enabled = name.isNotBlank()) { Text("儲存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun GrammarLibraryManageScreen(
    library: GrammarLibrary,
    notes: List<GrammarNote>,
    onEdit: (GrammarNote) -> Unit,
    onDelete: (GrammarNote) -> Unit,
    onReorder: (List<Long>) -> Unit,
    onAdd: () -> Unit,
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<GrammarNote?>(null) }
    val filtered = notes.filter {
        query.isBlank() || listOf(it.title, it.structure, it.exampleSentence, it.summary).any { text -> text.contains(query, true) }
    }
    val listState = rememberLazyListState()
    var noteIds by remember(filtered) { mutableStateOf(filtered.map { it.id }) }
    var orderChanged by remember { mutableStateOf(false) }
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        if (noteIds.isNotEmpty()) {
            val fromIndex = (from.index - 1).coerceIn(noteIds.indices)
            val toIndex = (to.index - 1).coerceIn(noteIds.indices)
            if (fromIndex != toIndex) {
                noteIds = noteIds.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
                orderChanged = true
            }
        }
    }
    LaunchedEffect(reorderState.isAnyItemDragging) {
        if (!reorderState.isAnyItemDragging && orderChanged) {
            onReorder(noteIds)
            orderChanged = false
        }
    }
    pendingDelete?.let { note -> AlertDialog(
        onDismissRequest = { pendingDelete = null },
        title = { Text("刪除文法「${note.title}」？") },
        text = { Text("句型、例句、題目與學習紀錄會一併刪除。") },
        confirmButton = { Button(onClick = { pendingDelete = null; onDelete(note) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("刪除") } },
        dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } }
    ) }
    Scaffold(
        floatingActionButton = { FloatingActionButton(onClick = onAdd) { Icon(Icons.Default.Add, "新增文法") } }
    ) { padding ->
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onBack) { Text("返回") }
                    Column { Text("${library.name} · ${notes.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(library.course, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    placeholder = { Text("搜尋文法、句型或例句") },
                    shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()
                )
            }
            if (filtered.isEmpty()) item { CalmEmptyState(Icons.Default.MenuBook, if (query.isBlank()) "尚無文法" else "找不到結果", if (query.isBlank()) "按右下角＋新增文法。" else "請換一個關鍵字。") }
            itemsIndexed(noteIds, key = { _, id -> id }) { _, id ->
                val note = filtered.firstOrNull { it.id == id } ?: return@itemsIndexed
                ReorderableItem(reorderState, key = id) { dragging ->
                    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (dragging) .8f else .55f))) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(note.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(note.structure.ifBlank { note.summary }, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(Icons.Default.DragHandle, "拖曳排序", Modifier.draggableHandle())
                                IconButton(onClick = { onEdit(note) }) { Icon(Icons.Default.Edit, "編輯文法") }
                                IconButton(onClick = { pendingDelete = note }) { Icon(Icons.Default.Delete, "刪除文法", tint = MaterialTheme.colorScheme.error) }
                            }
                            if (note.exampleSentence.isNotBlank()) Text(note.exampleSentence, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            LinearProgressIndicator(progress = { note.masteryPercent.coerceIn(0, 100) / 100f }, modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)))
                        }
                    }
                }
            }
        }
    }
}
