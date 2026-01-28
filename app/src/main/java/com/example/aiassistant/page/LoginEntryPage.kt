package com.example.aiassistant.page

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun LoginEntryPage(
    onPhoneCodeLogin: () -> Unit,
    onPasswordLogin: () -> Unit,
    onHelp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val agreed = remember { mutableStateOf(true) }
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 22.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable(onClick = onHelp)
                    .padding(8.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = "Help",
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "帮助",
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(88.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "AI Assistant",
                color = colors.primary,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { if (agreed.value) onPhoneCodeLogin() },
            enabled = agreed.value,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
                disabledContainerColor = colors.surfaceVariant,
                disabledContentColor = colors.onSurfaceVariant,
            ),
        ) {
            Text(text = "手机验证码登录")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { if (agreed.value) onPasswordLogin() },
            enabled = agreed.value,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = colors.onSurface,
                disabledContentColor = colors.onSurfaceVariant,
            ),
            border = BorderStroke(1.dp, colors.outlineVariant),
        ) {
            Text(text = "密码登录")
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 18.dp),
        ) {
            Checkbox(
                checked = agreed.value,
                onCheckedChange = { agreed.value = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = colors.primary,
                    uncheckedColor = colors.onSurfaceVariant,
                    checkmarkColor = colors.onPrimary,
                ),
            )
            Text(
                text = "我已阅读并同意",
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = " 用户协议 ",
                color = colors.primary,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "与",
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = " 隐私政策",
                color = colors.primary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
