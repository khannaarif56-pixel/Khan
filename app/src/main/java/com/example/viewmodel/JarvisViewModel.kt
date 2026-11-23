package com.example.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.core.SecurityVault
import com.example.core.TamperCheckResult
import com.example.data.ai.JarvisAiService
import com.example.data.device.CoreVoiceState
import com.example.data.device.DeviceControlManager
import com.example.data.device.DocumentReadResult
import com.example.data.device.VoiceEngine
import com.example.data.device.VoiceProfile
import com.example.data.firebase.ChatMessage
import com.example.data.firebase.CodingProjectRecord
import com.example.data.firebase.JarvisProfileSettings
import com.example.data.firebase.JarvisRepository
import com.example.data.firebase.PersonalMemory
import com.example.data.firebase.ProductivityNote
import com.example.data.firebase.ProductivityTask
import com.example.data.firebase.UpdateAuditRecord
import com.example.data.firebase.VisionHistoryEntry
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "JarvisViewModel"

sealed interface AsyncUiState<out T> {
    data object Idle : AsyncUiState<Nothing>
    data object Loading : AsyncUiState<Nothing>
    data class Streaming(val text: String) : AsyncUiState<Nothing>
    data class Success<T>(val data: T) : AsyncUiState<T>
    data class Error(val message: String) : AsyncUiState<Nothing>
}

class JarvisViewModel(
    private val repository: JarvisRepository,
    private val aiService: JarvisAiService,
    val voiceEngine: VoiceEngine,
    val securityVault: SecurityVault,
    val deviceControl: DeviceControlManager,
    val currentUserId: String,
    val currentUserEmail: String
) : ViewModel() {

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _firestoreError = MutableStateFlow<String?>(null)
    val firestoreError: StateFlow<String?> = _firestoreError.asStateFlow()

    val profileSettings: StateFlow<JarvisProfileSettings> =
        repository.observeProfileSettings(currentUserId)
            .catch { error ->
                unwrapFirestoreError(error, "profile settings")
                emit(JarvisProfileSettings(userId = currentUserId))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = JarvisProfileSettings(userId = currentUserId)
            )

    val chats: StateFlow<List<ChatMessage>> =
        repository.observeChats(currentUserId)
            .map { list ->
                list.map { msg ->
                    if (msg.encrypted) msg.copy(content = securityVault.decryptText(msg.content)) else msg
                }
            }
            .flowOn(Dispatchers.IO)
            .catch { error ->
                unwrapFirestoreError(error, "chat history")
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val memories: StateFlow<List<PersonalMemory>> =
        repository.observeMemories(currentUserId)
            .map { list ->
                list.map { mem ->
                    if (mem.encrypted) mem.copy(content = securityVault.decryptText(mem.content)) else mem
                }
            }
            .flowOn(Dispatchers.IO)
            .catch { error ->
                unwrapFirestoreError(error, "personal memories")
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val tasks: StateFlow<List<ProductivityTask>> =
        repository.observeTasks(currentUserId)
            .catch { error ->
                unwrapFirestoreError(error, "tasks")
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val notes: StateFlow<List<ProductivityNote>> =
        repository.observeNotes(currentUserId)
            .catch { error ->
                unwrapFirestoreError(error, "notes")
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val visionHistory: StateFlow<List<VisionHistoryEntry>> =
        repository.observeVisionHistory(currentUserId)
            .catch { error ->
                unwrapFirestoreError(error, "vision history")
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val codingProjects: StateFlow<List<CodingProjectRecord>> =
        repository.observeCodingProjects(currentUserId)
            .catch { error ->
                unwrapFirestoreError(error, "coding projects")
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val updateHistory: StateFlow<List<UpdateAuditRecord>> =
        repository.observeUpdateHistory(currentUserId)
            .catch { error ->
                unwrapFirestoreError(error, "update history")
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Streaming AI Brain state
    private val _brainStreamState = MutableStateFlow<AsyncUiState<String>>(AsyncUiState.Idle)
    val brainStreamState: StateFlow<AsyncUiState<String>> = _brainStreamState.asStateFlow()

    // Vision & Image Generation state
    private val _selectedImageBitmap = MutableStateFlow<Bitmap?>(null)
    val selectedImageBitmap: StateFlow<Bitmap?> = _selectedImageBitmap.asStateFlow()

    private val _visionAnalysisState = MutableStateFlow<AsyncUiState<String>>(AsyncUiState.Idle)
    val visionAnalysisState: StateFlow<AsyncUiState<String>> = _visionAnalysisState.asStateFlow()

    private val _generatedImageState = MutableStateFlow<AsyncUiState<Bitmap>>(AsyncUiState.Idle)
    val generatedImageState: StateFlow<AsyncUiState<Bitmap>> = _generatedImageState.asStateFlow()

    // AI Tools state
    private val _loadedDocument = MutableStateFlow<DocumentReadResult?>(null)
    val loadedDocument: StateFlow<DocumentReadResult?> = _loadedDocument.asStateFlow()

    private val _toolsOutputState = MutableStateFlow<AsyncUiState<String>>(AsyncUiState.Idle)
    val toolsOutputState: StateFlow<AsyncUiState<String>> = _toolsOutputState.asStateFlow()

    // Worldwide Search & Google Integrations state
    private val _searchState = MutableStateFlow<AsyncUiState<String>>(AsyncUiState.Idle)
    val searchState: StateFlow<AsyncUiState<String>> = _searchState.asStateFlow()

    private val _geminiPerspectiveState = MutableStateFlow<AsyncUiState<String>>(AsyncUiState.Idle)
    val geminiPerspectiveState: StateFlow<AsyncUiState<String>> = _geminiPerspectiveState.asStateFlow()

    // AI Development Center state
    private val _codingState = MutableStateFlow<AsyncUiState<String>>(AsyncUiState.Idle)
    val codingState: StateFlow<AsyncUiState<String>> = _codingState.asStateFlow()

    // Productivity AI Weather/News state
    private val _briefingState = MutableStateFlow<AsyncUiState<String>>(AsyncUiState.Idle)
    val briefingState: StateFlow<AsyncUiState<String>> = _briefingState.asStateFlow()

    // Device controls & Security UI state
    private val _torchSupported = MutableStateFlow(false)
    val torchSupported: StateFlow<Boolean> = _torchSupported.asStateFlow()

    private val _torchEnabled = MutableStateFlow(false)
    val torchEnabled: StateFlow<Boolean> = _torchEnabled.asStateFlow()

    private val _volumeFraction = MutableStateFlow(0.5f)
    val volumeFraction: StateFlow<Float> = _volumeFraction.asStateFlow()

    private val _brightnessFraction = MutableStateFlow(0.7f)
    val brightnessFraction: StateFlow<Float> = _brightnessFraction.asStateFlow()

    private val _tamperResult = MutableStateFlow(
        TamperCheckResult(
            passed = true,
            summary = "Running integrity verification...",
            checks = emptyList()
        )
    )
    val tamperResult: StateFlow<TamperCheckResult> = _tamperResult.asStateFlow()

    private val _handsFreeEnabled = MutableStateFlow(false)
    val handsFreeEnabled: StateFlow<Boolean> = _handsFreeEnabled.asStateFlow()

    private val _wakeWordArmed = MutableStateFlow(false)
    val wakeWordArmed: StateFlow<Boolean> = _wakeWordArmed.asStateFlow()

    private val _sessionUnlocked = MutableStateFlow(true)
    val sessionUnlocked: StateFlow<Boolean> = _sessionUnlocked.asStateFlow()

    val coreVoiceState: StateFlow<CoreVoiceState> = voiceEngine.coreState
    val voiceAmplitude: StateFlow<Float> = voiceEngine.voiceAmplitude
    val partialTranscript: StateFlow<String> = voiceEngine.partialTranscript
    val voiceProfiles: StateFlow<List<VoiceProfile>> = voiceEngine.voiceProfiles
    val ttsStatusMessage: StateFlow<String> = voiceEngine.ttsStatusMessage

    init {
        viewModelScope.launch {
            repository.ensureProfileInitialized(currentUserId)
        }
        viewModelScope.launch(Dispatchers.IO) {
            _volumeFraction.value = deviceControl.getMediaVolumeFraction()
            _torchSupported.value = deviceControl.isTorchSupported()
            _tamperResult.value = securityVault.runAntiTamperDiagnostics()
        }
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun showNotice(message: String) {
        _statusBanner.value = message
    }

    fun setLanguage(langCode: String) {
        val normalized = if (langCode == "hi") "hi" else "en"
        viewModelScope.launch {
            repository.updateProfileFields(currentUserId, mapOf("primaryLanguage" to normalized))
            showNotice(if (normalized == "hi") "Language switched to Hindi (हिंदी)" else "Language switched to English")
        }
    }

    fun setMemoryEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateProfileFields(currentUserId, mapOf("memoryEnabled" to enabled))
            showNotice(if (enabled) "Personal memory permission granted" else "Personal memory paused")
        }
    }

    fun setSecureChatEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateProfileFields(currentUserId, mapOf("secureChatEnabled" to enabled))
            showNotice(if (enabled) "AES-256 encrypted chat storage enabled" else "Standard cloud chat storage active")
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateProfileFields(currentUserId, mapOf("biometricEnabled" to enabled))
        }
    }

    fun setEmergencyLocked(locked: Boolean) {
        _sessionUnlocked.value = !locked
        viewModelScope.launch {
            repository.updateProfileFields(currentUserId, mapOf("emergencyLocked" to locked))
            showNotice(if (locked) "EMERGENCY LOCKDOWN ENGAGED" else "Emergency lock released")
        }
    }

    fun unlockSession() {
        _sessionUnlocked.value = true
        if (profileSettings.value.emergencyLocked) {
            setEmergencyLocked(false)
        }
    }

    fun configurePin(pin: String): Boolean {
        if (pin.length !in 4..8 || !pin.all { it.isDigit() }) {
            showNotice("PIN must be 4 to 8 digits")
            return false
        }
        viewModelScope.launch {
            val ok = withContext(Dispatchers.Default) { securityVault.savePin(pin) }
            if (ok) {
                repository.updateProfileFields(currentUserId, mapOf("securityPinConfigured" to true))
                showNotice("Security PIN stored with PBKDF2-HMAC-SHA256")
            } else {
                showNotice("Unable to save PIN.")
            }
        }
        return true
    }

    fun configureVoicePassphrase(phrase: String): Boolean {
        if (phrase.trim().length < 3) {
            showNotice("Passphrase must be at least 3 characters")
            return false
        }
        viewModelScope.launch {
            val ok = withContext(Dispatchers.Default) { securityVault.saveVoicePassphrase(phrase) }
            if (ok) {
                repository.updateProfileFields(currentUserId, mapOf("voicePassphraseEnabled" to true))
                showNotice("Secondary voice passphrase saved")
            } else {
                showNotice("Unable to save voice passphrase.")
            }
        }
        return true
    }

    fun refreshTamperScan() {
        viewModelScope.launch {
            _tamperResult.value = withContext(Dispatchers.IO) {
                securityVault.runAntiTamperDiagnostics()
            }
            showNotice("Integrity scan completed")
        }
    }

    // --- Voice Selection & Playback ---

    fun activeVoiceProfile(): VoiceProfile? {
        val selectedId = profileSettings.value.selectedVoiceId
        return voiceProfiles.value.firstOrNull { it.id == selectedId }
            ?: voiceProfiles.value.firstOrNull { it.availableOnDevice }
    }

    fun selectVoice(profile: VoiceProfile) {
        if (!profile.availableOnDevice) {
            showNotice("Voice unavailable on device: ${profile.availabilityNote}")
            return
        }
        viewModelScope.launch {
            repository.updateProfileFields(
                currentUserId,
                mapOf(
                    "selectedVoiceId" to profile.id,
                    "voiceSpeechRate" to profile.defaultRate.toDouble(),
                    "voicePitch" to profile.defaultPitch.toDouble()
                )
            )
            showNotice("Voice switched to ${profile.name} (${profile.style})")
        }
    }

    fun toggleFavoriteVoice(voiceId: String) {
        val current = profileSettings.value.favoriteVoiceIds.toMutableList()
        if (current.contains(voiceId)) {
            current.remove(voiceId)
        } else if (current.size < 20) {
            current.add(voiceId)
        }
        viewModelScope.launch {
            repository.updateProfileFields(currentUserId, mapOf("favoriteVoiceIds" to current))
        }
    }

    fun updateVoiceTuning(rate: Float, pitch: Float) {
        val clampedRate = rate.coerceIn(0.5f, 2.0f)
        val clampedPitch = pitch.coerceIn(0.5f, 2.0f)
        viewModelScope.launch {
            repository.updateProfileFields(
                currentUserId,
                mapOf(
                    "voiceSpeechRate" to clampedRate.toDouble(),
                    "voicePitch" to clampedPitch.toDouble()
                )
            )
        }
    }

    fun previewVoice(profile: VoiceProfile, customText: String? = null) {
        if (!profile.availableOnDevice) {
            showNotice(profile.availabilityNote)
            return
        }
        val text = customText?.takeIf { it.isNotBlank() }
            ?: if (profile.languageCode == "hi") profile.sampleHindi else profile.sampleEnglish
        val spoke = voiceEngine.speak(
            text = text,
            profile = profile,
            speechRateOverride = profile.defaultRate,
            pitchOverride = profile.defaultPitch
        )
        if (!spoke) {
            showNotice("Unable to synthesize audio on this device engine.")
        }
    }

    fun speakWithActiveVoice(text: String) {
        val settings = profileSettings.value
        val profile = activeVoiceProfile()
        voiceEngine.speak(
            text = text,
            profile = profile,
            speechRateOverride = settings.voiceSpeechRate.toFloat(),
            pitchOverride = settings.voicePitch.toFloat()
        )
    }

    fun setHandsFreeMode(enabled: Boolean) {
        _handsFreeEnabled.value = enabled
        showNotice(
            if (enabled) "Foreground hands-free voice reply enabled while JARVIS is open"
            else "Hands-free auto-speak paused"
        )
    }

    fun setWakeWordArmed(armed: Boolean) {
        _wakeWordArmed.value = armed
        showNotice(
            if (armed) "In-app wake phrase ('Hey Jarvis') active while app is foregrounded"
            else "Wake phrase listener disarmed"
        )
    }

    // --- Offline Command Handling + AI Brain Streaming ---

    fun handleVoiceCommand(spokenText: String) {
        val cleaned = spokenText.trim()
        if (cleaned.isEmpty()) return

        val lower = cleaned.lowercase()
        val offlineHandled = when {
            lower.contains("torch on") || lower.contains("flashlight on") || lower.contains("टॉर्च चालू") -> {
                toggleTorch(true)
                "Torch activated."
            }
            lower.contains("torch off") || lower.contains("flashlight off") || lower.contains("टॉर्च बंद") -> {
                toggleTorch(false)
                "Torch turned off."
            }
            lower.contains("volume up") || lower.contains("आवाज बढ़ाओ") -> {
                adjustVolume(true)
                "Media volume increased."
            }
            lower.contains("volume down") || lower.contains("आवाज कम") -> {
                adjustVolume(false)
                "Media volume decreased."
            }
            lower.contains("emergency lock") || lower.contains("लॉकडाउन") -> {
                setEmergencyLocked(true)
                "Emergency lock engaged."
            }
            lower.contains("switch to hindi") || lower.contains("हिंदी मोड") -> {
                setLanguage("hi")
                "हिंदी मोड सक्रिय हो गया है।"
            }
            lower.contains("switch to english") -> {
                setLanguage("en")
                "English mode is now active."
            }
            else -> null
        }

        if (offlineHandled != null) {
            showNotice("Offline command executed: $offlineHandled")
            speakWithActiveVoice(offlineHandled)
            return
        }

        val effectivePrompt = if (_wakeWordArmed.value && lower.startsWith("hey jarvis")) {
            cleaned.removePrefix("hey jarvis").removePrefix("Hey Jarvis").trim().ifEmpty { cleaned }
        } else {
            cleaned
        }
        sendAiBrainMessage(effectivePrompt, center = "voice", autoSpeak = true)
    }

    fun sendAiBrainMessage(
        prompt: String,
        center: String = "brain",
        autoSpeak: Boolean = _handsFreeEnabled.value
    ) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) return

        val settings = profileSettings.value
        val lang = settings.primaryLanguage
        val encrypt = settings.secureChatEnabled
        val historySnapshot = chats.value.takeLast(8).map { it.role to it.content }
        val memorySnapshot = if (settings.memoryEnabled) {
            memories.value.map { "${it.category}: ${it.content}" }
        } else {
            emptyList()
        }

        viewModelScope.launch {
            _brainStreamState.value = AsyncUiState.Loading
            voiceEngine.setThinking(true)
            val storedUserText = if (encrypt) {
                withContext(Dispatchers.IO) { securityVault.encryptText(trimmed) }
            } else {
                trimmed
            }
            repository.addChatMessage(
                userId = currentUserId,
                role = "user",
                content = storedUserText,
                language = lang,
                center = center,
                encrypted = encrypt
            )

            var finalReply = ""
            try {
                aiService.streamConversation(
                    prompt = trimmed,
                    languageCode = lang,
                    recentHistory = historySnapshot,
                    approvedMemories = memorySnapshot
                ).collect { partial ->
                    finalReply = partial
                    _brainStreamState.value = AsyncUiState.Streaming(partial)
                }

                voiceEngine.setThinking(false)
                _brainStreamState.value = AsyncUiState.Success(finalReply)

                val storedReply = if (encrypt) {
                    withContext(Dispatchers.IO) { securityVault.encryptText(finalReply) }
                } else {
                    finalReply
                }
                repository.addChatMessage(
                    userId = currentUserId,
                    role = "model",
                    content = storedReply,
                    language = lang,
                    center = center,
                    encrypted = encrypt
                )

                if (autoSpeak) {
                    speakWithActiveVoice(finalReply)
                }
            } catch (e: CancellationException) {
                voiceEngine.setThinking(false)
                throw e
            } catch (e: Exception) {
                voiceEngine.setThinking(false)
                Log.e(TAG, "AI Brain conversation failed", e)
                _brainStreamState.value = AsyncUiState.Error(
                    e.localizedMessage ?: "Failed to connect to JARVIS AI Core."
                )
            }
        }
    }

    // --- Personal Memory ---

    fun addPersonalMemory(category: String, content: String, encrypt: Boolean = true) {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val stored = if (encrypt) {
                withContext(Dispatchers.IO) { securityVault.encryptText(trimmed) }
            } else {
                trimmed
            }
            repository.addMemory(currentUserId, category, stored, encrypt)
                .onSuccess { showNotice("Memory saved${if (encrypt) " (AES-256 encrypted)" else ""}") }
                .onFailure { showNotice(it.localizedMessage ?: "Failed to save memory") }
        }
    }

    fun deletePersonalMemory(id: String) {
        viewModelScope.launch { repository.deleteMemory(currentUserId, id) }
    }

    // --- AI Vision & Image Center ---

    fun selectImageUri(uri: Uri) {
        viewModelScope.launch {
            val bmp = withContext(Dispatchers.IO) { deviceControl.loadBitmapFromUri(uri) }
            if (bmp != null) {
                _selectedImageBitmap.value = bmp
                _visionAnalysisState.value = AsyncUiState.Idle
            } else {
                showNotice("Unable to decode selected image.")
            }
        }
    }

    fun analyzeSelectedImage(question: String) {
        val bmp = _selectedImageBitmap.value
        if (bmp == null) {
            showNotice("Please select an image first.")
            return
        }
        val lang = profileSettings.value.primaryLanguage
        viewModelScope.launch {
            _visionAnalysisState.value = AsyncUiState.Loading
            voiceEngine.setThinking(true)
            try {
                val result = aiService.analyzeImage(bmp, question, lang)
                voiceEngine.setThinking(false)
                _visionAnalysisState.value = AsyncUiState.Success(result)
                repository.addVisionHistory(
                    userId = currentUserId,
                    mode = "analysis",
                    prompt = question.ifBlank { "Scene & object description" },
                    resultSummary = result,
                    savedLocally = false
                )
            } catch (e: CancellationException) {
                voiceEngine.setThinking(false)
                throw e
            } catch (e: Exception) {
                voiceEngine.setThinking(false)
                _visionAnalysisState.value = AsyncUiState.Error(
                    e.localizedMessage ?: "Image analysis failed."
                )
            }
        }
    }

    fun generateAiImage(prompt: String) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            _generatedImageState.value = AsyncUiState.Loading
            voiceEngine.setThinking(true)
            try {
                val bitmap = aiService.generateImage(trimmed)
                voiceEngine.setThinking(false)
                _generatedImageState.value = AsyncUiState.Success(bitmap)
                repository.addVisionHistory(
                    userId = currentUserId,
                    mode = "generation",
                    prompt = trimmed,
                    resultSummary = "Generated AI image (${bitmap.width}x${bitmap.height})",
                    savedLocally = false
                )
            } catch (e: CancellationException) {
                voiceEngine.setThinking(false)
                throw e
            } catch (e: Exception) {
                voiceEngine.setThinking(false)
                _generatedImageState.value = AsyncUiState.Error(
                    e.localizedMessage ?: "AI image generation failed."
                )
            }
        }
    }

    fun saveGeneratedImageToGallery(bitmap: Bitmap) {
        viewModelScope.launch {
            val res = withContext(Dispatchers.IO) { deviceControl.saveBitmapToGallery(bitmap) }
            res.onSuccess { filename ->
                showNotice("Saved to Pictures/Jarvis/$filename")
            }.onFailure { err ->
                showNotice(err.localizedMessage ?: "Unable to save image to gallery.")
            }
        }
    }

    // --- AI Tools Center ---

    fun openDocumentUri(uri: Uri) {
        viewModelScope.launch {
            val res = withContext(Dispatchers.IO) { deviceControl.readDocumentFromUri(uri) }
            res.onSuccess { doc ->
                _loadedDocument.value = doc
                showNotice("Loaded ${doc.fileName} (${doc.pageCount} page${if (doc.pageCount == 1) "" else "s"})")
            }.onFailure { err ->
                showNotice(err.localizedMessage ?: "Could not open document.")
            }
        }
    }

    fun runAiTool(toolName: String, inputText: String, targetLanguage: String = "Hindi") {
        val lang = profileSettings.value.primaryLanguage
        val doc = _loadedDocument.value

        viewModelScope.launch {
            _toolsOutputState.value = AsyncUiState.Loading
            voiceEngine.setThinking(true)
            try {
                val result = if (toolName == "PDF / Doc Reader" && doc?.previewBitmap != null) {
                    aiService.analyzeImage(
                        bitmap = doc.previewBitmap,
                        question = inputText.ifBlank {
                            "Read this PDF page carefully, extract key points, and provide a structured summary."
                        },
                        languageCode = lang
                    )
                } else {
                    val prompt = when (toolName) {
                        "PDF / Doc Reader" -> {
                            val source = doc?.extractedText ?: inputText
                            "Summarize and extract key insights from this document:\n$source\n\nUser focus: ${inputText.ifBlank { "Full summary and action items" }}"
                        }
                        "Translator" -> "Translate the following text accurately into $targetLanguage, preserving tone and nuance:\n\n$inputText"
                        "Math Solver" -> "Solve this math problem step-by-step with clear formulas and final verified answer:\n\n$inputText"
                        "Study Tutor" -> "Act as a patient expert tutor. Explain this topic with simple analogies, key definitions, and 3 quick practice questions:\n\n$inputText"
                        "Summarizer" -> "Provide a concise executive summary with bullet points and key takeaways:\n\n$inputText"
                        else -> "Assist with this technical / coding request with clear examples:\n\n$inputText"
                    }
                    aiService.generateSingleResponse(prompt, lang)
                }
                voiceEngine.setThinking(false)
                _toolsOutputState.value = AsyncUiState.Success(result)
            } catch (e: CancellationException) {
                voiceEngine.setThinking(false)
                throw e
            } catch (e: Exception) {
                voiceEngine.setThinking(false)
                _toolsOutputState.value = AsyncUiState.Error(e.localizedMessage ?: "Tool execution failed.")
            }
        }
    }

    // --- Mobile Control Center ---

    fun toggleTorch(enable: Boolean) {
        deviceControl.setTorchEnabled(enable)
            .onSuccess {
                _torchEnabled.value = it
                showNotice(if (it) "Torch enabled" else "Torch disabled")
            }
            .onFailure {
                _torchEnabled.value = false
                showNotice(it.localizedMessage ?: "Torch control unavailable on this device.")
            }
    }

    fun setVolume(fraction: Float) {
        _volumeFraction.value = deviceControl.setMediaVolumeFraction(fraction)
    }

    fun adjustVolume(up: Boolean) {
        _volumeFraction.value = deviceControl.adjustVolumeStep(up)
    }

    fun updateBrightnessState(fraction: Float) {
        _brightnessFraction.value = fraction
    }

    // --- Productivity Center ---

    fun addTask(title: String, notes: String, category: String, dueLabel: String) {
        viewModelScope.launch {
            repository.addTask(currentUserId, title, notes, category, dueLabel)
                .onSuccess { showNotice("${if (category == "reminder") "Reminder" else "Task"} saved") }
                .onFailure { showNotice(it.localizedMessage ?: "Failed to add item") }
        }
    }

    fun toggleTask(task: ProductivityTask) {
        viewModelScope.launch {
            repository.toggleTaskCompleted(currentUserId, task.id, !task.completed)
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch { repository.deleteTask(currentUserId, taskId) }
    }

    fun addNote(title: String, content: String, tag: String) {
        viewModelScope.launch {
            repository.addNote(currentUserId, title, content, tag)
                .onSuccess { showNotice("Note saved to cloud") }
                .onFailure { showNotice(it.localizedMessage ?: "Failed to save note") }
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch { repository.deleteNote(currentUserId, noteId) }
    }

    fun requestBriefing(topic: String, cityOrFocus: String) {
        val lang = profileSettings.value.primaryLanguage
        viewModelScope.launch {
            _briefingState.value = AsyncUiState.Loading
            try {
                val prompt = if (topic == "Weather") {
                    "Provide a helpful seasonal weather guide, preparation tips, and air-quality/clothing checklist for ${cityOrFocus.ifBlank { "Delhi, India" }}. Note clearly that live sensor readings require a live meteorological station."
                } else {
                    "Provide a structured global & technology news research briefing covering major themes in ${cityOrFocus.ifBlank { "AI, Mobile Technology, Science & India" }}."
                }
                val result = aiService.generateSingleResponse(prompt, lang, useLite = false)
                _briefingState.value = AsyncUiState.Success(result)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _briefingState.value = AsyncUiState.Error(e.localizedMessage ?: "Briefing request failed.")
            }
        }
    }

    // --- Worldwide Search & Google Integrations ---

    fun runWorldwideSearch(query: String, mode: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        val lang = profileSettings.value.primaryLanguage
        viewModelScope.launch {
            _searchState.value = AsyncUiState.Loading
            voiceEngine.setThinking(true)
            try {
                val prompt = """
                    Perform a comprehensive Worldwide Research & Knowledge Synthesis for the query below.
                    Category: $mode (Websites & Articles / News & Research / Executive Summary).
                    Provide:
                    1. Concise Executive Summary
                    2. Key Facts & Context
                    3. Recommended official search keywords & authoritative domains to explore further
                    Query: $trimmed
                """.trimIndent()
                val res = aiService.generateSingleResponse(prompt, lang)
                voiceEngine.setThinking(false)
                _searchState.value = AsyncUiState.Success(res)
            } catch (e: CancellationException) {
                voiceEngine.setThinking(false)
                throw e
            } catch (e: Exception) {
                voiceEngine.setThinking(false)
                _searchState.value = AsyncUiState.Error(e.localizedMessage ?: "Search synthesis failed.")
            }
        }
    }

    fun runGeminiMultiPerspective(prompt: String) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) return
        val lang = profileSettings.value.primaryLanguage
        viewModelScope.launch {
            _geminiPerspectiveState.value = AsyncUiState.Loading
            try {
                val structuredPrompt = """
                    Provide a Google Gemini Research & Alternative Perspectives analysis for:
                    "$trimmed"
                    Include:
                    - Analytical Viewpoint
                    - Practical / Real-world Trade-offs
                    - Alternative Approach or Counter-perspective
                    - Recommended Action Plan
                """.trimIndent()
                val res = aiService.generateSingleResponse(structuredPrompt, lang)
                _geminiPerspectiveState.value = AsyncUiState.Success(res)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _geminiPerspectiveState.value = AsyncUiState.Error(e.localizedMessage ?: "Analysis failed.")
            }
        }
    }

    // --- AI Development & Coding Center ---

    fun runCodingAssistant(
        language: String,
        capability: String,
        prompt: String,
        saveTitle: String? = null
    ) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) return
        val userLang = profileSettings.value.primaryLanguage
        viewModelScope.launch {
            _codingState.value = AsyncUiState.Loading
            voiceEngine.setThinking(true)
            try {
                val fullPrompt = """
                    You are JARVIS AI Development & Software Architecture Studio.
                    Target Programming Language / Stack: $language
                    Requested Capability: $capability
                    Instructions:
                    - Deliver clean, well-structured, idiomatic code or architecture blueprints.
                    - If asked for APK build / compilation, clearly explain exact Gradle/CLI steps and note that full signing/compilation requires Android SDK or cloud CI build tools.
                    - Include file organization, edge-case handling, and verification tests where appropriate.
                    User Specification:
                    $trimmed
                """.trimIndent()
                val output = aiService.generateSingleResponse(fullPrompt, userLang)
                voiceEngine.setThinking(false)
                _codingState.value = AsyncUiState.Success(output)
                if (!saveTitle.isNullOrBlank()) {
                    repository.addCodingProject(
                        userId = currentUserId,
                        title = saveTitle,
                        language = language,
                        category = capability,
                        prompt = trimmed,
                        codeOutput = output
                    )
                }
            } catch (e: CancellationException) {
                voiceEngine.setThinking(false)
                throw e
            } catch (e: Exception) {
                voiceEngine.setThinking(false)
                _codingState.value = AsyncUiState.Error(e.localizedMessage ?: "Code generation failed.")
            }
        }
    }

    fun saveCurrentCodingOutput(title: String, language: String, capability: String, prompt: String, output: String) {
        viewModelScope.launch {
            repository.addCodingProject(currentUserId, title, language, capability, prompt, output)
                .onSuccess { showNotice("Saved coding project '$title'") }
                .onFailure { showNotice(it.localizedMessage ?: "Unable to save coding project") }
        }
    }

    fun deleteCodingProject(id: String) {
        viewModelScope.launch { repository.deleteCodingProject(currentUserId, id) }
    }

    // --- Update Center (4-Month Review Cycle) ---

    fun approveUpdateCycle(nextVersion: String, reviewSummary: String, nextReviewIso: String) {
        viewModelScope.launch {
            repository.logUpdateReview(
                userId = currentUserId,
                version = nextVersion,
                summary = reviewSummary,
                status = "approved"
            )
            repository.updateProfileFields(
                currentUserId,
                mapOf(
                    "installedVersion" to nextVersion,
                    "lastUpdateReviewIso" to "2026-11-23",
                    "nextUpdateReviewIso" to nextReviewIso
                )
            )
            showNotice("Update $nextVersion approved by owner. Next 4-month review scheduled for $nextReviewIso.")
        }
    }

    private fun unwrapFirestoreError(error: Throwable, label: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val root = firestoreError ?: error
        Log.w(TAG, "Error observing $label", root)
        _firestoreError.value = "Sync notice ($label): ${root.message ?: "Unable to connect"}"
    }

    override fun onCleared() {
        super.onCleared()
        voiceEngine.release()
    }

    companion object {
        fun provideFactory(
            currentUserId: String,
            currentUserEmail: String
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                JarvisViewModel(
                    repository = JarvisRepository(app),
                    aiService = JarvisAiService(),
                    voiceEngine = VoiceEngine(app),
                    securityVault = SecurityVault(app),
                    deviceControl = DeviceControlManager(app),
                    currentUserId = currentUserId,
                    currentUserEmail = currentUserEmail
                )
            }
        }
    }
}
