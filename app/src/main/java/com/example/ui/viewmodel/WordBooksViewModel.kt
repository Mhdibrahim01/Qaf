package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
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
import com.example.data.local.SentenceEntity
import com.example.data.local.WordBooksDatabase
import com.example.data.local.WordProgressEntity
import com.example.data.local.DailyChecklistEntity
import com.example.data.local.DailyTaskItem
import com.example.data.local.QuizQuestionItem
import com.example.data.local.QuizResultEntity
import com.example.data.local.StudyPlanEntity
import com.example.data.local.StudyPlanProfileEntity
import com.example.data.local.WeakAreasState
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
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
    val localQuizEngine = LocalQuizEngine(database.wordBooksDao(), studyPlanRepository.timeProvider)
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

    val importState: StateFlow<ImportProgressState> = repository.importState
    val aiStatus: StateFlow<AiTaskStatus> = geminiService.status
    val batchTranslateState: StateFlow<BatchTranslateUiState> = batchTranslateManager.uiState
    val currentlyPlayingWord: StateFlow<String?> = audioPlayer.currentlyPlayingWord

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

    val reviewDueTodayCount: StateFlow<Int> = repository.observeReviewDueTodayCount().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val studiedTodayCount: StateFlow<Int> = repository.observeStudiedTodayCount().stateIn(
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
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    private val _selectedDeckId = MutableStateFlow<String?>(null)
    val selectedDeckId: StateFlow<String?> = _selectedDeckId.asStateFlow()

    private val _filterState = MutableStateFlow(WordFilterState())
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

    private val _flashcardDirection = MutableStateFlow(repository.getFlashcardDirection())
    val flashcardDirection: StateFlow<FlashcardDirection> = _flashcardDirection.asStateFlow()

    private val _flashcardAutoShowAnswer = MutableStateFlow(repository.getFlashcardAutoShowAnswer())
    val flashcardAutoShowAnswer: StateFlow<Boolean> = _flashcardAutoShowAnswer.asStateFlow()

    // --- SENTENCES STATE ---
    private val _sentenceSortOrder = MutableStateFlow(SentenceSortOrder.NEWEST)
    val sentenceSortOrder: StateFlow<SentenceSortOrder> = _sentenceSortOrder.asStateFlow()

    private val _showSentenceTranslation = MutableStateFlow(true)
    val showSentenceTranslation: StateFlow<Boolean> = _showSentenceTranslation.asStateFlow()

    private val _ttsSpeechRate = MutableStateFlow(0.95f)
    val ttsSpeechRate: StateFlow<Float> = _ttsSpeechRate.asStateFlow()

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

    val monthlyChecklists: StateFlow<List<DailyChecklistEntity>> = database.wordBooksDao().observeChecklistsForMonth(
        studyPlanRepository.timeProvider.currentMonthString()
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        startImportIfNeeded()
        refreshDailyChecklist()
    }

    fun startImportIfNeeded(forceRecheck: Boolean = false) {
        viewModelScope.launch {
            repository.ensureImportedOnFirstLaunch(forceRecheck = forceRecheck)
            loadFlashcardsForDeck(_flashcardDeckId.value)
            loadDictionarySampleExamples()
        }
    }

    fun selectFlashcardDeck(deckId: String) {
        _flashcardDeckId.value = deckId
        _isCurrentCardFlipped.value = false
        loadFlashcardsForDeck(deckId)
    }

    fun loadFlashcardsForDeck(deckId: String) {
        viewModelScope.launch {
            _isFlashcardsLoading.value = true
            try {
                val (cards, studyState) = repository.loadFlashcardDeckCards(deckId)
                _flashcardCards.value = cards
                val restoredIndex = if (cards.isEmpty()) 0 else studyState.lastCardIndex.coerceIn(0, cards.size - 1)
                _flashcardIndex.value = restoredIndex
                _isCurrentCardFlipped.value = false
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
        val currentCard = cards[clamped]
        val deckId = _flashcardDeckId.value
        viewModelScope.launch {
            repository.saveDeckStudyPosition(
                deckId = deckId,
                cardIndex = clamped,
                wordText = currentCard.text,
                incrementStudied = false
            )
        }
    }

    fun flipCurrentFlashcard() {
        _isCurrentCardFlipped.update { !it }
    }

    fun setCurrentFlashcardFlipped(flipped: Boolean) {
        _isCurrentCardFlipped.value = flipped
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
        val nextIndex = repository.findNextDueCardIndex(
            cards = cards,
            currentIndex = currentIndex,
            progressMap = progressMap.value
        )
        val nextWordText = cards.getOrNull(nextIndex)?.text ?: currentCard.text

        _isCurrentCardFlipped.value = false
        _flashcardIndex.value = nextIndex
        viewModelScope.launch {
            repository.applySm2Review(
                deckId = currentCard.deck,
                wordText = currentCard.text,
                rating = rating
            )
            repository.saveDeckStudyPosition(
                deckId = activeDeckId,
                cardIndex = nextIndex,
                wordText = nextWordText,
                incrementStudied = true
            )
        }
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
            repository.clearAndReimport()
            loadFlashcardsForDeck(_flashcardDeckId.value)
        }
    }

    fun importJsonUris(uris: List<Uri>) {
        viewModelScope.launch {
            repository.importFromContentUris(uris)
            loadFlashcardsForDeck(_flashcardDeckId.value)
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
        _filterState.update { it.copy(sortOrder = sortOrder) }
    }

    fun clearAllFilters() {
        val currentDeck = _selectedDeckId.value ?: ""
        _filterState.update {
            WordFilterState(
                deckId = currentDeck,
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
        }
    }

    fun openDetailedDefinition(deckId: String, wordText: String) {
        viewModelScope.launch {
            _detailedCard.value = repository.getWordCardWithVariants(deckId, wordText)
            loadAiDefinitionForWord(wordText, forceFetch = false)
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
        _filterState.update { it.copy(onlyMastered = isMasteredOnly) }
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

    fun speakSentenceTts(sentence: String, speechRateOverride: Float? = null) {
        audioPlayer.speakSentenceWithTts(sentence, speechRateOverride)
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
    }

    fun updateDictationIgnorePunctuation(ignore: Boolean) {
        _dictationIgnorePunctuation.value = ignore
    }

    fun updateAutoPlayPracticeAudio(autoPlay: Boolean) {
        _autoPlayPracticeAudio.value = autoPlay
    }

    fun loadDictionarySampleExamples() {
        viewModelScope.launch {
            _dictionarySampleExamples.value = repository.getDictionaryExamplesForQuickAdd(limit = 40)
        }
    }

    // --- GRAMMAR & READING INITIALIZATION ---
    init {
        viewModelScope.launch {
            grammarRepository.importGrammarLessonsIfNeeded()
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
        isDone: Boolean
    ) {
        viewModelScope.launch {
            grammarRepository.saveLessonProgress(
                lessonId = lessonId,
                completedExercises = completed,
                totalExercises = total,
                score = score,
                isCompleted = isDone
            )
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
        isDone: Boolean
    ) {
        viewModelScope.launch {
            readingRepository.saveReadingProgress(
                textId = textId,
                completedQuestions = completed,
                totalQuestions = total,
                score = score,
                isCompleted = isDone
            )
        }
    }

    suspend fun lookupWordLocally(rawWord: String): List<EntryEntity> {
        return readingRepository.lookupWordLocally(rawWord)
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
        targetWords: List<String>
    ): Result<String> {
        val res = readingAiService.createNewTextWithAi(level, topic, length, targetWords)
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
        }
    }

    suspend fun getGrammarLessonById(lessonId: String): GrammarLessonEntity? {
        return grammarRepository.getLessonById(lessonId)
    }

    fun refreshDailyChecklist() {
        viewModelScope.launch {
            val checklist = studyPlanRepository.getOrCreateDailyChecklist()
            _todayChecklist.value = checklist
            _studyStreak.value = studyPlanRepository.computeStreak()
            _weakAreas.value = studyPlanRepository.getWeakAreas()
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

    suspend fun generateDailyQuiz(): List<QuizQuestionItem> {
        return localQuizEngine.generateDailyMiniQuiz(isTtsAvailable = true)
    }

    suspend fun generateWeeklyQuiz(): List<QuizQuestionItem> {
        return localQuizEngine.generateWeeklyQuiz(isTtsAvailable = true)
    }

    suspend fun submitQuizAnswers(
        quizType: String,
        questions: List<QuizQuestionItem>,
        answers: Map<String, Int>
    ): Map<String, Float> {
        val result = localQuizEngine.recordQuizSubmission(quizType, questions, answers)
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
}
