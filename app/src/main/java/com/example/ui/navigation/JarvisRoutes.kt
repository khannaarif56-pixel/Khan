package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.ui.graphics.vector.ImageVector

enum class JarvisCenter(
    val route: String,
    val title: String,
    val shortLabel: String,
    val subtitle: String,
    val icon: ImageVector
) {
    HOME(
        route = "home",
        title = "J.A.R.V.I.S",
        shortLabel = "Core",
        subtitle = "GOLDEN HOLOGRAPHIC COMMAND HUB",
        icon = Icons.Filled.Dashboard
    ),
    AI_BRAIN(
        route = "ai_brain",
        title = "AI Brain Center",
        shortLabel = "Brain",
        subtitle = "Bilingual Conversation & Memory",
        icon = Icons.Filled.Psychology
    ),
    VOICE_ASSISTANT(
        route = "voice_assistant",
        title = "Voice Assistant",
        shortLabel = "Voice",
        subtitle = "Live Hindi & English Speech Engine",
        icon = Icons.Filled.Mic
    ),
    VOICE_SELECTION(
        route = "voice_selection",
        title = "Voice Selection",
        shortLabel = "Voices",
        subtitle = "20 Masculine & Feminine Styles",
        icon = Icons.Filled.RecordVoiceOver
    ),
    AI_VISION(
        route = "ai_vision",
        title = "AI Vision & Image",
        shortLabel = "Vision",
        subtitle = "Multimodal Analysis & Generation",
        icon = Icons.Filled.ImageSearch
    ),
    MOBILE_CONTROL(
        route = "mobile_control",
        title = "Mobile Control",
        shortLabel = "Controls",
        subtitle = "Torch, Volume, Settings & Launchers",
        icon = Icons.Filled.PhoneAndroid
    ),
    AI_TOOLS(
        route = "ai_tools",
        title = "AI Tools Center",
        shortLabel = "Tools",
        subtitle = "PDF, Docs, Math, Tutor & Translate",
        icon = Icons.Filled.Build
    ),
    PRODUCTIVITY(
        route = "productivity",
        title = "Productivity Center",
        shortLabel = "Tasks",
        subtitle = "Tasks, Notes, Alarms, Calc & News",
        icon = Icons.Filled.TaskAlt
    ),
    HIGH_SECURITY(
        route = "high_security",
        title = "High Security Center",
        shortLabel = "Security",
        subtitle = "10-Layer Auth, Vault & Anti-Tamper",
        icon = Icons.Filled.Security
    ),
    WORLDWIDE_SEARCH(
        route = "worldwide_search",
        title = "Worldwide Search",
        shortLabel = "Search",
        subtitle = "Global Research & Web Summaries",
        icon = Icons.Filled.Public
    ),
    GOOGLE_INTEGRATIONS(
        route = "google_integrations",
        title = "Google Integrations",
        shortLabel = "Google",
        subtitle = "Search, Gemini AI & Google Chat",
        icon = Icons.Filled.Hub
    ),
    AI_DEV(
        route = "ai_dev",
        title = "AI Development Center",
        shortLabel = "Coding",
        subtitle = "16-Tool Multi-Language Code Studio",
        icon = Icons.Filled.Code
    ),
    UPDATE_CENTER(
        route = "update_center",
        title = "Update Center",
        shortLabel = "Updates",
        subtitle = "4-Month Review & Security Audit",
        icon = Icons.Filled.SystemUpdate
    );

    companion object {
        val primaryTabs = listOf(HOME, AI_BRAIN, VOICE_ASSISTANT, AI_DEV, HIGH_SECURITY)
        val allTwelveCenters = entries.filter { it != HOME }
        fun fromRoute(route: String?): JarvisCenter =
            entries.firstOrNull { it.route == route } ?: HOME
    }
}
