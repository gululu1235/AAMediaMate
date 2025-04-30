@file:OptIn(ExperimentalMaterial3Api::class)

package com.gululu.aamediamate.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.gululu.aamediamate.R
import com.gululu.aamediamate.SettingsManager

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    BackHandler {
        onBack()
    }

    val context = LocalContext.current

    var lyricsEnabled by remember { mutableStateOf(SettingsManager.getLyricsEnabled(context)) }
    var apiKey by remember { mutableStateOf(SettingsManager.getApiKey(context)) }
    var lrcCxUri by remember { mutableStateOf(SettingsManager.getLrcCxBaseUri(context)) }
    var simplifyChinese by remember { mutableStateOf(SettingsManager.getSimplifyEnabled(context)) }
    var ignoreNativeAutoApps by remember { mutableStateOf(SettingsManager.getIgnoreNativeAutoApps(context)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(id = R.string.show_lyrics), modifier = Modifier.weight(1f))
                Switch(
                    checked = lyricsEnabled,
                    onCheckedChange = {
                        lyricsEnabled = it
                        SettingsManager.setLyricsEnabled(context, it)
                    }
                )
            }

            // Musixmatch API Key
            Text(text = stringResource(id = R.string.musixmatch_api_key))
            OutlinedTextField(
                value = apiKey,
                onValueChange = {
                    apiKey = it
                    SettingsManager.setApiKey(context, it)
                },
                label = { Text(stringResource(id = R.string.musixmatch_api_key)) },
                singleLine = true,
                visualTransformation = VisualTransformation.None,
                modifier = Modifier.fillMaxWidth()
            )

            // LrcCx Uri
            Text(text = stringResource(id = R.string.lrccx_api_uri))
            OutlinedTextField(
                value = lrcCxUri,
                onValueChange = {
                    lrcCxUri = it
                    SettingsManager.setLrcCxBaseUri(context, it)
                },
                label = { Text(stringResource(id = R.string.lrccx_api_uri)) },
                singleLine = true,
                visualTransformation = VisualTransformation.None,
                modifier = Modifier.fillMaxWidth()
            )

            // 繁体转简体开关
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(id = R.string.simplify_chinese), modifier = Modifier.weight(1f))
                Switch(
                    checked = simplifyChinese,
                    onCheckedChange = {
                        simplifyChinese = it
                        SettingsManager.setSimplifyEnabled(context, it)
                    }
                )
            }

            // 🚗 忽略原生 Android Auto App 开关
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(id = R.string.ignore_auto_supported_apps), modifier = Modifier.weight(1f))
                Switch(
                    checked = ignoreNativeAutoApps,
                    onCheckedChange = {
                        ignoreNativeAutoApps = it
                        SettingsManager.setIgnoreNativeAutoApps(context, it)
                    }
                )
            }
        }
    }
}
