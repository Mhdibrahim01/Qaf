# Qaf Adaptive Learning & Spaced Repetition Plan

## Step 0: Environment & Working Tree Report

1. **Working Directory & Repository State**:
   - **Active Workspace**: `/app/applet` (Google AI Studio cloud container environment; `.git` directory is not mounted in the container, so branch/commit hashes are inspected via source tree state).
   - **Comparison with GitHub `main` (`Mhdibrahim01/Qaf`)**: This working tree is **ahead** of the earlier GitHub `main` baseline and is the up-to-date active copy:
     - **Room Database Version is `13` (not `12`)**: `app/src/main/java/com/example/data/local/WordBooksDatabase.kt:32-33` declares `version = 13, exportSchema = false` and already includes `MIGRATION_12_13` (`WordBooksDatabase.kt:134-150`), which adds `entry_id TEXT` and `index_word_progress_entry_id` on `word_progress`.
     - **Additional Bundled Decks**: Beyond the 9 original decks, `DataImportRepository.kt:268-270` and `app/src/main/assets/data_import/` include 3 additional generated specialty decks: `business_english.json` (1,250 entries), `career_interview.json` (406 entries), and `medical_healthcare.json` (405 entries).
2. **Onboarding Step 1 (`OnboardingPreferencesRepository` / DataStore)**:
   - **Present and complete** in the working tree along with Steps 1–5:
     - `app/src/main/java/com/example/data/onboarding/OnboardingPreferencesRepository.kt` (DataStore `"qaf_onboarding_prefs"` storing `userLevel`, `dailyGoalMinutes`, `learningGoal`, `startAtUserLevel`, `selectedDecks`, `hasCompletedOnboarding`).
     - `app/src/main/java/com/example/data/onboarding/OnboardingPlacementQuiz.kt`.
     - Unit & Robolectric tests present in `app/src/test/java/com/example/`: `OnboardingStep1Test.kt`, `OnboardingStep2Test.kt`, `OnboardingStep3Test.kt`, `OnboardingStep4Test.kt`, `OnboardingStep5Test.kt`, `PostOnboardingDashboardTest.kt`, and `SectionLevelPersonalizationTest.kt`.

---

## Verification of "WHAT IS ALREADY TRUE" & Findings 1–10

### Baseline Data, SM-2, and Schema Claims

| Claim in Brief | Status | Code / Asset Evidence (File & Line) |
| :--- | :--- | :--- |
| `oxford5000` (5,948 rows) & `oxford_phrases` (750 rows) have real `A1`–`C1` levels | **Confirmed** (with minor note) | `assets/data_import/oxford5000_part1..3.json` & `oxford_phrases.json`. Note: 7 rows in `oxford5000_part1/2` have `""` level. |
| `ielts_core` (3,575) all `"B2"`, `it_industry` (1,622) all `"B2"`, `cefr_c1` (1,853) all `"C1"`, `pte_academic` (2,651) all `"B2-C1"`, `oxford_opal` (2,096) & `everyday_expressions` (600) & `essential_idioms` (468) have blank `""` level | **Confirmed** + **3 extra decks** | Verified across `assets/data_import/*.json`. **Different/Additional**: `business_english` (1,250), `career_interview` (406), and `medical_healthcare` (405) also exist with synthetic `B1`/`B2`/`C1` labels (`DataImportRepository.kt:268-270`). Per your decision, those 3 will also be treated as ungraded (`NULL` unless in `oxford5000`). |
| Entry `id`s are `String` (`TEXT`) | **Confirmed** | `WordBooksEntities.kt:23` (`@PrimaryKey val id: String`). |
| 61 reading texts (`B1`: 24, `B2`: 23, `C1`: 14, `r030`–`r090`) paired with grammar lessons `g030`–`g090` (90 lessons `A1`–`C1`); no `A1`/`A2` reading texts | **Confirmed** | `assets/reading_texts.json` (`r030`–`r090`), `assets/grammar_lessons.json` (`g001`–`g090`), `StudyPlanRepository.kt:520-528`. |
| SM-2 (`Sm2Algorithm`, `applySm2Review`): Again = 10m, reps=0, ease-0.20; Hard = 4h first review then `interval * 1.2`; Good = 1d, 6d, then `* ease`; Easy = 4d, 7d, then `* ease * 1.3`. Latest state only in `word_progress`, no review history | **Confirmed** | `Sm2Algorithm.kt:27-86`, `DataImportRepository.kt:386-415`, `WordBooksEntities.kt:129-144`. |
| Room version 12 with `exportSchema = false` | **Different (Version 13)** | `WordBooksDatabase.kt:32-33` is already at **`version = 13`** with `exportSchema = false` (`MIGRATION_12_13` added `word_progress.entry_id` at lines 134–150). Phase 1 will therefore migrate **`13 -> 14`**, and Phase 2 will migrate **`14 -> 15`**. |

### Verification Table for Planner & SM-2 Findings (Items 1–10)

| # | Finding to Verify | Status | Exact File & Line References + Technical Details |
| :--- | :--- | :--- | :--- |
| **1** | New words ordered by level then `e.id` (`TEXT`), sorting lexicographically (`"100"`, `"1006"`, `"1007"`, `"1010"`...) | **Confirmed** | `WordBooksDao.kt:1256-1263` (`getNewWordsForLevelsAndDecks`): `ORDER BY CASE e.level ... END ASC, e.id ASC`. Because `entries.id` is `TEXT`, SQLite orders `"100"` before `"1006"` and `"2"` after `"1999"`. |
| **2** | For B2 user, `levelsForUserProfile` proposes `B2, B1, C1`, but fixed `CASE` in DAO sorts `B1` before `B2` | **Confirmed** | `StudyPlanRepository.kt:830-839` (`levelsForUserProfile("B2")` returns `listOf("B2", "B1", "C1")`), but `WordBooksDao.kt:1256-1262` hardcodes `WHEN 'A1' THEN 1 ... WHEN 'B1' THEN 3 WHEN 'B2' THEN 4`, forcing all `B1` words ahead of `B2`. (Note: `WordBooksViewModel.kt:1985` also bypasses `levelsForUserProfile` and passes `listOf(userLevel)`). |
| **3** | `oxford_opal` contributes nothing (`level = ""`); `pte_academic` never matches (`"B2-C1"`) | **Confirmed** | `WordBooksDao.kt:1247`: `AND e.level IN (:levels)` where `:levels` only contains subsets of `A1..C1`. Blank `""` (`oxford_opal`, idioms, expressions) and `"B2-C1"` (`pte_academic`) match zero rows. |
| **4** | A2 users with IELTS+IT get B2 words via silent retry with all levels; IELTS ids (`100001+`) sort before IT ids (`200001+`) so IT never surfaces | **Confirmed** | `StudyPlanRepository.kt:453-460`: when `getNewWordsForLevelsAndDecks` returns empty for `A2, A1, B1`, line 456 silently re-runs with `listOf("A1", "A2", "B1", "B2", "C1")`. Since both IELTS and IT are all `"B2"`, `e.id ASC` puts all 3,575 IELTS rows (`"100001"`–`"103575"`) ahead of IT (`"200001"`+). |
| **5** | Learning goal only changes monthly-plan theme text, not daily words, grammar, or reading | **Confirmed** | `StudyPlanRepository.kt:252-279` (`getThemesForGoal`) only sets `WeeklyPlanSummary.theme`/`focusAreas`. `buildDailyTaskList` (`StudyPlanRepository.kt:366-628`) never reads `profile.goal`. |
| **6** | Query groups by `LOWER(text)` and selects `e.*`, so representative `e.id` is nondeterministic | **Confirmed** | `WordBooksDao.kt:1255`: `GROUP BY LOWER(e.text)` with `SELECT e.*` in SQLite picks an arbitrary row from the group without preferring the user's level or lowest numeric ID. |
| **7** | "Already studied" matched by `deck + LOWER(text)` instead of `entry_id`; due-card count is global, not limited to selected decks | **Confirmed** | `WordBooksDao.kt:1248-1254` checks `NOT EXISTS (SELECT 1 FROM word_progress wp WHERE wp.deck = e.deck AND LOWER(wp.word_text) = LOWER(e.text)...)`. Even though `MIGRATION_12_13` added `wp.entry_id`, `StudyPlanRepository` and `WordBooksDao.getDueCardsCount` (`WordBooksDao.kt:891-893`) still count globally across all decks. |
| **8** | Two definitions of "due": Home and due-today use `due <= endOfToday`; Plan uses `due <= now`. `findNextDueCardIndex` lets later-today cards (Again 10 min) return immediately | **Confirmed** | `DataImportRepository.kt:350-355` & `417-439` pass `endOfTodayMillis()` (`23:59:59.999`), whereas `StudyPlanRepository.kt:372,415` passes `now = System.currentTimeMillis()`. Inside a session, `findNextDueCardIndex` checks `due <= endOfToday`, so an `Again` (+10m) or first-`Hard` (+4h) card stays immediately eligible, and only scans `(currentIndex + 1) until cards.size` without wrapping. |
| **9** | `Sm2Algorithm.previewIntervals`: Hard label is `"1d"` after first review instead of `round(interval * 1.2)`; first review says `"Today"` instead of `"4h"`; KDoc says Good `1d->3d` vs code `6d`; ease has no upper cap; no dedicated SM-2 unit tests | **Confirmed** | `Sm2Algorithm.kt:94` sets `val hardDays = if (reps == 0) 0 else 1` (`"Today"` / `"1d"`), contradicting line 50 `max(1, (currentInterval * 1.2f).roundToInt())`. `DataImportRepository.kt:384` comment says `1d -> 3d` while `Sm2Algorithm.kt:62` uses `6`. `Sm2Algorithm.kt:81` only applies `.coerceAtLeast(1.3f)`. No `Sm2AlgorithmTest.kt` exists in `app/src/test/`. |
| **10** | Same `last_reviewed_timestamp` counted as both reviews and new words; manual mastery updates it; dashboard shows fixed `/ 20` goal; Home preview slices `tasks.take(4)`; weekly indicators ignore saved daily checklists | **Confirmed** | `WordBooksDao.kt:903` (`getStudiedTodayCount`) and `1232` (`getCardsRatedTodayCount`) run identical SQL on `last_reviewed_timestamp >= :startOfDayMillis`. `DataImportRepository.kt:611` (`toggleMastered`) sets `lastReviewedTimestamp = System.currentTimeMillis()`. `HomeScreen.kt:1327` hardcodes `"Daily Goal: $studiedTodayCount / 20"`. `HomeScreen.kt:524` slices `tasks.take(4)` while the badge shows `totalCount` (5). Weekly strip derives from raw timestamps rather than `daily_checklists`. |

### Additional Contradictions / Technical Notes Found in Code
- **Unused `PlanDailyTemplate` & Dead `shouldScheduleOnDay`**: `StudyPlanRepository.kt:209-215` computes `scaledTemplate` (`PlanDailyTemplate`), and `374-378` decodes `val template = ...`, which is never referenced in `buildDailyTaskList` (lines 407–443 recompute counts directly from `profile.minutesPerDay`). `shouldScheduleOnDay` (`StudyPlanRepository.kt:859-867`) is dead code.
- **Grammar & Reading Fallback Order**: When a `B2` user finishes `B2` grammar lessons, `StudyPlanRepository.kt:480-489` falls back in fixed order `A1, A2, B1, B2, C1` (jumping down to `A1` instead of nearest neighbor `B1`/`C1`). And for `A1`/`A2` users, because `g001`–`g029` have no paired `r001`–`r029`, reading selection falls through to `getLowestScoringReadingTexts(1)` or `r030` (`B1`) without explaining why.
- **`refreshChecklistCompletion` is Static Intra-Day**: `StudyPlanRepository.kt:633-717` only toggles `isCompleted` booleans on the tasks generated that morning; it never reflects newly due `correction_reviews` or explains backlog changes.

---

## Confirmed Product & Architecture Decisions

All 4 open technical questions were resolved with your confirmation:
1. **SM-2 Ease Factor Upper Cap (Phase 2)**: Cap `easeFactor` at **`3.0f`** (with floor `1.3f` and default `2.5f`).
2. **In-Session `Again` (10m) & First-`Hard` (4h) Behavior (Phase 2)**: **Hide until `dueTimestamp` is reached (`due <= nowMillis`)** so intra-day repeat cards wait for their actual timer before re-appearing.
3. **Grading Policy for `business_english`, `career_interview`, and `medical_healthcare` (Phase 1)**: **Treat as ungraded (`NULL` unless headword is in `oxford5000`)**, keeping only `oxford5000`, `oxford_phrases`, and `cefr_c1` as trusted CEFR-graded decks.
4. **"From A1" vs "At My Level" (`startAtUserLevel`) Storage (Phase 1)**: **Add `start_at_user_level INTEGER NOT NULL DEFAULT 1` column to `study_plan_profile` in Room** (and sync with `OnboardingPreferencesRepository` DataStore and JSON backup/export).

---

## Phase 1: Selection Fix (`effective_level`, Deterministic Ordering, Deck Weights & Honest Fallbacks)

- **Effort**: **M**
- **Scope**:
  1. **Add `entries.effective_level` & `study_plan_profile.start_at_user_level` (Safe Migration `13 -> 14`)**:
     - Add nullable `effective_level TEXT` to `entries` with index `index_entries_deck_effective_level(deck, effective_level)`.
     - Backfill rule (applied both in SQL `MIGRATION_13_14` and in `DataImportRepository` on asset import / data version bump):
       - First, if the lowercase trimmed headword (`LOWER(TRIM(text))`) matches an entry in `oxford5000` that has a valid CEFR level (`'A1'`, `'A2'`, `'B1'`, `'B2'`, `'C1'`), use the lowest CEFR level found in `oxford5000` for that headword.
       - Else, if `deck IN ('oxford5000', 'oxford_phrases', 'cefr_c1')` and `level IN ('A1', 'A2', 'B1', 'B2', 'C1')`, use `entries.level`.
       - Else, set `effective_level = NULL` (this converts `"B2-C1"` in `pte_academic`, blanket `"B2"` in `ielts_core`/`it_industry`, synthetic labels in `business_english`/`career_interview`/`medical_healthcare`, and blank `""` in `oxford_opal`/idioms/expressions to `NULL` whenever the headword is not in `oxford5000`).
     - Add `start_at_user_level INTEGER NOT NULL DEFAULT 1` to `study_plan_profile` in Room (`1` = start at user's level; `0` = "From A1" progressive mode). Sync two-way with `OnboardingPreferencesRepository` DataStore and include in JSON backup/export.
  2. **Deterministic Headword Deduplication & Level/Numeric Ordering**:
     - Level closeness order per user level (never jumping to far-higher levels):
       - **At My Level (`startAtUserLevel = true`)**:
         - `A1`: `["A1", "A2"]`
         - `A2`: `["A2", "B1", "A1"]`
         - `B1`: `["B1", "B2", "A2"]`
         - `B2`: `["B2", "C1", "B1"]`
         - `C1`: `["C1", "B2"]`
       - **From A1 (`startAtUserLevel = false`)**:
         - Progresses upward from `'A1'` up to the user's selected level (e.g., for `B2`: `["A1", "A2", "B1", "B2"]`).
     - One deterministic entry per lowercase headword (`LOWER(TRIM(e.text))`): choose the candidate row whose `effective_level` is closest to the user's target level priority, breaking ties by deck weight priority and then lowest numeric ID (`CAST(e.id AS INTEGER) ASC, e.id ASC`).
     - Order selected candidates by dynamic level closeness rank first, then `CAST(e.id AS INTEGER) ASC, e.id ASC`.
  3. **Centralized Deck Weights & Goal-Aware Specialty Share**:
     - Create `DeckSelectionConfig` in `StudyPlanRepository.kt` (single source of truth for deck weights and specialty share by goal: `general`, `ielts`, `it_career`, `business`, etc.).
     - **Graded vs. NULL-level ("Specialty Share") split**:
       - Default specialty (`effective_level IS NULL`) share: **30%** for general goals; **50%** for `ielts` / `it_career` goals when `B1+`.
       - **Level safety**: Unmatched `effective_level IS NULL` words are eligible **only for `B1`, `B2`, and `C1` users**. For `A1` and `A2` users, words from specialty decks (`oxford_opal`, `pte_academic`, `ielts_core`, `it_industry`, etc.) are only eligible when their `oxford5000`-matched `effective_level` is at most one level above the user (`A1` -> `A1..A2`; `A2` -> `A1..B1`).
       - Within the graded share and specialty share, allocate slots across the user's `selectedDecks` proportionally to the goal's deck weights so `ielts_core` IDs (`100001+`) can never starve `it_industry` (`200001+`), `oxford_opal`, or `pte_academic`.
  4. **Honest Depletion & Reason Strings (No Silent All-Level Fallback)**:
     - Remove the silent retry with `listOf("A1", "A2", "B1", "B2", "C1")` in `StudyPlanRepository.kt:455-460`.
     - If a selected deck has fewer words near the user's level than its quota, redistribute the remaining quota to other selected decks that still have eligible words near the user's level.
     - If all selected decks combined cannot fill `newWordsCount` near the user's level, return only the honest available words and attach a clear reason string to the task description/subtitle (e.g., `"Showing 6 of 10 words: selected decks have no more A2–B1 words"` or `"IT & IELTS specialty words unlock at B1+; showing A2–B1 core words from your decks"`).
     - Apply nearest-neighbor CEFR distance fallback for Grammar lessons (`B2` -> `B1, C1` before `A2, A1`) and add an explicit informational reason when an `A1`/`A2` user is shown a `B1` reading text (since `r030`–`r090` start at `B1`).
  5. **Entry-ID Studied Matching & Deck-Scoped Due Counts**:
     - Match studied words primarily by `wp.entry_id = e.id` (with fallback `OR (wp.entry_id IS NULL AND wp.deck = e.deck AND LOWER(wp.word_text) = LOWER(e.text))` for any pre-v13 legacy rows not yet linked, plus a backfill in `MIGRATION_13_14` that populates `wp.entry_id` where `NULL`).
     - Add deck-filtered due count & due card queries in `WordBooksDao` (`deck IN (:selectedDecks)`) so the daily plan and deck sessions only count due reviews from the user's active decks.
  6. **Before/After Simulation & Golden Tests**:
     - Create deterministic simulation & golden tests in `app/src/test/java/com/example/Phase1PlannerGoldenTest.kt` comparing **Profile A (`B2`, `oxford5000` + `oxford_opal`, General)** and **Profile B (`A2`, `ielts_core` + `it_industry`, IT Career)** on **Day 1** and **Day 7**, printing the before/after tables and asserting exact invariants.

- **Files to Modify / Create**:
  - `app/src/main/java/com/example/data/local/WordBooksEntities.kt` (`Entry.effectiveLevel`, `StudyPlanProfileEntity.startAtUserLevel`)
  - `app/src/main/java/com/example/data/local/WordBooksDatabase.kt` (bump version `13 -> 14`, add `MIGRATION_13_14`)
  - `app/src/main/java/com/example/data/local/WordBooksDao.kt` (backfill query, deterministic candidate queries, deck-scoped due queries, entry_id matching)
  - `app/src/main/java/com/example/data/DataImportRepository.kt` (compute `effective_level` on import & data version bump; deck-scoped due queries; backup/restore support for `startAtUserLevel`)
  - `app/src/main/java/com/example/data/StudyPlanRepository.kt` (`DeckSelectionConfig`, deterministic word selector, honest fallback reason strings, nearest-neighbor grammar fallback)
  - `app/src/main/java/com/example/ui/WordBooksViewModel.kt` (use shared selector in `startFocusedNewWordsSession` and plan creation)
  - `app/src/test/java/com/example/Phase1PlannerGoldenTest.kt` (new golden test + Day 1 / Day 7 simulation output)

- **Schema & Migration (`13 -> 14`)**:
  - `ALTER TABLE entries ADD COLUMN effective_level TEXT`
  - `CREATE INDEX IF NOT EXISTS index_entries_deck_effective_level ON entries(deck, effective_level)`
  - `UPDATE entries SET effective_level = ...` (SQL backfill matching `LOWER(TRIM(text))` against `oxford5000`, else preserving `'A1'..'C1'` for `oxford5000`, `oxford_phrases`, `cefr_c1`, else `NULL`)
  - `UPDATE word_progress SET entry_id = (SELECT MIN(CAST(e.id AS INTEGER)) FROM entries e WHERE e.deck = word_progress.deck AND LOWER(e.text) = LOWER(word_progress.word_text)) WHERE entry_id IS NULL`
  - `ALTER TABLE study_plan_profile ADD COLUMN start_at_user_level INTEGER NOT NULL DEFAULT 1`

- **Tests**:
  - `Phase1PlannerGoldenTest.kt`: verifies `effective_level` backfill across all 12 decks, numeric ID ordering (`"2"` before `"10"`, `"100"` before `"1006"`), `B2` level priority (`B2 -> C1 -> B1`), `From A1` mode (`A1 -> A2 -> ...`), `oxford_opal` & `pte_academic` inclusion, `A2` protection against `B2`-only ungraded words with honest reason string, and Day 1 & Day 7 before/after profiles.

- **Risks & Mitigations**:
  - *SQLite query speed on `LOWER(TRIM(text))` join during migration*: Mitigated by creating a temporary lookup index on `oxford5000(LOWER(TRIM(text)))` inside `MIGRATION_13_14` so the backfill runs in < 50 ms on 20k rows.

- **What You Will See on Screen**:
  - **B2 General (Oxford 5000 + OPAL)**: Daily new words immediately start with real `B2` words in numeric order alongside a 30% share of academic `oxford_opal` words, rather than being stuck on `B1` words `100, 1006, 1007...` with zero OPAL words.
  - **A2 IT Career (IELTS + IT)**: Instead of silently dumping `B2` IELTS words (`100001+`) and ignoring the IT deck, the planner selects only `A2`/`B1`/`A1` words (via Oxford 5000 cross-reference) balanced across IELTS and IT with an IT-weighted mix, and if fewer words match `A2–B1`, displays a clear reason line explaining why fewer words are shown.
  - **Due Badge**: Reflects due cards in the user's selected decks rather than all 12 decks globally.

---

## Phase 2: Practice History, Honest Metrics, Due-Time Unification & SM-2 Fixes

- **Effort**: **M**
- **Scope**:
  1. **Append-Only `attempts` Table & Transactional Logging (Migration `14 -> 15`)**:
     - Create `attempts` (`AttemptEntity` in `WordBooksEntities.kt`):
       - `id INTEGER PRIMARY KEY AUTOINCREMENT`
       - `activity_type TEXT NOT NULL` (`'flashcard_new'`, `'flashcard_review'`, `'quiz_question'`, `'grammar_quiz'`, `'reading_quiz'`, `'sentence_practice'`, `'correction_review'`)
       - `item_id TEXT NOT NULL` (`entry.id`, `lessonId`, `readingId`, `questionId`, etc.)
       - `deck TEXT` (nullable, for vocabulary items)
       - `timestamp INTEGER NOT NULL`
       - `sm2_rating TEXT` (nullable: `'AGAIN'`, `'HARD'`, `'GOOD'`, `'EASY'`)
       - `is_correct INTEGER` (nullable: `1` or `0`)
       - `score_percent INTEGER` (nullable: `0..100` for sentence-diff or quiz score)
       - `elapsed_ms INTEGER` (nullable)
     - Add `first_learned_timestamp INTEGER NOT NULL DEFAULT 0` to `word_progress` (backfilled in `MIGRATION_14_15` to `last_reviewed_timestamp` where `repetitions > 0 OR again_count > 0`, and `0` where the row was only created for `is_favorite = 1` or manual `is_mastered = 1` with `repetitions = 0 AND again_count = 0`). No fake/invented rows in `attempts`.
     - Write `AttemptEntity` inside the **same `@Transaction`** as `applySm2Review`, quiz completion, sentence practice submission, and correction review rating.
     - **Favorite & Manual Mastery Isolation**:
       - `toggleMastered` (`DataImportRepository.kt:604-617`) must **not** modify `lastReviewedTimestamp` or `firstLearnedTimestamp`, and neither `toggleFavorite` nor `toggleMastered` may insert into `attempts`.
  2. **Separate New Words, Reviews, Accuracy & Mastery Metrics**:
     - **New word studied today**: `word_progress.first_learned_timestamp` falls within `[startOfDayMillis, endOfDayMillis]` (set once on the card's very first real SM-2 rating and never overwritten on subsequent reviews or lapses).
     - **Review completed today**: distinct or total `'flashcard_review'` attempts in `attempts` within `[startOfDayMillis, endOfDayMillis]` (where the card already had `first_learned_timestamp > 0` prior to that rating).
     - **Repeated attempts**: every rating in a session appends a row to `attempts` so full history and accuracy (`GOOD`/`EASY` or `is_correct = 1` divided by total attempts) are preserved separately from checklist task completion.
  3. **Unified "Due Now" vs. "Due Today" & In-Session Waiting**:
     - Define two explicit helpers in `ClockProvider` / `TimeUtils` (using an injectable `Clock` / time source):
       - **`dueNowCutoff(nowMillis)`** = `nowMillis` (`due_timestamp > 0 AND due_timestamp <= nowMillis`): used for active review queues, in-session next-card selection, and the "Due now" badge.
       - **`dueTodayCutoff(nowMillis)`** = `endOfDayMillis(nowMillis)` (`due_timestamp > 0 AND due_timestamp <= endOfDayMillis`): used for daily planning capacity and "Due today (including X later today)" summary counts.
     - **In-Session `Again` (10m) & First-`Hard` (4h) Behavior** (per your decision):
       - Inside an active flashcard session, `findNextDueCardIndex` wraps around the full list (`(currentIndex + 1 until size) + (0 until currentIndex)`) and only selects cards where `dueTimestamp == 0L || dueTimestamp <= clock.nowMillis()`.
       - Cards rated `Again` (+10m) or first-`Hard` (+4h) are **hidden until `dueTimestamp <= clock.nowMillis()`**. If no other cards are due *now* but `N` cards are scheduled for later today (`nowMillis < dueTimestamp <= endOfDayMillis`), the session screen shows a clear, non-guilt state: `"All due cards done for now — N card(s) scheduled for later today (next in X min)"`.
  4. **SM-2 Fixes, `3.0f` Ease Cap & Injectable Clock Unit Tests**:
     - Fix `Sm2Algorithm.previewIntervals`:
       - When `reps == 0`, `Hard` label displays `"4h"` (matching `FOUR_HOURS_MILLIS`) instead of `"Today"`, and `Again` displays `"10m"`.
       - When `reps > 0`, `Hard` label computes `val hardDays = max(1, (interval * 1.2f).roundToInt())` and formats `"${hardDays}d"` (matching `calculateNextReview` line 50) instead of hardcoded `"1d"`.
     - Fix KDoc in `Sm2Algorithm.kt` and `DataImportRepository.kt:384` to state `Good: 1d -> 6d -> round(interval * ease)` consistently.
     - Apply your approved **Ease Factor Upper Cap of `3.0f`**: `.coerceIn(MIN_EASE_FACTOR, MAX_EASE_FACTOR)` (`1.3f..3.0f`) on `EASY` (`Sm2Algorithm.kt:81`).
     - Inject `nowMillis: Long` into `Sm2Algorithm.calculateNextReview` (already supported via default parameter) and pass an injectable `TimeProvider` into `DataImportRepository` and `StudyPlanRepository`.
  5. **Room Schema Export, Migration Test & Backup/Redo-Setup Preservation**:
     - Set `exportSchema = true` in `WordBooksDatabase.kt` and configure Room schema location (`$projectDir/schemas`) in `app/build.gradle.kts` with KSP argument `room.schemaLocation`.
     - Include `attempts` and `first_learned_timestamp` in JSON Backup/Export (`DataImportRepository.exportUserDataToJson` / `importUserDataFromJson`).
     - Ensure "Redo setup" / onboarding reset never deletes `attempts`, `word_progress`, favorites, notebook, or streak data.

- **Files to Modify / Create**:
  - `app/build.gradle.kts` (add KSP `room.schemaLocation` for `exportSchema = true`)
  - `app/src/main/java/com/example/data/local/WordBooksEntities.kt` (`AttemptEntity`, `WordProgress.firstLearnedTimestamp`)
  - `app/src/main/java/com/example/data/local/WordBooksDatabase.kt` (`version = 15`, `exportSchema = true`, `MIGRATION_14_15`)
  - `app/src/main/java/com/example/data/local/WordBooksDao.kt` (`insertAttempt`, `getNewWordsLearnedInRange`, `getReviewAttemptsInRange`, `getAccuracyStats`, `dueNow` vs `dueToday` queries)
  - `app/src/main/java/com/example/util/Sm2Algorithm.kt` (`MAX_EASE_FACTOR = 3.0f`, `previewIntervals` `"10m"`/`"4h"`/`"${hardDays}d"` fix, KDoc alignment)
  - `app/src/main/java/com/example/data/DataImportRepository.kt` (transactional attempt logging, `toggleMastered` timestamp fix, `findNextDueCardIndex` with `due <= nowMillis` & wrap-around, backup/export of `attempts`)
  - `app/src/main/java/com/example/ui/WordBooksViewModel.kt` & flashcard UI (show "next due in X min" when intra-day cards are waiting; expose distinct new/review/accuracy metrics)
  - `app/src/test/java/com/example/Sm2AlgorithmTest.kt` (new dedicated unit test suite with injectable clock)
  - `app/src/test/java/com/example/DatabaseMigration13To15Test.kt` (new migration & data-preservation test)

- **Schema & Migration (`14 -> 15`)**:
  - `ALTER TABLE word_progress ADD COLUMN first_learned_timestamp INTEGER NOT NULL DEFAULT 0`
  - `UPDATE word_progress SET first_learned_timestamp = last_reviewed_timestamp WHERE (repetitions > 0 OR again_count > 0) AND last_reviewed_timestamp > 0`
  - `CREATE TABLE IF NOT EXISTS attempts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, activity_type TEXT NOT NULL, item_id TEXT NOT NULL, deck TEXT, timestamp INTEGER NOT NULL, sm2_rating TEXT, is_correct INTEGER, score_percent INTEGER, elapsed_ms INTEGER)`
  - `CREATE INDEX IF NOT EXISTS index_attempts_timestamp ON attempts(timestamp)`
  - `CREATE INDEX IF NOT EXISTS index_attempts_activity_item ON attempts(activity_type, item_id)`

- **Tests**:
  - `Sm2AlgorithmTest.kt`: tests `Again` (10m, reps=0, ease-0.20 floored at 1.3f), `Hard` (reps=0 -> 4h label `"4h"`, reps>0 -> `round(interval * 1.2)` label `"${hardDays}d"`), `Good` (1d -> 6d -> `* ease`), `Easy` (4d -> 7d -> `* ease * 1.3`, capped at `3.0f`), and injectable clock determinism.
  - `DatabaseMigration13To15Test.kt`: seeds a v13 SQLite database with ratings, due dates, favorites, manual mastery, grammar/reading progress, quiz history, and notebook entries; runs `MIGRATION_13_14` and `MIGRATION_14_15`; verifies zero data loss, accurate `effective_level` and `first_learned_timestamp` backfills, and JSON export/import round-trip including `attempts`.

- **Risks & Mitigations**:
  - *Existing tests checking `previewIntervals` strings*: Search all test files for `"Today"` or `"1d"` before updating labels so any affected test assertions are updated with clear documentation.

- **What You Will See on Screen**:
  - **Flashcard Rating Buttons**: On an unreviewed or lapsed card (`reps == 0`), `Again` shows `10m` and `Hard` shows `4h`; on a card with a 10-day interval, `Hard` accurately shows `12d` instead of `1d`.
  - **In-Session Queue**: Cards rated `Again` or `Hard` (first review) no longer pop back up 2 seconds later; they wait until their 10-minute or 4-hour timer expires, with a clear status message if all currently due cards are finished.
  - **Progress Counters**: Favoriting or manually toggling "Mastered" on a word no longer inflates "Studied today" or "Reviewed today". New words learned today, review cards rated today, and accuracy % are displayed as distinct, honest numbers.

---

## Phase 3: Daily Plan Reflects the Whole App (Time Budget, Whole-App Tasks, Reasons & Weekly Strip)

- **Effort**: **M**
- **Scope**:
  1. **Whole-App Task Types in Daily Planner**:
     - Expand `buildDailyTaskList` and `refreshChecklistCompletion` in `StudyPlanRepository.kt` to schedule and track:
       - **Due Flashcard Reviews** (`task_review_due`, priority 1) — scoped to selected decks, showing both `dueNow` and `dueToday` counts.
       - **Due Correction Reviews** (`task_correction_reviews`, priority 2) — scheduled whenever `getDueCorrectionReviewsCount(now)` > 0 (from the user's saved mistake/correction cards).
       - **New Words** (`task_new_words`, priority 3) — selected via Phase 1's level-honest, goal-weighted selector.
       - **Grammar Lesson** (`task_grammar`, priority 4) — nearest-neighbor CEFR fallback with reason.
       - **Reading Practice OR Short Sentence Practice** (`task_reading` / `task_sentence_practice`, priority 5) — rotates short sentence practice on days when reading is not scheduled or when time budget is tight, ensuring `A1`/`A2` users also get level-appropriate sentence production practice.
       - **Daily Verification Quiz** (`task_daily_quiz`, priority 6) — when time budget permits.
  2. **Explicit Time-Budget Allocation & Load Reduction**:
     - Compute task quantities from `profile.minutesPerDay` using explicit time estimates:
       - Flashcard review: **`~10 s` (`0.17 min`)** per card
       - Correction review: **`~20 s` (`0.33 min`)** per item
       - New word: **`~45 s` (`0.75 min`)** per word
       - Short sentence practice: **`~3 min`** (3 sentences)
       - Reading text: **`~5 min`** (1 text)
       - Grammar lesson: **`~7 min`** (6–8 min midpoint, 1 lesson)
     - **Reviews First & Automatic Load Pacing**:
       - Deduct due reviews (and due correction reviews) from the daily minute budget first (capped at 60% of `minutesPerDay` so a large backlog never overwhelms the user in a single day).
       - If due reviews consume a large share of the budget (or after missed days), automatically reduce the new-word count or rotate out lower-priority tasks, and display the remaining review backlog honestly without guilt (e.g., `"25 of 42 due reviews scheduled today to fit your 15-min goal (17 remaining in backlog)"`).
  3. **One-Line Reason on Every Daily Task**:
     - Every `DailyTaskItem` carries a non-empty, data-backed `reason` string rendered directly below the task title on both `HomeScreen` and `PlanScreen`, e.g.:
       - Review task: `"18 due now + 4 later today in Oxford 5000 & OPAL (~3 min)"`
       - Reduced new words: `"Fewer new words today (6 instead of 10): 28 reviews are due"`
       - Normal new words: `"8 B2 + OPAL words matched to your General English goal (~6 min)"`
       - Correction review: `"3 saved sentence corrections are due for review today"`
       - Grammar / Reading: `"Unfinished B2 lesson g054 (~7 min)"` or `"Paired with grammar lesson g054 (~5 min)"`
  4. **Dynamic Daily Goal, Full Home Preview & Checklist-Backed Weekly Indicators**:
     - **Dynamic Daily Goal**: Replace hardcoded `"Daily Goal: $studiedTodayCount / 20"` in `HomeScreen.kt:1327` (`WeeklyProgressSummaryCard`) with the user's actual daily target from today's plan (`completedItemsToday / plannedItemsToday` and `completedTasks / totalTasks`).
     - **Show All Today's Tasks on Home**: Remove `.take(4)` in `HomeScreen.kt:524` so every task in `todayChecklist.tasks` is visible and matches the `completedTasks/totalTasks` badge in `HomeScreen.kt:504`.
     - **Weekly Indicators from Saved Checklists**: Drive the 7-day weekly indicator strip on `HomeScreen` and `PlanScreen` from each day's saved `daily_checklists` record (`completedCount`, `totalCount`, and whether at least one task/attempt was completed), rather than raw `word_progress.last_reviewed_timestamp` queries.
  5. **Forward-Compatibility Guardrails (Direction Only — Non-Blocking)**:
     - Keep `DailyTaskItem` metadata extensible (`linkedLessonId`, `linkedReadingId`, `reason`, `estimatedMinutes`, `backlogRemaining`) and keep `entries.effective_level` + `attempts` clean so future work ("Today's Lesson" bundled reading+words+grammar, vocabulary stages map per book, rule-based weekly insights, user-confirmed level-up suggestion bar, and book/topic onboarding questions) plugs in without schema rewrites.

- **Files to Modify / Create**:
  - `app/src/main/java/com/example/data/StudyPlanRepository.kt` (time-budget allocator, `task_correction_reviews` & `task_sentence_practice` scheduling, `DailyTaskItem.reason` & `backlogRemaining`, dynamic `refreshChecklistCompletion`, weekly checklist summary helper, remove dead `shouldScheduleOnDay` and unused `PlanDailyTemplate` duplication)
  - `app/src/main/java/com/example/ui/HomeScreen.kt` (remove `tasks.take(4)`, render `task.reason` line, replace `/ 20` with dynamic daily goal, bind weekly strip to saved checklists)
  - `app/src/main/java/com/example/ui/PlanScreen.kt` (render `task.reason`, time estimate, and backlog info; bind weekly strip to saved checklists)
  - `app/src/main/java/com/example/ui/WordBooksViewModel.kt` (expose dynamic daily goal and checklist-backed weekly status)
  - `app/src/test/java/com/example/Phase3DailyPlanTest.kt` (new unit/Robolectric tests for time-budget scaling, backlog reduction reasons, correction review inclusion, Home task parity, and weekly checklist indicators)

- **Schema & Migration**:
  - No additional Room table migration required beyond Phase 2 (`DailyTaskItem` is serialized via `kotlinx.serialization` in `daily_checklists.tasks_json` with default values for new fields `reason: String = ""`, `estimatedMinutes: Int = 1`, `backlogRemaining: Int = 0` so existing saved checklists deserialize seamlessly).

- **Tests**:
  - `Phase3DailyPlanTest.kt`: verifies time-budget math across `5`, `15`, and `30` min profiles; verifies that a 42-card review backlog reduces new words and populates the exact `"Fewer new words today: 42 reviews are due"` reason; verifies due correction reviews and rotating sentence practice appear; verifies `HomeScreen` displays all tasks without `.take(4)` truncation and dynamic goal instead of `/ 20`; verifies weekly indicators match `daily_checklists`.

- **Risks & Mitigations**:
  - *Deserializing existing `daily_checklists.tasks_json` rows*: All new fields on `DailyTaskItem` will have default values and `Json { ignoreUnknownKeys = true; encodeDefaults = true }` so existing checklist rows in Room load without error.

- **What You Will See on Screen**:
  - **Home & Plan Task Cards**: Every task shows its estimated time and a one-line data-backed reason (e.g., `"Fewer new words today: 42 reviews are due"` or `"3 saved sentence corrections due now"`).
  - **No Hidden Tasks on Home**: If today's plan has 5 tasks, all 5 appear in the Home card list—matching the `0/5` badge at the top of the card.
  - **Dynamic Daily Goal & Honest Weekly Strip**: The fixed `/ 20` label is replaced by today's actual planned target, and the 7-day strip reflects each day's saved checklist state.
