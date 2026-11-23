package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.core.SecurityVault
import com.example.core.onGoogleSignInClicked
import com.example.data.device.CoreVoiceState
import com.example.ui.components.GoldOutlinedButton
import com.example.ui.components.GoldPrimaryButton
import com.example.ui.components.GoldenHolographicCore
import com.example.ui.components.HolographicCard
import com.example.ui.components.JarvisHudBackground
import com.example.ui.theme.HoloCrimson
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.RadiantChampagne
import com.example.ui.theme.SolarGold
import com.example.ui.theme.TextSecondaryMuted

@Composable
fun JarvisAuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    JarvisHudBackground(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp)
        ) {
            // Upper-right golden owner badge
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Text(
                    text = "aarif khann",
                    style = MaterialTheme.typography.labelMedium,
                    color = SolarGold,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("auth_owner_badge")
                )
                Text(
                    text = "AUTHORIZED CORE",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondaryMuted
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .widthIn(max = 460.dp)
                    .fillMaxWidth()
            ) {
                GoldenHolographicCore(
                    coreState = if (isLoading) CoreVoiceState.THINKING else CoreVoiceState.IDLE,
                    amplitude = 0.25f,
                    coreSize = 210.dp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "J.A.R.V.I.S",
                    style = MaterialTheme.typography.displayLarge,
                    color = SolarGold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Golden Holographic Personal AI Assistant\nHindi & English • Cloud Sync • High Security",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryMuted,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(26.dp))

                HolographicCard(highlightBorder = true, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = SolarGold
                        )
                        Text(
                            text = "Owner Verification Required",
                            style = MaterialTheme.typography.titleMedium,
                            color = RadiantChampagne
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sign in with Google to unlock encrypted personal memory, cloud chat history, productivity sync, and Gemini AI services.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryMuted
                    )

                    if (!errorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = HoloCrimson
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            isLoading = true
                            errorMessage = null
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isLoading = false
                                    onAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isLoading = false
                                    errorMessage = msg
                                },
                                scope = scope,
                                onAuthCancelled = {
                                    isLoading = false
                                }
                            )
                        },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SolarGold,
                            contentColor = ObsidianBlack
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("google_sign_in_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = ObsidianBlack,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Sign in with Google",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmergencyLockScreen(
    securityVault: SecurityVault,
    ownerEmail: String,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pinInput by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf<String?>(null) }
    val hasPin = remember { securityVault.isPinConfigured() }

    JarvisHudBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = "Emergency Lock Engaged",
                tint = HoloCrimson,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "EMERGENCY LOCKDOWN",
                style = MaterialTheme.typography.headlineMedium,
                color = HoloCrimson
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "JARVIS Core is secured for $ownerEmail.\nAuthenticate via Android Biometrics or Owner PIN to resume.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondaryMuted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            HolographicCard(
                highlightBorder = true,
                modifier = Modifier
                    .widthIn(max = 440.dp)
                    .fillMaxWidth()
            ) {
                if (hasPin) {
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) pinInput = it },
                        label = { Text("Enter Security PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lock_pin_input")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    GoldPrimaryButton(
                        text = "Unlock with PIN",
                        onClick = {
                            val candidate = pinInput
                            scope.launch {
                                val verified = withContext(Dispatchers.Default) {
                                    securityVault.verifyPin(candidate)
                                }
                                if (verified) {
                                    onUnlocked()
                                } else {
                                    feedback = "Incorrect PIN. Try again."
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("unlock_pin_button")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                GoldOutlinedButton(
                    text = "Verify Fingerprint / Face / Device",
                    icon = Icons.Filled.Fingerprint,
                    onClick = {
                        val activity = context as? FragmentActivity
                        if (activity != null) {
                            securityVault.authenticateWithBiometric(
                                activity = activity,
                                onSuccess = onUnlocked,
                                onError = { msg -> feedback = msg }
                            )
                        } else {
                            feedback = "Biometric prompt unavailable in current host."
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("unlock_biometric_button")
                )

                if (!hasPin) {
                    Spacer(modifier = Modifier.height(10.dp))
                    GoldPrimaryButton(
                        text = "Owner Override Unlock (No PIN Set)",
                        onClick = onUnlocked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("unlock_owner_override_button")
                    )
                }

                if (!feedback.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = feedback!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = HoloCrimson
                    )
                }
            }
        }
    }
}
