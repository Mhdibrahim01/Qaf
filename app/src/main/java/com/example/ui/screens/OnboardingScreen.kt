package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.importer.ImportProgressState
import com.example.data.onboarding.CefrLevelOption
import com.example.data.onboarding.OnboardingPlacementQuiz
import com.example.data.onboarding.OnboardingPreferencesState
import com.example.data.onboarding.PlacementQuestionType
import com.example.data.onboarding.PlacementQuizQuestion
import com.example.ui.theme.ArabicTypography
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderDark
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.CoralAccentBg
import com.example.ui.theme.CoralAccentBorder
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
data class PlacementQuizUiState(
    val isQuizActive: Boolean = false,
    val isWaitingForImport: Boolean = false,
    val questions: List<PlacementQuizQuestion> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val answersByQuestionId: Map<String, String> = emptyMap(),
    val isQuizFinished: Boolean = false,
    val suggestedLevel: String = "B2",
    val editableLevel: String = "B2",
    val correctCountByLevel: Map<String, Int> = emptyMap()
)

/**
 * Shared Hero Header across Onboarding Steps 2–5.
 * Properly padded below the sticky top progress bar so titles never clip.
 */
@Composable
internal fun OnboardingHeroHeader(
    titleEn: String,
    titleAr: String,
    titleEnTestTag: String? = null,
    titleArTestTag: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = titleEn,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            ),
            color = TextPrimary,
            textAlign = TextAlign.Center,
            modifier = if (titleEnTestTag != null) Modifier.testTag(titleEnTestTag) else Modifier
        )
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Text(
                text = titleAr,
                style = ArabicTypography.titleMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = if (titleArTestTag != null) Modifier.testTag(titleArTestTag) else Modifier
            )
        }
    }
}

/**
 * Unified Section Header (Left-Grouped, BiDi Safe):
 * Renders `1. Starting Point • نقطة انطلاق الخطة` on the left and an optional badge on the right.
 */
@Composable
internal fun OnboardingSectionHeader(
    number: String,
    titleEn: String,
    titleAr: String,
    rightBadgeText: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = number,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = GoldAccent
                )
                Text(
                    text = titleEn,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = GoldAccent
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                    color = TextSecondary
                )
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = titleAr,
                        style = ArabicTypography.labelMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = TextSecondary
                    )
                }
            }
        }

        if (rightBadgeText != null) {
            Surface(
                color = SurfaceElevatedDark,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, BorderDark)
            ) {
                Text(
                    text = rightBadgeText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * Consistent 22dp Right Radio Circle indicator.
 * Filled with gold + black checkmark when selected; outlined when unselected.
 */
@Composable
internal fun OnboardingRadioCircle(
    isSelected: Boolean,
    testTag: String? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(if (isSelected) GoldAccent else Color.Transparent)
            .border(
                width = 1.5.dp,
                color = if (isSelected) GoldAccent else BorderDark,
                shape = CircleShape
            )
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Selected",
                tint = OnGoldDark,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * Consistent 22dp Rounded Square Checkbox indicator for multi-select deck cards.
 */
@Composable
internal fun OnboardingCheckboxSquare(
    isChecked: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(top = 2.dp)
            .size(22.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isChecked) GoldAccent else SurfaceElevatedDark)
            .border(
                width = 1.5.dp,
                color = if (isChecked) GoldAccent else BorderDark,
                shape = RoundedCornerShape(6.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isChecked) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Checked",
                tint = OnGoldDark,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * Sticky Bottom CTA Footer with top dark gradient fade.
 */
@Composable
internal fun OnboardingStickyFooter(
    ctaTextEn: String,
    ctaTextAr: String,
    onClick: () -> Unit,
    buttonTestTag: String,
    leadingIcon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        BackgroundDark.copy(alpha = 0.92f),
                        BackgroundDark
                    )
                )
            )
            .padding(top = 12.dp, bottom = 6.dp)
    ) {
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = GoldAccent,
                contentColor = OnGoldDark
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag(buttonTestTag)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (leadingIcon != null) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            tint = OnGoldDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = ctaTextEn,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = OnGoldDark
                    )
                }
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = ctaTextAr,
                        style = ArabicTypography.labelMedium.copy(
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = OnGoldDark.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingScreen(
    onboardingState: OnboardingPreferencesState,
    importState: ImportProgressState,
    totalEntriesCount: Int,
    placementQuizState: PlacementQuizUiState = PlacementQuizUiState(),
    onCompleteStep1: () -> Unit,
    onSelectLevel: (String) -> Unit = {},
    onCompleteStep2: (String) -> Unit = {},
    onStartPlacementQuiz: () -> Unit = {},
    onAnswerPlacementQuestion: (String) -> Unit = {},
    onRetakePlacementQuiz: () -> Unit = {},
    onUpdateQuizEditableLevel: (String) -> Unit = {},
    onPlacementQuizBack: () -> Unit = {},
    onSelectGoal: (String) -> Unit = {},
    onSelectMinutesPerDay: (Int) -> Unit = {},
    onSelectStudyDaysPerWeek: (Int) -> Unit = {},
    onCompleteStep3: (goal: String, minutesPerDay: Int, studyDaysPerWeek: Int) -> Unit = { _, _, _ -> },
    onCompleteStep3WithTopicsAndQuietMode: ((goal: String, minutesPerDay: Int, studyDaysPerWeek: Int, preferredTopics: List<String>, quietModeEnabled: Boolean) -> Unit)? = null,
    onStep3Back: () -> Unit = {},
    onCompleteStep4: (selectedDecks: List<String>, startAtUserLevel: Boolean) -> Unit = { _, _ -> },
    onStep4Back: () -> Unit = {},
    initialAllowAi: Boolean = false,
    onCompleteStep5: (allowAi: Boolean) -> Unit = {},
    onStep5Back: () -> Unit = {},
    onSkipOnboarding: () -> Unit,
    onRetryImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentStep = onboardingState.currentStepToResume

    Surface(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .testTag("onboarding_screen"),
        color = BackgroundDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Sticky Top Bar: Step indicator + single-line "Skip • تخطي" button with bottom clearance
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .background(BackgroundDark)
                    .padding(top = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.testTag("onboarding_step_indicator")
                ) {
                    for (step in 1..5) {
                        val isActive = step == currentStep
                        val isDone = step < currentStep
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(if (isActive) 24.dp else 8.dp)
                                .clip(if (isActive) RoundedCornerShape(4.dp) else CircleShape)
                                .background(
                                    when {
                                        isActive -> GoldAccent
                                        isDone -> GoldAccent.copy(alpha = 0.45f)
                                        else -> BorderDark
                                    }
                                )
                        )
                    }
                    Text(
                        text = "$currentStep / 5",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = TextSecondary,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }

                TextButton(
                    onClick = onSkipOnboarding,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .testTag("btn_onboarding_skip")
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = "Skip",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = TextSecondary
                            )
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                                color = TextSecondary
                            )
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Text(
                                    text = "تخطي",
                                    style = ArabicTypography.labelSmall.copy(fontSize = 13.sp),
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Compact Dictionary Import Progress / Retry Bar (shown across all steps when running or failed)
            OnboardingImportStatusBar(
                importState = importState,
                totalEntriesCount = totalEntriesCount,
                onRetryImport = onRetryImport,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .padding(bottom = 6.dp)
            )

            // Main step content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                when (currentStep) {
                    1 -> OnboardingWelcomeStep(
                        onGetStarted = onCompleteStep1
                    )
                    2 -> OnboardingLevelStep(
                        selectedLevel = onboardingState.selectedLevel.ifBlank { "B2" },
                        importState = importState,
                        placementQuizState = placementQuizState,
                        onSelectLevel = onSelectLevel,
                        onContinueWithLevel = onCompleteStep2,
                        onStartPlacementQuiz = onStartPlacementQuiz,
                        onAnswerPlacementQuestion = onAnswerPlacementQuestion,
                        onRetakePlacementQuiz = onRetakePlacementQuiz,
                        onUpdateQuizEditableLevel = onUpdateQuizEditableLevel,
                        onPlacementQuizBack = onPlacementQuizBack,
                        onRetryImport = onRetryImport
                    )
                    3 -> {
                        val initialTopics = remember(onboardingState.preferredTopicsJson) {
                            try {
                                if (onboardingState.preferredTopicsJson.isNotBlank()) {
                                    kotlinx.serialization.json.Json.decodeFromString<List<String>>(onboardingState.preferredTopicsJson)
                                } else {
                                    listOf("daily_life", "technology", "career")
                                }
                            } catch (_: Exception) {
                                listOf("daily_life", "technology", "career")
                            }
                        }
                        OnboardingGoalAndTimeStep(
                            selectedLevel = onboardingState.selectedLevel.ifBlank { "B2" },
                            selectedGoal = onboardingState.selectedGoal.ifBlank { "general_fluency" },
                            selectedMinutesPerDay = onboardingState.selectedMinutesPerDay,
                            selectedStudyDaysPerWeek = onboardingState.selectedStudyDaysPerWeek,
                            initialPreferredTopics = initialTopics,
                            initialQuietModeEnabled = onboardingState.quietModeEnabled,
                            onSelectGoal = onSelectGoal,
                            onSelectMinutesPerDay = onSelectMinutesPerDay,
                            onSelectStudyDaysPerWeek = onSelectStudyDaysPerWeek,
                            onContinueStep3 = onCompleteStep3,
                            onContinueStep3WithTopicsAndQuietMode = onCompleteStep3WithTopicsAndQuietMode,
                            onStep3Back = onStep3Back
                        )
                    }
                    4 -> {
                        val initialDecks = remember(onboardingState.selectedDecksJson) {
                            try {
                                if (onboardingState.selectedDecksJson.isNotBlank()) {
                                    kotlinx.serialization.json.Json.decodeFromString<List<String>>(onboardingState.selectedDecksJson)
                                } else {
                                    emptyList()
                                }
                            } catch (_: Exception) {
                                emptyList()
                            }
                        }
                        OnboardingDecksAndPlacementStep(
                            selectedLevel = onboardingState.selectedLevel.ifBlank { "B2" },
                            selectedGoal = onboardingState.selectedGoal.ifBlank { "general_fluency" },
                            initialSelectedDecks = initialDecks,
                            initialStartAtUserLevel = onboardingState.startAtUserLevel,
                            onContinueStep4 = onCompleteStep4,
                            onStep4Back = onStep4Back
                        )
                    }
                    else -> {
                        val decksCount = remember(onboardingState.selectedDecksJson) {
                            try {
                                if (onboardingState.selectedDecksJson.isNotBlank()) {
                                    kotlinx.serialization.json.Json.decodeFromString<List<String>>(onboardingState.selectedDecksJson).size
                                } else 2
                            } catch (_: Exception) {
                                2
                            }
                        }
                        OnboardingAiAndLaunchStep(
                            selectedLevel = onboardingState.selectedLevel.ifBlank { "B2" },
                            selectedGoal = onboardingState.selectedGoal.ifBlank { "general_fluency" },
                            selectedMinutesPerDay = onboardingState.selectedMinutesPerDay,
                            selectedStudyDaysPerWeek = onboardingState.selectedStudyDaysPerWeek,
                            selectedDecksCount = decksCount,
                            initialAllowAi = initialAllowAi,
                            totalEntriesCount = totalEntriesCount,
                            onCompleteStep5 = onCompleteStep5,
                            onStep5Back = onStep5Back
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingImportStatusBar(
    importState: ImportProgressState,
    totalEntriesCount: Int,
    onRetryImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasError = !importState.error.isNullOrBlank()
    val expectedRows = importState.totalExpectedRows.coerceAtLeast(1)
    val isRunning = importState.isImporting || (!importState.isCompleted && totalEntriesCount < expectedRows && !hasError)

    if (!isRunning && !hasError) return

    Surface(
        modifier = modifier.testTag("onboarding_import_status_bar"),
        shape = SquircleCardShape,
        color = if (hasError) CoralAccentBg else SurfaceDark,
        border = BorderStroke(1.dp, if (hasError) CoralAccentBorder else BorderDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (hasError) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = "Import error",
                            tint = CoralAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "Dictionary import paused",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary
                            )
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Text(
                                    text = "تعذّر إكمال استيراد القاموس",
                                    style = ArabicTypography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onRetryImport,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, GoldAccentBorder),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("btn_onboarding_retry_import")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Retry import",
                            tint = GoldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = "Retry import",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = GoldAccent
                        )
                    }
                }
            } else {
                val displayedImported = importState.importedRows.coerceIn(0, expectedRows)
                val pct = (importState.progressFraction * 100).toInt().coerceIn(0, 100)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Preparing offline dictionary… ($pct%)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                            color = TextSecondary
                        )
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Text(
                                text = "جاري تجهيز القاموس دون إنترنت…",
                                style = ArabicTypography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }
                    Text(
                        text = "$displayedImported / $expectedRows",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldAccent,
                        modifier = Modifier.testTag("onboarding_import_count_text")
                    )
                }
                LinearProgressIndicator(
                    progress = { importState.progressFraction.coerceIn(0.02f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .testTag("onboarding_import_progress_indicator"),
                    color = GoldAccent,
                    trackColor = SurfaceElevatedDark
                )
            }
        }
    }
}

@Composable
private fun OnboardingWelcomeStep(
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("onboarding_step_1_welcome"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Vertically centered Welcome Hero Card
        Box(
            modifier = Modifier
                .weight(1f, fill = true)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = SurfaceDark,
                border = BorderStroke(1.dp, GoldAccentBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 76dp Qaf (ق) Emblem Container with subtle gold glow
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        GoldAccent.copy(alpha = 0.18f),
                                        GoldAccent.copy(alpha = 0.06f)
                                    )
                                )
                            )
                            .border(1.dp, GoldAccentBorder, RoundedCornerShape(20.dp))
                            .testTag("onboarding_qaf_logo"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = "Qaf Logo ق",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(68.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Brand Pill
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            text = "Qaf • \u2067ق\u2069",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = GoldAccent
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Learn English, in your language",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 20.sp,
                            lineHeight = 26.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.testTag("onboarding_welcome_tagline_en")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(
                            text = "تعلّم الإنجليزية بلغتك",
                            style = ArabicTypography.titleMedium.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("onboarding_welcome_tagline_ar")
                        )
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // Inner Welcome Features Box
                    val summaryPrimaryColor = if (com.example.ui.theme.AppThemeState.isLightMode) {
                        TextPrimary
                    } else {
                        Color(0xFFF4F4F6)
                    }
                    val summaryDividerColor = if (com.example.ui.theme.AppThemeState.isLightMode) {
                        com.example.ui.theme.BorderSubtle
                    } else {
                        Color.White.copy(alpha = 0.07f)
                    }
                    val summaryArabicColor = if (com.example.ui.theme.AppThemeState.isLightMode) {
                        TextSecondary
                    } else {
                        Color(0xFF9496A1)
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SurfaceElevatedDark,
                        border = BorderStroke(1.dp, BorderDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Oxford vocabulary, graded grammar & reading — 100% offline, no account required.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.sp,
                                    lineHeight = 21.sp
                                ),
                                color = summaryPrimaryColor,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            HorizontalDivider(
                                color = summaryDividerColor,
                                thickness = 1.dp
                            )

                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Text(
                                    text = "مفردات أكسفورد، القواعد والقراءة المتدرجة — يعمل بالكامل دون إنترنت ودون حساب.",
                                    style = ArabicTypography.bodySmall.copy(
                                        fontSize = 13.sp,
                                        lineHeight = 20.sp
                                    ),
                                    color = summaryArabicColor,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }

        // Sticky Bottom CTA Button
        OnboardingStickyFooter(
            ctaTextEn = "Get started",
            ctaTextAr = "ابدأ الآن",
            onClick = onGetStarted,
            buttonTestTag = "btn_onboarding_get_started"
        )
    }
}

@Composable
private fun OnboardingLevelStep(
    selectedLevel: String,
    importState: ImportProgressState,
    placementQuizState: PlacementQuizUiState,
    onSelectLevel: (String) -> Unit,
    onContinueWithLevel: (String) -> Unit,
    onStartPlacementQuiz: () -> Unit,
    onAnswerPlacementQuestion: (String) -> Unit,
    onRetakePlacementQuiz: () -> Unit,
    onUpdateQuizEditableLevel: (String) -> Unit,
    onPlacementQuizBack: () -> Unit,
    onRetryImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (placementQuizState.isQuizActive) {
        BackHandler(enabled = true) {
            onPlacementQuizBack()
        }
        OnboardingPlacementQuizView(
            importState = importState,
            quizState = placementQuizState,
            onAnswerQuestion = onAnswerPlacementQuestion,
            onRetakeQuiz = onRetakePlacementQuiz,
            onUpdateEditableLevel = onUpdateQuizEditableLevel,
            onConfirmSuggestedLevel = { chosenLevel ->
                onContinueWithLevel(chosenLevel)
            },
            onBack = onPlacementQuizBack,
            onRetryImport = onRetryImport,
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("onboarding_step_2_level"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Hero Header
            OnboardingHeroHeader(
                titleEn = "Choose your English level",
                titleAr = "اختر مستواك في اللغة الإنجليزية",
                titleEnTestTag = "onboarding_level_header_en"
            )

            // 5 CEFR Level Cards (A1, A2, B1, B2, C1) with B2 preselected by default
            OnboardingPlacementQuiz.LEVEL_OPTIONS.forEach { option ->
                val isSelected = selectedLevel.equals(option.level, ignoreCase = true)
                LevelOptionCard(
                    option = option,
                    isSelected = isSelected,
                    onClick = { onSelectLevel(option.level) }
                )
            }

            // "Not sure" card -> dashed gold border + right chevron icon
            val dashedBorderColor = GoldAccentBorder
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .fillMaxWidth()
                    .heightIn(min = 58.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                GoldAccent.copy(alpha = 0.12f),
                                SurfaceElevatedDark.copy(alpha = 0.9f)
                            )
                        )
                    )
                    .drawBehind {
                        drawRoundRect(
                            color = dashedBorderColor,
                            style = Stroke(
                                width = 1.2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                            ),
                            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                        )
                    }
                    .clickable(role = Role.Button, onClick = onStartPlacementQuiz)
                    .padding(horizontal = 14.dp, vertical = 13.dp)
                    .testTag("btn_onboarding_not_sure")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceElevatedDark)
                            .border(1.dp, BorderDark, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Quiz,
                            contentDescription = "Not sure placement quiz",
                            tint = GoldAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Not sure? Take a quick placement quiz",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = GoldAccent
                        )
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Text(
                                text = "لست متأكداً؟ خذ اختبار تحديد مستوى من ١٠ أسئلة",
                                style = ArabicTypography.labelSmall.copy(fontSize = 12.sp),
                                color = TextSecondary,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Sticky Bottom CTA Button: Continue with selected level
        OnboardingStickyFooter(
            ctaTextEn = "Continue with $selectedLevel",
            ctaTextAr = "المتابعة بمستوى $selectedLevel",
            onClick = { onContinueWithLevel(selectedLevel) },
            buttonTestTag = "btn_onboarding_step2_continue"
        )
    }
}

@Composable
private fun LevelOptionCard(
    option: CefrLevelOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 62.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .testTag("onboarding_level_card_${option.level}"),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) GoldAccentDim else SurfaceDark,
        border = BorderStroke(
            width = 1.5.dp,
            color = if (isSelected) GoldAccent else BorderDark
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 44dp Level badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) GoldAccent else SurfaceElevatedDark)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) GoldAccent else BorderDark,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option.level,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (isSelected) OnGoldDark else TextPrimary
                )
            }

            // Center Content Column (BiDi-Safe Title Row: English • Arabic)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option.titleEn,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (isSelected) GoldAccent else TextPrimary
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                            color = TextSecondary
                        )
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Text(
                                text = option.titleAr,
                                style = ArabicTypography.labelSmall.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = if (isSelected) GoldAccent else TextSecondary
                            )
                        }
                    }
                }
                Text(
                    text = option.descriptionEn,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                    color = TextSecondary
                )
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = option.descriptionAr,
                        style = ArabicTypography.labelSmall.copy(fontSize = 12.sp),
                        color = TextMuted
                    )
                }
            }

            // Consistent Right 22dp Radio Circle on all 5 level cards
            OnboardingRadioCircle(
                isSelected = isSelected,
                testTag = if (isSelected) "onboarding_level_selected_${option.level}" else null
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OnboardingPlacementQuizView(
    importState: ImportProgressState,
    quizState: PlacementQuizUiState,
    onAnswerQuestion: (String) -> Unit,
    onRetakeQuiz: () -> Unit,
    onUpdateEditableLevel: (String) -> Unit,
    onConfirmSuggestedLevel: (String) -> Unit,
    onBack: () -> Unit,
    onRetryImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 8.dp)
            .testTag("onboarding_placement_quiz_container"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Quiz header with Back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("btn_onboarding_quiz_back")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Placement Quiz",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = "اختبار تحديد المستوى",
                        style = ArabicTypography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.size(48.dp))
        }

        when {
            quizState.isWaitingForImport -> {
                val hasError = !importState.error.isNullOrBlank()
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 12.dp)
                        .testTag("onboarding_quiz_waiting_import"),
                    shape = SquirclePlanCardShape,
                    color = SurfaceDark,
                    border = BorderStroke(1.dp, if (hasError) CoralAccentBorder else GoldAccentBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (hasError) {
                                "Dictionary import could not finish"
                            } else {
                                "Preparing quiz questions from offline dictionary…"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Text(
                                text = if (hasError) {
                                    "تعذّر إكمال استيراد القاموس لإنشاء الاختبار"
                                } else {
                                    "جاري تجهيز أسئلة الاختبار من القاموس المحلي…"
                                },
                                style = ArabicTypography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        if (hasError) {
                            Button(
                                onClick = onRetryImport,
                                shape = SquircleCardShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldAccent,
                                    contentColor = OnGoldDark
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_onboarding_quiz_retry_import")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Retry import",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Retry import / إعادة المحاولة",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        } else {
                            val expected = importState.totalExpectedRows.coerceAtLeast(1)
                            val imported = importState.importedRows.coerceIn(0, expected)
                            LinearProgressIndicator(
                                progress = { importState.progressFraction.coerceIn(0.04f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(999.dp)),
                                color = GoldAccent,
                                trackColor = SurfaceElevatedDark
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "$imported / $expected words loaded",
                                style = MaterialTheme.typography.labelMedium,
                                color = GoldAccent
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        OutlinedButton(
                            onClick = onBack,
                            shape = SquircleCardShape,
                            border = BorderStroke(1.dp, BorderDark),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("btn_onboarding_quiz_skip_to_manual")
                        ) {
                            Text(
                                text = "Choose level manually / اختر المستوى يدوياً",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            quizState.isQuizFinished -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .testTag("onboarding_placement_quiz_result"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = SquirclePlanCardShape,
                        color = SurfaceDark,
                        border = BorderStroke(1.dp, GoldAccentBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(GoldAccentDim)
                                    .border(1.5.dp, GoldAccent, RoundedCornerShape(18.dp))
                                    .testTag("onboarding_quiz_suggested_level"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = quizState.suggestedLevel,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = GoldAccent
                                )
                            }

                            Text(
                                text = "Suggested level: ${quizState.suggestedLevel}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = TextPrimary,
                                textAlign = TextAlign.Center
                            )
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Text(
                                    text = "المستوى المقترح: \u2066${quizState.suggestedLevel}\u2069",
                                    style = ArabicTypography.titleMedium,
                                    color = GoldAccent,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SurfaceElevatedDark,
                                border = BorderStroke(1.dp, BorderDark)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "This 10-question check is a rough estimate — you can change your level below.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.testTag("onboarding_quiz_rough_estimate_label")
                                    )
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                        Text(
                                            text = "هذا الاختبار تقدير تقريبي — يمكنك تعديل المستوى أدناه بحرية.",
                                            style = ArabicTypography.labelSmall,
                                            color = TextMuted,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "Selected level to save:",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OnboardingPlacementQuiz.CEFR_LEVELS.forEach { lvl ->
                                    val isChosen = quizState.editableLevel.equals(lvl, ignoreCase = true)
                                    Surface(
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onUpdateEditableLevel(lvl) }
                                            .testTag("onboarding_quiz_edit_level_$lvl"),
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isChosen) GoldAccent else SurfaceElevatedDark,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isChosen) GoldAccent else BorderDark
                                        )
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = lvl,
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontWeight = FontWeight.ExtraBold
                                                ),
                                                color = if (isChosen) OnGoldDark else TextPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onRetakeQuiz,
                        shape = SquircleCardShape,
                        border = BorderStroke(1.dp, GoldAccentBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("btn_onboarding_quiz_retake")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Retake placement quiz",
                            tint = GoldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Retake quiz • \u2067إعادة الاختبار\u2069",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = GoldAccent
                        )
                    }
                }

                OnboardingStickyFooter(
                    ctaTextEn = "Continue with ${quizState.editableLevel}",
                    ctaTextAr = "المتابعة بمستوى ${quizState.editableLevel}",
                    onClick = { onConfirmSuggestedLevel(quizState.editableLevel) },
                    buttonTestTag = "btn_onboarding_quiz_confirm_level"
                )
            }

            else -> {
                val question = quizState.questions.getOrNull(quizState.currentQuestionIndex)
                if (question != null) {
                    val totalQuestions = quizState.questions.size.coerceAtLeast(1)
                    val displayIndex = quizState.currentQuestionIndex + 1

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .testTag("onboarding_placement_quiz_card"),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Question $displayIndex / $totalQuestions",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = GoldAccent,
                                modifier = Modifier.testTag("onboarding_quiz_progress_text")
                            )
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = GoldAccentDim,
                                border = BorderStroke(1.dp, GoldAccentBorder)
                            ) {
                                Text(
                                    text = question.topicLabelEn,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = GoldAccent,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { displayIndex.toFloat() / totalQuestions.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(999.dp)),
                            color = GoldAccent,
                            trackColor = SurfaceElevatedDark
                        )

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = SquircleCardShape,
                            color = SurfaceDark,
                            border = BorderStroke(1.dp, GoldAccentBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (question.type == PlacementQuestionType.VOCABULARY) {
                                    Text(
                                        text = question.promptEn,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                        Text(
                                            text = question.promptAr,
                                            style = ArabicTypography.headlineMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = GoldAccent,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.testTag("onboarding_quiz_prompt")
                                        )
                                    }
                                } else {
                                    Text(
                                        text = question.promptEn,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = TextPrimary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.testTag("onboarding_quiz_prompt")
                                    )
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                        Text(
                                            text = question.promptAr,
                                            style = ArabicTypography.bodySmall,
                                            color = TextSecondary,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        question.options.forEachIndexed { idx, optionText ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 50.dp)
                                    .clip(SquircleCardShape)
                                    .clickable(
                                        role = Role.Button,
                                        onClick = { onAnswerQuestion(optionText) }
                                    )
                                    .testTag("onboarding_quiz_option_$idx"),
                                shape = SquircleCardShape,
                                color = SurfaceDark,
                                border = BorderStroke(1.dp, BorderDark)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(SurfaceElevatedDark)
                                            .border(1.dp, BorderDark, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${idx + 1}",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = GoldAccent
                                        )
                                    }
                                    Text(
                                        text = optionText,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                        color = TextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 50.dp)
                                .clip(SquircleCardShape)
                                .clickable(
                                    role = Role.Button,
                                    onClick = { onAnswerQuestion(OnboardingPlacementQuiz.I_DONT_KNOW_OPTION) }
                                )
                                .testTag("onboarding_quiz_option_idontknow"),
                            shape = SquircleCardShape,
                            color = SurfaceElevatedDark,
                            border = BorderStroke(1.dp, BorderDark)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                        contentDescription = "I don't know",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "I don't know",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = TextSecondary
                                    )
                                }
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                    Text(
                                        text = "لا أعرف",
                                        style = ArabicTypography.bodySmall,
                                        color = TextMuted
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
