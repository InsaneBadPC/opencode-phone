package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.WorkspaceFileEntity
import com.example.ui.OpenCodeViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tree node representation for hierarchical project file browsing.
 */
data class FileTreeNode(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val entity: WorkspaceFileEntity? = null,
    val children: MutableList<FileTreeNode> = mutableListOf(),
    val depth: Int = 0
)

enum class FileExplorerViewMode {
    TREE, FLAT, RECENT
}

enum class FileSortOrder {
    NAME_ASC, NAME_DESC, TYPE, RECENTLY_MODIFIED
}

/**
 * Production-ready File Explorer component for browsing and managing
 * local project files within the OpenCode development environment.
 */
@Composable
fun FileExplorerComponent(
    viewModel: OpenCodeViewModel,
    modifier: Modifier = Modifier,
    onFileSelected: ((WorkspaceFileEntity) -> Unit)? = null,
    compactMode: Boolean = false
) {
    val files by viewModel.workspaceFiles.collectAsState()
    val openedFile by viewModel.openedFile.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Search and filter state
    var searchQuery by remember { mutableStateOf("") }
    var viewMode by remember { mutableStateOf(FileExplorerViewMode.TREE) }
    var sortOrder by remember { mutableStateOf(FileSortOrder.NAME_ASC) }
    var isSearchExpanded by remember { mutableStateOf(false) }

    // Tree expanded folders set
    var expandedFolders by remember {
        mutableStateOf(
            setOf("src", "app", "app/src", "app/src/main", "config", "scripts", "docs")
        )
    }

    // Modal dialogs state
    var showNewFileDialog by remember { mutableStateOf(false) }
    var newFileParentFolder by remember { mutableStateOf("") }

    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderParentPath by remember { mutableStateOf("") }

    var renamingItem by remember { mutableStateOf<Pair<String, Boolean>?>(null) } // path, isDirectory
    var deletingItem by remember { mutableStateOf<Pair<String, Boolean>?>(null) } // path, isDirectory
    var duplicatingFile by remember { mutableStateOf<WorkspaceFileEntity?>(null) }
    var inspectingFile by remember { mutableStateOf<WorkspaceFileEntity?>(null) }

    // Build hierarchical tree
    val rootTree = remember(files, expandedFolders) {
        buildFileTree(files)
    }

    // Flatten tree respecting collapsed folders
    val flattenedTreeItems = remember(rootTree, expandedFolders, searchQuery, sortOrder) {
        if (searchQuery.isNotBlank()) {
            // Flatten all files matching query
            files.filter {
                it.path.contains(searchQuery, ignoreCase = true) ||
                        it.name.contains(searchQuery, ignoreCase = true)
            }.map {
                FileTreeNode(
                    path = it.path,
                    name = it.name,
                    isDirectory = it.isDirectory,
                    entity = it,
                    depth = 0
                )
            }
        } else {
            flattenTree(rootTree, expandedFolders, sortOrder)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        // --- 1. Explorer Header Toolbar ---
        Surface(
            color = Slate900,
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(CyanAccent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = CyanBright,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Projektový Průzkumník",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Slate100
                            )
                            Text(
                                text = "${files.count { !it.isDirectory }} souborů • ${files.count { it.isDirectory }} složek",
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate400,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Action Icons
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Search Toggle
                        IconButton(
                            onClick = { isSearchExpanded = !isSearchExpanded },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Hledat soubor",
                                tint = if (searchQuery.isNotBlank()) CyanBright else Slate300,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // New File in Root
                        IconButton(
                            onClick = {
                                newFileParentFolder = ""
                                showNewFileDialog = true
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("explorer_new_file_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.NoteAdd,
                                contentDescription = "Nový soubor",
                                tint = CyanBright,
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        // New Folder in Root
                        IconButton(
                            onClick = {
                                newFolderParentPath = ""
                                showNewFolderDialog = true
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("explorer_new_folder_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreateNewFolder,
                                contentDescription = "Nová složka",
                                tint = AmberWarning,
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        // Quick Menu (Expand All, Collapse All, Sort)
                        var showHeaderMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(
                                onClick = { showHeaderMenu = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Více možností",
                                    tint = Slate400,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showHeaderMenu,
                                onDismissRequest = { showHeaderMenu = false },
                                modifier = Modifier.background(Slate900)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Rozbalit všechny složky", color = Slate100, fontSize = 12.sp) },
                                    leadingIcon = { Icon(Icons.Default.UnfoldMore, null, tint = CyanBright, modifier = Modifier.size(16.dp)) },
                                    onClick = {
                                        showHeaderMenu = false
                                        val allFolderPaths = mutableSetOf<String>()
                                        collectAllFolderPaths(rootTree, allFolderPaths)
                                        expandedFolders = allFolderPaths
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sbalit všechny složky", color = Slate100, fontSize = 12.sp) },
                                    leadingIcon = { Icon(Icons.Default.UnfoldLess, null, tint = Slate400, modifier = Modifier.size(16.dp)) },
                                    onClick = {
                                        showHeaderMenu = false
                                        expandedFolders = emptySet()
                                    }
                                )
                                HorizontalDivider(color = Slate800)
                                DropdownMenuItem(
                                    text = { Text("Řadit podle názvu (A-Z)", color = Slate100, fontSize = 12.sp) },
                                    leadingIcon = { Icon(Icons.Default.SortByAlpha, null, tint = if (sortOrder == FileSortOrder.NAME_ASC) CyanBright else Slate400, modifier = Modifier.size(16.dp)) },
                                    onClick = {
                                        sortOrder = FileSortOrder.NAME_ASC
                                        showHeaderMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Řadit podle typu souboru", color = Slate100, fontSize = 12.sp) },
                                    leadingIcon = { Icon(Icons.Default.Category, null, tint = if (sortOrder == FileSortOrder.TYPE) CyanBright else Slate400, modifier = Modifier.size(16.dp)) },
                                    onClick = {
                                        sortOrder = FileSortOrder.TYPE
                                        showHeaderMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Řadit podle poslední změny", color = Slate100, fontSize = 12.sp) },
                                    leadingIcon = { Icon(Icons.Default.Schedule, null, tint = if (sortOrder == FileSortOrder.RECENTLY_MODIFIED) CyanBright else Slate400, modifier = Modifier.size(16.dp)) },
                                    onClick = {
                                        sortOrder = FileSortOrder.RECENTLY_MODIFIED
                                        showHeaderMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Expandable Search Bar
                AnimatedVisibility(
                    visible = isSearchExpanded || searchQuery.isNotBlank(),
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Filtrovat soubory a složky...", fontSize = 12.sp, color = Slate500) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("explorer_search_input"),
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.FilterList, contentDescription = null, tint = CyanBright, modifier = Modifier.size(16.dp))
                            },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Clear, contentDescription = "Vymazat", tint = Slate400, modifier = Modifier.size(14.dp))
                                    }
                                }
                            },
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, color = Slate100),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Slate950,
                                unfocusedContainerColor = Slate950,
                                focusedBorderColor = CyanBright,
                                unfocusedBorderColor = Slate800
                            )
                        )
                    }
                }

                // View Mode Segmented Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ExplorerTabPill(
                        selected = viewMode == FileExplorerViewMode.TREE,
                        icon = Icons.Default.AccountTree,
                        label = "Strom",
                        onClick = { viewMode = FileExplorerViewMode.TREE }
                    )
                    ExplorerTabPill(
                        selected = viewMode == FileExplorerViewMode.FLAT,
                        icon = Icons.Default.ViewList,
                        label = "Seznam",
                        onClick = { viewMode = FileExplorerViewMode.FLAT }
                    )
                    ExplorerTabPill(
                        selected = viewMode == FileExplorerViewMode.RECENT,
                        icon = Icons.Default.History,
                        label = "Nedávné",
                        onClick = { viewMode = FileExplorerViewMode.RECENT }
                    )
                }
            }
        }

        // --- 2. Interactive Breadcrumbs Path ---
        openedFile?.let { currentFile ->
            FileBreadcrumbBar(
                filePath = currentFile.path,
                onCrumbClick = { folderCrumbPath ->
                    expandedFolders = expandedFolders + folderCrumbPath
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Slate900.copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        HorizontalDivider(color = Slate800, thickness = 1.dp)

        // --- 3. File Items List / Tree View ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (viewMode) {
                FileExplorerViewMode.TREE -> {
                    if (flattenedTreeItems.isEmpty()) {
                        EmptyExplorerView(searchQuery = searchQuery)
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 4.dp)
                        ) {
                            items(
                                items = flattenedTreeItems,
                                key = { it.path }
                            ) { node ->
                                val isExpanded = expandedFolders.contains(node.path)
                                val isSelected = openedFile?.path == node.path

                                FileTreeRowItem(
                                    node = node,
                                    isExpanded = isExpanded,
                                    isSelected = isSelected,
                                    depth = node.depth,
                                    onClick = {
                                        if (node.isDirectory) {
                                            expandedFolders = if (isExpanded) {
                                                expandedFolders - node.path
                                            } else {
                                                expandedFolders + node.path
                                            }
                                        } else {
                                            node.entity?.let { fileEntity ->
                                                viewModel.openFile(fileEntity)
                                                onFileSelected?.invoke(fileEntity)
                                            }
                                        }
                                    },
                                    onNewFileInFolder = { folderPath ->
                                        newFileParentFolder = folderPath
                                        showNewFileDialog = true
                                    },
                                    onNewSubFolder = { folderPath ->
                                        newFolderParentPath = folderPath
                                        showNewFolderDialog = true
                                    },
                                    onRename = { path, isDir ->
                                        renamingItem = Pair(path, isDir)
                                    },
                                    onDuplicate = { entity ->
                                        duplicatingFile = entity
                                    },
                                    onInspect = { entity ->
                                        inspectingFile = entity
                                    },
                                    onDelete = { path, isDir ->
                                        deletingItem = Pair(path, isDir)
                                    }
                                )
                            }
                        }
                    }
                }

                FileExplorerViewMode.FLAT -> {
                    val flatFiles = remember(files, searchQuery, sortOrder) {
                        var list = files.filter { !it.isDirectory }
                        if (searchQuery.isNotBlank()) {
                            list = list.filter { it.path.contains(searchQuery, true) || it.name.contains(searchQuery, true) }
                        }
                        when (sortOrder) {
                            FileSortOrder.NAME_ASC -> list.sortedBy { it.name.lowercase() }
                            FileSortOrder.NAME_DESC -> list.sortedByDescending { it.name.lowercase() }
                            FileSortOrder.TYPE -> list.sortedBy { it.language }
                            FileSortOrder.RECENTLY_MODIFIED -> list.sortedByDescending { it.updatedAt }
                        }
                    }

                    if (flatFiles.isEmpty()) {
                        EmptyExplorerView(searchQuery = searchQuery)
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 4.dp)
                        ) {
                            items(flatFiles, key = { it.path }) { file ->
                                FlatFileRowItem(
                                    file = file,
                                    isSelected = openedFile?.path == file.path,
                                    onClick = {
                                        viewModel.openFile(file)
                                        onFileSelected?.invoke(file)
                                    },
                                    onRename = { renamingItem = Pair(file.path, false) },
                                    onDuplicate = { duplicatingFile = file },
                                    onInspect = { inspectingFile = file },
                                    onDelete = { deletingItem = Pair(file.path, false) }
                                )
                            }
                        }
                    }
                }

                FileExplorerViewMode.RECENT -> {
                    val recentFiles = remember(files) {
                        files.filter { !it.isDirectory }.sortedByDescending { it.updatedAt }.take(15)
                    }

                    if (recentFiles.isEmpty()) {
                        EmptyExplorerView(searchQuery = "")
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 4.dp)
                        ) {
                            items(recentFiles, key = { it.path }) { file ->
                                FlatFileRowItem(
                                    file = file,
                                    isSelected = openedFile?.path == file.path,
                                    showRelativeTime = true,
                                    onClick = {
                                        viewModel.openFile(file)
                                        onFileSelected?.invoke(file)
                                    },
                                    onRename = { renamingItem = Pair(file.path, false) },
                                    onDuplicate = { duplicatingFile = file },
                                    onInspect = { inspectingFile = file },
                                    onDelete = { deletingItem = Pair(file.path, false) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOGS ---

    // 1. Create New File Dialog
    if (showNewFileDialog) {
        EnhancedNewFileDialog(
            initialDirectory = newFileParentFolder,
            onDismiss = { showNewFileDialog = false },
            onConfirm = { fullPath, initialContent ->
                viewModel.createNewFile(fullPath, initialContent)
                showNewFileDialog = false
                Toast.makeText(context, "Soubor $fullPath vytvořen", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 2. Create New Folder Dialog
    if (showNewFolderDialog) {
        NewFolderModalDialog(
            parentDirectory = newFolderParentPath,
            onDismiss = { showNewFolderDialog = false },
            onConfirm = { folderPath ->
                viewModel.createNewFolder(folderPath)
                expandedFolders = expandedFolders + folderPath
                showNewFolderDialog = false
                Toast.makeText(context, "Složka $folderPath vytvořena", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 3. Rename File / Folder Dialog
    renamingItem?.let { (oldPath, isDir) ->
        RenameModalDialog(
            oldPath = oldPath,
            isDirectory = isDir,
            onDismiss = { renamingItem = null },
            onConfirm = { newPath ->
                viewModel.renameFile(oldPath, newPath)
                renamingItem = null
                Toast.makeText(context, "Přejmenováno na $newPath", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 4. Duplicate File Dialog
    duplicatingFile?.let { sourceFile ->
        DuplicateModalDialog(
            sourceFile = sourceFile,
            onDismiss = { duplicatingFile = null },
            onConfirm = { targetPath ->
                viewModel.duplicateFile(sourceFile, targetPath)
                duplicatingFile = null
                Toast.makeText(context, "Vytvořena kopie $targetPath", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 5. Delete Confirmation Dialog
    deletingItem?.let { (pathToDelete, isDir) ->
        AlertDialog(
            onDismissRequest = { deletingItem = null },
            title = {
                Text(
                    if (isDir) "Smazat složku?" else "Smazat soubor?",
                    color = Slate100,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isDir) {
                        "Opravdu chcete smazat složku '$pathToDelete' včetně veškerého jejího obsahu? Tuto akci nelze vrátit."
                    } else {
                        "Opravdu chcete smazat soubor '$pathToDelete'?"
                    },
                    color = Slate300,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isDir) {
                            viewModel.deleteFolder(pathToDelete)
                        } else {
                            viewModel.deleteFile(pathToDelete)
                        }
                        deletingItem = null
                        Toast.makeText(context, "Položka smazána", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseError)
                ) {
                    Text("Smazat", color = Slate100)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingItem = null }) {
                    Text("Zrušit", color = Slate400)
                }
            },
            containerColor = Slate900
        )
    }

    // 6. File Info Dialog
    inspectingFile?.let { file ->
        FileInfoModalDialog(
            file = file,
            onDismiss = { inspectingFile = null },
            onCopyPath = {
                clipboardManager.setText(AnnotatedString(file.path))
                Toast.makeText(context, "Cesta zkopírována do schránky", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

/**
 * Single Row item in the Hierarchical Tree View.
 */
@Composable
private fun FileTreeRowItem(
    node: FileTreeNode,
    isExpanded: Boolean,
    isSelected: Boolean,
    depth: Int,
    onClick: () -> Unit,
    onNewFileInFolder: (String) -> Unit,
    onNewSubFolder: (String) -> Unit,
    onRename: (String, Boolean) -> Unit,
    onDuplicate: (WorkspaceFileEntity) -> Unit,
    onInspect: (WorkspaceFileEntity) -> Unit,
    onDelete: (String, Boolean) -> Unit
) {
    var showItemMenu by remember { mutableStateOf(false) }

    Surface(
        color = if (isSelected) Slate800 else Color.Transparent,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 1.dp)
            .clickable { onClick() }
            .testTag("file_tree_item_${node.path}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp)
                .padding(start = (depth * 14 + 6).dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Expand / Collapse Arrow for Directories
            if (node.isDirectory) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                    contentDescription = if (isExpanded) "Sbalit" else "Rozbalit",
                    tint = if (isExpanded) AmberWarning else Slate400,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                // Folder Icon
                Icon(
                    imageVector = if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                    contentDescription = null,
                    tint = AmberWarning,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Spacer(modifier = Modifier.width(22.dp))
                // File Type Specific Icon
                val fileIcon = getFileTypeIcon(node.name)
                val iconTint = getFileTypeColor(node.name)
                Icon(
                    imageVector = fileIcon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // File / Folder Name
            Text(
                text = node.name,
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    isSelected -> CyanBright
                    node.isDirectory -> Slate200
                    else -> Slate300
                },
                fontWeight = if (isSelected || node.isDirectory) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Git / Modification Badge
            node.entity?.let { entity ->
                if (entity.gitStatus != "unmodified") {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (entity.gitStatus == "new") EmeraldBright else AmberWarning)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
            }

            // Options menu trigger
            Box {
                IconButton(
                    onClick = { showItemMenu = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Možnosti souboru",
                        tint = Slate500,
                        modifier = Modifier.size(14.dp)
                    )
                }

                DropdownMenu(
                    expanded = showItemMenu,
                    onDismissRequest = { showItemMenu = false },
                    modifier = Modifier.background(Slate900)
                ) {
                    if (node.isDirectory) {
                        DropdownMenuItem(
                            text = { Text("Nový soubor zde...", color = Slate100, fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.NoteAdd, null, tint = CyanBright, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                showItemMenu = false
                                onNewFileInFolder(node.path)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Nová podsložka...", color = Slate100, fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.CreateNewFolder, null, tint = AmberWarning, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                showItemMenu = false
                                onNewSubFolder(node.path)
                            }
                        )
                        HorizontalDivider(color = Slate800)
                    }

                    DropdownMenuItem(
                        text = { Text("Přejmenovat", color = Slate100, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, null, tint = Slate300, modifier = Modifier.size(16.dp)) },
                        onClick = {
                            showItemMenu = false
                            onRename(node.path, node.isDirectory)
                        }
                    )

                    if (!node.isDirectory && node.entity != null) {
                        DropdownMenuItem(
                            text = { Text("Duplikovat", color = Slate100, fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, null, tint = CyanBright, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                showItemMenu = false
                                onDuplicate(node.entity)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Podrobnosti", color = Slate100, fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Info, null, tint = Slate300, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                showItemMenu = false
                                onInspect(node.entity)
                            }
                        )
                    }

                    HorizontalDivider(color = Slate800)
                    DropdownMenuItem(
                        text = { Text("Smazat", color = RoseError, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = RoseError, modifier = Modifier.size(16.dp)) },
                        onClick = {
                            showItemMenu = false
                            onDelete(node.path, node.isDirectory)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Flat File Row Item for List View and Recent Files.
 */
@Composable
private fun FlatFileRowItem(
    file: WorkspaceFileEntity,
    isSelected: Boolean,
    showRelativeTime: Boolean = false,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onInspect: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        color = if (isSelected) Slate800 else Color.Transparent,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getFileTypeIcon(file.name),
                contentDescription = null,
                tint = getFileTypeColor(file.name),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) CyanBright else Slate100,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = file.path,
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate500,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (showRelativeTime) {
                val timeFormat = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
                Text(
                    text = timeFormat.format(Date(file.updatedAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate500,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.MoreVert, null, tint = Slate500, modifier = Modifier.size(14.dp))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, modifier = Modifier.background(Slate900)) {
                    DropdownMenuItem(
                        text = { Text("Přejmenovat", color = Slate100, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, null, tint = Slate300, modifier = Modifier.size(16.dp)) },
                        onClick = { showMenu = false; onRename() }
                    )
                    DropdownMenuItem(
                        text = { Text("Duplikovat", color = Slate100, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, null, tint = CyanBright, modifier = Modifier.size(16.dp)) },
                        onClick = { showMenu = false; onDuplicate() }
                    )
                    DropdownMenuItem(
                        text = { Text("Podrobnosti", color = Slate100, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Info, null, tint = Slate300, modifier = Modifier.size(16.dp)) },
                        onClick = { showMenu = false; onInspect() }
                    )
                    HorizontalDivider(color = Slate800)
                    DropdownMenuItem(
                        text = { Text("Smazat", color = RoseError, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = RoseError, modifier = Modifier.size(16.dp)) },
                        onClick = { showMenu = false; onDelete() }
                    )
                }
            }
        }
    }
}

/**
 * Interactive Breadcrumbs Bar showing current folder and file.
 */
@Composable
fun FileBreadcrumbBar(
    filePath: String,
    onCrumbClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val segments = remember(filePath) { filePath.split('/') }
    var runningPath = ""

    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = null,
            tint = AmberWarning,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))

        segments.forEachIndexed { index, segment ->
            runningPath = if (runningPath.isEmpty()) segment else "$runningPath/$segment"
            val isLast = index == segments.size - 1
            val currentPath = runningPath

            Text(
                text = segment,
                style = MaterialTheme.typography.labelSmall,
                color = if (isLast) CyanBright else Slate400,
                fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                modifier = Modifier.clickable {
                    if (!isLast) onCrumbClick(currentPath)
                }
            )

            if (!isLast) {
                Text(
                    text = " / ",
                    color = Slate600,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }
        }
    }
}

/**
 * Pill tab for switching between Tree, Flat, and Recent view.
 */
@Composable
private fun ExplorerTabPill(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) Slate800 else Slate950,
        shape = RoundedCornerShape(6.dp),
        border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, CyanBright.copy(alpha = 0.5f)) else null,
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) CyanBright else Slate400,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) Slate100 else Slate400,
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

/**
 * Empty state when no matching files exist.
 */
@Composable
private fun EmptyExplorerView(searchQuery: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (searchQuery.isNotBlank()) Icons.Default.SearchOff else Icons.Default.FolderOpen,
                contentDescription = null,
                tint = Slate600,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (searchQuery.isNotBlank()) "Nenalezeny žádné soubory pro '$searchQuery'" else "Žádné soubory v projektu",
                style = MaterialTheme.typography.bodyMedium,
                color = Slate400
            )
        }
    }
}

// --- DIALOG IMPLEMENTATIONS ---

@Composable
fun EnhancedNewFileDialog(
    initialDirectory: String,
    onDismiss: () -> Unit,
    onConfirm: (path: String, initialContent: String) -> Unit
) {
    var fileName by remember { mutableStateOf("NovySoubor.kt") }
    var folderPath by remember { mutableStateOf(initialDirectory.ifEmpty { "src" }) }
    var selectedTemplate by remember { mutableStateOf("Kotlin Třída") }
    var customContent by remember { mutableStateOf("") }

    val templates = listOf(
        "Kotlin Třída" to "package com.example\n\nclass NovySoubor {\n    fun execute() {\n        println(\"Spuštěno!\")\n    }\n}\n",
        "Compose Obrazovka" to "package com.example.ui\n\nimport androidx.compose.runtime.Composable\nimport androidx.compose.material3.*\n\n@Composable\nfun NovaObrazovka() {\n    Text(\"Nová obrazovka\")\n}\n",
        "Python Skript" to "#!/usr/bin/env python3\n\ndef main():\n    print(\"Hello from OpenCode Python engine!\")\n\nif __name__ == '__main__':\n    main()\n",
        "Markdown Dokumentace" to "# Název projektu\n\n## Přehled\nPopis nové komponenty nebo modulu.\n\n- [x] Implementace\n- [ ] Testování\n",
        "JSON Konfigurace" to "{\n  \"name\": \"konfigurace\",\n  \"version\": \"1.0.0\",\n  \"enabled\": true\n}\n",
        "Shell Skript (.sh)" to "#!/bin/bash\nset -e\necho \"Executing build script...\"\n",
        "Prázdný soubor" to ""
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Vytvořit nový soubor", color = Slate100, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Cílová složka:", style = MaterialTheme.typography.labelMedium, color = Slate400)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = folderPath,
                    onValueChange = { folderPath = it },
                    placeholder = { Text("např. src/main nebo scripts", color = Slate500) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Slate950,
                        unfocusedContainerColor = Slate950,
                        focusedBorderColor = CyanBright,
                        unfocusedBorderColor = Slate800
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text("Název souboru:", style = MaterialTheme.typography.labelMedium, color = Slate400)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_file_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Slate950,
                        unfocusedContainerColor = Slate950,
                        focusedBorderColor = CyanBright,
                        unfocusedBorderColor = Slate800
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text("Šablona:", style = MaterialTheme.typography.labelMedium, color = Slate400)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    templates.forEach { (name, content) ->
                        val isSel = selectedTemplate == name
                        Surface(
                            color = if (isSel) CyanBright.copy(alpha = 0.2f) else Slate800,
                            shape = RoundedCornerShape(6.dp),
                            border = if (isSel) androidx.compose.foundation.BorderStroke(1.dp, CyanBright) else null,
                            modifier = Modifier.clickable {
                                selectedTemplate = name
                                customContent = content
                                when (name) {
                                    "Kotlin Třída" -> if (!fileName.endsWith(".kt")) fileName = "NovyModel.kt"
                                    "Compose Obrazovka" -> if (!fileName.endsWith(".kt")) fileName = "NovaObrazovka.kt"
                                    "Python Skript" -> fileName = "script.py"
                                    "Markdown Dokumentace" -> fileName = "README.md"
                                    "JSON Konfigurace" -> fileName = "config.json"
                                    "Shell Skript (.sh)" -> fileName = "task.sh"
                                    else -> {}
                                }
                            }
                        ) {
                            Text(
                                text = name,
                                fontSize = 11.sp,
                                color = if (isSel) CyanBright else Slate300,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanFolder = folderPath.trim().trimEnd('/')
                    val cleanName = fileName.trim().trimStart('/')
                    val fullPath = if (cleanFolder.isNotEmpty()) "$cleanFolder/$cleanName" else cleanName
                    val finalContent = if (customContent.isNotEmpty()) customContent else (templates.find { it.first == selectedTemplate }?.second ?: "")
                    onConfirm(fullPath, finalContent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanBright, contentColor = Slate950)
            ) {
                Text("Vytvořit soubor", fontWeight = FontWeight.Bold)
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
fun NewFolderModalDialog(
    parentDirectory: String,
    onDismiss: () -> Unit,
    onConfirm: (folderPath: String) -> Unit
) {
    var folderName by remember { mutableStateOf("components") }
    var parentFolder by remember { mutableStateOf(parentDirectory) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Vytvořit novou složku", color = Slate100, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Nadřazená složka:", style = MaterialTheme.typography.labelMedium, color = Slate400)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = parentFolder,
                    onValueChange = { parentFolder = it },
                    placeholder = { Text("Kořen projektu (nebo např. src)", color = Slate500) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Slate950,
                        unfocusedContainerColor = Slate950,
                        focusedBorderColor = AmberWarning,
                        unfocusedBorderColor = Slate800
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("Název složky:", style = MaterialTheme.typography.labelMedium, color = Slate400)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_folder_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Slate950,
                        unfocusedContainerColor = Slate950,
                        focusedBorderColor = AmberWarning,
                        unfocusedBorderColor = Slate800
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanParent = parentFolder.trim().trimEnd('/')
                    val cleanName = folderName.trim().trim('/')
                    val fullPath = if (cleanParent.isNotEmpty()) "$cleanParent/$cleanName" else cleanName
                    onConfirm(fullPath)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AmberWarning, contentColor = Slate950)
            ) {
                Text("Vytvořit složku", fontWeight = FontWeight.Bold)
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
fun RenameModalDialog(
    oldPath: String,
    isDirectory: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (newPath: String) -> Unit
) {
    val parentDir = remember(oldPath) { oldPath.substringBeforeLast('/', "") }
    val currentName = remember(oldPath) { oldPath.substringAfterLast('/') }
    var newName by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isDirectory) "Přejmenovat složku" else "Přejmenovat soubor", color = Slate100, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Původní cesta: $oldPath", style = MaterialTheme.typography.labelSmall, color = Slate400, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Nový název:", style = MaterialTheme.typography.labelMedium, color = Slate300)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Slate950,
                        unfocusedContainerColor = Slate950,
                        focusedBorderColor = CyanBright,
                        unfocusedBorderColor = Slate800
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = newName.trim()
                    val targetPath = if (parentDir.isNotEmpty()) "$parentDir/$cleanName" else cleanName
                    onConfirm(targetPath)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanBright, contentColor = Slate950)
            ) {
                Text("Přejmenovat", fontWeight = FontWeight.Bold)
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
fun DuplicateModalDialog(
    sourceFile: WorkspaceFileEntity,
    onDismiss: () -> Unit,
    onConfirm: (targetPath: String) -> Unit
) {
    val parentDir = remember(sourceFile.path) { sourceFile.path.substringBeforeLast('/', "") }
    val baseName = remember(sourceFile.name) {
        val dot = sourceFile.name.lastIndexOf('.')
        if (dot > 0) sourceFile.name.substring(0, dot) else sourceFile.name
    }
    val ext = remember(sourceFile.name) {
        val dot = sourceFile.name.lastIndexOf('.')
        if (dot > 0) sourceFile.name.substring(dot) else ""
    }
    var newName by remember { mutableStateOf("${baseName}_kopie$ext") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Duplikovat soubor", color = Slate100, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Zdrojový soubor: ${sourceFile.path}", style = MaterialTheme.typography.labelSmall, color = Slate400)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Název nového souboru:", style = MaterialTheme.typography.labelMedium, color = Slate300)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Slate950,
                        unfocusedContainerColor = Slate950,
                        focusedBorderColor = CyanBright,
                        unfocusedBorderColor = Slate800
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = newName.trim()
                    val targetPath = if (parentDir.isNotEmpty()) "$parentDir/$cleanName" else cleanName
                    onConfirm(targetPath)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanBright, contentColor = Slate950)
            ) {
                Text("Vytvořit kopii", fontWeight = FontWeight.Bold)
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
fun FileInfoModalDialog(
    file: WorkspaceFileEntity,
    onDismiss: () -> Unit,
    onCopyPath: () -> Unit
) {
    val lineCount = remember(file.content) { file.content.lines().size }
    val charCount = remember(file.content) { file.content.length }
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = CyanBright, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Podrobnosti o souboru", color = Slate100, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoRow(label = "Název", value = file.name)
                InfoRow(label = "Cesta", value = file.path, isMonospace = true)
                InfoRow(label = "Jazyk", value = file.language.uppercase())
                InfoRow(label = "Počet řádků", value = "$lineCount řádků")
                InfoRow(label = "Velikost", value = "$charCount znaků (~${(charCount / 1024f * 10).toInt() / 10f} KB)")
                InfoRow(label = "Git stav", value = file.gitStatus)
                InfoRow(label = "Naposledy upraveno", value = dateFormat.format(Date(file.updatedAt)))

                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onCopyPath,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Slate800, contentColor = CyanBright)
                ) {
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Zkopírovat celou cestu", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Zavřít", color = CyanBright)
            }
        },
        containerColor = Slate900
    )
}

@Composable
private fun InfoRow(label: String, value: String, isMonospace: Boolean = false) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Slate400)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = Slate100,
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
            fontWeight = FontWeight.Medium
        )
    }
}

// --- TREE BUILDING & FLATTENING UTILITIES ---

private fun buildFileTree(files: List<WorkspaceFileEntity>): FileTreeNode {
    val root = FileTreeNode(path = "", name = "root", isDirectory = true)

    files.forEach { file ->
        val parts = file.path.split('/')
        var current = root

        for (i in parts.indices) {
            val part = parts[i]
            val isLast = i == parts.size - 1
            val currentPath = parts.take(i + 1).joinToString("/")

            if (isLast && !file.isDirectory) {
                // Leaf file
                current.children.add(
                    FileTreeNode(
                        path = currentPath,
                        name = part,
                        isDirectory = false,
                        entity = file,
                        depth = i
                    )
                )
            } else {
                // Folder
                var folderNode = current.children.find { it.isDirectory && it.name == part }
                if (folderNode == null) {
                    folderNode = FileTreeNode(
                        path = currentPath,
                        name = part,
                        isDirectory = true,
                        entity = if (isLast) file else null,
                        depth = i
                    )
                    current.children.add(folderNode)
                }
                current = folderNode
            }
        }
    }

    return root
}

private fun flattenTree(
    root: FileTreeNode,
    expandedFolders: Set<String>,
    sortOrder: FileSortOrder
): List<FileTreeNode> {
    val result = mutableListOf<FileTreeNode>()

    fun traverse(node: FileTreeNode) {
        val sortedChildren = when (sortOrder) {
            FileSortOrder.NAME_ASC -> node.children.sortedWith(
                compareBy<FileTreeNode> { !it.isDirectory }.thenBy { it.name.lowercase() }
            )
            FileSortOrder.NAME_DESC -> node.children.sortedWith(
                compareBy<FileTreeNode> { !it.isDirectory }.thenByDescending { it.name.lowercase() }
            )
            FileSortOrder.TYPE -> node.children.sortedWith(
                compareBy<FileTreeNode> { !it.isDirectory }.thenBy { it.entity?.language ?: "" }.thenBy { it.name }
            )
            FileSortOrder.RECENTLY_MODIFIED -> node.children.sortedWith(
                compareBy<FileTreeNode> { !it.isDirectory }.thenByDescending { it.entity?.updatedAt ?: 0L }
            )
        }

        for (child in sortedChildren) {
            result.add(child)
            if (child.isDirectory && expandedFolders.contains(child.path)) {
                traverse(child)
            }
        }
    }

    traverse(root)
    return result
}

private fun collectAllFolderPaths(node: FileTreeNode, destination: MutableSet<String>) {
    if (node.isDirectory && node.path.isNotEmpty()) {
        destination.add(node.path)
    }
    node.children.forEach { collectAllFolderPaths(it, destination) }
}

private fun getFileTypeIcon(fileName: String): ImageVector {
    return when {
        fileName.endsWith(".kt") || fileName.endsWith(".kts") -> Icons.Default.Code
        fileName.endsWith(".py") -> Icons.Default.Terminal
        fileName.endsWith(".json") -> Icons.Default.DataObject
        fileName.endsWith(".xml") -> Icons.Default.Code
        fileName.endsWith(".sql") -> Icons.Default.Storage
        fileName.endsWith(".md") || fileName.endsWith(".txt") -> Icons.Default.Article
        fileName.endsWith(".sh") || fileName.endsWith(".bash") -> Icons.Default.PlayCircleOutline
        fileName.endsWith(".gradle") -> Icons.Default.Build
        fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".webp") -> Icons.Default.Image
        else -> Icons.Default.Description
    }
}

private fun getFileTypeColor(fileName: String): Color {
    return when {
        fileName.endsWith(".kt") || fileName.endsWith(".kts") -> PurpleAccent
        fileName.endsWith(".py") -> AmberWarning
        fileName.endsWith(".json") -> Color(0xFFFB923C) // Orange
        fileName.endsWith(".xml") -> Color(0xFFF43F5E) // Coral / Rose
        fileName.endsWith(".sql") -> CyanBright
        fileName.endsWith(".md") -> Color(0xFF38BDF8) // Sky
        fileName.endsWith(".sh") || fileName.endsWith(".bash") -> EmeraldBright
        fileName.endsWith(".gradle") -> Color(0xFF2DD4BF) // Teal
        else -> Slate400
    }
}
