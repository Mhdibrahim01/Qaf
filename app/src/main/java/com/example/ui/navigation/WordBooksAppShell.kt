package com.example.ui.navigation

import com.example.data.importer.DataImportRepository

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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import com.example.ui.screens.QuizDialog
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
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
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WordBooksDatabaseScreen
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import com.example.ui.screens.WordDetailedDefinitionSheet
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
@OptIn(ExperimentalMaterial3Api::class)
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
    val selectedDeckId by wordBooksViewModel.selectedDeckId.collectAsStateWithLifecycle()
    val flashcardDeckId by wordBooksViewModel.flashcardDeckId.collectAsStateWithLifecycle()
    val isDeveloperModeEnabled by wordBooksViewModel.isDeveloperModeEnabled.collectAsStateWithLifecycle()
    val themeMode by wordBooksViewModel.themeMode.collectAsStateWithLifecycle()
    val batchTranslateState by wordBooksViewModel.batchTranslateState.collectAsStateWithLifecycle()
    val selectedGrammarLesson by wordBooksViewModel.selectedGrammarLesson.collectAsStateWithLifecycle()
    val selectedReadingText by wordBooksViewModel.selectedReadingText.collectAsStateWithLifecycle()
    val studyPlanProfile by wordBooksViewModel.studyPlanProfile.collectAsStateWithLifecycle()
    val onboardingState by wordBooksViewModel.onboardingState.collectAsStateWithLifecycle()
    val placementQuizState by wordBooksViewModel.placementQuizState.collectAsStateWithLifecycle()
    val userLevel = studyPlanProfile?.level?.ifBlank { "B2" } ?: "B2"
    val activeQuizSession by wordBooksViewModel.activeQuizSession.collectAsStateWithLifecycle()
    val isQuizLoading by wordBooksViewModel.isQuizLoading.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    var isApiKeyDialogOpen by remember { mutableStateOf(false) }
    val apiKeyTrigger by wordBooksViewModel.apiKeyUpdatedTrigger.collectAsStateWithLifecycle()
    val currentKeySource = remember(apiKeyTrigger) { wordBooksViewModel.getApiKeySource() }
    val customApiKey = remember(apiKeyTrigger) { wordBooksViewModel.getCustomApiKey() }
    val currentEngineMode = remember(apiKeyTrigger) { wordBooksViewModel.getEngineMode() }
    val isApiKeyConfigured = remember(apiKeyTrigger) { wordBooksViewModel.isApiKeyConfigured() }

    val resolvedOnboarding = onboardingState
    if (resolvedOnboarding != null && !resolvedOnboarding.onboardingCompleted) {
        com.example.ui.screens.OnboardingScreen(
            onboardingState = resolvedOnboarding,
            importState = importState,
            totalEntriesCount = totalEntriesCount,
            placementQuizState = placementQuizState,
            onCompleteStep1 = wordBooksViewModel::completeOnboardingStep1,
            onSelectLevel = wordBooksViewModel::selectOnboardingLevel,
            onCompleteStep2 = wordBooksViewModel::completeOnboardingStep2,
            onStartPlacementQuiz = wordBooksViewModel::startPlacementQuiz,
            onAnswerPlacementQuestion = wordBooksViewModel::answerPlacementQuizQuestion,
            onRetakePlacementQuiz = wordBooksViewModel::retakePlacementQuiz,
            onUpdateQuizEditableLevel = wordBooksViewModel::updateQuizEditableLevel,
            onPlacementQuizBack = wordBooksViewModel::handlePlacementQuizBack,
            onSelectGoal = wordBooksViewModel::selectOnboardingGoal,
            onSelectMinutesPerDay = wordBooksViewModel::selectOnboardingMinutesPerDay,
            onSelectStudyDaysPerWeek = wordBooksViewModel::selectOnboardingStudyDaysPerWeek,
            onCompleteStep3 = { g, m, d -> wordBooksViewModel.completeOnboardingStep3(g, m, d) },
            onCompleteStep3WithTopicsAndQuietMode = wordBooksViewModel::completeOnboardingStep3,
            onStep3Back = wordBooksViewModel::navigateBackFromStep3,
            onCompleteStep4 = wordBooksViewModel::completeOnboardingStep4,
            onStep4Back = wordBooksViewModel::navigateBackFromStep4,
            initialAllowAi = batchTranslateState.allowAi,
            onCompleteStep5 = wordBooksViewModel::completeOnboardingStep5,
            onStep5Back = wordBooksViewModel::navigateBackFromStep5,
            onSkipOnboarding = wordBooksViewModel::skipOnboarding,
            onRetryImport = wordBooksViewModel::retryDictionaryImportFromOnboarding,
            modifier = modifier
        )
        return
    }

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
        currentDestination?.hasRoute(AppRoute.Settings::class) == true -> AppRoute.Settings
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
                if (currentRoute == AppRoute.Labs) {
                    navController.navigate(AppRoute.Settings) { launchSingleTop = true }
                } else if (currentRoute in listOf(
                        AppRoute.Sentences,
                        AppRoute.Plan,
                        AppRoute.CorrectionNotebook,
                        AppRoute.Settings
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
        AppRoute.Settings -> Triple("Settings", "Audio, Study Goals, Backups & AI", "الإعدادات")
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
                val readingSubtitleContent: (@Composable () -> Unit)? = if (currentRoute == AppRoute.Reading && selectedReadingText != null) {
                    {
                        val text = selectedReadingText!!
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = GoldAccentDim,
                                border = BorderStroke(1.dp, GoldAccentBorder),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = text.level.uppercase(),
                                    color = GoldAccent,
                                    fontSize = 10.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                            val topicDisplay = text.topic.split(" ")
                                .joinToString(" ") { it.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase() else c.toString() } }
                            Text(
                                text = "• $topicDisplay",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                } else null

                WordBooksTopAppBar(
                    title = topTitle,
                    subtitle = topSubtitle,
                    arabicTitle = topArabicTitle,
                    canNavigateBack = canNavigateBack,
                    onNavigateBack = onNavigateBack,
                    subtitleContent = readingSubtitleContent,
                    actions = {
                        IconButton(
                            onClick = { wordBooksViewModel.toggleLightDarkTheme() },
                            modifier = Modifier.testTag("btn_top_theme_toggle")
                        ) {
                            Icon(
                                imageVector = if (com.example.ui.theme.AppThemeState.isLightMode) {
                                    Icons.Filled.DarkMode
                                } else {
                                    Icons.Filled.LightMode
                                },
                                contentDescription = if (com.example.ui.theme.AppThemeState.isLightMode) {
                                    "Switch to Dark Mode"
                                } else {
                                    "Switch to Light Mode"
                                },
                                tint = GoldAccent
                            )
                        }
                        if (currentRoute != AppRoute.Settings) {
                            IconButton(
                                onClick = {
                                    navController.navigate(AppRoute.Settings) { launchSingleTop = true }
                                },
                                modifier = Modifier.testTag("btn_top_settings")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = "Settings",
                                    tint = TextMuted
                                )
                            }
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
                    val reviewDueTodayCount by wordBooksViewModel.reviewDueTodayCount.collectAsStateWithLifecycle()
                    val reviewDueNowCount by wordBooksViewModel.reviewDueNowCount.collectAsStateWithLifecycle()
                    val waitingLaterTodayCount by wordBooksViewModel.waitingLaterTodayCount.collectAsStateWithLifecycle()
                    val nextWaitingDueTimestamp by wordBooksViewModel.nextWaitingDueTimestamp.collectAsStateWithLifecycle()
                    val studiedTodayCount by wordBooksViewModel.studiedTodayCount.collectAsStateWithLifecycle()
                    val masteredCount by wordBooksViewModel.masteredCount.collectAsStateWithLifecycle()
                    val deckStudyStates by wordBooksViewModel.deckStudyStates.collectAsStateWithLifecycle()
                    val todayChecklist by wordBooksViewModel.todayChecklist.collectAsStateWithLifecycle()
                    val monthlyChecklists by wordBooksViewModel.monthlyChecklists.collectAsStateWithLifecycle()
                    val studyStreak by wordBooksViewModel.studyStreak.collectAsStateWithLifecycle()
                    val learnerModelSnapshot by wordBooksViewModel.learnerModelSnapshot.collectAsStateWithLifecycle()
                    HomeScreen(
                        decks = decks,
                        importState = importState,
                        totalEntriesCount = totalEntriesCount,
                        reviewDueTodayCount = reviewDueTodayCount,
                        reviewDueNowCount = reviewDueNowCount,
                        waitingLaterTodayCount = waitingLaterTodayCount,
                        nextWaitingDueTimestamp = nextWaitingDueTimestamp,
                        studiedTodayCount = studiedTodayCount,
                        masteredCount = masteredCount,
                        deckStudyStates = deckStudyStates,
                        todayChecklist = todayChecklist,
                        weeklyChecklists = monthlyChecklists,
                        studyStreak = studyStreak,
                        studyPlanProfile = studyPlanProfile,
                        learnerModelSnapshot = learnerModelSnapshot,
                        onDismissInsight = wordBooksViewModel::dismissAdaptiveInsight,
                        onAcceptLevelUp = wordBooksViewModel::acceptSuggestedLevelUp,
                        onOpenDeck = { deckId ->
                            wordBooksViewModel.selectDeck(deckId.ifBlank { null })
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
                            if (route == "quiz") {
                                wordBooksViewModel.startDailyMiniQuiz()
                            } else {
                                wordBooksViewModel.preparePlanTaskNavigation(route, linkedId)
                                when (route) {
                                    "flashcards", "new_words" -> navController.navigate(AppRoute.Flashcards) { launchSingleTop = true }
                                    "word_books", "database" -> navController.navigate(AppRoute.WordBooks) { launchSingleTop = true }
                                    "reading" -> navController.navigate(AppRoute.Reading) { launchSingleTop = true }
                                    "grammar" -> navController.navigate(AppRoute.Grammar) { launchSingleTop = true }
                                    "correction_notebook" -> navController.navigate(AppRoute.CorrectionNotebook) { launchSingleTop = true }
                                    else -> navController.navigate(AppRoute.Plan) { launchSingleTop = true }
                                }
                            }
                        },
                        onToggleTaskCompleted = wordBooksViewModel::toggleTaskCompleted,
                        onLoadStageWords = wordBooksViewModel::getWordsForLifecycleStage,
                        onSpeakWord = { word -> wordBooksViewModel.playWordVoiceAudio(word) },
                        onOpenDictionaryForStage = { stageKey ->
                            val filterStatus = when (stageKey.lowercase()) {
                                "learning", "familiar" -> "learning"
                                "mature", "conquered" -> "mastered"
                                else -> "all"
                            }
                            wordBooksViewModel.updateStatusFilter(filterStatus)
                            navController.navigate(AppRoute.WordBooks) { launchSingleTop = true }
                        },
                        onStartStudying = { stageKey ->
                            if (stageKey.equals("new", ignoreCase = true)) {
                                wordBooksViewModel.startDailyMiniQuiz()
                            } else {
                                wordBooksViewModel.selectFlashcardDeck(DataImportRepository.DUE_TODAY_DECK_ID)
                                navController.navigate(AppRoute.Flashcards) { launchSingleTop = true }
                            }
                        },
                        onStartEarlyReview = {
                            wordBooksViewModel.startEarlyReviewSession()
                            navController.navigate(AppRoute.Flashcards) { launchSingleTop = true }
                        },
                        onReviewTodayLearnedWords = {
                            wordBooksViewModel.startReviewTodayLearnedWordsSession()
                            navController.navigate(AppRoute.Flashcards) { launchSingleTop = true }
                        },
                        onLearnExtraNewWords = { count ->
                            wordBooksViewModel.startFocusedNewWordsSession(count)
                            navController.navigate(AppRoute.Flashcards) { launchSingleTop = true }
                        }
                    )
                }
                composable<AppRoute.WordBooks> {
                    val filterState by wordBooksViewModel.filterState.collectAsStateWithLifecycle()
                    val pagedEntries = wordBooksViewModel.pagedEntries.collectAsLazyPagingItems()
                    val progressMap by wordBooksViewModel.progressMap.collectAsStateWithLifecycle()
                    val currentlyPlayingWord by wordBooksViewModel.currentlyPlayingWord.collectAsStateWithLifecycle()
                    val detailedCard by wordBooksViewModel.detailedCard.collectAsStateWithLifecycle()
                    val favoriteCount by wordBooksViewModel.favoriteCount.collectAsStateWithLifecycle()
                    val masteredCount by wordBooksViewModel.masteredCount.collectAsStateWithLifecycle()
                    val reviewDueTodayCount by wordBooksViewModel.reviewDueTodayCount.collectAsStateWithLifecycle()
                    val aiStatus by wordBooksViewModel.aiStatus.collectAsStateWithLifecycle()
                    val savedSentenceKeys by wordBooksViewModel.savedSentenceKeys.collectAsStateWithLifecycle()
                    val currentAiWordDetail by wordBooksViewModel.currentAiWordDetail.collectAsStateWithLifecycle()
                    val isAiDetailLoading by wordBooksViewModel.isAiDetailLoading.collectAsStateWithLifecycle()
                    val aiDetailError by wordBooksViewModel.aiDetailError.collectAsStateWithLifecycle()
                    val deckStudyStates by wordBooksViewModel.deckStudyStates.collectAsStateWithLifecycle()
                    val levelProgressSummary by wordBooksViewModel.levelProgressSummary.collectAsStateWithLifecycle()
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
                        onOpenFilteredFlashcards = { deckId, filterState ->
                            wordBooksViewModel.selectFilteredFlashcardDeck(deckId, filterState)
                            navController.navigate(AppRoute.Flashcards) { launchSingleTop = true }
                        },
                        onGetLetterIndex = wordBooksViewModel::getLetterIndex,
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
                        onPlayAudioSlow = { word, url, isGb -> wordBooksViewModel.playAudioSlow(word, url, isGb) },
                        onPlayVoiceAudio = wordBooksViewModel::playWordVoiceAudio,
                        onSpeakSentenceTts = wordBooksViewModel::speakSentenceTts,
                        onReverifyImport = { wordBooksViewModel.startImportIfNeeded(forceRecheck = true) },
                        deckStudyStates = deckStudyStates,
                        onSetCheckpoint = wordBooksViewModel::setDeckCheckpoint,
                        onOpenAllWords = wordBooksViewModel::openAllWords,
                        onOpenMasteredWords = wordBooksViewModel::openMasteredWords,
                        onOpenFavoriteWords = wordBooksViewModel::openFavoriteWords,
                        userLevel = userLevel,
                        levelProgressSummary = levelProgressSummary
                    )
                }
                composable<AppRoute.Flashcards> {
                    val flashcardCards by wordBooksViewModel.flashcardCards.collectAsStateWithLifecycle()
                    val flashcardIndex by wordBooksViewModel.flashcardIndex.collectAsStateWithLifecycle()
                    val isFlashcardsLoading by wordBooksViewModel.isFlashcardsLoading.collectAsStateWithLifecycle()
                    val progressMap by wordBooksViewModel.progressMap.collectAsStateWithLifecycle()
                    val reviewDueTodayCount by wordBooksViewModel.reviewDueTodayCount.collectAsStateWithLifecycle()
                    val studiedTodayCount by wordBooksViewModel.studiedTodayCount.collectAsStateWithLifecycle()
                    val currentlyPlayingWord by wordBooksViewModel.currentlyPlayingWord.collectAsStateWithLifecycle()
                    val detailedCard by wordBooksViewModel.detailedCard.collectAsStateWithLifecycle()
                    val aiStatus by wordBooksViewModel.aiStatus.collectAsStateWithLifecycle()
                    val savedSentenceKeys by wordBooksViewModel.savedSentenceKeys.collectAsStateWithLifecycle()
                    val currentAiWordDetail by wordBooksViewModel.currentAiWordDetail.collectAsStateWithLifecycle()
                    val isAiDetailLoading by wordBooksViewModel.isAiDetailLoading.collectAsStateWithLifecycle()
                    val aiDetailError by wordBooksViewModel.aiDetailError.collectAsStateWithLifecycle()
                    val isCurrentCardFlipped by wordBooksViewModel.isCurrentCardFlipped.collectAsStateWithLifecycle()
                    val flashcardDirection by wordBooksViewModel.flashcardDirection.collectAsStateWithLifecycle()
                    val flashcardAutoShowAnswer by wordBooksViewModel.flashcardAutoShowAnswer.collectAsStateWithLifecycle()
                    val waitingLaterTodayCount by wordBooksViewModel.waitingLaterTodayCount.collectAsStateWithLifecycle()
                    val nextWaitingDueTimestamp by wordBooksViewModel.nextWaitingDueTimestamp.collectAsStateWithLifecycle()
                    val isFlashcardSessionCompleted by wordBooksViewModel.isFlashcardSessionCompleted.collectAsStateWithLifecycle()
                    val flashcardSessionSummary by wordBooksViewModel.flashcardSessionSummary.collectAsStateWithLifecycle()
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
                        onNavigateHome = {
                            wordBooksViewModel.dismissFlashcardSessionCompletion()
                            navController.navigate(AppRoute.Home) { launchSingleTop = true }
                        },
                        onIndexChanged = wordBooksViewModel::onFlashcardIndexChanged,
                        onFlipCard = {
                            wordBooksViewModel.flipCurrentFlashcard()
                            wordBooksViewModel.dismissCoachTipFlip()
                        },
                        onSetCardFlipped = wordBooksViewModel::setCurrentFlashcardFlipped,
                        onUpdateCardDirection = wordBooksViewModel::updateFlashcardDirection,
                        onUpdateAutoShowAnswer = wordBooksViewModel::updateFlashcardAutoShowAnswer,
                        onRateCard = { rating ->
                            wordBooksViewModel.rateCurrentFlashcard(rating)
                            wordBooksViewModel.dismissCoachTipRate()
                        },
                        onToggleMastered = wordBooksViewModel::toggleCurrentFlashcardMastered,
                        onToggleFavorite = wordBooksViewModel::toggleCurrentFlashcardFavorite,
                        onPlayAudio = wordBooksViewModel::playAudio,
                        onPlayAudioSlow = { word, url -> wordBooksViewModel.playAudioSlow(word, url) },
                        onPlayVoiceAudio = wordBooksViewModel::playWordVoiceAudio,
                        onSpeakSentenceTts = wordBooksViewModel::speakSentenceTts,
                        onOpenDetailedDefinition = wordBooksViewModel::openDetailedDefinition,
                        onDismissDetailedDefinition = wordBooksViewModel::dismissDetailedDefinition,
                        onRetryAiDefinition = wordBooksViewModel::retryAiDefinition,
                        onEnrichWordWithAi = wordBooksViewModel::enrichCurrentWordWithAi,
                        onSaveUserArabic = wordBooksViewModel::saveUserArabicForEntry,
                        onToggleSaveSentence = wordBooksViewModel::toggleSaveExampleSentence,
                        onToggleCardFavoriteInSheet = wordBooksViewModel::toggleFavorite,
                        onToggleCardMasteredInSheet = wordBooksViewModel::toggleMastered,
                        showCoachTipFlip = onboardingState?.coachTipFlipDismissed == false,
                        showCoachTipRate = onboardingState?.coachTipRateDismissed == false,
                        onDismissCoachTipFlip = wordBooksViewModel::dismissCoachTipFlip,
                        onDismissCoachTipRate = wordBooksViewModel::dismissCoachTipRate,
                        waitingLaterTodayCount = waitingLaterTodayCount,
                        nextWaitingDueTimestamp = nextWaitingDueTimestamp,
                        onStartEarlyReview = wordBooksViewModel::startEarlyReviewSession,
                        isSessionCompleted = isFlashcardSessionCompleted,
                        sessionSummary = flashcardSessionSummary,
                        onRestartSession = wordBooksViewModel::restartCurrentFlashcardSession
                    )
                }
                composable<AppRoute.Sentences> {
                    val savedSentences by wordBooksViewModel.savedSentences.collectAsStateWithLifecycle()
                    val savedSentenceKeys by wordBooksViewModel.savedSentenceKeys.collectAsStateWithLifecycle()
                    val dictionarySampleExamples by wordBooksViewModel.dictionarySampleExamples.collectAsStateWithLifecycle()
                    val sentenceSortOrder by wordBooksViewModel.sentenceSortOrder.collectAsStateWithLifecycle()
                    val showSentenceTranslation by wordBooksViewModel.showSentenceTranslation.collectAsStateWithLifecycle()
                    val ttsSpeechRate by wordBooksViewModel.ttsSpeechRate.collectAsStateWithLifecycle()
                    val dictationIgnorePunctuation by wordBooksViewModel.dictationIgnorePunctuation.collectAsStateWithLifecycle()
                    val autoPlayPracticeAudio by wordBooksViewModel.autoPlayPracticeAudio.collectAsStateWithLifecycle()
                    val currentlyPlayingWord by wordBooksViewModel.currentlyPlayingWord.collectAsStateWithLifecycle()
                    val explainingSentenceIds by wordBooksViewModel.explainingSentenceIds.collectAsStateWithLifecycle()
                    val aiStatus by wordBooksViewModel.aiStatus.collectAsStateWithLifecycle()
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
                        quietModeEnabled = studyPlanProfile?.quietModeEnabled ?: false,
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
                        },
                        onRecordSentencePracticeAttempt = wordBooksViewModel::recordSentencePracticeAttempt
                    )
                }
                composable<AppRoute.Grammar> {
                    val grammarLessons by wordBooksViewModel.grammarLessons.collectAsStateWithLifecycle()
                    val grammarProgressList by wordBooksViewModel.grammarProgressList.collectAsStateWithLifecycle()
                    val grammarLessonChatHistory by wordBooksViewModel.grammarLessonChatHistory.collectAsStateWithLifecycle()
                    val grammarSentenceCheckHistory by wordBooksViewModel.grammarSentenceCheckHistory.collectAsStateWithLifecycle()
                    val readingTexts by wordBooksViewModel.readingTexts.collectAsStateWithLifecycle()
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
                        onImportJsonText = { wordBooksViewModel.importGrammarLessonsFromText(it) },
                        userLevel = userLevel
                    )
                }
                composable<AppRoute.Reading> {
                    val readingTexts by wordBooksViewModel.readingTexts.collectAsStateWithLifecycle()
                    val readingProgressList by wordBooksViewModel.readingProgressList.collectAsStateWithLifecycle()
                    val readingTextChatHistory by wordBooksViewModel.readingTextChatHistory.collectAsStateWithLifecycle()
                    val readingOpenQuestionHistory by wordBooksViewModel.readingOpenQuestionHistory.collectAsStateWithLifecycle()
                    val grammarLessons by wordBooksViewModel.grammarLessons.collectAsStateWithLifecycle()
                    val ttsSpeechRate by wordBooksViewModel.ttsSpeechRate.collectAsStateWithLifecycle()
                    val learnerModelSnapshot by wordBooksViewModel.learnerModelSnapshot.collectAsStateWithLifecycle()
                    val detailedCard by wordBooksViewModel.detailedCard.collectAsStateWithLifecycle()
                    val currentlyPlayingWord by wordBooksViewModel.currentlyPlayingWord.collectAsStateWithLifecycle()
                    val aiStatus by wordBooksViewModel.aiStatus.collectAsStateWithLifecycle()
                    val savedSentenceKeys by wordBooksViewModel.savedSentenceKeys.collectAsStateWithLifecycle()
                    val currentAiWordDetail by wordBooksViewModel.currentAiWordDetail.collectAsStateWithLifecycle()
                    val isAiDetailLoading by wordBooksViewModel.isAiDetailLoading.collectAsStateWithLifecycle()
                    val aiDetailError by wordBooksViewModel.aiDetailError.collectAsStateWithLifecycle()
                    val batchTranslateState by wordBooksViewModel.batchTranslateState.collectAsStateWithLifecycle()
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
                        onCreateNewReadingTextWithAi = { level, topic, length, targetWords, targetGrammarLessonId ->
                            wordBooksViewModel.createNewReadingTextWithAi(level, topic, length, targetWords, targetGrammarLessonId)
                        },
                        onSimplifyReadingText = wordBooksViewModel::simplifyReadingText,
                        onAskAiAboutText = wordBooksViewModel::askAiAboutReadingText,
                        onFeedbackOnOpenQuestion = wordBooksViewModel::feedbackOnOpenReadingQuestion,
                        onDeleteText = wordBooksViewModel::deleteReadingText,
                        onSyncAssets = { wordBooksViewModel.syncReadingTextsFromAssets(it) },
                        onImportJsonUris = { uris, cb -> wordBooksViewModel.importReadingTextsFromUris(uris, cb) },
                        onImportJsonText = wordBooksViewModel::importReadingTextsFromText,
                        onSpeakSentence = { wordBooksViewModel.speakSentenceTts(it) },
                        onSpeakSentenceSlow = { wordBooksViewModel.speakSentenceSlow(it) },
                        onStopAudio = { wordBooksViewModel.stopAudio() },
                        ttsSpeechRate = ttsSpeechRate,
                        userLevel = userLevel,
                        onScheduleWordForToday = wordBooksViewModel::scheduleLookedUpWordForToday,
                        storyReadabilityById = learnerModelSnapshot.storyReadabilityById,
                        wordStageMap = learnerModelSnapshot.wordStageByHeadword,
                        onOpenDetailedDefinition = wordBooksViewModel::openDetailedDefinition
                    )

                    if (detailedCard != null) {
                        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                        ModalBottomSheet(
                            onDismissRequest = wordBooksViewModel::dismissDetailedDefinition,
                            sheetState = sheetState,
                            containerColor = SurfaceDark,
                            contentColor = TextPrimary
                        ) {
                            WordDetailedDefinitionSheet(
                                card = detailedCard!!,
                                isPlaying = currentlyPlayingWord == detailedCard!!.text,
                                isAiTranslating = aiStatus.isRunning,
                                savedSentenceKeys = savedSentenceKeys,
                                aiDetail = currentAiWordDetail,
                                isAiDetailLoading = isAiDetailLoading,
                                aiDetailError = aiDetailError,
                                canRequestAi = wordBooksViewModel.canMakeAiRequest(),
                                isAllowAi = batchTranslateState.allowAi,
                                isOnline = wordBooksViewModel.isDeviceOnline(),
                                onDismiss = wordBooksViewModel::dismissDetailedDefinition,
                                onRetryAiDefinition = { wordBooksViewModel.retryAiDefinition(detailedCard!!.text) },
                                onPlayAudio = {
                                    val url = detailedCard!!.posVariants.firstOrNull { it.audioUrl.isNotBlank() }?.audioUrl ?: ""
                                    wordBooksViewModel.playAudio(detailedCard!!.text, url)
                                },
                                onPlayAudioSlow = {
                                    val url = detailedCard!!.posVariants.firstOrNull { it.audioUrl.isNotBlank() }?.audioUrl ?: ""
                                    wordBooksViewModel.playAudioSlow(detailedCard!!.text, url)
                                },
                                onPlayVoiceAudioSlow = { isGb ->
                                    val url = if (!isGb) detailedCard!!.posVariants.firstOrNull { it.audioUrl.isNotBlank() }?.audioUrl ?: "" else ""
                                    wordBooksViewModel.playAudioSlow(detailedCard!!.text, url, isGb)
                                },
                                onPlayVoiceAudio = { isGb ->
                                    val url = if (!isGb) detailedCard!!.posVariants.firstOrNull { it.audioUrl.isNotBlank() }?.audioUrl ?: "" else ""
                                    wordBooksViewModel.playWordVoiceAudio(detailedCard!!.text, url, isGb)
                                },
                                onSpeakSentenceTts = { wordBooksViewModel.speakSentenceTts(it) },
                                onEnrichWithAi = { wordBooksViewModel.enrichCurrentWordWithAi(detailedCard!!.deck, detailedCard!!.text) },
                                onSaveUserArabic = { entryId, userArabic ->
                                    wordBooksViewModel.saveUserArabicForEntry(entryId, detailedCard!!.deck, detailedCard!!.text, userArabic)
                                },
                                onToggleSaveSentence = { en, ar ->
                                    wordBooksViewModel.toggleSaveExampleSentence(en, ar, detailedCard!!.text)
                                },
                                onToggleFavorite = { wordBooksViewModel.toggleFavorite(detailedCard!!.deck, detailedCard!!.text) },
                                onToggleMastered = { wordBooksViewModel.toggleMastered(detailedCard!!.deck, detailedCard!!.text) }
                            )
                        }
                    }
                }
                composable<AppRoute.Labs> {
                    val isBatchTranslateOpen by wordBooksViewModel.isBatchTranslateOpen.collectAsStateWithLifecycle()
                    val aiStatus by wordBooksViewModel.aiStatus.collectAsStateWithLifecycle()
                    val emptyArabicCount by wordBooksViewModel.emptyArabicCount.collectAsStateWithLifecycle()
                    val deckArabicStats by wordBooksViewModel.deckArabicStats.collectAsStateWithLifecycle()
                    val grammarLessons by wordBooksViewModel.grammarLessons.collectAsStateWithLifecycle()
                    val todayChecklist by wordBooksViewModel.todayChecklist.collectAsStateWithLifecycle()
                    val recentAttempts by wordBooksViewModel.recentAttempts.collectAsStateWithLifecycle()
                    val totalAttemptsCount by wordBooksViewModel.totalAttemptsCount.collectAsStateWithLifecycle()
                    val reviewDueTodayCount by wordBooksViewModel.reviewDueTodayCount.collectAsStateWithLifecycle()
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
                            studyPlanProfile = studyPlanProfile,
                            todayChecklist = todayChecklist,
                            recentAttempts = recentAttempts,
                            totalAttemptsCount = totalAttemptsCount,
                            reviewDueTodayCount = reviewDueTodayCount,
                            onSimulateBacklog42 = wordBooksViewModel::simulateReviewBacklog42,
                            onRebuildTodayPlan = wordBooksViewModel::forceRebuildTodayPlan,
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
                    val correctionEntries by wordBooksViewModel.correctionEntries.collectAsStateWithLifecycle()
                    val grammarLessons by wordBooksViewModel.grammarLessons.collectAsStateWithLifecycle()
                    val readingTexts by wordBooksViewModel.readingTexts.collectAsStateWithLifecycle()
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
                    val currentStudyPlan by wordBooksViewModel.currentStudyPlan.collectAsStateWithLifecycle()
                    val todayChecklist by wordBooksViewModel.todayChecklist.collectAsStateWithLifecycle()
                    val studyStreak by wordBooksViewModel.studyStreak.collectAsStateWithLifecycle()
                    val quizHistory by wordBooksViewModel.quizHistory.collectAsStateWithLifecycle()
                    val weakAreas by wordBooksViewModel.weakAreas.collectAsStateWithLifecycle()
                    val monthlyChecklists by wordBooksViewModel.monthlyChecklists.collectAsStateWithLifecycle()
                    val practiceAccuracySummary by wordBooksViewModel.practiceAccuracySummary.collectAsStateWithLifecycle()
                    val learnerModelSnapshot by wordBooksViewModel.learnerModelSnapshot.collectAsStateWithLifecycle()
                    PlanScreen(
                        profile = studyPlanProfile,
                        currentPlan = currentStudyPlan,
                        todayChecklist = todayChecklist,
                        streak = studyStreak,
                        quizHistory = quizHistory,
                        weakAreas = weakAreas,
                        monthlyChecklists = monthlyChecklists,
                        practiceAccuracySummary = practiceAccuracySummary,
                        learnerModelSnapshot = learnerModelSnapshot,
                        onDismissInsight = wordBooksViewModel::dismissAdaptiveInsight,
                        onAcceptLevelUp = wordBooksViewModel::acceptSuggestedLevelUp,
                        onSaveProfile = wordBooksViewModel::savePlanProfile,
                        onGeneratePlanWithAi = wordBooksViewModel::generatePlanWithAi,
                        onGenerateDailyQuiz = wordBooksViewModel::generateDailyQuiz,
                        onGenerateWeeklyQuiz = wordBooksViewModel::generateWeeklyQuiz,
                        onGenerateWeakAreasQuiz = wordBooksViewModel::generateWeakAreasQuiz,
                        onRefreshPlan = wordBooksViewModel::refreshDailyChecklist,
                        onSubmitQuizAnswers = wordBooksViewModel::submitQuizAnswers,
                        onPlayAudio = { text: String -> wordBooksViewModel.speakSentenceTts(text) },
                        onToggleTaskCompleted = wordBooksViewModel::toggleTaskCompleted,
                        onStartDirectDailyQuiz = wordBooksViewModel::startDailyMiniQuiz,
                        onLoadStageWords = wordBooksViewModel::getWordsForLifecycleStage,
                        onOpenDictionaryForStage = { stageKey ->
                            val filterStatus = when (stageKey.lowercase()) {
                                "learning", "familiar" -> "learning"
                                "mature", "conquered" -> "mastered"
                                else -> "all"
                            }
                            wordBooksViewModel.updateStatusFilter(filterStatus)
                            navController.navigate(AppRoute.WordBooks) { launchSingleTop = true }
                        },
                        onStartStudying = { stageKey ->
                            if (stageKey.equals("new", ignoreCase = true)) {
                                wordBooksViewModel.startDailyMiniQuiz()
                            } else {
                                wordBooksViewModel.selectFlashcardDeck(DataImportRepository.DUE_TODAY_DECK_ID)
                                navController.navigate(AppRoute.Flashcards) { launchSingleTop = true }
                            }
                        },
                        onNavigateToTaskRoute = { route: String, linkedId: String ->
                            if (route == "quiz") {
                                wordBooksViewModel.startDailyMiniQuiz()
                            } else {
                                wordBooksViewModel.preparePlanTaskNavigation(route, linkedId)
                                when (route) {
                                    "flashcards", "new_words" -> navController.navigate(AppRoute.Flashcards) { launchSingleTop = true }
                                    "word_books", "database" -> navController.navigate(AppRoute.WordBooks) { launchSingleTop = true }
                                    "grammar" -> navController.navigate(AppRoute.Grammar) { launchSingleTop = true }
                                    "reading" -> navController.navigate(AppRoute.Reading) { launchSingleTop = true }
                                    "sentences" -> navController.navigate(AppRoute.Sentences) { launchSingleTop = true }
                                    "correction_notebook" -> {
                                        navController.navigate(AppRoute.CorrectionNotebook) {
                                            launchSingleTop = true
                                        }
                                    }
                                    else -> navController.navigate(AppRoute.Plan) { launchSingleTop = true }
                                }
                            }
                        },
                        onOpenDictionaryWord = { word ->
                            wordBooksViewModel.openDetailedDefinition("", word)
                        }
                    )
                }
                composable<AppRoute.More> {
                    val grammarLessons by wordBooksViewModel.grammarLessons.collectAsStateWithLifecycle()
                    val readingTexts by wordBooksViewModel.readingTexts.collectAsStateWithLifecycle()
                    val correctionEntries by wordBooksViewModel.correctionEntries.collectAsStateWithLifecycle()
                    val savedSentences by wordBooksViewModel.savedSentences.collectAsStateWithLifecycle()
                    val studyStreak by wordBooksViewModel.studyStreak.collectAsStateWithLifecycle()
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
                        onOpenSettings = {
                            navController.navigate(AppRoute.Settings) { launchSingleTop = true }
                        },
                        onNavigateToLabs = {
                            if (isDeveloperModeEnabled) {
                                navController.navigate(AppRoute.Labs) { launchSingleTop = true }
                            }
                        },
                        onExportBackupToUri = wordBooksViewModel::exportUserBackupToUri,
                        onImportBackupFromUri = wordBooksViewModel::importUserBackupFromUri,
                        grammarLessonsCount = grammarLessons.size,
                        readingTextsCount = readingTexts.size,
                        correctionEntriesCount = correctionEntries.size,
                        savedSentencesCount = savedSentences.size,
                        studyStreak = studyStreak
                    )
                }
                composable<AppRoute.Settings> {
                    val preferredAccentUk by wordBooksViewModel.preferredAccentUk.collectAsStateWithLifecycle()
                    val ttsSpeechRate by wordBooksViewModel.ttsSpeechRate.collectAsStateWithLifecycle()
                    val autoPlayPronunciation by wordBooksViewModel.autoPlayPronunciation.collectAsStateWithLifecycle()
                    SettingsScreen(
                        preferredAccentUk = preferredAccentUk,
                        ttsSpeechRate = ttsSpeechRate,
                        autoPlayPronunciation = autoPlayPronunciation,

                        studyPlanProfile = studyPlanProfile,
                        totalEntriesCount = totalEntriesCount,
                        allowAi = batchTranslateState.allowAi,
                        engineMode = currentEngineMode,
                        apiKeySource = currentKeySource,
                        isApiKeyConfigured = isApiKeyConfigured,
                        isDeveloperModeEnabled = isDeveloperModeEnabled,
                        themeMode = themeMode,
                        onSelectThemeMode = wordBooksViewModel::setThemeMode,
                        onSelectAccentUk = wordBooksViewModel::setPreferredAccentUk,
                        onSelectSpeechRate = wordBooksViewModel::updateTtsSpeechRate,
                        onToggleAutoPlayPronunciation = wordBooksViewModel::setAutoPlayPronunciation,

                        onTestVoicePreview = { sampleText ->
                            wordBooksViewModel.speakSentenceTts(sampleText, ttsSpeechRate)
                        },
                        onTestVoiceSlowPreview = { sampleText ->
                            wordBooksViewModel.speakSentenceSlow(sampleText)
                        },
                        onSavePlanProfile = wordBooksViewModel::savePlanProfile,
                        onNavigateToPlan = {
                            navController.navigate(AppRoute.Plan) { launchSingleTop = true }
                        },
                        onRedoOnboardingSetup = wordBooksViewModel::startRedoOnboardingSetup,
                        onExportBackupToUri = wordBooksViewModel::exportUserBackupToUri,
                        onImportBackupFromUri = wordBooksViewModel::importUserBackupFromUri,
                        onToggleAllowAi = wordBooksViewModel::setBatchAllowAi,
                        onSelectEngineMode = wordBooksViewModel::setEngineMode,
                        onOpenApiKeyDialog = { isApiKeyDialogOpen = true },
                        onSetDeveloperMode = wordBooksViewModel::setDeveloperModeEnabled,
                        onNavigateToLabs = {
                            if (isDeveloperModeEnabled) {
                                navController.navigate(AppRoute.Labs) { launchSingleTop = true }
                            }
                        }
                    )
                }
            }
        }
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

    if (activeQuizSession != null) {
        QuizDialog(
            title = activeQuizSession!!.title,
            quizType = activeQuizSession!!.quizType,
            questions = activeQuizSession!!.questions,
            onPlayAudio = { text -> wordBooksViewModel.speakSentenceTts(text) },
            onQuizCompleted = { answers ->
                coroutineScope.launch {
                    wordBooksViewModel.submitActiveQuiz(answers)
                }
            },
            onOpenDictionaryWord = { word ->
                wordBooksViewModel.openDetailedDefinition("", word)
            },
            scopeInfo = activeQuizSession!!.scopeInfo,
            onDismiss = { wordBooksViewModel.dismissActiveQuiz() }
        )
    }

    if (isQuizLoading) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = SurfaceDark,
            title = {
                Text(
                    text = "Preparing Daily Mini-Quiz...",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = GoldAccent,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "Aligning 10 questions with today's vocabulary, grammar, and reading...",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {}
        )
    }
}
