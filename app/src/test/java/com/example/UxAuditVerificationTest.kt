package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.plan.CefrReadinessSummary
import com.example.data.plan.LearnerModelSnapshot
import com.example.data.plan.WordLifecycleFunnel
import com.example.ui.components.CefrAndLifecycleCard
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.AppThemeState
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextSubtle
import com.example.ui.theme.WordBooksDesignTokens
import com.example.ui.theme.WordBooksTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h900dp")
class UxAuditVerificationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun cefrAndLifecycleCard_clarifiesLevelScopeAndSupportsCollapseToggle() {
        val readiness = CefrReadinessSummary(
            currentLevel = "B1",
            nextLevel = "B2",
            overallReadinessPercent = 2,
            vocabCoveragePercent = 2,
            grammarMasteryPercent = 0,
            readingMasteryPercent = 0,
            isReadyForLevelUp = false,
            vocabFamiliarOrMatureCount = 10,
            vocabTargetCount = 500,
            grammarPassedCount = 0,
            grammarTotalCount = 24,
            readingCompletedCount = 0,
            readingTargetCount = 26
        )
        val funnel = WordLifecycleFunnel(
            totalPoolCount = 16267,
            newCount = 16247,
            seenCount = 0,
            learningCount = 7,
            familiarCount = 0,
            matureCount = 13,
            conqueredCount = 0
        )

        composeTestRule.setContent {
            WordBooksTheme {
                CefrAndLifecycleCard(
                    readiness = readiness,
                    funnel = funnel
                )
            }
        }
        composeTestRule.waitForIdle()

        // Verify explicit B1 level label on Vocab readiness pillar and All Selected Books subtitle on Lifecycle
        composeTestRule.onNodeWithText("B1 Vocab (50%)").assertExists()
        composeTestRule.onNodeWithText("2% (10/500)").assertExists()
        composeTestRule.onNodeWithText("All Selected Books • كل الكتب المختارة").assertExists()
        composeTestRule.onNodeWithTag("funnel_stage_mature", useUnmergedTree = true).assertExists()

        // Toggle collapse on Vocabulary Lifecycle Stages
        composeTestRule.onNodeWithTag("btn_toggle_lifecycle_stages").performClick()
        composeTestRule.waitForIdle()

        // Toggle expand back
        composeTestRule.onNodeWithTag("btn_toggle_lifecycle_stages").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("funnel_stage_mature", useUnmergedTree = true).assertExists()
    }

    @Test
    fun homeScreen_waitingQueueDisplaysBilingualInQueueLabel() {
        composeTestRule.setContent {
            WordBooksTheme {
                HomeScreen(
                    reviewDueNowCount = 0,
                    waitingLaterTodayCount = 5,
                    nextWaitingDueTimestamp = System.currentTimeMillis() + 7 * 60_000L,
                    learnerModelSnapshot = LearnerModelSnapshot()
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("home_review_due_card", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag("home_review_due_count", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("in queue", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("في الانتظار", useUnmergedTree = true).assertExists()
    }

    @Test
    fun themeTokens_adaptCorrectlyBetweenDarkAndLightMode() {
        AppThemeState.isLightMode = false
        val darkSurface = SurfaceDark
        val darkTextPrimary = TextPrimary
        val darkTextSecondary = TextSecondary
        val darkTextSubtle = TextSubtle
        val darkGold = GoldAccent
        val darkEmerald = EmeraldAccent

        AppThemeState.isLightMode = true
        val lightSurface = SurfaceDark
        val lightTextPrimary = TextPrimary
        val lightTextSecondary = TextSecondary
        val lightTextSubtle = TextSubtle
        val lightGold = GoldAccent
        val lightEmerald = EmeraldAccent

        assertNotEquals(darkSurface, lightSurface)
        assertNotEquals(darkTextPrimary, lightTextPrimary)
        assertNotEquals(darkTextSecondary, lightTextSecondary)
        assertNotEquals(darkTextSubtle, lightTextSubtle)
        assertNotEquals(darkGold, lightGold)
        assertNotEquals(darkEmerald, lightEmerald)

        // Reset to Dark default
        AppThemeState.isLightMode = false
        assertEquals(1.0f, WordBooksDesignTokens.LOCKED_FONT_SCALE, 0.001f)
    }

    @Test
    fun bidiParenthetical_preservesClosingBracketForMixedLevelGloss() {
        val formatted = com.example.ui.components.isolateLatin("تعلم 10 مفردات جديدة (مستوى B2)")
        // Ensure the parenthetical is anchored with RLM on both opening '(' and closing ')' and bound with NBSP
        assertTrue(formatted.contains("\u200F(\u200Fمستوى\u00A0"))
        assertTrue(formatted.endsWith("\u200F)\u200F"))
    }

    @Test
    fun flashcardDeckStartIndex_startsAtZeroForUnstudiedDeckInSmartMixAndRestoresProgress() {
        val sampleCards = listOf("a", "abandon", "ability", "able", "about").mapIndexed { idx, word ->
            com.example.data.importer.GroupedWordCard(
                cardKey = "oxford5000:$word",
                deck = "oxford5000",
                text = word,
                primaryLevel = "A1",
                primaryType = "word",
                ipa = "",
                syllables = "",
                posVariants = listOf(
                    com.example.data.importer.PosSenseVariant(
                        entryId = (idx + 1).toString(),
                        pos = "n.",
                        level = "A1",
                        type = "word",
                        arabicMeaning = "معنى",
                        definitionEn = "",
                        noteEn = "",
                        examplesEn = emptyList(),
                        definitionUrl = "",
                        audioUrl = ""
                    )
                )
            )
        }
        val mixedCards = com.example.ui.viewmodel.WordBooksViewModel.sortGroupedCards(
            sampleCards,
            com.example.data.importer.WordSortOrder.MIXED
        )

        // Fresh unstudied deck state (empty lastWordText) starts at Card 1 (index 0)
        val freshState = com.example.data.local.DeckStudyStateEntity(
            deckId = "oxford5000",
            lastCardIndex = 0,
            lastWordText = "",
            totalStudiedInDeck = 0
        )
        assertEquals(
            0,
            com.example.ui.viewmodel.WordBooksViewModel.resolveRestoredFlashcardIndex(mixedCards, freshState)
        )

        // Legacy unstudied fallback row where lastCardIndex == 0 and totalStudiedInDeck == 0 also starts at Card 1 (index 0)
        val legacyUnstudiedState = com.example.data.local.DeckStudyStateEntity(
            deckId = "oxford5000",
            lastCardIndex = 0,
            lastWordText = "a",
            totalStudiedInDeck = 0
        )
        assertEquals(
            0,
            com.example.ui.viewmodel.WordBooksViewModel.resolveRestoredFlashcardIndex(mixedCards, legacyUnstudiedState)
        )

        // Genuine progress in the deck restores the saved card index
        val targetWord = mixedCards[3].text
        val studiedState = com.example.data.local.DeckStudyStateEntity(
            deckId = "oxford5000",
            lastCardIndex = 3,
            lastWordText = targetWord,
            totalStudiedInDeck = 4
        )
        assertEquals(
            3,
            com.example.ui.viewmodel.WordBooksViewModel.resolveRestoredFlashcardIndex(mixedCards, studiedState)
        )
    }

    @Test
    fun studyPlanTaskSummary_mergesDuplicateLinesAndStripsRedundantDuration() {
        val flashcardsUpToDate = com.example.data.local.DailyTaskItem(
            id = "task_flashcards",
            type = "flashcards",
            titleEn = "Flashcards Review Practice",
            titleAr = "مراجعة البطاقات التعليمية (0 بطاقة)",
            subtitleEn = "All reviews up to date! 0 cards due today",
            targetCount = 0,
            completedCount = 0,
            isCompleted = true,
            linkedRoute = "flashcards",
            linkedId = "oxford5000",
            reason = "All reviews up to date! 0 cards due today",
            estimatedMinutes = 0
        )
        assertEquals(
            "All reviews up to date! 0 cards due today",
            com.example.ui.screens.buildCleanTaskDetailEn(flashcardsUpToDate)
        )

        val newWordsTask = com.example.data.local.DailyTaskItem(
            id = "task_new_words",
            type = "new_words",
            titleEn = "Learn 10 New Words (B2)",
            titleAr = "تعلم 10 مفردات جديدة (مستوى B2)",
            subtitleEn = "Low B2 coverage (0%) — prioritizing B2 • Focus: abandoned, ability, able",
            targetCount = 10,
            completedCount = 0,
            isCompleted = false,
            linkedRoute = "new_words",
            linkedId = "oxford5000|B2",
            reason = "Pre-teaching 6 words for 'The Missed Interview' + 4 B2 General words (~8 min)",
            estimatedMinutes = 8
        )
        assertEquals(
            "Focus: abandoned, ability, able • Pre-teaching 6 words for 'The Missed Interview' + 4 B2 General words",
            com.example.ui.screens.buildCleanTaskDetailEn(newWordsTask)
        )
    }

    @Test
    @Config(sdk = [34], qualifiers = "w360dp-h1600dp")
    fun onboardingStep3_readingTopicsGridCardsMaintainEqualHeightAcrossDevices() {
        assertEquals(
            WordBooksDesignTokens.CANONICAL_EMULATOR_WIDTH_DP,
            WordBooksDesignTokens.MIN_NORMALIZED_WIDTH_DP,
            0.001f
        )

        composeTestRule.setContent {
            WordBooksTheme {
                com.example.ui.screens.OnboardingGoalAndTimeStep(
                    selectedLevel = "B2",
                    selectedGoal = "general_fluency",
                    selectedMinutesPerDay = 30,
                    selectedStudyDaysPerWeek = 6,
                    onSelectGoal = {},
                    onSelectMinutesPerDay = {},
                    onSelectStudyDaysPerWeek = {},
                    onContinueStep3 = { _, _, _ -> },
                    onStep3Back = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        val healthBounds = composeTestRule
            .onNodeWithTag("onboarding_topic_chip_health", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val scienceBounds = composeTestRule
            .onNodeWithTag("onboarding_topic_chip_science_nature", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot

        assertEquals(healthBounds.height, scienceBounds.height, 1.0f)
    }
}

