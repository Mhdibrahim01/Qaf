package com.example.data.onboarding

import androidx.compose.runtime.Immutable
import com.example.data.local.EntryEntity
import com.example.data.local.GrammarExerciseItem
import com.example.data.local.GrammarLessonEntity
import com.example.data.local.WordBooksDao
import kotlinx.serialization.json.Json
import kotlin.random.Random

enum class PlacementQuestionType {
    VOCABULARY,
    GRAMMAR
}

@Immutable
data class PlacementQuizQuestion(
    val id: String,
    val level: String,
    val type: PlacementQuestionType,
    val promptEn: String,
    val promptAr: String,
    val topicLabelEn: String,
    val topicLabelAr: String,
    val options: List<String>,
    val correctAnswer: String
)

@Immutable
data class CefrLevelOption(
    val level: String,
    val titleEn: String,
    val titleAr: String,
    val descriptionEn: String,
    val descriptionAr: String
)

object OnboardingPlacementQuiz {
    const val MIN_ENTRIES_PER_LEVEL = 100
    const val I_DONT_KNOW_OPTION = "__I_DONT_KNOW__"

    val CEFR_LEVELS: List<String> = listOf("A1", "A2", "B1", "B2", "C1")

    val LEVEL_OPTIONS: List<CefrLevelOption> = listOf(
        CefrLevelOption(
            level = "A1",
            titleEn = "Beginner",
            titleAr = "مبتدئ",
            descriptionEn = "Everyday words and simple sentences",
            descriptionAr = "كلمات يومية أساسية وجمل بسيطة"
        ),
        CefrLevelOption(
            level = "A2",
            titleEn = "Elementary",
            titleAr = "أساسي",
            descriptionEn = "Short conversations and familiar topics",
            descriptionAr = "محادثات قصيرة ومواضيع مألوفة"
        ),
        CefrLevelOption(
            level = "B1",
            titleEn = "Intermediate",
            titleAr = "متوسط",
            descriptionEn = "Main points of work, study, and travel",
            descriptionAr = "فهم النقاط الرئيسية في العمل والدراسة والسفر"
        ),
        CefrLevelOption(
            level = "B2",
            titleEn = "Upper-Intermediate",
            titleAr = "فوق المتوسط",
            descriptionEn = "Fluent discussion and academic/professional texts",
            descriptionAr = "نقاش بطلاقة ونصوص أكاديمية ومهنية"
        ),
        CefrLevelOption(
            level = "C1",
            titleEn = "Advanced",
            titleAr = "متقدم",
            descriptionEn = "Complex ideas, idioms, and nuanced expression",
            descriptionAr = "أفكار معقدة وتعابير دقيقة ومتقدمة"
        )
    )

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * Checks whether enough dictionary entries exist to start the placement quiz:
     * - If the dictionary import is already complete (`isImportCompleted == true`), never waits on the 100-entry threshold
     *   (only verifies at least 4 entries per level exist so real questions + distractors can be built without fallback content).
     * - Otherwise requires at least [minEntriesPerLevel] (default 100) entries with non-empty Arabic at each level A1..C1.
     */
    suspend fun hasEnoughEntriesForPlacementQuiz(
        dao: WordBooksDao,
        isImportCompleted: Boolean,
        minEntriesPerLevel: Int = MIN_ENTRIES_PER_LEVEL
    ): Boolean {
        val threshold = if (isImportCompleted) 4 else minEntriesPerLevel
        for (level in CEFR_LEVELS) {
            val count = dao.getEntryCountForLevelWithArabic(level)
            if (count < threshold) {
                return false
            }
        }
        return true
    }

    /**
     * Checks whether enough entries exist at the chosen [level] for Step 4 (First Win):
     * - If the import is already complete (`isImportCompleted == true`), never waits.
     * - Otherwise requires at least [minEntries] (default 100) entries at [level].
     */
    suspend fun hasEnoughEntriesForLevel(
        dao: WordBooksDao,
        level: String,
        isImportCompleted: Boolean,
        minEntries: Int = MIN_ENTRIES_PER_LEVEL
    ): Boolean {
        if (isImportCompleted) return true
        return dao.getEntryCountForLevelWithArabic(level) >= minEntries
    }

    /**
     * Builds a 10-question placement quiz (2 per level A1..C1) strictly from local SQLite entries
     * with non-empty Arabic and local grammar exercises.
     * - Never uses AI, network, or invented fallback content.
     * - For each level A1..C1: builds 1 vocabulary Arabic->English recall question + 1 grammar exercise question.
     * - If grammar is unavailable for a level, builds 2 vocabulary Arabic->English recall questions for that level.
     * - Returns null if local database does not have at least 4 distinct vocabulary words per level.
     */
    suspend fun buildPlacementQuizQuestions(
        dao: WordBooksDao,
        seed: Long = System.currentTimeMillis()
    ): List<PlacementQuizQuestion>? {
        val random = Random(seed)
        val questions = ArrayList<PlacementQuizQuestion>(10)

        for (level in CEFR_LEVELS) {
            val rawVocab = dao.getPlacementVocabularyCandidatesForLevel(level, limit = 80)
            val distinctVocab = distinctVocabularyCandidates(rawVocab)
            if (distinctVocab.size < 4) {
                return null
            }

            val shuffledVocab = distinctVocab.shuffled(random)
            val firstTarget = shuffledVocab[0]
            val firstDistractors = shuffledVocab
                .drop(1)
                .filter { !it.text.equals(firstTarget.text, ignoreCase = true) && it.arabic.trim() != firstTarget.arabic.trim() }
                .take(3)
            if (firstDistractors.size < 3) {
                return null
            }

            val firstOptions = (listOf(firstTarget.text.trim()) + firstDistractors.map { it.text.trim() })
                .shuffled(random)

            questions.add(
                PlacementQuizQuestion(
                    id = "quiz_${level}_1_vocab_${firstTarget.id}",
                    level = level,
                    type = PlacementQuestionType.VOCABULARY,
                    promptEn = "Choose the English word that matches this meaning:",
                    promptAr = firstTarget.arabic.trim(),
                    topicLabelEn = "Vocabulary • $level",
                    topicLabelAr = "مفردات • $level",
                    options = firstOptions,
                    correctAnswer = firstTarget.text.trim()
                )
            )

            // Try to pick 1 real grammar MCQ exercise for this level; if unavailable, pick a 2nd real vocabulary question
            val grammarLessons = dao.getGrammarLessonsForLevelQuiz(level)
            val grammarQuestion = pickGrammarQuestionForLevel(level, grammarLessons, random)
            if (grammarQuestion != null) {
                questions.add(grammarQuestion)
            } else {
                val secondTarget = shuffledVocab[1]
                val secondDistractors = shuffledVocab
                    .filter {
                        it.id != secondTarget.id &&
                            !it.text.equals(secondTarget.text, ignoreCase = true) &&
                            it.arabic.trim() != secondTarget.arabic.trim()
                    }
                    .take(3)
                if (secondDistractors.size < 3) {
                    return null
                }
                val secondOptions = (listOf(secondTarget.text.trim()) + secondDistractors.map { it.text.trim() })
                    .shuffled(random)

                questions.add(
                    PlacementQuizQuestion(
                        id = "quiz_${level}_2_vocab_${secondTarget.id}",
                        level = level,
                        type = PlacementQuestionType.VOCABULARY,
                        promptEn = "Choose the English word that matches this meaning:",
                        promptAr = secondTarget.arabic.trim(),
                        topicLabelEn = "Vocabulary • $level",
                        topicLabelAr = "مفردات • $level",
                        options = secondOptions,
                        correctAnswer = secondTarget.text.trim()
                    )
                )
            }
        }

        return questions
    }

    private fun distinctVocabularyCandidates(entries: List<EntryEntity>): List<EntryEntity> {
        val seenWords = HashSet<String>()
        val seenArabic = HashSet<String>()
        val result = ArrayList<EntryEntity>(entries.size)
        for (entry in entries) {
            val wordKey = entry.text.trim().lowercase()
            val arKey = entry.arabic.trim()
            if (wordKey.isNotEmpty() && arKey.isNotEmpty() && seenWords.add(wordKey) && seenArabic.add(arKey)) {
                result.add(entry)
            }
        }
        return result
    }

    private fun pickGrammarQuestionForLevel(
        level: String,
        lessons: List<GrammarLessonEntity>,
        random: Random
    ): PlacementQuizQuestion? {
        data class CandidateExercise(
            val lesson: GrammarLessonEntity,
            val exercise: GrammarExerciseItem
        )

        val candidates = ArrayList<CandidateExercise>()
        for (lesson in lessons) {
            val parsedExercises = runCatching {
                json.decodeFromString<List<GrammarExerciseItem>>(lesson.exercisesJson)
            }.getOrDefault(emptyList())

            for (ex in parsedExercises) {
                val cleanOptions = ex.options.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
                val cleanAnswer = ex.answer.trim()
                if (
                    ex.prompt.isNotBlank() &&
                    cleanAnswer.isNotEmpty() &&
                    cleanOptions.size >= 3 &&
                    cleanOptions.any { it.equals(cleanAnswer, ignoreCase = true) }
                ) {
                    val matchedAnswer = cleanOptions.first { it.equals(cleanAnswer, ignoreCase = true) }
                    candidates.add(
                        CandidateExercise(
                            lesson = lesson,
                            exercise = ex.copy(options = cleanOptions.take(4), answer = matchedAnswer)
                        )
                    )
                }
            }
        }

        if (candidates.isEmpty()) return null
        val chosen = candidates.random(random)
        val shuffledOptions = chosen.exercise.options.shuffled(random)
        val arabicSubtitle = chosen.lesson.titleAr.trim().ifBlank { "اختر الإجابة الصحيحة لإكمال الجملة" }

        return PlacementQuizQuestion(
            id = "quiz_${level}_2_grammar_${chosen.lesson.id}",
            level = level,
            type = PlacementQuestionType.GRAMMAR,
            promptEn = chosen.exercise.prompt.trim(),
            promptAr = arabicSubtitle,
            topicLabelEn = "Grammar • ${chosen.lesson.titleEn.ifBlank { level }}",
            topicLabelAr = "قواعد • $arabicSubtitle",
            options = shuffledOptions,
            correctAnswer = chosen.exercise.answer
        )
    }

    /**
     * Placement scoring algorithm:
     * Walk A1 to C1 in order and stop at the first level with fewer than 2 correct answers.
     * - If that level has 1/2 correct, suggest that level.
     * - Otherwise (0/2 correct), suggest the previous level (minimum A1).
     * - If all levels A1..C1 have 2/2 correct, suggest C1.
     */
    fun calculateSuggestedLevel(correctCountByLevel: Map<String, Int>): String {
        for ((index, level) in CEFR_LEVELS.withIndex()) {
            val correct = (correctCountByLevel[level] ?: 0).coerceIn(0, 2)
            if (correct < 2) {
                return if (correct == 1) {
                    level
                } else {
                    if (index > 0) CEFR_LEVELS[index - 1] else "A1"
                }
            }
        }
        return "C1"
    }

    /**
     * Computes per-level correct counts and the suggested CEFR level from the user's answers.
     * Choosing [I_DONT_KNOW_OPTION] counts as 0 (incorrect).
     */
    fun scorePlacementQuiz(
        questions: List<PlacementQuizQuestion>,
        answersByQuestionId: Map<String, String>
    ): Pair<String, Map<String, Int>> {
        val correctByLevel = LinkedHashMap<String, Int>()
        for (level in CEFR_LEVELS) {
            correctByLevel[level] = 0
        }
        for (q in questions) {
            val selected = answersByQuestionId[q.id]?.trim().orEmpty()
            val isCorrect = selected.isNotEmpty() &&
                selected != I_DONT_KNOW_OPTION &&
                selected.equals(q.correctAnswer.trim(), ignoreCase = true)
            if (isCorrect) {
                val lvl = q.level.uppercase()
                correctByLevel[lvl] = (correctByLevel[lvl] ?: 0) + 1
            }
        }
        val suggested = calculateSuggestedLevel(correctByLevel)
        return suggested to correctByLevel
    }
}
