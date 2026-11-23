package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.device.CoreVoiceState
import com.example.data.device.VoiceGender
import com.example.ui.components.GoldOutlinedButton
import com.example.ui.components.GoldPrimaryButton
import com.example.ui.components.GoldenHolographicCore
import com.example.ui.components.HolographicCard
import com.example.ui.components.HudSectionTitle
import com.example.ui.components.StatusPill
import com.example.ui.theme.HoloAmberWarn
import com.example.ui.theme.HoloCyan
import com.example.ui.theme.HoloEmerald
import com.example.ui.theme.RadiantChampagne
import com.example.ui.theme.SolarGold
import com.example.ui.theme.TextPrimaryGoldWhite
import com.example.ui.theme.TextSecondaryMuted
import com.example.viewmodel.AsyncUiState
import com.example.viewmodel.JarvisViewModel

@Composable
fun AiBrainScreen(viewModel: JarvisViewModel) {
    val settings by viewModel.profileSettings.collectAsStateWithLifecycle()
    val chats by viewModel.chats.collectAsStateWithLifecycle()
    val memories by viewModel.memories.collectAsStateWithLifecycle()
    val streamState by viewModel.brainStreamState.collectAsStateWithLifecycle()

    var input by remember { mutableStateOf("") }
    var showMemoryPanel by remember { mutableStateOf(false) }
    var newMemoryText by remember { mutableStateOf("") }
    var newMemoryCategory by remember { mutableStateOf("Preference") }

    val quickPrompts = listOf(
        "Explain Quantum Computing simply",
        "हिंदी में AI कैसे काम करता है समझाएं",
        "Solve: 3x² - 12x + 9 = 0",
        "Summarize Clean Architecture in Android",
        "Create a 7-day study timetable"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                HolographicCard(highlightBorder = true, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Personal Memory Permission",
                                style = MaterialTheme.typography.titleMedium,
                                color = RadiantChampagne
                            )
                            Text(
                                text = if (settings.memoryEnabled)
                                    "JARVIS remembers ${memories.size} approved items (AES-256 vault)"
                                else "Memory paused — replies use current session only",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondaryMuted
                            )
                        }
                        Switch(
                            checked = settings.memoryEnabled,
                            onCheckedChange = { viewModel.setMemoryEnabled(it) },
                            modifier = Modifier.testTag("memory_permission_switch")
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    GoldOutlinedButton(
                        text = if (showMemoryPanel) "Hide Personal Memory Vault" else "Manage Personal Memories (${memories.size})",
                        icon = Icons.Filled.Psychology,
                        onClick = { showMemoryPanel = !showMemoryPanel },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("toggle_memory_vault_button")
                    )
                }
            }

            if (showMemoryPanel) {
                item {
                    HolographicCard(modifier = Modifier.fillMaxWidth()) {
                        HudSectionTitle(title = "Add Approved Memory")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newMemoryCategory,
                            onValueChange = { newMemoryCategory = it },
                            label = { Text("Category (e.g., Study, Goal, Preference)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newMemoryText,
                            onValueChange = { newMemoryText = it },
                            label = { Text("What should JARVIS remember?") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("new_memory_input")
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        GoldPrimaryButton(
                            text = "Save Encrypted Memory",
                            icon = Icons.Filled.Add,
                            onClick = {
                                if (newMemoryText.isNotBlank()) {
                                    viewModel.addPersonalMemory(newMemoryCategory, newMemoryText, encrypt = true)
                                    newMemoryText = ""
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_memory_button")
                        )
                        if (memories.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            memories.forEach { mem ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = mem.category,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SolarGold
                                        )
                                        Text(
                                            text = mem.content,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextPrimaryGoldWhite
                                        )
                                    }
                                    IconButton(onClick = { viewModel.deletePersonalMemory(mem.id) }) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Delete Memory",
                                            tint = TextSecondaryMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(quickPrompts) { chipText ->
                        FilterChip(
                            selected = false,
                            onClick = { viewModel.sendAiBrainMessage(chipText) },
                            label = { Text(chipText, maxLines = 1) }
                        )
                    }
                }
            }

            if (chats.isEmpty() && streamState is AsyncUiState.Idle) {
                item {
                    HolographicCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (settings.primaryLanguage == "hi")
                                "नमस्ते aarif khann! मैं JARVIS हूँ — हिंदी या English में कुछ भी पूछें।"
                            else
                                "Welcome, aarif khann. Ask JARVIS anything — general knowledge, math, studies, summaries, or code in English or Hindi.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = RadiantChampagne
                        )
                    }
                }
            }

            items(chats, key = { it.id.ifBlank { it.hashCode().toString() } }) { msg ->
                val isUser = msg.role == "user"
                HolographicCard(
                    highlightBorder = !isUser,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusPill(
                            label = if (isUser) "OWNER" else "JARVIS AI",
                            tint = if (isUser) HoloCyan else SolarGold
                        )
                        if (!isUser) {
                            IconButton(
                                onClick = { viewModel.speakWithActiveVoice(msg.content) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.VolumeUp,
                                    contentDescription = "Read aloud",
                                    tint = SolarGold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = msg.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimaryGoldWhite
                    )
                }
            }

            when (val st = streamState) {
                is AsyncUiState.Loading -> {
                    item {
                        HolographicCard(highlightBorder = true, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "JARVIS is thinking...",
                                style = MaterialTheme.typography.labelLarge,
                                color = HoloCyan
                            )
                        }
                    }
                }
                is AsyncUiState.Streaming -> {
                    item {
                        HolographicCard(highlightBorder = true, modifier = Modifier.fillMaxWidth()) {
                            StatusPill(label = "STREAMING", tint = HoloCyan)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = st.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = RadiantChampagne
                            )
                        }
                    }
                }
                is AsyncUiState.Error -> {
                    item {
                        HolographicCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "AI Core Notice: ${st.message}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                else -> Unit
            }
        }

        // Bottom composer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = {
                    Text(
                        if (settings.primaryLanguage == "hi") "अपना संदेश लिखें..."
                        else "Message JARVIS..."
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_brain_input")
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (input.isNotBlank()) {
                        viewModel.sendAiBrainMessage(input)
                        input = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("ai_brain_send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send Message",
                    tint = SolarGold
                )
            }
        }
    }
}

@Composable
fun VoiceAssistantScreen(
    viewModel: JarvisViewModel,
    onOpenVoiceSelection: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.profileSettings.collectAsStateWithLifecycle()
    val coreState by viewModel.coreVoiceState.collectAsStateWithLifecycle()
    val amplitude by viewModel.voiceAmplitude.collectAsStateWithLifecycle()
    val transcript by viewModel.partialTranscript.collectAsStateWithLifecycle()
    val handsFree by viewModel.handsFreeEnabled.collectAsStateWithLifecycle()
    val wakeWord by viewModel.wakeWordArmed.collectAsStateWithLifecycle()
    val ttsStatus by viewModel.ttsStatusMessage.collectAsStateWithLifecycle()

    var micGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        micGranted = granted
        if (!granted) {
            viewModel.showNotice("Microphone permission is needed for live voice commands.")
        }
    }

    var rateSlider by remember(settings.voiceSpeechRate) {
        mutableStateOf(settings.voiceSpeechRate.toFloat())
    }
    var pitchSlider by remember(settings.voicePitch) {
        mutableStateOf(settings.voicePitch.toFloat())
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HolographicCard(highlightBorder = true, modifier = Modifier.fillMaxWidth()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatusPill(
                            label = if (micGranted) "MIC GRANTED" else "MIC NOT GRANTED",
                            tint = if (micGranted) HoloEmerald else HoloAmberWarn
                        )
                        StatusPill(
                            label = if (settings.primaryLanguage == "hi") "hi-IN" else "en-US",
                            tint = SolarGold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    GoldenHolographicCore(
                        coreState = coreState,
                        amplitude = amplitude,
                        coreSize = 200.dp,
                        onCoreTap = {
                            if (coreState == CoreVoiceState.LISTENING) {
                                viewModel.voiceEngine.stopListening()
                            } else if (micGranted) {
                                viewModel.voiceEngine.startListening(
                                    languageCode = settings.primaryLanguage,
                                    onResult = { viewModel.handleVoiceCommand(it) },
                                    onError = { viewModel.showNotice(it) }
                                )
                            } else {
                                micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    )
                    if (transcript.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Recognized: $transcript",
                            style = MaterialTheme.typography.bodyMedium,
                            color = RadiantChampagne
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GoldPrimaryButton(
                            text = if (coreState == CoreVoiceState.LISTENING) "Stop Mic" else "Speak Now",
                            icon = if (coreState == CoreVoiceState.LISTENING) Icons.Filled.Stop else Icons.Filled.Mic,
                            onClick = {
                                if (coreState == CoreVoiceState.LISTENING) {
                                    viewModel.voiceEngine.stopListening()
                                } else if (micGranted) {
                                    viewModel.voiceEngine.startListening(
                                        languageCode = settings.primaryLanguage,
                                        onResult = { viewModel.handleVoiceCommand(it) },
                                        onError = { viewModel.showNotice(it) }
                                    )
                                } else {
                                    micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("voice_center_mic_button")
                        )
                        GoldOutlinedButton(
                            text = "20 Voices",
                            icon = Icons.Filled.RecordVoiceOver,
                            onClick = onOpenVoiceSelection,
                            modifier = Modifier
                                .weight(0.8f)
                                .testTag("open_voice_selection_button")
                        )
                    }
                }
            }
        }

        item {
            HolographicCard(modifier = Modifier.fillMaxWidth()) {
                HudSectionTitle(title = "Voice Controls & Calibration")
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = ttsStatus, style = MaterialTheme.typography.labelSmall, color = HoloCyan)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hands-Free Auto Speak", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Automatically read AI replies aloud while JARVIS is active.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryMuted
                        )
                    }
                    Switch(checked = handsFree, onCheckedChange = { viewModel.setHandsFreeMode(it) })
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Optional Wake Phrase ('Hey Jarvis')", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Strips wake prefix during active in-app listening sessions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryMuted
                        )
                    }
                    Switch(checked = wakeWord, onCheckedChange = { viewModel.setWakeWordArmed(it) })
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Speech Speed: ${"%.2f".format(rateSlider)}x",
                    style = MaterialTheme.typography.labelMedium,
                    color = RadiantChampagne
                )
                Slider(
                    value = rateSlider,
                    onValueChange = { rateSlider = it },
                    onValueChangeFinished = { viewModel.updateVoiceTuning(rateSlider, pitchSlider) },
                    valueRange = 0.5f..2.0f,
                    colors = SliderDefaults.colors(thumbColor = SolarGold, activeTrackColor = SolarGold)
                )

                Text(
                    text = "Voice Pitch: ${"%.2f".format(pitchSlider)}x",
                    style = MaterialTheme.typography.labelMedium,
                    color = RadiantChampagne
                )
                Slider(
                    value = pitchSlider,
                    onValueChange = { pitchSlider = it },
                    onValueChangeFinished = { viewModel.updateVoiceTuning(rateSlider, pitchSlider) },
                    valueRange = 0.5f..2.0f,
                    colors = SliderDefaults.colors(thumbColor = SolarGold, activeTrackColor = SolarGold)
                )
            }
        }

        // Android Background Microphone Policy Transparency
        item {
            HolographicCard(modifier = Modifier.fillMaxWidth()) {
                HudSectionTitle(
                    title = "Android Microphone Privacy Notice",
                    icon = Icons.Filled.Info
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Android restricts background microphone capture to protect user privacy. JARVIS activates the microphone only when you grant RECORD_AUDIO permission and initiate listening while the app is visible. Always-on screen-off hotword detection requires a system-privileged OS assistant role.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryMuted
                )
            }
        }
    }
}

@Composable
fun VoiceSelectionScreen(viewModel: JarvisViewModel) {
    val settings by viewModel.profileSettings.collectAsStateWithLifecycle()
    val voices by viewModel.voiceProfiles.collectAsStateWithLifecycle()
    val ttsStatus by viewModel.ttsStatusMessage.collectAsStateWithLifecycle()

    var selectedFilter by remember { mutableStateOf("All") }
    var testPhrase by remember {
        mutableStateOf("Hello aarif khann, JARVIS golden holographic systems are at your command.")
    }

    val filters = listOf("All", "Masculine (10)", "Feminine (10)", "English", "Hindi", "Favorites")

    val filteredVoices = remember(voices, selectedFilter, settings.favoriteVoiceIds) {
        when (selectedFilter) {
            "Masculine (10)" -> voices.filter { it.gender == VoiceGender.MASCULINE }
            "Feminine (10)" -> voices.filter { it.gender == VoiceGender.FEMININE }
            "English" -> voices.filter { it.languageCode == "en" }
            "Hindi" -> voices.filter { it.languageCode == "hi" }
            "Favorites" -> voices.filter { settings.favoriteVoiceIds.contains(it.id) }
            else -> voices
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HolographicCard(highlightBorder = true, modifier = Modifier.fillMaxWidth()) {
                HudSectionTitle(title = "Voice Test Lab", icon = Icons.Filled.RecordVoiceOver)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = ttsStatus, style = MaterialTheme.typography.labelSmall, color = HoloCyan)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = testPhrase,
                    onValueChange = { testPhrase = it },
                    label = { Text("Custom Test Voice Phrase (EN / हिंदी)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voice_test_phrase_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GoldPrimaryButton(
                        text = "Test Active Voice",
                        icon = Icons.Filled.PlayArrow,
                        onClick = { viewModel.speakWithActiveVoice(testPhrase) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("test_active_voice_button")
                    )
                    GoldOutlinedButton(
                        text = "Stop",
                        icon = Icons.Filled.Stop,
                        onClick = { viewModel.voiceEngine.stopSpeaking() },
                        modifier = Modifier.weight(0.45f)
                    )
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filters) { f ->
                    FilterChip(
                        selected = selectedFilter == f,
                        onClick = { selectedFilter = f },
                        label = { Text(f) }
                    )
                }
            }
        }

        items(filteredVoices, key = { it.id }) { profile ->
            val isSelected = settings.selectedVoiceId == profile.id
            val isFavorite = settings.favoriteVoiceIds.contains(profile.id)

            HolographicCard(
                highlightBorder = isSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("voice_card_${profile.id}")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isSelected) SolarGold else TextPrimaryGoldWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${profile.gender.label} • ${profile.style} • ${profile.localeTag}",
                            style = MaterialTheme.typography.labelSmall,
                            color = RadiantChampagne
                        )
                    }
                    IconButton(
                        onClick = { viewModel.toggleFavoriteVoice(profile.id) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Save Favorite Voice",
                            tint = if (isFavorite) SolarGold else TextSecondaryMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusPill(
                        label = if (profile.availableOnDevice) "AVAILABLE" else "UNAVAILABLE ON DEVICE",
                        tint = if (profile.availableOnDevice) HoloEmerald else HoloAmberWarn
                    )
                    if (isSelected) {
                        StatusPill(label = "ACTIVE", tint = SolarGold)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = profile.availabilityNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryMuted
                )

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GoldOutlinedButton(
                        text = "Preview",
                        icon = Icons.Filled.PlayArrow,
                        enabled = profile.availableOnDevice,
                        onClick = { viewModel.previewVoice(profile) },
                        modifier = Modifier.weight(1f)
                    )
                    GoldOutlinedButton(
                        text = "Test",
                        enabled = profile.availableOnDevice,
                        onClick = { viewModel.previewVoice(profile, testPhrase) },
                        modifier = Modifier.weight(0.75f)
                    )
                    GoldPrimaryButton(
                        text = if (isSelected) "Selected" else "Select",
                        enabled = profile.availableOnDevice && !isSelected,
                        onClick = { viewModel.selectVoice(profile) },
                        modifier = Modifier.weight(0.95f)
                    )
                }
            }
        }
    }
}
