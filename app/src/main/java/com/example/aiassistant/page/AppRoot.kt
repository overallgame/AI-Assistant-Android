package com.example.aiassistant.page

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aiassistant.data.model.PickedFile
import com.example.aiassistant.data.model.PickedImage
import com.example.aiassistant.viewmodel.AuthViewModel
import com.example.aiassistant.viewmodel.ChatViewModel
import com.example.aiassistant.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

private enum class AppScreen {
    LoginEntry,
    PhoneCodeLogin,
    PasswordLogin,
    Chat,
    Settings,
    AccountManagement,
    FontSize,
    ImagePicker,
    FilePicker,
}

private fun screenOrder(screen: AppScreen): Int {
    return when (screen) {
        AppScreen.LoginEntry -> 0
        AppScreen.PhoneCodeLogin -> 1
        AppScreen.PasswordLogin -> 1
        AppScreen.Chat -> 10
        AppScreen.ImagePicker -> 11
        AppScreen.FilePicker -> 11
        AppScreen.Settings -> 20
        AppScreen.AccountManagement -> 21
        AppScreen.FontSize -> 21
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppRoot(modifier: Modifier = Modifier) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val screenState = remember { mutableStateOf(AppScreen.LoginEntry) }

    val authViewModel: AuthViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val chatViewModel: ChatViewModel = hiltViewModel()
    val userState by settingsViewModel.userState.collectAsState()
    val isLoggedIn = userState.session.isLoggedIn

    LaunchedEffect(isLoggedIn) {
        val current = screenState.value
        if (isLoggedIn && current in setOf(
                AppScreen.LoginEntry,
                AppScreen.PhoneCodeLogin,
                AppScreen.PasswordLogin,
            )
        ) {
            screenState.value = AppScreen.Chat
        }

        if (!isLoggedIn && current in setOf(
                AppScreen.Chat,
                AppScreen.Settings,
                AppScreen.AccountManagement,
                AppScreen.FontSize,
            )
        ) {
            screenState.value = AppScreen.LoginEntry
        }
    }

    AnimatedContent(
        targetState = screenState.value,
        transitionSpec = {
            val forward = screenOrder(targetState) > screenOrder(initialState)
            val direction = if (forward) 1 else -1
            (slideInHorizontally(
                animationSpec = tween(durationMillis = 220),
                initialOffsetX = { fullWidth -> fullWidth * direction },
            ) + fadeIn(animationSpec = tween(durationMillis = 220))) togetherWith
                (slideOutHorizontally(
                    animationSpec = tween(durationMillis = 220),
                    targetOffsetX = { fullWidth -> -fullWidth * direction },
                ) + fadeOut(animationSpec = tween(durationMillis = 220)))
        },
        label = "AppScreenSlideFade",
    ) { targetScreen ->
        when (targetScreen) {
        AppScreen.LoginEntry -> {
            LoginEntryPage(
                onPhoneCodeLogin = { screenState.value = AppScreen.PhoneCodeLogin },
                onPasswordLogin = { screenState.value = AppScreen.PasswordLogin },
                onHelp = { },
                modifier = modifier,
            )
        }

        AppScreen.PhoneCodeLogin -> {
            PhoneCodeLoginPage(
                onBack = { screenState.value = AppScreen.LoginEntry },
                onHelp = { },
                onLoginSuccess = { screenState.value = AppScreen.Chat },
                authViewModel = authViewModel,
                modifier = modifier,
            )
        }

        AppScreen.PasswordLogin -> {
            PasswordLoginPage(
                onBack = { screenState.value = AppScreen.LoginEntry },
                onHelp = { },
                onLoginSuccess = { screenState.value = AppScreen.Chat },
                onForgotPassword = { },
                authViewModel = authViewModel,
                modifier = modifier,
            )
        }

        AppScreen.Chat,
        AppScreen.Settings,
        AppScreen.AccountManagement,
        AppScreen.FontSize,
            -> {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ConversationDrawer(
                        onCloseDrawer = { scope.launch { drawerState.close() } },
                        onOpenSettings = {
                            scope.launch { drawerState.close() }
                            screenState.value = AppScreen.Settings
                        },
                    )
                },
                modifier = modifier,
            ) {
                when (targetScreen) {
                    AppScreen.Chat -> {
                        ChatPage(
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                            onNewChat = {},
                            onPickImage = { screenState.value = AppScreen.ImagePicker },
                            onPickFile = { screenState.value = AppScreen.FilePicker },
                            viewModel = chatViewModel,
                        )
                    }

                    AppScreen.Settings -> {
                        SettingsPage(
                            onBack = { screenState.value = AppScreen.Chat },
                            onOpenAccountManagement = {
                                screenState.value = AppScreen.AccountManagement
                            },
                            onOpenFontSize = { screenState.value = AppScreen.FontSize },
                            onLogout = {
                                authViewModel.logout {
                                    screenState.value = AppScreen.LoginEntry
                                }
                            },
                            viewModel = settingsViewModel,
                        )
                    }

                    AppScreen.AccountManagement -> {
                        AccountManagementPage(
                            onBack = { screenState.value = AppScreen.Settings },
                            viewModel = settingsViewModel,
                        )
                    }

                    AppScreen.FontSize -> {
                        FontSizePage(
                            onBack = { screenState.value = AppScreen.Settings },
                            viewModel = settingsViewModel,
                        )
                    }

                    else -> Unit
                }
            }
        }

        AppScreen.ImagePicker -> {
            ImagePickerPage(
                onBack = { screenState.value = AppScreen.Chat },
                onPicked = { pickedList: List<PickedImage> ->
                    pickedList.forEach { picked ->
                        chatViewModel.sendImage(
                            contentUri = picked.contentUri,
                            mimeType = picked.mimeType,
                            widthPx = picked.widthPx,
                            heightPx = picked.heightPx,
                        )
                    }
                    screenState.value = AppScreen.Chat
                },
                modifier = modifier,
            )
        }

        AppScreen.FilePicker -> {
            FilePickerPage(
                onBack = { screenState.value = AppScreen.Chat },
                onPicked = { pickedList: List<PickedFile> ->
                    pickedList.forEach { picked ->
                        chatViewModel.sendFile(
                            contentUri = picked.contentUri,
                            fileName = picked.fileName,
                            mimeType = picked.mimeType,
                            sizeBytes = picked.sizeBytes,
                        )
                    }
                    screenState.value = AppScreen.Chat
                },
                modifier = modifier,
            )
        }
        }
    }
}
