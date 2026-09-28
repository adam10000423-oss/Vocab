package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.VocabularyRepository
import com.example.data.dictionary.DictionaryEngine
import com.example.data.dictionary.DictionaryEntry
import com.example.data.dictionary.GoogleTranslateLookupService
import com.example.data.dictionary.FreeDictionaryLookupService
import com.example.data.dictionary.CambridgeDictionaryLookupService
import com.example.data.dictionary.WiktionaryLookupService
import com.example.data.dictionary.MyMemoryTranslateLookupService
import com.example.data.entity.Deck
import com.example.data.entity.Flashcard
import com.example.data.entity.StudyLog
import com.example.data.entity.GrammarDraft
import com.example.data.entity.GrammarNote
import com.example.data.entity.GrammarQuestion
import com.example.data.entity.GrammarQuestionDraft
import com.example.data.entity.GrammarWeakness
import com.example.data.entity.GrammarWritingIssue
import com.example.data.entity.GrammarWritingScanResult
import com.example.data.entity.GrammarWritingRecord
import com.example.data.entity.GrammarPattern
import com.example.data.entity.GrammarPatternDraft
import com.example.data.entity.GrammarExample
import com.example.data.entity.GrammarExampleDraft
import com.example.data.entity.GrammarLibrary
import com.example.data.importer.ExternalCardCandidate
import com.example.data.importer.AiExternalFormattingResult
import com.example.data.learning.LearningSessionStore
import com.example.data.learning.StudyCheckInStore
import com.example.data.settings.AppSettings
import com.example.data.settings.ReminderTime
import com.example.data.settings.SettingsRepository
import com.example.data.pronunciation.PronunciationPracticeStore
import com.example.util.DocumentParser
import com.example.util.AiImagePreprocessor
import androidx.room.withTransaction
import com.example.util.OcrCardCandidate
import com.example.widgets.VocabWidgetProvider
import com.example.util.OcrWordParser
import com.example.util.TtsManager
import com.example.util.SpeechSegment
import com.example.notifications.StudyReminderScheduler
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job

import com.example.data.api.GeminiService
import com.example.data.api.AiCredentialsStore
import com.example.data.api.AiApiProfile
import com.example.data.api.AiProvider
import com.example.data.api.DirectAiService
import com.example.data.api.PersonalAiConfig
import com.example.data.backup.BackupService
import com.example.data.assistant.AssistantChatStore
import com.example.data.assistant.AssistantConversation
import com.example.data.assistant.AssistantMessage
import com.example.data.assistant.AssistantPendingAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

sealed interface ImportUiState {
    data object Idle : ImportUiState
    data object Loading : ImportUiState
    data class Success(val itemCount: Int) : ImportUiState
    data class Error(val message: String) : ImportUiState
}

data class AssistantSendOutcome(
    val id: Long = System.nanoTime(),
    val succeeded: Boolean,
    val errorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class VocabularyViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = VocabularyRepository(db, db.deckDao(), db.flashcardDao(), db.studyLogDao())
    private val settingsRepository = SettingsRepository(application)
    private val backupService = BackupService(application, db)
    private val aiCredentialsStore = AiCredentialsStore(application)
    private val learningSessionStore = LearningSessionStore(application)
    private val studyCheckInStore = StudyCheckInStore(application)
    private val assistantChatStore = AssistantChatStore(application)
    private val pronunciationPracticeStore = PronunciationPracticeStore(application)
    private val cardSaveMutex = Mutex()
    val ttsManager = TtsManager(application)
    val ttsVoices = ttsManager.voices

    private val currentTime = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(60_000)
        }
    }

    val allDecks: StateFlow<List<Deck>> = repository.allDecks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allCards: StateFlow<List<Flashcard>> = repository.allCards.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    val allGrammarNotes: StateFlow<List<GrammarNote>> = db.grammarNoteDao().getAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    val grammarLibraries: StateFlow<List<GrammarLibrary>> = db.grammarLibraryDao().getAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    val allGrammarQuestions: StateFlow<List<GrammarQuestion>> = db.grammarQuestionDao().getAllQuestions().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    val allGrammarPatterns: StateFlow<List<GrammarPattern>> = db.grammarContentDao().getAllPatterns().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    val allGrammarExamples: StateFlow<List<GrammarExample>> = db.grammarContentDao().getAllExamples().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    val grammarWeaknesses: StateFlow<List<GrammarWeakness>> = db.grammarQuestionDao().getAllWeaknesses().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    val grammarWritingRecords: StateFlow<List<GrammarWritingRecord>> = db.grammarWritingDao().getAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    val pronunciationWeakCardIds: StateFlow<Set<Long>> = pronunciationPracticeStore.weakCardIds
    val studyCheckInDates = studyCheckInStore.dates

    val dueCards: StateFlow<List<Flashcard>> = currentTime.flatMapLatest {
        repository.getDueCards(it)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val totalCardCount: StateFlow<Int> = repository.totalCardCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val masteredCardCount: StateFlow<Int> = repository.masteredCardCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val dueCardCount: StateFlow<Int> = currentTime.flatMapLatest {
        repository.getDueCardCount(it)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val recentStudyLogs: StateFlow<List<StudyLog>> = repository.getAllLogs().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Selection & UI Filters
    private val _selectedDeckId = MutableStateFlow<Long?>(null)
    val selectedDeckId: StateFlow<Long?> = _selectedDeckId.asStateFlow()

    // OCR candidates state
    private val _ocrCandidates = MutableStateFlow<List<OcrCardCandidate>>(emptyList())
    val ocrCandidates: StateFlow<List<OcrCardCandidate>> = _ocrCandidates.asStateFlow()

    private val _importUiState = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    val importUiState: StateFlow<ImportUiState> = _importUiState.asStateFlow()

    private val _dataMessage = MutableStateFlow<String?>(null)
    val dataMessage: StateFlow<String?> = _dataMessage.asStateFlow()

    private val _settingsLoaded = MutableStateFlow(false)
    val settingsLoaded: StateFlow<Boolean> = _settingsLoaded.asStateFlow()

    private val _apiKeyConfigured = MutableStateFlow(aiCredentialsStore.hasApiKey())
    val apiKeyConfigured: StateFlow<Boolean> = _apiKeyConfigured.asStateFlow()

    private val _aiApiProfiles = MutableStateFlow<List<AiApiProfile>>(emptyList())
    val aiApiProfiles: StateFlow<List<AiApiProfile>> = _aiApiProfiles.asStateFlow()

    private val _availableAiModels = MutableStateFlow<List<String>>(emptyList())
    val availableAiModels: StateFlow<List<String>> = _availableAiModels.asStateFlow()

    private val _aiConnectionStatus = MutableStateFlow<String?>(null)
    val aiConnectionStatus: StateFlow<String?> = _aiConnectionStatus.asStateFlow()

    private val _assistantConversations = MutableStateFlow(assistantChatStore.loadConversations())
    val assistantConversations: StateFlow<List<AssistantConversation>> = _assistantConversations.asStateFlow()
    private val assistantAllMessages = assistantChatStore.loadMessages().toMutableList()
    private val _assistantMessages = MutableStateFlow<List<AssistantMessage>>(emptyList())
    val assistantMessages: StateFlow<List<AssistantMessage>> = _assistantMessages.asStateFlow()
    private val _assistantCurrentConversationId = MutableStateFlow<String?>(null)
    val assistantCurrentConversationId: StateFlow<String?> = _assistantCurrentConversationId.asStateFlow()
    private val _assistantBusy = MutableStateFlow(false)
    val assistantBusy: StateFlow<Boolean> = _assistantBusy.asStateFlow()
    private val _assistantStatus = MutableStateFlow<String?>(null)
    val assistantStatus: StateFlow<String?> = _assistantStatus.asStateFlow()
    private val _assistantSendOutcome = MutableStateFlow<AssistantSendOutcome?>(null)
    val assistantSendOutcome: StateFlow<AssistantSendOutcome?> = _assistantSendOutcome.asStateFlow()
    private val _assistantPendingAction = MutableStateFlow<AssistantPendingAction?>(null)
    val assistantPendingAction: StateFlow<AssistantPendingAction?> = _assistantPendingAction.asStateFlow()
    private val _assistantUndoAction = MutableStateFlow<AssistantPendingAction?>(null)
    val assistantUndoAction: StateFlow<AssistantPendingAction?> = _assistantUndoAction.asStateFlow()
    private val _assistantSelectedDeckIds = MutableStateFlow<Set<Long>>(emptySet())
    val assistantSelectedDeckIds: StateFlow<Set<Long>> = _assistantSelectedDeckIds.asStateFlow()
    private var assistantContextDeckId: Long? = null
    private var assistantRequestJob: Job? = null

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .onEach {
            _settingsLoaded.value = true
            configurePersonalAi(it)
        }
        .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings()
    )

    init {
        viewModelScope.launch {
            DirectAiService.usage.collect {
                refreshAiProfiles(settings.value)
            }
        }
        viewModelScope.launch {
            val savedSettings = settingsRepository.settings.first()
            if (savedSettings.usePersonalAiApi) {
                personalAiConfigs(savedSettings).forEach { config ->
                    runCatching { DirectAiService.refreshUsage(config) }
                }
                refreshAiProfiles(savedSettings)
            }
            if (!savedSettings.dataInitialized) {
                settingsRepository.markDataInitialized()
            }
        }
    }

    fun selectDeck(deckId: Long?) {
        _selectedDeckId.value = deckId
    }

    fun saveGrammarNote(note: GrammarNote, onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                val normalized = note.copy(
                    course = note.course.trim().ifBlank { "通用" },
                    title = note.title.trim(),
                    updatedAt = System.currentTimeMillis()
                )
                require(normalized.title.isNotBlank()) { "請輸入文法名稱" }
                if (normalized.questionTemplate.isNotBlank()) {
                    require("{{answer}}" in normalized.questionTemplate) { "練習題必須用 {{answer}} 標記挖空位置" }
                    require(normalized.answer.isNotBlank()) { "練習題必須填寫正確答案" }
                    val choices = normalized.options.split(',', '、', '\n')
                        .map { it.trim().lowercase() }
                        .filter(String::isNotBlank)
                    require(choices.isEmpty() || normalized.answer.trim().lowercase() in choices) {
                        "選項必須包含正確答案"
                    }
                }
                if (normalized.id == 0L) db.grammarNoteDao().insert(normalized)
                else {
                    db.grammarNoteDao().update(normalized)
                    normalized.id
                }
            }.onSuccess(onComplete).onFailure {
                _dataMessage.value = "儲存文法失敗：${it.message ?: "未知錯誤"}"
            }
        }
    }

    fun saveGrammarNoteBundle(
        note: GrammarNote,
        questions: List<GrammarQuestionDraft>,
        patterns: List<GrammarPatternDraft> = emptyList(),
        onComplete: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            runCatching {
                db.withTransaction {
                    val normalized = note.copy(
                        course = note.course.trim().ifBlank { "通用" },
                        folder = note.folder.trim().ifBlank { "未分類" },
                        title = note.title.trim(),
                        updatedAt = System.currentTimeMillis()
                    )
                    require(normalized.title.isNotBlank()) { "請輸入文法名稱" }
                    if (db.grammarLibraryDao().getAllOnce().none {
                            it.course.equals(normalized.course, true) && it.name.equals(normalized.folder, true)
                        }) {
                        db.grammarLibraryDao().insert(
                            GrammarLibrary(course = normalized.course, name = normalized.folder)
                        )
                    }
                    questions.forEach(::validateGrammarQuestion)
                    val noteId = if (normalized.id == 0L) db.grammarNoteDao().insert(normalized)
                    else {
                        db.grammarNoteDao().update(normalized)
                        normalized.id
                    }
                    val previousPatterns = db.grammarContentDao().getPatternsForNote(noteId)
                    db.grammarQuestionDao().deleteForNote(noteId)
                    db.grammarContentDao().deleteExamplesForNote(noteId)
                    db.grammarContentDao().deletePatternsForNote(noteId)
                    val effectivePatterns = patterns.ifEmpty { normalized.toLegacyPatternDrafts() }
                    val patternIds = effectivePatterns.mapIndexed { patternIndex, pattern ->
                        val previous = previousPatterns.firstOrNull { it.formula.trim().equals(pattern.formula.trim(), true) }
                        val patternId = db.grammarContentDao().insertPattern(
                            GrammarPattern(
                                grammarNoteId = noteId,
                                title = pattern.title.trim().ifBlank { "句型 ${patternIndex + 1}" },
                                formula = pattern.formula.trim(), meaning = pattern.meaning.trim(),
                                usage = pattern.usage.trim(), notes = pattern.notes.trim(),
                                masteryPercent = previous?.masteryPercent ?: 0,
                                nextReviewAt = previous?.nextReviewAt ?: System.currentTimeMillis(),
                                attemptCount = previous?.attemptCount ?: 0,
                                correctCount = previous?.correctCount ?: 0,
                                sortOrder = patternIndex.toLong()
                            )
                        )
                        pattern.examples.filter { it.sentence.isNotBlank() }.forEachIndexed { exampleIndex, example ->
                            db.grammarContentDao().insertExample(
                                GrammarExample(
                                    grammarNoteId = noteId, grammarPatternId = patternId,
                                    sentence = example.sentence.trim(), translation = example.translation.trim(),
                                    highlightedText = example.highlightedText.trim(), sortOrder = exampleIndex.toLong()
                                )
                            )
                        }
                        patternId
                    }
                    questions.forEachIndexed { index, draft ->
                        db.grammarQuestionDao().insertQuestion(
                            GrammarQuestion(
                                grammarNoteId = noteId,
                                grammarPatternId = patternIds.getOrNull(draft.grammarPatternIndex) ?: 0,
                                type = draft.type,
                                prompt = draft.prompt.trim(),
                                translation = draft.translation.trim(),
                                answer = draft.answer.trim(),
                                acceptedAnswers = draft.acceptedAnswers.trim(),
                                options = draft.options.trim(),
                                explanation = draft.explanation.trim(),
                                difficulty = draft.difficulty.coerceIn(1, 3),
                                sourceType = draft.sourceType,
                                sortOrder = index.toLong()
                            )
                        )
                    }
                    noteId
                }
            }.onSuccess(onComplete).onFailure {
                _dataMessage.value = "儲存文法失敗：${it.message ?: "未知錯誤"}"
            }
        }
    }

    fun saveGrammarDrafts(
        drafts: List<GrammarDraft>,
        course: String,
        duplicateMode: String,
        onComplete: (List<Long>) -> Unit = {}
    ) {
        viewModelScope.launch {
            runCatching {
                db.withTransaction {
                    val savedIds = mutableListOf<Long>()
                    drafts.filter { it.title.isNotBlank() }.forEach { draft ->
                        draft.questions.forEach(::validateGrammarQuestion)
                        val normalizedCourse = course.trim().ifBlank { "通用" }
                        if (db.grammarLibraryDao().getAllOnce().none {
                                it.course.equals(normalizedCourse, true) && it.name.equals("未分類", true)
                            }) {
                            db.grammarLibraryDao().insert(GrammarLibrary(course = normalizedCourse, name = "未分類"))
                        }
                        val existing = db.grammarNoteDao().getAllOnce().firstOrNull {
                            it.course.equals(normalizedCourse, true) && it.title.equals(draft.title.trim(), true)
                        }
                        if (existing != null && duplicateMode == "SKIP") return@forEach
                        val title = if (existing != null && duplicateMode == "COPY") {
                            var index = 2
                            var candidate: String
                            val names = db.grammarNoteDao().getAllOnce().map { it.title.lowercase() }.toSet()
                            do { candidate = "${draft.title.trim()} ($index)"; index++ } while (candidate.lowercase() in names)
                            candidate
                        } else draft.title.trim()
                        val legacy = draft.questions.firstOrNull()
                        val note = GrammarNote(
                            id = if (existing != null && duplicateMode == "OVERWRITE") existing.id else 0,
                            course = normalizedCourse, title = title, category = draft.category,
                            level = draft.level, summary = draft.summary,
                            structure = draft.structure, usage = draft.usage,
                            exampleSentence = draft.exampleSentence, exampleTranslation = draft.exampleTranslation,
                            commonMistakes = draft.commonMistakes, comparison = draft.comparison,
                            tags = draft.tags, sourceType = "IMPORT",
                            questionTemplate = legacy?.prompt.orEmpty(), answer = legacy?.answer.orEmpty(),
                            acceptedAnswers = legacy?.acceptedAnswers.orEmpty(), options = legacy?.options.orEmpty(),
                            explanation = legacy?.explanation.orEmpty(),
                            favorite = existing?.favorite ?: false,
                            masteryPercent = if (duplicateMode == "OVERWRITE") existing?.masteryPercent ?: 0 else 0,
                            nextReviewAt = existing?.nextReviewAt ?: System.currentTimeMillis(),
                            studyStep = existing?.studyStep ?: 0,
                            attemptCount = existing?.attemptCount ?: 0,
                            correctCount = existing?.correctCount ?: 0,
                            createdAt = existing?.createdAt ?: System.currentTimeMillis()
                        )
                        val noteId = if (note.id == 0L) db.grammarNoteDao().insert(note)
                        else { db.grammarNoteDao().update(note.copy(updatedAt = System.currentTimeMillis())); note.id }
                        val previousPatterns = db.grammarContentDao().getPatternsForNote(noteId)
                        db.grammarQuestionDao().deleteForNote(noteId)
                        db.grammarContentDao().deleteExamplesForNote(noteId)
                        db.grammarContentDao().deletePatternsForNote(noteId)
                        val effectivePatterns = draft.patterns.ifEmpty { note.toLegacyPatternDrafts() }
                        val patternIds = effectivePatterns.mapIndexed { patternIndex, pattern ->
                            val previous = previousPatterns.firstOrNull { it.formula.trim().equals(pattern.formula.trim(), true) }
                            val patternId = db.grammarContentDao().insertPattern(GrammarPattern(
                                grammarNoteId = noteId, title = pattern.title.ifBlank { "句型 ${patternIndex + 1}" },
                                formula = pattern.formula, meaning = pattern.meaning, usage = pattern.usage,
                                notes = pattern.notes,
                                masteryPercent = previous?.masteryPercent ?: 0,
                                nextReviewAt = previous?.nextReviewAt ?: System.currentTimeMillis(),
                                attemptCount = previous?.attemptCount ?: 0,
                                correctCount = previous?.correctCount ?: 0,
                                sortOrder = patternIndex.toLong()
                            ))
                            pattern.examples.filter { it.sentence.isNotBlank() }.forEachIndexed { exampleIndex, example ->
                                db.grammarContentDao().insertExample(GrammarExample(
                                    grammarNoteId = noteId, grammarPatternId = patternId,
                                    sentence = example.sentence, translation = example.translation,
                                    highlightedText = example.highlightedText, sortOrder = exampleIndex.toLong()
                                ))
                            }
                            patternId
                        }
                        draft.questions.forEachIndexed { index, q ->
                            db.grammarQuestionDao().insertQuestion(GrammarQuestion(
                                grammarNoteId = noteId,
                                grammarPatternId = patternIds.getOrNull(q.grammarPatternIndex) ?: 0,
                                type = q.type, prompt = q.prompt.trim(),
                                translation = q.translation.trim(), answer = q.answer.trim(),
                                acceptedAnswers = q.acceptedAnswers.trim(), options = q.options.trim(),
                                explanation = q.explanation.trim(), difficulty = q.difficulty.coerceIn(1, 3),
                                sourceType = q.sourceType, sortOrder = index.toLong()
                            ))
                        }
                        savedIds += noteId
                    }
                    savedIds
                }
            }.onSuccess(onComplete).onFailure {
                _dataMessage.value = "匯入文法失敗：${it.message ?: "未知錯誤"}"
            }
        }
    }

    private fun validateGrammarQuestion(question: GrammarQuestionDraft) {
        require(question.prompt.isNotBlank()) { "題目內容不能空白" }
        require("{{answer}}" in question.prompt) { "每題必須用 {{answer}} 標記唯一挖空位置" }
        require(question.prompt.windowed("{{answer}}".length).count { it == "{{answer}}" } == 1) {
            "每題只能有一個答案位置"
        }
        require(question.answer.isNotBlank()) { "每題都必須填寫正確答案" }
        val choices = question.options.split(',', '、', '\n').map { it.trim().lowercase() }.filter(String::isNotBlank)
        require(choices.isEmpty() || question.answer.trim().lowercase() in choices) { "題目選項必須包含正確答案" }
        require(choices.size == choices.distinct().size) { "題目選項不能重複" }
    }

    private fun GrammarNote.toLegacyPatternDrafts(): List<GrammarPatternDraft> {
        if (structure.isBlank() && exampleSentence.isBlank()) return emptyList()
        val formulas = structure.lines().map(String::trim).filter(String::isNotBlank)
            .ifEmpty { listOf("主要句型") }
        return formulas.mapIndexed { index, formula ->
            GrammarPatternDraft(
                title = if (formulas.size == 1) "主要句型" else "句型 ${index + 1}",
                formula = formula,
                meaning = if (index == 0) summary else "",
                usage = if (index == 0) usage else "",
                examples = if (index == 0 && exampleSentence.isNotBlank()) listOf(
                    GrammarExampleDraft(exampleSentence, exampleTranslation)
                ) else emptyList()
            )
        }
    }

    fun deleteGrammarNote(note: GrammarNote, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                db.withTransaction {
                    db.grammarQuestionDao().deleteForNote(note.id)
                    db.grammarQuestionDao().deleteWeaknessesForNote(note.id)
                    db.grammarContentDao().deleteExamplesForNote(note.id)
                    db.grammarContentDao().deletePatternsForNote(note.id)
                    db.grammarNoteDao().delete(note)
                }
            }
                .onSuccess { onComplete() }
                .onFailure { _dataMessage.value = "刪除文法失敗：${it.message ?: "未知錯誤"}" }
        }
    }

    fun toggleGrammarFavorite(note: GrammarNote) {
        viewModelScope.launch {
            db.grammarNoteDao().getById(note.id)?.let { current ->
                db.grammarNoteDao().update(current.copy(favorite = !current.favorite, updatedAt = System.currentTimeMillis()))
            }
        }
    }

    fun updateGrammarStudyStep(note: GrammarNote, step: Int) {
        viewModelScope.launch {
            db.grammarNoteDao().getById(note.id)?.let { current ->
                db.grammarNoteDao().update(current.copy(studyStep = step.coerceAtLeast(0), updatedAt = System.currentTimeMillis()))
            }
        }
    }

    fun recordGrammarAnswer(note: GrammarNote, correct: Boolean, usedHint: Boolean = false) {
        viewModelScope.launch {
            val current = db.grammarNoteDao().getById(note.id) ?: return@launch
            val now = System.currentTimeMillis()
            val gain = when {
                !correct -> -8
                usedHint -> 3
                else -> 8
            }
            val nextMastery = (current.masteryPercent + gain).coerceIn(0, 100)
            val delayDays = when {
                !correct -> 1L
                nextMastery < 40 -> 2L
                nextMastery < 70 -> 4L
                nextMastery < 90 -> 7L
                else -> 14L
            }
            db.grammarNoteDao().update(
                current.copy(
                    masteryPercent = nextMastery,
                    nextReviewAt = now + delayDays * 86_400_000L,
                    attemptCount = current.attemptCount + 1,
                    correctCount = current.correctCount + if (correct) 1 else 0,
                    studyStep = if (correct) 4 else current.studyStep,
                    updatedAt = now
                )
            )
        }
    }

    fun recordGrammarPatternResult(patternId: Long, correct: Boolean, usedHint: Boolean = false) {
        if (patternId <= 0) return
        viewModelScope.launch {
            val pattern = db.grammarContentDao().getPatternById(patternId) ?: return@launch
            val now = System.currentTimeMillis()
            val gain = when { !correct -> -10; usedHint -> 3; else -> 10 }
            val mastery = (pattern.masteryPercent + gain).coerceIn(0, 100)
            val days = when { !correct -> 1L; mastery < 40 -> 2L; mastery < 70 -> 4L; mastery < 90 -> 7L; else -> 14L }
            db.grammarContentDao().updatePattern(pattern.copy(
                masteryPercent = mastery,
                nextReviewAt = now + days * 86_400_000L,
                attemptCount = pattern.attemptCount + 1,
                correctCount = pattern.correctCount + if (correct) 1 else 0
            ))
        }
    }

    suspend fun generateGrammarDraft(request: String): GrammarDraft {
        require(settings.value.aiEnabled) { "請先在設定中啟用 AI" }
        val result = GeminiService.runAssistant(
            """
            你是嚴謹的英文文法教材編輯。請根據使用者要求建立一篇繁體中文文法筆記。
            一篇筆記代表一個可獨立學習與測驗的「文法類型」；同一類型可包含多組句型，每組句型再包含多個例句。
            只回傳一個 JSON 物件，不要 Markdown。欄位：
            title, category, level, summary, commonMistakes, comparison, tags、patterns 與 questions。
            patterns 每組包含 title, formula, meaning, usage, notes, examples；examples 每筆包含 sentence, translation, highlightedText。
            每個獨立句型必須分成一組 pattern，不可把全部公式塞進同一字串。
            structure、usage、commonMistakes、comparison 可用換行分隔；tags、acceptedAnswers、options 用逗號分隔。
            questions 每題格式：grammarPatternIndex, type, prompt, translation, answer, acceptedAnswers, options, explanation, difficulty。
            prompt 必須用 {{answer}} 標記唯一挖空位置，答案填回後必須成為自然完整的英文句子。
            options 必須包含 answer、不得重複，最多四個，且正確答案不可固定放第一個。
            全篇合計必須剛好 6 個例句與 10 題。前 2 題是基本概念選擇題，其餘 8 題以句子應用為主，包含動詞形式（V-ing、to V 等）、挖空、改錯、翻譯或重組；grammarPatternIndex 必須正確指向所屬句型。
            不確定的內容留空，不可捏造來源。

            使用者要求：${request.trim()}
            """.trimIndent()
        )
        return parseGrammarDraft(result)
    }

    suspend fun generateGrammarDraftFromText(text: String, mode: String): GrammarDraft {
        require(text.isNotBlank()) { "請先貼上文法內容" }
        val rule = when (mode) {
            "ENRICH" -> "保留原文內容，並補充自然例句、常見錯誤與 6～12 題練習。"
            "ORGANIZE" -> "不可改變原意，將內容整理成結構清楚的筆記與練習題。"
            else -> "只能擷取文字中確實存在的內容，不得補充或猜測；原文沒有題目就不要建立題目。"
        }
        return generateGrammarDraft("$rule\n\n待匯入內容：\n${text.trim()}")
    }

    suspend fun readGrammarShare(uri: Uri): List<GrammarDraft> {
        val text = getApplication<Application>().contentResolver.openInputStream(uri)
            ?.bufferedReader()?.use { it.readText() } ?: error("無法讀取分享檔")
        val root = JSONObject(text)
        require(root.optString("format") == "vocabshare") { "不是有效的 .vocabshare 檔案" }
        val items = root.optJSONArray("grammar") ?: root.optJSONArray("items") ?: JSONArray()
        return buildList {
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                val questionsJson = item.optJSONArray("questions") ?: JSONArray()
                val patternsJson = item.optJSONArray("patterns") ?: JSONArray()
                val patterns = buildList {
                    for (patternIndex in 0 until patternsJson.length()) {
                        val pattern = patternsJson.optJSONObject(patternIndex) ?: continue
                        val examplesJson = pattern.optJSONArray("examples") ?: JSONArray()
                        val examples = buildList {
                            for (exampleIndex in 0 until examplesJson.length()) {
                                val example = examplesJson.optJSONObject(exampleIndex) ?: continue
                                val sentence = example.optString("sentence").trim()
                                if (sentence.isNotBlank()) add(GrammarExampleDraft(sentence, example.optString("translation"), example.optString("highlightedText")))
                            }
                        }
                        val formula = pattern.optString("formula").trim()
                        if (formula.isNotBlank()) add(GrammarPatternDraft(
                            title = pattern.optString("title", "句型 ${patternIndex + 1}"), formula = formula,
                            meaning = pattern.optString("meaning"), usage = pattern.optString("usage"),
                            notes = pattern.optString("notes"), examples = examples
                        ))
                    }
                }
                val questions = buildList {
                    for (qIndex in 0 until questionsJson.length()) {
                        val q = questionsJson.optJSONObject(qIndex) ?: continue
                        val draft = GrammarQuestionDraft(
                            grammarPatternIndex = q.optInt("grammarPatternIndex", -1),
                            type = q.optString("type", "CLOZE_CHOICE"), prompt = q.optString("prompt"),
                            translation = q.optString("translation"), answer = q.optString("answer"),
                            acceptedAnswers = q.optString("acceptedAnswers"), options = q.optString("options"),
                            explanation = q.optString("explanation"), difficulty = q.optInt("difficulty", 1).coerceIn(1, 3),
                            sourceType = "SHARE"
                        )
                        if (draft.prompt.isNotBlank() && draft.answer.isNotBlank()) add(draft)
                    }
                }
                val draft = GrammarDraft(
                    title = item.optString("title"), category = item.optString("category", "其他"),
                    level = item.optString("level", "未分級"), summary = item.optString("summary"),
                    structure = item.optString("structure"), usage = item.optString("usage"),
                    exampleSentence = item.optString("exampleSentence"), exampleTranslation = item.optString("exampleTranslation"),
                    commonMistakes = item.optString("commonMistakes"), comparison = item.optString("comparison"),
                    tags = item.optString("tags"), patterns = patterns, questions = questions
                )
                if (draft.title.isNotBlank()) add(draft)
            }
        }.also { require(it.isNotEmpty()) { "分享檔沒有可匯入的文法" } }
    }

    suspend fun generateGrammarDraftFromImages(uris: List<Uri>): GrammarDraft = generateGrammarDraftsFromSources(uris, "ENRICH").first()

    suspend fun generateGrammarDraftFromSources(uris: List<Uri>, mode: String = "FAITHFUL"): GrammarDraft =
        generateGrammarDraftsFromSources(uris, mode).first()

    suspend fun generateGrammarDraftsFromSources(uris: List<Uri>, mode: String = "ENRICH"): List<GrammarDraft> {
        require(settings.value.aiEnabled) { "請先在設定中啟用 AI" }
        require(uris.isNotEmpty()) { "請先選擇圖片或檔案" }
        val context = getApplication<Application>()
        val payloads = buildList {
            uris.take(5).forEach { uri ->
                val mime = context.contentResolver.getType(uri).orEmpty()
                if (mime.contains("pdf", true)) addAll(DocumentParser.renderPdfPagesForAi(context, uri, 5))
                else if (mime.startsWith("image/")) add(AiImagePreprocessor.prepare(context, uri))
            }
        }
        val extractedText = uris.take(5).mapNotNull { uri ->
            val mime = context.contentResolver.getType(uri).orEmpty()
            if (mime.startsWith("image/") || mime.contains("pdf", true)) null
            else runCatching { DocumentParser.extractTextFromUri(context, uri, settings.value.pdfPageLimit) }.getOrNull()
        }.filter(String::isNotBlank).joinToString("\n\n")
        require(payloads.isNotEmpty() || extractedText.isNotBlank()) { "檔案沒有可辨識的內容" }
        val result = GeminiService.runAssistant(
            """
            你是嚴謹的英文文法教材圖片辨識與整理助手。先忠實讀取圖片或文件，再補齊成可學習的文法資料。
            資料階層固定為：課程由 App 另行選擇；每篇 note 是一個文法類型；類型內有多組 patterns；每組 pattern 有多個 examples；questions 必須涵蓋這個類型的每一個例句。
            依照圖片由上到下、由左到右整理。不同且可獨立學習的文法觀念必須拆成不同筆記。
            只回傳 JSON 物件 {"notes":[...]}，不要 Markdown。每篇欄位：title, category, level, summary, commonMistakes, comparison, tags, patterns, questions。
            patterns 每組包含 title, formula, meaning, usage, notes, examples；examples 包含 sentence, translation, highlightedText。
            questions 每題包含 grammarPatternIndex, type, prompt, translation, answer, acceptedAnswers, options, explanation, difficulty；題目中以 {{answer}} 標記唯一答案位置。
            教材已有的規則、句型與例句必須優先保留；看不清楚的內容留空，不可猜測原文。
            每篇完成稿必須補齊為剛好 6 個例句與 10 題。前 2 題為基本概念選擇題，後 8 題為應用題，依內容加入 V-ing、to V、時態、介系詞挖空、改錯、翻譯或重組。options 必須包含答案但不可固定把答案放第一個。
            辨識模式：${when (mode) {
                "ENRICH" -> "AI 補充：忠實保留原文，補齊自然例句、常見錯誤與練習題。每個例句至少要有一題，題目需涵蓋整個文法類型；補充題 sourceType 設為 AI。"
                "ORGANIZE" -> "AI 整理：不可改變原意，可重整段落與欄位，並依原文建立題目。"
                else -> "AI 補齊：先忠實保留文件內容，再補齊缺少的句型、例句、補充說明與題目。"
            }}
            """.trimIndent() + if (extractedText.isBlank()) "" else "\n\n文件文字：\n$extractedText",
            payloads
        )
        val notes = result.optJSONArray("notes")
        val drafts = if (notes == null) listOf(parseGrammarDraft(result)) else buildList {
            for (index in 0 until notes.length()) {
                val item = notes.optJSONObject(index) ?: continue
                runCatching { parseGrammarDraft(item) }.getOrNull()?.let(::add)
            }
        }
        require(drafts.isNotEmpty()) { "AI 沒有辨識到可匯入的文法" }
        return drafts
    }

    private fun parseGrammarDraft(result: JSONObject): GrammarDraft {
        fun value(name: String): String {
            val raw = result.opt(name)
            return when (raw) {
                is JSONArray -> (0 until raw.length()).joinToString("\n") { raw.optString(it) }
                else -> result.optString(name).trim()
            }
        }
        val patternItems = result.optJSONArray("patterns") ?: JSONArray()
        val patterns = buildList {
            for (index in 0 until patternItems.length()) {
                val item = patternItems.optJSONObject(index) ?: continue
                val exampleItems = item.optJSONArray("examples") ?: JSONArray()
                val examples = buildList {
                    for (exampleIndex in 0 until exampleItems.length()) {
                        val example = exampleItems.optJSONObject(exampleIndex) ?: continue
                        val sentence = example.optString("sentence").trim()
                        if (sentence.isNotBlank()) add(GrammarExampleDraft(
                            sentence = sentence,
                            translation = example.optString("translation").trim(),
                            highlightedText = example.optString("highlightedText").trim()
                        ))
                    }
                }
                val pattern = GrammarPatternDraft(
                    title = item.optString("title", "句型 ${index + 1}").trim(),
                    formula = item.optString("formula").trim(),
                    meaning = item.optString("meaning").trim(),
                    usage = item.optString("usage").trim(),
                    notes = item.optString("notes").trim(), examples = examples
                )
                if (pattern.formula.isNotBlank() || pattern.meaning.isNotBlank()) add(pattern)
            }
        }
        val questionItems = result.optJSONArray("questions") ?: JSONArray()
        val questions = buildList {
            for (index in 0 until questionItems.length()) {
                val item = questionItems.optJSONObject(index) ?: continue
                fun qValue(name: String): String {
                    val raw = item.opt(name)
                    return if (raw is JSONArray) (0 until raw.length()).joinToString(",") { raw.optString(it) }
                    else item.optString(name).trim()
                }
                val question = GrammarQuestionDraft(
                    grammarPatternIndex = item.optInt("grammarPatternIndex", -1),
                    type = qValue("type").ifBlank { "CLOZE_CHOICE" },
                    prompt = qValue("prompt"), translation = qValue("translation"),
                    answer = qValue("answer"), acceptedAnswers = qValue("acceptedAnswers"),
                    options = qValue("options"), explanation = qValue("explanation"),
                    difficulty = item.optInt("difficulty", 1).coerceIn(1, 3), sourceType = "AI"
                )
                if (question.prompt.isNotBlank() && question.answer.isNotBlank() && "{{answer}}" in question.prompt) add(question)
            }
        }
        val draft = GrammarDraft(
            title = value("title"),
            category = value("category").ifBlank { "其他" },
            level = value("level").ifBlank { "未分級" },
            summary = value("summary"),
            structure = value("structure"),
            usage = value("usage"),
            exampleSentence = value("exampleSentence"),
            exampleTranslation = value("exampleTranslation"),
            commonMistakes = value("commonMistakes"),
            comparison = value("comparison"),
            tags = value("tags").replace("\n", ","),
            questionTemplate = value("questionTemplate"),
            answer = value("answer"),
            acceptedAnswers = value("acceptedAnswers").replace("\n", ","),
            options = value("options").replace("\n", ","),
            explanation = value("explanation"), patterns = patterns,
            questions = questions
        )
        require(draft.title.isNotBlank()) { "AI 沒有回傳文法名稱" }
        if (draft.questionTemplate.isNotBlank()) {
            require("{{answer}}" in draft.questionTemplate) { "AI 題目缺少答案位置" }
            require(draft.answer.isNotBlank()) { "AI 題目缺少答案" }
        }
        return draft
    }

    suspend fun recognizeGrammarWritingSources(uris: List<Uri>): GrammarWritingScanResult {
        require(settings.value.aiEnabled) { "請先在設定中啟用 AI" }
        require(uris.isNotEmpty()) { "請先選擇圖片或文件" }
        val context = getApplication<Application>()
        val payloads = buildList {
            uris.take(10).forEach { uri ->
                val mime = context.contentResolver.getType(uri).orEmpty()
                if (mime.contains("pdf", true)) addAll(DocumentParser.renderPdfPagesForAi(context, uri, settings.value.pdfPageLimit))
                else if (mime.startsWith("image/")) add(AiImagePreprocessor.prepare(context, uri))
            }
        }
        val documentText = uris.take(10).mapNotNull { uri ->
            val mime = context.contentResolver.getType(uri).orEmpty()
            if (mime.startsWith("image/") || mime.contains("pdf", true)) null
            else runCatching { DocumentParser.extractTextFromUri(context, uri, settings.value.pdfPageLimit) }.getOrNull()
        }.filter(String::isNotBlank).joinToString("\n\n")
        require(payloads.isNotEmpty() || documentText.isNotBlank()) { "檔案沒有可辨識的作文內容" }
        val result = GeminiService.runAssistant(
            """
            你是嚴謹的英文作文辨識助手。依圖片或文件原始順序逐字擷取英文作文，保留段落、大小寫、拼字、標點與原本錯誤，不可先修正文法，不可猜測看不清楚的字。
            只回傳 JSON：{"recognizedText":"完整原文","uncertainParts":["無法確定的原始片段"]}。
            看不清楚處在 recognizedText 中以 [看不清楚] 標示。多張圖片依收到的順序合併。
            """.trimIndent() + if (documentText.isBlank()) "" else "\n\n文件內容：\n$documentText",
            payloads
        )
        return GrammarWritingScanResult(
            recognizedText = result.optString("recognizedText").trim(),
            uncertainParts = result.optJSONArray("uncertainParts")?.let { array ->
                (0 until array.length()).mapNotNull { array.optString(it).trim().takeIf(String::isNotBlank) }
            }.orEmpty()
        ).also { require(it.recognizedText.isNotBlank()) { "AI 沒有辨識到英文作文" } }
    }

    suspend fun checkGrammarWriting(text: String): List<GrammarWritingIssue> {
        require(text.isNotBlank()) { "請先輸入英文內容" }
        require(settings.value.aiEnabled) { "請先在設定中啟用 AI" }
        val result = GeminiService.runAssistant(
            """
            你是嚴謹的英文文法檢查助手。分析下方英文，只回報確定存在的文法錯誤；風格偏好或可接受變體不要當成錯誤。
            只回傳 JSON 物件：{"issues":[{"ruleKey":"穩定的英文規則代碼","title":"繁體中文文法名稱","originalSentence":"原句","correctedSentence":"完整修正句","originalText":"錯誤片段","correctedText":"替換片段","explanation":"繁體中文原因"}]}。
            originalText 必須逐字存在於 originalSentence；correctedText 必須逐字存在於 correctedSentence。沒有可靠錯誤就回傳空陣列。

            待檢查內容：
            $text
            """.trimIndent()
        )
        val items = result.optJSONArray("issues") ?: JSONArray()
        return buildList {
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                val issue = GrammarWritingIssue(
                    ruleKey = item.optString("ruleKey").trim(), title = item.optString("title").trim(),
                    originalSentence = item.optString("originalSentence").trim(),
                    correctedSentence = item.optString("correctedSentence").trim(),
                    originalText = item.optString("originalText").trim(), correctedText = item.optString("correctedText").trim(),
                    explanation = item.optString("explanation").trim()
                )
                if (issue.ruleKey.isNotBlank() && issue.title.isNotBlank() &&
                    issue.originalText in issue.originalSentence && issue.correctedText in issue.correctedSentence
                ) add(issue)
            }
        }.distinctBy { "${it.ruleKey}|${it.originalSentence}" }
    }

    fun acceptGrammarWritingIssue(issue: GrammarWritingIssue, onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                db.withTransaction {
                    val existingNote = db.grammarNoteDao().getAllOnce().firstOrNull {
                        it.title.equals(issue.title, ignoreCase = true)
                    }
                    val noteId = existingNote?.id ?: db.grammarNoteDao().insert(
                        GrammarNote(
                            course = "寫作弱點", title = issue.title,
                            summary = issue.explanation, commonMistakes = "${issue.originalText} → ${issue.correctedText}",
                            sourceType = "WRITING_CHECK"
                        )
                    )
                    val oldWeakness = db.grammarQuestionDao().getWeakness(issue.ruleKey)
                    if (oldWeakness == null) db.grammarQuestionDao().insertWeakness(
                        GrammarWeakness(
                            ruleKey = issue.ruleKey, grammarNoteId = noteId, title = issue.title,
                            originalText = issue.originalText, correctedText = issue.correctedText,
                            explanation = issue.explanation
                        )
                    ) else db.grammarQuestionDao().updateWeakness(
                        oldWeakness.copy(
                            grammarNoteId = noteId, originalText = issue.originalText,
                            correctedText = issue.correctedText, explanation = issue.explanation,
                            occurrenceCount = oldWeakness.occurrenceCount + 1,
                            lastOccurredAt = System.currentTimeMillis()
                        )
                    )
                    val prompt = issue.correctedSentence.replaceFirst(issue.correctedText, "{{answer}}")
                    if ("{{answer}}" in prompt && db.grammarQuestionDao().getForNote(noteId).none { it.prompt == prompt }) {
                        db.grammarQuestionDao().insertQuestion(
                            GrammarQuestion(
                                grammarNoteId = noteId, type = "CLOZE_INPUT", prompt = prompt,
                                answer = issue.correctedText, explanation = issue.explanation,
                                sourceType = "WRITING_CHECK"
                            )
                        )
                    }
                    noteId
                }
            }.onSuccess(onComplete).onFailure {
                _dataMessage.value = "建立弱點練習失敗：${it.message ?: "未知錯誤"}"
            }
        }
    }

    fun saveGrammarWritingRecord(
        title: String,
        course: String,
        originalText: String,
        revisedText: String,
        issues: List<GrammarWritingIssue>,
        onComplete: (Long) -> Unit = {}
    ) {
        if (title.isBlank() || originalText.isBlank()) return
        viewModelScope.launch {
            val payload = JSONArray().apply {
                issues.forEach { issue -> put(JSONObject().apply {
                    put("ruleKey", issue.ruleKey); put("title", issue.title)
                    put("originalSentence", issue.originalSentence); put("correctedSentence", issue.correctedSentence)
                    put("originalText", issue.originalText); put("correctedText", issue.correctedText)
                    put("explanation", issue.explanation)
                }) }
            }.toString()
            val id = db.grammarWritingDao().insert(
                GrammarWritingRecord(
                    course = course.ifBlank { "通用" },
                    title = title.trim(),
                    originalText = originalText,
                    revisedText = revisedText.ifBlank { originalText },
                    issuesJson = payload
                )
            )
            onComplete(id)
        }
    }

    fun deleteGrammarWritingRecord(record: GrammarWritingRecord) {
        viewModelScope.launch { db.grammarWritingDao().delete(record) }
    }

    fun moveCard(card: Flashcard, direction: Int) {
        if (direction !in setOf(-1, 1)) return
        viewModelScope.launch {
            repository.moveCard(card.id, card.deckId, direction)
        }
    }

    fun moveDeck(deck: Deck, direction: Int) {
        if (direction !in setOf(-1, 1)) return
        viewModelScope.launch {
            repository.moveDeck(deck.id, direction)
        }
    }

    fun moveDeckGlobally(deck: Deck, direction: Int) {
        if (direction !in setOf(-1, 1)) return
        viewModelScope.launch { repository.moveDeckGlobally(deck.id, direction) }
    }

    fun reorderDecks(orderedDeckIds: List<Long>) {
        viewModelScope.launch { repository.reorderDecks(orderedDeckIds) }
    }

    fun reorderCards(deckId: Long, orderedCardIds: List<Long>) {
        viewModelScope.launch { repository.reorderCards(deckId, orderedCardIds) }
    }

    fun reorderGrammarNotes(orderedNoteIds: List<Long>) {
        if (orderedNoteIds.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                db.withTransaction {
                    val all = db.grammarNoteDao().getAllOnce()
                    val requestedIds = orderedNoteIds.distinct()
                    require(requestedIds.size == orderedNoteIds.size) { "文法排序包含重複項目" }
                    val requestedSet = requestedIds.toSet()
                    require(requestedSet.all { id -> all.any { it.id == id } }) { "找不到要排序的文法" }
                    val reordered = requestedIds.map { id -> requireNotNull(all.firstOrNull { it.id == id }) }
                    var reorderedIndex = 0
                    val merged = all.map { note ->
                        if (note.id in requestedSet) reordered[reorderedIndex++] else note
                    }
                    db.grammarNoteDao().updateAll(
                        merged.mapIndexed { index, note -> note.copy(sortOrder = index.toLong()) }
                    )
                }
            }.onFailure {
                _dataMessage.value = "調整文法順序失敗：${it.message ?: "未知錯誤"}"
            }
        }
    }

    fun addGrammarLibrary(course: String, name: String, description: String, colorHex: String) {
        viewModelScope.launch {
            val cleanCourse = course.trim().ifBlank { "通用" }
            val cleanName = name.trim()
            if (cleanName.isBlank()) return@launch
            val duplicate = db.grammarLibraryDao().getAllOnce().any {
                it.course.equals(cleanCourse, true) && it.name.equals(cleanName, true)
            }
            if (duplicate) {
                _dataMessage.value = "這個課程已有同名文法庫"
            } else {
                db.grammarLibraryDao().insert(
                    GrammarLibrary(course = cleanCourse, name = cleanName, description = description.trim(), colorHex = colorHex)
                )
            }
        }
    }

    fun updateGrammarLibrary(library: GrammarLibrary) {
        viewModelScope.launch {
            db.withTransaction {
                val previous = db.grammarLibraryDao().getAllOnce().firstOrNull { it.id == library.id }
                if (previous != null && (previous.course != library.course || previous.name != library.name)) {
                    db.grammarNoteDao().renameLibrary(previous.course, previous.name, library.name, System.currentTimeMillis())
                    if (previous.course != library.course) {
                        db.grammarNoteDao().getAllOnce()
                            .filter { it.course == previous.course && it.folder == library.name }
                            .forEach { db.grammarNoteDao().update(it.copy(course = library.course, updatedAt = System.currentTimeMillis())) }
                    }
                }
                db.grammarLibraryDao().update(library)
            }
        }
    }

    fun deleteGrammarLibrary(library: GrammarLibrary) {
        viewModelScope.launch {
            db.withTransaction {
                val notes = db.grammarNoteDao().getAllOnce().filter { it.course == library.course && it.folder == library.name }
                notes.forEach { note ->
                    db.grammarQuestionDao().deleteForNote(note.id)
                    db.grammarContentDao().deleteExamplesForNote(note.id)
                    db.grammarContentDao().deletePatternsForNote(note.id)
                    db.grammarNoteDao().delete(note)
                }
                db.grammarLibraryDao().delete(library)
            }
        }
    }

    fun reorderGrammarLibraries(ids: List<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val all = db.grammarLibraryDao().getAllOnce()
            val requested = ids.mapNotNull { id -> all.firstOrNull { it.id == id } }
            if (requested.size == ids.size) {
                val selected = ids.toSet()
                var index = 0
                val merged = all.map { if (it.id in selected) requested[index++] else it }
                db.grammarLibraryDao().updateAll(merged.mapIndexed { order, item -> item.copy(sortOrder = order.toLong()) })
            }
        }
    }

    fun renameCourse(oldName: String, newName: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = runCatching {
                db.withTransaction {
                    repository.renameCourse(oldName, newName)
                    db.grammarNoteDao().renameCourse(oldName.trim(), newName.trim(), System.currentTimeMillis())
                }
            }.isSuccess
            onComplete(success)
        }
    }

    fun moveCards(
        cards: List<Flashcard>,
        targetDeckId: Long,
        onComplete: (movedCount: Int, mergedCount: Int) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            runCatching {
                repository.moveCards(cards.map { it.id }, targetDeckId)
            }.onSuccess { result ->
                onComplete(result.movedCount, result.mergedDuplicateCount)
            }.onFailure {
                _dataMessage.value = "移動失敗：${it.message ?: "未知錯誤"}"
                onComplete(-1, 0)
            }
        }
    }

    fun createFolderAndMoveCards(
        courseName: String,
        folderName: String,
        description: String,
        colorHex: String,
        cards: List<Flashcard>,
        onComplete: (movedCount: Int, mergedCount: Int) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            runCatching {
                val newId = repository.insertDeck(
                    Deck(
                        category = courseName.trim(),
                        name = folderName.trim(),
                        description = description.trim(),
                        colorHex = colorHex
                    )
                )
                repository.moveCards(cards.map { it.id }, newId)
            }.onSuccess { result ->
                onComplete(result.movedCount, result.mergedDuplicateCount)
            }.onFailure {
                _dataMessage.value = "建立資料夾並移動失敗：${it.message ?: "未知錯誤"}"
                onComplete(-1, 0)
            }
        }
    }

    fun copyCardToDeck(card: Flashcard, targetDeckId: Long, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                val sortOrder = repository.nextSortOrder(targetDeckId, 1)
                repository.insertCardsDeduplicating(
                    listOf(
                        card.copy(
                            id = 0,
                            deckId = targetDeckId,
                            isMastered = false,
                            isFavorite = false,
                            mistakeCount = 0,
                            intervalDays = 0,
                            easeFactor = 2.5f,
                            repetitionCount = 0,
                            nextReviewTimestamp = System.currentTimeMillis(),
                            lastReviewedTimestamp = null,
                            createdAt = System.currentTimeMillis(),
                            sortOrder = sortOrder
                        )
                    )
                )
            }.onSuccess { onComplete(it > 0) }
                .onFailure {
                    _dataMessage.value = "加入資料夾失敗：${it.message ?: "未知錯誤"}"
                    onComplete(false)
                }
        }
    }

    fun createFolderAndCopyCard(
        courseName: String,
        folderName: String,
        description: String,
        colorHex: String,
        card: Flashcard,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            runCatching {
                val safeCourse = courseName.trim()
                val safeName = folderName.trim()
                require(safeCourse.isNotBlank() && safeName.isNotBlank()) { "課程與資料夾名稱不能空白" }
                require(allDecks.value.none {
                    it.category.equals(safeCourse, ignoreCase = true) &&
                        it.name.equals(safeName, ignoreCase = true)
                }) { "同一課程中已經有同名資料夾" }
                val deckId = repository.insertDeck(
                    Deck(
                        category = safeCourse,
                        name = safeName,
                        description = description.trim(),
                        colorHex = colorHex
                    )
                )
                val copied = card.copy(
                    id = 0,
                    deckId = deckId,
                    isMastered = false,
                    isFavorite = false,
                    mistakeCount = 0,
                    intervalDays = 0,
                    easeFactor = 2.5f,
                    repetitionCount = 0,
                    nextReviewTimestamp = System.currentTimeMillis(),
                    lastReviewedTimestamp = null,
                    createdAt = System.currentTimeMillis(),
                    sortOrder = System.currentTimeMillis()
                )
                repository.insertCardsDeduplicating(listOf(copied)) > 0
            }.onSuccess(onComplete)
                .onFailure {
                    _dataMessage.value = "建立資料夾失敗：${it.message ?: "未知錯誤"}"
                    onComplete(false)
                }
        }
    }

    fun toggleStudyCheckIn(date: java.time.LocalDate) {
        if (date <= java.time.LocalDate.now()) {
            studyCheckInStore.toggle(date)
            VocabWidgetProvider.requestUpdate(getApplication())
        }
    }

    /**
     * Auto-fill dictionary entry details for a selected word.
     */
    fun getDictionaryMatch(word: String): DictionaryEntry? {
        return DictionaryEngine.findExact(word)
    }

    /**
     * The AI action always performs a real AI request. Offline dictionary lookup
     * is used only for exact local matches elsewhere and is never presented as AI output.
     */
    suspend fun fetchAiWordDetails(word: String): DictionaryEntry? {
        if (!settings.value.aiEnabled) return null
        val entry = GeminiService.fetchWordDetails(word)
        if (entry == null) {
            GeminiService.lastError.value?.let { error(it) }
        }
        return entry
    }

    suspend fun fetchAiWordDetailsBatch(words: List<String>): Map<String, DictionaryEntry> {
        if (!settings.value.aiEnabled) error("請先在設定中啟用 AI 功能")
        val uniqueWords = words
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinctBy { it.lowercase() }
            .take(80)
        if (uniqueWords.isEmpty()) return emptyMap()

        val candidates = uniqueWords.map { word ->
            OcrCardCandidate(
                word = word,
                partOfSpeech = "",
                definition = "",
                exampleSentence = ""
            )
        }
        val enriched = GeminiService.batchEnrichCandidates(candidates)
        val results = enriched.mapNotNull { item ->
            if (
                item.partOfSpeech.isBlank() &&
                item.definition.isBlank() &&
                item.phonetic.isBlank() &&
                item.exampleSentence.isBlank() &&
                item.exampleTranslation.isBlank()
            ) {
                null
            } else {
                item.word.lowercase() to DictionaryEntry(
                    word = item.word,
                    phonetic = item.phonetic,
                    partOfSpeech = item.partOfSpeech,
                    definition = item.definition,
                    exampleSentence = item.exampleSentence,
                    exampleTranslation = item.exampleTranslation,
                    category = "AI 補充"
                )
            }
        }.toMap().toMutableMap()
        uniqueWords.forEach { word ->
            val current = results[word.lowercase()]
            if (current?.phonetic.isNullOrBlank()) {
                runCatching { GeminiService.fetchWordDetails(word) }
                    .getOrNull()
                    ?.takeIf { it.phonetic.isNotBlank() }
                    ?.let { results[word.lowercase()] = it }
            }
        }
        if (results.isEmpty()) GeminiService.lastError.value?.let { error(it) }
        return results
    }

    suspend fun formatExternalCardsWithAi(
        candidates: List<ExternalCardCandidate>
    ): AiExternalFormattingResult {
        val selected = candidates.filter { it.selected }
        if (selected.isEmpty()) {
            return AiExternalFormattingResult(candidates, 0, 0, "沒有選取需要整理的卡片")
        }
        val pending = selected.filterNot { it.aiFormatted }
        if (pending.isEmpty()) {
            return AiExternalFormattingResult(candidates, selected.size, 0, "已選卡片都已完成 AI 格式整理")
        }
        val currentSettings = settings.value
        if (
            !currentSettings.aiEnabled ||
            !currentSettings.usePersonalAiApi ||
            personalAiConfigs(currentSettings).isEmpty()
        ) {
            return AiExternalFormattingResult(
                cards = candidates,
                formattedCount = 0,
                fallbackCount = pending.size,
                message = "尚未設定可用的個人 AI API"
            )
        }

        val formattedBySelectedIndex = GeminiService.formatImportedCards(
            candidates = pending,
            customInstructions = currentSettings.aiWordPrompt
        )
        var pendingIndex = 0
        val formattedCards = candidates.map { candidate ->
            if (!candidate.selected || candidate.aiFormatted) return@map candidate
            val entry = formattedBySelectedIndex[pendingIndex++]
            if (entry == null) {
                candidate
            } else {
                candidate.copy(
                    word = entry.word,
                    phonetic = entry.phonetic,
                    partOfSpeech = entry.partOfSpeech,
                    definition = entry.definition,
                    exampleSentence = entry.exampleSentence,
                    exampleTranslation = entry.exampleTranslation,
                    aiFormatted = true
                )
            }
        }
        val formattedCount = formattedBySelectedIndex.size
        val fallbackCount = pending.size - formattedCount
        return AiExternalFormattingResult(
            cards = formattedCards,
            formattedCount = formattedCount,
            fallbackCount = fallbackCount,
            message = if (fallbackCount == 0) {
                "AI 已完成 ${pending.size} 張卡片的格式整理"
            } else {
                "AI 完成 $formattedCount 張，$fallbackCount 張保留原始正背面"
            }
        )
    }

    fun updateBooleanSetting(name: String, enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBoolean(name, enabled)
            if (name == "remindersEnabled") {
                if (enabled) StudyReminderScheduler.scheduleAll(getApplication(), settings.value.reminderTimes)
                else StudyReminderScheduler.cancelAll(getApplication(), settings.value.reminderTimes)
            }
        }
    }

    fun updateIntSetting(name: String, value: Int) {
        viewModelScope.launch {
            settingsRepository.setInt(name, value)
            if (name == "dailyGoalCards") {
                VocabWidgetProvider.requestUpdate(getApplication())
            }
            if (name in setOf("reminderHour", "reminderMinute") && settings.value.remindersEnabled) {
                val hour = if (name == "reminderHour") value else settings.value.reminderHour
                val minute = if (name == "reminderMinute") value else settings.value.reminderMinute
                StudyReminderScheduler.schedule(getApplication(), hour, minute)
            }
        }
    }

    fun updateDictionarySource(source: String) {
        viewModelScope.launch { settingsRepository.setDictionarySource(source) }
    }

    fun updateReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsRepository.setReminderTime(hour, minute)
            if (settings.value.remindersEnabled) {
                StudyReminderScheduler.schedule(getApplication(), hour, minute)
            }
        }
    }

    fun updateReminderTimes(times: List<ReminderTime>) {
        viewModelScope.launch {
            val oldTimes = settings.value.reminderTimes
            settingsRepository.setReminderTimes(times)
            if (settings.value.remindersEnabled) {
                StudyReminderScheduler.replaceAll(getApplication(), oldTimes, times)
            } else {
                StudyReminderScheduler.cancelAll(getApplication(), oldTimes)
            }
        }
    }

    fun updateAiWordPrompt(prompt: String) {
        viewModelScope.launch { settingsRepository.setAiWordPrompt(prompt) }
    }

    fun resetAiWordPrompt() {
        viewModelScope.launch {
            settingsRepository.setAiWordPrompt(com.example.data.api.AiPromptDefaults.WORD_DETAILS)
        }
    }

    fun updateAiImagePrompt(prompt: String) {
        viewModelScope.launch { settingsRepository.setAiImagePrompt(prompt) }
    }

    fun resetAiImagePrompt() {
        viewModelScope.launch {
            settingsRepository.setAiImagePrompt(
                com.example.data.api.AiPromptDefaults.IMAGE_VOCABULARY_EXTRACTION
            )
        }
    }

    fun updateThemeMode(mode: String) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun updateThemeColorPreset(preset: String) {
        viewModelScope.launch { settingsRepository.setThemeColorPreset(preset) }
    }

    fun updateCustomThemeColors(primary: String, secondary: String) {
        viewModelScope.launch {
            settingsRepository.setCustomThemeColors(primary, secondary)
        }
    }

    fun updateGradientColors(start: String, end: String) {
        viewModelScope.launch { settingsRepository.setGradientColors(start, end) }
    }

    fun updateCustomTextColor(color: String) {
        viewModelScope.launch { settingsRepository.setCustomTextColor(color) }
    }

    fun updateBackgroundAppearance(brightness: Float, opacity: Float) {
        viewModelScope.launch {
            settingsRepository.setBackgroundAppearance(brightness, opacity)
        }
    }

    fun updateFontAppearance(chineseFamily: String, englishFamily: String, scale: Float) {
        viewModelScope.launch {
            settingsRepository.setFontAppearance(chineseFamily, englishFamily, scale)
        }
    }

    fun updateSpeechRate(rate: Float) {
        viewModelScope.launch { settingsRepository.setSpeechRate(rate) }
    }

    fun updateTtsVoiceStyle(style: String) {
        viewModelScope.launch { settingsRepository.setTtsVoiceStyle(style) }
    }

    fun updateTtsVoiceName(name: String) {
        viewModelScope.launch { settingsRepository.setTtsVoiceName(name) }
    }

    fun previewTtsVoice() {
        val value = settings.value
        ttsManager.speak(
            text = "Hello! This is your English pronunciation voice.",
            rate = value.speechRate,
            voiceName = value.ttsVoiceName,
            voiceStyle = value.ttsVoiceStyle
        )
    }

    fun updateAiProvider(providerName: String) {
        val provider = AiProvider.from(providerName)
        viewModelScope.launch {
            settingsRepository.setAiProvider(provider.name, aiCredentialsStore.savedModel(provider))
            _availableAiModels.value = emptyList()
            _aiConnectionStatus.value = null
        }
    }

    fun updateAiModel(model: String) {
        viewModelScope.launch {
            settingsRepository.setAiModel(model)
            aiCredentialsStore.updateModel(AiProvider.from(settings.value.aiProvider), model)
            _aiConnectionStatus.value = null
            refreshAiProfiles(settings.value.copy(aiModel = model))
        }
    }

    fun updateAiChatStyle(style: String) {
        viewModelScope.launch { settingsRepository.setAiChatStyle(style) }
    }

    fun savePersonalApiKey(apiKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val value = settings.value
                val provider = AiProvider.from(value.aiProvider)
                aiCredentialsStore.saveApiKey(provider, apiKey, value.aiModel)
                settingsRepository.setBoolean("usePersonalAiApi", true)
            }.onSuccess {
                _apiKeyConfigured.value = aiCredentialsStore.hasApiKey()
                configurePersonalAi(settings.value.copy(usePersonalAiApi = true))
                refreshAiProfiles(settings.value)
                _aiConnectionStatus.value = "已加密儲存，可繼續加入其他供應商"
            }.onFailure {
                _aiConnectionStatus.value = "儲存失敗：${it.message ?: "未知錯誤"}"
            }
        }
    }

    fun clearPersonalApiKey(credentialId: String) {
        aiCredentialsStore.clearCredential(credentialId)
        _apiKeyConfigured.value = aiCredentialsStore.hasApiKey()
        _availableAiModels.value = emptyList()
        configurePersonalAi(settings.value)
        refreshAiProfiles(settings.value)
        if (!_apiKeyConfigured.value) {
            viewModelScope.launch { settingsRepository.setBoolean("usePersonalAiApi", false) }
        }
        _aiConnectionStatus.value = "已移除 API Key"
    }

    fun refreshAiModels() {
        val config = selectedAiConfig(settings.value)
        if (config == null) {
            _aiConnectionStatus.value = "請先儲存 API Key"
            return
        }
        viewModelScope.launch {
            _aiConnectionStatus.value = "正在讀取可用模型..."
            runCatching { DirectAiService.listModels(config) }
                .onSuccess {
                    _availableAiModels.value = it
                    _aiConnectionStatus.value = "已取得 ${it.size} 個可用模型"
                }
                .onFailure {
                    _aiConnectionStatus.value = "讀取模型失敗：${it.message ?: "請檢查 Key"}"
                }
        }
    }

    fun testAiConnection(credentialId: String? = null) {
        val config = if (credentialId == null) {
            selectedAiConfig(settings.value)
        } else {
            personalAiConfigs(settings.value).firstOrNull { it.credentialId == credentialId }
        }
        if (config == null) {
            _aiConnectionStatus.value = "找不到這組 API Key，請重新加入"
            return
        }
        viewModelScope.launch {
            _aiConnectionStatus.value = "正在測試 ${config.provider.displayName}..."
            val result = DirectAiService.test(config)
            if (result.isSuccess) {
                _aiConnectionStatus.value = "連線測試成功，${config.model} 可以使用"
                DirectAiService.refreshUsage(config)
                refreshAiProfiles(settings.value)
            } else {
                val error = result.exceptionOrNull()
                _aiConnectionStatus.value =
                    "連線失敗：${error?.message ?: "請檢查 Key、模型與額度"}"
            }
        }
    }

    fun completeOnboarding(dailyGoalCards: Int, remindersEnabled: Boolean, reminderHour: Int) {
        viewModelScope.launch {
            settingsRepository.completeOnboarding(dailyGoalCards, remindersEnabled, reminderHour)
            if (remindersEnabled) {
                StudyReminderScheduler.schedule(getApplication(), reminderHour, 0)
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            runCatching {
                repository.clearAllData()
                db.grammarQuestionDao().deleteAllQuestions()
                db.grammarQuestionDao().deleteAllWeaknesses()
                db.grammarContentDao().deleteAllExamples()
                db.grammarContentDao().deleteAllPatterns()
                db.grammarNoteDao().deleteAll()
                db.grammarWritingDao().deleteAll()
                db.grammarLibraryDao().deleteAll()
                settingsRepository.resetAfterDataClear()
                aiCredentialsStore.clear()
                learningSessionStore.clearAll()
                studyCheckInStore.clear()
                assistantChatStore.clear()
                pronunciationPracticeStore.clear()
                assistantAllMessages.clear()
                _assistantConversations.value = emptyList()
                _assistantMessages.value = emptyList()
                _assistantCurrentConversationId.value = null
                _assistantPendingAction.value = null
                _assistantUndoAction.value = null
                GeminiService.configurePersonalApis(emptyList())
                _apiKeyConfigured.value = false
                _availableAiModels.value = emptyList()
                StudyReminderScheduler.cancelAll(getApplication(), settings.value.reminderTimes)
                _selectedDeckId.value = null
            }.onSuccess {
                _dataMessage.value = "所有資料已清空，現在可從 0 開始"
            }.onFailure {
                _dataMessage.value = "清空失敗：${it.message ?: "未知錯誤"}"
            }
        }
    }

    fun recordPronunciationResult(cardId: Long, score: Int) {
        pronunciationPracticeStore.recordResult(cardId, score.coerceIn(0, 100))
    }

    fun recordReadingMistake(cardId: Long) {
        allCards.value.firstOrNull { it.id == cardId }?.let(::recordGameMistake)
    }

    private fun configurePersonalAi(value: AppSettings) {
        GeminiService.configureWordDetailsPrompt(value.aiWordPrompt)
        GeminiService.configureImagePrompt(value.aiImagePrompt)
        GeminiService.configurePersonalApis(
            if (value.usePersonalAiApi) personalAiConfigs(value) else emptyList()
        )
        refreshAiProfiles(value)
    }

    private fun personalAiConfigs(value: AppSettings): List<PersonalAiConfig> {
        val provider = AiProvider.from(value.aiProvider)
        return aiCredentialsStore.readConfigs(
            preferredProvider = provider,
            preferredModel = value.aiModel.ifBlank { provider.defaultModel }
        )
    }

    private fun selectedAiConfig(value: AppSettings): PersonalAiConfig? =
        personalAiConfigs(value).firstOrNull {
            it.provider == AiProvider.from(value.aiProvider)
        }

    private fun refreshAiProfiles(value: AppSettings) {
        _aiApiProfiles.value = personalAiConfigs(value).map { config ->
            AiApiProfile(
                credentialId = config.credentialId,
                provider = config.provider,
                model = config.model,
                maskedKey = "••••••••••••"
            )
        }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            _dataMessage.value = runCatching {
                val count = backupService.exportJson(uri)
                "已備份 $count 張單字卡"
            }.getOrElse { "備份失敗：${it.message ?: "無法寫入檔案"}" }
        }
    }

    fun exportCsv(uri: Uri) {
        viewModelScope.launch {
            _dataMessage.value = runCatching {
                val count = backupService.exportCsv(uri)
                "已匯出 $count 張單字卡"
            }.getOrElse { "CSV 匯出失敗：${it.message ?: "無法寫入檔案"}" }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            _dataMessage.value = runCatching {
                val count = backupService.importJson(uri)
                studyCheckInStore.reload()
                "備份還原完成，新增 $count 張單字卡"
            }.getOrElse { "還原失敗：${it.message ?: "檔案格式錯誤"}" }
        }
    }

    /**
     * Submit SRS Review answer.
     */
    fun submitCardReview(card: Flashcard, rating: Int) {
        viewModelScope.launch {
            repository.recordReview(card, rating)
            VocabWidgetProvider.requestUpdate(getApplication())
        }
    }

    fun undoCardReview(originalCard: Flashcard) {
        viewModelScope.launch {
            repository.undoLatestReview(originalCard)
            VocabWidgetProvider.requestUpdate(getApplication())
        }
    }

    /**
     * Toggle mastered state directly.
     */
    fun toggleMastered(card: Flashcard) {
        viewModelScope.launch {
            repository.toggleMastered(card)
        }
    }

    fun toggleFavorite(card: Flashcard) {
        viewModelScope.launch { repository.toggleFavorite(card) }
    }

    fun recordGameMistake(card: Flashcard) {
        viewModelScope.launch { repository.recordGameMistake(card) }
    }

    fun addCard(card: Flashcard) {
        viewModelScope.launch {
            repository.insertCard(card)
            VocabWidgetProvider.requestUpdate(getApplication())
        }
    }

    fun updateCard(card: Flashcard) {
        viewModelScope.launch {
            repository.updateCard(card)
            VocabWidgetProvider.requestUpdate(getApplication())
        }
    }

    fun deleteCard(card: Flashcard) {
        viewModelScope.launch {
            repository.deleteCard(card)
            VocabWidgetProvider.requestUpdate(getApplication())
        }
    }

    fun deleteCards(cards: List<Flashcard>) {
        if (cards.isEmpty()) return
        viewModelScope.launch {
            repository.deleteCards(cards)
            VocabWidgetProvider.requestUpdate(getApplication())
        }
    }

    suspend fun saveBatchCards(
        cardsToSave: List<Flashcard>,
        cardsToDelete: List<Flashcard> = emptyList()
    ): Result<List<Long>> = runCatching {
        cardSaveMutex.withLock {
            repository.saveBatch(cardsToSave, cardsToDelete).also {
                VocabWidgetProvider.requestUpdate(getApplication())
            }
        }
    }

    fun addDeck(deck: Deck) {
        viewModelScope.launch {
            repository.insertDeck(deck)
        }
    }

    fun addFolder(courseName: String, folderName: String, description: String, colorHex: String) {
        viewModelScope.launch {
            val newDeck = Deck(
                category = courseName,
                name = folderName,
                description = description,
                colorHex = colorHex
            )
            val newId = repository.insertDeck(newDeck)
            _selectedDeckId.value = newId
        }
    }

    suspend fun lookupDictionary(word: String): Result<DictionaryEntry> = runCatching {
        val chineseQuery = word.any { it.code in 0x3400..0x9FFF }
        when {
            chineseQuery && settings.value.dictionarySource == "GOOGLE" -> GoogleTranslateLookupService.lookup(word)
            chineseQuery -> runCatching { GoogleTranslateLookupService.lookup(word) }
                .getOrElse { googleError ->
                    runCatching { MyMemoryTranslateLookupService.lookupChinese(word) }
                        .getOrElse { backupError -> error("Google：${googleError.message}\n備用翻譯：${backupError.message}") }
                }
            settings.value.dictionarySource == "GOOGLE" -> GoogleTranslateLookupService.lookup(word).let { GoogleTranslateLookupService.translateExample(it) }
            settings.value.dictionarySource == "CAMBRIDGE" -> runCatching { CambridgeDictionaryLookupService.lookup(word) }
                .getOrElse { WiktionaryLookupService.lookup(word) }
            settings.value.dictionarySource == "WIKTIONARY" -> WiktionaryLookupService.lookup(word)
            settings.value.dictionarySource == "FREE" -> FreeDictionaryLookupService.lookup(word)
            else -> runCatching {
                GoogleTranslateLookupService.lookup(word).let { GoogleTranslateLookupService.translateExample(it) }
            }.getOrElse { googleError ->
                runCatching { CambridgeDictionaryLookupService.lookup(word) }.getOrElse { cambridgeError ->
                    runCatching { WiktionaryLookupService.lookup(word) }.getOrElse { wiktionaryError ->
                        runCatching { FreeDictionaryLookupService.lookup(word) }.getOrElse { freeError ->
                            runCatching { MyMemoryTranslateLookupService.lookupEnglish(word) }.getOrElse { translateError ->
                                error("Google：${googleError.message}\n劍橋：${cambridgeError.message}\n維基：${wiktionaryError.message}\n免費字典：${freeError.message}\n備用翻譯：${translateError.message}")
                            }
                        }
                    }
                }
            }
        }
    }

    fun addDictionaryEntryToDeck(entry: DictionaryEntry, deckId: Long, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                val now = System.currentTimeMillis()
                repository.insertCardsDeduplicating(
                    listOf(
                        Flashcard(
                            deckId = deckId,
                            word = entry.word.trim(),
                            phonetic = entry.phonetic.trim(),
                            partOfSpeech = entry.partOfSpeech.trim(),
                            definition = entry.definition.trim(),
                            exampleSentence = entry.exampleSentence.trim(),
                            exampleTranslation = entry.exampleTranslation.trim(),
                            createdAt = now,
                            nextReviewTimestamp = now,
                            sortOrder = repository.nextSortOrder(deckId, 1)
                        )
                    )
                ) > 0
            }.onSuccess {
                _dataMessage.value = if (it) "已加入資料夾" else "這個資料夾已有相同單字"
                onComplete(it)
            }.onFailure {
                _dataMessage.value = "加入失敗：${it.message ?: "未知錯誤"}"
                onComplete(false)
            }
        }
    }

    fun createFolderAndAddDictionaryEntry(
        courseName: String,
        folderName: String,
        description: String,
        colorHex: String,
        entry: DictionaryEntry,
        onComplete: (Boolean) -> Unit = {}
    ) = createFolderAndCopyCard(
        courseName = courseName,
        folderName = folderName,
        description = description,
        colorHex = colorHex,
        card = Flashcard(
            deckId = 0,
            word = entry.word.trim(),
            phonetic = entry.phonetic.trim(),
            partOfSpeech = entry.partOfSpeech.trim(),
            definition = entry.definition.trim(),
            exampleSentence = entry.exampleSentence.trim(),
            exampleTranslation = entry.exampleTranslation.trim()
        ),
        onComplete = onComplete
    )

    fun updateDeck(deck: Deck) {
        viewModelScope.launch {
            repository.updateDeck(deck)
        }
    }

    fun deleteDeck(deck: Deck) {
        viewModelScope.launch {
            repository.deleteDeck(deck)
            if (_selectedDeckId.value == deck.id) {
                _selectedDeckId.value = null
            }
        }
    }

    fun speakText(text: String) {
        val value = settings.value
        ttsManager.speak(
            text = text,
            rate = value.speechRate,
            voiceName = value.ttsVoiceName,
            voiceStyle = value.ttsVoiceStyle
        )
    }

    fun speakLearningCard(card: Flashcard, onComplete: () -> Unit = {}) {
        val value = settings.value
        val segments = buildList {
            if (value.ttsReadWord) add(SpeechSegment(card.word, "en-US"))
            if (value.ttsSpellWord) {
                val spelling = card.word.filter(Char::isLetter).uppercase().toCharArray().joinToString(", ")
                if (spelling.isNotBlank()) add(SpeechSegment(spelling, "en-US"))
            }
            if (value.ttsReadDefinition && card.definition.isNotBlank()) {
                add(SpeechSegment(card.definition, "zh-TW"))
            }
            if (value.ttsReadPhonetic && card.phonetic.isNotBlank()) {
                add(SpeechSegment(card.phonetic, "en-US"))
            }
            if (value.ttsReadPartOfSpeech && card.partOfSpeech.isNotBlank()) {
                add(SpeechSegment(expandPartOfSpeechForSpeech(card.partOfSpeech), "en-US"))
            }
            if (value.ttsReadExample && card.exampleSentence.isNotBlank()) {
                add(SpeechSegment(card.exampleSentence, "en-US"))
            }
            if (value.ttsReadExampleTranslation && card.exampleTranslation.isNotBlank()) {
                add(SpeechSegment(card.exampleTranslation, "zh-TW"))
            }
        }
        ttsManager.speakSequence(
            segments = segments,
            rate = value.speechRate,
            voiceName = value.ttsVoiceName,
            voiceStyle = value.ttsVoiceStyle,
            repetitions = value.ttsGroupRepetitions,
            onComplete = onComplete
        )
    }

    fun speakLearningCardFront(card: Flashcard, onComplete: () -> Unit = {}) {
        val value = settings.value
        val segments = buildList {
            if (value.ttsFrontReadWord) add(SpeechSegment(card.word, "en-US"))
            if (value.ttsFrontSpellWord) {
                val spelling = card.word.filter(Char::isLetter).uppercase().toCharArray().joinToString(", ")
                if (spelling.isNotBlank()) add(SpeechSegment(spelling, "en-US"))
            }
            if (value.ttsFrontReadPartOfSpeech && card.partOfSpeech.isNotBlank()) {
                add(SpeechSegment(expandPartOfSpeechForSpeech(card.partOfSpeech), "en-US"))
            }
        }
        ttsManager.speakSequence(
            segments = segments,
            rate = value.speechRate,
            voiceName = value.ttsVoiceName,
            voiceStyle = value.ttsVoiceStyle,
            repetitions = value.ttsFrontRepetitions,
            onComplete = onComplete
        )
    }

    private fun expandPartOfSpeechForSpeech(value: String): String = value
        .replace(Regex("(?i)\\badj\\.?"), "adjective")
        .replace(Regex("(?i)\\badv\\.?"), "adverb")
        .replace(Regex("(?i)\\bvt\\.?"), "transitive verb")
        .replace(Regex("(?i)\\bvi\\.?"), "intransitive verb")
        .replace(Regex("(?i)\\bv\\.?"), "verb")
        .replace(Regex("(?i)\\bn\\.?"), "noun")
        .replace("/", ", ")

    fun stopSpeaking() {
        ttsManager.stop()
    }

    /** Parse pasted or recognized text immediately; AI enrichment is explicit. */
    fun parseOcrPhotoText(text: String, deckId: Long) {
        val initialCandidates = OcrWordParser.parseTextToCards(text, deckId)
        _ocrCandidates.value = initialCandidates
        _importUiState.value = ImportUiState.Success(initialCandidates.size)
    }

    /**
     * Extracts and parses a PDF, document, or image locally.
     * AI enrichment is a separate, explicit action on the preview screen.
     */
    fun parseDocumentUri(context: Context, uri: Uri, deckId: Long, onComplete: (List<OcrCardCandidate>) -> Unit = {}) {
        viewModelScope.launch {
            _importUiState.value = ImportUiState.Loading
            try {
                val (_, candidates) = DocumentParser.parseDocumentToCards(context, uri)
                _ocrCandidates.value = candidates.map { it.copy() }
                _importUiState.value = ImportUiState.Success(candidates.size)
                onComplete(candidates)
            } catch (error: Exception) {
                _importUiState.value = ImportUiState.Error(error.localizedMessage ?: "檔案讀取失敗")
                onComplete(emptyList())
            }
        }
    }

    /**
     * Triggers Gemini AI to batch complete missing phonetics, part of speech, definitions, and example sentences.
     */
    fun batchAiEnrichOcrCandidates() {
        val currentList = _ocrCandidates.value
        if (currentList.isEmpty()) return

        viewModelScope.launch {
            _importUiState.value = ImportUiState.Loading
            try {
                val enriched = GeminiService.batchEnrichCandidates(currentList)
                _ocrCandidates.value = enriched
                _importUiState.value = ImportUiState.Success(enriched.size)
            } catch (error: Exception) {
                _importUiState.value = ImportUiState.Error(error.localizedMessage ?: "AI 補齊暫時無法使用")
            }
        }
    }

    fun importOcrCandidates(candidates: List<OcrCardCandidate>, targetDeckId: Long) {
        val selectedCandidates = candidates
            .filter { it.isSelected && it.word.isNotBlank() }
            .distinctBy { it.word.trim().lowercase() }
        if (selectedCandidates.isEmpty()) return

        viewModelScope.launch {
            // Only exact dictionary data is applied automatically. AI changes are
            // made in the preview screen before the user confirms the import.
            val baseEnriched = OcrWordParser.enrichCandidatesWithDictionary(selectedCandidates)
            val bottomSortOrder = repository.nextBottomSortOrder(targetDeckId)

            val cards = baseEnriched.mapIndexed { index, candidate ->
                val dict = DictionaryEngine.findExact(candidate.word)
                val finalDef = candidate.definition.takeIf { it.isNotBlank() }
                    ?.let(OcrWordParser::cleanDefinition)
                    ?: dict?.definition.orEmpty()
                val finalSentence = candidate.exampleSentence.ifBlank {
                    dict?.exampleSentence.orEmpty()
                }
                val finalPhonetic = candidate.phonetic.ifBlank { dict?.phonetic.orEmpty() }
                val finalPos = candidate.partOfSpeech.ifBlank { dict?.partOfSpeech.orEmpty() }

                Flashcard(
                    deckId = targetDeckId,
                    word = candidate.word.trim(),
                    phonetic = finalPhonetic,
                    partOfSpeech = finalPos,
                    definition = finalDef,
                    exampleSentence = finalSentence,
                    exampleTranslation = candidate.exampleTranslation.ifBlank {
                        dict?.exampleTranslation.orEmpty()
                    },
                    // Cards are queried in descending sortOrder. Starting below
                    // the existing minimum appends this scan after every saved card
                    // while preserving the image's top-to-bottom order.
                    sortOrder = bottomSortOrder - index
                )
            }
            val insertedCount = repository.insertCardsDeduplicating(cards)
            val duplicateCount = cards.size - insertedCount
            _ocrCandidates.value = emptyList()
            _importUiState.value = ImportUiState.Success(insertedCount)
            _dataMessage.value = if (duplicateCount > 0) {
                "已依畫面順序匯入 $insertedCount 張，略過或更新 $duplicateCount 張重複單字"
            } else {
                "已依畫面順序匯入 $insertedCount 張單字卡"
            }
        }
    }

    fun importExternalCards(
        candidates: List<ExternalCardCandidate>,
        targetDeckId: Long,
        onComplete: (Int) -> Unit = {}
    ) {
        val selected = candidates
            .filter { it.selected && it.word.isNotBlank() && it.definition.isNotBlank() }
            .distinctBy { it.word.trim().lowercase() }
        if (selected.isEmpty()) {
            onComplete(0)
            return
        }
        viewModelScope.launch {
            val topSortOrder = repository.nextSortOrder(targetDeckId, selected.size)
            val cards = selected.mapIndexed { index, candidate ->
                val dictionary = DictionaryEngine.findExact(candidate.word)
                Flashcard(
                    deckId = targetDeckId,
                    word = candidate.word.trim(),
                    phonetic = candidate.phonetic.ifBlank { dictionary?.phonetic.orEmpty() },
                    partOfSpeech = candidate.partOfSpeech.ifBlank { dictionary?.partOfSpeech.orEmpty() },
                    definition = OcrWordParser.cleanDefinition(candidate.definition),
                    exampleSentence = candidate.exampleSentence.ifBlank { dictionary?.exampleSentence.orEmpty() },
                    exampleTranslation = candidate.exampleTranslation.ifBlank {
                        dictionary?.exampleTranslation.orEmpty()
                    },
                    notes = candidate.sourceName.takeIf { it.isNotBlank() }?.let { "匯入來源：$it" }.orEmpty(),
                    sortOrder = topSortOrder - index
                )
            }
            val insertedCount = repository.insertCardsDeduplicating(cards)
            val duplicateCount = cards.size - insertedCount
            _dataMessage.value = if (duplicateCount > 0) {
                "已匯入 $insertedCount 張，略過或更新 $duplicateCount 張同資料夾重複單字"
            } else {
                "已依原始順序匯入 $insertedCount 張單字卡"
            }
            onComplete(insertedCount)
        }
    }

    fun openAssistant(contextDeckId: Long?) {
        assistantContextDeckId = contextDeckId
        val availableIds = allDecks.value.mapTo(linkedSetOf()) { it.id }
        _assistantSelectedDeckIds.value = contextDeckId?.let(::setOf) ?: when {
            settings.value.assistantScopeMode == "ALL" -> availableIds
            else -> settings.value.assistantScopeDeckIds.filterTo(linkedSetOf()) { it in availableIds }
                .ifEmpty { availableIds }
        }
        val currentId = _assistantCurrentConversationId.value
        if (currentId == null || _assistantConversations.value.none { it.id == currentId }) {
            val existing = _assistantConversations.value.maxByOrNull { it.updatedAt }
            if (existing != null) selectAssistantConversation(existing.id)
            else newAssistantConversation(false)
        } else {
            refreshAssistantMessages()
        }
    }

    fun setAssistantScope(deckIds: Set<Long>) {
        val availableIds = allDecks.value.mapTo(hashSetOf()) { it.id }
        _assistantSelectedDeckIds.value = deckIds.filterTo(linkedSetOf()) { it in availableIds }
        assistantContextDeckId = _assistantSelectedDeckIds.value.singleOrNull()
        viewModelScope.launch {
            settingsRepository.setAssistantScope(
                allSelected = availableIds.isNotEmpty() && _assistantSelectedDeckIds.value == availableIds,
                deckIds = _assistantSelectedDeckIds.value
            )
        }
    }

    fun newAssistantConversation(incognito: Boolean) {
        val now = System.currentTimeMillis()
        val conversation = AssistantConversation(
            id = UUID.randomUUID().toString(),
            title = if (incognito) "無痕對話" else "新對話",
            createdAt = now,
            updatedAt = now,
            incognito = incognito
        )
        _assistantConversations.value = listOf(conversation) + _assistantConversations.value
        _assistantCurrentConversationId.value = conversation.id
        _assistantPendingAction.value = null
        _assistantUndoAction.value = null
        refreshAssistantMessages()
        persistAssistantState()
    }

    fun selectAssistantConversation(conversationId: String) {
        if (_assistantConversations.value.none { it.id == conversationId }) return
        _assistantCurrentConversationId.value = conversationId
        _assistantPendingAction.value = null
        _assistantUndoAction.value = null
        refreshAssistantMessages()
    }

    fun deleteAssistantConversation(conversationId: String) {
        assistantAllMessages.filter { it.conversationId == conversationId }
            .flatMap { it.attachmentUris }
            .forEach(::deletePersistedAssistantAttachment)
        assistantAllMessages.removeAll { it.conversationId == conversationId }
        _assistantConversations.value = _assistantConversations.value.filterNot { it.id == conversationId }
        if (_assistantCurrentConversationId.value == conversationId) {
            val next = _assistantConversations.value.firstOrNull()
            if (next == null) newAssistantConversation(false)
            else selectAssistantConversation(next.id)
        }
        persistAssistantState()
    }

    fun clearAssistantChats() {
        assistantAllMessages.flatMap { it.attachmentUris }
            .forEach(::deletePersistedAssistantAttachment)
        assistantAllMessages.clear()
        _assistantConversations.value = emptyList()
        _assistantCurrentConversationId.value = null
        _assistantMessages.value = emptyList()
        _assistantPendingAction.value = null
        _assistantUndoAction.value = null
        assistantChatStore.clear()
        newAssistantConversation(false)
    }

    private fun deletePersistedAssistantAttachment(rawUri: String) {
        val uri = runCatching { Uri.parse(rawUri) }.getOrNull() ?: return
        if (uri.scheme != "file") return
        val file = uri.path?.let(::File) ?: return
        val attachmentDirectory = File(
            getApplication<Application>().filesDir,
            "assistant_images"
        ).canonicalFile
        val target = runCatching { file.canonicalFile }.getOrNull() ?: return
        if (target.parentFile == attachmentDirectory) target.delete()
    }

    fun sendAssistantMessage(text: String, attachments: List<Uri> = emptyList()) {
        val safeAttachments = attachments.distinct().take(5)
        val attachmentLabels = safeAttachments.mapIndexed { index, uri ->
            assistantAttachmentLabel(uri, index)
        }
        val request = text.trim().take(4_000).ifBlank {
            if (safeAttachments.isNotEmpty()) "請分析我附加的檔案。" else ""
        }
        if ((request.isBlank() && safeAttachments.isEmpty()) || _assistantBusy.value) return
        if (_assistantCurrentConversationId.value == null) newAssistantConversation(false)
        val conversationId = _assistantCurrentConversationId.value ?: return
        val userMessageId = appendAssistantMessage(
            "USER",
            request,
            attachmentUris = safeAttachments.map(Uri::toString)
        )
        updateAssistantConversationTitle(conversationId, request)

        val targetDeck = resolveAssistantDeck(request)
        val asksForSort = request.contains("排序") || request.contains("排列")
        val mentionsAlphabet = request.contains("字母") ||
            request.contains("A-Z", ignoreCase = true) || request.contains("英文順序")
        if (safeAttachments.isEmpty() && asksForSort && mentionsAlphabet) {
            if (targetDeck == null) {
                appendAssistantMessage("ASSISTANT", "請先進入一個資料夾，或在訊息中告訴我要整理哪個資料夾。")
                return
            }
            if (targetDeck.id !in _assistantSelectedDeckIds.value) {
                appendAssistantMessage(
                    "ASSISTANT",
                    "我可以讀取「${targetDeck.name}」的內容，但它不在目前允許編輯的範圍內。請先在輸入框上方勾選它，才能修改排序。"
                )
                return
            }
            prepareAlphabeticalSort(targetDeck, descending = request.contains("Z-A", ignoreCase = true))
            return
        }

        assistantRequestJob = viewModelScope.launch {
            _assistantBusy.value = true
            _assistantSendOutcome.value = null
            _assistantStatus.value = if (safeAttachments.isEmpty()) null else "正在準備附件…"
            runCatching {
                if (safeAttachments.isNotEmpty()) {
                    val persistedUris = withContext(Dispatchers.IO) {
                        persistAssistantAttachments(safeAttachments)
                    }
                    updateAssistantMessageAttachments(userMessageId, persistedUris)
                }
                if (!settings.value.aiEnabled || !settings.value.usePersonalAiApi) {
                    error("請先在設定中啟用 AI，並加入至少一組可用的 API Key")
                }
                val imageAttachments = mutableListOf<DirectAiService.ImagePayload>()
                val textAttachments = StringBuilder()
                val attachmentSummary = mutableListOf<String>()
                safeAttachments.forEachIndexed { index, uri ->
                    val resolver = getApplication<Application>().contentResolver
                    val mimeType = resolver.getType(uri).orEmpty().lowercase()
                    val label = attachmentLabels[index]
                    val lowerLabel = label.lowercase()
                    val isPdf = mimeType.contains("pdf") || lowerLabel.endsWith(".pdf")
                    val isKnownImage = mimeType.startsWith("image/") ||
                        listOf(".jpg", ".jpeg", ".png", ".webp", ".gif", ".heic", ".heif")
                            .any(lowerLabel::endsWith)
                    _assistantStatus.value = "正在讀取附件 ${index + 1}/${safeAttachments.size}：$label"

                    if (isPdf) {
                        val remainingImageSlots = 5 - imageAttachments.size
                        require(remainingImageSlots > 0) { "圖片與 PDF 頁面合計最多 5 張" }
                        val pages = DocumentParser.renderPdfPagesForAi(
                            getApplication(),
                            uri,
                            minOf(settings.value.pdfPageLimit, remainingImageSlots)
                        )
                        require(pages.isNotEmpty()) { "$label 沒有可讀取的頁面" }
                        imageAttachments += pages
                        attachmentSummary += "$label：${pages.size} 頁 PDF 圖片"
                    } else if (isKnownImage) {
                        require(imageAttachments.size < 5) { "圖片與 PDF 頁面合計最多 5 張" }
                        imageAttachments += AiImagePreprocessor.prepare(getApplication(), uri)
                        attachmentSummary += "$label：圖片"
                    } else {
                        val extracted = DocumentParser.extractTextFromUri(
                            getApplication(), uri, settings.value.pdfPageLimit
                        ).trim().take(20_000 - textAttachments.length.coerceAtMost(20_000))
                        require(extracted.isNotBlank()) { "$label 沒有讀取到可傳給 AI 的文字" }
                        textAttachments.append("\n\n--- 附件：$label ---\n").append(extracted)
                        attachmentSummary += "$label：${extracted.length} 字"
                    }
                }
                val contextDeck = targetDeck
                val contextCards = selectAssistantContextCards(request, contextDeck)
                val showProgress = isAssistantOperationRequest(request)
                val editableIds = _assistantSelectedDeckIds.value
                _assistantStatus.value = if (!showProgress) null else assistantDetailedStatus(
                    request = request,
                    contextDeck = contextDeck,
                    contextCardCount = contextCards.size,
                    editableDeckCount = editableIds.size
                )
                GeminiService.runAssistant(
                    buildAssistantPrompt(
                        buildString {
                            append(request)
                            if (attachmentSummary.isNotEmpty()) {
                                append("\n\n附件讀取摘要：\n")
                                append(attachmentSummary.joinToString("\n"))
                                append("\n請確實閱讀附件內容後再回答，不可聲稱沒有收到附件。")
                            }
                            append(textAttachments)
                        },
                        contextDeck,
                        contextCards
                    ),
                    imageAttachments
                )
            }.onSuccess { result ->
                handleAssistantResult(result, targetDeck)
                if (safeAttachments.isNotEmpty()) {
                    _assistantSendOutcome.value = AssistantSendOutcome(succeeded = true)
                }
            }.onFailure { error ->
                if (error !is CancellationException) {
                    if (safeAttachments.isNotEmpty()) {
                        _assistantSendOutcome.value = AssistantSendOutcome(
                            succeeded = false,
                            errorMessage = error.localizedMessage ?: "附件無法讀取或傳送"
                        )
                    }
                    appendAssistantMessage(
                        "ASSISTANT",
                        "這次沒有完成：${error.localizedMessage ?: "AI 服務暫時無法使用"}"
                    )
                }
            }
            _assistantStatus.value = null
            _assistantBusy.value = false
            assistantRequestJob = null
        }
    }

    private fun assistantAttachmentLabel(uri: Uri, index: Int): String {
        val resolver = getApplication<Application>().contentResolver
        val displayName = runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
        return displayName?.trim()?.take(120)?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment?.substringAfterLast('/')?.take(120)
            ?: "附件 ${index + 1}"
    }

    private fun persistAssistantAttachments(uris: List<Uri>): List<String> {
        val application = getApplication<Application>()
        val resolver = application.contentResolver
        val imageDirectory = File(application.filesDir, "assistant_images").apply { mkdirs() }
        return uris.map { uri ->
            val mimeType = resolver.getType(uri).orEmpty().lowercase()
            if (!mimeType.startsWith("image/")) return@map uri.toString()
            runCatching {
                val extension = when (mimeType) {
                    "image/png" -> "png"
                    "image/webp" -> "webp"
                    "image/gif" -> "gif"
                    "image/heic", "image/heif" -> "heic"
                    else -> "jpg"
                }
                val target = File(imageDirectory, "${UUID.randomUUID()}.$extension")
                resolver.openInputStream(uri)?.use { input ->
                    target.outputStream().use(input::copyTo)
                } ?: error("無法保存圖片附件")
                Uri.fromFile(target).toString()
            }.getOrDefault(uri.toString())
        }
    }

    private fun updateAssistantMessageAttachments(messageId: String, uris: List<String>) {
        val index = assistantAllMessages.indexOfFirst { it.id == messageId }
        if (index < 0) return
        assistantAllMessages[index] = assistantAllMessages[index].copy(attachmentUris = uris)
        refreshAssistantMessages()
        persistAssistantState()
    }

    fun stopAssistantRequest() {
        assistantRequestJob?.cancel()
        assistantRequestJob = null
        _assistantStatus.value = null
        _assistantBusy.value = false
    }

    fun cancelAssistantAction() {
        _assistantPendingAction.value = null
        appendAssistantMessage("ASSISTANT", "已取消，沒有修改任何資料。", kind = "STATUS")
    }

    fun completeAssistantQuiz(messageId: String, correctCount: Int, totalCount: Int) {
        updateAssistantQuizPayload(messageId) { root ->
            val safeTotal = totalCount.coerceAtLeast(0)
            val safeCorrect = correctCount.coerceIn(0, safeTotal)
            val completedAt = System.currentTimeMillis()
            val attempts = root.optJSONArray("attempts") ?: JSONArray()
            attempts.put(
                JSONObject()
                    .put("correct", safeCorrect)
                    .put("total", safeTotal)
                    .put("completedAt", completedAt)
            )
            root.put("attempts", attempts)
                .put("completed", true)
                .put("currentIndex", safeTotal)
                .put("selectedIndex", -1)
                .put("latestCorrect", safeCorrect)
                .put("latestTotal", safeTotal)
                .put("completedAt", completedAt)
        }
    }

    fun restartAssistantQuiz(messageId: String) {
        updateAssistantQuizPayload(messageId) { root ->
            root.put("completed", false)
                .put("currentIndex", 0)
                .put("selectedIndex", -1)
                .put("correctCount", 0)
                .put("wrongAnswers", JSONArray())
                .put("attempts", JSONArray())
                .remove("latestCorrect")
            root.remove("latestTotal")
            root.remove("completedAt")
        }
    }

    fun saveAssistantQuizProgress(
        messageId: String,
        currentIndex: Int,
        selectedIndex: Int,
        correctCount: Int,
        wrongAnswers: Map<Int, Int>
    ) {
        updateAssistantQuizPayload(messageId) { root ->
            val questions = root.optJSONArray("questions") ?: JSONArray()
            val total = questions.length()
            root.put("completed", false)
                .put("currentIndex", currentIndex.coerceIn(0, total.coerceAtLeast(0)))
                .put("selectedIndex", selectedIndex)
                .put("correctCount", correctCount.coerceIn(0, total.coerceAtLeast(0)))
                .put("updatedAt", System.currentTimeMillis())
                .put(
                    "wrongAnswers",
                    JSONArray(wrongAnswers.entries.sortedBy { it.key }.map {
                        JSONObject().put("questionIndex", it.key).put("selectedIndex", it.value)
                    })
                )
        }
    }

    fun confirmAssistantAction() {
        val action = _assistantPendingAction.value ?: return
        _assistantPendingAction.value = null
        viewModelScope.launch {
            _assistantBusy.value = true
            _assistantStatus.value = "正在執行：${action.title}"
            runCatching {
                when (action.type) {
                    "SORT" -> {
                        val payload = JSONObject(action.payload)
                        val newOrder = payload.getJSONArray("newOrder").longValues()
                        repository.reorderCards(action.deckId, newOrder)
                        AssistantPendingAction(
                            id = UUID.randomUUID().toString(),
                            type = "RESTORE_ORDER",
                            deckId = action.deckId,
                            title = "復原排序",
                            description = "恢復執行 AI 排序前的單字順序",
                            payload = JSONObject()
                                .put("order", payload.getJSONArray("oldOrder"))
                                .toString()
                        )
                    }
                    "COUNTABILITY" -> {
                        val updates = JSONArray(action.payload)
                        val currentById = allCards.value.associateBy { it.id }
                        val changed = buildList {
                            for (index in 0 until updates.length()) {
                                val item = updates.optJSONObject(index) ?: continue
                                val card = currentById[item.optLong("cardId")] ?: continue
                                add(card.copy(partOfSpeech = item.optString("newPartOfSpeech")))
                            }
                        }
                        repository.updateCards(changed)
                        AssistantPendingAction(
                            id = UUID.randomUUID().toString(),
                            type = "RESTORE_POS",
                            deckId = action.deckId,
                            title = "復原詞性修改",
                            description = "恢復 AI 補充可數性前的詞性",
                            payload = action.payload
                        )
                    }
                    "CREATE_FOLDER" -> {
                        val payload = JSONObject(action.payload)
                        val name = payload.getString("name").trim()
                        val category = payload.optString("category", "通用").trim().ifBlank { "通用" }
                        val duplicate = allDecks.value.any {
                            it.name.equals(name, ignoreCase = true) &&
                                it.category.equals(category, ignoreCase = true)
                        }
                        check(!duplicate) { "「$category」課程中已經有同名資料夾" }
                        val newId = repository.insertDeck(
                            Deck(
                                name = name,
                                category = category,
                                description = payload.optString("description").trim().take(200),
                                colorHex = payload.optString("colorHex")
                                    .takeIf { it.matches(Regex("#[0-9A-Fa-f]{6}")) }
                                    ?: "#426B63"
                            )
                        )
                        AssistantPendingAction(
                            id = UUID.randomUUID().toString(),
                            type = "DELETE_CREATED_FOLDER",
                            deckId = newId,
                            title = "復原新增資料夾",
                            description = "刪除剛建立的「$name」",
                            payload = ""
                        )
                    }
                    "UPDATE_FOLDER" -> {
                        val payload = JSONObject(action.payload)
                        val current = allDecks.value.firstOrNull { it.id == action.deckId }
                            ?: error("找不到要修改的資料夾")
                        val newName = payload.getString("name").trim().take(60)
                        val newCategory = payload.getString("category").trim().take(40)
                        require(newName.isNotBlank() && newCategory.isNotBlank()) { "名稱與課程不能空白" }
                        check(allDecks.value.none {
                            it.id != current.id && it.name.equals(newName, true) && it.category.equals(newCategory, true)
                        }) { "目標課程中已經有同名資料夾" }
                        repository.updateDeck(current.copy(name = newName, category = newCategory))
                        AssistantPendingAction(
                            id = UUID.randomUUID().toString(), type = "RESTORE_FOLDER",
                            deckId = current.id, title = "復原資料夾名稱與課程",
                            description = "恢復為「${current.name}」／${current.category}",
                            payload = JSONObject().put("name", current.name).put("category", current.category).toString()
                        )
                    }
                    "UPDATE_GRAMMAR" -> {
                        val payload = JSONObject(action.payload)
                        val current = db.grammarNoteDao().getById(action.deckId)
                            ?: error("找不到要修改的文法")
                        val updated = current.copy(
                            course = payload.optString("course", current.course).trim().ifBlank { current.course },
                            folder = payload.optString("folder", current.folder).trim().ifBlank { current.folder },
                            title = payload.optString("title", current.title).trim().ifBlank { current.title },
                            category = payload.optString("category", current.category).trim().ifBlank { current.category },
                            level = payload.optString("level", current.level).trim().ifBlank { current.level },
                            summary = payload.optString("summary", current.summary).trim().ifBlank { current.summary },
                            tags = payload.optString("tags", current.tags).trim(),
                            updatedAt = System.currentTimeMillis()
                        )
                        db.grammarNoteDao().update(updated)
                        AssistantPendingAction(
                            id = UUID.randomUUID().toString(), type = "RESTORE_GRAMMAR", deckId = current.id,
                            title = "復原文法修改", description = "恢復「${current.title}」修改前的基本資料",
                            payload = JSONObject().put("course", current.course).put("folder", current.folder)
                                .put("title", current.title).put("category", current.category).put("level", current.level)
                                .put("summary", current.summary).put("tags", current.tags).toString()
                        )
                    }
                    "TRANSFER_CARDS" -> executeAssistantCardTransfer(action)
                    "ADD_CARDS" -> executeAssistantAddCards(action)
                    "ENRICH_CARDS" -> executeAssistantCardEnrichment(action)
                    "SPLIT_FOLDER" -> executeAssistantSplitFolder(action)
                    "MERGE_FOLDERS" -> executeAssistantMergeFolders(action)
                    "CREATE_WEAKNESS_FOLDER" -> executeAssistantWeaknessFolder(action)
                    "UPDATE_DAILY_PLAN" -> {
                        val payload = JSONObject(action.payload)
                        val old = JSONObject()
                            .put("dailyGoalCards", settings.value.dailyGoalCards)
                            .put("dailyNewCardLimit", settings.value.dailyNewCardLimit)
                            .put("dailyReviewLimit", settings.value.dailyReviewLimit)
                        settingsRepository.setInt("dailyGoalCards", payload.optInt("dailyGoalCards", settings.value.dailyGoalCards))
                        settingsRepository.setInt("dailyNewCardLimit", payload.optInt("dailyNewCardLimit", settings.value.dailyNewCardLimit))
                        settingsRepository.setInt("dailyReviewLimit", payload.optInt("dailyReviewLimit", settings.value.dailyReviewLimit))
                        VocabWidgetProvider.requestUpdate(getApplication())
                        AssistantPendingAction(
                            id = UUID.randomUUID().toString(), type = "RESTORE_DAILY_PLAN", deckId = -1,
                            title = "復原每日學習計畫", description = "恢復修改前的每日目標與上限",
                            payload = old.toString()
                        )
                    }
                    else -> error("不支援的操作")
                }
            }.onSuccess { undo ->
                if (undo.type == "NONE") {
                    _assistantUndoAction.value = null
                    appendAssistantMessage("ASSISTANT", "已完成：${action.title}", kind = "STATUS")
                } else {
                    _assistantUndoAction.value = undo
                    appendAssistantMessage(
                        "ASSISTANT", "已完成：${action.title}", kind = "ACTION", payload = undo.toJson()
                    )
                }
            }.onFailure { error ->
                appendAssistantMessage(
                    "ASSISTANT",
                    "操作失敗，沒有套用資料：${error.localizedMessage ?: "未知錯誤"}",
                    kind = "STATUS"
                )
            }
            _assistantStatus.value = null
            _assistantBusy.value = false
        }
    }

    fun undoAssistantAction() {
        val action = _assistantUndoAction.value ?: return
        _assistantUndoAction.value = null
        viewModelScope.launch {
            _assistantBusy.value = true
            _assistantStatus.value = action.title
            runCatching {
                when (action.type) {
                    "RESTORE_ORDER" -> repository.reorderCards(
                        action.deckId,
                        JSONObject(action.payload).getJSONArray("order").longValues()
                    )
                    "RESTORE_POS" -> {
                        val updates = JSONArray(action.payload)
                        val currentById = allCards.value.associateBy { it.id }
                        val restored = buildList {
                            for (index in 0 until updates.length()) {
                                val item = updates.optJSONObject(index) ?: continue
                                val card = currentById[item.optLong("cardId")] ?: continue
                                add(card.copy(partOfSpeech = item.optString("oldPartOfSpeech")))
                            }
                        }
                        repository.updateCards(restored)
                    }
                    "DELETE_CREATED_FOLDER" -> {
                        val deck = allDecks.value.firstOrNull { it.id == action.deckId }
                            ?: error("找不到剛建立的資料夾")
                        check(allCards.value.none { it.deckId == deck.id }) {
                            "資料夾已經加入單字，為避免遺失資料，無法直接復原刪除"
                        }
                        repository.deleteDeck(deck)
                    }
                    "RESTORE_FOLDER" -> {
                        val deck = allDecks.value.firstOrNull { it.id == action.deckId }
                            ?: error("找不到資料夾")
                        val payload = JSONObject(action.payload)
                        repository.updateDeck(deck.copy(name = payload.getString("name"), category = payload.getString("category")))
                    }
                    "RESTORE_GRAMMAR" -> {
                        val current = db.grammarNoteDao().getById(action.deckId) ?: error("找不到文法")
                        val payload = JSONObject(action.payload)
                        db.grammarNoteDao().update(current.copy(
                            course = payload.getString("course"), folder = payload.getString("folder"),
                            title = payload.getString("title"), category = payload.getString("category"),
                            level = payload.getString("level"), summary = payload.getString("summary"),
                            tags = payload.getString("tags"), updatedAt = System.currentTimeMillis()
                        ))
                    }
                    "RESTORE_CARD_LOCATIONS" -> {
                        val mappings = JSONArray(action.payload)
                        for (index in 0 until mappings.length()) {
                            val item = mappings.optJSONObject(index) ?: continue
                            repository.moveCards(item.getJSONArray("cardIds").longValues(), item.getLong("deckId"))
                        }
                    }
                    "DELETE_CREATED_CARDS" -> {
                        val ids = JSONArray(action.payload).longValues()
                        ids.forEach { repository.deleteCardById(it) }
                    }
                    "RESTORE_CARD_FIELDS" -> {
                        val values = JSONArray(action.payload)
                        val currentById = allCards.value.associateBy { it.id }
                        val restored = buildList {
                            for (index in 0 until values.length()) {
                                val item = values.optJSONObject(index) ?: continue
                                val card = currentById[item.optLong("cardId")] ?: continue
                                add(card.copy(
                                    phonetic = item.optString("phonetic"),
                                    partOfSpeech = item.optString("partOfSpeech"),
                                    definition = item.optString("definition"),
                                    exampleSentence = item.optString("exampleSentence"),
                                    exampleTranslation = item.optString("exampleTranslation")
                                ))
                            }
                        }
                        repository.updateCards(restored)
                    }
                    "UNDO_SPLIT" -> undoAssistantSplit(JSONObject(action.payload))
                    "UNDO_WEAKNESS_FOLDER" -> undoAssistantCreatedFolder(JSONObject(action.payload))
                    "RESTORE_DAILY_PLAN" -> {
                        val payload = JSONObject(action.payload)
                        settingsRepository.setInt("dailyGoalCards", payload.getInt("dailyGoalCards"))
                        settingsRepository.setInt("dailyNewCardLimit", payload.getInt("dailyNewCardLimit"))
                        settingsRepository.setInt("dailyReviewLimit", payload.getInt("dailyReviewLimit"))
                        VocabWidgetProvider.requestUpdate(getApplication())
                    }
                }
            }.onSuccess {
                appendAssistantMessage("ASSISTANT", "已復原上一個 AI 操作。", kind = "ACTION_UNDONE")
            }.onFailure { error ->
                appendAssistantMessage("ASSISTANT", "復原失敗：${error.localizedMessage}", kind = "STATUS")
            }
            _assistantStatus.value = null
            _assistantBusy.value = false
        }
    }

    private fun prepareAssistantTransfer(result: JSONObject, message: String) {
        val targetId = result.optLong("targetDeckId")
        val target = allDecks.value.firstOrNull { it.id == targetId }
        val ids = result.optJSONArray("cardIds")?.longValues().orEmpty().distinct()
        val actual = allCards.value.filter { it.id in ids }.filter { it.deckId != targetId }
        val mode = if (result.optString("mode").equals("COPY", true)) "COPY" else "MOVE"
        if (target == null || target.id !in _assistantSelectedDeckIds.value || actual.isEmpty()) {
            appendAssistantMessage("ASSISTANT", "$message\n\n找不到可安全${if (mode == "COPY") "複製" else "移動"}的目標或卡片。")
            return
        }
        _assistantPendingAction.value = AssistantPendingAction(
            id = UUID.randomUUID().toString(), type = "TRANSFER_CARDS", deckId = target.id,
            title = "${if (mode == "COPY") "複製" else "移動"} ${actual.size} 張單字卡",
            description = "目標：${target.name}\n同名單字會跳過，不會產生重複卡片。",
            payload = JSONObject().put("targetDeckId", target.id).put("mode", mode)
                .put("cardIds", JSONArray(actual.map { it.id })).toString()
        )
        appendAssistantMessage("ASSISTANT", "已核對 ${actual.size} 張真實卡片，請確認操作。", kind = "STATUS")
    }

    private fun prepareAssistantAddCards(result: JSONObject, message: String) {
        val target = allDecks.value.firstOrNull { it.id == result.optLong("targetDeckId") }
        val cards = result.optJSONArray("cards") ?: JSONArray()
        if (target == null || target.id !in _assistantSelectedDeckIds.value || cards.length() == 0) {
            appendAssistantMessage("ASSISTANT", "$message\n\n請指定允許編輯的目標資料夾，並提供要加入的單字。")
            return
        }
        val existing = allCards.value.filter { it.deckId == target.id }.mapTo(hashSetOf()) { it.normalizedWord }
        val safe = JSONArray()
        val seen = hashSetOf<String>()
        for (index in 0 until cards.length()) {
            val item = cards.optJSONObject(index) ?: continue
            val normalized = item.optString("word").trim().lowercase()
            if (normalized.isNotBlank() && normalized !in existing && seen.add(normalized)) safe.put(item)
        }
        if (safe.length() == 0) {
            appendAssistantMessage("ASSISTANT", "推薦單字都已存在，沒有建立重複卡片。")
            return
        }
        _assistantPendingAction.value = AssistantPendingAction(
            id = UUID.randomUUID().toString(), type = "ADD_CARDS", deckId = target.id,
            title = "加入 ${safe.length()} 個 AI 推薦單字", description = "目標：${target.name}\n只會加入預覽中的非重複單字。",
            payload = JSONObject().put("cards", safe).toString()
        )
        appendAssistantMessage("ASSISTANT", "已準備 ${safe.length()} 個模型知識補充單字，請確認後加入。", kind = "STATUS")
    }

    private fun prepareAssistantEnrichment(result: JSONObject, message: String) {
        val source = result.optJSONArray("updates") ?: JSONArray()
        val currentById = allCards.value.associateBy { it.id }
        val safe = JSONArray()
        for (index in 0 until source.length()) {
            val item = source.optJSONObject(index) ?: continue
            val card = currentById[item.optLong("cardId")] ?: continue
            if (card.deckId !in _assistantSelectedDeckIds.value) continue
            if (card.phonetic.isBlank() || card.partOfSpeech.isBlank() || card.definition.isBlank() ||
                card.exampleSentence.isBlank() || card.exampleTranslation.isBlank()) safe.put(item)
        }
        if (safe.length() == 0) {
            appendAssistantMessage("ASSISTANT", "$message\n\n沒有可安全補齊的空白欄位。")
            return
        }
        _assistantPendingAction.value = AssistantPendingAction(
            id = UUID.randomUUID().toString(), type = "ENRICH_CARDS", deckId = 0,
            title = "補齊 ${safe.length()} 張單字卡", description = "只填入原本空白的欄位，不會覆蓋既有內容。",
            payload = JSONObject().put("updates", safe).toString()
        )
        appendAssistantMessage("ASSISTANT", "已核對空白欄位，請確認批次補齊。", kind = "STATUS")
    }

    private fun prepareAssistantSplit(result: JSONObject, fallbackDeck: Deck?, message: String) {
        val source = allDecks.value.firstOrNull { it.id == result.optLong("sourceDeckId") } ?: fallbackDeck
        val groups = result.optJSONArray("groups") ?: JSONArray()
        if (source == null || source.id !in _assistantSelectedDeckIds.value || groups.length() < 2) {
            appendAssistantMessage("ASSISTANT", "$message\n\n需要一個可編輯的來源資料夾與至少兩個有效分類。")
            return
        }
        val validIds = allCards.value.filter { it.deckId == source.id }.mapTo(hashSetOf()) { it.id }
        var count = 0
        for (index in 0 until groups.length()) {
            count += groups.optJSONObject(index)?.optJSONArray("cardIds")?.longValues()?.count { it in validIds } ?: 0
        }
        if (count == 0) {
            appendAssistantMessage("ASSISTANT", "AI 沒有回傳可驗證的卡片分類，因此不會拆分。")
            return
        }
        _assistantPendingAction.value = AssistantPendingAction(
            id = UUID.randomUUID().toString(), type = "SPLIT_FOLDER", deckId = source.id,
            title = "拆分資料夾「${source.name}」", description = "建立 ${groups.length()} 個分類並移動約 $count 張卡片；未分類卡片留在原處。",
            payload = JSONObject().put("groups", groups).toString()
        )
        appendAssistantMessage("ASSISTANT", "分類結果已和真實卡片核對，請確認拆分。", kind = "STATUS")
    }

    private fun prepareAssistantMerge(result: JSONObject, message: String) {
        val targetId = result.optLong("targetDeckId")
        val sourceIds = result.optJSONArray("sourceDeckIds")?.longValues().orEmpty().filter { it != targetId }.distinct()
        val target = allDecks.value.firstOrNull { it.id == targetId }
        if (target == null || sourceIds.isEmpty() || (sourceIds + targetId).any { it !in _assistantSelectedDeckIds.value }) {
            appendAssistantMessage("ASSISTANT", "$message\n\n請指定至少兩個允許編輯的資料夾。")
            return
        }
        val affected = allCards.value.count { it.deckId in sourceIds }
        _assistantPendingAction.value = AssistantPendingAction(
            id = UUID.randomUUID().toString(), type = "MERGE_FOLDERS", deckId = target.id,
            title = "合併 ${sourceIds.size + 1} 個資料夾", description =
                "目標：${target.name}\n移動 $affected 張卡片並刪除來源資料夾。重複字與學習紀錄會整合，此操作無法完整復原。",
            payload = JSONObject().put("targetDeckId", target.id).put("sourceDeckIds", JSONArray(sourceIds)).toString()
        )
        appendAssistantMessage("ASSISTANT", "已準備合併；請特別確認不可完整復原的影響。", kind = "STATUS")
    }

    private fun prepareAssistantWeaknessFolder(result: JSONObject, message: String) {
        val name = result.optString("name", "弱點複習").trim().take(60).ifBlank { "弱點複習" }
        val category = result.optString("category").trim().take(40)
        if (category.isBlank()) {
            appendAssistantMessage("ASSISTANT", "「$name」要放在哪個課程？")
            return
        }
        val weakLogIds = recentStudyLogs.value.filter { it.rating <= 2 }.mapTo(hashSetOf()) { it.cardId }
        val requested = result.optJSONArray("cardIds")?.longValues().orEmpty().toSet()
        val safeIds = allCards.value.filter { it.id in requested && (it.mistakeCount > 0 || it.id in weakLogIds) }.map { it.id }
        if (safeIds.isEmpty()) {
            appendAssistantMessage("ASSISTANT", "$message\n\n目前沒有可由真實錯題紀錄驗證的卡片。")
            return
        }
        _assistantPendingAction.value = AssistantPendingAction(
            id = UUID.randomUUID().toString(), type = "CREATE_WEAKNESS_FOLDER", deckId = 0,
            title = "建立弱點資料夾「$name」", description = "課程：$category\n複製 ${safeIds.size} 張真實錯題卡，原卡片不會移動。",
            payload = JSONObject().put("name", name).put("category", category).put("cardIds", JSONArray(safeIds)).toString()
        )
        appendAssistantMessage("ASSISTANT", "已依錯題次數與低評分紀錄選出 ${safeIds.size} 張卡片。", kind = "STATUS")
    }

    private fun prepareAssistantDailyPlan(result: JSONObject, message: String) {
        val goal = result.optInt("dailyGoalCards", settings.value.dailyGoalCards).coerceIn(5, 100)
        val newLimit = result.optInt("dailyNewCardLimit", settings.value.dailyNewCardLimit).coerceIn(1, 100)
        val reviewLimit = result.optInt("dailyReviewLimit", settings.value.dailyReviewLimit).coerceIn(10, 500)
        _assistantPendingAction.value = AssistantPendingAction(
            id = UUID.randomUUID().toString(), type = "UPDATE_DAILY_PLAN", deckId = -1,
            title = "調整每日學習計畫", description = "每日目標：$goal 張\n新卡上限：$newLimit 張\n複習上限：$reviewLimit 張",
            payload = JSONObject().put("dailyGoalCards", goal).put("dailyNewCardLimit", newLimit).put("dailyReviewLimit", reviewLimit).toString()
        )
        appendAssistantMessage("ASSISTANT", "$message\n\n計畫已準備好，確認後才會更新設定。", kind = "STATUS")
    }

    private fun buildVerifiedAuditReport(result: JSONObject): String {
        val cards = allCards.value
        val duplicates = cards.groupBy { it.normalizedWord }
            .filter { (word, values) -> word.isNotBlank() && values.map { it.deckId }.distinct().size > 1 }
            .values.take(20)
        val incomplete = cards.filter {
            it.partOfSpeech.isBlank() || it.definition.isBlank() ||
                it.exampleSentence.isBlank() || it.exampleTranslation.isBlank()
        }.take(30)
        val suspected = result.optJSONArray("suspectedTypos") ?: JSONArray()
        val byId = cards.associateBy { it.id }
        val typoLines = buildList {
            for (index in 0 until suspected.length()) {
                val item = suspected.optJSONObject(index) ?: continue
                val card = byId[item.optLong("cardId")] ?: continue
                val suggestion = item.optString("suggestion").trim().take(100)
                if (suggestion.isBlank() || suggestion.equals(card.word, true)) continue
                add("• ${card.word} → 可能是 $suggestion：${item.optString("reason").trim().take(160)}")
            }
        }.take(20)
        return buildString {
            append("單字庫檢查結果\n\n")
            append("跨資料夾同字：${duplicates.size} 組")
            if (duplicates.isNotEmpty()) append("\n" + duplicates.joinToString("\n") { group ->
                "• ${group.first().word}（${group.size} 張）"
            })
            append("\n\n資料不完整：${incomplete.size} 張")
            if (incomplete.isNotEmpty()) append("\n" + incomplete.joinToString("\n") { card ->
                val missing = buildList {
                    if (card.partOfSpeech.isBlank()) add("詞性")
                    if (card.definition.isBlank()) add("解釋")
                    if (card.exampleSentence.isBlank()) add("例句")
                    if (card.exampleTranslation.isBlank()) add("例句翻譯")
                }
                "• ${card.word}：缺少${missing.joinToString("、")}"
            })
            append("\n\n疑似拼字問題：${typoLines.size} 張（AI 建議，修改前仍需確認）")
            if (typoLines.isNotEmpty()) append("\n" + typoLines.joinToString("\n"))
        }
    }

    private fun buildVerifiedLearningAnalysis(result: JSONObject): String {
        val logs = recentStudyLogs.value
        if (logs.isEmpty()) return "目前還沒有真實學習紀錄可以分析。"
        val byId = allCards.value.associateBy { it.id }
        val lowLogs = logs.filter { it.rating <= 2 }
        val hardCards = lowLogs.groupingBy { it.cardId }.eachCount().entries
            .sortedByDescending { it.value }.take(10)
        val partStats = lowLogs.mapNotNull { byId[it.cardId]?.partOfSpeech?.takeIf(String::isNotBlank) }
            .groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(6)
        val verified = buildString {
            append("真實學習紀錄分析\n\n")
            append("總複習紀錄：${logs.size} 次\n")
            append("Again／Hard：${lowLogs.size} 次\n")
            append("Good／Easy：${logs.size - lowLogs.size} 次")
            if (hardCards.isNotEmpty()) append("\n\n最常答錯：\n" + hardCards.joinToString("\n") {
                "• ${byId[it.key]?.word ?: "卡片 #${it.key}"}：${it.value} 次"
            })
            if (partStats.isNotEmpty()) append("\n\n較弱詞性：\n" + partStats.joinToString("\n") {
                "• ${it.key}：${it.value} 次低評分"
            })
        }
        val interpretation = formatAssistantDisplayText(result.optString("analysis"))
        return if (interpretation.isBlank()) verified else "$verified\n\nAI 學習建議\n$interpretation"
    }

    private suspend fun executeAssistantCardTransfer(action: AssistantPendingAction): AssistantPendingAction {
        val payload = JSONObject(action.payload)
        val targetDeckId = payload.getLong("targetDeckId")
        val target = allDecks.value.firstOrNull { it.id == targetDeckId } ?: error("找不到目標資料夾")
        check(target.id in _assistantSelectedDeckIds.value) { "目標資料夾不在可編輯範圍" }
        val requested = payload.getJSONArray("cardIds").longValues().distinct()
        val currentById = allCards.value.associateBy { it.id }
        val cards = requested.mapNotNull(currentById::get).filter { it.deckId != targetDeckId }
        require(cards.isNotEmpty()) { "沒有可處理的單字" }
        val existingWords = allCards.value.filter { it.deckId == targetDeckId }.mapTo(hashSetOf()) { it.normalizedWord }
        val safeCards = cards.filter { it.normalizedWord !in existingWords }.distinctBy { it.normalizedWord }
        require(safeCards.isNotEmpty()) { "目標資料夾已經包含這些單字" }
        return if (payload.optString("mode").equals("COPY", true)) {
            val insertedIds = safeCards.map { repository.insertCard(it.copyForAssistantDeck(targetDeckId)) }
            AssistantPendingAction(
                id = UUID.randomUUID().toString(), type = "DELETE_CREATED_CARDS", deckId = targetDeckId,
                title = "復原複製單字", description = "刪除剛複製的 ${insertedIds.size} 張卡片",
                payload = JSONArray(insertedIds).toString()
            )
        } else {
            val original = safeCards.groupBy { it.deckId }.map { (deckId, values) ->
                JSONObject().put("deckId", deckId).put("cardIds", JSONArray(values.map { it.id }))
            }
            check(original.all { item -> item.getLong("deckId") in _assistantSelectedDeckIds.value }) {
                "部分來源資料夾不在可編輯範圍"
            }
            repository.moveCards(safeCards.map { it.id }, targetDeckId)
            AssistantPendingAction(
                id = UUID.randomUUID().toString(), type = "RESTORE_CARD_LOCATIONS", deckId = targetDeckId,
                title = "復原移動單字", description = "將 ${safeCards.size} 張卡片移回原資料夾",
                payload = JSONArray(original).toString()
            )
        }
    }

    private suspend fun executeAssistantAddCards(action: AssistantPendingAction): AssistantPendingAction {
        val target = allDecks.value.firstOrNull { it.id == action.deckId } ?: error("找不到目標資料夾")
        check(target.id in _assistantSelectedDeckIds.value) { "目標資料夾不在可編輯範圍" }
        val source = JSONObject(action.payload).getJSONArray("cards")
        val existing = allCards.value.filter { it.deckId == target.id }.mapTo(hashSetOf()) { it.normalizedWord }
        val inserted = mutableListOf<Long>()
        val seen = hashSetOf<String>()
        for (index in 0 until source.length()) {
            val item = source.optJSONObject(index) ?: continue
            val word = item.optString("word").trim().take(100)
            val normalized = word.lowercase()
            if (word.isBlank() || normalized in existing || !seen.add(normalized)) continue
            inserted += repository.insertCard(
                Flashcard(
                    deckId = target.id, word = word,
                    phonetic = item.optString("phonetic").trim().take(100),
                    partOfSpeech = item.optString("partOfSpeech").trim().take(80),
                    definition = item.optString("definition").trim().take(500),
                    exampleSentence = item.optString("exampleSentence").trim().take(600),
                    exampleTranslation = item.optString("exampleTranslation").trim().take(600),
                    notes = "AI 知識補充"
                )
            )
        }
        require(inserted.isNotEmpty()) { "沒有可新增的非重複單字" }
        return AssistantPendingAction(
            id = UUID.randomUUID().toString(), type = "DELETE_CREATED_CARDS", deckId = target.id,
            title = "復原加入推薦單字", description = "刪除剛加入的 ${inserted.size} 張卡片",
            payload = JSONArray(inserted).toString()
        )
    }

    private suspend fun executeAssistantCardEnrichment(action: AssistantPendingAction): AssistantPendingAction {
        val updates = JSONObject(action.payload).getJSONArray("updates")
        val currentById = allCards.value.associateBy { it.id }
        val old = JSONArray()
        val changed = buildList {
            for (index in 0 until updates.length()) {
                val item = updates.optJSONObject(index) ?: continue
                val card = currentById[item.optLong("cardId")] ?: continue
                if (card.deckId !in _assistantSelectedDeckIds.value) continue
                old.put(JSONObject()
                    .put("cardId", card.id).put("phonetic", card.phonetic)
                    .put("partOfSpeech", card.partOfSpeech).put("definition", card.definition)
                    .put("exampleSentence", card.exampleSentence).put("exampleTranslation", card.exampleTranslation))
                add(card.copy(
                    phonetic = card.phonetic.ifBlank { item.optString("phonetic").trim().take(100) },
                    partOfSpeech = card.partOfSpeech.ifBlank { item.optString("partOfSpeech").trim().take(80) },
                    definition = card.definition.ifBlank { item.optString("definition").trim().take(500) },
                    exampleSentence = card.exampleSentence.ifBlank { item.optString("exampleSentence").trim().take(600) },
                    exampleTranslation = card.exampleTranslation.ifBlank { item.optString("exampleTranslation").trim().take(600) }
                ))
            }
        }.filter { updated -> currentById[updated.id] != updated }
        require(changed.isNotEmpty()) { "沒有找到需要補齊的空白欄位" }
        repository.updateCards(changed)
        return AssistantPendingAction(
            id = UUID.randomUUID().toString(), type = "RESTORE_CARD_FIELDS", deckId = action.deckId,
            title = "復原批次補齊", description = "恢復 ${changed.size} 張卡片原本的空白欄位",
            payload = old.toString()
        )
    }

    private suspend fun executeAssistantSplitFolder(action: AssistantPendingAction): AssistantPendingAction {
        val payload = JSONObject(action.payload)
        val sourceDeck = allDecks.value.firstOrNull { it.id == action.deckId } ?: error("找不到來源資料夾")
        check(sourceDeck.id in _assistantSelectedDeckIds.value) { "來源資料夾不在可編輯範圍" }
        val sourceCardIds = allCards.value.filter { it.deckId == sourceDeck.id }.mapTo(hashSetOf()) { it.id }
        val groups = payload.getJSONArray("groups")
        val planned = buildList {
            val used = hashSetOf<Long>()
            for (index in 0 until groups.length()) {
                val group = groups.optJSONObject(index) ?: continue
                val name = group.optString("name").trim().take(60)
                val category = group.optString("category", sourceDeck.category).trim().take(40).ifBlank { sourceDeck.category }
                val ids = group.optJSONArray("cardIds")?.longValues().orEmpty()
                    .filter { it in sourceCardIds && used.add(it) }
                if (name.isNotBlank() && ids.isNotEmpty()) add(Triple(name, category, ids))
            }
        }
        require(planned.size >= 2) { "至少需要兩個有效分類才能拆分" }
        planned.forEach { (name, category, _) ->
            check(allDecks.value.none { it.name.equals(name, true) && it.category.equals(category, true) }) {
                "「$category」已有名為「$name」的資料夾"
            }
        }
        val created = JSONArray()
        for ((name, category, ids) in planned) {
            val newId = repository.insertDeck(Deck(name = name, category = category, description = "由 AI 從「${sourceDeck.name}」拆分"))
            repository.moveCards(ids, newId)
            created.put(JSONObject().put("deckId", newId).put("cardIds", JSONArray(ids)))
        }
        return AssistantPendingAction(
            id = UUID.randomUUID().toString(), type = "UNDO_SPLIT", deckId = sourceDeck.id,
            title = "復原資料夾拆分", description = "把卡片移回「${sourceDeck.name}」並刪除新分類",
            payload = JSONObject().put("sourceDeckId", sourceDeck.id).put("created", created).toString()
        )
    }

    private suspend fun executeAssistantMergeFolders(action: AssistantPendingAction): AssistantPendingAction {
        val payload = JSONObject(action.payload)
        val targetId = payload.getLong("targetDeckId")
        val sourceIds = payload.getJSONArray("sourceDeckIds").longValues().filter { it != targetId }.distinct()
        val target = allDecks.value.firstOrNull { it.id == targetId } ?: error("找不到合併目標")
        check((sourceIds + targetId).all { it in _assistantSelectedDeckIds.value }) { "部分資料夾不在可編輯範圍" }
        require(sourceIds.isNotEmpty()) { "沒有可合併的來源資料夾" }
        sourceIds.forEach { sourceId ->
            val source = allDecks.value.firstOrNull { it.id == sourceId } ?: return@forEach
            repository.moveCards(allCards.value.filter { it.deckId == sourceId }.map { it.id }, target.id)
            repository.deleteDeck(source)
        }
        return AssistantPendingAction(
            id = UUID.randomUUID().toString(), type = "NONE", deckId = target.id,
            title = "", description = "合併包含學習紀錄整合，無法完整復原", payload = ""
        )
    }

    private suspend fun executeAssistantWeaknessFolder(action: AssistantPendingAction): AssistantPendingAction {
        val payload = JSONObject(action.payload)
        val name = payload.getString("name").trim().take(60)
        val category = payload.getString("category").trim().take(40)
        require(name.isNotBlank() && category.isNotBlank()) { "資料夾名稱與課程不能空白" }
        check(allDecks.value.none { it.name.equals(name, true) && it.category.equals(category, true) }) { "已有同名弱點資料夾" }
        val weakFromLogs = recentStudyLogs.value.filter { it.rating <= 2 }.mapTo(hashSetOf()) { it.cardId }
        val requested = payload.getJSONArray("cardIds").longValues().toSet()
        val weakCards = allCards.value.filter { it.id in requested && (it.mistakeCount > 0 || it.id in weakFromLogs) }
            .distinctBy { it.normalizedWord }
        require(weakCards.isNotEmpty()) { "目前沒有符合條件的真實錯題" }
        val newId = repository.insertDeck(Deck(name = name, category = category, description = "依真實錯題與低評分紀錄建立"))
        val inserted = weakCards.map { repository.insertCard(it.copyForAssistantDeck(newId)) }
        return AssistantPendingAction(
            id = UUID.randomUUID().toString(), type = "UNDO_WEAKNESS_FOLDER", deckId = newId,
            title = "復原弱點資料夾", description = "刪除剛建立的「$name」與其中副本",
            payload = JSONObject().put("deckId", newId).put("cardIds", JSONArray(inserted)).toString()
        )
    }

    private suspend fun undoAssistantSplit(payload: JSONObject) {
        val sourceDeckId = payload.getLong("sourceDeckId")
        val created = payload.getJSONArray("created")
        for (index in 0 until created.length()) {
            val item = created.optJSONObject(index) ?: continue
            repository.moveCards(item.getJSONArray("cardIds").longValues(), sourceDeckId)
            allDecks.value.firstOrNull { it.id == item.getLong("deckId") }?.let { repository.deleteDeck(it) }
        }
    }

    private suspend fun undoAssistantCreatedFolder(payload: JSONObject) {
        payload.getJSONArray("cardIds").longValues().forEach { repository.deleteCardById(it) }
        allDecks.value.firstOrNull { it.id == payload.getLong("deckId") }?.let { repository.deleteDeck(it) }
    }

    private fun Flashcard.copyForAssistantDeck(targetDeckId: Long): Flashcard = copy(
        id = 0L, deckId = targetDeckId, normalizedWord = word.trim().lowercase(),
        isMastered = false, isSuspended = false, mistakeCount = 0,
        intervalDays = 0, easeFactor = 2.5f, repetitionCount = 0,
        nextReviewTimestamp = System.currentTimeMillis(), lastReviewedTimestamp = null,
        createdAt = System.currentTimeMillis(), sortOrder = System.currentTimeMillis()
    )

    private fun prepareAlphabeticalSort(deck: Deck, descending: Boolean) {
        val cards = allCards.value.filter { it.deckId == deck.id }
        if (cards.isEmpty()) {
            appendAssistantMessage("ASSISTANT", "「${deck.name}」目前沒有單字可以排序。")
            return
        }
        val sorted = if (descending) {
            cards.sortedByDescending { it.word.lowercase() }
        } else {
            cards.sortedBy { it.word.lowercase() }
        }
        val payload = JSONObject()
            .put("oldOrder", JSONArray(cards.map { it.id }))
            .put("newOrder", JSONArray(sorted.map { it.id }))
            .toString()
        _assistantPendingAction.value = AssistantPendingAction(
            id = UUID.randomUUID().toString(),
            type = "SORT",
            deckId = deck.id,
            title = "依英文${if (descending) " Z–A" else " A–Z"} 排序",
            description = "資料夾：${deck.name}\n影響 ${cards.size} 張卡片；確認後才會儲存。",
            payload = payload
        )
        appendAssistantMessage(
            "ASSISTANT",
            "已讀取「${deck.name}」的 ${cards.size} 張卡片，準備好排序，請先確認操作。",
            kind = "STATUS"
        )
    }

    private fun handleAssistantResult(result: JSONObject, fallbackDeck: Deck?) {
        val type = result.optString("type", "REPLY").uppercase()
        val message = formatAssistantDisplayText(result.optString("message")).ifBlank { "已完成分析。" }
        when (type) {
            "UPDATE_GRAMMAR" -> {
                val note = allGrammarNotes.value.firstOrNull { it.id == result.optLong("grammarId") }
                if (note == null) appendAssistantMessage("ASSISTANT", "找不到要編輯的文法筆記。")
                else {
                    val payload = JSONObject()
                        .put("course", result.optString("course", note.course))
                        .put("folder", result.optString("folder", note.folder))
                        .put("title", result.optString("title", note.title))
                        .put("category", result.optString("category", note.category))
                        .put("level", result.optString("level", note.level))
                        .put("summary", result.optString("summary", note.summary))
                        .put("tags", result.optString("tags", note.tags))
                    _assistantPendingAction.value = AssistantPendingAction(
                        id = UUID.randomUUID().toString(), type = "UPDATE_GRAMMAR", deckId = note.id,
                        title = "更新文法「${note.title}」",
                        description = "課程：${payload.optString("course")}\n資料夾：${payload.optString("folder")}\n標題：${payload.optString("title")}\n確認後才會套用。",
                        payload = payload.toString()
                    )
                    appendAssistantMessage("ASSISTANT", "文法修改內容已準備好，請先確認。", kind = "STATUS")
                }
            }
            "UPDATE_FOLDER" -> {
                val deck = allDecks.value.firstOrNull { it.id == result.optLong("deckId") } ?: fallbackDeck
                if (deck == null || deck.id !in _assistantSelectedDeckIds.value) {
                    appendAssistantMessage("ASSISTANT", "找不到可編輯的資料夾。")
                } else {
                    val name = result.optString("name", deck.name).trim().take(60).ifBlank { deck.name }
                    val category = result.optString("category", deck.category).trim().take(40).ifBlank { deck.category }
                    _assistantPendingAction.value = AssistantPendingAction(
                        id = UUID.randomUUID().toString(), type = "UPDATE_FOLDER", deckId = deck.id,
                        title = "更新資料夾「${deck.name}」",
                        description = "名稱：${deck.name} → $name\n課程：${deck.category} → $category",
                        payload = JSONObject().put("name", name).put("category", category).toString()
                    )
                    appendAssistantMessage("ASSISTANT", "修改內容已準備好，確認後才會套用。", kind = "STATUS")
                }
            }
            "TRANSFER_CARDS" -> prepareAssistantTransfer(result, message)
            "ADD_CARDS" -> prepareAssistantAddCards(result, message)
            "ENRICH_CARDS" -> prepareAssistantEnrichment(result, message)
            "SPLIT_FOLDER" -> prepareAssistantSplit(result, fallbackDeck, message)
            "MERGE_FOLDERS" -> prepareAssistantMerge(result, message)
            "CREATE_WEAKNESS_FOLDER" -> prepareAssistantWeaknessFolder(result, message)
            "UPDATE_DAILY_PLAN" -> prepareAssistantDailyPlan(result, message)
            "AUDIT_REPORT" -> appendAssistantMessage("ASSISTANT", buildVerifiedAuditReport(result), kind = "STATUS")
            "LEARNING_ANALYSIS" -> appendAssistantMessage("ASSISTANT", buildVerifiedLearningAnalysis(result), kind = "STATUS")
            "CREATE_FOLDER" -> {
                val name = result.optString("name").trim().replace("\n", " ").take(60)
                val category = result.optString("category")
                    .trim().replace("\n", " ").take(40)
                if (name.isBlank()) {
                    appendAssistantMessage("ASSISTANT", "請告訴我新資料夾的名稱。")
                } else if (category.isBlank()) {
                    val courses = allDecks.value.map { it.category }.filter { it.isNotBlank() }.distinct()
                    appendAssistantMessage(
                        "ASSISTANT",
                        buildString {
                            append("「$name」要放在哪個課程？")
                            if (courses.isNotEmpty()) append("\n目前課程：${courses.joinToString("、")}")
                        }
                    )
                } else if (allDecks.value.any {
                        it.name.equals(name, ignoreCase = true) &&
                            it.category.equals(category, ignoreCase = true)
                    }) {
                    appendAssistantMessage("ASSISTANT", "「$category」課程中已經有名為「$name」的資料夾，因此沒有重複建立。")
                } else {
                    val payload = JSONObject()
                        .put("name", name)
                        .put("category", category)
                        .put("description", result.optString("description").trim().take(200))
                        .put("colorHex", result.optString("colorHex", "#426B63"))
                    _assistantPendingAction.value = AssistantPendingAction(
                        id = UUID.randomUUID().toString(),
                        type = "CREATE_FOLDER",
                        deckId = 0L,
                        title = "建立資料夾「$name」",
                        description = "課程：$category\n確認後才會寫入單字庫。",
                        payload = payload.toString()
                    )
                    appendAssistantMessage("ASSISTANT", "資料夾資料已準備好，請確認是否建立。", kind = "STATUS")
                }
            }
            "SORT" -> {
                val deck = allDecks.value.firstOrNull { it.id == result.optLong("deckId") }
                    ?: fallbackDeck
                if (deck == null || deck.id !in _assistantSelectedDeckIds.value) {
                    appendAssistantMessage("ASSISTANT", "我找不到可編輯的目標資料夾，請在輸入框上方調整資料夾範圍。")
                }
                else prepareAlphabeticalSort(deck, result.optString("direction").equals("ZA", true))
            }
            "COUNTABILITY" -> {
                val deck = allDecks.value.firstOrNull { it.id == result.optLong("deckId") }
                    ?: fallbackDeck
                val source = result.optJSONArray("updates") ?: JSONArray()
                val deckCardIds = allCards.value.filter { it.deckId == deck?.id }.mapTo(hashSetOf()) { it.id }
                val currentById = allCards.value.associateBy { it.id }
                val validated = JSONArray()
                for (index in 0 until source.length()) {
                    val item = source.optJSONObject(index) ?: continue
                    val cardId = item.optLong("cardId")
                    val suggestedValue = item.optString("partOfSpeech").trim().take(80)
                    val current = currentById[cardId] ?: continue
                    val newValue = mergeCountabilityLabel(current.partOfSpeech, suggestedValue)
                        ?: continue
                    if (cardId !in deckCardIds) continue
                    validated.put(
                        JSONObject()
                            .put("cardId", cardId)
                            .put("word", current.word)
                            .put("oldPartOfSpeech", current.partOfSpeech)
                            .put("newPartOfSpeech", newValue)
                    )
                }
                if (deck == null || deck.id !in _assistantSelectedDeckIds.value || validated.length() == 0) {
                    appendAssistantMessage("ASSISTANT", "$message\n\n沒有找到可安全套用的詞性修改。")
                } else {
                    _assistantPendingAction.value = AssistantPendingAction(
                        id = UUID.randomUUID().toString(),
                        type = "COUNTABILITY",
                        deckId = deck.id,
                        title = "補充名詞可數性",
                        description = "資料夾：${deck.name}\n預計修改 ${validated.length()} 張卡片；原有詞性會保留並可復原。",
                        payload = validated.toString()
                    )
                    appendAssistantMessage("ASSISTANT", message, kind = "STATUS")
                }
            }
            "QUIZ" -> {
                val questions = result.optJSONArray("questions") ?: JSONArray()
                if (questions.length() == 0) appendAssistantMessage("ASSISTANT", message)
                else appendAssistantMessage(
                    "ASSISTANT",
                    message,
                    kind = "QUIZ",
                    payload = JSONObject()
                        .put("questions", questions)
                        .put("completed", false)
                        .put("attempts", JSONArray())
                        .toString()
                )
            }
            "ARTICLE" -> appendAssistantMessage(
                "ASSISTANT",
                formatAssistantDisplayText(result.optString("article")).ifBlank { message },
                kind = "ARTICLE",
                payload = result.toString()
            )
            "CONFUSABLES" -> appendAssistantMessage(
                "ASSISTANT",
                formatConfusableResult(result, fallbackDeck).ifBlank { message },
                kind = "CONFUSABLES"
            )
            else -> appendAssistantMessage("ASSISTANT", message)
        }
    }

    private fun buildAssistantPrompt(request: String, deck: Deck?, cards: List<Flashcard>): String {
        val recent = _assistantMessages.value.takeLast(6).joinToString("\n") {
            val quizResult = assistantQuizResultForPrompt(it)
            "${it.role}: ${it.content.take(500)}${quizResult?.let { result -> "\n$result" }.orEmpty()}"
        }
        val quizHistory = _assistantMessages.value
            .filter { it.kind == "QUIZ" }
            .takeLast(3)
            .mapNotNull(::assistantQuizResultForPrompt)
            .joinToString("\n")
        val deckList = JSONArray(allDecks.value.map {
            JSONObject().put("id", it.id).put("name", it.name).put("course", it.category)
        })
        val cardList = JSONArray(cards.map {
            JSONObject()
                .put("id", it.id)
                .put("deckId", it.deckId)
                .put("word", it.word)
                .put("partOfSpeech", it.partOfSpeech)
                .put("definition", it.definition)
                .put("exampleSentence", it.exampleSentence)
                .put("exampleTranslation", it.exampleTranslation)
                .put("mistakeCount", it.mistakeCount)
                .put("repetitionCount", it.repetitionCount)
                .put("isMastered", it.isMastered)
        })
        val grammarList = JSONArray(allGrammarNotes.value.take(120).map {
            JSONObject().put("id", it.id).put("course", it.course).put("folder", it.folder)
                .put("title", it.title).put("category", it.category).put("level", it.level)
                .put("summary", it.summary).put("tags", it.tags)
        })
        val logSummary = recentStudyLogs.value.groupBy { it.cardId }.entries
            .mapNotNull { (cardId, logs) ->
                allCards.value.firstOrNull { it.id == cardId }?.let { card ->
                    JSONObject().put("cardId", cardId).put("word", card.word)
                        .put("reviews", logs.size).put("lowRatings", logs.count { it.rating <= 2 })
                }
            }.sortedByDescending { it.optInt("lowRatings") }.take(40)
        val selectedIds = _assistantSelectedDeckIds.value
        val selectedDecks = allDecks.value.filter { it.id in selectedIds }
        val chatStyleInstruction = when (settings.value.aiChatStyle) {
            "RELAXED" -> """
                目前聊天風格是「輕鬆」。用自然、親切、像熟悉朋友般的繁體中文交談，可以適度幽默；
                使用者可以聊生活、興趣或其他日常話題，不必強行把內容拉回英文，但事實仍須可靠，不能捏造。
            """.trimIndent()
            "STRICT" -> """
                目前聊天風格是「嚴謹」。只回答英文學習、語言知識、單字卡、學習規劃與 Vocab App 操作相關內容；
                對無關的閒聊或其他領域要求要簡短、禮貌拒絕。回答前要審慎辨別不確定性，沒有可靠依據就明確說明，禁止猜測。
            """.trimIndent()
            else -> """
                目前聊天風格是「一般」。使用清楚、自然、友善的繁體中文回答，資訊不足時先詢問，避免武斷或過度冗長。
            """.trimIndent()
        }
        return """
            你是 Vocab App 內的繁體中文 AI 助手。一般英文學習與知識問題可以使用你的可靠知識回答；
            涉及使用者 App 內實際有哪些資料時，只能根據下方提供的真實資料，不可捏造，也不可聲稱已修改資料。
            $chatStyleInstruction
            App 會在使用者確認後執行寫入。整體只回傳一個 JSON 物件，不可加 Markdown 程式碼圍欄。
            message、article、analysis 與 explanation 等文字欄位可以使用精簡 Markdown：
            #～### 標題、**粗體**、- 條列、1. 編號、行內公式 $...$、獨立公式 $$...$$。
            物理、化學與數學符號優先使用標準 LaTeX（例如 \\alpha、\\sqrt{x}、\\frac{a}{b}），
            但不要為普通短句加入多餘格式，也不要輸出 HTML。

            type 只能是 REPLY、CREATE_FOLDER、UPDATE_FOLDER、UPDATE_GRAMMAR、TRANSFER_CARDS、ADD_CARDS、SPLIT_FOLDER、
            MERGE_FOLDERS、ENRICH_CARDS、AUDIT_REPORT、CREATE_WEAKNESS_FOLDER、UPDATE_DAILY_PLAN、
            LEARNING_ANALYSIS、SORT、COUNTABILITY、ARTICLE、CONFUSABLES、QUIZ。
            通用格式：{"type":"REPLY","message":"繁體中文回覆","deckId":0}
            使用者要求新增或建立資料夾時使用 CREATE_FOLDER，另加：
            {"name":"資料夾名稱","category":"課程名稱","description":"簡短說明，可空白","colorHex":"#426B63"}
            如果使用者和最近對話都沒有指定要放入哪個課程，category 必須回傳空字串；
            message 要詢問使用者要放在哪個課程，絕對不可自行選擇「通用」或猜測課程。
            不可聲稱已建立；App 會顯示確認卡並檢查同一課程內的重複名稱。
            UPDATE_FOLDER：{"type":"UPDATE_FOLDER","deckId":1,"name":"新名稱，未改則保留","category":"新課程，未改則保留"}
            UPDATE_GRAMMAR：{"type":"UPDATE_GRAMMAR","grammarId":1,"course":"課程","folder":"資料夾","title":"標題","category":"分類","level":"程度","summary":"摘要","tags":"標籤"}
            只有使用者明確要求修改既有文法時使用；grammarId 必須來自可讀取的文法資料，未要求改動的欄位必須原樣回傳。App 會先顯示確認卡。
            TRANSFER_CARDS：{"type":"TRANSFER_CARDS","mode":"MOVE或COPY","targetDeckId":2,"cardIds":[1,2]}
            ADD_CARDS：{"type":"ADD_CARDS","targetDeckId":2,"cards":[{"word":"","phonetic":"","partOfSpeech":"","definition":"","exampleSentence":"","exampleTranslation":""}]}
            外部推薦字必須是可靠英文知識，且不可聲稱原本存在 App。
            SPLIT_FOLDER：{"type":"SPLIT_FOLDER","sourceDeckId":1,"groups":[{"name":"分類名稱","category":"課程","cardIds":[1,2]}]}
            cardIds 只能使用提供的真實卡片，每張最多出現在一組；沒有足夠依據時先詢問使用者分類方式。
            MERGE_FOLDERS：{"type":"MERGE_FOLDERS","targetDeckId":1,"sourceDeckIds":[2,3]}
            ENRICH_CARDS：{"type":"ENRICH_CARDS","updates":[{"cardId":1,"phonetic":"","partOfSpeech":"","definition":"","exampleSentence":"","exampleTranslation":""}]}
            只能補空白欄位，內容必須可靠，不可改寫既有欄位。
            AUDIT_REPORT：另加 suspectedTypos：[ {"cardId":1,"suggestion":"正確拼字","reason":"理由"} ]；不確定就不要列為拼錯。
            CREATE_WEAKNESS_FOLDER：{"type":"CREATE_WEAKNESS_FOLDER","name":"弱點複習","category":"課程，未指定留空","cardIds":[真實低評分或錯題卡ID]}
            UPDATE_DAILY_PLAN：{"type":"UPDATE_DAILY_PLAN","dailyGoalCards":20,"dailyNewCardLimit":10,"dailyReviewLimit":50,"message":"依真實紀錄說明理由"}
            LEARNING_ANALYSIS：另加 analysis，根據真實學習摘要分析，不可捏造次數。
            SORT 另加 direction（AZ 或 ZA）。
            COUNTABILITY 另加 updates：[ {"cardId":1,"partOfSpeech":"n. [C]"} ]。保留原本非名詞詞性；不確定時不要輸出該卡。
            ARTICLE 必須提供自然完整、盡量使用相關單字的英文文章，並回傳：
            {"type":"ARTICLE","message":"簡短說明","article":"英文文章","articleTranslation":"完整繁體中文翻譯",
            "targetCardIds":[真實卡片ID],"sentenceTranslations":[{"sentence":"英文原句","translation":"繁體中文逐句翻譯"}],
            "questions":[{"prompt":"閱讀理解或克漏字題目","explanation":"繁體中文解析","cardId":1,"options":[{"text":"選項","correct":true}]}]}
            targetCardIds 與 cardId 只能使用提供的真實卡片 ID。預設出 5 題並混合閱讀理解與克漏字；每題恰好一個正確答案。
            CONFUSABLES 必須先從提供的真實 App 卡片選出 folderWord，再用你的英文知識找出外部的易混淆字；
            不可把外部補充字說成原本就在資料夾。另加 groups：
            [{"folderWord":"affect","relatedWords":[{"word":"effect","explanation":"兩字的詞性、意思與用法差異"}]}]
            folderWord 必須逐字對應提供的卡片；relatedWords 可以來自模型知識。另可提供簡短 message，但不要把 groups 再序列化到 analysis 字串。
            QUIZ 另加 questions，每題格式：
            {"prompt":"題目","explanation":"繁體中文解釋","options":[{"text":"選項","correct":true}]}
            預設產生 15 題，可依可用內容調整為 10～20 題；題目不可重複或只替換順序湊數。
            每題必須恰好一個 correct=true。涉及 App 內容時必須能由提供的卡片驗證；
            若使用模型知識補充的易混淆字，可針對真實英文用法出題，但不可聲稱外部單字已存在資料夾。

            AI 可以讀取下方提供的所有資料夾與卡片，也可以回答資料夾以外的一般英文知識。
            使用者允許 AI 寫入或修改的資料夾（這只是編輯權限，不是知識或讀取限制）：${JSONArray(selectedDecks.map { JSONObject().put("id", it.id).put("name", it.name) })}
            目前明確指定的資料夾：${deck?.let { "${it.name}（id=${it.id}）" } ?: "未指定或多選"}
            可用資料夾：$deckList
            可讀取的 App 卡片：$cardList
            可讀取及可在確認後編輯的文法筆記：$grammarList
            真實學習摘要（依低評分排序）：${JSONArray(logSummary)}
            測驗進度摘要（已壓縮，這是 App 保存的真實狀態）：
            ${quizHistory.ifBlank { "尚無測驗紀錄" }}
            最近對話：
            $recent

            使用者最新要求：$request
        """.trimIndent()
    }

    private fun resolveAssistantDeck(request: String): Deck? {
        val selectedIds = _assistantSelectedDeckIds.value
        val explicit = allDecks.value
            .filter {
                it.name.isNotBlank() && request.contains(it.name, ignoreCase = true)
            }
            .maxByOrNull { it.name.length }
        return explicit
            ?: selectedIds.singleOrNull()?.let { id -> allDecks.value.firstOrNull { it.id == id } }
            ?: assistantContextDeckId?.takeIf { it in selectedIds }
                ?.let { id -> allDecks.value.firstOrNull { it.id == id } }
    }

    private fun selectAssistantContextCards(request: String, contextDeck: Deck?): List<Flashcard> {
        val cards = allCards.value
        val onlyAsksQuizRecord = (request.contains("成績") || request.contains("錯題") || request.contains("做到哪")) &&
            (request.contains("測驗") || request.contains("剛剛"))
        if (onlyAsksQuizRecord) return emptyList()
        if (contextDeck != null) return cards.filter { it.deckId == contextDeck.id }.take(120)
        val editableIds = _assistantSelectedDeckIds.value
        val needsAppData = listOf(
            "資料夾", "單字庫", "卡片", "這些單字", "我的單字", "排序",
            "詞性", "易混淆", "相近", "文章", "測驗", "題目"
        ).any { request.contains(it, ignoreCase = true) }
        // A custom scope is an explicit request to let the assistant read those folders.  Do not
        // silently return an empty context merely because the wording did not contain a keyword.
        if (!needsAppData && editableIds.isEmpty()) return emptyList()
        val scopedCards = if (editableIds.isEmpty()) cards else cards.filter { it.deckId in editableIds }
        val terms = request.lowercase()
            .split(Regex("[^a-zA-Z\u4e00-\u9fff]+"))
            .filter { it.length >= 2 }
            .toSet()
        // Take a balanced sample so that one large folder cannot crowd every other selected
        // folder out of the prompt. This also keeps token use predictable.
        if (editableIds.size > 1 && terms.isEmpty()) {
            val perDeck = (80 / editableIds.size).coerceAtLeast(4)
            return editableIds.sorted().flatMap { deckId ->
                scopedCards.filter { it.deckId == deckId }.take(perDeck)
            }.take(80)
        }
        return scopedCards.sortedByDescending { card ->
            val searchable = "${card.word} ${card.definition} ${card.partOfSpeech}".lowercase()
            terms.count { it in searchable } * 10
        }.take(80)
    }

    private fun assistantDetailedStatus(
        request: String,
        contextDeck: Deck?,
        contextCardCount: Int,
        editableDeckCount: Int
    ): String = when {
        request.contains("測驗") || request.contains("題目") ->
            "正在整理 ${contextCardCount} 張相關卡片與測驗紀錄，準備產生約 15 題並檢查答案…"
        request.contains("混淆") || request.contains("相近") || request.contains("很像") ->
            "正在核對${contextDeck?.let { "「${it.name}」" } ?: "單字庫"}的 $contextCardCount 張卡片，並比對模型中的相近字與用法…"
        request.contains("文章") ->
            "正在挑選 $contextCardCount 個可用單字，規劃文章內容並檢查語意…"
        request.contains("排序") || request.contains("修改") || request.contains("新增") ->
            "正在驗證目標資料與編輯權限；目前允許修改 $editableDeckCount 個資料夾…"
        else -> "正在整理 $contextCardCount 張相關卡片並準備回覆…"
    }

    private fun formatAssistantDisplayText(value: String): String {
        val text = value.trim()
        if (text.isBlank()) return ""
        if (!text.startsWith("[") && !text.startsWith("{")) return text
        return runCatching {
            val items = if (text.startsWith("[")) JSONArray(text)
            else JSONArray().put(JSONObject(text))
            buildList {
                for (index in 0 until items.length()) {
                    val item = items.optJSONObject(index) ?: continue
                    val words = item.optJSONArray("words")?.let { array ->
                        buildList {
                            for (wordIndex in 0 until array.length()) {
                                array.optString(wordIndex).trim()
                                    .takeIf { it.isNotBlank() }
                                    ?.let(::add)
                            }
                        }
                    }.orEmpty()
                    val title = words.joinToString("／").ifBlank {
                        item.optString("title").trim()
                    }
                    val explanation = item.optString("explanation").trim()
                        .ifBlank { item.optString("analysis").trim() }
                    val readable = listOf(title, explanation)
                        .filter { it.isNotBlank() }
                        .joinToString("\n")
                    if (readable.isNotBlank()) add(readable)
                }
            }.joinToString("\n\n").ifBlank { text }
        }.getOrDefault(text)
    }

    private fun updateAssistantQuizPayload(messageId: String, update: (JSONObject) -> Unit) {
        val index = assistantAllMessages.indexOfFirst { it.id == messageId && it.kind == "QUIZ" }
        if (index < 0) return
        val message = assistantAllMessages[index]
        val root = runCatching { JSONObject(message.payload) }.getOrElse {
            val legacyQuestions = runCatching { JSONArray(message.payload) }.getOrDefault(JSONArray())
            JSONObject()
                .put("questions", legacyQuestions)
                .put("completed", false)
                .put("attempts", JSONArray())
        }
        update(root)
        assistantAllMessages[index] = message.copy(payload = root.toString())
        refreshAssistantMessages()
        persistAssistantState()
    }

    private fun assistantQuizResultForPrompt(message: AssistantMessage): String? {
        if (message.kind != "QUIZ" || message.payload.isBlank()) return null
        val root = runCatching { JSONObject(message.payload) }.getOrNull() ?: return null
        val questions = root.optJSONArray("questions") ?: return null
        val wrongAnswers = root.optJSONArray("wrongAnswers") ?: JSONArray()
        val wrongSummary = buildList {
            for (index in 0 until wrongAnswers.length()) {
                val wrong = wrongAnswers.optJSONObject(index) ?: continue
                val question = questions.optJSONObject(wrong.optInt("questionIndex")) ?: continue
                add(question.optString("prompt").replace("\n", " ").take(70))
            }
        }.take(6).joinToString("；")
        return if (root.optBoolean("completed")) {
            "測驗狀態：已完成，答對 ${root.optInt("latestCorrect")}/${root.optInt("latestTotal")} 題" +
                if (wrongSummary.isNotBlank()) "；錯題：$wrongSummary" else "。"
        } else {
            val current = root.optInt("currentIndex", 0).coerceAtMost(questions.length())
            "測驗狀態：進行中，已完成 $current/${questions.length()} 題，目前答對 ${root.optInt("correctCount", 0)} 題" +
                if (wrongSummary.isNotBlank()) "；目前錯題：$wrongSummary" else "。"
        }
    }

    private fun formatConfusableResult(result: JSONObject, targetDeck: Deck?): String {
        val groups = result.optJSONArray("groups") ?: JSONArray()
        if (groups.length() == 0) {
            return formatAssistantDisplayText(result.optString("analysis"))
        }
        val allCardSnapshot = allCards.value
        val sourceCards = targetDeck?.let { deck ->
            allCardSnapshot.filter { it.deckId == deck.id }
        } ?: allCardSnapshot
        return buildList {
            for (index in 0 until groups.length()) {
                val group = groups.optJSONObject(index) ?: continue
                val requestedSource = group.optString("folderWord").trim()
                val sourceCard = sourceCards.firstOrNull {
                    it.word.equals(requestedSource, ignoreCase = true)
                } ?: continue
                val related = group.optJSONArray("relatedWords") ?: JSONArray()
                val relatedLines = buildList {
                    for (relatedIndex in 0 until related.length()) {
                        val item = related.optJSONObject(relatedIndex) ?: continue
                        val word = item.optString("word").trim()
                        if (word.isBlank() || word.equals(sourceCard.word, ignoreCase = true)) continue
                        val existing = allCardSnapshot.firstOrNull {
                            it.word.equals(word, ignoreCase = true)
                        }
                        val sourceLabel = when {
                            existing?.deckId == sourceCard.deckId -> "資料夾內"
                            existing != null -> "App 其他資料夾"
                            else -> "AI 知識補充"
                        }
                        val explanation = item.optString("explanation").trim()
                        add(
                            buildString {
                                append("• $word（$sourceLabel）")
                                if (explanation.isNotBlank()) append("\n  $explanation")
                            }
                        )
                    }
                }
                if (relatedLines.isNotEmpty()) {
                    add(
                        buildString {
                            append("${sourceCard.word}（資料夾內單字）")
                            append("\n")
                            append(relatedLines.joinToString("\n"))
                        }
                    )
                }
            }
        }.joinToString("\n\n")
            .ifBlank { formatAssistantDisplayText(result.optString("analysis")) }
    }

    private fun isAssistantOperationRequest(request: String): Boolean {
        val operationWords = listOf(
            "排序", "排列", "修改", "加上", "補上", "補齊", "建立", "新增",
            "移動", "複製", "刪除", "清空", "文章", "測驗", "題目", "混淆"
        )
        return operationWords.any { request.contains(it, ignoreCase = true) }
    }

    private fun mergeCountabilityLabel(current: String, suggested: String): String? {
        val marker = Regex("\\[(?:C/U|U/C|C|U)]", RegexOption.IGNORE_CASE)
            .find(suggested)
            ?.value
            ?.uppercase()
            ?.replace("U/C", "C/U")
            ?: return null
        val nounToken = Regex(
            "(?<![A-Za-z])n\\.?\\s*(?:\\[(?:C/U|U/C|C|U)])?",
            RegexOption.IGNORE_CASE
        )
        val base = current.ifBlank { suggested.substringBefore("[").trim() }
        if (!nounToken.containsMatchIn(base)) return null
        return nounToken.replace(base) { match ->
            val noun = if (match.value.trimStart().startsWith("N")) "N." else "n."
            "$noun $marker"
        }.trim()
    }

    private fun appendAssistantMessage(
        role: String,
        content: String,
        kind: String = "TEXT",
        payload: String = "",
        attachmentUris: List<String> = emptyList()
    ): String {
        val conversationId = _assistantCurrentConversationId.value ?: return ""
        val now = System.currentTimeMillis()
        val messageId = UUID.randomUUID().toString()
        assistantAllMessages += AssistantMessage(
            id = messageId,
            conversationId = conversationId,
            role = role,
            content = content.trim(),
            createdAt = now,
            kind = kind,
            payload = payload,
            attachmentUris = attachmentUris.take(5)
        )
        _assistantConversations.value = _assistantConversations.value.map {
            if (it.id == conversationId) it.copy(updatedAt = now) else it
        }.sortedByDescending { it.updatedAt }
        refreshAssistantMessages()
        persistAssistantState()
        return messageId
    }

    private fun updateAssistantConversationTitle(conversationId: String, firstMessage: String) {
        _assistantConversations.value = _assistantConversations.value.map { conversation ->
            if (conversation.id == conversationId && conversation.title in setOf("新對話", "無痕對話")) {
                conversation.copy(title = firstMessage.replace("\n", " ").take(24))
            } else conversation
        }
        persistAssistantState()
    }

    private fun refreshAssistantMessages() {
        val currentId = _assistantCurrentConversationId.value
        val currentMessages = assistantAllMessages
            .filter { it.conversationId == currentId }
            .distinctBy { it.id }
            .sortedBy { it.createdAt }
        _assistantMessages.value = currentMessages
        val latestActionEvent = currentMessages.lastOrNull {
            it.kind == "ACTION" || it.kind == "ACTION_UNDONE"
        }
        _assistantUndoAction.value = latestActionEvent
            ?.takeIf { it.kind == "ACTION" }
            ?.payload
            ?.let(::assistantActionFromJson)
    }

    private fun persistAssistantState() {
        assistantChatStore.save(_assistantConversations.value, assistantAllMessages)
    }

    private fun JSONArray.longValues(): List<Long> = buildList {
        for (index in 0 until length()) add(optLong(index))
    }.filter { it > 0L }

    private fun AssistantPendingAction.toJson(): String = JSONObject()
        .put("id", id)
        .put("type", type)
        .put("deckId", deckId)
        .put("title", title)
        .put("description", description)
        .put("payload", payload)
        .toString()

    private fun assistantActionFromJson(value: String): AssistantPendingAction? = runCatching {
        val root = JSONObject(value)
        AssistantPendingAction(
            id = root.optString("id"),
            type = root.optString("type"),
            deckId = root.optLong("deckId"),
            title = root.optString("title"),
            description = root.optString("description"),
            payload = root.optString("payload")
        ).takeIf { it.id.isNotBlank() && it.type.isNotBlank() }
    }.getOrNull()

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }
}
