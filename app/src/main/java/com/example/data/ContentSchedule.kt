package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "content_schedules")
data class ContentSchedule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val category: String, // UMKM, Affiliate, Personal Branding, Jasa, Edukasi, dll.
    val targetPlatform: String, // Video, Live Stream
    val dateString: String, // e.g. 2026-05-27
    val timeString: String, // e.g. 19:00
    val viralHook: String = "",
    val scriptText: String = "",
    val captionSEO: String = "",
    val funnelGoal: String = "", // Awareness, Engagement, Conversion, Community
    val ctaText: String = "",
    val isSyncedToGoogle: Boolean = false,
    val isCompleted: Boolean = false,
    
    // Workflow status metrics
    val researchStatus: String = "NOT_STARTED", // NOT_STARTED, IN_PROGRESS, COMPLETED
    val scriptStatus: String = "NOT_STARTED",
    val produceStatus: String = "NOT_STARTED",
    val editStatus: String = "NOT_STARTED",
    val uploadStatus: String = "NOT_STARTED",
    val liveStatus: String = "NOT_STARTED",
    val analyticsStatus: String = "NOT_STARTED",
    val conversionStatus: String = "NOT_STARTED",
    
    val timestamp: Long = System.currentTimeMillis()
)
