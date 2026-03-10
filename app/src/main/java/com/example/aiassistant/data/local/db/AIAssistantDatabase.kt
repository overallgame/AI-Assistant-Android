package com.example.aiassistant.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.RoomDatabase.JournalMode

@Database(
    entities = [UserStateEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AIAssistantDatabase : RoomDatabase() {

    abstract fun userStateDao(): UserStateDao

    companion object {
        private const val DATABASE_NAME = "ai_assistant.db"

        @Volatile
        private var INSTANCE: AIAssistantDatabase? = null

        fun getInstance(context: Context): AIAssistantDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AIAssistantDatabase {
            return Room.databaseBuilder(
                context,
                AIAssistantDatabase::class.java,
                DATABASE_NAME,
            )
                .fallbackToDestructiveMigration()
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .build()
        }

        fun destroyInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
