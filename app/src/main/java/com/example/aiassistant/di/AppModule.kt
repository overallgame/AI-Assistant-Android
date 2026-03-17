package com.example.aiassistant.di

import android.content.Context
import android.util.Log
import com.example.aiassistant.config.AppConfig
import com.example.aiassistant.data.model.AuthMode
import com.example.aiassistant.data.local.AIAssistantDatabase
import com.example.aiassistant.data.mock.DefaultMockApiHandler
import com.example.aiassistant.data.mock.DefaultMockWsHandler
import com.example.aiassistant.data.mock.MockServerManager
import com.example.aiassistant.data.repository.interfac.ChatWebSocketRepository
import com.example.aiassistant.data.repository.interfac.ConversationRepository
import com.example.aiassistant.data.repository.DefaultChatWebSocketRepository
import com.example.aiassistant.data.repository.DefaultConversationRepository
import com.example.aiassistant.data.repository.DefaultUserRepository
import com.example.aiassistant.data.repository.interfac.UserRepository
import com.example.aiassistant.data.source.FakeRemoteConversationDataSource
import com.example.aiassistant.data.source.FakeRemoteUserDataSource
import com.example.aiassistant.data.source.interfac.LocalUserDataSource
import com.example.aiassistant.data.source.MockHttpRemoteConversationDataSource
import com.example.aiassistant.data.source.MockHttpRemoteUserDataSource
import com.example.aiassistant.data.source.interfac.RemoteConversationDataSource
import com.example.aiassistant.data.source.interfac.RemoteUserDataSource
import com.example.aiassistant.data.source.RoomLocalUserDataSource
import com.example.aiassistant.data.websocket.ChatWebSocketManager
import com.example.aiassistant.speak.SpeakManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(appConfig: AppConfig): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .pingInterval(0, TimeUnit.SECONDS)

        // 仅在调试模式下启用日志
        if (appConfig.debugLogging) {
            val loggingInterceptor = HttpLoggingInterceptor { message ->
                Log.d("OkHttp", message)
            }.apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(loggingInterceptor)
        }

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideChatWebSocketManager(
        okHttpClient: OkHttpClient,
        appConfig: AppConfig,
    ): ChatWebSocketManager {
        return ChatWebSocketManager(okHttpClient, appConfig)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApplicationScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AIAssistantDatabase {
        return AIAssistantDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideMockServerManager(): MockServerManager {
        return MockServerManager()
    }

    @Provides
    @Singleton
    fun provideDefaultMockApiHandler(): DefaultMockApiHandler {
        return DefaultMockApiHandler()
    }

    @Provides
    @Singleton
    fun provideDefaultMockWsHandler(): DefaultMockWsHandler {
        return DefaultMockWsHandler()
    }

    @Provides
    @Singleton
    fun provideLocalUserDataSource(
        database: AIAssistantDatabase,
        applicationScope: CoroutineScope,
    ): LocalUserDataSource {
        return RoomLocalUserDataSource(
            dao = database.userStateDao(),
            scope = applicationScope,
        )
    }

    @Provides
    @Singleton
    fun provideRemoteUserDataSource(
        appConfig: AppConfig,
        mockServerManager: MockServerManager,
        defaultMockApiHandler: DefaultMockApiHandler,
        defaultMockWsHandler: DefaultMockWsHandler,
        okHttpClient: OkHttpClient,
    ): RemoteUserDataSource {
        return if (appConfig.useMockBackend) {
            MockHttpRemoteUserDataSource(
                mockServerManager = mockServerManager,
                mockApiHandler = defaultMockApiHandler,
                mockWsHandler = defaultMockWsHandler,
                okHttpClient = okHttpClient,
            )
        } else {
            FakeRemoteUserDataSource()
        }
    }

    @Provides
    @Singleton
    fun provideRemoteConversationDataSource(
        appConfig: AppConfig,
        mockServerManager: MockServerManager,
        defaultMockApiHandler: DefaultMockApiHandler,
        defaultMockWsHandler: DefaultMockWsHandler,
        okHttpClient: OkHttpClient,
    ): RemoteConversationDataSource {
        return if (appConfig.useMockBackend) {
            MockHttpRemoteConversationDataSource(
                mockServerManager = mockServerManager,
                mockApiHandler = defaultMockApiHandler,
                mockWsHandler = defaultMockWsHandler,
                okHttpClient = okHttpClient,
            )
        } else {
            FakeRemoteConversationDataSource()
        }
    }

    @Provides
    @Singleton
    fun provideAuthMode(appConfig: AppConfig): AuthMode {
        return when {
            appConfig.forceLocalAuth -> AuthMode.LOCAL_ONLY
            appConfig.useMockBackend -> AuthMode.LOCAL_THEN_REMOTE
            else -> AuthMode.REMOTE_ONLY
        }
    }

    @Provides
    @Singleton
    fun provideUserRepository(
        localUserDataSource: LocalUserDataSource,
        remoteUserDataSource: RemoteUserDataSource,
        authMode: AuthMode,
    ): UserRepository {
        return DefaultUserRepository(
            local = localUserDataSource,
            remote = remoteUserDataSource,
            authMode = authMode,
        )
    }

    @Provides
    @Singleton
    fun provideConversationRepository(
        remoteConversationDataSource: RemoteConversationDataSource,
    ): ConversationRepository {
        return DefaultConversationRepository(remote = remoteConversationDataSource)
    }

    @Provides
    @Singleton
    fun provideChatWebSocketRepository(
        webSocketManager: ChatWebSocketManager,
    ): ChatWebSocketRepository {
        return DefaultChatWebSocketRepository(webSocketManager)
    }

    /**
     * 提供语音管理器（讯飞SDK）
     */
    @Provides
    @Singleton
    fun provideSpeakManager(@ApplicationContext context: Context): SpeakManager {
        return SpeakManager(context).apply {
            init()
        }
    }
}
