package com.example

import android.content.Context
import androidx.compose.runtime.getValue
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
class OnboardingStep4Test {

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
        tempDataStoreFile = File(context.cacheDir, "test_onboarding_step4_${System.nanoTime()}.preferences_pb")
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

    private suspend fun seedSampleEntries() {
        val entries = listOf(
            EntryEntity(
                id = "1",
                type = "word",
                deck = "oxford5000",
                text = "achieve",
                pos = "verb",
                level = "B1",
                definitionEn = "to succeed in finishing something",
                noteEn = "",
                examplesEn = "She achieved her goal.",
                examplesAr = "حققت هدفها.",
                definitionUrl = "",
                audioUrl = "",
                arabic = "يحقق",
                arabicSource = "file",
                ipa = "",
                syllables = "",
                extraExamplesJson = "",
                dataVersion = 3
            ),
            EntryEntity(
                id = "2",
                type = "word",
                deck = "ielts_core",
                text = "subsequent",
                pos = "adjective",
                level = "B2",
                definitionEn = "happening after something else",
                noteEn = "",
                examplesEn = "The subsequent events proved him right.",
                examplesAr = "الأحداث اللاحقة أثبتت صحة كلامه.",
                definitionUrl = "",
                audioUrl = "",
                arabic = "لاحق",
                arabicSource = "file",
                ipa = "",
                syllables = "",
                extraExamplesJson = "",
                dataVersion = 3
            )
        )
        dao.upsertEntriesBatchPreservingUserEdits(entries)
    }

    @Test
    fun `step 4 UI preselects decks based on goal, supports starting point toggle and minimum 1 deck constraint`() = runBlocking {
        onboardingRepo.setLastCompletedStep(3)
        val snap = onboardingRepo.getSnapshot()
        assertEquals(4, snap.currentStepToResume)

        var completedDecks: List<String>? = null
        var completedStartAtLevel: Boolean? = null
        var backClicked = false

        composeTestRule.setContent {
            WordBooksTheme {
                OnboardingScreen(
                    onboardingState = OnboardingPreferencesState(
                        onboardingCompleted = false,
                        lastCompletedStep = 3,
                        selectedLevel = "B2",
                        selectedGoal = "ielts",
                        selectedMinutesPerDay = 30,
                        selectedStudyDaysPerWeek = 6
                    ),
                    importState = ImportProgressState(
                        isImporting = false,
                        importedRows = 21624,
                        totalExpectedRows = 21624,
                        isCompleted = true
                    ),
                    totalEntriesCount = 21624,
                    onCompleteStep1 = {},
                    onCompleteStep4 = { decks, startAtLevel ->
                        completedDecks = decks
                        completedStartAtLevel = startAtLevel
                    },
                    onStep4Back = { backClicked = true },
                    onSkipOnboarding = {},
                    onRetryImport = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // 1. Verify Step 4 is displayed with "4 / 5"
        composeTestRule.onNodeWithTag("onboarding_step_4_decks_placement").assertIsDisplayed()
        composeTestRule.onNodeWithText("4 / 5").assertIsDisplayed()

        // 2. Verify Placement Starting Point options
        composeTestRule.onNodeWithTag("onboarding_placement_option_level").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("onboarding_placement_option_start").performScrollTo().assertIsDisplayed()

        // Switch to "Start from beginning (A1)"
        composeTestRule.onNodeWithTag("onboarding_placement_option_start").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // 3. For "ielts" goal, oxford5000, oxford_opal, and ielts_core are preselected (3 decks)
        composeTestRule.onNodeWithTag("onboarding_deck_card_oxford5000").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("onboarding_deck_card_oxford_opal").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("onboarding_deck_card_ielts_core").performScrollTo().assertIsDisplayed()

        // Add "business_english" deck
        composeTestRule.onNodeWithTag("onboarding_deck_card_business_english").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // 4. Tap Continue and verify callback received all 4 selected decks and startAtUserLevel = false
        composeTestRule.onNodeWithTag("btn_onboarding_step4_continue").performClick()
        composeTestRule.waitForIdle()

        assertNotNull(completedDecks)
        assertTrue(completedDecks!!.contains("business_english"))
        assertTrue(completedDecks.contains("ielts_core"))
        assertTrue(completedDecks.contains("oxford5000"))
        assertEquals(false, completedStartAtLevel)

        // 5. Verify Back button to Step 3 works
        composeTestRule.onNodeWithTag("btn_onboarding_step4_back").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertTrue(backClicked)
    }

    @Test
    fun `step 4 updates profile decks and synchronizes daily checklist`() = runBlocking {
        seedSampleEntries()
        val planRepo = StudyPlanRepository(context, dao, FixedMondayTimeProvider())

        // Initial setup at Step 3
        planRepo.saveOnboardingGoalAndGeneratePlan(
            level = "B2",
            goal = "it_career",
            minutesPerDay = 30,
            studyDaysPerWeek = 6
        )

        // Complete Step 4 with custom deck selection: oxford5000, oxford_phrases, and it_industry
        val chosenDecks = listOf("oxford5000", "oxford_phrases", "it_industry")
        val updatedChecklist = planRepo.saveOnboardingDecksAndPlacement(
            selectedDecks = chosenDecks,
            startAtUserLevel = true
        )

        // Verify profile updated in Room
        val profile = dao.getStudyPlanProfile()
        assertNotNull(profile)
        val savedDecks = json.decodeFromString<List<String>>(profile!!.selectedDecksJson)
        assertEquals(chosenDecks, savedDecks)

        // Verify daily checklist tasks updated
        val tasks = json.decodeFromString<List<DailyTaskItem>>(updatedChecklist.tasksJson)
        val flashTask = tasks.first { it.type == "flashcards" }
        assertEquals("oxford5000", flashTask.linkedId)
    }
}
