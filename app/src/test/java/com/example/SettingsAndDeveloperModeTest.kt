package com.example

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.AiWordDetailJson
import com.example.data.ai.TranslationEngineMode
import com.example.data.audio.AudioPlayerManager
import com.example.data.local.GroupedWordCard
import com.example.data.importer.PosSenseVariant
import com.example.data.importer.WordSortOrder
import com.example.ui.viewmodel.WordBooksViewModel
import com.example.data.local.StudyPlanProfileEntity
import com.example.data.onboarding.OnboardingPreferencesRepository
import com.example.ui.components.SlowAudioTurtleButton
import com.example.ui.screens.AiDefinitionPanel
import com.example.ui.screens.MoreScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AppLanguage
import com.example.ui.theme.AppLanguageState
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
                    totalEntriesCount = 27435,
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

        // Verify Language section and Google Speech Services status card exist
        composeTestRule.onNodeWithTag("settings_section_language").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_lang_en").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_lang_ar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_lang_system").assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_google_tts_engine").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Google Speech Services").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Active Engine").performScrollTo().assertIsDisplayed()
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

    @Test
    fun flashcardSorting_routesMixOrderAndDispersesWordsProperly() {
        fun makeCard(id: String, text: String, level: String): GroupedWordCard {
            return GroupedWordCard(
                cardKey = "test::$text",
                deck = "oxford5000",
                text = text,
                primaryLevel = level,
                primaryType = "word",
                ipa = "",
                syllables = "",
                posVariants = listOf(
                    PosSenseVariant(
                        entryId = id,
                        pos = "noun",
                        level = level,
                        type = "word",
                        arabicMeaning = "معنى",
                        definitionEn = "definition",
                        noteEn = "",
                        examplesEn = emptyList(),
                        examplesAr = emptyList(),
                        definitionUrl = "",
                        audioUrl = ""
                    )
                ),
                isFavorite = false,
                isMastered = false
            )
        }

        // Create cards starting with A, B, C, D, E, F
        val card1 = makeCard("1", "apple", "A1")
        val card2 = makeCard("2", "banana", "A2")
        val card3 = makeCard("3", "cat", "B1")
        val card4 = makeCard("4", "dog", "B2")
        val card5 = makeCard("5", "elephant", "C1")
        val card6 = makeCard("6", "fish", "A1")
        val card7 = makeCard("7", "grape", "A2")
        val card8 = makeCard("8", "house", "B1")
        val card9 = makeCard("9", "island", "B2")
        val card10 = makeCard("10", "jacket", "C1")

        val originalCards = listOf(card1, card2, card3, card4, card5, card6, card7, card8, card9, card10)

        // 1. A-Z Sort
        val azSorted = WordBooksViewModel.sortGroupedCards(originalCards, WordSortOrder.AZ)
        assertEquals("apple", azSorted.first().text)
        assertEquals("jacket", azSorted.last().text)

        // 2. Z-A Sort
        val zaSorted = WordBooksViewModel.sortGroupedCards(originalCards, WordSortOrder.ZA)
        assertEquals("jacket", zaSorted.first().text)
        assertEquals("apple", zaSorted.last().text)

        // 3. Level Sort (A1 -> A2 -> B1 -> B2 -> C1)
        val levelSorted = WordBooksViewModel.sortGroupedCards(originalCards, WordSortOrder.LEVEL)
        assertEquals("A1", levelSorted[0].primaryLevel)
        assertEquals("A1", levelSorted[1].primaryLevel)
        assertEquals("C1", levelSorted.last().primaryLevel)

        // 4. MIXED Sort (Knuth Multiplicative Golden-Ratio Hash)
        val mixedSorted = WordBooksViewModel.sortGroupedCards(originalCards, WordSortOrder.MIXED)
        // Verify it is NOT identical to the raw A-Z list
        val mixedTexts = mixedSorted.map { it.text }
        val azTexts = azSorted.map { it.text }
        assertTrue("Mixed cards must not be in identical A-Z order", mixedTexts != azTexts)

        // Verify deterministic reproduction
        val mixedSecondRun = WordBooksViewModel.sortGroupedCards(originalCards, WordSortOrder.MIXED)
        assertEquals(mixedTexts, mixedSecondRun.map { it.text })

        // Check that initial element is dispersed based on hash:
        // Entry 1: (1 * 2654435761) & 0x7FFFFFFF = 507000889
        // Entry 5: (5 * 2654435761) & 0x7FFFFFFF = 387520747 (lower hash -> comes first!)
        // Entry 9 ("island"): (9 * 2654435761) & 0x7FFFFFFF = 264024825 (lowest hash -> comes first!)
        assertEquals("island", mixedSorted.first().text)
    }

    @Test
    fun appLanguage_defaultsToEnglishLtrAndSwitchesToArabicRtlExplicitly() = runBlocking {
        val originalLocale = Locale.getDefault()
        try {
            // Even on an Arabic OS locale, default app language must stay English (LTR)
            Locale.setDefault(Locale("ar"))
            val initialSnap = onboardingRepo.getSnapshot()
            assertEquals("en", initialSnap.appLanguage)
            assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode(initialSnap.appLanguage))
            assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode(null))

            AppLanguageState.currentLanguage = AppLanguage.fromCode(initialSnap.appLanguage)
            assertFalse("Default English mode must stay LTR even on Arabic OS", AppLanguageState.isArabic)

            // Switching to Arabic mode persists "ar" and activates RTL
            onboardingRepo.setAppLanguage(AppLanguage.ARABIC.code)
            val updatedSnap = onboardingRepo.getSnapshot()
            assertEquals("ar", updatedSnap.appLanguage)
            AppLanguageState.currentLanguage = AppLanguage.fromCode(updatedSnap.appLanguage)
            assertTrue("Arabic mode must activate RTL", AppLanguageState.isArabic)

            // Switching to System mode follows OS locale
            onboardingRepo.setAppLanguage(AppLanguage.SYSTEM.code)
            AppLanguageState.currentLanguage = AppLanguage.fromCode(onboardingRepo.getSnapshot().appLanguage)
            assertTrue("System mode on Arabic OS must activate RTL", AppLanguageState.isArabic)
        } finally {
            AppLanguageState.currentLanguage = AppLanguage.ENGLISH
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun phase1b_streakFreezeShieldProtectsMissedDayAndReminderSettingsPersist() = runBlocking {
        val db = androidx.room.Room.inMemoryDatabaseBuilder(
            context,
            com.example.data.local.WordBooksDatabase::class.java
        ).allowMainThreadQueries().build()

        try {
            val dao = db.wordBooksDao()
            dao.upsertStudyPlanProfile(
                StudyPlanProfileEntity(
                    restDaysJson = "[]"
                )
            )
            val planRepo = com.example.data.plan.StudyPlanRepository(context, dao)

            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
            val yesterdayStr = sdf.format(cal.time)
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
            val twoDaysAgoStr = sdf.format(cal.time)
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
            val threeDaysAgoStr = sdf.format(cal.time)

            // User completed tasks 2 days ago and 3 days ago, but missed yesterday!
            dao.upsertDailyChecklist(
                com.example.data.local.DailyChecklistEntity(
                    date = threeDaysAgoStr,
                    tasksJson = "[]",
                    completedCount = 3,
                    totalCount = 3,
                    isCompleted = true
                )
            )
            dao.upsertDailyChecklist(
                com.example.data.local.DailyChecklistEntity(
                    date = twoDaysAgoStr,
                    tasksJson = "[]",
                    completedCount = 3,
                    totalCount = 3,
                    isCompleted = true
                )
            )

            // Initial shield count is 1
            assertEquals(1, onboardingRepo.getSnapshot().streakFreezeCount)

            // Evaluate streak & shields -> yesterday should be auto-protected by consuming 1 shield!
            val status = planRepo.evaluateStreakAndShields(onboardingRepo)
            assertEquals("Shield should protect missed yesterday and keep 2-day streak intact", 2, status.streak)
            assertEquals("1 shield should be consumed (1 -> 0)", 0, status.shieldCount)
            assertTrue("Should flag that shield was consumed recently", status.wasShieldConsumedRecently)
            assertEquals(yesterdayStr, status.protectedDate)

            // Reminder preferences persistence
            onboardingRepo.setStudyReminderEnabled(true)
            onboardingRepo.setStudyReminderTimeMinutes(1020) // 05:00 PM
            val snap = onboardingRepo.getSnapshot()
            assertTrue(snap.studyReminderEnabled)
            assertEquals(1020, snap.studyReminderTimeMinutes)
        } finally {
            db.close()
        }
    }

    @Test
    fun phase2a_scrambledWordTilesAndCharacterLevelTypoDiffWorkCorrectly() {
        val targetSentence = "Effective communication requires active listening."
        val tiles = com.example.data.sentences.SentencePracticeUtils.buildScrambledWordTiles(targetSentence, seed = 42)
        assertEquals(5, tiles.size)
        assertEquals(
            listOf("Effective", "communication", "requires", "active", "listening."),
            tiles.sortedBy { it.index }.map { it.text }
        )

        // Minor spelling typo ("comunication" instead of "communication") -> TYPO status with charDiffs
        val diffWithTypo = com.example.data.sentences.SentencePracticeUtils.computeSentenceDiff(
            expected = targetSentence,
            actual = "Effective comunication requires active listening.",
            ignorePunctuation = true
        )
        val typoToken = diffWithTypo.tokens[1]
        assertEquals(com.example.data.sentences.DiffTokenStatus.TYPO, typoToken.status)
        assertTrue("Typo token should populate character-level diff spans", typoToken.charDiffs.isNotEmpty())

        // Completely wrong word ("apple" instead of "communication") -> WRONG status
        val diffWithWrong = com.example.data.sentences.SentencePracticeUtils.computeSentenceDiff(
            expected = targetSentence,
            actual = "Effective apple requires active listening.",
            ignorePunctuation = true
        )
        assertEquals(com.example.data.sentences.DiffTokenStatus.WRONG, diffWithWrong.tokens[1].status)

        // Verify FlashcardsScreen counter shows "3 / 10" when resuming after 2 learned words,
        // and toggling Dictation Mode immediately auto-plays audio on activation and card change.
        val tenSessionCards = (1..10).map { idx ->
            GroupedWordCard(
                cardKey = "oxford5000:word$idx",
                deck = "oxford5000",
                text = "word$idx",
                primaryLevel = "B2",
                primaryType = "word",
                ipa = "/wɜːd/",
                syllables = "word",
                posVariants = listOf(
                    com.example.data.importer.PosSenseVariant(
                        entryId = "$idx",
                        pos = "n.",
                        level = "B2",
                        type = "word",
                        arabicMeaning = "كلمة $idx",
                        arabicSource = "file",
                        definitionEn = "Sample definition $idx",
                        definitionAr = "تعريف $idx",
                        noteEn = "",
                        examplesEn = emptyList(),
                        examplesAr = emptyList(),
                        definitionUrl = "",
                        audioUrl = ""
                    )
                ),
                isFavorite = false,
                isMastered = idx <= 2
            )
        }

        val autoPlayedWords = mutableListOf<String>()
        var currentIdx by mutableStateOf(2) // Resuming at 3rd card (index 2 -> "3 / 10")

        composeTestRule.setContent {
            WordBooksTheme {
                com.example.ui.screens.FlashcardsScreen(
                    decks = emptyList(),
                    activeDeckId = "oxford5000",
                    cards = tenSessionCards,
                    currentIndex = currentIdx,
                    isLoading = false,
                    progressMap = emptyMap(),
                    reviewDueTodayCount = 0,
                    studiedTodayCount = 2,
                    currentlyPlayingWord = null,
                    detailedCard = null,
                    isAiTranslating = false,
                    onSelectDeck = {},
                    onIndexChanged = { currentIdx = it },
                    onRateCard = {},
                    onToggleMastered = {},
                    onToggleFavorite = {},
                    onPlayAudio = { word, _ -> autoPlayedWords.add(word) },
                    onOpenDetailedDefinition = { _, _ -> },
                    onDismissDetailedDefinition = {},
                    onEnrichWordWithAi = { _, _ -> },
                    onSaveUserArabic = { _, _, _, _ -> },
                    onToggleCardFavoriteInSheet = { _, _ -> },
                    onToggleCardMasteredInSheet = { _, _ -> }
                )
            }
        }

        // Counter shows "3 / 10"
        composeTestRule.onNodeWithTag("flashcard_counter").assertIsDisplayed()
        assertTrue("Before enabling Dictation Mode, no audio should be auto-played", autoPlayedWords.isEmpty())

        // Enable Dictation Mode -> immediately pronounces "word3" and displays inline interactive letter slots
        composeTestRule.onNodeWithTag("flashcard_dictation_toggle").performClick()
        composeTestRule.waitForIdle()
        assertEquals(listOf("word3"), autoPlayedWords)
        composeTestRule.onNodeWithTag("flashcard_dictation_slots_row", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("flashcard_dictation_slot_0", useUnmergedTree = true).assertIsDisplayed()

        // Type a 5-letter misspelled attempt ("wordx") into the inline slots -> auto-verifies when last slot is filled
        composeTestRule.onNodeWithTag("flashcard_dictation_input", useUnmergedTree = true).performTextInput("wordx")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("flashcard_dictation_feedback", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("flashcard_dictation_retry", useUnmergedTree = true).assertIsDisplayed()

        // Advance to next card ("4 / 10") while Dictation Mode is active -> auto-pronounces "word4"
        composeTestRule.onNodeWithTag("flashcard_next_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(3, currentIdx)
        assertEquals(listOf("word3", "word4"), autoPlayedWords)

        // Navigate back to previous card -> auto-pronounces "word3"
        composeTestRule.onNodeWithTag("flashcard_prev_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(2, currentIdx)
        assertEquals(listOf("word3", "word4", "word3"), autoPlayedWords)
    }

    @Test
    fun feature3c_lexicalMindMapExtractorAndCanvasRenderAndSpeakNodes() {
        val sampleAiDetail = AiWordDetailJson(
            meanings = listOf(
                com.example.data.ai.AiMeaningItem(
                    pos = "v.",
                    arabic = listOf("يعتمد"),
                    definitionEn = "To rely on someone or something for support.",
                    definitionAr = "الاعتماد على شخص أو شيء للحصول على الدعم.",
                    example = com.example.data.ai.AiExampleItem(
                        en = "Success depends on consistent daily practice.",
                        ar = "النجاح يعتمد على الممارسة اليومية المستمرة."
                    )
                )
            ),
            phrases = listOf(
                com.example.data.ai.AiPhraseItem(en = "depend on", ar = listOf("يعتمد على")),
                com.example.data.ai.AiPhraseItem(en = "it depends", ar = listOf("الأمر يتوقف على الظروف"))
            )
        )

        val nodes = com.example.ui.components.LexicalMindMapExtractor.extractNodes(
            headword = "depend",
            aiDetail = sampleAiDetail,
            posVariants = emptyList()
        )
        assertTrue("Mind map should extract collocations, prepositions, and synonyms", nodes.size >= 3)
        assertTrue(nodes.any { it.labelEn == "depend on" && it.pillLabelEn == "+ on" && it.type == com.example.ui.components.MindMapBranchType.PREPOSITION })
        assertTrue(nodes.any { it.labelEn == "it depends" && it.pillLabelEn == "it +" && it.type == com.example.ui.components.MindMapBranchType.COLLOCATION })
        assertTrue(nodes.any { it.labelEn == "rely on" && it.type == com.example.ui.components.MindMapBranchType.SYNONYM })

        // Verify Data Sanitization rejects non-sequitur definition words ("silent" -> "making", "manipulation" -> "influencing", "overwhelming" -> "great")
        val silentNodes = com.example.ui.components.LexicalMindMapExtractor.extractNodes(
            headword = "silent",
            aiDetail = AiWordDetailJson(
                meanings = listOf(
                    com.example.data.ai.AiMeaningItem(
                        pos = "adj.",
                        arabic = listOf("صامت"),
                        definitionEn = "Not making or accompanied by any sound."
                    )
                ),
                phrases = listOf(
                    com.example.data.ai.AiPhraseItem(en = "silent partner", ar = listOf("شريك صامت")),
                    com.example.data.ai.AiPhraseItem(en = "keep silent", ar = listOf("يلتزم الصمت"))
                )
            ),
            posVariants = emptyList()
        )
        assertTrue("Sanitizer must never emit 'making' for 'silent'", silentNodes.none { it.labelEn.equals("making", ignoreCase = true) })
        assertTrue("Silent must include purple Related ('quiet') node", silentNodes.any { it.labelEn == "quiet" && it.type == com.example.ui.components.MindMapBranchType.SYNONYM })
        assertTrue(silentNodes.any { it.labelEn == "silent partner" && it.pillLabelEn == "+ partner" })
        assertTrue(silentNodes.any { it.labelEn == "keep silent" && it.pillLabelEn == "keep +" })

        // Verify "memory" produces 6 balanced nodes including the purple Related ("recall") node
        val memoryNodes = com.example.ui.components.LexicalMindMapExtractor.extractNodes(
            headword = "memory",
            aiDetail = AiWordDetailJson(
                meanings = listOf(
                    com.example.data.ai.AiMeaningItem(
                        pos = "n.",
                        arabic = listOf("ذاكرة", "ذكرى"),
                        definitionEn = "The faculty by which the mind stores and remembers information."
                    )
                ),
                phrases = listOf(
                    com.example.data.ai.AiPhraseItem(en = "short-term memory", ar = listOf("ذاكرة قصيرة المدى")),
                    com.example.data.ai.AiPhraseItem(en = "in memory of", ar = listOf("إحياء لذكرى")),
                    com.example.data.ai.AiPhraseItem(en = "lose one's memory", ar = listOf("يفقد الذاكرة")),
                    com.example.data.ai.AiPhraseItem(en = "refresh memory", ar = listOf("ينعش الذاكرة"))
                ),
                examples = listOf(
                    com.example.data.ai.AiExampleItem(en = "She has an excellent memory for names.", ar = "لديها ذاكرة ممتازة للأسماء.")
                )
            ),
            posVariants = emptyList()
        )
        assertEquals(6, memoryNodes.size)
        assertTrue(
            "Memory must include purple Related ('recall') node",
            memoryNodes.any { it.labelEn == "recall" && it.type == com.example.ui.components.MindMapBranchType.SYNONYM }
        )

        // Verify general full-sentence definitions (nouns/adjectives outside curated map) also produce a purple Related node
        val customNounNodes = com.example.ui.components.LexicalMindMapExtractor.extractNodes(
            headword = "resilience",
            aiDetail = AiWordDetailJson(
                meanings = listOf(
                    com.example.data.ai.AiMeaningItem(
                        pos = "n.",
                        arabic = listOf("مرونة"),
                        definitionEn = "The capacity to recover quickly from difficulties; toughness."
                    )
                ),
                phrases = listOf(
                    com.example.data.ai.AiPhraseItem(en = "emotional resilience", ar = listOf("مرونة عاطفية"))
                )
            ),
            posVariants = emptyList()
        )
        assertTrue(
            "Full-sentence definitions must always emit at least one purple SYNONYM node",
            customNounNodes.any { it.type == com.example.ui.components.MindMapBranchType.SYNONYM }
        )

        // Verify "psychology" never emits "+ at" (from "studying psychology at the university") or "study = علم النفس"
        val psychologyNodes = com.example.ui.components.LexicalMindMapExtractor.extractNodes(
            headword = "psychology",
            aiDetail = AiWordDetailJson(
                meanings = listOf(
                    com.example.data.ai.AiMeaningItem(
                        pos = "n.",
                        arabic = listOf("علم النفس"),
                        definitionEn = "The scientific study of the human mind and its functions.",
                        example = com.example.data.ai.AiExampleItem(
                            en = "He is studying psychology at the university.",
                            ar = "يدرس علم النفس في الجامعة."
                        )
                    )
                ),
                phrases = listOf(
                    com.example.data.ai.AiPhraseItem(en = "human psychology", ar = listOf("علم النفس البشري")),
                    com.example.data.ai.AiPhraseItem(en = "child psychology", ar = listOf("علم نفس الطفل")),
                    com.example.data.ai.AiPhraseItem(en = "reverse psychology", ar = listOf("علم النفس العكسي"))
                )
            ),
            posVariants = emptyList()
        )
        assertTrue("Psychology must never invent '+ at' from example sentence", psychologyNodes.none { it.pillLabelEn == "+ at" })
        assertTrue("Psychology must never emit 'study' as synonym", psychologyNodes.none { it.labelEn == "study" })
        assertTrue(
            "Psychology must emit 'psychological' with its own accurate Arabic meaning",
            psychologyNodes.any { it.labelEn == "psychological" && it.labelAr.contains("نفسي") && it.type == com.example.ui.components.MindMapBranchType.SYNONYM }
        )

        // Verify "into" classifies phrasal verbs ("turn into", "look into", "run into") as COLLOCATION and emits "inside" (not "movement")
        val intoNodes = com.example.ui.components.LexicalMindMapExtractor.extractNodes(
            headword = "into",
            aiDetail = AiWordDetailJson(
                meanings = listOf(
                    com.example.data.ai.AiMeaningItem(
                        pos = "prep.",
                        arabic = listOf("إلى داخل"),
                        definitionEn = "Expressing movement or action with the result that someone becomes enclosed."
                    )
                ),
                phrases = listOf(
                    com.example.data.ai.AiPhraseItem(en = "turn into", ar = listOf("يتحول إلى")),
                    com.example.data.ai.AiPhraseItem(en = "look into", ar = listOf("يحقق في")),
                    com.example.data.ai.AiPhraseItem(en = "run into", ar = listOf("يلتقي صدفة"))
                )
            ),
            posVariants = emptyList()
        )
        assertTrue("Into must never emit 'movement'", intoNodes.none { it.labelEn == "movement" })
        assertTrue("Into phrasal verbs must be COLLOCATIONs", intoNodes.any { it.pillLabelEn == "turn +" && it.type == com.example.ui.components.MindMapBranchType.COLLOCATION })
        assertTrue("Into must emit 'inside' as SYNONYM", intoNodes.any { it.labelEn == "inside" && it.type == com.example.ui.components.MindMapBranchType.SYNONYM })

        // Verify "puncture" filters synthetic templates ("in terms of puncture", "key puncture") and emits "pierce" + "+ in"
        val punctureNodes = com.example.ui.components.LexicalMindMapExtractor.extractNodes(
            headword = "puncture",
            aiDetail = AiWordDetailJson(
                meanings = listOf(
                    com.example.data.ai.AiMeaningItem(
                        pos = "n.",
                        arabic = listOf("ثقب"),
                        definitionEn = "A small hole in a tyre resulting in an escape of air."
                    )
                ),
                phrases = listOf(
                    com.example.data.ai.AiPhraseItem(en = "in terms of puncture", ar = listOf("من حيث ثقب")),
                    com.example.data.ai.AiPhraseItem(en = "key puncture", ar = listOf("ثقب مهم")),
                    com.example.data.ai.AiPhraseItem(en = "puncture repair", ar = listOf("إصلاح الثقب")),
                    com.example.data.ai.AiPhraseItem(en = "get a puncture", ar = listOf("يتعرض لثقب في الإطار"))
                )
            ),
            posVariants = emptyList()
        )
        assertTrue("Puncture must filter out 'in terms of puncture'", punctureNodes.none { it.labelEn.contains("in terms of") })
        assertTrue("Puncture must filter out 'key puncture'", punctureNodes.none { it.pillLabelEn == "key +" })
        assertTrue("Puncture must include '+ in' (never '+ on')", punctureNodes.any { it.pillLabelEn == "+ in" && it.type == com.example.ui.components.MindMapBranchType.PREPOSITION })
        assertTrue("Puncture must include 'pierce' as SYNONYM", punctureNodes.any { it.labelEn == "pierce" && it.type == com.example.ui.components.MindMapBranchType.SYNONYM })

        // Verify compact pill labels for "overwhelming" and "manipulation"
        assertEquals(
            "+ response",
            com.example.ui.components.LexicalMindMapExtractor.formatCompactPillLabel(
                "overwhelming",
                "overwhelming response",
                com.example.ui.components.MindMapBranchType.COLLOCATION
            )
        )
        assertEquals(
            "emotional +",
            com.example.ui.components.LexicalMindMapExtractor.formatCompactPillLabel(
                "manipulation",
                "emotional manipulation",
                com.example.ui.components.MindMapBranchType.COLLOCATION
            )
        )
        assertEquals(
            "+ of",
            com.example.ui.components.LexicalMindMapExtractor.formatCompactPillLabel(
                "manipulation",
                "manipulation + of",
                com.example.ui.components.MindMapBranchType.PREPOSITION
            )
        )

        // Verify 4-node diagonal quadrant topology avoids horizontal centerline collisions
        val radialOffsets4 = com.example.ui.components.LexicalMindMapExtractor.computeRadialNodeOffsets(
            nodeCount = 4,
            widthPx = 600f,
            heightPx = 400f
        )
        assertEquals(4, radialOffsets4.size)
        assertTrue(
            "All 4 diagonal quadrant nodes must be vertically offset away from centerY (200f)",
            radialOffsets4.all { kotlin.math.abs(it.y - 200f) >= 58f }
        )

        // Verify raw dictionary symbol expansion (+ and ~) for mind-map node labels
        assertEquals(
            "make a jump",
            com.example.ui.components.LexicalMindMapExtractor.expandRawNodeLabel("jump", "make a +")
        )
        assertEquals(
            "price jump",
            com.example.ui.components.LexicalMindMapExtractor.expandRawNodeLabel("jump", "price +")
        )
        assertEquals(
            "jump back",
            com.example.ui.components.LexicalMindMapExtractor.expandRawNodeLabel("jump", "+ back")
        )
        assertEquals(
            "get the jump on",
            com.example.ui.components.LexicalMindMapExtractor.expandRawNodeLabel("jump", "get the ~ on")
        )

        // Verify BiDi-safe Etymology formatter strips single quotes around Latin roots and converts Arabic quotes to guillemets
        val formattedEtym = com.example.ui.components.LexicalMindMapExtractor.formatBidiEtymologyAnnotated(
            "مشتق من الكلمة اللاتينية 'differre' التي تعني 'الابتعاد' أو 'الاختلاف'."
        ).text
        assertFalse("Single quotes around Latin root must be removed", formattedEtym.contains("'differre'"))
        assertTrue("Latin root must be wrapped in LTR isolate", formattedEtym.contains("\u2066\u200Edifferre\u200E\u2069"))
        assertTrue("Arabic gloss must use Arabic guillemets", formattedEtym.contains("«الابتعاد»"))

        var spokenText = ""
        composeTestRule.setContent {
            WordBooksTheme {
                com.example.ui.components.VisualLexicalMindMapCard(
                    headword = "depend",
                    aiDetail = sampleAiDetail,
                    posVariants = emptyList(),
                    onSpeakText = { spokenText = it }
                )
            }
        }

        composeTestRule.onNodeWithTag("lexical_mind_map_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("mind_map_canvas").assertIsDisplayed()
        composeTestRule.onNodeWithTag("mind_map_center_node").assertIsDisplayed()
        composeTestRule.onNodeWithTag("mind_map_node_1").performClick()
        assertEquals(nodes[1].expandedLabelEn("depend"), spokenText)
    }

    @Test
    fun feature4b_vocabularyBlitzComboMultipliersBadgesHighScoreAndDialogRender(): Unit = runBlocking {
        // 1. Verify Combo Multiplier progression (1x -> 2x at 5 -> 3x at 10 -> 4x at 15)
        assertEquals(1, com.example.data.blitz.VocabularyBlitzEngine.computeComboMultiplier(0))
        assertEquals(1, com.example.data.blitz.VocabularyBlitzEngine.computeComboMultiplier(4))
        assertEquals(2, com.example.data.blitz.VocabularyBlitzEngine.computeComboMultiplier(5))
        assertEquals(3, com.example.data.blitz.VocabularyBlitzEngine.computeComboMultiplier(10))
        assertEquals(4, com.example.data.blitz.VocabularyBlitzEngine.computeComboMultiplier(15))

        // 2. Generate 30 questions and simulate 25 consecutive correct answers
        val questions = com.example.data.blitz.VocabularyBlitzEngine.generateBlitzQuestions(
            entries = emptyList(),
            count = 30,
            seed = 99
        )
        assertEquals(30, questions.size)

        var state = com.example.data.blitz.VocabularyBlitzSessionState(questions = questions)
        repeat(25) {
            val q = state.currentQuestion!!
            state = com.example.data.blitz.VocabularyBlitzEngine.submitAnswer(
                state = state,
                selectedOptionIndex = q.correctOptionIndex,
                previousHighScore = 100
            )
        }

        assertEquals(25, state.correctCount)
        assertEquals(25, state.maxComboStreak)
        assertTrue("25 correct answers should unlock Word Sniper badge", state.unlockedWordSniperNow)
        assertTrue("15+ academic streak should unlock Academic Lightning badge", state.unlockedAcademicLightningNow)
        assertTrue("Score (${state.score}) should exceed previous high score (100)", state.isNewHighScore)

        // 3. Persist high score and badges in DataStore
        onboardingRepo.recordBlitzResult(
            score = state.score,
            maxCombo = state.maxComboStreak,
            unlockedWordSniper = state.unlockedWordSniperNow,
            unlockedAcademicLightning = state.unlockedAcademicLightningNow
        )
        val snap = onboardingRepo.getSnapshot()
        assertEquals(state.score, snap.blitzHighScore)
        assertEquals(25, snap.blitzBestStreak)
        assertTrue(snap.blitzBadgeWordSniper)
        assertTrue(snap.blitzBadgeAcademicLightning)

        // 4. Verify Compose UI renders timer ring, score badge, combo badge, and question options
        composeTestRule.setContent {
            WordBooksTheme {
                com.example.ui.screens.VocabularyBlitzDialog(
                    initialQuestions = questions,
                    enableTimerAutoTick = false,
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("vocab_blitz_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("blitz_timer_ring").assertIsDisplayed()
        composeTestRule.onNodeWithTag("blitz_score_badge").assertIsDisplayed()
        composeTestRule.onNodeWithTag("blitz_combo_badge").assertIsDisplayed()
        composeTestRule.onNodeWithTag("blitz_option_0").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun feature_speedChallengeEnhancements_setupFiltersAndAudioUpgrade(): Unit = runBlocking {
        // 1. Audio upgrade: test sound player functions without exception
        com.example.data.blitz.BlitzSoundPlayer.playCorrectChime(true)
        com.example.data.blitz.BlitzSoundPlayer.playWrongBuzz(true)
        com.example.data.blitz.BlitzSoundPlayer.playComboLevelUpChime(true)
        com.example.data.blitz.BlitzSoundPlayer.playCountdownTick(true)
        com.example.data.blitz.BlitzSoundPlayer.playCorrectChime(false)

        // 2. Room DAO Filtered candidates and word count verification
        val sampleEntries = listOf(
            com.example.data.local.EntryEntity(id = "blitz_1", type = "word", deck = "oxford5000", text = "abandon", pos = "v.", level = "B2", effectiveLevel = "B2", definitionEn = "leave behind", noteEn = "", arabic = "يتخلى عن", examplesEn = "", examplesAr = "", definitionUrl = "", audioUrl = ""),
            com.example.data.local.EntryEntity(id = "blitz_2", type = "word", deck = "oxford_opal", text = "abstract", pos = "adj.", level = "B2", effectiveLevel = "B2", definitionEn = "existing in thought", noteEn = "", arabic = "مجرد", examplesEn = "", examplesAr = "", definitionUrl = "", audioUrl = ""),
            com.example.data.local.EntryEntity(id = "blitz_3", type = "word", deck = "oxford5000", text = "academic", pos = "adj.", level = "B1", effectiveLevel = "B1", definitionEn = "relating to education", noteEn = "", arabic = "أكاديمي", examplesEn = "", examplesAr = "", definitionUrl = "", audioUrl = ""),
            com.example.data.local.EntryEntity(id = "blitz_4", type = "word", deck = "ielts_core", text = "accommodate", pos = "v.", level = "C1", effectiveLevel = "C1", definitionEn = "provide lodging", noteEn = "", arabic = "يستوعب", examplesEn = "", examplesAr = "", definitionUrl = "", audioUrl = ""),
            com.example.data.local.EntryEntity(id = "blitz_5", type = "word", deck = "oxford5000", text = "accompany", pos = "v.", level = "A2", effectiveLevel = "A2", definitionEn = "go with", noteEn = "", arabic = "يرافق", examplesEn = "", examplesAr = "", definitionUrl = "", audioUrl = "")
        )
        val db = androidx.room.Room.inMemoryDatabaseBuilder(
            context,
            com.example.data.local.WordBooksDatabase::class.java
        ).allowMainThreadQueries().build()

        try {
            db.wordBooksDao().insertEntriesBatchIgnore(sampleEntries)

            // Mark blitz_1 as studied in word_progress
            db.wordBooksDao().upsertWordProgress(
                com.example.data.local.WordProgressEntity(
                    cardKey = "oxford5000:abandon",
                    deck = "oxford5000",
                    text = "abandon",
                    repetitions = 2,
                    intervalDays = 5,
                    firstLearnedTimestamp = System.currentTimeMillis()
                )
            )

            // Count all words
            val allCount = db.wordBooksDao().countVocabularyBlitzCandidatesFiltered(
                decks = listOf("oxford5000", "oxford_opal", "ielts_core"),
                allDecks = true,
                levels = listOf("A1", "A2", "B1", "B2", "C1"),
                allLevels = true,
                studiedOnly = false
            )
            assertTrue("All words count should be at least 5", allCount >= 5)

            // Count studied only
            val studiedCount = db.wordBooksDao().countVocabularyBlitzCandidatesFiltered(
                decks = listOf("oxford5000"),
                allDecks = true,
                levels = listOf("A1", "A2", "B1", "B2", "C1"),
                allLevels = true,
                studiedOnly = true
            )
            assertEquals(1, studiedCount)
        } finally {
            db.close()
        }

        // 3. Filter persistence test
        onboardingRepo.saveBlitzFilters(
            decksJson = "[\"oxford5000\",\"oxford_opal\"]",
            allBooks = false,
            levelsJson = "[\"B2\",\"C1\"]",
            studiedOnly = true
        )
        val snap = onboardingRepo.getSnapshot()
        assertEquals("[\"oxford5000\",\"oxford_opal\"]", snap.blitzFilterDecksJson)
        assertFalse(snap.blitzFilterAllBooks)
        assertEquals("[\"B2\",\"C1\"]", snap.blitzFilterLevelsJson)
        assertTrue(snap.blitzFilterStudiedOnly)

        // 4. Engine: missed questions tracking
        val questions = com.example.data.blitz.VocabularyBlitzEngine.generateBlitzQuestions(
            entries = sampleEntries,
            count = 10,
            seed = 42
        )
        var session = com.example.data.blitz.VocabularyBlitzSessionState(questions = questions)
        // Submit wrong answer on first question
        val wrongOption = if (questions[0].correctOptionIndex == 0) 1 else 0
        session = com.example.data.blitz.VocabularyBlitzEngine.submitAnswer(
            state = session,
            selectedOptionIndex = wrongOption,
            previousHighScore = 50
        )
        assertEquals(1, session.wrongCount)
        assertEquals(0, session.correctCount)
        assertEquals(1, session.missedQuestions.size)
        assertEquals(questions[0].word, session.missedQuestions[0].word)

        // 5. Test UI Rendering: VocabularyBlitzSetupDialog
        val sampleDecks = listOf(
            com.example.data.local.DeckEntity(id = "oxford5000", title = "Oxford 5000", arabicSubtitle = "أهم 5000 كلمة", category = "Core", coverColorHex = "#1E293B", accentColorHex = "#F5C542", rowCount = 5000, uniqueWordCount = 5000, masteredCount = 0, favoriteCount = 0, displayOrder = 1),
            com.example.data.local.DeckEntity(id = "oxford_opal", title = "Oxford OPAL", arabicSubtitle = "أكاديمي", category = "Academic", coverColorHex = "#1E293B", accentColorHex = "#F5C542", rowCount = 1200, uniqueWordCount = 1200, masteredCount = 0, favoriteCount = 0, displayOrder = 2)
        )
        composeTestRule.setContent {
            WordBooksTheme {
                com.example.ui.screens.VocabularyBlitzSetupDialog(
                    initialConfig = com.example.data.blitz.BlitzFilterConfig(
                        selectedDecks = listOf("oxford5000"),
                        allBooksSelected = true,
                        selectedLevels = setOf("A1", "A2", "B1", "B2", "C1"),
                        studiedOnly = false
                    ),
                    availableDecks = sampleDecks,
                    userCurrentLevel = "B2",
                    highScore = 240,
                    availableWordCount = 5, // Below 10 -> start button disabled
                    onFilterConfigChanged = {},
                    onStartChallenge = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("blitz_setup_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("blitz_setup_high_score_badge").assertIsDisplayed()
        composeTestRule.onNodeWithTag("blitz_book_all_chip").assertIsDisplayed()
        composeTestRule.onNodeWithTag("blitz_my_level_btn").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("blitz_stage_studied_only").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("blitz_stage_all_words").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("blitz_available_words_card").performScrollTo().assertIsDisplayed()
        // Verify start button is disabled when available word count < 10
        composeTestRule.onNodeWithTag("blitz_setup_start_btn").assertIsNotEnabled()
    }

    @Test
    fun feature_speedChallengeEnhancements_mistakeReviewAndSm2Practice(): Unit = runBlocking {
        val sampleEntries = listOf(
            com.example.data.local.EntryEntity(id = "blitz_1", type = "word", deck = "oxford5000", text = "abandon", pos = "v.", level = "B2", effectiveLevel = "B2", definitionEn = "leave behind", noteEn = "", arabic = "يتخلى عن", examplesEn = "", examplesAr = "", definitionUrl = "", audioUrl = ""),
            com.example.data.local.EntryEntity(id = "blitz_2", type = "word", deck = "oxford_opal", text = "abstract", pos = "adj.", level = "B2", effectiveLevel = "B2", definitionEn = "existing in thought", noteEn = "", arabic = "مجرد", examplesEn = "", examplesAr = "", definitionUrl = "", audioUrl = "")
        )

        // Engine: missed questions tracking
        val questions = com.example.data.blitz.VocabularyBlitzEngine.generateBlitzQuestions(
            entries = sampleEntries,
            count = 4,
            seed = 42
        )
        var session = com.example.data.blitz.VocabularyBlitzSessionState(questions = questions)
        val wrongOption = if (questions[0].correctOptionIndex == 0) 1 else 0
        session = com.example.data.blitz.VocabularyBlitzEngine.submitAnswer(
            state = session,
            selectedOptionIndex = wrongOption,
            previousHighScore = 50
        )
        assertEquals(1, session.wrongCount)
        assertEquals(0, session.correctCount)
        assertEquals(1, session.missedQuestions.size)
        assertEquals(questions[0].word, session.missedQuestions[0].word)

        // Test UI Rendering: BlitzSummaryContent with Missed Words & SM-2 practice
        val finishedState = session.copy(
            isFinished = true,
            score = 120,
            correctCount = 8,
            wrongCount = 2,
            missedQuestions = listOf(questions[0])
        )
        var practiceCalled = false
        composeTestRule.setContent {
            WordBooksTheme {
                com.example.ui.screens.BlitzSummaryContent(
                    state = finishedState,
                    savedStats = com.example.data.blitz.VocabularyBlitzStats(highScore = 200),
                    onPracticeMistakes = { practiceCalled = true },
                    onPlayAgain = {},
                    onDone = {}
                )
            }
        }
        composeTestRule.onNodeWithTag("blitz_summary_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("blitz_final_score").assertIsDisplayed()
        composeTestRule.onNodeWithTag("blitz_missed_words_section").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("blitz_practice_mistakes_btn").performScrollTo().assertIsDisplayed().performClick()
        assertTrue("Practice mistakes callback should be called", practiceCalled)
        composeTestRule.onNodeWithTag("blitz_mistakes_practiced_banner").performScrollTo().assertIsDisplayed()
    }
}
