package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavController
import com.example.data.entity.Flashcard
import com.example.data.entity.GrammarNote
import com.example.data.entity.GrammarQuestion
import com.example.data.entity.GrammarDraft
import com.example.data.entity.GrammarQuizResult
import com.example.data.entity.GrammarLibrary
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.dp
import com.example.ui.screens.AddEditCardScreen
import com.example.ui.screens.CardListScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FlashcardReviewScreen
import com.example.ui.screens.FoldersManagementScreen
import com.example.ui.screens.MainScreen
import com.example.ui.screens.PhotoOcrScreen
import com.example.ui.screens.QuizGamesScreen
import com.example.ui.screens.StudyHubScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ExternalImportScreen
import com.example.ui.screens.OnboardingGoalScreen
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.StudyCalendarScreen
import com.example.ui.screens.VocabularyQualityScreen
import com.example.ui.screens.WordLookupScreen
import com.example.ui.screens.InteractiveReadingScreen
import com.example.ui.screens.AiGenerationLoadingScreen
import com.example.ui.screens.GrammarDashboardScreen
import com.example.ui.screens.GrammarNotesScreen
import com.example.ui.screens.GrammarStudyHubScreen
import com.example.ui.screens.GrammarDetailScreen
import com.example.ui.screens.GrammarEditorScreen
import com.example.ui.screens.GrammarLearnScreen
import com.example.ui.screens.GrammarQuizFlowScreen
import com.example.ui.screens.GrammarQuizResultScreen
import com.example.ui.screens.GrammarQuizHubScreen
import com.example.ui.screens.GrammarImportScreen
import com.example.ui.screens.GrammarImportPreviewScreen
import com.example.ui.screens.GrammarNoteDetailScreen
import com.example.ui.screens.GrammarWritingCheckScreen
import com.example.ui.screens.GrammarSearchScreen
import com.example.ui.screens.GrammarLibrariesScreen
import com.example.ui.screens.GrammarLibraryManageScreen
import com.example.ui.screens.ReadingLibraryScreen
import com.example.ui.screens.WritingLibraryScreen
import com.example.viewmodel.VocabularyViewModel
import com.example.util.GitHubUpdateManager
import com.example.util.UpdateCheckResult
import kotlinx.coroutines.launch

object Routes {
    const val MAIN = "main"
    const val REVIEW = "review"
    const val CARD_LIST = "card_list"
    const val ADD_EDIT_CARD = "add_edit_card"
    const val PHOTO_OCR = "photo_ocr"
    const val SETTINGS = "settings"
    const val EXTERNAL_IMPORT = "external_import"
    const val AI_ASSISTANT = "ai_assistant"
    const val STUDY_CALENDAR = "study_calendar"
    const val QUALITY_CHECK = "quality_check"
    const val WORD_LOOKUP = "word_lookup"
    const val GRAMMAR_SEARCH = "grammar_search"
    const val GENERATED_READING = "generated_reading"
    const val GRAMMAR_EDITOR = "grammar_editor"
    const val GRAMMAR_DETAIL = "grammar_detail"
    const val GRAMMAR_LEARN = "grammar_learn"
    const val GRAMMAR_QUIZ = "grammar_quiz"
    const val GRAMMAR_WRITING_CHECK = "grammar_writing_check"
    const val GRAMMAR_IMPORT = "grammar_import"
    const val GRAMMAR_IMPORT_PREVIEW = "grammar_import_preview"
    const val GRAMMAR_QUIZ_RESULT = "grammar_quiz_result"
    const val GRAMMAR_LIBRARY_MANAGE = "grammar_library_manage"
}

@Composable
fun VocabApp(
    viewModel: VocabularyViewModel = viewModel(),
    widgetAction: String? = null,
    onWidgetActionConsumed: () -> Unit = {},
    sharedText: String? = null,
    onSharedTextConsumed: () -> Unit = {}
) {
    val decks by viewModel.allDecks.collectAsStateWithLifecycle()
    val allCards by viewModel.allCards.collectAsStateWithLifecycle()
    val dueCards by viewModel.dueCards.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalCardCount.collectAsStateWithLifecycle()
    val masteredCount by viewModel.masteredCardCount.collectAsStateWithLifecycle()
    val dueCount by viewModel.dueCardCount.collectAsStateWithLifecycle()
    val logs by viewModel.recentStudyLogs.collectAsStateWithLifecycle()
    val selectedDeckId by viewModel.selectedDeckId.collectAsStateWithLifecycle()
    val ocrCandidates by viewModel.ocrCandidates.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val settingsLoaded by viewModel.settingsLoaded.collectAsStateWithLifecycle()
    val dataMessage by viewModel.dataMessage.collectAsStateWithLifecycle()
    val apiKeyConfigured by viewModel.apiKeyConfigured.collectAsStateWithLifecycle()
    val aiApiProfiles by viewModel.aiApiProfiles.collectAsStateWithLifecycle()
    val availableAiModels by viewModel.availableAiModels.collectAsStateWithLifecycle()
    val aiConnectionStatus by viewModel.aiConnectionStatus.collectAsStateWithLifecycle()
    val ttsVoices by viewModel.ttsVoices.collectAsStateWithLifecycle()
    val assistantConversations by viewModel.assistantConversations.collectAsStateWithLifecycle()
    val assistantMessages by viewModel.assistantMessages.collectAsStateWithLifecycle()
    val assistantCurrentConversationId by viewModel.assistantCurrentConversationId.collectAsStateWithLifecycle()
    val assistantBusy by viewModel.assistantBusy.collectAsStateWithLifecycle()
    val assistantStatus by viewModel.assistantStatus.collectAsStateWithLifecycle()
    val assistantSendOutcome by viewModel.assistantSendOutcome.collectAsStateWithLifecycle()
    val assistantPendingAction by viewModel.assistantPendingAction.collectAsStateWithLifecycle()
    val assistantUndoAction by viewModel.assistantUndoAction.collectAsStateWithLifecycle()
    val assistantSelectedDeckIds by viewModel.assistantSelectedDeckIds.collectAsStateWithLifecycle()
    val pronunciationWeakCardIds by viewModel.pronunciationWeakCardIds.collectAsStateWithLifecycle()
    val studyCheckInDates by viewModel.studyCheckInDates.collectAsStateWithLifecycle()
    val grammarNotes by viewModel.allGrammarNotes.collectAsStateWithLifecycle()
    val grammarQuestions by viewModel.allGrammarQuestions.collectAsStateWithLifecycle()
    val grammarPatterns by viewModel.allGrammarPatterns.collectAsStateWithLifecycle()
    val grammarExamples by viewModel.allGrammarExamples.collectAsStateWithLifecycle()
    val grammarWeaknesses by viewModel.grammarWeaknesses.collectAsStateWithLifecycle()
    val grammarWritingRecords by viewModel.grammarWritingRecords.collectAsStateWithLifecycle()
    val grammarLibraries by viewModel.grammarLibraries.collectAsStateWithLifecycle()

    var currentTab by remember { mutableIntStateOf(0) }
    var editingCard by remember { mutableStateOf<Flashcard?>(null) }
    var pendingRevealCardId by remember { mutableStateOf<Long?>(null) }
    var pendingSharedText by remember { mutableStateOf<String?>(null) }
    var pendingQuizDeckIds by remember { mutableStateOf<Set<Long>?>(null) }
    var pendingQuizTitle by remember { mutableStateOf("") }
    var reviewDeckIds by remember { mutableStateOf<Set<Long>?>(null) }
    var reviewScopeId by remember { mutableStateOf(0L) }
    var quizGameActive by remember { mutableStateOf(false) }
    var updateAvailable by remember { mutableStateOf(false) }
    var readingStartedAt by remember { mutableStateOf(0L) }
    var auditStartedAt by remember { mutableStateOf(0L) }
    val grammarMode = settings.grammarMode
    var selectedGrammarId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingGrammar by remember { mutableStateOf<GrammarNote?>(null) }
    var launchGrammarAi by remember { mutableStateOf(false) }
    var grammarImportDrafts by remember { mutableStateOf<List<GrammarDraft>>(emptyList()) }
    var pendingGrammarDraft by remember { mutableStateOf<GrammarDraft?>(null) }
    var grammarQuizResult by remember { mutableStateOf<GrammarQuizResult?>(null) }
    var grammarQuizFilterIds by remember { mutableStateOf<Set<Long>?>(null) }
    var showSwitchConfirmDialog by remember { mutableStateOf(false) }
    var targetGrammarMode by remember { mutableStateOf(false) }
    var customQuizTitle by remember { mutableStateOf("") }
    var customQuizQuestions by remember { mutableStateOf<List<GrammarQuestion>?>(null) }
    var generatingAiTopic by remember { mutableStateOf<String?>(null) }
    var selectedGrammarLibraryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingGrammarCourse by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingGrammarLibraryName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedArticleId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedWritingRecordId by rememberSaveable { mutableStateOf<Long?>(null) }
    val updateCheckScope = rememberCoroutineScope()
    val appContext = LocalContext.current.applicationContext

    fun performWorldSwitch(newMode: Boolean) {
        viewModel.updateBooleanSetting("grammarMode", newMode)
        currentTab = 0
        quizGameActive = false
        editingCard = null
        editingGrammar = null
        viewModel.stopSpeaking()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        updateCheckScope.launch {
            GitHubUpdateManager.cleanupInstalledDownloads(appContext)
            updateAvailable = GitHubUpdateManager.checkForUpdate() is UpdateCheckResult.Available
        }
    }

    if (!settingsLoaded) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (!settings.onboardingCompleted) {
        OnboardingGoalScreen(onComplete = viewModel::completeOnboarding)
        return
    }

    // Recreated after a full reset, so completing onboarding always starts at Home.
    val navController = rememberNavController()
    DisposableEffect(navController) {
        val destinationListener = NavController.OnDestinationChangedListener { _, _, _ ->
            viewModel.stopSpeaking()
        }
        navController.addOnDestinationChangedListener(destinationListener)
        onDispose {
            navController.removeOnDestinationChangedListener(destinationListener)
            viewModel.stopSpeaking()
        }
    }
    LaunchedEffect(widgetAction) {
        if (widgetAction == com.example.MainActivity.WIDGET_ACTION_REVIEW) {
            performWorldSwitch(false)
            viewModel.selectDeck(null)
            reviewDeckIds = null
            reviewScopeId = 0L
            navController.navigate(Routes.REVIEW) { launchSingleTop = true }
            onWidgetActionConsumed()
        }
    }
    LaunchedEffect(sharedText) {
        if (!sharedText.isNullOrBlank()) {
            performWorldSwitch(false)
            pendingSharedText = sharedText
            editingCard = null
            navController.navigate(Routes.ADD_EDIT_CARD) { launchSingleTop = true }
            onSharedTextConsumed()
        }
    }

    if (showSwitchConfirmDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showSwitchConfirmDialog = false },
            title = { androidx.compose.material3.Text("切換產品世界？") },
            text = { androidx.compose.material3.Text("您目前的編輯或測驗進度將會取消，確定要切換嗎？") },
            confirmButton = {
                androidx.compose.material3.Button(onClick = {
                    showSwitchConfirmDialog = false
                    performWorldSwitch(targetGrammarMode)
                }) {
                    androidx.compose.material3.Text("確定切換")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showSwitchConfirmDialog = false }) {
                    androidx.compose.material3.Text("取消")
                }
            }
        )
    }

    if (generatingAiTopic != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {},
            title = { androidx.compose.material3.Text("AI 正在生成特訓中…") },
            text = {
                androidx.compose.foundation.layout.Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator()
                    androidx.compose.material3.Text("正在為【$generatingAiTopic】產生專業文法概念與特訓題目，請稍候")
                }
            },
            confirmButton = {}
        )
    }

    NavHost(navController = navController, startDestination = Routes.MAIN) {
        composable(Routes.MAIN) {
            MainScreen(
                currentTab = currentTab,
                onTabSelected = {
                    if (it != currentTab) viewModel.stopSpeaking()
                    if (it != 3) quizGameActive = false
                    currentTab = it
                },
                showBottomNavigation = grammarMode || !quizGameActive,
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenWordSearch = { navController.navigate(if (grammarMode) Routes.GRAMMAR_SEARCH else Routes.WORD_LOOKUP) },
                grammarMode = grammarMode,
                onToggleGrammarMode = {
                    val nextMode = !grammarMode
                    if (editingCard != null || editingGrammar != null || quizGameActive) {
                        targetGrammarMode = nextMode
                        showSwitchConfirmDialog = true
                    } else {
                        performWorldSwitch(nextMode)
                    }
                },
                updateAvailable = updateAvailable,
                onOpenAssistant = {
                    viewModel.openAssistant(null)
                    navController.navigate(Routes.AI_ASSISTANT) { launchSingleTop = true }
                },
                dashboardContent = {
                    if (grammarMode) {
                        GrammarDashboardScreen(
                            notes = grammarNotes,
                            weaknesses = grammarWeaknesses,
                            onAdd = {
                                editingGrammar = null
                                launchGrammarAi = false
                                navController.navigate(Routes.GRAMMAR_EDITOR)
                            },
                            onImport = { navController.navigate(Routes.GRAMMAR_IMPORT) },
                            onOpen = { note ->
                                selectedGrammarId = note.id
                                navController.navigate(Routes.GRAMMAR_DETAIL)
                            },
                            onLearn = { note ->
                                selectedGrammarId = note.id
                                navController.navigate(Routes.GRAMMAR_LEARN)
                            },
                            onQuiz = { note ->
                                selectedGrammarId = note.id
                                grammarQuizFilterIds = null
                                navController.navigate(Routes.GRAMMAR_QUIZ)
                            },
                            onWritingCheck = {
                                currentTab = 4
                                navController.navigate(Routes.GRAMMAR_WRITING_CHECK)
                            },
                            onSeeAllNotes = { currentTab = 1 }
                        )
                    } else DashboardScreen(
                        decks = decks,
                        allCards = allCards,
                        dueCards = dueCards,
                        totalCount = totalCount,
                        masteredCount = masteredCount,
                        dueCount = dueCount,
                        logs = logs,
                        makeUpDates = studyCheckInDates,
                        dailyGoalCards = settings.dailyGoalCards,
                        selectedDeckId = selectedDeckId,
                        onSelectDeck = { id ->
                            viewModel.selectDeck(id)
                            reviewDeckIds = null
                            reviewScopeId = id ?: 0L
                        },
                        onAddFolder = viewModel::addFolder,
                        onStartReview = {
                            reviewDeckIds = null
                            reviewScopeId = selectedDeckId ?: 0L
                            navController.navigate(Routes.REVIEW)
                        },
                        onOpenAddCard = {
                            editingCard = null
                            navController.navigate(Routes.ADD_EDIT_CARD)
                        },
                        onOpenPhotoOcr = { navController.navigate(Routes.PHOTO_OCR) },
                        onOpenQuizGames = { deckId ->
                            pendingQuizDeckIds = if (deckId == null) {
                                decks.map { it.id }.toSet()
                            } else {
                                setOf(deckId)
                            }
                            pendingQuizTitle = if (deckId == null) "全站所有單字卡"
                            else decks.firstOrNull { it.id == deckId }?.name.orEmpty()
                            currentTab = 3
                        },
                        onOpenCardList = { navController.navigate(Routes.CARD_LIST) },
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                        updateAvailable = updateAvailable,
                        showTopBar = false,
                        onOpenExternalImport = { navController.navigate(Routes.EXTERNAL_IMPORT) },
                        onStartInteractiveReading = { deckId ->
                            val deckName = decks.firstOrNull { it.id == deckId }?.name ?: "指定資料夾"
                            readingStartedAt = System.currentTimeMillis()
                            viewModel.openAssistant(deckId)
                            navController.navigate(Routes.GENERATED_READING) { launchSingleTop = true }
                            viewModel.sendAssistantMessage(
                                "請根據資料夾「$deckName」產生一篇自然的互動閱讀文章，盡量使用資料夾內的目標單字，並附完整繁體中文翻譯、逐句翻譯與 5 題閱讀測驗。"
                            )
                        },
                        onOpenStudyCalendar = { navController.navigate(Routes.STUDY_CALENDAR) },
                        onOpenQualityCheck = { navController.navigate(Routes.QUALITY_CHECK) },
                        onOpenAssistant = {
                            viewModel.openAssistant(selectedDeckId)
                            navController.navigate(Routes.AI_ASSISTANT) { launchSingleTop = true }
                        }
                    )
                },
                foldersContent = {
                    if (grammarMode) {
                        GrammarLibrariesScreen(
                            libraries = grammarLibraries,
                            notes = grammarNotes,
                            sharedCourses = decks.map { it.category },
                            highlightedAction = "manage",
                            onAdd = viewModel::addGrammarLibrary,
                            onUpdate = viewModel::updateGrammarLibrary,
                            onDelete = viewModel::deleteGrammarLibrary,
                            onReorder = viewModel::reorderGrammarLibraries,
                            onManage = { library -> selectedGrammarLibraryId = library.id; navController.navigate(Routes.GRAMMAR_LIBRARY_MANAGE) },
                            onLearn = { library -> grammarNotes.firstOrNull { it.course == library.course && it.folder == library.name }?.let { selectedGrammarId = it.id; navController.navigate(Routes.GRAMMAR_LEARN) } },
                            onQuiz = { library ->
                                val ids = grammarNotes.filter { it.course == library.course && it.folder == library.name }.map { it.id }.toSet()
                                customQuizTitle = library.name
                                customQuizQuestions = grammarQuestions.filter { it.grammarNoteId in ids }
                                selectedGrammarId = ids.firstOrNull()
                                navController.navigate(Routes.GRAMMAR_QUIZ)
                            }
                        )
                    } else FoldersManagementScreen(
                        decks = decks,
                        allCards = allCards,
                        onAddFolder = viewModel::addFolder,
                        onUpdateFolder = viewModel::updateDeck,
                        onDeleteFolder = viewModel::deleteDeck,
                        onMoveFolder = viewModel::moveDeck,
                        onMoveFolderGlobally = viewModel::moveDeckGlobally,
                        onReorderFolders = viewModel::reorderDecks,
                        onRenameCourse = viewModel::renameCourse,
                        onOpenCardList = { deckId ->
                            viewModel.selectDeck(deckId)
                            navController.navigate(Routes.CARD_LIST)
                        },
                        onJumpToStudy = { deckId ->
                            viewModel.selectDeck(deckId)
                            reviewDeckIds = null
                            reviewScopeId = deckId
                            navController.navigate(Routes.REVIEW)
                        },
                        onJumpToQuiz = { deckId ->
                            viewModel.selectDeck(deckId)
                            pendingQuizDeckIds = setOf(deckId)
                            pendingQuizTitle = decks.firstOrNull { it.id == deckId }?.name.orEmpty()
                            currentTab = 3
                        },
                        onOpenAssistant = {
                            viewModel.openAssistant(null)
                            navController.navigate(Routes.AI_ASSISTANT) { launchSingleTop = true }
                        },
                        showTopBar = false
                    )
                },
                studyContent = {
                    if (grammarMode) {
                        GrammarLibrariesScreen(
                            libraries = grammarLibraries,
                            notes = grammarNotes,
                            sharedCourses = decks.map { it.category },
                            highlightedAction = "learn",
                            onAdd = viewModel::addGrammarLibrary,
                            onUpdate = viewModel::updateGrammarLibrary,
                            onDelete = viewModel::deleteGrammarLibrary,
                            onReorder = viewModel::reorderGrammarLibraries,
                            onManage = { library -> selectedGrammarLibraryId = library.id; navController.navigate(Routes.GRAMMAR_LIBRARY_MANAGE) },
                            onLearn = { library -> grammarNotes.firstOrNull { it.course == library.course && it.folder == library.name }?.let { selectedGrammarId = it.id; navController.navigate(Routes.GRAMMAR_LEARN) } },
                            onQuiz = { library ->
                                val ids = grammarNotes.filter { it.course == library.course && it.folder == library.name }.map { it.id }.toSet()
                                customQuizTitle = library.name
                                customQuizQuestions = grammarQuestions.filter { it.grammarNoteId in ids }
                                selectedGrammarId = ids.firstOrNull()
                                navController.navigate(Routes.GRAMMAR_QUIZ)
                            }
                        )
                    } else StudyHubScreen(
                        decks = decks,
                        allCards = allCards,
                        onStartReviewForDeck = { deckId ->
                            viewModel.selectDeck(deckId)
                            reviewDeckIds = null
                            reviewScopeId = deckId
                            navController.navigate(Routes.REVIEW)
                        },
                        onStartReviewForCourse = { course ->
                            val ids = decks.filter { it.category == course }.map { it.id }.toSet()
                            viewModel.selectDeck(null)
                            reviewDeckIds = ids
                            reviewScopeId = -(course.hashCode().toLong().let { kotlin.math.abs(it) } + 1L)
                            navController.navigate(Routes.REVIEW)
                        },
                        onStartReviewForDecks = { ids ->
                            viewModel.selectDeck(null)
                            reviewDeckIds = ids
                            reviewScopeId = -(ids.sorted().joinToString(",").hashCode().toLong().let { kotlin.math.abs(it) } + 1L)
                            navController.navigate(Routes.REVIEW)
                        },
                        onManageDeck = { deckId ->
                            viewModel.selectDeck(deckId)
                            navController.navigate(Routes.CARD_LIST)
                        },
                        onQuizDeck = { deckId ->
                            viewModel.selectDeck(deckId)
                            pendingQuizDeckIds = setOf(deckId)
                            pendingQuizTitle = decks.firstOrNull { it.id == deckId }?.name.orEmpty()
                            currentTab = 3
                        },
                        onOpenAssistant = {
                            viewModel.openAssistant(null)
                            navController.navigate(Routes.AI_ASSISTANT) { launchSingleTop = true }
                        },
                        showTopBar = false
                    )
                },
                quizContent = {
                    if (grammarMode) {
                        GrammarLibrariesScreen(
                            libraries = grammarLibraries,
                            notes = grammarNotes,
                            sharedCourses = decks.map { it.category },
                            highlightedAction = "quiz",
                            onAdd = viewModel::addGrammarLibrary,
                            onUpdate = viewModel::updateGrammarLibrary,
                            onDelete = viewModel::deleteGrammarLibrary,
                            onReorder = viewModel::reorderGrammarLibraries,
                            onManage = { library -> selectedGrammarLibraryId = library.id; navController.navigate(Routes.GRAMMAR_LIBRARY_MANAGE) },
                            onLearn = { library -> grammarNotes.firstOrNull { it.course == library.course && it.folder == library.name }?.let { selectedGrammarId = it.id; navController.navigate(Routes.GRAMMAR_LEARN) } },
                            onQuiz = { library ->
                                val ids = grammarNotes.filter { it.course == library.course && it.folder == library.name }.map { it.id }.toSet()
                                customQuizTitle = library.name
                                customQuizQuestions = grammarQuestions.filter { it.grammarNoteId in ids }
                                selectedGrammarId = ids.firstOrNull()
                                navController.navigate(Routes.GRAMMAR_QUIZ)
                            }
                        )
                    } else QuizGamesScreen(
                        cards = allCards,
                        decks = decks,
                        launchDeckIds = pendingQuizDeckIds,
                        launchTitle = pendingQuizTitle,
                        onLaunchConsumed = { pendingQuizDeckIds = null },
                        onOpenDeckManagement = { deckId ->
                            viewModel.selectDeck(deckId)
                            navController.navigate(Routes.CARD_LIST)
                        },
                        onJumpToStudy = { deckId ->
                            if (deckId == null) {
                                currentTab = 2
                            } else {
                                viewModel.selectDeck(deckId)
                                navController.navigate(Routes.REVIEW)
                            }
                        },
                        onSpeak = viewModel::speakText,
                        onStopSpeaking = viewModel::stopSpeaking,
                        onWrongAnswer = { card ->
                            if (settings.gameMistakesToReview) viewModel.recordGameMistake(card)
                        },
                        onPronunciationResult = viewModel::recordPronunciationResult,
                        onGameActiveChange = { quizGameActive = it },
                        onOpenAssistant = {
                            viewModel.openAssistant(null)
                            navController.navigate(Routes.AI_ASSISTANT) { launchSingleTop = true }
                        },
                        showHubTopBar = false
                    )
                },
                libraryToolContent = {
                    if (grammarMode) {
                        WritingLibraryScreen(
                            records = grammarWritingRecords,
                            onNew = { selectedWritingRecordId = null; navController.navigate(Routes.GRAMMAR_WRITING_CHECK) },
                            onImport = { selectedWritingRecordId = null; navController.navigate(Routes.GRAMMAR_WRITING_CHECK) },
                            onOpen = { record -> selectedWritingRecordId = record.id; navController.navigate(Routes.GRAMMAR_WRITING_CHECK) }
                        )
                    } else {
                        val articles = assistantMessages.filter { it.role == "ASSISTANT" && it.kind == "ARTICLE" }.sortedByDescending { it.createdAt }
                        ReadingLibraryScreen(
                            decks = decks,
                            articleMessages = articles,
                            onGenerate = { deck ->
                                selectedArticleId = null
                                readingStartedAt = System.currentTimeMillis()
                                viewModel.openAssistant(deck.id)
                                navController.navigate(Routes.GENERATED_READING)
                                viewModel.sendAssistantMessage("請根據資料夾「${deck.name}」產生一篇自然的互動閱讀文章，優先使用待複習與不熟悉的目標單字，附繁體中文逐句翻譯與 5 題閱讀理解。")
                            },
                            onImport = { source ->
                                selectedArticleId = null
                                readingStartedAt = System.currentTimeMillis()
                                viewModel.openAssistant(null)
                                navController.navigate(Routes.GENERATED_READING)
                                viewModel.sendAssistantMessage("請保留以下英文文章原文，只整理標題、段落、繁體中文逐句翻譯與 5 題閱讀理解；不要改寫原文。若內容是公開網址，請明確指出無法直接讀取時需要使用者貼上內文。\n\n$source")
                            },
                            onOpenArticle = { message -> selectedArticleId = message.id; navController.navigate(Routes.GENERATED_READING) }
                        )
                    }
                }
            )
        }

        composable(Routes.REVIEW) {
            val scopedDeckIds = reviewDeckIds
            val deckFilteredDue = when {
                scopedDeckIds != null -> dueCards.filter { it.deckId in scopedDeckIds }
                selectedDeckId != null -> dueCards.filter { it.deckId == selectedDeckId }
                else -> dueCards
            }
            val deckFilteredAll = when {
                scopedDeckIds != null -> allCards.filter { it.deckId in scopedDeckIds }
                selectedDeckId != null -> allCards.filter { it.deckId == selectedDeckId }
                else -> allCards
            }
            val reviewPool = if (scopedDeckIds != null || selectedDeckId != null) {
                deckFilteredAll
            } else {
                deckFilteredDue.ifEmpty { deckFilteredAll }
            }
            val cardsToReview = reviewPool.distinctBy { it.id }
            FlashcardReviewScreen(
                cards = cardsToReview,
                sessionScopeId = reviewScopeId,
                advancedSrs = settings.advancedSrs,
                autoSpeak = settings.autoSpeak,
                onRecordReview = viewModel::submitCardReview,
                onUndoReview = viewModel::undoCardReview,
                onToggleFavorite = viewModel::toggleFavorite,
                onSpeak = viewModel::speakText,
                onSpeakLearningCardFront = viewModel::speakLearningCardFront,
                onSpeakLearningCard = viewModel::speakLearningCard,
                onStopSpeaking = viewModel::stopSpeaking,
                onBackToDashboard = {
                    currentTab = 2
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.MAIN) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onOpenQuizGames = {
                    pendingQuizDeckIds = reviewDeckIds
                        ?: selectedDeckId?.let(::setOf)
                        ?: decks.map { it.id }.toSet()
                    pendingQuizTitle = when {
                        reviewDeckIds != null -> "目前課程全部資料夾"
                        selectedDeckId != null -> decks.firstOrNull { it.id == selectedDeckId }?.name.orEmpty()
                        else -> "全站所有單字卡"
                    }
                    currentTab = 3
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.MAIN) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.WORD_LOOKUP) {
            WordLookupScreen(
                decks = decks,
                cards = allCards,
                onBack = { navController.popBackStack() },
                onLookup = viewModel::lookupDictionary,
                onAiComplete = viewModel::fetchAiWordDetails,
                onOpenLocalCard = { card ->
                    viewModel.selectDeck(card.deckId)
                    pendingRevealCardId = card.id
                    navController.navigate(Routes.CARD_LIST) { launchSingleTop = true }
                },
                onAddToDeck = viewModel::addDictionaryEntryToDeck,
                onCreateFolderAndAdd = viewModel::createFolderAndAddDictionaryEntry
            )
        }

        composable(Routes.GRAMMAR_SEARCH) {
            GrammarSearchScreen(
                notes = grammarNotes,
                onOpen = { note -> selectedGrammarId = note.id; navController.navigate(Routes.GRAMMAR_DETAIL) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.AI_ASSISTANT) {
            AiAssistantScreen(
                conversations = assistantConversations,
                currentConversationId = assistantCurrentConversationId,
                messages = assistantMessages,
                busy = assistantBusy,
                status = assistantStatus,
                sendOutcome = assistantSendOutcome,
                pendingAction = assistantPendingAction,
                undoAction = assistantUndoAction,
                apiConfigured = apiKeyConfigured,
                contextDeckName = assistantSelectedDeckIds.singleOrNull()
                    ?.let { id -> decks.firstOrNull { it.id == id }?.name },
                decks = decks,
                cards = allCards,
                selectedDeckIds = assistantSelectedDeckIds,
                onScopeChange = viewModel::setAssistantScope,
                onSend = viewModel::sendAssistantMessage,
                onStop = viewModel::stopAssistantRequest,
                onQuizCompleted = viewModel::completeAssistantQuiz,
                onQuizRestart = viewModel::restartAssistantQuiz,
                onQuizProgress = viewModel::saveAssistantQuizProgress,
                onSpeak = viewModel::speakText,
                onStopSpeaking = viewModel::stopSpeaking,
                onPronunciationResult = viewModel::recordPronunciationResult,
                onReadingMistake = viewModel::recordReadingMistake,
                onCopyReadingCard = viewModel::copyCardToDeck,
                onCreateFolderAndCopyReadingCard = viewModel::createFolderAndCopyCard,
                onConfirmAction = viewModel::confirmAssistantAction,
                onCancelAction = viewModel::cancelAssistantAction,
                onUndoAction = viewModel::undoAssistantAction,
                onNewConversation = viewModel::newAssistantConversation,
                onSelectConversation = viewModel::selectAssistantConversation,
                onDeleteConversation = viewModel::deleteAssistantConversation,
                onClearChats = viewModel::clearAssistantChats,
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.GENERATED_READING) {
            val article = selectedArticleId?.let { id -> assistantMessages.firstOrNull { it.id == id } }
                ?: assistantMessages.lastOrNull {
                    it.role == "ASSISTANT" && it.kind == "ARTICLE" && it.createdAt >= readingStartedAt
                }
            if (article == null) {
                val failure = assistantMessages.lastOrNull {
                    it.role == "ASSISTANT" && it.createdAt >= readingStartedAt && it.kind != "ARTICLE"
                }?.content?.takeIf { !assistantBusy }
                AiGenerationLoadingScreen(
                    title = "文章閱讀",
                    status = assistantStatus ?: "AI 正在根據資料夾產生文章…",
                    error = failure,
                    onBack = { viewModel.stopAssistantRequest(); navController.popBackStack() }
                )
            } else {
                InteractiveReadingScreen(
                    message = article,
                    cards = allCards,
                    decks = decks,
                    onSpeak = viewModel::speakText,
                    onStopSpeaking = viewModel::stopSpeaking,
                    onPronunciationResult = viewModel::recordPronunciationResult,
                    onReadingMistake = viewModel::recordReadingMistake,
                    onCopyCardToDeck = viewModel::copyCardToDeck,
                    onCreateFolderAndCopyCard = viewModel::createFolderAndCopyCard,
                    onBack = { selectedArticleId = null; navController.popBackStack() }
                )
            }
        }

        composable(Routes.GRAMMAR_LIBRARY_MANAGE) {
            val library = grammarLibraries.firstOrNull { it.id == selectedGrammarLibraryId }
            if (library == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                val libraryNotes = grammarNotes.filter { it.course == library.course && it.folder == library.name }
                GrammarLibraryManageScreen(
                    library = library,
                    notes = libraryNotes,
                    onEdit = { note ->
                        editingGrammar = note
                        pendingGrammarCourse = library.course
                        pendingGrammarLibraryName = library.name
                        navController.navigate(Routes.GRAMMAR_EDITOR)
                    },
                    onDelete = { note -> viewModel.deleteGrammarNote(note) },
                    onReorder = viewModel::reorderGrammarNotes,
                    onAdd = {
                        editingGrammar = null
                        pendingGrammarCourse = library.course
                        pendingGrammarLibraryName = library.name
                        launchGrammarAi = false
                        navController.navigate(Routes.GRAMMAR_EDITOR)
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(Routes.STUDY_CALENDAR) {
            StudyCalendarScreen(
                logs = logs,
                dailyGoalCards = settings.dailyGoalCards,
                makeUpDates = studyCheckInDates,
                onToggleMakeUp = viewModel::toggleStudyCheckIn,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.QUALITY_CHECK) {
            VocabularyQualityScreen(
                cards = allCards,
                decks = decks,
                onEditCard = { card ->
                    editingCard = card
                    navController.navigate(Routes.ADD_EDIT_CARD)
                },
                onRunAiAudit = {
                    auditStartedAt = System.currentTimeMillis()
                    viewModel.openAssistant(null)
                    viewModel.sendAssistantMessage(
                        "請對我的單字庫執行完整資料品質檢查，特別檢查疑似拼字錯誤與中文解釋是否可疑；只回報有可靠依據的問題，不確定時請明確標示並不要直接修改。"
                    )
                },
                aiRunning = assistantBusy && auditStartedAt > 0,
                aiStatus = assistantStatus,
                aiReport = if (auditStartedAt > 0) assistantMessages.lastOrNull {
                    it.role == "ASSISTANT" && it.createdAt >= auditStartedAt
                }?.content else null,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.CARD_LIST) {
            CardListScreen(
                cards = allCards,
                decks = decks,
                selectedDeckId = selectedDeckId,
                revealCardId = pendingRevealCardId,
                onRevealHandled = { pendingRevealCardId = null },
                onSelectDeck = { id ->
                    viewModel.selectDeck(id)
                    reviewDeckIds = null
                    reviewScopeId = id ?: 0L
                },
                onToggleFavorite = viewModel::toggleFavorite,
                onDeleteCard = viewModel::deleteCard,
                onDeleteCards = viewModel::deleteCards,
                onMoveCard = viewModel::moveCard,
                onReorderCards = viewModel::reorderCards,
                onMoveCards = viewModel::moveCards,
                onCreateFolderAndMoveCards = viewModel::createFolderAndMoveCards,
                onEditCard = { card ->
                    editingCard = card
                    pendingRevealCardId = card.id
                    navController.navigate(Routes.ADD_EDIT_CARD)
                },
                onSpeak = viewModel::speakText,
                onStopSpeaking = viewModel::stopSpeaking,
                onAddNewCard = {
                    editingCard = null
                    navController.navigate(Routes.ADD_EDIT_CARD)
                },
                onOpenExternalImport = {
                    navController.navigate(Routes.EXTERNAL_IMPORT)
                },
                onOpenScanner = {
                    navController.navigate(Routes.PHOTO_OCR)
                },
                onStartReview = { scopeIds ->
                    reviewDeckIds = scopeIds?.takeIf { it.size != 1 }
                    val onlyDeckId = scopeIds?.singleOrNull()
                    if (onlyDeckId != null) viewModel.selectDeck(onlyDeckId)
                    reviewScopeId = when {
                        onlyDeckId != null -> onlyDeckId
                        scopeIds != null -> -(scopeIds.sorted().joinToString(",").hashCode().toLong().let { kotlin.math.abs(it) } + 1L)
                        else -> 0L
                    }
                    navController.navigate(Routes.REVIEW)
                },
                onOpenQuizGames = { scopeIds ->
                    navController.popBackStack()
                    pendingQuizDeckIds = scopeIds ?: decks.map { it.id }.toSet()
                    pendingQuizTitle = when {
                        scopeIds?.size == 1 -> decks.firstOrNull { it.id == scopeIds.first() }?.name.orEmpty()
                        scopeIds != null -> "目前課程全部資料夾"
                        else -> "全站所有單字卡"
                    }
                    currentTab = 3
                },
                onOpenAssistant = {
                    viewModel.openAssistant(selectedDeckId)
                    navController.navigate(Routes.AI_ASSISTANT) { launchSingleTop = true }
                },
                pronunciationWeakCardIds = pronunciationWeakCardIds,
                onPronunciationResult = viewModel::recordPronunciationResult,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ADD_EDIT_CARD) {
            AddEditCardScreen(
                editingCard = editingCard,
                initialFocusCardId = editingCard?.id,
                initialSharedText = pendingSharedText,
                allCards = allCards,
                decks = decks,
                onGetDictionaryMatch = viewModel::getDictionaryMatch,
                onFetchAiWordDetails = viewModel::fetchAiWordDetails,
                onBatchFetchAiWordDetails = viewModel::fetchAiWordDetailsBatch,
                onAddFolder = viewModel::addFolder,
                onSaveBatchCards = viewModel::saveBatchCards,
                onBack = {
                    pendingSharedText = null
                    navController.popBackStack()
                },
                aiRequiresConfirmation = settings.aiRequiresConfirmation
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                settings = settings,
                onBooleanChange = viewModel::updateBooleanSetting,
                onIntChange = viewModel::updateIntSetting,
                onReminderTimesChange = viewModel::updateReminderTimes,
                onThemeChange = viewModel::updateThemeMode,
                onThemeColorPresetChange = viewModel::updateThemeColorPreset,
                onCustomThemeColorsChange = viewModel::updateCustomThemeColors,
                onGradientColorsChange = viewModel::updateGradientColors,
                onCustomTextColorChange = viewModel::updateCustomTextColor,
                onBackgroundAppearanceChange = viewModel::updateBackgroundAppearance,
                onFontAppearanceChange = viewModel::updateFontAppearance,
                onSpeechRateChange = viewModel::updateSpeechRate,
                onDictionarySourceChange = viewModel::updateDictionarySource,
                ttsVoices = ttsVoices,
                onTtsVoiceStyleChange = viewModel::updateTtsVoiceStyle,
                onTtsVoiceNameChange = viewModel::updateTtsVoiceName,
                onPreviewTtsVoice = viewModel::previewTtsVoice,
                dataMessage = dataMessage,
                apiKeyConfigured = apiKeyConfigured,
                aiApiProfiles = aiApiProfiles,
                availableAiModels = availableAiModels,
                aiConnectionStatus = aiConnectionStatus,
                onAiProviderChange = viewModel::updateAiProvider,
                onAiModelChange = viewModel::updateAiModel,
                onAiChatStyleChange = viewModel::updateAiChatStyle,
                onAiPromptChange = viewModel::updateAiWordPrompt,
                onResetAiPrompt = viewModel::resetAiWordPrompt,
                onAiImagePromptChange = viewModel::updateAiImagePrompt,
                onResetAiImagePrompt = viewModel::resetAiImagePrompt,
                onSaveApiKey = viewModel::savePersonalApiKey,
                onClearApiKey = viewModel::clearPersonalApiKey,
                onRefreshAiModels = viewModel::refreshAiModels,
                onTestAiConnection = viewModel::testAiConnection,
                onExportBackup = viewModel::exportBackup,
                onExportCsv = viewModel::exportCsv,
                onImportBackup = viewModel::importBackup,
                onClearAllData = viewModel::clearAllData,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.EXTERNAL_IMPORT) {
            ExternalImportScreen(
                decks = decks,
                initialDeckId = selectedDeckId,
                onImportCards = viewModel::importExternalCards,
                aiAvailable = settings.aiEnabled &&
                    settings.usePersonalAiApi &&
                    apiKeyConfigured,
                onFormatCardsWithAi = viewModel::formatExternalCardsWithAi,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PHOTO_OCR) {
            PhotoOcrScreen(
                decks = decks,
                candidates = ocrCandidates,
                initialDeckId = selectedDeckId,
                aiAvailable = settings.aiEnabled &&
                    settings.usePersonalAiApi &&
                    apiKeyConfigured,
                onImportCandidates = { candidates, deckId ->
                    viewModel.importOcrCandidates(candidates, deckId)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
                pdfPageLimit = settings.pdfPageLimit
            )
        }

        composable(Routes.GRAMMAR_IMPORT) {
            GrammarImportScreen(
                onBack = { navController.popBackStack() },
                onManual = {
                    editingGrammar = null
                    launchGrammarAi = false
                    navController.navigate(Routes.GRAMMAR_EDITOR)
                },
                onAiCreate = {
                    editingGrammar = null
                    launchGrammarAi = true
                    navController.navigate(Routes.GRAMMAR_EDITOR)
                },
                onRecognize = viewModel::generateGrammarDraftsFromSources,
                onRecognizeText = viewModel::generateGrammarDraftFromText,
                onImportShare = viewModel::readGrammarShare,
                onPreview = { drafts ->
                    grammarImportDrafts = drafts
                    if (drafts.size == 1) {
                        editingGrammar = null
                        pendingGrammarDraft = drafts.first()
                        navController.navigate(Routes.GRAMMAR_EDITOR)
                    } else navController.navigate(Routes.GRAMMAR_IMPORT_PREVIEW)
                }
            )
        }

        composable(Routes.GRAMMAR_IMPORT_PREVIEW) {
            GrammarImportPreviewScreen(
                initialDrafts = grammarImportDrafts,
                courses = (listOf("通用") + decks.map { it.category })
                    .filter(String::isNotBlank).distinct(),
                onBack = { navController.popBackStack() },
                onSave = { drafts, course, duplicateMode ->
                    viewModel.saveGrammarDrafts(drafts, course, duplicateMode) { ids ->
                        grammarImportDrafts = emptyList()
                        selectedGrammarId = ids.firstOrNull()
                        if (selectedGrammarId != null) navController.navigate(Routes.GRAMMAR_DETAIL) {
                            popUpTo(Routes.GRAMMAR_IMPORT) { inclusive = true }
                        } else navController.popBackStack(Routes.MAIN, false)
                    }
                }
            )
        }

        composable(Routes.GRAMMAR_EDITOR) {
            val editingQuestions = grammarQuestions.filter { it.grammarNoteId == editingGrammar?.id }
            GrammarEditorScreen(
                note = editingGrammar,
                initialDraft = pendingGrammarDraft,
                questions = editingQuestions,
                patterns = grammarPatterns.filter { it.grammarNoteId == editingGrammar?.id },
                examples = grammarExamples.filter { it.grammarNoteId == editingGrammar?.id },
                courses = (decks.map { it.category } + grammarNotes.map { it.course })
                    .filter(String::isNotBlank).distinct(),
                folders = grammarNotes.map { it.folder }.filter(String::isNotBlank).distinct(),
                categories = grammarNotes.map { it.category }.filter(String::isNotBlank).distinct(),
                levels = grammarNotes.map { it.level }.filter(String::isNotBlank).distinct(),
                initialCourse = pendingGrammarCourse,
                initialFolder = pendingGrammarLibraryName,
                initialShowAiDialog = launchGrammarAi,
                onGenerateAi = viewModel::generateGrammarDraft,
                onScanImages = viewModel::generateGrammarDraftFromImages,
                onSave = { note, questions, patterns ->
                    viewModel.saveGrammarNoteBundle(note, questions, patterns) { id ->
                        selectedGrammarId = id
                        navController.navigate(Routes.GRAMMAR_DETAIL) {
                            popUpTo(Routes.GRAMMAR_EDITOR) { inclusive = true }
                        }
                        launchGrammarAi = false
                        pendingGrammarDraft = null
                    }
                },
                onBack = { launchGrammarAi = false; pendingGrammarDraft = null; pendingGrammarCourse = null; pendingGrammarLibraryName = null; navController.popBackStack() }
            )
        }

        composable(Routes.GRAMMAR_DETAIL) {
            val note = grammarNotes.firstOrNull { it.id == selectedGrammarId }
            if (note == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else GrammarNoteDetailScreen(
                note = note,
                patterns = grammarPatterns.filter { it.grammarNoteId == note.id },
                examples = grammarExamples.filter { it.grammarNoteId == note.id },
                questionCount = grammarQuestions.count { it.grammarNoteId == note.id },
                onBack = { navController.popBackStack() },
                onEdit = {
                    editingGrammar = note
                    launchGrammarAi = false
                    navController.navigate(Routes.GRAMMAR_EDITOR)
                },
                onDelete = {
                    viewModel.deleteGrammarNote(note) {
                        selectedGrammarId = null
                        navController.popBackStack()
                    }
                },
                onFavorite = { viewModel.toggleGrammarFavorite(note) },
                onSpeak = viewModel::speakText,
                onLearn = { navController.navigate(Routes.GRAMMAR_LEARN) },
                onQuiz = { grammarQuizFilterIds = null; navController.navigate(Routes.GRAMMAR_QUIZ) }
            )
        }

        composable(Routes.GRAMMAR_LEARN) {
            val note = grammarNotes.firstOrNull { it.id == selectedGrammarId }
            if (note == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else GrammarLearnScreen(
                note = note,
                patterns = grammarPatterns.filter { it.grammarNoteId == note.id },
                examples = grammarExamples.filter { it.grammarNoteId == note.id },
                onProgress = { viewModel.updateGrammarStudyStep(note, it) },
                onRatePattern = viewModel::recordGrammarPatternResult,
                onBack = { navController.popBackStack() },
                onQuiz = { grammarQuizFilterIds = null; navController.navigate(Routes.GRAMMAR_QUIZ) }
            )
        }

        composable(Routes.GRAMMAR_QUIZ) {
            val note = grammarNotes.firstOrNull { it.id == selectedGrammarId }
            val quizQuestions = customQuizQuestions
                ?: grammarQuestions.filter { it.grammarNoteId == note?.id }
                    .filter { grammarQuizFilterIds == null || it.id in grammarQuizFilterIds.orEmpty() }
            if (quizQuestions.isEmpty() && note == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else GrammarQuizFlowScreen(
                noteId = note?.id ?: 0L,
                noteTitle = if (customQuizTitle.isNotBlank()) customQuizTitle else note?.title.orEmpty(),
                masteryBefore = note?.masteryPercent ?: 0,
                questions = quizQuestions,
                onAnswer = { question, correct, hint ->
                    grammarNotes.firstOrNull { it.id == question.grammarNoteId }?.let { viewModel.recordGrammarAnswer(it, correct, hint) }
                    viewModel.recordGrammarPatternResult(question.grammarPatternId, correct, hint)
                },
                onBack = { customQuizQuestions = null; customQuizTitle = ""; navController.popBackStack() },
                onComplete = { result ->
                    grammarQuizResult = result
                    navController.navigate(Routes.GRAMMAR_QUIZ_RESULT) {
                        popUpTo(Routes.GRAMMAR_QUIZ) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.GRAMMAR_QUIZ_RESULT) {
            val result = grammarQuizResult
            val note = grammarNotes.firstOrNull { it.id == result?.grammarNoteId }
            val title = if (customQuizTitle.isNotBlank()) customQuizTitle else note?.title.orEmpty()
            val questions = customQuizQuestions
                ?: (if (note != null) grammarQuestions.filter { it.grammarNoteId == note.id } else emptyList())
            if (result == null) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            else GrammarQuizResultScreen(
                noteTitle = title.ifBlank { "文法測驗" },
                result = result,
                questions = questions,
                onBack = { customQuizQuestions = null; customQuizTitle = ""; grammarQuizFilterIds = null; navController.popBackStack() },
                onPracticeWrong = {
                    grammarQuizFilterIds = result.wrongQuestionIds.toSet()
                    navController.navigate(Routes.GRAMMAR_QUIZ)
                },
                onRetryAll = {
                    grammarQuizFilterIds = null
                    navController.navigate(Routes.GRAMMAR_QUIZ)
                }
            )
        }

        composable(Routes.GRAMMAR_WRITING_CHECK) {
            GrammarWritingCheckScreen(
                weaknesses = grammarWeaknesses,
                records = grammarWritingRecords,
                courses = (decks.map { it.category } + grammarNotes.map { it.course }).filter(String::isNotBlank).distinct(),
                onCheck = viewModel::checkGrammarWriting,
                onRecognizeSources = viewModel::recognizeGrammarWritingSources,
                onAccept = viewModel::acceptGrammarWritingIssue,
                onSaveRecord = { title, course, original, revised, issues ->
                    viewModel.saveGrammarWritingRecord(title, course, original, revised, issues)
                },
                onDeleteRecord = viewModel::deleteGrammarWritingRecord,
                onBack = { selectedWritingRecordId = null; navController.popBackStack() },
                initialRecordId = selectedWritingRecordId
            )
        }
    }
}
