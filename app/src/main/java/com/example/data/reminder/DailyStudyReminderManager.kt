package com.example.data.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.EntryEntity
import com.example.data.local.WordBooksDao
import com.example.data.local.WordBooksDatabase
import com.example.data.local.WordProgressEntity
import com.example.data.onboarding.OnboardingPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DailyStudyReminderManager {
    const val CHANNEL_ID = "qaf_daily_study_reminder"
    const val NOTIFICATION_ID = 4102

    const val ACTION_TRIGGER_REMINDER = "com.example.action.TRIGGER_DAILY_REMINDER"
    const val ACTION_SPEAK_WORD = "com.example.action.REMINDER_SPEAK_WORD"
    const val ACTION_MARK_MASTERED = "com.example.action.REMINDER_MARK_MASTERED"

    const val EXTRA_ENTRY_ID = "extra_entry_id"
    const val EXTRA_DECK_ID = "extra_deck_id"
    const val EXTRA_WORD_TEXT = "extra_word_text"
    const val EXTRA_ARABIC_MEANING = "extra_arabic_meaning"

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "منبه الدراسة اليومي • Daily Study Reminder",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "إشعار يومي ذكي لمراجعة الكلمات المستحقة وحماية سلسلة التعلم"
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun computeNextTriggerTimeMillis(
        reminderMinutesFromMidnight: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        val clamped = reminderMinutesFromMidnight.coerceIn(0, 23 * 60 + 59)
        val targetHour = clamped / 60
        val targetMin = clamped % 60
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMin)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= nowMillis) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    fun scheduleOrCancelDailyReminder(
        context: Context,
        enabled: Boolean,
        reminderMinutesFromMidnight: Int
    ) {
        val appContext = context.applicationContext
        ensureNotificationChannel(appContext)
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(appContext, DailyStudyReminderReceiver::class.java).apply {
            action = ACTION_TRIGGER_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (!enabled) {
            alarmManager.cancel(pendingIntent)
            return
        }

        val triggerAtMillis = computeNextTriggerTimeMillis(reminderMinutesFromMidnight)
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    /**
     * Checks whether today's study checklist is already completed; if not, selects a due SRS word
     * and posts an interactive bilingual notification with "Listen" and "Mark Mastered" actions.
     */
    suspend fun checkAndDispatchReminder(
        context: Context,
        dao: WordBooksDao,
        onboardingRepo: OnboardingPreferencesRepository,
        nowMillis: Long = System.currentTimeMillis()
    ): EntryEntity? {
        val snap = onboardingRepo.getSnapshot()
        if (!snap.studyReminderEnabled) return null

        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(nowMillis))
        val todayChecklist = dao.getDailyChecklist(todayStr)
        if (todayChecklist?.isCompleted == true) {
            // User already completed their daily study goals — do not disturb
            return null
        }

        val endOfToday = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.timeInMillis

        val candidate = dao.getDueTodayEntriesOrdered(endOfToday).firstOrNull()
            ?: dao.getEntriesWithExamples(10).firstOrNull()
            ?: return null

        postWordReminderNotification(context, candidate)
        return candidate
    }

    fun postWordReminderNotification(context: Context, entry: EntryEntity) {
        val appContext = context.applicationContext
        ensureNotificationChannel(appContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val openAppIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            appContext,
            NOTIFICATION_ID + 1,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val speakIntent = Intent(appContext, DailyStudyReminderReceiver::class.java).apply {
            action = ACTION_SPEAK_WORD
            putExtra(EXTRA_WORD_TEXT, entry.text)
        }
        val speakPendingIntent = PendingIntent.getBroadcast(
            appContext,
            NOTIFICATION_ID + 2,
            speakIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val masterIntent = Intent(appContext, DailyStudyReminderReceiver::class.java).apply {
            action = ACTION_MARK_MASTERED
            putExtra(EXTRA_ENTRY_ID, entry.id)
            putExtra(EXTRA_DECK_ID, entry.deck)
            putExtra(EXTRA_WORD_TEXT, entry.text)
        }
        val masterPendingIntent = PendingIntent.getBroadcast(
            appContext,
            NOTIFICATION_ID + 3,
            masterIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val meaningText = entry.arabic.ifBlank { entry.definitionEn }.take(60)
        val contentLine = "كلمة مستحقة: ${entry.text} ($meaningText) — اضغط للمراجعة السريعة"

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("قاف • وقت مراجعة الكلمات اليومية 📖")
            .setContentText(contentLine)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentLine))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(0, "🔊 استمع / Listen", speakPendingIntent)
            .addAction(0, "✓ حفظت الكلمة / Mastered", masterPendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(appContext).notify(NOTIFICATION_ID, notification)
        }
    }

    suspend fun markWordMasteredFromNotification(
        dao: WordBooksDao,
        deckId: String,
        wordText: String,
        entryId: String?,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val cleanDeck = deckId.ifBlank { "oxford5000" }
        val cleanText = wordText.trim().lowercase(Locale.US)
        if (cleanText.isBlank()) return
        val cardKey = "$cleanDeck::$cleanText"
        val existing = dao.getWordProgress(cardKey)
        val updated = (existing ?: WordProgressEntity(
            cardKey = cardKey,
            entryId = entryId,
            deck = cleanDeck,
            text = cleanText
        )).copy(
            isMastered = true,
            lastReviewedTimestamp = nowMillis,
            firstLearnedTimestamp = existing?.firstLearnedTimestamp?.takeIf { it > 0L } ?: nowMillis
        )
        dao.upsertWordProgress(updated)
    }
}

class DailyStudyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        scope.launch {
            try {
                val appContext = context.applicationContext
                val db = WordBooksDatabase.getInstance(appContext)
                val dao = db.wordBooksDao()
                val onboardingRepo = OnboardingPreferencesRepository(appContext)

                when (action) {
                    Intent.ACTION_BOOT_COMPLETED -> {
                        val snap = onboardingRepo.getSnapshot()
                        DailyStudyReminderManager.scheduleOrCancelDailyReminder(
                            context = appContext,
                            enabled = snap.studyReminderEnabled,
                            reminderMinutesFromMidnight = snap.studyReminderTimeMinutes
                        )
                    }

                    DailyStudyReminderManager.ACTION_TRIGGER_REMINDER -> {
                        DailyStudyReminderManager.checkAndDispatchReminder(
                            context = appContext,
                            dao = dao,
                            onboardingRepo = onboardingRepo
                        )
                    }

                    DailyStudyReminderManager.ACTION_MARK_MASTERED -> {
                        val entryId = intent.getStringExtra(DailyStudyReminderManager.EXTRA_ENTRY_ID)
                        val deckId = intent.getStringExtra(DailyStudyReminderManager.EXTRA_DECK_ID).orEmpty()
                        val wordText = intent.getStringExtra(DailyStudyReminderManager.EXTRA_WORD_TEXT).orEmpty()
                        DailyStudyReminderManager.markWordMasteredFromNotification(
                            dao = dao,
                            deckId = deckId,
                            wordText = wordText,
                            entryId = entryId
                        )
                        runCatching {
                            NotificationManagerCompat.from(appContext).cancel(DailyStudyReminderManager.NOTIFICATION_ID)
                        }
                    }

                    DailyStudyReminderManager.ACTION_SPEAK_WORD -> {
                        val wordText = intent.getStringExtra(DailyStudyReminderManager.EXTRA_WORD_TEXT).orEmpty()
                        if (wordText.isNotBlank()) {
                            speakSingleWordOneShot(appContext, wordText)
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun speakSingleWordOneShot(context: Context, word: String) {
        var ttsRef: TextToSpeech? = null
        ttsRef = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsRef?.language = Locale.US
                ttsRef?.setSpeechRate(0.85f)
                ttsRef?.speak(word, TextToSpeech.QUEUE_FLUSH, null, "reminder_word_utt")
            }
        }
    }
}
