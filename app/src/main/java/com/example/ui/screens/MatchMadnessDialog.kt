package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.blitz.BlitzSoundPlayer
import com.example.data.blitz.MatchCardItem
import com.example.data.blitz.MatchMadnessEngine
import com.example.data.blitz.MatchMadnessSessionState
import com.example.data.blitz.MatchSelectionOutcome
import com.example.data.local.EntryEntity
import com.example.ui.components.BidiText
import com.example.ui.components.FloatingFeedbackBadge
import com.example.ui.components.GameFeedbackColors
import com.example.ui.components.GameFeedbackController
import com.example.ui.components.GameFeedbackState
import com.example.ui.components.gameFeedbackMotion
import com.example.ui.components.performGameFeedback
import com.example.ui.components.rememberGameFeedbackController
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.ArabicTypography
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderDark
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldAccentBorder
import com.example.ui.theme.GoldAccentDim
import com.example.ui.theme.PillShape
import com.example.ui.theme.SkyAccent
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevatedDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.bi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MatchMadnessDialog(
    initialEntries: List<EntryEntity>,
    bestTimeSeconds: Int = 0,
    enableTimerAutoTick: Boolean = true,
    onFinish: (Int) -> Unit,
    onPlayPronunciation: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    var sessionState by remember {
        mutableStateOf(MatchMadnessEngine.createSession(initialEntries))
    }
    var soundEnabled by remember { mutableStateOf(true) }
    var roundTransitionMessage by remember { mutableStateOf<String?>(null) }
    var isHandlingMismatch by remember { mutableStateOf(false) }
    var recentlyMatchedPairId by remember { mutableStateOf<String?>(null) }

    val feedbackController = rememberGameFeedbackController()
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val isFullHeightScreen = screenHeightDp >= 640
    val bottomSafePadding = if (isFullHeightScreen) 44.dp else 20.dp

    // 1-second timer loop while game is active
    LaunchedEffect(sessionState.isTimerRunning, sessionState.isFinished, enableTimerAutoTick) {
        if (!enableTimerAutoTick) return@LaunchedEffect
        while (sessionState.isTimerRunning && !sessionState.isFinished) {
            delay(1000L)
            sessionState = MatchMadnessEngine.tickTimer(sessionState)
        }
    }

    Dialog(
        onDismissRequest = {
            if (sessionState.isFinished) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .testTag("match_madness_dialog"),
            color = BackgroundDark
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = bottomSafePadding),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = SkyAccent.copy(alpha = 0.2f),
                            border = BorderStroke(1.5.dp, SkyAccent),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Extension,
                                    contentDescription = null,
                                    tint = SkyAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = bi("Match Madness", "توصيل الكلمات السريع 🧩"),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = TextPrimary
                            )
                            Text(
                                text = bi("Connect English & Arabic Pairs", "وصّل كل كلمة بالإنجليزية بمعناها بالعربية"),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = { soundEnabled = !soundEnabled },
                            modifier = Modifier.testTag("match_sound_toggle_btn")
                        ) {
                            Icon(
                                imageVector = if (soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                                contentDescription = if (soundEnabled) "Mute SFX" else "Unmute SFX",
                                tint = if (soundEnabled) GoldAccent else TextMuted
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("match_close_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Exit Match Madness",
                                tint = TextSecondary
                            )
                        }
                    }
                }

                if (!sessionState.isFinished) {
                    // HUD Row: Round Progress + Timer + Accuracy
                    val currentRound = sessionState.currentRound
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = SurfaceDark,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, BorderDark)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Round indicator
                            Surface(
                                color = SkyAccent.copy(alpha = 0.16f),
                                shape = PillShape,
                                border = BorderStroke(1.dp, SkyAccent)
                            ) {
                                Text(
                                    text = bi("Round ${sessionState.currentRoundIndex + 1}/3", "الجولة ${sessionState.currentRoundIndex + 1} من ٣"),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = SkyAccent,
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                        .testTag("match_round_indicator")
                                )
                            }

                            // Live Timer
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.testTag("match_timer_text")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                val mins = sessionState.elapsedSeconds / 60
                                val secs = sessionState.elapsedSeconds % 60
                                Text(
                                    text = "%02d:%02d".format(mins, secs),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                    color = GoldAccent
                                )
                            }

                            // Accuracy Pill
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = bi("Acc: ", "الدقة: "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "${sessionState.accuracyPercent}%",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = EmeraldAccent
                                )
                            }
                        }
                    }

                    // Optional round transition banner
                    if (roundTransitionMessage != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = EmeraldAccent.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, EmeraldAccent)
                        ) {
                            Text(
                                text = roundTransitionMessage.orEmpty(),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = EmeraldAccent,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }

                    // 2-Column Matching Grid (6 English Left, 6 Arabic Right) + Bilingual Footer Hint
                    if (currentRound != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (isFullHeightScreen) Modifier.weight(1f) else Modifier)
                                .testTag("match_grid_container"),
                            color = SurfaceDark,
                            shape = RoundedCornerShape(22.dp),
                            border = BorderStroke(1.dp, BorderDark)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(if (isFullHeightScreen) Modifier.fillMaxSize() else Modifier)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(if (isFullHeightScreen) Modifier.fillMaxSize() else Modifier)
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.SpaceBetween,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .then(if (isFullHeightScreen) Modifier.weight(1f) else Modifier),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Left Column: English Words
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .then(if (isFullHeightScreen) Modifier.fillMaxHeight() else Modifier),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            currentRound.leftCards.forEach { card ->
                                                MatchingCardView(
                                                    card = card,
                                                    isJustMatchedFlash = recentlyMatchedPairId == card.pairId,
                                                    feedbackTriggerCount = feedbackController.triggerCounter,
                                                    isEnabled = !isHandlingMismatch && !card.isMatched,
                                                    modifier = if (isFullHeightScreen) {
                                                        Modifier.weight(1f).fillMaxWidth()
                                                    } else {
                                                        Modifier.height(68.dp).fillMaxWidth()
                                                    },
                                                    onClick = {
                                                        handleCardClick(
                                                            cardId = card.id,
                                                            sessionState = sessionState,
                                                            bestTimeSeconds = bestTimeSeconds,
                                                            soundEnabled = soundEnabled,
                                                            haptic = haptic,
                                                            feedbackController = feedbackController,
                                                            onSetRecentlyMatchedPair = { recentlyMatchedPairId = it },
                                                            onUpdateState = { sessionState = it },
                                                            onFinish = onFinish,
                                                            onSetTransitionMessage = { roundTransitionMessage = it },
                                                            onSetHandlingMismatch = { isHandlingMismatch = it },
                                                            coroutineScope = coroutineScope
                                                        )
                                                    }
                                                )
                                            }
                                        }

                                        // Right Column: Arabic Meanings
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .then(if (isFullHeightScreen) Modifier.fillMaxHeight() else Modifier),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            currentRound.rightCards.forEach { card ->
                                                MatchingCardView(
                                                    card = card,
                                                    isJustMatchedFlash = recentlyMatchedPairId == card.pairId,
                                                    feedbackTriggerCount = feedbackController.triggerCounter,
                                                    isEnabled = !isHandlingMismatch && !card.isMatched,
                                                    modifier = if (isFullHeightScreen) {
                                                        Modifier.weight(1f).fillMaxWidth()
                                                    } else {
                                                        Modifier.height(68.dp).fillMaxWidth()
                                                    },
                                                    onClick = {
                                                        handleCardClick(
                                                            cardId = card.id,
                                                            sessionState = sessionState,
                                                            bestTimeSeconds = bestTimeSeconds,
                                                            soundEnabled = soundEnabled,
                                                            haptic = haptic,
                                                            feedbackController = feedbackController,
                                                            onSetRecentlyMatchedPair = { recentlyMatchedPairId = it },
                                                            onUpdateState = { sessionState = it },
                                                            onFinish = onFinish,
                                                            onSetTransitionMessage = { roundTransitionMessage = it },
                                                            onSetHandlingMismatch = { isHandlingMismatch = it },
                                                            coroutineScope = coroutineScope
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // Subtle bilingual footer hint
                                    BidiText(
                                        text = bi(
                                            "Tap matching pairs to clear the board • صل كل كلمة بمعناها الصحيح",
                                            "صل كل كلمة بمعناها الصحيح • Tap matching pairs to clear the board"
                                        ),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                                        color = TextMuted,
                                        fillMaxWidth = true,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .padding(top = 12.dp)
                                            .testTag("match_board_footer_hint")
                                    )
                                }

                                // Floating Feedback Toast (✓ تطابق صحيح! +10 / ✗ غير متطابق! حاول مجدداً)
                                FloatingFeedbackBadge(
                                    visible = feedbackController.toastVisible,
                                    feedbackState = feedbackController.state,
                                    text = feedbackController.toastMessage,
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = 24.dp)
                                )
                            }
                        }
                    }
                } else {
                    // FINAL RESULTS SCREEN
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("match_summary_card"),
                        color = SurfaceDark,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 18.dp, vertical = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Extension,
                                contentDescription = null,
                                tint = EmeraldAccent,
                                modifier = Modifier.size(52.dp)
                            )
                            Text(
                                text = bi("All 18 Words Matched! 🎉", "تم توصيل جميع الكلمات بنجاح! 🧩⚡"),
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                                color = EmeraldAccent,
                                textAlign = TextAlign.Center
                            )

                            if (sessionState.isNewBestTime) {
                                Surface(
                                    color = GoldAccentDim,
                                    shape = PillShape,
                                    border = BorderStroke(1.dp, GoldAccent)
                                ) {
                                    Text(
                                        text = bi("🏆 NEW PERSONAL BEST TIME!", "🏆 رقم قياسي جديد لأسرع زمن!"),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = GoldAccent,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            // Summary Metrics Row
                            val finalMins = sessionState.elapsedSeconds / 60
                            val finalSecs = sessionState.elapsedSeconds % 60
                            val timeStr = "%02d:%02d".format(finalMins, finalSecs)

                            val bestMins = bestTimeSeconds / 60
                            val bestSecs = bestTimeSeconds % 60
                            val bestStr = if (bestTimeSeconds > 0) "%02d:%02d".format(bestMins, bestSecs) else "--:--"

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MetricCardSmall(
                                    modifier = Modifier.weight(1f),
                                    titleEn = "Time",
                                    titleAr = "الزمن الكلي",
                                    value = timeStr,
                                    valueColor = GoldAccent
                                )
                                MetricCardSmall(
                                    modifier = Modifier.weight(1f),
                                    titleEn = "Best Time",
                                    titleAr = "أفضل رقم",
                                    value = bestStr,
                                    valueColor = SkyAccent
                                )
                                MetricCardSmall(
                                    modifier = Modifier.weight(1f),
                                    titleEn = "Accuracy",
                                    titleAr = "الدقة",
                                    value = "${sessionState.accuracyPercent}%",
                                    valueColor = EmeraldAccent
                                )
                            }

                            // Full Word Review
                            HorizontalDivider(color = BorderDark)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = bi("Vocabulary Review (18 Words):", "مراجعة الكلمات الـ ١٨:"),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = TextSecondary
                                )
                                sessionState.allPairs.forEach { pair ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = SurfaceElevatedDark,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, BorderDark)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = pair.englishWord,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = GoldAccent
                                                )
                                                if (pair.level.isNotBlank()) {
                                                    Text(
                                                        text = pair.level,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = TextMuted
                                                    )
                                                }
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = pair.arabicMeaning,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextPrimary
                                                )
                                                IconButton(
                                                    onClick = { onPlayPronunciation(pair.englishWord) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                        contentDescription = "Pronounce",
                                                        tint = TextSecondary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onDismiss,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = bi("Close", "إغلاق"), color = TextSecondary)
                                }

                                Button(
                                    onClick = {
                                        recentlyMatchedPairId = null
                                        feedbackController.hideToast()
                                        feedbackController.resetCardState()
                                        sessionState = MatchMadnessEngine.createSession(initialEntries.shuffled())
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldAccent,
                                        contentColor = BackgroundDark
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1.5f)
                                        .testTag("match_play_again_btn")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = bi("Play Again", "لعب مرة أخرى 🧩"), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchingCardView(
    card: MatchCardItem,
    isJustMatchedFlash: Boolean = false,
    feedbackTriggerCount: Int = 0,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(68.dp),
    onClick: () -> Unit
) {
    // Spec:
    // - Correct Pair: Flash both tiles Emerald Green (#34D399), pop scale to 1.06f,
    //   then smoothly animate to matched/cleared state (alpha = 0.28f, scale = 0.94f, with small ✓ checkmark).
    // - Wrong Pair: Flash both tiles Rose Red (#FB7185) and shake horizontally for 360ms before deselecting.
    val tileFeedbackState = when {
        card.isError -> GameFeedbackState.WRONG
        isJustMatchedFlash -> GameFeedbackState.CORRECT
        else -> GameFeedbackState.IDLE
    }

    val settledClearState = card.isMatched && !isJustMatchedFlash

    val targetAlpha = if (settledClearState) 0.28f else 1.0f
    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "matchTileAlpha"
    )

    val targetBaseScale = when {
        settledClearState -> 0.94f
        card.isSelected && !card.isError -> 1.03f
        else -> 1.0f
    }
    val animatedBaseScale by animateFloatAsState(
        targetValue = targetBaseScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "matchTileScale"
    )

    val cardColor by animateColorAsState(
        targetValue = when {
            card.isError -> GameFeedbackColors.RoseTint
            isJustMatchedFlash -> GameFeedbackColors.EmeraldTint
            settledClearState -> Color(0xFF181B24)
            card.isSelected -> SkyAccent.copy(alpha = 0.16f)
            else -> Color(0xFF181B24)
        },
        animationSpec = tween(durationMillis = 200),
        label = "matchCardColor"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            card.isError -> GameFeedbackColors.RoseBorder
            isJustMatchedFlash -> GameFeedbackColors.EmeraldBorder
            settledClearState -> GameFeedbackColors.EmeraldBorder.copy(alpha = 0.65f)
            card.isSelected -> SkyAccent
            else -> BorderDark
        },
        animationSpec = tween(durationMillis = 200),
        label = "matchBorderColor"
    )

    val displayText = if (card.isMatched) "✓ ${card.text}" else card.text

    val fontSize = when {
        displayText.length > 18 -> 13.sp
        !card.isEnglish -> 15.5.sp
        else -> 14.5.sp
    }
    val textStyle = if (!card.isEnglish) {
        ArabicTypography.bodyMedium.copy(
            fontSize = fontSize,
            lineHeight = 18.sp,
            fontWeight = if (card.isSelected || card.isMatched) FontWeight.Bold else FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            textDirection = TextDirection.Rtl
        )
    } else {
        MaterialTheme.typography.bodyMedium.copy(
            fontSize = fontSize,
            lineHeight = 18.sp,
            fontWeight = if (card.isSelected || card.isMatched) FontWeight.Bold else FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            textDirection = TextDirection.Ltr
        )
    }

    val borderWidth = when {
        card.isError || isJustMatchedFlash -> 2.dp
        card.isSelected -> 1.8.dp
        else -> 1.5.dp
    }

    Surface(
        modifier = modifier
            .gameFeedbackMotion(
                feedbackState = tileFeedbackState,
                triggerKey = feedbackTriggerCount,
                correctPeakScale = 1.06f
            )
            .scale(animatedBaseScale)
            .alpha(animatedAlpha)
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = isEnabled, onClick = onClick)
            .testTag(if (card.isEnglish) "match_card_en_${card.text}" else "match_card_ar_${card.id}"),
        color = cardColor,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayText,
                style = textStyle,
                color = when {
                    card.isError -> GameFeedbackColors.RoseToastText
                    isJustMatchedFlash || card.isMatched -> GameFeedbackColors.EmeraldBorder
                    card.isSelected -> SkyAccent
                    else -> TextPrimary
                },
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun handleCardClick(
    cardId: String,
    sessionState: MatchMadnessSessionState,
    bestTimeSeconds: Int,
    soundEnabled: Boolean,
    haptic: HapticFeedback,
    feedbackController: GameFeedbackController,
    onSetRecentlyMatchedPair: (String?) -> Unit,
    onUpdateState: (MatchMadnessSessionState) -> Unit,
    onFinish: (Int) -> Unit,
    onSetTransitionMessage: (String?) -> Unit,
    onSetHandlingMismatch: (Boolean) -> Unit,
    coroutineScope: kotlinx.coroutines.CoroutineScope
) {
    when (val outcome = MatchMadnessEngine.selectCard(sessionState, cardId, bestTimeSeconds)) {
        is MatchSelectionOutcome.CardSelected -> {
            haptic.performGameFeedback(true)
            onUpdateState(outcome.state)
        }
        is MatchSelectionOutcome.MatchSuccess -> {
            haptic.performGameFeedback(true)
            BlitzSoundPlayer.playCorrectChime(soundEnabled)
            onSetRecentlyMatchedPair(outcome.pairId)
            feedbackController.triggerCorrect("✓ Match! +10 pts • تطابق صحيح!")
            onUpdateState(outcome.state)

            coroutineScope.launch {
                delay(320L)
                onSetRecentlyMatchedPair(null)
                feedbackController.resetCardState()
                delay(240L)
                feedbackController.hideToast()
            }

            if (outcome.gameJustCompleted) {
                BlitzSoundPlayer.playComboLevelUpChime(soundEnabled)
                onFinish(outcome.state.elapsedSeconds)
            } else if (outcome.roundJustCompleted) {
                BlitzSoundPlayer.playComboLevelUpChime(soundEnabled)
                coroutineScope.launch {
                    onSetTransitionMessage("Round ${outcome.state.currentRoundIndex + 1} Complete! 🌟")
                    delay(750L)
                    onSetTransitionMessage(null)
                    onSetRecentlyMatchedPair(null)
                    onUpdateState(MatchMadnessEngine.advanceRound(outcome.state))
                }
            }
        }
        is MatchSelectionOutcome.MatchMismatch -> {
            haptic.performGameFeedback(false)
            BlitzSoundPlayer.playWrongBuzz(soundEnabled)
            feedbackController.triggerWrong("✗ Mismatch! • غير متطابق!")
            onUpdateState(outcome.state)
            onSetHandlingMismatch(true)

            coroutineScope.launch {
                // Fast, non-blocking 360ms horizontal shake before automatically deselecting
                delay(360L)
                onUpdateState(MatchMadnessEngine.clearSelectionsAndErrors(outcome.state))
                onSetHandlingMismatch(false)
                feedbackController.resetCardState()
                delay(200L)
                feedbackController.hideToast()
            }
        }
    }
}

@Composable
private fun MetricCardSmall(
    modifier: Modifier = Modifier,
    titleEn: String,
    titleAr: String,
    value: String,
    valueColor: Color
) {
    Surface(
        modifier = modifier,
        color = SurfaceElevatedDark,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderDark)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "$titleEn • $titleAr",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                color = valueColor
            )
        }
    }
}
