package com.example.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.example.model.LanguageMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.util.Locale

/**
 * Audio format configuration for streaming speech recognition.
 */
data class AudioStreamConfig(
    val sampleRate: Int = 16000,
    val channelConfig: Int = 1, // Mono
    val bitsPerSample: Int = 16,
    val bufferSizeBytes: Int = 2048
)

/**
 * Result chunk emitted during real-time streaming recognition.
 */
data class AsrStreamResult(
    val text: String,
    val isFinal: Boolean,
    val confidence: Float = 1.0f,
    val latencyMs: Long = 0L
)

/**
 * Speech recognition service interface abstracting speech-to-text operations
 * with support for both standard recognition and continuous low-latency streaming audio data.
 */
interface IAsrProvider {
    /**
     * Start speech recognition using the default microphone intent or provider input.
     */
    fun startListening(language: LanguageMode, listener: AsrListener)

    /**
     * Stop active speech recognition.
     */
    fun stopListening()

    /**
     * Release all internal resources.
     */
    fun destroy()

    /**
     * Check if the speech recognition service is available.
     */
    fun isAvailable(): Boolean

    /**
     * Start continuous streaming speech recognition designed for low-latency interactions.
     * Takes an optional [AudioStreamConfig] and returns a cold [Flow] of [AsrStreamResult].
     */
    fun startStreamingRecognition(
        language: LanguageMode,
        config: AudioStreamConfig = AudioStreamConfig(),
        listener: AsrListener? = null
    ): Flow<AsrStreamResult> = emptyFlow()

    /**
     * Feed an individual PCM audio chunk to the streaming speech recognition engine.
     * Enables external microphone audio capture (e.g., via AudioRecord / WebRTC)
     * to pipe streaming raw PCM data directly to the recognizer.
     */
    fun feedAudioStreamChunk(
        audioData: ByteArray,
        offset: Int = 0,
        length: Int = audioData.size
    ) {}
}

interface AsrListener {
    fun onReadyForSpeech()
    fun onRmsChanged(rmsdB: Float)
    fun onPartialResults(partialText: String)
    fun onFinalResult(text: String)
    fun onEndOfSpeech()
    fun onError(errorCode: Int, errorMessage: String)
}

class AndroidSpeechRecognizerProvider(private val context: Context) : IAsrProvider {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    override fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    override fun startListening(language: LanguageMode, listener: AsrListener) {
        stopListening()

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                        listener.onReadyForSpeech()
                    }

                    override fun onBeginningOfSpeech() {}

                    override fun onRmsChanged(rmsdB: Float) {
                        listener.onRmsChanged(rmsdB)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        listener.onEndOfSpeech()
                    }

                    override fun onError(error: Int) {
                        isListening = false
                        val errorMsg = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_CLIENT -> "Client-side recognition error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                            SpeechRecognizer.ERROR_NETWORK -> "Network error during speech recognition"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech match detected"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy"
                            SpeechRecognizer.ERROR_SERVER -> "Server error"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input detected"
                            else -> "Speech recognition error code $error"
                        }
                        listener.onError(error, errorMsg)
                    }

                    override fun onResults(results: Bundle?) {
                        isListening = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val fullText = matches?.firstOrNull() ?: ""
                        listener.onFinalResult(fullText)
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull() ?: ""
                        if (partial.isNotBlank()) {
                            listener.onPartialResults(partial)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                val localeTag = if (language == LanguageMode.ARABIC) "ar-SA" else "en-US"
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, localeTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                // Snappy silence detection for natural human-like voice conversation
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 800L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 800L)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            listener.onError(-1, "Unable to initialize speech recognizer: ${e.message}")
        }
    }

    override fun stopListening() {
        if (isListening) {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
            } catch (e: Exception) {
                // Ignore
            } finally {
                isListening = false
            }
        }
    }

    override fun destroy() {
        stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
