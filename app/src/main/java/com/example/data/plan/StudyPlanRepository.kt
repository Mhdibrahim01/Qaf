package com.example.data.plan

import android.content.Context
import com.example.data.local.DailyChecklistEntity
import com.example.data.local.DailyTaskItem
import com.example.data.local.EntryEntity
import com.example.data.local.GrammarLessonEntity
import com.example.data.local.PlanDailyTemplate
import com.example.data.local.PlanStudyGoals
import com.example.data.local.PlanWeekGoal
import com.example.data.local.PracticeAccuracySummary
import com.example.data.local.ReadingTextEntity
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

    val learnerModelRepository = LearnerModelRepository(context, dao, timeProvider)

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

    /**
     * Maps the onboarding goal to default vocabulary focus decks.
     */
    fun defaultDecksForGoal(goal: String): List<String> {
        return when (goal.trim().lowercase(Locale.US)) {
            "ielts" -> listOf("oxford5000", "oxford_opal", "ielts_core")
            "it_career" -> listOf("oxford5000", "oxford_opal", "it_industry")
            "medical" -> listOf("oxford5000", "oxford_opal", "medical_healthcare")
            else -> listOf("oxford5000", "oxford_opal")
        }
    }

    /**
     * Maps studyDaysPerWeek (1..7) to default rest day Calendar constants (Friday = 6 by default for 6 days/week).
     */
    fun defaultRestDaysForStudyDays(studyDaysPerWeek: Int): List<Int> {
        return when (studyDaysPerWeek.coerceIn(1, 7)) {
            7 -> emptyList()
            6 -> listOf(Calendar.FRIDAY)
            5 -> listOf(Calendar.FRIDAY, Calendar.SATURDAY)
            4 -> listOf(Calendar.TUESDAY, Calendar.FRIDAY, Calendar.SATURDAY)
            3 -> listOf(Calendar.SUNDAY, Calendar.TUESDAY, Calendar.THURSDAY, Calendar.FRIDAY)
            2 -> listOf(Calendar.SATURDAY, Calendar.SUNDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.FRIDAY)
            else -> listOf(
                Calendar.SATURDAY,
                Calendar.SUNDAY,
                Calendar.TUESDAY,
                Calendar.WEDNESDAY,
                Calendar.THURSDAY,
                Calendar.FRIDAY
            )
        }
    }

    /**
     * Persists the user's Step 3 onboarding choices into `study_plan_profile`, scales and saves
     * the monthly plan (`study_plans`) from `default_study_plan_b2.json`, and generates/updates
     * today's 5-pillar checklist (`daily_checklists`) without deleting any user progress.
     */
    suspend fun saveOnboardingGoalAndGeneratePlan(
        level: String,
        goal: String,
        minutesPerDay: Int,
        studyDaysPerWeek: Int,
        preferredTopics: List<String>? = null,
        quietModeEnabled: Boolean? = null
    ): DailyChecklistEntity {
        val cleanLevel = level.trim().uppercase(Locale.US).ifBlank { "B2" }
        val cleanGoal = goal.trim().lowercase(Locale.US).ifBlank { "general_fluency" }
        val cleanMinutes = minutesPerDay.coerceIn(15, 60)
        val cleanDays = studyDaysPerWeek.coerceIn(1, 7)

        val existing = dao.getStudyPlanProfile()
        val decksJson = if (existing != null && existing.goal == cleanGoal && existing.selectedDecksJson.isNotBlank()) {
            existing.selectedDecksJson
        } else {
            json.encodeToString(defaultDecksForGoal(cleanGoal))
        }
        val restDaysJson = if (existing != null && existing.studyDaysPerWeek == cleanDays && existing.restDaysJson.isNotBlank()) {
            existing.restDaysJson
        } else {
            json.encodeToString(defaultRestDaysForStudyDays(cleanDays))
        }
        val topicsJson = when {
            preferredTopics != null -> json.encodeToString(preferredTopics.map { AdaptiveConfig.normalizeMacroTopic(it) }.distinct().take(3))
            existing != null && existing.preferredTopicsJson.isNotBlank() && existing.preferredTopicsJson != "[]" -> existing.preferredTopicsJson
            else -> "[]"
        }
        val resolvedQuietMode = quietModeEnabled ?: existing?.quietModeEnabled ?: false

        val profile = (existing ?: StudyPlanProfileEntity(id = 1)).copy(
            level = cleanLevel,
            goal = cleanGoal,
            minutesPerDay = cleanMinutes,
            studyDaysPerWeek = cleanDays,
            startDate = existing?.startDate?.ifBlank { timeProvider.todayDateString() } ?: timeProvider.todayDateString(),
            selectedDecksJson = decksJson,
            restDaysJson = restDaysJson,
            preferredTopicsJson = topicsJson,
            quietModeEnabled = resolvedQuietMode,
            updatedAt = timeProvider.currentTimeMillis()
        )
        dao.upsertStudyPlanProfile(profile)
        val updatedPlan = loadDefaultPlanScaled(profile)
        return regenerateChecklistForUpdatedPlan(profile, updatedPlan, timeProvider.todayDateString())
    }

    /**
     * Updates the user's selected study decks and placement preference in `study_plan_profile`,
     * scales the monthly plan, and regenerates today's checklist tasks.
     */
    suspend fun saveOnboardingDecksAndPlacement(
        selectedDecks: List<String>,
        startAtUserLevel: Boolean
    ): DailyChecklistEntity {
        val existing = getOrCreateProfile()
        val safeDecks = selectedDecks.ifEmpty { listOf("oxford5000", "oxford_opal") }
        val updated = existing.copy(
            selectedDecksJson = json.encodeToString(safeDecks),
            startAtUserLevel = startAtUserLevel,
            updatedAt = timeProvider.currentTimeMillis()
        )
        dao.upsertStudyPlanProfile(updated)
        val updatedPlan = loadDefaultPlanScaled(updated)
        return regenerateChecklistForUpdatedPlan(updated, updatedPlan, timeProvider.todayDateString())
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
        val targetDayOfWeek = try {
            val cal = Calendar.getInstance()
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            sdf.parse(date)?.let { cal.time = it; cal.get(Calendar.DAY_OF_WEEK) } ?: timeProvider.dayOfWeek()
        } catch (_: Exception) {
            timeProvider.dayOfWeek()
        }
        val isRestDay = restDays.contains(targetDayOfWeek)
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
        val dueTodayCutoff = timeProvider.dueTodayCutoff(now)
        val dueNowCutoff = timeProvider.dueNowCutoff(now)

        val minutes = profile.minutesPerDay
        val deckNames = selectedDecks.map { formatDeckLabel(it) }.joinToString(" & ")

        // Priority 1: Due Flashcards Review (Time budget: ~10s / 0.17 min per card)
        val dueNow = if (selectedDecks.isNotEmpty()) {
            dao.getDueNowCardsCountForDecks(dueNowCutoff, selectedDecks)
        } else {
            dao.getDueNowCardsCount(dueNowCutoff)
        }
        val dueToday = if (selectedDecks.isNotEmpty()) {
            dao.getReviewDueTodayCountForDecks(dueTodayCutoff, selectedDecks)
        } else {
            dao.getReviewDueTodayCount(dueTodayCutoff)
        }
        val waitingLaterToday = maxOf(0, dueToday - dueNow)
        val totalDue = maxOf(dueToday, dueNow)

        val maxScheduledReviewsCap = minOf(
            AdaptiveConfig.OVERDUE_CAP_MAX,
            when {
                minutes <= 15 -> 25
                minutes <= 30 -> 50
                minutes <= 45 -> 75
                else -> 100
            }
        )

        val flashcardTarget: Int
        val backlogRemaining: Int
        if (totalDue > maxScheduledReviewsCap) {
            flashcardTarget = maxScheduledReviewsCap
            backlogRemaining = totalDue - maxScheduledReviewsCap
        } else if (totalDue > 0) {
            flashcardTarget = totalDue
            backlogRemaining = 0
        } else {
            flashcardTarget = if (isRestDay) 5 else when {
                minutes <= 15 -> 5
                minutes <= 30 -> 10
                minutes <= 45 -> 15
                else -> 20
            }
            backlogRemaining = 0
        }

        val flashcardMinutes = maxOf(1, (flashcardTarget * 0.17f).roundToInt())
        val flashcardReason = if (backlogRemaining > 0) {
            "$flashcardTarget of $totalDue due reviews scheduled today to fit your ${minutes}-min goal ($backlogRemaining remaining in backlog)"
        } else if (totalDue > 0) {
            if (waitingLaterToday > 0) "$dueNow due now + $waitingLaterToday later today in $deckNames (~$flashcardMinutes min)"
            else "$dueNow due now in $deckNames (~$flashcardMinutes min)"
        } else {
            "Daily retention practice for $deckNames (~$flashcardMinutes min)"
        }

        tasks.add(
            DailyTaskItem(
                id = "task_flashcards",
                type = "flashcards",
                titleEn = if (isRestDay) "Flashcards (Rest Day Light Review)" else if (totalDue > 0) "Review Due Flashcards" else "Flashcards Review Practice",
                titleAr = if (isRestDay) "مراجعة بطاقات خفيفة ليوم الراحة" else if (totalDue > 0) "مراجعة البطاقات المستحقة ($flashcardTarget بطاقة)" else "مراجعة البطاقات التعليمية ($flashcardTarget بطاقات)",
                subtitleEn = if (backlogRemaining > 0) "$flashcardTarget of $totalDue due reviews scheduled today to fit your ${minutes}-min goal ($backlogRemaining remaining in backlog)" else if (totalDue > 0) "$flashcardTarget cards due today • $flashcardMinutes min" else "$flashcardTarget practice cards • Tap to review ${formatDeckLabel(primaryDeck)}",
                targetCount = flashcardTarget,
                completedCount = 0,
                isCompleted = false,
                linkedRoute = "flashcards",
                linkedId = primaryDeck,
                explanationAr = "التكرار المتباعد يحفظ الكلمات في الذاكرة طويلة المدى.",
                reason = flashcardReason,
                estimatedMinutes = flashcardMinutes,
                backlogRemaining = backlogRemaining
            )
        )

        // Priority 2: Due Correction Reviews (Time budget: ~20s / 0.33 min per item)
        val dueCorrections = dao.getDueCorrectionReviewsCount(now)
        if (dueCorrections > 0) {
            val correctionMinutes = maxOf(1, (dueCorrections * 0.33f).roundToInt())
            val correctionReason = "$dueCorrections saved sentence corrections are due for review today"
            tasks.add(
                DailyTaskItem(
                    id = "task_correction_reviews",
                    type = "correction_reviews",
                    titleEn = "Review Due Sentence Corrections",
                    titleAr = "مراجعة تصحيحات الجمل المستحقة ($dueCorrections)",
                    subtitleEn = "$dueCorrections corrections due for review • ~$correctionMinutes min",
                    targetCount = dueCorrections,
                    completedCount = 0,
                    isCompleted = false,
                    linkedRoute = "sentences",
                    linkedId = "correction_notebook",
                    explanationAr = "مراجعة الجمل التي قمت بتصحيحها لترسيخ الصياغة السليمة في الذاكرة.",
                    reason = correctionReason,
                    estimatedMinutes = correctionMinutes,
                    backlogRemaining = 0
                )
            )
        }

        // Priority 3: New Words (scaled by minutes per day, deck-weighted, closeness-ordered, deterministic)
        val baseNewWords = when {
            minutes <= 15 -> AdaptiveConfig.NEW_WORDS_MIN
            minutes <= 30 -> 10
            minutes <= 45 -> 15
            else -> AdaptiveConfig.NEW_WORDS_MAX
        }
        val isReviewsHeavy = (backlogRemaining > 0 || totalDue >= AdaptiveConfig.BACKLOG_HEAVY_THRESHOLD)
        val sevenDaysAgo = now - 7L * 86_400_000L
        val recentAttempts = dao.getAttemptsInRange(sevenDaysAgo, now)
        val gradedAttempts = recentAttempts.filter { it.isCorrect != null }
        val rolling7dAccuracy = if (gradedAttempts.size >= 10) {
            gradedAttempts.count { it.isCorrect == true }.toFloat() / gradedAttempts.size.toFloat()
        } else null

        val newWordsTarget = when {
            isRestDay -> 3
            isReviewsHeavy -> maxOf(3, (baseNewWords * 0.6f).roundToInt())
            isAdjustedAfterMissedDay -> maxOf(3, baseNewWords / 2)
            rolling7dAccuracy != null && rolling7dAccuracy >= AdaptiveConfig.ACCURACY_FAST_TRACK_THRESHOLD ->
                minOf(AdaptiveConfig.NEW_WORDS_MAX, baseNewWords + 3)
            else -> baseNewWords
        }.coerceIn(3, AdaptiveConfig.NEW_WORDS_MAX)

        // Resolve Grammar & Anchor Story first so we can pre-teach story target words in Today's New Words
        val closenessSearch = levelsByCloseness(profile.level, profile.startAtUserLevel)
        val allCefrLevels = listOf("A1", "A2", "B1", "B2", "C1")
        val searchLevels = (closenessSearch + allCefrLevels).distinct()

        val grammarTime = when { minutes <= 15 -> 4; minutes <= 30 -> 7; minutes <= 45 -> 10; else -> 15 }
        val readingTime = when { minutes <= 15 -> 3; minutes <= 30 -> 5; minutes <= 45 -> 8; else -> 12 }
        val exerciseCount = when { minutes <= 15 -> 4; minutes <= 30 -> 6; minutes <= 45 -> 8; else -> 10 }

        // Check for Grammar Remediation (<65% score or >=3 active Correction Notebook items)
        var targetLesson: GrammarLessonEntity? = null
        var isGrammarRefresher = false
        var grammarRefresherReason: String? = null
        val allGrammarProgressMap = dao.getAllGrammarProgressList().associateBy { it.lessonId }
        val activeNotebookByLesson = dao.getAllCorrectionEntriesList()
            .filter { it.status != "mastered" && it.grammarLessonId.isNotBlank() }
            .groupBy { it.grammarLessonId }

        val levelGrammarLessons = dao.getGrammarLessonsByLevel(profile.level)
        val prevHadGrammarRefresher = latestPrevious?.let { prev ->
            try {
                val prevTasks = json.decodeFromString<List<DailyTaskItem>>(prev.tasksJson)
                prevTasks.any { it.type == "grammar" && it.reason.contains("Refresher", ignoreCase = true) }
            } catch (_: Exception) { false }
        } ?: false

        if (!prevHadGrammarRefresher) {
            val cooldownCutoff = now - AdaptiveConfig.GRAMMAR_REFRESHER_COOLDOWN_DAYS * 86_400_000L
            val allPastChecklists = dao.getAllDailyChecklists().filter { it.date != date }
            val recentlyRefreshedLessonIds = allPastChecklists.filter { ch ->
                val chStart = timeProvider.startOfDayMillis(ch.date)
                chStart >= cooldownCutoff
            }.flatMap { ch ->
                try {
                    val chTasks = json.decodeFromString<List<DailyTaskItem>>(ch.tasksJson)
                    chTasks.filter { it.type == "grammar" && it.reason.contains("Refresher", ignoreCase = true) }
                        .mapNotNull { it.linkedId }
                } catch (_: Exception) { emptyList() }
            }.toSet()

            for (lesson in levelGrammarLessons) {
                val gp = allGrammarProgressMap[lesson.id]
                val activeNotebookCount = activeNotebookByLesson[lesson.id]?.size ?: 0
                val isCoolingDown = recentlyRefreshedLessonIds.contains(lesson.id)
                if (!isCoolingDown) {
                    val pct = if (gp != null && gp.totalExercises > 0 && gp.bestScore <= gp.totalExercises) {
                        (gp.bestScore * 100) / gp.totalExercises
                    } else {
                        gp?.bestScore ?: 0
                    }
                    if (gp != null && gp.isCompleted && pct in 1 until AdaptiveConfig.GRAMMAR_REMEDIATION_SCORE_THRESHOLD) {
                        targetLesson = lesson
                        isGrammarRefresher = true
                        grammarRefresherReason = "Refresher for ${lesson.level} lesson ${lesson.id} (score $pct% < 65% • ~$grammarTime min)"
                        break
                    } else if (activeNotebookCount >= AdaptiveConfig.GRAMMAR_REMEDIATION_NOTEBOOK_THRESHOLD) {
                        targetLesson = lesson
                        isGrammarRefresher = true
                        grammarRefresherReason = "Refresher for ${lesson.level} lesson ${lesson.id} ($activeNotebookCount active corrections • ~$grammarTime min)"
                        break
                    }
                }
            }
        }

        if (targetLesson == null) {
            for (lvl in searchLevels) {
                val uncompleted = dao.getUncompletedGrammarLessonsForLevel(lvl)
                if (uncompleted.isNotEmpty()) {
                    targetLesson = uncompleted.first()
                    break
                }
            }
        }
        val isGrammarExhausted = targetLesson == null
        if (targetLesson == null) {
            targetLesson = dao.getLowestScoringGrammarLessons(1).firstOrNull()
                ?: dao.getAllGrammarLessons().firstOrNull()
        }

        // Resolve Anchor Reading Story (Hybrid Topic + Readability + Grammar Synergy)
        val allReadingTexts = dao.getAllReadingTexts()
        val hasReadingForUserLevel = allReadingTexts.any {
            it.level.equals(profile.level, ignoreCase = true) ||
                profile.level.uppercase(Locale.US) !in listOf("A1", "A2")
        }
        val preferSentencePractice = !hasReadingForUserLevel
        val explicitPreferredTopics: List<String> = try {
            json.decodeFromString<List<String>>(profile.preferredTopicsJson)
                .map { AdaptiveConfig.normalizeMacroTopic(it) }
                .distinct()
        } catch (_: Exception) {
            emptyList()
        }
        val effectivePreferredTopics = explicitPreferredTopics.ifEmpty {
            AdaptiveConfig.getDefaultTopicsForGoal(profile.goal)
        }

        val allWordProgress = dao.getAllWordProgressList()
        val stageByHeadword = HashMap<String, WordStage>(allWordProgress.size * 2)
        for (wp in allWordProgress) {
            val st = LearnerModelRepository.classifyWordStage(wp)
            val prev = stageByHeadword[wp.text.lowercase()]
            if (prev == null || st.ordinal > prev.ordinal) {
                stageByHeadword[wp.text.lowercase()] = st
            }
        }
        val baselineLevels = when (profile.level.trim().uppercase(Locale.US)) {
            "A2" -> listOf("A1")
            "B1" -> listOf("A1", "A2")
            "B2" -> listOf("A1", "A2", "B1")
            "C1" -> listOf("A1", "A2", "B1", "B2")
            else -> emptyList()
        }
        val baselineKnownHeadwords = if (baselineLevels.isNotEmpty()) {
            dao.getNewWordsForLevelsAndDecks(baselineLevels, listOf("oxford5000"), 1500)
                .mapTo(HashSet()) { it.text.lowercase() }
        } else emptySet()

        var anchorStory: ReadingTextEntity? = null
        var anchorStoryReadability: StoryReadabilityReport? = null
        if (!preferSentencePractice && targetLesson != null) {
            val pairedNumber = targetLesson.id.filter { it.isDigit() }
            val directPairedId = if (pairedNumber.isNotEmpty()) "r$pairedNumber" else targetLesson.id.replaceFirst("g", "r")
            val directPairedText = dao.getReadingTextById(directPairedId)

            val useHybridScoring = explicitPreferredTopics.isNotEmpty() ||
                profile.level.uppercase(Locale.US) in listOf("A1", "A2") ||
                directPairedText == null

            if (useHybridScoring) {
                val levelUncompleted = dao.getUncompletedReadingTextsForLevel(profile.level)
                    .ifEmpty { dao.getUncompletedReadingTextsForLevel(targetLesson.level) }
                    .ifEmpty { allReadingTexts.filter { it.level.equals(profile.level, ignoreCase = true) } }
                    .ifEmpty { allReadingTexts }

                var bestScore = Int.MIN_VALUE
                for (candidate in levelUncompleted) {
                    val report = learnerModelRepository.computeStoryReadability(
                        story = candidate,
                        userLevel = profile.level,
                        progressByHeadword = stageByHeadword,
                        baselineKnownHeadwords = baselineKnownHeadwords
                    )
                    var score = 0
                    if (report.macroTopic in effectivePreferredTopics) {
                        score += AdaptiveConfig.STORY_SCORE_TOPIC_MATCH
                    }
                    if (report.readabilityPercent in AdaptiveConfig.OPTIMAL_READABILITY_MIN_PERCENT..AdaptiveConfig.OPTIMAL_READABILITY_MAX_PERCENT) {
                        score += AdaptiveConfig.STORY_SCORE_OPTIMAL_READABILITY
                    }
                    if (candidate.id == directPairedId || candidate.grammarIdsJson.contains(targetLesson.id)) {
                        score += AdaptiveConfig.STORY_SCORE_GRAMMAR_SYNERGY
                    }
                    if (score > bestScore) {
                        bestScore = score
                        anchorStory = candidate
                        anchorStoryReadability = report
                    }
                }
            } else {
                anchorStory = directPairedText
                    ?: allReadingTexts.firstOrNull { it.grammarIdsJson.contains(targetLesson.id) }
                    ?: dao.getUncompletedReadingTextsForLevel(targetLesson.level).firstOrNull()
                    ?: dao.getLowestScoringReadingTexts(1).firstOrNull()
                    ?: allReadingTexts.firstOrNull()
                if (anchorStory != null) {
                    anchorStoryReadability = learnerModelRepository.computeStoryReadability(
                        story = anchorStory,
                        userLevel = profile.level,
                        progressByHeadword = stageByHeadword,
                        baselineKnownHeadwords = baselineKnownHeadwords
                    )
                }
            }
        }

        // Priority 3: New Words (pre-teaches 4-6 unstudied story target words when anchor story is linked, then fills from profile selection)
        val selectionResult = selectNewWordsForProfile(profile, newWordsTarget)
        val preTeachWords = if (anchorStory != null && (explicitPreferredTopics.isNotEmpty() || profile.level.uppercase(Locale.US) in listOf("A1", "A2"))) {
            selectStoryPreTeachEntries(anchorStory, anchorStoryReadability, selectedDecks, stageByHeadword, maxCount = minOf(4, newWordsTarget / 2))
        } else emptyList()

        val candidateWords = if (preTeachWords.isNotEmpty()) {
            val preTeachHeadwords = preTeachWords.mapTo(HashSet()) { it.text.lowercase() }
            val remaining = selectionResult.words.filter { it.text.lowercase() !in preTeachHeadwords }
            (preTeachWords + remaining).take(newWordsTarget)
        } else {
            selectionResult.words
        }

        val samplePreview = candidateWords.take(3).joinToString(", ") { it.text }
        val actualWordCount = candidateWords.size.coerceAtLeast(1)
        val newWordsTime = maxOf(1, (actualWordCount * 0.75f).roundToInt())
        val newWordsReason = if (isReviewsHeavy) {
            "Fewer new words today: $totalDue reviews are due"
        } else if (preTeachWords.isNotEmpty() && anchorStory != null) {
            "Pre-teaching ${preTeachWords.size} words for '${anchorStory.titleEn}' + ${actualWordCount - preTeachWords.size} ${profile.level} ${formatGoalBadge(profile.goal)} words (~$newWordsTime min)"
        } else if (selectionResult.reasonString != null) {
            selectionResult.reasonString
        } else {
            "$actualWordCount ${profile.level} words matched to your ${formatGoalBadge(profile.goal)} goal (~$newWordsTime min)"
        }
        val reasonPrefix = if (selectionResult.reasonString != null) "${selectionResult.reasonString} • " else ""

        tasks.add(
            DailyTaskItem(
                id = "task_new_words",
                type = "new_words",
                titleEn = if (isRestDay) "Learn ${candidateWords.size} Light Words (Rest Day)" else "Learn ${candidateWords.size} New Words (${profile.level})",
                titleAr = if (isRestDay) "تعلم ${candidateWords.size} مفردات خفيفة (يوم راحة)" else "تعلم ${candidateWords.size} مفردات جديدة (مستوى ${profile.level})",
                subtitleEn = if (samplePreview.isNotBlank()) "${reasonPrefix}Focus: $samplePreview" else "${reasonPrefix}Deck: ${formatDeckLabel(primaryDeck)}",
                targetCount = candidateWords.size.coerceAtLeast(1),
                completedCount = 0,
                isCompleted = false,
                linkedRoute = "word_books",
                linkedId = "$primaryDeck|${profile.level}",
                explanationAr = if (isAdjustedAfterMissedDay) {
                    "أهلاً بعودتك! خففنا كلمات اليوم لتستأنف التعلم بسلاسة وبدون أي ضغط."
                } else if (isRestDay) {
                    "مفردات خفيفة اختيارية للحفاظ على استمرارية التعلّم في يوم الراحة."
                } else {
                    "دراسة كلمات جديدة تناسب مستواك ${profile.level} مع أمثلتها."
                },
                reason = newWordsReason,
                estimatedMinutes = newWordsTime,
                backlogRemaining = 0
            )
        )

        // Priority 4 & 5: Grammar Lesson & Reading OR Short Sentence Practice
        if (targetLesson != null) {
            val grammarProgress = dao.getGrammarProgress(targetLesson.id)
            val isGrammarDone = !isGrammarRefresher && ((grammarProgress != null && (grammarProgress.completedExercises > 0 || grammarProgress.isCompleted)) || targetLesson.isCompleted)
            val grammarReason = when {
                isGrammarRefresher && grammarRefresherReason != null -> grammarRefresherReason
                isGrammarExhausted -> "Review of ${targetLesson.level} lesson ${targetLesson.id} (~$grammarTime min)"
                else -> "Unfinished ${targetLesson.level} lesson ${targetLesson.id} (~$grammarTime min)"
            }
            val grammarTitleEn = if (isGrammarRefresher) "Grammar [Refresher]: ${targetLesson.titleEn}" else "Grammar: ${targetLesson.titleEn}"
            val grammarTitleAr = if (isGrammarRefresher) "قواعد [مراجعة علاجية]: ${targetLesson.titleAr}" else "قواعد: ${targetLesson.titleAr}"
            tasks.add(
                DailyTaskItem(
                    id = "task_grammar_${targetLesson.id}",
                    type = "grammar",
                    titleEn = grammarTitleEn,
                    titleAr = grammarTitleAr,
                    subtitleEn = if (minutes <= 15) "Topic: ${targetLesson.topic} • ${targetLesson.level} ($exerciseCount exercises, $grammarTime min)" else "Topic: ${targetLesson.topic} • ${targetLesson.level} ($grammarTime min)",
                    targetCount = 1,
                    completedCount = if (isGrammarDone) 1 else 0,
                    isCompleted = isGrammarDone,
                    linkedRoute = "grammar",
                    linkedId = targetLesson.id,
                    explanationAr = if (isGrammarRefresher) "مراجعة علاجية لدرس القواعد لتقوية النقاط التي تحتاج لتثبيت." else if (isGrammarExhausted) "مراجعة ذكية لترسيخ القواعد بعد إتمام دروس المستوى." else "دراسة درس القواعد وحل تمارينه التفاعلية.",
                    reason = grammarReason,
                    estimatedMinutes = grammarTime,
                    backlogRemaining = 0
                )
            )

            if (preferSentencePractice) {
                val sentenceReason = "Sentence production practice matched to level ${profile.level} (~3 min)"
                tasks.add(
                    DailyTaskItem(
                        id = "task_sentence_practice",
                        type = "sentences",
                        titleEn = "Daily Sentence Practice",
                        titleAr = "تطبيق الجمل اليومي (٣ جمل)",
                        subtitleEn = "Practice building real sentences • Level ${profile.level} (3 min)",
                        targetCount = 3,
                        completedCount = 0,
                        isCompleted = false,
                        linkedRoute = "sentences",
                        linkedId = "",
                        explanationAr = "تطبيق عملي على صياغة الجمل لترسيخ القواعد والمفردات في سياق واقعي.",
                        reason = sentenceReason,
                        estimatedMinutes = 3,
                        backlogRemaining = 0
                    )
                )
            } else {
                val pairedText = anchorStory
                if (pairedText != null) {
                    val readingProgress = dao.getReadingProgress(pairedText.id)
                    val isReadingDone = readingProgress != null && (readingProgress.completedQuestions > 0 || readingProgress.isCompleted)
                    val readabilityBadge = anchorStoryReadability?.let { "${it.readabilityPercent}% readable • ${it.focusWords.size} focus words • " } ?: ""
                    val topicLabel = anchorStoryReadability?.let { AdaptiveConfig.getMacroTopicLabel(it.macroTopic) } ?: pairedText.topic
                    val readingReason = "${readabilityBadge}Topic: $topicLabel • Paired with grammar lesson ${targetLesson.titleEn} (~$readingTime min)"
                    tasks.add(
                        DailyTaskItem(
                            id = "task_reading_${pairedText.id}",
                            type = "reading",
                            titleEn = "Reading: ${pairedText.titleEn}",
                            titleAr = "قراءة: ${pairedText.titleAr}",
                            subtitleEn = if (minutes <= 15) "${readabilityBadge}Short text • Level: ${pairedText.level} ($readingTime min)" else "${readabilityBadge}Level: ${pairedText.level} • Paired with ${targetLesson.titleEn} ($readingTime min)",
                            targetCount = 1,
                            completedCount = if (isReadingDone) 1 else 0,
                            isCompleted = isReadingDone,
                            linkedRoute = "reading",
                            linkedId = pairedText.id,
                            explanationAr = if (isGrammarExhausted) "قراءة متقدمة لترسيخ المفردات والقواعد." else "قراءة النص في سياق درس القواعد والإجابة على أسئلة الفهم.",
                            reason = readingReason,
                            estimatedMinutes = readingTime,
                            backlogRemaining = 0
                        )
                    )
                } else {
                    tasks.add(
                        DailyTaskItem(
                            id = "task_reading_fallback",
                            type = "reading",
                            titleEn = "Reading: Graded Stories",
                            titleAr = "قراءة: نصوص متدرجة",
                            subtitleEn = "Bilingual texts • Level: ${profile.level} ($readingTime min)",
                            targetCount = 1,
                            completedCount = 0,
                            isCompleted = false,
                            linkedRoute = "reading",
                            linkedId = "",
                            explanationAr = "جميع نصوص المستوى مكتملة • يمكنك مراجعة أي نص لترسيخ الطلاقة.",
                            reason = "Graded reading practice • Level ${profile.level} (~$readingTime min)",
                            estimatedMinutes = readingTime,
                            backlogRemaining = 0
                        )
                    )
                }
            }
        } else {
            tasks.add(
                DailyTaskItem(
                    id = "task_grammar_fallback",
                    type = "grammar",
                    titleEn = "Grammar: Master Rules Review",
                    titleAr = "قواعد: مراجعة شاملة",
                    subtitleEn = "Comprehensive review • Level ${profile.level} ($grammarTime min)",
                    targetCount = 1,
                    completedCount = 0,
                    isCompleted = false,
                    linkedRoute = "grammar",
                    linkedId = "",
                    explanationAr = "تمت جميع دروس المستوى بنجاح • استمر في تثبيت القواعد.",
                    reason = "Comprehensive review • Level ${profile.level} (~$grammarTime min)",
                    estimatedMinutes = grammarTime,
                    backlogRemaining = 0
                )
            )
            if (preferSentencePractice) {
                val sentenceReason = "Sentence production practice matched to level ${profile.level} (~3 min)"
                tasks.add(
                    DailyTaskItem(
                        id = "task_sentence_practice",
                        type = "sentences",
                        titleEn = "Daily Sentence Practice",
                        titleAr = "تطبيق الجمل اليومي (٣ جمل)",
                        subtitleEn = "Practice building real sentences • Level ${profile.level} (3 min)",
                        targetCount = 3,
                        completedCount = 0,
                        isCompleted = false,
                        linkedRoute = "sentences",
                        linkedId = "",
                        explanationAr = "تطبيق عملي على صياغة الجمل لترسيخ القواعد والمفردات في سياق واقعي.",
                        reason = sentenceReason,
                        estimatedMinutes = 3,
                        backlogRemaining = 0
                    )
                )
            } else {
                tasks.add(
                    DailyTaskItem(
                        id = "task_reading_fallback",
                        type = "reading",
                        titleEn = "Reading: Graded Stories",
                        titleAr = "قراءة: نصوص متدرجة",
                        subtitleEn = "Bilingual texts • Level: ${profile.level} ($readingTime min)",
                        targetCount = 1,
                        completedCount = 0,
                        isCompleted = false,
                        linkedRoute = "reading",
                        linkedId = "",
                        explanationAr = "جميع نصوص المستوى مكتملة • يمكنك مراجعة أي نص لترسيخ الطلاقة.",
                        reason = "Graded reading practice • Level ${profile.level} (~$readingTime min)",
                        estimatedMinutes = readingTime,
                        backlogRemaining = 0
                    )
                )
            }
        }

        // Priority 6: Daily Mini-Quiz (Aligned 10-Question Assessment; adapts to Quiet Mode)
        val quizQuestions = 10
        val quizMin = 5
        val quietSuffix = if (profile.quietModeEnabled) " • Quiet Mode (No Audio/Mic)" else ""
        val quizReason = "Aligned assessment: 5 Vocab, 3 Grammar, 2 Reading (~$quizMin min)$quietSuffix"
        tasks.add(
            DailyTaskItem(
                id = "task_daily_quiz",
                type = "quiz",
                titleEn = if (profile.quietModeEnabled) "Daily Mini-Quiz (Quiet Mode)" else "Daily Mini-Quiz (10 Questions)",
                titleAr = if (profile.quietModeEnabled) "الاختبار اليومي (الوضع الهادئ - بدون صوت)" else "الاختبار اليومي (١٠ أسئلة)",
                subtitleEn = "5 Vocab • 3 Grammar • 2 Reading ($quizMin min)$quietSuffix",
                targetCount = quizQuestions,
                completedCount = 0,
                isCompleted = false,
                linkedRoute = "quiz",
                linkedId = "quiz",
                explanationAr = "اختبار يومي مباشر يقيّم كلمات اليوم وقاعدتها ونص القراءة.",
                reason = quizReason,
                estimatedMinutes = quizMin,
                backlogRemaining = 0
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

    private suspend fun selectStoryPreTeachEntries(
        story: ReadingTextEntity,
        readabilityReport: StoryReadabilityReport?,
        selectedDecks: List<String>,
        stageByHeadword: Map<String, WordStage>,
        maxCount: Int
    ): List<EntryEntity> {
        if (maxCount <= 0) return emptyList()
        val focusWords = readabilityReport?.focusWords?.ifEmpty { null }
            ?: try {
                json.decodeFromString<List<String>>(story.targetWordsJson)
            } catch (_: Exception) {
                emptyList()
            }
        val unstudiedFocus = focusWords
            .map { it.trim().lowercase(Locale.US) }
            .filter { word ->
                word.isNotBlank() &&
                    (stageByHeadword[word] == null || stageByHeadword[word] == WordStage.NEW || stageByHeadword[word] == WordStage.SEEN)
            }
            .distinct()
        if (unstudiedFocus.isEmpty()) return emptyList()

        val deckMatches = if (selectedDecks.isNotEmpty()) {
            dao.getEntriesByHeadwordsInDecks(unstudiedFocus, selectedDecks)
        } else emptyList()
        val allMatches = if (deckMatches.size >= maxCount) {
            deckMatches
        } else {
            deckMatches + dao.getEntriesByHeadwords(unstudiedFocus)
        }
        val seenHeadwords = HashSet<String>()
        val result = mutableListOf<EntryEntity>()
        for (entry in allMatches) {
            val lower = entry.text.trim().lowercase(Locale.US)
            if (entry.arabic.isNotBlank() && seenHeadwords.add(lower)) {
                result.add(entry)
                if (result.size >= maxCount) break
            }
        }
        return result
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
        val reviewCardsRatedToday = dao.getReviewCardsRatedTodayCount(startOfDay)
        val newWordsLearnedToday = dao.getNewWordsRatedTodayCount(startOfDay)
        val quizResultsToday = dao.getQuizResultsForDate(checklist.date)
        val dueCorrectionCount = dao.getDueCorrectionReviewsCount(now)

        val profile = getOrCreateProfile()
        val selectedDecks: List<String> = try {
            json.decodeFromString<List<String>>(profile.selectedDecksJson).ifEmpty {
                listOf("oxford5000", "oxford_opal")
            }
        } catch (_: Exception) {
            listOf("oxford5000", "oxford_opal")
        }
        val dueNowCutoff = timeProvider.dueNowCutoff(now)
        val dueNowCardsCount = if (selectedDecks.isNotEmpty()) {
            dao.getDueNowCardsCountForDecks(dueNowCutoff, selectedDecks)
        } else {
            dao.getDueNowCardsCount(dueNowCutoff)
        }
        val totalFlashcardsRatedToday = reviewCardsRatedToday + newWordsLearnedToday

        val updatedTasks = tasks.map { task ->
            when (task.type) {
                "flashcards" -> {
                    val normalizedTarget = if (task.targetCount <= 0) 10 else task.targetCount
                    val allDueReviewsCleared = (reviewCardsRatedToday > 0 && dueNowCardsCount == 0)
                    val targetSatisfiedByReviews = (reviewCardsRatedToday >= normalizedTarget)
                    val targetSatisfiedByGeneralPractice = (task.linkedRoute == "flashcards" && totalFlashcardsRatedToday >= normalizedTarget)
                    val isDone = task.isCompleted || targetSatisfiedByReviews || allDueReviewsCleared || targetSatisfiedByGeneralPractice
                    val effectiveTarget = if (allDueReviewsCleared && normalizedTarget > reviewCardsRatedToday) {
                        reviewCardsRatedToday
                    } else {
                        normalizedTarget
                    }
                    val currentCompleted = if (isDone) {
                        effectiveTarget
                    } else {
                        maxOf(minOf(reviewCardsRatedToday, effectiveTarget), minOf(totalFlashcardsRatedToday, effectiveTarget))
                    }
                    task.copy(
                        targetCount = effectiveTarget,
                        completedCount = currentCompleted,
                        isCompleted = isDone
                    )
                }
                "new_words" -> {
                    val normalizedTarget = if (task.targetCount <= 0) 5 else task.targetCount
                    val isDone = task.isCompleted || (newWordsLearnedToday >= normalizedTarget)
                    task.copy(
                        targetCount = normalizedTarget,
                        completedCount = if (isDone) normalizedTarget else minOf(newWordsLearnedToday, normalizedTarget),
                        isCompleted = isDone
                    )
                }
                "grammar" -> {
                    val lesson = dao.getGrammarLessonById(task.linkedId)
                    val progress = dao.getGrammarProgress(task.linkedId)
                    val isSubmitted = (progress != null && (progress.completedExercises > 0 || progress.isCompleted)) || (lesson?.isCompleted == true)
                    val isDone = task.isCompleted || isSubmitted
                    task.copy(
                        completedCount = if (isDone) 1 else 0,
                        isCompleted = isDone
                    )
                }
                "reading" -> {
                    val progress = dao.getReadingProgress(task.linkedId)
                    val isSubmitted = progress != null && (progress.completedQuestions > 0 || progress.isCompleted)
                    val isDone = task.isCompleted || isSubmitted
                    task.copy(
                        completedCount = if (isDone) 1 else 0,
                        isCompleted = isDone
                    )
                }
                "sentences" -> {
                    val attemptCount = dao.getAttemptsInRange(startOfDay, now).count { it.activityType == "sentence_practice" || it.activityType == "sentences" }
                    val isDone = task.isCompleted || (attemptCount >= task.targetCount)
                    task.copy(
                        completedCount = if (isDone) task.targetCount else minOf(attemptCount, task.targetCount),
                        isCompleted = isDone
                    )
                }
                "correction_reviews" -> {
                    val correctionAttempts = dao.getAttemptsInRange(startOfDay, now).count { it.activityType == "correction_review" }
                    val isDone = task.isCompleted || (dueCorrectionCount == 0 && (correctionAttempts > 0 || task.targetCount == 0)) || (correctionAttempts >= task.targetCount)
                    task.copy(
                        completedCount = if (isDone) task.targetCount else minOf(correctionAttempts, task.targetCount),
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
     * Recomputes completion idempotently from database state. Returns null if no checklist exists for that date.
     */
    suspend fun recomputeTodayChecklistCompletion(date: String = timeProvider.todayDateString()): DailyChecklistEntity? {
        val existing = dao.getDailyChecklist(date) ?: return null
        return refreshChecklistCompletion(existing)
    }

    /**
     * Toggles a task's manual completion state so users can both check and uncheck tasks.
     */
    suspend fun markTaskCompletedManually(date: String, taskId: String): DailyChecklistEntity {
        val checklist = dao.getDailyChecklist(date) ?: getOrCreateDailyChecklist(date)
        val tasks = try {
            json.decodeFromString<List<DailyTaskItem>>(checklist.tasksJson)
        } catch (e: Exception) {
            emptyList()
        }

        val updated = tasks.map { task ->
            if (task.id == taskId) {
                val nextCompleted = !task.isCompleted
                val normalizedTarget = if (task.targetCount <= 0) {
                    if (task.type == "flashcards") 10 else 1
                } else task.targetCount
                task.copy(
                    targetCount = normalizedTarget,
                    isCompleted = nextCompleted,
                    completedCount = if (nextCompleted) normalizedTarget else 0
                )
            } else {
                task
            }
        }
        val allDone = updated.isNotEmpty() && updated.all { it.isCompleted }
        val entity = checklist.copy(
            tasksJson = json.encodeToString(updated),
            completedCount = updated.count { it.isCompleted },
            totalCount = updated.size,
            isCompleted = allDone,
            completedAt = if (allDone) checklist.completedAt ?: timeProvider.currentTimeMillis() else null
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

    /**
     * Computes real stored practice accuracy, mastery, and due-now vs due-today metrics
     * strictly separated from daily task completion counts.
     */
    suspend fun getPracticeAccuracySummary(): PracticeAccuracySummary {
        val now = timeProvider.currentTimeMillis()
        val startOfToday = timeProvider.startOfDayMillis(timeProvider.todayDateString())
        val endOfToday = timeProvider.dueTodayCutoff(now)
        val dueNowCutoff = timeProvider.dueNowCutoff(now)
        val sevenDaysAgo = startOfToday - 6L * 86_400_000L

        val profile = getOrCreateProfile()
        val selectedDecks: List<String> = try {
            json.decodeFromString<List<String>>(profile.selectedDecksJson).ifEmpty {
                listOf("oxford5000", "oxford_opal")
            }
        } catch (_: Exception) {
            listOf("oxford5000", "oxford_opal")
        }

        val newWordsToday = dao.getNewWordsLearnedInRangeCount(startOfToday, endOfToday)
        val reviewsToday = dao.getReviewCardsRatedInRangeCount(startOfToday, endOfToday)
        val todayAttempts = dao.getAttemptsInRange(startOfToday, endOfToday)
        val sevenDayAttempts = dao.getAttemptsInRange(sevenDaysAgo, endOfToday)

        val todayGraded = todayAttempts.filter { it.isCorrect != null }
        val todayAccuracy = if (todayGraded.isNotEmpty()) {
            ((todayGraded.count { it.isCorrect == true }.toFloat() / todayGraded.size.toFloat()) * 100f).roundToInt()
        } else {
            null
        }

        val sevenDayGraded = sevenDayAttempts.filter { it.isCorrect != null }
        val sevenDayAccuracy = if (sevenDayGraded.isNotEmpty()) {
            ((sevenDayGraded.count { it.isCorrect == true }.toFloat() / sevenDayGraded.size.toFloat()) * 100f).roundToInt()
        } else {
            null
        }

        val allProgress = dao.getAllWordProgressList()
        val totalMastered = allProgress.count { it.isMastered }

        val dueNow = if (selectedDecks.isNotEmpty()) {
            dao.getDueNowCardsCountForDecks(dueNowCutoff, selectedDecks)
        } else {
            dao.getDueNowCardsCount(dueNowCutoff)
        }
        val dueToday = if (selectedDecks.isNotEmpty()) {
            dao.getReviewDueTodayCountForDecks(endOfToday, selectedDecks)
        } else {
            dao.getReviewDueTodayCount(endOfToday)
        }
        val waitingLaterToday = if (selectedDecks.isNotEmpty()) {
            dao.getLaterTodayCardsCountForDecks(dueNowCutoff, endOfToday, selectedDecks)
        } else {
            dao.getLaterTodayCardsCount(dueNowCutoff, endOfToday)
        }

        return PracticeAccuracySummary(
            newWordsLearnedToday = newWordsToday,
            reviewAttemptsToday = reviewsToday,
            totalAttemptsToday = todayAttempts.size,
            todayRecallAccuracyPercent = todayAccuracy,
            sevenDayRecallAccuracyPercent = sevenDayAccuracy,
            totalMasteredWords = totalMastered,
            dueNowCount = dueNow,
            dueTodayCount = dueToday,
            waitingLaterTodayCount = waitingLaterToday
        )
    }

    val deckWeightsConfig = DeckMixWeightsConfig()

    /**
     * 2. Order levels by closeness to the user's level:
     * - When `startAtUserLevel = true` ("At My Level"):
     *   - A1 user: A1, A2
     *   - A2 user: A2, B1, A1
     *   - B1 user: B1, B2, A2
     *   - B2 user: B2, C1, B1
     *   - C1 user: C1, B2
     *   (never silently jump to far-higher levels).
     * - When `startAtUserLevel = false` ("From A1"):
     *   Starts at A1 and progresses toward the selected level (plus at most 1 level above A1).
     */
    fun levelsByCloseness(userLevel: String, startAtUserLevel: Boolean = true): List<String> {
        val clean = userLevel.trim().uppercase(Locale.US)
        if (!startAtUserLevel) {
            return when (clean) {
                "A1" -> listOf("A1", "A2")
                "A2" -> listOf("A1", "A2", "B1")
                "B1" -> listOf("A1", "A2", "B1", "B2")
                "B2" -> listOf("A1", "A2", "B1", "B2", "C1")
                "C1" -> listOf("A1", "A2", "B1", "B2", "C1")
                else -> listOf("A1", "A2", "B1", "B2", "C1")
            }
        }
        return when (clean) {
            "A1" -> listOf("A1", "A2")
            "A2" -> listOf("A2", "B1", "A1")
            "B1" -> listOf("B1", "B2", "A2")
            "B2" -> listOf("B2", "C1", "B1")
            "C1" -> listOf("C1", "B2")
            else -> listOf(clean)
        }
    }

    /**
     * 3. Deck mix by weights, not by ID order:
     * Split the daily target across the selected decks by weights.
     */
    fun calculateDeckQuotas(
        selectedDecks: List<String>,
        goal: String,
        totalTarget: Int,
        config: DeckMixWeightsConfig = deckWeightsConfig
    ): Map<String, Int> {
        if (selectedDecks.isEmpty() || totalTarget <= 0) return emptyMap()
        if (selectedDecks.size == 1) return mapOf(selectedDecks.first() to totalTarget)

        val cleanGoal = goal.trim().lowercase(Locale.US)
        val goalWeights = config.goalDeckWeights[cleanGoal] ?: emptyMap()

        val rawWeights = selectedDecks.associateWith { deck ->
            goalWeights[deck] ?: config.defaultDeckWeights[deck] ?: (1.0f / selectedDecks.size)
        }
        val totalWeight = rawWeights.values.sum().coerceAtLeast(0.0001f)
        val normalizedWeights = rawWeights.mapValues { it.value / totalWeight }

        val quotas = mutableMapOf<String, Int>()
        var allocated = 0
        val sortedByWeightDesc = selectedDecks.sortedByDescending { normalizedWeights[it] ?: 0f }

        for (deck in sortedByWeightDesc) {
            val count = (totalTarget * (normalizedWeights[deck] ?: 0f)).roundToInt()
            quotas[deck] = count
            allocated += count
        }

        var diff = totalTarget - allocated
        var i = 0
        while (diff != 0 && i < sortedByWeightDesc.size * 2) {
            val deck = sortedByWeightDesc[i % sortedByWeightDesc.size]
            val current = quotas[deck] ?: 0
            if (diff > 0) {
                quotas[deck] = current + 1
                diff--
            } else if (diff < 0 && current > 0) {
                quotas[deck] = current - 1
                diff++
            }
            i++
        }

        return quotas
    }

    /**
     * Selects new words honoring all Phase 1 selection requirements:
     * 1. Uses `entries.effective_level` (Oxford 5000 headword match, else graded deck level for `oxford5000`/`oxford_phrases`/`cefr_c1`, else `NULL`).
     * 2. Orders levels by closeness to user level (`levelsByCloseness(cleanLevel, profile.startAtUserLevel)`), and orders words numerically (`CAST(e.id AS INTEGER)`).
     * 3. Picks one deterministic entry per lowercase headword (preferring the target level, then closeness rank, then lowest numeric ID).
     * 4. `NULL`-level words form a specialty share (default 30%, 40% IELTS, 45% IT career) used ONLY for `B1+` users;
     *    for `A1`/`A2` users, only words whose `effective_level` is in `closenessLevels` (at most 1 level above theirs) are eligible.
     * 5. If a selected deck has nothing near the user's level, fills ONLY from other `selectedDecks` (never unselected decks, never all levels),
     *    else returns fewer words with a clear reason string.
     * 6. Matches studied words by `entry_id`.
     */
    suspend fun selectNewWordsForProfile(
        profile: StudyPlanProfileEntity,
        targetCount: Int,
        weightsConfig: DeckMixWeightsConfig = deckWeightsConfig
    ): WordSelectionResult {
        if (targetCount <= 0) return WordSelectionResult(emptyList(), null)

        val cleanLevel = profile.level.trim().uppercase(Locale.US).ifBlank { "B2" }
        val cleanGoal = profile.goal.trim().lowercase(Locale.US).ifBlank { "general_fluency" }
        val selectedDecks: List<String> = try {
            json.decodeFromString<List<String>>(profile.selectedDecksJson).ifEmpty {
                listOf("oxford5000", "oxford_opal")
            }
        } catch (_: Exception) {
            listOf("oxford5000", "oxford_opal")
        }

        val closenessLevels = levelsByCloseness(cleanLevel, profile.startAtUserLevel)
        val preferredLevel = closenessLevels.firstOrNull() ?: cleanLevel
        val isB1Plus = cleanLevel in setOf("B1", "B2", "C1", "C2")
        val deckQuotas = calculateDeckQuotas(selectedDecks, cleanGoal, targetCount, weightsConfig)

        // Total NULL-level specialty share allowed across the daily selection (only for B1+ users)
        val totalSpecialtyBudget = if (isB1Plus) {
            (targetCount * weightsConfig.specialtyShareForGoal(cleanGoal))
                .roundToInt()
                .coerceIn(0, targetCount)
        } else {
            0
        }
        var remainingSpecialtyBudget = totalSpecialtyBudget

        val ungradedSelectedDecks = selectedDecks.filter { it !in EntryEntity.GRADED_DECKS }
        val totalUngradedQuota = ungradedSelectedDecks.sumOf { deckQuotas[it] ?: 0 }

        val lookedUpHeadwords = dao.getAllWordProgressList()
            .filter { it.lookupCount > 0 && it.firstLearnedTimestamp == 0L && it.lastReviewedTimestamp == 0L }
            .mapTo(HashSet()) { it.text.trim().lowercase(Locale.US) }

        // Pre-fetch and deduplicate unstudied candidates per selected deck
        val gradedByDeck = LinkedHashMap<String, List<EntryEntity>>()
        val nullLevelByDeck = LinkedHashMap<String, List<EntryEntity>>()

        for (deck in selectedDecks) {
            val rawCandidates = dao.getUnstudiedEntriesForDeckAndEffectiveLevels(
                deck = deck,
                levels = closenessLevels,
                includeNullLevel = if (isB1Plus) 1 else 0
            )
            val dedupedDeckCandidates = deduplicateCandidatesForDeck(
                entries = rawCandidates,
                closenessLevels = closenessLevels,
                lookedUpHeadwords = lookedUpHeadwords
            )
            gradedByDeck[deck] = dedupedDeckCandidates.filter { it.effectiveLevel != null }
            nullLevelByDeck[deck] = if (isB1Plus) {
                dedupedDeckCandidates.filter { it.effectiveLevel == null }
            } else {
                emptyList()
            }
        }

        val collectedEntries = mutableListOf<EntryEntity>()
        val usedIds = HashSet<String>()
        val usedHeadwords = HashSet<String>()
        val countPerDeck = mutableMapOf<String, Int>()

        fun takeFromList(source: List<EntryEntity>, maxCount: Int): List<EntryEntity> {
            if (maxCount <= 0) return emptyList()
            val picked = ArrayList<EntryEntity>(maxCount)
            for (item in source) {
                if (picked.size >= maxCount) break
                val headword = item.text.trim().lowercase(Locale.US)
                if (item.id !in usedIds && headword !in usedHeadwords) {
                    usedIds.add(item.id)
                    usedHeadwords.add(headword)
                    picked.add(item)
                }
            }
            return picked
        }

        // Pass 1: Fill each selected deck's quota
        for (deck in selectedDecks) {
            val quota = deckQuotas[deck] ?: 0
            if (quota <= 0) continue

            val deckPicked = mutableListOf<EntryEntity>()
            val isUngradedDeck = deck !in EntryEntity.GRADED_DECKS

            if (isB1Plus && isUngradedDeck && remainingSpecialtyBudget > 0) {
                val proportionalSpecialty = if (totalUngradedQuota > 0) {
                    ((totalSpecialtyBudget.toFloat() * quota.toFloat()) / totalUngradedQuota.toFloat())
                        .roundToInt()
                        .coerceIn(1, minOf(quota, remainingSpecialtyBudget))
                } else {
                    minOf(quota, remainingSpecialtyBudget)
                }
                val specialtyPicked = takeFromList(
                    source = nullLevelByDeck[deck].orEmpty(),
                    maxCount = proportionalSpecialty
                )
                deckPicked.addAll(specialtyPicked)
                remainingSpecialtyBudget -= specialtyPicked.size
            }

            // Fill remaining quota for this deck from effective_level IN closenessLevels
            val neededGraded = quota - deckPicked.size
            if (neededGraded > 0) {
                val gradedPicked = takeFromList(
                    source = gradedByDeck[deck].orEmpty(),
                    maxCount = neededGraded
                )
                deckPicked.addAll(gradedPicked)
            }

            // If B1+ and this ungraded deck has no graded words left (e.g. idioms/expressions),
            // allow filling up to remainingSpecialtyBudget from its NULL-level pool
            val stillShortInDeck = quota - deckPicked.size
            if (isB1Plus && isUngradedDeck && stillShortInDeck > 0 && remainingSpecialtyBudget > 0) {
                val extraSpecialty = takeFromList(
                    source = nullLevelByDeck[deck].orEmpty(),
                    maxCount = minOf(stillShortInDeck, remainingSpecialtyBudget)
                )
                deckPicked.addAll(extraSpecialty)
                remainingSpecialtyBudget -= extraSpecialty.size
            }

            countPerDeck[deck] = deckPicked.size
            collectedEntries.addAll(deckPicked)
        }

        val emptyDecks = selectedDecks.filter { (countPerDeck[it] ?: 0) == 0 }
        val shortDecks = selectedDecks.filter { (countPerDeck[it] ?: 0) < (deckQuotas[it] ?: 0) }

        // Pass 2: If any selected deck was short, fill ONLY from OTHER selectedDecks (never unselected decks)
        if (collectedEntries.size < targetCount) {
            for (altDeck in selectedDecks) {
                val stillNeeded = targetCount - collectedEntries.size
                if (stillNeeded <= 0) break
                val moreGraded = takeFromList(
                    source = gradedByDeck[altDeck].orEmpty(),
                    maxCount = stillNeeded
                )
                collectedEntries.addAll(moreGraded)
                countPerDeck[altDeck] = (countPerDeck[altDeck] ?: 0) + moreGraded.size
            }

            if (isB1Plus && collectedEntries.size < targetCount && remainingSpecialtyBudget > 0) {
                for (altDeck in selectedDecks) {
                    val stillNeeded = minOf(targetCount - collectedEntries.size, remainingSpecialtyBudget)
                    if (stillNeeded <= 0) break
                    val moreNull = takeFromList(
                        source = nullLevelByDeck[altDeck].orEmpty(),
                        maxCount = stillNeeded
                    )
                    collectedEntries.addAll(moreNull)
                    remainingSpecialtyBudget -= moreNull.size
                    countPerDeck[altDeck] = (countPerDeck[altDeck] ?: 0) + moreNull.size
                }
            }
        }

        val finalWords = deduplicateAndPickDeterministicEntry(collectedEntries, preferredLevel, lookedUpHeadwords)
            .take(targetCount)

        val reasonString: String? = when {
            finalWords.size < targetCount && emptyDecks.isNotEmpty() -> {
                "Fewer words today (${finalWords.size}/$targetCount): no $cleanLevel words in ${emptyDecks.joinToString(", ") { formatDeckLabel(it) }}"
            }
            finalWords.size < targetCount -> {
                "Fewer words today (${finalWords.size}/$targetCount): showing all available words near $cleanLevel in selected decks"
            }
            emptyDecks.isNotEmpty() -> {
                val donorDecks = selectedDecks.filter { it !in emptyDecks && (countPerDeck[it] ?: 0) > 0 }
                "No $cleanLevel words in ${emptyDecks.joinToString(", ") { formatDeckLabel(it) }}; filled from ${donorDecks.joinToString(", ") { formatDeckLabel(it) }}"
            }
            shortDecks.isNotEmpty() -> {
                val donorDecks = selectedDecks.filter { it !in shortDecks && (countPerDeck[it] ?: 0) > (deckQuotas[it] ?: 0) }
                if (donorDecks.isNotEmpty()) {
                    "Limited $cleanLevel words in ${shortDecks.joinToString(", ") { formatDeckLabel(it) }}; filled from ${donorDecks.joinToString(", ") { formatDeckLabel(it) }}"
                } else null
            }
            else -> null
        }

        return WordSelectionResult(
            words = finalWords,
            reasonString = reasonString
        )
    }

    suspend fun regenerateDailyChecklist(date: String = timeProvider.todayDateString()): DailyChecklistEntity {
        return regenerateChecklistForUpdatedPlan(date = date)
    }

    private fun compareNumericId(a: EntryEntity, b: EntryEntity): Int {
        val idA = a.id.toLongOrNull() ?: Long.MAX_VALUE
        val idB = b.id.toLongOrNull() ?: Long.MAX_VALUE
        val cmp = idA.compareTo(idB)
        return if (cmp != 0) cmp else a.id.compareTo(b.id)
    }

    private fun deduplicateCandidatesForDeck(
        entries: List<EntryEntity>,
        closenessLevels: List<String>,
        lookedUpHeadwords: Set<String> = emptySet()
    ): List<EntryEntity> {
        val rankMap = closenessLevels.withIndex().associate { (idx, lvl) -> lvl.uppercase(Locale.US) to idx }
        val grouped = entries.groupBy { it.text.trim().lowercase(Locale.US) }
        val deduped = ArrayList<EntryEntity>(grouped.size)
        for ((_, variants) in grouped) {
            val best = variants.minWithOrNull(
                Comparator<EntryEntity> { a, b ->
                    val rankA = a.effectiveLevel?.uppercase(Locale.US)?.let { rankMap[it] ?: 99 } ?: 100
                    val rankB = b.effectiveLevel?.uppercase(Locale.US)?.let { rankMap[it] ?: 99 } ?: 100
                    if (rankA != rankB) {
                        rankA.compareTo(rankB)
                    } else {
                        compareNumericId(a, b)
                    }
                }
            )
            if (best != null) {
                deduped.add(best)
            }
        }
        return deduped.sortedWith(
            Comparator<EntryEntity> { a, b ->
                val aLookedUp = a.text.trim().lowercase(Locale.US) in lookedUpHeadwords
                val bLookedUp = b.text.trim().lowercase(Locale.US) in lookedUpHeadwords
                if (aLookedUp != bLookedUp) {
                    return@Comparator if (aLookedUp) -1 else 1
                }
                val aIsNull = a.effectiveLevel == null
                val bIsNull = b.effectiveLevel == null
                if (aIsNull != bIsNull) {
                    if (aIsNull) 1 else -1
                } else if (!aIsNull) {
                    val rankA = rankMap[a.effectiveLevel.uppercase(Locale.US)] ?: 99
                    val rankB = rankMap[b.effectiveLevel?.uppercase(Locale.US)] ?: 99
                    if (rankA != rankB) {
                        rankA.compareTo(rankB)
                    } else {
                        compareNumericId(a, b)
                    }
                } else {
                    compareNumericId(a, b)
                }
            }
        )
    }

    /**
     * Deduplicates candidate entries so there is strictly 1 entry per lowercase word:
     * prefers row whose effectiveLevel (or level) matches user's preferredLevel,
     * then lowest numeric ID, and returns the final list sorted in ascending numeric ID order
     * (with looked-up SEEN words prioritized first).
     */
    fun deduplicateAndPickDeterministicEntry(
        entries: List<EntryEntity>,
        userLevel: String,
        lookedUpHeadwords: Set<String> = emptySet()
    ): List<EntryEntity> {
        val grouped = entries.groupBy { it.text.trim().lowercase(Locale.US) }
        val result = mutableListOf<EntryEntity>()
        for ((_, variants) in grouped) {
            val best = variants.minWithOrNull(
                Comparator<EntryEntity> { a, b ->
                    val aEff = a.effectiveLevel ?: a.level
                    val bEff = b.effectiveLevel ?: b.level
                    val aMatchesLevel = aEff.equals(userLevel, ignoreCase = true)
                    val bMatchesLevel = bEff.equals(userLevel, ignoreCase = true)
                    when {
                        aMatchesLevel && !bMatchesLevel -> -1
                        !aMatchesLevel && bMatchesLevel -> 1
                        else -> compareNumericId(a, b)
                    }
                }
            )
            if (best != null) {
                result.add(best)
            }
        }
        return result.sortedWith(
            Comparator { a, b ->
                val aLookedUp = a.text.trim().lowercase(Locale.US) in lookedUpHeadwords
                val bLookedUp = b.text.trim().lowercase(Locale.US) in lookedUpHeadwords
                if (aLookedUp != bLookedUp) {
                    if (aLookedUp) -1 else 1
                } else {
                    compareNumericId(a, b)
                }
            }
        )
    }

    private fun formatDeckLabel(deckId: String): String {
        return when (deckId) {
            "oxford5000" -> "Oxford 5000"
            "oxford_opal" -> "Oxford OPAL"
            "oxford_phrases" -> "Oxford Phrases"
            "everyday_expressions" -> "Everyday Expressions"
            "essential_idioms" -> "Essential Idioms"
            "ielts_core" -> "IELTS Vocabulary"
            "it_industry" -> "IT & Tech Industry"
            "cefr_c1" -> "CEFR C1 Advanced"
            "pte_academic" -> "PTE Academic"
            "business_english" -> "Business English"
            "career_interview" -> "Job Interview & Career"
            "medical_healthcare" -> "Medical & Healthcare"
            else -> deckId
        }
    }

    fun formatGoalBadge(goal: String?): String {
        return when (goal?.trim()?.lowercase(Locale.US)) {
            "ielts" -> "IELTS Prep"
            "it_career" -> "IT Career"
            "medical" -> "Medical & Healthcare"
            else -> "General English"
        }
    }

    suspend fun getWeeklyChecklists(referenceDate: String = timeProvider.todayDateString()): List<DailyChecklistEntity?> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        try {
            sdf.parse(referenceDate)?.let { cal.time = it }
        } catch (_: Exception) {}

        // Monday to Sunday alignment
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val offset = if (dayOfWeek == Calendar.SUNDAY) -6 else Calendar.MONDAY - dayOfWeek
        cal.add(Calendar.DAY_OF_MONTH, offset)

        val result = mutableListOf<DailyChecklistEntity?>()
        for (i in 0 until 7) {
            val dStr = sdf.format(cal.time)
            val checklist = dao.getDailyChecklist(dStr)
            result.add(checklist)
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return result
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
            "medical" -> Pair(
                listOf(
                    PlanWeekGoal(1, "Clinical & Healthcare Core Vocabulary ($lvl)", "المفردات الأساسية للرعاية الصحية والسريرية ($lvl)", baseGoals),
                    PlanWeekGoal(2, "Patient History, Symptoms & Case Reporting", "التاريخ الطبي وتوثيق أعراض المرضى والتقارير", baseGoals),
                    PlanWeekGoal(3, "Diagnostic Terms, Pharmacology & Medical Nuances", "مصطلحات التشخيص والأدوية والتواصل الطبي الدقيق", baseGoals),
                    PlanWeekGoal(4, "Medical Ethics, Consultations & Professional Practice", "أخلاقيات الطب والاستشارات والمراجعة الشاملة", baseGoals)
                ),
                "ركز على مفردات الرعاية الصحية والتواصل الطبي مع ممارسة استخدامها في سياق التقارير السريرية."
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

/**
 * 3. Centralized config object for deck mix weights and NULL-level specialty share.
 */
data class DeckMixWeightsConfig(
    val defaultSpecialtyShare: Float = 0.30f,
    val goalSpecialtyShares: Map<String, Float> = mapOf(
        "general_fluency" to 0.30f,
        "ielts" to 0.40f,
        "it_career" to 0.45f,
        "medical" to 0.45f
    ),
    val defaultDeckWeights: Map<String, Float> = mapOf(
        "oxford5000" to 0.45f,
        "oxford_opal" to 0.35f,
        "oxford_phrases" to 0.25f,
        "everyday_expressions" to 0.20f,
        "essential_idioms" to 0.15f,
        "ielts_core" to 0.35f,
        "it_industry" to 0.35f,
        "cefr_c1" to 0.30f,
        "pte_academic" to 0.35f,
        "business_english" to 0.25f,
        "career_interview" to 0.25f,
        "medical_healthcare" to 0.35f
    ),
    val goalDeckWeights: Map<String, Map<String, Float>> = mapOf(
        "it_career" to mapOf(
            "ielts_core" to 1.05f,
            "it_industry" to 0.70f,
            "oxford5000" to 0.20f,
            "oxford_opal" to 0.10f
        ),
        "medical" to mapOf(
            "medical_healthcare" to 0.70f,
            "oxford5000" to 0.20f,
            "oxford_opal" to 0.10f
        ),
        "ielts" to mapOf(
            "ielts_core" to 0.50f,
            "oxford_opal" to 0.30f,
            "oxford5000" to 0.20f
        ),
        "general_fluency" to mapOf(
            "oxford5000" to 0.50f,
            "oxford_opal" to 0.50f,
            "oxford_phrases" to 0.25f,
            "everyday_expressions" to 0.20f,
            "essential_idioms" to 0.15f,
            "pte_academic" to 0.25f
        )
    )
) {
    fun specialtyShareForGoal(goal: String): Float {
        return goalSpecialtyShares[goal.trim().lowercase(Locale.US)] ?: defaultSpecialtyShare
    }
}

data class WordSelectionResult(
    val words: List<EntryEntity>,
    val reasonString: String? = null
)

