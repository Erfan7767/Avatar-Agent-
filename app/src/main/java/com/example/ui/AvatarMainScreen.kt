package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import com.example.ui.permission.MicrophonePermissionDialog
import com.example.ui.permission.rememberMicrophonePermissionState
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.avatar.AvatarCanvas
import com.example.camera.CameraPreviewSurface
import com.example.model.AvatarCharacter
import com.example.model.AvatarGender
import com.example.model.AvatarState
import com.example.model.AvailableCharacters
import com.example.model.LanguageMode
import com.example.model.PersonaStyle
import com.example.model.ToolCallInfo
import com.example.model.VisemeFrame
import com.example.model.VoiceProfile
import com.example.viewmodel.AvatarViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarMainScreen(
    viewModel: AvatarViewModel,
    onBackToSelection: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val avatarState by viewModel.avatarState.collectAsStateWithLifecycle()
    val character by viewModel.selectedCharacter.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val viseme by viewModel.visemeFrame.collectAsStateWithLifecycle()
    val speechAmp by viewModel.speechAudioAmplitude.collectAsStateWithLifecycle()
    val userAmp by viewModel.userAudioAmplitude.collectAsStateWithLifecycle()
    val isMicListening by viewModel.isMicListening.collectAsStateWithLifecycle()
    val currentTranscript by viewModel.currentTranscript.collectAsStateWithLifecycle()
    val lastAvatarResponse by viewModel.lastAvatarResponse.collectAsStateWithLifecycle()
    val conversationTurns by viewModel.conversationTurns.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val isLiveCallActive by viewModel.isLiveCallActive.collectAsStateWithLifecycle()
    val activeToolCall by viewModel.activeToolCall.collectAsStateWithLifecycle()
    val currentSentiment by viewModel.currentSentiment.collectAsStateWithLifecycle()

    val isArabic = settings.languageMode == LanguageMode.ARABIC
    var showTranscriptSheet by remember { mutableStateOf(false) }
    var showToolsSheet by remember { mutableStateOf(false) }
    var showToneSheet by remember { mutableStateOf(false) }
    var isImmersiveVideoCall by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }
    val context = LocalContext.current

    val micPermissionState = rememberMicrophonePermissionState(
        onPermissionGranted = {
            if (isLiveCallActive) {
                viewModel.startLiveCall()
            } else {
                viewModel.startListening()
            }
        }
    )

    MicrophonePermissionDialog(
        isArabic = isArabic,
        permissionState = micPermissionState
    )

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleUserCamera(true)
        }
    }

    val onToggleCamera = {
        if (settings.userCameraEnabled) {
            viewModel.toggleUserCamera(false)
        } else {
            val hasCameraPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

            if (hasCameraPermission) {
                viewModel.toggleUserCamera(true)
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    val onToggleLiveCall = {
        if (isLiveCallActive) {
            viewModel.stopLiveCall()
        } else {
            if (micPermissionState.isGranted) {
                viewModel.startLiveCall()
            } else {
                micPermissionState.requestPermission()
            }
        }
    }

    val onMicClick = {
        if (isLiveCallActive) {
            // In live call mode, clicking the main button toggles listening or interrupts
            if (avatarState == AvatarState.SPEAKING) {
                viewModel.handleBargeInInterruption()
            } else if (isMicListening) {
                viewModel.stopListening()
            } else {
                if (micPermissionState.isGranted) {
                    viewModel.startListening()
                } else {
                    micPermissionState.requestPermission()
                }
            }
        } else {
            if (isMicListening) {
                viewModel.stopListening()
            } else {
                if (micPermissionState.isGranted) {
                    viewModel.startListening()
                } else {
                    micPermissionState.requestPermission()
                }
            }
        }
    }

    // Start session when entering screen
    LaunchedEffect(Unit) {
        viewModel.startSession()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val micPulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isMicListening) 1.15f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_scale"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF070B14),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isArabic) character.nameAr else character.nameEn,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Language Badge (click to toggle)
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    val nextLang = if (isArabic) LanguageMode.ENGLISH else LanguageMode.ARABIC
                                    viewModel.selectLanguage(nextLang)
                                }
                                .testTag("language_badge_toggle"),
                            color = Color(0xFF1E293B)
                        ) {
                            Text(
                                text = if (isArabic) "العربية" else "EN",
                                color = Color(0xFF60A5FA),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.endSession()
                            onBackToSelection()
                        },
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                },
                actions = {
                    // Real-time Hands-Free Voice Call Toggle (Like a real person call)
                    IconButton(
                        onClick = onToggleLiveCall,
                        modifier = Modifier.testTag("live_call_header_button")
                    ) {
                        Icon(
                            imageVector = if (isLiveCallActive) Icons.Default.CallEnd else Icons.Default.Call,
                            contentDescription = if (isLiveCallActive) "End Live Voice Call" else "Start Live Voice Call",
                            tint = if (isLiveCallActive) Color(0xFFEF4444) else Color(0xFF10B981)
                        )
                    }
                    // Real-time Face Camera Preview Toggle
                    IconButton(
                        onClick = onToggleCamera,
                        modifier = Modifier.testTag("camera_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (settings.userCameraEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            contentDescription = if (settings.userCameraEnabled) "Turn off camera" else "Turn on camera",
                            tint = if (settings.userCameraEnabled) Color(0xFF10B981) else Color(0xFF94A3B8)
                        )
                    }
                    // Mute / Unmute
                    IconButton(
                        onClick = { viewModel.toggleMute() },
                        modifier = Modifier.testTag("mute_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (settings.isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Mute",
                            tint = if (settings.isMuted) Color(0xFFEF4444) else Color(0xFF94A3B8)
                        )
                    }
                    // Full-Screen Live Video Mode Toggle
                    IconButton(
                        onClick = {
                            isImmersiveVideoCall = !isImmersiveVideoCall
                            if (isImmersiveVideoCall && !isLiveCallActive && micPermissionState.isGranted) {
                                viewModel.startLiveCall()
                            }
                        },
                        modifier = Modifier.testTag("fullscreen_video_mode_button")
                    ) {
                        Icon(
                            imageVector = if (isImmersiveVideoCall) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = if (isImmersiveVideoCall) "Exit Full-Screen Video" else "Full-Screen Live Video Call",
                            tint = if (isImmersiveVideoCall) Color(0xFF10B981) else Color(0xFF38BDF8)
                        )
                    }
                    // Character, Gender & Voice Tone Customizer Button
                    IconButton(
                        onClick = { showToneSheet = true },
                        modifier = Modifier.testTag("character_tone_header_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Character & Voice Tone",
                            tint = Color(0xFFF59E0B)
                        )
                    }
                    // Dynamic Tools & Local Files Function-Calling Inspector
                    IconButton(
                        onClick = { showToolsSheet = true },
                        modifier = Modifier.testTag("genai_tools_sheet_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Dynamic Tools & Local Files",
                            tint = Color(0xFF38BDF8)
                        )
                    }
                    // Transcript History Sheet Button
                    IconButton(
                        onClick = { showTranscriptSheet = true },
                        modifier = Modifier.testTag("transcript_sheet_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "Transcript",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                    // Settings Button
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("settings_main_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isImmersiveVideoCall) {
                ImmersiveLiveVideoCallView(
                    character = character,
                    avatarState = avatarState,
                    viseme = viseme,
                    speechAmp = speechAmp,
                    userAmp = userAmp,
                    isArabic = isArabic,
                    isMicListening = isMicListening,
                    currentTranscript = currentTranscript,
                    lastAvatarResponse = lastAvatarResponse,
                    activeToolCall = activeToolCall,
                    currentSentiment = currentSentiment,
                    onExitImmersive = { isImmersiveVideoCall = false },
                    onToggleLiveCall = onToggleLiveCall,
                    onMicClick = onMicClick,
                    onBargeIn = { viewModel.handleBargeInInterruption() },
                    onSelectCharacter = { charId -> viewModel.selectCharacter(charId) },
                    onToggleCamera = onToggleCamera,
                    isCameraActive = settings.userCameraEnabled,
                    onOpenToneSheet = { showToneSheet = true },
                    onToggleLanguage = {
                        val nextLang = if (isArabic) LanguageMode.ENGLISH else LanguageMode.ARABIC
                        viewModel.selectLanguage(nextLang)
                    }
                )
            } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // 1. Live State Pill Indicator & Voice Call Mode
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    val stateColor = when (avatarState) {
                        AvatarState.LISTENING -> Color(0xFF10B981)
                        AvatarState.THINKING -> Color(0xFFF59E0B)
                        AvatarState.SPEAKING -> Color(0xFF3B82F6)
                        AvatarState.INTERRUPTED -> Color(0xFFEF4444)
                        else -> Color(0xFF64748B)
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = stateColor.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, stateColor.copy(alpha = 0.5f)),
                        modifier = Modifier.testTag("avatar_state_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(stateColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isArabic) avatarState.labelAr else avatarState.labelEn,
                                color = stateColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Live Voice Call Status Badge / Quick Start Button
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isLiveCallActive) Color(0x3310B981) else Color(0x263B82F6),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isLiveCallActive) Color(0xFF10B981) else Color(0xFF3B82F6).copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .clickable { onToggleLiveCall() }
                            .testTag("live_call_pill_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isLiveCallActive) Icons.Default.CallEnd else Icons.Default.Call,
                                contentDescription = "Live Voice Call",
                                tint = if (isLiveCallActive) Color(0xFF34D399) else Color(0xFF60A5FA),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isLiveCallActive) {
                                    if (isArabic) "مكالمة حية نشطة 🔴" else "Live Call Active 🔴"
                                } else {
                                    if (isArabic) "بدء مكالمة صوتية مباشرة 📞" else "Start Live Voice Call 📞"
                                },
                                color = if (isLiveCallActive) Color(0xFF34D399) else Color(0xFF93C5FD),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Full-Screen Live Video Mode Button
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0x268B5CF6),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.6f)),
                        modifier = Modifier
                            .clickable {
                                isImmersiveVideoCall = true
                                if (!isLiveCallActive && micPermissionState.isGranted) {
                                    viewModel.startLiveCall()
                                }
                            }
                            .testTag("enter_immersive_video_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "Full-Screen Live Video Mode",
                                tint = Color(0xFFA78BFA),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isArabic) "مكالمة فيديو حية 🎥" else "Live Video 🎥",
                                color = Color(0xFFDDD6FE),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Microphone Permission Warning Banner if not granted
                if (!micPermissionState.isGranted) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x33F59E0B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x80F59E0B)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clickable { micPermissionState.requestPermission() }
                            .testTag("mic_permission_warning_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MicOff,
                                    contentDescription = null,
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isArabic) "الميكروفون غير مفعل - انقر لتفعيله والتحدث" else "Mic disabled - tap to enable voice chat",
                                    color = Color(0xFFFEF3C7),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = if (isArabic) "تفعيل 🎙️" else "Enable 🎙️",
                                color = Color(0xFFFBBF24),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Live Voice Call Active Info Banner
                if (isLiveCallActive) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x2E10B981),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x6610B981)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .testTag("live_call_active_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                LiveVoiceWaveform(
                                    amplitude = if (avatarState == AvatarState.SPEAKING) speechAmp else userAmp,
                                    isActive = avatarState == AvatarState.SPEAKING || isMicListening,
                                    color = if (avatarState == AvatarState.SPEAKING) Color(0xFF60A5FA) else Color(0xFF34D399)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when (avatarState) {
                                        AvatarState.SPEAKING -> if (isArabic) "الأفاتار يتحدث (تحدث لمقاطعته)" else "Speaking (speak to interrupt)"
                                        AvatarState.LISTENING -> if (isArabic) "يستمع إليك الآن... تحدث بحرية" else "Listening... speak freely"
                                        AvatarState.THINKING -> if (isArabic) "يفكر في الإجابة..." else "Thinking..."
                                        AvatarState.INTERRUPTED -> if (isArabic) "تمت المقاطعة، أستمع إليك" else "Interrupted, listening"
                                        else -> if (isArabic) "مكالمة مستمرة كشخص حقيقي" else "Hands-free continuous call"
                                    },
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            if (avatarState == AvatarState.SPEAKING) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFEF4444).copy(alpha = 0.2f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                                    modifier = Modifier
                                        .clickable { viewModel.handleBargeInInterruption() }
                                        .testTag("barge_in_button")
                                ) {
                                    Text(
                                        text = if (isArabic) "مقاطعة ✋" else "Interrupt ✋",
                                        color = Color(0xFFFCA5A5),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 1.5 Quick Character (Girl/Boy) & Voice Tone Switcher Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val allChars = AvailableCharacters.allCharacters
                    allChars.forEach { c ->
                        val isCharSelected = c.id == character.id
                        Surface(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { viewModel.selectCharacter(c.id) }
                                .testTag("quick_char_btn_${c.id}"),
                            color = if (isCharSelected) Color(0xFF2563EB) else Color(0xFF1E293B),
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isCharSelected) Color(0xFF60A5FA) else Color(0xFF334155)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = c.avatarResId),
                                    contentDescription = c.nameEn,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isArabic) c.nameAr else c.nameEn,
                                    color = if (isCharSelected) Color.White else Color(0xFFCBD5E1),
                                    fontSize = 12.sp,
                                    fontWeight = if (isCharSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isArabic) {
                                        if (c.gender == AvatarGender.FEMALE) "(بنت)" else "(ولد)"
                                    } else {
                                        if (c.gender == AvatarGender.FEMALE) "F" else "M"
                                    },
                                    color = if (isCharSelected) Color(0xFF93C5FD) else Color(0xFF64748B),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Quick Tone Adjust Button
                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { showToneSheet = true }
                            .testTag("quick_tone_pill_button"),
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Voice Tone",
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isArabic) "النبرة" else "Tone",
                                color = Color(0xFFF59E0B),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 2. Center Stage: Realistic Animated AI Avatar Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    AvatarCanvas(
                        avatarResId = character.avatarResId,
                        gender = character.gender,
                        state = avatarState,
                        viseme = viseme,
                        speechAudioAmplitude = speechAmp,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Live Subtitle Overlay & Active Agent Tool Indicator at bottom of Avatar
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .fillMaxWidth(0.92f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (activeToolCall != null) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                                    .testTag("active_tool_card"),
                                color = Color(0xEB0369A1),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = activeToolCall?.toolIcon ?: "⚡",
                                        fontSize = 18.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${if (isArabic) "استدعاء أداة ذكية" else "Agent Tool Call"}: ${activeToolCall?.toolName}",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${activeToolCall?.queryOrArg} ➔ ${activeToolCall?.resultPreview}",
                                            color = Color(0xFFE0F2FE),
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        if (lastAvatarResponse.isNotBlank() || currentTranscript.isNotBlank()) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("live_subtitle_card"),
                                color = Color(0xD90F172A),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                    // Context-Aware Sentiment / Emotional Tone Badge
                                    if (currentSentiment != null) {
                                        val s = currentSentiment!!
                                        Row(
                                            modifier = Modifier
                                                .padding(bottom = 6.dp)
                                                .testTag("sentiment_indicator_badge"),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                color = Color(0x33EC4899),
                                                shape = RoundedCornerShape(8.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF472B6).copy(alpha = 0.5f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(text = s.category.emoji, fontSize = 12.sp)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = if (isArabic) s.category.labelAr else s.category.labelEn,
                                                        color = Color(0xFFF472B6),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (isArabic) s.emotionalSummaryAr else s.emotionalSummaryEn,
                                                color = Color(0xFF94A3B8),
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }

                                    if (currentTranscript.isNotBlank() && avatarState == AvatarState.LISTENING) {
                                        Text(
                                            text = "${if (isArabic) "أنت" else "You"}: $currentTranscript",
                                            color = Color(0xFF93C5FD),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    } else if (lastAvatarResponse.isNotBlank()) {
                                        Text(
                                            text = "${if (isArabic) character.nameAr else character.nameEn}: $lastAvatarResponse",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Normal,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Floating Barge-in Affordance Button when avatar is speaking
                    if (avatarState == AvatarState.SPEAKING) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { viewModel.handleBargeInInterruption() }
                                .testTag("barge_in_button"),
                            color = Color(0xCCEF4444),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PanTool,
                                    contentDescription = "Interrupt",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isArabic) "مقاطعة" else "Interrupt",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Real-time CameraX Face Preview Surface for real-time interaction
                    if (settings.userCameraEnabled) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                        ) {
                            CameraPreviewSurface(
                                modifier = Modifier.size(width = 125.dp, height = 165.dp),
                                isArabic = isArabic,
                                onClose = { viewModel.toggleUserCamera(false) },
                                onFaceInteractionUpdate = { _, _ -> }
                            )
                        }
                    }
                }

                // 3. Error / Info Banner if present
                if (errorMessage != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { viewModel.clearError() }
                            .testTag("error_banner"),
                        color = Color(0xFF7F1D1D).copy(alpha = 0.9f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFFCA5A5),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage ?: "",
                                color = Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = Color(0xFFFCA5A5),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // 4. Quick Suggestion Chips (with Agentic Tool Calling)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val suggestions = if (isArabic) {
                        listOf(
                            "🌐 ابحث في جوجل عن تقنيات الذكاء الاصطناعي 2026",
                            "📁 اقرأ ملف استراتيجية المشروع",
                            "📋 اعرض قائمة الملفات المحلية",
                            "✍️ اكتب ملف ملخص التقرير",
                            "🕒 ما الوقت والسياق الحالي؟",
                            "ما رأيك واستشارتك كخبير عالمي؟"
                        )
                    } else {
                        listOf(
                            "🌐 Google Search AI Tech 2026",
                            "📁 Read local project strategy doc",
                            "📋 List local files",
                            "✍️ Write meeting summary file",
                            "🕒 What is live context & time?",
                            "Your global expert insight?"
                        )
                    }

                    suggestions.forEach { prompt ->
                        FilterChip(
                            selected = false,
                            onClick = { viewModel.processUserSpeech(prompt) },
                            label = { Text(prompt, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier.testTag("suggestion_chip_${prompt.hashCode()}")
                        )
                    }
                }

                // 5. Interactive Conversation Controls: Live Voice Call Mode / Standard Controls
                if (isLiveCallActive) {
                    // Dedicated Hands-Free Live Call Control Bar
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = Color(0xFF0F172A),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp, top = 6.dp)
                            .testTag("live_call_control_panel")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // End Call Button (Red)
                            Surface(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .clickable { viewModel.stopLiveCall() }
                                    .testTag("end_live_call_button"),
                                shape = CircleShape,
                                color = Color(0xFFEF4444),
                                shadowElevation = 4.dp
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CallEnd,
                                        contentDescription = "End Call",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            // Center Live Waveform & Status
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                            ) {
                                LiveVoiceWaveform(
                                    amplitude = if (avatarState == AvatarState.SPEAKING) speechAmp else userAmp,
                                    isActive = avatarState == AvatarState.SPEAKING || isMicListening,
                                    color = if (avatarState == AvatarState.SPEAKING) Color(0xFF60A5FA) else Color(0xFF10B981),
                                    barCount = 14
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (avatarState == AvatarState.SPEAKING) {
                                        if (isArabic) "الأفاتار يتحدث... (انقر للمقاطعة)" else "Avatar speaking... (tap to interrupt)"
                                    } else if (isMicListening) {
                                        if (isArabic) "الميكروفون مفتوح... تحدث الآن" else "Mic open... speak now"
                                    } else {
                                        if (isArabic) "مكالمة نشطة كإنسان حقيقي" else "Active human-like call"
                                    },
                                    color = if (avatarState == AvatarState.SPEAKING) Color(0xFF93C5FD) else Color(0xFF6EE7B7),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Dynamic Action: Mic toggle or Interrupt (Barge-In)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(52.dp)
                            ) {
                                if (isMicListening) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .scale(micPulseScale + (userAmp * 0.35f))
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981).copy(alpha = 0.3f))
                                    )
                                }

                                Surface(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .clickable { onMicClick() }
                                        .testTag("live_call_action_button"),
                                    shape = CircleShape,
                                    color = if (avatarState == AvatarState.SPEAKING) Color(0xFFF59E0B) else if (isMicListening) Color(0xFF10B981) else Color(0xFF2563EB),
                                    shadowElevation = 4.dp
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (avatarState == AvatarState.SPEAKING) Icons.Default.PanTool else if (isMicListening) Icons.Default.Mic else Icons.Default.Mic,
                                            contentDescription = if (avatarState == AvatarState.SPEAKING) "Interrupt" else "Mic",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Standard Input Controls: Text Field + Live Call Quick Button + Mic Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp, top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = {
                                Text(
                                    text = if (isArabic) "تحدث بالصوت أو اكتب هنا..." else "Speak or type here...",
                                    color = Color(0xFF64748B),
                                    fontSize = 13.sp
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("text_input_field"),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1E293B),
                                unfocusedContainerColor = Color(0xFF0F172A),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = Color(0xFF334155)
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (textInput.isNotBlank()) {
                                        viewModel.processUserSpeech(textInput.trim())
                                        textInput = ""
                                    }
                                }
                            ),
                            trailingIcon = {
                                if (textInput.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            viewModel.processUserSpeech(textInput.trim())
                                            textInput = ""
                                        },
                                        modifier = Modifier.testTag("send_message_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Send",
                                            tint = Color(0xFF3B82F6)
                                        )
                                    }
                                }
                            }
                        )

                        // Start Live Call Button (Phone icon)
                        Surface(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable { onToggleLiveCall() }
                                .testTag("start_live_call_bottom_button"),
                            shape = CircleShape,
                            color = Color(0xFF059669),
                            shadowElevation = 4.dp
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Start Live Voice Call",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // Primary Microphone Button with Wave Pulse
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(52.dp)
                        ) {
                            if (isMicListening) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .scale(micPulseScale + (userAmp * 0.35f))
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981).copy(alpha = 0.3f))
                                )
                            }

                            Surface(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .clickable { onMicClick() }
                                    .testTag("mic_toggle_button"),
                                shape = CircleShape,
                                color = if (isMicListening) Color(0xFF10B981) else Color(0xFF2563EB),
                                shadowElevation = 4.dp
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = if (isMicListening) "Stop Listening" else "Start Listening",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            }
        }

        // Transcript Modal Bottom Sheet
        if (showTranscriptSheet) {
            ModalBottomSheet(
                onDismissRequest = { showTranscriptSheet = false },
                sheetState = rememberModalBottomSheetState(),
                containerColor = Color(0xFF0F172A)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) "سجل المحادثة الحالية" else "Conversation Transcript",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showTranscriptSheet = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    if (conversationTurns.isEmpty()) {
                        Text(
                            text = if (isArabic) "لا توجد رسائل بعد." else "No messages recorded yet.",
                            color = Color(0xFF64748B),
                            fontSize = 14.sp
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(conversationTurns) { turn ->
                                val isUser = turn.role == "user"
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = if (isUser) Color(0xFF1E293B) else Color(0xFF1E3A8A).copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = if (isUser) (if (isArabic) "أنت" else "You") else (if (isArabic) character.nameAr else character.nameEn),
                                                color = if (isUser) Color(0xFF60A5FA) else Color(0xFF34D399),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = turn.evidenceLevel.name,
                                                color = Color(0xFF94A3B8),
                                                fontSize = 10.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = turn.content,
                                            color = Color.White,
                                            fontSize = 13.sp
                                        )

                                        if (turn.toolCallBadge != null) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Surface(
                                                color = Color(0xFF0284C7).copy(alpha = 0.35f),
                                                shape = RoundedCornerShape(6.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f))
                                            ) {
                                                Text(
                                                    text = "⚡ ${turn.toolCallBadge}",
                                                    color = Color(0xFF7DD3FC),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Character, Gender & Voice Tone Customizer Sheet
        if (showToneSheet) {
            CharacterToneModalSheet(
                viewModel = viewModel,
                onDismiss = { showToneSheet = false }
            )
        }

        // 7. Google Generative AI Dynamic Function Calling & Local Files Modal Sheet
        if (showToolsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showToolsSheet = false },
                sheetState = rememberModalBottomSheetState(),
                containerColor = Color(0xFF0F172A),
                contentColor = Color.White
            ) {
                var selectedToolTab by remember { mutableStateOf(0) }
                var searchInput by remember { mutableStateOf("") }
                var localFiles by remember { mutableStateOf(viewModel.getAvailableLocalFiles()) }
                var newFileName by remember { mutableStateOf("") }
                var newFileContent by remember { mutableStateOf("") }
                var showAddFileDialog by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isArabic) "واجهة أدوات Google GenAI" else "Google GenAI Tools Interface",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = Color(0xFF0284C7).copy(alpha = 0.3f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Function Calling",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        IconButton(onClick = { showToolsSheet = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tool Selector Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = selectedToolTab == 0,
                            onClick = { selectedToolTab = 0 },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text(if (isArabic) "🌐 بحث جوجل (Google Search)" else "🌐 Google Search") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF0284C7),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color(0xFF94A3B8)
                            )
                        )
                        FilterChip(
                            selected = selectedToolTab == 1,
                            onClick = {
                                selectedToolTab = 1
                                localFiles = viewModel.getAvailableLocalFiles()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text(if (isArabic) "📁 الملفات المحلية (${localFiles.size})" else "📁 Local Files (${localFiles.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF0D9488),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color(0xFF94A3B8)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (selectedToolTab == 0) {
                        // GOOGLE SEARCH FUNCTION CALLING TAB
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = if (isArabic) "استدعاء أداة البحث المباشر في جوجل (google_search):" else "Invoke Google Search Function Call (google_search):",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = searchInput,
                                onValueChange = { searchInput = it },
                                placeholder = {
                                    Text(
                                        if (isArabic) "اكتب استعلام البحث، مثلاً: أحدث استراتيجيات الأعمال 2026..." else "Enter search query, e.g. Business strategies 2026...",
                                        color = Color(0xFF64748B),
                                        fontSize = 13.sp
                                    )
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            if (searchInput.isNotBlank()) {
                                                viewModel.executeGoogleSearchAction(searchInput.trim())
                                                showToolsSheet = false
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF38BDF8))
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedContainerColor = Color(0xFF1E293B),
                                    unfocusedContainerColor = Color(0xFF1E293B),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (isArabic) "مواضيع بحث مقترحة:" else "Suggested Search Topics:",
                                color = Color(0xFFCBD5E1),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            val quickTopics = if (isArabic) {
                                listOf(
                                    "أحدث نماذج الذكاء الاصطناعي 2026",
                                    "مؤشرات الأسواق المالية العالمية",
                                    "تقنيات الطاقة النظيفة والمستدامة",
                                    "أفضل ممارسات إدارة وتطوير الفرق"
                                )
                            } else {
                                listOf(
                                    "Latest AI Models and Benchmarks 2026",
                                    "Global Market Indicators and Trends",
                                    "Sustainable Clean Energy Innovations",
                                    "High-Performance Team Leadership"
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                quickTopics.forEach { topic ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.executeGoogleSearchAction(topic)
                                                showToolsSheet = false
                                            },
                                        color = Color(0xFF1E293B),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = null,
                                                tint = Color(0xFF38BDF8),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(text = topic, color = Color.White, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // LOCAL FILES ACCESS FUNCTION CALLING TAB
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isArabic) "ملفات النظام والتخزين المحلي المتاحة للأفاتار:" else "Device Storage Files available to Avatar:",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 13.sp
                                )
                                OutlinedButton(
                                    onClick = { showAddFileDialog = !showAddFileDialog },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2DD4BF))
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isArabic) "ملف جديد" else "New File", fontSize = 12.sp)
                                }
                            }

                            if (showAddFileDialog) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    color = Color(0xFF1E293B),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        OutlinedTextField(
                                            value = newFileName,
                                            onValueChange = { newFileName = it },
                                            placeholder = { Text("example.txt", color = Color(0xFF64748B), fontSize = 12.sp) },
                                            label = { Text(if (isArabic) "اسم الملف" else "File Name", fontSize = 11.sp) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color(0xFF2DD4BF),
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = newFileContent,
                                            onValueChange = { newFileContent = it },
                                            placeholder = { Text(if (isArabic) "محتوى الملف..." else "File content...", color = Color(0xFF64748B), fontSize = 12.sp) },
                                            label = { Text(if (isArabic) "المحتوى" else "Content", fontSize = 11.sp) },
                                            modifier = Modifier.fillMaxWidth(),
                                            maxLines = 3,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color(0xFF2DD4BF),
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                            OutlinedButton(
                                                onClick = {
                                                    if (newFileName.isNotBlank() && newFileContent.isNotBlank()) {
                                                        viewModel.writeLocalFile(newFileName.trim(), newFileContent.trim())
                                                        localFiles = viewModel.getAvailableLocalFiles()
                                                        newFileName = ""
                                                        newFileContent = ""
                                                        showAddFileDialog = false
                                                    }
                                                },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2DD4BF))
                                            ) {
                                                Text(if (isArabic) "حفظ الملف" else "Save File")
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(localFiles) { file ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = Color(0xFF1E293B),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.Description,
                                                        contentDescription = null,
                                                        tint = Color(0xFF2DD4BF),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = file.name,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "${file.sizeBytes} bytes",
                                                    color = Color(0xFF94A3B8),
                                                    fontSize = 11.sp
                                                )
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    viewModel.executeFileInspectionAction(file.name)
                                                    showToolsSheet = false
                                                },
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = Color(0xFF38BDF8)
                                                ),
                                                modifier = Modifier.padding(start = 8.dp)
                                            ) {
                                                Text(
                                                    text = if (isArabic) "قراءة صوتية" else "Voice Read",
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

/**
 * Animated real-time voice spectrum waveform that reacts dynamically to voice amplitude.
 */
@Composable
fun LiveVoiceWaveform(
    amplitude: Float,
    isActive: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    barCount: Int = 10
) {
    val infiniteTransition = rememberInfiniteTransition(label = "live_waveform_transition")

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val animFraction by infiniteTransition.animateFloat(
                initialValue = 0.25f,
                targetValue = 0.95f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 280 + (i * 55), easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_anim_$i"
            )

            val heightFraction = if (isActive) {
                val clampedAmp = amplitude.coerceIn(0.1f, 1f)
                val waveOffset = (kotlin.math.sin(i * 0.7) * 0.2f + 0.8f).toFloat()
                (clampedAmp * 0.65f + animFraction * 0.35f) * waveOffset
            } else {
                0.15f
            }.coerceIn(0.12f, 1f)

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((22 * heightFraction).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}

/**
 * Bottom Sheet for Real-Time Character (Girl/Boy), Voice & Tone Customization.
 * Allows choosing avatar character, voice profile, pitch, speed, and persona style.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterToneModalSheet(
    viewModel: AvatarViewModel,
    onDismiss: () -> Unit
) {
    val character by viewModel.selectedCharacter.collectAsStateWithLifecycle()
    val selectedVoice by viewModel.selectedVoice.collectAsStateWithLifecycle()
    val selectedPersona by viewModel.selectedPersona.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isArabic = settings.languageMode == LanguageMode.ARABIC

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF0F172A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isArabic) "تخصيص الشخصية ونبرة الصوت" else "Character & Voice Tone",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Character & Gender Selector (بنت / ولد)
            Text(
                text = if (isArabic) "1. اختيار الشخصية والجنس (بنت / ولد)" else "1. Avatar Character & Gender",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AvailableCharacters.allCharacters.forEach { c ->
                    val isSel = c.id == character.id
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { viewModel.selectCharacter(c.id) }
                            .testTag("sheet_char_card_${c.id}"),
                        color = if (isSel) Color(0xFF1E293B) else Color(0xFF141D2D),
                        border = androidx.compose.foundation.BorderStroke(
                            2.dp,
                            if (isSel) Color(0xFF3B82F6) else Color(0xFF1E293B)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .border(
                                        2.dp,
                                        if (isSel) Color(0xFF3B82F6) else Color(0xFF334155),
                                        CircleShape
                                    )
                            ) {
                                Image(
                                    painter = painterResource(id = c.avatarResId),
                                    contentDescription = c.nameEn,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isArabic) c.nameAr else c.nameEn,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (c.gender == AvatarGender.FEMALE) Color(0x28EC4899) else Color(0x283B82F6)
                            ) {
                                Text(
                                    text = if (isArabic) {
                                        if (c.gender == AvatarGender.FEMALE) "👧 بنت" else "👦 ولد"
                                    } else {
                                        if (c.gender == AvatarGender.FEMALE) "👧 Girl" else "👦 Boy"
                                    },
                                    color = if (c.gender == AvatarGender.FEMALE) Color(0xFFF472B6) else Color(0xFF60A5FA),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 2. Voice Profiles (نبرات الصوت)
            val availableVoices = AvailableCharacters.getVoicesForGender(character.gender)
            Text(
                text = if (isArabic) "2. نبرة الصوت المتاحة" else "2. Available Voice Profile & Tone",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableVoices.forEach { voice ->
                    val isVoiceSelected = voice.id == selectedVoice.id
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.selectVoice(voice.id) }
                            .testTag("sheet_voice_item_${voice.id}"),
                        color = if (isVoiceSelected) Color(0xFF1E293B) else Color(0xFF141D2D),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isVoiceSelected) Color(0xFF3B82F6) else Color(0xFF1E293B)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isArabic) voice.nameAr else voice.nameEn,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = if (isVoiceSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = if (isArabic) voice.descriptionAr else voice.descriptionEn,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                            if (isVoiceSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color(0xFF3B82F6),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3. Fine-tuning Voice Pitch & Tone Presets (نبرة الصوت)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF141D2D),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (isArabic) "خيارات نبرة الصوت السريعة" else "Quick Tone Presets",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val presets = listOf(
                            Triple(if (isArabic) "🌸 دافئة" else "🌸 Warm", 1.12f, 1.0f),
                            Triple(if (isArabic) "🕊️ هادئة" else "🕊️ Calm", 1.00f, 0.95f),
                            Triple(if (isArabic) "⚡ حيوية" else "⚡ Sharp", 1.18f, 1.05f),
                            Triple(if (isArabic) "🎙️ عميقة" else "🎙️ Deep", 0.85f, 0.95f)
                        )
                        presets.forEach { (label, pitch, speed) ->
                            val isCurrent = kotlin.math.abs(settings.voicePitch - pitch) < 0.05f &&
                                            kotlin.math.abs(settings.voiceSpeed - speed) < 0.05f
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        viewModel.setVoicePitch(pitch)
                                        viewModel.setVoiceSpeed(speed)
                                    },
                                color = if (isCurrent) Color(0xFF2563EB) else Color(0xFF1E293B),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isCurrent) Color(0xFF60A5FA) else Color(0xFF334155)
                                )
                            ) {
                                Text(
                                    text = label,
                                    color = if (isCurrent) Color.White else Color(0xFFCBD5E1),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier
                                        .padding(vertical = 7.dp)
                                        .fillMaxWidth(),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) "نبرة الصوت الدقيقة (عميق جهير ← ناعم حاد)" else "Fine Pitch (Deep ← Sharp)",
                            color = Color(0xFFE2E8F0),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.2f", settings.voicePitch)}x",
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Slider(
                        value = settings.voicePitch,
                        onValueChange = { viewModel.setVoicePitch(it) },
                        valueRange = 0.8f..1.3f,
                        steps = 4,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF38BDF8),
                            activeTrackColor = Color(0xFF38BDF8),
                            inactiveTrackColor = Color(0xFF334155)
                        ),
                        modifier = Modifier.testTag("modal_voice_pitch_slider")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Speech Pace Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) "سرعة الإلقاء والكلام" else "Speech Pace",
                            color = Color(0xFFCBD5E1),
                            fontSize = 13.sp
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.2f", settings.voiceSpeed)}x",
                            color = Color(0xFF60A5FA),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Slider(
                        value = settings.voiceSpeed,
                        onValueChange = { viewModel.setVoiceSpeed(it) },
                        valueRange = 0.75f..1.5f,
                        steps = 3,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF60A5FA),
                            activeTrackColor = Color(0xFF60A5FA),
                            inactiveTrackColor = Color(0xFF334155)
                        ),
                        modifier = Modifier.testTag("modal_voice_speed_slider")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Live Voice Preview Button
                    OutlinedButton(
                        onClick = { viewModel.previewVoice(selectedVoice) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("modal_preview_voice_btn"),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B82F6))
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Test voice",
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isArabic) "تجربة الاستماع للنبرة والسرعة الحالية 🔊" else "Listen to Tone & Pace Sample 🔊",
                            color = Color(0xFF60A5FA),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 4. Persona Style Selector
            Text(
                text = if (isArabic) "4. أسلوب وطبيعة الشخصية" else "4. Persona Style",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PersonaStyle.entries.forEach { style ->
                    val isStyleSelected = style == selectedPersona
                    FilterChip(
                        selected = isStyleSelected,
                        onClick = { viewModel.setPersona(style) },
                        label = {
                            Text(
                                text = if (isArabic) style.titleAr else style.titleEn,
                                fontSize = 12.sp,
                                fontWeight = if (isStyleSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF2563EB),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF141D2D),
                            labelColor = Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.testTag("sheet_style_${style.name}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun ImmersiveLiveVideoCallView(
    character: AvatarCharacter,
    avatarState: AvatarState,
    viseme: VisemeFrame,
    speechAmp: Float,
    userAmp: Float,
    isArabic: Boolean,
    isMicListening: Boolean,
    currentTranscript: String,
    lastAvatarResponse: String,
    activeToolCall: ToolCallInfo?,
    currentSentiment: com.example.sentiment.SentimentAnalysisResult? = null,
    onExitImmersive: () -> Unit,
    onToggleLiveCall: () -> Unit,
    onMicClick: () -> Unit,
    onBargeIn: () -> Unit,
    onSelectCharacter: (String) -> Unit,
    onToggleCamera: () -> Unit,
    isCameraActive: Boolean,
    onOpenToneSheet: () -> Unit,
    onToggleLanguage: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B14))
            .testTag("immersive_video_call_view")
    ) {
        // Fullscreen dynamic 3D animated character
        AvatarCanvas(
            avatarResId = character.avatarResId,
            gender = character.gender,
            state = avatarState,
            viseme = viseme,
            speechAudioAmplitude = speechAmp,
            modifier = Modifier.fillMaxSize(),
            isImmersiveMode = true
        )

        // Top Floating Live HUD Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Exit Fullscreen Button
            Surface(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onExitImmersive() }
                    .testTag("exit_immersive_video_button"),
                color = Color(0xCC0F172A),
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Box(modifier = Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.FullscreenExit,
                        contentDescription = "Exit Fullscreen",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Live Call Status Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xDD0F172A),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.7f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isArabic) "مكالمة فيديو حية - بالوقت الفعلي 🟢" else "Real-Time Live Video Call 🟢",
                        color = Color(0xFF34D399),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Quick Tone & Language Controls
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onToggleLanguage() },
                    color = Color(0xCC0F172A),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Text(
                        text = if (isArabic) "العربية" else "EN",
                        color = Color(0xFF60A5FA),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Surface(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onOpenToneSheet() },
                    color = Color(0xCC0F172A),
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Tune",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Floating Overlays above the bottom control dock
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Active Tool Call Card (if present)
            if (activeToolCall != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(bottom = 6.dp),
                    color = Color(0xEB0369A1),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = activeToolCall.toolIcon, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${if (isArabic) "أداة ذكية" else "Agent Tool"}: ${activeToolCall.toolName}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${activeToolCall.queryOrArg} ➔ ${activeToolCall.resultPreview}",
                                color = Color(0xFFE0F2FE),
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Real-Time Subtitle Card
            if (lastAvatarResponse.isNotBlank() || currentTranscript.isNotBlank()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .padding(bottom = 8.dp)
                        .testTag("immersive_subtitle_card"),
                    color = Color(0xE60A0F1D),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        if (currentSentiment != null) {
                            val s = currentSentiment
                            Row(
                                modifier = Modifier.padding(bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = Color(0x33EC4899),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF472B6).copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = s.category.emoji, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isArabic) s.category.labelAr else s.category.labelEn,
                                            color = Color(0xFFF472B6),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isArabic) s.emotionalSummaryAr else s.emotionalSummaryEn,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (currentTranscript.isNotBlank() && avatarState == AvatarState.LISTENING) {
                            Text(
                                text = "${if (isArabic) "أنت" else "You"}: $currentTranscript",
                                color = Color(0xFF93C5FD),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        } else if (lastAvatarResponse.isNotBlank()) {
                            Text(
                                text = "${if (isArabic) character.nameAr else character.nameEn}: $lastAvatarResponse",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            // Live Voice Waveform & Status Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                LiveVoiceWaveform(
                    amplitude = if (avatarState == AvatarState.SPEAKING) speechAmp else userAmp,
                    isActive = avatarState == AvatarState.SPEAKING || isMicListening,
                    color = if (avatarState == AvatarState.SPEAKING) Color(0xFF60A5FA) else Color(0xFF34D399)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (avatarState) {
                        AvatarState.SPEAKING -> if (isArabic) "الأفاتار يتحدث معك الآن (تحدث لمقاطعته)" else "Avatar is speaking (speak to interrupt)"
                        AvatarState.LISTENING -> if (isArabic) "يستمع إليك في الوقت الفعلي... تحدث بحرية" else "Listening in real-time... speak freely"
                        AvatarState.THINKING -> if (isArabic) "يفكر في الإجابة..." else "Thinking..."
                        AvatarState.INTERRUPTED -> if (isArabic) "تمت المقاطعة، أستمع إليك" else "Interrupted, listening"
                        else -> if (isArabic) "مكالمة حية مباشرة ومستمرة" else "Continuous real-time call"
                    },
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Floating Bottom Control Dock
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("immersive_control_dock"),
                shape = RoundedCornerShape(32.dp),
                color = Color(0xF00F172A),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                shadowElevation = 12.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // 1. Red End Call Button
                    Surface(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .clickable {
                                onToggleLiveCall()
                                onExitImmersive()
                            }
                            .testTag("immersive_end_call_button"),
                        color = Color(0xFFDC2626),
                        shape = CircleShape
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "End Call",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // 2. Camera Toggle Button
                    Surface(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .clickable { onToggleCamera() }
                            .testTag("immersive_camera_toggle_button"),
                        color = if (isCameraActive) Color(0xFF1E3A8A) else Color(0xFF1E293B),
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isCameraActive) Color(0xFF3B82F6) else Color(0xFF334155)
                        )
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isCameraActive) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                contentDescription = "Camera",
                                tint = if (isCameraActive) Color(0xFF60A5FA) else Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // 3. Central Pulsing Mic / Voice Button
                    Surface(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .clickable { onMicClick() }
                            .testTag("immersive_mic_button"),
                        color = if (isMicListening) Color(0xFF10B981) else Color(0xFF2563EB),
                        shape = CircleShape,
                        shadowElevation = 6.dp
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isMicListening) Icons.Default.Mic else Icons.Default.MicOff,
                                contentDescription = "Mic",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // 4. Barge-In Interrupt Button (Hand)
                    Surface(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .clickable { onBargeIn() }
                            .testTag("immersive_barge_in_btn"),
                        color = if (avatarState == AvatarState.SPEAKING) Color(0x33EF4444) else Color(0xFF1E293B),
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (avatarState == AvatarState.SPEAKING) Color(0xFFEF4444) else Color(0xFF334155)
                        )
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PanTool,
                                contentDescription = "Interrupt",
                                tint = if (avatarState == AvatarState.SPEAKING) Color(0xFFFCA5A5) else Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // 5. Quick Character Switcher (Icons of other characters)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AvailableCharacters.allCharacters.forEach { c ->
                            val isSelected = c.id == character.id
                            Image(
                                painter = painterResource(id = c.avatarResId),
                                contentDescription = c.nameEn,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(if (isSelected) 34.dp else 28.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155),
                                        shape = CircleShape
                                    )
                                    .clickable { onSelectCharacter(c.id) }
                                    .testTag("immersive_char_${c.id}")
                            )
                        }
                    }
                }
            }
        }
    }
}

