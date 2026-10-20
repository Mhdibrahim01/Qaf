package com.example.data.importer

import kotlin.math.max
import kotlin.math.roundToInt

data class Sm2CalculatedResult(
    val repetitions: Int,
    val intervalDays: Int,
    val easeFactor: Float,
    val dueTimestamp: Long
)

/**
 * Pure, centralized mathematical engine for SM-2 Spaced Repetition calculations.
 * Used consistently across Flashcards study, progress tracking, and interval previews.
 */
object Sm2Algorithm {
    const val DAY_MILLIS = 24 * 60 * 60 * 1000L

    /**
     * Calculates the next review schedule according to the SuperMemo-2 algorithm:
     * - AGAIN: resets repetitions to 0, interval to 0 days, decreases EF by 0.2 (min 1.3).
     * - HARD: maintains repetitions, interval 1 day, decreases EF by 0.15 (min 1.3).
     * - GOOD: increments repetitions, scales interval by EF (1d -> 6d -> interval * EF).
     * - EASY: increments repetitions, scales interval by EF * 1.3, increases EF by 0.15.
     */
    fun calculateNextReview(
        currentReps: Int,
        currentInterval: Int,
        currentEf: Float,
        rating: Sm2Rating,
        nowMillis: Long = System.currentTimeMillis()
    ): Sm2CalculatedResult {
        return when (rating) {
            Sm2Rating.AGAIN -> Sm2CalculatedResult(
                repetitions = 0,
                intervalDays = 0,
                easeFactor = (currentEf - 0.2f).coerceAtLeast(1.3f),
                dueTimestamp = nowMillis + 10 * 60 * 1000L // 10 minutes
            )
            Sm2Rating.HARD -> {
                if (currentReps == 0) {
                    Sm2CalculatedResult(
                        repetitions = 0,
                        intervalDays = 0,
                        easeFactor = (currentEf - 0.15f).coerceAtLeast(1.3f),
                        dueTimestamp = nowMillis + 4 * 60 * 60 * 1000L // 4 hours (due today)
                    )
                } else {
                    val nextInterval = max(1, (currentInterval * 1.2f).roundToInt())
                    Sm2CalculatedResult(
                        repetitions = currentReps,
                        intervalDays = nextInterval,
                        easeFactor = (currentEf - 0.15f).coerceAtLeast(1.3f),
                        dueTimestamp = nowMillis + nextInterval * DAY_MILLIS
                    )
                }
            }
            Sm2Rating.GOOD -> {
                val nextInterval = when (currentReps) {
                    0 -> 1
                    1 -> 6
                    else -> max(1, (currentInterval * currentEf).roundToInt())
                }
                Sm2CalculatedResult(
                    repetitions = currentReps + 1,
                    intervalDays = nextInterval,
                    easeFactor = currentEf,
                    dueTimestamp = nowMillis + nextInterval * DAY_MILLIS
                )
            }
            Sm2Rating.EASY -> {
                val nextInterval = when (currentReps) {
                    0 -> 4
                    1 -> 7
                    else -> max(4, (currentInterval * currentEf * 1.3f).roundToInt())
                }
                Sm2CalculatedResult(
                    repetitions = currentReps + 1,
                    intervalDays = nextInterval,
                    easeFactor = (currentEf + 0.15f).coerceAtLeast(1.3f),
                    dueTimestamp = nowMillis + nextInterval * DAY_MILLIS
                )
            }
        }
    }

    /**
     * Previews human-readable interval labels for the rating buttons ("<10m", "1d", "6d", etc.).
     */
    fun previewIntervals(
        reps: Int,
        interval: Int,
        ef: Float
    ): Sm2IntervalPreview {
        val hardDays = if (reps == 0) 0 else 1
        val goodDays = when (reps) {
            0 -> 1
            1 -> 6
            else -> max(1, (interval * ef).roundToInt())
        }
        val easyDays = when (reps) {
            0 -> 4
            1 -> 7
            else -> max(4, (interval * ef * 1.3f).roundToInt())
        }

        return Sm2IntervalPreview(
            againLabel = "<10m",
            hardLabel = if (hardDays == 0) "Today" else "${hardDays}d",
            goodLabel = "${goodDays}d",
            easyLabel = "${easyDays}d"
        )
    }

    fun applyRating(
        progress: com.example.data.local.WordProgressEntity,
        rating: Sm2Rating,
        nowMillis: Long = System.currentTimeMillis()
    ): com.example.data.local.WordProgressEntity {
        val result = calculateNextReview(
            currentReps = progress.repetitions,
            currentInterval = progress.intervalDays,
            currentEf = progress.easeFactor,
            rating = rating,
            nowMillis = nowMillis
        )
        val againInc = if (rating == Sm2Rating.AGAIN) 1 else 0
        return progress.copy(
            repetitions = result.repetitions,
            intervalDays = result.intervalDays,
            easeFactor = result.easeFactor,
            dueTimestamp = result.dueTimestamp,
            lastReviewedTimestamp = nowMillis,
            againCount = progress.againCount + againInc
        )
    }
}
