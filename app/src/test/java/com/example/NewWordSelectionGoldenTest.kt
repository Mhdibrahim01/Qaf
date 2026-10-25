package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.test.core.app.ApplicationProvider
import com.example.data.importer.DataImportRepository
import com.example.data.local.DailyTaskItem
import com.example.data.local.StudyPlanProfileEntity
import com.example.data.local.WordBooksDatabase
import com.example.data.local.WordProgressEntity
import com.example.data.plan.StudyPlanRepository
import com.example.data.plan.TestTimeProvider
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NewWordSelectionGoldenTest {

    private lateinit var context: Context
    private lateinit var db: WordBooksDatabase
    private lateinit var repo: DataImportRepository
    private lateinit var planRepo: StudyPlanRepository
    private lateinit var timeProvider: TestTimeProvider
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setup() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, WordBooksDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        repo = DataImportRepository(context, db)
        repo.ensureImportedOnFirstLaunch(forceRecheck = true)

        // 2026-11-01 12:00:00 UTC (Sunday)
        timeProvider = TestTimeProvider(1793534400000L)
        planRepo = StudyPlanRepository(context, db.wordBooksDao(), timeProvider)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun goldenTestProfileA_B2_GeneralEnglish_Day1AndDay7() = runBlocking {
        // Profile A: B2, Oxford 5000 + OPAL, General English, 30 min/day (10 words target)
        val profileA = StudyPlanProfileEntity(
            id = 1,
            level = "B2",
            goal = "general_fluency",
            minutesPerDay = 30,
            studyDaysPerWeek = 6,
            startDate = "2026-11-01",
            selectedDecksJson = json.encodeToString(listOf("oxford5000", "oxford_opal")),
            restDaysJson = "[6]",
            startAtUserLevel = true,
            updatedAt = timeProvider.currentTimeMillis()
        )
        planRepo.updateProfile(profileA)

        // DAY 1
        val checklistDay1 = planRepo.getOrCreateDailyChecklist("2026-11-01")
        val tasksDay1 = json.decodeFromString<List<DailyTaskItem>>(checklistDay1.tasksJson)
        val newWordsTaskDay1 = tasksDay1.first { it.type == "new_words" }
        assertEquals(10, newWordsTaskDay1.targetCount)

        val day1Selection = planRepo.selectNewWordsForProfile(profileA, 10)
        val day1Words = day1Selection.words
        assertEquals(10, day1Words.size)
        assertNull(day1Selection.reasonString)

        // 1. Must be 100% deterministic (calling again with identical state yields identical word order)
        val repeatSelection = planRepo.selectNewWordsForProfile(profileA, 10)
        assertEquals(day1Words.map { it.id }, repeatSelection.words.map { it.id })

        // 2. Must not be alphabetically clumped (span at least 4 distinct initial letters)
        val initialLetters = day1Words.map { it.text.trim().first().lowercaseChar() }.distinct()
        assertTrue("Selection must span >= 4 distinct initial letters, got: $initialLetters", initialLetters.size >= 4)

        // 2. Must have 10 distinct lowercase headwords
        assertEquals(10, day1Words.map { it.text.trim().lowercase() }.distinct().size)

        // 3. Must include 5 Oxford 5000 words and 5 OPAL words
        val day1OpalWords = day1Words.filter { it.deck == "oxford_opal" }
        val day1OxfordWords = day1Words.filter { it.deck == "oxford5000" }
        assertEquals(5, day1OpalWords.size)
        assertEquals(5, day1OxfordWords.size)

        // 4. Oxford 5000 words must be at or near B2 (effective_level in B2, C1, B1)
        for (w in day1OxfordWords) {
            assertTrue(
                "Oxford words must have effective_level at/near B2: ${w.effectiveLevel}",
                w.effectiveLevel in listOf("B2", "C1", "B1")
            )
        }

        // 5. B2 user gets 30% NULL-level specialty share (3 words) + 2 graded OPAL words
        val nullLevelWords = day1Words.filter { it.effectiveLevel == null }
        assertEquals("B2 general_fluency should have 30% (3/10) NULL-level specialty words", 3, nullLevelWords.size)
        val gradedOpalWords = day1OpalWords.filter { it.effectiveLevel != null }
        assertEquals(2, gradedOpalWords.size)
        for (w in gradedOpalWords) {
            assertTrue(w.effectiveLevel in listOf("B2", "C1", "B1"))
        }

        // Simulate 6 days of study history (60 words studied across Days 1..6)
        val allStudiedIds = mutableSetOf<String>()
        for (day in 1..6) {
            planRepo.getOrCreateDailyChecklist("2026-11-0$day")
            val batch = planRepo.selectNewWordsForProfile(profileA, 10).words
            for (w in batch) {
                allStudiedIds.add(w.id)
                db.wordBooksDao().upsertWordProgress(
                    WordProgressEntity(
                        cardKey = "${w.deck}:${w.text.trim().lowercase()}",
                        entryId = w.id,
                        deck = w.deck,
                        text = w.text.trim().lowercase(),
                        repetitions = 1,
                        lastReviewedTimestamp = timeProvider.currentTimeMillis() + (day * 86_400_000L)
                    )
                )
            }
        }
        assertEquals(60, allStudiedIds.size)

        // DAY 7
        timeProvider.advanceDays(6) // Now Day 7 (2026-11-07)
        val checklistDay7 = planRepo.getOrCreateDailyChecklist("2026-11-07")
        val tasksDay7 = json.decodeFromString<List<DailyTaskItem>>(checklistDay7.tasksJson)
        val newWordsTaskDay7 = tasksDay7.first { it.type == "new_words" }
        assertEquals(10, newWordsTaskDay7.targetCount)

        val day7Selection = planRepo.selectNewWordsForProfile(profileA, 10)
        val day7Words = day7Selection.words
        assertEquals(10, day7Words.size)

        // Day 7 words must be 100% deterministic and span >= 4 distinct initial letters
        val repeatDay7 = planRepo.selectNewWordsForProfile(profileA, 10)
        assertEquals(day7Words.map { it.id }, repeatDay7.words.map { it.id })
        val day7InitialLetters = day7Words.map { it.text.trim().first().lowercaseChar() }.distinct()
        assertTrue("Day 7 selection must span >= 4 distinct initial letters, got: $day7InitialLetters", day7InitialLetters.size >= 4)

        // Day 7 words must NOT contain any previously studied words
        for (w in day7Words) {
            assertFalse("Day 7 must not repeat studied word ${w.id} (${w.text})", allStudiedIds.contains(w.id))
        }

        // Includes OPAL (3 NULL-level + 2 B2) and Oxford 5000 (5 B2) words
        assertEquals(5, day7Words.count { it.deck == "oxford_opal" })
        assertEquals(5, day7Words.count { it.deck == "oxford5000" })
        assertEquals(3, day7Words.count { it.effectiveLevel == null })
    }

    @Test
    fun goldenTestProfileB_A2_ITCareer_Day1AndDay7() = runBlocking {
        // Profile B: A2, IELTS + IT decks, IT career, 30 min/day (10 words target)
        val profileB = StudyPlanProfileEntity(
            id = 1,
            level = "A2",
            goal = "it_career",
            minutesPerDay = 30,
            studyDaysPerWeek = 6,
            startDate = "2026-11-01",
            selectedDecksJson = json.encodeToString(listOf("ielts_core", "it_industry")),
            restDaysJson = "[6]",
            startAtUserLevel = true,
            updatedAt = timeProvider.currentTimeMillis()
        )
        planRepo.updateProfile(profileB)

        // DAY 1
        val checklistDay1 = planRepo.getOrCreateDailyChecklist("2026-11-01")
        val tasksDay1 = json.decodeFromString<List<DailyTaskItem>>(checklistDay1.tasksJson)
        val newWordsTaskDay1 = tasksDay1.first { it.type == "new_words" }
        assertEquals(10, newWordsTaskDay1.targetCount)

        val day1Selection = planRepo.selectNewWordsForProfile(profileB, 10)
        val day1Words = day1Selection.words
        assertEquals(10, day1Words.size)
        assertNull("Both ielts_core and it_industry have Oxford-matched A2 words, so no shortfall reason needed", day1Selection.reasonString)

        // 1. Must be 100% deterministic and span >= 4 distinct initial letters
        val repeatDay1B = planRepo.selectNewWordsForProfile(profileB, 10)
        assertEquals(day1Words.map { it.id }, repeatDay1B.words.map { it.id })
        val initialLettersB = day1Words.map { it.text.trim().first().lowercaseChar() }.distinct()
        assertTrue("Selection must span >= 4 distinct initial letters, got: $initialLettersB", initialLettersB.size >= 4)

        // 2. Must come ONLY from selectedDecks (6 from ielts_core, 4 from it_industry) - never unselected oxford5000
        val ieltsWords = day1Words.filter { it.deck == "ielts_core" }
        val itWords = day1Words.filter { it.deck == "it_industry" }
        assertEquals(6, ieltsWords.size)
        assertEquals(4, itWords.size)

        // 3. Every word for an A2 user must have a non-null Oxford-matched effective_level at or near A2 (A2, B1, A1)
        // and NEVER NULL, B2, or C1!
        for (w in day1Words) {
            assertNotNull("A2 user must not receive NULL-level specialty words: ${w.text}", w.effectiveLevel)
            assertTrue(
                "A2 user words must have effective_level in A2/B1/A1, got ${w.effectiveLevel} for ${w.text}",
                w.effectiveLevel in listOf("A2", "B1", "A1")
            )
            assertEquals("Day 1 words should all match exact A2 effective_level", "A2", w.effectiveLevel)
        }

        // Simulate 6 days of study history (60 words studied across Days 1..6)
        val allStudiedIds = mutableSetOf<String>()
        for (day in 1..6) {
            val batch = planRepo.selectNewWordsForProfile(profileB, 10).words
            for (w in batch) {
                allStudiedIds.add(w.id)
                db.wordBooksDao().upsertWordProgress(
                    WordProgressEntity(
                        cardKey = "${w.deck}:${w.text.trim().lowercase()}",
                        entryId = w.id,
                        deck = w.deck,
                        text = w.text.trim().lowercase(),
                        repetitions = 1,
                        lastReviewedTimestamp = timeProvider.currentTimeMillis() + (day * 86_400_000L)
                    )
                )
            }
        }
        assertEquals(60, allStudiedIds.size)

        // DAY 7
        timeProvider.advanceDays(6)
        val day7Selection = planRepo.selectNewWordsForProfile(profileB, 10)
        val day7Words = day7Selection.words
        assertEquals(10, day7Words.size)

        // Day 7 words must be 100% deterministic and span >= 4 distinct initial letters
        val repeatDay7B = planRepo.selectNewWordsForProfile(profileB, 10)
        assertEquals(day7Words.map { it.id }, repeatDay7B.words.map { it.id })
        val day7InitialLettersB = day7Words.map { it.text.trim().first().lowercaseChar() }.distinct()
        assertTrue("Day 7 selection must span >= 4 distinct initial letters, got: $day7InitialLettersB", day7InitialLettersB.size >= 4)

        // Includes 6 IELTS and 4 IT words (only selected decks)
        assertEquals(6, day7Words.count { it.deck == "ielts_core" })
        assertEquals(4, day7Words.count { it.deck == "it_industry" })

        // Does NOT repeat studied words
        for (w in day7Words) {
            assertFalse("Day 7 must not repeat studied word ${w.id} (${w.text})", allStudiedIds.contains(w.id))
            assertEquals("A2", w.effectiveLevel)
        }
    }

    @Test
    fun testPteAcademicAndGeneratedDecksEffectiveLevels() = runBlocking {
        // Verify pte_academic ("B2-C1" raw level) has Oxford-matched effective_level or NULL
        val pteSelectionB2 = planRepo.selectNewWordsForProfile(
            StudyPlanProfileEntity(
                id = 1,
                level = "B2",
                goal = "ielts",
                minutesPerDay = 30,
                studyDaysPerWeek = 6,
                selectedDecksJson = json.encodeToString(listOf("pte_academic", "oxford5000"))
            ),
            targetCount = 10
        )
        assertEquals(10, pteSelectionB2.words.size)
        val pteWords = pteSelectionB2.words.filter { it.deck == "pte_academic" }
        assertTrue("pte_academic must contribute words to the daily mix", pteWords.isNotEmpty())
        assertTrue("pte_academic must contribute NULL-level specialty words for B2", pteWords.any { it.effectiveLevel == null })
        assertTrue("pte_academic must contribute B2-graded words for B2", pteWords.any { it.effectiveLevel == "B2" })

        // Verify business_english, career_interview, medical_healthcare treat synthetic raw levels as NULL unless in Oxford 5000
        val businessSelectionA2 = planRepo.selectNewWordsForProfile(
            StudyPlanProfileEntity(
                id = 1,
                level = "A2",
                goal = "general_fluency",
                minutesPerDay = 30,
                studyDaysPerWeek = 6,
                selectedDecksJson = json.encodeToString(listOf("business_english"))
            ),
            targetCount = 5
        )
        assertEquals(5, businessSelectionA2.words.size)
        for (w in businessSelectionA2.words) {
            assertEquals("A2", w.effectiveLevel)
        }
    }

    @Test
    fun testFromA1StartModeAndHonestShortfallReason() = runBlocking {
        // 1. "From A1" start mode (startAtUserLevel = false) for a B2 user starts at A1
        planRepo.saveOnboardingDecksAndPlacement(
            selectedDecks = listOf("oxford5000"),
            startAtUserLevel = false
        )
        val savedProfile = planRepo.getOrCreateProfile()
        assertFalse("startAtUserLevel should be persisted as false", savedProfile.startAtUserLevel)

        val fromA1Selection = planRepo.selectNewWordsForProfile(savedProfile, 10)
        assertEquals(10, fromA1Selection.words.size)
        for (w in fromA1Selection.words) {
            assertEquals("From A1 mode should start with A1 words", "A1", w.effectiveLevel)
        }

        // 2. Honest shortfall when an A2 user selects ONLY essential_idioms (only 3 headwords match Oxford A2/B1/A1; 465 are NULL)
        val idiomsOnlyA2 = StudyPlanProfileEntity(
            id = 1,
            level = "A2",
            goal = "general_fluency",
            minutesPerDay = 30,
            studyDaysPerWeek = 6,
            selectedDecksJson = json.encodeToString(listOf("essential_idioms")),
            startAtUserLevel = true
        )
        val partialShortfallResult = planRepo.selectNewWordsForProfile(idiomsOnlyA2, 10)
        assertEquals("Only the 3 Oxford-matched A2/B1/A1 entries in essential_idioms are eligible", 3, partialShortfallResult.words.size)
        assertTrue(partialShortfallResult.words.all { it.deck == "essential_idioms" && it.effectiveLevel in listOf("A2", "B1", "A1") })
        assertNotNull(partialShortfallResult.reasonString)
        assertTrue(partialShortfallResult.reasonString!!.contains("Fewer words today (3/10)", ignoreCase = true))

        // Mark those 3 words studied; now essential_idioms has 0 eligible words for A2
        for (w in partialShortfallResult.words) {
            db.wordBooksDao().upsertWordProgress(
                WordProgressEntity(
                    cardKey = "${w.deck}:${w.text.trim().lowercase()}",
                    entryId = w.id,
                    deck = w.deck,
                    text = w.text.trim().lowercase(),
                    repetitions = 1,
                    lastReviewedTimestamp = timeProvider.currentTimeMillis()
                )
            )
        }
        val zeroShortfallResult = planRepo.selectNewWordsForProfile(idiomsOnlyA2, 10)
        assertEquals("Must not silently fall back to unselected decks or NULL levels for A2", 0, zeroShortfallResult.words.size)
        assertNotNull(zeroShortfallResult.reasonString)
        assertTrue(zeroShortfallResult.reasonString!!.contains("Fewer words today (0/10): no A2 words in Essential Idioms", ignoreCase = true))

        // 3. When A2 user selects essential_idioms + oxford5000, fills from oxford5000 with reason string
        val idiomsAndOxfordA2 = idiomsOnlyA2.copy(
            selectedDecksJson = json.encodeToString(listOf("essential_idioms", "oxford5000"))
        )
        val filledResult = planRepo.selectNewWordsForProfile(idiomsAndOxfordA2, 10)
        assertEquals(10, filledResult.words.size)
        assertTrue(filledResult.words.all { it.deck == "oxford5000" && it.effectiveLevel == "A2" })
        assertNotNull(filledResult.reasonString)
        assertTrue(filledResult.reasonString!!.contains("No A2 words in Essential Idioms; filled from Oxford 5000", ignoreCase = true))

        // 4. Deck-filtered due counts only count selected decks
        val now = timeProvider.currentTimeMillis()
        db.wordBooksDao().upsertWordProgress(
            WordProgressEntity(
                cardKey = "oxford5000:abandon",
                entryId = "2",
                deck = "oxford5000",
                text = "abandon",
                repetitions = 1,
                dueTimestamp = now - 1000L,
                lastReviewedTimestamp = now - 86_400_000L
            )
        )
        db.wordBooksDao().upsertWordProgress(
            WordProgressEntity(
                cardKey = "it_industry:ability",
                entryId = "200001",
                deck = "it_industry",
                text = "ability",
                repetitions = 1,
                dueTimestamp = now - 1000L,
                lastReviewedTimestamp = now - 86_400_000L
            )
        )
        assertEquals(2, repo.getReviewDueTodayCount(nowMillis = now, selectedDecks = emptyList()))
        assertEquals(1, repo.getReviewDueTodayCount(nowMillis = now, selectedDecks = listOf("oxford5000")))
        assertEquals(1, repo.getReviewDueTodayCount(nowMillis = now, selectedDecks = listOf("it_industry")))
    }

    @Test
    fun testMigration13To14BackfillsEffectiveLevelAndPreservesUserData() {
        val dbName = "migration_test_13_14.db"
        context.deleteDatabase(dbName)

        val callback = object : SupportSQLiteOpenHelper.Callback(13) {
            override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `entries` (
                        `id` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `deck` TEXT NOT NULL,
                        `text` TEXT NOT NULL,
                        `pos` TEXT NOT NULL,
                        `level` TEXT NOT NULL,
                        `definition_en` TEXT NOT NULL,
                        `note_en` TEXT NOT NULL,
                        `examples_en` TEXT NOT NULL,
                        `examples_ar` TEXT NOT NULL,
                        `definition_url` TEXT NOT NULL,
                        `audio_url` TEXT NOT NULL,
                        `arabic` TEXT NOT NULL,
                        `arabic_source` TEXT NOT NULL,
                        `ipa` TEXT NOT NULL,
                        `syllables` TEXT NOT NULL,
                        `extra_examples_json` TEXT NOT NULL,
                        `data_version` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `word_progress` (
                        `cardKey` TEXT NOT NULL,
                        `entry_id` TEXT,
                        `deck` TEXT NOT NULL,
                        `text` TEXT NOT NULL,
                        `repetitions` INTEGER NOT NULL,
                        `intervalDays` INTEGER NOT NULL,
                        `easeFactor` REAL NOT NULL,
                        `dueTimestamp` INTEGER NOT NULL,
                        `lastReviewedTimestamp` INTEGER NOT NULL,
                        `againCount` INTEGER NOT NULL,
                        `isFavorite` INTEGER NOT NULL,
                        `isMastered` INTEGER NOT NULL,
                        PRIMARY KEY(`cardKey`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `study_plan_profile` (
                        `id` INTEGER NOT NULL,
                        `level` TEXT NOT NULL,
                        `goal` TEXT NOT NULL,
                        `minutesPerDay` INTEGER NOT NULL,
                        `studyDaysPerWeek` INTEGER NOT NULL,
                        `startDate` TEXT NOT NULL,
                        `selectedDecksJson` TEXT NOT NULL,
                        `restDaysJson` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }

            override fun onUpgrade(
                db: androidx.sqlite.db.SupportSQLiteDatabase,
                oldVersion: Int,
                newVersion: Int
            ) = Unit
        }

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(callback)
                .build()
        )

        val sqlDb = helper.writableDatabase
        // Seed v13 entries across decks
        sqlDb.execSQL("INSERT INTO entries VALUES ('1', 'word', 'oxford5000', 'ability', 'noun', 'A2', 'def', '', '', '', '', '', 'قدرة', 'user', '', '', '', 3)")
        sqlDb.execSQL("INSERT INTO entries VALUES ('6066', 'word', 'oxford_opal', 'ability', 'noun', '', 'def', '', '', '', '', '', 'قدرة', 'file', '', '', '', 3)")
        sqlDb.execSQL("INSERT INTO entries VALUES ('6070', 'word', 'oxford_opal', 'accompany', 'verb', '', 'def', '', '', '', '', '', 'يرافق', 'file', '', '', '', 3)")
        sqlDb.execSQL("INSERT INTO entries VALUES ('100028', 'word', 'ielts_core', 'ability', 'noun', 'B2', 'def', '', '', '', '', '', 'قدرة', 'file', '', '', '', 3)")
        sqlDb.execSQL("INSERT INTO entries VALUES ('100099', 'word', 'ielts_core', 'nonoxfordterm', 'noun', 'B2', 'def', '', '', '', '', '', 'مصطلح', 'file', '', '', '', 3)")
        sqlDb.execSQL("INSERT INTO entries VALUES ('400001', 'word', 'pte_academic', 'ability', 'noun', 'B2-C1', 'def', '', '', '', '', '', 'قدرة', 'file', '', '', '', 3)")
        sqlDb.execSQL("INSERT INTO entries VALUES ('500001', 'word', 'business_english', 'synergy', 'noun', 'C1', 'def', '', '', '', '', '', 'تآزر', 'file', '', '', '', 3)")

        // Seed v13 word_progress with null entry_id and user favorite/mastered state
        sqlDb.execSQL("INSERT INTO word_progress VALUES ('oxford5000:ability', NULL, 'oxford5000', 'ability', 3, 6, 2.6, 1730500000000, 1730000000000, 0, 1, 0)")
        sqlDb.execSQL("INSERT INTO study_plan_profile VALUES (1, 'B2', 'general_fluency', 30, 6, '2026-11-01', '[\"oxford5000\",\"oxford_opal\"]', '[6]', 1730000000000)")

        // Run MIGRATION_13_14
        WordBooksDatabase.MIGRATION_13_14.migrate(sqlDb)

        // Verify effective_level backfill
        val effectiveMap = mutableMapOf<String, String?>()
        sqlDb.query("SELECT id, effective_level, arabic_source FROM entries").use { c ->
            while (c.moveToNext()) {
                val id = c.getString(0)
                val eff = if (c.isNull(1)) null else c.getString(1)
                effectiveMap[id] = eff
                if (id == "1") {
                    assertEquals("User arabic_source must be preserved", "user", c.getString(2))
                }
            }
        }
        assertEquals("A2", effectiveMap["1"])       // oxford5000 A2
        assertEquals("A2", effectiveMap["6066"])    // oxford_opal matched to oxford5000 'ability' (A2)
        assertNull(effectiveMap["6070"])            // oxford_opal not in oxford5000 -> NULL
        assertEquals("A2", effectiveMap["100028"])  // ielts_core matched to oxford5000 'ability' (A2)
        assertNull(effectiveMap["100099"])          // ielts_core blanket B2 not in oxford5000 -> NULL
        assertEquals("A2", effectiveMap["400001"])  // pte_academic B2-C1 matched to oxford5000 'ability' (A2)
        assertNull(effectiveMap["500001"])          // business_english synthetic C1 not in oxford5000 -> NULL

        // Verify word_progress preserved and entry_id backfilled
        sqlDb.query("SELECT entry_id, repetitions, isFavorite FROM word_progress WHERE cardKey = 'oxford5000:ability'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("1", c.getString(0))
            assertEquals(3, c.getInt(1))
            assertEquals(1, c.getInt(2))
        }

        // Verify study_plan_profile.start_at_user_level added with default 1
        sqlDb.query("SELECT level, start_at_user_level FROM study_plan_profile WHERE id = 1").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("B2", c.getString(0))
            assertEquals(1, c.getInt(1))
        }

        helper.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun printSimulationDetails() = runBlocking {
        val sb = StringBuilder()
        sb.appendLine("=== PHASE 1 AFTER-FIX SIMULATION OUTPUT ===")
        // Profile A Day 1
        val profileA = StudyPlanProfileEntity(
            id = 1, level = "B2", goal = "general_fluency", minutesPerDay = 30, studyDaysPerWeek = 6,
            startDate = "2026-11-01", selectedDecksJson = json.encodeToString(listOf("oxford5000", "oxford_opal")),
            restDaysJson = "[6]", startAtUserLevel = true, updatedAt = timeProvider.currentTimeMillis()
        )
        val aDay1 = planRepo.selectNewWordsForProfile(profileA, 10)
        sb.appendLine("PROFILE_A_DAY_1 (Reason: ${aDay1.reasonString}):")
        aDay1.words.forEach {
            sb.appendLine("ID:${it.id} | TEXT:${it.text} | RAW_LEVEL:${it.level.ifBlank { "blank" }} | EFF_LEVEL:${it.effectiveLevel ?: "NULL"} | DECK:${it.deck}")
        }

        // Simulate 60 studied words across Days 1..6
        for (day in 1..6) {
            val batch = planRepo.selectNewWordsForProfile(profileA, 10).words
            batch.forEach {
                db.wordBooksDao().upsertWordProgress(
                    WordProgressEntity(
                        cardKey = "${it.deck}:${it.text.trim().lowercase()}",
                        entryId = it.id,
                        deck = it.deck,
                        text = it.text.trim().lowercase(),
                        repetitions = 1,
                        lastReviewedTimestamp = 1730000000000L + (day * 86400000L)
                    )
                )
            }
        }
        val aDay7 = planRepo.selectNewWordsForProfile(profileA, 10)
        sb.appendLine("PROFILE_A_DAY_7 (Reason: ${aDay7.reasonString}):")
        aDay7.words.forEach {
            sb.appendLine("ID:${it.id} | TEXT:${it.text} | RAW_LEVEL:${it.level.ifBlank { "blank" }} | EFF_LEVEL:${it.effectiveLevel ?: "NULL"} | DECK:${it.deck}")
        }

        // Profile B Day 1
        val profileB = StudyPlanProfileEntity(
            id = 1, level = "A2", goal = "it_career", minutesPerDay = 30, studyDaysPerWeek = 6,
            startDate = "2026-11-01", selectedDecksJson = json.encodeToString(listOf("ielts_core", "it_industry")),
            restDaysJson = "[6]", startAtUserLevel = true, updatedAt = timeProvider.currentTimeMillis()
        )
        val bDay1 = planRepo.selectNewWordsForProfile(profileB, 10)
        sb.appendLine("PROFILE_B_DAY_1 (Reason: ${bDay1.reasonString}):")
        bDay1.words.forEach {
            sb.appendLine("ID:${it.id} | TEXT:${it.text} | RAW_LEVEL:${it.level.ifBlank { "blank" }} | EFF_LEVEL:${it.effectiveLevel ?: "NULL"} | DECK:${it.deck}")
        }

        // Simulate 60 studied words across Days 1..6 for Profile B
        for (day in 1..6) {
            val batch = planRepo.selectNewWordsForProfile(profileB, 10).words
            batch.forEach {
                db.wordBooksDao().upsertWordProgress(
                    WordProgressEntity(
                        cardKey = "${it.deck}:${it.text.trim().lowercase()}",
                        entryId = it.id,
                        deck = it.deck,
                        text = it.text.trim().lowercase(),
                        repetitions = 1,
                        lastReviewedTimestamp = 1730000000000L + (day * 86400000L)
                    )
                )
            }
        }
        val bDay7 = planRepo.selectNewWordsForProfile(profileB, 10)
        sb.appendLine("PROFILE_B_DAY_7 (Reason: ${bDay7.reasonString}):")
        bDay7.words.forEach {
            sb.appendLine("ID:${it.id} | TEXT:${it.text} | RAW_LEVEL:${it.level.ifBlank { "blank" }} | EFF_LEVEL:${it.effectiveLevel ?: "NULL"} | DECK:${it.deck}")
        }
        sb.appendLine("=== SIMULATION OUTPUT END ===")

        val outFile = java.io.File("/tmp/phase1_simulation_results.txt")
        outFile.writeText(sb.toString())
    }
}
