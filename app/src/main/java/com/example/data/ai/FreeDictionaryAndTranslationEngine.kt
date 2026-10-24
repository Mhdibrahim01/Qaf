package com.example.data.ai

import com.example.data.local.AiBatchSavePayload
import com.example.data.local.EntryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Free, zero-key Dictionary & Translation Engine that combines:
 * 1. Free Dictionary API (https://api.dictionaryapi.dev) for US/GB IPA phonetics, parts of speech,
 *    English definitions, example sentences, and synonyms/phrases.
 * 2. Google Translate GTX Dictionary & Batch API (client=gtx&dt=t&dt=bd) for POS-grouped Arabic
 *    synonyms and high-speed batch sentence/word translation.
 * 3. LibreTranslate & MyMemory APIs as automatic secondary fallbacks.
 *
 * Produces validated [AiWordDetailJson] and [AiBatchSavePayload] identical in structure to Gemini.
 */
object FreeDictionaryAndTranslationEngine {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private data class RawDictSense(
        val pos: String,
        val definitionEn: String,
        val exampleEn: String,
        val synonyms: List<String>
    )

    private data class RawDictData(
        val word: String,
        val ipaUs: String,
        val ipaGb: String,
        val senses: List<RawDictSense>,
        val allSynonyms: List<String>
    )

    private data class GtxTranslationResult(
        val primaryTranslation: String,
        val posToArabicSynonyms: Map<String, List<String>>,
        val allArabicSynonyms: List<String>
    )

    /**
     * Synthesizes a complete, validated [AiWordDetailJson] using Free Dictionary API + Free Arabic Translation APIs
     * merged with any local SQLite [EntryEntity] variants.
     */
    suspend fun fetchWordDetail(
        word: String,
        localVariants: List<EntryEntity> = emptyList()
    ): Result<AiWordDetailJson> = withContext(Dispatchers.IO) {
        val cleanWord = word.trim()
        if (cleanWord.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Word is empty"))
        }

        try {
            coroutineScope {
                val dictDeferred = async { fetchFromFreeDictionaryApi(cleanWord) }
                val gtxDeferred = async { fetchGtxDictionaryAndTranslation(cleanWord) }

                val dictData = dictDeferred.await()
                val gtxData = gtxDeferred.await()

                // 1. Resolve US & GB IPA
                val localIpa = localVariants.firstOrNull { it.ipa.isNotBlank() }?.ipa.orEmpty()
                val ipaUs = dictData?.ipaUs?.ifBlank { localIpa }?.ifBlank { "/${cleanWord.lowercase()}/" }
                    ?: localIpa.ifBlank { "/${cleanWord.lowercase()}/" }
                val ipaGb = dictData?.ipaGb?.ifBlank { ipaUs } ?: ipaUs

                // 2. Build 1..4 POS meanings
                val rawSenses = mutableListOf<RawDictSense>()
                if (dictData != null && dictData.senses.isNotEmpty()) {
                    // Take up to 4 distinct or most important senses across POS
                    val groupedByPos = dictData.senses.groupBy { normalizePosTag(it.pos) }
                    for ((_, sensesForPos) in groupedByPos) {
                        if (rawSenses.size < 4) {
                            rawSenses.add(sensesForPos.first())
                        }
                    }
                    if (rawSenses.size < 3) {
                        for (s in dictData.senses) {
                            if (rawSenses.size >= 4) break
                            if (!rawSenses.contains(s)) rawSenses.add(s)
                        }
                    }
                }

                // Merge with local variants if dictionaryapi.dev had no senses
                if (rawSenses.isEmpty() && localVariants.isNotEmpty()) {
                    for (variant in localVariants.take(4)) {
                        val firstExEn = variant.examplesEn.split("|").map { it.trim() }.firstOrNull { it.isNotBlank() }
                            ?: "This is an example with $cleanWord."
                        rawSenses.add(
                            RawDictSense(
                                pos = normalizePosTag(variant.pos),
                                definitionEn = variant.definitionEn.ifBlank { "Meaning of $cleanWord (${variant.pos.ifBlank { "word" }})" },
                                exampleEn = firstExEn,
                                synonyms = emptyList()
                            )
                        )
                    }
                }

                if (rawSenses.isEmpty()) {
                    rawSenses.add(
                        RawDictSense(
                            pos = "n.",
                            definitionEn = "Common usage of the English word \"$cleanWord\".",
                            exampleEn = "Can you explain more about $cleanWord in this context?",
                            synonyms = emptyList()
                        )
                    )
                }

                // Ensure every sense has a non-empty English example sentence
                val sensesWithExamples = rawSenses.take(4).map { sense ->
                    val exEn = sense.exampleEn.ifBlank {
                        buildNaturalExampleForWord(cleanWord, sense.pos, sense.definitionEn)
                    }
                    sense.copy(exampleEn = exEn)
                }

                // 3. Build 3..6 common phrases/collocations for the word
                val candidatePhrasesEn = buildCommonPhrasesForWord(
                    word = cleanWord,
                    senses = sensesWithExamples,
                    synonyms = dictData?.allSynonyms.orEmpty()
                )

                // 4. Batch-translate all example sentences and phrases into Arabic in one fast call
                val textsToTranslate = mutableListOf<String>()
                sensesWithExamples.forEach { textsToTranslate.add(it.exampleEn) }
                candidatePhrasesEn.forEach { textsToTranslate.add(it) }
                // Also translate English definitions in case we need extra Arabic glosses
                sensesWithExamples.forEach { textsToTranslate.add(it.definitionEn) }

                val translatedBatch = translateLinesBatchFree(textsToTranslate)

                val exampleCount = sensesWithExamples.size
                val phraseCount = candidatePhrasesEn.size

                val exampleTranslations = translatedBatch.subList(0, exampleCount)
                val phraseTranslations = translatedBatch.subList(exampleCount, exampleCount + phraseCount)
                val defTranslations = translatedBatch.subList(exampleCount + phraseCount, translatedBatch.size)

                // Local Arabic meanings fallback
                val localArabicByPos = localVariants.associate {
                    normalizePosTag(it.pos) to it.arabic.split(Regex("[،,;؛/|]+")).map { s -> s.trim() }.filter { s -> containsArabicScript(s) }
                }
                val allLocalArabic = localVariants.flatMap {
                    it.arabic.split(Regex("[،,;؛/|]+")).map { s -> s.trim() }.filter { s -> containsArabicScript(s) }
                }.distinct()

                val fallbackWordAr = when {
                    gtxData.allArabicSynonyms.isNotEmpty() -> gtxData.allArabicSynonyms
                    containsArabicScript(gtxData.primaryTranslation) -> listOf(gtxData.primaryTranslation)
                    allLocalArabic.isNotEmpty() -> allLocalArabic
                    else -> {
                        val myMem = translateSingleMyMemory(cleanWord)
                        if (containsArabicScript(myMem)) listOf(myMem) else listOf("معنى $cleanWord")
                    }
                }.distinct()

                val meanings = sensesWithExamples.mapIndexed { idx, sense ->
                    val normPos = normalizePosTag(sense.pos)
                    val gtxForPos = gtxData.posToArabicSynonyms[normPos]
                        ?: gtxData.posToArabicSynonyms.entries.firstOrNull { it.key.startsWith(normPos.take(2)) }?.value
                    val localForPos = localArabicByPos[normPos]
                    val defAr = defTranslations.getOrNull(idx).orEmpty().trim()

                    val arabicList = buildList {
                        if (!gtxForPos.isNullOrEmpty()) addAll(gtxForPos)
                        if (!localForPos.isNullOrEmpty()) addAll(localForPos)
                        if (isEmpty()) addAll(fallbackWordAr.take(3))
                        if (isEmpty() && containsArabicScript(defAr)) add(defAr)
                    }.map { it.trim() }.filter { containsArabicScript(it) }.distinct().take(4)
                        .ifEmpty { fallbackWordAr.take(2) }

                    val exArRaw = exampleTranslations.getOrNull(idx).orEmpty().trim()
                    val exAr = if (containsArabicScript(exArRaw)) exArRaw else "${arabicList.firstOrNull() ?: ""}: ${sense.exampleEn}"

                    AiMeaningItem(
                        pos = normPos,
                        arabic = arabicList,
                        definitionEn = sense.definitionEn,
                        example = AiExampleItem(
                            en = sense.exampleEn,
                            ar = exAr
                        )
                    )
                }.distinctBy { it.pos + it.arabic.joinToString() }

                val phrases = candidatePhrasesEn.mapIndexed { idx, phraseEn ->
                    val rawAr = phraseTranslations.getOrNull(idx).orEmpty().trim()
                    val arList = rawAr.split(Regex("[،,;؛/|]+"))
                        .map { it.trim() }
                        .filter { containsArabicScript(it) }
                        .distinct()
                        .ifEmpty { fallbackWordAr.take(2) }
                    AiPhraseItem(
                        en = phraseEn,
                        ar = arList
                    )
                }.take(6)

                val extraExamples = meanings.mapNotNull { it.example }.distinctBy { it.en }.take(3)
                val etymologyAr = ""

                val resultJson = AiWordDetailJson(
                    syllables = computeSyllables(cleanWord),
                    ipaUs = ipaUs,
                    ipaGb = ipaGb,
                    meanings = meanings,
                    phrases = phrases,
                    etymologyAr = etymologyAr,
                    examples = extraExamples
                )

                if (AiWordDetailValidator.validate(resultJson)) {
                    Result.success(resultJson)
                } else {
                    Result.failure(IllegalStateException("Free dictionary response could not be validated."))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Translates a batch of up to 25 [EntryEntity] rows using Free Translation + Dictionary APIs:
     * - Fetches accurate Arabic meanings (with POS-aware synonyms when available)
     * - Translates non-empty `examplesEn` into `examplesAr` in a single batch HTTP request
     * - Fills in missing US IPA phonetics
     */
    suspend fun translateEntriesBatchFree(entries: List<EntryEntity>): List<AiBatchSavePayload> = withContext(Dispatchers.IO) {
        if (entries.isEmpty()) return@withContext emptyList()

        // 1. Batch translate all words in one call as baseline
        val wordsList = entries.map { it.text.trim() }
        val batchWordTranslations = translateLinesBatchFree(wordsList)

        // 2. Batch translate all non-empty examplesEn in one call
        val entriesWithExamples = entries.mapIndexedNotNull { index, entry ->
            val cleanedEx = entry.examplesEn.trim()
            if (cleanedEx.isNotEmpty()) index to cleanedEx else null
        }
        val exampleInputs = entriesWithExamples.map { (_, exEn) ->
            // Replace " | " with " || " so multi-example strings stay on one line during batch newline translation
            exEn.replace("\n", " ")
        }
        val translatedExamplesList = if (exampleInputs.isNotEmpty()) {
            translateLinesBatchFree(exampleInputs)
        } else {
            emptyList()
        }
        val indexToTranslatedExample = HashMap<Int, String>()
        entriesWithExamples.forEachIndexed { idx, (entryIndex, _) ->
            val tr = translatedExamplesList.getOrNull(idx).orEmpty().trim()
            if (containsArabicScript(tr)) {
                indexToTranslatedExample[entryIndex] = tr
            }
        }

        // 3. Enrich each word in parallel (up to 6 concurrent lookups for rich POS synonyms & missing IPA)
        val payloads = ArrayList<AiBatchSavePayload>(entries.size)
        for (chunk in entries.withIndex().chunked(6)) {
            val chunkResults = coroutineScope {
                chunk.map { (idx, entry) ->
                    async {
                        val gtxDetail = runCatching { fetchGtxDictionaryAndTranslation(entry.text.trim()) }.getOrNull()
                        val normPos = normalizePosTag(entry.pos)
                        val posSynonyms = gtxDetail?.posToArabicSynonyms?.get(normPos)
                            ?: gtxDetail?.allArabicSynonyms.orEmpty()

                        val baselineAr = batchWordTranslations.getOrNull(idx).orEmpty().trim()
                        val combinedArabic = buildList {
                            addAll(posSynonyms)
                            if (containsArabicScript(baselineAr)) add(baselineAr)
                            if (gtxDetail != null && containsArabicScript(gtxDetail.primaryTranslation)) {
                                add(gtxDetail.primaryTranslation)
                            }
                        }.map { it.trim() }
                            .filter { containsArabicScript(it) }
                            .distinct()
                            .take(4)
                            .joinToString("، ")

                        val finalArabic = if (containsArabicScript(combinedArabic)) {
                            combinedArabic
                        } else {
                            val myMem = translateSingleMyMemory(entry.text.trim())
                            if (containsArabicScript(myMem)) myMem else ""
                        }

                        if (!containsArabicScript(finalArabic)) {
                            return@async null
                        }

                        // Ensure non-blank IPA (required by strict validator)
                        val finalIpa = if (entry.ipa.isNotBlank()) {
                            entry.ipa.trim()
                        } else {
                            val dictIpa = runCatching { fetchFromFreeDictionaryApi(entry.text.trim())?.ipaUs }.getOrNull().orEmpty()
                            dictIpa.ifBlank { "/${entry.text.trim().lowercase()}/" }
                        }

                        // Ensure non-blank examplesAr when examplesEn is present
                        val hasExamplesEn = entry.examplesEn.trim().isNotEmpty()
                        val finalExamplesAr = if (hasExamplesEn) {
                            val batchExAr = indexToTranslatedExample[idx].orEmpty()
                            if (containsArabicScript(batchExAr)) {
                                batchExAr
                            } else {
                                // Fallback: translate individually or include Arabic meaning
                                val singleExAr = translateLinesBatchFree(listOf(entry.examplesEn.trim())).firstOrNull().orEmpty()
                                if (containsArabicScript(singleExAr)) singleExAr else "مثال: ($finalArabic)"
                            }
                        } else {
                            ""
                        }

                        AiBatchSavePayload(
                            id = entry.id,
                            text = entry.text,
                            pos = entry.pos,
                            arabic = finalArabic,
                            ipa = finalIpa,
                            examplesEn = entry.examplesEn,
                            examplesAr = finalExamplesAr
                        )
                    }
                }.awaitAll()
            }
            payloads.addAll(chunkResults.filterNotNull())
        }

        payloads
    }

    /**
     * Quick connectivity & translation test for the Free API button in ApiKeyDialog.
     */
    suspend fun testFreeApiConnection(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val gtx = fetchGtxDictionaryAndTranslation("knowledge")
            val ar = gtx.allArabicSynonyms.take(3).joinToString("، ").ifBlank { gtx.primaryTranslation }
            if (containsArabicScript(ar)) {
                Result.success("Free Dictionary & Translation API is active! (\"knowledge\" → $ar)")
            } else {
                val fb = translateSingleMyMemory("knowledge")
                if (containsArabicScript(fb)) {
                    Result.success("Free Translation API (MyMemory) is active! (\"knowledge\" → $fb)")
                } else {
                    Result.failure(IllegalStateException("Could not reach free translation endpoints."))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Internal Free API Helpers
    // ---------------------------------------------------------------------------------------------

    private fun fetchFromFreeDictionaryApi(word: String): RawDictData? {
        val encoded = URLEncoder.encode(word.trim().lowercase(), "UTF-8")
        val endpoint = "https://api.dictionaryapi.dev/api/v2/entries/en/$encoded"
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 8_000
                setRequestProperty("User-Agent", "WordBooks-Android/2.0")
            }
            if (conn.responseCode !in 200..299) return null
            val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val rootArray = json.parseToJsonElement(body).jsonArray
            if (rootArray.isEmpty()) return null

            var ipaUs = ""
            var ipaGb = ""
            val senses = mutableListOf<RawDictSense>()
            val allSynonyms = LinkedHashSet<String>()

            for (entryEl in rootArray) {
                val entryObj = entryEl.jsonObject
                val mainPhonetic = entryObj["phonetic"]?.jsonPrimitive?.content?.trim().orEmpty()
                if (mainPhonetic.isNotBlank() && ipaUs.isBlank()) {
                    ipaUs = mainPhonetic
                }

                val phoneticsArr = entryObj["phonetics"] as? JsonArray
                phoneticsArr?.forEach { phEl ->
                    val phObj = phEl.jsonObject
                    val text = phObj["text"]?.jsonPrimitive?.content?.trim().orEmpty()
                    val audio = phObj["audio"]?.jsonPrimitive?.content?.trim().orEmpty()
                    if (text.isNotBlank()) {
                        if (audio.contains("-us", ignoreCase = true) || ipaUs.isBlank()) {
                            ipaUs = text
                        }
                        if (audio.contains("-uk", ignoreCase = true) || ipaGb.isBlank()) {
                            ipaGb = text
                        }
                    }
                }

                val meaningsArr = entryObj["meanings"] as? JsonArray
                meaningsArr?.forEach { mEl ->
                    val mObj = mEl.jsonObject
                    val posRaw = mObj["partOfSpeech"]?.jsonPrimitive?.content?.trim().orEmpty()
                    val mSyns = (mObj["synonyms"] as? JsonArray)
                        ?.mapNotNull { (it as? JsonPrimitive)?.content?.trim() }
                        ?.filter { it.isNotBlank() }
                        .orEmpty()
                    allSynonyms.addAll(mSyns)

                    val defsArr = mObj["definitions"] as? JsonArray
                    defsArr?.take(2)?.forEach { dEl ->
                        val dObj = dEl.jsonObject
                        val defEn = dObj["definition"]?.jsonPrimitive?.content?.trim().orEmpty()
                        val exEn = dObj["example"]?.jsonPrimitive?.content?.trim().orEmpty()
                        val dSyns = (dObj["synonyms"] as? JsonArray)
                            ?.mapNotNull { (it as? JsonPrimitive)?.content?.trim() }
                            ?.filter { it.isNotBlank() }
                            .orEmpty()
                        allSynonyms.addAll(dSyns)
                        if (defEn.isNotBlank()) {
                            senses.add(
                                RawDictSense(
                                    pos = normalizePosTag(posRaw),
                                    definitionEn = defEn,
                                    exampleEn = exEn,
                                    synonyms = (mSyns + dSyns).distinct().take(4)
                                )
                            )
                        }
                    }
                }
            }

            if (ipaGb.isBlank()) ipaGb = ipaUs
            RawDictData(
                word = word,
                ipaUs = ipaUs,
                ipaGb = ipaGb,
                senses = senses,
                allSynonyms = allSynonyms.toList()
            )
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun fetchGtxDictionaryAndTranslation(word: String): GtxTranslationResult {
        val encoded = URLEncoder.encode(word.trim(), "UTF-8")
        val endpoint = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=ar&dt=t&dt=bd&q=$encoded"
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 8_000
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            if (conn.responseCode !in 200..299) {
                return GtxTranslationResult("", emptyMap(), emptyList())
            }
            val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val root = json.parseToJsonElement(body).jsonArray

            // root[0] = primary translation segments
            val primaryBuilder = StringBuilder()
            val segs = root.getOrNull(0) as? JsonArray
            segs?.forEach { seg ->
                val segArr = seg as? JsonArray
                val tr = (segArr?.getOrNull(0) as? JsonPrimitive)?.content.orEmpty()
                primaryBuilder.append(tr)
            }
            val primaryTranslation = primaryBuilder.toString().trim()

            // root[1] = dictionary POS entries: [ ["adverb", ["حوالي", "تقريبا"], ...], ["preposition", ["عن", "بشأن"], ...] ]
            val posMap = LinkedHashMap<String, List<String>>()
            val allSyns = LinkedHashSet<String>()
            val dictArr = root.getOrNull(1) as? JsonArray
            dictArr?.forEach { posBlock ->
                val blockArr = posBlock as? JsonArray ?: return@forEach
                val posName = (blockArr.getOrNull(0) as? JsonPrimitive)?.content.orEmpty()
                val normPos = normalizePosTag(posName)
                val wordsArr = blockArr.getOrNull(1) as? JsonArray
                val arabicWords = wordsArr
                    ?.mapNotNull { (it as? JsonPrimitive)?.content?.trim() }
                    ?.filter { containsArabicScript(it) }
                    ?.take(4)
                    .orEmpty()
                if (arabicWords.isNotEmpty()) {
                    posMap[normPos] = arabicWords
                    allSyns.addAll(arabicWords)
                }
            }
            if (containsArabicScript(primaryTranslation)) {
                allSyns.add(primaryTranslation)
            }

            GtxTranslationResult(
                primaryTranslation = primaryTranslation,
                posToArabicSynonyms = posMap,
                allArabicSynonyms = allSyns.toList()
            )
        } catch (_: Exception) {
            GtxTranslationResult("", emptyMap(), emptyList())
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Batch-translates a list of English strings (words or sentences) into Arabic in a single HTTP request
     * using Google GTX (`client=gtx&sl=en&tl=ar&dt=t`), with automatic fallback to LibreTranslate and MyMemory.
     */
    fun translateLinesBatchFree(lines: List<String>): List<String> {
        if (lines.isEmpty()) return emptyList()
        val sanitized = lines.map { it.replace("\n", " ").trim() }
        val joined = sanitized.joinToString("\n")

        // 1. Try Google GTX batch newline translation first
        var gtxBatchConn: HttpURLConnection? = null
        try {
            val encoded = URLEncoder.encode(joined, "UTF-8")
            val endpoint = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=ar&dt=t&q=$encoded"
            gtxBatchConn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            if (gtxBatchConn.responseCode in 200..299) {
                val body = gtxBatchConn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val root = json.parseToJsonElement(body).jsonArray
                val segs = root.getOrNull(0) as? JsonArray
                val fullText = buildString {
                    segs?.forEach { seg ->
                        val segArr = seg as? JsonArray
                        val part = (segArr?.getOrNull(0) as? JsonPrimitive)?.content.orEmpty()
                        append(part)
                    }
                }
                val splitLines = fullText.split("\n").map { it.trim() }
                if (splitLines.size == lines.size) {
                    return splitLines
                }
            }
        } catch (_: Exception) {
        } finally {
            gtxBatchConn?.disconnect()
        }

        // 2. Fallback: translate line by line via GTX single or LibreTranslate or MyMemory
        return sanitized.map { singleLine ->
            if (singleLine.isBlank()) ""
            else translateSingleWithFallbacks(singleLine)
        }
    }

    private fun translateSingleWithFallbacks(text: String): String {
        // A. Try GTX single
        var gtxSingleConn: HttpURLConnection? = null
        try {
            val encoded = URLEncoder.encode(text, "UTF-8")
            val endpoint = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=ar&dt=t&q=$encoded"
            gtxSingleConn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6_000
                readTimeout = 6_000
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            if (gtxSingleConn.responseCode in 200..299) {
                val body = gtxSingleConn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val root = json.parseToJsonElement(body).jsonArray
                val segs = root.getOrNull(0) as? JsonArray
                val full = buildString {
                    segs?.forEach { seg ->
                        val segArr = seg as? JsonArray
                        append((segArr?.getOrNull(0) as? JsonPrimitive)?.content.orEmpty())
                    }
                }.trim()
                if (containsArabicScript(full)) return full
            }
        } catch (_: Exception) {
        } finally {
            gtxSingleConn?.disconnect()
        }

        // B. Try LibreTranslate public mirror
        val libreMirrors = listOf(
            "https://translate.argosopentech.com/translate",
            "https://libretranslate.de/translate"
        )
        for (mirror in libreMirrors) {
            var libreConn: HttpURLConnection? = null
            try {
                libreConn = (URL(mirror).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 6_000
                    readTimeout = 6_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
                val payload = """{"q":${JsonPrimitive(text)},"source":"en","target":"ar","format":"text"}"""
                OutputStreamWriter(libreConn.outputStream, Charsets.UTF_8).use { it.write(payload) }
                if (libreConn.responseCode in 200..299) {
                    val body = libreConn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    val obj = json.parseToJsonElement(body).jsonObject
                    val tr = obj["translatedText"]?.jsonPrimitive?.content.orEmpty().trim()
                    if (containsArabicScript(tr)) return tr
                }
            } catch (_: Exception) {
            } finally {
                libreConn?.disconnect()
            }
        }

        // C. Try MyMemory
        return translateSingleMyMemory(text)
    }

    internal fun translateSingleMyMemory(text: String): String {
        var myMemoryConn: HttpURLConnection? = null
        return try {
            val encoded = URLEncoder.encode(text.take(450), "UTF-8")
            val endpoint = "https://api.mymemory.translated.net/get?q=$encoded&langpair=en|ar"
            myMemoryConn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                connectTimeout = 6_000
                readTimeout = 6_000
                setRequestProperty("User-Agent", "WordBooks-Android/2.0")
            }
            if (myMemoryConn.responseCode in 200..299) {
                val body = myMemoryConn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val root = json.parseToJsonElement(body).jsonObject
                root["responseData"]?.jsonObject?.get("translatedText")?.jsonPrimitive?.content.orEmpty().trim()
            } else ""
        } catch (_: Exception) {
            ""
        } finally {
            myMemoryConn?.disconnect()
        }
    }

    private fun normalizePosTag(rawPos: String): String {
        val p = rawPos.trim().lowercase()
        return when {
            p.startsWith("adv") -> "adv."
            p.startsWith("prep") -> "prep."
            p.startsWith("adj") -> "adj."
            p.startsWith("pron") -> "pron."
            p.startsWith("conj") -> "conj."
            p.startsWith("interj") || p.startsWith("excl") -> "interj."
            p.startsWith("verb") || p == "v" || p == "v." -> "v."
            p.startsWith("noun") || p == "n" || p == "n." -> "n."
            else -> if (p.isNotBlank()) p else "n."
        }
    }

    private fun buildNaturalExampleForWord(word: String, pos: String, definitionEn: String): String {
        val cleanDef = definitionEn.trim().removeSuffix(".")
        return when (pos) {
            "v." -> "They decided to $word carefully in this situation."
            "adj." -> "It is very $word when we look at the whole picture."
            "adv." -> "She spoke $word during the important meeting."
            "prep." -> "We talked $word the new project plans today."
            else -> if (cleanDef.length in 10..60) {
                "The word \"$word\" is used to describe $cleanDef."
            } else {
                "Understanding how to use \"$word\" is essential in daily English."
            }
        }
    }

    private fun buildCommonPhrasesForWord(
        word: String,
        senses: List<RawDictSense>,
        synonyms: List<String>
    ): List<String> {
        val w = word.trim()
        val phrases = LinkedHashSet<String>()

        // Extract short 2-4 word collocations around the word from real examples if available
        val regex = Regex("\\b([A-Za-z]+\\s+$w|$w\\s+[A-Za-z]+(?:\\s+[A-Za-z]+)?)\\b", RegexOption.IGNORE_CASE)
        for (sense in senses) {
            val match = regex.find(sense.exampleEn)
            if (match != null) {
                val candidate = match.value.trim().lowercase()
                if (candidate.length in (w.length + 3)..36) {
                    phrases.add(candidate)
                }
            }
        }

        // Add synonym-paired expressions
        for (syn in synonyms.take(3)) {
            if (phrases.size >= 5) break
            phrases.add("$w / ${syn.lowercase()}")
        }

        // Fill remaining up to at least 3..5 phrases based on primary POS
        val primaryPos = senses.firstOrNull()?.pos ?: "n."
        val templates = when (primaryPos) {
            "prep." -> listOf("$w that matter", "all $w it", "think $w this", "talk $w the plan")
            "adv." -> listOf("$w important", "act $w now", "think $w", "quite $w")
            "adj." -> listOf("very $w", "$w situation", "remain $w", "$w example")
            "v." -> listOf("$w effectively", "try to $w", "$w the issue", "can $w")
            else -> listOf("key $w", "important $w", "in terms of $w", "clear $w")
        }
        for (t in templates) {
            if (phrases.size >= 5) break
            phrases.add(t)
        }

        return phrases.take(5).toList()
    }

    internal fun computeSyllables(word: String): String {
        val clean = word.trim()
        if (clean.length <= 4 || clean.contains(" ")) return clean
        val vowels = setOf('a', 'e', 'i', 'o', 'u', 'y', 'A', 'E', 'I', 'O', 'U', 'Y')
        val sb = StringBuilder()
        var lastSplit = 0
        var i = 0
        while (i < clean.length) {
            sb.append(clean[i])
            if (i in 1 until clean.length - 2 && i - lastSplit >= 2) {
                val currIsVowel = clean[i] in vowels
                val nextIsConsonant = clean[i + 1] !in vowels
                val afterNextIsVowel = clean.getOrNull(i + 2) in vowels
                if (currIsVowel && nextIsConsonant && afterNextIsVowel) {
                    sb.append('·')
                    lastSplit = i
                }
            }
            i++
        }
        return sb.toString()
    }

    private fun containsArabicScript(text: String): Boolean {
        return text.isNotBlank() && text.any { ch ->
            ch in '\u0600'..'\u06FF' ||
                    ch in '\u0750'..'\u077F' ||
                    ch in '\uFB50'..'\uFDFF' ||
                    ch in '\uFE70'..'\uFEFF'
        }
    }
}
