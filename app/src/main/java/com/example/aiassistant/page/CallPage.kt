package com.example.aiassistant.page

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aiassistant.data.model.CallPhase
import com.example.aiassistant.viewmodel.CallViewModel

@Composable
fun CallPage(
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CallViewModel = hiltViewModel(),
) {
    val callState by viewModel.callState.collectAsState()
    val context = LocalContext.current

    // 权限状态 - 使用 State 在 LaunchedEffect 中动态更新
    var hasPermission by remember { mutableStateOf(false) }

    // 权限请求 launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    // 动态检查权限
    LaunchedEffect(Unit) {
        hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    // 动画效果：根据通话状态显示不同的动画
    val infiniteTransition = rememberInfiniteTransition(label = "callAnimation")

    // 声波动画 - 缩放效果
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "waveScale"
    )

    // 脉冲动画 - 透明度效果
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha"
    )

    // 根据通话阶段确定动画是否启用
    val isListening = callState.phase == CallPhase.Listening
    val isSpeaking = callState.phase == CallPhase.Speaking
    val showAnimation = isListening || isSpeaking

    // 权限被拒绝时显示提示
    if (!hasPermission) {
        PermissionDeniedContent(
            onRequestPermission = {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            },
            onBack = onEndCall
        )
        return
    }

    // 自动开始通话
    LaunchedEffect(Unit) {
        viewModel.startCall()
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFF1A1A2E), // 深色背景
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // 顶部：通话时长
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 48.dp),
            ) {
                Text(
                    text = callState.formattedDuration,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.White,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = callState.statusDescription,
                    fontSize = 18.sp,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }

            // 中间：动画区域
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.weight(1f),
            ) {
                // 外圈声波动画
                if (showAnimation) {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .scale(waveScale)
                            .clip(CircleShape)
                            .background(
                                if (isListening) Color(0xFF4CAF50).copy(alpha = pulseAlpha)
                                else Color(0xFF2196F3).copy(alpha = pulseAlpha)
                            )
                    )

                    // 第二层声波
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .scale(1f + (1f - waveScale) * 0.3f)
                            .clip(CircleShape)
                            .background(
                                if (isListening) Color(0xFF4CAF50).copy(alpha = pulseAlpha + 0.1f)
                                else Color(0xFF2196F3).copy(alpha = pulseAlpha + 0.1f)
                            )
                    )
                }

                // 中心图标
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(
                            when (callState.phase) {
                                CallPhase.Listening -> Color(0xFF4CAF50) // 绿色 - 正在听
                                CallPhase.Speaking -> Color(0xFF2196F3)  // 蓝色 - 正在说
                                CallPhase.Thinking -> Color(0xFFFF9800) // 橙色 - 思考中
                                CallPhase.Connecting -> Color(0xFF9E9E9E) // 灰色 - 连接中
                                else -> Color(0xFF757575)
                            }
                        ),
                ) {
                    Icon(
                        imageVector = when (callState.phase) {
                            CallPhase.Listening -> Icons.Filled.Mic
                            CallPhase.Speaking -> Icons.AutoMirrored.Filled.VolumeUp
                            else -> Icons.Filled.Call
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp),
                    )
                }
            }

            // 底部：通话控制栏
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 48.dp),
            ) {
                // 显示用户最后说的话（如果有）
                if (callState.lastUserSpeech.isNotBlank()) {
                    Text(
                        text = "你: ${callState.lastUserSpeech}",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 32.dp),
                        maxLines = 2,
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 显示AI最后的回复（如果有）
                if (callState.lastAiReply.isNotBlank()) {
                    Text(
                        text = "AI: ${callState.lastAiReply}",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(horizontal = 32.dp),
                        maxLines = 2,
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 控制按钮
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // 静音按钮
                    IconButton(
                        onClick = { viewModel.toggleMute() },
                        modifier = Modifier.size(56.dp),
                    ) {
                        Icon(
                            imageVector = if (callState.isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                            contentDescription = if (callState.isMuted) "取消静音" else "静音",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp),
                        )
                    }

                    Spacer(modifier = Modifier.width(32.dp))

                    // 挂断按钮
                    FloatingActionButton(
                        onClick = {
                            viewModel.endCall()
                            onEndCall()
                        },
                        containerColor = Color(0xFFE53935), // 红色
                        contentColor = Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 8.dp,
                        ),
                        modifier = Modifier.size(72.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CallEnd,
                            contentDescription = "挂断",
                            modifier = Modifier.size(36.dp),
                        )
                    }

                    Spacer(modifier = Modifier.width(32.dp))

                    // 打断按钮 - 仅在AI说话时显示
                    if (callState.phase == CallPhase.Speaking) {
                        IconButton(
                            onClick = { viewModel.interruptAi() },
                            modifier = Modifier.size(56.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Mic,
                                contentDescription = "打断AI说话",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    } else {
                        // 空白占位，保持对称
                        Box(modifier = Modifier.size(56.dp))
                    }
                }

                // 错误信息显示
                callState.errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = error,
                        fontSize = 14.sp,
                        color = Color(0xFFE53935),
                    )
                }
            }
        }
    }
}

/**
 * 权限被拒绝时显示的内容
 */
@Composable
private fun PermissionDeniedContent(
    onRequestPermission: () -> Unit,
    onBack: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF1A1A2E),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.MicOff,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(64.dp),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "需要麦克风权限",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "语音通话需要使用麦克风来识别您的语音。请授予麦克风权限后重试。",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onRequestPermission,
            ) {
                Text("授予权限")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onBack,
            ) {
                Text("返回")
            }
        }
    }
}
