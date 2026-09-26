package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_downloads WHERE status IN ('QUEUED', 'DOWNLOADING', 'PAUSED') ORDER BY createdAt DESC")
    fun getActiveDownloads(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_downloads WHERE status = 'COMPLETED' ORDER BY completedAt DESC, createdAt DESC")
    fun getCompletedDownloads(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_downloads WHERE id = :id LIMIT 1")
    suspend fun getDownloadById(id: Long): MediaItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(item: MediaItemEntity): Long

    @Update
    suspend fun updateDownload(item: MediaItemEntity)

    @Query("""
        UPDATE media_downloads 
        SET downloadedBytes = :downloadedBytes,
            fileSizeBytes = CASE WHEN :fileSizeBytes > 0 THEN :fileSizeBytes ELSE fileSizeBytes END,
            progress = :progress,
            speedText = :speedText,
            status = :status,
            completedAt = CASE WHEN :status = 'COMPLETED' THEN :completedAt ELSE completedAt END,
            localFilePath = CASE WHEN :localPath IS NOT NULL THEN :localPath ELSE localFilePath END,
            contentUri = CASE WHEN :contentUri IS NOT NULL THEN :contentUri ELSE contentUri END,
            errorMessage = :errorMessage
        WHERE id = :id
    """)
    suspend fun updateDownloadProgress(
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
    )

    @Query("DELETE FROM media_downloads WHERE id = :id")
    suspend fun deleteDownload(id: Long)

    @Query("DELETE FROM media_downloads WHERE status = 'COMPLETED'")
    suspend fun clearCompletedDownloads()

    // Statuses
    @Query("SELECT * FROM saved_statuses ORDER BY savedAt DESC")
    fun getAllSavedStatuses(): Flow<List<SavedStatusEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedStatus(status: SavedStatusEntity): Long

    @Query("DELETE FROM saved_statuses WHERE id = :id")
    suspend fun deleteSavedStatus(id: Long)
}
