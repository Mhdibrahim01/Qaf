package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.ai.AiWordDetailJson
import com.example.data.importer.DataImportRepository
import com.example.data.importer.FlashcardDirection
import com.example.data.importer.GroupedWordCard
import com.example.data.importer.PosSenseVariant
import com.example.data.importer.Sm2Rating
import com.example.data.local.DeckEntity
import com.example.data.local.WordProgressEntity
import com.example.ui.components.ArabicText
import com.example.ui.components.BooksModeSwitcher
import com.example.ui.components.EmptyState
import com.example.ui.components.GoldBadge
import com.example.ui.components.LoadingState
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardsScreen(
    decks: List<DeckEntity>,
    activeDeckId: String,
    cards: List<GroupedWordCard>,
    currentIndex: Int,
    isLoading: Boolean,
    progressMap: Map<String, WordProgressEntity>,
    reviewDueTodayCount: Int,
    studiedTodayCount: Int,
    currentlyPlayingWord: String?,
    detailedCard: GroupedWordCard?,
    isAiTranslating: Boolean,
    savedSentenceKeys: Set<String> = emptySet(),
    aiDetail: AiWordDetailJson? = null,
    isAiDetailLoading: Boolean = false,
    aiDetailError: String? = null,
    canRequestAi: Boolean = true,
    isAllowAi: Boolean = true,
    isOnline: Boolean = true,
    isCardFlipped: Boolean = false,
    cardDirection: FlashcardDirection = FlashcardDirection.ENGLISH_TO_ARABIC,
    autoShowAnswer: Boolean = false,
    onSelectDeck: (String) -> Unit,
    onSwitchToBooks: () -> Unit = {},
    onIndexChanged: (Int) -> Unit,
    onFlipCard: () -> Unit = {},
    onSetCardFlipped: (Boolean) -> Unit = {},
    onUpdateCardDirection: (FlashcardDirection) -> Unit = {},
    onUpdateAutoShowAnswer: (Boolean) -> Unit = {},
    onRateCard: (Sm2Rating) -> Unit,
    onToggleMastered: () -> Unit,
    onToggleFavorite: () -> Unit,
    onPlayAudio: (String, String) -> Unit,
    onPlayVoiceAudio: (String, String, Boolean) -> Unit = { word, url, _ -> onPlayAudio(word, url) },
    onSpeakSentenceTts: (String) -> Unit = {},
    onOpenDetailedDefinition: (String, String) -> Unit,
    onDismissDetailedDefinition: () -> Unit,
    onRetryAiDefinition: (String) -> Unit = {},
    onEnrichWordWithAi: (String, String) -> Unit,
    onSaveUserArabic: (String, String, String, String) -> Unit,
    onToggleSaveSentence: (String, String, String) -> Unit = { _, _, _ -> },
    onToggleCardFavoriteInSheet: (String, String) -> Unit,
    onToggleCardMasteredInSheet: (String, String) -> Unit,
    showCoachTipFlip: Boolean = false,
    showCoachTipRate: Boolean = false,
    onDismissCoachTipFlip: () -> Unit = {},
    onDismissCoachTipRate: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val formatter = remember { NumberFormat.getIntegerInstance(Locale.US) }
    val studyDecks = remember(decks) {
        decks.filter { it.id != DataImportRepository.SAVED_WORDS_DECK_ID && it.rowCount > 0 }
    }

    var showSettingsDialog by remember { mutableStateOf(false) }

    val safeIndex = if (cards.isEmpty()) 0 else currentIndex.coerceIn(0, cards.size - 1)
    val activeCard = cards.getOrNull(safeIndex)
    val activeProgress = activeCard?.let { progressMap[it.cardKey] }
    val intervalPreview = remember(activeProgress) {
        DataImportRepository.computeSm2IntervalPreview(activeProgress)
    }

    // 5-second Auto-show answer timer (off by default; when enabled, flips card automatically after 5s)
    LaunchedEffect(activeCard?.cardKey, safeIndex, isCardFlipped, autoShowAnswer, isLoading) {
        if (autoShowAnswer && !isCardFlipped && !isLoading && activeCard != null) {
            delay(5000)
            onSetCardFlipped(true)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .testTag("screen_flashcards"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 0. Mode Switcher: Word Books | Flashcards
            BooksModeSwitcher(
                isFlashcardsActive = true,
                dueTodayCount = reviewDueTodayCount,
                onSelectBooksMode = onSwitchToBooks,
                onSelectFlashcardsMode = {}
            )

            // 1. Deck selector bar + Due Today chip + Settings button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (reviewDueTodayCount > 0 || activeDeckId == DataImportRepository.DUE_TODAY_DECK_ID) {
                        val isDueSelected = activeDeckId == DataImportRepository.DUE_TODAY_DECK_ID
                        FilterChip(
                            selected = isDueSelected,
                            onClick = { onSelectDeck(DataImportRepository.DUE_TODAY_DECK_ID) },
                            label = {
                                Text(
                                    text = "Due Today ($reviewDueTodayCount)",
                                    fontWeight = if (isDueSelected) FontWeight.Bold else FontWeight.Medium
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
                                selected = isDueSelected,
                                borderColor = BorderDark,
                                selectedBorderColor = GoldAccent
                            ),
                            modifier = Modifier.testTag("flashcard_deck_chip_due_today")
                        )
                    }

                    studyDecks.forEach { deck ->
                        val isSelected = deck.id == activeDeckId
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectDeck(deck.id) },
                            label = {
                                Text(
                                    text = deck.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
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
                            modifier = Modifier.testTag("flashcard_deck_chip_${deck.id}")
                        )
                    }
                }

                // Flashcards Settings Button (Card direction + Auto-show answer)
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showSettingsDialog = true }
                        .testTag("flashcard_settings_button"),
                    color = if (cardDirection != FlashcardDirection.ENGLISH_TO_ARABIC || autoShowAnswer) {
                        GoldAccentDim
                    } else {
                        SurfaceDark
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        if (cardDirection != FlashcardDirection.ENGLISH_TO_ARABIC || autoShowAnswer) {
                            GoldAccent
                        } else {
                            BorderDark
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Flashcard settings",
                            tint = GoldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        if (autoShowAnswer) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Auto-show answer active",
                                tint = GoldAccent,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // 2. Top Counter Row: "N / total" with current number in gold + Prev/Next controls
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceDark,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, BorderDark)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (safeIndex > 0) onIndexChanged(safeIndex - 1) },
                            enabled = safeIndex > 0,
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("flashcard_prev_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Previous card",
                                tint = if (safeIndex > 0) TextPrimary else TextMuted
                            )
                        }

                        // "N / total" counter with current number in Gold (#F5C542)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(
                                        SpanStyle(
                                            color = GoldAccent,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 18.sp
                                        )
                                    ) {
                                        append(if (cards.isEmpty()) "0" else formatter.format(safeIndex + 1))
                                    }
                                    withStyle(
                                        SpanStyle(
                                            color = TextSecondary,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 15.sp
                                        )
                                    ) {
                                        append(" / ${formatter.format(cards.size)}")
                                    }
                                },
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.testTag("flashcard_counter")
                            )

                            if (reviewDueTodayCount > 0) {
                                GoldBadge(text = "Due: $reviewDueTodayCount")
                            }
                        }

                        IconButton(
                            onClick = { if (safeIndex + 1 < cards.size) onIndexChanged(safeIndex + 1) },
                            enabled = safeIndex + 1 < cards.size,
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("flashcard_next_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Next card",
                                tint = if (safeIndex + 1 < cards.size) TextPrimary else TextMuted
                            )
                        }
                    }

                    val progressFraction = if (cards.isNotEmpty()) {
                        ((safeIndex + 1).toFloat() / cards.size.toFloat()).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = GoldAccent,
                        trackColor = SurfaceElevatedDark
                    )
                }
            }

            // 3. Main Swipeable 3D Flip Card Area
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingState(
                        messageEn = "Loading flashcards...",
                        messageAr = "جاري تجهيز البطاقات..."
                    )
                }
            } else if (cards.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        titleEn = if (activeDeckId == DataImportRepository.DUE_TODAY_DECK_ID) {
                            "All due reviews completed for today!"
                        } else {
                            "No cards in this deck yet"
                        },
                        titleAr = if (activeDeckId == DataImportRepository.DUE_TODAY_DECK_ID) {
                            "تم إكمال جميع مراجعات اليوم بنجاح!"
                        } else {
                            "لا توجد بطاقات في هذا الكتاب بعد"
                        },
                        descriptionEn = "Select Oxford 5000 or another Word Book above to study.",
                        icon = Icons.AutoMirrored.Filled.MenuBook
                    )
                }
            } else {
                val pagerState = rememberPagerState(
                    initialPage = safeIndex,
                    pageCount = { cards.size }
                )

                // Keep pager synced when currentIndex changes via SM-2 rating or restored state
                LaunchedEffect(safeIndex, cards.size) {
                    if (pagerState.currentPage != safeIndex && safeIndex in cards.indices) {
                        pagerState.scrollToPage(safeIndex)
                    }
                }

                // Notify ViewModel when user swipes left/right
                LaunchedEffect(pagerState) {
                    snapshotFlow { pagerState.settledPage }
                        .distinctUntilChanged()
                        .collect { page ->
                            if (page != safeIndex && page in cards.indices) {
                                onIndexChanged(page)
                            }
                        }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("flashcard_pager"),
                    pageSpacing = 14.dp,
                    key = { index -> cards.getOrNull(index)?.cardKey ?: index }
                ) { pageIndex ->
                    val card = cards[pageIndex]
                    val cardProgress = progressMap[card.cardKey]
                    val isMastered = cardProgress?.isMastered ?: card.isMastered
                    val isFavorite = cardProgress?.isFavorite ?: card.isFavorite
                    val isCurrentPage = pageIndex == safeIndex
                    val pageFlipped = if (isCurrentPage) isCardFlipped else false

                    // Resolve effective direction for this card (supports Mixed mode deterministically)
                    val effectiveDirection = remember(card.cardKey, cardDirection, pageIndex) {
                        when (cardDirection) {
                            FlashcardDirection.ENGLISH_TO_ARABIC -> FlashcardDirection.ENGLISH_TO_ARABIC
                            FlashcardDirection.ARABIC_TO_ENGLISH -> FlashcardDirection.ARABIC_TO_ENGLISH
                            FlashcardDirection.MIXED -> {
                                if (abs(card.cardKey.hashCode() + pageIndex) % 2 == 1) {
                                    FlashcardDirection.ARABIC_TO_ENGLISH
                                } else {
                                    FlashcardDirection.ENGLISH_TO_ARABIC
                                }
                            }
                        }
                    }

                    FlipFlashcardItem(
                        card = card,
                        isFlipped = pageFlipped,
                        effectiveDirection = effectiveDirection,
                        autoShowAnswer = autoShowAnswer,
                        isMastered = isMastered,
                        isFavorite = isFavorite,
                        isPlayingAudio = currentlyPlayingWord == card.text,
                        savedSentenceKeys = savedSentenceKeys,
                        onFlipCard = onFlipCard,
                        onPlayAudio = {
                            val audioUrl = card.posVariants.firstOrNull { it.audioUrl.isNotBlank() }?.audioUrl.orEmpty()
                            onPlayAudio(card.text, audioUrl)
                        },
                        onToggleFavorite = onToggleFavorite,
                        onToggleSaveSentence = { en, ar ->
                            onToggleSaveSentence(en, ar, card.text)
                        },
                        onOpenDetailedDefinition = {
                            onOpenDetailedDefinition(card.deck, card.text)
                        }
                    )
                }

                // First-session Coach Tip guidance (dismissible on tap or flip/rate)
                if (showCoachTipFlip && !isCardFlipped) {
                    com.example.ui.components.CoachTipBanner(
                        icon = Icons.Filled.Flip,
                        messageEn = "Tap card anywhere to reveal Arabic meaning & examples",
                        messageAr = "اضغط على البطاقة لقلبها وإظهار المعنى والأمثلة",
                        testTag = "coach_tip_flip",
                        onDismiss = onDismissCoachTipFlip,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                } else if (showCoachTipRate && isCardFlipped) {
                    com.example.ui.components.CoachTipBanner(
                        icon = Icons.Filled.Timer,
                        messageEn = "Rate your recall (Again, Hard, Good, Easy) to schedule spaced repetition",
                        messageAr = "قيّم حفظك لجدولة المراجعة التكرارية",
                        testTag = "coach_tip_rate",
                        onDismiss = onDismissCoachTipRate,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                // 4. Bottom Action Bar:
                // Mark Mastered & Detailed Definition stay ALWAYS visible.
                // Again / Hard / Good / Easy (with intervals) appear ONLY after the card is flipped!
                if (activeCard != null) {
                    val isCurrentMastered = activeProgress?.isMastered ?: activeCard.isMastered
                    FlashcardBottomActionsBar(
                        isCardFlipped = isCardFlipped,
                        isMastered = isCurrentMastered,
                        intervalPreview = intervalPreview,
                        onToggleMastered = onToggleMastered,
                        onOpenDetailedDefinition = {
                            onOpenDetailedDefinition(activeCard.deck, activeCard.text)
                        },
                        onRateCard = onRateCard
                    )
                }
            }
        }
    }

    // Flashcard Settings Dialog (Card direction + Auto-show answer)
    if (showSettingsDialog) {
        FlashcardSettingsDialog(
            cardDirection = cardDirection,
            autoShowAnswer = autoShowAnswer,
            onSelectDirection = onUpdateCardDirection,
            onToggleAutoShowAnswer = onUpdateAutoShowAnswer,
            onDismiss = { showSettingsDialog = false }
        )
    }

    // Detailed Definition Bottom Sheet (shared with Word Books)
    if (detailedCard != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onDismissDetailedDefinition,
            sheetState = sheetState,
            containerColor = SurfaceDark,
            contentColor = TextPrimary
        ) {
            WordDetailedDefinitionSheet(
                card = detailedCard,
                isPlaying = currentlyPlayingWord == detailedCard.text,
                isAiTranslating = isAiTranslating,
                savedSentenceKeys = savedSentenceKeys,
                aiDetail = aiDetail,
                isAiDetailLoading = isAiDetailLoading,
                aiDetailError = aiDetailError,
                canRequestAi = canRequestAi,
                isAllowAi = isAllowAi,
                isOnline = isOnline,
                onDismiss = onDismissDetailedDefinition,
                onRetryAiDefinition = { onRetryAiDefinition(detailedCard.text) },
                onPlayAudio = {
                    val url = detailedCard.posVariants.firstOrNull { it.audioUrl.isNotBlank() }?.audioUrl ?: ""
                    onPlayAudio(detailedCard.text, url)
                },
                onPlayVoiceAudio = { isGb ->
                    val url = detailedCard.posVariants.firstOrNull { it.audioUrl.isNotBlank() }?.audioUrl ?: ""
                    onPlayVoiceAudio(detailedCard.text, url, isGb)
                },
                onSpeakSentenceTts = onSpeakSentenceTts,
                onEnrichWithAi = { onEnrichWordWithAi(detailedCard.deck, detailedCard.text) },
                onSaveUserArabic = { entryId, userArabic ->
                    onSaveUserArabic(entryId, detailedCard.deck, detailedCard.text, userArabic)
                },
                onToggleSaveSentence = { en, ar ->
                    onToggleSaveSentence(en, ar, detailedCard.text)
                },
                onToggleFavorite = { onToggleCardFavoriteInSheet(detailedCard.deck, detailedCard.text) },
                onToggleMastered = { onToggleCardMasteredInSheet(detailedCard.deck, detailedCard.text) }
            )
        }
    }
}

/**
 * 3D Flip Card Composable:
 * - Tap anywhere on the card surface to flip between Front and Back with a smooth 3D Y-axis rotation.
 * - Tapping Audio, Heart, or Info buttons consumes its own click event and does NOT flip the card.
 */
@Composable
private fun FlipFlashcardItem(
    card: GroupedWordCard,
    isFlipped: Boolean,
    effectiveDirection: FlashcardDirection,
    autoShowAnswer: Boolean,
    isMastered: Boolean,
    isFavorite: Boolean,
    isPlayingAudio: Boolean,
    savedSentenceKeys: Set<String>,
    onFlipCard: () -> Unit,
    onPlayAudio: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleSaveSentence: (String, String) -> Unit,
    onOpenDetailedDefinition: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "flashcard3dFlip"
    )
    val showingBack = rotation > 90f

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 14f * density
            }
            .clip(RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onFlipCard
            )
            .testTag("flashcard_card_${card.text}"),
        color = SurfaceDark,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isMastered) EmeraldAccent.copy(alpha = 0.65f) else GoldAccentBorder
        )
    ) {
        if (!showingBack) {
            FlashcardFrontFace(
                card = card,
                effectiveDirection = effectiveDirection,
                autoShowAnswer = autoShowAnswer,
                isFavorite = isFavorite,
                isPlayingAudio = isPlayingAudio,
                onPlayAudio = onPlayAudio,
                onToggleFavorite = onToggleFavorite,
                onOpenDetailedDefinition = onOpenDetailedDefinition
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f }
            ) {
                FlashcardBackFace(
                    card = card,
                    effectiveDirection = effectiveDirection,
                    isMastered = isMastered,
                    isFavorite = isFavorite,
                    isPlayingAudio = isPlayingAudio,
                    savedSentenceKeys = savedSentenceKeys,
                    onPlayAudio = onPlayAudio,
                    onToggleFavorite = onToggleFavorite,
                    onToggleSaveSentence = onToggleSaveSentence,
                    onOpenDetailedDefinition = onOpenDetailedDefinition
                )
            }
        }
    }
}

/**
 * FRONT of the Flip Card:
 * - English-to-Arabic: ONLY the English word (large), CEFR level badge (hidden if empty),
 *   audio button (tapping audio does NOT flip the card), heart and info icons.
 * - Arabic-to-English: Shows the Arabic meaning on the front (or "No Arabic yet" if empty),
 *   plus CEFR level badge, audio button, heart and info icons.
 */
@Composable
private fun FlashcardFrontFace(
    card: GroupedWordCard,
    effectiveDirection: FlashcardDirection,
    autoShowAnswer: Boolean,
    isFavorite: Boolean,
    isPlayingAudio: Boolean,
    onPlayAudio: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenDetailedDefinition: () -> Unit
) {
    val variantsWithArabic = remember(card.posVariants) {
        card.posVariants.filter { it.arabicMeaning.isNotBlank() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SurfaceElevatedDark.copy(alpha = 0.65f),
                        SurfaceDark
                    )
                )
            )
            .padding(22.dp)
            .testTag("flashcard_front_face"),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Row: CEFR Level Badge (hidden if empty) + Heart & Info icons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                if (card.primaryLevel.isNotBlank()) {
                    GoldBadge(
                        text = card.primaryLevel,
                        modifier = Modifier.testTag("flashcard_level_badge")
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("flashcard_favorite_button")
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite word",
                        tint = if (isFavorite) GoldAccent else TextSecondary
                    )
                }
                IconButton(
                    onClick = onOpenDetailedDefinition,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("flashcard_detail_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Detailed definition",
                        tint = GoldAccent
                    )
                }
            }
        }

        // Center Content: Large Word (or Arabic meaning in Arabic-to-English mode) + Audio Button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            if (effectiveDirection == FlashcardDirection.ARABIC_TO_ENGLISH) {
                if (variantsWithArabic.isNotEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.testTag("flashcard_front_arabic_content")
                    ) {
                        variantsWithArabic.forEach { variant ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (variant.pos.isNotBlank()) {
                                    GoldBadge(text = variant.pos)
                                }
                                ArabicText(
                                    text = variant.arabicMeaning,
                                    style = ArabicTypography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                } else {
                    Surface(
                        color = SurfaceElevatedDark,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BorderDark),
                        modifier = Modifier.testTag("flashcard_no_arabic_label")
                    ) {
                        Text(
                            text = "No Arabic yet",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            } else {
                Text(
                    text = card.text,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("flashcard_word_text")
                )
            }

            // Audio Button — consumes its own click so tapping audio NEVER flips the card
            IconButton(
                onClick = onPlayAudio,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (isPlayingAudio) GoldAccent else GoldAccentDim)
                    .testTag("flashcard_front_audio_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Play pronunciation",
                    tint = if (isPlayingAudio) BackgroundDark else GoldAccent,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // Bottom subtle flip prompt
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Flip,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = if (autoShowAnswer) "Tap to flip • Auto-flips in 5s" else "Tap anywhere on card to flip",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}

/**
 * BACK of the Flip Card:
 * - Arabic meanings grouped by part of speech (or a small "No Arabic yet" label if empty).
 * - In Arabic-to-English mode, also prominently displays the English word.
 * - IPA chip with speaker (hidden if ipa is empty; audio icon in top bar stays available).
 * - Examples (`examples_en` with target word highlighted in gold, `examples_ar` below; hidden if empty).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FlashcardBackFace(
    card: GroupedWordCard,
    effectiveDirection: FlashcardDirection,
    isMastered: Boolean,
    isFavorite: Boolean,
    isPlayingAudio: Boolean,
    savedSentenceKeys: Set<String>,
    onPlayAudio: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleSaveSentence: (String, String) -> Unit,
    onOpenDetailedDefinition: () -> Unit
) {
    val variantsWithArabic = remember(card.posVariants) {
        card.posVariants.filter { it.arabicMeaning.isNotBlank() }
    }

    // Collect up to 2 distinct examples across variants so examples are shown whenever present
    val examplePairs = remember(card.posVariants) {
        val list = mutableListOf<Pair<String, String>>()
        val seen = HashSet<String>()
        for (variant in card.posVariants) {
            variant.examplesEn.forEachIndexed { idx, en ->
                val cleanEn = en.trim()
                if (cleanEn.isNotEmpty() && seen.add(cleanEn.lowercase())) {
                    val cleanAr = variant.examplesAr.getOrNull(idx)?.trim().orEmpty()
                    list.add(cleanEn to cleanAr)
                }
            }
        }
        list.take(2)
    }

    val primaryDefinitionEn = remember(card.posVariants) {
        card.posVariants.firstOrNull { it.definitionEn.isNotBlank() }?.definitionEn.orEmpty()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SurfaceElevatedDark.copy(alpha = 0.75f),
                        SurfaceDark
                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .testTag("flashcard_back_face"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Back Top Row: CEFR Level badge (if non-empty) + English Word reference + Audio, Heart & Info icons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (card.primaryLevel.isNotBlank()) {
                    GoldBadge(
                        text = card.primaryLevel,
                        modifier = Modifier.testTag("flashcard_level_badge")
                    )
                }
                if (isMastered) {
                    Surface(
                        color = EmeraldAccent.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "Mastered",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldAccent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPlayAudio,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("flashcard_back_audio_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Play pronunciation",
                        tint = if (isPlayingAudio) GoldAccent else TextSecondary
                    )
                }
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("flashcard_favorite_button")
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite word",
                        tint = if (isFavorite) GoldAccent else TextSecondary
                    )
                }
                IconButton(
                    onClick = onOpenDetailedDefinition,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("flashcard_detail_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Detailed definition",
                        tint = GoldAccent
                    )
                }
            }
        }

        // English Word Title on Back (prominent in Arabic-to-English mode, compact reference in English-to-Arabic)
        Text(
            text = card.text,
            style = if (effectiveDirection == FlashcardDirection.ARABIC_TO_ENGLISH) {
                MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold)
            } else {
                MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            },
            color = TextPrimary,
            modifier = Modifier.testTag("flashcard_word_text")
        )

        // 1. Arabic meanings grouped by part of speech (or small "No Arabic yet" label if empty)
        if (variantsWithArabic.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("flashcard_arabic_section"),
                color = SurfaceElevatedDark,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, GoldAccentBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val groupedByPos = variantsWithArabic.groupBy { it.pos.trim() }
                    groupedByPos.forEach { (pos, posVariants) ->
                        val combinedArabic = posVariants
                            .map { it.arabicMeaning.trim() }
                            .filter { it.isNotEmpty() }
                            .distinct()
                            .joinToString(" • ")

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (pos.isNotBlank()) {
                                GoldBadge(text = pos)
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                ArabicText(
                                    text = combinedArabic,
                                    style = ArabicTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Surface(
                modifier = Modifier.testTag("flashcard_no_arabic_label"),
                color = SurfaceElevatedDark,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, BorderDark)
            ) {
                Text(
                    text = "No Arabic yet",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        // 2. IPA chip with speaker — hidden if card.ipa is empty!
        if (card.ipa.isNotBlank()) {
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onPlayAudio)
                    .testTag("flashcard_ipa_speaker_chip"),
                color = if (isPlayingAudio) GoldAccent else GoldAccentDim,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, GoldAccentBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Play pronunciation",
                        tint = if (isPlayingAudio) BackgroundDark else GoldAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "US ${card.ipa}",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (isPlayingAudio) BackgroundDark else GoldAccent,
                        modifier = Modifier.testTag("flashcard_ipa_text")
                    )
                }
            }
        }

        // Optional English Definition — hidden if empty!
        if (primaryDefinitionEn.isNotBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("flashcard_definition_section"),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Definition",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = GoldAccent
                )
                Text(
                    text = primaryDefinitionEn,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )
            }
        }

        // 3. Examples (examples_en with target word highlighted in gold, examples_ar below) — hidden if empty!
        if (examplePairs.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("flashcard_examples_section"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Examples",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = GoldAccent
                    )
                    Text(
                        text = "Long-press or bookmark to save",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }

                examplePairs.forEachIndexed { idx, (sentenceEn, sentenceAr) ->
                    val isSaved = sentenceEn.trim().lowercase() in savedSentenceKeys
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .combinedClickable(
                                onClick = {},
                                onLongClick = { onToggleSaveSentence(sentenceEn, sentenceAr) }
                            )
                            .testTag("flashcard_example_card_$idx"),
                        color = SurfaceElevatedDark,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isSaved) GoldAccentBorder else BorderDark)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = highlightWordInGold(sentenceEn, card.text),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                                // Hide examples_ar if empty
                                if (sentenceAr.isNotEmpty()) {
                                    ArabicText(
                                        text = sentenceAr,
                                        style = ArabicTypography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                            IconButton(
                                onClick = { onToggleSaveSentence(sentenceEn, sentenceAr) },
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("flashcard_bookmark_example_$idx")
                            ) {
                                Icon(
                                    imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = if (isSaved) "Remove saved sentence" else "Save sentence",
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

/**
 * Bottom Action Bar:
 * - Mark Mastered and Detailed Definition stay ALWAYS visible.
 * - Again / Hard / Good / Easy buttons (with their intervals) appear ONLY after the card is flipped.
 */
@Composable
private fun FlashcardBottomActionsBar(
    isCardFlipped: Boolean,
    isMastered: Boolean,
    intervalPreview: com.example.data.importer.Sm2IntervalPreview,
    onToggleMastered: () -> Unit,
    onOpenDetailedDefinition: () -> Unit,
    onRateCard: (Sm2Rating) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: Mark Mastered + Detailed Definition (ALWAYS visible)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onToggleMastered)
                    .testTag("flashcard_mastered_button"),
                color = if (isMastered) EmeraldAccent.copy(alpha = 0.2f) else SurfaceDark,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    if (isMastered) EmeraldAccent else BorderDark
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isMastered) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                        contentDescription = "Mastered",
                        tint = if (isMastered) EmeraldAccent else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isMastered) "Mastered ✓" else "Mark Mastered",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isMastered) EmeraldAccent else TextPrimary,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenDetailedDefinition)
                    .testTag("flashcard_ai_definition_button"),
                color = SurfaceDark,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, GoldAccentBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Word Details",
                        tint = GoldAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Word Details",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = GoldAccent,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Row 2: SM-2 Spaced Repetition Buttons (Again / Hard / Good / Easy)
        // Appear ONLY after the card is flipped!
        AnimatedVisibility(
            visible = isCardFlipped,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("flashcard_sm2_row"),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Sm2RateButton(
                    label = "Again",
                    intervalText = intervalPreview.againLabel,
                    accentColor = CoralAccent,
                    testTag = "sm2_button_again",
                    onClick = { onRateCard(Sm2Rating.AGAIN) },
                    modifier = Modifier.weight(1f)
                )
                Sm2RateButton(
                    label = "Hard",
                    intervalText = intervalPreview.hardLabel,
                    accentColor = Color(0xFFF59E0B),
                    testTag = "sm2_button_hard",
                    onClick = { onRateCard(Sm2Rating.HARD) },
                    modifier = Modifier.weight(1f)
                )
                Sm2RateButton(
                    label = "Good",
                    intervalText = intervalPreview.goodLabel,
                    accentColor = GoldAccent,
                    testTag = "sm2_button_good",
                    onClick = { onRateCard(Sm2Rating.GOOD) },
                    modifier = Modifier.weight(1f)
                )
                Sm2RateButton(
                    label = "Easy",
                    intervalText = intervalPreview.easyLabel,
                    accentColor = EmeraldAccent,
                    testTag = "sm2_button_easy",
                    onClick = { onRateCard(Sm2Rating.EASY) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FlashcardSettingsDialog(
    cardDirection: FlashcardDirection,
    autoShowAnswer: Boolean,
    onSelectDirection: (FlashcardDirection) -> Unit,
    onToggleAutoShowAnswer: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("flashcard_settings_dialog"),
            color = SurfaceDark,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, GoldAccentBorder)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Flashcard Settings",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close settings",
                            tint = TextSecondary
                        )
                    }
                }

                HorizontalDivider(color = BorderDark)

                // Setting 1: Card direction (English to Arabic, Arabic to English, Mixed)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Card direction",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = GoldAccent
                    )

                    FlashcardDirection.entries.forEach { direction ->
                        val isSelected = direction == cardDirection
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectDirection(direction) }
                                .testTag("direction_option_${direction.storageKey}"),
                            color = if (isSelected) GoldAccentDim else SurfaceElevatedDark,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (isSelected) GoldAccent else BorderDark)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = direction.labelEn,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (isSelected) GoldAccent else TextPrimary
                                    )
                                    ArabicText(
                                        text = direction.labelAr,
                                        style = ArabicTypography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onSelectDirection(direction) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = GoldAccent,
                                        unselectedColor = TextMuted
                                    )
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = BorderDark)

                // Setting 2: Auto-show answer (off by default, flips after 5 seconds)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleAutoShowAnswer(!autoShowAnswer) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Auto-show answer",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = "Flips the card automatically after 5 seconds",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = autoShowAnswer,
                        onCheckedChange = onToggleAutoShowAnswer,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BackgroundDark,
                            checkedTrackColor = GoldAccent,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = SurfaceElevatedDark
                        ),
                        modifier = Modifier.testTag("flashcard_auto_show_switch")
                    )
                }
            }
        }
    }
}

@Composable
private fun Sm2RateButton(
    label: String,
    intervalText: String,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        color = SurfaceDark,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = intervalText,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = TextSecondary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = accentColor
            )
        }
    }
}

private fun highlightWordInGold(sentence: String, rawWord: String) = buildAnnotatedString {
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
