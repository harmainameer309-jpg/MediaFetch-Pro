package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CoralRed
import com.example.ui.theme.EmberOrange
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateDark800
import com.example.ui.theme.SlateDark900
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaFetchTopBar(
    backendUrl: String,
    onNavigateSettings: () -> Unit
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = SlateDark900,
            titleContentColor = SlateTextPrimary
        ),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(EmberOrange, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = "Logo",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Row {
                    Text(
                        text = "MediaFetch",
                        fontWeight = FontWeight.Black,
                        fontSize = 19.sp,
                        color = SlateTextPrimary
                    )
                    Text(
                        text = " PRO",
                        fontWeight = FontWeight.Black,
                        fontSize = 19.sp,
                        color = EmberOrange
                    )
                }
            }
        },
        actions = {
            // Engine status badge
            val hasBackend = backendUrl.isNotBlank()
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (hasBackend) SuccessGreen.copy(alpha = 0.2f) else SlateDark800)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(if (hasBackend) SuccessGreen else EmberOrange, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (hasBackend) "Render" else "Direct",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasBackend) SuccessGreen else EmberOrange
                    )
                }
            }

            IconButton(
                onClick = onNavigateSettings,
                modifier = Modifier
                    .testTag("topbar_settings_button")
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = SlateTextSecondary
                )
            }
        }
    )
}

@Composable
fun MediaFetchBottomBar(
    currentTab: String,
    activeDownloadCount: Int,
    onTabSelected: (String) -> Unit
) {
    NavigationBar(
        containerColor = SlateDark900,
        contentColor = SlateTextPrimary
    ) {
        val items = listOf(
            Triple("home", "Fetch", Icons.Default.Home),
            Triple("browser", "Browser", Icons.Default.Language),
            Triple("status", "Status", Icons.Default.SaveAlt),
            Triple("downloads", "Downloads", Icons.Default.Download),
            Triple("settings", "Setup", Icons.Default.Settings)
        )

        items.forEach { (tabId, label, icon) ->
            val isSelected = currentTab == tabId
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tabId) },
                icon = {
                    if (tabId == "downloads" && activeDownloadCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(containerColor = CoralRed) {
                                    Text(
                                        text = "$activeDownloadCount",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        ) {
                            Icon(imageVector = icon, contentDescription = label)
                        }
                    } else {
                        Icon(imageVector = icon, contentDescription = label)
                    }
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = EmberOrange,
                    indicatorColor = EmberOrange,
                    unselectedIconColor = SlateTextSecondary,
                    unselectedTextColor = SlateTextSecondary
                ),
                modifier = Modifier.testTag("nav_tab_$tabId")
            )
        }
    }
}
