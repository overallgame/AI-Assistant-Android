package com.example.aiassistant.page

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.aiassistant.data.model.AppearancePreference
import com.example.aiassistant.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(
    onBack: () -> Unit,
    onOpenAccountManagement: () -> Unit,
    onOpenFontSize: () -> Unit,
    onLogout: () -> Unit,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val showAppearanceDialog = remember { mutableStateOf(false) }
    val appearanceDialogSelection = remember { mutableStateOf(AppearanceOption.System) }
    val infoDialogTitle = remember { mutableStateOf<String?>(null) }
    val showLogoutDialog = remember { mutableStateOf(false) }

    val userState by viewModel.userState.collectAsState()
    val appearanceOption = userState.preferences.appearance.toAppearanceOption()
    val phoneMasked = userState.user?.phoneMasked

    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = "设置",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onBackground,
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

        Spacer(modifier = Modifier.height(10.dp))

        SettingsSection(
            title = "账户",
            items = listOf(
                SettingsRowModel(
                    icon = Icons.Filled.ManageAccounts,
                    title = "账号管理",
                    value = phoneMasked,
                    onClick = onOpenAccountManagement,
                ),
                SettingsRowModel(
                    icon = Icons.Filled.Storage,
                    title = "数据管理",
                    value = null,
                    onClick = { infoDialogTitle.value = "数据管理" },
                ),
            ),
        )

        Spacer(modifier = Modifier.height(12.dp))

        SettingsSection(
            title = "应用",
            items = listOf(
                SettingsRowModel(
                    icon = Icons.Filled.ColorLens,
                    title = "外观",
                    value = appearanceOption.label,
                    onClick = {
                        appearanceDialogSelection.value = appearanceOption
                        showAppearanceDialog.value = true
                    },
                ),
                SettingsRowModel(
                    icon = Icons.Filled.TextFields,
                    title = "字体大小",
                    value = null,
                    onClick = onOpenFontSize,
                ),
            ),
        )

        Spacer(modifier = Modifier.height(12.dp))

        SettingsSection(
            title = "关于",
            items = listOf(
                SettingsRowModel(
                    icon = Icons.Filled.Update,
                    title = "检查更新",
                    value = "1.6.4(149)",
                    onClick = { infoDialogTitle.value = "检查更新" },
                ),
                SettingsRowModel(
                    icon = Icons.Filled.Description,
                    title = "服务协议",
                    value = null,
                    onClick = { infoDialogTitle.value = "服务协议" },
                ),
                SettingsRowModel(
                    icon = Icons.Filled.SupportAgent,
                    title = "联系我们",
                    value = null,
                    onClick = { infoDialogTitle.value = "联系我们" },
                ),
                SettingsRowModel(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    title = "退出登录",
                    value = null,
                    onClick = { showLogoutDialog.value = true },
                    emphasize = true,
                ),
            ),
        )

//        Spacer(modifier = Modifier.weight(1f))
//
//        Column(
//            horizontalAlignment = Alignment.CenterHorizontally,
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(bottom = 18.dp),
//        ) {
//            Text(
//                text = "模型名称：DeepSeek Chat",
//                color = colors.onSurfaceVariant,
//                style = MaterialTheme.typography.labelMedium,
//            )
//            Spacer(modifier = Modifier.height(6.dp))
//            Text(
//                text = "内容由AI生成，请仔细甄别，并合法使用",
//                color = colors.onSurfaceVariant,
//                style = MaterialTheme.typography.labelSmall,
//            )
//        }

        if (showAppearanceDialog.value) {
            AppearanceDialog(
                selected = appearanceDialogSelection.value,
                onSelect = { appearanceDialogSelection.value = it },
                onConfirm = {
                    showAppearanceDialog.value = false
                    val newValue = appearanceDialogSelection.value.toAppearancePreference()
                    if (newValue != userState.preferences.appearance) {
                        viewModel.setAppearance(newValue)
                    }
                },
                onDismiss = { showAppearanceDialog.value = false },
            )
        }

        if (infoDialogTitle.value != null) {
            AlertDialog(
                onDismissRequest = { infoDialogTitle.value = null },
                title = {
                    Text(
                        text = infoDialogTitle.value ?: "",
                        color = colors.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                text = {
                    Text(
                        text = "功能开发中",
                        color = colors.onSurface,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
                confirmButton = {
                    TextButton(onClick = { infoDialogTitle.value = null }) {
                        Text(text = "知道了")
                    }
                },
                containerColor = colors.surface,
            )
        }

        if (showLogoutDialog.value) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog.value = false },
                title = {
                    Text(
                        text = "退出登录",
                        color = colors.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                text = {
                    Text(
                        text = "确定要退出登录吗？",
                        color = colors.onSurface,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLogoutDialog.value = false
                            onLogout()
                        },
                    ) {
                        Text(text = "确认")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog.value = false }) {
                        Text(text = "取消")
                    }
                },
                containerColor = colors.surface,
            )
        }
    }
}

private enum class AppearanceOption(val label: String) {
    System("系统"),
    Light("浅色"),
    Dark("深色"),
}

private fun AppearancePreference.toAppearanceOption(): AppearanceOption {
    return when (this) {
        AppearancePreference.System -> AppearanceOption.System
        AppearancePreference.Light -> AppearanceOption.Light
        AppearancePreference.Dark -> AppearanceOption.Dark
    }
}

private fun AppearanceOption.toAppearancePreference(): AppearancePreference {
    return when (this) {
        AppearanceOption.System -> AppearancePreference.System
        AppearanceOption.Light -> AppearancePreference.Light
        AppearanceOption.Dark -> AppearancePreference.Dark
    }
}

private data class SettingsRowModel(
    val icon: ImageVector,
    val title: String,
    val value: String?,
    val onClick: () -> Unit = {},
    val emphasize: Boolean = false,
)

@Composable
private fun SettingsSection(
    title: String,
    items: List<SettingsRowModel>,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Text(
            text = title,
            color = colors.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )

        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                items.forEachIndexed { index, item ->
                    SettingsRow(
                        icon = item.icon,
                        title = item.title,
                        value = item.value,
                        onClick = item.onClick,
                        emphasize = item.emphasize,
                        showDivider = index != items.lastIndex,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    value: String?,
    onClick: () -> Unit,
    emphasize: Boolean,
    showDivider: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
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
                    color = if (emphasize) colors.error else colors.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!value.isNullOrBlank()) {
                    Text(
                        text = value,
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        if (showDivider) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.outlineVariant),
            )
        }
    }
}

@Composable
private fun AppearanceDialog(
    selected: AppearanceOption,
    onSelect: (AppearanceOption) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "外观",
                color = colors.onSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column {
                AppearanceOption.entries.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = { onSelect(option) })
                            .padding(vertical = 10.dp),
                    ) {
                        RadioButton(
                            selected = selected == option,
                            onClick = { onSelect(option) },
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = option.label,
                            color = colors.onSurface,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "确认")
            }
        },
        containerColor = colors.surface,
    )
}
