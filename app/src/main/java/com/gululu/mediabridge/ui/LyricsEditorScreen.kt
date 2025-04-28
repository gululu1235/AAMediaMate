@file:OptIn(ExperimentalMaterial3Api::class)
package com.gululu.mediabridge.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gululu.mediabridge.R
import com.gululu.mediabridge.lyrics.LyricsRepository
import kotlinx.coroutines.launch

@Composable
fun LyricsEditorScreen(
    lyricsKey: String,
    onBack: () -> Unit,
    onDeleted: () -> Unit
) {
    BackHandler {
        onBack()
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var content by remember { mutableStateOf("") }
    var isLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val text = LyricsRepository.loadLyricsText(context, lyricsKey)
        content = text
        isLoaded = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.edit_lyrics)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            LyricsRepository.saveLyricsText(context, lyricsKey, content)
                            onBack()
                        }
                    }) {
                        Icon(Icons.Default.Done, contentDescription = stringResource(id = R.string.save))
                    }
                    IconButton(onClick = {
                        coroutineScope.launch {
                            LyricsRepository.deleteLyrics(context, listOf(lyricsKey))
                            onDeleted()
                        }
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.delete))
                    }
                }
            )
        }
    ) { innerPadding ->
        if (isLoaded) {
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                singleLine = false,
                label = { Text(text = stringResource(id = R.string.lyrics_content_label)) }
            )
        }
    }
}
