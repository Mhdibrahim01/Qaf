package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items as lazyListItems
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.example.data.local.DeckStudyStateEntity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Style
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.VerticalDivider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontStyle
import com.example.data.ai.AiWordDetailJson
import com.example.ui.components.BidiText
import com.example.ui.components.isolateLatin
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.example.data.importer.DataImportRepository
import com.example.data.importer.ImportProgressState
import com.example.data.importer.WordFilterState
import com.example.data.importer.WordSortOrder
import com.example.data.local.DeckEntity
import com.example.data.local.EntryEntity
import com.example.data.local.GroupedWordCard
import com.example.data.local.WordProgressEntity
import com.example.ui.viewmodel.LevelProgressSummary
import com.example.ui.components.ArabicText
import com.example.ui.components.BooksModeSwitcher
import com.example.ui.components.EmptyState
import com.example.ui.components.ErrorState
import com.example.ui.components.GoldBadge
import com.example.ui.components.LoadingState
import com.example.ui.components.SearchField
import com.example.ui.components.SlowAudioTurtleButton
import com.example.ui.theme.ArabicTypography
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderDark
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldAccentBg
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldAccentBorder
import com.example.ui.theme.GoldAccentDim
import com.example.ui.theme.SkyAccent
import com.example.ui.theme.SkyAccentBg
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevatedDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletAccent
import com.example.ui.theme.VioletAccentBg
import com.example.ui.theme.EyebrowStyle
import com.example.ui.theme.StatValueGridStyle
import com.example.ui.theme.SquircleCardShape
import com.example.ui.theme.SquircleTileShape
import java.text.NumberFormat
import java.util.Locale

private val HEX_COLOR_CACHE = java.util.concurrent.ConcurrentHashMap<String, Color>()

private fun parseHexColor(hex: String, defaultColor: Color): Color {
    if (hex.isBlank()) return defaultColor
    return HEX_COLOR_CACHE.computeIfAbsent(hex) {
        try {
            Color(android.graphics.Color.parseColor(it))
        } catch (_: Exception) {
            defaultColor
        }
    }
}

private fun formatNumber(value: Int): String =
    NumberFormat.getNumberInstance(Locale.US).format(value)

@Composable
fun ImportProgressBanner(
    importState: ImportProgressState,
    totalEntriesCount: Int,
    onReverifyImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasMissingFiles = importState.missingFiles.isNotEmpty()
    if (!hasMissingFiles && !importState.isImporting) {
        return
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("import_progress_banner"),
        color = SurfaceDark,
        shape = SquircleCardShape,
        border = BorderStroke(
            width = 1.dp,
            color = when {
                hasMissingFiles -> CoralAccent
                importState.isImporting -> GoldAccentBorder
                else -> BorderDark
            }
        )
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = when {
                            hasMissingFiles -> Icons.Default.Warning
                            importState.isImporting -> Icons.Default.Storage
                            else -> Icons.Default.CheckCircle
                        },
                        contentDescription = null,
                        tint = when {
                            hasMissingFiles -> CoralAccent
                            importState.isImporting -> GoldAccent
                            else -> EmeraldAccent
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = when {
                                hasMissingFiles ->
                                    "Setting up vocabulary database..."
                                importState.isImporting ->
                                    "Importing vocabulary..."
                                else ->
                                    "Dictionary ready"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = when {
                                hasMissingFiles ->
                                    "Missing assets: ${importState.missingFiles.joinToString(", ")}"
                                importState.isImporting ->
                                    "${formatNumber(importState.processedRows)} / ${formatNumber(importState.totalExpectedRows)} words imported"
                                else ->
                                    "${formatNumber(totalEntriesCount)} words available"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (hasMissingFiles) CoralAccent else TextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onReverifyImport,
                    modifier = Modifier.testTag("reverify_import_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Re-verify import",
                        tint = GoldAccent
                    )
                }
            }

            AnimatedVisibility(visible = importState.isImporting) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { importState.progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = GoldAccent,
                        trackColor = SurfaceElevatedDark
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${(importState.progressFraction * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldAccent
                        )
                        Text(
                            text = "Importing vocabulary...",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
            }
        }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordBooksDatabaseScreen(
    decks: List<DeckEntity>,
    importState: ImportProgressState,
    totalEntriesCount: Int,
    selectedDeckId: String?,
    filterState: WordFilterState,
    pagedEntries: LazyPagingItems<EntryEntity>,
    progressMap: Map<String, WordProgressEntity>,
    currentlyPlayingWord: String?,
    detailedCard: GroupedWordCard?,
    favoriteCount: Int,
    masteredCount: Int,
    reviewDueTodayCount: Int = 0,
    isAiTranslating: Boolean = false,
    savedSentenceKeys: Set<String> = emptySet(),
    aiDetail: AiWordDetailJson? = null,
    isAiDetailLoading: Boolean = false,
    aiDetailError: String? = null,
    canRequestAi: Boolean = true,
    isAllowAi: Boolean = true,
    isOnline: Boolean = true,
    onSelectDeck: (String?) -> Unit,
    onOpenFlashcards: () -> Unit = {},
    onOpenFlashcardsForDeck: (String) -> Unit = {},
    onUpdateDeckFilterInList: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onTypeFilterChange: (String) -> Unit,
    onLevelFilterChange: (String) -> Unit,
    onPosFilterChange: (String) -> Unit,
    onSortOrderChange: (WordSortOrder) -> Unit,
    onClearFilters: () -> Unit,
    onToggleFavorite: (String, String) -> Unit,
    onToggleMastered: (String, String) -> Unit,
    onSelectWordListTab: (Boolean) -> Unit = {},
    onStatusFilterChange: (String) -> Unit = {},
    onOpenDetailedDefinition: (String, String) -> Unit,
    onDismissDetailedDefinition: () -> Unit,
    onRetryAiDefinition: (String) -> Unit = {},
    onEnrichWordWithAi: (String, String) -> Unit = { _, _ -> },
    onSaveUserArabic: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    onToggleSaveSentence: (String, String, String) -> Unit = { _, _, _ -> },
    onPlayAudio: (String, String) -> Unit,
    onPlayAudioSlow: (String, String, Boolean) -> Unit = { word, url, _ -> onPlayAudio(word, url) },
    onPlayVoiceAudio: (String, String, Boolean) -> Unit = { word, url, _ -> onPlayAudio(word, url) },
    onSpeakSentenceTts: (String) -> Unit = {},
    onReverifyImport: () -> Unit,
    deckStudyStates: List<DeckStudyStateEntity> = emptyList(),
    onSetCheckpoint: (String, String, Int) -> Unit = { _, _, _ -> },
    onOpenAllWords: (() -> Unit)? = null,
    onOpenMasteredWords: (() -> Unit)? = null,
    onOpenFavoriteWords: (() -> Unit)? = null,
    onOpenFilteredFlashcards: ((String, WordFilterState) -> Unit)? = null,
    onGetLetterIndex: (suspend (String?, Char) -> Int)? = null,
    showCoachTipWordTap: Boolean = false,
    onDismissCoachTipWordTap: () -> Unit = {},
    userLevel: String = "B2",
    levelProgressSummary: LevelProgressSummary = LevelProgressSummary(),
    modifier: Modifier = Modifier
) {
    val handleOpenAll = onOpenAllWords ?: {
        onSelectDeck("")
        onStatusFilterChange("All")
    }
    val handleOpenMastered = onOpenMasteredWords ?: {
        onSelectDeck("")
        onStatusFilterChange("Mastered")
    }
    val handleOpenFavorites = onOpenFavoriteWords ?: {
        onSelectDeck(DataImportRepository.SAVED_WORDS_DECK_ID)
        onStatusFilterChange("Favorites")
    }

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp.dp >= 720.dp

    Box(modifier = modifier.fillMaxSize()) {

        if (isTablet) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundDark)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    if (selectedDeckId != null) {
                        val selectedDeck = decks.find { it.id == filterState.deckId }
                        BackHandler {
                            onSelectDeck(null)
                        }
                        PagedDeckWordListScreen(
                            deck = selectedDeck,
                            allDecks = decks,
                            filterState = filterState,
                            pagedEntries = pagedEntries,
                            progressMap = progressMap,
                            currentlyPlayingWord = currentlyPlayingWord,
                            masteredCount = masteredCount,
                            totalEntriesCount = totalEntriesCount,
                            reviewDueTodayCount = reviewDueTodayCount,
                            deckStudyStates = deckStudyStates,
                            onSetCheckpoint = onSetCheckpoint,
                            onBack = { onSelectDeck(null) },
                            onOpenFlashcards = {
                                val targetDeckId = selectedDeck?.id ?: filterState.deckId
                                if (targetDeckId.isNotBlank() && targetDeckId != DataImportRepository.SAVED_WORDS_DECK_ID) {
                                    onOpenFlashcardsForDeck(targetDeckId)
                                } else {
                                    onOpenFlashcards()
                                }
                            },
                            onSelectWordListTab = onSelectWordListTab,
                            onStatusFilterChange = onStatusFilterChange,
                            onUpdateDeckFilter = onUpdateDeckFilterInList,
                            onSearchQueryChange = onSearchQueryChange,
                            onTypeFilterChange = onTypeFilterChange,
                            onLevelFilterChange = onLevelFilterChange,
                            onPosFilterChange = onPosFilterChange,
                            onSortOrderChange = onSortOrderChange,
                            onClearFilters = onClearFilters,
                            onToggleFavorite = onToggleFavorite,
                            onToggleMastered = onToggleMastered,
                            onOpenDetailedDefinition = onOpenDetailedDefinition,
                            onPlayAudio = onPlayAudio,
                            onOpenFilteredFlashcards = onOpenFilteredFlashcards,
                            onGetLetterIndex = onGetLetterIndex,
                            userLevel = userLevel,
                            levelProgressSummary = levelProgressSummary
                        )
                    } else {
                        DecksLibraryGrid(
                            decks = decks,
                            importState = importState,
                            totalEntriesCount = totalEntriesCount,
                            favoriteCount = favoriteCount,
                            masteredCount = masteredCount,
                            reviewDueTodayCount = reviewDueTodayCount,
                            onSelectDeck = onSelectDeck,
                            onOpenFlashcards = onOpenFlashcards,
                            onOpenFlashcardsForDeck = onOpenFlashcardsForDeck,
                            onReverifyImport = onReverifyImport,
                            onOpenAllWords = handleOpenAll,
                            onOpenMasteredWords = handleOpenMastered,
                            onOpenFavoriteWords = handleOpenFavorites
                        )
                    }
                }

                if (detailedCard != null) {
                    VerticalDivider(color = BorderDark, thickness = 1.dp)
                    AiDefinitionPanel(
                        card = detailedCard,
                        aiDetail = aiDetail,
                        isAiDetailLoading = isAiDetailLoading,
                        aiDetailError = aiDetailError,
                        canRequestAi = canRequestAi,
                        isAllowAi = isAllowAi,
                        isOnline = isOnline,
                        currentlyPlayingWord = currentlyPlayingWord,
                        savedSentenceKeys = savedSentenceKeys,
                        onPlayAudio = {
                            val url = detailedCard.posVariants.firstOrNull { it.audioUrl.isNotBlank() }?.audioUrl ?: ""
                            onPlayAudio(detailedCard.text, url)
                        },
                        onPlayAudioSlow = {
                            val url = detailedCard.posVariants.firstOrNull { it.audioUrl.isNotBlank() }?.audioUrl ?: ""
                            onPlayAudioSlow(detailedCard.text, url, false)
                        },
                        onPlayVoiceAudioSlow = { isGb ->
                            val url = if (!isGb) detailedCard.posVariants.firstOrNull { it.audioUrl.isNotBlank() }?.audioUrl ?: "" else ""
                            onPlayAudioSlow(detailedCard.text, url, isGb)
                        },
                        onPlayVoiceAudio = { isGb ->
                            val url = if (!isGb) detailedCard.posVariants.firstOrNull { it.audioUrl.isNotBlank() }?.audioUrl ?: "" else ""
                            onPlayVoiceAudio(detailedCard.text, url, isGb)
                        },
                        onSpeakSentenceTts = onSpeakSentenceTts,
                        onRetryAiDefinition = { onRetryAiDefinition(detailedCard.text) },
                        onDismiss = onDismissDetailedDefinition,
                        onEnrichWithAi = { onEnrichWordWithAi(detailedCard.deck, detailedCard.text) },
                        onSaveUserArabic = { entryId, userArabic ->
                            onSaveUserArabic(entryId, detailedCard.deck, detailedCard.text, userArabic)
                        },
                        onToggleSaveSentence = { en, ar ->
                            onToggleSaveSentence(en, ar, detailedCard.text)
                        },
                        onToggleFavorite = { onToggleFavorite(detailedCard.deck, detailedCard.text) },
                        onToggleMastered = { onToggleMastered(detailedCard.deck, detailedCard.text) },
                        modifier = Modifier
                            .width(400.dp)
                            .fillMaxHeight()
                            .background(SurfaceDark)
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundDark)
            ) {
                if (selectedDeckId != null) {
                    val selectedDeck = decks.find { it.id == filterState.deckId }
                    BackHandler {
                        onSelectDeck(null)
                    }
                    PagedDeckWordListScreen(
                        deck = selectedDeck,
                        allDecks = decks,
                        filterState = filterState,
                        pagedEntries = pagedEntries,
                        progressMap = progressMap,
                        currentlyPlayingWord = currentlyPlayingWord,
                        masteredCount = masteredCount,
                        totalEntriesCount = totalEntriesCount,
                        reviewDueTodayCount = reviewDueTodayCount,
                        deckStudyStates = deckStudyStates,
                        onSetCheckpoint = onSetCheckpoint,
                        onBack = { onSelectDeck(null) },
                        onOpenFlashcards = {
                            val targetDeckId = selectedDeck?.id ?: filterState.deckId
                            if (targetDeckId.isNotBlank() && targetDeckId != DataImportRepository.SAVED_WORDS_DECK_ID) {
                                onOpenFlashcardsForDeck(targetDeckId)
                            } else {
                                onOpenFlashcards()
                            }
                        },
                        onSelectWordListTab = onSelectWordListTab,
                        onStatusFilterChange = onStatusFilterChange,
                        onUpdateDeckFilter = onUpdateDeckFilterInList,
                        onSearchQueryChange = onSearchQueryChange,
                        onTypeFilterChange = onTypeFilterChange,
                        onLevelFilterChange = onLevelFilterChange,
                        onPosFilterChange = onPosFilterChange,
                        onSortOrderChange = onSortOrderChange,
                        onClearFilters = onClearFilters,
                        onToggleFavorite = onToggleFavorite,
                        onToggleMastered = onToggleMastered,
                        onOpenDetailedDefinition = onOpenDetailedDefinition,
                        onPlayAudio = onPlayAudio,
                        onOpenFilteredFlashcards = onOpenFilteredFlashcards,
                        onGetLetterIndex = onGetLetterIndex,
                        userLevel = userLevel,
                        levelProgressSummary = levelProgressSummary
                    )
                } else {
                    DecksLibraryGrid(
                        decks = decks,
                        importState = importState,
                        totalEntriesCount = totalEntriesCount,
                        favoriteCount = favoriteCount,
                        masteredCount = masteredCount,
                        reviewDueTodayCount = reviewDueTodayCount,
                        onSelectDeck = onSelectDeck,
                        onOpenFlashcards = onOpenFlashcards,
                        onOpenFlashcardsForDeck = onOpenFlashcardsForDeck,
                        onReverifyImport = onReverifyImport,
                        onOpenAllWords = handleOpenAll,
                        onOpenMasteredWords = handleOpenMastered,
                        onOpenFavoriteWords = handleOpenFavorites
                    )
                }
            }

            if (detailedCard != null) {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(
                    onDismissRequest = onDismissDetailedDefinition,
                    sheetState = sheetState,
                    containerColor = SurfaceDark,
                    contentColor = TextPrimary,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    AiDefinitionPanel(
                        card = detailedCard,
                        aiDetail = aiDetail,
                        isAiDetailLoading = isAiDetailLoading,
                        aiDetailError = aiDetailError,
                        canRequestAi = canRequestAi,
                        isAllowAi = isAllowAi,
                        isOnline = isOnline,
                        currentlyPlayingWord = currentlyPlayingWord,
                        savedSentenceKeys = savedSentenceKeys,
                        onPlayAudio = {
                            val url = detailedCard.posVariants.firstOrNull { it.audioUrl.isNotBlank() }?.audioUrl ?: ""
                            onPlayAudio(detailedCard.text, url)
                        },
                        onPlayAudioSlow = {
                            val url = detailedCard.posVariants.firstOrNull { it.audioUrl.isNotBlank() }?.audioUrl ?: ""
                            onPlayAudioSlow(detailedCard.text, url, false)
                        },
                        onPlayVoiceAudioSlow = { isGb ->
                            val url = if (!isGb) detailedCard.posVariants.firstOrNull { it.audioUrl.isNotBlank() }?.audioUrl ?: "" else ""
                            onPlayAudioSlow(detailedCard.text, url, isGb)
                        },
                        onPlayVoiceAudio = { isGb ->
                            val url = if (!isGb) detailedCard.posVariants.firstOrNull { it.audioUrl.isNotBlank() }?.audioUrl ?: "" else ""
                            onPlayVoiceAudio(detailedCard.text, url, isGb)
                        },
                        onSpeakSentenceTts = onSpeakSentenceTts,
                        onRetryAiDefinition = { onRetryAiDefinition(detailedCard.text) },
                        onDismiss = onDismissDetailedDefinition,
                        onEnrichWithAi = { onEnrichWordWithAi(detailedCard.deck, detailedCard.text) },
                        onSaveUserArabic = { entryId, userArabic ->
                            onSaveUserArabic(entryId, detailedCard.deck, detailedCard.text, userArabic)
                        },
                        onToggleSaveSentence = { en, ar ->
                            onToggleSaveSentence(en, ar, detailedCard.text)
                        },
                        onToggleFavorite = { onToggleFavorite(detailedCard.deck, detailedCard.text) },
                        onToggleMastered = { onToggleMastered(detailedCard.deck, detailedCard.text) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun DecksLibraryGrid(
    decks: List<DeckEntity>,
    importState: ImportProgressState,
    totalEntriesCount: Int,
    favoriteCount: Int,
    masteredCount: Int,
    reviewDueTodayCount: Int = 0,
    onSelectDeck: (String) -> Unit,
    onOpenFlashcards: () -> Unit = {},
    onOpenFlashcardsForDeck: (String) -> Unit = {},
    onReverifyImport: () -> Unit,
    onOpenAllWords: () -> Unit = {},
    onOpenMasteredWords: () -> Unit = {},
    onOpenFavoriteWords: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .testTag("screen_word_books"),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Mode Switcher: Word Books | Flashcards
            item(key = "mode_switcher", contentType = "mode_switcher") {
                BooksModeSwitcher(
                    isFlashcardsActive = false,
                    dueTodayCount = reviewDueTodayCount,
                    onSelectBooksMode = {},
                    onSelectFlashcardsMode = onOpenFlashcards
                )
            }

            item(key = "import_progress_banner", contentType = "import_banner") {
                ImportProgressBanner(
                    importState = importState,
                    totalEntriesCount = totalEntriesCount,
                    onReverifyImport = onReverifyImport
                )
            }

            // Page Intro (Figma 03-books.png)
            item(key = "page_intro", contentType = "page_intro") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${formatNumber(totalEntriesCount)} SAVED WORDS",
                        style = EyebrowStyle
                    )
                    Text(
                        text = "Your word books",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 28.sp
                        )
                    )
                    Text(
                        text = "Browse vocabulary by collection and learning level.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                    )
                }
            }

            // Stats Grid (3 columns: Total words, Mastered, Favorites)
            item(key = "stats_grid", contentType = "stats_grid") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    BookStatTile(
                        value = formatNumber(totalEntriesCount),
                        label = "Total words",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenAllWords
                    )
                    BookStatTile(
                        value = formatNumber(masteredCount),
                        label = "Mastered",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenMasteredWords
                    )
                    BookStatTile(
                        value = formatNumber(favoriteCount),
                        label = "Favorites",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenFavoriteWords
                    )
                }
            }

            // Section Heading: COLLECTIONS / Recommended for you
            item(key = "collections_heading", contentType = "collections_heading") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "COLLECTIONS",
                            style = EyebrowStyle
                        )
                        Text(
                            text = "Recommended for you",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }
                    Text(
                        text = "${decks.size} Books",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent
                        )
                    )
                }
            }

            // Numbered Book Cards
            itemsIndexed(
                items = decks,
                key = { _, deck -> deck.id },
                contentType = { _, _ -> "numbered_book_card" }
            ) { index, deck ->
                NumberedBookCard(
                    deck = deck,
                    index = index,
                    onClick = { onSelectDeck(deck.id) },
                    onStudyCards = if (deck.id != DataImportRepository.SAVED_WORDS_DECK_ID) {
                        { onOpenFlashcardsForDeck(deck.id) }
                    } else null
                )
            }
        }
    }
}

@Composable
private fun BookStatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else Modifier

    Surface(
        modifier = modifier
            .clip(SquircleTileShape)
            .then(clickableModifier),
        shape = SquircleTileShape,
        color = SurfaceDark,
        border = BorderStroke(1.dp, if (onClick != null) GoldAccent.copy(alpha = 0.35f) else BorderDark)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value,
                    style = StatValueGridStyle
                )
                if (onClick != null) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open $label",
                        tint = GoldAccent.copy(alpha = 0.7f),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (onClick != null) GoldAccent else TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun NumberedBookCard(
    deck: DeckEntity,
    index: Int,
    onClick: () -> Unit,
    onStudyCards: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isSavedBook = deck.id == DataImportRepository.SAVED_WORDS_DECK_ID
    val progressPercent = if (deck.rowCount > 0) {
        ((deck.masteredCount.toFloat() / deck.rowCount.toFloat()) * 100).toInt()
    } else 0
    val actionText = if (deck.masteredCount > 0 || deck.id == "oxford5000" || isSavedBook) {
        "Browse Words"
    } else {
        "Open Book"
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(SquircleCardShape)
            .clickable(onClick = onClick)
            .testTag("deck_card_${deck.id}"),
        shape = SquircleCardShape,
        color = SurfaceDark,
        border = BorderStroke(1.dp, BorderDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            val formattedIndex = remember(index) { String.format(Locale.US, "%02d", index + 1) }
            Text(
                text = formattedIndex,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4F4F57),
                    fontSize = 12.sp
                ),
                modifier = Modifier.padding(top = 2.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = deck.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 15.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (deck.nameAr.isNotBlank()) {
                    val isolatedNameAr = remember(deck.nameAr) { isolateLatin(deck.nameAr) }
                    ArabicText(
                        text = isolatedNameAr,
                        style = ArabicTypography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary,
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val metaText = remember(deck.rowCount, deck.category, deck.name, progressPercent) {
                    buildString {
                        append("${formatNumber(deck.rowCount)} words")
                        if (deck.category.isNotBlank() && deck.category != deck.name) {
                            append(" • ${deck.category}")
                        }
                        if (progressPercent > 0) {
                            append(" • $progressPercent% complete")
                        }
                    }
                }

                Text(
                    text = metaText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextMuted,
                        fontSize = 11.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("deck_count_${deck.id}")
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onClick,
                        shape = RoundedCornerShape(9.dp),
                        color = GoldAccentDim,
                        border = BorderStroke(1.dp, GoldAccentBorder),
                        modifier = Modifier.sizeIn(minWidth = 76.dp, minHeight = 40.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = actionText,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = GoldAccent,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    if (onStudyCards != null) {
                        Surface(
                            onClick = onStudyCards,
                            shape = RoundedCornerShape(9.dp),
                            color = SurfaceElevatedDark,
                            border = BorderStroke(1.dp, BorderDark),
                            modifier = Modifier
                                .sizeIn(minWidth = 76.dp, minHeight = 40.dp)
                                .testTag("deck_study_cards_${deck.id}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Style,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Study Cards",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookCoverGridCard(
    deck: DeckEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NumberedBookCard(
        deck = deck,
        index = 0,
        onClick = onClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PagedDeckWordListScreen(
    deck: DeckEntity?,
    allDecks: List<DeckEntity>,
    filterState: WordFilterState,
    pagedEntries: LazyPagingItems<EntryEntity>,
    progressMap: Map<String, WordProgressEntity>,
    currentlyPlayingWord: String?,
    masteredCount: Int = 0,
    totalEntriesCount: Int = 0,
    reviewDueTodayCount: Int = 0,
    deckStudyStates: List<DeckStudyStateEntity> = emptyList(),
    onSetCheckpoint: (String, String, Int) -> Unit = { _, _, _ -> },
    onBack: () -> Unit,
    onOpenFlashcards: () -> Unit = {},
    onSelectWordListTab: (Boolean) -> Unit = {},
    onStatusFilterChange: (String) -> Unit = {},
    onUpdateDeckFilter: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onTypeFilterChange: (String) -> Unit,
    onLevelFilterChange: (String) -> Unit,
    onPosFilterChange: (String) -> Unit,
    onSortOrderChange: (WordSortOrder) -> Unit,
    onClearFilters: () -> Unit,
    onToggleFavorite: (String, String) -> Unit,
    onToggleMastered: (String, String) -> Unit,
    onOpenDetailedDefinition: (String, String) -> Unit,
    onPlayAudio: (String, String) -> Unit,
    onOpenFilteredFlashcards: ((String, WordFilterState) -> Unit)? = null,
    onGetLetterIndex: (suspend (String?, Char) -> Int)? = null,
    userLevel: String = "B2",
    levelProgressSummary: LevelProgressSummary = LevelProgressSummary(),
    modifier: Modifier = Modifier
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var showAlphabetDialog by remember { mutableStateOf(false) }

    val levels = listOf("All", "A1", "A2", "B1", "B2", "C1")
    val statuses = listOf("All", "Learning", "Mastered", "Favorites")

    val activeStatus = when {
        filterState.onlyFavorites && filterState.deckId != DataImportRepository.SAVED_WORDS_DECK_ID -> "Favorites"
        filterState.onlyMastered -> "Mastered"
        filterState.onlyLearning -> "Learning"
        else -> "All"
    }

    val sortShortLabel = when (filterState.sortOrder) {
        WordSortOrder.AZ -> "A–Z"
        WordSortOrder.ZA -> "Z–A"
        WordSortOrder.LEVEL -> "Level"
        WordSortOrder.MIXED -> "Smart Mix"
    }

    val isEmpty = pagedEntries.itemCount == 0 &&
        pagedEntries.loadState.refresh is LoadState.NotLoading

    val currentDeckId = deck?.id ?: filterState.deckId
    val currentStudyState = remember(deckStudyStates, currentDeckId) {
        deckStudyStates.find { it.deckId == currentDeckId }
    }
    val checkpointWord = remember(currentStudyState) {
        currentStudyState?.lastWordText?.trim().orEmpty()
    }
    val hasCheckpoint = checkpointWord.isNotBlank()
    val headerItemsCount = 3 + (if (levelProgressSummary.hasRecommendedWords) 1 else 0) + (if (hasCheckpoint) 1 else 0)
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .testTag("deck_word_list_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize()
                .testTag("paged_word_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp)
        ) {
            // 1. Deck Header (Back + Title + Studied Progress + Study Button)
            item(key = "deck_header", contentType = "deck_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable(onClick = onBack)
                                    .padding(vertical = 2.dp)
                                    .testTag("back_to_decks_button"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to Word Books",
                                    tint = TextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "WORD BOOK",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        letterSpacing = 1.5.sp
                                    ),
                                    color = TextMuted
                                )
                            }

                            Text(
                                text = deck?.name ?: "All Words",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 28.sp,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = TextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            val totalCount = if (deck != null) deck.rowCount else totalEntriesCount
                            val studiedWords = remember(progressMap, currentDeckId, deck?.masteredCount, masteredCount) {
                                progressMap.values.count { 
                                    (currentDeckId.isEmpty() || it.deck == currentDeckId) && (it.isMastered || it.repetitions > 0)
                                }.coerceAtLeast(deck?.masteredCount ?: masteredCount)
                            }
                            val leftWords = remember(totalCount, studiedWords) { (totalCount - studiedWords).coerceAtLeast(0) }
                            val progressFraction = remember(totalCount, studiedWords) {
                                if (totalCount > 0) (studiedWords.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f) else 0f
                            }
                            val visualProgressFraction = remember(studiedWords, progressFraction) {
                                if (studiedWords > 0) progressFraction.coerceIn(0.02f, 1f) else 0f
                            }
                            val progressPercentText = remember(studiedWords, totalCount) {
                                formatProgressPercent(studiedWords, totalCount)
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = "${formatNumber(totalCount)} entries",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                    color = TextSecondary
                                )
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    ),
                                    color = GoldAccent
                                )
                                Text(
                                    text = "${formatNumber(studiedWords)} studied",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = GoldAccent
                                    )
                                )
                            }

                            // Studied vs Left Progress Bar Card
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = SurfaceElevatedDark,
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
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .background(GoldAccent)
                                            )
                                            Text(
                                                text = "${formatNumber(studiedWords)} Studied",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = GoldAccent
                                                )
                                            )
                                            Text(
                                                text = "•",
                                                color = TextMuted,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .border(1.dp, TextMuted, CircleShape)
                                            )
                                            Text(
                                                text = "${formatNumber(leftWords)} Left",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = TextSecondary
                                                )
                                            )
                                        }
                                        Text(
                                            text = progressPercentText,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = GoldAccent
                                            )
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { visualProgressFraction },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = GoldAccent,
                                        trackColor = BorderDark
                                    )
                                }
                            }
                        }

                        val hasNonSortFilters = filterState.searchQuery.isNotBlank() ||
                            (filterState.levelFilter.isNotBlank() && !filterState.levelFilter.equals("All", ignoreCase = true)) ||
                            filterState.posFilter.isNotBlank() ||
                            filterState.onlyFavorites ||
                            filterState.onlyMastered ||
                            filterState.onlyLearning

                        val isMixedOrder = filterState.sortOrder == WordSortOrder.MIXED
                        val hasActiveFilters = hasNonSortFilters || isMixedOrder || filterState.sortOrder != WordSortOrder.AZ

                        val studyButtonText = when {
                            hasNonSortFilters && isMixedOrder -> "Study mixed"
                            hasNonSortFilters -> "Study filtered"
                            isMixedOrder -> "Study mixed"
                            filterState.sortOrder != WordSortOrder.AZ -> "Study sorted"
                            else -> "Study cards"
                        }

                        Button(
                            onClick = {
                                if (hasActiveFilters && onOpenFilteredFlashcards != null) {
                                    onOpenFilteredFlashcards(currentDeckId, filterState)
                                } else {
                                    onOpenFlashcards()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldAccent,
                                contentColor = BackgroundDark
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            modifier = Modifier.testTag("study_cards_header_btn")
                        ) {
                            Text(
                                text = studyButtonText,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                color = BackgroundDark
                            )
                        }
                    }
                }

            // 2. Search & Filters Card
            item(key = "search_and_filters", contentType = "search_and_filters") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SearchField(
                        query = filterState.searchQuery,
                        onQueryChange = onSearchQueryChange,
                        placeholder = "Search word, translation, or meaning...",
                        onClear = { onSearchQueryChange("") },
                        testTag = "word_search_input"
                    )

                    // 3. Unified LEVEL & STATUS Filter Card matching image.png
                    Surface(
                        color = SurfaceDark,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, BorderDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // LEVEL section
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "LEVEL",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        letterSpacing = 1.2.sp
                                    ),
                                    color = TextMuted
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    levels.forEach { lvl ->
                                        val isAll = lvl == "All"
                                        val selected = if (isAll) {
                                            filterState.levelFilter.isEmpty()
                                        } else {
                                            filterState.levelFilter == lvl
                                        }
                                        val isUserLevel = lvl.equals(userLevel, ignoreCase = true) && !isAll
                                        val displayLabel = if (isUserLevel) "$lvl ★" else lvl
                                        WordBookFilterPill(
                                            label = displayLabel,
                                            selected = selected,
                                            onClick = {
                                                if (isAll) {
                                                    if (filterState.levelFilter.isNotEmpty()) {
                                                        onLevelFilterChange(filterState.levelFilter)
                                                    }
                                                } else {
                                                    onLevelFilterChange(lvl)
                                                }
                                            },
                                            testTag = "level_filter_$lvl"
                                        )
                                    }
                                }
                            }

                            // STATUS section
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "STATUS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        letterSpacing = 1.2.sp
                                    ),
                                    color = TextMuted
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    statuses.forEach { status ->
                                        val selected = activeStatus == status
                                        val tag = when (status) {
                                            "All" -> "tab_all_words"
                                            "Mastered" -> "tab_mastered_words"
                                            "Learning" -> "tab_learning_words"
                                            else -> "tab_favorites_words"
                                        }
                                        WordBookFilterPill(
                                            label = status,
                                            selected = selected,
                                            onClick = {
                                                onStatusFilterChange(status)
                                            },
                                            testTag = tag
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Words Count & Sort Bar
            item(key = "count_and_sort", contentType = "count_and_sort") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = formatNumber(pagedEntries.itemCount),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                color = TextPrimary
                            )
                            Text(
                                text = " words shown",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                color = TextSecondary
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (onGetLetterIndex != null) {
                                val isAzSort = filterState.sortOrder == WordSortOrder.AZ
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            if (!isAzSort) {
                                                onSortOrderChange(WordSortOrder.AZ)
                                            }
                                            showAlphabetDialog = true
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .testTag("alphabet_jump_button"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "🔤 A–Z",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        ),
                                        color = if (isAzSort) GoldAccent else TextMuted.copy(alpha = 0.4f)
                                    )
                                }
                            }

                            val isMixed = filterState.sortOrder == WordSortOrder.MIXED
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        onSortOrderChange(if (isMixed) WordSortOrder.AZ else WordSortOrder.MIXED)
                                    }
                                    .background(if (isMixed) GoldAccentDim else Color.Transparent)
                                    .border(1.dp, if (isMixed) GoldAccentBorder else BorderDark, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("smart_mix_toggle_button"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (isMixed) "⚡ Mix ON" else "⚡ Mix",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (isMixed) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp
                                    ),
                                    color = if (isMixed) GoldAccent else TextSecondary
                                )
                            }

                            Box {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { showSortMenu = true }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .testTag("sort_menu_button"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Sort,
                                        contentDescription = "Sort words",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = sortShortLabel,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.sp
                                        ),
                                        color = TextSecondary
                                    )
                                }
                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false },
                                    containerColor = SurfaceElevatedDark
                                ) {
                                    WordSortOrder.entries.forEach { order ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = order.labelEn,
                                                    color = if (filterState.sortOrder == order) GoldAccent else TextPrimary,
                                                    fontWeight = if (filterState.sortOrder == order) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                onSortOrderChange(order)
                                                showSortMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

            if (levelProgressSummary.hasRecommendedWords) {
                item(key = "level_banner", contentType = "level_banner") {
                    LevelProgressSummaryBanner(
                        summary = levelProgressSummary,
                        onFilterToLevel = { targetLvl ->
                            if (!filterState.levelFilter.equals(targetLvl, ignoreCase = true)) {
                                onLevelFilterChange(targetLvl)
                            }
                        },
                        onQuickPractice = {
                            val targetLevel = if (filterState.levelFilter.isNotBlank() && !filterState.levelFilter.equals("All", ignoreCase = true)) {
                                filterState.levelFilter
                            } else {
                                userLevel
                            }
                            val targetFilterState = filterState.copy(levelFilter = targetLevel)
                            if (onOpenFilteredFlashcards != null) {
                                onOpenFilteredFlashcards(currentDeckId, targetFilterState)
                            } else {
                                onOpenFlashcards()
                            }
                        },
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }

            if (hasCheckpoint) {
                item(key = "checkpoint_banner", contentType = "checkpoint_banner") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        color = GoldAccentDim,
                        border = BorderStroke(1.dp, GoldAccentBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "📍",
                                    fontSize = 18.sp
                                )
                                Column {
                                    Text(
                                        text = "LAST STUDIED CHECKPOINT",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp,
                                            color = GoldAccent
                                        )
                                    )
                                    val lastIndex = (currentStudyState?.lastCardIndex ?: 0) + 1
                                    val totalCount = deck?.rowCount ?: totalEntriesCount
                                    val percent = if (totalCount > 0) (lastIndex * 100 / totalCount).coerceIn(0, 100) else 0
                                    Text(
                                        text = "\"$checkpointWord\" • Card #$lastIndex of ${formatNumber(totalCount)} ($percent%)",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val totalCount = pagedEntries.itemCount
                                        if (totalCount > 0) {
                                            val candidateIdx = currentStudyState?.lastCardIndex?.takeIf { it in 0 until totalCount }
                                            val targetIdx = if (candidateIdx != null && pagedEntries.peek(candidateIdx)?.text.equals(checkpointWord, ignoreCase = true)) {
                                                candidateIdx
                                            } else {
                                                candidateIdx ?: 0
                                            }
                                            pagedEntries[targetIdx]
                                            listState.scrollToItem((targetIdx + headerItemsCount).coerceAtLeast(0))
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldAccent,
                                    contentColor = BackgroundDark
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.heightIn(min = 32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Resume →",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 5. Word List Content
            when {
                pagedEntries.loadState.refresh is LoadState.Loading && pagedEntries.itemCount == 0 -> {
                    item(key = "loading_state", contentType = "loading_state") {
                        LoadingState(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            messageEn = "Loading vocabulary...",
                            messageAr = "جاري تحميل المفردات..."
                        )
                    }
                }
                pagedEntries.loadState.refresh is LoadState.Error && pagedEntries.itemCount == 0 -> {
                    val err = (pagedEntries.loadState.refresh as LoadState.Error).error
                    item(key = "error_state", contentType = "error_state") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ErrorState(
                                messageEn = err.localizedMessage ?: "Failed to load vocabulary",
                                messageAr = "تعذر تحميل المفردات",
                                onRetry = { pagedEntries.retry() }
                            )
                        }
                    }
                }
                isEmpty -> {
                    val emptyTitle = when {
                        filterState.deckId == DataImportRepository.SAVED_WORDS_DECK_ID || activeStatus == "Favorites" ->
                            "No Favorite Words Yet"
                        activeStatus == "Mastered" ->
                            "No Mastered Words Yet"
                        (deck?.rowCount ?: 0) == 0 ->
                            "Empty Deck (${deck?.name ?: filterState.deckId})"
                        else ->
                            "No Matching Words"
                    }
                    val emptyDesc = when {
                        filterState.deckId == DataImportRepository.SAVED_WORDS_DECK_ID || activeStatus == "Favorites" ->
                            "Tap the heart icon on any word to save it to your favorites."
                        activeStatus == "Mastered" ->
                            "Study cards or mark words as mastered to see them here."
                        (deck?.rowCount ?: 0) == 0 ->
                            "0 rows imported for ${deck?.id}."
                        else ->
                            "Try adjusting your search query or clearing the level/status filters."
                    }
                    val emptyTitleAr = when {
                        filterState.deckId == DataImportRepository.SAVED_WORDS_DECK_ID || activeStatus == "Favorites" ->
                            "لا توجد كلمات مفضلة بعد"
                        activeStatus == "Mastered" ->
                            "لا توجد كلمات متقنة بعد"
                        (deck?.rowCount ?: 0) == 0 ->
                            "الكتاب فارغ"
                        else ->
                            "لا توجد نتائج مطابقة"
                    }
                    item(key = "empty_state", contentType = "empty_state") {
                        EmptyState(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            titleEn = emptyTitle,
                            titleAr = emptyTitleAr,
                            descriptionEn = emptyDesc,
                            icon = Icons.AutoMirrored.Filled.MenuBook
                        )
                    }
                }
                else -> {
                    val itemCount = pagedEntries.itemCount
                    items(
                        count = itemCount,
                        key = pagedEntries.itemKey { it.id },
                        contentType = pagedEntries.itemContentType { "word_card" }
                    ) { index ->
                        val entry = pagedEntries[index]
                        if (entry != null) {
                            val cardKey = remember(entry.id) { "${entry.deck}:${entry.text.trim().lowercase()}" }
                            val prog = progressMap[cardKey]
                            val isFirst = index == 0
                            val isLast = index == itemCount - 1
                            val isCurrentCheckpoint = hasCheckpoint && entry.text.equals(checkpointWord, ignoreCase = true)
                            val onPlayAudioLambda = remember(entry.text, entry.audioUrl, entry.deck, index, onPlayAudio, onSetCheckpoint) {
                                {
                                    onPlayAudio(entry.text, entry.audioUrl)
                                    onSetCheckpoint(entry.deck, entry.text, index)
                                }
                            }
                            val onToggleMasteredLambda = remember(entry.deck, entry.text, onToggleMastered) {
                                { onToggleMastered(entry.deck, entry.text) }
                            }
                            val onToggleFavoriteLambda = remember(entry.deck, entry.text, onToggleFavorite) {
                                { onToggleFavorite(entry.deck, entry.text) }
                            }
                            val onSetCheckpointLambda = remember(entry.deck, entry.text, index, onSetCheckpoint) {
                                { onSetCheckpoint(entry.deck, entry.text, index) }
                            }
                            val onOpenDetailLambda = remember(entry.deck, entry.text, index, onOpenDetailedDefinition, onSetCheckpoint) {
                                {
                                    onOpenDetailedDefinition(entry.deck, entry.text)
                                    onSetCheckpoint(entry.deck, entry.text, index)
                                }
                            }

                            WordBookEntryRow(
                                entry = entry,
                                isMastered = prog?.isMastered ?: false,
                                isFavorite = prog?.isFavorite ?: false,
                                isPlayingAudio = currentlyPlayingWord == entry.text,
                                studiedCount = prog?.repetitions ?: 0,
                                isCheckpoint = isCurrentCheckpoint,
                                isFirstInCard = isFirst,
                                isLastInCard = isLast,
                                onPlayAudio = onPlayAudioLambda,
                                onToggleMastered = onToggleMasteredLambda,
                                onToggleFavorite = onToggleFavoriteLambda,
                                onSetCheckpoint = onSetCheckpointLambda,
                                onOpenDetail = onOpenDetailLambda
                            )
                        } else {
                            WordBookEntryPlaceholderRow()
                        }
                    }

                    if (pagedEntries.loadState.append is LoadState.Loading) {
                        item(key = "append_loading", contentType = "append_loading") {
                            LoadingState(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                messageEn = "Loading more words..."
                            )
                        }
                    } else if (pagedEntries.loadState.append is LoadState.Error) {
                        item(key = "append_error", contentType = "append_error") {
                            val appendErr = (pagedEntries.loadState.append as LoadState.Error).error
                            ErrorState(
                                messageEn = appendErr.localizedMessage ?: "Failed to load more words",
                                onRetry = { pagedEntries.retry() }
                            )
                        }
                    }
                }
            }
        }

        if (showAlphabetDialog) {
            AlertDialog(
                onDismissRequest = { showAlphabetDialog = false },
                containerColor = SurfaceDark,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Text(
                        text = "Jump to Letter (A–Z)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                },
                text = {
                    val alphabet = ('A'..'Z').toList()
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Tap any letter to jump instantly to words starting with that letter:",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            alphabet.forEach { letter ->
                                Surface(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            showAlphabetDialog = false
                                            if (filterState.sortOrder != WordSortOrder.AZ) {
                                                onSortOrderChange(WordSortOrder.AZ)
                                            }
                                            coroutineScope.launch {
                                                val totalCount = pagedEntries.itemCount
                                                if (totalCount > 0) {
                                                    val targetIdx = onGetLetterIndex?.invoke(currentDeckId, letter) ?: 0
                                                    val clampedIdx = targetIdx.coerceIn(0, totalCount - 1)
                                                    pagedEntries[clampedIdx]
                                                    listState.scrollToItem((clampedIdx + headerItemsCount).coerceAtLeast(0))
                                                }
                                            }
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    color = SurfaceElevatedDark,
                                    border = BorderStroke(1.dp, BorderDark)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = letter.toString(),
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = GoldAccent
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAlphabetDialog = false }) {
                        Text("Close", color = TextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
private fun WordBookFilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String = ""
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier),
        color = if (selected) GoldAccentDim else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        border = if (selected) BorderStroke(1.dp, GoldAccentBorder) else null
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp
            ),
            color = if (selected) GoldAccent else TextSecondary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

fun posShortLabel(pos: String): String {
    return when (val clean = pos.trim().lowercase().removeSuffix(".")) {
        "n", "noun" -> "n."
        "v", "verb", "phrasal verb" -> "v."
        "adj", "adjective" -> "adj."
        "adv", "adverb" -> "adv."
        "prep", "preposition" -> "prep."
        "conj", "conjunction" -> "conj."
        "pron", "pronoun" -> "pron."
        "interj", "interjection" -> "interj."
        "art", "article", "indefinite article", "definite article" -> "art."
        "idm", "idiom" -> "idm."
        "phr", "phrase" -> "phr."
        "web" -> "web."
        "" -> "n."
        else -> if (clean.length > 4) clean.take(3) + "." else "$clean."
    }
}

private val ARABIC_MEANING_SPLIT_REGEX = Regex("[;؛\\n]+")
private val ROW_SHAPE_SINGLE = RoundedCornerShape(16.dp)
private val ROW_SHAPE_FIRST = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
private val ROW_SHAPE_LAST = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
private val ROW_SHAPE_MIDDLE = RoundedCornerShape(0.dp)

fun parseArabicMeaningGroups(pos: String, arabic: String): List<Pair<String, String>> {
    val cleanArabic = arabic.trim()
    if (cleanArabic.isEmpty()) return emptyList()

    val rawParts = cleanArabic.split(ARABIC_MEANING_SPLIT_REGEX)
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    if (rawParts.isEmpty()) return emptyList()

    val primaryPos = posShortLabel(pos)

    if (rawParts.size == 1) {
        return listOf(primaryPos to rawParts[0])
    }

    val firstGroup = rawParts.take(2).joinToString(" ؛ ")
    val result = mutableListOf(primaryPos to firstGroup)
    if (rawParts.size > 2) {
        val excessGroup = rawParts.drop(2).joinToString(" ؛ ")
        result.add("web." to excessGroup)
    }
    return result
}

fun posFullName(pos: String): String {
    return when (val clean = pos.trim().lowercase().removeSuffix(".")) {
        "n", "noun" -> "Noun"
        "v", "verb", "phrasal verb" -> "Verb"
        "adj", "adjective" -> "Adjective"
        "adv", "adverb" -> "Adverb"
        "prep", "preposition" -> "Preposition"
        "conj", "conjunction" -> "Conjunction"
        "pron", "pronoun" -> "Pronoun"
        "interj", "interjection" -> "Interjection"
        "art", "article", "indefinite article", "definite article" -> "Article"
        "idiom", "idm" -> "Idiom"
        "phrase", "phr" -> "Phrase"
        "web" -> "Web"
        else -> clean.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    }
}

@Composable
fun ShimmerPlaceholder(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_anim"
    )
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            SurfaceElevatedDark,
            SurfaceElevatedDark.copy(alpha = 0.6f),
            BorderDark,
            SurfaceElevatedDark
        ),
        start = Offset(translateAnim - 500f, 0f),
        end = Offset(translateAnim, 0f)
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(shimmerBrush)
    )
}

@Composable
private fun WordBookEntryPlaceholderRow(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        color = SurfaceDark,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceElevatedDark)
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 110.dp, height = 18.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(SurfaceElevatedDark)
                        )
                        Box(
                            modifier = Modifier
                                .size(width = 28.dp, height = 16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(SurfaceElevatedDark)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(width = 90.dp, height = 12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceElevatedDark)
                    )
                    Box(
                        modifier = Modifier
                            .size(width = 140.dp, height = 14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceElevatedDark)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(SurfaceElevatedDark)
            )
        }
    }
}

/**
 * Dictionary Word list row matching image.png:
 * - Left: Rounded-square speaker button
 * - Center:
 *   - Line 1: Bold word + Gold CEFR level badge (e.g., B2, A2, C1)
 *   - Line 2: Phonetic IPA • lowercase part of speech (e.g., /əˈbændən/ • verb)
 *   - Line 3: Arabic translation in clean white text
 * - Right: Favorite heart icon (filled gold when favorite, outlined muted when not)
 */
@Composable
fun WordBookEntryRow(
    entry: EntryEntity,
    isMastered: Boolean,
    isFavorite: Boolean,
    isPlayingAudio: Boolean,
    studiedCount: Int = 0,
    isCheckpoint: Boolean = false,
    isFirstInCard: Boolean = false,
    isLastInCard: Boolean = false,
    onPlayAudio: () -> Unit,
    onToggleMastered: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSetCheckpoint: (() -> Unit)? = null,
    onOpenDetail: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rowShape = when {
        isFirstInCard && isLastInCard -> ROW_SHAPE_SINGLE
        isFirstInCard -> ROW_SHAPE_FIRST
        isLastInCard -> ROW_SHAPE_LAST
        else -> ROW_SHAPE_MIDDLE
    }

    val posLower = remember(entry.pos) {
        posFullName(entry.pos).lowercase()
    }
    val displayLevel = remember(entry.level) { entry.level.uppercase() }
    val ipaAndPos = remember(entry.ipa, posLower) {
        when {
            entry.ipa.isNotBlank() && posLower.isNotBlank() -> "${entry.ipa}  •  $posLower"
            entry.ipa.isNotBlank() -> entry.ipa
            else -> posLower
        }
    }
    val formattedArabic = remember(entry.arabic) {
        entry.arabic
            .split(ARABIC_MEANING_SPLIT_REGEX)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .take(3)
            .joinToString("، ")
    }
    val containsLatin = remember(formattedArabic) {
        formattedArabic.any { (it in 'a'..'z') || (it in 'A'..'Z') }
    }

    val rowModifier = if (isFirstInCard || isLastInCard) {
        modifier
            .fillMaxWidth()
            .clip(rowShape)
            .background(SurfaceDark)
            .border(1.dp, BorderDark, rowShape)
    } else {
        modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .border(1.dp, BorderDark)
    }

    Column(
        modifier = rowModifier
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Button,
                onClick = onOpenDetail
            )
            .testTag("word_row_${entry.id}"),
    ) {
        Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Left: Rounded-square speaker button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isPlayingAudio) GoldAccentDim else SurfaceElevatedDark)
                        .border(1.dp, if (isPlayingAudio) GoldAccentBorder else BorderDark, RoundedCornerShape(10.dp))
                        .clickable(
                            interactionSource = null,
                            indication = null,
                            role = Role.Button,
                            onClick = onPlayAudio
                        )
                        .testTag("ipa_speaker_${entry.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Play pronunciation for ${entry.text}",
                        tint = if (isPlayingAudio) GoldAccent else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Middle: Word + Level Badge, IPA • POS, Arabic Meaning
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = entry.text,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = if (isMastered) EmeraldAccent else TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (entry.level.isNotBlank()) {
                            Text(
                                text = displayLevel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = GoldAccent,
                                modifier = Modifier
                                    .drawBehind {
                                        drawRoundRect(
                                            color = GoldAccentDim,
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                                        )
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                    }

                    if (ipaAndPos.isNotBlank()) {
                        Text(
                            text = ipaAndPos,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    if (formattedArabic.isNotBlank()) {
                        if (!containsLatin) {
                            Text(
                                text = formattedArabic,
                                style = ArabicTypography.bodyMedium.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 0.sp,
                                    textDirection = TextDirection.Rtl,
                                    textAlign = TextAlign.Right
                                ),
                                color = TextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            BidiText(
                                text = formattedArabic,
                                style = ArabicTypography.bodyMedium.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 0.sp
                                ),
                                color = TextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                fillMaxWidth = false
                            )
                        }
                    } else {
                        Text(
                            text = "Tap to look up",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontStyle = FontStyle.Italic,
                                fontSize = 12.sp
                            ),
                            color = TextMuted,
                            maxLines = 1
                        )
                    }
                }

                // Right: Checkpoint Pin Bookmark, Mastered Check & Favorite heart icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (onSetCheckpoint != null) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clickable(
                                    interactionSource = null,
                                    indication = null,
                                    role = Role.Button,
                                    onClick = onSetCheckpoint
                                )
                                .testTag("checkpoint_pin_${entry.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isCheckpoint) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = if (isCheckpoint) "Current Checkpoint" else "Set as Checkpoint",
                                tint = if (isCheckpoint) GoldAccent else TextMuted,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable(
                                interactionSource = null,
                                indication = null,
                                role = Role.Button,
                                onClick = onToggleMastered
                            )
                            .testTag("mastered_check_${entry.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isMastered) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                            contentDescription = if (isMastered) "Mastered" else "Mark as mastered",
                            tint = if (isMastered) EmeraldAccent else TextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable(
                                interactionSource = null,
                                indication = null,
                                role = Role.Button,
                                onClick = onToggleFavorite
                            )
                            .testTag("favorite_heart_${entry.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite word",
                            tint = if (isFavorite) GoldAccent else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
}


/**
 * Trancy AI Definition Panel:
 * Used as a full-height bottom sheet on phones, or as a side panel on tablets (maxWidth >= 720.dp).
 */
@Composable
fun AiDefinitionPanel(
    card: GroupedWordCard,
    aiDetail: AiWordDetailJson?,
    isAiDetailLoading: Boolean,
    aiDetailError: String?,
    currentlyPlayingWord: String?,
    savedSentenceKeys: Set<String>,
    canRequestAi: Boolean = true,
    isAllowAi: Boolean = true,
    isOnline: Boolean = true,
    onPlayAudio: () -> Unit,
    onPlayAudioSlow: () -> Unit = {},
    onPlayVoiceAudioSlow: (isGb: Boolean) -> Unit = { onPlayAudioSlow() },
    onPlayVoiceAudio: (isGb: Boolean) -> Unit,
    onSpeakSentenceTts: (String) -> Unit,
    onRetryAiDefinition: () -> Unit,
    onDismiss: () -> Unit,
    onEnrichWithAi: () -> Unit,
    onSaveUserArabic: (String, String) -> Unit,
    onToggleSaveSentence: (String, String) -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleMastered: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showOptionsMenu by remember { mutableStateOf(false) }
    var isEditingArabic by remember(card.cardKey) { mutableStateOf(false) }
    val primaryVariant = card.posVariants.firstOrNull()
    var editedArabicText by remember(card.cardKey, primaryVariant?.arabicMeaning) {
        mutableStateOf(primaryVariant?.arabicMeaning.orEmpty())
    }

    Column(
        modifier = modifier
            .background(SurfaceDark)
            .padding(bottom = 16.dp)
    ) {
        // Top Header: [X] AI Definition            [Heart] [Check] [3-dots]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "AI Definition",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (card.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Toggle favorite",
                        tint = if (card.isFavorite) GoldAccent else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onToggleMastered,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (card.isMastered) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                        contentDescription = if (card.isMastered) "Mastered" else "Mark as mastered",
                        tint = if (card.isMastered) EmeraldAccent else TextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Box {
                    IconButton(onClick = { showOptionsMenu = true }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showOptionsMenu,
                        onDismissRequest = { showOptionsMenu = false },
                        containerColor = SurfaceElevatedDark
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Arabic meaning", color = TextPrimary) },
                            onClick = {
                                isEditingArabic = true
                                showOptionsMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Regenerate with AI", color = GoldAccent) },
                            onClick = {
                                onRetryAiDefinition()
                                showOptionsMenu = false
                            }
                        )
                        if (primaryVariant?.arabicSource?.isNotBlank() == true) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Source: ${primaryVariant.arabicSource}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                },
                                onClick = { showOptionsMenu = false }
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = BorderDark, thickness = 1.dp)

        // Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Inline User Arabic Editing Field (accessible from 3-dots menu)
            if (isEditingArabic && primaryVariant != null) {
                Surface(
                    color = SurfaceElevatedDark,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, GoldAccentBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Custom Arabic Meaning (saved as 'user'):",
                            style = MaterialTheme.typography.labelMedium,
                            color = GoldAccent
                        )
                        OutlinedTextField(
                            value = editedArabicText,
                            onValueChange = { editedArabicText = it },
                            placeholder = { Text("Enter Arabic translation...", color = TextMuted) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("user_arabic_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldAccent,
                                unfocusedBorderColor = BorderDark,
                                focusedContainerColor = SurfaceDark,
                                unfocusedContainerColor = SurfaceDark,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { isEditingArabic = false }) {
                                Text("Cancel", color = TextSecondary)
                            }
                            TextButton(
                                onClick = {
                                    onSaveUserArabic(primaryVariant.entryId, editedArabicText)
                                    isEditingArabic = false
                                },
                                modifier = Modifier.testTag("save_user_arabic_button")
                            ) {
                                Text("Save", color = GoldAccent, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Section 1: Syllables Heading
            val syllablesText = aiDetail?.syllables?.ifBlank { null }
                ?: card.syllables.ifBlank { null }
                ?: card.text
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = syllablesText,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                if (card.primaryLevel.isNotBlank()) {
                    GoldBadge(text = card.primaryLevel)
                }
            }

            // Section 2: Dual US & GB pronunciation chips + Slow Turtle (0.6x)
            val isPlayingWord = currentlyPlayingWord == card.text
            var selectedIsGb by remember(card.cardKey) { mutableStateOf(false) }
            var isPlayingSlowLocal by remember(card.cardKey) { mutableStateOf(false) }
            LaunchedEffect(isPlayingWord) {
                if (!isPlayingWord) {
                    isPlayingSlowLocal = false
                }
            }
            val isUsPlaying = isPlayingWord && !selectedIsGb && !isPlayingSlowLocal
            val isGbPlaying = isPlayingWord && selectedIsGb && !isPlayingSlowLocal
            val isSlowPlaying = isPlayingWord && isPlayingSlowLocal

            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // US Chip
                val ipaUs = aiDetail?.ipaUs?.ifBlank { card.ipa } ?: card.ipa
                val isUsHighlighted = isUsPlaying || !selectedIsGb
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            selectedIsGb = false
                            isPlayingSlowLocal = false
                            onPlayVoiceAudio(false)
                        }
                        .testTag("pronounce_us_button"),
                    color = when {
                        isUsPlaying -> GoldAccent
                        !selectedIsGb -> GoldAccentDim
                        else -> SurfaceElevatedDark
                    },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isUsHighlighted) GoldAccentBorder else BorderDark
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (ipaUs.isNotBlank()) "US $ipaUs" else "US",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = when {
                                isUsPlaying -> BackgroundDark
                                !selectedIsGb -> GoldAccent
                                else -> TextSecondary
                            },
                            maxLines = 1,
                            softWrap = false
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "US Pronunciation",
                            tint = when {
                                isUsPlaying -> BackgroundDark
                                !selectedIsGb -> GoldAccent
                                else -> TextSecondary
                            },
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                // GB Chip
                val ipaGb = aiDetail?.ipaGb ?: ""
                val isGbHighlighted = isGbPlaying || selectedIsGb
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            selectedIsGb = true
                            isPlayingSlowLocal = false
                            onPlayVoiceAudio(true)
                        }
                        .testTag("pronounce_gb_button"),
                    color = when {
                        isGbPlaying -> GoldAccent
                        selectedIsGb -> GoldAccentDim
                        else -> SurfaceElevatedDark
                    },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isGbHighlighted) GoldAccentBorder else BorderDark
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (ipaGb.isNotBlank()) "GB $ipaGb" else "GB",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = when {
                                isGbPlaying -> BackgroundDark
                                selectedIsGb -> GoldAccent
                                else -> TextSecondary
                            },
                            maxLines = 1,
                            softWrap = false
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "GB Pronunciation",
                            tint = when {
                                isGbPlaying -> BackgroundDark
                                selectedIsGb -> GoldAccent
                                else -> TextSecondary
                            },
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                // Slow Turtle (0.6x) Chip
                SlowAudioTurtleButton(
                    onClick = {
                        isPlayingSlowLocal = true
                        onPlayVoiceAudioSlow(selectedIsGb)
                    },
                    isPlayingSlow = isSlowPlaying,
                    showSpeedLabel = true,
                    testTag = "pronounce_slow_button"
                )
            }

            // AI Status: Loading Shimmer, Error Card, or "AI Definition" Button
            if (isAiDetailLoading && aiDetail == null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShimmerPlaceholder(modifier = Modifier.fillMaxWidth().height(16.dp))
                    ShimmerPlaceholder(modifier = Modifier.fillMaxWidth(0.6f).height(14.dp))
                    ShimmerPlaceholder(modifier = Modifier.fillMaxWidth().height(60.dp))
                }
            } else if (aiDetailError != null && aiDetail == null) {
                Surface(
                    color = SurfaceElevatedDark,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, CoralAccent.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().testTag("ai_error_card")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = aiDetailError,
                            style = MaterialTheme.typography.bodySmall,
                            color = CoralAccent,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onRetryAiDefinition) {
                            Text("Retry", color = GoldAccent, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (aiDetail == null) {
                // When details not yet saved, provide the explicit AI Definition button
                val buttonText = when {
                    !isOnline -> "AI Definition (Offline)"
                    !canRequestAi && isAllowAi -> "AI Definition (Limit Reached)"
                    else -> "AI Definition"
                }
                Button(
                    onClick = onRetryAiDefinition,
                    enabled = isOnline,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldAccent,
                        contentColor = BackgroundDark,
                        disabledContainerColor = SurfaceElevatedDark,
                        disabledContentColor = TextMuted
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("request_ai_definition_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = buttonText,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Section 3: Meanings (Grouped by POS, badge shown ONCE per part of speech)
            val aiMeanings = aiDetail?.meanings
            if (!aiMeanings.isNullOrEmpty()) {
                val groupedByPos = aiMeanings.groupBy { posShortLabel(it.pos) }
                groupedByPos.forEach { (shortPos, meaningsInPos) ->
                    val fullName = posFullName(shortPos)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        GoldBadge(text = "$shortPos $fullName")

                        meaningsInPos.forEach { meaning ->
                            MeaningItemView(
                                arabicList = meaning.arabic,
                                definitionEn = meaning.definitionEn,
                                exampleEn = meaning.example?.en.orEmpty(),
                                exampleAr = meaning.example?.ar.orEmpty(),
                                cardText = card.text,
                                savedSentenceKeys = savedSentenceKeys,
                                onSpeakSentence = onSpeakSentenceTts,
                                onToggleSaveSentence = onToggleSaveSentence
                            )
                        }
                    }
                }
            } else {
                // Show local data fallback
                val groupedVariants = card.posVariants.groupBy { posShortLabel(it.pos) }
                groupedVariants.forEach { (shortPos, variantsInPos) ->
                    val fullName = posFullName(shortPos)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        GoldBadge(text = "$shortPos $fullName")

                        variantsInPos.forEach { variant ->
                            MeaningItemView(
                                arabicList = if (variant.arabicMeaning.isNotBlank()) listOf(variant.arabicMeaning) else emptyList(),
                                definitionEn = variant.definitionEn,
                                exampleEn = variant.examplesEn.firstOrNull() ?: "",
                                exampleAr = variant.examplesAr.firstOrNull() ?: "",
                                cardText = card.text,
                                savedSentenceKeys = savedSentenceKeys,
                                onSpeakSentence = onSpeakSentenceTts,
                                onToggleSaveSentence = onToggleSaveSentence
                            )
                        }
                    }
                }
            }

            // Section 4: Phrases Section (Trancy style with gold vertical bar on the left)
            val phrases = aiDetail?.phrases
            if (!phrases.isNullOrEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(18.dp)
                                .background(GoldAccent, RoundedCornerShape(1.dp))
                        )
                        Text(
                            text = "Phrases",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        phrases.forEach { phrase ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = phrase.en,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        textDirection = TextDirection.Ltr
                                    ),
                                    color = TextPrimary
                                )
                                if (phrase.ar.isNotEmpty()) {
                                    BidiText(
                                        text = phrase.ar.joinToString(" ؛ "),
                                        style = ArabicTypography.bodyMedium.copy(letterSpacing = 0.sp),
                                        color = TextSecondary,
                                        fillMaxWidth = true,
                                        textAlign = TextAlign.Right
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 5: Optional Etymology
            val etymology = aiDetail?.etymologyAr
            if (!etymology.isNullOrBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Etymology / أصل الكلمة",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = GoldAccent
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = SurfaceElevatedDark,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, BorderDark)
                    ) {
                        Box(modifier = Modifier.padding(12.dp)) {
                            BidiText(
                                text = etymology,
                                style = ArabicTypography.bodyMedium.copy(letterSpacing = 0.sp),
                                color = TextSecondary,
                                fillMaxWidth = true,
                                textAlign = TextAlign.Right
                            )
                        }
                    }
                }
            }

            // Section 5b: More Examples
            val extraExamples = aiDetail?.examples
            if (!extraExamples.isNullOrEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "More Examples",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = GoldAccent
                    )
                    extraExamples.forEachIndexed { _, ex ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = SurfaceElevatedDark,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, BorderDark)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = highlightTargetWordInGold(ex.en, card.text),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            textDirection = TextDirection.Ltr
                                        ),
                                        color = TextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { onSpeakSentenceTts(ex.en) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                contentDescription = "Listen",
                                                tint = GoldAccent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        val isSaved = ex.en.trim().lowercase() in savedSentenceKeys
                                        IconButton(
                                            onClick = { onToggleSaveSentence(ex.en, ex.ar) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                                contentDescription = if (isSaved) "Unsave" else "Save",
                                                tint = if (isSaved) GoldAccent else TextMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                                if (ex.ar.isNotBlank()) {
                                    BidiText(
                                        text = ex.ar,
                                        style = ArabicTypography.bodySmall.copy(letterSpacing = 0.sp),
                                        color = TextSecondary,
                                        fillMaxWidth = true,
                                        textAlign = TextAlign.Right
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

/**
 * Meaning Item View:
 * Fixes the layout bug by displaying Arabic text on its OWN full-width line, right-aligned, RTL,
 * with no letter-spacing and no per-character wrapping.
 */
@Composable
private fun MeaningItemView(
    arabicList: List<String>,
    definitionEn: String,
    exampleEn: String,
    exampleAr: String,
    cardText: String,
    savedSentenceKeys: Set<String>,
    onSpeakSentence: (String) -> Unit,
    onToggleSaveSentence: (String, String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Arabic text on its OWN line, full width, RTL, right-aligned, bold, 18sp
        val joinedArabic = arabicList.filter { it.isNotBlank() }.joinToString(" ؛ ")
        if (joinedArabic.isNotBlank()) {
            BidiText(
                text = joinedArabic,
                style = ArabicTypography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    letterSpacing = 0.sp
                ),
                color = TextPrimary,
                fillMaxWidth = true,
                textAlign = TextAlign.Right
            )
        }

        // Short English definition in gray
        if (definitionEn.isNotBlank()) {
            Text(
                text = definitionEn,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp
                ),
                color = TextSecondary
            )
        }

        // One example with target word highlighted in gold, speaker icon, and Arabic translation
        if (exampleEn.isNotBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = highlightTargetWordInGold(exampleEn, cardText),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            textDirection = TextDirection.Ltr,
                            textAlign = TextAlign.Left
                        ),
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { onSpeakSentence(exampleEn) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Listen to example",
                            tint = GoldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                if (exampleAr.isNotBlank()) {
                    BidiText(
                        text = exampleAr,
                        style = ArabicTypography.bodySmall.copy(letterSpacing = 0.sp),
                        color = TextSecondary,
                        fillMaxWidth = true,
                        textAlign = TextAlign.Right
                    )
                }
            }
        }
    }
}


/**
 * Backward-compatible wrapper for WordDetailedDefinitionSheet.
 * Preserves all previous parameters with sensible defaults so existing callers (e.g. FlashcardsScreen) continue to work.
 */
@Composable
internal fun WordDetailedDefinitionSheet(
    card: GroupedWordCard,
    isPlaying: Boolean,
    isAiTranslating: Boolean = false,
    savedSentenceKeys: Set<String> = emptySet(),
    aiDetail: AiWordDetailJson? = null,
    isAiDetailLoading: Boolean = false,
    aiDetailError: String? = null,
    canRequestAi: Boolean = true,
    isAllowAi: Boolean = true,
    isOnline: Boolean = true,
    onPlayAudio: () -> Unit,
    onPlayAudioSlow: () -> Unit = {},
    onPlayVoiceAudioSlow: (Boolean) -> Unit = { _ -> onPlayAudioSlow() },
    onPlayVoiceAudio: (Boolean) -> Unit = { _ -> onPlayAudio() },
    onSpeakSentenceTts: (String) -> Unit = {},
    onRetryAiDefinition: () -> Unit = {},
    onDismiss: () -> Unit = {},
    onEnrichWithAi: () -> Unit = {},
    onSaveUserArabic: (String, String) -> Unit = { _, _ -> },
    onToggleSaveSentence: (String, String) -> Unit = { _, _ -> },
    onToggleFavorite: () -> Unit,
    onToggleMastered: () -> Unit,
    modifier: Modifier = Modifier
) {
    AiDefinitionPanel(
        card = card,
        aiDetail = aiDetail,
        isAiDetailLoading = isAiDetailLoading,
        aiDetailError = aiDetailError,
        currentlyPlayingWord = if (isPlaying) card.text else null,
        savedSentenceKeys = savedSentenceKeys,
        canRequestAi = canRequestAi,
        isAllowAi = isAllowAi,
        isOnline = isOnline,
        onPlayAudio = onPlayAudio,
        onPlayAudioSlow = onPlayAudioSlow,
        onPlayVoiceAudioSlow = onPlayVoiceAudioSlow,
        onPlayVoiceAudio = onPlayVoiceAudio,
        onSpeakSentenceTts = onSpeakSentenceTts,
        onRetryAiDefinition = onRetryAiDefinition,
        onDismiss = onDismiss,
        onEnrichWithAi = onEnrichWithAi,
        onSaveUserArabic = onSaveUserArabic,
        onToggleSaveSentence = onToggleSaveSentence,
        onToggleFavorite = onToggleFavorite,
        onToggleMastered = onToggleMastered,
        modifier = modifier
    )
}

private fun highlightTargetWordInGold(sentence: String, rawWord: String) = buildAnnotatedString {
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

private fun formatProgressPercent(count: Int, total: Int): String {
    if (total <= 0 || count <= 0) return "0%"
    if (count >= total) return "100%"
    val rawPercent = (count.toDouble() * 100.0) / total.toDouble()
    return when {
        rawPercent < 0.1 -> "<0.1%"
        rawPercent < 10.0 -> {
            val oneDecimal = String.format(Locale.US, "%.1f", rawPercent)
            if (oneDecimal.endsWith(".0")) "${rawPercent.toInt()}%" else "$oneDecimal%"
        }
        else -> "${rawPercent.toInt()}%"
    }
}

@Composable
fun LevelProgressSummaryBanner(
    summary: LevelProgressSummary,
    onQuickPractice: () -> Unit,
    onFilterToLevel: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (!summary.hasRecommendedWords) return

    val displayLevel = summary.level

    val levelColor = when (displayLevel) {
        "A1", "A2" -> EmeraldAccent
        "B1", "B2" -> SkyAccent
        else -> VioletAccent
    }
    val levelBg = when (displayLevel) {
        "A1", "A2" -> EmeraldAccentBg
        "B1", "B2" -> SkyAccentBg
        else -> VioletAccentBg
    }
    val levelAr = when (displayLevel) {
        "A1" -> "مبتدئ"
        "A2" -> "ابتدائي"
        "B1" -> "متوسط"
        "B2" -> "فوق المتوسط"
        "C1" -> "متقدم"
        "C2" -> "إتقان"
        else -> displayLevel
    }

    val totalCount = summary.totalCount
    val masteredCount = summary.masteredCount
    val deckTotalMasteredCount = summary.deckTotalMasteredCount
    val progressFraction = if (totalCount > 0) (masteredCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f) else 0f
    val visualProgressFraction = if (masteredCount > 0) progressFraction.coerceIn(0.025f, 1f) else 0f
    val progressPercentText = formatProgressPercent(masteredCount, totalCount)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .testTag("level_progress_summary_banner"),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, levelColor.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header row with Level chip & Progress stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = if (onFilterToLevel != null) {
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onFilterToLevel(displayLevel) }
                        } else {
                            Modifier
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = levelBg,
                        border = BorderStroke(1.dp, levelColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "Level $displayLevel",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = levelColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceElevatedDark,
                        border = BorderStroke(1.dp, BorderDark)
                    ) {
                        Text(
                            text = levelAr,
                            style = ArabicTypography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = levelBg.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, levelColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = if (totalCount > 0) {
                            "${formatNumber(masteredCount)} / ${formatNumber(totalCount)} Mastered ($progressPercentText)"
                        } else {
                            "0 Words"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = levelColor,
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("level_progress_mastered_badge")
                    )
                }
            }

            // Progress bar
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Level $displayLevel Vocabulary Progress",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        if (deckTotalMasteredCount > masteredCount && summary.isDefaultLevelSelection) {
                            Text(
                                text = "${formatNumber(masteredCount)} $displayLevel mastered • ${formatNumber(deckTotalMasteredCount)} total mastered in book",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldAccent
                            )
                        }
                    }
                    Text(
                        text = "تقدم كلمات مستوى $displayLevel",
                        style = ArabicTypography.labelSmall,
                        color = TextMuted
                    )
                }

                LinearProgressIndicator(
                    progress = { visualProgressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .testTag("level_progress_bar"),
                    color = levelColor,
                    trackColor = SurfaceElevatedDark
                )
            }

            // Quick practice action bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Practice ${formatNumber(totalCount)} $displayLevel words with flashcards",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "تمرن على مفردات مستوى $displayLevel بالكروت التفاعلية",
                        style = ArabicTypography.labelSmall,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = onQuickPractice,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldAccent,
                        contentColor = BackgroundDark
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("quick_practice_button")
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Quick Practice",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

