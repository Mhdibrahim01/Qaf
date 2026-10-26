package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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

private fun buildWordBooksColorScheme(isLight: Boolean): ColorScheme {
    return if (isLight) {
        lightColorScheme(
            primary = GoldAccent,
            onPrimary = OnGoldDark,
            primaryContainer = GoldAccentDim,
            onPrimaryContainer = GoldAccent,
            secondary = SkyAccent,
            onSecondary = SurfaceDark,
            secondaryContainer = SurfaceElevatedDark,
            onSecondaryContainer = TextPrimary,
            tertiary = EmeraldAccent,
            onTertiary = SurfaceDark,
            background = BackgroundDark,
            onBackground = TextPrimary,
            surface = SurfaceDark,
            onSurface = TextPrimary,
            surfaceVariant = SurfaceElevatedDark,
            onSurfaceVariant = TextSecondary,
            surfaceContainerLowest = SurfaceDark,
            surfaceContainerLow = BackgroundDark,
            surfaceContainer = SurfaceElevatedDark,
            surfaceContainerHigh = SurfaceContainerHighDark,
            outline = BorderDark,
            outlineVariant = BorderSubtle
        )
    } else {
        darkColorScheme(
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
    }
}

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
val HairlineDarkBorderStroke: BorderStroke
    get() = BorderStroke(1.dp, BorderDark)
val GoldAccentBorderStroke: BorderStroke
    get() = BorderStroke(1.dp, GoldAccentBorder)
val EmeraldMasteredBorderStroke: BorderStroke
    get() = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.45f))

/**
 * Single Source of Truth (SSOT) for Cross-Device Viewport Normalization & Design Tokens.
 * Ensures physical phones with OEM Display Zoom or enlarged system font scales
 * render with the exact same proportions and line-wrapping behavior as the preview emulator.
 */
object WordBooksDesignTokens {
    const val CANONICAL_EMULATOR_WIDTH_DP = 411f
    const val MIN_NORMALIZED_WIDTH_DP = CANONICAL_EMULATOR_WIDTH_DP
    const val LOCKED_FONT_SCALE = 1.0f
    const val MIN_ACCESSIBLE_FONT_SCALE = 0.90f
    const val MAX_ACCESSIBLE_FONT_SCALE = 1.30f

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
    themeMode: AppThemeMode = AppThemeState.themeMode,
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val resolvedIsLight = when (themeMode) {
        AppThemeMode.LIGHT -> true
        AppThemeMode.DARK -> false
        AppThemeMode.SYSTEM -> !systemInDark
    }
    AppThemeState.themeMode = themeMode
    AppThemeState.isLightMode = resolvedIsLight

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = resolvedIsLight
                    isAppearanceLightNavigationBars = resolvedIsLight
                }
            }
        }
    }

    // Single Source of Truth Density & Font Scale Normalizer:
    // 1. Locks fontScale to 1.0f so system "Large/Huge Font" settings do not overflow single-line badges or compact buttons.
    // 2. Normalizes horizontal density on any handset narrower than 411dp (e.g., 360dp, 384dp, 392dp, 393dp)
    //    so physical phones render with the exact 411dp viewport proportions of the AI Studio Emulator.
    val configuration = LocalConfiguration.current
    val baseDensity = LocalDensity.current
    val displayWidthPx = view.resources.displayMetrics.widthPixels
    val normalizedDensity = remember(configuration.screenWidthDp, displayWidthPx, baseDensity.density) {
        val exactWidthDp = if (displayWidthPx > 0 && baseDensity.density > 0f) {
            displayWidthPx.toFloat() / baseDensity.density
        } else {
            configuration.screenWidthDp.toFloat()
        }
        val rawWidthDp = if (exactWidthDp > 0f) exactWidthDp else configuration.screenWidthDp.toFloat()
        val widthScaleFactor = if (rawWidthDp in 240f..<WordBooksDesignTokens.CANONICAL_EMULATOR_WIDTH_DP) {
            rawWidthDp / WordBooksDesignTokens.CANONICAL_EMULATOR_WIDTH_DP
        } else {
            1.0f
        }
        Density(
            density = baseDensity.density * widthScaleFactor,
            fontScale = WordBooksDesignTokens.LOCKED_FONT_SCALE
        )
    }

    val colorScheme = remember(resolvedIsLight) {
        buildWordBooksColorScheme(resolvedIsLight)
    }
    val typography = remember(resolvedIsLight) {
        Typography
    }

    CompositionLocalProvider(
        LocalDensity provides normalizedDensity,
        androidx.compose.material3.LocalContentColor provides TextPrimary
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
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
    WordBooksTheme(
        themeMode = if (darkTheme) AppThemeMode.DARK else AppThemeMode.LIGHT,
        content = content
    )
}
