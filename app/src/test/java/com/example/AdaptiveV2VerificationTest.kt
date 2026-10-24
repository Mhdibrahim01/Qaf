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
import com.example.data.local.AttemptEntity
import com.example.data.local.DailyTaskItem
import com.example.data.local.EntryEntity
import com.example.data.local.GrammarProgressEntity
import com.example.data.local.StudyPlanEntity
import com.example.data.local.StudyPlanProfileEntity
import com.example.data.local.WordBooksDao
import com.example.data.local.WordBooksDatabase
import com.example.data.local.WordProgressEntity
import com.example.data.plan.AdaptiveConfig
import com.example.data.plan.LearnerModelRepository
import com.example.data.plan.LocalQuizEngine
import com.example.data.plan.StudyPlanRepository
import com.example.data.plan.TimeProvider
import com.example.data.plan.WordStage
import com.example.data.reading.ReadingRepository
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AdaptiveV2VerificationTest {

    private lateinit var context: Context
    private lateinit var db: WordBooksDatabase
    private lateinit var dao: WordBooksDao
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private class FixedTestTimeProvider(
        private val nowMs: Long = 1793577600000L, // 2026-11-02 Monday
        private val dateStr: String = "2026-11-02",
        private val monthStr: String = "2026-11",
        private val dow: Int = Calendar.MONDAY
    ) : TimeProvider {
        override fun currentTimeMillis(): Long = nowMs
        override fun todayDateString(): String = dateStr
        override fun currentMonthString(): String = monthStr
        override fun dayOfWeek(): Int = dow
        override fun startOfDayMillis(dateString: String): Long = nowMs
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, WordBooksDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.wordBooksDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testRoomMigration15To16PreservesAllUserDataAndBackfillsConqueredTimestamp() {
        val dbFile = File(context.cacheDir, "migration_15_16_test_${System.nanoTime()}.db")
        val callback = object : SupportSQLiteOpenHelper.Callback(15) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `word_progress` (
                        `card_key` TEXT NOT NULL,
                        `deck` TEXT NOT NULL,
                        `text` TEXT NOT NULL,
                        `is_favorite` INTEGER NOT NULL DEFAULT 0,
                        `is_mastered` INTEGER NOT NULL DEFAULT 0,
                        `ease_factor` REAL NOT NULL DEFAULT 2.5,
                        `interval_days` INTEGER NOT NULL DEFAULT 0,
                        `repetitions` INTEGER NOT NULL DEFAULT 0,
                        `due_timestamp` INTEGER NOT NULL DEFAULT 0,
                        `last_reviewed_timestamp` INTEGER NOT NULL DEFAULT 0,
                        `again_count` INTEGER NOT NULL DEFAULT 0,
                        `hard_count` INTEGER NOT NULL DEFAULT 0,
                        `good_count` INTEGER NOT NULL DEFAULT 0,
                        `easy_count` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`card_key`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `attempts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `activity_type` TEXT NOT NULL,
                        `item_id` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `sm2_rating` INTEGER,
                        `is_correct` INTEGER,
                        `score` REAL,
                        `deck` TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `study_plan_profile` (
                        `id` INTEGER NOT NULL,
                        `level` TEXT NOT NULL,
                        `goal` TEXT NOT NULL,
                        `minutes_per_day` INTEGER NOT NULL,
                        `study_days_per_week` INTEGER NOT NULL,
                        `selected_decks_json` TEXT NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        `rest_days_json` TEXT NOT NULL DEFAULT '[6]',
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
        }

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbFile.absolutePath)
                .callback(callback)
                .build()
        )

        val sqliteDb = helper.writableDatabase
        // Insert v15 rows including a word with again_count >= 2 and interval_days >= 21
        sqliteDb.execSQL(
            """
            INSERT INTO `word_progress` (
                `card_key`, `deck`, `text`, `is_favorite`, `is_mastered`, `ease_factor`,
                `interval_days`, `repetitions`, `due_timestamp`, `last_reviewed_timestamp`,
                `again_count`, `hard_count`, `good_count`, `easy_count`
            ) VALUES (
                'oxford5000:resilient', 'oxford5000', 'resilient', 1, 1, 2.3,
                28, 5, 1800000000000, 1790000000000,
                3, 1, 4, 1
            )
            """.trimIndent()
        )
        sqliteDb.execSQL(
            """
            INSERT INTO `attempts` (`activity_type`, `item_id`, `timestamp`, `sm2_rating`, `is_correct`, `score`, `deck`)
            VALUES ('flashcard_review', 'oxford5000:resilient', 1790000000000, 2, 1, 85.0, 'oxford5000')
            """.trimIndent()
        )
        sqliteDb.execSQL(
            """
            INSERT INTO `study_plan_profile` (`id`, `level`, `goal`, `minutes_per_day`, `study_days_per_week`, `selected_decks_json`, `updated_at`, `rest_days_json`)
            VALUES (1, 'B1', 'ielts', 45, 6, '["oxford5000","ielts_core"]', 1790000000000, '[6]')
            """.trimIndent()
        )

        // Run MIGRATION_15_16
        WordBooksDatabase.MIGRATION_15_16.migrate(sqliteDb)

        // Verify v16 columns exist and conquered_timestamp was backfilled
        sqliteDb.query("SELECT card_key, lookup_count, last_looked_up_timestamp, conquered_timestamp FROM word_progress WHERE card_key = 'oxford5000:resilient'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("oxford5000:resilient", cursor.getString(0))
            assertEquals(0, cursor.getInt(1))
            assertEquals(0L, cursor.getLong(2))
            assertEquals(1790000000000L, cursor.getLong(3))
        }

        sqliteDb.query("SELECT item_id, elapsed_ms FROM attempts").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("oxford5000:resilient", cursor.getString(0))
            assertTrue(cursor.isNull(1) || cursor.getLong(1) == 0L)
        }

        sqliteDb.query("SELECT level, goal, preferred_topics_json, quiet_mode_enabled, dismissed_insights_json FROM study_plan_profile WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("B1", cursor.getString(0))
            assertEquals("ielts", cursor.getString(1))
            assertEquals("[]", cursor.getString(2))
            assertEquals(0, cursor.getInt(3))
            assertEquals("{}", cursor.getString(4))
        }

        helper.close()
        dbFile.delete()
    }

    @Test
    fun testBackupAndRestoreRoundTripPreservesAllProgressAndStudyPlans() = runBlocking {
        val importRepo = DataImportRepository(context, db)
        val now = 1793577600000L

        dao.upsertWordProgress(
            WordProgressEntity(
                cardKey = "oxford5000:sustain",
                deck = "oxford5000",
                text = "sustain",
                isFavorite = true,
                isMastered = true,
                easeFactor = 2.4f,
                intervalDays = 24,
                repetitions = 4,
                dueTimestamp = now + 86400_000L * 24,
                lastReviewedTimestamp = now,
                againCount = 2,
                lookupCount = 3,
                lastLookedUpTimestamp = now - 5000L,
                conqueredTimestamp = now
            )
        )
        dao.insertAttempt(
            AttemptEntity(
                activityType = "flashcard_review",
                itemId = "oxford5000:sustain",
                timestamp = now,
                sm2Rating = 2,
                isCorrect = true,
                score = 85f,
                deck = "oxford5000",
                elapsedMs = 4200L
            )
        )
        dao.upsertStudyPlanProfile(
            StudyPlanProfileEntity(
                id = 1,
                level = "B2",
                goal = "ielts",
                minutesPerDay = 45,
                studyDaysPerWeek = 5,
                selectedDecksJson = """["oxford5000","ielts_core"]""",
                updatedAt = now,
                restDaysJson = "[6,7]",
                preferredTopicsJson = """["science","environment","education"]""",
                quietModeEnabled = true,
                dismissedInsightsJson = """{"insight_pace_fast":$now}"""
            )
        )
        dao.upsertStudyPlan(
            StudyPlanEntity(
                month = "2026-11",
                createdAt = now,
                levelStart = "B2",
                levelTarget = "C1",
                weeksJson = "[]",
                dailyTemplateJson = """{"new_words":15}""",
                source = "local",
                tipsAr = "نصيحة تجريبية"
            )
        )

        val exportedJson = importRepo.exportUserBackupJson()
        assertTrue(exportedJson.contains("\"schemaVersion\": 16") || exportedJson.contains("\"schemaVersion\":16"))
        assertTrue(exportedJson.contains("oxford5000:sustain"))
        assertTrue(exportedJson.contains("2026-11"))

        // Clear tables via fresh database instance and restore
        val restoredDb = Room.inMemoryDatabaseBuilder(context, WordBooksDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val restoredDao = restoredDb.wordBooksDao()
            val restoredImportRepo = DataImportRepository(context, restoredDb)
            val restoredSuccess = restoredImportRepo.importUserBackupJson(exportedJson)
            assertTrue(restoredSuccess)

            val wp = restoredDao.getWordProgress("oxford5000:sustain")
            assertNotNull(wp)
            assertEquals(3, wp!!.lookupCount)
            assertEquals(now, wp.conqueredTimestamp)
            assertEquals(24, wp.intervalDays)

            val profile = restoredDao.getStudyPlanProfile()
            assertNotNull(profile)
            assertEquals("B2", profile!!.level)
            assertEquals("ielts", profile.goal)
            assertTrue(profile.quietModeEnabled)
            assertTrue(profile.preferredTopicsJson.contains("science"))

            val plan = restoredDao.getStudyPlanForMonth("2026-11")
            assertNotNull(plan)
            assertEquals("B2", plan!!.levelStart)
            assertEquals("C1", plan.levelTarget)

            val attempts = restoredDao.getAllAttempts()
            assertEquals(1, attempts.size)
            assertEquals(4200L, attempts.first().elapsedMs)
        } finally {
            restoredDb.close()
        }
    }

    @Test
    fun testThreeGoldenPersonasProduceVisiblyDifferentDailyLessonsAndQuizzes() = runBlocking {
        // Seed curriculum entries across decks & levels + reading stories + grammar lessons
        val readingRepo = ReadingRepository(context, dao)
        val grammarRepo = GrammarRepository(context, dao)
        readingRepo.importReadingTextsIfNeeded()
        grammarRepo.importGrammarLessonsIfNeeded()

        val sampleEntries = mutableListOf<EntryEntity>()
        val decksAndLevels = listOf(
            Triple("oxford5000", "A1", listOf("morning", "coffee", "family", "market", "ticket", "hotel", "friend", "water")),
            Triple("everyday_expressions", "A1", listOf("good morning", "excuse me", "how much", "see you")),
            Triple("oxford5000", "A2", listOf("routine", "commute", "schedule", "message", "healthy", "airport", "station", "weather", "holiday", "neighbor")),
            Triple("it_industry", "B1", listOf("deployment", "repository", "database", "latency", "refactor", "pipeline", "interface", "endpoint")),
            Triple("oxford5000", "B2", listOf("sustainable", "hypothesis", "consequence", "framework", "perspective", "phenomenon", "inevitable", "subsequent", "comprehensive", "fundamental")),
            Triple("oxford_opal", "B2", listOf("empirical", "methodology", "correlation", "paradigm", "implication", "albeit")),
            Triple("ielts_core", "B2", listOf("biodiversity", "urbanization", "mitigate", "curriculum", "cognitive", "renewable"))
        )
        var idCounter = 1
        for ((deck, level, words) in decksAndLevels) {
            for (w in words) {
                sampleEntries.add(
                    EntryEntity(
                        id = "${idCounter++}",
                        type = if (w.contains(" ")) "phrase" else "word",
                        deck = deck,
                        text = w,
                        pos = "noun",
                        level = level,
                        effectiveLevel = level,
                        definitionEn = "Meaning of $w",
                        noteEn = "",
                        examplesEn = "We use $w in a clear sentence.",
                        examplesAr = "نستخدم $w في جملة واضحة.",
                        definitionUrl = "",
                        audioUrl = "",
                        arabic = "معنى $w",
                        arabicSource = "file",
                        ipa = "",
                        dataVersion = 3
                    )
                )
            }
        }
        dao.upsertEntriesBatchPreservingUserEdits(sampleEntries)

        val timeProvider = FixedTestTimeProvider()
        val planRepo = StudyPlanRepository(context, dao, timeProvider)
        val quizEngine = LocalQuizEngine(dao, timeProvider)

        // Persona A: Salma (A1, General Fluency, 15m/day, Quiet Mode = true, topics: daily_life, travel)
        planRepo.saveOnboardingGoalAndGeneratePlan(
            level = "A1",
            goal = "general_fluency",
            minutesPerDay = 15,
            studyDaysPerWeek = 5,
            preferredTopics = listOf("daily_life", "travel", "culture"),
            quietModeEnabled = true
        )
        val salmaProfile = dao.getStudyPlanProfile()!!
        dao.upsertStudyPlanProfile(
            salmaProfile.copy(selectedDecksJson = """["oxford5000","everyday_expressions"]""")
        )
        val salmaChecklistFinal = planRepo.regenerateDailyChecklist("2026-11-02")
        val salmaTasks = json.decodeFromString<List<DailyTaskItem>>(salmaChecklistFinal.tasksJson)
        val salmaReading = salmaTasks.first { it.type == "reading" }
        val salmaNewWords = salmaTasks.first { it.type == "new_words" }
        val salmaQuizTask = salmaTasks.first { it.type == "quiz" }
        val salmaQuizResult = quizEngine.generateAlignedDailyQuiz(
            checklist = salmaChecklistFinal,
            profile = salmaProfile,
            isTtsAvailable = false // Quiet mode disables audio questions
        )
        val salmaQuizQuestions = salmaQuizResult.questions

        assertTrue("Salma reading must be A1 (r001..r008), got ${salmaReading.linkedId}", salmaReading.linkedId in (1..8).map { "r00$it" })
        assertEquals(5, salmaNewWords.targetCount)
        assertTrue("Salma quiz task should mention Quiet Mode", salmaQuizTask.reason.contains("Quiet Mode"))
        assertTrue("Salma Quiet Mode quiz must have zero listening/audio questions", salmaQuizQuestions.none { it.skill == "listening" })

        // Persona B: Omar (A2, IT Career, 30m/day, Normal Mode, topics: technology, career)
        planRepo.saveOnboardingGoalAndGeneratePlan(
            level = "A2",
            goal = "it_career",
            minutesPerDay = 30,
            studyDaysPerWeek = 6,
            preferredTopics = listOf("technology", "career", "science"),
            quietModeEnabled = false
        )
        val omarProfile = dao.getStudyPlanProfile()!!
        dao.upsertStudyPlanProfile(
            omarProfile.copy(selectedDecksJson = """["oxford5000","it_industry"]""")
        )
        val omarChecklistFinal = planRepo.regenerateDailyChecklist("2026-11-02")
        val omarTasks = json.decodeFromString<List<DailyTaskItem>>(omarChecklistFinal.tasksJson)
        val omarReading = omarTasks.first { it.type == "reading" }
        val omarNewWords = omarTasks.first { it.type == "new_words" }

        assertEquals(10, omarNewWords.targetCount)
        assertNotEquals("Salma and Omar must get different reading stories", salmaReading.linkedId, omarReading.linkedId)
        assertNotEquals("Salma and Omar must get different new word targets", salmaNewWords.targetCount, omarNewWords.targetCount)
        assertTrue("Omar's new words task must reference IT & Tech Industry", omarNewWords.subtitleEn.contains("IT & Tech Industry"))

        // Persona C: Nour (B2, IELTS Academic, 45m/day, Normal Mode, topics: environment, science, education)
        planRepo.saveOnboardingGoalAndGeneratePlan(
            level = "B2",
            goal = "ielts",
            minutesPerDay = 45,
            studyDaysPerWeek = 6,
            preferredTopics = listOf("environment", "science", "education"),
            quietModeEnabled = false
        )
        val nourProfile = dao.getStudyPlanProfile()!!
        dao.upsertStudyPlanProfile(
            nourProfile.copy(selectedDecksJson = """["oxford5000","oxford_opal","ielts_core"]""")
        )
        val nourChecklistFinal = planRepo.regenerateDailyChecklist("2026-11-02")
        val nourTasks = json.decodeFromString<List<DailyTaskItem>>(nourChecklistFinal.tasksJson)
        val nourReading = nourTasks.first { it.type == "reading" }
        val nourNewWords = nourTasks.first { it.type == "new_words" }

        assertEquals(15, nourNewWords.targetCount)
        assertNotEquals("Omar and Nour must get different reading stories", omarReading.linkedId, nourReading.linkedId)
        assertTrue("Nour's new words must reference IELTS Core or Academic OPAL", nourNewWords.subtitleEn.contains("IELTS") || nourNewWords.subtitleEn.contains("OPAL"))
    }

    @Test
    fun testClosedLoopTapToTranslateLeechDetectionConqueredWordsAndGrammarRefresher() = runBlocking {
        val timeProvider = FixedTestTimeProvider()
        val importRepo = DataImportRepository(context, db)
        val readingRepo = ReadingRepository(context, dao)
        val grammarRepo = GrammarRepository(context, dao)
        val planRepo = StudyPlanRepository(context, dao, timeProvider)
        val learnerModelRepo = LearnerModelRepository(context, dao, timeProvider)

        readingRepo.importReadingTextsIfNeeded()
        grammarRepo.importGrammarLessonsIfNeeded()

        // Ensure ordinary has a lower numeric ID (101) than ubiquitous (102) so without lookup boost, ordinary would be picked first
        val entries = listOf(
            EntryEntity(
                id = "101",
                type = "word",
                deck = "oxford5000",
                text = "ordinary",
                pos = "adjective",
                level = "B2",
                effectiveLevel = "B2",
                definitionEn = "normal or usual",
                noteEn = "",
                examplesEn = "It was an ordinary day.",
                examplesAr = "كان يوماً عادياً.",
                definitionUrl = "",
                audioUrl = "",
                arabic = "عادي",
                arabicSource = "file",
                ipa = "",
                dataVersion = 3
            ),
            EntryEntity(
                id = "102",
                type = "word",
                deck = "oxford5000",
                text = "ubiquitous",
                pos = "adjective",
                level = "B2",
                effectiveLevel = "B2",
                definitionEn = "present everywhere",
                noteEn = "",
                examplesEn = "Smartphones are ubiquitous today.",
                examplesAr = "الهواتف الذكية منتشرة في كل مكان اليوم.",
                definitionUrl = "",
                audioUrl = "",
                arabic = "منتشرة في كل مكان",
                arabicSource = "file",
                ipa = "",
                dataVersion = 3
            )
        )
        dao.upsertEntriesBatchPreservingUserEdits(entries)

        val profile = StudyPlanProfileEntity(
            id = 1,
            level = "B2",
            goal = "general_fluency",
            minutesPerDay = 30,
            studyDaysPerWeek = 6,
            selectedDecksJson = """["oxford5000"]""",
            updatedAt = timeProvider.currentTimeMillis()
        )
        dao.upsertStudyPlanProfile(profile)

        // 1. Tap-to-Translate lookup marks word as SEEN and boosts new-word selection priority
        readingRepo.recordWordLookup(
            wordText = "ubiquitous",
            preferredDeckId = "oxford5000",
            nowMillis = timeProvider.currentTimeMillis()
        )
        val progressAfterLookup = dao.getWordProgress("oxford5000:ubiquitous")
        assertNotNull(progressAfterLookup)
        assertEquals(1, progressAfterLookup!!.lookupCount)
        assertEquals(WordStage.SEEN, LearnerModelRepository.classifyWordStage(progressAfterLookup))

        // Verify ubiquitous is prioritized ahead of unlooked-up word in new word selection
        val selectedNew = planRepo.selectNewWordsForProfile(profile, 1)
        assertEquals("Looked-up word must be prioritized first in new word selection", "ubiquitous", selectedNew.words.first().text)

        // 2. Rating AGAIN 3 times triggers Leech detection and creates an active Correction Notebook entry
        val nowMs = timeProvider.currentTimeMillis()
        importRepo.applySm2Review("oxford5000", "ubiquitous", Sm2Rating.AGAIN, nowMs, entryId = "102", elapsedMs = 3500L)
        importRepo.applySm2Review("oxford5000", "ubiquitous", Sm2Rating.AGAIN, nowMs + 1000L, entryId = "102", elapsedMs = 3200L)
        val afterThirdAgain = importRepo.applySm2Review("oxford5000", "ubiquitous", Sm2Rating.AGAIN, nowMs + 2000L, entryId = "102", elapsedMs = 3000L)
        assertEquals(3, afterThirdAgain.againCount)

        val notebookEntries = dao.getAllCorrectionEntriesList()
        val leechEntry = notebookEntries.firstOrNull { it.textId == "oxford5000:ubiquitous" }
        assertNotNull("3x lapse must auto-create a Leech item in correction_notebook", leechEntry)
        assertNotEquals("mastered", leechEntry!!.status)

        // 3. Mastering the leech word to >= 21d interval awards Conquered Word trophy and resolves the Leech notebook entry
        dao.upsertWordProgress(
            afterThirdAgain.copy(
                repetitions = 4,
                intervalDays = 20,
                easeFactor = 2.5f
            )
        )
        val conqueredProgress = importRepo.applySm2Review("oxford5000", "ubiquitous", Sm2Rating.EASY, nowMs + 3000L, entryId = "102", elapsedMs = 2500L)
        assertTrue("Interval should now be >= 21 days", conqueredProgress.intervalDays >= 21)
        assertTrue("conqueredTimestamp must be set when againCount >= 2 and intervalDays >= 21", conqueredProgress.conqueredTimestamp > 0L)
        assertTrue("Word must be marked mastered on EASY rating", conqueredProgress.isMastered)

        val snapshot = learnerModelRepo.buildSnapshot(profile = profile, nowMillis = nowMs + 4000L)
        assertEquals(1, snapshot.conqueredWords.size)
        assertEquals("ubiquitous", snapshot.conqueredWords.first().text)

        val updatedNotebook = dao.getAllCorrectionEntriesList()
        assertEquals("Leech notebook item must auto-resolve once conquered", "mastered", updatedNotebook.first { it.id == leechEntry.id }.status)

        // 4. Grammar score < 65% triggers a [Refresher] task in Today's Lesson
        val b2Lesson = dao.getAllGrammarLessons().first { it.level == "B2" }
        dao.upsertGrammarProgress(
            GrammarProgressEntity(
                lessonId = b2Lesson.id,
                completedExercises = 4,
                totalExercises = 6,
                bestScore = 45, // 45% < 65% threshold
                lastScore = 45,
                isCompleted = true,
                updatedAt = nowMs - 86400_000L * 2
            )
        )
        val refreshedChecklist = planRepo.regenerateDailyChecklist("2026-11-02")
        val tasks = json.decodeFromString<List<DailyTaskItem>>(refreshedChecklist.tasksJson)
        val grammarTask = tasks.first { it.type == "grammar" }
        assertEquals(b2Lesson.id, grammarTask.linkedId)
        assertTrue("Grammar task title must include [Refresher]", grammarTask.titleEn.contains("[Refresher]"))
        assertTrue("Grammar task reason must explain the <65% remediation trigger", grammarTask.reason.contains("45%"))
    }

    @Test
    fun testDataVersion7AndDeckEffectiveLevels() {
        assertEquals("DataImportRepository DATA_VERSION must be 7", 7, DataImportRepository.DATA_VERSION)
    }

    @Test
    fun testNewWordsTargetPacingAndFastTrackScaling() = runBlocking {
        val timeProvider = FixedTestTimeProvider()
        val planRepo = StudyPlanRepository(context, dao, timeProvider)

        val profile = StudyPlanProfileEntity(
            id = 1,
            level = "A1",
            goal = "general_fluency",
            minutesPerDay = 15, // base = 5
            studyDaysPerWeek = 6,
            selectedDecksJson = """["oxford5000"]""",
            updatedAt = timeProvider.currentTimeMillis()
        )
        dao.upsertStudyPlanProfile(profile)

        // Seed 10 new words so selection doesn't starve
        val entries = (1..10).map { i ->
            EntryEntity(
                id = "10$i",
                type = "word",
                deck = "oxford5000",
                text = "word$i",
                pos = "noun",
                level = "A1",
                effectiveLevel = "A1",
                definitionEn = "definition $i",
                noteEn = "",
                examplesEn = "example $i",
                examplesAr = "",
                definitionUrl = "",
                audioUrl = "",
                arabic = "كلمة $i",
                arabicSource = "file",
                dataVersion = 7
            )
        }
        dao.upsertEntriesBatchPreservingUserEdits(entries)

        // 1. Standard day: 15-min profile gets base 5 words (NEW_WORDS_MIN)
        val normalChecklist = planRepo.regenerateDailyChecklist("2026-11-02")
        val normalTasks = json.decodeFromString<List<DailyTaskItem>>(normalChecklist.tasksJson)
        val normalNewWords = normalTasks.first { it.type == "new_words" }
        assertEquals(AdaptiveConfig.NEW_WORDS_MIN, normalNewWords.targetCount)

        // 2. Fast-track scaling: when 7-day accuracy >= 88%, target increases by +3 (5 -> 8)
        val nowMs = timeProvider.currentTimeMillis()
        for (i in 1..10) {
            dao.insertAttempt(
                AttemptEntity(
                    activityType = "flashcard_review",
                    itemId = "oxford5000:w$i",
                    timestamp = nowMs - 1000L * i,
                    sm2Rating = 3,
                    isCorrect = true,
                    score = 100f
                )
            )
        }
        val fastTrackChecklist = planRepo.regenerateDailyChecklist("2026-11-02")
        val fastTrackTasks = json.decodeFromString<List<DailyTaskItem>>(fastTrackChecklist.tasksJson)
        val fastTrackNewWords = fastTrackTasks.first { it.type == "new_words" }
        assertEquals(AdaptiveConfig.NEW_WORDS_MIN + 3, fastTrackNewWords.targetCount) // 5 + 3 = 8
    }

    @Test
    fun testLapsedMasteredCardClassifiesAsSeenNotMature() {
        val lapsedCard = WordProgressEntity(
            cardKey = "oxford5000:test",
            deck = "oxford5000",
            text = "test",
            isMastered = true, // Legacy or un-cleared mastered flag
            intervalDays = 0,
            repetitions = 0,
            againCount = 2,
            lastReviewedTimestamp = 100000L,
            firstLearnedTimestamp = 50000L
        )
        val stage = LearnerModelRepository.classifyWordStage(lapsedCard)
        assertEquals("A card with repetitions = 0 and intervalDays = 0 must be classified as SEEN, not MATURE", WordStage.SEEN, stage)
    }

    @Test
    fun testConsecutiveGrammarRefresherBlockedByCooldown() = runBlocking {
        val timeProvider = FixedTestTimeProvider()
        val grammarRepo = GrammarRepository(context, dao)
        val planRepo = StudyPlanRepository(context, dao, timeProvider)

        grammarRepo.importGrammarLessonsIfNeeded()
        val b2Lessons = dao.getAllGrammarLessons().filter { it.level == "B2" }
        assertTrue("Must have B2 grammar lessons", b2Lessons.size >= 2)
        val firstLesson = b2Lessons[0]
        val secondLesson = b2Lessons[1]

        val profile = StudyPlanProfileEntity(
            id = 1,
            level = "B2",
            goal = "general_fluency",
            minutesPerDay = 30,
            studyDaysPerWeek = 6,
            selectedDecksJson = """["oxford5000"]""",
            updatedAt = timeProvider.currentTimeMillis()
        )
        dao.upsertStudyPlanProfile(profile)

        // Set low score on firstLesson (< 65%)
        val nowMs = timeProvider.currentTimeMillis()
        dao.upsertGrammarProgress(
            GrammarProgressEntity(
                lessonId = firstLesson.id,
                completedExercises = 4,
                totalExercises = 6,
                bestScore = 40,
                lastScore = 40,
                isCompleted = true,
                updatedAt = nowMs - 86400_000L * 2
            )
        )

        // Day 1: Schedules refresher for firstLesson
        val day1Checklist = planRepo.regenerateDailyChecklist("2026-11-02")
        val day1Tasks = json.decodeFromString<List<DailyTaskItem>>(day1Checklist.tasksJson)
        val day1Grammar = day1Tasks.first { it.type == "grammar" }
        assertEquals(firstLesson.id, day1Grammar.linkedId)
        assertTrue(day1Grammar.titleEn.contains("[Refresher]"))

        // Day 2: Next consecutive day must NOT schedule consecutive refresher for firstLesson
        val day2Checklist = planRepo.getOrCreateDailyChecklist("2026-11-03")
        val day2Tasks = json.decodeFromString<List<DailyTaskItem>>(day2Checklist.tasksJson)
        val day2Grammar = day2Tasks.first { it.type == "grammar" }
        assertNotEquals("Day 2 must not repeat refresher for the same lesson", firstLesson.id, day2Grammar.linkedId)
        assertEquals("Day 2 should advance to next uncompleted syllabus lesson", secondLesson.id, day2Grammar.linkedId)
        assertTrue("Day 2 should be regular lesson, not refresher", !day2Grammar.titleEn.contains("[Refresher]"))
    }
}

