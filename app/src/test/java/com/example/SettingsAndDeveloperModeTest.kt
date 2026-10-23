package com.example

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.TranslationEngineMode
import com.example.data.local.StudyPlanProfileEntity
import com.example.data.onboarding.OnboardingPreferencesRepository
import com.example.ui.screens.MoreScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.WordBooksTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class SettingsAndDeveloperModeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var tempDataStoreFile: File
    private lateinit var onboardingRepo: OnboardingPreferencesRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dataStoreScope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
        tempDataStoreFile = File(context.cacheDir, "test_settings_dev_${System.nanoTime()}.preferences_pb")
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { tempDataStoreFile }
        )
        onboardingRepo = OnboardingPreferencesRepository(context, testDataStore)
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
        if (tempDataStoreFile.exists()) {
            tempDataStoreFile.delete()
        }
    }

    @Test
    fun moreScreen_hidesPublicLabsCardAndNavigatesToSettings() {
        var settingsOpened = false

        composeTestRule.setContent {
            WordBooksTheme {
                MoreScreen(
                    onNavigateToGrammar = {},
                    onNavigateToReading = {},
                    onNavigateToPlan = {},
                    onNavigateToCorrectionNotebook = {},
                    onOpenSettings = { settingsOpened = true }
                )
            }
        }

        // Verify public Labs & Diagnostics card is NOT rendered on MoreScreen
        composeTestRule.onNodeWithTag("nav_labs").assertDoesNotExist()

        // Verify Settings card is displayed and opens Settings
        composeTestRule.onNodeWithTag("nav_settings").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_settings").performClick()
        assertTrue(settingsOpened)
    }

    @Test
    fun settingsScreen_unlocksDeveloperModeAfter7TapsAndAllowsDisabling() {
        var isDevMode by mutableStateOf(false)
        var isUkAccent by mutableStateOf(false)
        var speechRate by mutableFloatStateOf(0.85f)
        var autoPlay by mutableStateOf(false)
        var allowAi by mutableStateOf(true)
        var engineMode by mutableStateOf(TranslationEngineMode.FREE_API_ONLY)
        var labsOpened = false
        var voiceTestedText: String? = null

        val sampleProfile = StudyPlanProfileEntity(
            id = 1,
            level = "B2",
            goal = "ielts",
            minutesPerDay = 30,
            studyDaysPerWeek = 6,
            startDate = "2026-10-23"
        )

        composeTestRule.setContent {
            WordBooksTheme {
                SettingsScreen(
                    preferredAccentUk = isUkAccent,
                    ttsSpeechRate = speechRate,
                    autoPlayPronunciation = autoPlay,
                    studyPlanProfile = sampleProfile,
                    totalEntriesCount = 21624,
                    allowAi = allowAi,
                    engineMode = engineMode,
                    apiKeySource = "Custom Gemini Key",
                    isApiKeyConfigured = true,
                    isDeveloperModeEnabled = isDevMode,
                    onSelectAccentUk = { isUkAccent = it },
                    onSelectSpeechRate = { speechRate = it },
                    onToggleAutoPlayPronunciation = { autoPlay = it },
                    onTestVoicePreview = { voiceTestedText = it },
                    onSavePlanProfile = {},
                    onNavigateToPlan = {},
                    onRedoOnboardingSetup = {},
                    onExportBackupToUri = { _, _ -> },
                    onImportBackupFromUri = { _, _ -> },
                    onToggleAllowAi = { allowAi = it },
                    onSelectEngineMode = { engineMode = it },
                    onOpenApiKeyDialog = {},
                    onSetDeveloperMode = { isDevMode = it },
                    onNavigateToLabs = { labsOpened = true }
                )
            }
        }

        // 1. Verify Audio & Pronunciation controls work
        composeTestRule.onNodeWithTag("settings_section_audio").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_accent_uk").performClick()
        assertTrue(isUkAccent)

        composeTestRule.onNodeWithTag("btn_speed_slow").performClick()
        assertEquals(0.60f, speechRate, 0.01f)

        composeTestRule.onNodeWithTag("btn_test_voice").performClick()
        assertTrue(!voiceTestedText.isNullOrBlank())

        // 2. Verify Developer Options card is initially hidden
        composeTestRule.onNodeWithTag("btn_settings_open_labs").assertDoesNotExist()

        // 3. Scroll to Version row and tap 7 times
        composeTestRule.onNodeWithTag("settings_version_row").performScrollTo().assertIsDisplayed()

        // Taps 1..3 -> no countdown banner yet
        repeat(3) {
            composeTestRule.onNodeWithTag("settings_version_row").performClick()
        }
        composeTestRule.onNodeWithTag("settings_developer_toast_banner").assertDoesNotExist()
        assertFalse(isDevMode)

        // Tap 4 -> "You are 3 steps away from developer mode"
        composeTestRule.onNodeWithTag("settings_version_row").performScrollTo().performClick()
        composeTestRule.onNodeWithText("You are 3 steps away from developer mode").performScrollTo().assertIsDisplayed()
        assertFalse(isDevMode)

        // Tap 5 -> "You are 2 steps away from developer mode"
        composeTestRule.onNodeWithTag("settings_version_row").performScrollTo().performClick()
        composeTestRule.onNodeWithText("You are 2 steps away from developer mode").performScrollTo().assertIsDisplayed()

        // Tap 6 -> "You are 1 step away from developer mode"
        composeTestRule.onNodeWithTag("settings_version_row").performScrollTo().performClick()
        composeTestRule.onNodeWithText("You are 1 step away from developer mode").performScrollTo().assertIsDisplayed()

        // Tap 7 -> Developer mode unlocked!
        composeTestRule.onNodeWithTag("settings_version_row").performScrollTo().performClick()
        assertTrue(isDevMode)

        // Verify Developer Options card and Open Labs button appear
        composeTestRule.onNodeWithTag("btn_settings_open_labs").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_settings_open_labs").performClick()
        assertTrue(labsOpened)

        // Toggle off Developer Mode via switch
        composeTestRule.onNodeWithTag("switch_developer_mode").performScrollTo().performClick()
        assertFalse(isDevMode)
    }

    @Test
    fun preferencesRepository_persistsDeveloperModeAndAudioSettings() = runBlocking {
        val initial = onboardingRepo.getSnapshot()
        assertFalse(initial.isDeveloperModeEnabled)
        assertFalse(initial.preferredAccentUk)
        assertFalse(initial.autoPlayPronunciation)
        assertEquals(0.85f, initial.ttsSpeechRate, 0.01f)

        onboardingRepo.setDeveloperModeEnabled(true)
        onboardingRepo.setPreferredAccentUk(true)
        onboardingRepo.setAutoPlayPronunciation(true)
        onboardingRepo.setTtsSpeechRate(0.60f)

        val updated = onboardingRepo.getSnapshot()
        assertTrue(updated.isDeveloperModeEnabled)
        assertTrue(updated.preferredAccentUk)
        assertTrue(updated.autoPlayPronunciation)
        assertEquals(0.60f, updated.ttsSpeechRate, 0.01f)
    }
}
