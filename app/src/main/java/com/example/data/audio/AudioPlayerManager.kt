package com.example.data.audio

import android.content.Context
import android.os.Handler
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.metadata.MetadataOutput
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.text.TextOutput
import androidx.media3.exoplayer.video.VideoRendererEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.ArrayList
import java.util.Locale

/**
 * Plays word or sentence audio lazily (never probing codecs or TTS on app startup):
 * 1. If `audioUrl` is non-blank, streams it via an audio-only Media3 ExoPlayer with browser User-Agent
 *    (falling back to Android TextToSpeech on any network/codec error).
 * 2. Otherwise speaks `text` using lazily-initialized Android TextToSpeech (US English locale).
 */
class AudioPlayerManager(context: Context) {

    private val appContext = context.applicationContext

    private val _currentlyPlayingWord = MutableStateFlow<String?>(null)
    val currentlyPlayingWord: StateFlow<String?> = _currentlyPlayingWord.asStateFlow()

    private var exoPlayer: ExoPlayer? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var isTtsInitializing = false
    private var pendingTtsText: String? = null
    private var pendingSpeechRate: Float? = null
    private var pendingLocale: Locale = Locale.US
    private var defaultSpeechRate: Float = 0.95f
    private var preferUkAccent: Boolean = false

    fun setDefaultSpeechRate(rate: Float) {
        defaultSpeechRate = rate.coerceIn(0.5f, 1.5f)
        tts?.setSpeechRate(defaultSpeechRate)
    }

    fun getDefaultSpeechRate(): Float = defaultSpeechRate

    fun setPreferUkAccent(isUk: Boolean) {
        preferUkAccent = isUk
        val targetLocale = if (isUk) Locale.UK else Locale.US
        pendingLocale = targetLocale
        if (isTtsReady && tts != null) {
            tts?.language = targetLocale
        }
    }

    fun isPreferUkAccent(): Boolean = preferUkAccent

    private fun ensureTtsInitialized() {
        if (tts != null || isTtsInitializing) return
        isTtsInitializing = true
        try {
            tts = TextToSpeech(appContext) { status ->
                isTtsInitializing = false
                if (status == TextToSpeech.SUCCESS) {
                    val targetLocale = pendingLocale
                    val result = tts?.setLanguage(targetLocale)
                    isTtsReady = result != TextToSpeech.LANG_MISSING_DATA &&
                        result != TextToSpeech.LANG_NOT_SUPPORTED
                    val rateToUse = pendingSpeechRate ?: defaultSpeechRate
                    tts?.setSpeechRate(rateToUse)
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {}
                        override fun onDone(utteranceId: String?) {
                            _currentlyPlayingWord.value = null
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            _currentlyPlayingWord.value = null
                        }
                    })
                    val pending = pendingTtsText
                    pendingTtsText = null
                    pendingSpeechRate = null
                    if (!pending.isNullOrBlank() && isTtsReady) {
                        tts?.language = targetLocale
                        tts?.speak(pending, TextToSpeech.QUEUE_FLUSH, null, "utt_${pending.hashCode()}_${targetLocale.country}")
                    } else if (!isTtsReady) {
                        _currentlyPlayingWord.value = null
                    }
                } else {
                    pendingTtsText = null
                    pendingSpeechRate = null
                    _currentlyPlayingWord.value = null
                }
            }
        } catch (_: Exception) {
            isTtsInitializing = false
            pendingTtsText = null
            pendingSpeechRate = null
            _currentlyPlayingWord.value = null
        }
    }

    @OptIn(UnstableApi::class)
    private fun getOrCreateExoPlayer(): ExoPlayer {
        val existing = exoPlayer
        if (existing != null) return existing

        val audioOnlyRenderersFactory = object : DefaultRenderersFactory(appContext) {
            override fun buildVideoRenderers(
                context: Context,
                extensionRendererMode: Int,
                mediaCodecSelector: MediaCodecSelector,
                enableDecoderFallback: Boolean,
                eventHandler: Handler,
                eventListener: VideoRendererEventListener,
                allowedVideoJoiningTimeMs: Long,
                out: ArrayList<Renderer>
            ) {
                // Skip video renderers to prevent Codec2 video resource queries on audio-only playback
            }

            override fun buildTextRenderers(
                context: Context,
                output: TextOutput,
                outputLooper: android.os.Looper,
                extensionRendererMode: Int,
                out: ArrayList<Renderer>
            ) {
                // Skip text renderers
            }

            override fun buildMetadataRenderers(
                context: Context,
                output: MetadataOutput,
                outputLooper: android.os.Looper,
                extensionRendererMode: Int,
                out: ArrayList<Renderer>
            ) {
                // Skip metadata renderers
            }

            override fun buildCameraMotionRenderers(
                context: Context,
                extensionRendererMode: Int,
                out: ArrayList<Renderer>
            ) {
                // Skip camera motion renderers
            }

            override fun buildImageRenderers(out: ArrayList<Renderer>) {
                // Skip image renderers
            }
        }.setEnableDecoderFallback(true)

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
            .setConnectTimeoutMs(10_000)
            .setReadTimeoutMs(10_000)
            .setAllowCrossProtocolRedirects(true)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .build()

        return ExoPlayer.Builder(appContext, audioOnlyRenderersFactory)
            .setMediaSourceFactory(DefaultMediaSourceFactory(httpDataSourceFactory))
            .setAudioAttributes(audioAttributes, true)
            .build()
            .also { player ->
                player.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED || playbackState == Player.STATE_IDLE) {
                            _currentlyPlayingWord.value = null
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        val activeWord = _currentlyPlayingWord.value
                        if (!activeWord.isNullOrBlank()) {
                            speakWithTts(activeWord)
                        } else {
                            _currentlyPlayingWord.value = null
                        }
                    }
                })
                exoPlayer = player
            }
    }

    fun playWordOrUrl(text: String, audioUrl: String = "") {
        playWordWithVoice(text, audioUrl, isGb = preferUkAccent)
    }

    fun playWordWithVoice(text: String, audioUrl: String = "", isGb: Boolean = false) {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return
        _currentlyPlayingWord.value = text
        val targetLocale = if (isGb) Locale.UK else Locale.US

        if (!isGb && audioUrl.isNotBlank() && (audioUrl.startsWith("http://") || audioUrl.startsWith("https://"))) {
            try {
                val player = getOrCreateExoPlayer()
                player.stop()
                player.setMediaItem(MediaItem.fromUri(audioUrl))
                player.prepare()
                player.play()
            } catch (_: Exception) {
                speakWithTts(cleanText, locale = targetLocale)
            }
        } else {
            speakWithTts(cleanText, locale = targetLocale)
        }
    }

    fun speakSentenceWithTts(text: String, speechRateOverride: Float? = null) {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return
        _currentlyPlayingWord.value = text
        val targetLocale = if (preferUkAccent) Locale.UK else Locale.US
        speakWithTts(cleanText, speechRateOverride, locale = targetLocale)
    }

    fun stopAudio() {
        try {
            exoPlayer?.stop()
        } catch (_: Exception) {}
        try {
            tts?.stop()
        } catch (_: Exception) {}
        _currentlyPlayingWord.value = null
    }

    private fun speakWithTts(
        text: String,
        speechRateOverride: Float? = null,
        locale: Locale = Locale.US
    ) {
        val rate = (speechRateOverride ?: defaultSpeechRate).coerceIn(0.5f, 1.5f)
        if (isTtsReady && tts != null) {
            tts?.language = locale
            tts?.setSpeechRate(rate)
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utt_${text.hashCode()}_${locale.country}")
        } else {
            pendingTtsText = text
            pendingSpeechRate = rate
            pendingLocale = locale
            ensureTtsInitialized()
        }
    }

    fun release() {
        try {
            exoPlayer?.release()
        } catch (_: Exception) {
        }
        exoPlayer = null
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {
        }
        tts = null
        isTtsReady = false
        isTtsInitializing = false
        pendingTtsText = null
        _currentlyPlayingWord.value = null
    }
}
