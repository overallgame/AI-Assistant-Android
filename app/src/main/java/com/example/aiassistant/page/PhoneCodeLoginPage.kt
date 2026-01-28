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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.aiassistant.viewmodel.AuthViewModel

@Composable
fun PhoneCodeLoginPage(
    onBack: () -> Unit,
    onHelp: () -> Unit,
    onLoginSuccess: () -> Unit,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val codeFocusRequester = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }

    val colors = MaterialTheme.colorScheme

    val phone = rememberSaveable { mutableStateOf("") }
    val code = rememberSaveable { mutableStateOf("") }
    val uiState by authViewModel.uiState.collectAsState()

    val phoneDigits = phone.value.filter { it.isDigit() }
    val codeDigits = code.value.filter { it.isDigit() }
    val hasSentCode =
        uiState.codeSentToPhoneDigits == phoneDigits && uiState.codeSentAtEpochMs != null
    val canSendCode =
        !uiState.isLoading && uiState.codeSecondsRemaining == 0 && phoneDigits.length == 11
    val canLogin =
        !uiState.isLoading && hasSentCode && phoneDigits.length == 11 && codeDigits.length == 6

    LaunchedEffect(uiState.errorMessage, uiState.infoMessage) {
        val message = uiState.errorMessage ?: uiState.infoMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        authViewModel.clearMessages()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = colors.background,
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(innerPadding)
                .padding(horizontal = 22.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.onBackground,
                    )
                }

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
                    )
                    Text(
                        text = "帮助",
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(54.dp))

            Text(
                text = "验证码登录",
                color = colors.onBackground,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(28.dp))

            OutlinedTextField(
                value = phone.value,
                onValueChange = {
                    phone.value = it.filter { ch -> ch.isDigit() }.take(11)
                    authViewModel.clearMessages()
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = {
                        if (canSendCode) {
                            authViewModel.sendCode(phone.value)
                        }
                        codeFocusRequester.requestFocus()
                    },
                ),
                leadingIcon = {
                    Text(
                        text = "+86",
                        color = colors.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                },
                trailingIcon = {
                    if (phone.value.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                phone.value = ""
                                authViewModel.clearMessages()
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = null,
                                tint = colors.onSurfaceVariant,
                            )
                        }
                    }
                },
                placeholder = {
                    Text(text = "手机号")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    disabledContainerColor = colors.surface,
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

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = code.value,
                    onValueChange = {
                        code.value = it.filter { ch -> ch.isDigit() }.take(6)
                        authViewModel.clearMessages()
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (canLogin) {
                                focusManager.clearFocus()
                                authViewModel.loginWithCode(
                                    phone = phone.value,
                                    code = code.value,
                                    onSuccess = onLoginSuccess,
                                )
                            }
                        },
                    ),
                    trailingIcon = {
                        if (code.value.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    code.value = ""
                                    authViewModel.clearMessages()
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = null,
                                    tint = colors.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    placeholder = { Text(text = "验证码") },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(codeFocusRequester)
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = colors.surface,
                        unfocusedContainerColor = colors.surface,
                        disabledContainerColor = colors.surface,
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

                Spacer(modifier = Modifier.width(12.dp))

                OutlinedButton(
                    onClick = { authViewModel.sendCode(phone.value) },
                    enabled = canSendCode,
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colors.onSurface,
                    ),
                ) {
                    val seconds = uiState.codeSecondsRemaining
                    Text(text = if (seconds > 0) "${seconds}s" else "发送验证码")
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    focusManager.clearFocus()
                    authViewModel.loginWithCode(
                        phone = phone.value,
                        code = code.value,
                        onSuccess = onLoginSuccess,
                    )
                },
                enabled = canLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                ),
            ) {
                Text(text = if (uiState.isLoading) "登录中..." else "登录")
            }
        }
    }
}
