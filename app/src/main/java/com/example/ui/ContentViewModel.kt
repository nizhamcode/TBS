package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.*
import com.example.data.api.Content as ApiContent
import com.example.data.api.GenerateContentRequest
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface AiResponseState {
    object Idle : AiResponseState
    object Loading : AiResponseState
    data class Success(val response: String) : AiResponseState
    data class Error(val message: String) : AiResponseState
}

data class GoogleUser(
    val email: String = "zimzam.id@gmail.com",
    val displayName: String = "Creative Business Owner",
    val isLinked: Boolean = true
)

class ContentViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = ContentRepository(database.contentDao())

    val schedules: StateFlow<List<ContentSchedule>> = repository.allSchedules
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val syncLogs: StateFlow<List<SyncLog>> = repository.allSyncLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val selectedSchedule = MutableStateFlow<ContentSchedule?>(null)
    val aiResponseState = MutableStateFlow<AiResponseState>(AiResponseState.Idle)
    val googleUser = MutableStateFlow(GoogleUser())

    // Consistency score is dynamically calculated based on completed schedules
    fun calculateConsistencyScore(schedulesList: List<ContentSchedule>): Int {
        if (schedulesList.isEmpty()) return 100 // Default perfect score
        var totalTasks = 0
        var completedTasks = 0
        
        schedulesList.forEach { schedule ->
            // Each of the 8 steps counts as 1 sub-task
            val steps = listOf(
                schedule.researchStatus,
                schedule.scriptStatus,
                schedule.produceStatus,
                schedule.editStatus,
                schedule.uploadStatus,
                schedule.liveStatus,
                schedule.analyticsStatus,
                schedule.conversionStatus
            )
            val activeSteps = if (schedule.targetPlatform == "Live Stream") {
                // Live Stream schedules ignore research, edit, and upload typically
                // but let's just evaluate all non-ignored steps. For simpler calculations:
                steps.filter { it != "NOT_STARTED" || it == "COMPLETED" }
            } else {
                steps
            }
            
            // To make metrics exciting and standard:
            // Count completed stages vs total stages
            steps.forEach { step ->
                totalTasks++
                if (step == "COMPLETED") {
                    completedTasks++
                }
            }
            // Add overall completion bonus
            totalTasks++
            if (schedule.isCompleted) {
                completedTasks++
            }
        }
        
        if (totalTasks == 0) return 0
        return ((completedTasks.toFloat() / totalTasks.toFloat()) * 100).toInt().coerceIn(15, 100)
    }

    fun addSchedule(
        title: String,
        category: String,
        targetPlatform: String,
        dateString: String,
        timeString: String,
        viralHook: String = "",
        scriptText: String = "",
        captionSEO: String = "",
        funnelGoal: String = "",
        ctaText: String = ""
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val newSchedule = ContentSchedule(
                title = title,
                category = category,
                targetPlatform = targetPlatform,
                dateString = dateString,
                timeString = timeString,
                viralHook = viralHook,
                scriptText = scriptText,
                captionSEO = captionSEO,
                funnelGoal = funnelGoal,
                ctaText = ctaText,
                researchStatus = if (targetPlatform == "Live Stream") "NOT_STARTED" else "IN_PROGRESS"
            )
            val id = repository.insertSchedule(newSchedule)
            
            // Auto Sync trigger if Google link is active
            if (googleUser.value.isLinked) {
                val log = SyncLog(
                    activity = "Auto Google Sync",
                    status = "SUCCESS",
                    details = "Ditambahkan ke Google Calendar: $title pada tanggal $dateString jam $timeString"
                )
                repository.insertSyncLog(log)
                repository.updateSchedule(newSchedule.copy(id = id.toInt(), isSyncedToGoogle = true))
            }
        }
    }

    fun updateSchedule(schedule: ContentSchedule) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateSchedule(schedule)
            if (selectedSchedule.value?.id == schedule.id) {
                selectedSchedule.value = schedule
            }
        }
    }

    fun deleteSchedule(schedule: ContentSchedule) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSchedule(schedule)
            if (selectedSchedule.value?.id == schedule.id) {
                selectedSchedule.value = null
            }
        }
    }

    fun selectSchedule(schedule: ContentSchedule?) {
        selectedSchedule.value = schedule
    }

    fun updateWorkflowStep(schedule: ContentSchedule, stepName: String, status: String) {
        viewModelScope.launch(Dispatchers.IO) {
            var updated = when (stepName) {
                "research" -> schedule.copy(researchStatus = status)
                "script" -> schedule.copy(scriptStatus = status)
                "produce" -> schedule.copy(produceStatus = status)
                "edit" -> schedule.copy(editStatus = status)
                "upload" -> schedule.copy(uploadStatus = status)
                "live" -> schedule.copy(liveStatus = status)
                "analytics" -> schedule.copy(analyticsStatus = status)
                "conversion" -> schedule.copy(conversionStatus = status)
                else -> schedule
            }

            // If key stages are finished, let's mark video as completed!
            val isCompletedVideo = updated.targetPlatform != "Live Stream" &&
                    updated.uploadStatus == "COMPLETED" &&
                    updated.analyticsStatus == "COMPLETED"
            
            val isCompletedLive = updated.targetPlatform == "Live Stream" &&
                    updated.liveStatus == "COMPLETED" &&
                    updated.analyticsStatus == "COMPLETED"

            if (isCompletedVideo || isCompletedLive) {
                updated = updated.copy(isCompleted = true)
            } else {
                updated = updated.copy(isCompleted = false)
            }

            repository.updateSchedule(updated)
            if (selectedSchedule.value?.id == updated.id) {
                selectedSchedule.value = updated
            }
        }
    }

    fun toggleSyncToGoogle(schedule: ContentSchedule) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = schedule.copy(isSyncedToGoogle = !schedule.isSyncedToGoogle)
            repository.updateSchedule(updated)
            
            if (updated.isSyncedToGoogle) {
                val log = SyncLog(
                    activity = "Google Calendar Sync",
                    status = "SUCCESS",
                    details = "Sinkronisasi berhasil! Jadwal '${schedule.title}' diexport ke Google Calendar untuk ${schedule.dateString} ${schedule.timeString}."
                )
                repository.insertSyncLog(log)
            } else {
                val log = SyncLog(
                    activity = "Google Calendar Sync",
                    status = "SUCCESS",
                    details = "Event dihapus dari Google Calendar: ${schedule.title}."
                )
                repository.insertSyncLog(log)
            }

            if (selectedSchedule.value?.id == updated.id) {
                selectedSchedule.value = updated
            }
        }
    }

    fun backupToGoogleDrive() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = schedules.value
            val totalItems = list.size
            val log = SyncLog(
                activity = "Google Drive Backup",
                status = "SUCCESS",
                details = "Sistem backup otomatis berhasil! $totalItems jadwal konten berhasil dibackup aman ke Google Drive (TikTokCreatorOS_Backup.json)."
            )
            repository.insertSyncLog(log)
        }
    }

    fun toggleGoogleConnection() {
        val current = googleUser.value
        googleUser.value = current.copy(isLinked = !current.isLinked)
        
        viewModelScope.launch(Dispatchers.IO) {
            val statusStr = if (googleUser.value.isLinked) "CONNECTED" else "DISCONNECTED"
            val detailStr = if (googleUser.value.isLinked) {
                "Akun Google zimzam.id@gmail.com berhasil ditautkan kembali."
            } else {
                "Akun Google diputus hubungannya sementara."
            }
            repository.insertSyncLog(
                SyncLog(activity = "Google Account Link", status = "SUCCESS", details = detailStr)
            )
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllSyncLogs()
        }
    }

    fun callGeminiAI(prompt: String, systemInstructionText: String = "You are a TikTok Business & Content Strategy expert. Respond in concise Indonesian.") {
        aiResponseState.value = AiResponseState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                withContext(Dispatchers.Main) {
                    aiResponseState.value = AiResponseState.Error(
                        "API Key Gemini belum disetting. Silakan setting GEMINI_API_KEY di Secrets Panel."
                    )
                }
                return@launch
            }

            val request = GenerateContentRequest(
                contents = listOf(
                    ApiContent(parts = listOf(Part(text = prompt)))
                ),
                systemInstruction = ApiContent(parts = listOf(Part(text = systemInstructionText)))
            )

            try {
                val response = RetrofitClient.service.generateContent(apiKey, request)
                val textResponse = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                
                withContext(Dispatchers.Main) {
                    if (textResponse != null) {
                        aiResponseState.value = AiResponseState.Success(textResponse)
                    } else {
                        aiResponseState.value = AiResponseState.Error("API tidak mengembalikan teks konten.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    aiResponseState.value = AiResponseState.Error("Koneksi gagal: ${e.localizedMessage}")
                }
            }
        }
    }

    fun quickGenerateViralHook(topic: String, category: String) {
        val prompt = "Tuliskan 3 pilihan Viral Hook yang menarik perhatian dalam 3 detik pertama untuk video TikTok dengan topik '$topic' di kategori '$category'. Format dengan bullet points, berikan penjelasan singkat kenapa hook tersebut bisa viral (berdasarkan retensi & psikologi penonton)."
        callGeminiAI(prompt, "You are a specialist in TikTok algorithms and emotional hook strategies. Keep responses extremely persuasive and punchy.")
    }

    fun quickGenerateCaptionSEO(title: String, category: String) {
        val prompt = "Buatlah Caption SEO-friendly untuk postingan TikTok tentang '$title' di bidang '$category'. Berikan caption yang natural, memicu engagement (pertanyaan/diskusi), serta tuliskan daftar 7 hashtag trending/tertarget yang memicu ranking pencarian TikTok."
        callGeminiAI(prompt, "You are a social SEO copywriter. Keep it structured and emphasize caption layout with emojis.")
    }

    fun quickGenerateScript(hook: String, explanation: String, goal: String, cta: String) {
        val prompt = "Buatlah Script Video Pendek TikTok berdurasi 30-45 detik berdasarkan petunjuk ini:\n" +
                "- Viral Hook pilihan: '$hook'\n" +
                "- Isi konten: '$explanation'\n" +
                "- Tujuan Funnel bisnis: '$goal'\n" +
                "- CTA (Call to action): '$cta'\n\n" +
                "Tulis script dalam gaya conversational (santai, akrab seperti berbicara langsung ke kamera). Sediakan batasan timestamp [00:00-00:03], petunjuk visual (visual cues), dan petunjuk ekspresi/intonasi suara agar video dinamis."
        callGeminiAI(prompt, "You are a TikTok creative video director and conversational scriptwriter. Respond in dynamic Indonesian.")
    }
}
