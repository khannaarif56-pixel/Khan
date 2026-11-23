package com.example.data.ai

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

private const val TAG = "JarvisAiService"

class JarvisAiService {

    private val generalModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    """
                    You are JARVIS, an advanced, calm, highly intelligent futuristic personal AI assistant crafted for owner "aarif khann".
                    - Communicate naturally and clearly in English or Hindi (Devanagari or Hinglish as the user prefers), respecting the requested language parameter.
                    - Be concise, accurate, helpful, and structured when explaining complex topics, solving math step-by-step, tutoring students, summarizing documents, or writing clean production code.
                    - Never claim to perform an Android OS action you cannot execute directly; explain device permissions honestly.
                    """.trimIndent()
                )
            }
        )
    }

    private val fastModel by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.5-flash-lite")
    }

    private val imageGenModel by lazy {
        val config = generationConfig {
            responseModalities = listOf(ResponseModality.IMAGE)
        }
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = config
        )
    }

    fun streamConversation(
        prompt: String,
        languageCode: String,
        recentHistory: List<Pair<String, String>>,
        approvedMemories: List<String>
    ): Flow<String> = flow {
        val langInstruction = if (languageCode == "hi") {
            "Respond primarily in clear, natural Hindi (using Devanagari script, keeping technical terms clear)."
        } else {
            "Respond in crisp, natural English."
        }
        val memoryContext = if (approvedMemories.isNotEmpty()) {
            "\nApproved Owner Memory:\n" + approvedMemories.take(10).joinToString("\n") { "- $it" }
        } else {
            ""
        }
        val historyContext = if (recentHistory.isNotEmpty()) {
            "\nRecent Conversation Context:\n" + recentHistory.takeLast(8).joinToString("\n") { (role, text) ->
                "${if (role == "user") "User" else "JARVIS"}: ${text.take(600)}"
            }
        } else {
            ""
        }
        val fullPrompt = buildString {
            append(langInstruction)
            append(memoryContext)
            append(historyContext)
            append("\n\nUser Request:\n")
            append(prompt)
        }

        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            generalModel.generateContentStream(fullPrompt).collect { chunk ->
                val part = chunk.text.orEmpty()
                if (part.isNotEmpty()) {
                    accumulated.append(part)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("JARVIS returned an empty response.")
        }
    }

    suspend fun generateSingleResponse(
        prompt: String,
        languageCode: String,
        useLite: Boolean = false
    ): String {
        val langDirective = if (languageCode == "hi") {
            "Answer in clear Hindi (with technical keywords preserved where helpful).\n\n"
        } else {
            "Answer in clear, well-structured English.\n\n"
        }
        return try {
            withTimeout(60_000L) {
                val model = if (useLite) fastModel else generalModel
                val response = model.generateContent(langDirective + prompt)
                response.text?.takeIf { it.isNotBlank() }
                    ?: throw IllegalStateException("Model returned an empty response.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI request timed out", e)
            throw IllegalStateException("Request timed out after 60 seconds. Please retry.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI generation failed", e)
            throw e
        }
    }

    suspend fun analyzeImage(
        bitmap: Bitmap,
        question: String,
        languageCode: String
    ): String {
        val langDirective = if (languageCode == "hi") {
            "Explain your visual analysis clearly in Hindi."
        } else {
            "Explain your visual analysis clearly in English."
        }
        val effectiveQuestion = question.ifBlank {
            "Describe the objects, scene, text, and important visual details in this image."
        }
        return try {
            withTimeout(60_000L) {
                val response = generalModel.generateContent(
                    content {
                        image(bitmap)
                        text("$langDirective\nUser question: $effectiveQuestion")
                    }
                )
                response.text?.takeIf { it.isNotBlank() }
                    ?: throw IllegalStateException("No analysis text was returned for this image.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Image analysis timed out", e)
            throw IllegalStateException("Image analysis timed out after 60 seconds.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image analysis failed", e)
            throw e
        }
    }

    suspend fun generateImage(prompt: String): Bitmap {
        return try {
            withTimeout(60_000L) {
                val response = imageGenModel.generateContent(prompt)
                val imagePart = response.candidates.firstOrNull()
                    ?.content?.parts
                    ?.filterIsInstance<ImagePart>()
                    ?.firstOrNull()
                imagePart?.image
                    ?: throw IllegalStateException("No image was returned by the model.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Image generation timed out", e)
            throw IllegalStateException("Image generation timed out after 60 seconds.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image generation failed", e)
            throw e
        }
    }
}
