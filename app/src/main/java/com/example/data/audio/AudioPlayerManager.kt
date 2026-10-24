package com.example.data.audio

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
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
 * Plays word or sentence audio lazily and reliably:
 * 1. If [audioUrl] is a valid remote dictionary audio URL, streams it via an audio-only Media3 ExoPlayer
 *    (falling back to Android TextToSpeech on any network/codec error).
 * 2. Otherwise synthesizes [text] using Android TextToSpeech, explicitly prioritizing Google Speech Services
 *    (`com.google.android.tts`) and Google's high-quality offline neural voices (`en-us-x-*-local`), with
 *    automatic fallback to the device's default TTS engine if Google TTS is unavailable.
 * 3. Calibrates natural human resonance with a 0.98f pitch, supporting dual speeds:
 *    - Normal Speech: 0.85f
 *    - Slow Turtle: 0.60f (0.70f for pre-recorded dictionary audio)
 */
class AudioPlayerManager(context: Context) {

    companion object {
        const val GOOGLE_TTS_PACKAGE = "com.google.android.tts"
        const val DEFAULT_NORMAL_RATE = 0.85f
        const val SLOW_TURTLE_RATE = 0.60f
        const val EXO_SLOW_RATE = 0.70f
        const val NATURAL_PITCH = 0.98f

        val PREFERRED_GOOGLE_NEURAL_VOICES = setOf(
            "en-us-x-tpf-local",
            "en-us-x-sfg-local",
            "en-us-x-iob-local",
            "en-us-x-iom-local",
            "en-us-x-iol-local",
            "en-us-x-tpc-local"
        )

        /**
         * Returns true if [locale] represents British English (`en-GB` / `en-UK`).
         */
        fun isUkLocale(locale: Locale): Boolean {
            val country = locale.country
            return country.equals("GB", ignoreCase = true) || country.equals("UK", ignoreCase = true)
        }

        /**
         * Scores an Android [Voice] candidate for [locale] selection.
         * Prioritizes Google's high-quality local neural voices for zero-latency offline pronunciation.
         * Returns a negative value if the voice does not match the language/country or is not installed.
         */
        fun scoreVoiceForLocale(voice: Voice, locale: Locale): Int {
            if (!voice.locale.language.equals(locale.language, ignoreCase = true)) return -1
            if (locale.country.isNotBlank()) {
                val targetIsUk = isUkLocale(locale)
                val voiceIsUk = isUkLocale(voice.locale)
                val countryMatches = if (targetIsUk) {
                    voiceIsUk
                } else {
                    voice.locale.country.equals(locale.country, ignoreCase = true)
                }
                if (!countryMatches) return -1
            }
            val features = voice.features ?: emptySet()
            if (features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) ||
                features.contains("notInstalled")
            ) {
                return -1
            }

            var score = 0
            val nameLower = voice.name.lowercase(Locale.ROOT)

            // Top priority: Known Google high-quality offline neural voices
            if (PREFERRED_GOOGLE_NEURAL_VOICES.any { nameLower.contains(it) }) {
                score += 1000
            }

            score += when (voice.quality) {
                Voice.QUALITY_VERY_HIGH -> 500
                Voice.QUALITY_HIGH -> 400
                Voice.QUALITY_NORMAL -> 300
                Voice.QUALITY_LOW -> 100
                else -> 50
            }

            // Prefer local offline voices for zero-latency, reliable speech
            if (!voice.isNetworkConnectionRequired) {
                score += 100
            }

            if (nameLower.contains("local") ||
                nameLower.contains("-x-") ||
                nameLower.contains("wavenet") ||
                nameLower.contains("neural")
            ) {
                score += 50
            }

            return score
        }
    }

    private val appContext = context.applicationContext

    private val mainHandler = Handler(Looper.getMainLooper())

    private val ttsStreamParams: Bundle by lazy {
        Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
        }
    }

    /**
     * Returns true if Google Speech Services is installed and enabled on this device.
     */
    fun isGoogleTtsAvailable(): Boolean {
        return try {
            val intent = Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE).apply {
                setPackage(GOOGLE_TTS_PACKAGE)
            }
            val list = appContext.packageManager.queryIntentServices(intent, 0)
            list.isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }

    init {
        // Pre-warm TTS asynchronously so the very first voice tap is instant
        mainHandler.post {
            ensureTtsInitialized()
        }
    }

    private val _currentlyPlayingWord = MutableStateFlow<String?>(null)
    val currentlyPlayingWord: StateFlow<String?> = _currentlyPlayingWord.asStateFlow()

    private val _isPlayingSlow = MutableStateFlow(false)
    val isPlayingSlow: StateFlow<Boolean> = _isPlayingSlow.asStateFlow()

    private val _currentlyPlayingIsGb = MutableStateFlow(false)
    val currentlyPlayingIsGb: StateFlow<Boolean> = _currentlyPlayingIsGb.asStateFlow()

    private var exoPlayer: ExoPlayer? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var isTtsInitializing = false
    private var pendingTtsText: String? = null
    private var pendingSpeechRate: Float? = null
    private var pendingLocale: Locale = Locale.US
    private var defaultSpeechRate: Float = DEFAULT_NORMAL_RATE
    private var preferUkAccent: Boolean = false
    private var lastExoWasSlow: Boolean = false
    private var lastRequestedIsGb: Boolean = false

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
            applyBestVoiceAndLocale(targetLocale)
        }
    }

    fun isPreferUkAccent(): Boolean = preferUkAccent

    /**
     * Queries the active TTS engine for all available voices matching [locale]
     * and selects the highest-quality neural voice (preferring local offline voices).
     */
    fun findBestVoiceForLocale(locale: Locale): Voice? {
        val engine = tts ?: return null
        val availableVoices = try {
            engine.voices
        } catch (_: Exception) {
            null
        } ?: return null

        return availableVoices
            .map { voice -> voice to scoreVoiceForLocale(voice, locale) }
            .filter { (_, score) -> score >= 0 }
            .maxByOrNull { (_, score) -> score }
            ?.first
    }

    private fun applyBestVoiceAndLocale(locale: Locale) {
        val engine = tts ?: return
        try {
            val bestInstalledVoice = findBestVoiceForLocale(locale)
            if (bestInstalledVoice != null) {
                engine.voice = bestInstalledVoice
            }
            engine.setPitch(NATURAL_PITCH)
        } catch (_: Exception) {
            try {
                engine.setPitch(NATURAL_PITCH)
            } catch (_: Exception) {}
        }
    }

    /**
     * Attempts to configure the TTS engine for [requestedLocale], cascading through
     * fallback English dialects (e.g. US, UK, generic English, device default) if the
     * specific accent data is not installed on the user's phone.
     */
    private fun setupLanguageWithFallback(requestedLocale: Locale): Boolean {
        val engine = tts ?: return false
        val candidates = listOf(
            requestedLocale,
            if (isUkLocale(requestedLocale)) Locale.US else Locale.UK,
            Locale.US,
            Locale.ENGLISH,
            Locale.getDefault()
        ).distinct()

        for (candidate in candidates) {
            try {
                val result = engine.setLanguage(candidate)
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    applyBestVoiceAndLocale(candidate)
                    return true
                }
            } catch (_: Exception) {}
        }
        return false
    }

    private fun ensureTtsInitialized(useGoogleEngine: Boolean = true) {
        if (tts != null || isTtsInitializing) return
        isTtsInitializing = true

        val shouldUseGoogle = useGoogleEngine && isGoogleTtsAvailable()

        try {
            val initListener = TextToSpeech.OnInitListener { status ->
                isTtsInitializing = false
                if (status == TextToSpeech.SUCCESS) {
                    try {
                        val speechAudioAttrs = android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                        tts?.setAudioAttributes(speechAudioAttrs)
                    } catch (_: Exception) {}

                    val targetLocale = pendingLocale
                    isTtsReady = setupLanguageWithFallback(targetLocale)

                    val rateToUse = pendingSpeechRate ?: defaultSpeechRate
                    tts?.setSpeechRate(rateToUse)
                    tts?.setPitch(NATURAL_PITCH)
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {}
                        override fun onDone(utteranceId: String?) {
                            _currentlyPlayingWord.value = null
                            _isPlayingSlow.value = false
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            _currentlyPlayingWord.value = null
                            _isPlayingSlow.value = false
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            _currentlyPlayingWord.value = null
                            _isPlayingSlow.value = false
                        }
                    })

                    val pending = pendingTtsText
                    pendingTtsText = null
                    pendingSpeechRate = null
                    if (!pending.isNullOrBlank() && isTtsReady) {
                        applyBestVoiceAndLocale(targetLocale)
                        tts?.setSpeechRate(rateToUse)
                        tts?.speak(
                            pending,
                            TextToSpeech.QUEUE_FLUSH,
                            ttsStreamParams,
                            "utt_${pending.hashCode()}_${targetLocale.country}_${System.nanoTime()}"
                        )
                    } else if (!isTtsReady) {
                        try {
                            tts?.shutdown()
                        } catch (_: Exception) {}
                        tts = null
                        _currentlyPlayingWord.value = null
                        _isPlayingSlow.value = false
                    }
                } else if (shouldUseGoogle) {
                    // Gracefully fallback to system default TTS engine scheduled asynchronously
                    // to prevent constructor re-entrancy / overwriting
                    mainHandler.post {
                        try {
                            tts?.shutdown()
                        } catch (_: Exception) {}
                        tts = null
                        ensureTtsInitialized(useGoogleEngine = false)
                    }
                } else {
                    pendingTtsText = null
                    pendingSpeechRate = null
                    _currentlyPlayingWord.value = null
                    _isPlayingSlow.value = false
                    try {
                        tts?.shutdown()
                    } catch (_: Exception) {}
                    tts = null
                }
            }

            tts = if (shouldUseGoogle) {
                TextToSpeech(appContext, initListener, GOOGLE_TTS_PACKAGE)
            } else {
                TextToSpeech(appContext, initListener)
            }
        } catch (_: Exception) {
            isTtsInitializing = false
            if (shouldUseGoogle) {
                mainHandler.post {
                    try {
                        tts?.shutdown()
                    } catch (_: Exception) {}
                    tts = null
                    ensureTtsInitialized(useGoogleEngine = false)
                }
            } else {
                pendingTtsText = null
                pendingSpeechRate = null
                _currentlyPlayingWord.value = null
                _isPlayingSlow.value = false
                try {
                    tts?.shutdown()
                } catch (_: Exception) {}
                tts = null
            }
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
                            _isPlayingSlow.value = false
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        val activeWord = _currentlyPlayingWord.value
                        val wasSlow = lastExoWasSlow
                        val wasGb = lastRequestedIsGb
                        if (!activeWord.isNullOrBlank()) {
                            speakWithTts(
                                text = activeWord,
                                speechRateOverride = if (wasSlow) SLOW_TURTLE_RATE else null,
                                locale = if (wasGb) Locale.UK else Locale.US
                            )
                        } else {
                            _currentlyPlayingWord.value = null
                            _isPlayingSlow.value = false
                        }
                    }
                })
                exoPlayer = player
            }
    }

    fun playWordOrUrl(text: String, audioUrl: String = "") {
        playWordWithVoice(text, audioUrl, isGb = preferUkAccent, isSlow = false)
    }

    fun playWordWithVoice(
        text: String,
        audioUrl: String = "",
        isGb: Boolean = false,
        isSlow: Boolean = false
    ) {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return

        // Stop any previous playback before switching voice/accent
        try {
            exoPlayer?.stop()
        } catch (_: Exception) {}
        try {
            tts?.stop()
        } catch (_: Exception) {}

        _currentlyPlayingWord.value = text
        _isPlayingSlow.value = isSlow
        _currentlyPlayingIsGb.value = isGb
        lastRequestedIsGb = isGb
        val targetLocale = if (isGb) Locale.UK else Locale.US
        val rateOverride = if (isSlow) SLOW_TURTLE_RATE else null

        if (audioUrl.isNotBlank() && (audioUrl.startsWith("http://") || audioUrl.startsWith("https://"))) {
            try {
                lastExoWasSlow = isSlow
                val player = getOrCreateExoPlayer()
                player.stop()
                player.playbackParameters = PlaybackParameters(if (isSlow) EXO_SLOW_RATE else 1.0f)
                player.setMediaItem(MediaItem.fromUri(audioUrl))
                player.prepare()
                player.play()
            } catch (_: Exception) {
                speakWithTts(cleanText, speechRateOverride = rateOverride, locale = targetLocale)
            }
        } else {
            speakWithTts(cleanText, speechRateOverride = rateOverride, locale = targetLocale)
        }
    }

    /**
     * Plays a word at slow turtle speed (`0.60x` for TTS, `0.70x` for pre-recorded dictionary MP3).
     */
    fun playWordSlow(text: String, audioUrl: String = "", isGb: Boolean = preferUkAccent) {
        playWordWithVoice(text = text, audioUrl = audioUrl, isGb = isGb, isSlow = true)
    }

    fun speakSentenceWithTts(text: String, speechRateOverride: Float? = null) {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return
        _currentlyPlayingWord.value = text
        _isPlayingSlow.value = speechRateOverride != null && speechRateOverride <= SLOW_TURTLE_RATE + 0.05f
        val targetLocale = if (preferUkAccent) Locale.UK else Locale.US
        speakWithTts(cleanText, speechRateOverride, locale = targetLocale)
    }

    /**
     * Speaks a sentence at slow turtle speed (`0.60x`) so learners can hear word boundaries and linking.
     */
    fun speakSentenceSlow(text: String) {
        speakSentenceWithTts(text, speechRateOverride = SLOW_TURTLE_RATE)
    }

    fun stopAudio() {
        try {
            exoPlayer?.stop()
        } catch (_: Exception) {}
        try {
            tts?.stop()
        } catch (_: Exception) {}
        _currentlyPlayingWord.value = null
        _isPlayingSlow.value = false
    }

    private fun speakWithTts(
        text: String,
        speechRateOverride: Float? = null,
        locale: Locale = Locale.US
    ) {
        val rate = (speechRateOverride ?: defaultSpeechRate).coerceIn(0.5f, 1.5f)
        if (isTtsReady && tts != null) {
            applyBestVoiceAndLocale(locale)
            tts?.setSpeechRate(rate)
            val result = tts?.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                ttsStreamParams,
                "utt_${text.hashCode()}_${locale.country}_${System.nanoTime()}"
            )
            if (result != TextToSpeech.SUCCESS) {
                isTtsReady = false
                try {
                    tts?.shutdown()
                } catch (_: Exception) {}
                tts = null
                pendingTtsText = text
                pendingSpeechRate = rate
                pendingLocale = locale
                ensureTtsInitialized()
            }
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
        _isPlayingSlow.value = false
    }
}
