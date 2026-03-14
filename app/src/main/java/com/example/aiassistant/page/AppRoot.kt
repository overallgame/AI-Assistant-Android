package com.example.aiassistant.page

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.aiassistant.navigation.AppNavGraph
import com.example.aiassistant.navigation.Screen
import com.example.aiassistant.viewmodel.AuthViewModel
import com.example.aiassistant.viewmodel.ChatViewModel
import com.example.aiassistant.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun AppRoot(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val authViewModel: AuthViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val chatViewModel: ChatViewModel = hiltViewModel()

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val showDrawer = currentRoute in listOf(
        Screen.Chat.route,
        Screen.Settings.route,
        Screen.AccountManagement.route,
        Screen.FontSize.route,
    )

    // 在 ChatPage 上启用滑动手势打开 drawer，在其他页面禁用
    val gesturesEnabled = currentRoute == Screen.Chat.route

    val navContent: @Composable () -> Unit = {
        AppNavGraph(
            navController = navController,
            authViewModel = authViewModel,
            settingsViewModel = settingsViewModel,
            chatViewModel = chatViewModel,
            onOpenDrawer = {
                scope.launch { drawerState.open() }
            },
        )
    }

    if (showDrawer) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ConversationDrawer(
                    onCloseDrawer = { scope.launch { drawerState.close() } },
                    onOpenSettings = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Settings.route)
                    },
                )
            },
            gesturesEnabled = gesturesEnabled,
            modifier = modifier,
        ) {
            navContent()
        }
    } else {
        navContent()
    }
}
