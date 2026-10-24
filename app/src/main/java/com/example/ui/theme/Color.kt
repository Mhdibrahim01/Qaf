package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

enum class AppThemeMode(val id: String) {
    DARK("dark"),
    LIGHT("light"),
    SYSTEM("system");

    companion object {
        fun fromId(id: String?): AppThemeMode {
            return entries.firstOrNull { it.id.equals(id?.trim(), ignoreCase = true) } ?: DARK
        }
    }
}

/**
 * Reactive global theme state holder so all semantic color tokens in [Color.kt]
 * automatically recompose across the entire app when switching between Dark and Light mode.
 */
object AppThemeState {
    var themeMode: AppThemeMode by mutableStateOf(AppThemeMode.DARK)
    var isLightMode: Boolean by mutableStateOf(false)
}

// Refined Dark Luxury vs. Warm Editorial Light Palette — Tonal surface layering & subtle luminance
val BackgroundDark: Color
    get() = if (AppThemeState.isLightMode) Color(0xFFF7F4EC) else Color(0xFF090A0F)

val SurfaceDark: Color
    get() = if (AppThemeState.isLightMode) Color(0xFFFFFFFF) else Color(0xFF12131A)

val SurfaceElevatedDark: Color
    get() = if (AppThemeState.isLightMode) Color(0xFFEFECE2) else Color(0xFF191B24)

val SurfaceContainerHighDark: Color
    get() = if (AppThemeState.isLightMode) Color(0xFFE5E0D3) else Color(0xFF212430)

// Soft translucent borders & surface highlights
val BorderDark: Color
    get() = if (AppThemeState.isLightMode) Color(0x1F1A1915) else Color(0x17FFFFFF)

val BorderSubtle: Color
    get() = if (AppThemeState.isLightMode) Color(0x141A1915) else Color(0x12FFFFFF)

val BorderMedium: Color
    get() = if (AppThemeState.isLightMode) Color(0x331A1915) else Color(0x24FFFFFF)

// Gold Accents for primary actions, active states, and mastery highlights
val GoldAccent: Color
    get() = if (AppThemeState.isLightMode) Color(0xFFB47D08) else Color(0xFFF5C542)

val GoldAccentDim: Color
    get() = if (AppThemeState.isLightMode) Color(0x1FD49B0C) else Color(0x21F5C542)

val GoldAccentBorder: Color
    get() = if (AppThemeState.isLightMode) Color(0x59B47D08) else Color(0x4DF5C542)

val GoldAccentBorderStrong: Color
    get() = if (AppThemeState.isLightMode) Color(0x73B47D08) else Color(0x66F5C542)

val BorderHighlight: Color
    get() = if (AppThemeState.isLightMode) Color(0x66B47D08) else Color(0x59F5C542)

val OnGoldDark: Color
    get() = if (AppThemeState.isLightMode) Color(0xFFFFFFFF) else Color(0xFF171200)

val GoldHover: Color
    get() = if (AppThemeState.isLightMode) Color(0xFF966604) else Color(0xFFFFDA5D)

val GoldGlow: Color
    get() = if (AppThemeState.isLightMode) Color(0x80D49B0C) else Color(0xB3F5C542)

val BackgroundGlowColor: Color
    get() = if (AppThemeState.isLightMode) Color(0xFFF3E8CF) else Color(0xFF231D0E)

// Text & Content Hierarchy
val TextPrimary: Color
    get() = if (AppThemeState.isLightMode) Color(0xFF171921) else Color(0xFFF5F6FA)

val TextSecondary: Color
    get() = if (AppThemeState.isLightMode) Color(0xFF4A4F62) else Color(0xFFA6A9B8)

val TextMuted: Color
    get() = if (AppThemeState.isLightMode) Color(0xFF72778C) else Color(0xFF6E7182)

val TextSubtle: Color
    get() = if (AppThemeState.isLightMode) Color(0xFF5D6275) else Color(0xFF8A8E9E)

val TextDisabled: Color
    get() = if (AppThemeState.isLightMode) Color(0xFF9EA3B5) else Color(0xFF4D505E)

// Semantic / Learning Module Accents (Deepened in Light Mode for WCAG AA+ contrast)
val EmeraldAccent: Color
    get() = if (AppThemeState.isLightMode) Color(0xFF059669) else Color(0xFF34D399)

val EmeraldAccentBg: Color
    get() = if (AppThemeState.isLightMode) Color(0x1A059669) else Color(0x1F34D399)

val EmeraldAccentBorder: Color
    get() = if (AppThemeState.isLightMode) Color(0x4D059669) else Color(0x4734D399)

val SkyAccent: Color
    get() = if (AppThemeState.isLightMode) Color(0xFF0284C7) else Color(0xFF38BDF8)

val SkyAccentBg: Color
    get() = if (AppThemeState.isLightMode) Color(0x1A0284C7) else Color(0x1F38BDF8)

val SkyAccentBorder: Color
    get() = if (AppThemeState.isLightMode) Color(0x4D0284C7) else Color(0x4738BDF8)

val CoralAccent: Color
    get() = if (AppThemeState.isLightMode) Color(0xFFE11D48) else Color(0xFFFB7185)

val CoralAccentBg: Color
    get() = if (AppThemeState.isLightMode) Color(0x17E11D48) else Color(0x1CFB7185)

val CoralAccentBorder: Color
    get() = if (AppThemeState.isLightMode) Color(0x45E11D48) else Color(0x42FB7185)

val VioletAccent: Color
    get() = if (AppThemeState.isLightMode) Color(0xFF7C3AED) else Color(0xFFA78BFA)

val VioletAccentBg: Color
    get() = if (AppThemeState.isLightMode) Color(0x1A7C3AED) else Color(0x1FA78BFA)

val VioletAccentBorder: Color
    get() = if (AppThemeState.isLightMode) Color(0x4D7C3AED) else Color(0x47A78BFA)

val AmberAccent: Color
    get() = if (AppThemeState.isLightMode) Color(0xFFD97706) else Color(0xFFFBBF24)

val AmberAccentBg: Color
    get() = if (AppThemeState.isLightMode) Color(0x1AD97706) else Color(0x1FFBBF24)
