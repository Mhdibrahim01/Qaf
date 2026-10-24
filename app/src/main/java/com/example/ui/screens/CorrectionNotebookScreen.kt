package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.BidiText
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevatedDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.data.local.CorrectionNotebookEntity
import com.example.data.local.GrammarLessonEntity
import com.example.data.local.ReadingTextEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.serialization.json.Json

private val notebookJson = Json { ignoreUnknownKeys = true; isLenient = true }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CorrectionNotebookScreen(
    entries: List<CorrectionNotebookEntity>,
    grammarLessons: List<GrammarLessonEntity>,
    readingTexts: List<ReadingTextEntity>,
    onBack: () -> Unit,
    onSaveEntry: (textId: String, lessonId: String, lessonTitle: String, original: String, corrected: String, expAr: String, alt: String, status: String) -> Unit,
    onUpdateEntry: (CorrectionNotebookEntity) -> Unit,
    onDeleteEntry: (Long) -> Unit,
    onCheckWithAi: suspend (CorrectionNotebookEntity) -> Result<Unit>,
    onRecordSpacedReview: (id: Long, newSentence: String, isSuccess: Boolean) -> Unit,
    onNavigateToLesson: (GrammarLessonEntity) -> Unit,
    onNavigateToReadingText: (ReadingTextEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedStatusFilter by remember { mutableStateOf("ALL") }
    var selectedLessonFilter by remember { mutableStateOf("ALL") }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<CorrectionNotebookEntity?>(null) }
    var reviewTargetEntry by remember { mutableStateOf<CorrectionNotebookEntity?>(null) }
    var deletingEntryId by remember { mutableStateOf<Long?>(null) }
    var checkingEntryId by remember { mutableStateOf<Long?>(null) }

    val statusOptions = listOf("ALL", "not_checked", "checked", "mastered", "due_review")
    val lessonIdsWithEntries = remember(entries) {
        listOf("ALL") + entries.map { it.grammarLessonId }.filter { it.isNotBlank() }.distinct()
    }

    val now = System.currentTimeMillis()
    val filteredEntries = remember(entries, selectedStatusFilter, selectedLessonFilter) {
        entries.filter { item ->
            val matchesStatus = when (selectedStatusFilter) {
                "ALL" -> true
                "due_review" -> item.nextReviewTimestamp in 1..now && item.status != "mastered"
                else -> item.status == selectedStatusFilter
            }
            val matchesLesson = selectedLessonFilter == "ALL" || item.grammarLessonId == selectedLessonFilter
            matchesStatus && matchesLesson
        }
    }

    Scaffold(
        modifier = modifier.testTag("correction_notebook_screen"),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Write Sentence") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.testTag("notebook_fab")
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Compact Action & Count Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${entries.size} Saved Sentences • 3-7-21 Day Spaced Review",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("notebook_add_manual_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Sentence", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Sentence", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // WEEKLY STUDY GOAL BANNER (Phase 9 Plan Integration)
            item(key = "weekly_study_plan_card", contentType = "weekly_plan_card") {
                WeeklyStudyPlanCard(
                    grammarLessons = grammarLessons,
                    readingTexts = readingTexts,
                    onOpenLesson = onNavigateToLesson,
                    onOpenText = onNavigateToReadingText
                )
            }

            // FILTER ROW: Status
            item(key = "notebook_status_filter_row", contentType = "status_filter_row") {
                val statusCounts = remember(entries, now) {
                    mapOf(
                        "ALL" to entries.size,
                        "not_checked" to entries.count { it.status == "not_checked" },
                        "checked" to entries.count { it.status == "checked" },
                        "mastered" to entries.count { it.status == "mastered" },
                        "due_review" to entries.count { it.nextReviewTimestamp in 1..now && it.status != "mastered" }
                    )
                }
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(
                        items = statusOptions,
                        key = { it },
                        contentType = { "status_filter_chip" }
                    ) { status ->
                        val count = statusCounts[status] ?: 0
                        val label = when (status) {
                            "ALL" -> "All ($count)"
                            "not_checked" -> "Not Checked ($count)"
                            "checked" -> "AI Checked ($count)"
                            "mastered" -> "Mastered ($count)"
                            "due_review" -> "Due for Review ($count)"
                            else -> status
                        }
                        FilterChip(
                            selected = selectedStatusFilter == status,
                            onClick = { selectedStatusFilter = status },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            // FILTER ROW: Lessons
            if (lessonIdsWithEntries.size > 2) {
                item(key = "notebook_lesson_filter_row", contentType = "lesson_filter_row") {
                    val lessonsById = remember(grammarLessons) { grammarLessons.associateBy { it.id } }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(
                            items = lessonIdsWithEntries,
                            key = { it },
                            contentType = { "lesson_filter_chip" }
                        ) { lId ->
                            val lessonTitle = if (lId == "ALL") "All Lessons" else {
                                lessonsById[lId]?.titleEn ?: lId
                            }
                            FilterChip(
                                selected = selectedLessonFilter == lId,
                                onClick = { selectedLessonFilter = lId },
                                label = { Text(lessonTitle) }
                            )
                        }
                    }
                }
            }

            // EMPTY STATE
            if (filteredEntries.isEmpty()) {
                item(key = "notebook_empty_state", contentType = "empty_state") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                modifier = Modifier.size(54.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "No notebook entries yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Read a passage like 'The Missed Interview' and write a sentence in 'Your turn', or add your own sentences by hand.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(14.dp))
                            Button(onClick = { showAddDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Add Practice Sentence")
                            }
                        }
                    }
                }
            } else {
                // LIST OF NOTEBOOK ENTRIES
                items(
                    items = filteredEntries,
                    key = { it.id },
                    contentType = { "correction_entry_card" }
                ) { entry ->
                    val linkedLesson = grammarLessons.firstOrNull { it.id == entry.grammarLessonId }
                    CorrectionEntryCard(
                        entry = entry,
                        linkedLesson = linkedLesson,
                        isChecking = checkingEntryId == entry.id,
                        onCheckWithAi = {
                            checkingEntryId = entry.id
                            scope.launch {
                                val res = onCheckWithAi(entry)
                                checkingEntryId = null
                                if (res.isSuccess) {
                                    snackbarHostState.showSnackbar("Sentence reviewed by AI successfully!")
                                } else {
                                    snackbarHostState.showSnackbar("AI check failed: ${res.exceptionOrNull()?.message}")
                                }
                            }
                        },
                        onEdit = { editingEntry = entry },
                        onDelete = { deletingEntryId = entry.id },
                        onStartReview = { reviewTargetEntry = entry },
                        onOpenLesson = {
                            if (linkedLesson != null) {
                                onNavigateToLesson(linkedLesson)
                            }
                        }
                    )
                }
            }
        }
    }

    // ADD / EDIT ENTRY DIALOG
    if (showAddDialog || editingEntry != null) {
        val target = editingEntry
        AddOrEditCorrectionDialog(
            initialEntry = target,
            grammarLessons = grammarLessons,
            onDismiss = {
                showAddDialog = false
                editingEntry = null
            },
            onSave = { lessonId, lessonTitle, original, corrected, explanationAr, alt ->
                if (target != null) {
                    onUpdateEntry(
                        target.copy(
                            grammarLessonId = lessonId,
                            grammarLessonTitle = lessonTitle,
                            originalSentence = original,
                            correctedSentence = corrected,
                            explanationAr = explanationAr,
                            betterAlternative = alt,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                } else {
                    onSaveEntry(
                        "",
                        lessonId,
                        lessonTitle,
                        original,
                        corrected,
                        explanationAr,
                        alt,
                        if (corrected.isNotBlank()) "checked" else "not_checked"
                    )
                }
                showAddDialog = false
                editingEntry = null
                scope.launch { snackbarHostState.showSnackbar("Sentence saved in notebook!") }
            }
        )
    }

    // SPACED REVIEW DIALOG ("Try again later")
    reviewTargetEntry?.let { entry ->
        val linkedLesson = grammarLessons.firstOrNull { it.id == entry.grammarLessonId }
        SpacedReviewDialog(
            entry = entry,
            linkedLesson = linkedLesson,
            onDismiss = { reviewTargetEntry = null },
            onSubmitAttempt = { newSentence, isSuccess ->
                onRecordSpacedReview(entry.id, newSentence, isSuccess)
                reviewTargetEntry = null
                scope.launch {
                    val nextStep = entry.reviewStep + 1
                    if (nextStep >= 3) {
                        snackbarHostState.showSnackbar("🎉 Mastered! 3 successful spaced reviews completed.")
                    } else {
                        snackbarHostState.showSnackbar("Attempt saved! Next review scheduled.")
                    }
                }
            }
        )
    }

    // DELETE CONFIRMATION
    deletingEntryId?.let { id ->
        AlertDialog(
            onDismissRequest = { deletingEntryId = null },
            title = { Text("Delete Entry") },
            text = { Text("Are you sure you want to remove this sentence from your notebook?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteEntry(id)
                        deletingEntryId = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingEntryId = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// -------------------------------------------------------------------------------------------------
// WEEKLY STUDY GOAL CARD (Phase 9 Plan Integration)
// -------------------------------------------------------------------------------------------------

@Composable
fun WeeklyStudyPlanCard(
    grammarLessons: List<GrammarLessonEntity>,
    readingTexts: List<ReadingTextEntity>,
    onOpenLesson: (GrammarLessonEntity) -> Unit,
    onOpenText: (ReadingTextEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    // Pair a sample/core grammar lesson (e.g. g054 Third conditional) with its linked reading text (r054)
    val targetText = readingTexts.firstOrNull { it.grammarIdsJson.contains("g054") || it.id == "r054" }
        ?: readingTexts.firstOrNull()
    val targetLesson = grammarLessons.firstOrNull { it.id == "g054" }
        ?: grammarLessons.firstOrNull()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("weekly_study_plan_card"),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceDark
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = GoldAccent,
                        shape = CircleShape,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "9",
                                fontWeight = FontWeight.Bold,
                                color = com.example.ui.theme.OnGoldDark,
                                fontSize = 13.sp
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Weekly Study Goal (Phase 9)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent
                        )
                        Text(
                            text = "الخطة الأسبوعية: قراءة • قاعدة • تصحيح",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Surface(
                    color = GoldAccent.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "1 Lesson + 1 Text",
                        color = GoldAccent,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // 3-Session Flow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Session 1: Read & Mark structures
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = targetText != null) {
                            if (targetText != null) onOpenText(targetText)
                        },
                    color = SurfaceElevatedDark,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Session 1", style = MaterialTheme.typography.labelSmall, color = GoldAccent, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("Read & Mark", style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text("نص القراءة", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }

                // Session 2: Lesson & Exercises
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = targetLesson != null) {
                            if (targetLesson != null) onOpenLesson(targetLesson)
                        },
                    color = SurfaceElevatedDark,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Session 2", style = MaterialTheme.typography.labelSmall, color = GoldAccent, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("Lesson & Quiz", style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text("شرح وتمارين", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }

                // Session 3: Write & Review
                Surface(
                    modifier = Modifier.weight(1f),
                    color = GoldAccent.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Session 3", style = MaterialTheme.typography.labelSmall, color = GoldAccent, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("Write & Review", style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text("اكتب وراجع", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// CORRECTION ENTRY CARD
// -------------------------------------------------------------------------------------------------

@Composable
fun CorrectionEntryCard(
    entry: CorrectionNotebookEntity,
    linkedLesson: GrammarLessonEntity?,
    isChecking: Boolean,
    onCheckWithAi: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStartReview: () -> Unit,
    onOpenLesson: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val dateStr = remember(entry.updatedAt) { dateFormat.format(Date(entry.updatedAt)) }
    val now = System.currentTimeMillis()
    val isDueForReview = entry.nextReviewTimestamp in 1..now && entry.status != "mastered"

    val (statusColor, statusText) = when (entry.status) {
        "mastered" -> Pair(GoldAccent, "Mastered")
        "checked" -> Pair(com.example.ui.theme.EmeraldAccent, "Checked")
        else -> Pair(com.example.ui.theme.AmberAccent, "Pending")
    }

    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("correction_card_${entry.id}"),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            if (isDueForReview) GoldAccent else MaterialTheme.colorScheme.outlineVariant
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (entry.status == "mastered") MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Lesson Title + Status Badge + Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.clickable(enabled = linkedLesson != null) { onOpenLesson() }
                    ) {
                        Text(
                            text = entry.grammarLessonTitle.ifBlank { entry.grammarLessonId },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Surface(
                        color = statusColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // User's original sentence
            Text(
                text = "Your sentence / جملتك:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = entry.originalSentence,
                style = MaterialTheme.typography.bodyLarge.copy(
                    textDirection = TextDirection.Ltr,
                    textAlign = TextAlign.Left
                ),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 2.dp)
            )

            // AI Checked details
            if (entry.correctedSentence.isNotBlank() && entry.correctedSentence != entry.originalSentence) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Corrected / التصحيح:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1B5E20), fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = entry.correctedSentence,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textDirection = TextDirection.Ltr,
                                textAlign = TextAlign.Left
                            ),
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1B5E20)
                        )
                    }
                }
            }

            // Arabic Explanation
            if (entry.explanationAr.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                BidiText(
                    text = entry.explanationAr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Better Alternative
            if (entry.betterAlternative.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFF57F17), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Alternative: \"${entry.betterAlternative}\"",
                        style = MaterialTheme.typography.bodySmall.copy(
                            textDirection = TextDirection.Ltr,
                            textAlign = TextAlign.Left
                        ),
                        color = Color(0xFFF57F17)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(10.dp))

            // Footer: Check with AI button & Spaced Review status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (entry.status == "not_checked") {
                        Button(
                            onClick = onCheckWithAi,
                            enabled = !isChecking,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("notebook_check_ai_button_${entry.id}")
                        ) {
                            if (isChecking) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(6.dp))
                                Text("Checking...")
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Check with AI", fontSize = 12.sp)
                            }
                        }
                    }

                    // Spaced Review button: "Try again later"
                    OutlinedButton(
                        onClick = onStartReview,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isDueForReview) com.example.ui.theme.GoldAccentDim else Color.Transparent
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isDueForReview) GoldAccent else MaterialTheme.colorScheme.outlineVariant
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("notebook_review_button_${entry.id}")
                    ) {
                        Icon(
                            imageVector = if (entry.status == "mastered") Icons.Default.CheckCircle else Icons.Default.Schedule,
                            contentDescription = null,
                            tint = if (entry.status == "mastered") Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (isDueForReview) "Review Due! (Step ${entry.reviewStep}/3)"
                            else if (entry.status == "mastered") "Mastered ✓"
                            else "Spaced Review (${entry.reviewStep}/3)",
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// ADD / EDIT CORRECTION DIALOG
// -------------------------------------------------------------------------------------------------

@Composable
fun AddOrEditCorrectionDialog(
    initialEntry: CorrectionNotebookEntity?,
    grammarLessons: List<GrammarLessonEntity>,
    onDismiss: () -> Unit,
    onSave: (lessonId: String, lessonTitle: String, original: String, corrected: String, expAr: String, alt: String) -> Unit
) {
    var selectedLessonId by remember {
        mutableStateOf(initialEntry?.grammarLessonId ?: grammarLessons.firstOrNull()?.id ?: "g054")
    }
    var originalSentence by remember { mutableStateOf(initialEntry?.originalSentence ?: "") }
    var correctedSentence by remember { mutableStateOf(initialEntry?.correctedSentence ?: "") }
    var explanationAr by remember { mutableStateOf(initialEntry?.explanationAr ?: "") }
    var betterAlternative by remember { mutableStateOf(initialEntry?.betterAlternative ?: "") }

    val selectedLesson = grammarLessons.firstOrNull { it.id == selectedLessonId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialEntry != null) "Edit Sentence" else "Add to Correction Notebook")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Grammar Structure / القاعدة:", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(grammarLessons) { lesson ->
                        FilterChip(
                            selected = selectedLessonId == lesson.id,
                            onClick = { selectedLessonId = lesson.id },
                            label = { Text(lesson.titleEn) }
                        )
                    }
                }

                OutlinedTextField(
                    value = originalSentence,
                    onValueChange = { originalSentence = it },
                    label = { Text("Your English Sentence *") },
                    placeholder = { Text("e.g. If I had known, I would have come.") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_original_sentence_input")
                )

                OutlinedTextField(
                    value = correctedSentence,
                    onValueChange = { correctedSentence = it },
                    label = { Text("Corrected Sentence (Optional / By hand)") },
                    minLines = 1,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = explanationAr,
                    onValueChange = { explanationAr = it },
                    label = { Text("Arabic Explanation (Optional)") },
                    minLines = 1,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = betterAlternative,
                    onValueChange = { betterAlternative = it },
                    label = { Text("Better Alternative (Optional)") },
                    minLines = 1,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (originalSentence.isNotBlank()) {
                        onSave(
                            selectedLessonId,
                            selectedLesson?.titleEn ?: selectedLessonId,
                            originalSentence.trim(),
                            correctedSentence.trim(),
                            explanationAr.trim(),
                            betterAlternative.trim()
                        )
                    }
                },
                enabled = originalSentence.isNotBlank(),
                modifier = Modifier.testTag("dialog_save_sentence_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// -------------------------------------------------------------------------------------------------
// SPACED REVIEW DIALOG ("Try again later": 3 -> 7 -> 21 days -> Mastered)
// -------------------------------------------------------------------------------------------------

@Composable
fun SpacedReviewDialog(
    entry: CorrectionNotebookEntity,
    linkedLesson: GrammarLessonEntity?,
    onDismiss: () -> Unit,
    onSubmitAttempt: (newSentence: String, isSuccess: Boolean) -> Unit
) {
    var newSentenceInput by remember { mutableStateOf("") }
    var showLessonExamples by remember { mutableStateOf(false) }

    val situation = entry.situationPrompt.ifBlank {
        "You missed your flight because you left your passport at home."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Spaced Review (Step ${entry.reviewStep + 1}/3)")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Target grammar rule
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Target Grammar Structure:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${entry.grammarLessonTitle} (${linkedLesson?.titleAr ?: ""})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Everyday Situation Prompt
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                            Spacer(Modifier.width(6.dp))
                            Text("New Situation Prompt:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        BidiText(text = situation, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                }

                Text(
                    text = "Write a sentence describing this situation using the target grammar structure:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = newSentenceInput,
                    onValueChange = { newSentenceInput = it },
                    placeholder = { Text("Write your new sentence here...") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("spaced_review_sentence_input")
                )

                // Compare with lesson examples
                TextButton(
                    onClick = { showLessonExamples = !showLessonExamples },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        imageVector = if (showLessonExamples) Icons.Default.Close else Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (showLessonExamples) "Hide Examples" else "Compare with Lesson Examples")
                }

                if (showLessonExamples && linkedLesson != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Lesson Model Examples:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            val examples = try {
                                notebookJson.decodeFromString<List<com.example.data.local.GrammarExampleItem>>(linkedLesson.examplesJson)
                            } catch (_: Exception) { emptyList() }
                            examples.take(2).forEach { eg ->
                                Text(
                                    text = "• ${eg.en}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        textDirection = TextDirection.Ltr,
                                        textAlign = TextAlign.Left
                                    ),
                                    fontWeight = FontWeight.SemiBold
                                )
                                BidiText(
                                    text = "  ${eg.ar}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newSentenceInput.isNotBlank()) {
                        onSubmitAttempt(newSentenceInput.trim(), true)
                    }
                },
                enabled = newSentenceInput.isNotBlank(),
                modifier = Modifier.testTag("spaced_review_submit_button")
            ) {
                Text(if (entry.reviewStep >= 2) "Complete & Master!" else "Submit Attempt")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
