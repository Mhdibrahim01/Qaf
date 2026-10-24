package com.example.data.reading

import com.example.BuildConfig
import com.example.ui.components.stripMarkdownAsterisks
import com.example.data.ai.GeminiHttpException
import com.example.data.local.ReadingQuestionItem
import com.example.data.local.ReadingSentenceItem
import com.example.data.local.ReadingTextEntity
import com.example.data.local.ReadingTextJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
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

@Serializable
data class CorrectionAiResult(
    @SerialName("corrected_sentence") val correctedSentence: String = "",
    @SerialName("explanation_ar") val explanationAr: String = "",
    @SerialName("better_alternative") val betterAlternative: String = ""
)

class ReadingAiService(
    private val repository: ReadingRepository,
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
     * 1. "Create a new text with AI":
     * Creates a complete text with target words in gold, Arabic sentence translations, and 5 questions.
     */
    suspend fun createNewTextWithAi(
        level: String,
        topic: String,
        length: String = "medium", // "short" (5-6) | "medium" (8-10) | "long" (12-15)
        targetWords: List<String> = emptyList(),
        targetGrammarLessonId: String? = null,
        targetGrammarTitle: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            checkPreconditions()

            val sentenceCountDesc = when (length.lowercase()) {
                "short" -> "5 to 6 sentences"
                "long" -> "12 to 15 sentences"
                else -> "8 to 10 sentences"
            }

            val targetWordsClause = if (targetWords.isNotEmpty()) {
                """
- TARGET VOCABULARY WORDS: You MUST naturally incorporate these target words in the story: ${targetWords.joinToString(", ")}.
""".trimIndent()
            } else ""

            val targetGrammarClause = if (!targetGrammarLessonId.isNullOrBlank()) {
                val titlePart = if (!targetGrammarTitle.isNullOrBlank()) " ($targetGrammarTitle)" else ""
                """
- TARGET GRAMMAR RULE: You MUST naturally and prominently practice the grammar rule "$targetGrammarLessonId$titlePart" throughout the story.
  For every sentence that demonstrates or applies this grammar rule, include `"grammar_id": "$targetGrammarLessonId"` inside that sentence's JSON object.
""".trimIndent()
            } else ""

            val prompt = """
You are an expert English language educator creating reading material for Arabic speakers.
Generate an engaging, educational reading passage matching the following:
- CEFR Level: ${level.uppercase()}
- Topic: $topic
- Length: Exactly $sentenceCountDesc
$targetWordsClause
$targetGrammarClause

REQUIREMENTS:
1. Every English sentence must have its accurate Arabic translation.
2. For sentences that clearly demonstrate the target grammar rule, set "grammar_id": ${if (targetGrammarLessonId != null) "\"$targetGrammarLessonId\"" else "null"} (or null if the sentence does not use that rule).
3. Provide exactly 5 comprehension multiple-choice questions testing main idea, specific details, vocabulary in context, and inference.
4. Each question must have 4 options, a 0-based answer_index (0 to 3), and explanation_ar in clear Arabic.
5. Return ONLY a valid JSON object matching this exact schema:
{
  "id": "r_ai_${System.currentTimeMillis().toString().takeLast(6)}",
  "level": "${level.uppercase()}",
  "topic": "$topic",
  "title_en": "Title in English",
  "title_ar": "العنوان بالعربية",
  "sentences": [
    { "en": "English sentence 1.", "ar": "الترجمة العربية للجملة الأولى.", "grammar_id": ${if (targetGrammarLessonId != null) "\"$targetGrammarLessonId\"" else "null"} }
  ],
  "questions": [
    {
      "prompt": "Question text?",
      "options": ["Option A", "Option B", "Option C", "Option D"],
      "answer_index": 0,
      "explanation_ar": "توضيح الإجابة الصحيحة بالعربية"
    }
  ],
  "target_words": [${targetWords.joinToString(", ") { "\"$it\"" }}],
  "grammar_ids": [${if (targetGrammarLessonId != null) "\"$targetGrammarLessonId\"" else ""}]
}
""".trimIndent()

            var attempts = 0
            var lastError: Exception? = null

            while (attempts < 2) {
                attempts++
                try {
                    val raw = callGeminiRaw(prompt, expectsJson = true)
                    val cleaned = stripCodeFences(raw)
                    val parsed = repository.parseReadingTextsFlexible(cleaned, defaultSource = "ai").firstOrNull()
                        ?: throw IllegalStateException("Could not parse AI reading text JSON")

                    if (parsed.sentences.isEmpty() || parsed.questions.isEmpty()) {
                        throw IllegalStateException("Generated text must contain sentences and questions")
                    }

                    // Auto-Matcher: Ensure sentences that demonstrate the target grammar rule have grammar_id,
                    // and guarantee targetGrammarLessonId is registered in grammar_ids
                    val resolvedSentences = if (!targetGrammarLessonId.isNullOrBlank()) {
                        autoTagGrammarSentences(parsed.sentences, targetGrammarLessonId)
                    } else {
                        parsed.sentences
                    }

                    val resolvedGrammarIds = if (!targetGrammarLessonId.isNullOrBlank()) {
                        (parsed.grammarIds + targetGrammarLessonId + resolvedSentences.mapNotNull { it.grammarId }).distinct()
                    } else {
                        (parsed.grammarIds + resolvedSentences.mapNotNull { it.grammarId }).distinct()
                    }

                    val finalParsed = parsed.copy(
                        sentences = resolvedSentences,
                        grammarIds = resolvedGrammarIds
                    )

                    val saveResult = repository.saveAiCreatedText(
                        text = finalParsed,
                        targetWords = targetWords
                    )
                    return@withContext saveResult
                } catch (e: Exception) {
                    lastError = e
                }
            }
            Result.failure(lastError ?: IllegalStateException("Failed to generate reading text"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 2. "Ask AI" on a word not in books, or "Meaning in this sentence".
     * Explains the word in simple Arabic in the context of the sentence, permanently saved per text, sentence, word.
     */
    suspend fun explainWordInContext(
        textId: String,
        sentenceIndex: Int,
        sentenceEn: String,
        word: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Check cache first
            val cached = repository.getCachedWordExplanation(textId, sentenceIndex, word)
            if (!cached.isNullOrBlank()) {
                return@withContext Result.success(cached)
            }

            checkPreconditions()

            val prompt = com.example.data.ai.AiPromptContract.buildWordInContextPrompt(
                word = word,
                sentenceEn = sentenceEn
            )

            val rawExplanation = callGeminiRaw(prompt, expectsJson = false)
            val explanation = com.example.data.ai.AiPromptContract.sanitizeAiPlainText(rawExplanation)
            repository.saveCachedWordExplanation(textId, sentenceIndex, word, explanation)
            Result.success(explanation)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 3. For pasted text: "Translate and make questions"
     * Generates Arabic translation per sentence and 5 comprehension questions once, saved with the text.
     */
    suspend fun translateAndMakeQuestionsForPastedText(
        text: ReadingTextEntity
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            checkPreconditions()

            val sentences = json.decodeFromString<List<ReadingSentenceItem>>(text.sentencesJson)
            if (sentences.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("No sentences found in text"))
            }

            val englishSentencesJson = json.encodeToString(sentences.map { it.en })

            val prompt = """
You are an English language educator helping an Arabic learner.
Here is a list of English sentences from a reading passage:
$englishSentencesJson

Title: ${text.titleEn}

TASK:
1. Provide an accurate Arabic translation for each sentence in order.
2. Create exactly 5 comprehension multiple-choice questions based on this text.
3. Suggest a natural Arabic title.

Output ONLY valid JSON matching:
{
  "title_ar": "العنوان بالعربية",
  "sentences": [
    { "en": "sentence text", "ar": "الترجمة العربية" }
  ],
  "questions": [
    {
      "prompt": "Question text?",
      "options": ["A", "B", "C", "D"],
      "answer_index": 0,
      "explanation_ar": "شرح الإجابة بالعربية"
    }
  ]
}
""".trimIndent()

            val raw = callGeminiRaw(prompt, expectsJson = true)
            val cleaned = stripCodeFences(raw)
            val parsedList = repository.parseReadingTextsFlexible(cleaned, defaultSource = "user")
            val first = parsedList.firstOrNull() ?: throw IllegalStateException("Could not parse translation result")

            repository.updatePastedTextWithAiResults(
                textId = text.id,
                sentences = first.sentences.ifEmpty { sentences },
                questions = first.questions,
                titleAr = first.titleAr
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 4. "Simplify this text": a version at a lower CEFR level, saved as a separate text linked to original.
     */
    suspend fun simplifyText(
        original: ReadingTextEntity
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            checkPreconditions()

            val targetLevel = when (original.level.uppercase()) {
                "C2", "C1" -> "B2"
                "B2" -> "B1"
                "B1" -> "A2"
                else -> "A1"
            }

            val sentences = json.decodeFromString<List<ReadingSentenceItem>>(original.sentencesJson)
            val fullText = sentences.joinToString(" ") { it.en }

            val prompt = """
You are an expert English language educator.
Simplify this reading text to a lower CEFR level: $targetLevel.
Original Level: ${original.level}
Original Title: ${original.titleEn}
Original Passage:
$fullText

REQUIREMENTS:
1. Rewrite the story using simpler vocabulary, clearer sentence structures, and high-frequency words appropriate for level $targetLevel.
2. Provide accurate Arabic translation for each simplified sentence.
3. Create 5 multiple-choice questions for the simplified text.
4. Return ONLY valid JSON:
{
  "id": "r_simplified_${System.currentTimeMillis().toString().takeLast(6)}",
  "level": "$targetLevel",
  "topic": "${original.topic}",
  "title_en": "${original.titleEn} (Simplified)",
  "title_ar": "${original.titleAr} (نسخة مبسطة)",
  "sentences": [
    { "en": "Simpler sentence.", "ar": "ترجمة الجملة المبسطة." }
  ],
  "questions": [
    {
      "prompt": "Question?",
      "options": ["A", "B", "C", "D"],
      "answer_index": 0,
      "explanation_ar": "شرح"
    }
  ],
  "original_text_id": "${original.id}"
}
""".trimIndent()

            val raw = callGeminiRaw(prompt, expectsJson = true)
            val cleaned = stripCodeFences(raw)
            val parsed = repository.parseReadingTextsFlexible(cleaned, defaultSource = "ai").firstOrNull()
                ?: throw IllegalStateException("Could not parse simplified text JSON")

            val saveRes = repository.saveAiCreatedText(
                text = parsed,
                originalTextId = original.id
            )
            saveRes
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 5. "Ask AI about this text": chat bottom sheet that knows the text content.
     */
    suspend fun askAboutText(
        text: ReadingTextEntity,
        userQuestion: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            checkPreconditions()
            repository.addChatMessage(text.id, "user", userQuestion)

            val sentences = json.decodeFromString<List<ReadingSentenceItem>>(text.sentencesJson)
            val textContent = sentences.joinToString("\n") { "- EN: ${it.en}\n  AR: ${it.ar}" }

            val prompt = """
You are an enthusiastic, friendly English reading tutor for Arabic speakers.
The student is reading this passage:
Title: ${text.titleEn} (${text.titleAr})
CEFR Level: ${text.level}
Content:
$textContent

Student Question: "$userQuestion"

Answer the student in clear, natural Arabic with helpful English examples from the text.
Be concise, polite, and encouraging.

STRICT FORMATTING: Do NOT use markdown asterisks (**) or markdown headers (###). Output clean plain text using bullet points (•) and natural punctuation.
""".trimIndent()

            val answer = callGeminiRaw(prompt, expectsJson = false).stripMarkdownAsterisks().trim()
            repository.addChatMessage(text.id, "assistant", answer)
            Result.success(answer)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 6. One open question at the end (e.g. "Summarize the text in 2 sentences"):
     * Student types an answer and Gemini gives feedback in Arabic; saved in history.
     */
    suspend fun feedbackOnOpenQuestion(
        text: ReadingTextEntity,
        questionPrompt: String,
        userAnswer: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            checkPreconditions()

            val sentences = json.decodeFromString<List<ReadingSentenceItem>>(text.sentencesJson)
            val passage = sentences.joinToString(" ") { it.en }

            val prompt = """
You are an encouraging English teacher reviewing an Arabic student's reading response.
Reading Title: ${text.titleEn}
Reading Passage: $passage

Question: "$questionPrompt"
Student's Answer: "$userAnswer"

TASK:
1. Praise the student's effort and answer quality in warm, simple Arabic.
2. Mention whether the summary or response captured the main idea correctly.
3. Gently correct any English grammar, spelling, or vocabulary mistakes with clear explanations in Arabic.
4. Suggest a polished English version of their answer.

Format nicely with bullet points.

STRICT FORMATTING: Do NOT use markdown asterisks (**) or markdown headers (###). Output clean plain text using bullet points (•) and natural punctuation.
""".trimIndent()

            val feedback = callGeminiRaw(prompt, expectsJson = false).stripMarkdownAsterisks().trim()
            repository.addOpenQuestionHistory(text.id, "user", "$questionPrompt\nAnswer: $userAnswer")
            repository.addOpenQuestionHistory(text.id, "assistant", feedback)
            Result.success(feedback)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 7. "Check my sentence" (Correction Notebook):
     * Checks user's practice sentence against grammar structure once:
     * returns corrected sentence, short explanation in Arabic, and a better alternative.
     */
    suspend fun checkUserSentence(
        grammarLessonTitle: String,
        userSentence: String
    ): Result<CorrectionAiResult> = withContext(Dispatchers.IO) {
        try {
            // Check cache first
            val cached = repository.getCachedCorrectionResult(grammarLessonTitle, userSentence)
            if (cached != null) {
                return@withContext Result.success(cached)
            }

            checkPreconditions()

            val prompt = """
You are an expert English grammar teacher reviewing a student's practice sentence.
Grammar Structure: $grammarLessonTitle
Student's Sentence: "$userSentence"

TASK:
1. Correct the sentence if it contains grammatical, spelling, punctuation, or structural errors. If it is already correct, keep it as is.
2. Provide a clear, concise explanation in simple Arabic explaining the grammar rule applied and why.
3. Suggest a natural, polished English alternative sentence illustrating the same grammar rule.

Output strictly valid JSON with no markdown formatting:
{
  "corrected_sentence": "Corrected English sentence",
  "explanation_ar": "شرح مبسط بالعربية عن تطبيق القاعدة",
  "better_alternative": "Natural advanced English alternative"
}
""".trimIndent()

            val raw = callGeminiRaw(prompt, expectsJson = true)
            val cleaned = stripCodeFences(raw)
            val result = json.decodeFromString<CorrectionAiResult>(cleaned)
            repository.saveCachedCorrectionResult(grammarLessonTitle, userSentence, result)
            Result.success(result)
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
