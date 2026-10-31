package com.example.data.blitz

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.ToneGenerator
import androidx.compose.runtime.Immutable
import com.example.data.local.EntryEntity
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
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
    val ipa: String = "",
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
data class BlitzFilterConfig(
    val selectedDecks: List<String> = emptyList(),
    val allBooksSelected: Boolean = true,
    val selectedLevels: Set<String> = setOf("A1", "A2", "B1", "B2", "C1"),
    val studiedOnly: Boolean = false
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
    val lastAnswerCorrect: Boolean? = null,
    val missedQuestions: List<BlitzQuestionItem> = emptyList()
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

@Immutable
data class SurvivalSessionState(
    val questions: List<BlitzQuestionItem> = emptyList(),
    val currentIndex: Int = 0,
    val hearts: Int = 3,
    val maxHearts: Int = 3,
    val currentStreak: Int = 0,
    val bestStreakInSession: Int = 0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val isFinished: Boolean = false,
    val missedQuestions: List<BlitzQuestionItem> = emptyList(),
    val heartRecoveredThisTurn: Boolean = false,
    val lastAnswerCorrect: Boolean? = null,
    val isNewBestStreak: Boolean = false
) {
    val currentQuestion: BlitzQuestionItem?
        get() = if (questions.isNotEmpty()) questions[currentIndex % questions.size] else null

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
                        ipa = target.ipa.trim(),
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
                        ipa = target.ipa.trim(),
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
        val newMissed = if (isCorrect) state.missedQuestions else (state.missedQuestions + currentQ)

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
            lastAnswerCorrect = isCorrect,
            missedQuestions = newMissed
        )
    }

    /**
     * Pure state transition for Survival Mode (3 Hearts).
     * Correct -> streak increases, restores +1 heart on every 15-streak milestone up to maxHearts.
     * Wrong -> loses 1 heart, resets streak, records mistake, finishes if hearts == 0.
     */
    fun submitSurvivalAnswer(
        state: SurvivalSessionState,
        selectedOptionIndex: Int,
        previousBestStreak: Int = 0
    ): SurvivalSessionState {
        if (state.isFinished || state.questions.isEmpty()) return state
        val currentQ = state.currentQuestion ?: return state
        val isCorrect = selectedOptionIndex == currentQ.correctOptionIndex

        return if (isCorrect) {
            val newStreak = state.currentStreak + 1
            val newBestStreak = max(state.bestStreakInSession, newStreak)
            val shouldRecoverHeart = (newStreak > 0 && newStreak % 15 == 0 && state.hearts < state.maxHearts)
            val newHearts = if (shouldRecoverHeart) state.hearts + 1 else state.hearts
            val isNewRecord = newBestStreak > previousBestStreak

            state.copy(
                currentIndex = state.currentIndex + 1,
                currentStreak = newStreak,
                bestStreakInSession = newBestStreak,
                correctCount = state.correctCount + 1,
                hearts = newHearts,
                heartRecoveredThisTurn = shouldRecoverHeart,
                lastAnswerCorrect = true,
                isNewBestStreak = isNewRecord
            )
        } else {
            val newHearts = state.hearts - 1
            val isOver = newHearts <= 0
            val newMissed = state.missedQuestions + currentQ
            state.copy(
                currentIndex = state.currentIndex + 1,
                currentStreak = 0,
                wrongCount = state.wrongCount + 1,
                hearts = max(0, newHearts),
                isFinished = isOver,
                missedQuestions = newMissed,
                heartRecoveredThisTurn = false,
                lastAnswerCorrect = false
            )
        }
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
 * Modern, low-latency, warm harmonic sound synthesizer for quiz & speed blitz interactions.
 * Synthesizes soft acoustic-style waveforms (warm bell ding, muted wooden thud)
 * directly via [AudioTrack] in static memory buffers:
 * - Zero external assets or file I/O overhead.
 * - Single-track retriggering: cutting off previous audio to eliminate overlapping noise on rapid clicks.
 * - Fallback safety for Robolectric / headless environments.
 */
object BlitzSoundPlayer {
    private const val SAMPLE_RATE = 44100
    private val lock = Any()
    private var activeTrack: AudioTrack? = null

    // Lazy pre-synthesized PCM buffers
    private val correctPcm: ByteArray by lazy { generateCorrectChimePcm() }
    private val wrongPcm: ByteArray by lazy { generateWrongThudPcm() }
    private val comboPcm: ByteArray by lazy { generateComboChimePcm() }
    private val tickPcm: ByteArray by lazy { generateTickPcm() }

    fun playCorrectChime(enabled: Boolean = true) {
        if (!enabled) return
        playPcmSafe(correctPcm)
    }

    fun playComboLevelUpChime(enabled: Boolean = true) {
        if (!enabled) return
        playPcmSafe(comboPcm)
    }

    /**
     * Replaces the harsh telecom buzzer with a soft, muted wooden thud / gentle double-tap.
     */
    fun playWrongBuzz(enabled: Boolean = true) {
        if (!enabled) return
        playPcmSafe(wrongPcm)
    }

    fun playCountdownTick(enabled: Boolean = true) {
        if (!enabled) return
        playPcmSafe(tickPcm)
    }

    private fun playPcmSafe(pcmData: ByteArray) {
        synchronized(lock) {
            runCatching {
                activeTrack?.run {
                    runCatching { stop() }
                    runCatching { release() }
                }
                activeTrack = null

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(pcmData.size)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(pcmData, 0, pcmData.size)
                track.play()
                activeTrack = track
            }.onFailure {
                // Safe headless / test fallback
            }
        }
    }

    /**
     * Soft, warm bell chime: Dual harmonious frequencies (880 Hz A5 + 1046.5 Hz C6)
     * with an 8ms anti-click attack and exponential decay over 240ms.
     */
    private fun generateCorrectChimePcm(): ByteArray {
        val durationMs = 240
        val totalSamples = (SAMPLE_RATE * durationMs) / 1000
        val buffer = ByteBuffer.allocate(totalSamples * 2).order(ByteOrder.LITTLE_ENDIAN)
        val attackSamples = (SAMPLE_RATE * 0.008).toInt()

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val envelope = when {
                i < attackSamples -> i.toDouble() / attackSamples.toDouble()
                else -> exp(-(t - 0.008) / 0.08)
            }
            val sampleVal = (sin(2.0 * PI * 880.0 * t) * 0.65 + sin(2.0 * PI * 1046.5 * t) * 0.35) * envelope * 0.35
            val pcmShort = (sampleVal * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            buffer.putShort(pcmShort)
        }
        return buffer.array()
    }

    /**
     * Soft low wooden thud: Low acoustic frequency sliding from 170 Hz down to 110 Hz
     * with rapid decay (110ms) mimicking a warm muted marimba/wood tap.
     */
    private fun generateWrongThudPcm(): ByteArray {
        val durationMs = 110
        val totalSamples = (SAMPLE_RATE * durationMs) / 1000
        val buffer = ByteBuffer.allocate(totalSamples * 2).order(ByteOrder.LITTLE_ENDIAN)
        val attackSamples = (SAMPLE_RATE * 0.004).toInt()

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val envelope = when {
                i < attackSamples -> i.toDouble() / attackSamples.toDouble()
                else -> exp(-(t - 0.004) / 0.035)
            }
            val freq = 170.0 - (60.0 * (i.toDouble() / totalSamples))
            val sampleVal = sin(2.0 * PI * freq * t) * envelope * 0.30
            val pcmShort = (sampleVal * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            buffer.putShort(pcmShort)
        }
        return buffer.array()
    }

    /**
     * Sparkling ascending combo chime: 3 harmonious notes in rapid succession (D5 -> G5 -> D6).
     */
    private fun generateComboChimePcm(): ByteArray {
        val durationMs = 280
        val totalSamples = (SAMPLE_RATE * durationMs) / 1000
        val buffer = ByteBuffer.allocate(totalSamples * 2).order(ByteOrder.LITTLE_ENDIAN)
        val stepSamples = totalSamples / 3

        for (i in 0 until totalSamples) {
            val noteIndex = (i / stepSamples).coerceIn(0, 2)
            val noteSample = i % stepSamples
            val tNote = noteSample.toDouble() / SAMPLE_RATE
            val freq = when (noteIndex) {
                0 -> 587.33 // D5
                1 -> 783.99 // G5
                else -> 1174.66 // D6
            }
            val envelope = exp(-tNote / 0.06)
            val sampleVal = sin(2.0 * PI * freq * tNote) * envelope * 0.30
            val pcmShort = (sampleVal * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            buffer.putShort(pcmShort)
        }
        return buffer.array()
    }

    /**
     * Subtle, quiet 35ms mechanical clock tick.
     */
    private fun generateTickPcm(): ByteArray {
        val durationMs = 35
        val totalSamples = (SAMPLE_RATE * durationMs) / 1000
        val buffer = ByteBuffer.allocate(totalSamples * 2).order(ByteOrder.LITTLE_ENDIAN)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val envelope = exp(-t / 0.007)
            val sampleVal = sin(2.0 * PI * 800.0 * t) * envelope * 0.15
            val pcmShort = (sampleVal * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            buffer.putShort(pcmShort)
        }
        return buffer.array()
    }
}
