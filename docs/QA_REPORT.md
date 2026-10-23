# Qaf Whole-App QA Report

**Run date:** 2026-10-22  
**Phase:** B, authorized by the user's `approved` reply  
**Scope:** 41 scenarios from `docs/QA_PLAN.md`

## Execution summary

The disposable emulator was unavailable. `adb devices -l` failed because the ADB daemon could not start, and the `emulator` executable was not available. The approved P0 Gradle test batch was attempted from this QA environment, including a retry using the existing Gradle cache and an offline retry; Gradle 9.3.1 stopped before test execution with `java.io.IOException: Unable to establish loopback connection`. The user then ran the filtered test batch from a regular PowerShell terminal. It reached `:app:compileDebugUnitTestKotlin` and failed with 22 Kotlin diagnostics in `DailyMiniQuizIntegrationTest.kt`. The test compilation failure prevents filtered unit-test classes from running because the unit-test source set must compile first. No test body ran and no JUnit pass result was produced.

Source-only checks completed: **1 Pass, 2 Fail, 38 Not run**. “Pass” below means the defined source inventory check completed; it does not imply an on-device feature passed. No screenshots were captured because there was no device. No source, test, user data, or app settings were changed. Only this report was added during Phase B; transient Gradle artifacts were allowed for the attempted test runs.

### Evidence gathered

- Parsed bundled JSON assets read-only: 14 vocabulary asset files contain **21,624 rows**, **21,624 unique IDs**, and **12 distinct deck values**; `ai_word_details.json` has 10,317 rows; `grammar_lessons.json` has 90; `reading_texts.json` has 61. This validates asset payload counts only; it does not verify database import.
- Test inventory: **78 `@Test` annotations across 17 test files**. The P0 import, reimport, migration, and JSON round-trip test filters were attempted but Gradle failed before execution.
- Static schema inspection: Room schema version 15; migration objects 9→10 through 14→15 are registered. No migration ran on a device during this phase.
- Static navigation/source scan found the backup JSON methods in `DataImportRepository` and `WordBooksViewModel`, but no user-facing SAF export/restore action or route.
- Static security review found the API-key preferences are regular `MODE_PRIVATE` SharedPreferences. Auto Backup is enabled, and the configured backup-rule files contain no active exclusions. Android's [Auto Backup documentation](https://developer.android.com/identity/data/autobackup?hl=en) says SharedPreferences are included by default.
- Static build review found release signing password fallbacks in `app/build.gradle.kts`; the local keystore file is ignored by Git and is not tracked. No key value is reproduced here.

## Word Books scrolling investigation

- **On-device status: Not verified.** The physical phone is attached, but both the bundled ADB client and the Android SDK ADB client report it as `unauthorized`. No device properties, frame statistics, Perfetto trace, or recomposition counts could be collected. The phone model, Android version, and refresh rate are therefore unknown.
- **Static evidence:** `DataImportRepository.kt:672-719` uses Paging with 60-row pages, a 30-row prefetch distance, and a 120-row initial load. `WordBooksDatabaseScreen.kt:1707-1711` supplies stable paging keys and a row content type. The standard entries query in `WordBooksDao.kt:480-515` uses dynamic `CASE`/`LOWER(...)` ordering; search uses leading-wildcard `LIKE` across text, Arabic, and English definitions. `RoomEntities.kt:66-78` declares raw-column indexes, not matching expression/order indexes.
- **Static evidence:** `WordBookEntryRow` in `WordBooksDatabaseScreen.kt:2018-2162` renders nested surfaces, several text nodes, and Arabic text through `BidiText`. `BidiText.kt:480-486` performs mixed-script normalization once per text value while that list item is composed. This makes row composition/text layout a plausible contributor to continuous stutter, but source inspection cannot measure its cost.
- **Static evidence:** `WordBooksViewModel.kt:323-328` builds a map from all observed word-progress rows and passes it to the screen. The selected-deck header scans progress values at `WordBooksDatabaseScreen.kt:1139-1141`; this may add work on data-driven recomposition, but ordinary scrolling does not itself update progress.
- **Assessment:** No root cause is confirmed. With **low confidence**, row composition/text layout is the better code-level match for lag across already-loaded rows; the SQL sort is a plausible cause of stalls when Paging requests more rows. The progress-map scan is a lower-ranked suspect unless traces show frequent recomposition.
- **Next evidence needed:** authorize the connected phone in the USB debugging dialog until `adb devices -l` reports `device`, then capture frame statistics and a short system trace while scrolling through loaded rows and across page boundaries. No app code, data, or settings were changed for this investigation.

## Scenario results

`NR-D` = no disposable emulator/device. `NR-G` = Gradle failed before test execution. “Not run” means no runtime pass or failure is claimed.

| ID | Status | Severity | Evidence / result |
|---|---|---|---|
| QA-001 | Not run | — | NR-D, NR-G. Bundled assets statically contain the expected 21,624 unique vocabulary IDs, 12 deck values, 10,317 details, 90 grammar lessons, and 61 readings. Clean install/import and database counts were not observed. |
| QA-002 | Not run | — | NR-G. `OnboardingStep1Test.verifyTrueUpsertPreservesUserEditsAndAllProgressAcrossReimport` was identified but not executed. |
| QA-003 | Not run | — | NR-D, NR-G. Migration chain is registered in `WordBooksDatabase.kt`; no older APK was installed or upgraded. |
| QA-004 | Not run | — | NR-D. No interrupted-import, low-storage, or low-memory injection was possible. |
| QA-005 | Not run | — | NR-D, NR-G. Onboarding test classes exist; no onboarding UI session or test ran. |
| QA-006 | Not run | — | NR-D. No navigation/back-stack walkthrough was possible. |
| QA-007 | Not run | — | NR-D. Search/filter/sort/jump behavior was not exercised. Static scan found DAO `LIKE` queries rather than FTS; see F-04. |
| QA-008 | Not run | — | NR-G. `WordDetailsAndTrancyTest` covers repository/cache cases, but no test ran and no offline UI fetch was exercised. |
| QA-009 | Not run | — | NR-D. No audio playback test. Source shows streamed `MediaItem` playback and TTS fallback; no local audio cache was found (F-05). |
| QA-010 | Not run | — | NR-G. `ExampleRobolectricTest.verifyFlashcardsStudy10CardsCloseAndReopenAndSm2DueToday` was not executed. |
| QA-011 | Not run | — | NR-G. `Sm2AlgorithmTest` was not executed. |
| QA-012 | Not run | — | NR-G. Due-now/due-today migration test was not executed; no clock-controlled device flow ran. |
| QA-013 | Not run | — | NR-G. Attempt-history isolation test was not executed. |
| QA-014 | Not run | — | NR-D, NR-G. Sentence integration coverage exists, but CRUD/favorites/sorting were not exercised. |
| QA-015 | Not run | — | NR-G. `ExampleRobolectricTest.verifySave3SentencesAndRunOfflineDictation` was not executed. |
| QA-016 | Not run | — | NR-D. Speech permission, recognition service, cancellation, and error states were not exercised. |
| QA-017 | Not run | — | NR-G. Grammar asset count is 90; grammar UI, exercises, and custom import were not run. |
| QA-018 | Not run | — | NR-G, NR-D. No AI-off/missing-key/invalid-response interaction was run. |
| QA-019 | Not run | — | NR-G, NR-D. Reading asset count is 61; comprehension, lookup, and persisted UI progress were not exercised. |
| QA-020 | Not run | — | NR-G, NR-D. Reading/grammar navigation and correction submission were not exercised. |
| QA-021 | Not run | — | NR-D, NR-G. Notebook integration coverage exists but was not executed; no CRUD or spaced-review UI check ran. |
| QA-022 | Not run | — | NR-G, NR-D. Study-plan tests exist but were not executed; profile/deck choices were not changed. |
| QA-023 | Not run | — | NR-G, NR-D. Checklist tests exist but were not executed; no time-budget/task navigation check ran. |
| QA-024 | Not run | — | NR-D, NR-G. Rest-day and date-boundary behavior was not exercised. |
| QA-025 | Not run | — | NR-G. `DailyMiniQuizIntegrationTest.kt` has 22 compile diagnostics; no quiz test body ran and no quiz result was created. See TB-01. |
| QA-026 | Not run | — | NR-D. AI was not toggled and no request-gating interaction was run. |
| QA-027 | Not run | — | NR-D. No disposable emulator was available to test AI enabled without a key. |
| QA-028 | Not run | — | NR-D. No live request was made; no disposable key/device was available. |
| QA-029 | Not run | — | NR-G, NR-D. Batch-translation test exists but was not executed; foreground-service and notification flows were not exercised. |
| QA-030 | Not run | — | NR-D. Settings, key masking, redo setup, and Labs entry were not checked on device. |
| QA-031 | Not run | — | NR-G. Backup/export round-trip test filters were included in the attempted P0 batch, which failed before test execution. |
| QA-032 | Fail | P0 | Source scan found JSON backup APIs only in `DataImportRepository.kt` and `WordBooksViewModel.kt`; no SAF backup/restore route or UI action was found. A user cannot reach the existing API from the inspected app UI. Corrupt-file behavior was not exercised. See F-01. |
| QA-033 | Not run | — | NR-D, NR-G. No airplane-mode session was possible. |
| QA-034 | Not run | — | NR-D, NR-G. No offline/online transition or timeout/invalid-response UI behavior was exercised. |
| QA-035 | Not run | — | NR-D. No startup, search, scroll, or memory measurements were taken. DAO search uses leading-wildcard `LIKE`; no performance conclusion is claimed. |
| QA-036 | Not run | — | NR-D. 360dp, rotation, enlarged font, tablet, RTL, dark theme, and cutout layouts were not visually checked. |
| QA-037 | Not run | — | NR-D. TalkBack, touch-target, contrast, and focus-order checks were not performed. |
| QA-038 | Not run | — | NR-D. Runtime microphone/notification grant and denial paths were not exercised. |
| QA-039 | Fail | P1 | Static configuration indicates API-key SharedPreferences are within Android Auto Backup's default included locations: manifest sets `allowBackup=true`, and backup rules have no active exclusions. Actual cloud/device backup was not run. Release signing password fallbacks are also hard-coded in `app/build.gradle.kts`; the keystore itself is ignored/untracked, so no private-key exposure is confirmed. See F-02 and F-03. |
| QA-040 | Pass | — | Source/roadmap inventory completed. Weak-word drill, scheduled reminders/streak freeze, home-screen widget, and cloud sync are absent from the shipped routes/services; basic speaking/dictation exists, while richer roadmap modes remain partial. These are roadmap gaps, not confirmed regressions. See F-06. |
| QA-041 | Not run | — | NR-D. Bilingual visual/copy review was not possible. Static route scan found no production route for `PlaceholderScreens.kt`; visual absence of placeholders was not verified. |

## Test-suite blocker

| ID | Severity | Evidence |
|---|---|---|
| TB-01: Unit-test Kotlin sources do not compile | P1 coverage blocker | User-provided Gradle log shows 22 diagnostics, all in `app/src/test/java/com/example/DailyMiniQuizIntegrationTest.kt`: constructor arguments `todayDate`, `currentMonth`, and `nowMillis` do not match `TestTimeProvider` (currently accepts `fixedTimeMillis`); `DataImportRepository.importBuiltInDictionary()` is unresolved; grammar model arguments (`summaryEn`, `ruleEn`, exercise `id`) do not match current entities; reading model arguments (`contentEn`, `contentAr`, question `id`, `isCompleted`) do not match current entities and `sentencesJson` is required; `insertGrammarLesson` and `insertReadingText` are unresolved while the DAO exposes `upsertGrammarLesson` and `upsertReadingText`. There is also a type-inference diagnostic at test line 44. Current signatures were cross-checked in `TimeProvider.kt:52`, `RoomEntities.kt:254-264,315-324,357-376,399-406`, and `WordBooksDao.kt:1119,1204`. No source or test code was changed. |

## Findings

| Finding | Severity | Evidence and impact |
|---|---|---|
| F-01: No user-facing backup/restore path | P0 | Backup JSON methods exist at `DataImportRepository.kt:1758,1780` and are forwarded by `WordBooksViewModel.kt:2338-2343`; no SAF launcher or production route calls them. The source capability is unreachable through the inspected UI, limiting user-controlled recovery. |
| F-02: API key may be included in OS backup | P1 | `GeminiApiKeyManager.kt:259-263` writes configured keys to `MODE_PRIVATE` SharedPreferences. `AndroidManifest.xml:22-24` enables backup and points to `backup_rules.xml` / `data_extraction_rules.xml`, which have no active exclusions. Android documents SharedPreferences as included by default. No actual backup archive was inspected in this run. |
| F-03: Release signing password fallback is hard-coded | P1 | `app/build.gradle.kts:30-34` provides literal release signing password fallbacks. The local keystore is Git-ignored and not tracked, so the review did not confirm the private key was exposed. Do not copy the literal values into logs or reports. |
| F-04: README's FTS promise is not reflected in the database query path | P2 | `README.md:70` claims FTS. No FTS entity/table/query was found; `WordBooksDao.kt:497-499` uses `LIKE` expressions with leading wildcards. Runtime search correctness and latency remain unverified. |
| F-05: README's audio-cache claim is not reflected in the player | P2 | `README.md:73` claims audio caching. `AudioPlayerManager.kt` streams a URL through Media3 and falls back to TTS; no audio cache implementation was found. Offline replay was not tested. |
| F-06: Roadmap features remain absent or partial | P2 | `ROADMAP.md:14-19` and feature sections describe the weak-word drill, reminders/streak freeze, widget, and cloud sync. No corresponding shipped route, scheduled worker, widget receiver, or sync flow was found. Basic speaking/dictation exists, but roadmap depth is incomplete. These are roadmap items, not confirmed regressions. |
| F-07: Deck count in README disagrees with the bundle | P2 | `README.md:46` says 13 decks; read-only JSON inspection found 12 distinct deck values across the vocabulary assets, totaling 21,624 rows. A clean app import/catalog was not verified. |

## Not verified

- All install, onboarding, navigation, list, study, sentence, grammar, reading, notebook, plan, quiz, settings, permission, and audio UI flows: no emulator was available.
- Existing automated tests: 78 tests were inventoried. The run in this QA environment could not reach test execution because local IPC failed; the user's run reached test compilation but failed on TB-01. The test cases listed in the plan remain unverified in this run.
- Import interruption, low storage/memory, APK upgrade, restore transactionality, and corrupt-file handling.
- Actual Auto Backup/device-transfer contents; whether a backup contains the API key was inferred from the static policy configuration, not observed in a device backup.
- Live AI behavior with AI off, missing key, or a disposable key; no network request was sent.
- Startup/large-list performance, 360dp/tablet/rotation/enlarged-font layout, RTL/dark/cutout visual layout, TalkBack, contrast, and runtime permission denial.
- Logs and exported user JSON for secret leakage, because no runtime export or device log session was available.
- No screenshots were captured.

## Prioritized fixes to consider (not implemented)

1. **P0 — Provide a user-facing local backup/restore flow** with SAF, clear success/failure feedback, schema/version validation, and preservation of existing data when a file is invalid. Verify export/restore on a disposable device.
2. **P1 — Exclude API-key preferences from cloud backup and device transfer**, or move credentials to storage intentionally excluded from backup. Verify actual backup contents on supported Android versions.
3. **P1 — Remove literal release signing password fallbacks from build configuration.** Supply signing credentials through protected release infrastructure; replace credentials if the current keystore uses the exposed defaults.
4. **P2 — Align the README with shipped behavior or implement the documented capabilities:** 12 versus 13 decks, FTS versus `LIKE` search, and audio caching versus URL streaming/TTS fallback.
5. **P2 — Decide which roadmap items are committed for the current release** and either implement them in a separately authorized change or label them clearly as planned: weak-word drill, reminders/streak freeze, widget, cloud sync, and advanced speaking/dictation modes.
6. **Run the P0 import/reimport/migration/backup tests and device flows** when a working Gradle IPC environment and disposable emulator are available; then continue the remaining scenarios in priority order.
