package com.example.data.plan

import com.example.data.local.GrammarExerciseItem
import com.example.data.local.QuizQuestionItem
import com.example.data.local.QuizResultEntity
import com.example.data.local.WordBooksDao
import kotlinx.serialization.json.Json
import java.util.UUID

class LocalQuizEngine(
    private val dao: WordBooksDao,
    private val timeProvider: TimeProvider = SystemTimeProvider()
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Generates a 5-question Daily Mini-Quiz:
     * - 2 Vocabulary (EN -> AR, AR -> EN)
     * - 1 Grammar (from studied/available lessons)
     * - 1 Reading Context / Cloze
     * - 1 Listening (or additional Vocabulary if TTS is unavailable)
     */
    suspend fun generateDailyMiniQuiz(isTtsAvailable: Boolean = true): List<QuizQuestionItem> {
        val questions = mutableListOf<QuizQuestionItem>()

        // 1. Vocabulary questions (only words with non-empty arabic!)
        val vocabCandidates = dao.getAllTranslatedEntries().filter { it.arabic.isNotBlank() && it.text.isNotBlank() }.shuffled()
        if (vocabCandidates.size >= 4) {
            val q1Target = vocabCandidates[0]
            val q1Distractors = vocabCandidates.filter { it.id != q1Target.id }.shuffled().take(3)
            val q1Options = (listOf(q1Target.arabic) + q1Distractors.map { it.arabic }).shuffled()
            questions.add(
                QuizQuestionItem(
                    id = UUID.randomUUID().toString(),
                    skill = "vocabulary",
                    questionType = "mcq_en_ar",
                    promptEn = "What is the correct Arabic meaning of \"${q1Target.text}\" (${q1Target.pos})?",
                    promptAr = "ما هو المعنى العربي الصحيح لكلمة \"${q1Target.text}\"؟",
                    options = q1Options,
                    answerIndex = q1Options.indexOf(q1Target.arabic),
                    correctText = q1Target.arabic,
                    explanationAr = "المعنى الصحيح هو: ${q1Target.arabic}"
                )
            )

            val q2Target = vocabCandidates[1]
            val q2Distractors = vocabCandidates.filter { it.id != q2Target.id && it.text != q2Target.text }.shuffled().take(3)
            val q2Options = (listOf(q2Target.text) + q2Distractors.map { it.text }).shuffled()
            questions.add(
                QuizQuestionItem(
                    id = UUID.randomUUID().toString(),
                    skill = "vocabulary",
                    questionType = "mcq_ar_en",
                    promptEn = "Which English word matches: \"${q2Target.arabic}\"?",
                    promptAr = "أي كلمة إنجليزية تعني: \"${q2Target.arabic}\"؟",
                    options = q2Options,
                    answerIndex = q2Options.indexOf(q2Target.text),
                    correctText = q2Target.text,
                    explanationAr = "الكلمة الصحيحة هي: ${q2Target.text}"
                )
            )
        }

        // 2. Grammar question
        val grammarLessons = dao.getAllGrammarLessons()
        val grammarQuestion = buildGrammarQuestion(grammarLessons)
        if (grammarQuestion != null) {
            questions.add(grammarQuestion)
        }

        // 3. Reading / Cloze Context question
        val readingQuestion = buildReadingContextQuestion()
        if (readingQuestion != null) {
            questions.add(readingQuestion)
        }

        // 4. Listening question (if TTS available, otherwise vocab/reading)
        if (isTtsAvailable && vocabCandidates.size >= 3) {
            val listenTarget = vocabCandidates[2]
            val distractors = vocabCandidates.filter { it.id != listenTarget.id && it.text != listenTarget.text }.shuffled().take(3)
            val options = (listOf(listenTarget.text) + distractors.map { it.text }).shuffled()
            questions.add(
                QuizQuestionItem(
                    id = UUID.randomUUID().toString(),
                    skill = "listening",
                    questionType = "listening",
                    promptEn = "Listen to the audio and choose the correct English word:",
                    promptAr = "استمع للمقطع الصوتي واختر الكلمة الإنجليزية الصحيحة:",
                    options = options,
                    answerIndex = options.indexOf(listenTarget.text),
                    correctText = listenTarget.text,
                    audioText = listenTarget.text,
                    explanationAr = "الكلمة المنطوقة هي: ${listenTarget.text} (${listenTarget.arabic})"
                )
            )
        } else if (vocabCandidates.size >= 4) {
            val qTarget = vocabCandidates[3]
            val distractors = vocabCandidates.filter { it.id != qTarget.id }.shuffled().take(3)
            val options = (listOf(qTarget.arabic) + distractors.map { it.arabic }).shuffled()
            questions.add(
                QuizQuestionItem(
                    id = UUID.randomUUID().toString(),
                    skill = "vocabulary",
                    questionType = "mcq_en_ar",
                    promptEn = "Meaning of: \"${qTarget.text}\"",
                    promptAr = "ما معنى: \"${qTarget.text}\"؟",
                    options = options,
                    answerIndex = options.indexOf(qTarget.arabic),
                    correctText = qTarget.arabic,
                    explanationAr = "المعنى الصحيح هو: ${qTarget.arabic}"
                )
            )
        }

        return questions.take(5)
    }

    /**
     * Generates a 20-question Weekly Quiz:
     * Mix: 8 Vocabulary, 6 Grammar, 3 Reading, 3 Listening (or 6 Reading if no TTS).
     */
    suspend fun generateWeeklyQuiz(isTtsAvailable: Boolean = true): List<QuizQuestionItem> {
        val questions = mutableListOf<QuizQuestionItem>()
        val vocabCandidates = dao.getAllTranslatedEntries().filter { it.arabic.isNotBlank() && it.text.isNotBlank() }.shuffled()
        val grammarLessons = dao.getAllGrammarLessons().shuffled()

        // 1. 8 Vocabulary questions
        if (vocabCandidates.size >= 4) {
            var vIndex = 0
            while (questions.count { it.skill == "vocabulary" } < 8 && vIndex < 24) {
                val target = vocabCandidates[vIndex % vocabCandidates.size]
                val distractors = vocabCandidates.filter { it.id != target.id && it.text != target.text }.shuffled().take(3)
                if (distractors.size >= 3) {
                    val isEnToAr = (vIndex % 2 == 0)

                    if (isEnToAr) {
                        val opts = (listOf(target.arabic) + distractors.map { it.arabic }).shuffled()
                        questions.add(
                            QuizQuestionItem(
                                id = UUID.randomUUID().toString(),
                                skill = "vocabulary",
                                questionType = "mcq_en_ar",
                                promptEn = "Select Arabic meaning for: \"${target.text}\"",
                                promptAr = "اختر المعنى العربي لكلمة: \"${target.text}\"",
                                options = opts,
                                answerIndex = opts.indexOf(target.arabic),
                                correctText = target.arabic,
                                explanationAr = "المعنى الصحيح هو: ${target.arabic}"
                            )
                        )
                    } else {
                        val opts = (listOf(target.text) + distractors.map { it.text }).shuffled()
                        questions.add(
                            QuizQuestionItem(
                                id = UUID.randomUUID().toString(),
                                skill = "vocabulary",
                                questionType = "mcq_ar_en",
                                promptEn = "Select English word for: \"${target.arabic}\"",
                                promptAr = "اختر الكلمة الإنجليزية المعبرة عن: \"${target.arabic}\"",
                                options = opts,
                                answerIndex = opts.indexOf(target.text),
                                correctText = target.text,
                                explanationAr = "الكلمة الصحيحة هي: ${target.text}"
                            )
                        )
                    }
                }
                vIndex++
            }
        }

        // 2. 6 Grammar questions
        for (lesson in grammarLessons) {
            if (questions.count { it.skill == "grammar" } >= 6) break
            val q = buildGrammarQuestion(listOf(lesson))
            if (q != null) questions.add(q)
        }

        // 3. 3 Reading context questions
        val targetReadingCount = if (isTtsAvailable) 3 else 6
        val readingTexts = dao.getAllReadingTexts().shuffled()
        for (text in readingTexts) {
            if (questions.count { it.skill == "reading" } >= targetReadingCount) break
            val q = buildReadingContextQuestionFromText(text)
            if (q != null && questions.none { it.promptEn == q.promptEn }) questions.add(q)
        }

        // 4. 3 Listening questions (if TTS available)
        if (isTtsAvailable && vocabCandidates.size >= 4) {
            var lIndex = 0
            while (questions.count { it.skill == "listening" } < 3 && lIndex < 24) {
                val target = vocabCandidates[(lIndex + 4) % vocabCandidates.size]
                val distractors = vocabCandidates.filter { it.id != target.id && it.text != target.text }.shuffled().take(3)
                if (distractors.size >= 3) {
                    val opts = (listOf(target.text) + distractors.map { it.text }).shuffled()
                    questions.add(
                        QuizQuestionItem(
                            id = UUID.randomUUID().toString(),
                            skill = "listening",
                            questionType = "listening",
                            promptEn = "Listen and identify the spoken word:",
                            promptAr = "استمع واختر الكلمة المسموعة:",
                            options = opts,
                            answerIndex = opts.indexOf(target.text),
                            correctText = target.text,
                            audioText = target.text,
                            explanationAr = "الكلمة هي: ${target.text}"
                        )
                    )
                }
                lIndex++
            }
        }

        return questions.take(20)
    }

    private suspend fun buildGrammarQuestion(lessons: List<com.example.data.local.GrammarLessonEntity>): QuizQuestionItem? {
        val lesson = lessons.shuffled().firstOrNull() ?: return null
        val exercises: List<GrammarExerciseItem> = try {
            json.decodeFromString(lesson.exercisesJson)
        } catch (e: Exception) {
            emptyList()
        }
        val ex = exercises.shuffled().firstOrNull() ?: return null

        val options = if (ex.options.size >= 2) {
            ex.options.shuffled()
        } else {
            listOf(ex.answer, "is", "was", "been").shuffled()
        }

        return QuizQuestionItem(
            id = UUID.randomUUID().toString(),
            skill = "grammar",
            questionType = "grammar_mcq",
            promptEn = ex.prompt,
            promptAr = "قواعد: ${lesson.titleAr}",
            options = options,
            answerIndex = options.indexOf(ex.answer).coerceAtLeast(0),
            correctText = ex.answer,
            explanationAr = ex.explanationAr.ifBlank { "القاعدة من درس: ${lesson.titleAr}" }
        )
    }

    private fun buildReadingContextQuestionFromText(text: com.example.data.local.ReadingTextEntity): QuizQuestionItem? {
        val questionsList: List<com.example.data.local.ReadingQuestionItem> = try {
            json.decodeFromString(text.questionsJson)
        } catch (e: Exception) {
            emptyList()
        }

        val q = questionsList.shuffled().firstOrNull()
        if (q != null && q.options.size >= 2) {
            return QuizQuestionItem(
                id = UUID.randomUUID().toString(),
                skill = "reading",
                questionType = "reading_context",
                promptEn = "[Reading: ${text.titleEn}] ${q.prompt}",
                promptAr = "من نص القراءة: ${text.titleAr}",
                options = q.options,
                answerIndex = q.answerIndex.coerceIn(0, q.options.size - 1),
                correctText = q.options.getOrNull(q.answerIndex) ?: "",
                explanationAr = q.explanationAr
            )
        }

        return null
    }

    private suspend fun buildReadingContextQuestion(): QuizQuestionItem? {
        val texts = dao.getAllReadingTexts().shuffled()
        val text = texts.firstOrNull() ?: return null
        return buildReadingContextQuestionFromText(text)
    }

    /**
     * Records quiz performance per skill in quiz_results table.
     */
    suspend fun recordQuizSubmission(
        quizType: String,
        questions: List<QuizQuestionItem>,
        userAnswers: Map<String, Int>,
        date: String = timeProvider.todayDateString()
    ): Map<String, Float> {
        val skills = listOf("vocabulary", "grammar", "reading", "listening")
        val skillAccuracy = mutableMapOf<String, Float>()
        val now = timeProvider.currentTimeMillis()

        for (skill in skills) {
            val skillQuestions = questions.filter { it.skill == skill }
            if (skillQuestions.isNotEmpty()) {
                val correctCount = skillQuestions.count { q ->
                    val userIdx = userAnswers[q.id]
                    userIdx == q.answerIndex
                }
                val accuracy = (correctCount.toFloat() / skillQuestions.size.toFloat()) * 100f
                skillAccuracy[skill] = accuracy

                dao.insertQuizResult(
                    QuizResultEntity(
                        date = date,
                        quizType = quizType,
                        skill = skill,
                        totalQuestions = skillQuestions.size,
                        correctAnswers = correctCount,
                        accuracyPercent = accuracy,
                        detailsJson = json.encodeToString(skillQuestions.map { it.id }),
                        timestamp = now
                    )
                )
            }
        }

        return skillAccuracy
    }
}
