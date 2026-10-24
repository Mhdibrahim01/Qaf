package com.example

import com.example.data.importer.Sm2Algorithm
import com.example.data.importer.Sm2Rating
import com.example.data.local.WordProgressEntity
import com.example.data.plan.TestTimeProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

/**
 * Phase 2 dedicated unit tests for [Sm2Algorithm] with an injectable clock.
 *
 * Verifies:
 * 1. All 4 ratings (Again, Hard, Good, Easy) across repetitions = 0, 1, 2, 5.
 * 2. Ease factor floor (1.30f) and approved upper cap (3.00f).
 * 3. Exact consistency between [Sm2Algorithm.previewIntervals] and [Sm2Algorithm.calculateNextReview]
 *    (including "10m" for Again, "4h" for first-time Hard, and "${round(interval * 1.2)}d" for subsequent Hard).
 * 4. First-learned timestamp preservation across subsequent reviews and Again lapses.
 */
class Sm2AlgorithmTest {

    private val baseClock = TestTimeProvider(fixedTimeMillis = 1_730_548_800_000L) // 2024-11-02 12:00:00 UTC

    @Test
    fun `again rating resets repetitions to 0, schedules in 10 minutes, and reduces ease with 1_30 floor`() {
        val t0 = baseClock.currentTimeMillis()
        val repsToTest = listOf(0, 1, 2, 5)

        for (reps in repsToTest) {
            val result = Sm2Algorithm.calculateNextReview(
                currentReps = reps,
                currentInterval = if (reps == 0) 0 else reps * 3,
                currentEf = 2.5f,
                rating = Sm2Rating.AGAIN,
                nowMillis = t0
            )
            assertEquals("Repetitions should reset to 0 for reps=$reps", 0, result.repetitions)
            assertEquals("Interval days should reset to 0 for reps=$reps", 0, result.intervalDays)
            assertEquals(2.3f, result.easeFactor, 0.0001f)
            assertEquals(t0 + Sm2Algorithm.TEN_MINUTES_MILLIS, result.dueTimestamp)
        }

        // Verify ease floor at 1.30f after repeated Again ratings
        var ef = 1.45f
        repeat(5) {
            val step = Sm2Algorithm.calculateNextReview(
                currentReps = 0,
                currentInterval = 0,
                currentEf = ef,
                rating = Sm2Rating.AGAIN,
                nowMillis = t0
            )
            ef = step.easeFactor
        }
        assertEquals(Sm2Algorithm.MIN_EASE_FACTOR, ef, 0.0001f)
    }

    @Test
    fun `hard rating on first review waits 4 hours and on subsequent reviews multiplies interval by 1_2`() {
        val t0 = baseClock.currentTimeMillis()

        // First review (reps = 0) -> 4h delay, reps = 0, interval = 0, ease = 2.35
        val firstHard = Sm2Algorithm.calculateNextReview(
            currentReps = 0,
            currentInterval = 0,
            currentEf = 2.5f,
            rating = Sm2Rating.HARD,
            nowMillis = t0
        )
        assertEquals(0, firstHard.repetitions)
        assertEquals(0, firstHard.intervalDays)
        assertEquals(2.35f, firstHard.easeFactor, 0.0001f)
        assertEquals(t0 + Sm2Algorithm.FOUR_HOURS_MILLIS, firstHard.dueTimestamp)

        // Subsequent reviews (reps = 1, 2, 5)
        val cases = listOf(
            Triple(1, 1, 1),   // round(1 * 1.2) = 1d
            Triple(2, 6, 7),   // round(6 * 1.2) = 7d
            Triple(5, 20, 24)  // round(20 * 1.2) = 24d
        )
        for ((reps, interval, expectedDays) in cases) {
            val res = Sm2Algorithm.calculateNextReview(
                currentReps = reps,
                currentInterval = interval,
                currentEf = 2.5f,
                rating = Sm2Rating.HARD,
                nowMillis = t0
            )
            assertEquals("Hard at reps=$reps should keep repetitions=$reps", reps, res.repetitions)
            assertEquals("Hard at interval=$interval should produce $expectedDays days", expectedDays, res.intervalDays)
            assertEquals(2.35f, res.easeFactor, 0.0001f)
            assertEquals(t0 + expectedDays * Sm2Algorithm.DAY_MILLIS, res.dueTimestamp)
        }
    }

    @Test
    fun `good rating follows 1d then 6d then round of interval times easeFactor`() {
        var now = baseClock.currentTimeMillis()
        var reps = 0
        var interval = 0
        var ef = 2.5f

        // Review 1 (reps = 0 -> 1, interval = 1d)
        val r1 = Sm2Algorithm.calculateNextReview(reps, interval, ef, Sm2Rating.GOOD, now)
        assertEquals(1, r1.repetitions)
        assertEquals(1, r1.intervalDays)
        assertEquals(2.5f, r1.easeFactor, 0.0001f)
        assertEquals(now + 1 * Sm2Algorithm.DAY_MILLIS, r1.dueTimestamp)

        // Review 2 (reps = 1 -> 2, interval = 6d)
        now = r1.dueTimestamp
        reps = r1.repetitions
        interval = r1.intervalDays
        ef = r1.easeFactor
        val r2 = Sm2Algorithm.calculateNextReview(reps, interval, ef, Sm2Rating.GOOD, now)
        assertEquals(2, r2.repetitions)
        assertEquals(6, r2.intervalDays)
        assertEquals(2.5f, r2.easeFactor, 0.0001f)
        assertEquals(now + 6 * Sm2Algorithm.DAY_MILLIS, r2.dueTimestamp)

        // Review 3 (reps = 2 -> 3, interval = round(6 * 2.5) = 15d)
        now = r2.dueTimestamp
        val r3 = Sm2Algorithm.calculateNextReview(r2.repetitions, r2.intervalDays, r2.easeFactor, Sm2Rating.GOOD, now)
        assertEquals(3, r3.repetitions)
        assertEquals(15, r3.intervalDays)

        // Review at reps = 5 (interval = 35d -> round(35 * 2.5) = 88d)
        val r5 = Sm2Algorithm.calculateNextReview(5, 35, 2.5f, Sm2Rating.GOOD, now)
        assertEquals(6, r5.repetitions)
        assertEquals((35 * 2.5f).roundToInt(), r5.intervalDays)
    }

    @Test
    fun `easy rating follows 4d then 7d then interval times ease times 1_3 and caps ease factor at 3_0f`() {
        val t0 = baseClock.currentTimeMillis()

        // reps = 0 -> 4d, ef = 2.65
        val r1 = Sm2Algorithm.calculateNextReview(0, 0, 2.5f, Sm2Rating.EASY, t0)
        assertEquals(1, r1.repetitions)
        assertEquals(4, r1.intervalDays)
        assertEquals(2.65f, r1.easeFactor, 0.0001f)

        // reps = 1 -> 7d, ef = 2.80
        val r2 = Sm2Algorithm.calculateNextReview(r1.repetitions, r1.intervalDays, r1.easeFactor, Sm2Rating.EASY, t0)
        assertEquals(2, r2.repetitions)
        assertEquals(7, r2.intervalDays)
        assertEquals(2.80f, r2.easeFactor, 0.0001f)

        // reps = 2 -> round(7 * 2.80 * 1.3) = 25d, ef = 2.95
        val r3 = Sm2Algorithm.calculateNextReview(r2.repetitions, r2.intervalDays, r2.easeFactor, Sm2Rating.EASY, t0)
        assertEquals(3, r3.repetitions)
        assertEquals((7 * 2.80f * 1.3f).roundToInt(), r3.intervalDays)
        assertEquals(2.95f, r3.easeFactor, 0.0001f)

        // reps = 5 with repeated Easy ratings -> ease factor strictly capped at 3.0f
        var ef = r3.easeFactor
        var reps = r3.repetitions
        var interval = r3.intervalDays
        repeat(6) {
            val step = Sm2Algorithm.calculateNextReview(reps, interval, ef, Sm2Rating.EASY, t0)
            reps = step.repetitions
            interval = step.intervalDays
            ef = step.easeFactor
            assertTrue("Ease factor ($ef) must never exceed 3.0f", ef <= Sm2Algorithm.MAX_EASE_FACTOR)
        }
        assertEquals(Sm2Algorithm.MAX_EASE_FACTOR, ef, 0.0001f)
    }

    @Test
    fun `previewIntervals labels match real calculateNextReview intervals across all states`() {
        val t0 = baseClock.currentTimeMillis()

        // 1. Brand-new card (reps = 0, interval = 0, ef = 2.5f)
        val previewNew = Sm2Algorithm.previewIntervals(0, 0, 2.5f)
        assertEquals("10m", previewNew.againLabel)
        assertEquals("4h", previewNew.hardLabel)
        assertEquals("1d", previewNew.goodLabel)
        assertEquals("4d", previewNew.easyLabel)

        // 2. Card after 1st Good review (reps = 1, interval = 1d, ef = 2.5f)
        val preview1 = Sm2Algorithm.previewIntervals(1, 1, 2.5f)
        assertEquals("10m", preview1.againLabel)
        assertEquals("1d", preview1.hardLabel)
        assertEquals("6d", preview1.goodLabel)
        assertEquals("7d", preview1.easyLabel)

        // 3. Card at reps = 2, interval = 6d, ef = 2.5f -> Hard is round(6 * 1.2) = 7d (not "1d"!)
        val preview2 = Sm2Algorithm.previewIntervals(2, 6, 2.5f)
        val actualHard2 = Sm2Algorithm.calculateNextReview(2, 6, 2.5f, Sm2Rating.HARD, t0)
        val actualGood2 = Sm2Algorithm.calculateNextReview(2, 6, 2.5f, Sm2Rating.GOOD, t0)
        val actualEasy2 = Sm2Algorithm.calculateNextReview(2, 6, 2.5f, Sm2Rating.EASY, t0)

        assertEquals("${actualHard2.intervalDays}d", preview2.hardLabel)
        assertEquals("7d", preview2.hardLabel)
        assertEquals("${actualGood2.intervalDays}d", preview2.goodLabel)
        assertEquals("15d", preview2.goodLabel)
        assertEquals("${actualEasy2.intervalDays}d", preview2.easyLabel)

        // 4. Card at reps = 5, interval = 20d, ef = 2.5f -> Hard is 24d
        val preview5 = Sm2Algorithm.previewIntervals(5, 20, 2.5f)
        assertEquals("24d", preview5.hardLabel)
        assertEquals("50d", preview5.goodLabel)
    }

    @Test
    fun `applyRating sets firstLearnedTimestamp on first rating and preserves it across reviews and lapses`() {
        val clock = TestTimeProvider(fixedTimeMillis = 1_730_548_800_000L)

        // First rating (new word) at t0
        val t0 = clock.currentTimeMillis()
        val initial = WordProgressEntity(
            cardKey = "oxford5000:resilient",
            deck = "oxford5000",
            text = "resilient"
        )
        val firstRated = Sm2Algorithm.applyRating(
            progress = initial,
            rating = Sm2Rating.GOOD,
            nowMillis = t0
        )
        assertEquals(t0, firstRated.firstLearnedTimestamp)
        assertEquals(t0, firstRated.lastReviewedTimestamp)
        assertEquals(1, firstRated.repetitions)

        // Advance clock by 1 day and rate AGAIN (lapse)
        clock.advanceDays(1)
        val t1 = clock.currentTimeMillis()
        val lapsed = Sm2Algorithm.applyRating(
            progress = firstRated,
            rating = Sm2Rating.AGAIN,
            nowMillis = t1
        )
        assertEquals("firstLearnedTimestamp must never be overwritten on lapse", t0, lapsed.firstLearnedTimestamp)
        assertEquals(t1, lapsed.lastReviewedTimestamp)
        assertEquals(0, lapsed.repetitions)
        assertEquals(1, lapsed.againCount)
    }
}
