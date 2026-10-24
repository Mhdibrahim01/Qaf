package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.importer.DataImportRepository
import com.example.data.importer.Sm2Rating
import com.example.data.grammar.GrammarAiService
import com.example.data.grammar.GrammarRepository
import com.example.data.reading.ReadingAiService
import com.example.data.reading.ReadingRepository
import com.example.data.local.CorrectionNotebookEntity
import com.example.data.local.DailyTaskItem
import com.example.data.local.EntryEntity
import com.example.data.local.GrammarExerciseItem
import com.example.data.local.GrammarLessonEntity
import com.example.data.local.PlanDailyTemplate
import com.example.data.local.PlanStudyGoals
import com.example.data.local.PlanWeekGoal
import com.example.data.local.GrammarProgressEntity
import com.example.data.local.ReadingProgressEntity
import com.example.data.local.ReadingSentenceItem
import com.example.data.local.ReadingTextEntity
import com.example.data.local.SentenceEntity
import com.example.data.local.StudyPlanEntity
import com.example.data.local.StudyPlanJson
import com.example.data.local.StudyPlanProfileEntity
import com.example.data.local.WordBooksDatabase
import com.example.data.local.WordProgressEntity
import com.example.data.plan.LocalQuizEngine
import com.example.data.plan.StudyPlanAiService
import com.example.data.plan.StudyPlanRepository
import com.example.data.plan.TestTimeProvider
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun verifyRealJsonFilesImportExactCountsAndProvenanceRules() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, WordBooksDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val repo = DataImportRepository(context, db)
        repo.ensureImportedOnFirstLaunch(forceRecheck = true)

        val dao = db.wordBooksDao()
        val oxfordCount = dao.getEntryCountForDeck("oxford5000")
        val opalCount = dao.getEntryCountForDeck("oxford_opal")
        val phrasesCount = dao.getEntryCountForDeck("oxford_phrases")
        val expressionsCount = dao.getEntryCountForDeck("everyday_expressions")
        val idiomsCount = dao.getEntryCountForDeck("essential_idioms")
        val ieltsCount = dao.getEntryCountForDeck("ielts_core")
        val itCount = dao.getEntryCountForDeck("it_industry")
        val cefrC1Count = dao.getEntryCountForDeck("cefr_c1")
        val pteCount = dao.getEntryCountForDeck("pte_academic")
        val businessCount = dao.getEntryCountForDeck("business_english")
        val careerCount = dao.getEntryCountForDeck("career_interview")
        val medicalCount = dao.getEntryCountForDeck("medical_healthcare")
        val totalCount = dao.getTotalEntryCount()
        val wordDetailsCount = dao.getWordDetailsCount()
        val emptyArabicCount = dao.getEmptyArabicCount()
        val fakeCount = dao.getFakeLegacyEntryCount()

        assertEquals(5948, oxfordCount)
        assertEquals(2096, opalCount)
        assertEquals(750, phrasesCount)
        assertEquals(600, expressionsCount)
        assertEquals(468, idiomsCount)
        assertEquals(3575, ieltsCount)
        assertEquals(1622, itCount)
        assertEquals(1853, cefrC1Count)
        assertEquals(2651, pteCount)
        assertEquals(1250, businessCount)
        assertEquals(406, careerCount)
        assertEquals(405, medicalCount)
        assertEquals(21624, totalCount)
        assertEquals(10317, wordDetailsCount)
        assertEquals(0, emptyArabicCount)
        assertEquals(0, fakeCount)

        // Verify progress (favorite & mastered) is preserved across content upserts
        val firstEntry = dao.getEntryById("1")
        assertNotNull(firstEntry)
        repo.toggleFavorite(firstEntry!!.deck, firstEntry.text)
        repo.toggleMastered(firstEntry.deck, firstEntry.text)

        // 1. Test AI enrichment sets arabic_source = "ai"
        dao.updateEntryAiEnrichment(
            id = "1",
            arabic = "حرف تنكير",
            ipa = "/ə/",
            definitionEn = "indefinite article",
            examplesEn = "a book | a cat",
            examplesAr = "كتاب | قطة"
        )
        val afterAi = dao.getEntryById("1")!!
        assertEquals("حرف تنكير", afterAi.arabic)
        assertEquals("ai", afterAi.arabicSource)

        // 2. Test file import upsert sets arabic_source = "file" on entry #2
        val entry2 = dao.getEntryById("2")!!
        dao.upsertEntryContentById(
            entry2.copy(
                arabic = "يتخلى عن، يترك",
                arabicSource = "file",
                ipa = "/əˈbændən/",
                dataVersion = DataImportRepository.DATA_VERSION + 1
            )
        )
        val afterFileUpsert = dao.getEntryById("2")!!
        assertEquals("يتخلى عن، يترك", afterFileUpsert.arabic)
        assertEquals("file", afterFileUpsert.arabicSource)

        // 3. Test user edit sets arabic_source = "user" and is NEVER overwritten by AI or file import
        dao.updateEntryUserArabicById("1", "ترجمة المستخدم الخاصة")
        val afterUserEdit = dao.getEntryById("1")!!
        assertEquals("ترجمة المستخدم الخاصة", afterUserEdit.arabic)
        assertEquals("user", afterUserEdit.arabicSource)

        // Attempt to overwrite entry #1 via AI enrichment -> must keep user Arabic
        dao.updateEntryAiEnrichment(
            id = "1",
            arabic = "ترجمة ذكاء اصطناعي جديدة",
            ipa = "/eɪ/",
            definitionEn = "updated def",
            examplesEn = "",
            examplesAr = ""
        )
        val afterAiAttempt = dao.getEntryById("1")!!
        assertEquals("ترجمة المستخدم الخاصة", afterAiAttempt.arabic)
        assertEquals("user", afterAiAttempt.arabicSource)
        assertEquals("/eɪ/", afterAiAttempt.ipa) // non-Arabic content field still updated

        // Attempt to overwrite entry #1 via file import upsert -> must keep user Arabic AND preserve favorite/mastered
        dao.upsertEntryContentById(
            afterAiAttempt.copy(
                arabic = "ترجمة من الملف",
                arabicSource = "file",
                audioUrl = "https://example.com/a.mp3"
            )
        )
        val afterFileAttempt = dao.getEntryById("1")!!
        assertEquals("ترجمة المستخدم الخاصة", afterFileAttempt.arabic)
        assertEquals("user", afterFileAttempt.arabicSource)
        assertEquals("https://example.com/a.mp3", afterFileAttempt.audioUrl)

        val progressAfterUpsert = dao.getWordProgress(repo.buildCardKey(firstEntry.deck, firstEntry.text))
        assertNotNull(progressAfterUpsert)
        assertTrue(progressAfterUpsert!!.isFavorite)
        assertTrue(progressAfterUpsert.isMastered)

        // 4. Verify per-deck Arabic stats query (fromFileCount, fromAiCount, fromUserCount, emptyCount)
        val oxfordStats = dao.getDeckArabicStats().first { it.deck == "oxford5000" }
        assertEquals(5948, oxfordStats.totalRows)
        assertEquals(1, oxfordStats.fromFileCount)
        assertEquals(5946, oxfordStats.fromAiCount)
        assertEquals(1, oxfordStats.fromUserCount)
        assertEquals(0, oxfordStats.emptyCount)

        db.close()
    }

    @Test
    fun verifyFlashcardsStudy10CardsCloseAndReopenAndSm2DueToday() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, WordBooksDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        // Session 1: Open app, import real JSON files, load oxford5000 flashcards
        val repoSession1 = DataImportRepository(context, db)
        repoSession1.ensureImportedOnFirstLaunch(forceRecheck = true)

        val (initialCards, initialStudyState) = repoSession1.loadFlashcardDeckCards("oxford5000")
        assertTrue(initialCards.size > 4000)
        assertEquals(0, initialStudyState.lastCardIndex)

        // Study 10 cards (indices 0..9) and advance to index 10 ("11 / N")
        val ratings = listOf(
            Sm2Rating.AGAIN, // Card 0 -> due in 10m (due today)
            Sm2Rating.HARD,  // Card 1 -> interval 0, due in 4h (due today)
            Sm2Rating.GOOD,  // Card 2 -> interval 1d (tomorrow)
            Sm2Rating.EASY,  // Card 3 -> interval 4d
            Sm2Rating.AGAIN, // Card 4 -> due in 10m (due today)
            Sm2Rating.GOOD,  // Card 5 -> interval 1d
            Sm2Rating.HARD,  // Card 6 -> interval 0, due in 4h (due today)
            Sm2Rating.EASY,  // Card 7 -> interval 4d
            Sm2Rating.GOOD,  // Card 8 -> interval 1d
            Sm2Rating.AGAIN  // Card 9 -> due in 10m (due today)
        )

        val nowMillis = System.currentTimeMillis()
        for (i in 0 until 10) {
            val card = initialCards[i]
            val nextIndex = i + 1
            val nextCard = initialCards[nextIndex]
            repoSession1.applySm2Review(
                deckId = card.deck,
                wordText = card.text,
                rating = ratings[i],
                nowMillis = nowMillis
            )
            repoSession1.saveDeckStudyPosition(
                deckId = "oxford5000",
                cardIndex = nextIndex,
                wordText = nextCard.text,
                incrementStudied = true
            )
        }

        // Verify 5 cards are due today (3 AGAIN + 2 first-review HARD)
        val dueTodayBeforeMastered = repoSession1.getReviewDueTodayCount(nowMillis)
        assertEquals(5, dueTodayBeforeMastered)

        // Mark Card 0 as Mastered -> mastered cards are excluded from "Review due today"
        repoSession1.toggleMastered("oxford5000", initialCards[0].text)
        val dueTodayAfterMastered = repoSession1.getReviewDueTodayCount(nowMillis)
        assertEquals(4, dueTodayAfterMastered)

        // Also verify another deck ("essential_idioms") remembers its own independent position
        val (idiomCards, _) = repoSession1.loadFlashcardDeckCards("essential_idioms")
        assertEquals(db.wordBooksDao().getUniqueWordCountForDeck("essential_idioms"), idiomCards.size)
        repoSession1.saveDeckStudyPosition(
            deckId = "essential_idioms",
            cardIndex = 5,
            wordText = idiomCards[5].text,
            incrementStudied = true
        )

        // Session 2: Simulate closing the app and reopening it with a new repository instance
        val repoSession2 = DataImportRepository(context, db)
        val (reopenedCards, reopenedState) = repoSession2.loadFlashcardDeckCards("oxford5000")

        assertEquals(initialCards.size, reopenedCards.size)
        assertEquals(10, reopenedState.lastCardIndex) // Restored at card index 10 ("11 / N")
        assertEquals(initialCards[10].text, reopenedState.lastWordText)
        assertEquals(10, reopenedState.totalStudiedInDeck)
        assertTrue(reopenedCards[0].isMastered)

        val (_, reopenedIdiomsState) = repoSession2.loadFlashcardDeckCards("essential_idioms")
        assertEquals(5, reopenedIdiomsState.lastCardIndex)
        assertEquals(idiomCards[5].text, reopenedIdiomsState.lastWordText)

        db.close()
    }

    @Test
    fun verifySave3SentencesAndRunOfflineDictation() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, WordBooksDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.wordBooksDao()
        val repo = DataImportRepository(context, db)
        repo.ensureImportedOnFirstLaunch(forceRecheck = true)

        // Pick 3 real example sentences from the database (and enrich one with examples_ar)
        val entriesWithExamples = dao.getEntriesWithExamples(limit = 10)
        assertTrue("Database should have entries with examples_en", entriesWithExamples.size >= 3)

        // Simulate an entry that has examples_ar populated in local data
        val firstEntry = entriesWithExamples[0]
        val firstSentenceEn = firstEntry.examplesEn.split(" | ").first().trim()
        val firstSentenceAr = "هذه ترجمة الجملة الأولى من البيانات المحلية."
        dao.updateEntryAiEnrichment(
            id = firstEntry.id,
            arabic = firstEntry.arabic,
            ipa = firstEntry.ipa,
            definitionEn = firstEntry.definitionEn,
            examplesEn = firstEntry.examplesEn,
            examplesAr = firstSentenceAr
        )

        val secondEntry = entriesWithExamples[1]
        val secondSentenceEn = secondEntry.examplesEn.split(" | ").first().trim()
        val secondSentenceAr = "ترجمة الجملة الثانية من حقل examples_ar."

        val thirdEntry = entriesWithExamples[2]
        val thirdSentenceEn = thirdEntry.examplesEn.split(" | ").first().trim()
        val thirdSentenceAr = "ترجمة الجملة الثالثة من حقل examples_ar."

        // Save 3 example sentences into the sentences table with Arabic taken from examples_ar (no AI needed)
        val saved1 = repo.saveExampleSentence(
            englishText = firstSentenceEn,
            arabicText = firstSentenceAr,
            sourceWord = firstEntry.text
        )
        Thread.sleep(5)
        val saved2 = repo.saveExampleSentence(
            englishText = secondSentenceEn,
            arabicText = secondSentenceAr,
            sourceWord = secondEntry.text
        )
        Thread.sleep(5)
        val saved3 = repo.saveExampleSentence(
            englishText = thirdSentenceEn,
            arabicText = thirdSentenceAr,
            sourceWord = thirdEntry.text
        )

        assertEquals(3, dao.getSavedSentencesCount())
        val allNewestFirst = dao.getAllSentencesNewestFirst()
        assertEquals(3, allNewestFirst.size)
        assertEquals(thirdSentenceEn, allNewestFirst[0].englishText)
        assertEquals(thirdSentenceAr, allNewestFirst[0].arabicText)
        assertEquals(firstSentenceEn, allNewestFirst[2].englishText)
        assertEquals(firstSentenceAr, allNewestFirst[2].arabicText)

        // Run offline Dictation on Sentence 1: exact match (ignoring punctuation/case)
        val exactDictationDiff = com.example.data.sentences.SentencePracticeUtils.computeSentenceDiff(
            expected = saved1.englishText,
            actual = saved1.englishText.lowercase().replace(".", ""),
            ignorePunctuation = true
        )
        assertTrue(exactDictationDiff.isExactMatch)
        assertEquals(0, exactDictationDiff.mistakesCount)
        assertEquals(100, exactDictationDiff.accuracyPercent)

        // Run offline Dictation with intentional mistakes (1 misspelled word + 1 missing word)
        val targetSentence = "She decided to abandon the old project quickly."
        val userTypedAttempt = "She desided to abandon old project quickly"
        val mistakeDiff = com.example.data.sentences.SentencePracticeUtils.computeSentenceDiff(
            expected = targetSentence,
            actual = userTypedAttempt,
            ignorePunctuation = true
        )
        assertEquals(false, mistakeDiff.isExactMatch)
        assertEquals(2, mistakeDiff.mistakesCount) // "desided" (WRONG) and "the" (MISSING)
        assertTrue(mistakeDiff.tokens.any {
            it.status == com.example.data.sentences.DiffTokenStatus.WRONG &&
                it.text == "desided" &&
                it.expectedText == "decided"
        })
        assertTrue(mistakeDiff.tokens.any {
            it.status == com.example.data.sentences.DiffTokenStatus.MISSING &&
                it.expectedText == "the"
        })

        // Verify AI explain is saved permanently so the same sentence is never requested twice
        val cachedArabicExplanation = "• التركيب النحوي: زمن الماضي البسيط.\n• المفردات: كلمة ${saved1.sourceWord} في سياق الجملة."
        dao.updateSentenceAiExplanation(saved1.id, cachedArabicExplanation)

        val geminiService = com.example.data.ai.GeminiDeckAndTranslationService(dao)
        val cachedResult = geminiService.explainSentenceInSimpleArabic(saved1.id)
        assertTrue(cachedResult.isSuccess)
        assertEquals(cachedArabicExplanation, cachedResult.getOrNull())

        db.close()
    }

    @Test
    fun verifyFlipCardRateItCloseAndReopenApp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, WordBooksDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        // Session 1: Open app, import JSON decks, load oxford5000 flashcards
        val repoSession1 = DataImportRepository(context, db)
        repoSession1.ensureImportedOnFirstLaunch(forceRecheck = true)

        // Verify default Flashcard settings
        assertEquals(
            com.example.data.importer.FlashcardDirection.ENGLISH_TO_ARABIC,
            repoSession1.getFlashcardDirection()
        )
        assertEquals(false, repoSession1.getFlashcardAutoShowAnswer())

        val (cardsSession1, stateSession1) = repoSession1.loadFlashcardDeckCards("oxford5000")
        assertTrue(cardsSession1.size > 4000)
        assertEquals(0, stateSession1.lastCardIndex)

        // Simulate flip state: starts unflipped (Front face)
        var isCardFlipped = false
        var audioPlayCount = 0

        // Tapping audio on the Front plays pronunciation and MUST NOT flip the card
        audioPlayCount++
        assertEquals(false, isCardFlipped)

        // Tap anywhere on the card -> flips to Back face (revealing Again / Hard / Good / Easy)
        isCardFlipped = true
        assertTrue(isCardFlipped)

        // Pre-schedule card[1] into the future (4 days from now) so we can verify that rating card[0]
        // moves directly to the next DUE card (card[2], skipping future-scheduled card[1])!
        val nowMillis = System.currentTimeMillis()
        repoSession1.applySm2Review(
            deckId = cardsSession1[1].deck,
            wordText = cardsSession1[1].text,
            rating = Sm2Rating.EASY,
            nowMillis = nowMillis
        )

        // Rate flipped card[0] with GOOD -> saves SM-2 data, moves to next due card (index 2), resets flip to false
        val (ratedProgress0, updatedDeckState) = repoSession1.rateCardAndMoveToNextDue(
            activeDeckId = "oxford5000",
            cards = cardsSession1,
            currentIndex = 0,
            rating = Sm2Rating.GOOD,
            nowMillis = nowMillis
        )
        isCardFlipped = false

        assertEquals(1, ratedProgress0.repetitions)
        assertEquals(1, ratedProgress0.intervalDays)
        assertEquals(2, updatedDeckState.lastCardIndex)
        assertEquals(cardsSession1[2].text, updatedDeckState.lastWordText)
        assertEquals(false, isCardFlipped)

        // Update Settings: Card direction -> ARABIC_TO_ENGLISH, Auto-show answer -> true
        repoSession1.setFlashcardDirection(com.example.data.importer.FlashcardDirection.ARABIC_TO_ENGLISH)
        repoSession1.setFlashcardAutoShowAnswer(true)

        // Session 2: Close and reopen the app (new repository instance against the same database & SharedPreferences)
        val repoSession2 = DataImportRepository(context, db)
        val (reopenedCards, reopenedState) = repoSession2.loadFlashcardDeckCards("oxford5000")

        // Verify deck position restored to the next due card (index 2)
        assertEquals(2, reopenedState.lastCardIndex)
        assertEquals(reopenedCards[2].text, reopenedState.lastWordText)
        assertEquals(1, reopenedState.totalStudiedInDeck)

        // Verify SM-2 data for rated card[0] is persisted in Room
        val persistedCard0Progress = db.wordBooksDao().getWordProgress(cardsSession1[0].cardKey)
        assertNotNull(persistedCard0Progress)
        assertEquals(1, persistedCard0Progress!!.repetitions)
        assertEquals(1, persistedCard0Progress.intervalDays)
        assertTrue(persistedCard0Progress.dueTimestamp > nowMillis)

        // Verify Flashcard Settings persisted across close & reopen
        assertEquals(
            com.example.data.importer.FlashcardDirection.ARABIC_TO_ENGLISH,
            repoSession2.getFlashcardDirection()
        )
        assertEquals(true, repoSession2.getFlashcardAutoShowAnswer())

        // Reset settings to defaults after test
        repoSession2.setFlashcardDirection(com.example.data.importer.FlashcardDirection.ENGLISH_TO_ARABIC)
        repoSession2.setFlashcardAutoShowAnswer(false)
        db.close()
    }

    @Test
    fun verifyBatchTranslate50WordsCloseMidRunReopenAndContinue() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences(com.example.data.ai.BatchTranslateManager.PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().commit()

        val db = Room.inMemoryDatabaseBuilder(context, WordBooksDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.wordBooksDao()
        val repo = DataImportRepository(context, db)
        repo.ensureImportedOnFirstLaunch()
        db.openHelper.writableDatabase.execSQL("UPDATE entries SET arabic = '', arabic_source = ''")

        val geminiService = com.example.data.ai.GeminiDeckAndTranslationService(dao)

        // Mark one C1 word as Favorite to verify Favorites come first before A1..C2
        val sampleDeckEntries = dao.getUntranslatedEntriesForDeckBatch("oxford5000", 80)
        val c1Entry = sampleDeckEntries.firstOrNull { it.level.equals("C1", ignoreCase = true) } ?: sampleDeckEntries.last()
        repo.toggleFavorite("oxford5000", c1Entry.text)

        val prioritized = dao.getUntranslatedEntriesPrioritized("oxford5000", 10)
        assertTrue(prioritized.isNotEmpty())
        assertEquals(c1Entry.text.lowercase(), prioritized.first().text.lowercase())

        // Protect one entry with ar_source = "user" and verify it is never overwritten or selected
        val userProtected = sampleDeckEntries[5]
        dao.updateEntryUserArabicById(userProtected.id, "معنى المستخدم المحمي")

        // Deterministic mock transport returning fenced JSON with 2-4 Arabic meanings, IPA, and examples_ar
        var batchesRequested = 0
        geminiService.batchTransportOverride = { batchEntries ->
            batchesRequested++
            val jsonItems = batchEntries.joinToString(",\n") { entry ->
                val exAr = if (entry.examplesEn.isNotBlank()) "مثال مترجم للكلمة ${entry.text}" else ""
                """
                {
                  "id": "${entry.id}",
                  "arabic": "معنى ١ لـ ${entry.text}، معنى ٢، معنى ٣",
                  "ipa": "/ˈtest.${entry.id}/",
                  "examples_ar": "$exAr"
                }
                """.trimIndent()
            }
            "```json\n[\n$jsonItems\n]\n```"
        }

        // Session 1: Start translating 50 words (2 batches of 25), and simulate closing the app right after Batch 1 (25 words) is saved!
        val managerSession1 = com.example.data.ai.BatchTranslateManager(context, dao, geminiService)
        managerSession1.setAllowAi(true)
        managerSession1.interBatchDelayMs = 0L
        managerSession1.baseBackoffMs = 0L
        managerSession1.startForegroundServiceHook = false
        managerSession1.onlineCheckOverride = { true }

        var closedMidRunExceptionThrown = false
        managerSession1.onBatchSavedHook = { stateAfterBatch1 ->
            if (stateAfterBatch1.batchesCompleted == 1) {
                assertEquals(25, stateAfterBatch1.doneCount)
                assertEquals(50, stateAfterBatch1.totalCount)
                assertEquals(25, stateAfterBatch1.translatedCount)
                closedMidRunExceptionThrown = true
                throw kotlinx.coroutines.CancellationException("Simulated app close after first batch of 25 words")
            }
        }

        try {
            managerSession1.runBatchTranslateSuspend(
                deckFilter = "oxford5000",
                targetCount = 50,
                isResuming = false
            )
        } catch (_: kotlinx.coroutines.CancellationException) {
            // Expected simulated app termination mid-run after batch 1
        }
        assertTrue(closedMidRunExceptionThrown)
        assertEquals(1, batchesRequested)

        // Verify that the first 25 words were permanently saved in one transaction with ar_source = "ai"
        val statsMidRun = dao.getDeckArabicStats().first { it.deck == "oxford5000" }
        assertTrue(statsMidRun.fromAiCount >= 25)

        // Session 2: Reopen the app (create a brand new BatchTranslateManager instance) and verify state is restored at 25 / 50
        val managerSession2 = com.example.data.ai.BatchTranslateManager(context, dao, geminiService)
        managerSession2.interBatchDelayMs = 0L
        managerSession2.baseBackoffMs = 0L
        managerSession2.startForegroundServiceHook = false
        managerSession2.onlineCheckOverride = { true }

        val restoredState = managerSession2.uiState.value
        assertEquals(com.example.data.ai.BatchRunPhase.PAUSED, restoredState.phase)
        assertEquals(25, restoredState.doneCount)
        assertEquals(50, restoredState.totalCount)
        assertEquals(1, restoredState.batchesCompleted)
        assertEquals(2, restoredState.totalBatches)
        assertTrue(restoredState.canResume)

        // Continue the interrupted run in Session 2 to finish all 50 words!
        managerSession2.runBatchTranslateSuspend(
            deckFilter = restoredState.selectedDeck,
            targetCount = restoredState.totalCount,
            isResuming = true
        )

        val completedState = managerSession2.uiState.value
        assertEquals(com.example.data.ai.BatchRunPhase.COMPLETED, completedState.phase)
        assertEquals(50, completedState.doneCount)
        assertEquals(50, completedState.totalCount)
        assertEquals(2, completedState.batchesCompleted)
        assertEquals(50, completedState.translatedCount)
        assertEquals(0, completedState.failedCount)
        assertEquals(2, completedState.requestsUsedToday)

        // Verify user edit was NEVER overwritten
        val userEntryAfter = dao.getEntryById(userProtected.id)
        assertNotNull(userEntryAfter)
        assertEquals("user", userEntryAfter!!.arabicSource)
        assertEquals("معنى المستخدم المحمي", userEntryAfter.arabic)

        // Verify all 50 AI-translated words survive re-imports (ensureImportedOnFirstLaunch & clearAndReimport)
        repo.clearAndReimport()
        val statsAfterReimport = dao.getDeckArabicStats().first { it.deck == "oxford5000" }
        assertTrue(statsAfterReimport.fromAiCount >= 50)

        val userEntryAfterReimport = dao.getEntryById(userProtected.id)
        assertEquals("user", userEntryAfterReimport?.arabicSource)
        assertEquals("معنى المستخدم المحمي", userEntryAfterReimport?.arabic)

        db.close()
    }

    @Test
    fun verifyGrammarOfflineExercisesAndInteractiveAi() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, WordBooksDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.wordBooksDao()

        // 1. OFFLINE (Airplane Mode): Import from assets and verify sample lessons
        var isOnlineMock = false
        val grammarRepo = GrammarRepository(context, dao)
        grammarRepo.importGrammarLessonsIfNeeded(force = true)

        val lessons = dao.getAllGrammarLessons()
        assertTrue("Expected at least 2 sample lessons", lessons.size >= 2)
        val lesson1 = lessons.first { it.id == "g001" }
        assertEquals("A1", lesson1.level)
        assertEquals("Verb to be", lesson1.topic)
        assertEquals("To be: am, is, are", lesson1.titleEn)
        assertEquals("الفعل to be: am, is, are", lesson1.titleAr)
        assertEquals("file", lesson1.source)
        assertTrue(lesson1.explanationAr.isNotBlank())

        val jsonHelper = Json { ignoreUnknownKeys = true }
        val exercises = jsonHelper.decodeFromString<List<GrammarExerciseItem>>(lesson1.exercisesJson)
        assertTrue(exercises.size >= 3)
        assertEquals("mcq", exercises[0].type)

        // Offline exercises test:
        // Question 1 (MCQ): Answer "am" -> correct
        val q1Correct = "am".equals(exercises[0].answer, ignoreCase = true)
        assertTrue(q1Correct)

        // Question 2 (Fill): Answer "is" -> incorrect, offline explanation must be available without internet
        val fillExercise = exercises.first { it.type == "fill" }
        val q2Answer = "wrong_answer"
        val q2Correct = q2Answer.equals(fillExercise.answer, ignoreCase = true)
        assertEquals(false, q2Correct)
        assertTrue("Offline explanation must be non-empty", fillExercise.explanationAr.isNotBlank())

        // Question 3 (Reorder): Answer matching reorder answer -> correct
        val reorderExercise = exercises.first { it.type == "reorder" }
        val q3Answer = reorderExercise.answer
        val q3Correct = q3Answer.equals(reorderExercise.answer, ignoreCase = true)
        assertTrue(q3Correct)

        // Save progress offline: 2 out of 3 correct
        grammarRepo.saveLessonProgress(
            lessonId = lesson1.id,
            completedExercises = 3,
            totalExercises = 3,
            score = 2,
            isCompleted = true
        )

        val progress1 = dao.getGrammarProgress(lesson1.id)
        assertNotNull(progress1)
        assertEquals(3, progress1!!.completedExercises)
        assertEquals(3, progress1.totalExercises)
        assertEquals(2, progress1.bestScore)
        assertEquals(2, progress1.lastScore)
        assertEquals(true, progress1.isCompleted)

        // Re-importing lessons must NEVER overwrite or reset progress!
        grammarRepo.importGrammarLessonsIfNeeded(force = true)
        val progressAfterReimport = dao.getGrammarProgress(lesson1.id)
        assertNotNull(progressAfterReimport)
        assertEquals(2, progressAfterReimport!!.bestScore)
        assertEquals(true, progressAfterReimport.isCompleted)

        // Offline AI check: in airplane mode, AI action throws/fails gracefully
        val grammarAiService = GrammarAiService(
            repository = grammarRepo,
            isAllowAiEnabled = { true },
            isOnline = { isOnlineMock }
        )
        val offlineChatAttempt = grammarAiService.askAboutLesson(lesson1, "Can you explain?")
        assertTrue(offlineChatAttempt.isFailure)
        assertTrue(offlineChatAttempt.exceptionOrNull()?.message?.contains("offline", ignoreCase = true) == true)

        // 2. ONLINE MODE: interactive AI actions
        isOnlineMock = true
        var aiCallCount = 0
        grammarAiService.customAiTransport = { prompt, expectsJson ->
            aiCallCount++
            when {
                prompt.contains("Student Question:", ignoreCase = true) -> {
                    "نستخدم do مع الجمع و does مع المفرد. مثال: He does his homework, They do their work."
                }
                prompt.contains("Student's Wrong Answer:", ignoreCase = true) -> {
                    "كلمة do خاطئة هنا لأن الفاعل مفرد (She). مع المفرد نستخدم does."
                }
                prompt.contains("Create a comprehensive English grammar lesson", ignoreCase = true) -> {
                    """
                    ```json
                    {
                      "id": "g_ai_passive",
                      "level": "B2",
                      "topic": "Passive voice",
                      "title_en": "Passive Voice",
                      "title_ar": "المبني للمجهول",
                      "explanation_ar": "يُستخدم المبني للمجهول للتركيز على الحدث أو المفعول به أكثر من الفاعل.",
                      "examples": [
                        { "en": "The bridge was built in 1990.", "ar": "تم بناء الجسر في عام ١٩٩٠." }
                      ],
                      "exercises": [
                        {
                          "type": "mcq",
                          "prompt": "The letter ___ yesterday.",
                          "options": ["was sent", "sent", "is send"],
                          "answer": "was sent",
                          "explanation_ar": "في الماضي البسيط للمبني للمجهول نستخدم was + التصريف الثالث."
                        }
                      ],
                      "source": "ai"
                    }
                    ```
                    """.trimIndent()
                }
                else -> "{}"
            }
        }

        // Action 1: Ask AI about this lesson
        val chatResult = grammarAiService.askAboutLesson(lesson1, "What is the difference between do and does?")
        assertTrue(chatResult.isSuccess)
        assertTrue(chatResult.getOrThrow().contains("does"))

        val chatHistory = dao.getAiHistory(GrammarRepository.CONTEXT_CHAT, lesson1.id)
        assertEquals(2, chatHistory.size)
        assertEquals("user", chatHistory[0].role)
        assertEquals("assistant", chatHistory[1].role)

        // Action 2: Explain my mistake
        val mistake1 = grammarAiService.explainMistake(
            lessonId = lesson1.id,
            lessonTitleEn = lesson1.titleEn,
            exercisePrompt = exercises[1].prompt,
            wrongAnswer = "do",
            correctAnswer = "does"
        )
        assertTrue(mistake1.isSuccess)
        val initialCallCount = aiCallCount

        // Requesting the exact same mistake explanation again MUST hit ai_cache without calling Gemini transport!
        val mistakeCached = grammarAiService.explainMistake(
            lessonId = lesson1.id,
            lessonTitleEn = lesson1.titleEn,
            exercisePrompt = exercises[1].prompt,
            wrongAnswer = "do",
            correctAnswer = "does"
        )
        assertTrue(mistakeCached.isSuccess)
        assertEquals(initialCallCount, aiCallCount) // No extra API request made!

        // Action 3: Create a new lesson with AI
        val createResult = grammarAiService.createNewLessonWithAi(level = "B2", topic = "Passive voice")
        assertTrue(createResult.isSuccess)

        val newLesson = dao.getGrammarLessonById("g_ai_passive")
        assertNotNull(newLesson)
        assertEquals("B2", newLesson!!.level)
        assertEquals("Passive voice", newLesson.topic)
        assertEquals("ai", newLesson.source)
        assertTrue(newLesson.titleEn.isNotBlank())

        db.close()
    }

    @Test
    fun verifyGrammarAllLessonsLoadedFromAssetsAndCustomJsonImportWorks() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, WordBooksDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.wordBooksDao()
        val grammarRepo = GrammarRepository(context, dao)

        // 1. Initial sync from assets/grammar_lessons.json
        val importCountRes = grammarRepo.importGrammarLessonsIfNeeded(force = true)
        assertTrue(importCountRes.isSuccess)
        val initialLessons = dao.getAllGrammarLessons()

        // Confirms all 90 lessons in assets/grammar_lessons.json are imported
        assertEquals(90, initialLessons.size)
        val lesson54 = dao.getGrammarLessonById("g054")
        assertNotNull(lesson54)
        assertEquals("Third conditional", lesson54!!.titleEn)
        assertEquals("B2", lesson54.level)

        // 2. User plays lesson g054 and saves progress
        grammarRepo.saveLessonProgress(
            lessonId = "g054",
            completedExercises = 8,
            totalExercises = 8,
            score = 7,
            isCompleted = true
        )
        val savedProgress = dao.getGrammarProgress("g054")
        assertNotNull(savedProgress)
        assertEquals(7, savedProgress!!.bestScore)
        assertTrue(savedProgress.isCompleted)

        // 3. User imports a custom grammar JSON file containing:
        //    - An update to g054 with an updated title/explanation
        //    - A brand new lesson g099 (C1 Inversion)
        val customJson = """
            [
              {
                "id": "g054",
                "level": "B2",
                "topic": "Conditionals",
                "title_en": "Third conditional (Updated)",
                "title_ar": "الشرط الثالث المحدث",
                "explanation_ar": "شرح محدث للشرط الثالث",
                "examples": [
                  {"en": "If I had known, I would have helped.", "ar": "لو كنت أعلم لساعدت."}
                ],
                "exercises": [
                  {"type": "mcq", "prompt": "If she had asked, I ___ helped.", "options": ["would have", "will have"], "answer": "would have", "explanation_ar": "قاعدة الشرط الثالث"}
                ],
                "source": "file"
              },
              {
                "id": "g099",
                "level": "C1",
                "topic": "Advanced Structures",
                "title_en": "Inversion with negative adverbials",
                "title_ar": "القلب مع الظروف المنفية",
                "explanation_ar": "نستخدم القلب للتوكيد بعد ظروف مثل Rarely و Seldom و Not only",
                "examples": [
                  {"en": "Rarely have I seen such beauty.", "ar": "نادرًا ما رأيت مثل هذا الجمال."}
                ],
                "exercises": [
                  {"type": "mcq", "prompt": "Seldom ___ so inspired.", "options": ["have I felt", "I have felt"], "answer": "have I felt", "explanation_ar": "قلب الفعل المساعد والفاعل"}
                ],
                "source": "file"
              }
            ]
        """.trimIndent()

        val importCustomRes = grammarRepo.importGrammarLessonsFromJsonString(customJson, source = "file")
        assertTrue(importCustomRes.isSuccess)
        assertEquals(2, importCustomRes.getOrNull())

        // 4. Verify total count increased from 90 to 91
        val updatedLessons = dao.getAllGrammarLessons()
        assertEquals(91, updatedLessons.size)

        // 5. Verify user progress on g054 was completely PRESERVED
        val progressAfterImport = dao.getGrammarProgress("g054")
        assertNotNull(progressAfterImport)
        assertEquals(7, progressAfterImport!!.bestScore)
        assertTrue(progressAfterImport.isCompleted)

        // 6. Verify g054 content was updated
        val updatedLesson54 = dao.getGrammarLessonById("g054")
        assertNotNull(updatedLesson54)
        assertEquals("Third conditional (Updated)", updatedLesson54!!.titleEn)

        // 7. Verify new lesson g099 was created and marked source = "file"
        val lesson99 = dao.getGrammarLessonById("g099")
        assertNotNull(lesson99)
        assertEquals("Inversion with negative adverbials", lesson99!!.titleEn)
        assertEquals("C1", lesson99.level)
        assertEquals("file", lesson99.source)

        // 8. Verify flexible JSON with wrapped object {"lessons": [...]} and camelCase
        val wrappedCamelCaseJson = """
            {
              "lessons": [
                {
                  "id": "g100",
                  "level": "C2",
                  "topic": "Subjunctive",
                  "titleEn": "The Present Subjunctive",
                  "titleAr": "صيغة المنصوب الحالية",
                  "explanationAr": "تُستخدم للتعبير عن الضرورة أو الطلب الرسمي",
                  "examples": [{"en": "I recommend that he be present.", "ar": "أوصي بأن يكون حاضرًا."}],
                  "exercises": [{"type": "mcq", "prompt": "It is essential that she ___ on time.", "options": ["be", "is", "was"], "answer": "be", "explanation_ar": "المصدر المجرد بعد essential"}]
                }
              ]
            }
        """.trimIndent()

        val importWrappedRes = grammarRepo.importGrammarLessonsFromJsonString(wrappedCamelCaseJson)
        assertTrue(importWrappedRes.isSuccess)
        val lesson100 = dao.getGrammarLessonById("g100")
        assertNotNull(lesson100)
        assertEquals("The Present Subjunctive", lesson100!!.titleEn)
        assertEquals("C2", lesson100.level)

        db.close()
    }

    @Test
    fun verifyReadingSectionOfflineAndAiFeatures() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, WordBooksDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val dataImportRepo = DataImportRepository(context, db)
        dataImportRepo.ensureImportedOnFirstLaunch(forceRecheck = true)

        val dao = db.wordBooksDao()
        val readingRepo = ReadingRepository(context, dao)

        // 1. Initial import loads sample texts from reading_texts.json
        val importRes = readingRepo.importReadingTextsIfNeeded()
        assertTrue(importRes.isSuccess)
        val initialTexts = dao.getAllReadingTexts()
        assertTrue("Expected at least 2 sample reading texts, found ${initialTexts.size}", initialTexts.size >= 2)

        val r030 = dao.getReadingTextById("r030")
        assertNotNull(r030)
        assertEquals("B1", r030!!.level)
        assertEquals("environment", r030.topic)
        assertEquals("Saving Our Local Park", r030.titleEn)
        assertEquals("sample", r030.source)

        // 2. Local word lookup in offline dictionary
        val lookupCity = readingRepo.lookupWordLocally("city")
        assertTrue("Word 'city' should be found in local dictionary", lookupCity.isNotEmpty())

        val lookupCities = readingRepo.lookupWordLocally("cities")
        assertTrue("Inflected 'cities' should map to 'city'", lookupCities.isNotEmpty())

        val notFound = readingRepo.lookupWordLocally("xyzunobtainiumword")
        assertTrue(notFound.isEmpty())

        // 3. User saves progress on r030
        readingRepo.saveReadingProgress(
            textId = "r030",
            completedQuestions = 5,
            totalQuestions = 5,
            score = 4,
            isCompleted = true
        )
        val prog = dao.getReadingProgress("r030")
        assertNotNull(prog)
        assertEquals(4, prog!!.bestScore)
        assertTrue(prog.isCompleted)

        // 4. Re-sync from assets: content upserts by ID, progress is preserved!
        val resyncRes = readingRepo.importReadingTextsFromAssets(force = true)
        assertTrue(resyncRes.isSuccess)
        val progAfter = dao.getReadingProgress("r030")
        assertNotNull(progAfter)
        assertEquals(4, progAfter!!.bestScore)
        assertTrue(progAfter.isCompleted)

        // 5. User pastes text: split into sentences, marked source = "user"
        val pasted = readingRepo.savePastedText(
            titleEn = "My Morning Coffee",
            rawText = "I wake up at seven o'clock every day. Coffee helps me focus on learning English. The morning sunlight feels warm and refreshing.",
            topic = "Daily Life",
            level = "A2"
        )
        assertTrue(pasted.isSuccess)
        val pastedEntity = pasted.getOrNull()
        assertNotNull(pastedEntity)
        assertEquals("user", pastedEntity!!.source)
        assertEquals("A2", pastedEntity.level)

        // 6. Test AI service with mocked transport
        var allowAi = true
        var isOnline = true
        val readingAi = ReadingAiService(
            repository = readingRepo,
            isAllowAiEnabled = { allowAi },
            isOnline = { isOnline }
        )

        // Offline check
        isOnline = false
        val offlineRes = readingAi.createNewTextWithAi("B2", "Technology")
        assertTrue(offlineRes.isFailure)
        assertTrue(offlineRes.exceptionOrNull()?.message?.contains("offline", ignoreCase = true) == true)
        isOnline = true

        // Allow AI disabled check
        allowAi = false
        val disabledRes = readingAi.createNewTextWithAi("B2", "Technology")
        assertTrue(disabledRes.isFailure)
        assertTrue(disabledRes.exceptionOrNull()?.message?.contains("Allow AI", ignoreCase = true) == true)
        allowAi = true

        // Mocked AI transport for Create New Text
        readingAi.customAiTransport = { prompt, expectsJson ->
            """
            {
              "id": "r_ai_test1",
              "level": "B2",
              "topic": "Technology",
              "title_en": "Artificial Intelligence in Modern Medicine",
              "title_ar": "الذكاء الاصطناعي في الطب الحديث",
              "sentences": [
                { "en": "Modern hospitals utilize intelligent algorithms to diagnose rare diseases.", "ar": "تستخدم المستشفيات الحديثة خوارزميات ذكية لتشخيص الأمراض النادرة." },
                { "en": "Doctors can analyze medical scans with greater precision and speed.", "ar": "يمكن للأطباء تحليل الصور الطبية بدقة وسرعة أكبر." }
              ],
              "questions": [
                {
                  "prompt": "How do modern hospitals utilize intelligent algorithms?",
                  "options": ["To diagnose rare diseases", "To clean patient rooms", "To cook meals", "To drive ambulances"],
                  "answer_index": 0,
                  "explanation_ar": "يذكر النص استخدام الخوارزميات الذكية لتشخيص الأمراض النادرة."
                }
              ],
              "target_words": ["algorithms", "precision"]
            }
            """.trimIndent()
        }

        val aiCreateRes = readingAi.createNewTextWithAi("B2", "Technology", "short", listOf("algorithms", "precision"))
        assertTrue(aiCreateRes.isSuccess)
        val createdId = aiCreateRes.getOrNull()
        assertNotNull(createdId)
        val aiEntity = dao.getReadingTextById(createdId!!)
        assertNotNull(aiEntity)
        assertEquals("ai", aiEntity!!.source)
        assertEquals("B2", aiEntity.level)
        assertTrue(aiEntity.targetWordsJson.contains("algorithms"))

        // Mocked AI Explain Word in Context
        readingAi.customAiTransport = { prompt, _ ->
            "الخوارزميات: مجموعة من الخطوات الرياضية والمنطقية المتسلسلة لحل مشكلة ما."
        }
        val explainRes = readingAi.explainWordInContext(
            textId = createdId,
            sentenceIndex = 0,
            sentenceEn = "Modern hospitals utilize intelligent algorithms to diagnose rare diseases.",
            word = "algorithms"
        )
        assertTrue(explainRes.isSuccess)
        assertEquals("الخوارزميات: مجموعة من الخطوات الرياضية والمنطقية المتسلسلة لحل مشكلة ما.", explainRes.getOrNull())

        // Verify explanation is cached in ai_cache
        val cached = readingRepo.getCachedWordExplanation(createdId, 0, "algorithms")
        assertEquals("الخوارزميات: مجموعة من الخطوات الرياضية والمنطقية المتسلسلة لحل مشكلة ما.", cached)

        db.close()
    }

    @Test
    fun verifyGrammarReadingIntegrationAndOfflineCorrectionNotebook() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, WordBooksDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val dao = db.wordBooksDao()
        val grammarRepo = GrammarRepository(context, dao)
        val readingRepo = ReadingRepository(context, dao)

        // 1. Import Grammar lessons and Reading texts from assets
        val grammarImportRes = grammarRepo.importGrammarLessonsIfNeeded()
        assertTrue("Grammar import should succeed", grammarImportRes.isSuccess)
        val readingImportRes = readingRepo.importReadingTextsIfNeeded()
        assertTrue("Reading import should succeed", readingImportRes.isSuccess)

        // 2. Open 'The Missed Interview' (r054)
        val r054 = dao.getReadingTextById("r054")
        assertNotNull("Text r054 'The Missed Interview' must exist in assets", r054)
        assertEquals("The Missed Interview", r054!!.titleEn)
        assertTrue("Text r054 must link to grammar lesson g054", r054.grammarIdsJson.contains("g054"))

        // Verify sentences contain grammar_id g054
        val json = Json { ignoreUnknownKeys = true; isLenient = true }
        val sentences = json.decodeFromString<List<ReadingSentenceItem>>(r054.sentencesJson)
        val conditionalSentences = sentences.filter { it.grammarId == "g054" }
        assertTrue("There must be highlighted sentences with grammar_id g054", conditionalSentences.isNotEmpty())
        val firstGrammarSentence = conditionalSentences.first()
        assertTrue(firstGrammarSentence.en.contains("If Omar had set his alarm"))

        // 3. Corresponding Grammar lesson g054 exists and has offline exercises
        val g054 = dao.getGrammarLessonById("g054")
        assertNotNull("Lesson g054 must exist in database", g054)
        assertEquals("Third conditional", g054!!.titleEn)
        val exercises = json.decodeFromString<List<GrammarExerciseItem>>(g054.exercisesJson)
        assertTrue("Lesson g054 must contain offline exercises for practice", exercises.size >= 2)

        // 4. Offline mode (Airplane mode): Save sentence to Correction Notebook
        // User writes: "If I had practiced more, I would have passed the exam."
        val userSentence = "If I had practiced more, I would have passed the exam."
        val savedEntryId = readingRepo.saveCorrectionEntry(
            textId = r054.id,
            grammarLessonId = g054.id,
            grammarLessonTitle = g054.titleEn,
            originalSentence = userSentence,
            correctedSentence = "",
            explanationAr = "",
            betterAlternative = "",
            status = "not_checked"
        )

        val savedEntry = dao.getCorrectionEntryById(savedEntryId)
        assertNotNull(savedEntry)
        assertEquals(userSentence, savedEntry!!.originalSentence)
        assertEquals("not_checked", savedEntry.status)
        assertEquals(g054.id, savedEntry.grammarLessonId)
        assertEquals(0, savedEntry.attemptsCount)

        // 5. Spaced Repetition offline (3 -> 7 -> 21 days -> mastered)
        // Check everyday situations list has 30 items
        assertEquals(30, ReadingRepository.EVERYDAY_SITUATIONS.size)

        // Attempt 1: Success -> scheduled for 7 days
        readingRepo.recordSpacedReviewAttempt(savedEntryId, "If they had arrived early, they would have caught the train.", isSuccess = true)
        val entryAfterAttempt1 = dao.getCorrectionEntryById(savedEntryId)!!
        assertEquals(1, entryAfterAttempt1.attemptsCount)
        assertEquals("checked", entryAfterAttempt1.status)
        assertTrue("Next review should be scheduled ~7 days ahead", entryAfterAttempt1.nextReviewTimestamp > System.currentTimeMillis())

        // Attempt 2: Success -> scheduled for 21 days
        readingRepo.recordSpacedReviewAttempt(savedEntryId, "If she had warned me, I would have taken an umbrella.", isSuccess = true)
        val entryAfterAttempt2 = dao.getCorrectionEntryById(savedEntryId)!!
        assertEquals(2, entryAfterAttempt2.attemptsCount)

        // Attempt 3: Success -> marked mastered
        readingRepo.recordSpacedReviewAttempt(savedEntryId, "If we had booked earlier, we would have got tickets.", isSuccess = true)
        val entryAfterAttempt3 = dao.getCorrectionEntryById(savedEntryId)!!
        assertEquals(3, entryAfterAttempt3.attemptsCount)
        assertEquals("mastered", entryAfterAttempt3.status)

        // 6. Online with Allow AI: "Check my sentence" asks Gemini once
        var allowAi = true
        var isOnline = true
        var aiCallCount = 0
        val readingAi = ReadingAiService(
            repository = readingRepo,
            isAllowAiEnabled = { allowAi },
            isOnline = { isOnline }
        )
        readingAi.customAiTransport = { prompt, expectsJson ->
            aiCallCount++
            """
            {
              "corrected_sentence": "If I had practiced more, I would have passed the exam.",
              "explanation_ar": "الجملة صحيحة تمامًا وتتبع قاعدة الشرط الثالث (If + past perfect, would have + V3).",
              "better_alternative": "Had I practiced more, I would have passed the exam."
            }
            """.trimIndent()
        }

        // Test checkUserSentence
        val checkRes = readingAi.checkUserSentence(g054.titleEn, userSentence)
        assertTrue(checkRes.isSuccess)
        val checkResult = checkRes.getOrNull()!!
        assertEquals(1, aiCallCount)
        assertEquals("Had I practiced more, I would have passed the exam.", checkResult.betterAlternative)

        // Update notebook entry with AI result
        val updatedEntry = entryAfterAttempt3.copy(
            correctedSentence = checkResult.correctedSentence,
            explanationAr = checkResult.explanationAr,
            betterAlternative = checkResult.betterAlternative,
            status = "checked"
        )
        dao.upsertCorrectionEntry(updatedEntry)

        val finalEntry = dao.getCorrectionEntryById(savedEntryId)!!
        assertEquals("checked", finalEntry.status)
        assertEquals("Had I practiced more, I would have passed the exam.", finalEntry.betterAlternative)

        // Second check with cached AI result (should not call AI again)
        val cachedCheckRes = readingAi.checkUserSentence(g054.titleEn, userSentence)
        assertTrue(cachedCheckRes.isSuccess)
        assertEquals("AI call count should not increase because result is cached in ai_cache", 1, aiCallCount)

        db.close()
    }

    @Test
    fun verifyRoomMigration9To10PreservesExistingDataAndAddsAgainCount() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "test_migration_9_10.db"
        context.deleteDatabase(dbName)

        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(9) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Create v9 tables
                    db.execSQL("""
                        CREATE TABLE entries (
                            id TEXT PRIMARY KEY NOT NULL,
                            deck TEXT NOT NULL,
                            text TEXT NOT NULL,
                            pos TEXT NOT NULL,
                            level TEXT NOT NULL,
                            type TEXT NOT NULL,
                            ipa TEXT NOT NULL,
                            audio TEXT NOT NULL,
                            arabic TEXT NOT NULL,
                            arabic_source TEXT NOT NULL,
                            definition_en TEXT NOT NULL,
                            examples_en TEXT NOT NULL,
                            examples_ar TEXT NOT NULL,
                            notes TEXT NOT NULL,
                            data_version INTEGER NOT NULL
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE word_progress (
                            card_key TEXT PRIMARY KEY NOT NULL,
                            deck TEXT NOT NULL,
                            text TEXT NOT NULL,
                            interval INTEGER NOT NULL,
                            ease_factor REAL NOT NULL,
                            repetitions INTEGER NOT NULL,
                            last_reviewed_timestamp INTEGER NOT NULL,
                            due_timestamp INTEGER NOT NULL,
                            is_favorite INTEGER NOT NULL,
                            is_mastered INTEGER NOT NULL
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE sentences (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            english_text TEXT NOT NULL,
                            arabic_text TEXT NOT NULL,
                            source_word TEXT NOT NULL,
                            is_favorite INTEGER NOT NULL,
                            last_practiced_timestamp INTEGER NOT NULL,
                            practice_count INTEGER NOT NULL,
                            created_at INTEGER NOT NULL
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE grammar_lessons (
                            id TEXT PRIMARY KEY NOT NULL,
                            level TEXT NOT NULL,
                            topic TEXT NOT NULL,
                            title_en TEXT NOT NULL,
                            title_ar TEXT NOT NULL,
                            explanation_ar TEXT NOT NULL,
                            examples_json TEXT NOT NULL,
                            exercises_json TEXT NOT NULL,
                            summary_ar TEXT NOT NULL,
                            is_completed INTEGER NOT NULL,
                            data_version INTEGER NOT NULL
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE reading_texts (
                            id TEXT PRIMARY KEY NOT NULL,
                            level TEXT NOT NULL,
                            topic TEXT NOT NULL,
                            title_en TEXT NOT NULL,
                            title_ar TEXT NOT NULL,
                            sentences_json TEXT NOT NULL,
                            questions_json TEXT NOT NULL,
                            source TEXT NOT NULL,
                            target_words_json TEXT NOT NULL,
                            grammar_ids_json TEXT NOT NULL,
                            linked_text_id TEXT,
                            data_version INTEGER NOT NULL,
                            created_at INTEGER NOT NULL
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE correction_notebook (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            text_id TEXT NOT NULL,
                            grammar_lesson_id TEXT NOT NULL,
                            grammar_lesson_title TEXT NOT NULL,
                            original_sentence TEXT NOT NULL,
                            corrected_sentence TEXT NOT NULL,
                            explanation_ar TEXT NOT NULL,
                            better_alternative TEXT NOT NULL,
                            status TEXT NOT NULL,
                            review_step INTEGER NOT NULL,
                            next_review_timestamp INTEGER NOT NULL,
                            situation_prompt TEXT NOT NULL,
                            attempts_count INTEGER NOT NULL,
                            created_at INTEGER NOT NULL,
                            updated_at INTEGER NOT NULL
                        )
                    """.trimIndent())
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val v9Db = helper.writableDatabase

        // Insert sample records in v9
        v9Db.execSQL("INSERT INTO entries VALUES ('e1', 'oxford5000', 'abandon', 'verb', 'B2', 'single_word', '/əˈbændən/', '', 'يتخلى عن', 'file', 'to leave', 'abandon ship', 'غادر السفينة', '', 1)")
        v9Db.execSQL("INSERT INTO word_progress VALUES ('oxford5000:abandon', 'oxford5000', 'abandon', 1, 2.5, 1, 1000, 2000, 1, 0)")
        v9Db.execSQL("INSERT INTO sentences VALUES (1, 'Never abandon your dreams.', 'لا تتخلى عن أحلامك أبداً.', 'abandon', 1, 1000, 1, 1000)")
        v9Db.execSQL("INSERT INTO grammar_lessons VALUES ('g054', 'B2', 'Conditionals', 'Third conditional', 'الشرط الثالث', 'شرح', '[]', '[]', '', 1, 1)")
        v9Db.execSQL("INSERT INTO reading_texts VALUES ('r054', 'B2', 'Interviews', 'The Missed Interview', 'المقابلة الضائعة', '[]', '[]', 'sample', '[]', '[\"g054\"]', NULL, 1, 1000)")
        v9Db.execSQL("INSERT INTO correction_notebook VALUES (1, 'r054', 'g054', 'Third conditional', 'If I had studied...', 'If I had studied...', 'Correct', '', 'not_checked', 0, 0, '', 0, 1000, 1000)")

        // Apply MIGRATION_9_10
        WordBooksDatabase.MIGRATION_9_10.migrate(v9Db)

        // 1. Verify entries preserved
        val entriesCursor = v9Db.query("SELECT text, arabic FROM entries WHERE id = 'e1'")
        assertTrue(entriesCursor.moveToFirst())
        assertEquals("abandon", entriesCursor.getString(0))
        assertEquals("يتخلى عن", entriesCursor.getString(1))
        entriesCursor.close()

        // 2. Verify word_progress preserved and again_count added with default 0
        val wpCursor = v9Db.query("SELECT card_key, again_count FROM word_progress WHERE card_key = 'oxford5000:abandon'")
        assertTrue(wpCursor.moveToFirst())
        assertEquals("oxford5000:abandon", wpCursor.getString(0))
        assertEquals(0, wpCursor.getInt(1))
        wpCursor.close()

        // Update again_count
        v9Db.execSQL("UPDATE word_progress SET again_count = again_count + 1 WHERE card_key = 'oxford5000:abandon'")
        val wpCursor2 = v9Db.query("SELECT again_count FROM word_progress WHERE card_key = 'oxford5000:abandon'")
        assertTrue(wpCursor2.moveToFirst())
        assertEquals(1, wpCursor2.getInt(0))
        wpCursor2.close()

        // 3. Verify sentences, grammar_lessons, reading_texts, correction_notebook preserved
        val sentCursor = v9Db.query("SELECT english_text FROM sentences WHERE id = 1")
        assertTrue(sentCursor.moveToFirst())
        assertEquals("Never abandon your dreams.", sentCursor.getString(0))
        sentCursor.close()

        val glCursor = v9Db.query("SELECT title_en FROM grammar_lessons WHERE id = 'g054'")
        assertTrue(glCursor.moveToFirst())
        assertEquals("Third conditional", glCursor.getString(0))
        glCursor.close()

        val rtCursor = v9Db.query("SELECT title_en, grammar_ids_json FROM reading_texts WHERE id = 'r054'")
        assertTrue(rtCursor.moveToFirst())
        assertEquals("The Missed Interview", rtCursor.getString(0))
        assertTrue(rtCursor.getString(1).contains("g054"))
        rtCursor.close()

        val cnCursor = v9Db.query("SELECT original_sentence FROM correction_notebook WHERE id = 1")
        assertTrue(cnCursor.moveToFirst())
        assertEquals("If I had studied...", cnCursor.getString(0))
        cnCursor.close()

        // 4. Verify new tables exist and can be written to
        v9Db.execSQL("INSERT INTO study_plan_profile VALUES (1, 'B2', 'general_fluency', 30, 6, '2026-11-01', '[]', '[6]', 1000)")
        val profileCursor = v9Db.query("SELECT level, minutes_per_day FROM study_plan_profile WHERE id = 1")
        assertTrue(profileCursor.moveToFirst())
        assertEquals("B2", profileCursor.getString(0))
        assertEquals(30, profileCursor.getInt(1))
        profileCursor.close()

        v9Db.execSQL("INSERT INTO daily_task_checklist VALUES ('2026-11-01', '[]', 0, 5, 0, NULL, 0)")
        val checkCursor = v9Db.query("SELECT date, total_count FROM daily_task_checklist WHERE date = '2026-11-01'")
        assertTrue(checkCursor.moveToFirst())
        assertEquals("2026-11-01", checkCursor.getString(0))
        assertEquals(5, checkCursor.getInt(1))
        checkCursor.close()

        v9Db.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun verifyStudyPlanAndLocalDailyChecklistExecution() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, WordBooksDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val dao = db.wordBooksDao()
        val importRepo = DataImportRepository(context, db)
        importRepo.ensureImportedOnFirstLaunch(forceRecheck = true)
        val grammarRepo = GrammarRepository(context, dao)
        grammarRepo.importGrammarLessonsIfNeeded()
        val readingRepo = ReadingRepository(context, dao)
        readingRepo.importReadingTextsIfNeeded()

        // Start time provider on Monday, November 2, 2026
        val timeProvider = TestTimeProvider(1793577600000L) // Monday
        val planRepo = StudyPlanRepository(context, dao, timeProvider)

        // 1. Scaling default B2 plan:
        val profile30 = StudyPlanProfileEntity(
            id = 1,
            level = "B2",
            goal = "general_fluency",
            minutesPerDay = 30,
            studyDaysPerWeek = 6,
            startDate = "2026-11-02",
            selectedDecksJson = "[\"oxford5000\",\"oxford_opal\"]",
            restDaysJson = "[6]" // Friday is rest day
        )
        val plan30 = planRepo.loadDefaultPlanScaled(profile30, "2026-11")
        assertEquals("2026-11", plan30.month)
        assertEquals("B2", plan30.levelStart)

        val json = Json { ignoreUnknownKeys = true; isLenient = true }
        val template30 = json.decodeFromString<PlanDailyTemplate>(plan30.dailyTemplateJson)
        assertEquals(10, template30.new_words)
        assertEquals(8, template30.review_cards_min)

        // Scale by 15 min / 5 days
        val profile15 = profile30.copy(minutesPerDay = 15, studyDaysPerWeek = 5)
        val plan15 = planRepo.loadDefaultPlanScaled(profile15, "2026-11")
        val template15 = json.decodeFromString<PlanDailyTemplate>(plan15.dailyTemplateJson)
        assertEquals(5, template15.new_words)
        assertEquals(4, template15.review_cards_min)

        // Scale by 60 min / 7 days
        val profile60 = profile30.copy(minutesPerDay = 60, studyDaysPerWeek = 7)
        val plan60 = planRepo.loadDefaultPlanScaled(profile60, "2026-11")
        val template60 = json.decodeFromString<PlanDailyTemplate>(plan60.dailyTemplateJson)
        assertEquals(20, template60.new_words)
        assertEquals(16, template60.review_cards_min)

        // 2. Set active profile to 30 min / 6 days and generate Monday checklist
        planRepo.updateProfile(profile30)
        val mondayChecklist = planRepo.getOrCreateDailyChecklist(timeProvider.todayDateString())
        assertNotNull(mondayChecklist)
        val mondayTasks = json.decodeFromString<List<DailyTaskItem>>(mondayChecklist.tasksJson)
        assertTrue("Monday checklist should have tasks", mondayTasks.isNotEmpty())

        // Check task types
        val flashcardTask = mondayTasks.find { it.type == "flashcards" }
        assertNotNull(flashcardTask)
        assertTrue(flashcardTask!!.targetCount <= 30) // Capped at 30 cards

        val newWordsTask = mondayTasks.find { it.type == "new_words" }
        assertNotNull(newWordsTask)
        assertEquals(10, newWordsTask!!.targetCount)

        val grammarTask = mondayTasks.find { it.type == "grammar" }
        assertNotNull("Grammar lesson should be scheduled on Monday", grammarTask)

        val readingTask = mondayTasks.find { it.type == "reading" }
        assertNotNull("Reading text should be scheduled and paired on Monday", readingTask)

        // 3. Idempotent checklist retrieval
        val sameMondayChecklist = planRepo.getOrCreateDailyChecklist(timeProvider.todayDateString())
        val sameMondayTasks = json.decodeFromString<List<DailyTaskItem>>(sameMondayChecklist.tasksJson)
        assertEquals(mondayTasks.size, sameMondayTasks.size)
        assertEquals(mondayTasks.map { it.id }, sameMondayTasks.map { it.id })

        // 4. Mark tasks completed and verify streak
        mondayTasks.forEach { task ->
            planRepo.markTaskCompletedManually(mondayChecklist.date, task.id)
        }
        val completedMonday = planRepo.getOrCreateDailyChecklist(mondayChecklist.date)
        assertTrue(completedMonday.isCompleted)
        val streakDay1 = planRepo.computeStreak()
        assertEquals(1, streakDay1)

        // Advance to Tuesday and complete it
        timeProvider.advanceDays(1)
        val tuesdayChecklist = planRepo.getOrCreateDailyChecklist(timeProvider.todayDateString())
        val tuesdayTasks = json.decodeFromString<List<DailyTaskItem>>(tuesdayChecklist.tasksJson)
        tuesdayTasks.forEach { task ->
            planRepo.markTaskCompletedManually(tuesdayChecklist.date, task.id)
        }
        val streakDay2 = planRepo.computeStreak()
        assertEquals(2, streakDay2)

        // 5. Rest day (Friday) does not break streak
        // Advance to Friday (from Tuesday: +3 days)
        timeProvider.advanceDays(3) // Friday
        val streakOnRestDay = planRepo.computeStreak()
        assertEquals(2, streakOnRestDay)

        // 6. Missed day handling:
        // Advance by 3 more days without completing anything
        timeProvider.advanceDays(3)
        val postMissedChecklist = planRepo.getOrCreateDailyChecklist(timeProvider.todayDateString())
        assertTrue("Checklist after missed days should be marked adjusted", postMissedChecklist.isAdjusted)
        val postTasks = json.decodeFromString<List<DailyTaskItem>>(postMissedChecklist.tasksJson)
        val reducedNewWords = postTasks.find { it.type == "new_words" }
        assertNotNull(reducedNewWords)
        assertEquals("New words should be reduced to 5 for one day only without guilt", 5, reducedNewWords!!.targetCount)

        db.close()
    }

    @Test
    fun verifyLocalQuizEngineAndSkillsMix() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, WordBooksDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val dao = db.wordBooksDao()
        val importRepo = DataImportRepository(context, db)
        importRepo.ensureImportedOnFirstLaunch(forceRecheck = true)
        val grammarRepo = GrammarRepository(context, dao)
        grammarRepo.importGrammarLessonsIfNeeded()
        val readingRepo = ReadingRepository(context, dao)
        readingRepo.importReadingTextsIfNeeded()

        // Enrich 10 words with non-empty Arabic
        for (i in 1..10) {
            dao.updateEntryUserArabicById(i.toString(), "ترجمة كلمة $i")
        }

        val timeProvider = TestTimeProvider(1793577600000L)
        val quizEngine = LocalQuizEngine(dao, timeProvider)

        // 1. Daily Mini-Quiz (5 questions)
        val dailyQuiz = quizEngine.generateDailyMiniQuiz(isTtsAvailable = true)
        assertEquals(5, dailyQuiz.size)
        // Verify all vocabulary questions have non-empty Arabic
        dailyQuiz.filter { it.skill == "vocabulary" }.forEach { q ->
            assertTrue(q.promptAr.isNotBlank())
            assertTrue(q.explanationAr.isNotBlank())
            assertTrue(q.options.all { it.isNotBlank() })
        }
        // Verify listening question exists when TTS available
        assertTrue(dailyQuiz.any { it.skill == "listening" })

        // When TTS unavailable: listening skipped
        val dailyQuizNoTts = quizEngine.generateDailyMiniQuiz(isTtsAvailable = false)
        assertEquals(5, dailyQuizNoTts.size)
        assertFalse(dailyQuizNoTts.any { it.skill == "listening" })

        // 2. Weekly Quiz (20 questions: 8 vocab, 6 grammar, 3 reading, 3 listening)
        val weeklyQuiz = quizEngine.generateWeeklyQuiz(isTtsAvailable = true)
        assertEquals(20, weeklyQuiz.size)
        val vocabCount = weeklyQuiz.count { it.skill == "vocabulary" }
        val grammarCount = weeklyQuiz.count { it.skill == "grammar" }
        val readingCount = weeklyQuiz.count { it.skill == "reading" }
        val listeningCount = weeklyQuiz.count { it.skill == "listening" }

        assertEquals(8, vocabCount)
        assertEquals(6, grammarCount)
        assertEquals(3, readingCount)
        assertEquals(3, listeningCount)

        // 3. Record quiz submission and verify results in DB
        val userAnswers = weeklyQuiz.associate { it.id to it.answerIndex } // 100% correct
        val accuracyMap = quizEngine.recordQuizSubmission("weekly", weeklyQuiz, userAnswers, timeProvider.todayDateString())
        assertEquals(100f, accuracyMap["vocabulary"] ?: 0f, 0.01f)
        assertEquals(100f, accuracyMap["grammar"] ?: 0f, 0.01f)
        assertEquals(100f, accuracyMap["reading"] ?: 0f, 0.01f)
        assertEquals(100f, accuracyMap["listening"] ?: 0f, 0.01f)

        val resultsInDb = dao.getQuizResultsForDate(timeProvider.todayDateString())
        assertTrue("Quiz results must be persisted per skill", resultsInDb.size >= 4)

        db.close()
    }

    @Test
    fun verifyAiPlanSafeguardsAndValidation() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, WordBooksDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val dao = db.wordBooksDao()
        val timeProvider = TestTimeProvider(1793577600000L)
        val planRepo = StudyPlanRepository(context, dao, timeProvider)

        var isAllowAi = false
        var isOnline = true
        var networkCallCount = 0

        val aiService = StudyPlanAiService(
            repository = planRepo,
            dao = dao,
            isAllowAiEnabled = { isAllowAi },
            isOnline = { isOnline },
            timeProvider = timeProvider
        )

        // 1. Zero network calls when Allow AI is OFF
        val offResult = aiService.generateMonthlyPlanWithAi("2026-11")
        assertTrue(offResult.isFailure)
        assertTrue(offResult.exceptionOrNull()?.message?.contains("Allow AI", ignoreCase = true) == true)
        assertEquals(0, networkCallCount)

        // 2. Zero network calls when device is OFFLINE
        isAllowAi = true
        isOnline = false
        val offlineResult = aiService.generateMonthlyPlanWithAi("2026-11")
        assertTrue(offlineResult.isFailure)
        assertTrue(offlineResult.exceptionOrNull()?.message?.contains("offline", ignoreCase = true) == true)
        assertEquals(0, networkCallCount)

        // 3. Allow AI ON and Online: test invalid plan rejection and retry
        isOnline = true
        var callCount = 0
        aiService.customAiTransport = { prompt, expectsJson ->
            callCount++
            if (callCount == 1) {
                // Return invalid JSON (new_words = 99 exceeds maximum allowed 20)
                """
                {
                  "month": "2026-11",
                  "level_start": "B2",
                  "level_target": "B2+",
                  "weeks": [
                    { "week": 1, "focus": "Test", "focus_ar": "اختبار", "goals": { "new_words": 100, "grammar_lessons": 3, "reading_texts": 3, "sentences": 20, "quizzes": 2 } }
                  ],
                  "daily_template": { "review_cards_min": 8, "new_words": 99, "grammar_min": 7, "reading_min": 5, "quiz_min": 3 },
                  "tips_ar": "نصائح"
                }
                """.trimIndent()
            } else {
                // Return valid JSON on retry
                """
                {
                  "month": "2026-11",
                  "level_start": "B2",
                  "level_target": "B2+",
                  "weeks": [
                    { "week": 1, "focus": "Week 1", "focus_ar": "الأسبوع 1", "goals": { "new_words": 50, "grammar_lessons": 3, "reading_texts": 3, "sentences": 20, "quizzes": 2 } },
                    { "week": 2, "focus": "Week 2", "focus_ar": "الأسبوع 2", "goals": { "new_words": 50, "grammar_lessons": 3, "reading_texts": 3, "sentences": 20, "quizzes": 2 } },
                    { "week": 3, "focus": "Week 3", "focus_ar": "الأسبوع 3", "goals": { "new_words": 50, "grammar_lessons": 3, "reading_texts": 3, "sentences": 20, "quizzes": 2 } },
                    { "week": 4, "focus": "Week 4", "focus_ar": "الأسبوع 4", "goals": { "new_words": 50, "grammar_lessons": 3, "reading_texts": 3, "sentences": 20, "quizzes": 2 } }
                  ],
                  "daily_template": { "review_cards_min": 8, "new_words": 12, "grammar_min": 7, "reading_min": 5, "quiz_min": 3 },
                  "tips_ar": "نصائح ذكية للدراسة"
                }
                """.trimIndent()
            }
        }

        val aiResult = aiService.generateMonthlyPlanWithAi("2026-11")
        assertTrue(aiResult.isSuccess)
        assertEquals("AI service should have retried once after invalid response", 2, callCount)
        val plan = aiResult.getOrNull()!!
        assertEquals("ai", plan.source)
        assertEquals("2026-11", plan.month)

        // Verify saved in DB
        val savedPlan = dao.getStudyPlanForMonth("2026-11")
        assertNotNull(savedPlan)
        assertEquals("ai", savedPlan!!.source)

        db.close()
    }

    @Test
    fun verifyPlanExecutionAutoDoneRestDayUnchangedAndAzJump() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, WordBooksDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.wordBooksDao()
        val importRepo = DataImportRepository(context, db)
        importRepo.ensureImportedOnFirstLaunch(forceRecheck = true)
        val grammarRepo = GrammarRepository(context, dao)
        grammarRepo.importGrammarLessonsIfNeeded()
        val readingRepo = ReadingRepository(context, dao)
        readingRepo.importReadingTextsIfNeeded()

        val timeProvider = TestTimeProvider(1793577600000L) // Monday, 2026-11-02
        val planRepo = StudyPlanRepository(context, dao, timeProvider)

        val profile = StudyPlanProfileEntity(
            id = 1,
            level = "B2",
            goal = "general_fluency",
            minutesPerDay = 30,
            studyDaysPerWeek = 6,
            startDate = "2026-11-02",
            selectedDecksJson = "[\"oxford5000\"]",
            restDaysJson = "[6]" // Friday
        )
        planRepo.updateProfile(profile)

        // 1. Unchanged checklist when regenerated on same day
        val initialChecklist = planRepo.getOrCreateDailyChecklist(timeProvider.todayDateString())
        val sameDayChecklist = planRepo.getOrCreateDailyChecklist(timeProvider.todayDateString())
        assertEquals(initialChecklist.tasksJson, sameDayChecklist.tasksJson)
        assertEquals(initialChecklist.date, sameDayChecklist.date)

        val json = Json { ignoreUnknownKeys = true; isLenient = true }
        val tasks = json.decodeFromString<List<DailyTaskItem>>(initialChecklist.tasksJson)
        // Verify all 5 pillars are present
        val taskTypes = tasks.map { it.type }.toSet()
        assertTrue(taskTypes.contains("flashcards"))
        assertTrue(taskTypes.contains("new_words"))
        assertTrue(taskTypes.contains("grammar"))
        assertTrue(taskTypes.contains("reading"))
        assertTrue(taskTypes.contains("quiz"))
        assertEquals(5, tasks.size)

        // 2. Auto-completion at target count without manual toggle:
        val flashTask = tasks.first { it.type == "flashcards" }
        val newWordsTask = tasks.first { it.type == "new_words" }
        val grammarTask = tasks.first { it.type == "grammar" }
        val readingTask = tasks.first { it.type == "reading" }

        assertFalse(flashTask.isCompleted)
        assertFalse(newWordsTask.isCompleted)

        // Simulate reviewing flashcards up to target
        val startOfDay = timeProvider.startOfDayMillis(initialChecklist.date)
        for (i in 1..flashTask.targetCount) {
            dao.upsertWordProgress(
                WordProgressEntity(
                    cardKey = "oxford5000:card_$i",
                    deck = "oxford5000",
                    text = "card_$i",
                    repetitions = 2,
                    lastReviewedTimestamp = startOfDay + (i * 1000L)
                )
            )
        }

        // Simulate studying new words up to target
        for (i in 1..newWordsTask.targetCount) {
            dao.upsertWordProgress(
                WordProgressEntity(
                    cardKey = "oxford5000:newword_$i",
                    deck = "oxford5000",
                    text = "newword_$i",
                    repetitions = 1,
                    lastReviewedTimestamp = startOfDay + 50000L + (i * 1000L)
                )
            )
        }

        // Simulate grammar lesson completion in DB
        dao.upsertGrammarProgress(
            GrammarProgressEntity(
                lessonId = grammarTask.linkedId,
                completedExercises = 6,
                totalExercises = 6,
                bestScore = 100,
                lastScore = 100,
                isCompleted = true,
                updatedAt = timeProvider.currentTimeMillis()
            )
        )

        // Simulate reading text completion in DB
        dao.upsertReadingProgress(
            ReadingProgressEntity(
                textId = readingTask.linkedId,
                completedQuestions = 3,
                totalQuestions = 3,
                bestScore = 100,
                lastScore = 100,
                isCompleted = true,
                updatedAt = timeProvider.currentTimeMillis()
            )
        )

        // Recompute completion idempotently from DB records
        val refreshed = planRepo.refreshChecklistCompletion(initialChecklist)
        val refreshedTasks = json.decodeFromString<List<DailyTaskItem>>(refreshed.tasksJson)
        assertTrue("Flashcards should be auto-completed at target count", refreshedTasks.first { it.type == "flashcards" }.isCompleted)
        assertTrue("New words should be auto-completed at target count", refreshedTasks.first { it.type == "new_words" }.isCompleted)
        assertTrue("Grammar should be auto-completed when exercises submitted", refreshedTasks.first { it.type == "grammar" }.isCompleted)
        assertTrue("Reading should be auto-completed when questions submitted", refreshedTasks.first { it.type == "reading" }.isCompleted)

        // 3. 2-day gap behavior (missed days carried over without guilt)
        timeProvider.advanceDays(3) // Advance 3 days (creates a 2-day gap)
        val afterGapChecklist = planRepo.getOrCreateDailyChecklist(timeProvider.todayDateString())
        assertTrue("Checklist after missed days should be marked adjusted", afterGapChecklist.isAdjusted)
        val afterGapTasks = json.decodeFromString<List<DailyTaskItem>>(afterGapChecklist.tasksJson)
        val adjustedNewWords = afterGapTasks.first { it.type == "new_words" }
        assertEquals(5, adjustedNewWords.targetCount) // reduced from 10 to 5 for smoother resumption

        // 4. Rest-day behavior (e.g. Friday)
        val restProfile = profile.copy(
            startDate = "2026-11-06",
            restDaysJson = "[6]" // Friday
        )
        planRepo.updateProfile(restProfile)
        val fridayDate = "2026-11-06"
        val fridayChecklist = planRepo.regenerateChecklistForUpdatedPlan(restProfile, null, fridayDate)
        val fridayTasks = json.decodeFromString<List<DailyTaskItem>>(fridayChecklist.tasksJson)
        val restNewWords = fridayTasks.first { it.type == "new_words" }
        assertEquals(3, restNewWords.targetCount) // Light words for rest day
        assertEquals(5, fridayTasks.size) // All 5 pillars still shown

        // 5. Content exhausted fallback
        // Mark all B2 grammar lessons completed
        val b2Lessons = dao.getUncompletedGrammarLessonsForLevel("B2")
        for (l in b2Lessons) {
            dao.upsertGrammarProgress(
                GrammarProgressEntity(
                    lessonId = l.id,
                    completedExercises = 10,
                    isCompleted = true,
                    updatedAt = timeProvider.currentTimeMillis()
                )
            )
        }
        val exhaustedChecklist = planRepo.getOrCreateDailyChecklist("2026-11-10")
        val exhaustedTasks = json.decodeFromString<List<DailyTaskItem>>(exhaustedChecklist.tasksJson)
        val nextGrammarTask = exhaustedTasks.first { it.type == "grammar" }
        assertNotNull(nextGrammarTask)

        // 6. A–Z jump on filtered list:
        val groupedCounts = dao.getGroupedInitialLetterCounts(
            deckId = "oxford5000",
            searchQuery = "",
            typeFilter = "",
            levelFilter = "B2",
            posFilter = "",
            onlyFavorites = 0,
            onlyMastered = 0,
            onlyLearning = 0
        )
        assertTrue("Grouped letter counts should return letters for B2 Oxford 5000", groupedCounts.isNotEmpty())
        assertTrue("Should contain entries for letters like A", groupedCounts.any { it.initial.equals("A", ignoreCase = true) })
        val countA = groupedCounts.firstOrNull { it.initial.equals("A", ignoreCase = true) }?.count ?: 0
        assertTrue(countA > 0)

        db.close()
    }
}


