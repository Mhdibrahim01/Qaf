package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Translate
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AiHistoryEntity
import com.example.data.local.CorrectionNotebookEntity
import com.example.data.local.EntryEntity
import com.example.data.local.GrammarExerciseItem
import com.example.data.local.GrammarLessonEntity
import com.example.data.local.ReadingProgressEntity
import com.example.data.local.ReadingQuestionItem
import com.example.data.local.ReadingSentenceItem
import com.example.data.local.ReadingTextEntity
import com.example.data.reading.GRAMMAR_LESSON_KEYWORDS
import com.example.data.reading.extractGrammarKeywords
import com.example.ui.components.BidiText
import com.example.ui.components.SlowAudioTurtleButton
import com.example.ui.theme.ArabicTypography
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderDark
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldAccentBg
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldAccentDim
import com.example.ui.theme.GoldAccentBorder
import com.example.ui.theme.SkyAccent
import com.example.ui.theme.SkyAccentBg
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevatedDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletAccent
import com.example.ui.theme.VioletAccentBg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

private val readingJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}

private val READING_WHITESPACE_REGEX = Regex("\\s+")
private val READING_TOKEN_CLEAN_REGEX = Regex("^[^a-zA-Z0-9]+|[^a-zA-Z0-9]+$")
private val READING_LOWER_CLEAN_REGEX = Regex("^[^a-z0-9']+|[^a-z0-9']+$")
private val READING_PAREN_REGEX = Regex("\\(([^)]+)\\)")
private val READING_PUNCT_PAREN_REGEX = Regex("[,:()]")
private val READING_SENTENCE_COUNT_CACHE = java.util.concurrent.ConcurrentHashMap<String, Int>()
private val READING_QUESTION_COUNT_CACHE = java.util.concurrent.ConcurrentHashMap<String, Int>()

private data class ReadingTokenItem(
    val token: String,
    val cleanWord: String,
    val isTarget: Boolean,
    val isGrammar: Boolean,
    val stage: com.example.data.plan.WordStage? = null
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingScreen(
    texts: List<ReadingTextEntity>,
    progressList: List<ReadingProgressEntity>,
    selectedText: ReadingTextEntity?,
    chatHistory: List<AiHistoryEntity>,
    openQuestionHistory: List<AiHistoryEntity>,
    grammarLessons: List<GrammarLessonEntity> = emptyList(),
    onSelectText: (ReadingTextEntity?) -> Unit,
    onSaveProgress: (textId: String, completed: Int, total: Int, score: Int, isDone: Boolean, elapsedMs: Long?) -> Unit,
    onOpenGrammarLesson: (GrammarLessonEntity) -> Unit = {},
    onSaveCorrectionSentence: ((textId: String, lessonId: String, lessonTitle: String, original: String, corrected: String, expAr: String, alt: String, status: String) -> Unit)? = null,
    onCheckCorrectionWithAi: (suspend (CorrectionNotebookEntity) -> Result<Unit>)? = null,
    onNavigateToNotebook: () -> Unit = {},
    onLookupWordLocally: suspend (String) -> List<EntryEntity>,
    onSaveWordFavorite: suspend (deckId: String, word: String, isFavorite: Boolean) -> Unit,
    onGetWordFavoriteStatus: suspend (deckId: String, word: String) -> Boolean,
    onGetSavedTargetWords: suspend (limit: Int) -> List<String>,
    onExplainWordInContext: suspend (textId: String, sentenceIndex: Int, sentenceEn: String, word: String) -> Result<String>,
    onSavePastedText: suspend (title: String, rawText: String, topic: String, level: String) -> Result<ReadingTextEntity>,
    onTranslateAndMakeQuestionsForPastedText: suspend (ReadingTextEntity) -> Result<Unit>,
    onCreateNewReadingTextWithAi: suspend (level: String, topic: String, length: String, targetWords: List<String>, targetGrammarLessonId: String?) -> Result<String>,
    onSimplifyReadingText: suspend (ReadingTextEntity) -> Result<String>,
    onAskAiAboutText: suspend (text: ReadingTextEntity, question: String) -> Result<String>,
    onFeedbackOnOpenQuestion: suspend (text: ReadingTextEntity, prompt: String, answer: String) -> Result<String>,
    onDeleteText: (String) -> Unit,
    onSyncAssets: ((Result<Int>) -> Unit) -> Unit,
    onImportJsonUris: (List<Uri>, (Result<Int>) -> Unit) -> Unit,
    onImportJsonText: suspend (String) -> Result<Int>,
    onSpeakSentence: (String) -> Unit,
    onSpeakSentenceSlow: (String) -> Unit = { onSpeakSentence(it) },
    onStopAudio: () -> Unit,
    ttsSpeechRate: Float = 0.85f,
    userLevel: String = "B2",
    onScheduleWordForToday: suspend (String) -> Boolean = { false },
    storyReadabilityById: Map<String, com.example.data.plan.StoryReadabilityReport> = emptyMap(),
    wordStageMap: Map<String, com.example.data.plan.WordStage> = emptyMap(),
    modifier: Modifier = Modifier
) {
    if (selectedText != null) {
        ReadingDetailScreen(
            text = selectedText,
            progress = progressList.firstOrNull { it.textId == selectedText.id },
            chatHistory = chatHistory,
            openQuestionHistory = openQuestionHistory,
            grammarLessons = grammarLessons,
            onBack = {
                onStopAudio()
                onSelectText(null)
            },
            onSaveProgress = { completed, total, score, isDone, elapsedMs ->
                onSaveProgress(selectedText.id, completed, total, score, isDone, elapsedMs)
            },
            onOpenGrammarLesson = onOpenGrammarLesson,
            onSaveCorrectionSentence = onSaveCorrectionSentence,
            onCheckCorrectionWithAi = onCheckCorrectionWithAi,
            onNavigateToNotebook = onNavigateToNotebook,
            onLookupWordLocally = onLookupWordLocally,
            onSaveWordFavorite = onSaveWordFavorite,
            onGetWordFavoriteStatus = onGetWordFavoriteStatus,
            onExplainWordInContext = { sIdx, sEn, word ->
                onExplainWordInContext(selectedText.id, sIdx, sEn, word)
            },
            onTranslateAndMakeQuestions = {
                onTranslateAndMakeQuestionsForPastedText(selectedText)
            },
            onSimplifyText = {
                onSimplifyReadingText(selectedText)
            },
            onAskAi = { q ->
                onAskAiAboutText(selectedText, q)
            },
            onFeedbackOnOpenQuestion = { prompt, answer ->
                onFeedbackOnOpenQuestion(selectedText, prompt, answer)
            },
            onDeleteText = {
                onDeleteText(selectedText.id)
            },
            onSpeakSentence = onSpeakSentence,
            onSpeakSentenceSlow = onSpeakSentenceSlow,
            onStopAudio = onStopAudio,
            ttsSpeechRate = ttsSpeechRate,
            onScheduleWordForToday = onScheduleWordForToday,
            readabilityReport = storyReadabilityById[selectedText.id],
            wordStageMap = wordStageMap,
            modifier = modifier
        )
    } else {
        ReadingListScreen(
            texts = texts,
            progressList = progressList,
            grammarLessons = grammarLessons,
            onSelectText = onSelectText,
            onCreateWithAi = onCreateNewReadingTextWithAi,
            onSavePastedText = onSavePastedText,
            onGetSavedTargetWords = onGetSavedTargetWords,
            onSyncAssets = onSyncAssets,
            onImportJsonUris = onImportJsonUris,
            onImportJsonText = onImportJsonText,
            onNavigateToNotebook = onNavigateToNotebook,
            userLevel = userLevel,
            storyReadabilityById = storyReadabilityById,
            modifier = modifier
        )
    }
}

// -------------------------------------------------------------------------------------------------
// 1. READING LIST SCREEN
// -------------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingListScreen(
    texts: List<ReadingTextEntity>,
    progressList: List<ReadingProgressEntity>,
    grammarLessons: List<GrammarLessonEntity> = emptyList(),
    onSelectText: (ReadingTextEntity) -> Unit,
    onCreateWithAi: suspend (level: String, topic: String, length: String, targetWords: List<String>, targetGrammarLessonId: String?) -> Result<String>,
    onSavePastedText: suspend (title: String, rawText: String, topic: String, level: String) -> Result<ReadingTextEntity>,
    onGetSavedTargetWords: suspend (limit: Int) -> List<String>,
    onSyncAssets: ((Result<Int>) -> Unit) -> Unit,
    onImportJsonUris: (List<Uri>, (Result<Int>) -> Unit) -> Unit,
    onImportJsonText: suspend (String) -> Result<Int>,
    onNavigateToNotebook: () -> Unit = {},
    userLevel: String = "B2",
    storyReadabilityById: Map<String, com.example.data.plan.StoryReadabilityReport> = emptyMap(),
    modifier: Modifier = Modifier
) {
    var selectedLevelFilter by remember { mutableStateOf("ALL") }
    var selectedTopicFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var initialAddTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val recommendedTexts = remember(texts, userLevel) {
        texts.filter { it.level.equals(userLevel, ignoreCase = true) }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onImportJsonUris(uris) { res ->
                scope.launch {
                    if (res.isSuccess) {
                        snackbarHostState.showSnackbar("Successfully imported ${res.getOrDefault(0)} texts")
                    } else {
                        snackbarHostState.showSnackbar("Import failed: ${res.exceptionOrNull()?.message}")
                    }
                }
            }
        }
    }

    val sentenceCountsById = remember(texts) {
        texts.associate { entity ->
            val count = READING_SENTENCE_COUNT_CACHE.computeIfAbsent(entity.sentencesJson) {
                try {
                    readingJson.decodeFromString<List<ReadingSentenceItem>>(it).size
                } catch (_: Exception) { 0 }
            }
            entity.id to count
        }
    }
    val questionCountsById = remember(texts) {
        texts.associate { entity ->
            val count = READING_QUESTION_COUNT_CACHE.computeIfAbsent(entity.questionsJson) {
                try {
                    readingJson.decodeFromString<List<ReadingQuestionItem>>(it).size
                } catch (_: Exception) { 0 }
            }
            entity.id to count
        }
    }

    val levels = remember { listOf("ALL", "A1", "A2", "B1", "B2", "C1") }
    val levelCounts = remember(texts) {
        val map = mutableMapOf<String, Int>()
        map["ALL"] = texts.size
        texts.forEach { t ->
            val lvl = t.level.uppercase()
            map[lvl] = (map[lvl] ?: 0) + 1
        }
        map
    }

    val topics = remember(texts, selectedLevelFilter) {
        val base = if (selectedLevelFilter == "ALL") texts else texts.filter { it.level.equals(selectedLevelFilter, ignoreCase = true) }
        listOf("ALL") + base.map { it.topic }.filter { it.isNotBlank() }.distinct()
    }

    LaunchedEffect(topics) {
        if (selectedTopicFilter != "ALL" && topics.none { it.equals(selectedTopicFilter, ignoreCase = true) }) {
            selectedTopicFilter = "ALL"
        }
    }

    val filteredTexts = remember(texts, selectedLevelFilter, selectedTopicFilter, searchQuery) {
        val q = searchQuery.trim().lowercase()
        texts.filter { item ->
            val matchesLevel = selectedLevelFilter == "ALL" || item.level.equals(selectedLevelFilter, ignoreCase = true)
            val matchesTopic = selectedTopicFilter == "ALL" || item.topic.equals(selectedTopicFilter, ignoreCase = true)
            val matchesQuery = q.isEmpty() ||
                item.titleEn.lowercase().contains(q) ||
                item.titleAr.lowercase().contains(q) ||
                item.topic.lowercase().contains(q)
            matchesLevel && matchesTopic && matchesQuery
        }
    }

    val completedCount = remember(progressList) {
        progressList.count { it.isCompleted }
    }

    val progressByTextId = remember(progressList) {
        progressList.associateBy { it.textId }
    }

    Scaffold(
        modifier = modifier.testTag("reading_list_screen"),
        containerColor = BackgroundDark,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Compact Header, Action Pills, Search & Filter Controls
            item(key = "reading_header_controls", contentType = "header") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    var showOverflowMenu by remember { mutableStateOf(false) }
                    val levelListState = rememberLazyListState()
                    val topicListState = rememberLazyListState()

                    LaunchedEffect(selectedLevelFilter) {
                        val idx = levels.indexOf(selectedLevelFilter)
                        if (idx >= 0) levelListState.animateScrollToItem(idx)
                    }

                    LaunchedEffect(selectedTopicFilter) {
                        val idx = topics.indexOf(selectedTopicFilter)
                        if (idx >= 0) topicListState.animateScrollToItem(idx)
                    }

                    // 1. Progress Banner Card
                    val totalPassages = texts.size
                    val progressFraction = if (totalPassages > 0) (completedCount.toFloat() / totalPassages.toFloat()).coerceIn(0f, 1f) else 0f
                    val progressPercent = (progressFraction * 100).toInt()

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reading_progress_banner"),
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
                            // Top Row: Completed • Total on Left, + AI Text + Overflow Menu on Right
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "$completedCount Completed",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                                        color = GoldAccent
                                    )
                                    Text(
                                        text = " • $totalPassages Passages",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = TextSecondary
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = GoldAccent.copy(alpha = 0.14f),
                                        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.40f)),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                initialAddTab = 0
                                                showAddDialog = true
                                            }
                                            .testTag("reading_add_fab")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = "Create with AI",
                                                tint = GoldAccent,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "+ AI Text",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = GoldAccent
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
                                                text = { Text("Paste Text", color = TextPrimary) },
                                                leadingIcon = { Icon(Icons.Default.ContentPaste, contentDescription = null, tint = GoldAccent) },
                                                onClick = {
                                                    showOverflowMenu = false
                                                    initialAddTab = 1
                                                    showAddDialog = true
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Import JSON", color = TextPrimary) },
                                                leadingIcon = { Icon(Icons.Default.UploadFile, contentDescription = null, tint = GoldAccent) },
                                                onClick = {
                                                    showOverflowMenu = false
                                                    initialAddTab = 2
                                                    showAddDialog = true
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Sync Assets", color = TextPrimary) },
                                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = GoldAccent) },
                                                onClick = {
                                                    showOverflowMenu = false
                                                    initialAddTab = 3
                                                    showAddDialog = true
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Middle: Progress Bar with linear-gradient(90deg, #F5B82E, #FDE047)
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
                                                    colors = listOf(GoldAccent, Color(0xFFFDE047))
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
                                    text = "CEFR A1–C1 • Graded Bilingual Reading",
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

                    // Row 3: Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                "Search passages by title or topic...",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
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
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reading_search_input")
                    )

                    // Pinned Recommended for You Shelf
                    if (recommendedTexts.isNotEmpty()) {
                        ReadingRecommendedShelf(
                            userLevel = userLevel,
                            recommendedTexts = recommendedTexts,
                            progressByTextId = progressByTextId,
                            onSelectText = onSelectText
                        )
                    }

                    // Row 3: CEFR Level Filter Pills with Auto-Scroll & Edge Fade
                    Box(modifier = Modifier.fillMaxWidth()) {
                        LazyRow(
                            state = levelListState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reading_level_filter_row"),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(end = 24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(
                                items = levels,
                                key = { it },
                                contentType = { "level_chip" }
                            ) { level ->
                                val isSelected = selectedLevelFilter == level
                                val count = levelCounts[level] ?: 0
                                val isUserLevel = level.equals(userLevel, ignoreCase = true) && level != "ALL"
                                val labelText = when {
                                    level == "ALL" -> "All ($count)"
                                    isUserLevel -> "$level ★ ($count)"
                                    else -> "$level ($count)"
                                }
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) GoldAccent else SurfaceDark,
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isSelected) GoldAccent else BorderDark
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable { selectedLevelFilter = level }
                                        .testTag("reading_filter_level_$level")
                                ) {
                                    Text(
                                        text = labelText,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) com.example.ui.theme.OnGoldDark else TextPrimary,
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

                    // Row 4: Topic Filter Chips with Auto-Scroll & Edge Fade
                    if (topics.size > 2) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            LazyRow(
                                state = topicListState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reading_topic_filter_row"),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                contentPadding = PaddingValues(end = 24.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                items(
                                    items = topics,
                                    key = { it },
                                    contentType = { "topic_chip" }
                                ) { topic ->
                                    val isSelected = selectedTopicFilter.equals(topic, ignoreCase = true)
                                    val displayTopic = if (topic == "ALL") "All Topics" else topic.replaceFirstChar { it.uppercase() }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) SurfaceElevatedDark else SurfaceDark,
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = if (isSelected) GoldAccent.copy(alpha = 0.65f) else BorderDark
                                        ),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { selectedTopicFilter = topic }
                                            .testTag("reading_filter_topic_$topic")
                                    ) {
                                        Text(
                                            text = displayTopic,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (isSelected) GoldAccent else TextSecondary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
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
                }
            }

            // 2. List of Reading Texts
            if (filteredTexts.isEmpty()) {
                item(key = "reading_empty_state", contentType = "empty_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = TextMuted
                            )
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = if (texts.isEmpty()) "No reading passages found" else "No passages match your filters",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = if (texts.isEmpty()) {
                                    "Add a new text with AI, paste your own, or re-sync sample passages."
                                } else {
                                    "Try switching the CEFR level pill or clearing your search query."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(16.dp))
                            if (texts.isEmpty()) {
                                Button(
                                    onClick = {
                                        onSyncAssets { res ->
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Synced ${res.getOrDefault(0)} texts from assets")
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GoldAccent,
                                        contentColor = com.example.ui.theme.OnGoldDark
                                    )
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Sync Sample Texts", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        selectedLevelFilter = "ALL"
                                        selectedTopicFilter = "ALL"
                                        searchQuery = ""
                                    },
                                    border = BorderStroke(1.dp, GoldAccent)
                                ) {
                                    Text("Reset Filters", color = GoldAccent)
                                }
                            }
                        }
                    }
                }
            } else {
                items(
                    items = filteredTexts,
                    key = { it.id },
                    contentType = { "reading_text_card" }
                ) { textEntity ->
                    val prog = progressByTextId[textEntity.id]
                    ReadingTextCard(
                        text = textEntity,
                        progress = prog,
                        readabilityReport = storyReadabilityById[textEntity.id],
                        inputSentencesCount = sentenceCountsById[textEntity.id],
                        inputQuestionsCount = questionCountsById[textEntity.id],
                        onClick = { onSelectText(textEntity) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddReadingTextDialog(
            initialTab = initialAddTab,
            onDismiss = { showAddDialog = false },
            grammarLessons = grammarLessons,
            onCreateWithAi = onCreateWithAi,
            onSavePastedText = onSavePastedText,
            onGetSavedTargetWords = onGetSavedTargetWords,
            onPickJsonFiles = {
                filePickerLauncher.launch(
                    arrayOf(
                        "application/json",
                        "text/json",
                        "text/plain",
                        "*/*"
                    )
                )
                showAddDialog = false
            },
            onImportJsonText = onImportJsonText,
            onSyncAssets = {
                onSyncAssets { res ->
                    scope.launch {
                        if (res.isSuccess) {
                            snackbarHostState.showSnackbar("Synced ${res.getOrDefault(0)} texts from assets")
                        } else {
                            snackbarHostState.showSnackbar("Sync failed: ${res.exceptionOrNull()?.message}")
                        }
                    }
                }
                showAddDialog = false
            },
            onSuccessMessage = { msg ->
                scope.launch { snackbarHostState.showSnackbar(msg) }
            }
        )
    }
}

// -------------------------------------------------------------------------------------------------
// READING TEXT CARD
// -------------------------------------------------------------------------------------------------

@Composable
fun ReadingTextCard(
    text: ReadingTextEntity,
    progress: ReadingProgressEntity?,
    onClick: () -> Unit,
    readabilityReport: com.example.data.plan.StoryReadabilityReport? = null,
    inputSentencesCount: Int? = null,
    inputQuestionsCount: Int? = null,
    modifier: Modifier = Modifier
) {
    val sentencesCount = inputSentencesCount ?: remember(text.sentencesJson) {
        READING_SENTENCE_COUNT_CACHE.computeIfAbsent(text.sentencesJson) {
            try {
                readingJson.decodeFromString<List<ReadingSentenceItem>>(it).size
            } catch (_: Exception) { 0 }
        }
    }
    val questionsCount = inputQuestionsCount ?: remember(text.questionsJson) {
        READING_QUESTION_COUNT_CACHE.computeIfAbsent(text.questionsJson) {
            try {
                readingJson.decodeFromString<List<ReadingQuestionItem>>(it).size
            } catch (_: Exception) { 0 }
        }
    }

    val sourceBadgeText = when (text.source.lowercase()) {
        "ai" -> "AI Generated"
        "user" -> "My Text"
        "file" -> "Imported"
        else -> null
    }

    val isDone = progress?.isCompleted == true

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("reading_card_${text.id}"),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceDark
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isDone) EmeraldAccent.copy(alpha = 0.45f) else BorderDark
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // Badges row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(end = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = GoldAccent.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.45f)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = text.level.uppercase(),
                            color = GoldAccent,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    if (text.topic.isNotBlank()) {
                        Surface(
                            modifier = Modifier.weight(1f, fill = false),
                            color = SurfaceElevatedDark,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = text.topic.replaceFirstChar { it.uppercase() },
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    if (sourceBadgeText != null) {
                        Surface(
                            color = SurfaceElevatedDark,
                            border = BorderStroke(1.dp, BorderDark),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = sourceBadgeText,
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (readabilityReport != null) {
                        Surface(
                            color = EmeraldAccentBg,
                            border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.45f)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.testTag("reading_readability_badge_${text.id}")
                        ) {
                            Text(
                                text = "${readabilityReport.readabilityPercent}% Readable • ${readabilityReport.focusWords.size} Focus",
                                color = EmeraldAccent,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }

                    if (isDone) {
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
                                Text(
                                    text = "${progress.bestScore ?: 0}/$questionsCount",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldAccent,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Title English
            Text(
                text = text.titleEn,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            // Title Arabic (preserved RTL alignment)
            if (text.titleAr.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                BidiText(
                    text = text.titleAr,
                    style = ArabicTypography.bodyMedium,
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Right,
                    fillMaxWidth = true
                )
            }

            Spacer(Modifier.height(12.dp))

            // Subtle divider above footer
            HorizontalDivider(
                thickness = 1.dp,
                color = BorderSubtle
            )

            Spacer(Modifier.height(10.dp))

            // Footer info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$sentencesCount sentences • $questionsCount quiz questions",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Text(
                    text = "Read & Practice →",
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldAccent,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 2. READING DETAIL SCREEN (The Text Screen)
// -------------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingDetailScreen(
    text: ReadingTextEntity,
    progress: ReadingProgressEntity?,
    chatHistory: List<AiHistoryEntity>,
    openQuestionHistory: List<AiHistoryEntity>,
    grammarLessons: List<GrammarLessonEntity> = emptyList(),
    onBack: () -> Unit,
    onSaveProgress: (completed: Int, total: Int, score: Int, isDone: Boolean, elapsedMs: Long?) -> Unit,
    onOpenGrammarLesson: (GrammarLessonEntity) -> Unit = {},
    onSaveCorrectionSentence: ((textId: String, lessonId: String, lessonTitle: String, original: String, corrected: String, expAr: String, alt: String, status: String) -> Unit)? = null,
    onCheckCorrectionWithAi: (suspend (CorrectionNotebookEntity) -> Result<Unit>)? = null,
    onNavigateToNotebook: () -> Unit = {},
    onLookupWordLocally: suspend (String) -> List<EntryEntity>,
    onSaveWordFavorite: suspend (deckId: String, word: String, isFavorite: Boolean) -> Unit,
    onGetWordFavoriteStatus: suspend (deckId: String, word: String) -> Boolean,
    onExplainWordInContext: suspend (sentenceIndex: Int, sentenceEn: String, word: String) -> Result<String>,
    onTranslateAndMakeQuestions: suspend () -> Result<Unit>,
    onSimplifyText: suspend () -> Result<String>,
    onAskAi: suspend (String) -> Result<String>,
    onFeedbackOnOpenQuestion: suspend (prompt: String, answer: String) -> Result<String>,
    onDeleteText: () -> Unit,
    onSpeakSentence: (String) -> Unit,
    onSpeakSentenceSlow: (String) -> Unit = { onSpeakSentence(it) },
    onStopAudio: () -> Unit,
    ttsSpeechRate: Float,
    onScheduleWordForToday: suspend (String) -> Boolean = { false },
    readabilityReport: com.example.data.plan.StoryReadabilityReport? = null,
    wordStageMap: Map<String, com.example.data.plan.WordStage> = emptyMap(),
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val quizStartTimeMillis = remember(text.id) { System.currentTimeMillis() }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val sentences = remember(text.sentencesJson) {
        try {
            readingJson.decodeFromString<List<ReadingSentenceItem>>(text.sentencesJson)
        } catch (_: Exception) { emptyList() }
    }

    val questions = remember(text.questionsJson) {
        try {
            readingJson.decodeFromString<List<ReadingQuestionItem>>(text.questionsJson)
        } catch (_: Exception) { emptyList() }
    }

    val targetWords = remember(text.targetWordsJson) {
        try {
            readingJson.decodeFromString<List<String>>(text.targetWordsJson)
                .map { it.lowercase().trim() }
                .toSet()
        } catch (_: Exception) { emptySet() }
    }

    val grammarLessonsById = remember(grammarLessons) {
        grammarLessons.associateBy { it.id }
    }

    // Per-sentence translation visibility map
    val sentenceTranslationVisibility = remember { mutableStateMapOf<Int, Boolean>() }
    var areAllTranslationsVisible by remember { mutableStateOf(false) }

    // Audio reading states
    var isReadingAudioActive by remember { mutableStateOf(false) }
    var currentReadingSentenceIndex by remember { mutableIntStateOf(-1) }
    var readingAudioJob by remember { mutableStateOf<Job?>(null) }

    // Word Lookup bottom sheet
    var lookupWordTarget by remember { mutableStateOf<Pair<Int, String>?>(null) } // (sentenceIndex, word)

    // Grammar Highlight & Bottom Sheet states
    var activeGrammarSentence by remember { mutableStateOf<Pair<ReadingSentenceItem, GrammarLessonEntity?>?>(null) }
    var activePracticeLesson by remember { mutableStateOf<GrammarLessonEntity?>(null) }
    var grammarHintsEnabled by remember { mutableStateOf(true) }
    var selectedSentenceIndex by remember { mutableStateOf<Int?>(null) }

    // AI Chat bottom sheet
    var showChatSheet by remember { mutableStateOf(false) }

    // Simplifying & Generating states
    var isSimplifying by remember { mutableStateOf(false) }
    var isGeneratingQuestionsForPasted by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Questions / Quiz state
    val userAnswers = remember { mutableStateMapOf<Int, Int>() } // questionIndex -> selectedOptionIndex
    var openQuestionAnswer by remember { mutableStateOf("") }
    var isSubmittingOpenQuestion by remember { mutableStateOf(false) }
    var openQuestionFeedback by remember { mutableStateOf<String?>(null) }

    fun toggleAllTranslations() {
        val next = !areAllTranslationsVisible
        areAllTranslationsVisible = next
        sentences.indices.forEach { idx ->
            sentenceTranslationVisibility[idx] = next
        }
    }

    fun startFullTextAudio() {
        if (sentences.isEmpty()) return
        onStopAudio()
        isReadingAudioActive = true
        currentReadingSentenceIndex = 0

        readingAudioJob?.cancel()
        readingAudioJob = scope.launch {
            for (idx in sentences.indices) {
                if (!isReadingAudioActive) break
                currentReadingSentenceIndex = idx
                val sentence = sentences[idx].en
                onSpeakSentence(sentence)
                // Estimate sentence reading duration based on word count & rate
                val wordCount = sentence.split(READING_WHITESPACE_REGEX).size
                val estDurationMs = ((wordCount * 450L) / ttsSpeechRate.coerceAtLeast(0.5f)).toLong() + 900L
                delay(estDurationMs)
            }
            isReadingAudioActive = false
            currentReadingSentenceIndex = -1
        }
    }

    fun stopFullTextAudio() {
        readingAudioJob?.cancel()
        readingAudioJob = null
        isReadingAudioActive = false
        currentReadingSentenceIndex = -1
        onStopAudio()
    }

    Scaffold(
        modifier = modifier.testTag("reading_detail_screen"),
        containerColor = BackgroundDark,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Info & Controls Card
            item(key = "detail_header", contentType = "detail_header") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = SurfaceDark
                    ),
                    border = BorderStroke(1.dp, BorderDark),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Stats Row: ✦ 8 Gold Words (left) + 86% Readable • 6 Focus (right)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left: Sparkle + Gold Words count
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${targetWords.size} Gold Words",
                                    color = GoldAccent,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Right: Readability Pill (Single Line) & Optional More Options Menu
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val readablePercent = readabilityReport?.readabilityPercent ?: 86
                                val focusWords = readabilityReport?.focusWords?.size ?: targetWords.size
                                Surface(
                                    color = EmeraldAccent.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.35f)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("reading_detail_readability_badge")
                                ) {
                                    Text(
                                        text = "$readablePercent% Readable • $focusWords Focus",
                                        color = EmeraldAccent,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }

                                var showMoreMenu by remember { mutableStateOf(false) }
                                Box {
                                    IconButton(
                                        onClick = { showMoreMenu = true },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "Options",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showMoreMenu,
                                        onDismissRequest = { showMoreMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Correction Notebook") },
                                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = GoldAccent) },
                                            onClick = {
                                                showMoreMenu = false
                                                onNavigateToNotebook()
                                            },
                                            modifier = Modifier.testTag("reading_detail_open_notebook_button")
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Ask AI about text") },
                                            leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = GoldAccent) },
                                            onClick = {
                                                showMoreMenu = false
                                                showChatSheet = true
                                            },
                                            modifier = Modifier.testTag("reading_ask_ai_chat_button")
                                        )
                                        if (text.source == "user" || text.source == "ai") {
                                            DropdownMenuItem(
                                                text = { Text("Delete Text", color = Color(0xFFF87171)) },
                                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFF87171)) },
                                                onClick = {
                                                    showMoreMenu = false
                                                    showDeleteConfirmDialog = true
                                                },
                                                modifier = Modifier.testTag("reading_delete_text_button")
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Subtle Horizontal Divider
                        HorizontalDivider(
                            color = BorderDark,
                            thickness = 1.dp
                        )

                        Spacer(Modifier.height(14.dp))

                        // 4-Column Action Grid (Equal width, 12sp, 15dp icon, singleLine=true, never truncates!)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Listen
                            val isListenActive = isReadingAudioActive
                            Surface(
                                onClick = {
                                    if (isReadingAudioActive) stopFullTextAudio()
                                    else startFullTextAudio()
                                },
                                shape = RoundedCornerShape(20.dp),
                                color = if (isListenActive) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                                        else SurfaceElevatedDark,
                                border = BorderStroke(
                                    1.dp,
                                    if (isListenActive) MaterialTheme.colorScheme.error else BorderDark
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("reading_listen_audio_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (isListenActive) Icons.Default.Stop else Icons.Default.Headphones,
                                        contentDescription = null,
                                        tint = if (isListenActive) MaterialTheme.colorScheme.error else TextSecondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = if (isListenActive) "Stop" else "Listen",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isListenActive) MaterialTheme.colorScheme.error else TextSecondary,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Clip
                                    )
                                }
                            }

                            // 2. Arabic Toggle
                            val isArabicActive = areAllTranslationsVisible
                            Surface(
                                onClick = { toggleAllTranslations() },
                                shape = RoundedCornerShape(20.dp),
                                color = if (isArabicActive) GoldAccent.copy(alpha = 0.14f) else SurfaceElevatedDark,
                                border = BorderStroke(
                                    1.dp,
                                    if (isArabicActive) GoldAccent else BorderDark
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("reading_toggle_all_translations_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Translate,
                                        contentDescription = null,
                                        tint = if (isArabicActive) GoldAccent else TextSecondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Arabic",
                                        fontSize = 12.sp,
                                        fontWeight = if (isArabicActive) FontWeight.SemiBold else FontWeight.Medium,
                                        color = if (isArabicActive) GoldAccent else TextSecondary,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Clip
                                    )
                                }
                            }

                            // 3. Hints Toggle
                            val isHintsActive = grammarHintsEnabled
                            Surface(
                                onClick = { grammarHintsEnabled = !grammarHintsEnabled },
                                shape = RoundedCornerShape(20.dp),
                                color = if (isHintsActive) GoldAccent.copy(alpha = 0.14f) else SurfaceElevatedDark,
                                border = BorderStroke(
                                    1.dp,
                                    if (isHintsActive) GoldAccent else BorderDark
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("reading_toggle_grammar_hints_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.School,
                                        contentDescription = null,
                                        tint = if (isHintsActive) GoldAccent else TextSecondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Hints",
                                        fontSize = 12.sp,
                                        fontWeight = if (isHintsActive) FontWeight.SemiBold else FontWeight.Medium,
                                        color = if (isHintsActive) GoldAccent else TextSecondary,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Clip
                                    )
                                }
                            }

                            // 4. Simplify (Guaranteed NO truncation)
                            Surface(
                                onClick = {
                                    if (!isSimplifying) {
                                        isSimplifying = true
                                        scope.launch {
                                            val res = onSimplifyText()
                                            isSimplifying = false
                                            if (res.isSuccess) {
                                                snackbarHostState.showSnackbar("Created simplified version at lower CEFR level!")
                                            } else {
                                                snackbarHostState.showSnackbar("Simplify failed: ${res.exceptionOrNull()?.message}")
                                            }
                                        }
                                    }
                                },
                                enabled = !isSimplifying,
                                shape = RoundedCornerShape(20.dp),
                                color = SurfaceElevatedDark,
                                border = BorderStroke(1.dp, BorderDark),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("reading_simplify_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (isSimplifying) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(12.dp),
                                            strokeWidth = 2.dp,
                                            color = GoldAccent
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = TextSecondary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Simplify",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Clip
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Pasted Text: Generate questions banner if needed
            if (text.source == "user" && questions.isEmpty()) {
                item(key = "pasted_banner", contentType = "pasted_banner") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Your Pasted Text (My Text)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Sentence lookup is ready offline. Tap below to have AI translate every sentence and generate 5 comprehension questions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    isGeneratingQuestionsForPasted = true
                                    scope.launch {
                                        val res = onTranslateAndMakeQuestions()
                                        isGeneratingQuestionsForPasted = false
                                        if (res.isSuccess) {
                                            snackbarHostState.showSnackbar("Translated & generated 5 quiz questions!")
                                        } else {
                                            snackbarHostState.showSnackbar("Failed: ${res.exceptionOrNull()?.message}")
                                        }
                                    }
                                },
                                enabled = !isGeneratingQuestionsForPasted,
                                modifier = Modifier.testTag("reading_translate_make_questions_button")
                            ) {
                                if (isGeneratingQuestionsForPasted) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Translating & Generating...")
                                } else {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Translate & Make 5 Questions")
                                }
                            }
                        }
                    }
                }
            }

            // PASSAGE BODY: Sentences with interactive word chips & translation toggle
            item(key = "passage_body", contentType = "passage_body") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = SurfaceDark
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, BorderDark)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        sentences.forEachIndexed { sIndex, sentenceItem ->
                            if (sIndex > 0) {
                                HorizontalDivider(
                                    color = BorderDark,
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            val isHighlighted = isReadingAudioActive && currentReadingSentenceIndex == sIndex
                            val isArabicVisible = sentenceTranslationVisibility[sIndex] ?: areAllTranslationsVisible
                            val isSentenceSelected = selectedSentenceIndex == sIndex
                            val linkedGrammarLesson = sentenceItem.grammarId?.let { grammarLessonsById[it] }
                            val grammarTitle = linkedGrammarLesson?.let {
                                if (it.titleEn.isNotBlank()) "${it.topic} • ${it.titleEn}" else it.topic
                            } ?: sentenceItem.grammarId?.takeIf { !it.startsWith("g", ignoreCase = true) } ?: "Grammar Rule"

                            ReadingSentenceBlock(
                                sentenceIndex = sIndex,
                                sentence = sentenceItem,
                                isHighlighted = isHighlighted,
                                isArabicVisible = isArabicVisible,
                                isSelected = isSentenceSelected,
                                showGrammarHints = grammarHintsEnabled || isSentenceSelected,
                                grammarLabel = grammarTitle,
                                linkedGrammarLesson = linkedGrammarLesson,
                                targetWords = targetWords,
                                wordStageMap = wordStageMap,
                                onSelectSentence = {
                                    selectedSentenceIndex = if (selectedSentenceIndex == sIndex) null else sIndex
                                },
                                onToggleArabic = {
                                    sentenceTranslationVisibility[sIndex] = !(sentenceTranslationVisibility[sIndex] ?: areAllTranslationsVisible)
                                },
                                onWordClicked = { word ->
                                    lookupWordTarget = Pair(sIndex, word)
                                },
                                onSpeakSentence = {
                                    onSpeakSentence(sentenceItem.en)
                                },
                                onSpeakSentenceSlow = {
                                    onSpeakSentenceSlow(sentenceItem.en)
                                },
                                onGrammarClicked = {
                                    activeGrammarSentence = Pair(
                                        sentenceItem,
                                        linkedGrammarLesson
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // 5 COMPREHENSION QUESTIONS SECTION
            if (questions.isNotEmpty()) {
                item(key = "questions_header", contentType = "questions_header") {
                    Text(
                        text = "Comprehension Questions (${questions.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    )
                }

                itemsIndexed(
                    items = questions,
                    key = { qIndex, q -> "q_${qIndex}_${q.prompt.hashCode()}" },
                    contentType = { _, _ -> "quiz_question" }
                ) { qIndex, question ->
                    val selectedOption = userAnswers[qIndex]
                    ReadingQuestionCard(
                        index = qIndex,
                        question = question,
                        selectedOptionIndex = selectedOption,
                        onSelectOption = { optIdx ->
                            if (userAnswers[qIndex] == null) {
                                userAnswers[qIndex] = optIdx
                                // Calculate score and save progress
                                val completedCount = userAnswers.size
                                val correctCount = questions.indices.count { idx ->
                                    userAnswers[idx] == questions[idx].answerIndex
                                }
                                val isDone = completedCount >= questions.size
                                val elapsedMs = System.currentTimeMillis() - quizStartTimeMillis
                                onSaveProgress(
                                    completedCount,
                                    questions.size,
                                    correctCount,
                                    isDone,
                                    elapsedMs
                                )
                            }
                        }
                    )
                }

                // QUIZ RESULT BANNER
                if (userAnswers.size == questions.size) {
                    item(key = "quiz_result_banner", contentType = "quiz_result_banner") {
                        val score = questions.indices.count { idx ->
                            userAnswers[idx] == questions[idx].answerIndex
                        }
                        QuizCompletionCard(
                            score = score,
                            total = questions.size,
                            onRetake = {
                                userAnswers.clear()
                            }
                        )
                    }

                    // YOUR TURN SECTION (Grammar application)
                    item(key = "your_turn_grammar", contentType = "your_turn_grammar") {
                        YourTurnGrammarCard(
                            text = text,
                            grammarLessons = grammarLessons,
                            onSaveSentenceOffline = { orig, lessonId, lessonTitle ->
                                onSaveCorrectionSentence?.invoke(text.id, lessonId, lessonTitle, orig, "", "", "", "not_checked")
                            },
                            onCheckSentenceWithAi = onCheckCorrectionWithAi
                        )
                    }
                }
            }

            // OPEN QUESTION AT THE END (e.g. Summarize the text)
            item(key = "open_question_card", contentType = "open_question_card") {
                OpenQuestionCard(
                    text = text,
                    answerText = openQuestionAnswer,
                    onAnswerChange = { openQuestionAnswer = it },
                    isSubmitting = isSubmittingOpenQuestion,
                    feedback = openQuestionFeedback,
                    history = openQuestionHistory,
                    onSubmitAnswer = { prompt, ans ->
                        isSubmittingOpenQuestion = true
                        scope.launch {
                            val res = onFeedbackOnOpenQuestion(prompt, ans)
                            isSubmittingOpenQuestion = false
                            if (res.isSuccess) {
                                openQuestionFeedback = res.getOrNull()
                            } else {
                                snackbarHostState.showSnackbar("AI feedback error: ${res.exceptionOrNull()?.message}")
                            }
                        }
                    }
                )
            }
        }
    }

    // WORD LOOKUP BOTTOM SHEET
    lookupWordTarget?.let { (sentenceIndex, word) ->
        val currentSentenceEn = sentences.getOrNull(sentenceIndex)?.en ?: ""
        WordLookupBottomSheet(
            word = word,
            sentenceIndex = sentenceIndex,
            sentenceEn = currentSentenceEn,
            onDismiss = { lookupWordTarget = null },
            onLookupWordLocally = onLookupWordLocally,
            onSaveWordFavorite = onSaveWordFavorite,
            onGetWordFavoriteStatus = onGetWordFavoriteStatus,
            onExplainWordInContext = onExplainWordInContext,
            onSpeakWord = { onSpeakSentence(it) },
            onSpeakWordSlow = { onSpeakSentenceSlow(it) },
            onScheduleWordForToday = onScheduleWordForToday
        )
    }

    // GRAMMAR IN THIS SENTENCE BOTTOM SHEET
    activeGrammarSentence?.let { (sentenceItem, lesson) ->
        GrammarSentenceBottomSheet(
            sentence = sentenceItem,
            lesson = lesson,
            onDismiss = { activeGrammarSentence = null },
            onOpenFullLesson = {
                activeGrammarSentence = null
                if (lesson != null) onOpenGrammarLesson(lesson)
            },
            onPractiseThis = {
                val targetLesson = lesson
                activeGrammarSentence = null
                activePracticeLesson = targetLesson
            }
        )
    }

    // OFFLINE GRAMMAR PRACTICE DIALOG (2-3 exercises from lesson)
    activePracticeLesson?.let { lesson ->
        OfflineGrammarPracticeDialog(
            lesson = lesson,
            onDismiss = { activePracticeLesson = null }
        )
    }

    // AI CHAT ABOUT TEXT BOTTOM SHEET
    if (showChatSheet) {
        ReadingChatBottomSheet(
            text = text,
            chatHistory = chatHistory,
            onDismiss = { showChatSheet = false },
            onSendMessage = onAskAi
        )
    }

    // DELETE CONFIRMATION DIALOG
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Reading Text") },
            text = { Text("Are you sure you want to delete \"${text.titleEn}\"? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteText()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// -------------------------------------------------------------------------------------------------
// READING SENTENCE BLOCK (Words clickable + Sentence audio + Arabic toggle)
// -------------------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReadingSentenceBlock(
    sentenceIndex: Int,
    sentence: ReadingSentenceItem,
    isHighlighted: Boolean,
    isArabicVisible: Boolean,
    isSelected: Boolean = false,
    showGrammarHints: Boolean = false,
    grammarLabel: String = "Grammar Rule",
    linkedGrammarLesson: GrammarLessonEntity? = null,
    targetWords: Set<String>,
    wordStageMap: Map<String, com.example.data.plan.WordStage> = emptyMap(),
    onSelectSentence: () -> Unit = {},
    onToggleArabic: () -> Unit,
    onWordClicked: (String) -> Unit,
    onSpeakSentence: () -> Unit,
    onSpeakSentenceSlow: () -> Unit = {},
    onGrammarClicked: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val hasGrammar = !sentence.grammarId.isNullOrBlank()
    val grammarKeywords = remember(linkedGrammarLesson) { extractGrammarKeywords(linkedGrammarLesson) }

    val targetBg = when {
        isHighlighted -> GoldAccentDim
        isSelected -> SurfaceElevatedDark
        else -> Color.Transparent
    }

    val backgroundColor by animateColorAsState(
        targetValue = targetBg,
        label = "sentence_bg"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .then(
                if (isSelected) {
                    Modifier.border(
                        BorderStroke(1.dp, BorderDark),
                        RoundedCornerShape(12.dp)
                    )
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onSelectSentence)
            .padding(vertical = 4.dp)
            .testTag("reading_sentence_$sentenceIndex")
    ) {
        // 1. Paragraph Toolbar (Placed ABOVE the text - Full Width Text Freedom!)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Pill with Normal Speaker + Flat Outline Turtle Icon
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SurfaceElevatedDark,
                border = BorderStroke(1.dp, BorderDark)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Normal Speaker Button
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onSpeakSentence)
                            .testTag("sentence_speak_${sentenceIndex}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Speak sentence",
                            tint = if (isHighlighted) GoldAccent else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Flat Vector Turtle Icon
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onSpeakSentenceSlow)
                            .testTag("sentence_speak_slow_${sentenceIndex}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = com.example.R.drawable.ic_turtle_flat),
                            contentDescription = "Play slow pronunciation at 0.6x speed",
                            tint = EmeraldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Right: + Rule chip (only when Hints active & rule exists) + Translate button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (hasGrammar && (showGrammarHints || isSelected)) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = SurfaceElevatedDark,
                        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onGrammarClicked?.invoke() }
                            .testTag("sentence_grammar_chip_$sentenceIndex")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = "Grammar Rule",
                                tint = GoldAccent,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Rule",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = GoldAccent,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Translate icon button
                if (sentence.ar.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onToggleArabic)
                            .testTag("sentence_arabic_toggle_$sentenceIndex"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "Toggle translation",
                            tint = if (isArabicVisible) GoldAccent else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // 2. Full-Width Paragraph Text (FlowRow spans entire width!)
        val parsedTokens = remember(sentence.en, targetWords, grammarKeywords, showGrammarHints, isSelected, wordStageMap) {
            sentence.en.split(READING_WHITESPACE_REGEX)
                .filter { it.isNotBlank() }
                .map { token ->
                    val cleanWord = token.replace(READING_TOKEN_CLEAN_REGEX, "")
                    val lowerClean = cleanWord.lowercase()
                    val isTarget = cleanWord.isNotBlank() && targetWords.contains(lowerClean)
                    val isGrammar = hasGrammar && (showGrammarHints || isSelected) &&
                            lowerClean.isNotBlank() && grammarKeywords.contains(lowerClean)
                    val stage = if (!isTarget && !isGrammar && lowerClean.isNotBlank()) {
                        wordStageMap[lowerClean]
                    } else null
                    ReadingTokenItem(token, cleanWord, isTarget, isGrammar, stage)
                }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for ((token, cleanWord, isTarget, isGrammar, stage) in parsedTokens) {
                val lowerClean = cleanWord.lowercase()
                val isArticle = lowerClean in setOf("a", "an", "the")

                when {
                    // Gold Words: Soft translucent gold background, gold text, rounded 6dp, subtle gold border
                    isTarget -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(GoldAccentDim)
                                .border(
                                    BorderStroke(1.dp, GoldAccentBorder),
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable {
                                    if (cleanWord.isNotBlank()) onWordClicked(cleanWord)
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = token,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 18.sp,
                                    lineHeight = 30.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = GoldAccent
                            )
                        }
                    }

                    // Grammar Words (e.g. was): Elevated inner background, dashed gold border, gold text
                    isGrammar -> {
                        val dashGold = GoldAccent
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceElevatedDark)
                                .drawBehind {
                                    val strokeWidth = 1.dp.toPx()
                                    val radius = 6.dp.toPx()
                                    drawRoundRect(
                                        color = dashGold,
                                        style = Stroke(
                                            width = strokeWidth,
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                                        ),
                                        cornerRadius = CornerRadius(radius, radius)
                                    )
                                }
                                .clickable {
                                    onGrammarClicked?.invoke()
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = token,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 18.sp,
                                    lineHeight = 30.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = GoldAccent
                            )
                        }
                    }

                    // Articles (a, an, the) when Hints are enabled: subtle dotted bottom underline
                    isArticle && showGrammarHints -> {
                        val dotColor = TextSecondary
                        Box(
                            modifier = Modifier
                                .drawBehind {
                                    val strokeWidth = 2.dp.toPx()
                                    val y = size.height - 1.dp.toPx()
                                    drawLine(
                                        color = dotColor,
                                        start = Offset(0f, y),
                                        end = Offset(size.width, y),
                                        strokeWidth = strokeWidth,
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f), 0f)
                                    )
                                }
                                .clickable {
                                    if (cleanWord.isNotBlank()) onWordClicked(cleanWord)
                                }
                                .padding(horizontal = 2.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = token,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 18.sp,
                                    lineHeight = 30.sp,
                                    fontWeight = FontWeight.Normal
                                ),
                                color = TextPrimary
                            )
                        }
                    }

                    // Word Stages (LEARNING, FAMILIAR, etc.)
                    stage != null -> {
                        val stageColor = when (stage) {
                            com.example.data.plan.WordStage.LEARNING -> SkyAccent
                            com.example.data.plan.WordStage.FAMILIAR -> EmeraldAccent
                            com.example.data.plan.WordStage.SEEN -> TextSecondary
                            else -> TextSecondary
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(stageColor.copy(alpha = 0.12f))
                                .border(
                                    BorderStroke(1.dp, stageColor.copy(alpha = 0.35f)),
                                    RoundedCornerShape(4.dp)
                                )
                                .clickable {
                                    if (cleanWord.isNotBlank()) onWordClicked(cleanWord)
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = token,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 18.sp,
                                    lineHeight = 30.sp,
                                    fontWeight = FontWeight.Normal
                                ),
                                color = stageColor
                            )
                        }
                    }

                    // Regular words
                    else -> {
                        Box(
                            modifier = Modifier
                                .clickable {
                                    if (cleanWord.isNotBlank()) onWordClicked(cleanWord)
                                }
                                .padding(horizontal = 1.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = token,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 18.sp,
                                    lineHeight = 30.sp,
                                    fontWeight = FontWeight.Normal
                                ),
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // 3. Arabic Translation Line (Below text)
        AnimatedVisibility(
            visible = isArabicVisible && sentence.ar.isNotBlank(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                color = SurfaceElevatedDark,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                BidiText(
                    text = sentence.ar,
                    style = ArabicTypography.bodyMedium.copy(
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    ),
                    color = TextSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// GRAMMAR IN THIS SENTENCE BOTTOM SHEET
// -------------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrammarSentenceBottomSheet(
    sentence: ReadingSentenceItem,
    lesson: GrammarLessonEntity?,
    onDismiss: () -> Unit,
    onOpenFullLesson: () -> Unit,
    onPractiseThis: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("grammar_sentence_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFFD54F).copy(alpha = 0.25f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.School, contentDescription = null, tint = Color(0xFFB78103))
                        }
                    }
                    Column {
                        Text(
                            text = "Grammar in this sentence",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "القاعدة النحوية في هذه الجملة",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            HorizontalDivider()

            // Lesson Title
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = lesson?.titleEn ?: "Grammar Rule (${sentence.grammarId ?: ""})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (lesson != null) {
                            Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFFFD54F).copy(alpha = 0.2f)) {
                                Text(
                                    text = lesson.level,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB78103),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    if (lesson?.titleAr?.isNotBlank() == true) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = lesson.titleAr,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Short part of explanation_ar (first 3 lines)
            val shortExplanation = remember(lesson?.explanationAr) {
                lesson?.explanationAr?.lines()?.filter { it.isNotBlank() }?.take(3)?.joinToString("\n") ?: ""
            }
            if (shortExplanation.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "ملخص القاعدة (Explanation):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        BidiText(
                            text = shortExplanation,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            // Sentence with translation and highlighted grammar keyword
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFFFD54F).copy(alpha = 0.6f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Sentence in text • الجملة في السياق:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB78103)
                    )
                    Spacer(Modifier.height(6.dp))
                    val sheetKeywords = remember(lesson) { extractGrammarKeywords(lesson) }
                    val highlightColor = GoldAccent
                    val highlightBg = GoldAccentDim
                    val annotatedSentence = remember(sentence.en, sheetKeywords, highlightColor, highlightBg) {
                        buildAnnotatedString {
                            val tokens = sentence.en.split(READING_WHITESPACE_REGEX)
                            tokens.forEachIndexed { idx, token ->
                                val clean = token.replace(READING_TOKEN_CLEAN_REGEX, "").lowercase()
                                if (clean in sheetKeywords) {
                                    withStyle(
                                        SpanStyle(
                                            color = highlightColor,
                                            fontWeight = FontWeight.Bold,
                                            background = highlightBg
                                        )
                                    ) {
                                        append(token)
                                    }
                                } else {
                                    append(token)
                                }
                                if (idx < tokens.lastIndex) append(" ")
                            }
                        }
                    }
                    Text(
                        text = annotatedSentence,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            textDirection = TextDirection.Ltr,
                            textAlign = TextAlign.Left
                        ),
                        fontWeight = FontWeight.Medium
                    )
                    if (sentence.ar.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        BidiText(
                            text = sentence.ar,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Direct practice button & Open full lesson
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onPractiseThis,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldAccent,
                        contentColor = com.example.ui.theme.OnGoldDark
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("practise_this_lesson_button")
                ) {
                    Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Practice Rule ➔",
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onOpenFullLesson,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("open_full_lesson_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Full Lesson")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// -------------------------------------------------------------------------------------------------
// OFFLINE GRAMMAR PRACTICE DIALOG (2-3 exercises from lesson)
// -------------------------------------------------------------------------------------------------

@Composable
fun OfflineGrammarPracticeDialog(
    lesson: GrammarLessonEntity,
    onDismiss: () -> Unit
) {
    val jsonHelper = remember { Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true } }
    val exercises = remember(lesson.exercisesJson) {
        try {
            jsonHelper.decodeFromString<List<GrammarExerciseItem>>(lesson.exercisesJson).take(3)
        } catch (_: Exception) {
            emptyList()
        }
    }

    val userAnswers = remember { mutableStateMapOf<Int, String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Quiz, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Practise: ${lesson.titleEn}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("تمارين تطبيقية للقاعدة (بدون إنترنت)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            if (exercises.isEmpty()) {
                Text("No exercises available for this lesson.")
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    exercises.forEachIndexed { index, exercise ->
                        val selected = userAnswers[index]
                        val isAnswered = selected != null
                        val isCorrect = selected != null && isGrammarAnswerCorrect(selected, exercise.answer)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Exercise ${index + 1}: ${exercise.prompt}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(8.dp))

                                if (exercise.options.isNotEmpty()) {
                                    exercise.options.forEach { option ->
                                        val isOptionSelected = selected == option
                                        val isOptionCorrect = isGrammarAnswerCorrect(option, exercise.answer)

                                        val containerColor = when {
                                            !isAnswered -> MaterialTheme.colorScheme.surface
                                            isOptionCorrect -> Color(0xFF2E7D32).copy(alpha = 0.15f)
                                            isOptionSelected && !isOptionCorrect -> Color(0xFFC62828).copy(alpha = 0.15f)
                                            else -> MaterialTheme.colorScheme.surface
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = containerColor,
                                            border = BorderStroke(
                                                1.dp,
                                                if (isAnswered && isOptionCorrect) Color(0xFF2E7D32)
                                                else if (isAnswered && isOptionSelected && !isOptionCorrect) Color(0xFFC62828)
                                                else MaterialTheme.colorScheme.outlineVariant
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp)
                                                .clickable(enabled = !isAnswered) {
                                                    userAnswers[index] = option
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = option,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (isOptionSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isAnswered && isOptionCorrect) Color(0xFF2E7D32)
                                                    else if (isAnswered && isOptionSelected && !isOptionCorrect) Color(0xFFC62828)
                                                    else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    var textInput by remember(index) { mutableStateOf(selected ?: "") }
                                    if (!isAnswered) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = textInput,
                                                onValueChange = { textInput = it },
                                                placeholder = { Text("Type answer...", fontSize = 13.sp) },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f).testTag("dialog_fill_input_$index"),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            Button(
                                                onClick = {
                                                    if (textInput.isNotBlank()) {
                                                        userAnswers[index] = textInput.trim()
                                                    }
                                                },
                                                enabled = textInput.isNotBlank(),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = GoldAccent,
                                                    contentColor = com.example.ui.theme.OnGoldDark
                                                ),
                                                modifier = Modifier.testTag("dialog_fill_submit_$index")
                                            ) {
                                                Text("Check", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isCorrect) Color(0xFF2E7D32).copy(alpha = 0.15f) else Color(0xFFC62828).copy(alpha = 0.15f),
                                            border = BorderStroke(1.dp, if (isCorrect) Color(0xFF2E7D32) else Color(0xFFC62828)),
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Your answer: $selected",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isCorrect) Color(0xFF2E7D32) else Color(0xFFC62828)
                                                )
                                                Text(
                                                    text = if (isCorrect) "✓" else "✗",
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isCorrect) Color(0xFF2E7D32) else Color(0xFFC62828)
                                                )
                                            }
                                        }
                                    }
                                }

                                if (isAnswered) {
                                    Spacer(Modifier.height(6.dp))
                                    Surface(
                                        color = if (isCorrect) Color(0xFF2E7D32).copy(alpha = 0.1f) else Color(0xFFC62828).copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(
                                                text = if (isCorrect) "✓ Correct! ممتاز" else "✗ Incorrect! الإجابة الصحيحة: ${exercise.answer}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCorrect) Color(0xFF2E7D32) else Color(0xFFC62828)
                                            )
                                            if (exercise.explanationAr.isNotBlank()) {
                                                Spacer(Modifier.height(2.dp))
                                                BidiText(
                                                    text = exercise.explanationAr,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

// -------------------------------------------------------------------------------------------------
// YOUR TURN GRAMMAR CARD (Write sentence using structure about a new situation)
// -------------------------------------------------------------------------------------------------

@Composable
fun YourTurnGrammarCard(
    text: ReadingTextEntity,
    grammarLessons: List<GrammarLessonEntity>,
    onSaveSentenceOffline: (original: String, lessonId: String, lessonTitle: String) -> Unit,
    onCheckSentenceWithAi: (suspend (CorrectionNotebookEntity) -> Result<Unit>)?,
    modifier: Modifier = Modifier
) {
    val jsonHelper = remember { Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true } }
    val grammarIds = remember(text.grammarIdsJson) {
        try {
            jsonHelper.decodeFromString<List<String>>(text.grammarIdsJson)
        } catch (_: Exception) {
            emptyList()
        }
    }
    val primaryLessonId = grammarIds.firstOrNull() ?: "g054"
    val targetLesson = remember(primaryLessonId, grammarLessons) {
        grammarLessons.firstOrNull { it.id == primaryLessonId }
    }
    val lessonTitle = targetLesson?.titleEn ?: "Third Conditional"
    val lessonTitleAr = targetLesson?.titleAr ?: "الشرط الثالث"

    var userSentence by remember { mutableStateOf("") }
    var savedOffline by remember { mutableStateOf(false) }
    var isCheckingAi by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val situationPrompt = remember(text.id, primaryLessonId) {
        when (text.id) {
            "r054" -> "You planned to attend an important interview, but you woke up late and missed the early train."
            "r091" -> "Describe a normal Friday morning in your routine using the present simple."
            "r092" -> "Describe your home or neighborhood using there is or there are."
            "r093" -> "Describe what you usually cook and what you are making today."
            "r094" -> "Describe what happened on a trip when you missed a train."
            "r095" -> "Describe how often you play football and what happened in your last match."
            "r096" -> "Describe what happened when a device you use stopped working."
            "r097" -> "Describe a repetitive work task that software has automated."
            "r098" -> "Explain what can happen when patients misuse antibiotics."
            "r099" -> "Explain how indexes help a database and what happens when there are too many."
            "r100" -> "Explain how wearable devices monitor and protect patient data."
            else -> when (primaryLessonId) {
                "g006" -> "Describe what is in a room or neighborhood using there is or there are."
                "g007" -> "Describe a normal part of your day using the present simple."
                "g008" -> "Describe what someone is doing right now using the present continuous."
                "g014" -> "Describe where you were and how you felt yesterday."
                "g015" -> "Write what happened on your last trip using the past simple."
                "g018" -> "Compare two phones, homes, or places using a comparative adjective."
                "g023" -> "Describe how often you play a sport or do another activity."
                "g030" -> "Describe how long you have used a tool or followed a routine using since or for."
                "g036" -> "Explain what happens when a patient follows or stops a treatment."
                "g037" -> "Explain how a team automates or transfers data using the passive voice."
                "g057" -> "Explain how database indexes are created or maintained using the passive voice."
                "g065" -> "Describe one benefit and one drawback, using a linking word such as however or consequently."
                "g079" -> "Explain how patient data is collected and protected using the passive voice."
                "g084" -> "Summarize the topic in a formal sentence using a noun such as integration or assessment."
                else -> "Write one sentence about this reading using the target grammar rule."
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("your_turn_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, Color(0xFFFFD54F))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFFD54F),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF3E2723), modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Your Turn • دورك الآن",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB78103)
                    )
                    Text(
                        text = "طبّق القاعدة في جملة من إنشائك عن موقف جديد",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Target structure box
            Surface(
                color = GoldAccentDim,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, GoldAccentBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Target Structure: $lessonTitle ($lessonTitleAr)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Situation: $situationPrompt",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedTextField(
                value = userSentence,
                onValueChange = {
                    userSentence = it
                    savedOffline = false
                    errorMessage = null
                },
                label = { Text("Write your sentence here") },
                placeholder = { Text("Write one sentence using $lessonTitle.") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("your_turn_sentence_input"),
                minLines = 2
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Offline save button
                OutlinedButton(
                    onClick = {
                        if (userSentence.isNotBlank()) {
                            onSaveSentenceOffline(userSentence.trim(), primaryLessonId, "$lessonTitle ($lessonTitleAr)")
                            savedOffline = true
                        }
                    },
                    enabled = userSentence.isNotBlank(),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("your_turn_save_offline_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (savedOffline) "Saved!" else "Save to Notebook")
                }

                // Check with AI button
                Button(
                    onClick = {
                        if (userSentence.isNotBlank() && onCheckSentenceWithAi != null) {
                            val dummyEntry = CorrectionNotebookEntity(
                                id = 0L,
                                textId = text.id,
                                grammarLessonId = primaryLessonId,
                                grammarLessonTitle = "$lessonTitle ($lessonTitleAr)",
                                originalSentence = userSentence.trim(),
                                status = "not_checked"
                            )
                            isCheckingAi = true
                            errorMessage = null
                            CoroutineScope(Dispatchers.Main).launch {
                                val res = onCheckSentenceWithAi(dummyEntry)
                                isCheckingAi = false
                                if (res.isSuccess) {
                                    onSaveSentenceOffline(userSentence.trim(), primaryLessonId, "$lessonTitle ($lessonTitleAr)")
                                    savedOffline = true
                                } else {
                                    errorMessage = res.exceptionOrNull()?.message ?: "Check failed"
                                }
                            }
                        }
                    },
                    enabled = userSentence.isNotBlank() && !isCheckingAi,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F), contentColor = Color(0xFF3E2723)),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("your_turn_check_ai_button")
                ) {
                    if (isCheckingAi) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF3E2723), strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                        Text("Checking...")
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Check my sentence", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (savedOffline) {
                Surface(
                    color = Color(0xFF2E7D32).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Saved to Correction Notebook with status: 'not checked'. You can review it anytime offline!",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Text(
                    text = "AI Check note: $errorMessage (Sentence saved offline)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 3. WORD LOOKUP BOTTOM SHEET (Local DB lookup + Ask AI in context)
// -------------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordLookupBottomSheet(
    word: String,
    sentenceIndex: Int,
    sentenceEn: String,
    onDismiss: () -> Unit,
    onLookupWordLocally: suspend (String) -> List<EntryEntity>,
    onSaveWordFavorite: suspend (deckId: String, word: String, isFavorite: Boolean) -> Unit,
    onGetWordFavoriteStatus: suspend (deckId: String, word: String) -> Boolean,
    onExplainWordInContext: suspend (sentenceIndex: Int, sentenceEn: String, word: String) -> Result<String>,
    onSpeakWord: (String) -> Unit,
    onSpeakWordSlow: (String) -> Unit = { onSpeakWord(it) },
    onScheduleWordForToday: suspend (String) -> Boolean = { false }
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var localEntries by remember { mutableStateOf<List<EntryEntity>?>(null) }
    var isLoadingLocal by remember { mutableStateOf(true) }
    var isFavorite by remember { mutableStateOf(false) }
    var isScheduledToday by remember { mutableStateOf(false) }

    // AI Context Explanation
    var aiExplanation by remember { mutableStateOf<String?>(null) }
    var isLoadingAi by remember { mutableStateOf(false) }
    var aiError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(word) {
        isLoadingLocal = true
        val matches = onLookupWordLocally(word)
        localEntries = matches
        isLoadingLocal = false

        val primaryDeck = matches.firstOrNull()?.deck ?: "oxford5000"
        isFavorite = onGetWordFavoriteStatus(primaryDeck, word)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag("reading_word_lookup_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header: Word + Audio + Favorite + Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = word,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = { onSpeakWord(word) },
                        modifier = Modifier.testTag("word_sheet_speak_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Speak",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    SlowAudioTurtleButton(
                        onClick = { onSpeakWordSlow(word) },
                        showSpeedLabel = true,
                        testTag = "word_sheet_speak_slow_button"
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            val next = !isFavorite
                            isFavorite = next
                            scope.launch {
                                val deckId = localEntries?.firstOrNull()?.deck ?: "oxford5000"
                                onSaveWordFavorite(deckId, word, next)
                            }
                        },
                        modifier = Modifier.testTag("word_sheet_favorite_button")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.outline
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }

            // IPA & POS if found
            val primary = localEntries?.firstOrNull()
            if (primary != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    if (primary.ipa.isNotBlank()) {
                        Text(
                            text = "/${primary.ipa}/",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    if (primary.pos.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = primary.pos,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (primary.level.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = primary.level.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // One-tap Add to Today's Practice button (Reactive Reader Loop)
            OutlinedButton(
                onClick = {
                    if (!isScheduledToday) {
                        scope.launch {
                            val ok = onScheduleWordForToday(word)
                            isScheduledToday = ok || true
                        }
                    }
                },
                enabled = !isScheduledToday,
                border = BorderStroke(
                    1.dp,
                    if (isScheduledToday) EmeraldAccent else GoldAccent
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isScheduledToday) EmeraldAccent else GoldAccent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .testTag("btn_add_lookup_to_practice")
            ) {
                Icon(
                    imageVector = if (isScheduledToday) Icons.Default.CheckCircle else Icons.Default.School,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isScheduledToday) "Added to Today's Practice • تمت الإضافة لمراجعة اليوم" else "Add to Today's Practice • أضف لمراجعة اليوم",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // LOCAL DATABASE RESULTS
            if (isLoadingLocal) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                }
            } else if (!localEntries.isNullOrEmpty()) {
                val entries = localEntries!!
                Text(
                    text = "Local Word Books Meaning",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                // Group entries by deck
                entries.forEach { entry ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                BidiText(
                                    text = entry.arabic.ifBlank { "—" },
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = entry.deck,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            if (entry.definitionEn.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = entry.definitionEn,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            if (entry.definitionAr.isNotBlank()) {
                                Spacer(Modifier.height(3.dp))
                                BidiText(
                                    text = entry.definitionAr,
                                    style = ArabicTypography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fillMaxWidth = true,
                                    textAlign = TextAlign.Right
                                )
                            }

                            if (entry.examplesEn.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "• ${entry.examplesEn}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        textDirection = TextDirection.Ltr,
                                        textAlign = TextAlign.Left
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                // NOT FOUND IN LOCAL BOOKS CARD
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Not in my word books",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "هذه الكلمة غير موجودة في كتب الكلمات المحلية.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ASK AI (Meaning in this sentence context)
            Text(
                text = "Meaning in this Sentence (Context)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "\"$sentenceEn\"",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            if (aiExplanation != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        BidiText(
                            text = aiExplanation!!,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                if (isLoadingAi) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Consulting Gemini AI...", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    Button(
                        onClick = {
                            isLoadingAi = true
                            aiError = null
                            scope.launch {
                                val res = onExplainWordInContext(sentenceIndex, sentenceEn, word)
                                isLoadingAi = false
                                if (res.isSuccess) {
                                    aiExplanation = res.getOrNull()
                                } else {
                                    aiError = res.exceptionOrNull()?.message ?: "AI failed"
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .testTag("word_ask_ai_context_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Ask AI for Meaning in Context")
                    }

                    aiError?.let { err ->
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 4. READING QUESTION CARD (Instant feedback & Arabic explanation)
// -------------------------------------------------------------------------------------------------

@Composable
fun ReadingQuestionCard(
    index: Int,
    question: ReadingQuestionItem,
    selectedOptionIndex: Int?,
    onSelectOption: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isAnswered = selectedOptionIndex != null
    val isCorrect = selectedOptionIndex == question.answerIndex

    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("reading_question_$index"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = SurfaceDark
        ),
        border = BorderStroke(
            1.dp,
            if (isAnswered) {
                if (isCorrect) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
            } else BorderDark
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Prompt
            Row(verticalAlignment = Alignment.Top) {
                Surface(
                    color = GoldAccentDim,
                    border = BorderStroke(1.dp, GoldAccentBorder),
                    shape = CircleShape,
                    modifier = Modifier.size(26.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = question.prompt,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            Spacer(Modifier.height(14.dp))

            // Options
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                question.options.forEachIndexed { optIndex, optionText ->
                    val isSelected = selectedOptionIndex == optIndex
                    val isRightAnswer = optIndex == question.answerIndex

                    val (containerColor, contentColor, borderColor) = when {
                        !isAnswered -> Triple(
                            SurfaceElevatedDark,
                            TextPrimary,
                            BorderDark
                        )
                        isSelected && isRightAnswer -> Triple(
                            Color(0xFF1B5E20).copy(alpha = 0.25f),
                            EmeraldAccent,
                            Color(0xFF2E7D32)
                        )
                        isSelected && !isRightAnswer -> Triple(
                            Color(0xFFB71C1C).copy(alpha = 0.25f),
                            Color(0xFFE57373),
                            Color(0xFFD32F2F)
                        )
                        isRightAnswer -> Triple(
                            Color(0xFF1B5E20).copy(alpha = 0.25f),
                            EmeraldAccent,
                            Color(0xFF2E7D32)
                        )
                        else -> Triple(
                            SurfaceElevatedDark.copy(alpha = 0.5f),
                            TextSecondary.copy(alpha = 0.6f),
                            BorderDark.copy(alpha = 0.4f)
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isAnswered) {
                                onSelectOption(optIndex)
                            }
                            .testTag("q_${index}_opt_${optIndex}"),
                        shape = RoundedCornerShape(12.dp),
                        color = containerColor,
                        border = BorderStroke(1.dp, borderColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${('A'.code + optIndex).toChar()}.",
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected || (isAnswered && isRightAnswer)) contentColor else TextSecondary,
                                modifier = Modifier.width(24.dp)
                            )
                            Text(
                                text = optionText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = contentColor,
                                modifier = Modifier.weight(1f)
                            )

                            if (isAnswered) {
                                if (isRightAnswer) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Correct",
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Wrong",
                                        tint = Color(0xFFD32F2F),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Arabic Explanation Banner
            if (isAnswered && question.explanationAr.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCorrect) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = if (isCorrect) Color(0xFF2E7D32) else Color(0xFFE65100),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        BidiText(
                            text = question.explanationAr,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isCorrect) Color(0xFF1B5E20) else Color(0xFFE65100)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// QUIZ COMPLETION CARD
// -------------------------------------------------------------------------------------------------

@Composable
fun QuizCompletionCard(
    score: Int,
    total: Int,
    onRetake: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("reading_quiz_completion_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Quiz Completed! / اكتمل الاختبار",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Score: $score / $total (${(score * 100) / total}%)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(14.dp))
            OutlinedButton(
                onClick = onRetake,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Retake Quiz / إعادة الاختبار")
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 5. OPEN QUESTION CARD (Summarize in 2 sentences with Gemini Feedback)
// -------------------------------------------------------------------------------------------------

@Composable
fun OpenQuestionCard(
    text: ReadingTextEntity,
    answerText: String,
    onAnswerChange: (String) -> Unit,
    isSubmitting: Boolean,
    feedback: String?,
    history: List<AiHistoryEntity>,
    onSubmitAnswer: (prompt: String, answer: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val prompt = "Summarize the key idea of this passage in 2-3 sentences."

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("reading_open_question_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Reading Response & Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = prompt,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = answerText,
                onValueChange = onAnswerChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reading_open_answer_input"),
                placeholder = { Text("Write your summary in English here...") },
                minLines = 3,
                maxLines = 6
            )

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = {
                    if (answerText.isNotBlank()) {
                        onSubmitAnswer(prompt, answerText.trim())
                    }
                },
                enabled = answerText.isNotBlank() && !isSubmitting,
                modifier = Modifier
                    .align(Alignment.End)
                    .testTag("reading_submit_open_answer_button")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("Getting AI Review...")
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Get AI Feedback")
                }
            }

            // AI Feedback result
            feedback?.let { fb ->
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Teacher Review / تقييم المعلم الذكي:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = fb,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 6. READING CHAT BOTTOM SHEET (Ask AI about this text)
// -------------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingChatBottomSheet(
    text: ReadingTextEntity,
    chatHistory: List<AiHistoryEntity>,
    onDismiss: () -> Unit,
    onSendMessage: suspend (String) -> Result<String>
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var inputQuery by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag("reading_chat_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Ask AI About This Text",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "اسأل عن أي جملة أو معنى أو فكرة في النص",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Chat Messages List - flexes smoothly when keyboard is visible
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(min = 100.dp, max = 320.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (chatHistory.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Ask anything about \"${text.titleEn}\"!\n(e.g., \"What does sentence 2 mean?\" or \"Explain the grammar in paragraph 1\")",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(chatHistory) { msg ->
                        val isUser = msg.role == "user"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isUser) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                BidiText(
                                    text = msg.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Input Row
            val canSend = inputQuery.isNotBlank() && !isSending
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputQuery,
                    onValueChange = { inputQuery = it },
                    placeholder = {
                        Text(
                            text = "Ask a question in Arabic or English...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("reading_chat_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    ),
                    maxLines = 3
                )
                FilledIconButton(
                    onClick = {
                        val q = inputQuery.trim()
                        if (q.isNotBlank() && !isSending) {
                            inputQuery = ""
                            isSending = true
                            scope.launch {
                                onSendMessage(q)
                                isSending = false
                            }
                        }
                    },
                    enabled = canSend,
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("reading_chat_send_button")
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.primary,
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

// -------------------------------------------------------------------------------------------------
// 7. ADD & IMPORT READING TEXTS DIALOG (4 Tabs: AI, Paste, Import JSON, Sync)
// -------------------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddReadingTextDialog(
    initialTab: Int = 0,
    onDismiss: () -> Unit,
    grammarLessons: List<GrammarLessonEntity> = emptyList(),
    onCreateWithAi: suspend (level: String, topic: String, length: String, targetWords: List<String>, targetGrammarLessonId: String?) -> Result<String>,
    onSavePastedText: suspend (title: String, rawText: String, topic: String, level: String) -> Result<ReadingTextEntity>,
    onGetSavedTargetWords: suspend (limit: Int) -> List<String>,
    onPickJsonFiles: () -> Unit,
    onImportJsonText: suspend (String) -> Result<Int>,
    onSyncAssets: () -> Unit,
    onSuccessMessage: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    val tabs = remember { listOf("Create with AI", "Paste Text", "Import JSON", "Sync Assets") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Reading Texts",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 0.dp
                ) {
                    tabs.forEachIndexed { index, tabTitle ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(tabTitle, maxLines = 1) }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                when (selectedTab) {
                    0 -> TabCreateWithAi(
                        onDismiss = onDismiss,
                        grammarLessons = grammarLessons,
                        onCreateWithAi = onCreateWithAi,
                        onGetSavedTargetWords = onGetSavedTargetWords,
                        onSuccessMessage = onSuccessMessage
                    )
                    1 -> TabPasteMyText(
                        onDismiss = onDismiss,
                        onSavePastedText = onSavePastedText,
                        onSuccessMessage = onSuccessMessage
                    )
                    2 -> TabImportJson(
                        onDismiss = onDismiss,
                        onPickJsonFiles = onPickJsonFiles,
                        onImportJsonText = onImportJsonText,
                        onSuccessMessage = onSuccessMessage
                    )
                    3 -> TabSyncAssets(
                        onDismiss = onDismiss,
                        onSyncAssets = onSyncAssets
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TabCreateWithAi(
    onDismiss: () -> Unit,
    grammarLessons: List<GrammarLessonEntity> = emptyList(),
    onCreateWithAi: suspend (level: String, topic: String, length: String, targetWords: List<String>, targetGrammarLessonId: String?) -> Result<String>,
    onGetSavedTargetWords: suspend (limit: Int) -> List<String>,
    onSuccessMessage: (String) -> Unit
) {
    var selectedLevel by remember { mutableStateOf("B1") }
    var topic by remember { mutableStateOf("Travel & Cultures") }
    var length by remember { mutableStateOf("medium") } // short, medium, long
    var useSavedWords by remember { mutableStateOf(true) }
    var customTargetWords by remember { mutableStateOf("") }
    var selectedGrammarLesson by remember { mutableStateOf<GrammarLessonEntity?>(null) }
    var showGrammarDropdown by remember { mutableStateOf(false) }
    var isGenerating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val levels = listOf("A1", "A2", "B1", "B2", "C1", "C2")

    val availableLessons = remember(grammarLessons, selectedLevel) {
        grammarLessons.filter { it.level.equals(selectedLevel, ignoreCase = true) }
    }

    LaunchedEffect(selectedLevel) {
        if (selectedGrammarLesson != null && !selectedGrammarLesson!!.level.equals(selectedLevel, ignoreCase = true)) {
            selectedGrammarLesson = null
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("CEFR Level:", style = MaterialTheme.typography.labelMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(levels) { lvl ->
                FilterChip(
                    selected = selectedLevel == lvl,
                    onClick = { selectedLevel = lvl },
                    label = { Text(lvl) }
                )
            }
        }

        Text("Target Grammar Rule (Optional):", style = MaterialTheme.typography.labelMedium)
        Box(modifier = Modifier.fillMaxWidth()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1C1C21),
                border = BorderStroke(
                    1.dp,
                    if (selectedGrammarLesson != null) Color(0xFFF5C542) else Color(0xFF27272E)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { showGrammarDropdown = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedGrammarLesson?.let { "${it.id.uppercase()} • ${it.titleEn}" }
                                ?: "Auto / General (No specific rule)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selectedGrammarLesson != null) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selectedGrammarLesson != null) Color(0xFFF5C542) else Color(0xFF9CA3AF)
                        )
                        selectedGrammarLesson?.titleAr?.let { arTitle ->
                            Text(
                                text = arTitle,
                                style = ArabicTypography.bodySmall,
                                color = Color(0xFF8E8E99)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select Grammar Rule",
                        tint = if (selectedGrammarLesson != null) Color(0xFFF5C542) else Color(0xFF8E8E99)
                    )
                }
            }

            DropdownMenu(
                expanded = showGrammarDropdown,
                onDismissRequest = { showGrammarDropdown = false },
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                DropdownMenuItem(
                    text = { Text("Auto / General (No specific rule)") },
                    onClick = {
                        selectedGrammarLesson = null
                        showGrammarDropdown = false
                    }
                )
                availableLessons.forEach { lesson ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = "${lesson.id.uppercase()} • ${lesson.titleEn}",
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = lesson.titleAr,
                                    style = ArabicTypography.bodySmall,
                                    color = Color(0xFF9CA3AF)
                                )
                            }
                        },
                        onClick = {
                            selectedGrammarLesson = lesson
                            showGrammarDropdown = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = topic,
            onValueChange = { topic = it },
            label = { Text("Topic (e.g. Travel, Job Interview, Science)") },
            modifier = Modifier.fillMaxWidth()
        )

        Text("Story Length:", style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = length == "short",
                onClick = { length = "short" },
                label = { Text("Short (5-6)") }
            )
            FilterChip(
                selected = length == "medium",
                onClick = { length = "medium" },
                label = { Text("Medium (8-10)") }
            )
            FilterChip(
                selected = length == "long",
                onClick = { length = "long" },
                label = { Text("Long (12-15)") }
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            FilterChip(
                selected = useSavedWords,
                onClick = { useSavedWords = !useSavedWords },
                label = { Text("Incorporate my saved / favorite words") }
            )
        }

        OutlinedTextField(
            value = customTargetWords,
            onValueChange = { customTargetWords = it },
            label = { Text("Optional extra target words (comma-separated)") },
            modifier = Modifier.fillMaxWidth()
        )

        errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Button(
            onClick = {
                isGenerating = true
                errorMessage = null
                scope.launch {
                    val targetList = mutableListOf<String>()
                    if (useSavedWords) {
                        targetList.addAll(onGetSavedTargetWords(10))
                    }
                    if (customTargetWords.isNotBlank()) {
                        targetList.addAll(customTargetWords.split(",").map { it.trim() }.filter { it.isNotBlank() })
                    }

                    val res = onCreateWithAi(selectedLevel, topic, length, targetList.distinct(), selectedGrammarLesson?.id)
                    isGenerating = false
                    if (res.isSuccess) {
                        onSuccessMessage("Successfully generated reading passage with AI!")
                        onDismiss()
                    } else {
                        errorMessage = res.exceptionOrNull()?.message ?: "AI generation failed"
                    }
                }
            },
            enabled = topic.isNotBlank() && !isGenerating,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isGenerating) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Generating Reading Story...")
            } else {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Generate Reading Passage")
            }
        }
    }
}

@Composable
fun TabPasteMyText(
    onDismiss: () -> Unit,
    onSavePastedText: suspend (title: String, rawText: String, topic: String, level: String) -> Result<ReadingTextEntity>,
    onSuccessMessage: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var pastedContent by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf("General") }
    var level by remember { mutableStateOf("B1") }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Paste any English text or article. It will be split into sentences with full local dictionary word lookups.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title (Optional)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = pastedContent,
            onValueChange = { pastedContent = it },
            label = { Text("English Passage Text *") },
            placeholder = { Text("Paste your article, story, or paragraph here...") },
            minLines = 5,
            maxLines = 10,
            modifier = Modifier.fillMaxWidth()
        )

        errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Button(
            onClick = {
                isSaving = true
                errorMessage = null
                scope.launch {
                    val res = onSavePastedText(title, pastedContent, topic, level)
                    isSaving = false
                    if (res.isSuccess) {
                        onSuccessMessage("Saved pasted text successfully!")
                        onDismiss()
                    } else {
                        errorMessage = res.exceptionOrNull()?.message ?: "Failed to save text"
                    }
                }
            },
            enabled = pastedContent.isNotBlank() && !isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.ContentPaste, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Save and Open Text")
            }
        }
    }
}

@Composable
fun TabImportJson(
    onDismiss: () -> Unit,
    onPickJsonFiles: () -> Unit,
    onImportJsonText: suspend (String) -> Result<Int>,
    onSuccessMessage: (String) -> Unit
) {
    var jsonText by remember { mutableStateOf("") }
    var isImporting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = onPickJsonFiles,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.UploadFile, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Select JSON File From Storage")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        Text(
            text = "Or paste JSON directly:",
            style = MaterialTheme.typography.labelMedium
        )

        OutlinedTextField(
            value = jsonText,
            onValueChange = { jsonText = it },
            placeholder = { Text("[{\"id\":\"r001\",\"level\":\"B1\",\"topic\":\"Travel\",\"title_en\":\"...\",\"title_ar\":\"...\",\"sentences\":[...],\"questions\":[...]}]") },
            minLines = 4,
            maxLines = 8,
            modifier = Modifier.fillMaxWidth()
        )

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Button(
            onClick = {
                isImporting = true
                error = null
                scope.launch {
                    val res = onImportJsonText(jsonText)
                    isImporting = false
                    if (res.isSuccess) {
                        onSuccessMessage("Imported ${res.getOrDefault(0)} texts!")
                        onDismiss()
                    } else {
                        error = res.exceptionOrNull()?.message ?: "Import error"
                    }
                }
            },
            enabled = jsonText.isNotBlank() && !isImporting,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isImporting) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Text("Import JSON Text")
            }
        }
    }
}

@Composable
fun TabSyncAssets(
    onDismiss: () -> Unit,
    onSyncAssets: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp)
        )
        Text(
            text = "Sync Reading Texts from Assets",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "This reloads reading_texts.json from the app's assets and upserts texts by ID. Your quiz scores and reading progress are always preserved.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = {
                onSyncAssets()
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Sync from Assets Now")
        }
    }
}

@Composable
fun ReadingRecommendedShelf(
    userLevel: String,
    recommendedTexts: List<ReadingTextEntity>,
    progressByTextId: Map<String, ReadingProgressEntity>,
    onSelectText: (ReadingTextEntity) -> Unit,
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
            .testTag("reading_recommended_shelf"),
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
                        color = TextPrimary
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SkyAccentBg,
                        border = BorderStroke(1.dp, SkyAccent.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = userLevel.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                            color = SkyAccent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "نصوص قراءة موصى بها لمستواك ($levelAr)",
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
                    text = "${recommendedTexts.size} passages",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = GoldAccent,
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
                items = recommendedTexts,
                key = { "rec_reading_${it.id}" }
            ) { text ->
                val progress = progressByTextId[text.id]
                val isDone = progress?.isCompleted == true
                ReadingRecommendedCard(
                    text = text,
                    isDone = isDone,
                    levelColor = levelColor,
                    levelBg = levelBg,
                    onClick = { onSelectText(text) }
                )
            }
        }
    }
}

@Composable
fun ReadingRecommendedCard(
    text: ReadingTextEntity,
    isDone: Boolean,
    levelColor: Color,
    levelBg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(250.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("reading_recommended_card_${text.id}"),
        shape = RoundedCornerShape(14.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, if (isDone) EmeraldAccent.copy(alpha = 0.4f) else GoldAccentBorder.copy(alpha = 0.4f))
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
                        text = text.level.uppercase(),
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
                        text = if (isDone) "✓ Completed" else "Read →",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        ),
                        color = if (isDone) EmeraldAccent else GoldAccent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // English Title: 2 full lines
            Text(
                text = text.titleEn,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp),
                color = TextPrimary,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Arabic Title: Right-aligned (RTL) in GoldAccent
            if (text.titleAr.isNotBlank()) {
                Text(
                    text = text.titleAr,
                    style = ArabicTypography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = GoldAccent,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Subtle divider above footer
            HorizontalDivider(
                modifier = Modifier.padding(top = 2.dp),
                thickness = 1.dp,
                color = BorderSubtle
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val totalWords = remember(text.sentencesJson) {
                    try {
                        val list = readingJson.decodeFromString<List<ReadingSentenceItem>>(text.sentencesJson)
                        list.sumOf { it.en.split(READING_WHITESPACE_REGEX).size }.coerceAtLeast(15)
                    } catch (e: Exception) {
                        (text.sentencesJson.length / 6).coerceAtLeast(15)
                    }
                }
                val durationMin = (totalWords / 65).coerceAtLeast(1)
                Text(
                    text = "~$durationMin min • $totalWords words",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    softWrap = false
                )

                if (text.topic.isNotBlank()) {
                    Text(
                        text = "• ${text.topic}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

