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
 * Selectable sound styles for wrong-answer audio feedback across Mini-Games & Quizzes.
 */
enum class WrongSoundStyle(
    val id: String,
    val labelEn: String,
    val subtitleEn: String,
    val labelAr: String
) {
    MARIMBA_DROP(
        id = "marimba_drop",
        labelEn = "Marimba",
        subtitleEn = "Wood Drop",
        labelAr = "ماريمبا خشبية"
    ),
    DUO_DESCENDING(
        id = "duo_descending",
        labelEn = "Duo Chime",
        subtitleEn = "Two-Tone",
        labelAr = "نغمة مزدوجة هابطة"
    ),
    ARCADE_BUZZ(
        id = "arcade_buzz",
        labelEn = "Buzz",
        subtitleEn = "Classic",
        labelAr = "جرس كلاسيكي"
    ),
    GENTLE_THUD(
        id = "gentle_thud",
        labelEn = "Soft Tap",
        subtitleEn = "Subtle",
        labelAr = "نقرة هادئة"
    )
}

/**
 * Modern, low-latency, warm harmonic sound synthesizer for quiz & speed blitz interactions.
 * Synthesizes a matching acoustic marimba pair directly via [AudioTrack] in static memory buffers:
 * - Right Answer: Cheerful ascending wooden marimba arpeggio (E5 -> G5 -> C6).
 * - Wrong Answer: Warm descending wooden marimba drop (G4 -> C4).
 * - Zero external assets or file I/O overhead.
 * - Single-track retriggering: cutting off previous audio to eliminate overlapping noise on rapid clicks.
 * - Fallback safety for Robolectric / headless environments.
 */
object BlitzSoundPlayer {
    private const val SAMPLE_RATE = 44100
    private val lock = Any()
    private var activeTrack: AudioTrack? = null

    @Volatile
    var currentWrongSoundStyle: WrongSoundStyle = WrongSoundStyle.MARIMBA_DROP
        private set

    // Lazy pre-synthesized PCM buffers
    private val correctPcm: ByteArray by lazy { generateCorrectMarimbaPcm() }
    private val wrongPcm: ByteArray by lazy { generateWrongMarimbaDropPcm() }
    private val comboPcm: ByteArray by lazy { generateComboChimePcm() }
    private val tickPcm: ByteArray by lazy { generateTickPcm() }

    fun setWrongSoundStyle(style: WrongSoundStyle) {
        currentWrongSoundStyle = style
    }

    /**
     * Plays the matching acoustic marimba right-answer ascending chime (E5 -> G5 -> C6).
     */
    fun playCorrectChime(enabled: Boolean = true) {
        if (!enabled) return
        playPcmSafe(correctPcm)
    }

    fun playComboLevelUpChime(enabled: Boolean = true) {
        if (!enabled) return
        playPcmSafe(comboPcm)
    }

    /**
     * Plays the warm acoustic marimba descending drop (G4 -> C4).
     */
    fun playWrongBuzz(
        enabled: Boolean = true,
        style: WrongSoundStyle = currentWrongSoundStyle
    ) {
        if (!enabled) return
        val pcm = if (style == WrongSoundStyle.MARIMBA_DROP || style == currentWrongSoundStyle) {
            wrongPcm
        } else {
            generateWrongMarimbaDropPcm()
        }
        playPcmSafe(pcm)
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
     * Cheerful ascending acoustic marimba arpeggio (E5 -> G5 -> C6):
     * Harmonious ("ملائم") companion to the descending marimba wrong answer drop.
     * Note 1 (0..75ms): E5 (659.25 Hz)
     * Note 2 (75..150ms): G5 (783.99 Hz)
     * Note 3 (150..280ms): C6 (1046.50 Hz) with warm marimba resonator decay.
     */
    private fun generateCorrectMarimbaPcm(): ByteArray {
        val note1Ms = 75
        val note2Ms = 75
        val note3Ms = 130
        val durationMs = note1Ms + note2Ms + note3Ms
        val totalSamples = (SAMPLE_RATE * durationMs) / 1000
        val note1Samples = (SAMPLE_RATE * note1Ms) / 1000
        val note2Samples = (SAMPLE_RATE * note2Ms) / 1000
        val buffer = ByteBuffer.allocate(totalSamples * 2).order(ByteOrder.LITTLE_ENDIAN)
        val attackSamples = (SAMPLE_RATE * 0.004).toInt()

        for (i in 0 until totalSamples) {
            val (noteIdx, localSample) = when {
                i < note1Samples -> 0 to i
                i < (note1Samples + note2Samples) -> 1 to (i - note1Samples)
                else -> 2 to (i - note1Samples - note2Samples)
            }

            val tLocal = localSample.toDouble() / SAMPLE_RATE
            val freq = when (noteIdx) {
                0 -> 659.25 // E5
                1 -> 783.99 // G5
                else -> 1046.50 // C6
            }

            val decayTau = when (noteIdx) {
                0 -> 0.055
                1 -> 0.055
                else -> 0.095
            }

            val envelope = when {
                localSample < attackSamples -> localSample.toDouble() / attackSamples.toDouble()
                else -> exp(-(tLocal - 0.004) / decayTau)
            }

            // Marimba fundamental + 4x resonator overtone tick for organic wooden mallet timbre
            val overtoneEnv = exp(-tLocal / 0.020)
            val wave = sin(2.0 * PI * freq * tLocal) * 0.74 +
                sin(2.0 * PI * (freq * 4.0) * tLocal) * 0.26 * overtoneEnv

            val sampleVal = wave * envelope * 0.42
            val pcmShort = (sampleVal * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
            buffer.putShort(pcmShort)
        }
        return buffer.array()
    }

    /**
     * Warm acoustic marimba two-step drop: G4 (392.0 Hz) -> C4 (261.63 Hz)
     * with classic 4x marimba resonator overtone.
     */
    private fun generateWrongMarimbaDropPcm(): ByteArray {
        val note1Ms = 95
        val note2Ms = 145
        val durationMs = note1Ms + note2Ms
        val totalSamples = (SAMPLE_RATE * durationMs) / 1000
        val note1Samples = (SAMPLE_RATE * note1Ms) / 1000
        val buffer = ByteBuffer.allocate(totalSamples * 2).order(ByteOrder.LITTLE_ENDIAN)
        val attackSamples = (SAMPLE_RATE * 0.004).toInt()

        for (i in 0 until totalSamples) {
            val isFirstNote = i < note1Samples
            val localSample = if (isFirstNote) i else (i - note1Samples)
            val tLocal = localSample.toDouble() / SAMPLE_RATE
            val freq = if (isFirstNote) 392.00 else 261.63 // G4 -> C4

            val envelope = when {
                localSample < attackSamples -> localSample.toDouble() / attackSamples.toDouble()
                else -> exp(-(tLocal - 0.004) / 0.065)
            }
            val overtoneEnv = exp(-tLocal / 0.022)
            val wave = sin(2.0 * PI * freq * tLocal) * 0.72 +
                sin(2.0 * PI * (freq * 4.0) * tLocal) * 0.28 * overtoneEnv

            val sampleVal = wave * envelope * 0.40
            val pcmShort = (sampleVal * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
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
