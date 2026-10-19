package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
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
import androidx.compose.ui.unit.dp
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
    onSubmitQuizAnswers: suspend (quizType: String, questions: List<QuizQuestionItem>, answers: Map<String, Int>) -> Map<String, Float>,
    onPlayAudio: (String) -> Unit,
    onToggleTaskCompleted: (date: String, taskId: String) -> Unit,
    onNavigateToTaskRoute: (route: String, id: String) -> Unit,
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

    val tabs = listOf("Today", "Monthly Plan", "Analytics")

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
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                        Text(
                            text = "${profile?.minutesPerDay ?: 30} min/day • ${profile?.studyDaysPerWeek ?: 6} days/wk",
                            style = MaterialTheme.typography.labelMedium,
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
                onToggleTask = onToggleTaskCompleted,
                onOpenTask = onNavigateToTaskRoute,
                onStartMiniQuiz = {
                    scope.launch {
                        val questions = onGenerateDailyQuiz()
                        activeQuizQuestions = questions
                        activeQuizTitle = "Daily Mini-Quiz"
                        activeQuizType = "daily_mini"
                    }
                }
            )
            1 -> MonthlyPlanTabContent(
                profile = profile,
                currentPlan = currentPlan,
                isAiGenerating = isAiGeneratingPlan,
                aiError = planAiError,
                onGenerateWithAi = { month ->
                    scope.launch {
                        isAiGeneratingPlan = true
                        planAiError = null
                        val res = onGeneratePlanWithAi(month)
                        if (res.isFailure) {
                            planAiError = res.exceptionOrNull()?.message ?: "Plan generation failed."
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
            onDismiss = { activeQuizQuestions = null }
        )
    }
}

@Composable
private fun TodayTabContent(
    todayChecklist: DailyChecklistEntity?,
    streak: Int,
    onToggleTask: (date: String, taskId: String) -> Unit,
    onOpenTask: (route: String, id: String) -> Unit,
    onStartMiniQuiz: () -> Unit
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
        item {
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
                            text = if (progressFraction >= 1f) "مبروك! أكملت جميع مهام اليوم بنجاح" else "استمر في التقدم لإكمال خطة اليوم",
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
            item {
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
        item {
            Text(
                text = "Today's Checklist",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
        }

        // Tasks List
        items(tasks, key = { it.id }) { task ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_item_${task.type}"),
                color = SurfaceDark,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, if (task.isCompleted) EmeraldAccent.copy(alpha = 0.45f) else BorderSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            if (todayChecklist != null) {
                                onToggleTask(todayChecklist.date, task.id)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (task.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                            contentDescription = "Toggle completion",
                            tint = if (task.isCompleted) EmeraldAccent else TextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = task.titleEn,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (task.isCompleted) TextMuted else TextPrimary
                            )
                        )
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

                    if (task.type == "quiz") {
                        Button(
                            onClick = onStartMiniQuiz,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = BackgroundDark),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_start_daily_quiz")
                        ) {
                            Text(if (task.isCompleted) "Retake" else "Take Quiz", fontWeight = FontWeight.Bold)
                        }
                    } else if (task.linkedRoute.isNotBlank()) {
                        IconButton(onClick = { onOpenTask(task.linkedRoute, task.linkedId) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Go to task", tint = GoldAccent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlyPlanTabContent(
    profile: StudyPlanProfileEntity?,
    currentPlan: StudyPlanEntity?,
    isAiGenerating: Boolean,
    aiError: String?,
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
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceDark,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(
                                text = "Month: ${currentPlan?.month ?: "Current"}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = "Level ${currentPlan?.levelStart ?: "B2"} → ${currentPlan?.levelTarget ?: "B2+"}",
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
                                text = if (currentPlan?.source == "ai") "AI Plan" else "Standard Plan",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (currentPlan?.source == "ai") GoldAccent else TextMuted,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (aiError != null) {
                        Text(text = aiError, style = MaterialTheme.typography.labelSmall, color = CoralAccent)
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
                            Text("Generating with AI...", color = GoldAccent)
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
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceDark,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Daily Time Allocation",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Cards: ${template.review_cards_min}m", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        Text("Words: ${template.new_words}m", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        Text("Grammar: ${template.grammar_min}m", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        Text("Reading: ${template.reading_min}m", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        Text("Quiz: ${template.quiz_min}m", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Arabic Coaching Tips
        if (!currentPlan?.tipsAr.isNullOrBlank()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SurfaceDark,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Coaching Tips",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = GoldAccent
                        )
                        BidiText(text = currentPlan?.tipsAr ?: "", style = ArabicTypography.bodySmall, color = TextPrimary)
                    }
                }
            }
        }

        // Weekly Goals
        item {
            Text(
                text = "Weekly Milestones",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
        }

        items(weeks) { week ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceDark,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = "Week ${week.week}: ${week.focus}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }
                    BidiText(text = week.focus_ar, style = ArabicTypography.labelSmall, color = TextSecondary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Words: ${week.goals.new_words}", style = MaterialTheme.typography.labelSmall, color = GoldAccent)
                        Text("Grammar: ${week.goals.grammar_lessons}", style = MaterialTheme.typography.labelSmall, color = GoldAccent)
                        Text("Reading: ${week.goals.reading_texts}", style = MaterialTheme.typography.labelSmall, color = GoldAccent)
                        Text("Sentences: ${week.goals.sentences}", style = MaterialTheme.typography.labelSmall, color = GoldAccent)
                        Text("Quizzes: ${week.goals.quizzes}", style = MaterialTheme.typography.labelSmall, color = GoldAccent)
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsTabContent(
    monthlyChecklists: List<DailyChecklistEntity>,
    quizHistory: List<QuizResultEntity>,
    weakAreas: WeakAreasState,
    onStartWeeklyQuiz: () -> Unit,
    onOpenTask: (route: String, id: String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("analytics_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Weekly Quiz Banner
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceDark,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, GoldAccentBorder)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
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
                        Text("Start", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Skill Accuracy breakdown from quizzes
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

        // Weak Words Needing Review
        item {
            Text(
                text = "Focus Areas & Weak Words",
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
            items(weakAreas.weakWords) { entry ->
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
