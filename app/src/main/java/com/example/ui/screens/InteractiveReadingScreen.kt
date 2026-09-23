package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.assistant.AssistantMessage
import com.example.data.entity.Flashcard
import com.example.data.entity.Deck
import com.example.ui.components.AddFolderDialog
import com.example.ui.components.PronunciationPracticeDialog
import org.json.JSONObject

private data class ReadingTranslation(val sentence: String, val translation: String)
private data class ReadingOption(val text: String, val correct: Boolean)
private data class ReadingQuestion(
    val prompt: String,
    val explanation: String,
    val cardId: Long?,
    val options: List<ReadingOption>
)

private data class ReadingContent(
    val article: String,
    val fullTranslation: String,
    val targetCards: List<Flashcard>,
    val translations: List<ReadingTranslation>,
    val questions: List<ReadingQuestion>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractiveReadingScreen(
    message: AssistantMessage,
    cards: List<Flashcard>,
    decks: List<Deck>,
    onSpeak: (String) -> Unit,
    onStopSpeaking: () -> Unit,
    onPronunciationResult: (Long, Int) -> Unit,
    onReadingMistake: (Long) -> Unit,
    onCopyCardToDeck: (Flashcard, Long, (Boolean) -> Unit) -> Unit,
    onCreateFolderAndCopyCard: (String, String, String, String, Flashcard, (Boolean) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    val content = remember(message.payload, message.content, cards) {
        parseReadingContent(message, cards)
    }
    var showTranslations by remember { mutableStateOf(false) }
    var selectedCard by remember { mutableStateOf<Flashcard?>(null) }
    var pronunciationCard by remember { mutableStateOf<Flashcard?>(null) }
    var quizMode by remember { mutableStateOf(false) }
    var questionIndex by remember { mutableIntStateOf(0) }
    var selectedOption by remember { mutableIntStateOf(-1) }
    var correctCount by remember { mutableIntStateOf(0) }
    var completed by remember { mutableStateOf(false) }
    var missedWords by remember { mutableStateOf<Set<String>>(emptySet()) }
    var cardToAdd by remember { mutableStateOf<Flashcard?>(null) }
    var showCreateFolder by remember { mutableStateOf(false) }
    var copyStatus by remember { mutableStateOf<String?>(null) }

    cardToAdd?.let { card ->
        if (showCreateFolder) {
            AddFolderDialog(
                existingCourses = decks.map { it.category }.distinct(),
                defaultCourse = decks.firstOrNull { it.id == card.deckId }?.category,
                onDismiss = { showCreateFolder = false },
                onConfirm = { course, folder, description, color ->
                    onCreateFolderAndCopyCard(course, folder, description, color, card) { added ->
                        copyStatus = if (added) "已建立資料夾並加入 ${card.word}" else "沒有新增：請確認資料夾名稱"
                    }
                    showCreateFolder = false
                    cardToAdd = null
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = { cardToAdd = null },
                title = { Text("加入資料夾") },
                text = {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val targets = decks.filter { it.id != card.deckId }
                        if (targets.isEmpty()) item {
                            Text("目前沒有其他資料夾，可以建立新的資料夾。")
                        }
                        items(targets) { deck ->
                            Surface(
                                onClick = {
                                    onCopyCardToDeck(card, deck.id) { added ->
                                        copyStatus = if (added) "已將 ${card.word} 加入「${deck.name}」" else "「${deck.name}」已有 ${card.word}"
                                    }
                                    cardToAdd = null
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(deck.name, fontWeight = FontWeight.Bold)
                                    Text(deck.category, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showCreateFolder = true }) { Text("新增資料夾") }
                },
                dismissButton = { TextButton(onClick = { cardToAdd = null }) { Text("取消") } }
            )
        }
    }

    selectedCard?.let { card ->
        var showBack by remember(card.id) { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { onStopSpeaking(); selectedCard = null },
            title = { Text(card.word, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (card.phonetic.isNotBlank()) Text(card.phonetic, color = MaterialTheme.colorScheme.primary)
                    if (showBack) {
                        if (card.partOfSpeech.isNotBlank()) Text(card.partOfSpeech, fontWeight = FontWeight.Bold)
                        Text(card.definition)
                        if (card.exampleSentence.isNotBlank()) Text(card.exampleSentence, style = MaterialTheme.typography.bodySmall)
                        if (card.exampleTranslation.isNotBlank()) Text(card.exampleTranslation, style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text("先想想意思，再翻面確認。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onSpeak(card.word) }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, null)
                            Text("發音")
                        }
                        OutlinedButton(onClick = { pronunciationCard = card }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Mic, null)
                            Text("練習")
                        }
                    }
                    OutlinedButton(onClick = { cardToAdd = card }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Folder, null)
                        Text("加入其他資料夾")
                    }
                    copyStatus?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            confirmButton = { Button(onClick = { showBack = !showBack }) { Text(if (showBack) "看正面" else "翻面") } },
            dismissButton = { TextButton(onClick = { onStopSpeaking(); selectedCard = null }) { Text("關閉") } }
        )
    }
    pronunciationCard?.let { card ->
                        PronunciationPracticeDialog(
            card = card,
            onSpeak = onSpeak,
            onStopSpeaking = onStopSpeaking,
            onResult = { onPronunciationResult(card.id, it) },
            onDismiss = { pronunciationCard = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (quizMode) "閱讀測驗" else "互動閱讀") },
                navigationIcon = {
                    IconButton(onClick = {
                        onStopSpeaking()
                        if (quizMode) quizMode = false else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    if (!quizMode) IconButton(onClick = { onSpeak(content.article) }) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, "朗讀文章")
                    }
                }
            )
        }
    ) { padding ->
        if (quizMode) {
            ReadingQuiz(
                questions = content.questions,
                questionIndex = questionIndex,
                selectedOption = selectedOption,
                correctCount = correctCount,
                completed = completed,
                missedWords = missedWords,
                onSelect = { selectedOption = it },
                onNext = {
                    val question = content.questions[questionIndex]
                    val correct = question.options.getOrNull(selectedOption)?.correct == true
                    if (correct) correctCount++ else {
                        question.cardId?.let(onReadingMistake)
                        val word = content.targetCards.firstOrNull { it.id == question.cardId }?.word
                        if (word != null) missedWords = missedWords + word
                    }
                    if (questionIndex == content.questions.lastIndex) completed = true
                    else { questionIndex++; selectedOption = -1 }
                },
                onRetry = {
                    questionIndex = 0; selectedOption = -1; correctCount = 0
                    completed = false; missedWords = emptySet()
                },
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text("點選文章中的重點單字可翻卡、聽發音或練習。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                item {
                    val highlighted = highlightedArticle(
                        content.article,
                        content.targetCards,
                        MaterialTheme.colorScheme.primary
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        ClickableText(
                            text = highlighted,
                            style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                            modifier = Modifier.fillMaxWidth().padding(18.dp),
                            onClick = { offset ->
                                val id = highlighted.getStringAnnotations("CARD", offset, offset)
                                    .firstOrNull()?.item?.toLongOrNull()
                                selectedCard = content.targetCards.firstOrNull { it.id == id }
                            }
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showTranslations = !showTranslations }, modifier = Modifier.weight(1f)) {
                            Text(if (showTranslations) "隱藏翻譯" else "逐句翻譯")
                        }
                        Button(
                            onClick = { quizMode = true },
                            enabled = content.questions.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) { Text("文章測驗 ${content.questions.size} 題") }
                    }
                }
                if (showTranslations) {
                    if (content.translations.isNotEmpty()) {
                        items(content.translations) { item ->
                            Column(
                                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(12.dp)).padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(item.sentence, fontWeight = FontWeight.Medium)
                                Text(item.translation, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else item {
                        Text(content.fullTranslation.ifBlank { "這篇舊文章沒有保存翻譯，可請 AI 重新產生互動文章。" })
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun ReadingQuiz(
    questions: List<ReadingQuestion>,
    questionIndex: Int,
    selectedOption: Int,
    correctCount: Int,
    completed: Boolean,
    missedWords: Set<String>,
    onSelect: (Int) -> Unit,
    onNext: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (questions.isEmpty()) {
        Column(modifier.fillMaxSize().padding(24.dp)) { Text("這篇文章沒有足夠資料可產生測驗。") }
        return
    }
    if (completed) {
        Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("測驗完成", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("答對 $correctCount / ${questions.size} 題")
            if (missedWords.isNotEmpty()) Text("需要再複習：${missedWords.joinToString("、")}")
            Button(onClick = onRetry) { Text("重新測驗") }
        }
        return
    }
    val question = questions[questionIndex]
    LazyColumn(modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("第 ${questionIndex + 1} / ${questions.size} 題", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
        item { Text(question.prompt, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        items(question.options.indices.toList()) { index ->
            val selected = selectedOption == index
            Surface(
                onClick = { onSelect(index) },
                shape = RoundedCornerShape(14.dp),
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) { Text(question.options[index].text, modifier = Modifier.padding(16.dp), fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) }
        }
        item {
            Button(onClick = onNext, enabled = selectedOption >= 0, modifier = Modifier.fillMaxWidth()) {
                Text(if (questionIndex == questions.lastIndex) "完成" else "下一題")
            }
        }
    }
}

private fun parseReadingContent(message: AssistantMessage, cards: List<Flashcard>): ReadingContent {
    val root = runCatching { JSONObject(message.payload) }.getOrElse { JSONObject() }
    val article = root.optString("article").ifBlank { message.content }
    val ids = root.optJSONArray("targetCardIds")?.let { array ->
        (0 until array.length()).map { array.optLong(it) }.toSet()
    }.orEmpty()
    val targetCards = if (ids.isNotEmpty()) cards.filter { it.id in ids } else cards.filter { card ->
        Regex("\\b${Regex.escape(card.word)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(article)
    }
    val translations = root.optJSONArray("sentenceTranslations")?.let { array ->
        (0 until array.length()).mapNotNull { index ->
            array.optJSONObject(index)?.let {
                ReadingTranslation(it.optString("sentence"), it.optString("translation"))
            }?.takeIf { it.sentence.isNotBlank() || it.translation.isNotBlank() }
        }
    }.orEmpty()
    val aiQuestions = root.optJSONArray("questions")?.let { array ->
        (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val options = item.optJSONArray("options")?.let { optionArray ->
                (0 until optionArray.length()).mapNotNull { optionIndex ->
                    optionArray.optJSONObject(optionIndex)?.let { ReadingOption(it.optString("text"), it.optBoolean("correct")) }
                }
            }.orEmpty()
            ReadingQuestion(item.optString("prompt"), item.optString("explanation"), item.optLong("cardId").takeIf { it > 0 }, options)
                .takeIf { it.prompt.isNotBlank() && options.size >= 2 && options.count(ReadingOption::correct) == 1 }
        }
    }.orEmpty()
    return ReadingContent(article, root.optString("articleTranslation"), targetCards, translations, aiQuestions.ifEmpty { localReadingQuestions(targetCards) })
}

private fun localReadingQuestions(cards: List<Flashcard>): List<ReadingQuestion> = cards.take(5).mapNotNull { card ->
    val sentence = card.exampleSentence.takeIf { it.isNotBlank() } ?: return@mapNotNull null
    val distractors = cards.asSequence().filter { it.id != card.id }.map { it.word }.distinct().take(3).toList()
    if (distractors.isEmpty()) return@mapNotNull null
    val options = (distractors + card.word).distinct().sorted().map { ReadingOption(it, it.equals(card.word, true)) }
    ReadingQuestion(
        prompt = sentence.replace(Regex("\\b${Regex.escape(card.word)}\\b", RegexOption.IGNORE_CASE), "____"),
        explanation = card.definition,
        cardId = card.id,
        options = options
    )
}

private fun highlightedArticle(article: String, cards: List<Flashcard>, highlight: Color): AnnotatedString = buildAnnotatedString {
    append(article)
    cards.sortedByDescending { it.word.length }.forEach { card ->
        Regex("\\b${Regex.escape(card.word)}\\b", RegexOption.IGNORE_CASE).findAll(article).forEach { match ->
            addStringAnnotation("CARD", card.id.toString(), match.range.first, match.range.last + 1)
            addStyle(SpanStyle(color = highlight, fontWeight = FontWeight.Bold, background = highlight.copy(alpha = 0.12f)), match.range.first, match.range.last + 1)
        }
    }
}
