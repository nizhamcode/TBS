package com.example.data

import kotlinx.coroutines.flow.Flow

class ContentRepository(private val contentDao: ContentDao) {
    val allSchedules: Flow<List<ContentSchedule>> = contentDao.getAllSchedulesFlow()
    val allSyncLogs: Flow<List<SyncLog>> = contentDao.getAllSyncLogsFlow()

    suspend fun getScheduleById(id: Int): ContentSchedule? {
        return contentDao.getScheduleById(id)
    }

    suspend fun insertSchedule(schedule: ContentSchedule): Long {
        return contentDao.insertSchedule(schedule)
    }

    suspend fun updateSchedule(schedule: ContentSchedule) {
        contentDao.updateSchedule(schedule)
    }

    suspend fun deleteSchedule(schedule: ContentSchedule) {
        contentDao.deleteSchedule(schedule)
    }

    suspend fun insertSyncLog(log: SyncLog): Long {
        return contentDao.insertSyncLog(log)
    }

    suspend fun clearAllSyncLogs() {
        contentDao.clearAllSyncLogs()
    }
}
