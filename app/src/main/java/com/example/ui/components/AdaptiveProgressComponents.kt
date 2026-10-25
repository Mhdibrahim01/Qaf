package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.plan.AdaptiveInsightBanner
import com.example.data.plan.CefrReadinessSummary
import com.example.data.plan.ConqueredWordBadge
import com.example.data.plan.DeckCoverageSummary
import com.example.data.plan.WeeklyGrowthReport
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import com.example.data.plan.LifecycleStageWordItem

import com.example.data.plan.WordLifecycleFunnel
import com.example.ui.theme.ArabicTypography
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderDark
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldAccentBorder
import com.example.ui.theme.GoldAccentDim
import com.example.ui.theme.SkyAccent
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevatedDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletAccent

@Composable
fun AdaptiveInsightBannerCard(
    insight: AdaptiveInsightBanner,
    onDismiss: (String) -> Unit,
    onAcceptLevelUp: ((String) -> Unit)? = null,
    onNavigateToRoute: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("adaptive_insight_banner"),
        color = SurfaceDark,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, GoldAccentBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = insight.titleEn,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        ),
                        color = GoldAccent
                    )
                }

                BidiText(
                    text = insight.messageAr,
                    style = ArabicTypography.labelSmall.copy(fontSize = 12.sp),
                    color = TextSecondary,
                    fillMaxWidth = true,
                    textAlign = TextAlign.Right
                )

                if (insight.actionRoute == "level_up" && insight.suggestedLevel != null && onAcceptLevelUp != null) {
                    Button(
                        onClick = { onAcceptLevelUp(insight.suggestedLevel) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldAccent,
                            contentColor = BackgroundDark
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .heightIn(min = 36.dp)
                            .testTag("btn_accept_level_up")
                    ) {
                        Text(
                            text = insight.actionLabelEn ?: "Advance to ${insight.suggestedLevel}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = BackgroundDark
                        )
                    }
                } else if (!insight.actionRoute.isNullOrBlank() && onNavigateToRoute != null) {
                    OutlinedButton(
                        onClick = { onNavigateToRoute(insight.actionRoute) },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, GoldAccentBorder),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .heightIn(min = 34.dp)
                            .testTag("btn_insight_action")
                    ) {
                        Text(
                            text = insight.actionLabelEn ?: "Open",
                            color = GoldAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            IconButton(
                onClick = { onDismiss(insight.id) },
                modifier = Modifier
                    .size(24.dp)
                    .testTag("btn_dismiss_insight")
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Dismiss insight",
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CefrAndLifecycleCard(
    readiness: CefrReadinessSummary,
    funnel: WordLifecycleFunnel,
    onAcceptLevelUp: ((String) -> Unit)? = null,
    onLoadStageWords: (suspend (String) -> List<LifecycleStageWordItem>)? = null,
    onSpeakWord: ((String) -> Unit)? = null,
    onOpenDictionaryForStage: ((String) -> Unit)? = null,
    onStartStudying: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showStageWordsSheet by remember { mutableStateOf(false) }
    var selectedStageKey by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("cefr_readiness_card"),
        color = SurfaceDark,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderDark)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Part: CEFR Mastery Header
            val titleText = if (readiness.nextLevel != null) {
                "CEFR ${readiness.currentLevel} → ${readiness.nextLevel} Mastery"
            } else {
                "CEFR ${readiness.currentLevel} Mastery"
            }
            val subtitleText = if (readiness.nextLevel != null) {
                "جاهزية الانتقال للمستوى ${readiness.nextLevel}"
            } else {
                "أعلى مستوى تم تحقيقه بنجاح"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = TextPrimary
                    )
                    BidiText(
                        text = subtitleText,
                        style = ArabicTypography.labelSmall.copy(fontSize = 12.sp),
                        color = TextSecondary,
                        fillMaxWidth = true,
                        textAlign = TextAlign.Right
                    )
                }

                Surface(
                    color = if (readiness.isReadyForLevelUp) EmeraldAccent.copy(alpha = 0.16f) else GoldAccentDim,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (readiness.isReadyForLevelUp) EmeraldAccent else GoldAccentBorder
                    )
                ) {
                    Text(
                        text = "${readiness.compositeReadinessPercent}%",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        ),
                        color = if (readiness.isReadyForLevelUp) EmeraldAccent else GoldAccent,
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("cefr_readiness_percent")
                    )
                }
            }

            HorizontalDivider(color = BorderSubtle, thickness = 1.dp)

            // 3-Column Mastery Breakdown Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    ReadinessPillarMetric(
                        label = "Vocab (50%)",
                        percent = readiness.vocabScorePercent,
                        detail = "${readiness.vocabFamiliarOrMatureCount}/${readiness.vocabTargetCount}",
                        color = GoldAccent
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    ReadinessPillarMetric(
                        label = "Grammar (30%)",
                        percent = readiness.grammarScorePercent,
                        detail = "${readiness.grammarPassedCount}/${readiness.grammarTotalCount}",
                        color = VioletAccent
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    ReadinessPillarMetric(
                        label = "Reading (20%)",
                        percent = readiness.readingScorePercent,
                        detail = "${readiness.readingCompletedCount}/${readiness.readingTargetCount}",
                        color = SkyAccent
                    )
                }
            }

            if (readiness.isReadyForLevelUp && readiness.nextLevel != null && onAcceptLevelUp != null) {
                Button(
                    onClick = { onAcceptLevelUp(readiness.nextLevel) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldAccent,
                        contentColor = BackgroundDark
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 42.dp)
                        .testTag("btn_cefr_level_up_confirm")
                ) {
                    Text(
                        text = "Ready! Advance to ${readiness.nextLevel}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = BackgroundDark
                    )
                }
            }

            HorizontalDivider(color = BorderSubtle, thickness = 1.dp)

            // Bottom Part: Vocabulary Lifecycle Stages (2-Column x 3-Row Grid)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("word_lifecycle_funnel_card"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Vocabulary Lifecycle Stages",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        ),
                        color = TextPrimary
                    )
                    Text(
                        text = "${funnel.activeStudiedCount} active • 🏆 ${funnel.conqueredCount} conquered",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.5.sp
                        ),
                        color = GoldAccent
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    // Row 1: New & Seen
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        FunnelStageChip(
                            labelEn = "New",
                            labelAr = "جديدة",
                            count = funnel.newCount,
                            color = Color(0xFF6B7280),
                            testTag = "funnel_stage_new",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                selectedStageKey = "new"
                                showStageWordsSheet = true
                            }
                        )
                        FunnelStageChip(
                            labelEn = "Seen",
                            labelAr = "مقروءة",
                            count = funnel.seenCount,
                            color = Color(0xFF38BDF8),
                            testTag = "funnel_stage_seen",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                selectedStageKey = "seen"
                                showStageWordsSheet = true
                            }
                        )
                    }

                    // Row 2: Learning & Familiar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        FunnelStageChip(
                            labelEn = "Learning",
                            labelAr = "قيد التعلم",
                            count = funnel.learningCount,
                            color = CoralAccent,
                            testTag = "funnel_stage_learning",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                selectedStageKey = "learning"
                                showStageWordsSheet = true
                            }
                        )
                        FunnelStageChip(
                            labelEn = "Familiar",
                            labelAr = "مألوفة",
                            count = funnel.familiarCount,
                            color = Color(0xFFF5B82E),
                            testTag = "funnel_stage_familiar",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                selectedStageKey = "familiar"
                                showStageWordsSheet = true
                            }
                        )
                    }

                    // Row 3: Mature & Conquered
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        FunnelStageChip(
                            labelEn = "Mature",
                            labelAr = "متقنة",
                            count = funnel.matureCount,
                            color = EmeraldAccent,
                            testTag = "funnel_stage_mature",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                selectedStageKey = "mature"
                                showStageWordsSheet = true
                            }
                        )
                        FunnelStageChip(
                            labelEn = "Conquered",
                            labelAr = "",
                            count = funnel.conqueredCount,
                            color = Color(0xFFA855F7),
                            testTag = "funnel_stage_conquered",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                selectedStageKey = "conquered"
                                showStageWordsSheet = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showStageWordsSheet && selectedStageKey != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = {
                showStageWordsSheet = false
                selectedStageKey = null
            },
            sheetState = sheetState,
            containerColor = SurfaceDark,
            contentColor = TextPrimary
        ) {
            WordLifecycleWordsBottomSheetContent(
                stageKey = selectedStageKey!!,
                funnel = funnel,
                onLoadStageWords = onLoadStageWords,
                onSpeakWord = onSpeakWord,
                onOpenDictionary = {
                    val key = selectedStageKey!!
                    showStageWordsSheet = false
                    selectedStageKey = null
                    onOpenDictionaryForStage?.invoke(key)
                },
                onStartStudying = {
                    val key = selectedStageKey!!
                    showStageWordsSheet = false
                    selectedStageKey = null
                    onStartStudying?.invoke(key)
                },
                onDismiss = {
                    showStageWordsSheet = false
                    selectedStageKey = null
                }
            )
        }
    }
}

@Composable
fun CefrReadinessBarCard(
    readiness: CefrReadinessSummary,
    onAcceptLevelUp: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("cefr_readiness_card"),
        color = SurfaceDark,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (readiness.isReadyForLevelUp) EmeraldAccent else GoldAccentBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val titleText = if (readiness.nextLevel != null) {
                "CEFR ${readiness.currentLevel} → ${readiness.nextLevel} Mastery"
            } else {
                "CEFR ${readiness.currentLevel} Mastery"
            }
            val subtitleText = if (readiness.nextLevel != null) {
                "جاهزية الانتقال للمستوى ${readiness.nextLevel}"
            } else {
                "أعلى مستوى تم تحقيقه بنجاح"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Surface(
                    color = if (readiness.isReadyForLevelUp) EmeraldAccent.copy(alpha = 0.16f) else GoldAccentDim,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (readiness.isReadyForLevelUp) EmeraldAccent else GoldAccentBorder)
                ) {
                    Text(
                        text = "${readiness.compositeReadinessPercent}%",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (readiness.isReadyForLevelUp) EmeraldAccent else GoldAccent,
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("cefr_readiness_percent")
                    )
                }
            }

            BidiText(
                text = subtitleText,
                style = ArabicTypography.labelSmall,
                color = TextSecondary,
                fillMaxWidth = true,
                textAlign = TextAlign.Right
            )

            LinearProgressIndicator(
                progress = { (readiness.compositeReadinessPercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (readiness.isReadyForLevelUp) EmeraldAccent else GoldAccent,
                trackColor = SurfaceElevatedDark
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ReadinessPillarMetric(
                    label = "Vocab (50%)",
                    percent = readiness.vocabScorePercent,
                    detail = "${readiness.vocabFamiliarOrMatureCount}/${readiness.vocabTargetCount}",
                    color = GoldAccent
                )
                ReadinessPillarMetric(
                    label = "Grammar (30%)",
                    percent = readiness.grammarScorePercent,
                    detail = "${readiness.grammarPassedCount}/${readiness.grammarTotalCount}",
                    color = VioletAccent
                )
                ReadinessPillarMetric(
                    label = "Reading (20%)",
                    percent = readiness.readingScorePercent,
                    detail = "${readiness.readingCompletedCount}/${readiness.readingTargetCount}",
                    color = SkyAccent
                )
            }

            if (readiness.isReadyForLevelUp && readiness.nextLevel != null && onAcceptLevelUp != null) {
                Button(
                    onClick = { onAcceptLevelUp(readiness.nextLevel) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldAccent,
                        contentColor = BackgroundDark
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 42.dp)
                        .testTag("btn_cefr_level_up_confirm")
                ) {
                    Text(
                        text = "Ready! Advance to ${readiness.nextLevel}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadinessPillarMetric(
    label: String,
    percent: Int,
    detail: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = TextSecondary
        )
        Text(
            text = "$percent% ($detail)",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp
            ),
            color = color
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WordLifecycleFunnelCard(
    funnel: WordLifecycleFunnel,
    onLoadStageWords: (suspend (String) -> List<LifecycleStageWordItem>)? = null,
    onSpeakWord: ((String) -> Unit)? = null,
    onOpenDictionaryForStage: ((String) -> Unit)? = null,
    onStartStudying: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showStageWordsSheet by remember { mutableStateOf(false) }
    var selectedStageKey by remember { mutableStateOf<String?>(null) }
    var showInfoSheet by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("word_lifecycle_funnel_card"),
        color = SurfaceDark,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Vocabulary Lifecycle Stages",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Surface(
                            color = GoldAccentDim,
                            shape = CircleShape,
                            border = BorderStroke(1.dp, GoldAccentBorder),
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .clickable {
                                    showInfoSheet = true
                                }
                                .testTag("word_lifecycle_info_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = "Explain vocabulary lifecycle stages",
                                    tint = GoldAccent,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                    BidiText(
                        text = "مراحل إتقان المفردات (اضغط على أي مرحلة لعرض كلماتها)",
                        style = ArabicTypography.labelSmall,
                        color = TextSecondary,
                        fillMaxWidth = false
                    )
                }
                Text(
                    text = "${funnel.activeStudiedCount} active • 🏆 ${funnel.conqueredCount} conquered",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = GoldAccent
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Row 1: New & Seen
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FunnelStageChip(
                        labelEn = "New",
                        labelAr = "جديدة",
                        count = funnel.newCount,
                        color = TextMuted,
                        testTag = "funnel_stage_new",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedStageKey = "new"
                            showStageWordsSheet = true
                        }
                    )
                    FunnelStageChip(
                        labelEn = "Seen",
                        labelAr = "مقروءة",
                        count = funnel.seenCount,
                        color = SkyAccent,
                        testTag = "funnel_stage_seen",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedStageKey = "seen"
                            showStageWordsSheet = true
                        }
                    )
                }

                // Row 2: Learning & Familiar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FunnelStageChip(
                        labelEn = "Learning",
                        labelAr = "قيد التعلم",
                        count = funnel.learningCount,
                        color = CoralAccent,
                        testTag = "funnel_stage_learning",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedStageKey = "learning"
                            showStageWordsSheet = true
                        }
                    )
                    FunnelStageChip(
                        labelEn = "Familiar",
                        labelAr = "مألوفة",
                        count = funnel.familiarCount,
                        color = GoldAccent,
                        testTag = "funnel_stage_familiar",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedStageKey = "familiar"
                            showStageWordsSheet = true
                        }
                    )
                }

                // Row 3: Mature & Conquered
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FunnelStageChip(
                        labelEn = "Mature",
                        labelAr = "متقنة",
                        count = funnel.matureCount,
                        color = EmeraldAccent,
                        testTag = "funnel_stage_mature",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedStageKey = "mature"
                            showStageWordsSheet = true
                        }
                    )
                    FunnelStageChip(
                        labelEn = "Conquered",
                        labelAr = "",
                        count = funnel.conqueredCount,
                        color = VioletAccent,
                        testTag = "funnel_stage_conquered",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedStageKey = "conquered"
                            showStageWordsSheet = true
                        }
                    )
                }
            }
        }
    }

    if (showStageWordsSheet && selectedStageKey != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = {
                showStageWordsSheet = false
                selectedStageKey = null
            },
            sheetState = sheetState,
            containerColor = SurfaceDark,
            contentColor = TextPrimary
        ) {
            WordLifecycleWordsBottomSheetContent(
                stageKey = selectedStageKey!!,
                funnel = funnel,
                onLoadStageWords = onLoadStageWords,
                onSpeakWord = onSpeakWord,
                onOpenDictionary = {
                    val key = selectedStageKey!!
                    showStageWordsSheet = false
                    selectedStageKey = null
                    onOpenDictionaryForStage?.invoke(key)
                },
                onStartStudying = {
                    val key = selectedStageKey!!
                    showStageWordsSheet = false
                    selectedStageKey = null
                    onStartStudying?.invoke(key)
                },
                onDismiss = {
                    showStageWordsSheet = false
                    selectedStageKey = null
                }
            )
        }
    }

    if (showInfoSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showInfoSheet = false },
            sheetState = sheetState,
            containerColor = SurfaceDark,
            contentColor = TextPrimary
        ) {
            WordLifecycleStagesGuideSheetContent(
                funnel = funnel,
                highlightedStageKey = null,
                onDismiss = { showInfoSheet = false }
            )
        }
    }
}

private data class LifecycleStageGuideItem(
    val key: String,
    val titleEn: String,
    val titleAr: String,
    val count: Int,
    val color: Color,
    val conditionBadge: String,
    val descriptionEn: String,
    val howToTriggerEn: String,
    val descriptionAr: String
)

@Composable
private fun WordLifecycleStagesGuideSheetContent(
    funnel: WordLifecycleFunnel,
    highlightedStageKey: String?,
    onDismiss: () -> Unit
) {
    val stages = remember(funnel) {
        listOf(
            LifecycleStageGuideItem(
                key = "new",
                titleEn = "New",
                titleAr = "جديدة",
                count = funnel.newCount,
                color = TextMuted,
                conditionBadge = "Not encountered yet",
                descriptionEn = "Words in your selected vocabulary books that you haven't looked up or practiced yet.",
                howToTriggerEn = "Start your daily new words session or open a word book to begin learning them.",
                descriptionAr = "كلمات في كتبك المختارة لم تضغط عليها في القصص ولم تراجعها في البطاقات بعد."
            ),
            LifecycleStageGuideItem(
                key = "seen",
                titleEn = "Seen",
                titleAr = "مقروءة / قيد التعرف",
                count = funnel.seenCount,
                color = SkyAccent,
                conditionBadge = "Tapped or Intraday (reps = 0)",
                descriptionEn = "Words you encountered while reading, or flashcards currently in immediate 10m/4h review steps before first recall.",
                howToTriggerEn = "Tap any word in Reading stories, or rate a new flashcard Again (10m) / Hard (4h).",
                descriptionAr = "كلمات ضغطت عليها أثناء القراءة، أو بطاقات قيد المراجعة الفورية (10 دقائق أو 4 ساعات) قبل ترسيخها."
            ),
            LifecycleStageGuideItem(
                key = "learning",
                titleEn = "Learning",
                titleAr = "قيد التعلم",
                count = funnel.learningCount,
                color = CoralAccent,
                conditionBadge = "Recalled (1–6 days)",
                descriptionEn = "Words you successfully recalled at least once (reps >= 1) with an active review gap under 7 days.",
                howToTriggerEn = "Rate a flashcard Good (1d) to graduate it from Seen into Learning and start daily spaced intervals.",
                descriptionAr = "كلمات تذكرتها بنجاح (Good) وبدأت فواصل مراجعتها بالأيام (فاصل يوم إلى ٦ أيام)."
            ),
            LifecycleStageGuideItem(
                key = "familiar",
                titleEn = "Familiar",
                titleAr = "مألوفة",
                count = funnel.familiarCount,
                color = GoldAccent,
                conditionBadge = "Interval 7–20 days",
                descriptionEn = "Words stored in medium-term memory whose reviews are spaced 1 to 3 weeks apart.",
                howToTriggerEn = "Keep rating a card Good across multiple review sessions until its interval reaches 7 to 20 days.",
                descriptionAr = "كلمات أصبحت مألوفة في ذاكرتك المتوسطة ووصل فاصل مراجعتها إلى ما بين ٧ و٢٠ يوماً."
            ),
            LifecycleStageGuideItem(
                key = "mature",
                titleEn = "Mature / Mastered",
                titleAr = "متقنة",
                count = funnel.matureCount,
                color = EmeraldAccent,
                conditionBadge = "Interval 21+ days or Easy",
                descriptionEn = "Words locked into long-term memory (21+ day review interval) or marked as Mastered.",
                howToTriggerEn = "Swipe Right (rate Easy) on a flashcard, tap the Mastered button, or build up a 21+ day interval with Good.",
                descriptionAr = "كلمات ترسّخت في ذاكرتك طويلة المدى (فاصل ٢١+ يوماً، أو قمت بسحب البطاقة لليمين Easy / اختيار Mastered)."
            ),
            LifecycleStageGuideItem(
                key = "conquered",
                titleEn = "Conquered",
                titleAr = "تم التغلب عليها",
                count = funnel.conqueredCount,
                color = VioletAccent,
                conditionBadge = "Failed 2+ times → Mastered",
                descriptionEn = "A special trophy badge for difficult words that you struggled with earlier and eventually mastered!",
                howToTriggerEn = "Earned when a word has at least 2 Again ratings (left swipes) and later reaches Mature / Mastered (21+ days or Easy). Words with 0 or 1 lapse go to Mature instead.",
                descriptionAr = "وسام خاص للكلمات الصعبة التي تعثرت بها مرتين أو أكثر (Again) ثم نجحت لاحقاً في إتقانها! (الكلمات التي تتقنها من أول مرة تذهب إلى «متقنة» فقط)."
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp)
            .testTag("word_lifecycle_guide_sheet"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "How Words Progress",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = TextPrimary
                )
                BidiText(
                    text = "دليل مراحل إتقان المفردات وكيفية الانتقال بينها",
                    style = ArabicTypography.bodySmall,
                    color = GoldAccent,
                    fillMaxWidth = false
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close guide",
                    tint = TextSecondary
                )
            }
        }

        HorizontalDivider(color = BorderDark)

        stages.forEach { item ->
            val isHighlighted = highlightedStageKey == item.key
            Surface(
                color = if (isHighlighted) item.color.copy(alpha = 0.14f) else SurfaceElevatedDark,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    width = if (isHighlighted) 1.5.dp else 1.dp,
                    color = if (isHighlighted) item.color else item.color.copy(alpha = 0.35f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(item.color)
                            )
                            Text(
                                text = "${item.titleEn} (${item.titleAr})",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                        }
                        Surface(
                            color = item.color.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, item.color.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = "${item.count} • ${item.conditionBadge}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = item.color,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = item.descriptionEn,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )

                    Text(
                        text = "How to trigger: ${item.howToTriggerEn}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = item.color
                    )

                    BidiText(
                        text = item.descriptionAr,
                        style = ArabicTypography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun FunnelStageChip(
    labelEn: String,
    labelAr: String,
    count: Int,
    color: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Surface(
        color = SurfaceElevatedDark,
        shape = RoundedCornerShape(9.dp),
        border = BorderStroke(1.dp, BorderDark),
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Text(
                    text = if (labelAr.isNotBlank()) "$labelEn ($labelAr)" else labelEn,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp
                ),
                color = if (count > 0 && labelEn != "New") color else TextPrimary
            )
        }
    }
}

@Composable
fun ConqueredWordsTrophyShelf(
    conqueredWords: List<ConqueredWordBadge>,
    onOpenWord: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("conquered_words_shelf"),
        color = SurfaceDark,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, GoldAccentBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = "Conquered Words",
                        tint = GoldAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Conquered Difficult Words (${conqueredWords.size})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        BidiText(
                            text = "كلمات صعبة تعثرت بها مرتين أو أكثر ثم تم التغلب عليها وإتقانها",
                            style = ArabicTypography.labelSmall,
                            color = TextSecondary,
                            fillMaxWidth = false
                        )
                    }
                }
            }

            if (conqueredWords.isEmpty()) {
                Text(
                    text = "Words that you struggle with 2+ times (Again) and later master earn a permanent trophy here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(conqueredWords, key = { it.cardKey }) { badge ->
                        val struggleCount = badge.againCount.coerceAtLeast(2)
                        val arabicStruggleLabel = when (struggleCount) {
                            2 -> "تعثرت بها مرتين ← أتقنتها"
                            in 3..10 -> "تعثرت بها $struggleCount مرات ← أتقنتها"
                            else -> "تعثرت بها $struggleCount مرة ← أتقنتها"
                        }
                        Surface(
                            color = GoldAccentDim,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, GoldAccent),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .then(
                                    if (onOpenWord != null) {
                                        Modifier.clickable { onOpenWord(badge.text) }
                                    } else {
                                        Modifier
                                    }
                                )
                                .testTag("conquered_word_chip_${badge.text.lowercase()}")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "★ ${badge.text}",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                    color = GoldAccent
                                )
                                if (badge.arabic.isNotBlank()) {
                                    BidiText(
                                        text = badge.arabic,
                                        style = ArabicTypography.labelSmall,
                                        color = TextPrimary,
                                        fillMaxWidth = false
                                    )
                                }
                                Text(
                                    text = "Struggled ${struggleCount}× → Mastered (${badge.intervalDays}d)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                                    color = EmeraldAccent
                                )
                                BidiText(
                                    text = arabicStruggleLabel,
                                    style = ArabicTypography.labelSmall.copy(fontSize = 10.sp),
                                    color = EmeraldAccent,
                                    fillMaxWidth = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyBeforeVsAfterCard(
    weeklyGrowth: WeeklyGrowthReport,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("weekly_growth_card"),
        color = SurfaceDark,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.TrendingUp,
                        contentDescription = null,
                        tint = EmeraldAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "This Week vs. Last Week",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        BidiText(
                            text = "مقارنة أدائك الفعلي خلال آخر ٧ أيام بالأسبوع السابق",
                            style = ArabicTypography.labelSmall,
                            color = TextSecondary,
                            fillMaxWidth = false
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                GrowthComparisonColumn(
                    title = "Words Learned",
                    thisWeek = "${weeklyGrowth.wordsLearnedThisWeek}",
                    prevWeek = "${weeklyGrowth.wordsLearnedPrevWeek}",
                    accent = GoldAccent
                )
                GrowthComparisonColumn(
                    title = "Stories Read",
                    thisWeek = "${weeklyGrowth.storiesCompletedThisWeek}",
                    prevWeek = "${weeklyGrowth.storiesCompletedPrevWeek}",
                    accent = SkyAccent
                )
                GrowthComparisonColumn(
                    title = "Recall Accuracy",
                    thisWeek = weeklyGrowth.accuracyThisWeekPercent?.let { "$it%" } ?: "—",
                    prevWeek = weeklyGrowth.accuracyPrevWeekPercent?.let { "$it%" } ?: "—",
                    accent = EmeraldAccent
                )
                GrowthComparisonColumn(
                    title = "Mistakes Fixed",
                    thisWeek = "${weeklyGrowth.notebookResolvedThisWeek}",
                    prevWeek = "resolved",
                    accent = VioletAccent
                )
            }
        }
    }
}

@Composable
private fun GrowthComparisonColumn(
    title: String,
    thisWeek: String,
    prevWeek: String,
    accent: Color
) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text(
            text = thisWeek,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = accent
        )
        Text(
            text = "Prev: $prevWeek",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = TextSecondary
        )
    }
}

@Composable
fun DeckVocabularyCoverageCard(
    deckCoverages: List<DeckCoverageSummary>,
    onOpenDeck: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (deckCoverages.isEmpty()) return
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("deck_coverage_map_card"),
        color = SurfaceDark,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Selected Decks Coverage Map",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            deckCoverages.forEach { deck ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (onOpenDeck != null) {
                                Modifier.clickable { onOpenDeck(deck.deckId) }
                            } else {
                                Modifier
                            }
                        ),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = deck.titleEn,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary
                        )
                        Text(
                            text = "${deck.coveragePercent}% (${deck.seenOrHigherWords}/${deck.totalWords})",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = GoldAccent
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (deck.coveragePercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = GoldAccent,
                        trackColor = SurfaceElevatedDark
                    )
                }
            }
        }
    }
}


private data class StageMeta(
    val key: String,
    val titleEn: String,
    val titleAr: String,
    val subtitleAr: String,
    val color: Color,
    val emptyMessageAr: String,
    val triggerHintAr: String
)

private fun getStageMeta(stageKey: String): StageMeta {
    return when (stageKey.lowercase()) {
        "new" -> StageMeta(
            key = "new",
            titleEn = "New Words",
            titleAr = "كلمات جديدة",
            subtitleAr = "كلمات في كتبك لم تبدأ بدراستها بعد",
            color = Color(0xFF9CA3AF),
            emptyMessageAr = "رائع! لقد بدأت بدراسة جميع الكلمات المتاحة في مستواك الحالي.",
            triggerHintAr = "تبدأ الكلمات كـ New فور اختيار كتب المستوى أو الكتب المخصصة."
        )
        "seen" -> StageMeta(
            key = "seen",
            titleEn = "Seen Words",
            titleAr = "كلمات مقروءة",
            subtitleAr = "كلمات مرّت عليك بالقراءة أو قيد المراجعة الفورية",
            color = Color(0xFF38BDF8),
            emptyMessageAr = "لا توجد كلمات مقروءة حالياً. اضغط على أي كلمة في قصص القراءة أو راجع البطاقات لتظهر هنا.",
            triggerHintAr = "تصبح الكلمة Seen عند الضغط عليها في القصص أو تقييمها بـ Again / Hard في أول مواجهة."
        )
        "learning" -> StageMeta(
            key = "learning",
            titleEn = "Learning Words",
            titleAr = "قيد التعلم",
            subtitleAr = "كلمات تذكرتها بنجاح وتخضع لفواصل مراجعة يومية (1–6 أيام)",
            color = CoralAccent,
            emptyMessageAr = "لا توجد كلمات قيد التعلم حالياً. عند تقييم بطاقة بـ Good (تذكرتها) ستبدأ بالظهور هنا!",
            triggerHintAr = "تنتقل الكلمة إلى Learning عند تقييمها بـ Good وتثبيت أول تكرار تباعدي لها."
        )
        "familiar" -> StageMeta(
            key = "familiar",
            titleEn = "Familiar Words",
            titleAr = "كلمات مألوفة",
            subtitleAr = "كلمات في الذاكرة المتوسطة بفاصل مراجعة بين أسبوع و 3 أسابيع (7–20 يوماً)",
            color = Color(0xFFF5B82E),
            emptyMessageAr = "لا توجد كلمات مألوفة بعد. استمر بمراجعة البطاقات حتى تتسع فواصل التكرار إلى 7 أيام فأكثر.",
            triggerHintAr = "تصبح الكلمة Familiar عندما يصل فاصل تكرارها إلى ما بين 7 و 20 يوماً."
        )
        "mature" -> StageMeta(
            key = "mature",
            titleEn = "Mature / Mastered",
            titleAr = "كلمات متقنة",
            subtitleAr = "كلمات استقرت في الذاكرة الدائمة (فاصل 21+ يوماً أو سحب Easy)",
            color = EmeraldAccent,
            emptyMessageAr = "لا توجد كلمات متقنة بعد. اسحب البطاقة لليمين كـ Easy أو داوم على تقييم Good لتصل للمستوى المتقن!",
            triggerHintAr = "تصل الكلمة إلى Mature عند سحب Easy أو الوصول لفاصل تكرار 21 يوماً فأكثر."
        )
        "conquered" -> StageMeta(
            key = "conquered",
            titleEn = "Conquered Words",
            titleAr = "تم التغلب عليها",
            subtitleAr = "وسام خاص للكلمات الصعبة التي تعثرت بها مرتين أو أكثر ثم أتقنتها بنجاح!",
            color = Color(0xFFA855F7),
            emptyMessageAr = "لا توجد كلمات صعبة تم التغلب عليها بعد. عند إتقان كلمات تعثرت بها سابقاً ستنال هذا الوسام!",
            triggerHintAr = "تمنح للكلمة التي تعثرت بها (Again) مرتين أو أكثر ثم وصلت إلى مرحلة الإتقان."
        )
        else -> StageMeta(
            key = stageKey,
            titleEn = stageKey.replaceFirstChar { it.uppercase() },
            titleAr = stageKey,
            subtitleAr = "مفردات مرحلة $stageKey",
            color = GoldAccent,
            emptyMessageAr = "لا توجد كلمات في هذه المرحلة.",
            triggerHintAr = ""
        )
    }
}

@Composable
fun WordLifecycleWordsBottomSheetContent(
    stageKey: String,
    funnel: WordLifecycleFunnel,
    onLoadStageWords: (suspend (String) -> List<LifecycleStageWordItem>)?,
    onSpeakWord: ((String) -> Unit)?,
    onOpenDictionary: () -> Unit,
    onStartStudying: () -> Unit,
    onDismiss: () -> Unit
) {
    val meta = remember(stageKey) { getStageMeta(stageKey) }
    var words by remember(stageKey) { mutableStateOf<List<LifecycleStageWordItem>>(emptyList()) }
    var isLoading by remember(stageKey) { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var showHowItWorks by remember { mutableStateOf(false) }

    LaunchedEffect(stageKey) {
        isLoading = true
        words = try {
            onLoadStageWords?.invoke(stageKey) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        isLoading = false
    }

    val filteredWords = remember(words, searchQuery) {
        if (searchQuery.isBlank()) words
        else words.filter {
            it.headword.contains(searchQuery.trim(), ignoreCase = true) ||
            it.arabic.contains(searchQuery.trim())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 680.dp)
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
            .testTag("word_lifecycle_words_sheet"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = meta.color.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, meta.color.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = meta.titleEn,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = meta.color,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = meta.titleAr,
                    style = ArabicTypography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                    color = TextPrimary
                )
            }

            Surface(
                color = SurfaceElevatedDark,
                shape = CircleShape,
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Text(
                    text = "${words.size} كلمة",
                    style = ArabicTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = GoldAccent,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        // Subtitle & Toggle for "How it works"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BidiText(
                text = meta.subtitleAr,
                style = ArabicTypography.bodySmall,
                color = TextSecondary,
                fillMaxWidth = false,
                modifier = Modifier.weight(1f)
            )

            TextButton(
                onClick = { showHowItWorks = !showHowItWorks },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "كيف تعمل المرحلة",
                        tint = GoldAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (showHowItWorks) "إخفاء الدليل" else "كيف تعمل؟",
                        style = ArabicTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = GoldAccent
                    )
                }
            }
        }

        if (showHowItWorks) {
            Surface(
                color = SurfaceElevatedDark,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, meta.color.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "شرط الوصول لهذه المرحلة:",
                        style = ArabicTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = meta.color
                    )
                    BidiText(
                        text = meta.triggerHintAr,
                        style = ArabicTypography.bodySmall,
                        color = TextPrimary,
                        fillMaxWidth = true
                    )
                }
            }
        }

        // Search Bar (shown when there are words)
        if (words.size > 5) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "بحث في كلمات هذه المرحلة...",
                        style = ArabicTypography.bodySmall,
                        color = TextMuted
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldAccent,
                    unfocusedBorderColor = BorderDark,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = SurfaceElevatedDark,
                    unfocusedContainerColor = SurfaceDark
                )
            )
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = GoldAccent,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(36.dp)
                )
            }
        } else if (words.isEmpty()) {
            // Empty State
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    color = meta.color.copy(alpha = 0.12f),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, meta.color.copy(alpha = 0.35f)),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when (stageKey.lowercase()) {
                                "conquered" -> Icons.Default.EmojiEvents
                                "mature" -> Icons.Outlined.CheckCircle
                                else -> Icons.Default.School
                            },
                            contentDescription = null,
                            tint = meta.color,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Text(
                    text = "لا توجد كلمات في هذه المرحلة بعد",
                    style = ArabicTypography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                BidiText(
                    text = meta.emptyMessageAr,
                    style = ArabicTypography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    fillMaxWidth = true
                )

                Button(
                    onClick = onStartStudying,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldAccent,
                        contentColor = BackgroundDark
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(44.dp)
                ) {
                    Text(
                        text = "ابدأ المذاكرة الآن",
                        style = ArabicTypography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                        color = BackgroundDark
                    )
                }
            }
        } else {
            // Word List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredWords, key = { "${it.deck}:${it.headword}" }) { item ->
                    WordLifecycleWordRow(
                        item = item,
                        onSpeakWord = onSpeakWord
                    )
                }
            }

            // Bottom action: Open in Dictionary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                OutlinedButton(
                    onClick = onOpenDictionary,
                    border = BorderStroke(1.dp, GoldAccentBorder),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = GoldAccent
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "عرض في القاموس الكامل",
                        style = ArabicTypography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
private fun WordLifecycleWordRow(
    item: LifecycleStageWordItem,
    onSpeakWord: ((String) -> Unit)?
) {
    Surface(
        color = SurfaceElevatedDark,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.headword,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    if (item.pos.isNotBlank() && item.pos != "word") {
                        Surface(
                            color = BackgroundDark,
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(0.5.dp, BorderSubtle)
                        ) {
                            Text(
                                text = item.pos,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                color = TextMuted,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                if (item.arabic.isNotBlank()) {
                    BidiText(
                        text = item.arabic,
                        style = ArabicTypography.bodySmall,
                        color = GoldAccent,
                        fillMaxWidth = false
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (item.intervalDays > 0) {
                    Surface(
                        color = SurfaceDark,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(0.5.dp, BorderSubtle)
                    ) {
                        Text(
                            text = "${item.intervalDays}d",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else if (item.repetitions > 0) {
                    Surface(
                        color = SurfaceDark,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(0.5.dp, BorderSubtle)
                    ) {
                        Text(
                            text = "${item.repetitions}x",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                IconButton(
                    onClick = { onSpeakWord?.invoke(item.headword) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Speak ${item.headword}",
                        tint = GoldAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
