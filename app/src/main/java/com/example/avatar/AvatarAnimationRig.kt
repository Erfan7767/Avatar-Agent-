package com.example.avatar

import com.example.model.AvatarState
import com.example.model.FacialExpression
import com.example.model.VisemeFrame
import kotlin.math.cos
import kotlin.math.sin

/**
 * Procedural animation rig calculating natural real-time facial parameters:
 * - Viseme mouth shapes (jawOpen, mouthSpread, mouthO)
 * - Micro-blinking (natural biological eyelid closure)
 * - Eye contact and saccadic gaze tracking
 * - Idle breathing and head sway/nods
 * - Contextual facial expressions
 */
class AvatarAnimationRig {

    private var lastBlinkTimeMs: Long = 0
    private var blinkIntervalMs: Long = 3800
    private var isBlinking: Boolean = false
    private var blinkProgress: Float = 0f

    // Gaze saccade state
    private var currentGazeX: Float = 0f
    private var currentGazeY: Float = 0f
    private var targetGazeX: Float = 0f
    private var targetGazeY: Float = 0f
    private var lastSaccadeTimeMs: Long = 0

    // Smooth mouth interpolation
    private var currentJawOpen: Float = 0f
    private var currentMouthSpread: Float = 0f
    private var currentMouthO: Float = 0f
    private var currentSmile: Float = 0.25f

    /**
     * Updates and computes the next VisemeFrame based on elapsed time, avatar state,
     * current speech amplitude (0f to 1f), and context facial expression.
     */
    fun updateFrame(
        currentTimeMs: Long,
        state: AvatarState,
        speechAudioAmplitude: Float, // 0f to 1f
        contextExpression: FacialExpression,
        animationLevelFactor: Float = 1.0f // 0.7f subtle, 1.0f balanced, 1.3f dynamic
    ): VisemeFrame {
        // 1. Natural Eyelid Blink Engine
        if (currentTimeMs - lastBlinkTimeMs > blinkIntervalMs && !isBlinking) {
            isBlinking = true
            lastBlinkTimeMs = currentTimeMs
            // Deterministic interval variation based on time
            blinkIntervalMs = 3200L + ((currentTimeMs % 1500L))
        }

        var eyelidOpen = 1.0f
        if (isBlinking) {
            val blinkElapsed = currentTimeMs - lastBlinkTimeMs
            val blinkDuration = 140L // typical human blink 120-150ms
            if (blinkElapsed < blinkDuration) {
                // Symmetrical smooth blink curve (0 to 1 to 0)
                val half = blinkDuration / 2f
                val t = if (blinkElapsed < half) {
                    blinkElapsed / half
                } else {
                    (blinkDuration - blinkElapsed) / half
                }
                // Eyelid drops to 0.05 at peak
                eyelidOpen = (1f - (t * 0.95f)).coerceIn(0.05f, 1.0f)
            } else {
                isBlinking = false
                eyelidOpen = 1.0f
            }
        }

        // 2. Gaze Controller (Natural Eye Contact & Saccades)
        if (currentTimeMs - lastSaccadeTimeMs > 2400L) {
            lastSaccadeTimeMs = currentTimeMs
            when (state) {
                AvatarState.LISTENING -> {
                    // Attentive forward gaze with slight micro-shift
                    targetGazeX = (((currentTimeMs % 200) - 100) / 1000f) * 0.08f
                    targetGazeY = (((currentTimeMs % 300) - 150) / 1000f) * 0.06f
                }
                AvatarState.THINKING -> {
                    // Looking slightly upward or to the side while thinking
                    targetGazeX = 0.15f * if ((currentTimeMs / 1000) % 2L == 0L) 1f else -1f
                    targetGazeY = -0.18f
                }
                AvatarState.SPEAKING -> {
                    // Confident communicative eye contact with micro-shifts
                    targetGazeX = (((currentTimeMs % 400) - 200) / 1000f) * 0.12f
                    targetGazeY = (((currentTimeMs % 500) - 250) / 1000f) * 0.08f
                }
                else -> {
                    targetGazeX = (((currentTimeMs % 500) - 250) / 1000f) * 0.15f
                    targetGazeY = (((currentTimeMs % 400) - 200) / 1000f) * 0.10f
                }
            }
        }
        // Smooth interpolation towards target gaze
        currentGazeX += (targetGazeX - currentGazeX) * 0.12f
        currentGazeY += (targetGazeY - currentGazeY) * 0.12f

        // 3. Natural Breathing & Head Motion
        val breathTime = currentTimeMs / 1000.0
        // Breathing frequency: ~0.25 Hz (1 breath every 4 seconds)
        val breathFactor = sin(breathTime * 1.5).toFloat() * 0.02f * animationLevelFactor

        // Subtle head tilt & nod based on state and elapsed time
        var headTilt = (sin(breathTime * 0.7).toFloat() * 1.2f) * animationLevelFactor
        var headNod = (cos(breathTime * 0.9).toFloat() * 0.8f + breathFactor * 10f) * animationLevelFactor

        when (state) {
            AvatarState.LISTENING -> {
                // Attentive gentle nodding to affirm listening
                val nodOscillation = (sin(breathTime * 3.2).toFloat() * 1.5f).coerceAtLeast(0f)
                headNod += nodOscillation * animationLevelFactor
                headTilt += 0.8f // slight attentive head cock
            }
            AvatarState.THINKING -> {
                headTilt += 2.2f * animationLevelFactor
                headNod -= 0.6f
            }
            AvatarState.SPEAKING -> {
                // Conversational head emphasis synchronized with speech envelope
                val speechNod = (speechAudioAmplitude * 1.8f)
                headNod += speechNod * animationLevelFactor
                headTilt += (sin(breathTime * 2.1).toFloat() * 1.0f) * animationLevelFactor
            }
            AvatarState.INTERRUPTED -> {
                headTilt = -1.2f
                headNod = -0.5f
            }
            else -> {}
        }

        // 4. Lip Sync & Mouth Shapes
        var targetJawOpen = 0f
        var targetMouthSpread = 0.2f
        var targetMouthO = 0f

        if (state == AvatarState.SPEAKING && speechAudioAmplitude > 0.02f) {
            // Audio-driven mouth opening with phoneme modulation (vowel/consonant cycle)
            val phonemeMod = (sin(currentTimeMs / 65.0).toFloat() * 0.5f + 0.5f)
            val vowelMix = (cos(currentTimeMs / 95.0).toFloat() * 0.5f + 0.5f)

            // Dynamic jaw opening scaled by speech amplitude
            targetJawOpen = (speechAudioAmplitude * 0.85f * (0.4f + 0.6f * phonemeMod)).coerceIn(0.1f, 0.85f)
            targetMouthSpread = (0.3f + 0.5f * vowelMix).coerceIn(0.1f, 0.9f)
            targetMouthO = (if (vowelMix > 0.65f) 0.6f else 0.05f) * speechAudioAmplitude
        } else {
            // Closed/resting mouth
            targetJawOpen = 0f
            targetMouthSpread = 0.15f
            targetMouthO = 0f
        }

        // Smooth mouth interpolation (snappy response for phoneme sync)
        currentJawOpen += (targetJawOpen - currentJawOpen) * 0.48f
        currentMouthSpread += (targetMouthSpread - currentMouthSpread) * 0.38f
        currentMouthO += (targetMouthO - currentMouthO) * 0.38f

        // 5. Facial Expression Smile Modulation
        val targetSmile = when (contextExpression) {
            FacialExpression.WARM_SMILE -> 0.75f
            FacialExpression.GENTLE_SMILE -> 0.45f
            FacialExpression.THOUGHTFUL_ATTENTIVE -> 0.15f
            FacialExpression.SERIOUS_FOCUSED -> 0.05f
            FacialExpression.LISTENING_NOD -> 0.35f
            FacialExpression.NEUTRAL -> 0.20f
        }
        currentSmile += (targetSmile - currentSmile) * 0.1f

        return VisemeFrame(
            jawOpen = currentJawOpen.coerceIn(0f, 1f),
            mouthSpread = currentMouthSpread.coerceIn(0f, 1f),
            mouthO = currentMouthO.coerceIn(0f, 1f),
            eyelidOpen = eyelidOpen.coerceIn(0f, 1f),
            gazeX = currentGazeX.coerceIn(-1f, 1f),
            gazeY = currentGazeY.coerceIn(-1f, 1f),
            headTilt = headTilt,
            headNod = headNod,
            smile = currentSmile,
            expression = contextExpression
        )
    }

    fun reset() {
        currentJawOpen = 0f
        currentMouthSpread = 0f
        currentMouthO = 0f
        currentSmile = 0.25f
        isBlinking = false
    }
}
