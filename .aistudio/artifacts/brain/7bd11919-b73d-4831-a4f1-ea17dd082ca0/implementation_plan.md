# Study Plan Feature Optimization & Architecture Blueprint

An end-to-end enhancement of the personalized Study Plan system: fixing critical task navigation and completion bugs, eliminating hardcoded CEFR levels, correcting AI plan validation, and introducing a motivating monthly consistency calendar with actionable weak-area practice sessions.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> The plan addresses all diagnosed bugs and elevates the user experience across Today's Checklist, Monthly Curriculum, and Analytics:

- **Confirmed Decision 1 (Task Deep-Linking & Smart Action)**: Clicking on a linked task in Today's Checklist will directly open the specific Grammar lesson (`wordBooksViewModel.selectGrammarLesson`), specific Reading text (`wordBooksViewModel.selectReadingText`), or filtered Flashcard deck for today's words—eliminating generic list drop-offs.
- **Confirmed Decision 2 (Task Completion Toggle & Progress)**: Tapping a task checkmark will toggle completion status (`true` <-> `false`), recalculate completed totals dynamically, and display live progress bars (`completedCount / targetCount`) for multi-step tasks (e.g., flashcard reviews, words learned).
- **Confirmed Decision 3 (Real-Time Profile & AI Re-planning Sync)**: When the user edits their profile (level, minutes, goal, decks) or generates a new plan, uncompleted tasks in Today's checklist will automatically regenerate and scale to match the new settings immediately without losing progress on already completed tasks.
- **Confirmed Decision 4 (Goal-Tailored Curriculum & Resilient AI Fallback)**: Fix the AI validation bug where word counts were miscalculated as minutes. Provide dynamic curated curriculum generators for **IELTS Preparation**, **IT & Tech Career**, and **General Fluency** across all levels (A1 to C1) so re-planning works smoothly even when offline or without API keys.
- **Confirmed Decision 5 (Interactive Analytics & Weak-Area Mastery)**: Transform the Analytics tab into an actionable hub: 30-day activity consistency grid, interactive weak grammar cards with direct "Practice Lesson" buttons, and a 1-tap "Practice Weak Words" quiz or flashcard launcher.

---

## 1. Overview & Core Concept

### What It Does
The Study Plan feature acts as an adaptive personal English tutor. It translates the learner's chosen CEFR level (A1–C1), career/learning goal (General Fluency, IELTS, IT Career), daily study minutes (15–60 min), and active days per week into:
1. **Today's Actionable Checklist**: A daily queue of bite-sized, spaced-repetition tasks with live progress tracking, zero-guilt missed day recovery, and direct deep-links.
2. **Monthly Curriculum**: 4–5 milestone weeks with clear thematic focus, weekly targets, and tailored coaching tips in English and Arabic.
3. **Analytics & Recovery Hub**: Visual monthly consistency calendar, skill accuracy benchmarks (Vocab, Grammar, Reading, Listening), and 1-tap targeted review of weak words and grammar rules.

### Key Value Delivered
- **Zero Friction**: 1-tap deep links take the user directly inside the exact lesson, text, or deck they need to study today.
- **True Personalization**: Respects user's actual level (no hardcoded B2 constraints) and aligns weekly themes with their career or academic goals.
- **Unbreakable Momentum**: Flexible rest-day schedules and non-guilt recovery adjustments ensure learners stay motivated even after busy days.

---

## 2. User Experience & Visual Design

### Key User Flows

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                             STUDY PLAN HUB                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│ Level: B2 • 30 min/day • 6 days/wk                  [ Customize Profile ]  │
│ [ Today (3/6) ]          [ Monthly Plan ]               [ Analytics ]       │
└───────────────────────┬───────────────────────────────┬─────────────────────┘
                        │                               │
        ┌───────────────┴──────────────┐   ┌────────────┴─────────────┐
        ▼                              ▼   ▼                          ▼
┌───────────────────────────────┐ ┌───────────────────────────────────────────┐
│ TODAY'S CHECKLIST             │ │ ANALYTICS & WEAK AREAS                    │
│ • Streak: 7 Days 🔥           │ │ • 30-Day Activity Heatmap (Days completed)│
│ • Progress: 50% (3/6 done)    │ │ • Quiz Accuracy by Skill (Bar charts)     │
│                               │ │                                           │
│ [✓] Flashcards (12/12)       │ │ • Weak Vocabulary (1-Tap "Practice Deck") │
│ [ ] Grammar: Conditionals ───►│ │ • Weak Grammar Lessons (1-Tap "Study")   │
│     (Direct Link to Lesson)   │ │ • Start Weekly Comprehensive Quiz (20 Qs) │
│ [ ] 5 Sentences Practice      │ └───────────────────────────────────────────┘
│ [ ] Daily Mini-Quiz (5 Qs)    │
└───────────────────────────────┘
```

### Visual Identity & Theme
- **Theme**: Luxury Dark Gold (`BackgroundDark = #0D1117`, `SurfaceDark = #161B22`, `SurfaceElevatedDark = #21262D`, `BorderSubtle = #30363D`).
- **Accent Tokens**:
  - `GoldAccent = #D4AF37` / `GoldAccentDim = rgba(212, 175, 55, 0.15)` for primary actions, CEFR level badges, and streaks.
  - `EmeraldAccent = #10B981` for completed tasks, passing scores (≥70%), and active calendar days.
  - `CoralAccent = #EF4444` for weak areas and incorrect quiz options.
- **Rhythm & Touch Targets**:
  - Minimum 48dp touch targets on task toggle buttons, audio controls, and navigation arrows.
  - Spaced task cards with clear visual hierarchy: Title in bold English, subtitle with Arabic translation, progress indicators, and actionable buttons.
- **Interactive Micro-Feedback**:
  - Smooth task completion animation: Strikethrough/muted styling on completed tasks with immediate Circular/Linear progress bar updates.
  - Checkbox toggle can be undone at any time with a single tap.

---

## 3. Key Technical Decisions & Diagnostics

### Diagnostic & Solutions Matrix

| Issue Diagnosed | Root Cause | Architectural Solution |
| :--- | :--- | :--- |
| **Task Deep-Linking broken** | `WordBooksAppShell.kt:672` ignored `linkedId` parameter in `onNavigateToTaskRoute`. | Pass `linkedId` and invoke `wordBooksViewModel.selectGrammarLesson(lesson)` or `selectReadingText(text)` before navigating to route. |
| **Task toggle irreversible** | `StudyPlanRepository.kt:484` hardcoded `isCompleted = true`. | Change to `val newState = !it.isCompleted`, updating `completedCount` to `if (newState) it.targetCount else 0`. |
| **Checklist doesn't refresh on profile save** | `getOrCreateDailyChecklist` returned existing entity without recalculating uncompleted tasks. | Create `regenerateChecklistForUpdatedProfile()`, preserving completed tasks while updating uncompleted ones to match new level/targets. |
| **Hardcoded "B2" level** | Static strings, level lists (`listOf("B1", "B2", "C1")`), and hardcoded B2 grammar queries in `StudyPlanRepository.kt`. | Make all level queries dynamic based on `profile.level` (A1 to C1). Fallback gracefully to adjacent levels if no unstudied lessons exist. |
| **AI Plan validation failure** | `StudyPlanAiService.kt:99` added `new_words` (word count) directly into `totalDailyMins` sum. | Separate daily minute allocation checks from word count checks. Add robust goal-tailored template generators for offline/fallback mode. |
| **Setup dialog missing deck & day options** | `PlanSetupDialog.kt` had no deck selection chips or granular rest-day checkboxes. | Add interactive deck multi-select chips and flexible rest-day selector (e.g., Friday, Sunday, or custom). |
| **Analytics missing visuals** | `monthlyChecklists` was unused in `AnalyticsTabContent`. | Add a 30-day activity grid, interactive weak grammar section with 1-tap review, and quick-launch practice session. |

---

## 4. Technical Architecture & Component Flow

```
┌───────────────────────────────────────────────────────────────────────────┐
│                           WORD BOOKS VIEW MODEL                           │
│  - studyPlanProfile: StateFlow<StudyPlanProfileEntity?>                   │
│  - currentStudyPlan: StateFlow<StudyPlanEntity?>                          │
│  - todayChecklist: StateFlow<DailyChecklistEntity?>                       │
│  - studyStreak: StateFlow<Int>                                            │
│  - weakAreas: StateFlow<WeakAreasState>                                   │
│  - monthlyChecklists: StateFlow<List<DailyChecklistEntity>>               │
└─────────────────────────────────────┬─────────────────────────────────────┘
                                      │
              ┌───────────────────────┴───────────────────────┐
              ▼                                               ▼
┌───────────────────────────────┐           ┌────────────────────────────────┐
│      STUDY PLAN REPOSITORY    │           │      STUDY PLAN AI SERVICE     │
│  • getOrCreateProfile()       │           │  • generateMonthlyPlanWithAi() │
│  • updateProfile()            │◄──────────┤  • fixed minute-budget parser  │
│  • getOrCreateDailyChecklist()│           │  • goal-based offline syllabi  │
│  • toggleTaskCompletion()     │           └────────────────────────────────┘
│  • computeStreak()            │
│  • getWeakAreas(level)        │
└─────────────┬─────────────────┘
              │
              ▼
┌───────────────────────────────┐
│        WORD BOOKS DAO         │
│  • daily_checklists           │
│  • study_plan_profiles        │
│  • study_plans                │
│  • quiz_results               │
│  • grammar_lessons & texts    │
└───────────────────────────────┘
```

---

## 5. Implementation Stages & Verification Steps

### Stage 1: Data & Repository Layer Fixes
1. **Update `StudyPlanRepository.kt`**:
   - Fix `toggleTaskCompleted`: Toggle `isCompleted = !it.isCompleted` and update counts.
   - Dynamic CEFR level handling: Use `profile.level` in word candidate queries, grammar lookups, and weak area queries.
   - Profile update synchronization: Automatically re-align uncompleted tasks in Today's checklist when profile settings change.
   - Dynamic Base Plan generation: Tailor default weekly goals and focus themes according to the user's selected learning goal (`general_fluency`, `ielts`, `it_career`).
2. **Update `StudyPlanAiService.kt`**:
   - Fix validation logic in `tryParseAndValidatePlan` (remove `new_words` from minute budget calculation).
   - Enhance prompt to generate tailored milestone goals for the user's specific learning goal and level.
   - Add resilient goal-specific curriculum generation fallback so users always receive a high-quality plan even if offline or AI keys are unavailable.

### Stage 2: Navigation & ViewModel Integration
1. **Update `WordBooksAppShell.kt`**:
   - Implement complete handler for `onNavigateToTaskRoute(route, linkedId)`:
     - For `"grammar"`: Look up lesson by `linkedId`, invoke `wordBooksViewModel.selectGrammarLesson(lesson)`, then navigate to `AppRoute.Grammar`.
     - For `"reading"`: Look up text by `linkedId`, invoke `wordBooksViewModel.selectReadingText(text)`, then navigate to `AppRoute.Reading`.
     - For `"word_books"` / `"flashcards"`: Open corresponding flashcard study view with appropriate deck filter.
2. **Update `WordBooksViewModel.kt`**:
   - Add helper methods to fetch entity by ID for instant task routing.
   - Ensure `refreshDailyChecklist()` publishes fresh state immediately to UI after task toggle or profile change.

### Stage 3: UI & UX Enhancements in Plan Screens
1. **Update `PlanScreen.kt`**:
   - **Today Tab**:
     - Display task progress indicator (`completedCount / targetCount`) on multi-step tasks.
     - Add visual toggle feedback and smooth strikethrough.
     - Add a "Quick Practice Weak Words" shortcut card when weak words exist.
   - **Monthly Plan Tab**:
     - Show clear goal badge (`IELTS Preparation`, `IT Career`, `General Fluency`) alongside CEFR level.
     - Enhance weekly goal cards with expandable milestone details.
   - **Analytics Tab**:
     - Render 30-day monthly activity calendar grid using `monthlyChecklists` with colored streak indicators.
     - Render **Weak Grammar Lessons** list alongside Weak Vocabulary, each with a 1-tap "Study Lesson" button.
     - Add 1-tap "Start Weak Areas Practice" quiz session.
2. **Update `PlanSetupDialog.kt`**:
   - Add interactive Deck Selection chips (Oxford 5000, Oxford OPAL, Idioms & Collocations, etc.).
   - Add customizable Rest Day selector so users can pick any day of the week as their rest day.

### Stage 4: Verification & Automated Tests
1. Run `compile_applet` to verify compilation across all modules.
2. Run `gradle :app:testDebugUnitTest` to verify unit and Robolectric tests for `StudyPlanRepository` and `LocalQuizEngine`.
