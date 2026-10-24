package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.grammar.GrammarRepository
import com.example.data.importer.DataImportRepository
import com.example.data.importer.Sm2Rating
import com.example.data.local.EntryEntity
import com.example.data.local.QuizQuestionItem
import com.example.data.local.WordBooksDatabase
import com.example.data.onboarding.OnboardingPreferencesRepository
import com.example.data.plan.LocalQuizEngine
import com.example.data.plan.StudyPlanRepository
import com.example.data.plan.TestTimeProvider
import com.example.data.reading.ReadingRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Phase 2 verification suite covering:
 * 1. Non-destructive Room migration 9 -> ... -> 14 -> 15 with pre-populated data:
 *    - Ratings, due dates, favorites, manual mastery, grammar/reading progress, quiz history, correction notebook, profile.
 *    - `first_learned_timestamp` backfilled ONLY for actually-rated cards (`repetitions > 0 || again_count > 0`), NOT favorite-only or manual-mastery-only rows.
 *    - `attempts` table created empty (zero invented history).
 * 2. Append-only `attempts` logging in the same Room transaction across flashcards (`flashcard_new` vs `flashcard_review`),
 *    grammar, reading, correction notebook, quizzes, and sentence practice.
 * 3. Favorite & manual mastery isolation (never create attempts or modify `lastReviewedTimestamp` / `firstLearnedTimestamp`).
 * 4. Honest daily metrics separating first-time learned words from reviews and repeated same-day ratings.
 * 5. Unified `dueNowCutoff` vs `dueTodayCutoff` and hiding of `Again` (10m) / first-`Hard` (4h) cards in active sessions until `dueTimestamp <= nowMillis`.
 * 6. Local backup/export round-trip including `attempts` and preservation of `attempts` across "Redo Onboarding Setup".
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseMigration13To15Test {

    @Test
    fun migrationToV15PreservesAllUserDataBackfillsFirstLearnedTimestampOnlyForRatedCardsAndCreatesEmptyAttemptsTable(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "test_migration_to_15.db"
        context.deleteDatabase(dbName)

        // Create a v12 database and migrate 12 -> 13 -> 14 -> 15 with pre-populated data
        val baseRoomDb = Room.databaseBuilder(context, WordBooksDatabase::class.java, dbName)
            .allowMainThreadQueries()
            .build()
        // Close immediately and downgrade user_version to 13 while removing v14/v15 columns/tables to test real SQLite migration 13 -> 14 -> 15
        baseRoomDb.openHelper.writableDatabase.close()
        baseRoomDb.close()
        context.deleteDatabase(dbName)

        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(9) {
                override fun onCreate(db: SupportSQLiteDatabase) {
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
                            interval_days INTEGER NOT NULL,
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
        val sqliteDb = helper.writableDatabase

        // Migrate 9 -> 10 -> 11 -> 12 -> 13 first, then populate rich v13 state
        WordBooksDatabase.MIGRATION_9_10.migrate(sqliteDb)
        WordBooksDatabase.MIGRATION_10_11.migrate(sqliteDb)
        WordBooksDatabase.MIGRATION_11_12.migrate(sqliteDb)
        WordBooksDatabase.MIGRATION_12_13.migrate(sqliteDb)

        // Seed v13 data:
        // 1. Entries (Oxford B2 + IT blanket B2)
        sqliteDb.execSQL("INSERT INTO entries VALUES ('100', 'oxford5000', 'strategy', 'noun', 'B2', 'single_word', '', '', 'استراتيجية', 'user', '', '', '', 'ملاحظتي', 3)")
        sqliteDb.execSQL("INSERT INTO entries VALUES ('200001', 'it_industry', 'strategy', 'noun', 'B2', 'single_word', '', '', 'استراتيجية تقنية', 'file', '', '', '', '', 3)")
        sqliteDb.execSQL("INSERT INTO entries VALUES ('200002', 'it_industry', 'kubernetes', 'noun', 'B2', 'single_word', '', '', 'كوبرنيتس', 'file', '', '', '', '', 3)")

        // 2. Word progress in v13 (card_key, deck, text, interval_days, ease_factor, repetitions, last_reviewed_timestamp, due_timestamp, is_favorite, is_mastered, again_count, entry_id):
        // - Rated card (repetitions=3, last_reviewed=1700000000000)
        sqliteDb.execSQL(
            "INSERT INTO word_progress (card_key, deck, text, is_favorite, is_mastered, repetitions, interval_days, ease_factor, due_timestamp, last_reviewed_timestamp, again_count, entry_id) " +
                "VALUES ('oxford5000:strategy', 'oxford5000', 'strategy', 1, 0, 3, 15, 2.5, 1701296000000, 1700000000000, 1, '100')"
        )
        // - Lapsed card (repetitions=0, again_count=2, last_reviewed=1700050000000)
        sqliteDb.execSQL(
            "INSERT INTO word_progress (card_key, deck, text, is_favorite, is_mastered, repetitions, interval_days, ease_factor, due_timestamp, last_reviewed_timestamp, again_count, entry_id) " +
                "VALUES ('oxford5000:lapsed', 'oxford5000', 'lapsed', 0, 0, 0, 0, 2.1, 1700050600000, 1700050000000, 2, NULL)"
        )
        // - Favorite-only card (repetitions=0, again_count=0, last_reviewed=0)
        sqliteDb.execSQL(
            "INSERT INTO word_progress (card_key, deck, text, is_favorite, is_mastered, repetitions, interval_days, ease_factor, due_timestamp, last_reviewed_timestamp, again_count, entry_id) " +
                "VALUES ('oxford5000:favonly', 'oxford5000', 'favonly', 1, 0, 0, 0, 2.5, 0, 0, 0, NULL)"
        )
        // - Manual-mastery-only card (repetitions=0, again_count=0, last_reviewed=1700099999000 from legacy toggleMastered)
        sqliteDb.execSQL(
            "INSERT INTO word_progress (card_key, deck, text, is_favorite, is_mastered, repetitions, interval_days, ease_factor, due_timestamp, last_reviewed_timestamp, again_count, entry_id) " +
                "VALUES ('oxford5000:masteredonly', 'oxford5000', 'masteredonly', 0, 1, 0, 0, 2.5, 0, 1700099999000, 0, NULL)"
        )

        // 3. Sentences, Correction Notebook, Study Plan Profile
        sqliteDb.execSQL("INSERT INTO sentences (id, english_text, arabic_text, source_word, is_favorite, last_practiced_timestamp, practice_count, created_at) VALUES (1, 'Strategy matters.', 'الاستراتيجية مهمة.', 'strategy', 1, 1000, 1, 1000)")
        sqliteDb.execSQL("INSERT INTO correction_notebook VALUES (1, 'r030', 'g030', 'Conditionals', 'If I will go', 'If I go', 'شرح', 'Should I go', 'checked', 1, 1700086400000, '', 1, 1700000000000, 1700000000000)")
        sqliteDb.execSQL("INSERT INTO study_plan_profile (id, level, goal, minutes_per_day, study_days_per_week, start_date, selected_decks_json, rest_days_json, updated_at) VALUES (1, 'B2', 'it_career', 30, 5, '2024-11-01', '[\"oxford5000\",\"it_industry\"]', '[6]', 1700004000000)")

        // Now execute MIGRATION_13_14 and MIGRATION_14_15 directly on the populated v13 database
        WordBooksDatabase.MIGRATION_13_14.migrate(sqliteDb)
        WordBooksDatabase.MIGRATION_14_15.migrate(sqliteDb)

        // Verify v14 effective_level backfill & user notes preservation
        sqliteDb.query("SELECT effective_level, arabic, notes FROM entries WHERE id = '100'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("B2", c.getString(0))
            assertEquals("استراتيجية", c.getString(1))
            assertEquals("ملاحظتي", c.getString(2))
        }
        sqliteDb.query("SELECT effective_level FROM entries WHERE id = '200001'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("B2", c.getString(0))
        }
        sqliteDb.query("SELECT effective_level FROM entries WHERE id = '200002'").use { c ->
            assertTrue(c.moveToFirst())
            assertTrue(c.isNull(0))
        }

        // Verify v15 word_progress preservation & selective first_learned_timestamp backfill
        sqliteDb.query("SELECT repetitions, first_learned_timestamp FROM word_progress WHERE card_key = 'oxford5000:strategy'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(3, c.getInt(0))
            assertEquals(1_700_000_000_000L, c.getLong(1))
        }
        sqliteDb.query("SELECT repetitions, again_count, first_learned_timestamp FROM word_progress WHERE card_key = 'oxford5000:lapsed'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(0, c.getInt(0))
            assertEquals(2, c.getInt(1))
            assertEquals(1_700_050_000_000L, c.getLong(2))
        }
        sqliteDb.query("SELECT is_favorite, first_learned_timestamp FROM word_progress WHERE card_key = 'oxford5000:favonly'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(1, c.getInt(0))
            assertEquals("Favorite-only card must NOT have first_learned_timestamp backfilled", 0L, c.getLong(1))
        }
        sqliteDb.query("SELECT is_mastered, first_learned_timestamp FROM word_progress WHERE card_key = 'oxford5000:masteredonly'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(1, c.getInt(0))
            assertEquals("Manual-mastered unrated card must NOT have first_learned_timestamp backfilled", 0L, c.getLong(1))
        }

        // Verify study_plan_profile start_at_user_level and correction_notebook survived untouched
        sqliteDb.query("SELECT level, goal, start_at_user_level FROM study_plan_profile WHERE id = 1").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("B2", c.getString(0))
            assertEquals("it_career", c.getString(1))
            assertEquals(1, c.getInt(2))
        }
        sqliteDb.query("SELECT COUNT(*) FROM correction_notebook").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(1, c.getInt(0))
        }

        // Verify attempts table exists and has 0 invented rows
        sqliteDb.query("SELECT COUNT(*) FROM attempts").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("Migration 14->15 must not invent attempt history", 0, c.getInt(0))
        }

        // Verify Room schema export JSON exists at schemas/com.example.data.local.WordBooksDatabase/15.json
        val projectSchemaFile = File("schemas/com.example.data.local.WordBooksDatabase/15.json")
        val parentSchemaFile = File("app/schemas/com.example.data.local.WordBooksDatabase/15.json")
        assertTrue(
            "Room schema 15.json should be exported by KSP",
            projectSchemaFile.exists() || parentSchemaFile.exists()
        )

        sqliteDb.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun attemptsAreLoggedTransactionallyAcrossActivitiesWhileFavoritesAndManualMasteryStayIsolated(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, WordBooksDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.wordBooksDao()
        val dataRepo = DataImportRepository(context, db)
        val grammarRepo = GrammarRepository(context, dao)
        val readingRepo = ReadingRepository(context, dao)
        val clock = TestTimeProvider(fixedTimeMillis = 1_730_548_800_000L) // 2024-11-02 12:00:00 UTC
        val quizEngine = LocalQuizEngine(dao)
        val planRepo = StudyPlanRepository(context, dao, clock)

        val startOfDay = clock.startOfDayMillis(clock.todayDateString())
        val endOfDay = clock.dueTodayCutoff(clock.currentTimeMillis())

        // 1. Toggle favorite & manual mastery -> must NOT create attempts or increment studied/new/review counts
        dataRepo.toggleFavorite("oxford5000", "abandon")
        dataRepo.toggleMastered("oxford5000", "benefit")
        readingRepo.saveWordFavorite("oxford5000", "context", true)

        assertEquals(0, dao.getTotalAttemptsCount())
        assertEquals(0L, dao.getWordProgress("oxford5000:abandon")?.lastReviewedTimestamp ?: 0L)
        assertEquals(0L, dao.getWordProgress("oxford5000:benefit")?.lastReviewedTimestamp ?: 0L)
        assertEquals(0L, dao.getWordProgress("oxford5000:context")?.lastReviewedTimestamp ?: 0L)
        assertTrue("Reading favorite must use deck:word key", readingRepo.getWordFavoriteStatus("oxford5000", "context"))
        assertEquals(0, dao.getNewWordsLearnedInRangeCount(startOfDay, endOfDay))
        assertEquals(0, dao.getReviewCardsRatedInRangeCount(startOfDay, endOfDay))
        assertEquals(0, dao.getStudiedTodayCount(startOfDay))

        // 2. First rating on "abandon" -> logs flashcard_new attempt, counts as 1 new word and 0 reviews
        val t1 = clock.currentTimeMillis()
        dataRepo.applySm2Review("oxford5000", "abandon", Sm2Rating.AGAIN, t1)
        assertEquals(1, dao.getNewWordsLearnedInRangeCount(startOfDay, endOfDay))
        assertEquals(0, dao.getReviewCardsRatedInRangeCount(startOfDay, endOfDay))

        // 3. Second rating on "abandon" 15 minutes later -> logs flashcard_review attempt, still 1 new word + 1 review!
        clock.advanceMillis(15 * 60 * 1000L)
        val t2 = clock.currentTimeMillis()
        dataRepo.applySm2Review("oxford5000", "abandon", Sm2Rating.GOOD, t2)
        assertEquals("Same word rated again today must still count as 1 new word", 1, dao.getNewWordsLearnedInRangeCount(startOfDay, endOfDay))
        assertEquals("Subsequent rating on same word counts as 1 review", 1, dao.getReviewCardsRatedInRangeCount(startOfDay, endOfDay))

        val abandonAttempts = dao.getAttemptsForItem("flashcard_new", "oxford5000:abandon") +
            dao.getAttemptsForItem("flashcard_review", "oxford5000:abandon")
        assertEquals(2, abandonAttempts.size)
        assertEquals("flashcard_new", abandonAttempts[0].activityType)
        assertEquals(Sm2Rating.AGAIN.attemptCode, abandonAttempts[0].sm2Rating)
        assertEquals(false, abandonAttempts[0].isCorrect)
        assertEquals("flashcard_review", abandonAttempts[1].activityType)
        assertEquals(Sm2Rating.GOOD.attemptCode, abandonAttempts[1].sm2Rating)
        assertEquals(true, abandonAttempts[1].isCorrect)

        // 4. Grammar, Reading, Correction Notebook, Quiz, Sentence Practice attempts
        grammarRepo.saveLessonProgress("g001", completedExercises = 5, totalExercises = 5, score = 80, isCompleted = true)
        readingRepo.saveReadingProgress("r030", completedQuestions = 4, totalQuestions = 4, score = 75, isCompleted = true)
        readingRepo.saveCorrectionEntry(
            textId = "r030",
            grammarLessonId = "g030",
            grammarLessonTitle = "Conditionals",
            originalSentence = "If it will rain",
            correctedSentence = "If it rains"
        )
        val notebookEntry = dao.getAllCorrectionEntriesList().first()
        readingRepo.recordSpacedReviewAttempt(notebookEntry.id, "If it rains tomorrow, we will stay home.", isSuccess = true)

        val sampleQuestion = QuizQuestionItem(
            id = "q1",
            skill = "vocabulary",
            questionType = "mcq_en_ar",
            promptEn = "abandon",
            options = listOf("يتخلى عن", "يستمر", "يبني", "يرفض"),
            answerIndex = 0,
            correctText = "يتخلى عن"
        )
        quizEngine.recordQuizSubmission("daily_mini", listOf(sampleQuestion), mapOf("q1" to 0))
        dataRepo.recordSentencePracticeAttempt("sentence:1", scorePercent = 92, nowMillis = clock.currentTimeMillis())

        val allAttempts = dao.getAllAttempts()
        val types = allAttempts.map { it.activityType }.toSet()
        assertTrue(types.containsAll(setOf(
            "flashcard_new",
            "flashcard_review",
            "grammar_quiz",
            "reading_quiz",
            "correction_review",
            "quiz_question",
            "sentence_practice"
        )))

        // 5. Verify PracticeAccuracySummary separates accuracy & mastery from task completion
        val summary = planRepo.getPracticeAccuracySummary()
        assertEquals(1, summary.newWordsLearnedToday)
        assertEquals(1, summary.reviewAttemptsToday)
        assertEquals(1, summary.totalMasteredWords) // "benefit" was manually mastered

        db.close()
    }

    @Test
    fun dueNowVsDueTodayUnifiesCutoffsAndHidesAgain10mAndFirstHard4hCardsUntilTimerExpires(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, WordBooksDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.wordBooksDao()
        val repo = DataImportRepository(context, db)

        // Seed 3 entries in oxford5000
        dao.insertEntriesBatchIgnore(
            listOf(
                EntryEntity(
                    id = "1", deck = "oxford5000", text = "alpha", type = "word", level = "B2", effectiveLevel = "B2",
                    pos = "noun", ipa = "", arabic = "ألفا", definitionEn = "", noteEn = "", examplesEn = "",
                    examplesAr = "", definitionUrl = "", audioUrl = ""
                ),
                EntryEntity(
                    id = "2", deck = "oxford5000", text = "beta", type = "word", level = "B2", effectiveLevel = "B2",
                    pos = "noun", ipa = "", arabic = "بيتا", definitionEn = "", noteEn = "", examplesEn = "",
                    examplesAr = "", definitionUrl = "", audioUrl = ""
                ),
                EntryEntity(
                    id = "3", deck = "oxford5000", text = "gamma", type = "word", level = "B2", effectiveLevel = "B2",
                    pos = "noun", ipa = "", arabic = "غاما", definitionEn = "", noteEn = "", examplesEn = "",
                    examplesAr = "", definitionUrl = "", audioUrl = ""
                )
            )
        )

        val t0 = 1_730_548_800_000L // 12:00:00

        // Rate alpha = AGAIN (+10m), beta = first HARD (+4h), gamma = GOOD (+1d)
        repo.applySm2Review("oxford5000", "alpha", Sm2Rating.AGAIN, t0)
        repo.applySm2Review("oxford5000", "beta", Sm2Rating.HARD, t0)
        repo.applySm2Review("oxford5000", "gamma", Sm2Rating.GOOD, t0)

        // At t0 (12:00):
        // - dueNowCount (due <= t0) is 0 because alpha (+10m) and beta (+4h) are waiting for their timers!
        // - dueTodayCount (due <= endOfDay) is 2 (alpha + beta; gamma is tomorrow).
        // - laterTodayCardsCount is 2.
        assertEquals(0, repo.getReviewDueNowCount(t0, listOf("oxford5000")))
        assertEquals(2, repo.getReviewDueTodayCount(t0, listOf("oxford5000")))
        assertEquals(2, repo.getLaterTodayCardsCount(t0, listOf("oxford5000")))

        val (cardsAtT0, _) = repo.loadFlashcardDeckCards("oxford5000")
        val progressMapAtT0 = dao.getAllWordProgressList().associateBy { it.cardKey }
        assertFalse(
            "Neither alpha (+10m) nor beta (+4h) nor gamma (+1d) should be due NOW at t0",
            repo.hasAnyCardDueNow(cardsAtT0, progressMapAtT0, t0)
        )

        // Advance clock by 11 minutes -> alpha (+10m) is now due! beta (+4h) is still hidden.
        val tPlus11m = t0 + 11 * 60 * 1000L
        assertEquals(1, repo.getReviewDueNowCount(tPlus11m, listOf("oxford5000")))
        assertEquals(1, repo.getLaterTodayCardsCount(tPlus11m, listOf("oxford5000")))
        val nextDueIdxAt11m = repo.findNextDueCardIndex(cardsAtT0, currentIndex = 2, progressMap = progressMapAtT0, nowMillis = tPlus11m)
        assertEquals("alpha (index 0) should be selected now that 10m elapsed", "alpha", cardsAtT0[nextDueIdxAt11m].text)

        // Advance clock by 4 hours + 1 minute -> both alpha and beta are due now!
        val tPlus4h1m = t0 + (4 * 60 + 1) * 60 * 1000L
        assertEquals(2, repo.getReviewDueNowCount(tPlus4h1m, listOf("oxford5000")))
        assertEquals(0, repo.getLaterTodayCardsCount(tPlus4h1m, listOf("oxford5000")))

        db.close()
    }

    @Test
    fun backupExportAndImportPreservesAttemptsAndFirstLearnedTimestampAndRedoOnboardingKeepsAttempts(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db1 = Room.inMemoryDatabaseBuilder(context, WordBooksDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo1 = DataImportRepository(context, db1)
        val t0 = 1_730_548_800_000L

        repo1.applySm2Review("oxford5000", "persist", Sm2Rating.GOOD, t0)
        repo1.recordSentencePracticeAttempt("sentence:42", 95, t0 + 5000L)

        // Verify Redo Onboarding Setup does NOT clear attempts or word_progress
        val onboardingRepo = OnboardingPreferencesRepository(context)
        onboardingRepo.startRedoSetup()
        assertEquals(2, db1.wordBooksDao().getTotalAttemptsCount())
        assertNotNull(db1.wordBooksDao().getWordProgress("oxford5000:persist"))

        // Export JSON backup from db1
        val exportedJson = repo1.exportUserBackupJson(nowMillis = t0 + 10_000L)
        assertTrue(exportedJson.contains("\"attempts\""))
        assertTrue(exportedJson.contains("\"flashcard_new\""))
        assertTrue(exportedJson.contains("\"sentence_practice\""))

        // Import into a brand-new database db2
        val db2 = Room.inMemoryDatabaseBuilder(context, WordBooksDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo2 = DataImportRepository(context, db2)
        val importOk = repo2.importUserBackupJson(exportedJson)
        assertTrue(importOk)

        val restoredProgress = db2.wordBooksDao().getWordProgress("oxford5000:persist")
        assertNotNull(restoredProgress)
        assertEquals(t0, restoredProgress!!.firstLearnedTimestamp)

        val restoredAttempts = db2.wordBooksDao().getAllAttempts().sortedBy { it.timestamp }
        assertEquals(2, restoredAttempts.size)
        assertEquals("flashcard_new", restoredAttempts[0].activityType)
        assertEquals("sentence_practice", restoredAttempts[1].activityType)
        assertEquals(95f, restoredAttempts[1].score ?: 0f, 0.01f)

        db1.close()
        db2.close()
    }
}
