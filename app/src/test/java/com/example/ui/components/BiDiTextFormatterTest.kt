package com.example.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BiDiTextFormatterTest {

    @Test
    fun testExplicitTrailingExampleExtraction() {
        val input = "1) I تأخذ am (مثال: I am happy)."
        val parsed = extractTrailingExample(input)
        assertEquals("1) I تأخذ am", parsed.mainText)
        assertEquals("I am happy", parsed.extractedExample)
    }

    @Test
    fun testTrailingParentheticalExampleExtraction() {
        val input = "- في حالة النفي، نضيف not بعد is أو are: .(There is not / There are not)"
        val parsed = extractTrailingExample(input)
        assertEquals("- في حالة النفي، نضيف not بعد is أو are", parsed.mainText)
        assertEquals("There is not / There are not", parsed.extractedExample)
    }

    @Test
    fun testTrailingExampleWithColon() {
        val input = "- في حالة السؤال، نبدأ بـ Is أو Are: مثال: Is there a pen? / Are there pens?"
        val parsed = extractTrailingExample(input)
        assertEquals("- في حالة السؤال، نبدأ بـ Is أو Are", parsed.mainText)
        assertEquals("Is there a pen? / Are there pens?", parsed.extractedExample)
    }

    @Test
    fun testContrastiveExclusionsRemainInline() {
        val input = "نسيان وضع (a) أو (an) قبل الاسم المفرد، والصواب قول (There is a car) وليس (There is car)."
        val parsed = extractTrailingExample(input)
        // Should NOT extract 'There is car' because it is preceded by 'وليس'
        assertNull("Contrast phrase must not be extracted into separate example box", parsed.extractedExample)
        assertEquals(input.trimEnd('.'), parsed.mainText.trimEnd('.'))
    }

    @Test
    fun testFormulaHeaderNotTreatedAsExample() {
        val formula = "التركيب: There is + Singular Noun | There are + Plural Noun"
        val parsed = extractTrailingExample(formula)
        assertNull(parsed.extractedExample)
        assertEquals(formula, parsed.mainText)
    }

    @Test
    fun testInlineEnglishPillsWrapping() {
        val arabicWithEnglish = "نستخدم تعبيري (There is) و (There are) للحديث عن المفرد والجمع."
        val annotated = buildBiDiPillAnnotatedString(arabicWithEnglish)

        // Verifies English terms were wrapped in LRI and PDI isolates
        val text = annotated.text
        assertTrue("Contains isolated There is", text.contains("${LRI}${LRM}There is${LRM}${PDI}"))
        assertTrue("Contains isolated There are", text.contains("${LRI}${LRM}There are${LRM}${PDI}"))

        // Verifies annotations exist for pill background drawing
        val pillAnnotations = annotated.getStringAnnotations("EN_PILL", 0, annotated.length)
        assertEquals(2, pillAnnotations.size)
        assertEquals("There is", pillAnnotations[0].item)
        assertEquals("There are", pillAnnotations[1].item)
    }

    @Test
    fun testBareEnglishWordsWrappedInPills() {
        val input = "نضيف not بعد is أو are في النفي"
        val annotated = buildBiDiPillAnnotatedString(input)
        val text = annotated.text

        assertTrue("not is wrapped in isolate", text.contains("${LRI}${LRM}not${LRM}${PDI}"))
        assertTrue("is is wrapped in isolate", text.contains("${LRI}${LRM}is${LRM}${PDI}"))
        assertTrue("are is wrapped in isolate", text.contains("${LRI}${LRM}are${LRM}${PDI}"))

        val pillAnnotations = annotated.getStringAnnotations("EN_PILL", 0, annotated.length)
        val pillTerms = pillAnnotations.map { it.item }
        assertTrue(pillTerms.contains("not"))
        assertTrue(pillTerms.contains("is"))
        assertTrue(pillTerms.contains("are"))
    }

    @Test
    fun testPureArabicUnchanged() {
        val pureArabic = "تستخدم الأسماء المعدودة للتعبير عن الأشياء التي يمكن عدها."
        val annotated = buildBiDiPillAnnotatedString(pureArabic)
        assertEquals(pureArabic, annotated.text)
        val pillAnnotations = annotated.getStringAnnotations("EN_PILL", 0, annotated.length)
        assertTrue(pillAnnotations.isEmpty())
    }
}
