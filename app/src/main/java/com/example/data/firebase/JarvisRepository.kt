package com.example.data.firebase

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class JarvisRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    // --- Profile Settings ---

    fun observeProfileSettings(userId: String): Flow<JarvisProfileSettings> = flow {
        val path = "users/$userId/profile/settings"
        emitAll(
            userDoc(userId).collection("profile").document("settings")
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            JarvisProfileSettings::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ) ?: JarvisProfileSettings(userId = userId)
                    } else {
                        JarvisProfileSettings(userId = userId)
                    }
                }
                .catch { error -> logFlowError(error, userId, OperationType.GET, path); throw error }
        )
    }

    suspend fun ensureProfileInitialized(userId: String): Result<Unit> {
        val path = "users/$userId/profile/settings"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val ref = userDoc(uid).collection("profile").document("settings")
            val existing = ref.get().await()
            if (!existing.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "ownerDisplayName" to "aarif khann",
                    "primaryLanguage" to "en",
                    "memoryEnabled" to true,
                    "selectedVoiceId" to "m_cinematic_en",
                    "voiceSpeechRate" to 1.0,
                    "voicePitch" to 1.0,
                    "favoriteVoiceIds" to listOf("m_cinematic_en", "f_calm_hi"),
                    "securityPinConfigured" to false,
                    "biometricEnabled" to false,
                    "secureChatEnabled" to true,
                    "voicePassphraseEnabled" to false,
                    "emergencyLocked" to false,
                    "installedVersion" to "1.0.0",
                    "lastUpdateReviewIso" to "2026-11-23",
                    "nextUpdateReviewIso" to "2027-03-23",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                ref.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProfileFields(userId: String, updates: Map<String, Any>): Result<Unit> {
        val path = "users/$userId/profile/settings"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            ensureProfileInitialized(uid)
            val ref = userDoc(uid).collection("profile").document("settings")
            val mutablePayload = updates.toMutableMap()
            mutablePayload["updatedAt"] = FieldValue.serverTimestamp()
            ref.update(mutablePayload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- Chats ---

    fun observeChats(userId: String): Flow<List<ChatMessage>> = flow {
        val path = "users/$userId/chats"
        emitAll(
            userDoc(userId).collection("chats")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        ChatMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error -> logFlowError(error, userId, OperationType.LIST, path); throw error }
        )
    }

    suspend fun addChatMessage(
        userId: String,
        role: String,
        content: String,
        language: String,
        center: String,
        encrypted: Boolean
    ): Result<String> {
        val path = "users/$userId/chats"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val safeContent = content.trim().take(19500)
            require(safeContent.isNotEmpty()) { "Message content cannot be blank." }
            val ref = userDoc(uid).collection("chats").document()
            val payload = mapOf(
                "userId" to uid,
                "role" to if (role == "model") "model" else "user",
                "content" to safeContent,
                "language" to if (language == "hi") "hi" else "en",
                "center" to center,
                "encrypted" to encrypted,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ref.set(payload).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteChatMessage(userId: String, messageId: String): Result<Unit> =
        deleteDocument(userId, "chats", messageId)

    // --- Personal Memories ---

    fun observeMemories(userId: String): Flow<List<PersonalMemory>> = flow {
        val path = "users/$userId/memories"
        emitAll(
            userDoc(userId).collection("memories")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        PersonalMemory::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error -> logFlowError(error, userId, OperationType.LIST, path); throw error }
        )
    }

    suspend fun addMemory(
        userId: String,
        category: String,
        content: String,
        encrypted: Boolean
    ): Result<String> {
        val path = "users/$userId/memories"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val safeCategory = category.trim().ifEmpty { "General" }.take(40)
            val safeContent = content.trim().take(1950)
            require(safeContent.isNotEmpty()) { "Memory content cannot be blank." }
            val ref = userDoc(uid).collection("memories").document()
            val payload = mapOf(
                "userId" to uid,
                "category" to safeCategory,
                "content" to safeContent,
                "encrypted" to encrypted,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ref.set(payload).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteMemory(userId: String, memoryId: String): Result<Unit> =
        deleteDocument(userId, "memories", memoryId)

    // --- Productivity Tasks & Reminders ---

    fun observeTasks(userId: String): Flow<List<ProductivityTask>> = flow {
        val path = "users/$userId/tasks"
        emitAll(
            userDoc(userId).collection("tasks")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        ProductivityTask::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error -> logFlowError(error, userId, OperationType.LIST, path); throw error }
        )
    }

    suspend fun addTask(
        userId: String,
        title: String,
        notes: String,
        category: String,
        dueLabel: String
    ): Result<String> {
        val path = "users/$userId/tasks"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val safeTitle = title.trim().take(200)
            require(safeTitle.isNotEmpty()) { "Title cannot be blank." }
            val ref = userDoc(uid).collection("tasks").document()
            val payload = mapOf(
                "userId" to uid,
                "title" to safeTitle,
                "notes" to notes.trim().take(2000),
                "category" to if (category == "reminder") "reminder" else "task",
                "dueLabel" to dueLabel.trim().take(80),
                "completed" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ref.set(payload).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleTaskCompleted(userId: String, taskId: String, completed: Boolean): Result<Unit> {
        val path = "users/$userId/tasks/$taskId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            require(taskId.isNotBlank())
            userDoc(uid).collection("tasks").document(taskId)
                .update(
                    mapOf(
                        "completed" to completed,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteTask(userId: String, taskId: String): Result<Unit> =
        deleteDocument(userId, "tasks", taskId)

    // --- Productivity Notes ---

    fun observeNotes(userId: String): Flow<List<ProductivityNote>> = flow {
        val path = "users/$userId/notes"
        emitAll(
            userDoc(userId).collection("notes")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        ProductivityNote::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error -> logFlowError(error, userId, OperationType.LIST, path); throw error }
        )
    }

    suspend fun addNote(
        userId: String,
        title: String,
        content: String,
        tag: String
    ): Result<String> {
        val path = "users/$userId/notes"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val safeTitle = title.trim().take(150)
            val safeContent = content.trim().take(9800)
            require(safeTitle.isNotEmpty() && safeContent.isNotEmpty()) { "Note title and content required." }
            val ref = userDoc(uid).collection("notes").document()
            val payload = mapOf(
                "userId" to uid,
                "title" to safeTitle,
                "content" to safeContent,
                "tag" to tag.trim().take(40),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ref.set(payload).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteNote(userId: String, noteId: String): Result<Unit> =
        deleteDocument(userId, "notes", noteId)

    // --- Vision History ---

    fun observeVisionHistory(userId: String): Flow<List<VisionHistoryEntry>> = flow {
        val path = "users/$userId/visionHistory"
        emitAll(
            userDoc(userId).collection("visionHistory")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        VisionHistoryEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error -> logFlowError(error, userId, OperationType.LIST, path); throw error }
        )
    }

    suspend fun addVisionHistory(
        userId: String,
        mode: String,
        prompt: String,
        resultSummary: String,
        savedLocally: Boolean
    ): Result<String> {
        val path = "users/$userId/visionHistory"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val ref = userDoc(uid).collection("visionHistory").document()
            val payload = mapOf(
                "userId" to uid,
                "mode" to if (mode == "generation") "generation" else "analysis",
                "prompt" to prompt.trim().ifEmpty { "Image Query" }.take(2000),
                "resultSummary" to resultSummary.trim().ifEmpty { "Completed" }.take(9800),
                "savedLocally" to savedLocally,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ref.set(payload).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // --- Coding Projects ---

    fun observeCodingProjects(userId: String): Flow<List<CodingProjectRecord>> = flow {
        val path = "users/$userId/codingProjects"
        emitAll(
            userDoc(userId).collection("codingProjects")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        CodingProjectRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error -> logFlowError(error, userId, OperationType.LIST, path); throw error }
        )
    }

    suspend fun addCodingProject(
        userId: String,
        title: String,
        language: String,
        category: String,
        prompt: String,
        codeOutput: String
    ): Result<String> {
        val path = "users/$userId/codingProjects"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val ref = userDoc(uid).collection("codingProjects").document()
            val payload = mapOf(
                "userId" to uid,
                "title" to title.trim().ifEmpty { "Untitled Snippet" }.take(150),
                "language" to language.trim().ifEmpty { "Kotlin" }.take(40),
                "category" to category.trim().ifEmpty { "Code Generation" }.take(60),
                "prompt" to prompt.trim().ifEmpty { "Code assistance" }.take(4000),
                "codeOutput" to codeOutput.trim().ifEmpty { "// Empty output" }.take(29000),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ref.set(payload).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCodingProject(userId: String, projectId: String): Result<Unit> =
        deleteDocument(userId, "codingProjects", projectId)

    // --- Update Center Audit History ---

    fun observeUpdateHistory(userId: String): Flow<List<UpdateAuditRecord>> = flow {
        val path = "users/$userId/updateHistory"
        emitAll(
            userDoc(userId).collection("updateHistory")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        UpdateAuditRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error -> logFlowError(error, userId, OperationType.LIST, path); throw error }
        )
    }

    suspend fun logUpdateReview(
        userId: String,
        version: String,
        summary: String,
        status: String
    ): Result<String> {
        val path = "users/$userId/updateHistory"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val ref = userDoc(uid).collection("updateHistory").document()
            val payload = mapOf(
                "userId" to uid,
                "version" to version.trim().ifEmpty { "1.0.0" }.take(30),
                "summary" to summary.trim().ifEmpty { "System check completed" }.take(2000),
                "status" to if (status == "approved") "approved" else "reviewed",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ref.set(payload).await()
            Result.success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    private suspend fun deleteDocument(
        userId: String,
        collectionName: String,
        docId: String
    ): Result<Unit> {
        val path = "users/$userId/$collectionName/$docId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            require(docId.isNotBlank())
            userDoc(uid).collection(collectionName).document(docId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    private fun logFlowError(
        error: Throwable,
        userId: String,
        operationType: OperationType,
        path: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }
}
