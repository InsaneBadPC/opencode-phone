package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
        // Clean Top Toolbar: Model & Project selector
        Surface(
            color = Slate900,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
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
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(EmeraldBright, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${selectedAiModel.displayName} • ${selectedAiModel.providerName}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = CyanBright
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = CyanBright,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Slate800
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = Slate700,
                            enabled = true,
                            selected = true
                        ),
                        modifier = Modifier.testTag("model_picker_chip")
                    )

                    // Session Controls
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showSessionsDialog = true },
                            modifier = Modifier.size(36.dp).testTag("btn_open_sessions_dialog")
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
                                    tint = Slate300,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.createNewSession() },
                            modifier = Modifier.size(36.dp).testTag("new_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddComment,
                                contentDescription = "Nová relace",
                                tint = Slate300,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Active Project & Working Folder Bar
                activeProject?.let { proj ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Slate950,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Slate800),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp)
                            .clickable { showProjectsDialog = true }
                            .testTag("chat_active_project_bar")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = CyanBright, modifier = Modifier.size(13.dp))
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
                                    color = Slate400,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1
                                )
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Slate500, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        // Messages List or Empty Hero State
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(CyanBright.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = CyanBright,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "OpenCode Agent",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate100
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Autonomní vývojářský agent přímo ve vašem pracovním prostoru.\nVytváří soubory, píše kód, hledá na webu a spouští terminál.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // 4 Quick Start Action Cards
                    val starterPrompts = listOf(
                        Triple("📝 Vytvoř soubor calculator.py", "Vytvoř soubor calculator.py s kalkulačkou v Pythonu", EmeraldBright),
                        Triple("🌐 Hledej na webu Jetpack Compose novinky", "Hledej na webu: Jetpack Compose 1.7 news", CyanBright),
                        Triple("💻 Spusť v terminálu git status", "git status", AmberWarning),
                        Triple("🚀 Vytvoř novou verzi a sestav APK", "/release 1.3.0 'Automatická aktualizace aplikace'", VioletPurple)
                    )

                    starterPrompts.forEach { (title, promptToSend, color) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Slate900,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Slate800),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable { viewModel.sendMessage(promptToSend) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(color, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = Slate200
                                )
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatMessageItem(message = msg)
                }

                if (isThinking) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Slate900,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Slate800),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = CyanBright,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "OpenCode Agent provádí požadavek...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CyanBright,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Clean Input Bar
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
                            "Napište úkol (vytvoř soubor, uprav, hledej na webu, terminál)...",
                            color = Slate400,
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    maxLines = 4,
                    shape = RoundedCornerShape(18.dp),
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
                        .size(44.dp)
                        .background(
                            if (chatInput.isNotBlank() && !isThinking) CyanBright else Slate800,
                            CircleShape
                        )
                        .testTag("send_message_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Odeslat",
                        tint = if (chatInput.isNotBlank() && !isThinking) Slate950 else Slate500,
                        modifier = Modifier.size(18.dp)
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
        // Subtle role indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(if (isUser) VioletPurple else CyanBright, CircleShape)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = if (isUser) "Vývojář" else "OpenCode Agent",
                style = MaterialTheme.typography.labelSmall,
                color = if (isUser) Slate400 else CyanBright,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
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

        // Message Content
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
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Slate800),
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
                modifier = Modifier.padding(vertical = 4.dp)
            )
        } else {
            // Normal text
            val trimmedPart = part.trim()
            if (trimmedPart.isNotBlank()) {
                Text(
                    text = trimmedPart,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate100,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}
