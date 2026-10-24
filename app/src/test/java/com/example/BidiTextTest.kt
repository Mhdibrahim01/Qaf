package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import com.example.ui.components.LRI
import com.example.ui.components.LRM
import com.example.ui.components.NBSP
import com.example.ui.components.PDI
import com.example.ui.components.RLM
import com.example.ui.components.WJ
import com.example.ui.components.isolateLatin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BidiTextTest {

    private fun String.normalizeHelpers(): String = this
        .replace(LRM.toString(), "")
        .replace(RLM.toString(), "")
        .replace(WJ.toString(), "")
        .replace(NBSP, ' ')
        .replace(":\n", ": ")
        .replace(".\nمثال:", ". مثال:")

    @Test
    fun testColonAfterEnglishRun() {
        val input = "صيغة The + comparative: نستخدمها للتعبير عن التناسب"
        val output = isolateLatin(input).normalizeHelpers()

        // The Latin run should be wrapped in LRI ... PDI
        // The colon ':' MUST be outside the isolate
        val expected = "صيغة ${LRI}The + comparative${PDI}: نستخدمها للتعبير عن التناسب"
        assertEquals(expected, output)
        assertFalse("Colon must not be inside isolate", output.contains(":${PDI}"))
    }

    @Test
    fun testBrackets() {
        // Subcase 1: Arabic sentence with English formula in brackets
        val input1 = "صيغة (The + comparative) هي الأكثر شيوعاً"
        val output1 = isolateLatin(input1).normalizeHelpers()
        val expected1 = "صيغة (${LRI}The + comparative${PDI}) هي الأكثر شيوعاً"
        assertEquals("Outer brackets must stay outside the isolate", expected1, output1)

        // Subcase 2: Pure English line stays in LTR base direction
        val input2 = "The verb (to be) is used here."
        val output2 = isolateLatin(input2).normalizeHelpers()
        assertTrue("Inner brackets must remain intact", output2.contains("(to be)"))

        // Subcase 3: Multiple formulas in parentheses with trailing period
        val input3 = "صيغة التناسب المزدوج للمقارنة المتقدمة (The + comparative, the + comparative)."
        val output3 = isolateLatin(input3).normalizeHelpers()
        val expected3 = "صيغة التناسب المزدوج للمقارنة المتقدمة (${LRI}The + comparative, the + comparative${PDI})."
        assertEquals("Parentheses and trailing period must stay outside the isolate", expected3, output3)
    }

    @Test
    fun testPeriodInsideEnglishSentence() {
        // In "مثال: The harder you work, the more you achieve."
        // The period terminates the English example, so it must stay inside the isolate
        val input = "مثال: The harder you work, the more you achieve."
        val output = isolateLatin(input).normalizeHelpers()
        val expected = "مثال: ${LRI}The harder you work, the more you achieve.${PDI}"
        assertEquals(expected, output)
        assertTrue("Period must be inside the isolate", output.contains(".${PDI}"))
    }

    @Test
    fun testArabicTextWithNoEnglishStaysUnchanged() {
        val input = "تستخدم تراكيب المقارنة المتقدمة للتعبير عن التناسب بين حدثين."
        val output = isolateLatin(input)
        assertEquals("Pure Arabic text must remain completely unchanged", input, output)
        assertFalse("Should not contain LRI", output.contains(LRI))
        assertFalse("Should not contain PDI", output.contains(PDI))
    }

    @Test
    fun testLessonG068FormulasAndExamples() {
        val line1 = "1) The + comparative, the + comparative: نستخدمها للتعبير عن التناسب بين حدثين (كلما... كلما...). مثال: The harder you work, the more you achieve."
        val out1 = isolateLatin(line1).normalizeHelpers()
        val expected1 = "1) ${LRI}The + comparative, the + comparative${PDI}: نستخدمها للتعبير عن التناسب بين حدثين (كلما... كلما...). مثال: ${LRI}The harder you work, the more you achieve.${PDI}"
        assertEquals(expected1, out1)

        val line2 = "2) Comparative + and + comparative: للتعبير عن التغير التدريجي المستمر. مثال: It is getting colder and colder."
        val out2 = isolateLatin(line2).normalizeHelpers()
        val expected2 = "2) ${LRI}Comparative + and + comparative${PDI}: للتعبير عن التغير التدريجي المستمر. مثال: ${LRI}It is getting colder and colder.${PDI}"
        assertEquals(expected2, out2)

        val line3 = "3) Much/Far/A lot + comparative: لتقوية معنى المقارنة. مثال: This car is much faster than that one."
        val out3 = isolateLatin(line3).normalizeHelpers()
        val expected3 = "3) ${LRI}Much/Far/A lot + comparative${PDI}: لتقوية معنى المقارنة. مثال: ${LRI}This car is much faster than that one.${PDI}"
        assertEquals(expected3, out3)
    }

    @Test
    fun testGenerateVisualComparisonArtifacts() {
        val artifactDir = File(System.getProperty("java.io.tmpdir"), "wordbooks_artifacts")
        artifactDir.mkdirs()

        val sampleTextLines = listOf(
            "1) The + comparative, the + comparative: نستخدمها للتعبير عن التناسب بين حدثين (كلما... كلما...). مثال: The harder you work, the more you achieve.",
            "2) Comparative + and + comparative: للتعبير عن التغير التدريجي المستمر. مثال: It is getting colder and colder.",
            "3) Much/Far/A lot + comparative: لتقوية معنى المقارنة. مثال: This car is much faster than that one.",
            "4) Not nearly as... as: للتعبير عن وجود فارق كبير جداً في المقارنة. مثال: This is not nearly as good as the previous version."
        )

        // 1. Render BEFORE (raw mixed text without LRI/PDI isolates)
        val beforeBitmap = renderCards(
            title = "BEFORE (Standard BiDi without isolateLatin - Scrambled)",
            lines = sampleTextLines,
            applyIsolation = false
        )
        val beforeFile = File(artifactDir, "before_bidi.png")
        FileOutputStream(beforeFile).use { out ->
            beforeBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        assertTrue(beforeFile.exists())

        // 2. Render AFTER (with isolateLatin LRI/PDI isolates + per-line RTL alignment)
        val afterBitmap = renderCards(
            title = "AFTER (With isolateLatin U+2066..U+2069 Isolates - Fixed)",
            lines = sampleTextLines,
            applyIsolation = true
        )
        val afterFile = File(artifactDir, "after_bidi.png")
        FileOutputStream(afterFile).use { out ->
            afterBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        assertTrue(afterFile.exists())
    }

    private fun renderCards(title: String, lines: List<String>, applyIsolation: Boolean): Bitmap {
        val width = 960
        val height = 620
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background (#141416 dark surface)
        canvas.drawColor(Color.rgb(0x14, 0x14, 0x16))

        // Card Container (#1a1a1c)
        val cardPaint = Paint().apply {
            color = Color.rgb(0x1e, 0x1e, 0x22)
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val borderPaint = Paint().apply {
            color = if (applyIsolation) Color.rgb(0x2E, 0x7D, 0x32) else Color.rgb(0xC6, 0x28, 0x28)
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        canvas.drawRoundRect(24f, 24f, (width - 24).toFloat(), (height - 24).toFloat(), 20f, 20f, cardPaint)
        canvas.drawRoundRect(24f, 24f, (width - 24).toFloat(), (height - 24).toFloat(), 20f, 20f, borderPaint)

        // Title text
        val titlePaint = TextPaint().apply {
            color = if (applyIsolation) Color.rgb(0x81, 0xC7, 0x84) else Color.rgb(0xEF, 0x9A, 0x9A)
            textSize = 24f
            isAntiAlias = true
            isFakeBoldText = true
        }
        canvas.drawText(title, 48f, 70f, titlePaint)

        val headerPaint = TextPaint().apply {
            color = Color.rgb(0xE5, 0xC0, 0x7B) // Gold accent
            textSize = 20f
            isAntiAlias = true
            isFakeBoldText = true
        }
        canvas.drawText("Lesson g068: Advanced comparison structures • الشرح والقواعد", 48f, 108f, headerPaint)

        // Body TextPaint
        val bodyPaint = TextPaint().apply {
            color = Color.rgb(0xEE, 0xEE, 0xF0)
            textSize = 18f
            isAntiAlias = true
        }

        var currentY = 140f
        val contentWidth = width - 96

        lines.forEach { rawLine ->
            val processedLine = if (applyIsolation) isolateLatin(rawLine) else rawLine

            val layout = StaticLayout.Builder.obtain(
                processedLine,
                0,
                processedLine.length,
                bodyPaint,
                contentWidth
            )
                .setTextDirection(TextDirectionHeuristics.RTL)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(6f, 1.2f)
                .build()

            canvas.save()
            canvas.translate(48f, currentY)
            layout.draw(canvas)
            canvas.restore()

            currentY += layout.height + 24f
        }

        return bitmap
    }

    @Test
    fun testPreservesExerciseBlanksAndClues() {
        val q1 = "I ____ play football well."
        val out1 = isolateLatin(q1).normalizeHelpers()
        assertTrue("Blanks must be preserved", out1.contains("____"))
        assertEquals("I ____ play football well.", out1.replace(LRI.toString(), "").replace(PDI.toString(), ""))

        val q5 = "He ____ (not / can) cook."
        val out5 = isolateLatin(q5).normalizeHelpers()
        assertTrue("Blanks must be preserved", out5.contains("____"))
        assertTrue("Clues must be preserved", out5.contains("(not / can)"))
        assertEquals("He ____ (not / can) cook.", out5.replace(LRI.toString(), "").replace(PDI.toString(), ""))

        val markdownWithBlanks = "Rule: __bold__ and blank ____."
        val outMarkdown = isolateLatin(markdownWithBlanks)
        assertTrue("Bold markdown markers removed", outMarkdown.contains("bold"))
        assertFalse("Double underscore around bold removed", outMarkdown.contains("__bold__"))
        assertTrue("Four-underscore blank preserved", outMarkdown.contains("____"))
    }

    @Test
    fun testGrammarAnswerMatching() {
        assertTrue(com.example.ui.screens.isGrammarAnswerCorrect("can't", "can't"))
        assertTrue(com.example.ui.screens.isGrammarAnswerCorrect("cannot", "can't"))
        assertTrue(com.example.ui.screens.isGrammarAnswerCorrect("can not", "can't"))
        assertTrue(com.example.ui.screens.isGrammarAnswerCorrect("can’t", "can't")) // curly apostrophe
        assertTrue(com.example.ui.screens.isGrammarAnswerCorrect("can't.", "can't"))
        assertTrue(com.example.ui.screens.isGrammarAnswerCorrect("Can't", "can't"))
        assertFalse(com.example.ui.screens.isGrammarAnswerCorrect("could", "can't"))
    }
}
