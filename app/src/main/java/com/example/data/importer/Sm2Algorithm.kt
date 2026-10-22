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
    const val TEN_MINUTES_MILLIS = 10 * 60 * 1000L
    const val FOUR_HOURS_MILLIS = 4 * 60 * 60 * 1000L
    const val MIN_EASE_FACTOR = 1.3f
    const val MAX_EASE_FACTOR = 3.0f

    /**
     * Calculates the next review schedule according to Qaf's SuperMemo-2 rules:
     * - AGAIN: resets repetitions to 0, interval to 0 days, decreases EF by 0.20 (min 1.3), due in 10 minutes.
     * - HARD: on first review (reps == 0), keeps reps = 0 and interval = 0 days, due in 4 hours;
     *         on subsequent reviews (reps > 0), keeps reps unchanged and scales interval by max(1, round(interval * 1.2)),
     *         decreases EF by 0.15 (min 1.3).
     * - GOOD: increments repetitions, interval 1d -> 6d -> max(1, round(interval * EF)), EF unchanged (clamped to 1.3..3.0).
     * - EASY: increments repetitions, interval 4d -> 7d -> max(4, round(interval * EF * 1.3)), increases EF by 0.15 (max 3.0).
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
                easeFactor = (currentEf - 0.2f).coerceIn(MIN_EASE_FACTOR, MAX_EASE_FACTOR),
                dueTimestamp = nowMillis + TEN_MINUTES_MILLIS // 10 minutes
            )
            Sm2Rating.HARD -> {
                if (currentReps == 0) {
                    Sm2CalculatedResult(
                        repetitions = 0,
                        intervalDays = 0,
                        easeFactor = (currentEf - 0.15f).coerceIn(MIN_EASE_FACTOR, MAX_EASE_FACTOR),
                        dueTimestamp = nowMillis + FOUR_HOURS_MILLIS // 4 hours
                    )
                } else {
                    val nextInterval = max(1, (currentInterval * 1.2f).roundToInt())
                    Sm2CalculatedResult(
                        repetitions = currentReps,
                        intervalDays = nextInterval,
                        easeFactor = (currentEf - 0.15f).coerceIn(MIN_EASE_FACTOR, MAX_EASE_FACTOR),
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
                    easeFactor = currentEf.coerceIn(MIN_EASE_FACTOR, MAX_EASE_FACTOR),
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
                    easeFactor = (currentEf + 0.15f).coerceIn(MIN_EASE_FACTOR, MAX_EASE_FACTOR),
                    dueTimestamp = nowMillis + nextInterval * DAY_MILLIS
                )
            }
        }
    }

    /**
     * Previews human-readable interval labels for the rating buttons ("10m", "4h" / "Xd", "1d" / "6d" / "Xd", etc.).
     * Matches calculateNextReview exactly.
     */
    fun previewIntervals(
        reps: Int,
        interval: Int,
        ef: Float
    ): Sm2IntervalPreview {
        val hardLabel = if (reps == 0) {
            "4h"
        } else {
            val hardDays = max(1, (interval * 1.2f).roundToInt())
            "${hardDays}d"
        }
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
            againLabel = "10m",
            hardLabel = hardLabel,
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
        val firstLearned = if (progress.firstLearnedTimestamp > 0L) {
            progress.firstLearnedTimestamp
        } else {
            nowMillis
        }
        return progress.copy(
            repetitions = result.repetitions,
            intervalDays = result.intervalDays,
            easeFactor = result.easeFactor,
            dueTimestamp = result.dueTimestamp,
            lastReviewedTimestamp = nowMillis,
            againCount = progress.againCount + againInc,
            firstLearnedTimestamp = firstLearned
        )
    }
}
