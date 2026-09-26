package com.example.service

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.example.data.db.MediaRepository
import com.example.data.db.SavedStatusEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class StatusSaverManager(
    private val context: Context,
    private val repository: MediaRepository
) {

    /**
     * Import a video or image URI (from WhatsApp Share intent or SAF file picker)
     * and save it directly into Movies/MediaFetch directory and MediaStore.
     */
    suspend fun importMedia(uri: Uri, customNamePrefix: String = "WhatsApp_Status"): Result<SavedStatusEntity> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: "video/mp4"
            val isVideo = mimeType.startsWith("video")

            // Query file display name
            var originalName: String? = null
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    originalName = cursor.getString(nameIndex)
                }
            }

            val ext = when {
                mimeType.contains("video/mp4") -> "mp4"
                mimeType.contains("video/3gpp") -> "3gp"
                mimeType.contains("image/jpeg") -> "jpg"
                mimeType.contains("image/png") -> "png"
                isVideo -> "mp4"
                else -> "jpg"
            }

            val timestamp = System.currentTimeMillis()
            val safeName = originalName ?: "${customNamePrefix}_$timestamp.$ext"

            // Target folder: Movies/MediaFetch
            val targetDir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_MOVIES),
                "MediaFetch"
            ).apply { mkdirs() }

            val targetFile = File(targetDir, "${timestamp}_$safeName")

            // Copy stream
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception("Cannot open input stream for media"))

            // Register to MediaStore
            registerStatusToMediaStore(targetFile, safeName, mimeType, isVideo)

            val statusEntity = SavedStatusEntity(
                title = originalName ?: "Status $timestamp",
                originalUri = uri.toString(),
                savedFilePath = targetFile.absolutePath,
                mimeType = mimeType,
                isVideo = isVideo,
                fileSizeBytes = targetFile.length(),
                savedAt = timestamp
            )

            val newId = repository.insertSavedStatus(statusEntity)
            Result.success(statusEntity.copy(id = newId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun registerStatusToMediaStore(file: File, displayName: String, mimeType: String, isVideo: Boolean) {
        try {
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (isVideo) MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                else MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        if (isVideo) Environment.DIRECTORY_MOVIES + "/MediaFetch"
                        else Environment.DIRECTORY_PICTURES + "/MediaFetch"
                    )
                }
            }

            context.contentResolver.insert(collection, values)
        } catch (_: Exception) {
        }
    }

    suspend fun deleteStatus(status: SavedStatusEntity) = withContext(Dispatchers.IO) {
        try {
            val file = File(status.savedFilePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {}
        repository.deleteSavedStatus(status.id)
    }
}
