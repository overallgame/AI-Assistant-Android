package com.example.aiassistant.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [UserStateEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AIAssistantDatabase : RoomDatabase() {

    abstract fun userStateDao(): UserStateDao

    companion object {
        @Volatile
        private var INSTANCE: AIAssistantDatabase? = null

        fun getInstance(context: Context): AIAssistantDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AIAssistantDatabase::class.java,
                    "ai_assistant.db",
                ).build().also { INSTANCE = it }
            }
        }
    }
}
