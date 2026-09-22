package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entities.ChatSessionEntity
import com.example.data.models.TermuxSyncState
import com.example.ui.OpenCodeViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SessionsDialog(
    viewModel: OpenCodeViewModel,
    onDismissRequest: () -> Unit
) {
    val allSessions by viewModel.sessions.collectAsState()
    val activeProjectId by viewModel.currentProjectId.collectAsState()
    val allProjects by viewModel.allProjects.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()
    val termuxSyncConfig by viewModel.termuxSyncConfig.collectAsState()
    val termuxSyncStatus by viewModel.termuxSyncStatus.collectAsState()
    val context = LocalContext.current

    var filterOnlyCurrentProject by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var sessionToRename by remember { mutableStateOf<ChatSessionEntity?>(null) }
    var sessionToDelete by remember { mutableStateOf<ChatSessionEntity?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val activeProject = allProjects.find { it.id == activeProjectId }

    val filteredSessions = remember(allSessions, filterOnlyCurrentProject, activeProjectId, searchQuery) {
        allSessions
            .filter { session ->
                if (filterOnlyCurrentProject) session.projectId == activeProjectId else true
            }
            .filter { session ->
                if (searchQuery.isBlank()) true
                else session.title.contains(searchQuery, ignoreCase = true)
            }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .testTag("sessions_dialog"),
            shape = RoundedCornerShape(16.dp),
            color = Slate900,
            tonalElevation = 8.dp
        ) {
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CyanDark.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ChatBubble, contentDescription = null, tint = CyanBright, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Správa relací (Sessions)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate100
                            )
                            Text(
                                text = "Projekt: ${activeProject?.name ?: "Všechny"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate400,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.testTag("btn_close_sessions_dialog")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Zavřít", tint = Slate400)
                    }
                }

                // Termux CLI Auto-Sync Banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Slate950,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (termuxSyncConfig.isAutoSyncEnabled) EmeraldSuccess.copy(alpha = 0.5f) else Slate800
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (termuxSyncConfig.isAutoSyncEnabled) EmeraldSuccess else Slate500)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Termux CLI Auto-Sync",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate100
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Badge(containerColor = Slate800) {
                                        Text(
                                            text = ":${termuxSyncConfig.port}",
                                            fontSize = 9.sp,
                                            color = CyanBright,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                                Text(
                                    text = if (termuxSyncConfig.isAutoSyncEnabled)
                                        "Aktivní synchronizace (každých ${termuxSyncConfig.syncIntervalSeconds}s)"
                                    else "Automatická synchronizace pozastavena",
                                    fontSize = 9.sp,
                                    color = if (termuxSyncConfig.isAutoSyncEnabled) EmeraldBright else Slate400
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    viewModel.triggerTermuxSyncNow()
                                    Toast.makeText(context, "Synchronizuji z Termuxu...", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp).testTag("btn_termux_sync_now")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Synchronizovat z Termuxu",
                                    tint = if (termuxSyncStatus.state == TermuxSyncState.SYNCING) AmberWarning else CyanBright,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Switch(
                                checked = termuxSyncConfig.isAutoSyncEnabled,
                                onCheckedChange = { viewModel.toggleTermuxAutoSync(it) },
                                modifier = Modifier.scale(0.75f).testTag("switch_termux_auto_sync")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Search Bar + Create Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Hledat relaci...", fontSize = 12.sp, color = Slate500) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate400, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Vymazat", tint = Slate400, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanBright,
                            unfocusedBorderColor = Slate700,
                            focusedTextColor = Slate100,
                            unfocusedTextColor = Slate100
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    )

                    Button(
                        onClick = { showCreateDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanBright, contentColor = Slate950),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("btn_create_new_session")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Nová", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filter chips: All vs Active Project
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !filterOnlyCurrentProject,
                        onClick = { filterOnlyCurrentProject = false },
                        label = { Text("Všechny relace (${allSessions.size})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Slate800,
                            selectedLabelColor = CyanBright
                        )
                    )
                    FilterChip(
                        selected = filterOnlyCurrentProject,
                        onClick = { filterOnlyCurrentProject = true },
                        label = {
                            Text(
                                "Projekt: ${activeProject?.name?.take(16) ?: "Aktivní"}...",
                                fontSize = 11.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Slate800,
                            selectedLabelColor = CyanBright
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Sessions List
                if (filteredSessions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Chat, contentDescription = null, tint = Slate600, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Žádné relace neodpovídají filtru", color = Slate400, fontSize = 13.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredSessions, key = { it.id }) { session ->
                            val isCurrent = session.id == currentSessionId
                            val sessionProject = allProjects.find { it.id == session.projectId }

                            SessionCardItem(
                                session = session,
                                isCurrent = isCurrent,
                                projectName = sessionProject?.name,
                                onSelect = {
                                    viewModel.selectSession(session.id)
                                    // Switch project if session belongs to another project
                                    if (session.projectId != null && session.projectId != activeProjectId) {
                                        viewModel.selectProject(session.projectId)
                                    }
                                    viewModel.selectTab(0) // Switch to Chat tab
                                    onDismissRequest()
                                    Toast.makeText(context, "Otevřena relace: ${session.title}", Toast.LENGTH_SHORT).show()
                                },
                                onRename = { sessionToRename = session },
                                onDelete = { sessionToDelete = session }
                            )
                        }
                    }
                }
            }
        }
    }

    // Sub-dialog: Create Session
    if (showCreateDialog) {
        CreateSessionDialog(
            activeProjectId = activeProjectId,
            projects = allProjects,
            onDismiss = { showCreateDialog = false },
            onCreate = { title, projId ->
                viewModel.createSessionForProject(projId, title)
                showCreateDialog = false
                viewModel.selectTab(0)
                onDismissRequest()
                Toast.makeText(context, "Vytvořena relace: $title", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Sub-dialog: Rename Session
    sessionToRename?.let { sess ->
        RenameSessionDialog(
            session = sess,
            onDismiss = { sessionToRename = null },
            onRename = { newTitle ->
                viewModel.renameSession(sess.id, newTitle)
                sessionToRename = null
                Toast.makeText(context, "Relace přejmenována", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Confirmation dialog: Delete Session
    sessionToDelete?.let { sess ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("Smazat relaci?", color = Slate100, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Opravdu si přejete smazat relaci „${sess.title}“ a celou její historii zpráv?",
                    color = Slate300
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSession(sess)
                        sessionToDelete = null
                        Toast.makeText(context, "Relace smazána", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Smazat", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("Zrušit", color = Slate400)
                }
            },
            containerColor = Slate900
        )
    }
}

@Composable
private fun SessionCardItem(
    session: ChatSessionEntity,
    isCurrent: Boolean,
    projectName: String?,
    onSelect: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("d. M. yyyy, HH:mm", Locale.getDefault()) }
    val formattedDate = remember(session.createdAt) { dateFormat.format(Date(session.createdAt)) }

    val isTermuxSession = session.projectId == "proj_termux_cli" || session.title.contains("Termux", ignoreCase = true)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("session_item_${session.id}")
            .clickable { onSelect() },
        shape = RoundedCornerShape(12.dp),
        color = if (isCurrent) Slate800 else Slate850,
        border = if (isCurrent) androidx.compose.foundation.BorderStroke(1.5.dp, if (isTermuxSession) EmeraldSuccess else CyanBright)
        else androidx.compose.foundation.BorderStroke(1.dp, if (isTermuxSession) EmeraldSuccess.copy(alpha = 0.4f) else Slate700)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isCurrent) (if (isTermuxSession) EmeraldSuccess else CyanBright).copy(alpha = 0.2f)
                        else Slate900
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isTermuxSession) Icons.Default.Terminal
                    else if (isCurrent) Icons.Default.ChatBubble
                    else Icons.Default.ChatBubbleOutline,
                    contentDescription = null,
                    tint = if (isTermuxSession) EmeraldBright
                    else if (isCurrent) CyanBright
                    else Slate400,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Body
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = session.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                        color = if (isCurrent) (if (isTermuxSession) EmeraldBright else CyanBright) else Slate100,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isTermuxSession) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Badge(containerColor = EmeraldSuccess.copy(alpha = 0.2f)) {
                            Text("TERMUX", color = EmeraldBright, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (isCurrent) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Badge(containerColor = CyanBright) {
                            Text("AKTIVNÍ", color = Slate950, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (projectName != null) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Slate950,
                            modifier = Modifier.padding(vertical = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(10.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(projectName, fontSize = 9.sp, color = Slate300, maxLines = 1)
                            }
                        }
                    }

                    Text(
                        text = formattedDate,
                        fontSize = 10.sp,
                        color = Slate500,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Action buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onRename,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Přejmenovat", tint = Slate400, modifier = Modifier.size(15.dp))
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Smazat", tint = ErrorRed, modifier = Modifier.size(15.dp))
                }
            }
        }
    }
}

@Composable
private fun CreateSessionDialog(
    activeProjectId: String,
    projects: List<com.example.data.local.entities.ProjectEntity>,
    onDismiss: () -> Unit,
    onCreate: (title: String, projectId: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedProjectId by remember { mutableStateOf(activeProjectId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AddComment, contentDescription = null, tint = CyanBright)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Vytvořit novou relaci", color = Slate100, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Název relace") },
                    placeholder = { Text("např. Refaktoring databáze") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanBright,
                        unfocusedBorderColor = Slate700,
                        focusedTextColor = Slate100,
                        unfocusedTextColor = Slate100
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_session_title")
                )

                Text("Přiřadit k projektu:", fontSize = 12.sp, color = Slate400)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    projects.forEach { proj ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { selectedProjectId = proj.id }
                                .background(if (selectedProjectId == proj.id) Slate800 else Color.Transparent)
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedProjectId == proj.id,
                                onClick = { selectedProjectId = proj.id },
                                colors = RadioButtonDefaults.colors(selectedColor = CyanBright)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(proj.name, fontSize = 12.sp, color = Slate100, fontWeight = FontWeight.Medium)
                                Text(proj.workingDirectory, fontSize = 10.sp, color = Slate400, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalTitle = title.trim().ifEmpty { "Nová relace" }
                    onCreate(finalTitle, selectedProjectId)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanBright, contentColor = Slate950),
                modifier = Modifier.testTag("btn_confirm_create_session")
            ) {
                Text("Zahájit relaci", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zrušit", color = Slate400)
            }
        },
        containerColor = Slate900
    )
}

@Composable
private fun RenameSessionDialog(
    session: ChatSessionEntity,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var title by remember { mutableStateOf(session.title) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Přejmenovat relaci", color = Slate100, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Název") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanBright,
                    unfocusedBorderColor = Slate700,
                    focusedTextColor = Slate100,
                    unfocusedTextColor = Slate100
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) onRename(title.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanBright, contentColor = Slate950)
            ) {
                Text("Uložit", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zrušit", color = Slate400)
            }
        },
        containerColor = Slate900
    )
}
