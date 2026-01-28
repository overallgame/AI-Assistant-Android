package com.example.aiassistant.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserStateDao {

    @Query("SELECT * FROM user_state WHERE id = 0")
    fun observeUserState(): Flow<UserStateEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UserStateEntity)
}
