package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.audio.AudioPlayerManager
import com.example.data.local.SentenceEntity
import com.example.data.sentences.DictationDiffResult
import com.example.data.sentences.DiffTokenStatus
import com.example.data.sentences.SentencePracticeUtils
import com.example.data.sentences.SentenceSortOrder
import com.example.ui.components.ArabicText
import com.example.ui.components.GoldBadge
import com.example.ui.components.SlowAudioTurtleButton
import com.example.ui.theme.AmberAccent
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SentencesTab(val titleEn: String, val titleAr: String, val testTag: String) {
    FAVORITES("Favorites", "المفضلة", "sentences_tab_favorites"),
    DICTATION("Dictation", "الإملاء", "sentences_tab_dictation"),
    SPEAKING("Speaking", "التحدث", "sentences_tab_speaking")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SentencesScreen(
    savedSentences: List<SentenceEntity>,
    savedSentenceKeys: Set<String>,
    dictionarySampleExamples: List<SentenceEntity>,
    sortOrder: SentenceSortOrder,
    showTranslation: Boolean,
    ttsSpeechRate: Float,
    dictationIgnorePunctuation: Boolean,
    autoPlayPracticeAudio: Boolean,
    currentlyPlayingText: String?,
    explainingSentenceIds: Set<Long>,
    aiErrorMessage: String?,
    onPlaySentenceAudio: (String, String) -> Unit,
    onSpeakSentenceTts: (String, Float?) -> Unit,
    onToggleSaveSentence: (String, String, String) -> Unit,
    onDeleteSentence: (Long) -> Unit,
    onExplainSentenceWithAi: (Long) -> Unit,
    onToggleSortOrder: () -> Unit,
    onToggleShowTranslation: () -> Unit,
    onUpdateTtsSpeechRate: (Float) -> Unit,
    onUpdateDictationIgnorePunctuation: (Boolean) -> Unit,
    onUpdateAutoPlayPracticeAudio: (Boolean) -> Unit,
    onNavigateToWordBooks: () -> Unit,
    onNavigateToFlashcards: () -> Unit,
    onRecordSentencePracticeAttempt: (String, Int) -> Unit = { _, _ -> },
    quietModeEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(SentencesTab.FAVORITES) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showDictionaryPicker by remember { mutableStateOf(false) }
    var copiedFeedbackText by remember { mutableStateOf<String?>(null) }

    // Track which sentence IDs have their AI explanation expanded in the UI
    val expandedExplanationMap = remember { mutableStateMapOf<Long, Boolean>() }

    // Practice index shared or independent for Dictation / Speaking
    var practiceIndex by remember { mutableIntStateOf(0) }
    val clampedPracticeIndex = if (savedSentences.isEmpty()) {
        0
    } else {
        practiceIndex.coerceIn(0, savedSentences.size - 1)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .testTag("screen_sentences"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Sentences Top Bar: Practice, Sort by time, Show/Hide translation, Settings
            SentencesTopBar(
                activeTab = activeTab,
                sortOrder = sortOrder,
                showTranslation = showTranslation,
                onCyclePracticeMode = {
                    activeTab = when (activeTab) {
                        SentencesTab.FAVORITES -> SentencesTab.DICTATION
                        SentencesTab.DICTATION -> SentencesTab.SPEAKING
                        SentencesTab.SPEAKING -> SentencesTab.FAVORITES
                    }
                },
                onToggleSortOrder = onToggleSortOrder,
                onToggleShowTranslation = onToggleShowTranslation,
                onOpenSettings = { showSettingsSheet = true }
            )

            // 2. Tabs: Favorites (count), Dictation, Speaking
            SentencesTabRow(
                activeTab = activeTab,
                favoritesCount = savedSentences.size,
                onSelectTab = { activeTab = it }
            )

            // Optional Clipboard / AI status banner
            if (copiedFeedbackText != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = EmeraldAccent.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, EmeraldAccent)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = copiedFeedbackText ?: "",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = EmeraldAccent
                        )
                        TextButton(onClick = { copiedFeedbackText = null }) {
                            Text("OK", color = EmeraldAccent)
                        }
                    }
                }
            }

            if (!aiErrorMessage.isNullOrBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = CoralAccent.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CoralAccent)
                ) {
                    Text(
                        text = aiErrorMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = CoralAccent,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // 3. Active Tab Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (activeTab) {
                    SentencesTab.FAVORITES -> {
                        FavoritesSentencesTabContent(
                            savedSentences = savedSentences,
                            savedSentenceKeys = savedSentenceKeys,
                            dictionarySampleExamples = dictionarySampleExamples,
                            showTranslation = showTranslation,
                            currentlyPlayingText = currentlyPlayingText,
                            explainingSentenceIds = explainingSentenceIds,
                            expandedExplanationMap = expandedExplanationMap,
                            showDictionaryPicker = showDictionaryPicker,
                            onToggleDictionaryPicker = { showDictionaryPicker = !showDictionaryPicker },
                            onPlaySentence = { sentence ->
                                onPlaySentenceAudio(sentence.englishText, sentence.audioUrl)
                            },
                            onPlaySentenceSlow = { sentence ->
                                onSpeakSentenceTts(sentence.englishText, AudioPlayerManager.SLOW_TURTLE_RATE)
                            },
                            onCopySentence = { sentence ->
                                val textToCopy = if (showTranslation && sentence.arabicText.isNotBlank()) {
                                    "${sentence.englishText}\n${sentence.arabicText}"
                                } else {
                                    sentence.englishText
                                }
                                copyTextToClipboard(context, textToCopy)
                                copiedFeedbackText = "Copied: \"${sentence.englishText.take(40)}...\""
                            },
                            onDeleteSentence = onDeleteSentence,
                            onTapAiExplainSparkle = { sentence ->
                                val currentlyExpanded = expandedExplanationMap[sentence.id] == true
                                if (sentence.aiExplanation.isNotBlank()) {
                                    // Already saved permanently in DB -> never request twice, just toggle visibility
                                    expandedExplanationMap[sentence.id] = !currentlyExpanded
                                } else {
                                    expandedExplanationMap[sentence.id] = true
                                    onExplainSentenceWithAi(sentence.id)
                                }
                            },
                            onToggleSaveExampleSentence = onToggleSaveSentence,
                            onNavigateToFlashcards = onNavigateToFlashcards,
                            onNavigateToWordBooks = onNavigateToWordBooks
                        )
                    }

                    SentencesTab.DICTATION -> {
                        DictationTabContent(
                            savedSentences = savedSentences,
                            currentIndex = clampedPracticeIndex,
                            showTranslation = showTranslation,
                            ignorePunctuation = dictationIgnorePunctuation,
                            autoPlayAudio = autoPlayPracticeAudio,
                            currentlyPlayingText = currentlyPlayingText,
                            onIndexChange = { practiceIndex = it },
                            onPlayNormalTts = { text -> onSpeakSentenceTts(text, null) },
                            onPlaySlowTts = { text -> onSpeakSentenceTts(text, AudioPlayerManager.SLOW_TURTLE_RATE) },
                            onRecordAttempt = onRecordSentencePracticeAttempt,
                            onSwitchToFavorites = { activeTab = SentencesTab.FAVORITES }
                        )
                    }

                    SentencesTab.SPEAKING -> {
                        SpeakingTabContent(
                            savedSentences = savedSentences,
                            currentIndex = clampedPracticeIndex,
                            showTranslation = showTranslation,
                            ignorePunctuation = dictationIgnorePunctuation,
                            currentlyPlayingText = currentlyPlayingText,
                            onIndexChange = { practiceIndex = it },
                            onPlayNormalTts = { text -> onSpeakSentenceTts(text, null) },
                            onPlaySlowTts = { text -> onSpeakSentenceTts(text, AudioPlayerManager.SLOW_TURTLE_RATE) },
                            onRecordAttempt = onRecordSentencePracticeAttempt,
                            quietModeEnabled = quietModeEnabled,
                            onSwitchToFavorites = { activeTab = SentencesTab.FAVORITES }
                        )
                    }
                }
            }
        }
    }

    if (showSettingsSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            sheetState = sheetState,
            containerColor = SurfaceDark,
            contentColor = TextPrimary
        ) {
            SentencesSettingsSheetContent(
                ttsSpeechRate = ttsSpeechRate,
                ignorePunctuation = dictationIgnorePunctuation,
                autoPlayPracticeAudio = autoPlayPracticeAudio,
                showTranslation = showTranslation,
                sortOrder = sortOrder,
                onUpdateSpeechRate = onUpdateTtsSpeechRate,
                onUpdateIgnorePunctuation = onUpdateDictationIgnorePunctuation,
                onUpdateAutoPlay = onUpdateAutoPlayPracticeAudio,
                onToggleShowTranslation = onToggleShowTranslation,
                onToggleSortOrder = onToggleSortOrder,
                onClose = { showSettingsSheet = false }
            )
        }
    }
}

@Composable
private fun SentencesTopBar(
    activeTab: SentencesTab,
    sortOrder: SentenceSortOrder,
    showTranslation: Boolean,
    onCyclePracticeMode: () -> Unit,
    onToggleSortOrder: () -> Unit,
    onToggleShowTranslation: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceDark,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, BorderDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Practice button
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onCyclePracticeMode)
                    .testTag("sentences_top_practice_button"),
                color = if (activeTab != SentencesTab.FAVORITES) GoldAccent else GoldAccentDim,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, GoldAccentBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = "Practice",
                        tint = if (activeTab != SentencesTab.FAVORITES) BackgroundDark else GoldAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Practice",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (activeTab != SentencesTab.FAVORITES) BackgroundDark else GoldAccent
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 2. Sort by time button
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onToggleSortOrder)
                        .testTag("sentences_top_sort_button"),
                    color = SurfaceElevatedDark,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BorderDark)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort by time",
                            tint = GoldAccent,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (sortOrder == SentenceSortOrder.NEWEST) "Newest" else "Oldest",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary
                        )
                    }
                }

                // 3. Show / hide translation button
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onToggleShowTranslation)
                        .testTag("sentences_top_translation_toggle"),
                    color = if (showTranslation) GoldAccentDim else SurfaceElevatedDark,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (showTranslation) GoldAccentBorder else BorderDark)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (showTranslation) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Show or hide Arabic translation",
                            tint = if (showTranslation) GoldAccent else TextMuted,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (showTranslation) "AR On" else "AR Off",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (showTranslation) GoldAccent else TextSecondary
                        )
                    }
                }

                // 4. Settings icon button
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("sentences_top_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Sentences settings",
                        tint = TextSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SentencesTabRow(
    activeTab: SentencesTab,
    favoritesCount: Int,
    onSelectTab: (SentencesTab) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SentencesTab.entries.forEach { tab ->
            val isSelected = tab == activeTab
            val label = if (tab == SentencesTab.FAVORITES) {
                "Favorites ($favoritesCount)"
            } else {
                tab.titleEn
            }
            val icon = when (tab) {
                SentencesTab.FAVORITES -> Icons.Default.Bookmark
                SentencesTab.DICTATION -> Icons.Default.Keyboard
                SentencesTab.SPEAKING -> Icons.Default.Mic
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelectTab(tab) }
                    .testTag(tab.testTag),
                color = if (isSelected) GoldAccentDim else SurfaceDark,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isSelected) GoldAccent else BorderDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = tab.titleEn,
                        tint = if (isSelected) GoldAccent else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSelected) GoldAccent else TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoritesSentencesTabContent(
    savedSentences: List<SentenceEntity>,
    savedSentenceKeys: Set<String>,
    dictionarySampleExamples: List<SentenceEntity>,
    showTranslation: Boolean,
    currentlyPlayingText: String?,
    explainingSentenceIds: Set<Long>,
    expandedExplanationMap: MutableMap<Long, Boolean>,
    showDictionaryPicker: Boolean,
    onToggleDictionaryPicker: () -> Unit,
    onPlaySentence: (SentenceEntity) -> Unit,
    onPlaySentenceSlow: (SentenceEntity) -> Unit = {},
    onCopySentence: (SentenceEntity) -> Unit,
    onDeleteSentence: (Long) -> Unit,
    onTapAiExplainSparkle: (SentenceEntity) -> Unit,
    onToggleSaveExampleSentence: (String, String, String) -> Unit,
    onNavigateToFlashcards: () -> Unit,
    onNavigateToWordBooks: () -> Unit
) {
    var pendingDeleteSentence by remember { mutableStateOf<SentenceEntity?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("sentences_favorites_list"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Quick action bar to browse/bookmark real example sentences from the dictionary
        item(key = "sentences_save_example_bar", contentType = "save_example_bar") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceDark,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, BorderDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Save Example Sentences",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = "Long-press or tap the bookmark icon on any example in Flashcards, Word Books, or below.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    OutlinedButton(
                        onClick = onToggleDictionaryPicker,
                        border = BorderStroke(1.dp, GoldAccentBorder),
                        modifier = Modifier.testTag("browse_dictionary_examples_button")
                    ) {
                        Text(
                            text = if (showDictionaryPicker) "Hide Examples" else "Browse Examples",
                            color = GoldAccent,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        if (showDictionaryPicker && dictionarySampleExamples.isNotEmpty()) {
            item(key = "dictionary_examples_picker", contentType = "dictionary_picker") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dictionary_examples_picker_card"),
                    color = SurfaceDark,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, GoldAccentBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Real Examples from Oxford / Idioms Database (Tap bookmark or long-press)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = GoldAccent
                        )
                        dictionarySampleExamples.take(10).forEachIndexed { idx, sample ->
                            val isSaved = sample.englishText.trim().lowercase() in savedSentenceKeys
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .combinedClickable(
                                        onClick = {},
                                        onLongClick = {
                                            onToggleSaveExampleSentence(
                                                sample.englishText,
                                                sample.arabicText,
                                                sample.sourceWord
                                            )
                                        }
                                    )
                                    .testTag("picker_example_row_$idx"),
                                color = SurfaceElevatedDark,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (isSaved) GoldAccent else BorderDark)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(
                                            text = highlightSentenceWordInGold(sample.englishText, sample.sourceWord),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                textDirection = TextDirection.Ltr,
                                                textAlign = TextAlign.Left
                                            ),
                                            color = TextPrimary
                                        )
                                        if (showTranslation && sample.arabicText.isNotBlank()) {
                                            ArabicText(
                                                text = sample.arabicText,
                                                style = ArabicTypography.bodyMedium,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            onToggleSaveExampleSentence(
                                                sample.englishText,
                                                sample.arabicText,
                                                sample.sourceWord
                                            )
                                        },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("picker_bookmark_button_$idx")
                                    ) {
                                        Icon(
                                            imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                            contentDescription = if (isSaved) "Unsave sentence" else "Save sentence",
                                            tint = if (isSaved) GoldAccent else TextMuted,
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

        if (savedSentences.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sentences_empty_state"),
                    color = SurfaceDark,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, BorderDark)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "No Saved Sentences Yet",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        ArabicText(
                            text = "لم تقم بحفظ أي جمل بعد — اضغط مطولاً أو اضغط على أيقونة الإشارة المرجعية لحفظ أي جملة مع ترجمتها العربية.",
                            style = ArabicTypography.bodyMedium,
                            color = TextSecondary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onToggleDictionaryPicker,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldAccent,
                                    contentColor = BackgroundDark
                                ),
                                modifier = Modifier.testTag("empty_state_browse_examples_button")
                            ) {
                                Text("Pick from Examples", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = onNavigateToFlashcards,
                                border = BorderStroke(1.dp, GoldAccentBorder)
                            ) {
                                Text("Open Flashcards", color = GoldAccent)
                            }
                        }
                    }
                }
            }
        } else {
            items(
                items = savedSentences,
                key = { it.id },
                contentType = { "saved_sentence_card" }
            ) { sentence ->
                val isPlaying = currentlyPlayingText == sentence.englishText
                val isExplaining = sentence.id in explainingSentenceIds
                val isExplanationExpanded = expandedExplanationMap[sentence.id] == true ||
                    (sentence.aiExplanation.isNotBlank() && expandedExplanationMap[sentence.id] != false)

                SavedSentenceRowCard(
                    sentence = sentence,
                    showTranslation = showTranslation,
                    isPlaying = isPlaying,
                    isExplaining = isExplaining,
                    isExplanationExpanded = isExplanationExpanded,
                    onPlay = { onPlaySentence(sentence) },
                    onPlaySlow = { onPlaySentenceSlow(sentence) },
                    onCopy = { onCopySentence(sentence) },
                    onDelete = { pendingDeleteSentence = sentence },
                    onTapAiExplain = { onTapAiExplainSparkle(sentence) }
                )
            }
        }
    }

    pendingDeleteSentence?.let { sentenceToDelete ->
        AlertDialog(
            onDismissRequest = { pendingDeleteSentence = null },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "Delete Saved Sentence? / حذف الجملة",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Are you sure you want to remove this sentence from your saved collection?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "\"${sentenceToDelete.englishText}\"",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = GoldAccent
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSentence(sentenceToDelete.id)
                        pendingDeleteSentence = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoralAccent,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_confirm_delete_sentence")
                ) {
                    Text("Delete / حذف", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingDeleteSentence = null },
                    modifier = Modifier.testTag("btn_cancel_delete_sentence")
                ) {
                    Text("Cancel / إلغاء", color = TextMuted)
                }
            }
        )
    }
}

@Composable
private fun SavedSentenceRowCard(
    sentence: SentenceEntity,
    showTranslation: Boolean,
    isPlaying: Boolean,
    isExplaining: Boolean,
    isExplanationExpanded: Boolean,
    onPlay: () -> Unit,
    onPlaySlow: () -> Unit = {},
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onTapAiExplain: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.US) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("saved_sentence_card_${sentence.id}"),
        color = SurfaceDark,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isPlaying) GoldAccent else BorderDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header row: source word chip + timestamp + 4 row action icons (play, copy, delete, AI sparkle)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (sentence.sourceWord.isNotBlank()) {
                        GoldBadge(text = sentence.sourceWord)
                    }
                    Text(
                        text = dateFormat.format(Date(sentence.createdAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }

                // Row icons: Play, Slow, Copy, AI Explain, and Delete (far right)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onPlay,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("sentence_play_${sentence.id}")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Play sentence audio",
                            tint = if (isPlaying) GoldAccent else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    SlowAudioTurtleButton(
                        onClick = onPlaySlow,
                        showSpeedLabel = false,
                        testTag = "sentence_play_slow_${sentence.id}"
                    )

                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("sentence_copy_${sentence.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy sentence",
                            tint = TextSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    IconButton(
                        onClick = onTapAiExplain,
                        enabled = !isExplaining,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("sentence_ai_explain_${sentence.id}")
                    ) {
                        if (isExplaining) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = GoldAccent
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI explain grammar and vocabulary",
                                tint = if (sentence.aiExplanation.isNotBlank()) GoldAccent else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("sentence_delete_${sentence.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete sentence",
                            tint = CoralAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // English sentence with target word highlighted in gold
            Text(
                text = highlightSentenceWordInGold(sentence.englishText, sentence.sourceWord),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    textDirection = TextDirection.Ltr,
                    textAlign = TextAlign.Left
                ),
                color = TextPrimary,
                modifier = Modifier.testTag("sentence_english_text_${sentence.id}")
            )

            // Arabic translation from examples_ar (hidden if showTranslation is false or if empty)
            if (showTranslation && sentence.arabicText.isNotBlank()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sentence_arabic_text_${sentence.id}"),
                    color = SurfaceElevatedDark,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BorderDark)
                ) {
                    Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        ArabicText(
                            text = sentence.arabicText,
                            style = ArabicTypography.bodyLarge,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Permanently cached Gemini Arabic grammar & vocabulary explanation
            AnimatedVisibility(visible = isExplanationExpanded && (sentence.aiExplanation.isNotBlank() || isExplaining)) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sentence_ai_explanation_box_${sentence.id}"),
                    color = SurfaceElevatedDark,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, GoldAccentBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "AI Grammar & Vocabulary • شرح القواعد والمفردات",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = GoldAccent
                                )
                            }
                            if (sentence.aiExplanation.isNotBlank()) {
                                GoldBadge(text = "Saved Forever")
                            }
                        }

                        if (isExplaining && sentence.aiExplanation.isBlank()) {
                            Text(
                                text = "Generating Arabic grammar & vocabulary explanation with Gemini...",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        } else if (sentence.aiExplanation.isNotBlank()) {
                            ArabicText(
                                text = sentence.aiExplanation,
                                style = ArabicTypography.bodyMedium,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DictationTabContent(
    savedSentences: List<SentenceEntity>,
    currentIndex: Int,
    showTranslation: Boolean,
    ignorePunctuation: Boolean,
    autoPlayAudio: Boolean,
    currentlyPlayingText: String?,
    onIndexChange: (Int) -> Unit,
    onPlayNormalTts: (String) -> Unit,
    onPlaySlowTts: (String) -> Unit,
    onRecordAttempt: (String, Int) -> Unit = { _, _ -> },
    onSwitchToFavorites: () -> Unit
) {
    if (savedSentences.isEmpty()) {
        EmptyPracticeStateCard(
            title = "No Saved Sentences for Dictation",
            subtitleAr = "احفظ ٣ جمل على الأقل من البطاقات أو القاموس لبدء تدريب الإملاء بدون إنترنت.",
            onSwitchToFavorites = onSwitchToFavorites
        )
        return
    }

    val currentSentence = savedSentences[currentIndex.coerceIn(0, savedSentences.size - 1)]
    val focusManager = LocalFocusManager.current
    var typedInput by remember(currentSentence.id) { mutableStateOf("") }
    var diffResult by remember(currentSentence.id) { mutableStateOf<DictationDiffResult?>(null) }
    var showEnglishHint by remember(currentSentence.id) { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    LaunchedEffect(currentSentence.id, autoPlayAudio) {
        if (autoPlayAudio) {
            onPlayNormalTts(currentSentence.englishText)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .testTag("sentences_dictation_content"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Card counter & navigation header
        PracticeNavigationHeader(
            modeTitle = "Offline TTS Dictation • الإملاء الصوتي",
            currentIndex = currentIndex,
            totalCount = savedSentences.size,
            sourceWord = currentSentence.sourceWord,
            onPrev = { if (currentIndex > 0) onIndexChange(currentIndex - 1) },
            onNext = { if (currentIndex + 1 < savedSentences.size) onIndexChange(currentIndex + 1) }
        )

        // Audio Prompt Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SurfaceDark,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, GoldAccentBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onPlayNormalTts(currentSentence.englishText) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldAccent,
                            contentColor = BackgroundDark
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dictation_play_tts_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Play sentence with TextToSpeech",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (currentlyPlayingText == currentSentence.englishText) "Playing..." else "Listen (TTS)",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = { onPlaySlowTts(currentSentence.englishText) },
                        border = BorderStroke(1.dp, GoldAccentBorder),
                        modifier = Modifier.testTag("dictation_play_slow_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SlowMotionVideo,
                            contentDescription = "Play slow TextToSpeech",
                            tint = GoldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("🐢 Slow 0.6x", color = GoldAccent, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Arabic meaning prompt (from examples_ar)
                if (showTranslation && currentSentence.arabicText.isNotBlank()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = SurfaceElevatedDark,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BorderDark)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Arabic Translation Prompt (examples_ar)",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldAccent
                            )
                            ArabicText(
                                text = currentSentence.arabicText,
                                style = ArabicTypography.bodyLarge,
                                color = TextPrimary
                            )
                        }
                    }
                }

                // Peek English hint toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showEnglishHint = !showEnglishHint },
                        modifier = Modifier.testTag("dictation_peek_button")
                    ) {
                        Icon(
                            imageVector = if (showEnglishHint) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (showEnglishHint) "Hide Target Sentence" else "Peek Target Sentence",
                            color = TextSecondary
                        )
                    }
                }

                if (showEnglishHint) {
                    Text(
                        text = currentSentence.englishText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = GoldAccent,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                // Dictation Input Field
                OutlinedTextField(
                    value = typedInput,
                    onValueChange = { typedInput = it },
                    label = { Text("Type the English sentence you heard...") },
                    placeholder = { Text("Type here and tap Check Dictation...", color = TextMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dictation_input_field"),
                    minLines = 2,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (typedInput.isNotBlank()) {
                                val computed = SentencePracticeUtils.computeSentenceDiff(
                                    expected = currentSentence.englishText,
                                    actual = typedInput,
                                    ignorePunctuation = ignorePunctuation
                                )
                                diffResult = computed
                                val itemId = if (currentSentence.id > 0L) {
                                    "sentence:${currentSentence.id}"
                                } else {
                                    currentSentence.englishText
                                }
                                onRecordAttempt(itemId, computed.accuracyPercent)
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldAccent,
                        unfocusedBorderColor = BorderDark,
                        focusedContainerColor = SurfaceElevatedDark,
                        unfocusedContainerColor = SurfaceElevatedDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = GoldAccent,
                        unfocusedLabelColor = TextSecondary
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val computed = SentencePracticeUtils.computeSentenceDiff(
                                expected = currentSentence.englishText,
                                actual = typedInput,
                                ignorePunctuation = ignorePunctuation
                            )
                            diffResult = computed
                            val itemId = if (currentSentence.id > 0L) {
                                "sentence:${currentSentence.id}"
                            } else {
                                currentSentence.englishText
                            }
                            onRecordAttempt(itemId, computed.accuracyPercent)
                        },
                        enabled = typedInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldAccent,
                            contentColor = BackgroundDark
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dictation_check_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Check Dictation", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            typedInput = ""
                            diffResult = null
                        },
                        border = BorderStroke(1.dp, BorderDark),
                        modifier = Modifier.testTag("dictation_clear_button")
                    ) {
                        Text("Reset", color = TextSecondary)
                    }
                }
            }
        }

        // Visual Diff Result Card
        val currentDiff = diffResult
        if (currentDiff != null) {
            SentenceDiffResultCard(
                diff = currentDiff,
                testTag = "dictation_diff_card"
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpeakingTabContent(
    savedSentences: List<SentenceEntity>,
    currentIndex: Int,
    showTranslation: Boolean,
    ignorePunctuation: Boolean,
    currentlyPlayingText: String?,
    onIndexChange: (Int) -> Unit,
    onPlayNormalTts: (String) -> Unit,
    onPlaySlowTts: (String) -> Unit = { onPlayNormalTts(it) },
    onRecordAttempt: (String, Int) -> Unit = { _, _ -> },
    quietModeEnabled: Boolean = false,
    onSwitchToFavorites: () -> Unit
) {
    val context = LocalContext.current
    if (savedSentences.isEmpty()) {
        EmptyPracticeStateCard(
            title = "No Saved Sentences for Speaking",
            subtitleAr = "احفظ جملاً من البطاقات أو القاموس لتدريب النطق والمقارنة الصوتية.",
            onSwitchToFavorites = onSwitchToFavorites
        )
        return
    }

    val currentSentence = savedSentences[currentIndex.coerceIn(0, savedSentences.size - 1)]
    val scrollState = rememberScrollState()

    var quietTypedInput by remember(currentSentence.id) { mutableStateOf("") }
    val quietTokens = remember(currentSentence.id) {
        currentSentence.englishText
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .shuffled()
    }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var isListening by remember { mutableStateOf(false) }
    var preferOfflinePack by remember { mutableStateOf(true) }
    var spokenTranscript by remember(currentSentence.id) { mutableStateOf("") }
    var speakingDiff by remember(currentSentence.id) { mutableStateOf<DictationDiffResult?>(null) }
    var offlinePackWarningMessage by remember { mutableStateOf<String?>(null) }
    var recognitionStatusText by remember { mutableStateOf("Tap the microphone and read the sentence aloud.") }

    // Check if SpeechRecognizer is available & probe offline English pack support (suppressed in quiet mode)
    val isSpeechRecognitionAvailable = remember(context) {
        SpeechRecognizer.isRecognitionAvailable(context)
    }

    LaunchedEffect(context, isSpeechRecognitionAvailable, quietModeEnabled) {
        if (quietModeEnabled) return@LaunchedEffect
        if (!isSpeechRecognitionAvailable) {
            offlinePackWarningMessage =
                "No offline English speech pack or SpeechRecognizer service found on this device. Install Google Voice Typing / Offline English (US) speech pack in system settings."
        } else {
            checkOfflineEnglishSpeechPack(context) { hasOfflineEn ->
                if (!hasOfflineEn) {
                    offlinePackWarningMessage =
                        "Device has no offline English (en-US) speech recognition pack installed. Download 'English (US)' in Android Offline Speech Recognition settings for offline speaking practice."
                }
            }
        }
    }

    val speechRecognizerRef = remember { mutableStateOf<SpeechRecognizer?>(null) }

    DisposableEffect(context) {
        onDispose {
            runCatching {
                speechRecognizerRef.value?.destroy()
                speechRecognizerRef.value = null
            }
        }
    }

    val startListeningSession: () -> Unit = {
        if (!isSpeechRecognitionAvailable) {
            offlinePackWarningMessage =
                "No offline English speech recognition service is installed on this device."
        } else {
            runCatching {
                speechRecognizerRef.value?.destroy()
                val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                speechRecognizerRef.value = recognizer
                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                        recognitionStatusText = "Listening (English US)..."
                    }

                    override fun onBeginningOfSpeech() {
                        isListening = true
                    }

                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        isListening = false
                        recognitionStatusText = "Processing spoken sentence..."
                    }

                    override fun onError(error: Int) {
                        isListening = false
                        val errorMsg = mapSpeechRecognizerError(error, preferOfflinePack)
                        recognitionStatusText = errorMsg
                        if (
                            error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ||
                            error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED ||
                            error == SpeechRecognizer.ERROR_NETWORK ||
                            error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT ||
                            error == SpeechRecognizer.ERROR_SERVER_DISCONNECTED ||
                            error == SpeechRecognizer.ERROR_CLIENT
                        ) {
                            offlinePackWarningMessage =
                                "Offline English (en-US) speech pack is not installed or unavailable on this device ($errorMsg). Download the offline English voice pack in Android Settings -> Voice Input."
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        isListening = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val bestMatch = matches?.firstOrNull()?.trim().orEmpty()
                        spokenTranscript = bestMatch
                        if (bestMatch.isNotEmpty()) {
                            recognitionStatusText = "Heard: \"$bestMatch\""
                            val computed = SentencePracticeUtils.computeSentenceDiff(
                                expected = currentSentence.englishText,
                                actual = bestMatch,
                                ignorePunctuation = ignorePunctuation
                            )
                            speakingDiff = computed
                            val itemId = if (currentSentence.id > 0L) {
                                "sentence:${currentSentence.id}"
                            } else {
                                currentSentence.englishText
                            }
                            onRecordAttempt(itemId, computed.accuracyPercent)
                        } else {
                            recognitionStatusText = "No speech detected. Please try again."
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val partial = partialResults
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                            ?.trim()
                            .orEmpty()
                        if (partial.isNotEmpty()) {
                            spokenTranscript = partial
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, preferOfflinePack)
                }
                recognizer.startListening(intent)
            }.onFailure { e ->
                isListening = false
                offlinePackWarningMessage =
                    "Unable to start offline English speech recognition: ${e.message ?: "No offline English speech pack available"}."
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) {
            startListeningSession()
        } else {
            recognitionStatusText = "Microphone permission is required for Speaking practice."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .testTag("sentences_speaking_content"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PracticeNavigationHeader(
            modeTitle = "Speaking & Pronunciation • تدريب التحدث",
            currentIndex = currentIndex,
            totalCount = savedSentences.size,
            sourceWord = currentSentence.sourceWord,
            onPrev = { if (currentIndex > 0) onIndexChange(currentIndex - 1) },
            onNext = { if (currentIndex + 1 < savedSentences.size) onIndexChange(currentIndex + 1) }
        )

        // Offline English Speech Pack Notice Banner (shown when device lacks offline English pack and quiet mode is disabled)
        if (!quietModeEnabled && !offlinePackWarningMessage.isNullOrBlank()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("speaking_offline_pack_banner"),
                color = AmberAccent.copy(alpha = 0.14f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, AmberAccent)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = AmberAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Offline English Speech Pack Status",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = AmberAccent
                        )
                    }
                    Text(
                        text = offlinePackWarningMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )
                    ArabicText(
                        text = "تنبيه: حزمة التعرف الصوتي الإنجليزي بدون إنترنت (Offline English Speech Pack) غير مثبتة على هذا الجهاز.",
                        style = ArabicTypography.bodyMedium,
                        color = TextSecondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                    )
                                }
                            },
                            border = BorderStroke(1.dp, AmberAccent),
                            modifier = Modifier.testTag("open_voice_input_settings_button")
                        ) {
                            Text("Voice Input Settings", color = AmberAccent)
                        }
                        TextButton(
                            onClick = { preferOfflinePack = !preferOfflinePack }
                        ) {
                            Text(
                                text = if (preferOfflinePack) "Mode: Offline Preferred" else "Mode: Online Fallback",
                                color = GoldAccent
                            )
                        }
                    }
                }
            }
        }

        // Target Sentence to Read Aloud
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SurfaceDark,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, GoldAccentBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Read this sentence aloud:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = GoldAccent
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onPlayNormalTts(currentSentence.englishText) },
                            border = BorderStroke(1.dp, GoldAccentBorder),
                            modifier = Modifier.testTag("speaking_listen_tts_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Listen to target sentence",
                                tint = GoldAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (currentlyPlayingText == currentSentence.englishText) "Playing..." else "Listen",
                                color = GoldAccent
                            )
                        }

                        SlowAudioTurtleButton(
                            onClick = { onPlaySlowTts(currentSentence.englishText) },
                            showSpeedLabel = true,
                            testTag = "speaking_listen_slow_tts_button"
                        )
                    }
                }

                Text(
                    text = highlightSentenceWordInGold(currentSentence.englishText, currentSentence.sourceWord),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        textDirection = TextDirection.Ltr,
                        textAlign = TextAlign.Left
                    ),
                    color = TextPrimary
                )

                if (showTranslation && currentSentence.arabicText.isNotBlank()) {
                    ArabicText(
                        text = currentSentence.arabicText,
                        style = ArabicTypography.bodyLarge,
                        color = TextSecondary
                    )
                }

                HorizontalDivider(color = BorderDark)

                if (quietModeEnabled) {
                    // Quiet Mode tile assembly & input
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("speaking_quiet_mode_banner"),
                        color = AmberAccent.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, AmberAccent.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.School,
                                contentDescription = null,
                                tint = AmberAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Quiet Mode: Sentence Construction • وضع الهدوء",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = AmberAccent
                            )
                        }
                    }

                    Text(
                        text = "Tap words to assemble sentence:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )

                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quiet_mode_word_tiles"),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quietTokens.forEach { token ->
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        quietTypedInput = if (quietTypedInput.isBlank()) token else "$quietTypedInput $token"
                                    },
                                color = SurfaceElevatedDark,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, BorderDark)
                            ) {
                                Text(
                                    text = token,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = GoldAccent,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = quietTypedInput,
                        onValueChange = { quietTypedInput = it },
                        label = { Text("Assembled Sentence") },
                        placeholder = { Text("Tap word tiles or type sentence...", color = TextMuted) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("speaking_quiet_input_field"),
                        minLines = 2,
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldAccent,
                            unfocusedBorderColor = BorderDark,
                            focusedContainerColor = SurfaceElevatedDark,
                            unfocusedContainerColor = SurfaceElevatedDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedLabelColor = GoldAccent,
                            unfocusedLabelColor = TextSecondary
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val computed = SentencePracticeUtils.computeSentenceDiff(
                                    expected = currentSentence.englishText,
                                    actual = quietTypedInput,
                                    ignorePunctuation = ignorePunctuation
                                )
                                speakingDiff = computed
                                spokenTranscript = quietTypedInput
                                val itemId = if (currentSentence.id > 0L) {
                                    "sentence:${currentSentence.id}"
                                } else {
                                    currentSentence.englishText
                                }
                                onRecordAttempt(itemId, computed.accuracyPercent)
                            },
                            enabled = quietTypedInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldAccent,
                                contentColor = BackgroundDark
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("speaking_check_button")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Check Sentence • تحقق", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { quietTypedInput = "" },
                            border = BorderStroke(1.dp, BorderDark),
                            modifier = Modifier.testTag("speaking_clear_button")
                        ) {
                            Text("Clear", color = TextSecondary)
                        }
                    }
                } else {
                    // Record Button & Live Transcript Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (isListening) {
                                    runCatching { speechRecognizerRef.value?.stopListening() }
                                    isListening = false
                                } else if (hasAudioPermission) {
                                    startListeningSession()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isListening) CoralAccent else GoldAccent,
                                contentColor = BackgroundDark
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("speaking_record_button")
                        ) {
                            Icon(
                                imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Record with SpeechRecognizer",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isListening) "Stop Recording" else "Record & Compare",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = recognitionStatusText,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isListening) GoldAccent else TextSecondary,
                        modifier = Modifier.testTag("speaking_status_text")
                    )

                    if (spokenTranscript.isNotBlank()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = SurfaceElevatedDark,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, BorderDark)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Recognized Speech:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GoldAccent
                                )
                                Text(
                                    text = spokenTranscript,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        val currentSpeakingDiff = speakingDiff
        if (currentSpeakingDiff != null) {
            SentenceDiffResultCard(
                diff = currentSpeakingDiff,
                testTag = "speaking_diff_card"
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SentenceDiffResultCard(
    diff: DictationDiffResult,
    testTag: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        color = SurfaceDark,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (diff.isExactMatch) EmeraldAccent else GoldAccentBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        imageVector = if (diff.isExactMatch) Icons.Default.CheckCircle else Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = if (diff.isExactMatch) EmeraldAccent else GoldAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (diff.isExactMatch) {
                            "Perfect Match! (0 mistakes)"
                        } else {
                            "Diff of Mistakes (${diff.mistakesCount} mistake${if (diff.mistakesCount == 1) "" else "s"})"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (diff.isExactMatch) EmeraldAccent else TextPrimary,
                        modifier = Modifier.testTag("${testTag}_summary")
                    )
                }

                GoldBadge(text = "Accuracy: ${diff.accuracyPercent}%")
            }

            // Word-by-word visual diff chips
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("${testTag}_tokens"),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                diff.tokens.forEach { token ->
                    when (token.status) {
                        DiffTokenStatus.CORRECT -> {
                            Surface(
                                color = EmeraldAccent.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = token.text,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = EmeraldAccent,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        DiffTokenStatus.WRONG -> {
                            Surface(
                                color = CoralAccent.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, CoralAccent.copy(alpha = 0.6f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = token.text,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            textDecoration = TextDecoration.LineThrough
                                        ),
                                        color = CoralAccent
                                    )
                                    Text(
                                        text = "→ ${token.expectedText}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = GoldAccent
                                    )
                                }
                            }
                        }

                        DiffTokenStatus.MISSING -> {
                            Surface(
                                color = AmberAccent.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, AmberAccent.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = "[missing: ${token.expectedText}]",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = AmberAccent,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        DiffTokenStatus.EXTRA -> {
                            Surface(
                                color = CoralAccent.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, CoralAccent.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "+${token.text}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        textDecoration = TextDecoration.LineThrough
                                    ),
                                    color = CoralAccent,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Legend for diff tokens
            Text(
                text = "Green = Correct • Red strikethrough → Gold = Replaced mistake • Amber [missing] = Omitted word",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun PracticeNavigationHeader(
    modeTitle: String,
    currentIndex: Int,
    totalCount: Int,
    sourceWord: String,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceDark,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, BorderDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = modeTitle,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                if (sourceWord.isNotBlank()) {
                    Text(
                        text = "Source word: $sourceWord",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldAccent
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = onPrev,
                    enabled = currentIndex > 0,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("practice_prev_sentence_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous sentence",
                        tint = if (currentIndex > 0) GoldAccent else TextMuted
                    )
                }

                Surface(
                    color = GoldAccentDim,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, GoldAccentBorder)
                ) {
                    Text(
                        text = "${currentIndex + 1} / $totalCount",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = GoldAccent,
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("practice_sentence_counter")
                    )
                }

                IconButton(
                    onClick = onNext,
                    enabled = currentIndex + 1 < totalCount,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("practice_next_sentence_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next sentence",
                        tint = if (currentIndex + 1 < totalCount) GoldAccent else TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyPracticeStateCard(
    title: String,
    subtitleAr: String,
    onSwitchToFavorites: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("practice_empty_state"),
        color = SurfaceDark,
        shape = RoundedCornerShape(18.dp),
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
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            ArabicText(
                text = subtitleAr,
                style = ArabicTypography.bodyMedium,
                color = TextSecondary
            )
            Button(
                onClick = onSwitchToFavorites,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldAccent,
                    contentColor = BackgroundDark
                )
            ) {
                Text("Go to Favorites to Save Sentences", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SentencesSettingsSheetContent(
    ttsSpeechRate: Float,
    ignorePunctuation: Boolean,
    autoPlayPracticeAudio: Boolean,
    showTranslation: Boolean,
    sortOrder: SentenceSortOrder,
    onUpdateSpeechRate: (Float) -> Unit,
    onUpdateIgnorePunctuation: (Boolean) -> Unit,
    onUpdateAutoPlay: (Boolean) -> Unit,
    onToggleShowTranslation: () -> Unit,
    onToggleSortOrder: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .padding(bottom = 28.dp)
            .testTag("sentences_settings_sheet"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Sentences & Practice Settings",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                ArabicText(
                    text = "إعدادات الجمل والإملاء والتحدث",
                    style = ArabicTypography.bodyMedium,
                    color = GoldAccent
                )
            }
            TextButton(onClick = onClose) {
                Text("Done", color = GoldAccent, fontWeight = FontWeight.Bold)
            }
        }

        HorizontalDivider(color = BorderDark)

        // TTS Speech Rate
        Text(
            text = "TextToSpeech Playback Speed",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val rates = listOf(
                0.75f to "0.75x Slow",
                0.95f to "0.95x Normal",
                1.15f to "1.15x Fast"
            )
            rates.forEach { (rate, label) ->
                val selected = kotlin.math.abs(ttsSpeechRate - rate) < 0.05f
                FilterChip(
                    selected = selected,
                    onClick = { onUpdateSpeechRate(rate) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoldAccentDim,
                        selectedLabelColor = GoldAccent,
                        containerColor = SurfaceElevatedDark,
                        labelColor = TextSecondary
                    )
                )
            }
        }

        // Ignore Punctuation in Dictation & Speaking Diff
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Ignore Punctuation & Case in Diff",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary
                )
                Text(
                    text = "Focus on word spelling accuracy during Dictation and Speaking",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            Switch(
                checked = ignorePunctuation,
                onCheckedChange = onUpdateIgnorePunctuation,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = BackgroundDark,
                    checkedTrackColor = GoldAccent
                )
            )
        }

        // Auto-play audio when switching cards in Dictation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Auto-play TTS in Dictation",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary
                )
                Text(
                    text = "Automatically speak each sentence when navigating Dictation cards",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            Switch(
                checked = autoPlayPracticeAudio,
                onCheckedChange = onUpdateAutoPlay,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = BackgroundDark,
                    checkedTrackColor = GoldAccent
                )
            )
        }

        // Show Arabic translation toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Show Arabic Translations (examples_ar)",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary
                )
                Text(
                    text = "Currently: ${if (showTranslation) "Visible" else "Hidden"} • Sort: ${sortOrder.labelEn}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            Switch(
                checked = showTranslation,
                onCheckedChange = { onToggleShowTranslation() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = BackgroundDark,
                    checkedTrackColor = GoldAccent
                )
            )
        }
    }
}

private fun highlightSentenceWordInGold(sentence: String, rawWord: String) = buildAnnotatedString {
    val cleanTarget = rawWord.trim()
    if (cleanTarget.isEmpty()) {
        append(sentence)
        return@buildAnnotatedString
    }
    val regex = Regex("(?i)\\b${Regex.escape(cleanTarget)}\\w*\\b")
    var lastIndex = 0
    for (match in regex.findAll(sentence)) {
        if (match.range.first > lastIndex) {
            append(sentence.substring(lastIndex, match.range.first))
        }
        withStyle(SpanStyle(color = GoldAccent, fontWeight = FontWeight.Bold)) {
            append(match.value)
        }
        lastIndex = match.range.last + 1
    }
    if (lastIndex < sentence.length) {
        append(sentence.substring(lastIndex))
    }
}

private fun copyTextToClipboard(context: Context, text: String) {
    runCatching {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("Qaf Sentence", text))
    }
}

private fun mapSpeechRecognizerError(error: Int, preferOffline: Boolean): String = when (error) {
    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
    SpeechRecognizer.ERROR_CLIENT -> if (preferOffline) {
        "Offline English speech pack unavailable on this device"
    } else {
        "Speech recognizer client error"
    }
    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission not granted"
    SpeechRecognizer.ERROR_NETWORK -> "Network error (offline English speech pack missing)"
    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout (offline English speech pack missing)"
    SpeechRecognizer.ERROR_NO_MATCH -> "No English speech match recognized — please speak clearly and try again"
    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "SpeechRecognizer is busy"
    SpeechRecognizer.ERROR_SERVER -> "Speech recognition server error (offline pack missing)"
    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input detected"
    SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "Offline English (en-US) speech pack is not supported/installed"
    SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "Offline English (en-US) speech pack is not installed on this device"
    SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> "Speech service disconnected (offline pack missing)"
    else -> "Speech recognition error (code $error)"
}

private fun checkOfflineEnglishSpeechPack(context: Context, onResult: (Boolean) -> Unit) {
    runCatching {
        val detailsIntent = Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS)
        context.sendOrderedBroadcast(
            detailsIntent,
            null,
            object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    val extras = getResultExtras(true)
                    val supported = extras?.getStringArrayList(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES)
                    val hasEn = supported?.any {
                        it.startsWith("en", ignoreCase = true)
                    } == true
                    onResult(hasEn)
                }
            },
            null,
            Activity.RESULT_OK,
            null,
            null
        )
    }.onFailure {
        onResult(false)
    }
}
