package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/**
 * Supported UI language modes for Qaf (قاف).
 * Default is [ENGLISH] so existing and new installs remain English-first LTR
 * regardless of the device OS locale unless the user opts into Arabic UI mode.
 */
enum class AppLanguage(val code: String, val titleEn: String, val titleAr: String) {
    ENGLISH("en", "English", "الإنجليزية"),
    ARABIC("ar", "Arabic", "العربية"),
    SYSTEM("system", "System Default", "تلقائي (حسب لغة الهاتف)");

    companion object {
        fun fromCode(code: String?): AppLanguage {
            return entries.firstOrNull { it.code.equals(code?.trim(), ignoreCase = true) } ?: ENGLISH
        }
    }
}

/**
 * Reactive global language state holder so layout direction and string resources
 * recompose smoothly across all screens when switching between English and Arabic.
 */
object AppLanguageState {
    var currentLanguage: AppLanguage by mutableStateOf(AppLanguage.ENGLISH)

    val isArabic: Boolean
        get() = when (currentLanguage) {
            AppLanguage.ARABIC -> true
            AppLanguage.ENGLISH -> false
            AppLanguage.SYSTEM -> {
                val sysLang = Locale.getDefault().language
                sysLang.equals("ar", ignoreCase = true)
            }
        }
}

val LocalUiLanguage = compositionLocalOf { AppLanguage.ENGLISH }

/**
 * Returns [ar] when the active UI language resolves to Arabic (and [ar] is non-blank),
 * otherwise [en].
 */
@Composable
fun bi(en: String, ar: String): String {
    val lang = LocalUiLanguage.current
    val useAr = when (lang) {
        AppLanguage.ARABIC -> true
        AppLanguage.ENGLISH -> false
        AppLanguage.SYSTEM -> Locale.getDefault().language.equals("ar", ignoreCase = true)
    }
    return if (useAr && ar.isNotBlank()) ar else en
}

/**
 * Non-composable bilingual helper reading [AppLanguageState.isArabic].
 */
fun biText(en: String, ar: String): String =
    if (AppLanguageState.isArabic && ar.isNotBlank()) ar else en
