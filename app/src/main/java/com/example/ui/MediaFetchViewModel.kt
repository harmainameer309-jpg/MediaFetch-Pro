package com.example.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.MediaItemEntity
import com.example.data.db.MediaRepository
import com.example.data.db.SavedStatusEntity
import com.example.data.model.MediaInfo
import com.example.data.model.VideoFormat
import com.example.data.network.MediaEngine
import com.example.service.MediaDownloadManager
import com.example.service.StatusSaverManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MediaFetchViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = MediaRepository(db.mediaDao())
    private val mediaEngine = MediaEngine(application)
    private val downloadManager = MediaDownloadManager(application, repository, viewModelScope)
    val statusSaverManager = StatusSaverManager(application, repository)

    private val prefs: SharedPreferences =
        application.getSharedPreferences("mediafetch_prefs", Context.MODE_PRIVATE)

    // UI States
    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _isInspecting = MutableStateFlow(false)
    val isInspecting: StateFlow<Boolean> = _isInspecting.asStateFlow()

    private val _inspectedMedia = MutableStateFlow<MediaInfo?>(null)
    val inspectedMedia: StateFlow<MediaInfo?> = _inspectedMedia.asStateFlow()

    private val _inspectError = MutableStateFlow<String?>(null)
    val inspectError: StateFlow<String?> = _inspectError.asStateFlow()

    private val _showQualityDialog = MutableStateFlow(false)
    val showQualityDialog: StateFlow<Boolean> = _showQualityDialog.asStateFlow()

    private val _backendUrl = MutableStateFlow(prefs.getString("backend_url", "") ?: "")
    val backendUrl: StateFlow<String> = _backendUrl.asStateFlow()

    private val _backendStatus = MutableStateFlow("Ready (Direct Engine Active)")
    val backendStatus: StateFlow<String> = _backendStatus.asStateFlow()

    private val _selectedTab = MutableStateFlow("home") // "home", "browser", "status", "downloads", "settings"
    val selectedTab: StateFlow<String> = _selectedTab.asStateFlow()

    private val _playingMedia = MutableStateFlow<Pair<String, String>?>(null) // (Title, LocalPath or StreamUrl)
    val playingMedia: StateFlow<Pair<String, String>?> = _playingMedia.asStateFlow()

    private val _snackBarMessage = MutableStateFlow<String?>(null)
    val snackBarMessage: StateFlow<String?> = _snackBarMessage.asStateFlow()

    // Browser detected media
    private val _browserUrl = MutableStateFlow("https://m.youtube.com")
    val browserUrl: StateFlow<String> = _browserUrl.asStateFlow()

    private val _detectedBrowserMedia = MutableStateFlow<MediaInfo?>(null)
    val detectedBrowserMedia: StateFlow<MediaInfo?> = _detectedBrowserMedia.asStateFlow()

    // Reactive streams from Room DB
    val activeDownloads: StateFlow<List<MediaItemEntity>> = repository.activeDownloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedDownloads: StateFlow<List<MediaItemEntity>> = repository.completedDownloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedStatuses: StateFlow<List<SavedStatusEntity>> = repository.savedStatuses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Test backend ping if already configured
        if (_backendUrl.value.isNotBlank()) {
            testBackendConnection(_backendUrl.value)
        }
    }

    fun onUrlChanged(newUrl: String) {
        _urlInput.value = newUrl
        if (_inspectError.value != null) _inspectError.value = null
    }

    fun selectTab(tab: String) {
        _selectedTab.value = tab
    }

    fun inspectUrl(targetUrl: String? = null) {
        val url = (targetUrl ?: _urlInput.value).trim()
        if (url.isBlank()) {
            _inspectError.value = "Please enter or paste a valid link"
            return
        }

        _isInspecting.value = true
        _inspectError.value = null
        viewModelScope.launch {
            val result = mediaEngine.inspectUrl(url, _backendUrl.value)
            _isInspecting.value = false
            result.fold(
                onSuccess = { info ->
                    _inspectedMedia.value = info
                    _showQualityDialog.value = true
                },
                onFailure = { error ->
                    _inspectError.value = error.localizedMessage ?: "Failed to inspect media"
                }
            )
        }
    }

    fun loadPreset(preset: MediaInfo) {
        _inspectedMedia.value = preset
        _urlInput.value = preset.originalUrl
        _showQualityDialog.value = true
    }

    fun dismissQualityDialog() {
        _showQualityDialog.value = false
    }

    fun startDownloadWithFormat(mediaInfo: MediaInfo, format: VideoFormat) {
        _showQualityDialog.value = false
        viewModelScope.launch {
            val item = MediaItemEntity(
                title = mediaInfo.title,
                originalUrl = mediaInfo.originalUrl,
                downloadUrl = format.downloadUrl,
                thumbnailUrl = mediaInfo.thumbnailUrl,
                mediaType = if (format.isAudioOnly) "audio" else "video",
                qualityLabel = format.label,
                fileSizeBytes = format.estimatedSizeBytes,
                downloadedBytes = 0L,
                progress = 0f,
                status = "QUEUED",
                platform = mediaInfo.platform
            )

            val id = repository.insertDownload(item)
            val insertedItem = item.copy(id = id)
            downloadManager.startDownload(insertedItem, _backendUrl.value)

            _snackBarMessage.value = "Started downloading: ${mediaInfo.title.take(30)}..."
            _selectedTab.value = "downloads"
        }
    }

    fun pauseDownload(id: Long) {
        downloadManager.pauseDownload(id)
    }

    fun resumeDownload(item: MediaItemEntity) {
        downloadManager.resumeDownload(item, _backendUrl.value)
    }

    fun cancelDownload(id: Long) {
        downloadManager.cancelDownload(id)
    }

    fun deleteCompleted(id: Long) {
        viewModelScope.launch {
            repository.deleteDownload(id)
        }
    }

    fun clearAllCompleted() {
        viewModelScope.launch {
            repository.clearCompleted()
        }
    }

    // Media Player
    fun playMedia(title: String, pathOrUrl: String) {
        _playingMedia.value = Pair(title, pathOrUrl)
    }

    fun closePlayer() {
        _playingMedia.value = null
    }

    // WhatsApp Status / Shared media
    fun handleSharedMediaUri(uri: Uri) {
        viewModelScope.launch {
            _snackBarMessage.value = "Saving shared media to Movies/MediaFetch..."
            val result = statusSaverManager.importMedia(uri)
            result.fold(
                onSuccess = { saved ->
                    _snackBarMessage.value = "Saved to Movies/MediaFetch: ${saved.title}"
                    _selectedTab.value = "status"
                },
                onFailure = { err ->
                    _snackBarMessage.value = "Error saving: ${err.localizedMessage}"
                }
            )
        }
    }

    fun deleteStatus(status: SavedStatusEntity) {
        viewModelScope.launch {
            statusSaverManager.deleteStatus(status)
            _snackBarMessage.value = "Deleted status item"
        }
    }

    // Backend configuration
    fun updateBackendUrl(newUrl: String) {
        val trimmed = newUrl.trim()
        _backendUrl.value = trimmed
        prefs.edit().putString("backend_url", trimmed).apply()
        testBackendConnection(trimmed)
    }

    fun testBackendConnection(url: String) {
        if (url.isBlank()) {
            _backendStatus.value = "No backend configured (Direct Engine active)"
            return
        }

        viewModelScope.launch {
            _backendStatus.value = "Testing connection..."
            val ok = mediaEngine.pingBackend(url)
            _backendStatus.value = if (ok) "Connected to Render / Docker Server" else "Connection failed (Offline/Invalid URL)"
        }
    }

    // In-app Browser
    fun setBrowserUrl(url: String) {
        _browserUrl.value = url
    }

    fun onBrowserMediaDetected(mediaInfo: MediaInfo) {
        _detectedBrowserMedia.value = mediaInfo
    }

    fun clearDetectedBrowserMedia() {
        _detectedBrowserMedia.value = null
    }

    fun clearSnackBar() {
        _snackBarMessage.value = null
    }
}
