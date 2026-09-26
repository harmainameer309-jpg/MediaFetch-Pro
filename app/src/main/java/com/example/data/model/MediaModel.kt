package com.example.data.model

data class VideoFormat(
    val id: String,
    val label: String,          // e.g. "1080p (Best)", "720p (HD)", "480p (SD)", "Audio (MP3)"
    val resolution: String,     // "1080p", "720p", "480p", "Audio"
    val ext: String,            // "mp4", "mp3", "webm"
    val isAudioOnly: Boolean = false,
    val estimatedSizeBytes: Long = 0L,
    val downloadUrl: String
) {
    val formattedSize: String
        get() = formatBytes(estimatedSizeBytes)
}

data class MediaInfo(
    val title: String,
    val thumbnailUrl: String?,
    val durationText: String?,
    val author: String?,
    val platform: String,
    val originalUrl: String,
    val availableFormats: List<VideoFormat>
)

data class JobStatusResponse(
    val id: String,
    val status: String, // "pending", "downloading", "processing", "completed", "error"
    val progress: Int = 0,
    val downloadUrl: String? = null,
    val fileName: String? = null,
    val error: String? = null
)

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "Unknown size"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format("%.2f GB", gb)
        mb >= 1.0 -> String.format("%.1f MB", mb)
        kb >= 1.0 -> String.format("%.0f KB", kb)
        else -> "$bytes B"
    }
}

fun formatSpeed(bytesPerSec: Long): String {
    if (bytesPerSec <= 0) return "0 KB/s"
    val kb = bytesPerSec / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
        String.format("%.1f MB/s", mb)
    } else {
        String.format("%.0f KB/s", kb)
    }
}
