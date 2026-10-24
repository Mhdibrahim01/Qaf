package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ArabicTypography
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.TextPrimary

// Single Source of Truth Theme Tokens for Universal BiDi Formatter
val BiDiGoldPillBg = Color(0x24F5C542)          // rgba(245, 197, 66, 0.14)
val BiDiGoldPillBorder = Color(0x59F5C542)      // rgba(245, 197, 66, 0.35)
val BiDiGoldPillText = Color(0xFFFDE08D)        // Light gold text
val BiDiRedPillBg = Color(0x1FF87171)           // rgba(248, 113, 113, 0.12)
val BiDiRedPillBorder = Color(0x4DF87171)       // rgba(248, 113, 113, 0.30)
val BiDiRedPillText = Color(0xFFF87171)         // Subtle red text
val BiDiExampleBoxBg = Color(0x59000000)        // rgba(0, 0, 0, 0.35)

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
 * 1) Parenthesized English: (There is) or (a book, a car)
 * 2) Quoted English: 'a' or "There is" or ‘is’ or “are”
 * 3) Bare Latin terms/phrases: There is, not, is, are, I, can
 */
private val INLINE_EN_REGEX = Regex(
    """(?:\(([A-Za-z0-9][A-Za-z0-9\s,'\"!?+\-/=]*)\)|['"‘“]([A-Za-z0-9][A-Za-z0-9\s,'\"!?+\-/=]*)['"’”]|(?<![A-Za-z0-9_])([A-Za-z][A-Za-z0-9'/-]*(?:\s+[A-Za-z][A-Za-z0-9'/-]*)*)(?![A-Za-z0-9_]))"""
)

/**
 * Builds an [AnnotatedString] that isolates inline English words/phrases into styled pills
 * with BiDi Unicode isolates (U+2066 ... U+2069) and strong LTR marks (U+200E) to eliminate
 * RTL punctuation and word-order flipping.
 */
fun buildBiDiPillAnnotatedString(
    text: String,
    isMistake: Boolean = false
): AnnotatedString {
    if (!hasLatinCharacters(text) || !hasArabicCharacters(text)) {
        return buildAnnotatedString { append(text) }
    }

    val textColor = if (isMistake) BiDiRedPillText else BiDiGoldPillText
    val bgColor = if (isMistake) BiDiRedPillBg else BiDiGoldPillBg

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
                val startSpan = length

                withStyle(
                    SpanStyle(
                        color = textColor,
                        background = bgColor,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = InterFontFamily
                    )
                ) {
                    // Wrap English segment in Unicode BiDi LTR Isolates
                    append("$LRI$LRM$rawTerm$LRM$PDI")
                }
                val endSpan = length
                addStringAnnotation(tag = "EN_PILL", annotation = rawTerm, start = startSpan, end = endSpan)
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
 * 2. Auto-wraps inline English words, parenthesized phrases, and quoted characters in BiDi-isolated pills.
 * 3. Draws subtle rounded background pills via [Modifier.drawBehind] for high visual polish.
 * 4. Auto-extracts trailing examples into a dedicated [LayoutDirection.Ltr] box with a 3dp solid gold border.
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

    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val pillBgColor = if (isMistakeTheme) BiDiRedPillBg else BiDiGoldPillBg
    val pillBorderColor = if (isMistakeTheme) BiDiRedPillBorder else BiDiGoldPillBorder
    val resolvedLineHeight = if (lineHeight != TextUnit.Unspecified) lineHeight else style.lineHeight * 1.35f

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Enforce RTL Container Direction for the Arabic paragraph
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Text(
                text = annotated,
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        textLayoutResult?.let { layout ->
                            val annotations = annotated.getStringAnnotations("EN_PILL", 0, annotated.length)
                            for (ann in annotations) {
                                val s = ann.start
                                val e = ann.end
                                if (s >= e) continue
                                val startLine = layout.getLineForOffset(s)
                                val endLine = layout.getLineForOffset(e - 1)

                                for (line in startLine..endLine) {
                                    val lineStart = maxOf(s, layout.getLineStart(line))
                                    val lineEnd = minOf(e, layout.getLineEnd(line, visibleEnd = true))
                                    if (lineStart >= lineEnd) continue

                                    val x1 = layout.getHorizontalPosition(lineStart, usePrimaryDirection = true)
                                    val x2 = layout.getHorizontalPosition(lineEnd, usePrimaryDirection = true)
                                    val left = minOf(x1, x2) - 4.dp.toPx()
                                    val right = maxOf(x1, x2) + 4.dp.toPx()
                                    val top = layout.getLineTop(line) + 2.dp.toPx()
                                    val bottom = layout.getLineBottom(line) - 2.dp.toPx()

                                    if (right > left && bottom > top) {
                                        val rectSize = Size(right - left, bottom - top)
                                        val rectTopLeft = Offset(left, top)
                                        val cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())

                                        drawRoundRect(
                                            color = pillBgColor,
                                            topLeft = rectTopLeft,
                                            size = rectSize,
                                            cornerRadius = cornerRadius
                                        )
                                        drawRoundRect(
                                            color = pillBorderColor,
                                            topLeft = rectTopLeft,
                                            size = rectSize,
                                            cornerRadius = cornerRadius,
                                            style = Stroke(width = 1.dp.toPx())
                                        )
                                    }
                                }
                            }
                        }
                    },
                style = style.copy(
                    textDirection = TextDirection.Rtl,
                    textAlign = TextAlign.Right
                ),
                color = color,
                lineHeight = resolvedLineHeight,
                maxLines = maxLines,
                onTextLayout = { textLayoutResult = it }
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
                                color = if (isMistakeTheme) BiDiRedPillText else GoldAccent,
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
