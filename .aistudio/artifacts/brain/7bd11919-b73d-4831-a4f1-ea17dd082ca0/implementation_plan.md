# Specialized Decks Registration & Offline Word Profiles Auto-Seeding

This update wires the 10,317 pre-seeded offline dictionary profiles (`ai_word_details.json`) and registers 5 new specialized vocabulary decks (`cefr_c1`, `pte_academic`, `business_english`, `career_interview`, and `medical_healthcare`) alongside `ielts_core` and `it_industry` with automatic first-launch and version-triggered database synchronization.

## User Review & Critical Decisions

> [!IMPORTANT]
> Below is a summary of your confirmed preferences from the clarification step, which govern how the new decks and offline profiles are integrated:

- **Confirmed Decision 1 — Full-App Deck Visibility**: All 5 new specialized decks (`CEFR C1 Advanced`, `PTE Academic`, `Business English`, `Job Interview & Career`, and `Medical & Healthcare`) will be surfaced across the **Word Books Library**, **Flashcards Deck Switcher**, **Study Plan Setup & Daily Tasks**, **Home Screen Quick-Resume**, and **Labs Data Check** diagnostics.
- **Confirmed Decision 2 — Non-Destructive Offline Word Profile Seeding**: Pre-seeded entries from `ai_word_details.json` (10,317 profiles) are inserted in batches of 500 using conflict-ignore (`OnConflictStrategy.IGNORE`), preserving any existing user-edited or previously cached AI word profiles while filling in all missing words whenever the stored profile count is below 10,000 or when a data version upgrade occurs.

---

## 1. Overview & Core Concept

- **What It Does**:
  - Automatically imports and maintains 7 specialized vocabulary decks (`ielts_core`, `it_industry`, `cefr_c1`, `pte_academic`, `business_english`, `career_interview`, `medical_healthcare`) on top of the 5 core Oxford/Idiom collections.
  - Pre-seeds 10,317 rich bilingual dictionary profiles (US/UK IPA, syllable breakdowns, Arabic/English definitions, collocations/phrases, Arabic etymology, and bilingual examples) from `ai_word_details.json` for instant zero-latency offline lookup.
  - Triggers automatic synchronization (`DATA_VERSION = 3`) and per-deck zero-count verification so existing installations seamlessly receive all new decks without losing favorites, mastered progress, or SRS schedules.
- **Target Audience / Persona**: Arabic-speaking English learners preparing for academic exams (IELTS, PTE, CEFR C1, OET) or advancing their careers in IT, Business, Healthcare, and international job interviews.
- **Key Value**: Expands the built-in corpus to over 20,000 curated entries and 10,300+ deep word profiles completely offline.

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Automatic Launch Sync**: On app startup, the importer checks `DATA_VERSION = 3`, verifies row counts for all core and specialized decks, imports any deck with `0` rows from `assets/data_import/`, and seeds `ai_word_details.json` in chunks of 500 until all 10,317 profiles are stored.
  2. **Word Books & Flashcards**: Users browse all 12 vocabulary collections (plus *Saved Words*) in the Word Books Library and Flashcards selector, with real-time total, unique, mastered, and favorite word counters.
  3. **Instant Word Detail Sheet**: Tapping any word row opens the rich dictionary sheet immediately from the pre-seeded local `word_details` table without waiting for a network request.
  4. **Study Plan & Data Check**: Learners can include any of the specialized decks in their personalized Study Plan and inspect live row/translation counts in the Data Check screen.
- **Visual Identity & Theme**:
  - **CEFR C1 Advanced** (`cefr_c1`): Deep Indigo cover (`#1E1B4B`) with Soft Indigo accent (`#818CF8`), category *Advanced English*, subtitle *"المفردات المتقدمة للمستوى C1"*, displayOrder `8`.
  - **PTE Academic** (`pte_academic`): Deep Sky Blue cover (`#0C4A6E`) with Sky Blue accent (`#38BDF8`), category *Exam Preparation*, subtitle *"مفردات اختبار بيرسون الأكاديمي"*, displayOrder `9`.
  - **Business English** (`business_english`): Navy Blue cover (`#172554`) with Amber Gold accent (`#F59E0B`), category *Professional English*, subtitle *"الإنجليزية المهنية وإدارة الأعمال"*, displayOrder `10`.
  - **Job Interview & Career** (`career_interview`): Deep Emerald cover (`#064E3B`) with Emerald accent (`#10B981`), category *Career Skills*, subtitle *"مقابلات العمل والتوظيف الدولي"*, displayOrder `11`.
  - **Medical & Healthcare** (`medical_healthcare`): Deep Crimson cover (`#450A0A`) with Coral Red accent (`#EF4444`), category *Healthcare & OET*, subtitle *"المصطلحات الطبية والرعاية الصحية"*, displayOrder `12`.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Chunked Non-Destructive Seeding for `ai_word_details.json`**
  - *Chosen Approach*: Stream `data_import/ai_word_details.json` on `Dispatchers.IO`, decode into `List<WordDetailEntity>`, and batch-insert in chunks of 500 via `@Insert(onConflict = OnConflictStrategy.IGNORE)` whenever `dao.getWordDetailsCount() < 10000` or `versionNeedsUpsert` is true.
  - *Why*: Prevents memory spikes and SQLite transaction lock contention while guaranteeing that any custom or updated word profiles remain intact.
  - *Alternatives Considered*: Replacing all rows (`OnConflictStrategy.REPLACE`) on every launch, which would waste I/O and overwrite newly generated profiles.
- **Decision 2: Individual Zero-Count Checks for All 7 Specialized Decks**
  - *Chosen Approach*: Check source availability and `dao.getEntryCountForDeck(deckId) == 0` for each of the 7 optional specialized files (`ielts_core`, `it_industry`, `cefr_c1`, `pte_academic`, `business_english`, `career_interview`, `medical_healthcare`) inside `ensureImportedOnFirstLaunch()`.
  - *Why*: Ensures that even if a user's database is already at `DATA_VERSION = 3`, any newly added specialized deck asset with `0` entries in SQLite is immediately imported and its deck metadata upserted.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Architecture & Component Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    Assets: app/src/main/assets/data_import/             │
│  • 7 Core JSONs (Oxford 5000, OPAL, Phrases, Expressions, Idioms)       │
│  • 7 Specialized JSONs (IELTS, IT, CEFR C1, PTE, Business, Career, Med) │
│  • ai_word_details.json (10,317 pre-seeded dictionary profiles)         │
└───────────────────────────────────┬─────────────────────────────────────┘
                                    │ Stream & Chunk (BATCH_SIZE = 500)
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         DataImportRepository                            │
│  • DATA_VERSION = 3 & OPTIONAL_GENERATED_FILES (all 7 specialized JSONs)│
│  • ensureImportedOnFirstLaunch():                                       │
│      - seedInitialDecks() & refreshAllDeckCounts() (all 13 deck cards)  │
│      - seedAiWordDetailsIfNeeded() (checks count < 10,000 or upsert)    │
│      - Checks 0-count per specialized deck & imports missing decks      │
└───────────────────────────────────┬─────────────────────────────────────┘
                                    │ Room Transactions (Dispatchers.IO)
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      WordBooksDao (Room SQLite)                         │
│  • entries table (upsertEntriesBatchPreservingUserEdits)                │
│  • decks table (upsertDecks with live row, unique, mastered, fav counts)│
│  • word_details table (insertWordDetails with OnConflictStrategy.IGNORE)│
└─────────────────┬─────────────────┬───────────────────┬─────────────────┘
                  │                 │                   │
                  ▼                 ▼                   ▼
        ┌─────────────────┐ ┌───────────────┐ ┌────────────────────┐
        │ Word Books &    │ │ Study Plan &  │ │ Labs Data Check    │
        │ Flashcards UI   │ │ Home Screen   │ │ Diagnostics Screen │
        └─────────────────┘ └───────────────┘ └────────────────────┘
```

### Data Model & State
- **`WordDetailEntity` (`word_details` table)**: Stores `key` (lowercase word), `json` (serialized dictionary profile with US/UK IPA, syllables, meanings, phrases, etymology, examples), and `createdAt`.
- **`DeckEntity` (`decks` table)**: Stores deck metadata (`id`, `title`, `arabicSubtitle`, `category`, `coverColorHex`, `accentColorHex`, `displayOrder`) and live counters (`rowCount`, `uniqueWordCount`, `masteredCount`, `favoriteCount`) for all 13 decks (`saved_words` + 5 core + 7 specialized).
- **`EntryEntity` (`entries` table)**: Stores vocabulary rows across all decks with fixed ID ranges (`100001+` IELTS, `200001+` IT, `300001+` CEFR C1, `400001+` PTE Academic, `500001+` Business English, `600001+` Career Interview, `700001+` Medical Healthcare).

### Interactive Component & State Mapping
- **Repository Startup & Upsert Flow**:
  - `seedInitialDecks()` and `refreshAllDeckCounts()` query `getEntryCountForDeck`, `getUniqueWordCountForDeck`, `getMasteredCountForDeck`, and `getFavoriteCountForDeck` for all 12 content decks and sum favorites into `Saved Words` (`savedFavTotal`), including `refreshDeckProgressCounts(deckId)` when toggling favorites or mastered flags.
  - `seedAiWordDetailsIfNeeded()` checks `dao.getWordDetailsCount() < 10000` (or `versionNeedsUpsert`) and batch-inserts `ai_word_details.json` in chunks of `BATCH_SIZE` (`500`).
- **UI Layer Integration**:
  - **Word Books & Flashcards**: Reactive `decksFlow` automatically displays the 5 new specialized decks with their custom cover/accent colors and badges.
  - **Study Plan Setup (`PlanSetupDialog` & `StudyPlanRepository`)**: Adds the specialized decks to `availableDecks` and `formatDeckLabel()` so users can schedule daily vocabulary tasks from any specialized deck.
  - **Labs Data Check (`LabsDataCheckScreen`)**: Displays verification rows and Arabic provenance counters for all 7 specialized decks alongside the core decks and total `word_details` count.
