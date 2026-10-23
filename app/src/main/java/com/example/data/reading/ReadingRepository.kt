package com.example.data.reading

import android.content.Context
import android.net.Uri
import com.example.data.local.AiCacheEntity
import com.example.data.local.AiHistoryEntity
import com.example.data.local.AttemptEntity
import com.example.data.local.CorrectionNotebookEntity
import com.example.data.local.EntryEntity
import com.example.data.local.ReadingProgressEntity
import com.example.data.local.ReadingQuestionItem
import com.example.data.local.ReadingSentenceItem
import com.example.data.local.ReadingTextEntity
import com.example.data.local.ReadingTextJson
import com.example.data.local.WordBooksDao
import com.example.data.local.WordProgressEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.InputStreamReader

class ReadingRepository(
    private val context: Context,
    private val dao: WordBooksDao
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    val textsFlow: Flow<List<ReadingTextEntity>> = dao.observeAllReadingTexts()
    val progressFlow: Flow<List<ReadingProgressEntity>> = dao.observeAllReadingProgress()

    /**
     * Reads `reading_texts.json` from assets and imports/upserts texts by id.
     * Never touches user progress in reading_progress.
     */
    suspend fun importReadingTextsIfNeeded(force: Boolean = false): Result<Int> = withContext(Dispatchers.IO) {
        val jsonString = loadAssetJsonString("reading_texts.json")
            ?: loadAssetJsonString("data_import/reading_texts.json")
            ?: return@withContext Result.failure(IllegalStateException("No reading_texts.json found in assets"))

        val parsed = parseReadingTextsFlexible(jsonString, defaultSource = "sample")
        val currentCount = dao.getReadingTextsCount()
        val currentVersion = prefs.getInt(KEY_DATA_VERSION, 0)
        val assetHash = jsonString.hashCode()
        val savedHash = prefs.getInt(KEY_ASSET_HASH, 0)

        if (!force && currentVersion >= READING_DATA_VERSION && currentCount >= parsed.size && assetHash == savedHash) {
            return@withContext Result.success(currentCount)
        }

        val count = upsertParsedTexts(parsed, defaultSource = "sample")
        prefs.edit()
            .putInt(KEY_DATA_VERSION, READING_DATA_VERSION)
            .putInt(KEY_ASSET_HASH, assetHash)
            .apply()
        Result.success(count)
    }

    suspend fun importReadingTextsFromAssets(force: Boolean = true): Result<Int> = withContext(Dispatchers.IO) {
        importReadingTextsIfNeeded(force = force)
    }

    suspend fun importReadingTextsFromJsonString(
        rawJson: String,
        source: String = "file"
    ): Result<Int> = withContext(Dispatchers.IO) {
        val clean = stripCodeFences(rawJson)
        if (clean.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Empty JSON content"))
        }

        val texts = try {
            parseReadingTextsFlexible(clean, defaultSource = source)
        } catch (e: Exception) {
            return@withContext Result.failure(IllegalArgumentException("Failed to parse JSON: ${e.message}", e))
        }

        if (texts.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("No valid reading texts found in JSON"))
        }

        val count = upsertParsedTexts(texts, defaultSource = source)
        Result.success(count)
    }

    suspend fun importReadingTextsFromUris(uris: List<Uri>): Result<Int> = withContext(Dispatchers.IO) {
        var totalImported = 0
        val errors = mutableListOf<String>()

        for (uri in uris) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                    InputStreamReader(stream, Charsets.UTF_8).use { it.readText() }
                }
                if (!content.isNullOrBlank()) {
                    val res = importReadingTextsFromJsonString(content, source = "file")
                    if (res.isSuccess) {
                        totalImported += res.getOrDefault(0)
                    } else {
                        errors.add(res.exceptionOrNull()?.message ?: "Import error")
                    }
                }
            } catch (e: Exception) {
                errors.add(e.message ?: "Failed to read file")
            }
        }

        if (totalImported > 0 || errors.isEmpty()) {
            Result.success(totalImported)
        } else {
            Result.failure(Exception(errors.joinToString("; ")))
        }
    }

    private suspend fun upsertParsedTexts(
        texts: List<ReadingTextJson>,
        defaultSource: String
    ): Int {
        var count = 0
        for ((index, parsed) in texts.withIndex()) {
            val id = parsed.id.trim().ifBlank {
                "r_${System.currentTimeMillis().toString().takeLast(6)}_${index + 1}"
            }
            val sentencesJson = json.encodeToString(parsed.sentences)
            val questionsJson = json.encodeToString(parsed.questions)
            val targetWordsJson = json.encodeToString(parsed.targetWords)
            val grammarIdsJson = json.encodeToString(parsed.grammarIds)
            val existing = dao.getReadingTextById(id)

            if (existing == null) {
                dao.upsertReadingText(
                    ReadingTextEntity(
                        id = id,
                        level = parsed.level.trim().uppercase().ifBlank { "B1" },
                        topic = parsed.topic.trim().ifBlank { "General" },
                        titleEn = parsed.titleEn.trim(),
                        titleAr = parsed.titleAr.trim(),
                        sentencesJson = sentencesJson,
                        questionsJson = questionsJson,
                        source = parsed.source.ifBlank { defaultSource },
                        targetWordsJson = targetWordsJson,
                        grammarIdsJson = grammarIdsJson,
                        originalTextId = parsed.originalTextId,
                        dataVersion = READING_DATA_VERSION,
                        createdAt = System.currentTimeMillis()
                    )
                )
            } else {
                // Upsert content by ID without wiping user progress
                dao.updateReadingTextContentById(
                    id = id,
                    level = parsed.level.trim().uppercase().ifBlank { existing.level },
                    topic = parsed.topic.trim().ifBlank { existing.topic },
                    titleEn = parsed.titleEn.trim().ifBlank { existing.titleEn },
                    titleAr = parsed.titleAr.trim().ifBlank { existing.titleAr },
                    sentencesJson = sentencesJson,
                    questionsJson = questionsJson,
                    grammarIdsJson = grammarIdsJson,
                    dataVersion = READING_DATA_VERSION,
                    targetWordsJson = targetWordsJson
                )
            }
            count++
        }
        return count
    }

    /**
     * Splits user-pasted text into sentences and saves it locally marked "source": "user".
     */
    suspend fun savePastedText(
        titleEn: String,
        rawText: String,
        topic: String = "General",
        level: String = "B1"
    ): Result<ReadingTextEntity> = withContext(Dispatchers.IO) {
        val cleanText = rawText.trim()
        if (cleanText.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Text cannot be empty"))
        }

        val rawSentences = splitTextIntoSentences(cleanText)
        if (rawSentences.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Could not extract sentences"))
        }

        val sentenceItems = rawSentences.map { en ->
            ReadingSentenceItem(en = en.trim(), ar = "")
        }

        val id = "r_user_${System.currentTimeMillis().toString().takeLast(6)}"
        val entity = ReadingTextEntity(
            id = id,
            level = level.trim().uppercase().ifBlank { "B1" },
            topic = topic.trim().ifBlank { "My Texts" },
            titleEn = titleEn.trim().ifBlank { rawSentences.first().take(40) },
            titleAr = "",
            sentencesJson = json.encodeToString(sentenceItems),
            questionsJson = "[]",
            source = "user",
            targetWordsJson = "[]",
            grammarIdsJson = "[]",
            originalTextId = null,
            dataVersion = READING_DATA_VERSION,
            createdAt = System.currentTimeMillis()
        )
        dao.upsertReadingText(entity)
        Result.success(entity)
    }

    suspend fun updatePastedTextWithAiResults(
        textId: String,
        sentences: List<ReadingSentenceItem>,
        questions: List<ReadingQuestionItem>,
        titleAr: String = ""
    ) = withContext(Dispatchers.IO) {
        val existing = dao.getReadingTextById(textId) ?: return@withContext
        val sentencesJson = json.encodeToString(sentences)
        val questionsJson = json.encodeToString(questions)
        dao.updateReadingTextSentencesAndQuestions(textId, sentencesJson, questionsJson)
        if (titleAr.isNotBlank() && existing.titleAr.isBlank()) {
            dao.updateReadingTextContentById(
                id = textId,
                level = existing.level,
                topic = existing.topic,
                titleEn = existing.titleEn,
                titleAr = titleAr,
                sentencesJson = sentencesJson,
                questionsJson = questionsJson,
                grammarIdsJson = existing.grammarIdsJson,
                dataVersion = existing.dataVersion
            )
        }
    }

    suspend fun saveAiCreatedText(
        text: ReadingTextJson,
        targetWords: List<String> = emptyList(),
        originalTextId: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val id = if (text.id.isNotBlank() && dao.getReadingTextById(text.id) == null) {
            text.id.trim()
        } else {
            "r_ai_${System.currentTimeMillis().toString().takeLast(6)}"
        }

        val entity = ReadingTextEntity(
            id = id,
            level = text.level.trim().uppercase().ifBlank { "B1" },
            topic = text.topic.trim().ifBlank { "AI Reading" },
            titleEn = text.titleEn.trim(),
            titleAr = text.titleAr.trim(),
            sentencesJson = json.encodeToString(text.sentences),
            questionsJson = json.encodeToString(text.questions),
            source = "ai",
            targetWordsJson = json.encodeToString(targetWords.ifEmpty { text.targetWords }),
            grammarIdsJson = json.encodeToString(text.grammarIds),
            originalTextId = originalTextId ?: text.originalTextId,
            dataVersion = READING_DATA_VERSION,
            createdAt = System.currentTimeMillis()
        )
        dao.upsertReadingText(entity)
        Result.success(id)
    }

    suspend fun getReadingTextById(id: String): ReadingTextEntity? = withContext(Dispatchers.IO) {
        dao.getReadingTextById(id)
    }

    suspend fun deleteReadingText(id: String) = withContext(Dispatchers.IO) {
        dao.deleteReadingTextById(id)
    }

    suspend fun saveReadingProgress(
        textId: String,
        completedQuestions: Int,
        totalQuestions: Int,
        score: Int,
        isCompleted: Boolean,
        nowMillis: Long = System.currentTimeMillis()
    ) = withContext(Dispatchers.IO) {
        val existing = dao.getReadingProgress(textId)
        val bestScore = maxOf(existing?.bestScore ?: 0, score)
        val progress = ReadingProgressEntity(
            textId = textId,
            completedQuestions = completedQuestions,
            totalQuestions = totalQuestions,
            bestScore = bestScore,
            lastScore = score,
            isCompleted = isCompleted || (existing?.isCompleted == true),
            updatedAt = nowMillis
        )
        val attempt = AttemptEntity(
            activityType = "reading_quiz",
            itemId = textId,
            timestamp = nowMillis,
            sm2Rating = null,
            isCorrect = score >= 60,
            score = score.toFloat(),
            deck = null
        )
        dao.upsertReadingProgressWithAttempt(progress, attempt)
    }

    /**
     * Looks up a word in the local dictionary across all decks.
     * Cleans punctuation and tests base/inflected forms if direct match is empty.
     */
    suspend fun lookupWordLocally(rawWord: String): List<EntryEntity> = withContext(Dispatchers.IO) {
        val clean = cleanWordForLookup(rawWord)
        if (clean.isBlank()) return@withContext emptyList()

        // 1. Direct match
        val direct = dao.getWordVariantsAcrossDecks(clean)
        if (direct.isNotEmpty()) return@withContext direct

        // 2. Lowercase match
        val lower = clean.lowercase()
        val lowerMatch = dao.getWordVariantsAcrossDecks(lower)
        if (lowerMatch.isNotEmpty()) return@withContext lowerMatch

        // 3. Common inflections: plural -s/-es, past -ed, gerund -ing
        val candidates = mutableListOf<String>()
        if (lower.endsWith("ies") && lower.length > 3) candidates.add(lower.dropLast(3) + "y")
        if (lower.endsWith("es") && lower.length > 2) candidates.add(lower.dropLast(2))
        if (lower.endsWith("s") && lower.length > 1) candidates.add(lower.dropLast(1))
        if (lower.endsWith("ing") && lower.length > 3) {
            candidates.add(lower.dropLast(3))
            candidates.add(lower.dropLast(3) + "e")
        }
        if (lower.endsWith("ed") && lower.length > 2) {
            candidates.add(lower.dropLast(2))
            candidates.add(lower.dropLast(1))
        }

        for (cand in candidates) {
            val candMatches = dao.getWordVariantsAcrossDecks(cand)
            if (candMatches.isNotEmpty()) return@withContext candMatches
        }

        emptyList()
    }

    /**
     * Saves favorite flag for a word looked up from Reading without touching `lastReviewedTimestamp`
     * or `firstLearnedTimestamp` and without creating an `AttemptEntity`.
     */
    suspend fun saveWordFavorite(deckId: String, wordText: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        val clean = cleanWordForLookup(wordText).lowercase()
        val cardKey = "${deckId.trim()}:${clean}"
        val existing = dao.getWordProgress(cardKey) ?: dao.getWordProgress("${deckId}_${clean}")
        val resolvedEntryId = existing?.entryId ?: dao.getPrimaryEntryIdForCard(deckId, clean)
        val updated = (existing ?: WordProgressEntity(
            cardKey = cardKey,
            entryId = resolvedEntryId,
            deck = deckId,
            text = clean
        )).copy(
            cardKey = cardKey,
            entryId = resolvedEntryId,
            isFavorite = isFavorite
        )
        dao.upsertWordProgress(updated)
    }

    suspend fun getWordFavoriteStatus(deckId: String, wordText: String): Boolean = withContext(Dispatchers.IO) {
        val clean = cleanWordForLookup(wordText).lowercase()
        val cardKey = "${deckId.trim()}:${clean}"
        (dao.getWordProgress(cardKey) ?: dao.getWordProgress("${deckId}_${clean}"))?.isFavorite == true
    }

    /**
     * Records a passive Tap-to-Translate lookup in a reading story:
     * increments `lookup_count` and sets `last_looked_up_timestamp` so the word transitions
     * to the `SEEN` stage and receives a priority boost in upcoming vocabulary selections.
     */
    suspend fun recordWordLookup(
        wordText: String,
        preferredDeckId: String? = null,
        nowMillis: Long = System.currentTimeMillis()
    ): WordProgressEntity? = withContext(Dispatchers.IO) {
        val clean = cleanWordForLookup(wordText).lowercase()
        if (clean.isBlank()) return@withContext null
        val variants = lookupWordLocally(clean)
        val matchedEntry = if (!preferredDeckId.isNullOrBlank()) {
            variants.firstOrNull { it.deck == preferredDeckId } ?: variants.firstOrNull()
        } else {
            variants.firstOrNull { it.deck == "oxford5000" }
                ?: variants.firstOrNull { it.deck == "oxford_opal" }
                ?: variants.firstOrNull()
        }
        val deckId = matchedEntry?.deck ?: preferredDeckId ?: "oxford5000"
        val headword = matchedEntry?.text?.trim()?.lowercase() ?: clean
        val cardKey = "$deckId:$headword"
        val existing = dao.getWordProgress(cardKey) ?: dao.getWordProgress("${deckId}_$headword")
        val resolvedEntryId = existing?.entryId ?: matchedEntry?.id ?: dao.getPrimaryEntryIdForCard(deckId, headword)
        val updated = (existing ?: WordProgressEntity(
            cardKey = cardKey,
            entryId = resolvedEntryId,
            deck = deckId,
            text = headword
        )).copy(
            cardKey = cardKey,
            entryId = resolvedEntryId,
            lookupCount = (existing?.lookupCount ?: 0) + 1,
            lastLookedUpTimestamp = nowMillis
        )
        dao.upsertWordProgress(updated)
        updated
    }

    /**
     * Schedules a looked-up word for immediate SRS review today ("Add to Today's Practice").
     */
    suspend fun scheduleWordForPracticeToday(
        wordText: String,
        preferredDeckId: String? = null,
        nowMillis: Long = System.currentTimeMillis()
    ): WordProgressEntity? = withContext(Dispatchers.IO) {
        val lookedUp = recordWordLookup(wordText, preferredDeckId, nowMillis) ?: return@withContext null
        val updated = if (lookedUp.dueTimestamp == 0L || lookedUp.dueTimestamp > nowMillis) {
            lookedUp.copy(dueTimestamp = nowMillis)
        } else {
            lookedUp
        }
        dao.upsertWordProgress(updated)
        updated
    }

    suspend fun getSavedTargetWords(limit: Int = 15): List<String> = withContext(Dispatchers.IO) {
        val progressList = dao.getAllWordProgressList()
        val favorites = progressList.filter { it.isFavorite }.map { it.text }
        if (favorites.isNotEmpty()) {
            favorites.shuffled().take(limit)
        } else {
            // Fallback to words studied or due
            progressList.map { it.text }.shuffled().take(limit)
        }
    }

    suspend fun getCachedWordExplanation(
        textId: String,
        sentenceIndex: Int,
        word: String
    ): String? = withContext(Dispatchers.IO) {
        val key = buildWordCacheKey(textId, sentenceIndex, word)
        dao.getAiCache(key)?.detailedExplanationAr?.ifBlank { null }?.let {
            com.example.data.ai.AiPromptContract.sanitizeAiPlainText(it)
        }
    }

    suspend fun saveCachedWordExplanation(
        textId: String,
        sentenceIndex: Int,
        word: String,
        explanationAr: String
    ) = withContext(Dispatchers.IO) {
        val key = buildWordCacheKey(textId, sentenceIndex, word)
        val cleanExplanation = com.example.data.ai.AiPromptContract.sanitizeAiPlainText(explanationAr)
        dao.upsertAiCache(
            AiCacheEntity(
                cacheKey = key,
                word = word,
                ipa = "",
                syllables = "",
                arabicSummary = "Word Context: $word",
                extraExamplesJson = "",
                detailedExplanationAr = cleanExplanation,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    private fun buildWordCacheKey(textId: String, sentenceIndex: Int, word: String): String {
        val cleanWord = cleanWordForLookup(word).lowercase()
        return com.example.data.ai.AiPromptContract.versionedCacheKey(
            namespace = "reading_word",
            rawKey = "${textId}:${sentenceIndex}:${cleanWord}"
        )
    }

    suspend fun getCachedCorrectionResult(lessonTitle: String, userSentence: String): CorrectionAiResult? = withContext(Dispatchers.IO) {
        val key = com.example.data.ai.AiPromptContract.versionedCacheKey(
            namespace = "correction",
            rawKey = "${lessonTitle.trim().lowercase()}:${userSentence.trim().lowercase()}"
        )
        val cached = dao.getAiCache(key) ?: return@withContext null
        try {
            json.decodeFromString<CorrectionAiResult>(cached.extraExamplesJson)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveCachedCorrectionResult(lessonTitle: String, userSentence: String, result: CorrectionAiResult) = withContext(Dispatchers.IO) {
        val key = com.example.data.ai.AiPromptContract.versionedCacheKey(
            namespace = "correction",
            rawKey = "${lessonTitle.trim().lowercase()}:${userSentence.trim().lowercase()}"
        )
        dao.upsertAiCache(
            AiCacheEntity(
                cacheKey = key,
                word = userSentence.take(50),
                ipa = "",
                syllables = "",
                arabicSummary = result.correctedSentence,
                extraExamplesJson = json.encodeToString(result),
                detailedExplanationAr = com.example.data.ai.AiPromptContract.sanitizeAiPlainText(result.explanationAr),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    fun observeTextChat(textId: String): Flow<List<AiHistoryEntity>> =
        dao.observeAiHistory(CONTEXT_READING_CHAT, textId)

    suspend fun addChatMessage(textId: String, role: String, text: String): Long = withContext(Dispatchers.IO) {
        dao.insertAiHistory(
            AiHistoryEntity(
                contextType = CONTEXT_READING_CHAT,
                contextId = textId,
                role = role,
                text = text,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    fun observeOpenQuestionHistory(textId: String): Flow<List<AiHistoryEntity>> =
        dao.observeAiHistory(CONTEXT_READING_OPEN_QUESTION, textId)

    suspend fun addOpenQuestionHistory(textId: String, role: String, text: String): Long = withContext(Dispatchers.IO) {
        dao.insertAiHistory(
            AiHistoryEntity(
                contextType = CONTEXT_READING_OPEN_QUESTION,
                contextId = textId,
                role = role,
                text = text,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    private fun cleanWordForLookup(word: String): String {
        return word.trim()
            .replace(Regex("^[^a-zA-Z0-9]+|[^a-zA-Z0-9]+$"), "")
            .trim()
    }

    private fun splitTextIntoSentences(text: String): List<String> {
        val clean = text.replace("\r\n", "\n").replace("\r", "\n")
        val lines = clean.split("\n").filter { it.isNotBlank() }
        val results = mutableListOf<String>()

        for (line in lines) {
            // Split line by sentence terminators (. ! ?)
            val sentenceRegex = Regex("(?<=[.!?])\\s+")
            val parts = line.split(sentenceRegex)
            for (p in parts) {
                val trimmed = p.trim()
                if (trimmed.isNotBlank()) {
                    results.add(trimmed)
                }
            }
        }
        return results
    }

    fun parseReadingTextsFlexible(
        jsonString: String,
        defaultSource: String = "file"
    ): List<ReadingTextJson> {
        val root = try {
            json.parseToJsonElement(jsonString)
        } catch (_: Exception) {
            return json.decodeFromString<List<ReadingTextJson>>(jsonString)
        }

        val rawList: List<JsonObject> = when (root) {
            is JsonArray -> root.mapNotNull { it as? JsonObject }
            is JsonObject -> {
                val nestedArray = root["texts"]?.takeIf { it is JsonArray }?.jsonArray
                    ?: root["reading_texts"]?.takeIf { it is JsonArray }?.jsonArray
                    ?: root["data"]?.takeIf { it is JsonArray }?.jsonArray
                nestedArray?.mapNotNull { it as? JsonObject } ?: listOf(root)
            }
            else -> emptyList()
        }

        return rawList.mapIndexed { index, obj ->
            parseSingleReadingTextObject(obj, index, defaultSource)
        }
    }

    private fun parseSingleReadingTextObject(
        obj: JsonObject,
        fallbackIndex: Int,
        defaultSource: String
    ): ReadingTextJson {
        val id = obj["id"]?.jsonPrimitive?.contentOrNull?.trim()
            ?: "r_file_${fallbackIndex + 1}"
        val level = (obj["level"]?.jsonPrimitive?.contentOrNull?.trim()?.uppercase())
            ?.ifBlank { "B1" } ?: "B1"
        val topic = (obj["topic"]?.jsonPrimitive?.contentOrNull?.trim())
            ?.ifBlank { "General Reading" } ?: "General Reading"
        val titleEn = obj["title_en"]?.jsonPrimitive?.contentOrNull?.trim()
            ?: obj["titleEn"]?.jsonPrimitive?.contentOrNull?.trim()
            ?: obj["title"]?.jsonPrimitive?.contentOrNull?.trim() ?: ""
        val titleAr = obj["title_ar"]?.jsonPrimitive?.contentOrNull?.trim()
            ?: obj["titleAr"]?.jsonPrimitive?.contentOrNull?.trim() ?: ""
        val source = obj["source"]?.jsonPrimitive?.contentOrNull?.trim()
            ?.ifBlank { defaultSource } ?: defaultSource
        val originalTextId = obj["original_text_id"]?.jsonPrimitive?.contentOrNull?.trim()
            ?: obj["originalTextId"]?.jsonPrimitive?.contentOrNull?.trim()

        val sentences = mutableListOf<ReadingSentenceItem>()
        val sentencesArray = obj["sentences"]?.takeIf { it is JsonArray }?.jsonArray
        if (sentencesArray != null) {
            for (elem in sentencesArray) {
                if (elem is JsonObject) {
                    val en = elem["en"]?.jsonPrimitive?.contentOrNull
                        ?: elem["english"]?.jsonPrimitive?.contentOrNull ?: ""
                    val ar = elem["ar"]?.jsonPrimitive?.contentOrNull
                        ?: elem["arabic"]?.jsonPrimitive?.contentOrNull ?: ""
                    val grammarId = elem["grammar_id"]?.jsonPrimitive?.contentOrNull
                        ?: elem["grammarId"]?.jsonPrimitive?.contentOrNull
                    if (en.isNotBlank() || ar.isNotBlank()) {
                        sentences.add(
                            ReadingSentenceItem(
                                en = en.trim(),
                                ar = ar.trim(),
                                grammarId = grammarId?.trim()?.ifBlank { null }
                            )
                        )
                    }
                }
            }
        }

        val questions = mutableListOf<ReadingQuestionItem>()
        val questionsArray = obj["questions"]?.takeIf { it is JsonArray }?.jsonArray
        if (questionsArray != null) {
            for (elem in questionsArray) {
                if (elem is JsonObject) {
                    val prompt = elem["prompt"]?.jsonPrimitive?.contentOrNull?.trim() ?: ""
                    val answerIndex = elem["answer_index"]?.jsonPrimitive?.intOrNull
                        ?: elem["answerIndex"]?.jsonPrimitive?.intOrNull ?: 0
                    val explanation = elem["explanation_ar"]?.jsonPrimitive?.contentOrNull?.trim()
                        ?: elem["explanationAr"]?.jsonPrimitive?.contentOrNull?.trim()
                        ?: elem["explanation"]?.jsonPrimitive?.contentOrNull?.trim() ?: ""

                    val options = mutableListOf<String>()
                    val optionsArray = elem["options"]?.takeIf { it is JsonArray }?.jsonArray
                    if (optionsArray != null) {
                        for (opt in optionsArray) {
                            opt.jsonPrimitive.contentOrNull?.let { options.add(it.trim()) }
                        }
                    }

                    if (prompt.isNotBlank() && options.isNotEmpty()) {
                        questions.add(
                            ReadingQuestionItem(
                                prompt = prompt,
                                options = options,
                                answerIndex = answerIndex.coerceIn(0, options.size - 1),
                                explanationAr = explanation
                            )
                        )
                    }
                }
            }
        }

        val targetWords = mutableListOf<String>()
        val targetArray = obj["target_words"]?.takeIf { it is JsonArray }?.jsonArray
            ?: obj["targetWords"]?.takeIf { it is JsonArray }?.jsonArray
        if (targetArray != null) {
            for (t in targetArray) {
                t.jsonPrimitive.contentOrNull?.let { targetWords.add(it.trim()) }
            }
        }

        val grammarIds = mutableListOf<String>()
        val gArray = obj["grammar_ids"]?.takeIf { it is JsonArray }?.jsonArray
            ?: obj["grammarIds"]?.takeIf { it is JsonArray }?.jsonArray
        if (gArray != null) {
            for (g in gArray) {
                g.jsonPrimitive.contentOrNull?.let { if (it.isNotBlank()) grammarIds.add(it.trim()) }
            }
        }

        return ReadingTextJson(
            id = id,
            level = level,
            topic = topic,
            titleEn = titleEn,
            titleAr = titleAr,
            sentences = sentences,
            questions = questions,
            source = source,
            targetWords = targetWords,
            grammarIds = grammarIds,
            originalTextId = originalTextId
        )
    }

    // --- CORRECTION NOTEBOOK & SPACED REVIEW ---
    val correctionEntriesFlow: Flow<List<CorrectionNotebookEntity>> = dao.observeAllCorrectionEntries()
    val uncheckedCorrectionCountFlow: Flow<Int> = dao.observeUncheckedCount()

    fun observeCorrectionEntriesForLesson(lessonId: String): Flow<List<CorrectionNotebookEntity>> =
        dao.observeCorrectionEntriesForLesson(lessonId)

    suspend fun saveCorrectionEntry(
        textId: String = "",
        grammarLessonId: String = "",
        grammarLessonTitle: String = "",
        originalSentence: String,
        correctedSentence: String = "",
        explanationAr: String = "",
        betterAlternative: String = "",
        status: String = "not_checked"
    ): Long = withContext(Dispatchers.IO) {
        val entry = CorrectionNotebookEntity(
            textId = textId,
            grammarLessonId = grammarLessonId,
            grammarLessonTitle = grammarLessonTitle,
            originalSentence = originalSentence.trim(),
            correctedSentence = correctedSentence.trim(),
            explanationAr = explanationAr.trim(),
            betterAlternative = betterAlternative.trim(),
            status = status,
            reviewStep = 0,
            nextReviewTimestamp = System.currentTimeMillis() + 3L * 24 * 60 * 60 * 1000, // 3 days
            situationPrompt = EVERYDAY_SITUATIONS.random(),
            attemptsCount = 0,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        dao.upsertCorrectionEntry(entry)
    }

    suspend fun updateCorrectionEntry(entry: CorrectionNotebookEntity): Long = withContext(Dispatchers.IO) {
        dao.upsertCorrectionEntry(entry.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteCorrectionEntry(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteCorrectionEntryById(id)
    }

    suspend fun recordSpacedReviewAttempt(
        id: Long,
        newSentence: String,
        isSuccess: Boolean = true,
        nowMillis: Long = System.currentTimeMillis()
    ): CorrectionNotebookEntity? = withContext(Dispatchers.IO) {
        val existing = dao.getCorrectionEntryById(id) ?: return@withContext null
        val nextStep = if (isSuccess) existing.reviewStep + 1 else maxOf(0, existing.reviewStep)
        val nextAttempts = existing.attemptsCount + 1

        val (nextIntervalDays, nextStatus) = when {
            isSuccess && nextStep >= 3 -> Pair(0L, "mastered")
            nextStep == 2 -> Pair(21L, "checked")
            nextStep == 1 -> Pair(7L, "checked")
            else -> Pair(3L, "checked")
        }

        val nextTimestamp = if (nextIntervalDays > 0) nowMillis + nextIntervalDays * 24 * 60 * 60 * 1000L else 0L
        val updated = existing.copy(
            originalSentence = if (newSentence.isNotBlank()) newSentence.trim() else existing.originalSentence,
            reviewStep = nextStep,
            attemptsCount = nextAttempts,
            status = nextStatus,
            nextReviewTimestamp = nextTimestamp,
            situationPrompt = if (nextStatus != "mastered") EVERYDAY_SITUATIONS.random() else existing.situationPrompt,
            updatedAt = nowMillis
        )
        val attempt = AttemptEntity(
            activityType = "correction_review",
            itemId = existing.id.toString(),
            timestamp = nowMillis,
            sm2Rating = null,
            isCorrect = isSuccess,
            score = if (isSuccess) 100f else 0f,
            deck = null
        )
        dao.upsertCorrectionWithAttempt(updated, attempt)
        updated
    }

    fun getRandomEverydaySituation(): String = EVERYDAY_SITUATIONS.random()

    private fun stripCodeFences(text: String): String {
        var s = text.trim()
        if (s.startsWith("```")) {
            val firstLineEnd = s.indexOf('\n')
            if (firstLineEnd != -1) {
                s = s.substring(firstLineEnd + 1)
            }
        }
        if (s.endsWith("```")) {
            s = s.substring(0, s.length - 3)
        }
        return s.trim()
    }

    private fun loadAssetJsonString(assetPath: String): String? {
        return try {
            context.assets.open(assetPath).use { stream ->
                InputStreamReader(stream, Charsets.UTF_8).use { it.readText() }
            }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        const val READING_DATA_VERSION = 3
        const val PREFS_NAME = "reading_repository_prefs"
        private const val KEY_DATA_VERSION = "reading_data_version"
        private const val KEY_ASSET_HASH = "reading_asset_hash"

        const val CONTEXT_READING_CHAT = "reading_text_chat"
        const val CONTEXT_READING_OPEN_QUESTION = "reading_open_question"

        val EVERYDAY_SITUATIONS = listOf(
            "You missed your flight because you left your passport at home.",
            "Your friend forgot an umbrella and got completely soaked in the heavy rain.",
            "You stayed up all night watching videos and failed an important exam the next morning.",
            "Someone ordered coffee but received tea by mistake at a busy café.",
            "You forgot to charge your phone before going on a long road trip.",
            "A tourist got lost because they didn't download an offline map.",
            "You didn't book concert tickets early and they were completely sold out.",
            "An athlete didn't stretch before exercising and pulled a muscle.",
            "You bought shoes that were one size too small because they looked stylish.",
            "A chef forgot to add salt to a special wedding dinner soup.",
            "You arrived late to a movie and missed the shocking opening scene.",
            "Someone didn't bring cash to a local farmers market that doesn't accept cards.",
            "You left your sunglasses on a park bench on a very sunny afternoon.",
            "A student procrastinated on a term paper until two hours before midnight.",
            "You decided not to bring a coat and the temperature dropped suddenly by evening.",
            "A driver ignored the low fuel warning and ran out of petrol on the highway.",
            "You forgot to reply to an important job offer email within 24 hours.",
            "Someone touched a hot baking tray without using oven mitts.",
            "You skipped breakfast and felt exhausted during a morning job interview.",
            "A photographer forgot an extra memory card on an African safari trip.",
            "You didn't set an alarm for a Saturday morning family hiking trip.",
            "Someone wore white clothes while cooking tomato pasta sauce.",
            "You didn't back up your computer files before a major system update.",
            "A traveler didn't check passport validity dates before arriving at airport check-in.",
            "You ate street food that was too spicy and couldn't sleep that night.",
            "Someone forgot to lock their bicycle outside a busy metro station.",
            "You didn't check the weather forecast before scheduling an outdoor garden picnic.",
            "A student studied the wrong chapter for a difficult biology quiz.",
            "You sent an informal message to your boss by accident instead of your best friend.",
            "Someone didn't practice their presentation and got nervous in front of the board."
        )
    }
}
