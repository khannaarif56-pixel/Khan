package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.GoldOutlinedButton
import com.example.ui.components.GoldPrimaryButton
import com.example.ui.components.HolographicCard
import com.example.ui.components.HudSectionTitle
import com.example.ui.components.StatusPill
import com.example.ui.theme.HoloCyan
import com.example.ui.theme.HoloEmerald
import com.example.ui.theme.RadiantChampagne
import com.example.ui.theme.SolarGold
import com.example.ui.theme.TextPrimaryGoldWhite
import com.example.ui.theme.TextSecondaryMuted
import com.example.viewmodel.AsyncUiState
import com.example.viewmodel.JarvisViewModel
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun MobileControlScreen(viewModel: JarvisViewModel) {
    val context = LocalContext.current
    val activity = context as? Activity
    val torchOn by viewModel.torchEnabled.collectAsStateWithLifecycle()
    val volume by viewModel.volumeFraction.collectAsStateWithLifecycle()
    val brightness by viewModel.brightnessFraction.collectAsStateWithLifecycle()
    val torchSupported by viewModel.torchSupported.collectAsStateWithLifecycle()

    var cameraGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraGranted = granted
        if (granted) {
            viewModel.toggleTorch(!torchOn)
        } else {
            viewModel.showNotice("Camera permission declined. Torch control may require Camera access on some devices.")
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Torch, Volume & Brightness Hardware Controls
        item {
            HolographicCard(highlightBorder = true, modifier = Modifier.fillMaxWidth()) {
                HudSectionTitle(title = "Device Hardware Controls", icon = Icons.Filled.PhoneAndroid)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.FlashlightOn, contentDescription = null, tint = SolarGold)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Flashlight / Torch", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = if (torchSupported) "Camera flash hardware detected" else "No physical flash on this device/emulator",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondaryMuted
                            )
                        }
                    }
                    Switch(
                        checked = torchOn,
                        onCheckedChange = { enable ->
                            if (cameraGranted) {
                                viewModel.toggleTorch(enable)
                            } else {
                                cameraLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.testTag("torch_toggle_switch")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.VolumeUp, contentDescription = null, tint = SolarGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Media Volume: ${(volume * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = RadiantChampagne
                    )
                }
                Slider(
                    value = volume,
                    onValueChange = { viewModel.setVolume(it) },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(thumbColor = SolarGold, activeTrackColor = SolarGold)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GoldOutlinedButton(
                        text = "Vol -",
                        onClick = { viewModel.adjustVolume(false) },
                        modifier = Modifier.weight(1f)
                    )
                    GoldOutlinedButton(
                        text = "Vol +",
                        onClick = { viewModel.adjustVolume(true) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.BrightnessHigh, contentDescription = null, tint = SolarGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "HUD Window Brightness: ${(brightness * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = RadiantChampagne
                    )
                }
                Slider(
                    value = brightness,
                    onValueChange = {
                        val applied = viewModel.deviceControl.setWindowBrightness(activity, it)
                        viewModel.updateBrightnessState(applied)
                    },
                    valueRange = 0.08f..1f,
                    colors = SliderDefaults.colors(thumbColor = SolarGold, activeTrackColor = SolarGold)
                )
            }
        }

        // Open Installed Applications
        item {
            HolographicCard(modifier = Modifier.fillMaxWidth()) {
                HudSectionTitle(title = "Installed App Launchers")
                Spacer(modifier = Modifier.height(10.dp))
                val apps = listOf(
                    "Phone Dialer" to { viewModel.deviceControl.launchDialer().map { "Opened Dialer" } },
                    "Messages" to { viewModel.deviceControl.launchCategoryApp(Intent.CATEGORY_APP_MESSAGING).map { "Opened Messages" } },
                    "Calendar" to { viewModel.deviceControl.launchCategoryApp(Intent.CATEGORY_APP_CALENDAR).map { "Opened Calendar" } },
                    "Calculator" to { viewModel.deviceControl.launchCategoryApp(Intent.CATEGORY_APP_CALCULATOR).map { "Opened Calculator" } },
                    "Maps" to { viewModel.deviceControl.launchCategoryApp(Intent.CATEGORY_APP_MAPS).map { "Opened Maps" } },
                    "Browser" to { viewModel.deviceControl.launchPackageOrWeb("com.android.chrome", "https://www.google.com") },
                    "YouTube" to { viewModel.deviceControl.launchPackageOrWeb("com.google.android.youtube", "https://m.youtube.com") },
                    "Gmail" to { viewModel.deviceControl.launchPackageOrWeb("com.google.android.gm", "https://mail.google.com") }
                )
                apps.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pair.forEach { (label, action) ->
                            GoldOutlinedButton(
                                text = label,
                                onClick = {
                                    action().onFailure { err ->
                                        viewModel.showNotice(err.localizedMessage ?: "App unavailable")
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Android System Settings Shortcuts
        item {
            HolographicCard(modifier = Modifier.fillMaxWidth()) {
                HudSectionTitle(title = "Android Settings Shortcuts", icon = Icons.Filled.Settings)
                Spacer(modifier = Modifier.height(10.dp))
                val settingsShortcuts = listOf(
                    "Wi-Fi" to Settings.ACTION_WIFI_SETTINGS,
                    "Bluetooth" to Settings.ACTION_BLUETOOTH_SETTINGS,
                    "Display" to Settings.ACTION_DISPLAY_SETTINGS,
                    "Sound" to Settings.ACTION_SOUND_SETTINGS,
                    "Battery" to Settings.ACTION_BATTERY_SAVER_SETTINGS,
                    "Security" to Settings.ACTION_SECURITY_SETTINGS
                )
                settingsShortcuts.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pair.forEach { (label, action) ->
                            GoldOutlinedButton(
                                text = label,
                                onClick = {
                                    viewModel.deviceControl.openAndroidSetting(action)
                                        .onFailure { viewModel.showNotice("Settings panel unavailable on this ROM.") }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Offline Voice Commands & Android OS Restrictions Notice
        item {
            HolographicCard(modifier = Modifier.fillMaxWidth()) {
                HudSectionTitle(title = "Offline Voice Commands & OS Policy", icon = Icons.Filled.Info)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Supported Offline Voice Commands:\n• \"Torch on\" / \"Torch off\" (टॉर्च चालू / बंद)\n• \"Volume up\" / \"Volume down\" (आवाज बढ़ाओ / कम)\n• \"Switch to Hindi\" / \"Switch to English\"\n• \"Emergency lock\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = RadiantChampagne
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Android Security Policy: Modern Android prohibits third-party apps from silently toggling Wi-Fi/Airplane Mode, placing unconfirmed calls, or modifying global system brightness without explicit system settings approval. JARVIS opens official Android Settings and asks confirmation before sensitive operations.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryMuted
                )
            }
        }
    }
}

@Composable
fun ProductivityScreen(
    viewModel: JarvisViewModel,
    onOpenChatHistory: () -> Unit
) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val briefingState by viewModel.briefingState.collectAsStateWithLifecycle()

    val tabs = listOf("Tasks & Reminders", "Notes", "Alarms & Calendar", "Calculator", "Weather & News")
    var selectedTab by remember { mutableStateOf(tabs.first()) }

    var taskTitle by remember { mutableStateOf("") }
    var taskDue by remember { mutableStateOf("Today 6:00 PM") }
    var taskType by remember { mutableStateOf("task") }

    var noteTitle by remember { mutableStateOf("") }
    var noteBody by remember { mutableStateOf("") }

    var alarmHour by remember { mutableStateOf("7") }
    var alarmMinute by remember { mutableStateOf("30") }
    var alarmLabel by remember { mutableStateOf("JARVIS Morning Briefing") }
    var eventTitle by remember { mutableStateOf("") }

    var calcExpression by remember { mutableStateOf("") }
    var calcResult by remember { mutableStateOf("0") }

    var briefingFocus by remember { mutableStateOf("Delhi, India") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tabs) { tab ->
                    FilterChip(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text(tab) }
                    )
                }
            }
        }

        when (selectedTab) {
            "Tasks & Reminders" -> {
                item {
                    HolographicCard(highlightBorder = true, modifier = Modifier.fillMaxWidth()) {
                        HudSectionTitle(title = "Add Task or Reminder")
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = taskType == "task",
                                onClick = { taskType = "task" },
                                label = { Text("Task") }
                            )
                            FilterChip(
                                selected = taskType == "reminder",
                                onClick = { taskType = "reminder" },
                                label = { Text("Reminder") }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = taskTitle,
                            onValueChange = { taskTitle = it },
                            label = { Text("Title") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("task_title_input")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = taskDue,
                            onValueChange = { taskDue = it },
                            label = { Text("Schedule / Due Label") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        GoldPrimaryButton(
                            text = "Save to Cloud",
                            icon = Icons.Filled.Add,
                            enabled = taskTitle.isNotBlank(),
                            onClick = {
                                viewModel.addTask(taskTitle, "", taskType, taskDue)
                                taskTitle = ""
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_task_button")
                        )
                    }
                }

                items(tasks, key = { it.id.ifBlank { it.hashCode().toString() } }) { item ->
                    HolographicCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = item.completed,
                                onCheckedChange = { viewModel.toggleTask(item) }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    textDecoration = if (item.completed) TextDecoration.LineThrough else null,
                                    color = if (item.completed) TextSecondaryMuted else TextPrimaryGoldWhite
                                )
                                Text(
                                    text = "${item.category.uppercase()} • ${item.dueLabel}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SolarGold
                                )
                            }
                            IconButton(onClick = { viewModel.deleteTask(item.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete Task", tint = TextSecondaryMuted)
                            }
                        }
                    }
                }
            }

            "Notes" -> {
                item {
                    HolographicCard(highlightBorder = true, modifier = Modifier.fillMaxWidth()) {
                        HudSectionTitle(title = "Create Cloud Note")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = noteTitle,
                            onValueChange = { noteTitle = it },
                            label = { Text("Note Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = noteBody,
                            onValueChange = { noteBody = it },
                            label = { Text("Note Content") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        GoldPrimaryButton(
                            text = "Save Note",
                            icon = Icons.Filled.Add,
                            enabled = noteTitle.isNotBlank() && noteBody.isNotBlank(),
                            onClick = {
                                viewModel.addNote(noteTitle, noteBody, "Personal")
                                noteTitle = ""
                                noteBody = ""
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                items(notes, key = { it.id.ifBlank { it.hashCode().toString() } }) { note ->
                    HolographicCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(note.title, style = MaterialTheme.typography.titleMedium, color = RadiantChampagne)
                            IconButton(onClick = { viewModel.deleteNote(note.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete Note", tint = TextSecondaryMuted)
                            }
                        }
                        Text(note.content, style = MaterialTheme.typography.bodyMedium, color = TextPrimaryGoldWhite)
                    }
                }
            }

            "Alarms & Calendar" -> {
                item {
                    HolographicCard(highlightBorder = true, modifier = Modifier.fillMaxWidth()) {
                        HudSectionTitle(title = "Set Android Alarm", icon = Icons.Filled.Alarm)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = alarmHour,
                                onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) alarmHour = it },
                                label = { Text("Hour (0-23)") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = alarmMinute,
                                onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) alarmMinute = it },
                                label = { Text("Minute (0-59)") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = alarmLabel,
                            onValueChange = { alarmLabel = it },
                            label = { Text("Alarm Label") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        GoldPrimaryButton(
                            text = "Launch Android Clock Alarm",
                            icon = Icons.Filled.Alarm,
                            onClick = {
                                viewModel.deviceControl.createAlarmIntent(
                                    hour = alarmHour.toIntOrNull() ?: 7,
                                    minute = alarmMinute.toIntOrNull() ?: 0,
                                    message = alarmLabel
                                ).onFailure { viewModel.showNotice(it.localizedMessage ?: "Alarm app unavailable") }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                item {
                    HolographicCard(modifier = Modifier.fillMaxWidth()) {
                        HudSectionTitle(title = "Calendar Event (Privacy-First Intent)", icon = Icons.Filled.CalendarMonth)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "JARVIS opens your Android Calendar directly so you review and confirm events before saving, without needing invasive background calendar read access.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryMuted
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = eventTitle,
                            onValueChange = { eventTitle = it },
                            label = { Text("Event Title") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        GoldOutlinedButton(
                            text = "Add to Android Calendar",
                            icon = Icons.Filled.CalendarMonth,
                            onClick = {
                                viewModel.deviceControl.createCalendarEventIntent(
                                    title = eventTitle,
                                    description = "Created via JARVIS Assistant"
                                ).onFailure { viewModel.showNotice(it.localizedMessage ?: "Calendar app unavailable") }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            "Calculator" -> {
                item {
                    HolographicCard(highlightBorder = true, modifier = Modifier.fillMaxWidth()) {
                        HudSectionTitle(title = "Holographic Calculator", icon = Icons.Filled.Calculate)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = calcExpression,
                            onValueChange = { calcExpression = it },
                            label = { Text("Expression (e.g., (25 * 4) + sqrt(144))") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "= $calcResult",
                            style = MaterialTheme.typography.headlineLarge,
                            color = SolarGold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        val keypadRows = listOf(
                            listOf("7", "8", "9", "/"),
                            listOf("4", "5", "6", "*"),
                            listOf("1", "2", "3", "-"),
                            listOf("C", "0", ".", "+")
                        )
                        keypadRows.forEach { row ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                row.forEach { key ->
                                    GoldOutlinedButton(
                                        text = key,
                                        onClick = {
                                            if (key == "C") {
                                                calcExpression = ""
                                                calcResult = "0"
                                            } else {
                                                calcExpression += key
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        GoldPrimaryButton(
                            text = "Calculate",
                            onClick = {
                                calcResult = evaluateSimpleMath(calcExpression)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            "Weather & News" -> {
                item {
                    HolographicCard(highlightBorder = true, modifier = Modifier.fillMaxWidth()) {
                        HudSectionTitle(title = "Weather & Global News Briefings", icon = Icons.Filled.WbSunny)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = briefingFocus,
                            onValueChange = { briefingFocus = it },
                            label = { Text("City or News Topic") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GoldPrimaryButton(
                                text = "Weather Guide",
                                icon = Icons.Filled.WbSunny,
                                onClick = { viewModel.requestBriefing("Weather", briefingFocus) },
                                modifier = Modifier.weight(1f)
                            )
                            GoldOutlinedButton(
                                text = "News Brief",
                                icon = Icons.Filled.Newspaper,
                                onClick = { viewModel.requestBriefing("News", briefingFocus) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        when (val st = briefingState) {
                            is AsyncUiState.Loading -> CircularProgressIndicator(color = SolarGold, modifier = Modifier.size(24.dp))
                            is AsyncUiState.Success -> Text(st.data, style = MaterialTheme.typography.bodyMedium, color = TextPrimaryGoldWhite)
                            is AsyncUiState.Error -> Text(st.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            else -> Unit
                        }
                    }
                }
            }
        }

        item {
            GoldOutlinedButton(
                text = "Open Full Cloud Chat History",
                onClick = onOpenChatHistory,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun evaluateSimpleMath(expr: String): String {
    val clean = expr.replace(" ", "")
    if (clean.isBlank()) return "0"
    return try {
        val value = SimpleMathParser(clean).parse()
        if (value % 1.0 == 0.0) value.toLong().toString() else "%.6f".format(value).trimEnd('0').trimEnd('.')
    } catch (_: Exception) {
        "Invalid expression"
    }
}

private class SimpleMathParser(private val str: String) {
    private var pos = -1
    private var ch = 0

    private fun nextChar() {
        ch = if (++pos < str.length) str[pos].code else -1
    }

    private fun eat(charToEat: Int): Boolean {
        while (ch == ' '.code) nextChar()
        if (ch == charToEat) {
            nextChar()
            return true
        }
        return false
    }

    fun parse(): Double {
        nextChar()
        val x = parseExpression()
        if (pos < str.length) throw IllegalArgumentException("Unexpected char")
        return x
    }

    private fun parseExpression(): Double {
        var x = parseTerm()
        while (true) {
            x = when {
                eat('+'.code) -> x + parseTerm()
                eat('-'.code) -> x - parseTerm()
                else -> return x
            }
        }
    }

    private fun parseTerm(): Double {
        var x = parseFactor()
        while (true) {
            x = when {
                eat('*'.code) -> x * parseFactor()
                eat('/'.code) -> x / parseFactor()
                else -> return x
            }
        }
    }

    private fun parseFactor(): Double {
        if (eat('+'.code)) return parseFactor()
        if (eat('-'.code)) return -parseFactor()
        var x: Double
        val startPos = pos
        if (eat('('.code)) {
            x = parseExpression()
            eat(')'.code)
        } else if (ch in '0'.code..'9'.code || ch == '.'.code) {
            while (ch in '0'.code..'9'.code || ch == '.'.code) nextChar()
            x = str.substring(startPos, pos).toDouble()
        } else if (ch in 'a'.code..'z'.code) {
            while (ch in 'a'.code..'z'.code) nextChar()
            val func = str.substring(startPos, pos)
            x = parseFactor()
            x = when (func) {
                "sqrt" -> sqrt(x)
                "sin" -> sin(Math.toRadians(x))
                "cos" -> cos(Math.toRadians(x))
                else -> throw IllegalArgumentException("Unknown func $func")
            }
        } else {
            throw IllegalArgumentException("Unexpected factor")
        }
        if (eat('^'.code)) x = x.pow(parseFactor())
        return x
    }
}
