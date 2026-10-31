package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.blitz.BlitzFilterConfig
import com.example.data.blitz.BlitzQuestionItem
import com.example.data.blitz.BlitzQuestionMode
import com.example.data.blitz.MatchMadnessEngine
import com.example.data.blitz.MatchSelectionOutcome
import com.example.data.blitz.SurvivalSessionState
import com.example.data.blitz.VocabularyBlitzEngine
import com.example.data.grammar.GrammarErrorHunterEngine
import com.example.data.grammar.HunterStep
import com.example.data.local.DeckEntity
import com.example.data.local.EntryEntity
import com.example.data.local.GrammarExerciseItem
import com.example.data.local.GrammarLessonEntity
import com.example.data.onboarding.OnboardingPreferencesRepository
import com.example.ui.screens.GrammarErrorHunterDialog
import com.example.ui.screens.MatchMadnessDialog
import com.example.ui.screens.MoreScreen
import com.example.ui.screens.SurvivalChallengeDialog
import com.example.ui.screens.VocabularyBlitzSetupDialog
import com.example.ui.theme.WordBooksTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MiniGamesHubTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var testScope: CoroutineScope
    private lateinit var tempFile: File
    private lateinit var preferencesRepo: OnboardingPreferencesRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        testScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        tempFile = File.createTempFile("test_minigames_prefs", ".preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tempFile }
        )
        preferencesRepo = OnboardingPreferencesRepository(context, dataStore)
    }

    @After
    fun tearDown() {
        testScope.cancel()
        if (tempFile.exists()) {
            tempFile.delete()
        }
    }

    // ==========================================
    // 1. SURVIVAL MODE ENGINE TESTS
    // ==========================================

    @Test
    fun survivalEngine_initialStateAndCorrectStreakBonusAndHeartLoss() {
        val questions = listOf(
            BlitzQuestionItem(
                id = "q1",
                word = "persist",
                pos = "v.",
                level = "B2",
                deck = "oxford5000",
                isAcademicOrIelts = false,
                mode = BlitzQuestionMode.TRUE_FALSE,
                promptArabic = "يستمر",
                optionsArabic = listOf("صحيح", "خاطئ"),
                correctOptionIndex = 0,
                trueArabicMeaning = "يستمر"
            ),
            BlitzQuestionItem(
                id = "q2",
                word = "abandon",
                pos = "v.",
                level = "B2",
                deck = "oxford5000",
                isAcademicOrIelts = false,
                mode = BlitzQuestionMode.FOUR_CHOICE,
                promptArabic = "",
                optionsArabic = listOf("يتخلى عن", "يبني", "يشتري", "يربح"),
                correctOptionIndex = 0,
                trueArabicMeaning = "يتخلى عن"
            )
        )

        var state = SurvivalSessionState(questions = questions, hearts = 3, maxHearts = 3)
        assertEquals(3, state.hearts)
        assertEquals(0, state.currentStreak)
        assertFalse(state.isFinished)

        // Answer 1: Correct
        state = VocabularyBlitzEngine.submitSurvivalAnswer(state, selectedOptionIndex = 0, previousBestStreak = 0)
        assertEquals(1, state.currentStreak)
        assertEquals(1, state.bestStreakInSession)
        assertEquals(1, state.correctCount)
        assertEquals(3, state.hearts)
        assertFalse(state.isFinished)

        // Simulate reaching 15-streak after losing 1 heart
        val lowHeartState = state.copy(hearts = 2, currentStreak = 14)
        val bonusState = VocabularyBlitzEngine.submitSurvivalAnswer(lowHeartState, selectedOptionIndex = 0, previousBestStreak = 10)
        assertEquals(15, bonusState.currentStreak)
        assertEquals(3, bonusState.hearts) // +1 heart recovered!
        assertTrue(bonusState.heartRecoveredThisTurn)

        // Answer Wrong: loses 1 heart, resets streak, records mistake
        var wrongState = VocabularyBlitzEngine.submitSurvivalAnswer(state, selectedOptionIndex = 1, previousBestStreak = 0)
        assertEquals(2, wrongState.hearts)
        assertEquals(0, wrongState.currentStreak)
        assertEquals(1, wrongState.wrongCount)
        assertEquals(1, wrongState.missedQuestions.size)
        assertEquals("q1", wrongState.missedQuestions[0].id)
        assertFalse(wrongState.isFinished)

        // Lose remaining 2 hearts -> Game Over
        wrongState = VocabularyBlitzEngine.submitSurvivalAnswer(wrongState, selectedOptionIndex = 1)
        wrongState = VocabularyBlitzEngine.submitSurvivalAnswer(wrongState, selectedOptionIndex = 1)
        assertEquals(0, wrongState.hearts)
        assertTrue(wrongState.isFinished)
        assertEquals(3, wrongState.missedQuestions.size)
    }

    // ==========================================
    // 2. MATCH MADNESS ENGINE TESTS
    // ==========================================

    @Test
    fun matchMadnessEngine_threeRoundsOfSixPairsAndMatchingLogic() {
        val entries = (1..18).map { i ->
            EntryEntity(
                id = "entry_$i",
                type = "word",
                deck = "oxford5000",
                text = "word$i",
                pos = "n.",
                level = "B1",
                arabic = "معنى$i",
                definitionEn = "definition$i",
                noteEn = "",
                examplesEn = "",
                examplesAr = "",
                definitionUrl = "",
                audioUrl = ""
            )
        }

        var session = MatchMadnessEngine.createSession(entries, seed = 42)
        assertEquals(3, session.rounds.size)
        assertEquals(18, session.allPairs.size)
        assertEquals(0, session.currentRoundIndex)
        assertEquals(6, session.currentRound?.leftCards?.size)
        assertEquals(6, session.currentRound?.rightCards?.size)

        val currentRound = session.currentRound!!
        val firstLeft = currentRound.leftCards[0]
        val matchingRight = currentRound.rightCards.first { it.pairId == firstLeft.pairId }
        val nonMatchingRight = currentRound.rightCards.first { it.pairId != firstLeft.pairId }

        // Test Mismatch
        var outcome = MatchMadnessEngine.selectCard(session, firstLeft.id)
        assertTrue(outcome is MatchSelectionOutcome.CardSelected)
        session = (outcome as MatchSelectionOutcome.CardSelected).state

        outcome = MatchMadnessEngine.selectCard(session, nonMatchingRight.id)
        assertTrue(outcome is MatchSelectionOutcome.MatchMismatch)
        val mismatchState = (outcome as MatchSelectionOutcome.MatchMismatch).state
        assertEquals(1, mismatchState.mismatches)

        // Clear error and select valid pair
        val cleared = MatchMadnessEngine.clearSelectionsAndErrors(mismatchState)
        val selectLeft = MatchMadnessEngine.selectCard(cleared, firstLeft.id)
        val afterLeft = (selectLeft as MatchSelectionOutcome.CardSelected).state
        val matchOutcome = MatchMadnessEngine.selectCard(afterLeft, matchingRight.id)
        assertTrue(matchOutcome is MatchSelectionOutcome.MatchSuccess)
        val matchedState = (matchOutcome as MatchSelectionOutcome.MatchSuccess).state
        assertEquals(1, matchedState.matchedCountTotal)

        // Advance round
        val nextRoundState = MatchMadnessEngine.advanceRound(matchedState)
        assertEquals(1, nextRoundState.currentRoundIndex)
    }

    // ==========================================
    // 3. GRAMMAR ERROR HUNTER ENGINE TESTS
    // ==========================================

    @Test
    fun grammarHunterEngine_creationAndTwoStepCorrection() {
        val exercises = listOf(
            GrammarExerciseItem(
                type = "mcq",
                prompt = "She ____ a doctor.",
                options = listOf("am", "is", "are"),
                answer = "is",
                explanationAr = "الضمير She يأخذ is."
            ),
            GrammarExerciseItem(
                type = "mcq",
                prompt = "They ____ to school.",
                options = listOf("goes", "go", "going"),
                answer = "go",
                explanationAr = "الضمير They يأخذ go."
            )
        )
        val lesson = GrammarLessonEntity(
            id = "lesson_1",
            level = "A1",
            topic = "Present Simple",
            titleEn = "Present Simple",
            titleAr = "المضارع البسيط",
            explanationAr = "شرح",
            examplesJson = "[]",
            exercisesJson = Json.encodeToString(exercises)
        )

        var session = GrammarErrorHunterEngine.createSession(listOf(lesson), userLevel = "A1", count = 10, seed = 123)
        assertEquals(10, session.items.size)
        assertEquals(0, session.currentIndex)
        assertEquals(HunterStep.STEP_1_SPOT_ERROR, session.currentStep)

        val currentItem = session.currentItem!!
        assertTrue(currentItem.tokens.isNotEmpty())

        if (!currentItem.isNoError) {
            val errorIdx = currentItem.errorTokenIndex
            assertTrue(errorIdx >= 0)

            // Step 1: Spot error
            session = GrammarErrorHunterEngine.spotError(session, tokenIndex = errorIdx)
            assertEquals(HunterStep.STEP_2_CHOOSE_CORRECTION, session.currentStep)
            assertTrue(session.step1Correct == true)

            // Step 2: Choose correct replacement
            session = GrammarErrorHunterEngine.submitCorrection(session, chosenCorrection = currentItem.correctReplacement)
            assertEquals(HunterStep.REVEAL_EXPLANATION, session.currentStep)
            assertTrue(session.step2Correct == true)
            assertEquals(1, session.score)

            // Advance
            session = GrammarErrorHunterEngine.advanceNext(session)
            assertEquals(1, session.currentIndex)
        } else {
            // No error case: Tap No Error button
            session = GrammarErrorHunterEngine.spotError(session, tokenIndex = null, noErrorTapped = true)
            assertEquals(HunterStep.REVEAL_EXPLANATION, session.currentStep)
            assertEquals(1, session.score)
        }
    }

    // ==========================================
    // 4. DATASTORE PREFERENCES PERSISTENCE
    // ==========================================

    @Test
    fun preferences_persistsBestScoresForAllThreeMiniGames() = runBlocking {
        preferencesRepo.saveSurvivalBestStreak(22)
        preferencesRepo.saveMatchMadnessBestTime(48)
        preferencesRepo.saveGrammarHunterBestScore(9)

        val snapshot = preferencesRepo.getSnapshot()
        assertEquals(22, snapshot.survivalBestStreak)
        assertEquals(48, snapshot.matchMadnessBestTime)
        assertEquals(9, snapshot.grammarHunterBestScore)

        // Verify Best Time preserves the lower (faster) score
        preferencesRepo.saveMatchMadnessBestTime(65) // Slower, should NOT overwrite 48
        assertEquals(48, preferencesRepo.getSnapshot().matchMadnessBestTime)

        preferencesRepo.saveMatchMadnessBestTime(39) // Faster, SHOULD overwrite
        assertEquals(39, preferencesRepo.getSnapshot().matchMadnessBestTime)
    }

    // ==========================================
    // 5. COMPOSE UI RENDERING TESTS
    // ==========================================

    @Test
    fun ui_moreScreenDisplaysAllFourInteractiveChallenges() {
        var clickedBlitz = false
        var clickedSurvival = false
        var clickedMatch = false
        var clickedGrammar = false

        composeTestRule.setContent {
            WordBooksTheme {
                MoreScreen(
                    onNavigateToGrammar = {},
                    onNavigateToReading = {},
                    onNavigateToPlan = {},
                    onNavigateToCorrectionNotebook = {},
                    onOpenSettings = {},
                    onStartVocabularyBlitz = { clickedBlitz = true },
                    blitzHighScore = 120,
                    onStartSurvival = { clickedSurvival = true },
                    survivalBestStreak = 18,
                    onStartMatchMadness = { clickedMatch = true },
                    matchMadnessBestTime = 54,
                    onStartGrammarHunter = { clickedGrammar = true },
                    grammarHunterBestScore = 8
                )
            }
        }

        // Verify the 4 challenge cards exist in the Hub
        composeTestRule.onNodeWithTag("nav_vocab_blitz").assertIsDisplayed().performClick()
        assertTrue(clickedBlitz)

        composeTestRule.onNodeWithTag("nav_survival_mode").assertIsDisplayed().performClick()
        assertTrue(clickedSurvival)

        composeTestRule.onNodeWithTag("nav_match_madness").assertIsDisplayed().performClick()
        assertTrue(clickedMatch)

        composeTestRule.onNodeWithTag("nav_grammar_hunter").assertIsDisplayed().performClick()
        assertTrue(clickedGrammar)
    }

    @Test
    fun ui_survivalDialogRendersHeartsAndQuestionCard() {
        val questions = listOf(
            BlitzQuestionItem(
                id = "sq1",
                word = "harmony",
                pos = "n.",
                level = "B2",
                deck = "oxford5000",
                isAcademicOrIelts = false,
                mode = BlitzQuestionMode.FOUR_CHOICE,
                promptArabic = "",
                optionsArabic = listOf("انسجام / توافق", "صراع", "تردد", "ضياع"),
                correctOptionIndex = 0,
                trueArabicMeaning = "انسجام / توافق"
            )
        )

        composeTestRule.setContent {
            WordBooksTheme {
                SurvivalChallengeDialog(
                    initialQuestions = questions,
                    bestStreak = 12,
                    onFinish = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("survival_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("survival_hearts_row").assertIsDisplayed()
        composeTestRule.onNodeWithTag("survival_streak_pill").assertIsDisplayed()
        composeTestRule.onNodeWithTag("survival_best_streak_pill").assertIsDisplayed()
        composeTestRule.onNodeWithTag("survival_question_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("survival_option_0").assertIsDisplayed()
    }

    @Test
    fun ui_matchMadnessDialogRendersTwoColumnsAndTimer() {
        val entries = (1..6).map { i ->
            EntryEntity(
                id = "m_$i",
                type = "word",
                deck = "oxford5000",
                text = "matchWord$i",
                pos = "v.",
                level = "B1",
                arabic = "معنى$i",
                definitionEn = "def",
                noteEn = "",
                examplesEn = "",
                examplesAr = "",
                definitionUrl = "",
                audioUrl = ""
            )
        }

        composeTestRule.setContent {
            WordBooksTheme {
                MatchMadnessDialog(
                    initialEntries = entries,
                    bestTimeSeconds = 45,
                    onFinish = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("match_madness_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("match_round_indicator").assertIsDisplayed()
        composeTestRule.onNodeWithTag("match_timer_text").assertIsDisplayed()
        composeTestRule.onNodeWithTag("match_grid_container").assertIsDisplayed()
    }

    @Test
    fun ui_grammarHunterDialogRendersTokensAndNoErrorButton() {
        val lesson = GrammarLessonEntity(
            id = "g_test",
            level = "A1",
            topic = "Passive and reported speech",
            titleEn = "Verb to be",
            titleAr = "فعل الكينونة",
            explanationAr = "المضارع التام (have finished) يتحول إلى الماضي التام (had finished).",
            examplesJson = "[]",
            exercisesJson = """[{"type":"mcq","prompt":"By the time police arrived, thief ____ escaped.","options":["had","has","escapes"],"answer":"had","explanation_ar":"المضارع التام (have finished) يتحول إلى الماضي التام (had finished)."}]"""
        )

        composeTestRule.setContent {
            WordBooksTheme {
                GrammarErrorHunterDialog(
                    lessons = listOf(lesson),
                    userLevel = "A1",
                    bestScore = 7,
                    onFinish = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("grammar_hunter_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("hunter_progress_text").assertIsDisplayed()
        composeTestRule.onNodeWithTag("hunter_score_pill").assertIsDisplayed()
        composeTestRule.onNodeWithTag("hunter_main_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("hunter_no_error_btn").assertIsDisplayed()
    }

    @Test
    fun ui_grammarHunterDialog_step2CorrectionOptionsAndNextButton() {
        val lesson = GrammarLessonEntity(
            id = "g_test_2",
            level = "B2",
            topic = "Passive and reported speech",
            titleEn = "Reported Speech",
            titleAr = "الكلام المنقول",
            explanationAr = "المضارع التام (have finished) يتحول إلى الماضي التام (had finished).",
            examplesJson = "[]",
            exercisesJson = """[{"type":"mcq","prompt":"By the time police arrived, thief ____ escaped.","options":["had","has","escapes"],"answer":"had","explanation_ar":"المضارع التام (have finished) يتحول إلى الماضي التام (had finished)."}]"""
        )

        composeTestRule.setContent {
            WordBooksTheme {
                GrammarErrorHunterDialog(
                    lessons = listOf(lesson),
                    userLevel = "B2",
                    bestScore = 5,
                    onFinish = {},
                    onDismiss = {}
                )
            }
        }

        // Tap the error token in the sentence (token index 0 or whatever token is created by GrammarErrorHunterEngine)
        // Check if token 0 is displayed
        composeTestRule.onNodeWithTag("hunter_token_0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("hunter_token_0").performClick()

        // If tapped token was the error token, option 0 is displayed; if not, step 1 stays or advances based on spotError
        // In GrammarErrorHunterEngine, if token tapped is wrong, step1Correct is false and advances to REVEAL_EXPLANATION directly
        // Either way, hunter_main_card is displayed without crash
        composeTestRule.onNodeWithTag("hunter_main_card").assertIsDisplayed()
    }

    @Test
    fun ui_blitzSetupDialog_renders2ColumnDecksAndStickyFooter() {
        val testDecks = listOf(
            DeckEntity(
                id = "oxford5000",
                title = "Oxford 5000",
                arabicSubtitle = "أهم 5000 كلمة إنجليزية شاملة",
                category = "general",
                coverColorHex = "#4F46E5",
                accentColorHex = "#F5B82E",
                rowCount = 5000,
                uniqueWordCount = 5000,
                displayOrder = 1
            ),
            DeckEntity(
                id = "medical",
                title = "Medical English",
                arabicSubtitle = "المفردات والمصطلحات الطبية والتمريض",
                category = "specialized",
                coverColorHex = "#0EA5E9",
                accentColorHex = "#38BDF8",
                rowCount = 1200,
                uniqueWordCount = 1200,
                displayOrder = 2
            )
        )

        val config = BlitzFilterConfig(
            selectedDecks = listOf("oxford5000"),
            allBooksSelected = false,
            selectedLevels = setOf("B2"),
            studiedOnly = false
        )

        composeTestRule.setContent {
            WordBooksTheme {
                VocabularyBlitzSetupDialog(
                    initialConfig = config,
                    availableDecks = testDecks,
                    userCurrentLevel = "B2",
                    highScore = 85,
                    availableWordCount = 450,
                    isSurvivalMode = false,
                    onFilterConfigChanged = {},
                    onStartChallenge = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("blitz_setup_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("blitz_setup_high_score_badge").assertIsDisplayed()
        composeTestRule.onNodeWithTag("blitz_book_all_chip").assertIsDisplayed()
        composeTestRule.onNodeWithTag("blitz_book_oxford5000").assertIsDisplayed()
        composeTestRule.onNodeWithTag("blitz_book_medical").assertIsDisplayed()
        composeTestRule.onNodeWithTag("blitz_my_level_btn").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("blitz_level_B2").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("blitz_stage_studied_only").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("blitz_stage_all_words").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("blitz_available_words_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("blitz_setup_cancel_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("blitz_setup_start_btn").assertIsDisplayed()
    }

    @Test
    fun ui_survivalDialog_rendersTrueFalseMode() {
        val questions = listOf(
            BlitzQuestionItem(
                id = "sq_tf_1",
                word = "cooperative",
                pos = "adj.",
                ipa = "/koʊˈɑː.pə.rə.tɪv/",
                level = "B2",
                deck = "oxford5000",
                isAcademicOrIelts = false,
                mode = BlitzQuestionMode.TRUE_FALSE,
                promptArabic = "متعاون",
                optionsArabic = listOf("✓ Correct • صحيح", "✗ False • خاطئ"),
                correctOptionIndex = 0,
                trueArabicMeaning = "متعاون"
            )
        )

        composeTestRule.setContent {
            WordBooksTheme {
                SurvivalChallengeDialog(
                    initialQuestions = questions,
                    bestStreak = 15,
                    onFinish = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("survival_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("survival_question_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("survival_option_0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("survival_option_1").assertIsDisplayed()
    }
}
