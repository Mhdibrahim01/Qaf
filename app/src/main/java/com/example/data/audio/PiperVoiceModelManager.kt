package com.example.data.audio

import android.content.Context
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

enum class TtsEngineMode(val id: String) {
    STANDARD_SYSTEM("system"),
    NATURAL_AI_OFFLINE("natural_ai_offline");

    companion object {
        fun fromId(id: String?): TtsEngineMode =
            entries.firstOrNull { it.id.equals(id?.trim(), ignoreCase = true) } ?: STANDARD_SYSTEM
    }
}

@Immutable
sealed interface NeuralVoicePackState {
    data object NotDownloaded : NeuralVoicePackState
    data class Downloading(
        val progressFraction: Float,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : NeuralVoicePackState
    data object Extracting : NeuralVoicePackState
    data class Downloaded(val sizeOnDiskBytes: Long) : NeuralVoicePackState
    data class Error(val message: String) : NeuralVoicePackState
}

/**
 * Manages the optional downloadable Offline Neural TTS voice pack (`vits-piper-en_US-amy-low`)
 * stored inside `context.filesDir/tts_models/vits-piper-en_US-amy-low/`.
 */
class PiperVoiceModelManager(
    context: Context,
    private val customRootDir: File? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    companion object {
        const val MODEL_FOLDER_NAME = "vits-piper-en_US-amy-low"
        const val ONNX_FILE_NAME = "en_US-amy-low.onnx"
        const val TOKENS_FILE_NAME = "tokens.txt"
        const val ESPEAK_DIR_NAME = "espeak-ng-data"
        const val APPROX_ARCHIVE_BYTES = 22_400_000L // ~21.4 MB

        const val PRIMARY_DOWNLOAD_URL =
            "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-en_US-amy-low.tar.bz2"
    }

    private val appContext = context.applicationContext
    private val ttsModelsRootDir: File
        get() = File(customRootDir ?: appContext.filesDir, "tts_models")

    val modelDir: File
        get() = File(ttsModelsRootDir, MODEL_FOLDER_NAME)

    val onnxModelFile: File
        get() = File(modelDir, ONNX_FILE_NAME)

    val tokensFile: File
        get() = File(modelDir, TOKENS_FILE_NAME)

    val espeakDataDir: File
        get() = File(modelDir, ESPEAK_DIR_NAME)

    private val _state = MutableStateFlow<NeuralVoicePackState>(NeuralVoicePackState.NotDownloaded)
    val state: StateFlow<NeuralVoicePackState> = _state.asStateFlow()

    private var downloadJob: Job? = null

    init {
        refreshInstallationState()
    }

    /**
     * Checks whether the Piper VITS `.onnx` model, `tokens.txt`, and `espeak-ng-data` directory
     * are present and valid on disk.
     */
    fun isModelInstalled(): Boolean {
        val onnxOk = onnxModelFile.exists() && onnxModelFile.isFile && onnxModelFile.length() > 1_000L
        val tokensOk = tokensFile.exists() && tokensFile.isFile && tokensFile.length() > 10L
        val espeakOk = espeakDataDir.exists() && espeakDataDir.isDirectory &&
            (espeakDataDir.list()?.isNotEmpty() == true)
        return onnxOk && tokensOk && espeakOk
    }

    fun refreshInstallationState() {
        if (downloadJob?.isActive == true) return
        if (isModelInstalled()) {
            _state.value = NeuralVoicePackState.Downloaded(computeDirectorySize(modelDir))
        } else {
            _state.value = NeuralVoicePackState.NotDownloaded
        }
    }

    fun startDownload(
        downloadUrl: String = PRIMARY_DOWNLOAD_URL,
        onDownloadCompleted: (() -> Unit)? = null
    ) {
        if (downloadJob?.isActive == true) return
        downloadJob = scope.launch {
            val archiveTempFile = File(ttsModelsRootDir, "$MODEL_FOLDER_NAME.tar.bz2.part")
            val stagingRootDir = File(ttsModelsRootDir, "_staging_${System.nanoTime()}")
            try {
                if (!ttsModelsRootDir.exists()) {
                    ttsModelsRootDir.mkdirs()
                }
                if (archiveTempFile.exists()) {
                    archiveTempFile.delete()
                }
                _state.value = NeuralVoicePackState.Downloading(
                    progressFraction = 0f,
                    downloadedBytes = 0L,
                    totalBytes = APPROX_ARCHIVE_BYTES
                )

                downloadFileWithRedirects(
                    urlString = downloadUrl,
                    destination = archiveTempFile
                ) { downloaded, total ->
                    val effectiveTotal = if (total > 0L) total else APPROX_ARCHIVE_BYTES
                    val fraction = (downloaded.toFloat() / effectiveTotal.toFloat()).coerceIn(0f, 0.99f)
                    _state.value = NeuralVoicePackState.Downloading(
                        progressFraction = fraction,
                        downloadedBytes = downloaded,
                        totalBytes = effectiveTotal
                    )
                }

                ensureActive()
                _state.value = NeuralVoicePackState.Extracting

                if (stagingRootDir.exists()) {
                    stagingRootDir.deleteRecursively()
                }
                stagingRootDir.mkdirs()

                extractTarBz2Archive(archiveTempFile, stagingRootDir)
                archiveTempFile.delete()

                val extractedModelDir = File(stagingRootDir, MODEL_FOLDER_NAME).takeIf { it.exists() }
                    ?: stagingRootDir

                if (modelDir.exists()) {
                    modelDir.deleteRecursively()
                }
                if (!extractedModelDir.renameTo(modelDir)) {
                    extractedModelDir.copyRecursively(modelDir, overwrite = true)
                }
                stagingRootDir.deleteRecursively()

                if (isModelInstalled()) {
                    val totalSize = computeDirectorySize(modelDir)
                    _state.value = NeuralVoicePackState.Downloaded(totalSize)
                    withContext(Dispatchers.Main) {
                        onDownloadCompleted?.invoke()
                    }
                } else {
                    modelDir.deleteRecursively()
                    _state.value = NeuralVoicePackState.Error(
                        "Downloaded archive was incomplete. Please check your connection and try again."
                    )
                }
            } catch (ce: CancellationException) {
                archiveTempFile.delete()
                stagingRootDir.deleteRecursively()
                _state.value = if (isModelInstalled()) {
                    NeuralVoicePackState.Downloaded(computeDirectorySize(modelDir))
                } else {
                    NeuralVoicePackState.NotDownloaded
                }
                throw ce
            } catch (e: Exception) {
                archiveTempFile.delete()
                stagingRootDir.deleteRecursively()
                _state.value = NeuralVoicePackState.Error(
                    e.localizedMessage ?: "Failed to download offline voice pack."
                )
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        val archiveTempFile = File(ttsModelsRootDir, "$MODEL_FOLDER_NAME.tar.bz2.part")
        if (archiveTempFile.exists()) {
            archiveTempFile.delete()
        }
        _state.value = if (isModelInstalled()) {
            NeuralVoicePackState.Downloaded(computeDirectorySize(modelDir))
        } else {
            NeuralVoicePackState.NotDownloaded
        }
    }

    fun deleteVoicePack(onBeforeDelete: (() -> Unit)? = null) {
        cancelDownload()
        onBeforeDelete?.invoke()
        try {
            if (modelDir.exists()) {
                modelDir.deleteRecursively()
            }
        } catch (_: Exception) {}
        _state.value = NeuralVoicePackState.NotDownloaded
    }

    private suspend fun downloadFileWithRedirects(
        urlString: String,
        destination: File,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit
    ) = withContext(Dispatchers.IO) {
        var currentUrl = urlString
        var redirects = 0
        var connection: HttpURLConnection? = null

        try {
            while (redirects < 8) {
                val url = URL(currentUrl)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    instanceFollowRedirects = true
                    setRequestProperty(
                        "User-Agent",
                        "Qaf-Android-OfflineTTS/1.0"
                    )
                }
                val code = connection.responseCode
                if (code in 300..399) {
                    val location = connection.getHeaderField("Location")
                        ?: throw IllegalStateException("Redirect missing Location header (HTTP $code)")
                    connection.disconnect()
                    currentUrl = URL(url, location).toString()
                    redirects++
                    continue
                }
                if (code != HttpURLConnection.HTTP_OK) {
                    throw IllegalStateException("Server returned HTTP $code")
                }

                val contentLength = connection.contentLengthLong
                connection.inputStream.use { input ->
                    FileOutputStream(destination).use { output ->
                        val buffer = ByteArray(32 * 1024)
                        var downloaded = 0L
                        var lastEmitTime = 0L
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            coroutineContext.ensureActive()
                            output.write(buffer, 0, read)
                            downloaded += read
                            val now = System.currentTimeMillis()
                            if (now - lastEmitTime >= 80L || downloaded == contentLength) {
                                lastEmitTime = now
                                onProgress(downloaded, contentLength)
                            }
                        }
                        output.flush()
                    }
                }
                return@withContext
            }
            throw IllegalStateException("Too many HTTP redirects while downloading voice pack")
        } finally {
            connection?.disconnect()
        }
    }

    internal fun extractTarBz2Archive(archiveFile: File, targetDir: File) {
        val canonicalTargetRoot = targetDir.canonicalPath + File.separator
        FileInputStream(archiveFile).use { fis ->
            BufferedInputStream(fis, 64 * 1024).use { bis ->
                BZip2CompressorInputStream(bis).use { bzis ->
                    TarArchiveInputStream(bzis).use { tis ->
                        val buffer = ByteArray(32 * 1024)
                        var entry = tis.nextEntry
                        while (entry != null) {
                            val outFile = File(targetDir, entry.name)
                            val canonicalOutPath = outFile.canonicalPath
                            if (!canonicalOutPath.startsWith(canonicalTargetRoot) &&
                                canonicalOutPath != targetDir.canonicalPath
                            ) {
                                throw SecurityException("Blocked unsafe tar entry path: ${entry.name}")
                            }
                            if (entry.isDirectory) {
                                outFile.mkdirs()
                            } else {
                                outFile.parentFile?.mkdirs()
                                FileOutputStream(outFile).use { fos ->
                                    var len: Int
                                    while (tis.read(buffer).also { len = it } != -1) {
                                        fos.write(buffer, 0, len)
                                    }
                                }
                            }
                            entry = tis.nextEntry
                        }
                    }
                }
            }
        }
    }

    private fun computeDirectorySize(dir: File): Long {
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }
}
