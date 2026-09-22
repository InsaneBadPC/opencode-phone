package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.AiModel
import com.example.data.models.AiProvider
import com.example.ui.OpenCodeViewModel
import com.example.ui.theme.*

@Composable
fun AiProvidersSettingsView(
    viewModel: OpenCodeViewModel,
    modifier: Modifier = Modifier
) {
    val providers by viewModel.allAiProviders.collectAsState()
    val context = LocalContext.current

    var filterCategory by remember { mutableStateOf("all") } // "all", "ready", "needs_key", "no_key_needed"
    var successToastProviderId by remember { mutableStateOf<String?>(null) }

    val readyCount = providers.count { it.isAvailable }
    val totalModels = providers.sumOf { it.models.size }
    val availableModelsCount = providers.filter { it.isAvailable }.sumOf { it.models.size }

    val filteredProviders = remember(providers, filterCategory) {
        when (filterCategory) {
            "ready" -> providers.filter { it.isAvailable }
            "needs_key" -> providers.filter { it.requiresKey && it.apiKey.isBlank() }
            "no_key_needed" -> providers.filter { !it.requiresKey }
            else -> providers
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("ai_providers_settings_view"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate800, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(CyanAccent.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Hub,
                                    contentDescription = null,
                                    tint = CyanBright,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "AI Provideři & API Klíče",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate100
                                )
                                Text(
                                    text = "Všichni provideři s bezplatnými modely & free tiery",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate400
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Nakonfigurujte si přístup k poskytovatelům AI modelů. API klíče jsou bezpečně uloženy v hardware-backed Android KeyStore pomocí šifrování AES-256-GCM a nikdy neopouštějí zařízení. V seznamu pro výběr v chatu se zobrazují pouze aktivní a dostupné modely.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate300,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        color = Slate950,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Zabezpečení: ${viewModel.getStorageSecurityInfo()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate200,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Stats Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = Slate950,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Aktivní provideři", style = MaterialTheme.typography.labelSmall, color = Slate400)
                                Text("$readyCount z ${providers.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                            }
                        }
                        Surface(
                            color = Slate950,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Dostupné modely", style = MaterialTheme.typography.labelSmall, color = Slate400)
                                Text("$availableModelsCount z $totalModels", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CyanBright)
                            }
                        }
                    }
                }
            }
        }

        // Filter chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterCategory == "all",
                    onClick = { filterCategory = "all" },
                    label = { Text("Vše (${providers.size})", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanBright,
                        selectedLabelColor = Slate950,
                        containerColor = Slate900,
                        labelColor = Slate300
                    )
                )
                FilterChip(
                    selected = filterCategory == "ready",
                    onClick = { filterCategory = "ready" },
                    label = { Text("Připraveno ($readyCount)", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldSuccess,
                        selectedLabelColor = Slate950,
                        containerColor = Slate900,
                        labelColor = Slate300
                    )
                )
                FilterChip(
                    selected = filterCategory == "needs_key",
                    onClick = { filterCategory = "needs_key" },
                    label = { Text("Chybí klíč (${providers.size - readyCount})", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AmberWarning,
                        selectedLabelColor = Slate950,
                        containerColor = Slate900,
                        labelColor = Slate300
                    )
                )
            }
        }

        // Provider items
        items(filteredProviders, key = { it.id }) { provider ->
            ProviderConfigurationCard(
                provider = provider,
                maskedKey = viewModel.getMaskedApiKey(provider.id),
                onSaveApiKey = { key ->
                    viewModel.updateProviderApiKey(provider.id, key)
                    successToastProviderId = provider.id
                },
                onSaveCustomUrl = { url ->
                    viewModel.updateProviderBaseUrl(provider.id, url)
                },
                onToggleEnabled = { enabled ->
                    viewModel.toggleProviderEnabled(provider.id, enabled)
                },
                onReset = {
                    viewModel.resetProviderSettings(provider.id)
                },
                onOpenKeyWebsite = { url ->
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Ignore
                    }
                },
                showSavedConfirmation = successToastProviderId == provider.id
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderConfigurationCard(
    provider: AiProvider,
    maskedKey: String = "",
    onSaveApiKey: (String) -> Unit,
    onSaveCustomUrl: (String) -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onReset: () -> Unit,
    onOpenKeyWebsite: (String) -> Unit,
    showSavedConfirmation: Boolean
) {
    var isExpanded by remember { mutableStateOf(false) }
    var apiKeyInput by remember(provider.apiKey) { mutableStateOf(provider.apiKey) }
    var customUrlInput by remember(provider.customBaseUrl) { mutableStateOf(provider.customBaseUrl) }
    var isKeyObscured by remember { mutableStateOf(true) }
    var showModelsList by remember { mutableStateOf(false) }

    val statusColor = if (provider.isAvailable) EmeraldSuccess else AmberWarning
    val borderColor = if (provider.isAvailable) Slate800 else AmberWarning.copy(alpha = 0.5f)

    Card(
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .testTag("provider_card_${provider.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = provider.iconEmoji, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = provider.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate100
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Badge(containerColor = Slate800) {
                                Text(
                                    text = provider.badge,
                                    color = EmeraldSuccess,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (provider.isAvailable) "Dostupný v chatu" else "Vyžaduje API klíč",
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "• ${provider.models.size} modelů",
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate400
                            )
                        }
                    }
                }

                Switch(
                    checked = provider.isEnabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Slate950,
                        checkedTrackColor = CyanBright,
                        uncheckedThumbColor = Slate400,
                        uncheckedTrackColor = Slate800
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Provider description
            Text(
                text = provider.description,
                style = MaterialTheme.typography.bodySmall,
                color = Slate300,
                lineHeight = 16.sp
            )

            // Free tier note callout
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = Slate950,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Savings,
                        contentDescription = null,
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Free Tier: ${provider.freeTierDetails}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate200,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Expand/Collapse Details button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) "Skrýt konfiguraci klíče" else "Nastavit API klíč & parametry",
                    style = MaterialTheme.typography.labelMedium,
                    color = CyanBright,
                    fontWeight = FontWeight.SemiBold
                )

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = CyanBright
                )
            }

            // Expanded Settings Section
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    // API Key Field
                    if (provider.requiresKey) {
                        Text(
                            text = "API Klíč",
                            style = MaterialTheme.typography.labelMedium,
                            color = Slate300,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Masked Key status if saved
                        if (maskedKey.isNotBlank() && maskedKey != "Žádný klíč") {
                            Surface(
                                color = Slate950,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Shield, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "KeyStore: $maskedKey",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EmeraldSuccess,
                                            fontSize = 11.sp
                                        )
                                    }

                                    TextButton(
                                        onClick = {
                                            apiKeyInput = ""
                                            onReset()
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                        modifier = Modifier.height(24.dp)
                                    ) {
                                        Text("Smazat", color = RoseError, fontSize = 10.sp)
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = { apiKeyInput = it },
                            placeholder = {
                                Text(
                                    text = if (provider.id == "groq") "gsk_..."
                                    else if (provider.id == "openrouter") "sk-or-v1-..."
                                    else if (provider.id == "github_models") "ghp_... nebo github_pat_..."
                                    else "Vložte nový API klíč",
                                    color = Slate500,
                                    fontSize = 12.sp
                                )
                            },
                            visualTransformation = if (isKeyObscured) PasswordVisualTransformation() else VisualTransformation.None,
                            trailingIcon = {
                                IconButton(onClick = { isKeyObscured = !isKeyObscured }) {
                                    Icon(
                                        imageVector = if (isKeyObscured) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Zobrazit klíč",
                                        tint = Slate400,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Slate950,
                                unfocusedContainerColor = Slate950,
                                focusedBorderColor = CyanBright,
                                unfocusedBorderColor = Slate800,
                                focusedTextColor = Slate100,
                                unfocusedTextColor = Slate200
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("api_key_input_${provider.id}")
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Link to get API Key
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { onOpenKeyWebsite(provider.apiKeyUrl) },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = CyanBright
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Získat bezplatný API klíč", fontSize = 11.sp, color = CyanBright)
                            }

                            Button(
                                onClick = { onSaveApiKey(apiKeyInput) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyanBright,
                                    contentColor = Slate950
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Uložit do KeyStore", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Custom URL for Ollama or self-hosted
                    val allowsCustomUrl = provider.id == "ollama" || provider.id == "ollama_local" || provider.defaultBaseUrl.contains("localhost") || provider.id.contains("custom")
                    if (allowsCustomUrl) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Vlastní Base URL (volitelné)",
                            style = MaterialTheme.typography.labelMedium,
                            color = Slate300,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        OutlinedTextField(
                            value = customUrlInput,
                            onValueChange = { customUrlInput = it },
                            placeholder = { Text(provider.defaultBaseUrl, color = Slate500, fontSize = 12.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Slate950,
                                unfocusedContainerColor = Slate950,
                                focusedBorderColor = CyanBright,
                                unfocusedBorderColor = Slate800,
                                focusedTextColor = Slate100,
                                unfocusedTextColor = Slate200
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = { onSaveCustomUrl(customUrlInput) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Uložit URL", fontSize = 11.sp)
                            }
                        }
                    }

                    // Provider Models Accordion
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = Slate950,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showModelsList = !showModelsList }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FormatListBulleted,
                                    contentDescription = null,
                                    tint = Slate400,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Dostupné modely tohoto providera (${provider.models.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Slate200
                                )
                            }
                            Icon(
                                imageVector = if (showModelsList) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = Slate400,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (showModelsList) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            provider.models.forEach { model ->
                                Surface(
                                    color = Slate950,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = model.displayName,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Slate100
                                            )
                                            Badge(containerColor = Slate800) {
                                                Text(model.tag, fontSize = 9.sp, color = CyanBright)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = model.description,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Slate400,
                                            fontSize = 10.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Limit: ${model.freeTierNote} · Kontext: ${model.contextWindow}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EmeraldSuccess,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Reset button
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onReset,
                            colors = ButtonDefaults.textButtonColors(contentColor = Slate400)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Resetovat nastavení providera", fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}
