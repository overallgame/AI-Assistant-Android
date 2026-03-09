package com.example.aiassistant.di

import android.content.Context
import com.example.aiassistant.config.AppConfig
import com.example.aiassistant.data.local.db.AIAssistantDatabase
import com.example.aiassistant.data.mock.DefaultMockApiHandler
import com.example.aiassistant.data.mock.DefaultMockWsHandler
import com.example.aiassistant.data.mock.MockServerManager
import com.example.aiassistant.data.repository.ChatWebSocketRepository
import com.example.aiassistant.data.repository.ConversationRepository
import com.example.aiassistant.data.repository.DefaultChatWebSocketRepository
import com.example.aiassistant.data.repository.DefaultConversationRepository
import com.example.aiassistant.data.repository.DefaultUserRepository
import com.example.aiassistant.data.repository.UserRepository
import com.example.aiassistant.data.source.FakeRemoteConversationDataSource
import com.example.aiassistant.data.source.FakeRemoteUserDataSource
import com.example.aiassistant.data.source.LocalUserDataSource
import com.example.aiassistant.data.source.MockHttpRemoteConversationDataSource
import com.example.aiassistant.data.source.MockHttpRemoteUserDataSource
import com.example.aiassistant.data.source.RemoteConversationDataSource
import com.example.aiassistant.data.source.RemoteUserDataSource
import com.example.aiassistant.data.source.RoomLocalUserDataSource
import com.example.aiassistant.data.websocket.ChatWebSocketManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .pingInterval(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideChatWebSocketManager(
        okHttpClient: OkHttpClient,
    ): ChatWebSocketManager {
        return ChatWebSocketManager(okHttpClient)
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
    fun provideRemoteUserDataSource(
        appConfig: AppConfig,
        mockServerManager: MockServerManager,
        okHttpClient: OkHttpClient,
    ): RemoteUserDataSource {
        return if (appConfig.useMockBackend) {
            MockHttpRemoteUserDataSource(
                mockServerManager = mockServerManager,
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
        okHttpClient: OkHttpClient,
    ): RemoteConversationDataSource {
        return if (appConfig.useMockBackend) {
            MockHttpRemoteConversationDataSource(
                mockServerManager = mockServerManager,
                okHttpClient = okHttpClient,
            )
        } else {
            FakeRemoteConversationDataSource()
        }
    }

    @Provides
    @Singleton
    fun provideUserRepository(
        localUserDataSource: LocalUserDataSource,
        remoteUserDataSource: RemoteUserDataSource,
    ): UserRepository {
        return DefaultUserRepository(
            local = localUserDataSource,
            remote = remoteUserDataSource,
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
        return DefaultChatWebSocketRepository(webSocketManager = webSocketManager)
    }
}
