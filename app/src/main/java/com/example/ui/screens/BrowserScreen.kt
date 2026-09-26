package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.EmberOrange
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateDark800
import com.example.ui.theme.SlateDark900
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    currentUrl: String,
    onUrlChange: (String) -> Unit,
    onDownloadDetectedUrl: (String) -> Unit
) {
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var inputUrl by remember(currentUrl) { mutableStateOf(currentUrl) }
    var pageProgress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(false) }
    var detectedMediaUrl by remember { mutableStateOf<String?>(null) }

    val keyboardController = LocalSoftwareKeyboardController.current

    val bookmarks = listOf(
        Pair("YouTube", "https://m.youtube.com"),
        Pair("TikTok", "https://www.tiktok.com"),
        Pair("Archive Movies", "https://archive.org/details/movies"),
        Pair("Vimeo", "https://vimeo.com/watch"),
        Pair("Dailymotion", "https://www.dailymotion.com")
    )

    BackHandler(enabled = webViewInstance?.canGoBack() == true) {
        webViewInstance?.goBack()
    }

    Box(modifier = Modifier.fillMaxSize().background(SlateDark900)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // URL Bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(containerColor = SlateDark800)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { webViewInstance?.goBack() },
                            enabled = webViewInstance?.canGoBack() == true,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = if (webViewInstance?.canGoBack() == true) SlateTextPrimary else SlateTextSecondary.copy(alpha = 0.4f)
                            )
                        }

                        IconButton(
                            onClick = { webViewInstance?.goForward() },
                            enabled = webViewInstance?.canGoForward() == true,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Forward",
                                tint = if (webViewInstance?.canGoForward() == true) SlateTextPrimary else SlateTextSecondary.copy(alpha = 0.4f)
                            )
                        }

                        IconButton(
                            onClick = { webViewInstance?.reload() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reload",
                                tint = SlateTextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        OutlinedTextField(
                            value = inputUrl,
                            onValueChange = { inputUrl = it },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("browser_address_bar"),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                            placeholder = { Text("Search or type URL...", fontSize = 12.sp, color = SlateTextSecondary) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(
                                onGo = {
                                    keyboardController?.hide()
                                    var formatted = inputUrl.trim()
                                    if (!formatted.startsWith("http://") && !formatted.startsWith("https://")) {
                                        formatted = if (formatted.contains(".") && !formatted.contains(" ")) {
                                            "https://$formatted"
                                        } else {
                                            "https://www.google.com/search?q=${java.net.URLEncoder.encode(formatted, "UTF-8")}"
                                        }
                                    }
                                    inputUrl = formatted
                                    onUrlChange(formatted)
                                    webViewInstance?.loadUrl(formatted)
                                }
                            ),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmberOrange,
                                unfocusedBorderColor = SlateBorder,
                                focusedContainerColor = SlateDark900,
                                unfocusedContainerColor = SlateDark900,
                                focusedTextColor = SlateTextPrimary,
                                unfocusedTextColor = SlateTextPrimary
                            )
                        )
                    }

                    // Bookmarks Row
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                    ) {
                        items(bookmarks) { (name, url) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SlateDark900)
                                    .clickable {
                                        inputUrl = url
                                        onUrlChange(url)
                                        webViewInstance?.loadUrl(url)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = name,
                                    color = SlateTextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Loading bar
            if (isLoading && pageProgress < 1f) {
                LinearProgressIndicator(
                    progress = { pageProgress },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = EmberOrange,
                    trackColor = SlateDark900
                )
            }

            // WebView
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.mediaPlaybackRequiresUserGesture = false

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                pageProgress = newProgress / 100f
                                isLoading = newProgress < 100
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                                url?.let {
                                    inputUrl = it
                                    onUrlChange(it)
                                    checkUrlForMedia(it) { detected ->
                                        detectedMediaUrl = detected
                                    }
                                }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                url?.let {
                                    inputUrl = it
                                    checkUrlForMedia(it) { detected ->
                                        detectedMediaUrl = detected
                                    }
                                }
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val reqUrl = request?.url?.toString() ?: return false
                                checkUrlForMedia(reqUrl) { detected ->
                                    detectedMediaUrl = detected
                                }
                                return false
                            }
                        }

                        loadUrl(currentUrl)
                        webViewInstance = this
                    }
                },
                update = { view ->
                    if (view.url != currentUrl && !currentUrl.isBlank()) {
                        view.loadUrl(currentUrl)
                    }
                }
            )
        }

        // Sniffer Floating Action Button
        AnimatedVisibility(
            visible = detectedMediaUrl != null || currentUrl.contains("youtube") || currentUrl.contains("tiktok"),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            val target = detectedMediaUrl ?: currentUrl
            ExtendedFloatingActionButton(
                onClick = {
                    onDownloadDetectedUrl(target)
                },
                containerColor = EmberOrange,
                contentColor = Color.White,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download detected media",
                        modifier = Modifier.size(24.dp)
                    )
                },
                text = {
                    Text(
                        text = "Fetch This Page",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                },
                modifier = Modifier.testTag("browser_download_media_fab")
            )
        }
    }
}

private fun checkUrlForMedia(url: String, onDetected: (String) -> Unit) {
    val lower = url.lowercase()
    if (lower.contains(".mp4") || lower.contains(".webm") || lower.contains(".mp3") ||
        lower.contains("watch?v=") || lower.contains("tiktok.com/@") || lower.contains("fb.watch")) {
        onDetected(url)
    }
}
