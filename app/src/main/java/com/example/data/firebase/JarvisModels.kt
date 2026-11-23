package com.example.data.firebase

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class JarvisProfileSettings(
    @DocumentId val id: String = "settings",
    val userId: String = "",
    val ownerDisplayName: String = "aarif khann",
    val primaryLanguage: String = "en",
    val memoryEnabled: Boolean = true,
    val selectedVoiceId: String = "m_cinematic_en",
    val voiceSpeechRate: Double = 1.0,
    val voicePitch: Double = 1.0,
    val favoriteVoiceIds: List<String> = listOf("m_cinematic_en", "f_calm_hi"),
    val securityPinConfigured: Boolean = false,
    val biometricEnabled: Boolean = false,
    val secureChatEnabled: Boolean = true,
    val voicePassphraseEnabled: Boolean = false,
    val emergencyLocked: Boolean = false,
    val installedVersion: String = "1.0.0",
    val lastUpdateReviewIso: String = "2026-11-23",
    val nextUpdateReviewIso: String = "2027-03-23",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val role: String = "user",
    val content: String = "",
    val language: String = "en",
    val center: String = "brain",
    val encrypted: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PersonalMemory(
    @DocumentId val id: String = "",
    val userId: String = "",
    val category: String = "General",
    val content: String = "",
    val encrypted: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ProductivityTask(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val notes: String = "",
    val category: String = "task",
    val dueLabel: String = "",
    val completed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ProductivityNote(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val tag: String = "General",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class VisionHistoryEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val mode: String = "analysis",
    val prompt: String = "",
    val resultSummary: String = "",
    val savedLocally: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CodingProjectRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val language: String = "Kotlin",
    val category: String = "Code Generation",
    val prompt: String = "",
    val codeOutput: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UpdateAuditRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val version: String = "1.0.0",
    val summary: String = "",
    val status: String = "reviewed",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
