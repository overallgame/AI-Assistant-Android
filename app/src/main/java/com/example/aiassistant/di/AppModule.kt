package com.example.aiassistant.di

import android.content.Context
import com.example.aiassistant.data.local.db.AIAssistantDatabase
import com.example.aiassistant.data.repository.ConversationRepository
import com.example.aiassistant.data.repository.DefaultConversationRepository
import com.example.aiassistant.data.repository.DefaultUserRepository
import com.example.aiassistant.data.repository.UserRepository
import com.example.aiassistant.data.source.FakeRemoteConversationDataSource
import com.example.aiassistant.data.source.FakeRemoteUserDataSource
import com.example.aiassistant.data.source.LocalUserDataSource
import com.example.aiassistant.data.source.RemoteConversationDataSource
import com.example.aiassistant.data.source.RemoteUserDataSource
import com.example.aiassistant.data.source.RoomLocalUserDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

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
    fun provideRemoteUserDataSource(): RemoteUserDataSource {
        return FakeRemoteUserDataSource()
    }

    @Provides
    @Singleton
    fun provideRemoteConversationDataSource(): RemoteConversationDataSource {
        return FakeRemoteConversationDataSource()
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
}
