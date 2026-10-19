package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.QuizQuestionItem
import com.example.ui.components.BidiText
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

@Composable
fun QuizDialog(
    title: String,
    quizType: String,
    questions: List<QuizQuestionItem>,
    onPlayAudio: (String) -> Unit,
    onQuizCompleted: (userAnswers: Map<String, Int>) -> Unit,
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
    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
    var isSubmitted by remember { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }

    val currentQuestion = questions[currentIndex]

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
                        text = if (!isFinished) "Question ${currentIndex + 1} of ${questions.size} • Skill: ${currentQuestion.skill.uppercase()}" else "Quiz Results",
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
                            Text(
                                text = currentQuestion.promptEn,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary
                            )
                            if (currentQuestion.promptAr.isNotBlank()) {
                                BidiText(
                                    text = currentQuestion.promptAr,
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

                    // Options
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
                // Final Results Card
                val total = questions.size
                val correct = questions.count { q -> userAnswers[q.id] == q.answerIndex }
                val scorePercent = ((correct.toFloat() / total.toFloat()) * 100).toInt()

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
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
                }
            }
        },
        confirmButton = {
            if (!isFinished) {
                if (!isSubmitted) {
                    Button(
                        onClick = {
                            if (selectedOptionIndex != null) {
                                userAnswers[currentQuestion.id] = selectedOptionIndex!!
                                isSubmitted = true
                            }
                        },
                        enabled = selectedOptionIndex != null,
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
