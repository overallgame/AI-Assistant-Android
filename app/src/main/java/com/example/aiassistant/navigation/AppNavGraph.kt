package com.example.aiassistant.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.aiassistant.data.model.PickedFile
import com.example.aiassistant.data.model.PickedImage
import com.example.aiassistant.page.AccountManagementPage
import com.example.aiassistant.page.ChatPage
import com.example.aiassistant.page.FilePickerPage
import com.example.aiassistant.page.FontSizePage
import com.example.aiassistant.page.ImagePickerPage
import com.example.aiassistant.page.LoginEntryPage
import com.example.aiassistant.page.PasswordLoginPage
import com.example.aiassistant.page.PhoneCodeLoginPage
import com.example.aiassistant.page.SettingsPage
import com.example.aiassistant.viewmodel.AuthViewModel
import com.example.aiassistant.viewmodel.ChatViewModel
import com.example.aiassistant.viewmodel.SettingsViewModel

private const val ANIMATION_DURATION = 280

private val authScreens = setOf(
    Screen.LoginEntry.route,
    Screen.PhoneCodeLogin.route,
    Screen.PasswordLogin.route,
)

private val mainScreens = setOf(
    Screen.Chat.route,
    Screen.Settings.route,
    Screen.AccountManagement.route,
    Screen.FontSize.route,
)

private val pickerScreens = setOf(
    Screen.ImagePicker.route,
    Screen.FilePicker.route,
)

@Composable
fun AppNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    settingsViewModel: SettingsViewModel,
    chatViewModel: ChatViewModel,
    modifier: Modifier = Modifier,
    onOpenDrawer: (() -> Unit)? = null,
) {
    val userState by settingsViewModel.userState.collectAsState()
    val isLoggedIn = userState.session.isLoggedIn

    // 根据登录状态决定起始页面
    val startDestination = if (isLoggedIn) Screen.Chat.route else Screen.LoginEntry.route

    // 监听登录状态变化，自动导航
    LaunchedEffect(isLoggedIn) {
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        if (isLoggedIn && currentRoute in authScreens) {
            navController.navigate(Screen.Chat.route) {
                popUpTo(Screen.LoginEntry.route) { inclusive = true }
            }
        } else if (!isLoggedIn && currentRoute in mainScreens) {
            navController.navigate(Screen.LoginEntry.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(
            route = Screen.LoginEntry.route,
            enterTransition = { fadeIn(tween(ANIMATION_DURATION)) },
            exitTransition = { fadeOut(tween(ANIMATION_DURATION)) },
            popEnterTransition = { fadeIn(tween(ANIMATION_DURATION)) },
            popExitTransition = { fadeOut(tween(ANIMATION_DURATION)) },
        ) {
            LoginEntryPage(
                onPhoneCodeLogin = { navController.navigate(Screen.PhoneCodeLogin.route) },
                onPasswordLogin = { navController.navigate(Screen.PasswordLogin.route) },
                onHelp = { },
            )
        }

        composable(
            route = Screen.PhoneCodeLogin.route,
            enterTransition = { fadeIn(tween(ANIMATION_DURATION)) },
            exitTransition = { fadeOut(tween(ANIMATION_DURATION)) },
            popEnterTransition = { fadeIn(tween(ANIMATION_DURATION)) },
            popExitTransition = { fadeOut(tween(ANIMATION_DURATION)) },
        ) {
            PhoneCodeLoginPage(
                onBack = { navController.popBackStack() },
                onHelp = { },
                onLoginSuccess = {
                    navController.navigate(Screen.Chat.route) {
                        popUpTo(Screen.LoginEntry.route) { inclusive = true }
                    }
                },
                authViewModel = authViewModel,
            )
        }

        composable(
            route = Screen.PasswordLogin.route,
            enterTransition = { fadeIn(tween(ANIMATION_DURATION)) },
            exitTransition = { fadeOut(tween(ANIMATION_DURATION)) },
            popEnterTransition = { fadeIn(tween(ANIMATION_DURATION)) },
            popExitTransition = { fadeOut(tween(ANIMATION_DURATION)) },
        ) {
            PasswordLoginPage(
                onBack = { navController.popBackStack() },
                onHelp = { },
                onLoginSuccess = {
                    navController.navigate(Screen.Chat.route) {
                        popUpTo(Screen.LoginEntry.route) { inclusive = true }
                    }
                },
                onForgotPassword = { },
                authViewModel = authViewModel,
            )
        }

        composable(
            route = Screen.Chat.route,
            enterTransition = { fadeIn(tween(ANIMATION_DURATION)) },
            exitTransition = { fadeOut(tween(ANIMATION_DURATION)) },
            popEnterTransition = { fadeIn(tween(ANIMATION_DURATION)) },
            popExitTransition = { fadeOut(tween(ANIMATION_DURATION)) },
        ) {
            ChatPage(
                onOpenDrawer = { onOpenDrawer?.invoke() },
                onNewChat = { },
                onPickImage = { navController.navigate(Screen.ImagePicker.route) },
                onPickFile = { navController.navigate(Screen.FilePicker.route) },
                viewModel = chatViewModel,
            )
        }

        composable(
            route = Screen.Settings.route,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(ANIMATION_DURATION)
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(ANIMATION_DURATION)
                )
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec = tween(ANIMATION_DURATION)
                )
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(ANIMATION_DURATION)
                )
            },
        ) {
            SettingsPage(
                onBack = { navController.popBackStack() },
                onOpenAccountManagement = {
                    navController.navigate(Screen.AccountManagement.route)
                },
                onOpenFontSize = { navController.navigate(Screen.FontSize.route) },
                onLogout = {
                    authViewModel.logout {
                        navController.navigate(Screen.LoginEntry.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                viewModel = settingsViewModel,
            )
        }

        composable(
            route = Screen.AccountManagement.route,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(ANIMATION_DURATION)
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(ANIMATION_DURATION)
                )
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec = tween(ANIMATION_DURATION)
                )
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(ANIMATION_DURATION)
                )
            },
        ) {
            AccountManagementPage(
                onBack = { navController.popBackStack() },
                viewModel = settingsViewModel,
            )
        }

        composable(
            route = Screen.FontSize.route,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(ANIMATION_DURATION)
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(ANIMATION_DURATION)
                )
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec = tween(ANIMATION_DURATION)
                )
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(ANIMATION_DURATION)
                )
            },
        ) {
            FontSizePage(
                onBack = { navController.popBackStack() },
                viewModel = settingsViewModel,
            )
        }

        composable(
            route = Screen.ImagePicker.route,
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(ANIMATION_DURATION)
                )
            },
            exitTransition = {
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(ANIMATION_DURATION)
                )
            },
            popEnterTransition = {
                slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(ANIMATION_DURATION)
                )
            },
            popExitTransition = {
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(ANIMATION_DURATION)
                )
            },
        ) {
            ImagePickerPage(
                onBack = { navController.popBackStack() },
                onPicked = { pickedList: List<PickedImage> ->
                    pickedList.forEach { picked ->
                        chatViewModel.sendImage(
                            contentUri = picked.contentUri,
                            mimeType = picked.mimeType,
                            widthPx = picked.widthPx,
                            heightPx = picked.heightPx,
                        )
                    }
                    navController.popBackStack()
                },
            )
        }

        composable(
            route = Screen.FilePicker.route,
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(ANIMATION_DURATION)
                )
            },
            exitTransition = {
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(ANIMATION_DURATION)
                )
            },
            popEnterTransition = {
                slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(ANIMATION_DURATION)
                )
            },
            popExitTransition = {
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(ANIMATION_DURATION)
                )
            },
        ) {
            FilePickerPage(
                onBack = { navController.popBackStack() },
                onPicked = { pickedList: List<PickedFile> ->
                    pickedList.forEach { picked ->
                        chatViewModel.sendFile(
                            contentUri = picked.contentUri,
                            fileName = picked.fileName,
                            mimeType = picked.mimeType,
                            sizeBytes = picked.sizeBytes,
                        )
                    }
                    navController.popBackStack()
                },
            )
        }
    }
}
