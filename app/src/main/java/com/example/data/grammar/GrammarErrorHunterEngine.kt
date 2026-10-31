package com.example.data.grammar

import androidx.compose.runtime.Immutable
import com.example.data.local.GrammarExerciseItem
import com.example.data.local.GrammarLessonEntity
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt
import kotlin.random.Random

@Immutable
data class GrammarHunterItem(
    val id: String,
    val lessonId: String,
    val lessonTopic: String,
    val level: String,
    val tokens: List<String>,
    val errorTokenIndex: Int, // -1 if isNoError
    val isNoError: Boolean,
    val faultyWord: String,
    val correctReplacement: String,
    val correctionOptions: List<String>,
    val explanationAr: String,
    val originalPrompt: String
)

enum class HunterStep {
    STEP_1_SPOT_ERROR,
    STEP_2_CHOOSE_CORRECTION,
    REVEAL_EXPLANATION
}

@Immutable
data class GrammarHunterSessionState(
    val items: List<GrammarHunterItem> = emptyList(),
    val currentIndex: Int = 0,
    val currentStep: HunterStep = HunterStep.STEP_1_SPOT_ERROR,
    val selectedTokenIndex: Int? = null,
    val selectedNoError: Boolean = false,
    val selectedCorrection: String? = null,
    val step1Correct: Boolean? = null,
    val step2Correct: Boolean? = null,
    val score: Int = 0,
    val isFinished: Boolean = false,
    val missedItems: List<GrammarHunterItem> = emptyList(),
    val isNewBestScore: Boolean = false
) {
    val currentItem: GrammarHunterItem?
        get() = items.getOrNull(currentIndex)

    val totalCount: Int
        get() = items.size

    val accuracyPercent: Int
        get() = if (totalCount > 0) {
            ((score.toDouble() / totalCount.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
        } else 0
}

object GrammarErrorHunterEngine {
    const val DEFAULT_CHALLENGE_COUNT = 10
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun createSession(
        lessons: List<GrammarLessonEntity>,
        userLevel: String? = null,
        count: Int = DEFAULT_CHALLENGE_COUNT,
        seed: Int = System.currentTimeMillis().toInt()
    ): GrammarHunterSessionState {
        val rng = Random(seed)

        // Filter by user level if provided, otherwise all
        val levelFiltered = if (!userLevel.isNullOrBlank()) {
            val matching = lessons.filter { it.level.equals(userLevel, ignoreCase = true) }
            matching.ifEmpty { lessons }
        } else {
            lessons
        }

        val allCandidates = mutableListOf<GrammarHunterItem>()

        for (lesson in levelFiltered) {
            val exercises = try {
                if (lesson.exercisesJson.isNotBlank() && lesson.exercisesJson != "[]") {
                    json.decodeFromString<List<GrammarExerciseItem>>(lesson.exercisesJson)
                } else emptyList()
            } catch (e: Exception) {
                emptyList()
            }

            for ((exIdx, ex) in exercises.withIndex()) {
                if (ex.prompt.isBlank() || ex.answer.isBlank() || ex.options.size < 2) continue
                if (!ex.prompt.contains("____") && !ex.prompt.contains("___")) continue

                val item = buildHunterItem(
                    lesson = lesson,
                    exercise = ex,
                    itemIndex = exIdx,
                    makeNoError = (allCandidates.size % 5 == 3), // ~20% No Error sentences
                    rng = rng
                )
                if (item != null) {
                    allCandidates.add(item)
                }
            }
        }

        val finalItems = if (allCandidates.size >= count) {
            allCandidates.shuffled(rng).take(count)
        } else if (allCandidates.isNotEmpty()) {
            val list = mutableListOf<GrammarHunterItem>()
            while (list.size < count) {
                list.addAll(allCandidates.shuffled(rng))
            }
            list.take(count)
        } else {
            fallbackItems().shuffled(rng).take(count)
        }

        return GrammarHunterSessionState(
            items = finalItems,
            currentIndex = 0,
            currentStep = HunterStep.STEP_1_SPOT_ERROR,
            score = 0,
            isFinished = false,
            missedItems = emptyList()
        )
    }

    private fun buildHunterItem(
        lesson: GrammarLessonEntity,
        exercise: GrammarExerciseItem,
        itemIndex: Int,
        makeNoError: Boolean,
        rng: Random
    ): GrammarHunterItem? {
        val blankRegex = Regex("_{2,}|\\[blank\\]")
        val distractors = exercise.options
            .map { it.trim() }
            .filter { !it.equals(exercise.answer.trim(), ignoreCase = true) }
        if (distractors.isEmpty() && !makeNoError) return null

        val answerTrimmed = exercise.answer.trim()

        if (makeNoError) {
            val fullSentence = exercise.prompt.replace(blankRegex, answerTrimmed)
            val tokens = fullSentence.split("\\s+".toRegex()).filter { it.isNotBlank() }
            return GrammarHunterItem(
                id = "${lesson.id}_gh_${itemIndex}_noerr",
                lessonId = lesson.id,
                lessonTopic = lesson.topic.ifBlank { lesson.titleEn },
                level = lesson.level,
                tokens = tokens,
                errorTokenIndex = -1,
                isNoError = true,
                faultyWord = "",
                correctReplacement = answerTrimmed,
                correctionOptions = emptyList(),
                explanationAr = exercise.explanationAr.ifBlank { "الجملة صحيحة قواعدياً ولا تحتوي على خطأ." },
                originalPrompt = exercise.prompt
            )
        } else {
            val wrongChoice = distractors.shuffled(rng).first()
            val fullSentence = exercise.prompt.replace(blankRegex, wrongChoice)
            val tokens = fullSentence.split("\\s+".toRegex()).filter { it.isNotBlank() }

            // Find which token index holds the wrongChoice
            val errorIdx = tokens.indexOfFirst { token ->
                val cleanToken = token.replace(Regex("[.,!?;:\"']"), "")
                cleanToken.equals(wrongChoice, ignoreCase = true)
            }
            if (errorIdx == -1) return null

            val correctionChoices = (distractors.shuffled(rng).take(2) + answerTrimmed)
                .distinct()
                .shuffled(rng)

            return GrammarHunterItem(
                id = "${lesson.id}_gh_${itemIndex}_err",
                lessonId = lesson.id,
                lessonTopic = lesson.topic.ifBlank { lesson.titleEn },
                level = lesson.level,
                tokens = tokens,
                errorTokenIndex = errorIdx,
                isNoError = false,
                faultyWord = wrongChoice,
                correctReplacement = answerTrimmed,
                correctionOptions = correctionChoices,
                explanationAr = exercise.explanationAr.ifBlank { "التصحيح المناسب هو $answerTrimmed." },
                originalPrompt = exercise.prompt
            )
        }
    }

    /**
     * Step 1: User spots error by tapping a word token or tapping "No Error".
     * If user taps No Error: pass tokenIndex = null, noErrorTapped = true.
     */
    fun spotError(
        state: GrammarHunterSessionState,
        tokenIndex: Int?,
        noErrorTapped: Boolean = false
    ): GrammarHunterSessionState {
        val item = state.currentItem ?: return state
        if (state.currentStep != HunterStep.STEP_1_SPOT_ERROR) return state

        if (noErrorTapped) {
            val isCorrect = item.isNoError
            return if (isCorrect) {
                // Correct! There was no error in this sentence
                state.copy(
                    currentStep = HunterStep.REVEAL_EXPLANATION,
                    selectedTokenIndex = null,
                    selectedNoError = true,
                    step1Correct = true,
                    step2Correct = true,
                    score = state.score + 1
                )
            } else {
                // Wrong! There was an error, user missed it
                val newMissed = (state.missedItems + item).distinctBy { it.id }
                state.copy(
                    currentStep = HunterStep.STEP_2_CHOOSE_CORRECTION, // Still let them see and correct the error!
                    selectedTokenIndex = null,
                    selectedNoError = true,
                    step1Correct = false,
                    missedItems = newMissed
                )
            }
        } else if (tokenIndex != null) {
            val isCorrectToken = (!item.isNoError && tokenIndex == item.errorTokenIndex)
            return if (isCorrectToken) {
                // Correctly spotted the error! Move to Step 2
                state.copy(
                    currentStep = HunterStep.STEP_2_CHOOSE_CORRECTION,
                    selectedTokenIndex = tokenIndex,
                    selectedNoError = false,
                    step1Correct = true
                )
            } else {
                // Selected a token that is not the error, or sentence was No Error
                val newMissed = (state.missedItems + item).distinctBy { it.id }
                if (item.isNoError) {
                    // Sentence was No Error, jump to reveal
                    state.copy(
                        currentStep = HunterStep.REVEAL_EXPLANATION,
                        selectedTokenIndex = tokenIndex,
                        selectedNoError = false,
                        step1Correct = false,
                        step2Correct = false,
                        missedItems = newMissed
                    )
                } else {
                    // Jump to Step 2 to practice correction
                    state.copy(
                        currentStep = HunterStep.STEP_2_CHOOSE_CORRECTION,
                        selectedTokenIndex = tokenIndex,
                        selectedNoError = false,
                        step1Correct = false,
                        missedItems = newMissed
                    )
                }
            }
        }
        return state
    }

    /**
     * Step 2: User selects correction chip.
     */
    fun submitCorrection(
        state: GrammarHunterSessionState,
        chosenCorrection: String,
        previousBestScore: Int = 0
    ): GrammarHunterSessionState {
        val item = state.currentItem ?: return state
        if (state.currentStep != HunterStep.STEP_2_CHOOSE_CORRECTION) return state

        val isCorrectionRight = chosenCorrection.trim().equals(item.correctReplacement.trim(), ignoreCase = true)
        val fullCredit = (state.step1Correct == true && isCorrectionRight)
        val newScore = if (fullCredit) state.score + 1 else state.score

        val newMissed = if (fullCredit) state.missedItems else (state.missedItems + item).distinctBy { it.id }

        return state.copy(
            currentStep = HunterStep.REVEAL_EXPLANATION,
            selectedCorrection = chosenCorrection,
            step2Correct = isCorrectionRight,
            score = newScore,
            missedItems = newMissed
        )
    }

    /**
     * Advance to the next sentence or finish challenge.
     */
    fun advanceNext(
        state: GrammarHunterSessionState,
        previousBestScore: Int = 0
    ): GrammarHunterSessionState {
        val nextIdx = state.currentIndex + 1
        return if (nextIdx < state.items.size) {
            state.copy(
                currentIndex = nextIdx,
                currentStep = HunterStep.STEP_1_SPOT_ERROR,
                selectedTokenIndex = null,
                selectedNoError = false,
                selectedCorrection = null,
                step1Correct = null,
                step2Correct = null
            )
        } else {
            val isRecord = state.score > previousBestScore
            state.copy(
                isFinished = true,
                isNewBestScore = isRecord
            )
        }
    }

    /**
     * Allows user to replay the missed sentences.
     */
    fun retryMistakes(state: GrammarHunterSessionState): GrammarHunterSessionState {
        if (state.missedItems.isEmpty()) return state
        return state.copy(
            items = state.missedItems,
            currentIndex = 0,
            currentStep = HunterStep.STEP_1_SPOT_ERROR,
            selectedTokenIndex = null,
            selectedNoError = false,
            selectedCorrection = null,
            step1Correct = null,
            step2Correct = null,
            score = 0,
            isFinished = false,
            missedItems = emptyList(),
            isNewBestScore = false
        )
    }

    fun fallbackItems(): List<GrammarHunterItem> = listOf(
        GrammarHunterItem(
            id = "fb_gh_1",
            lessonId = "g001",
            lessonTopic = "Verb to be",
            level = "A1",
            tokens = listOf("She", "are", "my", "sister."),
            errorTokenIndex = 1,
            isNoError = false,
            faultyWord = "are",
            correctReplacement = "is",
            correctionOptions = listOf("am", "is", "be"),
            explanationAr = "الضمير She مفرد غائب ويأخذ الفعل is وليس are.",
            originalPrompt = "She ____ my sister."
        ),
        GrammarHunterItem(
            id = "fb_gh_2",
            lessonId = "g002",
            lessonTopic = "Past Simple",
            level = "A2",
            tokens = listOf("They", "goed", "to", "the", "market", "yesterday."),
            errorTokenIndex = 1,
            isNoError = false,
            faultyWord = "goed",
            correctReplacement = "went",
            correctionOptions = listOf("gone", "went", "going"),
            explanationAr = "الفعل go فعل شاذ وتصريفه في الماضي البسيط هو went وليس goed.",
            originalPrompt = "They ____ to the market yesterday."
        ),
        GrammarHunterItem(
            id = "fb_gh_3",
            lessonId = "g003",
            lessonTopic = "Present Perfect with For & Since",
            level = "B1",
            tokens = listOf("I", "have", "lived", "here", "since", "five", "years."),
            errorTokenIndex = 4,
            isNoError = false,
            faultyWord = "since",
            correctReplacement = "for",
            correctionOptions = listOf("for", "during", "in"),
            explanationAr = "نستخدم for مع المدة الزمنية المحددة (five years) بينما since لنقطة البداية.",
            originalPrompt = "I have lived here ____ five years."
        ),
        GrammarHunterItem(
            id = "fb_gh_4",
            lessonId = "g004",
            lessonTopic = "Articles (A / An / The)",
            level = "A1",
            tokens = listOf("He", "is", "an", "honest", "man."),
            errorTokenIndex = -1,
            isNoError = true,
            faultyWord = "",
            correctReplacement = "an",
            correctionOptions = emptyList(),
            explanationAr = "الجملة صحيحة! نستخدم an قبل honest لأن حرف h صامت وتبدأ بصوت متحرك.",
            originalPrompt = "He is ____ honest man."
        ),
        GrammarHunterItem(
            id = "fb_gh_5",
            lessonId = "g005",
            lessonTopic = "Conditionals",
            level = "B2",
            tokens = listOf("If", "I", "will", "see", "him,", "I", "will", "tell", "him."),
            errorTokenIndex = 2,
            isNoError = false,
            faultyWord = "will",
            correctReplacement = "see",
            correctionOptions = listOf("see", "saw", "seen"),
            explanationAr = "في جملة شرط First Conditional نستخدم المضارع البسيط في جملة if وليس will.",
            originalPrompt = "If I ____ him, I will tell him."
        ),
        GrammarHunterItem(
            id = "fb_gh_6",
            lessonId = "g006",
            lessonTopic = "Passive Voice",
            level = "B1",
            tokens = listOf("The", "letter", "was", "wrote", "by", "Sara."),
            errorTokenIndex = 3,
            isNoError = false,
            faultyWord = "wrote",
            correctReplacement = "written",
            correctionOptions = listOf("written", "write", "writing"),
            explanationAr = "في المبني للمجهول في الماضي نستخدم was متبوعة بالتصريف الثالث written.",
            originalPrompt = "The letter was ____ by Sara."
        ),
        GrammarHunterItem(
            id = "fb_gh_7",
            lessonId = "g007",
            lessonTopic = "Comparatives",
            level = "A2",
            tokens = listOf("This", "car", "is", "more", "cheaper", "than", "mine."),
            errorTokenIndex = 3,
            isNoError = false,
            faultyWord = "more",
            correctReplacement = "much",
            correctionOptions = listOf("much", "far", "very"),
            explanationAr = "الصفة القصيرة cheap تأخذ er فقط (cheaper) ولا نضع قبلها more.",
            originalPrompt = "This car is ____ cheaper than mine."
        ),
        GrammarHunterItem(
            id = "fb_gh_8",
            lessonId = "g008",
            lessonTopic = "Present Continuous",
            level = "A1",
            tokens = listOf("Look!", "The", "sun", "is", "shining."),
            errorTokenIndex = -1,
            isNoError = true,
            faultyWord = "",
            correctReplacement = "shining",
            correctionOptions = emptyList(),
            explanationAr = "الجملة صحيحة قواعدياً! تدل Look على حدث مستمر الآن مع is + V-ing.",
            originalPrompt = "Look! The sun is ____."
        ),
        GrammarHunterItem(
            id = "fb_gh_9",
            lessonId = "g009",
            lessonTopic = "Modal Verbs",
            level = "B1",
            tokens = listOf("You", "must", "to", "finish", "your", "homework."),
            errorTokenIndex = 2,
            isNoError = false,
            faultyWord = "to",
            correctReplacement = "finish",
            correctionOptions = listOf("finish", "finishing", "finished"),
            explanationAr = "الأفعال الناقصة مثل must يأتي بعدها المصدر مباشرة بدون to.",
            originalPrompt = "You must ____ your homework."
        ),
        GrammarHunterItem(
            id = "fb_gh_10",
            lessonId = "g010",
            lessonTopic = "Relative Clauses",
            level = "B2",
            tokens = listOf("The", "man", "which", "called", "you", "is", "my", "uncle."),
            errorTokenIndex = 2,
            isNoError = false,
            faultyWord = "which",
            correctReplacement = "who",
            correctionOptions = listOf("who", "whose", "whom"),
            explanationAr = "نستخدم who مع العاقل (The man) بينما which لغير العاقل.",
            originalPrompt = "The man ____ called you is my uncle."
        )
    )
}
