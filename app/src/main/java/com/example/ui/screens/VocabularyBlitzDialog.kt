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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
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

@Composable
fun VocabularyBlitzDialog(
    initialQuestions: List<BlitzQuestionItem>,
    savedStats: VocabularyBlitzStats = VocabularyBlitzStats(),
    enableTimerAutoTick: Boolean = true,
    onPlayPronunciation: (String) -> Unit = {},
    onFinishRound: (VocabularyBlitzSessionState) -> Unit = {},
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
    onSpeakWord: (String) -> Unit,
    onSelectOption: (Int) -> Unit
) {
    val question = state.currentQuestion ?: return
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

    // HUD Row: Circular Fiery Timer + Live Score + Combo Multiplier
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

    // Active Question Card
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
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Meta Row: CEFR Level + Academic Badge + Audio Button
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

            // Target English Word (Always LTR)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = question.word,
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            textDirection = TextDirection.Ltr
                        ),
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("blitz_target_word")
                    )
                    if (question.pos.isNotBlank()) {
                        Text(
                            text = question.pos,
                            style = MaterialTheme.typography.labelMedium,
                            color = GoldAccent
                        )
                    }
                }
            }

            if (question.mode == BlitzQuestionMode.TRUE_FALSE) {
                Surface(
                    color = SurfaceElevatedDark,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, BorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = bi("Does this word mean:", "هل معنى هذه الكلمة هو:"),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        BidiText(
                            text = question.promptArabic,
                            style = ArabicTypography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
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
                    color = TextSecondary
                )
            }

            // Options Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (question.mode == BlitzQuestionMode.TRUE_FALSE) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        question.optionsArabic.forEachIndexed { idx, label ->
                            val accent = if (idx == 0) EmeraldAccent else CoralAccent
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onSelectOption(idx) }
                                    .testTag("blitz_option_$idx"),
                                color = accent.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.5.dp, accent)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = accent,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 18.dp, horizontal = 10.dp)
                                )
                            }
                        }
                    }
                } else {
                    question.optionsArabic.forEachIndexed { idx, optionAr ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectOption(idx) }
                                .testTag("blitz_option_$idx"),
                            color = SurfaceElevatedDark,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, GoldAccentBorder)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                BidiText(
                                    text = optionAr,
                                    style = ArabicTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary,
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
private fun BlitzSummaryContent(
    state: VocabularyBlitzSessionState,
    savedStats: VocabularyBlitzStats,
    onPlayAgain: () -> Unit,
    onDone: () -> Unit
) {
    val scrollState = rememberScrollState()
    val wordSniperEarned = state.unlockedWordSniperNow || savedStats.wordSniperUnlocked
    val academicLightningEarned = state.unlockedAcademicLightningNow || savedStats.academicLightningUnlocked

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
            }

            Text(
                text = "${state.score} pts",
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = GoldAccent,
                modifier = Modifier.testTag("blitz_final_score")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BlitzStatPill(
                    label = bi("Correct", "صحيحة"),
                    value = "${state.correctCount}",
                    color = EmeraldAccent
                )
                BlitzStatPill(
                    label = bi("Max Combo", "أعلى سلسلة"),
                    value = "${state.maxComboStreak} 🔥",
                    color = CoralAccent
                )
                BlitzStatPill(
                    label = bi("Accuracy", "الدقة"),
                    value = "${state.accuracyPercent}%",
                    color = SkyAccent
                )
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
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onPlayAgain,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldAccent,
                        contentColor = BackgroundDark
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("blitz_play_again_btn")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(bi("Play Again", "العب مجدداً"), fontWeight = FontWeight.Bold)
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
    color: Color
) {
    Surface(
        color = SurfaceElevatedDark,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderDark)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
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
