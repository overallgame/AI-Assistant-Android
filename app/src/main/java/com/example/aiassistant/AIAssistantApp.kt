package com.example.aiassistant

import android.app.Application
import com.example.aiassistant.config.AppConfig
import com.example.aiassistant.data.mock.DefaultMockApiHandler
import com.example.aiassistant.data.mock.DefaultMockWsHandler
import com.example.aiassistant.data.mock.MockServerManager
import com.example.aiassistant.data.websocket.ChatWebSocketManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class AIAssistantApp : Application() {

    @Inject lateinit var appConfig: AppConfig
    @Inject lateinit var mockServerManager: MockServerManager
    @Inject lateinit var mockApiHandler: DefaultMockApiHandler
    @Inject lateinit var mockWsHandler: DefaultMockWsHandler
    @Inject lateinit var chatWebSocketManager: ChatWebSocketManager

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        if (appConfig.useMockBackend) {
            mockServerManager.startAll(mockApiHandler, mockWsHandler)
            appScope.launch {
                mockServerManager.wsServerUrl.collect { wsUrl ->
                    if (wsUrl != null) {
                        chatWebSocketManager.setServerUrl(wsUrl)
                    }
                }
            }
        }
    }
}
