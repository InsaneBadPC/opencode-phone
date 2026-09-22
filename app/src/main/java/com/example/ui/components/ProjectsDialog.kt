package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entities.ProjectEntity
import com.example.ui.OpenCodeViewModel
import com.example.ui.theme.*

@Composable
fun ProjectsDialog(
    viewModel: OpenCodeViewModel,
    onDismissRequest: () -> Unit
) {
    val projects by viewModel.allProjects.collectAsState()
    val activeProject by viewModel.activeProject.collectAsState()
    val context = LocalContext.current

    var showCreateForm by remember { mutableStateOf(false) }
    var projectToEdit by remember { mutableStateOf<ProjectEntity?>(null) }
    var projectToDelete by remember { mutableStateOf<ProjectEntity?>(null) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .testTag("projects_dialog"),
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
                            Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = CyanBright, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Správa projektů",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate100
                            )
                            Text(
                                text = "Každý projekt má svou vlastní pracovní složku",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate400,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.testTag("btn_close_projects_dialog")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Zavřít", tint = Slate400)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Bar: Add project button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Celkem projektů: ${projects.size}",
                        fontSize = 12.sp,
                        color = Slate400,
                        fontWeight = FontWeight.Medium
                    )

                    Button(
                        onClick = { showCreateForm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanBright, contentColor = Slate950),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_add_new_project")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Nový projekt", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Projects List
                if (projects.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Žádné projekty nenalezeny", color = Slate400)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(projects, key = { it.id }) { project ->
                            val isActive = project.id == (activeProject?.id ?: "proj_opencode")

                            ProjectCardItem(
                                project = project,
                                isActive = isActive,
                                onSelect = {
                                    viewModel.selectProject(project.id)
                                    Toast.makeText(context, "Aktivován projekt: ${project.name}", Toast.LENGTH_SHORT).show()
                                },
                                onOpenFiles = {
                                    viewModel.selectProject(project.id)
                                    viewModel.selectTab(1) // Tab Soubory
                                    onDismissRequest()
                                },
                                onCopyPath = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Project Path", project.workingDirectory))
                                    Toast.makeText(context, "Zkopírována cesta: ${project.workingDirectory}", Toast.LENGTH_SHORT).show()
                                },
                                onEdit = { projectToEdit = project },
                                onDelete = { projectToDelete = project }
                            )
                        }
                    }
                }
            }
        }
    }

    // Sub-dialog: Create New Project
    if (showCreateForm) {
        CreateProjectDialog(
            onDismiss = { showCreateForm = false },
            onCreate = { name, dir, lang, desc, branch ->
                viewModel.createProject(name, dir, lang, desc, branch)
                showCreateForm = false
                Toast.makeText(context, "Projekt $name vytvořen s pracovní složkou $dir", Toast.LENGTH_LONG).show()
            }
        )
    }

    // Sub-dialog: Edit Project
    projectToEdit?.let { proj ->
        EditProjectDialog(
            project = proj,
            onDismiss = { projectToEdit = null },
            onSave = { updated ->
                viewModel.updateProject(updated)
                projectToEdit = null
                Toast.makeText(context, "Projekt upraven", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Confirmation dialog: Delete Project
    projectToDelete?.let { proj ->
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            title = { Text("Smazat projekt ${proj.name}?", color = Slate100, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Tato akce odstraní projekt, jeho pracovní složku ${proj.workingDirectory} a související soubory.",
                    color = Slate300
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProject(proj)
                        projectToDelete = null
                        Toast.makeText(context, "Projekt smazán", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Smazat", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToDelete = null }) {
                    Text("Zrušit", color = Slate400)
                }
            },
            containerColor = Slate900
        )
    }
}

@Composable
private fun ProjectCardItem(
    project: ProjectEntity,
    isActive: Boolean,
    onSelect: () -> Unit,
    onOpenFiles: () -> Unit,
    onCopyPath: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("project_item_${project.id}")
            .clickable { onSelect() },
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) Slate800 else Slate850,
        border = if (isActive) androidx.compose.foundation.BorderStroke(1.5.dp, CyanBright) else androidx.compose.foundation.BorderStroke(1.dp, Slate700)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Row 1: Title + Active badge + Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) CyanBright else Slate100
                    )
                    if (isActive) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge(containerColor = CyanBright) {
                            Text("AKTIVNÍ", color = Slate950, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Upravit", tint = Slate400, modifier = Modifier.size(16.dp))
                    }
                    if (!project.isDefault) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Smazat", tint = ErrorRed, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Working directory box
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Slate950,
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCopyPath() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = project.workingDirectory,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Slate200,
                            maxLines = 1
                        )
                    }
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Kopírovat cestu",
                        tint = Slate400,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            if (project.description.isNotBlank()) {
                Text(
                    text = project.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate300,
                    fontSize = 12.sp,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Tags + Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AssistChip(
                        onClick = {},
                        label = { Text(project.language, fontSize = 10.sp, color = CyanBright) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = Slate900),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Slate700)
                    )
                    AssistChip(
                        onClick = {},
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ForkRight, contentDescription = null, modifier = Modifier.size(12.dp), tint = Slate400)
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(project.gitBranch, fontSize = 10.sp, color = Slate300)
                            }
                        },
                        colors = AssistChipDefaults.assistChipColors(containerColor = Slate900),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Slate700)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!isActive) {
                        OutlinedButton(
                            onClick = onSelect,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanBright),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanBright)
                        ) {
                            Text("Aktivovat", fontSize = 11.sp)
                        }
                    }

                    FilledTonalButton(
                        onClick = onOpenFiles,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = if (isActive) CyanBright else Slate700, contentColor = if (isActive) Slate950 else Slate100)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Soubory", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, dir: String, lang: String, desc: String, branch: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var dir by remember { mutableStateOf("/workspace/") }
    var lang by remember { mutableStateOf("Kotlin / Compose") }
    var desc by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("main") }

    val languages = listOf("Kotlin / Compose", "TypeScript / Node.js", "Python / AI", "React / Vite", "Rust", "Go")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = CyanBright)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Vytvořit nový projekt", color = Slate100, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (dir == "/workspace/" || dir.startsWith("/workspace/")) {
                            val slug = it.lowercase().replace(" ", "-").replace(Regex("[^a-z0-9-]"), "")
                            dir = "/workspace/$slug"
                        }
                    },
                    label = { Text("Název projektu") },
                    placeholder = { Text("např. E-Commerce API") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanBright,
                        unfocusedBorderColor = Slate700,
                        focusedTextColor = Slate100,
                        unfocusedTextColor = Slate100
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_project_name")
                )

                OutlinedTextField(
                    value = dir,
                    onValueChange = { dir = it },
                    label = { Text("Pracovní složka") },
                    placeholder = { Text("/workspace/muj-projekt") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanBright,
                        unfocusedBorderColor = Slate700,
                        focusedTextColor = Slate100,
                        unfocusedTextColor = Slate100
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_project_dir")
                )

                // Language selector
                Text("Hlavní jazyk / technologie:", fontSize = 12.sp, color = Slate400)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    languages.take(3).forEach { l ->
                        FilterChip(
                            selected = lang == l,
                            onClick = { lang = l },
                            label = { Text(l.substringBefore(" /"), fontSize = 10.sp) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    languages.drop(3).forEach { l ->
                        FilterChip(
                            selected = lang == l,
                            onClick = { lang = l },
                            label = { Text(l.substringBefore(" /"), fontSize = 10.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Popis projektu") },
                    placeholder = { Text("K čemu projekt slouží...") },
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanBright,
                        unfocusedBorderColor = Slate700,
                        focusedTextColor = Slate100,
                        unfocusedTextColor = Slate100
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && dir.isNotBlank()) {
                        onCreate(name, dir, lang, desc, branch)
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = CyanBright, contentColor = Slate950),
                modifier = Modifier.testTag("btn_confirm_create_project")
            ) {
                Text("Vytvořit projekt", fontWeight = FontWeight.Bold)
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
private fun EditProjectDialog(
    project: ProjectEntity,
    onDismiss: () -> Unit,
    onSave: (ProjectEntity) -> Unit
) {
    var name by remember { mutableStateOf(project.name) }
    var dir by remember { mutableStateOf(project.workingDirectory) }
    var branch by remember { mutableStateOf(project.gitBranch) }
    var desc by remember { mutableStateOf(project.description) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Upravit projekt", color = Slate100, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Název projektu") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanBright,
                        unfocusedBorderColor = Slate700,
                        focusedTextColor = Slate100,
                        unfocusedTextColor = Slate100
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dir,
                    onValueChange = { dir = it },
                    label = { Text("Pracovní složka") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanBright,
                        unfocusedBorderColor = Slate700,
                        focusedTextColor = Slate100,
                        unfocusedTextColor = Slate100
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = branch,
                    onValueChange = { branch = it },
                    label = { Text("Git větev") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanBright,
                        unfocusedBorderColor = Slate700,
                        focusedTextColor = Slate100,
                        unfocusedTextColor = Slate100
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Popis") },
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanBright,
                        unfocusedBorderColor = Slate700,
                        focusedTextColor = Slate100,
                        unfocusedTextColor = Slate100
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(project.copy(name = name, workingDirectory = dir, gitBranch = branch, description = desc))
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanBright, contentColor = Slate950)
            ) {
                Text("Uložit změny", fontWeight = FontWeight.Bold)
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
