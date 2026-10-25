package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.importer.DataImportRepository
import com.example.data.importer.ImportProgressState
import com.example.data.local.DailyChecklistEntity
import com.example.data.local.DailyTaskItem
import com.example.data.local.DeckEntity
import com.example.data.local.DeckStudyStateEntity
import com.example.data.local.StudyPlanProfileEntity
import com.example.data.plan.LearnerModelSnapshot
import com.example.data.plan.LifecycleStageWordItem
import com.example.ui.components.AdaptiveInsightBannerCard
import com.example.ui.components.BidiText
import com.example.ui.components.CefrAndLifecycleCard
import com.example.ui.components.ConqueredWordsTrophyShelf
import com.example.ui.theme.ArabicTypography
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderDark
import com.example.ui.theme.BorderMedium
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.CoralAccentBg
import com.example.ui.theme.CoralAccentBorder
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldAccentBg
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldAccentBorder
import com.example.ui.theme.GoldAccentDim
import com.example.ui.theme.OnGoldDark
import com.example.ui.theme.PillShape
import com.example.ui.theme.SkyAccent
import com.example.ui.theme.SkyAccentBg
import com.example.ui.theme.SkyAccentBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevatedDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletAccent
import com.example.ui.theme.VioletAccentBg
import com.example.ui.theme.WordBooksSpacing
import kotlinx.serialization.json.Json
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val homeTaskJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

private val HOME_TASKS_CACHE = java.util.concurrent.ConcurrentHashMap<String, List<DailyTaskItem>>()

// Reactive palette delegates matching Dark and Light mode seamlessly
private val ProtoBgMain: Color get() = BackgroundDark
private val ProtoBgCard: Color get() = SurfaceDark
private val ProtoBorderSubtle: Color get() = BorderDark
private val ProtoGold: Color get() = GoldAccent
private val ProtoGoldDim: Color get() = GoldAccentDim
private val ProtoGoldBorder: Color get() = GoldAccentBorder
private val ProtoCyan: Color get() = SkyAccent
private val ProtoCyanDim: Color get() = SkyAccentBg
private val ProtoCyanBorder: Color get() = SkyAccentBorder
private val ProtoOnGoldDark: Color get() = OnGoldDark

/**
 * Optimized Dark Learning Dashboard for Home destination.
 * Matches the HTML/CSS target prototype with zero scroll fatigue, high-contrast gold buttons,
 * integrated 7-day progress strip, 3-line task cards, merged CEFR + 2-column Lifecycle card,
 * and active book resume card.
 */
@Composable
fun HomeScreen(
    decks: List<DeckEntity> = emptyList(),
    importState: ImportProgressState = ImportProgressState(),
    totalEntriesCount: Int = 0,
    reviewDueTodayCount: Int = 0,
    reviewDueNowCount: Int = 0,
    waitingLaterTodayCount: Int = 0,
    nextWaitingDueTimestamp: Long? = null,
    studiedTodayCount: Int = 0,
    newWordsLearnedTodayCount: Int = 0,
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
    onLoadStageWords: (suspend (String) -> List<LifecycleStageWordItem>)? = null,
    onSpeakWord: ((String) -> Unit)? = null,
    onOpenDictionaryForStage: ((String) -> Unit)? = null,
    onStartStudying: ((String) -> Unit)? = null,
    onStartEarlyReview: () -> Unit = {},
    onReviewTodayLearnedWords: () -> Unit = {},
    onLearnExtraNewWords: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    val scrollState = rememberScrollState()

    var showNewWordsCompletedDialog by remember { mutableStateOf(false) }
    var completedWordsCount by remember { mutableIntStateOf(0) }

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

    // Filter valid visible decks (hide internal saved words deck from library count)
    val displayDecks = remember(decks) {
        decks.filter { it.id != DataImportRepository.SAVED_WORDS_DECK_ID && it.rowCount > 0 }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ProtoBgMain)
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
            // 1. Hero Card with Integrated 7-Day Weekly Strip
            WelcomeSection(
                studyStreak = studyStreak,
                studyPlanProfile = studyPlanProfile,
                todayChecklist = todayChecklist,
                weeklyChecklists = weeklyChecklists,
                studiedTodayCount = studiedTodayCount
            )

            // 2. Compact Pace / Adaptive Insight Banner
            learnerModelSnapshot.activeInsight?.let { insight ->
                AdaptiveInsightBannerCard(
                    insight = insight,
                    onDismiss = onDismissInsight,
                    onAcceptLevelUp = onAcceptLevelUp,
                    onNavigateToRoute = { route -> onNavigateToTask(route, null) }
                )
            }

            if (isTablet) {
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
                            onToggleTaskCompleted = onToggleTaskCompleted,
                            onOpenNewWordsCompletedOptions = { count ->
                                completedWordsCount = count
                                showNewWordsCompletedDialog = true
                            },
                            newWordsLearnedTodayCount = newWordsLearnedTodayCount
                        )

                        CefrAndLifecycleCard(
                            readiness = learnerModelSnapshot.cefrReadiness,
                            funnel = learnerModelSnapshot.funnel,
                            onAcceptLevelUp = onAcceptLevelUp,
                            onLoadStageWords = onLoadStageWords,
                            onSpeakWord = onSpeakWord,
                            onOpenDictionaryForStage = onOpenDictionaryForStage,
                            onStartStudying = onStartStudying
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
                            reviewDueNowCount = reviewDueNowCount,
                            waitingLaterTodayCount = waitingLaterTodayCount,
                            nextWaitingDueTimestamp = nextWaitingDueTimestamp,
                            continueDeckId = continueDeckId,
                            onOpenFlashcardsForDeck = onOpenFlashcardsForDeck,
                            onStartEarlyReview = onStartEarlyReview
                        )

                        ContinueLearningSection(
                            continueDeck = continueDeck,
                            continueCardPosition = continueCardPosition,
                            continueTotalCards = continueTotalCards,
                            continueDeckId = continueDeckId,
                            totalDecksCount = displayDecks.size.coerceAtLeast(13),
                            studyStateByDeck = studyStateByDeck,
                            onOpenDeck = onOpenDeck,
                            onOpenFlashcardsForDeck = onOpenFlashcardsForDeck
                        )
                    }
                }
            } else {
                // Single-column phone layout matching prototype order
                TodayPlanCard(
                    todayChecklist = todayChecklist,
                    onOpenPlanScreen = onOpenPlanScreen,
                    onNavigateToTask = onNavigateToTask,
                    onToggleTaskCompleted = onToggleTaskCompleted,
                    onOpenNewWordsCompletedOptions = { count ->
                        completedWordsCount = count
                        showNewWordsCompletedDialog = true
                    },
                    newWordsLearnedTodayCount = newWordsLearnedTodayCount
                )

                CefrAndLifecycleCard(
                    readiness = learnerModelSnapshot.cefrReadiness,
                    funnel = learnerModelSnapshot.funnel,
                    onAcceptLevelUp = onAcceptLevelUp,
                    onLoadStageWords = onLoadStageWords,
                    onSpeakWord = onSpeakWord,
                    onOpenDictionaryForStage = onOpenDictionaryForStage,
                    onStartStudying = onStartStudying
                )

                if (learnerModelSnapshot.conqueredWords.isNotEmpty()) {
                    ConqueredWordsTrophyShelf(
                        conqueredWords = learnerModelSnapshot.conqueredWords
                    )
                }

                ReviewDueSection(
                    reviewDueNowCount = reviewDueNowCount,
                    waitingLaterTodayCount = waitingLaterTodayCount,
                    nextWaitingDueTimestamp = nextWaitingDueTimestamp,
                    continueDeckId = continueDeckId,
                    onOpenFlashcardsForDeck = onOpenFlashcardsForDeck,
                    onStartEarlyReview = onStartEarlyReview
                )

                ContinueLearningSection(
                    continueDeck = continueDeck,
                    continueCardPosition = continueCardPosition,
                    continueTotalCards = continueTotalCards,
                    continueDeckId = continueDeckId,
                    totalDecksCount = if (displayDecks.isNotEmpty()) displayDecks.size else 13,
                    studyStateByDeck = studyStateByDeck,
                    onOpenDeck = onOpenDeck,
                    onOpenFlashcardsForDeck = onOpenFlashcardsForDeck
                )
            }

            Spacer(modifier = Modifier.height(WordBooksSpacing.large))
        }
    }

    if (showNewWordsCompletedDialog) {
        NewWordsCompletedBottomSheet(
            completedCount = maxOf(completedWordsCount, newWordsLearnedTodayCount),
            onDismiss = { showNewWordsCompletedDialog = false },
            onReviewTodayLearnedWords = onReviewTodayLearnedWords,
            onLearnExtraNewWords = onLearnExtraNewWords
        )
    }
}

/**
 * Section 1: Hero Card + Integrated 7-Day Weekly Strip (`M T W T F S S`)
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
        color = Color.Transparent,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, ProtoBorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(SurfaceElevatedDark, SurfaceDark)
                    )
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Row: Date + Greeting + Arabic Subtitle on left, Streak Pill on right
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
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        color = TextMuted
                    )
                    Text(
                        text = "$greeting, Learner",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = TextPrimary
                    )
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(
                            text = "جاهز لجلسة تعلم جديدة اليوم؟",
                            style = ArabicTypography.bodyMedium.copy(
                                fontSize = 13.sp
                            ),
                            color = TextSecondary
                        )
                    }
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
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = CoralAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "$streakCount day streak",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.5.sp
                            ),
                            color = CoralAccent
                        )
                    }
                }
            }

            // Personalized Profile Pills Row (CEFR Level, Goal, Daily Target)
            if (studyPlanProfile != null) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // 1. CEFR Level Pill
                    val level = studyPlanProfile.level.ifBlank { "B2" }
                    val levelColor = when (level) {
                        "A1", "A2" -> EmeraldAccent
                        "B1", "B2" -> ProtoCyan
                        else -> VioletAccent
                    }
                    val levelBg = when (level) {
                        "A1", "A2" -> EmeraldAccentBg
                        "B1", "B2" -> ProtoCyanDim
                        else -> VioletAccentBg
                    }
                    val levelBorder = when (level) {
                        "A1", "A2" -> EmeraldAccent.copy(alpha = 0.35f)
                        "B1", "B2" -> ProtoCyanBorder
                        else -> VioletAccent.copy(alpha = 0.35f)
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
                        border = BorderStroke(1.dp, levelBorder),
                        modifier = Modifier.testTag("home_cefr_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.5.dp),
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
                                    fontWeight = FontWeight.SemiBold,
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
                        "medical" -> "Medical" to "الطب والرعاية"
                        else -> "General Fluency" to "طلاقة عامة"
                    }
                    Surface(
                        color = SurfaceElevatedDark,
                        shape = PillShape,
                        border = BorderStroke(1.dp, ProtoBorderSubtle),
                        modifier = Modifier.testTag("home_goal_chip")
                    ) {
                        Text(
                            text = "$goalEn · $goalAr",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp
                            ),
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.5.dp)
                        )
                    }

                    // 3. Pace Chip
                    Surface(
                        color = SurfaceElevatedDark,
                        shape = PillShape,
                        border = BorderStroke(1.dp, ProtoBorderSubtle),
                        modifier = Modifier.testTag("home_pace_chip")
                    ) {
                        Text(
                            text = "${studyPlanProfile.minutesPerDay} min/day",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp
                            ),
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.5.dp)
                        )
                    }
                }
            }

            // Subtle top divider before 7-day strip
            HorizontalDivider(color = BorderSubtle, thickness = 1.dp)

            // Integrated 7-Day Weekly Progress Strip (M T W T F S S)
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                dayNames.forEachIndexed { index, dayLetter ->
                    val isToday = index == currentDayIndex
                    val calForDay = Calendar.getInstance().apply {
                        time = baseCal.time
                        add(Calendar.DAY_OF_MONTH, index - currentDayIndex)
                    }
                    val dateStr = sdf.format(calForDay.time)
                    val checklistForDay = weeklyChecklists.firstOrNull { it.date == dateStr }
                        ?: if (isToday) todayChecklist else null
                    val isCompleted = checklistForDay?.isCompleted == true ||
                        (checklistForDay != null && checklistForDay.totalCount > 0 && checklistForDay.completedCount >= checklistForDay.totalCount)
                    val hasPartialActivity = (checklistForDay != null && checklistForDay.completedCount > 0) ||
                        (isToday && studiedTodayCount > 0)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = dayLetter,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.5.sp,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isToday) ProtoGold else TextMuted
                        )
                        Box(
                            modifier = Modifier
                                .size(25.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isCompleted -> EmeraldAccent.copy(alpha = 0.2f)
                                        isToday || hasPartialActivity -> ProtoGoldDim
                                        else -> Color.Transparent
                                    }
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = when {
                                        isCompleted -> EmeraldAccent
                                        isToday -> ProtoGold
                                        hasPartialActivity -> ProtoGoldBorder
                                        else -> BorderMedium
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
                            } else if (isToday || hasPartialActivity) {
                                Box(
                                    modifier = Modifier
                                        .size(6.5.dp)
                                        .clip(CircleShape)
                                        .background(ProtoGold)
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
 * Section 3: Today's Study Plan Card
 * Compact 3-line task cards with zero title truncation and high-contrast #0B0D12 text on #F5B82E gold buttons.
 */
@Composable
private fun TodayPlanCard(
    todayChecklist: DailyChecklistEntity?,
    onOpenPlanScreen: () -> Unit,
    onNavigateToTask: (route: String, linkedId: String?) -> Unit = { _, _ -> onOpenPlanScreen() },
    onToggleTaskCompleted: (date: String, taskId: String) -> Unit = { _, _ -> },
    onOpenNewWordsCompletedOptions: (Int) -> Unit = {},
    newWordsLearnedTodayCount: Int = 0,
    modifier: Modifier = Modifier
) {
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

    val displayTasks = remember(tasks, newWordsLearnedTodayCount) {
        val baseTasks = if (tasks.isNotEmpty()) {
            tasks
        } else {
            listOf(
                DailyTaskItem(
                    id = "task_flashcards",
                    type = "flashcards",
                    titleEn = "Flashcards Review",
                    titleAr = "مراجعة البطاقات التعليمية (10 بطاقات)",
                    subtitleEn = "Oxford 5000 • Spaced Retention",
                    targetCount = 10,
                    estimatedMinutes = 2,
                    isCompleted = false
                ),
                DailyTaskItem(
                    id = "task_new_words",
                    type = "new_words",
                    titleEn = "Learn 10 New Words",
                    titleAr = "تعلم 10 مفردات جديدة (مستوى B2)",
                    subtitleEn = "Oxford 5000 • Personalized Pace",
                    targetCount = 10,
                    estimatedMinutes = 8,
                    isCompleted = false
                ),
                DailyTaskItem(
                    id = "task_grammar",
                    type = "grammar",
                    titleEn = "Grammar: Third Conditional",
                    titleAr = "قواعد: الشرط الثالث",
                    subtitleEn = "Conditionals • B2 Lesson",
                    targetCount = 1,
                    estimatedMinutes = 7,
                    isCompleted = false
                ),
                DailyTaskItem(
                    id = "task_reading",
                    type = "reading",
                    titleEn = "Reading: The Missed Interview",
                    titleAr = "قراءة: المقابلة الفائتة",
                    subtitleEn = "75% readable • 6 focus words",
                    targetCount = 1,
                    estimatedMinutes = 5,
                    isCompleted = false
                ),
                DailyTaskItem(
                    id = "task_daily_quiz",
                    type = "quiz",
                    titleEn = "Daily Mini-Quiz",
                    titleAr = "الاختبار اليومي (١٠ أسئلة)",
                    subtitleEn = "5 Vocab • 3 Grammar • 2 Reading",
                    targetCount = 10,
                    estimatedMinutes = 5,
                    isCompleted = false
                )
            )
        }
        if (newWordsLearnedTodayCount <= 0) {
            baseTasks
        } else {
            baseTasks.map { task ->
                if (task.type == "new_words" && newWordsLearnedTodayCount > task.completedCount) {
                    val effTarget = maxOf(task.targetCount, newWordsLearnedTodayCount)
                    val isDone = task.isCompleted || (newWordsLearnedTodayCount >= task.targetCount)
                    task.copy(
                        completedCount = newWordsLearnedTodayCount,
                        targetCount = effTarget,
                        isCompleted = isDone,
                        titleEn = task.titleEn.replaceFirst(Regex("\\d+"), effTarget.toString()),
                        titleAr = task.titleAr.replaceFirst(Regex("\\d+"), effTarget.toString()),
                        estimatedMinutes = maxOf(task.estimatedMinutes, (effTarget * 0.75f).roundToInt())
                    )
                } else {
                    task
                }
            }
        }
    }

    val totalTasks = todayChecklist?.totalCount?.takeIf { it > 0 } ?: displayTasks.size
    val completedTasks = todayChecklist?.completedCount ?: displayTasks.count { it.isCompleted }
    val progressFraction = (completedTasks.toFloat() / totalTasks.toFloat()).coerceIn(0f, 1f)
    val todayDateStr = remember(todayChecklist?.date) {
        todayChecklist?.date ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_today_plan_widget"),
        color = SurfaceDark,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, ProtoGoldBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(ProtoGoldDim)
                            .border(1.dp, ProtoGoldBorder, RoundedCornerShape(11.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.EventNote,
                            contentDescription = "Plan",
                            tint = ProtoGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = "Today's Study Plan",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = TextPrimary
                        )
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Text(
                                text = "خطتك اليومية الذكية",
                                style = ArabicTypography.labelMedium.copy(fontSize = 12.sp),
                                color = TextSecondary
                            )
                        }
                    }
                }

                Text(
                    text = "$completedTasks/$totalTasks",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = ProtoGold
                )
            }

            // Progress Bar
            LinearProgressIndicator(
                progress = { progressFraction.coerceAtLeast(if (completedTasks == 0) 0.06f else 0f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(PillShape),
                color = ProtoGold,
                trackColor = SurfaceElevatedDark
            )

            // Compact 3-Line Task Cards List
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                displayTasks.forEach { task ->
                    val isCompletedNewWords = task.type == "new_words" && (task.isCompleted || (task.targetCount > 0 && task.completedCount >= task.targetCount))
                    TaskCheckRow(
                        task = task,
                        isCompletedNewWords = isCompletedNewWords,
                        onClick = {
                            if (isCompletedNewWords) {
                                onOpenNewWordsCompletedOptions(maxOf(task.completedCount, newWordsLearnedTodayCount))
                            } else {
                                val targetRoute = when (task.type) {
                                    "flashcards" -> "flashcards"
                                    "new_words" -> "new_words"
                                    "reading" -> "reading"
                                    "grammar" -> "grammar"
                                    "quiz" -> "quiz"
                                    else -> "plan"
                                }
                                onNavigateToTask(targetRoute, task.linkedId.ifBlank { task.id })
                            }
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
 * Formats a concise 1-line gold metadata string for Line 3 of a task card without repeating duration or concatenating paragraphs.
 */
private fun formatCompactTaskMeta(task: DailyTaskItem): String {
    val rawSub = task.subtitleEn.trim()
    val rawReason = task.reason.trim()

    fun stripDurationSuffix(text: String): String {
        return text
            .replace(Regex("\\s*\\(~?\\d+\\s*min\\)"), "")
            .replace(Regex("\\s*•\\s*~?\\d+\\s*min"), "")
            .trim()
    }

    return when (task.type) {
        "flashcards" -> {
            when {
                rawSub.contains("Tap to review ", ignoreCase = true) -> {
                    val deckName = rawSub.substringAfter("Tap to review ").trim()
                    "$deckName • Spaced Retention"
                }
                rawSub.isNotBlank() -> stripDurationSuffix(rawSub)
                rawReason.isNotBlank() -> stripDurationSuffix(rawReason)
                else -> "Oxford 5000 • Spaced Retention"
            }
        }
        "new_words" -> {
            val preTeachMatch = Regex("Pre-teaching \\d+ words for '([^']+)'").find(rawReason)
            when {
                preTeachMatch != null -> "Pre-teaching for '${preTeachMatch.groupValues[1]}'"
                rawSub.isNotBlank() -> stripDurationSuffix(rawSub)
                rawReason.isNotBlank() -> stripDurationSuffix(rawReason)
                else -> "Oxford 5000 • Personalized Pace"
            }
        }
        "grammar" -> {
            if (rawSub.startsWith("Topic:", ignoreCase = true)) {
                val cleaned = stripDurationSuffix(rawSub.removePrefix("Topic:").trim())
                    .replace(Regex("\\s*\\(\\d+\\s*exercises.*\\)"), "")
                    .trim()
                if (cleaned.endsWith("Lesson", ignoreCase = true)) cleaned else "$cleaned Lesson"
            } else if (rawSub.isNotBlank()) {
                stripDurationSuffix(rawSub)
            } else {
                stripDurationSuffix(rawReason)
            }
        }
        "reading" -> {
            val readabilityMatch = Regex("(\\d+% readable • \\d+ focus words)").find(rawSub.ifBlank { rawReason })
            when {
                readabilityMatch != null -> readabilityMatch.groupValues[1]
                rawSub.isNotBlank() -> stripDurationSuffix(rawSub)
                rawReason.isNotBlank() -> stripDurationSuffix(rawReason)
                else -> "Graded Reading Comprehension"
            }
        }
        "quiz" -> {
            if (rawSub.isNotBlank()) stripDurationSuffix(rawSub)
            else "5 Vocab • 3 Grammar • 2 Reading"
        }
        else -> {
            if (rawSub.isNotBlank()) stripDurationSuffix(rawSub)
            else stripDurationSuffix(rawReason)
        }
    }
}

/**
 * Task Card inside Today's Study Plan:
 * - 3 compact lines (Line 1: Full English title + inline duration & count pills; Line 2: RTL right-aligned Arabic subtitle; Line 3: Concise gold metadata).
 * - High-contrast gold action button (`#F5B82E` background with `#0B0D12` bold text).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskCheckRow(
    task: DailyTaskItem,
    isCompletedNewWords: Boolean = false,
    onClick: () -> Unit,
    onToggleComplete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isZeroDueFlashcards = (task.type == "flashcards" && task.targetCount == 0 && task.isCompleted)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = !isZeroDueFlashcards, onClick = onClick)
            .testTag("home_task_item_${task.type}"),
        color = ProtoBgCard,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (task.isCompleted || isCompletedNewWords) EmeraldAccent.copy(alpha = 0.55f) else ProtoBorderSubtle
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 11.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Left: Checkbox + 3-Line Task Body
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Task check square (20dp with 6dp radius matching prototype)
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (task.isCompleted) EmeraldAccent else Color.Transparent)
                        .border(
                            width = 1.5.dp,
                            color = if (task.isCompleted) EmeraldAccent else BorderMedium,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { onToggleComplete() }
                        .testTag("home_task_checkbox_${task.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    if (task.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = ProtoOnGoldDark,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                // 3-Line Task Body
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Line 1: Full English Title (no truncation) + inline duration & progress pills
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = task.titleEn,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.5.sp,
                                lineHeight = 18.sp,
                                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            color = if (task.isCompleted) TextMuted else TextPrimary,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )

                        if (task.estimatedMinutes > 0) {
                            Surface(
                                color = SurfaceElevatedDark,
                                shape = RoundedCornerShape(5.dp),
                                modifier = Modifier.align(Alignment.CenterVertically)
                            ) {
                                Text(
                                    text = "${task.estimatedMinutes}m",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.5.sp
                                    ),
                                    color = TextSecondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp)
                                )
                            }
                        }

                        if (task.targetCount > 1) {
                            Surface(
                                color = if (task.isCompleted) EmeraldAccent.copy(alpha = 0.15f) else ProtoGoldDim,
                                shape = RoundedCornerShape(5.dp),
                                modifier = Modifier.align(Alignment.CenterVertically)
                            ) {
                                Text(
                                    text = "${task.completedCount}/${task.targetCount}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.5.sp
                                    ),
                                    color = if (task.isCompleted) EmeraldAccent else ProtoGold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp)
                                )
                            }
                        }
                    }

                    // Line 2: Arabic Subtitle with explicit RTL direction and right alignment
                    if (task.titleAr.isNotBlank()) {
                        BidiText(
                            text = task.titleAr,
                            style = ArabicTypography.labelSmall.copy(fontSize = 12.sp),
                            color = if (task.isCompleted) TextMuted else TextSecondary,
                            fillMaxWidth = true,
                            textAlign = TextAlign.Right
                        )
                    }

                    // Line 3: Single concise gold metadata line (#F5B82E, 11.5sp)
                    val metaText = remember(task) { formatCompactTaskMeta(task) }
                    if (metaText.isNotBlank()) {
                        Text(
                            text = metaText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = ProtoGold
                        )
                    }
                }
            }

            // High-Contrast Gold Action Button
            val actionLabel = when {
                isZeroDueFlashcards -> "مكتملة ✓"
                isCompletedNewWords -> "مكتملة ✓"
                task.type == "quiz" -> if (task.isCompleted) "Retake" else "Quiz"
                task.type == "flashcards" -> if (task.isCompleted) "Review" else "Start"
                task.type == "new_words" -> if (task.isCompleted) "Words" else "Learn"
                task.type == "grammar" -> if (task.isCompleted) "Lesson" else "Study"
                task.type == "reading" -> if (task.isCompleted) "Story" else "Read"
                task.type == "sentences" -> "Practice"
                task.type == "correction_reviews" -> "Review"
                else -> "Open"
            }

            val buttonTextColor = when {
                isZeroDueFlashcards -> TextMuted
                isCompletedNewWords -> ProtoGold
                task.isCompleted -> ProtoGold
                else -> ProtoOnGoldDark
            }

            Button(
                onClick = onClick,
                enabled = !isZeroDueFlashcards,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (task.isCompleted || isCompletedNewWords) SurfaceElevatedDark else ProtoGold,
                    contentColor = buttonTextColor,
                    disabledContainerColor = SurfaceElevatedDark.copy(alpha = 0.5f),
                    disabledContentColor = TextMuted
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 13.dp, vertical = 8.dp),
                modifier = Modifier
                    .heightIn(min = 34.dp)
                    .testTag("btn_home_task_action_${task.type}")
            ) {
                Text(
                    text = if (isZeroDueFlashcards || isCompletedNewWords) actionLabel else "$actionLabel →",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        color = buttonTextColor
                    ),
                    color = buttonTextColor
                )
            }
        }
    }
}

/**
 * Section 5: Spaced Repetition Banner (`ReviewDueSection`)
 * High-contrast #0B0D12 bold text on #F5B82E full-width gold button.
 */
@Composable
private fun ReviewDueSection(
    reviewDueNowCount: Int,
    waitingLaterTodayCount: Int = 0,
    nextWaitingDueTimestamp: Long? = null,
    continueDeckId: String,
    onOpenFlashcardsForDeck: (String) -> Unit,
    onStartEarlyReview: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val hasDueNow = reviewDueNowCount > 0
    val hasWaitingLater = waitingLaterTodayCount > 0

    val waitingMinutes = remember(nextWaitingDueTimestamp) {
        if (nextWaitingDueTimestamp != null) {
            val diffMs = nextWaitingDueTimestamp - System.currentTimeMillis()
            if (diffMs > 0) ((diffMs + 59_999) / 60_000).toInt().coerceAtLeast(1) else 1
        } else {
            null
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_review_due_card"),
        color = ProtoBgCard,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (hasDueNow) ProtoGoldBorder else ProtoBorderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val isWaitingQueueState = !hasDueNow && hasWaitingLater
                val displayedCount = if (isWaitingQueueState) waitingLaterTodayCount else reviewDueNowCount
                val displayedLabel = if (isWaitingQueueState) "in queue" else "due now"

                // Due Now / In Queue Count Block
                Column(
                    modifier = Modifier.widthIn(min = 56.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = displayedCount.toString(),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = when {
                            hasDueNow -> ProtoGold
                            hasWaitingLater -> ProtoGold.copy(alpha = 0.88f)
                            else -> TextMuted
                        },
                        modifier = Modifier.testTag("home_review_due_count")
                    )
                    Text(
                        text = displayedLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp
                        ),
                        color = if (isWaitingQueueState) ProtoGold.copy(alpha = 0.75f) else TextSecondary
                    )
                }

                VerticalDivider(
                    modifier = Modifier.height(38.dp),
                    color = ProtoBorderSubtle,
                    thickness = 1.dp
                )

                // Bilingual Info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Keep your memory strong",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.5.sp
                        ),
                        color = TextPrimary
                    )

                    val arabicStatusText = when {
                        hasDueNow && hasWaitingLater -> {
                            val waitSuffix = if (waitingMinutes != null) " (~$waitingMinutes\u00A0د)" else ""
                            "$reviewDueNowCount جاهزة الآن • $waitingLaterTodayCount في الانتظار ⏳$waitSuffix"
                        }
                        hasDueNow -> "جاهزة للمراجعة الفورية الآن"
                        hasWaitingLater -> {
                            val waitSuffix = if (waitingMinutes != null) " (~$waitingMinutes\u00A0د)" else ""
                            "كل المراجعات الفورية منجزة ✓ • $waitingLaterTodayCount في الانتظار ⏳$waitSuffix"
                        }
                        else -> "كل المراجعات مكتملة لليوم ✓"
                    }

                    BidiText(
                        text = arabicStatusText,
                        style = ArabicTypography.labelSmall.copy(
                            fontSize = 12.sp
                        ),
                        color = if (hasDueNow || hasWaitingLater) TextSecondary else EmeraldAccent,
                        fillMaxWidth = true,
                        textAlign = TextAlign.Right
                    )
                }
            }

            // Full-Width High-Contrast Action Button
            when {
                hasDueNow -> {
                    Button(
                        onClick = {
                            onOpenFlashcardsForDeck(DataImportRepository.DUE_TODAY_DECK_ID)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ProtoGold,
                            contentColor = ProtoOnGoldDark
                        ),
                        shape = RoundedCornerShape(11.dp),
                        contentPadding = PaddingValues(vertical = 11.dp, horizontal = 16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 42.dp)
                            .testTag("home_review_due_button")
                    ) {
                        Text(
                            text = "Start review ($reviewDueNowCount) →",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = ProtoOnGoldDark
                            ),
                            color = ProtoOnGoldDark
                        )
                    }
                }
                hasWaitingLater -> {
                    Button(
                        onClick = onStartEarlyReview,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceElevatedDark,
                            contentColor = ProtoGold
                        ),
                        border = BorderStroke(1.dp, ProtoGoldBorder),
                        shape = RoundedCornerShape(11.dp),
                        contentPadding = PaddingValues(vertical = 11.dp, horizontal = 16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 42.dp)
                            .testTag("home_review_due_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = ProtoGold,
                            modifier = Modifier.size(16.dp).padding(end = 6.dp)
                        )
                        Text(
                            text = "مراجعة مبكرة بدون انتظار ⚡",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = ProtoGold
                            ),
                            color = ProtoGold
                        )
                    }
                }
                else -> {
                    Button(
                        onClick = {
                            onOpenFlashcardsForDeck(continueDeckId)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceElevatedDark,
                            contentColor = TextSecondary
                        ),
                        shape = RoundedCornerShape(11.dp),
                        contentPadding = PaddingValues(vertical = 11.dp, horizontal = 16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 42.dp)
                            .testTag("home_review_due_button")
                    ) {
                        Text(
                            text = "Practice more cards →",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.5.sp,
                                color = TextSecondary
                            ),
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Section 6: Continue Learning (Active Book Only + "All X books →" link)
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
        "oxford5000" -> Pair(EmeraldAccent, Color(0x1F10B981))
        "everyday" -> Pair(SkyAccent, SkyAccentBg)
        "idioms" -> Pair(VioletAccent, VioletAccentBg)
        else -> Pair(EmeraldAccent, Color(0x1F10B981))
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Section Header: "Continue learning" on left, "All 13 books →" on right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Continue learning",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = TextPrimary
            )

            Text(
                text = "All $totalDecksCount books →",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp
                ),
                color = ProtoGold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenDeck("") }
                    .padding(vertical = 4.dp, horizontal = 4.dp)
            )
        }

        // Active Deck Compact Card
        if (activeDeck != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(15.dp))
                    .clickable(role = Role.Button) { onOpenFlashcardsForDeck(continueDeckId) }
                    .testTag("home_continue_learning_card"),
                color = ProtoBgCard,
                shape = RoundedCornerShape(15.dp),
                border = BorderStroke(1.dp, ProtoBorderSubtle)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Book Icon Box (44x44, 12dp radius)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentBg)
                            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(21.dp)
                        )
                    }

                    // Book Info
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
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
                                    fontSize = 14.5.sp
                                ),
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            Text(
                                text = "Card $continueCardPosition / $formattedTotalCards →",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                ),
                                color = ProtoGold,
                                modifier = Modifier.testTag("home_continue_position_text")
                            )
                        }

                        if (activeDeck.arabicSubtitle.isNotBlank()) {
                            BidiText(
                                text = activeDeck.arabicSubtitle,
                                style = ArabicTypography.labelSmall.copy(
                                    fontSize = 12.5.sp
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewWordsCompletedBottomSheet(
    completedCount: Int,
    onDismiss: () -> Unit,
    onReviewTodayLearnedWords: () -> Unit,
    onLearnExtraNewWords: (Int) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ProtoBgCard,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(EmeraldAccent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = EmeraldAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "هدف الكلمات اليومي مكتمل! 🎉",
                        style = ArabicTypography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = TextPrimary
                    )
                    Text(
                        text = if (completedCount > 0) "أكملت دراسة $completedCount كلمة بنجاح اليوم" else "أتممت دراسة حصة المفردات لليوم",
                        style = ArabicTypography.labelSmall.copy(fontSize = 13.sp),
                        color = TextSecondary
                    )
                }
            }

            HorizontalDivider(color = ProtoBorderSubtle)

            Text(
                text = "ماذا ترغب أن تفعل الآن؟",
                style = ArabicTypography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                ),
                color = ProtoGold
            )

            // Option 1: Review today's learned words
            Button(
                onClick = {
                    onDismiss()
                    onReviewTodayLearnedWords()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ProtoGold,
                    contentColor = ProtoOnGoldDark
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "مراجعة وتثبيت كلمات اليوم (${if (completedCount > 0) completedCount else "اليوم"})",
                        style = ArabicTypography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = ProtoOnGoldDark
                    )
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = ProtoOnGoldDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Option 2: Learn 5 extra words
            OutlinedButton(
                onClick = {
                    onDismiss()
                    onLearnExtraNewWords(5)
                },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextPrimary
                ),
                border = BorderStroke(1.dp, ProtoGoldBorder),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "تعلم 5 كلمات إضافية 🚀",
                        style = ArabicTypography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = TextPrimary
                    )
                    Text(
                        text = "+5",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        color = ProtoGold
                    )
                }
            }

            // Option 3: Learn 10 extra words
            OutlinedButton(
                onClick = {
                    onDismiss()
                    onLearnExtraNewWords(10)
                },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextPrimary
                ),
                border = BorderStroke(1.dp, ProtoBorderSubtle),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "تعلم 10 كلمات إضافية 📚",
                        style = ArabicTypography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = TextPrimary
                    )
                    Text(
                        text = "+10",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        color = ProtoGold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
