package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.ZenModel
import com.example.data.local.entities.ChatMessageEntity
import com.example.ui.OpenCodeViewModel
import com.example.ui.components.AiModelPickerDialog
import com.example.ui.components.CodeBlockView
import com.example.ui.components.ProjectsDialog
import com.example.ui.components.SessionsDialog
import com.example.ui.components.ToolExecutionCard
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: OpenCodeViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.currentMessages.collectAsState()
    val isThinking by viewModel.isAiThinking.collectAsState()
    val chatInput by viewModel.chatInput.collectAsState()
    val selectedAiModel by viewModel.selectedAiModel.collectAsState()
    val availableModelsByProvider by viewModel.availableModelsByProvider.collectAsState()
    val allAiProviders by viewModel.allAiProviders.collectAsState()
    val skills by viewModel.skills.collectAsState()
    val enabledSkills = remember(skills) { skills.filter { it.isEnabled } }

    val activeProject by viewModel.activeProject.collectAsState()
    val sessions by viewModel.sessions.collectAsState()

    var showModelPickerDialog by remember { mutableStateOf(false) }
    var showSessionsDialog by remember { mutableStateOf(false) }
    var showProjectsDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    if (showSessionsDialog) {
        SessionsDialog(
            viewModel = viewModel,
            onDismissRequest = { showSessionsDialog = false }
        )
    }

    if (showProjectsDialog) {
        ProjectsDialog(
            viewModel = viewModel,
            onDismissRequest = { showProjectsDialog = false }
        )
    }

    if (showModelPickerDialog) {
        AiModelPickerDialog(
            selectedModel = selectedAiModel,
            availableModelsByProvider = availableModelsByProvider,
            allProviders = allAiProviders,
            onModelSelected = { viewModel.selectAiModel(it) },
            onSaveApiKey = { providerId, key -> viewModel.updateProviderApiKey(providerId, key) },
            onUpdateBaseUrl = { providerId, url -> viewModel.updateProviderBaseUrl(providerId, url) },
            onOpenProviderSettings = { viewModel.openAiProvidersSettings() },
            onDismiss = { showModelPickerDialog = false },
            securityInfo = viewModel.getStorageSecurityInfo()
        )
    }

    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Toolbar: Model Selector & Actions
        Surface(
            color = Slate900,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Model Dropdown Badge
                    FilterChip(
                        selected = true,
                        onClick = { showModelPickerDialog = true },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = CyanBright,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${selectedAiModel.displayName} • ${selectedAiModel.providerName}",
                                    fontWeight = FontWeight.SemiBold,
                                    color = CyanBright
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = CyanBright,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Slate800
                        ),
                        modifier = Modifier.testTag("model_picker_chip")
                    )

                    // Session Controls
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showSessionsDialog = true },
                            modifier = Modifier.testTag("btn_open_sessions_dialog")
                        ) {
                            BadgedBox(
                                badge = {
                                    Badge(containerColor = CyanBright) {
                                        Text(
                                            text = "${sessions.size}",
                                            color = Slate950,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = "Seznam relací",
                                    tint = Slate200,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.createNewSession() },
                            modifier = Modifier.testTag("new_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddComment,
                                contentDescription = "Nová relace",
                                tint = Slate200
                            )
                        }
                    }
                }

                // Active Project & Working Folder Chip
                activeProject?.let { proj ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Slate950,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Slate800),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .clickable { showProjectsDialog = true }
                            .testTag("chat_active_project_bar")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = CyanBright, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = proj.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate200
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = proj.workingDirectory,
                                    fontSize = 10.sp,
                                    color = AmberWarning,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1
                                )
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Slate400, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Active Skills Pills
                if (enabledSkills.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Aktivní dovednosti: ",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate400
                        )
                        enabledSkills.forEach { skill ->
                            AssistChip(
                                onClick = {},
                                label = { Text(skill.name, fontSize = 11.sp) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(EmeraldBright, CircleShape)
                                    )
                                },
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .height(28.dp),
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = Slate800,
                                    labelColor = Slate200
                                )
                            )
                        }
                    }
                }
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatMessageItem(message = msg)
            }

            if (isThinking) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = CyanBright,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "${selectedAiModel.displayName} generuje kód a analyzuje odpověď...",
                            style = MaterialTheme.typography.bodySmall,
                            color = CyanBright,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Quick Prompt Action Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            val chips = listOf(
                "/release 1.3.0 'Nová aktualizace kódu'",
                "/secret list",
                "/update check",
                "/edit AppConfig.json",
                "/secret set GEMINI_API_KEY AIzaSy...",
                "/terminal git status",
                "/search Jetpack Compose M3",
                "Vysvětli architekturu projektu"
            )
            chips.forEach { chipText ->
                SuggestionChip(
                    onClick = {
                        viewModel.sendMessage(chipText)
                    },
                    label = {
                        Text(
                            text = chipText,
                            fontSize = 11.sp,
                            fontFamily = if (chipText.startsWith("/")) FontFamily.Monospace else FontFamily.Default
                        )
                    },
                    modifier = Modifier.padding(end = 6.dp),
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = Slate800,
                        labelColor = if (chipText.startsWith("/")) CyanBright else Slate200
                    )
                )
            }
        }

        // Input Bar
        Surface(
            color = Slate900,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = chatInput,
                    onValueChange = { viewModel.updateChatInput(it) },
                    placeholder = {
                        Text(
                            "Zeptejte se OpenCode nebo zadejte /search, /terminal...",
                            color = Slate400,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    maxLines = 4,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Slate950,
                        unfocusedContainerColor = Slate950,
                        focusedBorderColor = CyanBright,
                        unfocusedBorderColor = Slate700,
                        focusedTextColor = Slate100,
                        unfocusedTextColor = Slate100
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { viewModel.sendMessage() },
                    enabled = chatInput.isNotBlank() && !isThinking,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (chatInput.isNotBlank() && !isThinking) CyanBright else Slate700,
                            CircleShape
                        )
                        .testTag("send_message_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Odeslat",
                        tint = if (chatInput.isNotBlank() && !isThinking) Slate950 else Slate400,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessageEntity) {
    val isUser = message.role == "user"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Role Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Icon(
                imageVector = if (isUser) Icons.Default.Person else Icons.Default.Psychology,
                contentDescription = null,
                tint = if (isUser) VioletPurple else CyanBright,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isUser) "Vývojář" else "OpenCode Zen",
                style = MaterialTheme.typography.labelSmall,
                color = if (isUser) VioletPurple else CyanBright,
                fontWeight = FontWeight.Bold
            )
        }

        // Tool call card if available
        if (!message.toolName.isNullOrBlank()) {
            ToolExecutionCard(
                toolName = message.toolName,
                toolArgs = message.toolArgs,
                toolResult = message.toolResult,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        // Message Content Bubble or Blocks
        if (isUser) {
            Surface(
                shape = RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp),
                color = Slate800,
                modifier = Modifier.widthIn(max = 320.dp)
            ) {
                Text(
                    text = message.content,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate100
                )
            }
        } else {
            Surface(
                shape = RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp),
                color = Slate900,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    RenderMarkdownWithCode(message.content)
                }
            }
        }
    }
}

@Composable
fun RenderMarkdownWithCode(text: String) {
    // Splits text by triple-backtick code blocks
    val parts = text.split("```")
    parts.forEachIndexed { index, part ->
        if (index % 2 == 1) {
            // Code block
            val lines = part.lines()
            val firstLine = lines.firstOrNull()?.trim().orEmpty()
            val lang = if (firstLine.isNotBlank() && firstLine.all { it.isLetterOrDigit() }) firstLine else "kotlin"
            val codeBody = if (firstLine.isNotBlank() && firstLine.all { it.isLetterOrDigit() }) {
                lines.drop(1).joinToString("\n")
            } else {
                part
            }
            CodeBlockView(
                code = codeBody.trimEnd(),
                language = lang,
                modifier = Modifier.padding(vertical = 6.dp)
            )
        } else {
            // Normal text
            if (part.isNotBlank()) {
                Text(
                    text = part.trim(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate100,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}
