package com.example.data.db

import kotlinx.coroutines.flow.Flow

class MediaRepository(private val mediaDao: MediaDao) {
    val allDownloads: Flow<List<MediaItemEntity>> = mediaDao.getAllDownloads()
    val activeDownloads: Flow<List<MediaItemEntity>> = mediaDao.getActiveDownloads()
    val completedDownloads: Flow<List<MediaItemEntity>> = mediaDao.getCompletedDownloads()
    val savedStatuses: Flow<List<SavedStatusEntity>> = mediaDao.getAllSavedStatuses()

    suspend fun insertDownload(item: MediaItemEntity): Long = mediaDao.insertDownload(item)

    suspend fun getDownloadById(id: Long): MediaItemEntity? = mediaDao.getDownloadById(id)

    suspend fun updateDownload(item: MediaItemEntity) = mediaDao.updateDownload(item)

    suspend fun updateProgress(
        id: Long,
        downloadedBytes: Long,
        fileSizeBytes: Long,
        progress: Float,
        speedText: String,
        status: String,
        completedAt: Long? = null,
        localPath: String? = null,
        contentUri: String? = null,
        errorMessage: String? = null
    ) {
        mediaDao.updateDownloadProgress(
            id = id,
            downloadedBytes = downloadedBytes,
            fileSizeBytes = fileSizeBytes,
            progress = progress,
            speedText = speedText,
            status = status,
            completedAt = completedAt,
            localPath = localPath,
            contentUri = contentUri,
            errorMessage = errorMessage
        )
    }

    suspend fun deleteDownload(id: Long) = mediaDao.deleteDownload(id)

    suspend fun clearCompleted() = mediaDao.clearCompletedDownloads()

    suspend fun insertSavedStatus(status: SavedStatusEntity): Long = mediaDao.insertSavedStatus(status)

    suspend fun deleteSavedStatus(id: Long) = mediaDao.deleteSavedStatus(id)
}
