package com.example.data.plan

import com.example.data.local.AttemptEntity
import com.example.data.local.DailyChecklistEntity
import com.example.data.local.DailyTaskItem
import com.example.data.local.EntryEntity
import com.example.data.local.GrammarExerciseItem
import com.example.data.local.GrammarLessonEntity
import com.example.data.local.QuizQuestionItem
import com.example.data.local.QuizResultEntity
import com.example.data.local.ReadingQuestionItem
import com.example.data.local.ReadingTextEntity
import com.example.data.local.StudyPlanProfileEntity
import com.example.data.local.WordBooksDao
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Locale
import java.util.UUID

@Serializable
data class DailyQuizScopeInfo(
    val vocabTitle: String = "10 New Words",
    val vocabCompleted: Boolean = false,
    val grammarLessonId: String = "",
    val grammarTitle: String = "Grammar Lesson",
    val grammarTitleAr: String = "درس القواعد",
    val grammarCompleted: Boolean = false,
    val readingStoryId: String = "",
    val readingTitle: String = "Reading Story",
    val readingTitleAr: String = "قصة القراءة",
    val readingCompleted: Boolean = false,
    val allCompleted: Boolean = false
)

data class AlignedDailyQuizResult(
    val questions: List<QuizQuestionItem>,
    val scopeInfo: DailyQuizScopeInfo
)

class LocalQuizEngine(
    private val dao: WordBooksDao,
    private val timeProvider: TimeProvider = SystemTimeProvider(),
    private val studyPlanRepository: StudyPlanRepository? = null
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Generates a 5-question Daily Mini-Quiz with TTS-aware listening support
     * (2 Vocabulary, 1 Grammar, 1 Reading, and 1 Listening when TTS is available, or 2 Reading when TTS is unavailable).
     * Note: The 10-question daily plan assessment uses [generateAlignedDailyQuiz].
     */
    suspend fun generateDailyMiniQuiz(isTtsAvailable: Boolean = true): List<QuizQuestionItem> {
        val today = timeProvider.todayDateString()
        val checklist = dao.getDailyChecklist(today)
        val profile = dao.getStudyPlanProfile()
        val aligned = generateAlignedDailyQuiz(checklist, profile, isTtsAvailable).questions
        val vocabQuestions = aligned.filter { it.skill == "vocabulary" }.take(2)
        val grammarQuestion = aligned.firstOrNull { it.skill == "grammar" }
        val readingQuestions = aligned.filter { it.skill == "reading" }

        val result = mutableListOf<QuizQuestionItem>()
        result.addAll(vocabQuestions)
        if (grammarQuestion != null) result.add(grammarQuestion)
        if (readingQuestions.isNotEmpty()) result.add(readingQuestions.first())

        if (isTtsAvailable) {
            val vocabCandidates = dao.getAllTranslatedEntries().filter { it.arabic.isNotBlank() && it.text.isNotBlank() }
            if (vocabCandidates.size >= 4) {
                val target = vocabCandidates.first()
                val distractors = vocabCandidates.filter { it.id != target.id && !it.text.equals(target.text, ignoreCase = true) }.take(3)
                if (distractors.size >= 3) {
                    val opts = (listOf(target.text) + distractors.map { it.text }).shuffled()
                    result.add(
                        QuizQuestionItem(
                            id = UUID.randomUUID().toString(),
                            skill = "listening",
                            questionType = "listening",
                            promptEn = "Listen and identify the spoken word:",
                            promptAr = "استمع واختر الكلمة المسموعة:",
                            options = opts,
                            answerIndex = opts.indexOf(target.text).coerceAtLeast(0),
                            correctText = target.text,
                            audioText = target.text,
                            explanationAr = "الكلمة هي: ${target.text}"
                        )
                    )
                }
            }
        } else {
            val extraReading = readingQuestions.drop(1).firstOrNull()
                ?: aligned.filter { it.skill == "vocabulary" }.drop(2).firstOrNull()
            if (extraReading != null) {
                result.add(extraReading)
            }
        }

        // Pad to 5 if needed from aligned questions
        for (q in aligned) {
            if (result.size >= 5) break
            if (result.none { it.id == q.id }) {
                result.add(q)
            }
        }
        return result.take(5)
    }

    /**
     * Core aligned assessment generator that pairs today's checklist tasks
     * with exact questions and creates a DailyQuizScopeInfo preview.
     */
    suspend fun generateAlignedDailyQuiz(
        checklist: DailyChecklistEntity?,
        profile: StudyPlanProfileEntity?,
        isTtsAvailable: Boolean = true
    ): AlignedDailyQuizResult {
        val questions = mutableListOf<QuizQuestionItem>()
        val tasks: List<DailyTaskItem> = try {
            if (checklist != null && checklist.tasksJson.isNotBlank()) {
                json.decodeFromString(checklist.tasksJson)
            } else emptyList()
        } catch (_: Exception) { emptyList() }

        val userLevel = profile?.level?.trim()?.uppercase(Locale.US)?.ifBlank { "B2" } ?: "B2"
        val vocabTask = tasks.find { it.type == "new_words" }
        val grammarTask = tasks.find { it.type == "grammar" }
        val readingTask = tasks.find { it.type == "reading" }

        // -------------------------------------------------------------
        // 1. VOCABULARY (5 Questions from today's 10 assigned words)
        // -------------------------------------------------------------
        val assignedWords = mutableListOf<EntryEntity>()
        if (studyPlanRepository != null && profile != null) {
            try {
                val date = checklist?.date ?: timeProvider.todayDateString()
                val targetCount = vocabTask?.targetCount ?: 10
                val selection = studyPlanRepository.getTodayTargetNewWords(profile, targetCount, date)
                assignedWords.addAll(selection.filter { it.text.isNotBlank() && it.arabic.isNotBlank() })
            } catch (_: Exception) {
                // Ignore and fall back to database entries
            }
        }
        if (assignedWords.isEmpty()) {
            val levelEntries = dao.getAllTranslatedEntries()
                .filter { it.level.equals(userLevel, ignoreCase = true) && it.text.isNotBlank() && it.arabic.isNotBlank() }
            assignedWords.addAll(levelEntries.take(10))
        }
        if (assignedWords.size < 5) {
            val anyTranslated = dao.getAllTranslatedEntries()
                .filter { it.text.isNotBlank() && it.arabic.isNotBlank() }
            assignedWords.addAll(anyTranslated.take(10 - assignedWords.size))
        }

        val allTranslatedForLevel = dao.getAllTranslatedEntries()
            .filter { it.level.equals(userLevel, ignoreCase = true) && it.arabic.isNotBlank() && it.text.isNotBlank() }
        val distractorPool = (assignedWords + allTranslatedForLevel + dao.getAllTranslatedEntries())
            .distinctBy { it.text.lowercase().trim() }

        val vocabTargetWords = assignedWords.distinctBy { it.text.lowercase().trim() }.take(5)
        vocabTargetWords.forEachIndexed { index, target ->
            val otherWords = distractorPool
                .filter { !it.text.equals(target.text, ignoreCase = true) && it.arabic != target.arabic }
                .shuffled()
                .take(3)

            when (index % 3) {
                0 -> {
                    // EN -> AR
                    val options = (listOf(target.arabic) + otherWords.map { it.arabic }).shuffled()
                    questions.add(
                        QuizQuestionItem(
                            id = UUID.randomUUID().toString(),
                            skill = "vocabulary",
                            questionType = "mcq_en_ar",
                            promptEn = "What is the correct Arabic meaning of \"${target.text}\"${if (target.pos.isNotBlank()) " (${target.pos})" else ""}?",
                            promptAr = "ما المعنى العربي لكلمة اليوم: \"${target.text}\"؟",
                            options = options,
                            answerIndex = options.indexOf(target.arabic),
                            correctText = target.arabic,
                            explanationAr = "المعنى الصحيح لكلمة اليوم \"${target.text}\" هو: ${target.arabic}" +
                                if (target.examplesEn.isNotBlank()) " • مثال: ${target.examplesEn}" else ""
                        )
                    )
                }
                1 -> {
                    // AR -> EN
                    val options = (listOf(target.text) + otherWords.map { it.text }).shuffled()
                    questions.add(
                        QuizQuestionItem(
                            id = UUID.randomUUID().toString(),
                            skill = "vocabulary",
                            questionType = "mcq_ar_en",
                            promptEn = "Which English word matches: \"${target.arabic}\"?",
                            promptAr = "أي كلمة إنجليزية تطابق: \"${target.arabic}\"؟",
                            options = options,
                            answerIndex = options.indexOf(target.text),
                            correctText = target.text,
                            explanationAr = "الكلمة المطابقة للمعنى العربي \"${target.arabic}\" هي: ${target.text}"
                        )
                    )
                }
                else -> {
                    // Listening (if TTS available) or Contextual Fill-in
                    if (isTtsAvailable) {
                        val options = (listOf(target.text) + otherWords.map { it.text }).shuffled()
                        questions.add(
                            QuizQuestionItem(
                                id = UUID.randomUUID().toString(),
                                skill = "vocabulary",
                                questionType = "listening",
                                promptEn = "Listen to today's assigned word and select the correct match:",
                                promptAr = "استمع للكلمة المقررة اليوم واختر الكلمة الصحيحة:",
                                options = options,
                                answerIndex = options.indexOf(target.text),
                                correctText = target.text,
                                audioText = target.text,
                                explanationAr = "الكلمة المنطوقة هي: ${target.text} (${target.arabic})"
                            )
                        )
                    } else {
                        val options = (listOf(target.arabic) + otherWords.map { it.arabic }).shuffled()
                        questions.add(
                            QuizQuestionItem(
                                id = UUID.randomUUID().toString(),
                                skill = "vocabulary",
                                questionType = "mcq_en_ar",
                                promptEn = "Today's Vocabulary: Meaning of \"${target.text}\"",
                                promptAr = "مفردات اليوم: ما هو معنى \"${target.text}\"؟",
                                options = options,
                                answerIndex = options.indexOf(target.arabic),
                                correctText = target.arabic,
                                explanationAr = "المعنى الصحيح هو: ${target.arabic}"
                            )
                        )
                    }
                }
            }
        }

        // Fill up to 5 vocabulary if fewer targets were available
        while (questions.count { it.skill == "vocabulary" } < 5) {
            val fallbackTarget = distractorPool.firstOrNull { candidate -> questions.none { it.correctText == candidate.arabic || it.correctText == candidate.text } }
                ?: distractorPool.firstOrNull()
            if (fallbackTarget != null) {
                val opts = (listOf(fallbackTarget.arabic) + distractorPool.filter { it.id != fallbackTarget.id }.take(3).map { it.arabic }).shuffled()
                questions.add(
                    QuizQuestionItem(
                        id = UUID.randomUUID().toString(),
                        skill = "vocabulary",
                        questionType = "mcq_en_ar",
                        promptEn = "Vocabulary Review: What is the Arabic meaning of \"${fallbackTarget.text}\"?",
                        promptAr = "مراجعة مفردات: ما معنى \"${fallbackTarget.text}\"؟",
                        options = opts,
                        answerIndex = opts.indexOf(fallbackTarget.arabic),
                        correctText = fallbackTarget.arabic,
                        explanationAr = "المعنى الصحيح هو: ${fallbackTarget.arabic}"
                    )
                )
            } else {
                break
            }
        }

        // -------------------------------------------------------------
        // 2. GRAMMAR (3 Questions strictly from today's assigned grammar lesson)
        // -------------------------------------------------------------
        val grammarLessonId = grammarTask?.linkedId.orEmpty().ifBlank {
            dao.getUncompletedGrammarLessonsForLevel(userLevel).firstOrNull()?.id
                ?: dao.getAllGrammarLessons().firstOrNull { it.level.equals(userLevel, ignoreCase = true) }?.id
                ?: dao.getAllGrammarLessons().firstOrNull()?.id
                .orEmpty()
        }
        val assignedGrammarLesson = if (grammarLessonId.isNotBlank()) dao.getGrammarLessonById(grammarLessonId) else null

        if (assignedGrammarLesson != null) {
            val exercises: List<GrammarExerciseItem> = try {
                json.decodeFromString(assignedGrammarLesson.exercisesJson)
            } catch (_: Exception) { emptyList() }

            val candidateExercises = exercises.filter { it.prompt.isNotBlank() && it.answer.isNotBlank() }
            val targetExercises = candidateExercises.take(3)
            targetExercises.forEach { ex ->
                val exType = ex.type.lowercase().trim()
                val isReorder = exType == "reorder" ||
                    (ex.options.size >= 3 && !ex.options.contains(ex.answer) && ex.answer.trim().contains(" "))

                if (isReorder) {
                    val wordTokens = if (ex.options.size >= 2) {
                        ex.options.shuffled()
                    } else {
                        ex.answer.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.shuffled()
                    }
                    questions.add(
                        QuizQuestionItem(
                            id = UUID.randomUUID().toString(),
                            skill = "grammar",
                            questionType = "grammar_reorder",
                            promptEn = ex.prompt,
                            promptAr = "قواعد: ${assignedGrammarLesson.titleAr}",
                            options = wordTokens,
                            answerIndex = 0,
                            correctText = ex.answer.trim(),
                            explanationAr = ex.explanationAr.ifBlank { "القاعدة من درس اليوم: ${assignedGrammarLesson.titleAr} (${assignedGrammarLesson.titleEn})" }
                        )
                    )
                } else {
                    val options = if (ex.options.size >= 2 && ex.options.contains(ex.answer)) {
                        ex.options.shuffled()
                    } else {
                        val otherAnswers = candidateExercises.map { it.answer.trim() }
                            .filter { it.isNotBlank() && !it.equals(ex.answer.trim(), ignoreCase = true) }
                        val fallbackPool = (otherAnswers + listOf("is", "was", "been", "have", "had", "would")).distinct()
                            .filter { !it.equals(ex.answer.trim(), ignoreCase = true) }
                            .take(3)
                        (listOf(ex.answer) + fallbackPool).shuffled()
                    }
                    questions.add(
                        QuizQuestionItem(
                            id = UUID.randomUUID().toString(),
                            skill = "grammar",
                            questionType = "grammar_mcq",
                            promptEn = ex.prompt,
                            promptAr = "قواعد: ${assignedGrammarLesson.titleAr}",
                            options = options,
                            answerIndex = options.indexOf(ex.answer).coerceAtLeast(0),
                            correctText = ex.answer,
                            explanationAr = ex.explanationAr.ifBlank { "القاعدة من درس اليوم: ${assignedGrammarLesson.titleAr} (${assignedGrammarLesson.titleEn})" }
                        )
                    )
                }
            }
        }

        // Fill up to 3 grammar questions if exercises were fewer than 3
        while (questions.count { it.skill == "grammar" } < 3) {
            val fallbackQ = buildGrammarQuestion(if (assignedGrammarLesson != null) listOf(assignedGrammarLesson) else dao.getAllGrammarLessons())
            if (fallbackQ != null) {
                questions.add(fallbackQ.copy(id = UUID.randomUUID().toString()))
            } else {
                break
            }
        }

        // -------------------------------------------------------------
        // 3. READING (2 Questions strictly from today's assigned reading text)
        // -------------------------------------------------------------
        val readingTextId = readingTask?.linkedId.orEmpty().ifBlank {
            if (grammarLessonId.isNotBlank()) {
                val num = grammarLessonId.filter { it.isDigit() }
                if (num.isNotEmpty()) "r$num" else grammarLessonId.replaceFirst("g", "r")
            } else ""
        }
        val assignedReadingText = (if (readingTextId.isNotBlank()) dao.getReadingTextById(readingTextId) else null)
            ?: dao.getAllReadingTexts().firstOrNull { it.level.equals(userLevel, ignoreCase = true) }
            ?: dao.getAllReadingTexts().firstOrNull()

        if (assignedReadingText != null) {
            val readingQuestions: List<ReadingQuestionItem> = try {
                json.decodeFromString(assignedReadingText.questionsJson)
            } catch (_: Exception) { emptyList() }

            val validReadingQuestions = readingQuestions.filter { it.options.size >= 2 }
            val targetReadingQuestions = validReadingQuestions.take(2)
            targetReadingQuestions.forEach { q ->
                questions.add(
                    QuizQuestionItem(
                        id = UUID.randomUUID().toString(),
                        skill = "reading",
                        questionType = "reading_context",
                        promptEn = "[Reading: ${assignedReadingText.titleEn}] ${q.prompt}",
                        promptAr = "من نص اليوم: ${assignedReadingText.titleAr}",
                        options = q.options,
                        answerIndex = q.answerIndex.coerceIn(0, q.options.size - 1),
                        correctText = q.options.getOrNull(q.answerIndex) ?: "",
                        explanationAr = "من نص اليوم (${assignedReadingText.titleAr}): ${q.explanationAr}"
                    )
                )
            }
        }

        // Fill up to 2 reading questions if assigned text had fewer
        while (questions.count { it.skill == "reading" } < 2) {
            val fallbackQ = if (assignedReadingText != null) {
                buildReadingContextQuestionFromText(assignedReadingText)
            } else {
                buildReadingContextQuestion()
            }
            if (fallbackQ != null) {
                questions.add(fallbackQ.copy(id = UUID.randomUUID().toString()))
            } else {
                break
            }
        }

        val finalQuestions = questions.take(10)

        val scopeInfo = DailyQuizScopeInfo(
            vocabTitle = vocabTask?.titleEn ?: "Learn 10 New Words ($userLevel)",
            vocabCompleted = vocabTask?.isCompleted ?: false,
            grammarLessonId = assignedGrammarLesson?.id ?: "",
            grammarTitle = assignedGrammarLesson?.titleEn ?: (grammarTask?.titleEn ?: "Grammar Lesson"),
            grammarTitleAr = assignedGrammarLesson?.titleAr ?: (grammarTask?.titleAr ?: "درس القواعد"),
            grammarCompleted = grammarTask?.isCompleted ?: false,
            readingStoryId = assignedReadingText?.id ?: "",
            readingTitle = assignedReadingText?.titleEn ?: (readingTask?.titleEn ?: "Reading Story"),
            readingTitleAr = assignedReadingText?.titleAr ?: (readingTask?.titleAr ?: "قصة القراءة"),
            readingCompleted = readingTask?.isCompleted ?: false,
            allCompleted = (vocabTask?.isCompleted == true) && (grammarTask?.isCompleted == true) && (readingTask?.isCompleted == true)
        )

        return AlignedDailyQuizResult(
            questions = finalQuestions,
            scopeInfo = scopeInfo
        )
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

    /**
     * Generates a targeted 8-question Weak Areas Practice Quiz focusing on the learner's
     * weakest vocabulary words and uncompleted grammar lessons.
     */
    suspend fun generateWeakAreasQuiz(
        targetLevel: String = "B2",
        isTtsAvailable: Boolean = true
    ): List<QuizQuestionItem> {
        val questions = mutableListOf<QuizQuestionItem>()
        val weakWords = dao.getWeakestWords(12).filter { it.arabic.isNotBlank() && it.text.isNotBlank() }
        val allTranslated = dao.getAllTranslatedEntries().filter { it.arabic.isNotBlank() && it.text.isNotBlank() }.shuffled()
        val vocabPool = (weakWords + allTranslated).distinctBy { it.id }

        // 1. Up to 4 Weak Vocabulary questions
        if (vocabPool.size >= 4) {
            val targets = weakWords.ifEmpty { vocabPool }.take(4)
            targets.forEachIndexed { idx, target ->
                val distractors = vocabPool.filter { it.id != target.id && it.text != target.text }.shuffled().take(3)
                if (distractors.size >= 3) {
                    if (idx % 2 == 0) {
                        val opts = (listOf(target.arabic) + distractors.map { it.arabic }).shuffled()
                        questions.add(
                            QuizQuestionItem(
                                id = UUID.randomUUID().toString(),
                                skill = "vocabulary",
                                questionType = "mcq_en_ar",
                                promptEn = "Weak Word Review: What is the Arabic meaning of \"${target.text}\" (${target.pos})?",
                                promptAr = "مراجعة الكلمات الصعبة: ما معنى \"${target.text}\"؟",
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
                                promptEn = "Weak Word Review: Which English word means \"${target.arabic}\"?",
                                promptAr = "أي كلمة إنجليزية تعني: \"${target.arabic}\"؟",
                                options = opts,
                                answerIndex = opts.indexOf(target.text),
                                correctText = target.text,
                                explanationAr = "الكلمة الصحيحة هي: ${target.text}"
                            )
                        )
                    }
                }
            }
        }

        // 2. Up to 3 Weak / Uncompleted Grammar questions
        val uncompletedGrammar = dao.getUncompletedGrammarLessonsForLevel(targetLevel)
            .ifEmpty { dao.getAllGrammarLessons().filter { !it.isCompleted } }
            .ifEmpty { dao.getAllGrammarLessons() }
            .shuffled()
        for (lesson in uncompletedGrammar.take(3)) {
            val q = buildGrammarQuestion(listOf(lesson))
            if (q != null) questions.add(q)
        }

        // 3. 1 Listening question on a weak word
        if (isTtsAvailable && vocabPool.size >= 4) {
            val listenTarget = (weakWords.ifEmpty { vocabPool }).random()
            val distractors = vocabPool.filter { it.id != listenTarget.id && it.text != listenTarget.text }.shuffled().take(3)
            if (distractors.size >= 3) {
                val opts = (listOf(listenTarget.text) + distractors.map { it.text }).shuffled()
                questions.add(
                    QuizQuestionItem(
                        id = UUID.randomUUID().toString(),
                        skill = "listening",
                        questionType = "listening",
                        promptEn = "Listen and identify the spoken word:",
                        promptAr = "استمع واختر الكلمة الإنجليزية الصحيحة:",
                        options = opts,
                        answerIndex = opts.indexOf(listenTarget.text),
                        correctText = listenTarget.text,
                        audioText = listenTarget.text,
                        explanationAr = "الكلمة المنطوقة هي: ${listenTarget.text} (${listenTarget.arabic})"
                    )
                )
            }
        }

        return questions.take(8)
    }

    private suspend fun buildGrammarQuestion(lessons: List<GrammarLessonEntity>): QuizQuestionItem? {
        val lesson = lessons.shuffled().firstOrNull() ?: return null
        val exercises: List<GrammarExerciseItem> = try {
            json.decodeFromString(lesson.exercisesJson)
        } catch (e: Exception) {
            emptyList()
        }
        val ex = exercises.shuffled().firstOrNull() ?: return null
        val exType = ex.type.lowercase().trim()
        val isReorder = exType == "reorder" ||
            (ex.options.size >= 3 && !ex.options.contains(ex.answer) && ex.answer.trim().contains(" "))

        if (isReorder) {
            val wordTokens = if (ex.options.size >= 2) {
                ex.options.shuffled()
            } else {
                ex.answer.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.shuffled()
            }
            return QuizQuestionItem(
                id = UUID.randomUUID().toString(),
                skill = "grammar",
                questionType = "grammar_reorder",
                promptEn = ex.prompt,
                promptAr = "قواعد: ${lesson.titleAr}",
                options = wordTokens,
                answerIndex = 0,
                correctText = ex.answer.trim(),
                explanationAr = ex.explanationAr.ifBlank { "القاعدة من درس: ${lesson.titleAr}" }
            )
        }

        val options = if (ex.options.size >= 2 && ex.options.contains(ex.answer)) {
            ex.options.shuffled()
        } else {
            val otherAnswers = exercises
                .map { it.answer.trim() }
                .filter { it.isNotBlank() && !it.equals(ex.answer.trim(), ignoreCase = true) }
                .distinct()
            val fallbackPool = (otherAnswers + listOf("is", "was", "been", "have", "had", "would")).distinct()
                .filter { !it.equals(ex.answer.trim(), ignoreCase = true) }
                .take(3)
            (listOf(ex.answer) + fallbackPool).shuffled()
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

    private fun buildReadingContextQuestionFromText(text: ReadingTextEntity): QuizQuestionItem? {
        val questionsList: List<ReadingQuestionItem> = try {
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
     * Records quiz performance per skill in `quiz_results` and per question in `attempts`
     * within a single Room transaction.
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
        val resultsToInsert = mutableListOf<QuizResultEntity>()
        val attemptsToInsert = mutableListOf<AttemptEntity>()

        for (q in questions) {
            val userIdx = userAnswers[q.id]
            val isCorrect = userIdx != null && userIdx == q.answerIndex
            attemptsToInsert.add(
                AttemptEntity(
                    activityType = "quiz_question",
                    itemId = "${quizType}:${q.skill}:${q.id}",
                    timestamp = now,
                    sm2Rating = null,
                    isCorrect = isCorrect,
                    score = if (isCorrect) 100f else 0f,
                    deck = null
                )
            )
        }

        for (skill in skills) {
            val skillQuestions = questions.filter { it.skill == skill }
            if (skillQuestions.isNotEmpty()) {
                val correctCount = skillQuestions.count { q ->
                    val userIdx = userAnswers[q.id]
                    userIdx == q.answerIndex
                }
                val accuracy = (correctCount.toFloat() / skillQuestions.size.toFloat()) * 100f
                skillAccuracy[skill] = accuracy

                resultsToInsert.add(
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

        dao.insertQuizResultsWithAttempts(resultsToInsert, attemptsToInsert)
        return skillAccuracy
    }
}
