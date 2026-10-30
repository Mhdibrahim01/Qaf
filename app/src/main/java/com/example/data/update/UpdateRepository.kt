package com.example.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** Mirrors the `update.json` asset published with every GitHub Release (see docs/APK_UPDATE_PLAN.md). */
@Serializable
data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val sha256: String,
    val notes: String = ""
)

/**
 * Checks GitHub Releases for a newer APK, downloads it (SHA-256 verified) and hands it to the
 * system installer. Android always asks the user to confirm the install; Android also rejects
 * any APK that is not signed with the same key as the installed app.
 */
class UpdateRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Returns the latest release info, or null when offline / malformed / unavailable. */
    suspend fun fetchLatest(): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL(MANIFEST_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                instanceFollowRedirects = true
            }
            try {
                if (conn.responseCode != HttpURLConnection.HTTP_OK) return@runCatching null
                val info = conn.inputStream.bufferedReader().use { json.decodeFromString<UpdateInfo>(it.readText()) }
                if (isTrustedApkUrl(info.apkUrl)) info else null
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    fun isNewer(info: UpdateInfo): Boolean = info.versionCode > BuildConfig.VERSION_CODE

    /** True when the automatic check should run (at most once per [CHECK_INTERVAL_MS]). */
    fun shouldAutoCheck(now: Long = System.currentTimeMillis()): Boolean =
        now - prefs.getLong(KEY_LAST_CHECK, 0L) >= CHECK_INTERVAL_MS

    fun markChecked(now: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(KEY_LAST_CHECK, now).apply()
    }

    fun isSkipped(info: UpdateInfo, now: Long = System.currentTimeMillis()): Boolean =
        prefs.getInt(KEY_SKIPPED_CODE, -1) == info.versionCode &&
            now - prefs.getLong(KEY_SKIPPED_AT, 0L) < SKIP_DURATION_MS

    fun skip(info: UpdateInfo, now: Long = System.currentTimeMillis()) {
        prefs.edit().putInt(KEY_SKIPPED_CODE, info.versionCode).putLong(KEY_SKIPPED_AT, now).apply()
    }

    /** Downloads to cache/updates/update.apk and verifies the SHA-256. Returns null on any failure. */
    suspend fun download(info: UpdateInfo, onProgress: (Float) -> Unit): File? = withContext(Dispatchers.IO) {
        if (!isTrustedApkUrl(info.apkUrl)) return@withContext null
        var out: File? = null
        runCatching {
            val dir = File(context.cacheDir, "updates").apply {
                mkdirs()
                listFiles()?.forEach { it.delete() }
            }
            val target = File(dir, "update.apk")
            out = target
            val conn = (URL(info.apkUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                instanceFollowRedirects = true
            }
            try {
                if (conn.responseCode != HttpURLConnection.HTTP_OK) return@runCatching null
                val total = conn.contentLengthLong.coerceAtLeast(1L)
                val digest = MessageDigest.getInstance("SHA-256")
                conn.inputStream.use { input ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var done = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            digest.update(buffer, 0, read)
                            done += read
                            onProgress((done.toFloat() / total).coerceIn(0f, 1f))
                        }
                    }
                }
                val hex = digest.digest().joinToString("") { "%02x".format(it) }
                if (hex.equals(info.sha256.trim(), ignoreCase = true)) target else null
            } finally {
                conn.disconnect()
            }
        }.getOrNull().also { result ->
            if (result == null) out?.delete()
        }
    }

    /**
     * Opens the system installer for [apk]. Returns false when the user first has to allow
     * "Install unknown apps" for this app (the settings screen is opened; user taps Update again).
     */
    fun install(apk: File): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return false
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
        return true
    }

    companion object {
        const val MANIFEST_URL = "https://github.com/Mhdibrahim01/Qaf/releases/latest/download/update.json"
        private const val TRUSTED_APK_PREFIX = "https://github.com/Mhdibrahim01/Qaf/releases/download/"
        private const val PREFS_NAME = "app_update_prefs"
        private const val KEY_LAST_CHECK = "last_check"
        private const val KEY_SKIPPED_CODE = "skipped_code"
        private const val KEY_SKIPPED_AT = "skipped_at"
        private const val CHECK_INTERVAL_MS = 12L * 60 * 60 * 1000
        private const val SKIP_DURATION_MS = 3L * 24 * 60 * 60 * 1000

        /** Only download APKs from this repo's release assets over HTTPS. */
        fun isTrustedApkUrl(url: String): Boolean = url.startsWith(TRUSTED_APK_PREFIX)
    }
}
