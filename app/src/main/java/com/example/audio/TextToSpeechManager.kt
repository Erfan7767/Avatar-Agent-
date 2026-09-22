package com.example.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.model.AvatarGender
import com.example.model.LanguageMode
import com.example.model.VoiceProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.util.Locale

/**
 * A streamed chunk of synthesized audio data with timing and viseme metadata.
 */
data class TtsAudioChunk(
    val utteranceId: String,
    val audioData: ByteArray,
    val sampleRate: Int = 24000,
    val isLastChunk: Boolean = false,
    val currentWord: String? = null,
    val sequenceNumber: Int = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TtsAudioChunk
        if (utteranceId != other.utteranceId) return false
        if (!audioData.contentEquals(other.audioData)) return false
        if (sampleRate != other.sampleRate) return false
        if (isLastChunk != other.isLastChunk) return false
        if (currentWord != other.currentWord) return false
        if (sequenceNumber != other.sequenceNumber) return false
        return true
    }

    override fun hashCode(): Int {
        var result = utteranceId.hashCode()
        result = 31 * result + audioData.contentHashCode()
        result = 31 * result + sampleRate
        result = 31 * result + isLastChunk.hashCode()
        result = 31 * result + (currentWord?.hashCode() ?: 0)
        result = 31 * result + sequenceNumber
        return result
    }
}

/**
 * Text-to-speech service interface abstracting speech synthesis
 * with full support for low-latency streaming audio data and real-time playback.
 */
interface ITtsProvider {
    /**
     * Initialize the TTS engine asynchronously.
     */
    fun initialize(onReady: (Boolean) -> Unit)

    /**
     * Synthesize and play complete text utterance.
     */
    fun speak(
        utteranceId: String,
        text: String,
        language: LanguageMode,
        voiceProfile: VoiceProfile,
        rateMultiplier: Float,
        pitchMultiplier: Float = 1.0f,
        listener: TtsListener
    )

    /**
     * Stop any ongoing speech synthesis or audio playback immediately.
     */
    fun stop()

    /**
     * Release all internal resources and audio tracks.
     */
    fun shutdown()

    /**
     * Query if the engine is currently synthesizing or speaking.
     */
    fun isSpeaking(): Boolean

    /**
     * Stream synthesized audio data chunks as a cold [Flow].
     * Enables low-latency time-to-first-audio playback before complete utterance synthesis is finished.
     */
    fun synthesizeStream(
        utteranceId: String,
        text: String,
        language: LanguageMode,
        voiceProfile: VoiceProfile,
        rateMultiplier: Float = 1.0f
    ): Flow<TtsAudioChunk> = emptyFlow()

    /**
     * Play an incoming streaming audio chunk directly through low-latency audio pipelines (e.g. AudioTrack).
     */
    fun playAudioStreamChunk(chunk: TtsAudioChunk) {}
}

interface TtsListener {
    fun onStart(utteranceId: String)
    fun onRangeStart(utteranceId: String, start: Int, end: Int)
    fun onDone(utteranceId: String)
    fun onError(utteranceId: String, errorMsg: String)
}

class AndroidTextToSpeechProvider(private val context: Context) : ITtsProvider {

    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false
    private var isInitializing = false
    private var activeListener: TtsListener? = null
    private var pendingRequest: PendingSpeakRequest? = null

    private data class PendingSpeakRequest(
        val utteranceId: String,
        val text: String,
        val language: LanguageMode,
        val voiceProfile: VoiceProfile,
        val rateMultiplier: Float,
        val pitchMultiplier: Float = 1.0f,
        val listener: TtsListener
    )

    override fun initialize(onReady: (Boolean) -> Unit) {
        if (isInitialized) {
            onReady(true)
            return
        }
        isInitializing = true
        try {
            textToSpeech = TextToSpeech(context) { status ->
                isInitializing = false
                if (status == TextToSpeech.SUCCESS) {
                    isInitialized = true
                    textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            utteranceId?.let { activeListener?.onStart(it) }
                        }

                        override fun onDone(utteranceId: String?) {
                            utteranceId?.let { activeListener?.onDone(it) }
                        }

                        override fun onError(utteranceId: String?) {
                            utteranceId?.let { activeListener?.onError(it, "TTS engine synthesis notice") }
                        }

                        override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                            super.onRangeStart(utteranceId, start, end, frame)
                            utteranceId?.let { activeListener?.onRangeStart(it, start, end) }
                        }
                    })
                    onReady(true)

                    // Execute any pending speech that was queued while initializing
                    pendingRequest?.let { req ->
                        pendingRequest = null
                        speak(req.utteranceId, req.text, req.language, req.voiceProfile, req.rateMultiplier, req.pitchMultiplier, req.listener)
                    }
                } else {
                    isInitialized = false
                    onReady(false)
                    pendingRequest?.let { req ->
                        pendingRequest = null
                        req.listener.onError(req.utteranceId, "TextToSpeech engine initialization returned error status $status")
                    }
                }
            }
        } catch (e: Exception) {
            isInitializing = false
            isInitialized = false
            onReady(false)
        }
    }

    override fun speak(
        utteranceId: String,
        text: String,
        language: LanguageMode,
        voiceProfile: VoiceProfile,
        rateMultiplier: Float,
        pitchMultiplier: Float,
        listener: TtsListener
    ) {
        if (isInitializing) {
            // Queue request until initialization completes
            pendingRequest = PendingSpeakRequest(utteranceId, text, language, voiceProfile, rateMultiplier, pitchMultiplier, listener)
            return
        }

        if (!isInitialized || textToSpeech == null) {
            // Attempt re-initialization
            pendingRequest = PendingSpeakRequest(utteranceId, text, language, voiceProfile, rateMultiplier, pitchMultiplier, listener)
            initialize { success ->
                if (!success) {
                    listener.onError(utteranceId, "TextToSpeech engine not available")
                }
            }
            return
        }

        activeListener = listener

        try {
            val locale = if (language == LanguageMode.ARABIC) {
                Locale.forLanguageTag("ar")
            } else {
                Locale.US
            }

            val result = textToSpeech?.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                textToSpeech?.setLanguage(Locale.getDefault())
            }

            // Pick the highest quality female voice from the engine if available
            try {
                val availableVoices = textToSpeech?.voices
                if (!availableVoices.isNullOrEmpty()) {
                    val matchingVoice = availableVoices.find { v ->
                        val isLangMatch = v.locale.language.equals(locale.language, ignoreCase = true)
                        val name = v.name.lowercase()
                        if (voiceProfile.gender == AvatarGender.FEMALE) {
                            isLangMatch && (name.contains("female") || name.contains("fem") || name.contains("woman") || name.contains("#female") || name.contains("ar-xa-x-ard-local") || name.contains("ar-xa-x-arc-local") || name.contains("en-us-x-tpf-local") || name.contains("en-us-x-sfg-local"))
                        } else {
                            isLangMatch && (name.contains("male") || name.contains("#male") || name.contains("man"))
                        }
                    } ?: availableVoices.find { v ->
                        v.locale.language.equals(locale.language, ignoreCase = true)
                    }
                    if (matchingVoice != null) {
                        textToSpeech?.voice = matchingVoice
                    }
                }
            } catch (e: Exception) {
                // Fallback gracefully to default voice selection
            }

            textToSpeech?.setPitch(voiceProfile.pitch * pitchMultiplier)
            textToSpeech?.setSpeechRate(voiceProfile.rate * rateMultiplier)

            val params = Bundle()
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)

            val speakResult = textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            if (speakResult != TextToSpeech.SUCCESS) {
                listener.onError(utteranceId, "TTS speak returned $speakResult")
            }
        } catch (e: Exception) {
            listener.onError(utteranceId, e.message ?: "TTS speak exception")
        }
    }

    override fun stop() {
        try {
            textToSpeech?.stop()
        } catch (e: Exception) {
            // Ignore
        }
    }

    override fun isSpeaking(): Boolean {
        return textToSpeech?.isSpeaking ?: false
    }

    override fun shutdown() {
        stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        isInitialized = false
    }
}
