package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ArabicTypography
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.TextPrimary

// Single Source of Truth Theme Tokens for Universal BiDi Formatter
val BiDiGoldInlineText = Color(0xFFF5C542)       // Bright gold text
val BiDiGoldInlineBg = Color(0x1FF5C542)         // 12% subtle soft gold tint
val BiDiGoldPillBorder = Color(0x59F5C542)       // 35% gold border
val BiDiGoldPillBg = Color(0x24F5C542)           // 14% gold pill background

val BiDiRedInlineText = Color(0xFFFCA5A5)        // Bright coral/red text
val BiDiRedInlineBg = Color(0x1CFCA5A5)          // 11% subtle soft red tint
val BiDiRedPillBorder = Color(0x4DF87171)        // 30% red border
val BiDiRedPillBg = Color(0x1FF87171)            // 12% red pill background
val BiDiRedPillText = Color(0xFFF87171)

val BiDiExampleBoxBg = Color(0x59000000)         // rgba(0, 0, 0, 0.35)

private const val TRAILING_PUNCT = ".,،؛؟:!?"

/**
 * Data holder representing a parsed grammar explanation line.
 * [mainText] contains the Arabic explanation with inline English terms.
 * [extractedExample] contains any trailing example sentence/clause extracted into a dedicated LTR box.
 */
data class BiDiParsedLine(
    val mainText: String,
    val extractedExample: String? = null
)

private val EXCLUDE_PREFIX_TRAIL = listOf(
    "قول", "والصواب", "وليس", "بدلاً من", "عوضاً عن", "مثل", "لكن", "تكتب", "نقول"
)

private val EXPLICIT_EXAMPLE_REGEX = Regex(
    """(?:[\s.]+(?:مثال|مثل|مثلاً)\s*:\s*([A-Za-z0-9][^\u0600-\u06FF]+)|[\s.]*\((?:مثال|مثل|مثلاً)\s*:\s*([A-Za-z0-9][^)]*)\))\s*\.?$"""
)

private val TRAILING_PAREN_EXAMPLE_REGEX = Regex(
    """[\s.]*\(([A-Za-z0-9][A-Za-z0-9\s,'\"!?+\-/=]+)\)\s*\.?$"""
)

/**
 * Detects whether a rule or note string contains an explicit or parenthetical trailing English example
 * (such as `مثال: There is a book on the table.` or `.(There is not / There are not)` or `.(a book, a car)`)
 * and splits it cleanly from the Arabic paragraph.
 */
fun extractTrailingExample(rawLine: String): BiDiParsedLine {
    val trimmed = rawLine.trim()
    if (trimmed.isEmpty()) return BiDiParsedLine(trimmed, null)

    // Formula boxes, standalone subheaders, and pure English lines are not trailing examples
    if (trimmed.startsWith("التركيب") || trimmed.startsWith("الصيغة") || trimmed.startsWith("القاعدة:") ||
        trimmed.startsWith("ملاحظات") || trimmed.startsWith("أخطاء") || !hasArabicCharacters(trimmed)
    ) {
        return BiDiParsedLine(trimmed, null)
    }

    // 1. Check explicit example indicator: مثال: English... or (مثال: English...)
    val explicitMatch = EXPLICIT_EXAMPLE_REGEX.find(trimmed)
    if (explicitMatch != null) {
        val captured = (explicitMatch.groupValues[1].ifEmpty { explicitMatch.groupValues[2] }).trim()
        val prefix = trimmed.substring(0, explicitMatch.range.first).trimEnd('.', ' ', ':', '،').trim()
        val cleanEx = captured.trimEnd('.', ' ')
        if (cleanEx.isNotEmpty() && hasLatinCharacters(cleanEx)) {
            return BiDiParsedLine(prefix, cleanEx)
        }
    }

    // 2. Check trailing parenthetical English phrase: .(There is not / There are not) or (a book, a car)
    val parenMatch = TRAILING_PAREN_EXAMPLE_REGEX.find(trimmed)
    if (parenMatch != null) {
        val candidate = parenMatch.groupValues[1].trim()
        val hasMultiWordsOrSeparators = candidate.contains(' ') || candidate.contains('/') || candidate.contains(',')
        val prefix = trimmed.substring(0, parenMatch.range.first).trimEnd('.', ' ', ':', '،').trim()
        val isExcludedPrefix = EXCLUDE_PREFIX_TRAIL.any { prefix.endsWith(it) }

        if (hasMultiWordsOrSeparators && !isExcludedPrefix && hasLatinCharacters(candidate)) {
            return BiDiParsedLine(prefix, candidate.trimEnd('.', ' '))
        }
    }

    return BiDiParsedLine(trimmed, null)
}

/**
 * Regex matching English words/phrases inside Arabic explanations:
 * 1) Parenthesized English: (There is) or (a, e, i, o, u)
 * 2) Quoted English: 'a', 'an hour', "There is", ‘is’, “are”
 * 3) Bare Latin terms/phrases connected with slashes or commas: A / An, a, e, i, o, u, I have a pen, not, is, are
 */
private val INLINE_EN_REGEX = Regex(
    """(?:\(([A-Za-z0-9][A-Za-z0-9\s,'\"!?+\-/=]*)\)|['"‘“]([A-Za-z0-9][A-Za-z0-9\s,'\"!?+\-/=]*)['"’”]|(?<![A-Za-z0-9_])([A-Za-z][A-Za-z0-9\'/]*(?:[\s,/+=-]+[A-Za-z0-9][A-Za-z0-9\'/]*)*)(?![A-Za-z0-9_]))"""
)

/**
 * Builds an [AnnotatedString] that styles inline English words/phrases using pure typography:
 * - Unifies slash/comma-separated English terms (e.g. `A / An`, `a, e, i, o, u`) as single LTR tokens.
 * - Replaces regular spaces with non-breaking spaces (\u00A0) inside short phrases so they never break across lines.
 * - Excludes trailing sentence dots (.) so closing periods stay part of the RTL Arabic text.
 * - Applies clean [SpanStyle] with bright gold/coral text without tall clashing boxes.
 */
fun buildBiDiPillAnnotatedString(
    text: String,
    isMistake: Boolean = false
): AnnotatedString {
    if (!hasLatinCharacters(text) || !hasArabicCharacters(text)) {
        return buildAnnotatedString { append(text) }
    }

    val textColor = if (isMistake) BiDiRedInlineText else BiDiGoldInlineText
    val bgColor = if (isMistake) BiDiRedInlineBg else BiDiGoldInlineBg

    return buildAnnotatedString {
        var lastIndex = 0
        val matches = INLINE_EN_REGEX.findAll(text).toList()

        for (match in matches) {
            val range = match.range
            if (range.first > lastIndex) {
                append(text.substring(lastIndex, range.first))
            }

            // Extract the core English term (from group 1, 2, or 3)
            val rawTerm = match.groupValues[1].ifEmpty {
                match.groupValues[2].ifEmpty {
                    match.groupValues[3]
                }
            }.trim()

            if (rawTerm.isNotEmpty() && hasLatinCharacters(rawTerm)) {
                // Strip trailing punctuation from the English token so periods stay in Arabic sentence
                var cleanTerm = rawTerm
                var trailingPunct = ""
                while (cleanTerm.isNotEmpty() && cleanTerm.last() in TRAILING_PUNCT) {
                    trailingPunct = cleanTerm.last() + trailingPunct
                    cleanTerm = cleanTerm.dropLast(1).trimEnd()
                }

                if (cleanTerm.isNotEmpty()) {
                    // Replace spaces inside English phrase with non-breaking spaces (NBSP)
                    // so phrases like 'I have a pen' or 'a, e, i, o, u' never split across lines
                    val nonBreakingTerm = cleanTerm.replace(' ', NBSP)

                    withStyle(
                        SpanStyle(
                            color = textColor,
                            background = bgColor,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                    ) {
                        append("$LRI$LRM$nonBreakingTerm$LRM$PDI")
                    }
                }

                if (trailingPunct.isNotEmpty()) {
                    append(trailingPunct)
                }
            } else {
                append(match.value)
            }

            lastIndex = range.last + 1
        }

        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
}

/**
 * Universal Composable for Arabic Grammar Explanations:
 * 1. Enforces [LayoutDirection.Rtl] on the container.
 * 2. Formats inline English words, parenthesized phrases, and quoted characters cleanly without tall boxes.
 * 3. Auto-extracts trailing examples into a dedicated [LayoutDirection.Ltr] box with a 3dp solid gold border.
 */
@Composable
fun BiDiParsedText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = ArabicTypography.bodyLarge,
    color: Color = TextPrimary,
    isMistakeTheme: Boolean = false,
    lineHeight: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE
) {
    if (text.isEmpty()) return

    val parsed = remember(text) { extractTrailingExample(text) }
    val annotated = remember(parsed.mainText, isMistakeTheme) {
        buildBiDiPillAnnotatedString(parsed.mainText, isMistake = isMistakeTheme)
    }

    val resolvedLineHeight = if (lineHeight != TextUnit.Unspecified) lineHeight else style.lineHeight * 1.35f

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Enforce RTL Container Direction for the Arabic paragraph
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Text(
                text = annotated,
                modifier = Modifier.fillMaxWidth(),
                style = style.copy(
                    textDirection = TextDirection.Rtl,
                    textAlign = TextAlign.Right
                ),
                color = color,
                lineHeight = resolvedLineHeight,
                maxLines = maxLines
            )
        }

        // Dedicated LTR Example Box if a trailing example was extracted
        parsed.extractedExample?.let { ex ->
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(BiDiExampleBoxBg)
                        .drawBehind {
                            val strokeWidth = 3.dp.toPx()
                            drawLine(
                                color = if (isMistakeTheme) BiDiRedInlineText else GoldAccent,
                                start = Offset(strokeWidth / 2f, 0f),
                                end = Offset(strokeWidth / 2f, size.height),
                                strokeWidth = strokeWidth
                            )
                        }
                        .padding(start = 12.dp, end = 10.dp, top = 8.dp, bottom = 8.dp)
                ) {
                    Text(
                        text = ex,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = InterFontFamily,
                            textDirection = TextDirection.Ltr,
                            textAlign = TextAlign.Left
                        ),
                        color = Color(0xFFE5E7EB),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
