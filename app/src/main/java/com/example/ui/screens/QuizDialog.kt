package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.QuizQuestionItem
import com.example.ui.components.BidiText
import com.example.ui.components.cleanGrammarArabicTitle
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

private fun normalizeSentenceForComparison(text: String): String {
    return text.trim()
        .replace(Regex("\\s+"), " ")
        .replace(Regex("\\s+([.,!?;:])"), "$1")
        .lowercase()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuizDialog(
    title: String,
    quizType: String,
    questions: List<QuizQuestionItem>,
    onPlayAudio: (String) -> Unit,
    onQuizCompleted: (userAnswers: Map<String, Int>) -> Unit,
    onOpenDictionaryWord: ((String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    if (questions.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = SurfaceDark,
            title = { Text(title, color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("No questions could be generated. Please make sure you have translated words and lessons.", color = TextMuted) },
            confirmButton = { Button(onClick = onDismiss) { Text("OK") } }
        )
        return
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    val userAnswers = remember { mutableStateMapOf<String, Int>() }
    var selectedOptionIndex by remember(currentIndex) { mutableStateOf<Int?>(null) }
    var arrangedWordIndices by remember(currentIndex) { mutableStateOf<List<Int>>(emptyList()) }
    var isSubmitted by remember(currentIndex) { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }

    val currentQuestion = questions[currentIndex]
    val isReorderQuestion = remember(currentQuestion) {
        currentQuestion.questionType == "grammar_reorder" ||
            (currentQuestion.promptEn.contains("رتّب") && currentQuestion.correctText.trim().contains(" ")) ||
            (currentQuestion.options.size >= 4 &&
                !currentQuestion.options.contains(currentQuestion.correctText) &&
                currentQuestion.correctText.trim().contains(" "))
    }

    val arrangedSentence = remember(arrangedWordIndices, currentQuestion) {
        arrangedWordIndices
            .mapNotNull { idx -> currentQuestion.options.getOrNull(idx) }
            .joinToString(" ")
    }

    val isReorderAnswerCorrect = remember(arrangedSentence, currentQuestion) {
        normalizeSentenceForComparison(arrangedSentence) ==
            normalizeSentenceForComparison(currentQuestion.correctText)
    }

    AlertDialog(
        onDismissRequest = { if (isFinished) onDismiss() },
        containerColor = SurfaceDark,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = if (!isFinished) "Question ${currentIndex + 1} of ${questions.size} • Skill: ${currentQuestion.skill.uppercase()}" else "Quiz Summary & Word Review",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldAccent
                    )
                }
            }
        },
        text = {
            if (!isFinished) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { (currentIndex + 1).toFloat() / questions.size.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                        color = GoldAccent,
                        trackColor = SurfaceElevatedDark
                    )

                    // Prompt Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = SurfaceElevatedDark,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BorderDark)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            BidiText(
                                text = currentQuestion.promptEn,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary
                            )
                            if (currentQuestion.promptAr.isNotBlank()) {
                                BidiText(
                                    text = cleanGrammarArabicTitle(currentQuestion.promptAr),
                                    style = ArabicTypography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                            if (currentQuestion.audioText.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { onPlayAudio(currentQuestion.audioText) },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, GoldAccentBorder),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldAccent)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Play audio", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text("Play Audio")
                                }
                            }
                        }
                    }

                    if (isReorderQuestion) {
                        // Interactive Word Arrangement UI
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Tap words in order to build the sentence:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )
                                if (!isSubmitted && arrangedWordIndices.isNotEmpty()) {
                                    TextButton(
                                        onClick = { arrangedWordIndices = emptyList() },
                                        modifier = Modifier.testTag("quiz_reorder_clear_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Refresh,
                                            contentDescription = "Clear sentence",
                                            tint = GoldAccent,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.size(4.dp))
                                        Text(
                                            text = "Reset",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = GoldAccent
                                        )
                                    }
                                }
                            }

                            // Constructed sentence box
                            val boxBorderColor = when {
                                isSubmitted && isReorderAnswerCorrect -> EmeraldAccent
                                isSubmitted && !isReorderAnswerCorrect -> CoralAccent
                                arrangedWordIndices.isNotEmpty() -> GoldAccentBorder
                                else -> BorderDark
                            }
                            val boxBgColor = when {
                                isSubmitted && isReorderAnswerCorrect -> EmeraldAccent.copy(alpha = 0.14f)
                                isSubmitted && !isReorderAnswerCorrect -> CoralAccent.copy(alpha = 0.14f)
                                else -> SurfaceElevatedDark
                            }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 68.dp)
                                    .testTag("quiz_reorder_sentence_box"),
                                shape = RoundedCornerShape(12.dp),
                                color = boxBgColor,
                                border = BorderStroke(1.dp, boxBorderColor)
                            ) {
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    if (arrangedWordIndices.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            Text(
                                                text = "Tap words below to arrange them here...",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextMuted
                                            )
                                        }
                                    } else {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                arrangedWordIndices.forEachIndexed { posIdx, optIdx ->
                                                    val word = currentQuestion.options.getOrElse(optIdx) { "" }
                                                    Surface(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .clickable(enabled = !isSubmitted) {
                                                                arrangedWordIndices = arrangedWordIndices.toMutableList().also {
                                                                    it.removeAt(posIdx)
                                                                }
                                                            }
                                                            .testTag("quiz_placed_word_$posIdx"),
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = GoldAccentDim,
                                                        border = BorderStroke(1.dp, GoldAccent)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            Text(
                                                                text = word,
                                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                                    fontWeight = FontWeight.Bold,
                                                                    textDirection = TextDirection.Ltr
                                                                ),
                                                                color = TextPrimary
                                                            )
                                                            if (!isSubmitted) {
                                                                Icon(
                                                                    imageVector = Icons.Filled.Close,
                                                                    contentDescription = "Remove $word",
                                                                    tint = GoldAccent,
                                                                    modifier = Modifier.size(13.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            if (isSubmitted) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (isReorderAnswerCorrect) Icons.Filled.Check else Icons.Filled.Close,
                                                        contentDescription = null,
                                                        tint = if (isReorderAnswerCorrect) EmeraldAccent else CoralAccent,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Text(
                                                        text = if (isReorderAnswerCorrect) {
                                                            "Correct! $arrangedSentence"
                                                        } else {
                                                            "Correct order: ${currentQuestion.correctText}"
                                                        },
                                                        style = MaterialTheme.typography.labelMedium.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            textDirection = TextDirection.Ltr,
                                                            textAlign = TextAlign.Left
                                                        ),
                                                        color = if (isReorderAnswerCorrect) EmeraldAccent else CoralAccent
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Word Bank Chips (LTR FlowRow)
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    currentQuestion.options.forEachIndexed { optIdx, word ->
                                        val isUsed = optIdx in arrangedWordIndices
                                        Surface(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable(enabled = !isSubmitted) {
                                                    arrangedWordIndices = if (isUsed) {
                                                        arrangedWordIndices - optIdx
                                                    } else {
                                                        arrangedWordIndices + optIdx
                                                    }
                                                }
                                                .testTag("quiz_word_chip_$optIdx"),
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isUsed) BackgroundDark.copy(alpha = 0.5f) else SurfaceElevatedDark,
                                            border = BorderStroke(
                                                1.dp,
                                                if (isUsed) BorderDark.copy(alpha = 0.4f) else GoldAccentBorder
                                            )
                                        ) {
                                            Text(
                                                text = word,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (isUsed) FontWeight.Normal else FontWeight.SemiBold,
                                                    textDirection = TextDirection.Ltr
                                                ),
                                                color = if (isUsed) TextMuted.copy(alpha = 0.45f) else TextPrimary,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Standard Multiple-Choice Options
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            currentQuestion.options.forEachIndexed { optIdx, optText ->
                                val isSelected = selectedOptionIndex == optIdx
                                val isCorrectAnswer = optIdx == currentQuestion.answerIndex
                                val optionColor = when {
                                    isSubmitted && isCorrectAnswer -> EmeraldAccent.copy(alpha = 0.2f)
                                    isSubmitted && isSelected && !isCorrectAnswer -> CoralAccent.copy(alpha = 0.2f)
                                    isSelected -> GoldAccentDim
                                    else -> SurfaceElevatedDark
                                }
                                val borderColor = when {
                                    isSubmitted && isCorrectAnswer -> EmeraldAccent
                                    isSubmitted && isSelected && !isCorrectAnswer -> CoralAccent
                                    isSelected -> GoldAccent
                                    else -> BorderDark
                                }

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = !isSubmitted) {
                                            selectedOptionIndex = optIdx
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    color = optionColor,
                                    border = BorderStroke(1.dp, borderColor)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = optText,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                            color = TextPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (isSubmitted && isCorrectAnswer) {
                                            Icon(Icons.Filled.Check, contentDescription = "Correct", tint = EmeraldAccent, modifier = Modifier.size(18.dp))
                                        } else if (isSubmitted && isSelected && !isCorrectAnswer) {
                                            Icon(Icons.Filled.Close, contentDescription = "Wrong", tint = CoralAccent, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Explanation when submitted
                    AnimatedVisibility(visible = isSubmitted && currentQuestion.explanationAr.isNotBlank()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = SurfaceElevatedDark,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            BidiText(
                                text = "الشرح: ${currentQuestion.explanationAr}",
                                style = ArabicTypography.labelSmall,
                                color = TextSecondary,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            } else {
                // Final Results & Missed Questions Summary View
                val total = questions.size
                val correct = questions.count { q -> userAnswers[q.id] == q.answerIndex }
                val missedQuestions = questions.filter { q -> userAnswers[q.id] != q.answerIndex }
                val scorePercent = ((correct.toFloat() / total.toFloat()) * 100).toInt()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$scorePercent%",
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (scorePercent >= 70) EmeraldAccent else GoldAccent
                    )
                    Text(
                        text = "Score: $correct of $total correct",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = if (scorePercent >= 80) "ممتاز! أداء رائع واستيعاب متقدم للمهارات." else "عمل جيد! استمر في المراجعة لتقوية الجوانب الضعيفة.",
                        style = ArabicTypography.bodySmall,
                        color = TextSecondary
                    )

                    // Skill breakdown
                    val skills = listOf("vocabulary", "grammar", "reading", "listening")
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        skills.forEach { skill ->
                            val sQuestions = questions.filter { it.skill == skill }
                            if (sQuestions.isNotEmpty()) {
                                val sCorrect = sQuestions.count { userAnswers[it.id] == it.answerIndex }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(skill.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                                    Text("$sCorrect / ${sQuestions.size}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = GoldAccent)
                                }
                            }
                        }
                    }

                    // Missed Questions Section with One-Tap Dictionary Access
                    if (missedQuestions.isNotEmpty()) {
                        HorizontalDivider(color = BorderDark, modifier = Modifier.padding(vertical = 4.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Missed Questions (${missedQuestions.size})",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = CoralAccent
                            )
                            Text(
                                text = "Tap 'Review Word' to open dictionary definitions and phonetic details for missed terms.",
                                style = ArabicTypography.labelSmall,
                                color = TextMuted
                            )

                            missedQuestions.forEachIndexed { index, missed ->
                                val targetWord = remember(missed) {
                                    val wordCandidate = missed.correctText.trim().takeIf { it.isNotBlank() && !it.contains(" ") }
                                        ?: missed.audioText.trim().takeIf { it.isNotBlank() && !it.contains(" ") }
                                        ?: missed.promptEn.split(" ").firstOrNull { w -> w.length > 2 && w.all { c -> c.isLetter() } }
                                        ?: missed.correctText
                                    wordCandidate.lowercase().replace(Regex("[^a-z\\s-]"), "").trim()
                                }

                                val userAnswerIdx = userAnswers[missed.id] ?: -1
                                val userAnswerText = missed.options.getOrNull(userAnswerIdx) ?: "No answer selected"

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("quiz_missed_card_$index"),
                                    shape = RoundedCornerShape(12.dp),
                                    color = SurfaceElevatedDark,
                                    border = BorderStroke(1.dp, CoralAccent.copy(alpha = 0.4f))
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
                                                text = "Question ${questions.indexOf(missed) + 1} • ${missed.skill.uppercase()}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = CoralAccent
                                            )
                                        }

                                        BidiText(
                                            text = missed.promptEn,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = TextPrimary
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Filled.Close, contentDescription = null, tint = CoralAccent, modifier = Modifier.size(14.dp))
                                            Text(
                                                text = "Your Answer: $userAnswerText",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = CoralAccent
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Filled.Check, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(14.dp))
                                            Text(
                                                text = "Correct: ${missed.correctText}",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = EmeraldAccent
                                            )
                                        }

                                        if (missed.explanationAr.isNotBlank()) {
                                            BidiText(
                                                text = "الشرح: ${missed.explanationAr}",
                                                style = ArabicTypography.labelSmall,
                                                color = TextSecondary
                                            )
                                        }

                                        if (targetWord.isNotBlank()) {
                                            OutlinedButton(
                                                onClick = {
                                                    onOpenDictionaryWord?.invoke(targetWord)
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 4.dp)
                                                    .testTag("quiz_review_word_btn_$index"),
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(1.dp, GoldAccentBorder),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldAccent)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                                    contentDescription = "Review Word",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Review \"$targetWord\" in Dictionary 📖",
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
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
        },
        confirmButton = {
            if (!isFinished) {
                val canSubmit = if (isReorderQuestion) arrangedWordIndices.isNotEmpty() else selectedOptionIndex != null
                if (!isSubmitted) {
                    Button(
                        onClick = {
                            if (isReorderQuestion) {
                                if (arrangedWordIndices.isNotEmpty()) {
                                    userAnswers[currentQuestion.id] = if (isReorderAnswerCorrect) currentQuestion.answerIndex else -1
                                    isSubmitted = true
                                }
                            } else if (selectedOptionIndex != null) {
                                userAnswers[currentQuestion.id] = selectedOptionIndex!!
                                isSubmitted = true
                            }
                        },
                        enabled = canSubmit,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = BackgroundDark),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Check Answer", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            if (currentIndex < questions.size - 1) {
                                currentIndex++
                                selectedOptionIndex = null
                                arrangedWordIndices = emptyList()
                                isSubmitted = false
                            } else {
                                isFinished = true
                                onQuizCompleted(userAnswers)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = BackgroundDark),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (currentIndex < questions.size - 1) "Next" else "Finish & Save", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = BackgroundDark),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (!isFinished) {
                TextButton(onClick = onDismiss) {
                    Text("Quit Quiz", color = TextMuted)
                }
            }
        }
    )
}
