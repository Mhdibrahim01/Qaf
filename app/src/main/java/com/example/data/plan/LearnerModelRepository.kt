package com.example.data.plan

import android.content.Context
import androidx.compose.runtime.Immutable
import com.example.data.local.EntryEntity
import com.example.data.local.ReadingTextEntity
import com.example.data.local.StudyPlanProfileEntity
import com.example.data.local.WordBooksDao
import com.example.data.local.WordProgressEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlin.math.roundToInt

enum class WordStage {
    NEW,
    SEEN,
    LEARNING,
    FAMILIAR,
    MATURE
}

typealias WordLifecycleStage = WordStage

@Immutable
data class WordLifecycleFunnel(
    val totalPoolCount: Int = 0,
    val newCount: Int = 0,
    val seenCount: Int = 0,
    val learningCount: Int = 0,
    val familiarCount: Int = 0,
    val matureCount: Int = 0,
    val conqueredCount: Int = 0,
    val conqueredWords: List<ConqueredWordBadge> = emptyList()
) {
    val activeStudiedCount: Int
        get() = seenCount + learningCount + familiarCount + matureCount
}

@Immutable
data class ConqueredWordBadge(
    val cardKey: String,
    val text: String,
    val arabic: String,
    val againCount: Int,
    val intervalDays: Int,
    val conqueredTimestamp: Long
)

@Immutable
data class LifecycleStageWordItem(
    val headword: String,
    val deck: String = "oxford5000",
    val pos: String = "word",
    val arabic: String = "",
    val intervalDays: Int = 0,
    val repetitions: Int = 0,
    val stage: WordStage = WordStage.NEW
)

@Immutable
data class DeckCoverageSummary(
    val deckId: String,
    val titleEn: String = deckId,
    val totalWords: Int,
    val seenOrBetterCount: Int,
    val familiarOrMatureCount: Int,
    val matureCount: Int,
    val coveragePercent: Int
) {
    val seenOrHigherWords: Int
        get() = seenOrBetterCount
}

@Immutable
data class StoryReadabilityReport(
    val textId: String,
    val macroTopic: String,
    val readabilityPercent: Int,
    val focusWords: List<String>,
    val knownTokenCount: Int,
    val totalTokenCount: Int,
    val badgeLabelEn: String
)

@Immutable
data class CefrReadinessSummary(
    val currentLevel: String,
    val nextLevel: String?,
    val overallReadinessPercent: Int,
    val vocabCoveragePercent: Int,
    val grammarMasteryPercent: Int,
    val readingMasteryPercent: Int,
    val isReadyForLevelUp: Boolean,
    val vocabFamiliarOrMatureCount: Int = 0,
    val vocabTargetCount: Int = 120,
    val grammarPassedCount: Int = 0,
    val grammarTotalCount: Int = 0,
    val readingCompletedCount: Int = 0,
    val readingTargetCount: Int = 0
) {
    val compositeReadinessPercent: Int
        get() = overallReadinessPercent
    val vocabScorePercent: Int
        get() = vocabCoveragePercent
    val grammarScorePercent: Int
        get() = grammarMasteryPercent
    val readingScorePercent: Int
        get() = readingMasteryPercent
}

@Immutable
data class WeeklyGrowthReport(
    val hasSufficientHistory: Boolean = false,
    val wordsPromotedToFamiliarOrMature: Int = 0,
    val wordsLearnedPrevWeek: Int = 0,
    val wordsConqueredThisWeek: Int = 0,
    val storiesCompletedThisWeek: Int = 0,
    val storiesCompletedPrevWeek: Int = 0,
    val grammarCompletedThisWeek: Int = 0,
    val notebookResolvedThisWeek: Int = 0,
    val currentWeekAccuracyPercent: Int? = null,
    val previousWeekAccuracyPercent: Int? = null,
    val accuracyDeltaPercent: Int? = null
) {
    val wordsLearnedThisWeek: Int
        get() = wordsPromotedToFamiliarOrMature
    val accuracyThisWeekPercent: Int?
        get() = currentWeekAccuracyPercent
    val accuracyPrevWeekPercent: Int?
        get() = previousWeekAccuracyPercent
}

@Immutable
data class AdaptiveInsightBanner(
    val id: String,
    val titleEn: String,
    val titleAr: String,
    val reasonEn: String,
    val badgeText: String,
    val messageAr: String = titleAr,
    val actionRoute: String? = null,
    val actionLabelEn: String? = null,
    val suggestedLevel: String? = null
) {
    val messageEn: String
        get() = reasonEn
}

@Immutable
data class LearnerModelSnapshot(
    val funnel: WordLifecycleFunnel = WordLifecycleFunnel(),
    val deckCoverage: List<DeckCoverageSummary> = emptyList(),
    val cefrReadiness: CefrReadinessSummary = CefrReadinessSummary("B1", "B2", 0, 0, 0, 0, false),
    val weeklyGrowth: WeeklyGrowthReport = WeeklyGrowthReport(),
    val personalPaceSecondsPerWord: Float = AdaptiveConfig.DEFAULT_SECONDS_PER_WORD,
    val rollingSevenDayAccuracyPercent: Int? = null,
    val matureRetentionPercent: Int? = null,
    val totalAttemptsCount: Int = 0,
    val isColdStart: Boolean = true,
    val activeInsight: AdaptiveInsightBanner? = null,
    val storyReadabilityById: Map<String, StoryReadabilityReport> = emptyMap(),
    val wordStageByHeadword: Map<String, WordStage> = emptyMap()
) {
    val conqueredWords: List<ConqueredWordBadge>
        get() = funnel.conqueredWords
    val deckCoverages: List<DeckCoverageSummary>
        get() = deckCoverage
}

@Serializable
data class ReadingWordLinkItem(
    @SerialName("text_id") val id: String = "",
    val level: String = "B1",
    @SerialName("raw_topic") val rawTopic: String = "",
    @SerialName("macro_topic") val macroTopic: String = "daily_life",
    @SerialName("target_words") val targetWords: List<String> = emptyList(),
    @SerialName("linked_headwords") val linkedHeadwords: List<String> = emptyList(),
    @SerialName("content_tokens") val contentTokens: List<String> = emptyList()
)

class LearnerModelRepository(
    private val context: Context,
    private val dao: WordBooksDao,
    private val timeProvider: TimeProvider = SystemTimeProvider()
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Volatile
    private var cachedReadingLinks: Map<String, ReadingWordLinkItem>? = null

    companion object {
        private val STOPWORDS = setOf(
            "a", "an", "the", "and", "or", "but", "if", "in", "on", "at", "to", "for",
            "of", "with", "by", "from", "up", "about", "into", "over", "after", "is",
            "am", "are", "was", "were", "be", "been", "being", "have", "has", "had",
            "do", "does", "did", "will", "would", "shall", "should", "can", "could",
            "may", "might", "must", "i", "you", "he", "she", "it", "we", "they", "me",
            "him", "her", "us", "them", "my", "your", "his", "its", "our", "their",
            "this", "that", "these", "those", "not", "no", "yes", "so", "too", "very",
            "just", "now", "then", "here", "there", "when", "where", "why", "how",
            "all", "any", "both", "each", "more", "most", "other", "some", "such",
            "than", "as", "who", "what", "which"
        )

        private val DECK_TITLES = mapOf(
            "oxford5000" to "Oxford 5000 Core",
            "oxford_opal" to "Academic (OPAL)",
            "oxford_phrases" to "Oxford Phrases",
            "everyday_expressions" to "Everyday Expressions",
            "career_interview" to "Career & Interviews",
            "it_industry" to "IT & Tech Industry",
            "business_english" to "Business English",
            "medical_healthcare" to "Medical & Healthcare",
            "ielts_core" to "IELTS Vocabulary",
            "pte_academic" to "PTE Academic",
            "cefr_c1" to "Advanced C1 Mastery",
            "essential_idioms" to "Essential Idioms"
        )

        fun classifyWordStage(progress: WordProgressEntity?): WordStage {
            if (progress == null) return WordStage.NEW
            if (progress.isMastered && progress.intervalDays >= AdaptiveConfig.STAGE_MATURE_MIN_INTERVAL_DAYS) {
                return WordStage.MATURE
            }
            if (progress.repetitions >= 1) {
                return when {
                    progress.isMastered || progress.intervalDays >= AdaptiveConfig.STAGE_MATURE_MIN_INTERVAL_DAYS -> WordStage.MATURE
                    progress.intervalDays >= AdaptiveConfig.STAGE_FAMILIAR_MIN_INTERVAL_DAYS -> WordStage.FAMILIAR
                    else -> WordStage.LEARNING
                }
            }
            if (progress.lookupCount > 0 || progress.againCount > 0 || progress.lastReviewedTimestamp > 0L || progress.firstLearnedTimestamp > 0L) {
                return WordStage.SEEN
            }
            return WordStage.NEW
        }

        fun computeWordStage(progress: WordProgressEntity?): WordStage = classifyWordStage(progress)

        fun isWordConquered(progress: WordProgressEntity?): Boolean {
            if (progress == null) return false
            return progress.againCount >= AdaptiveConfig.CONQUERED_MIN_LAPSES &&
                (progress.intervalDays >= AdaptiveConfig.CONQUERED_MIN_INTERVAL_DAYS ||
                    progress.isMastered ||
                    progress.conqueredTimestamp > 0L)
        }

        fun nextCefrLevel(level: String): String? {
            return when (level.trim().uppercase()) {
                "A1" -> "A2"
                "A2" -> "B1"
                "B1" -> "B2"
                "B2" -> "C1"
                else -> null
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun getReadingWordLinks(): Map<String, ReadingWordLinkItem> {
        cachedReadingLinks?.let { return it }
        return synchronized(this) {
            cachedReadingLinks?.let { return it }
            val loaded = try {
                context.assets.open("reading_word_links.json").buffered().use { stream ->
                    val list = json.decodeFromStream(ListSerializer(ReadingWordLinkItem.serializer()), stream)
                    list.associateBy { it.id }
                }
            } catch (_: Exception) {
                emptyMap()
            }
            cachedReadingLinks = loaded
            loaded
        }
    }

    /**
     * Computes story readability % and focus words for a given reading text using the user's
     * current `word_progress` stages and CEFR baseline.
     */
    fun computeStoryReadability(
        story: ReadingTextEntity,
        userLevel: String,
        progressByHeadword: Map<String, WordStage>,
        baselineKnownHeadwords: Set<String>
    ): StoryReadabilityReport {
        val linkItem = getReadingWordLinks()[story.id]
        val macroTopic = AdaptiveConfig.normalizeMacroTopic(linkItem?.macroTopic ?: story.topic)

        val contentTokens = if (linkItem != null && linkItem.contentTokens.isNotEmpty()) {
            linkItem.contentTokens
        } else {
            extractContentTokens(story.sentencesJson)
        }

        val targetWords = if (linkItem != null && linkItem.targetWords.isNotEmpty()) {
            linkItem.targetWords
        } else {
            try {
                json.decodeFromString(ListSerializer(String.serializer()), story.targetWordsJson)
            } catch (_: Exception) {
                emptyList()
            }
        }

        val totalTokens = contentTokens.size.coerceAtLeast(1)
        var knownCount = 0
        for (token in contentTokens) {
            val lower = token.lowercase()
            val stage = progressByHeadword[lower]
            val isKnown = stage == WordStage.FAMILIAR ||
                stage == WordStage.MATURE ||
                stage == WordStage.LEARNING ||
                lower in baselineKnownHeadwords
            if (isKnown) {
                knownCount++
            }
        }

        // Blend token coverage with story level relative to user level so A1/A2/B1/B2 stories
        // reflect realistic readability even before all 21,624 headwords are rated.
        val rawPercent = ((knownCount.toFloat() / totalTokens.toFloat()) * 100f).roundToInt()
        val levelBaseline = when (compareCefrRank(story.level) - compareCefrRank(userLevel)) {
            -3, -2 -> 94
            -1 -> 90
            0 -> 84
            1 -> 74
            else -> 64
        }
        val unstudiedTargetCount = targetWords.count { word ->
            val st = progressByHeadword[word.lowercase()]
            st == null || st == WordStage.NEW || st == WordStage.SEEN
        }
        val learnedTargetBonus = (targetWords.size - unstudiedTargetCount).coerceAtLeast(0) * 2
        val readabilityPercent = ((rawPercent * 0.45f) + (levelBaseline * 0.55f) + learnedTargetBonus)
            .roundToInt()
            .coerceIn(50, 99)

        val focusWords = targetWords.filter { word ->
            val st = progressByHeadword[word.lowercase()]
            st != WordStage.FAMILIAR && st != WordStage.MATURE
        }.ifEmpty { targetWords }.take(AdaptiveConfig.PRE_TEACH_STORY_WORDS_MAX)

        val badgeLabel = "${readabilityPercent}% Readable • ${focusWords.size} Focus Words"
        return StoryReadabilityReport(
            textId = story.id,
            macroTopic = macroTopic,
            readabilityPercent = readabilityPercent,
            focusWords = focusWords,
            knownTokenCount = knownCount,
            totalTokenCount = totalTokens,
            badgeLabelEn = badgeLabel
        )
    }

    /**
     * Computes the full deterministic LearnerModelSnapshot from Room tables.
     */
    suspend fun buildSnapshot(
        profile: StudyPlanProfileEntity? = null,
        nowMillis: Long = timeProvider.currentTimeMillis()
    ): LearnerModelSnapshot = withContext(Dispatchers.IO) {
        val resolvedProfile = profile ?: dao.getStudyPlanProfile() ?: StudyPlanProfileEntity()
        val selectedDecks = parseStringList(resolvedProfile.selectedDecksJson)
            .ifEmpty { listOf("oxford5000", "oxford_opal") }

        dao.clearInvalidConqueredTimestamps()
        val allProgress = dao.getAllWordProgressList()
        val allAttempts = dao.getAllAttempts()
        val isColdStart = allAttempts.size < AdaptiveConfig.COLD_START_MIN_ATTEMPTS

        // 1. Classify word stages across selected decks (and globally for conquered shelf)
        val scopedProgress = if (selectedDecks.isEmpty()) allProgress else allProgress.filter { it.deck in selectedDecks }
        val stageByHeadword = HashMap<String, WordStage>(scopedProgress.size * 2)

        for (wp in scopedProgress) {
            val stage = classifyWordStage(wp)
            val currentBest = stageByHeadword[wp.text.lowercase()]
            if (currentBest == null || stage.ordinal > currentBest.ordinal) {
                stageByHeadword[wp.text.lowercase()] = stage
            }
        }

        val conqueredBadges = mutableListOf<ConqueredWordBadge>()
        for (wp in allProgress) {
            if (isWordConquered(wp)) {
                conqueredBadges.add(
                    ConqueredWordBadge(
                        cardKey = wp.cardKey,
                        text = wp.text,
                        arabic = "",
                        againCount = wp.againCount,
                        intervalDays = wp.intervalDays,
                        conqueredTimestamp = if (wp.conqueredTimestamp > 0L) wp.conqueredTimestamp else wp.lastReviewedTimestamp
                    )
                )
            }
        }

        val seenCount = stageByHeadword.values.count { it == WordStage.SEEN }
        val learningCount = stageByHeadword.values.count { it == WordStage.LEARNING }
        val familiarCount = stageByHeadword.values.count { it == WordStage.FAMILIAR }
        val matureCount = stageByHeadword.values.count { it == WordStage.MATURE }

        // Enrich conquered badges with Arabic translations from entries if available
        val sortedConquered = conqueredBadges
            .sortedByDescending { it.conqueredTimestamp }
            .take(12)
            .map { badge ->
                val entry = dao.getEntriesByHeadwords(listOf(badge.text)).firstOrNull { it.arabic.isNotBlank() }
                badge.copy(arabic = entry?.arabic.orEmpty())
            }

        // 2. Deck coverage summaries
        val deckCoverage = selectedDecks.map { deckId ->
            val totalUnique = dao.getUniqueWordCountForDeck(deckId).coerceAtLeast(1)
            val deckProgress = allProgress.filter { it.deck == deckId }
            var deckSeenOrBetter = 0
            var deckFamiliarOrMature = 0
            var deckMature = 0
            for (wp in deckProgress) {
                when (classifyWordStage(wp)) {
                    WordStage.NEW -> Unit
                    WordStage.SEEN -> deckSeenOrBetter++
                    WordStage.LEARNING -> deckSeenOrBetter++
                    WordStage.FAMILIAR -> {
                        deckSeenOrBetter++
                        deckFamiliarOrMature++
                    }
                    WordStage.MATURE -> {
                        deckSeenOrBetter++
                        deckFamiliarOrMature++
                        deckMature++
                    }
                }
            }
            val pct = ((deckFamiliarOrMature.toFloat() / totalUnique.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
            DeckCoverageSummary(
                deckId = deckId,
                titleEn = DECK_TITLES[deckId] ?: deckId,
                totalWords = totalUnique,
                seenOrBetterCount = deckSeenOrBetter,
                familiarOrMatureCount = deckFamiliarOrMature,
                matureCount = deckMature,
                coveragePercent = pct
            )
        }

        val activeStudiedCount = seenCount + learningCount + familiarCount + matureCount
        val totalPoolCount = maxOf(deckCoverage.sumOf { it.totalWords }, activeStudiedCount)
        val newCount = (totalPoolCount - activeStudiedCount).coerceAtLeast(0)

        val funnel = WordLifecycleFunnel(
            totalPoolCount = totalPoolCount,
            newCount = newCount,
            seenCount = seenCount,
            learningCount = learningCount,
            familiarCount = familiarCount,
            matureCount = matureCount,
            conqueredCount = conqueredBadges.size,
            conqueredWords = sortedConquered
        )

        // 3. Personal Pace EMA from attempts.elapsed_ms
        val validElapsedList = allAttempts.mapNotNull { it.elapsedMs }.filter {
            it in AdaptiveConfig.MIN_VALID_ATTEMPT_MS..AdaptiveConfig.MAX_VALID_ATTEMPT_MS
        }
        val personalPaceSeconds = if (validElapsedList.size >= 5) {
            val avgSeconds = (validElapsedList.takeLast(30).average() / 1000.0).toFloat()
            ((AdaptiveConfig.PACE_EMA_HISTORICAL_WEIGHT * AdaptiveConfig.DEFAULT_SECONDS_PER_WORD) +
                (AdaptiveConfig.PACE_EMA_OBSERVED_WEIGHT * avgSeconds)).coerceIn(8f, 45f)
        } else {
            AdaptiveConfig.DEFAULT_SECONDS_PER_WORD
        }

        // 4. Rolling 7-day accuracy & Mature retention
        val sevenDaysAgo = nowMillis - 7L * 24L * 60L * 60L * 1000L
        val fourteenDaysAgo = nowMillis - 14L * 24L * 60L * 60L * 1000L
        val recent7dAttempts = allAttempts.filter { it.timestamp >= sevenDaysAgo }
        val prev7dAttempts = allAttempts.filter { it.timestamp in fourteenDaysAgo until sevenDaysAgo }

        val rollingAccuracy = if (recent7dAttempts.isNotEmpty()) {
            val correct = recent7dAttempts.count { it.isCorrect == true }
            ((correct.toFloat() / recent7dAttempts.size.toFloat()) * 100f).roundToInt()
        } else null

        val prevAccuracy = if (prev7dAttempts.isNotEmpty()) {
            val correct = prev7dAttempts.count { it.isCorrect == true }
            ((correct.toFloat() / prev7dAttempts.size.toFloat()) * 100f).roundToInt()
        } else null

        val reviewAttempts = recent7dAttempts.filter { it.activityType == "flashcard_review" }
        val matureRetention = if (reviewAttempts.size >= 5) {
            val recalled = reviewAttempts.count { it.isCorrect == true }
            ((recalled.toFloat() / reviewAttempts.size.toFloat()) * 100f).roundToInt()
        } else null

        // 5. Story Readability map
        val allStories = dao.getAllReadingTexts()
        val baselineLevels = getBaselineKnownLevels(resolvedProfile.level)
        val baselineEntries = if (baselineLevels.isNotEmpty()) {
            dao.getNewWordsForLevelsAndDecks(baselineLevels, listOf("oxford5000"), 1500)
        } else emptyList()
        val baselineKnownHeadwords = baselineEntries.mapTo(HashSet()) { it.text.lowercase() }

        val storyReadabilityMap = allStories.associate { story ->
            story.id to computeStoryReadability(
                story = story,
                userLevel = resolvedProfile.level,
                progressByHeadword = stageByHeadword,
                baselineKnownHeadwords = baselineKnownHeadwords
            )
        }

        // 6. Composite CEFR Readiness
        val userLevel = resolvedProfile.level.uppercase()
        val grammarLessonsAtLevel = dao.getGrammarLessonsByLevel(userLevel)
        val allGrammarProgress = dao.getAllGrammarProgressList().associateBy { it.lessonId }
        val completedGrammarAtLevel = grammarLessonsAtLevel.count { lesson ->
            val gp = allGrammarProgress[lesson.id]
            if (gp == null || !gp.isCompleted) return@count false
            val pct = if (gp.totalExercises > 0 && gp.bestScore <= gp.totalExercises) {
                (gp.bestScore * 100) / gp.totalExercises
            } else {
                gp.bestScore
            }
            if (gp.totalExercises == 0) gp.isCompleted else pct >= 70
        }
        val grammarMasteryPct = if (grammarLessonsAtLevel.isNotEmpty()) {
            ((completedGrammarAtLevel.toFloat() / grammarLessonsAtLevel.size.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
        } else 0

        val storiesAtLevel = allStories.filter { it.level.equals(userLevel, ignoreCase = true) }
        val allReadingProgress = dao.getAllReadingProgress().associateBy { it.textId }
        val completedStoriesAtLevel = storiesAtLevel.count { st ->
            val rp = allReadingProgress[st.id]
            if (rp == null || !rp.isCompleted) return@count false
            val pct = if (rp.totalQuestions > 0 && rp.bestScore <= rp.totalQuestions) {
                (rp.bestScore * 100) / rp.totalQuestions
            } else {
                rp.bestScore
            }
            if (rp.totalQuestions == 0) rp.isCompleted else pct >= 70
        }
        val readingMasteryPct = if (storiesAtLevel.isNotEmpty()) {
            ((completedStoriesAtLevel.toFloat() / storiesAtLevel.size.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
        } else 0

        // Vocab coverage at user level or above up to C1
        val levelTargetWords = AdaptiveConfig.getCefrVocabTarget(userLevel)
        val eligibleLevels = AdaptiveConfig.getCefrLevelsAtOrAbove(userLevel)
        val levelEntries = dao.getRecommendedWordsForLevels(eligibleLevels, limit = 20000)
        val levelHeadwords = levelEntries
            .map { it.text.trim().lowercase() }
            .toSet()
        val familiarOrMatureTotal = if (levelHeadwords.isNotEmpty()) {
            val allStudiedStageByHeadword = HashMap<String, WordStage>(allProgress.size * 2)
            for (wp in allProgress) {
                val stage = classifyWordStage(wp)
                val currentBest = allStudiedStageByHeadword[wp.text.lowercase()]
                if (currentBest == null || stage.ordinal > currentBest.ordinal) {
                    allStudiedStageByHeadword[wp.text.lowercase()] = stage
                }
            }
            allStudiedStageByHeadword.count { (word, stage) ->
                word in levelHeadwords && stage in setOf(WordStage.LEARNING, WordStage.FAMILIAR, WordStage.MATURE)
            }
        } else {
            learningCount + familiarCount + matureCount
        }
        val vocabCoveragePct = ((familiarOrMatureTotal.toFloat() / levelTargetWords.toFloat()) * 100f)
            .roundToInt()
            .coerceIn(0, 100)

        val overallReadiness = (
            (vocabCoveragePct * AdaptiveConfig.CEFR_WEIGHT_VOCAB) +
                (grammarMasteryPct * AdaptiveConfig.CEFR_WEIGHT_GRAMMAR) +
                (readingMasteryPct * AdaptiveConfig.CEFR_WEIGHT_READING)
            ).roundToInt().coerceIn(0, 100)

        val nextLevel = nextCefrLevel(userLevel)
        val cefrReadiness = CefrReadinessSummary(
            currentLevel = userLevel,
            nextLevel = nextLevel,
            overallReadinessPercent = overallReadiness,
            vocabCoveragePercent = vocabCoveragePct,
            grammarMasteryPercent = grammarMasteryPct,
            readingMasteryPercent = readingMasteryPct,
            isReadyForLevelUp = nextLevel != null && overallReadiness >= AdaptiveConfig.CEFR_LEVEL_UP_READY_PERCENT,
            vocabFamiliarOrMatureCount = familiarOrMatureTotal,
            vocabTargetCount = levelTargetWords,
            grammarPassedCount = completedGrammarAtLevel,
            grammarTotalCount = grammarLessonsAtLevel.size,
            readingCompletedCount = completedStoriesAtLevel,
            readingTargetCount = storiesAtLevel.size
        )

        // 7. Weekly Growth ("Before vs. After")
        val promotedThisWeek = allProgress.count { wp ->
            wp.lastReviewedTimestamp >= sevenDaysAgo &&
                (wp.intervalDays >= AdaptiveConfig.STAGE_FAMILIAR_MIN_INTERVAL_DAYS)
        }
        val promotedPrevWeek = allProgress.count { wp ->
            wp.lastReviewedTimestamp in fourteenDaysAgo until sevenDaysAgo &&
                (wp.intervalDays >= AdaptiveConfig.STAGE_FAMILIAR_MIN_INTERVAL_DAYS)
        }
        val conqueredThisWeek = allProgress.count { wp ->
            isWordConquered(wp) && wp.conqueredTimestamp >= sevenDaysAgo
        }
        val storiesThisWeek = allReadingProgress.values.count { rp ->
            rp.isCompleted && rp.updatedAt >= sevenDaysAgo
        }
        val storiesPrevWeek = allReadingProgress.values.count { rp ->
            rp.isCompleted && rp.updatedAt in fourteenDaysAgo until sevenDaysAgo
        }
        val grammarThisWeek = allGrammarProgress.values.count { gp ->
            gp.isCompleted && gp.updatedAt >= sevenDaysAgo
        }
        val notebookResolvedThisWeek = dao.getAllCorrectionEntriesList().count { cn ->
            cn.status == "mastered" && cn.updatedAt >= sevenDaysAgo
        }
        val accuracyDelta = if (rollingAccuracy != null && prevAccuracy != null) {
            rollingAccuracy - prevAccuracy
        } else null

        val weeklyGrowth = WeeklyGrowthReport(
            hasSufficientHistory = allAttempts.isNotEmpty() || promotedThisWeek > 0 || storiesThisWeek > 0 || grammarThisWeek > 0,
            wordsPromotedToFamiliarOrMature = promotedThisWeek,
            wordsLearnedPrevWeek = promotedPrevWeek,
            wordsConqueredThisWeek = conqueredThisWeek,
            storiesCompletedThisWeek = storiesThisWeek,
            storiesCompletedPrevWeek = storiesPrevWeek,
            grammarCompletedThisWeek = grammarThisWeek,
            notebookResolvedThisWeek = notebookResolvedThisWeek,
            currentWeekAccuracyPercent = rollingAccuracy,
            previousWeekAccuracyPercent = prevAccuracy,
            accuracyDeltaPercent = accuracyDelta
        )

        // 8. Data-Backed Insight Banner (with Cold-Start & 7-day dismissal cooldown)
        val quizScoresWithTime = mutableListOf<Pair<Long, Float>>()
        val allQuizResults = dao.getAllQuizResultsList()
        for (qr in allQuizResults) {
            quizScoresWithTime.add(qr.timestamp to qr.accuracyPercent)
        }
        for (att in allAttempts) {
            if (att.activityType in listOf("grammar_quiz", "reading_quiz") && att.score != null) {
                quizScoresWithTime.add(att.timestamp to att.score)
            }
        }
        val last3QuizScores = quizScoresWithTime
            .sortedByDescending { it.first }
            .take(3)
            .map { it.second }
        val hasMasteredQuizzesForLevelUp = last3QuizScores.size >= 3 && last3QuizScores.all { it >= 80f }

        val activeInsight = selectActiveInsight(
            profile = resolvedProfile,
            isColdStart = isColdStart,
            cefrReadiness = cefrReadiness,
            conqueredThisWeek = conqueredThisWeek,
            promotedThisWeek = promotedThisWeek,
            personalPaceSeconds = personalPaceSeconds,
            nowMillis = nowMillis,
            recent7dAttemptsCount = recent7dAttempts.size,
            rollingAccuracy = rollingAccuracy,
            hasMasteredQuizzesForLevelUp = hasMasteredQuizzesForLevelUp
        )

        LearnerModelSnapshot(
            funnel = funnel,
            deckCoverage = deckCoverage,
            cefrReadiness = cefrReadiness,
            weeklyGrowth = weeklyGrowth,
            personalPaceSecondsPerWord = personalPaceSeconds,
            rollingSevenDayAccuracyPercent = rollingAccuracy,
            matureRetentionPercent = matureRetention,
            totalAttemptsCount = allAttempts.size,
            isColdStart = isColdStart,
            activeInsight = activeInsight,
            storyReadabilityById = storyReadabilityMap,
            wordStageByHeadword = stageByHeadword
        )
    }

    suspend fun dismissInsight(insightId: String, nowMillis: Long = timeProvider.currentTimeMillis()): StudyPlanProfileEntity = withContext(Dispatchers.IO) {
        val profile = dao.getStudyPlanProfile() ?: StudyPlanProfileEntity()
        val map = parseDismissedMap(profile.dismissedInsightsJson).toMutableMap()
        map[insightId] = nowMillis
        val encoded = json.encodeToString(MapSerializer(String.serializer(), Long.serializer()), map)
        val updated = profile.copy(dismissedInsightsJson = encoded, updatedAt = nowMillis)
        dao.upsertStudyPlanProfile(updated)
        updated
    }

    private fun selectActiveInsight(
        profile: StudyPlanProfileEntity,
        isColdStart: Boolean,
        cefrReadiness: CefrReadinessSummary,
        conqueredThisWeek: Int,
        promotedThisWeek: Int,
        personalPaceSeconds: Float,
        nowMillis: Long,
        recent7dAttemptsCount: Int = 0,
        rollingAccuracy: Int? = null,
        hasMasteredQuizzesForLevelUp: Boolean = true
    ): AdaptiveInsightBanner? {
        val dismissedMap = parseDismissedMap(profile.dismissedInsightsJson)
        val cooldownMillis = AdaptiveConfig.INSIGHT_DISMISS_COOLDOWN_DAYS * 24L * 60L * 60L * 1000L
        fun isEligible(id: String): Boolean {
            val dismissedAt = dismissedMap[id] ?: return true
            return (nowMillis - dismissedAt) >= cooldownMillis
        }

        if (isColdStart) {
            if (!isEligible("cold_start_calibrating")) return null
            return AdaptiveInsightBanner(
                id = "cold_start_calibrating",
                titleEn = "Calibrating Your Personal Pace",
                titleAr = "جاري معايرة سرعتك الشخصية",
                reasonEn = "Complete a few more practice cards so Qaf can adapt daily targets to your exact recall speed.",
                badgeText = "Adaptive Engine",
                messageAr = "أكمل بضع بطاقات إضافية ليقوم قاف بضبط أهدافك اليومية حسب سرعة تذكرك الفعلية."
            )
        }

        if (
            recent7dAttemptsCount >= 15 &&
            rollingAccuracy != null &&
            rollingAccuracy < (AdaptiveConfig.STRUGGLE_DOWNSHIFT_ACCURACY * 100f) &&
            isEligible("struggle_downshift")
        ) {
            return AdaptiveInsightBanner(
                id = "struggle_downshift",
                titleEn = "Learning Pace Adjustment",
                titleAr = "تعديل وتيرة التعلم",
                reasonEn = "Recent 7-day accuracy is $rollingAccuracy% across $recent7dAttemptsCount practice sessions. Consider lowering daily target pace to solidify retention.",
                badgeText = "Pace Recommendation",
                messageAr = "دقة الاستذكار في الأيام السبعة الماضية $rollingAccuracy% خلال $recent7dAttemptsCount تدريباً. يُنصح بتخفيف الهدف اليومي لترسيخ الذاكرة.",
                actionRoute = "plan",
                actionLabelEn = "Adjust Study Target"
            )
        }

        if (
            cefrReadiness.isReadyForLevelUp &&
            cefrReadiness.nextLevel != null &&
            hasMasteredQuizzesForLevelUp &&
            isEligible("level_up_${cefrReadiness.nextLevel}")
        ) {
            return AdaptiveInsightBanner(
                id = "level_up_${cefrReadiness.nextLevel}",
                titleEn = "Ready for Level ${cefrReadiness.nextLevel}!",
                titleAr = "أنت مستعد للانتقال إلى المستوى ${cefrReadiness.nextLevel}!",
                reasonEn = "You reached ${cefrReadiness.overallReadinessPercent}% mastery in ${cefrReadiness.currentLevel} (${cefrReadiness.vocabCoveragePercent}% vocab, ${cefrReadiness.grammarMasteryPercent}% grammar, ${cefrReadiness.readingMasteryPercent}% reading) and passed your last 3 mastery quizzes.",
                badgeText = "${cefrReadiness.overallReadinessPercent}% Ready",
                messageAr = "حققت ${cefrReadiness.overallReadinessPercent}% إتقاناً في المستوى ${cefrReadiness.currentLevel} واجتزت آخر ٣ اختبارات بنجاح.",
                actionRoute = "level_up",
                actionLabelEn = "Advance to ${cefrReadiness.nextLevel}",
                suggestedLevel = cefrReadiness.nextLevel
            )
        }

        if (conqueredThisWeek > 0 && isEligible("conquered_milestone")) {
            return AdaptiveInsightBanner(
                id = "conquered_milestone",
                titleEn = "You Conquered $conqueredThisWeek Difficult ${if (conqueredThisWeek == 1) "Word" else "Words"}!",
                titleAr = "تغلّبت على $conqueredThisWeek كلمات صعبة هذا الأسبوع!",
                reasonEn = "Words that previously caused 2+ lapses now have a 21+ day memory interval.",
                badgeText = "Conquered 🏆",
                messageAr = "الكلمات التي أخطأت فيها سابقاً أصبحت الآن في ذاكرتك طويلة المدى (٢١+ يوماً)."
            )
        }

        if (promotedThisWeek >= 5 && isEligible("weekly_promotion")) {
            return AdaptiveInsightBanner(
                id = "weekly_promotion",
                titleEn = "$promotedThisWeek Words Reached Long-Term Memory",
                titleAr = "انتقلت $promotedThisWeek كلمة إلى الذاكرة طويلة المدى",
                reasonEn = "Your 7+ day interval vocabulary grew by $promotedThisWeek words over the last 7 days.",
                badgeText = "+$promotedThisWeek Familiar",
                messageAr = "نمت مفرداتك ذات الفاصل الزمني (٧+ أيام) بمقدار $promotedThisWeek كلمة خلال الأسبوع الماضي."
            )
        }

        if (personalPaceSeconds <= 15f && isEligible("fast_pace_bonus")) {
            return AdaptiveInsightBanner(
                id = "fast_pace_bonus",
                titleEn = "Fast Recall Pace (${personalPaceSeconds.roundToInt()}s / word)",
                titleAr = "سرعة استذكار عالية (${personalPaceSeconds.roundToInt()} ثانية لكل كلمة)",
                reasonEn = "Your rolling answer speed is faster than average, allowing deeper story vocabulary coverage.",
                badgeText = "${personalPaceSeconds.roundToInt()}s/word",
                messageAr = "متوسط سرعة إجابتك أسرع من المعتاد مما يسمح بتغطية أعمق لمفردات القصص."
            )
        }

        return null
    }

    private fun parseStringList(rawJson: String): List<String> {
        return try {
            json.decodeFromString(ListSerializer(String.serializer()), rawJson)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseDismissedMap(rawJson: String): Map<String, Long> {
        return try {
            json.decodeFromString(MapSerializer(String.serializer(), Long.serializer()), rawJson)
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun extractContentTokens(sentencesJson: String): List<String> {
        val regex = Regex("[A-Za-z][A-Za-z'-]*")
        val tokens = LinkedHashSet<String>()
        for (match in regex.findAll(sentencesJson)) {
            val word = match.value.lowercase().trim('\'', '-')
            if (word.length >= 3 && word !in STOPWORDS) {
                tokens.add(word)
            }
        }
        return tokens.toList()
    }

    private fun compareCefrRank(level: String): Int {
        return AdaptiveConfig.compareCefrRank(level)
    }

    private fun getBaselineKnownLevels(userLevel: String): List<String> {
        return when (userLevel.trim().uppercase()) {
            "A2" -> listOf("A1")
            "B1" -> listOf("A1", "A2")
            "B2" -> listOf("A1", "A2", "B1")
            "C1" -> listOf("A1", "A2", "B1", "B2")
            else -> emptyList()
        }
    }
}
