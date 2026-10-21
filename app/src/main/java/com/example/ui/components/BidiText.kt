package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
const val RLI = '\u2067' // Right-to-Left Isolate
const val PDI = '\u2069' // Pop Directional Isolate
const val LRM = '\u200E' // Left-to-Right Mark (Strong LTR)
const val RLM = '\u200F' // Right-to-Left Mark (Strong RTL)
const val NBSP = '\u00A0' // Non-Breaking Space
const val WJ = '\u2060'   // Word Joiner

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
 * Checks whether a character is an Arabic letter (excluding Arabic punctuation).
 */
fun isArabicLetter(c: Char): Boolean {
    return isArabicChar(c) && !isArabicPunctuation(c)
}

/**
 * Checks whether a character is a Latin letter.
 */
fun isLatinChar(c: Char): Boolean {
    return (c in 'A'..'Z') || (c in 'a'..'z') || (c in '\u00C0'..'\u024F')
}

/**
 * Checks whether a character is a checkmark or cross symbol used in grammar examples.
 */
private fun isVerdictSymbol(c: Char): Boolean {
    return c == '✓' || c == '✗' || c == '✔' || c == '✘'
}

/**
 * Checks whether the text contains any Arabic letters.
 */
fun hasArabicCharacters(text: String): Boolean {
    return text.any { isArabicLetter(it) }
}

/**
 * Checks whether the text contains any Latin letters.
 */
fun hasLatinCharacters(text: String): Boolean {
    return text.any { isLatinChar(it) }
}

/**
 * Checks if a character is permitted inside an English run between Latin letters/digits/symbols.
 */
private fun isAllowedInRun(c: Char): Boolean {
    if (isLatinChar(c) || c.isDigit() || isVerdictSymbol(c)) return true
    if (c == ' ' || c == '\t' || c == NBSP || c == '\'' || c == '’' || c == '‘' || c == '`') return true
    if (c == '+' || c == '-' || c == '–' || c == '—' || c == '/' || c == '=' || c == '%' || c == '_' || c == '→') return true
    if (c == ',' || c == '.' || c == '!' || c == '?' || c == ';' || c == ':') return true
    if (c == '(' || c == ')' || c == '[' || c == ']' || c == '"' || c == '“' || c == '”') return true
    return false
}

/**
 * Helper function to strip markdown bold asterisks, conversational fluff, and convert formatting asterisks to bullets
 * via the centralized [com.example.data.ai.AiPromptContract] Single Source of Truth.
 */
fun String.stripMarkdownAsterisks(): String {
    return com.example.data.ai.AiPromptContract.sanitizeAiPlainText(this)
}

private val PRE_ISOLATE_CHARS_REGEX = Regex("""[\u2066\u2067\u2068\u2069\u200E\u200F]""")
private val ARABIC_COMMA_BETWEEN_LATIN_REGEX =
    Regex("""([A-Za-z0-9✓✗✔✘)'"\]]\s*)،(\s*[(\['"✓✗✔✘]*[A-Za-z0-9])""")
private val ARABIC_SEMICOLON_BETWEEN_LATIN_REGEX =
    Regex("""([A-Za-z0-9✓✗✔✘)'"\]]\s*)؛(\s*[(\['"✓✗✔✘]*[A-Za-z0-9])""")
private val PAREN_EXAMPLE_PREFIX_REGEX =
    Regex("""\((مثال|مثل|مثلاً|خطأ|الصواب|صحيح)\s*:\s*([A-Za-z0-9✓✗✔✘'"(][^)]*)\)""")
private val INLINE_EXAMPLE_BREAK_REGEX =
    Regex("""([.؟!])\s+(?=(مثال|التركيب|القاعدة|الصيغة)\s*:)""")
private val INLINE_COLON_SENTENCE_BREAK_REGEX =
    Regex("""([\u0600-\u06FF)\]]\s*):\s+(?=[✓✗✔✘'"(]*[A-Z][A-Za-z']*(?:\s+[A-Za-z0-9',.?!()+\-/✓✗✔✘"]+){3,})""")
private val TRAILING_ARABIC_TRANSLATION_SPLIT_REGEX =
    Regex("""^([✓✗✔✘'"(]*[A-Za-z0-9][^\u0600-\u06FF]{8,}?[.!?]?)\s+(\([\u0600-\u06FF][^)]*\)\.?)$""")

/**
 * Pre-normalizes mixed Arabic/English text before directional isolation:
 * 1. Strips any existing Bidi control characters so runs are never split prematurely.
 * 2. Converts Arabic commas/semicolons (`،` / `؛`) located between English segments into ASCII `,` / `;`.
 * 3. Unwraps mixed parentheses like `(مثال: I am happy)` into `مثال: (I am happy)` so brackets never mismatch.
 * 4. Breaks inline `. مثال:` / `. التركيب:` and trailing full English example sentences onto their own lines
 *    so the first English word (`I`, `If`, etc.) never stays stranded at the end of an Arabic line.
 */
fun normalizeMixedBidiText(rawText: String): String {
    var s = rawText.stripMarkdownAsterisks()
    if (s.isEmpty()) return s

    s = PRE_ISOLATE_CHARS_REGEX.replace(s, "")
    s = ARABIC_COMMA_BETWEEN_LATIN_REGEX.replace(s, "$1,$2")
    s = ARABIC_SEMICOLON_BETWEEN_LATIN_REGEX.replace(s, "$1;$2")
    s = PAREN_EXAMPLE_PREFIX_REGEX.replace(s) { m ->
        "${m.groupValues[1]}: (${m.groupValues[2].trim()})"
    }

    val expandedLines = mutableListOf<String>()
    for (rawLine in s.lines()) {
        val trimmed = rawLine.trim()
        if (trimmed.isEmpty()) {
            expandedLines.add("")
            continue
        }
        val step1 = INLINE_EXAMPLE_BREAK_REGEX.replace(trimmed, "$1\n")
        for (subLine in step1.lines()) {
            val subTrimmed = subLine.trim()
            // Only split at colon before a 4+ word English sentence if the Arabic prefix is a full clause (> 22 chars)
            val colonBroken = if (subTrimmed.length > 35 && hasArabicCharacters(subTrimmed)) {
                INLINE_COLON_SENTENCE_BREAK_REGEX.replace(subTrimmed) { matchResult ->
                    val prefixLen = matchResult.range.first
                    if (prefixLen >= 22) "${matchResult.groupValues[1]}:\n" else matchResult.value
                }
            } else {
                subTrimmed
            }
            for (part in colonBroken.lines()) {
                val p = part.trim()
                if (p.isEmpty()) continue
                // If a line is `<English example sentence> (<Arabic translation>).` where total length > 42,
                // split the English sentence and the Arabic translation onto separate lines so each renders
                // in its native direction without awkward cross-direction line wrapping.
                val transMatch = TRAILING_ARABIC_TRANSLATION_SPLIT_REGEX.matchEntire(p)
                if (transMatch != null && p.length > 42 && !hasArabicCharacters(transMatch.groupValues[1])) {
                    expandedLines.add(transMatch.groupValues[1].trim())
                    expandedLines.add(transMatch.groupValues[2].trim())
                } else {
                    expandedLines.add(p)
                }
            }
        }
    }
    return expandedLines.joinToString("\n")
}

/**
 * Determines whether a single line should use Left-to-Right (`TextDirection.Ltr`) base direction.
 * Returns `true` when:
 * - The line contains no Arabic letters, OR
 * - The line starts with an English token/formula/example and any Arabic letters are only inside
 *   parenthetical glosses `(ترجمة)` or a trailing formula slot after `+` / `->` (e.g. `am/is/are + going to + الفعل في المصدر`).
 */
fun isLtrDominantLine(line: String): Boolean {
    val clean = PRE_ISOLATE_CHARS_REGEX.replace(line, "").trim()
    if (clean.isEmpty()) return false
    if (!hasArabicCharacters(clean)) return true
    if (!hasLatinCharacters(clean)) return false

    // Check first strong character (ignoring leading punctuation/brackets/verdict symbols)
    val firstStrong = clean.firstOrNull { isLatinChar(it) || isArabicLetter(it) } ?: return false
    if (!isLatinChar(firstStrong)) return false

    // Remove parenthetical segments `(...)` to see if Arabic only appears as parenthetical glosses
    val withoutParens = clean.replace(Regex("""\([^)]*\)"""), "")
    if (!hasArabicCharacters(withoutParens)) return true

    // Also treat English formulas where Arabic only appears after `+` or `->` or `, would +` as LTR-dominant
    val beforeFirstArabic = withoutParens.takeWhile { !isArabicLetter(it) }
    val latinCountBeforeArabic = beforeFirstArabic.count { isLatinChar(it) }
    val totalArabicLetters = withoutParens.count { isArabicLetter(it) }
    if (latinCountBeforeArabic >= 10 && (beforeFirstArabic.contains('+') || beforeFirstArabic.contains("->")) && totalArabicLetters <= 18) {
        return true
    }

    return false
}

/**
 * Wraps each run of English text in U+2066 U+200E ... U+200E U+2069 (for RTL lines)
 * or wraps inline Arabic glosses in U+2067 U+200F ... U+200F U+2069 (for LTR-dominant lines).
 */
fun isolateLatin(rawText: String): String {
    val normalized = normalizeMixedBidiText(rawText)
    if (normalized.isEmpty()) return normalized

    return normalized.lines().joinToString("\n") { line ->
        if (isLtrDominantLine(line)) {
            isolateArabicInLtrLine(line)
        } else {
            isolateLatinInRtlLine(line)
        }
    }
}

/**
 * For LTR-dominant lines (such as English formulas or example sentences with Arabic glosses in parentheses),
 * keeps the English text un-isolated at LTR base level 0 and isolates any Arabic segments in RLI + RLM ... RLM + PDI.
 */
private fun isolateArabicInLtrLine(line: String): String {
    if (!hasArabicCharacters(line)) return line

    // First isolate any parenthesized Arabic glosses `(ترجمة...)` as complete RTL units
    val parenRegex = Regex("""\(([^)]*[\u0600-\u06FF][^)]*)\)""")
    var result = parenRegex.replace(line) { m ->
        val inner = m.groupValues[1].trim()
        "$LRM($RLI$RLM$inner$RLM$PDI)$LRM"
    }

    // Also isolate any remaining unparenthesized Arabic runs (e.g. after `+` in `going to + الفعل في المصدر`)
    val bareArabicRegex = Regex("""(?<!\u200F)([\u0600-\u06FF][\u0600-\u06FF\s،؛؟\-/]*[\u0600-\u06FF]|[\u0600-\u06FF]+)""")
    result = bareArabicRegex.replace(result) { m ->
        val seg = m.value.trim()
        val nbspSeg = seg.replace(' ', NBSP)
        "$RLI$RLM$nbspSeg$RLM$PDI"
    }

    return "$LRM$result$LRM"
}

/**
 * Binds words inside an inline English segment with Non-Breaking Spaces (`\u00A0`) and Word Joiners (`\u2060`)
 * so that Android's StaticLayout never breaks a line in the middle of an `\u2066...\u2069` isolate.
 */
private fun bindInlineEnglishRun(candidate: String): String {
    // Bind hyphens and slashes between letters/digits with Word Joiner so `V-ing` or `have/has` never splits
    var bound = candidate
        .replace(Regex("""([A-Za-z0-9])-([A-Za-z0-9])"""), "$1$WJ-$WJ$2")
        .replace(Regex("""([A-Za-z0-9])/([A-Za-z0-9])"""), "$1$WJ/$WJ$2")

    // If the run is up to 34 chars, bind all internal spaces with NBSP so the entire phrase stays on one line
    if (candidate.length <= 34) {
        bound = bound.replace(' ', NBSP)
        return "$LRI$LRM$bound$LRM$PDI"
    }

    // If the run is longer than 34 chars and is still inline inside an RTL line, split it into
    // self-contained `<= 28`-char LRI..PDI clauses (preferring splits after commas `, ` or `; `)
    //Wait: if you split an English run inside an RTL line into two separate LRI..PDI isolates on the SAME line,
    //the outer RTL line would order Isolate 1 on the right and Isolate 2 on the left!
    //So we must NEVER split a single English sentence into multiple sibling isolates on the same RTL line!
    //Instead, keep it as a single LRI+LRM...LRM+PDI isolate and bind the first 2 words and last 2 words
    //with NBSP so a single word like `I` or `have helped.` is never orphaned!
    val words = bound.split(' ').filter { it.isNotEmpty() }
    if (words.size <= 5) {
        return "$LRI$LRM${words.joinToString(NBSP.toString())}$LRM$PDI"
    }
    val sb = StringBuilder()
    for (idx in words.indices) {
        if (idx > 0) {
            // Bind the first 3 words together and the last 3 words together with NBSP
            val useNbsp = idx <= 2 || idx >= words.size - 2
            sb.append(if (useNbsp) NBSP else ' ')
        }
        sb.append(words[idx])
    }
    return "$LRI$LRM$sb$LRM$PDI"
}

private fun isolateLatinInRtlLine(rawLine: String): String {
    // First, protect pure-Arabic parentheticals `(أتمنى لو ...)` with RLM around parentheses
    // so their opening/closing brackets never mirror the wrong way across line wraps.
    val arabicParenRegex = Regex("""\(([\u0600-\u06FF][^A-Za-z)]*)\)""")
    val line = arabicParenRegex.replace(rawLine) { m ->
        "$RLM($RLM${m.groupValues[1]}$RLM)$RLM"
    }

    if (!hasLatinCharacters(line)) return line

    val sb = StringBuilder(line.length + 48)
    var i = 0
    val n = line.length

    while (i < n) {
        val c = line[i]

        // Check if an English run can start here:
        // - A Latin letter
        // - A digit followed by a Latin letter (e.g. `3rd`, `V1`)
        // - An opening bracket/quote/verdict symbol (`(`, `[`, `"`, `'`, `✓`, `✗`) followed shortly by a Latin letter
        //   before any Arabic letter or closing bracket!
        val canStart = when {
            isLatinChar(c) -> true
            c.isDigit() && i + 1 < n && isLatinChar(line[i + 1]) -> true
            (c == '(' || c == '[' || c == '"' || c == '“' || c == '\'' || c == '‘' || isVerdictSymbol(c)) -> {
                var k = i + 1
                while (k < n && (line[k] == ' ' || line[k] == '(' || line[k] == '[' || line[k] == '"' || line[k] == '\'' || isVerdictSymbol(line[k]) || line[k] == '-' || line[k].isDigit())) {
                    k++
                }
                k < n && isLatinChar(line[k])
            }
            else -> false
        }

        if (!canStart) {
            sb.append(c)
            i++
            continue
        }

        val start = i
        while (i < n && isAllowedInRun(line[i]) && !isArabicChar(line[i]) && !isArabicPunctuation(line[i])) {
            // Stop at a colon `:` UNLESS the colon is followed by an English letter/symbol (e.g. inside an English pair)
            if (line[i] == ':') {
                var lookahead = i + 1
                while (lookahead < n && line[lookahead] == ' ') lookahead++
                val nextIsEnglish = lookahead < n && (isLatinChar(line[lookahead]) || isVerdictSymbol(line[lookahead]))
                // Even if next is English, if the run so far is a short label at the very start of the line
                // and the rest of the line has Arabic right after, check if there is Arabic before the next English
                if (!nextIsEnglish) {
                    break
                }
            }
            i++
        }

        var end = i

        // Trim invalid trailing characters from the end of the candidate run
        while (end > start) {
            val last = line[end - 1]
            if (last == ' ' || last == '\t' || last == NBSP || last == ',' || last == ';' || last == ':' ||
                last == '+' || last == '-' || last == '–' || last == '—' || last == '/' || last == '=' ||
                last == '_' || last == '→' || last == '(' || last == '['
            ) {
                end--
                continue
            }
            if (last == ')' || last == ']') {
                val openChar = if (last == ')') '(' else '['
                var openCount = 0
                var closeCount = 0
                for (k in start until end) {
                    if (line[k] == openChar) openCount++
                    if (line[k] == last) closeCount++
                }
                if (closeCount > openCount) {
                    end--
                    continue
                }
            }
            if (last == '.' || last == '!' || last == '?') {
                // Keep `.` inside the LTR run if the run is a sentence/clause or ends with a balanced `)`,
                // unless `)` before `.` was unmatched!
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
            if (last == '\'' || last == '’' || last == '`') {
                // Keep trailing apostrophe if it's part of a plural possessive like `students'` or balanced quotes
                var quoteCount = 0
                for (k in start until end) {
                    if (line[k] == '\'' || line[k] == '’' || line[k] == '‘' || line[k] == '`') quoteCount++
                }
                val precededByS = end - 2 >= start && (line[end - 2] == 's' || line[end - 2] == 'S')
                if (quoteCount % 2 != 0 && !precededByS) {
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

        // Also trim unmatched leading `(` or `"` if the closing bracket wasn't inside the English run
        // (e.g., `(can للإمكانية العامة)` where `(` opened before `can` and closed after Arabic)
        var actualStart = start
        while (actualStart < end) {
            val first = line[actualStart]
            if (first == '(' || first == '[') {
                val closeChar = if (first == '(') ')' else ']'
                var openCount = 0
                var closeCount = 0
                for (k in actualStart until end) {
                    if (line[k] == first) openCount++
                    if (line[k] == closeChar) closeCount++
                }
                if (openCount > closeCount) {
                    sb.append(first)
                    actualStart++
                    continue
                }
            }
            if (first == '"' || first == '“' || first == '\'' || first == '‘') {
                var quoteCount = 0
                for (k in actualStart until end) {
                    if (line[k] == '"' || line[k] == '“' || line[k] == '”' || line[k] == '\'' || line[k] == '‘' || line[k] == '’') {
                        quoteCount++
                    }
                }
                if (quoteCount == 1) {
                    sb.append(first)
                    actualStart++
                    continue
                }
            }
            break
        }

        val candidate = line.substring(actualStart, end)
        if (hasLatinCharacters(candidate)) {
            sb.append(bindInlineEnglishRun(candidate))
        } else {
            sb.append(candidate)
        }

        if (end < i) {
            sb.append(line, end, i)
        }
    }

    return sb.toString()
}

/**
 * Composable that displays bidirectional mixed Arabic/English text accurately:
 * - Pre-normalizes mixed punctuation, brackets, and embedded examples.
 * - Splits multi-line text into individual paragraphs so each paragraph is rendered with its own
 *   optimal base direction (`TextDirection.Ltr` for English-dominant lines/formulas/examples,
 *   `TextDirection.Rtl` for Arabic explanation lines).
 * - Prevents mid-phrase line breaks inside inline English segments so words and punctuation never scramble.
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

    val paragraphs = remember(text) {
        val normalized = normalizeMixedBidiText(text)
        normalized.lines().map { line ->
            val isLtr = isLtrDominantLine(line)
            val formatted = if (isLtr) isolateArabicInLtrLine(line) else isolateLatinInRtlLine(line)
            formatted to isLtr
        }
    }

    val baseModifier = if (fillMaxWidth) modifier.fillMaxWidth() else modifier

    if (paragraphs.size <= 1 || maxLines == 1) {
        val (singleText, isLtr) = if (paragraphs.size == 1) {
            paragraphs[0]
        } else {
            val isLtrFirst = isLtrDominantLine(text)
            isolateLatin(text) to isLtrFirst
        }
        val direction = if (isLtr) TextDirection.Ltr else TextDirection.Rtl
        val resolvedAlign = textAlign ?: if (isLtr) TextAlign.Left else TextAlign.Right

        Text(
            text = singleText,
            modifier = baseModifier,
            style = style.copy(textDirection = direction, textAlign = resolvedAlign),
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            lineHeight = lineHeight,
            maxLines = maxLines,
            overflow = overflow
        )
    } else {
        Column(
            modifier = baseModifier,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for ((paraText, isLtr) in paragraphs) {
                if (paraText.isBlank()) continue
                val direction = if (isLtr) TextDirection.Ltr else TextDirection.Rtl
                val resolvedAlign = textAlign ?: if (isLtr) TextAlign.Left else TextAlign.Right
                Text(
                    text = paraText,
                    modifier = if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier,
                    style = style.copy(textDirection = direction, textAlign = resolvedAlign),
                    color = color,
                    fontSize = fontSize,
                    fontWeight = fontWeight,
                    lineHeight = lineHeight,
                    maxLines = maxLines,
                    overflow = overflow
                )
            }
        }
    }
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
