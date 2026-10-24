package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.importer.DataImportRepository
import com.example.data.importer.ImportProgressState
import com.example.data.local.DailyChecklistEntity
import com.example.data.local.DailyTaskItem
import com.example.data.local.DeckEntity
import com.example.data.local.DeckStudyStateEntity
import com.example.data.local.EntryEntity
import com.example.data.local.StudyPlanProfileEntity
import com.example.ui.components.BidiText
import com.example.ui.theme.ArabicTypography
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BackgroundGlowColor
import com.example.ui.theme.BookSpineShape
import com.example.ui.theme.BorderDark
import com.example.ui.theme.BorderHighlight
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.CoralAccentBg
import com.example.ui.theme.CoralAccentBorder
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldAccentBg
import com.example.ui.theme.EyebrowStyle
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldAccentBorder
import com.example.ui.theme.GoldAccentDim
import com.example.ui.theme.OnGoldDark
import com.example.ui.theme.PillShape
import com.example.ui.theme.SkyAccent
import com.example.ui.theme.SkyAccentBg
import com.example.ui.theme.SquircleCardShape
import com.example.ui.theme.SquircleCheckShape
import com.example.ui.theme.SquirclePlanCardShape
import com.example.ui.theme.SquircleReviewShape
import com.example.ui.theme.SquircleTaskShape
import com.example.ui.theme.SquircleTileShape
import com.example.ui.theme.StatValueGridStyle
import com.example.ui.theme.StatValueReviewStyle
import com.example.ui.theme.SurfaceContainerHighDark
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevatedDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextSubtle
import com.example.ui.theme.VioletAccent
import com.example.ui.theme.VioletAccentBg
import com.example.ui.theme.WordBooksSpacing
import com.example.data.plan.LearnerModelSnapshot
import com.example.ui.components.AdaptiveInsightBannerCard
import com.example.ui.components.CefrAndLifecycleCard
import com.example.ui.components.ConqueredWordsTrophyShelf
import kotlinx.serialization.json.Json
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val homeTaskJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

private val HOME_TASKS_CACHE = java.util.concurrent.ConcurrentHashMap<String, List<DailyTaskItem>>()

/**
 * Figma-aligned Dark Learning Dashboard for Home destination.
 * Matches 01-home.png and 02-home-library.png with real database data.
 */
@Composable
fun HomeScreen(
    decks: List<DeckEntity> = emptyList(),
    importState: ImportProgressState = ImportProgressState(),
    totalEntriesCount: Int = 0,
    reviewDueTodayCount: Int = 0,
    studiedTodayCount: Int = 0,
    masteredCount: Int = 0,
    deckStudyStates: List<DeckStudyStateEntity> = emptyList(),
    todayChecklist: DailyChecklistEntity? = null,
    weeklyChecklists: List<DailyChecklistEntity> = emptyList(),
    studyStreak: Int = 0,
    studyPlanProfile: StudyPlanProfileEntity? = null,
    learnerModelSnapshot: LearnerModelSnapshot = LearnerModelSnapshot(),
    onDismissInsight: (String) -> Unit = {},
    onAcceptLevelUp: (String) -> Unit = {},
    onOpenDeck: (String) -> Unit = {},
    onOpenFlashcardsForDeck: (String) -> Unit = {},
    onOpenPlanScreen: () -> Unit = {},
    onNavigateToTask: (route: String, linkedId: String?) -> Unit = { _, _ -> onOpenPlanScreen() },
    onToggleTaskCompleted: (date: String, taskId: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    val scrollState = rememberScrollState()

    val studyStateByDeck = remember(deckStudyStates) { deckStudyStates.associateBy { it.deckId } }
    val mostRecentStudyState = remember(deckStudyStates) {
        deckStudyStates.firstOrNull {
            it.deckId != DataImportRepository.DUE_TODAY_DECK_ID
        }
    }
    val continueDeckId = remember(mostRecentStudyState) { mostRecentStudyState?.deckId ?: "oxford5000" }
    val continueDeck = remember(decks, continueDeckId) { decks.find { it.id == continueDeckId } }
    val continueCardPosition = remember(mostRecentStudyState) { (mostRecentStudyState?.lastCardIndex ?: 0) + 1 }
    val continueTotalCards = remember(continueDeck) {
        (continueDeck?.uniqueWordCount ?: continueDeck?.rowCount ?: 0).coerceAtLeast(1)
    }

    // Filter valid visible decks (hide internal saved words deck from library cards)
    val displayDecks = remember(decks) {
        decks.filter { it.id != DataImportRepository.SAVED_WORDS_DECK_ID && it.rowCount > 0 }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(BackgroundGlowColor, BackgroundDark),
                    center = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY / 2, 0f),
                    radius = 900f
                )
            )
            .testTag("screen_home"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = if (isTablet) 1100.dp else 720.dp)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Welcome section: Date eyebrow, Greeting H1, Arabic subtitle, Coral streak badge, Level & Goal chips, and 7-day progress strip
            WelcomeSection(
                studyStreak = studyStreak,
                studyPlanProfile = studyPlanProfile,
                todayChecklist = todayChecklist,
                weeklyChecklists = weeklyChecklists,
                studiedTodayCount = studiedTodayCount
            )

            // 1b. Data-backed Adaptive Insight Banner (Cold-Start guarded < 20 attempts)
            learnerModelSnapshot.activeInsight?.let { insight ->
                AdaptiveInsightBannerCard(
                    insight = insight,
                    onDismiss = onDismissInsight,
                    onAcceptLevelUp = onAcceptLevelUp,
                    onNavigateToRoute = { route -> onNavigateToTask(route, null) }
                )
            }

            if (isTablet) {
                // Two-column tablet layout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        TodayPlanCard(
                            todayChecklist = todayChecklist,
                            onOpenPlanScreen = onOpenPlanScreen,
                            onNavigateToTask = onNavigateToTask,
                            onToggleTaskCompleted = onToggleTaskCompleted
                        )

                        CefrAndLifecycleCard(
                            readiness = learnerModelSnapshot.cefrReadiness,
                            funnel = learnerModelSnapshot.funnel,
                            onAcceptLevelUp = onAcceptLevelUp
                        )

                        if (learnerModelSnapshot.conqueredWords.isNotEmpty()) {
                            ConqueredWordsTrophyShelf(
                                conqueredWords = learnerModelSnapshot.conqueredWords
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ReviewDueSection(
                            reviewDueTodayCount = reviewDueTodayCount,
                            continueDeckId = continueDeckId,
                            onOpenFlashcardsForDeck = onOpenFlashcardsForDeck
                        )

                        ContinueLearningSection(
                            continueDeck = continueDeck,
                            continueCardPosition = continueCardPosition,
                            continueTotalCards = continueTotalCards,
                            continueDeckId = continueDeckId,
                            totalDecksCount = displayDecks.size,
                            studyStateByDeck = studyStateByDeck,
                            onOpenDeck = onOpenDeck,
                            onOpenFlashcardsForDeck = onOpenFlashcardsForDeck
                        )
                    }
                }
            } else {
                // Single-column phone layout: Actionable study cards first, analytics & milestones below
                TodayPlanCard(
                    todayChecklist = todayChecklist,
                    onOpenPlanScreen = onOpenPlanScreen,
                    onNavigateToTask = onNavigateToTask,
                    onToggleTaskCompleted = onToggleTaskCompleted
                )

                CefrAndLifecycleCard(
                    readiness = learnerModelSnapshot.cefrReadiness,
                    funnel = learnerModelSnapshot.funnel,
                    onAcceptLevelUp = onAcceptLevelUp
                )

                if (learnerModelSnapshot.conqueredWords.isNotEmpty()) {
                    ConqueredWordsTrophyShelf(
                        conqueredWords = learnerModelSnapshot.conqueredWords
                    )
                }

                ReviewDueSection(
                    reviewDueTodayCount = reviewDueTodayCount,
                    continueDeckId = continueDeckId,
                    onOpenFlashcardsForDeck = onOpenFlashcardsForDeck
                )

                ContinueLearningSection(
                    continueDeck = continueDeck,
                    continueCardPosition = continueCardPosition,
                    continueTotalCards = continueTotalCards,
                    continueDeckId = continueDeckId,
                    totalDecksCount = displayDecks.size,
                    studyStateByDeck = studyStateByDeck,
                    onOpenDeck = onOpenDeck,
                    onOpenFlashcardsForDeck = onOpenFlashcardsForDeck
                )
            }

            Spacer(modifier = Modifier.height(WordBooksSpacing.large))
        }
    }
}

/**
 * Section 1: Welcome Section
 * Date eyebrow, personalized greeting H1, Arabic subtitle on its own line, and streak badge pill.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WelcomeSection(
    studyStreak: Int,
    studyPlanProfile: StudyPlanProfileEntity? = null,
    todayChecklist: DailyChecklistEntity? = null,
    weeklyChecklists: List<DailyChecklistEntity> = emptyList(),
    studiedTodayCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val dateString = remember {
        try {
            SimpleDateFormat("EEEE, MMMM d", Locale.ENGLISH).format(Date())
        } catch (_: Exception) {
            "Today's Learning"
        }
    }

    val greeting = remember {
        val hour = Calendar.getInstance()[Calendar.HOUR_OF_DAY]
        when (hour) {
            in 5..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = SurfaceDark,
        shape = SquircleCardShape,
        border = BorderStroke(1.dp, BorderDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = dateString.uppercase(Locale.ENGLISH),
                        style = EyebrowStyle
                    )
                    Text(
                        text = "$greeting, Learner",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = TextPrimary
                    )
                    BidiText(
                        text = "جاهز لجلسة تعلم جديدة اليوم؟",
                        style = ArabicTypography.bodyMedium.copy(
                            fontSize = 13.sp
                        ),
                        color = TextSecondary,
                        fillMaxWidth = true,
                        textAlign = TextAlign.Right
                    )
                }

                // Streak Badge Pill
                val streakCount = if (studyStreak > 0) studyStreak else 1
                Surface(
                    color = CoralAccentBg,
                    shape = PillShape,
                    border = BorderStroke(1.dp, CoralAccentBorder),
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .testTag("home_study_streak_badge")
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .heightIn(min = 26.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = CoralAccent,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "$streakCount day streak",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = CoralAccent
                        )
                    }
                }
            }

            // Personalized Profile Row (CEFR Level, Goal, Daily Target) wrapped in FlowRow
            if (studyPlanProfile != null) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // 1. CEFR Level Badge
                    val level = studyPlanProfile.level.ifBlank { "B2" }
                    val levelColor = when (level) {
                        "A1", "A2" -> EmeraldAccent
                        "B1", "B2" -> SkyAccent
                        else -> VioletAccent
                    }
                    val levelBg = when (level) {
                        "A1", "A2" -> EmeraldAccentBg
                        "B1", "B2" -> SkyAccentBg
                        else -> VioletAccentBg
                    }
                    val levelArLabel = when (level) {
                        "A1" -> "مبتدئ"
                        "A2" -> "ابتدائي"
                        "B1" -> "متوسط"
                        "B2" -> "فوق المتوسط"
                        "C1" -> "متقدم"
                        "C2" -> "إتقان"
                        else -> level
                    }
                    Surface(
                        color = levelBg,
                        shape = PillShape,
                        border = BorderStroke(1.dp, levelColor.copy(alpha = 0.5f)),
                        modifier = Modifier.testTag("home_cefr_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(levelColor)
                            )
                            Text(
                                text = "$level · $levelArLabel",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = levelColor
                            )
                        }
                    }

                    // 2. Goal Chip
                    val (goalEn, goalAr) = when (studyPlanProfile.goal) {
                        "ielts" -> "IELTS Academic" to "تحضير الآيلتس"
                        "it_career" -> "IT Career" to "تقنية المعلومات"
                        else -> "General Fluency" to "طلاقة عامة"
                    }
                    Surface(
                        color = SurfaceElevatedDark,
                        shape = PillShape,
                        border = BorderStroke(1.dp, BorderDark),
                        modifier = Modifier.testTag("home_goal_chip")
                    ) {
                        Text(
                            text = "$goalEn · $goalAr",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp
                            ),
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // 3. Pace Chip
                    Surface(
                        color = SurfaceElevatedDark,
                        shape = PillShape,
                        border = BorderStroke(1.dp, BorderDark),
                        modifier = Modifier.testTag("home_pace_chip")
                    ) {
                        Text(
                            text = "${studyPlanProfile.minutesPerDay} min/day",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp
                            ),
                            color = TextMuted,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = BorderSubtle, thickness = 1.dp)

            // 7-day Weekly Progress Strip directly integrated into the Welcome Card
            val dayNames = listOf("M", "T", "W", "T", "F", "S", "S")
            val currentDayIndex = remember {
                val cal = Calendar.getInstance()
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - 2
            }
            val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
            val baseCal = remember { Calendar.getInstance() }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dayNames.forEachIndexed { index, dayLetter ->
                    val isToday = index == currentDayIndex
                    val calForDay = Calendar.getInstance().apply {
                        time = baseCal.time
                        add(Calendar.DAY_OF_MONTH, index - currentDayIndex)
                    }
                    val dateStr = sdf.format(calForDay.time)
                    val checklistForDay = weeklyChecklists.firstOrNull { it.date == dateStr } ?: if (isToday) todayChecklist else null
                    val isCompleted = checklistForDay?.isCompleted == true || (checklistForDay != null && checklistForDay.totalCount > 0 && checklistForDay.completedCount >= checklistForDay.totalCount)
                    val hasPartialActivity = (checklistForDay != null && checklistForDay.completedCount > 0) || (isToday && studiedTodayCount > 0)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = dayLetter,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                color = if (isToday) GoldAccent else TextSecondary
                            )
                        )
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isCompleted -> EmeraldAccent.copy(alpha = 0.2f)
                                        hasPartialActivity -> GoldAccentDim
                                        isToday -> GoldAccentDim
                                        else -> SurfaceElevatedDark
                                    }
                                )
                                .border(
                                    width = 1.dp,
                                    color = when {
                                        isCompleted -> EmeraldAccent
                                        isToday -> GoldAccent
                                        hasPartialActivity -> GoldAccentBorder
                                        else -> BorderDark
                                    },
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCompleted) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = EmeraldAccent,
                                    modifier = Modifier.size(13.dp)
                                )
                            } else if (hasPartialActivity || isToday) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(GoldAccent)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Section 2: Today's Study Plan Card
 * Synchronized with PlanScreen: renders personalized tasks, progress fraction, level badges, and action buttons.
 */
@Composable
private fun TodayPlanCard(
    todayChecklist: DailyChecklistEntity?,
    onOpenPlanScreen: () -> Unit,
    onNavigateToTask: (route: String, linkedId: String?) -> Unit = { _, _ -> onOpenPlanScreen() },
    onToggleTaskCompleted: (date: String, taskId: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val totalTasks = todayChecklist?.totalCount?.takeIf { it > 0 } ?: 4
    val completedTasks = todayChecklist?.completedCount ?: 0
    val progressFraction = (completedTasks.toFloat() / totalTasks.toFloat()).coerceIn(0f, 1f)
    val todayDateStr = remember(todayChecklist?.date) {
        todayChecklist?.date ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    val tasks: List<DailyTaskItem> = remember(todayChecklist?.tasksJson) {
        val raw = todayChecklist?.tasksJson
        if (!raw.isNullOrBlank()) {
            HOME_TASKS_CACHE.computeIfAbsent(raw) {
                try {
                    homeTaskJson.decodeFromString<List<DailyTaskItem>>(it)
                } catch (_: Exception) {
                    emptyList()
                }
            }
        } else {
            emptyList()
        }
    }

    val displayTasks = if (tasks.isNotEmpty()) {
        tasks
    } else {
        listOf(
            DailyTaskItem(id = "task_flashcards", type = "flashcards", titleEn = "Review due flashcards", titleAr = "مراجعة البطاقات المستحقة", targetCount = 20, isCompleted = false),
            DailyTaskItem(id = "task_new_words", type = "new_words", titleEn = "Learn 10 new words", titleAr = "تعلم ١٠ كلمات جديدة", targetCount = 10, isCompleted = false),
            DailyTaskItem(id = "task_reading", type = "reading", titleEn = "Read a short story", titleAr = "قراءة قصة قصيرة متدرجة", targetCount = 1, isCompleted = false),
            DailyTaskItem(id = "task_grammar", type = "grammar", titleEn = "Practice grammar lesson", titleAr = "تطبيق على درس القواعد", targetCount = 1, isCompleted = false)
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_today_plan_widget"),
        color = SurfaceDark,
        shape = SquirclePlanCardShape,
        border = BorderStroke(1.dp, BorderHighlight)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(GoldAccentDim, SurfaceDark)
                    )
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Plan Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button, onClick = onOpenPlanScreen),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(SquircleTileShape)
                            .background(GoldAccentDim)
                            .border(1.dp, GoldAccentBorder, SquircleTileShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.EventNote,
                            contentDescription = "Plan",
                            tint = GoldAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Today's Study Plan",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                letterSpacing = (-0.3).sp
                            ),
                            color = TextPrimary
                        )
                        BidiText(
                            text = "خطتك اليومية الذكية",
                            style = ArabicTypography.labelMedium.copy(fontSize = 12.sp),
                            color = TextSecondary,
                            fillMaxWidth = false,
                            textAlign = TextAlign.Start
                        )
                    }
                }

                Text(
                    text = "$completedTasks/$totalTasks",
                    style = StatValueGridStyle.copy(fontSize = 15.sp),
                    color = GoldAccent
                )
            }

            // Progress Bar
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(PillShape),
                color = GoldAccent,
                trackColor = BorderDark
            )

            // Tasks List (synced task rendering)
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                displayTasks.forEach { task ->
                    TaskCheckRow(
                        task = task,
                        onClick = {
                            val targetRoute = when (task.type) {
                                "flashcards" -> "flashcards"
                                "new_words" -> "new_words"
                                "reading" -> "reading"
                                "grammar" -> "grammar"
                                "quiz" -> "quiz"
                                else -> "plan"
                            }
                            onNavigateToTask(targetRoute, task.linkedId.ifBlank { task.id })
                        },
                        onToggleComplete = {
                            onToggleTaskCompleted(todayDateStr, task.id)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Task Row inside Today's Study Plan (Upgraded compact 3-line layout)
 */
@Composable
private fun TaskCheckRow(
    task: DailyTaskItem,
    onClick: () -> Unit,
    onToggleComplete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(SquircleTaskShape)
            .clickable(onClick = onClick)
            .testTag("home_task_item_${task.type}"),
        color = SurfaceElevatedDark,
        shape = SquircleTaskShape,
        border = BorderStroke(1.dp, if (task.isCompleted) EmeraldAccent.copy(alpha = 0.55f) else BorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Compact Task check squircle
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable { onToggleComplete() }
                    .testTag("home_task_checkbox_${task.id}"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (task.isCompleted) EmeraldAccent else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (task.isCompleted) EmeraldAccent else Color(0xFF55555E),
                            shape = RoundedCornerShape(5.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (task.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = BackgroundDark,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            // 3 Compact Lines Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Line 1: English Title + Badges (time, count)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = task.titleEn,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textDecoration = if (task.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else androidx.compose.ui.text.style.TextDecoration.None
                        ),
                        color = if (task.isCompleted) TextMuted else TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (task.estimatedMinutes > 0) {
                        Surface(
                            color = SurfaceDark,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "${task.estimatedMinutes}m",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium, fontSize = 10.sp),
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (task.targetCount > 1) {
                        Surface(
                            color = if (task.isCompleted) EmeraldAccent.copy(alpha = 0.15f) else SurfaceDark,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${task.completedCount}/${task.targetCount}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                color = if (task.isCompleted) EmeraldAccent else GoldAccent,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Line 2: Arabic Subtitle right-aligned (direction: rtl; text-align: right;)
                if (task.titleAr.isNotBlank()) {
                    BidiText(
                        text = task.titleAr,
                        style = ArabicTypography.labelSmall.copy(fontSize = 11.sp),
                        color = if (task.isCompleted) TextMuted else TextSecondary,
                        fillMaxWidth = true,
                        maxLines = 1,
                        textAlign = TextAlign.Right
                    )
                }

                // Line 3: Single concise Metadata line in GoldAccent
                val metaText = when {
                    task.subtitleEn.isNotBlank() && task.reason.isNotBlank() && task.subtitleEn != task.reason ->
                        "${task.subtitleEn} • ${task.reason}"
                    task.subtitleEn.isNotBlank() -> task.subtitleEn
                    task.reason.isNotBlank() -> task.reason
                    else -> ""
                }
                if (metaText.isNotBlank()) {
                    Text(
                        text = metaText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = GoldAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Typed action button with dark text #0B0D12 on GoldAccent #F5B82E (Contrast fix)
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

            val buttonTextColor = if (task.isCompleted) GoldAccent else Color(0xFF0B0D12)

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (task.isCompleted) SurfaceDark else GoldAccent,
                    contentColor = buttonTextColor
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier
                    .heightIn(min = 32.dp)
                    .testTag("btn_home_task_action_${task.type}")
            ) {
                Text(
                    text = actionLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = buttonTextColor
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = buttonTextColor,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}

/**
 * Section 3: Ready for Review (Spaced Repetition)
 */
@Composable
private fun ReviewDueSection(
    reviewDueTodayCount: Int,
    continueDeckId: String,
    onOpenFlashcardsForDeck: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_review_due_card"),
        color = SurfaceDark,
        shape = SquircleReviewShape,
        border = BorderStroke(1.dp, if (reviewDueTodayCount > 0) BorderHighlight else BorderDark)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Big Count block
                Column(
                    modifier = Modifier.widthIn(min = 60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = reviewDueTodayCount.toString(),
                        style = StatValueReviewStyle.copy(fontSize = 24.sp),
                        color = GoldAccent,
                        modifier = Modifier.testTag("home_review_due_count")
                    )
                    Text(
                        text = "due today",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    )
                }

                VerticalDivider(
                    modifier = Modifier.height(38.dp),
                    color = BorderDark,
                    thickness = 1.dp
                )

                // Description Copy (2 lines matching prototype)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "Keep your memory strong",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = TextPrimary
                    )
                    BidiText(
                        text = "حافظ على قوة ذاكرتك واكتمال مراجعاتك",
                        style = ArabicTypography.labelSmall.copy(
                            fontSize = 11.sp
                        ),
                        color = TextSecondary,
                        fillMaxWidth = true,
                        textAlign = TextAlign.Right
                    )
                }
            }

            // Primary Action Button
            Button(
                onClick = {
                    if (reviewDueTodayCount > 0) {
                        onOpenFlashcardsForDeck(DataImportRepository.DUE_TODAY_DECK_ID)
                    } else {
                        onOpenFlashcardsForDeck(continueDeckId)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldAccent,
                    contentColor = Color(0xFF0B0D12)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("home_review_due_button")
            ) {
                Text(
                    text = if (reviewDueTodayCount > 0) "Start review" else "Practice more cards",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF0B0D12)
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFF0B0D12),
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

/**
 * Section 4: Compact Continue Learning Section
 * Replaces the long 13-book list with a smart unified active book card + "All X books →" link.
 */
@Composable
private fun ContinueLearningSection(
    continueDeck: DeckEntity?,
    continueCardPosition: Int,
    continueTotalCards: Int,
    continueDeckId: String,
    totalDecksCount: Int,
    studyStateByDeck: Map<String, DeckStudyStateEntity>,
    onOpenDeck: (String) -> Unit,
    onOpenFlashcardsForDeck: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeDeck = continueDeck

    val formattedTotalCards = try {
        NumberFormat.getNumberInstance(Locale.US).format(continueTotalCards)
    } catch (_: Exception) {
        continueTotalCards.toString()
    }

    val (accentColor, accentBg) = when (activeDeck?.id) {
        "oxford5000" -> Pair(EmeraldAccent, EmeraldAccentBg)
        "everyday" -> Pair(SkyAccent, SkyAccentBg)
        "idioms" -> Pair(VioletAccent, VioletAccentBg)
        else -> Pair(GoldAccent, GoldAccentDim)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Section Header (Single row: "Continue learning" on left, "All X books →" on right)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Continue learning",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = TextPrimary
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenDeck("") }
                    .padding(vertical = 4.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "All $totalDecksCount books",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    ),
                    color = GoldAccent
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = GoldAccent,
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        // Active Deck Compact Card
        if (activeDeck != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(SquircleCardShape)
                    .clickable(role = Role.Button) { onOpenFlashcardsForDeck(continueDeckId) }
                    .testTag("home_continue_learning_card"),
                color = SurfaceDark,
                shape = SquircleCardShape,
                border = BorderStroke(1.dp, BorderDark)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Book Cover rounded square
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(accentBg)
                            .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Deck Info
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = activeDeck.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            Row(
                                modifier = Modifier.testTag("home_continue_position_text"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Card $continueCardPosition / $formattedTotalCards",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = GoldAccent
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        if (activeDeck.arabicSubtitle.isNotBlank()) {
                            BidiText(
                                text = activeDeck.arabicSubtitle,
                                style = ArabicTypography.labelSmall.copy(
                                    fontSize = 11.sp
                                ),
                                color = TextSecondary,
                                fillMaxWidth = true,
                                maxLines = 1,
                                textAlign = TextAlign.Right
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Section 6: Weekly Progress Summary Card
 */
@Composable
private fun WeeklyProgressSummaryCard(
    studiedTodayCount: Int,
    studyStreak: Int,
    masteredCount: Int,
    totalEntriesCount: Int,
    todayChecklist: DailyChecklistEntity? = null,
    weeklyChecklists: List<DailyChecklistEntity> = emptyList(),
    modifier: Modifier = Modifier
) {
    val dayNames = listOf("M", "T", "W", "T", "F", "S", "S")
    val currentDayIndex = remember {
        val cal = Calendar.getInstance()
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - 2
    }

    val goalCompleted = todayChecklist?.completedCount ?: studiedTodayCount
    val goalTarget = todayChecklist?.totalCount ?: 20

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = SurfaceDark,
        shape = SquircleCardShape,
        border = BorderStroke(1.dp, BorderDark)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Weekly Progress Summary",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = TextPrimary
                )
                Text(
                    text = "Daily Goal: $goalCompleted / $goalTarget",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = GoldAccent
                    )
                )
            }

            // 7-day row
            val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
            val baseCal = remember { Calendar.getInstance() }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dayNames.forEachIndexed { index, dayLetter ->
                    val isToday = index == currentDayIndex
                    val isPast = index <= currentDayIndex

                    val calForDay = Calendar.getInstance().apply {
                        time = baseCal.time
                        add(Calendar.DAY_OF_MONTH, index - currentDayIndex)
                    }
                    val dateStr = sdf.format(calForDay.time)
                    val checklistForDay = weeklyChecklists.firstOrNull { it.date == dateStr } ?: if (isToday) todayChecklist else null
                    val isCompleted = checklistForDay?.isCompleted == true || (checklistForDay != null && checklistForDay.totalCount > 0 && checklistForDay.completedCount >= checklistForDay.totalCount)
                    val hasPartialActivity = (checklistForDay != null && checklistForDay.completedCount > 0) || (isToday && studiedTodayCount > 0)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = dayLetter,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                color = if (isToday) GoldAccent else TextSecondary
                            )
                        )
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isCompleted -> EmeraldAccent.copy(alpha = 0.2f)
                                        hasPartialActivity -> GoldAccentDim
                                        isToday -> GoldAccentDim
                                        else -> SurfaceElevatedDark
                                    }
                                )
                                .border(
                                    width = 1.dp,
                                    color = when {
                                        isCompleted -> EmeraldAccent
                                        isToday -> GoldAccent
                                        hasPartialActivity -> GoldAccentBorder
                                        else -> BorderDark
                                    },
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCompleted) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = EmeraldAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                            } else if (hasPartialActivity || isToday) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(GoldAccent)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = BorderDark, thickness = 1.dp)

            // Summary row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Mastered: $masteredCount",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                )
                Text(
                    text = "Vocabulary Library: $totalEntriesCount words",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                )
            }
        }
    }
}
