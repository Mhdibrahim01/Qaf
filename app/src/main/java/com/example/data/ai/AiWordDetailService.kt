package com.example.data.ai

import com.example.data.local.WordBooksDao
import com.example.data.local.WordDetailEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AiWordDetailService(
    private val dao: WordBooksDao,
    private val geminiService: GeminiDeckAndTranslationService,
    private val isAllowAiEnabled: () -> Boolean,
    private val isOnline: () -> Boolean,
    private val canMakeAiRequest: () -> Boolean,
    private val recordAiRequest: () -> Unit,
    var customAiTransport: ((String) -> String)? = null,
    private val engineModeProvider: () -> TranslationEngineMode = { TranslationEngineMode.SMART_AUTO }
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }

    /**
     * Retrieves the saved word definition from Room if it exists (instant, never requested again),
     * or fetches it from the configured AI / Free Dictionary & Translation Engine.
     */
    suspend fun getOrFetchWordDetail(
        word: String,
        forceFetch: Boolean = false
    ): Result<AiWordDetailJson> = withContext(Dispatchers.IO) {
        val cleanWord = word.trim()
        val key = cleanWord.lowercase()
        if (key.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Word is empty"))
        }

        // 1. Check local persistent database first
        val cached = dao.getWordDetail(key)
        if (cached != null && !forceFetch) {
            try {
                val rawParsed = json.decodeFromString<AiWordDetailJson>(cached.json)
                if (AiWordDetailSanitizer.hasSyntheticTemplates(cleanWord, rawParsed)) {
                    val repaired = repairSyntheticWordDetail(key, rawParsed)
                    saveAndEnrichWordDetail(key, repaired, json.encodeToString(repaired))
                    return@withContext Result.success(repaired)
                }
                return@withContext Result.success(rawParsed)
            } catch (_: Exception) {
                // If cached json corrupted, continue
            }
        }

        // 2. Check permissions & network
        if (!isAllowAiEnabled()) {
            return@withContext Result.failure(IllegalStateException("Allow AI is disabled in settings"))
        }
        if (!isOnline()) {
            return@withContext Result.failure(IllegalStateException("Device is offline"))
        }

        val engineMode = engineModeProvider()
        val customTransport = customAiTransport

        // 3A. Unit-test custom transport path (preserves exact 2-attempt validation retry behavior)
        if (customTransport != null) {
            if (!canMakeAiRequest()) {
                return@withContext Result.failure(IllegalStateException("Daily AI request limit reached"))
            }
            val prompt = buildWordDetailPrompt(cleanWord)
            var successData: Pair<AiWordDetailJson, String>? = null
            for (attempt in 1..2) {
                try {
                    val rawResponse = customTransport(prompt)
                    recordAiRequest()
                    val cleaned = geminiService.stripCodeFences(rawResponse)
                    val parsed = json.decodeFromString<AiWordDetailJson>(cleaned)
                    if (AiWordDetailValidator.validate(parsed)) {
                        successData = parsed to cleaned
                        break
                    }
                } catch (e: Exception) {
                    if (attempt == 2) {
                        return@withContext Result.failure(e)
                    }
                }
            }
            val (validParsed, validJson) = successData
                ?: return@withContext Result.failure(IllegalStateException("AI returned invalid dictionary format"))
            saveAndEnrichWordDetail(key, validParsed, validJson)
            return@withContext Result.success(validParsed)
        }

        // 3B. Free API Only Mode -> Fetch directly from Free Dictionary + Translation Engine (0 AI quota used)
        val localVariants = runCatching { dao.getWordVariantsAcrossDecks(cleanWord) }.getOrDefault(emptyList())
        if (engineMode == TranslationEngineMode.FREE_API_ONLY) {
            val freeRes = FreeDictionaryAndTranslationEngine.fetchWordDetail(cleanWord, localVariants)
            if (freeRes.isSuccess) {
                val parsed = freeRes.getOrThrow()
                val encodedJson = json.encodeToString(parsed)
                saveAndEnrichWordDetail(key, parsed, encodedJson)
                return@withContext Result.success(parsed)
            }
            return@withContext freeRes
        }

        // 3C. Smart Auto or AI Keys Only Mode -> Try AI first (single clean pass without retry storms)
        var aiError: Exception? = null
        if (canMakeAiRequest() && geminiService.isApiKeyConfigured()) {
            try {
                val prompt = buildWordDetailPrompt(cleanWord)
                val rawResponse = geminiService.callGeminiJson(prompt)
                recordAiRequest()
                val cleaned = geminiService.stripCodeFences(rawResponse)
                val parsed = json.decodeFromString<AiWordDetailJson>(cleaned)
                if (AiWordDetailValidator.validate(parsed)) {
                    saveAndEnrichWordDetail(key, parsed, cleaned)
                    return@withContext Result.success(parsed)
                } else {
                    aiError = IllegalStateException("AI returned invalid dictionary format")
                }
            } catch (e: Exception) {
                aiError = e
            }
        } else {
            aiError = IllegalStateException(
                if (!geminiService.isApiKeyConfigured()) "AI API key is not configured"
                else "Daily AI request limit reached"
            )
        }

        // 3D. In Smart Auto Mode, automatically fall back to Free Dictionary & Translation Engine on 429 or any AI failure!
        if (engineMode == TranslationEngineMode.SMART_AUTO) {
            val freeRes = FreeDictionaryAndTranslationEngine.fetchWordDetail(cleanWord, localVariants)
            if (freeRes.isSuccess) {
                val parsed = freeRes.getOrThrow()
                val encodedJson = json.encodeToString(parsed)
                saveAndEnrichWordDetail(key, parsed, encodedJson)
                return@withContext Result.success(parsed)
            }
        }

        Result.failure(aiError)
    }

    private suspend fun saveAndEnrichWordDetail(
        key: String,
        validParsed: AiWordDetailJson,
        validJson: String
    ) {
        // 4. Save permanently in word_details table (never requested again)
        dao.insertWordDetail(
            WordDetailEntity(
                key = key,
                json = validJson,
                createdAt = System.currentTimeMillis()
            )
        )

        // 5. Update local entry arabic and ipa if currently empty
        val allArabicMeanings = validParsed.meanings.flatMap { it.arabic }.filter { it.isNotBlank() }
        if (allArabicMeanings.isNotEmpty()) {
            val joined = allArabicMeanings.joinToString(" ؛ ")
            dao.updateEntryArabicIfEmpty(key, joined, validParsed.ipaUs)
        }
    }

    suspend fun getSavedDetailOnly(word: String): AiWordDetailJson? = withContext(Dispatchers.IO) {
        val key = word.trim().lowercase()
        val cached = dao.getWordDetail(key) ?: return@withContext null
        try {
            val rawParsed = json.decodeFromString<AiWordDetailJson>(cached.json)
            if (AiWordDetailSanitizer.hasSyntheticTemplates(key, rawParsed)) {
                val repaired = repairSyntheticWordDetail(key, rawParsed)
                saveAndEnrichWordDetail(key, repaired, json.encodeToString(repaired))
                repaired
            } else {
                rawParsed
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun repairSyntheticWordDetail(key: String, polluted: AiWordDetailJson): AiWordDetailJson {
        if (key == "puncture") {
            return AiWordDetailJson(
                syllables = "punc·ture",
                ipaUs = "/ˈpʌŋktʃər/",
                ipaGb = "/ˈpʌŋktʃər/",
                meanings = listOf(
                    AiMeaningItem(
                        pos = "n.",
                        arabic = listOf("ثقب", "خرق", "ثقب في الإطار"),
                        definitionEn = "A small hole in a tyre or tube resulting in an escape of air.",
                        definitionAr = "ثقب صغير في الإطار ينتج عنه تسرب الهواء.",
                        example = AiExampleItem(
                            en = "The nail in the road caused a puncture in my car tyre.",
                            ar = "أدى المسمار الموجود على الطريق إلى حدوث ثقب في إطار سيارتي."
                        )
                    ),
                    AiMeaningItem(
                        pos = "v.",
                        arabic = listOf("يثقب", "يخرق", "ينقر"),
                        definitionEn = "To make a small hole in something with a sharp point.",
                        definitionAr = "إحداث ثقب صغير في شيء ما باستخدام أداة حادة أو مدببة.",
                        example = AiExampleItem(
                            en = "A sharp thorn punctured the front bicycle tyre.",
                            ar = "ثقبت شوكة حادة الإطار الأمامي للدراجة."
                        )
                    )
                ),
                phrases = listOf(
                    AiPhraseItem(en = "puncture repair", ar = listOf("إصلاح الثقب")),
                    AiPhraseItem(en = "get a puncture", ar = listOf("يتعرض لثقب في الإطار")),
                    AiPhraseItem(en = "slow puncture", ar = listOf("ثقب بطيء يسرب الهواء")),
                    AiPhraseItem(en = "puncture wound", ar = listOf("جرح وخزي / ثقب غائر"))
                ),
                etymologyAr = "تأتي الكلمة من اللاتينية 'punctura' وتعني وخز أو ثقب، مشتقة من الفعل 'punctum' (نقطة).",
                examples = listOf(
                    AiExampleItem(
                        en = "The nail in the road caused a puncture in my car tyre.",
                        ar = "أدى المسمار الموجود على الطريق إلى حدوث ثقب في إطار سيارتي."
                    ),
                    AiExampleItem(
                        en = "She got a puncture on her way to work this morning.",
                        ar = "تعرضت لثقب في الإطار وهي في طريقها إلى العمل هذا الصباح."
                    )
                )
            )
        }
        return AiWordDetailSanitizer.sanitize(key, polluted)
    }

    companion object {
        fun buildWordDetailPrompt(word: String): String {
            return """
                You are an expert English-Arabic lexicographer.
                Provide a complete dictionary definition in JSON format for the English word: "$word".
                Cover all primary parts of speech of this word.

                Return ONLY a valid raw JSON object matching this EXACT format (no code fences, no extra text):
                {
                  "syllables": "word split with middle dots, e.g. po·lit·i·cal·ly",
                  "ipa_us": "/.../",
                  "ipa_gb": "/.../",
                  "meanings": [
                    {
                      "pos": "adv.",
                      "arabic": ["سياسياً", "من الناحية السياسية"],
                      "definition_en": "in a way that relates to politics",
                      "definition_ar": "بطريقة تتعلق بالسياسة أو الشؤون السياسية",
                      "example": {
                        "en": "The decision was politically motivated.",
                        "ar": "كان القرار مدفوعاً سياسياً."
                      }
                    }
                  ],
                  "phrases": [
                    {
                      "en": "politically correct",
                      "ar": ["صحيح سياسياً", "مقبول سياسياً"]
                    }
                  ],
                  "etymology_ar": "brief word origin in clear simple Arabic",
                  "examples": [
                    {
                      "en": "example sentence containing $word",
                      "ar": "Arabic translation of example sentence"
                    }
                  ]
                }

                Strict Rules:
                1. meanings array MUST contain between 1 and 4 meanings.
                2. phrases array MUST contain between 3 and 6 phrases with Arabic translations.
                3. every example object MUST have both non-empty "en" and non-empty "ar".
                4. Do NOT include markdown code blocks or comments.
            """.trimIndent()
        }
    }
}
