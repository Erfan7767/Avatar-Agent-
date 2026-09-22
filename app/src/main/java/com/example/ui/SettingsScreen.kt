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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AvailableCharacters
import com.example.model.LanguageMode
import com.example.model.PersonaStyle
import com.example.viewmodel.AvatarViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AvatarViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val persistentMemories by viewModel.persistentMemories.collectAsStateWithLifecycle()
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()
    val isArabic = settings.languageMode == LanguageMode.ARABIC

    var showAddMemoryDialog by remember { mutableStateOf(false) }
    var newMemoryKey by remember { mutableStateOf("") }
    var newMemoryValue by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF0B0F19),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isArabic) "الإعدادات والتحكم" else "Settings & Controls",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Language & Mode
            SettingsSectionCard(title = if (isArabic) "اللغة وقواعد المحادثة" else "Language & Guardrails") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = settings.languageMode == LanguageMode.ARABIC,
                        onClick = { viewModel.selectLanguage(LanguageMode.ARABIC) },
                        label = { Text("العربية") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_lang_ar"),
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
                        label = { Text("English") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_lang_en"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF3B82F6),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFFCBD5E1)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isArabic) "السماح بالتبديل اللغوي (Code-Switching)" else "Allow Code-Switching",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (isArabic) "السماح بمزيج الكلمات العربية والإنجليزية" else "Permit mixed English and Arabic dialogue",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = settings.allowCodeSwitching,
                        onCheckedChange = { viewModel.toggleCodeSwitching(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF3B82F6)
                        ),
                        modifier = Modifier.testTag("code_switching_switch")
                    )
                }
            }

            // 2. Voice Speed & Tone (نبرة وسرعة الصوت)
            SettingsSectionCard(title = if (isArabic) "نبرة الصوت وسرعة الإلقاء" else "Voice Tone & Speech Rate") {
                // Tone Pitch Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isArabic) "نبرة الصوت (عميق ← ناعم/حاد)" else "Voice Pitch (Deep ← Sharp)",
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp
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
                    modifier = Modifier.testTag("voice_pitch_slider")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Speech Pace Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isArabic) "معدل الإلقاء وسرعة الكلام" else "Speech Pace",
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
                        thumbColor = Color(0xFF3B82F6),
                        activeTrackColor = Color(0xFF3B82F6),
                        inactiveTrackColor = Color(0xFF334155)
                    ),
                    modifier = Modifier.testTag("voice_speed_slider")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        val currentVoice = AvailableCharacters.getVoice(settings.selectedVoiceId)
                        viewModel.previewVoice(currentVoice)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("preview_voice_pitch_button"),
                    border = BorderStroke(1.dp, Color(0xFF3B82F6))
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Test voice pitch",
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF60A5FA)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isArabic) "تجربة الاستماع للنبرة والسرعة الحالية" else "Listen to Tone & Pace Sample",
                        color = Color(0xFF60A5FA),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // 3. Persistent Memory Management
            SettingsSectionCard(
                title = if (isArabic) "إدارة الذاكرة الدائمة (Verified Memory)" else "Persistent Memory Management"
            ) {
                Text(
                    text = if (isArabic)
                        "يتذكر النظام فقط المعلومات التي وافقتَ عليها بصراحة، ولا يخترع أي ذكريات زائفة."
                    else
                        "The system only recalls explicitly verified facts and never invents fake memories.",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isArabic) "المعلومات المحفوظة (${persistentMemories.size})" else "Saved Facts (${persistentMemories.size})",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    OutlinedButton(
                        onClick = { showAddMemoryDialog = true },
                        modifier = Modifier.testTag("add_memory_button"),
                        border = BorderStroke(1.dp, Color(0xFF3B82F6))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF60A5FA))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isArabic) "إضافة" else "Add Fact", color = Color(0xFF60A5FA), fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (persistentMemories.isEmpty()) {
                    Text(
                        text = if (isArabic) "لا توجد ذكريات دائمة محفوظة." else "No persistent memories saved.",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        persistentMemories.forEach { memory ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFF1E293B),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = memory.keyName,
                                            color = Color(0xFF60A5FA),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = memory.valueContent,
                                            color = Color.White,
                                            fontSize = 12.sp
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteMemory(memory.id) },
                                        modifier = Modifier.size(28.dp).testTag("delete_memory_${memory.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { viewModel.clearAllMemories() },
                        modifier = Modifier.align(Alignment.End).testTag("clear_all_memory_button")
                    ) {
                        Text(
                            text = if (isArabic) "مسح جميع الذكريات" else "Clear All Memories",
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // 4. Privacy & Camera Permissions
            SettingsSectionCard(title = if (isArabic) "الخصوصية والوسائط" else "Privacy & Media") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isArabic) "تفعيل كاميرا المستخدم" else "User Camera Input",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (isArabic)
                                "تُستخدم فقط للتفاعل المباشر. لا يتم تسجيل أو إرسال أي فيديو دون علمك."
                            else
                                "Used strictly for live interaction. No video is ever stored or transmitted.",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = settings.userCameraEnabled,
                        onCheckedChange = { viewModel.toggleUserCamera(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF3B82F6)
                        ),
                        modifier = Modifier.testTag("user_camera_switch")
                    )
                }
            }

            // 5. Diagnostics & Audit Logs Viewer
            SettingsSectionCard(title = if (isArabic) "سجل التدقيق والتشخيص (Audit Log)" else "Audit Log & Telemetry") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isArabic) "أحدث العمليات (${auditLogs.size})" else "Recent Operations (${auditLogs.size})",
                        color = Color(0xFFCBD5E1),
                        fontSize = 12.sp
                    )
                    TextButton(
                        onClick = { viewModel.clearAuditLogs() },
                        modifier = Modifier.testTag("clear_audit_logs_button")
                    ) {
                        Text(
                            text = if (isArabic) "تفريغ السجل" else "Clear Logs",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }

                val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (auditLogs.isEmpty()) {
                        Text(
                            text = if (isArabic) "لا توجد سجلات بعد." else "No audit logs recorded yet.",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp
                        )
                    } else {
                        auditLogs.take(25).forEach { log ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFF141D2D),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${timeFormat.format(Date(log.timestamp))} - ${log.eventType}",
                                            color = Color(0xFF60A5FA),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = log.status.name,
                                            color = if (log.status.name == "SUCCEEDED") Color(0xFF34D399) else Color(0xFFEF4444),
                                            fontSize = 10.sp
                                        )
                                    }
                                    Text(
                                        text = log.details,
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 11.sp
                                    )
                                    if (log.latencyMs > 0) {
                                        Text(
                                            text = "Latency: ${log.latencyMs}ms",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Add Memory Dialog
    if (showAddMemoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddMemoryDialog = false },
            title = {
                Text(
                    text = if (isArabic) "إضافة معلومة للذاكرة الدائمة" else "Add Persistent Fact",
                    color = Color.White
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newMemoryKey,
                        onValueChange = { newMemoryKey = it },
                        label = { Text(if (isArabic) "العنوان / المفتاح (مثال: الاسم، الهواية)" else "Key (e.g. Name, Topic)") },
                        modifier = Modifier.fillMaxWidth().testTag("add_memory_key_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0xFF475569)
                        )
                    )
                    OutlinedTextField(
                        value = newMemoryValue,
                        onValueChange = { newMemoryValue = it },
                        label = { Text(if (isArabic) "المحتوى المعتمد" else "Fact Content") },
                        modifier = Modifier.fillMaxWidth().testTag("add_memory_value_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0xFF475569)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newMemoryKey.isNotBlank() && newMemoryValue.isNotBlank()) {
                            viewModel.addMemory(newMemoryKey.trim(), newMemoryValue.trim())
                            newMemoryKey = ""
                            newMemoryValue = ""
                            showAddMemoryDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    modifier = Modifier.testTag("confirm_add_memory_button")
                ) {
                    Text(if (isArabic) "حفظ" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMemoryDialog = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF0F172A)
        )
    }
}

@Composable
fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}
