package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.importer.DataImportRepository
import com.example.data.importer.WordFilterState
import com.example.data.local.WordBooksDao
import com.example.data.local.WordBooksDatabase
import com.example.ui.screens.OnboardingStep4Defaults
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DeckWordCountConsistencyTest {

    private lateinit var context: Context
    private lateinit var db: WordBooksDatabase
    private lateinit var dao: WordBooksDao
    private lateinit var repo: DataImportRepository

    companion object {
        val EXPECTED_UNIQUE_COUNTS = mapOf(
            "oxford5000" to 4958,
            "oxford_opal" to 1923,
            "oxford_phrases" to 750,
            "everyday_expressions" to 600,
            "essential_idioms" to 462,
            "ielts_core" to 3575,
            "it_industry" to 1622,
            "cefr_c1" to 1853,
            "pte_academic" to 2651,
            "business_english" to 1250,
            "career_interview" to 406,
            "medical_healthcare" to 405
        )

        const val EXPECTED_SUM_OF_DECK_UNIQUE_WORDS = 20455
    }

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, WordBooksDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.wordBooksDao()
        repo = DataImportRepository(context, db)
        repo.ensureImportedOnFirstLaunch(forceRecheck = true)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun verifyAll12DecksUniqueCountsMatchFlashcardsAndDecksTable() = runBlocking {
        val decks = dao.getDecksList().filter { it.id != DataImportRepository.SAVED_WORDS_DECK_ID }
        assertEquals("Must have all 12 vocabulary decks", 12, decks.size)

        var sumUniqueInDecksTable = 0

        for (deck in decks) {
            val expectedUnique = EXPECTED_UNIQUE_COUNTS[deck.id]
            assertNotNull("Deck ${deck.id} must be in EXPECTED_UNIQUE_COUNTS", expectedUnique)

            // 1. DeckEntity.uniqueWordCount in Room decks table
            assertEquals(
                "DeckEntity.uniqueWordCount for deck ${deck.id}",
                expectedUnique!!,
                deck.uniqueWordCount
            )
            sumUniqueInDecksTable += deck.uniqueWordCount

            // 2. Database distinct count in entries table
            val daoDistinctCount = dao.getUniqueWordCountForDeck(deck.id)
            assertEquals(
                "dao.getUniqueWordCountForDeck for ${deck.id}",
                expectedUnique,
                daoDistinctCount
            )

            // 3. Flashcards card count (which groups multi-POS entries into a single card)
            val (flashcards, _) = repo.loadFlashcardDeckCards(deck.id)
            assertEquals(
                "Flashcards card count for ${deck.id} must equal uniqueWordCount",
                expectedUnique,
                flashcards.size
            )
        }

        assertEquals(
            "Sum of all 12 decks unique words in decks table",
            EXPECTED_SUM_OF_DECK_UNIQUE_WORDS,
            sumUniqueInDecksTable
        )

        // 4. Global total unique words query in entries table
        val totalUniqueCount = dao.getTotalUniqueWordCount()
        assertEquals(
            "dao.getTotalUniqueWordCount() must match expected sum across all decks",
            EXPECTED_SUM_OF_DECK_UNIQUE_WORDS,
            totalUniqueCount
        )
    }

    @Test
    fun verifyOxford5000LevelCountsMatchBetweenWordBooksAndFlashcards() = runBlocking {
        val (allCards, _) = repo.loadFlashcardDeckCards("oxford5000")
        assertEquals(4958, allCards.size)

        val levels = listOf("A1", "A2", "B1", "B2", "C1")
        val levelExpectedCounts = mapOf(
            "A1" to 898,
            "A2" to 867,
            "B1" to 802,
            "B2" to 1428,
            "C1" to 1314
        )

        for (lvl in levels) {
            val expectedLvlCount = levelExpectedCounts[lvl]!!

            // Unique words in Oxford 5000 having an effective_level at lvl
            val daoLvlCount = dao.getUniqueWordCountForDeckByLevel("oxford5000", lvl)
            assertEquals(
                "Level $lvl unique words in Oxford 5000",
                expectedLvlCount,
                daoLvlCount
            )

            // Filtered flashcards card count for lvl (matching WordBooksViewModel logic)
            val filteredCards = allCards.filter { card ->
                card.primaryLevel.equals(lvl, ignoreCase = true) ||
                    card.posVariants.any { it.level.equals(lvl, ignoreCase = true) }
            }
            assertEquals(
                "Filtered flashcards count for Level $lvl in Oxford 5000",
                expectedLvlCount,
                filteredCards.size
            )
        }
    }

    @Test
    fun verifyOnboardingStep4DefaultsMatchUniqueCounts() {
        for (onboardingDeck in OnboardingStep4Defaults.ALL_DECKS) {
            val expected = EXPECTED_UNIQUE_COUNTS[onboardingDeck.id]
            assertNotNull("Deck ${onboardingDeck.id} must be in EXPECTED_UNIQUE_COUNTS", expected)
            assertEquals(
                "OnboardingStep4Defaults entryCount for ${onboardingDeck.id}",
                expected!!,
                onboardingDeck.entryCount
            )
        }
    }
}
