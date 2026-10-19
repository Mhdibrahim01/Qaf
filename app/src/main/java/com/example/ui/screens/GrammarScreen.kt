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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.example.ui.components.BidiText
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
    onSaveProgress: (lessonId: String, completed: Int, total: Int, score: Int, isDone: Boolean) -> Unit,
    onAskAi: suspend (lesson: GrammarLessonEntity, question: String) -> Result<String>,
    onExplainMistake: suspend (lessonId: String, title: String, prompt: String, wrong: String, correct: String) -> Result<String>,
    onCheckSentence: suspend (lesson: GrammarLessonEntity, sentence: String) -> Result<GrammarSentenceCheckResult>,
    onMorePractice: suspend (lesson: GrammarLessonEntity) -> Result<List<GrammarExerciseItem>>,
    onCreateLessonWithAi: suspend (level: String, topic: String) -> Result<Unit>,
    onSyncAssets: (() -> Unit)? = null,
    onImportJsonUris: ((List<Uri>) -> Unit)? = null,
    onImportJsonText: (suspend (String) -> Result<Int>)? = null,
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
            onBack = { onSelectLesson(null) },
            onSpeakTts = onSpeakTts,
            onOpenReadingText = onOpenReadingText,
            onSaveProgress = onSaveProgress,
            onAskAi = onAskAi,
            onExplainMistake = onExplainMistake,
            onCheckSentence = onCheckSentence,
            onMorePractice = onMorePractice,
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
    modifier: Modifier = Modifier
) {
    var selectedLevelFilter by remember { mutableStateOf("ALL") }
    var selectedTopicFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }

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
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Summary + Compact Action Pills Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${filteredLessons.size} of ${lessons.size} Lessons • $completedCount Completed",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = GoldAccent
                            )
                            Text(
                                text = "CEFR A1–C1 • Bilingual Explanations & Interactive Practice",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = onNavigateToNotebook,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("grammar_open_notebook_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Correction Notebook",
                                    tint = GoldAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = onSyncAssets,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("sync_grammar_assets_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Sync from assets",
                                    tint = GoldAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Compact Header Action Pills: Correction Notebook | Import JSON | Create with AI
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1.1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onNavigateToNotebook() }
                                .testTag("grammar_notebook_banner"),
                            shape = RoundedCornerShape(10.dp),
                            color = SurfaceDark,
                            border = BorderStroke(1.dp, GoldAccentBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Notebook",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary,
                                    maxLines = 1
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onOpenAddLessons(0) }
                                .testTag("import_grammar_json_button"),
                            shape = RoundedCornerShape(10.dp),
                            color = SurfaceDark,
                            border = BorderStroke(1.dp, BorderDark)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.UploadFile,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Import",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = TextPrimary,
                                    maxLines = 1
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1.15f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onOpenAddLessons(2) }
                                .testTag("create_ai_lesson_button"),
                            shape = RoundedCornerShape(10.dp),
                            color = GoldAccentDim,
                            border = BorderStroke(1.dp, GoldAccent)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Create AI",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = GoldAccent,
                                    maxLines = 1
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

                    // CEFR Level Filter Pills (All, A1, A2, B1, B2, C1...)
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("grammar_level_filter_row"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableLevels) { level ->
                            val isSelected = selectedLevelFilter == level
                            val count = if (level == "ALL") lessons.size else (levelCounts[level] ?: 0)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedLevelFilter = level
                                    selectedTopicFilter = "ALL"
                                },
                                label = {
                                    Text(
                                        text = if (level == "ALL") "All ($count)" else "$level ($count)",
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldAccent,
                                    selectedLabelColor = BackgroundDark,
                                    containerColor = SurfaceDark,
                                    labelColor = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = BorderDark,
                                    selectedBorderColor = GoldAccent
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("grammar_level_chip_$level")
                            )
                        }
                    }

                    // Topic Filter Chips
                    if (availableTopics.size > 2) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("grammar_topic_filter_row"),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(availableTopics) { topic ->
                                val isSelected = selectedTopicFilter == topic
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedTopicFilter = topic },
                                    label = {
                                        Text(
                                            text = if (topic == "ALL") "All Topics" else topic,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GoldAccentDim,
                                        selectedLabelColor = GoldAccent,
                                        containerColor = SurfaceDark,
                                        labelColor = TextSecondary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = BorderDark,
                                        selectedBorderColor = GoldAccent
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("grammar_topic_chip_$topic")
                                )
                            }
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
                item {
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
                item {
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
                item {
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
                        val doneInLevel = levelLessons.count { progressMap[it.id]?.isCompleted == true }
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

                items(levelLessons, key = { it.id }) { lesson ->
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
    val total = progress?.totalExercises ?: 0
    val best = progress?.bestScore ?: 0
    val isCompleted = progress?.isCompleted == true
    val percent = if (total > 0) (best.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("lesson_card_${lesson.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, if (isCompleted) EmeraldAccent.copy(alpha = 0.6f) else BorderDark)
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SurfaceElevatedDark
                    ) {
                        Text(
                            text = lesson.topic,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
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

                Text(
                    text = lesson.titleEn,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                BidiText(
                    text = lesson.titleAr,
                    style = ArabicTypography.bodyMedium,
                    color = TextSecondary
                )
            }

            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { if (isCompleted) 1f else percent },
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("lesson_progress_ring_${lesson.id}"),
                    color = if (isCompleted) EmeraldAccent else GoldAccent,
                    trackColor = SurfaceElevatedDark,
                    strokeWidth = 4.dp
                )
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = EmeraldAccent,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = if (total > 0) "${(percent * 100).toInt()}%" else "0%",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
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
    onSaveProgress: (lessonId: String, completed: Int, total: Int, score: Int, isDone: Boolean) -> Unit,
    onAskAi: suspend (lesson: GrammarLessonEntity, question: String) -> Result<String>,
    onExplainMistake: suspend (lessonId: String, title: String, prompt: String, wrong: String, correct: String) -> Result<String>,
    onCheckSentence: suspend (lesson: GrammarLessonEntity, sentence: String) -> Result<GrammarSentenceCheckResult>,
    onMorePractice: suspend (lesson: GrammarLessonEntity) -> Result<List<GrammarExerciseItem>>,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

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

    // Exercise state
    val userAnswers = remember(lesson.id) { mutableStateMapOf<Int, String>() }
    val isSubmitted = remember(lesson.id) { mutableStateMapOf<Int, Boolean>() }
    val mistakeExplanations = remember(lesson.id) { mutableStateMapOf<Int, String>() }
    val isExplainingMistake = remember(lesson.id) { mutableStateMapOf<Int, Boolean>() }
    var exercisesFinished by remember(lesson.id) { mutableStateOf(false) }

    val safeQuestionIndex = if (exercises.isEmpty()) 0 else currentQuestionIndex.coerceIn(0, exercises.lastIndex)

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
            color = Color(0xFF14151B),
            border = BorderStroke(1.dp, BorderDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Segmented 3-Tab Switcher
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = BackgroundDark,
                    border = BorderStroke(1.dp, BorderDark),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val tabs = listOf(
                            "Learn" to "grammar_tab_learn",
                            "Practice (${exercises.size})" to "grammar_tab_practice",
                            "AI Write" to "grammar_tab_write"
                        )
                        tabs.forEachIndexed { idx, (label, tag) ->
                            val isSelected = selectedTab == idx
                            Surface(
                                shape = RoundedCornerShape(9.dp),
                                color = if (isSelected) GoldAccent else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(9.dp))
                                    .clickable { selectedTab = idx }
                                    .testTag(tag)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) BackgroundDark else TextSecondary,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                // Compact AI Tutor Pill Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GoldAccentDim,
                    border = BorderStroke(1.dp, GoldAccent),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showChatSheet = true }
                        .testTag("ask_ai_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QuestionAnswer,
                            contentDescription = "Ask AI about this lesson",
                            tint = GoldAccent,
                            modifier = Modifier.size(16.dp)
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
            contentPadding = PaddingValues(top = 14.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                                            shape = RoundedCornerShape(12.dp),
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
                                                        .padding(end = 8.dp),
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
                                                    BidiText(
                                                        text = ex.ar,
                                                        style = ArabicTypography.bodyMedium,
                                                        color = TextSecondary
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { onSpeakTts(ex.en) },
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .clip(CircleShape)
                                                        .background(GoldAccentDim)
                                                        .testTag("tts_example_button_$index")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.VolumeUp,
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
                                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
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
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { onOpenReadingText(readingText) }
                                                .testTag("read_in_context_item_${readingText.id}"),
                                            shape = RoundedCornerShape(10.dp),
                                            color = SurfaceElevatedDark,
                                            border = BorderStroke(1.dp, BorderDark)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                                                        Spacer(Modifier.height(2.dp))
                                                        BidiText(
                                                            text = readingText.titleAr,
                                                            style = ArabicTypography.bodySmall,
                                                            color = TextSecondary
                                                        )
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
                                    text = "Start Practice Quiz (${exercises.size} Questions) →",
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
                                        Text(
                                            text = if (exercises.isEmpty()) "Practice Quiz" else "Question ${safeQuestionIndex + 1} of ${exercises.size}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = GoldAccent,
                                            fontWeight = FontWeight.Bold
                                        )
                                        val answeredCount = exercises.indices.count { isSubmitted[it] == true }
                                        Text(
                                            text = "$answeredCount of ${exercises.size} completed",
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
                                    val progressFraction = ((safeQuestionIndex + 1).toFloat() / exercises.size.toFloat()).coerceIn(0f, 1f)
                                    LinearProgressIndicator(
                                        progress = { progressFraction },
                                        color = GoldAccent,
                                        trackColor = SurfaceElevatedDark,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                    )

                                    // Step Number Pills Row (Jump to any question 1..N)
                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        itemsIndexed(exercises) { idx, ex ->
                                            val isCurrent = idx == safeQuestionIndex
                                            val sub = isSubmitted[idx] == true
                                            val ans = userAnswers[idx].orEmpty().trim()
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
                                                    .clickable { currentQuestionIndex = idx }
                                                    .testTag("quiz_step_pill_$idx")
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = "${idx + 1}",
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
                                    val activeExercise = exercises[safeQuestionIndex]
                                    val activeSubmitted = isSubmitted[safeQuestionIndex] == true
                                    ExerciseItemView(
                                        index = safeQuestionIndex,
                                        exercise = activeExercise,
                                        currentAnswer = userAnswers[safeQuestionIndex].orEmpty(),
                                        submitted = activeSubmitted,
                                        onAnswerChange = { userAnswers[safeQuestionIndex] = it },
                                        onSubmit = {
                                            isSubmitted[safeQuestionIndex] = true
                                            if (exercises.indices.all { isSubmitted[it] == true }) {
                                                exercisesFinished = true
                                                val score = exercises.indices.count { i ->
                                                    userAnswers[i]?.trim().equals(exercises[i].answer.trim(), ignoreCase = true)
                                                }
                                                onSaveProgress(lesson.id, exercises.size, exercises.size, score, true)
                                            }
                                        },
                                        mistakeExplanation = mistakeExplanations[safeQuestionIndex],
                                        isExplainingMistake = isExplainingMistake[safeQuestionIndex] == true,
                                        onExplainMistakeClick = {
                                            coroutineScope.launch {
                                                isExplainingMistake[safeQuestionIndex] = true
                                                val res = onExplainMistake(
                                                    lesson.id,
                                                    lesson.titleEn,
                                                    activeExercise.prompt,
                                                    userAnswers[safeQuestionIndex].orEmpty(),
                                                    activeExercise.answer
                                                )
                                                isExplainingMistake[safeQuestionIndex] = false
                                                if (res.isSuccess) {
                                                    mistakeExplanations[safeQuestionIndex] = res.getOrThrow()
                                                }
                                            }
                                        }
                                    )

                                    // Step Navigation Footer (Previous / Check Answer / Next Question)
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
                                                text = "← Previous",
                                                color = if (safeQuestionIndex > 0) TextPrimary else TextMuted
                                            )
                                        }

                                        if (!activeSubmitted) {
                                            Button(
                                                onClick = {
                                                    isSubmitted[safeQuestionIndex] = true
                                                    if (exercises.indices.all { isSubmitted[it] == true }) {
                                                        exercisesFinished = true
                                                        val score = exercises.indices.count { i ->
                                                            userAnswers[i]?.trim().equals(exercises[i].answer.trim(), ignoreCase = true)
                                                        }
                                                        onSaveProgress(lesson.id, exercises.size, exercises.size, score, true)
                                                    }
                                                },
                                                enabled = userAnswers[safeQuestionIndex].orEmpty().isNotBlank(),
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = GoldAccent,
                                                    contentColor = BackgroundDark
                                                ),
                                                modifier = Modifier.testTag("submit_exercise_$safeQuestionIndex")
                                            ) {
                                                Text("Check Answer", fontWeight = FontWeight.Bold)
                                            }
                                        } else if (safeQuestionIndex < exercises.lastIndex) {
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
                                                Text("Next Question →", fontWeight = FontWeight.Bold)
                                            }
                                        } else {
                                            Button(
                                                onClick = { selectedTab = 2 },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = GoldAccent,
                                                    contentColor = BackgroundDark
                                                ),
                                                modifier = Modifier.testTag("quiz_go_to_writing_button")
                                            ) {
                                                Text("Try AI Writing →", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Score Summary Card when all questions are completed
                    if (exercisesFinished || (exercises.isNotEmpty() && exercises.indices.all { isSubmitted[it] == true })) {
                        item {
                            val correctCount = exercises.indices.count { i ->
                                userAnswers[i]?.trim().equals(exercises[i].answer.trim(), ignoreCase = true)
                            }
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
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Text(
                                        text = "Quiz Complete: $correctCount / ${exercises.size} (${(correctCount * 100) / exercises.size}%)",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = EmeraldAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Your lesson progress has been saved.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                userAnswers.clear()
                                                isSubmitted.clear()
                                                mistakeExplanations.clear()
                                                exercisesFinished = false
                                                currentQuestionIndex = 0
                                            },
                                            border = BorderStroke(1.dp, GoldAccent),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Practice Again", color = GoldAccent, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = { selectedTab = 2 },
                                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = BackgroundDark),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("AI Writing →", fontWeight = FontWeight.Bold)
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

/**
 * Isolates parenthesized English segments inside RTL Arabic strings with Unicode LTR Isolate
 * (\u2066 ... \u2069) so brackets, ticks (✓/✗), and trailing periods never flip in RTL layout.
 */
private fun formatRtlWithLtrIsolates(raw: String): String {
    val regex = Regex("""\(([A-Za-z0-9][^)]*)\)""")
    return regex.replace(raw) { match ->
        "(\u2066${match.groupValues[1]}\u2069)"
    }
}

/**
 * Parses a grammar lesson's `explanationAr` into structured visual sub-cards:
 * - Core concept overview paragraphs
 * - Gold Formula Callout Box (`التركيب:`)
 * - Inline examples
 * - Numbered Rules & Common Mistake (`✗` / `✓`) cards
 */
@Composable
fun StructuredGrammarExplanation(
    lesson: GrammarLessonEntity,
    modifier: Modifier = Modifier
) {
    val lines = remember(lesson.explanationAr) {
        lesson.explanationAr.lines().map { it.trim() }.filter { it.isNotEmpty() }
    }

    val numberedRegex = remember { Regex("""^(\d+|[١-٩])[\)\-\.]\s*(.+)$""") }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, BorderDark)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Clean English Header + Level Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Rule & Formula",
                        style = MaterialTheme.typography.titleMedium,
                        color = GoldAccent,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = GoldAccentDim,
                    border = BorderStroke(1.dp, GoldAccentBorder)
                ) {
                    Text(
                        text = "${lesson.level} • ${lesson.topic}",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldAccent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            lines.forEach { rawLine ->
                val line = formatRtlWithLtrIsolates(rawLine)
                val numberedMatch = numberedRegex.find(line)

                when {
                    // 1. Formula Callout Box
                    rawLine.startsWith("التركيب") || rawLine.startsWith("القاعدة:") || rawLine.startsWith("الصيغة:") -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = GoldAccentDim,
                            border = BorderStroke(1.dp, GoldAccent),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                BidiText(
                                    text = line,
                                    style = ArabicTypography.bodyLarge,
                                    color = GoldAccent,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // 2. Sub-section Headers (e.g. ملاحظات مهمة: / أمثلة:)
                    rawLine == "أمثلة:" || rawLine.startsWith("ملاحظات") || rawLine.startsWith("تنبيه") -> {
                        Spacer(modifier = Modifier.height(2.dp))
                        BidiText(
                            text = line,
                            style = ArabicTypography.titleMedium,
                            color = GoldAccent,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // 3. Inline Bullet Examples (- I can swim. (...))
                    rawLine.startsWith("-") || rawLine.startsWith("•") -> {
                        val cleanBullet = line.removePrefix("-").removePrefix("•").trim()
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SurfaceElevatedDark,
                            border = BorderStroke(1.dp, BorderDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            BidiText(
                                text = cleanBullet,
                                style = ArabicTypography.bodyMedium,
                                color = TextPrimary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 9.dp)
                            )
                        }
                    }

                    // 4. Numbered Rule / Common Mistake Cards (1), 2), 3), 4))
                    numberedMatch != null -> {
                        val numLabel = numberedMatch.groupValues[1]
                        val bodyText = numberedMatch.groupValues[2]
                        val isCommonMistake = bodyText.contains("خطأ") || bodyText.contains("✗")

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceElevatedDark,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isCommonMistake) CoralAccent.copy(alpha = 0.45f) else BorderDark
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                BidiText(
                                    text = bodyText,
                                    style = ArabicTypography.bodyMedium,
                                    color = TextPrimary,
                                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.3f,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = if (isCommonMistake) CoralAccent.copy(alpha = 0.18f) else GoldAccentDim,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isCommonMistake) CoralAccent else GoldAccentBorder
                                    ),
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = numLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCommonMistake) CoralAccent else GoldAccent
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 5. Standard Concept Explanation Line
                    else -> {
                        BidiText(
                            text = line,
                            style = ArabicTypography.bodyLarge,
                            color = TextPrimary,
                            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.35f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
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
    val isCorrect = submitted && currentAnswer.trim().equals(exercise.answer.trim(), ignoreCase = true)
    val isWrong = submitted && !isCorrect

    val typeLabel = when (exercise.type.lowercase()) {
        "mcq" -> "Multiple Choice"
        "reorder" -> "Word Order"
        "fill" -> "Fill in the Blank"
        "correct" -> "Error Correction"
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

        BidiText(
            text = exercise.prompt,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )

        when (exercise.type.lowercase()) {
            "mcq" -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    exercise.options.forEachIndexed { optIdx, option ->
                        val isSelected = currentAnswer == option
                        val optionLetter = ('A' + optIdx).toString()
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(enabled = !submitted) { onAnswerChange(option) }
                                .testTag("mcq_option_${index}_$option"),
                            shape = RoundedCornerShape(10.dp),
                            color = when {
                                submitted && option == exercise.answer -> EmeraldAccent.copy(alpha = 0.18f)
                                submitted && isSelected && isWrong -> CoralAccent.copy(alpha = 0.18f)
                                isSelected -> GoldAccent.copy(alpha = 0.18f)
                                else -> SurfaceElevatedDark
                            },
                            border = BorderStroke(
                                1.dp,
                                when {
                                    submitted && option == exercise.answer -> EmeraldAccent
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
                                        submitted && option == exercise.answer -> EmeraldAccent
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
                                            color = if (isSelected || (submitted && option == exercise.answer)) BackgroundDark else TextSecondary
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
                            text = formatRtlWithLtrIsolates(exercise.explanationAr),
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
        if (currentAnswer.isBlank()) mutableListOf() else currentAnswer.split(" ").toMutableList()
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Display selected sentence
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp),
            color = SurfaceElevatedDark,
            border = BorderStroke(1.dp, BorderDark)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (currentAnswer.isBlank()) "Tap words below in correct order..." else currentAnswer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (currentAnswer.isBlank()) TextMuted else TextPrimary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                if (!submitted && currentAnswer.isNotBlank()) {
                    IconButton(onClick = { onAnswerChange("") }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
                    }
                }
            }
        }

        // Available words to tap
        if (!submitted) {
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("lesson_chat_bottom_sheet"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.QuestionAnswer, contentDescription = null, tint = GoldAccent)
                    Column {
                        Text(
                            text = "Ask AI about this lesson",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${lesson.titleEn} (${lesson.titleAr})",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            HorizontalDivider(color = BorderDark)

            // Message list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputQuestion,
                    onValueChange = { inputQuestion = it },
                    placeholder = { Text("Ask a question in Arabic or English...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldAccent,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    maxLines = 2
                )

                IconButton(
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
                    enabled = inputQuestion.isNotBlank() && !isSending,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(GoldAccent)
                        .testTag("chat_send_button")
                ) {
                    if (isSending) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = BackgroundDark, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = BackgroundDark)
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
        if (!uris.isNullOrEmpty()) {
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
