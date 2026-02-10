package com.example.aiassistant.page

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aiassistant.viewmodel.ConversationDrawerViewModel

@Composable
fun ConversationDrawer(
    onCloseDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ConversationDrawerViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(320.dp)
            .background(colors.background)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Spacer(modifier = Modifier.height(28.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            when {
                uiState.isLoading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp),
                    ) {
                        CircularProgressIndicator(
                            color = colors.primary,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "加载中...",
                            color = colors.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                uiState.errorMessage != null -> {
                    Surface(
                        color = colors.surface,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 14.dp),
                        ) {
                            Text(
                                text = uiState.errorMessage ?: "加载失败",
                                color = colors.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            TextButton(onClick = { viewModel.refresh() }) {
                                Text(text = "重试")
                            }
                        }
                    }
                }

                uiState.groups.isEmpty() -> {
                    Text(
                        text = "暂无会话",
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
                    )
                }

                else -> {
                    val rows = buildList {
                        uiState.groups.forEach { group ->
                            add(DrawerListRow.GroupTitle(group.title))
                            group.items.forEachIndexed { index, item ->
                                add(DrawerListRow.Conversation(id = item.id, title = item.title))
                                if (index != group.items.lastIndex) {
                                    add(DrawerListRow.Divider(id = "${group.title}_$index"))
                                }
                            }
                            add(DrawerListRow.GroupSpacer(id = group.title))
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                    ) {
                        items(
                            items = rows,
                            key = { row -> row.key },
                        ) { row ->
                            when (row) {
                                is DrawerListRow.GroupTitle -> {
                                    Text(
                                        text = row.title,
                                        color = colors.onSurfaceVariant,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
                                    )
                                }

                                is DrawerListRow.Conversation -> {
                                    DrawerConversationRow(
                                        title = row.title,
                                        onClick = {
                                            viewModel.selectConversation(row.id)
                                            onCloseDrawer()
                                        },
                                        onMore = { /* TODO */ },
                                    )
                                }

                                is DrawerListRow.Divider -> {
                                    HorizontalDivider(color = colors.outlineVariant)
                                }

                                is DrawerListRow.GroupSpacer -> {
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenSettings)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                val displayName = uiState.userInfo.displayName?.takeIf { it.isNotBlank() }
                val primaryText = displayName ?: uiState.userInfo.phoneMasked.ifBlank { "未登录" }
                val secondaryText = if (displayName != null && uiState.userInfo.phoneMasked.isNotBlank()) uiState.userInfo.phoneMasked else null

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp),
                ) {
                    Surface(
                        color = colors.surfaceVariant,
                        shape = CircleShape,
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = null,
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = primaryText,
                            color = colors.onSurface,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (secondaryText != null) {
                            Text(
                                text = secondaryText,
                                color = colors.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = colors.onSurface,
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}

private sealed interface DrawerListRow {
    val key: String

    data class GroupTitle(val title: String) : DrawerListRow {
        override val key: String = "group_$title"
    }

    data class Conversation(val id: String, val title: String) : DrawerListRow {
        override val key: String = "conv_$id"
    }

    data class Divider(val id: String) : DrawerListRow {
        override val key: String = "divider_$id"
    }

    data class GroupSpacer(val id: String) : DrawerListRow {
        override val key: String = "spacer_$id"
    }
}

@Composable
private fun DrawerConversationRow(
    title: String,
    onClick: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Text(
            text = title,
            color = colors.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onMore) {
            Icon(
                imageVector = Icons.Filled.MoreHoriz,
                contentDescription = "More",
                tint = colors.onSurfaceVariant,
            )
        }
    }
}
