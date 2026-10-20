package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

const val LRI = '\u2066' // Left-to-Right Isolate
const val PDI = '\u2069' // Pop Directional Isolate

/**
 * Checks whether a character is in the Arabic Unicode blocks.
 */
fun isArabicChar(c: Char): Boolean {
    return (c in '\u0600'..'\u06FF') ||
           (c in '\u0750'..'\u077F') ||
           (c in '\u08A0'..'\u08FF') ||
           (c in '\uFB50'..'\uFDFF') ||
           (c in '\uFE70'..'\uFEFF')
}

/**
 * Checks whether a character is an Arabic-specific punctuation mark.
 */
fun isArabicPunctuation(c: Char): Boolean {
    return c == '،' || c == '؟' || c == '؛' || c == '۔' || c == '٪' || c == '«' || c == '»'
}

/**
 * Checks whether a character is a Latin letter.
 */
fun isLatinChar(c: Char): Boolean {
    return (c in 'A'..'Z') || (c in 'a'..'z') || (c in '\u00C0'..'\u024F')
}

/**
 * Checks whether the text contains any Arabic characters.
 */
fun hasArabicCharacters(text: String): Boolean {
    return text.any { isArabicChar(it) }
}

/**
 * Checks whether the text contains any Latin letters.
 */
fun hasLatinCharacters(text: String): Boolean {
    return text.any { isLatinChar(it) }
}

/**
 * Checks if a character is permitted inside an English run between Latin letters/digits.
 */
private fun isAllowedInRun(c: Char): Boolean {
    if (isLatinChar(c) || c.isDigit()) return true
    if (c == ' ' || c == '\t' || c == '\'' || c == '’' || c == '`') return true
    if (c == '+' || c == '-' || c == '/' || c == '=' || c == '%' || c == '_') return true
    if (c == ',' || c == '.' || c == '!' || c == '?') return true
    if (c == '(' || c == ')' || c == '"' || c == '“' || c == '”') return true
    return false
}

/**
 * Helper function to strip markdown bold asterisks, conversational fluff, and convert formatting asterisks to bullets
 * via the centralized [com.example.data.ai.AiPromptContract] Single Source of Truth.
 */
fun String.stripMarkdownAsterisks(): String {
    return com.example.data.ai.AiPromptContract.sanitizeAiPlainText(this)
}

/**
 * Wraps each run of English text in U+2066 (LRI) ... U+2069 (PDI).
 * A run = Latin letters/digits plus the spaces, apostrophes, + - / , . ! ? ( ) " ' % = between them.
 * Never merges runs across line breaks, and does NOT include a trailing colon or Arabic punctuation inside the isolate.
 */
fun isolateLatin(rawText: String): String {
    val text = rawText.stripMarkdownAsterisks()
    if (text.isEmpty() || !hasLatinCharacters(text)) {
        return text
    }

    return text.lines().joinToString("\n") { line ->
        isolateLatinInLine(line)
    }
}

private fun isolateLatinInLine(line: String): String {
    if (!hasLatinCharacters(line)) return line

    val sb = StringBuilder(line.length + 32)
    var i = 0
    val n = line.length

    while (i < n) {
        val c = line[i]

        // An English run starts with a Latin letter
        // (or a digit immediately followed by a Latin letter, e.g. "3rd")
        val canStart = isLatinChar(c) || (c.isDigit() && i + 1 < n && isLatinChar(line[i + 1]))

        if (!canStart) {
            sb.append(c)
            i++
            continue
        }

        val start = i
        while (i < n && isAllowedInRun(line[i]) && !isArabicChar(line[i]) && !isArabicPunctuation(line[i]) && line[i] != ':') {
            i++
        }

        var end = i

        // Trim invalid trailing characters from the end of the candidate run
        while (end > start) {
            val last = line[end - 1]
            if (last == ' ' || last == '\t' || last == ',' || last == '+' || last == '-' || last == '/' || last == '=' || last == '_' || last == '(' || last == '[') {
                end--
                continue
            }
            if (last == '.' || last == '!' || last == '?') {
                // If the punctuation is immediately preceded by an unmatched closing bracket or quote,
                // then the punctuation belongs to the outer sentence!
                var openCount = 0
                var closeCount = 0
                for (k in start until end - 1) {
                    if (line[k] == '(') openCount++
                    if (line[k] == ')') closeCount++
                }
                if (closeCount > openCount) {
                    end--
                    continue
                }
            }
            if (last == ')') {
                // Check if there is an unmatched opening bracket inside [start, end)
                var openCount = 0
                var closeCount = 0
                for (k in start until end) {
                    if (line[k] == '(') openCount++
                    if (line[k] == ')') closeCount++
                }
                if (closeCount > openCount) {
                    end--
                    continue
                }
            }
            if (last == '\'' || last == '’' || last == '`') {
                var quoteCount = 0
                for (k in start until end) {
                    if (line[k] == '\'' || line[k] == '’' || line[k] == '`') quoteCount++
                }
                if (quoteCount % 2 != 0) {
                    end--
                    continue
                }
            }
            if (last == '"' || last == '“' || last == '”') {
                var quoteCount = 0
                for (k in start until end) {
                    if (line[k] == '"' || line[k] == '“' || line[k] == '”') quoteCount++
                }
                if (quoteCount % 2 != 0) {
                    end--
                    continue
                }
            }
            break
        }

        val candidate = line.substring(start, end)
        if (hasLatinCharacters(candidate)) {
            // Avoid double wrapping if already isolated
            if (candidate.startsWith(LRI) && candidate.endsWith(PDI)) {
                sb.append(candidate)
            } else {
                sb.append(LRI).append(candidate).append(PDI)
            }
        } else {
            sb.append(candidate)
        }

        // Append any characters that were trimmed between end and current scan position i
        if (end < i) {
            sb.append(line, end, i)
        }
    }

    return sb.toString()
}

/**
 * Composable that displays bidirectional mixed Arabic/English text correctly:
 * - Each English run is wrapped in LRI ... PDI.
 * - Multi-line text is split into lines and rendered as individual paragraphs.
 * - Lines containing Arabic are rendered RTL and right-aligned.
 * - Lines containing no Arabic are rendered LTR and left-aligned.
 */
@Composable
fun BidiText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    textAlign: TextAlign? = null,
    fillMaxWidth: Boolean = true
) {
    if (text.isEmpty()) return

    val isolated = remember(text) { isolateLatin(text) }
    val baseModifier = if (fillMaxWidth) modifier.fillMaxWidth() else modifier
    val hasArabic = remember(isolated) { hasArabicCharacters(isolated) }
    val direction = if (hasArabic) TextDirection.Rtl else TextDirection.Ltr
    val resolvedAlign = textAlign ?: if (hasArabic) TextAlign.Right else TextAlign.Left

    Text(
        text = isolated,
        modifier = baseModifier,
        style = style.copy(textDirection = direction, textAlign = resolvedAlign),
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        lineHeight = lineHeight,
        maxLines = maxLines,
        overflow = overflow
    )
}

/**
 * Sanitizes mixed grammar lesson Arabic titles so that English keywords and Arabic titles
 * are separated cleanly into pure Arabic phrases, preventing RTL/LTR bidirectional jumbling.
 */
fun cleanGrammarArabicTitle(rawTitleAr: String): String {
    val trimmed = rawTitleAr.trim()
    return when (trimmed) {
        "القدرة: can و can't" -> "القدرة والاستطاعة"
        "الفعل to be: am, is, are" -> "فعل الكينونة (to be)"
        "أدوات التعريف والتنكير: a, an, the" -> "أدوات التعريف والتنكير"
        "there is و there are" -> "التعبير عن الوجود والوفرة"
        "حروف الجر للمكان والزمان: in, on, at" -> "حروف الجر للمكان والزمان"
        "المعدود وغير المعدود: some و any" -> "الأسماء المعدودة وغير المعدودة"
        "الماضي من to be: was و were" -> "الماضي من فعل الكينونة"
        "المستقبل: will و be going to" -> "صيغ التعبير عن المستقبل"
        "have to و don't have to و must" -> "أفعال الإلزام والضرورة"
        "النصيحة: should و shouldn't" -> "إعطاء النصيحة والتوجيه"
        "much و many و a lot of و a few و a little" -> "محددات الكمية والمقدار"
        "used to و would للعادات في الماضي" -> "العادات في الماضي"
        "wish و if only" -> "التمني والندم"
        "too و enough" -> "أدوات الإفراط والكفاية"
        "as ... as و the more ... the more" -> "تراكيب المقارنة المتلازمة"
        "جمل الصلة المحددة: who, which, that" -> "جمل الصلة المحددة"
        "أفعال الاحتمال: might, may, could, must, can't" -> "أفعال الاحتمال والاستنتاج"
        "أدوات الكمية: both, either, neither, all, none" -> "أدوات الشمول والبدائل"
        "أدوات الربط: although, however, because, so" -> "أدوات الربط والسببية"
        "السببية: have/get something done" -> "صيغة السببية"
        "would rather و had better و prefer" -> "التفضيل والنصح"
        "used to و be used to و get used to" -> "التعبير عن العادات والتأقلم"
        "so و such" -> "أدوات التوكيد والنتيجة"
        "it's time وتراكيب مشابهة" -> "تراكيب التمني والوقت الحاضر"
        else -> {
            if (trimmed.contains(":") && trimmed.substringAfter(":").any { isLatinChar(it) }) {
                val arabicPart = trimmed.substringBefore(":").trim()
                if (arabicPart.isNotBlank() && hasArabicCharacters(arabicPart)) {
                    arabicPart
                } else {
                    trimmed
                }
            } else {
                trimmed
            }
        }
    }
}
