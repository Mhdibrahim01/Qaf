package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.BidiText
import com.example.ui.components.BiDiParsedText
import com.example.ui.components.BiDiGoldPillBg
import com.example.ui.components.BiDiGoldPillBorder
import com.example.ui.components.BiDiRedPillBg
import com.example.ui.components.BiDiRedPillBorder
import com.example.ui.components.BiDiRedPillText
import com.example.ui.components.BiDiExampleBoxBg
import com.example.ui.theme.InterFontFamily
import androidx.compose.material.icons.filled.Warning
import com.example.ui.components.cleanGrammarArabicTitle
import com.example.data.local.AiHistoryEntity
import com.example.data.local.GrammarExampleItem
import com.example.data.local.GrammarExerciseItem
import com.example.data.local.GrammarLessonEntity
import com.example.data.local.GrammarProgressEntity
import com.example.data.local.GrammarSentenceCheckResult
import com.example.data.local.ReadingTextEntity
import com.example.ui.theme.ArabicTypography
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderDark
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldAccentBg
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldAccentBorder
import com.example.ui.theme.GoldAccentDim
import com.example.ui.theme.GoldHover
import com.example.ui.theme.OnGoldDark
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
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrammarScreen(
    lessons: List<GrammarLessonEntity>,
    progressList: List<GrammarProgressEntity>,
    chatHistory: List<AiHistoryEntity>,
    sentenceCheckHistory: List<AiHistoryEntity>,
    selectedLesson: GrammarLessonEntity?,
    readingTexts: List<ReadingTextEntity> = emptyList(),
    onSelectLesson: (GrammarLessonEntity?) -> Unit,
    onOpenReadingText: (ReadingTextEntity) -> Unit = {},
    onNavigateToNotebook: () -> Unit = {},
    onSpeakTts: (String) -> Unit,
    onSaveProgress: (lessonId: String, completed: Int, total: Int, score: Int, isDone: Boolean, elapsedMs: Long?) -> Unit,
    onAskAi: suspend (lesson: GrammarLessonEntity, question: String) -> Result<String>,
    onExplainMistake: suspend (lessonId: String, title: String, prompt: String, wrong: String, correct: String) -> Result<String>,
    onCheckSentence: suspend (lesson: GrammarLessonEntity, sentence: String) -> Result<GrammarSentenceCheckResult>,
    onMorePractice: suspend (lesson: GrammarLessonEntity) -> Result<List<GrammarExerciseItem>>,
    onCreateLessonWithAi: suspend (level: String, topic: String) -> Result<Unit>,
    onSyncAssets: (() -> Unit)? = null,
    onImportJsonUris: ((List<Uri>) -> Unit)? = null,
    onImportJsonText: (suspend (String) -> Result<Int>)? = null,
    onSaveCorrectionEntry: ((lessonId: String, title: String, prompt: String, wrong: String, correct: String, expAr: String) -> Unit)? = null,
    userLevel: String = "B2",
    onBackFromDetail: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showAddLessonDialog by remember { mutableStateOf(false) }
    var selectedAddTab by remember { mutableIntStateOf(0) }
    var userFeedbackBanner by remember { mutableStateOf<String?>(null) }

    if (selectedLesson != null) {
        GrammarLessonDetailScreen(
            lesson = selectedLesson,
            progress = progressList.firstOrNull { it.lessonId == selectedLesson.id },
            chatHistory = chatHistory,
            sentenceCheckHistory = sentenceCheckHistory,
            readingTexts = readingTexts,
            onBack = {
                if (onBackFromDetail != null) {
                    onBackFromDetail()
                } else {
                    onSelectLesson(null)
                }
            },
            onSpeakTts = onSpeakTts,
            onOpenReadingText = onOpenReadingText,
            onSaveProgress = onSaveProgress,
            onAskAi = onAskAi,
            onExplainMistake = onExplainMistake,
            onCheckSentence = onCheckSentence,
            onMorePractice = onMorePractice,
            onSaveCorrectionEntry = onSaveCorrectionEntry,
            modifier = modifier
        )
    } else {
        GrammarLessonListScreen(
            lessons = lessons,
            progressList = progressList,
            feedbackBanner = userFeedbackBanner,
            onDismissBanner = { userFeedbackBanner = null },
            onSelectLesson = onSelectLesson,
            onNavigateToNotebook = onNavigateToNotebook,
            onOpenAddLessons = { tab ->
                selectedAddTab = tab
                showAddLessonDialog = true
            },
            onSyncAssets = {
                onSyncAssets?.invoke()
                userFeedbackBanner = "Re-synced grammar lessons from assets"
            },
            userLevel = userLevel,
            modifier = modifier
        )
    }

    if (showAddLessonDialog) {
        AddGrammarLessonsDialog(
            initialTab = selectedAddTab,
            onDismiss = { showAddLessonDialog = false },
            onCreateWithAi = onCreateLessonWithAi,
            onImportJsonUris = { uris ->
                onImportJsonUris?.invoke(uris)
                userFeedbackBanner = "Importing JSON file(s)..."
            },
            onImportJsonText = onImportJsonText,
            onSyncAssets = {
                onSyncAssets?.invoke()
                userFeedbackBanner = "Re-synced grammar lessons from assets"
            },
            onSuccessMessage = { msg ->
                userFeedbackBanner = msg
            }
        )
    }
}

@Composable
fun GrammarLessonListScreen(
    lessons: List<GrammarLessonEntity>,
    progressList: List<GrammarProgressEntity>,
    feedbackBanner: String?,
    onDismissBanner: () -> Unit,
    onSelectLesson: (GrammarLessonEntity) -> Unit,
    onOpenAddLessons: (initialTab: Int) -> Unit,
    onSyncAssets: () -> Unit,
    onNavigateToNotebook: () -> Unit = {},
    userLevel: String = "B2",
    modifier: Modifier = Modifier
) {
    var selectedLevelFilter by remember { mutableStateOf("ALL") }
    var selectedTopicFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }

    val recommendedLessons = remember(lessons, userLevel) {
        lessons.filter { it.level.equals(userLevel, ignoreCase = true) }
    }

    val progressMap = remember(progressList) { progressList.associateBy { it.lessonId } }
    val completedCount = remember(progressList) { progressList.count { it.isCompleted } }

    val availableLevels = remember(lessons) {
        val baseOrder = listOf("A1", "A2", "B1", "B2", "C1", "C2")
        val present = lessons.map { it.level.uppercase().trim() }.filter { it.isNotBlank() }.toSet()
        val ordered = baseOrder.filter { it in present || it in listOf("A1", "A2", "B1", "B2", "C1") }
        val extras = (present - ordered.toSet()).sorted()
        listOf("ALL") + ordered + extras
    }

    val levelCounts = remember(lessons) {
        lessons.groupingBy { it.level.uppercase().trim() }.eachCount()
    }

    val availableTopics = remember(lessons, selectedLevelFilter) {
        val levelFiltered = if (selectedLevelFilter == "ALL") {
            lessons
        } else {
            lessons.filter { it.level.equals(selectedLevelFilter, ignoreCase = true) }
        }
        listOf("ALL") + levelFiltered.map { it.topic.trim() }.filter { it.isNotBlank() }.distinct()
    }

    val filteredLessons = remember(lessons, selectedLevelFilter, selectedTopicFilter, searchQuery) {
        val q = searchQuery.trim().lowercase()
        lessons.filter { lesson ->
            val matchesLevel = selectedLevelFilter == "ALL" ||
                lesson.level.equals(selectedLevelFilter, ignoreCase = true)
            val matchesTopic = selectedTopicFilter == "ALL" ||
                lesson.topic.equals(selectedTopicFilter, ignoreCase = true)
            val matchesSearch = q.isEmpty() ||
                lesson.titleEn.lowercase().contains(q) ||
                lesson.titleAr.contains(q) ||
                lesson.topic.lowercase().contains(q) ||
                lesson.level.lowercase().contains(q)
            matchesLevel && matchesTopic && matchesSearch
        }
    }

    val groupedByLevel = remember(filteredLessons) {
        filteredLessons.groupBy { it.level.uppercase() }
    }

    val doneCountByLevel = remember(groupedByLevel, progressMap) {
        groupedByLevel.mapValues { (_, list) ->
            list.count { progressMap[it.id]?.isCompleted == true }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .testTag("grammar_lesson_list_screen")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Compact Header & Action Pills Bar (No overlapping bottom FAB)
            item(key = "grammar_header_controls", contentType = "header") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    var showOverflowMenu by remember { mutableStateOf(false) }
                    val levelListState = rememberLazyListState()
                    val topicListState = rememberLazyListState()

                    LaunchedEffect(selectedLevelFilter) {
                        val idx = availableLevels.indexOf(selectedLevelFilter)
                        if (idx >= 0) levelListState.animateScrollToItem(idx)
                    }

                    LaunchedEffect(selectedTopicFilter) {
                        val idx = availableTopics.indexOf(selectedTopicFilter)
                        if (idx >= 0) topicListState.animateScrollToItem(idx)
                    }

                    // 1. Progress Banner Card
                    val totalLessons = lessons.size
                    val progressFraction = if (totalLessons > 0) (completedCount.toFloat() / totalLessons.toFloat()).coerceIn(0f, 1f) else 0f
                    val progressPercent = (progressFraction * 100).toInt()

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("grammar_progress_banner"),
                        shape = RoundedCornerShape(14.dp),
                        color = SurfaceDark,
                        border = BorderStroke(1.dp, BorderDark)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Top Row: Completed • Total on Left, + AI Lesson + Overflow on Right
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp)
                                ) {
                                    Text(
                                        text = "$completedCount Completed",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                                        color = GoldAccent,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Text(
                                        text = " • $totalLessons Lessons",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = TextSecondary,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = GoldAccentDim,
                                        border = BorderStroke(1.dp, GoldAccentBorder),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { onOpenAddLessons(2) }
                                            .testTag("create_ai_lesson_button")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = GoldAccent,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "+ AI Lesson",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = GoldAccent,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }

                                    Box {
                                        IconButton(
                                            onClick = { showOverflowMenu = true },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = "More Options",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = showOverflowMenu,
                                            onDismissRequest = { showOverflowMenu = false },
                                            modifier = Modifier.background(SurfaceDark)
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Correction Notebook", color = TextPrimary) },
                                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = GoldAccent) },
                                                onClick = {
                                                    showOverflowMenu = false
                                                    onNavigateToNotebook()
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Import JSON", color = TextPrimary) },
                                                leadingIcon = { Icon(Icons.Default.UploadFile, contentDescription = null, tint = GoldAccent) },
                                                onClick = {
                                                    showOverflowMenu = false
                                                    onOpenAddLessons(0)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Sync Assets", color = TextPrimary) },
                                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = GoldAccent) },
                                                onClick = {
                                                    showOverflowMenu = false
                                                    onSyncAssets()
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Middle: Progress Bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(SurfaceElevatedDark)
                            ) {
                                if (progressFraction > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(fraction = progressFraction)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    colors = listOf(GoldAccent, GoldHover)
                                                )
                                            )
                                    )
                                }
                            }

                            // Bottom Row: Subtitle & Percentage
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Bilingual Grammar Practice",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "$progressPercent%",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = "Search grammar rule, topic, or Arabic title...",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search grammar lessons",
                                tint = GoldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear search",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldAccent,
                            unfocusedBorderColor = BorderDark,
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("grammar_search_input")
                    )

                    // Pinned Recommended for You Shelf
                    if (recommendedLessons.isNotEmpty()) {
                        GrammarRecommendedShelf(
                            userLevel = userLevel,
                            recommendedLessons = recommendedLessons,
                            progressMap = progressMap,
                            onSelectLesson = onSelectLesson
                        )
                    }

                    // CEFR Level Filter Pills with Auto-Scroll & High Contrast Active State
                    Box(modifier = Modifier.fillMaxWidth()) {
                        LazyRow(
                            state = levelListState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("grammar_level_filter_row"),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(end = 24.dp)
                        ) {
                            items(
                                items = availableLevels,
                                key = { it },
                                contentType = { "level_chip" }
                            ) { level ->
                                val isSelected = selectedLevelFilter == level
                                val count = if (level == "ALL") lessons.size else (levelCounts[level] ?: 0)
                                val isUserLevel = level.equals(userLevel, ignoreCase = true) && level != "ALL"
                                val labelText = when {
                                    level == "ALL" -> "All ($count)"
                                    isUserLevel -> "$level ★ ($count)"
                                    else -> "$level ($count)"
                                }
                                Surface(
                                    shape = RoundedCornerShape(99.dp),
                                    color = if (isSelected) GoldAccent else SurfaceDark,
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isSelected) GoldAccent else BorderDark
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(99.dp))
                                        .clickable {
                                            selectedLevelFilter = level
                                            selectedTopicFilter = "ALL"
                                        }
                                        .testTag("grammar_level_chip_$level")
                                ) {
                                    Text(
                                        text = labelText,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) OnGoldDark else TextSecondary,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .width(24.dp)
                                .height(38.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Color.Transparent, BackgroundDark)
                                    )
                                )
                        )
                    }

                    // Topic Filter Chips with Distinct Rounded Rectangle Styling
                    if (availableTopics.size > 2) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            LazyRow(
                                state = topicListState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("grammar_topic_filter_row"),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(end = 24.dp)
                            ) {
                                items(
                                    items = availableTopics,
                                    key = { it },
                                    contentType = { "topic_chip" }
                                ) { topic ->
                                    val isSelected = selectedTopicFilter == topic
                                    val displayTopic = if (topic == "ALL") "All Topics" else topic
                                    Surface(
                                        shape = RoundedCornerShape(9.dp),
                                        color = if (isSelected) GoldAccentDim else SurfaceDark,
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = if (isSelected) GoldAccentBorder else BorderDark
                                        ),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(9.dp))
                                            .clickable { selectedTopicFilter = topic }
                                            .testTag("grammar_topic_chip_$topic")
                                    ) {
                                        Text(
                                            text = displayTopic,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (isSelected) GoldAccent else TextSecondary,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .width(24.dp)
                                    .height(34.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(Color.Transparent, BackgroundDark)
                                        )
                                    )
                            )
                        }
                    }

                    // Feedback Banner
                    AnimatedVisibility(
                        visible = feedbackBanner != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        if (feedbackBanner != null) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = EmeraldAccent.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(18.dp))
                                        Text(
                                            text = feedbackBanner,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextPrimary
                                        )
                                    }
                                    IconButton(
                                        onClick = onDismissBanner,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = TextMuted, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (lessons.isEmpty()) {
                item(key = "grammar_loading", contentType = "loading") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(color = GoldAccent)
                            Text(
                                text = "Loading grammar lessons from assets...",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            } else if (filteredLessons.isEmpty()) {
                item(key = "grammar_empty", contentType = "empty") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        border = BorderStroke(1.dp, BorderDark)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "No matching grammar lessons",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = "Try clearing your search or switching the CEFR level filter.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            OutlinedButton(
                                onClick = {
                                    selectedLevelFilter = "ALL"
                                    selectedTopicFilter = "ALL"
                                    searchQuery = ""
                                },
                                border = BorderStroke(1.dp, GoldAccentBorder),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Reset Filters", color = GoldAccent, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            groupedByLevel.forEach { (level, levelLessons) ->
                item(key = "level_header_$level", contentType = "level_header") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = GoldAccentDim,
                                border = BorderStroke(1.dp, GoldAccentBorder)
                            ) {
                                Text(
                                    text = "CEFR $level",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = GoldAccent,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = "${levelLessons.size} lessons",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextMuted
                            )
                        }
                        val doneInLevel = doneCountByLevel[level] ?: 0
                        if (doneInLevel > 0) {
                            Text(
                                text = "$doneInLevel/${levelLessons.size} done",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldAccent,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                items(
                    items = levelLessons,
                    key = { it.id },
                    contentType = { "grammar_lesson_card" }
                ) { lesson ->
                    val progress = progressMap[lesson.id]
                    LessonCard(
                        lesson = lesson,
                        progress = progress,
                        onClick = { onSelectLesson(lesson) }
                    )
                }
            }
        }
    }
}

@Composable
fun LessonCard(
    lesson: GrammarLessonEntity,
    progress: GrammarProgressEntity?,
    onClick: () -> Unit
) {
    val isCompleted = progress?.isCompleted == true

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("lesson_card_${lesson.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, if (isCompleted) EmeraldAccent.copy(alpha = 0.5f) else BorderDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Row 1: Level badge + Category/Topic name (+ optional AI badge)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = GoldAccentDim,
                        border = BorderStroke(1.dp, GoldAccentBorder)
                    ) {
                        Text(
                            text = lesson.level.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldAccent,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (lesson.topic.isNotBlank()) {
                        Text(
                            text = lesson.topic,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (lesson.source == "ai") {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = EmeraldAccent.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = "AI",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldAccent,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Row 2: English Title
                Text(
                    text = lesson.titleEn,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.5.sp),
                    color = TextPrimary
                )

                // Row 3: Arabic Title directly underneath, start-aligned
                val cleanedTitleAr = remember(lesson.titleAr) { cleanGrammarArabicTitle(lesson.titleAr) }
                if (cleanedTitleAr.isNotBlank()) {
                    Text(
                        text = cleanedTitleAr,
                        style = ArabicTypography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isCompleted) {
                    Surface(
                        color = EmeraldAccent.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Completed",
                                tint = EmeraldAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            val total = if (progress?.totalExercises != null && progress.totalExercises > 0) {
                                progress.totalExercises
                            } else {
                                8
                            }
                            Text(
                                text = "${progress?.bestScore ?: 0}/$total",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldAccent,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // Right side: 32x32px rounded icon box with chevron right
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = if (isCompleted) EmeraldAccent.copy(alpha = 0.15f) else SurfaceElevatedDark,
                    border = BorderStroke(
                        1.dp,
                        if (isCompleted) EmeraldAccent.copy(alpha = 0.4f) else BorderDark
                    ),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Open Lesson",
                            tint = if (isCompleted) EmeraldAccent else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrammarLessonDetailScreen(
    lesson: GrammarLessonEntity,
    progress: GrammarProgressEntity?,
    chatHistory: List<AiHistoryEntity>,
    sentenceCheckHistory: List<AiHistoryEntity>,
    readingTexts: List<ReadingTextEntity> = emptyList(),
    onBack: () -> Unit,
    onSpeakTts: (String) -> Unit,
    onOpenReadingText: (ReadingTextEntity) -> Unit = {},
    onSaveProgress: (lessonId: String, completed: Int, total: Int, score: Int, isDone: Boolean, elapsedMs: Long?) -> Unit,
    onAskAi: suspend (lesson: GrammarLessonEntity, question: String) -> Result<String>,
    onExplainMistake: suspend (lessonId: String, title: String, prompt: String, wrong: String, correct: String) -> Result<String>,
    onCheckSentence: suspend (lesson: GrammarLessonEntity, sentence: String) -> Result<GrammarSentenceCheckResult>,
    onMorePractice: suspend (lesson: GrammarLessonEntity) -> Result<List<GrammarExerciseItem>>,
    onSaveCorrectionEntry: ((lessonId: String, title: String, prompt: String, wrong: String, correct: String, expAr: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val quizStartTimeMillis = remember(lesson.id) { System.currentTimeMillis() }

    val jsonHelper = remember {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
    }

    val examples = remember(lesson.examplesJson) {
        try {
            jsonHelper.decodeFromString<List<GrammarExampleItem>>(lesson.examplesJson)
        } catch (_: Exception) {
            emptyList()
        }
    }

    val exercises = remember(lesson.exercisesJson) {
        try {
            jsonHelper.decodeFromString<List<GrammarExerciseItem>>(lesson.exercisesJson)
        } catch (_: Exception) {
            emptyList()
        }
    }

    var selectedTab by remember(lesson.id) { mutableIntStateOf(0) } // 0 = Learn, 1 = Practice, 2 = AI Write
    var currentQuestionIndex by remember(lesson.id) { mutableIntStateOf(0) }

    var showChatSheet by remember { mutableStateOf(false) }
    var isGeneratingMoreExercises by remember { mutableStateOf(false) }
    var morePracticeSuccessMessage by remember { mutableStateOf<String?>(null) }
    var userSentenceInput by remember { mutableStateOf("") }
    var isCheckingSentence by remember { mutableStateOf(false) }
    var sentenceCheckResult by remember { mutableStateOf<GrammarSentenceCheckResult?>(null) }
    var sentenceCheckError by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    // Exercise state & Persistence
    val isAlreadyCompleted = progress?.isCompleted == true
    var isReviewMode by remember(lesson.id, isAlreadyCompleted) { mutableStateOf(isAlreadyCompleted) }
    var activeMistakeFilterIndices by remember(lesson.id) { mutableStateOf<List<Int>?>(null) }
    val firstAttemptCorrect = remember(lesson.id) { mutableStateMapOf<Int, Boolean>() }
    val userAnswers = remember(lesson.id) { mutableStateMapOf<Int, String>() }
    val isSubmitted = remember(lesson.id) { mutableStateMapOf<Int, Boolean>() }
    val mistakeExplanations = remember(lesson.id) { mutableStateMapOf<Int, String>() }
    val isExplainingMistake = remember(lesson.id) { mutableStateMapOf<Int, Boolean>() }
    var exercisesFinished by remember(lesson.id, isAlreadyCompleted) { mutableStateOf(isAlreadyCompleted) }

    val activeIndices = activeMistakeFilterIndices ?: exercises.indices.toList()
    val safeQuestionIndex = if (activeIndices.isEmpty()) 0 else currentQuestionIndex.coerceIn(0, activeIndices.lastIndex)
    val activeRealIndex = if (activeIndices.isEmpty()) 0 else activeIndices[safeQuestionIndex]

    val linkedReadingTexts = remember(readingTexts, lesson.id) {
        readingTexts.filter { text ->
            try {
                jsonHelper.decodeFromString<List<String>>(text.grammarIdsJson).contains(lesson.id)
            } catch (_: Exception) {
                text.grammarIdsJson.contains(lesson.id)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .testTag("lesson_detail_screen")
    ) {
        // Unified Sub-Header: Segmented Tabs + AI Tutor Action Pill
        Surface(
            color = SurfaceDark,
            border = BorderStroke(1.dp, BorderDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Segmented 3-Tab Switcher
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = BackgroundDark,
                    border = BorderStroke(1.dp, BorderDark),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val tabs = listOf(
                            "Learn" to "grammar_tab_learn",
                            "Practice (${exercises.size})" to "grammar_tab_practice",
                            "AI Write" to "grammar_tab_write"
                        )
                        tabs.forEachIndexed { idx, (label, tag) ->
                            val isSelected = selectedTab == idx
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) GoldAccent else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedTab = idx }
                                    .testTag(tag)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) BackgroundDark else TextSecondary,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Compact AI Tutor Pill Button
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = GoldAccentDim,
                    border = BorderStroke(1.dp, GoldAccent),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showChatSheet = true }
                        .testTag("ask_ai_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QuestionAnswer,
                            contentDescription = "Ask AI about this lesson",
                            tint = GoldAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "AI Tutor",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent
                        )
                    }
                }
            }
        }

        // Content Area based on Selected Tab
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    // TAB 0: LEARN & EXAMPLES
                    item {
                        StructuredGrammarExplanation(
                            lesson = lesson
                        )
                    }

                    // Examples Card
                    if (examples.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                border = BorderStroke(1.dp, BorderDark)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Examples",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = GoldAccent,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = SurfaceElevatedDark
                                        ) {
                                            Text(
                                                text = "${examples.size} Sentences",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    examples.forEachIndexed { index, ex ->
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = SurfaceElevatedDark,
                                            border = BorderStroke(1.dp, BorderDark),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .padding(end = 12.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = ex.en,
                                                        style = MaterialTheme.typography.bodyLarge.copy(
                                                            textDirection = TextDirection.Ltr,
                                                            textAlign = TextAlign.Left
                                                        ),
                                                        color = TextPrimary,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                                        Text(
                                                            text = ex.ar,
                                                            style = ArabicTypography.bodyMedium.copy(
                                                                textDirection = TextDirection.Rtl,
                                                                textAlign = TextAlign.Right
                                                            ),
                                                            color = TextSecondary,
                                                            modifier = Modifier.fillMaxWidth()
                                                        )
                                                    }
                                                }
                                                Surface(
                                                    shape = CircleShape,
                                                    color = BiDiGoldPillBg,
                                                    border = BorderStroke(1.dp, BiDiGoldPillBorder),
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    IconButton(
                                                        onClick = { onSpeakTts(ex.en) },
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .testTag("tts_example_button_$index")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                            contentDescription = "Speak sentence",
                                                            tint = GoldAccent,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Read in Context Card
                    if (linkedReadingTexts.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("read_in_context_section"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.45f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                                        Text(
                                            text = "Read in Context",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = GoldAccent,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = "Explore graded reading passages featuring this grammar rule:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )

                                    linkedReadingTexts.forEach { readingText ->
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .clickable { onOpenReadingText(readingText) }
                                                .testTag("read_in_context_item_${readingText.id}"),
                                            shape = RoundedCornerShape(14.dp),
                                            color = SurfaceElevatedDark,
                                            border = BorderStroke(1.dp, BorderDark)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(14.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .padding(end = 12.dp)
                                                ) {
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = GoldAccent.copy(alpha = 0.16f)
                                                        ) {
                                                            Text(
                                                                text = readingText.level,
                                                                color = GoldAccent,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                        Text(
                                                            text = readingText.titleEn,
                                                            style = MaterialTheme.typography.titleSmall,
                                                            color = TextPrimary,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                    if (readingText.titleAr.isNotBlank()) {
                                                        Spacer(Modifier.height(4.dp))
                                                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                                            Text(
                                                                text = readingText.titleAr,
                                                                style = ArabicTypography.bodySmall.copy(
                                                                    textDirection = TextDirection.Rtl,
                                                                    textAlign = TextAlign.Right
                                                                ),
                                                                color = TextSecondary,
                                                                modifier = Modifier.fillMaxWidth()
                                                            )
                                                        }
                                                    }
                                                }
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                    contentDescription = "Open reading text",
                                                    tint = GoldAccent,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom CTA to launch Practice Quiz
                    if (exercises.isNotEmpty()) {
                        item {
                            val isAr = com.example.ui.theme.AppLanguageState.isArabic
                            val isCompleted = progress?.isCompleted == true
                            val bestScore = progress?.bestScore ?: 0
                            Button(
                                onClick = { selectedTab = 1 },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldAccent,
                                    contentColor = BackgroundDark
                                ),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(vertical = 14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("start_practice_quiz_cta")
                            ) {
                                Text(
                                    text = if (isCompleted) {
                                        if (isAr) "مراجعة تمارين الدرس ($bestScore/${exercises.size} • مكتمل ✓) ←"
                                        else "Review Practice Quiz ($bestScore/${exercises.size} • Completed ✓) →"
                                    } else {
                                        if (isAr) "بدء كويز التمارين (${exercises.size} أسئلة) ←"
                                        else "Start Practice Quiz (${exercises.size} Questions) →"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: STEP-BY-STEP INTERACTIVE PRACTICE QUIZ
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            border = BorderStroke(1.dp, BorderDark)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Quiz Header: Question X of N + More Practice AI Pill
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        val isAr = com.example.ui.theme.AppLanguageState.isArabic
                                        Text(
                                            text = if (activeMistakeFilterIndices != null) {
                                                if (isAr) "إعادة الأخطاء: السؤال ${safeQuestionIndex + 1} من ${activeIndices.size}"
                                                else "Retrying Mistakes: Question ${safeQuestionIndex + 1} of ${activeIndices.size}"
                                            } else if (exercises.isEmpty()) {
                                                "Practice Quiz"
                                            } else {
                                                if (isAr) "السؤال ${safeQuestionIndex + 1} من ${exercises.size}"
                                                else "Question ${safeQuestionIndex + 1} of ${exercises.size}"
                                            },
                                            style = MaterialTheme.typography.titleMedium,
                                            color = GoldAccent,
                                            fontWeight = FontWeight.Bold
                                        )
                                        val answeredCount = activeIndices.count { isSubmitted[it] == true }
                                        Text(
                                            text = "$answeredCount of ${activeIndices.size} completed",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                isGeneratingMoreExercises = true
                                                morePracticeSuccessMessage = null
                                                val res = onMorePractice(lesson)
                                                isGeneratingMoreExercises = false
                                                if (res.isSuccess) {
                                                    morePracticeSuccessMessage = "+5 new practice exercises added!"
                                                }
                                            }
                                        },
                                        enabled = !isGeneratingMoreExercises,
                                        border = BorderStroke(1.dp, GoldAccent),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("more_practice_button")
                                    ) {
                                        if (isGeneratingMoreExercises) {
                                            CircularProgressIndicator(modifier = Modifier.size(15.dp), color = GoldAccent, strokeWidth = 2.dp)
                                        } else {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text("More Practice", color = GoldAccent, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                if (morePracticeSuccessMessage != null) {
                                    Text(
                                        text = morePracticeSuccessMessage!!,
                                        color = EmeraldAccent,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                if (exercises.isNotEmpty()) {
                                    // Linear Progress Bar
                                    val progressFraction = ((safeQuestionIndex + 1).toFloat() / activeIndices.size.toFloat()).coerceIn(0f, 1f)
                                    LinearProgressIndicator(
                                        progress = { progressFraction },
                                        color = GoldAccent,
                                        trackColor = SurfaceElevatedDark,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                    )

                                    // Step Number Pills Row
                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        itemsIndexed(activeIndices) { stepIdx, realIdx ->
                                            val ex = exercises[realIdx]
                                            val isCurrent = stepIdx == safeQuestionIndex
                                            val sub = isSubmitted[realIdx] == true
                                            val ans = userAnswers[realIdx].orEmpty().trim()
                                            val correct = sub && ans.equals(ex.answer.trim(), ignoreCase = true)

                                            val pillBg = when {
                                                isCurrent -> GoldAccent
                                                sub && correct -> EmeraldAccent.copy(alpha = 0.2f)
                                                sub && !correct -> CoralAccent.copy(alpha = 0.2f)
                                                else -> SurfaceElevatedDark
                                            }
                                            val pillBorder = when {
                                                isCurrent -> GoldAccent
                                                sub && correct -> EmeraldAccent
                                                sub && !correct -> CoralAccent
                                                else -> BorderDark
                                            }
                                            val pillText = when {
                                                isCurrent -> BackgroundDark
                                                sub && correct -> EmeraldAccent
                                                sub && !correct -> CoralAccent
                                                else -> TextSecondary
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = pillBg,
                                                border = BorderStroke(1.dp, pillBorder),
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { currentQuestionIndex = stepIdx }
                                                    .testTag("quiz_step_pill_$realIdx")
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = "${realIdx + 1}",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = pillText
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    HorizontalDivider(color = BorderDark)

                                    // Active Question Card
                                    val activeExercise = exercises[activeRealIndex]
                                    val activeSubmitted = isSubmitted[activeRealIndex] == true
                                    val isCurrentCorrect = activeSubmitted && isGrammarAnswerCorrect(userAnswers[activeRealIndex].orEmpty(), activeExercise.answer)
                                    val isCurrentWrong = activeSubmitted && !isCurrentCorrect

                                    ExerciseItemView(
                                        index = activeRealIndex,
                                        exercise = activeExercise,
                                        currentAnswer = userAnswers[activeRealIndex].orEmpty(),
                                        submitted = activeSubmitted,
                                        onAnswerChange = { userAnswers[activeRealIndex] = it },
                                        onSubmit = {
                                            isSubmitted[activeRealIndex] = true
                                            val isCorrect = isGrammarAnswerCorrect(userAnswers[activeRealIndex].orEmpty(), activeExercise.answer)
                                            if (activeRealIndex !in firstAttemptCorrect) {
                                                firstAttemptCorrect[activeRealIndex] = isCorrect
                                                if (!isCorrect) {
                                                    onSaveCorrectionEntry?.invoke(
                                                        lesson.id,
                                                        lesson.titleEn,
                                                        activeExercise.prompt,
                                                        userAnswers[activeRealIndex].orEmpty(),
                                                        activeExercise.answer,
                                                        activeExercise.explanationAr
                                                    )
                                                }
                                            }
                                            if (activeIndices.all { isSubmitted[it] == true }) {
                                                exercisesFinished = true
                                                val score = exercises.indices.count { i ->
                                                    isGrammarAnswerCorrect(userAnswers[i].orEmpty(), exercises[i].answer)
                                                }
                                                val elapsedMs = System.currentTimeMillis() - quizStartTimeMillis
                                                onSaveProgress(lesson.id, exercises.size, exercises.size, score, true, elapsedMs)
                                            }
                                        },
                                        mistakeExplanation = mistakeExplanations[activeRealIndex],
                                        isExplainingMistake = isExplainingMistake[activeRealIndex] == true,
                                        onExplainMistakeClick = {
                                            coroutineScope.launch {
                                                isExplainingMistake[activeRealIndex] = true
                                                val res = onExplainMistake(
                                                    lesson.id,
                                                    lesson.titleEn,
                                                    activeExercise.prompt,
                                                    userAnswers[activeRealIndex].orEmpty(),
                                                    activeExercise.answer
                                                )
                                                isExplainingMistake[activeRealIndex] = false
                                                if (res.isSuccess) {
                                                    mistakeExplanations[activeRealIndex] = res.getOrThrow()
                                                }
                                            }
                                        }
                                    )

                                    // Step Navigation Footer (Previous / Check Answer / Try Again / Next Question)
                                    val isAr = com.example.ui.theme.AppLanguageState.isArabic
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                if (safeQuestionIndex > 0) {
                                                    currentQuestionIndex = safeQuestionIndex - 1
                                                }
                                            },
                                            enabled = safeQuestionIndex > 0,
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, if (safeQuestionIndex > 0) BorderDark else BorderDark.copy(alpha = 0.4f)),
                                            modifier = Modifier.testTag("quiz_prev_button")
                                        ) {
                                            Text(
                                                text = if (isAr) "السابق ←" else "← Previous",
                                                color = if (safeQuestionIndex > 0) TextPrimary else TextMuted
                                            )
                                        }

                                        if (!activeSubmitted) {
                                            Button(
                                                onClick = {
                                                    isSubmitted[activeRealIndex] = true
                                                    val isCorrect = isGrammarAnswerCorrect(userAnswers[activeRealIndex].orEmpty(), activeExercise.answer)
                                                    if (activeRealIndex !in firstAttemptCorrect) {
                                                        firstAttemptCorrect[activeRealIndex] = isCorrect
                                                        if (!isCorrect) {
                                                            onSaveCorrectionEntry?.invoke(
                                                                lesson.id,
                                                                lesson.titleEn,
                                                                activeExercise.prompt,
                                                                userAnswers[activeRealIndex].orEmpty(),
                                                                activeExercise.answer,
                                                                activeExercise.explanationAr
                                                            )
                                                        }
                                                    }
                                                    if (activeIndices.all { isSubmitted[it] == true }) {
                                                        exercisesFinished = true
                                                        val score = exercises.indices.count { i ->
                                                            isGrammarAnswerCorrect(userAnswers[i].orEmpty(), exercises[i].answer)
                                                        }
                                                        val elapsedMs = System.currentTimeMillis() - quizStartTimeMillis
                                                        onSaveProgress(lesson.id, exercises.size, exercises.size, score, true, elapsedMs)
                                                    }
                                                },
                                                enabled = userAnswers[activeRealIndex].orEmpty().isNotBlank(),
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = GoldAccent,
                                                    contentColor = BackgroundDark
                                                ),
                                                modifier = Modifier.testTag("submit_exercise_$activeRealIndex")
                                            ) {
                                                Text(if (isAr) "تحقق من الإجابة" else "Check Answer", fontWeight = FontWeight.Bold)
                                            }
                                        } else {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (isCurrentWrong) {
                                                    OutlinedButton(
                                                        onClick = {
                                                            isSubmitted[activeRealIndex] = false
                                                        },
                                                        border = BorderStroke(1.dp, CoralAccent),
                                                        shape = RoundedCornerShape(10.dp),
                                                        modifier = Modifier.testTag("try_again_button_$activeRealIndex")
                                                    ) {
                                                        Icon(Icons.Default.Refresh, contentDescription = null, tint = CoralAccent, modifier = Modifier.size(16.dp))
                                                        Spacer(Modifier.width(4.dp))
                                                        Text(if (isAr) "حاول مجدداً" else "Try Again", color = CoralAccent, fontWeight = FontWeight.Bold)
                                                    }
                                                }

                                                if (safeQuestionIndex < activeIndices.lastIndex) {
                                                    Button(
                                                        onClick = {
                                                            currentQuestionIndex = safeQuestionIndex + 1
                                                        },
                                                        shape = RoundedCornerShape(10.dp),
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = GoldAccent,
                                                            contentColor = BackgroundDark
                                                        ),
                                                        modifier = Modifier.testTag("quiz_next_button")
                                                    ) {
                                                        Text(if (isAr) "السؤال التالي ←" else "Next Question →", fontWeight = FontWeight.Bold)
                                                    }
                                                } else {
                                                    Button(
                                                        onClick = {
                                                            if (activeMistakeFilterIndices != null) {
                                                                activeMistakeFilterIndices = null
                                                                exercisesFinished = true
                                                                currentQuestionIndex = 0
                                                            } else {
                                                                selectedTab = 2
                                                            }
                                                        },
                                                        shape = RoundedCornerShape(10.dp),
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = GoldAccent,
                                                            contentColor = BackgroundDark
                                                        ),
                                                        modifier = Modifier.testTag("quiz_go_to_writing_button")
                                                    ) {
                                                        Text(
                                                            if (activeMistakeFilterIndices != null) {
                                                                if (isAr) "إنهاء مراجعة الأخطاء ✓" else "Finish Retry ✓"
                                                            } else {
                                                                if (isAr) "كتابة بالذكاء الاصطناعي ←" else "Try AI Writing →"
                                                            },
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Score Summary Card when quiz is completed or already completed
                    if (exercisesFinished || progress?.isCompleted == true || (exercises.isNotEmpty() && exercises.indices.all { isSubmitted[it] == true })) {
                        item {
                            val isAr = com.example.ui.theme.AppLanguageState.isArabic
                            val currentScore = if (userAnswers.isNotEmpty()) {
                                exercises.indices.count { i ->
                                    isGrammarAnswerCorrect(userAnswers[i].orEmpty(), exercises[i].answer)
                                }
                            } else {
                                progress?.bestScore ?: 0
                            }
                            val bestScore = maxOf(progress?.bestScore ?: 0, currentScore)
                            val percentage = if (exercises.isNotEmpty()) (bestScore * 100) / exercises.size else 100

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("exercise_score_card"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                border = BorderStroke(1.dp, EmeraldAccent)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = EmeraldAccent,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = if (isAr) "اكتمل الاختبار: $bestScore من ${exercises.size} ($percentage%)"
                                        else "Quiz Complete: $bestScore / ${exercises.size} ($percentage%)",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = EmeraldAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isAr) "تم حفظ تقدمك في هذا الدرس بنجاح."
                                        else "Your lesson progress has been saved.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        val hasMistakes = bestScore < exercises.size
                                        if (hasMistakes) {
                                            OutlinedButton(
                                                onClick = {
                                                    val mistakeIndices = exercises.indices.filter { idx ->
                                                        val ans = userAnswers[idx].orEmpty()
                                                        !isGrammarAnswerCorrect(ans, exercises[idx].answer)
                                                    }.ifEmpty {
                                                        exercises.indices.toList().takeLast((exercises.size - bestScore).coerceAtLeast(1))
                                                    }
                                                    mistakeIndices.forEach {
                                                        isSubmitted.remove(it)
                                                        userAnswers.remove(it)
                                                    }
                                                    activeMistakeFilterIndices = mistakeIndices
                                                    currentQuestionIndex = 0
                                                    exercisesFinished = false
                                                    isReviewMode = false
                                                },
                                                border = BorderStroke(1.dp, CoralAccent),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.Refresh, contentDescription = null, tint = CoralAccent, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(if (isAr) "إعادة الأخطاء" else "Retry Mistakes", color = CoralAccent, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                userAnswers.clear()
                                                isSubmitted.clear()
                                                firstAttemptCorrect.clear()
                                                mistakeExplanations.clear()
                                                activeMistakeFilterIndices = null
                                                exercisesFinished = false
                                                isReviewMode = false
                                                currentQuestionIndex = 0
                                            },
                                            border = BorderStroke(1.dp, GoldAccent),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (isAr) "إعادة الكل" else "Practice Again", color = GoldAccent, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = { selectedTab = 2 },
                                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = BackgroundDark),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(if (isAr) "كتابة بالذكاء الاصطناعي ←" else "AI Writing →", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: AI WRITING ("Write Your Own Sentence")
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("write_sentence_card"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            border = BorderStroke(1.dp, BorderDark)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(20.dp))
                                    Text(
                                        text = "Write Your Own Sentence",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = GoldAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = SurfaceElevatedDark,
                                    border = BorderStroke(1.dp, BorderDark),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Target Rule: ${lesson.titleEn} (${lesson.level})",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = GoldAccent,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Write an English sentence applying this grammar rule. Gemini will evaluate your grammar and explain improvements in Arabic.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = userSentenceInput,
                                    onValueChange = { userSentenceInput = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("write_sentence_input"),
                                    placeholder = {
                                        Text(
                                            text = "e.g. I can swim very fast, but I can't dive...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextMuted
                                        )
                                    },
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        color = TextPrimary,
                                        textDirection = TextDirection.Ltr
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = GoldAccent,
                                        unfocusedBorderColor = BorderDark,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = false,
                                    minLines = 3,
                                    maxLines = 5
                                )

                                Button(
                                    onClick = {
                                        if (userSentenceInput.isNotBlank()) {
                                            coroutineScope.launch {
                                                isCheckingSentence = true
                                                sentenceCheckError = null
                                                sentenceCheckResult = null
                                                val res = onCheckSentence(lesson, userSentenceInput.trim())
                                                isCheckingSentence = false
                                                if (res.isSuccess) {
                                                    sentenceCheckResult = res.getOrThrow()
                                                } else {
                                                    sentenceCheckError = res.exceptionOrNull()?.message ?: "Check failed"
                                                }
                                            }
                                        }
                                    },
                                    enabled = userSentenceInput.isNotBlank() && !isCheckingSentence,
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(vertical = 12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = BackgroundDark),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("check_sentence_button")
                                ) {
                                    if (isCheckingSentence) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = BackgroundDark, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Evaluating with Gemini...", fontWeight = FontWeight.Bold)
                                    } else {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Check Sentence with AI", fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (sentenceCheckResult != null) {
                                    val result = sentenceCheckResult!!
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("sentence_feedback_card"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = SurfaceElevatedDark),
                                        border = BorderStroke(1.dp, if (result.isCorrect) EmeraldAccent else CoralAccent)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = if (result.isCorrect) "✓ Correct!" else "⚠ Needs Refinement",
                                                color = if (result.isCorrect) EmeraldAccent else CoralAccent,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                            if (result.correctedSentence.isNotBlank() && !result.isCorrect) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = BackgroundDark,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = "Corrected: ${result.correctedSentence}",
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            textDirection = TextDirection.Ltr,
                                                            textAlign = TextAlign.Left
                                                        ),
                                                        color = EmeraldAccent,
                                                        fontWeight = FontWeight.SemiBold,
                                                        modifier = Modifier.padding(10.dp)
                                                    )
                                                }
                                            }
                                            BidiText(
                                                text = result.explanationAr,
                                                style = ArabicTypography.bodyMedium,
                                                color = TextPrimary
                                            )
                                            if (result.betterVersion.isNotBlank()) {
                                                Text(
                                                    text = "Natural alternative: \"${result.betterVersion}\"",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        textDirection = TextDirection.Ltr,
                                                        textAlign = TextAlign.Left
                                                    ),
                                                    color = GoldAccent,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }

                                if (sentenceCheckError != null) {
                                    Text(text = sentenceCheckError!!, color = CoralAccent, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showChatSheet) {
        LessonChatBottomSheet(
            lesson = lesson,
            chatHistory = chatHistory,
            onDismiss = { showChatSheet = false },
            onAskAi = onAskAi
        )
    }
}

private val INLINE_NUMBERED_ITEM_SPLIT_REGEX =
    Regex("""(?<=[:.؟!])\s+(?=(\d+|[١-٩])[)-]\s+)""")

/**
 * Splits a grammar lesson's `explanationAr` into structured lines, breaking inline numbered items
 * (such as `تُقسم الصفات إلى نوعين: 1) ... 2) ...`) into distinct lines so each renders as its own card.
 */
private fun splitGrammarExplanationLines(rawExplanation: String): List<String> {
    return rawExplanation
        .lines()
        .flatMap { rawLine ->
            INLINE_NUMBERED_ITEM_SPLIT_REGEX.split(rawLine.trim())
        }
        .map { it.trim() }
        .filter { it.isNotEmpty() }
}

/**
 * Parses a grammar lesson's `explanationAr` into structured visual sub-cards:
 * - Core concept overview paragraphs
 * - Gold Formula Callout Box (`التركيب:`) with RTL-aligned sparkle header and full-width formula body
 * - Dedicated Example Callout Cards (`مثال:` and `-` bullets)
 * - Numbered Rules & Common Mistake (`✗` / `✓`) cards
 */
@Composable
fun StructuredGrammarExplanation(
    lesson: GrammarLessonEntity,
    modifier: Modifier = Modifier
) {
    val lines = remember(lesson.explanationAr) {
        splitGrammarExplanationLines(lesson.explanationAr)
    }

    val numberedRegex = remember { Regex("""^(\d+|[١-٩])[)-.]\s*(.+)$""") }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, BorderDark)
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Clean English Header (Duplicate Level Badge removed)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = GoldAccent,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Rule & Formula",
                    style = MaterialTheme.typography.titleMedium,
                    color = GoldAccent,
                    fontWeight = FontWeight.Bold
                )
            }

            lines.forEach { rawLine ->
                val numberedMatch = numberedRegex.find(rawLine)

                when {
                    // 1. Formula Callout Box (Only render if formulaBody is not blank)
                    rawLine.startsWith("التركيب") || rawLine.startsWith("القاعدة:") || rawLine.startsWith("الصيغة:") -> {
                        val hasColon = rawLine.contains(":")
                        val headerLabel = if (hasColon) "${rawLine.substringBefore(":").trim()}:" else rawLine.trim()
                        val formulaBody = if (hasColon) rawLine.substringAfter(":").trim() else ""

                        if (formulaBody.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0x14F5C542), // rgba(245, 197, 66, 0.08)
                                border = BorderStroke(1.dp, BiDiGoldPillBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.Start)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = GoldAccent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = headerLabel,
                                                style = ArabicTypography.bodyLarge,
                                                color = GoldAccent,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    // Explicit LTR inner box to protect operators like + and |
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0x4D000000),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = formulaBody,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontFamily = InterFontFamily,
                                                    textDirection = TextDirection.Ltr,
                                                    textAlign = TextAlign.Left
                                                ),
                                                color = TextPrimary,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Sub-section Headers: Notes or Common Mistakes
                    rawLine == "أمثلة:" || rawLine.startsWith("ملاحظات") || rawLine.startsWith("تنبيه") -> {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.Start)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = rawLine,
                                    style = ArabicTypography.titleMedium,
                                    color = GoldAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    rawLine == "أخطاء شائعة:" || rawLine.startsWith("أخطاء شائعة") || rawLine.startsWith("خطأ شائع:") || rawLine.startsWith("خطأ شائع لدى") -> {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.Start)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = BiDiRedPillText,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = rawLine,
                                    style = ArabicTypography.titleMedium,
                                    color = BiDiRedPillText,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // 3. Standalone Example Lines (`مثال: ...`)
                    rawLine.startsWith("مثال:") || rawLine.startsWith("مثال :") -> {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SurfaceElevatedDark,
                            border = BorderStroke(1.dp, BorderDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            BiDiParsedText(
                                text = rawLine,
                                style = ArabicTypography.bodyMedium,
                                color = TextPrimary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            )
                        }
                    }

                    // 4. Inline Bullet Items (- item / • item)
                    rawLine.startsWith("-") || rawLine.startsWith("•") -> {
                        val cleanBullet = rawLine.removePrefix("-").removePrefix("•").trim()
                        val isCommonMistake = cleanBullet.contains("خطأ") || cleanBullet.contains("✗") ||
                            cleanBullet.contains("الصواب") || cleanBullet.contains("والصحيح")

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isCommonMistake) Color(0x0AF87171) else SurfaceElevatedDark,
                            border = BorderStroke(
                                1.dp,
                                if (isCommonMistake) BiDiRedPillBorder else BorderDark
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            BiDiParsedText(
                                text = cleanBullet,
                                style = ArabicTypography.bodyMedium,
                                color = TextPrimary,
                                isMistakeTheme = isCommonMistake,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            )
                        }
                    }

                    // 5. Numbered Rule / Common Mistake Cards (1), 2), 3))
                    numberedMatch != null -> {
                        val numLabel = numberedMatch.groupValues[1]
                        val bodyText = numberedMatch.groupValues[2]
                        val isCommonMistake = bodyText.contains("خطأ") || bodyText.contains("✗") ||
                            bodyText.contains("الصواب") || bodyText.contains("والصحيح")

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isCommonMistake) Color(0x0AF87171) else SurfaceElevatedDark,
                            border = BorderStroke(
                                1.dp,
                                if (isCommonMistake) BiDiRedPillBorder else BorderDark
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // High-Contrast Circular Number Badge (26dp)
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isCommonMistake) BiDiRedPillBg else BiDiGoldPillBg,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isCommonMistake) BiDiRedPillBorder else BiDiGoldPillBorder
                                        ),
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = numLabel,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 13.sp,
                                                    fontFamily = InterFontFamily
                                                ),
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCommonMistake) BiDiRedPillText else GoldAccent
                                            )
                                        }
                                    }

                                    // Parsed Body with Auto-Extracted Example Box & Inline English Pills
                                    BiDiParsedText(
                                        text = bodyText,
                                        style = ArabicTypography.bodyMedium,
                                        color = TextPrimary,
                                        isMistakeTheme = isCommonMistake,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // 6. Standard Concept Explanation Line
                    else -> {
                        BiDiParsedText(
                            text = rawLine,
                            style = ArabicTypography.bodyLarge,
                            color = TextPrimary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

/**
 * Normalizes grammar answers by trimming, lowercasing, and normalizing apostrophes/whitespace.
 */
fun normalizeGrammarAnswer(input: String): String {
    return input.trim()
        .lowercase()
        .replace("’", "'")
        .replace("`", "'")
        .replace("‘", "'")
        .replace(Regex("""\s+"""), " ")
}

/**
 * Smart answer matching: checks exact matches and standard English contraction equivalences
 * (e.g. can't == cannot == can not).
 */
fun isGrammarAnswerCorrect(userAnswer: String, expectedAnswer: String): Boolean {
    val u = normalizeGrammarAnswer(userAnswer)
    val e = normalizeGrammarAnswer(expectedAnswer)
    if (u == e) return true

    fun expand(s: String): String {
        return s
            .replace("can't", "cannot")
            .replace("can not", "cannot")
            .replace("won't", "will not")
            .replace("n't", " not")
            .replace("'re", " are")
            .replace("'m", " am")
            .replace("'s", " is")
            .replace("'ve", " have")
            .replace("'ll", " will")
            .replace("'d", " would")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    if (expand(u) == expand(e)) return true
    val uNoPunct = u.trimEnd('.', '?', '!')
    val eNoPunct = e.trimEnd('.', '?', '!')
    return uNoPunct == eNoPunct || expand(uNoPunct) == expand(eNoPunct)
}

/**
 * Enhanced Grammar Exercise Prompt Renderer:
 * - Detects blanks (____, ___, [...]) and renders a distinct, styled blank slot pill.
 * - Dynamically displays the user's selected/typed answer inside the blank slot.
 * - Extracts parenthetical clues (e.g. `(not / can)` or `(bus)`) into a dedicated clue chip below the sentence.
 * - Recovers blanks if the clue was in place of a stripped blank (e.g. `He (not / can) cook.`).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GrammarExercisePrompt(
    prompt: String,
    currentAnswer: String,
    submitted: Boolean,
    isCorrect: Boolean,
    correctAnswer: String,
    modifier: Modifier = Modifier
) {
    val clueRegex = Regex("""\(([^)]+)\)""")
    val clueMatch = clueRegex.find(prompt)
    val extractedClue = clueMatch?.groupValues?.get(1)?.trim().orEmpty()
    val displayClue = extractedClue.removePrefix("use:").removePrefix("clue:").removePrefix("hint:").trim()

    var sentenceText = if (clueMatch != null) {
        prompt.replace(clueMatch.value, "").replace(Regex("""\s+"""), " ").trim()
    } else {
        prompt.trim()
    }

    val blankRegex = Regex("""_{2,}|\[_+\]|\[\.\.\.\]|\.\.\.{2,}""")
    var blankMatch = blankRegex.find(sentenceText)

    if (blankMatch == null && clueMatch != null) {
        val clueStart = clueMatch.range.first
        val beforeClue = prompt.substring(0, clueStart).trimEnd()
        val afterClue = prompt.substring(clueMatch.range.last + 1).trimStart()
        sentenceText = "$beforeClue ____ $afterClue".trim()
        blankMatch = blankRegex.find(sentenceText)
    }

    if (blankMatch != null) {
        val blankRange = blankMatch.range
        val prefix = sentenceText.substring(0, blankRange.first).trim()
        val suffix = sentenceText.substring(blankRange.last + 1).trim()
        val prefixWords = remember(prefix) {
            prefix.split(Regex("""\s+""")).filter { it.isNotBlank() }
        }
        val suffixWords = remember(suffix) {
            suffix.split(Regex("""\s+""")).filter { it.isNotBlank() }
        }

        // English exercise prompts, blanks, and clues must always flow left-to-right
        // even when the surrounding UI chrome is mirrored in Arabic RTL mode.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Card(
                modifier = modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceElevatedDark),
                border = BorderStroke(1.dp, BorderDark)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)
                    ) {
                        for (word in prefixWords) {
                            Text(
                                text = word,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    textDirection = TextDirection.Ltr,
                                    textAlign = TextAlign.Left
                                ),
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                submitted && isCorrect -> EmeraldAccent.copy(alpha = 0.22f)
                                submitted && !isCorrect -> CoralAccent.copy(alpha = 0.22f)
                                currentAnswer.isNotBlank() -> GoldAccent.copy(alpha = 0.18f)
                                else -> BackgroundDark
                            },
                            border = BorderStroke(
                                1.5.dp,
                                when {
                                    submitted && isCorrect -> EmeraldAccent
                                    submitted && !isCorrect -> CoralAccent
                                    currentAnswer.isNotBlank() -> GoldAccent
                                    else -> GoldAccent.copy(alpha = 0.6f)
                                }
                            ),
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .testTag("dynamic_blank_slot")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (currentAnswer.isNotBlank()) {
                                    Text(
                                        text = currentAnswer,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = when {
                                                submitted && isCorrect -> EmeraldAccent
                                                submitted && !isCorrect -> CoralAccent
                                                else -> GoldAccent
                                            }
                                        )
                                    )
                                    if (submitted) {
                                        Text(
                                            text = if (isCorrect) "✓" else "✗",
                                            color = if (isCorrect) EmeraldAccent else CoralAccent,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "[  ______  ]",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = GoldAccent.copy(alpha = 0.75f),
                                            letterSpacing = 1.sp
                                        )
                                    )
                                }
                            }
                        }

                        for (word in suffixWords) {
                            Text(
                                text = word,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    textDirection = TextDirection.Ltr,
                                    textAlign = TextAlign.Left
                                ),
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }
                    }

                    if (displayClue.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = GoldAccentDim,
                            border = BorderStroke(1.dp, GoldAccentBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Clue: $displayClue",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = GoldAccent
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            BidiText(
                text = prompt,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun ExerciseItemView(
    index: Int,
    exercise: GrammarExerciseItem,
    currentAnswer: String,
    submitted: Boolean,
    onAnswerChange: (String) -> Unit,
    onSubmit: () -> Unit,
    mistakeExplanation: String?,
    isExplainingMistake: Boolean,
    onExplainMistakeClick: () -> Unit
) {
    val isCorrect = submitted && isGrammarAnswerCorrect(currentAnswer, exercise.answer)
    val isWrong = submitted && !isCorrect

    val isAr = com.example.ui.theme.AppLanguageState.isArabic
    val typeLabel = when (exercise.type.lowercase()) {
        "mcq" -> if (isAr) "اختيار من متعدد" else "Multiple Choice"
        "reorder" -> if (isAr) "ترتيب الكلمات" else "Word Order"
        "fill" -> if (isAr) "أكمل الفراغ" else "Fill in the Blank"
        "correct" -> if (isAr) "تصحيح الخطأ" else "Error Correction"
        else -> exercise.type.uppercase()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("exercise_item_$index"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = GoldAccentDim,
                border = BorderStroke(1.dp, GoldAccentBorder)
            ) {
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldAccent,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            if (submitted) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isCorrect) EmeraldAccent.copy(alpha = 0.2f) else CoralAccent.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = if (isCorrect) "✓ Correct" else "✗ Incorrect",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCorrect) EmeraldAccent else CoralAccent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        GrammarExercisePrompt(
            prompt = exercise.prompt,
            currentAnswer = currentAnswer,
            submitted = submitted,
            isCorrect = isCorrect,
            correctAnswer = exercise.answer
        )

        // Keep English grammar answer choices and English answer text inputs strictly LTR.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            when (exercise.type.lowercase()) {
                "mcq" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        exercise.options.forEachIndexed { optIdx, option ->
                            val isSelected = currentAnswer == option
                            val optionLetter = ('A' + optIdx).toString()
                            val optionIsCorrect = isGrammarAnswerCorrect(option, exercise.answer)
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(enabled = !submitted) { onAnswerChange(option) }
                                    .testTag("mcq_option_${index}_$option"),
                                shape = RoundedCornerShape(10.dp),
                                color = when {
                                    submitted && optionIsCorrect -> EmeraldAccent.copy(alpha = 0.18f)
                                    submitted && isSelected && isWrong -> CoralAccent.copy(alpha = 0.18f)
                                    isSelected -> GoldAccent.copy(alpha = 0.18f)
                                    else -> SurfaceElevatedDark
                                },
                                border = BorderStroke(
                                    1.dp,
                                    when {
                                        submitted && optionIsCorrect -> EmeraldAccent
                                        submitted && isSelected && isWrong -> CoralAccent
                                        isSelected -> GoldAccent
                                        else -> BorderDark
                                    }
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = when {
                                            submitted && optionIsCorrect -> EmeraldAccent
                                            submitted && isSelected && isWrong -> CoralAccent
                                            isSelected -> GoldAccent
                                            else -> BackgroundDark
                                        },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = optionLetter,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected || (submitted && optionIsCorrect)) BackgroundDark else TextSecondary
                                            )
                                        }
                                    }
                                    Text(
                                        text = option,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            textDirection = TextDirection.Ltr,
                                            textAlign = TextAlign.Left
                                        ),
                                        color = TextPrimary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
                "fill", "correct" -> {
                    OutlinedTextField(
                        value = currentAnswer,
                        onValueChange = { if (!submitted) onAnswerChange(it) },
                        enabled = !submitted,
                        placeholder = { Text("Type your answer...", color = TextMuted) },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = TextPrimary,
                            textDirection = TextDirection.Ltr
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("fill_input_$index"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldAccent,
                            unfocusedBorderColor = BorderDark,
                            disabledBorderColor = if (isCorrect) EmeraldAccent else if (isWrong) CoralAccent else BorderDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            disabledTextColor = TextPrimary
                        )
                    )
                }
                "reorder" -> {
                    ReorderExerciseView(
                        options = exercise.options,
                        currentAnswer = currentAnswer,
                        submitted = submitted,
                        onAnswerChange = onAnswerChange
                    )
                }
            }
        }

        if (submitted) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(if (isWrong) "wrong_explanation_card_$index" else "correct_explanation_card_$index"),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceElevatedDark),
                border = BorderStroke(
                    1.dp,
                    if (isCorrect) EmeraldAccent.copy(alpha = 0.5f) else CoralAccent.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isCorrect) "✓ Correct: ${exercise.answer}" else "Correct answer: ${exercise.answer}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            textDirection = TextDirection.Ltr,
                            textAlign = TextAlign.Left
                        ),
                        color = EmeraldAccent,
                        fontWeight = FontWeight.Bold
                    )
                    if (exercise.explanationAr.isNotBlank()) {
                        BidiText(
                            text = exercise.explanationAr,
                            style = ArabicTypography.bodyMedium,
                            color = TextSecondary
                        )
                    }

                    if (isWrong) {
                        if (mistakeExplanation == null) {
                            Button(
                                onClick = onExplainMistakeClick,
                                enabled = !isExplainingMistake,
                                colors = ButtonDefaults.buttonColors(containerColor = CoralAccent.copy(alpha = 0.16f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("explain_mistake_button_$index")
                            ) {
                                if (isExplainingMistake) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = CoralAccent, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Analyzing mistake...", color = CoralAccent)
                                } else {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CoralAccent, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Explain my mistake (AI)", color = CoralAccent, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CoralAccent.copy(alpha = 0.1f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("ai_mistake_explanation_$index")
                            ) {
                                BidiText(
                                    text = mistakeExplanation,
                                    style = ArabicTypography.bodyMedium,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReorderExerciseView(
    options: List<String>,
    currentAnswer: String,
    submitted: Boolean,
    onAnswerChange: (String) -> Unit
) {
    val selectedWords = remember(currentAnswer) {
        if (currentAnswer.isBlank()) mutableListOf() else currentAnswer.split(" ").filter { it.isNotBlank() }.toMutableList()
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Display selected sentence with interactive removable word chips
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp)),
            shape = RoundedCornerShape(10.dp),
            color = SurfaceElevatedDark,
            border = BorderStroke(1.dp, if (selectedWords.isNotEmpty()) GoldAccentBorder else BorderDark)
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedWords.isEmpty()) {
                        Text(
                            text = "Tap words below in correct order...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        FlowRow(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            selectedWords.forEachIndexed { wordIdx, word ->
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable(enabled = !submitted) {
                                            val updated = selectedWords.toMutableList().also { it.removeAt(wordIdx) }
                                            onAnswerChange(updated.joinToString(" "))
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    color = GoldAccentDim,
                                    border = BorderStroke(1.dp, GoldAccent)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = word,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                textDirection = TextDirection.Ltr
                                            ),
                                            color = TextPrimary
                                        )
                                        if (!submitted) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove $word",
                                                tint = GoldAccent,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (!submitted && currentAnswer.isNotBlank()) {
                        IconButton(onClick = { onAnswerChange("") }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
                        }
                    }
                }
            }
        }

        // Available words to tap
        if (!submitted) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    options.forEach { word ->
                        val isUsed = selectedWords.count { it == word } >= options.count { it == word }
                        FilterChip(
                            selected = isUsed,
                            onClick = {
                                if (!isUsed) {
                                    val newSentence = if (currentAnswer.isBlank()) word else "$currentAnswer $word"
                                    onAnswerChange(newSentence)
                                } else {
                                    val lastIdx = selectedWords.lastIndexOf(word)
                                    if (lastIdx >= 0) {
                                        val updated = selectedWords.toMutableList().also { it.removeAt(lastIdx) }
                                        onAnswerChange(updated.joinToString(" "))
                                    }
                                }
                            },
                            label = { Text(word) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = SurfaceElevatedDark,
                                labelColor = TextPrimary,
                                selectedContainerColor = BorderDark,
                                selectedLabelColor = TextMuted
                            )
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonChatBottomSheet(
    lesson: GrammarLessonEntity,
    chatHistory: List<AiHistoryEntity>,
    onDismiss: () -> Unit,
    onAskAi: suspend (GrammarLessonEntity, String) -> Result<String>
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var inputQuestion by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val listState = rememberLazyListState()

    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .testTag("lesson_chat_bottom_sheet"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.QuestionAnswer, contentDescription = null, tint = GoldAccent)
                    Column {
                        Text(
                            text = "Ask AI about this lesson",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = lesson.titleEn,
                            style = MaterialTheme.typography.bodySmall,
                            color = GoldAccent
                        )
                        BidiText(
                            text = cleanGrammarArabicTitle(lesson.titleAr),
                            style = ArabicTypography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            HorizontalDivider(color = BorderDark)

            // Message list with flexible height that shrinks cleanly when keyboard opens
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(min = 100.dp, max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (chatHistory.isEmpty()) {
                    item {
                        Text(
                            text = "Ask anything about this grammar rule! Gemini will answer in simple Arabic with helpful English examples.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }

                items(chatHistory, key = { it.id }) { msg ->
                    val isUser = msg.role == "user"
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isUser) GoldAccent.copy(alpha = 0.2f) else SurfaceElevatedDark,
                            border = BorderStroke(1.dp, if (isUser) GoldAccent else BorderDark)
                        ) {
                            BidiText(
                                text = msg.text,
                                style = if (isUser) MaterialTheme.typography.bodyMedium else ArabicTypography.bodyMedium,
                                color = TextPrimary,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }

            if (errorMessage != null) {
                Text(text = errorMessage!!, color = CoralAccent, style = MaterialTheme.typography.bodySmall)
            }

            val canSend = inputQuestion.isNotBlank() && !isSending
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputQuestion,
                    onValueChange = { inputQuestion = it },
                    placeholder = {
                        Text(
                            text = "Ask a question in Arabic or English...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldAccent,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedPlaceholderColor = TextMuted,
                        unfocusedPlaceholderColor = TextMuted,
                        cursorColor = GoldAccent,
                        focusedContainerColor = SurfaceElevatedDark,
                        unfocusedContainerColor = SurfaceElevatedDark
                    ),
                    maxLines = 2
                )

                FilledIconButton(
                    onClick = {
                        val q = inputQuestion.trim()
                        if (q.isNotBlank()) {
                            coroutineScope.launch {
                                isSending = true
                                errorMessage = null
                                inputQuestion = ""
                                val res = onAskAi(lesson, q)
                                isSending = false
                                if (res.isFailure) {
                                    errorMessage = res.exceptionOrNull()?.message ?: "AI request failed"
                                }
                            }
                        }
                    },
                    enabled = canSend,
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = GoldAccent,
                        contentColor = BackgroundDark,
                        disabledContainerColor = SurfaceElevatedDark,
                        disabledContentColor = TextMuted
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("chat_send_button")
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = GoldAccent,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddGrammarLessonsDialog(
    initialTab: Int = 0,
    onDismiss: () -> Unit,
    onCreateWithAi: suspend (level: String, topic: String) -> Result<Unit>,
    onImportJsonUris: (List<Uri>) -> Unit,
    onImportJsonText: (suspend (String) -> Result<Int>)?,
    onSyncAssets: () -> Unit,
    onSuccessMessage: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    val tabTitles = listOf("📁 File", "📋 Paste", "✨ AI", "🔄 Assets")
    val coroutineScope = rememberCoroutineScope()

    // Tab 0: File Picker
    val jsonFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onImportJsonUris(uris)
            onSuccessMessage("Importing ${uris.size} JSON file(s)...")
            onDismiss()
        }
    }

    // Tab 1: Paste JSON
    var pasteText by remember { mutableStateOf("") }
    var isImportingText by remember { mutableStateOf(false) }
    var pasteError by remember { mutableStateOf<String?>(null) }

    // Tab 2: AI Create
    var selectedLevel by remember { mutableStateOf("B1") }
    var topicInput by remember { mutableStateOf("Future continuous") }
    var isCreatingAi by remember { mutableStateOf(false) }
    var aiErrorMessage by remember { mutableStateOf<String?>(null) }
    val levelOptions = listOf("A1", "A2", "B1", "B2", "C1", "C2")
    val topicSuggestions = listOf("Present continuous", "Past perfect", "Modals of obligation", "Passive voice", "Reported speech", "Mixed conditionals")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Add, contentDescription = null, tint = GoldAccent)
                Text("Add Grammar Lessons", color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Tab Row
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = SurfaceElevatedDark,
                    contentColor = GoldAccent,
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = GoldAccent
                            )
                        }
                    }
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    color = if (selectedTab == index) GoldAccent else TextSecondary
                                )
                            }
                        )
                    }
                }

                when (selectedTab) {
                    0 -> {
                        // Import JSON File
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Select your grammar JSON file from your phone storage, Google Drive, or Downloads. Lessons are upserted by ID without resetting your progress.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )

                            Button(
                                onClick = {
                                    jsonFilePickerLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = BackgroundDark),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("dialog_pick_json_file_button")
                            ) {
                                Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Choose JSON File from Device", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    1 -> {
                        // Paste JSON
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Paste JSON lessons:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )

                                Text(
                                    text = "Sample Format",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GoldAccent,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(SurfaceElevatedDark)
                                        .clickable {
                                            pasteText = """[{"id":"g_custom_1","level":"B1","topic":"Conditionals","title_en":"Second conditional","title_ar":"الشرط الثاني","explanation_ar":"يُستخدم للتعبير عن مواقف خيالية أو غير محتملة في الحاضر","examples":[{"en":"If I had a car, I would drive to work.","ar":"لو كان لدي سيارة لقدت إلى العمل."}],"exercises":[{"type":"mcq","prompt":"If I ___ rich, I would buy a boat.","options":["were","was","am"],"answer":"were","explanation_ar":"نستخدم were مع جميع الضمائر في الشرط الثاني"}]}]"""
                                        }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            OutlinedTextField(
                                value = pasteText,
                                onValueChange = {
                                    pasteText = it
                                    pasteError = null
                                },
                                placeholder = { Text("[{\"id\": \"...\", \"title_en\": \"...\", ...}]") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .testTag("dialog_paste_json_textfield"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldAccent,
                                    unfocusedBorderColor = BorderDark,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            if (pasteError != null) {
                                Text(text = pasteError!!, color = CoralAccent, style = MaterialTheme.typography.bodySmall)
                            }

                            Button(
                                onClick = {
                                    if (pasteText.isNotBlank()) {
                                        coroutineScope.launch {
                                            isImportingText = true
                                            pasteError = null
                                            val res = onImportJsonText?.invoke(pasteText)
                                            isImportingText = false
                                            if (res != null && res.isSuccess) {
                                                onSuccessMessage("Successfully imported ${res.getOrNull()} lesson(s)!")
                                                onDismiss()
                                            } else {
                                                pasteError = res?.exceptionOrNull()?.message ?: "Failed to import JSON"
                                            }
                                        }
                                    }
                                },
                                enabled = pasteText.isNotBlank() && !isImportingText,
                                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = BackgroundDark),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("dialog_paste_import_button")
                            ) {
                                if (isImportingText) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = BackgroundDark, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Importing...")
                                } else {
                                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Validate & Import JSON", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    2 -> {
                        // Create with AI
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Select CEFR Level:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                levelOptions.forEach { lvl ->
                                    FilterChip(
                                        selected = selectedLevel == lvl,
                                        onClick = { selectedLevel = lvl },
                                        label = { Text(lvl) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = GoldAccent.copy(alpha = 0.2f),
                                            selectedLabelColor = GoldAccent
                                        ),
                                        modifier = Modifier.testTag("ai_level_$lvl")
                                    )
                                }
                            }

                            Text("Grammar Topic:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            OutlinedTextField(
                                value = topicInput,
                                onValueChange = { topicInput = it },
                                placeholder = { Text("e.g. Second conditional") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("ai_lesson_topic_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldAccent,
                                    unfocusedBorderColor = BorderDark,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                topicSuggestions.forEach { suggestion ->
                                    Text(
                                        text = suggestion,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GoldAccent,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(SurfaceElevatedDark)
                                            .clickable { topicInput = suggestion }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (aiErrorMessage != null) {
                                Text(text = aiErrorMessage!!, color = CoralAccent, style = MaterialTheme.typography.bodySmall)
                            }

                            Button(
                                onClick = {
                                    if (topicInput.isNotBlank()) {
                                        coroutineScope.launch {
                                            isCreatingAi = true
                                            aiErrorMessage = null
                                            val res = onCreateWithAi(selectedLevel, topicInput.trim())
                                            isCreatingAi = false
                                            if (res.isSuccess) {
                                                onSuccessMessage("Successfully generated lesson with AI!")
                                                onDismiss()
                                            } else {
                                                aiErrorMessage = res.exceptionOrNull()?.message ?: "AI lesson generation failed"
                                            }
                                        }
                                    }
                                },
                                enabled = topicInput.isNotBlank() && !isCreatingAi,
                                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = BackgroundDark),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("submit_create_lesson_button")
                            ) {
                                if (isCreatingAi) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = BackgroundDark, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Generating with AI...")
                                } else {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Generate Lesson", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    3 -> {
                        // Sync Assets
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Reload all bundled grammar lessons from assets/grammar_lessons.json. Any new lessons or exercises will be added or updated, preserving your progress.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )

                            Button(
                                onClick = {
                                    onSyncAssets()
                                    onSuccessMessage("Synced grammar lessons from assets!")
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = BackgroundDark),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("dialog_sync_assets_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Re-sync grammar_lessons.json from Assets", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateAiLessonDialog(
    onDismiss: () -> Unit,
    onCreate: suspend (level: String, topic: String) -> Result<Unit>
) {
    var selectedLevel by remember { mutableStateOf("B1") }
    var topicInput by remember { mutableStateOf("Future continuous") }
    var isCreating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val levelOptions = listOf("A1", "A2", "B1", "B2", "C1", "C2")
    val topicSuggestions = listOf("Present continuous", "Past perfect", "Modals of obligation", "Passive voice", "Reported speech")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = GoldAccent)
                Text("Create Lesson with AI", color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Select CEFR Level:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    levelOptions.forEach { lvl ->
                        FilterChip(
                            selected = selectedLevel == lvl,
                            onClick = { selectedLevel = lvl },
                            label = { Text(lvl) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldAccent.copy(alpha = 0.2f),
                                selectedLabelColor = GoldAccent
                            ),
                            modifier = Modifier.testTag("ai_level_$lvl")
                        )
                    }
                }

                Text("Grammar Topic:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                OutlinedTextField(
                    value = topicInput,
                    onValueChange = { topicInput = it },
                    placeholder = { Text("e.g. Second conditional") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_lesson_topic_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldAccent,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    topicSuggestions.forEach { suggestion ->
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldAccent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SurfaceElevatedDark)
                                .clickable { topicInput = suggestion }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (errorMessage != null) {
                    Text(text = errorMessage!!, color = CoralAccent, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (topicInput.isNotBlank()) {
                        coroutineScope.launch {
                            isCreating = true
                            errorMessage = null
                            val res = onCreate(selectedLevel, topicInput.trim())
                            isCreating = false
                            if (res.isSuccess) {
                                onDismiss()
                            } else {
                                errorMessage = res.exceptionOrNull()?.message ?: "Lesson generation failed"
                            }
                        }
                    }
                },
                enabled = topicInput.isNotBlank() && !isCreating,
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = BackgroundDark),
                modifier = Modifier.testTag("submit_create_lesson_button")
            ) {
                if (isCreating) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = BackgroundDark, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Generating...")
                } else {
                    Text("Generate Lesson", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !isCreating) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun GrammarRecommendedShelf(
    userLevel: String,
    recommendedLessons: List<GrammarLessonEntity>,
    progressMap: Map<String, GrammarProgressEntity>,
    onSelectLesson: (GrammarLessonEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val levelColor = when (userLevel.uppercase()) {
        "A1", "A2" -> EmeraldAccent
        "B1", "B2" -> SkyAccent
        else -> VioletAccent
    }
    val levelBg = when (userLevel.uppercase()) {
        "A1", "A2" -> EmeraldAccentBg
        "B1", "B2" -> SkyAccentBg
        else -> VioletAccentBg
    }
    val levelAr = when (userLevel.uppercase()) {
        "A1" -> "مبتدئ"
        "A2" -> "ابتدائي"
        "B1" -> "متوسط"
        "B2" -> "فوق المتوسط"
        "C1" -> "متقدم"
        "C2" -> "إتقان"
        else -> userLevel
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("grammar_recommended_shelf"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Recommended for You",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                        maxLines = 1,
                        softWrap = false
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SkyAccentBg,
                        border = BorderStroke(1.dp, SkyAccentBorder)
                    ) {
                        Text(
                            text = userLevel.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                            color = SkyAccent,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "دروس موصى بها لمستواك ($levelAr)",
                    style = ArabicTypography.labelSmall,
                    color = TextSecondary
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceDark,
                border = BorderStroke(1.dp, BorderDark)
            ) {
                Text(
                    text = "${recommendedLessons.size} lessons",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = GoldAccent,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
        ) {
            items(
                items = recommendedLessons,
                key = { "rec_grammar_${it.id}" }
            ) { lesson ->
                val progress = progressMap[lesson.id]
                val isDone = progress?.isCompleted == true
                GrammarRecommendedCard(
                    lesson = lesson,
                    isDone = isDone,
                    levelColor = levelColor,
                    levelBg = levelBg,
                    onClick = { onSelectLesson(lesson) }
                )
            }
        }
    }
}

@Composable
fun GrammarRecommendedCard(
    lesson: GrammarLessonEntity,
    isDone: Boolean,
    levelColor: Color,
    levelBg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(240.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("grammar_recommended_card_${lesson.id}"),
        shape = RoundedCornerShape(14.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, if (isDone) EmeraldAccent.copy(alpha = 0.4f) else BorderDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = levelBg,
                    border = BorderStroke(1.dp, levelColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = lesson.level.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                        color = levelColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDone) EmeraldAccent.copy(alpha = 0.15f) else SurfaceElevatedDark
                ) {
                    Text(
                        text = if (isDone) "✓ Completed" else "Study →",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        ),
                        color = if (isDone) EmeraldAccent else GoldAccent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // English Title: Allow 2 full lines
            Text(
                text = lesson.titleEn,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp),
                color = TextPrimary,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Arabic Title: in gold
            val cleanAr = remember(lesson.titleAr) { cleanGrammarArabicTitle(lesson.titleAr) }
            if (cleanAr.isNotBlank()) {
                Text(
                    text = cleanAr,
                    style = ArabicTypography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = GoldAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Subtle divider
            HorizontalDivider(
                modifier = Modifier.padding(top = 2.dp),
                thickness = 1.dp,
                color = BorderSubtle
            )

            // Category/Topic in bottom metadata
            if (lesson.topic.isNotBlank()) {
                Text(
                    text = "• ${lesson.topic}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

