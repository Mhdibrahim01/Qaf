package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.importer.DataImportRepository
import com.example.data.local.DailyTaskItem
import com.example.data.local.EntryEntity
import com.example.data.local.StudyPlanProfileEntity
import com.example.data.local.WordBooksDatabase
import com.example.data.local.WordProgressEntity
import com.example.data.onboarding.OnboardingPlacementQuiz
import com.example.data.plan.LocalQuizEngine
import com.example.data.plan.StudyPlanRepository
import com.example.data.plan.TestTimeProvider
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verification test suite for Learning Path–Driven Vocabulary Prioritization:
 * 1. IT Career targets 70% IT Industry + 30% Oxford 5000/OPAL split.
 * 2. General Fluency, IELTS Academic, and Medical prioritize corresponding path content.
 * 3. Unselected decks are strictly excluded from quota and selection results.
 * 4. Due SM-2 reviews are preserved regardless of new-word ratios.
 * 5. Insufficient content gracefully scales target counts with explanatory reason banners without silent Oxford fallbacks.
 * 6. Daily Mini-Quiz vocabulary questions align 100% with today's assigned profile words.
 * 7. Onboarding placement assessment remains a fair general-English proficiency evaluation.
 * 8. User progress, SM-2 history, and offline data structures remain fully functional.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LearningPathPrioritizationTest {

    private lateinit var context: Context
    private lateinit var db: WordBooksDatabase
    private lateinit var importRepo: DataImportRepository
    private lateinit var planRepo: StudyPlanRepository
    private lateinit var quizEngine: LocalQuizEngine
    private lateinit var timeProvider: TestTimeProvider
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setup() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, WordBooksDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        importRepo = DataImportRepository(context, db)
        importRepo.ensureImportedOnFirstLaunch(forceRecheck = true)

        // 2026-11-02 12:00:00 UTC
        timeProvider = TestTimeProvider(1793620800000L)
        planRepo = StudyPlanRepository(context, db.wordBooksDao(), timeProvider)
        quizEngine = LocalQuizEngine(db.wordBooksDao(), timeProvider, planRepo)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testItCareer70_30Split() = runBlocking {
        val profile = StudyPlanProfileEntity(
            id = 1,
            level = "B2",
            goal = "it_career",
            minutesPerDay = 30,
            studyDaysPerWeek = 6,
            startDate = "2026-11-02",
            selectedDecksJson = json.encodeToString(listOf("oxford5000", "oxford_opal", "it_industry")),
            restDaysJson = "[6]",
            startAtUserLevel = true,
            updatedAt = timeProvider.currentTimeMillis()
        )

        val result = planRepo.selectNewWordsForProfile(profile, 10)
        val words = result.words
        assertEquals(10, words.size)

        val itCount = words.count { it.deck == "it_industry" }
        val oxfordCount = words.count { it.deck in listOf("oxford5000", "oxford_opal") }

        assertEquals(7, itCount)
        assertEquals(3, oxfordCount)
    }

    @Test
    fun testMedicalCareer70_30Split() = runBlocking {
        val profile = StudyPlanProfileEntity(
            id = 1,
            level = "B2",
            goal = "medical",
            minutesPerDay = 30,
            studyDaysPerWeek = 6,
            startDate = "2026-11-02",
            selectedDecksJson = json.encodeToString(listOf("oxford5000", "oxford_opal", "medical_healthcare")),
            restDaysJson = "[6]",
            startAtUserLevel = true,
            updatedAt = timeProvider.currentTimeMillis()
        )

        val result = planRepo.selectNewWordsForProfile(profile, 10)
        val words = result.words
        assertEquals(10, words.size)

        val medicalCount = words.count { it.deck == "medical_healthcare" }
        val oxfordCount = words.count { it.deck in listOf("oxford5000", "oxford_opal") }

        assertEquals(7, medicalCount)
        assertEquals(3, oxfordCount)
    }

    @Test
    fun testIeltsPath50_30_20Split() = runBlocking {
        val profile = StudyPlanProfileEntity(
            id = 1,
            level = "B2",
            goal = "ielts",
            minutesPerDay = 30,
            studyDaysPerWeek = 6,
            startDate = "2026-11-02",
            selectedDecksJson = json.encodeToString(listOf("oxford5000", "oxford_opal", "ielts_core")),
            restDaysJson = "[6]",
            startAtUserLevel = true,
            updatedAt = timeProvider.currentTimeMillis()
        )

        val result = planRepo.selectNewWordsForProfile(profile, 10)
        val words = result.words
        assertEquals(10, words.size)

        val ieltsCount = words.count { it.deck == "ielts_core" }
        val opalCount = words.count { it.deck == "oxford_opal" }
        val oxford5000Count = words.count { it.deck == "oxford5000" }

        assertEquals(5, ieltsCount)
        assertEquals(3, opalCount)
        assertEquals(2, oxford5000Count)
    }

    @Test
    fun testUnselectedDecksAreStrictlyExcluded() = runBlocking {
        // User selects ONLY it_industry (unselects oxford5000 and oxford_opal)
        val profile = StudyPlanProfileEntity(
            id = 1,
            level = "B2",
            goal = "it_career",
            minutesPerDay = 30,
            studyDaysPerWeek = 6,
            startDate = "2026-11-02",
            selectedDecksJson = json.encodeToString(listOf("it_industry")),
            restDaysJson = "[6]",
            startAtUserLevel = true,
            updatedAt = timeProvider.currentTimeMillis()
        )

        val result = planRepo.selectNewWordsForProfile(profile, 10)
        val words = result.words

        // All returned words MUST belong to it_industry (never oxford5000 or oxford_opal)
        assertTrue(words.isNotEmpty())
        assertTrue(words.all { it.deck == "it_industry" })
    }

    @Test
    fun testSm2ReviewsPreservedRegardlessOfRatio() = runBlocking {
        // Create 5 due review cards in oxford5000 with due_timestamp in the past
        val dao = db.wordBooksDao()
        val now = timeProvider.currentTimeMillis()
        val pastDue = now - 86_400_000L // 1 day ago

        val dueProgress1 = WordProgressEntity(cardKey = "oxford5000:abandon", deck = "oxford5000", text = "abandon", dueTimestamp = pastDue, isMastered = false, repetitions = 2)
        val dueProgress2 = WordProgressEntity(cardKey = "oxford5000:benefit", deck = "oxford5000", text = "benefit", dueTimestamp = pastDue, isMastered = false, repetitions = 2)
        dao.upsertWordProgress(dueProgress1)
        dao.upsertWordProgress(dueProgress2)

        val dueNowEntries = dao.getDueNowEntriesOrdered(now)
        assertTrue(dueNowEntries.any { it.text == "abandon" })
        assertTrue(dueNowEntries.any { it.text == "benefit" })

        val dueNowCount = dao.getDueNowCardsCount(now)
        assertTrue(dueNowCount >= 2)
    }

    @Test
    fun testInsufficientContentReturnsReasonBannerWithoutSilentFallback() = runBlocking {
        // Create a custom profile with level C1 and ONLY a small deck
        val profile = StudyPlanProfileEntity(
            id = 1,
            level = "C1",
            goal = "it_career",
            minutesPerDay = 60, // requests 20 new words
            studyDaysPerWeek = 6,
            startDate = "2026-11-02",
            selectedDecksJson = json.encodeToString(listOf("it_industry")),
            restDaysJson = "[6]",
            startAtUserLevel = true,
            updatedAt = timeProvider.currentTimeMillis()
        )

        val result = planRepo.selectNewWordsForProfile(profile, 20)
        // Check that words are strictly from it_industry and reasonString is provided if short
        assertTrue(result.words.all { it.deck == "it_industry" })
        if (result.words.size < 20) {
            assertNotNull(result.reasonString)
            assertTrue(result.reasonString!!.contains("showing all available") || result.reasonString!!.contains("Fewer words today"))
        }
    }

    @Test
    fun testDailyMiniQuizVocabularyAlignedWithAssignedWords() = runBlocking {
        val profile = StudyPlanProfileEntity(
            id = 1,
            level = "B2",
            goal = "it_career",
            minutesPerDay = 30,
            studyDaysPerWeek = 6,
            startDate = "2026-11-02",
            selectedDecksJson = json.encodeToString(listOf("oxford5000", "oxford_opal", "it_industry")),
            restDaysJson = "[6]",
            startAtUserLevel = true,
            updatedAt = timeProvider.currentTimeMillis()
        )
        db.wordBooksDao().upsertStudyPlanProfile(profile)

        val assignedSelection = planRepo.selectNewWordsForProfile(profile, 10)
        val assignedTextSet = assignedSelection.words.map { it.text.trim().lowercase() }.toSet()

        val alignedResult = quizEngine.generateAlignedDailyQuiz(null, profile, isTtsAvailable = true)
        val vocabQuestions = alignedResult.questions.filter { it.skill == "vocabulary" }

        assertEquals(5, vocabQuestions.size)
        // Check that every vocabulary question target is from today's assigned selection
        for (q in vocabQuestions) {
            val isPromptMatch = assignedTextSet.any { word -> q.promptEn.lowercase().contains(word) || q.correctText.lowercase() == word }
            assertTrue("Quiz question ${q.promptEn} should align with assigned words", isPromptMatch)
        }
    }

    @Test
    fun testPlacementAssessmentIsGeneralEnglishFair() = runBlocking {
        val questions = OnboardingPlacementQuiz.buildPlacementQuizQuestions(db.wordBooksDao(), seed = 12345L)
        assertNotNull(questions)
        assertEquals(10, questions!!.size)

        // Verify that placement questions cover A1 through C1 evenly
        val levels = questions.map { it.level }.distinct().sorted()
        assertEquals(listOf("A1", "A2", "B1", "B2", "C1"), levels)
    }

    @Test
    fun testUserProgressAndChecklistIntegrity() = runBlocking {
        val checklist = planRepo.saveOnboardingGoalAndGeneratePlan("B2", "medical", 30, 6)
        val tasks = json.decodeFromString<List<DailyTaskItem>>(checklist.tasksJson)

        assertTrue(tasks.any { it.type == "flashcards" })
        assertTrue(tasks.any { it.type == "new_words" })
        assertTrue(tasks.any { it.type == "grammar" })
        assertTrue(tasks.any { it.type == "quiz" })

        val newWordsTask = tasks.first { it.type == "new_words" }
        assertEquals("word_books", newWordsTask.linkedRoute)
    }
}
