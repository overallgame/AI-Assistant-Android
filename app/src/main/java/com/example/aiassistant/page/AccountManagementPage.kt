package com.example.aiassistant.page

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.aiassistant.data.model.Avatar
import com.example.aiassistant.data.model.AvatarType
import com.example.aiassistant.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountManagementPage(
    onBack: () -> Unit,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val userState by viewModel.userState.collectAsState()
    val user = userState.user
    val phoneMasked = maskPhoneForDisplay(user?.phoneE164 ?: user?.phoneMasked)
    val displayName = user?.displayName.orEmpty()

    val showEditNameDialog = remember { mutableStateOf(false) }
    val nameDraft = remember { mutableStateOf("") }
    val showAvatarDialog = remember { mutableStateOf(false) }

    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = "账号管理",
                    color = colors.onBackground,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.onBackground,
                    )
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = colors.background,
            ),
        )

        Spacer(modifier = Modifier.height(18.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier
                    .size(88.dp)
                    .clickable(onClick = { showAvatarDialog.value = true }),
            ) {
                Surface(
                    shape = CircleShape,
                    color = avatarColor(user?.avatar),
                    border = BorderStroke(1.dp, colors.outlineVariant),
                    modifier = Modifier.fillMaxSize(),
                ) {}

                Surface(
                    shape = CircleShape,
                    color = colors.surfaceVariant,
                    border = BorderStroke(1.dp, colors.outlineVariant),
                    modifier = Modifier
                        .size(28.dp)
                        .padding(2.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "EditAvatar",
                            tint = colors.onSurface,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = phoneMasked,
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "账户",
            color = colors.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
        )

        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                AccountRow(
                    icon = Icons.Filled.Edit,
                    title = "昵称",
                    value = displayName.ifBlank { "未设置" },
                    onClick = {
                        nameDraft.value = displayName
                        showEditNameDialog.value = true
                    },
                )
                AccountRow(
                    icon = Icons.Filled.Phone,
                    title = "手机号",
                    value = phoneMasked,
                    enabled = false,
                    showChevron = false,
                    onClick = { },
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = "登出所有设备",
                color = colors.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { })
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = "注销账号",
                color = colors.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { })
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        if (showEditNameDialog.value) {
            AlertDialog(
                onDismissRequest = { showEditNameDialog.value = false },
                title = {
                    Text(
                        text = "修改昵称",
                        color = colors.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                text = {
                    OutlinedTextField(
                        value = nameDraft.value,
                        onValueChange = { nameDraft.value = it.take(20) },
                        singleLine = true,
                        placeholder = { Text(text = "请输入昵称") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done,
                        ),
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showEditNameDialog.value = false
                            val newName = nameDraft.value.trim().takeIf { it.isNotBlank() }
                            val currentName = user?.displayName?.trim()?.takeIf { it.isNotBlank() }
                            if (newName != currentName) {
                                viewModel.setDisplayName(newName)
                            }
                        },
                    ) {
                        Text(text = "保存")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditNameDialog.value = false }) {
                        Text(text = "取消")
                    }
                },
                containerColor = colors.surface,
            )
        }

        if (showAvatarDialog.value) {
            AlertDialog(
                onDismissRequest = { showAvatarDialog.value = false },
                title = {
                    Text(
                        text = "选择头像",
                        color = colors.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                text = {
                    Column {
                        AvatarOptionRow(label = "蓝色") {
                            showAvatarDialog.value = false
                            viewModel.setAvatar(Avatar(type = AvatarType.Default, value = "Blue"))
                        }
                        AvatarOptionRow(label = "紫色") {
                            showAvatarDialog.value = false
                            viewModel.setAvatar(Avatar(type = AvatarType.Default, value = "Purple"))
                        }
                        AvatarOptionRow(label = "灰色") {
                            showAvatarDialog.value = false
                            viewModel.setAvatar(Avatar(type = AvatarType.Default, value = "Gray"))
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAvatarDialog.value = false }) {
                        Text(text = "关闭")
                    }
                },
                containerColor = colors.surface,
            )
        }
    }
}

@Composable
private fun AvatarOptionRow(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        Text(
            text = label,
            color = colors.onSurface,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun avatarColor(avatar: Avatar?): Color {
    val colors = MaterialTheme.colorScheme
    if (avatar?.type != AvatarType.Default) return colors.surfaceVariant
    return when (avatar.value) {
        "Purple" -> colors.tertiary
        "Gray" -> colors.surfaceVariant
        else -> colors.primary
    }
}

@Composable
private fun AccountRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    showChevron: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        val colors = MaterialTheme.colorScheme
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.onSurface,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    color = colors.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (showChevron) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.outlineVariant),
        )
    }
}

private fun maskPhoneForDisplay(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    val digits = raw.filter { it.isDigit() }
    if (digits.length < 5) return raw

    val prefix = digits.take(3)
    val suffix = digits.takeLast(2)
    return "$prefix*****$suffix"
}
