package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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

@Composable
fun PlanSetupDialog(
    initialProfile: StudyPlanProfileEntity?,
    onDismiss: () -> Unit,
    onSaveProfile: (StudyPlanProfileEntity) -> Unit
) {
    var selectedLevel by remember { mutableStateOf(initialProfile?.level ?: "B2") }
    var selectedGoal by remember { mutableStateOf(initialProfile?.goal ?: "general_fluency") }
    var selectedMinutes by remember { mutableIntStateOf(initialProfile?.minutesPerDay ?: 30) }
    var selectedDays by remember { mutableIntStateOf(initialProfile?.studyDaysPerWeek ?: 6) }

    val levels = listOf("A1", "A2", "B1", "B2", "C1")
    val goals = listOf(
        Triple("general_fluency", "General Fluency", "طلاقة عامة وتواصل يومي"),
        Triple("ielts", "IELTS Preparation", "إعداد لاختبار الآيلتس الأكاديمي"),
        Triple("it_career", "IT & Tech Career", "المصطلحات التقنية والمهنية البرمجية")
    )
    val minuteOptions = listOf(15, 30, 45, 60)
    val dayOptions = listOf(5, 6, 7)

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
                    text = "Personalize your daily pace and CEFR target",
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
                                .clickable { selectedGoal = id },
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

                // 3. Minutes Per Day
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Daily Commitment",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = TextPrimary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        minuteOptions.forEach { mins ->
                            FilterChip(
                                selected = selectedMinutes == mins,
                                onClick = { selectedMinutes = mins },
                                label = { Text("$mins min") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldAccentDim,
                                    selectedLabelColor = GoldAccent,
                                    containerColor = SurfaceElevatedDark,
                                    labelColor = TextMuted
                                )
                            )
                        }
                    }
                }

                // 4. Study Days Per Week
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Study Days Per Week",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = TextPrimary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        dayOptions.forEach { days ->
                            FilterChip(
                                selected = selectedDays == days,
                                onClick = { selectedDays = days },
                                label = { Text("$days days") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldAccentDim,
                                    selectedLabelColor = GoldAccent,
                                    containerColor = SurfaceElevatedDark,
                                    labelColor = TextMuted
                                )
                            )
                        }
                    }
                    Text(
                        text = when (selectedDays) {
                            6 -> "Rest day: Friday"
                            5 -> "Rest days: Friday & Saturday"
                            else -> "Everyday learning schedule"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val defaultRest = when (selectedDays) {
                        5 -> listOf(Calendar.FRIDAY, Calendar.SATURDAY)
                        6 -> listOf(Calendar.FRIDAY)
                        else -> emptyList()
                    }
                    val updated = (initialProfile ?: StudyPlanProfileEntity()).copy(
                        level = selectedLevel,
                        goal = selectedGoal,
                        minutesPerDay = selectedMinutes,
                        studyDaysPerWeek = selectedDays,
                        restDaysJson = Json.encodeToString(defaultRest)
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
