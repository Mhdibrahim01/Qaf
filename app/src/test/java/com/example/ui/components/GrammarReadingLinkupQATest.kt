package com.example.ui.components

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.GrammarExerciseItem
import com.example.data.local.ReadingSentenceItem
import com.example.data.local.ReadingTextJson
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Senior QA Regression Suite: Grammar <-> Reading Linkup Verification
 * Validates data integrity, bidirectional mapping consistency, and exercise availability.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GrammarReadingLinkupQATest {

    private lateinit var context: Context
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private lateinit var grammarLessonsJson: List<JsonObject>
    private lateinit var readingTextsJson: List<ReadingTextJson>
    private lateinit var grammarIdsSet: Set<String>

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        val grammarStr = context.assets.open("grammar_lessons.json").bufferedReader().use { it.readText() }
        val grammarArr = json.parseToJsonElement(grammarStr).jsonArray
        grammarLessonsJson = grammarArr.map { it.jsonObject }
        grammarIdsSet = grammarLessonsJson.mapNotNull { it["id"]?.jsonPrimitive?.content }.toSet()

        val readingStr = context.assets.open("reading_texts.json").bufferedReader().use { it.readText() }
        readingTextsJson = json.decodeFromString<List<ReadingTextJson>>(readingStr)
    }

    @Test
    fun `QA 1 - Total Grammar Lessons must be 90`() {
        assertEquals(90, grammarLessonsJson.size)
        assertEquals(90, grammarIdsSet.size)
    }

    @Test
    fun `QA 2 - Reading texts must have zero dangling grammar IDs at text and sentence level`() {
        val danglingTextGids = mutableListOf<String>()
        val danglingSentenceGids = mutableListOf<String>()

        for (reading in readingTextsJson) {
            for (gid in reading.grammarIds) {
                if (gid !in grammarIdsSet) {
                    danglingTextGids.add("Text ${reading.id} references invalid gid '$gid'")
                }
            }
            for (sentence in reading.sentences) {
                val sgid = sentence.grammarId
                if (!sgid.isNullOrBlank() && sgid !in grammarIdsSet) {
                    danglingSentenceGids.add("Text ${reading.id} sentence '${sentence.en}' references invalid gid '$sgid'")
                }
            }
        }

        assertTrue("Found dangling grammar IDs at text level: $danglingTextGids", danglingTextGids.isEmpty())
        assertTrue("Found dangling grammar IDs at sentence level: $danglingSentenceGids", danglingSentenceGids.isEmpty())
    }

    @Test
    fun `QA 3 - Every sentence grammar_id must be declared in parent text grammar_ids`() {
        val undeclaredSentenceGids = mutableListOf<String>()

        for (reading in readingTextsJson) {
            val textGids = reading.grammarIds.toSet()
            for ((idx, sentence) in reading.sentences.withIndex()) {
                val sgid = sentence.grammarId
                if (!sgid.isNullOrBlank() && sgid !in textGids) {
                    undeclaredSentenceGids.add("Text ${reading.id} S#$idx has sgid '$sgid' not in text grammar_ids $textGids")
                }
            }
        }

        assertTrue("Found undeclared sentence grammar_ids: $undeclaredSentenceGids", undeclaredSentenceGids.isEmpty())
    }

    @Test
    fun `QA 4 - Every reading text must contain at least one grammar-tagged sentence`() {
        val untaggedTexts = mutableListOf<String>()

        for (reading in readingTextsJson) {
            val hasTaggedSentence = reading.sentences.any { !it.grammarId.isNullOrBlank() }
            if (!hasTaggedSentence) {
                untaggedTexts.add(reading.id)
            }
        }

        assertTrue("Reading texts with 0 grammar-tagged sentences: $untaggedTexts", untaggedTexts.isEmpty())
    }

    @Test
    fun `QA 5 - All 90 grammar lessons must have valid exercises for Offline Practice Dialog`() {
        for (g in grammarLessonsJson) {
            val gid = g["id"]?.jsonPrimitive?.content ?: ""
            val exercisesRaw = g["exercises"]?.jsonArray
            assertNotNull("Lesson $gid must have an exercises array", exercisesRaw)
            assertTrue("Lesson $gid must have at least 3 exercises for the dialog", (exercisesRaw?.size ?: 0) >= 3)
        }
    }

    @Test
    fun `QA 6 - Level tiers between reading texts and grammar rules must be compatible`() {
        val levelOrder = listOf("A1", "A2", "B1", "B2", "C1", "C2")
        val grammarLevelMap = grammarLessonsJson.associate {
            (it["id"]?.jsonPrimitive?.content ?: "") to (it["level"]?.jsonPrimitive?.content ?: "A1")
        }

        val severeMismatches = mutableListOf<String>()

        for (reading in readingTextsJson) {
            val rLevel = reading.level.uppercase()
            val rIdx = levelOrder.indexOf(rLevel)
            if (rIdx == -1) continue

            for (gid in reading.grammarIds) {
                val gLevel = grammarLevelMap[gid]?.uppercase() ?: continue
                val gIdx = levelOrder.indexOf(gLevel)
                if (gIdx != -1 && kotlin.math.abs(rIdx - gIdx) >= 2) {
                    severeMismatches.add("Text ${reading.id} ($rLevel) paired with $gid ($gLevel)")
                }
            }
        }

        assertTrue("Found severe level mismatches between reading and grammar: $severeMismatches", severeMismatches.isEmpty())
    }

    @Test
    fun `QA 7 - 100 percent Grammar Coverage (Zero orphan grammar lessons in reading)`() {
        val linkedGrammarIds = mutableSetOf<String>()
        for (reading in readingTextsJson) {
            linkedGrammarIds.addAll(reading.grammarIds)
            for (sentence in reading.sentences) {
                sentence.grammarId?.let { if (it.isNotBlank()) linkedGrammarIds.add(it) }
            }
        }

        val orphanLessons = grammarIdsSet - linkedGrammarIds
        assertTrue(
            "Found orphan grammar lessons with 0 reading texts: $orphanLessons",
            orphanLessons.isEmpty()
        )
        assertEquals("All 90 grammar lessons must be linked in reading", 90, linkedGrammarIds.size)
    }

    @Test
    fun `QA 8 - Total Reading Texts must be at least 97`() {
        assertTrue(
            "Expected at least 97 reading texts, but found ${readingTextsJson.size}",
            readingTextsJson.size >= 97
        )
    }
}
