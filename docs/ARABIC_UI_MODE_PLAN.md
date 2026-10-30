# Arabic UI Mode (full RTL) - Implementation Plan for a Future Agent

> Goal: add an **optional, user-selectable "Arabic interface" mode**. Default stays exactly as today (English LTR UI, Arabic content aligned per text). Nothing changes for users who don't enable it.

## 0. Current state - READ FIRST (these were deliberately done and must be respected)
| Item | Where | Meaning |
|---|---|---|
| Layout forced LTR | `ui/theme/Theme.kt` -> `WordBooksTheme` provides `LocalLayoutDirection = Ltr` | App never mirrors with device language. **Must become conditional** (Step 3). |
| `android:supportsRtl="false"` | `AndroidManifest.xml` L28 | Must be `true` for Arabic mode (Step 3). |
| Global bidi text default | `Theme.kt`: `ProvideTextStyle(TextStyle(textDirection = Content))` | Each paragraph picks direction by first strong char. Keep. |
| Per-text direction | `BidiText`, `BiDiTextFormatter`, explicit `TextDirection.Ltr/Rtl` + `TextAlign.Left/Right` in ~35 places (Grammar, Flashcards, Quiz, Reading, Sentences, Database, Correction, Onboarding) | These are hand-tuned for content. **Do not blindly replace**. |
| Strings | `res/values/strings.xml` has only 9 nav strings; **743 hardcoded lines containing Arabic** across ~25 files; **351** usages of `*Ar` fields (`titleAr`, `messageAr`, `descriptionAr`...) | UI is bilingual by design: English text + Arabic companion. |
| Locale | `MainActivity` has no locale handling; only fontScale override in `attachBaseContext` | Needs per-app locale (Step 2). |
| Prefs | `OnboardingPreferencesRepository` (SharedPreferences, 33 refs), `SettingsScreen` has theme toggle (`AppThemeMode`) | Reuse for the language setting. |
| minSdk 24, compileSdk 36, `activity-compose 1.10.1` | `app/build.gradle.kts`, `libs.versions.toml` | `AppCompatDelegate` per-app locales needs `androidx.appcompat`; alternative below. |

## 1. Decide the model (do before coding)
Recommended: **"Interface language" setting = `English` (default) | `العربية`**.
- English: today's behavior unchanged.
- Arabic: layout direction = RTL (mirrored), UI chrome strings come from `values-ar`, and bilingual blocks show the **Arabic line primary**.
Keep learning content (English words, examples, grammar rules) LTR - only chrome mirrors.

## 2. Step-by-step

### Step 1 - Setting + state holder (small)
1. Add `enum class AppLanguage { ENGLISH, ARABIC }` in `ui/theme/` (or `data/prefs`).
2. Persist in the existing prefs repository (`OnboardingPreferencesRepository` or a new tiny `UiPreferencesRepository` with SharedPreferences key `ui_language`, default `ENGLISH`).
3. Expose `uiLanguage: StateFlow<AppLanguage>` + `setUiLanguage()` in `WordBooksViewModel`.
4. Add a row in `SettingsScreen` next to the theme toggle ("Interface language / لغة الواجهة").
**Accept:** toggling persists across restarts; default is English.

### Step 2 - Locale plumbing
Option A (preferred): add `androidx.appcompat:appcompat` and call `AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ar"|"en"))`; requires `MainActivity` to extend `AppCompatActivity` (verify `enableEdgeToEdge` + Compose still fine) and `<service AppLocalesMetadataHolderService>` autoStoreLocales for API<33.
Option B (no appcompat): override `attachBaseContext` (already overriding for fontScale) to wrap the context with `Configuration.setLocale(...)`, then `recreate()` on change. Keep fontScale override intact.
Either way: add `res/xml/locales_config.xml` (en, ar) + `android:localeConfig` in manifest.
**Accept:** changing the setting flips `resources.configuration.locale` and `recreate()`s without losing nav state (state is in ViewModel).

### Step 3 - Make layout direction conditional
1. `Theme.kt`: replace the hard `LocalLayoutDirection provides Ltr` with `if (language == ARABIC) Rtl else Ltr`. Read language via a parameter to `WordBooksTheme(uiLanguage = ...)` supplied from `MainActivity`/shell.
2. Manifest: `android:supportsRtl="true"`.
3. Keep `TextDirection.Content` default (neutral text then follows the layout direction automatically).
**Accept:** in Arabic mode `LocalLayoutDirection.current == Rtl` everywhere including Dialogs/ModalBottomSheet (they inherit from composition - verify each).

### Step 4 - Extract strings (the big step; do per screen, one PR each)
Order: Navigation/shell -> Settings -> Home -> Plan/PlanSetup -> Quiz dialog chrome -> Flashcards -> Word Books/Database -> Reading -> Grammar -> Sentences -> Correction Notebook -> Onboarding steps -> Labs.
Per screen:
1. Move **English UI chrome** literals (buttons, headings, hints, snackbars) into `values/strings.xml`; create the Arabic counterpart in `values-ar/strings.xml`. Use `stringResource(R.string.x)`.
2. For existing bilingual pairs (`titleEn/titleAr`, `labelAr`, `messageAr`...) add a helper:
   ```kotlin
   @Composable fun bi(en: String, ar: String) = if (LocalUiLanguage.current == ARABIC) ar else en
   ```
   (`LocalUiLanguage` = new CompositionLocal provided in `WordBooksTheme`.) Use it where text currently shows "En (Ar)" or both lines.
3. **Do not translate**: dictionary words, example sentences, grammar rule content, DB data, API/AI responses.
4. Track progress with a checklist table in `docs/ARABIC_UI_PROGRESS.md`. Use `grep -rn "[\u0600-\u06FF]" app/src/main/java --include=*.kt` to count remaining literals; target: Arabic literals only inside data/fallback strings.
**Accept (per screen):** no raw English chrome text remains; screen renders in both languages; no crash on language switch.

### Step 5 - Mirroring audit (visual)
Go through each item; fix with start/end or direction-aware code:
1. **Icons**: ~117 already `AutoMirrored`; check any `Icons.Filled.*` that is directional (play/forward/send/chevrons, custom drawables in `res/drawable`).
2. **Animations/gestures**: `NavigationRoutes.kt:162` (`slideIn/OutHorizontally`) and `FlashcardsScreen.kt:687` (swipe/horizontal gestures) -> multiply direction sign by `if (LocalLayoutDirection.current == Rtl) -1 else 1`. Swipe-right/left card semantics must still map to the same grading actions visually mirrored.
3. **Padding/alignment**: replace `padding(left/right)`, `Alignment.Left/Right`, absolute offsets with start/end variants (grep `\.padding\(.*(left|right)`, `offset(x`).
4. **Explicit `TextAlign.Left/Right` + `TextDirection.Ltr/Rtl`** (~50 sites): keep when the text is language-specific content (English example stays Left/LTR; Arabic stays Right/RTL). Change to `Start/End` only when it is UI chrome.
5. **Progress bars / sliders / charts / step indicators**: confirm fill direction mirrors (Material components do; custom `Canvas` drawings do not - check `AdaptiveProgressComponents.kt`, `HomeScreen.kt`).
6. **Bottom bar / tab order, back arrow side** in `WordBooksAppShell.kt`.
7. **Mixed text** (Arabic + English words): keep using `BidiText` / `BiDiTextFormatter` isolates.

### Step 6 - Inputs and special widgets
`OutlinedTextField`s (Reading 10, Grammar 7, Correction 5, Sentences 2, ApiKeyDialog 3): ensure cursor/hint/leading-trailing icons mirror; API key field must stay LTR (`TextDirection.Ltr`) in Arabic mode.

### Step 7 - Tests
1. Compose UI tests (`createComposeRule`) rendering Quiz, Flashcards, Grammar question card, Settings in `CompositionLocalProvider(LocalLayoutDirection provides Rtl)` - assert key nodes exist and no exceptions.
2. Unit test for the preference repository default (`ENGLISH`) and persistence.
3. Existing suite must still pass (`./gradlew :app:testDebugUnitTest --offline`; known unrelated failure: `LearningPathPrioritizationTest.testUserProgressAndChecklistIntegrity` expects `word_books` but code returns `new_words`).
4. Manual matrix: {English mode, Arabic mode} x {device en, device ar} x {Quiz, Flashcards, Grammar questions, Reading, Settings, Onboarding}. In English mode with device=ar, nothing may mirror (regression guard for the bug we fixed).

## 3. Definition of Done
- Default install: pixel-identical behavior to today, including with device language = Arabic.
- Arabic mode: all navigation/chrome mirrored and Arabic; English learning content (words, examples, grammar rules) remains LTR and readable.
- No hardcoded UI chrome strings left in migrated screens; both `values` and `values-ar` have the same keys (add lint `MissingTranslation` as error).
- Build, unit tests, and manual matrix pass.

## 4. Risks and guardrails
- Biggest risk: mirroring breaks hand-tuned per-text alignment -> work **per screen, behind the setting**, keep each PR small, screenshot before/after.
- Never mirror content that is inherently LTR (English sentences, code-like tokens, API keys, phonetics).
- Don't remove `ProvideTextStyle(TextDirection.Content)` or the per-text overrides.
- Dialog/bottom-sheet windows: verify they pick up `LocalLayoutDirection` (they do when created inside `WordBooksTheme`).
- Rollback is trivial: setting default is English; Step 3 conditional falls back to Ltr.

## 5. Suggested PR sequence
1. Setting + prefs + `LocalUiLanguage` (no visible change)
2. Locale plumbing + conditional layout direction + manifest
3. Strings: nav/shell + Settings
4-12. One PR per screen group (Step 4 order) including its mirroring audit
13. Inputs, tests, final manual matrix
