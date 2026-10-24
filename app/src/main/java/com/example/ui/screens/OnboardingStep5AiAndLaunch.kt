package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ArabicTypography
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderDark
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldAccentBorder
import com.example.ui.theme.GoldAccentDim
import com.example.ui.theme.OnGoldDark
import com.example.ui.theme.SquircleCardShape
import com.example.ui.theme.SquirclePlanCardShape
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevatedDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun OnboardingAiAndLaunchStep(
    selectedLevel: String,
    selectedGoal: String,
    selectedMinutesPerDay: Int,
    selectedStudyDaysPerWeek: Int,
    selectedDecksCount: Int,
    initialAllowAi: Boolean = false,
    totalEntriesCount: Int,
    onCompleteStep5: (allowAi: Boolean) -> Unit,
    onStep5Back: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onStep5Back)

    var allowAi by remember(initialAllowAi) { mutableStateOf(initialAllowAi) }

    val goalTitleEn = remember(selectedGoal) {
        when (selectedGoal.trim().lowercase()) {
            "ielts" -> "IELTS & Academic"
            "it_career" -> "IT & Tech Career"
            else -> "General Fluency"
        }
    }

    val goalTitleAr = remember(selectedGoal) {
        when (selectedGoal.trim().lowercase()) {
            "ielts" -> "إعداد الآيلتس الأكاديمي"
            "it_career" -> "المصطلحات التقنية والبرمجية"
            else -> "طلاقة عامة وتواصل يومي"
        }
    }

    val newWordsPace = remember(selectedMinutesPerDay) {
        when (selectedMinutesPerDay) {
            15 -> 5
            30 -> 10
            45 -> 15
            else -> 20
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 6.dp)
            .testTag("onboarding_step_5_ai_launch"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Back button to Step 4 + Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                TextButton(
                    onClick = onStep5Back,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("btn_onboarding_step5_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to deck selection",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Decks",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "AI Features & Plan Launch",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("onboarding_step5_header_en")
                )
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = "ميزات الذكاء الاصطناعي وإطلاق الخطة",
                        style = ArabicTypography.titleMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("onboarding_step5_header_ar")
                    )
                }
            }

            // Section 1: AI Features Toggle (Default OFF for privacy & offline capability)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboarding_step5_ai_card"),
                shape = SquirclePlanCardShape,
                color = SurfaceDark,
                border = BorderStroke(1.dp, if (allowAi) GoldAccent else BorderDark)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (allowAi) GoldAccent else SurfaceElevatedDark),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = "AI Assistant",
                                    tint = if (allowAi) OnGoldDark else GoldAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Enable Gemini AI Features",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (allowAi) GoldAccent else TextPrimary
                                )
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                    Text(
                                        text = "تفعيل ميزات الذكاء الاصطناعي",
                                        style = ArabicTypography.labelMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }

                        Switch(
                            checked = allowAi,
                            onCheckedChange = { allowAi = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BackgroundDark,
                                checkedTrackColor = GoldAccent,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SurfaceElevatedDark
                            ),
                            modifier = Modifier.testTag("switch_onboarding_allow_ai")
                        )
                    }

                    HorizontalDivider(color = BorderDark)

                    // What AI provides
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Optional on-demand AI capabilities:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = "• Deep Arabic explanations with root nuances & collocations\n• AI sentence writing feedback and grammar corrections\n• Interactive reading tutor for reading comprehension texts",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    // 100% Offline & Privacy Guarantee Note
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_step5_offline_note"),
                        shape = RoundedCornerShape(10.dp),
                        color = SurfaceElevatedDark,
                        border = BorderStroke(1.dp, BorderDark)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = "Privacy note",
                                tint = EmeraldAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "100% Offline by default. Works without AI.",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = EmeraldAccent
                                )
                                Text(
                                    text = "All 21,624 bundled dictionary words, grammar lessons, and audio are stored locally. You can toggle AI on or off anytime in Settings.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                    Text(
                                        text = "يعمل التطبيق بالكامل بدون إنترنت مع ٢١,٦٢٤ كلمة وقواعد جاهزة محلياً. يمكنك تفعيل أو إيقاف الذكاء الاصطناعي لاحقاً من الإعدادات.",
                                        style = ArabicTypography.labelSmall,
                                        color = TextMuted,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: Completed Plan Summary Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboarding_step5_plan_summary"),
                shape = SquirclePlanCardShape,
                color = SurfaceDark,
                border = BorderStroke(1.dp, GoldAccentBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.RocketLaunch,
                                contentDescription = "Ready to launch",
                                tint = GoldAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Your Personalized Study Plan",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = GoldAccent
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = GoldAccentDim,
                            border = BorderStroke(1.dp, GoldAccent)
                        ) {
                            Text(
                                text = selectedLevel,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = GoldAccent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = BorderDark)

                    // Profile Summary Grid
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SummaryRow(
                            label = "Goal / الهدف",
                            value = "$goalTitleEn ($goalTitleAr)"
                        )
                        SummaryRow(
                            label = "Daily Commitment / الوقت اليومي",
                            value = "$selectedMinutesPerDay min/day • $newWordsPace words/day"
                        )
                        SummaryRow(
                            label = "Weekly Schedule / الجدول الأسبوعي",
                            value = "$selectedStudyDaysPerWeek days/week (Friday rest day)"
                        )
                        SummaryRow(
                            label = "Active Decks / القوائم المختارة",
                            value = "$selectedDecksCount focus decks"
                        )
                        SummaryRow(
                            label = "Offline Dictionary / المعجم المدمج",
                            value = "21,624 entries ready offline"
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Primary Launch CTA
        Button(
            onClick = { onCompleteStep5(allowAi) },
            shape = SquircleCardShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = GoldAccent,
                contentColor = OnGoldDark
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag("btn_onboarding_step5_start")
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.RocketLaunch,
                    contentDescription = null,
                    tint = OnGoldDark,
                    modifier = Modifier.size(22.dp)
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Text(
                        text = "Start Learning",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = OnGoldDark
                    )
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(
                            text = "ابدأ رحلة التعلم",
                            style = ArabicTypography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = OnGoldDark.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary
        )
    }
}
