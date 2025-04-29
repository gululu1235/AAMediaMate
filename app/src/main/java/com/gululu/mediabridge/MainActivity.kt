@file:OptIn(ExperimentalMaterial3Api::class)
package com.gululu.mediabridge

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gululu.mediabridge.models.MediaInfo
import com.gululu.mediabridge.ui.LyricsEditorScreen
import com.gululu.mediabridge.ui.LyricsManagerScreen
import com.gululu.mediabridge.ui.SettingsScreen

// 颜色统一管理
val DeepPurpleBackground = Color(0xFF1B1B2F)  // 背景深紫
val CardBackgroundColor = Color(0xFF2B2B40)    // 每行Card颜色，偏浅一点紫灰
val LeftTextColor = Color(0xFFAAAAAA)          // 左边文字浅灰
val RightTextColor = Color(0xFFFFFFFF)         // 右边文字白色

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MediaBridgeApp()
        }
    }
}

@Preview
@Composable
fun MediaBridgeApp() {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val intent = Intent(context, MediaBridgeService::class.java)
        context.startService(intent)
    }

    var showSettings by remember { mutableStateOf(false) }
    var showLyricsManager by remember { mutableStateOf(false) }
    var selectedLyricsKey by remember { mutableStateOf<String?>(null) }
    var currentMediaInfo by remember { mutableStateOf<MediaInfo?>(null) }

    LaunchedEffect(Unit) {
        MediaBridgeSessionManager.setMediaInfoListener { info ->
            currentMediaInfo = info
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            MediaBridgeSessionManager.clearMediaInfoListener()
        }
    }

    when {
        selectedLyricsKey != null -> LyricsEditorScreen(
            lyricsKey = selectedLyricsKey!!,
            onBack = { selectedLyricsKey = null },
            onDeleted = {
                selectedLyricsKey = null
            }
        )
        showSettings -> SettingsScreen { showSettings = false }
        showLyricsManager -> LyricsManagerScreen(
            onBack = { showLyricsManager = false },
            onOpenEditor = { lyricsKey -> selectedLyricsKey = lyricsKey }
        )
        else -> MainScreen(
            mediaInfo = currentMediaInfo,
            isLyricsEnabled = SettingsManager.getLyricsEnabled(context),
            onOpenSettings = { showSettings = true },
            onOpenLyricsManager = { showLyricsManager = true },
            onOpenLyricsEditor = { title, artist ->
                selectedLyricsKey = title + "_" + artist
            },
            onOpenApp = { packageName ->
                val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            }
        )
    }
}

@Composable
fun MainScreen(
    mediaInfo: MediaInfo?,
    isLyricsEnabled: Boolean,
    onOpenSettings: () -> Unit,
    onOpenLyricsManager: () -> Unit,
    onOpenLyricsEditor: (String, String) -> Unit,
    onOpenApp: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Media Bridge", color = RightTextColor) },
                actions = {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = RightTextColor)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.settings)) },
                            onClick = {
                                expanded = false
                                onOpenSettings()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.lyrics_manager)) },
                            onClick = {
                                expanded = false
                                onOpenLyricsManager()
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepPurpleBackground
                )
            )
        },
        containerColor = DeepPurpleBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DeepPurpleBackground)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                AlbumArtWithShadow(mediaInfo?.albumArt)

                Spacer(modifier = Modifier.height(32.dp))

                MediaInfoRow(
                    leftText = stringResource(id = R.string.playing_from),
                    rightText = mediaInfo?.appName ?: "",
                    rightIcon = if (!mediaInfo?.appName.isNullOrEmpty()) Icons.AutoMirrored.Filled.KeyboardArrowRight else null,
                    onClick = {
                        mediaInfo?.let { onOpenApp(it.appPackageName) }
                    }
                )

                MediaInfoRow(
                    leftText = stringResource(id = R.string.currently_playing),
                    rightText = if (mediaInfo != null) "${mediaInfo.title} - ${mediaInfo.artist}" else ""
                )

                MediaInfoRow(
                    leftText = "",
                    rightComposable = {
                        if (isLyricsEnabled && mediaInfo != null) {
                            Row(
                                modifier = Modifier.clickable {
                                    onOpenLyricsEditor(mediaInfo.title, mediaInfo.artist)
                                },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(id = R.string.edit_current_lyrics),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = RightTextColor
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = RightTextColor
                                )
                            }
                        } else {
                            Text(
                                text = stringResource(id = R.string.lyrics_not_enabled),
                                style = MaterialTheme.typography.bodyMedium,
                                color = LeftTextColor
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun MediaInfoRow(
    leftText: String,
    rightText: String = "",
    rightIcon: ImageVector? = null,
    rightComposable: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        colors = CardDefaults.cardColors(containerColor = CardBackgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = leftText,
                style = MaterialTheme.typography.bodyMedium,
                color = LeftTextColor
            )
            if (rightComposable != null) {
                rightComposable()
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = rightText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = RightTextColor,
                        softWrap = true
                    )
                    if (rightIcon != null) {
                        Icon(
                            imageVector = rightIcon,
                            contentDescription = null,
                            tint = RightTextColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AlbumArtWithShadow(albumArtBitmap: Bitmap?) {
    if (albumArtBitmap != null) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(240.dp)
                .shadow(
                    elevation = 24.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = Color(0x66FFFFFF), // 白色浅阴影
                    spotColor = Color(0x66FFFFFF),
                    clip = false
                )
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black)
        ) {
            Image(
                bitmap = albumArtBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
            )
        }
    } else {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Default icon",
            modifier = Modifier
                .size(240.dp),
            tint = Color(0xFF888888)
        )
    }
}



