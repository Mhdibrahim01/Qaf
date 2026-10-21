package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.local.DailyChecklistEntity
import com.example.data.local.DailyTaskItem
import com.example.data.local.EntryEntity
import com.example.data.local.StudyPlanProfileEntity
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.WordBooksTheme
import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PostOnboardingDashboardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val json = Json { ignoreUnknownKeys = true }

    private val sampleKickoffWords = listOf(
        EntryEntity(
            id = "1",
            type = "word",
            deck = "oxford5000",
            text = "abandon",
            pos = "verb",
            level = "B2",
            ipa = "/əˈbændən/",
            definitionEn = "to leave a place, thing, or person forever",
            noteEn = "",
            examplesEn = "They had to abandon their car in the snow.",
            examplesAr = "اضطروا للتخلي عن سيارتهم في الثلج.",
            definitionUrl = "",
            audioUrl = "",
            arabic = "يتخلى عن / يترك",
            arabicSource = "file",
            dataVersion = 1
        ),
        EntryEntity(
            id = "2",
            type = "word",
            deck = "oxford5000",
            text = "abrupt",
            pos = "adjective",
            level = "B2",
            ipa = "/əˈbrʌpt/",
            definitionEn = "sudden and unexpected",
            noteEn = "",
            examplesEn = "The road made an abrupt turn to the left.",
            examplesAr = "انعطف الطريق انعطافاً مفاجئاً إلى اليسار.",
            definitionUrl = "",
            audioUrl = "",
            arabic = "مفاجئ / مباغت",
            arabicSource = "file",
            dataVersion = 1
        ),
        EntryEntity(
            id = "3",
            type = "word",
            deck = "oxford5000",
            text = "academic",
            pos = "adjective",
            level = "B2",
            ipa = "/ˌækəˈdemɪk/",
            definitionEn = "connected with education",
            noteEn = "",
            examplesEn = "She has high academic standards.",
            examplesAr = "لديها معايير أكاديمية عالية.",
            definitionUrl = "",
            audioUrl = "",
            arabic = "أكاديمي / دراسي",
            arabicSource = "file",
            dataVersion = 1
        ),
        EntryEntity(
            id = "4",
            type = "word",
            deck = "oxford5000",
            text = "accelerate",
            pos = "verb",
            level = "B2",
            ipa = "/əkˈseləreɪt/",
            definitionEn = "to happen or make something happen sooner or faster",
            noteEn = "",
            examplesEn = "Inflation continues to accelerate.",
            examplesAr = "يستمر التضخم في التسارع.",
            definitionUrl = "",
            audioUrl = "",
            arabic = "يُسرّع / يتعجّل",
            arabicSource = "file",
            dataVersion = 1
        ),
        EntryEntity(
            id = "5",
            type = "word",
            deck = "oxford5000",
            text = "accommodate",
            pos = "verb",
            level = "B2",
            ipa = "/əˈkɒmədeɪt/",
            definitionEn = "to provide space or a place for someone",
            noteEn = "",
            examplesEn = "The hotel can accommodate up to 500 guests.",
            examplesAr = "يمكن للفندق استيعاب ما يصل إلى 500 ضيف.",
            definitionUrl = "",
            audioUrl = "",
            arabic = "يستوعب / يتسع لـ",
            arabicSource = "file",
            dataVersion = 1
        )
    )

    @Test
    fun postOnboardingDashboard_displaysPersonalizedBadges() {
        val profile = StudyPlanProfileEntity(
            id = 1,
            level = "B2",
            goal = "ielts",
            minutesPerDay = 30,
            studyDaysPerWeek = 6,
            startDate = "2026-10-21",
            selectedDecksJson = json.encodeToString(listOf("oxford5000", "ielts_core")),
            restDaysJson = json.encodeToString(listOf(6)),
            updatedAt = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            WordBooksTheme {
                HomeScreen(
                    studyStreak = 3,
                    studyPlanProfile = profile,
                    kickoffWords = emptyList()
                )
            }
        }

        // Verify CEFR level badge, goal chip, pace chip, and streak
        composeTestRule.onNodeWithTag("home_cefr_badge").assertIsDisplayed()
        composeTestRule.onNodeWithText("B2 · فوق المتوسط").assertIsDisplayed()

        composeTestRule.onNodeWithTag("home_goal_chip").assertIsDisplayed()
        composeTestRule.onNodeWithText("IELTS Academic · تحضير الآيلتس").assertIsDisplayed()

        composeTestRule.onNodeWithTag("home_pace_chip").assertIsDisplayed()
        composeTestRule.onNodeWithText("30 min/day").assertIsDisplayed()

        composeTestRule.onNodeWithTag("home_study_streak_badge").assertIsDisplayed()
        composeTestRule.onNodeWithText("3 day streak").assertIsDisplayed()
    }

    @Test
    fun postOnboardingDashboard_displaysKickoffStarterCardAndNavigates() {
        val profile = StudyPlanProfileEntity(
            id = 1,
            level = "B2",
            goal = "general_fluency",
            minutesPerDay = 30,
            studyDaysPerWeek = 6,
            startDate = "2026-10-21",
            selectedDecksJson = json.encodeToString(listOf("oxford5000")),
            restDaysJson = json.encodeToString(listOf(6)),
            updatedAt = System.currentTimeMillis()
        )

        var kickoffStarted = false
        var kickoffDismissed = false

        composeTestRule.setContent {
            WordBooksTheme {
                HomeScreen(
                    studyStreak = 1,
                    studyPlanProfile = profile,
                    kickoffWords = sampleKickoffWords,
                    isKickoffDismissed = false,
                    onStartKickoffSession = { kickoffStarted = true },
                    onDismissKickoffSession = { kickoffDismissed = true }
                )
            }
        }

        // Verify starter card is displayed
        composeTestRule.onNodeWithTag("home_kickoff_starter_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("5-Min Kickoff Session").assertIsDisplayed()

        // Verify word chips
        composeTestRule.onNodeWithTag("kickoff_word_chip_0").assertIsDisplayed()
        composeTestRule.onNodeWithText("abandon").assertIsDisplayed()
        composeTestRule.onNodeWithText("يتخلى عن / يترك").assertIsDisplayed()

        composeTestRule.onNodeWithTag("kickoff_word_chip_1").assertIsDisplayed()
        composeTestRule.onNodeWithText("abrupt").assertIsDisplayed()

        // Click Start 5-Min Session
        composeTestRule.onNodeWithTag("btn_start_kickoff_session").performClick()
        assertTrue("onStartKickoffSession should have been called", kickoffStarted)

        // Click dismiss button
        composeTestRule.onNodeWithTag("btn_dismiss_kickoff_session").performClick()
        assertTrue("onDismissKickoffSession should have been called", kickoffDismissed)
    }

    @Test
    fun postOnboardingDashboard_todayChecklistReflectsPlan() {
        val sampleTasks = listOf(
            DailyTaskItem(
                id = "vocab_1",
                type = "flashcards",
                titleEn = "Learn 10 words",
                titleAr = "تعلم 10 كلمات جديدة",
                subtitleEn = "oxford5000",
                targetCount = 10,
                completedCount = 0,
                isCompleted = false,
                linkedRoute = "flashcards"
            ),
            DailyTaskItem(
                id = "sentences_1",
                type = "sentences",
                titleEn = "Review 5 sentences",
                titleAr = "مراجعة 5 جمل",
                subtitleEn = "Level B2 examples",
                targetCount = 5,
                completedCount = 0,
                isCompleted = false,
                linkedRoute = "database"
            )
        )

        val checklist = DailyChecklistEntity(
            date = "2026-10-21",
            tasksJson = json.encodeToString(sampleTasks),
            completedCount = 0,
            totalCount = 2,
            isCompleted = false
        )

        composeTestRule.setContent {
            WordBooksTheme {
                HomeScreen(
                    studyStreak = 2,
                    todayChecklist = checklist
                )
            }
        }

        // Verify TodayPlanCard renders the tailored tasks
        composeTestRule.onNodeWithTag("home_today_plan_widget").assertIsDisplayed()
        composeTestRule.onNodeWithText("Learn 10 words").assertIsDisplayed()
        composeTestRule.onNodeWithText("Review 5 sentences").assertIsDisplayed()
    }
}
