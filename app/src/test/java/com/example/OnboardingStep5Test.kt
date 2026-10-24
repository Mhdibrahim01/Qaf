package com.example

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.BatchTranslateManager
import com.example.data.ai.GeminiDeckAndTranslationService
import com.example.data.importer.ImportProgressState
import com.example.data.local.DailyTaskItem
import com.example.data.local.EntryEntity
import com.example.data.local.StudyPlanProfileEntity
import com.example.data.local.WordBooksDao
import com.example.data.local.WordBooksDatabase
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
import org.junit.Assert.assertFalse
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
class OnboardingStep5Test {

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
        tempDataStoreFile = File(context.cacheDir, "test_onboarding_step5_${System.nanoTime()}.preferences_pb")
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

    @Test
    fun `step 5 UI defaults AI to OFF, shows 100 percent offline notice, profile summary, and handles start launch`() = runBlocking {
        onboardingRepo.setLastCompletedStep(4)
        val snap = onboardingRepo.getSnapshot()
        assertEquals(5, snap.currentStepToResume)

        var completedAllowAi: Boolean? = null
        var backClicked = false

        composeTestRule.setContent {
            WordBooksTheme {
                OnboardingScreen(
                    onboardingState = OnboardingPreferencesState(
                        onboardingCompleted = false,
                        lastCompletedStep = 4,
                        selectedLevel = "B2",
                        selectedGoal = "general_fluency",
                        selectedMinutesPerDay = 30,
                        selectedStudyDaysPerWeek = 6,
                        selectedDecksJson = "[\"oxford5000\",\"oxford_opal\"]"
                    ),
                    importState = ImportProgressState(
                        isImporting = false,
                        importedRows = 21624,
                        totalExpectedRows = 21624,
                        isCompleted = true
                    ),
                    totalEntriesCount = 21624,
                    initialAllowAi = false,
                    onCompleteStep1 = {},
                    onCompleteStep5 = { allowAi ->
                        completedAllowAi = allowAi
                    },
                    onStep5Back = { backClicked = true },
                    onSkipOnboarding = {},
                    onRetryImport = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // 1. Verify Step 5 is displayed with "5 / 5"
        composeTestRule.onNodeWithTag("onboarding_step_5_ai_launch").assertIsDisplayed()
        composeTestRule.onNodeWithText("5 / 5").assertIsDisplayed()

        // 2. Verify AI switch defaults to OFF
        composeTestRule.onNodeWithTag("switch_onboarding_allow_ai").assertIsOff()

        // 3. Verify Offline guarantee and plan summary cards
        composeTestRule.onNodeWithTag("onboarding_step5_offline_note").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("onboarding_step5_plan_summary").performScrollTo().assertIsDisplayed()

        // 4. Toggle AI switch to ON
        composeTestRule.onNodeWithTag("switch_onboarding_allow_ai").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("switch_onboarding_allow_ai").assertIsOn()

        // 5. Tap Start Learning button and verify callback received allowAi = true
        composeTestRule.onNodeWithTag("btn_onboarding_step5_start").performClick()
        composeTestRule.waitForIdle()
        assertEquals(true, completedAllowAi)

        // 6. Verify Back button to Step 4 works
        composeTestRule.onNodeWithTag("btn_onboarding_step5_back").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertTrue(backClicked)
    }

    @Test
    fun `step 5 completion persists allowAi and marks onboarding completed`() = runBlocking {
        val planRepo = StudyPlanRepository(context, dao, FixedMondayTimeProvider())
        planRepo.saveOnboardingGoalAndGeneratePlan(
            level = "B2",
            goal = "general_fluency",
            minutesPerDay = 30,
            studyDaysPerWeek = 6
        )

        val geminiService = GeminiDeckAndTranslationService(dao = dao, apiKeyProvider = { "" })
        val batchTranslateManager = BatchTranslateManager(context, dao, geminiService)
        batchTranslateManager.setAllowAi(false)
        assertFalse(batchTranslateManager.uiState.value.allowAi)

        // Simulate completing Step 5
        batchTranslateManager.setAllowAi(true)
        assertTrue(batchTranslateManager.uiState.value.allowAi)

        onboardingRepo.markOnboardingCompleted()
        val finalState = onboardingRepo.getSnapshot()
        assertTrue(finalState.onboardingCompleted)
        assertEquals(5, finalState.lastCompletedStep)

        // Verify today's checklist exists and has all 5 pillars
        val checklist = planRepo.getOrCreateDailyChecklist("2026-11-02")
        assertEquals(5, checklist.totalCount)
    }
}
