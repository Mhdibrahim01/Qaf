package com.example.data.plan

import android.content.Context
import com.example.data.local.DailyChecklistEntity
import com.example.data.local.DailyTaskItem
import com.example.data.local.PlanDailyTemplate
import com.example.data.local.PlanStudyGoals
import com.example.data.local.PlanWeekGoal
import com.example.data.local.StudyPlanEntity
import com.example.data.local.StudyPlanJson
import com.example.data.local.StudyPlanProfileEntity
import com.example.data.local.WeakAreasState
import com.example.data.local.WordBooksDao
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

class StudyPlanRepository(
    private val context: Context,
    private val dao: WordBooksDao,
    val timeProvider: TimeProvider = SystemTimeProvider()
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    val profileFlow: Flow<StudyPlanProfileEntity?> = dao.observeStudyPlanProfile()

    suspend fun getOrCreateProfile(): StudyPlanProfileEntity {
        val existing = dao.getStudyPlanProfile()
        if (existing != null) return existing

        val defaultProfile = StudyPlanProfileEntity(
            id = 1,
            level = "B2",
            goal = "general_fluency",
            minutesPerDay = 30,
            studyDaysPerWeek = 6,
            startDate = timeProvider.todayDateString(),
            selectedDecksJson = json.encodeToString(listOf("oxford5000", "oxford_opal")),
            restDaysJson = json.encodeToString(listOf(Calendar.FRIDAY)),
            updatedAt = timeProvider.currentTimeMillis()
        )
        dao.upsertStudyPlanProfile(defaultProfile)
        return defaultProfile
    }

    suspend fun updateProfile(profile: StudyPlanProfileEntity) {
        val stamped = profile.copy(updatedAt = timeProvider.currentTimeMillis())
        dao.upsertStudyPlanProfile(stamped)
        // Automatically scale and update current month plan with new profile settings
        val updatedPlan = loadDefaultPlanScaled(stamped)
        // Refresh today's checklist so uncompleted tasks immediately reflect the new profile settings
        regenerateChecklistForUpdatedPlan(stamped, updatedPlan, timeProvider.todayDateString())
    }

    suspend fun getActiveStudyPlan(month: String = timeProvider.currentMonthString()): StudyPlanEntity {
        val existing = dao.getStudyPlanForMonth(month)
        if (existing != null) return existing

        val profile = getOrCreateProfile()
        return loadDefaultPlanScaled(profile, month)
    }

    fun observeActiveStudyPlan(month: String = timeProvider.currentMonthString()): Flow<StudyPlanEntity?> {
        return dao.observeStudyPlanForMonth(month)
    }

    /**
     * Loads the default plan from assets, adapts themes to the user's goal & level,
     * stamps the target month, and scales the daily template by minutesPerDay and weekly goals by studyDaysPerWeek.
     */
    suspend fun loadDefaultPlanScaled(
        profile: StudyPlanProfileEntity,
        targetMonth: String = timeProvider.currentMonthString()
    ): StudyPlanEntity {
        val rawJsonString = try {
            context.assets.open("default_study_plan_b2.json").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            ""
        }

        val assetPlan = if (rawJsonString.isNotBlank()) {
            try {
                json.decodeFromString<StudyPlanJson>(rawJsonString)
            } catch (e: Exception) {
                createGoalTailoredBasePlan(profile)
            }
        } else {
            createGoalTailoredBasePlan(profile)
        }

        val goalAdaptedBase = adaptBasePlanThemesToGoal(assetPlan, profile)
        val scaled = scalePlan(goalAdaptedBase, profile, targetMonth)
        dao.upsertStudyPlan(scaled)
        return scaled
    }

    fun scalePlan(
        base: StudyPlanJson,
        profile: StudyPlanProfileEntity,
        targetMonth: String
    ): StudyPlanEntity {
        val minutesScale = profile.minutesPerDay / 30.0f
        val daysScale = profile.studyDaysPerWeek / 6.0f

        val scaledTemplate = PlanDailyTemplate(
            review_cards_min = (base.daily_template.review_cards_min * minutesScale).roundToInt().coerceAtLeast(3),
            new_words = (base.daily_template.new_words * minutesScale).roundToInt().coerceIn(5, 20),
            grammar_min = (base.daily_template.grammar_min * minutesScale).roundToInt().coerceAtLeast(3),
            reading_min = (base.daily_template.reading_min * minutesScale).roundToInt().coerceAtLeast(3),
            quiz_min = (base.daily_template.quiz_min * minutesScale).roundToInt().coerceAtLeast(2)
        )

        val scaledWeeks = base.weeks.map { week ->
            val g = week.goals
            val scaledGoals = PlanStudyGoals(
                new_words = (g.new_words * daysScale * minutesScale).roundToInt().coerceAtLeast(10),
                grammar_lessons = (g.grammar_lessons * daysScale).roundToInt().coerceIn(1, 5),
                reading_texts = (g.reading_texts * daysScale).roundToInt().coerceIn(1, 5),
                sentences = (g.sentences * daysScale * minutesScale).roundToInt().coerceAtLeast(5),
                quizzes = (g.quizzes * daysScale).roundToInt().coerceIn(1, 3)
            )
            week.copy(goals = scaledGoals)
        }

        val startLvl = profile.level.ifBlank { "B2" }
        val nextLvl = computeNextLevelTarget(startLvl)

        return StudyPlanEntity(
            month = targetMonth,
            levelStart = startLvl,
            levelTarget = nextLvl,
            weeksJson = json.encodeToString(scaledWeeks),
            dailyTemplateJson = json.encodeToString(scaledTemplate),
            tipsAr = base.tips_ar.ifBlank { "ركز على مراجعة البطاقات المستحقة يومياً قبل البدء في الكلمات الجديدة." },
            source = "default",
            createdAt = timeProvider.currentTimeMillis(),
            updatedAt = timeProvider.currentTimeMillis()
        )
    }

    fun computeNextLevelTarget(level: String): String {
        return when (level.uppercase(Locale.US)) {
            "A1" -> "A2"
            "A2" -> "B1"
            "B1" -> "B2"
            "B2" -> "B2+"
            "C1" -> "C1+"
            else -> "$level+"
        }
    }

    /**
     * Rebuilds today's checklist when profile or monthly plan changes, preserving already-completed tasks
     * while updating uncompleted tasks to match the new level, decks, and daily targets.
     */
    suspend fun regenerateChecklistForUpdatedPlan(
        profile: StudyPlanProfileEntity? = null,
        plan: StudyPlanEntity? = null,
        date: String = timeProvider.todayDateString()
    ): DailyChecklistEntity {
        val resolvedProfile = profile ?: getOrCreateProfile()
        val resolvedPlan = plan ?: getActiveStudyPlan(timeProvider.currentMonthString())
        val existing = dao.getDailyChecklist(date)
        val completedByType = mutableMapOf<String, DailyTaskItem>()
        if (existing != null) {
            try {
                val oldTasks = json.decodeFromString<List<DailyTaskItem>>(existing.tasksJson)
                oldTasks.filter { it.isCompleted }.forEach { completedByType[it.type] = it }
            } catch (_: Exception) {
            }
        }

        // Remove existing today record so getOrCreateDailyChecklist generates fresh tasks
        if (existing != null) {
            val freshTasks = buildDailyTaskList(
                date = date,
                profile = resolvedProfile,
                plan = resolvedPlan,
                isAdjustedAfterMissedDay = existing.isAdjusted,
                daysMissed = 0,
                latestPrevious = null
            ).map { newTask ->
                val prevDone = completedByType[newTask.type]
                if (prevDone != null) {
                    newTask.copy(
                        completedCount = maxOf(prevDone.completedCount, newTask.targetCount),
                        isCompleted = true
                    )
                } else {
                    newTask
                }
            }

            val merged = existing.copy(
                tasksJson = json.encodeToString(freshTasks),
                completedCount = freshTasks.count { it.isCompleted },
                totalCount = freshTasks.size,
                isCompleted = freshTasks.isNotEmpty() && freshTasks.all { it.isCompleted }
            )
            dao.upsertDailyChecklist(merged)
            return refreshChecklistCompletion(merged)
        }

        return getOrCreateDailyChecklist(date)
    }

    /**
     * Returns today's checklist. If it does not exist for the specified date,
     * it generates it once, persists it, and evaluates completion idempotently from the database.
     */
    suspend fun getOrCreateDailyChecklist(
        date: String = timeProvider.todayDateString()
    ): DailyChecklistEntity {
        val existing = dao.getDailyChecklist(date)
        if (existing != null) {
            return refreshChecklistCompletion(existing)
        }

        val profile = getOrCreateProfile()
        val plan = getActiveStudyPlan(timeProvider.currentMonthString())

        // 1. Check for missed days gap
        val allChecklists = dao.getAllDailyChecklists()
        val latestPrevious = allChecklists.firstOrNull { it.date < date }
        var isAdjustedAfterMissedDay = false
        var daysMissed = 0

        if (latestPrevious != null) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val prevTime = sdf.parse(latestPrevious.date)?.time ?: 0L
            val currTime = sdf.parse(date)?.time ?: 0L
            val diffDays = ((currTime - prevTime) / 86_400_000L).toInt()
            if (diffDays > 1) {
                daysMissed = diffDays - 1
                isAdjustedAfterMissedDay = true
            }
        }

        val tasks = buildDailyTaskList(
            date = date,
            profile = profile,
            plan = plan,
            isAdjustedAfterMissedDay = isAdjustedAfterMissedDay,
            daysMissed = daysMissed,
            latestPrevious = latestPrevious
        )

        val checklist = DailyChecklistEntity(
            date = date,
            tasksJson = json.encodeToString(tasks),
            completedCount = tasks.count { it.isCompleted },
            totalCount = tasks.size,
            isCompleted = tasks.isNotEmpty() && tasks.all { it.isCompleted },
            completedAt = null,
            isAdjusted = isAdjustedAfterMissedDay
        )

        dao.upsertDailyChecklist(checklist)
        return refreshChecklistCompletion(checklist)
    }

    private suspend fun buildDailyTaskList(
        date: String,
        profile: StudyPlanProfileEntity,
        plan: StudyPlanEntity,
        isAdjustedAfterMissedDay: Boolean,
        daysMissed: Int,
        latestPrevious: DailyChecklistEntity?
    ): List<DailyTaskItem> {
        val template = try {
            json.decodeFromString<PlanDailyTemplate>(plan.dailyTemplateJson)
        } catch (e: Exception) {
            PlanDailyTemplate()
        }

        val restDays: List<Int> = try {
            json.decodeFromString(profile.restDaysJson)
        } catch (e: Exception) {
            listOf(Calendar.FRIDAY)
        }
        val isRestDay = restDays.contains(timeProvider.dayOfWeek())
        val selectedDecks: List<String> = try {
            json.decodeFromString<List<String>>(profile.selectedDecksJson).ifEmpty {
                listOf("oxford5000", "oxford_opal")
            }
        } catch (e: Exception) {
            listOf("oxford5000", "oxford_opal")
        }
        val primaryDeck = selectedDecks.firstOrNull() ?: "oxford5000"

        val tasks = mutableListOf<DailyTaskItem>()
        val now = timeProvider.currentTimeMillis()

        // Task 1: Flashcards (Capped at 30 cards, oldest due first)
        val dueCount = dao.getDueCardsCount(now)
        val flashcardTarget = minOf(dueCount, 30)
        tasks.add(
            DailyTaskItem(
                id = "task_flashcards",
                type = "flashcards",
                titleEn = if (flashcardTarget > 0) "Review Due Flashcards" else "Flashcards Review (All Caught Up)",
                titleAr = if (flashcardTarget > 0) "مراجعة البطاقات المستحقة ($flashcardTarget بطاقة)" else "البطاقات التعليمية (تمت مراجعة الجميع)",
                subtitleEn = if (flashcardTarget > 0) "$flashcardTarget cards due today • ${template.review_cards_min} min" else "No cards due • Tap to practice $primaryDeck",
                targetCount = flashcardTarget,
                completedCount = 0,
                isCompleted = (flashcardTarget == 0),
                linkedRoute = "flashcards",
                linkedId = primaryDeck,
                explanationAr = "التكرار المتباعد يحفظ الكلمات في الذاكرة طويلة المدى."
            )
        )

        // Task 2: New Words
        val newWordsTarget = if (isRestDay) {
            0
        } else if (isAdjustedAfterMissedDay) {
            // Missed day: reduce new words to 5 for single day only, without guilt
            5
        } else {
            template.new_words
        }

        if (newWordsTarget > 0) {
            val preferredLevels = levelsForUserProfile(profile.level)
            val candidateWords = dao.getNewWordsForLevelsAndDecks(preferredLevels, selectedDecks, newWordsTarget)
                .ifEmpty { dao.getNewWordsForLevelsAndDecks(listOf("A1", "A2", "B1", "B2", "C1"), selectedDecks, newWordsTarget) }
            val samplePreview = candidateWords.take(3).joinToString(", ") { it.text }

            tasks.add(
                DailyTaskItem(
                    id = "task_new_words",
                    type = "new_words",
                    titleEn = "Learn $newWordsTarget New Words (${profile.level})",
                    titleAr = "تعلم $newWordsTarget مفردات جديدة (مستوى ${profile.level})",
                    subtitleEn = if (samplePreview.isNotBlank()) "Focus: $samplePreview" else "Deck: ${formatDeckLabel(primaryDeck)}",
                    targetCount = newWordsTarget,
                    completedCount = 0,
                    isCompleted = false,
                    linkedRoute = "word_books",
                    linkedId = "$primaryDeck|${profile.level}",
                    explanationAr = if (isAdjustedAfterMissedDay) {
                        "أهلاً بعودتك! خففنا كلمات اليوم لتستأنف التعلم بسلاسة وبدون أي ضغط."
                    } else {
                        "دراسة كلمات جديدة تناسب مستواك ${profile.level} مع أمثلتها."
                    }
                )
            )
        }

        // Task 3 & 4: Grammar Lesson & Linked Reading Text Pairing
        val currentWeekGoals = try {
            val weeks = json.decodeFromString<List<PlanWeekGoal>>(plan.weeksJson)
            weeks.firstOrNull()?.goals ?: PlanStudyGoals()
        } catch (e: Exception) {
            PlanStudyGoals()
        }

        val studyDays = profile.studyDaysPerWeek.coerceIn(1, 7)
        val grammarTargetPerWeek = currentWeekGoals.grammar_lessons.coerceIn(1, studyDays)
        val shouldScheduleGrammarToday = !isRestDay && shouldScheduleOnDay(timeProvider.dayOfWeek(), grammarTargetPerWeek, studyDays)

        if (shouldScheduleGrammarToday) {
            val uncompletedLessons = dao.getUncompletedGrammarLessonsForLevel(profile.level)
            val targetLesson = uncompletedLessons.firstOrNull()
                ?: dao.getUncompletedGrammarLessonsForLevel("B2").firstOrNull()
                ?: dao.getUncompletedGrammarLessonsForLevel("C1").firstOrNull()
                ?: dao.getAllGrammarLessons().firstOrNull { !it.isCompleted }
                ?: dao.getAllGrammarLessons().firstOrNull()

            if (targetLesson != null) {
                tasks.add(
                    DailyTaskItem(
                        id = "task_grammar_${targetLesson.id}",
                        type = "grammar",
                        titleEn = "Grammar: ${targetLesson.titleEn}",
                        titleAr = "قواعد: ${targetLesson.titleAr}",
                        subtitleEn = "Topic: ${targetLesson.topic} • ${targetLesson.level} (${template.grammar_min} min)",
                        targetCount = 1,
                        completedCount = if (targetLesson.isCompleted) 1 else 0,
                        isCompleted = targetLesson.isCompleted,
                        linkedRoute = "grammar",
                        linkedId = targetLesson.id,
                        explanationAr = "دراسة درس القواعد وحل تمارينه التفاعلية."
                    )
                )

                // Pair reading text with linked grammar lesson (via grammar_ids)
                val allTexts = dao.getAllReadingTexts()
                val pairedText = allTexts.firstOrNull { it.grammarIdsJson.contains(targetLesson.id) }
                    ?: dao.getUncompletedReadingTextsForLevel(profile.level).firstOrNull()
                    ?: allTexts.firstOrNull { it.level == profile.level }
                    ?: allTexts.firstOrNull()

                if (pairedText != null) {
                    val readingProgress = dao.getReadingProgress(pairedText.id)
                    val isReadingDone = readingProgress?.isCompleted == true
                    tasks.add(
                        DailyTaskItem(
                            id = "task_reading_${pairedText.id}",
                            type = "reading",
                            titleEn = "Reading: ${pairedText.titleEn}",
                            titleAr = "قراءة: ${pairedText.titleAr}",
                            subtitleEn = "Level: ${pairedText.level} • Paired with ${targetLesson.titleEn} (${template.reading_min} min)",
                            targetCount = 1,
                            completedCount = if (isReadingDone) 1 else 0,
                            isCompleted = isReadingDone,
                            linkedRoute = "reading",
                            linkedId = pairedText.id,
                            explanationAr = "قراءة النص في سياق درس القواعد والإجابة على أسئلة الفهم."
                        )
                    )
                }
            }
        }

        // Task 5: 5 Sentences to Review or Dictation
        if (!isRestDay) {
            tasks.add(
                DailyTaskItem(
                    id = "task_sentences",
                    type = "sentences",
                    titleEn = "Practice 5 Sentences",
                    titleAr = "ممارسة 5 جمل (إملاء أو تحدث)",
                    subtitleEn = "Dictation or speaking practice",
                    targetCount = 5,
                    completedCount = 0,
                    isCompleted = false,
                    linkedRoute = "sentences",
                    explanationAr = "الاستماع لجمل حقيقية وكتابتها بالإملاء يعزز الطلاقة."
                )
            )
        }

        // Task 6: Due Correction Notebook Reviews (if any are due)
        val dueCorrections = dao.getDueCorrectionReviewsCount(now)
        if (dueCorrections > 0) {
            tasks.add(
                DailyTaskItem(
                    id = "task_correction_reviews",
                    type = "correction_reviews",
                    titleEn = "Correction Notebook ($dueCorrections Due)",
                    titleAr = "دفتر التصحيحات ($dueCorrections جمل مستحقة)",
                    subtitleEn = "Spaced rewrite of your personal sentences",
                    targetCount = dueCorrections,
                    completedCount = 0,
                    isCompleted = false,
                    linkedRoute = "correction_notebook",
                    explanationAr = "إعادة كتابة الجمل المصححة في مواقف جديدة لترسيخ القاعدة."
                )
            )
        }

        // Task 7: Daily Mini-Quiz (5 questions)
        tasks.add(
            DailyTaskItem(
                id = "task_daily_quiz",
                type = "quiz",
                titleEn = "Daily Mini-Quiz (5 Questions)",
                titleAr = "الاختبار اليومي السريع (5 أسئلة)",
                subtitleEn = "Vocabulary, Grammar, Reading & Listening (${template.quiz_min} min)",
                targetCount = 5,
                completedCount = 0,
                isCompleted = false,
                linkedRoute = "quiz",
                explanationAr = "اختبار قصير يقيس تقدمك في المهارات الأربع."
            )
        )

        // Carried unfinished tasks from within the last 2 days
        if (daysMissed in 1..2 && latestPrevious != null) {
            try {
                val prevTasks = json.decodeFromString<List<DailyTaskItem>>(latestPrevious.tasksJson)
                val uncompletedPrev = prevTasks.filter { !it.isCompleted && it.type != "quiz" && tasks.none { t -> t.id == it.id } }
                tasks.addAll(uncompletedPrev.take(2))
            } catch (e: Exception) {
                // Ignore parse errors
            }
        }

        return tasks
    }

    /**
     * Re-evaluates checklist completion idempotently directly from DB records.
     */
    suspend fun refreshChecklistCompletion(checklist: DailyChecklistEntity): DailyChecklistEntity {
        val tasks = try {
            json.decodeFromString<List<DailyTaskItem>>(checklist.tasksJson)
        } catch (e: Exception) {
            return checklist
        }

        val startOfDay = timeProvider.startOfDayMillis(checklist.date)
        val now = timeProvider.currentTimeMillis()
        val cardsRatedToday = dao.getCardsRatedTodayCount(startOfDay)
        val newWordsRatedToday = dao.getNewWordsRatedTodayCount(startOfDay)
        val quizResultsToday = dao.getQuizResultsForDate(checklist.date)
        val dueCorrectionCount = dao.getDueCorrectionReviewsCount(now)

        val updatedTasks = tasks.map { task ->
            when (task.type) {
                "flashcards" -> {
                    val isDone = task.isCompleted || (task.targetCount == 0) || (cardsRatedToday >= task.targetCount)
                    task.copy(
                        completedCount = if (isDone) task.targetCount else minOf(cardsRatedToday, task.targetCount),
                        isCompleted = isDone
                    )
                }
                "new_words" -> {
                    val isDone = task.isCompleted || (task.targetCount == 0) || (newWordsRatedToday >= task.targetCount)
                    task.copy(
                        completedCount = if (isDone) task.targetCount else minOf(newWordsRatedToday, task.targetCount),
                        isCompleted = isDone
                    )
                }
                "grammar" -> {
                    val lesson = dao.getGrammarLessonById(task.linkedId)
                    val progress = dao.getGrammarProgress(task.linkedId)
                    val isDone = task.isCompleted || (progress?.isCompleted == true) || (lesson?.isCompleted == true)
                    task.copy(
                        completedCount = if (isDone) 1 else 0,
                        isCompleted = isDone
                    )
                }
                "reading" -> {
                    val progress = dao.getReadingProgress(task.linkedId)
                    val isDone = task.isCompleted || (progress?.isCompleted == true)
                    task.copy(
                        completedCount = if (isDone) 1 else 0,
                        isCompleted = isDone
                    )
                }
                "sentences" -> task
                "correction_reviews" -> {
                    val isDone = task.isCompleted || (dueCorrectionCount == 0)
                    task.copy(
                        completedCount = if (isDone) task.targetCount else (task.targetCount - dueCorrectionCount).coerceAtLeast(0),
                        isCompleted = isDone
                    )
                }
                "quiz" -> {
                    val isDone = task.isCompleted || quizResultsToday.isNotEmpty()
                    task.copy(
                        completedCount = if (isDone) task.targetCount else 0,
                        isCompleted = isDone
                    )
                }
                else -> task
            }
        }

        val completedCount = updatedTasks.count { it.isCompleted }
        val allDone = updatedTasks.isNotEmpty() && updatedTasks.all { it.isCompleted }
        val updatedChecklist = checklist.copy(
            tasksJson = json.encodeToString(updatedTasks),
            completedCount = completedCount,
            totalCount = updatedTasks.size,
            isCompleted = allDone,
            completedAt = if (allDone) checklist.completedAt ?: timeProvider.currentTimeMillis() else null
        )

        dao.upsertDailyChecklist(updatedChecklist)
        return updatedChecklist
    }

    /**
     * Toggles a task's manual completion state so users can both check and uncheck tasks.
     */
    suspend fun markTaskCompletedManually(date: String, taskId: String): DailyChecklistEntity {
        val checklist = getOrCreateDailyChecklist(date)
        val tasks = try {
            json.decodeFromString<List<DailyTaskItem>>(checklist.tasksJson)
        } catch (e: Exception) {
            emptyList()
        }

        val updated = tasks.map { task ->
            if (task.id == taskId) {
                val nextCompleted = !task.isCompleted
                task.copy(
                    isCompleted = nextCompleted,
                    completedCount = if (nextCompleted) task.targetCount.coerceAtLeast(1) else 0
                )
            } else {
                task
            }
        }
        val allDone = updated.isNotEmpty() && updated.all { it.isCompleted }
        val entity = checklist.copy(
            tasksJson = json.encodeToString(updated),
            completedCount = updated.count { it.isCompleted },
            isCompleted = allDone,
            completedAt = if (allDone) timeProvider.currentTimeMillis() else null
        )
        dao.upsertDailyChecklist(entity)
        return entity
    }

    /**
     * Computes consecutive completed study days, skipping scheduled rest days.
     */
    suspend fun computeStreak(): Int {
        val all = dao.getAllDailyChecklists().sortedByDescending { it.date }
        if (all.isEmpty()) return 0

        val profile = getOrCreateProfile()
        val restDays: List<Int> = try {
            json.decodeFromString(profile.restDaysJson)
        } catch (e: Exception) {
            listOf(Calendar.FRIDAY)
        }

        var streak = 0
        val cal = Calendar.getInstance()
        cal.timeInMillis = timeProvider.currentTimeMillis()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        // Check today
        val todayStr = sdf.format(cal.time)
        val todayChecklist = all.find { it.date == todayStr }

        if (todayChecklist?.isCompleted == true) {
            streak++
            cal.add(Calendar.DAY_OF_YEAR, -1)
        } else {
            // If today is not completed yet, streak can still be alive from yesterday
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }

        var missedGraceDays = 0
        for (i in 0..60) {
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            val dateStr = sdf.format(cal.time)
            val check = all.find { it.date == dateStr }

            if (check?.isCompleted == true) {
                streak++
            } else if (restDays.contains(dayOfWeek)) {
                // Rest day: does not break streak!
            } else {
                missedGraceDays++
                if (missedGraceDays > 2) {
                    break
                }
            }
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }

        return streak
    }

    suspend fun getWeakAreas(limit: Int = 10): WeakAreasState {
        val profile = getOrCreateProfile()
        val weakestWords = dao.getWeakestWords(limit)
        val uncompletedLessons = dao.getUncompletedGrammarLessonsForLevel(profile.level)
            .ifEmpty { dao.getUncompletedGrammarLessonsForLevel("B2") }
            .ifEmpty { dao.getAllGrammarLessons().filter { !it.isCompleted } }
        return WeakAreasState(
            weakWords = weakestWords,
            weakGrammarLessons = uncompletedLessons.take(5)
        )
    }

    private fun levelsForUserProfile(level: String): List<String> {
        return when (level.uppercase(Locale.US)) {
            "A1" -> listOf("A1", "A2")
            "A2" -> listOf("A2", "A1", "B1")
            "B1" -> listOf("B1", "A2", "B2")
            "B2" -> listOf("B2", "B1", "C1")
            "C1" -> listOf("C1", "B2")
            else -> listOf("B1", "B2", "C1")
        }
    }

    private fun formatDeckLabel(deckId: String): String {
        return when (deckId) {
            "oxford5000" -> "Oxford 5000"
            "oxford_opal" -> "Oxford OPAL"
            "oxford_phrases" -> "Oxford Phrases"
            "everyday_expressions" -> "Everyday Expressions"
            "essential_idioms" -> "Essential Idioms"
            "ielts_core" -> "IELTS Core"
            "it_industry" -> "IT & Tech Industry"
            "cefr_c1" -> "CEFR C1 Advanced"
            "pte_academic" -> "PTE Academic"
            "business_english" -> "Business English"
            "career_interview" -> "Job Interview & Career"
            "medical_healthcare" -> "Medical & Healthcare"
            else -> deckId
        }
    }

    private fun shouldScheduleOnDay(dayOfWeek: Int, targetPerWeek: Int, studyDays: Int): Boolean {
        return when (targetPerWeek) {
            1 -> dayOfWeek == Calendar.MONDAY
            2 -> dayOfWeek == Calendar.MONDAY || dayOfWeek == Calendar.THURSDAY
            3 -> dayOfWeek == Calendar.MONDAY || dayOfWeek == Calendar.WEDNESDAY || dayOfWeek == Calendar.SATURDAY
            4 -> dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.MONDAY || dayOfWeek == Calendar.WEDNESDAY || dayOfWeek == Calendar.THURSDAY
            else -> true
        }
    }

    private fun adaptBasePlanThemesToGoal(base: StudyPlanJson, profile: StudyPlanProfileEntity): StudyPlanJson {
        if (profile.goal == "general_fluency" && profile.level == "B2") {
            return base
        }
        val custom = createGoalTailoredBasePlan(profile)
        // Keep base daily_template numbers so scaling math remains identical
        return custom.copy(daily_template = base.daily_template)
    }

    fun createGoalTailoredBasePlan(profile: StudyPlanProfileEntity): StudyPlanJson {
        val lvl = profile.level.ifBlank { "B2" }
        val nextLvl = computeNextLevelTarget(lvl)
        val baseGoals = PlanStudyGoals(50, 3, 3, 20, 2)

        val (weeks, tipsAr) = when (profile.goal) {
            "ielts" -> Pair(
                listOf(
                    PlanWeekGoal(1, "IELTS Academic Vocabulary & Complex Clauses ($lvl)", "المفردات الأكاديمية للآيلتس والجمل المركبة ($lvl)", baseGoals),
                    PlanWeekGoal(2, "Reading Skimming, Scanning & Passive Structures", "مهارات القراءة السريعة والمبني للمجهول الأكاديمي", baseGoals),
                    PlanWeekGoal(3, "Task 2 Essay Connectors, Hedging & Cohesion", "روابط المقال الأكاديمي وأساليب الترابط المنطقي", baseGoals),
                    PlanWeekGoal(4, "Speaking Fluency, Idiomatic Precision & Mock Review", "طلاقة التحدث والدقة الاصطلاحية والمراجعة الشاملة", baseGoals)
                ),
                "ركز على مفردات القوائم الأكاديمية (OPAL) واستخدم الروابط المنطقية في كتابة الجمل اليومية لرفع درجتك في الآيلتس."
            )
            "it_career" -> Pair(
                listOf(
                    PlanWeekGoal(1, "Software Engineering & System Design Terminology ($lvl)", "مصطلحات هندسة البرمجيات وتصميم الأنظمة ($lvl)", baseGoals),
                    PlanWeekGoal(2, "Standups, Code Reviews & Cause-Effect Grammar", "اجتماعات الفرق التقنية ومراجعة الكود وصيغ السبب والنتيجة", baseGoals),
                    PlanWeekGoal(3, "Technical Documentation, RFCs & Modal Precision", "التوثيق التقني الدقيق وأفعال الاحتمال والالتزام", baseGoals),
                    PlanWeekGoal(4, "Tech Interviews, Incident Reports & Executive Summaries", "المقابلات التقنية وتقارير الأعطال والتلخيص الاحترافي", baseGoals)
                ),
                "تدرب على صياغة جمل واضحة ومختصرة تشرح المشاكل التقنية والحلول البرمجية بدقة."
            )
            else -> Pair(
                listOf(
                    PlanWeekGoal(1, "Core Communication & Conditionals in Context ($lvl)", "التواصل اليومي والمهني وجمل الشرط في السياق ($lvl)", baseGoals),
                    PlanWeekGoal(2, "Natural Collocations, Phrasal Verbs & Passive Nuances", "المتلازمات اللفظية والأفعال المركبة والمبني للمجهول", baseGoals),
                    PlanWeekGoal(3, "Narrative Fluency, Storytelling & Idiomatic Expressions", "الطلاقة السردية والتعبيرات الاصطلاحية الطبيعية", baseGoals),
                    PlanWeekGoal(4, "Active Writing, Listening Precision & Monthly Mastery", "الكتابة الفعالة ودقة الاستماع والمراجعة الشاملة", baseGoals)
                ),
                "ركز على ممارسة المفردات الجديدة في جمل سياقية كاملة، وراجع البطاقات المستحقة يومياً قبل البدء في الكلمات الجديدة."
            )
        }

        return StudyPlanJson(
            month = timeProvider.currentMonthString(),
            level_start = lvl,
            level_target = nextLvl,
            weeks = weeks,
            daily_template = PlanDailyTemplate(8, 10, 7, 5, 3),
            tips_ar = tipsAr
        )
    }
}
