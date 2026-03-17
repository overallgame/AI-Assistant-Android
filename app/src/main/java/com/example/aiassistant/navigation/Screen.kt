package com.example.aiassistant.navigation

sealed class Screen(val route: String) {
    // Auth screens
    data object LoginEntry : Screen("login_entry")
    data object PhoneCodeLogin : Screen("phone_code_login")
    data object PasswordLogin : Screen("password_login")
    
    // Main screens
    data object Chat : Screen("chat")
    data object Call : Screen("call")
    data object Settings : Screen("settings")
    data object AccountManagement : Screen("account_management")
    data object FontSize : Screen("font_size")
    
    // Picker screens
    data object ImagePicker : Screen("image_picker")
    data object FilePicker : Screen("file_picker")
}
