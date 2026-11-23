package com.example.data.device

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

enum class CoreVoiceState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}

class VoiceEngine(context: Context) {
    private val appContext = context.applicationContext
    private val bgScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var tts: TextToSpeech? = null
    @Volatile
    private var ttsReady = false
    private var speechRecognizer: SpeechRecognizer? = null

    private val _coreState = MutableStateFlow(CoreVoiceState.IDLE)
    val coreState: StateFlow<CoreVoiceState> = _coreState.asStateFlow()

    private val _voiceAmplitude = MutableStateFlow(0f)
    val voiceAmplitude: StateFlow<Float> = _voiceAmplitude.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _voiceProfiles = MutableStateFlow(VoiceCatalog.baseProfiles)
    val voiceProfiles: StateFlow<List<VoiceProfile>> = _voiceProfiles.asStateFlow()

    private val _ttsStatusMessage = MutableStateFlow("Initializing Android Speech Engine...")
    val ttsStatusMessage: StateFlow<String> = _ttsStatusMessage.asStateFlow()

    private val _speechRecognizerAvailable = MutableStateFlow(true)
    val speechRecognizerAvailable: StateFlow<Boolean> = _speechRecognizerAvailable.asStateFlow()

    @Volatile
    private var deviceVoices: List<Voice> = emptyList()

    init {
        bgScope.launch {
            _speechRecognizerAvailable.value = try {
                SpeechRecognizer.isRecognitionAvailable(appContext)
            } catch (_: Exception) {
                false
            }
        }
        initTextToSpeech()
    }

    private fun initTextToSpeech() {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _coreState.value = CoreVoiceState.SPEAKING
                        _voiceAmplitude.value = 0.65f
                    }

                    override fun onDone(utteranceId: String?) {
                        if (_coreState.value == CoreVoiceState.SPEAKING) {
                            _coreState.value = CoreVoiceState.IDLE
                        }
                        _voiceAmplitude.value = 0f
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        if (_coreState.value == CoreVoiceState.SPEAKING) {
                            _coreState.value = CoreVoiceState.IDLE
                        }
                        _voiceAmplitude.value = 0f
                    }
                })
                bgScope.launch {
                    refreshAvailableDeviceVoices()
                }
            } else {
                ttsReady = false
                _ttsStatusMessage.value = "Android TTS engine unavailable on this device."
                _voiceProfiles.value = VoiceCatalog.baseProfiles.map {
                    it.copy(
                        availableOnDevice = false,
                        availabilityNote = "Speech engine not installed on device"
                    )
                }
            }
        }
    }

    fun refreshAvailableDeviceVoices() {
        val engine = tts ?: return
        if (!ttsReady) return
        val voices = try {
            engine.voices?.toList().orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
        deviceVoices = voices

        val enAvailable = engine.isLanguageAvailable(Locale.US) >= TextToSpeech.LANG_AVAILABLE ||
            engine.isLanguageAvailable(Locale.ENGLISH) >= TextToSpeech.LANG_AVAILABLE
        val hiAvailable = engine.isLanguageAvailable(Locale.forLanguageTag("hi-IN")) >= TextToSpeech.LANG_AVAILABLE

        val enVoices = voices.filter { it.locale?.language == "en" }
        val hiVoices = voices.filter { it.locale?.language == "hi" }

        val mapped = VoiceCatalog.baseProfiles.mapIndexed { idx, profile ->
            val langPool = if (profile.languageCode == "hi") hiVoices else enVoices
            val langSupported = if (profile.languageCode == "hi") hiAvailable else enAvailable

            val matchedVoice = if (langPool.isNotEmpty()) {
                val genderHint = if (profile.gender == VoiceGender.FEMININE) {
                    listOf("female", "sfg", "iob", "hie", "tpf", "wavenet-a", "wavenet-c", "wavenet-e")
                } else {
                    listOf("male", "iom", "hid", "tpd", "wavenet-b", "wavenet-d")
                }
                langPool.firstOrNull { v ->
                    genderHint.any { hint -> v.name.lowercase().contains(hint) }
                } ?: langPool[idx % langPool.size]
            } else {
                null
            }

            if (langSupported) {
                profile.copy(
                    engineVoiceName = matchedVoice?.name,
                    availableOnDevice = true,
                    availabilityNote = if (matchedVoice != null) {
                        "Device engine ready (${matchedVoice.locale.displayName})"
                    } else {
                        "Device locale ready (${profile.localeTag}) with pitch/rate profile"
                    }
                )
            } else {
                profile.copy(
                    engineVoiceName = null,
                    availableOnDevice = false,
                    availabilityNote = "Install ${if (profile.languageCode == "hi") "Hindi (hi-IN)" else "English"} speech data in Android TTS Settings"
                )
            }
        }

        _voiceProfiles.value = mapped
        val readyCount = mapped.count { it.availableOnDevice }
        _ttsStatusMessage.value = "Speech Engine Active • $readyCount/${mapped.size} profiles supported on this device"
    }

    fun setThinking(thinking: Boolean) {
        if (thinking) {
            _coreState.value = CoreVoiceState.THINKING
        } else if (_coreState.value == CoreVoiceState.THINKING) {
            _coreState.value = CoreVoiceState.IDLE
        }
    }

    fun speak(
        text: String,
        profile: VoiceProfile?,
        speechRateOverride: Float? = null,
        pitchOverride: Float? = null
    ): Boolean {
        val engine = tts ?: return false
        if (!ttsReady || text.isBlank()) return false
        if (profile != null && !profile.availableOnDevice) return false

        val cleanText = text
            .replace(Regex("[*#`_]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(3500)

        val locale = VoiceCatalog.localeFor(profile?.localeTag ?: "en-US")
        engine.language = locale
        if (profile?.engineVoiceName != null) {
            deviceVoices.firstOrNull { it.name == profile.engineVoiceName }?.let {
                engine.voice = it
            }
        }
        val rate = (speechRateOverride ?: profile?.defaultRate ?: 1.0f).coerceIn(0.5f, 2.0f)
        val pitch = (pitchOverride ?: profile?.defaultPitch ?: 1.0f).coerceIn(0.5f, 2.0f)
        engine.setSpeechRate(rate)
        engine.setPitch(pitch)

        val utteranceId = UUID.randomUUID().toString()
        val result = engine.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        return result == TextToSpeech.SUCCESS
    }

    fun stopSpeaking() {
        tts?.stop()
        if (_coreState.value == CoreVoiceState.SPEAKING) {
            _coreState.value = CoreVoiceState.IDLE
        }
        _voiceAmplitude.value = 0f
    }

    fun startListening(
        languageCode: String,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            onError("On-device Speech Recognition service is not available on this emulator/device.")
            return
        }
        stopSpeaking()
        stopListening()

        val recognizer = SpeechRecognizer.createSpeechRecognizer(appContext)
        speechRecognizer = recognizer
        _partialTranscript.value = ""
        _coreState.value = CoreVoiceState.LISTENING

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _coreState.value = CoreVoiceState.LISTENING
            }

            override fun onBeginningOfSpeech() {
                _coreState.value = CoreVoiceState.LISTENING
            }

            override fun onRmsChanged(rmsdB: Float) {
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.08f, 1.0f)
                _voiceAmplitude.value = normalized
            }

            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() {
                _voiceAmplitude.value = 0.15f
            }

            override fun onError(error: Int) {
                _coreState.value = CoreVoiceState.IDLE
                _voiceAmplitude.value = 0f
                val message = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                        "Network unavailable for speech recognition."
                    SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that. Try speaking again."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected."
                    else -> "Voice input stopped (code $error)."
                }
                onError(message)
            }

            override fun onResults(results: Bundle?) {
                _voiceAmplitude.value = 0f
                _coreState.value = CoreVoiceState.IDLE
                val spoken = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                if (!spoken.isNullOrEmpty()) {
                    _partialTranscript.value = spoken
                    onResult(spoken)
                } else {
                    onError("No voice command recognized.")
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                if (!partial.isNullOrBlank()) {
                    _partialTranscript.value = partial
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (languageCode == "hi") "hi-IN" else "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        try {
            recognizer.startListening(intent)
        } catch (e: Exception) {
            _coreState.value = CoreVoiceState.IDLE
            onError(e.localizedMessage ?: "Unable to start microphone listening.")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) {
        } finally {
            speechRecognizer = null
            if (_coreState.value == CoreVoiceState.LISTENING) {
                _coreState.value = CoreVoiceState.IDLE
            }
            _voiceAmplitude.value = 0f
        }
    }

    fun release() {
        bgScope.cancel()
        stopListening()
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
