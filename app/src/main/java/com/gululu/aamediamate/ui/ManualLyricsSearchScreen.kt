@file:OptIn(ExperimentalMaterial3Api::class)

package com.gululu.aamediamate.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gululu.aamediamate.R
import com.gululu.aamediamate.SettingsManager
import com.gululu.aamediamate.data.LyricsProviderConfig
import com.gululu.aamediamate.lyrics.LyricsRepository
import kotlinx.coroutines.launch

@Composable
fun ManualLyricsSearchScreen(
    lyricsKey: String,
    onBack: () -> Unit,
) {
    BackHandler {
        onBack()
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val (initialTitle, initialArtist) = remember(lyricsKey) {
        lyricsKey.split("_", limit = 2).let {
            if (it.size == 2) it[0] to it[1] else it[0] to ""
        }
    }

    var title by remember { mutableStateOf(initialTitle) }
    var artist by remember { mutableStateOf(initialArtist) }
    var searchResult by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    var providers by remember { mutableStateOf<List<LyricsProviderConfig>>(emptyList()) }
    var selectedProvider by remember { mutableStateOf<LyricsProviderConfig?>(null) }
    var isProvidersDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        providers = SettingsManager.getEnabledProvidersInOrder(context)
        selectedProvider = providers.firstOrNull()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.manual_search_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back))
                    }
                },
                actions = {
                    if (searchResult != null) {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                LyricsRepository.saveLyricsText(context, lyricsKey, searchResult!!)
                                Toast.makeText(context, context.getString(R.string.lyrics_saved), Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                        }) {
                            Icon(Icons.Default.Done, contentDescription = stringResource(id = R.string.save))
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(id = R.string.song_title)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = artist,
                onValueChange = { artist = it },
                label = { Text(stringResource(id = R.string.artist)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            ExposedDropdownMenuBox(
                expanded = isProvidersDropdownExpanded,
                onExpandedChange = { isProvidersDropdownExpanded = !isProvidersDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = selectedProvider?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(id = R.string.provider)) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = isProvidersDropdownExpanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = isProvidersDropdownExpanded,
                    onDismissRequest = { isProvidersDropdownExpanded = false }
                ) {
                    providers.forEach { provider ->
                        DropdownMenuItem(
                            text = { Text(provider.name) },
                            onClick = {
                                selectedProvider = provider
                                isProvidersDropdownExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (selectedProvider != null) {
                        coroutineScope.launch {
                            isLoading = true
                            message = null
                            searchResult = null
                            val result = selectedProvider!!.provider.getLyricsLrc(context, title, artist, "")
                            if (result.isNullOrBlank()) {
                                message = context.getString(R.string.lyrics_not_found)
                            } else {
                                searchResult = result
                            }
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading && selectedProvider != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(id = R.string.search_lyrics))
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            message?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            searchResult?.let {
                OutlinedTextField(
                    value = it,
                    onValueChange = { searchResult = it },
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(top = 16.dp),
                    singleLine = false,
                    label = { Text(text = stringResource(id = R.string.lyrics_content_label)) }
                )
            }
        }
    }
}
