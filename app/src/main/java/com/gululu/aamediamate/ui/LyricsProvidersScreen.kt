package com.gululu.aamediamate.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.gululu.aamediamate.R
import com.gululu.aamediamate.SettingsManager
import com.gululu.aamediamate.data.LyricsProviderConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsProvidersScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var providers by remember { mutableStateOf(SettingsManager.getLyricsProviders(context)) }
    var apiKey by remember { mutableStateOf(SettingsManager.getApiKey(context)) }
    var lrcApiUri by remember { mutableStateOf(SettingsManager.getLrcApiBaseUri(context)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.lyrics_providers_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
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
                    text = stringResource(R.string.lyrics_providers_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(providers) { provider ->
                ProviderConfigCard(
                    provider = provider,
                    context = context,
                    apiKey = if (provider.id == "musixmatch") apiKey else "",
                    lrcApiUri = if (provider.id == "lrc_api") lrcApiUri else "",
                    onEnabledChange = { enabled ->
                        SettingsManager.updateProviderEnabled(context, provider.id, enabled)
                        providers = SettingsManager.getLyricsProviders(context)
                    },
                    onApiKeyChange = { newApiKey ->
                        if (provider.id == "musixmatch") {
                            apiKey = newApiKey
                            SettingsManager.setApiKey(context, newApiKey)
                        }
                    },
                    onLrcApiUriChange = { newUri ->
                        if (provider.id == "lrc_api") {
                            lrcApiUri = newUri
                            SettingsManager.setLrcApiBaseUri(context, newUri)
                        }
                    },
                    onPriorityUp = {
                        if (provider.priority > 1) {
                            val newPriority = provider.priority - 1
                            val otherProvider = providers.find { it.priority == newPriority }
                            if (otherProvider != null) {
                                SettingsManager.updateProviderPriority(context, otherProvider.id, provider.priority)
                            }
                            SettingsManager.updateProviderPriority(context, provider.id, newPriority)
                            providers = SettingsManager.getLyricsProviders(context)
                        }
                    },
                    onPriorityDown = {
                        val maxPriority = providers.maxOfOrNull { it.priority } ?: 1
                        if (provider.priority < maxPriority) {
                            val newPriority = provider.priority + 1
                            val otherProvider = providers.find { it.priority == newPriority }
                            if (otherProvider != null) {
                                SettingsManager.updateProviderPriority(context, otherProvider.id, provider.priority)
                            }
                            SettingsManager.updateProviderPriority(context, provider.id, newPriority)
                            providers = SettingsManager.getLyricsProviders(context)
                        }
                    }
                )
            }
            
            if (providers.none { it.isEnabled }) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.no_providers_enabled_warning),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            
            // Setup Guidance Link
            item {
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
                        .padding(vertical = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun ProviderConfigCard(
    provider: LyricsProviderConfig,
    context: android.content.Context,
    apiKey: String,
    lrcApiUri: String,
    onEnabledChange: (Boolean) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onLrcApiUriChange: (String) -> Unit,
    onPriorityUp: () -> Unit,
    onPriorityDown: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (provider.isEnabled) 
                MaterialTheme.colorScheme.surface 
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = provider.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = provider.getDescription(context),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Switch(
                    checked = provider.isEnabled,
                    onCheckedChange = onEnabledChange
                )
            }
            
            if (provider.isEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Priority controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.priority_format, provider.priority),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row {
                        OutlinedButton(
                            onClick = onPriorityUp,
                            modifier = Modifier.size(36.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("↑", style = MaterialTheme.typography.bodyLarge)
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        OutlinedButton(
                            onClick = onPriorityDown,
                            modifier = Modifier.size(36.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("↓", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                
                // Provider-specific configuration
                when (provider.id) {
                    "musixmatch" -> {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = onApiKeyChange,
                            label = { Text(stringResource(R.string.musixmatch_api_key)) },
                            singleLine = true,
                            visualTransformation = VisualTransformation.None,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    "lrc_api" -> {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = lrcApiUri,
                            onValueChange = onLrcApiUriChange,
                            label = { Text(stringResource(R.string.lrc_api_uri)) },
                            singleLine = true,
                            visualTransformation = VisualTransformation.None,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}