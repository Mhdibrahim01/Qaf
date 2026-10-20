package com.example.data.importer

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.Immutable
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.data.local.DeckArabicStats
import com.example.data.local.DeckEntity
import com.example.data.local.DeckStudyStateEntity
import com.example.data.local.EntryEntity
import com.example.data.local.FeatureFlagEntity
import com.example.data.local.RawJsonEntry
import com.example.data.local.SentenceEntity
import com.example.data.local.WordBooksDao
import com.example.data.local.WordBooksDatabase
import com.example.data.local.WordDetailEntity
import com.example.data.local.WordProgressEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.util.Calendar
import kotlin.math.roundToInt

enum class Sm2Rating(val labelEn: String, val labelAr: String, val quality: Int) {
    AGAIN("Again", "مرّة أخرى", 1),
    HARD("Hard", "صعب", 3),
    GOOD("Good", "جيد", 4),
    EASY("Easy", "سهل", 5)
}

@Immutable
data class Sm2IntervalPreview(
    val againLabel: String = "<10m",
    val hardLabel: String = "Today",
    val goodLabel: String = "1d",
    val easyLabel: String = "4d"
)

enum class WordSortOrder(val label: String, val sqlKey: String) {
    AZ("A → Z", "AZ"),
    ZA("Z → A", "ZA"),
    LEVEL("CEFR Level", "LEVEL");

    val labelEn: String get() = label
}

enum class FlashcardDirection(
    val storageKey: String,
    val labelEn: String,
    val labelAr: String
) {
    ENGLISH_TO_ARABIC("en_to_ar", "English to Arabic", "إنجليزي ← عربي"),
    ARABIC_TO_ENGLISH("ar_to_en", "Arabic to English", "عربي ← إنجليزي"),
    MIXED("mixed", "Mixed", "مختلط");

    companion object {
        fun fromKey(key: String?): FlashcardDirection =
            entries.firstOrNull { it.storageKey == key } ?: ENGLISH_TO_ARABIC
    }
}

@Immutable
data class WordFilterState(
    val deckId: String = "",
    val searchQuery: String = "",
    val typeFilter: String = "",
    val levelFilter: String = "",
    val posFilter: String = "",
    val sortOrder: WordSortOrder = WordSortOrder.AZ,
    val onlyFavorites: Boolean = false,
    val onlyMastered: Boolean = false,
    val onlyLearning: Boolean = false
)

@Immutable
data class ImportProgressState(
    val isImporting: Boolean = false,
    val currentFile: String = "",
    val currentFileSourcePath: String = "",
    val importedRows: Int = 0,
    val totalExpectedRows: Int = 9862,
    val batchNumber: Int = 0,
    val isCompleted: Boolean = false,
    val missingFiles: List<String> = emptyList(),
    val foundFilePaths: Map<String, String> = emptyMap(),
    val error: String? = null
) {
    val processedRows: Int get() = importedRows
    val progressFraction: Float
        get() = if (totalExpectedRows > 0) {
            (importedRows.toFloat() / totalExpectedRows.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
}

/**
 * UI representation of a grouped word card where the same `text` with different `pos`
 * in a deck is merged into one card with POS tabs.
 */
@Immutable
data class GroupedWordCard(
    val cardKey: String,
    val deck: String,
    val text: String,
    val primaryLevel: String,
    val primaryType: String,
    val ipa: String,
    val syllables: String,
    val posVariants: List<PosSenseVariant>,
    val isFavorite: Boolean = false,
    val isMastered: Boolean = false
)

@Immutable
data class PosSenseVariant(
    val entryId: String,
    val pos: String,
    val level: String,
    val type: String,
    val arabicMeaning: String,
    val arabicSource: String = "",
    val definitionEn: String,
    val noteEn: String,
    val examplesEn: List<String>,
    val examplesAr: List<String> = emptyList(),
    val definitionUrl: String,
    val audioUrl: String
)

private data class ResolvedJsonSource(
    val fileName: String,
    val exactPath: String,
    val openStream: () -> InputStream
)

class DataImportRepository(
    private val context: Context,
    private val database: WordBooksDatabase
) {
    private val dao: WordBooksDao = database.wordBooksDao()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val _importState = MutableStateFlow(ImportProgressState())
    val importState: StateFlow<ImportProgressState> = _importState.asStateFlow()

    private val flashcardPrefs by lazy {
        context.getSharedPreferences("wordbooks_flashcard_settings", Context.MODE_PRIVATE)
    }

    fun getFlashcardDirection(): FlashcardDirection {
        val raw = flashcardPrefs.getString("card_direction", FlashcardDirection.ENGLISH_TO_ARABIC.storageKey)
        return FlashcardDirection.fromKey(raw)
    }

    fun setFlashcardDirection(direction: FlashcardDirection) {
        flashcardPrefs.edit().putString("card_direction", direction.storageKey).apply()
    }

    fun getFlashcardAutoShowAnswer(): Boolean {
        return flashcardPrefs.getBoolean("auto_show_answer", false)
    }

    fun setFlashcardAutoShowAnswer(enabled: Boolean) {
        flashcardPrefs.edit().putBoolean("auto_show_answer", enabled).apply()
    }

    val decksFlow: Flow<List<DeckEntity>> = dao.observeDecks()
    val totalEntriesFlow: Flow<Int> = dao.observeTotalEntryCount()
    val emptyArabicCountFlow: Flow<Int> = dao.observeEmptyArabicCount()
    val deckArabicStatsFlow: Flow<List<DeckArabicStats>> = dao.observeDeckArabicStats()
    val allProgressFlow: Flow<List<WordProgressEntity>> = dao.observeAllWordProgress()
    val favoriteCountFlow: Flow<Int> = dao.observeFavoriteWordCount()
    val masteredCountFlow: Flow<Int> = dao.observeMasteredWordCount()
    val deckStudyStatesFlow: Flow<List<DeckStudyStateEntity>> = dao.observeAllDeckStudyStates()
    val sentencesNewestFirstFlow: Flow<List<SentenceEntity>> = dao.observeSentencesNewestFirst()
    val sentencesOldestFirstFlow: Flow<List<SentenceEntity>> = dao.observeSentencesOldestFirst()
    val savedSentencesCountFlow: Flow<Int> = dao.observeSavedSentencesCount()

    fun observeReviewDueTodayCount(nowMillis: Long = System.currentTimeMillis()): Flow<Int> {
        return dao.observeReviewDueTodayCount(computeEndOfTodayMillis(nowMillis))
    }

    fun observeStudiedTodayCount(nowMillis: Long = System.currentTimeMillis()): Flow<Int> {
        return dao.observeStudiedTodayCount(computeStartOfTodayMillis(nowMillis))
    }

    suspend fun getReviewDueTodayCount(nowMillis: Long = System.currentTimeMillis()): Int =
        withContext(Dispatchers.IO) {
            dao.getReviewDueTodayCount(computeEndOfTodayMillis(nowMillis))
        }

    companion object {
        const val BATCH_SIZE = 500
        const val TOTAL_EXPECTED_ROWS = 9862
        const val DATA_VERSION = 3
        const val SAVED_WORDS_DECK_ID = "saved_words"
        const val DUE_TODAY_DECK_ID = "due_today"
        const val DAY_MILLIS = 24L * 60L * 60L * 1000L

        fun computeStartOfTodayMillis(nowMillis: Long = System.currentTimeMillis()): Long {
            val cal = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis
        }

        fun computeEndOfTodayMillis(nowMillis: Long = System.currentTimeMillis()): Long {
            val cal = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            return maxOf(cal.timeInMillis, nowMillis + 12L * 3600_000L)
        }

        fun computeSm2IntervalPreview(progress: WordProgressEntity?): Sm2IntervalPreview {
            return Sm2Algorithm.previewIntervals(
                reps = progress?.repetitions ?: 0,
                interval = progress?.intervalDays ?: 0,
                ef = progress?.easeFactor ?: 2.5f
            )
        }

        /**
         * The 7 required JSON files for the core Word Books (9,862 rows total).
         */
        val REQUIRED_JSON_FILES = listOf(
            "oxford5000_part1.json",
            "oxford5000_part2.json",
            "oxford5000_part3.json",
            "oxford_opal.json",
            "oxford_phrases.json",
            "everyday_expressions.json",
            "essential_idioms.json"
        )

        /**
         * Optional generated decks created by `generate_decks.py` (fixed ID ranges: 100001+ to 700001+).
         */
        val OPTIONAL_GENERATED_FILES = listOf(
            "ielts_core.json",
            "it_industry.json",
            "cefr_c1.json",
            "pte_academic.json",
            "business_english.json",
            "career_interview.json",
            "medical_healthcare.json"
        )

        /**
         * Expected counts per deck for the 7 core JSON files.
         */
        val EXPECTED_DECK_COUNTS = linkedMapOf(
            "oxford5000" to 5948,
            "oxford_opal" to 2096,
            "oxford_phrases" to 750,
            "everyday_expressions" to 600,
            "essential_idioms" to 468
        )
    }

    /**
     * Loads all grouped flashcard items for the given `deckId` from Room SQLite along with
     * the persisted `DeckStudyStateEntity` (remembered last card position for that deck).
     */
    suspend fun loadFlashcardDeckCards(
        deckId: String,
        nowMillis: Long = System.currentTimeMillis()
    ): Pair<List<GroupedWordCard>, DeckStudyStateEntity> = withContext(Dispatchers.IO) {
        val rawEntries = when (deckId) {
            DUE_TODAY_DECK_ID -> dao.getDueTodayEntriesOrdered(computeEndOfTodayMillis(nowMillis))
            else -> dao.getEntriesForDeckOrdered(deckId)
        }
        val progressByKey = if (deckId == DUE_TODAY_DECK_ID) {
            dao.getAllWordProgressList().associateBy { it.cardKey }
        } else {
            dao.getWordProgressForDeck(deckId).associateBy { it.cardKey }
        }

        // Group same word (`LOWER(text)`) with different `pos` in the same deck into one card with POS tabs
        val groupedMap = LinkedHashMap<String, MutableList<EntryEntity>>()
        for (entry in rawEntries) {
            val key = "${entry.deck}:${entry.text.trim().lowercase()}"
            groupedMap.getOrPut(key) { ArrayList(2) }.add(entry)
        }

        val cards = groupedMap.map { (cardKey, variants) ->
            buildGroupedCardFromEntries(variants, progressByKey[cardKey])
        }

        val savedState = dao.getDeckStudyState(deckId)
        val clampedIndex = if (cards.isEmpty()) {
            0
        } else {
            (savedState?.lastCardIndex ?: 0).coerceIn(0, cards.size - 1)
        }
        val resolvedState = savedState?.copy(lastCardIndex = clampedIndex)
            ?: DeckStudyStateEntity(
                deckId = deckId,
                lastCardIndex = 0,
                lastWordText = cards.firstOrNull()?.text.orEmpty(),
                totalStudiedInDeck = 0
            )

        cards to resolvedState
    }

    suspend fun getDeckStudyState(deckId: String): DeckStudyStateEntity? = withContext(Dispatchers.IO) {
        dao.getDeckStudyState(deckId)
    }

    /**
     * Persists the user's current flashcard position (`cardIndex`, `wordText`) for `deckId` in Room.
     */
    suspend fun saveDeckStudyPosition(
        deckId: String,
        cardIndex: Int = -1,
        wordText: String,
        incrementStudied: Boolean = false
    ): DeckStudyStateEntity = withContext(Dispatchers.IO) {
        val existing = dao.getDeckStudyState(deckId)
        val newStudiedCount = (existing?.totalStudiedInDeck ?: 0) + (if (incrementStudied) 1 else 0)
        val resolvedIndex = if (cardIndex >= 0) cardIndex else (existing?.lastCardIndex ?: 0)
        val updated = DeckStudyStateEntity(
            deckId = deckId,
            lastCardIndex = resolvedIndex,
            lastWordText = wordText,
            totalStudiedInDeck = newStudiedCount,
            updatedAt = System.currentTimeMillis()
        )
        dao.upsertDeckStudyState(updated)
        updated
    }

    /**
     * Applies SM-2 spaced repetition algorithm (`Again`, `Hard`, `Good`, `Easy`) to the given word card
     * and persists the updated `WordProgressEntity` in Room SQLite.
     *
     * - `AGAIN`: resets repetitions to 0, sets intervalDays = 0, due in 10 minutes (counted in "Review due today").
     * - `HARD`: on first review sets intervalDays = 0 (due later today, counted in "Review due today"), otherwise 1.2x interval.
     * - `GOOD`: 1d -> 3d -> round(interval * easeFactor) days.
     * - `EASY`: 4d -> 7d -> round(interval * easeFactor * 1.3) days, +0.15 easeFactor.
     */
    suspend fun applySm2Review(
        deckId: String,
        wordText: String,
        rating: Sm2Rating,
        nowMillis: Long = System.currentTimeMillis()
    ): WordProgressEntity = withContext(Dispatchers.IO) {
        val cardKey = buildCardKey(deckId, wordText)
        val current = dao.getWordProgress(cardKey) ?: WordProgressEntity(
            cardKey = cardKey,
            deck = deckId,
            text = wordText.trim().lowercase()
        )
        val updated = Sm2Algorithm.applyRating(current, rating, nowMillis)
        dao.upsertWordProgress(updated)
        updated
    }

    /**
     * Finds the next due (or unreviewed) card index in [cards] after [currentIndex].
     * Unreviewed cards (`dueTimestamp == 0L`) and cards due today (`dueTimestamp <= endOfToday`)
     * that are not mastered are prioritized. If all remaining cards are future-scheduled or mastered,
     * falls back to the immediate next card index so navigation always advances cleanly.
     */
    fun findNextDueCardIndex(
        cards: List<GroupedWordCard>,
        currentIndex: Int,
        progressMap: Map<String, WordProgressEntity>,
        nowMillis: Long = System.currentTimeMillis()
    ): Int {
        if (cards.isEmpty()) return 0
        if (currentIndex + 1 >= cards.size) return currentIndex.coerceIn(0, cards.size - 1)
        val endOfToday = computeEndOfTodayMillis(nowMillis)

        for (idx in (currentIndex + 1) until cards.size) {
            val card = cards[idx]
            val p = progressMap[card.cardKey]
            val isMastered = p?.isMastered ?: card.isMastered
            if (!isMastered) {
                val due = p?.dueTimestamp ?: 0L
                if (due == 0L || due <= endOfToday) {
                    return idx
                }
            }
        }
        return (currentIndex + 1).coerceIn(0, cards.size - 1)
    }

    /**
     * Rates the current card using SM-2, saves the updated `WordProgressEntity`, computes the next
     * due card index in [cards], and persists the new `DeckStudyStateEntity` for [activeDeckId].
     */
    suspend fun rateCardAndMoveToNextDue(
        activeDeckId: String,
        cards: List<GroupedWordCard>,
        currentIndex: Int,
        rating: Sm2Rating,
        nowMillis: Long = System.currentTimeMillis()
    ): Pair<WordProgressEntity, DeckStudyStateEntity> = withContext(Dispatchers.IO) {
        val safeCurrent = currentIndex.coerceIn(0, (cards.size - 1).coerceAtLeast(0))
        val currentCard = cards[safeCurrent]
        val updatedProgress = applySm2Review(
            deckId = currentCard.deck,
            wordText = currentCard.text,
            rating = rating,
            nowMillis = nowMillis
        )
        val progressByKey = if (activeDeckId == DUE_TODAY_DECK_ID) {
            dao.getAllWordProgressList().associateBy { it.cardKey }
        } else {
            dao.getWordProgressForDeck(activeDeckId).associateBy { it.cardKey }
        }
        val nextIndex = findNextDueCardIndex(
            cards = cards,
            currentIndex = safeCurrent,
            progressMap = progressByKey,
            nowMillis = nowMillis
        )
        val nextWordText = cards.getOrNull(nextIndex)?.text ?: currentCard.text
        val updatedDeckState = saveDeckStudyPosition(
            deckId = activeDeckId,
            cardIndex = nextIndex,
            wordText = nextWordText,
            incrementStudied = true
        )
        updatedProgress to updatedDeckState
    }

    fun getPagedEntries(filter: WordFilterState): Flow<PagingData<EntryEntity>> {
        return Pager(
            config = PagingConfig(
                pageSize = 60,
                prefetchDistance = 30,
                enablePlaceholders = false,
                initialLoadSize = 120
            ),
            pagingSourceFactory = {
                if (filter.onlyFavorites || filter.deckId == SAVED_WORDS_DECK_ID) {
                    dao.getPagedFavoriteEntries(
                        deckId = if (filter.deckId == SAVED_WORDS_DECK_ID) "" else filter.deckId,
                        searchQuery = filter.searchQuery.trim(),
                        typeFilter = filter.typeFilter,
                        levelFilter = filter.levelFilter,
                        posFilter = filter.posFilter,
                        sortOrder = filter.sortOrder.sqlKey
                    )
                } else if (filter.onlyMastered) {
                    dao.getPagedMasteredEntries(
                        deckId = filter.deckId,
                        searchQuery = filter.searchQuery.trim(),
                        typeFilter = filter.typeFilter,
                        levelFilter = filter.levelFilter,
                        posFilter = filter.posFilter,
                        sortOrder = filter.sortOrder.sqlKey
                    )
                } else if (filter.onlyLearning) {
                    dao.getPagedLearningEntries(
                        deckId = filter.deckId,
                        searchQuery = filter.searchQuery.trim(),
                        typeFilter = filter.typeFilter,
                        levelFilter = filter.levelFilter,
                        posFilter = filter.posFilter,
                        sortOrder = filter.sortOrder.sqlKey
                    )
                } else {
                    dao.getPagedEntries(
                        deckId = filter.deckId,
                        searchQuery = filter.searchQuery.trim(),
                        typeFilter = filter.typeFilter,
                        levelFilter = filter.levelFilter,
                        posFilter = filter.posFilter,
                        sortOrder = filter.sortOrder.sqlKey
                    )
                }
            }
        ).flow
    }

    suspend fun getWordCardWithVariants(deckId: String, wordText: String): GroupedWordCard? =
        withContext(Dispatchers.IO) {
            val variants = if (deckId == SAVED_WORDS_DECK_ID || deckId.isBlank()) {
                dao.getWordVariantsAcrossDecks(wordText)
            } else {
                dao.getWordVariants(deckId, wordText)
            }
            if (variants.isEmpty()) return@withContext null

            val resolvedDeckId = variants.first().deck
            val cardKey = buildCardKey(resolvedDeckId, wordText)
            val progress = dao.getWordProgress(cardKey)
            buildGroupedCardFromEntries(variants, progress)
        }

    fun buildGroupedCardFromEntries(
        variants: List<EntryEntity>,
        progress: WordProgressEntity?
    ): GroupedWordCard {
        val primary = variants.first()
        val cardKey = buildCardKey(primary.deck, primary.text)
        val posSenseList = variants.map { entry ->
            PosSenseVariant(
                entryId = entry.id,
                pos = entry.pos.trim(),
                level = entry.level,
                type = entry.type,
                arabicMeaning = entry.arabic.trim(),
                arabicSource = entry.arabicSource.trim(),
                definitionEn = entry.definitionEn.trim(),
                noteEn = entry.noteEn.trim(),
                examplesEn = entry.examplesEn
                    .split("|")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() },
                examplesAr = entry.examplesAr
                    .split("|")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() },
                definitionUrl = entry.definitionUrl,
                audioUrl = entry.audioUrl
            )
        }

        val resolvedIpa = variants.firstOrNull { it.ipa.isNotBlank() }?.ipa ?: ""
        val resolvedSyllables = variants.firstOrNull { it.syllables.isNotBlank() }?.syllables ?: ""

        return GroupedWordCard(
            cardKey = cardKey,
            deck = primary.deck,
            text = primary.text,
            primaryLevel = variants.firstOrNull { it.level.isNotBlank() }?.level ?: "",
            primaryType = primary.type,
            ipa = resolvedIpa,
            syllables = resolvedSyllables,
            posVariants = posSenseList,
            isFavorite = progress?.isFavorite == true,
            isMastered = progress?.isMastered == true
        )
    }

    suspend fun toggleFavorite(deckId: String, wordText: String) = withContext(Dispatchers.IO) {
        val cardKey = buildCardKey(deckId, wordText)
        val existing = dao.getWordProgress(cardKey)
        val updated = (existing ?: WordProgressEntity(
            cardKey = cardKey,
            deck = deckId,
            text = wordText.lowercase()
        )).copy(isFavorite = !(existing?.isFavorite ?: false))
        dao.upsertWordProgress(updated)
        refreshDeckProgressCounts(deckId)
    }

    suspend fun toggleMastered(deckId: String, wordText: String) = withContext(Dispatchers.IO) {
        val cardKey = buildCardKey(deckId, wordText)
        val existing = dao.getWordProgress(cardKey)
        val updated = (existing ?: WordProgressEntity(
            cardKey = cardKey,
            deck = deckId,
            text = wordText.lowercase()
        )).copy(isMastered = !(existing?.isMastered ?: false))
        dao.upsertWordProgress(updated)
        refreshDeckProgressCounts(deckId)
    }

    /**
     * Saves a user-edited Arabic meaning for a word/variant and sets `arabic_source = 'user'`
     * so future file imports or AI runs never overwrite it.
     */
    suspend fun saveUserArabicForEntry(entryId: String, userArabic: String) = withContext(Dispatchers.IO) {
        dao.updateEntryUserArabicById(entryId, userArabic.trim())
    }

    suspend fun saveUserArabicForWord(deckId: String, wordText: String, userArabic: String) = withContext(Dispatchers.IO) {
        dao.updateEntryUserArabicForWord(deckId, wordText, userArabic.trim())
    }

    /**
     * Saves an example sentence into the `sentences` table with its Arabic translation taken directly
     * from `examples_ar` (no AI needed). If the sentence is already saved, updates its Arabic text if blank
     * while preserving `aiExplanation` and `createdAt`.
     */
    suspend fun saveExampleSentence(
        englishText: String,
        arabicText: String = "",
        sourceWord: String = "",
        audioUrl: String = ""
    ): SentenceEntity = withContext(Dispatchers.IO) {
        val cleanEn = englishText.trim()
        val cleanAr = arabicText.trim()
        val existing = dao.getSentenceByEnglishText(cleanEn)
        if (existing != null) {
            if (existing.arabicText.isBlank() && cleanAr.isNotBlank()) {
                dao.updateSentenceArabicText(existing.id, cleanAr)
                return@withContext existing.copy(arabicText = cleanAr)
            }
            return@withContext existing
        }
        val entity = SentenceEntity(
            englishText = cleanEn,
            arabicText = cleanAr,
            sourceWord = sourceWord.trim(),
            audioUrl = audioUrl.trim(),
            isFavorite = true,
            aiExplanation = "",
            createdAt = System.currentTimeMillis()
        )
        val newId = dao.insertSentence(entity)
        entity.copy(id = newId)
    }

    /**
     * Toggles whether an example sentence is saved in the `sentences` table (used by bookmark icon
     * or long-press on any example sentence in Flashcards or Word Books).
     * Returns `true` if the sentence is now saved, `false` if it was removed.
     */
    suspend fun toggleSaveExampleSentence(
        englishText: String,
        arabicText: String = "",
        sourceWord: String = "",
        audioUrl: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanEn = englishText.trim()
        if (cleanEn.isEmpty()) return@withContext false
        val existing = dao.getSentenceByEnglishText(cleanEn)
        if (existing != null) {
            dao.deleteSentenceById(existing.id)
            false
        } else {
            saveExampleSentence(
                englishText = cleanEn,
                arabicText = arabicText.trim(),
                sourceWord = sourceWord.trim(),
                audioUrl = audioUrl.trim()
            )
            true
        }
    }

    suspend fun deleteSentence(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteSentenceById(id)
    }

    /**
     * Loads real example sentences from the imported dictionary entries (`entries.examples_en` and `examples_ar`)
     * so the user can also browse and bookmark example sentences directly from the Sentences screen.
     */
    suspend fun getDictionaryExamplesForQuickAdd(limit: Int = 40): List<SentenceEntity> = withContext(Dispatchers.IO) {
        val entries = dao.getEntriesWithExamples(limit = limit)
        val result = ArrayList<SentenceEntity>()
        val seen = HashSet<String>()
        for (entry in entries) {
            val enList = entry.examplesEn.split(" | ").map { it.trim() }.filter { it.isNotEmpty() }
            val arList = entry.examplesAr.split(" | ").map { it.trim() }
            for (i in enList.indices) {
                val en = enList[i]
                val key = en.lowercase()
                if (seen.add(key)) {
                    val ar = arList.getOrNull(i).orEmpty()
                    result.add(
                        SentenceEntity(
                            id = 0L,
                            englishText = en,
                            arabicText = ar,
                            sourceWord = entry.text,
                            audioUrl = ""
                        )
                    )
                }
            }
            if (result.size >= limit) break
        }
        result
    }

    private suspend fun refreshDeckProgressCounts(deckId: String) {
        val currentDecks = dao.getDecksList()
        if (currentDecks.isEmpty()) return
        val totalFavs = dao.getFavoriteCountForDeck("oxford5000") +
            dao.getFavoriteCountForDeck("oxford_opal") +
            dao.getFavoriteCountForDeck("oxford_phrases") +
            dao.getFavoriteCountForDeck("everyday_expressions") +
            dao.getFavoriteCountForDeck("essential_idioms") +
            dao.getFavoriteCountForDeck("ielts_core") +
            dao.getFavoriteCountForDeck("it_industry") +
            dao.getFavoriteCountForDeck("cefr_c1") +
            dao.getFavoriteCountForDeck("pte_academic") +
            dao.getFavoriteCountForDeck("business_english") +
            dao.getFavoriteCountForDeck("career_interview") +
            dao.getFavoriteCountForDeck("medical_healthcare")

        val updatedDecks = currentDecks.map { deck ->
            when (deck.id) {
                deckId -> {
                    val mastered = dao.getMasteredCountForDeck(deckId)
                    val fav = dao.getFavoriteCountForDeck(deckId)
                    deck.copy(masteredCount = mastered, favoriteCount = fav)
                }
                SAVED_WORDS_DECK_ID -> {
                    deck.copy(rowCount = totalFavs, uniqueWordCount = totalFavs, favoriteCount = totalFavs)
                }
                else -> deck
            }
        }
        dao.upsertDecks(updatedDecks)
    }

    fun buildCardKey(deckId: String, wordText: String): String {
        return "${deckId}:${wordText.trim().lowercase()}"
    }

    /**
     * Locates the 7 real JSON files in assets/data_import, assets/, internal filesDir, or project root.
     */
    private fun locateJsonSources(): Pair<List<ResolvedJsonSource>, List<String>> {
        val resolved = mutableListOf<ResolvedJsonSource>()
        val missing = mutableListOf<String>()

        val assetDataImportFiles = runCatching {
            context.assets.list("data_import")?.toSet() ?: emptySet()
        }.getOrDefault(emptySet())

        val assetRootFiles = runCatching {
            context.assets.list("")?.toSet() ?: emptySet()
        }.getOrDefault(emptySet())

        val candidateDirs = listOfNotNull(
            File(context.filesDir, "data_import"),
            context.filesDir,
            context.getExternalFilesDir(null)?.let { File(it, "data_import") },
            File("data_import"),
            File("app/src/main/assets/data_import"),
            File("../data_import")
        )

        val allFilesToLookUp = REQUIRED_JSON_FILES + OPTIONAL_GENERATED_FILES
        for (fileName in allFilesToLookUp) {
            val isRequired = fileName in REQUIRED_JSON_FILES
            when {
                fileName in assetDataImportFiles -> {
                    val assetPath = "app/src/main/assets/data_import/$fileName"
                    resolved.add(
                        ResolvedJsonSource(
                            fileName = fileName,
                            exactPath = assetPath,
                            openStream = { context.assets.open("data_import/$fileName") }
                        )
                    )
                }
                fileName in assetRootFiles -> {
                    val assetPath = "app/src/main/assets/$fileName"
                    resolved.add(
                        ResolvedJsonSource(
                            fileName = fileName,
                            exactPath = assetPath,
                            openStream = { context.assets.open(fileName) }
                        )
                    )
                }
                else -> {
                    val diskFile = candidateDirs
                        .map { File(it, fileName) }
                        .firstOrNull { it.exists() && it.isFile && it.canRead() }

                    if (diskFile != null) {
                        resolved.add(
                            ResolvedJsonSource(
                                fileName = fileName,
                                exactPath = diskFile.absolutePath,
                                openStream = { diskFile.inputStream() }
                            )
                        )
                    } else if (isRequired) {
                        missing.add(fileName)
                    }
                }
            }
        }

        return resolved to missing
    }

    /**
     * Imports user-selected JSON files via Android Storage Access Framework (ContentResolver)
     * using content-only upsert by ID (`upsertEntriesBatchPreservingUserEdits`).
     */
    suspend fun importFromContentUris(uris: List<Uri>) = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext
        val dataImportDir = File(context.filesDir, "data_import").apply { mkdirs() }

        for (uri in uris) {
            val displayName = queryDisplayName(uri) ?: continue
            val matchedName = (REQUIRED_JSON_FILES + OPTIONAL_GENERATED_FILES).firstOrNull {
                displayName.equals(it, ignoreCase = true)
            } ?: displayName

            context.contentResolver.openInputStream(uri)?.use { input ->
                val targetFile = File(dataImportDir, matchedName)
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        ensureImportedOnFirstLaunch(forceRecheck = true)
    }

    private fun queryDisplayName(uri: Uri): String? {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                cursor.getString(nameIndex)
            } else {
                uri.lastPathSegment
            }
        } ?: uri.lastPathSegment
    }

    suspend fun clearAndReimport() = withContext(Dispatchers.IO) {
        val savedTranslations = dao.getAllTranslatedEntries()
        dao.deleteAllEntries()
        ensureImportedOnFirstLaunch(forceRecheck = true)
        if (savedTranslations.isNotEmpty()) {
            dao.upsertEntriesBatchPreservingUserEdits(savedTranslations)
            refreshAllDeckCounts()
        }
    }

    /**
     * Imports or upserts the 7 core JSON files (and optional ielts_core.json / it_industry.json)
     * in batches of 500 inside a Room transaction using `upsertEntriesBatchPreservingUserEdits`.
     *
     * - If `data_version` increased or `forceRecheck = true`, upserts content fields by `id`
     *   (`arabic`, `ipa`, `definition_en`, `examples_en`, `examples_ar`, `level`, `pos`, `audio_url`)
     *   without ever touching `word_progress` (favorites, mastered, SRS) or `sentences`,
     *   and never overwriting entries where `arabic_source == 'user'`.
     */
    suspend fun ensureImportedOnFirstLaunch(forceRecheck: Boolean = false) = withContext(Dispatchers.IO) {
        try {
            seedDefaultFeatureFlags()
            seedInitialDecks()

            val fakeCount = dao.getFakeLegacyEntryCount()
            if (fakeCount > 0) {
                dao.deleteAllEntries()
                refreshAllDeckCounts()
            }

            val (resolvedSources, missing) = locateJsonSources()
            val foundPathsMap = resolvedSources.associate { it.fileName to it.exactPath }

            val currentTotal = dao.getTotalEntryCount()
            val oxfordCount = dao.getEntryCountForDeck("oxford5000")
            val opalCount = dao.getEntryCountForDeck("oxford_opal")
            val phrasesCount = dao.getEntryCountForDeck("oxford_phrases")
            val expressionsCount = dao.getEntryCountForDeck("everyday_expressions")
            val idiomsCount = dao.getEntryCountForDeck("essential_idioms")
            val dbDataVersion = dao.getMaxDataVersion() ?: 0

            val coreCountsMatchExact = oxfordCount == 5948 &&
                opalCount == 2096 &&
                phrasesCount == 750 &&
                expressionsCount == 600 &&
                idiomsCount == 468

            val ieltsSourceExists = resolvedSources.any { it.fileName == "ielts_core.json" }
            val itSourceExists = resolvedSources.any { it.fileName == "it_industry.json" }
            val cefrC1SourceExists = resolvedSources.any { it.fileName == "cefr_c1.json" }
            val pteSourceExists = resolvedSources.any { it.fileName == "pte_academic.json" }
            val businessSourceExists = resolvedSources.any { it.fileName == "business_english.json" }
            val careerSourceExists = resolvedSources.any { it.fileName == "career_interview.json" }
            val medicalSourceExists = resolvedSources.any { it.fileName == "medical_healthcare.json" }

            val ieltsNeedsImport = ieltsSourceExists && dao.getEntryCountForDeck("ielts_core") == 0
            val itNeedsImport = itSourceExists && dao.getEntryCountForDeck("it_industry") == 0
            val cefrC1NeedsImport = cefrC1SourceExists && dao.getEntryCountForDeck("cefr_c1") == 0
            val pteNeedsImport = pteSourceExists && dao.getEntryCountForDeck("pte_academic") == 0
            val businessNeedsImport = businessSourceExists && dao.getEntryCountForDeck("business_english") == 0
            val careerNeedsImport = careerSourceExists && dao.getEntryCountForDeck("career_interview") == 0
            val medicalNeedsImport = medicalSourceExists && dao.getEntryCountForDeck("medical_healthcare") == 0

            val anyOptionalNeedsImport = ieltsNeedsImport ||
                itNeedsImport ||
                cefrC1NeedsImport ||
                pteNeedsImport ||
                businessNeedsImport ||
                careerNeedsImport ||
                medicalNeedsImport

            val versionNeedsUpsert = dbDataVersion < DATA_VERSION

            if (dao.getWordDetailsCount() < 10317 || versionNeedsUpsert) {
                seedAiWordDetailsIfNeeded()
            }

            if (coreCountsMatchExact && !anyOptionalNeedsImport && !versionNeedsUpsert && missing.isEmpty() && !forceRecheck) {
                refreshAllDeckCounts()
                _importState.value = ImportProgressState(
                    isImporting = false,
                    importedRows = currentTotal,
                    totalExpectedRows = TOTAL_EXPECTED_ROWS,
                    isCompleted = true,
                    missingFiles = emptyList(),
                    foundFilePaths = foundPathsMap
                )
                return@withContext
            }

            if (resolvedSources.isEmpty()) {
                refreshAllDeckCounts()
                _importState.value = ImportProgressState(
                    isImporting = false,
                    importedRows = dao.getTotalEntryCount(),
                    totalExpectedRows = TOTAL_EXPECTED_ROWS,
                    isCompleted = false,
                    missingFiles = missing,
                    foundFilePaths = emptyMap(),
                    error = "Waiting for the 7 real JSON files (${missing.joinToString(", ")})."
                )
                return@withContext
            }

            var runningTotalProcessed = 0
            var batchCounter = 0

            for (source in resolvedSources) {
                _importState.value = ImportProgressState(
                    isImporting = true,
                    currentFile = source.fileName,
                    currentFileSourcePath = source.exactPath,
                    importedRows = runningTotalProcessed,
                    totalExpectedRows = TOTAL_EXPECTED_ROWS,
                    batchNumber = batchCounter,
                    missingFiles = missing,
                    foundFilePaths = foundPathsMap
                )

                val processedInFile = importJsonStreamInBatches(
                    source = source,
                    onBatchCommitted = { batchRowsCount ->
                        runningTotalProcessed += batchRowsCount
                        batchCounter++
                        _importState.value = _importState.value.copy(
                            currentFile = source.fileName,
                            currentFileSourcePath = source.exactPath,
                            importedRows = runningTotalProcessed,
                            batchNumber = batchCounter
                        )
                    }
                )
                if (processedInFile == 0) {
                    // File was empty or unreadable
                }
            }

            refreshAllDeckCounts()
            val finalDbCount = dao.getTotalEntryCount()

            _importState.value = ImportProgressState(
                isImporting = false,
                currentFile = "",
                importedRows = finalDbCount,
                totalExpectedRows = TOTAL_EXPECTED_ROWS,
                batchNumber = batchCounter,
                isCompleted = missing.isEmpty() && finalDbCount >= TOTAL_EXPECTED_ROWS,
                missingFiles = missing,
                foundFilePaths = foundPathsMap,
                error = if (missing.isNotEmpty()) {
                    "Missing ${missing.size} of 7 required JSON files: ${missing.joinToString(", ")}"
                } else {
                    null
                }
            )
        } catch (e: Exception) {
            _importState.value = _importState.value.copy(
                isImporting = false,
                error = e.message ?: "Import error"
            )
        }
    }

    private suspend fun importJsonStreamInBatches(
        source: ResolvedJsonSource,
        onBatchCommitted: (Int) -> Unit
    ): Int {
        val rawText = source.openStream().bufferedReader(Charsets.UTF_8).use { it.readText() }
        val rawEntries = json.decodeFromString<List<RawJsonEntry>>(rawText)

        var totalProcessed = 0
        val chunks = rawEntries.chunked(BATCH_SIZE)

        for (chunk in chunks) {
            val entities = chunk.mapNotNull { raw ->
                mapRawToEntity(raw, source.fileName)
            }
            if (entities.isNotEmpty()) {
                dao.upsertEntriesBatchPreservingUserEdits(entities)
                totalProcessed += entities.size
                onBatchCommitted(entities.size)
            }
        }
        return totalProcessed
    }

    /**
     * Maps the JSON entry directly as written in the file:
     * - Uses `raw.id.content` as `id` (preserved as-is)
     * - Uses `raw.text` as `text` (no suffixes or numbers)
     * - Merges `oxford5000_part1/2/3.json` into deck `"oxford5000"`
     * - Marks `arabicSource = "file"` whenever `arabic` is non-empty in the file (unless explicitly set to "ai" or "user")
     * - Sets `dataVersion = DATA_VERSION`
     */
    private fun mapRawToEntity(raw: RawJsonEntry, fallbackFileName: String): EntryEntity? {
        val idTrimmed = raw.id.content.trim()
        val textTrimmed = raw.text.trim()
        if (idTrimmed.isEmpty() || textTrimmed.isEmpty()) return null

        val normalizedDeck = when {
            raw.deck.startsWith("oxford5000", ignoreCase = true) ||
                fallbackFileName.startsWith("oxford5000", ignoreCase = true) -> "oxford5000"
            raw.deck.isNotBlank() -> raw.deck.trim()
            else -> fallbackFileName.removeSuffix(".json")
        }

        val arabicText = raw.arabic.trim()
        val resolvedArabicSource = if (arabicText.isNotEmpty()) {
            when (raw.arabicSource.trim().lowercase()) {
                "ai" -> "ai"
                "user" -> "user"
                else -> "file"
            }
        } else {
            ""
        }

        return EntryEntity(
            id = idTrimmed,
            type = raw.type.ifBlank { "word" },
            deck = normalizedDeck,
            text = textTrimmed,
            pos = raw.pos,
            level = raw.level,
            definitionEn = raw.definitionEn,
            noteEn = raw.noteEn,
            examplesEn = raw.examplesEn,
            examplesAr = raw.examplesAr,
            definitionUrl = raw.definitionUrl,
            audioUrl = raw.audioUrl,
            arabic = arabicText,
            arabicSource = resolvedArabicSource,
            ipa = raw.ipa,
            syllables = "",
            extraExamplesJson = "",
            dataVersion = DATA_VERSION
        )
    }

    private suspend fun seedInitialDecks() {
        val existingDecks = dao.getDecksList()
        if (existingDecks.size >= 13) return

        val initialDecks = buildDeckEntitiesWithCounts(
            oxfordCount = 0, oxfordUnique = 0, oxfordMastered = 0, oxfordFav = 0,
            opalCount = 0, opalUnique = 0, opalMastered = 0, opalFav = 0,
            phrasesCount = 0, phrasesUnique = 0, phrasesMastered = 0, phrasesFav = 0,
            expressionsCount = 0, expressionsUnique = 0, expressionsMastered = 0, expressionsFav = 0,
            idiomsCount = 0, idiomsUnique = 0, idiomsMastered = 0, idiomsFav = 0,
            ieltsCount = 0, ieltsUnique = 0, ieltsMastered = 0, ieltsFav = 0,
            itCount = 0, itUnique = 0, itMastered = 0, itFav = 0,
            cefrC1Count = 0, cefrC1Unique = 0, cefrC1Mastered = 0, cefrC1Fav = 0,
            pteCount = 0, pteUnique = 0, pteMastered = 0, pteFav = 0,
            businessCount = 0, businessUnique = 0, businessMastered = 0, businessFav = 0,
            careerCount = 0, careerUnique = 0, careerMastered = 0, careerFav = 0,
            medicalCount = 0, medicalUnique = 0, medicalMastered = 0, medicalFav = 0,
            savedFavTotal = 0
        )
        val existingIds = existingDecks.map { it.id }.toSet()
        val missingDecks = initialDecks.filter { it.id !in existingIds }
        if (missingDecks.isNotEmpty()) {
            dao.upsertDecks(missingDecks)
        }
    }

    suspend fun refreshAllDeckCounts() {
        val oxfordCount = dao.getEntryCountForDeck("oxford5000")
        val oxfordUnique = dao.getUniqueWordCountForDeck("oxford5000")
        val oxfordMastered = dao.getMasteredCountForDeck("oxford5000")
        val oxfordFav = dao.getFavoriteCountForDeck("oxford5000")

        val opalCount = dao.getEntryCountForDeck("oxford_opal")
        val opalUnique = dao.getUniqueWordCountForDeck("oxford_opal")
        val opalMastered = dao.getMasteredCountForDeck("oxford_opal")
        val opalFav = dao.getFavoriteCountForDeck("oxford_opal")

        val phrasesCount = dao.getEntryCountForDeck("oxford_phrases")
        val phrasesUnique = dao.getUniqueWordCountForDeck("oxford_phrases")
        val phrasesMastered = dao.getMasteredCountForDeck("oxford_phrases")
        val phrasesFav = dao.getFavoriteCountForDeck("oxford_phrases")

        val expressionsCount = dao.getEntryCountForDeck("everyday_expressions")
        val expressionsUnique = dao.getUniqueWordCountForDeck("everyday_expressions")
        val expressionsMastered = dao.getMasteredCountForDeck("everyday_expressions")
        val expressionsFav = dao.getFavoriteCountForDeck("everyday_expressions")

        val idiomsCount = dao.getEntryCountForDeck("essential_idioms")
        val idiomsUnique = dao.getUniqueWordCountForDeck("essential_idioms")
        val idiomsMastered = dao.getMasteredCountForDeck("essential_idioms")
        val idiomsFav = dao.getFavoriteCountForDeck("essential_idioms")

        val ieltsCount = dao.getEntryCountForDeck("ielts_core")
        val ieltsUnique = dao.getUniqueWordCountForDeck("ielts_core")
        val ieltsMastered = dao.getMasteredCountForDeck("ielts_core")
        val ieltsFav = dao.getFavoriteCountForDeck("ielts_core")

        val itCount = dao.getEntryCountForDeck("it_industry")
        val itUnique = dao.getUniqueWordCountForDeck("it_industry")
        val itMastered = dao.getMasteredCountForDeck("it_industry")
        val itFav = dao.getFavoriteCountForDeck("it_industry")

        val cefrC1Count = dao.getEntryCountForDeck("cefr_c1")
        val cefrC1Unique = dao.getUniqueWordCountForDeck("cefr_c1")
        val cefrC1Mastered = dao.getMasteredCountForDeck("cefr_c1")
        val cefrC1Fav = dao.getFavoriteCountForDeck("cefr_c1")

        val pteCount = dao.getEntryCountForDeck("pte_academic")
        val pteUnique = dao.getUniqueWordCountForDeck("pte_academic")
        val pteMastered = dao.getMasteredCountForDeck("pte_academic")
        val pteFav = dao.getFavoriteCountForDeck("pte_academic")

        val businessCount = dao.getEntryCountForDeck("business_english")
        val businessUnique = dao.getUniqueWordCountForDeck("business_english")
        val businessMastered = dao.getMasteredCountForDeck("business_english")
        val businessFav = dao.getFavoriteCountForDeck("business_english")

        val careerCount = dao.getEntryCountForDeck("career_interview")
        val careerUnique = dao.getUniqueWordCountForDeck("career_interview")
        val careerMastered = dao.getMasteredCountForDeck("career_interview")
        val careerFav = dao.getFavoriteCountForDeck("career_interview")

        val medicalCount = dao.getEntryCountForDeck("medical_healthcare")
        val medicalUnique = dao.getUniqueWordCountForDeck("medical_healthcare")
        val medicalMastered = dao.getMasteredCountForDeck("medical_healthcare")
        val medicalFav = dao.getFavoriteCountForDeck("medical_healthcare")

        val savedFavTotal = oxfordFav + opalFav + phrasesFav + expressionsFav + idiomsFav +
            ieltsFav + itFav + cefrC1Fav + pteFav + businessFav + careerFav + medicalFav

        val decks = buildDeckEntitiesWithCounts(
            oxfordCount, oxfordUnique, oxfordMastered, oxfordFav,
            opalCount, opalUnique, opalMastered, opalFav,
            phrasesCount, phrasesUnique, phrasesMastered, phrasesFav,
            expressionsCount, expressionsUnique, expressionsMastered, expressionsFav,
            idiomsCount, idiomsUnique, idiomsMastered, idiomsFav,
            ieltsCount, ieltsUnique, ieltsMastered, ieltsFav,
            itCount, itUnique, itMastered, itFav,
            cefrC1Count, cefrC1Unique, cefrC1Mastered, cefrC1Fav,
            pteCount, pteUnique, pteMastered, pteFav,
            businessCount, businessUnique, businessMastered, businessFav,
            careerCount, careerUnique, careerMastered, careerFav,
            medicalCount, medicalUnique, medicalMastered, medicalFav,
            savedFavTotal
        )
        dao.upsertDecks(decks)
    }

    private fun buildDeckEntitiesWithCounts(
        oxfordCount: Int, oxfordUnique: Int, oxfordMastered: Int, oxfordFav: Int,
        opalCount: Int, opalUnique: Int, opalMastered: Int, opalFav: Int,
        phrasesCount: Int, phrasesUnique: Int, phrasesMastered: Int, phrasesFav: Int,
        expressionsCount: Int, expressionsUnique: Int, expressionsMastered: Int, expressionsFav: Int,
        idiomsCount: Int, idiomsUnique: Int, idiomsMastered: Int, idiomsFav: Int,
        ieltsCount: Int, ieltsUnique: Int, ieltsMastered: Int, ieltsFav: Int,
        itCount: Int, itUnique: Int, itMastered: Int, itFav: Int,
        cefrC1Count: Int, cefrC1Unique: Int, cefrC1Mastered: Int, cefrC1Fav: Int,
        pteCount: Int, pteUnique: Int, pteMastered: Int, pteFav: Int,
        businessCount: Int, businessUnique: Int, businessMastered: Int, businessFav: Int,
        careerCount: Int, careerUnique: Int, careerMastered: Int, careerFav: Int,
        medicalCount: Int, medicalUnique: Int, medicalMastered: Int, medicalFav: Int,
        savedFavTotal: Int
    ): List<DeckEntity> = listOf(
        DeckEntity(
            id = SAVED_WORDS_DECK_ID,
            title = "Saved Words",
            arabicSubtitle = "الكلمات المحفوظة والمفضلة",
            category = "Personal Favorites",
            coverColorHex = "#2B2210",
            accentColorHex = "#F5C542",
            rowCount = savedFavTotal,
            uniqueWordCount = savedFavTotal,
            masteredCount = 0,
            favoriteCount = savedFavTotal,
            displayOrder = 0
        ),
        DeckEntity(
            id = "oxford5000",
            title = "Oxford 5000",
            arabicSubtitle = "أهم ٥٠٠٠ كلمة أكاديمية وعامة",
            category = "Core Vocabulary (Merged Part 1–3)",
            coverColorHex = "#1E293B",
            accentColorHex = "#F5C542",
            rowCount = oxfordCount,
            uniqueWordCount = oxfordUnique,
            masteredCount = oxfordMastered,
            favoriteCount = oxfordFav,
            displayOrder = 1
        ),
        DeckEntity(
            id = "oxford_opal",
            title = "Oxford OPAL",
            arabicSubtitle = "المفردات الأكاديمية (OPAL)",
            category = "Academic Word List",
            coverColorHex = "#172554",
            accentColorHex = "#60A5FA",
            rowCount = opalCount,
            uniqueWordCount = opalUnique,
            masteredCount = opalMastered,
            favoriteCount = opalFav,
            displayOrder = 2
        ),
        DeckEntity(
            id = "oxford_phrases",
            title = "Oxford Phrases",
            arabicSubtitle = "العبارات والتراكيب الأساسية",
            category = "Phrases & Collocations",
            coverColorHex = "#064E3B",
            accentColorHex = "#34D399",
            rowCount = phrasesCount,
            uniqueWordCount = phrasesUnique,
            masteredCount = phrasesMastered,
            favoriteCount = phrasesFav,
            displayOrder = 3
        ),
        DeckEntity(
            id = "everyday_expressions",
            title = "Everyday Expressions",
            arabicSubtitle = "تعبيرات الحياة اليومية والمحادثة",
            category = "Spoken English",
            coverColorHex = "#3B0764",
            accentColorHex = "#C084FC",
            rowCount = expressionsCount,
            uniqueWordCount = expressionsUnique,
            masteredCount = expressionsMastered,
            favoriteCount = expressionsFav,
            displayOrder = 4
        ),
        DeckEntity(
            id = "essential_idioms",
            title = "Essential Idioms",
            arabicSubtitle = "التعابير الاصطلاحية الشائعة",
            category = "Idioms & Figurative",
            coverColorHex = "#451A03",
            accentColorHex = "#FBBF24",
            rowCount = idiomsCount,
            uniqueWordCount = idiomsUnique,
            masteredCount = idiomsMastered,
            favoriteCount = idiomsFav,
            displayOrder = 5
        ),
        DeckEntity(
            id = "ielts_core",
            title = "IELTS Core",
            arabicSubtitle = "مفردات الآيلتس الأساسية",
            category = "Exam Preparation",
            coverColorHex = "#1F242D",
            accentColorHex = "#9CA3AF",
            rowCount = ieltsCount,
            uniqueWordCount = ieltsUnique,
            masteredCount = ieltsMastered,
            favoriteCount = ieltsFav,
            displayOrder = 6
        ),
        DeckEntity(
            id = "it_industry",
            title = "IT & Tech Industry",
            arabicSubtitle = "مصطلحات البرمجة وتقنية المعلومات",
            category = "Professional English",
            coverColorHex = "#18181B",
            accentColorHex = "#38BDF8",
            rowCount = itCount,
            uniqueWordCount = itUnique,
            masteredCount = itMastered,
            favoriteCount = itFav,
            displayOrder = 7
        ),
        DeckEntity(
            id = "cefr_c1",
            title = "CEFR C1 Advanced",
            arabicSubtitle = "المفردات المتقدمة للمستوى C1",
            category = "Advanced English",
            coverColorHex = "#1E1B4B",
            accentColorHex = "#818CF8",
            rowCount = cefrC1Count,
            uniqueWordCount = cefrC1Unique,
            masteredCount = cefrC1Mastered,
            favoriteCount = cefrC1Fav,
            displayOrder = 8
        ),
        DeckEntity(
            id = "pte_academic",
            title = "PTE Academic",
            arabicSubtitle = "مفردات اختبار بيرسون الأكاديمي",
            category = "Exam Preparation",
            coverColorHex = "#0C4A6E",
            accentColorHex = "#38BDF8",
            rowCount = pteCount,
            uniqueWordCount = pteUnique,
            masteredCount = pteMastered,
            favoriteCount = pteFav,
            displayOrder = 9
        ),
        DeckEntity(
            id = "business_english",
            title = "Business English",
            arabicSubtitle = "الإنجليزية المهنية وإدارة الأعمال",
            category = "Professional English",
            coverColorHex = "#172554",
            accentColorHex = "#F59E0B",
            rowCount = businessCount,
            uniqueWordCount = businessUnique,
            masteredCount = businessMastered,
            favoriteCount = businessFav,
            displayOrder = 10
        ),
        DeckEntity(
            id = "career_interview",
            title = "Job Interview & Career",
            arabicSubtitle = "مقابلات العمل والتوظيف الدولي",
            category = "Career Skills",
            coverColorHex = "#064E3B",
            accentColorHex = "#10B981",
            rowCount = careerCount,
            uniqueWordCount = careerUnique,
            masteredCount = careerMastered,
            favoriteCount = careerFav,
            displayOrder = 11
        ),
        DeckEntity(
            id = "medical_healthcare",
            title = "Medical & Healthcare",
            arabicSubtitle = "المصطلحات الطبية والرعاية الصحية",
            category = "Healthcare & OET",
            coverColorHex = "#450A0A",
            accentColorHex = "#EF4444",
            rowCount = medicalCount,
            uniqueWordCount = medicalUnique,
            masteredCount = medicalMastered,
            favoriteCount = medicalFav,
            displayOrder = 12
        )
    )

    private suspend fun seedDefaultFeatureFlags() {
        val flags = listOf(
            FeatureFlagEntity(
                id = "pronunciation_coach",
                title = "Pronunciation Coach",
                description = "Real-time phoneme and stress feedback with Arabic articulation tips.",
                badge = "Coming soon",
                isEnabled = false
            ),
            FeatureFlagEntity(
                id = "watch_and_learn",
                title = "Watch & Learn",
                description = "Dual-subtitle video player inspired by Trancy with instant word tap.",
                badge = "Coming soon",
                isEnabled = false
            ),
            FeatureFlagEntity(
                id = "ai_tutor_chat",
                title = "AI Tutor Chat",
                description = "Interactive bilingual conversation practice using your active Word Books.",
                badge = "Coming soon",
                isEnabled = false
            )
        )
        dao.upsertFeatureFlags(flags)
    }

    /**
     * Exports all permanently saved AI word details to JSON.
     */
    suspend fun exportWordDetailsJson(): String = withContext(Dispatchers.IO) {
        val list = dao.getAllWordDetails()
        json.encodeToString(list)
    }

    private suspend fun seedAiWordDetailsIfNeeded() = withContext(Dispatchers.IO) {
        if (dao.getWordDetailsCount() >= 10317) return@withContext
        try {
            val stream = context.assets.open("data_import/ai_word_details.json")
            val rawText = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val details = json.decodeFromString<List<WordDetailEntity>>(rawText)
            details.chunked(BATCH_SIZE).forEach { batch ->
                dao.insertWordDetails(batch)
            }
        } catch (e: Exception) {
            // Fallback / log
        }
    }

    /**
     * Imports AI word details from JSON without ever overwriting existing entries.
     */
    suspend fun importWordDetailsJson(jsonText: String): Int = withContext(Dispatchers.IO) {
        val items = runCatching { json.decodeFromString<List<WordDetailEntity>>(jsonText) }.getOrDefault(emptyList())
        if (items.isEmpty()) return@withContext 0
        val countBefore = dao.getWordDetailsCount()
        items.chunked(500).forEach { chunk ->
            dao.insertWordDetails(chunk)
        }
        val countAfter = dao.getWordDetailsCount()
        (countAfter - countBefore).coerceAtLeast(0)
    }
}

