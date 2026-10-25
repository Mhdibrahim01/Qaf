package com.example

import com.example.data.local.GrammarLessonEntity
import com.example.data.onboarding.OnboardingPlacementQuiz
import com.example.data.onboarding.PlacementQuestionType

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
import com.example.data.local.DailyChecklistEntity
import com.example.data.local.DailyTaskItem
import com.example.data.local.EntryEntity
import com.example.data.local.GrammarProgressEntity
import com.example.data.local.ReadingProgressEntity
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
import org.junit.Assert.assertNull
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
        assertEquals("DataImportRepository DATA_VERSION must be 8", 8, DataImportRepository.DATA_VERSION)
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

    @Test
    fun testColdStartInsightDismissal() {
        runBlocking {
            val timeProvider = FixedTestTimeProvider()
            val learnerModelRepo = LearnerModelRepository(context, dao, timeProvider)
            val profile = StudyPlanProfileEntity(
                id = 1,
                level = "B2",
                selectedDecksJson = """["oxford5000"]"""
            )
            dao.upsertStudyPlanProfile(profile)

            // 1. Initial cold-start state: banner must be active
            val initialSnapshot = learnerModelRepo.buildSnapshot(profile, nowMillis = timeProvider.currentTimeMillis())
            assertTrue("Must be cold start when no attempts exist", initialSnapshot.isColdStart)
            assertNotNull("Cold start banner must be present initially", initialSnapshot.activeInsight)
            assertEquals("cold_start_calibrating", initialSnapshot.activeInsight?.id)

            // 2. Dismiss the banner
            val nowMs = timeProvider.currentTimeMillis()
            val updatedProfile = learnerModelRepo.dismissInsight("cold_start_calibrating", nowMs)

            // 3. Rebuild snapshot: banner must now be dismissed (null)
            val afterDismissSnapshot = learnerModelRepo.buildSnapshot(updatedProfile, nowMillis = nowMs)
            assertNull("Cold start banner must be null immediately after dismissal", afterDismissSnapshot.activeInsight)

            // 4. Verification from DAO directly
            val daoProfile = dao.getStudyPlanProfile()
            assertNotNull(daoProfile)
            val fromDaoSnapshot = learnerModelRepo.buildSnapshot(daoProfile, nowMillis = nowMs)
            assertNull("Cold start banner must stay dismissed when loaded from DAO", fromDaoSnapshot.activeInsight)
        }
    }

    @Test
    fun testGrammarAndReadingWithRawScoreCountInLevelReadiness() {
        runBlocking {
            val timeProvider = FixedTestTimeProvider()
            val grammarRepo = GrammarRepository(context, dao)
            val readingRepo = ReadingRepository(context, dao)
            grammarRepo.importGrammarLessonsIfNeeded()
            readingRepo.importReadingTextsIfNeeded()

            val learnerModelRepo = LearnerModelRepository(context, dao, timeProvider)
            val profile = StudyPlanProfileEntity(
                id = 1,
                level = "B1",
                selectedDecksJson = """["oxford5000"]"""
            )
            dao.upsertStudyPlanProfile(profile)

            // Find a B1 grammar lesson
            val b1Lessons = dao.getGrammarLessonsByLevel("B1")
            assertTrue(b1Lessons.isNotEmpty())
            val lesson = b1Lessons.first()

            // Save grammar progress using raw count (e.g. 5 out of 5 exercises correct)
            dao.upsertGrammarProgress(
                GrammarProgressEntity(
                    lessonId = lesson.id,
                    completedExercises = 5,
                    totalExercises = 5,
                    bestScore = 5, // Raw count: 5 out of 5 = 100%
                    lastScore = 5,
                    isCompleted = true,
                    updatedAt = timeProvider.currentTimeMillis()
                )
            )

            // Find a B1 reading story
            val b1Stories = dao.getAllReadingTexts().filter { it.level.equals("B1", ignoreCase = true) }
            assertTrue(b1Stories.isNotEmpty())
            val story = b1Stories.first()

            // Save reading progress using raw count (e.g. 3 out of 3 questions correct)
            dao.upsertReadingProgress(
                ReadingProgressEntity(
                    textId = story.id,
                    completedQuestions = 3,
                    totalQuestions = 3,
                    bestScore = 3, // Raw count: 3 out of 3 = 100%
                    lastScore = 3,
                    isCompleted = true,
                    updatedAt = timeProvider.currentTimeMillis()
                )
            )

            val snapshot = learnerModelRepo.buildSnapshot(profile, nowMillis = timeProvider.currentTimeMillis())
            assertTrue("Grammar passed count must be at least 1", snapshot.cefrReadiness.grammarPassedCount >= 1)
            assertTrue("Grammar mastery % must be greater than 0", snapshot.cefrReadiness.grammarMasteryPercent > 0)
            assertTrue("Reading completed count must be at least 1", snapshot.cefrReadiness.readingCompletedCount >= 1)
            assertTrue("Reading mastery % must be greater than 0", snapshot.cefrReadiness.readingMasteryPercent > 0)
        }
    }

    @Test
    fun testDueFlashcardsCompletionWhenAllDueCardsReviewed() {
        runBlocking {
            val timeProvider = FixedTestTimeProvider()
            val planRepo = StudyPlanRepository(context, dao, timeProvider)
            val now = timeProvider.currentTimeMillis()
            val startOfDay = timeProvider.startOfDayMillis(timeProvider.todayDateString())

            val profile = planRepo.getOrCreateProfile().copy(
                level = "B1",
                goal = "general_fluency",
                minutesPerDay = 30,
                selectedDecksJson = Json.encodeToString(listOf("oxford5000")),
                updatedAt = now
            )
            dao.upsertStudyPlanProfile(profile)

            // Setup a daily checklist where task_flashcards has target = 10 (default)
            val flashcardTask = DailyTaskItem(
                id = "task_flashcards",
                type = "flashcards",
                titleEn = "Review Due Flashcards",
                titleAr = "مراجعة البطاقات المستحقة (10 بطاقات)",
                targetCount = 10,
                completedCount = 0,
                isCompleted = false,
                linkedRoute = "flashcards",
                linkedId = "oxford5000"
            )
            val checklist = DailyChecklistEntity(
                date = timeProvider.todayDateString(),
                tasksJson = Json.encodeToString(listOf(flashcardTask)),
                completedCount = 0,
                totalCount = 1,
                isCompleted = false
            )
            dao.upsertDailyChecklist(checklist)

            // Simulate the user reviewing 3 cards today and clearing their due queue (due_timestamp tomorrow, so dueNow = 0)
            for (i in 1..3) {
                val cardKey = "oxford5000:word$i"
                val wp = WordProgressEntity(
                    cardKey = cardKey,
                    deck = "oxford5000",
                    text = "word$i",
                    repetitions = 1,
                    intervalDays = 1,
                    dueTimestamp = now + 86_400_000L, // Tomorrow (dueNow is 0)
                    lastReviewedTimestamp = now,
                    firstLearnedTimestamp = startOfDay - 86_400_000L // Learned previously
                )
                val attempt = AttemptEntity(
                    activityType = "flashcard_review",
                    itemId = cardKey,
                    timestamp = now,
                    isCorrect = true,
                    sm2Rating = 3,
                    score = 85f,
                    deck = "oxford5000"
                )
                dao.upsertWordProgressWithAttempt(wp, attempt)
            }

            // Verify due count is 0
            val dueNowCount = dao.getDueNowCardsCountForDecks(now, listOf("oxford5000"))
            assertEquals(0, dueNowCount)

            // Refresh checklist completion
            val updated = planRepo.refreshChecklistCompletion(checklist)
            val tasks = Json.decodeFromString<List<DailyTaskItem>>(updated.tasksJson)
            val refreshedFlashcardTask = tasks.first { it.id == "task_flashcards" }

            assertTrue("Flashcards task must be marked complete when all due reviews are finished", refreshedFlashcardTask.isCompleted)
            assertEquals(3, refreshedFlashcardTask.completedCount)
            assertEquals(3, refreshedFlashcardTask.targetCount)
            assertTrue("Checklist should be marked complete", updated.isCompleted)
        }
    }

    @Test
    fun testPlacementQuizGrammarQuestionHasNoSpoilersAndNeutralPrompt() {
        val exercisesJson = """[
            {
                "id": "ex1",
                "prompt": "I don't have ___ water in my bottle.",
                "options": ["any", "some", "many", "few"],
                "answer": "any",
                "explanation": "Use any in negative sentences."
            }
        ]"""
        val lesson = GrammarLessonEntity(
            id = "g001",
            level = "A1",
            topic = "quantifiers",
            titleEn = "Countable and uncountable nouns: some and any",
            titleAr = "المعدود وغير المعدود: some و any",
            explanationAr = "شرح",
            examplesJson = "[]",
            exercisesJson = exercisesJson
        )

        val grammarQ = OnboardingPlacementQuiz.pickGrammarQuestionForLevel(
            level = "A1",
            lessons = listOf(lesson),
            random = kotlin.random.Random(42)
        )

        assertNotNull("Grammar question must be generated", grammarQ)
        assertEquals("Grammar", grammarQ!!.topicLabelEn)
        assertEquals("قواعد", grammarQ.topicLabelAr)
        assertEquals("اختر الكلمة الصحيحة لإكمال الجملة", grammarQ.promptAr)

        // Verify NO spoilers in badge or prompt!
        assertTrue(!grammarQ.topicLabelEn.contains("Countable"))
        assertTrue(!grammarQ.topicLabelEn.contains("some"))
        assertTrue(!grammarQ.topicLabelAr.contains("المعدود"))
        assertTrue(!grammarQ.topicLabelAr.contains("some"))
        assertTrue(!grammarQ.promptAr.contains("المعدود"))
    }

    @Test
    fun testSm2DueCardsExcludesUnseenCardsRepetitionsZero() = runBlocking {
        // Card 1: Unseen card (repetitions = 0) with a past due_timestamp
        dao.upsertWordProgress(
            WordProgressEntity(
                cardKey = "oxford5000:unseen_word",
                deck = "oxford5000",
                text = "unseen_word",
                repetitions = 0,
                dueTimestamp = 1000L,
                isMastered = false
            )
        )
        // Card 2: Valid SM-2 reviewed card (repetitions = 2) due now
        dao.upsertWordProgress(
            WordProgressEntity(
                cardKey = "oxford5000:reviewed_due",
                deck = "oxford5000",
                text = "reviewed_due",
                repetitions = 2,
                dueTimestamp = 1000L,
                isMastered = false
            )
        )
        // Card 3: Future card (repetitions = 1) due at 5000L
        dao.upsertWordProgress(
            WordProgressEntity(
                cardKey = "oxford5000:future_card",
                deck = "oxford5000",
                text = "future_card",
                repetitions = 1,
                dueTimestamp = 5000L,
                isMastered = false
            )
        )

        val nowMs = 2000L
        val dueNowCount = dao.getDueNowCardsCount(nowMs)
        val dueNowForDecks = dao.getDueNowCardsCountForDecks(nowMs, listOf("oxford5000"))
        val dueTodayCount = dao.getReviewDueTodayCount(nowMs)

        // Only Card 2 satisfies repetitions > 0 AND due_timestamp <= 2000L!
        assertEquals("Due cards count must strictly exclude unseen cards (repetitions = 0)", 1, dueNowCount)
        assertEquals("Due cards for decks must strictly exclude unseen cards", 1, dueNowForDecks)
        assertEquals("Due today count must strictly exclude unseen cards", 1, dueTodayCount)
    }

    @Test
    fun testStudyPlanWhenNoDueCardsGeneratesZeroTargetAndCompleted() = runBlocking {
        val repo = StudyPlanRepository(context, dao, FixedTestTimeProvider())

        // Generate tasks when database has 0 due cards
        val checklist = repo.getOrCreateDailyChecklist("2026-11-02")
        val tasks = json.decodeFromString<List<DailyTaskItem>>(checklist.tasksJson)
        val flashcardsTask = tasks.find { it.type == "flashcards" }

        assertNotNull("Flashcards task must exist in plan", flashcardsTask)
        assertEquals("Target count must be 0 when no cards are due", 0, flashcardsTask!!.targetCount)
        assertEquals("Completed count must be 0 when no cards are due", 0, flashcardsTask.completedCount)
        assertTrue("Task must be marked completed (0/0) when 0 cards are due", flashcardsTask.isCompleted)
        assertTrue("Subtitle must reflect all reviews up to date", flashcardsTask.subtitleEn.contains("0 cards due today"))
    }

    @Test
    fun testStudyPlanRefreshChecklistNewWordsDoNotCompleteDueFlashcards() = runBlocking {
        val repo = StudyPlanRepository(context, dao, FixedTestTimeProvider())
        val initialTask = DailyTaskItem(
            id = "task_flashcards",
            type = "flashcards",
            titleEn = "Review Due Flashcards",
            titleAr = "مراجعة البطاقات المستحقة",
            subtitleEn = "5 cards due today",
            targetCount = 5,
            completedCount = 0,
            isCompleted = false
        )
        val checklist = DailyChecklistEntity(
            date = "2026-11-02",
            tasksJson = json.encodeToString(listOf(initialTask))
        )

        // No review attempts, but user learned 10 new words today
        dao.insertAttempt(
            AttemptEntity(
                activityType = "flashcard_new",
                itemId = "oxford5000:new1",
                timestamp = 1793577600000L + 1000L,
                sm2Rating = 3,
                isCorrect = true
            )
        )

        val refreshed = repo.refreshChecklistCompletion(checklist)
        val refreshedTasks = json.decodeFromString<List<DailyTaskItem>>(refreshed.tasksJson)
        val refreshedFlashcards = refreshedTasks.find { it.type == "flashcards" }!!

        // task_flashcards must NOT be satisfied by new words rated today!
        assertEquals("Flashcards completedCount must remain 0 when only new words were learned", 0, refreshedFlashcards.completedCount)
        assertTrue("Flashcards task must NOT be completed by new words", !refreshedFlashcards.isCompleted)
    }

    @Test
    fun testArabicDefinitionChipsDeduplication() {
        val rawArabic1 = "المريض، صابر، زبون، صبور، حليم"
        val rawArabic2 = "صابر، حليم، مستكين"
        val entries = listOf(
            EntryEntity(
                id = "1",
                type = "word",
                deck = "oxford5000",
                text = "patient",
                pos = "noun",
                level = "B1",
                definitionEn = "a person receiving medical care",
                noteEn = "",
                examplesEn = "",
                definitionUrl = "",
                audioUrl = "",
                arabic = rawArabic1
            ),
            EntryEntity(
                id = "2",
                type = "word",
                deck = "oxford5000",
                text = "patient",
                pos = "adjective",
                level = "B1",
                definitionEn = "able to wait calmly",
                noteEn = "",
                examplesEn = "",
                definitionUrl = "",
                audioUrl = "",
                arabic = rawArabic2
            )
        )

        val deduplicatedChips = entries
            .flatMap { entry ->
                entry.arabic
                    .split(',', '،', ';', '؛', '\n', '/')
                    .map { it.trim().removePrefix("-").removePrefix("•").trim() }
                    .filter { it.isNotBlank() }
            }
            .distinct()

        val expected = listOf("المريض", "صابر", "زبون", "صبور", "حليم", "مستكين")
        assertEquals(expected, deduplicatedChips)
    }

    @Test
    fun testFlashcardSessionSummaryDynamicMetrics() {
        val reviewSummary = com.example.ui.viewmodel.FlashcardSessionSummary(
            sessionType = com.example.ui.viewmodel.FlashcardSessionType.REVIEW,
            deckName = "Oxford 5000",
            totalCount = 20,
            masteredCount = 18,
            needsReviewCount = 2,
            qualitySum = 85
        )
        assertEquals(90, reviewSummary.retentionRatePercent)

        val learnNewSummary = com.example.ui.viewmodel.FlashcardSessionSummary(
            sessionType = com.example.ui.viewmodel.FlashcardSessionType.LEARN_NEW,
            deckName = "Oxford 5000",
            totalCount = 10,
            masteredCount = 8,
            needsReviewCount = 2,
            qualitySum = 42
        )
        assertEquals(10, learnNewSummary.totalCount)
        assertEquals(8, learnNewSummary.masteredCount)
        assertEquals(2, learnNewSummary.needsReviewCount)
    }

    @Test
    fun testDueNowVsWaitingLaterTodaySeparation() = runBlocking {
        val now = 1793577600000L // 12:00:00
        val endOfDay = now + 43200_000L // 23:59:59

        dao.insertEntryIgnore(
            EntryEntity(
                id = "cool_sep",
                type = "word",
                deck = "oxford5000",
                text = "cooldown",
                pos = "noun",
                level = "B2",
                definitionEn = "cooling down",
                noteEn = "",
                examplesEn = "",
                definitionUrl = "",
                audioUrl = "",
                arabic = "تبريد"
            )
        )

        // 1 card due right now (dueTimestamp = now - 1000L)
        dao.upsertWordProgress(
            WordProgressEntity(
                cardKey = "oxford5000:due_now",
                deck = "oxford5000",
                text = "due_now",
                repetitions = 2,
                dueTimestamp = now - 1000L,
                lastReviewedTimestamp = now - 86400_000L
            )
        )
        // 1 card waiting on cooldown later today (e.g. +9 minutes = now + 540_000L)
        dao.upsertWordProgress(
            WordProgressEntity(
                cardKey = "oxford5000:cooldown",
                deck = "oxford5000",
                text = "cooldown",
                repetitions = 1,
                dueTimestamp = now + 540_000L,
                lastReviewedTimestamp = now - 60_000L
            )
        )
        // 1 card due tomorrow (dueTimestamp = endOfDay + 10_000L)
        dao.upsertWordProgress(
            WordProgressEntity(
                cardKey = "oxford5000:tomorrow",
                deck = "oxford5000",
                text = "tomorrow",
                repetitions = 3,
                dueTimestamp = endOfDay + 10_000L,
                lastReviewedTimestamp = now - 86400_000L
            )
        )

        val dueNow = dao.getDueNowCardsCount(now)
        val waitingLater = dao.getLaterTodayCardsCount(now, endOfDay)
        val earlyReviewEntries = dao.getEarlyReviewEntriesOrdered(now, endOfDay)

        assertEquals("Due now must count only cards with dueTimestamp <= now", 1, dueNow)
        assertEquals("Waiting later must count cards with now < dueTimestamp <= endOfDay", 1, waitingLater)
        assertEquals("Early review entries must retrieve the cooldown card", 1, earlyReviewEntries.size)
        assertEquals("cooldown", earlyReviewEntries[0].text)
    }

    @Test
    fun testEarlyReviewSessionLoadsWaitingCooldownCards() = runBlocking {
        val now = 1793577600000L
        val endOfDay = now + 43200_000L

        // Insert entry and progress on cooldown
        dao.insertEntryIgnore(
            EntryEntity(
                id = "cool_1",
                type = "word",
                deck = "oxford5000",
                text = "phenomenon",
                pos = "noun",
                level = "B2",
                definitionEn = "a remarkable occurrence",
                noteEn = "",
                examplesEn = "",
                definitionUrl = "",
                audioUrl = "",
                arabic = "ظاهرة"
            )
        )
        dao.upsertWordProgress(
            WordProgressEntity(
                cardKey = "oxford5000:phenomenon",
                deck = "oxford5000",
                text = "phenomenon",
                repetitions = 0,
                lastReviewedTimestamp = now - 60_000L, // was reviewed 1 min ago with AGAIN
                dueTimestamp = now + 540_000L // 9 mins later
            )
        )

        val earlyEntries = dao.getEarlyReviewEntriesOrdered(now, endOfDay)
        assertEquals(1, earlyEntries.size)
        assertEquals("phenomenon", earlyEntries[0].text)
    }

    @Test
    fun testLearnedTodayEntriesQuery() = runBlocking {
        val now = 1793577600000L
        val startOfDay = now - 3600_000L

        dao.insertEntryIgnore(
            EntryEntity(
                id = "learn_1",
                type = "word",
                deck = "oxford5000",
                text = "resilience",
                pos = "noun",
                level = "B2",
                definitionEn = "capacity to recover quickly",
                noteEn = "",
                examplesEn = "",
                definitionUrl = "",
                audioUrl = "",
                arabic = "مرونة"
            )
        )
        dao.upsertWordProgress(
            WordProgressEntity(
                cardKey = "oxford5000:resilience",
                deck = "oxford5000",
                text = "resilience",
                firstLearnedTimestamp = now,
                lastReviewedTimestamp = now,
                repetitions = 1,
                dueTimestamp = now + 86400_000L
            )
        )
        // Insert flashcard_new attempt today
        dao.insertAttempt(
            AttemptEntity(
                activityType = "flashcard_new",
                itemId = "oxford5000:resilience",
                timestamp = now,
                sm2Rating = 3,
                isCorrect = true,
                score = 80f,
                deck = "oxford5000"
            )
        )

        val learnedToday = dao.getEntriesLearnedToday(startOfDay)
        assertEquals(1, learnedToday.size)
        assertEquals("resilience", learnedToday[0].text)
    }
}
