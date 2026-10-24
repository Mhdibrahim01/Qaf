package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ThumbUp
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalConfiguration
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
import com.example.ui.components.SlowAudioTurtleButton
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
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sign

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    onPlayAudioSlow: (String, String) -> Unit = { word, url -> onPlayAudio(word, url) },
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
    waitingLaterTodayCount: Int = 0,
    nextWaitingDueTimestamp: Long? = null,
    modifier: Modifier = Modifier
) {
    val formatter = remember { NumberFormat.getIntegerInstance(Locale.US) }
    val studyDecks = remember(decks) {
        decks.filter { it.id != DataImportRepository.SAVED_WORDS_DECK_ID && it.rowCount > 0 }
    }
    val safeIndex = if (cards.isEmpty()) 0 else currentIndex.coerceIn(0, cards.size - 1)
    val activeCard = cards.getOrNull(safeIndex)
    val activeProgress = activeCard?.let { progressMap[it.cardKey] }
    val intervalPreview = remember(activeProgress) {
        DataImportRepository.computeSm2IntervalPreview(activeProgress)
    }

    var showSettingsDialog by remember { mutableStateOf(false) }
    var isDistractionFreeMode by remember { mutableStateOf(false) }
    var isAnimatingRating by remember { mutableStateOf(false) }
    var ratingStamp by remember { mutableStateOf<Sm2Rating?>(null) }
    var ratingCardSnapshot by remember { mutableStateOf<GroupedWordCard?>(null) }
    var flippedCardKey by remember { mutableStateOf<String?>(null) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var cardTransitionToken by remember { mutableIntStateOf(0) }
    var lastActiveCardKey by remember { mutableStateOf(activeCard?.cardKey) }
    val coroutineScope = rememberCoroutineScope()
    val translationX = remember { Animatable(0f) }
    val translationY = remember { Animatable(0f) }
    val rotationZ = remember { Animatable(0f) }
    val cardAlpha = remember { Animatable(1f) }
    val cardScale = remember { Animatable(1f) }
    val stampScale = remember { Animatable(0.6f) }
    val stampAlpha = remember { Animatable(0f) }
    val density = LocalDensity.current
    val hardExitOffsetY = with(density) { 150.dp.toPx() }
    val easyExitOffsetY = with(density) { -150.dp.toPx() }
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val isActiveCardFlipped = activeCard?.cardKey != null &&
        flippedCardKey == activeCard.cardKey && isCardFlipped
    val displayedCard = ratingCardSnapshot ?: activeCard

    LaunchedEffect(activeCard?.cardKey, isCardFlipped) {
        if (!isCardFlipped) {
            flippedCardKey = null
        } else if (flippedCardKey == null) {
            flippedCardKey = activeCard?.cardKey
        }
    }

    // 5-second Auto-show answer timer (off by default; when enabled, flips card automatically after 5s)
    LaunchedEffect(activeCard?.cardKey, safeIndex, isActiveCardFlipped, autoShowAnswer, isLoading) {
        if (autoShowAnswer && !isActiveCardFlipped && !isLoading && activeCard != null) {
            delay(5000)
            onSetCardFlipped(true)
        }
    }

    val handleRateCard: (Sm2Rating) -> Unit = { rating ->
        val cardBeingRated = activeCard
        if (isActiveCardFlipped && !isAnimatingRating && cardBeingRated != null) {
            isAnimatingRating = true
            ratingCardSnapshot = cardBeingRated
            ratingStamp = rating
            val initialDragOffset = dragOffsetX
            coroutineScope.launch {
                var ratingSubmitted = false
                try {
                    translationX.snapTo(initialDragOffset)
                    dragOffsetX = 0f
                    translationY.snapTo(0f)
                    rotationZ.snapTo(0f)
                    cardAlpha.snapTo(1f)
                    stampScale.snapTo(0.6f)
                    stampAlpha.snapTo(0f)
                    val exitX = when (rating) {
                        Sm2Rating.AGAIN -> -screenWidthPx
                        Sm2Rating.HARD -> -screenWidthPx * 0.6f
                        Sm2Rating.GOOD, Sm2Rating.EASY -> screenWidthPx
                    }
                    val exitY = when (rating) {
                        Sm2Rating.HARD -> hardExitOffsetY
                        Sm2Rating.EASY -> easyExitOffsetY
                        else -> 0f
                    }
                    val exitRotation = when (rating) {
                        Sm2Rating.AGAIN -> -12f
                        Sm2Rating.HARD -> -6f
                        Sm2Rating.GOOD -> 10f
                        Sm2Rating.EASY -> 14f
                    }
                    launch {
                        stampScale.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = 1000f
                            )
                        )
                    }
                    launch { stampAlpha.animateTo(1f, tween(durationMillis = 120)) }
                    val exitAnimation = launch {
                        translationX.animateTo(exitX, tween(durationMillis = 280, easing = FastOutLinearInEasing))
                    }
                    val verticalAnimation = launch {
                        translationY.animateTo(exitY, tween(durationMillis = 280, easing = FastOutLinearInEasing))
                    }
                    val rotationAnimation = launch {
                        rotationZ.animateTo(exitRotation, tween(durationMillis = 280, easing = FastOutLinearInEasing))
                    }
                    val fadeAnimation = launch {
                        cardAlpha.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(durationMillis = 150, delayMillis = 130, easing = FastOutLinearInEasing)
                        )
                    }
                    exitAnimation.join()
                    verticalAnimation.join()
                    rotationAnimation.join()
                    fadeAnimation.join()

                    onSetCardFlipped(false)
                    flippedCardKey = null
                    onRateCard(rating)
                    ratingSubmitted = true
                } finally {
                    withContext(NonCancellable) {
                        translationX.snapTo(0f)
                        translationY.snapTo(0f)
                        rotationZ.snapTo(0f)
                        cardAlpha.snapTo(1f)
                        stampScale.snapTo(0.6f)
                        stampAlpha.snapTo(0f)
                    }
                    dragOffsetX = 0f
                    ratingStamp = null
                    ratingCardSnapshot = null
                    if (ratingSubmitted) cardTransitionToken++
                    isAnimatingRating = false
                    if (!ratingSubmitted) {
                        onSetCardFlipped(false)
                        flippedCardKey = null
                    }
                }
            }
        }
    }

    LaunchedEffect(activeCard?.cardKey, cardTransitionToken) {
        val activeCardKey = activeCard?.cardKey
        if (activeCardKey != lastActiveCardKey || cardTransitionToken > 0) {
            lastActiveCardKey = activeCardKey
            translationX.snapTo(0f)
            translationY.snapTo(0f)
            rotationZ.snapTo(0f)
            cardAlpha.snapTo(1f)
            dragOffsetX = 0f
            ratingStamp = null
            stampScale.snapTo(0.6f)
            stampAlpha.snapTo(0f)
            if (activeCard != null) {
                cardScale.snapTo(0.92f)
                cardScale.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = 450f))
            } else {
                cardScale.snapTo(1f)
            }
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
            // 0. Mode Switcher & Deck Selector (AnimatedVisibility for Distraction-Free Mode)
            AnimatedVisibility(visible = !isDistractionFreeMode) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    BooksModeSwitcher(
                        isFlashcardsActive = true,
                        dueTodayCount = reviewDueTodayCount,
                        onSelectBooksMode = { if (!isAnimatingRating) onSwitchToBooks() },
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
                                    onClick = { if (!isAnimatingRating) onSelectDeck(DataImportRepository.DUE_TODAY_DECK_ID) },
                                    enabled = !isAnimatingRating,
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
                                        enabled = !isAnimatingRating,
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
                                    onClick = { if (!isAnimatingRating) onSelectDeck(deck.id) },
                                    enabled = !isAnimatingRating,
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
                                        enabled = !isAnimatingRating,
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
                                .clickable(enabled = !isAnimatingRating) { showSettingsDialog = true }
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
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.04f),
                            border = BorderStroke(1.dp, BorderDark),
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(
                                    enabled = safeIndex > 0 && !isAnimatingRating,
                                    onClick = { if (safeIndex > 0 && !isAnimatingRating) onIndexChanged(safeIndex - 1) }
                                )
                                .testTag("flashcard_prev_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "Previous card",
                                    tint = if (safeIndex > 0) TextPrimary else TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Center: "N / total" counter + Focus pill badge together in the center
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(
                                        SpanStyle(
                                            color = GoldAccent,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 17.sp
                                        )
                                    ) {
                                        append(if (cards.isEmpty()) "0" else formatter.format(safeIndex + 1))
                                    }
                                    withStyle(
                                        SpanStyle(
                                            color = TextSecondary,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        )
                                    ) {
                                        append(" / ${formatter.format(cards.size)}")
                                    }
                                },
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.testTag("flashcard_counter")
                            )

                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { isDistractionFreeMode = !isDistractionFreeMode }
                                    .testTag("distraction_free_toggle_btn"),
                                color = if (isDistractionFreeMode) GoldAccentDim else SurfaceElevatedDark,
                                border = BorderStroke(1.dp, if (isDistractionFreeMode) GoldAccent else BorderDark)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Toggle Focus Mode",
                                        tint = GoldAccent,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = if (isDistractionFreeMode) "Exit Focus" else "Focus",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = GoldAccent
                                        )
                                    )
                                }
                            }

                            if (reviewDueTodayCount > 0) {
                                GoldBadge(text = "Due: $reviewDueTodayCount")
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.04f),
                            border = BorderStroke(1.dp, BorderDark),
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(
                                    enabled = safeIndex + 1 < cards.size && !isAnimatingRating,
                                    onClick = { if (safeIndex + 1 < cards.size && !isAnimatingRating) onIndexChanged(safeIndex + 1) }
                                )
                                .testTag("flashcard_next_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Next card",
                                    tint = if (safeIndex + 1 < cards.size) TextPrimary else TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
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
                val waitingMinutes = remember(nextWaitingDueTimestamp) {
                    val nextTs = nextWaitingDueTimestamp ?: return@remember null
                    val diffMs = (nextTs - System.currentTimeMillis()).coerceAtLeast(60_000L)
                    (diffMs / 60_000L).toInt().coerceAtLeast(1)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        titleEn = if (activeDeckId == DataImportRepository.DUE_TODAY_DECK_ID) {
                            if (waitingLaterTodayCount > 0) {
                                "All due-now reviews done! ($waitingLaterTodayCount scheduled for later today)"
                            } else {
                                "All due reviews completed for today!"
                            }
                        } else {
                            "No cards in this deck yet"
                        },
                        titleAr = if (activeDeckId == DataImportRepository.DUE_TODAY_DECK_ID) {
                            if (waitingLaterTodayCount > 0) {
                                "تم إكمال المراجعات المستحقة الآن! ($waitingLaterTodayCount بطاقة مجدولة لوقت لاحق اليوم)"
                            } else {
                                "تم إكمال جميع مراجعات اليوم بنجاح!"
                            }
                        } else {
                            "لا توجد بطاقات في هذا الكتاب بعد"
                        },
                        descriptionEn = if (activeDeckId == DataImportRepository.DUE_TODAY_DECK_ID && waitingLaterTodayCount > 0 && waitingMinutes != null) {
                            "Short-interval cards are hidden until their timer finishes (~${waitingMinutes}m). Or select a Word Book above to study."
                        } else {
                            "Select Oxford 5000 or another Word Book above to study."
                        },
                        icon = Icons.AutoMirrored.Filled.MenuBook
                    )
                }
            } else {
                val card = displayedCard
                if (card != null) {
                    val cardProgress = progressMap[card.cardKey]
                    val isMastered = cardProgress?.isMastered ?: card.isMastered
                    val isFavorite = cardProgress?.isFavorite ?: card.isFavorite
                    val effectiveDirection = remember(card.cardKey, cardDirection, safeIndex) {
                        when (cardDirection) {
                            FlashcardDirection.ENGLISH_TO_ARABIC -> FlashcardDirection.ENGLISH_TO_ARABIC
                            FlashcardDirection.ARABIC_TO_ENGLISH -> FlashcardDirection.ARABIC_TO_ENGLISH
                            FlashcardDirection.MIXED -> {
                                if (abs(card.cardKey.hashCode() + safeIndex) % 2 == 1) {
                                    FlashcardDirection.ARABIC_TO_ENGLISH
                                } else {
                                    FlashcardDirection.ENGLISH_TO_ARABIC
                                }
                            }
                        }
                    }

                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("flashcard_rating_area"),
                        contentAlignment = Alignment.Center
                    ) {
                        val cardWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
                        val swipeDistanceThresholdPx = with(LocalDensity.current) { 120.dp.toPx() }
                        val swipeVelocityThresholdPxPerSecond = with(LocalDensity.current) { 1000.dp.toPx() }
                        val swipeModifier = if (isActiveCardFlipped && !isAnimatingRating) {
                            Modifier.pointerInput(
                                card.cardKey,
                                isActiveCardFlipped,
                                isAnimatingRating,
                                cardWidthPx,
                                swipeDistanceThresholdPx,
                                swipeVelocityThresholdPxPerSecond
                            ) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val velocityTracker = VelocityTracker().apply {
                                        addPosition(down.uptimeMillis, down.position)
                                    }
                                    val pointerId = down.id
                                    val slopChange = awaitHorizontalTouchSlopOrCancellation(pointerId) { change, overSlop ->
                                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                                        dragOffsetX = (dragOffsetX + overSlop)
                                            .coerceIn(-cardWidthPx * 0.75f, cardWidthPx * 0.75f)
                                        change.consume()
                                    }

                                    val dragCompleted = if (slopChange != null) {
                                        horizontalDrag(pointerId) { change ->
                                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                                            dragOffsetX = (dragOffsetX + change.positionChange().x)
                                                .coerceIn(-cardWidthPx * 0.75f, cardWidthPx * 0.75f)
                                            change.consume()
                                        }
                                    } else {
                                        false
                                    }

                                    val dragDirection = if (abs(dragOffsetX) >= swipeDistanceThresholdPx) {
                                        dragOffsetX.sign
                                    } else {
                                        0f
                                    }
                                    val releaseVelocityX = velocityTracker.calculateVelocity().x
                                    val flingDirection = if (abs(releaseVelocityX) >= swipeVelocityThresholdPxPerSecond) {
                                        releaseVelocityX.sign
                                    } else {
                                        0f
                                    }
                                    val committedDirection = if (dragDirection != 0f) dragDirection else flingDirection

                                    if (dragCompleted && committedDirection != 0f) {
                                        handleRateCard(if (committedDirection < 0f) Sm2Rating.AGAIN else Sm2Rating.EASY)
                                    } else if (dragOffsetX != 0f) {
                                        coroutineScope.launch {
                                            val returnAnimation = Animatable(dragOffsetX)
                                            returnAnimation.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 650f)) {
                                                dragOffsetX = value
                                            }
                                            dragOffsetX = 0f
                                        }
                                    }
                                }
                            }
                        } else {
                            Modifier
                        }

                        val translationXAnimatable = translationX
                        val translationYAnimatable = translationY
                        val rotationZAnimatable = rotationZ
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    this.translationX = translationXAnimatable.value + dragOffsetX
                                    this.translationY = translationYAnimatable.value
                                    this.rotationZ = rotationZAnimatable.value
                                    alpha = cardAlpha.value
                                    scaleX = cardScale.value
                                    scaleY = cardScale.value
                                }
                                .then(swipeModifier),
                            contentAlignment = Alignment.Center
                        ) {
                            key(card.cardKey) {
                                FlipFlashcardItem(
                                    card = card,
                                    isFlipped = isActiveCardFlipped,
                                    effectiveDirection = effectiveDirection,
                                    autoShowAnswer = autoShowAnswer,
                                    isMastered = isMastered,
                                    isFavorite = isFavorite,
                                    isPlayingAudio = currentlyPlayingWord == card.text,
                                    savedSentenceKeys = savedSentenceKeys,
                                    ratingColor = ratingStamp?.ratingAccent(),
                                    onFlipCard = { if (!isAnimatingRating) onFlipCard() },
                                    onPlayAudio = {
                                        val audioUrl = card.posVariants.firstOrNull { it.audioUrl.isNotBlank() }?.audioUrl.orEmpty()
                                        onPlayAudio(card.text, audioUrl)
                                    },
                                    onPlayAudioSlow = {
                                        val audioUrl = card.posVariants.firstOrNull { it.audioUrl.isNotBlank() }?.audioUrl.orEmpty()
                                        onPlayAudioSlow(card.text, audioUrl)
                                    },
                                    onToggleFavorite = onToggleFavorite,
                                    onToggleSaveSentence = { en, ar -> onToggleSaveSentence(en, ar, card.text) },
                                    onOpenDetailedDefinition = { onOpenDetailedDefinition(card.deck, card.text) }
                                )
                            }
                            ratingStamp?.let { rating ->
                                RatingStampOverlay(
                                    rating = rating,
                                    scaleProvider = { stampScale.value },
                                    alphaProvider = { stampAlpha.value },
                                    modifier = Modifier.align(Alignment.TopCenter)
                                )
                            }
                        }
                    }
                }
                // First-session Coach Tip guidance (dismissible on tap or flip/rate)
                if (showCoachTipFlip && !isActiveCardFlipped) {
                    com.example.ui.components.CoachTipBanner(
                        icon = Icons.Filled.Flip,
                        messageEn = "Tap card anywhere to reveal Arabic meaning & examples",
                        messageAr = "اضغط على البطاقة لقلبها وإظهار المعنى والأمثلة",
                        testTag = "coach_tip_flip",
                        onDismiss = onDismissCoachTipFlip,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                } else if (showCoachTipRate && isActiveCardFlipped) {
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
                val cardForActions = displayedCard
                if (cardForActions != null) {
                    val isCurrentMastered = progressMap[cardForActions.cardKey]?.isMastered ?: cardForActions.isMastered
                    FlashcardBottomActionsBar(
                        isCardFlipped = isActiveCardFlipped,
                        isRatingLocked = isAnimatingRating,
                        isMastered = isCurrentMastered,
                        intervalPreview = intervalPreview,
                        onToggleMastered = onToggleMastered,
                        onOpenDetailedDefinition = {
                            onOpenDetailedDefinition(cardForActions.deck, cardForActions.text)
                        },
                        onRateCard = handleRateCard
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
                onPlayAudioSlow = {
                    val url = detailedCard.posVariants.firstOrNull { it.audioUrl.isNotBlank() }?.audioUrl ?: ""
                    onPlayAudioSlow(detailedCard.text, url)
                },
                onPlayVoiceAudioSlow = { isGb ->
                    val url = if (!isGb) detailedCard.posVariants.firstOrNull { it.audioUrl.isNotBlank() }?.audioUrl ?: "" else ""
                    if (isGb) {
                        onPlayVoiceAudio(detailedCard.text, url, true)
                    } else {
                        onPlayAudioSlow(detailedCard.text, url)
                    }
                },
                onPlayVoiceAudio = { isGb ->
                    val url = if (!isGb) detailedCard.posVariants.firstOrNull { it.audioUrl.isNotBlank() }?.audioUrl ?: "" else ""
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
    ratingColor: Color? = null,
    savedSentenceKeys: Set<String>,
    onFlipCard: () -> Unit,
    onPlayAudio: () -> Unit,
    onPlayAudioSlow: () -> Unit = {},
    onToggleFavorite: () -> Unit,
    onToggleSaveSentence: (String, String) -> Unit,
    onOpenDetailedDefinition: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "flashcard3dFlip"
    )
    val showingBack = rotation > 90f

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
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
                color = ratingColor ?: if (isMastered) EmeraldAccent.copy(alpha = 0.65f) else GoldAccentBorder
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (!showingBack) {
                    FlashcardFrontFace(
                        card = card,
                        effectiveDirection = effectiveDirection,
                        autoShowAnswer = autoShowAnswer,
                        isFavorite = isFavorite,
                        isPlayingAudio = isPlayingAudio,
                        onPlayAudio = onPlayAudio,
                        onPlayAudioSlow = onPlayAudioSlow,
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
                            onPlayAudioSlow = onPlayAudioSlow,
                            onToggleFavorite = onToggleFavorite,
                            onToggleSaveSentence = onToggleSaveSentence,
                            onOpenDetailedDefinition = onOpenDetailedDefinition
                        )
                    }
                }
            }
        }
    }
}

private fun Sm2Rating.ratingAccent(): Color = when (this) {
    Sm2Rating.AGAIN -> CoralAccent
    Sm2Rating.HARD -> Color(0xFFF59E0B)
    Sm2Rating.GOOD -> Color(0xFFFACC15)
    Sm2Rating.EASY -> EmeraldAccent
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
    onPlayAudioSlow: () -> Unit = {},
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
            }
        }

        // Center Content: Large Word (or Arabic meaning in Arabic-to-English mode) + Part of Speech + Audio Button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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

                    val primaryPos = remember(card) {
                        card.posVariants.firstOrNull { it.pos.isNotBlank() }?.pos?.ifBlank { card.primaryType }
                            ?: card.primaryType
                    }
                    if (primaryPos.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color(0xFF1B1E28),
                            border = BorderStroke(1.dp, Color(0xFF282C3A))
                        ) {
                            Text(
                                text = primaryPos,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF9CA3AF),
                                    fontSize = 12.sp
                                ),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // Audio Buttons — consume their own clicks so tapping audio NEVER flips the card
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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

                SlowAudioTurtleButton(
                    onClick = onPlayAudioSlow,
                    showSpeedLabel = true,
                    testTag = "flashcard_front_slow_audio_button"
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
    onPlayAudioSlow: () -> Unit = {},
    onToggleFavorite: () -> Unit,
    onToggleSaveSentence: (String, String) -> Unit,
    onOpenDetailedDefinition: () -> Unit
) {
    val variantsWithArabic = remember(card.posVariants) {
        card.posVariants.filter { it.arabicMeaning.isNotBlank() }
    }

    // Collect single example across variants to prevent overcrowding on flip
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
        list.take(1)
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
        // Back Top Row: CEFR Level badge (if non-empty) + Audio, Turtle & Heart icons (No duplicate info button)
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
                SlowAudioTurtleButton(
                    onClick = onPlayAudioSlow,
                    showSpeedLabel = false,
                    testTag = "flashcard_back_slow_audio_button"
                )
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
            }
        }

        // Word + Pronunciation chip combined into a single row to save vertical space
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = card.text,
                style = if (effectiveDirection == FlashcardDirection.ARABIC_TO_ENGLISH) {
                    MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold)
                } else {
                    MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold)
                },
                color = TextPrimary,
                modifier = Modifier.testTag("flashcard_word_text")
            )

            if (card.ipa.isNotBlank()) {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onPlayAudio)
                        .testTag("flashcard_ipa_speaker_chip"),
                    color = if (isPlayingAudio) GoldAccent else GoldAccentDim,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, GoldAccentBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Play pronunciation",
                            tint = if (isPlayingAudio) BackgroundDark else GoldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "US ${card.ipa}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isPlayingAudio) BackgroundDark else GoldAccent,
                            modifier = Modifier.testTag("flashcard_ipa_text")
                        )
                    }
                }
            }
        }

        // 1. Meaning Box: POS on left, Arabic meaning on right (RTL)
        if (variantsWithArabic.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("flashcard_arabic_section"),
                color = Color(0xFF13161F),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF232734))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val groupedByPos = variantsWithArabic.groupBy { it.pos.trim() }
                    groupedByPos.forEach { (pos, posVariants) ->
                        val combinedArabic = posVariants
                            .map { it.arabicMeaning.trim() }
                            .filter { it.isNotEmpty() }
                            .distinct()
                            .joinToString("، ")

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (pos.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = GoldAccentDim,
                                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.35f))
                                ) {
                                    Text(
                                        text = pos,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = GoldAccent,
                                            fontSize = 11.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.width(4.dp))
                            }

                            ArabicText(
                                text = combinedArabic,
                                style = ArabicTypography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp
                                ),
                                color = TextPrimary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                modifier = Modifier.weight(1f)
                            )
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

        // 2. Optional English Definition
        if (primaryDefinitionEn.isNotBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("flashcard_definition_section"),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Definition",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = GoldAccent
                )
                Text(
                    text = primaryDefinitionEn,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFFE5E7EB),
                        lineHeight = 20.sp
                    )
                )
            }
        }

        // 3. Examples (Single example limit to prevent overcrowding on flipped card)
        if (examplePairs.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("flashcard_examples_section"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Examples",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = GoldAccent
                )

                examplePairs.forEachIndexed { idx, (sentenceEn, sentenceAr) ->
                    val isSaved = sentenceEn.trim().lowercase() in savedSentenceKeys
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .combinedClickable(
                                onClick = {},
                                onLongClick = { onToggleSaveSentence(sentenceEn, sentenceAr) }
                            )
                            .testTag("flashcard_example_card_$idx"),
                        color = Color(0xFF13161F),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (isSaved) GoldAccentBorder else Color(0xFF232734))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .fillMaxHeight()
                                    .background(GoldAccent)
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = highlightWordInGold(sentenceEn, card.text),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        lineHeight = 20.sp
                                    ),
                                    color = TextPrimary
                                )
                                if (sentenceAr.isNotEmpty()) {
                                    ArabicText(
                                        text = sentenceAr,
                                        style = ArabicTypography.bodyMedium,
                                        color = TextSecondary,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "Long-press or bookmark to save",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 2.dp)
                )
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
    isRatingLocked: Boolean,
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
        val sm2Alpha by animateFloatAsState(
            targetValue = if (isCardFlipped && !isRatingLocked) 1f else 0f,
            animationSpec = tween(durationMillis = 280),
            label = "sm2ButtonsAlpha"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = sm2Alpha }
                .testTag("flashcard_sm2_row"),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Sm2RateButton(
                label = "Again",
                intervalText = intervalPreview.againLabel,
                accentColor = CoralAccent,
                testTag = "sm2_button_again",
                enabled = isCardFlipped && !isRatingLocked,
                onClick = { if (isCardFlipped && !isRatingLocked) onRateCard(Sm2Rating.AGAIN) },
                modifier = Modifier.weight(1f)
            )
            Sm2RateButton(
                label = "Hard",
                intervalText = intervalPreview.hardLabel,
                accentColor = Color(0xFFF59E0B),
                testTag = "sm2_button_hard",
                enabled = isCardFlipped && !isRatingLocked,
                onClick = { if (isCardFlipped && !isRatingLocked) onRateCard(Sm2Rating.HARD) },
                modifier = Modifier.weight(1f)
            )
            Sm2RateButton(
                label = "Good",
                intervalText = intervalPreview.goodLabel,
                accentColor = Color(0xFFFACC15),
                testTag = "sm2_button_good",
                enabled = isCardFlipped && !isRatingLocked,
                onClick = { if (isCardFlipped && !isRatingLocked) onRateCard(Sm2Rating.GOOD) },
                modifier = Modifier.weight(1f)
            )
            Sm2RateButton(
                label = "Easy",
                intervalText = intervalPreview.easyLabel,
                accentColor = EmeraldAccent,
                testTag = "sm2_button_easy",
                enabled = isCardFlipped && !isRatingLocked,
                onClick = { if (isCardFlipped && !isRatingLocked) onRateCard(Sm2Rating.EASY) },
                modifier = Modifier.weight(1f)
            )
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
private fun RatingStampOverlay(
    rating: Sm2Rating,
    scaleProvider: () -> Float,
    alphaProvider: () -> Float,
    modifier: Modifier = Modifier,
    intervalText: String = ""
) {
    val (label, color, icon) = when (rating) {
        Sm2Rating.AGAIN -> Triple("AGAIN", CoralAccent, Icons.Default.Refresh)
        Sm2Rating.HARD -> Triple("HARD", Color(0xFFF59E0B), Icons.Default.FitnessCenter)
        Sm2Rating.GOOD -> Triple("GOOD", Color(0xFFFACC15), Icons.Default.ThumbUp)
        Sm2Rating.EASY -> Triple("EASY", EmeraldAccent, Icons.Default.AutoAwesome)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 10.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            modifier = Modifier.graphicsLayer {
                scaleX = scaleProvider()
                scaleY = scaleProvider()
                alpha = alphaProvider()
            },
            color = SurfaceElevatedDark.copy(alpha = 0.95f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.5.dp, color),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = color
                )
                if (intervalText.isNotBlank()) {
                    Text(
                        text = "• +$intervalText",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = color.copy(alpha = 0.9f)
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
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (label.lowercase()) {
        "again" -> Icons.Default.Refresh
        "hard" -> Icons.Default.FitnessCenter
        "good" -> Icons.Default.ThumbUp
        else -> Icons.Default.AutoAwesome
    }

    var isPressed by remember { mutableStateOf(false) }
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "sm2RateBtnScale"
    )

    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = buttonScale
                scaleY = buttonScale
            }
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled) {
                isPressed = true
                onClick()
            }
            .testTag(testTag),
        color = SurfaceDark,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.70f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.14f),
                            SurfaceDark
                        )
                    )
                )
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(17.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = accentColor
            )
            Text(
                text = intervalText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                ),
                color = TextSecondary
            )
        }
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(120)
            isPressed = false
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
