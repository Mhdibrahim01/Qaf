package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.local.EntryEntity
import com.example.data.local.GrammarLessonEntity
import com.example.data.local.ReadingTextEntity
import com.example.ui.screens.GrammarLessonListScreen
import com.example.ui.screens.GrammarRecommendedShelf
import com.example.ui.screens.ReadingListScreen
import com.example.ui.screens.ReadingRecommendedShelf
import com.example.ui.screens.WordBooksDatabaseScreen
import com.example.ui.screens.WordBooksRecommendedShelf
import com.example.ui.theme.WordBooksTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    private val sampleWords = listOf(
        EntryEntity(
            id = "w1",
            type = "word",
            deck = "oxford5000",
            text = "perceive",
            pos = "verb",
            level = "B2",
            ipa = "/pəˈsiːv/",
            definitionEn = "to notice or become aware of something",
            translationAr = "يدرك / يلاحظ",
            orderIndex = 1
        ),
        EntryEntity(
            id = "w2",
            type = "word",
            deck = "oxford5000",
            text = "advocate",
            pos = "noun / verb",
            level = "B2",
            ipa = "/ˈædvəkeɪt/",
            definitionEn = "to publicly support or recommend a particular cause or policy",
            translationAr = "يؤيد / مدافع",
            orderIndex = 2
        )
    )

    private val sampleGrammarLessons = listOf(
        GrammarLessonEntity(
            id = "g_b2_1",
            titleEn = "Inversion for Emphasis",
            titleAr = "التقديم والتأخير للتوكيد",
            level = "B2",
            topic = "Sentence Structure",
            orderIndex = 1
        ),
        GrammarLessonEntity(
            id = "g_a1_1",
            titleEn = "Present Simple Basics",
            titleAr = "أساسيات المضارع البسيط",
            level = "A1",
            topic = "Tenses",
            orderIndex = 2
        )
    )

    private val sampleReadingTexts = listOf(
        ReadingTextEntity(
            id = "r_b2_1",
            titleEn = "The Psychology of Habits",
            titleAr = "علم نفس العادات",
            level = "B2",
            topic = "Psychology",
            wordCount = 350
        ),
        ReadingTextEntity(
            id = "r_a2_1",
            titleEn = "A Trip to the Market",
            titleAr = "رحلة إلى السوق",
            level = "A2",
            topic = "Daily Life",
            wordCount = 120
        )
    )

    @Test
    fun wordBooksRecommendedShelf_rendersAndHandlesClicks() {
        var playedWord = ""
        var openedWord: EntryEntity? = null

        composeTestRule.setContent {
            WordBooksTheme {
                WordBooksRecommendedShelf(
                    userLevel = "B2",
                    recommendedWords = sampleWords,
                    onPlayAudio = { playedWord = it },
                    onOpenDetailedDefinition = { openedWord = it }
                )
            }
        }

        // Verify shelf exists and level badge displays B2
        composeTestRule.onNodeWithTag("wordbooks_recommended_shelf").assertIsDisplayed()
        composeTestRule.onNodeWithText("Recommended for Your Level (B2)").assertIsDisplayed()
        composeTestRule.onNodeWithText("perceive").assertIsDisplayed()
        composeTestRule.onNodeWithText("advocate").assertIsDisplayed()

        // Test audio playback button click
        composeTestRule.onNodeWithTag("rec_audio_btn_perceive").performClick()
        assertEquals("perceive", playedWord)

        // Test card tap to open detailed definition
        composeTestRule.onNodeWithTag("rec_card_perceive").performClick()
        assertEquals("w1", openedWord?.id)
    }

    @Test
    fun grammarRecommendedShelf_rendersAndHandlesLessonSelection() {
        var selectedLesson: GrammarLessonEntity? = null

        composeTestRule.setContent {
            WordBooksTheme {
                GrammarRecommendedShelf(
                    userLevel = "B2",
                    recommendedLessons = sampleGrammarLessons.filter { it.level == "B2" },
                    progressByLessonId = emptyMap(),
                    onSelectLesson = { selectedLesson = it }
                )
            }
        }

        composeTestRule.onNodeWithTag("grammar_recommended_shelf").assertIsDisplayed()
        composeTestRule.onNodeWithText("Recommended for Your Level (B2)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Inversion for Emphasis").assertIsDisplayed()

        // Click on the recommended lesson card
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

        // Click on the recommended reading text card
        composeTestRule.onNodeWithTag("reading_recommended_card_r_b2_1").performClick()
        assertEquals("r_b2_1", selectedText?.id)
    }

    @Test
    fun readingListScreen_showsStarOnUserLevelFilter() {
        composeTestRule.setContent {
            WordBooksTheme {
                ReadingListScreen(
                    texts = sampleReadingTexts,
                    progressList = emptyList(),
                    onSelectText = {},
                    onCreateWithAi = { _, _, _, _ -> Result.success("id") },
                    onSavePastedText = { _, _, _, _ -> Result.success(sampleReadingTexts.first()) },
                    onGetSavedTargetWords = { emptyList() },
                    onSyncAssets = {},
                    onImportJsonUris = { _, _ -> },
                    onImportJsonText = { Result.success(1) },
                    userLevel = "B2"
                )
            }
        }

        // Shelf is displayed
        composeTestRule.onNodeWithTag("reading_recommended_shelf").assertIsDisplayed()
        // The B2 filter chip has a star
        composeTestRule.onNodeWithText("B2 ★ (1)").assertIsDisplayed()
    }
}
