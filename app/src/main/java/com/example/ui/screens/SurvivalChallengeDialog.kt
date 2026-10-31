package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.blitz.BlitzQuestionItem
import com.example.data.blitz.BlitzQuestionMode
import com.example.data.blitz.BlitzSoundPlayer
import com.example.data.blitz.SurvivalSessionState
import com.example.data.blitz.VocabularyBlitzEngine
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
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevatedDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.bi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SurvivalChallengeDialog(
    initialQuestions: List<BlitzQuestionItem>,
    bestStreak: Int = 0,
    onFinish: (SurvivalSessionState) -> Unit,
    onPracticeMistakes: (List<BlitzQuestionItem>) -> Unit = {},
    onPlayPronunciation: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    var sessionState by remember {
        mutableStateOf(
            SurvivalSessionState(
                questions = initialQuestions,
                bestStreakInSession = 0
            )
        )
    }
    var soundEnabled by remember { mutableStateOf(true) }
    var tappedOptionIndex by remember { mutableStateOf<Int?>(null) }
    var isEvaluatingAnswer by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

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
                .testTag("survival_dialog"),
            color = BackgroundDark
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top Action Bar
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
                            shape = CircleShape,
                            color = CoralAccent.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, CoralAccent),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = CoralAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = bi("Survival Mode", "تحدي النجاة ❤️"),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = TextPrimary
                            )
                            Text(
                                text = bi("3 Hearts • Untimed Challenge", "٣ قلوب • تحدي مفتوح بدون وقت"),
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
                            modifier = Modifier.testTag("survival_sound_toggle_btn")
                        ) {
                            Icon(
                                imageVector = if (soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                                contentDescription = if (soundEnabled) "Mute SFX" else "Unmute SFX",
                                tint = if (soundEnabled) GoldAccent else TextMuted
                            )
                        }

                        IconButton(
                            onClick = {
                                onFinish(sessionState)
                                onDismiss()
                            },
                            modifier = Modifier.testTag("survival_close_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Exit Survival",
                                tint = TextSecondary
                            )
                        }
                    }
                }

                if (!sessionState.isFinished) {
                    // HUD: 3 Hearts + Current Streak + Best Streak
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = SurfaceDark,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, BorderDark)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Hearts Display
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.testTag("survival_hearts_row")
                            ) {
                                for (i in 0 until sessionState.maxHearts) {
                                    val isAlive = i < sessionState.hearts
                                    Icon(
                                        imageVector = if (isAlive) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = null,
                                        tint = if (isAlive) CoralAccent else TextMuted,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .scale(if (isAlive) 1.0f else 0.85f)
                                    )
                                }
                            }

                            // Current Streak
                            Surface(
                                color = if (sessionState.currentStreak > 0) CoralAccent.copy(alpha = 0.16f) else SurfaceElevatedDark,
                                shape = PillShape,
                                border = BorderStroke(1.dp, if (sessionState.currentStreak > 0) CoralAccent else BorderDark),
                                modifier = Modifier.testTag("survival_streak_pill")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = if (sessionState.currentStreak > 0) CoralAccent else TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "${sessionState.currentStreak}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                        color = if (sessionState.currentStreak > 0) CoralAccent else TextSecondary
                                    )
                                    Text(
                                        text = bi("Streak", "سلسلة"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }

                            // Personal Best Streak
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.testTag("survival_best_streak_pill")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = bi("BEST", "الأعلى"),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "$bestStreak",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = GoldAccent
                                    )
                                }
                            }
                        }
                    }

                    // Active Question Area
                    val currentQ = sessionState.currentQuestion
                    if (currentQ != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .testTag("survival_question_card"),
                            color = SurfaceDark,
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, GoldAccentBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 18.dp, vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Question Header (Level, Pos, Pronunciation)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            color = GoldAccentDim,
                                            shape = PillShape,
                                            border = BorderStroke(1.dp, GoldAccentBorder)
                                        ) {
                                            Text(
                                                text = currentQ.level,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                color = GoldAccent,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        if (currentQ.pos.isNotBlank()) {
                                            Text(
                                                text = currentQ.pos,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = TextSecondary
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { onPlayPronunciation(currentQ.word) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "Pronounce",
                                            tint = GoldAccent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                // English Word in Large Typography (tightened spacing)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                ) {
                                    Text(
                                        text = currentQ.word,
                                        style = MaterialTheme.typography.headlineLarge.copy(
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 0.5.sp
                                        ),
                                        color = TextPrimary,
                                        textAlign = TextAlign.Center
                                    )
                                    val cleanIpa = currentQ.ipa.trim().trim('/')
                                    if (cleanIpa.isNotBlank()) {
                                        Text(
                                            text = "/$cleanIpa/",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }

                                    if (currentQ.mode == BlitzQuestionMode.TRUE_FALSE) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = bi("Does this mean?", "هل تعني بالعربية:"),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = TextSecondary
                                        )
                                        Text(
                                            text = "« ${currentQ.promptArabic} »",
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                            color = AmberAccent,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }

                                // Answer Options Zone
                                if (currentQ.mode == BlitzQuestionMode.TRUE_FALSE) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        currentQ.optionsArabic.forEachIndexed { idx, optionText ->
                                            val isSelected = tappedOptionIndex == idx
                                            val isCorrectOption = idx == currentQ.correctOptionIndex
                                            val isFirst = idx == 0
                                            val baseAccent = if (isFirst) Color(0xFF34D399) else Color(0xFFFB7185)

                                            val cardColor = when {
                                                isEvaluatingAnswer && isCorrectOption -> Color(0xFF34D399).copy(alpha = 0.28f)
                                                isEvaluatingAnswer && isSelected && !isCorrectOption -> Color(0xFFFB7185).copy(alpha = 0.28f)
                                                isSelected -> baseAccent.copy(alpha = 0.22f)
                                                else -> baseAccent.copy(alpha = 0.10f)
                                            }

                                            val borderColor = when {
                                                isEvaluatingAnswer && isCorrectOption -> Color(0xFF34D399)
                                                isEvaluatingAnswer && isSelected && !isCorrectOption -> Color(0xFFFB7185)
                                                isSelected -> baseAccent
                                                else -> baseAccent.copy(alpha = 0.50f)
                                            }

                                            val topLabel = if (isFirst) "✓ Correct" else "✗ False"
                                            val bottomLabel = if (isFirst) "صحيح" else "خاطئ"

                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(62.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .clickable(enabled = !isEvaluatingAnswer) {
                                                        tappedOptionIndex = idx
                                                        isEvaluatingAnswer = true

                                                        val isCorrect = idx == currentQ.correctOptionIndex
                                                        if (isCorrect) {
                                                            BlitzSoundPlayer.playCorrectChime(soundEnabled)
                                                        } else {
                                                            BlitzSoundPlayer.playWrongBuzz(soundEnabled)
                                                        }

                                                        coroutineScope.launch {
                                                            val delayMs = if (isCorrect) 200L else 900L
                                                            delay(delayMs)

                                                            val newState = VocabularyBlitzEngine.submitSurvivalAnswer(
                                                                state = sessionState,
                                                                selectedOptionIndex = idx,
                                                                previousBestStreak = bestStreak
                                                            )
                                                            if (newState.heartRecoveredThisTurn) {
                                                                BlitzSoundPlayer.playComboLevelUpChime(soundEnabled)
                                                            }
                                                            sessionState = newState
                                                            tappedOptionIndex = null
                                                            isEvaluatingAnswer = false

                                                            if (newState.isFinished) {
                                                                onFinish(newState)
                                                            }
                                                        }
                                                    }
                                                    .testTag("survival_option_$idx"),
                                                color = cardColor,
                                                shape = RoundedCornerShape(14.dp),
                                                border = BorderStroke(1.5.dp, borderColor)
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    val textColor = when {
                                                        isEvaluatingAnswer && isCorrectOption -> Color(0xFF34D399)
                                                        isEvaluatingAnswer && isSelected && !isCorrectOption -> Color(0xFFFB7185)
                                                        else -> baseAccent
                                                    }
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
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        currentQ.optionsArabic.forEachIndexed { idx, optionText ->
                                            val isSelected = tappedOptionIndex == idx
                                            val isCorrectOption = idx == currentQ.correctOptionIndex

                                            val cardColor = when {
                                                isEvaluatingAnswer && isCorrectOption -> EmeraldAccent.copy(alpha = 0.22f)
                                                isEvaluatingAnswer && isSelected && !isCorrectOption -> CoralAccent.copy(alpha = 0.22f)
                                                isSelected -> GoldAccent.copy(alpha = 0.16f)
                                                else -> SurfaceElevatedDark
                                            }

                                            val borderColor = when {
                                                isEvaluatingAnswer && isCorrectOption -> EmeraldAccent
                                                isEvaluatingAnswer && isSelected && !isCorrectOption -> CoralAccent
                                                isSelected -> GoldAccent
                                                else -> BorderDark
                                            }

                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .clickable(enabled = !isEvaluatingAnswer) {
                                                        tappedOptionIndex = idx
                                                        isEvaluatingAnswer = true

                                                        val isCorrect = idx == currentQ.correctOptionIndex
                                                        if (isCorrect) {
                                                            BlitzSoundPlayer.playCorrectChime(soundEnabled)
                                                        } else {
                                                            BlitzSoundPlayer.playWrongBuzz(soundEnabled)
                                                        }

                                                        coroutineScope.launch {
                                                            val delayMs = if (isCorrect) 200L else 900L
                                                            delay(delayMs)

                                                            val newState = VocabularyBlitzEngine.submitSurvivalAnswer(
                                                                state = sessionState,
                                                                selectedOptionIndex = idx,
                                                                previousBestStreak = bestStreak
                                                            )
                                                            if (newState.heartRecoveredThisTurn) {
                                                                BlitzSoundPlayer.playComboLevelUpChime(soundEnabled)
                                                            }
                                                            sessionState = newState
                                                            tappedOptionIndex = null
                                                            isEvaluatingAnswer = false

                                                            if (newState.isFinished) {
                                                                onFinish(newState)
                                                            }
                                                        }
                                                    }
                                                    .testTag("survival_option_$idx"),
                                                color = cardColor,
                                                shape = RoundedCornerShape(14.dp),
                                                border = BorderStroke(1.5.dp, borderColor)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                                    horizontalArrangement = Arrangement.Center,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = optionText,
                                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = when {
                                                            isEvaluatingAnswer && isCorrectOption -> EmeraldAccent
                                                            isEvaluatingAnswer && isSelected && !isCorrectOption -> CoralAccent
                                                            else -> TextPrimary
                                                        },
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
                } else {
                    // GAME OVER / SUMMARY SCREEN
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("survival_game_over_card"),
                        color = SurfaceDark,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, CoralAccent.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 18.dp, vertical = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Header
                            Icon(
                                imageVector = Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = CoralAccent,
                                modifier = Modifier.size(54.dp)
                            )
                            Text(
                                text = bi("Game Over!", "انتهت المحاولة! 💔"),
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                                color = CoralAccent
                            )

                            if (sessionState.isNewBestStreak) {
                                Surface(
                                    color = GoldAccentDim,
                                    shape = PillShape,
                                    border = BorderStroke(1.dp, GoldAccent)
                                ) {
                                    Text(
                                        text = bi("🎉 NEW BEST STREAK!", "🎉 رقم قياسي جديد في النجاة!"),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = GoldAccent,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            // Summary Metrics
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MetricCard(
                                    modifier = Modifier.weight(1f),
                                    titleEn = "Best Streak",
                                    titleAr = "أعلى سلسلة",
                                    value = "${sessionState.bestStreakInSession}",
                                    valueColor = CoralAccent
                                )
                                MetricCard(
                                    modifier = Modifier.weight(1f),
                                    titleEn = "Answered",
                                    titleAr = "إجمالي الكلمات",
                                    value = "${sessionState.totalAnswered}",
                                    valueColor = GoldAccent
                                )
                                MetricCard(
                                    modifier = Modifier.weight(1f),
                                    titleEn = "Accuracy",
                                    titleAr = "الدقة",
                                    value = "${sessionState.accuracyPercent}%",
                                    valueColor = EmeraldAccent
                                )
                            }

                            // Missed Words Review
                            if (sessionState.missedQuestions.isNotEmpty()) {
                                HorizontalDivider(color = BorderDark)
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = bi("Words That Cost Your Hearts:", "الكلمات التي تسببت في الخسارة:"),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = TextSecondary
                                    )
                                    sessionState.missedQuestions.forEach { missed ->
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            color = SurfaceElevatedDark,
                                            shape = RoundedCornerShape(12.dp),
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
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            text = missed.word,
                                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                                            color = CoralAccent
                                                        )
                                                        Text(
                                                            text = missed.level,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = TextMuted
                                                        )
                                                    }
                                                    Text(
                                                        text = missed.trueArabicMeaning,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = TextPrimary
                                                    )
                                                }

                                                IconButton(onClick = { onPlayPronunciation(missed.word) }) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                        contentDescription = "Pronounce",
                                                        tint = GoldAccent,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Action Buttons
                            if (sessionState.missedQuestions.isNotEmpty()) {
                                Button(
                                    onClick = {
                                        onPracticeMistakes(sessionState.missedQuestions)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AmberAccent,
                                        contentColor = BackgroundDark
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("survival_practice_mistakes_btn")
                                ) {
                                    Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = bi("Practice Mistakes (Update in SM-2)", "مراجعة الأخطاء وتحديث التكرار (SM-2)"),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        onFinish(sessionState)
                                        onDismiss()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = bi("Close", "إغلاق"), color = TextSecondary)
                                }

                                Button(
                                    onClick = {
                                        sessionState = SurvivalSessionState(
                                            questions = initialQuestions.shuffled(),
                                            bestStreakInSession = 0
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CoralAccent,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1.5f)
                                        .testTag("survival_play_again_btn")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = bi("Try Again", "إعادة التحدي ❤️"), fontWeight = FontWeight.Bold)
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
private fun MetricCard(
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
