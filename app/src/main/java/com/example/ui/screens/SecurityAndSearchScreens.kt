package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.signOutJarvis
import com.example.ui.components.GoldOutlinedButton
import com.example.ui.components.GoldPrimaryButton
import com.example.ui.components.HolographicCard
import com.example.ui.components.HudSectionTitle
import com.example.ui.components.StatusPill
import com.example.ui.theme.HoloAmberWarn
import com.example.ui.theme.HoloCrimson
import com.example.ui.theme.HoloCyan
import com.example.ui.theme.HoloEmerald
import com.example.ui.theme.RadiantChampagne
import com.example.ui.theme.SolarGold
import com.example.ui.theme.TextPrimaryGoldWhite
import com.example.ui.theme.TextSecondaryMuted
import com.example.viewmodel.AsyncUiState
import com.example.viewmodel.JarvisViewModel

@Composable
fun HighSecurityScreen(viewModel: JarvisViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val settings by viewModel.profileSettings.collectAsStateWithLifecycle()
    val tamperResult by viewModel.tamperResult.collectAsStateWithLifecycle()
    val memories by viewModel.memories.collectAsStateWithLifecycle()

    val (bioReady, bioNote) = remember { viewModel.securityVault.biometricAvailabilityLabel() }
    val micGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED
    val camGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED

    var pinDraft by remember { mutableStateOf("") }
    var voicePassDraft by remember { mutableStateOf("") }
    var voiceVerifyDraft by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 10. Emergency Lock + 5. Owner Authorization
        item {
            HolographicCard(highlightBorder = true, modifier = Modifier.fillMaxWidth()) {
                HudSectionTitle(title = "Owner Authorization & Emergency Lock", icon = Icons.Filled.Shield)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusPill(label = "OWNER: ${settings.ownerDisplayName}", tint = SolarGold)
                    StatusPill(label = "GOOGLE AUTH VERIFIED", tint = HoloEmerald)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Verified Account: ${viewModel.currentUserEmail.ifBlank { viewModel.currentUserId }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondaryMuted
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GoldPrimaryButton(
                        text = "Emergency Lock",
                        icon = Icons.Filled.Lock,
                        onClick = { viewModel.setEmergencyLocked(true) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("emergency_lock_button")
                    )
                    GoldOutlinedButton(
                        text = "Sign Out",
                        icon = Icons.Filled.Logout,
                        onClick = { signOutJarvis(credentialManager, scope) },
                        modifier = Modifier
                            .weight(0.65f)
                            .testTag("security_sign_out_button")
                    )
                }
            }
        }

        // 2 & 4. Biometric (Fingerprint & Face Authentication)
        item {
            HolographicCard(modifier = Modifier.fillMaxWidth()) {
                HudSectionTitle(title = "2 & 4. Fingerprint & Face Biometrics", icon = Icons.Filled.Fingerprint)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$bioNote. Uses Android BiometricPrompt hardware keystore — never relies on camera-only face checks for sensitive operations.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryMuted
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Require Biometrics for Sensitive Actions", style = MaterialTheme.typography.titleMedium)
                    Switch(
                        checked = settings.biometricEnabled,
                        onCheckedChange = { viewModel.setBiometricEnabled(it) }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                GoldOutlinedButton(
                    text = "Test BiometricPrompt Now",
                    icon = Icons.Filled.Fingerprint,
                    onClick = {
                        val activity = context as? FragmentActivity
                        if (activity != null) {
                            viewModel.securityVault.authenticateWithBiometric(
                                activity = activity,
                                onSuccess = { viewModel.showNotice("Biometric owner identity verified!") },
                                onError = { msg -> viewModel.showNotice("Biometric notice: $msg") }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 3. PIN Protection (PBKDF2-HMAC-SHA256)
        item {
            HolographicCard(modifier = Modifier.fillMaxWidth()) {
                HudSectionTitle(title = "3. Owner PIN Protection (PBKDF2-SHA256)", icon = Icons.Filled.Lock)
                Spacer(modifier = Modifier.height(6.dp))
                StatusPill(
                    label = if (viewModel.securityVault.isPinConfigured()) "PIN ACTIVE" else "PIN NOT SET",
                    tint = if (viewModel.securityVault.isPinConfigured()) HoloEmerald else HoloAmberWarn
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = pinDraft,
                    onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) pinDraft = it },
                    label = { Text("Set or Update 4–8 Digit PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("security_pin_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                GoldPrimaryButton(
                    text = "Save Hashed PIN",
                    enabled = pinDraft.length in 4..8,
                    onClick = {
                        if (viewModel.configurePin(pinDraft)) pinDraft = ""
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_pin_button")
                )
            }
        }

        // 1. Voice Passphrase (Secondary Convenience Only)
        item {
            HolographicCard(modifier = Modifier.fillMaxWidth()) {
                HudSectionTitle(title = "1. Voice Passphrase Check (Secondary Only)")
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Security Note: Voice recognition alone is never used for high-risk authorization. Use alongside Android Biometrics or PIN.",
                    style = MaterialTheme.typography.bodySmall,
                    color = HoloAmberWarn
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = voicePassDraft,
                    onValueChange = { voicePassDraft = it },
                    label = { Text("Set Voice Passphrase (e.g., golden horizon)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                GoldOutlinedButton(
                    text = "Save Voice Passphrase",
                    enabled = voicePassDraft.trim().length >= 3,
                    onClick = {
                        if (viewModel.configureVoicePassphrase(voicePassDraft)) voicePassDraft = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                if (viewModel.securityVault.isVoicePassphraseConfigured()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = voiceVerifyDraft,
                            onValueChange = { voiceVerifyDraft = it },
                            label = { Text("Test Passphrase") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        GoldPrimaryButton(
                            text = "Verify",
                            onClick = {
                                val ok = viewModel.securityVault.verifyVoicePassphrase(voiceVerifyDraft)
                                viewModel.showNotice(if (ok) "Voice passphrase matched." else "Voice passphrase did not match.")
                            }
                        )
                    }
                }
            }
        }

        // 6 & 7. Encrypted Personal Memory & Secure Chat History
        item {
            HolographicCard(modifier = Modifier.fillMaxWidth()) {
                HudSectionTitle(title = "6 & 7. AES-256 Encrypted Memory & Secure Chat", icon = Icons.Filled.Security)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Encrypt Cloud Chat History (AES-256-GCM)", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "New messages are encrypted with your Android Keystore key before Firestore upload.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryMuted
                        )
                    }
                    Switch(
                        checked = settings.secureChatEnabled,
                        onCheckedChange = { viewModel.setSecureChatEnabled(it) }
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Encrypted Memory Vault: ${memories.count { it.encrypted }}/${memories.size} memories protected by Android Keystore.",
                    style = MaterialTheme.typography.labelSmall,
                    color = HoloEmerald
                )
            }
        }

        // 8. Permission Management
        item {
            HolographicCard(modifier = Modifier.fillMaxWidth()) {
                HudSectionTitle(title = "8. Permission Management", icon = Icons.Filled.Settings)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Microphone (RECORD_AUDIO): ${if (micGranted) "GRANTED" else "DENIED / NOT REQUESTED"}\n• Camera/Flash (CAMERA): ${if (camGranted) "GRANTED" else "DENIED / NOT REQUESTED"}\n• Photo & Document Access: Zero-Permission System Pickers (Play Compliant)",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimaryGoldWhite
                )
                Spacer(modifier = Modifier.height(10.dp))
                GoldOutlinedButton(
                    text = "Open Android App Permission Settings",
                    icon = Icons.Filled.Settings,
                    onClick = { viewModel.deviceControl.openAppDetailsSettings() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 9. Anti-Tampering Alerts
        item {
            HolographicCard(modifier = Modifier.fillMaxWidth()) {
                HudSectionTitle(title = "9. Anti-Tampering Integrity Scanner", icon = Icons.Filled.VerifiedUser)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = tamperResult.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (tamperResult.passed) HoloEmerald else HoloAmberWarn
                )
                Spacer(modifier = Modifier.height(8.dp))
                tamperResult.checks.forEach { (checkName, ok) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (ok) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                            contentDescription = null,
                            tint = if (ok) HoloEmerald else HoloCrimson,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(checkName, style = MaterialTheme.typography.bodySmall, color = TextPrimaryGoldWhite)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                GoldOutlinedButton(
                    text = "Re-Run Integrity Scan",
                    onClick = { viewModel.refreshTamperScan() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun WorldwideSearchScreen(viewModel: JarvisViewModel) {
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val settings by viewModel.profileSettings.collectAsStateWithLifecycle()

    var query by remember { mutableStateOf("") }
    var searchCategory by remember { mutableStateOf("Websites & Articles") }
    val categories = listOf("Websites & Articles", "News & Research", "Quick Summary", "Academic Deep Dive")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HolographicCard(highlightBorder = true, modifier = Modifier.fillMaxWidth()) {
                HudSectionTitle(title = "Worldwide Knowledge & Web Search", icon = Icons.Filled.Public)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Combines AI research synthesis in ${if (settings.primaryLanguage == "hi") "Hindi" else "English"} with direct official Google Search web launch.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryMuted
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = searchCategory == cat,
                            onClick = { searchCategory = cat },
                            label = { Text(cat) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search worldwide topics, news, or research...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("worldwide_search_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GoldPrimaryButton(
                        text = "AI Research Summary",
                        icon = Icons.Filled.Search,
                        enabled = query.isNotBlank() && searchState !is AsyncUiState.Loading,
                        onClick = { viewModel.runWorldwideSearch(query, searchCategory) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("run_worldwide_search_button")
                    )
                    GoldOutlinedButton(
                        text = "Open Web",
                        icon = Icons.Filled.OpenInBrowser,
                        enabled = query.isNotBlank(),
                        onClick = {
                            val encoded = Uri.encode(query.trim())
                            viewModel.deviceControl.openWebUrl("https://www.google.com/search?q=$encoded")
                        },
                        modifier = Modifier.weight(0.55f)
                    )
                }

                when (val st = searchState) {
                    is AsyncUiState.Loading -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        CircularProgressIndicator(color = SolarGold, modifier = Modifier.size(24.dp))
                    }
                    is AsyncUiState.Success -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(st.data, style = MaterialTheme.typography.bodyMedium, color = TextPrimaryGoldWhite)
                    }
                    is AsyncUiState.Error -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(st.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                    else -> Unit
                }
            }
        }
    }
}

@Composable
fun GoogleIntegrationsScreen(
    viewModel: JarvisViewModel,
    onOpenWorldwideSearch: () -> Unit
) {
    val geminiState by viewModel.geminiPerspectiveState.collectAsStateWithLifecycle()
    var perspectivePrompt by remember { mutableStateOf("") }
    var chatDraft by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Google Search Integration Card (Kept separate from Chat)
        item {
            HolographicCard(modifier = Modifier.fillMaxWidth()) {
                HudSectionTitle(title = "Google Search Service", icon = Icons.Filled.Search, trailingText = "SEPARATE MODULE")
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Web search, articles, news, and Hindi/English research summaries run independently from messaging workflows.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryMuted
                )
                Spacer(modifier = Modifier.height(10.dp))
                GoldOutlinedButton(
                    text = "Open Worldwide Google Search Center",
                    icon = Icons.Filled.Public,
                    onClick = onOpenWorldwideSearch,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 2. Google Gemini AI Multi-Perspective Research
        item {
            HolographicCard(highlightBorder = true, modifier = Modifier.fillMaxWidth()) {
                HudSectionTitle(title = "Google Gemini AI — Multi-Perspective Lab", icon = Icons.Filled.Hub)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Powered securely by Firebase AI Logic (App Check attested — no hardcoded keys). Generates structured analysis and alternative perspectives.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryMuted
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = perspectivePrompt,
                    onValueChange = { perspectivePrompt = it },
                    label = { Text("Enter topic for multi-perspective Gemini analysis...") },
                    minLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gemini_perspective_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                GoldPrimaryButton(
                    text = "Run Multi-Perspective Analysis",
                    enabled = perspectivePrompt.isNotBlank() && geminiState !is AsyncUiState.Loading,
                    onClick = { viewModel.runGeminiMultiPerspective(perspectivePrompt) },
                    modifier = Modifier.fillMaxWidth()
                )

                when (val st = geminiState) {
                    is AsyncUiState.Loading -> {
                        Spacer(modifier = Modifier.height(10.dp))
                        CircularProgressIndicator(color = SolarGold, modifier = Modifier.size(22.dp))
                    }
                    is AsyncUiState.Success -> {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(st.data, style = MaterialTheme.typography.bodyMedium, color = TextPrimaryGoldWhite)
                    }
                    is AsyncUiState.Error -> {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(st.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                    else -> Unit
                }
            }
        }

        // 3. Google Chat (Explicitly separated messaging integration with account control)
        item {
            HolographicCard(modifier = Modifier.fillMaxWidth()) {
                HudSectionTitle(title = "Google Chat — Workspace Messaging", icon = Icons.Filled.Chat, trailingText = "ISOLATED")
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Kept strictly separate from Web Search. Draft a message or launch your authorized Google Chat workspace directly with explicit owner control.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryMuted
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = chatDraft,
                    onValueChange = { chatDraft = it },
                    label = { Text("Draft Google Chat update or team message...") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                GoldOutlinedButton(
                    text = "Open Official Google Chat App / Web",
                    icon = Icons.Filled.Chat,
                    onClick = {
                        viewModel.deviceControl.launchPackageOrWeb(
                            packageName = "com.google.android.apps.dynamite",
                            fallbackUrl = "https://chat.google.com"
                        ).onSuccess { res -> viewModel.showNotice("Google Chat: $res") }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
