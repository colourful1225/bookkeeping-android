package com.example.bookkeeping.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.bookkeeping.data.local.entity.SyncStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncStateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: SyncStateEntity)

    @Query("SELECT * FROM sync_state WHERE `key` = 'global' LIMIT 1")
    suspend fun get(): SyncStateEntity?

    @Query("SELECT * FROM sync_state WHERE `key` = 'global' LIMIT 1")
    fun observe(): Flow<SyncStateEntity?>
}
