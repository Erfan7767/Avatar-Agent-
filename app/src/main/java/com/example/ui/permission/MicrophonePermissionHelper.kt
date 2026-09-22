package com.example.ui.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Encapsulates the runtime status of the audio recording permission.
 */
enum class MicPermissionStatus {
    GRANTED,
    NEEDS_REQUEST,
    DENIED_RATIONALE,
    PERMANENTLY_DENIED
}

object MicrophonePermissionHelper {

    fun checkPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun getStatus(context: Context, hasEverRequested: Boolean = false): MicPermissionStatus {
        if (checkPermission(context)) {
            return MicPermissionStatus.GRANTED
        }
        val activity = context as? Activity ?: return MicPermissionStatus.NEEDS_REQUEST
        val shouldShow = ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.RECORD_AUDIO
        )
        return when {
            shouldShow -> MicPermissionStatus.DENIED_RATIONALE
            hasEverRequested -> MicPermissionStatus.PERMANENTLY_DENIED
            else -> MicPermissionStatus.NEEDS_REQUEST
        }
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

/**
 * A stateful holder for microphone permission in Compose, observing lifecycle
 * so that returning from System Settings updates the status immediately.
 */
@Composable
fun rememberMicrophonePermissionState(
    onPermissionGranted: () -> Unit = {}
): MicrophonePermissionState {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasRequestedOnce by remember { mutableStateOf(false) }
    var isGranted by remember {
        mutableStateOf(MicrophonePermissionHelper.checkPermission(context))
    }
    var showDialog by remember { mutableStateOf(false) }

    // Re-check permission whenever user returns to the app from Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val currentGranted = MicrophonePermissionHelper.checkPermission(context)
                if (currentGranted != isGranted) {
                    isGranted = currentGranted
                    if (currentGranted) {
                        showDialog = false
                        onPermissionGranted()
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasRequestedOnce = true
        isGranted = granted
        if (granted) {
            showDialog = false
            onPermissionGranted()
        } else {
            // Keep dialog open or show explanation dialog
            showDialog = true
        }
    }

    return remember(isGranted, showDialog, hasRequestedOnce) {
        MicrophonePermissionState(
            isGranted = isGranted,
            showDialog = showDialog,
            onDismissDialog = { showDialog = false },
            onShowDialog = { showDialog = true },
            requestPermission = {
                val currentStatus = MicrophonePermissionHelper.getStatus(context, hasRequestedOnce)
                if (currentStatus == MicPermissionStatus.PERMANENTLY_DENIED) {
                    showDialog = true
                } else {
                    launcher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            status = MicrophonePermissionHelper.getStatus(context, hasRequestedOnce)
        )
    }
}

class MicrophonePermissionState(
    val isGranted: Boolean,
    val showDialog: Boolean,
    val onDismissDialog: () -> Unit,
    val onShowDialog: () -> Unit,
    val requestPermission: () -> Unit,
    val status: MicPermissionStatus
)

/**
 * Onboarding Card shown on the setup screen to guide the user naturally
 * before initiating the conversation.
 */
@Composable
fun MicrophoneOnboardingCard(
    isArabic: Boolean,
    permissionState: MicrophonePermissionState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("mic_onboarding_card"),
        shape = RoundedCornerShape(16.dp),
        color = if (permissionState.isGranted) Color(0x1A10B981) else Color(0xFF131D31),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (permissionState.isGranted) Color(0x6610B981) else Color(0xFF2563EB).copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (permissionState.isGranted) Color(0xFF10B981).copy(alpha = 0.2f)
                                else Color(0xFF3B82F6).copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (permissionState.isGranted) Icons.Default.CheckCircle else Icons.Default.Mic,
                            contentDescription = null,
                            tint = if (permissionState.isGranted) Color(0xFF34D399) else Color(0xFF60A5FA),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isArabic) "إذن الصوت والميكروفون" else "Microphone & Voice Access",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (permissionState.isGranted) {
                                if (isArabic) "مفعل وجاهز للمحادثة الحية" else "Active & ready for real-time talk"
                            } else {
                                if (isArabic) "مطلوب للمكالمات الصوتية الحية" else "Required for live voice call"
                            },
                            color = if (permissionState.isGranted) Color(0xFF34D399) else Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }

                if (!permissionState.isGranted) {
                    Button(
                        onClick = { permissionState.requestPermission() },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        modifier = Modifier.testTag("enable_mic_onboarding_btn")
                    ) {
                        Text(
                            text = if (isArabic) "تفعيل" else "Enable",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (!permissionState.isGranted) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (isArabic) {
                        "تتيح لك صلاحية الميكروفون التحدث مع الوكيل الذكي ومقاطعته صوتياً بشكل فوري وطبيعي."
                    } else {
                        "Microphone access enables natural real-time voice chat and instant barge-in interruptions."
                    },
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

/**
 * Educational & Settings Dialog when permission is needed or permanently denied.
 */
@Composable
fun MicrophonePermissionDialog(
    isArabic: Boolean,
    permissionState: MicrophonePermissionState,
    onContinueAnyway: () -> Unit = {}
) {
    if (!permissionState.showDialog || permissionState.isGranted) return

    val context = LocalContext.current
    val isPermanentlyDenied = permissionState.status == MicPermissionStatus.PERMANENTLY_DENIED

    AlertDialog(
        onDismissRequest = { permissionState.onDismissDialog() },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color(0xFF0F172A),
        titleContentColor = Color.White,
        textContentColor = Color(0xFFCBD5E1),
        icon = {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2563EB).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPermanentlyDenied) Icons.Default.Lock else Icons.Default.Mic,
                    contentDescription = null,
                    tint = Color(0xFF60A5FA),
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = if (isArabic) {
                    if (isPermanentlyDenied) "تفعيل الميكروفون من الإعدادات" else "إذن الميكروفون مطلوب"
                } else {
                    if (isPermanentlyDenied) "Enable Mic in Settings" else "Microphone Permission Required"
                },
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isArabic) {
                        if (isPermanentlyDenied) {
                            "تم رفض إذن الميكروفون مسبقاً. لتمكين الصوت المباشر في الوقت الفعلي مع الأفاتار، يرجى فتح إعدادات التطبيق ومنح إذن الميكروفون."
                        } else {
                            "يحتاج الوكيل الذكي للوصول إلى الميكروفون ليتمكن من سماع صوتك في الوقت الفعلي والتحدث معك كشخص حقيقي."
                        }
                    } else {
                        if (isPermanentlyDenied) {
                            "Microphone permission is currently blocked. To enjoy real-time voice calls, please open app settings and enable Microphone access."
                        } else {
                            "The AI Agent requires microphone access to hear your speech in real-time and provide seamless voice-to-voice interaction."
                        }
                    },
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isArabic) {
                                "خصوصيتك محمية: يُستخدم الصوت فقط للحوار المباشر."
                            } else {
                                "Privacy protected: Audio is only used for live conversation."
                            },
                            color = Color(0xFFE2E8F0),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isPermanentlyDenied) {
                        MicrophonePermissionHelper.openAppSettings(context)
                        permissionState.onDismissDialog()
                    } else {
                        permissionState.requestPermission()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                modifier = Modifier.testTag("permission_dialog_confirm_btn")
            ) {
                if (isPermanentlyDenied) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = if (isArabic) {
                        if (isPermanentlyDenied) "فتح الإعدادات" else "السماح بالميكروفون"
                    } else {
                        if (isPermanentlyDenied) "Open Settings" else "Allow Microphone"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    permissionState.onDismissDialog()
                    onContinueAnyway()
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("permission_dialog_dismiss_btn")
            ) {
                Text(
                    text = if (isArabic) "متابعة بدون صوت" else "Continue without mic",
                    color = Color(0xFF94A3B8)
                )
            }
        }
    )
}
