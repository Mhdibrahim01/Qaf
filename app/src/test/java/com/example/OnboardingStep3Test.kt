package com.example

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.importer.ImportProgressState
import com.example.data.local.DailyTaskItem
import com.example.data.local.EntryEntity
import com.example.data.local.GrammarLessonEntity
import com.example.data.local.PlanDailyTemplate
import com.example.data.local.ReadingTextEntity
import com.example.data.local.WordBooksDao
import com.example.data.local.WordBooksDatabase
import com.example.data.local.WordProgressEntity
import com.example.data.onboarding.OnboardingPreferencesRepository
import com.example.data.onboarding.OnboardingPreferencesState
import com.example.data.plan.StudyPlanRepository
import com.example.data.plan.TimeProvider
import com.example.ui.screens.OnboardingScreen
import com.example.ui.theme.WordBooksTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OnboardingStep3Test {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var db: WordBooksDatabase
    private lateinit var dao: WordBooksDao
    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var tempDataStoreFile: File
    private lateinit var onboardingRepo: OnboardingPreferencesRepository
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private class FixedMondayTimeProvider : TimeProvider {
        // Monday 2026-11-02 (not a Friday rest day)
        override fun currentTimeMillis(): Long = 1793577600000L
        override fun todayDateString(): String = "2026-11-02"
        override fun currentMonthString(): String = "2026-11"
        override fun dayOfWeek(): Int = Calendar.MONDAY
        override fun startOfDayMillis(dateString: String): Long = 1793577600000L
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, WordBooksDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.wordBooksDao()

        dataStoreScope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
        tempDataStoreFile = File(context.cacheDir, "test_onboarding_step3_${System.nanoTime()}.preferences_pb")
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { tempDataStoreFile }
        )
        onboardingRepo = OnboardingPreferencesRepository(context, testDataStore)
    }

    @After
    fun tearDown() {
        db.close()
        dataStoreScope.cancel()
        if (tempDataStoreFile.exists()) {
            tempDataStoreFile.delete()
        }
    }

    private suspend fun seedSampleContentForPlan() {
        val entries = (1..25).map { idx ->
            EntryEntity(
                id = "$idx",
                type = "word",
                deck = if (idx % 2 == 0) "oxford5000" else "oxford_opal",
                text = "word_$idx",
                pos = "noun",
                level = "B2",
                definitionEn = "Definition $idx",
                noteEn = "",
                examplesEn = "Example sentence $idx",
                examplesAr = "مثال $idx",
                definitionUrl = "",
                audioUrl = "",
                arabic = "معنى $idx",
                arabicSource = "file",
                ipa = "",
                syllables = "",
                extraExamplesJson = "",
                dataVersion = 3
            )
        }
        dao.upsertEntriesBatchPreservingUserEdits(entries)

        dao.upsertGrammarLesson(
            GrammarLessonEntity(
                id = "B2_G01",
                level = "B2",
                topic = "conditionals",
                titleEn = "Second Conditional",
                titleAr = "الشرط الثاني",
                explanationAr = "تفصيل الشرط الثاني",
                examplesJson = "[]",
                exercisesJson = "[]"
            )
        )

        dao.upsertReadingText(
            ReadingTextEntity(
                id = "B2_R01",
                level = "B2",
                topic = "career",
                titleEn = "Future of Work",
                titleAr = "مستقبل العمل",
                sentencesJson = "[]",
                questionsJson = "[]"
            )
        )
    }

    @Test
    fun `step 3 generates profile, scales monthly plan for 15-30-45-60 min, and builds 5-pillar daily checklist`() = runBlocking {
        seedSampleContentForPlan()
        val planRepo = StudyPlanRepository(context, dao, FixedMondayTimeProvider())

        // Test 15, 30, 45, 60 min/day scaling and goal deck mappings
        val expectedNewWordsByMinutes = mapOf(
            15 to 5,
            30 to 10,
            45 to 15,
            60 to 20
        )

        for ((minutes, expectedWords) in expectedNewWordsByMinutes) {
            val checklist = planRepo.saveOnboardingGoalAndGeneratePlan(
                level = "B1",
                goal = "ielts",
                minutesPerDay = minutes,
                studyDaysPerWeek = 6
            )

            // 1. Verify profile saved in Room
            val profile = dao.getStudyPlanProfile()
            assertNotNull(profile)
            assertEquals("B1", profile!!.level)
            assertEquals("ielts", profile.goal)
            assertEquals(minutes, profile.minutesPerDay)
            assertEquals(6, profile.studyDaysPerWeek)

            val decks = json.decodeFromString<List<String>>(profile.selectedDecksJson)
            assertEquals(listOf("oxford5000", "oxford_opal", "ielts_core"), decks)

            val restDays = json.decodeFromString<List<Int>>(profile.restDaysJson)
            assertEquals(listOf(Calendar.FRIDAY), restDays)

            // 2. Verify scaled monthly plan saved in study_plans
            val plan = dao.getStudyPlanForMonth("2026-11")
            assertNotNull(plan)
            assertEquals("B1", plan!!.levelStart)
            assertEquals("B2", plan.levelTarget)
            val template = json.decodeFromString<PlanDailyTemplate>(plan.dailyTemplateJson)
            assertEquals(expectedWords, template.new_words)

            // 3. Verify today's 5-pillar checklist generated in daily_checklists
            val tasks = json.decodeFromString<List<DailyTaskItem>>(checklist.tasksJson)
            assertEquals(5, tasks.size)
            val types = tasks.map { it.type }.toSet()
            assertEquals(setOf("flashcards", "new_words", "grammar", "reading", "quiz"), types)

            val newWordsTask = tasks.first { it.type == "new_words" }
            assertEquals(expectedWords, newWordsTask.targetCount)
        }
    }

    @Test
    fun `step 3 redo setup preserves completed checklist tasks and user word progress`() = runBlocking {
        seedSampleContentForPlan()
        val planRepo = StudyPlanRepository(context, dao, FixedMondayTimeProvider())

        // Initial Step 3 completion
        val initialChecklist = planRepo.saveOnboardingGoalAndGeneratePlan(
            level = "B2",
            goal = "general_fluency",
            minutesPerDay = 30,
            studyDaysPerWeek = 6
        )
        assertEquals(5, initialChecklist.totalCount)

        // User studies flashcards so flashcard task auto-completes and word_progress is saved
        val startOfDay = FixedMondayTimeProvider().startOfDayMillis("2026-11-02")
        for (i in 1..10) {
            dao.upsertWordProgress(
                WordProgressEntity(
                    cardKey = "oxford5000:word_$i",
                    deck = "oxford5000",
                    text = "word_$i",
                    repetitions = 2,
                    intervalDays = 4,
                    lastReviewedTimestamp = startOfDay + i * 1000L
                )
            )
        }
        val refreshedBeforeRedo = planRepo.getOrCreateDailyChecklist("2026-11-02")
        val tasksBeforeRedo = json.decodeFromString<List<DailyTaskItem>>(refreshedBeforeRedo.tasksJson)
        assertTrue(tasksBeforeRedo.first { it.type == "flashcards" }.isCompleted)

        // User runs Redo Setup and changes goal to it_career and time to 45 min/day
        val checklistAfterRedo = planRepo.saveOnboardingGoalAndGeneratePlan(
            level = "C1",
            goal = "it_career",
            minutesPerDay = 45,
            studyDaysPerWeek = 5
        )

        // Verify completed flashcard task and word progress survived Redo Setup, while uncompleted new_words scaled to 15
        val tasksAfterRedo = json.decodeFromString<List<DailyTaskItem>>(checklistAfterRedo.tasksJson)
        assertTrue(tasksAfterRedo.first { it.type == "flashcards" }.isCompleted)
        assertEquals(15, tasksAfterRedo.first { it.type == "new_words" }.targetCount)
        assertNotNull(dao.getWordProgress("oxford5000:word_1"))
    }

    @Test
    fun `step 3 UI preselects General Fluency, 30 min, 6 days (Friday rest) and saves user selections`() = runBlocking {
        onboardingRepo.setLastCompletedStep(2)
        val snap = onboardingRepo.getSnapshot()
        assertEquals(3, snap.currentStepToResume)
        assertEquals("general_fluency", snap.selectedGoal)
        assertEquals(30, snap.selectedMinutesPerDay)
        assertEquals(6, snap.selectedStudyDaysPerWeek)

        var selectedGoalState by mutableStateOf(snap.selectedGoal)
        var selectedMinutesState by mutableIntStateOf(snap.selectedMinutesPerDay)
        var selectedDaysState by mutableIntStateOf(snap.selectedStudyDaysPerWeek)
        var completedTriple: Triple<String, Int, Int>? = null
        var backClicked = false

        composeTestRule.setContent {
            WordBooksTheme {
                OnboardingScreen(
                    onboardingState = OnboardingPreferencesState(
                        onboardingCompleted = false,
                        lastCompletedStep = 2,
                        selectedLevel = "B2",
                        selectedGoal = selectedGoalState,
                        selectedMinutesPerDay = selectedMinutesState,
                        selectedStudyDaysPerWeek = selectedDaysState
                    ),
                    importState = ImportProgressState(
                        isImporting = false,
                        importedRows = 21624,
                        totalExpectedRows = 21624,
                        isCompleted = true
                    ),
                    totalEntriesCount = 21624,
                    onCompleteStep1 = {},
                    onSelectGoal = { selectedGoalState = it },
                    onSelectMinutesPerDay = { selectedMinutesState = it },
                    onSelectStudyDaysPerWeek = { selectedDaysState = it },
                    onCompleteStep3 = { goal, mins, days ->
                        completedTriple = Triple(goal, mins, days)
                    },
                    onStep3Back = { backClicked = true },
                    onSkipOnboarding = {},
                    onRetryImport = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // 1. Verify Step 3 screen and preselected defaults
        composeTestRule.onNodeWithTag("onboarding_step_3_goal_time").assertIsDisplayed()
        composeTestRule.onNodeWithText("3 / 5").assertIsDisplayed()
        composeTestRule.onNodeWithTag("onboarding_goal_card_general_fluency").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("onboarding_minutes_chip_30").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("onboarding_study_days_chip_6").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("onboarding_step3_rest_day_note").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("onboarding_step3_plan_preview").performScrollTo().assertIsDisplayed()

        // 2. Select IT & Tech Career, 45 min/day, and 5 study days/week
        composeTestRule.onNodeWithTag("onboarding_goal_card_it_career").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals("it_career", selectedGoalState)

        composeTestRule.onNodeWithTag("onboarding_minutes_chip_45").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(45, selectedMinutesState)

        composeTestRule.onNodeWithTag("onboarding_study_days_chip_5").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(5, selectedDaysState)

        // 3. Tap Continue button and verify Step 3 completion callback receives ("it_career", 45, 5)
        composeTestRule.onNodeWithTag("btn_onboarding_step3_continue").performClick()
        composeTestRule.waitForIdle()
        assertEquals(Triple("it_career", 45, 5), completedTriple)

        // 4. Verify Back button works
        composeTestRule.onNodeWithTag("btn_onboarding_step3_back").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertTrue(backClicked)
    }
}
