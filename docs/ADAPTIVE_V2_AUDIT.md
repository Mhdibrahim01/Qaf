# Qaf (قاف) Adaptive V2: Unified Independent Verification & Audit Report

**Audit Date:** October 24, 2026  
**Auditor:** Antigravity Independent Investigation Agent (Second-Pass Deep Audit)  
**Audit Branch:** `audit/adaptive-v2` (Base commit: `0d1a81f Merge branch 'main' of https://github.com/Mhdibrahim01/Wordbooksv2 into feat/audio-pronunciation-turtle-mode`)  
**Database Schema Version:** Room `v16` (`app/schemas/com.example.data.local.WordBooksDatabase/16.json`)  
**Mode:** READ-ONLY Verification (Zero source code, data, or configuration fixes applied)

---

## 1. Executive Summary & Top Findings

### Final Verdict: **DO NOT MERGE TO PRODUCTION WITHOUT FIXES**

While the Adaptive V2 branch delivers essential architectural milestones—including Room schema `v16`, 16 new A1/A2 micro-stories (`r001`–`r016`), centralized `AdaptiveConfig.kt`, and `LearnerModelRepository.kt`—the implementation contains **critical progression blockers, dead code, logic regressions, and inaccurate metric pipelines**. 

Crucially, this second-pass audit **challenges and corrects several misconceptions from the prior investigation**:
1. The prior claim that the 3-tier overdue prioritization query was missing was **false**: `WordBooksDao.kt:1920-1943` implements the exact 3-tier algorithm. The real flaw is that `WordBooksViewModel.kt:1161` re-sorts the prioritized results alphabetically (`sortGroupedCards`), destroying the tier ordering in the UI.
2. The prior claim that story pre-teaching is completely dead code was an **exaggeration**: while `reading_word_links.json` has a deserialization key mismatch, `StudyPlanRepository.kt:941-946` and `LearnerModelRepository.kt:288-296` contain functional fallbacks to `story.targetWordsJson`, allowing pre-teaching to function for stories with embedded target words.
3. The prior claim that only flashcards failed to record `elapsed_ms` was **understated**: grammar quizzes (`GrammarRepository.kt:374`), reading quizzes (`ReadingRepository.kt:306`), sentence practice, and mini-quizzes *all* omit `elapsedMs`, rendering attempt duration tracking 100% dead code across the entire application.

---

### Top 5 Critical Findings

1. **Infinite Grammar Refresher Loop ("Groundhog Day" Progression Trap) (`P0`):**  
   [`StudyPlanRepository.kt:555-570`](file:///c:/Users/EGYPT_LAPTOP/Downloads/wordbooks/app/src/main/java/com/example/data/plan/StudyPlanRepository.kt#L555-L570) iterates `levelGrammarLessons` in ascending ID order (`g001`, `g002`, ...). If a learner scores `<65%` on `g001` or accumulates $\ge 3$ active notebook errors linked to it, `g001` is scheduled as a refresher task every single day. Because retaking a lesson only updates `bestScore` if the new score is strictly higher ([`GrammarRepository.kt:364`](file:///c:/Users/EGYPT_LAPTOP/Downloads/wordbooks/app/src/main/java/com/example/data/grammar/GrammarRepository.kt#L364)), and there is **no cooldown period, retry cap, or backoff**, struggling learners are trapped on `g001` indefinitely, halting linear syllabus advancement.
2. **`reading_word_links.json` Deserialization Key Mismatch (`P1`):**  
   [`tools/build_reading_word_links.py:815`](file:///c:/Users/EGYPT_LAPTOP/Downloads/wordbooks/tools/build_reading_word_links.py#L815) outputs `"text_id": story["id"]`, but [`LearnerModelRepository.kt:166`](file:///c:/Users/EGYPT_LAPTOP/Downloads/wordbooks/app/src/main/java/com/example/data/plan/LearnerModelRepository.kt#L166) declared `val id: String = ""` without `@SerialName("text_id")`. Kotlinx Serialization defaults `it.id` to `""`, so `getReadingWordLinks()[story.id]` returns `null` for all 87 reading passages. This completely bypasses the 146 KB precomputed link asset, forcing runtime regex token extraction over passage text on the UI thread and ignoring the 20 pre-linked headwords.
3. **`elapsed_ms` Is Never Passed for ANY Activity — Personal Pace EMA Is 100% Dead Code (`P1`):**  
   [`WordBooksViewModel.kt:1319`](file:///c:/Users/EGYPT_LAPTOP/Downloads/wordbooks/app/src/main/java/com/example/ui/viewmodel/WordBooksViewModel.kt#L1319) calls `applySm2Review(...)` omitting `elapsedMs`, causing [`DataImportRepository.kt:526`](file:///c:/Users/EGYPT_LAPTOP/Downloads/wordbooks/app/src/main/java/com/example/data/importer/DataImportRepository.kt#L526) to default it to `0L`. Furthermore, grammar quiz attempts ([`GrammarRepository.kt:374`](file:///c:/Users/EGYPT_LAPTOP/Downloads/wordbooks/app/src/main/java/com/example/data/grammar/GrammarRepository.kt#L374)), reading quiz attempts ([`ReadingRepository.kt:306`](file:///c:/Users/EGYPT_LAPTOP/Downloads/wordbooks/app/src/main/java/com/example/data/reading/ReadingRepository.kt#L306)), and sentence practice *all* create `AttemptEntity` instances with `elapsedMs = null`. [`LearnerModelRepository.kt:459`](file:///c:/Users/EGYPT_LAPTOP/Downloads/wordbooks/app/src/main/java/com/example/data/plan/LearnerModelRepository.kt#L459) filters for `elapsedMs in 1500L..60000L`. Since all attempts record `0L` or `null`, `validElapsedList` is permanently empty, locking `personalPaceSeconds` to the hardcoded `20.0s` fallback constant forever.
4. **Lapsed Mastered & Mature Words Stage Classification Defect (`P1`):**  
   - In [`Sm2Algorithm.kt:153`](file:///c:/Users/EGYPT_LAPTOP/Downloads/wordbooks/app/src/main/java/com/example/data/importer/Sm2Algorithm.kt#L153), rating `Sm2Rating.AGAIN` resets `repetitions = 0` and `intervalDays = 0`, but fails to reset `isMastered = false`. In [`LearnerModelRepository.kt:220`](file:///c:/Users/EGYPT_LAPTOP/Downloads/wordbooks/app/src/main/java/com/example/data/plan/LearnerModelRepository.kt#L220), `classifyWordStage` checks `if (progress.isMastered || progress.intervalDays >= 21) return MATURE`. When a user fails a previously mastered card, it remains classified as `MATURE`.
   - Even when `isMastered` is `false`, [`LearnerModelRepository.kt:226`](file:///c:/Users/EGYPT_LAPTOP/Downloads/wordbooks/app/src/main/java/com/example/data/plan/LearnerModelRepository.kt#L226) checks `if (progress.firstLearnedTimestamp > 0L || progress.repetitions > 0 || progress.lastReviewedTimestamp > 0L) return LEARNING`. Because any previously studied card has `firstLearnedTimestamp > 0L`, lapsed cards (`repetitions = 0`) are classified as `LEARNING` instead of `SEEN`. In fact, `WordStage.SEEN` is unreachable for any card that has ever been reviewed in flashcards.
5. **Lifecycle Funnel Multi-Deck Double-Counting & Pool Underflow (`P1`):**  
   [`LearnerModelRepository.kt:372-384`](file:///c:/Users/EGYPT_LAPTOP/Downloads/wordbooks/app/src/main/java/com/example/data/plan/LearnerModelRepository.kt#L372-L384) iterates every row in `word_progress` across all decks without deduplicating by headword or filtering by `selectedDecks`. Words in multiple decks (e.g. Oxford 5000 + IELTS) increment the funnel 2–3 times. Furthermore, progress rows from unselected decks leak into `activeStudiedCount`, exceeding `totalWords` and forcing `newCount = 0`.

---

## 2. Step 0: Environment Verification

| Item | Expected State | Actual Verified State | Status | Evidence / Notes |
| :--- | :--- | :--- | :---: | :--- |
| **Git Branch** | `audit/adaptive-v2` | `audit/adaptive-v2` | **Pass** | Verified via `git status`. |
| **Latest Commit** | Clean HEAD | `0d1a81f` | **Pass** | `0d1a81f Merge branch 'main' ... into feat/audio-pronunciation-turtle-mode`. |
| **Database Schema** | Room `v16` | Version 16 registered | **Pass** | `WordBooksDatabase.kt:33` registers `version = 16`, exportSchema = true. `app/schemas/com.example.data.local.WordBooksDatabase/16.json` exists. |
| **Migration Script** | `MIGRATION_15_16` | Present & Registered | **Pass** | `WordBooksDatabase.kt:244-266` alters `word_progress`, `attempts`, and `study_plan_profile`. Registered in `Room.databaseBuilder` at line 304. |
| **Offline Tooling** | `tools/*.py` | 2 scripts present | **Partial** | `tools/assign_cefr_levels.py` (106 lines) and `tools/build_reading_word_links.py` (837 lines) exist. However, `assign_cefr_levels.py` only generated a markdown table and never updated database/assets. |
| **Curriculum Data** | 87 Stories & Word Links | Assets present | **Partial** | `reading_texts.json` has 87 stories (including `r001`–`r016`). `reading_word_links.json` (146 KB) present in assets, but serialization mismatch disables it. |
| **Adaptive Modules** | `AdaptiveConfig` & `LearnerModelRepository` | Present | **Pass** | `AdaptiveConfig.kt` (126 lines) and `LearnerModelRepository.kt` (764 lines) compiled. |
| **Backup / Restore UI** | `MoreScreen.kt` & `SettingsScreen.kt` | Partially Implemented | **Partial** | SAF launchers wired, but **missing user confirmation dialog** before destructive overwrite. |

---

## 3. Step 1: Baseline Build, Test & Lint Verification

### 3.1 Unit Test Execution (`./gradlew.bat testDebugUnitTest`)
- **Total Tests Completed:** 93
- **Passed:** 91
- **Failed:** 2

#### Detailed Failure Analysis:
1. `Phase3DailyPlanTest > testSentencePracticeRotationForA1AndA2Levels FAILED`  
   - **Stack Trace:** `java.lang.AssertionError: Sentence practice task must be scheduled for A2 user at Phase3DailyPlanTest.kt:257`  
   - **Verdict:** **STALE TEST**. In Adaptive V1, `reading_texts.json` lacked A1/A2 stories, so `preferSentencePractice` forced A1/A2 users onto sentence practice. Adaptive V2 introduced 16 A1/A2 micro-stories (`r001`–`r016`). In `StudyPlanRepository.kt:589-593`, `hasReadingForUserLevel` now evaluates to `true`. Thus, the scheduler correctly assigns a reading task instead of sentence practice. The test assertion is obsolete.
2. `DatabaseMigration13To15Test > migrationToV15PreservesAllUserDataBackfillsFirstLearnedTimestampOnlyForRatedCardsAndCreatesEmptyAttemptsTable FAILED`  
   - **Stack Trace:** `android.database.sqlite.SQLiteCantOpenDatabaseException at DatabaseMigration13To15Test.kt:49`  
   - **Verdict:** **STALE TEST SETUP**. The test attempted to instantiate `WordBooksDatabase` using `Room.databaseBuilder` which defaults to schema v16 without registering `MIGRATION_15_16` in the test setup.

### 3.2 Android Lint Execution (`./gradlew.bat lintDebug`)
- **Errors:** 2 (`NewApi`)
- **Warnings:** 89

#### Lint Errors:
- [`app/src/main/java/com/example/data/ai/BatchTranslateManager.kt:882`](file:///c:/Users/EGYPT_LAPTOP/Downloads/wordbooks/app/src/main/java/com/example/data/ai/BatchTranslateManager.kt#L882): `val today = LocalDate.now().toString()` [NewApi]
- [`app/src/main/java/com/example/data/ai/BatchTranslateManager.kt:896`](file:///c:/Users/EGYPT_LAPTOP/Downloads/wordbooks/app/src/main/java/com/example/data/ai/BatchTranslateManager.kt#L896): `val today = LocalDate.now().toString()` [NewApi]  
*Impact:* App `minSdk = 24`, while `java.time.LocalDate` requires API level 26. Because `coreLibraryDesugaring` is **not enabled** in `app/build.gradle.kts:66-69`, executing batch translation on Android 7.0 or 7.1 devices will cause a fatal `NoClassDefFoundError` crash.

---

## 4. Step 2: Master Plan vs. Codebase Matrix

| Plan Item / Capability | Plan Specification | Codebase Reality | Status | Evidence (File & Line) |
| :--- | :--- | :--- | :---: | :--- |
| **Room Schema v16** | 6 columns added to `word_progress`, `attempts`, `study_plan_profile` | All 6 columns added in `MIGRATION_15_16` | **Done** | `WordBooksDatabase.kt:244-266` |
| **A1/A2 Micro-Stories** | 16 graded passages (`r001`–`r016`) across 8 topics | 16 passages present with Arabic summaries; 2 questions each | **Done** | `assets/reading_texts.json:1-1250` |
| **`reading_word_links.json`** | Maps passages to vocabulary & macro-topics | JSON generated; key mismatch (`text_id` vs `id`) causes null lookup | **Broken (P1)** | `build_reading_word_links.py:815`<br>`LearnerModelRepository.kt:166` |
| **`personalPaceSeconds` EMA** | EMA ($\alpha=0.20$) over attempt durations `[1.5s..60s]` | `elapsed_ms` never passed for ANY activity (0L or null) | **Dead Code (P1)** | `WordBooksViewModel.kt:1319`<br>`GrammarRepository.kt:374` |
| **Lifecycle Stages (5+1)** | Mutually exclusive: NEW, SEEN, LEARNING, FAMILIAR, MATURE + CONQUERED | Lapsed cards classified as LEARNING/MATURE; multi-deck double-counting | **Broken (P1)** | `Sm2Algorithm.kt:153`<br>`LearnerModelRepository.kt:220, 372` |
| **CEFR Readiness Vocab** | 50% weight, level target thresholds ($N_L = 300..600$) | Hardcoded `levelTargetWords = 120`, global unlevelled sum | **Broken (P1)** | `LearnerModelRepository.kt:532-536` |
| **Grammar Remediation** | Refresher for score `<65%` or $\ge 3$ active notebook items | Groundhog Day loop: no cooldown, retry cap, or backoff | **Broken (P0)** | `StudyPlanRepository.kt:555-570` |
| **Leech Flagging** | Accumulates $\ge 3$ lapses within 14-day window | Lifetime lapses used; fake `grammarLessonId = "leech_vocab"` | **Broken (P1)** | `DataImportRepository.kt:584-597` |
| **Specialty Deck CEFR** | Populate `effective_level` for all 21,624 rows | Script only printed markdown; 5,324 rows still NULL | **Missing (P1)** | `assign_cefr_levels.py:98`<br>`docs/DECK_LEVEL_AUDIT.md:1-18` |
| **Dynamic New Words** | Scale up (+3) on `>88%` acc; scale down on `>25` due | Scale-up missing; scale-down allows 3 words (plan min 5) | **Broken (P1)** | `AdaptiveConfig.kt:20`<br>`StudyPlanRepository.kt:526-535` |
| **3-Tier Overdue Queue** | Tier 0 -> Tier 1 -> Tier 2 Overdueness ratio; cap 30 | SQL query exists in DAO, but ViewModel re-sorts alphabetically | **Broken (P2)** | `WordBooksDao.kt:1920-1943`<br>`WordBooksViewModel.kt:1161` |
| **Quiet / No-Mic Mode** | Disable mic prompts; fallback to tile assembly | Stored in DB, but completely ignored in `SentencesScreen.kt` | **Broken (P2)** | `SentencesScreen.kt:1341-1440` |
| **JSON Backup Confirm** | Confirmation dialog and record summary on restore | Missing dialog; database overwritten immediately on file select | **Broken (P2)** | `MoreScreen.kt:107-115`<br>`SettingsScreen.kt:160-165` |
| **Data Version Upgrade** | Bump `CURRENT_DATA_VERSION = 7` | `DataImportRepository.DATA_VERSION` remains 4 | **Missing (P2)** | `DataImportRepository.kt:305` |

---

### Step 2b: Comprehensive `AdaptiveConfig.kt` Constants Audit

| Constant Name | Plan Table Value | Actual Code Value | Status | Discrepancy & Governing Behavior |
| :--- | :---: | :---: | :---: | :--- |
| `COLD_START_MIN_ATTEMPTS` | `20` | `20` | **Match** | `AdaptiveConfig.kt:23`. |
| `COLD_START_MIN_SESSIONS` | `2` | *Missing* | **Missing** | Not in `AdaptiveConfig.kt`. Code only checks attempt count < 20. |
| `OVERDUE_CAP_MAX` | `30` | `30` | **Deviates** | Declared in `AdaptiveConfig.kt:20`, but **ignored** in `StudyPlanRepository.kt:440-445` (hardcodes 25/50/75/100). |
| `NEW_WORDS_MIN` / `MAX` | `5` / `20` | *Missing* | **Deviates** | Not in `AdaptiveConfig.kt`. `StudyPlanRepository.kt:528` allows 3 words. |
| `BACKLOG_HEAVY_THRESHOLD` | `25` | `15` | **Inconsistent** | Declared as `15` in `AdaptiveConfig.kt:21`, but `StudyPlanRepository.kt:526` checks `totalDue >= 25`. |
| `ACCURACY_FAST_TRACK_THRESHOLD`| `0.88f` | *Missing* | **Missing** | Missing from config and scheduler; +3 scaling never occurs. |
| `STORY_VOCAB_PRETEACH_MIN/MAX` | `4` / `6` | `4` / `6` | **Deviates** | Declared in config (lines 38-39), but `StudyPlanRepository.kt:687` hardcodes `maxCount = minOf(4, newWordsTarget / 2)`. |
| `STORY_READABILITY_BAND_MIN/MAX`| `80` / `92` | `80` / `92` | **Match** | `AdaptiveConfig.kt:33-34`. |
| `LEECH_LAPSE_THRESHOLD` | `3` | `3` | **Match** | `AdaptiveConfig.kt:15`. |
| `LEECH_WINDOW_DAYS` | `14` | *Missing* | **Missing** | Not declared; code checks lifetime `againCount >= 3`. |
| `GRAMMAR_REMEDIATION_SCORE` | `65` | `65` | **Match** | `AdaptiveConfig.kt:16`. |
| `GRAMMAR_REMEDIATION_NOTEBOOK`| `3` | `3` | **Match** | `AdaptiveConfig.kt:17`. |
| `LEVEL_UP_READINESS_THRESHOLD` | `80%` | `80` | **Match** | `AdaptiveConfig.kt:45`. |
| `STRUGGLE_DOWNSHIFT_ACCURACY` | `0.55f` | *Missing* | **Missing** | Not in `AdaptiveConfig.kt` or `LearnerModelRepository.kt`. |
| `DEFAULT_SECONDS_PER_WORD` | `12f` (flashcard) | `20f` (global) | **Deviates** | Plan specified 12s flashcards, 25s sentences, 180s grammar, 150s reading. Code uses global 20s. |

---

## 5. Step 3: Correctness Deep-Dives (15 Specific Suspicions)

### 1. Word Identity & Funnel Double-Counting (`P1`)
- **Keying:** Rows in `word_progress` are keyed by `card_key` (`deck:text`). If a headword exists in multiple decks (e.g. "ability" in `oxford5000` and `ielts_core`), there are multiple distinct rows.
- **Funnel Calculation:** [`LearnerModelRepository.kt:372-384`](file:///c:/Users/EGYPT_LAPTOP/Downloads/wordbooks/app/src/main/java/com/example/data/plan/LearnerModelRepository.kt#L372-L384) iterates `allProgress` without deduplicating by headword or filtering by `selectedDecks`.
- **Impact:** Duplicate headwords increment `seenCount`, `learningCount`, `familiarCount`, and `matureCount` multiple times. Progress from unselected decks leaks into `activeStudiedCount`, exceeding `totalWords` and forcing `newCount = 0`. In `cefrReadinessPercent`, `familiarOrMatureTotal` sums rows across all decks and CEFR levels.

### 2. "New Word Learned Today" & Seeded Legacy Data (`Pass`)
- **Investigation:** Plan Section 2.2 #4 proposed querying `attempts` where no prior attempt existed. Since `attempts` was created empty in migration 14->15, a naive attempt query would have miscounted old rated cards as new words.
- **Actual Code:** [`WordBooksDao.kt:1446-1450`](file:///c:/Users/EGYPT_LAPTOP/Downloads/wordbooks/app/src/main/java/com/example/data/local/WordBooksDao.kt#L1446-L1450) queries `first_learned_timestamp >= :startOfDayMillis`. In `MIGRATION_14_15`, `first_learned_timestamp` was backfilled with `last_reviewed_timestamp` for all cards with `repetitions > 0 || again_count > 0`.
- **Verdict:** Old rated cards do NOT count as new words when reviewed today. Only truly unrated cards (or favorite-only rows) receive `firstLearnedTimestamp = nowMillis` on first rating. The implementation is correct and superior to the plan's naive specification.

### 3. Stage Transitions, Lapses, and CONQUERED Milestone (`P1`)
- **Lapsed Mastered Cards:** In [`Sm2Algorithm.kt:153`](file:///c:/Users/EGYPT_LAPTOP/Downloads/wordbooks/app/src/main/java/com/example/data/importer/Sm2Algorithm.kt#L153), rating `AGAIN` resets `repetitions = 0` and `intervalDays = 0`, but preserves `isMastered = true`. In [`LearnerModelRepository.kt:220`](file:///c:/Users/EGYPT_LAPTOP/Downloads/wordbooks/app/src/main/java/com/example/data/plan/LearnerModelRepository.kt#L220), `if (progress.isMastered || progress.intervalDays >= 21) return MATURE`. A card failed 10 seconds ago remains `MATURE`.
- **Lapsed Non-Mastered Cards:** Even when `isMastered` is false, line 226 checks `if (progress.firstLearnedTimestamp > 0L || ...) return LEARNING`. Since `firstLearnedTimestamp > 0L` is true for every studied card, lapsed cards are classified as `LEARNING`, never `SEEN`. The plan's rule that "a MATURE word that lapses becomes SEEN" is violated.
- **CONQUERED Milestone:** Correctly set when `againCount >= 2` and `intervalDays >= 21`. Backfill in `MIGRATION_15_16` is correct. However, if a conquered card subsequently lapses to interval 0, [`LearnerModelRepository.kt:242`](file:///c:/Users/EGYPT_LAPTOP/Downloads/wordbooks/app/src/main/java/com/example/data/plan/LearnerModelRepository.kt#L242) still treats it as conquered because `conqueredTimestamp > 0L`.

### 4. Leech Rule & Correction Notebook Enrolment (`P1`)
- **Time Window:** [`DataImportRepository.kt:584`](file:///c:/Users/EGYPT_LAPTOP/Downloads/wordbooks/app/src/main/java/com/example/data/importer/DataImportRepository.kt#L584) checks `updated.againCount >= 3`. This is lifetime lapses, completely ignoring `LEECH_WINDOW_DAYS` (14 days).
- **Schema & Meaning:** Auto-enrolling vocabulary into `correction_notebook` inserts a single word as `originalSentence` and a dictionary example as `correctedSentence`. This distorts sentence practice UI with vocabulary fragments.
- **Remediation Link:** Enrolls with `grammarLessonId = "leech_vocab"`. In [`StudyPlanRepository.kt:558`](file:///c:/Users/EGYPT_LAPTOP/Downloads/wordbooks/app/src/main/java/com/example/data/plan/StudyPlanRepository.kt#L558), grammar lessons iterate `g001`–`g090`. `"leech_vocab"` never matches any grammar lesson, so grammar remediation never triggers for leeches.

### 5. Story Selection on Day 1 & Readability Denominator (`P2`)
- **Day 1 Behavior:** On Day 1, with 0 known words, [`LearnerModelRepository.kt:327`](file:///c:/Users/EGYPT_LAPTOP/Downloads/wordbooks/app/src/main/java/com/example/data/plan/LearnerModelRepository.kt#L327) blends 45% raw coverage with 55% `levelBaseline` (84% for matching CEFR rank), clamped to `[50, 99]`. Every A1 story reports exactly **50% Readable** (below the 80–92% band). However, because `score` starts at `Int.MIN_VALUE`, `r001` is selected via grammar synergy (+25 pts) and topic match (+40 pts). Selection is deterministic and never empty.
- **Denominator:** Bounded by `contentTokens.size`. When `reading_word_links.json` fails, it falls back to regex matching on `sentencesJson`, which includes JSON structural keys as content tokens if not in stop-words.

### 6. Story Pre-Teaching Filters & Quotas (`P2`)
- **Deck & Level Filtering:** In [`StudyPlanRepository.kt:959-963`](file:///c:/Users/EGYPT_LAPTOP/Downloads/wordbooks/app/src/main/java/com/example/data/plan/StudyPlanRepository.kt#L959-L963), if `selectedDecks` have fewer than `maxCount` matches, it falls back to `dao.getEntriesByHeadwords(unstudiedFocus)` across all decks, pulling in unselected deck words.
- **Quotas:** Line 687 hardcodes `maxCount = minOf(4, newWordsTarget / 2)`, capping pre-teaching at 4 words even when `newWordsTarget = 20` (plan specified 4–6 words).
- **Deduplication:** Correctly excludes `preTeachHeadwords` from general new-word selection (line 692).

### 7. Due Selection, Home Badge vs. Plan Count, and 3-Tier Sort (`P2`)
- **Home Badge vs. Plan:** Home displays `reviewDueTodayCount` (uncapped due cards, e.g. 120). Plan task displays `flashcardTarget` (capped at 25/50/75/100, e.g. 50). They are intentionally different metrics: backlog vs. scheduled batch.
- **3-Tier Sort:** Implemented in [`WordBooksDao.kt:1920-1943`](file:///c:/Users/EGYPT_LAPTOP/Downloads/wordbooks/app/src/main/java/com/example/data/local/WordBooksDao.kt#L1920-L1943), but [`WordBooksViewModel.kt:1161`](file:///c:/Users/EGYPT_LAPTOP/Downloads/wordbooks/app/src/main/java/com/example/ui/viewmodel/WordBooksViewModel.kt#L1161) re-sorts cards by `filterState.sortOrder` (default AZ), wiping out the tier prioritization during study sessions!

### 8. Rule Precedence, Fast-Track Scaling & Bounds (`P1`)
- **Precedence Order:** In `StudyPlanRepository.kt:527-535`: (1) `isRestDay` -> 3, (2) `isReviewsHeavy` (`totalDue >= 25`) -> `maxOf(3, base * 0.6)`, (3) `isAdjustedAfterMissedDay` -> `maxOf(3, base / 2)`, (4) `baseNewWords`.
- **Fast-Track Scaling (+3):** Completely missing from codebase.
- **Bounds Violation:** Allows 3 words, violating the plan's hard minimum of `NEW_WORDS_MIN = 5`.

### 9. Grammar Remediation Groundhog Day Loop (`P0`)
- Proved in Finding 1. As long as `bestScore < 65%` or active corrections $\ge 3$, `g001` is scheduled every single day. No cooldown, no retry counter, no backoff.

### 10. A1/A2 Stories Schema & Passage Coverage (`P2`)
- **Schema Discrepancy:** All 16 micro-stories (`r001`–`r016`) have **only 2 comprehension questions each**, whereas all B1–C1 stories have 5 questions. All 16 stories have Arabic translations for every sentence, grammar IDs, and target words.
- **A2 Lessons `g017`–`g029`:** Handled cleanly via hybrid scoring over `r009`–`r016`.
- **Links JSON Coverage:** Covers all 87 passages (verified via Python script).

### 11. `effective_level` Script & Specialty Deck Exclusion (`P1`)
- `tools/assign_cefr_levels.py` only produced `docs/DECK_LEVEL_AUDIT.md`. Exactly 5,324 entries have `effective_level = NULL`.
- In `StudyPlanRepository.kt:1407-1411`, A1/A2 learners have `nullLevelByDeck = emptyList()`, permanently locking them out of all 5,324 specialty terms.

### 12. Migration 15 to 16 & Elapsed Time Tracking (`P1`)
- **Entity Defaults:** Match SQL defaults in `MIGRATION_15_16` exactly.
- **Testing:** Tested in `AdaptiveV2VerificationTest.kt:78` using hand-rolled SQLite callbacks. Does NOT use `MigrationTestHelper` or validate all 20 tables.
- **`elapsed_ms`:** Never captured from UI timers for flashcards, grammar, reading, or sentences.

### 13. Backup & Restore Round-Trip (`P2`)
- **Coverage:** Round trip preserves `wordProgress`, `attempts`, `studyPlanProfile`, `studyPlans`, `dailyChecklists`, `quizResults`. Verified in `AdaptiveV2VerificationTest.kt:205`. No API keys exported.
- **Safety Risk:** Missing confirmation dialog in `MoreScreen.kt:107-115` and `SettingsScreen.kt:160-165`. Restore does not wipe existing tables before upserting.

### 14. Metrics & CEFR Readiness Edge Cases (`P1`)
- **Readiness Hardcoding:** [`LearnerModelRepository.kt:532`](file:///c:/Users/EGYPT_LAPTOP/Downloads/wordbooks/app/src/main/java/com/example/data/plan/LearnerModelRepository.kt#L532) hardcodes `levelTargetWords = 120` across all levels (plan: 300 to 600). Counts global unlevelled words, granting instant 100% vocab readiness upon level-up.
- **Banner Safety:** Level-up banner never changes level automatically (`WordBooksViewModel.kt:2464` requires user tap).
- **Insight Cooldown:** Persists to `study_plan_profile.dismissed_insights_json` with 7-day cooldown.

### 15. Onboarding Additions & Quiet Mode (`P2`)
- **Quiet Mode in UI:** Stored in DataStore and Room profile, but [`SentencesScreen.kt`](file:///c:/Users/EGYPT_LAPTOP/Downloads/wordbooks/app/src/main/java/com/example/ui/screens/SentencesScreen.kt) has zero references to `quietModeEnabled`. Microphone prompt is always shown; tile-assembly fallback is absent.
- **Redo Setup:** DataStore preserves topics and quiet mode across Redo Setup, but `startRedoSetup` omits copying them from an existing Room profile.

---

## 6. Step 4: Persona & Scenario Behavior Simulations

| Scenario | Input Profile | Expected Behavior (Plan) | Actual Behavior (Code) | Verdict |
| :--- | :--- | :--- | :--- | :---: |
| **Day 1 Fresh Install** | A1, 30m, `oxford5000` | Cold start banner; 10 new words (4–6 from `r001`); `g001`; `r001` | Cold start banner shown; 10 new words scheduled with fallback pre-teaching; `g001`; `r001`; reports 50% readability | **Partial** |
| **30-Day Consistent Learner** | B1, 30m, 92% Acc, 8 due | Fast-track scaling (+3 words -> 13 words); dynamic pace EMA | Fast-track missing (10 words); pace locked to 20s (all elapsed 0L); progresses unless prior lesson <65% | **Degraded** |
| **Returning Learner (14d Break)**| B2, 30m, 120 due cards | Cap at 30 priority reviews; new words downshift to 5; 3-tier sort | Schedules 50 cards; new words drop to 3 (below min 5); queue sorted alphabetically | **Broken** |
| **150 Due Cards (45m Profile)** | B2, 45m, 150 due cards | Cap at 30 priority reviews; <=45% review time budget | Schedules 75 cards (taking 13 min of review time) | **Broken** |
| **Exhausted Reading Level** | C1, all stories completed | Replaces reading task with 3-sentence practice task | `hasReadingForUserLevel = false` triggers `preferSentencePractice = true`; schedules sentence task | **Pass** |
| **Single Deck (Medical, 405w)** | A2, `medical_healthcare` | Learns foundational medical words | 170 medical words have `effective_level = NULL`; A2 user receives only 235 Oxford words | **Broken** |
| **Persona 2 (Omar — IT Deck)** | A2, 30m, `it_industry` | Receives authentic tech terms | 429 IT words are NULL level; Omar starved of tech terms and receives generic words | **Broken** |

---

## 7. Step 5: Performance & Indexing Analysis

1. **Full-Table In-Memory Deserialization:**  
   [`LearnerModelRepository.kt:360`](file:///c:/Users/EGYPT_LAPTOP/Downloads/wordbooks/app/src/main/java/com/example/data/plan/LearnerModelRepository.kt#L360) calls `dao.getAllWordProgressList()` on every snapshot build. In [`WordBooksViewModel.kt:2012`](file:///c:/Users/EGYPT_LAPTOP/Downloads/wordbooks/app/src/main/java/com/example/ui/viewmodel/WordBooksViewModel.kt#L2012), `buildSnapshot()` is invoked on **every single word tap in a reading passage**. Tapping 6 words deserializes the entire table 6 times into memory.
2. **Missing Composite Index on `word_progress`:**  
   `getDueTodayEntriesForDecksAdaptiveOrdered` filters `(is_mastered = 0 AND due_timestamp > 0 AND due_timestamp <= :endOfTodayMillis AND deck IN (:decks))`. The existing index is `(is_mastered, due_timestamp)` without `deck`.
3. **UI Thread Regex Parsing:**  
   `extractContentTokens` executes regex pattern matching on the full passage JSON string on every readability calculation due to null `linkItem` lookups.

---

## 8. Step 6: UI, BiDi & Design Review

1. **BiDi Parentheses Isolation (`BidiText.kt`):**  
   `isolateLatinInRtlLine` wraps outer parentheses inside the LTR isolate block `\u2066...\u2069` on mixed English/Arabic lines, causing reversed parenthesis rendering on older Android WebView engines.
2. **Quiet Mode Accessibility:**  
   Users enabling Quiet Mode for public transport or quiet environments are still presented with an active microphone prompt in `SentencesScreen.kt` that stalls without audio input.
3. **Screen Reader Support:**  
   Interactive elements correctly expose `testTag` semantics (`screen_home`, `card_practice_accuracy_summary`, `btn_restore_backup`, `analytics_list`).

---

## 9. Step 7: Security & Release Hygiene

1. **Catastrophic Accidental Data Loss Risk (`P2`):**  
   Selecting a JSON backup file in `MoreScreen.kt:107` immediately overwrites the database with zero confirmation dialog.
2. **Hardcoded Fallback Signing Passwords:**  
   [`app/build.gradle.kts:33-35`](file:///c:/Users/EGYPT_LAPTOP/Downloads/wordbooks/app/build.gradle.kts#L33-L35) falls back to `"wordbooks2026"` in release signing config if environment variables are unset.
3. **Application ID Suffix:**  
   `app/build.gradle.kts:18` uses AI Studio auto-generated ID: `com.aistudio.wordbooks.nxvkqp`.
4. **API Key Security:**  
   No active Gemini API key literals found in source tree. Backups cleanly omit API keys.

---

## 10. Step 8: Test Quality & Behavioral Integrity

- **Test Suite Status:** 91 passed, 2 failed (both due to stale assertions rather than underlying bugs).
- **Behavioral Testing Gaps:**
  1. *Closed-Loop Pacing:* No test verified that `WordBooksViewModel.applySm2Review` actually logs non-zero `elapsed_ms`.
  2. *Refresher Progression:* `AdaptiveV2VerificationTest.kt:547` tests that `<65%` triggers a refresher on Day 1, but never tests Day 2/3, missing the Groundhog Day loop.
  3. *Tier Order Preservation:* No test verified that `sortGroupedCards` preserves 3-tier overdue queue order.

---

## 11. Prioritized Refinement Backlog

### P0 (Blockers)
| ID | Finding | Impact | Effort | Risk | Citation | Architectural Fix |
| :--- | :--- | :--- | :---: | :---: | :--- | :--- |
| **P0-1** | Infinite Grammar Refresher Loop | Learner is permanently trapped on a single grammar lesson; syllabus advancement blocked. | **S** | Low | `StudyPlanRepository.kt:555-570` | Add a 3-day cooldown timestamp and `maxConsecutiveRefreshers = 1` guard before re-scheduling the same refresher. |

### P1 (Pedagogy & Functional Defects)
| ID | Finding | Impact | Effort | Risk | Citation | Architectural Fix |
| :--- | :--- | :--- | :---: | :---: | :--- | :--- |
| **P1-1** | `reading_word_links.json` Key Mismatch | 100% of reading passages fail to load precomputed word links; forces UI-thread regex parsing. | **S** | Low | `LearnerModelRepository.kt:166` | Add `@SerialName("text_id") val id: String = ""` to `ReadingWordLinkItem`. |
| **P1-2** | `elapsed_ms` Never Passed for Any Activity | Personal pace EMA is 100% dead code; pacing locked to 20s fallback forever. | **M** | Med | `WordBooksViewModel.kt:1319`<br>`GrammarRepository.kt:374`<br>`ReadingRepository.kt:306` | Measure elapsed interaction duration in ViewModel/screens and pass into attempt loggers. |
| **P1-3** | Lapsed Mastered & Mature Stage Classification | Lapsed cards report MATURE or LEARNING instead of SEEN. | **S** | Low | `Sm2Algorithm.kt:153`<br>`LearnerModelRepository.kt:220, 226` | In `applyRating`, set `isMastered = false` on `AGAIN`. In `classifyWordStage`, require `repetitions >= 1` for `LEARNING`. |
| **P1-4** | Multi-Deck Funnel Double-Counting | Funnel counts duplicate headwords & unselected decks; `newCount` forced to 0. | **M** | Med | `LearnerModelRepository.kt:372-384, 443-456` | Deduplicate `allProgress` by headword, filter by `selectedDecks`, and derive `newCount` accurately. |
| **P1-5** | CEFR Vocab Coverage Calculation Bug | Target words hardcoded to 120; unlevelled global count grants instant 100% on level-up. | **S** | Low | `LearnerModelRepository.kt:532-536` | Use level thresholds ($N_{\text{A1}}=300..N_{\text{C1}}=400$) and count only words where `effective_level == userLevel`. |
| **P1-6** | Fast-Track (+3 words) Missing & Min Bound Violation | Daily new word scaling is missing; allows 3 words (plan min 5). | **M** | Med | `AdaptiveConfig.kt:20`<br>`StudyPlanRepository.kt:526-535` | Implement `ACCURACY_FAST_TRACK_THRESHOLD` (0.88f) and enforce `NEW_WORDS_MIN = 5`. |
| **P1-7** | Leech Coerces Vocab into Grammar | Pollutes correction notebook with `"leech_vocab"` and never triggers grammar remediation. | **M** | Med | `DataImportRepository.kt:584-599` | Separate vocab leeches from grammar corrections; check 14-day window for 3 lapses. |
| **P1-8** | Specialty Deck CEFR Levels Never Populated | 5,324 entries have `effective_level = NULL`; A1/A2 learners locked out of specialty decks. | **M** | Med | `tools/assign_cefr_levels.py:98`<br>`DataImportRepository.kt:305` | Run offline leveling script to populate `effective_level` into asset JSONs and bump `DATA_VERSION = 7`. |

### P2 (UX / Bug Fixes)
| ID | Finding | Impact | Effort | Risk | Citation | Architectural Fix |
| :--- | :--- | :--- | :---: | :---: | :--- | :--- |
| **P2-1** | 3-Tier Overdue Queue Destroyed by Re-Sorting | Overdue cards re-sorted alphabetically by ViewModel instead of tier priority. | **S** | Low | `WordBooksViewModel.kt:1161` | Exclude `DUE_TODAY_DECK_ID` from `sortGroupedCards`, preserving DAO tier order. Enforce `OVERDUE_CAP_MAX = 30`. |
| **P2-2** | Missing Confirmation Dialog on Restore | Accidental tap on JSON file immediately overwrites user database. | **S** | Low | `MoreScreen.kt:107-115`<br>`SettingsScreen.kt:160-165` | Show an `AlertDialog` summarizing backup payload stats with explicit "Confirm Restore" button. |
| **P2-3** | Quiet Mode Ignored in `SentencesScreen` | Microphone is always active; no tile-assembly alternative. | **M** | Med | `SentencesScreen.kt:1341-1440` | If `profile.quietModeEnabled == true`, hide mic UI and present word-order assembly chips. |
| **P2-4** | Lint `NewApi` Errors in `BatchTranslateManager` | Fatal crash on Android 7.0/7.1 devices running API 24/25. | **S** | Low | `BatchTranslateManager.kt:882, 896` | Replace `LocalDate.now()` with `Calendar.getInstance()` or enable core library desugaring. |
| **P2-5** | Level-Up Missing 3-Quiz Gate; Struggle Downshift Missing | Premature level-up banner; no assistance offered when struggling. | **S** | Low | `LearnerModelRepository.kt:664-712` | Require last 3 quizzes $\ge 80\%$; add struggle downshift banner when 7d accuracy < 55%. |
| **P2-6** | In-Story Live Word Highlighting Missing | Learners cannot see which story words reinforce their learning stages. | **M** | Med | `ReadingScreen.kt:1840-1875` | Underline/tint tokens belonging to `SEEN`, `LEARNING`, and `FAMILIAR` stages. |

---

## 12. Remaining Questions & Gaps ("Not Verified")

1. **Live Device Mic & Voice Latency:** Headless Gradle/Robolectric test environments cannot measure real-device microphone latency or offline English voice pack behavior on Android 7.0–10.0 physical hardware.
2. **Screenshots on Physical Device / Emulator:** No Android emulator or physical device was attached (`adb devices` returned 0 devices). Visual rendering was verified via Compose preview inspection and unit layout tests.
3. **Live Gemini API Quota Behavior:** Live API calls to Gemini were not triggered to avoid unmetered developer quota usage.
4. **Follow-Up Worker Focus:** The next implementation turn should atomically resolve **P0-1 (Grammar loop)**, **P1-1 (Word links serialization)**, **P1-2 (`elapsed_ms` across ViewModel & repositories)**, **P1-3 (Lapsed stage classification)**, and **P2-1 (Overdue queue tier preservation)**.
