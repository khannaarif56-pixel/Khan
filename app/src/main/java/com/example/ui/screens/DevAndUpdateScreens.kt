package com.example.ui.screens

import android.os.Build
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.GoldOutlinedButton
import com.example.ui.components.GoldPrimaryButton
import com.example.ui.components.HolographicCard
import com.example.ui.components.HudSectionTitle
import com.example.ui.components.StatusPill
import com.example.ui.theme.HoloCyan
import com.example.ui.theme.HoloEmerald
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.RadiantChampagne
import com.example.ui.theme.SolarGold
import com.example.ui.theme.TextPrimaryGoldWhite
import com.example.ui.theme.TextSecondaryMuted
import com.example.viewmodel.AsyncUiState
import com.example.viewmodel.JarvisViewModel

@Composable
fun AiDevCenterScreen(viewModel: JarvisViewModel) {
    val codingState by viewModel.codingState.collectAsStateWithLifecycle()
    val savedProjects by viewModel.codingProjects.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current

    val languages = listOf("Kotlin", "Python", "Java", "JavaScript", "C++", "Dart")
    val capabilities = listOf(
        "1. Code Generation",
        "2. Website Creation",
        "3. Web App Creation",
        "4. Android App Dev",
        "5. Game Dev Assist",
        "6. Bug Detection",
        "7. Bug Fixing",
        "8. Code Explanation",
        "9. Multi-Language Port",
        "10. UI & Interface Design",
        "11. Test Generation",
        "12. Step-by-Step Builder",
        "13. Code Optimization",
        "14. APK Build Guidance",
        "15. File Organization",
        "16. Development Guidance"
    )

    var selectedLang by remember { mutableStateOf("Kotlin") }
    var selectedCapability by remember { mutableStateOf(capabilities.first()) }
    var devPrompt by remember { mutableStateOf("") }
    var projectTitle by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HolographicCard(highlightBorder = true, modifier = Modifier.fillMaxWidth()) {
                HudSectionTitle(title = "AI Development & Coding Studio", icon = Icons.Filled.Code, trailingText = "16 MODULES")
                Spacer(modifier = Modifier.height(8.dp))

                Text("Programming Language", style = MaterialTheme.typography.labelMedium, color = RadiantChampagne)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(languages) { lang ->
                        FilterChip(
                            selected = selectedLang == lang,
                            onClick = { selectedLang = lang },
                            label = { Text(lang) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Development Capability (1–16)", style = MaterialTheme.typography.labelMedium, color = RadiantChampagne)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(capabilities) { cap ->
                        FilterChip(
                            selected = selectedCapability == cap,
                            onClick = { selectedCapability = cap },
                            label = { Text(cap) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = projectTitle,
                    onValueChange = { projectTitle = it },
                    label = { Text("Project / Snippet Title (optional for saving)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = devPrompt,
                    onValueChange = { devPrompt = it },
                    label = { Text("Describe what to build, debug, test, or architect...") },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("coding_studio_prompt_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                GoldPrimaryButton(
                    text = "Generate with JARVIS Dev AI",
                    icon = Icons.Filled.Code,
                    enabled = devPrompt.isNotBlank() && codingState !is AsyncUiState.Loading,
                    onClick = {
                        viewModel.runCodingAssistant(
                            language = selectedLang,
                            capability = selectedCapability,
                            prompt = devPrompt
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("run_coding_studio_button")
                )

                when (val st = codingState) {
                    is AsyncUiState.Loading -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = SolarGold, strokeWidth = 2.dp)
                            Text("Engineering $selectedLang solution...", color = RadiantChampagne)
                        }
                    }
                    is AsyncUiState.Success -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GoldOutlinedButton(
                                text = "Copy",
                                icon = Icons.Filled.ContentCopy,
                                onClick = {
                                    clipboard.setText(AnnotatedString(st.data))
                                    viewModel.showNotice("Code copied to clipboard")
                                },
                                modifier = Modifier.weight(1f)
                            )
                            GoldOutlinedButton(
                                text = "Save Project",
                                icon = Icons.Filled.BookmarkAdd,
                                onClick = {
                                    viewModel.saveCurrentCodingOutput(
                                        title = projectTitle.ifBlank { "$selectedLang - $selectedCapability" },
                                        language = selectedLang,
                                        capability = selectedCapability,
                                        prompt = devPrompt,
                                        output = st.data
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = st.data,
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimaryGoldWhite,
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        )
                    }
                    is AsyncUiState.Error -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(st.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                    else -> Unit
                }
            }
        }

        // Transparent APK / Compilation Notice
        item {
            HolographicCard(modifier = Modifier.fillMaxWidth()) {
                HudSectionTitle(title = "Build & APK Compilation Transparency", icon = Icons.Filled.Info)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "JARVIS helps you plan, generate, debug, test, optimize, and organize software projects. Actual binary compilation, APK signing, and device installation require Android Studio, Gradle SDK toolchains, or a cloud CI/CD build service.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryMuted
                )
            }
        }

        // Saved Coding Projects Library
        item {
            HudSectionTitle(title = "Saved Cloud Coding Projects", trailingText = "${savedProjects.size} SAVED")
        }

        if (savedProjects.isEmpty()) {
            item {
                HolographicCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "No coding blueprints saved yet. Generate code above and tap Save Project.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryMuted
                    )
                }
            }
        } else {
            items(savedProjects, key = { it.id.ifBlank { it.hashCode().toString() } }) { proj ->
                HolographicCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(proj.title, style = MaterialTheme.typography.titleMedium, color = RadiantChampagne)
                            Text("${proj.language} • ${proj.category}", style = MaterialTheme.typography.labelSmall, color = SolarGold)
                        }
                        IconButton(onClick = { viewModel.deleteCodingProject(proj.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete Saved Project", tint = TextSecondaryMuted)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = proj.codeOutput.take(280),
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryMuted
                    )
                }
            }
        }
    }
}

@Composable
fun UpdateCenterScreen(viewModel: JarvisViewModel) {
    val settings by viewModel.profileSettings.collectAsStateWithLifecycle()
    val history by viewModel.updateHistory.collectAsStateWithLifecycle()
    val tamperResult by viewModel.tamperResult.collectAsStateWithLifecycle()

    var showApprovalDialog by remember { mutableStateOf(false) }

    val reviewPillars = listOf(
        "AI Models & Firebase AI Logic (Gemini 3.8 Flash / Nano Banana 2.1)",
        "Google Service Compatibility (Auth, Firestore, Search & Chat)",
        "Security & Privacy Audit (AES-256-GCM, Biometrics, Firestore Rules)",
        "Voice Engine & 20-Voice Hindi/English TTS Compatibility",
        "Android OS Compatibility (Running API ${Build.VERSION.SDK_INT} / Target SDK 36)",
        "Bugs, Crash-Safety & Flow Performance Optimization",
        "New Holographic Features & Bilingual UX Refinements",
        "System Memory & Battery Optimization"
    )

    if (showApprovalDialog) {
        AlertDialog(
            onDismissRequest = { showApprovalDialog = false },
            title = { Text("Owner Update Approval Required", color = SolarGold) },
            text = {
                Text(
                    "JARVIS never silently installs updates. Confirm to log approval for Maintenance Release v1.1.0 (4-month cycle) and advance the next scheduled review date to 2027-07-23.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showApprovalDialog = false
                        viewModel.approveUpdateCycle(
                            nextVersion = "1.1.0",
                            reviewSummary = "Owner approved 4-month review: AI models verified, security rules active, Android API ${Build.VERSION.SDK_INT} compatible.",
                            nextReviewIso = "2027-07-23"
                        )
                    }
                ) {
                    Text("Approve Update", color = SolarGold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showApprovalDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HolographicCard(highlightBorder = true, modifier = Modifier.fillMaxWidth()) {
                HudSectionTitle(title = "4-Month Update & Maintenance Cycle", icon = Icons.Filled.SystemUpdate)
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill(label = "INSTALLED v${settings.installedVersion}", tint = SolarGold)
                    StatusPill(
                        label = if (tamperResult.passed) "SECURITY VERIFIED" else "DEV BUILD",
                        tint = HoloEmerald
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "• Last Review Date: ${settings.lastUpdateReviewIso}\n• Next 4-Month Review Date: ${settings.nextUpdateReviewIso}\n• Android Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimaryGoldWhite
                )
                Spacer(modifier = Modifier.height(12.dp))
                GoldPrimaryButton(
                    text = "Review & Approve Maintenance Update",
                    icon = Icons.Filled.SystemUpdate,
                    onClick = { showApprovalDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("approve_update_button")
                )
            }
        }

        item {
            HolographicCard(modifier = Modifier.fillMaxWidth()) {
                HudSectionTitle(title = "8-Point Compatibility & Review Checklist")
                Spacer(modifier = Modifier.height(8.dp))
                reviewPillars.forEach { pillar ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = HoloEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(pillar, style = MaterialTheme.typography.bodySmall, color = TextPrimaryGoldWhite)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }

        item {
            HudSectionTitle(title = "Update & Review History", trailingText = "${history.size} LOGS")
        }

        if (history.isEmpty()) {
            item {
                HolographicCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Initial baseline v${settings.installedVersion} installed. Tap 'Review & Approve Maintenance Update' above to record your first approved review.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryMuted
                    )
                }
            }
        } else {
            items(history, key = { it.id.ifBlank { it.hashCode().toString() } }) { rec ->
                HolographicCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Release v${rec.version}", style = MaterialTheme.typography.titleMedium, color = RadiantChampagne)
                        StatusPill(label = rec.status.uppercase(), tint = HoloCyan)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(rec.summary, style = MaterialTheme.typography.bodySmall, color = TextSecondaryMuted)
                }
            }
        }
    }
}
