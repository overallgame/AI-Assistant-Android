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

    // 星火大模型配置
    var appId: String
        get() = prefs.getString(KEY_XINGHUO_APP_ID, "ce6cf5f7") ?: ""
        set(value) = prefs.edit().putString(KEY_XINGHUO_APP_ID, value).apply()

    var apiKey: String
        get() = prefs.getString(KEY_XINGHUO_API_KEY, "53c22e3cf1ec6c2248f0c6fbe78551d5") ?: ""
        set(value) = prefs.edit().putString(KEY_XINGHUO_API_KEY, value).apply()

    var apiSecret: String
        get() = prefs.getString(KEY_XINGHUO_API_SECRET, "ZDM2ODU5NzJlOGI5ZjY5ZTBjZmMzMzMz") ?: ""
        set(value) = prefs.edit().putString(KEY_XINGHUO_API_SECRET, value).apply()

    var xinghuoDomain: String
        get() = prefs.getString(KEY_XINGHUO_DOMAIN, DEFAULT_XINGHUO_DOMAIN) ?: DEFAULT_XINGHUO_DOMAIN
        set(value) = prefs.edit().putString(KEY_XINGHUO_DOMAIN, value).apply()

    var thinkingType: String
        get() = prefs.getString(KEY_XINGHUO_THINKING_TYPE, DEFAULT_THINKING_TYPE) ?: DEFAULT_THINKING_TYPE
        set(value) = prefs.edit().putString(KEY_XINGHUO_THINKING_TYPE, value).apply()

    var searchEnabled: Boolean
        get() = prefs.getBoolean(KEY_XINGHUO_SEARCH_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_XINGHUO_SEARCH_ENABLED, value).apply()

    var searchMode: String
        get() = prefs.getString(KEY_XINGHUO_SEARCH_MODE, DEFAULT_SEARCH_MODE) ?: DEFAULT_SEARCH_MODE
        set(value) = prefs.edit().putString(KEY_XINGHUO_SEARCH_MODE, value).apply()
    
    var autoConnect: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CONNECT, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_CONNECT, value).apply()

    var useMockBackend: Boolean
        get() = prefs.getBoolean(KEY_USE_MOCK_BACKEND, DEFAULT_USE_MOCK_BACKEND)
        set(value) = prefs.edit().putBoolean(KEY_USE_MOCK_BACKEND, value).apply()

    var debugLogging: Boolean
        get() = prefs.getBoolean(KEY_DEBUG_LOGGING, true)
        set(value) = prefs.edit().putBoolean(KEY_DEBUG_LOGGING, value).apply()

    var forceLocalAuth: Boolean
        get() = prefs.getBoolean(KEY_FORCE_LOCAL_AUTH, false)
        set(value) = prefs.edit().putBoolean(KEY_FORCE_LOCAL_AUTH, value).apply()

    companion object {
        private const val PREFS_NAME = "ai_assistant_config"
        private const val KEY_AUTO_CONNECT = "auto_connect"
        private const val KEY_USE_MOCK_BACKEND = "use_mock_backend"
        private const val KEY_DEBUG_LOGGING = "debug_logging"
        private const val KEY_FORCE_LOCAL_AUTH = "force_local_auth"

        // 星火配置键
        private const val KEY_XINGHUO_APP_ID = "xinghuo_app_id"
        private const val KEY_XINGHUO_API_KEY = "xinghuo_api_key"
        private const val KEY_XINGHUO_API_SECRET = "xinghuo_api_secret"
        private const val KEY_XINGHUO_DOMAIN = "xinghuo_domain"
        private const val KEY_XINGHUO_THINKING_TYPE = "xinghuo_thinking_type"
        private const val KEY_XINGHUO_SEARCH_ENABLED = "xinghuo_search_enabled"
        private const val KEY_XINGHUO_SEARCH_MODE = "xinghuo_search_mode"

        const val DEFAULT_USE_MOCK_BACKEND = false
        const val DEFAULT_XINGHUO_DOMAIN = "spark-x"
        const val DEFAULT_THINKING_TYPE = "auto"
        const val DEFAULT_SEARCH_MODE = "normal"
    }
}
