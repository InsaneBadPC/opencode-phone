package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.models.AiModel
import com.example.data.models.AiProvider
import com.example.ui.theme.*

/**
 * Filter categories for categorizing AI models by provider,
 * explicitly supporting: Free tier, Groq, OpenRouter, GitHub Models, Gemini, Ollama, All, and More.
 */
enum class ModelCategory(val displayName: String, val iconEmoji: String) {
    ALL("Všechny", "🌟"),
    FREE_TIER("Free tier", "🎁"),
    GEMINI("Gemini", "✨"),
    GROQ("Groq", "⚡"),
    OPENROUTER("OpenRouter", "🌐"),
    GITHUB_MODELS("GitHub Models", "🐙"),
    OLLAMA("Ollama", "🦙"),
    MORE("Další provideři", "➕")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiModelPickerDialog(
    selectedModel: AiModel,
    availableModelsByProvider: Map<AiProvider, List<AiModel>>,
    allProviders: List<AiProvider>,
    onModelSelected: (AiModel) -> Unit,
    onSaveApiKey: ((providerId: String, apiKey: String) -> Unit)? = null,
    onUpdateBaseUrl: ((providerId: String, url: String) -> Unit)? = null,
    onOpenProviderSettings: () -> Unit,
    onDismiss: () -> Unit,
    securityInfo: String = "Hardware-backed Android KeyStore (AES-256-GCM)"
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ModelCategory.ALL) }
    var capabilityFilter by remember { mutableStateOf("all") } // "all", "code", "fast", "reasoning"

    // State for in-dialog secure key entry sheet
    var activeConfiguringProvider by remember { mutableStateOf<AiProvider?>(null) }
    var keyInputValue by remember { mutableStateOf("") }
    var urlInputValue by remember { mutableStateOf("") }
    var isKeyVisible by remember { mutableStateOf(false) }
    var keySaveSuccessMessage by remember { mutableStateOf<String?>(null) }

    val totalAllModels = remember(allProviders) { allProviders.sumOf { it.models.size } }
    val readyProvidersCount = remember(allProviders) { allProviders.count { it.isAvailable } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .testTag("ai_model_picker_dialog")
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(CyanAccent.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "🤖", fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Výběr AI Modelu",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate100
                                )
                                Text(
                                    text = "Kategorizováno podle providerů · $readyProvidersCount z ${allProviders.size} aktivních",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate400
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Zavřít",
                                tint = Slate400
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Security Storage Badge Banner
                    Surface(
                        color = Slate950,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Šifrování: $securityInfo",
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate300,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Provider Category Tabs (Free tier, Groq, OpenRouter, GitHub Models, Gemini, Ollama, All, More)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ModelCategory.values().forEach { category ->
                            val isSelected = selectedCategory == category
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = category },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = category.iconEmoji, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = category.displayName,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 12.sp
                                        )
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (category == ModelCategory.FREE_TIER) EmeraldSuccess.copy(alpha = 0.25f) else CyanBright.copy(alpha = 0.2f),
                                    selectedLabelColor = if (category == ModelCategory.FREE_TIER) EmeraldSuccess else CyanBright,
                                    containerColor = Slate950,
                                    labelColor = Slate300
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = Slate800,
                                    selectedBorderColor = if (category == ModelCategory.FREE_TIER) EmeraldSuccess else CyanBright
                                ),
                                modifier = Modifier.testTag("category_chip_${category.name.lowercase()}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Search and Capability Quick-Filters
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Hledat model, providera...", color = Slate500, fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = CyanBright,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Vymazat",
                                            tint = Slate400,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
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
                                .weight(1f)
                                .height(48.dp)
                                .testTag("model_search_input")
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Capability filter dropdown / button
                        Surface(
                            color = Slate950,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                            modifier = Modifier
                                .clickable {
                                    capabilityFilter = when (capabilityFilter) {
                                        "all" -> "code"
                                        "code" -> "reasoning"
                                        "reasoning" -> "fast"
                                        else -> "all"
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = null,
                                    tint = CyanBright,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = when (capabilityFilter) {
                                        "code" -> "💻 Kód"
                                        "reasoning" -> "🧠 Uvažování"
                                        "fast" -> "⚡ Rychlé"
                                        else -> "Filtrovat"
                                    },
                                    color = if (capabilityFilter == "all") Slate300 else CyanBright,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Grouped List of Providers & Models based on Category & Search
                    val providersToDisplay = remember(allProviders, selectedCategory, searchQuery, capabilityFilter) {
                        val baseList = when (selectedCategory) {
                            ModelCategory.ALL -> allProviders
                            ModelCategory.FREE_TIER -> allProviders.filter { p ->
                                p.badge.contains("Free", ignoreCase = true) || !p.requiresKey || p.models.any { it.isFree }
                            }
                            ModelCategory.GEMINI -> allProviders.filter { it.id == "google_gemini" }
                            ModelCategory.GROQ -> allProviders.filter { it.id == "groq" }
                            ModelCategory.OPENROUTER -> allProviders.filter { it.id == "openrouter" }
                            ModelCategory.GITHUB_MODELS -> allProviders.filter { it.id == "github_models" }
                            ModelCategory.OLLAMA -> allProviders.filter { it.id == "ollama_local" }
                            ModelCategory.MORE -> allProviders.filter {
                                it.id !in listOf("google_gemini", "groq", "openrouter", "github_models", "ollama_local")
                            }
                        }

                        baseList.filter { provider ->
                            val matchesProvider = provider.name.contains(searchQuery, ignoreCase = true)
                            val hasMatchingModels = provider.models.any { model ->
                                val matchesSearch = searchQuery.isBlank() ||
                                        model.displayName.contains(searchQuery, ignoreCase = true) ||
                                        model.description.contains(searchQuery, ignoreCase = true) ||
                                        model.tag.contains(searchQuery, ignoreCase = true)

                                val matchesCapability = when (capabilityFilter) {
                                    "code" -> model.tag.contains("Code", ignoreCase = true) || model.tag.contains("Coder", ignoreCase = true)
                                    "reasoning" -> model.isReasoning || model.tag.contains("Reasoning", ignoreCase = true) || model.tag.contains("Thinking", ignoreCase = true)
                                    "fast" -> model.tag.contains("Fast", ignoreCase = true) || model.tag.contains("Turbo", ignoreCase = true)
                                    else -> true
                                }

                                val matchesFreeTierOnly = if (selectedCategory == ModelCategory.FREE_TIER) {
                                    model.isFree || model.freeTierNote.contains("Free", ignoreCase = true) || model.freeTierNote.contains("zdarma", ignoreCase = true)
                                } else true

                                matchesSearch && matchesCapability && matchesFreeTierOnly
                            }
                            matchesProvider || hasMatchingModels
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (providersToDisplay.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("🔍", fontSize = 32.sp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Žádné modely neodpovídají zadanému filtru",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Slate300
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Button(
                                            onClick = {
                                                searchQuery = ""
                                                selectedCategory = ModelCategory.ALL
                                                capabilityFilter = "all"
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = CyanBright, contentColor = Slate950)
                                        ) {
                                            Text("Zobrazit všechny modely")
                                        }
                                    }
                                }
                            }
                        }

                        providersToDisplay.forEach { provider ->
                            val matchingModels = provider.models.filter { model ->
                                val matchesSearch = searchQuery.isBlank() ||
                                        model.displayName.contains(searchQuery, ignoreCase = true) ||
                                        model.description.contains(searchQuery, ignoreCase = true) ||
                                        model.tag.contains(searchQuery, ignoreCase = true) ||
                                        provider.name.contains(searchQuery, ignoreCase = true)

                                val matchesCapability = when (capabilityFilter) {
                                    "code" -> model.tag.contains("Code", ignoreCase = true) || model.tag.contains("Coder", ignoreCase = true)
                                    "reasoning" -> model.isReasoning || model.tag.contains("Reasoning", ignoreCase = true) || model.tag.contains("Thinking", ignoreCase = true)
                                    "fast" -> model.tag.contains("Fast", ignoreCase = true) || model.tag.contains("Turbo", ignoreCase = true)
                                    else -> true
                                }

                                val matchesFreeTierOnly = if (selectedCategory == ModelCategory.FREE_TIER) {
                                    model.isFree || model.freeTierNote.contains("Free", ignoreCase = true) || model.freeTierNote.contains("zdarma", ignoreCase = true)
                                } else true

                                matchesSearch && matchesCapability && matchesFreeTierOnly
                            }

                            if (matchingModels.isNotEmpty()) {
                                item(key = "provider_header_${provider.id}") {
                                    CategorizedProviderHeaderCard(
                                        provider = provider,
                                        modelCount = matchingModels.size,
                                        onConfigureKey = {
                                            activeConfiguringProvider = provider
                                            keyInputValue = provider.apiKey
                                            urlInputValue = provider.customBaseUrl
                                            isKeyVisible = false
                                            keySaveSuccessMessage = null
                                        },
                                        onOpenKeyUrl = {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(provider.apiKeyUrl))
                                                context.startActivity(intent)
                                            } catch (_: Exception) {}
                                        }
                                    )
                                }

                                items(matchingModels, key = { "${provider.id}_${it.id}" }) { model ->
                                    EnhancedModelItemCard(
                                        model = model,
                                        provider = provider,
                                        isSelected = model.id == selectedModel.id,
                                        onSelect = {
                                            if (provider.isAvailable) {
                                                onModelSelected(model)
                                                onDismiss()
                                            } else {
                                                // Prompt user to enter key right away
                                                activeConfiguringProvider = provider
                                                keyInputValue = provider.apiKey
                                                urlInputValue = provider.customBaseUrl
                                                isKeyVisible = false
                                                keySaveSuccessMessage = null
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Dialog Bottom Action Footer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onOpenProviderSettings()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanBright),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Všechna nastavení providerů", fontSize = 11.sp)
                        }

                        TextButton(onClick = onDismiss) {
                            Text("Zavřít", color = Slate400, fontSize = 12.sp)
                        }
                    }
                }

                // In-Dialog Secure Key Management Sheet / Modal
                activeConfiguringProvider?.let { provider ->
                    InDialogSecureKeySheet(
                        provider = provider,
                        keyInput = keyInputValue,
                        urlInput = urlInputValue,
                        isKeyVisible = isKeyVisible,
                        successMessage = keySaveSuccessMessage,
                        securityInfo = securityInfo,
                        onKeyChange = { keyInputValue = it },
                        onUrlChange = { urlInputValue = it },
                        onToggleVisibility = { isKeyVisible = !isKeyVisible },
                        onSave = { key, url ->
                            onSaveApiKey?.invoke(provider.id, key)
                            if (url.isNotBlank()) {
                                onUpdateBaseUrl?.invoke(provider.id, url)
                            }
                            keySaveSuccessMessage = "Klíč byl bezpečně zašifrován a uložen do Android KeyStore!"
                        },
                        onOpenKeyUrl = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(provider.apiKeyUrl))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        onClose = {
                            activeConfiguringProvider = null
                            keySaveSuccessMessage = null
                        }
                    )
                }
            }
        }
    }
}

/**
 * Provider section card showing badge, free tier status, and quick key action.
 */
@Composable
fun CategorizedProviderHeaderCard(
    provider: AiProvider,
    modelCount: Int,
    onConfigureKey: () -> Unit,
    onOpenKeyUrl: () -> Unit
) {
    Surface(
        color = Slate950,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(text = provider.iconEmoji, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = provider.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Slate100
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Badge(
                                containerColor = if (provider.id == "ollama_local" || provider.badge.contains("Free", ignoreCase = true)) EmeraldSuccess.copy(alpha = 0.2f) else Slate800
                            ) {
                                Text(
                                    text = provider.badge,
                                    color = if (provider.id == "ollama_local" || provider.badge.contains("Free", ignoreCase = true)) EmeraldSuccess else CyanBright,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Text(
                            text = provider.freeTierDetails,
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400,
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }
                }

                // Status chip & Key action
                Column(horizontalAlignment = Alignment.End) {
                    if (provider.isAvailable) {
                        Surface(
                            color = EmeraldSuccess.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = EmeraldSuccess,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Aktivní",
                                    color = EmeraldSuccess,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        FilledTonalButton(
                            onClick = onConfigureKey,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = AmberWarning.copy(alpha = 0.2f),
                                contentColor = AmberWarning
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Zadat klíč", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$modelCount ${if (modelCount == 1) "model" else if (modelCount in 2..4) "modely" else "modelů"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate500,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

/**
 * Polished model item card with tags, context window, and free tier details.
 */
@Composable
fun EnhancedModelItemCard(
    model: AiModel,
    provider: AiProvider,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val borderColor = if (isSelected) CyanBright else Slate800
    val containerColor = if (isSelected) Slate800 else Slate950
    val isLocked = !provider.isAvailable

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onSelect)
            .testTag("model_card_${model.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(
                    selectedColor = CyanBright,
                    unselectedColor = if (isLocked) Slate600 else Slate400
                ),
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = model.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) CyanBright else Slate100
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Capability Tag Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    when {
                                        model.tag.contains("Fast", ignoreCase = true) -> CyanAccent.copy(alpha = 0.25f)
                                        model.isReasoning || model.tag.contains("Reasoning", ignoreCase = true) || model.tag.contains("Thinking", ignoreCase = true) -> PurpleAccent.copy(alpha = 0.25f)
                                        model.tag.contains("Code", ignoreCase = true) || model.tag.contains("Coder", ignoreCase = true) -> EmeraldSuccess.copy(alpha = 0.25f)
                                        model.tag.contains("Offline", ignoreCase = true) -> IndigoAccent.copy(alpha = 0.25f)
                                        else -> Slate800
                                    }
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = model.tag,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    model.tag.contains("Fast", ignoreCase = true) -> CyanBright
                                    model.isReasoning || model.tag.contains("Reasoning", ignoreCase = true) || model.tag.contains("Thinking", ignoreCase = true) -> PurpleAccent
                                    model.tag.contains("Code", ignoreCase = true) || model.tag.contains("Coder", ignoreCase = true) -> EmeraldSuccess
                                    model.tag.contains("Offline", ignoreCase = true) -> IndigoAccent
                                    else -> Slate300
                                }
                            )
                        }

                        // Context Window Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Slate800)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = model.contextWindow,
                                fontSize = 10.sp,
                                color = Slate300
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = model.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate300,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = model.freeTierNote,
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldSuccess,
                            fontSize = 10.sp
                        )
                    }

                    if (isLocked) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = AmberWarning,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Klikněte pro zadání klíče",
                                style = MaterialTheme.typography.labelSmall,
                                color = AmberWarning,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * In-Dialog bottom sheet / modal for entering and securely storing API keys.
 */
@Composable
fun InDialogSecureKeySheet(
    provider: AiProvider,
    keyInput: String,
    urlInput: String,
    isKeyVisible: Boolean,
    successMessage: String?,
    securityInfo: String,
    onKeyChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onToggleVisibility: () -> Unit,
    onSave: (String, String) -> Unit,
    onOpenKeyUrl: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        color = Slate950.copy(alpha = 0.95f),
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onClose)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanBright.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clickable(enabled = false) {}
                    .testTag("in_dialog_key_config_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(provider.iconEmoji, fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Zabezpečený klíč: ${provider.name}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate100
                                )
                                Text(
                                    text = provider.badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldSuccess
                                )
                            }
                        }

                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = "Zavřít", tint = Slate400)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Encryption info banner
                    Surface(
                        color = Slate950,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Klíč je šifrován přes AES-256-GCM v Android KeyStore. Neopouští vaše zařízení.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate300,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Free Key Action link
                    FilledTonalButton(
                        onClick = onOpenKeyUrl,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Slate800,
                            contentColor = CyanBright
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Získat bezplatný API klíč na oficiálním webu ↗", fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // API Key Input
                    Text(
                        text = "API Klíč pro ${provider.name}:",
                        style = MaterialTheme.typography.labelMedium,
                        color = Slate300
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = onKeyChange,
                        placeholder = { Text(provider.keyPlaceholder, color = Slate500, fontSize = 12.sp) },
                        visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = onToggleVisibility) {
                                Icon(
                                    imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Zobrazit klíč",
                                    tint = Slate400,
                                    modifier = Modifier.size(16.dp)
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
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("in_dialog_api_key_input")
                    )

                    // Optional Custom URL (For Ollama or proxies)
                    if (provider.id == "ollama_local" || urlInput.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "URL serveru (výchozí: ${provider.defaultBaseUrl}):",
                            style = MaterialTheme.typography.labelMedium,
                            color = Slate300
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = onUrlChange,
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
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    successMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = EmeraldSuccess.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(msg, color = EmeraldSuccess, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onClose,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate300)
                        ) {
                            Text("Zavřít")
                        }

                        Button(
                            onClick = { onSave(keyInput, urlInput) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanBright, contentColor = Slate950)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Uložit bezpečně", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
