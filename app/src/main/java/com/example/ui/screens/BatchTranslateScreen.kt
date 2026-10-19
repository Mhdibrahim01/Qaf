package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ai.BatchRunPhase
import com.example.data.ai.BatchTargetOption
import com.example.data.ai.BatchTranslateUiState
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderDark
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevatedDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BatchTranslateScreen(
    state: BatchTranslateUiState,
    onBack: () -> Unit,
    onSelectDeck: (String) -> Unit,
    onSelectTargetCount: (Int) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onRetryFailed: () -> Unit,
    onToggleAllowAi: (Boolean) -> Unit,
    onChangeDailyLimit: (Int) -> Unit,
    onOpenApiKeyDialog: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        onStart()
    }

    val triggerStartWithPermissionCheck = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onStart()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(horizontal = 20.dp)
            .testTag("batch_translate_screen"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with Back button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(SurfaceElevatedDark)
                            .testTag("batch_translate_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Data Check",
                            tint = TextPrimary
                        )
                    }
                    Column {
                        Text(
                            text = "Batch Translate",
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "25 words/request • Favorites & A1→C2 first • Foreground service",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Active Engine Mode & API Key Status Card
        item {
            val context = androidx.compose.ui.platform.LocalContext.current
            val trigger by com.example.data.ai.GeminiApiKeyManager.apiKeyUpdatedTrigger.collectAsState()
            val keySource = remember(state, trigger) {
                com.example.data.ai.GeminiApiKeyManager.getKeySource(context)
            }
            val isCustom = remember(state, trigger) {
                com.example.data.ai.GeminiApiKeyManager.hasCustomKey(context)
            }
            val activeEngineMode = remember(state, trigger) {
                com.example.data.ai.GeminiApiKeyManager.getEngineMode(context)
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = SurfaceDark,
                border = BorderStroke(
                    1.dp,
                    if (isCustom || activeEngineMode == com.example.data.ai.TranslationEngineMode.FREE_API_ONLY)
                        EmeraldAccent.copy(alpha = 0.5f)
                    else BorderDark
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isCustom) EmeraldAccent.copy(alpha = 0.15f) else GoldAccent.copy(alpha = 0.15f),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = if (isCustom) EmeraldAccent else GoldAccent,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Engine: $keySource",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isCustom || activeEngineMode == com.example.data.ai.TranslationEngineMode.FREE_API_ONLY)
                                        EmeraldAccent else TextPrimary
                                )
                                Text(
                                    text = "Multi-key rotation (Gemini/Groq) + Free API fallback",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = onOpenApiKeyDialog,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.7f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("batch_header_change_key_button")
                        ) {
                            Text("Keys & Mode", style = MaterialTheme.typography.labelSmall, color = GoldAccent, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Quick Engine Mode Toggle Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BackgroundDark, RoundedCornerShape(10.dp))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        com.example.data.ai.TranslationEngineMode.entries.forEach { mode ->
                            val isSelected = activeEngineMode == mode
                            Surface(
                                color = if (isSelected) GoldAccent.copy(alpha = 0.16f) else Color.Transparent,
                                border = if (isSelected) BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f)) else null,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        com.example.data.ai.GeminiApiKeyManager.setEngineMode(context, mode)
                                    }
                                    .testTag("batch_engine_mode_${mode.id}")
                            ) {
                                Text(
                                    text = mode.shortBadge,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) GoldAccent else TextSecondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 1. Choose a Deck (or "All Decks") showing how many empty words each deck has
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
                    Text(
                        text = "1. Choose Deck",
                        style = MaterialTheme.typography.titleMedium,
                        color = GoldAccent,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Select a specific deck or translate across all decks. Empty Arabic word counts are shown per deck:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    // "All Decks" row
                    DeckSelectionRow(
                        title = "All Decks",
                        emptyCount = state.totalEmptyAcrossDecks,
                        totalCount = state.deckStats.sumOf { it.totalRows },
                        selected = state.selectedDeck.isBlank(),
                        enabled = !state.isRunning,
                        onClick = { onSelectDeck("") },
                        testTag = "deck_option_all"
                    )

                    state.deckStats.forEach { deckStat ->
                        DeckSelectionRow(
                            title = deckStat.deck,
                            emptyCount = deckStat.emptyCount,
                            totalCount = deckStat.totalRows,
                            selected = state.selectedDeck == deckStat.deck,
                            enabled = !state.isRunning,
                            onClick = { onSelectDeck(deckStat.deck) },
                            testTag = "deck_option_${deckStat.deck}"
                        )
                    }
                }
            }
        }

        // 2. Choose how many empty words to translate: 25, 100, 500, or All empty (plus 50 for quick testing)
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
                    Text(
                        text = "2. How Many Empty Words to Translate",
                        style = MaterialTheme.typography.titleMedium,
                        color = GoldAccent,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Processed in batches of 25 per Gemini request. Entries sharing (text, pos) share one translation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val options = listOf(
                            BatchTargetOption.WORDS_25,
                            BatchTargetOption.WORDS_50,
                            BatchTargetOption.WORDS_100,
                            BatchTargetOption.WORDS_500,
                            BatchTargetOption.ALL_EMPTY
                        )
                        options.forEach { option ->
                            val isSelected = state.selectedTargetCount == option.count
                            val tagSuffix = if (option == BatchTargetOption.ALL_EMPTY) "all" else option.count.toString()
                            FilterChip(
                                selected = isSelected,
                                onClick = { if (!state.isRunning) onSelectTargetCount(option.count) },
                                enabled = !state.isRunning,
                                label = {
                                    Text(
                                        text = option.label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldAccent.copy(alpha = 0.2f),
                                    selectedLabelColor = GoldAccent
                                ),
                                modifier = Modifier.testTag("batch_target_$tagSuffix")
                            )
                        }
                    }
                }
            }
        }

        // 3. Progress & Controls Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceElevatedDark),
                border = BorderStroke(
                    1.dp,
                    when (state.phase) {
                        BatchRunPhase.RUNNING -> GoldAccent.copy(alpha = 0.55f)
                        BatchRunPhase.STOPPED_ERROR, BatchRunPhase.BLOCKED -> CoralAccent.copy(alpha = 0.6f)
                        BatchRunPhase.COMPLETED -> EmeraldAccent.copy(alpha = 0.55f)
                        else -> BorderDark
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = GoldAccent
                            )
                            Text(
                                text = "Progress & Controls",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SurfaceDark
                        ) {
                            Text(
                                text = "${state.doneCount} / ${state.totalCount}",
                                style = MaterialTheme.typography.labelLarge,
                                color = GoldAccent,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                    .testTag("batch_done_total_text")
                            )
                        }
                    }

                    val progressFraction = if (state.totalCount > 0) {
                        (state.doneCount.toFloat() / state.totalCount.toFloat()).coerceIn(0f, 1f)
                    } else {
                        0f
                    }

                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .testTag("batch_progress_bar"),
                        color = GoldAccent,
                        trackColor = SurfaceDark
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Batches: ${state.batchesCompleted} / ${state.totalBatches}",
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldAccent,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.testTag("batch_batches_completed_text")
                        )
                        Text(
                            text = formatEstimatedTimeLeft(state.estimatedSecondsLeft, state.isRunning),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            modifier = Modifier.testTag("batch_eta_text")
                        )
                    }

                    if (state.statusMessage.isNotBlank()) {
                        Text(
                            text = state.statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.testTag("batch_status_message")
                        )
                    }

                    if (!state.errorMessage.isNullOrBlank()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(CoralAccent.copy(alpha = 0.14f))
                                .padding(12.dp)
                                .testTag("batch_error_banner"),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = CoralAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = state.errorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CoralAccent,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            OutlinedButton(
                                onClick = onOpenApiKeyDialog,
                                border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .testTag("btn_error_change_api_key")
                            ) {
                                Icon(Icons.Default.Key, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Change Google API Key", color = GoldAccent, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    // Control buttons: Start, Pause/Resume, Stop
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (!state.isRunning && !state.canResume) {
                            Button(
                                onClick = triggerStartWithPermissionCheck,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldAccent,
                                    contentColor = BackgroundDark
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("batch_start_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Start Batch Translate", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            // Pause / Resume button
                            Button(
                                onClick = {
                                    if (state.isRunning) onPause() else onResume()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (state.isRunning) GoldAccent else EmeraldAccent,
                                    contentColor = BackgroundDark
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("batch_pause_resume_button")
                            ) {
                                Icon(
                                    imageVector = if (state.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (state.isRunning) "Pause" else "Resume",
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Stop button
                            OutlinedButton(
                                onClick = onStop,
                                border = BorderStroke(1.dp, CoralAccent),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralAccent),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("batch_stop_button")
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Stop", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // 4. Summary & Retry Failed Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("batch_summary_card"),
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
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Run Summary",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryStatBox(
                            label = "Translated",
                            value = state.translatedCount.toString(),
                            color = EmeraldAccent,
                            testTag = "summary_translated_count",
                            modifier = Modifier.weight(1f)
                        )
                        SummaryStatBox(
                            label = "Skipped",
                            value = state.skippedCount.toString(),
                            color = GoldAccent,
                            testTag = "summary_skipped_count",
                            modifier = Modifier.weight(1f)
                        )
                        SummaryStatBox(
                            label = "Failed",
                            value = state.failedCount.toString(),
                            color = if (state.failedCount > 0) CoralAccent else TextMuted,
                            testTag = "summary_failed_count",
                            modifier = Modifier.weight(1f)
                        )
                        SummaryStatBox(
                            label = "Reqs Today",
                            value = "${state.requestsUsedToday}/${state.dailyRequestLimit}",
                            color = GoldAccent,
                            testTag = "summary_requests_today",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (state.failedEntryIds.isNotEmpty()) {
                        Button(
                            onClick = onRetryFailed,
                            enabled = !state.isRunning,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldAccent,
                                contentColor = BackgroundDark
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("batch_retry_failed_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Retry Failed (${state.failedEntryIds.size} words)",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 5. Safety & Rate Limit Settings ("Allow AI" switch & Daily Request Limit, default 40)
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
                    Text(
                        text = "Safety & Rate Limits",
                        style = MaterialTheme.typography.titleMedium,
                        color = GoldAccent,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Allow AI",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Master switch for Gemini API translation requests",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = state.allowAi,
                            onCheckedChange = onToggleAllowAi,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BackgroundDark,
                                checkedTrackColor = GoldAccent
                            ),
                            modifier = Modifier.testTag("batch_allow_ai_switch")
                        )
                    }

                    HorizontalDivider(color = BorderDark)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Daily Request Limit",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Default 40 requests/day • ~5s delay between requests • Auto-stop after 5 failures",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onChangeDailyLimit((state.dailyRequestLimit - 10).coerceAtLeast(5)) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("batch_limit_minus_button")
                            ) {
                                Text("-10")
                            }
                            Text(
                                text = state.dailyRequestLimit.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                color = GoldAccent,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("batch_daily_limit_value")
                            )
                            OutlinedButton(
                                onClick = { onChangeDailyLimit(state.dailyRequestLimit + 10) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("batch_limit_plus_button")
                            ) {
                                Text("+10")
                            }
                        }
                    }

                    HorizontalDivider(color = BorderDark)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Google Gemini API Key",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Configure custom key to bypass rate limits (429)",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        OutlinedButton(
                            onClick = onOpenApiKeyDialog,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("batch_change_api_key_button")
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Change Key", color = GoldAccent, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeckSelectionRow(
    title: String,
    emptyCount: Int,
    totalCount: Int,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) GoldAccent.copy(alpha = 0.14f) else SurfaceElevatedDark,
        border = BorderStroke(
            1.dp,
            if (selected) GoldAccent else BorderDark
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (selected) GoldAccent else TextPrimary,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
                Text(
                    text = "Total rows: $totalCount",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (emptyCount > 0) GoldAccent.copy(alpha = 0.16f) else EmeraldAccent.copy(alpha = 0.16f)
            ) {
                Text(
                    text = "$emptyCount empty",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (emptyCount > 0) GoldAccent else EmeraldAccent,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun SummaryStatBox(
    label: String,
    value: String,
    color: Color,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceElevatedDark)
            .padding(vertical = 10.dp, horizontal = 6.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}

private fun formatEstimatedTimeLeft(seconds: Long, isRunning: Boolean): String {
    if (!isRunning || seconds <= 0L) return "ETA: --"
    val mins = seconds / 60
    val secs = seconds % 60
    return if (mins > 0) {
        "ETA: ~${mins}m ${secs}s left"
    } else {
        "ETA: ~${secs}s left"
    }
}
