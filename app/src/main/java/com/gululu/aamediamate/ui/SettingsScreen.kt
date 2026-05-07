@file:OptIn(ExperimentalMaterial3Api::class)

package com.gululu.aamediamate.ui

import android.app.LocaleManager
import android.content.Intent
import android.os.Build
import android.os.LocaleList
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gululu.aamediamate.MainActivity
import com.gululu.aamediamate.R
import com.gululu.aamediamate.SettingsManager
import com.gululu.aamediamate.models.LanguageOption

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToLyricsSettings: () -> Unit = {},
    onNavigateToBridgedApps: () -> Unit = {},
    onNavigateToDisplaySettings: () -> Unit = {}
) {
    BackHandler {
        onBack()
    }

    val context = LocalContext.current

    var ignoreNativeAutoApps by remember { mutableStateOf(SettingsManager.getIgnoreNativeAutoApps(context)) }

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
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
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

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val localeManager = context.getSystemService(LocaleManager::class.java)
                                    localeManager.applicationLocales = if (option.language.isEmpty()) {
                                        LocaleList.getEmptyLocaleList()
                                    } else {
                                        LocaleList.forLanguageTags("${option.language}-${option.country}")
                                    }
                                    // System handles activity restart automatically
                                } else {
                                    val intent = Intent(context, MainActivity::class.java)
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                    context.startActivity(intent)
                                    Runtime.getRuntime().exit(0)
                                }
                            }
                        )
                    }
                }
            }

            // Ignore native Android Auto apps
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

            // Display Settings Button
            OutlinedButton(
                onClick = onNavigateToDisplaySettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.display_settings_title))
            }

            // Customizations per App Button (Bridged Apps)
            OutlinedButton(
                onClick = onNavigateToBridgedApps,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.customizations_per_app_title))
            }

            OutlinedButton(
                onClick = onNavigateToLyricsSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.lyrics_settings_title))
            }

        }
    }
}
