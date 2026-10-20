# Single Source of Truth (SSOT) for Cross-Device UI & Data Parity

Establish a unified Single Source of Truth architecture that guarantees pixel-consistent rendering, deterministic bilingual typography, and synchronized AI response formatting between the AI Studio preview emulator and physical Android devices.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions have been confirmed and will govern the implementation across the theme engine, typography pipeline, and local persistence layer:

- **Confirmed Decision 1 — Locked `1.0x` Font Scale & Normalized Viewport Density**: Override the Compose `LocalDensity` at the root theme level so `fontScale = 1.0f` is strictly enforced and narrow OEM display zoom modes (`360dp`–`384dp`) scale proportionally to a canonical `411dp` reference width.
- **Confirmed Decision 2 — Three-Pillar SSOT Scope**: Unify (1) UI Design System & Layout Tokens, (2) Bundled Offline Fonts for English and Arabic, and (3) AI Prompt Formatting & Cache Versioning under single authoritative definitions.
- **Confirmed Decision 3 — Automatic AI Cache Invalidation on Version Bump**: Embed a centralized `PROMPT_SCHEMA_VERSION` token into AI cache keys and execute an automatic startup purge/sanitization of legacy unversioned or markdown-polluted cache entries when installing an updated APK, without losing user flashcard or quiz progress.

---

## 1. Overview & Core Concept

- **What It Does**: Eliminates visual drift and stale data discrepancies when moving from the browser preview emulator to a real phone. Buttons, cards, bilingual text blocks, and AI dictionary explanations render with identical line breaks, font weights, colors, and clean formatting regardless of OEM skin (Samsung One UI, Xiaomi HyperOS, Pixel) or previous APK installations.
- **Target Audience / Persona**: Arabic-speaking English learners who test and iterate in AI Studio and install updated APKs on their personal phones for daily study.
- **Key Value**: True "What You See Is What You Get" (WYSIWYG) parity between development preview and physical device, zero button text wrapping/truncation, and instant clean-up of legacy cached AI outputs upon app update.

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **App Launch & Automatic Parity Sync**: When the user installs a new APK over an existing installation on their phone, the startup initializer checks the active `SSOT_DATA_VERSION`. If bumped, it automatically sanitizes and invalidates stale AI explanations (removing legacy conversational fluff or `**` markdown asterisks) while keeping all mastered words, streaks, and study plans intact.
  2. **Deterministic Screen Rendering**: As the user navigates across Home, Word Books, Flashcards, Grammar, and Reading, the root `DensityNormalizer` locks `fontScale` to `1.0f` and normalizes horizontal density so action rows (such as *"Word Details"* and *"Mark Mastered"*) never wrap or clip on high-zoom phones.
  3. **Bilingual Text & AI Explanation Display**: Every AI-generated explanation passes through a single formatting and sanitization pipeline before storage and again at render time via `BidiText`, guaranteeing clean `•` bullet points and isolated LTR/RTL runs.

- **Visual Identity & Theme**:
  - *Aesthetic Direction*: High-craft dark editorial lexicon UI with deterministic spatial math.
  - *Color Palette & Mood*: Locked dark palette (`#0B0C10` background, `#13151B` elevated surface, `#232733` hairline borders, `#F2C94C` gold primary accent, `#38BDF8` sky accent, `#10B981` emerald mastery state) with Android 12+ dynamic wallpaper color overrides strictly disabled.
  - *Typography & Hierarchy*: Bundled static `.ttf` fonts only (`Inter` for Latin/IPA, `Cairo` for Arabic headings/labels, `Tajawal` for Arabic body copy) with explicit `lineHeight` and `PlatformTextStyle(includeFontPadding = false)` to eliminate OEM font metric padding differences.
  - *Component Styling & Layout*: Centralized spacing tokens (`WordBooksSpacing`), shape tokens (`SquircleCardShape`, `PillShape`), and single-line button constraints with responsive horizontal padding.

- **Interactive Feedback & Motion**: Smooth 60/120 FPS lazy list scrolling with stable keys, zero allocation inside scroll passes, and instant visual state updates when toggling mastery or opening word details.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Root `CompositionLocalProvider(LocalDensity)` Normalization**
  - *Chosen Approach*: Wrap the root theme content in a custom `LocalDensity` provider that sets `fontScale = 1.0f` and computes a normalized density scale when a phone's logical width is narrower than `392dp` due to OS Display Zoom.
  - *Why*: Physical phones frequently ship with Display Zoom set to "Large" or Font Size set to `115%–130%`, causing 2-button and 3-button rows to wrap into 2 lines even when they look fine on the `411dp` emulator.
  - *Alternatives Considered*: Per-button `autoSize` text was considered, but normalizing at the root `LocalDensity` level fixes every screen, badge, chip, and card simultaneously from a Single Source of Truth.

- **Decision 2: Strict Font Metric Locking (`includeFontPadding = false` + Bundled Fonts)**
  - *Chosen Approach*: Enforce `PlatformTextStyle(includeFontPadding = false)` across all English (`Typography`) and Arabic (`ArabicTypography`) styles in the central theme, backed exclusively by bundled `res/font/` resources.
  - *Why*: Samsung and Xiaomi phones inject extra top/bottom ascent padding into Arabic system fallback fonts, shifting vertical alignment inside pills and buttons. Bundled fonts with `includeFontPadding = false` guarantee identical vertical centering on all devices.
  - *Alternatives Considered*: Relying on `FontFamily.Default` or downloadable fonts was rejected because OEM system fonts vary wildly across Android vendors.

- **Decision 3: Versioned AI Prompt Contract & Startup Cache Sanitizer**
  - *Chosen Approach*: Create a single `AiPromptContract` object containing the canonical formatting rules, output sanitizer (`sanitizeAiPlainText`), and `PROMPT_SCHEMA_VERSION = 3`. Prefix all `ai_cache` lookup keys with `v3_` and run a one-time startup migration that strips `**`, `###`, and conversational greetings from any previously saved local database rows.
  - *Why*: Updating a prompt in code only affects *new* API calls unless existing cached entries on the phone's Room database are invalidated or sanitized on app upgrade.
  - *Alternatives Considered*: Wiping the entire Room database on update was rejected because it would destroy the user's flashcard mastery, SRS schedules, and study plan progress.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

- **Architecture & Component Diagram**:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    SINGLE SOURCE OF TRUTH (SSOT) CORE                   │
├───────────────────────┬─────────────────────────┬───────────────────────┤
│   1. UI & DENSITY     │  2. BILINGUAL TYPOGRAPHY│  3. AI & CACHE SSOT   │
│  WordBooksDesignSSOT  │   Typography & BidiText │   AiPromptContract    │
│  • fontScale = 1.0f   │   • Bundled Inter/Cairo │   • Schema Version v3 │
│  • Canonical 411dp    │   • includeFontPadding  │   • Strict No-Fluff   │
│    Width Normalizer   │     = false             │   • Auto-Sanitizer    │
│  • DynamicColor = OFF │   • LRI/PDI Isolation   │   • Startup Migration │
└───────────┬───────────┴────────────┬────────────┴───────────┬───────────┘
            │                        │                        │
            ▼                        ▼                        ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                     JETPACK COMPOSE APPLICATION SHELL                   │
│  ┌─────────────────┐  ┌──────────────────┐  ┌────────────────────────┐  │
│  │ Flashcards &    │  │ Reading &        │  │ Word Books &           │  │
│  │ Quiz Screens    │  │ Grammar Screens  │  │ Dictionary Detail Sheet│  │
│  └─────────────────┘  └──────────────────┘  └────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
```

- **Data Model & State**:
  - **Design & Density SSOT**: `WordBooksDesignTokens` holds canonical reference width (`411f`), locked `fontScale = 1.0f`, standardized button padding, and elevation/border tokens.
  - **AI Prompt & Cache SSOT**: `AiPromptContract` centralizes prompt rules (no greetings, no sign-offs, no `**` or `###`, strict `•` bullets), versioned cache key generation (`v3:<feature>:<key>`), and text sanitization.
  - **Database Cache Hygiene**: Startup synchronization checks the stored `ssot_cache_version` preference; when upgraded, it purges legacy unversioned `ai_cache` rows and cleans any markdown artifacts in `ai_history` so the phone immediately reflects the latest formatting rules without requiring an app uninstall.

- **Interactive Component & State Mapping**:
  - **Root Theme Wrapper (`WordBooksTheme`)**: Reads current window configuration, calculates normalized density if logical width `< 392dp`, locks `fontScale = 1.0f`, and provides the unified `MaterialTheme` to all screens.
  - **AI Services (`ReadingAiService`, `GrammarAiService`)**: Delegate prompt suffixes, cache key construction, and post-response sanitization to `AiPromptContract` so every AI feature obeys the exact same rules.
  - **Action Buttons & Badges (`FlashcardsScreen`, `ReadingScreen`, `GrammarScreen`)**: Consume standardized single-line button tokens (`maxLines = 1`, `softWrap = false`, `includeFontPadding = false`) so labels never wrap or truncate on any physical screen.
