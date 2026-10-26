package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import java.util.Locale
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.data.ai.AiTaskStatus
import com.example.data.ai.AiWordDetailJson
import com.example.data.ai.AiWordDetailService
import com.example.data.ai.BatchTranslateManager
import com.example.data.ai.BatchTranslateUiState
import com.example.data.ai.GeminiDeckAndTranslationService
import com.example.data.audio.AudioPlayerManager
import com.example.data.importer.DataImportRepository
import com.example.data.importer.FlashcardDirection
import com.example.data.importer.GroupedWordCard
import com.example.data.importer.ImportProgressState
import com.example.data.importer.Sm2Algorithm
import com.example.data.importer.Sm2Rating
import com.example.data.importer.WordFilterState
import com.example.data.importer.WordSortOrder
import com.example.data.grammar.GrammarAiService
import com.example.data.grammar.GrammarRepository
import com.example.data.local.AiHistoryEntity
import com.example.data.local.DeckArabicStats
import com.example.data.local.CorrectionNotebookEntity
import com.example.data.local.DeckEntity
import com.example.data.local.DeckStudyStateEntity
import com.example.data.local.EntryEntity
import com.example.data.local.GrammarExerciseItem
import com.example.data.local.GrammarLessonEntity
import com.example.data.local.GrammarProgressEntity
import com.example.data.local.GrammarSentenceCheckResult
import com.example.data.local.PracticeAccuracySummary
import com.example.data.local.SentenceEntity
import com.example.data.local.WordBooksDatabase
import com.example.data.local.WordProgressEntity
import com.example.data.local.AttemptEntity
import com.example.data.local.DailyChecklistEntity
import com.example.data.local.DailyTaskItem
import com.example.data.local.QuizQuestionItem
import com.example.data.local.QuizResultEntity
import com.example.data.local.StudyPlanEntity
import com.example.data.local.StudyPlanProfileEntity
import com.example.data.local.WeakAreasState
import com.example.data.plan.LifecycleStageWordItem
import com.example.data.plan.WordStage
import com.example.data.plan.LearnerModelRepository
import com.example.data.plan.LocalQuizEngine
import com.example.data.plan.StudyPlanAiService
import com.example.data.plan.StudyPlanRepository
import com.example.data.reading.ReadingAiService
import com.example.data.reading.ReadingRepository
import com.example.data.local.ReadingProgressEntity
import com.example.data.local.ReadingQuestionItem
import com.example.data.local.ReadingSentenceItem
import com.example.data.local.ReadingTextEntity
import com.example.data.sentences.SentenceSortOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class LevelProgressSummary(
    val level: String = "B2",
    val isDefaultLevelSelection: Boolean = true,
    val hasRecommendedWords: Boolean = false,
    val totalCount: Int = 0,
    val masteredCount: Int = 0,
    val deckTotalMasteredCount: Int = 0
)

enum class FlashcardSessionType {
    REVIEW,
    LEARN_NEW
}

@Immutable
data class FlashcardSessionSummary(
    val sessionType: FlashcardSessionType = FlashcardSessionType.REVIEW,
    val deckName: String = "",
    val totalCount: Int = 0,
    val masteredCount: Int = 0,
    val needsReviewCount: Int = 0,
    val qualitySum: Int = 0,
    val failedCards: List<GroupedWordCard> = emptyList(),
    val initialCards: List<GroupedWordCard> = emptyList()
) {
    val retentionRatePercent: Int
        get() = if (totalCount > 0) {
            (((totalCount - needsReviewCount).coerceAtLeast(0) * 100f) / totalCount).toInt()
        } else 100

    val averageQuality: Float
        get() = if (totalCount > 0) (qualitySum.toFloat() / totalCount) else 0f
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class WordBooksViewModel(application: Application) : AndroidViewModel(application) {

    private val database = WordBooksDatabase.getInstance(application)
    private val repository = DataImportRepository(application, database)
    private val geminiService = GeminiDeckAndTranslationService(
        dao = database.wordBooksDao(),
        apiKeyProvider = { com.example.data.ai.GeminiApiKeyManager.getApiKey(application) },
        apiKeysProvider = { com.example.data.ai.GeminiApiKeyManager.getAllConfiguredKeys(application) }
    )
    val batchTranslateManager = BatchTranslateManager(application, database.wordBooksDao(), geminiService)
    val aiWordDetailService = AiWordDetailService(
        dao = database.wordBooksDao(),
        geminiService = geminiService,
        isAllowAiEnabled = { batchTranslateManager.uiState.value.allowAi },
        isOnline = { batchTranslateManager.isDeviceOnline() },
        canMakeAiRequest = { batchTranslateManager.canMakeAiRequest() },
        recordAiRequest = { batchTranslateManager.recordAiRequest() },
        engineModeProvider = { com.example.data.ai.GeminiApiKeyManager.getEngineMode(application) }
    )
    val grammarRepository = GrammarRepository(application, database.wordBooksDao())
    val grammarAiService = GrammarAiService(
        repository = grammarRepository,
        isAllowAiEnabled = { batchTranslateManager.uiState.value.allowAi },
        isOnline = { batchTranslateManager.isDeviceOnline() },
        apiKeyProvider = { com.example.data.ai.GeminiApiKeyManager.getApiKey(application) },
        apiKeysProvider = { com.example.data.ai.GeminiApiKeyManager.getAllConfiguredKeys(application) }
    )
    val readingRepository = ReadingRepository(application, database.wordBooksDao())
    val readingAiService = ReadingAiService(
        repository = readingRepository,
        isAllowAiEnabled = { batchTranslateManager.uiState.value.allowAi },
        isOnline = { batchTranslateManager.isDeviceOnline() },
        apiKeyProvider = { com.example.data.ai.GeminiApiKeyManager.getApiKey(application) },
        apiKeysProvider = { com.example.data.ai.GeminiApiKeyManager.getAllConfiguredKeys(application) }
    )
    val studyPlanRepository = StudyPlanRepository(application, database.wordBooksDao())
    val localQuizEngine = LocalQuizEngine(database.wordBooksDao(), studyPlanRepository.timeProvider, studyPlanRepository)
    val studyPlanAiService = StudyPlanAiService(
        repository = studyPlanRepository,
        dao = database.wordBooksDao(),
        isAllowAiEnabled = { batchTranslateManager.uiState.value.allowAi },
        isOnline = { batchTranslateManager.isDeviceOnline() },
        timeProvider = studyPlanRepository.timeProvider,
        apiKeyProvider = { com.example.data.ai.GeminiApiKeyManager.getApiKey(application) },
        apiKeysProvider = { com.example.data.ai.GeminiApiKeyManager.getAllConfiguredKeys(application) }
    )
    private val audioPlayer = AudioPlayerManager(application)
    val onboardingRepository = com.example.data.onboarding.OnboardingPreferencesRepository(application)

    val onboardingState: StateFlow<com.example.data.onboarding.OnboardingPreferencesState?> =
        onboardingRepository.stateFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    private val _placementQuizState = MutableStateFlow(com.example.ui.screens.PlacementQuizUiState())
    val placementQuizState: StateFlow<com.example.ui.screens.PlacementQuizUiState> = _placementQuizState.asStateFlow()

    private val _isGrammarImporting = MutableStateFlow(true)
    val isGrammarImporting: StateFlow<Boolean> = _isGrammarImporting.asStateFlow()

    val importState: StateFlow<ImportProgressState> = repository.importState
    val aiStatus: StateFlow<AiTaskStatus> = geminiService.status
    val batchTranslateState: StateFlow<BatchTranslateUiState> = batchTranslateManager.uiState
    val currentlyPlayingWord: StateFlow<String?> = audioPlayer.currentlyPlayingWord
    val isPlayingSlow: StateFlow<Boolean> = audioPlayer.isPlayingSlow

    private val _isBatchTranslateOpen = MutableStateFlow(false)
    val isBatchTranslateOpen: StateFlow<Boolean> = _isBatchTranslateOpen.asStateFlow()

    // --- GRAMMAR STATE ---
    val grammarLessons: StateFlow<List<GrammarLessonEntity>> = grammarRepository.lessonsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val grammarProgressList: StateFlow<List<GrammarProgressEntity>> = grammarRepository.progressFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedGrammarLesson = MutableStateFlow<GrammarLessonEntity?>(null)
    val selectedGrammarLesson: StateFlow<GrammarLessonEntity?> = _selectedGrammarLesson.asStateFlow()

    val grammarLessonChatHistory: StateFlow<List<AiHistoryEntity>> = _selectedGrammarLesson
        .flatMapLatest { lesson ->
            if (lesson != null) grammarRepository.observeLessonChat(lesson.id)
            else kotlinx.coroutines.flow.flowOf(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val grammarSentenceCheckHistory: StateFlow<List<AiHistoryEntity>> = _selectedGrammarLesson
        .flatMapLatest { lesson ->
            if (lesson != null) grammarRepository.observeSentenceCheckHistory(lesson.id)
            else kotlinx.coroutines.flow.flowOf(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- READING STATE ---
    val readingTexts: StateFlow<List<ReadingTextEntity>> = readingRepository.textsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val readingProgressList: StateFlow<List<ReadingProgressEntity>> = readingRepository.progressFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedReadingText = MutableStateFlow<ReadingTextEntity?>(null)
    val selectedReadingText: StateFlow<ReadingTextEntity?> = _selectedReadingText.asStateFlow()

    val readingTextChatHistory: StateFlow<List<AiHistoryEntity>> = _selectedReadingText
        .flatMapLatest { text ->
            if (text != null) readingRepository.observeTextChat(text.id)
            else kotlinx.coroutines.flow.flowOf(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val readingOpenQuestionHistory: StateFlow<List<AiHistoryEntity>> = _selectedReadingText
        .flatMapLatest { text ->
            if (text != null) readingRepository.observeOpenQuestionHistory(text.id)
            else kotlinx.coroutines.flow.flowOf(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- CORRECTION NOTEBOOK STATE ---
    val correctionEntries: StateFlow<List<CorrectionNotebookEntity>> = readingRepository.correctionEntriesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val uncheckedCorrectionCount: StateFlow<Int> = readingRepository.uncheckedCorrectionCountFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val decks: StateFlow<List<DeckEntity>> = repository.decksFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val totalEntriesCount: StateFlow<Int> = repository.totalEntriesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val emptyArabicCount: StateFlow<Int> = repository.emptyArabicCountFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val deckArabicStats: StateFlow<List<DeckArabicStats>> = repository.deckArabicStatsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val favoriteCount: StateFlow<Int> = repository.favoriteCountFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val masteredCount: StateFlow<Int> = repository.masteredCountFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    private fun parseSelectedDecks(profile: StudyPlanProfileEntity?): List<String> {
        val raw = profile?.selectedDecksJson.orEmpty()
        if (raw.isBlank()) return emptyList()
        return try {
            kotlinx.serialization.json.Json.decodeFromString<List<String>>(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    val reviewDueTodayCount: StateFlow<Int> = studyPlanRepository.profileFlow
        .flatMapLatest { profile ->
            val decks = parseSelectedDecks(profile)
            repository.observeReviewDueTodayCount(selectedDecks = decks)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val reviewDueNowCount: StateFlow<Int> = studyPlanRepository.profileFlow
        .flatMapLatest { profile ->
            val decks = parseSelectedDecks(profile)
            repository.observeReviewDueNowCount(selectedDecks = decks)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    private val _waitingLaterTodayCount = MutableStateFlow(0)
    val waitingLaterTodayCount: StateFlow<Int> = _waitingLaterTodayCount.asStateFlow()

    private val _nextWaitingDueTimestamp = MutableStateFlow<Long?>(null)
    val nextWaitingDueTimestamp: StateFlow<Long?> = _nextWaitingDueTimestamp.asStateFlow()

    val studiedTodayCount: StateFlow<Int> = repository.observeStudiedTodayCount().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val newWordsLearnedTodayCount: StateFlow<Int> = repository.observeNewWordsLearnedTodayCount().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val deckStudyStates: StateFlow<List<DeckStudyStateEntity>> = repository.deckStudyStatesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val progressMap: StateFlow<Map<String, WordProgressEntity>> = repository.allProgressFlow
        .map { list -> list.associateBy { it.cardKey } }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    private val _selectedDeckId = MutableStateFlow<String?>(null)
    val selectedDeckId: StateFlow<String?> = _selectedDeckId.asStateFlow()

    private val _filterState = MutableStateFlow(
        WordFilterState(sortOrder = repository.getPreferredWordSortOrder())
    )
    val filterState: StateFlow<WordFilterState> = _filterState.asStateFlow()

    private val _detailedCard = MutableStateFlow<GroupedWordCard?>(null)
    val detailedCard: StateFlow<GroupedWordCard?> = _detailedCard.asStateFlow()

    private val _currentAiWordDetail = MutableStateFlow<AiWordDetailJson?>(null)
    val currentAiWordDetail: StateFlow<AiWordDetailJson?> = _currentAiWordDetail.asStateFlow()

    private val _isAiDetailLoading = MutableStateFlow(false)
    val isAiDetailLoading: StateFlow<Boolean> = _isAiDetailLoading.asStateFlow()

    private val _aiDetailError = MutableStateFlow<String?>(null)
    val aiDetailError: StateFlow<String?> = _aiDetailError.asStateFlow()

    // --- FLASHCARDS STATE ---
    private val _flashcardDeckId = MutableStateFlow("oxford5000")
    val flashcardDeckId: StateFlow<String> = _flashcardDeckId.asStateFlow()

    private val _flashcardCards = MutableStateFlow<List<GroupedWordCard>>(emptyList())
    val flashcardCards: StateFlow<List<GroupedWordCard>> = _flashcardCards.asStateFlow()

    private val _flashcardIndex = MutableStateFlow(0)
    val flashcardIndex: StateFlow<Int> = _flashcardIndex.asStateFlow()

    private val _isFlashcardsLoading = MutableStateFlow(false)
    val isFlashcardsLoading: StateFlow<Boolean> = _isFlashcardsLoading.asStateFlow()

    private val _isCurrentCardFlipped = MutableStateFlow(false)
    val isCurrentCardFlipped: StateFlow<Boolean> = _isCurrentCardFlipped.asStateFlow()

    private var cardDisplayStartTimeMillis: Long = System.currentTimeMillis()

    private val _flashcardDirection = MutableStateFlow(repository.getFlashcardDirection())
    val flashcardDirection: StateFlow<FlashcardDirection> = _flashcardDirection.asStateFlow()

    private val _flashcardAutoShowAnswer = MutableStateFlow(repository.getFlashcardAutoShowAnswer())
    val flashcardAutoShowAnswer: StateFlow<Boolean> = _flashcardAutoShowAnswer.asStateFlow()

    private val _flashcardSessionType = MutableStateFlow(FlashcardSessionType.REVIEW)
    val flashcardSessionType: StateFlow<FlashcardSessionType> = _flashcardSessionType.asStateFlow()

    private val _isFlashcardSessionCompleted = MutableStateFlow(false)
    val isFlashcardSessionCompleted: StateFlow<Boolean> = _isFlashcardSessionCompleted.asStateFlow()

    private val _flashcardSessionSummary = MutableStateFlow(FlashcardSessionSummary())
    val flashcardSessionSummary: StateFlow<FlashcardSessionSummary> = _flashcardSessionSummary.asStateFlow()

    private val sessionRatedCardKeys = mutableSetOf<String>()
    private val sessionFailedCards = mutableListOf<GroupedWordCard>()
    private var sessionMasteredCount = 0
    private var sessionNeedsReviewCount = 0
    private var sessionQualitySum = 0
    private var sessionInitialCards = listOf<GroupedWordCard>()

    // --- SENTENCES STATE ---
    private val _sentenceSortOrder = MutableStateFlow(SentenceSortOrder.NEWEST)
    val sentenceSortOrder: StateFlow<SentenceSortOrder> = _sentenceSortOrder.asStateFlow()

    private val _showSentenceTranslation = MutableStateFlow(true)
    val showSentenceTranslation: StateFlow<Boolean> = _showSentenceTranslation.asStateFlow()

    private val _ttsSpeechRate = MutableStateFlow(0.85f)
    val ttsSpeechRate: StateFlow<Float> = _ttsSpeechRate.asStateFlow()

    private val _preferredAccentUk = MutableStateFlow(false)
    val preferredAccentUk: StateFlow<Boolean> = _preferredAccentUk.asStateFlow()

    private val _autoPlayPronunciation = MutableStateFlow(false)
    val autoPlayPronunciation: StateFlow<Boolean> = _autoPlayPronunciation.asStateFlow()

    private val _isDeveloperModeEnabled = MutableStateFlow(false)
    val isDeveloperModeEnabled: StateFlow<Boolean> = _isDeveloperModeEnabled.asStateFlow()

    private val _themeMode = MutableStateFlow(com.example.ui.theme.AppThemeMode.DARK)
    val themeMode: StateFlow<com.example.ui.theme.AppThemeMode> = _themeMode.asStateFlow()


    private val _dictationIgnorePunctuation = MutableStateFlow(true)
    val dictationIgnorePunctuation: StateFlow<Boolean> = _dictationIgnorePunctuation.asStateFlow()

    private val _autoPlayPracticeAudio = MutableStateFlow(false)
    val autoPlayPracticeAudio: StateFlow<Boolean> = _autoPlayPracticeAudio.asStateFlow()

    private val _explainingSentenceIds = MutableStateFlow<Set<Long>>(emptySet())
    val explainingSentenceIds: StateFlow<Set<Long>> = _explainingSentenceIds.asStateFlow()

    private val _dictionarySampleExamples = MutableStateFlow<List<SentenceEntity>>(emptyList())
    val dictionarySampleExamples: StateFlow<List<SentenceEntity>> = _dictionarySampleExamples.asStateFlow()

    val savedSentences: StateFlow<List<SentenceEntity>> = _sentenceSortOrder
        .flatMapLatest { sortOrder ->
            when (sortOrder) {
                SentenceSortOrder.NEWEST -> repository.sentencesNewestFirstFlow
                SentenceSortOrder.OLDEST -> repository.sentencesOldestFirstFlow
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val savedSentenceKeys: StateFlow<Set<String>> = repository.sentencesNewestFirstFlow
        .map { list -> list.mapTo(HashSet()) { it.englishText.trim().lowercase() } }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    val pagedEntries: Flow<PagingData<EntryEntity>> = _filterState
        .flatMapLatest { filter -> repository.getPagedEntries(filter) }
        .cachedIn(viewModelScope)

    // --- STUDY PLAN STATE ---
    val studyPlanProfile: StateFlow<StudyPlanProfileEntity?> = studyPlanRepository.profileFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val currentStudyPlan: StateFlow<StudyPlanEntity?> = studyPlanRepository.observeActiveStudyPlan().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val _todayChecklist = MutableStateFlow<DailyChecklistEntity?>(null)
    val todayChecklist: StateFlow<DailyChecklistEntity?> = _todayChecklist.asStateFlow()

    private val _studyStreak = MutableStateFlow(0)
    val studyStreak: StateFlow<Int> = _studyStreak.asStateFlow()

    val quizHistory: StateFlow<List<QuizResultEntity>> = database.wordBooksDao().observeAllQuizResults().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _weakAreas = MutableStateFlow(WeakAreasState())
    val weakAreas: StateFlow<WeakAreasState> = _weakAreas.asStateFlow()

    private val _practiceAccuracySummary = MutableStateFlow(PracticeAccuracySummary())
    val practiceAccuracySummary: StateFlow<PracticeAccuracySummary> = _practiceAccuracySummary.asStateFlow()

    private val _learnerModelSnapshot = MutableStateFlow(com.example.data.plan.LearnerModelSnapshot())
    val learnerModelSnapshot: StateFlow<com.example.data.plan.LearnerModelSnapshot> = _learnerModelSnapshot.asStateFlow()

    val monthlyChecklists: StateFlow<List<DailyChecklistEntity>> = database.wordBooksDao().observeChecklistsForMonth(
        studyPlanRepository.timeProvider.currentMonthString()
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val recentAttempts: StateFlow<List<AttemptEntity>> = database.wordBooksDao().observeRecentAttempts(8)
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalAttemptsCount: StateFlow<Int> = database.wordBooksDao().observeTotalAttemptsCount()
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // --- LETTER OFFSETS IN-MEMORY CACHE FOR A-Z JUMP ---
    private val _letterOffsetsCache = MutableStateFlow<Map<Char, Int>>(emptyMap())
    val letterOffsetsCache: StateFlow<Map<Char, Int>> = _letterOffsetsCache.asStateFlow()

    // --- POST-ONBOARDING KICKOFF STARTER SESSION ---
    private val _isKickoffDismissed = MutableStateFlow(false)
    val isKickoffDismissed: StateFlow<Boolean> = _isKickoffDismissed.asStateFlow()

    fun dismissKickoffStarter() {
        _isKickoffDismissed.value = true
    }

    val kickoffWords: StateFlow<List<EntryEntity>> = studyPlanProfile
        .flatMapLatest { profile ->
            kotlinx.coroutines.flow.flow {
                val level = profile?.level.orEmpty()
                val decks = try {
                    if (!profile?.selectedDecksJson.isNullOrBlank()) {
                        kotlinx.serialization.json.Json.decodeFromString<List<String>>(profile.selectedDecksJson)
                    } else emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
                val words = database.wordBooksDao().getKickoffStarterWords(
                    level = level,
                    decks = decks,
                    hasDecks = if (decks.isNotEmpty()) 1 else 0,
                    limit = 5
                )
                emit(words)
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val levelProgressSummary: StateFlow<LevelProgressSummary> = combine(
        studyPlanProfile,
        _selectedDeckId,
        _filterState
    ) { profile, selectedDeck, filter ->
        val displayLevel = if (filter.levelFilter.isNotBlank() && !filter.levelFilter.equals("All", ignoreCase = true)) {
            filter.levelFilter.uppercase()
        } else {
            profile?.level?.ifBlank { "B2" }?.uppercase() ?: "B2"
        }
        val rawDeck = filter.deckId.ifBlank { selectedDeck.orEmpty() }
        val currentDeckId = if (rawDeck == DataImportRepository.SAVED_WORDS_DECK_ID) "" else rawDeck
        val isDefaultLevel = filter.levelFilter.isBlank() || filter.levelFilter.equals("All", ignoreCase = true)
        Triple(currentDeckId, displayLevel, isDefaultLevel)
    }.flatMapLatest { (currentDeckId, displayLevel, isDefaultLevel) ->
        database.wordBooksDao().observeLevelProgressCounts(
            deckId = currentDeckId,
            level = displayLevel
        ).map { counts ->
            LevelProgressSummary(
                level = displayLevel,
                isDefaultLevelSelection = isDefaultLevel,
                hasRecommendedWords = counts.totalCount > 0,
                totalCount = counts.totalCount,
                masteredCount = counts.masteredCount,
                deckTotalMasteredCount = counts.deckTotalMasteredCount
            )
        }
    }.flowOn(Dispatchers.IO).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LevelProgressSummary()
    )

    fun startKickoffStarterSession(starterDeckId: String? = null) {
        val targetDeck = starterDeckId?.ifBlank { null }
            ?: studyPlanProfile.value?.let { profile ->
                try {
                    if (!profile.selectedDecksJson.isBlank()) {
                        kotlinx.serialization.json.Json.decodeFromString<List<String>>(profile.selectedDecksJson).firstOrNull()
                    } else null
                } catch (e: Exception) {
                    null
                }
            } ?: "oxford5000"
        selectFlashcardDeck(targetDeck)
    }

    init {
        startImportIfNeeded()
        // 1. Initial study plan load if profile already exists or onboarding already completed
        viewModelScope.launch {
            val existingProfile = database.wordBooksDao().getStudyPlanProfile()
            val onboardingSnap = onboardingRepository.getSnapshot()
            _isDeveloperModeEnabled.value = onboardingSnap.isDeveloperModeEnabled
            _themeMode.value = com.example.ui.theme.AppThemeMode.fromId(onboardingSnap.themeMode)
            com.example.ui.theme.AppThemeState.themeMode = _themeMode.value
            _preferredAccentUk.value = onboardingSnap.preferredAccentUk
            _autoPlayPronunciation.value = onboardingSnap.autoPlayPronunciation
            _autoPlayPracticeAudio.value = onboardingSnap.autoPlayPronunciation
            _ttsSpeechRate.value = onboardingSnap.ttsSpeechRate
            audioPlayer.setPreferUkAccent(onboardingSnap.preferredAccentUk)
            audioPlayer.setDefaultSpeechRate(onboardingSnap.ttsSpeechRate)
            if (existingProfile != null || onboardingSnap.onboardingCompleted) {
                val checklist = studyPlanRepository.getOrCreateDailyChecklist()
                _todayChecklist.value = checklist
                _studyStreak.value = studyPlanRepository.computeStreak()
                _weakAreas.value = studyPlanRepository.getWeakAreas()
                _learnerModelSnapshot.value = studyPlanRepository.learnerModelRepository.buildSnapshot(existingProfile)
            }
        }

        // 1b. Observe preference updates for developer mode, theme mode, and audio settings
        viewModelScope.launch {
            onboardingRepository.stateFlow.collectLatest { prefs ->
                _isDeveloperModeEnabled.value = prefs.isDeveloperModeEnabled
                val resolvedTheme = com.example.ui.theme.AppThemeMode.fromId(prefs.themeMode)
                _themeMode.value = resolvedTheme
                com.example.ui.theme.AppThemeState.themeMode = resolvedTheme
                _preferredAccentUk.value = prefs.preferredAccentUk
                _autoPlayPronunciation.value = prefs.autoPlayPronunciation
                _ttsSpeechRate.value = prefs.ttsSpeechRate
                audioPlayer.setPreferUkAccent(prefs.preferredAccentUk)
                audioPlayer.setDefaultSpeechRate(prefs.ttsSpeechRate)
            }
        }

        // 2. Debounced (300ms) Room Flow that recomputes completion idempotently from database state
        combine(
            database.wordBooksDao().observeAllWordProgress(),
            database.wordBooksDao().observeAllQuizResults(),
            database.wordBooksDao().observeAllGrammarProgress(),
            database.wordBooksDao().observeAllReadingProgress()
        ) { _, _, _, _ -> System.currentTimeMillis() }
            .debounce(300L)
            .flowOn(Dispatchers.Default)
            .onEach {
                if (database.wordBooksDao().getStudyPlanProfile() != null) {
                    val updated = studyPlanRepository.recomputeTodayChecklistCompletion()
                    if (updated != null) {
                        _todayChecklist.value = updated
                        _studyStreak.value = studyPlanRepository.computeStreak()
                    }
                }
            }
            .launchIn(viewModelScope)

        // 3. Invalidate in-memory letter offset cache on filter change (rebuilt lazily on demand when A-Z jump is opened)
        viewModelScope.launch {
            _filterState.collectLatest {
                clearLetterOffsetsCache()
            }
        }

        // 4. Clear letter offsets cache on import completion; also resume waiting placement quiz if active
        viewModelScope.launch {
            combine(
                repository.importState,
                _isGrammarImporting
            ) { impState, grammarBusy -> impState to grammarBusy }
                .collectLatest { (impState, grammarBusy) ->
                    if (impState.isCompleted) {
                        clearLetterOffsetsCache()
                    }
                    val currentQuiz = _placementQuizState.value
                    if (currentQuiz.isQuizActive && currentQuiz.isWaitingForImport && !grammarBusy) {
                        tryLoadPlacementQuizQuestions()
                    }
                }
        }
    }

    fun startImportIfNeeded(forceRecheck: Boolean = false) {
        viewModelScope.launch {
            com.example.data.ai.AiPromptContract.syncAndSanitizeCacheIfNeeded(
                context = getApplication(),
                dao = database.wordBooksDao()
            )
            repository.ensureImportedOnFirstLaunch(forceRecheck = forceRecheck)
            loadFlashcardsForDeck(_flashcardDeckId.value)
            loadDictionarySampleExamples()
        }
    }

    /**
     * Completes Step 1 (Welcome) and persists `last_completed_step = 1` in DataStore.
     */
    fun completeOnboardingStep1() {
        viewModelScope.launch {
            onboardingRepository.setLastCompletedStep(1)
        }
    }

    /**
     * Updates the selected CEFR level on Step 2 (persisted in DataStore).
     */
    fun selectOnboardingLevel(level: String) {
        viewModelScope.launch {
            onboardingRepository.saveSelectedLevel(level)
        }
    }

    /**
     * Completes Step 2 (Level Selection) with [level] and advances to Step 3 (`last_completed_step = 2`).
     */
    fun completeOnboardingStep2(level: String) {
        viewModelScope.launch {
            val cleanLevel = level.trim().uppercase().ifBlank { "B2" }
            onboardingRepository.saveSelectedLevel(cleanLevel)
            _placementQuizState.value = com.example.ui.screens.PlacementQuizUiState()
            onboardingRepository.setLastCompletedStep(2)
        }
    }

    /**
     * Updates the selected learning goal on Step 3 (persisted in DataStore).
     */
    fun selectOnboardingGoal(goal: String) {
        viewModelScope.launch {
            val snap = onboardingRepository.getSnapshot()
            onboardingRepository.saveGoalAndTime(
                goal = goal.trim().ifBlank { "general_fluency" },
                minutesPerDay = snap.selectedMinutesPerDay,
                studyDaysPerWeek = snap.selectedStudyDaysPerWeek
            )
        }
    }

    /**
     * Updates the selected daily minutes commitment on Step 3 (persisted in DataStore).
     */
    fun selectOnboardingMinutesPerDay(minutesPerDay: Int) {
        viewModelScope.launch {
            val snap = onboardingRepository.getSnapshot()
            onboardingRepository.saveGoalAndTime(
                goal = snap.selectedGoal,
                minutesPerDay = minutesPerDay.coerceIn(15, 60),
                studyDaysPerWeek = snap.selectedStudyDaysPerWeek
            )
        }
    }

    /**
     * Updates the selected weekly study days on Step 3 (persisted in DataStore).
     */
    fun selectOnboardingStudyDaysPerWeek(studyDaysPerWeek: Int) {
        viewModelScope.launch {
            val snap = onboardingRepository.getSnapshot()
            onboardingRepository.saveGoalAndTime(
                goal = snap.selectedGoal,
                minutesPerDay = snap.selectedMinutesPerDay,
                studyDaysPerWeek = studyDaysPerWeek.coerceIn(1, 7)
            )
        }
    }

    /**
     * Completes Step 3 (Goal & Daily Time):
     * - Saves goal, minutesPerDay, and studyDaysPerWeek in DataStore.
     * - Writes/updates `StudyPlanProfileEntity` in Room (`study_plan_profile`).
     * - Scales and saves the monthly plan (`study_plans`) from `default_study_plan_b2.json`.
     * - Generates/updates today's 5-pillar checklist (`daily_checklists`).
     * - Advances onboarding to Step 4 (`last_completed_step = 3`).
     */
    fun completeOnboardingStep3(
        goal: String,
        minutesPerDay: Int,
        studyDaysPerWeek: Int,
        preferredTopics: List<String>? = null,
        quietModeEnabled: Boolean? = null
    ) {
        viewModelScope.launch {
            val snap = onboardingRepository.getSnapshot()
            val cleanLevel = snap.selectedLevel.trim().uppercase().ifBlank { "B2" }
            val cleanGoal = goal.trim().ifBlank { "general_fluency" }
            val cleanMinutes = minutesPerDay.coerceIn(15, 60)
            val cleanDays = studyDaysPerWeek.coerceIn(1, 7)
            val topicsJson = preferredTopics?.let {
                kotlinx.serialization.json.Json.encodeToString(it.take(3))
            }

            onboardingRepository.saveGoalAndTime(
                goal = cleanGoal,
                minutesPerDay = cleanMinutes,
                studyDaysPerWeek = cleanDays,
                preferredTopicsJson = topicsJson,
                quietModeEnabled = quietModeEnabled
            )

            val checklist = studyPlanRepository.saveOnboardingGoalAndGeneratePlan(
                level = cleanLevel,
                goal = cleanGoal,
                minutesPerDay = cleanMinutes,
                studyDaysPerWeek = cleanDays,
                preferredTopics = preferredTopics,
                quietModeEnabled = quietModeEnabled
            )
            _todayChecklist.value = checklist
            _studyStreak.value = studyPlanRepository.computeStreak()
            _weakAreas.value = studyPlanRepository.getWeakAreas()
            _learnerModelSnapshot.value = studyPlanRepository.learnerModelRepository.buildSnapshot()

            onboardingRepository.setLastCompletedStep(3)
        }
    }

    /**
     * Navigates back from Step 3 (Goal & Time) to Step 2 (Level Selection).
     */
    fun navigateBackFromStep3() {
        viewModelScope.launch {
            onboardingRepository.setLastCompletedStep(1)
        }
    }

    /**
     * Completes Step 4 (Deck Selection & Placement):
     * - Saves selectedDecks and startAtUserLevel in DataStore.
     * - Updates StudyPlanProfileEntity.selectedDecksJson in Room.
     * - Re-scales monthly plan and updates today's checklist tasks.
     * - Advances onboarding to Step 5 (last_completed_step = 4).
     */
    fun completeOnboardingStep4(
        selectedDecks: List<String>,
        startAtUserLevel: Boolean
    ) {
        viewModelScope.launch {
            val safeDecks = selectedDecks.ifEmpty { listOf("oxford5000", "oxford_opal") }
            val decksJson = kotlinx.serialization.json.Json.encodeToString(safeDecks)
            onboardingRepository.saveSelectedDecks(decksJson, startAtUserLevel)

            val checklist = studyPlanRepository.saveOnboardingDecksAndPlacement(
                selectedDecks = safeDecks,
                startAtUserLevel = startAtUserLevel
            )
            _todayChecklist.value = checklist
            _weakAreas.value = studyPlanRepository.getWeakAreas()

            onboardingRepository.setLastCompletedStep(4)
        }
    }

    /**
     * Navigates back from Step 4 (Deck Selection) to Step 3 (Goal & Time).
     */
    fun navigateBackFromStep4() {
        viewModelScope.launch {
            onboardingRepository.setLastCompletedStep(2)
        }
    }

    /**
     * Completes Step 5 (AI Features & Launch):
     * - Saves user's allowAi preference.
     * - Marks onboarding as completed in DataStore (`onboarding_completed = true`, `last_completed_step = 5`).
     * - Generates or ensures today's 5-pillar daily checklist is ready for immediate study.
     */
    fun completeOnboardingStep5(allowAi: Boolean) {
        viewModelScope.launch {
            batchTranslateManager.setAllowAi(allowAi)
            val existingProfile = database.wordBooksDao().getStudyPlanProfile()
            if (existingProfile == null) {
                val created = studyPlanRepository.getOrCreateProfile()
                val plan = studyPlanRepository.loadDefaultPlanScaled(created)
                _todayChecklist.value = studyPlanRepository.regenerateChecklistForUpdatedPlan(created, plan)
            } else if (_todayChecklist.value == null) {
                _todayChecklist.value = studyPlanRepository.getOrCreateDailyChecklist()
            }
            _studyStreak.value = studyPlanRepository.computeStreak()
            _weakAreas.value = studyPlanRepository.getWeakAreas()
            onboardingRepository.markOnboardingCompleted()
        }
    }

    /**
     * Navigates back from Step 5 (AI Features) to Step 4 (Decks & Placement).
     */
    fun navigateBackFromStep5() {
        viewModelScope.launch {
            onboardingRepository.setLastCompletedStep(3)
        }
    }

    fun dismissCoachTipFlip() {
        viewModelScope.launch {
            onboardingRepository.dismissCoachTipFlip()
        }
    }

    fun dismissCoachTipRate() {
        viewModelScope.launch {
            onboardingRepository.dismissCoachTipRate()
        }
    }

    fun dismissCoachTipWordTap() {
        viewModelScope.launch {
            onboardingRepository.dismissCoachTipWordTap()
        }
    }

    /**
     * Starts the 10-question local placement quiz on Step 2 ("Not sure").
     * Waits for grammar import and dictionary import (>= 100 entries per level or import completed)
     * without ever inventing fallback questions.
     */
    fun startPlacementQuiz() {
        _placementQuizState.value = com.example.ui.screens.PlacementQuizUiState(
            isQuizActive = true,
            isWaitingForImport = true
        )
        viewModelScope.launch {
            tryLoadPlacementQuizQuestions()
        }
    }

    private suspend fun tryLoadPlacementQuizQuestions() {
        val dao = database.wordBooksDao()
        if (_isGrammarImporting.value) {
            grammarRepository.importGrammarLessonsIfNeeded()
            _isGrammarImporting.value = false
        }
        val isImportDone = repository.importState.value.isCompleted
        val ready = com.example.data.onboarding.OnboardingPlacementQuiz.hasEnoughEntriesForPlacementQuiz(
            dao = dao,
            isImportCompleted = isImportDone
        )
        if (!ready) {
            _placementQuizState.value = _placementQuizState.value.copy(
                isQuizActive = true,
                isWaitingForImport = true
            )
            return
        }
        val questions = com.example.data.onboarding.OnboardingPlacementQuiz.buildPlacementQuizQuestions(dao)
        if (questions.isNullOrEmpty()) {
            _placementQuizState.value = _placementQuizState.value.copy(
                isQuizActive = true,
                isWaitingForImport = true
            )
            return
        }
        _placementQuizState.value = com.example.ui.screens.PlacementQuizUiState(
            isQuizActive = true,
            isWaitingForImport = false,
            questions = questions,
            currentQuestionIndex = 0,
            answersByQuestionId = emptyMap(),
            isQuizFinished = false
        )
    }

    fun answerPlacementQuizQuestion(selectedOption: String) {
        val current = _placementQuizState.value
        val question = current.questions.getOrNull(current.currentQuestionIndex) ?: return
        val updatedAnswers = current.answersByQuestionId + (question.id to selectedOption)

        if (current.currentQuestionIndex + 1 < current.questions.size) {
            _placementQuizState.value = current.copy(
                currentQuestionIndex = current.currentQuestionIndex + 1,
                answersByQuestionId = updatedAnswers
            )
        } else {
            val (suggested, countsByLevel) = com.example.data.onboarding.OnboardingPlacementQuiz.scorePlacementQuiz(
                questions = current.questions,
                answersByQuestionId = updatedAnswers
            )
            _placementQuizState.value = current.copy(
                answersByQuestionId = updatedAnswers,
                isQuizFinished = true,
                suggestedLevel = suggested,
                editableLevel = suggested,
                correctCountByLevel = countsByLevel
            )
            viewModelScope.launch {
                onboardingRepository.saveSelectedLevel(suggested)
            }
        }
    }

    fun retakePlacementQuiz() {
        startPlacementQuiz()
    }

    fun updateQuizEditableLevel(level: String) {
        val cleanLevel = level.trim().uppercase()
        _placementQuizState.value = _placementQuizState.value.copy(editableLevel = cleanLevel)
        viewModelScope.launch {
            onboardingRepository.saveSelectedLevel(cleanLevel)
        }
    }

    fun handlePlacementQuizBack() {
        val current = _placementQuizState.value
        if (!current.isQuizActive) return
        if (!current.isWaitingForImport && !current.isQuizFinished && current.currentQuestionIndex > 0) {
            _placementQuizState.value = current.copy(
                currentQuestionIndex = current.currentQuestionIndex - 1
            )
        } else {
            _placementQuizState.value = com.example.ui.screens.PlacementQuizUiState(isQuizActive = false)
        }
    }

    /**
     * Skips onboarding at any step:
     * - Writes the default profile (B2, general_fluency, 30 min, 6 days, Friday rest) ONLY if no profile exists.
     * - Never overwrites an existing profile and never changes `allowAi`.
     * - Marks `onboarding_completed = true` and `last_completed_step = 5`.
     */
    fun skipOnboarding() {
        viewModelScope.launch {
            val existingProfile = database.wordBooksDao().getStudyPlanProfile()
            if (existingProfile == null) {
                val created = studyPlanRepository.getOrCreateProfile()
                val plan = studyPlanRepository.loadDefaultPlanScaled(created)
                _todayChecklist.value = studyPlanRepository.regenerateChecklistForUpdatedPlan(created, plan)
            } else if (_todayChecklist.value == null) {
                _todayChecklist.value = studyPlanRepository.getOrCreateDailyChecklist()
            }
            _studyStreak.value = studyPlanRepository.computeStreak()
            _weakAreas.value = studyPlanRepository.getWeakAreas()
            _placementQuizState.value = com.example.ui.screens.PlacementQuizUiState()
            onboardingRepository.markOnboardingCompleted()
        }
    }

    /**
     * Idempotent retry for dictionary import during onboarding: upserts by ID without duplicating entries
     * or altering user progress.
     */
    fun retryDictionaryImportFromOnboarding() {
        startImportIfNeeded(forceRecheck = true)
    }

    /**
     * Starts "Redo setup" from Settings without deleting any progress, streaks, saved sentences,
     * notebook entries, checklists, or backups, and without changing `allowAi`.
     */
    fun startRedoOnboardingSetup() {
        viewModelScope.launch {
            val existingProfile = database.wordBooksDao().getStudyPlanProfile()
            _placementQuizState.value = com.example.ui.screens.PlacementQuizUiState()
            onboardingRepository.startRedoSetup(
                existingLevel = existingProfile?.level,
                existingGoal = existingProfile?.goal,
                existingMinutes = existingProfile?.minutesPerDay,
                existingStudyDays = existingProfile?.studyDaysPerWeek
            )
        }
    }

    fun selectFlashcardDeck(deckId: String) {
        _flashcardDeckId.value = deckId
        _isCurrentCardFlipped.value = false
        loadFlashcardsForDeck(deckId)
    }

    fun selectFilteredFlashcardDeck(deckId: String, filterState: WordFilterState) {
        _flashcardDeckId.value = deckId
        _isCurrentCardFlipped.value = false
        viewModelScope.launch {
            _isFlashcardsLoading.value = true
            try {
                val (allCards, _) = repository.loadFlashcardDeckCards(deckId)
                val filteredCards = allCards.filter { card ->
                    val matchesLevel = filterState.levelFilter.isBlank() || filterState.levelFilter.equals("All", ignoreCase = true) ||
                        card.primaryLevel.equals(filterState.levelFilter, ignoreCase = true) ||
                        card.posVariants.any { it.level.equals(filterState.levelFilter, ignoreCase = true) }

                    val matchesPos = if (filterState.posFilter.isBlank()) {
                        true
                    } else {
                        val posTarget = filterState.posFilter.trim().lowercase()
                        card.posVariants.any { variant ->
                            val p = variant.pos.trim().lowercase()
                            when (posTarget) {
                                "noun" -> p in setOf("noun", "n.", "n")
                                "verb" -> p in setOf("verb", "v.", "v", "phrasal verb")
                                "adjective" -> p in setOf("adjective", "adj.", "adj")
                                "adverb" -> p in setOf("adverb", "adv.", "adv")
                                else -> p == posTarget
                            }
                        }
                    }

                    val matchesStatus = when {
                        filterState.onlyFavorites -> card.isFavorite
                        filterState.onlyMastered -> card.isMastered
                        filterState.onlyLearning -> !card.isMastered
                        else -> true
                    }

                    val q = filterState.searchQuery.trim()
                    val matchesQuery = q.isBlank() ||
                        card.text.contains(q, ignoreCase = true) ||
                        card.posVariants.any {
                            it.arabicMeaning.contains(q, ignoreCase = true) ||
                                it.definitionEn.contains(q, ignoreCase = true)
                        }

                    matchesLevel && matchesPos && matchesStatus && matchesQuery
                }
                val rawList = if (filteredCards.isNotEmpty()) filteredCards else allCards
                val sortedCards = if (deckId == DataImportRepository.DUE_TODAY_DECK_ID) rawList else sortGroupedCards(rawList, filterState.sortOrder)
                _flashcardCards.value = sortedCards
                sessionRatedCardKeys.clear()
                sessionFailedCards.clear()
                sessionMasteredCount = 0
                sessionNeedsReviewCount = 0
                sessionQualitySum = 0
                sessionInitialCards = sortedCards
                _flashcardSessionType.value = FlashcardSessionType.REVIEW
                _isFlashcardSessionCompleted.value = false
                val activeDeckTitle = decks.value.find { it.id == deckId }?.let { if (it.nameAr.isNotBlank()) it.nameAr else it.title }
                    ?: if (deckId == DataImportRepository.DUE_TODAY_DECK_ID) "أهم 5000 كلمة أكاديمية وعامة" else deckId
                _flashcardSessionSummary.value = FlashcardSessionSummary(
                    sessionType = FlashcardSessionType.REVIEW,
                    deckName = activeDeckTitle,
                    totalCount = sortedCards.size,
                    initialCards = sortedCards
                )
                _flashcardIndex.value = 0
                _isCurrentCardFlipped.value = false
                cardDisplayStartTimeMillis = System.currentTimeMillis()
            } finally {
                _isFlashcardsLoading.value = false
            }
        }
    }

    fun clearLetterOffsetsCache() {
        _letterOffsetsCache.value = emptyMap()
    }

    suspend fun rebuildLetterOffsetsCache(filter: WordFilterState = _filterState.value) {
        if (filter.sortOrder != WordSortOrder.AZ) {
            _letterOffsetsCache.value = emptyMap()
            return
        }
        val effectiveDeckId = filter.deckId
        val onlyFav = if (filter.onlyFavorites || effectiveDeckId == DataImportRepository.SAVED_WORDS_DECK_ID) 1 else 0
        val onlyMast = if (filter.onlyMastered) 1 else 0
        val onlyLearn = if (filter.onlyLearning) 1 else 0
        val deckArg = if (effectiveDeckId == DataImportRepository.SAVED_WORDS_DECK_ID) "" else effectiveDeckId

        val counts = withContext(Dispatchers.IO) {
            database.wordBooksDao().getGroupedInitialLetterCounts(
                deckId = deckArg,
                searchQuery = filter.searchQuery.trim(),
                typeFilter = filter.typeFilter,
                levelFilter = filter.levelFilter,
                posFilter = filter.posFilter,
                onlyFavorites = onlyFav,
                onlyMastered = onlyMast,
                onlyLearning = onlyLearn
            )
        }

        val offsets = mutableMapOf<Char, Int>()
        var runningOffset = 0
        val groupedByChar = mutableMapOf<Char, Int>()
        for (item in counts) {
            val ch = item.initial.firstOrNull()?.uppercaseChar()
            if (ch != null && ch in 'A'..'Z') {
                groupedByChar[ch] = (groupedByChar[ch] ?: 0) + item.count
            } else {
                runningOffset += item.count
            }
        }

        for (ch in 'A'..'Z') {
            offsets[ch] = runningOffset
            runningOffset += (groupedByChar[ch] ?: 0)
        }

        _letterOffsetsCache.value = offsets
    }

    fun getLetterIndexInstant(letter: Char): Int {
        return _letterOffsetsCache.value[letter.uppercaseChar()] ?: 0
    }

    suspend fun getLetterIndex(deckId: String?, letter: Char): Int {
        val cached = _letterOffsetsCache.value[letter.uppercaseChar()]
        if (cached != null) return cached
        rebuildLetterOffsetsCache(_filterState.value)
        return _letterOffsetsCache.value[letter.uppercaseChar()] ?: 0
    }

    fun loadFlashcardsForDeck(deckId: String) {
        viewModelScope.launch {
            _isFlashcardsLoading.value = true
            try {
                val selectedDecks = parseSelectedDecks(studyPlanProfile.value)
                val (cards, studyState) = repository.loadFlashcardDeckCards(
                    deckId = deckId,
                    selectedDecks = selectedDecks
                )
                val currentSortOrder = _filterState.value.sortOrder
                val sortedCards = if (deckId == DataImportRepository.DUE_TODAY_DECK_ID) cards else sortGroupedCards(cards, currentSortOrder)
                _flashcardCards.value = sortedCards
                sessionRatedCardKeys.clear()
                sessionFailedCards.clear()
                sessionMasteredCount = 0
                sessionNeedsReviewCount = 0
                sessionQualitySum = 0
                sessionInitialCards = sortedCards
                _flashcardSessionType.value = FlashcardSessionType.REVIEW
                _isFlashcardSessionCompleted.value = false
                val activeDeckTitle = decks.value.find { it.id == deckId }?.let { if (it.nameAr.isNotBlank()) it.nameAr else it.title }
                    ?: if (deckId == DataImportRepository.DUE_TODAY_DECK_ID) "أهم 5000 كلمة أكاديمية وعامة" else deckId
                _flashcardSessionSummary.value = FlashcardSessionSummary(
                    sessionType = FlashcardSessionType.REVIEW,
                    deckName = activeDeckTitle,
                    totalCount = sortedCards.size,
                    initialCards = sortedCards
                )
                val restoredIndex = resolveRestoredFlashcardIndex(sortedCards, studyState)
                _flashcardIndex.value = restoredIndex
                _isCurrentCardFlipped.value = false
                cardDisplayStartTimeMillis = System.currentTimeMillis()
                val nowMillis = System.currentTimeMillis()
                _waitingLaterTodayCount.value = repository.getLaterTodayCardsCount(
                    nowMillis = nowMillis,
                    selectedDecks = selectedDecks
                )
                _nextWaitingDueTimestamp.value = repository.getNextLaterTodayDueTimestamp(
                    nowMillis = nowMillis,
                    selectedDecks = selectedDecks
                )
            } finally {
                _isFlashcardsLoading.value = false
            }
        }
    }

    fun onFlashcardIndexChanged(newIndex: Int) {
        val cards = _flashcardCards.value
        if (cards.isEmpty()) return
        val clamped = newIndex.coerceIn(0, cards.size - 1)
        if (_flashcardIndex.value == clamped) return
        _flashcardIndex.value = clamped
        _isCurrentCardFlipped.value = false
        cardDisplayStartTimeMillis = System.currentTimeMillis()
        val currentCard = cards[clamped]
        val deckId = _flashcardDeckId.value
        val sessionType = _flashcardSessionType.value
        if (sessionType != FlashcardSessionType.LEARN_NEW &&
            deckId != DataImportRepository.DUE_TODAY_DECK_ID &&
            deckId != "learned_today"
        ) {
            viewModelScope.launch {
                repository.saveDeckStudyPosition(
                    deckId = deckId,
                    cardIndex = clamped,
                    wordText = currentCard.text,
                    incrementStudied = false
                )
            }
        }
    }

    fun flipCurrentFlashcard() {
        val nextFlipped = !_isCurrentCardFlipped.value
        _isCurrentCardFlipped.value = nextFlipped
        if (nextFlipped && _autoPlayPronunciation.value && studyPlanProfile.value?.quietModeEnabled != true) {
            val currentCard = _flashcardCards.value.getOrNull(_flashcardIndex.value)
            if (currentCard != null) {
                val url = currentCard.posVariants.firstOrNull { it.audioUrl.isNotBlank() }?.audioUrl.orEmpty()
                playWordVoiceAudio(currentCard.text, url, _preferredAccentUk.value)
            }
        }
    }

    fun setCurrentFlashcardFlipped(flipped: Boolean) {
        val changedToFlipped = flipped && !_isCurrentCardFlipped.value
        _isCurrentCardFlipped.value = flipped
        if (changedToFlipped && _autoPlayPronunciation.value && studyPlanProfile.value?.quietModeEnabled != true) {
            val currentCard = _flashcardCards.value.getOrNull(_flashcardIndex.value)
            if (currentCard != null) {
                val url = currentCard.posVariants.firstOrNull { it.audioUrl.isNotBlank() }?.audioUrl.orEmpty()
                playWordVoiceAudio(currentCard.text, url, _preferredAccentUk.value)
            }
        }
    }

    fun updateFlashcardDirection(direction: FlashcardDirection) {
        _flashcardDirection.value = direction
        repository.setFlashcardDirection(direction)
        _isCurrentCardFlipped.value = false
    }

    fun updateFlashcardAutoShowAnswer(enabled: Boolean) {
        _flashcardAutoShowAnswer.value = enabled
        repository.setFlashcardAutoShowAnswer(enabled)
    }

    fun rateCurrentFlashcard(rating: Sm2Rating) {
        val cards = _flashcardCards.value
        val currentIndex = _flashcardIndex.value
        val currentCard = cards.getOrNull(currentIndex) ?: return
        val activeDeckId = _flashcardDeckId.value
        val nowMillis = System.currentTimeMillis()

        // Compute optimistic updated SM-2 progress FIRST so Again (+10m) and first-Hard (+4h)
        // are immediately excluded from due-now eligibility until their dueTimestamp is reached.
        val existingProgress = progressMap.value[currentCard.cardKey] ?: WordProgressEntity(
            cardKey = currentCard.cardKey,
            deck = currentCard.deck,
            text = currentCard.text.trim().lowercase()
        )
        val baseOptimistic = Sm2Algorithm.applyRating(
            progress = existingProgress,
            rating = rating,
            nowMillis = nowMillis
        )
        val optimisticUpdated = if (rating == Sm2Rating.EASY) {
            val matureInterval = maxOf(baseOptimistic.intervalDays, com.example.data.plan.AdaptiveConfig.STAGE_MATURE_MIN_INTERVAL_DAYS)
            val conqueredTime = when {
                baseOptimistic.againCount < com.example.data.plan.AdaptiveConfig.CONQUERED_MIN_LAPSES -> 0L
                baseOptimistic.conqueredTimestamp > 0L -> baseOptimistic.conqueredTimestamp
                else -> nowMillis
            }
            baseOptimistic.copy(
                isMastered = true,
                intervalDays = matureInterval,
                dueTimestamp = nowMillis + matureInterval * Sm2Algorithm.DAY_MILLIS,
                conqueredTimestamp = conqueredTime
            )
        } else {
            baseOptimistic
        }
        val updatedProgressMap = progressMap.value + (currentCard.cardKey to optimisticUpdated)

        val updatedCards = if (rating == Sm2Rating.EASY) {
            cards.mapIndexed { idx, c ->
                if (idx == currentIndex) c.copy(isMastered = true) else c
            }
        } else {
            cards
        }

        val remainingCards: List<GroupedWordCard>
        val nextIndex: Int
        if (activeDeckId == DataImportRepository.DUE_TODAY_DECK_ID && (optimisticUpdated.dueTimestamp > nowMillis || optimisticUpdated.isMastered)) {
            // Hide rated card from active due-session until dueTimestamp <= nowMillis
            remainingCards = updatedCards.filterIndexed { index, _ -> index != currentIndex }
            nextIndex = if (remainingCards.isEmpty()) {
                0
            } else {
                val candidateStart = (currentIndex - 1).coerceAtLeast(-1)
                repository.findNextDueCardIndex(
                    cards = remainingCards,
                    currentIndex = candidateStart.coerceAtLeast(0),
                    progressMap = updatedProgressMap,
                    nowMillis = nowMillis
                ).coerceIn(0, remainingCards.size - 1)
            }
            _flashcardCards.value = remainingCards
        } else {
            remainingCards = updatedCards
            _flashcardCards.value = updatedCards
            nextIndex = repository.findNextDueCardIndex(
                cards = updatedCards,
                currentIndex = currentIndex,
                progressMap = updatedProgressMap,
                nowMillis = nowMillis
            )
        }
        val nextWordText = remainingCards.getOrNull(nextIndex)?.text ?: currentCard.text

        val elapsedMs = (nowMillis - cardDisplayStartTimeMillis).coerceIn(100L, 300_000L)
        cardDisplayStartTimeMillis = nowMillis

        _isCurrentCardFlipped.value = false
        _flashcardIndex.value = nextIndex

        sessionRatedCardKeys.add(currentCard.cardKey)
        val quality = when (rating) {
            Sm2Rating.AGAIN -> 0
            Sm2Rating.HARD -> 2
            Sm2Rating.GOOD -> 3
            Sm2Rating.EASY -> 5
        }
        sessionQualitySum += quality
        if (rating == Sm2Rating.EASY || rating == Sm2Rating.GOOD) {
            sessionMasteredCount++
        } else {
            sessionNeedsReviewCount++
            if (!sessionFailedCards.any { it.cardKey == currentCard.cardKey }) {
                sessionFailedCards.add(currentCard)
            }
        }

        val totalSessionCards = if (sessionInitialCards.isNotEmpty()) sessionInitialCards.size else cards.size
        val isDueSessionFinished = (activeDeckId == DataImportRepository.DUE_TODAY_DECK_ID && remainingCards.isEmpty())
        val isAllRated = sessionRatedCardKeys.size >= totalSessionCards
        val isEndOfDeck = (activeDeckId != DataImportRepository.DUE_TODAY_DECK_ID && (currentIndex >= cards.size - 1 || nextIndex <= currentIndex))

        val isSessionDone = isDueSessionFinished || isAllRated || (isEndOfDeck && sessionRatedCardKeys.size >= minOf(5, totalSessionCards))

        if (isSessionDone) {
            val deckEntity = decks.value.find { it.id == activeDeckId }
            val finalDeckName = when {
                deckEntity != null && deckEntity.nameAr.isNotBlank() -> deckEntity.nameAr
                deckEntity != null && deckEntity.title.isNotBlank() -> deckEntity.title
                activeDeckId == DataImportRepository.DUE_TODAY_DECK_ID -> "أهم 5000 كلمة أكاديمية وعامة"
                else -> activeDeckId
            }

            _flashcardSessionSummary.value = FlashcardSessionSummary(
                sessionType = _flashcardSessionType.value,
                deckName = finalDeckName,
                totalCount = maxOf(sessionRatedCardKeys.size, totalSessionCards),
                masteredCount = sessionMasteredCount,
                needsReviewCount = sessionNeedsReviewCount,
                qualitySum = sessionQualitySum,
                failedCards = sessionFailedCards.toList(),
                initialCards = if (sessionInitialCards.isNotEmpty()) sessionInitialCards else cards
            )
            _isFlashcardSessionCompleted.value = true
        }

        viewModelScope.launch {
            repository.applySm2Review(
                deckId = currentCard.deck,
                wordText = currentCard.text,
                rating = rating,
                nowMillis = nowMillis,
                elapsedMs = elapsedMs
            )
            if (rating == Sm2Rating.EASY) {
                refreshDetailedCardIfOpen(currentCard.deck, currentCard.text)
                _learnerModelSnapshot.value = studyPlanRepository.learnerModelRepository.buildSnapshot(studyPlanProfile.value)
            }
            if (_flashcardSessionType.value != FlashcardSessionType.LEARN_NEW &&
                activeDeckId != DataImportRepository.DUE_TODAY_DECK_ID &&
                activeDeckId != "learned_today"
            ) {
                repository.saveDeckStudyPosition(
                    deckId = activeDeckId,
                    cardIndex = nextIndex,
                    wordText = nextWordText,
                    incrementStudied = true
                )
            }
            val selectedDecks = parseSelectedDecks(studyPlanProfile.value)
            _waitingLaterTodayCount.value = repository.getLaterTodayCardsCount(
                nowMillis = nowMillis,
                selectedDecks = selectedDecks
            )
            _nextWaitingDueTimestamp.value = repository.getNextLaterTodayDueTimestamp(
                nowMillis = nowMillis,
                selectedDecks = selectedDecks
            )
            refreshDailyChecklist()
        }
    }

    fun restartCurrentFlashcardSession() {
        val summary = _flashcardSessionSummary.value
        val retryCards = if (summary.failedCards.isNotEmpty()) {
            summary.failedCards
        } else {
            summary.initialCards.ifEmpty { _flashcardCards.value }
        }
        if (retryCards.isNotEmpty()) {
            sessionRatedCardKeys.clear()
            sessionFailedCards.clear()
            sessionMasteredCount = 0
            sessionNeedsReviewCount = 0
            sessionQualitySum = 0
            sessionInitialCards = retryCards
            _flashcardCards.value = retryCards
            _flashcardIndex.value = 0
            _isCurrentCardFlipped.value = false
            _isFlashcardSessionCompleted.value = false
        }
    }

    fun dismissFlashcardSessionCompletion() {
        _isFlashcardSessionCompleted.value = false
        _isCurrentCardFlipped.value = false
        refreshDailyChecklist()
    }

    fun toggleCurrentFlashcardMastered() {
        val cards = _flashcardCards.value
        val currentIndex = _flashcardIndex.value
        val currentCard = cards.getOrNull(currentIndex) ?: return
        viewModelScope.launch {
            repository.toggleMastered(currentCard.deck, currentCard.text)
            val updatedCard = currentCard.copy(isMastered = !currentCard.isMastered)
            _flashcardCards.update { list ->
                list.toMutableList().apply {
                    if (currentIndex in indices) {
                        this[currentIndex] = updatedCard
                    }
                }
            }
            refreshDetailedCardIfOpen(currentCard.deck, currentCard.text)
            _learnerModelSnapshot.value = studyPlanRepository.learnerModelRepository.buildSnapshot(studyPlanProfile.value)
        }
    }

    fun toggleCurrentFlashcardFavorite() {
        val cards = _flashcardCards.value
        val currentIndex = _flashcardIndex.value
        val currentCard = cards.getOrNull(currentIndex) ?: return
        viewModelScope.launch {
            repository.toggleFavorite(currentCard.deck, currentCard.text)
            val updatedCard = currentCard.copy(isFavorite = !currentCard.isFavorite)
            _flashcardCards.update { list ->
                list.toMutableList().apply {
                    if (currentIndex in indices) {
                        this[currentIndex] = updatedCard
                    }
                }
            }
            refreshDetailedCardIfOpen(currentCard.deck, currentCard.text)
        }
    }

    fun clearAndReimportDatabase() {
        viewModelScope.launch {
            clearLetterOffsetsCache()
            repository.clearAndReimport()
            loadFlashcardsForDeck(_flashcardDeckId.value)
            rebuildLetterOffsetsCache(_filterState.value)
        }
    }

    fun importJsonUris(uris: List<Uri>) {
        viewModelScope.launch {
            clearLetterOffsetsCache()
            repository.importFromContentUris(uris)
            loadFlashcardsForDeck(_flashcardDeckId.value)
            rebuildLetterOffsetsCache(_filterState.value)
        }
    }

    fun openBatchTranslateScreen() {
        viewModelScope.launch {
            batchTranslateManager.refreshDeckCounts()
        }
        _isBatchTranslateOpen.value = true
    }

    fun closeBatchTranslateScreen() {
        _isBatchTranslateOpen.value = false
    }

    fun selectBatchTranslateDeck(deckId: String) {
        batchTranslateManager.selectDeck(deckId)
    }

    fun selectBatchTranslateTargetCount(count: Int) {
        batchTranslateManager.selectTargetCount(count)
    }

    fun startBatchTranslate() {
        batchTranslateManager.startBatchTranslate()
    }

    fun pauseBatchTranslate() {
        batchTranslateManager.pauseBatchTranslate()
    }

    fun resumeBatchTranslate() {
        batchTranslateManager.resumeBatchTranslate()
    }

    fun stopBatchTranslate() {
        batchTranslateManager.stopBatchTranslate()
    }

    fun retryBatchTranslateFailed() {
        batchTranslateManager.retryFailedEntries()
    }

    fun setBatchAllowAi(enabled: Boolean) {
        batchTranslateManager.setAllowAi(enabled)
    }

    fun setBatchDailyLimit(limit: Int) {
        batchTranslateManager.setDailyRequestLimit(limit)
    }

    fun translateEmptyArabicBatch(batchSize: Int = 25, deckFilter: String = "") {
        batchTranslateManager.startBatchTranslate(deckFilter = deckFilter, targetCount = batchSize)
    }

    fun generateMissingDeckBatch(deckName: String, count: Int = 25) {
        viewModelScope.launch {
            geminiService.generateDeckBatch(
                deckName = deckName,
                count = count,
                onDecksUpdated = { repository.refreshAllDeckCounts() }
            )
            if (_flashcardDeckId.value == deckName) {
                loadFlashcardsForDeck(deckName)
            }
        }
    }

    fun enrichCurrentWordWithAi(deckId: String, wordText: String) {
        viewModelScope.launch {
            geminiService.enrichSingleWord(deckId = deckId, wordText = wordText)
            val refreshed = repository.getWordCardWithVariants(deckId, wordText)
            _detailedCard.value = refreshed
            if (refreshed != null) {
                _flashcardCards.update { list ->
                    list.map { if (it.cardKey == refreshed.cardKey) refreshed else it }
                }
            }
        }
    }

    fun saveUserArabicForEntry(entryId: String, deckId: String, wordText: String, userArabic: String) {
        viewModelScope.launch {
            repository.saveUserArabicForEntry(entryId, userArabic)
            val refreshed = repository.getWordCardWithVariants(deckId, wordText)
            _detailedCard.value = refreshed
            if (refreshed != null) {
                _flashcardCards.update { list ->
                    list.map { if (it.cardKey == refreshed.cardKey) refreshed else it }
                }
            }
        }
    }

    fun selectDeck(deckId: String?) {
        _selectedDeckId.value = deckId
        _filterState.update { current ->
            current.copy(
                deckId = deckId ?: "",
                onlyFavorites = deckId == DataImportRepository.SAVED_WORDS_DECK_ID,
                onlyMastered = false,
                onlyLearning = false,
                searchQuery = "",
                typeFilter = "",
                levelFilter = "",
                posFilter = ""
            )
        }
    }

    fun updateDeckFilterInList(deckId: String) {
        _selectedDeckId.value = deckId.ifBlank { null }
        _filterState.update {
            it.copy(
                deckId = deckId,
                onlyFavorites = deckId == DataImportRepository.SAVED_WORDS_DECK_ID
            )
        }
    }

    fun updateSearchQuery(query: String) {
        _filterState.update { it.copy(searchQuery = query) }
    }

    fun updateTypeFilter(type: String) {
        _filterState.update { current ->
            current.copy(typeFilter = if (current.typeFilter == type) "" else type)
        }
    }

    fun updateLevelFilter(level: String) {
        _filterState.update { current ->
            current.copy(levelFilter = if (current.levelFilter == level) "" else level)
        }
    }

    fun updatePosFilter(pos: String) {
        _filterState.update { current ->
            current.copy(posFilter = if (current.posFilter == pos) "" else pos)
        }
    }

    fun updateSortOrder(sortOrder: WordSortOrder) {
        repository.setPreferredWordSortOrder(sortOrder)
        _filterState.update { it.copy(sortOrder = sortOrder) }
    }

    fun clearAllFilters() {
        val currentDeck = _selectedDeckId.value ?: ""
        val currentSort = _filterState.value.sortOrder
        _filterState.update {
            WordFilterState(
                deckId = currentDeck,
                sortOrder = currentSort,
                onlyFavorites = currentDeck == DataImportRepository.SAVED_WORDS_DECK_ID
            )
        }
    }

    fun toggleFavorite(deckId: String, wordText: String) {
        viewModelScope.launch {
            repository.toggleFavorite(deckId, wordText)
            refreshDetailedCardIfOpen(deckId, wordText)
        }
    }

    fun toggleMastered(deckId: String, wordText: String) {
        viewModelScope.launch {
            repository.toggleMastered(deckId, wordText)
            refreshDetailedCardIfOpen(deckId, wordText)
            _learnerModelSnapshot.value = studyPlanRepository.learnerModelRepository.buildSnapshot(studyPlanProfile.value)
        }
    }

    fun openDetailedDefinition(deckId: String, wordText: String, cardIndex: Int = -1) {
        viewModelScope.launch {
            readingRepository.recordWordLookup(wordText, deckId)
            var card = repository.getWordCardWithVariants(deckId, wordText)
            if (card == null && deckId.isNotBlank()) {
                // Fallback across all decks
                card = repository.getWordCardWithVariants("", wordText)
            }
            if (card == null) {
                // Fallback via readingRepository local lookup which resolves inflections & lemmas
                val localMatches = readingRepository.lookupWordLocally(wordText)
                val primaryMatch = localMatches.firstOrNull()
                if (primaryMatch != null) {
                    card = repository.getWordCardWithVariants(primaryMatch.deck, primaryMatch.text)
                }
            }
            _detailedCard.value = card
            val targetDeck = card?.deck ?: deckId
            val targetWord = card?.text ?: wordText
            if (card != null && _autoPlayPronunciation.value && studyPlanProfile.value?.quietModeEnabled != true) {
                val url = card.posVariants.firstOrNull { it.audioUrl.isNotBlank() }?.audioUrl.orEmpty()
                playWordVoiceAudio(card.text, url, _preferredAccentUk.value)
            }
            loadAiDefinitionForWord(targetWord, forceFetch = false)
            if (cardIndex >= 0 && targetDeck.isNotBlank() && targetWord.isNotBlank()) {
                repository.saveDeckStudyPosition(
                    deckId = targetDeck,
                    cardIndex = cardIndex,
                    wordText = targetWord,
                    incrementStudied = false
                )
            }
            _learnerModelSnapshot.value = studyPlanRepository.learnerModelRepository.buildSnapshot(studyPlanProfile.value)
        }
    }

    fun setDeckCheckpoint(deckId: String, wordText: String, cardIndex: Int = 0) {
        if (deckId.isBlank() || wordText.isBlank()) return
        viewModelScope.launch {
            repository.saveDeckStudyPosition(
                deckId = deckId,
                cardIndex = cardIndex,
                wordText = wordText,
                incrementStudied = false
            )
        }
    }

    fun openMasteredWords() {
        _selectedDeckId.value = ""
        _filterState.update { current ->
            current.copy(
                deckId = "",
                onlyFavorites = false,
                onlyMastered = true,
                onlyLearning = false,
                searchQuery = "",
                typeFilter = "",
                levelFilter = "",
                posFilter = ""
            )
        }
    }

    fun openFavoriteWords() {
        _selectedDeckId.value = DataImportRepository.SAVED_WORDS_DECK_ID
        _filterState.update { current ->
            current.copy(
                deckId = DataImportRepository.SAVED_WORDS_DECK_ID,
                onlyFavorites = true,
                onlyMastered = false,
                onlyLearning = false,
                searchQuery = "",
                typeFilter = "",
                levelFilter = "",
                posFilter = ""
            )
        }
    }

    fun openAllWords() {
        _selectedDeckId.value = ""
        _filterState.update { current ->
            current.copy(
                deckId = "",
                onlyFavorites = false,
                onlyMastered = false,
                onlyLearning = false,
                searchQuery = "",
                typeFilter = "",
                levelFilter = "",
                posFilter = ""
            )
        }
    }

    fun loadAiDefinitionForWord(wordText: String, forceFetch: Boolean = false) {
        val cleanWord = wordText.trim()
        if (cleanWord.isEmpty()) return
        viewModelScope.launch {
            // Check local Room cache first (instant, saved forever, works offline)
            val saved = aiWordDetailService.getSavedDetailOnly(cleanWord)
            if (saved != null && !forceFetch) {
                _currentAiWordDetail.value = saved
                _isAiDetailLoading.value = false
                _aiDetailError.value = null
                return@launch
            }

            // If not saved and allowAi is off, or offline, or limit reached:
            // don't auto-fetch, leave null so local data is shown with disabled button
            if (!batchTranslateManager.canMakeAiRequest() && !forceFetch) {
                _currentAiWordDetail.value = null
                _isAiDetailLoading.value = false
                _aiDetailError.value = null
                return@launch
            }

            _isAiDetailLoading.value = true
            _aiDetailError.value = null
            val result = aiWordDetailService.getOrFetchWordDetail(cleanWord, forceFetch = forceFetch)
            result.fold(
                onSuccess = { detail ->
                    _currentAiWordDetail.value = detail
                    _isAiDetailLoading.value = false
                    _aiDetailError.value = null
                    _detailedCard.value?.let { openCard ->
                        refreshDetailedCardIfOpen(openCard.deck, openCard.text)
                    }
                },
                onFailure = { err ->
                    _isAiDetailLoading.value = false
                    _aiDetailError.value = err.message ?: "Failed to generate AI definition"
                }
            )
        }
    }

    fun retryAiDefinition(wordText: String) {
        if (!batchTranslateManager.uiState.value.allowAi) {
            batchTranslateManager.setAllowAi(true)
        }
        loadAiDefinitionForWord(wordText, forceFetch = true)
    }

    fun dismissDetailedDefinition() {
        _detailedCard.value = null
        _currentAiWordDetail.value = null
        _isAiDetailLoading.value = false
        _aiDetailError.value = null
    }

    fun selectWordListTab(isMasteredOnly: Boolean) {
        _filterState.update {
            it.copy(
                onlyMastered = isMasteredOnly,
                onlyLearning = false,
                onlyFavorites = !isMasteredOnly && (it.deckId == DataImportRepository.SAVED_WORDS_DECK_ID)
            )
        }
    }

    fun updateStatusFilter(status: String) {
        _filterState.update { current ->
            when (status.lowercase()) {
                "learning" -> current.copy(
                    onlyLearning = true,
                    onlyMastered = false,
                    onlyFavorites = false
                )
                "mastered" -> current.copy(
                    onlyLearning = false,
                    onlyMastered = true,
                    onlyFavorites = false
                )
                "favorites" -> current.copy(
                    onlyLearning = false,
                    onlyMastered = false,
                    onlyFavorites = true
                )
                else -> current.copy(
                    onlyLearning = false,
                    onlyMastered = false,
                    onlyFavorites = current.deckId == DataImportRepository.SAVED_WORDS_DECK_ID
                )
            }
        }
    }

    fun canMakeAiRequest(): Boolean = batchTranslateManager.canMakeAiRequest()

    fun isDeviceOnline(): Boolean = batchTranslateManager.isDeviceOnline()

    private suspend fun refreshDetailedCardIfOpen(deckId: String, wordText: String) {
        val current = _detailedCard.value ?: return
        if (current.deck == deckId && current.text.equals(wordText, ignoreCase = true)) {
            _detailedCard.value = repository.getWordCardWithVariants(deckId, wordText)
        }
    }

    fun playAudio(word: String, audioUrl: String) {
        audioPlayer.playWordOrUrl(word, audioUrl)
    }

    fun playWordVoiceAudio(word: String, audioUrl: String = "", isGb: Boolean = false) {
        audioPlayer.playWordWithVoice(word, audioUrl, isGb)
    }

    fun playAudioSlow(word: String, audioUrl: String = "", isGb: Boolean = _preferredAccentUk.value) {
        audioPlayer.playWordSlow(word, audioUrl, isGb)
    }

    fun speakSentenceTts(sentence: String, speechRateOverride: Float? = null) {
        audioPlayer.speakSentenceWithTts(sentence, speechRateOverride)
    }

    fun speakSentenceSlow(sentence: String) {
        audioPlayer.speakSentenceSlow(sentence)
    }

    fun toggleSaveExampleSentence(
        englishText: String,
        arabicText: String,
        sourceWord: String = ""
    ) {
        viewModelScope.launch {
            repository.toggleSaveExampleSentence(
                englishText = englishText,
                arabicText = arabicText,
                sourceWord = sourceWord
            )
        }
    }

    fun saveExampleSentence(
        englishText: String,
        arabicText: String,
        sourceWord: String = ""
    ) {
        viewModelScope.launch {
            repository.saveExampleSentence(
                englishText = englishText,
                arabicText = arabicText,
                sourceWord = sourceWord
            )
        }
    }

    fun deleteSentence(id: Long) {
        viewModelScope.launch {
            repository.deleteSentence(id)
        }
    }

    /**
     * Triggered ONLY when the user taps the sparkle icon on a saved sentence row.
     * If `aiExplanation` is already stored in Room, no API request is made.
     */
    fun explainSentenceWithAi(sentenceId: Long) {
        if (sentenceId in _explainingSentenceIds.value) return
        viewModelScope.launch {
            _explainingSentenceIds.update { it + sentenceId }
            try {
                geminiService.explainSentenceInSimpleArabic(sentenceId)
            } finally {
                _explainingSentenceIds.update { it - sentenceId }
            }
        }
    }

    fun toggleSentenceSortOrder() {
        _sentenceSortOrder.update { current ->
            if (current == SentenceSortOrder.NEWEST) SentenceSortOrder.OLDEST else SentenceSortOrder.NEWEST
        }
    }

    fun toggleShowSentenceTranslation() {
        _showSentenceTranslation.update { !it }
    }

    fun updateTtsSpeechRate(rate: Float) {
        val clamped = rate.coerceIn(0.6f, 1.3f)
        _ttsSpeechRate.value = clamped
        audioPlayer.setDefaultSpeechRate(clamped)
        viewModelScope.launch {
            onboardingRepository.setTtsSpeechRate(clamped)
        }
    }

    fun setPreferredAccentUk(isUk: Boolean) {
        _preferredAccentUk.value = isUk
        audioPlayer.setPreferUkAccent(isUk)
        viewModelScope.launch {
            onboardingRepository.setPreferredAccentUk(isUk)
        }
    }

    fun setAutoPlayPronunciation(enabled: Boolean) {
        _autoPlayPronunciation.value = enabled
        _autoPlayPracticeAudio.value = enabled
        viewModelScope.launch {
            onboardingRepository.setAutoPlayPronunciation(enabled)
        }
    }

    fun setDeveloperModeEnabled(enabled: Boolean) {
        _isDeveloperModeEnabled.value = enabled
        viewModelScope.launch {
            onboardingRepository.setDeveloperModeEnabled(enabled)
        }
    }

    fun setThemeMode(mode: com.example.ui.theme.AppThemeMode) {
        _themeMode.value = mode
        com.example.ui.theme.AppThemeState.themeMode = mode
        viewModelScope.launch {
            onboardingRepository.setThemeMode(mode.id)
        }
    }

    fun toggleLightDarkTheme() {
        val nextMode = if (com.example.ui.theme.AppThemeState.isLightMode) {
            com.example.ui.theme.AppThemeMode.DARK
        } else {
            com.example.ui.theme.AppThemeMode.LIGHT
        }
        setThemeMode(nextMode)
    }


    fun updateDictationIgnorePunctuation(ignore: Boolean) {
        _dictationIgnorePunctuation.value = ignore
    }

    fun updateAutoPlayPracticeAudio(autoPlay: Boolean) {
        _autoPlayPracticeAudio.value = autoPlay
        _autoPlayPronunciation.value = autoPlay
        viewModelScope.launch {
            onboardingRepository.setAutoPlayPronunciation(autoPlay)
        }
    }

    fun loadDictionarySampleExamples() {
        viewModelScope.launch {
            _dictionarySampleExamples.value = repository.getDictionaryExamplesForQuickAdd(limit = 40)
        }
    }

    // --- GRAMMAR & READING INITIALIZATION ---
    init {
        viewModelScope.launch {
            try {
                grammarRepository.importGrammarLessonsIfNeeded()
            } finally {
                _isGrammarImporting.value = false
            }
            readingRepository.importReadingTextsIfNeeded()
        }
    }

    fun selectGrammarLesson(lesson: GrammarLessonEntity?) {
        _selectedGrammarLesson.value = lesson
    }

    fun saveGrammarProgress(
        lessonId: String,
        completed: Int,
        total: Int,
        score: Int,
        isDone: Boolean,
        elapsedMs: Long? = null
    ) {
        viewModelScope.launch {
            grammarRepository.saveLessonProgress(
                lessonId = lessonId,
                completedExercises = completed,
                totalExercises = total,
                score = score,
                isCompleted = isDone,
                elapsedMs = elapsedMs
            )
            refreshDailyChecklist()
        }
    }

    suspend fun askAiAboutLesson(lesson: GrammarLessonEntity, question: String): Result<String> {
        return grammarAiService.askAboutLesson(lesson, question)
    }

    suspend fun explainGrammarMistake(
        lessonId: String,
        title: String,
        prompt: String,
        wrong: String,
        correct: String
    ): Result<String> {
        return grammarAiService.explainMistake(lessonId, title, prompt, wrong, correct)
    }

    suspend fun checkGrammarSentence(
        lesson: GrammarLessonEntity,
        sentence: String
    ): Result<GrammarSentenceCheckResult> {
        return grammarAiService.checkWrittenSentence(lesson, sentence)
    }

    suspend fun generateMoreGrammarPractice(
        lesson: GrammarLessonEntity
    ): Result<List<GrammarExerciseItem>> {
        val result = grammarAiService.generateMorePracticeExercises(lesson)
        if (result.isSuccess) {
            val updated = grammarRepository.getLessonById(lesson.id)
            if (updated != null) {
                _selectedGrammarLesson.value = updated
            }
        }
        return result
    }

    suspend fun createGrammarLessonWithAi(
        level: String,
        topic: String
    ): Result<Unit> {
        val res = grammarAiService.createNewLessonWithAi(level, topic)
        return if (res.isSuccess) Result.success(Unit) else Result.failure(res.exceptionOrNull() ?: Exception())
    }

    fun syncGrammarLessonsFromAssets(onResult: (Result<Int>) -> Unit = {}) {
        viewModelScope.launch {
            val res = grammarRepository.importGrammarLessonsFromAssets(force = true)
            onResult(res)
        }
    }

    suspend fun importGrammarLessonsFromText(jsonText: String): Result<Int> {
        return grammarRepository.importGrammarLessonsFromJsonString(jsonText, source = "file")
    }

    fun importGrammarLessonsFromUris(uris: List<Uri>, onResult: (Result<Int>) -> Unit = {}) {
        viewModelScope.launch {
            val res = grammarRepository.importGrammarLessonsFromUris(uris)
            onResult(res)
        }
    }

    // --- READING ACTIONS ---
    fun selectReadingText(text: ReadingTextEntity?) {
        _selectedReadingText.value = text
    }

    fun saveReadingProgress(
        textId: String,
        completed: Int,
        total: Int,
        score: Int,
        isDone: Boolean,
        elapsedMs: Long? = null
    ) {
        viewModelScope.launch {
            readingRepository.saveReadingProgress(
                textId = textId,
                completedQuestions = completed,
                totalQuestions = total,
                score = score,
                isCompleted = isDone,
                elapsedMs = elapsedMs
            )
            refreshDailyChecklist()
        }
    }

    suspend fun lookupWordLocally(rawWord: String): List<EntryEntity> {
        readingRepository.recordWordLookup(rawWord)
        val results = readingRepository.lookupWordLocally(rawWord)
        _learnerModelSnapshot.value = studyPlanRepository.learnerModelRepository.buildSnapshot(studyPlanProfile.value)
        return results
    }

    suspend fun scheduleLookedUpWordForToday(rawWord: String): Boolean {
        val scheduled = readingRepository.scheduleWordForPracticeToday(rawWord) != null
        if (scheduled) {
            refreshDailyChecklist()
        }
        return scheduled
    }

    suspend fun saveWordFavorite(deckId: String, wordText: String, isFavorite: Boolean) {
        readingRepository.saveWordFavorite(deckId, wordText, isFavorite)
    }

    suspend fun getWordFavoriteStatus(deckId: String, wordText: String): Boolean {
        return readingRepository.getWordFavoriteStatus(deckId, wordText)
    }

    suspend fun getSavedTargetWords(limit: Int = 15): List<String> {
        return readingRepository.getSavedTargetWords(limit)
    }

    suspend fun explainWordInContext(
        textId: String,
        sentenceIndex: Int,
        sentenceEn: String,
        word: String
    ): Result<String> {
        return readingAiService.explainWordInContext(textId, sentenceIndex, sentenceEn, word)
    }

    suspend fun savePastedText(
        titleEn: String,
        rawText: String,
        topic: String,
        level: String
    ): Result<ReadingTextEntity> {
        return readingRepository.savePastedText(titleEn, rawText, topic, level)
    }

    suspend fun translateAndMakeQuestionsForPastedText(
        text: ReadingTextEntity
    ): Result<Unit> {
        val res = readingAiService.translateAndMakeQuestionsForPastedText(text)
        if (res.isSuccess) {
            val updated = readingRepository.getReadingTextById(text.id)
            if (updated != null) {
                _selectedReadingText.value = updated
            }
        }
        return res
    }

    suspend fun createNewReadingTextWithAi(
        level: String,
        topic: String,
        length: String,
        targetWords: List<String>,
        targetGrammarLessonId: String? = null
    ): Result<String> {
        val grammarLesson = if (!targetGrammarLessonId.isNullOrBlank()) {
            grammarRepository.getLessonById(targetGrammarLessonId)
        } else null
        val res = readingAiService.createNewTextWithAi(
            level = level,
            topic = topic,
            length = length,
            targetWords = targetWords,
            targetGrammarLessonId = targetGrammarLessonId,
            targetGrammarTitle = grammarLesson?.titleEn
        )
        if (res.isSuccess) {
            val newId = res.getOrNull()
            if (newId != null) {
                val entity = readingRepository.getReadingTextById(newId)
                if (entity != null) {
                    _selectedReadingText.value = entity
                }
            }
        }
        return res
    }

    suspend fun simplifyReadingText(original: ReadingTextEntity): Result<String> {
        val res = readingAiService.simplifyText(original)
        if (res.isSuccess) {
            val newId = res.getOrNull()
            if (newId != null) {
                val entity = readingRepository.getReadingTextById(newId)
                if (entity != null) {
                    _selectedReadingText.value = entity
                }
            }
        }
        return res
    }

    suspend fun askAiAboutReadingText(text: ReadingTextEntity, question: String): Result<String> {
        return readingAiService.askAboutText(text, question)
    }

    suspend fun feedbackOnOpenReadingQuestion(
        text: ReadingTextEntity,
        prompt: String,
        answer: String
    ): Result<String> {
        return readingAiService.feedbackOnOpenQuestion(text, prompt, answer)
    }

    fun deleteReadingText(id: String) {
        viewModelScope.launch {
            readingRepository.deleteReadingText(id)
            if (_selectedReadingText.value?.id == id) {
                _selectedReadingText.value = null
            }
        }
    }

    fun syncReadingTextsFromAssets(onResult: (Result<Int>) -> Unit = {}) {
        viewModelScope.launch {
            val res = readingRepository.importReadingTextsFromAssets(force = true)
            onResult(res)
        }
    }

    suspend fun importReadingTextsFromText(jsonText: String): Result<Int> {
        return readingRepository.importReadingTextsFromJsonString(jsonText, source = "file")
    }

    fun importReadingTextsFromUris(uris: List<Uri>, onResult: (Result<Int>) -> Unit = {}) {
        viewModelScope.launch {
            val res = readingRepository.importReadingTextsFromUris(uris)
            onResult(res)
        }
    }

    fun stopAudio() {
        audioPlayer.stopAudio()
    }

    // --- CORRECTION NOTEBOOK & SPACED REVIEW ACTIONS ---
    fun saveCorrectionNotebookEntry(
        textId: String,
        grammarLessonId: String,
        grammarLessonTitle: String,
        originalSentence: String,
        correctedSentence: String = "",
        explanationAr: String = "",
        betterAlternative: String = "",
        status: String = "not_checked"
    ) {
        viewModelScope.launch {
            readingRepository.saveCorrectionEntry(
                textId = textId,
                grammarLessonId = grammarLessonId,
                grammarLessonTitle = grammarLessonTitle,
                originalSentence = originalSentence,
                correctedSentence = correctedSentence,
                explanationAr = explanationAr,
                betterAlternative = betterAlternative,
                status = status
            )
        }
    }

    suspend fun checkCorrectionSentenceWithAi(entry: CorrectionNotebookEntity): Result<Unit> {
        val lessonTitle = entry.grammarLessonTitle.ifBlank {
            grammarRepository.getLessonById(entry.grammarLessonId)?.titleEn ?: "Grammar Structure"
        }
        val res = readingAiService.checkUserSentence(lessonTitle, entry.originalSentence)
        if (res.isSuccess) {
            val aiResult = res.getOrNull()
            if (aiResult != null) {
                val updated = entry.copy(
                    correctedSentence = aiResult.correctedSentence,
                    explanationAr = aiResult.explanationAr,
                    betterAlternative = aiResult.betterAlternative,
                    status = "checked",
                    updatedAt = System.currentTimeMillis()
                )
                readingRepository.updateCorrectionEntry(updated)
            }
            return Result.success(Unit)
        } else {
            return Result.failure(res.exceptionOrNull() ?: Exception("AI check failed"))
        }
    }

    fun updateCorrectionNotebookEntry(entry: CorrectionNotebookEntity) {
        viewModelScope.launch {
            readingRepository.updateCorrectionEntry(entry)
        }
    }

    fun deleteCorrectionNotebookEntry(id: Long) {
        viewModelScope.launch {
            readingRepository.deleteCorrectionEntry(id)
        }
    }

    fun recordSpacedReviewAttempt(id: Long, newSentence: String, isSuccess: Boolean = true) {
        viewModelScope.launch {
            readingRepository.recordSpacedReviewAttempt(id, newSentence, isSuccess)
            refreshDailyChecklist()
        }
    }

    suspend fun getGrammarLessonById(lessonId: String): GrammarLessonEntity? {
        return grammarRepository.getLessonById(lessonId)
    }

    fun startFocusedReviewPracticeSession(targetCountOverride: Int? = null) {
        viewModelScope.launch {
            _isFlashcardsLoading.value = true
            try {
                val checklist = _todayChecklist.value
                val taskCount = targetCountOverride ?: run {
                    if (checklist != null && checklist.tasksJson.isNotBlank()) {
                        val tasks = try {
                            kotlinx.serialization.json.Json.decodeFromString<List<DailyTaskItem>>(checklist.tasksJson)
                        } catch (_: Exception) { emptyList() }
                        val flashcardTask = tasks.find { it.type == "flashcards" }
                        if (flashcardTask != null && flashcardTask.targetCount > 0) {
                            (flashcardTask.targetCount - flashcardTask.completedCount).coerceAtLeast(1)
                        } else null
                    } else null
                } ?: 25

                val userLevel = studyPlanProfile.value?.level?.ifBlank { "B2" }?.uppercase() ?: "B2"
                val selectedDecks = parseSelectedDecks(studyPlanProfile.value)
                val nowMillis = System.currentTimeMillis()
                val dueNowCutoff = DataImportRepository.computeDueNowCutoff(nowMillis)

                val dueEntries = if (selectedDecks.isNotEmpty()) {
                    database.wordBooksDao().getDueNowEntriesForDecksOrdered(dueNowCutoff, selectedDecks)
                } else {
                    database.wordBooksDao().getDueNowEntriesOrdered(dueNowCutoff)
                }
                val allProgress = database.wordBooksDao().getAllWordProgressList()
                val progressMap = allProgress.associateBy { it.cardKey }

                val candidates = database.wordBooksDao().getPlacementVocabularyCandidatesForLevel(userLevel, limit = 150)
                val reviewedLevelEntries = candidates.filter { entry ->
                    val key = "${entry.deck}:${entry.text.trim().lowercase()}"
                    val p = progressMap[key]
                    p != null && (p.repetitions > 0 || p.lastReviewedTimestamp > 0) && !p.isMastered &&
                        p.dueTimestamp > 0 && p.dueTimestamp <= dueNowCutoff
                }

                val combined = (dueEntries + reviewedLevelEntries).distinctBy { "${it.deck}:${it.text.trim().lowercase()}" }
                val finalEntries = combined

                val groupedMap = LinkedHashMap<String, MutableList<EntryEntity>>()
                for (entry in finalEntries) {
                    val key = "${entry.deck}:${entry.text.trim().lowercase()}"
                    val list = groupedMap.getOrPut(key) { ArrayList(2) }
                    list.add(entry)
                    if (groupedMap.size >= taskCount) break
                }

                val cards: List<GroupedWordCard> = groupedMap.map { (cardKey, variants) ->
                    repository.buildGroupedCardFromEntries(variants, progressMap[cardKey])
                }.take(taskCount)

                _waitingLaterTodayCount.value = repository.getLaterTodayCardsCount(
                    nowMillis = nowMillis,
                    selectedDecks = selectedDecks
                )
                _nextWaitingDueTimestamp.value = repository.getNextLaterTodayDueTimestamp(
                    nowMillis = nowMillis,
                    selectedDecks = selectedDecks
                )

                _flashcardCards.value = cards
                _flashcardDeckId.value = DataImportRepository.DUE_TODAY_DECK_ID
                _flashcardIndex.value = 0
                _isCurrentCardFlipped.value = false

                sessionRatedCardKeys.clear()
                sessionFailedCards.clear()
                sessionMasteredCount = 0
                sessionNeedsReviewCount = 0
                sessionQualitySum = 0
                sessionInitialCards = cards
                _flashcardSessionType.value = FlashcardSessionType.REVIEW
                _isFlashcardSessionCompleted.value = false
                _flashcardSessionSummary.value = FlashcardSessionSummary(
                    sessionType = FlashcardSessionType.REVIEW,
                    deckName = "أهم 5000 كلمة أكاديمية وعامة",
                    totalCount = cards.size,
                    initialCards = cards
                )

                if (cards.isEmpty()) {
                    refreshDailyChecklist()
                }
            } catch (_: Exception) {
                loadFlashcardsForDeck("oxford5000")
            } finally {
                _isFlashcardsLoading.value = false
            }
        }
    }

    fun startFocusedNewWordsSession(targetCountOverride: Int? = null) {
        viewModelScope.launch {
            _isFlashcardsLoading.value = true
            try {
                val checklist = _todayChecklist.value
                val taskCount = targetCountOverride ?: run {
                    if (checklist != null && checklist.tasksJson.isNotBlank()) {
                        val tasks = try {
                            kotlinx.serialization.json.Json.decodeFromString<List<DailyTaskItem>>(checklist.tasksJson)
                        } catch (_: Exception) { emptyList() }
                        val newWordsTask = tasks.find { it.type == "new_words" }
                        if (newWordsTask != null && newWordsTask.targetCount > 0) {
                            val remaining = newWordsTask.targetCount - newWordsTask.completedCount
                            if (remaining > 0) remaining else 5
                        } else null
                    } else null
                } ?: 10

                val userLevel = studyPlanProfile.value?.level?.ifBlank { "B2" }?.uppercase() ?: "B2"
                val selectedDecks = try {
                    studyPlanProfile.value?.selectedDecksJson?.let {
                        if (it.isNotBlank()) {
                            kotlinx.serialization.json.Json.decodeFromString<List<String>>(it)
                        } else emptyList()
                    } ?: listOf("oxford5000", "oxford_opal")
                } catch (_: Exception) {
                    listOf("oxford5000", "oxford_opal")
                }

                val activeProfile = studyPlanProfile.value ?: studyPlanRepository.getOrCreateProfile()
                val newEntries = studyPlanRepository.getTodayTargetNewWords(
                    profile = activeProfile,
                    targetCount = taskCount
                ).ifEmpty {
                    database.wordBooksDao().getPlacementVocabularyCandidatesForLevel(userLevel, limit = taskCount)
                }

                val allProgress = database.wordBooksDao().getAllWordProgressList().associateBy { it.cardKey }
                val groupedMap = LinkedHashMap<String, MutableList<EntryEntity>>()
                for (entry in newEntries) {
                    val key = "${entry.deck}:${entry.text.trim().lowercase()}"
                    val list = groupedMap.getOrPut(key) { ArrayList(2) }
                    list.add(entry)
                    if (groupedMap.size >= taskCount) break
                }

                val cards: List<GroupedWordCard> = groupedMap.map { (cardKey, variants) ->
                    repository.buildGroupedCardFromEntries(variants, allProgress[cardKey])
                }.take(taskCount)

                if (cards.isNotEmpty()) {
                    _flashcardCards.value = cards
                    _flashcardDeckId.value = selectedDecks.firstOrNull() ?: "oxford5000"
                    _flashcardIndex.value = 0
                    _isCurrentCardFlipped.value = false

                    sessionRatedCardKeys.clear()
                    sessionFailedCards.clear()
                    sessionMasteredCount = 0
                    sessionNeedsReviewCount = 0
                    sessionQualitySum = 0
                    sessionInitialCards = cards
                    _flashcardSessionType.value = FlashcardSessionType.LEARN_NEW
                    _isFlashcardSessionCompleted.value = false
                    _flashcardSessionSummary.value = FlashcardSessionSummary(
                        sessionType = FlashcardSessionType.LEARN_NEW,
                        deckName = "مفردات جديدة",
                        totalCount = cards.size,
                        initialCards = cards
                    )
                } else {
                    loadFlashcardsForDeck("oxford5000")
                }
            } catch (_: Exception) {
                loadFlashcardsForDeck("oxford5000")
            } finally {
                _isFlashcardsLoading.value = false
            }
        }
    }

    fun startEarlyReviewSession() {
        viewModelScope.launch {
            _isFlashcardsLoading.value = true
            try {
                val selectedDecks = parseSelectedDecks(studyPlanProfile.value)
                val nowMillis = System.currentTimeMillis()
                val endOfTodayMillis = DataImportRepository.computeEndOfTodayMillis(nowMillis)

                val earlyEntries = if (selectedDecks.isNotEmpty()) {
                    database.wordBooksDao().getEarlyReviewEntriesForDecksOrdered(nowMillis, endOfTodayMillis, selectedDecks)
                } else {
                    database.wordBooksDao().getEarlyReviewEntriesOrdered(nowMillis, endOfTodayMillis)
                }

                val allProgress = database.wordBooksDao().getAllWordProgressList().associateBy { it.cardKey }
                val groupedMap = LinkedHashMap<String, MutableList<EntryEntity>>()
                for (entry in earlyEntries) {
                    val key = "${entry.deck}:${entry.text.trim().lowercase()}"
                    val list = groupedMap.getOrPut(key) { ArrayList(2) }
                    list.add(entry)
                }

                val cards: List<GroupedWordCard> = groupedMap.map { (cardKey, variants) ->
                    repository.buildGroupedCardFromEntries(variants, allProgress[cardKey])
                }

                _flashcardCards.value = cards
                _flashcardDeckId.value = DataImportRepository.DUE_TODAY_DECK_ID
                _flashcardIndex.value = 0
                _isCurrentCardFlipped.value = false
                _isFlashcardSessionCompleted.value = false

                if (cards.isNotEmpty()) {
                    sessionRatedCardKeys.clear()
                    sessionFailedCards.clear()
                    sessionMasteredCount = 0
                    sessionNeedsReviewCount = 0
                    sessionQualitySum = 0
                    sessionInitialCards = cards
                    _flashcardSessionType.value = FlashcardSessionType.REVIEW
                    _flashcardSessionSummary.value = FlashcardSessionSummary(
                        sessionType = FlashcardSessionType.REVIEW,
                        deckName = "مراجعة مبكرة (Early Review)",
                        totalCount = cards.size,
                        initialCards = cards
                    )
                }
            } catch (_: Exception) {
                // Ignore
            } finally {
                _isFlashcardsLoading.value = false
            }
        }
    }

    fun startReviewTodayLearnedWordsSession() {
        viewModelScope.launch {
            _isFlashcardsLoading.value = true
            try {
                val nowMillis = System.currentTimeMillis()
                val startOfDay = DataImportRepository.computeStartOfTodayMillis(nowMillis)
                val entries = database.wordBooksDao().getEntriesLearnedToday(startOfDay)

                val allProgress = database.wordBooksDao().getAllWordProgressList().associateBy { it.cardKey }
                val groupedMap = LinkedHashMap<String, MutableList<EntryEntity>>()
                for (entry in entries) {
                    val key = "${entry.deck}:${entry.text.trim().lowercase()}"
                    val list = groupedMap.getOrPut(key) { ArrayList(2) }
                    list.add(entry)
                }

                val cards: List<GroupedWordCard> = groupedMap.map { (cardKey, variants) ->
                    repository.buildGroupedCardFromEntries(variants, allProgress[cardKey])
                }

                _flashcardCards.value = cards
                _flashcardDeckId.value = "learned_today"
                _flashcardIndex.value = 0
                _isCurrentCardFlipped.value = false
                _isFlashcardSessionCompleted.value = false

                if (cards.isNotEmpty()) {
                    sessionRatedCardKeys.clear()
                    sessionFailedCards.clear()
                    sessionMasteredCount = 0
                    sessionNeedsReviewCount = 0
                    sessionQualitySum = 0
                    sessionInitialCards = cards
                    _flashcardSessionType.value = FlashcardSessionType.REVIEW
                    _flashcardSessionSummary.value = FlashcardSessionSummary(
                        sessionType = FlashcardSessionType.REVIEW,
                        deckName = "كلمات تم تعلمها اليوم",
                        totalCount = cards.size,
                        initialCards = cards
                    )
                }
            } catch (_: Exception) {
                // Ignore
            } finally {
                _isFlashcardsLoading.value = false
            }
        }
    }

    fun preparePlanTaskNavigation(route: String, linkedId: String?) {
        val safeLinkedId = linkedId.orEmpty()
        val checklist = _todayChecklist.value
        val tasks = if (checklist != null && checklist.tasksJson.isNotBlank()) {
            try {
                kotlinx.serialization.json.Json.decodeFromString<List<DailyTaskItem>>(checklist.tasksJson)
            } catch (_: Exception) { emptyList() }
        } else emptyList()

        val matchingTask = tasks.find { it.id == safeLinkedId || it.linkedId == safeLinkedId || it.type == route }
        val remainingCount = matchingTask?.let {
            val rem = it.targetCount - it.completedCount
            if (rem > 0) rem else null
        }

        when {
            route == "quiz" || safeLinkedId.contains("quiz") -> {
                startDailyMiniQuiz()
            }
            route == "flashcards" || safeLinkedId.contains("flashcard") || safeLinkedId.contains("review") -> {
                startFocusedReviewPracticeSession(remainingCount)
            }
            route == "new_words" || safeLinkedId.contains("new_words") || safeLinkedId.contains("learn") -> {
                startFocusedNewWordsSession(remainingCount)
            }
            route == "word_books" || route == "database" -> {
                val parts = safeLinkedId.split("|")
                val deck = parts.getOrNull(0)?.ifBlank { "oxford5000" } ?: "oxford5000"
                val level = parts.getOrNull(1).orEmpty()
                _selectedDeckId.value = deck
                _filterState.update { current ->
                    current.copy(
                        deckId = deck,
                        onlyFavorites = false,
                        searchQuery = "",
                        typeFilter = "",
                        levelFilter = level,
                        posFilter = ""
                    )
                }
            }
            route == "grammar" -> {
                if (safeLinkedId.isNotBlank()) {
                    val existing = grammarLessons.value.find { it.id == safeLinkedId }
                    if (existing != null) {
                        selectGrammarLesson(existing)
                    } else {
                        viewModelScope.launch {
                            val loaded = grammarRepository.getLessonById(safeLinkedId)
                            selectGrammarLesson(loaded)
                        }
                    }
                }
            }
            route == "reading" -> {
                if (safeLinkedId.isNotBlank()) {
                    val existing = readingTexts.value.find { it.id == safeLinkedId }
                    if (existing != null) {
                        selectReadingText(existing)
                    } else {
                        viewModelScope.launch {
                            val loaded = readingRepository.getReadingTextById(safeLinkedId)
                            selectReadingText(loaded)
                        }
                    }
                }
            }
        }
    }

    fun refreshDailyChecklist() {
        viewModelScope.launch {
            val checklist = studyPlanRepository.getOrCreateDailyChecklist()
            _todayChecklist.value = checklist
            _studyStreak.value = studyPlanRepository.computeStreak()
            _weakAreas.value = studyPlanRepository.getWeakAreas()
            _practiceAccuracySummary.value = studyPlanRepository.getPracticeAccuracySummary()
            _learnerModelSnapshot.value = studyPlanRepository.learnerModelRepository.buildSnapshot(studyPlanProfile.value)
            val nowMillis = System.currentTimeMillis()
            val selectedDecks = parseSelectedDecks(studyPlanProfile.value)
            _waitingLaterTodayCount.value = repository.getLaterTodayCardsCount(
                nowMillis = nowMillis,
                selectedDecks = selectedDecks
            )
            _nextWaitingDueTimestamp.value = repository.getNextLaterTodayDueTimestamp(
                nowMillis = nowMillis,
                selectedDecks = selectedDecks
            )
        }
    }

    fun dismissAdaptiveInsight(insightId: String) {
        if (_learnerModelSnapshot.value.activeInsight?.id == insightId) {
            _learnerModelSnapshot.value = _learnerModelSnapshot.value.copy(activeInsight = null)
        }
        viewModelScope.launch {
            val updatedProfile = studyPlanRepository.learnerModelRepository.dismissInsight(insightId)
            _learnerModelSnapshot.value = studyPlanRepository.learnerModelRepository.buildSnapshot(updatedProfile)
        }
    }

    fun acceptSuggestedLevelUp(nextLevel: String) {
        viewModelScope.launch {
            val current = studyPlanProfile.value ?: studyPlanRepository.getOrCreateProfile()
            val updated = current.copy(
                level = nextLevel.trim().uppercase(),
                updatedAt = studyPlanRepository.timeProvider.currentTimeMillis()
            )
            studyPlanRepository.updateProfile(updated)
            refreshDailyChecklist()
        }
    }

    suspend fun getWordsForLifecycleStage(stageKey: String, limit: Int = 100): List<LifecycleStageWordItem> = withContext(Dispatchers.IO) {
        val allProgress = database.wordBooksDao().getAllWordProgressList()
        val requestedStage = when (stageKey.lowercase()) {
            "seen" -> WordStage.SEEN
            "learning" -> WordStage.LEARNING
            "familiar" -> WordStage.FAMILIAR
            "mature" -> WordStage.MATURE
            else -> null
        }
        val isConquered = stageKey.equals("conquered", ignoreCase = true)
        val isNew = stageKey.equals("new", ignoreCase = true)

        if (isNew) {
            val userLevel = studyPlanProfile.value?.level?.ifBlank { "B1" }?.uppercase() ?: "B1"
            val newCandidates = database.wordBooksDao().getPlacementVocabularyCandidatesForLevel(userLevel, limit = limit * 2)
            val progressMap = allProgress.associateBy { it.cardKey }
            val unstudied = newCandidates.filter { entry ->
                val key = "${entry.deck}:${entry.text.trim().lowercase()}"
                progressMap[key] == null || LearnerModelRepository.classifyWordStage(progressMap[key]) == WordStage.NEW
            }.distinctBy { it.text.trim().lowercase() }.take(limit)
            return@withContext unstudied.map { entry ->
                LifecycleStageWordItem(
                    headword = entry.text,
                    deck = entry.deck,
                    pos = entry.pos,
                    arabic = entry.arabic,
                    intervalDays = 0,
                    repetitions = 0,
                    stage = WordStage.NEW
                )
            }
        }

        val targetProgress = when {
            isConquered -> allProgress.filter { LearnerModelRepository.isWordConquered(it) }
                .sortedByDescending { if (it.conqueredTimestamp > 0L) it.conqueredTimestamp else it.lastReviewedTimestamp }
            requestedStage != null -> allProgress.filter { LearnerModelRepository.classifyWordStage(it) == requestedStage }
                .sortedByDescending { it.lastReviewedTimestamp }
            else -> emptyList()
        }.take(limit)

        if (targetProgress.isEmpty()) return@withContext emptyList()

        val headwords = targetProgress.map { it.text.trim().lowercase() }.distinct()
        val entries = database.wordBooksDao().getEntriesByHeadwords(headwords)
        val entryMap = entries.groupBy { "${it.deck}:${it.text.trim().lowercase()}" }

        targetProgress.map { wp ->
            val matched = entryMap[wp.cardKey]?.firstOrNull() ?: entries.firstOrNull { it.text.equals(wp.text, ignoreCase = true) }
            LifecycleStageWordItem(
                headword = wp.text,
                deck = wp.deck,
                pos = matched?.pos ?: "word",
                arabic = matched?.arabic.orEmpty(),
                intervalDays = wp.intervalDays,
                repetitions = wp.repetitions,
                stage = requestedStage ?: if (isConquered) WordStage.MATURE else WordStage.NEW
            )
        }
    }

    fun recordSentencePracticeAttempt(sentenceIdOrText: String, scorePercent: Int, elapsedMs: Long? = null) {
        viewModelScope.launch {
            repository.recordSentencePracticeAttempt(
                sentenceIdOrText = sentenceIdOrText,
                scorePercent = scorePercent,
                nowMillis = System.currentTimeMillis(),
                elapsedMs = elapsedMs
            )
            refreshDailyChecklist()
        }
    }

    fun forceRebuildTodayPlan() {
        viewModelScope.launch {
            val updated = studyPlanRepository.regenerateChecklistForUpdatedPlan()
            _todayChecklist.value = updated
            refreshDailyChecklist()
        }
    }

    fun simulateReviewBacklog42() {
        viewModelScope.launch {
            val now = studyPlanRepository.timeProvider.currentTimeMillis()
            val yesterday = now - 86_400_000L
            val profile = studyPlanProfile.value ?: studyPlanRepository.getOrCreateProfile()
            val decks = parseSelectedDecks(profile).ifEmpty { listOf("oxford5000") }
            val primaryDeck = decks.first()
            val candidates = database.wordBooksDao().getPlacementVocabularyCandidatesForLevel(
                level = profile.level.ifBlank { "B1" },
                limit = 45
            )
            val entriesToSeed = if (candidates.size >= 42) {
                candidates.take(42)
            } else {
                database.wordBooksDao().getRecommendedWordsForLevel("", limit = 42)
            }
            val progressBatch = entriesToSeed.mapIndexed { idx, entry ->
                val wordText = entry.text.trim().lowercase()
                val deck = entry.deck.ifBlank { primaryDeck }
                WordProgressEntity(
                    cardKey = "$deck:$wordText",
                    entryId = entry.id,
                    deck = deck,
                    text = wordText,
                    easeFactor = 2.5f,
                    intervalDays = 3,
                    repetitions = 2,
                    dueTimestamp = now - (idx + 1) * 60_000L,
                    lastReviewedTimestamp = yesterday,
                    firstLearnedTimestamp = yesterday - 86_400_000L
                )
            }
            if (progressBatch.isNotEmpty()) {
                database.wordBooksDao().upsertWordProgressBatch(progressBatch)
            }
            val updated = studyPlanRepository.regenerateChecklistForUpdatedPlan()
            _todayChecklist.value = updated
            refreshDailyChecklist()
        }
    }

    suspend fun exportUserBackupJson(): String {
        return repository.exportUserBackupJson()
    }

    suspend fun importUserBackupJson(jsonText: String): Boolean {
        val ok = repository.importUserBackupJson(jsonText)
        if (ok) {
            refreshDailyChecklist()
        }
        return ok
    }

    fun exportUserBackupToUri(uri: Uri, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            try {
                val jsonText = withContext(Dispatchers.IO) {
                    repository.exportUserBackupJson()
                }
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(jsonText.toByteArray(Charsets.UTF_8))
                    } ?: throw IllegalStateException("Unable to open output stream")
                }
                onResult(true, "Progress backup exported successfully (Schema v16).")
            } catch (e: Exception) {
                onResult(false, "Export failed: ${e.message ?: "Unknown error"}")
            }
        }
    }

    fun importUserBackupFromUri(uri: Uri, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            try {
                val jsonText = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                        it.readText()
                    } ?: throw IllegalStateException("Unable to read backup file")
                }
                val ok = repository.importUserBackupJson(jsonText)
                if (ok) {
                    refreshDailyChecklist()
                    onResult(true, "Progress backup restored! All streaks, cards, and plans are up to date.")
                } else {
                    onResult(false, "Invalid backup file format.")
                }
            } catch (e: Exception) {
                onResult(false, "Restore failed: ${e.message ?: "Unknown error"}")
            }
        }
    }

    fun savePlanProfile(profile: StudyPlanProfileEntity) {
        viewModelScope.launch {
            studyPlanRepository.updateProfile(profile)
            refreshDailyChecklist()
        }
    }

    suspend fun generatePlanWithAi(month: String): Result<Unit> {
        val result = studyPlanAiService.generateMonthlyPlanWithAi(month)
        if (result.isSuccess) {
            refreshDailyChecklist()
            return Result.success(Unit)
        } else {
            return Result.failure(result.exceptionOrNull() ?: Exception("Failed to generate plan with AI"))
        }
    }

    private val _activeQuizSession = MutableStateFlow<ActiveQuizSession?>(null)
    val activeQuizSession: StateFlow<ActiveQuizSession?> = _activeQuizSession.asStateFlow()

    private val _isQuizLoading = MutableStateFlow(false)
    val isQuizLoading: StateFlow<Boolean> = _isQuizLoading.asStateFlow()

    fun startDailyMiniQuiz() {
        viewModelScope.launch {
            _isQuizLoading.value = true
            try {
                val checklist = _todayChecklist.value ?: studyPlanRepository.getOrCreateDailyChecklist()
                val profile = studyPlanProfile.value ?: studyPlanRepository.getOrCreateProfile()
                val ttsAllowed = !profile.quietModeEnabled
                val alignedResult = localQuizEngine.generateAlignedDailyQuiz(checklist, profile, isTtsAvailable = ttsAllowed)
                _activeQuizSession.value = ActiveQuizSession(
                    title = if (profile.quietModeEnabled) "Daily Mini-Quiz (Quiet Mode)" else "Daily Mini-Quiz",
                    quizType = "daily_mini",
                    questions = alignedResult.questions,
                    scopeInfo = alignedResult.scopeInfo
                )
            } catch (e: Exception) {
                android.util.Log.e("WordBooksViewModel", "Error creating daily mini quiz", e)
            } finally {
                _isQuizLoading.value = false
            }
        }
    }

    fun startWeeklyQuiz() {
        viewModelScope.launch {
            _isQuizLoading.value = true
            try {
                val ttsAllowed = !(studyPlanProfile.value?.quietModeEnabled ?: false)
                val questions = localQuizEngine.generateWeeklyQuiz(isTtsAvailable = ttsAllowed)
                _activeQuizSession.value = ActiveQuizSession(
                    title = "Weekly Comprehensive Quiz",
                    quizType = "weekly",
                    questions = questions,
                    scopeInfo = null
                )
            } catch (e: Exception) {
                android.util.Log.e("WordBooksViewModel", "Error creating weekly quiz", e)
            } finally {
                _isQuizLoading.value = false
            }
        }
    }

    fun startWeakAreasQuiz() {
        viewModelScope.launch {
            _isQuizLoading.value = true
            try {
                val level = studyPlanProfile.value?.level ?: "B2"
                val ttsAllowed = !(studyPlanProfile.value?.quietModeEnabled ?: false)
                val questions = localQuizEngine.generateWeakAreasQuiz(targetLevel = level, isTtsAvailable = ttsAllowed)
                _activeQuizSession.value = ActiveQuizSession(
                    title = "Weak Areas Targeted Practice",
                    quizType = "weak_areas",
                    questions = questions,
                    scopeInfo = null
                )
            } catch (e: Exception) {
                android.util.Log.e("WordBooksViewModel", "Error creating weak areas quiz", e)
            } finally {
                _isQuizLoading.value = false
            }
        }
    }

    fun dismissActiveQuiz() {
        _activeQuizSession.value = null
    }

    suspend fun submitActiveQuiz(answers: Map<String, Int>): Map<String, Float> {
        val session = _activeQuizSession.value ?: return emptyMap()
        val result = localQuizEngine.recordQuizSubmission(session.quizType, session.questions, answers)
        if (session.quizType == "daily_mini") {
            val today = studyPlanRepository.timeProvider.todayDateString()
            studyPlanRepository.markTaskCompletedManually(today, "task_daily_quiz")
        }
        refreshDailyChecklist()
        return result
    }

    suspend fun generateDailyQuiz(): List<QuizQuestionItem> {
        val checklist = _todayChecklist.value ?: studyPlanRepository.getOrCreateDailyChecklist()
        val profile = studyPlanProfile.value ?: studyPlanRepository.getOrCreateProfile()
        return localQuizEngine.generateAlignedDailyQuiz(checklist, profile, isTtsAvailable = !profile.quietModeEnabled).questions
    }

    suspend fun generateWeeklyQuiz(): List<QuizQuestionItem> {
        return localQuizEngine.generateWeeklyQuiz(isTtsAvailable = true)
    }

    suspend fun generateWeakAreasQuiz(): List<QuizQuestionItem> {
        val level = studyPlanProfile.value?.level ?: "B2"
        return localQuizEngine.generateWeakAreasQuiz(targetLevel = level, isTtsAvailable = true)
    }

    suspend fun submitQuizAnswers(
        quizType: String,
        questions: List<QuizQuestionItem>,
        answers: Map<String, Int>
    ): Map<String, Float> {
        val result = localQuizEngine.recordQuizSubmission(quizType, questions, answers)
        if (quizType == "daily_mini") {
            val today = studyPlanRepository.timeProvider.todayDateString()
            studyPlanRepository.markTaskCompletedManually(today, "task_daily_quiz")
        }
        refreshDailyChecklist()
        return result
    }

    fun toggleTaskCompleted(date: String, taskId: String) {
        viewModelScope.launch {
            val updated = studyPlanRepository.markTaskCompletedManually(date, taskId)
            _todayChecklist.value = updated
            _studyStreak.value = studyPlanRepository.computeStreak()
        }
    }

    val apiKeyUpdatedTrigger = com.example.data.ai.GeminiApiKeyManager.apiKeyUpdatedTrigger

    fun getCustomApiKey(): String = com.example.data.ai.GeminiApiKeyManager.getCustomKey(getApplication())

    fun setCustomApiKey(key: String) {
        com.example.data.ai.GeminiApiKeyManager.setCustomApiKey(getApplication(), key)
    }

    fun clearCustomApiKey() {
        com.example.data.ai.GeminiApiKeyManager.clearCustomApiKey(getApplication())
    }

    fun getApiKeySource(): String = com.example.data.ai.GeminiApiKeyManager.getKeySource(getApplication())

    fun isApiKeyConfigured(): Boolean = com.example.data.ai.GeminiApiKeyManager.isConfigured(getApplication())

    fun getEngineMode(): com.example.data.ai.TranslationEngineMode =
        com.example.data.ai.GeminiApiKeyManager.getEngineMode(getApplication())

    fun setEngineMode(mode: com.example.data.ai.TranslationEngineMode) {
        com.example.data.ai.GeminiApiKeyManager.setEngineMode(getApplication(), mode)
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }

    companion object {
        /**
         * Sorts a list of [GroupedWordCard] according to [WordSortOrder].
         * For [WordSortOrder.MIXED], uses Knuth's multiplicative 32-bit golden-ratio hash
         * to produce an identical deterministic distribution across initial letters as Room SQLite.
         */
        internal fun sortGroupedCards(cards: List<GroupedWordCard>, sortOrder: WordSortOrder): List<GroupedWordCard> {
            if (cards.size <= 1) return cards
            return when (sortOrder) {
                WordSortOrder.AZ -> cards.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.text })
                WordSortOrder.ZA -> cards.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.text })
                WordSortOrder.LEVEL -> {
                    val levelRank = mapOf("A1" to 1, "A2" to 2, "B1" to 3, "B2" to 4, "C1" to 5)
                    cards.sortedWith(
                        compareBy<GroupedWordCard> { levelRank[it.primaryLevel.uppercase(Locale.ROOT)] ?: 99 }
                            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.text }
                    )
                }
                WordSortOrder.MIXED -> {
                    cards.sortedBy { card ->
                        val entryIdLong = card.posVariants.firstOrNull()?.entryId?.toLongOrNull()
                        val seed = entryIdLong ?: (card.cardKey.hashCode().toLong() and 0x7FFFFFFFL)
                        (seed * 2654435761L) and 0x7FFFFFFFL
                    }
                }
            }
        }

        internal fun resolveRestoredFlashcardIndex(
            sortedCards: List<GroupedWordCard>,
            studyState: com.example.data.local.DeckStudyStateEntity
        ): Int {
            if (sortedCards.isEmpty()) return 0
            if (studyState.lastWordText.isBlank()) return 0
            if (studyState.lastCardIndex == 0 && studyState.totalStudiedInDeck == 0) return 0
            val matchingIndex = sortedCards.indexOfFirst {
                it.text.equals(studyState.lastWordText, ignoreCase = true)
            }
            return if (matchingIndex >= 0) {
                matchingIndex
            } else {
                studyState.lastCardIndex.coerceIn(0, sortedCards.size - 1)
            }
        }
    }

}


data class ActiveQuizSession(
    val title: String,
    val quizType: String,
    val questions: List<QuizQuestionItem>,
    val scopeInfo: com.example.data.plan.DailyQuizScopeInfo? = null
)
