package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ai.AiTaskStatus
import com.example.data.importer.DataImportRepository
import com.example.data.importer.ImportProgressState
import com.example.data.local.AttemptEntity
import com.example.data.local.DailyChecklistEntity
import com.example.data.local.DailyTaskItem
import com.example.data.local.DeckArabicStats
import com.example.data.local.DeckEntity
import com.example.data.local.StudyPlanProfileEntity
import com.example.ui.components.ArabicText
import com.example.ui.components.GoldBadge
import kotlinx.serialization.json.Json
import com.example.ui.theme.ArabicTypography
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderDark
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldAccentBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevatedDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.NumberFormat
import java.util.Locale

data class LabsModuleRegistryEntry(
    val id: String,
    val titleEn: String,
    val titleAr: String,
    val descriptionEn: String,
    val badge: String = "Coming soon"
)

val DefaultLabsRegistry = listOf(
    LabsModuleRegistryEntry(
        id = "pronunciation_coach",
        titleEn = "Pronunciation Coach",
        titleAr = "مدرب النطق الصوتي الذكي",
        descriptionEn = "Phoneme-level speech scoring with Arabic phonetic guidance."
    ),
    LabsModuleRegistryEntry(
        id = "watch_and_learn",
        titleEn = "Watch & Learn",
        titleAr = "شاهد وتعلم بالترجمة المزدوجة",
        descriptionEn = "Interactive dual-subtitle video player with one-tap word lookup."
    ),
    LabsModuleRegistryEntry(
        id = "ai_tutor_chat",
        titleEn = "AI Tutor Chat",
        titleAr = "المعلم الذكي للمحادثة",
        descriptionEn = "Roleplay scenarios in English with instant Arabic corrections."
    )
)

@Composable
fun LabsDataCheckScreen(
    decks: List<DeckEntity>,
    importState: ImportProgressState,
    aiStatus: AiTaskStatus = AiTaskStatus(),
    totalEntriesCount: Int,
    emptyArabicCount: Int,
    deckArabicStats: List<DeckArabicStats> = emptyList(),
    grammarLessonsCount: Int = 0,
    studyPlanProfile: StudyPlanProfileEntity? = null,
    todayChecklist: DailyChecklistEntity? = null,
    recentAttempts: List<AttemptEntity> = emptyList(),
    totalAttemptsCount: Int = 0,
    reviewDueTodayCount: Int = 0,
    onSimulateBacklog42: () -> Unit = {},
    onRebuildTodayPlan: () -> Unit = {},
    onReimportDatabase: () -> Unit,
    onResetAndReimport: () -> Unit,
    onImportJsonUris: (List<Uri>) -> Unit,
    onOpenBatchTranslate: () -> Unit = {},
    onGenerateDeckBatch: (String) -> Unit = {},
    onSyncGrammarAssets: () -> Unit = {},
    onImportGrammarJsonUris: (List<Uri>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val formatter = remember { NumberFormat.getIntegerInstance(Locale.US) }
    val todayTasks = remember(todayChecklist?.tasksJson) {
        val raw = todayChecklist?.tasksJson.orEmpty()
        if (raw.isBlank()) emptyList()
        else try {
            Json { ignoreUnknownKeys = true }.decodeFromString<List<DailyTaskItem>>(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }
    val deckCountMap = remember(decks) { decks.associateBy({ it.id }, { it.rowCount }) }
    val statsByDeck = remember(deckArabicStats) { deckArabicStats.associateBy { it.deck } }
    val enabledStates = remember { mutableStateMapOf<String, Boolean>() }
    var suggestionText by remember { mutableStateOf("") }
    var suggestionSaved by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    val jsonPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onImportJsonUris(uris)
        }
    }

    val grammarJsonPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onImportGrammarJsonUris(uris)
        }
    }

    val coreDecksMatched = DataImportRepository.EXPECTED_DECK_COUNTS.all { (deckId, expected) ->
        (deckCountMap[deckId] ?: 0) == expected
    }
    val coreEntriesTotal = DataImportRepository.EXPECTED_DECK_COUNTS.keys.sumOf { deckCountMap[it] ?: 0 }
    val totalFromFile = deckArabicStats.sumOf { it.fromFileCount }
    val totalFromAi = deckArabicStats.sumOf { it.fromAiCount }
    val totalFromUser = deckArabicStats.sumOf { it.fromUserCount }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .testTag("screen_labs"),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 0. ADAPTIVE ENGINE DIAGNOSTICS & PHASE 1-3 VERIFIER CARD
            item {
                val totalEstimatedMinutes = todayTasks.sumOf { it.estimatedMinutes }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("labs_adaptive_verifier_card"),
                    color = SurfaceDark,
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.65f))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Adaptive Engine Status",
                                    tint = EmeraldAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "Adaptive Engine Verifier (Phases 1–3)",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "فحص مباشر للخطة الذكية، تقسيم المراجعات، وسجل المحاولات (v15)",
                                        style = ArabicTypography.labelMedium,
                                        color = GoldAccent
                                    )
                                }
                            }
                            GoldBadge(text = "DB v15 ACTIVE")
                        }

                        HorizontalDivider(color = BorderDark)

                        // Live metrics summary
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = SurfaceElevatedDark,
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.dp, BorderDark)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                ProvenanceCounterChip(
                                    label = "Level / Goal",
                                    count = "${studyPlanProfile?.level ?: "B1"} • ${studyPlanProfile?.minutesPerDay ?: 15}m",
                                    color = GoldAccent,
                                    testTag = "verifier_level_goal"
                                )
                                ProvenanceCounterChip(
                                    label = "Due Reviews",
                                    count = formatter.format(reviewDueTodayCount),
                                    color = if (reviewDueTodayCount > 25) CoralAccent else EmeraldAccent,
                                    testTag = "verifier_due_reviews"
                                )
                                ProvenanceCounterChip(
                                    label = "Plan Est. Time",
                                    count = "~${totalEstimatedMinutes}m (${todayTasks.size} tasks)",
                                    color = EmeraldAccent,
                                    testTag = "verifier_plan_time"
                                )
                                ProvenanceCounterChip(
                                    label = "Attempts Log",
                                    count = formatter.format(totalAttemptsCount),
                                    color = TextPrimary,
                                    testTag = "verifier_attempts_count"
                                )
                            }
                        }

                        // Today's Scheduled Tasks & Data-Backed Reasons
                        Text(
                            text = "Today's Scheduled Tasks & Reasons (${todayChecklist?.completedCount ?: 0}/${todayTasks.size} done):",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary
                        )

                        if (todayTasks.isEmpty()) {
                            Text(
                                text = "No checklist generated yet — tap 'Rebuild Today's Plan' below.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        } else {
                            todayTasks.forEach { task ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = SurfaceElevatedDark.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, BorderDark)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${task.titleEn} (${task.completedCount}/${task.targetCount})",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = if (task.isCompleted) EmeraldAccent else TextPrimary,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                if (task.backlogRemaining > 0) {
                                                    GoldBadge(text = "+${task.backlogRemaining} backlog")
                                                }
                                                GoldBadge(text = "~${task.estimatedMinutes}m")
                                            }
                                        }
                                        if (task.reason.isNotBlank()) {
                                            Text(
                                                text = "Reason: ${task.reason}",
                                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                                color = GoldAccent
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Recent Attempts Log (Phase 2 verification)
                        if (recentAttempts.isNotEmpty()) {
                            HorizontalDivider(color = BorderDark)
                            Text(
                                text = "Latest Recorded Attempts (Phase 2 `attempts` table):",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary
                            )
                            recentAttempts.take(4).forEach { att ->
                                Text(
                                    text = "• [${att.activityType}] item=${att.itemId} • rating=${att.sm2Rating ?: "-"} • score=${att.score ?: "-"}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = TextSecondary
                                )
                            }
                        }

                        // Interactive Simulation Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onSimulateBacklog42,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldAccent,
                                    contentColor = BackgroundDark
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("labs_simulate_backlog_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Science,
                                    contentDescription = "Simulate 42 Due Reviews",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Simulate 42 Due Reviews", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = onRebuildTodayPlan,
                                border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.7f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("labs_rebuild_plan_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Rebuild Today's Plan",
                                    tint = EmeraldAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Rebuild Today's Plan", color = EmeraldAccent, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // 1. DATA CHECK CARD (Real SQLite counts & per-deck Arabic provenance)
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("data_check_card"),
                    color = SurfaceDark,
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (coreDecksMatched) EmeraldAccent.copy(alpha = 0.6f) else GoldAccentBorder
                    )
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (coreDecksMatched) Icons.Default.Verified else Icons.Default.ErrorOutline,
                                    contentDescription = "Data check status",
                                    tint = if (coreDecksMatched) EmeraldAccent else GoldAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "Data check • Room SQLite (v${DataImportRepository.DATA_VERSION})",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Per-deck row counts & Arabic provenance (file / ai / user / empty)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                            GoldBadge(
                                text = if (coreDecksMatched) "VERIFIED" else "PENDING"
                            )
                        }

                        if (importState.isImporting) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Upserting ${importState.currentFile} (Batch #${importState.batchNumber})...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GoldAccent
                                )
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(999.dp)),
                                    color = GoldAccent,
                                    trackColor = SurfaceElevatedDark
                                )
                            }
                        }

                        HorizontalDivider(color = BorderDark)

                        // Expected vs Actual per core deck + per-deck Arabic source breakdown
                        DataImportRepository.EXPECTED_DECK_COUNTS.forEach { (deckId, expectedCount) ->
                            val stats = statsByDeck[deckId]
                            val actualCount = stats?.totalRows ?: (deckCountMap[deckId] ?: 0)
                            val isMatch = actualCount == expectedCount
                            DeckStatsVerificationRow(
                                label = deckId,
                                actualText = "${formatter.format(actualCount)} / ${formatter.format(expectedCount)}",
                                isVerified = isMatch,
                                fromFileCount = stats?.fromFileCount ?: 0,
                                fromAiCount = stats?.fromAiCount ?: 0,
                                fromUserCount = stats?.fromUserCount ?: 0,
                                emptyCount = stats?.emptyCount ?: actualCount,
                                formatter = formatter,
                                testTag = "data_check_row_$deckId"
                            )
                        }

                        // Specialized generated decks (100001+ to 700001+)
                        val specializedDecksMeta = listOf(
                            "ielts_core" to "ielts_core (IDs 100001+ • target 3,575)",
                            "it_industry" to "it_industry (IDs 200001+ • target 1,622)",
                            "cefr_c1" to "cefr_c1 (IDs 300001+ • target 1,853)",
                            "pte_academic" to "pte_academic (IDs 400001+ • target 2,651)",
                            "business_english" to "business_english (IDs 500001+ • target 1,250)",
                            "career_interview" to "career_interview (IDs 600001+ • target 406)",
                            "medical_healthcare" to "medical_healthcare (IDs 700001+ • target 405)"
                        )
                        specializedDecksMeta.forEach { (deckId, labelText) ->
                            val specStats = statsByDeck[deckId]
                            val specCount = specStats?.totalRows ?: (deckCountMap[deckId] ?: 0)
                            DeckStatsVerificationRow(
                                label = labelText,
                                actualText = formatter.format(specCount),
                                isVerified = specCount > 0,
                                fromFileCount = specStats?.fromFileCount ?: 0,
                                fromAiCount = specStats?.fromAiCount ?: 0,
                                fromUserCount = specStats?.fromUserCount ?: 0,
                                emptyCount = specStats?.emptyCount ?: 0,
                                formatter = formatter,
                                testTag = "data_check_row_$deckId"
                            )
                        }

                        HorizontalDivider(color = BorderDark)

                        // Total rows and Arabic provenance summary
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = SurfaceElevatedDark,
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.dp, BorderDark)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Core 7 Files Total",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${formatter.format(coreEntriesTotal)} / ${formatter.format(DataImportRepository.CORE_EXPECTED_ROWS)}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (coreDecksMatched) EmeraldAccent else GoldAccent,
                                        modifier = Modifier.testTag("data_check_total_count")
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Total DB Entries (incl. new decks)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = formatter.format(totalEntriesCount),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary
                                    )
                                }

                                HorizontalDivider(color = BorderDark)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    ProvenanceCounterChip(
                                        label = "From File",
                                        count = formatter.format(totalFromFile),
                                        color = EmeraldAccent,
                                        testTag = "data_check_total_from_file"
                                    )
                                    ProvenanceCounterChip(
                                        label = "From AI",
                                        count = formatter.format(totalFromAi),
                                        color = GoldAccent,
                                        testTag = "data_check_total_from_ai"
                                    )
                                    ProvenanceCounterChip(
                                        label = "User Edits",
                                        count = formatter.format(totalFromUser),
                                        color = TextPrimary,
                                        testTag = "data_check_total_from_user"
                                    )
                                    ProvenanceCounterChip(
                                        label = "Still Empty",
                                        count = formatter.format(emptyArabicCount),
                                        color = CoralAccent,
                                        testTag = "data_check_empty_arabic_count"
                                    )
                                }
                            }
                        }

                        // Action buttons for re-checking, resetting, or importing JSON files from device
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onReimportDatabase,
                                enabled = !importState.isImporting,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldAccent,
                                    contentColor = BackgroundDark
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("recheck_import_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Upsert from JSON files",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Upsert & Verify", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { showResetConfirmDialog = true },
                                enabled = !importState.isImporting,
                                border = BorderStroke(1.dp, CoralAccent.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("clear_reimport_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Clear and re-import",
                                    tint = CoralAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Clear & Re-import", color = CoralAccent)
                            }
                        }

                        OutlinedButton(
                            onClick = { jsonPickerLauncher.launch(arrayOf("application/json", "*/*")) },
                            enabled = !importState.isImporting,
                            border = BorderStroke(1.dp, GoldAccentBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pick_json_files_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileOpen,
                                contentDescription = "Select JSON files from device",
                                tint = GoldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Import JSON Files (Upserts by ID • Preserves Progress & User Edits)",
                                color = GoldAccent,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = onOpenBatchTranslate,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldAccent,
                                contentColor = BackgroundDark
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_batch_translate_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = "Open Batch Translate",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Batch Translate (${formatter.format(emptyArabicCount)} Empty Words)",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        HorizontalDivider(color = BorderDark, thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Grammar Lessons Database",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$grammarLessonsCount lessons loaded (Room DB)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = onSyncGrammarAssets,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("labs_sync_grammar_assets_button")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sync Assets", style = MaterialTheme.typography.labelSmall)
                                }

                                Button(
                                    onClick = { grammarJsonPickerLauncher.launch(arrayOf("application/json", "*/*")) },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = BackgroundDark),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("labs_import_grammar_json_button")
                                ) {
                                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Import JSON", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 2. GEMINI AI DECK BUILDER & BATCH ARABIC TRANSLATOR
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gemini_deck_builder_card"),
                    color = SurfaceDark,
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, GoldAccentBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Gemini AI",
                                tint = GoldAccent,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = "Gemini Deck Builder & Arabic Translator",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Uses Gemini Models with multi-model fallback & custom key support • Fixed IDs (100001+ / 200001+)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )
                            }
                        }

                        if (aiStatus.title.isNotBlank() || aiStatus.error != null) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = SurfaceElevatedDark,
                                shape = MaterialTheme.shapes.medium,
                                border = BorderStroke(
                                    1.dp,
                                    if (aiStatus.error != null) CoralAccent.copy(alpha = 0.6f) else GoldAccentBorder
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    if (aiStatus.isRunning) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = GoldAccent,
                                            strokeWidth = 2.dp
                                        )
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        if (aiStatus.title.isNotBlank()) {
                                            Text(
                                                text = aiStatus.title,
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                color = if (aiStatus.error != null) CoralAccent else GoldAccent
                                            )
                                        }
                                        if (aiStatus.detail.isNotBlank()) {
                                            Text(
                                                text = aiStatus.detail,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                        }
                                        if (aiStatus.error != null) {
                                            Text(
                                                text = aiStatus.error,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = CoralAccent
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = onOpenBatchTranslate,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldAccent,
                                contentColor = BackgroundDark
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("translate_batch_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = "Open Batch Translate",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Open Batch Translate (${formatter.format(emptyArabicCount)} Empty Words)",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onGenerateDeckBatch("ielts_core") },
                                enabled = !aiStatus.isRunning,
                                border = BorderStroke(1.dp, GoldAccentBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("generate_ielts_batch_button")
                            ) {
                                Text(
                                    text = "+25 ielts_core",
                                    color = GoldAccent,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            OutlinedButton(
                                onClick = { onGenerateDeckBatch("it_industry") },
                                enabled = !aiStatus.isRunning,
                                border = BorderStroke(1.dp, GoldAccentBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("generate_it_batch_button")
                            ) {
                                Text(
                                    text = "+25 it_industry",
                                    color = GoldAccent,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // 3. EXPERIMENTAL LABS REGISTRY
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SurfaceDark,
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, BorderDark)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = "Labs modules",
                                tint = GoldAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Experimental Modules Registry",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                        }

                        DefaultLabsRegistry.forEach { module ->
                            val isEnabled = enabledStates[module.id] ?: false
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = SurfaceElevatedDark,
                                shape = MaterialTheme.shapes.medium,
                                border = BorderStroke(1.dp, if (isEnabled) GoldAccentBorder else BorderDark)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = module.titleEn,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = TextPrimary
                                            )
                                            GoldBadge(text = module.badge)
                                        }
                                        ArabicText(
                                            text = module.titleAr,
                                            style = ArabicTypography.bodyMedium,
                                            color = GoldAccent
                                        )
                                        Text(
                                            text = module.descriptionEn,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                    Switch(
                                        checked = isEnabled,
                                        onCheckedChange = { enabledStates[module.id] = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = BackgroundDark,
                                            checkedTrackColor = GoldAccent
                                        ),
                                        modifier = Modifier.testTag("labs_switch_${module.id}")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. SUGGEST A FEATURE FORM
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SurfaceDark,
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, BorderDark)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Suggest a Feature",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        OutlinedTextField(
                            value = suggestionText,
                            onValueChange = {
                                suggestionText = it
                                suggestionSaved = false
                            },
                            placeholder = {
                                Text("Describe an English-Arabic feature you want in WordBooks...", color = TextMuted)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("labs_suggestion_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldAccent,
                                unfocusedBorderColor = BorderDark,
                                focusedContainerColor = SurfaceElevatedDark,
                                unfocusedContainerColor = SurfaceElevatedDark,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (suggestionSaved) {
                                Text(
                                    text = "Saved locally to device",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = EmeraldAccent
                                )
                            } else {
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Button(
                                onClick = {
                                    if (suggestionText.isNotBlank()) {
                                        suggestionSaved = true
                                        suggestionText = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldAccent,
                                    contentColor = BackgroundDark
                                ),
                                modifier = Modifier.testTag("labs_submit_suggestion_button")
                            ) {
                                Text("Save Suggestion", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "Clear & Re-import Database? / إعادة تعيين قاعدة البيانات",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "This will completely clear your database and re-seed all wordbooks from assets/JSON. Any custom edits and progress may be reset. Are you sure you want to proceed?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmDialog = false
                        onResetAndReimport()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoralAccent,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_confirm_clear_reimport")
                ) {
                    Text("Clear & Re-import / تفريغ واستيراد", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetConfirmDialog = false },
                    modifier = Modifier.testTag("btn_cancel_clear_reimport")
                ) {
                    Text("Cancel / إلغاء", color = TextMuted)
                }
            }
        )
    }
}

@Composable
private fun DeckStatsVerificationRow(
    label: String,
    actualText: String,
    isVerified: Boolean,
    fromFileCount: Int,
    fromAiCount: Int,
    fromUserCount: Int,
    emptyCount: Int,
    formatter: NumberFormat,
    testTag: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        color = SurfaceElevatedDark.copy(alpha = 0.55f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, BorderDark)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
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
                        imageVector = if (isVerified) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = if (isVerified) EmeraldAccent else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        color = TextPrimary
                    )
                }
                Text(
                    text = actualText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = if (isVerified) EmeraldAccent else GoldAccent
                )
            }

            // Per-deck Arabic breakdown: From file, From AI, User, Empty
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "File: ${formatter.format(fromFileCount)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = if (fromFileCount > 0) EmeraldAccent else TextSecondary
                )
                Text(
                    text = "AI: ${formatter.format(fromAiCount)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = if (fromAiCount > 0) GoldAccent else TextSecondary
                )
                if (fromUserCount > 0) {
                    Text(
                        text = "User: ${formatter.format(fromUserCount)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = TextPrimary
                    )
                }
                Text(
                    text = "Empty: ${formatter.format(emptyCount)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = if (emptyCount > 0) CoralAccent else EmeraldAccent
                )
            }
        }
    }
}

@Composable
private fun ProvenanceCounterChip(
    label: String,
    count: String,
    color: androidx.compose.ui.graphics.Color,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
        Text(
            text = count,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            ),
            color = color,
            modifier = Modifier.testTag(testTag)
        )
    }
}
