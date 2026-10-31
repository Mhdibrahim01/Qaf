package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.blitz.BlitzSoundPlayer
import com.example.data.grammar.GrammarErrorHunterEngine
import com.example.data.grammar.GrammarHunterItem
import com.example.data.grammar.GrammarHunterSessionState
import com.example.data.grammar.HunterStep
import com.example.data.local.GrammarLessonEntity
import com.example.ui.components.BiDiParsedText
import com.example.ui.components.FloatingFeedbackBadge
import com.example.ui.components.GameFeedbackColors
import com.example.ui.components.GameFeedbackState
import com.example.ui.components.SmoothQuestionSlideContent
import com.example.ui.components.gameFeedbackMotion
import com.example.ui.components.performGameFeedback
import com.example.ui.components.rememberFeedbackBackgroundColor
import com.example.ui.components.rememberFeedbackBorderColor
import com.example.ui.components.rememberGameFeedbackController
import com.example.ui.components.streakBumpScale
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GrammarErrorHunterDialog(
    lessons: List<GrammarLessonEntity>,
    userLevel: String = "B2",
    bestScore: Int = 0,
    onFinish: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var sessionState by remember {
        mutableStateOf(GrammarErrorHunterEngine.createSession(lessons, userLevel))
    }
    var soundEnabled by remember { mutableStateOf(true) }
    var lastTappedOptionText by remember { mutableStateOf<String?>(null) }

    val feedbackController = rememberGameFeedbackController()
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val triggerFeedbackBanner: (Boolean, String) -> Unit = { isCorrect, msg ->
        haptic.performGameFeedback(isCorrect)
        if (isCorrect) {
            feedbackController.triggerCorrect(msg)
        } else {
            feedbackController.triggerWrong(msg)
        }
        coroutineScope.launch {
            delay(340L)
            feedbackController.resetCardState()
            delay(260L)
            feedbackController.hideToast()
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
                .testTag("grammar_hunter_dialog"),
            color = BackgroundDark
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF231D12),
                            border = BorderStroke(1.5.dp, GoldAccent),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = bi("Grammar Error Hunter", "صائد الأخطاء القواعدية 🔍"),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                ),
                                color = TextPrimary
                            )
                            Text(
                                text = bi("Spot the Error & Pick Correction", "اكتشف الخطأ في الجملة واختر التصحيح المناسب"),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
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
                            modifier = Modifier.testTag("hunter_sound_toggle_btn")
                        ) {
                            Icon(
                                imageVector = if (soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                                contentDescription = if (soundEnabled) "Mute SFX" else "Unmute SFX",
                                tint = if (soundEnabled) GoldAccent else TextMuted
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("hunter_close_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Exit Grammar Hunter",
                                tint = TextSecondary
                            )
                        }
                    }
                }

                if (!sessionState.isFinished) {
                    // HUD Progress Row
                    val currentItem = sessionState.currentItem
                    val isScoreBump = feedbackController.state == GameFeedbackState.CORRECT

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
                            // Question count (Sentence X of 10)
                            Text(
                                text = bi(
                                    "Sentence ${sessionState.currentIndex + 1} of ${sessionState.items.size}",
                                    "الجملة ${sessionState.currentIndex + 1} من ${sessionState.items.size}"
                                ),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.5.sp
                                ),
                                color = TextPrimary,
                                softWrap = false,
                                maxLines = 1,
                                modifier = Modifier.testTag("hunter_progress_text")
                            )

                            // Current Topic Badge (Truncates cleanly with ellipsis, never crushes score pill)
                            if (currentItem != null) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .padding(horizontal = 8.dp)
                                ) {
                                    Surface(
                                        color = Color(0xFF231D12),
                                        shape = PillShape,
                                        border = BorderStroke(1.dp, GoldAccentBorder)
                                    ) {
                                        Text(
                                            text = currentItem.lessonTopic,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 11.5.sp
                                            ),
                                            color = GoldAccent,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            // Score pill (Enforces softWrap = false and maxLines = 1 so it never wraps vertically)
                            Surface(
                                color = if (isScoreBump) GameFeedbackColors.EmeraldTint else EmeraldAccent.copy(alpha = 0.12f),
                                shape = PillShape,
                                border = BorderStroke(
                                    1.dp,
                                    if (isScoreBump) GameFeedbackColors.EmeraldBorder else EmeraldAccent.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.streakBumpScale(activeBump = isScoreBump)
                            ) {
                                Text(
                                    text = "✓ ${sessionState.score}/${sessionState.currentIndex}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp
                                    ),
                                    color = GameFeedbackColors.EmeraldBorder,
                                    softWrap = false,
                                    maxLines = 1,
                                    modifier = Modifier
                                        .padding(horizontal = 11.dp, vertical = 4.dp)
                                        .testTag("hunter_score_pill")
                                )
                            }
                        }
                    }

                    // Main Interactive Card (Wraps content cleanly without excessive empty black void)
                    if (currentItem != null) {
                        val mainCardBorderColor = rememberFeedbackBorderColor(
                            feedbackState = feedbackController.state,
                            idleColor = GoldAccentBorder
                        )
                        val mainCardBorderWidth = if (feedbackController.state != GameFeedbackState.IDLE) 2.dp else 1.dp

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .gameFeedbackMotion(
                                    feedbackState = feedbackController.state,
                                    triggerKey = feedbackController.triggerCounter,
                                    correctPeakScale = 1.03f
                                )
                                .testTag("hunter_main_card"),
                            color = SurfaceDark,
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(mainCardBorderWidth, mainCardBorderColor)
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight()
                                        .padding(horizontal = 16.dp, vertical = 18.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Step Instructions
                                    val instructionText = when (sessionState.currentStep) {
                                        HunterStep.STEP_1_SPOT_ERROR -> bi(
                                            "Step 1: Tap the faulty word in the sentence (or 'No Error')",
                                            "الخطوة الأولى: المس الكلمة الخاطئة في الجملة (أو زر 'الجملة صحيحة')"
                                        )
                                        HunterStep.STEP_2_CHOOSE_CORRECTION -> bi(
                                            "Step 2: Choose the correct grammar replacement below",
                                            "الخطوة الثانية: اختر التصحيح القواعدي المناسب من الخيارات أسفله"
                                        )
                                        HunterStep.REVEAL_EXPLANATION -> bi(
                                            "Grammar Rule & Explanation",
                                            "شرح القاعدة والتصحيح القواعدي 💡"
                                        )
                                    }
                                    Text(
                                        text = instructionText,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = GoldAccent
                                    )

                                    // Interactive Clickable Word Tokens of the Sentence wrapped in 180ms Smooth Slide Transition
                                    SmoothQuestionSlideContent(
                                        targetState = currentItem,
                                        contentKey = { "${sessionState.currentIndex}_${it.lessonTopic}_${it.tokens.joinToString("_")}" },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { animatedItem ->
                                        val tokenBoxBorderColor = rememberFeedbackBorderColor(
                                            feedbackState = feedbackController.state,
                                            idleColor = BorderDark
                                        )
                                        val tokenBoxBgColor = rememberFeedbackBackgroundColor(
                                            feedbackState = feedbackController.state,
                                            idleColor = SurfaceElevatedDark
                                        )

                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            color = tokenBoxBgColor,
                                            shape = RoundedCornerShape(14.dp),
                                            border = BorderStroke(
                                                if (feedbackController.state != GameFeedbackState.IDLE) 2.dp else 1.dp,
                                                tokenBoxBorderColor
                                            )
                                        ) {
                                            FlowRow(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(14.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                animatedItem.tokens.forEachIndexed { tokenIdx, tokenText ->
                                                    val isSelectedToken = sessionState.selectedTokenIndex == tokenIdx
                                                    val isFaultyToken = tokenIdx == animatedItem.errorTokenIndex

                                                    val tokenFeedbackState = when {
                                                        isSelectedToken && feedbackController.state != GameFeedbackState.IDLE -> feedbackController.state
                                                        else -> GameFeedbackState.IDLE
                                                    }

                                                    val tokenColor = when {
                                                        sessionState.currentStep == HunterStep.REVEAL_EXPLANATION && isFaultyToken -> {
                                                            if (sessionState.step2Correct == true) GameFeedbackColors.EmeraldTint else GameFeedbackColors.RoseTint
                                                        }
                                                        sessionState.currentStep == HunterStep.STEP_2_CHOOSE_CORRECTION && isFaultyToken -> GameFeedbackColors.EmeraldTint
                                                        isSelectedToken -> GoldAccent.copy(alpha = 0.2f)
                                                        else -> SurfaceDark
                                                    }

                                                    val borderColor = when {
                                                        sessionState.currentStep == HunterStep.REVEAL_EXPLANATION && isFaultyToken -> {
                                                            if (sessionState.step2Correct == true) GameFeedbackColors.EmeraldBorder else GameFeedbackColors.RoseBorder
                                                        }
                                                        sessionState.currentStep == HunterStep.STEP_2_CHOOSE_CORRECTION && isFaultyToken -> GameFeedbackColors.EmeraldBorder
                                                        isSelectedToken -> GoldAccent
                                                        else -> BorderDark
                                                    }

                                                    // Text to show inside chip
                                                    val displayText = if (sessionState.currentStep == HunterStep.REVEAL_EXPLANATION && isFaultyToken && sessionState.step2Correct == true) {
                                                        animatedItem.correctReplacement
                                                    } else {
                                                        tokenText
                                                    }

                                                    val spotCorrectToast = bi("✓ Error Spotted! Pick correction", "✓ أصبت الخطأ! اختر التصحيح")
                                                    val spotWrongToast = bi("✗ Not the faulty word!", "✗ لم تصب الكلمة الخاطئة!")

                                                    Surface(
                                                        modifier = Modifier
                                                            .gameFeedbackMotion(
                                                                feedbackState = tokenFeedbackState,
                                                                triggerKey = feedbackController.triggerCounter,
                                                                correctPeakScale = 1.06f
                                                            )
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .clickable(enabled = sessionState.currentStep == HunterStep.STEP_1_SPOT_ERROR) {
                                                                val outcome = GrammarErrorHunterEngine.spotError(
                                                                    sessionState,
                                                                    tokenIndex = tokenIdx,
                                                                    noErrorTapped = false
                                                                )
                                                                val isCorrect = outcome.step1Correct == true
                                                                if (isCorrect) {
                                                                    BlitzSoundPlayer.playCorrectChime(soundEnabled)
                                                                    triggerFeedbackBanner(
                                                                        true,
                                                                        spotCorrectToast
                                                                    )
                                                                } else {
                                                                    BlitzSoundPlayer.playWrongBuzz(soundEnabled)
                                                                    triggerFeedbackBanner(
                                                                        false,
                                                                        spotWrongToast
                                                                    )
                                                                }
                                                                sessionState = outcome
                                                            }
                                                            .testTag("hunter_token_$tokenIdx"),
                                                        color = tokenColor,
                                                        shape = RoundedCornerShape(8.dp),
                                                        border = BorderStroke(
                                                            if (isFaultyToken || isSelectedToken) 1.6.dp else 1.2.dp,
                                                            borderColor
                                                        )
                                                    ) {
                                                        Text(
                                                            text = displayText,
                                                            style = MaterialTheme.typography.titleMedium.copy(
                                                                fontWeight = if (isFaultyToken || isSelectedToken) FontWeight.Black else FontWeight.Medium
                                                            ),
                                                            color = when {
                                                                sessionState.currentStep == HunterStep.REVEAL_EXPLANATION && isFaultyToken && sessionState.step2Correct == true -> GameFeedbackColors.EmeraldBorder
                                                                sessionState.currentStep == HunterStep.REVEAL_EXPLANATION && isFaultyToken && sessionState.step2Correct == false -> GameFeedbackColors.RoseBorder
                                                                sessionState.currentStep == HunterStep.STEP_2_CHOOSE_CORRECTION && isFaultyToken -> GameFeedbackColors.EmeraldBorder
                                                                isSelectedToken -> GoldAccent
                                                                else -> TextPrimary
                                                            },
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Dedicated "No Error" Button (Available during Step 1)
                                    if (sessionState.currentStep == HunterStep.STEP_1_SPOT_ERROR) {
                                        val noErrorCorrectToast = bi("+1 pt 🔥 Sentence is Correct! • أحسنت!", "+1 نقطة 🔥 الجملة صحيحة! أحسنت!")
                                        val noErrorWrongToast = bi("✗ Has an error! Review rule", "✗ الجملة بها خطأ قواعدي!")
                                        OutlinedButton(
                                            onClick = {
                                                val outcome = GrammarErrorHunterEngine.spotError(
                                                    sessionState,
                                                    tokenIndex = null,
                                                    noErrorTapped = true
                                                )
                                                val isCorrect = outcome.step1Correct == true
                                                if (isCorrect) {
                                                    BlitzSoundPlayer.playCorrectChime(soundEnabled)
                                                    triggerFeedbackBanner(
                                                        true,
                                                        noErrorCorrectToast
                                                    )
                                                } else {
                                                    BlitzSoundPlayer.playWrongBuzz(soundEnabled)
                                                    triggerFeedbackBanner(
                                                        false,
                                                        noErrorWrongToast
                                                    )
                                                }
                                                sessionState = outcome
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.2.dp, EmeraldAccent),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldAccent),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("hunter_no_error_btn")
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = bi("No Error • Sentence is Correct", "الجملة صحيحة قواعدياً (خالية من الأخطاء) ✓"),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    // Step 2 Area: Correction Options (Full-Width Stack -> Zero Mid-Word Wrapping!)
                                    if (sessionState.currentStep == HunterStep.STEP_2_CHOOSE_CORRECTION) {
                                        val step2CorrectToast = bi("+1 pt 🔥 أحسنت! تصحيح سليم", "+1 نقطة 🔥 أحسنت! تصحيح سليم")
                                        val step2WrongToast = bi("✗ Wrong replacement! • تصحيح خاطئ!", "✗ تصحيح خاطئ! راجع القاعدة")
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Text(
                                                text = bi("Select the correct replacement:", "اختر التصحيح القواعدي المناسب:"),
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp
                                                ),
                                                color = TextSecondary
                                            )

                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                currentItem.correctionOptions.forEachIndexed { optIdx, optionText ->
                                                    val letter = ('A'.code + optIdx).toChar().toString()
                                                    val isTappedOption = lastTappedOptionText == optionText
                                                    Surface(
                                                        onClick = {
                                                            lastTappedOptionText = optionText
                                                            val outcome = GrammarErrorHunterEngine.submitCorrection(
                                                                sessionState,
                                                                chosenCorrection = optionText,
                                                                previousBestScore = bestScore
                                                            )
                                                            val isCorrect = outcome.step2Correct == true
                                                            if (isCorrect) {
                                                                BlitzSoundPlayer.playCorrectChime(soundEnabled)
                                                                triggerFeedbackBanner(
                                                                    true,
                                                                    step2CorrectToast
                                                                )
                                                            } else {
                                                                BlitzSoundPlayer.playWrongBuzz(soundEnabled)
                                                                triggerFeedbackBanner(
                                                                    false,
                                                                    step2WrongToast
                                                                )
                                                            }
                                                            sessionState = outcome
                                                        },
                                                        color = SurfaceElevatedDark,
                                                        shape = RoundedCornerShape(13.dp),
                                                        border = BorderStroke(
                                                            if (isTappedOption) 2.dp else 1.dp,
                                                            GoldAccentBorder
                                                        ),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .testTag("hunter_correction_opt_$optIdx")
                                                    ) {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(28.dp)
                                                                    .clip(RoundedCornerShape(8.dp))
                                                                    .background(Color(0xFF251F13))
                                                                    .border(1.dp, GoldAccentBorder, RoundedCornerShape(8.dp)),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(
                                                                    text = letter,
                                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                                        fontWeight = FontWeight.Bold,
                                                                        fontSize = 12.5.sp
                                                                    ),
                                                                    color = GoldAccent
                                                                )
                                                            }

                                                            Text(
                                                                text = optionText,
                                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    fontSize = 15.sp
                                                                ),
                                                                color = TextPrimary,
                                                                maxLines = 1,
                                                                softWrap = false,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Reveal Explanation Card (BiDi-Safe RTL Explanation & Isolates!)
                                    if (sessionState.currentStep == HunterStep.REVEAL_EXPLANATION) {
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            color = if (sessionState.step1Correct == true && sessionState.step2Correct == true) {
                                                GameFeedbackColors.EmeraldTint
                                            } else {
                                                GameFeedbackColors.RoseTint
                                            },
                                            shape = RoundedCornerShape(16.dp),
                                            border = BorderStroke(
                                                1.5.dp,
                                                if (sessionState.step1Correct == true && sessionState.step2Correct == true) {
                                                    GameFeedbackColors.EmeraldBorder
                                                } else {
                                                    GameFeedbackColors.RoseBorder
                                                }
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(14.dp),
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                // LTR Header Row
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (sessionState.step1Correct == true && sessionState.step2Correct == true) {
                                                            Icons.Default.CheckCircle
                                                        } else Icons.AutoMirrored.Filled.HelpOutline,
                                                        contentDescription = null,
                                                        tint = if (sessionState.step1Correct == true && sessionState.step2Correct == true) GameFeedbackColors.EmeraldBorder else GameFeedbackColors.RoseBorder,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Text(
                                                        text = if (sessionState.step1Correct == true && sessionState.step2Correct == true) {
                                                            bi("Correct! +1 point", "إجابة صحيحة! +١ نقطة ✓")
                                                        } else {
                                                            bi("Needs Review", "تحتاج لمراجعة 💡")
                                                        },
                                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = if (sessionState.step1Correct == true && sessionState.step2Correct == true) GameFeedbackColors.EmeraldBorder else GameFeedbackColors.RoseBorder
                                                    )
                                                }

                                                // Arabic Explanation with BiDi Isolates and RTL Layout Direction
                                                BiDiParsedText(
                                                    text = currentItem.explanationAr,
                                                    style = ArabicTypography.bodyMedium.copy(
                                                        fontSize = 14.sp,
                                                        lineHeight = 23.sp
                                                    ),
                                                    color = TextPrimary,
                                                    isMistakeTheme = !(sessionState.step1Correct == true && sessionState.step2Correct == true)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        // High-Contrast Gold Next Button (Dark Navy Text on Gold)
                                        Button(
                                            onClick = {
                                                lastTappedOptionText = null
                                                feedbackController.resetCardState()
                                                feedbackController.hideToast()
                                                val next = GrammarErrorHunterEngine.advanceNext(sessionState, bestScore)
                                                sessionState = next
                                                if (next.isFinished) {
                                                    onFinish(next.score)
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = GoldAccent,
                                                contentColor = Color(0xFF0B0D12)
                                            ),
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(50.dp)
                                                .testTag("hunter_next_btn")
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = bi("Next Sentence", "الجملة التالية"),
                                                    style = MaterialTheme.typography.bodyLarge.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 15.sp,
                                                        color = Color(0xFF0B0D12)
                                                    )
                                                )
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                    contentDescription = null,
                                                    tint = Color(0xFF0B0D12),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Floating Feedback Badge (+1 pt 🔥 أحسنت! / ✗ خطأ! راجع القاعدة)
                                FloatingFeedbackBadge(
                                    visible = feedbackController.toastVisible,
                                    feedbackState = feedbackController.state,
                                    text = feedbackController.toastMessage,
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = 12.dp)
                                )
                            }
                        }
                    }
                } else {
                    // SUMMARY RESULTS SCREEN (Wraps cleanly without empty voids)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .testTag("hunter_summary_card"),
                        color = SurfaceDark,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, AmberAccent.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .padding(horizontal = 18.dp, vertical = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(52.dp)
                            )
                            Text(
                                text = bi("Grammar Challenge Complete!", "اكتمل تحدي صيد الأخطاء! 🔍🎯"),
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                                color = GoldAccent,
                                textAlign = TextAlign.Center
                            )

                            if (sessionState.isNewBestScore) {
                                Surface(
                                    color = GoldAccentDim,
                                    shape = PillShape,
                                    border = BorderStroke(1.dp, GoldAccent)
                                ) {
                                    Text(
                                        text = bi("🎉 NEW BEST SCORE!", "🎉 رقم قياسي جديد في القواعد!"),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = GoldAccent,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            // Metrics Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MetricCardItem(
                                    modifier = Modifier.weight(1f),
                                    titleEn = "Score",
                                    titleAr = "الدرجة",
                                    value = "${sessionState.score}/${sessionState.items.size}",
                                    valueColor = EmeraldAccent
                                )
                                MetricCardItem(
                                    modifier = Modifier.weight(1f),
                                    titleEn = "Accuracy",
                                    titleAr = "نسبة الدقة",
                                    value = "${sessionState.accuracyPercent}%",
                                    valueColor = GoldAccent
                                )
                                MetricCardItem(
                                    modifier = Modifier.weight(1f),
                                    titleEn = "Best Score",
                                    titleAr = "أفضل نتيجة",
                                    value = "$bestScore/10",
                                    valueColor = SkyAccent
                                )
                            }

                            // Missed Sentences Review
                            if (sessionState.missedItems.isNotEmpty()) {
                                HorizontalDivider(color = BorderDark)
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = bi("Review Missed Sentences:", "مراجعة الجمل التي أخطأت فيها:"),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = TextSecondary
                                    )

                                    sessionState.missedItems.forEach { item ->
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            color = SurfaceElevatedDark,
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, BorderDark)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = item.lessonTopic,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = AmberAccent
                                                    )
                                                    Text(
                                                        text = item.level,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = TextMuted
                                                    )
                                                }
                                                Text(
                                                    text = item.tokens.joinToString(" "),
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                                    color = TextPrimary
                                                )
                                                BiDiParsedText(
                                                    text = item.explanationAr,
                                                    style = ArabicTypography.bodySmall.copy(
                                                        fontSize = 12.5.sp,
                                                        lineHeight = 19.sp
                                                    ),
                                                    color = TextSecondary,
                                                    isMistakeTheme = true
                                                )
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

                                if (sessionState.missedItems.isNotEmpty()) {
                                    Button(
                                        onClick = {
                                            sessionState = GrammarErrorHunterEngine.retryMistakes(sessionState)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = AmberAccent,
                                            contentColor = BackgroundDark
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1.5f)
                                            .testTag("hunter_retry_mistakes_btn")
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = bi("Retry Mistakes", "إعادة الأخطاء 🔍"), fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            sessionState = GrammarErrorHunterEngine.createSession(lessons, userLevel)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = GoldAccent,
                                            contentColor = BackgroundDark
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1.5f)
                                            .testTag("hunter_play_again_btn")
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = bi("New Challenge", "تحدي جديد 🎯"), fontWeight = FontWeight.Bold)
                                    }
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
private fun MetricCardItem(
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
