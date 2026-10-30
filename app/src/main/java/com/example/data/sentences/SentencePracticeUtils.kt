package com.example.data.sentences

import kotlin.math.max
import kotlin.math.roundToInt

enum class SentenceSortOrder(val labelEn: String, val labelAr: String) {
    NEWEST("Newest First", "الأحدث أولاً"),
    OLDEST("Oldest First", "الأقدم أولاً")
}

enum class DiffTokenStatus {
    CORRECT,
    TYPO,
    WRONG,
    MISSING,
    EXTRA
}

data class CharDiffSpan(
    val char: Char,
    val isMatch: Boolean
)

data class ScrambledWordTile(
    val index: Int,
    val text: String
)

data class DiffToken(
    val text: String,
    val expectedText: String = "",
    val status: DiffTokenStatus,
    val charDiffs: List<CharDiffSpan> = emptyList()
)

data class DictationDiffResult(
    val expectedSentence: String,
    val userSentence: String,
    val tokens: List<DiffToken>,
    val accuracyPercent: Int,
    val mistakesCount: Int,
    val isExactMatch: Boolean
)

object SentencePracticeUtils {

    private val PUNCTUATION_REGEX = Regex("[^\\p{L}\\p{N}'’]+")

    private fun normalizeWord(word: String, ignorePunctuation: Boolean): String {
        val trimmed = word.trim().replace('’', '\'')
        return if (ignorePunctuation) {
            trimmed.replace(PUNCTUATION_REGEX, "").lowercase()
        } else {
            trimmed.lowercase()
        }
    }

    private fun tokenizeWords(sentence: String): List<String> {
        return sentence
            .trim()
            .split(Regex("\\s+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    /**
     * Builds a deterministic list of scrambled word tiles for Scrambled Word Tiles mode.
     */
    fun buildScrambledWordTiles(
        sentence: String,
        seed: Int = sentence.hashCode()
    ): List<ScrambledWordTile> {
        val rawWords = tokenizeWords(sentence)
        if (rawWords.size <= 1) {
            return rawWords.mapIndexed { idx, w -> ScrambledWordTile(idx, w) }
        }
        val indexed = rawWords.mapIndexed { idx, w -> ScrambledWordTile(idx, w) }
        val shuffled = indexed.shuffled(kotlin.random.Random(seed))
        return if (shuffled.map { it.text } == rawWords) {
            indexed.reversed()
        } else {
            shuffled
        }
    }

    fun computeCharEditDistance(a: String, b: String): Int {
        val n = a.length
        val m = b.length
        if (n == 0) return m
        if (m == 0) return n
        val dp = Array(n + 1) { IntArray(m + 1) }
        for (i in 0..n) dp[i][0] = i
        for (j in 0..m) dp[0][j] = j
        for (i in 1..n) {
            for (j in 1..m) {
                val cost = if (a[i - 1].lowercaseChar() == b[j - 1].lowercaseChar()) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j - 1] + cost,
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1
                )
            }
        }
        return dp[n][m]
    }

    fun computeCharDiffSpans(actual: String, expected: String): List<CharDiffSpan> {
        val n = actual.length
        val m = expected.length
        if (n == 0) return emptyList()
        val dp = Array(n + 1) { IntArray(m + 1) }
        for (i in 0..n) dp[i][0] = i
        for (j in 0..m) dp[0][j] = j
        for (i in 1..n) {
            for (j in 1..m) {
                val cost = if (actual[i - 1].lowercaseChar() == expected[j - 1].lowercaseChar()) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j - 1] + cost,
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1
                )
            }
        }
        val reversed = ArrayList<CharDiffSpan>(n)
        var i = n
        var j = m
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0) {
                val isSame = actual[i - 1].lowercaseChar() == expected[j - 1].lowercaseChar()
                if (isSame && dp[i][j] == dp[i - 1][j - 1]) {
                    reversed.add(CharDiffSpan(actual[i - 1], true))
                    i--
                    j--
                    continue
                }
                if (dp[i][j] == dp[i - 1][j - 1] + 1) {
                    reversed.add(CharDiffSpan(actual[i - 1], false))
                    i--
                    j--
                    continue
                }
            }
            if (i > 0 && (j == 0 || dp[i][j] == dp[i - 1][j] + 1)) {
                reversed.add(CharDiffSpan(actual[i - 1], false))
                i--
            } else if (j > 0) {
                j--
            }
        }
        return reversed.reversed()
    }

    /**
     * Computes a word-by-word diff between [expected] and [actual] (used by both Offline Dictation
     * and Android SpeechRecognizer Speaking comparison).
     */
    fun computeSentenceDiff(
        expected: String,
        actual: String,
        ignorePunctuation: Boolean = true
    ): DictationDiffResult {
        val expectedRawWords = tokenizeWords(expected)
        val actualRawWords = tokenizeWords(actual)

        val expectedPairs = expectedRawWords.mapNotNull { raw ->
            val norm = normalizeWord(raw, ignorePunctuation)
            if (norm.isEmpty()) null else raw to norm
        }
        val actualPairs = actualRawWords.mapNotNull { raw ->
            val norm = normalizeWord(raw, ignorePunctuation)
            if (norm.isEmpty()) null else raw to norm
        }

        if (expectedPairs.isEmpty()) {
            return DictationDiffResult(
                expectedSentence = expected,
                userSentence = actual,
                tokens = emptyList(),
                accuracyPercent = if (actualPairs.isEmpty()) 100 else 0,
                mistakesCount = actualPairs.size,
                isExactMatch = actualPairs.isEmpty()
            )
        }

        val n = expectedPairs.size
        val m = actualPairs.size

        // DP matrix for minimum edit distance alignment at word level
        val dp = Array(n + 1) { IntArray(m + 1) }
        for (i in 0..n) dp[i][0] = i
        for (j in 0..m) dp[0][j] = j

        for (i in 1..n) {
            for (j in 1..m) {
                val cost = if (expectedPairs[i - 1].second == actualPairs[j - 1].second) 0 else 1
                val sub = dp[i - 1][j - 1] + cost
                val del = dp[i - 1][j] + 1 // missing word from expected
                val ins = dp[i][j - 1] + 1 // extra word in actual
                dp[i][j] = minOf(sub, del, ins)
            }
        }

        // Backtrack to build aligned DiffToken list
        val reversedTokens = ArrayList<DiffToken>(max(n, m) + 2)
        var i = n
        var j = m
        var mistakes = 0
        var correctCount = 0

        while (i > 0 || j > 0) {
            if (i > 0 && j > 0) {
                val expRaw = expectedPairs[i - 1].first
                val expNorm = expectedPairs[i - 1].second
                val actRaw = actualPairs[j - 1].first
                val actNorm = actualPairs[j - 1].second

                if (expNorm == actNorm && dp[i][j] == dp[i - 1][j - 1]) {
                    reversedTokens.add(
                        DiffToken(
                            text = expRaw,
                            expectedText = expRaw,
                            status = DiffTokenStatus.CORRECT
                        )
                    )
                    correctCount++
                    i--
                    j--
                    continue
                }

                if (dp[i][j] == dp[i - 1][j - 1] + 1) {
                    val charDist = computeCharEditDistance(actNorm, expNorm)
                    val maxLen = max(actNorm.length, expNorm.length).coerceAtLeast(1)
                    val similarity = 1.0 - (charDist.toDouble() / maxLen.toDouble())
                    val isMinorTypo = (charDist <= 2 && maxLen >= 3) || similarity >= 0.60
                    reversedTokens.add(
                        DiffToken(
                            text = actRaw,
                            expectedText = expRaw,
                            status = if (isMinorTypo) DiffTokenStatus.TYPO else DiffTokenStatus.WRONG,
                            charDiffs = if (isMinorTypo) computeCharDiffSpans(actRaw, expRaw) else emptyList()
                        )
                    )
                    mistakes++
                    i--
                    j--
                    continue
                }
            }

            if (i > 0 && (j == 0 || dp[i][j] == dp[i - 1][j] + 1)) {
                val expRaw = expectedPairs[i - 1].first
                reversedTokens.add(
                    DiffToken(
                        text = expRaw,
                        expectedText = expRaw,
                        status = DiffTokenStatus.MISSING
                    )
                )
                mistakes++
                i--
            } else if (j > 0) {
                val actRaw = actualPairs[j - 1].first
                reversedTokens.add(
                    DiffToken(
                        text = actRaw,
                        expectedText = "",
                        status = DiffTokenStatus.EXTRA
                    )
                )
                mistakes++
                j--
            }
        }

        val tokens = reversedTokens.reversed()
        val denominator = max(n, correctCount + mistakes)
        val accuracy = if (denominator > 0) {
            ((correctCount.toDouble() / denominator.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
        } else {
            0
        }

        return DictationDiffResult(
            expectedSentence = expected,
            userSentence = actual,
            tokens = tokens,
            accuracyPercent = accuracy,
            mistakesCount = mistakes,
            isExactMatch = mistakes == 0 && correctCount == n
        )
    }
}
