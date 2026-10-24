package com.example.data.ai

import com.example.BuildConfig
import com.example.data.importer.DataImportRepository
import com.example.data.local.AiCacheEntity
import com.example.data.local.EntryEntity
import com.example.data.local.WordBooksDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class AiTaskStatus(
    val isRunning: Boolean = false,
    val title: String = "",
    val detail: String = "",
    val processedCount: Int = 0,
    val totalCount: Int = 0,
    val error: String? = null
)

@Serializable
data class TranslatedBatchItem(
    val id: JsonPrimitive,
    val arabic: String = "",
    val ipa: String = "",
    @SerialName("definition_en") val definitionEn: String = "",
    @SerialName("examples_en") val examplesEn: String = "",
    @SerialName("examples_ar") val examplesAr: String = ""
)

@Serializable
data class GeneratedDeckItem(
    val text: String = "",
    val type: String = "word",
    val pos: String = "noun",
    val level: String = "B2",
    val ipa: String = "",
    @SerialName("definition_en") val definitionEn: String = "",
    @SerialName("examples_en") val examplesEn: String = "",
    val arabic: String = "",
    @SerialName("examples_ar") val examplesAr: String = ""
)

/**
 * Implements the exact Gemini AI translation and deck-building workflows from `generate_decks.py`
 * directly inside the Android app using `BuildConfig.GEMINI_API_KEY`.
 *
 * Fixed ID ranges (matching generate_decks.py):
 * - ielts_core: starts at 100001
 * - it_industry: starts at 200001
 *
 * Provenance rules:
 * - AI translations set `arabic_source = "ai"`
 * - Entries with `arabic_source = "user"` are NEVER overwritten by AI or file imports.
 */
class GeminiDeckAndTranslationService(
    private val dao: WordBooksDao,
    private val apiKeyProvider: () -> String = { "" },
    private val apiKeysProvider: (() -> List<String>)? = null
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val _status = MutableStateFlow(AiTaskStatus())
    val status: StateFlow<AiTaskStatus> = _status.asStateFlow()

    companion object {
        const val IELTS_CORE_START_ID = 100001
        const val IT_INDUSTRY_START_ID = 200001

        // Active Gemini model candidates (managed with smart cooldowns in GeminiApiKeyManager)
        val MODEL_CANDIDATES = GeminiApiKeyManager.ACTIVE_GEMINI_MODELS

        val IELTS_TOPICS = listOf(
            "Academic Writing Task 1 - charts, trends, comparisons",
            "Academic Writing Task 2 - essays, arguments, linking",
            "Environment, climate change, energy, sustainability",
            "Education, research, universities, learning",
            "Technology, science, innovation, AI",
            "Health, medicine, lifestyle, psychology",
            "Society, culture, globalization, demographics",
            "Economy, business, work, employment, finance",
            "Government, law, policy, crime, justice",
            "Media, communication, arts, literature",
            "Urbanization, transport, housing, infrastructure"
        )

        val IT_TOPICS = listOf(
            "Programming fundamentals, algorithms, data structures",
            "Software engineering, architecture, design patterns, clean code",
            "Web development - frontend, backend, APIs, HTTP, REST",
            "Databases, SQL, NoSQL, indexing, transactions, caching",
            "Cloud computing, DevOps, Docker, Kubernetes, CI/CD",
            "Cybersecurity, encryption, authentication, vulnerabilities",
            "Networking, protocols, DNS, TCP/IP, load balancing",
            "Operating systems, Linux, processes, memory, concurrency",
            "Mobile development - Android, Kotlin, Jetpack Compose, UI/UX",
            "Artificial intelligence, machine learning, LLMs, data science",
            "Git, version control, code review, testing, debugging",
            "Agile, Scrum, product management, tech workplace phrases"
        )

        fun startIdForDeck(deckId: String): Int = when (deckId) {
            "ielts_core" -> IELTS_CORE_START_ID
            "it_industry" -> IT_INDUSTRY_START_ID
            else -> 300001
        }
    }

    fun isApiKeyConfigured(): Boolean {
        val keys = (apiKeysProvider?.invoke()?.filter { it.isNotBlank() } ?: listOf(apiKeyProvider())).map { it.trim() }
        return keys.any { it.isNotBlank() && it != "MY_GEMINI_API_KEY" && it != "YOUR_GEMINI_API_KEY" && it != "null" }
    }

    /**
     * Enriches a single word (all its POS variants in the deck) with Arabic meaning, US IPA,
     * English definition (if empty), and Arabic example translations via Gemini, and caches it in Room.
     * Never overwrites entries where `arabicSource == "user"`.
     */
    suspend fun enrichSingleWord(deckId: String, wordText: String): Result<Int> = withContext(Dispatchers.IO) {
        val variants = if (deckId.isNotBlank()) {
            dao.getWordVariants(deckId, wordText)
        } else {
            dao.getWordVariantsAcrossDecks(wordText)
        }
        if (variants.isEmpty()) {
            return@withContext Result.success(0)
        }

        _status.value = AiTaskStatus(
            isRunning = true,
            title = "Translating \"$wordText\"...",
            detail = "Fetching Arabic meaning, US IPA & example translations",
            processedCount = 0,
            totalCount = variants.size
        )

        return@withContext try {
            val count = try {
                if (isApiKeyConfigured()) {
                    translateEntriesBatchInternal(variants)
                } else {
                    val freePayloads = FreeDictionaryAndTranslationEngine.translateEntriesBatchFree(variants)
                    if (freePayloads.isNotEmpty()) dao.saveAiTranslatedBatchTransaction(freePayloads) else 0
                }
            } catch (_: Exception) {
                val freePayloads = FreeDictionaryAndTranslationEngine.translateEntriesBatchFree(variants)
                if (freePayloads.isNotEmpty()) dao.saveAiTranslatedBatchTransaction(freePayloads) else 0
            }
            val updatedVariants = if (deckId.isNotBlank()) {
                dao.getWordVariants(deckId, wordText)
            } else {
                dao.getWordVariantsAcrossDecks(wordText)
            }
            val first = updatedVariants.firstOrNull()
            if (first != null && first.arabic.isNotBlank()) {
                dao.upsertAiCache(
                    AiCacheEntity(
                        cacheKey = "${first.deck}:${first.text.lowercase()}",
                        word = first.text,
                        ipa = first.ipa,
                        syllables = first.syllables,
                        arabicSummary = updatedVariants.joinToString(" • ") { "${it.pos}: ${it.arabic}" },
                        extraExamplesJson = first.examplesEn,
                        detailedExplanationAr = first.examplesAr
                    )
                )
            }
            _status.value = AiTaskStatus(
                isRunning = false,
                title = "Translated \"$wordText\"",
                detail = "Updated $count variant(s) in Room SQLite (source: ai)",
                processedCount = count,
                totalCount = variants.size
            )
            Result.success(count)
        } catch (e: Exception) {
            _status.value = AiTaskStatus(
                isRunning = false,
                title = "Translation failed",
                error = e.message ?: "Unknown Gemini API error"
            )
            Result.failure(e)
        }
    }

    /**
     * Explains grammar and vocabulary for a saved sentence in simple Arabic ONLY when the user taps
     * the AI sparkle icon, and saves the explanation permanently in `sentences.ai_explanation`
     * so the same sentence is never requested twice.
     */
    suspend fun explainSentenceInSimpleArabic(sentenceId: Long): Result<String> = withContext(Dispatchers.IO) {
        val existingSentence = dao.getSentenceById(sentenceId)
            ?: return@withContext Result.failure(IllegalArgumentException("Sentence #$sentenceId not found"))

        // If already explained, return cached explanation immediately without calling Gemini again!
        if (existingSentence.aiExplanation.isNotBlank()) {
            return@withContext Result.success(existingSentence.aiExplanation)
        }

        if (!isApiKeyConfigured()) {
            val msg = "GEMINI_API_KEY is not configured. Please add your Gemini API key in settings."
            _status.value = AiTaskStatus(isRunning = false, error = msg)
            return@withContext Result.failure(IllegalStateException(msg))
        }

        _status.value = AiTaskStatus(
            isRunning = true,
            title = "Explaining sentence grammar & vocabulary...",
            detail = existingSentence.englishText.take(60),
            processedCount = 0,
            totalCount = 1
        )

        return@withContext try {
            val focusHint = if (existingSentence.sourceWord.isNotBlank()) {
                "Focus word/phrase: \"${existingSentence.sourceWord}\"\n"
            } else {
                ""
            }
            val prompt = """
You are an expert English teacher for Arabic speakers.
Explain the grammar and key vocabulary of this English sentence in simple, clear Arabic (3 to 5 short bullet points):
Sentence: "${existingSentence.englishText}"
${focusHint}Return a JSON object with a single string field "explanation_ar" containing the Arabic explanation:
- 1–2 points explaining the tense and grammar structure (التركيب النحوي والزمن)
- 2–3 points explaining key vocabulary or idioms in context (المفردات والتعبيرات المهمة)
Keep the Arabic natural, clear, and easy to read.
""".trimIndent()

            val rawResponse = callGeminiJson(prompt)
            val parsedExplanation = runCatching {
                var cleaned = rawResponse.trim()
                cleaned = cleaned.replace(Regex("^```(?:json)?\\s*", RegexOption.MULTILINE), "")
                cleaned = cleaned.replace(Regex("\\s*```$", RegexOption.MULTILINE), "").trim()
                val obj = json.parseToJsonElement(cleaned).jsonObject
                obj["explanation_ar"]?.jsonPrimitive?.content?.trim().orEmpty()
            }.getOrDefault("").ifBlank {
                rawResponse
                    .replace(Regex("^```(?:json)?\\s*", RegexOption.MULTILINE), "")
                    .replace(Regex("\\s*```$", RegexOption.MULTILINE), "")
                    .trim()
            }

            if (parsedExplanation.isBlank()) {
                throw IllegalStateException("Received empty explanation from Gemini")
            }

            // Permanently save in the sentences table so the same sentence is never requested twice
            dao.updateSentenceAiExplanation(sentenceId, parsedExplanation)

            _status.value = AiTaskStatus(
                isRunning = false,
                title = "Grammar & vocabulary explanation saved",
                detail = existingSentence.englishText.take(50),
                processedCount = 1,
                totalCount = 1
            )
            Result.success(parsedExplanation)
        } catch (e: Exception) {
            _status.value = AiTaskStatus(
                isRunning = false,
                title = "Sentence AI explanation failed",
                error = e.message ?: "Gemini API error"
            )
            Result.failure(e)
        }
    }

    /**
     * Translates a batch of entries that currently have an empty `arabic` field (and are not user-edited)
     * using the prompt from `generate_decks.py`.
     */
    suspend fun translateEmptyEntriesBatch(batchSize: Int = 20, deckFilter: String = ""): Result<Int> = withContext(Dispatchers.IO) {
        if (!isApiKeyConfigured()) {
            val msg = "GEMINI_API_KEY is not configured. Please add your Gemini API key in settings."
            _status.value = AiTaskStatus(isRunning = false, error = msg)
            return@withContext Result.failure(IllegalStateException(msg))
        }

        val todo = if (deckFilter.isNotBlank()) {
            dao.getUntranslatedEntriesForDeckBatch(deckFilter, batchSize)
        } else {
            dao.getUntranslatedEntriesBatch(batchSize)
        }

        if (todo.isEmpty()) {
            _status.value = AiTaskStatus(
                isRunning = false,
                title = "All entries already have Arabic translations!",
                detail = "0 empty Arabic rows remaining."
            )
            return@withContext Result.success(0)
        }

        _status.value = AiTaskStatus(
            isRunning = true,
            title = "Translating batch of ${todo.size} entries...",
            detail = "Words: ${todo.take(4).joinToString(", ") { it.text }}...",
            processedCount = 0,
            totalCount = todo.size
        )

        return@withContext try {
            val updated = translateEntriesBatchInternal(todo)
            val remaining = dao.getEmptyArabicCount()
            _status.value = AiTaskStatus(
                isRunning = false,
                title = "Batch translation complete (+$updated words)",
                detail = "$remaining entries still have empty Arabic.",
                processedCount = updated,
                totalCount = todo.size
            )
            Result.success(updated)
        } catch (e: Exception) {
            _status.value = AiTaskStatus(
                isRunning = false,
                title = "Batch translation failed",
                error = e.message ?: "Gemini API request failed"
            )
            Result.failure(e)
        }
    }

    /**
     * Generates a batch of vocabulary entries for `ielts_core` (IDs from 100001) or `it_industry` (IDs from 200001)
     * using the exact topic list and prompt structure from `generate_decks.py`.
     * Running again continues from the highest existing ID in that deck's fixed range without duplicate IDs or words.
     */
    suspend fun generateDeckBatch(
        deckName: String,
        count: Int = 25,
        onDecksUpdated: suspend () -> Unit = {}
    ): Result<Int> = withContext(Dispatchers.IO) {
        if (!isApiKeyConfigured()) {
            val msg = "GEMINI_API_KEY is not configured. Please add your Gemini API key in settings."
            _status.value = AiTaskStatus(isRunning = false, error = msg)
            return@withContext Result.failure(IllegalStateException(msg))
        }

        val topics = if (deckName == "ielts_core") IELTS_TOPICS else IT_TOPICS
        val purpose = if (deckName == "ielts_core") {
            "the IELTS Academic exam (Band 7+)"
        } else {
            "working in software engineering & IT"
        }

        val existingWords = dao.getExistingWordsInDeck(deckName).toMutableSet()
        val topicIndex = (existingWords.size / 30) % topics.size
        val topic = topics[topicIndex]
        val minId = startIdForDeck(deckName)

        _status.value = AiTaskStatus(
            isRunning = true,
            title = "Generating $count entries for $deckName (IDs >= $minId)...",
            detail = "Topic: $topic",
            processedCount = 0,
            totalCount = count
        )

        return@withContext try {
            val avoidList = existingWords.toList().takeLast(120).joinToString(", ").ifBlank { "none" }
            val prompt = """
Generate $count unique English vocabulary entries for the deck "$deckName"
focused on the topic: "$topic".
Target learner: Arabic speaker preparing for $purpose.

Avoid these already-used words: $avoidList

Return a JSON array of objects with EXACTLY these fields:
- "text": the word or short technical term (lowercase unless acronym like API, SQL)
- "type": "word" | "academic" | "phrase"
- "pos": "noun" | "verb" | "adjective" | "adverb" | "phrase"
- "level": CEFR level "B1" | "B2" | "C1"
- "ipa": US IPA like "/kənˈsɪstənt/"
- "definition_en": clear, concise English definition (1 sentence)
- "examples_en": TWO natural example sentences separated by " | "
- "arabic": natural, accurate Arabic meanings (2-3 synonyms separated by "، ")
- "examples_ar": Arabic translation of the two examples, separated by " | "
""".trimIndent()

            val rawResponse = callGeminiJson(prompt)
            val generatedItems = parseJsonArray<GeneratedDeckItem>(rawResponse)

            var nextId = dao.getMaxNumericIdForDeckInRange(
                deck = deckName,
                minId = minId,
                minIdMinusOne = minId - 1
            ) + 1

            val newEntries = ArrayList<EntryEntity>(generatedItems.size)
            for (item in generatedItems) {
                val word = item.text.trim()
                if (word.isEmpty() || existingWords.contains(word.lowercase())) continue
                existingWords.add(word.lowercase())
                val arabicText = item.arabic.trim()
                newEntries.add(
                    EntryEntity(
                        id = nextId.toString(),
                        type = item.type.trim().ifBlank { "word" },
                        deck = deckName,
                        text = word,
                        pos = item.pos.trim(),
                        level = item.level.trim().ifBlank { "B2" },
                        definitionEn = item.definitionEn.trim(),
                        noteEn = "",
                        examplesEn = item.examplesEn.trim(),
                        examplesAr = item.examplesAr.trim(),
                        definitionUrl = "",
                        audioUrl = "",
                        arabic = arabicText,
                        arabicSource = if (arabicText.isNotEmpty()) "ai" else "",
                        ipa = item.ipa.trim(),
                        dataVersion = DataImportRepository.DATA_VERSION
                    )
                )
                nextId++
            }

            if (newEntries.isNotEmpty()) {
                dao.upsertEntriesBatchPreservingUserEdits(newEntries)
                onDecksUpdated()
            }

            val newDeckTotal = dao.getEntryCountForDeck(deckName)
            _status.value = AiTaskStatus(
                isRunning = false,
                title = "Added ${newEntries.size} entries to $deckName",
                detail = "$deckName now has $newDeckTotal rows in SQLite (IDs $minId..${nextId - 1}).",
                processedCount = newEntries.size,
                totalCount = count
            )
            Result.success(newEntries.size)
        } catch (e: Exception) {
            _status.value = AiTaskStatus(
                isRunning = false,
                title = "Failed to generate $deckName batch",
                error = e.message ?: "Gemini API error"
            )
            Result.failure(e)
        }
    }

    private suspend fun translateEntriesBatchInternal(entries: List<EntryEntity>): Int {
        val rawJson = requestBatchTranslationJson(entries)
        val validation = parseAndValidateBatchResponse(entries, rawJson)
        if (validation.validPayloads.isEmpty()) return 0
        return dao.saveAiTranslatedBatchTransaction(validation.validPayloads)
    }

    /**
     * Sends a single batch (up to 25 entries) to Gemini requesting:
     * - "id": exact entry id
     * - "arabic": 2-4 accurate Arabic meanings separated by "، "
     * - "ipa": US IPA pronunciation
     * - "examples_ar": Arabic translation of examples_en when examples_en exists
     */
    suspend fun requestBatchTranslationJson(entries: List<EntryEntity>): String = withContext(Dispatchers.IO) {
        val customTransport = batchTransportOverride
        if (customTransport != null) {
            return@withContext customTransport(entries)
        }

        val itemsJson = buildJsonArray {
            for (r in entries) {
                add(
                    buildJsonObject {
                        put("id", r.id)
                        put("text", r.text)
                        put("pos", r.pos)
                        put("level", r.level)
                        put("definition_en", r.definitionEn)
                        put("examples_en", r.examplesEn)
                    }
                )
            }
        }.toString()

        val prompt = """
You are building an English->Arabic vocabulary dictionary for Arabic learners.
For each item in the input JSON array, return a JSON object with EXACTLY these fields:
- "id": SAME id as input (as string)
- "arabic": 2 to 4 accurate, natural Arabic meanings for this part of speech separated by "، "
- "ipa": US IPA pronunciation like "/əˈbændən/"
- "examples_ar": if input "examples_en" is non-empty, provide the accurate Arabic translation of the example sentence(s) (separated by " | " if multiple); if input "examples_en" is empty, return ""

Return ONLY a valid JSON array with no markdown or extra commentary.

Input:
$itemsJson
""".trimIndent()

        callGeminiJson(prompt)
    }

    @Volatile
    var batchTransportOverride: (suspend (List<EntryEntity>) -> String)? = null

    /**
     * Strips markdown code fences, parses the JSON array element-by-element, validates every item,
     * and discards any invalid items without saving.
     */
    fun parseAndValidateBatchResponse(
        requestedEntries: List<EntryEntity>,
        rawResponse: String
    ): BatchValidationResult {
        val requestedById = requestedEntries.associateBy { it.id.trim() }
        val cleaned = stripCodeFences(rawResponse)

        val jsonArray = try {
            val startIdx = cleaned.indexOf('[')
            val endIdx = cleaned.lastIndexOf(']')
            if (startIdx == -1 || endIdx <= startIdx) {
                return BatchValidationResult(
                    validPayloads = emptyList(),
                    invalidEntryIds = requestedEntries.map { it.id }
                )
            }
            val arraySubstring = cleaned.substring(startIdx, endIdx + 1)
            json.parseToJsonElement(arraySubstring).jsonArray
        } catch (_: Exception) {
            return BatchValidationResult(
                validPayloads = emptyList(),
                invalidEntryIds = requestedEntries.map { it.id }
            )
        }

        val validPayloads = ArrayList<com.example.data.local.AiBatchSavePayload>(requestedEntries.size)
        val seenValidIds = HashSet<String>()

        for (element in jsonArray) {
            try {
                val obj = element.jsonObject
                val rawId = obj["id"]?.jsonPrimitive?.content?.trim().orEmpty()
                if (rawId.isEmpty() || seenValidIds.contains(rawId)) continue

                val sourceEntry = requestedById[rawId] ?: continue
                val arabicRaw = obj["arabic"]?.jsonPrimitive?.content?.trim().orEmpty()
                val ipaRaw = obj["ipa"]?.jsonPrimitive?.content?.trim().orEmpty()
                val examplesArRaw = obj["examples_ar"]?.jsonPrimitive?.content?.trim().orEmpty()

                // Validate arabic: must be non-blank and contain at least one Arabic script character
                if (!isValidArabicMeaning(arabicRaw)) {
                    continue
                }

                // Validate ipa: must be non-blank when requested for vocabulary entries
                if (ipaRaw.isBlank()) {
                    continue
                }

                // Validate examples_ar: when examples_en exists on the entry, examples_ar must be non-blank and contain Arabic
                val hasExamplesEn = sourceEntry.examplesEn.trim().isNotEmpty()
                if (hasExamplesEn && !containsArabicScript(examplesArRaw)) {
                    continue
                }

                seenValidIds.add(rawId)
                validPayloads.add(
                    com.example.data.local.AiBatchSavePayload(
                        id = sourceEntry.id,
                        text = sourceEntry.text,
                        pos = sourceEntry.pos,
                        arabic = normalizeArabicMeanings(arabicRaw),
                        ipa = ipaRaw,
                        examplesEn = sourceEntry.examplesEn,
                        examplesAr = if (hasExamplesEn) examplesArRaw else ""
                    )
                )
            } catch (_: Exception) {
                // Discard malformed item without failing the entire batch
            }
        }

        val invalidIds = requestedEntries.map { it.id }.filter { !seenValidIds.contains(it.idOrTrimmed()) }
        return BatchValidationResult(
            validPayloads = validPayloads,
            invalidEntryIds = invalidIds
        )
    }

    private fun String.idOrTrimmed(): String = this.trim()

    fun stripCodeFences(rawText: String): String {
        var cleaned = rawText.trim()
        cleaned = cleaned.replace(Regex("^```(?:json)?\\s*", setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE)), "")
        cleaned = cleaned.replace(Regex("\\s*```$", RegexOption.MULTILINE), "")
        return cleaned.trim()
    }

    private fun containsArabicScript(text: String): Boolean {
        return text.isNotBlank() && text.any { ch -> ch in '\u0600'..'\u06FF' || ch in '\u0750'..'\u077F' || ch in '\uFB50'..'\uFDFF' || ch in '\uFE70'..'\uFEFF' }
    }

    private fun isValidArabicMeaning(arabic: String): Boolean {
        if (!containsArabicScript(arabic)) return false
        val meanings = arabic
            .split(Regex("[،,;؛/|]+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        return meanings.isNotEmpty()
    }

    private fun normalizeArabicMeanings(arabic: String): String {
        val meanings = arabic
            .split(Regex("[،,;؛]+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(4)
        return if (meanings.isEmpty()) arabic.trim() else meanings.joinToString("، ")
    }

    internal fun callGeminiJson(prompt: String): String {
        val keys = (apiKeysProvider?.invoke()?.filter { it.isNotBlank() } ?: listOf(apiKeyProvider()))
            .map { it.trim() }
            .filter { it.isNotBlank() && it != "YOUR_GEMINI_API_KEY" && it != "MY_GEMINI_API_KEY" && it != "null" }

        return GeminiApiKeyManager.executeAiPrompt(
            prompt = prompt,
            keys = keys,
            expectsJson = true,
            timeoutMs = 30_000
        )
    }

    /**
     * Uses the Free Dictionary & Translation Engine (Google GTX Dictionary & Batch API + LibreTranslate + MyMemory + dictionaryapi.dev)
     * to translate words, examples, and missing IPAs with zero Gemini quota required.
     */
    suspend fun translateWithMyMemoryFallback(entries: List<EntryEntity>): List<com.example.data.local.AiBatchSavePayload> {
        return FreeDictionaryAndTranslationEngine.translateEntriesBatchFree(entries)
    }

    private inline fun <reified T> parseJsonArray(rawText: String): List<T> {
        var cleaned = stripCodeFences(rawText)
        val startIdx = cleaned.indexOf('[')
        val endIdx = cleaned.lastIndexOf(']')
        if (startIdx != -1 && endIdx > startIdx) {
            cleaned = cleaned.substring(startIdx, endIdx + 1)
        }
        return json.decodeFromString(cleaned)
    }
}

class GeminiHttpException(
    val statusCode: Int,
    message: String
) : java.io.IOException(message) {
    val isRetryableRateLimitOrServer: Boolean
        get() = statusCode == 429 || statusCode in 500..599
}

data class BatchValidationResult(
    val validPayloads: List<com.example.data.local.AiBatchSavePayload>,
    val invalidEntryIds: List<String>
)

