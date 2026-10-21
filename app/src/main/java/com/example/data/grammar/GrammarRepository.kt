package com.example.data.grammar

import android.content.Context
import android.net.Uri
import com.example.data.local.AiCacheEntity
import com.example.data.local.AiHistoryEntity
import com.example.data.local.GrammarExampleItem
import com.example.data.local.GrammarExerciseItem
import com.example.data.local.GrammarLessonEntity
import com.example.data.local.GrammarLessonJson
import com.example.data.local.GrammarProgressEntity
import com.example.data.local.WordBooksDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.InputStreamReader

class GrammarRepository(
    private val context: Context,
    private val dao: WordBooksDao
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    val lessonsFlow: Flow<List<GrammarLessonEntity>> = dao.observeAllGrammarLessons()
    val progressFlow: Flow<List<GrammarProgressEntity>> = dao.observeAllGrammarProgress()

    /**
     * Reads `grammar_lessons.json` from assets and imports/upserts lessons by id.
     * Never touches user progress in grammar_progress.
     * Automatically imports if force=true, or if new lessons exist in the assets file
     * that aren't yet in the database.
     */
    suspend fun importGrammarLessonsIfNeeded(force: Boolean = false): Result<Int> = withContext(Dispatchers.IO) {
        val jsonString = loadAssetJsonString("grammar_lessons.json")
            ?: loadAssetJsonString("data_import/grammar_lessons.json")
            ?: return@withContext Result.failure(IllegalStateException("No grammar_lessons.json found in assets"))

        val parsed = parseGrammarLessonsFlexible(jsonString, defaultSource = "file")
        val currentLessonCount = dao.getGrammarLessonCount()
        val currentVersion = prefs.getInt(KEY_DATA_VERSION, 0)
        val assetHash = jsonString.hashCode()
        val savedHash = prefs.getInt(KEY_ASSET_HASH, 0)

        // If not forced and version is up to date, hash matches, and all lessons exist, skip
        if (!force && currentVersion >= GRAMMAR_DATA_VERSION && currentLessonCount >= parsed.size && assetHash == savedHash) {
            return@withContext Result.success(currentLessonCount)
        }

        val count = upsertParsedLessons(parsed, defaultSource = "file")
        prefs.edit()
            .putInt(KEY_DATA_VERSION, GRAMMAR_DATA_VERSION)
            .putInt(KEY_ASSET_HASH, assetHash)
            .apply()
        Result.success(count)
    }

    /**
     * Force re-syncs all grammar lessons from assets/grammar_lessons.json.
     * Preserves user progress and any AI-generated exercises.
     */
    suspend fun importGrammarLessonsFromAssets(force: Boolean = true): Result<Int> = withContext(Dispatchers.IO) {
        importGrammarLessonsIfNeeded(force = force)
    }

    /**
     * Imports grammar lessons from a JSON string provided by the user (file or pasted text).
     * Accepts:
     * - An array of lessons: `[ { "id": "...", ... } ]`
     * - An object with lessons: `{ "lessons": [ ... ] }` or `{ "grammar_lessons": [ ... ] }`
     * - A single lesson object: `{ "id": "...", ... }`
     */
    suspend fun importGrammarLessonsFromJsonString(
        rawJson: String,
        source: String = "file"
    ): Result<Int> = withContext(Dispatchers.IO) {
        val clean = stripCodeFences(rawJson)
        if (clean.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Empty JSON content"))
        }

        val lessons = try {
            parseGrammarLessonsFlexible(clean, defaultSource = source)
        } catch (e: Exception) {
            return@withContext Result.failure(IllegalArgumentException("Failed to parse JSON: ${e.message}", e))
        }

        if (lessons.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("No valid grammar lessons found in JSON"))
        }

        val count = upsertParsedLessons(lessons, defaultSource = source)
        Result.success(count)
    }

    /**
     * Reads and imports grammar lessons from user-selected Uris (e.g. from File Picker).
     */
    suspend fun importGrammarLessonsFromUris(uris: List<Uri>): Result<Int> = withContext(Dispatchers.IO) {
        var totalImported = 0
        val errors = mutableListOf<String>()

        for (uri in uris) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                    InputStreamReader(stream, Charsets.UTF_8).use { it.readText() }
                }
                if (!content.isNullOrBlank()) {
                    val res = importGrammarLessonsFromJsonString(content, source = "file")
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

    private suspend fun upsertParsedLessons(
        lessons: List<GrammarLessonJson>,
        defaultSource: String
    ): Int {
        var count = 0
        for ((index, parsed) in lessons.withIndex()) {
            val id = parsed.id.trim().ifBlank {
                "g_${System.currentTimeMillis().toString().takeLast(6)}_${index + 1}"
            }
            val examplesJson = json.encodeToString(parsed.examples)
            val exercisesJson = json.encodeToString(parsed.exercises)
            val existing = dao.getGrammarLessonById(id)

            if (existing == null) {
                dao.upsertGrammarLesson(
                    GrammarLessonEntity(
                        id = id,
                        level = parsed.level.trim().uppercase().ifBlank { "A2" },
                        topic = parsed.topic.trim().ifBlank { "General Grammar" },
                        titleEn = parsed.titleEn.trim(),
                        titleAr = parsed.titleAr.trim(),
                        explanationAr = parsed.explanationAr.trim(),
                        examplesJson = examplesJson,
                        exercisesJson = exercisesJson,
                        source = parsed.source.ifBlank { defaultSource },
                        dataVersion = GRAMMAR_DATA_VERSION
                    )
                )
            } else {
                // Upsert content fields by id without touching user progress
                // Merge any AI-generated exercises added via "More practice" so they are preserved
                val mergedExercisesJson = mergeExercises(existing.exercisesJson, parsed.exercises)
                dao.updateGrammarLessonContentById(
                    id = id,
                    level = parsed.level.trim().uppercase().ifBlank { existing.level },
                    topic = parsed.topic.trim().ifBlank { existing.topic },
                    titleEn = parsed.titleEn.trim().ifBlank { existing.titleEn },
                    titleAr = parsed.titleAr.trim().ifBlank { existing.titleAr },
                    explanationAr = parsed.explanationAr.trim().ifBlank { existing.explanationAr },
                    examplesJson = examplesJson,
                    exercisesJson = mergedExercisesJson,
                    dataVersion = GRAMMAR_DATA_VERSION
                )
            }
            count++
        }
        return count
    }

    /**
     * Flexible parser that handles:
     * 1. Direct JSON array: [ {...}, {...} ]
     * 2. JSON object with array: { "lessons": [ ... ] } or { "grammar_lessons": [ ... ] }
     * 3. Single lesson object: { "id": "...", ... }
     * 4. Supports camelCase (titleEn, titleAr, explanationAr) and snake_case (title_en, title_ar, explanation_ar).
     */
    fun parseGrammarLessonsFlexible(
        jsonString: String,
        defaultSource: String = "file"
    ): List<GrammarLessonJson> {
        val root = try {
            json.parseToJsonElement(jsonString)
        } catch (_: Exception) {
            // Fallback direct decode
            return json.decodeFromString<List<GrammarLessonJson>>(jsonString)
        }

        val rawList: List<JsonObject> = when (root) {
            is JsonArray -> root.mapNotNull { it as? JsonObject }
            is JsonObject -> {
                val nestedArray = root["lessons"]?.takeIf { it is JsonArray }?.jsonArray
                    ?: root["grammar_lessons"]?.takeIf { it is JsonArray }?.jsonArray
                    ?: root["data"]?.takeIf { it is JsonArray }?.jsonArray
                nestedArray?.mapNotNull { it as? JsonObject } ?: listOf(root)
            }
            else -> emptyList()
        }

        return rawList.mapIndexed { index, obj ->
            parseSingleLessonObject(obj, index, defaultSource)
        }
    }

    private fun parseSingleLessonObject(
        obj: JsonObject,
        fallbackIndex: Int,
        defaultSource: String
    ): GrammarLessonJson {
        val id = obj["id"]?.jsonPrimitive?.contentOrNull?.trim()
            ?: "g_file_${fallbackIndex + 1}"
        val level = (obj["level"]?.jsonPrimitive?.contentOrNull?.trim()?.uppercase())
            ?.ifBlank { "A2" } ?: "A2"
        val topic = (obj["topic"]?.jsonPrimitive?.contentOrNull?.trim())
            ?.ifBlank { "General Grammar" } ?: "General Grammar"
        val titleEn = obj["title_en"]?.jsonPrimitive?.contentOrNull?.trim()
            ?: obj["titleEn"]?.jsonPrimitive?.contentOrNull?.trim()
            ?: obj["title"]?.jsonPrimitive?.contentOrNull?.trim() ?: ""
        val titleAr = obj["title_ar"]?.jsonPrimitive?.contentOrNull?.trim()
            ?: obj["titleAr"]?.jsonPrimitive?.contentOrNull?.trim() ?: ""
        val explanationAr = obj["explanation_ar"]?.jsonPrimitive?.contentOrNull?.trim()
            ?: obj["explanationAr"]?.jsonPrimitive?.contentOrNull?.trim()
            ?: obj["explanation"]?.jsonPrimitive?.contentOrNull?.trim() ?: ""
        val source = obj["source"]?.jsonPrimitive?.contentOrNull?.trim()
            ?.ifBlank { defaultSource } ?: defaultSource

        // Examples
        val examples = mutableListOf<GrammarExampleItem>()
        val examplesArray = obj["examples"]?.takeIf { it is JsonArray }?.jsonArray
        if (examplesArray != null) {
            for (exElement in examplesArray) {
                if (exElement is JsonObject) {
                    val en = exElement["en"]?.jsonPrimitive?.contentOrNull
                        ?: exElement["english"]?.jsonPrimitive?.contentOrNull ?: ""
                    val ar = exElement["ar"]?.jsonPrimitive?.contentOrNull
                        ?: exElement["arabic"]?.jsonPrimitive?.contentOrNull ?: ""
                    if (en.isNotBlank() || ar.isNotBlank()) {
                        examples.add(GrammarExampleItem(en = en.trim(), ar = ar.trim()))
                    }
                }
            }
        }

        // Exercises
        val exercises = mutableListOf<GrammarExerciseItem>()
        val exercisesArray = obj["exercises"]?.takeIf { it is JsonArray }?.jsonArray
        if (exercisesArray != null) {
            for (exElement in exercisesArray) {
                if (exElement is JsonObject) {
                    val type = exElement["type"]?.jsonPrimitive?.contentOrNull?.trim() ?: "mcq"
                    val prompt = exElement["prompt"]?.jsonPrimitive?.contentOrNull?.trim() ?: ""
                    val answer = exElement["answer"]?.jsonPrimitive?.contentOrNull?.trim() ?: ""
                    val explanation = exElement["explanation_ar"]?.jsonPrimitive?.contentOrNull?.trim()
                        ?: exElement["explanationAr"]?.jsonPrimitive?.contentOrNull?.trim()
                        ?: exElement["explanation"]?.jsonPrimitive?.contentOrNull?.trim() ?: ""
                    val exSource = exElement["source"]?.jsonPrimitive?.contentOrNull?.trim() ?: source

                    val options = mutableListOf<String>()
                    val optionsArray = exElement["options"]?.takeIf { it is JsonArray }?.jsonArray
                    if (optionsArray != null) {
                        for (opt in optionsArray) {
                            opt.jsonPrimitive.contentOrNull?.let { options.add(it) }
                        }
                    }

                    if (prompt.isNotBlank() && answer.isNotBlank()) {
                        exercises.add(
                            GrammarExerciseItem(
                                type = type,
                                prompt = prompt,
                                options = options,
                                answer = answer,
                                explanationAr = explanation,
                                source = exSource
                            )
                        )
                    }
                }
            }
        }

        return GrammarLessonJson(
            id = id,
            level = level,
            topic = topic,
            titleEn = titleEn,
            titleAr = titleAr,
            explanationAr = explanationAr,
            examples = examples,
            exercises = exercises,
            source = source
        )
    }

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

    private fun mergeExercises(
        existingJson: String,
        fileExercises: List<GrammarExerciseItem>
    ): String {
        return try {
            val existingList = json.decodeFromString<List<GrammarExerciseItem>>(existingJson)
            val aiExercises = existingList.filter { it.source == "ai" }
            if (aiExercises.isEmpty()) {
                json.encodeToString(fileExercises)
            } else {
                val combined = fileExercises + aiExercises
                json.encodeToString(combined)
            }
        } catch (_: Exception) {
            json.encodeToString(fileExercises)
        }
    }

    suspend fun getLessonById(id: String): GrammarLessonEntity? = withContext(Dispatchers.IO) {
        dao.getGrammarLessonById(id)
    }

    suspend fun getLessonProgress(lessonId: String): GrammarProgressEntity? = withContext(Dispatchers.IO) {
        dao.getGrammarProgress(lessonId)
    }

    suspend fun saveLessonProgress(
        lessonId: String,
        completedExercises: Int,
        totalExercises: Int,
        score: Int,
        isCompleted: Boolean
    ) = withContext(Dispatchers.IO) {
        val existing = dao.getGrammarProgress(lessonId)
        val bestScore = maxOf(existing?.bestScore ?: 0, score)
        dao.upsertGrammarProgress(
            GrammarProgressEntity(
                lessonId = lessonId,
                completedExercises = completedExercises,
                totalExercises = totalExercises,
                bestScore = bestScore,
                lastScore = score,
                isCompleted = isCompleted || (existing?.isCompleted == true),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun appendExercisesToLesson(
        lessonId: String,
        newExercises: List<GrammarExerciseItem>
    ): Result<Int> = withContext(Dispatchers.IO) {
        val lesson = dao.getGrammarLessonById(lessonId)
            ?: return@withContext Result.failure(IllegalArgumentException("Lesson $lessonId not found"))

        val currentList = try {
            json.decodeFromString<List<GrammarExerciseItem>>(lesson.exercisesJson).toMutableList()
        } catch (_: Exception) {
            mutableListOf()
        }

        currentList.addAll(newExercises)
        val updatedJson = json.encodeToString(currentList)
        dao.updateGrammarLessonExercisesJson(lessonId, updatedJson)
        Result.success(currentList.size)
    }

    suspend fun saveAiCreatedLesson(lesson: GrammarLessonJson): Result<String> = withContext(Dispatchers.IO) {
        val nextId = if (lesson.id.isNotBlank() && dao.getGrammarLessonById(lesson.id) == null) {
            lesson.id.trim()
        } else {
            "g_ai_${System.currentTimeMillis().toString().takeLast(6)}"
        }

        val entity = GrammarLessonEntity(
            id = nextId,
            level = lesson.level.trim().uppercase().ifBlank { "B1" },
            topic = lesson.topic.trim().ifBlank { "General Grammar" },
            titleEn = lesson.titleEn.trim(),
            titleAr = lesson.titleAr.trim(),
            explanationAr = lesson.explanationAr.trim(),
            examplesJson = json.encodeToString(lesson.examples),
            exercisesJson = json.encodeToString(lesson.exercises),
            source = "ai",
            dataVersion = GRAMMAR_DATA_VERSION
        )
        dao.upsertGrammarLesson(entity)
        Result.success(nextId)
    }

    fun observeLessonChat(lessonId: String): Flow<List<AiHistoryEntity>> =
        dao.observeAiHistory(CONTEXT_CHAT, lessonId)

    suspend fun addChatMessage(lessonId: String, role: String, text: String): Long = withContext(Dispatchers.IO) {
        dao.insertAiHistory(
            AiHistoryEntity(
                contextType = CONTEXT_CHAT,
                contextId = lessonId,
                role = role,
                text = text,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    fun observeSentenceCheckHistory(lessonId: String): Flow<List<AiHistoryEntity>> =
        dao.observeAiHistory(CONTEXT_SENTENCE_CHECK, lessonId)

    suspend fun addSentenceCheckHistory(lessonId: String, role: String, text: String): Long = withContext(Dispatchers.IO) {
        dao.insertAiHistory(
            AiHistoryEntity(
                contextType = CONTEXT_SENTENCE_CHECK,
                contextId = lessonId,
                role = role,
                text = text,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun getCachedMistakeExplanation(
        lessonId: String,
        prompt: String,
        wrongAnswer: String
    ): String? = withContext(Dispatchers.IO) {
        val cacheKey = buildMistakeCacheKey(lessonId, prompt, wrongAnswer)
        dao.getAiCache(cacheKey)?.detailedExplanationAr?.ifBlank { null }?.let {
            com.example.data.ai.AiPromptContract.sanitizeAiPlainText(it)
        }
    }

    suspend fun saveMistakeExplanationToCache(
        lessonId: String,
        prompt: String,
        wrongAnswer: String,
        explanationAr: String
    ) = withContext(Dispatchers.IO) {
        val cacheKey = buildMistakeCacheKey(lessonId, prompt, wrongAnswer)
        val cleanExplanation = com.example.data.ai.AiPromptContract.sanitizeAiPlainText(explanationAr)
        dao.upsertAiCache(
            AiCacheEntity(
                cacheKey = cacheKey,
                word = prompt.take(50),
                ipa = "",
                syllables = "",
                arabicSummary = "Mistake: $wrongAnswer",
                extraExamplesJson = "",
                detailedExplanationAr = cleanExplanation,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    private fun buildMistakeCacheKey(lessonId: String, prompt: String, wrongAnswer: String): String {
        val cleanPrompt = prompt.trim().lowercase().replace(Regex("[^a-z0-9]"), "_").take(40)
        val cleanAnswer = wrongAnswer.trim().lowercase().replace(Regex("[^a-z0-9]"), "_").take(30)
        return com.example.data.ai.AiPromptContract.versionedCacheKey(
            namespace = "mistake",
            rawKey = "${lessonId}:$cleanPrompt:$cleanAnswer"
        )
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
        const val GRAMMAR_DATA_VERSION = 3
        const val PREFS_NAME = "grammar_repository_prefs"
        private const val KEY_DATA_VERSION = "grammar_data_version"
        private const val KEY_ASSET_HASH = "grammar_asset_hash"

        const val CONTEXT_CHAT = "grammar_lesson_chat"
        const val CONTEXT_SENTENCE_CHECK = "grammar_sentence_check"
    }
}
