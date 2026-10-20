# 🗺️ قاف | Qaf — Future Feature Roadmap & Implementation Blueprint

This document serves as the self-contained engineering roadmap for **Qaf (قاف)**. Each feature is designed to integrate directly with Qaf's **Single Source of Truth (SSOT)** architecture (`WordBooksRepository` + `WordBooksDatabase`), **60/120 FPS** Jetpack Compose UI, and **Obsidian Navy (`#0B0F17`) + Warm Gold (`#F59E0B`)** visual identity.

> 💡 **How to Use This Roadmap Later**:
> Whenever you want to build one of these features in AI Studio, simply copy the **"Quick-Start Prompt"** under that feature and paste it into the chat.

---

## 📊 Phased Priority Overview

| Phase | Feature | Effort | Primary Learning Pillar | Core Dependencies |
| :--- | :--- | :--- | :--- | :--- |
| **Phase 1A** | **Mistake Notebook & Weak-Words ("Leech") Drill** | ⚡ Quick Win | Active Retention & Remediation | Room SSOT (`word_progress.again_count`, `correction_notebook`) |
| **Phase 1B** | **Smart Daily Study Reminders & Streak Freeze** | ⚡ Quick Win | Daily Habit & Consistency | `WorkManager`, `NotificationCompat`, `study_plan_profile` |
| **Phase 2A** | **Interactive Listening & Dictation Challenges** | 🛠️ Medium | Listening & Spelling Accuracy | Android TTS / Audio Cache, `SentencesScreen`, `quiz_results` |
| **Phase 2B** | **Home Screen "Word of the Day" Widget** | 🛠️ Medium | Passive Recall & Quick Access | Jetpack Glance (`GlanceAppWidget`), Room SSOT DAO |
| **Phase 3A** | **Voice Pronunciation & Shadow Speaking Lab** | 🚀 Advanced | Speaking & Phonetic Accuracy | Android `SpeechRecognizer`, `RECORD_AUDIO`, Gemini Coaching |
| **Phase 3B** | **Cloud Backup, JSON Export & Multi-Device Sync** | 🚀 Advanced | Data Durability & Cross-Device | SAF JSON Export + Google Sign-In (`CredentialManager`) & Firestore |

---

## ⚡ Phase 1: Quick Wins (Retention & Daily Habits)

### Feature 1A: Mistake Notebook & Weak-Words ("Leech") Drill Mode
**خزانة الأخطاء وتدريب الكلمات الصعبة (Leech Words)**

#### 1. Concept & User Value
Learners often get stuck on the same 5–10% of "leech" words (words rated **Again** 3+ times in SRS or missed repeatedly in Grammar/Reading quizzes). This feature aggregates those weak spots into a dedicated remediation queue with a rapid 10-question drill mode.

#### 2. User Experience & Visual Flow
- **Home Dashboard & SRS Banner**: Displays a warm coral-amber card (`#F97316` accent on `#131B2B` slate surface) whenever the learner has `>= 5` weak words (`again_count >= 3 AND is_mastered = 0`).
- **Mistake Notebook Tabs**:
  1. **Weak Vocabulary (`Leech Words`)**: Sorted by highest `again_count`, showing the word, IPA, Arabic meaning, and error count badge.
  2. **Grammar & Reading Mistakes**: Pulls logged quiz errors from `correction_notebook` and `quiz_results`.
- **Rapid Remediation Drill**:
  - 10-card focused micro-session mixing:
    1. *Arabic → English Multiple Choice*
    2. *Cloze Sentence Completion* (fill the blank in context)
    3. *Spelling Recall*
  - Answering a word correctly **2 consecutive times** decrements its `again_count` and graduates it out of the Weak Words list with an Emerald (`#10B981`) celebration badge.

#### 3. Technical Implementation Checklist
- [ ] **DAO Query (`WordBooksDao.kt`)**:
  - Add `observeWeakWords(minAgainCount: Int = 3): Flow<List<EntryWithProgress>>` querying `entries INNER JOIN word_progress WHERE again_count >= :minAgainCount AND is_mastered = 0 ORDER BY again_count DESC`.
  - Add `recordDrillSuccess(entryId: String)` to decrement `again_count` (down to `0`) and update `last_reviewed_timestamp`.
- [ ] **Repository & ViewModel (`WordBooksRepository.kt`)**:
  - Expose `weakWordsFlow: Flow<List<EntryWithProgress>>` and `mistakeNotebookFlow: Flow<List<CorrectionNotebookEntity>>`.
- [ ] **Compose UI (`WeakWordsDrillSheet.kt` / `MistakeNotebookScreen.kt`)**:
  - Build the 3-stage interactive drill card with `AnimatedContent` transitions and `testTag("start_weak_words_drill_button")`.

> 💬 **Quick-Start Prompt to Build Feature 1A:**
> *"Implement Phase 1A from ROADMAP.md: Mistake Notebook & Weak-Words ('Leech') Drill Mode using the existing `again_count` in `word_progress` and `correction_notebook` in our SSOT Room database."*

---

### Feature 1B: Smart Daily Study Reminders & Streak Freeze
**التذكير اليومي الذكي ودرع حماية السلسلة (Streak Freeze)**

#### 1. Concept & User Value
Protects learner motivation by dispatching a context-aware daily study notification (featuring a live due word from their SRS queue) and awarding **Streak Freeze Shields** (`❄️`) for every 7-day streak milestone so a single busy day never wipes out weeks of progress.

#### 2. User Experience & Visual Flow
- **Streak Shield Indicator**: Top App Bar and Home Dashboard streak pill display an Ice Cyan (`#38BDF8`) shield badge (`🛡️ x1` or `🛡️ x2`, max capacity 2).
- **Automatic Protection Banner**: If the user misses a study day and has `streakFreezeCount > 0`, the shield is consumed automatically and a reassuring banner appears on launch: *"تم استخدام درع الحماية للحفاظ على سلسلتك اليومية!"*.
- **Interactive Daily Notification**:
  - Scheduled at the user's chosen time (default `20:00`).
  - Displays a due SRS word directly in the notification body (`"Word due: Resilient (مرن / صامد) — Tap to review"`).
  - Includes a direct notification action (`"Mark Mastered ✓"`) via `BroadcastReceiver` so learners can interact without even opening the full app.

#### 3. Technical Implementation Checklist
- [ ] **Permissions (`AndroidManifest.xml`)**:
  - Declare `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />` and request it gracefully in Compose on Android 13+ (`TIRAMISU`).
- [ ] **Data Persistence**:
  - Store `streakFreezeCount` (0–2), `lastProtectedDate`, `reminderEnabled`, `reminderHour`, and `reminderMinute` in DataStore / `study_plan_profile`.
- [ ] **Background Worker (`DailyStudyReminderWorker.kt`)**:
  - Schedule a periodic 24-hour `CoroutineWorker` via `WorkManager`.
  - Check if today's `daily_task_checklist` is already completed; if completed, skip the notification so the user is never spammed.

> 💬 **Quick-Start Prompt to Build Feature 1B:**
> *"Implement Phase 1B from ROADMAP.md: Smart Daily Study Reminders with a due SRS word notification action and Streak Freeze Shields."*

---

## 🛠️ Phase 2: Medium Effort (Audio Dictation & Home Screen Presence)

### Feature 2A: Interactive Listening & Dictation Challenges
**تحديات الاستماع والإملاء التفاعلي (Audio Dictation Mode)**

#### 1. Concept & User Value
Transforms passive sentence browsing and SRS reviews into an active listening and spelling workout. Learners hear a sentence or word spoken aloud while the English text is hidden, then reconstruct what they heard.

#### 2. User Experience & Visual Flow
- **Entry Point**: Toggle chip **"🎧 Dictation / إملاء سمعي"** in the **Sentences** screen header and **SRS Review** mode selector.
- **Dual Playback Controls**:
  - **Normal Speed (`1.0x`)** primary gold button (`#F59E0B`).
  - **Slow Turtle Speed (`0.70x`)** secondary button for connected-speech clarity.
- **Two Difficulty Modes**:
  1. **Word Tile Builder (Easy / Medium)**: Tap scrambled word chips in the order heard to assemble the sentence.
  2. **Full Keyboard Dictation (Hard / IELTS Prep)**: Type the heard sentence and receive a character-by-character diff highlight (green for exact match, amber underline for minor typos, red strikethrough for missing words).
- **SSOT Integration**: Logs results to `quiz_results` (`quiz_type = "DICTATION"`, `skill = "LISTENING"`) and increments `again_count` on missed target words.

#### 3. Technical Implementation Checklist
- [ ] **Audio Rate Support**:
  - Add playback rate control (`speechRate = 0.70f` vs `1.0f`) to the TTS/Audio playback helper.
- [ ] **Diff & Scoring Engine (`DictationEvaluator.kt`)**:
  - Implement token-level and Levenshtein character-level comparison ignoring punctuation case differences, returning `DictationFeedback(accuracyPercent, tokenDiffs)`.
- [ ] **UI Composable (`DictationChallengeSheet.kt`)**:
  - Build both the scrambled word-bank flow (`FlowRow` with animated chip placement) and the keyboard input flow.

> 💬 **Quick-Start Prompt to Build Feature 2A:**
> *"Implement Phase 2A from ROADMAP.md: Interactive Listening & Dictation Challenges with 1.0x/0.7x audio playback, scrambled word tiles, and keyboard character-diff scoring."*

---

### Feature 2B: Android Home Screen "Word of the Day" Widget
**ويدجت الشاشة الرئيسية لكلمة اليوم (Qaf Home Screen Widget)**

#### 1. Concept & User Value
Keeps English vocabulary visible every time the user unlocks their phone, rotating through unmastered words from their active deck and allowing one-tap audio playback or mastery directly from the Android home screen.

#### 2. User Experience & Visual Flow
- **Obsidian & Gold Aesthetic**: Styled with Qaf's signature `#0B0F17` dark background, `#F59E0B` gold Arabic **ق** badge, English headword, IPA phonetics, Arabic translation, and 1 contextual example sentence.
- **Interactive Widget Buttons**:
  - **🔄 Next Word**: Cycles to another due/unmastered word from the user's selected study decks.
  - **🔊 Listen**: Triggers background pronunciation audio playback.
  - **✓ Mastered**: Marks the word as mastered in the Room SSOT database and immediately advances to the next word.

#### 3. Technical Implementation Checklist
- [ ] **Dependencies (`app/build.gradle.kts`)**:
  - Add `androidx.glance:glance-appwidget` and `androidx.glance:glance-material3`.
- [ ] **Widget Receiver & Provider (`QafWordWidget.kt` & `QafWordWidgetReceiver.kt`)**:
  - Register `<receiver android:name=".ui.widget.QafWordWidgetReceiver" ...>` with `android.appwidget.action.APPWIDGET_UPDATE` and `res/xml/qaf_word_widget_info.xml`.
- [ ] **SSOT Synchronizer**:
  - Trigger `QafWordWidget().updateAll(context)` inside `WordBooksRepository` whenever mastery state changes in the main app so the widget and app stay 100% in sync.

> 💬 **Quick-Start Prompt to Build Feature 2B:**
> *"Implement Phase 2B from ROADMAP.md: Android Home Screen 'Word of the Day' Widget matching Qaf's dark navy and gold theme with Next, Listen, and Mastered actions synced with our SSOT database."*

---

## 🚀 Phase 3: Advanced Capabilities (Voice Speaking Lab & Cloud Sync)

### Feature 3A: Voice Pronunciation & Shadow Speaking Lab
**مختبر النطق والتحدث التفاعلي (Shadow Speaking Lab)**

#### 1. Concept & User Value
Adds active **Speaking** practice to Qaf. Learners listen to a native word or sentence, tap the microphone to shadow-read it aloud, and receive an instant pronunciation accuracy score with targeted feedback on tricky English phonemes for Arabic speakers (such as `/p/` vs `/b/`, `/v/` vs `/f/`, `/θ/`, and `/ð/`).

#### 2. User Experience & Visual Flow
- **In-Card Mic Button**: Every Word Detail sheet, SRS Flashcard, and Sentence card gains a **"🎙️ Practice Speaking"** action.
- **Real-Time Waveform & Transcript**:
  - Displays an animated gold audio visualizer while listening to the user's voice.
  - Shows word-by-word color coding:
    - **Emerald (`#10B981`)**: Pronounced clearly.
    - **Amber (`#F59E0B`)**: Close/partial phonetic match (e.g., *berry* instead of *very*).
    - **Coral (`#EF4444`)**: Missed or skipped word.
- **Bilingual Articulation Tip**: For mispronounced words, displays a concise Arabic tip explaining tongue/lip placement (with optional `gemini-3.5-flash` phonetic coaching).

#### 3. Technical Implementation Checklist
- [ ] **Permissions (`AndroidManifest.xml`)**:
  - Add `<uses-permission android:name="android.permission.RECORD_AUDIO" />` and request runtime permission via `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())`.
- [ ] **Speech Recognition Engine (`PronunciationEvaluator.kt`)**:
  - Wrap Android `SpeechRecognizer` (`RecognizerIntent.ACTION_RECOGNIZE_SPEECH`, `en-US` / `en-GB`) in a lifecycle-aware Kotlin `callbackFlow` emitting partial and final transcripts plus confidence scores.
  - Include a built-in Arabic-speaker phonetic confusion matrix (`/p/↔/b/`, `/v/↔/f/`, `/tʃ/↔/ʃ/`) to give specific Arabic articulation guidance even offline.

> 💬 **Quick-Start Prompt to Build Feature 3A:**
> *"Implement Phase 3A from ROADMAP.md: Voice Pronunciation & Shadow Speaking Lab using Android SpeechRecognizer with word-by-word accuracy scoring and Arabic phonetic tips."*

---

### Feature 3B: Cloud Backup, JSON Export & Multi-Device Sync
**النسخ الاحتياطي السحابي والمزامنة عبر الأجهزة (Cloud & Local Backup)**

#### 1. Concept & User Value
Protects the learner's hard-earned SRS history, mastered words, custom study plans, and saved sentences against phone loss or device upgrades using a two-tier durability architecture:
1. **Zero-Account Local JSON Backup**: Export/Import a single `.qafbackup.json` file to device storage or Google Drive via the Android Storage Access Framework (SAF).
2. **Real-Time Cloud Sync**: Sign in with Google (`CredentialManager`) to sync progress automatically across phones and tablets via **Firebase Firestore**.

#### 2. User Experience & Visual Flow
- **Backup & Sync Hub (in Settings / Profile)**:
  - **Local Snapshot Card**: *"Export Backup (.json)"* and *"Restore from Backup"* buttons showing the total count of mastered words, SRS reviews, and study plans included.
  - **Google Cloud Sync Card**:
    - Unauthenticated state: Displays a **"Sign in with Google"** button (`GetSignInWithGoogleOption`).
    - Authenticated state: Displays the user's Google profile badge, *"Last synced: Just now"*, and a *"Sync Now"* trigger.

#### 3. Technical Implementation Checklist
- [ ] **Tier 1 — Local JSON Backup (`BackupRestoreManager.kt`)**:
  - Serialize `word_progress`, `deck_study_state`, `study_plan_profile`, `study_plans`, `daily_task_checklist`, `grammar_progress`, `reading_progress`, and `correction_notebook` into a versioned `@Serializable data class QafBackupPayload`.
  - Wire `ActivityResultContracts.CreateDocument("application/json")` and `ActivityResultContracts.OpenDocument()` in Compose.
- [ ] **Tier 2 — Firebase Auth & Firestore Sync (`firebase-firestore-android` skill)**:
  - Provision Firebase (`set_up_firebase` with `"firestore"`).
  - Authenticate with Google Sign-In via Jetpack `CredentialManager` (`GetSignInWithGoogleOption` using `R.string.default_web_client_id`).
  - Scope all user documents under `/users/{uid}/wordProgress/{entryId}` and `/users/{uid}/profile/studyState` with zero-trust `firestore.rules` (`request.auth != null && request.auth.uid == userId`).
  - Resolve conflicts deterministically using `updatedAt` timestamps (`Last-Write-Wins` + union of `isMastered = true`).

> 💬 **Quick-Start Prompt to Build Feature 3B:**
> *"Implement Phase 3B from ROADMAP.md: Start with Local JSON Backup & Restore in Settings, or set up Firebase Firestore Cloud Sync with Google Sign-In."*
