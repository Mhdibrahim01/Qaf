package com.example.data.ai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * Foreground service that keeps Batch Translate running when the user leaves the screen
 * and shows a live progress notification with "done / total" and batches completed.
 */
class BatchTranslateForegroundService : Service() {

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> {
                BatchTranslateManager.InstanceHolder.current?.pauseBatchTranslate()
            }
            ACTION_STOP -> {
                BatchTranslateManager.InstanceHolder.current?.stopBatchTranslate()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }

        val state = BatchTranslateManager.InstanceHolder.current?.uiState?.value
        val notification = buildProgressNotification(state)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: Exception) {
            // In restricted background contexts, fallback to updating NotificationManager directly
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.notify(NOTIFICATION_ID, notification)
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Batch Vocabulary Translation",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress while translating vocabulary decks with Gemini"
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildProgressNotification(state: BatchTranslateUiState?): Notification {
        val done = state?.doneCount ?: 0
        val total = state?.totalCount ?: 0
        val batchesDone = state?.batchesCompleted ?: 0
        val totalBatches = state?.totalBatches ?: 0
        val title = if (state?.isPaused == true) {
            "Batch Translate Paused ($done / $total)"
        } else {
            "Batch Translating ($done / $total words)"
        }
        val content = if (totalBatches > 0) {
            "Batches: $batchesDone / $totalBatches • Translated: ${state?.translatedCount ?: 0}"
        } else {
            state?.statusMessage?.ifBlank { "Preparing batch translation..." } ?: "Running..."
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(content)
            .setOngoing(state?.isRunning == true)
            .setOnlyAlertOnce(true)
            .setProgress(total.coerceAtLeast(1), done.coerceAtMost(total.coerceAtLeast(1)), total == 0)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "batch_translate_channel"
        const val NOTIFICATION_ID = 4201
        const val ACTION_START_OR_UPDATE = "com.example.action.BATCH_TRANSLATE_UPDATE"
        const val ACTION_PAUSE = "com.example.action.BATCH_TRANSLATE_PAUSE"
        const val ACTION_STOP = "com.example.action.BATCH_TRANSLATE_STOP"

        fun startOrUpdate(context: Context) {
            runCatching {
                val intent = Intent(context, BatchTranslateForegroundService::class.java).apply {
                    action = ACTION_START_OR_UPDATE
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }

        fun stop(context: Context) {
            runCatching {
                val intent = Intent(context, BatchTranslateForegroundService::class.java)
                context.stopService(intent)
            }
        }
    }
}
