package com.example.data.blitz

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.runtime.Immutable
import com.example.data.local.EntryEntity
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random

enum class BlitzQuestionMode {
    TRUE_FALSE,
    FOUR_CHOICE
}

@Immutable
data class BlitzQuestionItem(
    val id: String,
    val word: String,
    val pos: String,
    val level: String,
    val deck: String,
    val isAcademicOrIelts: Boolean,
    val mode: BlitzQuestionMode,
    val promptArabic: String,
    val optionsArabic: List<String>,
    val correctOptionIndex: Int,
    val trueArabicMeaning: String
)

@Immutable
data class VocabularyBlitzStats(
    val highScore: Int = 0,
    val bestStreak: Int = 0,
    val wordSniperUnlocked: Boolean = false,
    val academicLightningUnlocked: Boolean = false
)

@Immutable
data class VocabularyBlitzSessionState(
    val questions: List<BlitzQuestionItem> = emptyList(),
    val currentIndex: Int = 0,
    val remainingSeconds: Int = VocabularyBlitzEngine.BLITZ_DURATION_SECONDS,
    val isRunning: Boolean = true,
    val isFinished: Boolean = false,
    val score: Int = 0,
    val currentStreak: Int = 0,
    val maxComboStreak: Int = 0,
    val academicStreak: Int = 0,
    val maxAcademicStreak: Int = 0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val isNewHighScore: Boolean = false,
    val unlockedWordSniperNow: Boolean = false,
    val unlockedAcademicLightningNow: Boolean = false,
    val lastAnswerCorrect: Boolean? = null
) {
    val currentQuestion: BlitzQuestionItem?
        get() = if (questions.isNotEmpty()) questions[currentIndex % questions.size] else null

    val comboMultiplier: Int
        get() = VocabularyBlitzEngine.computeComboMultiplier(currentStreak)

    val totalAnswered: Int
        get() = correctCount + wrongCount

    val accuracyPercent: Int
        get() = if (totalAnswered > 0) {
            ((correctCount.toDouble() / totalAnswered.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
        } else 0
}

object VocabularyBlitzEngine {

    const val BLITZ_DURATION_SECONDS = 60
    const val BASE_POINTS_PER_CORRECT = 10
    const val WORD_SNIPER_THRESHOLD = 25
    const val ACADEMIC_LIGHTNING_THRESHOLD = 15

    /**
     * Every 5 consecutive correct answers increases the multiplier:
     * 0..4 -> 1x, 5..9 -> 2x, 10..14 -> 3x, 15+ -> 4x
     */
    fun computeComboMultiplier(streak: Int): Int {
        return when {
            streak >= 15 -> 4
            streak >= 10 -> 3
            streak >= 5 -> 2
            else -> 1
        }
    }

    fun isAcademicEntry(entry: EntryEntity): Boolean {
        val deckLower = entry.deck.lowercase()
        val levelUpper = (entry.effectiveLevel ?: entry.level).uppercase().trim()
        return "ielts" in deckLower ||
            "opal" in deckLower ||
            "academic" in deckLower ||
            levelUpper in setOf("B2", "C1", "C2")
    }

    private fun cleanPrimaryArabic(raw: String): String {
        return raw
            .split(Regex("[؛،,/\\n]"))
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() }
            ?: raw.trim()
    }

    /**
     * Generates a 60-question rapid-fire pool mixing True/False and 4-Option Arabic choices
     * from Room [EntryEntity] records.
     */
    fun generateBlitzQuestions(
        entries: List<EntryEntity>,
        count: Int = 60,
        seed: Int = System.currentTimeMillis().toInt()
    ): List<BlitzQuestionItem> {
        val validPool = entries
            .filter { it.text.isNotBlank() && it.arabic.isNotBlank() }
            .distinctBy { it.text.trim().lowercase() }
            .ifEmpty { fallbackEntries() }

        val rng = Random(seed)
        val shuffledPool = validPool.shuffled(rng)
        val allMeanings = validPool
            .map { cleanPrimaryArabic(it.arabic) }
            .filter { it.isNotBlank() }
            .distinct()

        val result = ArrayList<BlitzQuestionItem>(count)
        for (i in 0 until count) {
            val target = shuffledPool[i % shuffledPool.size]
            val trueAr = cleanPrimaryArabic(target.arabic)
            val distractors = allMeanings
                .filter { !it.equals(trueAr, ignoreCase = true) }
                .shuffled(rng)
                .take(3)
                .ifEmpty { listOf("يتجاهل", "يستمر", "يتردد").filter { it != trueAr } }

            val isTrueFalseMode = (i % 2 == 0)
            if (isTrueFalseMode) {
                val showTrueMeaning = rng.nextBoolean()
                val displayedAr = if (showTrueMeaning || distractors.isEmpty()) {
                    trueAr
                } else {
                    distractors.first()
                }
                result.add(
                    BlitzQuestionItem(
                        id = "${target.id}_tf_$i",
                        word = target.text.trim(),
                        pos = target.pos.trim(),
                        level = (target.effectiveLevel ?: target.level).trim().ifBlank { "B2" },
                        deck = target.deck,
                        isAcademicOrIelts = isAcademicEntry(target),
                        mode = BlitzQuestionMode.TRUE_FALSE,
                        promptArabic = displayedAr,
                        optionsArabic = listOf("✓ Correct • صحيح", "✗ False • خاطئ"),
                        correctOptionIndex = if (displayedAr == trueAr) 0 else 1,
                        trueArabicMeaning = trueAr
                    )
                )
            } else {
                val options = (distractors.take(3) + trueAr).distinct().shuffled(rng)
                val finalOptions = if (trueAr in options) options else (options.take(3) + trueAr)
                val correctIdx = finalOptions.indexOf(trueAr).coerceAtLeast(0)
                result.add(
                    BlitzQuestionItem(
                        id = "${target.id}_mc_$i",
                        word = target.text.trim(),
                        pos = target.pos.trim(),
                        level = (target.effectiveLevel ?: target.level).trim().ifBlank { "B2" },
                        deck = target.deck,
                        isAcademicOrIelts = isAcademicEntry(target),
                        mode = BlitzQuestionMode.FOUR_CHOICE,
                        promptArabic = "",
                        optionsArabic = finalOptions,
                        correctOptionIndex = correctIdx,
                        trueArabicMeaning = trueAr
                    )
                )
            }
        }
        return result
    }

    /**
     * Pure state transition for answering the current Blitz question.
     */
    fun submitAnswer(
        state: VocabularyBlitzSessionState,
        selectedOptionIndex: Int,
        previousHighScore: Int = 0
    ): VocabularyBlitzSessionState {
        if (state.isFinished || state.questions.isEmpty()) return state
        val currentQ = state.currentQuestion ?: return state
        val isCorrect = selectedOptionIndex == currentQ.correctOptionIndex

        val newStreak = if (isCorrect) state.currentStreak + 1 else 0
        val newMaxStreak = max(state.maxComboStreak, newStreak)
        val newAcademicStreak = if (isCorrect && currentQ.isAcademicOrIelts) {
            state.academicStreak + 1
        } else if (!isCorrect) {
            0
        } else {
            state.academicStreak
        }
        val newMaxAcademicStreak = max(state.maxAcademicStreak, newAcademicStreak)

        val multiplier = computeComboMultiplier(state.currentStreak)
        val pointsEarned = if (isCorrect) BASE_POINTS_PER_CORRECT * multiplier else 0
        val newScore = state.score + pointsEarned
        val newCorrect = if (isCorrect) state.correctCount + 1 else state.correctCount
        val newWrong = if (isCorrect) state.wrongCount else state.wrongCount + 1

        val unlockedSniper = state.unlockedWordSniperNow || newCorrect >= WORD_SNIPER_THRESHOLD
        val unlockedLightning = state.unlockedAcademicLightningNow ||
            newMaxAcademicStreak >= ACADEMIC_LIGHTNING_THRESHOLD ||
            newMaxStreak >= ACADEMIC_LIGHTNING_THRESHOLD

        return state.copy(
            currentIndex = state.currentIndex + 1,
            score = newScore,
            currentStreak = newStreak,
            maxComboStreak = newMaxStreak,
            academicStreak = newAcademicStreak,
            maxAcademicStreak = newMaxAcademicStreak,
            correctCount = newCorrect,
            wrongCount = newWrong,
            isNewHighScore = newScore > previousHighScore,
            unlockedWordSniperNow = unlockedSniper,
            unlockedAcademicLightningNow = unlockedLightning,
            lastAnswerCorrect = isCorrect
        )
    }

    private fun fallbackEntries(): List<EntryEntity> = listOf(
        EntryEntity(id = "fb_1", type = "word", deck = "ielts_academic", text = "acquire", pos = "v.", level = "B2", effectiveLevel = "B2", definitionEn = "To obtain or gain", noteEn = "", arabic = "يكتسب", examplesEn = "", examplesAr = "", definitionUrl = "", audioUrl = ""),
        EntryEntity(id = "fb_2", type = "word", deck = "ielts_academic", text = "resilient", pos = "adj.", level = "C1", effectiveLevel = "C1", definitionEn = "Able to recover quickly", noteEn = "", arabic = "مرن / صامد", examplesEn = "", examplesAr = "", definitionUrl = "", audioUrl = ""),
        EntryEntity(id = "fb_3", type = "word", deck = "oxford_opal", text = "comprehensive", pos = "adj.", level = "B2", effectiveLevel = "B2", definitionEn = "Complete and including everything", noteEn = "", arabic = "شامل", examplesEn = "", examplesAr = "", definitionUrl = "", audioUrl = ""),
        EntryEntity(id = "fb_4", type = "word", deck = "ielts_academic", text = "inevitable", pos = "adj.", level = "C1", effectiveLevel = "C1", definitionEn = "Certain to happen", noteEn = "", arabic = "حتمي", examplesEn = "", examplesAr = "", definitionUrl = "", audioUrl = ""),
        EntryEntity(id = "fb_5", type = "word", deck = "oxford5000", text = "fluctuate", pos = "v.", level = "C1", effectiveLevel = "C1", definitionEn = "To rise and fall irregularly", noteEn = "", arabic = "يتقلب / يتذبذب", examplesEn = "", examplesAr = "", definitionUrl = "", audioUrl = ""),
        EntryEntity(id = "fb_6", type = "word", deck = "oxford_opal", text = "substantiate", pos = "v.", level = "C1", effectiveLevel = "C1", definitionEn = "Provide evidence to support", noteEn = "", arabic = "يثبت بالدليل", examplesEn = "", examplesAr = "", definitionUrl = "", audioUrl = "")
    )
}

/**
 * Lightweight synthesizer sound effect helper using Android's [ToneGenerator].
 * Safe in headless/Robolectric environments.
 */
object BlitzSoundPlayer {
    fun playCorrectChime(enabled: Boolean = true) {
        if (!enabled) return
        playToneSafe(ToneGenerator.TONE_PROP_ACK, 90)
    }

    fun playComboLevelUpChime(enabled: Boolean = true) {
        if (!enabled) return
        playToneSafe(ToneGenerator.TONE_PROP_BEEP2, 140)
    }

    fun playWrongBuzz(enabled: Boolean = true) {
        if (!enabled) return
        playToneSafe(ToneGenerator.TONE_PROP_NACK, 120)
    }

    fun playCountdownTick(enabled: Boolean = true) {
        if (!enabled) return
        playToneSafe(ToneGenerator.TONE_PROP_BEEP, 45)
    }

    private fun playToneSafe(toneType: Int, durationMs: Int) {
        runCatching {
            val generator = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
            generator.startTone(toneType, durationMs)
        }
    }
}
