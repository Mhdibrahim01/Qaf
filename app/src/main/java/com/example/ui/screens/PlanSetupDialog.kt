package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.StudyPlanProfileEntity
import com.example.ui.theme.ArabicTypography
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderDark
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldAccentDim
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevatedDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.serialization.json.Json
import java.util.Calendar

private val setupJson = Json { ignoreUnknownKeys = true; isLenient = true }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlanSetupDialog(
    initialProfile: StudyPlanProfileEntity?,
    onDismiss: () -> Unit,
    onSaveProfile: (StudyPlanProfileEntity) -> Unit
) {
    var selectedLevel by remember { mutableStateOf(initialProfile?.level ?: "B2") }
    var selectedGoal by remember { mutableStateOf(initialProfile?.goal ?: "general_fluency") }
    var selectedMinutes by remember { mutableIntStateOf(initialProfile?.minutesPerDay ?: 30) }

    val initialDecks = remember(initialProfile?.selectedDecksJson) {
        try {
            setupJson.decodeFromString<List<String>>(initialProfile?.selectedDecksJson ?: "[\"oxford5000\",\"oxford_opal\"]")
                .ifEmpty { listOf("oxford5000", "oxford_opal") }
        } catch (e: Exception) {
            listOf("oxford5000", "oxford_opal")
        }
    }
    var selectedDecks by remember { mutableStateOf(initialDecks) }

    val initialRestDays = remember(initialProfile?.restDaysJson) {
        try {
            setupJson.decodeFromString<List<Int>>(initialProfile?.restDaysJson ?: "[6]")
        } catch (e: Exception) {
            listOf(Calendar.FRIDAY)
        }
    }
    var selectedRestDays by remember { mutableStateOf(initialRestDays) }

    val levels = listOf("A1", "A2", "B1", "B2", "C1")
    val goals = listOf(
        Triple("general_fluency", "General Fluency", "طلاقة عامة وتواصل يومي"),
        Triple("ielts", "IELTS Preparation", "إعداد لاختبار الآيلتس الأكاديمي"),
        Triple("it_career", "IT & Tech Career", "المصطلحات التقنية والمهنية البرمجية")
    )
    val availableDecks = listOf(
        "oxford5000" to "Oxford 5000",
        "oxford_opal" to "Academic OPAL",
        "oxford_phrases" to "Oxford Phrases",
        "everyday_expressions" to "Everyday Expressions",
        "essential_idioms" to "Essential Idioms",
        "ielts_core" to "IELTS Core",
        "it_industry" to "IT & Tech Industry",
        "cefr_c1" to "CEFR C1 Advanced",
        "pte_academic" to "PTE Academic",
        "business_english" to "Business English",
        "career_interview" to "Job Interview & Career",
        "medical_healthcare" to "Medical & Healthcare"
    )
    val minuteOptions = listOf(15, 30, 45, 60)
    val weekDays = listOf(
        Calendar.SATURDAY to "Sat",
        Calendar.SUNDAY to "Sun",
        Calendar.MONDAY to "Mon",
        Calendar.TUESDAY to "Tue",
        Calendar.WEDNESDAY to "Wed",
        Calendar.THURSDAY to "Thu",
        Calendar.FRIDAY to "Fri"
    )

    val activeStudyDaysCount = (7 - selectedRestDays.size).coerceIn(1, 7)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(22.dp),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Study Plan Setup",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = "Personalize your CEFR target, decks, and weekly schedule",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Target Level
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Target CEFR Level",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = TextPrimary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        levels.forEach { lvl ->
                            FilterChip(
                                selected = selectedLevel == lvl,
                                onClick = { selectedLevel = lvl },
                                label = { Text(lvl, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldAccentDim,
                                    selectedLabelColor = GoldAccent,
                                    containerColor = SurfaceElevatedDark,
                                    labelColor = TextMuted
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selectedLevel == lvl,
                                    borderColor = if (selectedLevel == lvl) GoldAccent else BorderDark
                                )
                            )
                        }
                    }
                }

                // 2. Goal
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Learning Goal",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = TextPrimary
                    )
                    goals.forEach { (id, titleEn, titleAr) ->
                        val isSelected = selectedGoal == id
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedGoal = id
                                    if (id == "ielts" && "oxford_opal" !in selectedDecks) {
                                        selectedDecks = (selectedDecks + "oxford_opal").distinct()
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) GoldAccentDim else SurfaceElevatedDark,
                            border = BorderStroke(1.dp, if (isSelected) GoldAccent else BorderDark)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = titleEn,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) GoldAccent else TextPrimary
                                )
                                Text(
                                    text = titleAr,
                                    style = ArabicTypography.labelSmall,
                                    color = if (isSelected) TextPrimary else TextSecondary
                                )
                            }
                        }
                    }
                }

                // 3. Vocabulary Focus Decks
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Vocabulary Decks",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = TextPrimary
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        availableDecks.forEach { (deckId, label) ->
                            val isSelected = deckId in selectedDecks
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedDecks = if (isSelected) {
                                        if (selectedDecks.size > 1) selectedDecks - deckId else selectedDecks
                                    } else {
                                        selectedDecks + deckId
                                    }
                                },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldAccentDim,
                                    selectedLabelColor = GoldAccent,
                                    containerColor = SurfaceElevatedDark,
                                    labelColor = TextMuted
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) GoldAccent else BorderDark
                                )
                            )
                        }
                    }
                }

                // 4. Daily Commitment & Pacing
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Daily Commitment & Pacing",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = TextPrimary
                    )
                    Text(
                        text = "Choose your daily study time and new words target:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        minuteOptions.forEach { mins ->
                            val wordsPace = when (mins) {
                                15 -> "5 words"
                                30 -> "10 words"
                                45 -> "15 words"
                                else -> "20 words"
                            }
                            FilterChip(
                                selected = selectedMinutes == mins,
                                onClick = { selectedMinutes = mins },
                                label = { Text("$mins min • $wordsPace", style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldAccentDim,
                                    selectedLabelColor = GoldAccent,
                                    containerColor = SurfaceElevatedDark,
                                    labelColor = TextMuted
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selectedMinutes == mins,
                                    borderColor = if (selectedMinutes == mins) GoldAccent else BorderDark
                                )
                            )
                        }
                    }
                }

                // 5. Study Days & Custom Rest Days
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Weekly Schedule ($activeStudyDaysCount study days/wk)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = TextPrimary
                    )
                    Text(
                        text = "Tap any day to mark it as a Rest Day (maintains your streak):",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        weekDays.forEach { (calDay, label) ->
                            val isRest = calDay in selectedRestDays
                            FilterChip(
                                selected = !isRest,
                                onClick = {
                                    selectedRestDays = if (isRest) {
                                        selectedRestDays - calDay
                                    } else {
                                        if (selectedRestDays.size < 3) selectedRestDays + calDay else selectedRestDays
                                    }
                                },
                                label = {
                                    Text(
                                        text = if (isRest) "$label (Rest)" else label,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldAccentDim,
                                    selectedLabelColor = GoldAccent,
                                    containerColor = SurfaceElevatedDark,
                                    labelColor = TextMuted
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = !isRest,
                                    borderColor = if (!isRest) GoldAccent else BorderDark
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = (initialProfile ?: StudyPlanProfileEntity()).copy(
                        level = selectedLevel,
                        goal = selectedGoal,
                        minutesPerDay = selectedMinutes,
                        studyDaysPerWeek = activeStudyDaysCount,
                        selectedDecksJson = setupJson.encodeToString(selectedDecks),
                        restDaysJson = setupJson.encodeToString(selectedRestDays)
                    )
                    onSaveProfile(updated)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldAccent,
                    contentColor = BackgroundDark
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("btn_save_plan_profile")
            ) {
                Text("Save Plan", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}
