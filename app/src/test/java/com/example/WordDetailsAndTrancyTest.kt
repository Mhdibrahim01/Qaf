package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.AiExampleItem
import com.example.data.ai.AiMeaningItem
import com.example.data.ai.AiPhraseItem
import com.example.data.ai.AiWordDetailJson
import com.example.data.ai.AiWordDetailService
import com.example.data.ai.AiWordDetailValidator
import com.example.data.ai.GeminiDeckAndTranslationService
import com.example.data.importer.DataImportRepository
import com.example.data.local.EntryEntity
import com.example.data.local.WordBooksDatabase
import com.example.data.local.WordDetailEntity
import com.example.ui.screens.parseArabicMeaningGroups
import com.example.ui.screens.posFullName
import com.example.ui.screens.posShortLabel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WordDetailsAndTrancyTest {

    private val jsonHelper = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private fun sampleValidJson(word: String): String {
        val detail = AiWordDetailJson(
            syllables = "po·lit·i·cal·ly",
            ipaUs = "/pəˈlɪt̬.ə.kli/",
            ipaGb = "/pəˈlɪt.ɪ.kəl.i/",
            meanings = listOf(
                AiMeaningItem(
                    pos = "adv.",
                    arabic = listOf("سياسياً", "من الناحية السياسية"),
                    definitionEn = "in a way that relates to politics or politicians",
                    example = AiExampleItem(
                        en = "The decision was politically motivated.",
                        ar = "كان القرار ذو دوافع سياسية."
                    )
                )
            ),
            phrases = listOf(
                AiPhraseItem(
                    en = "politically correct",
                    ar = listOf("ملتزم باللباقة السياسية")
                ),
                AiPhraseItem(
                    en = "politically active",
                    ar = listOf("نشط سياسياً")
                ),
                AiPhraseItem(
                    en = "politically charged",
                    ar = listOf("مشحون سياسياً")
                )
            ),
            etymologyAr = "مشتقة من political مع اللاحقة -ly",
            examples = listOf(
                AiExampleItem(
                    en = "She is politically astute.",
                    ar = "هي ذكية من الناحية السياسية."
                )
            )
        )
        return jsonHelper.encodeToString(detail)
    }

    @Test
    fun verifyOnlineFetchSavesToWordDetailsAndOfflineLoadsFromCacheWithoutNetwork() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, WordBooksDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.wordBooksDao()
        val geminiService = GeminiDeckAndTranslationService(dao)

        var isOnline = true
        var networkCallsMade = 0

        val service = AiWordDetailService(
            dao = dao,
            geminiService = geminiService,
            isAllowAiEnabled = { true },
            isOnline = { isOnline },
            canMakeAiRequest = { true },
            recordAiRequest = {},
            customAiTransport = { prompt ->
                networkCallsMade++
                "```json\n${sampleValidJson("politically")}\n```"
            }
        )

        // 1. Initial online fetch for "politically"
        val firstResult = service.getOrFetchWordDetail("politically")
        assertTrue(firstResult.isSuccess)
        val detail1 = firstResult.getOrThrow()
        assertEquals("po·lit·i·cal·ly", detail1.syllables)
        assertEquals("/pəˈlɪt̬.ə.kli/", detail1.ipaUs)
        assertEquals(1, networkCallsMade)

        // Verify it was saved to the Room word_details table
        val savedEntity = dao.getWordDetail("politically")
        assertNotNull(savedEntity)
        assertEquals("politically", savedEntity!!.key)
        assertTrue(savedEntity.json.contains("po·lit·i·cal·ly"))

        // 2. Switch to OFFLINE mode (airplane mode)
        isOnline = false

        // Fetching "politically" again MUST return immediately from local Room cache with 0 network calls!
        val cachedResult = service.getOrFetchWordDetail("politically")
        assertTrue(cachedResult.isSuccess)
        val cachedDetail = cachedResult.getOrThrow()
        assertEquals("po·lit·i·cal·ly", cachedDetail.syllables)
        assertEquals(1, networkCallsMade) // Still 1! No new network call was made

        // 3. Requesting an uncached word while offline MUST fail gracefully with offline message
        val uncachedOfflineResult = service.getOrFetchWordDetail("democracy")
        assertTrue(uncachedOfflineResult.isFailure)
        assertTrue(uncachedOfflineResult.exceptionOrNull()?.message?.contains("offline", ignoreCase = true) == true)
        assertEquals(1, networkCallsMade) // Still 1!

        db.close()
    }

    @Test
    fun verifyInvalidGeminiJsonIsRejectedAndRetriedOnce() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, WordBooksDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.wordBooksDao()
        val geminiService = GeminiDeckAndTranslationService(dao)

        var callCount = 0
        val service = AiWordDetailService(
            dao = dao,
            geminiService = geminiService,
            isAllowAiEnabled = { true },
            isOnline = { true },
            canMakeAiRequest = { true },
            recordAiRequest = {},
            customAiTransport = {
                callCount++
                if (callCount == 1) {
                    // Attempt 1: Invalid schema (missing phrases, invalid JSON)
                    "This is an invalid Gemini response with no JSON"
                } else {
                    // Attempt 2 (Retry once): Valid strict JSON
                    "```json\n${sampleValidJson("retryword")}\n```"
                }
            }
        )

        val result = service.getOrFetchWordDetail("retryword")
        assertTrue(result.isSuccess)
        assertEquals(2, callCount) // Verified it retried once

        // Verify valid retry data is now cached in Room
        val saved = dao.getWordDetail("retryword")
        assertNotNull(saved)
        assertTrue(saved!!.json.contains("po·lit·i·cal·ly"))

        db.close()
    }

    @Test
    fun verifyStrictValidatorRejectionRules() {
        // Valid
        val valid = AiWordDetailJson(
            syllables = "test",
            meanings = listOf(
                AiMeaningItem(pos = "n.", arabic = listOf("اختبار"), definitionEn = "a test")
            ),
            phrases = listOf(
                AiPhraseItem(en = "p1", ar = listOf("ف1")),
                AiPhraseItem(en = "p2", ar = listOf("ف2")),
                AiPhraseItem(en = "p3", ar = listOf("ف3"))
            )
        )
        assertTrue(AiWordDetailValidator.validate(valid))

        // Less than 3 phrases -> invalid
        val tooFewPhrases = valid.copy(phrases = valid.phrases.take(2))
        assertFalse(AiWordDetailValidator.validate(tooFewPhrases))

        // Empty meanings -> invalid
        val emptyMeanings = valid.copy(meanings = emptyList())
        assertFalse(AiWordDetailValidator.validate(emptyMeanings))

        // Meaning with empty arabic -> invalid
        val emptyArabicMeaning = valid.copy(
            meanings = listOf(
                AiMeaningItem(pos = "n.", arabic = emptyList(), definitionEn = "def")
            )
        )
        assertFalse(AiWordDetailValidator.validate(emptyArabicMeaning))

        // Meaning with blank pos -> invalid
        val blankPosMeaning = valid.copy(
            meanings = listOf(
                AiMeaningItem(pos = "", arabic = listOf("معنى"), definitionEn = "def")
            )
        )
        assertFalse(AiWordDetailValidator.validate(blankPosMeaning))
    }

    @Test
    fun verifyParseArabicMeaningGroups() {
        // Single meaning
        val single = parseArabicMeaningGroups("adv.", "سياسياً")
        assertEquals(1, single.size)
        assertEquals("adv.", single[0].first)
        assertEquals("سياسياً", single[0].second)

        // Multiple meanings grouped with primary pos and web
        val multi = parseArabicMeaningGroups("adv.", "سياسيا ؛ سياسياً ؛ بسياسة")
        assertEquals(2, multi.size)
        assertEquals("adv.", multi[0].first)
        assertEquals("سياسيا ؛ سياسياً", multi[0].second)
        assertEquals("web.", multi[1].first)
        assertEquals("بسياسة", multi[1].second)

        // Empty Arabic
        val empty = parseArabicMeaningGroups("n.", "")
        assertTrue(empty.isEmpty())
    }

    @Test
    fun verifyPosFullName() {
        assertEquals("Adverb", posFullName("adv."))
        assertEquals("Noun", posFullName("n."))
        assertEquals("Verb", posFullName("v."))
        assertEquals("Adjective", posFullName("adj."))
        assertEquals("Preposition", posFullName("prep."))
    }

    @Test
    fun verifyPosShortLabel() {
        assertEquals("adv.", posShortLabel("adverb"))
        assertEquals("adv.", posShortLabel("adv."))
        assertEquals("adv.", posShortLabel("Adverb"))
        assertEquals("n.", posShortLabel("noun"))
        assertEquals("n.", posShortLabel("Noun"))
        assertEquals("v.", posShortLabel("verb"))
        assertEquals("v.", posShortLabel("phrasal verb"))
        assertEquals("adj.", posShortLabel("adjective"))
        assertEquals("art.", posShortLabel("indefinite article"))
        assertEquals("art.", posShortLabel("article"))
        assertEquals("web.", posShortLabel("web"))
    }

    @Test
    fun verifyUpdateEntryArabicIfEmptyWhenAiDefinitionFetched() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, WordBooksDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.wordBooksDao()
        val geminiService = GeminiDeckAndTranslationService(dao)

        // Seed an entry with empty arabic and ipa
        val entry = EntryEntity(
            id = "e1",
            type = "word",
            deck = "oxford5000",
            text = "politically",
            pos = "adverb",
            level = "B2",
            definitionEn = "in a political way",
            noteEn = "",
            examplesEn = "She is politically active.",
            examplesAr = "",
            definitionUrl = "",
            audioUrl = "",
            ipa = "",
            arabic = "",
            arabicSource = "",
            dataVersion = 1
        )
        dao.insertEntryIgnore(entry)

        // Verify entry has empty arabic initially
        val before = dao.getWordVariants("oxford5000", "politically")
        assertEquals(1, before.size)
        assertEquals("", before[0].arabic)
        assertEquals("", before[0].ipa)

        val service = AiWordDetailService(
            dao = dao,
            geminiService = geminiService,
            isAllowAiEnabled = { true },
            isOnline = { true },
            canMakeAiRequest = { true },
            recordAiRequest = {},
            customAiTransport = {
                "```json\n${sampleValidJson("politically")}\n```"
            }
        )

        // Fetch AI definition - this must automatically populate empty arabic & ipa in entries
        val res = service.getOrFetchWordDetail("politically")
        assertTrue(res.isSuccess)

        val after = dao.getWordVariants("oxford5000", "politically")
        assertEquals(1, after.size)
        assertEquals("سياسياً ؛ من الناحية السياسية", after[0].arabic)
        assertEquals("ai", after[0].arabicSource)
        assertEquals("/pəˈlɪt̬.ə.kli/", after[0].ipa)

        // Verify user edited arabic is never overwritten by updateEntryArabicIfEmpty
        dao.updateEntryUserArabicById("e1", "معنى مخصص")
        val userEdited = dao.getWordVariants("oxford5000", "politically")
        assertEquals("معنى مخصص", userEdited[0].arabic)
        assertEquals("user", userEdited[0].arabicSource)

        dao.updateEntryArabicIfEmpty("politically", "معنى جديد من الذكاء", "/new_ipa/")
        val afterAttemptedOverwrite = dao.getWordVariants("oxford5000", "politically")
        assertEquals("معنى مخصص", afterAttemptedOverwrite[0].arabic)
        assertEquals("user", afterAttemptedOverwrite[0].arabicSource)

        db.close()
    }

    @Test
    fun verifyWordDetailsExportAndImportWithoutOverwriting() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, WordBooksDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.wordBooksDao()
        val repo = DataImportRepository(context, db)

        // Insert initial detail
        dao.insertWordDetail(
            WordDetailEntity(
                key = "hello",
                json = """{"syllables":"hel·lo","ipa_us":"/həˈloʊ/","ipa_gb":"/həˈləʊ/"}""",
                createdAt = 1000L
            )
        )

        // Export JSON
        val exported = repo.exportWordDetailsJson()
        assertTrue(exported.contains("hel·lo"))

        // Re-importing must preserve existing details without overwriting
        val itemsToImport = listOf(
            WordDetailEntity(key = "hello", json = """{"syllables":"MODIFIED"}""", createdAt = 2000L),
            WordDetailEntity(key = "world", json = """{"syllables":"world"}""", createdAt = 3000L)
        )
        val newImportJson = jsonHelper.encodeToString(itemsToImport)

        val importedCount = repo.importWordDetailsJson(newImportJson)
        assertEquals(1, importedCount) // Only "world" was inserted, "hello" was preserved!

        val helloEntity = dao.getWordDetail("hello")
        assertNotNull(helloEntity)
        assertEquals(1000L, helloEntity!!.createdAt)
        assertTrue(helloEntity.json.contains("hel·lo")) // Not overwritten!

        val worldEntity = dao.getWordDetail("world")
        assertNotNull(worldEntity)
        assertEquals("world", worldEntity!!.key)

        db.close()
    }

    @Test
    fun verifyCleanArabicGlossAndWordProfilePolishes() {
        // 1. Verify CJK character stripping and semicolon normalization on "tradition"
        assertEquals(
            "بالتقليد • حسب العادة",
            com.example.ui.screens.cleanArabicGloss("بالتقليد ؛ 按照 العادة")
        )
        assertEquals(
            "تقليد • عرف • عادة متوارثة",
            com.example.ui.screens.cleanArabicGloss("تقليد ؛ عرف ؛ عادة متوارثة")
        )

        // 2. Verify + and ~ placeholder expansion for mind-map nodes
        assertEquals(
            "break with tradition",
            com.example.ui.components.LexicalMindMapExtractor.expandRawNodeLabel("tradition", "break with +")
        )
        assertEquals(
            "in tradition with",
            com.example.ui.components.LexicalMindMapExtractor.expandRawNodeLabel("tradition", "in ~ with")
        )
        assertEquals(
            "young entrepreneur",
            com.example.ui.components.LexicalMindMapExtractor.expandRawNodeLabel("entrepreneur", "young +")
        )

        // 3. Verify 1-node adaptive placement (node at y=22%, headword at y=72%)
        val singleNodeOffsets = com.example.ui.components.LexicalMindMapExtractor.computeRadialNodeOffsets(
            nodeCount = 1,
            widthPx = 400f,
            heightPx = 190f
        )
        val singleHeadwordOffset = com.example.ui.components.LexicalMindMapExtractor.computeHeadwordCenterOffset(
            nodeCount = 1,
            widthPx = 400f,
            heightPx = 190f
        )
        assertEquals(1, singleNodeOffsets.size)
        assertEquals(190f * 0.22f, singleNodeOffsets[0].y, 0.5f)
        assertEquals(190f * 0.72f, singleHeadwordOffset.y, 0.5f)

        // 4. Verify BiDi etymology formatting on entrepreneur and tradition (including attached و'prendre')
        val entrepreneurEtym = com.example.ui.components.LexicalMindMapExtractor.formatBidiEtymologyAnnotated(
            "كلمة فرنسية مأخوذة من الفعل 'entreprendre' (يتعهد بـ أو يقوم بـ)، وتتكون من 'entre' (بين) و'prendre' (يأخذ)."
        ).text
        assertTrue(entrepreneurEtym.contains("\u2066\u200Eentreprendre\u200E\u2069"))
        assertTrue(entrepreneurEtym.contains("\u2066\u200Eentre\u200E\u2069"))
        assertTrue(entrepreneurEtym.contains("و \u2066\u200Eprendre\u200E\u2069"))

        // 5. Verify 2-line balanced pill wrapping and horizontal clamping for long headwords
        assertEquals(
            "successful\nentrepreneur",
            com.example.ui.components.LexicalMindMapExtractor.formatBalancedTwoLineLabel(
                "successful entrepreneur",
                maxSingleLineChars = 14
            )
        )
        assertEquals(
            "aspiring\nentrepreneur",
            com.example.ui.components.LexicalMindMapExtractor.formatBalancedTwoLineLabel(
                "aspiring entrepreneur",
                maxSingleLineChars = 14
            )
        )
        assertEquals(
            "young entrepreneur",
            com.example.ui.components.LexicalMindMapExtractor.formatBalancedTwoLineLabel(
                "young entrepreneur",
                maxSingleLineChars = 24
            )
        )
        val clampedRight = com.example.ui.components.LexicalMindMapExtractor.clampHorizontalNodeOffset(
            rawX = 310f,
            canvasWidthPx = 330f,
            pillWidthPx = 110f,
            edgeMarginPx = 8f
        )
        assertEquals(330f - 55f - 8f, clampedRight, 0.01f)

        // 6. Verify false-prefix "entrenched" is excluded for "entrepreneur" and curated related nodes are present
        val entrepreneurNodes = com.example.ui.components.LexicalMindMapExtractor.extractNodes(
            headword = "entrepreneur",
            aiDetail = AiWordDetailJson(
                phrases = listOf(
                    AiPhraseItem(en = "young +", ar = listOf("رائد أعمال شاب")),
                    AiPhraseItem(en = "successful +", ar = listOf("رائد أعمال ناجح")),
                    AiPhraseItem(en = "entrepreneurial spirit", ar = listOf("روح المبادرة")),
                    AiPhraseItem(en = "aspiring +", ar = listOf("رائد أعمال طموح"))
                )
            ),
            posVariants = emptyList()
        )
        assertFalse(entrepreneurNodes.any { it.pillLabelEn.equals("entrenched", ignoreCase = true) })
        assertTrue(entrepreneurNodes.any { it.pillLabelEn.equals("entrepreneurial", ignoreCase = true) })
        assertTrue(entrepreneurNodes.any { it.pillLabelEn.equals("founder", ignoreCase = true) })
    }
}

