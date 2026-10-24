package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.local.GrammarLessonEntity
import com.example.data.local.ReadingTextEntity
import com.example.ui.screens.GrammarRecommendedShelf
import com.example.ui.screens.ReadingRecommendedShelf
import com.example.ui.theme.WordBooksTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SectionLevelPersonalizationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleGrammarLessons = listOf(
        GrammarLessonEntity(
            id = "g_b2_1",
            titleEn = "Inversion for Emphasis",
            titleAr = "التقديم والتأخير للتوكيد",
            level = "B2",
            topic = "Sentence Structure",
            explanationAr = "شرح",
            examplesJson = "[]",
            exercisesJson = "[]"
        ),
        GrammarLessonEntity(
            id = "g_a1_1",
            titleEn = "Present Simple Basics",
            titleAr = "أساسيات المضارع البسيط",
            level = "A1",
            topic = "Tenses",
            explanationAr = "شرح",
            examplesJson = "[]",
            exercisesJson = "[]"
        )
    )

    private val sampleReadingTexts = listOf(
        ReadingTextEntity(
            id = "r_b2_1",
            titleEn = "The Psychology of Habits",
            titleAr = "علم نفس العادات",
            level = "B2",
            topic = "Psychology",
            sentencesJson = "[]",
            questionsJson = "[]"
        ),
        ReadingTextEntity(
            id = "r_a2_1",
            titleEn = "A Trip to the Market",
            titleAr = "رحلة إلى السوق",
            level = "A2",
            topic = "Daily Life",
            sentencesJson = "[]",
            questionsJson = "[]"
        )
    )

    @Test
    fun grammarRecommendedShelf_rendersAndHandlesLessonSelection() {
        var selectedLesson: GrammarLessonEntity? = null

        composeTestRule.setContent {
            WordBooksTheme {
                GrammarRecommendedShelf(
                    userLevel = "B2",
                    recommendedLessons = sampleGrammarLessons.filter { it.level == "B2" },
                    progressMap = emptyMap(),
                    onSelectLesson = { selectedLesson = it }
                )
            }
        }

        composeTestRule.onNodeWithTag("grammar_recommended_shelf").assertIsDisplayed()
        composeTestRule.onNodeWithText("Recommended for You").assertIsDisplayed()
        composeTestRule.onNodeWithText("Inversion for Emphasis").assertIsDisplayed()

        composeTestRule.onNodeWithTag("grammar_recommended_card_g_b2_1").performClick()
        assertEquals("g_b2_1", selectedLesson?.id)
    }

    @Test
    fun readingRecommendedShelf_rendersAndHandlesTextSelection() {
        var selectedText: ReadingTextEntity? = null

        composeTestRule.setContent {
            WordBooksTheme {
                ReadingRecommendedShelf(
                    userLevel = "B2",
                    recommendedTexts = sampleReadingTexts.filter { it.level == "B2" },
                    progressByTextId = emptyMap(),
                    onSelectText = { selectedText = it }
                )
            }
        }

        composeTestRule.onNodeWithTag("reading_recommended_shelf").assertIsDisplayed()
        composeTestRule.onNodeWithText("Recommended for Your Level (B2)").assertIsDisplayed()
        composeTestRule.onNodeWithText("The Psychology of Habits").assertIsDisplayed()

        composeTestRule.onNodeWithTag("reading_recommended_card_r_b2_1").performClick()
        assertEquals("r_b2_1", selectedText?.id)
    }
}
