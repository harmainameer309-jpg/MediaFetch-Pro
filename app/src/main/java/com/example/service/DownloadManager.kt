package com.example.service

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.data.db.MediaItemEntity
import com.example.data.db.MediaRepository
import com.example.data.model.formatSpeed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class MediaDownloadManager(
    private val context: Context,
    private val repository: MediaRepository,
    private val scope: CoroutineScope
) {
    private val activeJobs = ConcurrentHashMap<Long, Job>()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun startDownload(item: MediaItemEntity, backendUrl: String? = null) {
        if (activeJobs.containsKey(item.id)) return

        val job = scope.launch(Dispatchers.IO) {
            try {
                repository.updateProgress(
                    id = item.id,
                    downloadedBytes = item.downloadedBytes,
                    fileSizeBytes = item.fileSizeBytes,
                    progress = item.progress,
                    speedText = "Connecting...",
                    status = "DOWNLOADING"
                )

                // If backend URL is provided and not a direct file, try Render backend polling job
                val useBackend = !backendUrl.isNullOrBlank() && 
                        backendUrl.startsWith("http") && 
                        !item.downloadUrl.endsWith(".mp4", true) && 
                        !item.downloadUrl.endsWith(".mp3", true)

                if (useBackend) {
                    executeBackendJob(item, backendUrl.trimEnd('/'))
                } else {
                    executeDirectDownload(item)
                }
            } catch (e: Exception) {
                if (isActive) {
                    repository.updateProgress(
                        id = item.id,
                        downloadedBytes = item.downloadedBytes,
                        fileSizeBytes = item.fileSizeBytes,
                        progress = item.progress,
                        speedText = "",
                        status = "FAILED",
                        errorMessage = e.localizedMessage ?: "Download failed"
                    )
                }
            } finally {
                activeJobs.remove(item.id)
            }
        }

        activeJobs[item.id] = job
    }

    private suspend fun executeBackendJob(item: MediaItemEntity, backendBase: String) = kotlinx.coroutines.withContext(Dispatchers.IO) {
        // 1. POST /api/download
        val postJson = JSONObject().apply {
            put("url", item.originalUrl)
            put("quality", item.qualityLabel)
        }

        val postRequest = Request.Builder()
            .url("$backendBase/api/download")
            .post(postJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val postResp = httpClient.newCall(postRequest).execute()
        if (!postResp.isSuccessful) {
            // Fallback to direct download
            executeDirectDownload(item)
            return@withContext
        }

        val bodyStr = postResp.body?.string() ?: ""
        val json = JSONObject(bodyStr)
        val jobId = json.optString("jobId", json.optString("id", ""))

        if (jobId.isEmpty()) {
            executeDirectDownload(item)
            return@withContext
        }

        // 2. Poll /api/job/:id
        var isDone = false
        var directDownloadUrl: String? = null

        while (!isDone && isActive) {
            delay(1000)
            val pollRequest = Request.Builder()
                .url("$backendBase/api/job/$jobId")
                .get()
                .build()

            val pollResp = httpClient.newCall(pollRequest).execute()
            if (pollResp.isSuccessful) {
                val pollBody = pollResp.body?.string() ?: ""
                val pollJson = JSONObject(pollBody)
                val status = pollJson.optString("status", "processing")
                val progress = pollJson.optInt("progress", 0)

                repository.updateProgress(
                    id = item.id,
                    downloadedBytes = (item.fileSizeBytes * (progress / 100f)).toLong(),
                    fileSizeBytes = item.fileSizeBytes,
                    progress = (progress / 100f).coerceIn(0f, 0.95f),
                    speedText = "Processing: $progress%",
                    status = "DOWNLOADING"
                )

                if (status.equals("completed", true) || pollJson.has("downloadUrl")) {
                    directDownloadUrl = pollJson.optString("downloadUrl", "$backendBase/api/file/$jobId")
                    isDone = true
                } else if (status.equals("error", true)) {
                    throw Exception(pollJson.optString("error", "Backend extraction error"))
                }
            }
        }

        if (directDownloadUrl != null) {
            val updatedItem = item.copy(downloadUrl = directDownloadUrl)
            executeDirectDownload(updatedItem)
        }
    }

    private suspend fun executeDirectDownload(item: MediaItemEntity) = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val targetDir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_MOVIES),
            "MediaFetch"
        ).apply { mkdirs() }

        val safeTitle = sanitizeFilename(item.title)
        val ext = if (item.mediaType == "audio" || item.qualityLabel.contains("MP3", true)) "mp3" else "mp4"
        val fileName = "${safeTitle}_${item.id}.$ext"
        val outputFile = File(targetDir, fileName)

        val request = Request.Builder()
            .url(item.downloadUrl)
            .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:109.0) Gecko/111.0 Firefox/111.0")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("HTTP Error: ${response.code}")
        }

        val body = response.body ?: throw Exception("Empty response body")
        val totalLength = if (body.contentLength() > 0) body.contentLength() else item.fileSizeBytes

        val inputStream: InputStream = body.byteStream()
        val outputStream = FileOutputStream(outputFile)

        val buffer = ByteArray(16 * 1024)
        var bytesRead: Int
        var totalBytesRead = 0L
        var lastUpdateTime = System.currentTimeMillis()
        var lastBytesCount = 0L

        try {
            while (inputStream.read(buffer).also { bytesRead = it } != -1 && isActive) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead

                val now = System.currentTimeMillis()
                if (now - lastUpdateTime >= 350) {
                    val timeDeltaSec = (now - lastUpdateTime) / 1000.0
                    val bytesDelta = totalBytesRead - lastBytesCount
                    val speedBytesSec = if (timeDeltaSec > 0) (bytesDelta / timeDeltaSec).toLong() else 0L

                    val progressFloat = if (totalLength > 0) {
                        (totalBytesRead.toFloat() / totalLength).coerceIn(0f, 1f)
                    } else 0.5f

                    repository.updateProgress(
                        id = item.id,
                        downloadedBytes = totalBytesRead,
                        fileSizeBytes = if (totalLength > 0) totalLength else totalBytesRead,
                        progress = progressFloat,
                        speedText = formatSpeed(speedBytesSec),
                        status = "DOWNLOADING"
                    )

                    lastUpdateTime = now
                    lastBytesCount = totalBytesRead
                }
            }
        } finally {
            outputStream.flush()
            outputStream.close()
            inputStream.close()
        }

        if (isActive) {
            // Register with MediaStore so it appears in standard Android Gallery/Players
            val contentUri = registerToMediaStore(outputFile, item.title, ext)

            repository.updateProgress(
                id = item.id,
                downloadedBytes = totalBytesRead,
                fileSizeBytes = totalBytesRead,
                progress = 1.0f,
                speedText = "Completed",
                status = "COMPLETED",
                completedAt = System.currentTimeMillis(),
                localPath = outputFile.absolutePath,
                contentUri = contentUri?.toString()
            )
        }
    }

    private fun registerToMediaStore(file: File, title: String, ext: String): Uri? {
        return try {
            val isAudio = ext.equals("mp3", true)
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (isAudio) MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                else MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                if (isAudio) MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.TITLE, title)
                put(MediaStore.MediaColumns.MIME_TYPE, if (isAudio) "audio/mpeg" else "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        if (isAudio) Environment.DIRECTORY_MUSIC + "/MediaFetch"
                        else Environment.DIRECTORY_MOVIES + "/MediaFetch"
                    )
                }
            }

            context.contentResolver.insert(collection, values)
        } catch (_: Exception) {
            null
        }
    }

    fun pauseDownload(id: Long) {
        val job = activeJobs.remove(id)
        job?.cancel()
        scope.launch(Dispatchers.IO) {
            val item = repository.getDownloadById(id)
            if (item != null) {
                repository.updateProgress(
                    id = id,
                    downloadedBytes = item.downloadedBytes,
                    fileSizeBytes = item.fileSizeBytes,
                    progress = item.progress,
                    speedText = "Paused",
                    status = "PAUSED"
                )
            }
        }
    }

    fun resumeDownload(item: MediaItemEntity, backendUrl: String? = null) {
        startDownload(item, backendUrl)
    }

    fun cancelDownload(id: Long) {
        val job = activeJobs.remove(id)
        job?.cancel()
        scope.launch(Dispatchers.IO) {
            repository.deleteDownload(id)
        }
    }

    private fun sanitizeFilename(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .take(50)
            .ifBlank { "MediaFetch_Video" }
    }
}
