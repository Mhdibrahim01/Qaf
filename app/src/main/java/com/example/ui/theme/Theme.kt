package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
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

// Figma Component Shapes
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

    MaterialTheme(
        colorScheme = WordBooksDarkColorScheme,
        typography = Typography,
        shapes = WordBooksShapes,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    WordBooksTheme(content = content)
}
