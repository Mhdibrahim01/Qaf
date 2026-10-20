package com.example.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.ApiKeyDialog
import com.example.ui.components.GoldBadge
import com.example.ui.components.WordBooksBottomNavigation
import com.example.ui.components.WordBooksNavigationRail
import com.example.ui.components.WordBooksTopAppBar
import com.example.ui.components.cleanGrammarArabicTitle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.ui.screens.BatchTranslateScreen
import com.example.ui.screens.CorrectionNotebookScreen
import com.example.ui.screens.FlashcardsScreen
import com.example.ui.screens.GrammarScreen
import com.example.ui.screens.HomePlaceholderScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LabsDataCheckScreen
import com.example.ui.screens.MoreScreen
import com.example.ui.screens.PlanScreen
import com.example.ui.screens.ReadingScreen
import com.example.ui.screens.SentencesScreen
import com.example.ui.screens.WordBooksDatabaseScreen
import com.example.ui.viewmodel.WordBooksViewModel
import com.example.ui.theme.ArabicTypography
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderDark
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldAccentBorder
import com.example.ui.theme.GoldAccentDim
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevatedDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Responsive navigation shell for WordBooks:
 * - Bottom navigation bar on phones (< 600dp width) showing all 7 sections
 * - Side NavigationRail on tablets / landscape (>= 600dp width)
 */
@Composable
fun WordBooksAppShell(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    wordBooksViewModel: WordBooksViewModel = viewModel()
) {
    val configuration = LocalConfiguration.current
    val useSideRail = configuration.screenWidthDp >= 600

    val decks by wordBooksViewModel.decks.collectAsStateWithLifecycle()
    val importState by wordBooksViewModel.importState.collectAsStateWithLifecycle()
    val totalEntriesCount by wordBooksViewModel.totalEntriesCount.collectAsStateWithLifecycle()
    val emptyArabicCount by wordBooksViewModel.emptyArabicCount.collectAsStateWithLifecycle()
    val selectedDeckId by wordBooksViewModel.selectedDeckId.collectAsStateWithLifecycle()
    val filterState by wordBooksViewModel.filterState.collectAsStateWithLifecycle()
    val progressMap by wordBooksViewModel.progressMap.collectAsStateWithLifecycle()
    val currentlyPlayingWord by wordBooksViewModel.currentlyPlayingWord.collectAsStateWithLifecycle()
    val detailedCard by wordBooksViewModel.detailedCard.collectAsStateWithLifecycle()
    val favoriteCount by wordBooksViewModel.favoriteCount.collectAsStateWithLifecycle()
    val masteredCount by wordBooksViewModel.masteredCount.collectAsStateWithLifecycle()
    val reviewDueTodayCount by wordBooksViewModel.reviewDueTodayCount.collectAsStateWithLifecycle()
    val studiedTodayCount by wordBooksViewModel.studiedTodayCount.collectAsStateWithLifecycle()
    val deckStudyStates by wordBooksViewModel.deckStudyStates.collectAsStateWithLifecycle()
    val flashcardDeckId by wordBooksViewModel.flashcardDeckId.collectAsStateWithLifecycle()
    val flashcardCards by wordBooksViewModel.flashcardCards.collectAsStateWithLifecycle()
    val flashcardIndex by wordBooksViewModel.flashcardIndex.collectAsStateWithLifecycle()
    val isFlashcardsLoading by wordBooksViewModel.isFlashcardsLoading.collectAsStateWithLifecycle()
    val isCurrentCardFlipped by wordBooksViewModel.isCurrentCardFlipped.collectAsStateWithLifecycle()
    val flashcardDirection by wordBooksViewModel.flashcardDirection.collectAsStateWithLifecycle()
    val flashcardAutoShowAnswer by wordBooksViewModel.flashcardAutoShowAnswer.collectAsStateWithLifecycle()
    val savedSentences by wordBooksViewModel.savedSentences.collectAsStateWithLifecycle()
    val savedSentenceKeys by wordBooksViewModel.savedSentenceKeys.collectAsStateWithLifecycle()
    val dictionarySampleExamples by wordBooksViewModel.dictionarySampleExamples.collectAsStateWithLifecycle()
    val sentenceSortOrder by wordBooksViewModel.sentenceSortOrder.collectAsStateWithLifecycle()
    val showSentenceTranslation by wordBooksViewModel.showSentenceTranslation.collectAsStateWithLifecycle()
    val ttsSpeechRate by wordBooksViewModel.ttsSpeechRate.collectAsStateWithLifecycle()
    val dictationIgnorePunctuation by wordBooksViewModel.dictationIgnorePunctuation.collectAsStateWithLifecycle()
    val autoPlayPracticeAudio by wordBooksViewModel.autoPlayPracticeAudio.collectAsStateWithLifecycle()
    val explainingSentenceIds by wordBooksViewModel.explainingSentenceIds.collectAsStateWithLifecycle()
    val deckArabicStats by wordBooksViewModel.deckArabicStats.collectAsStateWithLifecycle()
    val aiStatus by wordBooksViewModel.aiStatus.collectAsStateWithLifecycle()
    val batchTranslateState by wordBooksViewModel.batchTranslateState.collectAsStateWithLifecycle()
    val isBatchTranslateOpen by wordBooksViewModel.isBatchTranslateOpen.collectAsStateWithLifecycle()
    val grammarLessons by wordBooksViewModel.grammarLessons.collectAsStateWithLifecycle()
    val grammarProgressList by wordBooksViewModel.grammarProgressList.collectAsStateWithLifecycle()
    val selectedGrammarLesson by wordBooksViewModel.selectedGrammarLesson.collectAsStateWithLifecycle()
    val grammarLessonChatHistory by wordBooksViewModel.grammarLessonChatHistory.collectAsStateWithLifecycle()
    val grammarSentenceCheckHistory by wordBooksViewModel.grammarSentenceCheckHistory.collectAsStateWithLifecycle()
    val readingTexts by wordBooksViewModel.readingTexts.collectAsStateWithLifecycle()
    val readingProgressList by wordBooksViewModel.readingProgressList.collectAsStateWithLifecycle()
    val selectedReadingText by wordBooksViewModel.selectedReadingText.collectAsStateWithLifecycle()
    val readingTextChatHistory by wordBooksViewModel.readingTextChatHistory.collectAsStateWithLifecycle()
    val readingOpenQuestionHistory by wordBooksViewModel.readingOpenQuestionHistory.collectAsStateWithLifecycle()
    val correctionEntries by wordBooksViewModel.correctionEntries.collectAsStateWithLifecycle()
    val studyPlanProfile by wordBooksViewModel.studyPlanProfile.collectAsStateWithLifecycle()
    val currentStudyPlan by wordBooksViewModel.currentStudyPlan.collectAsStateWithLifecycle()
    val todayChecklist by wordBooksViewModel.todayChecklist.collectAsStateWithLifecycle()
    val studyStreak by wordBooksViewModel.studyStreak.collectAsStateWithLifecycle()
    val quizHistory by wordBooksViewModel.quizHistory.collectAsStateWithLifecycle()
    val weakAreas by wordBooksViewModel.weakAreas.collectAsStateWithLifecycle()
    val monthlyChecklists by wordBooksViewModel.monthlyChecklists.collectAsStateWithLifecycle()
    val currentAiWordDetail by wordBooksViewModel.currentAiWordDetail.collectAsStateWithLifecycle()
    val isAiDetailLoading by wordBooksViewModel.isAiDetailLoading.collectAsStateWithLifecycle()
    val aiDetailError by wordBooksViewModel.aiDetailError.collectAsStateWithLifecycle()
    var isSettingsOpen by remember { mutableStateOf(false) }
    var isApiKeyDialogOpen by remember { mutableStateOf(false) }
    val apiKeyTrigger by wordBooksViewModel.apiKeyUpdatedTrigger.collectAsStateWithLifecycle()
    val currentKeySource = remember(apiKeyTrigger) { wordBooksViewModel.getApiKeySource() }
    val customApiKey = remember(apiKeyTrigger) { wordBooksViewModel.getCustomApiKey() }
    val pagedEntries = wordBooksViewModel.pagedEntries.collectAsLazyPagingItems()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val currentRoute: AppRoute = when {
        currentDestination?.hasRoute(AppRoute.Home::class) == true -> AppRoute.Home
        currentDestination?.hasRoute(AppRoute.WordBooks::class) == true -> AppRoute.WordBooks
        currentDestination?.hasRoute(AppRoute.Flashcards::class) == true -> AppRoute.Flashcards
        currentDestination?.hasRoute(AppRoute.Sentences::class) == true -> AppRoute.Sentences
        currentDestination?.hasRoute(AppRoute.Grammar::class) == true -> AppRoute.Grammar
        currentDestination?.hasRoute(AppRoute.Reading::class) == true -> AppRoute.Reading
        currentDestination?.hasRoute(AppRoute.Plan::class) == true -> AppRoute.Plan
        currentDestination?.hasRoute(AppRoute.Labs::class) == true -> AppRoute.Labs
        currentDestination?.hasRoute(AppRoute.CorrectionNotebook::class) == true -> AppRoute.CorrectionNotebook
        currentDestination?.hasRoute(AppRoute.More::class) == true -> AppRoute.More
        else -> AppRoute.Home
    }

    val onNavigateToDestination: (BottomNavDestination) -> Unit = { destination ->
        if (destination.route == AppRoute.WordBooks) {
            wordBooksViewModel.selectDeck(null)
        }
        if (destination == BottomNavDestination.GRAMMAR) {
            wordBooksViewModel.selectGrammarLesson(null)
        }
        if (destination == BottomNavDestination.READING) {
            wordBooksViewModel.stopAudio()
            wordBooksViewModel.selectReadingText(null)
        }
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                inclusive = false
            }
            launchSingleTop = true
        }
    }

    val topLevelDestinations = setOf(
        AppRoute.Home,
        AppRoute.WordBooks,
        AppRoute.Flashcards,
        AppRoute.Grammar,
        AppRoute.Reading,
        AppRoute.More
    )

    val canNavigateBack = currentRoute !in topLevelDestinations ||
        (currentRoute == AppRoute.Grammar && selectedGrammarLesson != null) ||
        (currentRoute == AppRoute.Reading && selectedReadingText != null)

    val onNavigateBack: () -> Unit = {
        when {
            currentRoute == AppRoute.Grammar && selectedGrammarLesson != null -> {
                wordBooksViewModel.selectGrammarLesson(null)
            }
            currentRoute == AppRoute.Reading && selectedReadingText != null -> {
                wordBooksViewModel.stopAudio()
                wordBooksViewModel.selectReadingText(null)
            }
            !navController.popBackStack() -> {
                if (currentRoute in listOf(
                        AppRoute.Sentences,
                        AppRoute.Plan,
                        AppRoute.CorrectionNotebook,
                        AppRoute.Labs
                    )
                ) {
                    navController.navigate(AppRoute.More) { launchSingleTop = true }
                } else {
                    navController.navigate(AppRoute.Home) { launchSingleTop = true }
                }
            }
        }
    }

    val (topTitle, topSubtitle, topArabicTitle) = when (currentRoute) {
        AppRoute.Home -> Triple("Qaf", "قاف • Learning Dashboard", "الرئيسية")
        AppRoute.WordBooks -> Triple("Word Books", "Vocabulary Library & Cards", "مكتبة المفردات")
        AppRoute.Flashcards -> {
            val deckTitle = decks.find { it.id == flashcardDeckId }?.title ?: "Review"
            Triple("Word Books", "Flashcards • $deckTitle", "البطاقات التعليمية")
        }
        AppRoute.Sentences -> Triple("Sentences", "Favorites & Practice", "الجمل المحفوظة")
        AppRoute.More -> Triple("Learning Hub", "Advanced Tools & Preferences", "المزيد")
        AppRoute.Grammar -> {
            val lesson = selectedGrammarLesson
            if (lesson != null) {
                Triple(lesson.titleEn, "${lesson.level} • ${lesson.topic}", cleanGrammarArabicTitle(lesson.titleAr))
            } else {
                Triple("Grammar Lessons", "CEFR A1–C1", "القواعد النحوية")
            }
        }
        AppRoute.Reading -> {
            val text = selectedReadingText
            if (text != null) {
                Triple(text.titleEn, "${text.level} • ${text.topic}", text.titleAr)
            } else {
                Triple("Graded Reading", "Bilingual Texts", "القراءة المتدرجة")
            }
        }
        AppRoute.Plan -> Triple("Study Plan", "Daily Adaptive Checklist", "خطة التعلم")
        AppRoute.CorrectionNotebook -> Triple("Correction Notebook", "Spaced Review", "دفتر تصحيح الأخطاء")
        AppRoute.Labs -> Triple("Labs & Diagnostics", "Database Tools", "المختبر")
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark),
        containerColor = BackgroundDark,
        topBar = {
            val isWordBookDetailOpen = currentRoute == AppRoute.WordBooks && selectedDeckId != null
            if (!isWordBookDetailOpen) {
                WordBooksTopAppBar(
                    title = topTitle,
                    subtitle = topSubtitle,
                    arabicTitle = topArabicTitle,
                    canNavigateBack = canNavigateBack,
                    onNavigateBack = onNavigateBack,
                    actions = {
                        IconButton(
                            onClick = { isSettingsOpen = true },
                            modifier = Modifier.testTag("btn_top_settings")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Settings",
                                tint = TextMuted
                            )
                        }
                    }
                )
            }
        },
        bottomBar = {
            val isDetailActive = (currentRoute == AppRoute.Grammar && selectedGrammarLesson != null) ||
                (currentRoute == AppRoute.Reading && selectedReadingText != null) ||
                currentRoute !in topLevelDestinations
            if (!useSideRail && !isDetailActive) {
                WordBooksBottomNavigation(
                    currentRoute = currentRoute,
                    onNavigateToDestination = onNavigateToDestination
                )
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (useSideRail) {
                WordBooksNavigationRail(
                    currentRoute = currentRoute,
                    onNavigateToDestination = onNavigateToDestination
                )
                VerticalDivider(color = BorderDark, thickness = 1.dp)
            }

            NavHost(
                navController = navController,
                startDestination = AppRoute.Home,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                enterTransition = { fadeIn() },
                exitTransition = { fadeOut() }
            ) {
                composable<AppRoute.Home> {
                    HomeScreen(
                        decks = decks,
                        importState = importState,
                        totalEntriesCount = totalEntriesCount,
                        reviewDueTodayCount = reviewDueTodayCount,
                        studiedTodayCount = studiedTodayCount,
                        masteredCount = masteredCount,
                        deckStudyStates = deckStudyStates,
                        todayChecklist = todayChecklist,
                        studyStreak = studyStreak,
                        onOpenDeck = { deckId ->
                            wordBooksViewModel.selectDeck(deckId)
                            navController.navigate(AppRoute.WordBooks) { launchSingleTop = true }
                        },
                        onOpenFlashcardsForDeck = { deckId ->
                            wordBooksViewModel.selectFlashcardDeck(deckId)
                            navController.navigate(AppRoute.Flashcards) { launchSingleTop = true }
                        },
                        onOpenPlanScreen = {
                            navController.navigate(AppRoute.Plan) { launchSingleTop = true }
                        },
                        onNavigateToTask = { route, linkedId ->
                            wordBooksViewModel.preparePlanTaskNavigation(route, linkedId)
                            when (route) {
                                "flashcards" -> navController.navigate(AppRoute.Flashcards) { launchSingleTop = true }
                                "database" -> navController.navigate(AppRoute.WordBooks) { launchSingleTop = true }
                                "reading" -> navController.navigate(AppRoute.Reading) { launchSingleTop = true }
                                "grammar" -> navController.navigate(AppRoute.Grammar) { launchSingleTop = true }
                                else -> navController.navigate(AppRoute.Plan) { launchSingleTop = true }
                            }
                        }
                    )
                }
                composable<AppRoute.WordBooks> {
                    WordBooksDatabaseScreen(
                        decks = decks,
                        importState = importState,
                        totalEntriesCount = totalEntriesCount,
                        selectedDeckId = selectedDeckId,
                        filterState = filterState,
                        pagedEntries = pagedEntries,
                        progressMap = progressMap,
                        currentlyPlayingWord = currentlyPlayingWord,
                        detailedCard = detailedCard,
                        favoriteCount = favoriteCount,
                        masteredCount = masteredCount,
                        reviewDueTodayCount = reviewDueTodayCount,
                        isAiTranslating = aiStatus.isRunning,
                        savedSentenceKeys = savedSentenceKeys,
                        aiDetail = currentAiWordDetail,
                        isAiDetailLoading = isAiDetailLoading,
                        aiDetailError = aiDetailError,
                        canRequestAi = wordBooksViewModel.canMakeAiRequest(),
                        isAllowAi = batchTranslateState.allowAi,
                        isOnline = wordBooksViewModel.isDeviceOnline(),
                        onSelectDeck = wordBooksViewModel::selectDeck,
                        onOpenFlashcards = {
                            navController.navigate(AppRoute.Flashcards) { launchSingleTop = true }
                        },
                        onOpenFlashcardsForDeck = { deckId ->
                            wordBooksViewModel.selectFlashcardDeck(deckId)
                            navController.navigate(AppRoute.Flashcards) { launchSingleTop = true }
                        },
                        onUpdateDeckFilterInList = wordBooksViewModel::updateDeckFilterInList,
                        onSearchQueryChange = wordBooksViewModel::updateSearchQuery,
                        onTypeFilterChange = wordBooksViewModel::updateTypeFilter,
                        onLevelFilterChange = wordBooksViewModel::updateLevelFilter,
                        onPosFilterChange = wordBooksViewModel::updatePosFilter,
                        onSortOrderChange = wordBooksViewModel::updateSortOrder,
                        onClearFilters = wordBooksViewModel::clearAllFilters,
                        onToggleFavorite = wordBooksViewModel::toggleFavorite,
                        onToggleMastered = wordBooksViewModel::toggleMastered,
                        onSelectWordListTab = wordBooksViewModel::selectWordListTab,
                        onStatusFilterChange = wordBooksViewModel::updateStatusFilter,
                        onOpenDetailedDefinition = wordBooksViewModel::openDetailedDefinition,
                        onDismissDetailedDefinition = wordBooksViewModel::dismissDetailedDefinition,
                        onRetryAiDefinition = wordBooksViewModel::retryAiDefinition,
                        onEnrichWordWithAi = wordBooksViewModel::enrichCurrentWordWithAi,
                        onSaveUserArabic = wordBooksViewModel::saveUserArabicForEntry,
                        onToggleSaveSentence = wordBooksViewModel::toggleSaveExampleSentence,
                        onPlayAudio = wordBooksViewModel::playAudio,
                        onPlayVoiceAudio = wordBooksViewModel::playWordVoiceAudio,
                        onSpeakSentenceTts = wordBooksViewModel::speakSentenceTts,
                        onReverifyImport = { wordBooksViewModel.startImportIfNeeded(forceRecheck = true) }
                    )
                }
                composable<AppRoute.Flashcards> {
                    FlashcardsScreen(
                        decks = decks,
                        activeDeckId = flashcardDeckId,
                        cards = flashcardCards,
                        currentIndex = flashcardIndex,
                        isLoading = isFlashcardsLoading,
                        progressMap = progressMap,
                        reviewDueTodayCount = reviewDueTodayCount,
                        studiedTodayCount = studiedTodayCount,
                        currentlyPlayingWord = currentlyPlayingWord,
                        detailedCard = detailedCard,
                        isAiTranslating = aiStatus.isRunning,
                        savedSentenceKeys = savedSentenceKeys,
                        aiDetail = currentAiWordDetail,
                        isAiDetailLoading = isAiDetailLoading,
                        aiDetailError = aiDetailError,
                        canRequestAi = wordBooksViewModel.canMakeAiRequest(),
                        isAllowAi = batchTranslateState.allowAi,
                        isOnline = wordBooksViewModel.isDeviceOnline(),
                        isCardFlipped = isCurrentCardFlipped,
                        cardDirection = flashcardDirection,
                        autoShowAnswer = flashcardAutoShowAnswer,
                        onSelectDeck = wordBooksViewModel::selectFlashcardDeck,
                        onSwitchToBooks = {
                            navController.navigate(AppRoute.WordBooks) { launchSingleTop = true }
                        },
                        onIndexChanged = wordBooksViewModel::onFlashcardIndexChanged,
                        onFlipCard = wordBooksViewModel::flipCurrentFlashcard,
                        onSetCardFlipped = wordBooksViewModel::setCurrentFlashcardFlipped,
                        onUpdateCardDirection = wordBooksViewModel::updateFlashcardDirection,
                        onUpdateAutoShowAnswer = wordBooksViewModel::updateFlashcardAutoShowAnswer,
                        onRateCard = wordBooksViewModel::rateCurrentFlashcard,
                        onToggleMastered = wordBooksViewModel::toggleCurrentFlashcardMastered,
                        onToggleFavorite = wordBooksViewModel::toggleCurrentFlashcardFavorite,
                        onPlayAudio = wordBooksViewModel::playAudio,
                        onPlayVoiceAudio = wordBooksViewModel::playWordVoiceAudio,
                        onSpeakSentenceTts = wordBooksViewModel::speakSentenceTts,
                        onOpenDetailedDefinition = wordBooksViewModel::openDetailedDefinition,
                        onDismissDetailedDefinition = wordBooksViewModel::dismissDetailedDefinition,
                        onRetryAiDefinition = wordBooksViewModel::retryAiDefinition,
                        onEnrichWordWithAi = wordBooksViewModel::enrichCurrentWordWithAi,
                        onSaveUserArabic = wordBooksViewModel::saveUserArabicForEntry,
                        onToggleSaveSentence = wordBooksViewModel::toggleSaveExampleSentence,
                        onToggleCardFavoriteInSheet = wordBooksViewModel::toggleFavorite,
                        onToggleCardMasteredInSheet = wordBooksViewModel::toggleMastered
                    )
                }
                composable<AppRoute.Sentences> {
                    SentencesScreen(
                        savedSentences = savedSentences,
                        savedSentenceKeys = savedSentenceKeys,
                        dictionarySampleExamples = dictionarySampleExamples,
                        sortOrder = sentenceSortOrder,
                        showTranslation = showSentenceTranslation,
                        ttsSpeechRate = ttsSpeechRate,
                        dictationIgnorePunctuation = dictationIgnorePunctuation,
                        autoPlayPracticeAudio = autoPlayPracticeAudio,
                        currentlyPlayingText = currentlyPlayingWord,
                        explainingSentenceIds = explainingSentenceIds,
                        aiErrorMessage = aiStatus.error,
                        onPlaySentenceAudio = wordBooksViewModel::playAudio,
                        onSpeakSentenceTts = wordBooksViewModel::speakSentenceTts,
                        onToggleSaveSentence = wordBooksViewModel::toggleSaveExampleSentence,
                        onDeleteSentence = wordBooksViewModel::deleteSentence,
                        onExplainSentenceWithAi = wordBooksViewModel::explainSentenceWithAi,
                        onToggleSortOrder = wordBooksViewModel::toggleSentenceSortOrder,
                        onToggleShowTranslation = wordBooksViewModel::toggleShowSentenceTranslation,
                        onUpdateTtsSpeechRate = wordBooksViewModel::updateTtsSpeechRate,
                        onUpdateDictationIgnorePunctuation = wordBooksViewModel::updateDictationIgnorePunctuation,
                        onUpdateAutoPlayPracticeAudio = wordBooksViewModel::updateAutoPlayPracticeAudio,
                        onNavigateToWordBooks = {
                            navController.navigate(AppRoute.WordBooks) { launchSingleTop = true }
                        },
                        onNavigateToFlashcards = {
                            navController.navigate(AppRoute.Flashcards) { launchSingleTop = true }
                        }
                    )
                }
                composable<AppRoute.Grammar> {
                    GrammarScreen(
                        lessons = grammarLessons,
                        progressList = grammarProgressList,
                        chatHistory = grammarLessonChatHistory,
                        sentenceCheckHistory = grammarSentenceCheckHistory,
                        selectedLesson = selectedGrammarLesson,
                        readingTexts = readingTexts,
                        onSelectLesson = wordBooksViewModel::selectGrammarLesson,
                        onOpenReadingText = { text ->
                            wordBooksViewModel.selectReadingText(text)
                            navController.navigate(AppRoute.Reading) {
                                launchSingleTop = true
                            }
                        },
                        onNavigateToNotebook = {
                            navController.navigate(AppRoute.CorrectionNotebook) {
                                launchSingleTop = true
                            }
                        },
                        onSpeakTts = { wordBooksViewModel.speakSentenceTts(it) },
                        onSaveProgress = wordBooksViewModel::saveGrammarProgress,
                        onAskAi = wordBooksViewModel::askAiAboutLesson,
                        onExplainMistake = wordBooksViewModel::explainGrammarMistake,
                        onCheckSentence = wordBooksViewModel::checkGrammarSentence,
                        onMorePractice = wordBooksViewModel::generateMoreGrammarPractice,
                        onCreateLessonWithAi = wordBooksViewModel::createGrammarLessonWithAi,
                        onSyncAssets = { wordBooksViewModel.syncGrammarLessonsFromAssets() },
                        onImportJsonUris = { wordBooksViewModel.importGrammarLessonsFromUris(it) },
                        onImportJsonText = { wordBooksViewModel.importGrammarLessonsFromText(it) }
                    )
                }
                composable<AppRoute.Reading> {
                    ReadingScreen(
                        texts = readingTexts,
                        progressList = readingProgressList,
                        selectedText = selectedReadingText,
                        chatHistory = readingTextChatHistory,
                        openQuestionHistory = readingOpenQuestionHistory,
                        grammarLessons = grammarLessons,
                        onSelectText = wordBooksViewModel::selectReadingText,
                        onSaveProgress = wordBooksViewModel::saveReadingProgress,
                        onOpenGrammarLesson = { lesson ->
                            wordBooksViewModel.selectGrammarLesson(lesson)
                            navController.navigate(AppRoute.Grammar) {
                                launchSingleTop = true
                            }
                        },
                        onSaveCorrectionSentence = { textId, lessonId, lessonTitle, original, corrected, expAr, alt, status ->
                            wordBooksViewModel.saveCorrectionNotebookEntry(
                                textId = textId,
                                grammarLessonId = lessonId,
                                grammarLessonTitle = lessonTitle,
                                originalSentence = original,
                                correctedSentence = corrected,
                                explanationAr = expAr,
                                betterAlternative = alt,
                                status = status
                            )
                        },
                        onCheckCorrectionWithAi = { entry ->
                            wordBooksViewModel.checkCorrectionSentenceWithAi(entry)
                        },
                        onNavigateToNotebook = {
                            navController.navigate(AppRoute.CorrectionNotebook) {
                                launchSingleTop = true
                            }
                        },
                        onLookupWordLocally = wordBooksViewModel::lookupWordLocally,
                        onSaveWordFavorite = wordBooksViewModel::saveWordFavorite,
                        onGetWordFavoriteStatus = wordBooksViewModel::getWordFavoriteStatus,
                        onGetSavedTargetWords = wordBooksViewModel::getSavedTargetWords,
                        onExplainWordInContext = wordBooksViewModel::explainWordInContext,
                        onSavePastedText = wordBooksViewModel::savePastedText,
                        onTranslateAndMakeQuestionsForPastedText = wordBooksViewModel::translateAndMakeQuestionsForPastedText,
                        onCreateNewReadingTextWithAi = wordBooksViewModel::createNewReadingTextWithAi,
                        onSimplifyReadingText = wordBooksViewModel::simplifyReadingText,
                        onAskAiAboutText = wordBooksViewModel::askAiAboutReadingText,
                        onFeedbackOnOpenQuestion = wordBooksViewModel::feedbackOnOpenReadingQuestion,
                        onDeleteText = wordBooksViewModel::deleteReadingText,
                        onSyncAssets = { wordBooksViewModel.syncReadingTextsFromAssets(it) },
                        onImportJsonUris = { uris, cb -> wordBooksViewModel.importReadingTextsFromUris(uris, cb) },
                        onImportJsonText = wordBooksViewModel::importReadingTextsFromText,
                        onSpeakSentence = { wordBooksViewModel.speakSentenceTts(it) },
                        onStopAudio = { wordBooksViewModel.stopAudio() },
                        ttsSpeechRate = ttsSpeechRate
                    )
                }
                composable<AppRoute.Labs> {
                    if (isBatchTranslateOpen) {
                        BatchTranslateScreen(
                            state = batchTranslateState,
                            onBack = wordBooksViewModel::closeBatchTranslateScreen,
                            onSelectDeck = wordBooksViewModel::selectBatchTranslateDeck,
                            onSelectTargetCount = wordBooksViewModel::selectBatchTranslateTargetCount,
                            onStart = wordBooksViewModel::startBatchTranslate,
                            onPause = wordBooksViewModel::pauseBatchTranslate,
                            onResume = wordBooksViewModel::resumeBatchTranslate,
                            onStop = wordBooksViewModel::stopBatchTranslate,
                            onRetryFailed = wordBooksViewModel::retryBatchTranslateFailed,
                            onToggleAllowAi = wordBooksViewModel::setBatchAllowAi,
                            onChangeDailyLimit = wordBooksViewModel::setBatchDailyLimit,
                            onOpenApiKeyDialog = { isApiKeyDialogOpen = true }
                        )
                    } else {
                        LabsDataCheckScreen(
                            decks = decks,
                            importState = importState,
                            aiStatus = aiStatus,
                            totalEntriesCount = totalEntriesCount,
                            emptyArabicCount = emptyArabicCount,
                            deckArabicStats = deckArabicStats,
                            grammarLessonsCount = grammarLessons.size,
                            onReimportDatabase = { wordBooksViewModel.startImportIfNeeded(forceRecheck = true) },
                            onResetAndReimport = wordBooksViewModel::clearAndReimportDatabase,
                            onImportJsonUris = wordBooksViewModel::importJsonUris,
                            onOpenBatchTranslate = wordBooksViewModel::openBatchTranslateScreen,
                            onGenerateDeckBatch = { deckId -> wordBooksViewModel.generateMissingDeckBatch(deckId, 25) },
                            onSyncGrammarAssets = { wordBooksViewModel.syncGrammarLessonsFromAssets() },
                            onImportGrammarJsonUris = { wordBooksViewModel.importGrammarLessonsFromUris(it) }
                        )
                    }
                }
                composable<AppRoute.CorrectionNotebook> {
                    CorrectionNotebookScreen(
                        entries = correctionEntries,
                        grammarLessons = grammarLessons,
                        readingTexts = readingTexts,
                        onBack = { navController.popBackStack() },
                        onSaveEntry = { textId, lessonId, lessonTitle, original, corrected, expAr, alt, status ->
                            wordBooksViewModel.saveCorrectionNotebookEntry(
                                textId = textId,
                                grammarLessonId = lessonId,
                                grammarLessonTitle = lessonTitle,
                                originalSentence = original,
                                correctedSentence = corrected,
                                explanationAr = expAr,
                                betterAlternative = alt,
                                status = status
                            )
                        },
                        onUpdateEntry = wordBooksViewModel::updateCorrectionNotebookEntry,
                        onDeleteEntry = wordBooksViewModel::deleteCorrectionNotebookEntry,
                        onCheckWithAi = { entry ->
                            wordBooksViewModel.checkCorrectionSentenceWithAi(entry)
                        },
                        onRecordSpacedReview = { id, newSentence, isSuccess ->
                            wordBooksViewModel.recordSpacedReviewAttempt(id, newSentence, isSuccess)
                        },
                        onNavigateToLesson = { lesson ->
                            wordBooksViewModel.selectGrammarLesson(lesson)
                            navController.navigate(AppRoute.Grammar) {
                                launchSingleTop = true
                            }
                        },
                        onNavigateToReadingText = { text ->
                            wordBooksViewModel.selectReadingText(text)
                            navController.navigate(AppRoute.Reading) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable<AppRoute.Plan> {
                    PlanScreen(
                        profile = studyPlanProfile,
                        currentPlan = currentStudyPlan,
                        todayChecklist = todayChecklist,
                        streak = studyStreak,
                        quizHistory = quizHistory,
                        weakAreas = weakAreas,
                        monthlyChecklists = monthlyChecklists,
                        onSaveProfile = wordBooksViewModel::savePlanProfile,
                        onGeneratePlanWithAi = wordBooksViewModel::generatePlanWithAi,
                        onGenerateDailyQuiz = wordBooksViewModel::generateDailyQuiz,
                        onGenerateWeeklyQuiz = wordBooksViewModel::generateWeeklyQuiz,
                        onGenerateWeakAreasQuiz = wordBooksViewModel::generateWeakAreasQuiz,
                        onRefreshPlan = wordBooksViewModel::refreshDailyChecklist,
                        onSubmitQuizAnswers = wordBooksViewModel::submitQuizAnswers,
                        onPlayAudio = { text: String -> wordBooksViewModel.speakSentenceTts(text) },
                        onToggleTaskCompleted = wordBooksViewModel::toggleTaskCompleted,
                        onNavigateToTaskRoute = { route: String, linkedId: String ->
                            wordBooksViewModel.preparePlanTaskNavigation(route, linkedId)
                            when (route) {
                                "flashcards" -> navController.navigate(AppRoute.Flashcards) { launchSingleTop = true }
                                "word_books" -> navController.navigate(AppRoute.WordBooks) { launchSingleTop = true }
                                "grammar" -> navController.navigate(AppRoute.Grammar) { launchSingleTop = true }
                                "reading" -> navController.navigate(AppRoute.Reading) { launchSingleTop = true }
                                "sentences" -> navController.navigate(AppRoute.Sentences) { launchSingleTop = true }
                                "correction_notebook" -> {
                                    navController.navigate(AppRoute.CorrectionNotebook) {
                                        launchSingleTop = true
                                    }
                                }
                            }
                        },
                        onOpenDictionaryWord = { word ->
                            wordBooksViewModel.openDetailedDefinition("", word)
                        }
                    )
                }
                composable<AppRoute.More> {
                    MoreScreen(
                        onNavigateToGrammar = {
                            wordBooksViewModel.selectGrammarLesson(null)
                            navController.navigate(AppRoute.Grammar) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    inclusive = false
                                }
                                launchSingleTop = true
                            }
                        },
                        onNavigateToReading = {
                            wordBooksViewModel.stopAudio()
                            wordBooksViewModel.selectReadingText(null)
                            navController.navigate(AppRoute.Reading) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    inclusive = false
                                }
                                launchSingleTop = true
                            }
                        },
                        onNavigateToPlan = {
                            navController.navigate(AppRoute.Plan) { launchSingleTop = true }
                        },
                        onNavigateToCorrectionNotebook = {
                            navController.navigate(AppRoute.CorrectionNotebook) { launchSingleTop = true }
                        },
                        onNavigateToSentences = {
                            navController.navigate(AppRoute.Sentences) { launchSingleTop = true }
                        },
                        onOpenSettings = { isSettingsOpen = true },
                        onNavigateToLabs = {
                            navController.navigate(AppRoute.Labs) { launchSingleTop = true }
                        },
                        grammarLessonsCount = grammarLessons.size,
                        readingTextsCount = readingTexts.size,
                        correctionEntriesCount = correctionEntries.size,
                        savedSentencesCount = savedSentences.size,
                        studyStreak = studyStreak
                    )
                }
            }
        }
    }

    if (isSettingsOpen) {
        AlertDialog(
            onDismissRequest = { isSettingsOpen = false },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(22.dp),
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Settings & Preferences",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "Configure AI features, API keys, and diagnostics",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Allow AI Toggle Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Allow AI Features",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary
                            )
                            Text(
                                text = "Enable on-demand Gemini explanations and translations",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = batchTranslateState.allowAi,
                            onCheckedChange = { wordBooksViewModel.setBatchAllowAi(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BackgroundDark,
                                checkedTrackColor = GoldAccent
                            ),
                            modifier = Modifier.testTag("switch_allow_ai_settings")
                        )
                    }

                    HorizontalDivider(color = BorderDark)

                    // Google Gemini API Key row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Google Gemini API Key",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary
                            )
                            Text(
                                text = currentKeySource,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (wordBooksViewModel.isApiKeyConfigured()) EmeraldAccent else CoralAccent
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                isSettingsOpen = false
                                isApiKeyDialogOpen = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, GoldAccentBorder),
                            modifier = Modifier.testTag("btn_settings_change_api_key")
                        ) {
                            Text("Change", color = GoldAccent, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }

                    HorizontalDivider(color = BorderDark)

                    // Labs & Data Check Navigation
                    OutlinedButton(
                        onClick = {
                            isSettingsOpen = false
                            navController.navigate(AppRoute.Labs) {
                                launchSingleTop = true
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_settings_open_labs"),
                        border = BorderStroke(1.dp, GoldAccentBorder)
                    ) {
                        Text(
                            text = "Open Labs & Diagnostics",
                            color = GoldAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { isSettingsOpen = false },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldAccent,
                        contentColor = BackgroundDark
                    )
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (isApiKeyDialogOpen) {
        ApiKeyDialog(
            initialKey = customApiKey,
            keySource = currentKeySource,
            onSaveKey = { key -> wordBooksViewModel.setCustomApiKey(key) },
            onResetToDefault = { wordBooksViewModel.clearCustomApiKey() },
            onDismiss = { isApiKeyDialogOpen = false }
        )
    }
}
