package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ContentSchedule
import com.example.data.SyncLog
import java.text.SimpleDateFormat
import java.util.*

// Style Color Tokens matching TikTok signature premium palette
val CarbonDark = Color(0xFF0C0E14)      // Super sleek background
val SlateGray = Color(0xFF141724)       // Container background
val NeonCyan = Color(0xFF00F2FE)        // Electric Cyan branding highlight
val HotPink = Color(0xFFFF0050)         // TikTok pink branding accent
val WarmWhite = Color(0xFFF0F2FA)       // High contrast text
val CoolGray = Color(0xFF8F9BB3)        // Muted labels
val DarkBorder = Color(0xFF282F44)      // Clean borders
val EmeraldAccent = Color(0xFF00E676)   // Completion green

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreatorUi(viewModel: ContentViewModel) {
    val schedules by viewModel.schedules.collectAsStateWithLifecycle()
    val syncLogs by viewModel.syncLogs.collectAsStateWithLifecycle()
    val selectedSchedule by viewModel.selectedSchedule.collectAsStateWithLifecycle()
    val aiResponseState by viewModel.aiResponseState.collectAsStateWithLifecycle()
    val googleUser by viewModel.googleUser.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf("home") } // "home", "ai", "templates", "analytics", "google"
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = CarbonDark,
        bottomBar = {
            CreatorBottomNavigation(activeTab = activeTab, onTabSelected = { activeTab = it })
        },
        floatingActionButton = {
            if (activeTab == "home" && selectedSchedule == null) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = HotPink,
                    contentColor = WarmWhite,
                    modifier = Modifier
                        .testTag("add_schedule_fab")
                        .padding(bottom = 12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah Jadwal Konten")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main views transition matching screen activeTab
            Crossfade(targetState = activeTab, animationSpec = tween(250), label = "ScreenTransition") { tab ->
                when (tab) {
                    "home" -> {
                        if (selectedSchedule != null) {
                            ScheduleDetailScreen(
                                schedule = selectedSchedule!!,
                                onBack = { viewModel.selectSchedule(null) },
                                onUpdateStep = { step, status ->
                                    viewModel.updateWorkflowStep(selectedSchedule!!, step, status)
                                },
                                onDelete = {
                                    viewModel.deleteSchedule(selectedSchedule!!)
                                },
                                onToggleSync = {
                                    viewModel.toggleSyncToGoogle(selectedSchedule!!)
                                },
                                onEditValues = { updated ->
                                    viewModel.updateSchedule(updated)
                                }
                            )
                        } else {
                            HomeScreen(
                                schedules = schedules,
                                onSelect = { viewModel.selectSchedule(it) },
                                consistencyScore = viewModel.calculateConsistencyScore(schedules)
                            )
                        }
                    }
                    "ai" -> {
                        AiAssistantScreen(
                            aiState = aiResponseState,
                            onGenerateHook = { topic, cat -> viewModel.quickGenerateViralHook(topic, cat) },
                            onGenerateCaption = { title, cat -> viewModel.quickGenerateCaptionSEO(title, cat) },
                            onGenerateScript = { h, exp, goal, c -> viewModel.quickGenerateScript(h, exp, goal, c) },
                            onClearState = { viewModel.aiResponseState.value = AiResponseState.Idle },
                            onCustomPrompt = { prompt -> viewModel.callGeminiAI(prompt) },
                            onSaveAsSchedule = { title, cat, hook, script ->
                                viewModel.addSchedule(
                                    title = title,
                                    category = cat,
                                    targetPlatform = "Video",
                                    dateString = getTodayDateString(),
                                    timeString = "19:00",
                                    viralHook = hook,
                                    scriptText = script,
                                    funnelGoal = "Awareness",
                                    ctaText = "Klik link di bio!"
                                )
                                activeTab = "home"
                            }
                        )
                    }
                    "templates" -> {
                        FrameworkLibraryScreen()
                    }
                    "analytics" -> {
                        AnalyticsReportScreen(schedules = schedules, score = viewModel.calculateConsistencyScore(schedules))
                    }
                    "google" -> {
                        GoogleSyncScreen(
                            user = googleUser,
                            logs = syncLogs,
                            onToggleLink = { viewModel.toggleGoogleConnection() },
                            onBackup = { viewModel.backupToGoogleDrive() },
                            onClearLogs = { viewModel.clearAllLogs() }
                        )
                    }
                }
            }

            if (showAddDialog) {
                AddScheduleDialog(
                    onDismiss = { showAddDialog = false },
                    onSave = { title, category, platform, date, time, hook, script, goal, cvText ->
                        viewModel.addSchedule(
                            title = title,
                            category = category,
                            targetPlatform = platform,
                            dateString = date,
                            timeString = time,
                            viralHook = hook,
                            scriptText = script,
                            funnelGoal = goal,
                            ctaText = cvText
                        )
                        showAddDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun CreatorBottomNavigation(activeTab: String, onTabSelected: (String) -> Unit) {
    NavigationBar(
        containerColor = SlateGray,
        tonalElevation = 8.dp,
        windowInsets = WindowInsets.navigationBars,
        modifier = Modifier.testTag("bottom_nav_bar")
    ) {
        val navItems = listOf(
            Triple("home", "Jadwal", Icons.Default.CalendarToday),
            Triple("ai", "Sistem AI", Icons.Default.AutoAwesome),
            Triple("templates", "Formula", Icons.Default.ImportContacts),
            Triple("analytics", "Analytics", Icons.Default.BarChart),
            Triple("google", "Google Sync", Icons.Default.Sync)
        )

        navItems.forEach { item ->
            val route = item.first
            val label = item.second
            val icon = item.third
            val isSelected = activeTab == route
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(route) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) NeonCyan else CoolGray
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) WarmWhite else CoolGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color(0xFF22293F),
                )
            )
        }
    }
}

@Composable
fun HomeScreen(
    schedules: List<ContentSchedule>,
    onSelect: (ContentSchedule) -> Unit,
    consistencyScore: Int
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonDark)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TikTok Creator OS 2026",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = WarmWhite,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = "Consistency Beats Motivation.",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        fontFamily = FontFamily.SansSerif
                    )
                }
                
                Box(
                    modifier = Modifier
                        .background(SlateGray, CircleShape)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(EmeraldAccent, CircleShape)
                        )
                        Text(
                            text = "ACTIVE",
                            color = EmeraldAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        // Executive circular progress consistency widget
        item {
            ConsistencyScoreCard(score = consistencyScore)
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "Timeline Jadwal Konten",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = WarmWhite
                )
                Text(
                    text = "${schedules.size} Agenda",
                    fontSize = 12.sp,
                    color = CoolGray
                )
            }
        }

        if (schedules.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp)
                        .background(SlateGray, RoundedCornerShape(16.dp))
                        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventNote,
                            contentDescription = "Empty Schedule",
                            tint = CoolGray,
                            modifier = Modifier.size(54.dp)
                        )
                        Text(
                            text = "Belum Ada Jadwal Konten",
                            color = WarmWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Mulai bangun sistem konsistensi konten Anda dengan menekan tombol (+) di kanan bawah.",
                            color = CoolGray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(schedules) { schedule ->
                ScheduleCard(schedule = schedule, onClick = { onSelect(schedule) })
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
fun ConsistencyScoreCard(score: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateGray),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                border = BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(listOf(NeonCyan.copy(alpha = 0.4f), HotPink.copy(alpha = 0.4f)))
                ),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Radial Gauge via custom draw behind
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .drawBehind {
                        // Background track
                        drawArc(
                            color = Color(0xFF1E2235),
                            startAngle = -220f,
                            sweepAngle = 260f,
                            useCenter = false,
                            style = Stroke(width = 24f, cap = StrokeCap.Round)
                        )
                        // Progress arc
                        val sweep = (score.toFloat() / 100f) * 260f
                        drawArc(
                            brush = Brush.sweepGradient(listOf(HotPink, NeonCyan, HotPink)),
                            startAngle = -220f,
                            sweepAngle = sweep,
                            useCenter = false,
                            style = Stroke(width = 24f, cap = StrokeCap.Round)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$score%",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = WarmWhite
                    )
                    Text(
                        text = "Score",
                        fontSize = 9.sp,
                        color = CoolGray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Productivity Consistency",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = WarmWhite
                )
                Text(
                    text = "Rating konsistensi sistem konten TikTok 2026 Anda berdasarkan progres harian yang terekam.",
                    fontSize = 11.sp,
                    color = CoolGray,
                    lineHeight = 14.sp
                )
                
                val levelLabel = when {
                    score >= 85 -> "Sangat Konsisten! ✨"
                    score >= 60 -> "Konsistensi Baik 👍"
                    else -> "Butuh Perbaikan ⚠️"
                }
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .background(Color(0xFF1E2235), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Status: $levelLabel",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (score >= 60) EmeraldAccent else HotPink
                    )
                }
            }
        }
    }
}

@Composable
fun ScheduleCard(schedule: ContentSchedule, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateGray),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(1.dp, if (schedule.isCompleted) EmeraldAccent.copy(alpha = 0.3f) else DarkBorder, RoundedCornerShape(16.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    if (schedule.targetPlatform == "Live Stream") HotPink.copy(alpha = 0.15f) else NeonCyan.copy(alpha = 0.15f),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = schedule.targetPlatform.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (schedule.targetPlatform == "Live Stream") HotPink else NeonCyan
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(Color(0xFF22293F), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = schedule.category,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarmWhite
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = schedule.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = WarmWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = schedule.dateString,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoolGray
                    )
                    Text(
                        text = " pukul ${schedule.timeString}",
                        fontSize = 11.sp,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(color = DarkBorder, thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Step completion metrics indicator
                val metrics = calculateCompletedSteps(schedule)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = "Funnel Goal",
                        tint = CoolGray,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Funnel: ${schedule.funnelGoal.ifEmpty { "General" }}",
                        fontSize = 11.sp,
                        color = CoolGray
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Workflow: ${metrics.first}/${metrics.second}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (schedule.isCompleted) EmeraldAccent else NeonCyan
                    )

                    if (schedule.isSyncedToGoogle) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Synced to Google",
                            tint = EmeraldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (schedule.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Tuntas",
                            tint = EmeraldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayCircleOutline,
                            contentDescription = "Belum Tuntas",
                            tint = CoolGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// Counts workflow progress stages depending on the target format (short video vs stream)
fun calculateCompletedSteps(schedule: ContentSchedule): Pair<Int, Int> {
    val steps = if (schedule.targetPlatform == "Live Stream") {
        listOf(
            schedule.researchStatus,
            schedule.scriptStatus,
            schedule.liveStatus,
            schedule.analyticsStatus,
            schedule.conversionStatus
        )
    } else {
        listOf(
            schedule.researchStatus,
            schedule.scriptStatus,
            schedule.produceStatus,
            schedule.editStatus,
            schedule.uploadStatus,
            schedule.analyticsStatus,
            schedule.conversionStatus
        )
    }
    val completedCount = steps.count { it == "COMPLETED" }
    return Pair(completedCount, steps.size)
}

@Suppress("UnrememberedMutableState")
@Composable
fun ScheduleDetailScreen(
    schedule: ContentSchedule,
    onBack: () -> Unit,
    onUpdateStep: (String, String) -> Unit,
    onDelete: () -> Unit,
    onToggleSync: () -> Unit,
    onEditValues: (ContentSchedule) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var titleInput by remember { mutableStateOf(schedule.title) }
    var hookInput by remember { mutableStateOf(schedule.viralHook) }
    var scriptInput by remember { mutableStateOf(schedule.scriptText) }
    var captionInput by remember { mutableStateOf(schedule.captionSEO) }
    var ctaInput by remember { mutableStateOf(schedule.ctaText) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.background(SlateGray, CircleShape)
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Kembali", tint = WarmWhite)
            }

            Text(
                text = "Detail Rencana",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = WarmWhite
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = {
                        if (isEditing) {
                            onEditValues(
                                schedule.copy(
                                    title = titleInput,
                                    viralHook = hookInput,
                                    scriptText = scriptInput,
                                    captionSEO = captionInput,
                                    ctaText = ctaInput
                                )
                            )
                            isEditing = false
                        } else {
                            isEditing = true
                        }
                    },
                    modifier = Modifier.background(SlateGray, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isEditing) Icons.Default.Save else Icons.Default.Edit,
                        contentDescription = "Edit Konten",
                        tint = if (isEditing) EmeraldAccent else WarmWhite
                    )
                }

                IconButton(
                    onClick = {
                        onDelete()
                        onBack()
                    },
                    modifier = Modifier.background(SlateGray, CircleShape)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Hapus", tint = HotPink)
                }
            }
        }

        // Heading Header Details
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateGray),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isEditing) {
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("Judul Konten/Video") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = WarmWhite,
                            unfocusedTextColor = WarmWhite,
                            focusedLabelColor = NeonCyan
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = schedule.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = WarmWhite
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(NeonCyan.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(text = schedule.targetPlatform.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                    }

                    Box(
                        modifier = Modifier
                            .background(Color(0xFF22293F), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(text = schedule.category, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = WarmWhite)
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                if (schedule.isCompleted) EmeraldAccent.copy(alpha = 0.15f) else Color.Yellow.copy(alpha = 0.15f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (schedule.isCompleted) "COMPLETED" else "IN PROGRESS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (schedule.isCompleted) EmeraldAccent else Color.Yellow
                        )
                    }
                }

                HorizontalDivider(color = DarkBorder, thickness = 1.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Rencana Publikasi", fontSize = 11.sp, color = CoolGray)
                        Text(text = "${schedule.dateString} / Pukul ${schedule.timeString}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WarmWhite)
                    }

                    Button(
                        onClick = onToggleSync,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (schedule.isSyncedToGoogle) EmeraldAccent.copy(alpha = 0.2f) else HotPink,
                            contentColor = WarmWhite
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = if (schedule.isSyncedToGoogle) Icons.Default.SyncAlt else Icons.Default.Sync,
                                contentDescription = "",
                                modifier = Modifier.size(16.dp),
                                tint = WarmWhite
                            )
                            Text(
                                text = if (schedule.isSyncedToGoogle) "Google Cal Synced" else "Sync Google Cal",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Creator Daily Workflow Interactive steps
        Text(
            text = "📋 Workflow Operasional Kreator",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = WarmWhite
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = SlateGray),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Ketuk ikon status untuk melacak progres tahapan kerja pembuatan konten harian Anda.",
                    fontSize = 11.sp,
                    color = CoolGray,
                    style = MaterialTheme.typography.bodySmall
                )
                
                Spacer(modifier = Modifier.height(4.dp))

                if (schedule.targetPlatform == "Live Stream") {
                    // Live specific steps
                    WorkflowStepRow("Tren Riset & Strategi", schedule.researchStatus) { next -> onUpdateStep("research", next) }
                    WorkflowStepRow("Scripting Live Penawaran", schedule.scriptStatus) { next -> onUpdateStep("script", next) }
                    WorkflowStepRow("Siaran Live Streaming", schedule.liveStatus) { next -> onUpdateStep("live", next) }
                    WorkflowStepRow("Statistik Live Analytics", schedule.analyticsStatus) { next -> onUpdateStep("analytics", next) }
                    WorkflowStepRow("Konversi & Funnel Penjualan", schedule.conversionStatus) { next -> onUpdateStep("conversion", next) }
                } else {
                    // Video specific steps
                    WorkflowStepRow("Tren & Riset Ide", schedule.researchStatus) { next -> onUpdateStep("research", next) }
                    WorkflowStepRow("Script Skenario & Hooks", schedule.scriptStatus) { next -> onUpdateStep("script", next) }
                    WorkflowStepRow("Fase Produksi (Shooting)", schedule.produceStatus) { next -> onUpdateStep("produce", next) }
                    WorkflowStepRow("Editing & Efek Suplemen", schedule.editStatus) { next -> onUpdateStep("edit", next) }
                    WorkflowStepRow("Upload TikTok & SEO Caption", schedule.uploadStatus) { next -> onUpdateStep("upload", next) }
                    WorkflowStepRow("Review Analytics Kunci", schedule.analyticsStatus) { next -> onUpdateStep("analytics", next) }
                    WorkflowStepRow("Hasil Konversi Funnel Bisnis", schedule.conversionStatus) { next -> onUpdateStep("conversion", next) }
                }
            }
        }

        // Copywriting and script details section
        Text(
            text = "🎬 Copywriting & Script Editor",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = WarmWhite
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = SlateGray),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Funnel Goal
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Tujuan Funnel Bisnis", fontSize = 11.sp, color = CoolGray)
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF22293F), RoundedCornerShape(6.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(text = schedule.funnelGoal.ifEmpty { "General Awareness" }, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                    }
                }

                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)

                // Viral Hook
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Viral Hook (Detik 0-3)", fontSize = 11.sp, color = CoolGray)
                    if (isEditing) {
                        OutlinedTextField(
                            value = hookInput,
                            onValueChange = { hookInput = it },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = WarmWhite,
                                unfocusedTextColor = WarmWhite
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = schedule.viralHook.ifEmpty { "Belum menulis hook konten." },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (schedule.viralHook.isNotEmpty()) WarmWhite else CoolGray,
                            lineHeight = 18.sp
                        )
                    }
                }

                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)

                // Script / Outline Text
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Naskah Script Konten Lengkap", fontSize = 11.sp, color = CoolGray)
                    if (isEditing) {
                        OutlinedTextField(
                            value = scriptInput,
                            onValueChange = { scriptInput = it },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = WarmWhite,
                                unfocusedTextColor = WarmWhite
                            ),
                            maxLines = 10,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = schedule.scriptText.ifEmpty { "Belum menulis script detail." },
                            fontSize = 13.sp,
                            color = if (schedule.scriptText.isNotEmpty()) WarmWhite else CoolGray,
                            lineHeight = 18.sp
                        )
                    }
                }

                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)

                // Custom Caption & Call-To-Action (CTA)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "SEO Caption & HashTags", fontSize = 11.sp, color = CoolGray)
                    }
                    if (isEditing) {
                        OutlinedTextField(
                            value = captionInput,
                            onValueChange = { captionInput = it },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = WarmWhite,
                                unfocusedTextColor = WarmWhite
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = schedule.captionSEO.ifEmpty { "#foryoupage #creator #business" },
                            fontSize = 13.sp,
                            color = WarmWhite,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }

                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)

                // Call to action
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Target Call To Action (CTA)", fontSize = 11.sp, color = CoolGray)
                    if (isEditing) {
                        OutlinedTextField(
                            value = ctaInput,
                            onValueChange = { ctaInput = it },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = WarmWhite,
                                unfocusedTextColor = WarmWhite
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = schedule.ctaText.ifEmpty { "Menggiring penonton ke link di bio atau keranjang kuning." },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = HotPink
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

// Custom step status row with click actions
@Composable
fun WorkflowStepRow(label: String, status: String, onStatusChanged: (String) -> Unit) {
    val statusColors = when (status) {
        "NOT_STARTED" -> Triple(Color(0xFF22293F), CoolGray, "Mulai ➔")
        "IN_PROGRESS" -> Triple(Color(0xFF333010), Color.Yellow, "Proses ➔")
        "COMPLETED" -> Triple(EmeraldAccent.copy(alpha = 0.15f), EmeraldAccent, "Selesai ✓")
        else -> Triple(Color(0xFF22293F), CoolGray, "Mulai")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF161824), RoundedCornerShape(8.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(statusColors.second, CircleShape)
            )
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = WarmWhite
            )
        }

        // Tap workflow state toggles
        Box(
            modifier = Modifier
                .background(statusColors.first, RoundedCornerShape(6.dp))
                .clickable {
                    val nextStatus = when (status) {
                        "NOT_STARTED" -> "IN_PROGRESS"
                        "IN_PROGRESS" -> "COMPLETED"
                        "COMPLETED" -> "NOT_STARTED"
                        else -> "NOT_STARTED"
                    }
                    onStatusChanged(nextStatus)
                }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = statusColors.third,
                color = statusColors.second,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// AI Content generation Tab
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiAssistantScreen(
    aiState: AiResponseState,
    onGenerateHook: (String, String) -> Unit,
    onGenerateCaption: (String, String) -> Unit,
    onGenerateScript: (String, String, String, String) -> Unit,
    onClearState: () -> Unit,
    onCustomPrompt: (String) -> Unit,
    onSaveAsSchedule: (String, String, String, String) -> Unit
) {
    var promptTopic by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("UMKM & Jasa") }
    var activeMode by remember { mutableStateOf("hook") } // "hook", "caption", "script"

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val categories = listOf("UMKM & Jasa", "Affiliate Marketing", "Personal Branding", "E-Commerce", "Edukasi & Jasa", "AI Creator")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonDark)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "🤖 AI Content Assistant",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = WarmWhite
                )
                Text(
                    text = "Didukung oleh model AI Gemini 3.5 Flash",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = NeonCyan
                )
            }
        }

        // Quick Command selection Tabs
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateGray),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val modes = listOf("hook" to "Viral Hook", "caption" to "Caption/SEO", "script" to "Script Video")
                    modes.forEach { (mCode, label) ->
                        val isSel = activeMode == mCode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isSel) Color(0xFF22293F) else Color.Transparent,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    activeMode = mCode
                                    onClearState()
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) NeonCyan else CoolGray
                            )
                        }
                    }
                }
            }
        }

        // Form Inputs
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateGray),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Category list grid
                    Text(text = "Kategori Bisnis / Niche", fontSize = 11.sp, color = CoolGray)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSel = selectedCategory == cat
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSel) NeonCyan.copy(alpha = 0.15f) else Color(0xFF22293F),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSel) NeonCyan else Color.Transparent,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { selectedCategory = cat }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) NeonCyan else WarmWhite
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Topic Input
                    OutlinedTextField(
                        value = promptTopic,
                        onValueChange = { promptTopic = it },
                        label = {
                            Text(
                                if (activeMode == "hook") "Topik/Ide konten spesifik Anda (Misal: Sepatu anti slip)"
                                else if (activeMode == "caption") "Judul postingan materi Anda"
                                else "Isi gagasan konten & CTA"
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = WarmWhite,
                            unfocusedTextColor = WarmWhite,
                            focusedLabelColor = NeonCyan,
                            unfocusedLabelColor = CoolGray
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("ai_topic_input")
                    )

                    Button(
                        onClick = {
                            if (promptTopic.isNotBlank()) {
                                if (activeMode == "hook") {
                                    onGenerateHook(promptTopic, selectedCategory)
                                } else if (activeMode == "caption") {
                                    onGenerateCaption(promptTopic, selectedCategory)
                                } else {
                                    // Script Video Mode
                                    onGenerateScript(
                                        "Ingat Hook Penasaran Ini!",
                                        promptTopic,
                                        "Awareness & Direct Promo",
                                        "Klik link kuning sekarang!"
                                    )
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HotPink),
                        modifier = Modifier.fillMaxWidth().testTag("ai_generate_btn"),
                        shape = RoundedCornerShape(10.dp),
                        enabled = promptTopic.isNotBlank() && aiState !is AiResponseState.Loading
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "")
                            Text(
                                text = if (aiState is AiResponseState.Loading) "AI Sedang Berpikir..." else "Hasilkan Formula AI",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Output Result presentation
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🚀 Formula AI Hasil Optimasi",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoolGray
                    )

                    if (aiState is AiResponseState.Success) {
                        IconButton(onClick = { onClearState() }) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Clear", tint = CoolGray)
                        }
                    }
                }

                when (aiState) {
                    is AiResponseState.Idle -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SlateGray, RoundedCornerShape(16.dp))
                                .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                                .padding(30.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Lengkapi form topik di atas, klik tombol naksir untuk meluncurkan asisten AI kreatif TikTok.",
                                fontSize = 12.sp,
                                color = CoolGray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    is AiResponseState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SlateGray, RoundedCornerShape(16.dp))
                                .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(color = NeonCyan)
                                Text(
                                    text = "Gemini AI sedang memproses naskah terbaik...",
                                    color = WarmWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    is AiResponseState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF2D1418), RoundedCornerShape(16.dp))
                                .border(1.dp, HotPink.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Gagal memproses: ${aiState.message}",
                                color = Color.Red,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    is AiResponseState.Success -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF161824)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(16.dp)
                                )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = aiState.response,
                                    fontSize = 13.sp,
                                    color = WarmWhite,
                                    lineHeight = 20.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.testTag("ai_response_text")
                                )

                                HorizontalDivider(color = DarkBorder, thickness = 1.dp)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Action to copy
                                    Button(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(aiState.response))
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22293F)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "", modifier = Modifier.size(16.dp), tint = NeonCyan)
                                            Text(text = "Salin Teks", fontSize = 12.sp, color = WarmWhite)
                                        }
                                    }

                                    // Direct save to schedule content OS!
                                    Button(
                                        onClick = {
                                            onSaveAsSchedule(
                                                promptTopic.ifEmpty { "Optimasi AI: $selectedCategory" },
                                                selectedCategory,
                                                if (activeMode == "hook") aiState.response.take(150) else "Formula AI terpilih",
                                                aiState.response
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1.2f)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Save, contentDescription = "", modifier = Modifier.size(16.dp), tint = CarbonDark)
                                            Text(text = "Masuk Jadwal", fontSize = 12.sp, color = CarbonDark, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// Built-in static Frameworks Playbook cheatsheet
@Composable
fun FrameworkLibraryScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "📚 TikTok Creator Playbook",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = WarmWhite
            )
            Text(
                text = "Sistem Formula Konten Algoritma TikTok Terbaru",
                fontSize = 13.sp,
                color = CoolGray
            )
        }

        // Framework section 1: Viral Hooks
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateGray),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Whatshot, contentDescription = "", tint = HotPink)
                    Text(text = "🔥 Hook Viral Terbukti (Fase 3 Detik)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = WarmWhite)
                }
                HorizontalDivider(color = DarkBorder)
                
                HookTemplateItem(
                    hookType = "Self-Correction (Koreksi Diri)",
                    template = "“Saya nyesel banget baru tahu hacks ini setelah buang-buang uang jutaan...”",
                    impact = "Retensi tinggi karena memicu empati dan rasa ingin tahu yang ekstrem."
                )
                HookTemplateItem(
                    hookType = "Fakta Kontradiktif",
                    template = "“Stop bikin konten setiap hari kalau mau dagangan kalian laku keras...”",
                    impact = "Mematahkan asumsi membuat penonton berhenti scrolling (Pattern Interrupt)."
                )
                HookTemplateItem(
                    hookType = "Rahasia Industri",
                    template = "“3 Trik curang dari kompetitor yang mereka nggak mau kalian tahu sekarang...”",
                    impact = "Menggiring penonton karena merasa mendapatkan nilai rahasia eksklusif gratis."
                )
            }
        }

        // Framework section 2: Storytelling formulas
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateGray),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.RecordVoiceOver, contentDescription = "", tint = NeonCyan)
                    Text(text = "✍️ Formula Storytelling Naratif", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = WarmWhite)
                }
                HorizontalDivider(color = DarkBorder)

                StorytellingFormulaItem(
                    title = "PAS: Problem ➔ Agitate ➔ Solve",
                    desc = "Gali masalah terbesar penonton, buat situasi makin gawat secara emosional, lalu tawarkan solusi produk Anda.",
                    example = "“Sering sepi penonton? Algoritma 2026 kejam banget kalau pemula asal posting. Ini yang bikin video Anda hancur. Solusinya, ikuti 3 pola scheduler di aplikasi TikTok Creator OS ini.”"
                )

                StorytellingFormulaItem(
                    title = "BAB: Before ➔ After ➔ Bridge",
                    desc = "Gambarkan kondisi menyedihkan masa lalu, tunjukkan pencapaian indah saat ini, lalu berikan jembatan rahasia kesuksesannya.",
                    example = "“Dulu jualan online zong sebulan penuh. Sekarang bisa packing 100 paket per hari. Cuma beralih pakai kalender workflow yang terjadwal.”"
                )
            }
        }

        // Live streaming blueprint
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateGray),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.LiveTv, contentDescription = "", tint = EmeraldAccent)
                    Text(text = "🔴 Live Streaming Funnel Loops", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = WarmWhite)
                }
                HorizontalDivider(color = DarkBorder)

                Text(
                    text = "Gunakan strategi berulang ini agar penonton bertahan (retention) dan algoritma terus menyebarkan room live Anda:",
                    fontSize = 12.sp,
                    color = CoolGray,
                    lineHeight = 16.sp
                )

                BulletItem("Loop Menit 0-5: Sapaan interaktif, janjikan bagi-bagi koin/voucher rahasia di menit ke-10.")
                BulletItem("Loop Menit 5-15: Sampaikan tips bisnis gratis berbobot tinggi (buat penonton screenshot).")
                BulletItem("Loop Menit 15-25: Pitching keranjang kuning, sebut 'stok voucher sisa 3 orang lagi!' untuk memicu urgensi belanja.")
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun HookTemplateItem(hookType: String, template: String, impact: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = hookType, fontSize = 11.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
        Text(text = template, fontSize = 13.sp, fontWeight = FontWeight.Black, color = WarmWhite, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
        Text(text = "Psikologi: $impact", fontSize = 10.sp, color = CoolGray)
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun StorytellingFormulaItem(title: String, desc: String, example: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = title, fontSize = 13.sp, color = WarmWhite, fontWeight = FontWeight.Bold)
        Text(text = desc, fontSize = 11.sp, color = CoolGray, lineHeight = 14.sp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161824), RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            Text(text = "Contoh: $example", fontSize = 11.sp, color = WarmWhite, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun BulletItem(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = "•", color = EmeraldAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(text = text, fontSize = 12.sp, color = WarmWhite, lineHeight = 16.sp)
    }
}

// Graphical Analytics Screen with Canvas-drawn Trend Charts
@Composable
fun AnalyticsReportScreen(schedules: List<ContentSchedule>, score: Int) {
    val completedSchedules = schedules.filter { it.isCompleted }
    val pendingSchedules = schedules.filter { !it.isCompleted }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "📈 Growth & Productivity Analytics",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = WarmWhite
            )
            Text(
                text = "Monitoring sistem pertumbuhan atensi audiens Anda.",
                fontSize = 13.sp,
                color = CoolGray
            )
        }

        // Metrics Grid Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateGray),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = "Agenda Dibuat", fontSize = 11.sp, color = CoolGray)
                    Text(text = "${schedules.size}", fontSize = 28.sp, fontWeight = FontWeight.Black, color = WarmWhite)
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = SlateGray),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, EmeraldAccent.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = "Agenda Selesai", fontSize = 11.sp, color = CoolGray)
                    Text(text = "${completedSchedules.size}", fontSize = 28.sp, fontWeight = FontWeight.Black, color = EmeraldAccent)
                }
            }
        }

        // Attention Economics Canvas Chart
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateGray),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Prediksi Tren Atensi & Konsistensi (2026)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = WarmWhite
                )

                // Canvas line graph draw
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    val width = size.width
                    val height = size.height

                    // Grid lines
                    val gridLines = 4
                    for (i in 0..gridLines) {
                        val y = (height / gridLines) * i
                        drawLine(
                            color = DarkBorder.copy(alpha = 0.5f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 2f
                        )
                    }

                    // Simulated data points: score history
                    val points = listOf(
                        Offset(width * 0.05f, height * 0.8f),
                        Offset(width * 0.2f, height * 0.75f),
                        Offset(width * 0.4f, height * 0.4f),
                        Offset(width * 0.6f, height * 0.65f),
                        Offset(width * 0.8f, height * 0.25f),
                        Offset(width * 0.95f, height * (1 - (score.toFloat() / 100f)) * height * 0.9f)
                    )

                    // Draw line
                    for (i in 0 until points.size - 1) {
                        drawLine(
                            brush = Brush.horizontalGradient(listOf(HotPink, NeonCyan)),
                            start = points[i],
                            end = points[i + 1],
                            strokeWidth = 6f,
                            cap = StrokeCap.Round
                        )
                    }

                    // Draw points
                    points.forEach { pt ->
                        drawCircle(
                            color = NeonCyan,
                            center = pt,
                            radius = 8f
                        )
                        drawCircle(
                            color = CarbonDark,
                            center = pt,
                            radius = 4f
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Senin", fontSize = 10.sp, color = CoolGray)
                    Text(text = "Rabu", fontSize = 10.sp, color = CoolGray)
                    Text(text = "Jumat", fontSize = 10.sp, color = CoolGray)
                    Text(text = "Hari Ini (Skor: $score%)", fontSize = 10.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Funnel Analysis Chart
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateGray),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Konversi Funnel Konten Anda",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = WarmWhite
                )

                FunnelProgressRow(label = "Top Funnel (Branding Hook)", ratio = 1.0f, color = HotPink, count = schedules.size)
                FunnelProgressRow(
                    label = "Mid Funnel (Edukasi Nilai)",
                    ratio = if (schedules.isNotEmpty()) completedSchedules.size.toFloat() / schedules.size.toFloat() else 0.5f,
                    color = NeonCyan,
                    count = completedSchedules.size
                )
                FunnelProgressRow(
                    label = "Bottom Funnel (CTA & Sales)",
                    ratio = if (schedules.isNotEmpty()) (completedSchedules.count { it.ctaText.isNotBlank() }.toFloat() / schedules.size.toFloat()) else 0.2f,
                    color = EmeraldAccent,
                    count = completedSchedules.count { it.ctaText.isNotBlank() }
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun FunnelProgressRow(label: String, ratio: Float, color: Color, count: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, fontSize = 12.sp, color = WarmWhite, fontWeight = FontWeight.Bold)
            Text(text = "$count Agenda (${(ratio * 100).toInt()}%)", fontSize = 11.sp, color = CoolGray)
        }
        
        // Progress track drawn cleanly
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(Color(0xFF1E2235), RoundedCornerShape(5.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(ratio.coerceIn(0.01f, 1.0f))
                    .background(color, RoundedCornerShape(5.dp))
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

// Google Center Sync integration tab
@Composable
fun GoogleSyncScreen(
    user: GoogleUser,
    logs: List<SyncLog>,
    onToggleLink: () -> Unit,
    onBackup: () -> Unit,
    onClearLogs: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "🔗 Google Account Hub",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = WarmWhite
            )
            Text(
                text = "Integrasi data jadwal dengan Google Calendar & Google Drive",
                fontSize = 13.sp,
                color = CoolGray
            )
        }

        // Account linked status card
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateGray),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.dp, if (user.isLinked) EmeraldAccent.copy(alpha = 0.2f) else DarkBorder, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(if (user.isLinked) EmeraldAccent.copy(alpha = 0.15f) else Color.Red.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (user.isLinked) Icons.Default.CloudQueue else Icons.Default.CloudOff,
                                contentDescription = "",
                                tint = if (user.isLinked) EmeraldAccent else Color.Red,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Text(
                                text = if (user.isLinked) "Google Terhubung" else "Google Terputus",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarmWhite
                            )
                            Text(
                                text = if (user.isLinked) user.email else "Belum tersinkronisasi",
                                fontSize = 12.sp,
                                color = CoolGray
                            )
                        }
                    }

                    Switch(
                        checked = user.isLinked,
                        onCheckedChange = { onToggleLink() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = EmeraldAccent,
                            checkedTrackColor = EmeraldAccent.copy(alpha = 0.5f),
                            uncheckedThumbColor = CoolGray,
                            uncheckedTrackColor = DarkBorder
                        )
                    )
                }

                if (user.isLinked) {
                    Column(
                        modifier = Modifier
                            .background(Color(0xFF1E2235), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "✅ Autopost & Autosync Aktif",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldAccent
                        )
                        Text(
                            text = "Setiap jadwal baru yang ditambahkan di kalender akan langsung disinkronkan secara realtime ke Google Calendar & agenda Reminder Gmail akun Anda.",
                            fontSize = 11.sp,
                            color = CoolGray,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // Operations buttons
        Text(
            text = "🛠️ Tindakan Manual Sistem",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = WarmWhite
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onBackup,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22293F)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f),
                enabled = user.isLinked
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Backup, contentDescription = "", modifier = Modifier.size(16.dp), tint = NeonCyan)
                    Text(text = "Drive Backup", color = WarmWhite, fontSize = 12.sp)
                }
            }
        }

        // Sync logs section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📊 Riwayat Sinkronisasi",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = WarmWhite
            )

            if (logs.isNotEmpty()) {
                Text(
                    text = "Bersihkan",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = HotPink,
                    modifier = Modifier.clickable { onClearLogs() }
                )
            }
        }

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SlateGray, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Belum ada riwayat aktivitas sync saat ini.",
                    fontSize = 12.sp,
                    color = CoolGray
                )
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateGray),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    logs.forEach { log ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E2235), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = log.activity, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WarmWhite)
                                Text(text = log.details, fontSize = 11.sp, color = CoolGray, lineHeight = 14.sp)
                                Text(
                                    text = formatTimestamp(log.timestamp),
                                    fontSize = 9.sp,
                                    color = NeonCyan
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (log.status == "SUCCESS") EmeraldAccent.copy(alpha = 0.15f) else Color.Red.copy(alpha = 0.15f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = log.status,
                                    color = if (log.status == "SUCCESS") EmeraldAccent else Color.Red,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

// Dialog form for creating schedules
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddScheduleDialog(onDismiss: () -> Unit, onSave: (String, String, String, String, String, String, String, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("UMKM & Jasa") }
    var targetPlatform by remember { mutableStateOf("Video") } // Video, Live Stream
    var dateString by remember { mutableStateOf("2026-05-27") }
    var timeString by remember { mutableStateOf("19:00") }
    var funnelGoal by remember { mutableStateOf("Awareness") } // Awareness, Engagement, Conversion, Community
    var customHook by remember { mutableStateOf("") }
    var customScript by remember { mutableStateOf("") }
    var customCta by remember { mutableStateOf("") }

    val categoryOptions = listOf("UMKM & Jasa", "Affiliate", "Personal Branding", "E-Commerce", "Edukasi & Tech", "AI Creator")
    val platformOptions = listOf("Video", "Live Stream")
    val funnelOptions = listOf("Awareness", "Engagement", "Conversion", "Community Growth")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateGray),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, DarkBorder, RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "✍️ Jadwalkan Agenda Baru",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = WarmWhite
                )

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tema/Judul Konten") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = WarmWhite,
                        unfocusedTextColor = WarmWhite,
                        focusedLabelColor = NeonCyan
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("add_schedule_title")
                )

                // Select Platform Row
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Target Format", fontSize = 11.sp, color = CoolGray)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        platformOptions.forEach { plat ->
                            val isSel = targetPlatform == plat
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSel) NeonCyan.copy(alpha = 0.15f) else Color(0xFF1E2235),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(1.dp, if (isSel) NeonCyan else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { targetPlatform = plat }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = plat,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) NeonCyan else WarmWhite
                                )
                            }
                        }
                    }
                }

                // Category dropdown representation tags
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Kategori Bisnis", fontSize = 11.sp, color = CoolGray)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categoryOptions.forEach { cat ->
                            val isSel = selectedCategory == cat
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSel) NeonCyan.copy(alpha = 0.12f) else Color(0xFF22293F),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { selectedCategory = cat }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(text = cat, fontSize = 11.sp, color = if (isSel) NeonCyan else WarmWhite, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Funnel selection tags
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Funnel Goal", fontSize = 11.sp, color = CoolGray)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        funnelOptions.forEach { opt ->
                            val isSel = funnelGoal == opt
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSel) HotPink.copy(alpha = 0.15f) else Color(0xFF22293F),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { funnelGoal = opt }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(text = opt, fontSize = 11.sp, color = if (isSel) HotPink else WarmWhite, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Date Time row inputs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = dateString,
                        onValueChange = { dateString = it },
                        label = { Text("Tanggal (YYYY-MM-DD)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = WarmWhite,
                            unfocusedTextColor = WarmWhite
                        ),
                        modifier = Modifier.weight(1.3f)
                    )

                    OutlinedTextField(
                        value = timeString,
                        onValueChange = { timeString = it },
                        label = { Text("Jam (HH:MM)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = WarmWhite,
                            unfocusedTextColor = WarmWhite
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = DarkBorder)

                // Optional hook
                OutlinedTextField(
                    value = customHook,
                    onValueChange = { customHook = it },
                    label = { Text("Viral Hook Konten (Opsional)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = WarmWhite,
                        unfocusedTextColor = WarmWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Optional CTA
                OutlinedTextField(
                    value = customCta,
                    onValueChange = { customCta = it },
                    label = { Text("Call to Action / CTA (Opsional)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = WarmWhite,
                        unfocusedTextColor = WarmWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Submit Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, HotPink),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = HotPink),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal")
                    }

                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onSave(
                                    title,
                                    selectedCategory,
                                    targetPlatform,
                                    dateString,
                                    timeString,
                                    customHook,
                                    customScript,
                                    funnelGoal,
                                    customCta
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CarbonDark),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.2f).testTag("add_schedule_save_btn"),
                        enabled = title.isNotBlank()
                    ) {
                        Text("Jadwalkan!", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Formatter helpers
fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM YYYY, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun getTodayDateString(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date())
}
