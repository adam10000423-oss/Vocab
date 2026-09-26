package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Deck
import com.example.data.entity.Flashcard
import com.example.ui.components.AddFolderDialog
import com.example.ui.components.CalmEmptyState
import com.example.ui.components.PronunciationPracticeDialog
import com.example.ui.components.rememberResponsiveLayout
import com.example.util.OcrWordParser
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CardListScreen(
    cards: List<Flashcard>,
    decks: List<Deck> = emptyList(),
    selectedDeckId: Long? = null,
    onSelectDeck: (Long?) -> Unit = {},
    onToggleFavorite: (Flashcard) -> Unit,
    onDeleteCard: (Flashcard) -> Unit,
    onDeleteCards: (List<Flashcard>) -> Unit = {},
    onMoveCard: (Flashcard, Int) -> Unit = { _, _ -> },
    onReorderCards: (Long, List<Long>) -> Unit = { _, _ -> },
    onMoveCards: (List<Flashcard>, Long, (Int, Int) -> Unit) -> Unit = { _, _, done -> done(0, 0) },
    onCreateFolderAndMoveCards: (
        String,
        String,
        String,
        String,
        List<Flashcard>,
        (Int, Int) -> Unit
    ) -> Unit = { _, _, _, _, _, done -> done(0, 0) },
    onEditCard: (Flashcard) -> Unit,
    onSpeak: (String) -> Unit,
    onStopSpeaking: () -> Unit = {},
    onAddNewCard: () -> Unit,
    onOpenExternalImport: () -> Unit = {},
    onOpenScanner: () -> Unit = {},
    onStartReview: (Set<Long>?) -> Unit = { _ -> },
    onOpenQuizGames: (Set<Long>?) -> Unit = { _ -> },
    onOpenAssistant: () -> Unit = {},
    pronunciationWeakCardIds: Set<Long> = emptySet(),
    onPronunciationResult: (Long, Int) -> Unit = { _, _ -> },
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val responsive = rememberResponsiveLayout()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: All, 1: Due, 2: Favorites
    var showAddModeDialog by remember { mutableStateOf(false) }
    var selectedCourseFilter by remember { mutableStateOf<String?>(null) }
    var showCourseMenu by remember { mutableStateOf(false) }
    var showDeckMenu by remember { mutableStateOf(false) }

    var isMultiSelectMode by remember { mutableStateOf(false) }
    val selectedCards = remember { mutableStateListOf<Flashcard>() }
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }
    var showMoveCardsDialog by remember { mutableStateOf(false) }
    var showCreateMoveFolderDialog by remember { mutableStateOf(false) }
    var pronunciationCard by remember { mutableStateOf<Flashcard?>(null) }

    pronunciationCard?.let { card ->
        PronunciationPracticeDialog(
            card = card,
            onSpeak = onSpeak,
            onStopSpeaking = onStopSpeaking,
            onResult = { score -> onPronunciationResult(card.id, score) },
            onDismiss = { pronunciationCard = null }
        )
    }

    val activeDeck = decks.firstOrNull { it.id == selectedDeckId }
    LaunchedEffect(selectedDeckId) {
        if (selectedDeckId != null) selectedCourseFilter = activeDeck?.category
    }
    val selectedScopeDeckIds: Set<Long>? = when {
        selectedDeckId != null -> setOf(selectedDeckId)
        selectedCourseFilter != null -> decks
            .filter { it.category == selectedCourseFilter }
            .map { it.id }
            .toSet()
        else -> null
    }

    // Filter cards belonging to selected deck
    val deckFilteredCards = remember(cards, selectedDeckId, selectedCourseFilter, decks) {
        when {
            selectedDeckId != null -> cards.filter { it.deckId == selectedDeckId }
            selectedCourseFilter != null -> {
                val courseDeckIds = decks
                    .filter { it.category == selectedCourseFilter }
                    .map { it.id }
                    .toSet()
                cards.filter { it.deckId in courseDeckIds }
            }
            else -> cards
        }
    }

    val finalFilteredCards = deckFilteredCards.filter { card ->
        val matchesSearch = card.word.contains(searchQuery, ignoreCase = true) ||
                card.definition.contains(searchQuery, ignoreCase = true)

        val matchesTab = when (selectedTab) {
            1 -> card.nextReviewTimestamp <= System.currentTimeMillis() && !card.isMastered && !card.isSuspended
            2 -> card.isFavorite
            3 -> card.id in pronunciationWeakCardIds
            else -> true
        }

        matchesSearch && matchesTab
    }
    val dragReorderEnabled = selectedDeckId != null &&
        selectedTab == 0 &&
        searchQuery.isBlank() &&
        !isMultiSelectMode
    val sourceCardIds = if (dragReorderEnabled) finalFilteredCards.map { it.id } else emptyList()
    var displayedCardIds by remember(sourceCardIds) { mutableStateOf(sourceCardIds) }
    val displayedCards = if (dragReorderEnabled) {
        displayedCardIds.mapNotNull { id -> finalFilteredCards.firstOrNull { it.id == id } }
    } else {
        finalFilteredCards
    }
    val cardListState = rememberLazyListState()
    var cardOrderChanged by remember { mutableStateOf(false) }
    val cardReorderState = rememberReorderableLazyListState(
        lazyListState = cardListState
    ) { from, to ->
        if (dragReorderEnabled && displayedCardIds.isNotEmpty()) {
            val fromIndex = from.index.coerceIn(displayedCardIds.indices)
            val toIndex = to.index.coerceIn(displayedCardIds.indices)
            if (fromIndex != toIndex) {
                displayedCardIds = displayedCardIds.toMutableList().apply {
                    add(toIndex, removeAt(fromIndex))
                }
                cardOrderChanged = true
            }
        }
    }

    LaunchedEffect(cardReorderState.isAnyItemDragging) {
        if (!cardReorderState.isAnyItemDragging && cardOrderChanged) {
            selectedDeckId?.let { onReorderCards(it, displayedCardIds) }
            cardOrderChanged = false
        }
    }

    // Dialog for Choosing Add Card Method
    if (showAddModeDialog) {
        AlertDialog(
            onDismissRequest = { showAddModeDialog = false },
            title = {
                Text(
                    text = "新增卡片至「${activeDeck?.name ?: "單字庫"}」",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    // Option 1: Manual Input
                    Card(
                        onClick = {
                            showAddModeDialog = false
                            onAddNewCard()
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("手動輸入單字", fontWeight = FontWeight.Bold)
                                Text("批次輸入或 AI 補齊", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }

                    // Option 2: Online vocabulary set
                    Card(
                        onClick = {
                            showAddModeDialog = false
                            onOpenExternalImport()
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("匯入線上單字集", fontWeight = FontWeight.Bold)
                                Text(
                                    "網址、搜尋或平台檔案",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    // Option 3: Camera, document scanner, photo or file
                    Card(
                        onClick = {
                            showAddModeDialog = false
                            onOpenScanner()
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DocumentScanner,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("掃描或匯入文件", fontWeight = FontWeight.Bold)
                                Text("拍照、相片、PDF 或 DOCX", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddModeDialog = false }) {
                    Text("取消", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showBulkDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteConfirm = false },
            title = { Text("批量刪除單字卡", fontWeight = FontWeight.Bold) },
            text = { Text("確定要刪除選取的 ${selectedCards.size} 張單字卡嗎？此操作無法復原。") },
            confirmButton = {
                Button(
                    onClick = {
                        showBulkDeleteConfirm = false
                        onDeleteCards(selectedCards.toList())
                        selectedCards.clear()
                        isMultiSelectMode = false
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("已刪除選取的單字卡")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("確定刪除 (${selectedCards.size})", color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }

    fun selectedCardsInDisplayOrder(): List<Flashcard> =
        selectedCards.map { it.id }.toSet().let { selectedIds ->
            cards.filter { it.id in selectedIds }
        }

    fun finishMove(movedCount: Int, mergedCount: Int) {
        selectedCards.clear()
        isMultiSelectMode = false
        coroutineScope.launch {
            snackbarHostState.showSnackbar(
                when {
                    movedCount < 0 -> "移動失敗，單字卡沒有變更"
                    mergedCount > 0 ->
                        "已移動 $movedCount 張，另有 $mergedCount 張與目標資料夾重複並完成合併"
                    movedCount > 0 -> "已移動 $movedCount 張單字卡"
                    else -> "選取的單字已在目標資料夾"
                }
            )
        }
    }

    if (showMoveCardsDialog) {
        AlertDialog(
            onDismissRequest = { showMoveCardsDialog = false },
            title = { Text("移動 ${selectedCards.size} 張單字卡", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(decks, key = { it.id }) { deck ->
                        val cardCount = cards.count { it.deckId == deck.id }
                        Surface(
                            onClick = {
                                val moving = selectedCardsInDisplayOrder()
                                showMoveCardsDialog = false
                                onMoveCards(moving, deck.id, ::finishMove)
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(deck.name, fontWeight = FontWeight.Bold)
                                Text(
                                    "${deck.category}・$cardCount 張",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showMoveCardsDialog = false
                        showCreateMoveFolderDialog = true
                    }
                ) {
                    Text("新增資料夾並移入", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMoveCardsDialog = false }) { Text("取消") }
            }
        )
    }

    if (showCreateMoveFolderDialog) {
        AddFolderDialog(
            existingCourses = decks.map { it.category }.distinct(),
            defaultCourse = activeDeck?.category ?: selectedCourseFilter,
            onDismiss = { showCreateMoveFolderDialog = false },
            onConfirm = { courseName, folderName, description, colorHex ->
                val moving = selectedCardsInDisplayOrder()
                showCreateMoveFolderDialog = false
                onCreateFolderAndMoveCards(
                    courseName,
                    folderName,
                    description,
                    colorHex,
                    moving,
                    ::finishMove
                )
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                expandedHeight = 48.dp,
                title = { Text("單字卡 · ${deckFilteredCards.size}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenAssistant) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Vocab AI")
                    }
                    IconButton(
                        onClick = {
                            isMultiSelectMode = !isMultiSelectMode
                            if (!isMultiSelectMode) selectedCards.clear()
                        },
                        modifier = Modifier.testTag("toggle_multi_select_button")
                    ) {
                        Icon(
                            imageVector = if (isMultiSelectMode) Icons.Default.CheckCircle else Icons.Default.EditNote,
                            contentDescription = if (isMultiSelectMode) "完成多選" else "多選管理",
                            tint = if (isMultiSelectMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!isMultiSelectMode) {
            FloatingActionButton(
                onClick = { showAddModeDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.testTag("add_card_in_folder_fab")
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "新增單字卡")
            }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(top = 6.dp)
                .padding(horizontal = 16.dp)
        ) {
            val courses = remember(decks) { decks.map { it.category }.distinct() }
            val visibleDecks = remember(decks, selectedCourseFilter) {
                if (selectedCourseFilter == null) decks else decks.filter { it.category == selectedCourseFilter }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactSelector(
                    label = selectedCourseFilter ?: "全部課程",
                    expanded = showCourseMenu,
                    onExpandedChange = { showCourseMenu = it },
                    modifier = Modifier.weight(1f)
                ) {
                    DropdownMenuItem(
                        text = { Text("全部課程（${cards.size}）") },
                        onClick = {
                            selectedCourseFilter = null
                            onSelectDeck(null)
                            showCourseMenu = false
                        }
                    )
                    courses.forEach { course ->
                        val count = cards.count { card -> decks.firstOrNull { it.id == card.deckId }?.category == course }
                        DropdownMenuItem(
                            text = { Text("$course（$count）", maxLines = 1) },
                            onClick = {
                                selectedCourseFilter = course
                                onSelectDeck(null)
                                showCourseMenu = false
                            }
                        )
                    }
                }
                CompactSelector(
                    label = activeDeck?.name ?: if (selectedCourseFilter == null) "全部資料夾" else "課程全部",
                    expanded = showDeckMenu,
                    onExpandedChange = { showDeckMenu = it },
                    modifier = Modifier.weight(1f)
                ) {
                    DropdownMenuItem(
                        text = { Text("${if (selectedCourseFilter == null) "全部資料夾" else "課程全部"}（${deckFilteredCards.size}）") },
                        onClick = {
                            onSelectDeck(null)
                            showDeckMenu = false
                        }
                    )
                    visibleDecks.forEach { deck ->
                        val count = cards.count { it.deckId == deck.id }
                        DropdownMenuItem(
                            text = { Text("${deck.name}（$count）", maxLines = 1) },
                            onClick = {
                                selectedCourseFilter = deck.category
                                onSelectDeck(deck.id)
                                showDeckMenu = false
                            }
                        )
                    }
                }
            }

            // Search Input with Clear button
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                textStyle = MaterialTheme.typography.bodySmall,
                placeholder = {
                    Text(
                        "搜尋單字或解釋",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "清除搜尋")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .testTag("card_list_search_input")
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Button(
                    onClick = { onStartReview(selectedScopeDeckIds) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 46.dp).testTag("folder_start_review_button")
                ) { Text("學習", fontWeight = FontWeight.Bold) }
                OutlinedButton(
                    onClick = { onOpenQuizGames(selectedScopeDeckIds) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 46.dp).testTag("folder_open_quiz_button")
                ) { Text("測驗", fontWeight = FontWeight.Bold) }
            }

            val dueCount = deckFilteredCards.count {
                it.nextReviewTimestamp <= System.currentTimeMillis() && !it.isMastered && !it.isSuspended
            }
            val favoriteCount = deckFilteredCards.count { it.isFavorite }
            val pronunciationWeakCount = deckFilteredCards.count { it.id in pronunciationWeakCardIds }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp)
            ) {
                item { PlainTab("全部 ${deckFilteredCards.size}", selectedTab == 0, { selectedTab = 0 }, Modifier.width(96.dp)) }
                item { PlainTab("待複習 $dueCount", selectedTab == 1, { selectedTab = 1 }, Modifier.width(96.dp)) }
                item { PlainTab("收藏 $favoriteCount", selectedTab == 2, { selectedTab = 2 }, Modifier.width(96.dp)) }
                item { PlainTab("發音弱點 $pronunciationWeakCount", selectedTab == 3, { selectedTab = 3 }, Modifier.width(112.dp)) }
            }

            if (isMultiSelectMode) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "已選取 ${selectedCards.size} / ${finalFilteredCards.size}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            maxItemsInEachRow = if (responsive.isSmallWidth || responsive.isLargeText) 2 else 4,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(
                                onClick = {
                                    selectedCards.clear()
                                    selectedCards.addAll(finalFilteredCards)
                                }
                            ) {
                                Text("全選", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            TextButton(
                                onClick = { selectedCards.clear() }
                            ) {
                                Text("取消全選", fontSize = 12.sp)
                            }

                            Button(
                                onClick = { showMoveCardsDialog = true },
                                enabled = selectedCards.isNotEmpty() && decks.isNotEmpty(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DriveFileMove,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text("移動", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { showBulkDeleteConfirm = true },
                                enabled = selectedCards.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("bulk_delete_confirm_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text("刪除", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (finalFilteredCards.isEmpty()) {
                CalmEmptyState(
                    icon = Icons.Default.Folder,
                    title = if (searchQuery.isBlank()) "這裡還沒有單字" else "找不到符合的單字",
                    message = if (searchQuery.isBlank()) "點右下角新增或匯入。" else "換個關鍵字試試看。",
                    modifier = Modifier.padding(top = 24.dp)
                )
            } else {
                LazyColumn(
                    state = cardListState,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(displayedCards, key = { _, card -> card.id }) { _, card ->
                        val isSelected = selectedCards.contains(card)
                        ReorderableItem(
                            state = cardReorderState,
                            key = card.id
                        ) {
                            CardListItem(
                                card = card,
                                isMultiSelectMode = isMultiSelectMode,
                                isSelected = isSelected,
                                onToggleSelect = {
                                    if (isSelected) selectedCards.remove(card) else selectedCards.add(card)
                                },
                                onToggleFavorite = { onToggleFavorite(card) },
                                onDeleteCard = { onDeleteCard(card) },
                                onEditCard = { onEditCard(card) },
                                onSpeak = { onSpeak(card.word) },
                                onPracticePronunciation = { pronunciationCard = card },
                                showReorder = dragReorderEnabled,
                                dragHandleModifier = Modifier.draggableHandle()
                            )
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CardListItem(
    card: Flashcard,
    isMultiSelectMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDeleteCard: () -> Unit,
    onEditCard: () -> Unit,
    onSpeak: () -> Unit,
    onPracticePronunciation: () -> Unit,
    showReorder: Boolean,
    dragHandleModifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isMultiSelectMode, onClick = onToggleSelect)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.Top) {
                if (isMultiSelectMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect() },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = card.word,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (card.partOfSpeech.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = card.partOfSpeech,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (card.phonetic.isNotBlank()) {
                    Text(
                        text = card.phonetic,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }

                Text(
                    text = OcrWordParser.cleanDefinition(card.definition),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )

                if (card.exampleSentence.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "例: ${card.exampleSentence}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1
                    )
                }
            }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showReorder) {
                    IconButton(
                        onClick = {},
                        modifier = dragHandleModifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = "拖曳調整單字順序",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onSpeak) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "發音", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onPracticePronunciation) {
                    Icon(imageVector = Icons.Default.Mic, contentDescription = "發音練習", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (card.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (card.isFavorite) "取消收藏" else "加入收藏",
                        tint = if (card.isFavorite) Color(0xFFFFB800) else MaterialTheme.colorScheme.outline
                    )
                }
                IconButton(onClick = onEditCard) {
                    Icon(imageVector = Icons.Filled.Edit, contentDescription = "編輯", tint = MaterialTheme.colorScheme.secondary)
                }
                IconButton(onClick = onDeleteCard) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "刪除", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun CompactSelector(
    label: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier) {
        Surface(
            onClick = { onExpandedChange(true) },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    label,
                    maxLines = 1,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "展開選單")
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) { content() }
    }
}

@Composable
private fun PlainTab(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable(onClick = onClick).padding(top = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            title,
            maxLines = 1,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Surface(
            color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
            modifier = Modifier.fillMaxWidth().height(2.dp)
        ) {}
    }
}
