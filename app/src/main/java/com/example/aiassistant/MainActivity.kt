package com.example.aiassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aiassistant.page.AppRoot
import com.example.aiassistant.ui.theme.AIAssistantTheme
import com.example.aiassistant.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val userState by settingsViewModel.userState.collectAsState()
            val fontPref = userState.preferences.fontSize
            val fontScaleOverride = if (fontPref.followSystem) null else fontPref.scale

            AIAssistantTheme(
                fontScale = fontScaleOverride,
            ) {
                AppRoot()
            }
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