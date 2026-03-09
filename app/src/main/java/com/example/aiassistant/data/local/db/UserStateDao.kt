package com.example.aiassistant.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface UserStateDao {

    @Query("SELECT * FROM user_state WHERE id = 0")
    fun observeUserState(): Flow<UserStateEntity?>

    @Query("SELECT * FROM user_state WHERE id = 0")
    suspend fun getUserState(): UserStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UserStateEntity)

    @Query("DELETE FROM user_state WHERE id = 0")
    suspend fun clearUserState()

    @Transaction
    @Query("SELECT * FROM user_state WHERE id = 0")
    suspend fun getUserStateInTransaction(): UserStateEntity?
}
