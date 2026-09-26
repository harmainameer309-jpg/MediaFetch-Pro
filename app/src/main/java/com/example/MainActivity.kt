package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MediaFetchViewModel
import com.example.ui.components.MediaFetchBottomBar
import com.example.ui.components.MediaFetchTopBar
import com.example.ui.components.QualitySelectorDialog
import com.example.ui.components.VideoPlayerDialog
import com.example.ui.screens.BrowserScreen
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.HomeFetchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatusSaverScreen
import com.example.ui.theme.MediaFetchProTheme
import com.example.ui.theme.SlateDark900

class MainActivity : ComponentActivity() {

    private val viewModel: MediaFetchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            MediaFetchProTheme(darkTheme = true) {
                MediaFetchApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        val action = intent.action
        val type = intent.type

        if (Intent.ACTION_SEND == action && type != null) {
            if (type.startsWith("video/") || type.startsWith("image/")) {
                // Media shared from WhatsApp Status or Gallery
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
                }

                if (uri != null) {
                    viewModel.handleSharedMediaUri(uri)
                }
            } else if (type == "text/plain") {
                // Link shared from YouTube, TikTok, Facebook, Browser
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (!sharedText.isNullOrBlank()) {
                    // Extract URL if surrounded by text
                    val url = extractUrl(sharedText)
                    viewModel.onUrlChanged(url)
                    viewModel.selectTab("home")
                    viewModel.inspectUrl(url)
                }
            }
        }
    }

    private fun extractUrl(text: String): String {
        val pattern = "(https?://[\\w.-]+(?:\\.[\\w\\.-]+)+[/\\w\\._~:?#\\[\\]@!$&'()*+,;=.-]*)".toRegex()
        val match = pattern.find(text)
        return match?.value ?: text.trim()
    }
}

@Composable
fun MediaFetchApp(viewModel: MediaFetchViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val urlInput by viewModel.urlInput.collectAsStateWithLifecycle()
    val isInspecting by viewModel.isInspecting.collectAsStateWithLifecycle()
    val inspectError by viewModel.inspectError.collectAsStateWithLifecycle()
    val showQualityDialog by viewModel.showQualityDialog.collectAsStateWithLifecycle()
    val inspectedMedia by viewModel.inspectedMedia.collectAsStateWithLifecycle()
    val backendUrl by viewModel.backendUrl.collectAsStateWithLifecycle()
    val backendStatus by viewModel.backendStatus.collectAsStateWithLifecycle()
    val activeDownloads by viewModel.activeDownloads.collectAsStateWithLifecycle()
    val completedDownloads by viewModel.completedDownloads.collectAsStateWithLifecycle()
    val savedStatuses by viewModel.savedStatuses.collectAsStateWithLifecycle()
    val playingMedia by viewModel.playingMedia.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackBarMessage.collectAsStateWithLifecycle()
    val browserUrl by viewModel.browserUrl.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackBar()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = SlateDark900,
        topBar = {
            MediaFetchTopBar(
                backendUrl = backendUrl,
                onNavigateSettings = { viewModel.selectTab("settings") }
            )
        },
        bottomBar = {
            MediaFetchBottomBar(
                currentTab = selectedTab,
                activeDownloadCount = activeDownloads.size,
                onTabSelected = { viewModel.selectTab(it) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(SlateDark900)
        ) {
            when (selectedTab) {
                "home" -> {
                    HomeFetchScreen(
                        urlInput = urlInput,
                        isInspecting = isInspecting,
                        inspectError = inspectError,
                        onUrlChange = { viewModel.onUrlChanged(it) },
                        onInspectClick = { viewModel.inspectUrl() },
                        onLoadPreset = { viewModel.loadPreset(it) },
                        onNavigateToStatus = { viewModel.selectTab("status") },
                        onNavigateToBrowser = { viewModel.selectTab("browser") }
                    )
                }
                "browser" -> {
                    BrowserScreen(
                        currentUrl = browserUrl,
                        onUrlChange = { viewModel.setBrowserUrl(it) },
                        onDownloadDetectedUrl = { detected ->
                            viewModel.onUrlChanged(detected)
                            viewModel.inspectUrl(detected)
                        }
                    )
                }
                "status" -> {
                    StatusSaverScreen(
                        savedStatuses = savedStatuses,
                        onImportMediaUri = { viewModel.handleSharedMediaUri(it) },
                        onPlayMedia = { title, path -> viewModel.playMedia(title, path) },
                        onDeleteStatus = { viewModel.deleteStatus(it) }
                    )
                }
                "downloads" -> {
                    DownloadsScreen(
                        activeDownloads = activeDownloads,
                        completedDownloads = completedDownloads,
                        onPauseDownload = { viewModel.pauseDownload(it) },
                        onResumeDownload = { viewModel.resumeDownload(it) },
                        onCancelDownload = { viewModel.cancelDownload(it) },
                        onDeleteCompleted = { viewModel.deleteCompleted(it) },
                        onClearCompleted = { viewModel.clearAllCompleted() },
                        onPlayMedia = { title, path -> viewModel.playMedia(title, path) }
                    )
                }
                "settings" -> {
                    SettingsScreen(
                        backendUrl = backendUrl,
                        backendStatus = backendStatus,
                        onSaveBackendUrl = { viewModel.updateBackendUrl(it) },
                        onTestBackend = { viewModel.testBackendConnection(it) }
                    )
                }
            }
        }

        // Quality selection modal
        if (showQualityDialog && inspectedMedia != null) {
            QualitySelectorDialog(
                mediaInfo = inspectedMedia!!,
                onDismiss = { viewModel.dismissQualityDialog() },
                onConfirmDownload = { media, format ->
                    viewModel.startDownloadWithFormat(media, format)
                }
            )
        }

        // In-App Video & Audio player modal
        playingMedia?.let { (title, path) ->
            VideoPlayerDialog(
                title = title,
                pathOrUrl = path,
                onDismiss = { viewModel.closePlayer() }
            )
        }
    }
}
