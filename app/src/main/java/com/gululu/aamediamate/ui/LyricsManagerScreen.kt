@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.gululu.aamediamate.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gululu.aamediamate.R
import com.gululu.aamediamate.lyrics.LyricsRepository
import com.gululu.aamediamate.models.LyricsEntry
import kotlinx.coroutines.launch

enum class LyricsFilterType {
    All, HasLyrics, NoLyrics
}

@Composable
fun LyricsManagerScreen(
    onBack: () -> Unit,
    onOpenEditor: (String) -> Unit,
    onNavigateToBridgedApps: () -> Unit = {}
) {
    BackHandler {
        onBack()
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var fullLyricsList by remember { mutableStateOf<List<LyricsEntry>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf(LyricsFilterType.All) }
    var filterExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    var selectedKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    val isSelectionMode = selectedKeys.isNotEmpty()

    LaunchedEffect(Unit) {
        fullLyricsList = LyricsRepository.getAllLyrics(context)
    }

    val filteredList = remember(fullLyricsList, searchQuery, filterType) {
        fullLyricsList
            .filter { entry ->
                when (filterType) {
                    LyricsFilterType.All -> true
                    LyricsFilterType.HasLyrics -> entry.hasLyrics
                    LyricsFilterType.NoLyrics -> !entry.hasLyrics
                }
            }
            .filter { entry ->
                val keyword = searchQuery.trim().lowercase()
                keyword.isBlank() ||
                        entry.title.lowercase().contains(keyword) ||
                        entry.artist.lowercase().contains(keyword)
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSelectionMode) {
                        Text(stringResource(id = R.string.lyrics_manager_top_selected, selectedKeys.size))
                    } else {
                        Text(stringResource(id = R.string.lyrics_manager))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSelectionMode) {
                            selectedKeys = emptySet()
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        IconButton(onClick = {
                            showDeleteDialog = true
                        }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(id = R.string.delete)
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text(stringResource(id = R.string.search)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box {
                    OutlinedButton(onClick = { filterExpanded = true }) {
                        Text(
                            when (filterType) {
                                LyricsFilterType.All -> stringResource(id = R.string.filter_all)
                                LyricsFilterType.HasLyrics -> stringResource(id = R.string.has_lyrics)
                                LyricsFilterType.NoLyrics -> stringResource(id = R.string.no_lyrics)
                            }
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }

                    DropdownMenu(
                        expanded = filterExpanded,
                        onDismissRequest = { filterExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.filter_all)) },
                            onClick = {
                                filterType = LyricsFilterType.All
                                filterExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.has_lyrics)) },
                            onClick = {
                                filterType = LyricsFilterType.HasLyrics
                                filterExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.no_lyrics)) },
                            onClick = {
                                filterType = LyricsFilterType.NoLyrics
                                filterExpanded = false
                            }
                        )
                    }
                }
            }

            // Bridged Apps Button - only show when not in selection mode
            if (!isSelectionMode) {
                OutlinedButton(
                    onClick = onNavigateToBridgedApps,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp)
                ) {
                    Text(stringResource(id = R.string.bridged_apps_title))
                }
            }

            // list of lyrics
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxSize()
            ) {
                items(filteredList) { entry ->
                    LyricsListItem(
                        entry = entry,
                        isSelected = selectedKeys.contains(entry.key),
                        onClick = {
                            if (isSelectionMode) {
                                selectedKeys = if (selectedKeys.contains(entry.key)) {
                                    selectedKeys - entry.key
                                } else {
                                    selectedKeys + entry.key
                                }
                            } else {
                                onOpenEditor(entry.key)
                            }
                        },
                        onLongClick = {
                            selectedKeys = selectedKeys + entry.key
                        }
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(text = stringResource(id = R.string.confirm_delete_title)) },
            text = { Text(text = stringResource(id = R.string.confirm_delete_message, selectedKeys.size)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        coroutineScope.launch {
                            LyricsRepository.deleteLyrics(context, selectedKeys.toList())
                            fullLyricsList = LyricsRepository.getAllLyrics(context)
                            Toast.makeText(context, context.getString(R.string.deleted_message, selectedKeys.size), Toast.LENGTH_SHORT).show()
                            selectedKeys = emptySet()
                        }
                    }
                ) {
                    Text(text = stringResource(id = R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun LyricsListItem(
    entry: LyricsEntry,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (entry.hasLyrics) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint = if (entry.hasLyrics) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = entry.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(id = R.string.item_selected),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
