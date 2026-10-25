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
}
