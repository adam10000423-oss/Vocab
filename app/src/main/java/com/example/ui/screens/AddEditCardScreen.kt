package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material3.FloatingActionButton
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.dictionary.DictionaryEntry
import com.example.data.entity.Deck
import com.example.data.entity.Flashcard
import com.example.ui.components.CalmEmptyState
import com.example.util.OcrWordParser
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch

class EditableCardState(
    originalId: Long = 0L,
    initialWord: String = "",
    initialPos: String = "",
    initialPhonetic: String = "",
    initialDef: String = "",
    initialSentence: String = "",
    initialTranslation: String = ""
) {
    var originalId by mutableStateOf(originalId)
    var word by mutableStateOf(initialWord)
    var partOfSpeech by mutableStateOf(initialPos)
    var phonetic by mutableStateOf(initialPhonetic)
    var definition by mutableStateOf(initialDef)
    var exampleSentence by mutableStateOf(initialSentence)
    var exampleTranslation by mutableStateOf(initialTranslation)
    var isAiLoading by mutableStateOf(false)
}

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun AddEditCardScreen(
    editingCard: Flashcard?,
    initialSharedText: String? = null,
    allCards: List<Flashcard>,
    decks: List<Deck>,
    onGetDictionaryMatch: (String) -> DictionaryEntry?,
    onFetchAiWordDetails: (suspend (String) -> DictionaryEntry?)? = null,
    onBatchFetchAiWordDetails: (suspend (List<String>) -> Map<String, DictionaryEntry>)? = null,
    onAddFolder: (courseName: String, folderName: String, description: String, colorHex: String) -> Unit,
    onSaveBatchCards: suspend (cardsToSave: List<Flashcard>, cardsToDelete: List<Flashcard>) -> Result<List<Long>>,
    onBack: () -> Unit,
    aiRequiresConfirmation: Boolean = true,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    if (decks.isEmpty()) {
        NoFolderForCardEditor(
            onAddFolder = onAddFolder,
            onBack = onBack,
            modifier = modifier
        )
        return
    }

    val initialDeckId = editingCard?.deckId ?: decks.first().id
    val initialDeck = decks.firstOrNull { it.id == initialDeckId }

    var selectedDeckId by remember { mutableLongStateOf(initialDeckId) }

    val courses = remember(decks) { decks.map { it.category }.distinct().ifEmpty { listOf("通用課程") } }
    var selectedCourse by remember(initialDeck, courses) {
        mutableStateOf(initialDeck?.category ?: courses.first())
    }

    val availableFoldersInCourse = remember(selectedCourse, decks) {
        decks.filter { it.category == selectedCourse }
    }

    var showAddFolderDialog by remember { mutableStateOf(false) }
    var pendingAiApply by remember { mutableStateOf<Pair<EditableCardState, DictionaryEntry>?>(null) }
    var isBatchAiLoading by remember { mutableStateOf(false) }

    fun applyAiToBlankFields(target: EditableCardState, entry: DictionaryEntry) {
        if (target.phonetic.isBlank()) target.phonetic = entry.phonetic
        if (target.partOfSpeech.isBlank()) target.partOfSpeech = entry.partOfSpeech
        if (target.definition.isBlank()) {
            target.definition = OcrWordParser.cleanDefinition(entry.definition)
        }
        if (target.exampleSentence.isBlank()) target.exampleSentence = entry.exampleSentence
        if (target.exampleTranslation.isBlank()) target.exampleTranslation = entry.exampleTranslation
    }

    fun needsAiCompletion(target: EditableCardState): Boolean =
        target.word.isNotBlank() && (
            target.phonetic.isBlank() ||
                target.partOfSpeech.isBlank() ||
                target.definition.isBlank() ||
                target.exampleSentence.isBlank() ||
                target.exampleTranslation.isBlank()
            )

    pendingAiApply?.let { (target, entry) ->
        AlertDialog(
            onDismissRequest = { pendingAiApply = null },
            title = { Text("確認套用 AI 資料") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(entry.word, fontWeight = FontWeight.Bold)
                    Text("${entry.partOfSpeech} ${entry.phonetic}".trim())
                    Text(entry.definition)
                    Text(entry.exampleSentence, style = MaterialTheme.typography.bodySmall)
                    Text("來源：${entry.category}", color = MaterialTheme.colorScheme.primary)
                }
            },
            confirmButton = {
                Button(onClick = {
                    applyAiToBlankFields(target, entry)
                    pendingAiApply = null
                }) { Text("確認套用") }
            },
            dismissButton = {
                TextButton(onClick = { pendingAiApply = null }) { Text("取消") }
            }
        )
    }

    val editableCards = remember { mutableStateListOf<EditableCardState>() }
    val originalCardsInDeck = remember { mutableListOf<Flashcard>() }
    val deletedOriginalCards = remember { mutableStateListOf<Flashcard>() }

    // Helper function to reload cards when deck changes
    fun loadCardsForDeck(deckId: Long) {
        editableCards.clear()
        originalCardsInDeck.clear()
        deletedOriginalCards.clear()

        val cardsInDeck = allCards.filter { it.deckId == deckId }
        originalCardsInDeck.addAll(cardsInDeck)

        if (cardsInDeck.isNotEmpty()) {
            cardsInDeck.forEach { card ->
                editableCards.add(
                    EditableCardState(
                        originalId = card.id,
                        initialWord = card.word,
                        initialPos = card.partOfSpeech,
                        initialPhonetic = card.phonetic,
                        initialDef = card.definition,
                        initialSentence = card.exampleSentence,
                        initialTranslation = card.exampleTranslation
                    )
                )
            }
        } else {
            // Initial blank card if folder is empty
            editableCards.add(EditableCardState())
        }
    }

    var isInitialized by remember { mutableStateOf(false) }
    var sharedTextApplied by remember(initialSharedText) { mutableStateOf(false) }
    var saveStatus by remember { mutableStateOf("所有變更會自動儲存") }

    suspend fun autoSave(): Boolean {
        if (!isInitialized) return true
        val validItems = editableCards.filter { it.word.isNotBlank() }
        val duplicates = validItems
            .groupBy { it.word.trim().lowercase() }
            .filterKeys { it.isNotBlank() }
            .filterValues { it.size > 1 }
            .keys
        if (duplicates.isNotEmpty()) {
            saveStatus = "尚未儲存：同一資料夾不能有重複單字"
            snackbarHostState.showSnackbar("重複單字：${duplicates.joinToString()}，請保留其中一張")
            return false
        }
        val topSortOrder = maxOf(
            System.currentTimeMillis() + validItems.size,
            (originalCardsInDeck.maxOfOrNull { it.sortOrder } ?: 0L) + validItems.size
        )
        val cardsToSave = validItems.mapIndexed { index, item ->
            val dict = onGetDictionaryMatch(item.word)
            val finalDef = item.definition.takeIf { it.isNotBlank() }
                ?.let(OcrWordParser::cleanDefinition)
                ?: dict?.definition.orEmpty()
            val finalSentence = item.exampleSentence.ifBlank { dict?.exampleSentence ?: "" }
            val finalPhonetic = item.phonetic.ifBlank { dict?.phonetic ?: "" }

            val original = originalCardsInDeck.firstOrNull { it.id == item.originalId }
            (original ?: Flashcard(
                id = item.originalId,
                deckId = selectedDeckId,
                word = item.word.trim(),
                definition = finalDef
            )).copy(
                deckId = selectedDeckId,
                word = item.word.trim(),
                normalizedWord = item.word.trim().lowercase(),
                partOfSpeech = item.partOfSpeech.trim(),
                phonetic = finalPhonetic,
                definition = finalDef,
                exampleSentence = finalSentence,
                exampleTranslation = item.exampleTranslation.ifBlank {
                    dict?.exampleTranslation.orEmpty()
                },
                sortOrder = topSortOrder - index
            )
        }
        saveStatus = "正在儲存…"
        return onSaveBatchCards(cardsToSave, deletedOriginalCards.toList())
            .fold(
                onSuccess = { savedIds ->
                    validItems.zip(savedIds).forEach { (item, id) -> item.originalId = id }
                    deletedOriginalCards.clear()
                    saveStatus = "已自動儲存"
                    true
                },
                onFailure = {
                    saveStatus = "儲存失敗"
                    snackbarHostState.showSnackbar(it.message ?: "自動儲存失敗")
                    false
                }
            )
    }

    LaunchedEffect(selectedDeckId) {
        isInitialized = false
        loadCardsForDeck(selectedDeckId)
        if (!sharedTextApplied && !initialSharedText.isNullOrBlank()) {
            val sharedWords = Regex("[A-Za-z]+(?:['’-][A-Za-z]+)*")
                .findAll(initialSharedText)
                .map { it.value }
                .distinctBy { it.lowercase() }
                .take(100)
                .toList()
            if (sharedWords.isNotEmpty()) {
                val firstBlank = editableCards.firstOrNull { it.word.isBlank() }
                sharedWords.forEachIndexed { index, word ->
                    if (index == 0 && firstBlank != null) firstBlank.word = word
                    else editableCards.add(EditableCardState(initialWord = word))
                }
                sharedTextApplied = true
            }
        }
        isInitialized = true
    }

    LaunchedEffect(selectedDeckId) {
        snapshotFlow {
            editableCards.toList().map {
                listOf(
                    it.word,
                    it.definition,
                    it.partOfSpeech,
                    it.phonetic,
                    it.exampleSentence,
                    it.exampleTranslation
                )
            }
        }
            .drop(1)
            .debounce(900)
            .distinctUntilChanged()
            .collectLatest {
                if (isInitialized) {
                    autoSave()
                }
            }
    }

    fun saveThenSwitchDeck(deckId: Long) {
        if (deckId == selectedDeckId) return
        coroutineScope.launch {
            if (autoSave()) selectedDeckId = deckId
        }
    }

    fun saveThenLeave() {
        coroutineScope.launch {
            if (autoSave()) onBack()
        }
    }

    BackHandler(onBack = ::saveThenLeave)

    if (showAddFolderDialog) {
        com.example.ui.components.AddFolderDialog(
            existingCourses = courses,
            defaultCourse = selectedCourse,
            onDismiss = { showAddFolderDialog = false },
            onConfirm = { cName, fName, desc, color ->
                onAddFolder(cName, fName, desc, color)
                selectedCourse = cName
                showAddFolderDialog = false
            }
        )
    }

    val currentFolder = decks.firstOrNull { it.id == selectedDeckId }
    val listState = rememberLazyListState()
    var requestedFocusCard by remember { mutableStateOf<EditableCardState?>(null) }

    fun insertCardAt(index: Int) {
        val newCard = EditableCardState(initialPos = "")
        editableCards.add(index.coerceIn(0, editableCards.size), newCard)
        requestedFocusCard = newCard
        coroutineScope.launch {
            listState.animateScrollToItem((index * 2 + 2).coerceAtLeast(0))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                expandedHeight = 48.dp,
                title = {
                    Text(
                        text = if (editingCard == null) "新增單字卡" else "編輯單字卡",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = ::saveThenLeave) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val targets = editableCards.filter(::needsAiCompletion)
                            if (targets.isEmpty()) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("沒有需要 AI 補齊的卡片")
                                }
                            } else if (!isBatchAiLoading) {
                                isBatchAiLoading = true
                                targets.forEach { it.isAiLoading = true }
                                coroutineScope.launch {
                                    runCatching {
                                        onBatchFetchAiWordDetails?.invoke(
                                            targets.map { it.word.trim() }
                                        ) ?: error("AI 一鍵補齊尚未設定")
                                    }.onSuccess { entries ->
                                        var completedCount = 0
                                        targets.forEach { target ->
                                            entries[target.word.trim().lowercase()]?.let { entry ->
                                                applyAiToBlankFields(target, entry)
                                                completedCount++
                                            }
                                        }
                                        snackbarHostState.showSnackbar(
                                            if (completedCount > 0) {
                                                "AI 已補齊 $completedCount 張；原有內容均未覆蓋"
                                            } else {
                                                "AI 沒有回傳可套用的新資料"
                                            }
                                        )
                                    }.onFailure { error ->
                                        snackbarHostState.showSnackbar(
                                            "AI 一鍵補齊失敗：${error.localizedMessage ?: "請檢查 API、模型與額度"}"
                                        )
                                    }
                                    targets.forEach { it.isAiLoading = false }
                                    isBatchAiLoading = false
                                }
                            }
                        },
                        enabled = !isBatchAiLoading &&
                            editableCards.any(::needsAiCompletion),
                        modifier = Modifier.testTag("batch_ai_complete_button")
                    ) {
                        if (isBatchAiLoading) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        }
                        Spacer(Modifier.width(4.dp))
                        Text("AI 一鍵補齊", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    insertCardAt(editableCards.size)
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.testTag("add_card_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "新增單字")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Folder & Course Selector Header
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "儲存位置",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = saveStatus,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            TextButton(onClick = { showAddFolderDialog = true }) {
                                Text("新增資料夾", fontSize = 12.sp)
                            }
                        }

                        // Course Selection Chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(courses, key = { it }) { course ->
                                val isSelected = selectedCourse == course
                                Surface(
                                    onClick = {
                                        selectedCourse = course
                                        val firstFolder = decks.firstOrNull { it.category == course }
                                        if (firstFolder != null) {
                                            saveThenSwitchDeck(firstFolder.id)
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                ) {
                                    Text(
                                        text = course,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        // Folder Chips
                        if (availableFoldersInCourse.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(availableFoldersInCourse, key = { it.id }) { folder ->
                                    val isSelected = selectedDeckId == folder.id
                                    Surface(
                                        onClick = { saveThenSwitchDeck(folder.id) },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = folder.name,
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Top Insert Button
            item {
                InsertCardDivider(
                    label = "新增到最前面",
                    onClick = { insertCardAt(0) }
                )
            }

            // Cards List
            itemsIndexed(editableCards) { index, cardState ->
                val shouldAutoFocus = requestedFocusCard === cardState

                CompactCardEditItem(
                    index = index + 1,
                    cardState = cardState,
                    autoFocus = shouldAutoFocus,
                    onFetchAiDetails = {
                        if (cardState.word.isNotBlank() && !cardState.isAiLoading) {
                            cardState.isAiLoading = true
                            coroutineScope.launch {
                                runCatching {
                                    onFetchAiWordDetails?.invoke(cardState.word)
                                }.onSuccess { aiEntry ->
                                    if (aiEntry != null) {
                                        if (aiRequiresConfirmation && aiEntry.category == "AI 補充") {
                                            pendingAiApply = cardState to aiEntry
                                        } else {
                                            applyAiToBlankFields(cardState, aiEntry)
                                            snackbarHostState.showSnackbar("已套用${aiEntry.category}資料")
                                        }
                                    } else {
                                        snackbarHostState.showSnackbar("找不到可信資料，未變更任何欄位")
                                    }
                                }.onFailure { error ->
                                    snackbarHostState.showSnackbar(
                                        "AI 補齊失敗：${error.localizedMessage ?: "請檢查 API 設定"}"
                                    )
                                }
                                cardState.isAiLoading = false
                            }
                        }
                    },
                    onDelete = {
                        if (cardState.originalId != 0L) {
                            val orig = originalCardsInDeck.firstOrNull { it.id == cardState.originalId }
                            if (orig != null) deletedOriginalCards.add(orig)
                        }
                        editableCards.removeAt(index)
                        if (editableCards.isEmpty()) {
                            editableCards.add(EditableCardState())
                        }
                    }
                )

                // Plus Insertion Button Between Cards
                InsertCardDivider(
                    label = "在此新增",
                    onClick = { insertCardAt(index + 1) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}

@Composable
private fun CompactCardEditItem(
    index: Int,
    cardState: EditableCardState,
    autoFocus: Boolean = false,
    onFetchAiDetails: () -> Unit,
    onDelete: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            focusRequester.requestFocus()
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("compact_card_edit_item_$index")
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Keep the primary input wide; compact actions remain icons.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "#$index",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Word Input
                OutlinedTextField(
                    value = cardState.word,
                    onValueChange = { cardState.word = it },
                    label = { Text("英文單字") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .testTag("compact_word_input_$index")
                )

                IconButton(
                    onClick = onFetchAiDetails,
                    enabled = !cardState.isAiLoading && cardState.word.isNotBlank(),
                    modifier = Modifier.size(44.dp)
                ) {
                    if (cardState.isAiLoading) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    } else {
                        Icon(
                            Icons.Default.AutoAwesome,
                        contentDescription = "AI 補齊",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Delete Button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "刪除",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Pronunciation belongs below the word so actions never squeeze it.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = cardState.partOfSpeech,
                    onValueChange = { cardState.partOfSpeech = it },
                    label = { Text("詞性") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(0.75f)
                )

                OutlinedTextField(
                    value = cardState.phonetic,
                    onValueChange = { cardState.phonetic = it },
                    label = { Text("音標") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1.25f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = cardState.definition,
                    onValueChange = { cardState.definition = it },
                    label = { Text("繁體中文定義") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("compact_def_input_$index")
                )

                OutlinedTextField(
                    value = cardState.exampleSentence,
                    onValueChange = { cardState.exampleSentence = it },
                    label = { Text("英文例句") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("compact_sentence_input_$index")
                )
            }

            OutlinedTextField(
                value = cardState.exampleTranslation,
                onValueChange = { cardState.exampleTranslation = it },
                label = { Text("中文例句") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun InsertCardDivider(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoFolderForCardEditor(
    onAddFolder: (String, String, String, String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        com.example.ui.components.AddFolderDialog(
            existingCourses = listOf("通用課程"),
            defaultCourse = "通用課程",
            onDismiss = { showDialog = false },
            onConfirm = { course, folder, description, color ->
                showDialog = false
                onAddFolder(course, folder, description, color)
            }
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                expandedHeight = 48.dp,
                title = { Text("手動新增單字") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            CalmEmptyState(
                icon = Icons.Default.Folder,
                title = "還沒有資料夾",
                message = "先建立資料夾，再新增單字。",
                action = {
                    Button(onClick = { showDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("建立資料夾")
                    }
                }
            )
        }
    }
}
