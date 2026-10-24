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
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ArabicTypography
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderDark
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldAccentBorder
import com.example.ui.theme.GoldAccentDim
import com.example.ui.theme.OnGoldDark
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
            "medical" -> "Medical & Healthcare"
            else -> "General Fluency"
        }
    }

    val goalTitleAr = remember(selectedGoal) {
        when (selectedGoal.trim().lowercase()) {
            "ielts" -> "إعداد الآيلتس الأكاديمي"
            "it_career" -> "المصطلحات التقنية والبرمجية"
            "medical" -> "الطب والرعاية الصحية"
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Back button to Step 4
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                TextButton(
                    onClick = onStep5Back,
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .testTag("btn_onboarding_step5_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to deck selection",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Decks",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = TextSecondary,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }

            OnboardingHeroHeader(
                titleEn = "AI Features & Plan Launch",
                titleAr = "ميزات الذكاء الاصطناعي وإطلاق الخطة",
                titleEnTestTag = "onboarding_step5_header_en",
                titleArTestTag = "onboarding_step5_header_ar"
            )

            // Card 1: AI Features Toggle (Default OFF for privacy & offline capability)
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (allowAi) GoldAccent else GoldAccentDim)
                                    .border(
                                        width = 1.dp,
                                        color = if (allowAi) GoldAccent else GoldAccentBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = "AI Assistant",
                                    tint = if (allowAi) OnGoldDark else GoldAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Enable Gemini AI Features",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (allowAi) GoldAccent else TextPrimary
                                )
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                    Text(
                                        text = "تفعيل ميزات الذكاء الاصطناعي",
                                        style = ArabicTypography.labelMedium.copy(
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
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
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = TextPrimary
                        )
                        Text(
                            text = "• Deep Arabic explanations with root nuances & collocations\n• AI sentence writing feedback and grammar corrections\n• Interactive reading tutor for reading comprehension texts",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp
                            ),
                            color = TextSecondary
                        )
                    }

                    // 100% Offline & Privacy Guarantee Note
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_step5_offline_note"),
                        shape = RoundedCornerShape(12.dp),
                        color = EmeraldAccent.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.25f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Security,
                                    contentDescription = "Privacy note",
                                    tint = EmeraldAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "100% Offline by default. Works without AI.",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = EmeraldAccent
                                )
                            }
                            Text(
                                text = "All 21,624 bundled dictionary words, grammar lessons, and audio are stored locally. You can toggle AI on or off anytime in Settings.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp
                                ),
                                color = Color(0xFFD1D5DB)
                            )
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Text(
                                    text = "يعمل التطبيق بالكامل بدون إنترنت مع ٢١,٦٢٤ كلمة وقواعد جاهزة محلياً. يمكنك تفعيل أو إيقاف الذكاء الاصطناعي لاحقاً من الإعدادات.",
                                    style = ArabicTypography.labelSmall.copy(
                                        fontSize = 11.5.sp,
                                        lineHeight = 17.sp
                                    ),
                                    color = TextSecondary,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Right
                                )
                            }
                        }
                    }
                }
            }

            // Card 2: Completed Plan Summary Card
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Your Personalized Study Plan",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold
                                ),
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
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = GoldAccent,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = BorderDark)

                    // Clean 5-Row Bilingual Summary Table
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PlanSummaryRow(
                            labelEn = "Goal",
                            labelAr = "الهدف",
                            valueEn = goalTitleEn,
                            valueAr = goalTitleAr
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.04f))
                        PlanSummaryRow(
                            labelEn = "Daily Commitment",
                            labelAr = "الوقت اليومي",
                            valueEn = "$selectedMinutesPerDay min/day • $newWordsPace words/day"
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.04f))
                        PlanSummaryRow(
                            labelEn = "Weekly Schedule",
                            labelAr = "الجدول الأسبوعي",
                            valueEn = "$selectedStudyDaysPerWeek days/week",
                            valueAr = "Friday rest day • الجمعة راحة"
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.04f))
                        PlanSummaryRow(
                            labelEn = "Active Decks",
                            labelAr = "القوائم المختارة",
                            valueEn = "$selectedDecksCount focus decks"
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.04f))
                        PlanSummaryRow(
                            labelEn = "Offline Dictionary",
                            labelAr = "المعجم المدمج",
                            valueEn = "21,624 entries ready offline",
                            valueColor = EmeraldAccent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }

        // Sticky Primary Launch CTA Footer
        OnboardingStickyFooter(
            ctaTextEn = "Start Learning",
            ctaTextAr = "ابدأ رحلة التعلم",
            buttonTestTag = "btn_onboarding_step5_start",
            leadingIcon = Icons.Filled.RocketLaunch,
            onClick = { onCompleteStep5(allowAi) }
        )
    }
}

@Composable
private fun PlanSummaryRow(
    labelEn: String,
    labelAr: String,
    valueEn: String,
    valueAr: String? = null,
    valueColor: Color = TextPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = labelEn,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = TextPrimary
            )
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = labelAr,
                    style = ArabicTypography.labelSmall.copy(fontSize = 11.5.sp),
                    color = TextSecondary
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(
                    text = valueEn,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = valueColor,
                    textAlign = TextAlign.End
                )
            }
            if (valueAr != null) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = valueAr,
                        style = ArabicTypography.labelSmall.copy(fontSize = 11.5.sp),
                        color = TextSecondary,
                        textAlign = TextAlign.Right
                    )
                }
            }
        }
    }
}
