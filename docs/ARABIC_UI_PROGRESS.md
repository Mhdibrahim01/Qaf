# Arabic UI Mode (Full RTL) Implementation Status

## 1. Architecture & Language State
- **[`Language.kt`](../app/src/main/java/com/example/ui/theme/Language.kt)**:
  - `AppLanguage`: `ENGLISH("en")`, `ARABIC("ar")`, `SYSTEM("system")`.
  - Default language in `AppLanguage.fromCode(...)` and `AppLanguageState.currentLanguage` is **`AppLanguage.ENGLISH`** so existing and new installs stay English-first LTR even on Arabic OS devices until explicitly switched in Settings.
  - Provides `LocalUiLanguage`, `@Composable fun bi(en: String, ar: String)`, and `fun biText(en: String, ar: String)`.
- **[`OnboardingPreferencesRepository.kt`](../app/src/main/java/com/example/data/onboarding/OnboardingPreferencesRepository.kt)**:
  - Persists `app_language` in DataStore (`KEY_APP_LANGUAGE`), defaulting to `"en"`.
- **[`WordBooksViewModel.kt`](../app/src/main/java/com/example/ui/viewmodel/WordBooksViewModel.kt)**:
  - Exposes `appLanguage: StateFlow<AppLanguage>` and `setAppLanguage(language: AppLanguage)`, syncing `AppLanguageState.currentLanguage`.
- **[`Theme.kt`](../app/src/main/java/com/example/ui/theme/Theme.kt)**:
  - Injects `LocalLayoutDirection` (`LayoutDirection.Rtl` when `AppLanguageState.isArabic` is true, else `LayoutDirection.Ltr`), `LocalUiLanguage`, and locale-bound `LocalContext` / `LocalConfiguration`.
  - Preserves global `ProvideTextStyle(TextStyle(textDirection = TextDirection.Content))` for automatic per-paragraph Bidi detection.
- **[`locales_config.xml`](../app/src/main/res/xml/locales_config.xml) & [`AndroidManifest.xml`](../app/src/main/AndroidManifest.xml)**:
  - Declares `en` and `ar` locales and `android:supportsRtl="true"`.

---

## 2. Screen-by-Screen RTL & LTR Guardrails

| Screen / Component | UI Chrome Status | English Content LTR Guardrails |
|---|---|---|
| **App Shell & Navigation** (`WordBooksAppShell.kt`, `WordBooksDesignComponents.kt`) | Localized top bar titles, subtitles, bottom navigation, and navigation rail (`الرئيسية`, `الخطة`, `البطاقات`, `القاموس`, `المزيد`) | AutoMirrored back/navigation icons mirror automatically in RTL |
| **Settings** (`SettingsScreen.kt`) | Interface Language selector (`English`, `العربية`, `System / لغة الجهاز`) with instant recomposition (`btn_lang_en`, `btn_lang_ar`, `btn_lang_system`) | Version and technical identifiers remain readable |
| **Home & Study Plan** (`HomeScreen.kt`, `PlanScreen.kt`) | Promotes `task.titleAr` to primary heading when Arabic UI mode is active | CEFR badges (`A1`–`C2`) and numeric progress pills stay intact |
| **Flashcards** (`FlashcardsScreen.kt`) | Localized flip hint, Mastered/Word Details buttons, and SM-2 rating buttons (`نسيتها`, `صعبة`, `جيدة`, `سهلة`) | Headword, IPA (`/.../`), and English example sentences remain LTR |
| **Grammar** (`GrammarScreen.kt`) | Localized exercise type badges (`اختيار من متعدد`, `أكمل الفراغ`, `صحح الخطأ`, `رتب الجملة`) | `GrammarExercisePrompt` fill-in-the-blank `FlowRow` and `ExerciseItemView` (`mcq`, `fill`, `correct`, `reorder`) wrapped in `LayoutDirection.Ltr` |
| **Daily & Weekly Quiz** (`QuizDialog.kt`) | Localized progress header, action buttons (`تحقق من الإجابة`, `التالي`, `إنهاء وحفظ`, `إغلاق`), and missed-word review CTA | Placed-word sentence box `FlowRow` and word bank chips `FlowRow` wrapped in `LayoutDirection.Ltr`; options rendered with `BidiText` |
| **Reading** (`ReadingScreen.kt`) | Localized headers and Arabic translation toggles | Story sentence token `FlowRow` and English comprehension MCQ option rows wrapped in `LayoutDirection.Ltr` |
| **Sentences** (`SentencesScreen.kt`) | Bilingual tabs (`المفضلة`, `الإملاء`, `التحدث`) | Dictation input field, Quiet Mode word tiles `FlowRow`, assembled sentence input field, and `SentenceDiffResultCard` token `FlowRow` wrapped in `LayoutDirection.Ltr` |
| **API Key Dialog** (`ApiKeyDialog.kt`) | Bilingual status & instructions | `input_custom_gemini_api_key` `OutlinedTextField` wrapped in `LayoutDirection.Ltr` with `TextDirection.Ltr` |
