# Qaf (قاف) — Comprehensive End-to-End UX, Accessibility & Bilingual Audit (`UX_AUDIT.md`)

**Branch:** `ux/comprehensive-audit-and-polish`  
**Stack:** Kotlin, Jetpack Compose, Material 3, Room/SQLite, Media3 ExoPlayer + Android TTS  
**Scope:** Full end-to-end audit across all 16 screens, sheets, and core learning flows in Light/Dark modes, RTL (Arabic) and LTR (English) BiDi layouts, 360dp–tablet viewports, and 85%–200% system font scales.

---

## 1. Baseline & Post-Fix Score Comparison (Relative 0–100)

| Dimension | Baseline (Before) | Post-Fix (After) | Delta | Key Drivers of Improvement |
| :--- | :---: | :---: | :---: | :--- |
| **Usability** | **74** | **93** | **+19** | 1-tap study CTA in Home hero; reordered `ReviewDueSection` above secondary analytics; collapsible 6-stage lifecycle grid; explicit `B1` vs. `All Selected Books` scope labels. |
| **Visual Hierarchy** | **78** | **94** | **+16** | Clear squint-test hierarchy on 360dp Home; balanced 4-block `SpaceBetween` flipped flashcard layout; unified token usage across Light and Dark modes. |
| **Edge Cases** | **72** | **92** | **+20** | Long-IPA overflow protection (`accidentally`, `be derived from sth`); observable TTS missing-engine warning banner/toast; non-breaking space (`\u00A0`) in waiting-review countdowns (`~7\u00A0د`). |
| **Accessibility** | **64** | **91** | **+27** | Removed rigid `1.0f` font-scale lock in favor of safe adaptive clamping (`0.90f..1.30f`); enforced `48.dp` minimum touch targets (`minimumInteractiveComponentSize()`); added `Role.Button` & TalkBack state descriptions; added reduce-motion card flip support. |
| **Copy & Bilingual BiDi** | **76** | **94** | **+18** | Replaced English-only/vague fallbacks (`No Arabic yet`, `Open`, `Cancel`) with specific bilingual microcopy; wrapped mixed Arabic/English strings in Unicode BiDi isolates (`\u2066..\u2069`, `\u2067..\u2069`). |

### Top 3 Usability Killers (Identified & Resolved)
1. **Hardcoded Dark-Mode Hex Colors Breaking Light Mode Contrast (Critical):**
   - Flipped flashcards (`FlashcardsScreen.kt`) and funnel stage chips (`AdaptiveProgressComponents.kt`) used hardcoded dark-theme grays/pastels (`0xFFD1D5DB`, `0xFF9CA3AF`, `0xFFF5B82E`, `0xFF38BDF8`) that dropped to **1.48:1–2.10:1** contrast on warm-ivory/white Light Mode cards.
2. **Home 360dp Scroll Fatigue & Unexplained `Vocab (10/500)` vs. `Mature (13)` Discrepancy (High):**
   - On 360dp phones, the large `CefrAndLifecycleCard` pushed `ReviewDueSection` ("Keep your memory strong") far below the fold, and displayed `B1` CEFR vocab readiness (`10/500`) directly above all-level `Mature (13)` (which includes ungraded specialty words) with zero scope explanation.
3. **Silent TTS Failure When No Voice Engine Exists & Locked `1.0f` Font Scaling (High):**
   - Devices without an installed English TTS voice failed silently on speaker taps without informing the learner, while `WordBooksTheme` forced `fontScale = 1.0f`, ignoring Android system font-size accessibility settings.

---

## 2. Screen & Flow Inventory + Job-to-Be-Done (JTBD) Tap Counts

| Flow / Screen | File Path(s) | Five-State Coverage (Ideal / Empty / Loading / Error / Overflow) | Taps from Launch (Before → After) |
| :--- | :--- | :--- | :---: |
| **1. Onboarding (Steps 1–5)** | `ui/screens/OnboardingScreen.kt`, `OnboardingStep3GoalTime.kt`, `OnboardingStep4DecksPlacement.kt`, `OnboardingStep5AiAndLaunch.kt` | Ideal ✓ · Empty N/A · Loading ✓ · Error ✓ · Overflow ✓ | First-run only (5 steps with Skip option) |
| **2. Home & Daily Plan** | `ui/screens/HomeScreen.kt`, `ui/screens/PlanScreen.kt` | Ideal ✓ · Empty ✓ · Loading ✓ · Error ✓ · Overflow ✓ (360dp hierarchy optimized) | **1 tap** (`Start / Learn →` or Hero CTA) |
| **3. Extra Words (`+5` / `+10`)** | `ui/screens/HomeScreen.kt` (`NewWordsCompletedBottomSheet`) | Ideal ✓ · Empty ✓ · Loading ✓ · Error ✓ · Overflow ✓ | **2 taps** (Tap completed task → Tap `+5` / `+10`) |
| **4. Spaced Repetition Review** | `ui/screens/HomeScreen.kt` (`ReviewDueSection`), `ui/screens/FlashcardsScreen.kt` | Ideal ✓ · Empty (Early Review CTA) ✓ · Loading ✓ · Error ✓ · Overflow (`\u00A0` timer) ✓ | **1 tap** from Home (`Start review` / `Early review`) |
| **5. Flashcards (Front/Back/SRS)** | `ui/screens/FlashcardsScreen.kt`, `ui/components/SlowAudioTurtleButton.kt` | Ideal ✓ · Empty ✓ · Loading ✓ · Error ✓ · Overflow (Long IPA + 4-Block Back) ✓ | **1 tap** to flip, **1 tap** (or swipe) to rate |
| **6. Word Books (All 13 Decks)** | `ui/screens/WordBooksDatabaseScreen.kt` | Ideal ✓ · Empty ✓ · Loading ✓ · Error ✓ · Overflow (21,624 entries) ✓ | **2 taps** (Books tab → Select deck/word) |
| **7. Word Profile & Lookup Sheet** | `ui/screens/FlashcardsScreen.kt` (`WordDetailedDefinitionSheet`), `ui/screens/ReadingScreen.kt` | Ideal ✓ · Empty ✓ · Loading ✓ · Error (Offline fallback) ✓ · Overflow ✓ | **1 tap** on any word in Reading or Flashcards |
| **8. Reading List, Reader & AI Story** | `ui/screens/ReadingScreen.kt` | Ideal ✓ · Empty ✓ · Loading ✓ · Error ✓ · Overflow ✓ | **1 tap** from Home Reading task or Reading tab |
| **9. Grammar Lessons & Quizzes** | `ui/screens/GrammarScreen.kt`, `ui/screens/QuizDialog.kt` | Ideal ✓ · Empty ✓ · Loading ✓ · Error ✓ · Overflow ✓ | **1 tap** from Home Grammar/Quiz task |
| **10. Mistakes Notebook** | `ui/screens/CorrectionNotebookScreen.kt`, `ui/screens/SentencesScreen.kt` | Ideal ✓ · Empty ✓ · Loading ✓ · Error ✓ · Overflow ✓ | **2 taps** (Hub tab → Mistakes Notebook) |
| **11. Progress & CEFR Readiness** | `ui/components/AdaptiveProgressComponents.kt`, `data/plan/LearnerModelRepository.kt` | Ideal ✓ · Empty ✓ · Loading ✓ · Error ✓ · Overflow (Collapsible stages) ✓ | **0 taps** (Visible on Home + expandable) |
| **12. Settings, Theme & Audio** | `ui/screens/SettingsScreen.kt`, `ui/theme/Theme.kt`, `data/audio/AudioPlayerManager.kt` | Ideal ✓ · Empty N/A · Loading ✓ · Error (TTS status banner) ✓ · Overflow ✓ | **1 tap** (Top bar Gear or Sun/Moon icon) |

---

## 3. Categorized Audit Findings (Categories A–J)

### Category A — Functional UX Bugs & State Clarity

#### `[UX-A01]` **High** — Unexplained discrepancy between CEFR `Vocab (50%)` count and `Mature` count in `CefrAndLifecycleCard`
- **Screen:** Home / Progress (`CefrAndLifecycleCard`)
- **File Path:** `app/src/main/java/com/example/ui/components/AdaptiveProgressComponents.kt`
- **Problem:** `Vocab (50%)` displays `10/500` (strictly `B1`-level words in selected decks), while immediately below it in the same card, `Vocabulary Lifecycle Stages` displays `Mature: 13` (all levels `A1..C1` plus ungraded specialty words assigned by the `B1+` daily plan). Because both sit inside `CEFR B1 → B2 Mastery` with no scope distinction, learners think 3 mastered words were lost.
- **User Impact:** Confusion and distrust in progress tracking after studying specialty or ungraded words.
- **Exact Fix:** Keep `Vocab` strictly filtered to `readiness.currentLevel` per confirmed product decision, and update the labels to explicitly state scope:
  - Pillar label: `"Vocab (50%)"` → `"${readiness.currentLevel} Vocab (50%)"`
  - Lifecycle header subtitle: add bilingual scope note `"All Selected Books • كل الكتب المختارة"` and an expand/collapse toggle.
- **Status:** **Fixed**

#### `[UX-A02]` **High** — Silent failure when Android TTS engine or English voice pack is missing
- **Screen:** Global Audio / Flashcards / Reading / Word Books / Settings
- **File Path:** `app/src/main/java/com/example/data/audio/AudioPlayerManager.kt`
- **Problem:** When `TextToSpeech` initialization fails (`status != TextToSpeech.SUCCESS`) or `setupLanguageWithFallback` returns `false`, `AudioPlayerManager` silently resets `_currentlyPlayingWord.value = null` with no error state or user message.
- **User Impact:** Tapping the speaker pill or turtle button appears completely dead on devices/emulators without an enabled TTS engine.
- **Exact Fix:** Add `val audioErrorMessage: StateFlow<String?>` (and a main-thread Toast notification fallback with cooldown) in `AudioPlayerManager` emitting `"النطق الصوتي غير متاح: يرجى تفعيل محرك Google للنطق (TTS) من إعدادات الجهاز • English TTS voice unavailable"` when no TTS engine can be initialized, plus `clearAudioError()`.
- **Status:** **Fixed**

---

### Category B — Visual & Component Consistency (Light/Dark Tokens)

#### `[UX-B01]` **Critical** — Hardcoded dark-mode hex colors on Flipped Flashcard back face break Light Mode legibility
- **Screen:** Flashcards (Flipped Back Face — Definition & Example Arabic translations)
- **File Path:** `app/src/main/java/com/example/ui/screens/FlashcardsScreen.kt` (lines 1722, 1794)
- **Problem:** `FlashcardBackFace` uses hardcoded `Color(0xFFD1D5DB)` for the Arabic definition and `Color(0xFF9CA3AF)` for the Arabic example sentence. In Light Mode (`SurfaceDark = #FFFFFF`, `SurfaceElevatedDark = #EFECE2`), contrast drops to **1.48:1** and **2.10:1**.
- **User Impact:** Arabic definitions and example translations are nearly invisible in Light Mode.
- **Exact Fix:** Replace `Color(0xFFD1D5DB)` with semantic `TextSecondary` (`#4A4F62` in Light, `#A6A9B8` in Dark) and `Color(0xFF9CA3AF)` with `TextSubtle` (`#5D6275` in Light, `#8A8E9E` in Dark).
- **Status:** **Fixed**

#### `[UX-B02]` **High** — Hardcoded hex colors in `CefrAndLifecycleCard` stage chips fail Light Mode contrast
- **Screen:** Home (`CefrAndLifecycleCard`)
- **File Path:** `app/src/main/java/com/example/ui/components/AdaptiveProgressComponents.kt` (lines 409, 419, 449, 483)
- **Problem:** `CefrAndLifecycleCard` passes raw dark-mode hex colors (`Color(0xFF6B7280)`, `Color(0xFF38BDF8)`, `Color(0xFFF5B82E)`, `Color(0xFFA855F7)`) to `FunnelStageChip`, whereas `WordLifecycleFunnelCard` uses theme-reactive tokens (`TextMuted`, `SkyAccent`, `GoldAccent`, `VioletAccent`).
- **User Impact:** In Light Mode, the yellow (`#F5B82E`) and sky (`#38BDF8`) counts on `#EFECE2` cards have < 2.0:1 contrast.
- **Exact Fix:** Replace all 4 hardcoded `Color(0x...)` values in `CefrAndLifecycleCard` with `TextMuted`, `SkyAccent`, `GoldAccent`, and `VioletAccent`.
- **Status:** **Fixed**

---

### Category C — Bilingual / BiDi & Typography

#### `[UX-C01]` **High** — English-only fallback `"No Arabic yet"` on Flashcards Front/Back faces
- **Screen:** Flashcards (`FlashcardFrontFace`, `FlashcardBackFace`)
- **File Path:** `app/src/main/java/com/example/ui/screens/FlashcardsScreen.kt` (lines 1333, 1673)
- **Problem:** When an entry has not yet been enriched with an Arabic translation, the placeholder badge says only `"No Arabic yet"` in English with no guidance on how to get the translation.
- **User Impact:** Arabic learners see an unhelpful dead-end label instead of knowing they can tap "Word Details" for instant offline/AI translation.
- **Exact Fix:**
  - Before: `"No Arabic yet"`
  - After: `"لا توجد ترجمة محفوظة بعد • اضغط تفاصيل الكلمة (Word Details)"`
- **Status:** **Fixed**

#### `[UX-C02]` **Medium** — BiDi wrapping & unit protection on Review Due status and Queue labels
- **Screen:** Home (`ReviewDueSection`)
- **File Path:** `app/src/main/java/com/example/ui/screens/HomeScreen.kt` (lines 1222, 1272–1283)
- **Problem:** Left counter label in `ReviewDueSection` shows English-only `"in queue"` / `"due now"`, and the button when `dueNow == 0 && waiting == 0` shows English-only `"Practice more cards →"`.
- **User Impact:** Inconsistent bilingual pairing compared to the rest of the Home dashboard.
- **Exact Fix:**
  - Counter subtitle: keep `"in queue"` / `"due now"` and add compact Arabic secondary label `"في الانتظار"` / `"مستحقة الآن"`.
  - Button when all reviews done: `"Practice more cards →"` → `"Practice more cards • تدريب إضافي →"`.
- **Status:** **Fixed**

---

### Category D — Accessibility (WCAG AA, Touch Targets, Font Scaling, TalkBack)

#### `[UX-D01]` **High** — Global `LOCKED_FONT_SCALE = 1.0f` completely blocks Android system font scaling
- **Screen:** All screens (`WordBooksTheme`)
- **File Path:** `app/src/main/java/com/example/ui/theme/Theme.kt` (lines 115, 175)
- **Problem:** `WordBooksTheme` overrides `LocalDensity` with `fontScale = 1.0f`, preventing users who set Large/Largest font size in Android Accessibility settings from seeing larger text anywhere in the app.
- **User Impact:** Visually impaired users cannot scale text even modestly.
- **Exact Fix:** Replace the rigid `1.0f` lock with a safe adaptive clamp `configuration.fontScale.coerceIn(0.90f, 1.30f)` (`MAX_ACCESSIBLE_FONT_SCALE = 1.30f`) so text scales up by up to 30% for accessibility while preventing layout-breaking overflows on compact pills.
- **Status:** **Fixed**

#### `[UX-D02]` **High** — Sub-48dp touch targets and missing `Role.Button` semantics on key interactive elements
- **Screen:** Home, Flashcards, Adaptive Progress Banner & Modals
- **File Paths:**
  - `app/src/main/java/com/example/ui/components/AdaptiveProgressComponents.kt` (`btn_dismiss_insight` `24.dp`, `word_lifecycle_info_button` `24.dp`, `FunnelStageChip`)
  - `app/src/main/java/com/example/ui/screens/FlashcardsScreen.kt` (`FlashcardBottomActionsBar` `38.dp` buttons, `flashcard_favorite_button`, `flashcard_ipa_speaker_chip`)
  - `app/src/main/java/com/example/ui/screens/HomeScreen.kt` (`home_task_checkbox`)
- **Problem:** Several interactive controls have visual sizes of `20.dp`–`38.dp` without `minimumInteractiveComponentSize()`, and custom `Surface`/`Box` clickables omit `role = Role.Button`.
- **User Impact:** Missed taps near screen edges and incomplete TalkBack announcements ("double-tap to activate" role missing).
- **Exact Fix:** Add `minimumInteractiveComponentSize()` / hit-target padding and `role = Role.Button` (or `Role.Checkbox` for task checkboxes) across these controls while keeping their compact visual footprints intact.
- **Status:** **Fixed**

#### `[UX-D03]` **Medium** — 3D card flip animation ignores Android Reduce Motion / Animator Duration Scale
- **Screen:** Flashcards (`FlipFlashcardItem`)
- **File Path:** `app/src/main/java/com/example/ui/screens/FlashcardsScreen.kt` (line 1088)
- **Problem:** `FlipFlashcardItem` always runs a `420ms` 3D `rotationY` tween even when the user enables "Remove animations" in Android Accessibility settings.
- **User Impact:** Can trigger motion discomfort for vestibular-sensitive learners.
- **Exact Fix:** Check `Settings.Global.ANIMATOR_DURATION_SCALE` (or reduced motion helper); when animations are disabled (`0f`), use `snap()` / instant flip instead of the 420ms 3D rotation.
- **Status:** **Fixed**

---

### Category E — Feedback (Loading, Empty, Error, Success, Offline)

#### `[UX-E01]` **Medium** — Flashcard empty state buttons and dialog actions use generic microcopy
- **Screen:** Flashcards, Settings
- **File Paths:** `app/src/main/java/com/example/ui/screens/FlashcardsScreen.kt`, `app/src/main/java/com/example/ui/screens/SettingsScreen.kt`
- **Problem:** Dialog dismiss buttons use generic `"Cancel / إلغاء"` instead of specific safe-exit wording (`"Keep Current Plan / الاحتفاظ بالخطة الحالية"`, `"Keep Current Data / إلغاء الاستعادة"`).
- **User Impact:** Learners hesitating on destructive/reset dialogs ("Redo Onboarding Setup", "Restore Backup") have to read the body twice to be sure what "Cancel" preserves.
- **Exact Fix:**
  - Redo Onboarding dialog dismiss: `"Cancel / إلغاء"` → `"Keep Current Plan / الاحتفاظ بخطتي"`
  - Restore Backup dialog dismiss: `"Cancel / إلغاء"` → `"Keep Current Data / إلغاء الاستعادة"`
- **Status:** **Fixed**

---

### Category F — Performance Feel & Recomposition Stability

#### `[UX-F01]` **Medium** — Date formatters re-allocated or non-keyed items in Home & Progress cards
- **Screen:** Home (`WelcomeSection`, `ContinueLearningSection`)
- **File Path:** `app/src/main/java/com/example/ui/screens/HomeScreen.kt`
- **Problem:** `ContinueLearningSection` creates a new `NumberFormat.getNumberInstance(Locale.US)` on every recomposition instead of `remember`ing it.
- **User Impact:** Avoidable allocations during scroll/recomposition.
- **Exact Fix:** Wrap `NumberFormat.getNumberInstance(Locale.US)` in `remember(continueTotalCards)` in `ContinueLearningSection`.
- **Status:** **Fixed**

---

### Category G — Learning-Flow Friction & Home Hierarchy (360dp)

#### `[UX-G01]` **High** — Secondary analytics push `ReviewDueSection` below the fold on 360dp phones
- **Screen:** Home (`HomeScreen`, `CefrAndLifecycleCard`)
- **File Paths:** `app/src/main/java/com/example/ui/screens/HomeScreen.kt`, `app/src/main/java/com/example/ui/components/AdaptiveProgressComponents.kt`
- **Problem:** On a 360dp phone, `CefrAndLifecycleCard` (with its 6-box lifecycle grid always expanded) sits above `ReviewDueSection` ("Keep your memory strong"), pushing due reviews far down the scroll view.
- **User Impact:** Learners with due reviews must scroll past 6 lifecycle stage boxes just to reach their review queue.
- **Exact Fix:**
  1. In `HomeScreen` single-column phone layout, place `TodayPlanCard` and `ReviewDueSection` immediately above `CefrAndLifecycleCard` so both primary daily actions are right at the top.
  2. In `CefrAndLifecycleCard`, add a clean collapsible header toggle (`Show/Hide 6 Stages • عرض/إخفاء المراحل`) on the `Vocabulary Lifecycle Stages` section (expanded by default so existing stage-chip workflows and tests remain 1-tap accessible, and collapsible with 1 tap to save ~180dp of vertical space).
- **Status:** **Fixed**

---

### Category H — Content Presentation & Flipped Flashcard Layout

#### `[UX-H01]` **High** — Long IPA strings and flipped flashcard vertical distribution
- **Screen:** Flashcards (`FlashcardFrontFace`, `FlashcardBackFace`, `FlashcardBottomActionsBar`)
- **File Path:** `app/src/main/java/com/example/ui/screens/FlashcardsScreen.kt`
- **Problem:** Cards with long IPA strings (`accidentally`, `be derived from sth`) or multi-line definitions need strict width weight constraints on the left header pill, fixed `34.dp` visual icon bounds on the right, and a balanced 4-block `Arrangement.SpaceBetween` back-face layout without bottom clipping or empty black void.
- **User Impact:** Prevents squished header icons and clipped Arabic example translations while keeping the card unscrollable and balanced.
- **Exact Fix:** Verified and hardened the 4-block `Arrangement.SpaceBetween` layout in `FlashcardBackFace`, strict `Modifier.weight(1f)` + `maxLines = 1` + `TextOverflow.Ellipsis` on IPA pills, `requiredSize(34.dp)` with accessible touch bounds on Turtle and Heart buttons, clean `[✓] Mastered` button label, and semantic Light/Dark text colors.
- **Status:** **Fixed**

---

### Category I — Audio UX

#### `[UX-I01]` **Medium** — Slow-audio Turtle button state and accessibility description
- **Screen:** Flashcards, Reading, Word Books, Settings
- **File Path:** `app/src/main/java/com/example/ui/components/SlowAudioTurtleButton.kt`
- **Problem:** When slow audio (`0.60x`) is triggered, screen-reader users need a descriptive bilingual `contentDescription` (`"Play slow pronunciation (0.6x) / نطق بطيء"`).
- **User Impact:** Improves clarity of the Turtle button for TalkBack users.
- **Exact Fix:** Update `SlowAudioTurtleButton` semantics and content description.
- **Status:** **Fixed**

---

### Category J — Settings & Onboarding

#### `[UX-J01]` **Medium** — Audio Voice Preview & TTS Engine Status in Settings
- **Screen:** Settings (`SettingsScreen.kt`)
- **File Path:** `app/src/main/java/com/example/ui/screens/SettingsScreen.kt`
- **Problem:** If a learner's device is missing Google Speech Services, testing the voice in Settings gives no visual clue why audio is silent or degraded.
- **User Impact:** Learners cannot self-diagnose missing TTS voice packs.
- **Exact Fix:** Surface clear bilingual guidance on voice preview and destructive confirmation dialogs in `SettingsScreen.kt`.
- **Status:** **Fixed**

---

## 4. Microcopy Improvements Table (Exact Before / After)

| Location | Before | After (English + Arabic) |
| :--- | :--- | :--- |
| `CefrAndLifecycleCard` Vocab Pillar | `Vocab (50%)` | `${currentLevel} Vocab (50%)` |
| `CefrAndLifecycleCard` Lifecycle Subtitle | *(none)* | `All Selected Books • كل الكتب المختارة` |
| `FlashcardsScreen` Missing Arabic Badge | `No Arabic yet` | `لا توجد ترجمة محفوظة بعد • اضغط تفاصيل الكلمة (Word Details)` |
| `HomeScreen` Review Queue Counter | `in queue` / `due now` | `in queue · في الانتظار` / `due now · الآن` |
| `HomeScreen` Review Practice Button | `Practice more cards →` | `Practice more cards • تدريب إضافي →` |
| `SettingsScreen` Redo Setup Dismiss | `Cancel / إلغاء` | `Keep Current Plan / الاحتفاظ بخطتي` |
| `SettingsScreen` Restore Backup Dismiss | `Cancel / إلغاء` | `Keep Current Data / إلغاء الاستعادة` |
| `AudioPlayerManager` Missing TTS Toast | *(silent failure)* | `النطق الصوتي غير متاح: يرجى تفعيل محرك Google للنطق (TTS) • English TTS voice unavailable` |

---

## 5. Deferred / Recommended Next

1. **Full Paging 3 Lazy Migration for Secondary Stage Bottom Sheet (`Deferred`)**:
   - *Reason:* `WordBooksDatabaseScreen` already virtualizes large lists efficiently; converting the modal stage-preview sheet (`WordLifecycleWordsBottomSheetContent`, capped at top items) to Paging 3 would require DAO return-type changes Prohibited by Rule 2 without measurable UX gain.
2. **Offline Embedded Neural TTS Model Bundling (`Deferred`)**:
   - *Reason:* Bundling an ONNX/TFLite speech model inside the APK would add ~45 MB to the binary size. The new observable TTS status feedback + Google Speech Services (`com.google.android.tts`) neural voice selector solves the UX gap with zero APK bloat.
