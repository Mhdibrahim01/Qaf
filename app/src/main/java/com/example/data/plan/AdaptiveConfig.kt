package com.example.data.plan

/**
 * Single source of truth for all Qaf (قاف) Adaptive V2 pedagogical thresholds,
 * scoring weights, macro-topics, and cold-start guards.
 */
object AdaptiveConfig {
    // 1. Word Lifecycle Stage Thresholds
    const val STAGE_FAMILIAR_MIN_INTERVAL_DAYS = 7
    const val STAGE_MATURE_MIN_INTERVAL_DAYS = 21
    const val CONQUERED_MIN_LAPSES = 2
    const val CONQUERED_MIN_INTERVAL_DAYS = 21

    // 2. Leech & Remediation Thresholds
    const val LEECH_LAPSE_THRESHOLD = 3
    const val LEECH_WINDOW_DAYS = 14
    const val GRAMMAR_REMEDIATION_SCORE_THRESHOLD = 65
    const val GRAMMAR_REMEDIATION_NOTEBOOK_THRESHOLD = 3
    const val GRAMMAR_REFRESHER_COOLDOWN_DAYS = 3
    const val MAX_CONSECUTIVE_GRAMMAR_REFRESHERS = 1

    // 3. Backlog & Pacing Thresholds
    const val OVERDUE_CAP_MAX = 30
    const val HEAVY_BACKLOG_THRESHOLD = 15
    const val BACKLOG_HEAVY_THRESHOLD = 15
    const val MODERATE_BACKLOG_THRESHOLD = 8
    const val COLD_START_MIN_ATTEMPTS = 20
    const val NEW_WORDS_MIN = 5
    const val NEW_WORDS_MAX = 20
    const val ACCURACY_FAST_TRACK_THRESHOLD = 0.88f

    // 4. Personal Pace EMA Constants
    const val DEFAULT_SECONDS_PER_WORD = 20f
    const val MIN_VALID_ATTEMPT_MS = 1_500L
    const val MAX_VALID_ATTEMPT_MS = 60_000L
    const val PACE_EMA_HISTORICAL_WEIGHT = 0.7f
    const val PACE_EMA_OBSERVED_WEIGHT = 0.3f

    // 5. Story Selection & Readability Thresholds
    const val OPTIMAL_READABILITY_MIN_PERCENT = 80
    const val OPTIMAL_READABILITY_MAX_PERCENT = 92
    const val STORY_SCORE_TOPIC_MATCH = 40
    const val STORY_SCORE_OPTIMAL_READABILITY = 35
    const val STORY_SCORE_GRAMMAR_SYNERGY = 25
    const val PRE_TEACH_STORY_WORDS_MIN = 4
    const val PRE_TEACH_STORY_WORDS_MAX = 6

    // 6. CEFR Readiness Composite Weights & Gate
    const val CEFR_WEIGHT_VOCAB = 0.50f
    const val CEFR_WEIGHT_GRAMMAR = 0.30f
    const val CEFR_WEIGHT_READING = 0.20f
    const val CEFR_LEVEL_UP_READY_PERCENT = 80
    const val STRUGGLE_DOWNSHIFT_ACCURACY = 0.55f
    const val INSIGHT_DISMISS_COOLDOWN_DAYS = 7

    fun getCefrVocabTarget(level: String): Int = when (level.uppercase()) {
        "A1" -> 300
        "A2" -> 400
        "B1" -> 500
        "B2" -> 600
        "C1" -> 600
        else -> 400
    }

    data class MacroTopic(
        val id: String,
        val titleEn: String,
        val titleAr: String,
        val rawTopics: Set<String>
    )

    val MACRO_TOPICS: List<MacroTopic> = listOf(
        MacroTopic(
            id = "daily_life",
            titleEn = "Daily Life & Culture",
            titleAr = "الحياة اليومية والثقافة",
            rawTopics = setOf("daily_life", "daily_life_culture", "culture", "food", "home", "shopping", "society", "history", "routine", "community", "family", "traditions", "living", "cooking", "lifestyle", "money", "friendship", "hobbies", "daily", "life")
        ),
        MacroTopic(
            id = "work_career",
            titleEn = "Work & Career",
            titleAr = "العمل والمهنة",
            rawTopics = setOf("work_career", "business_career", "career", "workplace", "economics", "leadership", "business", "work", "office", "interview", "crafts", "careers", "jobs", "employment")
        ),
        MacroTopic(
            id = "technology",
            titleEn = "Technology & AI",
            titleAr = "التكنولوجيا والذكاء الاصطناعي",
            rawTopics = setOf("technology", "technology_science", "digital_life", "innovation", "computing", "ai", "information", "software", "tech")
        ),
        MacroTopic(
            id = "travel",
            titleEn = "Travel & Places",
            titleAr = "السفر والأماكن",
            rawTopics = setOf("travel", "travel_global", "transport", "cities", "tourism", "geography", "city", "places", "urban")
        ),
        MacroTopic(
            id = "health",
            titleEn = "Health & Mind",
            titleAr = "الصحة والعقل",
            rawTopics = setOf("health", "health_psychology", "psychology", "fitness", "medicine", "wellbeing", "sport", "sports", "medical", "mind", "abilities")
        ),
        MacroTopic(
            id = "science_nature",
            titleEn = "Science & Environment",
            titleAr = "العلوم والبيئة",
            rawTopics = setOf("science_nature", "environment_nature", "science", "environment", "nature", "climate", "space", "biology", "safety", "discovery")
        ),
        MacroTopic(
            id = "education",
            titleEn = "Education & Learning",
            titleAr = "التعليم والتعلم",
            rawTopics = setOf("education", "education_language", "language", "study", "academics", "university", "communication", "school")
        ),
        MacroTopic(
            id = "media_arts",
            titleEn = "Media, Books & Arts",
            titleAr = "الإعلام والكتب والفنون",
            rawTopics = setOf("media_arts", "media", "arts", "literature", "entertainment", "architecture", "philosophy", "design", "art", "books")
        )
    )

    private val ASSET_TOPIC_MAP: Map<String, String> = mapOf(
        "business_career" to "work_career",
        "technology_science" to "technology",
        "environment_nature" to "science_nature",
        "daily_life_culture" to "daily_life",
        "travel_global" to "travel",
        "health_psychology" to "health",
        "education_language" to "education"
    )

    fun normalizeMacroTopic(rawTopic: String?): String {
        if (rawTopic.isNullOrBlank()) return "daily_life"
        val cleaned = rawTopic.trim().lowercase()

        // 1. Direct match with asset mappings
        ASSET_TOPIC_MAP[cleaned]?.let { return it }

        // 2. Direct exact match with macro topic id or rawTopics
        val exactMatch = MACRO_TOPICS.firstOrNull { cleaned == it.id || cleaned in it.rawTopics }
        if (exactMatch != null) return exactMatch.id

        // 3. Tokenized match for compound phrases (e.g., "career and interview", "work and office")
        val tokens = cleaned.split(Regex("[\\s_&/,-]+|\\band\\b"))
            .map { it.trim() }
            .filter { it.isNotEmpty() && it != "and" && it != "the" && it != "of" }

        val nonDefaultTopics = MACRO_TOPICS.filter { it.id != "daily_life" }
        for (token in tokens) {
            val matched = nonDefaultTopics.firstOrNull { token == it.id || token in it.rawTopics }
            if (matched != null) return matched.id
        }

        val dailyTopic = MACRO_TOPICS.firstOrNull { it.id == "daily_life" }
        if (dailyTopic != null) {
            for (token in tokens) {
                if (token == dailyTopic.id || token in dailyTopic.rawTopics) {
                    return dailyTopic.id
                }
            }
        }

        return "daily_life"
    }

    fun getMacroTopicLabel(topicId: String): String {
        val norm = normalizeMacroTopic(topicId)
        return MACRO_TOPICS.firstOrNull { it.id == norm }?.titleEn ?: "Daily Life & Culture"
    }

    fun getDefaultTopicsForGoal(learningGoal: String): List<String> {
        return when (learningGoal.lowercase()) {
            "it_career" -> listOf("technology", "work_career", "science_nature")
            "work_career" -> listOf("work_career", "technology", "daily_life")
            "ielts_academic" -> listOf("science_nature", "education", "health")
            else -> listOf("daily_life", "travel", "health")
        }
    }
}
