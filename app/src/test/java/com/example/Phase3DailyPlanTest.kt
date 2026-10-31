package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.importer.DataImportRepository
import com.example.data.local.CorrectionNotebookEntity
import com.example.data.local.DailyChecklistEntity
import com.example.data.local.DailyTaskItem
import com.example.data.local.StudyPlanProfileEntity
import com.example.data.local.WordBooksDatabase
import com.example.data.local.WordProgressEntity
import com.example.data.plan.LearnerModelRepository
import com.example.data.plan.StudyPlanRepository
import com.example.data.plan.TestTimeProvider
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase 3 verification suite covering:
 * 1. Explicit time-budget scaling across 15, 30, and 45-minute profiles.
 * 2. Heavy review backlog (e.g. 42 reviews due on a 15-min goal) load pacing:
 *    - Schedules 25 reviews today and sets backlogRemaining = 17.
 *    - Populates exact reason "25 of 42 due reviews scheduled today to fit your 15-min goal (17 remaining in backlog)".
 *    - Reduces new words and populates exact reason "Fewer new words today: 42 reviews are due".
 * 3. Due sentence corrections inclusion:
 *    - When due correction reviews exist, schedules `task_correction_reviews` with exact reason.
 * 4. Sentence practice rotation:
 *    - For A1/A2 levels, schedules `task_sentence_practice` instead of reading texts (which start at B1).
 * 5. Data-backed reason and estimatedMinutes populated on EVERY DailyTaskItem.
 * 6. Dynamic Daily Goal and Weekly Checklist 7-day strip driven by database records.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Phase3DailyPlanTest {

    private lateinit var context: Context
    private lateinit var db: WordBooksDatabase
    private lateinit var repo: DataImportRepository
    private lateinit var planRepo: StudyPlanRepository
    private lateinit var learnerModelRepo: LearnerModelRepository
    private lateinit var timeProvider: TestTimeProvider
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setup() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, WordBooksDatabase::class.java)
        .allowMainThreadQueries()
        .build()

        repo = DataImportRepository(context, db)
        repo.ensureImportedOnFirstLaunch(forceRecheck = true)
        com.example.data.reading.ReadingRepository(context, db.wordBooksDao()).importReadingTextsIfNeeded()
        com.example.data.grammar.GrammarRepository(context, db.wordBooksDao()).importGrammarLessonsIfNeeded()

        // 2026-11-02 12:00:00 UTC (Monday)
        timeProvider = TestTimeProvider(1793620800000L)
        planRepo = StudyPlanRepository(context, db.wordBooksDao(), timeProvider)
        learnerModelRepo = LearnerModelRepository(context, db.wordBooksDao(), timeProvider)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testTimeBudgetScaling15_30_45Min() = runBlocking {
        // 15 min profile
        val profile15 = StudyPlanProfileEntity(
            id = 1,
            level = "B2",
            goal = "general_fluency",
            minutesPerDay = 15,
            studyDaysPerWeek = 6,
            startDate = "2026-11-02",
            selectedDecksJson = json.encodeToString(listOf("oxford5000", "oxford_opal")),
            restDaysJson = "[6]",
            startAtUserLevel = true,
            updatedAt = timeProvider.currentTimeMillis()
        )
        val checklist15 = planRepo.saveOnboardingGoalAndGeneratePlan("B2", "general_fluency", 15, 6)
        val tasks15 = json.decodeFromString<List<DailyTaskItem>>(checklist15.tasksJson)

        val flashcards15 = tasks15.first { it.type == "flashcards" }
        val newWords15 = tasks15.first { it.type == "new_words" }
        val grammar15 = tasks15.first { it.type == "grammar" }

        assertEquals(0, flashcards15.targetCount)
        assertTrue(flashcards15.isCompleted)
        assertEquals(5, newWords15.targetCount)
        assertEquals(4, grammar15.estimatedMinutes)

        // 30 min profile
        val checklist30 = planRepo.saveOnboardingGoalAndGeneratePlan("B2", "general_fluency", 30, 6)
        val tasks30 = json.decodeFromString<List<DailyTaskItem>>(checklist30.tasksJson)

        val flashcards30 = tasks30.first { it.type == "flashcards" }
        val newWords30 = tasks30.first { it.type == "new_words" }
        val grammar30 = tasks30.first { it.type == "grammar" }
        val reading30 = tasks30.firstOrNull { it.type == "reading" }

        assertEquals(0, flashcards30.targetCount)
        assertTrue(flashcards30.isCompleted)
        assertEquals(10, newWords30.targetCount)
        assertEquals(7, grammar30.estimatedMinutes)
        assertNotNull(reading30)
        assertEquals(5, reading30?.estimatedMinutes)

        // Verify every task carries a non-empty data-backed reason and estimatedMinutes > 0 for active tasks
        for (task in tasks30) {
            assertTrue("Task ${task.id} must have non-empty reason", task.reason.isNotBlank())
            if (task.targetCount > 0) {
                assertTrue("Task ${task.id} must have estimatedMinutes > 0", task.estimatedMinutes > 0)
            }
        }
    }

    @Test
    fun testReviewBacklogPacingAndReductionReason() = runBlocking {
        // Seed 42 due cards in Oxford 5000 deck
        val now = timeProvider.currentTimeMillis()
        val dueCards = (1..42).map { i ->
            WordProgressEntity(
                cardKey = "oxford5000_$i",
                deck = "oxford5000",
                text = "word$i",
                entryId = "$i",
                repetitions = 2,
                intervalDays = 5,
                easeFactor = 2.5f,
                lastReviewedTimestamp = now - 5 * 86_400_000L,
                dueTimestamp = now - 1000L, // Overdue
                firstLearnedTimestamp = now - 15 * 86_400_000L
            )
        }
        for (card in dueCards) {
            db.wordBooksDao().upsertWordProgress(card)
        }

        val profile15 = StudyPlanProfileEntity(
            id = 1,
            level = "B2",
            goal = "general_fluency",
            minutesPerDay = 15,
            studyDaysPerWeek = 6,
            startDate = "2026-11-02",
            selectedDecksJson = json.encodeToString(listOf("oxford5000", "oxford_opal")),
            restDaysJson = "[6]",
            startAtUserLevel = true,
            updatedAt = now
        )
        planRepo.updateProfile(profile15)

        val checklist = planRepo.getOrCreateDailyChecklist("2026-11-02")
        val tasks = json.decodeFromString<List<DailyTaskItem>>(checklist.tasksJson)

        // 1. Review task pacing: 25 scheduled today out of 42, 17 in backlog
        val reviewTask = tasks.first { it.type == "flashcards" }
        assertEquals(25, reviewTask.targetCount)
        assertEquals(17, reviewTask.backlogRemaining)
        assertTrue(
            "Review task reason must explain 25 of 42 scheduled reviews with 17 in backlog",
            reviewTask.reason.contains("25 of 42 due reviews scheduled today to fit your 15-min goal (17 remaining in backlog)")
        )

        // 2. New words task: reduced due to heavy review load with exact reason
        val newWordsTask = tasks.first { it.type == "new_words" }
        assertEquals(3, newWordsTask.targetCount) // reduced from 5 to 3
        assertEquals("Fewer new words today: 42 reviews are due", newWordsTask.reason)
    }

    @Test
    fun testDueCorrectionReviewsTaskIncludedWhenDueCorrectionsExist() = runBlocking {
        val now = timeProvider.currentTimeMillis()
        // Seed 3 due correction notebook entries
        val corrections = listOf(
            CorrectionNotebookEntity(
                id = 1L,
                originalSentence = "He go to school yesterday.",
                correctedSentence = "He went to school yesterday.",
                explanationAr = "صيغة الماضي البسيط",
                status = "checked",
                nextReviewTimestamp = now - 5000L
            ),
            CorrectionNotebookEntity(
                id = 2L,
                originalSentence = "She have two brother.",
                correctedSentence = "She has two brothers.",
                explanationAr = "توافق الفعل والفاعل",
                status = "checked",
                nextReviewTimestamp = now - 1000L
            ),
            CorrectionNotebookEntity(
                id = 3L,
                originalSentence = "I am agree with you.",
                correctedSentence = "I agree with you.",
                explanationAr = "استخدام agree كفعل أساسي",
                status = "checked",
                nextReviewTimestamp = now - 2000L
            )
        )
        for (c in corrections) {
            db.wordBooksDao().upsertCorrectionEntry(c)
        }

        val profile = StudyPlanProfileEntity(
            id = 1,
            level = "B2",
            goal = "general_fluency",
            minutesPerDay = 30,
            studyDaysPerWeek = 6,
            startDate = "2026-11-02",
            selectedDecksJson = json.encodeToString(listOf("oxford5000", "oxford_opal")),
            restDaysJson = "[6]",
            startAtUserLevel = true,
            updatedAt = now
        )
        planRepo.updateProfile(profile)

        val checklist = planRepo.getOrCreateDailyChecklist("2026-11-02")
        val tasks = json.decodeFromString<List<DailyTaskItem>>(checklist.tasksJson)

        val correctionTask = tasks.firstOrNull { it.type == "correction_reviews" }
        assertNotNull("Correction reviews task must be present when corrections are due", correctionTask)
        assertEquals("task_correction_reviews", correctionTask?.id)
        assertEquals(3, correctionTask?.targetCount)
        assertEquals("3 saved sentence corrections are due for review today", correctionTask?.reason)
        assertEquals("sentences", correctionTask?.linkedRoute)
    }

    @Test
    fun testSentencePracticeRotationForA1AndA2Levels() = runBlocking {
        // A2 user: when A2 reading texts are imported in assets, planner schedules reading task for A2
        val profileA2 = StudyPlanProfileEntity(
            id = 1,
            level = "A2",
            goal = "general_fluency",
            minutesPerDay = 30,
            studyDaysPerWeek = 6,
            startDate = "2026-11-02",
            selectedDecksJson = json.encodeToString(listOf("oxford5000")),
            restDaysJson = "[6]",
            startAtUserLevel = true,
            updatedAt = timeProvider.currentTimeMillis()
        )
        planRepo.updateProfile(profileA2)

        val checklist = planRepo.getOrCreateDailyChecklist("2026-11-02")
        val tasks = json.decodeFromString<List<DailyTaskItem>>(checklist.tasksJson)

        val readingTask = tasks.firstOrNull { it.type == "reading" }
        assertNotNull("Reading task matched to A2 level must be scheduled when A2 texts exist", readingTask)
        assertTrue("Reading task should link to reading story", readingTask?.id?.startsWith("task_reading_") == true)

        // When reading texts are absent/exhausted for the level, planner falls back to sentence production practice
        db.wordBooksDao().deleteAllReadingTexts()
        val planRepoFallback = StudyPlanRepository(context, db.wordBooksDao(), timeProvider)
        val checklistFallback = planRepoFallback.getOrCreateDailyChecklist("2026-11-03")
        val tasksFallback = json.decodeFromString<List<DailyTaskItem>>(checklistFallback.tasksJson)

        val sentenceTask = tasksFallback.firstOrNull { it.type == "sentences" }
        assertNotNull("Sentence practice task must be scheduled when no reading texts exist for level", sentenceTask)
        assertEquals("task_sentence_practice", sentenceTask?.id)
        assertEquals(3, sentenceTask?.targetCount)
        assertEquals("Sentence production practice matched to level A2 (~3 min)", sentenceTask?.reason)
    }

    @Test
    fun testWeeklyChecklistSummaryDerivedFromDatabase() = runBlocking {
        // Seed checklists for Monday (complete) and Tuesday (partial)
        val monChecklist = DailyChecklistEntity(
            date = "2026-11-02",
            tasksJson = "[]",
            completedCount = 4,
            totalCount = 4,
            isCompleted = true,
            completedAt = timeProvider.currentTimeMillis()
        )
        val tueChecklist = DailyChecklistEntity(
            date = "2026-11-03",
            tasksJson = "[]",
            completedCount = 2,
            totalCount = 5,
            isCompleted = false,
            completedAt = null
        )
        db.wordBooksDao().upsertDailyChecklist(monChecklist)
        db.wordBooksDao().upsertDailyChecklist(tueChecklist)

        val weeklyList = planRepo.getWeeklyChecklists("2026-11-02")
        assertEquals(7, weeklyList.size)

        val mon = weeklyList[0]
        val tue = weeklyList[1]
        val wed = weeklyList[2]

        assertNotNull(mon)
        assertTrue(mon?.isCompleted == true)
        assertEquals(4, mon?.completedCount)

        assertNotNull(tue)
        assertFalse(tue?.isCompleted == true)
        assertEquals(2, tue?.completedCount)

        // Wednesday has no record yet
        assertEquals(null, wed)
    }

    @Test
    fun testExtraNewWordsCountSynchronization() = runBlocking {
        val now = timeProvider.currentTimeMillis()
        val dateStr = "2026-11-02"

        // Create initial checklist with targetCount = 10, completedCount = 0
        val initialTask = DailyTaskItem(
            id = "task_new_words",
            type = "new_words",
            titleEn = "Learn 10 New Words (B1)",
            titleAr = "تعلم 10 مفردات جديدة (مستوى B1)",
            subtitleEn = "Deck: Oxford 5000",
            targetCount = 10,
            completedCount = 0,
            isCompleted = false,
            linkedRoute = "word_books",
            linkedId = "oxford5000|B1"
        )
        val initialChecklist = DailyChecklistEntity(
            date = dateStr,
            tasksJson = json.encodeToString(listOf(initialTask)),
            completedCount = 0,
            totalCount = 1,
            isCompleted = false
        )
        db.wordBooksDao().upsertDailyChecklist(initialChecklist)

        // Insert 15 words learned today
        for (i in 1..15) {
            db.wordBooksDao().upsertWordProgress(
                WordProgressEntity(
                    cardKey = "oxford5000|word_$i",
                    deck = "oxford5000",
                    text = "word_$i",
                    firstLearnedTimestamp = now,
                    lastReviewedTimestamp = now,
                    repetitions = 1,
                    againCount = 0
                )
            )
        }

        // Refresh checklist completion
        val refreshed = planRepo.refreshChecklistCompletion(initialChecklist)
        val tasks = json.decodeFromString<List<DailyTaskItem>>(refreshed.tasksJson)
        val task = tasks.first { it.type == "new_words" }

        assertEquals(15, task.targetCount)
        assertEquals(15, task.completedCount)
        assertTrue(task.isCompleted)
        assertTrue(task.titleEn.contains("15"))
        assertFalse(task.titleEn.contains("10"))
        assertEquals("Learn 15 New Words (B1)", task.titleEn)
        assertTrue(task.titleAr.contains("15"))
        assertFalse(task.titleAr.contains("10"))
        // Consecutive extra words increments: +5, then +5, then +10
        // Words 16..20 (+5 extra words -> total 20)
        for (i in 16..20) {
            db.wordBooksDao().upsertWordProgress(
                WordProgressEntity(
                    cardKey = "oxford5000|word_$i",
                    deck = "oxford5000",
                    text = "word_$i",
                    firstLearnedTimestamp = now,
                    lastReviewedTimestamp = now,
                    repetitions = 1,
                    againCount = 0
                )
            )
        }
        val refreshed20 = planRepo.refreshChecklistCompletion(refreshed)
        val task20 = json.decodeFromString<List<DailyTaskItem>>(refreshed20.tasksJson).first { it.type == "new_words" }
        assertEquals(20, task20.targetCount)
        assertEquals(20, task20.completedCount)
        assertTrue(task20.isCompleted)
        assertEquals("Learn 20 New Words (B1)", task20.titleEn)
        assertEquals("تعلم 20 مفردات جديدة (مستوى B1)", task20.titleAr)

        // Idempotency check: Calling refreshChecklistCompletion again with no new words yields identical state
        val refreshed20Again = planRepo.refreshChecklistCompletion(refreshed20)
        val task20Again = json.decodeFromString<List<DailyTaskItem>>(refreshed20Again.tasksJson).first { it.type == "new_words" }
        assertEquals(20, task20Again.targetCount)
        assertEquals(20, task20Again.completedCount)
        assertEquals("Learn 20 New Words (B1)", task20Again.titleEn)
        assertEquals("تعلم 20 مفردات جديدة (مستوى B1)", task20Again.titleAr)

        // Words 21..30 (+10 extra words -> total 30)
        for (i in 21..30) {
            db.wordBooksDao().upsertWordProgress(
                WordProgressEntity(
                    cardKey = "oxford5000|word_$i",
                    deck = "oxford5000",
                    text = "word_$i",
                    firstLearnedTimestamp = now,
                    lastReviewedTimestamp = now,
                    repetitions = 1,
                    againCount = 0
                )
            )
        }
        val refreshed30 = planRepo.refreshChecklistCompletion(refreshed20)
        val task30 = json.decodeFromString<List<DailyTaskItem>>(refreshed30.tasksJson).first { it.type == "new_words" }
        assertEquals(30, task30.targetCount)
        assertEquals(30, task30.completedCount)
        assertTrue(task30.isCompleted)
        assertEquals("Learn 30 New Words (B1)", task30.titleEn)
        assertEquals("تعلم 30 مفردات جديدة (مستوى B1)", task30.titleAr)
    }

    @Test
    fun testPartialNewWordsCompletionKeepsOriginalTarget() = runBlocking {
        val now = timeProvider.currentTimeMillis()
        val dateStr = "2026-11-02"

        val initialTask = DailyTaskItem(
            id = "task_new_words",
            type = "new_words",
            titleEn = "Learn 10 New Words (B2)",
            titleAr = "تعلم 10 مفردات جديدة (مستوى B2)",
            subtitleEn = "Deck: Oxford 5000",
            targetCount = 10,
            completedCount = 0,
            isCompleted = false,
            linkedRoute = "word_books",
            linkedId = "oxford5000|B2"
        )
        val initialChecklist = DailyChecklistEntity(
            date = dateStr,
            tasksJson = json.encodeToString(listOf(initialTask)),
            completedCount = 0,
            totalCount = 1,
            isCompleted = false
        )
        db.wordBooksDao().upsertDailyChecklist(initialChecklist)

        // Learn only 4 words (less than target of 10)
        for (i in 1..4) {
            db.wordBooksDao().upsertWordProgress(
                WordProgressEntity(
                    cardKey = "oxford5000|partial_word_$i",
                    deck = "oxford5000",
                    text = "partial_word_$i",
                    firstLearnedTimestamp = now,
                    lastReviewedTimestamp = now,
                    repetitions = 1,
                    againCount = 0
                )
            )
        }

        val refreshed = planRepo.refreshChecklistCompletion(initialChecklist)
        val task = json.decodeFromString<List<DailyTaskItem>>(refreshed.tasksJson).first { it.type == "new_words" }

        assertEquals(10, task.targetCount)
        assertEquals(4, task.completedCount)
        assertFalse(task.isCompleted)
        assertEquals("Learn 10 New Words (B2)", task.titleEn)
        assertEquals("تعلم 10 مفردات جديدة (مستوى B2)", task.titleAr)
    }

    @Test
    fun testMacroTopicNormalizationAndCompoundPhrases() {
        // Asset topics from reading_word_links.json
        assertEquals("work_career", com.example.data.plan.AdaptiveConfig.normalizeMacroTopic("business_career"))
        assertEquals("technology", com.example.data.plan.AdaptiveConfig.normalizeMacroTopic("technology_science"))
        assertEquals("science_nature", com.example.data.plan.AdaptiveConfig.normalizeMacroTopic("environment_nature"))
        assertEquals("daily_life", com.example.data.plan.AdaptiveConfig.normalizeMacroTopic("daily_life_culture"))
        assertEquals("travel", com.example.data.plan.AdaptiveConfig.normalizeMacroTopic("travel_global"))
        assertEquals("health", com.example.data.plan.AdaptiveConfig.normalizeMacroTopic("health_psychology"))
        assertEquals("education", com.example.data.plan.AdaptiveConfig.normalizeMacroTopic("education_language"))

        // Compound topics from reading_texts.json
        assertEquals("work_career", com.example.data.plan.AdaptiveConfig.normalizeMacroTopic("career and interview"))
        assertEquals("work_career", com.example.data.plan.AdaptiveConfig.normalizeMacroTopic("work and office"))
        assertEquals("work_career", com.example.data.plan.AdaptiveConfig.normalizeMacroTopic("work and careers"))
        assertEquals("work_career", com.example.data.plan.AdaptiveConfig.normalizeMacroTopic("work and crafts"))
        assertEquals("technology", com.example.data.plan.AdaptiveConfig.normalizeMacroTopic("information technology"))
        assertEquals("health", com.example.data.plan.AdaptiveConfig.normalizeMacroTopic("health and fitness"))
        assertEquals("health", com.example.data.plan.AdaptiveConfig.normalizeMacroTopic("sports and abilities"))
        assertEquals("science_nature", com.example.data.plan.AdaptiveConfig.normalizeMacroTopic("science and safety"))
        assertEquals("science_nature", com.example.data.plan.AdaptiveConfig.normalizeMacroTopic("science and discovery"))
        assertEquals("education", com.example.data.plan.AdaptiveConfig.normalizeMacroTopic("education and language"))
        assertEquals("education", com.example.data.plan.AdaptiveConfig.normalizeMacroTopic("education and science"))
        assertEquals("education", com.example.data.plan.AdaptiveConfig.normalizeMacroTopic("friendship and school"))
        assertEquals("daily_life", com.example.data.plan.AdaptiveConfig.normalizeMacroTopic("daily routine"))
        assertEquals("daily_life", com.example.data.plan.AdaptiveConfig.normalizeMacroTopic("daily life"))
        assertEquals("daily_life", com.example.data.plan.AdaptiveConfig.normalizeMacroTopic("daily"))
        assertEquals("daily_life", com.example.data.plan.AdaptiveConfig.normalizeMacroTopic("life"))
        assertEquals("science_nature", com.example.data.plan.AdaptiveConfig.normalizeMacroTopic("nature and travel"))
        assertEquals("travel", com.example.data.plan.AdaptiveConfig.normalizeMacroTopic("cities and tourism"))
    }

    @Test
    fun testRecommendedTextsPrioritizesPreferredTopics() {
        val texts = listOf(
            com.example.data.local.ReadingTextEntity(
                id = "story_1",
                titleEn = "A Day in Paris",
                titleAr = "يوم في باريس",
                level = "B1",
                topic = "daily life",
                sentencesJson = "[]",
                questionsJson = "[]"
            ),
            com.example.data.local.ReadingTextEntity(
                id = "story_2",
                titleEn = "Interview Preparation",
                titleAr = "التحضير للمقابلة",
                level = "B1",
                topic = "career and interview",
                sentencesJson = "[]",
                questionsJson = "[]"
            ),
            com.example.data.local.ReadingTextEntity(
                id = "story_3",
                titleEn = "Advanced AI",
                titleAr = "الذكاء الاصطناعي المتقدم",
                level = "B2",
                topic = "technology",
                sentencesJson = "[]",
                questionsJson = "[]"
            )
        )

        val userLevel = "B1"
        // Also verify with raw asset key / compound name
        val preferredTopics = listOf("business_career")

        val levelTexts = texts.filter { it.level.equals(userLevel, ignoreCase = true) }
        val preferredSet = preferredTopics.map { com.example.data.plan.AdaptiveConfig.normalizeMacroTopic(it) }.toSet()
        val sortedRecommended = levelTexts.sortedWith(
            compareByDescending<com.example.data.local.ReadingTextEntity> { text ->
                val normalized = com.example.data.plan.AdaptiveConfig.normalizeMacroTopic(text.topic)
                normalized in preferredSet
            }.thenBy { it.id }
        )

        assertEquals(2, sortedRecommended.size)
        // Career and interview should be prioritized to index 0
        assertEquals("story_2", sortedRecommended[0].id)
        assertEquals("story_1", sortedRecommended[1].id)
    }

    @Test
    fun testDailyMiniQuizOnlyCompletedByDailyMiniQuiz() = runBlocking {
        val today = timeProvider.todayDateString()
        val profile = StudyPlanProfileEntity(
            id = 1,
            level = "B2",
            goal = "general_fluency",
            minutesPerDay = 30,
            studyDaysPerWeek = 6,
            startDate = today,
            selectedDecksJson = "[\"oxford5000\"]",
            restDaysJson = "[]",
            updatedAt = timeProvider.currentTimeMillis()
        )
        db.wordBooksDao().upsertStudyPlanProfile(profile)

        val checklist = planRepo.getOrCreateDailyChecklist(today)
        val tasks = json.decodeFromString<List<DailyTaskItem>>(checklist.tasksJson)
        val quizTask = tasks.first { it.id == "task_daily_quiz" }
        assertFalse("Daily quiz task must start uncompleted", quizTask.isCompleted)
        assertEquals(0, quizTask.completedCount)

        // 1. Completing 60s speed challenge (vocab_blitz_60s) must NOT complete daily mini quiz
        db.wordBooksDao().insertQuizResult(
            com.example.data.local.QuizResultEntity(
                date = today,
                quizType = "vocab_blitz_60s",
                skill = "vocabulary",
                totalQuestions = 15,
                correctAnswers = 14,
                accuracyPercent = 93.3f,
                timestamp = timeProvider.currentTimeMillis()
            )
        )
        val afterBlitz = planRepo.refreshChecklistCompletion(checklist)
        val afterBlitzTasks = json.decodeFromString<List<DailyTaskItem>>(afterBlitz.tasksJson)
        val afterBlitzQuizTask = afterBlitzTasks.first { it.id == "task_daily_quiz" }
        assertFalse("Daily quiz must NOT be completed by 60s speed challenge", afterBlitzQuizTask.isCompleted)
        assertEquals(0, afterBlitzQuizTask.completedCount)

        // 2. Completing weekly or weak areas quiz must NOT complete daily mini quiz
        db.wordBooksDao().insertQuizResult(
            com.example.data.local.QuizResultEntity(
                date = today,
                quizType = "weekly",
                skill = "vocabulary",
                totalQuestions = 20,
                correctAnswers = 18,
                accuracyPercent = 90f,
                timestamp = timeProvider.currentTimeMillis()
            )
        )
        val afterWeekly = planRepo.refreshChecklistCompletion(afterBlitz)
        val afterWeeklyTasks = json.decodeFromString<List<DailyTaskItem>>(afterWeekly.tasksJson)
        val afterWeeklyQuizTask = afterWeeklyTasks.first { it.id == "task_daily_quiz" }
        assertFalse("Daily quiz must NOT be completed by weekly quiz", afterWeeklyQuizTask.isCompleted)
        assertEquals(0, afterWeeklyQuizTask.completedCount)

        // 3. Completing daily mini quiz (daily_mini) DOES complete daily mini quiz
        db.wordBooksDao().insertQuizResult(
            com.example.data.local.QuizResultEntity(
                date = today,
                quizType = "daily_mini",
                skill = "vocabulary",
                totalQuestions = 5,
                correctAnswers = 5,
                accuracyPercent = 100f,
                timestamp = timeProvider.currentTimeMillis()
            )
        )
        val afterDaily = planRepo.refreshChecklistCompletion(afterWeekly)
        val afterDailyTasks = json.decodeFromString<List<DailyTaskItem>>(afterDaily.tasksJson)
        val afterDailyQuizTask = afterDailyTasks.first { it.id == "task_daily_quiz" }
        assertTrue("Daily quiz MUST be completed when daily_mini result exists", afterDailyQuizTask.isCompleted)
        assertEquals(afterDailyQuizTask.targetCount, afterDailyQuizTask.completedCount)
    }

    @Test
    fun testLevelsByClosenessExcludesLowerLevelsWhenStartAtUserLevel() {
        // B1 user must ONLY get B1, B2, C1 (strictly no A1 or A2)
        val b1Levels = planRepo.levelsByCloseness("B1", startAtUserLevel = true)
        assertEquals(listOf("B1", "B2", "C1"), b1Levels)
        assertFalse("B1 should never contain A1", b1Levels.contains("A1"))
        assertFalse("B1 should never contain A2", b1Levels.contains("A2"))

        // B2 user must ONLY get B2, C1 (strictly no B1, A2, A1)
        val b2Levels = planRepo.levelsByCloseness("B2", startAtUserLevel = true)
        assertEquals(listOf("B2", "C1"), b2Levels)

        // C1 user must ONLY get C1 (peak level in Qaf)
        val c1Levels = planRepo.levelsByCloseness("C1", startAtUserLevel = true)
        assertEquals(listOf("C1"), c1Levels)

        // A2 user must get A2, B1, B2 (no A1)
        val a2Levels = planRepo.levelsByCloseness("A2", startAtUserLevel = true)
        assertEquals(listOf("A2", "B1", "B2"), a2Levels)
        assertFalse("A2 should never contain A1", a2Levels.contains("A1"))

        // A1 user gets A1, A2
        val a1Levels = planRepo.levelsByCloseness("A1", startAtUserLevel = true)
        assertEquals(listOf("A1", "A2"), a1Levels)
    }

    @Test
    fun testStoryPreTeachWordSelectionExcludesWordsBelowUserLevel() = runBlocking {
        val today = timeProvider.todayDateString()
        val profile = StudyPlanProfileEntity(
            id = 1,
            level = "B1",
            goal = "general_fluency",
            minutesPerDay = 30,
            studyDaysPerWeek = 6,
            startDate = today,
            selectedDecksJson = "[\"oxford5000\"]",
            restDaysJson = "[]",
            startAtUserLevel = true,
            updatedAt = timeProvider.currentTimeMillis()
        )
        db.wordBooksDao().upsertStudyPlanProfile(profile)

        // Insert mock entries for A1, A2, B1, B2
        val entries = listOf(
            com.example.data.local.EntryEntity(
                id = "pre_a1",
                type = "word",
                deck = "oxford5000",
                text = "simpleword",
                pos = "noun",
                level = "A1",
                definitionEn = "very easy",
                noteEn = "",
                examplesEn = "a simple word",
                definitionUrl = "",
                audioUrl = "",
                arabic = "كلمة بسيطة",
                effectiveLevel = "A1"
            ),
            com.example.data.local.EntryEntity(
                id = "pre_a2",
                type = "word",
                deck = "oxford5000",
                text = "basicword",
                pos = "noun",
                level = "A2",
                definitionEn = "basic word",
                noteEn = "",
                examplesEn = "a basic word",
                definitionUrl = "",
                audioUrl = "",
                arabic = "كلمة أساسية",
                effectiveLevel = "A2"
            ),
            com.example.data.local.EntryEntity(
                id = "pre_b1",
                type = "word",
                deck = "oxford5000",
                text = "connectword",
                pos = "verb",
                level = "B1",
                definitionEn = "to connect",
                noteEn = "",
                examplesEn = "connect words together",
                definitionUrl = "",
                audioUrl = "",
                arabic = "يربط",
                effectiveLevel = "B1"
            ),
            com.example.data.local.EntryEntity(
                id = "pre_b2",
                type = "word",
                deck = "oxford5000",
                text = "expandword",
                pos = "verb",
                level = "B2",
                definitionEn = "to expand",
                noteEn = "",
                examplesEn = "expand knowledge",
                definitionUrl = "",
                audioUrl = "",
                arabic = "يوسّع",
                effectiveLevel = "B2"
            )
        )
        db.wordBooksDao().insertEntriesBatchIgnore(entries)

        val story = com.example.data.local.ReadingTextEntity(
            id = "story_preteach_test",
            titleEn = "Building Strong Connections",
            titleAr = "بناء علاقات قوية",
            level = "B1",
            topic = "daily_life",
            sentencesJson = "[]",
            targetWordsJson = "[\"simpleword\", \"basicword\", \"connectword\", \"expandword\"]",
            questionsJson = "[]"
        )
        db.wordBooksDao().upsertReadingText(story)

        val selectedWords = planRepo.getTodayTargetNewWords(
            profile = profile,
            targetCount = 4,
            date = today,
            anchorStoryOverride = story
        )

        val selectedTexts = selectedWords.map { it.text.lowercase() }
        assertFalse("A1 simpleword must NOT be selected for B1 user", selectedTexts.contains("simpleword"))
        assertFalse("A2 basicword must NOT be selected for B1 user", selectedTexts.contains("basicword"))
        assertTrue("B1 connectword should be selected", selectedTexts.contains("connectword"))
        assertTrue("B2 expandword should be selected", selectedTexts.contains("expandword"))
    }

    @Test
    fun testCefrB1ToB2MasteryIncludesLearningFamiliarMatureAtOrAboveUserLevel() = runBlocking {
        val today = timeProvider.todayDateString()
        val now = timeProvider.currentTimeMillis()
        val profile = StudyPlanProfileEntity(
            id = 1,
            level = "B1",
            goal = "general_fluency",
            minutesPerDay = 30,
            studyDaysPerWeek = 6,
            startDate = today,
            selectedDecksJson = "[\"oxford5000\"]",
            restDaysJson = "[]",
            startAtUserLevel = true,
            updatedAt = now
        )
        db.wordBooksDao().upsertStudyPlanProfile(profile)

        // Insert entries with different levels
        val entries = listOf(
            com.example.data.local.EntryEntity(
                id = "m_a1",
                type = "word",
                deck = "oxford5000",
                text = "apple",
                pos = "noun",
                level = "A1",
                definitionEn = "fruit",
                noteEn = "",
                examplesEn = "an apple",
                definitionUrl = "",
                audioUrl = "",
                arabic = "تفاحة",
                effectiveLevel = "A1"
            ),
            com.example.data.local.EntryEntity(
                id = "m_a2",
                type = "word",
                deck = "oxford5000",
                text = "travel",
                pos = "verb",
                level = "A2",
                definitionEn = "to go places",
                noteEn = "",
                examplesEn = "travel abroad",
                definitionUrl = "",
                audioUrl = "",
                arabic = "يسافر",
                effectiveLevel = "A2"
            ),
            com.example.data.local.EntryEntity(
                id = "m_b1",
                type = "word",
                deck = "oxford5000",
                text = "achieve",
                pos = "verb",
                level = "B1",
                definitionEn = "to accomplish",
                noteEn = "",
                examplesEn = "achieve goals",
                definitionUrl = "",
                audioUrl = "",
                arabic = "يحقق",
                effectiveLevel = "B1"
            ),
            com.example.data.local.EntryEntity(
                id = "m_b2",
                type = "word",
                deck = "oxford5000",
                text = "collaborate",
                pos = "verb",
                level = "B2",
                definitionEn = "work together",
                noteEn = "",
                examplesEn = "collaborate on task",
                definitionUrl = "",
                audioUrl = "",
                arabic = "يتعاون",
                effectiveLevel = "B2"
            ),
            com.example.data.local.EntryEntity(
                id = "m_c1",
                type = "word",
                deck = "oxford5000",
                text = "sophisticated",
                pos = "adj",
                level = "C1",
                definitionEn = "complex or refined",
                noteEn = "",
                examplesEn = "sophisticated method",
                definitionUrl = "",
                audioUrl = "",
                arabic = "متطور ومتقن",
                effectiveLevel = "C1"
            )
        )
        db.wordBooksDao().insertEntriesBatchIgnore(entries)

        // Progress:
        // "apple" (A1): mature (reps=5, interval=30, isMastered=true) -> should NOT count for B1->B2 Mastery
        // "travel" (A2): learning (reps=1, interval=1) -> should NOT count for B1->B2 Mastery
        // "achieve" (B1): learning (reps=1, interval=1) -> MUST count immediately!
        // "collaborate" (B2): familiar (reps=3, interval=10) -> MUST count!
        // "sophisticated" (C1): mature (reps=5, interval=30, isMastered=true) -> MUST count!
        db.wordBooksDao().upsertWordProgressBatch(
            listOf(
                WordProgressEntity(
                    cardKey = "oxford5000:apple",
                    deck = "oxford5000",
                    text = "apple",
                    repetitions = 5,
                    intervalDays = 30,
                    isMastered = true,
                    firstLearnedTimestamp = now - 100000L,
                    lastReviewedTimestamp = now
                ),
                WordProgressEntity(
                    cardKey = "oxford5000:travel",
                    deck = "oxford5000",
                    text = "travel",
                    repetitions = 1,
                    intervalDays = 1,
                    firstLearnedTimestamp = now - 50000L,
                    lastReviewedTimestamp = now
                ),
                WordProgressEntity(
                    cardKey = "oxford5000:achieve",
                    deck = "oxford5000",
                    text = "achieve",
                    repetitions = 1,
                    intervalDays = 1,
                    firstLearnedTimestamp = now,
                    lastReviewedTimestamp = now
                ),
                WordProgressEntity(
                    cardKey = "oxford5000:collaborate",
                    deck = "oxford5000",
                    text = "collaborate",
                    repetitions = 3,
                    intervalDays = 10,
                    firstLearnedTimestamp = now - 200000L,
                    lastReviewedTimestamp = now
                ),
                WordProgressEntity(
                    cardKey = "oxford5000:sophisticated",
                    deck = "oxford5000",
                    text = "sophisticated",
                    repetitions = 5,
                    intervalDays = 30,
                    isMastered = true,
                    firstLearnedTimestamp = now - 300000L,
                    lastReviewedTimestamp = now
                )
            )
        )

        val summary = learnerModelRepo.buildSnapshot(profile)
        val readiness = summary.cefrReadiness

        assertEquals("B1", readiness.currentLevel)
        assertEquals("B2", readiness.nextLevel)
        assertEquals(500, readiness.vocabTargetCount)

        // Must count achieve (B1), collaborate (B2), sophisticated (C1) = exactly 3
        // Must exclude apple (A1) and travel (A2)
        assertEquals("B1 Vocab count must include B1, B2, C1 words and exclude A1, A2", 3, readiness.vocabFamiliarOrMatureCount)
    }
}
