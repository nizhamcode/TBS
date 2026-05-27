package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_logs")
data class SyncLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val activity: String, // Google Calendar Sync, Google Task Creation, Drive Backup
    val status: String, // SUCCESS, FAILED
    val details: String
)
