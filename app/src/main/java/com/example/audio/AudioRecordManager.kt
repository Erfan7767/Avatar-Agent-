package com.example.audio

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.log10

/**
 * Monitors live microphone amplitude for Voice Activity Detection (VAD),
 * live wave visualization, and barge-in triggers.
 */
class AudioRecordManager(private val context: Context) {

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var isMonitoring: Boolean = false

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(2048)

    fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Starts monitoring microphone level and invokes onAmplitude(0f..1f)
     * and onVoiceDetected(Boolean) when voice exceeds threshold.
     */
    fun startMonitoring(
        scope: CoroutineScope,
        sensitivityThreshold: Float = 0.12f,
        onAmplitude: (Float) -> Unit,
        onVoiceDetected: (Boolean) -> Unit
    ) {
        if (!hasRecordPermission()) return
        if (isMonitoring) return

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = null
                return
            }

            audioRecord?.startRecording()
            isMonitoring = true

            recordingJob = scope.launch(Dispatchers.Default) {
                val buffer = ShortArray(bufferSize)
                while (isActive && isMonitoring) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        var sum = 0.0
                        for (i in 0 until read) {
                            sum += abs(buffer[i].toDouble())
                        }
                        val avg = sum / read
                        // Normalize 0.0 to 1.0
                        val normalized = (avg / 8000.0).toFloat().coerceIn(0f, 1f)
                        onAmplitude(normalized)

                        if (normalized > sensitivityThreshold) {
                            onVoiceDetected(true)
                        }
                    }
                    delay(40) // ~25 fps update rate
                }
            }
        } catch (e: Exception) {
            isMonitoring = false
            audioRecord?.release()
            audioRecord = null
        }
    }

    fun stopMonitoring() {
        isMonitoring = false
        recordingJob?.cancel()
        recordingJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            // Ignore clean teardown error
        } finally {
            audioRecord = null
        }
    }
}
