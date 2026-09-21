package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Flashcard
import com.example.data.learning.LearningHistoryEntry
import com.example.data.learning.LearningRound
import com.example.data.learning.LearningSessionStore
import com.example.ui.components.FlipCard
import kotlinx.coroutines.delay

private enum class LearningStartChoice {
    CONTINUE,
    ORDERED,
    RANDOM
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardReviewScreen(
    cards: List<Flashcard>,
    sessionScopeId: Long = 0L,
    advancedSrs: Boolean = false,
    autoSpeak: Boolean = false,
    onRecordReview: (Flashcard, Int) -> Unit,
    onUndoReview: (Flashcard) -> Unit = {},
    onToggleFavorite: (Flashcard) -> Unit,
    onSpeak: (String) -> Unit,
    onStopSpeaking: () -> Unit = {},
    onBackToDashboard: () -> Unit,
    onOpenQuizGames: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isFlipped by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val sessionStore = remember(context) { LearningSessionStore(context) }
    var remainingCardIds by remember(sessionScopeId) { mutableStateOf<List<Long>>(emptyList()) }
    var unfamiliarCardIds by remember(sessionScopeId) { mutableStateOf<Set<Long>>(emptySet()) }
    var sessionLoaded by remember(sessionScopeId) { mutableStateOf(false) }
    var roundTotal by remember(sessionScopeId) { mutableIntStateOf(0) }
    var roundFinished by remember(sessionScopeId) { mutableStateOf(false) }
    var allRemembered by remember(sessionScopeId) { mutableStateOf(false) }
    var nextRoundCardCount by remember(sessionScopeId) { mutableIntStateOf(0) }
    var learningHistory by remember(sessionScopeId) {
        mutableStateOf<List<LearningHistoryEntry>>(emptyList())
    }
    var autoPlaying by remember(sessionScopeId) { mutableStateOf(false) }
    val availableCardIds = remember(cards) { cards.map { it.id } }
    val savedRoundAtEntry = remember(sessionScopeId, availableCardIds) {
        sessionStore.loadSaved(sessionScopeId, availableCardIds)
    }
    var startChoice by remember(sessionScopeId, availableCardIds) {
        mutableStateOf<LearningStartChoice?>(null)
    }
    var showResumeDialog by remember(sessionScopeId, availableCardIds) {
        mutableStateOf(savedRoundAtEntry != null)
    }
    var showOrderDialog by remember(sessionScopeId, availableCardIds) {
        mutableStateOf(savedRoundAtEntry == null && cards.isNotEmpty())
    }
    var orderDialogExitRequested by remember(sessionScopeId) { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { onStopSpeaking() }
    }

    fun leaveReview(action: () -> Unit) {
        onStopSpeaking()
        action()
    }

    LaunchedEffect(sessionScopeId, availableCardIds, startChoice) {
        if (cards.isNotEmpty()) {
            val selectedStartChoice = startChoice ?: return@LaunchedEffect
            val round = when (selectedStartChoice) {
                LearningStartChoice.CONTINUE ->
                    sessionStore.loadSaved(sessionScopeId, availableCardIds)
                        ?: sessionStore.startNew(sessionScopeId, availableCardIds, false)
                LearningStartChoice.ORDERED ->
                    sessionStore.startNew(sessionScopeId, availableCardIds, false)
                LearningStartChoice.RANDOM ->
                    sessionStore.startNew(sessionScopeId, availableCardIds, true)
            }
            remainingCardIds = round.remainingCardIds
            unfamiliarCardIds = round.unfamiliarCardIds
            roundTotal = round.totalCardCount
            learningHistory = round.history
            sessionLoaded = true
        } else if (cards.isEmpty()) {
            sessionLoaded = true
        }
    }

    if (cards.isNotEmpty() && showResumeDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!orderDialogExitRequested) {
                    orderDialogExitRequested = true
                    leaveReview(onBackToDashboard)
                }
            },
            title = { Text("繼續上次學習？") },
            text = {
                val learned = savedRoundAtEntry?.let {
                    (it.totalCardCount - it.remainingCardIds.size).coerceAtLeast(0)
                } ?: 0
                Text("上次已學習 $learned 張，可以接著原本的卡片順序繼續。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResumeDialog = false
                        startChoice = LearningStartChoice.CONTINUE
                    }
                ) {
                    Text("接續上次")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showResumeDialog = false
                        showOrderDialog = true
                    }
                ) {
                    Text("重新學習")
                }
            }
        )
    }

    if (cards.isNotEmpty() && showOrderDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!orderDialogExitRequested) {
                    orderDialogExitRequested = true
                    leaveReview(onBackToDashboard)
                }
            },
            title = { Text("選擇卡片順序") },
            text = { Text("選擇這一輪要依資料夾順序學習，或隨機排列卡片。") },
            confirmButton = {
                TextButton(onClick = {
                    showOrderDialog = false
                    startChoice = LearningStartChoice.RANDOM
                }) {
                    Text("隨機排列")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showOrderDialog = false
                    startChoice = LearningStartChoice.ORDERED
                }) {
                    Text("依照順序")
                }
            }
        )
    }

    val currentCard = remainingCardIds.firstOrNull()?.let { id -> cards.firstOrNull { it.id == id } }
    val isEmptySession = sessionLoaded && cards.isEmpty()
    val isSessionComplete = isEmptySession || roundFinished

    fun answerCurrent(remembered: Boolean) {
        val card = currentCard ?: return
        onRecordReview(card, if (remembered) 3 else 1)
        val advance = sessionStore.answer(
            sessionScopeId,
            LearningRound(remainingCardIds, unfamiliarCardIds, roundTotal, learningHistory),
            card.id,
            remembered
        )
        advance.nextRound?.let {
            remainingCardIds = it.remainingCardIds
            unfamiliarCardIds = it.unfamiliarCardIds
            learningHistory = it.history
        }
        if (advance.roundFinished) {
            roundFinished = true
            allRemembered = advance.allRemembered
            nextRoundCardCount = advance.nextRoundCardCount
        }
        isFlipped = false
    }

    fun undoLastAnswer() {
        val undo = sessionStore.undo(
            sessionScopeId,
            LearningRound(remainingCardIds, unfamiliarCardIds, roundTotal, learningHistory)
        ) ?: return
        val card = cards.firstOrNull { it.id == undo.cardId } ?: return
        onUndoReview(card)
        remainingCardIds = undo.round.remainingCardIds
        unfamiliarCardIds = undo.round.unfamiliarCardIds
        roundTotal = undo.round.totalCardCount
        learningHistory = undo.round.history
        roundFinished = false
        allRemembered = false
        nextRoundCardCount = 0
        isFlipped = false
    }

    LaunchedEffect(currentCard?.id, isFlipped, autoSpeak, sessionLoaded) {
        if (autoSpeak && sessionLoaded && !roundFinished) {
            currentCard?.let { onSpeak(it.word) }
        }
    }

    LaunchedEffect(autoPlaying, currentCard?.id, isFlipped) {
        if (!autoPlaying || currentCard == null || roundFinished) return@LaunchedEffect
        delay(if (isFlipped) 2_800 else 1_800)
        if (!autoPlaying) return@LaunchedEffect
        if (!isFlipped) {
            isFlipped = true
        } else {
            answerCurrent(true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                expandedHeight = 48.dp,
                title = {
                    Text(
                        text = when {
                            !sessionLoaded -> "準備中"
                            !isSessionComplete -> "學習 · ${remainingCardIds.size} 張"
                            isEmptySession -> "沒有單字"
                            else -> "本輪完成"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { leaveReview(onBackToDashboard) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!sessionLoaded) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
            } else if (!isSessionComplete && currentCard != null) {
                // Progress Bar
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "進度",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(roundTotal - remainingCardIds.size).coerceAtLeast(0)} / $roundTotal · 不熟 ${unfamiliarCardIds.size}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = {
                            if (roundTotal == 0) 0f
                            else 1f - (remainingCardIds.size.toFloat() / roundTotal.toFloat())
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3D Flip Flashcard with Swipe Gestures
                FlipCard(
                    card = currentCard,
                    isFlipped = isFlipped,
                    onFlip = { isFlipped = !isFlipped },
                    onSpeak = onSpeak,
                    onToggleFavorite = { onToggleFavorite(currentCard) },
                    onSwipeLeft = {
                        answerCurrent(false)
                    },
                    onSwipeRight = {
                        answerCurrent(true)
                    },
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = ::undoLastAnswer,
                        enabled = learningHistory.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = "返回上一步")
                    }
                    IconButton(onClick = { autoPlaying = !autoPlaying }) {
                        Icon(
                            if (autoPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (autoPlaying) "暫停自動播放" else "開始自動播放"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            } else {
                // Session Complete View
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isEmptySession) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(72.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "完成",
                                        tint = if (isEmptySession) {
                                            MaterialTheme.colorScheme.onSecondary
                                        } else {
                                            MaterialTheme.colorScheme.onPrimary
                                        },
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = when {
                                    isEmptySession -> "這裡沒有單字"
                                    allRemembered -> "全部記熟了"
                                    else -> "本輪完成"
                                },
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = when {
                                    isEmptySession ->
                                        "先新增或匯入單字，再開始學習。"
                                    allRemembered ->
                                        "可以前往測驗，或重新學習全部卡片。"
                                    else ->
                                        "還有 $nextRoundCardCount 張不熟，下次會接著練習。"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            if (!isEmptySession && allRemembered && onOpenQuizGames != null) {
                                Button(
                                    onClick = { leaveReview(onOpenQuizGames) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("prompt_to_games_button")
                                ) {
                                    Text(
                                        text = "前往測驗",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedButton(
                                    onClick = {
                                        startChoice = null
                                        showResumeDialog = false
                                        showOrderDialog = true
                                        sessionLoaded = false
                                        roundFinished = false
                                        allRemembered = false
                                        nextRoundCardCount = 0
                                        learningHistory = emptyList()
                                    },
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null)
                                    Text("重新學習")
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedButton(
                                    onClick = { leaveReview(onBackToDashboard) },
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("finish_review_button")
                                ) {
                                    Text(
                                        text = "返回學習",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            } else if (!isEmptySession && !allRemembered) {
                                Button(
                                    onClick = {
                                        val nextRound = sessionStore.load(
                                            sessionScopeId,
                                            cards.map { it.id }
                                        )
                                        remainingCardIds = nextRound.remainingCardIds
                                        unfamiliarCardIds = nextRound.unfamiliarCardIds
                                        roundTotal = nextRound.totalCardCount
                                        learningHistory = nextRound.history
                                        roundFinished = false
                                        nextRoundCardCount = 0
                                    },
                                    modifier = Modifier.fillMaxWidth().height(52.dp)
                                ) {
                                    Text("繼續複習（$nextRoundCardCount）")
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = { leaveReview(onBackToDashboard) },
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Text("返回學習")
                                }
                            } else {
                                Button(
                                    onClick = { leaveReview(onBackToDashboard) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("finish_review_button")
                                ) {
                                    Text(
                                        text = "返回學習",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
