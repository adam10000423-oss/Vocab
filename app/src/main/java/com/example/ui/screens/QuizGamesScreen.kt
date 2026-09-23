package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import com.example.data.entity.Deck
import com.example.data.entity.Flashcard
import com.example.ui.components.CalmEmptyState
import kotlinx.coroutines.delay

private enum class QuizPlayMode(val label: String) {
    CHINESE_TO_ENGLISH("中選英"),
    ENGLISH_TO_CHINESE("英選中"),
    CHINESE_TYPE_ENGLISH("中文填英文"),
    CLOZE_CHOICE("英文克漏字選擇"),
    CLOZE_TYPING("英文克漏字填空"),
    LISTENING_CHOICE("聽力選字"),
    LISTENING_SPELLING("聽力拼字"),
    SENTENCE_ORDER("句子重組"),
    PART_OF_SPEECH("詞性選擇"),
    SYNONYM_DISCRIMINATION("近義字辨析"),
    MATCH("配對")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizGamesScreen(
    cards: List<Flashcard>,
    decks: List<Deck> = emptyList(),
    launchDeckId: Long? = null,
    launchDeckIds: Set<Long>? = null,
    launchTitle: String = "",
    onLaunchConsumed: () -> Unit = {},
    onOpenDeckManagement: (Long) -> Unit = {},
    onJumpToStudy: (Long?) -> Unit = {},
    onSpeak: (String) -> Unit = {},
    onStopSpeaking: () -> Unit = {},
    onWrongAnswer: (Flashcard) -> Unit = {},
    onGameActiveChange: (Boolean) -> Unit = {},
    onBack: (() -> Unit)? = null,
    onOpenAssistant: () -> Unit = {},
    showHubTopBar: Boolean = true,
    modifier: Modifier = Modifier
) {
    var activeGameDeckIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var activeGameTitle by remember { mutableStateOf("") }
    var selectedPlayMode by remember { mutableStateOf<QuizPlayMode?>(null) }
    var pendingGameSelection by remember {
        mutableStateOf<Pair<Set<Long>, String>?>(null)
    }
    val courses = remember(decks) { decks.map { it.category }.distinct() }
    var selectedCourse by remember(courses) { mutableStateOf<String?>(null) }
    val filteredDecks = remember(decks, selectedCourse) {
        selectedCourse?.let { course -> decks.filter { it.category == course } } ?: decks
    }

    val isGameActive = activeGameDeckIds.isNotEmpty() && selectedPlayMode != null
    BackHandler(enabled = isGameActive) {
        onStopSpeaking()
        activeGameDeckIds = emptySet()
        selectedPlayMode = null
    }
    LaunchedEffect(isGameActive) {
        onGameActiveChange(isGameActive)
    }
    DisposableEffect(Unit) {
        onDispose {
            onStopSpeaking()
            onGameActiveChange(false)
        }
    }

    LaunchedEffect(launchDeckId, launchDeckIds) {
        if (!launchDeckIds.isNullOrEmpty()) {
            val validIds = launchDeckIds.filterTo(linkedSetOf()) { requestedId ->
                decks.any { it.id == requestedId } && cards.any { it.deckId == requestedId }
            }
            if (validIds.isNotEmpty()) {
                pendingGameSelection = validIds to launchTitle.ifBlank { "全部單字卡" }
            }
            onLaunchConsumed()
        } else launchDeckId?.let { requestedId ->
            if (decks.any { it.id == requestedId } && cards.any { it.deckId == requestedId }) {
                pendingGameSelection =
                    setOf(requestedId) to (decks.firstOrNull { it.id == requestedId }?.name ?: "測驗")
            }
            onLaunchConsumed()
        }
    }

    pendingGameSelection?.let { (deckIds, title) ->
        AlertDialog(
            onDismissRequest = { pendingGameSelection = null },
            title = { Text("選擇模式", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .heightIn(max = 520.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("$title・共 ${cards.count { it.deckId in deckIds }} 張")
                    val selectedCards = cards.filter { it.deckId in deckIds }
                    QuizPlayMode.entries.forEach { mode ->
                        val eligibleCount = eligibleCardsForMode(mode, selectedCards).size
                        OutlinedButton(
                            onClick = {
                                activeGameDeckIds = deckIds
                                activeGameTitle = title
                                selectedPlayMode = mode
                                pendingGameSelection = null
                            },
                            enabled = eligibleCount > 0,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${mode.label} · $eligibleCount")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { pendingGameSelection = null }) { Text("取消") }
            }
        )
    }

    if (activeGameDeckIds.isNotEmpty() && selectedPlayMode != null) {
        val deckCards = cards.filter { it.deckId in activeGameDeckIds }
        val deckTitle = activeGameTitle.ifBlank { "單字測驗" }

        Scaffold(
            topBar = {
                TopAppBar(
                    expandedHeight = 48.dp,
                    title = {
                        Text(deckTitle, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    },
                    actions = {
                        OutlinedButton(
                            onClick = {
                                // Exit midway and save/return to folder selection list
                                onStopSpeaking()
                                activeGameDeckIds = emptySet()
                                selectedPlayMode = null
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.padding(end = 12.dp).testTag("exit_game_midway_button")
                        ) {
                            Text("退出", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            },
            modifier = modifier
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                if (deckCards.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CalmEmptyState(
                            icon = Icons.Default.Folder,
                            title = "沒有可測驗的單字",
                            message = "先新增單字卡再回來。"
                        )
                    }
                } else {
                    when (selectedPlayMode) {
                        QuizPlayMode.MATCH -> DynamicWordMatchingGameView(
                            cards = deckCards,
                            wrongCardsPool = emptyList(),
                            onAddWrongCard = onWrongAnswer
                        )
                        null -> Unit
                        else -> IndependentQuizView(
                            mode = requireNotNull(selectedPlayMode),
                            cards = deckCards,
                            onWrongAnswer = onWrongAnswer,
                            onSpeak = onSpeak,
                            onFinish = {
                                onStopSpeaking()
                                activeGameDeckIds = emptySet()
                                selectedPlayMode = null
                            }
                        )
                    }
                }
            }
        }
        return
    }

    // Default View: Folder list (identical in structure to StudyHubScreen and FoldersManagementScreen)
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (showHubTopBar) TopAppBar(
                expandedHeight = 48.dp,
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("測驗", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
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
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (decks.isEmpty()) {
                item {
                    CalmEmptyState(
                        icon = Icons.Default.Folder,
                        title = "還沒有資料夾",
                        message = "先建立資料夾，再開始測驗。"
                    )
                }
            } else {
                item {
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
                            val isSelected = selectedCourse == null
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { selectedCourse = null }
                            ) {
                                Text(
                                    text = "全部資料夾 (${decks.size})",
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                        items(courses) { course ->
                            val isSelected = selectedCourse == course
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { selectedCourse = course }
                            ) {
                                Text(
                                    text = "$course (${decks.count { it.category == course }})",
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                    val courseIds = filteredDecks.map { it.id }.toSet()
                    val courseCount = cards.count { it.deckId in courseIds }
                    Button(
                        onClick = {
                            pendingGameSelection = courseIds to
                                (selectedCourse?.let { "$it・全部資料夾" } ?: "全部資料夾")
                        },
                        enabled = courseCount > 0,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Extension, contentDescription = null)
                        Text("測驗全部 · $courseCount")
                    }
                }
                items(filteredDecks) { deck ->
                    val deckCards = cards.filter { it.deckId == deck.id }
                    val totalCards = deckCards.size
                    val masteredCount = deckCards.count { it.isMastered }
                    val dueCount = deckCards.count { it.nextReviewTimestamp <= System.currentTimeMillis() && !it.isMastered && !it.isSuspended }
                    val progressRatio = if (totalCards > 0) masteredCount.toFloat() / totalCards.toFloat() else 0f

                    val deckColor = try {
                        Color(android.graphics.Color.parseColor(deck.colorHex))
                    } catch (e: Exception) {
                        MaterialTheme.colorScheme.primary
                    }

                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
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
                                            .size(14.dp)
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

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = "$totalCards 張",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }

                            // Action buttons toolbar matching Tab 2 and Tab 3 with polished icons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { onOpenDeckManagement(deck.id) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("管理", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { onJumpToStudy(deck.id) },
                                    enabled = totalCards > 0,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("學習", fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        pendingGameSelection = setOf(deck.id) to deck.name
                                    },
                                    enabled = totalCards > 0,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1.2f).testTag("select_game_deck_${deck.id}")
                                ) {
                                    Text("測驗", fontWeight = FontWeight.Bold)
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

private fun clozeFor(card: Flashcard): String? {
    val sentence = card.exampleSentence.trim()
    if (sentence.isBlank() || card.word.isBlank()) return null
    val regex = Regex(
        "(?<![A-Za-z])${Regex.escape(card.word.trim())}(?![A-Za-z])",
        RegexOption.IGNORE_CASE
    )
    if (!regex.containsMatchIn(sentence)) return null
    return regex.replace(sentence, "____")
}

private fun definitionTerms(card: Flashcard): Set<String> = card.definition
    .split('；', '／', '、', '，', ',', ';', '/')
    .map { it.trim().replace(Regex("[（）()\\s]"), "") }
    .filter { it.length >= 2 }
    .toSet()

private fun areMeaningRelated(first: Flashcard, second: Flashcard): Boolean {
    val firstTerms = definitionTerms(first)
    val secondTerms = definitionTerms(second)
    return firstTerms.any { left ->
        secondTerms.any { right -> left == right || left.contains(right) || right.contains(left) }
    }
}

private fun nearMeaningCards(card: Flashcard, cards: List<Flashcard>): List<Flashcard> =
    cards.filter { other ->
        other.id != card.id &&
            other.word.isNotBlank() &&
            other.definition.isNotBlank() &&
            areMeaningRelated(card, other)
    }

private fun eligibleCardsForMode(
    mode: QuizPlayMode,
    cards: List<Flashcard>
): List<Flashcard> {
    val realCards = cards.distinctBy { it.id }.filter { it.word.isNotBlank() }
    val distinctWords = realCards.map { it.word.trim().lowercase() }.distinct().size
    val distinctDefinitions = realCards.map { it.definition.trim() }.filter { it.isNotBlank() }.distinct().size
    val distinctParts = realCards.map { it.partOfSpeech.trim() }.filter { it.isNotBlank() }.distinct().size
    return when (mode) {
        QuizPlayMode.CHINESE_TO_ENGLISH ->
            if (distinctWords >= 2) realCards.filter { it.definition.isNotBlank() } else emptyList()
        QuizPlayMode.ENGLISH_TO_CHINESE ->
            if (distinctDefinitions >= 2) realCards.filter { it.definition.isNotBlank() } else emptyList()
        QuizPlayMode.CHINESE_TYPE_ENGLISH -> realCards.filter { it.definition.isNotBlank() }
        QuizPlayMode.CLOZE_CHOICE ->
            if (distinctWords >= 2) realCards.filter { clozeFor(it) != null } else emptyList()
        QuizPlayMode.CLOZE_TYPING -> realCards.filter { clozeFor(it) != null }
        QuizPlayMode.LISTENING_CHOICE -> if (distinctWords >= 2) realCards else emptyList()
        QuizPlayMode.LISTENING_SPELLING -> realCards
        QuizPlayMode.SENTENCE_ORDER -> realCards.filter {
            it.exampleSentence.trim().split(Regex("\\s+")).size >= 3
        }
        QuizPlayMode.PART_OF_SPEECH ->
            if (distinctParts >= 2) realCards.filter { it.partOfSpeech.isNotBlank() } else emptyList()
        QuizPlayMode.SYNONYM_DISCRIMINATION -> realCards.filter {
            it.definition.isNotBlank() && nearMeaningCards(it, realCards).isNotEmpty()
        }
        QuizPlayMode.MATCH -> if (realCards.size >= 2) realCards else emptyList()
    }
}

private fun choiceOptions(
    mode: QuizPlayMode,
    card: Flashcard,
    cards: List<Flashcard>
): List<String> {
    val correct = when (mode) {
        QuizPlayMode.ENGLISH_TO_CHINESE -> card.definition.trim()
        QuizPlayMode.PART_OF_SPEECH -> card.partOfSpeech.trim()
        else -> card.word.trim()
    }
    val distractors = when (mode) {
        QuizPlayMode.ENGLISH_TO_CHINESE -> cards.asSequence()
            .filter { it.id != card.id }
            .map { it.definition.trim() }
            .filter { it.isNotBlank() && it != correct }
            .distinct()
            .toList()
        QuizPlayMode.PART_OF_SPEECH -> cards.asSequence()
            .filter { it.id != card.id }
            .map { it.partOfSpeech.trim() }
            .filter { it.isNotBlank() && it != correct }
            .distinct()
            .toList()
        QuizPlayMode.SYNONYM_DISCRIMINATION -> {
            val related = nearMeaningCards(card, cards)
            val remaining = cards.filter { other ->
                other.id != card.id && other !in related && other.word.isNotBlank()
            }
            (related + remaining).map { it.word.trim() }.filter { it != correct }.distinct()
        }
        else -> cards.asSequence()
            .filter { it.id != card.id }
            .map { it.word.trim() }
            .filter { it.isNotBlank() && !it.equals(correct, ignoreCase = true) }
            .distinctBy { it.lowercase() }
            .toList()
    }
    return (distractors.shuffled().take(3) + correct).distinct().shuffled()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IndependentQuizView(
    mode: QuizPlayMode,
    cards: List<Flashcard>,
    onWrongAnswer: (Flashcard) -> Unit,
    onSpeak: (String) -> Unit,
    onFinish: () -> Unit
) {
    // Keep one stable shuffled question order for the whole play session. Wrong-answer
    // persistence updates card fields in the parent state; using a card hash here made
    // that update rebuild and reshuffle the list, which looked like an automatic skip.
    val questionSetIds = cards.map { it.id }.sorted()
    val eligible = remember(mode, questionSetIds) {
        eligibleCardsForMode(mode, cards).shuffled()
    }
    var questionIndex by remember(mode, eligible.map { it.id }) { mutableIntStateOf(0) }
    var correctCount by remember(mode, eligible.map { it.id }) { mutableIntStateOf(0) }
    var selectedOption by remember(mode, questionIndex) { mutableStateOf<String?>(null) }
    var typedAnswer by remember(mode, questionIndex) { mutableStateOf("") }
    var answerResult by remember(mode, questionIndex) { mutableStateOf<Boolean?>(null) }
    var wrongReported by remember(mode, questionIndex) { mutableStateOf(false) }
    var selectedTokenIndices by remember(mode, questionIndex) { mutableStateOf<List<Int>>(emptyList()) }
    var canAdvance by remember(mode, questionIndex) { mutableStateOf(false) }

    LaunchedEffect(answerResult, questionIndex) {
        canAdvance = false
        if (answerResult != null) {
            delay(450)
            canAdvance = true
        }
    }

    if (eligible.isEmpty()) {
        CalmEmptyState(
            icon = Icons.Default.Lightbulb,
            title = "沒有符合條件的題目",
            message = when (mode) {
                QuizPlayMode.CLOZE_CHOICE, QuizPlayMode.CLOZE_TYPING -> "需要包含目標單字的完整英文例句。"
                QuizPlayMode.PART_OF_SPEECH -> "需要至少兩種不同的已填詞性。"
                QuizPlayMode.SYNONYM_DISCRIMINATION -> "需要至少兩張中文意思相近的卡片。"
                else -> "請先補齊此題型需要的卡片資料。"
            }
        )
        return
    }

    if (questionIndex >= eligible.size) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(60.dp)
                    )
                    Text(mode.label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "答對 $correctCount / ${eligible.size} 題",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
                        Text("返回測驗")
                    }
                }
            }
        }
        return
    }

    val card = eligible[questionIndex]
    val isListening = mode == QuizPlayMode.LISTENING_CHOICE ||
        mode == QuizPlayMode.LISTENING_SPELLING
    val isChoice = mode in setOf(
        QuizPlayMode.CHINESE_TO_ENGLISH,
        QuizPlayMode.ENGLISH_TO_CHINESE,
        QuizPlayMode.CLOZE_CHOICE,
        QuizPlayMode.LISTENING_CHOICE,
        QuizPlayMode.PART_OF_SPEECH,
        QuizPlayMode.SYNONYM_DISCRIMINATION
    )
    val options = remember(mode, card.id, cards.map { it.id }) {
        if (isChoice) choiceOptions(mode, card, cards) else emptyList()
    }
    val sentenceTokens = remember(card.id) {
        card.exampleSentence.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    }
    val shuffledTokenIndices = remember(card.id) {
        val normal = sentenceTokens.indices.toList()
        normal.shuffled().let { shuffled ->
            if (shuffled == normal && shuffled.size > 1) shuffled.drop(1) + shuffled.first()
            else shuffled
        }
    }

    LaunchedEffect(card.id, isListening) {
        if (isListening) onSpeak(card.word)
    }

    fun expectedAnswer(): String = when (mode) {
        QuizPlayMode.ENGLISH_TO_CHINESE -> card.definition.trim()
        QuizPlayMode.PART_OF_SPEECH -> card.partOfSpeech.trim()
        QuizPlayMode.SENTENCE_ORDER -> sentenceTokens.joinToString(" ")
        else -> card.word.trim()
    }

    fun submittedAnswer(): String = when {
        isChoice -> selectedOption.orEmpty()
        mode == QuizPlayMode.SENTENCE_ORDER ->
            selectedTokenIndices.joinToString(" ") { sentenceTokens[it] }
        else -> typedAnswer
    }

    fun submit() {
        if (answerResult != null) return
        val correct = submittedAnswer().trim().equals(expectedAnswer(), ignoreCase = true)
        answerResult = correct
        if (correct) correctCount++
        else if (!wrongReported) {
            wrongReported = true
            onWrongAnswer(card)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(mode.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "${questionIndex + 1} / ${eligible.size}",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        LinearProgressIndicator(
            progress = { questionIndex.toFloat() / eligible.size.coerceAtLeast(1) },
            modifier = Modifier.fillMaxWidth()
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = if (isListening) Alignment.CenterHorizontally else Alignment.Start
                ) {
                    when (mode) {
                        QuizPlayMode.CHINESE_TO_ENGLISH,
                        QuizPlayMode.CHINESE_TYPE_ENGLISH -> {
                            Text("中文提示", color = MaterialTheme.colorScheme.primary)
                            Text(card.definition, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        QuizPlayMode.ENGLISH_TO_CHINESE -> {
                            Text("選擇正確中文", color = MaterialTheme.colorScheme.primary)
                            Text(card.word, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            card.exampleSentence.takeIf { it.isNotBlank() }?.let { Text(it) }
                        }
                        QuizPlayMode.CLOZE_CHOICE,
                        QuizPlayMode.CLOZE_TYPING,
                        QuizPlayMode.SYNONYM_DISCRIMINATION -> {
                            Text(
                                if (mode == QuizPlayMode.SYNONYM_DISCRIMINATION) "從相近單字中選出最符合者" else "完成例句",
                                color = MaterialTheme.colorScheme.primary
                            )
                            clozeFor(card)?.let {
                                Text(it, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Text(card.definition, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        QuizPlayMode.LISTENING_CHOICE,
                        QuizPlayMode.LISTENING_SPELLING -> {
                            Text("聽發音作答", color = MaterialTheme.colorScheme.primary)
                            IconButton(onClick = { onSpeak(card.word) }, modifier = Modifier.size(64.dp)) {
                                Icon(
                                    Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "重新播放",
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                            Text("點喇叭可以重播", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        QuizPlayMode.SENTENCE_ORDER -> {
                            Text("依照正確順序重組例句", color = MaterialTheme.colorScheme.primary)
                            Text(
                                card.exampleTranslation.ifBlank { card.definition },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        QuizPlayMode.PART_OF_SPEECH -> {
                            Text("選擇詞性", color = MaterialTheme.colorScheme.primary)
                            Text(card.word, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            card.exampleSentence.takeIf { it.isNotBlank() }?.let { Text(it) }
                        }
                        QuizPlayMode.MATCH -> Unit
                    }
                }
            }

            if (isChoice) {
                options.forEach { option ->
                    val selected = selectedOption == option
                    val correctOption = option.equals(expectedAnswer(), ignoreCase = true)
                    OutlinedButton(
                        onClick = { if (answerResult == null) selectedOption = option },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = when {
                                answerResult != null && correctOption -> MaterialTheme.colorScheme.primaryContainer
                                answerResult == false && selected -> MaterialTheme.colorScheme.errorContainer
                                selected -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.surface
                            }
                        ),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(option, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                    }
                }
            } else if (mode == QuizPlayMode.SENTENCE_ORDER) {
                val compactFontSize = when {
                    sentenceTokens.size >= 18 -> 10.sp
                    sentenceTokens.size >= 12 -> 11.sp
                    else -> 13.sp
                }
                Text("排列結果・點單字可放回下方", fontWeight = FontWeight.Bold)
                Surface(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 112.dp, max = 180.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f)
                ) {
                    FlowRow(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (selectedTokenIndices.isEmpty()) {
                            Text(
                                "從下方選擇單字",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        selectedTokenIndices.forEach { tokenIndex ->
                            CompactWordChip(
                                text = sentenceTokens[tokenIndex],
                                fontSize = compactFontSize,
                                enabled = answerResult == null,
                                onClick = {
                                    selectedTokenIndices = selectedTokenIndices.filterNot { it == tokenIndex }
                                }
                            )
                        }
                    }
                }

                Text("可選單字", fontWeight = FontWeight.Bold)
                Surface(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 130.dp, max = 210.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                ) {
                    FlowRow(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        shuffledTokenIndices
                            .filter { it !in selectedTokenIndices }
                            .forEach { tokenIndex ->
                                CompactWordChip(
                                    text = sentenceTokens[tokenIndex],
                                    fontSize = compactFontSize,
                                    enabled = answerResult == null,
                                    onClick = {
                                        selectedTokenIndices = selectedTokenIndices + tokenIndex
                                    }
                                )
                            }
                    }
                }
            } else {
                OutlinedTextField(
                    value = typedAnswer,
                    onValueChange = { if (answerResult == null) typedAnswer = it },
                    label = {
                        Text(if (mode == QuizPlayMode.LISTENING_SPELLING) "輸入聽到的英文" else "輸入完整英文單字")
                    },
                    supportingText = { Text("不分大小寫，但必須完整輸入") },
                    singleLine = true,
                    enabled = answerResult == null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = when (answerResult) {
                true -> MaterialTheme.colorScheme.primaryContainer
                false -> MaterialTheme.colorScheme.errorContainer
                null -> MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    when (answerResult) {
                        true -> "答對了"
                        false -> "答錯了，答案：${expectedAnswer()}"
                        null -> "完成後確認答案"
                    },
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold
                )
                if (answerResult == null) {
                    Button(
                        onClick = ::submit,
                        enabled = when {
                            isChoice -> selectedOption != null
                            mode == QuizPlayMode.SENTENCE_ORDER ->
                                selectedTokenIndices.size == sentenceTokens.size
                            else -> typedAnswer.isNotBlank()
                        }
                    ) { Text("確認") }
                } else {
                    Button(
                        onClick = { questionIndex++ },
                        enabled = canAdvance
                    ) {
                        Text(if (questionIndex == eligible.lastIndex) "查看結果" else "下一題")
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactWordChip(
    text: String,
    fontSize: TextUnit,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(9.dp),
        color = if (enabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            lineHeight = (fontSize.value + 2).sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
        )
    }
}

private enum class ChallengeStage {
    MULTIPLE_CHOICE,
    MULTIPLE_CHOICE_RETRY,
    TYPING,
    FINAL_REVIEW,
    COMPLETE
}

private enum class ChallengeQuestionKind { MULTIPLE_CHOICE, TYPING }

private data class ChallengeQuestion(
    val cardId: Long,
    val kind: ChallengeQuestionKind
)

@Composable
private fun ThreeStageVocabularyGame(
    cards: List<Flashcard>,
    onWrongAnswer: (Flashcard) -> Unit,
    onSpeak: (String) -> Unit,
    onFinish: () -> Unit
) {
    val shuffledCards = remember(cards.map { it.id }) { cards.shuffled() }

    var stage by remember(cards.map { it.id }) { mutableStateOf(ChallengeStage.MULTIPLE_CHOICE) }
    var queue by remember(cards.map { it.id }) {
        mutableStateOf(shuffledCards.map {
            ChallengeQuestion(it.id, ChallengeQuestionKind.MULTIPLE_CHOICE)
        })
    }
    var stageTotal by remember(cards.map { it.id }) { mutableIntStateOf(shuffledCards.size) }
    var firstPartWrong by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var firstPartStillWrong by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var secondPartWrong by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var answerText by remember { mutableStateOf("") }
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var answeredCorrectly by remember { mutableStateOf<Boolean?>(null) }
    var questionHadWrongAttempt by remember { mutableStateOf(false) }
    var questionWrongReported by remember { mutableStateOf(false) }
    var canAdvance by remember { mutableStateOf(false) }

    val currentQuestion = queue.firstOrNull()
    val currentCard = currentQuestion?.let { question ->
        cards.firstOrNull { it.id == question.cardId }
    }

    LaunchedEffect(answeredCorrectly, currentQuestion?.cardId) {
        canAdvance = false
        if (answeredCorrectly != null) {
            delay(450)
            canAdvance = true
        }
    }
    val options = remember(currentCard?.id, stage) {
        currentCard?.let { card ->
            (cards.asSequence()
                .filter { it.id != card.id }
                .map { it.word }
                .distinct()
                .shuffled()
                .take(3)
                .toList() + card.word).shuffled()
        }.orEmpty()
    }

    fun startTypingStage(stillWrongIds: Set<Long> = firstPartStillWrong) {
        if (shuffledCards.isEmpty()) {
            val finalQuestions = stillWrongIds
                .map { ChallengeQuestion(it, ChallengeQuestionKind.MULTIPLE_CHOICE) }
                .shuffled()
            if (finalQuestions.isEmpty()) {
                stage = ChallengeStage.COMPLETE
                queue = emptyList()
            } else {
                stage = ChallengeStage.FINAL_REVIEW
                queue = finalQuestions
                stageTotal = finalQuestions.size
            }
        } else {
            stage = ChallengeStage.TYPING
            queue = shuffledCards.map { ChallengeQuestion(it.id, ChallengeQuestionKind.TYPING) }
            stageTotal = shuffledCards.size
        }
    }

    fun advanceAfterAnswer() {
        val question = currentQuestion ?: return
        val wasCorrect = answeredCorrectly == true && !questionHadWrongAttempt
        val remaining = queue.drop(1)

        when (stage) {
            ChallengeStage.MULTIPLE_CHOICE -> {
                val updatedWrong = if (wasCorrect) firstPartWrong else firstPartWrong + question.cardId
                firstPartWrong = updatedWrong
                if (remaining.isNotEmpty()) {
                    queue = remaining
                } else if (updatedWrong.isNotEmpty()) {
                    stage = ChallengeStage.MULTIPLE_CHOICE_RETRY
                    queue = updatedWrong
                        .map { ChallengeQuestion(it, ChallengeQuestionKind.MULTIPLE_CHOICE) }
                        .shuffled()
                    stageTotal = updatedWrong.size
                } else {
                    startTypingStage()
                }
            }

            ChallengeStage.MULTIPLE_CHOICE_RETRY -> {
                val updatedStillWrong =
                    if (wasCorrect) firstPartStillWrong else firstPartStillWrong + question.cardId
                firstPartStillWrong = updatedStillWrong
                if (remaining.isNotEmpty()) queue = remaining else startTypingStage(updatedStillWrong)
            }

            ChallengeStage.TYPING -> {
                val updatedSecondWrong =
                    if (wasCorrect) secondPartWrong else secondPartWrong + question.cardId
                secondPartWrong = updatedSecondWrong
                if (remaining.isNotEmpty()) {
                    queue = remaining
                } else {
                    val finalQuestions = (
                        firstPartStillWrong.map {
                            ChallengeQuestion(it, ChallengeQuestionKind.MULTIPLE_CHOICE)
                        } +
                            updatedSecondWrong.map {
                                ChallengeQuestion(it, ChallengeQuestionKind.TYPING)
                            }
                        ).shuffled()
                    if (finalQuestions.isEmpty()) {
                        stage = ChallengeStage.COMPLETE
                        queue = emptyList()
                    } else {
                        stage = ChallengeStage.FINAL_REVIEW
                        queue = finalQuestions
                        stageTotal = finalQuestions.size
                    }
                }
            }

            ChallengeStage.FINAL_REVIEW -> {
                queue = if (wasCorrect) {
                    remaining
                } else {
                    (remaining + question).shuffled()
                }
                stageTotal = maxOf(stageTotal, queue.size)
                if (wasCorrect && remaining.isEmpty()) {
                    stage = ChallengeStage.COMPLETE
                }
            }

            ChallengeStage.COMPLETE -> Unit
        }

        answerText = ""
        selectedOption = null
        answeredCorrectly = null
        questionHadWrongAttempt = false
        questionWrongReported = false
    }

    fun submitAnswer(answer: String) {
        val card = currentCard ?: return
        if (answeredCorrectly != null) return
        val correct = answer.trim().equals(card.word.trim(), ignoreCase = true)
        answeredCorrectly = correct
        if (!correct) {
            questionHadWrongAttempt = true
            if (!questionWrongReported) {
                questionWrongReported = true
                onWrongAnswer(card)
            }
        }
    }

    if (stage == ChallengeStage.COMPLETE) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                    Text("全部答對了", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
                        Text("返回測驗")
                    }
                }
            }
        }
        return
    }

    if (currentCard == null || currentQuestion == null) return

    val stageTitle = when (stage) {
        ChallengeStage.MULTIPLE_CHOICE -> "第一部分・選擇題"
        ChallengeStage.MULTIPLE_CHOICE_RETRY -> "第一部分錯題・再答一次"
        ChallengeStage.TYPING -> "第二部分・自行輸入"
        ChallengeStage.FINAL_REVIEW -> "第三部分・錯題循環"
        ChallengeStage.COMPLETE -> ""
    }
    val clozeSentence = remember(currentCard.id) {
        currentCard.exampleSentence.takeIf { it.isNotBlank() }?.let { sentence ->
            Regex(
                "(?<![A-Za-z])${Regex.escape(currentCard.word)}(?![A-Za-z])",
                RegexOption.IGNORE_CASE
            )
                .replace(sentence, "____")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stageTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                if (stage == ChallengeStage.FINAL_REVIEW) {
                    "待通過 ${queue.size} 題"
                } else {
                    "第 ${(stageTotal - queue.size + 1).coerceAtLeast(1)} / $stageTotal 題"
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        LinearProgressIndicator(
            progress = {
                if (stageTotal <= 0) 0f
                else ((stageTotal - queue.size).toFloat() / stageTotal.toFloat()).coerceIn(0f, 1f)
            },
            modifier = Modifier.fillMaxWidth()
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "中文提示",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { onSpeak(currentCard.word) }) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "播放發音")
                        }
                    }
                    Text(
                        currentCard.definition,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    clozeSentence?.let {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                it,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (currentQuestion.kind == ChallengeQuestionKind.MULTIPLE_CHOICE) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEachIndexed { optionIndex, option ->
                        val isSelected = selectedOption == option
                        val isCorrectOption = option.equals(currentCard.word, ignoreCase = true)
                        val checked = answeredCorrectly != null
                        OutlinedButton(
                            onClick = {
                                selectedOption = option
                                if (checked) {
                                    answeredCorrectly = isCorrectOption
                                    if (!isCorrectOption) {
                                        questionHadWrongAttempt = true
                                        if (!questionWrongReported) {
                                            questionWrongReported = true
                                            onWrongAnswer(currentCard)
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = when {
                                    checked && isCorrectOption ->
                                        MaterialTheme.colorScheme.primaryContainer
                                    checked && isSelected && !isCorrectOption ->
                                        MaterialTheme.colorScheme.errorContainer
                                    isSelected -> MaterialTheme.colorScheme.secondaryContainer
                                    else -> MaterialTheme.colorScheme.surface
                                },
                                contentColor = when {
                                    checked && isCorrectOption ->
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    checked && isSelected && !isCorrectOption ->
                                        MaterialTheme.colorScheme.onErrorContainer
                                    isSelected -> MaterialTheme.colorScheme.onSecondaryContainer
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Text(
                                "${('A'.code + optionIndex).toChar()}  $option",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = answerText,
                    onValueChange = {
                        if (answeredCorrectly == null) answerText = it
                    },
                    label = { Text("輸入完整英文單字") },
                    supportingText = { Text("不分大小寫，必須輸入完整單字") },
                    singleLine = true,
                    enabled = answeredCorrectly == null,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            tonalElevation = 2.dp,
            color = when (answeredCorrectly) {
                true -> MaterialTheme.colorScheme.primaryContainer
                false -> MaterialTheme.colorScheme.errorContainer
                null -> MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (val correct = answeredCorrectly) {
                    null -> {
                        Text(
                            if (currentQuestion.kind == ChallengeQuestionKind.MULTIPLE_CHOICE) {
                                selectedOption?.let { "已選擇 $it" } ?: "選一個答案"
                            } else {
                                "輸入後確認答案"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (currentQuestion.kind == ChallengeQuestionKind.MULTIPLE_CHOICE) {
                                    selectedOption?.let(::submitAnswer)
                                } else {
                                    submitAnswer(answerText)
                                }
                            },
                            enabled = if (
                                currentQuestion.kind == ChallengeQuestionKind.MULTIPLE_CHOICE
                            ) selectedOption != null else answerText.isNotBlank()
                        ) {
                            Text("確認答案", fontWeight = FontWeight.Bold)
                        }
                    }

                    else -> {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (correct) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (correct) "答對了" else "答錯了",
                                fontWeight = FontWeight.Bold,
                                color = if (correct) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onErrorContainer
                                }
                            )
                            if (!correct) {
                                Text(
                                    "答案  ${currentCard.word}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            } else if (questionHadWrongAttempt) {
                                Text(
                                    "已修正，稍後會再練一次",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Button(
                            onClick = ::advanceAfterAnswer,
                            enabled = canAdvance,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                when {
                                    queue.size > 1 -> "下一題"
                                    stage == ChallengeStage.FINAL_REVIEW &&
                                        questionHadWrongAttempt -> "再練一次"
                                    stage == ChallengeStage.FINAL_REVIEW -> "完成測驗"
                                    else -> "下一階段"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpellingQuizView(
    cards: List<Flashcard>,
    wrongCardsPool: List<Flashcard>,
    onAddWrongCard: (Flashcard) -> Unit,
    onWrongAnswer: (Flashcard) -> Unit,
    onSpeak: (String) -> Unit
) {
    // Weighted queue by error frequency
    val adaptiveQueue = remember(cards, wrongCardsPool.size) {
        val list = cards.toMutableList()
        if (wrongCardsPool.isNotEmpty()) {
            list.addAll(wrongCardsPool) // Higher frequency for wrong cards
        }
        list.shuffled()
    }

    var quizIndex by remember { mutableIntStateOf(0) }
    var userInput by remember { mutableStateOf("") }
    var isAnswered by remember { mutableStateOf(false) }
    var isCorrect by remember { mutableStateOf(false) }
    var streakCount by remember { mutableIntStateOf(0) }
    var hintLettersCount by remember { mutableIntStateOf(0) }

    val currentCard = adaptiveQueue[quizIndex % adaptiveQueue.size]

    fun checkAnswer() {
        if (userInput.trim().lowercase() == currentCard.word.lowercase()) {
            isCorrect = true
            streakCount++
        } else {
            isCorrect = false
            streakCount = 0
            onAddWrongCard(currentCard)
            onWrongAnswer(currentCard)
        }
        isAnswered = true
        onSpeak(currentCard.word)
    }

    fun nextQuestion() {
        quizIndex++
        userInput = ""
        isAnswered = false
        hintLettersCount = 0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "第 ${quizIndex + 1} 題",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Surface(
                color = if (streakCount >= 3) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "連勝 $streakCount",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (streakCount >= 3) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = currentCard.definition,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (currentCard.phonetic.isNotBlank()) {
                    Text(
                        text = currentCard.phonetic,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = { onSpeak(currentCard.word) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "發音", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        if (hintLettersCount > 0) {
            val hintText = currentCard.word.take(hintLettersCount) + "_".repeat((currentCard.word.length - hintLettersCount).coerceAtLeast(0))
            Text(
                text = "提示開頭：$hintText",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.tertiary
            )
        }

        OutlinedTextField(
            value = userInput,
            onValueChange = { userInput = it },
            label = { Text("輸入英文單字") },
            singleLine = true,
            enabled = !isAnswered,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("spelling_input_field")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { if (hintLettersCount < currentCard.word.length) hintLettersCount++ },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("提示")
            }

            Button(
                onClick = { if (!isAnswered) checkAnswer() else nextQuestion() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAnswered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f).testTag("spelling_submit_button")
            ) {
                Text(
                    text = if (!isAnswered) "確認" else "下一題",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (isAnswered) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCorrect) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = if (isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isCorrect) "答對了" else "正確拼法：${currentCard.word}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isCorrect) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onErrorContainer
                        )
                    )
                }
            }
        }
    }
}

/**
 * Dynamic Word Matching Game View with continuous stream of cards based on queue.
 * Rule: 每答對一個框框就會自動消失並換成下一個單字，直到全部單字配對完成。
 */
@Composable
private fun DynamicWordMatchingGameView(
    cards: List<Flashcard>,
    wrongCardsPool: List<Flashcard>,
    onAddWrongCard: (Flashcard) -> Unit
) {
    val emptySlot = 0L
    val eligibleCards = cards
        .filter { it.id != emptySlot && it.word.isNotBlank() && it.definition.isNotBlank() }
        .distinctBy { it.id }
    val cardSetKey = eligibleCards.map { it.id }.sorted()
    val cardById = eligibleCards.associateBy { it.id }
    val wrongIds = wrongCardsPool.mapTo(mutableSetOf()) { it.id }
    val shakeOffset = remember { Animatable(0f) }
    val shakeDistance = with(LocalDensity.current) { 8.dp.toPx() }

    fun definitionKey(id: Long): String = cardById[id]?.definition
        ?.trim()
        ?.lowercase()
        .orEmpty()

    fun areCompatible(wordId: Long, definitionId: Long): Boolean =
        wordId != emptySlot && definitionId != emptySlot &&
            definitionKey(wordId) == definitionKey(definitionId)

    fun paddedSlots(ids: List<Long>): ArrayList<Long> = ArrayList<Long>(4).apply {
        addAll(ids.take(4))
        while (size < 4) add(emptySlot)
    }

    fun buildInitialRightIds(order: List<Long>, leftIds: List<Long>): List<Long> {
        if (leftIds.isEmpty()) return emptyList()
        val result = mutableListOf(leftIds.first())
        result += order.drop(4).filterNot { it in result }.take(3)
        result += leftIds.drop(1).filterNot { it in result }.take(4 - result.size)
        return result.shuffled()
    }

    val initialOrder = rememberSaveable(cardSetKey) {
        val prioritized = eligibleCards.shuffled().sortedByDescending {
            it.id in wrongIds || it.mistakeCount > 0
        }
        ArrayList(prioritized.map { it.id })
    }
    val initialLeftIds = initialOrder.take(4)
    val initialRightOrder = rememberSaveable(cardSetKey) {
        ArrayList(buildInitialRightIds(initialOrder, initialLeftIds))
    }

    var leftSlots by rememberSaveable(cardSetKey) {
        mutableStateOf(paddedSlots(initialLeftIds))
    }
    var rightSlots by rememberSaveable(cardSetKey) {
        mutableStateOf(paddedSlots(initialRightOrder))
    }
    var leftQueue by rememberSaveable(cardSetKey) {
        mutableStateOf(ArrayList(initialOrder.filterNot { it in initialLeftIds }))
    }
    var rightQueue by rememberSaveable(cardSetKey) {
        mutableStateOf(ArrayList(initialOrder.filterNot { it in initialRightOrder }))
    }
    var matchedCount by rememberSaveable(cardSetKey) { mutableIntStateOf(0) }
    var wrongAttempts by rememberSaveable(cardSetKey) { mutableIntStateOf(0) }
    var startedAt by rememberSaveable(cardSetKey) { mutableStateOf(System.currentTimeMillis()) }
    var selectedWordId by rememberSaveable(cardSetKey) { mutableStateOf<Long?>(null) }
    var selectedDefinitionId by rememberSaveable(cardSetKey) { mutableStateOf<Long?>(null) }
    var wrongWordId by rememberSaveable(cardSetKey) { mutableStateOf<Long?>(null) }
    var wrongDefinitionId by rememberSaveable(cardSetKey) { mutableStateOf<Long?>(null) }
    var wrongFeedbackVersion by rememberSaveable(cardSetKey) { mutableIntStateOf(0) }
    val totalCount = eligibleCards.size

    fun boardHasMatch(left: List<Long>, right: List<Long>): Boolean =
        left.any { wordId -> right.any { definitionId -> areCompatible(wordId, definitionId) } }

    fun refillMatchedSlots(wordId: Long, definitionId: Long) {
        val nextLeft = ArrayList(leftSlots)
        val nextRight = ArrayList(rightSlots)
        val nextLeftQueue = ArrayList(leftQueue)
        val nextRightQueue = ArrayList(rightQueue)
        val leftIndex = nextLeft.indexOf(wordId)
        val rightIndex = nextRight.indexOf(definitionId)
        if (leftIndex < 0 || rightIndex < 0) return
        nextLeft[leftIndex] = emptySlot
        nextRight[rightIndex] = emptySlot

        fun takeLeftAt(index: Int): Long = nextLeftQueue.removeAt(index)
        fun takeRightAt(index: Int): Long = nextRightQueue.removeAt(index)

        // First try to expose the counterpart of a card already visible on the other side.
        if (!boardHasMatch(nextLeft, nextRight)) {
            val leftMatchIndex = nextLeftQueue.indexOfFirst { candidate ->
                nextRight.any { areCompatible(candidate, it) }
            }
            if (leftMatchIndex >= 0) nextLeft[leftIndex] = takeLeftAt(leftMatchIndex)
        }
        if (!boardHasMatch(nextLeft, nextRight)) {
            val rightMatchIndex = nextRightQueue.indexOfFirst { candidate ->
                nextLeft.any { areCompatible(it, candidate) }
            }
            if (rightMatchIndex >= 0) nextRight[rightIndex] = takeRightAt(rightMatchIndex)
        }
        // If neither visible side had a counterpart, introduce one compatible queued pair.
        if (!boardHasMatch(nextLeft, nextRight) &&
            nextLeft[leftIndex] == emptySlot && nextRight[rightIndex] == emptySlot
        ) {
            val pair = nextLeftQueue.asSequence().mapNotNull { leftId ->
                val rightQueueIndex = nextRightQueue.indexOfFirst { areCompatible(leftId, it) }
                if (rightQueueIndex >= 0) leftId to rightQueueIndex else null
            }.firstOrNull()
            if (pair != null) {
                nextLeft.removeAt(leftIndex)
                nextLeft.add(leftIndex, pair.first)
                nextLeftQueue.remove(pair.first)
                nextRight[rightIndex] = takeRightAt(pair.second)
            }
        }
        if (nextLeft[leftIndex] == emptySlot && nextLeftQueue.isNotEmpty()) {
            // When another match already exists, avoid making the two freshly emptied
            // positions a new pair. This prevents solving by tapping the same two spots.
            val avoidSamePositionIndex = if (boardHasMatch(nextLeft, nextRight)) {
                nextLeftQueue.indexOfFirst { candidate ->
                    !areCompatible(candidate, nextRight[rightIndex])
                }
            } else {
                -1
            }
            nextLeft[leftIndex] = takeLeftAt(
                if (avoidSamePositionIndex >= 0) avoidSamePositionIndex else 0
            )
        }
        if (nextRight[rightIndex] == emptySlot && nextRightQueue.isNotEmpty()) {
            val crossMatchIndex = nextRightQueue.indexOfFirst { candidate ->
                !areCompatible(nextLeft[leftIndex], candidate) &&
                    nextLeft.withIndex().any { (index, wordId) ->
                        index != leftIndex && areCompatible(wordId, candidate)
                    }
            }
            val avoidSamePositionIndex = nextRightQueue.indexOfFirst { candidate ->
                !areCompatible(nextLeft[leftIndex], candidate)
            }
            val anyMatchIndex = nextRightQueue.indexOfFirst { candidate ->
                nextLeft.any { areCompatible(it, candidate) }
            }
            val chosenIndex = when {
                crossMatchIndex >= 0 -> crossMatchIndex
                boardHasMatch(nextLeft, nextRight) && avoidSamePositionIndex >= 0 ->
                    avoidSamePositionIndex
                anyMatchIndex >= 0 -> anyMatchIndex
                else -> 0
            }
            nextRight[rightIndex] = takeRightAt(chosenIndex)
        }

        leftSlots = nextLeft
        rightSlots = nextRight
        leftQueue = nextLeftQueue
        rightQueue = nextRightQueue
    }

    fun resetGame() {
        val order = eligibleCards.shuffled().sortedByDescending {
            it.id in wrongIds || it.mistakeCount > 0
        }.map { it.id }
        val newLeft = order.take(4)
        val newRight = buildInitialRightIds(order, newLeft)
        leftSlots = paddedSlots(newLeft)
        rightSlots = paddedSlots(newRight)
        leftQueue = ArrayList(order.filterNot { it in newLeft })
        rightQueue = ArrayList(order.filterNot { it in newRight })
        matchedCount = 0
        wrongAttempts = 0
        startedAt = System.currentTimeMillis()
        selectedWordId = null
        selectedDefinitionId = null
        wrongWordId = null
        wrongDefinitionId = null
    }

    fun evaluatePair(wordId: Long?, definitionId: Long?) {
        if (wordId == null || definitionId == null || wrongWordId != null) return
        if (areCompatible(wordId, definitionId)) {
            refillMatchedSlots(wordId, definitionId)
            matchedCount++
            selectedWordId = null
            selectedDefinitionId = null
        } else {
            cardById[wordId]?.let(onAddWrongCard)
            wrongAttempts++
            wrongWordId = wordId
            wrongDefinitionId = definitionId
            wrongFeedbackVersion++
        }
    }

    LaunchedEffect(wrongFeedbackVersion) {
        if (wrongFeedbackVersion > 0) {
            shakeOffset.snapTo(0f)
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 420
                    -shakeDistance at 55
                    shakeDistance at 110
                    -shakeDistance * 0.75f at 165
                    shakeDistance * 0.75f at 220
                    -shakeDistance * 0.4f at 285
                    shakeDistance * 0.4f at 345
                    0f at 420
                }
            )
            delay(130)
            wrongWordId = null
            wrongDefinitionId = null
            selectedWordId = null
            selectedDefinitionId = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "配對",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Text(
                text = "$matchedCount / $totalCount",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }

        Text(
            text = "左右各選一格；配對成功後會立即補入下一組。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (eligibleCards.isEmpty()) {
            CalmEmptyState(
                icon = Icons.Default.Extension,
                title = "沒有可配對的單字",
                message = "配對題需要同時具有英文單字與中文解釋。"
            )
        } else if (matchedCount >= totalCount) {
            Spacer(modifier = Modifier.height(20.dp))
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "全部配對完成",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "正確率 ${((totalCount.toFloat() / (totalCount + wrongAttempts).coerceAtLeast(1)) * 100).toInt()}%・答錯 $wrongAttempts 次・用時 ${(System.currentTimeMillis() - startedAt) / 1000} 秒",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Button(
                        onClick = ::resetGame,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("再玩一次", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { translationX = shakeOffset.value },
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    leftSlots.forEachIndexed { slotIndex, cardId ->
                        Crossfade(targetState = cardId, label = "matching_word_$slotIndex") { visibleId ->
                            val card = cardById[visibleId]
                            MatchingTile(
                                text = card?.word.orEmpty(),
                                selected = selectedWordId == visibleId,
                                error = wrongWordId == visibleId,
                                enabled = card != null && wrongWordId == null,
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                onClick = {
                                    selectedWordId = visibleId
                                    evaluatePair(visibleId, selectedDefinitionId)
                                }
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rightSlots.forEachIndexed { slotIndex, cardId ->
                        Crossfade(targetState = cardId, label = "matching_definition_$slotIndex") { visibleId ->
                            val card = cardById[visibleId]
                            MatchingTile(
                                text = card?.definition.orEmpty(),
                                selected = selectedDefinitionId == visibleId,
                                error = wrongDefinitionId == visibleId,
                                enabled = card != null && wrongWordId == null,
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                onClick = {
                                    selectedDefinitionId = visibleId
                                    evaluatePair(selectedWordId, visibleId)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchingTile(
    text: String,
    selected: Boolean,
    error: Boolean = false,
    enabled: Boolean = true,
    selectedContainerColor: Color,
    onClick: () -> Unit
) {
    val fontSize = when {
        text.length > 70 -> 11.sp
        text.length > 46 -> 12.sp
        text.length > 30 -> 13.sp
        text.length > 18 -> 15.sp
        else -> 17.sp
    }
    Card(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                error -> MaterialTheme.colorScheme.errorContainer
                selected -> selectedContainerColor
                enabled -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
            },
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = fontSize,
                lineHeight = (fontSize.value + 2).sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    error -> MaterialTheme.colorScheme.onErrorContainer
                    selected -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurface
                },
                textAlign = TextAlign.Center,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
