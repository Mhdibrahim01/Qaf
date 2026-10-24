# Qaf Whole-App QA Report

**Execution Timestamp:** 2026-10-22
**Evaluator:** Senior QA Engineer
**Scope:** Whole-App Verification across all core modules, database schema, background jobs, and documentation claims.
**Rules & Constraints Strictly Observed:**
- Zero code, data, database, or test files were modified during this QA job.
- Permitted artifacts produced: `docs/QA_PLAN.md` and `docs/QA_REPORT.md`.
- No "Pass" is logged without concrete evidence (exact test suite, method, and log/assertion output).

---

## Executive Summary

- **Total Automated Test Suites Executed:** 16 files (72 unit and Robolectric tests total).
- **Test Results Summary:** **66 PASSED**, **6 FAILED**.
- **Manual / Physical Device Scenarios (Not Run):** 15 scenarios requiring live device hardware, physical speech recognizer, system SAF file dialogs, 200% accessibility font scale, TalkBack screen reader, or live API key credentials were marked **Not run** with detailed reasons in the "Not Verified" section.

---

## Priority 0: Critical Data Integrity, Schema, and Crashes

| ID | Area | Scenario | Status | Evidence / Test Details | Priority |
|---|---|---|---|---|---|
| QA-001 | Install/import | Fresh install asset import & table counts | **PASS** | `OnboardingStep2Test.bundled dictionary expected row count matches all 14 JSON files (21624) and core 7 files (9862)` passed in 0.060s. `ExampleRobolectricTest.verifyRealJsonFilesImportExactCountsAndProvenanceRules` passed in 2.342s. | P0 |
| QA-002 | Migrations | Room DB schema migration 13 to 15 | **PASS** | `DatabaseMigration13To15Test.migrationFrom13To15_preservesData_andAppliesNewSchema` passed in 0.185s. | P0 |
| QA-003 | Flashcards | SM-2 calculation state machine & intervals | **PASS** | `Sm2AlgorithmTest` passed all 6 test cases (`again rating resets...`, `applyRating sets firstLearnedTimestamp...`, `previewIntervals labels match...`, `hard rating...`, `easy rating...`, `good rating...`). | P0 |
| QA-004 | Backup/Restore | JSON backup import corruption / error handling | **NOT RUN** | Requires live SAF file picker and physical storage permissions on device. Code inspection shows ViewModel supports JSON import, but UI route is unexposed. | P0 |
| QA-005 | Word Details | Local database cache loading without network | **PASS** | `WordDetailsAndTrancyTest.verifyOnlineFetchSavesToWordDetailsAndOfflineLoadsFromCacheWithoutNetwork` passed in 0.029s. | P0 |

---

## Priority 1: Core User Flows and Primary Features

| ID | Area | Scenario | Status | Evidence / Test Details | Priority |
|---|---|---|---|---|---|
| QA-006 | Onboarding | 5-step onboarding wizard execution | **FAIL** | Step 1, 2, 4, 5 passed (`OnboardingStep1Test`, `OnboardingStep2Test`, `OnboardingStep4Test`, `OnboardingStep5Test`). Step 3 failed: `OnboardingStep3Test > step 3 generates profile, scales monthly plan for 15-30-45-60 min, and builds 5-pillar daily checklist` (`AssertionError: expected:<[flashcards, new_words, grammar, reading, quiz]> but was:<[flashcards, new_words, grammar, sentences, quiz]>`). | P1 |
| QA-007 | Navigation | Post-onboarding dashboard initialization | **PASS** | `PostOnboardingDashboardTest.postOnboardingDashboard_displaysPersonalizedBadges` passed in 0.195s; `postOnboardingDashboard_todayChecklistReflectsPlan` passed in 0.117s. | P1 |
| QA-008 | Word Lists | Golden profile word selection Day 1 & Day 7 | **FAIL** | `NewWordSelectionGoldenTest.goldenTestProfileA_B2_GeneralEnglish_Day1AndDay7` (`AssertionError: Day 1 IDs must be sorted numerically: 5288 < 2318`). `NewWordSelectionGoldenTest.goldenTestProfileB_A2_ITCareer_Day1AndDay7` (`AssertionError: Day 1 IDs must be sorted numerically: 200961 < 103210`). | P1 |
| QA-009 | Flashcards | Card flips and review completion | **NOT RUN** | Requires manual screen gestures and Compose touch interactions on a physical or streaming device. | P1 |
| QA-010 | Sentences | Bidi text rendering and brackets formatting | **FAIL** | `BidiTextTest.testLessonG068FormulasAndExamples` failed (`ComparisonFailure`). `BidiTextTest.testBrackets` failed (`ComparisonFailure: Outer brackets must stay outside the isolate expected:<صيغة [(⁦The + comparative⁩)] هي الأكثر شيوعاً> but was:<صيغة [⁦(The + comparative)⁩] هي الأكثر شيوعاً>`). | P1 |
| QA-011 | Grammar | Grammar lesson shelf rendering | **PASS** | `SectionLevelPersonalizationTest.grammarRecommendedShelf_rendersAndHandlesLessonSelection` passed in 0.075s. | P1 |
| QA-012 | Reading | Reading recommended shelf rendering | **PASS** | `SectionLevelPersonalizationTest.readingRecommendedShelf_rendersAndHandlesTextSelection` passed in 0.210s. | P1 |
| QA-013 | Study Plan | Daily plan backlog pacing & time budget scaling | **PASS** | `Phase3DailyPlanTest.testTimeBudgetScaling15_30_45Min` passed in 2.342s; `testReviewBacklogPacingAndReductionReason` passed in 2.929s; `testWeeklyChecklistSummaryDerivedFromDatabase` passed in 2.072s. | P1 |
| QA-014 | Quizzes | Daily quiz engine and skill mixing | **FAIL** | `ExampleRobolectricTest.verifyLocalQuizEngineAndSkillsMix` failed (`AssertionError: expected:<5> but was:<10>`). | P1 |
| QA-015 | AI Features | Strict JSON validator & invalid response retries | **PASS** | `WordDetailsAndTrancyTest.verifyStrictValidatorRejectionRules` passed in 0.019s; `verifyInvalidGeminiJsonIsRejectedAndRetriedOnce` passed in 0.033s. | P1 |
| QA-016 | Settings | API key stored in private SharedPreferences | **NOT RUN** | Requires live UI interaction with `Settings/API-key dialog` and local device storage inspection. | P1 |

---

## Priority 2: Secondary Features, Settings, and Data Handling

| ID | Area | Scenario | Status | Evidence / Test Details | Priority |
|---|---|---|---|---|---|
| QA-017 | Search/A-Z | Dictionary entry parsing and POS formatting | **PASS** | `WordDetailsAndTrancyTest.verifyPosFullName` passed in 0.020s; `verifyPosShortLabel` passed in 0.018s. | P2 |
| QA-018 | Word Details | Arabic definition updates on AI fetch | **PASS** | `WordDetailsAndTrancyTest.verifyUpdateEntryArabicIfEmptyWhenAiDefinitionFetched` passed in 0.039s. | P2 |
| QA-019 | Sentences | A1/A2 level sentence practice rotation | **PASS** | `Phase3DailyPlanTest.testSentencePracticeRotationForA1AndA2Levels` passed in 2.110s. | P2 |
| QA-020 | Dictation | Dictation task inclusion in daily checklist | **PASS** | `Phase3DailyPlanTest.testDueCorrectionReviewsTaskIncludedWhenDueCorrectionsExist` passed in 2.329s. | P2 |
| QA-021 | Corrections | Word details export and import | **PASS** | `WordDetailsAndTrancyTest.verifyWordDetailsExportAndImportWithoutOverwriting` passed in 0.030s. | P2 |
| QA-022 | Onboarding | Profile deck assignment and daily checklist sync | **PASS** | `OnboardingStep4Test.step 4 updates profile decks and synchronizes daily checklist` passed in 0.097s. | P2 |
| QA-023 | Onboarding | Placement quiz rough estimate and 'I dont know' | **PASS** | `OnboardingStep2Test.step 2 UI preselects B2, displays 21624 import total, and supports placement quiz I dont know...` passed in 0.561s. | P2 |
| QA-024 | Onboarding | Redo setup preserving completed tasks & progress | **PASS** | `OnboardingStep3Test.step 3 redo setup preserves completed checklist tasks and user word progress` passed in 0.093s. | P2 |
| QA-025 | Audio | Media3 dictionary URL audio stream & TTS fallback | **NOT RUN** | Requires audio hardware output / Android `TextToSpeech` service initialization on device. | P2 |
| QA-026 | Bidi Text | Visual comparison artifact generation | **PASS** | `BidiTextTest.testGenerateVisualComparisonArtifacts` passed in 0.629s. | P2 |
| QA-027 | Onboarding | Step 5 completion and allowAi persistence | **PASS** | `OnboardingStep5Test.step 5 completion persists allowAi and marks onboarding completed` passed in 0.092s. | P2 |
| QA-028 | Network | Offline / Airplane mode graceful handling | **NOT RUN** | Requires disabling network interface on device/emulator. | P2 |

---

## Priority 3: Visual Layout, Polish, and Edge Cases

| ID | Area | Scenario | Status | Evidence / Test Details | Priority |
|---|---|---|---|---|---|
| QA-029 | Onboarding | Step 1 Goal Selection UI | **PASS** | `OnboardingStep1Test.step 1 UI preselects B2, displays 21624 import total...` passed in 0.380s. | P3 |
| QA-030 | Onboarding | Step 3 Schedule & Time Budget UI | **PASS** | `OnboardingStep3Test.step 3 UI preselects General Fluency, 30 min, 6 days...` passed in 0.371s. | P3 |
| QA-031 | Onboarding | Step 5 Profile summary & offline notice | **PASS** | `OnboardingStep5Test.step 5 UI defaults AI to OFF, shows 100 percent offline notice...` passed in 0.214s. | P3 |
| QA-032 | Layout | Screen rotation state retention | **NOT RUN** | Requires manual orientation change trigger on device/emulator. | P3 |
| QA-033 | Accessibility| 200% font scale & touch target 48dp verification | **NOT RUN** | Requires modifying device accessibility settings and measuring layout nodes on device. | P3 |
| QA-034 | Utility | Example Unit Test baseline sanity check | **PASS** | `ExampleUnitTest.addition_isCorrect` passed in 0.001s. | P3 |
| QA-035 | Audio | Dictation speech recognition via `SpeechRecognizer` | **NOT RUN** | Requires active microphone hardware permission and speech service on device. | P3 |
| QA-036 | Visual | RTL / Dark Theme / Notch layout verification | **NOT RUN** | Requires physical device display rendering and visual comparison. | P3 |
| QA-037 | AI | Live API key invocation (Gemini/Groq/OpenAI) | **NOT RUN** | Live API keys are not provided in environment; live-key invocation marked not run per scope instructions. | P3 |
| QA-038 | Performance | Large list scrolling performance (21,624 items) | **NOT RUN** | Requires device frame rate profiling during list scrolling. | P3 |
| QA-039 | Security | Auto-Backup privacy rule inspection | **NOT RUN** | Requires triggering system backup manager on device and inspecting backup archive. | P3 |
| QA-040 | Performance | First-launch cold startup latency | **NOT RUN** | Requires cold-launch benchmarking on device hardware. | P3 |
| QA-041 | System | Low memory interrupt handling during database import | **NOT RUN** | Requires sending low-memory pressure signal (`onLowMemory`) during import on device. | P3 |

---

## Not Verified Section

The following 15 scenarios could not be executed during this headless verification run due to environment constraints (no physical device, no live API keys, no interactive touchscreen, or hardware permissions required):

1. **QA-004 (Backup/Restore UI Reachability):** Requires system SAF file picker and physical storage permissions.
2. **QA-009 (Flashcard Gestures):** Requires manual swipe/flip touch interactions on a physical display.
3. **QA-016 (API Key private SharedPreferences UI):** Requires live UI interaction with Settings dialog and local storage inspection.
4. **QA-025 (Media3 Audio Streaming & TTS):** Requires audio output device and Android `TextToSpeech` service runtime.
5. **QA-028 (Airplane Mode Network Interruption):** Requires toggling system connectivity on device.
6. **QA-032 (Screen Rotation Retention):** Requires manual orientation change event on display.
7. **QA-033 (Accessibility 200% Font Scale & 48dp Touch Target):** Requires system accessibility font scaling.
8. **QA-035 (Dictation Speech Recognition):** Requires microphone hardware and speech-to-text service.
9. **QA-036 (RTL Display & Dark Theme Notch Verification):** Requires visual display inspection.
10. **QA-037 (Live AI Key Invocations):** No disposable live API key configured in environment.
11. **QA-038 (Large List Scrolling Performance):** Requires GPU frame profiling during fast fling scrolling.
12. **QA-039 (Auto-Backup Exclusions Inspection):** Requires Android System Backup Agent triggers.
13. **QA-040 (Cold Launch Startup Time Measurement):** Requires System Trace / Macrobenchmark on hardware.
14. **QA-041 (Low Memory Signal Interruption):** Requires memory pressure injection (`ActivityManager.getMemoryInfo`).
15. **Placeholder Screens Entry Points:** Code inspection shows prototype screens in `PlaceholderScreens.kt` with no active navigation route; manual verify on device to confirm they are inaccessible.

---

## Prioritized List of Recommended Fixes (Described, Not Implemented)

### High Severity (Failed Tests)
1. **Fix 5-Pillar Daily Checklist Definition in `OnboardingStep3Test`:**
   - **Issue:** Step 3 test expects `reading` as pillar #4, but the generator builds `sentences` (`[flashcards, new_words, grammar, sentences, quiz]`).
   - **Recommended Action:** Align `DailyChecklistGenerator` and `OnboardingStep3Test` so the 5 pillars match product requirements consistently across onboarding and daily task creation.
2. **Fix Word Selection Numeric Sorting in `NewWordSelectionGoldenTest`:**
   - **Issue:** Golden test fails assertion `Day 1 IDs must be sorted numerically: 5288 < 2318` and `200961 < 103210`.
   - **Recommended Action:** Update `NewWordSelector` or repository query to explicitly sort selected entry IDs numerically before returning them to golden test consumers.
3. **Fix BiDi Outer Bracket Isolation in `BidiTextTest`:**
   - **Issue:** Test `testBrackets` failed with `Outer brackets must stay outside the isolate expected:<صيغة [(⁦The + comparative⁩)] هي الأكثر شيوعاً> but was:<صيغة [⁦(The + comparative)⁩] هي الأكثر شيوعاً>`.
   - **Recommended Action:** Correct unicode isolate marker placement in `BidiFormatterUtil` for bracketed formula strings.
4. **Fix Daily Quiz Skill Mix Baseline in `ExampleRobolectricTest`:**
   - **Issue:** Test `verifyLocalQuizEngineAndSkillsMix` expects 5 questions, but the quiz generator produces 10 questions (`expected:<5> but was:<10>`).
   - **Recommended Action:** Update test assertion or engine logic to match the 10-question specification established in product rules.

### Medium Severity (Documentation and Scope Discrepancies)
5. **Update Deck Count in README:**
   - **Issue:** README states 13 decks, but asset catalog contains 12 distinct deck IDs.
   - **Recommended Action:** Update README documentation to state 12 bundled decks.
6. **Correct Search Feature Claim in README:**
   - **Issue:** README claims SQLite Full-Text Search (FTS), but code uses standard SQLite LIKE queries.
   - **Recommended Action:** Either implement FTS5 virtual tables or update README to accurately describe standard search capabilities.

### Low Severity (UI Reachability & Polish)
7. **Expose Backup & Restore Controls in Settings:**
   - **Issue:** Repository and ViewModel contain JSON backup/restore methods, but no user-facing UI button exists in Settings.
   - **Recommended Action:** Add a "Backup Data" and "Restore Data" action item in `SettingsScreen.kt` using `ActivityResultContracts.CreateDocument` and `OpenDocument`.
8. **Clarify Audio Offline Caching Behavior:**
   - **Issue:** Documentation implies audio caching, but Media3 streams directly from URL with TTS fallback.
   - **Recommended Action:** Document streaming nature or add disk cache configuration to Media3 `ExoPlayer` instance.
