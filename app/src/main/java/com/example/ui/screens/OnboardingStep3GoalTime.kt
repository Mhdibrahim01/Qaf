package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.plan.AdaptiveConfig
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

@Immutable
data class OnboardingGoalOption(
    val id: String,
    val titleEn: String,
    val titleAr: String,
    val subtitleEn: String,
    val subtitleAr: String,
    val defaultDecksLabelEn: String,
    val icon: ImageVector
)

@Immutable
data class OnboardingMinutesOption(
    val minutes: Int,
    val newWordsPerDay: Int,
    val flashcardsPerDay: Int,
    val wordsAr: String,
    val labelEn: String,
    val labelAr: String,
    val isRecommended: Boolean = false
)

object OnboardingStep3Defaults {
    val GOAL_OPTIONS = listOf(
        OnboardingGoalOption(
            id = "general_fluency",
            titleEn = "General Fluency",
            titleAr = "طلاقة عامة وتواصل يومي",
            subtitleEn = "Everyday conversation, Oxford 5000 & natural expressions",
            subtitleAr = "محادثة يومية ومفردات أكسفورد والتعبيرات الطبيعية",
            defaultDecksLabelEn = "Oxford 5000 + Academic OPAL",
            icon = Icons.Filled.Public
        ),
        OnboardingGoalOption(
            id = "ielts",
            titleEn = "IELTS & Academic",
            titleAr = "إعداد لاختبار الآيلتس الأكاديمي",
            subtitleEn = "Academic OPAL, IELTS Core, reading & formal writing",
            subtitleAr = "المفردات الأكاديمية \u2066(OPAL)\u2069 ومهارات القراءة والكتابة للآيلتس",
            defaultDecksLabelEn = "Oxford 5000 + OPAL + IELTS Core",
            icon = Icons.Filled.School
        ),
        OnboardingGoalOption(
            id = "it_career",
            titleEn = "IT & Tech Career",
            titleAr = "المصطلحات التقنية والبرمجية",
            subtitleEn = "Software engineering, standups, RFCs & tech interviews",
            subtitleAr = "هندسة البرمجيات واجتماعات الفرق التقنية والمقابلات المهنية",
            defaultDecksLabelEn = "Oxford 5000 + OPAL + IT & Tech Industry",
            icon = Icons.Filled.Code
        ),
        OnboardingGoalOption(
            id = "medical",
            titleEn = "Medical & Healthcare",
            titleAr = "الطب والرعاية الصحية",
            subtitleEn = "Medical terminology, clinical communication & healthcare",
            subtitleAr = "مصطلحات الطب والمرضى والتقارير الطبية والصحية",
            defaultDecksLabelEn = "Oxford 5000 + OPAL + Medical & Healthcare",
            icon = Icons.Filled.LocalHospital
        )
    )

    val MINUTES_OPTIONS = listOf(
        OnboardingMinutesOption(
            minutes = 15,
            newWordsPerDay = 5,
            flashcardsPerDay = 5,
            wordsAr = "٥ كلمات",
            labelEn = "15 min/day • 5 new words",
            labelAr = "١٥ دقيقة يومياً • ٥ كلمات جديدة"
        ),
        OnboardingMinutesOption(
            minutes = 30,
            newWordsPerDay = 10,
            flashcardsPerDay = 10,
            wordsAr = "١٠ كلمات",
            labelEn = "30 min/day • 10 new words",
            labelAr = "٣٠ دقيقة يومياً • ١٠ كلمات جديدة",
            isRecommended = true
        ),
        OnboardingMinutesOption(
            minutes = 45,
            newWordsPerDay = 15,
            flashcardsPerDay = 15,
            wordsAr = "١٥ كلمة",
            labelEn = "45 min/day • 15 new words",
            labelAr = "٤٥ دقيقة يومياً • ١٥ كلمة جديدة"
        ),
        OnboardingMinutesOption(
            minutes = 60,
            newWordsPerDay = 20,
            flashcardsPerDay = 20,
            wordsAr = "٢٠ كلمة",
            labelEn = "60 min/day • 20 new words",
            labelAr = "٦٠ دقيقة يومياً • ٢٠ كلمة جديدة"
        )
    )

    val STUDY_DAYS_OPTIONS = listOf(3, 4, 5, 6, 7)

    fun restDayDescriptionEn(studyDaysPerWeek: Int): String {
        return when (studyDaysPerWeek) {
            7 -> "7 study days/week • No rest day"
            6 -> "6 study days/week • Friday rest day (keeps your streak)"
            5 -> "5 study days/week • Fri & Sat rest days (keeps your streak)"
            4 -> "4 study days/week • 3 rest days/week (keeps your streak)"
            else -> "3 study days/week • 4 rest days/week (keeps your streak)"
        }
    }

    fun restDayDescriptionAr(studyDaysPerWeek: Int): String {
        return when (studyDaysPerWeek) {
            7 -> "٧ أيام دراسة أسبوعياً • بدون يوم راحة"
            6 -> "٦ أيام دراسة أسبوعياً • الجمعة يوم راحة \u2067(يحافظ على الاستمرارية)\u2069"
            5 -> "٥ أيام دراسة أسبوعياً • الجمعة والسبت راحة \u2067(يحافظ على الاستمرارية)\u2069"
            4 -> "٤ أيام دراسة أسبوعياً • ٣ أيام راحة \u2067(يحافظ على الاستمرارية)\u2069"
            else -> "٣ أيام دراسة أسبوعياً • ٤ أيام راحة \u2067(يحافظ على الاستمرارية)\u2069"
        }
    }
}

@Composable
fun OnboardingGoalAndTimeStep(
    selectedLevel: String,
    selectedGoal: String,
    selectedMinutesPerDay: Int,
    selectedStudyDaysPerWeek: Int,
    initialPreferredTopics: List<String> = listOf("daily_life", "technology", "career"),
    initialQuietModeEnabled: Boolean = false,
    onSelectGoal: (String) -> Unit,
    onSelectMinutesPerDay: (Int) -> Unit,
    onSelectStudyDaysPerWeek: (Int) -> Unit,
    onContinueStep3: (goal: String, minutesPerDay: Int, studyDaysPerWeek: Int) -> Unit,
    onContinueStep3WithTopicsAndQuietMode: ((goal: String, minutesPerDay: Int, studyDaysPerWeek: Int, preferredTopics: List<String>, quietModeEnabled: Boolean) -> Unit)? = null,
    onStep3Back: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onStep3Back)

    val activeGoalOption = OnboardingStep3Defaults.GOAL_OPTIONS.firstOrNull {
        it.id.equals(selectedGoal, ignoreCase = true)
    } ?: OnboardingStep3Defaults.GOAL_OPTIONS.first()

    val activeMinutesOption = OnboardingStep3Defaults.MINUTES_OPTIONS.firstOrNull {
        it.minutes == selectedMinutesPerDay
    } ?: OnboardingStep3Defaults.MINUTES_OPTIONS[1] // 30 min default

    val activeStudyDays = selectedStudyDaysPerWeek.coerceIn(3, 7)

    var selectedTopics by remember(initialPreferredTopics) {
        mutableStateOf(
            initialPreferredTopics.ifEmpty { listOf("daily_life", "technology", "career") }.take(3)
        )
    }
    var quietModeEnabled by remember(initialQuietModeEnabled) {
        mutableStateOf(initialQuietModeEnabled)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("onboarding_step_3_goal_time"),
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
            // Back to Step 2 button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                TextButton(
                    onClick = onStep3Back,
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .testTag("btn_onboarding_step3_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to level selection",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Level ($selectedLevel)",
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
                titleEn = "Set your goal & daily pace",
                titleAr = "حدد هدفك ووقت الدراسة اليومي",
                titleEnTestTag = "onboarding_step3_header_en",
                titleArTestTag = "onboarding_step3_header_ar"
            )

            // Section 1: Learning Goal
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OnboardingSectionHeader(
                    number = "1",
                    titleEn = "Learning Goal",
                    titleAr = "هدف التعلّم"
                )

                OnboardingStep3Defaults.GOAL_OPTIONS.forEach { option ->
                    val isSelected = activeGoalOption.id == option.id
                    GoalOptionCard(
                        option = option,
                        isSelected = isSelected,
                        onClick = { onSelectGoal(option.id) }
                    )
                }
            }

            // Section 2: Daily Study Time (2x2 Grid)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OnboardingSectionHeader(
                    number = "2",
                    titleEn = "Daily Study Time",
                    titleAr = "الوقت اليومي للدراسة"
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OnboardingStep3Defaults.MINUTES_OPTIONS.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { minOpt ->
                                val isSelected = activeMinutesOption.minutes == minOpt.minutes
                                MinutesOptionCard(
                                    option = minOpt,
                                    isSelected = isSelected,
                                    onClick = { onSelectMinutesPerDay(minOpt.minutes) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                )
                            }
                        }
                    }
                }
            }

            // Section 3: Study Days / Week (5-Column Single Row)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OnboardingSectionHeader(
                    number = "3",
                    titleEn = "Study Days / Week",
                    titleAr = "أيام الدراسة"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OnboardingStep3Defaults.STUDY_DAYS_OPTIONS.forEach { days ->
                        val isSelected = activeStudyDays == days
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .heightIn(min = 54.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(
                                    role = Role.RadioButton,
                                    onClick = { onSelectStudyDaysPerWeek(days) }
                                )
                                .testTag("onboarding_study_days_chip_$days"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) GoldAccentDim else SurfaceDark,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) GoldAccent else BorderDark
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "$days",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = if (isSelected) GoldAccent else TextPrimary,
                                    maxLines = 1
                                )
                                Text(
                                    text = "days/wk",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = if (isSelected) GoldAccent.copy(alpha = 0.85f) else TextSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Rest day explanation banner
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_step3_rest_day_note"),
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceElevatedDark,
                    border = BorderStroke(1.dp, BorderDark)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = OnboardingStep3Defaults.restDayDescriptionEn(activeStudyDays),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = TextPrimary
                        )
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Text(
                                text = OnboardingStep3Defaults.restDayDescriptionAr(activeStudyDays),
                                style = ArabicTypography.labelSmall.copy(fontSize = 12.sp),
                                color = TextSecondary,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Right
                            )
                        }
                    }
                }
            }

            // Section 4: Reading Topics (2-Column Equal-Width Grid)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OnboardingSectionHeader(
                    number = "4",
                    titleEn = "Reading Topics",
                    titleAr = "مواضيع القراءة",
                    rightBadgeText = "Pick up to 3"
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdaptiveConfig.MACRO_TOPICS.chunked(2).forEach { rowTopics ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowTopics.forEach { (topicId, labelEn, labelAr) ->
                                val isTopicSelected = topicId in selectedTopics
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .heightIn(min = 54.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable(
                                            role = Role.Checkbox,
                                            onClick = {
                                                selectedTopics = if (isTopicSelected) {
                                                    if (selectedTopics.size > 1) selectedTopics - topicId else selectedTopics
                                                } else {
                                                    if (selectedTopics.size < 3) {
                                                        selectedTopics + topicId
                                                    } else {
                                                        selectedTopics.drop(1) + topicId
                                                    }
                                                }
                                            }
                                        )
                                        .testTag("onboarding_topic_chip_$topicId"),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isTopicSelected) GoldAccentDim else SurfaceDark,
                                    border = BorderStroke(
                                        width = if (isTopicSelected) 1.5.dp else 1.dp,
                                        color = if (isTopicSelected) GoldAccent else BorderDark
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalAlignment = Alignment.Start,
                                        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
                                    ) {
                                        Text(
                                            text = labelEn,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = if (isTopicSelected) GoldAccent else TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                            Text(
                                                text = labelAr,
                                                style = ArabicTypography.labelSmall.copy(fontSize = 12.sp),
                                                color = if (isTopicSelected) GoldAccent.copy(alpha = 0.9f) else TextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                            if (rowTopics.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Section 5: Modality Preference (Quiet / No-Mic Mode)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboarding_quiet_mode_card"),
                shape = SquircleCardShape,
                color = SurfaceDark,
                border = BorderStroke(1.dp, if (quietModeEnabled) GoldAccent else BorderDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "Quiet / Text-Only Mode (No Audio Required)",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (quietModeEnabled) GoldAccent else TextPrimary
                        )
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Text(
                                text = "الوضع الهادئ \u2067(بدون صوت أو ميكروفون — مثالي للمواصلات والأماكن العامة)\u2069",
                                style = ArabicTypography.labelSmall.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = TextSecondary,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Right
                            )
                        }
                        Text(
                            text = "Replaces listening & speaking tasks with silent reading and sentence exercises.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = TextSecondary
                        )
                    }

                    Switch(
                        checked = quietModeEnabled,
                        onCheckedChange = { quietModeEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BackgroundDark,
                            checkedTrackColor = GoldAccent
                        ),
                        modifier = Modifier.testTag("toggle_quiet_mode")
                    )
                }
            }

            // Live Daily Plan Preview Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboarding_step3_plan_preview"),
                shape = SquirclePlanCardShape,
                color = SurfaceDark,
                border = BorderStroke(1.dp, GoldAccentBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = "Your daily plan summary",
                                tint = GoldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Your Daily 5-Pillar Plan ($selectedLevel)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = GoldAccent
                            )
                        }
                        Text(
                            text = "${activeMinutesOption.minutes} min/day",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldAccent
                        )
                    }

                    Text(
                        text = "• ${activeMinutesOption.newWordsPerDay} new words + ${activeMinutesOption.flashcardsPerDay} review cards/day\n• 1 Grammar lesson + 1 Reading text + 1 Daily Quiz\n• Focus decks: ${activeGoalOption.defaultDecksLabelEn}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                        color = TextPrimary
                    )

                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(
                            text = "خطة متكاملة من ٥ أركان: ${activeMinutesOption.newWordsPerDay} كلمات جديدة، مراجعة البطاقات، درس قواعد، نص قراءة، واختبار يومي.",
                            style = ArabicTypography.labelSmall.copy(fontSize = 12.sp),
                            color = TextSecondary,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Right
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }

        // Sticky Primary CTA Footer
        OnboardingStickyFooter(
            ctaTextEn = "Continue (${activeMinutesOption.minutes} min/day)",
            ctaTextAr = "حفظ الخطة والمتابعة",
            buttonTestTag = "btn_onboarding_step3_continue",
            onClick = {
                if (onContinueStep3WithTopicsAndQuietMode != null) {
                    onContinueStep3WithTopicsAndQuietMode(
                        activeGoalOption.id,
                        activeMinutesOption.minutes,
                        activeStudyDays,
                        selectedTopics,
                        quietModeEnabled
                    )
                } else {
                    onContinueStep3(
                        activeGoalOption.id,
                        activeMinutesOption.minutes,
                        activeStudyDays
                    )
                }
            }
        )
    }
}

@Composable
private fun GoalOptionCard(
    option: OnboardingGoalOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .clip(SquircleCardShape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .testTag("onboarding_goal_card_${option.id}"),
        shape = SquircleCardShape,
        color = if (isSelected) GoldAccentDim else SurfaceDark,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) GoldAccent else BorderDark
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) GoldAccent else SurfaceElevatedDark)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) GoldAccent else BorderDark,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = option.icon,
                    contentDescription = option.titleEn,
                    tint = if (isSelected) OnGoldDark else GoldAccent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = option.titleEn,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (isSelected) GoldAccent else TextPrimary
                )
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = option.titleAr,
                        style = ArabicTypography.labelMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = TextSecondary
                    )
                }
                Text(
                    text = option.subtitleEn,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp
                    ),
                    color = TextSecondary
                )
            }

            OnboardingRadioCircle(isSelected = isSelected)
        }
    }
}

@Composable
private fun MinutesOptionCard(
    option: OnboardingMinutesOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .heightIn(min = 68.dp)
            .clip(SquircleCardShape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .testTag("onboarding_minutes_chip_${option.minutes}"),
        shape = SquircleCardShape,
        color = if (isSelected) GoldAccentDim else SurfaceDark,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) GoldAccent else BorderDark
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 13.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${option.minutes} min/day",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = if (isSelected) GoldAccent else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (option.isRecommended) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = GoldAccent.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = "Default",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = GoldAccent,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${option.newWordsPerDay} new words/day",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                        color = TextMuted,
                        maxLines = 1
                    )
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(
                            text = option.wordsAr,
                            style = ArabicTypography.labelSmall.copy(fontSize = 11.5.sp),
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
