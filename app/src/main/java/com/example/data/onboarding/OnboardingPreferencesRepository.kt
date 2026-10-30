package com.example.data.onboarding

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.onboardingDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "qaf_onboarding_prefs"
)

@Immutable
data class OnboardingPreferencesState(
    val onboardingCompleted: Boolean = false,
    val lastCompletedStep: Int = 0,
    val selectedLevel: String = "B2",
    val selectedGoal: String = "general_fluency",
    val selectedMinutesPerDay: Int = 30,
    val selectedStudyDaysPerWeek: Int = 6,
    val selectedDecksJson: String = "",
    val startAtUserLevel: Boolean = true,
    val isRedoSetup: Boolean = false,
    val coachTipFlipDismissed: Boolean = false,
    val coachTipRateDismissed: Boolean = false,
    val coachTipWordTapDismissed: Boolean = false,
    val firstSessionCompleted: Boolean = false,
    val notificationPromptShown: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val preferredTopicsJson: String = "[]",
    val quietModeEnabled: Boolean = false,
    val isDeveloperModeEnabled: Boolean = false,
    val preferredAccentUk: Boolean = false,
    val autoPlayPronunciation: Boolean = false,
    val ttsSpeechRate: Float = 0.85f,
    val ttsEngineMode: String = "system",
    val themeMode: String = "dark",
    val appLanguage: String = "en"
) {
    /**
     * Returns the 1-indexed step (1..5) that should currently be shown when onboarding is active.
     */
    val currentStepToResume: Int
        get() = (lastCompletedStep + 1).coerceIn(1, 5)
}

/**
 * Persists first-run onboarding state, resume step, and coach tip flags in Jetpack DataStore.
 */
class OnboardingPreferencesRepository(
    private val context: Context,
    private val customDataStore: DataStore<Preferences>? = null
) {
    private val dataStore: DataStore<Preferences>
        get() = customDataStore ?: context.applicationContext.onboardingDataStore

    companion object {
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_LAST_COMPLETED_STEP = intPreferencesKey("last_completed_step")
        val KEY_SELECTED_LEVEL = stringPreferencesKey("selected_level")
        val KEY_SELECTED_GOAL = stringPreferencesKey("selected_goal")
        val KEY_SELECTED_MINUTES = intPreferencesKey("selected_minutes_per_day")
        val KEY_SELECTED_STUDY_DAYS = intPreferencesKey("selected_study_days_per_week")
        val KEY_SELECTED_DECKS = stringPreferencesKey("selected_decks_json")
        val KEY_START_AT_LEVEL = booleanPreferencesKey("start_at_user_level")
        val KEY_IS_REDO_SETUP = booleanPreferencesKey("is_redo_setup")
        val KEY_COACH_TIP_FLIP_DISMISSED = booleanPreferencesKey("coach_tip_flip_dismissed")
        val KEY_COACH_TIP_RATE_DISMISSED = booleanPreferencesKey("coach_tip_rate_dismissed")
        val KEY_COACH_TIP_WORD_TAP_DISMISSED = booleanPreferencesKey("coach_tip_word_tap_dismissed")
        val KEY_FIRST_SESSION_COMPLETED = booleanPreferencesKey("first_session_completed")
        val KEY_NOTIFICATION_PROMPT_SHOWN = booleanPreferencesKey("notification_prompt_shown")
        val KEY_REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val KEY_REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val KEY_PREFERRED_TOPICS = stringPreferencesKey("preferred_topics_json")
        val KEY_QUIET_MODE = booleanPreferencesKey("quiet_mode_enabled")
        val KEY_DEVELOPER_MODE_ENABLED = booleanPreferencesKey("developer_mode_enabled")
        val KEY_PREFERRED_ACCENT_UK = booleanPreferencesKey("preferred_accent_uk")
        val KEY_AUTO_PLAY_PRONUNCIATION = booleanPreferencesKey("auto_play_pronunciation")
        val KEY_TTS_SPEECH_RATE = floatPreferencesKey("tts_speech_rate")
        val KEY_TTS_ENGINE_MODE = stringPreferencesKey("tts_engine_mode")
        val KEY_THEME_MODE = stringPreferencesKey("app_theme_mode")
        val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")

        /**
         * Kept false until the daily reminder WorkManager notification feature is enabled.
         */
        const val ENABLE_DAILY_REMINDER_PROMPT = false
    }

    val stateFlow: Flow<OnboardingPreferencesState> = dataStore.data.map { prefs ->
        OnboardingPreferencesState(
            onboardingCompleted = prefs[KEY_ONBOARDING_COMPLETED] ?: false,
            lastCompletedStep = (prefs[KEY_LAST_COMPLETED_STEP] ?: 0).coerceIn(0, 5),
            selectedLevel = prefs[KEY_SELECTED_LEVEL] ?: "B2",
            selectedGoal = prefs[KEY_SELECTED_GOAL] ?: "general_fluency",
            selectedMinutesPerDay = prefs[KEY_SELECTED_MINUTES] ?: 30,
            selectedStudyDaysPerWeek = prefs[KEY_SELECTED_STUDY_DAYS] ?: 6,
            selectedDecksJson = prefs[KEY_SELECTED_DECKS] ?: "",
            startAtUserLevel = prefs[KEY_START_AT_LEVEL] ?: true,
            isRedoSetup = prefs[KEY_IS_REDO_SETUP] ?: false,
            coachTipFlipDismissed = prefs[KEY_COACH_TIP_FLIP_DISMISSED] ?: false,
            coachTipRateDismissed = prefs[KEY_COACH_TIP_RATE_DISMISSED] ?: false,
            coachTipWordTapDismissed = prefs[KEY_COACH_TIP_WORD_TAP_DISMISSED] ?: false,
            firstSessionCompleted = prefs[KEY_FIRST_SESSION_COMPLETED] ?: false,
            notificationPromptShown = prefs[KEY_NOTIFICATION_PROMPT_SHOWN] ?: false,
            reminderHour = prefs[KEY_REMINDER_HOUR] ?: 20,
            reminderMinute = prefs[KEY_REMINDER_MINUTE] ?: 0,
            preferredTopicsJson = prefs[KEY_PREFERRED_TOPICS] ?: "[]",
            quietModeEnabled = prefs[KEY_QUIET_MODE] ?: false,
            isDeveloperModeEnabled = prefs[KEY_DEVELOPER_MODE_ENABLED] ?: false,
            preferredAccentUk = prefs[KEY_PREFERRED_ACCENT_UK] ?: false,
            autoPlayPronunciation = prefs[KEY_AUTO_PLAY_PRONUNCIATION] ?: false,
            ttsSpeechRate = prefs[KEY_TTS_SPEECH_RATE] ?: 0.85f,
            ttsEngineMode = prefs[KEY_TTS_ENGINE_MODE] ?: "system",
            themeMode = prefs[KEY_THEME_MODE] ?: "dark",
            appLanguage = prefs[KEY_APP_LANGUAGE] ?: "en"
        )
    }

    suspend fun getSnapshot(): OnboardingPreferencesState = stateFlow.first()

    suspend fun setLastCompletedStep(step: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_LAST_COMPLETED_STEP] = step.coerceIn(0, 5)
            if (step >= 5) {
                prefs[KEY_ONBOARDING_COMPLETED] = true
                prefs[KEY_IS_REDO_SETUP] = false
            }
        }
    }

    suspend fun saveSelectedLevel(level: String) {
        dataStore.edit { prefs ->
            prefs[KEY_SELECTED_LEVEL] = level.trim().uppercase()
        }
    }

    suspend fun saveGoalAndTime(
        goal: String,
        minutesPerDay: Int,
        studyDaysPerWeek: Int,
        preferredTopicsJson: String? = null,
        quietModeEnabled: Boolean? = null
    ) {
        dataStore.edit { prefs ->
            prefs[KEY_SELECTED_GOAL] = goal
            prefs[KEY_SELECTED_MINUTES] = minutesPerDay
            prefs[KEY_SELECTED_STUDY_DAYS] = studyDaysPerWeek.coerceIn(1, 7)
            if (preferredTopicsJson != null) {
                prefs[KEY_PREFERRED_TOPICS] = preferredTopicsJson
            }
            if (quietModeEnabled != null) {
                prefs[KEY_QUIET_MODE] = quietModeEnabled
            }
        }
    }

    suspend fun saveSelectedDecks(decksJson: String, startAtUserLevel: Boolean = true) {
        dataStore.edit { prefs ->
            prefs[KEY_SELECTED_DECKS] = decksJson
            prefs[KEY_START_AT_LEVEL] = startAtUserLevel
        }
    }

    suspend fun markOnboardingCompleted() {
        dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETED] = true
            prefs[KEY_LAST_COMPLETED_STEP] = 5
            prefs[KEY_IS_REDO_SETUP] = false
        }
    }

    /**
     * Re-enters onboarding at Step 1 without deleting any progress, streaks, saved sentences,
     * notebook entries, checklists, or backups, and pre-populates from existing profile if provided.
     */
    suspend fun startRedoSetup(
        existingLevel: String? = null,
        existingGoal: String? = null,
        existingMinutes: Int? = null,
        existingStudyDays: Int? = null
    ) {
        dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETED] = false
            prefs[KEY_LAST_COMPLETED_STEP] = 0
            prefs[KEY_IS_REDO_SETUP] = true
            if (!existingLevel.isNullOrBlank()) {
                prefs[KEY_SELECTED_LEVEL] = existingLevel
            }
            if (!existingGoal.isNullOrBlank()) {
                prefs[KEY_SELECTED_GOAL] = existingGoal
            }
            if (existingMinutes != null && existingMinutes > 0) {
                prefs[KEY_SELECTED_MINUTES] = existingMinutes
            }
            if (existingStudyDays != null && existingStudyDays in 1..7) {
                prefs[KEY_SELECTED_STUDY_DAYS] = existingStudyDays
            }
        }
    }

    suspend fun dismissCoachTipFlip() {
        dataStore.edit { prefs ->
            prefs[KEY_COACH_TIP_FLIP_DISMISSED] = true
        }
    }

    suspend fun dismissCoachTipRate() {
        dataStore.edit { prefs ->
            prefs[KEY_COACH_TIP_RATE_DISMISSED] = true
        }
    }

    suspend fun dismissCoachTipWordTap() {
        dataStore.edit { prefs ->
            prefs[KEY_COACH_TIP_WORD_TAP_DISMISSED] = true
        }
    }

    suspend fun resetCoachTips() {
        dataStore.edit { prefs ->
            prefs[KEY_COACH_TIP_FLIP_DISMISSED] = false
            prefs[KEY_COACH_TIP_RATE_DISMISSED] = false
            prefs[KEY_COACH_TIP_WORD_TAP_DISMISSED] = false
        }
    }

    suspend fun markFirstSessionCompleted() {
        dataStore.edit { prefs ->
            prefs[KEY_FIRST_SESSION_COMPLETED] = true
        }
    }

    suspend fun markNotificationPromptShown(hour: Int = 20, minute: Int = 0) {
        dataStore.edit { prefs ->
            prefs[KEY_NOTIFICATION_PROMPT_SHOWN] = true
            prefs[KEY_REMINDER_HOUR] = hour.coerceIn(0, 23)
            prefs[KEY_REMINDER_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    suspend fun setDeveloperModeEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_DEVELOPER_MODE_ENABLED] = enabled
        }
    }

    suspend fun setPreferredAccentUk(isUk: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_PREFERRED_ACCENT_UK] = isUk
        }
    }

    suspend fun setAutoPlayPronunciation(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_AUTO_PLAY_PRONUNCIATION] = enabled
        }
    }

    suspend fun setTtsSpeechRate(rate: Float) {
        dataStore.edit { prefs ->
            prefs[KEY_TTS_SPEECH_RATE] = rate.coerceIn(0.5f, 1.5f)
        }
    }

    suspend fun setTtsEngineMode(modeId: String) {
        dataStore.edit { prefs ->
            prefs[KEY_TTS_ENGINE_MODE] = modeId.trim().lowercase()
        }
    }

    suspend fun setThemeMode(modeId: String) {
        dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = modeId.trim().lowercase()
        }
    }

    suspend fun setAppLanguage(languageCode: String) {
        dataStore.edit { prefs ->
            prefs[KEY_APP_LANGUAGE] = languageCode.trim().lowercase()
        }
    }

    suspend fun clearAllForTest() {
        dataStore.edit { it.clear() }
    }
}
