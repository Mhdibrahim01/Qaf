package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DailyChecklistEntity
import com.example.data.local.DailyTaskItem
import com.example.data.local.PlanDailyTemplate
import com.example.data.local.PlanWeekGoal
import com.example.data.local.QuizQuestionItem
import com.example.data.local.QuizResultEntity
import com.example.data.local.StudyPlanEntity
import com.example.data.local.StudyPlanProfileEntity
import com.example.data.local.WeakAreasState
import com.example.ui.components.BidiText
import com.example.ui.theme.ArabicTypography
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderDark
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldAccentBorder
import com.example.ui.theme.GoldAccentDim
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevatedDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

private val planUiJson = Json { ignoreUnknownKeys = true; isLenient = true }

private fun formatGoalBadge(goal: String?): String {
    return when (goal) {
        "ielts" -> "IELTS Prep"
        "it_career" -> "IT Career"
        else -> "General Fluency"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(
    profile: StudyPlanProfileEntity?,
    currentPlan: StudyPlanEntity?,
    todayChecklist: DailyChecklistEntity?,
    streak: Int,
    quizHistory: List<QuizResultEntity>,
    weakAreas: WeakAreasState,
    monthlyChecklists: List<DailyChecklistEntity>,
    onSaveProfile: (StudyPlanProfileEntity) -> Unit,
    onGeneratePlanWithAi: suspend (month: String) -> Result<Unit>,
    onGenerateDailyQuiz: suspend () -> List<QuizQuestionItem>,
    onGenerateWeeklyQuiz: suspend () -> List<QuizQuestionItem>,
    onGenerateWeakAreasQuiz: suspend () -> List<QuizQuestionItem> = { emptyList() },
    onRefreshPlan: () -> Unit = {},
    onSubmitQuizAnswers: suspend (quizType: String, questions: List<QuizQuestionItem>, answers: Map<String, Int>) -> Map<String, Float>,
    onPlayAudio: (String) -> Unit,
    onToggleTaskCompleted: (date: String, taskId: String) -> Unit,
    onNavigateToTaskRoute: (route: String, id: String) -> Unit,
    onOpenDictionaryWord: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var isSetupDialogOpen by remember { mutableStateOf(false) }

    // Quiz Dialog State
    var activeQuizQuestions by remember { mutableStateOf<List<QuizQuestionItem>?>(null) }
    var activeQuizTitle by remember { mutableStateOf("") }
    var activeQuizType by remember { mutableStateOf("") }
    var isAiGeneratingPlan by remember { mutableStateOf(false) }
    var planAiError by remember { mutableStateOf<String?>(null) }
    var planAiSuccessMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        onRefreshPlan()
    }

    val completedToday = todayChecklist?.completedCount ?: 0
    val totalToday = todayChecklist?.totalCount ?: 0
    val todayTabTitle = if (totalToday > 0) "Today ($completedToday/$totalToday)" else "Today"
    val tabs = listOf(todayTabTitle, "Monthly Plan", "Analytics")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Compact Profile Summary & Tab Bar
        Surface(
            color = SurfaceDark,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = GoldAccentDim,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, GoldAccentBorder)
                        ) {
                            Text(
                                text = "CEFR ${profile?.level ?: "B2"}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = GoldAccent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            color = SurfaceElevatedDark,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Text(
                                text = formatGoalBadge(profile?.goal),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = TextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = "${profile?.minutesPerDay ?: 30}m • ${profile?.studyDaysPerWeek ?: 6}d/wk",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }

                    OutlinedButton(
                        onClick = { isSetupDialogOpen = true },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BorderDark),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_plan_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = "Edit Plan Profile",
                            tint = GoldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Customize",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }
                }

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = SurfaceDark,
                    contentColor = GoldAccent,
                    divider = { HorizontalDivider(color = BorderSubtle) },
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = GoldAccent
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (selectedTab == index) GoldAccent else TextMuted
                                )
                            }
                        )
                    }
                }
            }
        }

        when (selectedTab) {
            0 -> TodayTabContent(
                todayChecklist = todayChecklist,
                streak = streak,
                weakAreas = weakAreas,
                onToggleTask = onToggleTaskCompleted,
                onOpenTask = onNavigateToTaskRoute,
                onStartMiniQuiz = {
                    scope.launch {
                        val questions = onGenerateDailyQuiz()
                        activeQuizQuestions = questions
                        activeQuizTitle = "Daily Mini-Quiz"
                        activeQuizType = "daily_mini"
                    }
                },
                onStartWeakAreasQuiz = {
                    scope.launch {
                        val questions = onGenerateWeakAreasQuiz()
                        activeQuizQuestions = questions
                        activeQuizTitle = "Weak Areas Targeted Practice"
                        activeQuizType = "weak_areas"
                    }
                }
            )
            1 -> MonthlyPlanTabContent(
                profile = profile,
                currentPlan = currentPlan,
                isAiGenerating = isAiGeneratingPlan,
                aiError = planAiError,
                aiSuccessMessage = planAiSuccessMessage,
                onGenerateWithAi = { month ->
                    scope.launch {
                        isAiGeneratingPlan = true
                        planAiError = null
                        planAiSuccessMessage = null
                        val res = onGeneratePlanWithAi(month)
                        if (res.isFailure) {
                            planAiError = res.exceptionOrNull()?.message ?: "Plan generation failed."
                        } else {
                            planAiSuccessMessage = "تم تحديث الخطة الشهرية ومهام اليوم بنجاح!"
                        }
                        isAiGeneratingPlan = false
                    }
                }
            )
            2 -> AnalyticsTabContent(
                monthlyChecklists = monthlyChecklists,
                quizHistory = quizHistory,
                weakAreas = weakAreas,
                onStartWeeklyQuiz = {
                    scope.launch {
                        val questions = onGenerateWeeklyQuiz()
                        activeQuizQuestions = questions
                        activeQuizTitle = "Weekly Comprehensive Quiz"
                        activeQuizType = "weekly"
                    }
                },
                onStartWeakAreasQuiz = {
                    scope.launch {
                        val questions = onGenerateWeakAreasQuiz()
                        activeQuizQuestions = questions
                        activeQuizTitle = "Weak Areas Targeted Practice"
                        activeQuizType = "weak_areas"
                    }
                },
                onOpenTask = onNavigateToTaskRoute
            )
        }
    }

    if (isSetupDialogOpen) {
        PlanSetupDialog(
            initialProfile = profile,
            onDismiss = { isSetupDialogOpen = false },
            onSaveProfile = onSaveProfile
        )
    }

    if (activeQuizQuestions != null) {
        QuizDialog(
            title = activeQuizTitle,
            quizType = activeQuizType,
            questions = activeQuizQuestions!!,
            onPlayAudio = onPlayAudio,
            onQuizCompleted = { answers ->
                scope.launch {
                    onSubmitQuizAnswers(activeQuizType, activeQuizQuestions!!, answers)
                }
            },
            onOpenDictionaryWord = onOpenDictionaryWord,
            onDismiss = { activeQuizQuestions = null }
        )
    }
}

@Composable
private fun TodayTabContent(
    todayChecklist: DailyChecklistEntity?,
    streak: Int,
    weakAreas: WeakAreasState,
    onToggleTask: (date: String, taskId: String) -> Unit,
    onOpenTask: (route: String, id: String) -> Unit,
    onStartMiniQuiz: () -> Unit,
    onStartWeakAreasQuiz: () -> Unit
) {
    val tasks: List<DailyTaskItem> = remember(todayChecklist?.tasksJson) {
        try {
            planUiJson.decodeFromString(todayChecklist?.tasksJson ?: "[]")
        } catch (e: Exception) {
            emptyList()
        }
    }
    val completedCount = todayChecklist?.completedCount ?: 0
    val totalCount = todayChecklist?.totalCount ?: tasks.size.coerceAtLeast(1)
    val progressFraction = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("today_plan_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Progress & Streak Card
        item(key = "today_hero_streak", contentType = "hero_streak") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceDark,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, GoldAccentBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.LocalFireDepartment, contentDescription = "Streak", tint = GoldAccent, modifier = Modifier.size(22.dp))
                            Text(
                                text = "$streak Day Streak",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = GoldAccent
                            )
                        }
                        Text(
                            text = "$completedCount of $totalCount tasks completed today",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary
                        )
                        Text(
                            text = if (progressFraction >= 1f) "مبروك! أكملت جميع مهام اليوم بنجاح" else "اضغط على أي مهمة لفتح الدرس أو ضع علامة صح عند الإنجاز",
                            style = ArabicTypography.labelSmall,
                            color = if (progressFraction >= 1f) EmeraldAccent else TextSecondary
                        )
                    }

                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
                        CircularProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier.fillMaxSize(),
                            color = if (progressFraction >= 1f) EmeraldAccent else GoldAccent,
                            trackColor = SurfaceElevatedDark,
                            strokeWidth = 6.dp
                        )
                        Text(
                            text = "${(progressFraction * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        // Missed Days Non-Guilt Banner
        if (todayChecklist?.isAdjusted == true) {
            item(key = "today_adjusted_banner", contentType = "adjusted_banner") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = GoldAccentDim,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, GoldAccentBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = "Warm Welcome", tint = GoldAccent, modifier = Modifier.size(22.dp))
                        Column {
                            Text(
                                text = "Welcome Back",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = "قمنا بتخفيف أهداف اليوم لتستأنف التعلم بسلاسة وبدون أي ضغط.",
                                style = ArabicTypography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Task Items Header
        item(key = "today_tasks_header", contentType = "tasks_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Checklist",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = "Tap circle to check/uncheck",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }

        // Tasks List
        items(
            items = tasks,
            key = { it.id },
            contentType = { "daily_task_row" }
        ) { task ->
            val canOpenRoute = task.type != "quiz" && task.linkedRoute.isNotBlank()
            val taskProgress = if (task.targetCount > 0) {
                (task.completedCount.toFloat() / task.targetCount.toFloat()).coerceIn(0f, 1f)
            } else {
                if (task.isCompleted) 1f else 0f
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_item_${task.type}")
                    .then(
                        if (canOpenRoute) {
                            Modifier.clickable { onOpenTask(task.linkedRoute, task.linkedId) }
                        } else {
                            Modifier
                        }
                    ),
                color = SurfaceDark,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, if (task.isCompleted) EmeraldAccent.copy(alpha = 0.55f) else BorderSubtle)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = {
                                if (todayChecklist != null) {
                                    onToggleTask(todayChecklist.date, task.id)
                                }
                            },
                            modifier = Modifier.testTag("btn_toggle_${task.type}")
                        ) {
                            Icon(
                                imageVector = if (task.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                contentDescription = if (task.isCompleted) "Uncheck task" else "Complete task",
                                tint = if (task.isCompleted) EmeraldAccent else GoldAccent,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = task.titleEn,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (task.isCompleted) TextMuted else TextPrimary,
                                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                    ),
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (task.targetCount > 1) {
                                    Surface(
                                        color = if (task.isCompleted) EmeraldAccent.copy(alpha = 0.15f) else SurfaceElevatedDark,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "${task.completedCount}/${task.targetCount}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (task.isCompleted) EmeraldAccent else GoldAccent,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = task.titleAr,
                                style = ArabicTypography.labelSmall,
                                color = if (task.isCompleted) TextMuted else TextSecondary
                            )
                            if (task.subtitleEn.isNotBlank()) {
                                Text(
                                    text = task.subtitleEn,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GoldAccent
                                )
                            }
                        }

                        val actionLabel = when (task.type) {
                            "quiz" -> if (task.isCompleted) "Retake" else "Quiz"
                            "flashcards" -> if (task.isCompleted) "Review" else "Start"
                            "new_words" -> if (task.isCompleted) "Words" else "Learn"
                            "grammar" -> if (task.isCompleted) "Lesson" else "Study"
                            "reading" -> if (task.isCompleted) "Story" else "Read"
                            "sentences" -> "Practice"
                            "correction_reviews" -> "Review"
                            else -> "Open"
                        }

                        if (task.type == "quiz") {
                            Button(
                                onClick = onStartMiniQuiz,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (task.isCompleted) SurfaceElevatedDark else GoldAccent,
                                    contentColor = if (task.isCompleted) GoldAccent else BackgroundDark
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .heightIn(min = 36.dp)
                                    .testTag("btn_start_daily_quiz")
                            ) {
                                Text(actionLabel, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        } else if (task.linkedRoute.isNotBlank()) {
                            Button(
                                onClick = { onOpenTask(task.linkedRoute, task.linkedId) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (task.isCompleted) SurfaceElevatedDark else GoldAccent,
                                    contentColor = if (task.isCompleted) GoldAccent else BackgroundDark
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .heightIn(min = 36.dp)
                                    .testTag("btn_open_task_${task.type}")
                            ) {
                                Text(actionLabel, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }

                    if (task.explanationAr.isNotBlank()) {
                        Text(
                            text = task.explanationAr,
                            style = ArabicTypography.labelSmall.copy(fontSize = 11.sp),
                            color = TextMuted,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    // Show progress bar for multi-item tasks (e.g. Flashcards, New Words, Sentences)
                    if (task.targetCount > 1) {
                        LinearProgressIndicator(
                            progress = { taskProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = if (task.isCompleted) EmeraldAccent else GoldAccent,
                            trackColor = SurfaceElevatedDark
                        )
                    }
                }
            }
        }

        // Targeted Weak-Areas Quick Drill Card on Today Tab
        if (weakAreas.weakWords.isNotEmpty() || weakAreas.weakGrammarLessons.isNotEmpty()) {
            item(key = "today_weak_areas_drill", contentType = "weak_areas_drill") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SurfaceDark,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "Weak-Areas Booster",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                            }
                            Text(
                                text = "${weakAreas.weakWords.size} weak words • ${weakAreas.weakGrammarLessons.size} focus lessons",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                        OutlinedButton(
                            onClick = onStartWeakAreasQuiz,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, GoldAccentBorder),
                            modifier = Modifier.testTag("btn_today_weak_quiz")
                        ) {
                            Text("Practice Now", color = GoldAccent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MonthlyPlanTabContent(
    profile: StudyPlanProfileEntity?,
    currentPlan: StudyPlanEntity?,
    isAiGenerating: Boolean,
    aiError: String?,
    aiSuccessMessage: String?,
    onGenerateWithAi: (month: String) -> Unit
) {
    val weeks: List<PlanWeekGoal> = remember(currentPlan?.weeksJson) {
        try {
            planUiJson.decodeFromString(currentPlan?.weeksJson ?: "[]")
        } catch (e: Exception) {
            emptyList()
        }
    }
    val template: PlanDailyTemplate = remember(currentPlan?.dailyTemplateJson) {
        try {
            planUiJson.decodeFromString(currentPlan?.dailyTemplateJson ?: "{}")
        } catch (e: Exception) {
            PlanDailyTemplate()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("monthly_plan_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Month Header
        item(key = "monthly_header", contentType = "monthly_header") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceDark,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Month: ${currentPlan?.month ?: "Current"}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = "Level ${currentPlan?.levelStart ?: profile?.level ?: "B2"} → ${currentPlan?.levelTarget ?: "B2+"} • ${formatGoalBadge(profile?.goal)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = GoldAccent
                            )
                        }
                        Surface(
                            color = if (currentPlan?.source == "ai") GoldAccentDim else SurfaceElevatedDark,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Text(
                                text = if (currentPlan?.source == "ai") "AI Adapted Plan" else "Goal Curriculum",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (currentPlan?.source == "ai") GoldAccent else TextMuted,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (aiError != null) {
                        Text(text = aiError, style = MaterialTheme.typography.labelSmall, color = CoralAccent)
                    }
                    if (aiSuccessMessage != null) {
                        BidiText(text = aiSuccessMessage, style = ArabicTypography.labelSmall, color = EmeraldAccent)
                    }

                    OutlinedButton(
                        onClick = { onGenerateWithAi(currentPlan?.month ?: "2026-11") },
                        enabled = !isAiGenerating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_replan_ai"),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, GoldAccentBorder)
                    ) {
                        if (isAiGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = GoldAccent, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generating Personalized Plan...", color = GoldAccent)
                        } else {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = "AI", tint = GoldAccent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Re-plan with AI", color = GoldAccent, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Daily Time Breakdown Template
        item(key = "monthly_daily_template", contentType = "daily_template") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceDark,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daily Pace & Targets",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = "${template.new_words} new words/day",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = GoldAccent
                        )
                    }
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PaceBadge("Flashcards", "${template.review_cards_min} min")
                        PaceBadge("Grammar", "${template.grammar_min} min")
                        PaceBadge("Reading", "${template.reading_min} min")
                        PaceBadge("Quiz", "${template.quiz_min} min")
                    }
                }
            }
        }

        // Arabic Coaching Tips
        if (!currentPlan?.tipsAr.isNullOrBlank()) {
            item(key = "monthly_coaching_tips", contentType = "coaching_tips") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SurfaceDark,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, GoldAccentBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Personal Coaching Tips • نصائح المدرب",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = GoldAccent
                        )
                        BidiText(text = currentPlan?.tipsAr ?: "", style = ArabicTypography.bodySmall, color = TextPrimary)
                    }
                }
            }
        }

        // Weekly Goals
        item(key = "monthly_weeks_header", contentType = "weeks_header") {
            Text(
                text = "Weekly Milestones (${weeks.size} Weeks)",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
        }

        items(
            items = weeks,
            key = { "week_${it.week}" },
            contentType = { "week_goal_card" }
        ) { week ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceDark,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Week ${week.week}: ${week.focus}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    BidiText(text = week.focus_ar, style = ArabicTypography.labelSmall, color = TextSecondary)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PaceBadge("Words", "${week.goals.new_words}")
                        PaceBadge("Grammar", "${week.goals.grammar_lessons}")
                        PaceBadge("Reading", "${week.goals.reading_texts}")
                        PaceBadge("Sentences", "${week.goals.sentences}")
                        PaceBadge("Quizzes", "${week.goals.quizzes}")
                    }
                }
            }
        }
    }
}

@Composable
private fun PaceBadge(label: String, value: String) {
    Surface(
        color = SurfaceElevatedDark,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "$label:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(text = value, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = GoldAccent)
        }
    }
}

@Composable
private fun AnalyticsTabContent(
    monthlyChecklists: List<DailyChecklistEntity>,
    quizHistory: List<QuizResultEntity>,
    weakAreas: WeakAreasState,
    onStartWeeklyQuiz: () -> Unit,
    onStartWeakAreasQuiz: () -> Unit,
    onOpenTask: (route: String, id: String) -> Unit
) {
    val completedDaysCount = remember(monthlyChecklists) {
        monthlyChecklists.count { it.isCompleted }
    }
    val totalTasksDoneMonth = remember(monthlyChecklists) {
        monthlyChecklists.sumOf { it.completedCount }
    }
    val sortedMonthChecklists = remember(monthlyChecklists) {
        monthlyChecklists.sortedBy { it.date }.takeLast(14)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("analytics_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Monthly Activity & Consistency Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceDark,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(20.dp))
                            Text(
                                text = "Monthly Consistency",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = "$completedDaysCount full days • $totalTasksDoneMonth tasks done",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldAccent
                        )
                    }

                    if (sortedMonthChecklists.isEmpty()) {
                        Text(
                            text = "Start completing today's tasks to build your monthly activity log.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(sortedMonthChecklists, key = { it.date }) { dayItem ->
                                val dayNumber = dayItem.date.takeLast(2)
                                val ratio = if (dayItem.totalCount > 0) {
                                    dayItem.completedCount.toFloat() / dayItem.totalCount.toFloat()
                                } else 0f
                                val dotColor = when {
                                    dayItem.isCompleted || ratio >= 1f -> EmeraldAccent
                                    dayItem.completedCount > 0 -> GoldAccent
                                    else -> SurfaceElevatedDark
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(dotColor.copy(alpha = if (dayItem.completedCount > 0) 0.22f else 1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dayNumber,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (dayItem.completedCount > 0) dotColor else TextMuted
                                        )
                                    }
                                    Text(
                                        text = "${dayItem.completedCount}/${dayItem.totalCount}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Weekly Quiz & Weak Areas Quiz Banner
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceDark,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, GoldAccentBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Weekly Comprehensive Quiz",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = "20 Questions • Vocab, Grammar, Reading & Listening",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Button(
                            onClick = onStartWeeklyQuiz,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = BackgroundDark),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_start_weekly_quiz")
                        ) {
                            Text("Start (20 Qs)", fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = BorderSubtle)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Targeted Weak-Areas Drill",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = "8 Questions focused on your hardest words & unstudied grammar",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                        OutlinedButton(
                            onClick = onStartWeakAreasQuiz,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, GoldAccentBorder),
                            modifier = Modifier.testTag("btn_start_weak_quiz")
                        ) {
                            Text("Drill (8 Qs)", color = GoldAccent, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. Skill Accuracy breakdown from quizzes
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceDark,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Quiz Accuracy by Skill",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )

                    val skills = listOf("vocabulary", "grammar", "reading", "listening")
                    skills.forEach { skill ->
                        val results = quizHistory.filter { it.skill == skill }
                        val totalQ = results.sumOf { it.totalQuestions }
                        val correctQ = results.sumOf { it.correctAnswers }
                        val pct = if (totalQ > 0) (correctQ.toFloat() / totalQ.toFloat()) * 100f else 0f

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(skill.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                                Text("${pct.toInt()}% ($correctQ / $totalQ)", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = GoldAccent)
                            }
                            LinearProgressIndicator(
                                progress = { (pct / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (pct >= 70) EmeraldAccent else GoldAccent,
                                trackColor = SurfaceElevatedDark
                            )
                        }
                    }
                }
            }
        }

        // 4. Focus Grammar Lessons
        if (weakAreas.weakGrammarLessons.isNotEmpty()) {
            item {
                Text(
                    text = "Recommended Grammar Lessons",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            }

            items(weakAreas.weakGrammarLessons, key = { it.id }) { lesson ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenTask("grammar", lesson.id) },
                    color = SurfaceDark,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Filled.MenuBook, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(20.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "${lesson.titleEn} (${lesson.level})",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                BidiText(
                                    text = lesson.titleAr,
                                    style = ArabicTypography.labelSmall,
                                    color = TextSecondary,
                                    fillMaxWidth = false
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = { onOpenTask("grammar", lesson.id) },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, GoldAccentBorder)
                        ) {
                            Text("Study", color = GoldAccent, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }

        // 5. Weak Words Needing Review
        item {
            Text(
                text = "Focus Vocabulary & Weak Words",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
        }

        if (weakAreas.weakWords.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SurfaceDark,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Text(
                        text = "لا توجد كلمات ضعيفة مسجلة حالياً! واصل التقدم الرائع.",
                        style = ArabicTypography.bodySmall,
                        color = TextMuted,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(weakAreas.weakWords, key = { it.id }) { entry ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SurfaceDark,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.text,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            BidiText(
                                text = "${entry.pos} • ${entry.arabic}",
                                style = ArabicTypography.labelSmall,
                                color = TextSecondary,
                                fillMaxWidth = false
                            )
                        }
                        OutlinedButton(
                            onClick = { onOpenTask("flashcards", entry.deck) },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, GoldAccentBorder)
                        ) {
                            Text("Review", color = GoldAccent, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }
    }
}
