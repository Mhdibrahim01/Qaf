package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val WordBooksDarkColorScheme = darkColorScheme(
    primary = GoldAccent,
    onPrimary = OnGoldDark,
    primaryContainer = GoldAccentDim,
    onPrimaryContainer = GoldAccent,
    secondary = SkyAccent,
    onSecondary = BackgroundDark,
    secondaryContainer = SurfaceElevatedDark,
    onSecondaryContainer = TextPrimary,
    tertiary = EmeraldAccent,
    onTertiary = BackgroundDark,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevatedDark,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = BackgroundDark,
    surfaceContainerLow = SurfaceDark,
    surfaceContainer = SurfaceElevatedDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    outline = BorderDark,
    outlineVariant = BorderDark
)

val WordBooksShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

// Figma Component Shapes (Single Source of Truth)
val BookSpineShape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 5.dp, bottomEnd = 5.dp)
val SquircleCheckShape = RoundedCornerShape(6.dp)
val SquircleTaskShape = RoundedCornerShape(10.dp)
val SquircleBadgeShape = RoundedCornerShape(11.dp)
val SquircleTileShape = RoundedCornerShape(12.dp)
val SquircleCardShape = RoundedCornerShape(14.dp)
val SquircleReviewShape = RoundedCornerShape(16.dp)
val SquirclePlanCardShape = RoundedCornerShape(20.dp)
val SquircleSheetShape = RoundedCornerShape(22.dp)
val SquircleFlashcardShape = RoundedCornerShape(28.dp)
val PillShape = RoundedCornerShape(999.dp)

// Reusable Border Tokens (Single Source of Truth)
val HairlineDarkBorderStroke = BorderStroke(1.dp, BorderDark)
val GoldAccentBorderStroke = BorderStroke(1.dp, GoldAccentBorder)
val EmeraldMasteredBorderStroke = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.45f))

/**
 * Single Source of Truth (SSOT) for Cross-Device Viewport Normalization & Design Tokens.
 * Ensures physical phones with OEM Display Zoom or enlarged system font scales
 * render with the exact same proportions and line-wrapping behavior as the preview emulator.
 */
object WordBooksDesignTokens {
    const val CANONICAL_EMULATOR_WIDTH_DP = 411f
    const val MIN_NORMALIZED_WIDTH_DP = 392f
    const val LOCKED_FONT_SCALE = 1.0f

    val CompactActionButtonPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
    val StandardCardPadding = PaddingValues(16.dp)
}

object WordBooksSpacing {
    val xsmall = 4.dp
    val small = 8.dp
    val mediumSmall = 10.dp
    val medium = 12.dp
    val mediumLarge = 14.dp
    val large = 16.dp
    val xlarge = 18.dp
    val extraLarge = 20.dp
    val doubleLarge = 24.dp
    val tripleLarge = 32.dp
}

@Composable
fun WordBooksTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
            }
        }
    }

    // Single Source of Truth Density & Font Scale Normalizer:
    // 1. Locks fontScale = 1.0f so OEM font size settings (1.15x - 1.35x) never break single-line buttons or badges.
    // 2. Normalizes horizontal density when a physical device's Display Zoom shrinks screenWidthDp below 392dp.
    val configuration = LocalConfiguration.current
    val baseDensity = LocalDensity.current
    val normalizedDensity = remember(configuration.screenWidthDp, baseDensity.density) {
        val rawWidthDp = configuration.screenWidthDp.toFloat()
        val widthScaleFactor = if (rawWidthDp in 240f..<WordBooksDesignTokens.MIN_NORMALIZED_WIDTH_DP) {
            rawWidthDp / WordBooksDesignTokens.MIN_NORMALIZED_WIDTH_DP
        } else {
            1.0f
        }
        Density(
            density = baseDensity.density * widthScaleFactor,
            fontScale = WordBooksDesignTokens.LOCKED_FONT_SCALE
        )
    }

    CompositionLocalProvider(LocalDensity provides normalizedDensity) {
        MaterialTheme(
            colorScheme = WordBooksDarkColorScheme,
            typography = Typography,
            shapes = WordBooksShapes,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    WordBooksTheme(content = content)
}
