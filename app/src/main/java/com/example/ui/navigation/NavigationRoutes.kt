package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Spellcheck
import androidx.compose.material.icons.outlined.Style
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

/**
 * Type-safe navigation destinations for WordBooks.
 */
@Serializable
sealed interface AppRoute {
    @Serializable
    data object Home : AppRoute

    @Serializable
    data object WordBooks : AppRoute

    @Serializable
    data object Flashcards : AppRoute

    @Serializable
    data object Sentences : AppRoute

    @Serializable
    data object Grammar : AppRoute

    @Serializable
    data object Reading : AppRoute

    @Serializable
    data object Plan : AppRoute

    @Serializable
    data object Labs : AppRoute

    @Serializable
    data object CorrectionNotebook : AppRoute

    @Serializable
    data object More : AppRoute
}

enum class BottomNavDestination(
    val route: AppRoute,
    val titleEn: String,
    val shortLabel: String,
    val titleAr: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    HOME(
        route = AppRoute.Home,
        titleEn = "Home",
        shortLabel = "Home",
        titleAr = "الرئيسية",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_home"
    ),
    WORD_BOOKS(
        route = AppRoute.WordBooks,
        titleEn = "Word Books",
        shortLabel = "Books",
        titleAr = "الكتب",
        selectedIcon = Icons.AutoMirrored.Filled.MenuBook,
        unselectedIcon = Icons.AutoMirrored.Outlined.MenuBook,
        testTag = "nav_word_books"
    ),
    GRAMMAR(
        route = AppRoute.Grammar,
        titleEn = "Grammar",
        shortLabel = "Grammar",
        titleAr = "القواعد",
        selectedIcon = Icons.Filled.Spellcheck,
        unselectedIcon = Icons.Outlined.Spellcheck,
        testTag = "nav_grammar"
    ),
    READING(
        route = AppRoute.Reading,
        titleEn = "Graded Reading",
        shortLabel = "Reading",
        titleAr = "القراءة",
        selectedIcon = Icons.Filled.Description,
        unselectedIcon = Icons.Outlined.Description,
        testTag = "nav_reading"
    ),
    MORE(
        route = AppRoute.More,
        titleEn = "Learning Hub",
        shortLabel = "Hub",
        titleAr = "المزيد",
        selectedIcon = Icons.Filled.GridView,
        unselectedIcon = Icons.Outlined.GridView,
        testTag = "nav_more"
    )
}

enum class MainSection(
    val route: AppRoute,
    val titleEn: String,
    val shortLabel: String,
    val titleAr: String,
    val subtitleEn: String,
    val descriptionAr: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeTag: String,
    val testTag: String
) {
    HOME(
        route = AppRoute.Home,
        titleEn = "Home",
        shortLabel = "Home",
        titleAr = "الرئيسية • متابعة التعلم اليومي",
        subtitleEn = "Daily goal, streak, review due today, continue learning, and Featured Word Books.",
        descriptionAr = "هدفك اليومي، سلسلة الأيام المتتالية، الكلمات المستحقة للمراجعة اليوم، وأبرز كتب الكلمات.",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        badgeTag = "Section 1 / 7",
        testTag = "nav_home"
    ),
    WORD_BOOKS(
        route = AppRoute.WordBooks,
        titleEn = "Word Books",
        shortLabel = "Books",
        titleAr = "كتب الكلمات • مكتبة المفردات",
        subtitleEn = "Library of colored book covers (9,862 entries), US IPA audio chips, Arabic POS meanings, filters & search.",
        descriptionAr = "مكتبة البطاقات والكتب الملونة (Oxford 5000 وغيرها) مع النطق الصوتي الأمريكي IPA والمعاني العربية المصنفة.",
        selectedIcon = Icons.AutoMirrored.Filled.MenuBook,
        unselectedIcon = Icons.AutoMirrored.Outlined.MenuBook,
        badgeTag = "Section 2 / 7",
        testTag = "nav_word_books"
    ),
    FLASHCARDS(
        route = AppRoute.Flashcards,
        titleEn = "Flashcards",
        shortLabel = "Cards",
        titleAr = "البطاقات التعليمية • التكرار المتباعد",
        subtitleEn = "Swipeable study cards with gold '1 / N' counter, highlighted examples, AI Definition sheet, and SRS (Again / Hard / Good / Easy).",
        descriptionAr = "دراسة الكلمات بالسحب مع عداد ذهبي، أمثلة سياقية ملونة، شرح ذكي مفصل، ونظام التكرار المتباعد SRS.",
        selectedIcon = Icons.Filled.Style,
        unselectedIcon = Icons.Outlined.Style,
        badgeTag = "Section 3 / 7",
        testTag = "nav_flashcards"
    ),
    SENTENCES(
        route = AppRoute.Sentences,
        titleEn = "Sentences",
        shortLabel = "Sentences",
        titleAr = "الجمل المحفوظة • الإملاء والتحدث",
        subtitleEn = "Saved sentences with Favorites, Dictation, and Speaking tabs, translation toggle, audio playback, and AI explain.",
        descriptionAr = "قائمة الجمل المحفوظة مع تبويبات المفضلة والإملاء والتحدث، إظهار/إخفاء الترجمة، والنطق والشرح بالذكاء الاصطناعي.",
        selectedIcon = Icons.Filled.FormatQuote,
        unselectedIcon = Icons.Outlined.FormatQuote,
        badgeTag = "Section 4 / 7",
        testTag = "nav_sentences"
    ),
    GRAMMAR(
        route = AppRoute.Grammar,
        titleEn = "Grammar",
        shortLabel = "Grammar",
        titleAr = "القواعد النحوية • مستويات A1 إلى C1",
        subtitleEn = "CEFR-graded grammar lessons explained in simple Arabic with bilingual examples and interactive exercises.",
        descriptionAr = "دروس القواعد مرتبة حسب المستوى (A1-C1) والموضوع مع شرح عربي مبسط وأمثلة وتمارين تفاعلية.",
        selectedIcon = Icons.Filled.Spellcheck,
        unselectedIcon = Icons.Outlined.Spellcheck,
        badgeTag = "Section 5 / 7",
        testTag = "nav_grammar"
    ),
    READING(
        route = AppRoute.Reading,
        titleEn = "Reading",
        shortLabel = "Reading",
        titleAr = "القراءة المتدرجة • نصوص تفاعلية",
        subtitleEn = "Graded texts (A1–C1) where tapping any word reveals its Arabic meaning, IPA, and audio, plus comprehension quizzes.",
        descriptionAr = "نصوص قراءة متدرجة المستوى؛ اضغط على أي كلمة لمعرفة معناها ونطقها وإضافتها لمفرداتك مع أسئلة الفهم.",
        selectedIcon = Icons.Filled.Description,
        unselectedIcon = Icons.Outlined.Description,
        badgeTag = "Section 6 / 7",
        testTag = "nav_reading"
    ),
    PLAN(
        route = AppRoute.Plan,
        titleEn = "Study Plan",
        shortLabel = "Plan",
        titleAr = "خطة التعلم • خطتك اليومية والشهرية",
        subtitleEn = "Monthly goals, daily adaptive checklist, multi-skill quizzes, progress calendar, and streak.",
        descriptionAr = "خطة دراسية شهرية ويومية ذكية تتكيف مع أدائك وتتبع تقدمك عبر التكرار المتباعد والاختبارات التفاعلية.",
        selectedIcon = Icons.AutoMirrored.Filled.EventNote,
        unselectedIcon = Icons.AutoMirrored.Outlined.EventNote,
        badgeTag = "Section 7 / 7",
        testTag = "nav_plan"
    )
}
