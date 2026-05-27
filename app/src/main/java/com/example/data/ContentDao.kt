package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ContentDao {
    @Query("SELECT * FROM content_schedules ORDER BY dateString ASC, timeString ASC")
    fun getAllSchedulesFlow(): Flow<List<ContentSchedule>>

    @Query("SELECT * FROM content_schedules WHERE id = :id")
    suspend fun getScheduleById(id: Int): ContentSchedule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ContentSchedule): Long

    @Update
    suspend fun updateSchedule(schedule: ContentSchedule)

    @Delete
    suspend fun deleteSchedule(schedule: ContentSchedule)

    @Query("SELECT * FROM sync_logs ORDER BY timestamp DESC LIMIT 50")
    fun getAllSyncLogsFlow(): Flow<List<SyncLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncLog(log: SyncLog): Long

    @Query("DELETE FROM sync_logs")
    suspend fun clearAllSyncLogs()
}
