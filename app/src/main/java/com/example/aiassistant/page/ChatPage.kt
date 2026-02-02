package com.example.aiassistant.page

import android.net.Uri
import android.util.Size
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aiassistant.data.model.AttachmentTransferStatus
import com.example.aiassistant.data.model.ChatMessage
import com.example.aiassistant.data.model.ChatMessagePart
import com.example.aiassistant.data.model.ChatRole
import com.example.aiassistant.viewmodel.ChatViewModel
import androidx.compose.material.icons.filled.Image as ImageIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatPage(
    onOpenDrawer: () -> Unit,
    onNewChat: () -> Unit,
    onPickImage: () -> Unit,
    onPickFile: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    val colors = MaterialTheme.colorScheme

    val topBarColors = TopAppBarDefaults.centerAlignedTopAppBarColors(
        containerColor = colors.background,
        titleContentColor = colors.onBackground,
        navigationIconContentColor = colors.onBackground,
        actionIconContentColor = colors.onBackground,
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = uiState.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            navigationIcon = {
                IconButton(onClick = onOpenDrawer) {
                    Icon(imageVector = Icons.Filled.Menu, contentDescription = "Menu")
                }
            },
            actions = {
                IconButton(
                    onClick = {
                        viewModel.newChat()
                        onNewChat()
                    },
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "New")
                }
            },
            colors = topBarColors,
        )

        if (uiState.messages.isEmpty()) {
            NewChatEmptyState(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        } else {
            ChatConversation(
                messages = uiState.messages,
                onRetryAttachment = viewModel::retryAttachment,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        }

        ChatInputBar(
            inputText = uiState.inputText,
            onInputChange = viewModel::setInputText,
            thinkingSelected = uiState.mode.thinkingEnabled,
            onToggleThinking = viewModel::toggleThinking,
            searchSelected = uiState.mode.searchEnabled,
            onToggleSearch = viewModel::toggleSearch,
            onMockSend = viewModel::mockSendHello,
            onPickImage = onPickImage,
            onPickFile = onPickFile,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        )
    }
}

@Composable
private fun NewChatEmptyState(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.Psychology,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(44.dp),
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "今天有什么可以帮到你？",
                color = colors.onBackground,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ChatConversation(
    messages: List<ChatMessage>,
    onRetryAttachment: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(items = messages, key = { it.id }) { msg ->
            ChatMessageItem(message = msg, onRetryAttachment = onRetryAttachment)
        }
        item {
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun ChatMessageItem(
    message: ChatMessage,
    onRetryAttachment: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val contentResolver = context.contentResolver
    val isUser = message.role == ChatRole.User
    val bubbleShape = if (isUser) {
        RoundedCornerShape(
            topStart = 18.dp,
            topEnd = 18.dp,
            bottomEnd = 6.dp,
            bottomStart = 18.dp,
        )
    } else {
        RoundedCornerShape(
            topStart = 18.dp,
            topEnd = 18.dp,
            bottomEnd = 18.dp,
            bottomStart = 6.dp,
        )
    }

    Row(
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        modifier = modifier.fillMaxWidth(),
    ) {
        Surface(
            color = colors.surfaceVariant,
            shape = bubbleShape,
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                message.parts.forEachIndexed { index, part ->
                    when (part) {
                        is ChatMessagePart.Text -> {
                            if (part.text.isNotBlank()) {
                                Text(
                                    text = part.text,
                                    color = colors.onSurface,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }

                        is ChatMessagePart.Image -> {
                            val thumbBitmap by produceState<android.graphics.Bitmap?>(initialValue = null, key1 = part.contentUri) {
                                value = try {
                                    contentResolver.loadThumbnail(Uri.parse(part.contentUri), Size(420, 420), null)
                                } catch (_: Throwable) {
                                    null
                                }
                            }

                            if (thumbBitmap != null) {
                                Image(
                                    bitmap = thumbBitmap!!.asImageBitmap(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 120.dp, max = 220.dp)
                                        .clip(RoundedCornerShape(14.dp)),
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(colors.surface)
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ImageIcon,
                                        contentDescription = null,
                                        tint = colors.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "图片",
                                        color = colors.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }

                            if (part.transferStatus != AttachmentTransferStatus.Done) {
                                Spacer(modifier = Modifier.height(6.dp))
                                AttachmentStatusRow(
                                    status = part.transferStatus,
                                    progress = part.progress,
                                    onRetry = { onRetryAttachment(message.id) },
                                )
                            }
                        }

                        is ChatMessagePart.File -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.surface)
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Description,
                                    contentDescription = null,
                                    tint = colors.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = part.fileName,
                                    color = colors.onSurface,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            if (part.transferStatus != AttachmentTransferStatus.Done) {
                                Spacer(modifier = Modifier.height(6.dp))
                                AttachmentStatusRow(
                                    status = part.transferStatus,
                                    progress = part.progress,
                                    onRetry = { onRetryAttachment(message.id) },
                                )
                            }
                        }
                    }

                    if (index != message.parts.lastIndex) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentStatusRow(
    status: AttachmentTransferStatus,
    progress: Float?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    when (status) {
        AttachmentTransferStatus.Uploading -> {
            Column(modifier = modifier.fillMaxWidth()) {
                Text(
                    text = "上传中...",
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress ?: 0f },
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.primary,
                    trackColor = colors.surface,
                )
            }
        }

        AttachmentTransferStatus.Processing -> {
            Text(
                text = "解析中...",
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = modifier,
            )
        }

        AttachmentTransferStatus.Failed -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier,
            ) {
                Text(
                    text = "失败",
                    color = colors.error,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = Modifier.width(10.dp))
                TextButton(onClick = onRetry) {
                    Icon(imageVector = Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "重试")
                }
            }
        }

        AttachmentTransferStatus.Done -> Unit
    }
}

@Composable
private fun ChatInputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    thinkingSelected: Boolean,
    onToggleThinking: () -> Unit,
    searchSelected: Boolean,
    onToggleSearch: () -> Unit,
    onMockSend: () -> Unit,
    onPickImage: () -> Unit,
    onPickFile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val canSend = inputText.isNotBlank()
    var showAttachmentMenu by remember { mutableStateOf(false) }

    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 10.dp,
        modifier = modifier.navigationBarsPadding(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box {
                    OutlinedIconButton(
                        onClick = { showAttachmentMenu = true },
                        shape = CircleShape,
                        colors = IconButtonDefaults.outlinedIconButtonColors(
                            contentColor = colors.onSurface,
                        ),
                        border = BorderStroke(1.dp, colors.outlineVariant),
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                    }

                    DropdownMenu(
                        expanded = showAttachmentMenu,
                        onDismissRequest = { showAttachmentMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(text = "图片") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Filled.ImageIcon, contentDescription = null)
                            },
                            onClick = {
                                showAttachmentMenu = false
                                onPickImage()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(text = "文件") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Filled.AttachFile, contentDescription = null)
                            },
                            onClick = {
                                showAttachmentMenu = false
                                onPickFile()
                            },
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                TextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    placeholder = {
                        Text(
                            text = "输入消息",
                            color = colors.onSurfaceVariant,
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp, max = 120.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = colors.surfaceVariant,
                        unfocusedContainerColor = colors.surfaceVariant,
                        disabledContainerColor = colors.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        focusedTextColor = colors.onSurface,
                        unfocusedTextColor = colors.onSurface,
                        focusedPlaceholderColor = colors.onSurfaceVariant,
                        unfocusedPlaceholderColor = colors.onSurfaceVariant,
                        cursorColor = colors.primary,
                    ),
                )

                Spacer(modifier = Modifier.width(10.dp))

                FilledIconButton(
                    onClick = { if (canSend) onMockSend() },
                    enabled = canSend,
                    shape = CircleShape,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary,
                        disabledContainerColor = colors.surfaceVariant,
                        disabledContentColor = colors.onSurfaceVariant,
                    ),
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ModeChip(
                        icon = Icons.Filled.Psychology,
                        text = "思考",
                        selected = thinkingSelected,
                        accent = colors.primary,
                        onClick = onToggleThinking,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    ModeChip(
                        icon = Icons.Filled.Public,
                        text = "搜索",
                        selected = searchSelected,
                        accent = colors.onSurface,
                        onClick = onToggleSearch,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedIconButton(
                        onClick = { /* TODO */ onMockSend() },
                        shape = CircleShape,
                        colors = IconButtonDefaults.outlinedIconButtonColors(
                            contentColor = colors.onSurface,
                        ),
                        border = BorderStroke(1.dp, colors.outlineVariant),
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(imageVector = Icons.Filled.MicNone, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeChip(
    icon: ImageVector,
    text: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) colors.surfaceVariant else colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) accent else colors.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = if (selected) colors.onSurface else colors.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
