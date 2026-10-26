# Pedagogical & Balanced New-Word Selection Engine — Implementation Plan

Eliminates the sequential `'A' → 'Z'` alphabetical clustering in daily new-word learning sessions by replacing raw numeric ID re-sorting with a **deterministic, 4-tier pedagogical ranking and initial-letter/POS diversity pass** across daily study plans, focused flashcard sessions, onboarding starter cards, and unit tests.

---

### Root Cause Summary

1. **Alphabetical ID Assignment in Bundled Datasets**:
   - In `app/src/main/assets/*.json` (`oxford5000.json`, `oxford_opal.json`, `ielts_core.json`, `it_industry.json`, etc.), primary key `id`s are assigned in strict alphabetical order (`id = "1"` is `"a"`, `id = "2"` is `"abandon"`, `id = "3"` is `"ability"`, `id = "4"` is `"able"`).
2. **Kotlin Post-Processing Overrides SQL Hash Ordering**:
   - `WordBooksDao.getUnstudiedEntriesForDeckAndEffectiveLevels` orders rows using Knuth's multiplicative hash `((CAST(e.id AS INTEGER) * 2654435761) % 4294967296) ASC`, **but** returns all unstudied rows for the deck without a `LIMIT`.
   - `StudyPlanRepository.deduplicateCandidatesForDeck` (lines 727–740) and `deduplicateAndPickDeterministicEntry` (lines 783–790) re-sort all candidates in Kotlin using `compareNumericId(a, b)` (`1, 2, 3, 4...`).
   - As a result, every daily session picks the lowest numeric IDs first (`abandon`, `ability`, `able`, `abolish`...).
3. **Secondary SQL Queries Also Sort by Raw `CAST(id AS INTEGER) ASC`**:
   - `WordBooksDao.getNewWordsForLevelsAndDecks` (line 217)
   - `WordBooksDao.getOnboardingFirstWinEntries` (line 248)
   - `WordBooksDao.getKickoffStarterWords` (line 526)

---

### Target Files to Modify

1. **`app/src/main/java/com/example/data/plan/StudyPlanRepository.kt`**
   - Add deterministic 32-bit Knuth hash helper `knuthHash(entry: EntryEntity): Long`:
     ```kotlin
     internal fun knuthHash(entry: EntryEntity): Long {
         val numericId = entry.id.toLongOrNull()
         return if (numericId != null) {
             (numericId * 2654435761L) and 0xFFFFFFFFL
         } else {
             (entry.id.hashCode().toLong() * 2654435761L) and 0xFFFFFFFFL
         }
     }
     ```
   - Add pedagogical content richness helper `contentRichnessRank(entry: EntryEntity): Int`:
     ```kotlin
     private fun contentRichnessRank(entry: EntryEntity): Int {
         val hasExample = entry.examplesEn.isNotBlank() && entry.examplesEn != "[]"
         val hasAudio = entry.audioUrl.isNotBlank()
         return when {
             hasExample && hasAudio -> 0
             hasExample -> 1
             hasAudio -> 2
             else -> 3
         }
     }
     ```
   - In `deduplicateCandidatesForDeck`:
     - Keep `compareNumericId(a, b)` inside `variants.minWithOrNull` (for picking the canonical variant of a single headword).
     - Replace `compareNumericId(a, b)` in `deduped.sortedWith` with:
       1. `aLookedUp != bLookedUp` (looked-up `SEEN` words first)
       2. `aIsNull != bIsNull` (graded words before `NULL`-level words)
       3. `rankA != rankB` (CEFR closeness rank)
       4. `contentRichnessRank(a) != contentRichnessRank(b)` (prefer entries with examples + audio)
       5. `knuthHash(a).compareTo(knuthHash(b))` (fallback `compareNumericId(a, b)` on hash tie)
   - In `selectNewWordsForProfile` (`takeFromList`):
     - Track `letterCounts = mutableMapOf<Char, Int>()` across `collectedEntries` with a soft cap of `maxPerLetter = (targetCount / 5).coerceAtLeast(2)` (max 2 words per initial letter in a 10-word session).
     - Pass A: Pick unpicked candidates whose initial letter count is `< maxPerLetter`.
     - Pass B: If `picked.size < maxCount`, backfill from any skipped candidates in `source` so small candidate pools never suffer artificial shortfalls.
   - In `deduplicateAndPickDeterministicEntry`:
     - Keep `compareNumericId(a, b)` inside `variants.minWithOrNull`.
     - In the final `.sortedWith(...)` across distinct headwords, order by `aLookedUp != bLookedUp` then `knuthHash(a).compareTo(knuthHash(b))` (removing the ascending numeric ID sort across distinct words).
   - In `buildDailyTaskList`:
     - Enable `selectStoryPreTeachEntries` whenever `anchorStory != null && newWordsTarget > 0` across all levels (`A1`–`C1`), pre-teaching up to `minOf(4, newWordsTarget / 2)` unstudied story focus words.

2. **`app/src/main/java/com/example/data/local/WordBooksDao.kt`**
   - Update `getNewWordsForLevelsAndDecks`, `getOnboardingFirstWinEntries`, and `getKickoffStarterWords` to replace `CAST(id AS INTEGER) ASC` with `((CAST(id AS INTEGER) * 2654435761) % 4294967296) ASC, id ASC`.

3. **`app/src/main/java/com/example/ui/WordBooksViewModel.kt`**
   - Ensure `startFocusedNewWordsSession` includes the same story pre-taught words + profile-selected new words shown on the Today's Lesson checklist.

4. **`app/src/test/java/com/example/NewWordSelectionGoldenTest.kt`**
   - Replace the legacy ascending-ID assertion (`day1Ids[i] < day1Ids[i + 1]`) with:
     - **Determinism check**: Calling `selectNewWordsForProfile(profile, 10)` twice returns the exact same ordered list of IDs.
     - **Alphabetical diversity check**: A 10-word daily batch spans at least 4 distinct starting letters (`day1Words.mapNotNull { it.text.trim().lowercase().firstOrNull() }.distinct().size >= 4`).
     - Keep all existing deck-split, CEFR-level, and Day 1–6 vs. Day 7 non-repetition assertions intact.
