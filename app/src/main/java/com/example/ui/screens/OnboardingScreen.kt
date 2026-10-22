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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar: Step indicator + Skip button on every screen
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
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
                                .height(6.dp)
                                .widthIn(min = if (isActive) 24.dp else 10.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    when {
                                        isActive -> GoldAccent
                                        isDone -> GoldAccent.copy(alpha = 0.45f)
                                        else -> SurfaceElevatedDark
                                    }
                                )
                        )
                    }
                    Text(
                        text = "$currentStep / 5",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }

                TextButton(
                    onClick = onSkipOnboarding,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("btn_onboarding_skip")
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Skip",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = TextSecondary
                        )
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Text(
                                text = "تخطي",
                                style = ArabicTypography.labelSmall,
                                color = TextMuted
                            )
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
                    .padding(top = 6.dp)
            )

            // Main step content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
                contentAlignment = Alignment.Center
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
                    3 -> OnboardingGoalAndTimeStep(
                        selectedLevel = onboardingState.selectedLevel.ifBlank { "B2" },
                        selectedGoal = onboardingState.selectedGoal.ifBlank { "general_fluency" },
                        selectedMinutesPerDay = onboardingState.selectedMinutesPerDay,
                        selectedStudyDaysPerWeek = onboardingState.selectedStudyDaysPerWeek,
                        onSelectGoal = onSelectGoal,
                        onSelectMinutesPerDay = onSelectMinutesPerDay,
                        onSelectStudyDaysPerWeek = onSelectStudyDaysPerWeek,
                        onContinueStep3 = onCompleteStep3,
                        onStep3Back = onStep3Back
                    )
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
            .padding(vertical = 8.dp)
            .testTag("onboarding_step_1_welcome"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Scrollable Hero Card with Qaf (ق) Crest Logo Asset + Bilingual Tagline
        Box(
            modifier = Modifier
                .weight(1f, fill = true)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
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
                        .padding(horizontal = 20.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Qaf (ق) Emblem Container using existing launcher foreground vector + gold crest styling
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        GoldAccentDim,
                                        SurfaceElevatedDark
                                    )
                                )
                            )
                            .border(1.5.dp, GoldAccentBorder, RoundedCornerShape(24.dp))
                            .testTag("onboarding_qaf_logo"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = "Qaf Logo ق",
                            tint = androidx.compose.ui.graphics.Color.Unspecified,
                            modifier = Modifier.size(80.dp)
                        )
                    }

                    // Brand Title
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Qaf • ق",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp
                            ),
                            color = GoldAccent
                        )
                        Text(
                            text = "Learn English, in your language",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = TextPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("onboarding_welcome_tagline_en")
                        )
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Text(
                                text = "تعلّم الإنجليزية بلغتك",
                                style = ArabicTypography.titleMedium.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.testTag("onboarding_welcome_tagline_ar")
                            )
                        }
                    }

                    // Concise offline-first value highlights
                    Surface(
                        shape = SquircleCardShape,
                        color = SurfaceElevatedDark,
                        border = BorderStroke(1.dp, BorderDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Oxford vocabulary, graded grammar & reading — 100% offline, no account required.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Text(
                                    text = "مفردات أكسفورد، القواعد والقراءة المتدرجة — يعمل بالكامل دون إنترنت ودون حساب.",
                                    style = ArabicTypography.bodySmall,
                                    color = TextMuted,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Primary CTA Button: "Get started" (+ Arabic line underneath), pinned at bottom
        Button(
            onClick = onGetStarted,
            shape = SquircleCardShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = GoldAccent,
                contentColor = OnGoldDark
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag("btn_onboarding_get_started")
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(
                    text = "Get started",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = OnGoldDark
                )
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = "ابدأ الآن",
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
            .padding(vertical = 8.dp)
            .testTag("onboarding_step_2_level"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Choose your English level",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("onboarding_level_header_en")
                )
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = "اختر مستواك في اللغة الإنجليزية",
                        style = ArabicTypography.titleMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 5 CEFR Level Cards (A1, A2, B1, B2, C1) with B2 preselected by default
            OnboardingPlacementQuiz.LEVEL_OPTIONS.forEach { option ->
                val isSelected = selectedLevel.equals(option.level, ignoreCase = true)
                LevelOptionCard(
                    option = option,
                    isSelected = isSelected,
                    onClick = { onSelectLevel(option.level) }
                )
            }

            // "Not sure" card -> opens 10-question local placement quiz
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clip(SquircleCardShape)
                    .clickable(role = Role.Button, onClick = onStartPlacementQuiz)
                    .testTag("btn_onboarding_not_sure"),
                shape = SquircleCardShape,
                color = SurfaceElevatedDark,
                border = BorderStroke(1.dp, GoldAccentBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(GoldAccentDim)
                                .border(1.dp, GoldAccentBorder, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Quiz,
                                contentDescription = "Not sure placement quiz",
                                tint = GoldAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Not sure? Take a quick placement quiz",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = GoldAccent
                            )
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Text(
                                    text = "لست متأكداً؟ خذ اختبار تحديد مستوى من ١٠ أسئلة",
                                    style = ArabicTypography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Primary CTA Button: Continue with selected level
        Button(
            onClick = { onContinueWithLevel(selectedLevel) },
            shape = SquircleCardShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = GoldAccent,
                contentColor = OnGoldDark
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag("btn_onboarding_step2_continue")
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                Text(
                    text = "Continue with $selectedLevel",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = OnGoldDark
                )
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = "المتابعة بمستوى $selectedLevel",
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

@Composable
private fun LevelOptionCard(
    option: CefrLevelOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clip(SquircleCardShape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .testTag("onboarding_level_card_${option.level}"),
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
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Level badge
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) GoldAccent else SurfaceElevatedDark)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) GoldAccent else BorderDark,
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option.level,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = if (isSelected) OnGoldDark else TextPrimary
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option.titleEn,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (isSelected) GoldAccent else TextPrimary
                        )
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Text(
                                text = "• ${option.titleAr}",
                                style = ArabicTypography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                    Text(
                        text = option.descriptionEn,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(
                            text = option.descriptionAr,
                            style = ArabicTypography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Selected level ${option.level}",
                    tint = GoldAccent,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(22.dp)
                        .testTag("onboarding_level_selected_${option.level}")
                )
            }
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
                // Waiting for dictionary / grammar import (zero invented fallback questions)
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
                // Placement Quiz Result screen: rough estimate + editable level + Retake
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
                                    text = "المستوى المقترح: ${quizState.suggestedLevel}",
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

                            // Editable level selector chips
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

                    // Retake button
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
                            text = "Retake quiz • إعادة الاختبار",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = GoldAccent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { onConfirmSuggestedLevel(quizState.editableLevel) },
                    shape = SquircleCardShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldAccent,
                        contentColor = OnGoldDark
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .testTag("btn_onboarding_quiz_confirm_level")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = "Continue with ${quizState.editableLevel}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = OnGoldDark
                        )
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Text(
                                text = "المتابعة بمستوى ${quizState.editableLevel}",
                                style = ArabicTypography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = OnGoldDark.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
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
                        // Question progress + level badge
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

                        // Question Prompt Card
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

                        // 4 Real Options
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

                        // Mandatory "I don't know" option on every question
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
