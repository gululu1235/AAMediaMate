@file:OptIn(ExperimentalMaterial3Api::class)

package com.gululu.aamediamate.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gululu.aamediamate.R
import com.gululu.aamediamate.SettingsManager
import com.gululu.aamediamate.models.BridgedApp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BridgedAppsScreen(onBack: () -> Unit) {
    BackHandler {
        onBack()
    }

    val context = LocalContext.current
    var bridgedApps by remember { mutableStateOf(SettingsManager.getBridgedApps(context)) }
    
    // Refresh the list when the screen is composed
    LaunchedEffect(Unit) {
        bridgedApps = SettingsManager.getBridgedApps(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.bridged_apps_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = stringResource(id = R.string.bridged_apps_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (bridgedApps.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(id = R.string.no_bridged_apps),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(bridgedApps.sortedByDescending { it.lastSeen }) { app ->
                    BridgedAppItem(
                        app = app,
                        onToggleLyrics = { enabled ->
                            SettingsManager.setAppLyricsEnabled(context, app.packageName, enabled)
                            bridgedApps = SettingsManager.getBridgedApps(context)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BridgedAppItem(
    app: BridgedApp,
    onToggleLyrics: (Boolean) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Last seen: ${dateFormat.format(Date(app.lastSeen))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Switch(
                        checked = app.lyricsEnabled,
                        onCheckedChange = onToggleLyrics
                    )
                    Text(
                        text = if (app.lyricsEnabled) 
                            stringResource(id = R.string.lyrics_enabled) 
                        else 
                            stringResource(id = R.string.lyrics_disabled),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (app.lyricsEnabled) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}