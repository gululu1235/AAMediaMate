@file:OptIn(ExperimentalMaterial3Api::class)

package com.gululu.aamediamate.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.gululu.aamediamate.R
import com.gululu.aamediamate.SettingsManager
import com.gululu.aamediamate.data.LyricsCleanupField
import com.gululu.aamediamate.data.LyricsCleanupRule
import java.util.UUID

@Composable
fun LyricsCleanupRulesScreen(
    onBack: () -> Unit,
) {
    BackHandler {
        onBack()
    }

    val context = LocalContext.current
    var rules by remember {
        mutableStateOf(SettingsManager.getLyricsCleanupRules(context).toMutableList())
    }
    var editorRule by remember { mutableStateOf<LyricsCleanupRule?>(null) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }

    fun persist(updatedRules: List<LyricsCleanupRule>) {
        rules = updatedRules.toMutableList()
        SettingsManager.saveLyricsCleanupRules(context, updatedRules)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.lyrics_cleanup_rules_title)) },
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
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.lyrics_cleanup_rules_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        editorRule = LyricsCleanupRule(
                            id = UUID.randomUUID().toString(),
                            name = "",
                            field = LyricsCleanupField.ARTIST,
                            pattern = "",
                            isEnabled = true
                        )
                        editingIndex = null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.lyrics_cleanup_rule_add))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.lyrics_cleanup_rules_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            itemsIndexed(
                items = rules,
                key = { _, rule -> rule.id }
            ) { index, rule ->
                CleanupRuleCard(
                    rule = rule,
                    canMoveUp = index > 0,
                    canMoveDown = index < rules.lastIndex,
                    onToggleEnabled = { enabled ->
                        val updated = rules.toMutableList()
                        updated[index] = updated[index].copy(isEnabled = enabled)
                        persist(updated)
                    },
                    onEdit = {
                        editorRule = rule.copy()
                        editingIndex = index
                    },
                    onDelete = {
                        val updated = rules.toMutableList()
                        updated.removeAt(index)
                        persist(updated)
                    },
                    onMoveUp = {
                        if (index > 0) {
                            val updated = rules.toMutableList()
                            updated[index] = updated[index - 1]
                            updated[index - 1] = rule
                            persist(updated)
                        }
                    },
                    onMoveDown = {
                        if (index < rules.lastIndex) {
                            val updated = rules.toMutableList()
                            updated[index] = updated[index + 1]
                            updated[index + 1] = rule
                            persist(updated)
                        }
                    }
                )
            }

            if (rules.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.lyrics_cleanup_rules_empty),
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }

    if (editorRule != null) {
        LyricsCleanupRuleEditorDialog(
            rule = editorRule!!,
            onDismiss = {
                editorRule = null
                editingIndex = null
            },
            onSave = { savedRule ->
                val updated = rules.toMutableList()
                val index = editingIndex
                if (index == null) {
                    updated.add(savedRule)
                } else {
                    updated[index] = savedRule
                }
                persist(updated)
                editorRule = null
                editingIndex = null
                Toast.makeText(context, context.getString(R.string.lyrics_cleanup_rule_saved), Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun CleanupRuleCard(
    rule: LyricsCleanupRule,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (rule.isEnabled) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = rule.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(
                            id = when (rule.field) {
                                LyricsCleanupField.ARTIST -> R.string.lyrics_cleanup_field_artist
                                LyricsCleanupField.TITLE -> R.string.lyrics_cleanup_field_title
                            }
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = rule.isEnabled,
                    onCheckedChange = onToggleEnabled
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = rule.pattern,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = canMoveUp
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = stringResource(R.string.lyrics_cleanup_rule_move_up))
                    }
                    IconButton(
                        onClick = onMoveDown,
                        enabled = canMoveDown
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = stringResource(R.string.lyrics_cleanup_rule_move_down))
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onEdit) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(stringResource(R.string.lyrics_cleanup_rule_edit))
                        }
                    }
                    TextButton(onClick = onDelete) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(stringResource(R.string.delete))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricsCleanupRuleEditorDialog(
    rule: LyricsCleanupRule,
    onDismiss: () -> Unit,
    onSave: (LyricsCleanupRule) -> Unit,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var name by remember(rule.id) { mutableStateOf(rule.name) }
    var pattern by remember(rule.id) { mutableStateOf(rule.pattern) }
    var field by remember(rule.id) { mutableStateOf(rule.field) }
    var enabled by remember(rule.id) { mutableStateOf(rule.isEnabled) }
    var fieldMenuExpanded by remember { mutableStateOf(false) }

    val regexError = remember(pattern) {
        runCatching { Regex(pattern) }.exceptionOrNull()?.message
    }
    val canSave = name.isNotBlank() && pattern.isNotBlank() && regexError == null
    val aiPrompt = stringResource(R.string.lyrics_cleanup_ai_prompt_text)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.lyrics_cleanup_rule_edit_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.lyrics_cleanup_rule_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = fieldMenuExpanded,
                    onExpandedChange = { fieldMenuExpanded = !fieldMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = stringResource(
                            id = when (field) {
                                LyricsCleanupField.ARTIST -> R.string.lyrics_cleanup_field_artist
                                LyricsCleanupField.TITLE -> R.string.lyrics_cleanup_field_title
                            }
                        ),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.lyrics_cleanup_rule_field)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = fieldMenuExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = fieldMenuExpanded,
                        onDismissRequest = { fieldMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.lyrics_cleanup_field_artist)) },
                            onClick = {
                                field = LyricsCleanupField.ARTIST
                                fieldMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.lyrics_cleanup_field_title)) },
                            onClick = {
                                field = LyricsCleanupField.TITLE
                                fieldMenuExpanded = false
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    label = { Text(stringResource(R.string.lyrics_cleanup_rule_pattern)) },
                    supportingText = {
                        Text(stringResource(R.string.lyrics_cleanup_rule_pattern_hint))
                    },
                    isError = regexError != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.lyrics_cleanup_ai_prompt_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(aiPrompt))
                            Toast.makeText(
                                context,
                                context.getString(R.string.lyrics_cleanup_ai_prompt_copied),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    ) {
                        Text(stringResource(R.string.lyrics_cleanup_ai_prompt_copy))
                    }
                }

                if (regexError != null) {
                    Text(
                        text = regexError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.lyrics_cleanup_rule_enabled))
                    Switch(
                        checked = enabled,
                        onCheckedChange = { enabled = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        rule.copy(
                            name = name.trim(),
                            field = field,
                            pattern = pattern.trim(),
                            isEnabled = enabled
                        )
                    )
                },
                enabled = canSave
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
