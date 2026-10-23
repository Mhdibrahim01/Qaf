package com.example.data.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
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
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

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
    private var activeAudioTrack: AudioTrack? = null
    private var activeSpeakJob: Job? = null

    private fun getOrCreateOfflineTts(): OfflineTts? {
        val existing = offlineTts
        if (existing != null) return existing
        if (!modelManager.isModelInstalled()) return null

        val vitsConfig = OfflineTtsVitsModelConfig(
            model = modelManager.onnxModelFile.absolutePath,
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
                    onError()
                    return@launch
                }

                playPcmFloatSamples(samples, sampleRate)
                onComplete()
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: Throwable) {
                onError()
            }
        }
    }

    private fun CoroutineScope.playPcmFloatSamples(samples: FloatArray, sampleRate: Int) {
        val minBufferBytes = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_FLOAT
        ).coerceAtLeast(sampleRate * 4)

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
            val chunkSize = 2048
            while (offset < samples.size) {
                ensureActive()
                val count = minOf(chunkSize, samples.size - offset)
                val written = track.write(samples, offset, count, AudioTrack.WRITE_BLOCKING)
                if (written <= 0) break
                offset += written
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
