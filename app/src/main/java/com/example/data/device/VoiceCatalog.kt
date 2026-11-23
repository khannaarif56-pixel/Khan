package com.example.data.device

import java.util.Locale

enum class VoiceGender(val label: String) {
    MASCULINE("Masculine"),
    FEMININE("Feminine")
}

data class VoiceProfile(
    val id: String,
    val name: String,
    val gender: VoiceGender,
    val style: String,
    val languageCode: String,
    val localeTag: String,
    val defaultPitch: Float,
    val defaultRate: Float,
    val sampleEnglish: String,
    val sampleHindi: String,
    val engineVoiceName: String? = null,
    val availableOnDevice: Boolean = false,
    val availabilityNote: String = "Checking device speech engine..."
)

object VoiceCatalog {
    val baseProfiles: List<VoiceProfile> = listOf(
        // 10 Masculine-style voice profiles
        VoiceProfile(
            id = "m_cinematic_en",
            name = "Aurelius Core",
            gender = VoiceGender.MASCULINE,
            style = "Cinematic",
            languageCode = "en",
            localeTag = "en-US",
            defaultPitch = 0.86f,
            defaultRate = 0.96f,
            sampleEnglish = "Good evening, aarif khann. All JARVIS golden holographic telemetry systems are online.",
            sampleHindi = "नमस्ते आरिफ खान, जार्विस के सभी सिस्टम सक्रिय हैं।"
        ),
        VoiceProfile(
            id = "m_deep_en",
            name = "Titan Deep",
            gender = VoiceGender.MASCULINE,
            style = "Deep",
            languageCode = "en",
            localeTag = "en-GB",
            defaultPitch = 0.72f,
            defaultRate = 0.92f,
            sampleEnglish = "Deep vocal calibration engaged. Ready for tactical briefings and research.",
            sampleHindi = "डीप वॉयस मोड सक्रिय है। कृपया अपना आदेश दें।"
        ),
        VoiceProfile(
            id = "m_natural_en",
            name = "Orion Natural",
            gender = VoiceGender.MASCULINE,
            style = "Natural",
            languageCode = "en",
            localeTag = "en-US",
            defaultPitch = 0.98f,
            defaultRate = 1.02f,
            sampleEnglish = "Hey there, I'm ready to help with notes, reminders, and quick answers.",
            sampleHindi = "नमस्ते, मैं आपकी सहायता के लिए तैयार हूँ।"
        ),
        VoiceProfile(
            id = "m_robotic_en",
            name = "Sentinel Synth",
            gender = VoiceGender.MASCULINE,
            style = "Robotic",
            languageCode = "en",
            localeTag = "en-US",
            defaultPitch = 0.65f,
            defaultRate = 1.12f,
            sampleEnglish = "Synthesized acoustic matrix online. Diagnostics nominal.",
            sampleHindi = "सिस्टम डायग्नोस्टिक्स सामान्य हैं।"
        ),
        VoiceProfile(
            id = "m_professional_en",
            name = "Sterling Exec",
            gender = VoiceGender.MASCULINE,
            style = "Professional",
            languageCode = "en",
            localeTag = "en-GB",
            defaultPitch = 0.92f,
            defaultRate = 1.0f,
            sampleEnglish = "Executive assistant protocol ready. Let us review your agenda and priorities.",
            sampleHindi = "आपकी मीटिंग्स और कार्यों का विवरण तैयार है।"
        ),
        VoiceProfile(
            id = "m_calm_en",
            name = "Solace Calm",
            gender = VoiceGender.MASCULINE,
            style = "Calm",
            languageCode = "en",
            localeTag = "en-IN",
            defaultPitch = 0.90f,
            defaultRate = 0.88f,
            sampleEnglish = "Take a steady breath. Everything is organized and under control.",
            sampleHindi = "शांत रहें, आपके सभी कार्य व्यवस्थित हैं।"
        ),
        VoiceProfile(
            id = "m_friendly_en",
            name = "Rohan Spark",
            gender = VoiceGender.MASCULINE,
            style = "Friendly",
            languageCode = "en",
            localeTag = "en-IN",
            defaultPitch = 1.04f,
            defaultRate = 1.06f,
            sampleEnglish = "Great to see you! What awesome project are we building today?",
            sampleHindi = "आज हम क्या नया बनाने जा रहे हैं?"
        ),
        VoiceProfile(
            id = "m_cinematic_hi",
            name = "Vikram Neural",
            gender = VoiceGender.MASCULINE,
            style = "Cinematic",
            languageCode = "hi",
            localeTag = "hi-IN",
            defaultPitch = 0.84f,
            defaultRate = 0.95f,
            sampleEnglish = "Jarvis Hindi cinematic core initialized.",
            sampleHindi = "नमस्ते आरिफ खान। जार्विस हिंदी कमांड कोर पूरी तरह तैयार है।"
        ),
        VoiceProfile(
            id = "m_deep_hi",
            name = "Surya Gambhir",
            gender = VoiceGender.MASCULINE,
            style = "Deep",
            languageCode = "hi",
            localeTag = "hi-IN",
            defaultPitch = 0.74f,
            defaultRate = 0.90f,
            sampleEnglish = "Deep Hindi profile active.",
            sampleHindi = "गंभीर स्वर सक्रिय है। सुरक्षा और डेटा प्रणाली सुरक्षित है।"
        ),
        VoiceProfile(
            id = "m_friendly_hi",
            name = "Aarav Mitra",
            gender = VoiceGender.MASCULINE,
            style = "Friendly",
            languageCode = "hi",
            localeTag = "hi-IN",
            defaultPitch = 1.02f,
            defaultRate = 1.04f,
            sampleEnglish = "Friendly Hindi assistant active.",
            sampleHindi = "नमस्कार दोस्त! आज पढ़ाई, कोडिंग या किसी काम में कैसे मदद करूँ?"
        ),

        // 10 Feminine-style voice profiles
        VoiceProfile(
            id = "f_cinematic_en",
            name = "Athena Prime",
            gender = VoiceGender.FEMININE,
            style = "Cinematic",
            languageCode = "en",
            localeTag = "en-GB",
            defaultPitch = 1.12f,
            defaultRate = 0.96f,
            sampleEnglish = "Welcome back, aarif khann. Holographic intelligence matrix is synchronized.",
            sampleHindi = "वापसी पर स्वागत है। जार्विस मैट्रिक्स तैयार है।"
        ),
        VoiceProfile(
            id = "f_natural_en",
            name = "Nova Natural",
            gender = VoiceGender.FEMININE,
            style = "Natural",
            languageCode = "en",
            localeTag = "en-US",
            defaultPitch = 1.16f,
            defaultRate = 1.02f,
            sampleEnglish = "Hi! Ask me anything from coding architecture to daily planning.",
            sampleHindi = "नमस्ते, आप मुझसे कुछ भी पूछ सकते हैं।"
        ),
        VoiceProfile(
            id = "f_professional_en",
            name = "Clara Strategist",
            gender = VoiceGender.FEMININE,
            style = "Professional",
            languageCode = "en",
            localeTag = "en-US",
            defaultPitch = 1.08f,
            defaultRate = 1.0f,
            sampleEnglish = "Briefing compiled. Security protocols and cloud sync are verified.",
            sampleHindi = "आपकी रिपोर्ट और सुरक्षा जांच पूरी हो गई है।"
        ),
        VoiceProfile(
            id = "f_calm_en",
            name = "Lyra Serene",
            gender = VoiceGender.FEMININE,
            style = "Calm",
            languageCode = "en",
            localeTag = "en-IN",
            defaultPitch = 1.10f,
            defaultRate = 0.88f,
            sampleEnglish = "Calm mode enabled. Let us work through your goals peacefully.",
            sampleHindi = "शांत मोड चालू है। आइए आपके लक्ष्य पूरे करें।"
        ),
        VoiceProfile(
            id = "f_friendly_en",
            name = "Zara Bright",
            gender = VoiceGender.FEMININE,
            style = "Friendly",
            languageCode = "en",
            localeTag = "en-US",
            defaultPitch = 1.24f,
            defaultRate = 1.08f,
            sampleEnglish = "Hello! Ready to brainstorm, translate, or create something brilliant?",
            sampleHindi = "हेलो! आज क्या नया सीखें या बनाएं?"
        ),
        VoiceProfile(
            id = "f_deep_en",
            name = "Valkyrie Alto",
            gender = VoiceGender.FEMININE,
            style = "Deep",
            languageCode = "en",
            localeTag = "en-GB",
            defaultPitch = 0.96f,
            defaultRate = 0.94f,
            sampleEnglish = "Alto vocal resonance locked. Standing by for complex analysis.",
            sampleHindi = "विश्लेषण के लिए तैयार।"
        ),
        VoiceProfile(
            id = "f_robotic_en",
            name = "Helios Cyber",
            gender = VoiceGender.FEMININE,
            style = "Robotic",
            languageCode = "en",
            localeTag = "en-US",
            defaultPitch = 1.32f,
            defaultRate = 1.14f,
            sampleEnglish = "Telemetry online. Optical vision and neural processors nominal.",
            sampleHindi = "न्यूरल प्रोसेसर सक्रिय है।"
        ),
        VoiceProfile(
            id = "f_calm_hi",
            name = "Ananya Shanti",
            gender = VoiceGender.FEMININE,
            style = "Calm",
            languageCode = "hi",
            localeTag = "hi-IN",
            defaultPitch = 1.12f,
            defaultRate = 0.92f,
            sampleEnglish = "Calm Hindi voice ready.",
            sampleHindi = "नमस्ते आरिफ खान। मैं जार्विस हूँ, आपकी शांत हिंदी सहायक।"
        ),
        VoiceProfile(
            id = "f_professional_hi",
            name = "Meera Vidya",
            gender = VoiceGender.FEMININE,
            style = "Professional",
            languageCode = "hi",
            localeTag = "hi-IN",
            defaultPitch = 1.08f,
            defaultRate = 1.0f,
            sampleEnglish = "Professional Hindi voice ready.",
            sampleHindi = "सुप्रभात। आपके अध्ययन, अनुवाद और रिसर्च टूल तैयार हैं।"
        ),
        VoiceProfile(
            id = "f_friendly_hi",
            name = "Kavya Tarang",
            gender = VoiceGender.FEMININE,
            style = "Friendly",
            languageCode = "hi",
            localeTag = "hi-IN",
            defaultPitch = 1.22f,
            defaultRate = 1.06f,
            sampleEnglish = "Friendly Hindi voice ready.",
            sampleHindi = "नमस्ते! बताइए आज मैं आपके कौन से काम आसान करूँ?"
        )
    )

    fun localeFor(tag: String): Locale = Locale.forLanguageTag(tag)
}
