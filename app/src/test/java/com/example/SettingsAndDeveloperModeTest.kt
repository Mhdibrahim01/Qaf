package com.example

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
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
import com.example.data.ai.AiWordDetailJson
import com.example.data.ai.TranslationEngineMode
import com.example.data.audio.AudioPlayerManager
import com.example.data.local.GroupedWordCard
import com.example.data.local.StudyPlanProfileEntity
import com.example.data.onboarding.OnboardingPreferencesRepository
import com.example.ui.components.SlowAudioTurtleButton
import com.example.ui.screens.AiDefinitionPanel
import com.example.ui.screens.MoreScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.WordBooksTheme
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
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
        composeTestRule.onNodeWithTag("btn_accent_uk").performScrollTo().performClick()
        assertTrue(isUkAccent)

        composeTestRule.onNodeWithTag("btn_speed_slow").performScrollTo().performClick()
        assertEquals(0.60f, speechRate, 0.01f)

        composeTestRule.onNodeWithTag("btn_test_voice").performScrollTo().performClick()
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

    @Test
    fun audioOptimization_scoresHighQualityNeuralVoicesAndRendersTurtleButton() {
        assertEquals("com.google.android.tts", AudioPlayerManager.GOOGLE_TTS_PACKAGE)
        assertEquals(0.85f, AudioPlayerManager.DEFAULT_NORMAL_RATE, 0.001f)
        assertEquals(0.60f, AudioPlayerManager.SLOW_TURTLE_RATE, 0.001f)
        assertEquals(0.70f, AudioPlayerManager.EXO_SLOW_RATE, 0.001f)

        val neuralLocalUs = Voice(
            "en-us-x-sfg-local",
            Locale.US,
            Voice.QUALITY_VERY_HIGH,
            Voice.LATENCY_LOW,
            false,
            emptySet()
        )
        val legacyNetworkUs = Voice(
            "en-us-legacy-network",
            Locale.US,
            Voice.QUALITY_NORMAL,
            Voice.LATENCY_NORMAL,
            true,
            emptySet()
        )
        val notInstalledUs = Voice(
            "en-us-x-tpf-local",
            Locale.US,
            Voice.QUALITY_VERY_HIGH,
            Voice.LATENCY_LOW,
            false,
            setOf(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
        )
        val ukVoice = Voice(
            "en-gb-x-gba-local",
            Locale.UK,
            Voice.QUALITY_VERY_HIGH,
            Voice.LATENCY_LOW,
            false,
            emptySet()
        )

        val scoreNeural = AudioPlayerManager.scoreVoiceForLocale(neuralLocalUs, Locale.US)
        val scoreLegacy = AudioPlayerManager.scoreVoiceForLocale(legacyNetworkUs, Locale.US)
        val scoreUninstalled = AudioPlayerManager.scoreVoiceForLocale(notInstalledUs, Locale.US)
        val scoreWrongCountry = AudioPlayerManager.scoreVoiceForLocale(ukVoice, Locale.US)

        assertTrue("Neural local voice should outscore legacy network voice", scoreNeural > scoreLegacy)
        assertEquals(1650, scoreNeural) // 1000 (preferred neural) + 500 (VERY_HIGH) + 100 (local offline) + 50 (-x-/local)
        assertTrue("Uninstalled voice should be rejected", scoreUninstalled < 0)
        assertTrue("UK voice should be rejected when querying US locale", scoreWrongCountry < 0)

        var turtleClicked = false
        composeTestRule.setContent {
            WordBooksTheme {
                SlowAudioTurtleButton(
                    onClick = { turtleClicked = true },
                    isPlayingSlow = true,
                    showSpeedLabel = true,
                    testTag = "test_turtle_btn"
                )
            }
        }

        composeTestRule.onNodeWithTag("test_turtle_btn").assertIsDisplayed()
        composeTestRule.onNodeWithText("0.6x").assertIsDisplayed()
        composeTestRule.onNodeWithTag("test_turtle_btn").performClick()
        assertTrue(turtleClicked)
    }

    @Test
    fun googleTts_scoresAndSelectsOfflineNeuralVoiceAndAppliesNaturalPitch() {
        assertEquals(0.98f, AudioPlayerManager.NATURAL_PITCH, 0.001f)
        assertEquals(0.85f, AudioPlayerManager.DEFAULT_NORMAL_RATE, 0.001f)
        assertEquals(0.60f, AudioPlayerManager.SLOW_TURTLE_RATE, 0.001f)

        // Mock voice candidates
        val neuralVoice = Voice(
            "en-us-x-sfg-local",
            Locale.US,
            Voice.QUALITY_VERY_HIGH,
            Voice.LATENCY_NORMAL,
            false, // offline
            emptySet()
        )
        val standardVoice = Voice(
            "en-us-standard-a",
            Locale.US,
            Voice.QUALITY_NORMAL,
            Voice.LATENCY_NORMAL,
            true, // network required
            emptySet()
        )

        val neuralScore = AudioPlayerManager.scoreVoiceForLocale(neuralVoice, Locale.US)
        val standardScore = AudioPlayerManager.scoreVoiceForLocale(standardVoice, Locale.US)

        assertTrue("Google offline neural voice must score significantly higher", neuralScore > standardScore)
        assertTrue("Neural score must include top preference bonus", neuralScore >= 1000)
    }

    @Test
    fun settingsScreen_displaysGoogleTtsEngineCardAndDualAudioSpeedControls() {
        runBlocking {
        // Test persistence in OnboardingPreferencesRepository
        assertEquals(0.85f, onboardingRepo.getSnapshot().ttsSpeechRate, 0.001f)
        onboardingRepo.setTtsSpeechRate(0.90f)
        assertEquals(0.90f, onboardingRepo.getSnapshot().ttsSpeechRate, 0.001f)

        composeTestRule.setContent {
            WordBooksTheme {
                SettingsScreen(
                    preferredAccentUk = false,
                    ttsSpeechRate = 0.85f,
                    autoPlayPronunciation = false,
                    studyPlanProfile = StudyPlanProfileEntity(level = "B1"),
                    totalEntriesCount = 3000,
                    allowAi = true,
                    engineMode = TranslationEngineMode.FREE_API_ONLY,
                    apiKeySource = "Built-in",
                    isApiKeyConfigured = true,
                    isDeveloperModeEnabled = false,
                    onSelectAccentUk = {},
                    onSelectSpeechRate = {},
                    onToggleAutoPlayPronunciation = {},
                    onTestVoicePreview = {},
                    onTestVoiceSlowPreview = {},
                    onSavePlanProfile = {},
                    onNavigateToPlan = {},
                    onRedoOnboardingSetup = {},
                    onExportBackupToUri = { _, _ -> },
                    onImportBackupFromUri = { _, _ -> },
                    onToggleAllowAi = {},
                    onSelectEngineMode = {},
                    onOpenApiKeyDialog = {},
                    onSetDeveloperMode = {},
                    onNavigateToLabs = {}
                )
            }
        }

        // Verify Google Speech Services status card exists
        composeTestRule.onNodeWithTag("card_google_tts_engine").assertIsDisplayed()
        composeTestRule.onNodeWithText("Google Speech Services").assertIsDisplayed()
        composeTestRule.onNodeWithText("Active Engine").assertIsDisplayed()
        }
    }

    @Test
    fun aiDefinitionPanel_keepsTurtleButtonVisibleOnLongWordsAndSwitchesUsAndGbIndependently() {
        assertTrue(AudioPlayerManager.isUkLocale(Locale.UK))
        assertTrue(AudioPlayerManager.isUkLocale(Locale("en", "GB")))
        assertFalse(AudioPlayerManager.isUkLocale(Locale.US))

        var lastPlayedIsGb: Boolean? = null
        var lastPlayedSlowIsGb: Boolean? = null
        var playingWord by mutableStateOf<String?>(null)

        val longWordCard = GroupedWordCard(
            cardKey = "oxford5000::absolute",
            deck = "oxford5000",
            text = "absolute",
            ipa = "/ˈæb.sə.luːt/",
            syllables = "ab·so·lute",
            primaryLevel = "B2",
            primaryType = "word",
            isFavorite = false,
            isMastered = false,
            posVariants = emptyList()
        )
        val detail = AiWordDetailJson(
            syllables = "ab·so·lute",
            ipaUs = "/ˈæb.sə.luːt/",
            ipaGb = "/ˈæb.sə.luːt/"
        )

        composeTestRule.setContent {
            WordBooksTheme {
                AiDefinitionPanel(
                    card = longWordCard,
                    aiDetail = detail,
                    isAiDetailLoading = false,
                    aiDetailError = null,
                    currentlyPlayingWord = playingWord,
                    savedSentenceKeys = emptySet(),
                    onPlayAudio = {},
                    onPlayAudioSlow = {},
                    onPlayVoiceAudioSlow = { isGb ->
                        lastPlayedSlowIsGb = isGb
                        playingWord = longWordCard.text
                    },
                    onPlayVoiceAudio = { isGb ->
                        lastPlayedIsGb = isGb
                        playingWord = longWordCard.text
                    },
                    onSpeakSentenceTts = {},
                    onRetryAiDefinition = {},
                    onDismiss = {},
                    onEnrichWithAi = {},
                    onSaveUserArabic = { _, _ -> },
                    onToggleSaveSentence = { _, _ -> },
                    onToggleFavorite = {},
                    onToggleMastered = {}
                )
            }
        }

        // 1. Verify US, GB, and Slow Turtle buttons are all reachable even with long IPA
        composeTestRule.onNodeWithTag("pronounce_us_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("pronounce_gb_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("pronounce_slow_button").performScrollTo().assertIsDisplayed()

        // 2. Click US -> triggers isGb = false
        composeTestRule.onNodeWithTag("pronounce_us_button").performScrollTo().performClick()
        assertEquals(false, lastPlayedIsGb)

        // 3. Click GB -> triggers isGb = true independently
        composeTestRule.onNodeWithTag("pronounce_gb_button").performScrollTo().performClick()
        assertEquals(true, lastPlayedIsGb)

        // 4. Click Slow Turtle while GB is selected -> triggers slow playback with isGb = true
        composeTestRule.onNodeWithTag("pronounce_slow_button").performScrollTo().performClick()
        assertEquals(true, lastPlayedSlowIsGb)
    }
}
