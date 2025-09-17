@file:OptIn(ExperimentalMaterial3Api::class)
package com.gululu.aamediamate

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.Settings
import android.util.Log
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.gululu.aamediamate.models.MediaInfo
import com.gululu.aamediamate.ui.LyricsEditorScreen
import com.gululu.aamediamate.ui.LyricsManagerScreen
import com.gululu.aamediamate.ui.SettingsScreen
import com.gululu.aamediamate.ui.BridgedAppsScreen
import java.util.Locale

val DeepPurpleBackground = Color(0xFF1B1B2F)
val CardBackgroundColor = Color(0xFF2B2B40)
val LeftTextColor = Color(0xFFAAAAAA)
val RightTextColor = Color(0xFFFFFFFF)

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val langCode = SettingsManager.getLanguagePreference(newBase).split("_")
        Log.d("mediaBridge", "$langCode")
        val language = langCode[0]
        val country = if (langCode.size <= 1) "" else langCode[1]
        val context = applyLanguage(newBase, language, country)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MediaBridgeApp()
        }
    }
}

fun hasNotificationAccess(context: Context): Boolean {
    val enabledPackages = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners"
    ) ?: return false

    val packageName = context.packageName
    return enabledPackages.contains(packageName)
}

fun openNotificationAccessSettings(context: Context) {
    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    context.startActivity(intent)
}

fun applyLanguage(context: Context, language: String, country: String): Context {
    if (language.isEmpty()) return context

    Log.d("Media Bridge", "Setting locale to $language $country")
    val locale = Locale(language, country)
    Locale.setDefault(locale)

    val config = context.resources.configuration
    config.setLocale(locale)
    return context.createConfigurationContext(config)
}

@Preview
@Composable
fun MediaBridgeApp() {
    val context = LocalContext.current

    var showSettings by remember { mutableStateOf(false) }
    var showLyricsManager by remember { mutableStateOf(false) }
    var showBridgedApps by remember { mutableStateOf(false) }
    var selectedLyricsKey by remember { mutableStateOf<String?>(null) }
    var currentMediaInfo by remember { mutableStateOf<MediaInfo?>(null) }

    LaunchedEffect(Unit) {
        currentMediaInfo = MediaInformationRetriever.refreshCurrentMediaInfo(context )
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
        showBridgedApps -> BridgedAppsScreen { showBridgedApps = false }
        showSettings -> SettingsScreen(
            onBack = { showSettings = false }
        )
        showLyricsManager -> LyricsManagerScreen(
            onBack = { showLyricsManager = false },
            onOpenEditor = { lyricsKey -> selectedLyricsKey = lyricsKey },
            onNavigateToBridgedApps = { showBridgedApps = true }
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
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        hasPermission = hasNotificationAccess(context)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = hasNotificationAccess(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_title), color = RightTextColor) },
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
                if (!hasPermission) {
                    NotificationAccessBanner {
                        openNotificationAccessSettings(context)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                AlbumArtWithShadow(mediaInfo?.albumArt)

                Spacer(modifier = Modifier.height(32.dp))

                MediaInfoRow(
                    leftText = stringResource(id = R.string.playing_from),
                    rightText = mediaInfo?.appName ?: "--",
                    rightIcon = if (!mediaInfo?.appPackageName.isNullOrEmpty()) Icons.AutoMirrored.Filled.KeyboardArrowRight else null,
                    onClick = {
                        mediaInfo?.let { onOpenApp(it.appPackageName) }
                    }
                )

                MediaInfoRow(
                    leftText = stringResource(id = R.string.currently_playing),
                    rightText = if (mediaInfo != null) "${mediaInfo.title} - ${mediaInfo.artist}" else "--"
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
        Image(
            painter = painterResource(id = R.drawable.app_icon),
            contentDescription = "App icon",
            modifier = Modifier.size(240.dp)
        )
    }
}

@Composable
fun NotificationAccessBanner(onFixClicked: () -> Unit) {
    Surface(
        color = Color(0xFFFFE0B2),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.message_enable_permission),
                color = Color(0xFF5D4037),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = onFixClicked) {
                Text(stringResource(R.string.direct_to_settings))
            }
        }
    }
}



