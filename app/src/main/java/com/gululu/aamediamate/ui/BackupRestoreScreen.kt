@file:OptIn(ExperimentalMaterial3Api::class)

package com.gululu.aamediamate.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import com.gululu.aamediamate.backup.BackupManager
import com.gululu.aamediamate.backup.BackupPreview
import kotlinx.coroutines.launch

@Composable
fun BackupRestoreScreen(
    onBack: () -> Unit
) {
    BackHandler {
        onBack()
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var includeLyrics by remember { mutableStateOf(true) }
    var includeSettings by remember { mutableStateOf(true) }
    var includeSecrets by remember { mutableStateOf(false) }
    var pendingCreateOptions by remember { mutableStateOf<BackupCreateOptions?>(null) }

    var backupPreview by remember { mutableStateOf<BackupPreview?>(null) }
    var restoreUri by remember { mutableStateOf<Uri?>(null) }
    var restoreLyrics by remember { mutableStateOf(false) }
    var restoreSettings by remember { mutableStateOf(false) }

    var isBusy by remember { mutableStateOf(false) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var showRestartDialog by remember { mutableStateOf(false) }

    val createBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        val options = pendingCreateOptions
        pendingCreateOptions = null
        if (uri == null || options == null) return@rememberLauncherForActivityResult

        coroutineScope.launch {
            isBusy = true
            runCatching {
                BackupManager.createBackup(
                    context = context,
                    outputUri = uri,
                    includeLyrics = options.includeLyrics,
                    includeSettings = options.includeSettings,
                    includeSecrets = options.includeSecrets
                )
            }.onSuccess { result ->
                val settingsSuffix = if (result.includesSettings) {
                    context.getString(R.string.backup_settings_suffix)
                } else {
                    ""
                }
                Toast.makeText(
                    context,
                    context.getString(R.string.backup_created, result.lyricsFileCount, settingsSuffix),
                    Toast.LENGTH_LONG
                ).show()
            }.onFailure { error ->
                Toast.makeText(
                    context,
                    context.getString(R.string.backup_operation_failed, error.localizedMessage ?: ""),
                    Toast.LENGTH_LONG
                ).show()
            }
            isBusy = false
        }
    }

    val openBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        coroutineScope.launch {
            isBusy = true
            runCatching {
                BackupManager.readBackupPreview(context, uri)
            }.onSuccess { preview ->
                restoreUri = uri
                backupPreview = preview
                restoreLyrics = preview.includesLyrics
                restoreSettings = preview.includesSettings
            }.onFailure {
                restoreUri = null
                backupPreview = null
                Toast.makeText(context, context.getString(R.string.backup_invalid_file), Toast.LENGTH_LONG).show()
            }
            isBusy = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.backup_restore_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isBusy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Text(
                text = stringResource(R.string.backup_create_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.backup_select_content_create),
                style = MaterialTheme.typography.bodyMedium
            )
            BackupCheckboxRow(
                text = stringResource(R.string.backup_include_lyrics),
                checked = includeLyrics,
                enabled = !isBusy,
                onCheckedChange = { includeLyrics = it }
            )
            BackupCheckboxRow(
                text = stringResource(R.string.backup_include_config),
                checked = includeSettings,
                enabled = !isBusy,
                onCheckedChange = {
                    includeSettings = it
                    if (!it) includeSecrets = false
                }
            )
            BackupCheckboxRow(
                text = stringResource(R.string.backup_include_keys),
                description = stringResource(R.string.backup_include_keys_description),
                checked = includeSecrets,
                enabled = includeSettings && !isBusy,
                onCheckedChange = { includeSecrets = it }
            )
            Button(
                onClick = {
                    if (!includeLyrics && !includeSettings) {
                        Toast.makeText(context, context.getString(R.string.backup_no_content_selected), Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    pendingCreateOptions = BackupCreateOptions(
                        includeLyrics = includeLyrics,
                        includeSettings = includeSettings,
                        includeSecrets = includeSettings && includeSecrets
                    )
                    createBackupLauncher.launch(BackupManager.createDefaultBackupFileName())
                },
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.backup_create))
            }

            HorizontalDivider()

            Text(
                text = stringResource(R.string.backup_restore_section_title),
                style = MaterialTheme.typography.titleMedium
            )
            Button(
                onClick = {
                    openBackupLauncher.launch(
                        arrayOf(
                            "application/zip",
                            "application/octet-stream",
                            "application/x-zip-compressed"
                        )
                    )
                },
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.backup_choose_restore_file))
            }

            backupPreview?.let { preview ->
                BackupPreviewContent(
                    preview = preview,
                    restoreLyrics = restoreLyrics,
                    restoreSettings = restoreSettings,
                    enabled = !isBusy,
                    onRestoreLyricsChange = { restoreLyrics = it },
                    onRestoreSettingsChange = { restoreSettings = it }
                )

                Button(
                    onClick = {
                        if (!restoreLyrics && !restoreSettings) {
                            Toast.makeText(context, context.getString(R.string.backup_no_content_selected), Toast.LENGTH_SHORT).show()
                        } else {
                            showRestoreConfirmDialog = true
                        }
                    },
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.backup_restore))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            title = { Text(stringResource(R.string.backup_restore_confirm_title)) },
            text = { Text(stringResource(R.string.backup_restore_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val uri = restoreUri
                        showRestoreConfirmDialog = false
                        if (uri == null) return@TextButton

                        coroutineScope.launch {
                            isBusy = true
                            runCatching {
                                BackupManager.restoreBackup(
                                    context = context,
                                    inputUri = uri,
                                    restoreLyrics = restoreLyrics,
                                    restoreSettings = restoreSettings
                                )
                            }.onSuccess { result ->
                                val settingsSuffix = if (result.restoredSettings) {
                                    context.getString(R.string.backup_settings_suffix)
                                } else {
                                    ""
                                }
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.backup_restored, result.restoredLyricsCount, settingsSuffix),
                                    Toast.LENGTH_LONG
                                ).show()
                                if (result.restoredSettings) {
                                    showRestartDialog = true
                                }
                            }.onFailure { error ->
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.backup_operation_failed, error.localizedMessage ?: ""),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            isBusy = false
                        }
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { showRestartDialog = false },
            title = { Text(stringResource(R.string.backup_restart_required_title)) },
            text = { Text(stringResource(R.string.backup_restart_required_message)) },
            confirmButton = {
                TextButton(onClick = { restartApp(context) }) {
                    Text(stringResource(R.string.backup_restart_app))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestartDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun BackupPreviewContent(
    preview: BackupPreview,
    restoreLyrics: Boolean,
    restoreSettings: Boolean,
    enabled: Boolean,
    onRestoreLyricsChange: (Boolean) -> Unit,
    onRestoreSettingsChange: (Boolean) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.backup_selected_file_summary, preview.lyricsFileCount),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = if (preview.includesSettings) {
                stringResource(R.string.backup_config_included)
            } else {
                stringResource(R.string.backup_config_not_included)
            },
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = if (preview.includesSecrets) {
                stringResource(R.string.backup_keys_included)
            } else {
                stringResource(R.string.backup_keys_not_included)
            },
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = stringResource(R.string.backup_select_content_restore),
            style = MaterialTheme.typography.titleSmall
        )
        BackupCheckboxRow(
            text = stringResource(R.string.backup_include_lyrics),
            checked = restoreLyrics,
            enabled = enabled && preview.includesLyrics,
            onCheckedChange = onRestoreLyricsChange
        )
        BackupCheckboxRow(
            text = stringResource(R.string.backup_include_config),
            checked = restoreSettings,
            enabled = enabled && preview.includesSettings,
            onCheckedChange = onRestoreSettingsChange
        )
    }
}

@Composable
private fun BackupCheckboxRow(
    text: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = text, style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private data class BackupCreateOptions(
    val includeLyrics: Boolean,
    val includeSettings: Boolean,
    val includeSecrets: Boolean
)

private fun restartApp(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    context.startActivity(intent)
    Runtime.getRuntime().exit(0)
}
