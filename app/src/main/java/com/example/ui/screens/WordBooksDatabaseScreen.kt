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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.paging.compose.itemKey
import com.example.data.importer.DataImportRepository
import com.example.data.importer.ImportProgressState
import com.example.data.importer.WordFilterState
import com.example.data.importer.WordSortOrder
import com.example.data.local.DeckEntity
import com.example.data.local.EntryEntity
import com.example.data.local.GroupedWordCard
import com.example.data.local.WordProgressEntity
import com.example.ui.components.ArabicText
import com.example.ui.components.BooksModeSwitcher
import com.example.ui.components.EmptyState
import com.example.ui.components.ErrorState
import com.example.ui.components.GoldBadge
import com.example.ui.components.LoadingState
import com.example.ui.components.SearchField
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
import com.example.ui.theme.EyebrowStyle
import com.example.ui.theme.StatValueGridStyle
import com.example.ui.theme.SquircleCardShape
import com.example.ui.theme.SquircleTileShape
import java.text.NumberFormat
import java.util.Locale

private fun parseHexColor(hex: String, defaultColor: Color): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        defaultColor
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
    onOpenDetailedDefinition: (String, String) -> Unit,
    onDismissDetailedDefinition: () -> Unit,
    onRetryAiDefinition: (String) -> Unit = {},
    onEnrichWordWithAi: (String, String) -> Unit = { _, _ -> },
    onSaveUserArabic: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    onToggleSaveSentence: (String, String, String) -> Unit = { _, _, _ -> },
    onPlayAudio: (String, String) -> Unit,
    onPlayVoiceAudio: (String, String, Boolean) -> Unit = { word, url, _ -> onPlayAudio(word, url) },
    onSpeakSentenceTts: (String) -> Unit = {},
    onReverifyImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 720.dp

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
                            reviewDueTodayCount = reviewDueTodayCount,
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
                            onPlayAudio = onPlayAudio
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
                            onReverifyImport = onReverifyImport
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
                        reviewDueTodayCount = reviewDueTodayCount,
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
                        onPlayAudio = onPlayAudio
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
                        onReverifyImport = onReverifyImport
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
            item {
                BooksModeSwitcher(
                    isFlashcardsActive = false,
                    dueTodayCount = reviewDueTodayCount,
                    onSelectBooksMode = {},
                    onSelectFlashcardsMode = onOpenFlashcards
                )
            }

            item {
                ImportProgressBanner(
                    importState = importState,
                    totalEntriesCount = totalEntriesCount,
                    onReverifyImport = onReverifyImport
                )
            }

            // Page Intro (Figma 03-books.png)
            item {
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
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    BookStatTile(
                        value = formatNumber(totalEntriesCount),
                        label = "Total words",
                        modifier = Modifier.weight(1f)
                    )
                    BookStatTile(
                        value = formatNumber(masteredCount),
                        label = "Mastered",
                        modifier = Modifier.weight(1f)
                    )
                    BookStatTile(
                        value = formatNumber(favoriteCount),
                        label = "Favorites",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Section Heading: COLLECTIONS / Recommended for you
            item {
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
            itemsIndexed(decks, key = { _, deck -> deck.id }) { index, deck ->
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
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = SquircleTileShape,
        color = SurfaceDark,
        border = BorderStroke(1.dp, BorderDark)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = StatValueGridStyle
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextMuted,
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
            Text(
                text = String.format(Locale.US, "%02d", index + 1),
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
                    ArabicText(
                        text = isolateLatin(deck.nameAr),
                        style = ArabicTypography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary,
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val metaText = buildString {
                    append("${formatNumber(deck.rowCount)} words")
                    if (deck.category.isNotBlank() && deck.category != deck.name) {
                        append(" • ${deck.category}")
                    }
                    if (progressPercent > 0) {
                        append(" • $progressPercent% complete")
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
    reviewDueTodayCount: Int = 0,
    onBack: () -> Unit,
    onOpenFlashcards: () -> Unit = {},
    onSelectWordListTab: (Boolean) -> Unit = {},
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
    modifier: Modifier = Modifier
) {
    var showFiltersPanel by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    val levels = listOf("A1", "A2", "B1", "B2", "C1")
    val posOptions = listOf("n.", "v.", "adj.", "adv.", "prep.")
    val typeOptions = listOf("word", "academic", "phrase", "expression", "idiom")

    val hasActiveFilters = filterState.levelFilter.isNotEmpty() ||
        filterState.posFilter.isNotEmpty() ||
        filterState.typeFilter.isNotEmpty() ||
        filterState.searchQuery.isNotEmpty()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .testTag("deck_word_list_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize()
        ) {
            Surface(
                color = SurfaceDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BooksModeSwitcher(
                        isFlashcardsActive = false,
                        dueTodayCount = reviewDueTodayCount,
                        onSelectBooksMode = {},
                        onSelectFlashcardsMode = onOpenFlashcards
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.testTag("back_to_decks_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to Word Books",
                                    tint = TextPrimary
                                )
                            }
                            Column {
                                Text(
                                    text = deck?.name ?: "All Words",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${formatNumber(deck?.rowCount ?: 0)} entries • Dictionary View",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GoldAccent
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { showFiltersPanel = !showFiltersPanel },
                                modifier = Modifier.testTag("toggle_filters_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Filters",
                                    tint = if (showFiltersPanel || hasActiveFilters) GoldAccent else TextSecondary
                                )
                            }

                            Box {
                                IconButton(
                                    onClick = { showSortMenu = true },
                                    modifier = Modifier.testTag("sort_menu_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Sort,
                                        contentDescription = "Sort words",
                                        tint = GoldAccent
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

                    SearchField(
                        query = filterState.searchQuery,
                        onQueryChange = onSearchQueryChange,
                        placeholder = "Search word, definition, example, or Arabic...",
                        onClear = { onSearchQueryChange("") },
                        testTag = "word_search_input"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        levels.forEach { lvl ->
                            val selected = filterState.levelFilter == lvl
                            FilterChip(
                                selected = selected,
                                onClick = { onLevelFilterChange(lvl) },
                                label = { Text(lvl) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldAccentDim,
                                    selectedLabelColor = GoldAccent,
                                    containerColor = SurfaceElevatedDark,
                                    labelColor = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selected,
                                    borderColor = BorderDark,
                                    selectedBorderColor = GoldAccentBorder
                                )
                            )
                        }

                        posOptions.forEach { pos ->
                            val selected = filterState.posFilter == pos
                            FilterChip(
                                selected = selected,
                                onClick = { onPosFilterChange(pos) },
                                label = { Text(pos) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldAccentDim,
                                    selectedLabelColor = GoldAccent,
                                    containerColor = SurfaceElevatedDark,
                                    labelColor = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selected,
                                    borderColor = BorderDark,
                                    selectedBorderColor = GoldAccentBorder
                                )
                            )
                        }
                    }

                    AnimatedVisibility(visible = showFiltersPanel) {
                        Surface(
                            color = SurfaceElevatedDark,
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.dp, BorderDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Filter by Deck & Entry Type",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = GoldAccent
                                    )
                                    if (hasActiveFilters) {
                                        TextButton(onClick = onClearFilters) {
                                            Text("Reset Filters", color = CoralAccent)
                                        }
                                    }
                                }

                                Text(
                                    text = "Entry Type:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    typeOptions.forEach { type ->
                                        val selected = filterState.typeFilter == type
                                        FilterChip(
                                            selected = selected,
                                            onClick = { onTypeFilterChange(type) },
                                            label = { Text(type) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = GoldAccentDim,
                                                selectedLabelColor = GoldAccent,
                                                containerColor = SurfaceDark,
                                                labelColor = TextSecondary
                                            )
                                        )
                                    }
                                }

                                Text(
                                    text = "Switch Deck:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    allDecks.forEach { d ->
                                        val selected = filterState.deckId == d.id
                                        FilterChip(
                                            selected = selected,
                                            onClick = { onUpdateDeckFilter(d.id) },
                                            label = { Text(d.name) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = GoldAccentDim,
                                                selectedLabelColor = GoldAccent,
                                                containerColor = SurfaceDark,
                                                labelColor = TextSecondary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                    // Top Tabs: All | Mastered <count>
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isMasteredOnly = filterState.onlyMastered
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { onSelectWordListTab(false) }
                                .testTag("tab_all_words"),
                            color = if (!isMasteredOnly) GoldAccentDim else SurfaceElevatedDark,
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, if (!isMasteredOnly) GoldAccentBorder else BorderDark)
                        ) {
                            Text(
                                text = "All",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (!isMasteredOnly) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (!isMasteredOnly) GoldAccent else TextSecondary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { onSelectWordListTab(true) }
                                .testTag("tab_mastered_words"),
                            color = if (isMasteredOnly) EmeraldAccent.copy(alpha = 0.18f) else SurfaceElevatedDark,
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, if (isMasteredOnly) EmeraldAccent.copy(alpha = 0.5f) else BorderDark)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Mastered",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isMasteredOnly) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isMasteredOnly) EmeraldAccent else TextSecondary
                                )
                                Surface(
                                    color = if (isMasteredOnly) EmeraldAccent.copy(alpha = 0.3f) else BackgroundDark,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "$masteredCount",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        ),
                                        color = if (isMasteredOnly) EmeraldAccent else TextMuted,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = BorderDark, thickness = 1.dp)

            val isEmpty = pagedEntries.itemCount == 0 &&
                pagedEntries.loadState.refresh is LoadState.NotLoading

            when {
                pagedEntries.loadState.refresh is LoadState.Loading && pagedEntries.itemCount == 0 -> {
                    LoadingState(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        messageEn = "Loading vocabulary...",
                        messageAr = "جاري تحميل المفردات..."
                    )
                }
                pagedEntries.loadState.refresh is LoadState.Error && pagedEntries.itemCount == 0 -> {
                    val err = (pagedEntries.loadState.refresh as LoadState.Error).error
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
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
                isEmpty -> {
                    val emptyTitle = when {
                        filterState.deckId == DataImportRepository.SAVED_WORDS_DECK_ID ->
                            "No Saved Words Yet"
                        (deck?.rowCount ?: 0) == 0 ->
                            "Empty Deck (${deck?.name ?: filterState.deckId})"
                        else ->
                            "No Matching Words"
                    }
                    val emptyDesc = when {
                        filterState.deckId == DataImportRepository.SAVED_WORDS_DECK_ID ->
                            "Tap the heart icon on any word to save it here."
                        (deck?.rowCount ?: 0) == 0 ->
                            "0 rows imported for ${deck?.id}."
                        else ->
                            "Try adjusting your search query or clearing the level/POS filters."
                    }
                    val emptyTitleAr = when {
                        filterState.deckId == DataImportRepository.SAVED_WORDS_DECK_ID ->
                            "لا توجد كلمات محفوظة بعد"
                        (deck?.rowCount ?: 0) == 0 ->
                            "الكتاب فارغ"
                        else ->
                            "لا توجد نتائج مطابقة"
                    }
                    EmptyState(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        titleEn = emptyTitle,
                        titleAr = emptyTitleAr,
                        descriptionEn = emptyDesc,
                        icon = Icons.AutoMirrored.Filled.MenuBook
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("paged_word_list"),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            count = pagedEntries.itemCount,
                            key = pagedEntries.itemKey { it.id }
                        ) { index ->
                            val entry = pagedEntries[index]
                            if (entry != null) {
                                val cardKey = "${entry.deck}:${entry.text.trim().lowercase()}"
                                val prog = progressMap[cardKey]
                                WordBookEntryRow(
                                    entry = entry,
                                    isMastered = prog?.isMastered ?: false,
                                    isFavorite = prog?.isFavorite ?: false,
                                    isPlayingAudio = currentlyPlayingWord == entry.text,
                                    studiedCount = prog?.repetitions ?: 0,
                                    onPlayAudio = { onPlayAudio(entry.text, entry.audioUrl) },
                                    onToggleMastered = { onToggleMastered(entry.deck, entry.text) },
                                    onToggleFavorite = { onToggleFavorite(entry.deck, entry.text) },
                                    onOpenDetail = { onOpenDetailedDefinition(entry.deck, entry.text) }
                                )
                            }
                        }

                        if (pagedEntries.loadState.append is LoadState.Loading) {
                            item {
                                LoadingState(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    messageEn = "Loading more words..."
                                )
                            }
                        } else if (pagedEntries.loadState.append is LoadState.Error) {
                            item {
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
        }
    }
}

fun posShortLabel(pos: String): String {
    val clean = pos.trim().lowercase().removeSuffix(".")
    return when (clean) {
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

fun parseArabicMeaningGroups(pos: String, arabic: String): List<Pair<String, String>> {
    val cleanArabic = arabic.trim()
    if (cleanArabic.isEmpty()) return emptyList()

    val rawParts = cleanArabic.split(Regex("[;؛\\n]+"))
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
    val clean = pos.trim().lowercase().removeSuffix(".")
    return when (clean) {
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

/**
 * Trancy-style Word list row:
 * - One row per word
 * - Small count badge (times studied)
 * - Word in bold
 * - "US" chip with speaker icon and IPA
 * - Arabic meanings in ONE compact line grouped by POS tag chips (n., v., adj., adv.; "web." for extra meanings)
 * - Ending with mastered checkbox and heart
 * - "Tap to look up" if no Arabic
 */
@Composable
fun WordBookEntryRow(
    entry: EntryEntity,
    isMastered: Boolean,
    isFavorite: Boolean,
    isPlayingAudio: Boolean,
    studiedCount: Int = 0,
    onPlayAudio: () -> Unit,
    onToggleMastered: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenDetail: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onOpenDetail)
            .testTag("word_row_${entry.id}"),
        color = SurfaceDark,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = 1.dp,
            color = when {
                isFavorite -> GoldAccentBorder
                isMastered -> EmeraldAccent.copy(alpha = 0.4f)
                else -> BorderDark
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Small count badge (times studied)
            Surface(
                color = SurfaceElevatedDark,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(0.5.dp, BorderDark)
            ) {
                Text(
                    text = "$studiedCount",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    ),
                    color = TextMuted,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }

            // Word in bold
            Text(
                text = entry.text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = if (isMastered) EmeraldAccent else TextPrimary,
                maxLines = 1
            )

            // "US" chip with speaker icon and IPA
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onPlayAudio)
                    .testTag("ipa_speaker_${entry.id}"),
                color = if (isPlayingAudio) GoldAccent else GoldAccentDim,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, GoldAccentBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "US",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = if (isPlayingAudio) BackgroundDark else GoldAccent
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Play pronunciation for ${entry.text}",
                        tint = if (isPlayingAudio) BackgroundDark else GoldAccent,
                        modifier = Modifier.size(12.dp)
                    )
                    if (entry.ipa.isNotBlank()) {
                        Text(
                            text = entry.ipa,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Normal,
                                fontSize = 11.sp
                            ),
                            color = if (isPlayingAudio) BackgroundDark else GoldAccent,
                            maxLines = 1
                        )
                    }
                }
            }

            // Arabic meanings in ONE compact line grouped by POS tag chips
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (entry.arabic.isBlank()) {
                    Text(
                        text = "Tap to look up",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontStyle = FontStyle.Italic,
                            fontSize = 12.sp
                        ),
                        color = TextMuted,
                        maxLines = 1
                    )
                } else {
                    val groups = parseArabicMeaningGroups(entry.pos, entry.arabic)
                    groups.forEach { (posChip, arabicText) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Surface(
                                color = GoldAccentDim,
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(0.5.dp, GoldAccentBorder)
                            ) {
                                Text(
                                    text = posChip,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = GoldAccent,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                            BidiText(
                                text = arabicText,
                                style = ArabicTypography.bodySmall.copy(
                                    fontSize = 13.sp,
                                    letterSpacing = 0.sp
                                ),
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fillMaxWidth = false
                            )
                        }
                    }
                }
            }

            // Mastered checkbox
            IconButton(
                onClick = onToggleMastered,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("mastered_checkbox_${entry.id}")
            ) {
                Icon(
                    imageVector = if (isMastered) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                    contentDescription = "Mark mastered",
                    tint = if (isMastered) EmeraldAccent else TextMuted,
                    modifier = Modifier.size(19.dp)
                )
            }

            // Heart icon
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("favorite_heart_${entry.id}")
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite word",
                    tint = if (isFavorite) GoldAccent else TextMuted,
                    modifier = Modifier.size(19.dp)
                )
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
                IconButton(onClick = onToggleMastered, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (card.isMastered) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                        contentDescription = "Toggle mastered",
                        tint = if (card.isMastered) EmeraldAccent else TextSecondary,
                        modifier = Modifier.size(20.dp)
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

            // Section 2: Dual US & GB pronunciation chips
            val isPlayingWord = currentlyPlayingWord == card.text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // US Chip
                val ipaUs = aiDetail?.ipaUs?.ifBlank { card.ipa } ?: card.ipa
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onPlayVoiceAudio(false) }
                        .testTag("pronounce_us_button"),
                    color = if (isPlayingWord) GoldAccent else GoldAccentDim,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, GoldAccentBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (ipaUs.isNotBlank()) "US $ipaUs" else "US",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isPlayingWord) BackgroundDark else GoldAccent
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "US Pronunciation",
                            tint = if (isPlayingWord) BackgroundDark else GoldAccent,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                // GB Chip
                val ipaGb = aiDetail?.ipaGb ?: ""
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onPlayVoiceAudio(true) }
                        .testTag("pronounce_gb_button"),
                    color = GoldAccentDim,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, GoldAccentBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (ipaGb.isNotBlank()) "GB $ipaGb" else "GB",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = GoldAccent
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "GB Pronunciation",
                            tint = GoldAccent,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
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

