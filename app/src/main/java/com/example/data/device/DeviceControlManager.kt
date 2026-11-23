package com.example.data.device

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Settings
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.roundToInt

data class DocumentReadResult(
    val fileName: String,
    val mimeType: String,
    val pageCount: Int,
    val previewBitmap: Bitmap?,
    val extractedText: String
)

class DeviceControlManager(context: Context) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val cameraManager = appContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    @Volatile
    private var torchCameraId: String? = null
    @Volatile
    private var torchChecked: Boolean = false

    private fun findFlashCameraId(): String? {
        return try {
            cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (_: Exception) {
            null
        }
    }

    fun isTorchSupported(): Boolean {
        if (!torchChecked) {
            torchCameraId = findFlashCameraId()
            torchChecked = true
        }
        return torchCameraId != null
    }

    fun setTorchEnabled(enabled: Boolean): Result<Boolean> {
        val camId = torchCameraId ?: findFlashCameraId().also { torchCameraId = it }
            ?: return Result.failure(IllegalStateException("No camera flash hardware detected on this device."))
        return try {
            cameraManager.setTorchMode(camId, enabled)
            Result.success(enabled)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getMediaVolumeFraction(): Float {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val cur = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        return cur.toFloat() / max.toFloat()
    }

    fun setMediaVolumeFraction(fraction: Float): Float {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val target = (fraction.coerceIn(0f, 1f) * max).roundToInt()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        return target.toFloat() / max.toFloat()
    }

    fun adjustVolumeStep(up: Boolean): Float {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            if (up) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI
        )
        return getMediaVolumeFraction()
    }

    fun setWindowBrightness(activity: Activity?, fraction: Float): Float {
        val clamped = fraction.coerceIn(0.08f, 1f)
        activity?.window?.let { win ->
            val attrs = win.attributes
            attrs.screenBrightness = clamped
            win.attributes = attrs
        }
        return clamped
    }

    fun openAndroidSetting(action: String): Result<Unit> {
        return try {
            val intent = Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun openAppDetailsSettings(): Result<Unit> {
        return try {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", appContext.packageName, null)
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun launchCategoryApp(category: String): Result<Unit> {
        return try {
            val intent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, category)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(IllegalStateException("No installed app handles this shortcut on this device."))
        }
    }

    fun launchDialer(): Result<Unit> {
        return try {
            val intent = Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun launchPackageOrWeb(packageName: String, fallbackUrl: String): Result<String> {
        return try {
            val launchIntent = appContext.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                appContext.startActivity(launchIntent)
                Result.success("Opened app")
            } else {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                appContext.startActivity(webIntent)
                Result.success("Opened in browser")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun openWebUrl(url: String): Result<Unit> {
        return try {
            val parsed = Uri.parse(url)
            require(parsed.scheme == "https" || parsed.scheme == "http") {
                "Only HTTP/HTTPS URLs are permitted."
            }
            val intent = Intent(Intent.ACTION_VIEW, parsed).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun createAlarmIntent(hour: Int, minute: Int, message: String): Result<Unit> {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour.coerceIn(0, 23))
                putExtra(AlarmClock.EXTRA_MINUTES, minute.coerceIn(0, 59))
                putExtra(AlarmClock.EXTRA_MESSAGE, message.ifBlank { "JARVIS Alarm" })
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Clock/Alarm app unavailable on this device."))
        }
    }

    fun createCalendarEventIntent(title: String, description: String): Result<Unit> {
        return try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, title.ifBlank { "JARVIS Scheduled Event" })
                putExtra(CalendarContract.Events.DESCRIPTION, description)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Calendar app unavailable on this device."))
        }
    }

    fun loadBitmapFromUri(uri: Uri, maxDimension: Int = 1024): Bitmap? {
        return try {
            appContext.contentResolver.openInputStream(uri)?.use { stream ->
                val raw = BitmapFactory.decodeStream(stream) ?: return null
                val scale = minOf(
                    1f,
                    maxDimension.toFloat() / maxOf(raw.width, raw.height).coerceAtLeast(1)
                )
                if (scale < 1f) {
                    Bitmap.createScaledBitmap(
                        raw,
                        (raw.width * scale).roundToInt().coerceAtLeast(1),
                        (raw.height * scale).roundToInt().coerceAtLeast(1),
                        true
                    )
                } else {
                    raw
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    fun saveBitmapToGallery(bitmap: Bitmap, displayNamePrefix: String = "JARVIS_AI"): Result<String> {
        return try {
            val filename = "${displayNamePrefix}_${System.currentTimeMillis()}.png"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Jarvis")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val resolver = appContext.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return Result.failure(IllegalStateException("Unable to create MediaStore entry."))

            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            Result.success(filename)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun readDocumentFromUri(uri: Uri): Result<DocumentReadResult> {
        return try {
            val resolver = appContext.contentResolver
            val mimeType = resolver.getType(uri) ?: "application/octet-stream"
            var fileName = "document"
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) fileName = cursor.getString(idx) ?: fileName
                }
            }

            if (mimeType == "application/pdf" || fileName.endsWith(".pdf", ignoreCase = true)) {
                val pfd = resolver.openFileDescriptor(uri, "r")
                    ?: return Result.failure(IllegalStateException("Unable to open PDF descriptor."))
                pfd.use { descriptor ->
                    PdfRenderer(descriptor).use { renderer ->
                        val count = renderer.pageCount
                        val preview = if (count > 0) {
                            renderer.openPage(0).use { page ->
                                val width = page.width.coerceIn(400, 1080)
                                val height = ((page.height.toFloat() / page.width.coerceAtLeast(1)) * width)
                                    .roundToInt()
                                    .coerceIn(400, 1500)
                                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                bmp.eraseColor(android.graphics.Color.WHITE)
                                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                bmp
                            }
                        } else {
                            null
                        }
                        Result.success(
                            DocumentReadResult(
                                fileName = fileName,
                                mimeType = mimeType,
                                pageCount = count,
                                previewBitmap = preview,
                                extractedText = "PDF loaded ($count page${if (count == 1) "" else "s"}). Page 1 rendered for multimodal AI reading and summarization."
                            )
                        )
                    }
                }
            } else {
                val textBuilder = StringBuilder()
                resolver.openInputStream(uri)?.use { input ->
                    BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { reader ->
                        val buffer = CharArray(4096)
                        var total = 0
                        while (total < 24_000) {
                            val read = reader.read(buffer)
                            if (read <= 0) break
                            textBuilder.append(buffer, 0, read)
                            total += read
                        }
                    }
                }
                Result.success(
                    DocumentReadResult(
                        fileName = fileName,
                        mimeType = mimeType,
                        pageCount = 1,
                        previewBitmap = null,
                        extractedText = textBuilder.toString().ifBlank { "File opened, but no plain text characters were detected." }
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
