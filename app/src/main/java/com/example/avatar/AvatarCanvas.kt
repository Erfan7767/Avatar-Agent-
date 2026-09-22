package com.example.avatar

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AvatarGender
import com.example.model.AvatarState
import com.example.model.VisemeFrame
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-fidelity, real-time interactive avatar stage.
 * Dynamically articulates the character in 3D perspective space (head turns, nods, tilts),
 * animated organic breathing, biological micro-blinking, expressive eyebrows,
 * and synchronized photorealistic phoneme lip-sync and jaw movements.
 */
@Composable
fun AvatarCanvas(
    @DrawableRes avatarResId: Int,
    gender: AvatarGender,
    state: AvatarState,
    viseme: VisemeFrame,
    speechAudioAmplitude: Float,
    modifier: Modifier = Modifier,
    isImmersiveMode: Boolean = false
) {
    // 1. Organic Breathing & Ambient Rhythm Animations
    val infiniteTransition = rememberInfiniteTransition(label = "avatar_life_cycle")

    // Slow rhythmic breathing (expands chest/shoulders subtly)
    val breathingCycle by infiniteTransition.animateFloat(
        initialValue = -1.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "organic_breathing"
    )

    // Ambient micro-sway to prevent any static freeze
    val ambientSway by infiniteTransition.animateFloat(
        initialValue = -0.6f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_sway"
    )

    // Dynamic state-responsive glowing aura
    val auraColor = when (state) {
        AvatarState.LISTENING -> Color(0xFF10B981)   // Emerald Green
        AvatarState.THINKING -> Color(0xFFF59E0B)    // Golden Amber
        AvatarState.SPEAKING -> Color(0xFF3B82F6)    // Vibrant Electric Blue
        AvatarState.INTERRUPTED -> Color(0xFFEF4444) // Coral Red
        AvatarState.PAUSED -> Color(0xFF6B7280)      // Slate Gray
        AvatarState.ERROR -> Color(0xFFDC2626)       // Crimson
        else -> Color(0xFF6366F1).copy(alpha = 0.7f) // Idle Royal Indigo
    }

    // Calibrated skin and lip palettes according to character
    val isBlonde = avatarResId == R.drawable.img_avatar_blonde
    val skinTone = when {
        isBlonde -> Color(0xFFE8C8B8)
        gender == AvatarGender.FEMALE -> Color(0xFFD6A48D)
        else -> Color(0xFFB88069)
    }

    val lipColor = when {
        isBlonde -> Color(0xFFC46D74)
        gender == AvatarGender.FEMALE -> Color(0xFFA65057)
        else -> Color(0xFF8B4D4B)
    }

    // 3D Motion Kinematics based on viseme, state, and speech audio
    // Head nods: emphasis when speaking + responsive listening nods
    val nodAngle = (viseme.headNod * 2.8f) + (if (state == AvatarState.SPEAKING) speechAudioAmplitude * 2.2f else 0f)
    // Head tilt: curious tilt when listening/thinking + subtle ambient sway
    val tiltAngle = viseme.headTilt + ambientSway
    // Head yaw (turning left/right): follows gaze naturally
    val yawAngle = viseme.gazeX * 4.5f

    // Total vertical bounce combining breathing and speech emphasis
    val verticalBounce = (breathingCycle * 2.2f) + (viseme.headNod * 4.0f) + (speechAudioAmplitude * 3.5f)
    val breathingScale = 1.0f + (breathingCycle * 0.008f) + (speechAudioAmplitude * 0.015f)

    Box(
        modifier = modifier
            .testTag("avatar_container")
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 1. Reactive Ambient Aura behind the avatar
        val haloSize = if (isImmersiveMode) 480.dp else 360.dp
        Box(
            modifier = Modifier
                .size(haloSize)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            auraColor.copy(alpha = 0.40f * (0.7f + speechAudioAmplitude * 0.5f)),
                            auraColor.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )

        // 2. Character Canvas Frame with Full 3D Perspective Transformation
        val cardSize = if (isImmersiveMode) 420.dp else 340.dp
        val cornerRadius = if (isImmersiveMode) 28.dp else 36.dp

        Box(
            modifier = Modifier
                .size(cardSize)
                .graphicsLayer {
                    // Full 3D perspective rotation (Pitch, Yaw, Roll)
                    rotationX = -nodAngle
                    rotationY = yawAngle
                    rotationZ = tiltAngle
                    cameraDistance = 14f * density

                    // Organic breathing & speech expansion
                    scaleX = breathingScale
                    scaleY = breathingScale
                    translationY = verticalBounce
                }
                .clip(RoundedCornerShape(cornerRadius))
                .border(
                    width = if (state == AvatarState.SPEAKING || state == AvatarState.LISTENING) 2.5.dp else 1.5.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            auraColor.copy(alpha = 0.85f),
                            Color(0xFF1E293B).copy(alpha = 0.5f)
                        )
                    ),
                    shape = RoundedCornerShape(cornerRadius)
                )
                .background(Color(0xFF0F172A)),
            contentAlignment = Alignment.Center
        ) {
            // Base Photographic Human Portrait
            Image(
                painter = painterResource(id = avatarResId),
                contentDescription = "Real-Time AI Avatar",
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("avatar_portrait_image"),
                contentScale = ContentScale.Crop
            )

            // Dynamic Real-Time Articulation & Facial Animation Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("avatar_animation_canvas")
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                // --- 1. EYE REGION: Specular Gaze Tracking & Eyelid Micro-Blinking ---
                val leftEyeCenter = Offset(canvasWidth * 0.41f, canvasHeight * 0.41f)
                val rightEyeCenter = Offset(canvasWidth * 0.59f, canvasHeight * 0.41f)
                val eyeRadiusX = canvasWidth * 0.054f
                val eyeRadiusY = canvasHeight * 0.027f

                val gazeOffset = Offset(
                    x = viseme.gazeX * (eyeRadiusX * 0.35f),
                    y = viseme.gazeY * (eyeRadiusY * 0.35f)
                )

                // Living specular iris highlights that follow gaze
                if (viseme.eyelidOpen > 0.25f) {
                    val highlightLeft = leftEyeCenter + gazeOffset + Offset(-2.5f, -2.5f)
                    val highlightRight = rightEyeCenter + gazeOffset + Offset(-2.5f, -2.5f)
                    val glintAlpha = 0.55f * viseme.eyelidOpen

                    drawCircle(
                        color = Color.White.copy(alpha = glintAlpha),
                        radius = 3.0f,
                        center = highlightLeft
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = glintAlpha),
                        radius = 3.0f,
                        center = highlightRight
                    )
                    // Secondary soft glint
                    drawCircle(
                        color = Color(0xFFBAE6FD).copy(alpha = glintAlpha * 0.6f),
                        radius = 1.8f,
                        center = highlightLeft + Offset(3.5f, 2.5f)
                    )
                    drawCircle(
                        color = Color(0xFFBAE6FD).copy(alpha = glintAlpha * 0.6f),
                        radius = 1.8f,
                        center = highlightRight + Offset(3.5f, 2.5f)
                    )
                }

                // Biological Micro-Blinking Eyelid Drop
                if (viseme.eyelidOpen < 0.96f) {
                    val eyelidDrop = (1.0f - viseme.eyelidOpen)
                    val eyelidFill = skinTone.copy(alpha = 0.98f)
                    val creaseTone = Color(0xFF3E231D).copy(alpha = 0.5f * eyelidDrop)

                    // Left Upper Eyelid
                    val leftEyelidPath = Path().apply {
                        val topY = leftEyeCenter.y - eyeRadiusY * 1.35f
                        val bottomY = leftEyeCenter.y - eyeRadiusY + (eyeRadiusY * 2.3f * eyelidDrop)
                        moveTo(leftEyeCenter.x - eyeRadiusX * 1.15f, leftEyeCenter.y)
                        quadraticTo(
                            leftEyeCenter.x,
                            bottomY,
                            leftEyeCenter.x + eyeRadiusX * 1.15f,
                            leftEyeCenter.y
                        )
                        lineTo(leftEyeCenter.x + eyeRadiusX * 1.15f, topY)
                        lineTo(leftEyeCenter.x - eyeRadiusX * 1.15f, topY)
                        close()
                    }
                    drawPath(leftEyelidPath, color = eyelidFill)
                    // Eyelash contour & crease
                    drawPath(
                        leftEyelidPath,
                        color = Color(0xFF1E110E).copy(alpha = 0.85f * eyelidDrop),
                        style = Stroke(width = 2.2f)
                    )

                    // Right Upper Eyelid
                    val rightEyelidPath = Path().apply {
                        val topY = rightEyeCenter.y - eyeRadiusY * 1.35f
                        val bottomY = rightEyeCenter.y - eyeRadiusY + (eyeRadiusY * 2.3f * eyelidDrop)
                        moveTo(rightEyeCenter.x - eyeRadiusX * 1.15f, rightEyeCenter.y)
                        quadraticTo(
                            rightEyeCenter.x,
                            bottomY,
                            rightEyeCenter.x + eyeRadiusX * 1.15f,
                            rightEyeCenter.y
                        )
                        lineTo(rightEyeCenter.x + eyeRadiusX * 1.15f, topY)
                        lineTo(rightEyeCenter.x - eyeRadiusX * 1.15f, topY)
                        close()
                    }
                    drawPath(rightEyelidPath, color = eyelidFill)
                    drawPath(
                        rightEyelidPath,
                        color = Color(0xFF1E110E).copy(alpha = 0.85f * eyelidDrop),
                        style = Stroke(width = 2.2f)
                    )
                }

                // --- 2. EXPRESSIVE EYEBROWS ---
                // Eyebrows subtly raise when listening attentively or emphasizing speech
                val browLift = when (state) {
                    AvatarState.LISTENING -> -3.5f
                    AvatarState.SPEAKING -> -(speechAudioAmplitude * 4.5f)
                    AvatarState.THINKING -> -1.5f
                    else -> 0f
                }

                val leftBrowY = (canvasHeight * 0.355f) + browLift
                val rightBrowY = (canvasHeight * 0.355f) + browLift + (if (state == AvatarState.THINKING) -2f else 0f)

                val browColor = Color(0xFF2C1914).copy(alpha = 0.22f)
                val leftBrowPath = Path().apply {
                    moveTo(canvasWidth * 0.35f, leftBrowY + 1f)
                    quadraticTo(canvasWidth * 0.41f, leftBrowY - 2.5f, canvasWidth * 0.47f, leftBrowY + 0.5f)
                }
                drawPath(leftBrowPath, color = browColor, style = Stroke(width = 3.0f))

                val rightBrowPath = Path().apply {
                    moveTo(canvasWidth * 0.53f, rightBrowY + 0.5f)
                    quadraticTo(canvasWidth * 0.59f, rightBrowY - 2.5f, canvasWidth * 0.65f, rightBrowY + 1f)
                }
                drawPath(rightBrowPath, color = browColor, style = Stroke(width = 3.0f))

                // --- 3. MOUTH, ORAL CAVITY & PHONEME LIP-SYNC RIG ---
                val mouthCenterX = canvasWidth * 0.50f
                val mouthCenterY = canvasHeight * 0.648f

                val baseHalfWidth = canvasWidth * 0.086f
                val smileSpread = viseme.smile * (canvasWidth * 0.016f)
                val spreadMod = (viseme.mouthSpread - 0.5f) * (canvasWidth * 0.020f)
                val mouthHalfWidth = (baseHalfWidth + smileSpread + spreadMod) * (if (viseme.mouthO > 0.4f) 0.82f else 1.0f)

                // Effective jaw opening (driven by speech amplitude and viseme phonemes)
                val jawOpening = (viseme.jawOpen * (canvasHeight * 0.052f)) + (speechAudioAmplitude * canvasHeight * 0.012f)
                val smileLift = (viseme.smile - 0.2f) * (canvasHeight * 0.012f)

                val leftCorner = Offset(mouthCenterX - mouthHalfWidth, mouthCenterY - smileLift)
                val rightCorner = Offset(mouthCenterX + mouthHalfWidth, mouthCenterY - smileLift)

                // If mouth is actively speaking or open
                if (jawOpening > 2.0f) {
                    // Soft skin-toned feathering zone that blends cleanly over the photo's original mouth
                    val patchRadiusX = mouthHalfWidth * 1.35f
                    val patchRadiusY = (jawOpening * 1.5f).coerceAtLeast(14f)
                    drawOval(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                skinTone.copy(alpha = 0.95f),
                                skinTone.copy(alpha = 0.70f),
                                Color.Transparent
                            ),
                            center = Offset(mouthCenterX, mouthCenterY + (jawOpening * 0.4f)),
                            radius = patchRadiusX
                        ),
                        topLeft = Offset(mouthCenterX - patchRadiusX, mouthCenterY - patchRadiusY * 0.5f),
                        size = Size(patchRadiusX * 2f, patchRadiusY * 2f)
                    )

                    // 1. Dark Inner Oral Cavity
                    val oralCavityPath = Path().apply {
                        moveTo(leftCorner.x, leftCorner.y)
                        quadraticTo(
                            mouthCenterX,
                            mouthCenterY - (jawOpening * 0.22f),
                            rightCorner.x,
                            rightCorner.y
                        )
                        quadraticTo(
                            mouthCenterX,
                            mouthCenterY + jawOpening,
                            leftCorner.x,
                            leftCorner.y
                        )
                        close()
                    }
                    drawPath(
                        oralCavityPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF140608), Color(0xFF280B0F), Color(0xFF381016)),
                            startY = mouthCenterY - (jawOpening * 0.3f),
                            endY = mouthCenterY + jawOpening
                        )
                    )

                    // 2. Visible Upper Teeth Row
                    if (jawOpening > 3.5f) {
                        val teethWidth = mouthHalfWidth * 0.68f
                        val teethHeight = (jawOpening * 0.38f).coerceIn(3.0f, 7.5f)
                        val teethPath = Path().apply {
                            moveTo(mouthCenterX - teethWidth, mouthCenterY - (jawOpening * 0.16f))
                            quadraticTo(
                                mouthCenterX,
                                mouthCenterY - (jawOpening * 0.14f) + teethHeight,
                                mouthCenterX + teethWidth,
                                mouthCenterY - (jawOpening * 0.16f)
                            )
                            lineTo(mouthCenterX + teethWidth, mouthCenterY - (jawOpening * 0.22f))
                            lineTo(mouthCenterX - teethWidth, mouthCenterY - (jawOpening * 0.22f))
                            close()
                        }
                        drawPath(teethPath, color = Color(0xFFF6F4EE).copy(alpha = 0.92f))
                    }

                    // 3. Realistic Lower Lip (drops anatomically with jaw)
                    val lowerLipThickness = 4.2f + (jawOpening * 0.22f)
                    val lowerLipPath = Path().apply {
                        moveTo(leftCorner.x, leftCorner.y)
                        quadraticTo(
                            mouthCenterX,
                            mouthCenterY + jawOpening + lowerLipThickness,
                            rightCorner.x,
                            rightCorner.y
                        )
                        quadraticTo(
                            mouthCenterX,
                            mouthCenterY + jawOpening,
                            leftCorner.x,
                            leftCorner.y
                        )
                        close()
                    }
                    drawPath(
                        lowerLipPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(lipColor.copy(alpha = 0.92f), lipColor.copy(alpha = 0.75f)),
                            startY = mouthCenterY + jawOpening,
                            endY = mouthCenterY + jawOpening + lowerLipThickness
                        )
                    )

                    // 4. Realistic Upper Lip with Cupid's bow curve
                    val upperLipThickness = 3.6f
                    val upperLipPath = Path().apply {
                        moveTo(leftCorner.x, leftCorner.y)
                        quadraticTo(
                            mouthCenterX - (mouthHalfWidth * 0.25f),
                            mouthCenterY - (jawOpening * 0.25f) - upperLipThickness,
                            mouthCenterX,
                            mouthCenterY - (jawOpening * 0.20f) - (upperLipThickness * 0.65f)
                        )
                        quadraticTo(
                            mouthCenterX + (mouthHalfWidth * 0.25f),
                            mouthCenterY - (jawOpening * 0.25f) - upperLipThickness,
                            rightCorner.x,
                            rightCorner.y
                        )
                        quadraticTo(
                            mouthCenterX,
                            mouthCenterY - (jawOpening * 0.22f),
                            leftCorner.x,
                            leftCorner.y
                        )
                        close()
                    }
                    drawPath(upperLipPath, color = lipColor.copy(alpha = 0.92f))

                    // Dynamic chin depression shadow indicating real bone jaw movement
                    val chinShadowPath = Path().apply {
                        moveTo(mouthCenterX - mouthHalfWidth * 0.8f, mouthCenterY + jawOpening + lowerLipThickness + 3f)
                        quadraticTo(
                            mouthCenterX,
                            mouthCenterY + jawOpening + lowerLipThickness + 7f,
                            mouthCenterX + mouthHalfWidth * 0.8f,
                            mouthCenterY + jawOpening + lowerLipThickness + 3f
                        )
                    }
                    drawPath(
                        chinShadowPath,
                        color = Color(0xFF2D1612).copy(alpha = 0.28f),
                        style = Stroke(width = 2.0f)
                    )
                } else {
                    // Resting closed mouth with natural smile curvature
                    val restingLipPath = Path().apply {
                        moveTo(leftCorner.x, leftCorner.y)
                        quadraticTo(
                            mouthCenterX,
                            mouthCenterY + (viseme.smile * 2.8f),
                            rightCorner.x,
                            rightCorner.y
                        )
                    }
                    drawPath(
                        restingLipPath,
                        color = Color(0xFF4C2224).copy(alpha = 0.65f),
                        style = Stroke(width = 2.0f)
                    )
                }

                // --- 4. SPEECH VOCAL RESONANCE RIPPLES ---
                if (state == AvatarState.SPEAKING && speechAudioAmplitude > 0.06f) {
                    val rippleRadius = (canvasWidth * 0.38f) * (0.85f + speechAudioAmplitude * 0.25f)
                    drawCircle(
                        color = Color(0xFF60A5FA).copy(alpha = 0.25f * speechAudioAmplitude),
                        radius = rippleRadius,
                        center = Offset(mouthCenterX, mouthCenterY),
                        style = Stroke(width = 2.2f)
                    )
                    drawCircle(
                        color = Color(0xFF93C5FD).copy(alpha = 0.15f * speechAudioAmplitude),
                        radius = rippleRadius * 1.18f,
                        center = Offset(mouthCenterX, mouthCenterY),
                        style = Stroke(width = 1.4f)
                    )
                }
            }

            // Cinematic subtle gradient vignette along bottom border
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .size(70.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xDD0B0F19))
                        )
                    )
            )
        }
    }
}
