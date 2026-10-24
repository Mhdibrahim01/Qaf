# Qaf (قاف) Adaptive V2: Unified Architectural & Pedagogical Master Plan

**Repository / Environment State:**
- **Workspace Root:** `/app/applet` (Cloud Build Environment; Git `.git` metadata directory is not mounted in container; verified against live source tree and Room schema export `app/schemas/com.example.data.local.WordBooksDatabase/15.json`).
- **Database Schema Version:** Room `v15` (`exportSchema = true`, 20 `@Entity` tables).
- **Turn Scope:** Planning & Codebase Reality Audit Only (Zero `.kt`, `.xml`, `.json`, or Gradle changes in this turn).

---

## 1. Codebase Reality Audit (Current State vs. Brief)

Every claim in Section 3 of the master specification was verified directly against the codebase:

| Audit Item / Claim | Status | Exact File Paths & Line Numbers | Technical Findings & Evidence |
| :--- | :--- | :--- | :--- |
| **12 Decks (`21,624` rows in `entries`)** | **Confirmed** | `app/src/main/java/com/example/data/importer/DataImportRepository.kt:288, 344-385, 1520-1532` | 7 core JSON files (`9,862` rows across 5 core decks: `oxford5000` [5,948], `oxford_opal` [2,096], `oxford_phrases` [750], `everyday_expressions` [600], `essential_idioms` [468]) + 7 specialty JSON files (`11,762` rows: `ielts_core` [3,575], `it_industry` [1,622], `cefr_c1` [1,853], `pte_academic` [2,651], `business_english` [1,250], `career_interview` [406], `medical_healthcare` [405]). Total = **21,624** entries (+ 1 virtual `saved_words` deck row). |
| **90 Grammar Lessons (`grammar_lessons`)** | **Confirmed** | `app/src/main/assets/grammar_lessons.json`<br>`DataImportRepository.kt:1299-1356` | `g001` through `g090` spanning CEFR A1 (`g001`–`g015`), A2 (`g016`–`g029`), B1 (`g030`–`g053`), B2 (`g054`–`g076`), and C1 (`g077`–`g090`). |
| **61 Reading Texts (`reading_texts`, B1–C1)** | **Confirmed** | `app/src/main/assets/reading_texts.json`<br>`DataImportRepository.kt:1358-1404` | `r030` through `r090` (61 passages: B1 = 24, B2 = 23, C1 = 14). Zero A1 or A2 reading passages exist in `reading_texts.json`. |
| **Room v15 (Claimed 14 tables)** | **Different (20 tables)** | `app/src/main/java/com/example/data/local/WordBooksDatabase.kt:10-35`<br>`app/src/main/java/com/example/data/local/RoomEntities.kt:1-721` | `WordBooksDatabase` is at `version = 15, exportSchema = true`, and registers **20 `@Entity` tables**: `entries`, `decks`, `word_progress`, `attempts`, `sentences`, `grammar_lessons`, `grammar_progress`, `ai_history`, `reading_texts`, `reading_progress`, `ai_cache`, `feature_flags`, `feature_suggestions`, `deck_study_state`, `correction_notebook`, `study_plan_profile`, `study_plans`, `daily_task_checklist`, `quiz_results`, and `word_details`. |
| **Append-Only `attempts` Log** | **Confirmed (Missing `elapsed_ms`)** | `RoomEntities.kt:203-219`<br>`WordBooksDatabase.kt:228-241` | `AttemptEntity` logs `id, activityType, itemId, timestamp, sm2Rating, isCorrect, score, deck` with composite index `(activity_type, timestamp, is_correct)`. However, it **lacks an `elapsed_ms` column** required to compute rolling `personalPaceSeconds` EMA in Capability A. |
| **SM-2 (`Sm2Algorithm.kt`) with Intraday Timers** | **Confirmed** | `app/src/main/java/com/example/data/importer/Sm2Algorithm.kt:17-92` | `Rating 0 (Again)`: 10m timer (`600_000ms`), `reps=0`, `EF - 0.20`, `lapses + 1`. `Rating 1 (Hard)`: 4h timer (`14_400_000ms`) on `reps==0`, else `max(1, round(interval * 1.2))`, `EF - 0.15`. `Rating 2 (Good)`: `1d -> 6d -> round(interval * EF)`. `Rating 3 (Easy)`: `4d -> 7d -> round(interval * EF * 1.3)`, `EF + 0.15`. EF clamped to `[1.3f, 3.0f]`. |
| **Time-Budgeted `StudyPlanRepository.kt` & `correction_notebook`** | **Confirmed** | `app/src/main/java/com/example/data/plan/StudyPlanRepository.kt:414-783`<br>`RoomEntities.kt:428-452` | Tasks allocate from `remainingSeconds = minutes * 60`. `CorrectionNotebookEntity` implements 3-stage mastery (`masteryStage` 0 -> 1 -> 2 -> 3 at 3d / 7d / 21d intervals). |
| **Gap 1: Independent 5 Pillars vs. Text-First Spine** | **Confirmed** | `StudyPlanRepository.kt:370-783, 632-641, 1143-1346` | `buildDailyTaskList` schedules flashcards (lines 414–480), corrections (482–505), new words via `selectNewWordsForProfile` (507–562), sequential grammar lesson `targetLesson` (564–607), and then pairs reading purely by replacing `gXXX -> rXXX` (lines 632–641). `selectNewWordsForProfile` never reads today's reading passage or its vocabulary. `tools/build_reading_word_links.py` and `reading_word_links.json` are **Missing**. |
| **Gap 2: Non-Oxford Decks Lack Granular CEFR Grading** | **Confirmed** | `RoomEntities.kt:102-122`<br>`WordBooksDatabase.kt:160-193`<br>`StudyPlanRepository.kt:1180-1190` | `MIGRATION_13_14` populates `effective_level` only by exact headword match against `oxford5000`. Non-matching words in `ielts_core`, `it_industry`, `pte_academic`, `oxford_opal`, `business_english`, `career_interview`, and `medical_healthcare` have `effective_level = NULL`. For A1/A2 learners (`isB1Plus = false`), `includeNullLevel = 0` completely filters out those specialty deck words. |
| **Gap 3: No A1/A2 Reading Texts (3-Sentence Substitution)** | **Confirmed** | `StudyPlanRepository.kt:610-630`<br>`assets/reading_texts.json:1-30` | `preferSentencePractice` forces `level in listOf("A1", "A2")` onto a 3-sentence practice task instead of a reading passage because `reading_texts.json` starts at `r030` (B1). |
| **Gap 4: Linear Grammar Progression (No `<65%` Remediation)** | **Confirmed** | `StudyPlanRepository.kt:565-580` | Grammar selection loops over `searchLevels` and picks `dao.getUncompletedGrammarLessonsForLevel(lvl).first()`. It never inspects `grammar_progress.best_score < 65` or `correction_notebook` counts per `grammar_lesson_id`. |
| **Gap 5: Tap-to-Translate Does Not Log Lookup Signal or SRS Stage Highlights** | **Confirmed** | `app/src/main/java/com/example/ui/screens/ReadingScreen.kt:1505-1507, 1620-1633, 1776-1804` | Tapping a word in `ReadingScreen` opens `WordLookupBottomSheet` and runs `onLookupWordLocally(word)`. No lookup count, timestamp, or `SEEN` status is recorded unless the user manually favorites the word. `ReadingSentenceBlock` only highlights static `targetWords` (yellow) and `grammarKeywords` (gold), not the user's live SRS stages (`SEEN`, `LEARNING`, `FAMILIAR`). |
| **Gap 6: Missing Attempts-Driven Visualizations** | **Confirmed (Missing)** | `app/src/main/java/com/example/ui/screens/HomeScreen.kt`<br>`app/src/main/java/com/example/ui/screens/PlanScreen.kt` | Word Lifecycle Stages (6-stage funnel), Deck/CEFR Vocabulary Coverage Map, Conquered Words Trophy Shelf, Pre-Reading Readability %, Weekly "Before vs. After" Report, Composite CEFR Readiness Bar, and Data-Backed Insight Cards are all **Missing**. |
| **Gap 7: Onboarding Topics, Decks & Quiet Mode** | **Partially Different / Missing** | `app/src/main/java/com/example/ui/screens/OnboardingStep4DecksPlacement.kt:81-212, 464-542`<br>`OnboardingPreferencesRepository.kt:1-219` | **Deck Selection is Already Present** in Step 4 (users can toggle any of the 12 decks and choose "Start at my level" vs "From A1"). However, **3 Reading Topics selection** and **Quiet / No-Mic Mode toggle** are **Missing** from Onboarding and `StudyPlanProfileEntity`. |
| **Gap 8: JSON Backup/Export & Restore** | **Partially Present (Backend Exists, UI & `study_plans` Missing)** | `DataImportRepository.kt:1758-1788`<br>`RoomEntities.kt:707-720`<br>`app/src/main/java/com/example/ui/screens/MoreScreen.kt` | `exportUserBackupJson()` and `importUserBackupJson()` exist in `DataImportRepository.kt`, covering 9 progress tables (`schemaVersion = 15`). However, `study_plans` (`StudyPlanEntity`) is omitted from `UserBackupPayload`, and no Storage Access Framework (`CreateDocument` / `OpenDocument`) buttons exist in `MoreScreen.kt`. |
| **Gap 9: 6 Failing Automated Tests in `QA_REPORT.md`** | **Confirmed (Exact Root Causes Identified)** | `app/docs/QA_REPORT.md:1-145`<br>`OnboardingStep3Test.kt:199`<br>`NewWordSelectionGoldenTest.kt:91, 200`<br>`BidiTextTest.kt:58, 97`<br>`ExampleRobolectricTest.kt:1502` | **1.** `OnboardingStep3Test`: seeds a B2 reading text for a B1 15-min Monday profile; `minutes <= 15 && targetDayOfWeek % 2 == 0` (`Calendar.MONDAY == 2`) triggers `preferSentencePractice = true`, skipping reading.<br>**2 & 3.** `NewWordSelectionGoldenTest`: expects ascending numeric IDs (`day1Ids[i] < day1Ids[i+1]`), broken by `computeCandidateSequenceHash()` in `StudyPlanRepository.kt:1348-1458`.<br>**4 & 5.** `BidiTextTest`: `isolateLatinInRtlLine` (`BidiText.kt:306`) wraps outer `(` inside `\u2066...\u2069` instead of keeping matched outer parens outside `(\u2066...\u2069)`, plus `normalizeMixedBidiText` (`BidiText.kt:145`) inserts `\n` after `:`.<br>**6.** `ExampleRobolectricTest`: asserts `dailyQuiz.size == 5`, whereas `LocalQuizEngine.generateAlignedDailyQuiz` (`LocalQuizEngine.kt:345`) returns 10 questions (`5 Vocab + 3 Grammar + 2 Reading`). |

---

## 2. Canonical Metric & Stage Definitions (`LearnerModelRepository`)

To eliminate contradictory numbers across screens, `LearnerModelRepository` will serve as the **single source of truth** for all learner state, word lifecycle stages, and progress metrics.

### 2.1 Word Lifecycle Stages (Mutually Exclusive, Collectively Exhaustive)

Every entry in `entries` (joined with `word_progress` on `entries.id = word_progress.word_id`) belongs to **exactly one** of 5 active lifecycle stages (`NEW`, `SEEN`, `LEARNING`, `FAMILIAR`, `MATURE`), plus an orthogonal achievement badge (`CONQUERED`):

| Stage / Badge | Canonical SQL / Kotlin Predicate | Pedagogical Meaning |
| :--- | :--- | :--- |
| **`NEW`** | `wp.word_id IS NULL OR (wp.repetitions = 0 AND wp.lapses = 0 AND wp.lookup_count = 0 AND wp.next_review_timestamp = 0)` | Never encountered in flashcards, quizzes, or story Tap-to-Translate. |
| **`SEEN`** | `wp.repetitions = 0 AND (wp.lookup_count > 0 OR wp.lapses > 0 OR wp.next_review_timestamp > 0)` | Encountered via story Tap-to-Translate (`lookup_count > 0`) or currently in intraday `Again`/`Hard` learning step (`repetitions = 0`). |
| **`LEARNING`** | `wp.repetitions >= 1 AND wp.interval_days < 7` | Successfully recalled at least once, building initial short-term stability (`1..6` days). |
| **`FAMILIAR`** | `wp.repetitions >= 1 AND wp.interval_days >= 7 AND wp.interval_days < 21` | Medium-term retention established (`7..20` days). |
| **`MATURE`** | `wp.repetitions >= 1 AND wp.interval_days >= 21` | Long-term mastery achieved (`>= 21` days). |
| **`CONQUERED`** *(Badge)* | `wp.lapses >= 2 AND wp.interval_days >= 21` (with `wp.conquered_timestamp > 0` recorded when first crossing `interval_days >= 21` after `lapses >= 2`) | A formerly difficult word (`lapses >= 2` or resolved from `correction_notebook`) that the user has now pushed into `MATURE` (`interval_days >= 21`). |

### 2.2 Canonical Daily & Rolling Metrics

All timestamps use `TimeProvider` local day boundaries (`startOfDayMillis`, `endOfDayMillis`, `nowMillis`):

1. **`dueNow`**:
   $$\text{COUNT}(\text{wp}) \text{ WHERE } \text{wp.next\_review\_timestamp} > 0 \text{ AND } \text{wp.next\_review\_timestamp} \le \text{nowMillis}$$
2. **`dueToday`**:
   $$\text{COUNT}(\text{wp}) \text{ WHERE } \text{wp.next\_review\_timestamp} > 0 \text{ AND } \text{wp.next\_review\_timestamp} \le \text{endOfDayMillis}$$
   *(Includes intraday `Again` [10m] and `Hard` [4h] cards scheduled before midnight today).*
3. **`reviewedToday`**:
   $$\text{COUNT(DISTINCT item\_id)} \text{ FROM attempts WHERE activity\_type = 'flashcard' AND timestamp} \in [\text{startOfDayMillis}, \text{endOfDayMillis}]$$
4. **`newWordLearnedToday`**:
   $$\text{COUNT(DISTINCT a.item\_id)} \text{ FROM attempts a WHERE a.activity\_type = 'flashcard' AND a.timestamp} \in [\text{startOfDayMillis}, \text{endOfDayMillis}] \text{ AND NOT EXISTS (SELECT 1 FROM attempts p WHERE p.item\_id = a.item\_id AND p.activity\_type = 'flashcard' AND p.timestamp < startOfDayMillis)}$$
5. **`rollingAccuracy(pillar, windowDays = 7)`**:
   - Computed over `attempts` where `activity_type = pillar` and `timestamp >= nowMillis - windowDays * 86_400_000L`.
   - Returns `Float?` (`null` when `< 5` attempts exist in window; never fakes `0%` or `100%` on empty data).
6. **`retentionRate(windowDays = 14)`**:
   - Percentage of review attempts on previously learned cards (`interval_days >= 1` prior to attempt, or second+ flashcard attempt on `item_id`) where `sm2_rating >= 2` (`Good` or `Easy`) over the last 14 days.
7. **`personalPaceSeconds(activityType)`**:
   - Exponential Moving Average (EMA, $\alpha = 0.20$) of valid attempt durations (`elapsed_ms` clamped to `[1_500ms, 60_000ms]` for flashcards/sentences, `[5_000ms, 180_000ms]` for grammar/reading questions) over the last 30 attempts of `activityType`, falling back to baseline constants (`12f` sec/flashcard, `25f` sec/sentence, `180f` sec/grammar lesson, `150f` sec/reading passage) until $\ge 5$ timed attempts exist.
8. **`storyReadabilityPercent(readingTextId)`**:
   - Given the unique lemmatized/linked content tokens $T$ of a passage (from `reading_word_links.json` + stop-word baseline of top-200 A1 function words):
   $$\text{Readability \%} = \frac{|\{w \in T \mid w \in \text{FunctionWords} \lor \text{stage}(w) \in \{\text{LEARNING}, \text{FAMILIAR}, \text{MATURE}\}\}|}{|T|} \times 100$$
9. **`cefrReadinessPercent(level)`**:
   - Weighted composite across the target CEFR `level` (`A1`, `A2`, `B1`, `B2`, `C1`):
     - **50% Core Vocabulary Coverage:** $\min\left(1.0, \frac{\text{Words in selected/core decks at } \text{effective\_level} = L \text{ with stage} \in \{\text{FAMILIAR}, \text{MATURE}\}}{\text{Target Level Threshold } N_L}\right)$ where $N_{\text{A1}}=300, N_{\text{A2}}=400, N_{\text{B1}}=500, N_{\text{B2}}=600, N_{\text{C1}}=400$ (or actual deck count if smaller).
     - **30% Grammar Mastery:** $\frac{\text{Completed grammar lessons at level } L \text{ with } \text{best\_score} \ge 70}{\text{Total grammar lessons at level } L}$.
     - **20% Reading Comprehension:** $\min\left(1.0, \frac{\text{Completed reading passages at level } L \text{ with } \text{best\_score} \ge 70}{\text{Target Passages } R_L}\right)$ where $R_{\text{A1}}=8, R_{\text{A2}}=8, R_{\text{B1}}=12, R_{\text{B2}}=12, R_{\text{C1}}=10$.

---

## 3. Deterministic Adaptation Rules & `AdaptiveConfig.kt` Thresholds

All magic numbers and pedagogical thresholds are centralized in `com.example.data.plan.AdaptiveConfig`:

| Constant Name | Value | Governing Rule & Deterministic Behavior |
| :--- | :--- | :--- |
| **`COLD_START_MIN_ATTEMPTS`** | `20` | Minimum total logged attempts (across $\ge 2$ distinct study sessions/days) before dynamic pace/difficulty adjustments activate. Below this threshold, Qaf uses baseline onboarding defaults and displays a `"Calibrating to your pace (X/20 interactions)"` badge. |
| **`COLD_START_MIN_SESSIONS`** | `2` | Minimum distinct local calendar days with logged attempts required to exit Cold-Start calibration. |
| **`OVERDUE_CAP_MAX`** | `30` | Maximum due SRS reviews scheduled in a single daily plan (`<= 45%` of total daily minute budget). When `totalDue > 30`, UI surfaces a supportive backlog banner: *"Showing 30 priority reviews today (X more queued for upcoming days)"*. |
| **`NEW_WORDS_MIN` / `NEW_WORDS_MAX`** | `5` / `20` | Hard bounds on daily new vocabulary words. |
| **`BACKLOG_HEAVY_THRESHOLD`** | `25` | When `dueToday > 25` OR 7-day `rollingAccuracy("flashcard") < 0.70f`, daily `newWordsCount` scales down to `NEW_WORDS_MIN` (`5`) with reason string: *"Reduced to 5 new words today so you can clear due reviews comfortably."* |
| **`ACCURACY_FAST_TRACK_THRESHOLD`** | `0.88f` | When `dueToday < 10` AND 7-day `rollingAccuracy("flashcard") > 0.88f` (post cold-start), daily `newWordsCount` scales up by `+3` (up to `20`, respecting remaining time budget). |
| **`STORY_VOCAB_PRETEACH_MIN` / `MAX`** | `4` / `6` | Number of daily new words drawn directly from today's anchored reading story (`reading_word_links.json`) before filling remaining slots from the user's selected decks + goal weights. |
| **`STORY_READABILITY_BAND_MIN` / `MAX`** | `0.80f` / `0.92f` | Target instructional readability band (`80%–92%` known/familiar words) for selecting today's reading passage. |
| **`LEECH_LAPSE_THRESHOLD`** | `3` | If a word accumulates `>= 3` lapses (`sm2_rating == 0`) within `LEECH_WINDOW_DAYS` (`14` days) or `correction_notebook.error_count >= 3`, it is flagged as a Leech, enrolled in `correction_notebook`, and prioritized in sentence practice & daily quiz. |
| **`GRAMMAR_REMEDIATION_SCORE_THRESHOLD`** | `65` | If a completed grammar lesson has `best_score < 65` OR `>= 3` active `correction_notebook` items linked to `grammar_lesson_id`, the scheduler injects a `[Refresher]` task for that lesson before advancing to a new grammar topic. |
| **`LEVEL_UP_READINESS_THRESHOLD`** | `0.80f` | When `cefrReadinessPercent(currentLevel) >= 80%` AND the last 3 daily quizzes average `>= 80%`, Qaf surfaces a dismissible **Level-Up Readiness Banner** on Home/Plan (never auto-switches CEFR level without user confirmation). |
| **`STRUGGLE_DOWNSHIFT_ACCURACY`** | `0.55f` | If 7-day rolling accuracy across flashcards + grammar drops `< 55%` over `>= 20` attempts, Qaf offers a supportive one-tap suggestion to ease daily new-word load or review foundational level material. |

### 3.1 Zero-Division-Guarded 3-Tier Due Review Prioritization (`WordBooksDao` / `StudyPlanRepository`)

When selecting up to `OVERDUE_CAP_MAX` (`30`) due cards from `dueToday`, cards are ordered deterministically by:
1. **Tier 0 (Intraday Learning / Lapsed Cards):** `wp.interval_days == 0` (cards in 10m `Again` or 4h `Hard` steps), ordered by `wp.next_review_timestamp ASC`.
2. **Tier 1 (Active Leech / Lookup-Boosted Cards):** Cards present in `correction_notebook` (`mastery_stage < 3`) OR looked up in a story within the last 72 hours (`nowMillis - wp.last_looked_up_timestamp <= 259_200_000L`).
3. **Tier 2 (Relative Overdueness Ratio):** For `wp.interval_days >= 1`, ordered descending by:
   $$\text{OverduenessRatio} = \frac{\max(0, \text{nowMillis} - \text{wp.next\_review\_timestamp})}{\max(1, \text{wp.interval\_days}) \times 86\_400\_000}$$
   with tie-breaking by `wp.ease_factor ASC, CAST(wp.word_id AS INTEGER) ASC`.

---

## 4. Room Schema Changes (`v15 -> v16`) & Versioned Backup Format

### 4.1 Non-Destructive Migration `MIGRATION_15_16`

We strictly preserve **Content vs. Progress Separation**: curriculum tables (`entries`, `grammar_lessons`, `reading_texts`) are refreshed via `DataImportRepository` on `data_version` bumps without ever wiping `word_progress`, `attempts`, `grammar_progress`, `reading_progress`, `correction_notebook`, `study_plan_profile`, `study_plans`, `daily_task_checklist`, or `quiz_results`.

In `WordBooksDatabase.kt` (`version = 16`):

```sql
-- 1. Track story Tap-to-Translate lookups and Conquered milestone in word_progress
ALTER TABLE word_progress ADD COLUMN lookup_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE word_progress ADD COLUMN last_looked_up_timestamp INTEGER NOT NULL DEFAULT 0;
ALTER TABLE word_progress ADD COLUMN conquered_timestamp INTEGER NOT NULL DEFAULT 0;

-- Backfill conquered_timestamp for already-conquered words (lapses >= 2 AND interval_days >= 21)
UPDATE word_progress
SET conquered_timestamp = CASE WHEN last_reviewed_timestamp > 0 THEN last_reviewed_timestamp ELSE 1 END
WHERE lapses >= 2 AND interval_days >= 21 AND conquered_timestamp = 0;

-- 2. Track per-attempt response time (ms) in attempts for personalPaceSeconds EMA
ALTER TABLE attempts ADD COLUMN elapsed_ms INTEGER DEFAULT NULL;

-- 3. Store Onboarding reading topics, Quiet Mode, and dismissed insight cards in study_plan_profile
ALTER TABLE study_plan_profile ADD COLUMN preferred_topics_json TEXT NOT NULL DEFAULT '[]';
ALTER TABLE study_plan_profile ADD COLUMN quiet_mode_enabled INTEGER NOT NULL DEFAULT 0;
ALTER TABLE study_plan_profile ADD COLUMN dismissed_insights_json TEXT NOT NULL DEFAULT '{}';
```

### 4.2 Curriculum Asset & Data Version Upgrade (`CURRENT_DATA_VERSION = 7`)

Without mutating user progress tables, `DataImportRepository` (`CURRENT_DATA_VERSION` bumped from `6` to `7`) will:
1. Populate deterministic `effective_level` (`A1`..`C1`) across all **21,624** rows in `entries` (generated offline via `tools/assign_cefr_levels.py` so specialty decks like `it_industry`, `ielts_core`, `pte_academic`, `business_english`, `career_interview`, and `medical_healthcare` have full A2–C1 coverage instead of `NULL`).
2. Bundle **16 new A1/A2 Micro-Stories** (`r001`–`r016`: 8 A1 stories of 50–80 words, 8 A2 stories of 80–120 words, with Arabic summaries, comprehension questions, and `target_words_json`) in `assets/reading_texts.json` (total passages = **77**, spanning A1–C1).
3. Bundle `assets/reading_word_links.json` (generated offline via `tools/build_reading_word_links.py`) mapping every reading passage `r001`–`r090` to its matched `entries.id` vocabulary list and normalized macro-topic tag.

### 4.3 Versioned JSON Backup & Restore (`UserBackupPayload` v16)

1. Update `UserBackupPayload` (`RoomEntities.kt:707-720`) to default `schemaVersion = 16`, include `studyPlans: List<StudyPlanEntity> = emptyList()`, and seamlessly import both `v15` and `v16` backup JSON payloads (defaulting missing v16 fields gracefully).
2. Wire **Export Progress Backup (JSON)** (`ActivityResultContracts.CreateDocument("application/json")`) and **Restore Progress Backup (JSON)** (`ActivityResultContracts.OpenDocument()`) into `MoreScreen.kt` with a confirmation dialog and snackbar summary of restored records.

---

## 5. Phased Implementation Roadmap

### Phase 0: Safety Net, Test Suite Green-Light & Offline Curriculum Scripts
- **Goal:** Establish 100% test suite health, user backup/restore UI, and deterministic curriculum metadata before touching adaptive scheduling logic.
- **Files Created / Modified:**
  - `tools/assign_cefr_levels.py` & `tools/build_reading_word_links.py` (offline build scripts).
  - `app/src/main/assets/reading_texts.json` (add 16 A1/A2 micro-stories `r001`–`r016` + normalized topic tags) & `app/src/main/assets/reading_word_links.json`.
  - `app/src/main/java/com/example/data/local/RoomEntities.kt` & `DataImportRepository.kt` (add `studyPlans` to `UserBackupPayload`, bump `CURRENT_DATA_VERSION = 7`, load `effective_level` for all 12 decks).
  - `app/src/main/java/com/example/ui/screens/MoreScreen.kt` (wire SAF JSON Export/Restore buttons).
  - Fix the 6 failing tests in `BidiText.kt`, `StudyPlanRepository.kt` (`selectNewWordsForProfile` deterministic ID ordering within equal priority tiers), `OnboardingStep3Test.kt`, and `ExampleRobolectricTest.kt`.
- **Verification Gate:** `gradle :app:testDebugUnitTest` passes with **0 failures** across all existing test suites + new `BackupRestoreRoundTripTest`.

### Phase 1: Room `v15 -> v16` Migration, `AdaptiveConfig` & `LearnerModelRepository`
- **Goal:** Build the unified data & analytics foundation so every screen and scheduler queries one canonical source of truth.
- **Files Created / Modified:**
  - `app/src/main/java/com/example/data/plan/AdaptiveConfig.kt` (all threshold constants).
  - `app/src/main/java/com/example/data/plan/LearnerModelRepository.kt` (canonical flows and suspend queries for 6 word stages, `dueNow`, `dueToday`, `reviewedToday`, `newWordLearnedToday`, `rollingAccuracy`, `retentionRate`, `personalPaceSeconds`, `storyReadabilityPercent`, `cefrReadinessPercent`, and Arabic speaker trap clustering).
  - `app/src/main/java/com/example/data/local/WordBooksDatabase.kt`, `WordBooksDao.kt`, `RoomEntities.kt` (`MIGRATION_15_16`, exported schema `16.json`).
- **Verification Gate:** `Migration15To16Test` verifies zero data loss on realistic pre-migration user data; unit tests verify all 6 word stage predicates and zero-division-guarded overdueness sorting.

### Phase 2: Text-First "Today's Lesson" Spine & Reactive Reader Loop
- **Goal:** Transform the 5 independent daily tasks into a connected, story-anchored lesson spine with reactive Tap-to-Translate and grammar remediation.
- **Files Created / Modified:**
  - `app/src/main/java/com/example/data/plan/StudyPlanRepository.kt`:
    - **Step 1 (Anchor Passage Selection):** Pick today's reading passage (`r001`–`r090`) matching user's CEFR level, preferred topics (`preferred_topics_json`), and `80%–92%` readability band (or grammar synergy).
    - **Step 2 (Pre-Teach Story Vocabulary):** Select `4–6` unlearned words directly from today's story (`reading_word_links.json`), filling remaining `newWordsCount` (`5..20`, dynamically scaled by accuracy/backlog) from the user's selected decks + goal weights.
    - **Step 3 (Due SRS & Leech Prioritization):** Cap daily due flashcards at `30` (`<= 45%` time budget) using the 3-tier overdueness query, boosting words looked up in stories (`lookup_count > 0` within 72h).
    - **Step 4 (Grammar Progression + Remediation):** Inject a `[Refresher]` grammar lesson when a completed lesson has `best_score < 65%` or `>= 3` active `correction_notebook` items; otherwise advance sequentially.
    - **Step 5 (Contextual Sentence Practice & End-of-Lesson Synthesis Quiz):** Sentence practice pulls today's pre-taught story words + active leeches (and respects `quiet_mode_enabled` by switching speaking/mic prompts to word-order tile assembly). `LocalQuizEngine.kt` draws from today's story vocabulary, grammar rule, and passage comprehension.
  - `app/src/main/java/com/example/ui/screens/ReadingScreen.kt`:
    - Record `lookup_count + 1` and `last_looked_up_timestamp` in `word_progress` whenever a user taps a word in a story (`SEEN` stage transition + `+25%` next-day review priority boost).
    - Add a subtle one-tap **"Add to Today's Practice"** chip in `WordLookupBottomSheet` and underline/tint words in `ReadingSentenceBlock` that belong to the user's `SEEN` / `LEARNING` / `FAMILIAR` stages, with a completion banner: *"You reinforced X of your learning words in this story!"*
- **Verification Gate:** Golden Persona Tests (Personas 1, 2, 3 produce visibly distinct plans and reason strings) + Closed-Loop Behavioral Tests (Tap-to-Translate lookup -> next-day new-word queue; 3x flashcard lapse -> `correction_notebook` -> next-day sentence & quiz remediation; `<65%` grammar score -> refresher lesson).

### Phase 3: Onboarding Upgrade & Tangible Progress Visualizations
- **Goal:** Upgrade Onboarding to capture reading topics & Quiet Mode, and surface 100% real-data progress visualizations on Home, Plan, and Reading screens.
- **Files Created / Modified:**
  - `app/src/main/java/com/example/ui/screens/OnboardingStep3GoalTime.kt`, `OnboardingStep4DecksPlacement.kt`, `OnboardingPreferencesRepository.kt`, `PlanSetupDialog.kt`:
    - Add **Pick 3 Reading Topics** selector (8 normalized macro-topics: *Technology & Science, Business & Career, Daily Life & Culture, Health & Psychology, Environment & Nature, Travel & Global Issues, Education & Language, History & Society*) and **Quiet / No-Mic Mode** toggle.
  - `app/src/main/java/com/example/ui/screens/HomeScreen.kt` & `PlanScreen.kt`:
    - **Word Lifecycle Funnel Bar:** Live counts across `NEW -> SEEN -> LEARNING -> FAMILIAR -> MATURE`.
    - **Conquered Words Trophy Shelf:** Expandable card showcasing formerly difficult words (`lapses >= 2`) now at `MATURE` (`interval >= 21d`).
    - **Vocabulary Coverage Map & Composite CEFR Readiness Bar:** Breakdown by selected deck and CEFR level (`A1`–`C1`), plus dismissible Level-Up Readiness Banner at `>= 80%`.
    - **Weekly "Before vs. After" Progress Card & Data-Backed Insight Banner:** Compares this week vs. last week from `attempts` (with Cold-Start `"Calibrating (X/20)"` guard and 7-day dismissal cooldown per insight rule ID).
- **Verification Gate:** Robolectric UI tests verifying every progress widget renders exact database counts (`0` on fresh install, exact matches after seeded attempts) and preserves all existing `testTag`s.

---

## 6. Architectural Decisions Requiring User Approval

Before we begin Phase 0 implementation, please confirm or adjust our recommended choices on these 4 architectural decisions:

1. **Decoupling Story Selection from Rigid `gXXX -> rXXX` Numbering (Recommended: Hybrid Topic + Readability Match with Grammar Bonus)**
   - *Context:* Currently, `StudyPlanRepository.kt:632` hardcodes `rXXX` to match the exact number of today's grammar lesson `gXXX`. This prevents selecting stories based on the user's **3 chosen reading topics** or **80%–92% readability band**.
   - *Recommendation:* Score candidate uncompleted reading passages at the user's CEFR level using a deterministic composite score: `+40 pts` for matching one of the user's 3 preferred topics, `+35 pts` for falling in the `80%–92%` readability band, and `+25 pts` if `rXXX` shares the same lesson index or grammar tag as today's grammar lesson.
2. **Offline CEFR Leveling Strategy for Specialty Decks (Recommended: Multi-Signal Deterministic Heuristic Script)**
   - *Context:* `it_industry`, `ielts_core`, `pte_academic`, `business_english`, `career_interview`, and `medical_healthcare` currently have `effective_level = NULL` for words not in `oxford5000`, starving A2 learners (like Persona 2: Omar) of specialty vocabulary.
   - *Recommendation:* Run `tools/assign_cefr_levels.py` during Phase 0 to assign deterministic CEFR levels (`A2`, `B1`, `B2`, `C1`) to all non-Oxford specialty entries using headword/stem matching against Oxford 5000 + word frequency/length/morphological complexity tiers, baking the result into `DataImportRepository` (`CURRENT_DATA_VERSION = 7`).
3. **A1/A2 Micro-Stories Authoring & Packaging (Recommended: Add `r001`–`r016` Directly to `assets/reading_texts.json`)**
   - *Context:* `reading_texts.json` starts at `r030` (B1), leaving `r001`–`r029` unused.
   - *Recommendation:* Add 16 graded micro-stories (`r001`–`r008` for A1 at 50–80 words; `r009`–`r016` for A2 at 80–120 words) directly into `assets/reading_texts.json` across the 8 macro-topics, linking each to high-frequency A1/A2 vocabulary in `entries`.
4. **Fixing `NewWordSelectionGoldenTest` vs. Diversity Ordering (Recommended: Deterministic Sub-Tier Ordering by `CAST(e.id AS INTEGER) ASC`)**
   - *Context:* `NewWordSelectionGoldenTest` fails because `computeCandidateSequenceHash` scrambles numeric ID ordering (`day1Ids[i] < day1Ids[i+1]`).
   - *Recommendation:* Keep goal-weighted deck allocations and story pre-teaching at the macro level, and restore `CAST(e.id AS INTEGER) ASC` ordering within each deck/level tier so selection is 100% deterministic and satisfies `NewWordSelectionGoldenTest`.

---

## 7. Contradictions, Edge Cases & Missing Pieces Found in Audit

1. **Contradiction Resolved — Persona 2 (A2 Learner with `it_industry` & `ielts_core` Decks):**
   - In the current v15 codebase, `selectNewWordsForProfile` sets `includeNullLevel = 0` for A1/A2 users (`StudyPlanRepository.kt:1180`), and `it_industry` / `ielts_core` words not in Oxford 5000 have `effective_level = NULL`. Furthermore, IELTS/IT words that *do* match Oxford 5000 are mostly B1–C1. Without our Phase 0 `tools/assign_cefr_levels.py` grading (assigning foundational IT/career terms to A2 and allowing a controlled fallback to B1 specialty terms when a user explicitly selects a specialty deck at A1/A2), Persona 2 would receive zero non-Oxford IT words. We will explicitly allow selected specialty decks to supply `effective_level IN (userLevel, nextLevel)` when `userLevel` is A1/A2.
2. **Edge Case — `reading_texts.json` Granular Topic Strings:**
   - `reading_texts.json` currently uses 45+ free-form topic strings (e.g., `"food and culture"`, `"interviews"`, `"environment"`, `"remote work"`). To make the Onboarding "Pick 3 Reading Topics" selector clean and effective, `tools/build_reading_word_links.py` will map all 77 passages onto **8 canonical macro-topics** while preserving the original sub-topic label for display.
3. **Missing Schema Column Found — `attempts.elapsed_ms`:**
   - The prompt specifies computing `personalPaceSeconds` via an EMA of response times from `attempts`, but `AttemptEntity` (`RoomEntities.kt:203-219`) has no duration column. We added `ALTER TABLE attempts ADD COLUMN elapsed_ms INTEGER DEFAULT NULL` to `MIGRATION_15_16` so every flashcard, grammar, sentence, and reading interaction logs its exact duration in milliseconds.
4. **Edge Case — 15-Minute Budget vs. Story-Anchored Spine (`OnboardingStep3Test` Failure):**
   - Previously, 15-minute plans skipped reading on even days of the week (`targetDayOfWeek % 2 == 0`), causing `OnboardingStep3Test` to fail on Mondays and breaking the "Today's Lesson" reading spine for 15-minute learners. In Adaptive V2, 15-minute plans always include the compact daily reading/micro-story task (scaled to fit the 15-minute budget alongside 5–8 due reviews and 5 pre-taught story words).
