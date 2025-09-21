@file:OptIn(ExperimentalMaterial3Api::class)

package com.gululu.aamediamate.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.gululu.aamediamate.MainActivity
import com.gululu.aamediamate.R
import com.gululu.aamediamate.SettingsManager
import com.gululu.aamediamate.models.LanguageOption

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    BackHandler {
        onBack()
    }

    val context = LocalContext.current

    var lyricsEnabled by remember { mutableStateOf(SettingsManager.getLyricsEnabled(context)) }
    var apiKey by remember { mutableStateOf(SettingsManager.getApiKey(context)) }
    var lrcApiUri by remember { mutableStateOf(SettingsManager.getLrcApiBaseUri(context)) }
    var simplifyChinese by remember { mutableStateOf(SettingsManager.getSimplifyEnabled(context)) }
    var ignoreNativeAutoApps by remember { mutableStateOf(SettingsManager.getIgnoreNativeAutoApps(context)) }
    var showLyricsConfirmationDialog by remember { mutableStateOf(false) }
    var pendingEnableLyrics by remember { mutableStateOf(false) }

    val languageOptions = listOf(
        LanguageOption.SYSTEM,
        LanguageOption.ENGLISH,
        LanguageOption.SIMPLIFIED_CHINESE,
        LanguageOption.TRADITIONAL_CHINESE
    )
    var selectedLanguage by remember {
        mutableStateOf(
            languageOptions.firstOrNull {
                "${it.language}_${it.country}" == SettingsManager.getLanguagePreference(context)
            } ?: LanguageOption.SYSTEM
        )
    }
    var expanded by remember { mutableStateOf(false) }

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
            Text(text = stringResource(id = R.string.language_title), style = MaterialTheme.typography.labelLarge)
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                TextField(
                    readOnly = true,
                    value = selectedLanguage.displayName,
                    onValueChange = {},
                    label = { Text(stringResource(id = R.string.language)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    languageOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.displayName) },
                            onClick = {
                                selectedLanguage = option
                                expanded = false
                                SettingsManager.setLanguagePreference(context, option)

                                val intent = Intent(context, MainActivity::class.java)
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                context.startActivity(intent)
                                Runtime.getRuntime().exit(0)
                            }
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(id = R.string.show_lyrics), modifier = Modifier.weight(1f))
                Switch(
                    checked = lyricsEnabled,
                    onCheckedChange = { checked ->
                        if (checked) {
                            pendingEnableLyrics = true
                            showLyricsConfirmationDialog = true
                        } else {
                            lyricsEnabled = false
                            SettingsManager.setLyricsEnabled(context, false)
                        }
                    }
                )
            }

            // Setup Guidance Link
            val setupGuidanceUrl = stringResource(id = R.string.setup_guidance_url)
            Text(
                text = stringResource(id = R.string.setup_guidance),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(setupGuidanceUrl))
                        context.startActivity(intent)
                    }
                    .padding(vertical = 8.dp)
            )

            if (showLyricsConfirmationDialog) {
                AlertDialog(
                    onDismissRequest = { showLyricsConfirmationDialog = false },
                    title = { Text(stringResource(id = R.string.lyrics_enable_warning_title)) },
                    text = { Text(stringResource(id = R.string.lyrics_enable_warning_text)) },
                    confirmButton = {
                        TextButton(onClick = {
                            lyricsEnabled = true
                            SettingsManager.setLyricsEnabled(context, true)
                            showLyricsConfirmationDialog = false
                        }) {
                            Text(stringResource(id = R.string.lyrics_enable_confirm_button))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showLyricsConfirmationDialog = false
                            pendingEnableLyrics = false
                        }) {
                            Text(stringResource(id = R.string.cancel))
                        }
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

            // Lrc api Uri
            Text(text = stringResource(id = R.string.lrc_api_uri))
            OutlinedTextField(
                value = lrcApiUri,
                onValueChange = {
                    lrcApiUri = it
                    SettingsManager.setLrcApiBaseUri(context, it)
                },
                label = { Text(stringResource(id = R.string.lrc_api_uri)) },
                singleLine = true,
                visualTransformation = VisualTransformation.None,
                modifier = Modifier.fillMaxWidth()
            )

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
