package com.example.aiassistant.config

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppConfig @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, 
        Context.MODE_PRIVATE
    )
    
    var webSocketUrl: String
        get() = prefs.getString(KEY_WS_URL, DEFAULT_WS_URL) ?: DEFAULT_WS_URL
        set(value) = prefs.edit().putString(KEY_WS_URL, value).apply()
    
    var autoConnect: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CONNECT, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_CONNECT, value).apply()

    var useMockBackend: Boolean
        get() = prefs.getBoolean(KEY_USE_MOCK_BACKEND, DEFAULT_USE_MOCK_BACKEND)
        set(value) = prefs.edit().putBoolean(KEY_USE_MOCK_BACKEND, value).apply()
    
    companion object {
        private const val PREFS_NAME = "ai_assistant_config"
        private const val KEY_WS_URL = "websocket_url"
        private const val KEY_AUTO_CONNECT = "auto_connect"
        private const val KEY_USE_MOCK_BACKEND = "use_mock_backend"

        const val DEFAULT_WS_URL = "wss://api.example.com/chat"
        const val DEFAULT_USE_MOCK_BACKEND = true
    }
}
