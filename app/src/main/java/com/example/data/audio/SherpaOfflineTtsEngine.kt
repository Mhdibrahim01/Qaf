package com.example.data.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import android.os.SystemClock

/**
 * Wraps Sherpa-ONNX [OfflineTts] (Piper VITS model) and Android [AudioTrack] PCM playback
 * to synthesize natural offline speech from files stored in `context.filesDir/tts_models/`.
 */
class SherpaOfflineTtsEngine(
    private val modelManager: PiperVoiceModelManager
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    private val ttsDispatcher = Dispatchers.Default.limitedParallelism(1)
    private val scope = CoroutineScope(SupervisorJob() + ttsDispatcher)

    private var offlineTts: OfflineTts? = null
    private var loadedModelPath: String? = null
    private var activeAudioTrack: AudioTrack? = null
    private var activeSpeakJob: Job? = null

    private fun getOrCreateOfflineTts(): OfflineTts? {
        if (!modelManager.isModelInstalled()) return null
        val targetModelPath = modelManager.onnxModelFile.absolutePath
        val existing = offlineTts
        if (existing != null && loadedModelPath == targetModelPath) {
            return existing
        }
        if (existing != null) {
            try {
                existing.release()
            } catch (_: Throwable) {}
            offlineTts = null
            loadedModelPath = null
        }

        val vitsConfig = OfflineTtsVitsModelConfig(
            model = targetModelPath,
            lexicon = "",
            tokens = modelManager.tokensFile.absolutePath,
            dataDir = modelManager.espeakDataDir.absolutePath,
            dictDir = "",
            noiseScale = 0.667f,
            noiseScaleW = 0.8f,
            lengthScale = 1.0f
        )
        val modelConfig = OfflineTtsModelConfig(
            vits = vitsConfig,
            numThreads = 2,
            debug = false,
            provider = "cpu"
        )
        val config = OfflineTtsConfig(
            model = modelConfig,
            ruleFsts = "",
            ruleFars = "",
            maxNumSentences = 1,
            silenceScale = 0.2f
        )
        return OfflineTts(assetManager = null, config = config).also {
            offlineTts = it
            loadedModelPath = targetModelPath
        }
    }

    /**
     * Preloads the ONNX model in the background so the first word plays with zero initialization lag.
     */
    fun preload() {
        scope.launch {
            try {
                getOrCreateOfflineTts()
            } catch (t: Throwable) {
                Log.e("SherpaOfflineTts", "Failed to preload OfflineTts model", t)
            }
        }
    }

    /**
     * Synthesizes [text] at [speechRate] using the local Piper VITS model and plays the resulting
     * PCM float samples via [AudioTrack]. Calls [onError] if the model is missing or JNI fails so
     * [AudioPlayerManager] can fall back to standard Android TTS.
     */
    fun speak(
        text: String,
        speechRate: Float,
        onComplete: () -> Unit,
        onError: () -> Unit
    ) {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) {
            onComplete()
            return
        }

        stopActivePlaybackOnly()

        activeSpeakJob = scope.launch {
            try {
                val engine = getOrCreateOfflineTts()
                if (engine == null) {
                    Log.w("SherpaOfflineTts", "Model not installed; falling back to system TTS")
                    onError()
                    return@launch
                }

                ensureActive()
                val clampedSpeed = speechRate.coerceIn(0.5f, 1.5f)
                val generated = engine.generate(
                    text = cleanText,
                    sid = 0,
                    speed = clampedSpeed
                )

                ensureActive()
                val samples = generated.samples
                val sampleRate = generated.sampleRate
                if (samples.isEmpty() || sampleRate <= 0) {
                    Log.w("SherpaOfflineTts", "Generated empty samples for '$cleanText'")
                    onError()
                    return@launch
                }

                playPcmFloatSamples(samples, sampleRate)
                onComplete()
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                Log.e("SherpaOfflineTts", "Error synthesizing speech with Sherpa-ONNX", t)
                onError()
            }
        }
    }

    private suspend fun CoroutineScope.playPcmFloatSamples(samples: FloatArray, sampleRate: Int) {
        val hwMinBuffer = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_FLOAT
        )
        val minBufferBytes = if (hwMinBuffer > 0) hwMinBuffer.coerceAtLeast(4096) else 8192

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()

        val track = AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(minBufferBytes)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        activeAudioTrack = track
        try {
            track.play()
            var offset = 0
            val chunkSize = 1024
            while (offset < samples.size) {
                ensureActive()
                val count = minOf(chunkSize, samples.size - offset)
                val written = track.write(samples, offset, count, AudioTrack.WRITE_BLOCKING)
                if (written <= 0) break
                offset += written
            }
            // Wait for AudioTrack hardware mixer to finish draining all frames before stopping/releasing,
            // otherwise short words (< 1s) get cut off while still buffered in the stream ring buffer.
            val expectedDurationMs = ((samples.size.toLong() * 1000L) / sampleRate.coerceAtLeast(1)).coerceAtLeast(120L)
            val deadlineMs = SystemClock.elapsedRealtime() + expectedDurationMs + 400L
            while (SystemClock.elapsedRealtime() < deadlineMs) {
                ensureActive()
                val head = try {
                    track.playbackHeadPosition
                } catch (_: Throwable) {
                    samples.size
                }
                if (head >= samples.size) break
                delay(15)
            }
            track.stop()
        } finally {
            try {
                track.release()
            } catch (_: Throwable) {}
            if (activeAudioTrack === track) {
                activeAudioTrack = null
            }
        }
    }

    private fun stopActivePlaybackOnly() {
        activeSpeakJob?.cancel()
        activeSpeakJob = null
        val track = activeAudioTrack
        activeAudioTrack = null
        if (track != null) {
            try {
                track.pause()
                track.flush()
                track.release()
            } catch (_: Throwable) {}
        }
    }

    fun stop() {
        stopActivePlaybackOnly()
    }

    fun releaseEngine() {
        stopActivePlaybackOnly()
        scope.launch {
            val engine = offlineTts
            offlineTts = null
            if (engine != null) {
                try {
                    engine.release()
                } catch (_: Throwable) {}
            }
        }
    }
}
