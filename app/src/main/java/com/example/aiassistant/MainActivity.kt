package com.example.aiassistant

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aiassistant.data.model.AppearancePreference
import com.example.aiassistant.page.AppRoot
import com.example.aiassistant.ui.theme.AIAssistantTheme
import com.example.aiassistant.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // 权限请求回调
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            // 所有权限已授予
            android.util.Log.d("MainActivity", "所有语音权限已授予")
        } else {
            // 部分权限被拒绝
            val deniedPermissions = permissions.filter { !it.value }.keys
            android.util.Log.w("MainActivity", "部分权限被拒绝: $deniedPermissions")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 检查并请求语音权限
        checkAndRequestPermissions()

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val userState by settingsViewModel.userState.collectAsState()
            val fontPref = userState.preferences.fontSize
            val fontScaleOverride = if (fontPref.followSystem) null else fontPref.scale

            val darkTheme = when (userState.preferences.appearance) {
                AppearancePreference.System -> isSystemInDarkTheme()
                AppearancePreference.Light -> false
                AppearancePreference.Dark -> true
            }

            AIAssistantTheme(
                darkTheme = darkTheme,
                fontScale = fontScaleOverride,
            ) {
                AppRoot()
            }
        }
    }

    /**
     * 检查并请求语音相关权限
     */
    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // 检查 RECORD_AUDIO 权限 (语音识别和录音需要)
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }

        // 如果有需要申请的权限，进行请求
        if (permissionsToRequest.isNotEmpty()) {
            android.util.Log.d("MainActivity", "请求权限: $permissionsToRequest")
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            android.util.Log.d("MainActivity", "所有语音权限已具备")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppRootPreview() {
    AIAssistantTheme {
        AppRoot()
    }
}