package com.example.data.plan

import com.example.BuildConfig
import com.example.data.local.StudyPlanEntity
import com.example.data.local.StudyPlanJson
import com.example.data.local.StudyPlanProfileEntity
import com.example.data.local.WordBooksDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class StudyPlanAiService(
    private val repository: StudyPlanRepository,
    private val dao: WordBooksDao,
    private val isAllowAiEnabled: () -> Boolean,
    private val isOnline: () -> Boolean,
    private val timeProvider: TimeProvider = SystemTimeProvider(),
    private val apiKeyProvider: () -> String = { BuildConfig.GEMINI_API_KEY },
    private val apiKeysProvider: (() -> List<String>)? = null
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    var customAiTransport: (suspend (prompt: String, expectsJson: Boolean) -> String)? = null

    /**
     * Generates a monthly study plan with Gemini.
     * ZERO network calls if Allow AI is off or device is offline.
     */
    suspend fun generateMonthlyPlanWithAi(
        targetMonth: String = timeProvider.currentMonthString()
    ): Result<StudyPlanEntity> = withContext(Dispatchers.IO) {
        if (!isAllowAiEnabled()) {
            return@withContext Result.failure(IllegalStateException("Allow AI is disabled in settings."))
        }
        if (!isOnline()) {
            return@withContext Result.failure(IllegalStateException("Device is offline. Please check your internet connection."))
        }

        val profile = repository.getOrCreateProfile()

        // Gather aggregate telemetry stats
        val totalLearned = dao.getLearnedWordsCountByLevel(profile.level)
        val grammarCount = dao.getAllGrammarLessons().count { it.isCompleted }
        val readingCount = dao.getAllReadingProgress().count { it.isCompleted }
        val weakWords = dao.getWeakestWords(20).map { "${it.text} (${it.pos})" }

        val prompt = buildPlanPrompt(profile, targetMonth, totalLearned, grammarCount, readingCount, weakWords)

        // Attempt 1
        var rawResponse = callGemini(prompt, expectsJson = true)
        var parsed = tryParseAndValidatePlan(rawResponse, profile, targetMonth)

        // Retry once on failure
        if (parsed == null) {
            val retryPrompt = "$prompt\n\nCRITICAL: Your previous response was invalid. Return ONLY a valid JSON object matching the exact schema."
            rawResponse = callGemini(retryPrompt, expectsJson = true)
            parsed = tryParseAndValidatePlan(rawResponse, profile, targetMonth)
        }

        if (parsed != null) {
            val entity = StudyPlanEntity(
                month = targetMonth,
                levelStart = profile.level,
                levelTarget = parsed.level_target.ifBlank { "${profile.level}+" },
                weeksJson = json.encodeToString(parsed.weeks),
                dailyTemplateJson = json.encodeToString(parsed.daily_template),
                tipsAr = parsed.tips_ar,
                source = "ai",
                createdAt = timeProvider.currentTimeMillis(),
                updatedAt = timeProvider.currentTimeMillis()
            )
            dao.upsertStudyPlan(entity)
            Result.success(entity)
        } else {
            Result.failure(IllegalArgumentException("AI generated invalid plan JSON after retry."))
        }
    }

    private fun tryParseAndValidatePlan(
        raw: String,
        profile: StudyPlanProfileEntity,
        targetMonth: String
    ): StudyPlanJson? {
        val clean = stripCodeFences(raw)
        return try {
            val plan = json.decodeFromString<StudyPlanJson>(clean)

            // Validate strict constraints
            val weeks = plan.weeks
            if (weeks.size !in 4..5) return null

            // Validate daily template ranges
            val template = plan.daily_template
            if (template.new_words !in 5..20) return null
            val totalDailyMins = template.review_cards_min + template.new_words + template.grammar_min + template.reading_min + template.quiz_min
            if (totalDailyMins > profile.minutesPerDay + 10) return null // Must be within reasonable range of daily budget

            plan.copy(month = targetMonth)
        } catch (e: Exception) {
            null
        }
    }

    private fun buildPlanPrompt(
        profile: StudyPlanProfileEntity,
        targetMonth: String,
        wordsLearned: Int,
        grammarDone: Int,
        readingDone: Int,
        weakWords: List<String>
    ): String {
        return """
        You are an expert CEFR English curriculum designer. Create a monthly study plan for month $targetMonth.
        Learner Profile:
        - Current Level: ${profile.level} (Target: ${profile.level}+)
        - Goal: ${profile.goal}
        - Time: ${profile.minutesPerDay} minutes/day
        - Active Study Days: ${profile.studyDaysPerWeek} days/week
        
        Learner Telemetry:
        - Words mastered at level: $wordsLearned
        - Grammar lessons completed: $grammarDone
        - Reading texts completed: $readingDone
        - Weak words needing reinforcement (max 20): ${weakWords.joinToString(", ")}
        
        Strict JSON format:
        {
          "month": "$targetMonth",
          "level_start": "${profile.level}",
          "level_target": "${profile.level}+",
          "weeks": [
            {
              "week": 1,
              "focus": "English focus theme",
              "focus_ar": "عنوان التركيز الأسبوعي بالعربية",
              "goals": {
                "new_words": 50,
                "grammar_lessons": 3,
                "reading_texts": 3,
                "sentences": 20,
                "quizzes": 2
              }
            }
          ],
          "daily_template": {
            "review_cards_min": 8,
            "new_words": 10,
            "grammar_min": 7,
            "reading_min": 5,
            "quiz_min": 3
          },
          "tips_ar": "نصائح وإرشادات باللغة العربية لتحقيق أفضل نتيجة هذا الشهر"
        }
        
        Rules:
        - Exactly 4 or 5 weeks.
        - daily_template.new_words must be between 5 and 20.
        - Sum of daily_template minutes should closely match ${profile.minutesPerDay} minutes.
        - Respond ONLY with the JSON object, without markdown or extra commentary.
        """.trimIndent()
    }

    private suspend fun callGemini(prompt: String, expectsJson: Boolean): String {
        val custom = customAiTransport
        if (custom != null) {
            return custom(prompt, expectsJson)
        }

        val keys = (apiKeysProvider?.invoke()?.filter { it.isNotBlank() } ?: listOf(apiKeyProvider()))
            .map { it.trim() }
            .filter { it.isNotBlank() && it != "YOUR_GEMINI_API_KEY" && it != "MY_GEMINI_API_KEY" && it != "null" }

        return com.example.data.ai.GeminiApiKeyManager.executeAiPrompt(
            prompt = prompt,
            keys = keys,
            expectsJson = expectsJson,
            timeoutMs = 40_000
        )
    }

    private fun stripCodeFences(text: String): String {
        return text.trim()
            .removePrefix("```json")
            .removePrefix("```JSON")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }
}
