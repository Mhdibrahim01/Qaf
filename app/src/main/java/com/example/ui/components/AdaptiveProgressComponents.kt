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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.plan.AdaptiveInsightBanner
import com.example.data.plan.CefrReadinessSummary
import com.example.data.plan.ConqueredWordBadge
import com.example.data.plan.DeckCoverageSummary
import com.example.data.plan.WeeklyGrowthReport
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
        color = GoldAccentDim,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, GoldAccentBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = insight.titleEn,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = GoldAccent
                        )
                        BidiText(
                            text = insight.titleAr,
                            style = ArabicTypography.labelSmall,
                            color = TextPrimary,
                            fillMaxWidth = false
                        )
                    }
                }

                IconButton(
                    onClick = { onDismiss(insight.id) },
                    modifier = Modifier
                        .size(32.dp)
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

            Text(
                text = insight.messageEn,
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary
            )
            BidiText(
                text = insight.messageAr,
                style = ArabicTypography.labelSmall,
                color = TextSecondary
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
                        .heightIn(min = 38.dp)
                        .testTag("btn_accept_level_up")
                ) {
                    Text(
                        text = insight.actionLabelEn ?: "Advance to ${insight.suggestedLevel}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            } else if (!insight.actionRoute.isNullOrBlank() && onNavigateToRoute != null) {
                OutlinedButton(
                    onClick = { onNavigateToRoute(insight.actionRoute) },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, GoldAccentBorder),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier
                        .heightIn(min = 36.dp)
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "CEFR ${readiness.currentLevel} → ${readiness.nextLevel} Mastery",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    BidiText(
                        text = "جاهزية المستوى الموحدة (٥٠٪ مفردات • ٣٠٪ قواعد • ٢٠٪ قراءة)",
                        style = ArabicTypography.labelSmall,
                        color = TextSecondary,
                        fillMaxWidth = false
                    )
                }
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
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
        Text(
            text = "$percent% ($detail)",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = color
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WordLifecycleFunnelCard(
    funnel: WordLifecycleFunnel,
    modifier: Modifier = Modifier
) {
    var showInfoSheet by remember { mutableStateOf(false) }
    var highlightedStageKey by remember { mutableStateOf<String?>(null) }

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
                                    highlightedStageKey = null
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
                        text = "مراحل إتقان المفردات (اضغط على أي مرحلة لمعرفة التفاصيل)",
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
                            highlightedStageKey = "new"
                            showInfoSheet = true
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
                            highlightedStageKey = "seen"
                            showInfoSheet = true
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
                            highlightedStageKey = "learning"
                            showInfoSheet = true
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
                            highlightedStageKey = "familiar"
                            showInfoSheet = true
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
                            highlightedStageKey = "mature"
                            showInfoSheet = true
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
                            highlightedStageKey = "conquered"
                            showInfoSheet = true
                        }
                    )
                }
            }
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
                highlightedStageKey = highlightedStageKey,
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
                titleAr = "مقروءة",
                count = funnel.seenCount,
                color = SkyAccent,
                conditionBadge = "Tapped in Reading",
                descriptionEn = "Words you encountered in context while reading a story, before studying them as flashcards.",
                howToTriggerEn = "Open any story in the Reading tab and tap a word to view its instant translation. Seen words are prioritized in your next daily plan!",
                descriptionAr = "كلمات ضغطت عليها لمعرفة معناها أثناء قراءة القصص، وتُعطى أولوية تلقائياً في خطتك اليومية القادمة."
            ),
            LifecycleStageGuideItem(
                key = "learning",
                titleEn = "Learning",
                titleAr = "قيد التعلم",
                count = funnel.learningCount,
                color = CoralAccent,
                conditionBadge = "Interval < 7 days",
                descriptionEn = "Words you are actively practicing in flashcards that still have a short review gap (under 7 days).",
                howToTriggerEn = "Flip a flashcard and rate it Again (10m), Hard (4h / 1d), or Good on your first two reviews (1d → 6d).",
                descriptionAr = "كلمات بدأت بتدريبها في البطاقات التعليمية وما زال فاصل مراجعتها قصيراً (أقل من ٧ أيام)."
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
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
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
                .padding(horizontal = 12.dp, vertical = 9.dp),
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
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                color = color
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
