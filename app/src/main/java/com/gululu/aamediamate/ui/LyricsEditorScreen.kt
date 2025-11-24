@file:OptIn(ExperimentalMaterial3Api::class)
package com.gululu.aamediamate.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.gululu.aamediamate.R
import com.gululu.aamediamate.lyrics.LyricsRepository
import kotlinx.coroutines.launch

@Composable
fun LyricsEditorScreen(
    lyricsKey: String,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    onManualSearch: (String) -> Unit
) {
    BackHandler {
        onBack()
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var content by remember { mutableStateOf("") }
    var isLoaded by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, lyricsKey) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                coroutineScope.launch {
                    val text = LyricsRepository.loadLyricsText(context, lyricsKey)
                    content = text
                    if (!isLoaded) isLoaded = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
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
                    TextButton(onClick = {
                        fun shift(contentText: String, deltaMs: Long): String {
                            val pattern = Regex("""\[(\d+):(\d+(?:\.\d+)?)]""")
                            fun format(totalSec: Float): String {
                                val clamped = if (totalSec < 0f) 0f else totalSec
                                val minutes = kotlin.math.floor((clamped / 60f).toDouble()).toInt()
                                val seconds = clamped - minutes * 60f
                                return String.format("[%02d:%05.2f]", minutes, seconds)
                            }
                            return contentText.lineSequence().joinToString("\n") { line ->
                                pattern.replace(line) { m ->
                                    val min = m.groupValues[1].toIntOrNull() ?: return@replace m.value
                                    val sec = m.groupValues[2].toFloatOrNull() ?: return@replace m.value
                                    val total = min * 60f + sec + (deltaMs / 1000f)
                                    format(total)
                                }
                            }
                        }
                        content = shift(content, -500)
                        Toast.makeText(
                            context,
                            context.getString(R.string.shifted_by_seconds, -0.5f, 1),
                            Toast.LENGTH_SHORT
                        ).show()
                    }) {
                        Text(text = stringResource(id = R.string.shift_backward_half))
                    }
                    TextButton(onClick = {
                        fun shift(contentText: String, deltaMs: Long): String {
                            val pattern = Regex("""\[(\d+):(\d+(?:\.\d+)?)]""")
                            fun format(totalSec: Float): String {
                                val clamped = if (totalSec < 0f) 0f else totalSec
                                val minutes = kotlin.math.floor((clamped / 60f).toDouble()).toInt()
                                val seconds = clamped - minutes * 60f
                                return String.format("[%02d:%05.2f]", minutes, seconds)
                            }
                            return contentText.lineSequence().joinToString("\n") { line ->
                                pattern.replace(line) { m ->
                                    val min = m.groupValues[1].toIntOrNull() ?: return@replace m.value
                                    val sec = m.groupValues[2].toFloatOrNull() ?: return@replace m.value
                                    val total = min * 60f + sec + (deltaMs / 1000f)
                                    format(total)
                                }
                            }
                        }
                        content = shift(content, 500)
                        Toast.makeText(
                            context,
                            context.getString(R.string.shifted_by_seconds, 0.5f, 1),
                            Toast.LENGTH_SHORT
                        ).show()
                    }) {
                        Text(text = stringResource(id = R.string.shift_forward_half))
                    }

                    IconButton(onClick = {
                        coroutineScope.launch {
                            LyricsRepository.saveLyricsText(context, lyricsKey, content)
                            Toast.makeText(context, context.getString(R.string.lyrics_saved), Toast.LENGTH_SHORT).show()
                            onBack()
                        }
                    }) {
                        Icon(Icons.Default.Done, contentDescription = stringResource(id = R.string.save))
                    }
                    IconButton(onClick = {
                        coroutineScope.launch {
                            LyricsRepository.deleteLyrics(context, listOf(lyricsKey))
                            Toast.makeText(context, context.getString(R.string.lyrics_deleted), Toast.LENGTH_SHORT).show()
                            onDeleted()
                        }
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.delete))
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.manual_search)) },
                            onClick = {
                                showMenu = false
                                onManualSearch(lyricsKey)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.do_not_show_lyrics)) },
                            onClick = {
                                showMenu = false
                                content = ""
                                coroutineScope.launch {
                                    LyricsRepository.saveLyricsText(context, lyricsKey, "")
                                    Toast.makeText(context, context.getString(R.string.lyrics_cleared), Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
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
