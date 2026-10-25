package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp
import com.example.R

/**
 * Single Source of Truth (SSOT) Font Metric Configuration:
 * Disables OEM-specific extra font ascent/descent padding (`includeFontPadding = false`)
 * and centers line heights so bundled Inter, Cairo, and Tajawal fonts render with
 * identical vertical alignment on every physical phone and emulator.
 */
private val DeterministicPlatformTextStyle = PlatformTextStyle(
    includeFontPadding = false
)

private val DeterministicLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

val InterFontFamily = FontFamily(
    Font(R.font.inter, FontWeight.Normal, loadingStrategy = FontLoadingStrategy.OptionalLocal),
    Font(R.font.inter, FontWeight.Medium, loadingStrategy = FontLoadingStrategy.OptionalLocal),
    Font(R.font.inter, FontWeight.SemiBold, loadingStrategy = FontLoadingStrategy.OptionalLocal),
    Font(R.font.inter, FontWeight.Bold, loadingStrategy = FontLoadingStrategy.OptionalLocal)
)

val CairoFontFamily = FontFamily(
    Font(R.font.cairo, FontWeight.Normal, loadingStrategy = FontLoadingStrategy.OptionalLocal),
    Font(R.font.cairo, FontWeight.Medium, loadingStrategy = FontLoadingStrategy.OptionalLocal),
    Font(R.font.cairo, FontWeight.SemiBold, loadingStrategy = FontLoadingStrategy.OptionalLocal),
    Font(R.font.cairo, FontWeight.Bold, loadingStrategy = FontLoadingStrategy.OptionalLocal)
)

val TajawalFontFamily = FontFamily(
    Font(R.font.tajawal, FontWeight.Normal, loadingStrategy = FontLoadingStrategy.OptionalLocal),
    Font(R.font.tajawal, FontWeight.Medium, loadingStrategy = FontLoadingStrategy.OptionalLocal),
    Font(R.font.tajawal, FontWeight.Bold, loadingStrategy = FontLoadingStrategy.OptionalLocal)
)

// Dedicated Arabic TextStyles with Cairo/Tajawal, ContentOrRtl direction, and locked font padding
object ArabicTypography {
    val headlineMedium = TextStyle(
        fontFamily = CairoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 34.sp,
        textDirection = TextDirection.ContentOrRtl,
        platformStyle = DeterministicPlatformTextStyle,
        lineHeightStyle = DeterministicLineHeightStyle
    )

    val titleMedium = TextStyle(
        fontFamily = CairoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 28.sp,
        textDirection = TextDirection.ContentOrRtl,
        platformStyle = DeterministicPlatformTextStyle,
        lineHeightStyle = DeterministicLineHeightStyle
    )

    val bodyLarge = TextStyle(
        fontFamily = CairoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp,
        textDirection = TextDirection.ContentOrRtl,
        platformStyle = DeterministicPlatformTextStyle,
        lineHeightStyle = DeterministicLineHeightStyle
    )

    val bodyMedium = TextStyle(
        fontFamily = TajawalFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 24.sp,
        textDirection = TextDirection.ContentOrRtl,
        platformStyle = DeterministicPlatformTextStyle,
        lineHeightStyle = DeterministicLineHeightStyle
    )

    val labelMedium = TextStyle(
        fontFamily = CairoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 20.sp,
        textDirection = TextDirection.ContentOrRtl,
        platformStyle = DeterministicPlatformTextStyle,
        lineHeightStyle = DeterministicLineHeightStyle
    )

    val bodySmall = TextStyle(
        fontFamily = TajawalFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 20.sp,
        textDirection = TextDirection.ContentOrRtl,
        platformStyle = DeterministicPlatformTextStyle,
        lineHeightStyle = DeterministicLineHeightStyle
    )

    val labelSmall = TextStyle(
        fontFamily = CairoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        textDirection = TextDirection.ContentOrRtl,
        platformStyle = DeterministicPlatformTextStyle,
        lineHeightStyle = DeterministicLineHeightStyle
    )
}

val Typography: Typography
    get() = Typography(
        displayLarge = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 40.sp,
            lineHeight = 48.sp,
            letterSpacing = (-0.5).sp,
            color = TextPrimary,
            platformStyle = DeterministicPlatformTextStyle,
            lineHeightStyle = DeterministicLineHeightStyle
        ),
        displayMedium = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 36.sp,
            lineHeight = 44.sp,
            letterSpacing = (-0.5).sp,
            color = TextPrimary,
            platformStyle = DeterministicPlatformTextStyle,
            lineHeightStyle = DeterministicLineHeightStyle
        ),
        displaySmall = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = (-0.5).sp,
            color = TextPrimary,
            platformStyle = DeterministicPlatformTextStyle,
            lineHeightStyle = DeterministicLineHeightStyle
        ),
        headlineLarge = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = (-0.3).sp,
            color = TextPrimary,
            platformStyle = DeterministicPlatformTextStyle,
            lineHeightStyle = DeterministicLineHeightStyle
        ),
        headlineMedium = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = (-0.3).sp,
            color = TextPrimary,
            platformStyle = DeterministicPlatformTextStyle,
            lineHeightStyle = DeterministicLineHeightStyle
        ),
        headlineSmall = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 28.sp,
            color = TextPrimary,
            platformStyle = DeterministicPlatformTextStyle,
            lineHeightStyle = DeterministicLineHeightStyle
        ),
        titleLarge = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 26.sp,
            color = TextPrimary,
            platformStyle = DeterministicPlatformTextStyle,
            lineHeightStyle = DeterministicLineHeightStyle
        ),
        titleMedium = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.1.sp,
            color = TextPrimary,
            platformStyle = DeterministicPlatformTextStyle,
            lineHeightStyle = DeterministicLineHeightStyle
        ),
        titleSmall = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
            color = TextPrimary,
            platformStyle = DeterministicPlatformTextStyle,
            lineHeightStyle = DeterministicLineHeightStyle
        ),
        bodyLarge = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            color = TextPrimary,
            platformStyle = DeterministicPlatformTextStyle,
            lineHeightStyle = DeterministicLineHeightStyle
        ),
        bodyMedium = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = TextSecondary,
            platformStyle = DeterministicPlatformTextStyle,
            lineHeightStyle = DeterministicLineHeightStyle
        ),
        bodySmall = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = TextSecondary,
            platformStyle = DeterministicPlatformTextStyle,
            lineHeightStyle = DeterministicLineHeightStyle
        ),
        labelLarge = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.2.sp,
            platformStyle = DeterministicPlatformTextStyle,
            lineHeightStyle = DeterministicLineHeightStyle
        ),
        labelMedium = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.3.sp,
            color = TextSecondary,
            platformStyle = DeterministicPlatformTextStyle,
            lineHeightStyle = DeterministicLineHeightStyle
        ),
        labelSmall = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.4.sp,
            color = TextMuted,
            platformStyle = DeterministicPlatformTextStyle,
            lineHeightStyle = DeterministicLineHeightStyle
        )
    )

val EyebrowStyle: TextStyle
    get() = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.3.sp,
        color = TextSubtle,
        platformStyle = DeterministicPlatformTextStyle,
        lineHeightStyle = DeterministicLineHeightStyle
    )

val StatValueGridStyle: TextStyle
    get() = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 26.sp,
        letterSpacing = (-1).sp,
        color = GoldAccent,
        platformStyle = DeterministicPlatformTextStyle,
        lineHeightStyle = DeterministicLineHeightStyle
    )

val StatValueReviewStyle: TextStyle
    get() = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 31.sp,
        lineHeight = 36.sp,
        letterSpacing = (-1).sp,
        color = GoldAccent,
        platformStyle = DeterministicPlatformTextStyle,
        lineHeightStyle = DeterministicLineHeightStyle
    )
