package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.device.CoreVoiceState
import com.example.ui.components.GoldOutlinedButton
import com.example.ui.components.GoldPrimaryButton
import com.example.ui.components.GoldenHolographicCore
import com.example.ui.components.HolographicCard
import com.example.ui.components.HudSectionTitle
import com.example.ui.components.StatusPill
import com.example.ui.navigation.JarvisCenter
import com.example.ui.theme.HoloCyan
import com.example.ui.theme.HoloEmerald
import com.example.ui.theme.RadiantChampagne
import com.example.ui.theme.SolarGold
import com.example.ui.theme.TextPrimaryGoldWhite
import com.example.ui.theme.TextSecondaryMuted
import com.example.viewmodel.AsyncUiState
import com.example.viewmodel.JarvisViewModel

@Composable
fun HomeDashboardScreen(
    viewModel: JarvisViewModel,
    onNavigateCenter: (JarvisCenter) -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.profileSettings.collectAsStateWithLifecycle()
    val coreState by viewModel.coreVoiceState.collectAsStateWithLifecycle()
    val amplitude by viewModel.voiceAmplitude.collectAsStateWithLifecycle()
    val partialTranscript by viewModel.partialTranscript.collectAsStateWithLifecycle()
    val brainState by viewModel.brainStreamState.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val memories by viewModel.memories.collectAsStateWithLifecycle()

    var quickPrompt by remember { mutableStateOf("") }
    var micGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        micGranted = granted
        if (granted) {
            viewModel.voiceEngine.startListening(
                languageCode = settings.primaryLanguage,
                onResult = { spoken -> viewModel.handleVoiceCommand(spoken) },
                onError = { err -> viewModel.showNotice(err) }
            )
        } else {
            viewModel.showNotice("Microphone permission denied. You can still type commands or enable Mic in Security Center.")
        }
    }

    fun triggerVoiceCommand() {
        when (coreState) {
            CoreVoiceState.LISTENING -> viewModel.voiceEngine.stopListening()
            CoreVoiceState.SPEAKING -> viewModel.voiceEngine.stopSpeaking()
            else -> {
                if (micGranted) {
                    viewModel.voiceEngine.startListening(
                        languageCode = settings.primaryLanguage,
                        onResult = { spoken -> viewModel.handleVoiceCommand(spoken) },
                        onError = { err -> viewModel.showNotice(err) }
                    )
                } else {
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Central Golden Holographic AI Core Showcase
            item {
                HolographicCard(
                    highlightBorder = true,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusPill(
                                label = if (settings.primaryLanguage == "hi") "HINDI + EN CORE" else "EN + HINDI CORE",
                                tint = SolarGold
                            )
                            StatusPill(
                                label = if (micGranted) "MIC ARMED" else "MIC STANDBY",
                                tint = if (micGranted) HoloEmerald else HoloCyan
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        GoldenHolographicCore(
                            coreState = coreState,
                            amplitude = amplitude,
                            coreSize = 224.dp,
                            onCoreTap = { triggerVoiceCommand() }
                        )

                        if (partialTranscript.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "\"$partialTranscript\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = RadiantChampagne,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Main Voice-Command Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GoldPrimaryButton(
                                text = when (coreState) {
                                    CoreVoiceState.LISTENING -> "Stop Listening"
                                    CoreVoiceState.SPEAKING -> "Mute Voice"
                                    else -> "Voice Command"
                                },
                                icon = if (coreState == CoreVoiceState.LISTENING || coreState == CoreVoiceState.SPEAKING) {
                                    Icons.Filled.Stop
                                } else {
                                    Icons.Filled.Mic
                                },
                                onClick = { triggerVoiceCommand() },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("main_voice_command_button")
                            )
                            GoldOutlinedButton(
                                text = "AI Brain",
                                icon = Icons.Filled.GraphicEq,
                                onClick = { onNavigateCenter(JarvisCenter.AI_BRAIN) },
                                modifier = Modifier
                                    .weight(0.7f)
                                    .testTag("quick_ai_brain_button")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quick bilingual prompt bar right beneath the core
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = quickPrompt,
                                onValueChange = { quickPrompt = it },
                                placeholder = {
                                    Text(
                                        if (settings.primaryLanguage == "hi")
                                            "जार्विस से कुछ भी पूछें (हिंदी / English)..."
                                        else
                                            "Ask JARVIS anything in English or Hindi..."
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("dashboard_quick_prompt_input")
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    if (quickPrompt.isNotBlank()) {
                                        viewModel.sendAiBrainMessage(quickPrompt, center = "brain")
                                        quickPrompt = ""
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("dashboard_quick_send_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send command to JARVIS",
                                    tint = SolarGold
                                )
                            }
                        }

                        // Live AI response telemetry box
                        when (val st = brainState) {
                            is AsyncUiState.Streaming -> {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = st.text,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimaryGoldWhite,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            is AsyncUiState.Success -> {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = st.data,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = RadiantChampagne,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            is AsyncUiState.Error -> {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = st.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            else -> Unit
                        }
                    }
                }
            }

            // Telemetry summary row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HolographicCard(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${tasks.count { !it.completed }}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = SolarGold
                        )
                        Text(
                            text = "OPEN TASKS",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryMuted
                        )
                    }
                    HolographicCard(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${memories.size}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = RadiantChampagne
                        )
                        Text(
                            text = "MEMORIES",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryMuted
                        )
                    }
                    HolographicCard(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "v${settings.installedVersion}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = HoloCyan
                        )
                        Text(
                            text = "CORE BUILD",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryMuted
                        )
                    }
                }
            }

            // Organized 12-Center Holographic Command Grid
            item {
                HudSectionTitle(
                    title = "Command Centers",
                    trailingText = "12 MODULES ONLINE"
                )
            }

            val rows = JarvisCenter.allTwelveCenters.chunked(2)
            items(rows.size) { rowIdx ->
                val pair = rows[rowIdx]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    pair.forEach { center ->
                        HolographicCard(
                            onClick = { onNavigateCenter(center) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("center_card_${center.route}")
                        ) {
                            Icon(
                                imageVector = center.icon,
                                contentDescription = center.title,
                                tint = SolarGold,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = center.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimaryGoldWhite,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = center.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondaryMuted,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}
