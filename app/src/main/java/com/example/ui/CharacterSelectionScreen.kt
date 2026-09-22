package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.avatar.AvatarCanvas
import com.example.model.AvailableCharacters
import com.example.model.AvatarGender
import com.example.model.AvatarState
import com.example.model.LanguageMode
import com.example.model.PersonaStyle
import com.example.ui.permission.MicrophoneOnboardingCard
import com.example.ui.permission.MicrophonePermissionDialog
import com.example.ui.permission.rememberMicrophonePermissionState
import com.example.viewmodel.AvatarViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterSelectionScreen(
    viewModel: AvatarViewModel,
    onStartConversation: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val selectedCharacter by viewModel.selectedCharacter.collectAsStateWithLifecycle()
    val selectedVoice by viewModel.selectedVoice.collectAsStateWithLifecycle()
    val selectedPersona by viewModel.selectedPersona.collectAsStateWithLifecycle()
    val viseme by viewModel.visemeFrame.collectAsStateWithLifecycle()
    val speechAmp by viewModel.speechAudioAmplitude.collectAsStateWithLifecycle()

    val isArabic = settings.languageMode == LanguageMode.ARABIC
    val availableVoices = AvailableCharacters.getVoicesForGender(selectedCharacter.gender)
    val micPermissionState = rememberMicrophonePermissionState(
        onPermissionGranted = {
            // Permission gained smoothly
        }
    )

    MicrophonePermissionDialog(
        isArabic = isArabic,
        permissionState = micPermissionState,
        onContinueAnyway = onStartConversation
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF0B0F19),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isArabic) "الشخصية الافتراضية" else "AI Avatar Setup",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                actions = {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("settings_button")
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
        },
        bottomBar = {
            Surface(
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = {
                            if (micPermissionState.isGranted) {
                                onStartConversation()
                            } else {
                                micPermissionState.requestPermission()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("start_conversation_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isArabic) "بدء المحادثة الحية" else "Start Real-Time Conversation",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 0. Microphone Access Readiness Onboarding Card
            MicrophoneOnboardingCard(
                isArabic = isArabic,
                permissionState = micPermissionState,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // 1. Language Toggle
            Text(
                text = if (isArabic) "لغة المحادثة / Conversation Language" else "Conversation Language / لغة المحادثة",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = settings.languageMode == LanguageMode.ARABIC,
                    onClick = { viewModel.selectLanguage(LanguageMode.ARABIC) },
                    label = { Text("العربية (Arabic)", fontWeight = FontWeight.SemiBold) },
                    leadingIcon = if (settings.languageMode == LanguageMode.ARABIC) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("language_arabic_chip"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF3B82F6),
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF1E293B),
                        labelColor = Color(0xFFCBD5E1)
                    )
                )
                FilterChip(
                    selected = settings.languageMode == LanguageMode.ENGLISH,
                    onClick = { viewModel.selectLanguage(LanguageMode.ENGLISH) },
                    label = { Text("English", fontWeight = FontWeight.SemiBold) },
                    leadingIcon = if (settings.languageMode == LanguageMode.ENGLISH) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("language_english_chip"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF3B82F6),
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF1E293B),
                        labelColor = Color(0xFFCBD5E1)
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Character Selection (إيلينا / سارة / عمر)
            Text(
                text = if (isArabic) "اختر الشخصية الافتراضية" else "Choose Avatar Character",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AvailableCharacters.CHARACTERS.forEach { char ->
                    val isSelected = selectedCharacter.id == char.id
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { viewModel.selectCharacter(char.id) }
                            .testTag("select_char_${char.id}"),
                        color = if (isSelected) Color(0xFF2563EB) else Color(0xFF1E293B),
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF60A5FA) else Color(0xFF334155))
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isArabic) char.nameAr else char.nameEn,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (char.id == "elena_female") {
                                    if (isArabic) "شقراء فائقة الجمال" else "Blonde Beauty"
                                } else if (char.gender == AvatarGender.FEMALE) {
                                    if (isArabic) "أنثى دافئة" else "Warm Female"
                                } else {
                                    if (isArabic) "شخصية رسمية" else "Professional"
                                },
                                color = if (isSelected) Color(0xFFBFDBFE) else Color(0xFF64748B),
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Live Interactive Avatar Preview Box
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(310.dp)
                    .testTag("avatar_preview_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AvatarCanvas(
                        avatarResId = selectedCharacter.avatarResId,
                        gender = selectedCharacter.gender,
                        state = AvatarState.IDLE,
                        viseme = viseme,
                        speechAudioAmplitude = speechAmp,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Top Badge on Preview
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(14.dp),
                        color = Color(0x99000000),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isArabic) "معاينة حية للمظهر والحركة" else "Live Facial Preview",
                                color = Color(0xFFE2E8F0),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Bottom info on Preview
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color(0xDD0B1329))
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        color = Color.Transparent
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (isArabic) selectedCharacter.nameAr else selectedCharacter.nameEn,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isArabic) selectedCharacter.descriptionAr else selectedCharacter.descriptionEn,
                                color = Color(0xFFCBD5E1),
                                fontSize = 12.sp,
                                maxLines = 2
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Voice Selection (Female 1,2,3 / Male 1,2,3)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isArabic) "صوت الشخصية" else "Character Voice",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedButton(
                    onClick = { viewModel.previewVoice(selectedVoice) },
                    modifier = Modifier.testTag("preview_voice_button"),
                    contentPadding = ButtonDefaults.TextButtonContentPadding,
                    border = BorderStroke(1.dp, Color(0xFF3B82F6))
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Test voice",
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF60A5FA)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isArabic) "استماع للصوت" else "Play Sample",
                        color = Color(0xFF60A5FA),
                        fontSize = 12.sp
                    )
                }
            }
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
                            .testTag("voice_item_${voice.id}"),
                        color = if (isVoiceSelected) Color(0xFF1E293B) else Color(0xFF141D2D),
                        border = BorderStroke(
                            1.dp,
                            if (isVoiceSelected) Color(0xFF3B82F6) else Color(0xFF1E293B)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
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

            Spacer(modifier = Modifier.height(20.dp))

            // 5. Persona Style Selection
            Text(
                text = if (isArabic) "أسلوب وطبيعة الشخصية" else "Persona Style",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val personas = listOf(
                    PersonaStyle.FRIENDLY,
                    PersonaStyle.PROFESSIONAL,
                    PersonaStyle.TEACHER,
                    PersonaStyle.CASUAL
                )
                personas.forEach { persona ->
                    val isSelected = persona == selectedPersona
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setPersona(persona) },
                        label = {
                            Text(
                                text = if (isArabic) persona.titleAr else persona.titleEn,
                                fontSize = 12.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF3B82F6),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.testTag("persona_${persona.name.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
