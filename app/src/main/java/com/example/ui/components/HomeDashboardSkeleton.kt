package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppThemeState
import com.example.ui.theme.ArabicTypography
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.BorderDark
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldAccentBorder
import com.example.ui.theme.PillShape
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WordBooksSpacing

/**
 * Stage 1: Branded Splash Hold Overlay (`QafBrandSplashOverlay`)
 * Displays a sleek dark branded screen with the Qaf (`ق`) gold emblem and a subtle pulsing glow
 * while initial user preferences and onboarding state are read on cold launch.
 */
@Composable
fun QafBrandSplashOverlay(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "qaf_splash_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "splash_logo_scale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.16f,
        targetValue = 0.32f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "splash_glow_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0D12))
            .testTag("qaf_brand_splash_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Outer ambient glow + 88x88 Qaf emblem box
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.scale(pulseScale)
            ) {
                // Soft radial halo behind emblem
                Box(
                    modifier = Modifier
                        .size(128.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFF5B82E).copy(alpha = glowAlpha),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // 88x88dp Rounded 24.dp Emblem Box
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .shadow(
                            elevation = 24.dp,
                            shape = RoundedCornerShape(24.dp),
                            ambientColor = Color(0xFFF5B82E).copy(alpha = glowAlpha),
                            spotColor = Color(0xFFF5B82E).copy(alpha = glowAlpha)
                        )
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0x38F5B82E),
                                    Color(0xFF171510)
                                )
                            )
                        )
                        .border(
                            width = 1.5.dp,
                            color = Color(0x59F5B82E),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .testTag("qaf_splash_logo_box"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ق",
                        style = ArabicTypography.headlineMedium.copy(
                            fontSize = 46.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFFF5B82E)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Qaf • قاف",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = Color(0xFFF5B82E),
                modifier = Modifier.testTag("qaf_splash_title")
            )

            Spacer(modifier = Modifier.height(2.dp))

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = "تعلّم الإنجليزية بلغتك",
                    style = ArabicTypography.labelMedium.copy(
                        fontSize = 13.sp
                    ),
                    color = Color(0xFF9CA3AF),
                    modifier = Modifier.testTag("qaf_splash_subtitle")
                )
            }
        }
    }
}

/**
 * Reusable animated shimmer brush using `rememberInfiniteTransition` across dark surface tones
 * (`#13161F` -> `#222736` -> `#13161F`), or light mode equivalents when Light Mode is active.
 */
@Composable
fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "home_skeleton_shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = -400f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val isLight = AppThemeState.isLightMode
    val shimmerColors = if (isLight) {
        listOf(
            Color(0xFFE8E4D9),
            Color(0xFFF4F1E8),
            Color(0xFFE8E4D9)
        )
    } else {
        listOf(
            Color(0xFF13161F),
            Color(0xFF222736),
            Color(0xFF2A3145),
            Color(0xFF222736),
            Color(0xFF13161F)
        )
    }

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 300f, 0f),
        end = Offset(translateAnim + 300f, 0f)
    )
}

@Composable
private fun ShimmerBlock(
    brush: Brush,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(brush)
    )
}

/**
 * Stage 2: Facebook-style animated Shimmer Skeleton (`HomeDashboardSkeleton`)
 * Mirrors the exact geometry, heights, and border radii of `HomeScreen` cards:
 * 1. Skeleton Hero Card (`148.dp` min height, `18.dp` radius): Shimmer bars for date, greeting title,
 *    streak pill on the right, 3 profile pills, and 7 circular day dots at the bottom.
 * 2. Skeleton Today's Study Plan Card (`18.dp` radius): Header shimmer row + 5 compact task card
 *    skeletons (`64.dp` height each) with a left checkbox square, 2-3 text shimmer bars, and a right button rectangle.
 * 3. Skeleton Mastery & Lifecycle Card (`16.dp` radius): Top title bar + 3-column mastery bars + 2x3 grid of pill skeletons.
 */
@Composable
fun HomeDashboardSkeleton(
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    val scrollState = rememberScrollState()
    val shimmerBrush = rememberShimmerBrush()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .testTag("home_dashboard_skeleton"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = if (isTablet) 1100.dp else 720.dp)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Skeleton Hero Card (148.dp min height, 18.dp radius)
            SkeletonHeroCard(shimmerBrush = shimmerBrush)

            if (isTablet) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SkeletonTodayStudyPlanCard(shimmerBrush = shimmerBrush)
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SkeletonMasteryAndLifecycleCard(shimmerBrush = shimmerBrush)
                    }
                }
            } else {
                // 2. Skeleton Today's Study Plan Card (18.dp radius, 5 task rows of 64.dp height)
                SkeletonTodayStudyPlanCard(shimmerBrush = shimmerBrush)

                // 3. Skeleton Mastery & Lifecycle Card (16.dp radius, 3-column mastery + 2x3 grid)
                SkeletonMasteryAndLifecycleCard(shimmerBrush = shimmerBrush)
            }

            Spacer(modifier = Modifier.height(WordBooksSpacing.large))
        }
    }
}

@Composable
private fun SkeletonHeroCard(
    shimmerBrush: Brush,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 148.dp)
            .testTag("skeleton_hero_card"),
        color = SurfaceDark,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, BorderDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Row: Date + Greeting + Subtitle on left, Streak pill on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    ShimmerBlock(
                        brush = shimmerBrush,
                        modifier = Modifier
                            .fillMaxWidth(0.45f)
                            .height(10.dp)
                    )
                    ShimmerBlock(
                        brush = shimmerBrush,
                        modifier = Modifier
                            .fillMaxWidth(0.82f)
                            .height(22.dp)
                    )
                    ShimmerBlock(
                        brush = shimmerBrush,
                        modifier = Modifier
                            .fillMaxWidth(0.68f)
                            .height(14.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                ShimmerBlock(
                    brush = shimmerBrush,
                    shape = PillShape,
                    modifier = Modifier
                        .width(96.dp)
                        .height(28.dp)
                )
            }

            // 3 Profile Pills Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShimmerBlock(
                    brush = shimmerBrush,
                    shape = PillShape,
                    modifier = Modifier
                        .width(95.dp)
                        .height(24.dp)
                )
                ShimmerBlock(
                    brush = shimmerBrush,
                    shape = PillShape,
                    modifier = Modifier
                        .width(120.dp)
                        .height(24.dp)
                )
                ShimmerBlock(
                    brush = shimmerBrush,
                    shape = PillShape,
                    modifier = Modifier
                        .width(70.dp)
                        .height(24.dp)
                )
            }

            HorizontalDivider(color = BorderSubtle, thickness = 1.dp)

            // 7 Circular Day Dots at the Bottom
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(7) {
                    ShimmerBlock(
                        brush = shimmerBrush,
                        shape = CircleShape,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SkeletonTodayStudyPlanCard(
    shimmerBrush: Brush,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("skeleton_study_plan_card"),
        color = SurfaceDark,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, GoldAccentBorder.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Plan Header Shimmer Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ShimmerBlock(
                        brush = shimmerBrush,
                        shape = RoundedCornerShape(11.dp),
                        modifier = Modifier.size(38.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ShimmerBlock(
                            brush = shimmerBrush,
                            modifier = Modifier
                                .width(140.dp)
                                .height(16.dp)
                        )
                        ShimmerBlock(
                            brush = shimmerBrush,
                            modifier = Modifier
                                .width(105.dp)
                                .height(12.dp)
                        )
                    }
                }

                ShimmerBlock(
                    brush = shimmerBrush,
                    modifier = Modifier
                        .width(36.dp)
                        .height(20.dp)
                )
            }

            // Progress Bar Shimmer
            ShimmerBlock(
                brush = shimmerBrush,
                shape = PillShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
            )

            // 5 Compact Task Card Skeletons (64.dp height each)
            val titleWidths = listOf(0.70f, 0.65f, 0.75f, 0.68f, 0.60f)
            val subtitleWidths = listOf(0.85f, 0.80f, 0.62f, 0.78f, 0.72f)
            titleWidths.indices.forEach { index ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .testTag("skeleton_task_row_$index"),
                    color = if (AppThemeState.isLightMode) BackgroundDark else Color(0xFF10131B),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, BorderDark)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Left Checkbox Square
                        ShimmerBlock(
                            brush = shimmerBrush,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.size(20.dp)
                        )

                        // Center 2 Text Shimmer Bars
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ShimmerBlock(
                                brush = shimmerBrush,
                                modifier = Modifier
                                    .fillMaxWidth(titleWidths[index])
                                    .height(14.dp)
                            )
                            ShimmerBlock(
                                brush = shimmerBrush,
                                modifier = Modifier
                                    .fillMaxWidth(subtitleWidths[index])
                                    .height(11.dp)
                            )
                        }

                        // Right Button Rectangle
                        ShimmerBlock(
                            brush = shimmerBrush,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .width(68.dp)
                                .height(32.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SkeletonMasteryAndLifecycleCard(
    shimmerBrush: Brush,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("skeleton_mastery_card"),
        color = SurfaceDark,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Title Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ShimmerBlock(
                        brush = shimmerBrush,
                        modifier = Modifier
                            .width(155.dp)
                            .height(16.dp)
                    )
                    ShimmerBlock(
                        brush = shimmerBrush,
                        modifier = Modifier
                            .width(120.dp)
                            .height(12.dp)
                    )
                }
                ShimmerBlock(
                    brush = shimmerBrush,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .width(48.dp)
                        .height(26.dp)
                )
            }

            HorizontalDivider(color = BorderSubtle, thickness = 1.dp)

            // 3-Column Mastery Bars
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(3) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ShimmerBlock(
                            brush = shimmerBrush,
                            modifier = Modifier
                                .fillMaxWidth(0.75f)
                                .height(11.dp)
                        )
                        ShimmerBlock(
                            brush = shimmerBrush,
                            modifier = Modifier
                                .fillMaxWidth(0.60f)
                                .height(14.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = BorderSubtle, thickness = 1.dp)

            // 2x3 Grid of Pill Skeletons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(2) {
                            ShimmerBlock(
                                brush = shimmerBrush,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
