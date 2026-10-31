package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.blitz.BlitzQuestionItem
import com.example.data.blitz.BlitzQuestionMode
import com.example.data.blitz.BlitzSoundPlayer
import com.example.data.blitz.VocabularyBlitzEngine
import com.example.data.blitz.VocabularyBlitzSessionState
import com.example.data.blitz.VocabularyBlitzStats
import com.example.ui.components.BidiText
import com.example.ui.components.GoldBadge
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
import com.example.ui.theme.VioletAccent
import com.example.ui.theme.bi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VocabularyBlitzDialog(
    initialQuestions: List<BlitzQuestionItem>,
    savedStats: VocabularyBlitzStats = VocabularyBlitzStats(),
    enableTimerAutoTick: Boolean = true,
    onPlayPronunciation: (String) -> Unit = {},
    onFinishRound: (VocabularyBlitzSessionState) -> Unit = {},
    onPracticeMistakes: (List<BlitzQuestionItem>) -> Unit = {},
    onChangeFiltersRequest: () -> Unit = {},
    onPlayAgainRequest: () -> Unit = {},
    onDismiss: () -> Unit
) {
    var sessionState by remember(initialQuestions) {
        mutableStateOf(
            VocabularyBlitzSessionState(
                questions = initialQuestions,
                remainingSeconds = VocabularyBlitzEngine.BLITZ_DURATION_SECONDS,
                isRunning = true,
                isFinished = false
            )
        )
    }
    var soundEffectsEnabled by remember { mutableStateOf(true) }

    // 60-second countdown loop
    LaunchedEffect(sessionState.isRunning, sessionState.isFinished, initialQuestions, enableTimerAutoTick) {
        if (!enableTimerAutoTick) return@LaunchedEffect
        while (sessionState.isRunning && !sessionState.isFinished && sessionState.remainingSeconds > 0) {
            delay(1000L)
            val nextSec = (sessionState.remainingSeconds - 1).coerceAtLeast(0)
            if (nextSec in 1..10) {
                BlitzSoundPlayer.playCountdownTick(soundEffectsEnabled)
            }
            if (nextSec <= 0) {
                val finishedState = sessionState.copy(
                    remainingSeconds = 0,
                    isRunning = false,
                    isFinished = true
                )
                sessionState = finishedState
                onFinishRound(finishedState)
            } else {
                sessionState = sessionState.copy(remainingSeconds = nextSec)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .testTag("vocab_blitz_dialog"),
            color = BackgroundDark
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Bar: Title + Sound Toggle + Close
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
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = bi("60-Second Vocabulary Blitz", "تحدي الـ 60 ثانية السريع ⚡"),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = TextPrimary
                            )
                            Text(
                                text = bi(
                                    "Best Score: ${maxOf(savedStats.highScore, sessionState.score)} pts",
                                    "أعلى نتيجة: ${maxOf(savedStats.highScore, sessionState.score)} نقطة"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldAccent
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { soundEffectsEnabled = !soundEffectsEnabled },
                            modifier = Modifier.testTag("blitz_sound_toggle")
                        ) {
                            Icon(
                                imageVector = if (soundEffectsEnabled) {
                                    Icons.AutoMirrored.Filled.VolumeUp
                                } else {
                                    Icons.AutoMirrored.Filled.VolumeOff
                                },
                                contentDescription = "Toggle sound effects",
                                tint = if (soundEffectsEnabled) GoldAccent else TextMuted
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("blitz_close_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Blitz",
                                tint = TextSecondary
                            )
                        }
                    }
                }

                if (sessionState.isFinished) {
                    BlitzSummaryContent(
                        state = sessionState,
                        savedStats = savedStats,
                        onSpeakWord = onPlayPronunciation,
                        onPracticeMistakes = onPracticeMistakes,
                        onChangeFilters = onChangeFiltersRequest,
                        onPlayAgain = {
                            onPlayAgainRequest()
                            sessionState = VocabularyBlitzSessionState(
                                questions = initialQuestions.shuffled(),
                                remainingSeconds = VocabularyBlitzEngine.BLITZ_DURATION_SECONDS,
                                isRunning = true,
                                isFinished = false
                            )
                        },
                        onDone = onDismiss
                    )
                } else {
                    BlitzActiveGameContent(
                        state = sessionState,
                        enableFlashDelay = enableTimerAutoTick,
                        onSpeakWord = onPlayPronunciation,
                        onSelectOption = { selectedIdx ->
                            val prevMultiplier = sessionState.comboMultiplier
                            val updated = VocabularyBlitzEngine.submitAnswer(
                                state = sessionState,
                                selectedOptionIndex = selectedIdx,
                                previousHighScore = savedStats.highScore
                            )
                            if (updated.lastAnswerCorrect == true) {
                                if (updated.comboMultiplier > prevMultiplier) {
                                    BlitzSoundPlayer.playComboLevelUpChime(soundEffectsEnabled)
                                } else {
                                    BlitzSoundPlayer.playCorrectChime(soundEffectsEnabled)
                                }
                            } else {
                                BlitzSoundPlayer.playWrongBuzz(soundEffectsEnabled)
                            }
                            sessionState = updated
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.BlitzActiveGameContent(
    state: VocabularyBlitzSessionState,
    enableFlashDelay: Boolean = true,
    onSpeakWord: (String) -> Unit,
    onSelectOption: (Int) -> Unit
) {
    val question = state.currentQuestion ?: return
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    var tappedOptionIndex by remember(question.id, state.currentIndex) { mutableStateOf<Int?>(null) }

    val handleOptionTap: (Int) -> Unit = { idx ->
        if (tappedOptionIndex == null) {
            tappedOptionIndex = idx
            runCatching { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
            if (enableFlashDelay) {
                coroutineScope.launch {
                    delay(150L)
                    onSelectOption(idx)
                }
            } else {
                onSelectOption(idx)
            }
        }
    }

    val isLowTime = state.remainingSeconds <= 10
    val timerColor by animateColorAsState(
        targetValue = when {
            state.remainingSeconds <= 10 -> CoralAccent
            state.remainingSeconds <= 20 -> AmberAccent
            else -> GoldAccent
        },
        label = "blitzTimerColor"
    )
    val timerProgress by animateFloatAsState(
        targetValue = (state.remainingSeconds.toFloat() / VocabularyBlitzEngine.BLITZ_DURATION_SECONDS.toFloat()).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "blitzTimerProgress"
    )

    val pulseScale = if (isLowTime) {
        val infiniteTransition = rememberInfiniteTransition(label = "blitzPulse")
        val animated by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(420, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "blitzPulseScale"
        )
        animated
    } else {
        1f
    }

    // TOP ZONE: Fixed HUD Row (Circular Fiery Timer + Live Score + Combo Multiplier)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceDark,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, if (isLowTime) CoralAccent else GoldAccentBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Animated 60-Second Circular Timer
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(64.dp)
                    .scale(pulseScale)
                    .testTag("blitz_timer_ring")
            ) {
                CircularProgressIndicator(
                    progress = { timerProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = timerColor,
                    trackColor = SurfaceElevatedDark,
                    strokeWidth = 6.dp
                )
                Text(
                    text = "${state.remainingSeconds}s",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = timerColor,
                    modifier = Modifier.testTag("blitz_timer_text")
                )
            }

            // Live Score Counter
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.testTag("blitz_score_badge")
            ) {
                Text(
                    text = bi("SCORE", "النقاط"),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextSecondary
                )
                Text(
                    text = "${state.score}",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = GoldAccent
                )
            }

            // Combo Multiplier Pill
            Surface(
                color = if (state.comboMultiplier > 1) CoralAccent.copy(alpha = 0.18f) else SurfaceElevatedDark,
                shape = PillShape,
                border = BorderStroke(
                    1.dp,
                    if (state.comboMultiplier > 1) CoralAccent else BorderDark
                ),
                modifier = Modifier.testTag("blitz_combo_badge")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = if (state.comboMultiplier > 1) CoralAccent else GoldAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${state.comboMultiplier}x COMBO",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = if (state.comboMultiplier > 1) CoralAccent else TextPrimary
                        )
                        Text(
                            text = "Streak: ${state.currentStreak}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }

    // Active Question Card (Balanced 3-Zone Layout: Top Meta, Vertically Centered Prompt, Bottom Thumb Action Zone)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .testTag("blitz_question_card"),
        color = SurfaceDark,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, GoldAccentBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Zone 1: Card Top Meta Row (CEFR Level + Academic Badge + Audio Button)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GoldBadge(text = "CEFR ${question.level}")
                    if (question.isAcademicOrIelts) {
                        Surface(
                            color = SkyAccent.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, SkyAccent.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "⚡ IELTS / Academic",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = SkyAccent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { onSpeakWord(question.word) },
                    modifier = Modifier.testTag("blitz_speak_word_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Listen to word",
                        tint = GoldAccent
                    )
                }
            }

            // Zone 2: Vertically Centered Word Prompt + Phonetics + Meaning Prompt Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = question.word,
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 36.sp,
                                textDirection = TextDirection.Ltr
                            ),
                            color = TextPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("blitz_target_word")
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (question.pos.isNotBlank()) {
                                Surface(
                                    color = GoldAccentDim,
                                    shape = PillShape,
                                    border = BorderStroke(1.dp, GoldAccentBorder)
                                ) {
                                    Text(
                                        text = question.pos,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = GoldAccent,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            val cleanIpa = question.ipa.trim().trim('/')
                            if (cleanIpa.isNotBlank()) {
                                Surface(
                                    color = SurfaceElevatedDark,
                                    shape = PillShape,
                                    border = BorderStroke(1.dp, BorderDark),
                                    modifier = Modifier
                                        .clip(PillShape)
                                        .clickable { onSpeakWord(question.word) }
                                ) {
                                    Text(
                                        text = "/$cleanIpa/",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (question.mode == BlitzQuestionMode.TRUE_FALSE) {
                    Surface(
                        color = SurfaceElevatedDark,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.2.dp, GoldAccentBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = bi("Does this word mean:", "هل معنى هذه الكلمة هو:"),
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                            BidiText(
                                text = question.promptArabic,
                                style = ArabicTypography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 28.sp
                                ),
                                color = GoldAccent,
                                fillMaxWidth = true,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    Text(
                        text = bi("Select the exact Arabic translation:", "اختر الترجمة العربية الصحيحة بأسرع وقت:"),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Zone 3: Bottom Thumb-Reach Action Zone
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (question.mode == BlitzQuestionMode.TRUE_FALSE) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        question.optionsArabic.forEachIndexed { idx, label ->
                            val isTapped = tappedOptionIndex == idx
                            val isCorrectOption = idx == question.correctOptionIndex
                            val baseAccent = if (idx == 0) EmeraldAccent else CoralAccent

                            val bgColor = when {
                                isTapped && isCorrectOption -> EmeraldAccent.copy(alpha = 0.36f)
                                isTapped && !isCorrectOption -> CoralAccent.copy(alpha = 0.36f)
                                else -> baseAccent.copy(alpha = 0.16f)
                            }
                            val borderColor = when {
                                isTapped && isCorrectOption -> EmeraldAccent
                                isTapped && !isCorrectOption -> CoralAccent
                                else -> baseAccent
                            }

                            val topLabel = if (idx == 0 || label.contains("صحيح")) "✓ Correct" else "✗ False"
                            val bottomLabel = if (idx == 0 || label.contains("صحيح")) "صحيح" else "خاطئ"

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(62.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { handleOptionTap(idx) }
                                    .testTag("blitz_option_$idx"),
                                color = bgColor,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(if (isTapped) 2.2.dp else 1.5.dp, borderColor)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    val textColor = if (isTapped) TextPrimary else baseAccent
                                    Text(
                                        text = topLabel,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        ),
                                        color = textColor,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = bottomLabel,
                                        style = ArabicTypography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        ),
                                        color = textColor,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                } else {
                    question.optionsArabic.forEachIndexed { idx, optionAr ->
                        val isTapped = tappedOptionIndex == idx
                        val isCorrectOption = idx == question.correctOptionIndex
                        val showCorrectHighlight = tappedOptionIndex != null && isCorrectOption

                        val optionBgColor = when {
                            isTapped && isCorrectOption -> EmeraldAccent.copy(alpha = 0.28f)
                            isTapped && !isCorrectOption -> CoralAccent.copy(alpha = 0.28f)
                            showCorrectHighlight -> EmeraldAccent.copy(alpha = 0.18f)
                            else -> SurfaceElevatedDark
                        }
                        val optionBorderColor = when {
                            isTapped && isCorrectOption -> EmeraldAccent
                            isTapped && !isCorrectOption -> CoralAccent
                            showCorrectHighlight -> EmeraldAccent
                            else -> GoldAccentBorder
                        }
                        val optionTextColor = when {
                            isTapped && isCorrectOption -> EmeraldAccent
                            isTapped && !isCorrectOption -> CoralAccent
                            showCorrectHighlight -> EmeraldAccent
                            else -> TextPrimary
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 58.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { handleOptionTap(idx) }
                                .testTag("blitz_option_$idx"),
                            color = optionBgColor,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(if (isTapped || showCorrectHighlight) 1.8.dp else 1.dp, optionBorderColor)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 15.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                BidiText(
                                    text = optionAr,
                                    style = ArabicTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = optionTextColor,
                                    fillMaxWidth = true,
                                    textAlign = TextAlign.Center
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
internal fun BlitzSummaryContent(
    state: VocabularyBlitzSessionState,
    savedStats: VocabularyBlitzStats,
    onSpeakWord: (String) -> Unit = {},
    onPracticeMistakes: (List<BlitzQuestionItem>) -> Unit = {},
    onChangeFilters: () -> Unit = {},
    onPlayAgain: () -> Unit,
    onDone: () -> Unit
) {
    val scrollState = rememberScrollState()
    val wordSniperEarned = state.unlockedWordSniperNow || savedStats.wordSniperUnlocked
    val academicLightningEarned = state.unlockedAcademicLightningNow || savedStats.academicLightningUnlocked
    var hasPracticedMistakes by remember { mutableStateOf(false) }

    val distinctMissedWords = remember(state.missedQuestions) {
        state.missedQuestions.distinctBy { it.word.lowercase().trim() }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .testTag("blitz_summary_card"),
        color = SurfaceDark,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.5.dp, GoldAccentBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = GoldAccent,
                modifier = Modifier.size(44.dp)
            )

            Text(
                text = bi("60 Seconds Up! • انتهى الوقت!", "انتهى تحدي الـ 60 ثانية! ⚡"),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = TextPrimary
            )

            if (state.isNewHighScore) {
                Surface(
                    color = GoldAccentDim,
                    shape = PillShape,
                    border = BorderStroke(1.dp, GoldAccent),
                    modifier = Modifier.testTag("blitz_new_high_score_banner")
                ) {
                    Text(
                        text = bi("🎉 NEW PERSONAL HIGH SCORE!", "🎉 رقم قياسي شخصي جديد!"),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = GoldAccent,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            } else {
                Surface(
                    color = SurfaceElevatedDark,
                    shape = PillShape,
                    border = BorderStroke(1.dp, BorderDark),
                    modifier = Modifier.testTag("blitz_personal_best_info")
                ) {
                    Text(
                        text = bi(
                            "Personal Best: ${maxOf(savedStats.highScore, state.score)} pts",
                            "أعلى نتيجة شخصية: ${maxOf(savedStats.highScore, state.score)} نقطة 🏆"
                        ),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = GoldAccent,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }

            Text(
                text = "${state.score} pts",
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = GoldAccent,
                modifier = Modifier.testTag("blitz_final_score")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BlitzStatPill(
                    label = bi("Correct", "صحيحة"),
                    value = "${state.correctCount}",
                    color = EmeraldAccent,
                    modifier = Modifier.weight(1f)
                )
                BlitzStatPill(
                    label = bi("Wrong", "خاطئة"),
                    value = "${state.wrongCount}",
                    color = CoralAccent,
                    modifier = Modifier.weight(1f)
                )
                BlitzStatPill(
                    label = bi("Accuracy", "الدقة"),
                    value = "${state.accuracyPercent}%",
                    color = SkyAccent,
                    modifier = Modifier.weight(1f)
                )
                BlitzStatPill(
                    label = bi("Max Combo", "السلسلة"),
                    value = "${state.maxComboStreak} 🔥",
                    color = AmberAccent,
                    modifier = Modifier.weight(1f)
                )
            }

            // Missed Words Review Section
            if (distinctMissedWords.isNotEmpty()) {
                HorizontalDivider(color = BorderDark)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("blitz_missed_words_section"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = CoralAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = bi(
                                    "Missed Words Review (${distinctMissedWords.size})",
                                    "مراجعة الكلمات الخاطئة (${distinctMissedWords.size})"
                                ),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                        }

                        Surface(
                            color = CoralAccent.copy(alpha = 0.15f),
                            shape = PillShape
                        ) {
                            Text(
                                text = "${distinctMissedWords.size} words",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = CoralAccent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        distinctMissedWords.forEachIndexed { idx, item ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("blitz_missed_word_$idx"),
                                color = SurfaceElevatedDark,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, BorderDark)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = item.word,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = TextPrimary
                                            )
                                            if (item.pos.isNotBlank()) {
                                                Surface(
                                                    color = GoldAccentDim,
                                                    shape = PillShape
                                                ) {
                                                    Text(
                                                        text = item.pos,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                        color = GoldAccent,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            GoldBadge(text = item.level)
                                        }

                                        BidiText(
                                            text = item.trueArabicMeaning,
                                            style = ArabicTypography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                            color = EmeraldAccent
                                        )
                                    }

                                    IconButton(
                                        onClick = { onSpeakWord(item.word) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("blitz_speak_missed_$idx")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "Listen to pronunciation",
                                            tint = GoldAccent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (!hasPracticedMistakes) {
                        Button(
                            onClick = {
                                hasPracticedMistakes = true
                                onPracticeMistakes(distinctMissedWords)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CoralAccent,
                                contentColor = BackgroundDark
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("blitz_practice_mistakes_btn")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = bi("Practice Mistakes (Update in SM-2)", "مراجعة الأخطاء وتحديث SM-2 🔄"),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("blitz_mistakes_practiced_banner"),
                            color = EmeraldAccent.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = EmeraldAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = bi(
                                        "✓ Queued in Spaced Repetition (SM-2) for tomorrow's review!",
                                        "✓ تم تحديث خوارزمية SM-2 وجدولة الكلمات في مراجعة الغد!"
                                    ),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = EmeraldAccent
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = BorderDark)

            // Achievement Badges Section (Word Sniper & Academic Lightning)
            Text(
                text = bi("Blitz Achievement Badges • شارات الإنجاز", "شارات تحدي السرعة"),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = GoldAccent
            )

            BlitzAchievementBadgeRow(
                emoji = "🥇",
                titleEn = "Word Sniper",
                titleAr = "قناص المفردات",
                description = bi(
                    "Answer 25+ words correctly in 60 seconds (${state.correctCount}/25)",
                    "الإجابة على 25 كلمة صحيحة في دقيقة واحدة (${state.correctCount}/25)"
                ),
                isUnlocked = wordSniperEarned,
                accentColor = GoldAccent,
                testTag = "badge_word_sniper"
            )

            BlitzAchievementBadgeRow(
                emoji = "⚡",
                titleEn = "Academic Lightning",
                titleAr = "البرق الأكاديمي",
                description = bi(
                    "Reach a 15-word streak on IELTS / Academic words (${maxOf(state.maxAcademicStreak, state.maxComboStreak)}/15)",
                    "تحقيق سلسلة 15 إجابة صحيحة متتالية (${maxOf(state.maxAcademicStreak, state.maxComboStreak)}/15)"
                ),
                isUnlocked = academicLightningEarned,
                accentColor = SkyAccent,
                testTag = "badge_academic_lightning"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPlayAgain,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldAccent,
                        contentColor = BackgroundDark
                    ),
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("blitz_play_again_btn")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(bi("Play Again", "العب مجدداً"), fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onChangeFilters,
                    border = BorderStroke(1.dp, GoldAccentBorder),
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("blitz_change_filters_btn")
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(bi("Filters", "تعديل الفلاتر"), color = GoldAccent)
                }

                OutlinedButton(
                    onClick = onDone,
                    border = BorderStroke(1.dp, BorderDark),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("blitz_done_btn")
                ) {
                    Text(bi("Done", "إنهاء"), color = TextPrimary)
                }
            }
        }
    }
}

@Composable
private fun BlitzStatPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = SurfaceElevatedDark,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderDark)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun BlitzAchievementBadgeRow(
    emoji: String,
    titleEn: String,
    titleAr: String,
    description: String,
    isUnlocked: Boolean,
    accentColor: Color,
    testTag: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        color = if (isUnlocked) accentColor.copy(alpha = 0.14f) else SurfaceElevatedDark,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isUnlocked) accentColor else BorderDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (isUnlocked) accentColor.copy(alpha = 0.25f) else SurfaceDark,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = emoji, fontSize = 20.sp)
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "$titleEn • $titleAr",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isUnlocked) accentColor else TextPrimary
                    )
                    if (isUnlocked) {
                        Surface(
                            color = EmeraldAccent.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = bi("UNLOCKED", "مفتوح ✓"),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 9.5.sp
                                ),
                                color = EmeraldAccent,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}
