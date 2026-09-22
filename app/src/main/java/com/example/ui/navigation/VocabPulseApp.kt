package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

    var currentTab by remember { mutableIntStateOf(0) }
    var editingCard by remember { mutableStateOf<Flashcard?>(null) }
    var pendingSharedText by remember { mutableStateOf<String?>(null) }
    var pendingQuizDeckIds by remember { mutableStateOf<Set<Long>?>(null) }
    var pendingQuizTitle by remember { mutableStateOf("") }
    var reviewDeckIds by remember { mutableStateOf<Set<Long>?>(null) }
    var reviewScopeId by remember { mutableStateOf(0L) }
    var quizGameActive by remember { mutableStateOf(false) }
    var updateAvailable by remember { mutableStateOf(false) }
    val updateCheckScope = rememberCoroutineScope()
    val appContext = LocalContext.current.applicationContext

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
            viewModel.selectDeck(null)
            reviewDeckIds = null
            reviewScopeId = 0L
            navController.navigate(Routes.REVIEW) { launchSingleTop = true }
            onWidgetActionConsumed()
        }
    }
    LaunchedEffect(sharedText) {
        if (!sharedText.isNullOrBlank()) {
            pendingSharedText = sharedText
            editingCard = null
            navController.navigate(Routes.ADD_EDIT_CARD) { launchSingleTop = true }
            onSharedTextConsumed()
        }
    }
    NavHost(navController = navController, startDestination = Routes.MAIN) {
        composable(Routes.MAIN) {
            MainScreen(
                currentTab = currentTab,
                onTabSelected = {
                    if (it != currentTab) viewModel.stopSpeaking()
                    if (it == 3) pendingQuizDeckIds = null
                    if (it != 3) quizGameActive = false
                    currentTab = it
                },
                showBottomNavigation = !quizGameActive,
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                updateAvailable = updateAvailable,
                onOpenAssistant = {
                    viewModel.openAssistant(null)
                    navController.navigate(Routes.AI_ASSISTANT) { launchSingleTop = true }
                },
                dashboardContent = {
                    DashboardScreen(
                        decks = decks,
                        allCards = allCards,
                        dueCards = dueCards,
                        totalCount = totalCount,
                        masteredCount = masteredCount,
                        dueCount = dueCount,
                        logs = logs,
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
                        onOpenAssistant = {
                            viewModel.openAssistant(selectedDeckId)
                            navController.navigate(Routes.AI_ASSISTANT) { launchSingleTop = true }
                        }
                    )
                },
                foldersContent = {
                    FoldersManagementScreen(
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
                    StudyHubScreen(
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
                    QuizGamesScreen(
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
                        onGameActiveChange = { quizGameActive = it },
                        onOpenAssistant = {
                            viewModel.openAssistant(null)
                            navController.navigate(Routes.AI_ASSISTANT) { launchSingleTop = true }
                        },
                        showHubTopBar = false
                    )
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
                onStopSpeaking = viewModel::stopSpeaking,
                onBackToDashboard = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.MAIN) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onOpenQuizGames = {
                    navController.popBackStack()
                    pendingQuizDeckIds = reviewDeckIds
                        ?: selectedDeckId?.let(::setOf)
                        ?: decks.map { it.id }.toSet()
                    pendingQuizTitle = when {
                        reviewDeckIds != null -> "目前課程全部資料夾"
                        selectedDeckId != null -> decks.firstOrNull { it.id == selectedDeckId }?.name.orEmpty()
                        else -> "全站所有單字卡"
                    }
                    currentTab = 3
                }
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
                selectedDeckIds = assistantSelectedDeckIds,
                onScopeChange = viewModel::setAssistantScope,
                onSend = viewModel::sendAssistantMessage,
                onStop = viewModel::stopAssistantRequest,
                onQuizCompleted = viewModel::completeAssistantQuiz,
                onQuizRestart = viewModel::restartAssistantQuiz,
                onQuizProgress = viewModel::saveAssistantQuizProgress,
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

        composable(Routes.CARD_LIST) {
            CardListScreen(
                cards = allCards,
                decks = decks,
                selectedDeckId = selectedDeckId,
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
                    navController.navigate(Routes.ADD_EDIT_CARD)
                },
                onSpeak = viewModel::speakText,
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
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ADD_EDIT_CARD) {
            AddEditCardScreen(
                editingCard = editingCard,
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
                onParseText = viewModel::parseOcrPhotoText,
                onImportCandidates = { candidates, deckId ->
                    viewModel.importOcrCandidates(candidates, deckId)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
                pdfPageLimit = settings.pdfPageLimit
            )
        }
    }
}
