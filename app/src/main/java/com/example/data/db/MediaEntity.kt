package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.formatBytes

@Entity(tableName = "media_downloads")
data class MediaItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val originalUrl: String,
    val downloadUrl: String,
    val localFilePath: String? = null,
    val contentUri: String? = null,
    val thumbnailUrl: String? = null,
    val mediaType: String = "video", // "video", "audio", "whatsapp_status", "image"
    val qualityLabel: String = "720p",
    val fileSizeBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val progress: Float = 0f, // 0.0 to 1.0
    val status: String = "QUEUED", // "QUEUED", "DOWNLOADING", "PAUSED", "COMPLETED", "FAILED"
    val speedText: String = "",
    val errorMessage: String? = null,
    val platform: String = "Direct", // "YouTube", "TikTok", "Facebook", "Instagram", "WhatsApp", "Direct", "Web"
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
) {
    val isCompleted: Boolean get() = status == "COMPLETED"
    val isDownloading: Boolean get() = status == "DOWNLOADING"
    val isPaused: Boolean get() = status == "PAUSED"
    val isFailed: Boolean get() = status == "FAILED"

    val displaySize: String
        get() = formatBytes(if (fileSizeBytes > 0) fileSizeBytes else downloadedBytes)

    val progressPercent: Int
        get() = (progress * 100).coerceIn(0f, 100f).toInt()
}

@Entity(tableName = "saved_statuses")
data class SavedStatusEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val originalUri: String,
    val savedFilePath: String,
    val mimeType: String,
    val isVideo: Boolean = true,
    val fileSizeBytes: Long = 0L,
    val savedAt: Long = System.currentTimeMillis()
)
