package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.BatchTranslateManager
import com.example.data.ai.GeminiApiKeyManager
import com.example.data.ai.GeminiDeckAndTranslationService
import com.example.data.importer.DataImportRepository
import com.example.data.importer.ImportProgressState
import com.example.data.importer.Sm2Rating
import com.example.data.local.EntryEntity
import com.example.data.local.SentenceEntity
import com.example.data.local.StudyPlanProfileEntity
import com.example.data.local.WordBooksDatabase
import com.example.data.local.WordProgressEntity
import com.example.data.onboarding.OnboardingPreferencesRepository
import com.example.data.onboarding.OnboardingPreferencesState
import com.example.data.plan.StudyPlanRepository
import com.example.ui.screens.OnboardingScreen
import com.example.ui.theme.WordBooksTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.Calendar
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OnboardingStep1Test {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createIsolatedDataStore(context: Context): OnboardingPreferencesRepository {
        val file = File(context.cacheDir, "onboarding_test_${UUID.randomUUID()}.preferences_pb")
        val ds = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            produceFile = { file }
        )
        return OnboardingPreferencesRepository(context, ds)
    }

    @Test
    fun verifyTrueUpsertPreservesUserEditsAndAllProgressAcrossReimport() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, WordBooksDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.wordBooksDao()
        val repo = DataImportRepository(context, db)

        // 1. Seed initial entry (ar_source = "file")
        val initialEntry = EntryEntity(
            id = "501",
            type = "word",
            deck = "oxford5000",
            text = "achieve",
            pos = "verb",
            level = "A2",
            definitionEn = "to succeed in reaching a particular goal",
            noteEn = "",
            examplesEn = "She achieved remarkable results.",
            examplesAr = "حققت نتائج مذهلة.",
            definitionUrl = "",
            audioUrl = "https://example.com/achieve.mp3",
            arabic = "يحقق، ينجز",
            arabicSource = "file",
            ipa = "/əˈtʃiːv/",
            dataVersion = 1
        )
        val nonUserEntry = EntryEntity(
            id = "502",
            type = "word",
            deck = "oxford5000",
            text = "benefit",
            pos = "noun",
            level = "A2",
            definitionEn = "old definition",
            noteEn = "",
            examplesEn = "old example",
            examplesAr = "مثال قديم",
            definitionUrl = "",
            audioUrl = "",
            arabic = "فائدة قديمة",
            arabicSource = "file",
            ipa = "/ˈbenɪفɪt/",
            dataVersion = 1
        )
        dao.upsertEntriesBatchPreservingUserEdits(listOf(initialEntry, nonUserEntry))

        // 2. User edits entry #501 Arabic (sets arabic_source = 'user') and rates it in SRS + saves a sentence
        repo.saveUserArabicForEntry("501", "يُنجز هدفاً (تعديل المستخدم)")
        val progressAfterRate = repo.applySm2Review(
            deckId = "oxford5000",
            wordText = "achieve",
            rating = Sm2Rating.EASY,
            entryId = "501"
        )
        repo.toggleFavorite("oxford5000", "achieve")
        repo.saveExampleSentence(
            englishText = "She achieved remarkable results.",
            arabicText = "حققت نتائج مذهلة.",
            sourceWord = "achieve"
        )

        assertEquals("501", progressAfterRate.entryId)

        // 3. Re-run batch import with updated content for both entries
        val reimportBatch = listOf(
            initialEntry.copy(
                definitionEn = "overwritten definition that should NOT apply to user row",
                arabic = "معنى جديد من الملف يجب ألا يمسح تعديل المستخدم",
                arabicSource = "file",
                dataVersion = 3
            ),
            nonUserEntry.copy(
                definitionEn = "an advantage that something gives you",
                examplesEn = "Regular exercise has many benefits.",
                examplesAr = "للتمارين المنتظمة فوائد عديدة.",
                audioUrl = "https://example.com/benefit.mp3",
                arabic = "فائدة، منفعة",
                arabicSource = "file",
                dataVersion = 3
            )
        )
        dao.upsertEntriesBatchPreservingUserEdits(reimportBatch)

        // 4. Verify user-edited entry #501 was untouched and no duplicate rows were created
        assertEquals(2, dao.getTotalEntryCount())
        val after501 = dao.getEntryById("501")!!
        assertEquals("يُنجز هدفاً (تعديل المستخدم)", after501.arabic)
        assertEquals("user", after501.arabicSource)

        // 5. Verify non-user entry #502 received updated content fields
        val after502 = dao.getEntryById("502")!!
        assertEquals("an advantage that something gives you", after502.definitionEn)
        assertEquals("https://example.com/benefit.mp3", after502.audioUrl)
        assertEquals("فائدة، منفعة", after502.arabic)

        // 6. Verify SRS progress, favorites, and saved sentences all survived intact
        val savedProgress = dao.getWordProgress("oxford5000:achieve")!!
        assertTrue(savedProgress.isFavorite)
        assertEquals(1, savedProgress.repetitions)
        assertEquals("501", savedProgress.entryId)
        assertEquals(1, dao.getSavedSentencesCount())

        db.close()
    }

    @Test
    fun verifyStep4QueryUsesNotExistsByEntryIdAndFiltersContentWordsAndNullSafe() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, WordBooksDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.wordBooksDao()

        val testEntries = listOf(
            // 1. Valid B2 content word WITH audio -> should be #1
            EntryEntity(
                id = "10",
                type = "word",
                deck = "oxford5000",
                text = "abandon",
                pos = "verb",
                level = "B2",
                definitionEn = "to leave",
                noteEn = "",
                examplesEn = "",
                definitionUrl = "",
                audioUrl = "https://example.com/abandon.mp3",
                arabic = "يتخلى عن"
            ),
            // 2. Valid B2 content word from oxford_opal WITHOUT audio -> should be after audio cards
            EntryEntity(
                id = "20",
                type = "academic",
                deck = "oxford_opal",
                text = "hypothesis",
                pos = "noun",
                level = "B2",
                definitionEn = "idea",
                noteEn = "",
                examplesEn = "",
                definitionUrl = "",
                audioUrl = "",
                arabic = "فرضية"
            ),
            // 3. Function word (preposition) -> MUST BE EXCLUDED
            EntryEntity(
                id = "30",
                type = "word",
                deck = "oxford5000",
                text = "beneath",
                pos = "preposition",
                level = "B2",
                definitionEn = "under",
                noteEn = "",
                examplesEn = "",
                definitionUrl = "",
                audioUrl = "https://example.com/beneath.mp3",
                arabic = "تحت"
            ),
            // 4. Short word (< 4 letters) -> MUST BE EXCLUDED
            EntryEntity(
                id = "40",
                type = "word",
                deck = "oxford5000",
                text = "act",
                pos = "verb",
                level = "B2",
                definitionEn = "do",
                noteEn = "",
                examplesEn = "",
                definitionUrl = "",
                audioUrl = "https://example.com/act.mp3",
                arabic = "يتصرف"
            ),
            // 5. Already rated entry (by entry_id = "50") -> MUST BE EXCLUDED
            EntryEntity(
                id = "50",
                type = "word",
                deck = "oxford5000",
                text = "accurate",
                pos = "adjective",
                level = "B2",
                definitionEn = "correct",
                noteEn = "",
                examplesEn = "",
                definitionUrl = "",
                audioUrl = "https://example.com/accurate.mp3",
                arabic = "دقيق"
            ),
            // 6. Same word text "abandon" with DIFFERENT entry_id ("60", noun) not rated -> MUST BE INCLUDED because lookup is by entry_id
            EntryEntity(
                id = "60",
                type = "word",
                deck = "oxford5000",
                text = "abandon",
                pos = "noun",
                level = "B2",
                definitionEn = "lack of inhibition",
                noteEn = "",
                examplesEn = "",
                definitionUrl = "",
                audioUrl = "https://example.com/abandon_n.mp3",
                arabic = "اندفاع"
            )
        )
        dao.upsertEntriesBatchPreservingUserEdits(testEntries)

        // Insert a progress row with NULL entry_id AND a progress row with entry_id = "50"
        // Verify NULL entry_id never empties NOT EXISTS result!
        dao.upsertWordProgress(
            WordProgressEntity(
                cardKey = "oxford5000:legacy_null_id",
                entryId = null,
                deck = "oxford5000",
                text = "legacy"
            )
        )
        dao.upsertWordProgress(
            WordProgressEntity(
                cardKey = "oxford5000:accurate",
                entryId = "50",
                deck = "oxford5000",
                text = "accurate",
                repetitions = 2
            )
        )

        val candidates = dao.getOnboardingFirstWinEntries("B2")
        // Audio entries ("10" and "60") come first, then non-audio ("20")
        assertEquals(setOf("10", "60"), candidates.take(2).map { it.id }.toSet())
        assertEquals("20", candidates[2].id)

        db.close()
    }

    @Test
    fun verifyMigration12To13AddsEntryIdAndBackfillsFromEntries() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration_test_${UUID.randomUUID()}.db"
        val callback = object : SupportSQLiteOpenHelper.Callback(12) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS entries (
                        id TEXT NOT NULL PRIMARY KEY,
                        type TEXT NOT NULL,
                        deck TEXT NOT NULL,
                        text TEXT NOT NULL,
                        pos TEXT NOT NULL,
                        level TEXT NOT NULL,
                        definition_en TEXT NOT NULL,
                        note_en TEXT NOT NULL,
                        examples_en TEXT NOT NULL,
                        examples_ar TEXT NOT NULL,
                        definition_url TEXT NOT NULL,
                        audio_url TEXT NOT NULL,
                        arabic TEXT NOT NULL,
                        arabic_source TEXT NOT NULL,
                        ipa TEXT NOT NULL,
                        syllables TEXT NOT NULL,
                        extra_examples_json TEXT NOT NULL,
                        data_version INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS word_progress (
                        card_key TEXT NOT NULL PRIMARY KEY,
                        deck TEXT NOT NULL,
                        text TEXT NOT NULL,
                        is_favorite INTEGER NOT NULL,
                        is_mastered INTEGER NOT NULL,
                        repetitions INTEGER NOT NULL,
                        interval_days INTEGER NOT NULL,
                        ease_factor REAL NOT NULL,
                        due_timestamp INTEGER NOT NULL,
                        last_reviewed_timestamp INTEGER NOT NULL,
                        again_count INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO entries VALUES (
                        '777', 'word', 'oxford5000', 'sustain', 'verb', 'B2',
                        'def', '', '', '', '', '', 'يدعم', 'file', '', '', '', 3
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO word_progress VALUES (
                        'oxford5000:sustain', 'oxford5000', 'sustain', 1, 0, 2, 6, 2.5, 1000, 900, 0
                    )
                    """.trimIndent()
                )
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
        }

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(callback)
                .build()
        )
        val sqlDb = helper.writableDatabase
        WordBooksDatabase.MIGRATION_12_13.migrate(sqlDb)

        val cursor = sqlDb.query("SELECT entry_id FROM word_progress WHERE card_key = 'oxford5000:sustain'")
        assertTrue(cursor.moveToFirst())
        assertEquals("777", cursor.getString(0))
        cursor.close()
        sqlDb.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun verifyStep1WelcomeUiSkipAndResumeAndAllowAiDefaultFalse() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("wordbooks_batch_translate_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        GeminiApiKeyManager.clearCustomApiKey(context)

        val db = Room.inMemoryDatabaseBuilder(context, WordBooksDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.wordBooksDao()
        val geminiService = GeminiDeckAndTranslationService(dao)
        val batchManager = BatchTranslateManager(context, dao, geminiService)
        val planRepo = StudyPlanRepository(context, dao)
        val onboardingRepo = createIsolatedDataStore(context)

        // 1. Confirm allowAi is false by default on fresh install and no developer key is active
        assertFalse(batchManager.uiState.value.allowAi)
        assertTrue(GeminiApiKeyManager.getAllConfiguredKeys(context).isEmpty())

        // 2. Fresh launch starts at Step 1 (lastCompletedStep = 0 -> currentStepToResume = 1)
        val initialSnap = onboardingRepo.getSnapshot()
        assertFalse(initialSnap.onboardingCompleted)
        assertEquals(0, initialSnap.lastCompletedStep)
        assertEquals(1, initialSnap.currentStepToResume)

        var getStartedClicked = false
        var skipClicked = false
        var retryClicked = false

        composeTestRule.setContent {
            WordBooksTheme {
                OnboardingScreen(
                    onboardingState = initialSnap,
                    importState = ImportProgressState(
                        isImporting = true,
                        importedRows = 2400,
                        totalExpectedRows = 9862
                    ),
                    totalEntriesCount = 2400,
                    onCompleteStep1 = { getStartedClicked = true },
                    onSkipOnboarding = { skipClicked = true },
                    onRetryImport = { retryClicked = true }
                )
            }
        }

        // Verify Qaf logo, English tagline, Arabic subtitle, progress bar, Skip, and Get started
        composeTestRule.onNodeWithTag("onboarding_qaf_logo", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag("onboarding_welcome_tagline_en", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag("onboarding_welcome_tagline_ar", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag("onboarding_import_status_bar", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag("onboarding_import_progress_indicator", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag("btn_onboarding_skip").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_onboarding_get_started").assertIsDisplayed()

        composeTestRule.onNodeWithTag("btn_onboarding_get_started").performClick()
        assertTrue(getStartedClicked)

        // 3. Persist Step 1 completion & simulate force-stop / relaunch -> resumes at Step 2
        onboardingRepo.setLastCompletedStep(1)
        val resumedSnap = onboardingRepo.getSnapshot()
        assertFalse(resumedSnap.onboardingCompleted)
        assertEquals(1, resumedSnap.lastCompletedStep)
        assertEquals(2, resumedSnap.currentStepToResume)

        // 4. Verify Skip writes default profile (B2, general_fluency, 30m, 6d, Friday rest) ONLY when no profile exists
        assertNull(dao.getStudyPlanProfile())
        val createdDefault = planRepo.getOrCreateProfile()
        assertEquals("B2", createdDefault.level)
        assertEquals("general_fluency", createdDefault.goal)
        assertEquals(30, createdDefault.minutesPerDay)
        assertEquals(6, createdDefault.studyDaysPerWeek)
        assertTrue(createdDefault.restDaysJson.contains(Calendar.FRIDAY.toString()))

        // 5. Verify Skip / Redo NEVER overwrites an existing custom profile or changes allowAi
        val customProfile = StudyPlanProfileEntity(
            id = 1,
            level = "C1",
            goal = "ielts_prep",
            minutesPerDay = 60,
            studyDaysPerWeek = 5,
            startDate = "2026-10-21",
            selectedDecksJson = "[\"oxford5000\",\"ielts_core\"]",
            restDaysJson = "[6]",
            updatedAt = 12345L
        )
        dao.upsertStudyPlanProfile(customProfile)
        batchManager.setAllowAi(true)

        onboardingRepo.startRedoSetup(
            existingLevel = customProfile.level,
            existingGoal = customProfile.goal,
            existingMinutes = customProfile.minutesPerDay,
            existingStudyDays = customProfile.studyDaysPerWeek
        )
        // Simulate user pressing Skip during Redo
        if (dao.getStudyPlanProfile() == null) {
            planRepo.getOrCreateProfile()
        }
        onboardingRepo.markOnboardingCompleted()

        val preservedProfile = dao.getStudyPlanProfile()!!
        assertEquals("C1", preservedProfile.level)
        assertEquals("ielts_prep", preservedProfile.goal)
        assertEquals(60, preservedProfile.minutesPerDay)
        assertEquals(5, preservedProfile.studyDaysPerWeek)
        assertTrue(batchManager.uiState.value.allowAi)

        db.close()
    }
}
