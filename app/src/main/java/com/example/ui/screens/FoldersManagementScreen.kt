package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Deck
import com.example.data.entity.Flashcard
import com.example.ui.components.CalmEmptyState
import com.example.ui.components.AddFolderDialog
import com.example.ui.components.EditFolderDialog
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersManagementScreen(
    decks: List<Deck>,
    allCards: List<Flashcard>,
    onAddFolder: (courseName: String, folderName: String, description: String, colorHex: String) -> Unit,
    onUpdateFolder: (Deck) -> Unit,
    onDeleteFolder: (Deck) -> Unit,
    onMoveFolder: (Deck, Int) -> Unit = { _, _ -> },
    onMoveFolderGlobally: (Deck, Int) -> Unit = { _, _ -> },
    onReorderFolders: (List<Long>) -> Unit = {},
    onRenameCourse: (String, String, (Boolean) -> Unit) -> Unit = { _, _, done -> done(false) },
    onOpenCardList: (deckId: Long) -> Unit,
    onJumpToStudy: (deckId: Long) -> Unit,
    onJumpToQuiz: (deckId: Long) -> Unit,
    onOpenAssistant: () -> Unit = {},
    showTopBar: Boolean = true,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingDeck by remember { mutableStateOf<Deck?>(null) }
    var deckToDelete by remember { mutableStateOf<Deck?>(null) }
    var courseToRename by remember { mutableStateOf<String?>(null) }
    var renamedCourseValue by remember { mutableStateOf("") }

    val courses = remember(decks) {
        decks.map { it.category }.distinct().ifEmpty { listOf("通用課程") }
    }
    var selectedCourseFilter by remember { mutableStateOf<String?>(null) }

    val filteredDecks = remember(decks, selectedCourseFilter) {
        if (selectedCourseFilter == null) decks
        else decks.filter { it.category == selectedCourseFilter }
    }
    val sourceDeckIds = filteredDecks.map { it.id }
    var displayedDeckIds by remember(sourceDeckIds) { mutableStateOf(sourceDeckIds) }
    val displayedDecks = displayedDeckIds.mapNotNull { id -> filteredDecks.firstOrNull { it.id == id } }
    val folderListState = rememberLazyListState()
    var folderOrderChanged by remember { mutableStateOf(false) }
    val folderReorderState = rememberReorderableLazyListState(
        lazyListState = folderListState
    ) { from, to ->
        // The course selector occupies the first LazyColumn position.
        if (displayedDeckIds.isNotEmpty()) {
            val fromIndex = (from.index - 1).coerceIn(displayedDeckIds.indices)
            val toIndex = (to.index - 1).coerceIn(displayedDeckIds.indices)
            if (fromIndex != toIndex) {
                displayedDeckIds = displayedDeckIds.toMutableList().apply {
                    add(toIndex, removeAt(fromIndex))
                }
                folderOrderChanged = true
            }
        }
    }

    LaunchedEffect(folderReorderState.isAnyItemDragging) {
        if (!folderReorderState.isAnyItemDragging && folderOrderChanged) {
            onReorderFolders(displayedDeckIds)
            folderOrderChanged = false
        }
    }

    if (showAddDialog) {
        AddFolderDialog(
            existingCourses = courses,
            defaultCourse = selectedCourseFilter,
            onDismiss = { showAddDialog = false },
            onConfirm = { courseName, folderName, description, colorHex ->
                showAddDialog = false
                onAddFolder(courseName, folderName, description, colorHex)
            }
        )
    }

    editingDeck?.let { deck ->
        EditFolderDialog(
            deck = deck,
            existingCourses = courses,
            onDismiss = { editingDeck = null },
            onConfirm = { updated ->
                editingDeck = null
                onUpdateFolder(updated)
            }
        )
    }

    deckToDelete?.let { deck ->
        AlertDialog(
            onDismissRequest = { deckToDelete = null },
            title = { Text("確定刪除資料夾「${deck.name}」？", fontWeight = FontWeight.Bold) },
            text = { Text("刪除此資料夾將一併移除其中的所有單字卡，此操作無法復原。") },
            confirmButton = {
                Button(
                    onClick = {
                        deckToDelete = null
                        onDeleteFolder(deck)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("確定刪除", color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deckToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }

    courseToRename?.let { originalName ->
        AlertDialog(
            onDismissRequest = { courseToRename = null },
            title = { Text("重新命名課程", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renamedCourseValue,
                    onValueChange = { renamedCourseValue = it },
                    label = { Text("課程名稱") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newName = renamedCourseValue.trim()
                        onRenameCourse(originalName, newName) { success ->
                            if (success) selectedCourseFilter = newName
                            courseToRename = null
                        }
                    },
                    enabled = renamedCourseValue.isNotBlank() &&
                        renamedCourseValue.trim() != originalName
                ) {
                    Text("儲存")
                }
            },
            dismissButton = {
                TextButton(onClick = { courseToRename = null }) { Text("取消") }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (showTopBar) TopAppBar(
                expandedHeight = 48.dp,
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("資料夾", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                },
                actions = {
                    IconButton(onClick = onOpenAssistant) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Vocab AI")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.testTag("add_folder_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "新增資料夾")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            state = folderListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(top = if (showTopBar) 6.dp else 0.dp)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                // Course Category Filter Chips
                Text(
                    text = "選擇課程",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (selectedCourseFilter == null) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { selectedCourseFilter = null }
                        ) {
                            Text(
                                text = "全部資料夾 (${decks.size})",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                color = if (selectedCourseFilter == null) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    items(courses) { course ->
                        val count = decks.count { it.category == course }
                        val isSelected = selectedCourseFilter == course
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable {
                                    selectedCourseFilter = if (isSelected) null else course
                                }
                        ) {
                            Text(
                                text = "$course ($count)",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                selectedCourseFilter?.let { course ->
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            renamedCourseValue = course
                            courseToRename = course
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("重新命名課程「$course」")
                    }
                } ?: Text(
                    text = "拖曳卡片右上方把手可調整整體順序。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (displayedDecks.isEmpty()) {
                item {
                    CalmEmptyState(
                        icon = Icons.Default.Folder,
                        title = "還沒有資料夾",
                        message = "點右下角新增第一個資料夾。"
                    )
                }
            } else {
                itemsIndexed(displayedDecks, key = { _, deck -> deck.id }) { _, deck ->
                    val deckCards = allCards.filter { it.deckId == deck.id }
                    val totalCardCount = deckCards.size
                    val masteredCount = deckCards.count { it.isMastered }
                    val dueCount = deckCards.count { it.nextReviewTimestamp <= System.currentTimeMillis() && !it.isMastered && !it.isSuspended }

                    val deckColor = try {
                        Color(android.graphics.Color.parseColor(deck.colorHex))
                    } catch (e: Exception) {
                        MaterialTheme.colorScheme.primary
                    }

                    ReorderableItem(
                        state = folderReorderState,
                        key = deck.id
                    ) {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Folder Header Info
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(deckColor)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = deck.name,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = deck.category,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                                    IconButton(
                                        onClick = {},
                                        modifier = Modifier
                                            .draggableHandle()
                                            .size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DragHandle,
                                            contentDescription = "拖曳調整資料夾順序",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { editingDeck = deck },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Edit, contentDescription = "編輯資料夾", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = { deckToDelete = deck },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "刪除資料夾", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            if (deck.description.isNotBlank()) {
                                Text(
                                    text = deck.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Card Statistics Summary
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("總單字", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    Text("$totalCardCount", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("已精通", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    Text("$masteredCount", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("待複習", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    Text("$dueCount", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            // Folder Actions Toolbar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { onOpenCardList(deck.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("管理", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { onJumpToStudy(deck.id) },
                                    enabled = totalCardCount > 0,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("學習", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { onJumpToQuiz(deck.id) },
                                    enabled = totalCardCount > 0,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("測驗", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
