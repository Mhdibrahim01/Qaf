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
import com.example.data.importer.DataImportRepository
import com.example.data.importer.ImportProgressState
import com.example.data.local.EntryEntity
import com.example.data.local.GrammarLessonEntity
import com.example.data.local.WordBooksDao
import com.example.data.local.WordBooksDatabase
import com.example.data.onboarding.OnboardingPlacementQuiz
import com.example.data.onboarding.OnboardingPreferencesRepository
import com.example.data.onboarding.OnboardingPreferencesState
import com.example.data.onboarding.PlacementQuestionType
import com.example.data.onboarding.PlacementQuizQuestion
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PlacementQuizUiState
import com.example.ui.theme.WordBooksTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OnboardingStep2Test {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var database: WordBooksDatabase
    private lateinit var dao: WordBooksDao
    private lateinit var tempDataStoreFile: File
    private lateinit var onboardingRepo: OnboardingPreferencesRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, WordBooksDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.wordBooksDao()

        tempDataStoreFile = File(context.cacheDir, "test_onboarding_step2_${System.nanoTime()}.preferences_pb")
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = { tempDataStoreFile }
        )
        onboardingRepo = OnboardingPreferencesRepository(context, testDataStore)
    }

    @After
    fun tearDown() {
        database.close()
        if (::tempDataStoreFile.isInitialized && tempDataStoreFile.exists()) {
            tempDataStoreFile.delete()
        }
    }

    @Test
    fun `bundled dictionary expected row count matches all 14 JSON files (21624) and core 7 files (9862)`() {
        assertEquals(9862, DataImportRepository.CORE_EXPECTED_ROWS)
        assertEquals(21624, DataImportRepository.TOTAL_EXPECTED_ROWS)
        assertEquals(14, DataImportRepository.EXPECTED_FILE_ROW_COUNTS.size)
        assertEquals(
            21624,
            DataImportRepository.EXPECTED_FILE_ROW_COUNTS.values.sum()
        )
        val defaultImportState = ImportProgressState(importedRows = 14437)
        assertEquals(21624, defaultImportState.totalExpectedRows)
        assertTrue(defaultImportState.progressFraction < 1.0f)
    }

    @Test
    fun `placement scoring walks A1 to C1 and stops at first level with fewer than 2 correct`() {
        // 1. User who scores A1 2/2, A2 2/2, B1 1/2 gets B1
        val scoreB1 = OnboardingPlacementQuiz.calculateSuggestedLevel(
            mapOf("A1" to 2, "A2" to 2, "B1" to 1, "B2" to 0, "C1" to 0)
        )
        assertEquals("B1", scoreB1)

        // 2. User who scores A1 2/2, A2 1/2 gets A2
        val scoreA2 = OnboardingPlacementQuiz.calculateSuggestedLevel(
            mapOf("A1" to 2, "A2" to 1, "B1" to 0, "B2" to 0, "C1" to 0)
        )
        assertEquals("A2", scoreA2)

        // 3. User who scores 0/2 at A1 gets A1
        val scoreA1Zero = OnboardingPlacementQuiz.calculateSuggestedLevel(
            mapOf("A1" to 0, "A2" to 0, "B1" to 0, "B2" to 0, "C1" to 0)
        )
        assertEquals("A1", scoreA1Zero)

        // 4. A lucky C1 pass after a failed A2 (0/2 at A2) does NOT suggest C1 -> suggests A1
        val luckyC1AfterFailedA2 = OnboardingPlacementQuiz.calculateSuggestedLevel(
            mapOf("A1" to 2, "A2" to 0, "B1" to 2, "B2" to 2, "C1" to 2)
        )
        assertEquals("A1", luckyC1AfterFailedA2)

        // 5. All 2/2 across A1..C1 suggests C1
        val fullC1 = OnboardingPlacementQuiz.calculateSuggestedLevel(
            mapOf("A1" to 2, "A2" to 2, "B1" to 2, "B2" to 2, "C1" to 2)
        )
        assertEquals("C1", fullC1)
    }

    @Test
    fun `placement quiz builds 10 local questions with vocab and grammar or 2 vocab fallback when grammar unavailable`() = runBlocking {
        // Seed 5 vocabulary entries per level A1..C1 with non-empty Arabic
        val seededEntries = mutableListOf<EntryEntity>()
        var idCounter = 1
        for (level in OnboardingPlacementQuiz.CEFR_LEVELS) {
            for (i in 1..5) {
                seededEntries.add(
                    EntryEntity(
                        id = "${idCounter++}",
                        type = "word",
                        deck = "oxford5000",
                        text = "${level.lowercase()}_word_$i",
                        pos = "noun",
                        level = level,
                        definitionEn = "Definition $i for $level",
                        noteEn = "",
                        examplesEn = "",
                        examplesAr = "",
                        definitionUrl = "",
                        audioUrl = "",
                        arabic = "معنى ${level}_$i",
                        arabicSource = "file",
                        ipa = "",
                        syllables = "",
                        extraExamplesJson = "",
                        dataVersion = 3
                    )
                )
            }
        }
        dao.upsertEntriesBatchPreservingUserEdits(seededEntries)

        // Case A: When no grammar lessons exist for levels, builder uses 2 vocabulary questions per level (10 total)
        val vocabOnlyQuestions = OnboardingPlacementQuiz.buildPlacementQuizQuestions(dao, seed = 42L)
        assertNotNull(vocabOnlyQuestions)
        assertEquals(10, vocabOnlyQuestions!!.size)
        assertTrue(vocabOnlyQuestions.all { it.type == PlacementQuestionType.VOCABULARY })

        // Case B: Seed grammar lessons for A1..C1 and verify each level gets 1 vocab + 1 grammar question
        for (level in OnboardingPlacementQuiz.CEFR_LEVELS) {
            dao.upsertGrammarLesson(
                GrammarLessonEntity(
                    id = "g_$level",
                    level = level,
                    topic = "Grammar $level",
                    titleEn = "$level Structure",
                    titleAr = "قاعدة $level",
                    explanationAr = "شرح $level",
                    examplesJson = "[]",
                    exercisesJson = """
                        [
                          {
                            "type": "mcq",
                            "prompt": "Choose the correct $level form:",
                            "options": ["opt_a_$level", "opt_b_$level", "opt_c_$level", "opt_d_$level"],
                            "answer": "opt_b_$level",
                            "explanation_ar": "شرح"
                          }
                        ]
                    """.trimIndent()
                )
            )
        }

        val mixedQuestions = OnboardingPlacementQuiz.buildPlacementQuizQuestions(dao, seed = 99L)
        assertNotNull(mixedQuestions)
        assertEquals(10, mixedQuestions!!.size)
        for (level in OnboardingPlacementQuiz.CEFR_LEVELS) {
            val forLevel = mixedQuestions.filter { it.level == level }
            assertEquals(2, forLevel.size)
            assertEquals(1, forLevel.count { it.type == PlacementQuestionType.VOCABULARY })
            assertEquals(1, forLevel.count { it.type == PlacementQuestionType.GRAMMAR })
        }

        // Verify I_DONT_KNOW_OPTION counts as incorrect in scorePlacementQuiz
        val answers = mixedQuestions.associate { q ->
            when (q.level) {
                "A1", "A2" -> q.id to q.correctAnswer
                "B1" -> if (q.type == PlacementQuestionType.VOCABULARY) {
                    q.id to q.correctAnswer
                } else {
                    q.id to OnboardingPlacementQuiz.I_DONT_KNOW_OPTION
                }
                else -> q.id to OnboardingPlacementQuiz.I_DONT_KNOW_OPTION
            }
        }
        val (suggested, counts) = OnboardingPlacementQuiz.scorePlacementQuiz(mixedQuestions, answers)
        assertEquals(2, counts["A1"])
        assertEquals(2, counts["A2"])
        assertEquals(1, counts["B1"])
        assertEquals(0, counts["B2"])
        assertEquals("B1", suggested)
    }

    @Test
    fun `enough entries check waits for 100 entries when import running and never waits when import complete`() = runBlocking {
        // Only 4 entries per level seeded
        var idCounter = 1
        val smallSeed = OnboardingPlacementQuiz.CEFR_LEVELS.flatMap { level ->
            (1..4).map { idx ->
                EntryEntity(
                    id = "${idCounter++}",
                    type = "word",
                    deck = "oxford5000",
                    text = "${level}_small_$idx",
                    pos = "noun",
                    level = level,
                    definitionEn = "Def",
                    noteEn = "",
                    examplesEn = "",
                    examplesAr = "",
                    definitionUrl = "",
                    audioUrl = "",
                    arabic = "عربي_${level}_$idx",
                    arabicSource = "file",
                    ipa = "",
                    syllables = "",
                    extraExamplesJson = "",
                    dataVersion = 3
                )
            }
        }
        dao.upsertEntriesBatchPreservingUserEdits(smallSeed)

        // When import is NOT complete, 4 < 100 so hasEnoughEntriesForPlacementQuiz is false
        assertFalse(
            OnboardingPlacementQuiz.hasEnoughEntriesForPlacementQuiz(
                dao = dao,
                isImportCompleted = false
            )
        )
        // When import IS complete, never waits on the 100-entry threshold
        assertTrue(
            OnboardingPlacementQuiz.hasEnoughEntriesForPlacementQuiz(
                dao = dao,
                isImportCompleted = true
            )
        )
    }

    @Test
    fun `step 2 UI preselects B2, displays 21624 import total, and supports placement quiz I dont know and editable rough estimate`() = runBlocking {
        // Simulate Step 1 completed -> resumes at Step 2
        onboardingRepo.setLastCompletedStep(1)
        val snapBefore = onboardingRepo.getSnapshot()
        assertEquals(2, snapBefore.currentStepToResume)
        assertEquals("B2", snapBefore.selectedLevel)

        var selectedLevelState by mutableStateOf(snapBefore.selectedLevel)
        var completedWithLevel: String? = null
        var quizState by mutableStateOf(PlacementQuizUiState())
        var lastAnsweredOption: String? = null
        var retakeCount = 0

        val sampleQuestion = PlacementQuizQuestion(
            id = "vocab_A1_1",
            level = "A1",
            type = PlacementQuestionType.VOCABULARY,
            promptEn = "Choose the English word that matches this meaning:",
            promptAr = "يحقق، ينجز",
            topicLabelEn = "Vocabulary Recall",
            topicLabelAr = "مفردات",
            options = listOf("achieve", "abandon", "benefit", "create"),
            correctAnswer = "achieve"
        )

        composeTestRule.setContent {
            WordBooksTheme {
                OnboardingScreen(
                    onboardingState = OnboardingPreferencesState(
                        onboardingCompleted = false,
                        lastCompletedStep = 1,
                        selectedLevel = selectedLevelState
                    ),
                    importState = ImportProgressState(
                        isImporting = true,
                        importedRows = 14437,
                        totalExpectedRows = 21624,
                        isCompleted = false
                    ),
                    totalEntriesCount = 14437,
                    placementQuizState = quizState,
                    onCompleteStep1 = {},
                    onSelectLevel = { selectedLevelState = it },
                    onCompleteStep2 = { completedWithLevel = it },
                    onStartPlacementQuiz = {
                        quizState = PlacementQuizUiState(
                            isQuizActive = true,
                            questions = listOf(sampleQuestion),
                            currentQuestionIndex = 0
                        )
                    },
                    onPlacementQuizBack = {
                        quizState = PlacementQuizUiState(isQuizActive = false)
                    },
                    onAnswerPlacementQuestion = { option ->
                        lastAnsweredOption = option
                        quizState = quizState.copy(
                            isQuizFinished = true,
                            suggestedLevel = "B1",
                            editableLevel = "B1",
                            correctCountByLevel = mapOf("A1" to 2, "A2" to 2, "B1" to 1, "B2" to 0, "C1" to 0)
                        )
                    },
                    onRetakePlacementQuiz = {
                        retakeCount++
                        quizState = quizState.copy(isQuizFinished = false, currentQuestionIndex = 0)
                    },
                    onUpdateQuizEditableLevel = { lvl ->
                        quizState = quizState.copy(editableLevel = lvl)
                    },
                    onSkipOnboarding = {},
                    onRetryImport = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // 1. Verify Step 2 is displayed with B2 preselected and 14437 / 21624 in the import status bar
        composeTestRule.onNodeWithTag("onboarding_step_2_level").assertIsDisplayed()
        composeTestRule.onNodeWithTag("onboarding_level_card_B2").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("14437 / 21624").assertIsDisplayed()

        // 2. Tap "Not sure" button to launch placement quiz
        composeTestRule.onNodeWithTag("btn_onboarding_not_sure").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("onboarding_placement_quiz_card").assertIsDisplayed()

        // 3. Verify "I don't know" option exists and can be clicked
        composeTestRule.onNodeWithTag("onboarding_quiz_option_idontknow").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(OnboardingPlacementQuiz.I_DONT_KNOW_OPTION, lastAnsweredOption)

        // 4. Verify quiz result shows rough estimate notice, suggested level B1, Retake, and editable level chips
        composeTestRule.onNodeWithTag("onboarding_placement_quiz_result").assertIsDisplayed()
        composeTestRule.onNodeWithTag("onboarding_quiz_rough_estimate_label").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("onboarding_quiz_suggested_level").performScrollTo().assertIsDisplayed()

        // Edit the suggested level from B1 to A2 before confirming
        composeTestRule.onNodeWithTag("onboarding_quiz_edit_level_A2").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Confirm quiz level -> completes Step 2 with A2
        composeTestRule.onNodeWithTag("btn_onboarding_quiz_confirm_level").performClick()
        composeTestRule.waitForIdle()
        assertEquals("A2", completedWithLevel)
    }
}
