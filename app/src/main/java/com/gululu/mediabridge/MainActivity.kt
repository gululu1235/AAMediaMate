@file:OptIn(ExperimentalMaterial3Api::class)
package com.gululu.mediabridge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gululu.mediabridge.ui.*
import androidx.compose.runtime.livedata.observeAsState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MediaBridgeApp()
        }
    }
}

@Composable
fun MediaBridgeApp() {
    var showSettings by remember { mutableStateOf(false) }
    var showLyricsManager by remember { mutableStateOf(false) }
    var selectedLyricsKey by remember { mutableStateOf<String?>(null) }

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
            onOpenSettings = { showSettings = true },
            onOpenLyricsManager = { showLyricsManager = true }
        )
    }
}

@Composable
fun MainScreen(onOpenSettings: () -> Unit,
               onOpenLyricsManager: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.app_name)) },
                actions = {
                    var expanded by remember { mutableStateOf(false) }

                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.settings_title)) },
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
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            LogDisplay()
        }
    }
}

@Composable
fun LogDisplay() {
    val logs by LogBuffer.logs.observeAsState("")
    Text(text = logs)
}
