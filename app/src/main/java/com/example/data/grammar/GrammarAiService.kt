package com.example.data.grammar

import com.example.BuildConfig
import com.example.ui.components.stripMarkdownAsterisks
import com.example.data.ai.GeminiHttpException
import com.example.data.local.GrammarExerciseItem
import com.example.data.local.GrammarLessonEntity
import com.example.data.local.GrammarLessonJson
import com.example.data.local.GrammarSentenceCheckResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class GrammarAiService(
    private val repository: GrammarRepository,
    private val isAllowAiEnabled: () -> Boolean,
    private val isOnline: () -> Boolean,
    private val apiKeyProvider: () -> String = { "" },
    private val apiKeysProvider: (() -> List<String>)? = null
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Volatile
    var customAiTransport: (suspend (prompt: String, expectsJson: Boolean) -> String)? = null

    fun isApiKeyConfigured(): Boolean {
        val keys = (apiKeysProvider?.invoke()?.filter { it.isNotBlank() } ?: listOf(apiKeyProvider())).map { it.trim() }
        return keys.any { it.isNotBlank() && it != "MY_GEMINI_API_KEY" && it != "YOUR_GEMINI_API_KEY" && it != "null" }
    }

    private fun checkPreconditions() {
        if (!isAllowAiEnabled()) {
            throw IllegalStateException("AI features are turned off ('Allow AI' switch is disabled).")
        }
        if (!isOnline()) {
            throw IllegalStateException("Device is offline. An internet connection is required for AI features.")
        }
        if (!isApiKeyConfigured() && customAiTransport == null) {
            throw IllegalStateException("GEMINI_API_KEY is not configured. Please configure an API key in settings.")
        }
    }

    /**
     * 1. "Ask AI about this lesson": chat bottom sheet that knows lesson content.
     * Answers in simple Arabic with English examples, and saves conversation in ai_history.
     */
    suspend fun askAboutLesson(
        lesson: GrammarLessonEntity,
        userQuestion: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            checkPreconditions()
            repository.addChatMessage(lesson.id, "user", userQuestion)

            val prompt = """
You are a friendly, encouraging English grammar teacher for Arabic speakers.
The student is studying this lesson:
- Level: ${lesson.level}
- Topic: ${lesson.topic}
- Title: ${lesson.titleEn} (${lesson.titleAr})
- Explanation: ${lesson.explanationAr}
- Examples: ${lesson.examplesJson}

Student Question: "$userQuestion"

Answer the student clearly in simple, natural Arabic with helpful English examples.
Use bullet points and short sentences so it is easy to read.

STRICT FORMATTING: Do NOT use markdown bold asterisks (**) or formatting syntax. Output clean, readable plain text with bullet points (•).
""".trimIndent()

            val rawAnswer = callGeminiRaw(prompt, expectsJson = false)
            val answer = rawAnswer.stripMarkdownAsterisks().trim()
            repository.addChatMessage(lesson.id, "assistant", answer)
            Result.success(answer)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 2. "Explain my mistake" button after a wrong exercise:
     * Explains in simple Arabic why the user's answer is wrong and the correct answer is right.
     * Saved permanently in ai_cache so the same mistake is NEVER requested twice!
     */
    suspend fun explainMistake(
        lessonId: String,
        lessonTitleEn: String,
        exercisePrompt: String,
        wrongAnswer: String,
        correctAnswer: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Check cache first!
            val cached = repository.getCachedMistakeExplanation(lessonId, exercisePrompt, wrongAnswer)
            if (!cached.isNullOrBlank()) {
                return@withContext Result.success(cached)
            }

            checkPreconditions()

            val prompt = """
You are an expert English grammar teacher for Arabic speakers.
Lesson: $lessonTitleEn
Exercise Prompt: "$exercisePrompt"
Student's Wrong Answer: "$wrongAnswer"
Correct Answer: "$correctAnswer"

Explain clearly and concisely in simple Arabic (2 to 4 bullet points):
1. Why "$wrongAnswer" is incorrect in this context.
2. Why "$correctAnswer" is the correct grammatical choice.
3. A tip or mnemonic in Arabic to help avoid this mistake again.
Keep the explanation gentle, clear, and direct.

STRICT FORMATTING: Do NOT use markdown bold asterisks (**) or formatting syntax. Output clean, readable plain text with bullet points (•).
""".trimIndent()

            val rawExplanation = callGeminiRaw(prompt, expectsJson = false)
            val explanation = rawExplanation.stripMarkdownAsterisks().trim()
            repository.saveMistakeExplanationToCache(lessonId, exercisePrompt, wrongAnswer, explanation)
            Result.success(explanation)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 3. "Write your own sentence": student writes a sentence using the grammar point.
     * Gemini corrects it, explains in Arabic, suggests a better version, and saves in ai_history.
     */
    suspend fun checkWrittenSentence(
        lesson: GrammarLessonEntity,
        userSentence: String
    ): Result<GrammarSentenceCheckResult> = withContext(Dispatchers.IO) {
        try {
            checkPreconditions()
            repository.addSentenceCheckHistory(lesson.id, "user", userSentence)

            val prompt = """
You are an English teacher evaluating a sentence written by an Arabic speaker studying ${lesson.titleEn} (${lesson.titleAr}, level ${lesson.level}).
Student sentence: "$userSentence"

Evaluate if the sentence is grammatically correct and uses the grammar point properly.
Return ONLY a valid JSON object with EXACTLY these fields:
- "is_correct": boolean (true if grammatically sound, false otherwise)
- "corrected_sentence": string (the corrected English sentence if needed, or the original if already correct)
- "explanation_ar": string (clear explanation in simple Arabic of what was right/wrong)
- "better_version": string (a more natural, native English phrasing or alternative)
""".trimIndent()

            val rawJson = callGeminiRaw(prompt, expectsJson = true)
            val cleaned = stripCodeFences(rawJson)
            val parsed = json.decodeFromString<GrammarSentenceCheckResult>(cleaned)

            val summaryText = if (parsed.isCorrect) {
                "✅ Correct! ${parsed.explanationAr}\nBetter: ${parsed.betterVersion}"
            } else {
                "⚠️ Correction: ${parsed.correctedSentence}\n${parsed.explanationAr}\nBetter: ${parsed.betterVersion}"
            }
            repository.addSentenceCheckHistory(lesson.id, "assistant", summaryText)

            Result.success(parsed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 4. "More practice": creates 5 new exercises for this lesson in the same format,
     * validated and saved permanently into the lesson (marked "source": "ai").
     */
    suspend fun generateMorePracticeExercises(
        lesson: GrammarLessonEntity
    ): Result<List<GrammarExerciseItem>> = withContext(Dispatchers.IO) {
        try {
            checkPreconditions()

            val prompt = """
You are an English grammar curriculum creator.
Generate 5 new varied exercises for this lesson:
- Level: ${lesson.level}
- Topic: ${lesson.topic}
- Title: ${lesson.titleEn} (${lesson.titleAr})
- Explanation: ${lesson.explanationAr}

Return ONLY a JSON array with EXACTLY 5 objects. Each object must have:
- "type": "mcq" | "fill" | "reorder" (use a variety)
- "prompt": string (the question or instruction)
- "options": array of strings (for mcq: 3-4 choices; for fill: 2-3 hint words or choices; for reorder: the scrambled words)
- "answer": string (the exact correct answer)
- "explanation_ar": string (brief explanation in Arabic of why this is correct)
- "source": "ai"
""".trimIndent()

            val rawJson = callGeminiRaw(prompt, expectsJson = true)
            val cleaned = stripCodeFences(rawJson)
            val exercises = json.decodeFromString<List<GrammarExerciseItem>>(cleaned)
                .map { it.copy(source = "ai") }
                .filter { it.prompt.isNotBlank() && it.answer.isNotBlank() && it.explanationAr.isNotBlank() }

            if (exercises.isEmpty()) {
                throw IllegalStateException("Generated exercises failed validation")
            }

            repository.appendExercisesToLesson(lesson.id, exercises)
            Result.success(exercises)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 5. "Create a new lesson with AI": chooses level and topic.
     * Validates format (retries once if invalid, never saves invalid data), saves permanently with "source": "ai".
     */
    suspend fun createNewLessonWithAi(
        level: String,
        topic: String
    ): Result<GrammarLessonJson> = withContext(Dispatchers.IO) {
        try {
            checkPreconditions()

            var attempts = 0
            var lastError: Exception? = null

            while (attempts < 2) {
                attempts++
                try {
                    val prompt = """
Create a comprehensive English grammar lesson for Arabic speakers.
- Level: $level (CEFR)
- Topic: $topic

Return ONLY a single valid JSON object in EXACTLY this format:
{
  "id": "g_ai_${System.currentTimeMillis().toString().takeLast(6)}",
  "level": "$level",
  "topic": "$topic",
  "title_en": "Clear English Title",
  "title_ar": "العنوان باللغة العربية",
  "explanation_ar": "Detailed yet simple explanation in Arabic covering rules, formation, and usage",
  "examples": [
    { "en": "Example sentence 1.", "ar": "الترجمة العربية ١" },
    { "en": "Example sentence 2.", "ar": "الترجمة العربية ٢" }
  ],
  "exercises": [
    {
      "type": "mcq",
      "prompt": "Question text...",
      "options": ["opt1", "opt2", "opt3", "opt4"],
      "answer": "opt1",
      "explanation_ar": "شرح الإجابة بالعربية"
    },
    {
      "type": "fill",
      "prompt": "Sentence with ___ blank.",
      "options": ["word1", "word2"],
      "answer": "word1",
      "explanation_ar": "شرح الإجابة بالعربية"
    },
    {
      "type": "reorder",
      "prompt": "Reorder the words:",
      "options": ["word1", "word2", "word3"],
      "answer": "word1 word2 word3",
      "explanation_ar": "شرح الإجابة بالعربية"
    }
  ],
  "source": "ai"
}
""".trimIndent()

                    val rawJson = callGeminiRaw(prompt, expectsJson = true)
                    val cleaned = stripCodeFences(rawJson)
                    val parsed = json.decodeFromString<GrammarLessonJson>(cleaned)

                    // Strict validation: never save invalid data
                    if (parsed.titleEn.isBlank() ||
                        parsed.titleAr.isBlank() ||
                        parsed.explanationAr.isBlank() ||
                        parsed.examples.isEmpty() ||
                        parsed.exercises.isEmpty()
                    ) {
                        throw IllegalStateException("Lesson JSON missing required non-blank fields")
                    }

                    // Save permanently
                    repository.saveAiCreatedLesson(parsed.copy(source = "ai", level = level, topic = topic))
                    return@withContext Result.success(parsed)
                } catch (e: Exception) {
                    lastError = e
                }
            }

            Result.failure(lastError ?: IllegalStateException("Failed to generate valid lesson after 2 attempts"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun callGeminiRaw(prompt: String, expectsJson: Boolean): String {
        customAiTransport?.let { return it(prompt, expectsJson) }

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

    private fun stripCodeFences(raw: String): String {
        var cleaned = raw.trim()
        cleaned = cleaned.replace(Regex("^```(?:json)?\\s*", setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE)), "")
        cleaned = cleaned.replace(Regex("\\s*```$", RegexOption.MULTILINE), "")
        return cleaned.trim()
    }
}
