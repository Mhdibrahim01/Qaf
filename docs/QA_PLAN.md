# Qaf Whole-App QA Plan

## Scope and execution gate

This is Phase A: a source and documentation based inventory and verification plan. Phase B is not authorized until the user replies **approved**. After approval, execute in priority order in batches on a disposable emulator with synthetic data. Never use the user's personal device or data. Use a disposable API key only if one is available; otherwise mark the live-key scenario not run. No code, data, app settings, or automated tests may be changed for this QA job. The only permitted artifacts are this plan, `docs/QA_REPORT.md`, and screenshots under `docs/qa/`.

## Inventory

| Inventory area | Findings |
|---|---|
| Screens and navigation | Five-step onboarding; Home; Word Books and word-detail sheet; Flashcards; Sentences with Favorites, Dictation, and Speaking tabs; Grammar list/detail/chat/import; Reading list/detail/word lookup/grammar practice/import; Correction Notebook; Study Plan/checklist/quiz; More; Settings/API-key dialog; Labs/Data Check; Batch Translate. `PlaceholderScreens.kt` has prototype screens, with no production route identified for them. |
| Learning features | Vocabulary lists, search, filters, sorting and A–Z navigation; word details; favorites/mastery; flashcard sessions and SM-2 scheduling; sentence CRUD/favorites; dictation and speech recognition; grammar lessons/exercises/import/AI help; reading/comprehension/word lookup/grammar links; corrections and review; study plans, daily checklist, streaks, missed-day handling, quizzes; batch translation. |
| Room schema | Database version 15; 20 tables: `entries`, `decks`, `word_progress`, `attempts`, `sentences`, `grammar_lessons`, `grammar_progress`, `ai_history`, `reading_texts`, `reading_progress`, `ai_cache`, `feature_flags`, `feature_suggestions`, `deck_study_state`, `correction_notebook`, `study_plan_profile`, `study_plans`, `daily_task_checklist`, `quiz_results`, `word_details`. Migrations 9→10 through 14→15 are registered. |
| Bundled content | 21,624 vocabulary rows across 12 distinct deck IDs; 10,317 word-detail records; 90 grammar lessons; 61 reading texts. |
| Background work | Batch translation foreground service. No scheduled worker or reminder job was found. |
| Permissions and service declarations | `INTERNET`, `ACCESS_NETWORK_STATE`, `RECORD_AUDIO`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`, `POST_NOTIFICATIONS`; speech-recognition and TTS service queries. |
| External calls | Gemini, Groq, OpenRouter, OpenAI; dictionary and translation providers; dictionary audio streamed with Media3 and TTS fallback; Android `SpeechRecognizer` and `TextToSpeech`. |
| AppFunctions | No AppFunctions dependency, service, or documentation promise found. Potential useful future functions from current workflows are a rich vocabulary search and a guarded sentence/correction save action. These are discovery notes only; this QA job will not add or expose them. Raw API keys and destructive actions should never be exposed as agent functions. |

## Promise and implementation gaps

| Promise or roadmap item | Source review finding | QA classification |
|---|---|---|
| README says 13 decks | The onboarding catalog has 12 distinct deck IDs. | Content/documentation discrepancy; verify shipped catalog on device. |
| README promises FTS search | No FTS table or FTS query was found in the inspected schema/source. | Promise appears missing or implemented through another search path; verify search behavior and report. |
| README claims audio caching | Audio player streams dictionary URLs and has a TTS fallback; no audio cache implementation was found. | Claimed cache appears absent; verify offline replay. |
| Backup and restore | JSON APIs exist in repository/ViewModel code, but no user-facing backup/restore route or call was found. | Partial; validate UI reachability and report if unavailable. |
| Weak-word drill; reminders/streak freeze; home-screen widget; cloud sync | Listed in roadmap but no shipped implementation was found. | Roadmap gaps; distinguish from shipped regressions. |
| Dictation and speaking | Basic screens and code exist; richer roadmap modes were not evident. | Partial; validate basic flows and document advanced-mode gaps. |
| Placeholder screens | Prototype screens exist but are not routed through production navigation. | Confirm no production entry point exposes prototypes. |
| API-key backup privacy | Key is in private SharedPreferences; Auto Backup is enabled and current backup-rule files have no explicit exclusions. This is a risk to verify, not a confirmed exposure. | Inspect actual backup behavior without capturing the secret. |

## Scenarios

Verification labels: **Auto** means an existing related automated test (coverage may be partial); **Manual** means on-device/manual verification; **Source** means static code or database inspection. No new automated tests will be authored because code changes are out of scope. P0 = data loss or crash; P1 = core flow; P2 = important; P3 = polish.

| ID | Area | Scenario | Steps | Expected result | How verified / existing evidence | Priority |
|---|---|---|---|---|---|---|
| QA-001 | Install/import | Fresh install and first-launch asset import | Install clean; launch; wait for import; inspect row counts and deck IDs. | 21,624 vocabulary rows, 12 decks, 10,317 details, 90 grammar lessons, 61 readings; no duplicates; onboarding opens. | Auto: `ExampleRobolectricTest.verifyRealJsonFilesImportExactCountsAndProvenanceRules`; Manual install/count check. | P0 |
| QA-002 | Import/data integrity | Reimport after edits and progress | Favorite/master a word and rate cards; trigger bundled-data reimport. | No duplicates; edits, progress, and attempts survive. | Auto: `OnboardingStep1Test.verifyTrueUpsertPreservesUserEditsAndAllProgressAcrossReimport`. | P0 |
| QA-003 | Upgrade/migrations | Install over an older version | Seed progress, favorites, sentences, notebook, and plan; upgrade to current build; inspect DB/data. | Schema reaches v15 and user data remains intact without destructive fallback. | Auto: `DatabaseMigration13To15Test` and v9→10/v12→13 migration tests; Manual APK upgrade. | P0 |
| QA-004 | Import failure | Interrupted import, low storage, or low memory | Interrupt import or constrain device resources; relaunch and retry. | App remains usable; retry is recoverable and creates no duplicates or progress loss. | Manual; no failure-injection test found. | P0 |
| QA-005 | Onboarding | Complete, skip/resume, and redo all five steps | Exercise welcome, placement, goals, deck selection, and AI choice; close/reopen partway. | Choices persist, resume is coherent, and completion opens the personalized dashboard. | Auto: `OnboardingStep1Test`–`OnboardingStep5Test`, `PostOnboardingDashboardTest`. | P1 |
| QA-006 | Navigation | Bottom navigation, More hub, back stack, cross-links | Open every destination; navigate back from details and linked lessons. | Each reachable screen opens; back returns to the expected state without losing work. | Manual; no whole-app navigation-flow test found. | P1 |
| QA-007 | Word lists/search/A–Z | Search, filters, sort, paging, letter jump | Search English/Arabic; apply filters/sorts; browse full list; jump by letter. | Matching results, stable ordering, and correct paging/jumps. | Auto: selection/A–Z tests; Manual list UI checks. | P1 |
| QA-008 | Word details | Offline details, cache, AI fetch, invalid response | Open bundled details offline; fetch uncached details; reopen cache; submit invalid response. | Local details work offline; valid results persist; invalid results are rejected/retried safely. | Auto: `WordDetailsAndTrancyTest` cache, validation, retry cases. | P1 |
| QA-009 | Audio | US/UK pronunciation and offline fallback | Play available voices; change voice/speed; disable network and replay. | Audio plays or gives a clear fallback/error; playback stop/resume is correct. | Manual; no audio-player test found. | P1 |
| QA-010 | Flashcards | Session, flip, close/reopen, card position | Study and rate cards; leave and reopen the session/app. | Position/state restore and rating updates the correct progress row. | Auto: `ExampleRobolectricTest.verifyFlashcardsStudy10CardsCloseAndReopenAndSm2DueToday`. | P1 |
| QA-011 | Flashcards/SM-2 | Rating intervals and previews | Rate new/review cards Again, Hard, Good, Easy; compare preview to stored due time. | Intervals, ease bounds, and labels match SM-2 calculation. | Auto: `Sm2AlgorithmTest`. | P1 |
| QA-012 | Flashcards scheduling | Due-now versus due-later | Rate Again and first-review Hard; revisit before due; advance test clock. | Cards wait until due; due-today count distinguishes later-today cards. | Auto: `DatabaseMigration13To15Test.dueNowVsDueTodayUnifiesCutoffsAndHidesAgain10mAndFirstHard4hCardsUntilTimerExpires`. | P1 |
| QA-013 | Progress/history | Attempt records and metric isolation | Rate cards and complete quizzes; favorite/master without studying. | Study actions log attempts; favorite/manual mastery does not inflate study counts. | Auto: `DatabaseMigration13To15Test.attemptsAreLoggedTransactionallyAcrossActivitiesWhileFavoritesAndManualMasteryStayIsolated`. | P1 |
| QA-014 | Sentences | Save/edit/delete, favorites, sorting | Add sentences from a word; change favorite/sort; edit and delete. | Items and preferences persist and list order reflects settings. | Auto: sentence integration test; Manual CRUD UI checks. | P1 |
| QA-015 | Dictation | Offline sentence dictation and scoring | Use saved sentences; type heard text; vary punctuation and spelling. | Diff highlighting and score are consistent; offline practice works. | Auto: `ExampleRobolectricTest.verifySave3SentencesAndRunOfflineDictation`. | P1 |
| QA-016 | Speaking | Permission and speech-recognition outcomes | Grant/deny microphone; test listening, unavailable service, cancel, and errors. | Permission is contextual; states are understandable and recoverable. | Manual; no speech-recognition UI test found. | P1 |
| QA-017 | Grammar | Catalog, filters, exercises, JSON import | Browse levels; complete offline exercises; import valid custom lessons. | Lessons/answers load; imports appear without damaging existing content. | Auto: `ExampleRobolectricTest.verifyGrammarOfflineExercisesAndInteractiveAi` and `verifyGrammarAllLessonsLoadedFromAssetsAndCustomJsonImportWorks`. | P1 |
| QA-018 | Grammar/AI | AI help off, missing key, invalid response | Ask for help with AI disabled, no key, then fake configured transport. | Safe disabled/key-required state; local lesson usable; invalid output rejected. | Auto: AI safeguard/validation tests; Manual settings check. | P2 |
| QA-019 | Reading | Levels, comprehension, lookup, saved progress | Open texts at available levels; answer questions; tap words; reopen offline. | Text, results, lookup, and saved progress are correct. | Auto: `ExampleRobolectricTest.verifyReadingSectionOfflineAndAiFeatures`, `SectionLevelPersonalizationTest`. | P1 |
| QA-020 | Reading/grammar links | Navigate linked reading and grammar | Follow links both directions; submit a correction. | Link targets are correct and correction is stored by the expected feature. | Auto: `ExampleRobolectricTest.verifyGrammarReadingIntegrationAndOfflineCorrectionNotebook`. | P1 |
| QA-021 | Correction Notebook | CRUD and spaced review | Save, edit, remove, rate a due correction, and reopen notebook. | Entries persist; due dates and lesson/reading links remain consistent. | Auto: notebook integration coverage; Manual screen checks. | P1 |
| QA-022 | Study plan | Profile, plan, selected decks | Change goal, level, minutes, study days, decks; regenerate/reopen. | Plan reflects saved selections and uses intended decks. | Auto: onboarding, `NewWordSelectionGoldenTest`, study-plan Robolectric tests. | P1 |
| QA-023 | Daily checklist | Tasks, time budget, backlog, links | Generate plans at varied time budgets; add reviews; complete/open tasks. | Tasks fit budget, explain backlog/reasons, and open correct feature. | Auto: `Phase3DailyPlanTest`, `PostOnboardingDashboardTest`. | P1 |
| QA-024 | Streak/missed days | Rest days, missed days, weekly summary | Check rest/missed day, date rollover, and weekly checklist. | Streak follows saved daily records; incomplete days are not counted complete. | Auto: weekly/rest-day tests; Manual date-boundary checks. | P1 |
| QA-025 | Quizzes | Daily quiz mix and persisted results | Generate quiz; answer and submit; reopen history. | Scope/scoring are correct; results and attempts persist. | Auto: `DailyMiniQuizIntegrationTest` and quiz Robolectric tests. | P1 |
| QA-026 | AI | AI off | Turn AI off; use local flows and invoke an AI action. | Local learning works; AI remains disabled and sends no request. | Auto: `OnboardingStep5Test` default-off check; Manual request gating. | P2 |
| QA-027 | AI | AI on without a key | Enable AI without configuring a key; invoke AI actions. | No crash/data loss; clear key-required state; local flows remain usable. | Auto: AI safeguard coverage; Manual UI check. | P2 |
| QA-028 | AI | AI on with a key | Use disposable key and synthetic prompt; check response and errors. | Response handled/cached as intended; key and prompt are absent from evidence. If no disposable key, mark not run. | Auto: fake-transport tests; Manual live smoke only with disposable key. | P2 |
| QA-029 | Batch Translate | Foreground translation lifecycle | Start small batch; background/close; resume/cancel; test missing key/network. | Progress appears; restart/cancel/errors do not corrupt entries; notification handling works. | Auto: `ExampleRobolectricTest.verifyBatchTranslate50WordsCloseMidRunReopenAndContinue`; Manual permission check. | P2 |
| QA-030 | Settings | AI/key dialog, redo setup, Labs | Toggle AI; add/clear disposable key; redo onboarding; open Labs. | Settings are reachable; key masked; redo preserves study data. | Auto: redo-setup persistence test; Manual settings checks. | P2 |
| QA-031 | Backup/export | JSON round-trip | Export user progress, attempts, plans, notebook, sentences; restore to clean DB. | Supported user state is preserved and restore is transactional. | Auto: `DatabaseMigration13To15Test` round-trip and `WordDetailsAndTrancyTest` export/import. | P0 |
| QA-032 | Backup/restore UI | SAF export/import and corrupt file | Locate UI; if present, export/restore valid, truncated, invalid JSON. | Valid restore works; invalid file leaves existing data unchanged. If UI absent, report feature partial. | Manual; source review found APIs but no UI call. | P0 |
| QA-033 | Offline/airplane mode | Core learning without network | Enable airplane mode; use lists/details, grammar, reading, dictation. | Local content and offline practice work; online-only actions explain unavailability. | Auto: offline/cache tests; Manual airplane-mode check. | P1 |
| QA-034 | Network/error handling | Timeout, invalid response, fallback | Simulate bad AI/network response; retry/fallback; transition offline/online. | Input/progress is retained; errors and retry states are clear. | Auto: invalid-response/retry tests; Manual network transition. | P1 |
| QA-035 | Performance | Large-list search and cold start | Measure startup/import, search, and scroll on full dataset; observe memory/responsiveness. | No crash; record device/timings; list interactions remain usable. | Manual; no startup/large-list performance test found. | P2 |
| QA-036 | Layout | Width, rotation, font, tablet, RTL, dark, cutouts | Check representative screens at 360dp, tablet, 200% font, RTL, dark theme and notches; rotate during tasks. | Controls/text remain visible and usable; order is correct; active work survives rotation. | Manual; `BidiTextTest` covers text handling only. | P2 |
| QA-037 | Accessibility | Targets, contrast, screen reader | Inspect navigation, ratings, dialogs, speech, quiz controls with TalkBack/enlarged text. | Usable target sizes, meaningful labels, readable contrast and focus order. | Manual; no accessibility-specific automated tests found. | P2 |
| QA-038 | Permissions | Microphone/notification grant and denial | Grant/deny permissions on supported API levels; start speech and batch translation. | Requests are contextual and denial recoverable; no unrelated storage permission. | Manual; no runtime-permission UI tests found. | P1 |
| QA-039 | Security/privacy | Keys, logs, exports, backups, permissions | Check masking/storage, logs, exported JSON, OS backup inclusion, permission use. | No key in logs/screenshots/user export; permissions match feature. Treat backup inclusion as unconfirmed risk. | Manual + Source review; no security-specific automated tests found. | P1 |
| QA-040 | Roadmap status | Weak-word drill, reminders/freeze, widget, sync | Inspect app routes/settings and compare shipped behavior with roadmap. | Mark each shipped, partial, or absent; separate gaps from regressions. | Manual + Source review; no related automated tests. | P2 |
| QA-041 | Polish | Bilingual copy and visual consistency | Review key flows in English/Arabic for clipping, terminology, placeholder copy. | No prototype screen is exposed in production; labels are legible and consistent. | Manual; no dedicated copy/visual test found. | P3 |

## Top 10 data and daily-loop risks

1. An interrupted first-run import may leave incomplete content or duplicate rows on retry.
2. An upgrade migration may damage progress, favorites, attempts, sentences, notebook entries, or plans.
3. Restore may be unavailable from the UI, and malformed restore data could threaten current user state.
4. Reimport or redo onboarding may clear progress that should persist.
5. SM-2 due-time errors may resurface cards too early or hide them from the daily queue.
6. Date boundaries, missed days, or rest days may produce incorrect streak/checklist state.
7. The large bundled dataset may slow startup or exhaust memory/storage; README FTS claims are not reflected in the inspected schema/query code.
8. The API key may be included in OS backups; verify actual backup behavior and exclusions without exposing the key.
9. Microphone, speech-service, or audio fallback failures may interrupt speaking and dictation.
10. Missing reminders, streak freeze, weak-word drill, backup UI, and sync may weaken the daily loop and data durability.

## Coverage summary

- Inventory: **41 scenario groups**.
- Existing tests: **78 `@Test` methods across 17 test files**. **29/41** groups have at least some related automated coverage; this does not imply full UI/device coverage.
- No related automated coverage identified for **12/41** groups: QA-004 import failure injection; QA-006 whole-app navigation; QA-009 audio playback; QA-016 speech-recognition UI; QA-032 SAF backup/restore UI; QA-035 startup/large-list performance; QA-036 responsive layouts; QA-037 accessibility; QA-038 runtime permissions; QA-039 privacy/backup behavior; QA-040 roadmap gaps; QA-041 visual/copy review.
- Critical flows with no end-to-end device automation include fresh install/upgrade, import failure recovery, navigation, speech/audio, SAF restore, and airplane-mode operation. Existing repository tests provide partial data/repository coverage for several of these.
- No new tests will be added under the no-code-change constraint. Manual checks will be marked not run when the disposable emulator or required disposable key is unavailable.

## Phase B execution and reporting

After the user's explicit `approved` reply, execute scenarios in priority order, batched by area. Record each as **Pass**, **Fail**, or **Not run** in `docs/QA_REPORT.md`. A pass must include evidence (test name/result, screenshot, log line, query result, or measured value). Each failure gets a priority/severity and reproducible evidence. Include a **Not verified** list with reasons and a prioritized fixes list describing changes without implementing them. Store screenshots only in `docs/qa/`; redact secrets and personal data. If a device/emulator or disposable key is unavailable, report affected scenarios as not run instead of inferring a result.
