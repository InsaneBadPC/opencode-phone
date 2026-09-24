package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import com.example.data.models.GitHubActionRun
import com.example.data.models.GitHubPullRequest
import com.example.data.models.GitHubRepository
import com.example.data.models.GitStatusFile
import com.example.ui.OpenCodeViewModel
import com.example.ui.theme.*

@Composable
fun GitHubIntegrationView(
    viewModel: OpenCodeViewModel,
    modifier: Modifier = Modifier
) {
    val isConnected by viewModel.gitHubConnected.collectAsState()
    val gitUser by viewModel.gitHubUser.collectAsState()
    val currentBranch by viewModel.currentGitBranch.collectAsState()
    val availableBranches by viewModel.availableGitBranches.collectAsState()
    val commitMsg by viewModel.gitCommitMessage.collectAsState()
    val repositories by viewModel.gitHubRepositories.collectAsState()
    val statusFiles by viewModel.gitStatusFiles.collectAsState()
    val commitHistory by viewModel.gitCommitHistory.collectAsState()
    val pullRequests by viewModel.gitHubPullRequests.collectAsState()
    val context = LocalContext.current

    var selectedTab by remember { mutableStateOf(0) } // 0 = Změny (Git Status & Commit), 1 = Repozitáře, 2 = Větve & PR, 3 = Historie commitů
    var showNewBranchDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .padding(14.dp)
    ) {
        // GitHub Profile & Connection Status Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF24292E)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Code, contentDescription = "GitHub", tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "GitHub / @$gitUser",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate100
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(EmeraldSuccess, CircleShape)
                                )
                            }
                            Text(
                                text = "Nativní Git SSH & Personal Access Token ověřen",
                                fontSize = 11.sp,
                                color = EmeraldSuccess
                            )
                        }
                    }

                    // Push / Pull Quick Action Group
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalButton(
                            onClick = {
                                viewModel.gitPull { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = Slate800, contentColor = Slate200),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Pull", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.gitPush { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanBright, contentColor = Slate950),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Push", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Active Branch Indicator
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Slate950,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ForkRight, contentDescription = null, tint = CyanBright, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Aktivní větev: ", fontSize = 11.sp, color = Slate400)
                            Text(currentBranch, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyanBright, fontFamily = FontFamily.Monospace)
                        }

                        Text(
                            text = "Repozitář: InsaneBadPC/opencode-phone",
                            fontSize = 11.sp,
                            color = Slate300
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Sub-tabs Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Slate900,
            contentColor = CyanBright
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Změny (${statusFiles.size})", fontSize = 11.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Repozitáře (${repositories.size})", fontSize = 11.sp) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("PR & Větve", fontSize = 11.sp) }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text("Commity", fontSize = 11.sp) }
            )
            Tab(
                selected = selectedTab == 4,
                onClick = { selectedTab = 4 },
                text = { Text("🚀 CI/CD (APK)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        when (selectedTab) {
            0 -> {
                // Git Changes & Commit View
                Column(modifier = Modifier.fillMaxSize()) {
                    // Staging Actions Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val stagedCount = statusFiles.count { it.isStaged }
                        Text(
                            text = "Pracovní strom ($stagedCount z ${statusFiles.size} připraveno)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate300
                        )

                        TextButton(
                            onClick = { viewModel.stageAllFiles() },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Připravit vše (git add .)", color = CyanBright, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(statusFiles) { file ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Slate900),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleStageFile(file.filePath) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Checkbox(
                                            checked = file.isStaged,
                                            onCheckedChange = { viewModel.toggleStageFile(file.filePath) },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = CyanBright,
                                                uncheckedColor = Slate600,
                                                checkmarkColor = Slate950
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = file.filePath,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Slate100,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = if (file.isStaged) "Připraveno ke commitu (Staged)" else "Nepřipraveno (Unstaged)",
                                                fontSize = 10.sp,
                                                color = if (file.isStaged) EmeraldSuccess else Slate400
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (file.status == "MODIFIED") AmberWarning.copy(alpha = 0.2f) else EmeraldSuccess.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = file.status,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (file.status == "MODIFIED") AmberWarning else EmeraldSuccess,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Commit Box
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Slate900,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Vytvořit Git Commit", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate200)

                                TextButton(
                                    onClick = { viewModel.generateAiCommitMessage() },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyanBright, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("AI Zpráva", fontSize = 11.sp, color = CyanBright)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = commitMsg,
                                onValueChange = { viewModel.gitCommitMessage.value = it },
                                placeholder = { Text("Zadejte popis změn (např. feat: přidaná podpora pro skilly)...", fontSize = 11.sp, color = Slate500) },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 2,
                                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = Slate100),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Slate950,
                                    unfocusedContainerColor = Slate950,
                                    focusedBorderColor = CyanBright,
                                    unfocusedBorderColor = Slate800
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    viewModel.commitGitChanges { msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanBright, contentColor = Slate950),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Commitnout změny na '$currentBranch'", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            1 -> {
                // Repositories List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(repositories, key = { it.id }) { repo ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Slate900),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Folder, contentDescription = null, tint = CyanBright, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = repo.fullName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate100
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (repo.isPrivate) AmberWarning.copy(alpha = 0.2f) else Slate800
                                    ) {
                                        Text(
                                            text = if (repo.isPrivate) "Private" else "Public",
                                            fontSize = 10.sp,
                                            color = if (repo.isPrivate) AmberWarning else Slate300,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = repo.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate300,
                                    fontSize = 12.sp
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("⭐ ${repo.starsCount} • ${repo.updatedAt}", fontSize = 11.sp, color = Slate400)

                                    Button(
                                        onClick = {
                                            viewModel.cloneGitHubRepo(repo) { msg ->
                                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Slate800, contentColor = CyanBright),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Klonovat do workspace", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Branches & Pull Requests
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Branch Selector Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Slate900),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Větve projektu", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Slate100)

                                TextButton(
                                    onClick = { showNewBranchDialog = true },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = CyanBright, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Nová větev", color = CyanBright, fontSize = 11.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            availableBranches.forEach { branch ->
                                val isSelected = branch == currentBranch
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) Slate800 else Slate950,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .clickable { viewModel.switchGitBranch(branch) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.ForkRight,
                                                contentDescription = null,
                                                tint = if (isSelected) CyanBright else Slate500,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = branch,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) CyanBright else Slate300,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }

                                        if (isSelected) {
                                            Text("Aktivní", fontSize = 10.sp, color = EmeraldSuccess, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Pull Requests
                    Text("Pull Requesty (${pullRequests.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate300)

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(pullRequests) { pr ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Slate900),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "#${pr.number} ${pr.title}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate100
                                        )

                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (pr.status == "OPEN") EmeraldSuccess.copy(alpha = 0.2f) else Slate800
                                        ) {
                                            Text(
                                                text = pr.status,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (pr.status == "OPEN") EmeraldSuccess else Slate400,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Větev: ${pr.branch} • Autor: ${pr.author} • Komentáře: ${pr.commentsCount}",
                                        fontSize = 10.sp,
                                        color = Slate400
                                    )
                                }
                            }
                        }
                    }
                }
            }

            3 -> {
                // Commit History
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(commitHistory) { commit ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Slate900),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = commit.message,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate100
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Slate800
                                    ) {
                                        Text(
                                            text = commit.hash,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = CyanBright,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Commitoval ${commit.author} • ${commit.timeAgo}",
                                    fontSize = 10.sp,
                                    color = Slate400
                                )
                            }
                        }
                    }
                }
            }

            4 -> {
                GitHubActionsApkBuildView(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (showNewBranchDialog) {
        var branchName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewBranchDialog = false },
            title = { Text("Vytvořit novou Git větev", color = Slate100, fontSize = 16.sp) },
            text = {
                OutlinedTextField(
                    value = branchName,
                    onValueChange = { branchName = it },
                    label = { Text("Název větve (např. feature/nova-funkce)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (branchName.isNotBlank()) {
                            viewModel.createGitBranch(branchName) { msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                            showNewBranchDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanBright, contentColor = Slate950)
                ) {
                    Text("Vytvořit větev")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewBranchDialog = false }) {
                    Text("Zrušit", color = Slate400)
                }
            },
            containerColor = Slate900
        )
    }
}

@Composable
fun GitHubActionsApkBuildView(
    viewModel: OpenCodeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isRunning by viewModel.isWorkflowRunning.collectAsState()
    val activeStep by viewModel.activeWorkflowStep.collectAsState()
    val logs by viewModel.workflowExecutionLogs.collectAsState()
    val workflowRuns by viewModel.gitHubWorkflowRuns.collectAsState()
    val yamlContent by viewModel.workflowYamlContent.collectAsState()
    val currentBranch by viewModel.currentGitBranch.collectAsState()

    var selectedBuildType by remember { mutableStateOf("debug") }
    var isYamlExpanded by remember { mutableStateOf(false) }

    val buildSteps = listOf(
        "1. Checkout kódu",
        "2. JDK 17 & SDK",
        "3. .env & Keystore",
        "4. ./gradlew assemble",
        "5. Upload APK artefaktu"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. CI/CD Header banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanBright.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(CyanBright.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = CyanBright, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "GitHub Actions: APK Build CI/CD",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .background(EmeraldSuccess, CircleShape)
                                    )
                                }
                                Text(
                                    text = "Automatická kompilace APK při pushi na GitHub & manuální dispatch",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate400,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Tags row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        WorkflowBadge(text = ".github/workflows/build-apk.yml", color = CyanBright)
                        WorkflowBadge(text = "Temurin JDK 17", color = Slate300)
                        WorkflowBadge(text = "Gradle 9.3.1", color = Slate300)
                        WorkflowBadge(text = "Android SDK 36", color = EmeraldSuccess)
                    }
                }
            }
        }

        // 2. Dispatch / Manual trigger card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isRunning) CyanBright else Slate800),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚡ Spustit workflow (workflow_dispatch)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Slate100
                        )

                        Text(
                            text = "Větev: $currentBranch",
                            fontSize = 11.sp,
                            color = CyanBright,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Variant choice
                    Text("Vyberte variantu sestavení (Build Variant):", fontSize = 11.sp, color = Slate400)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("debug" to "Debug APK", "release" to "Release APK", "both" to "Oba balíčky").forEach { (variant, label) ->
                            val isSelected = selectedBuildType == variant
                            FilterChip(
                                selected = isSelected,
                                onClick = { if (!isRunning) selectedBuildType = variant },
                                label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanBright.copy(alpha = 0.2f),
                                    selectedLabelColor = CyanBright,
                                    containerColor = Slate950,
                                    labelColor = Slate400
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) CyanBright else Slate800
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Progress steps if running
                    if (isRunning) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            color = CyanBright,
                            trackColor = Slate800
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Steps visualizer
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            buildSteps.forEachIndexed { i, sName ->
                                val isDone = activeStep > i + 1
                                val isCurr = activeStep == i + 1
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(
                                                if (isDone) EmeraldSuccess else if (isCurr) CyanBright else Slate800,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isDone) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Slate950, modifier = Modifier.size(12.dp))
                                        } else {
                                            Text(
                                                "${i + 1}",
                                                fontSize = 10.sp,
                                                color = if (isCurr) Slate950 else Slate400,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = sName.substringAfter(". "),
                                        fontSize = 8.sp,
                                        color = if (isCurr) CyanBright else if (isDone) EmeraldSuccess else Slate500,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Terminal Logs Console
                    if (logs.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Slate950,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(6.dp).background(if (isRunning) AmberBright else EmeraldSuccess, CircleShape))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Konzole běhu akce (Runner Output)", fontSize = 10.sp, color = Slate400)
                                    }
                                    Text(if (isRunning) "Kompiluji..." else "Dokončeno", fontSize = 9.sp, color = if (isRunning) AmberBright else EmeraldSuccess)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                logs.takeLast(4).forEach { l ->
                                    Text(
                                        text = l,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = if (l.contains("Hotovo")) EmeraldBright else Slate300
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Trigger button
                    Button(
                        onClick = {
                            viewModel.triggerGitHubApkWorkflow(
                                buildType = selectedBuildType,
                                onProgress = { pLog -> Toast.makeText(context, pLog, Toast.LENGTH_SHORT).show() },
                                onFinished = { fMsg -> Toast.makeText(context, fMsg, Toast.LENGTH_LONG).show() }
                            )
                        },
                        enabled = !isRunning,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanBright,
                            contentColor = Slate950
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("trigger_action_button")
                    ) {
                        Icon(
                            if (isRunning) Icons.Default.HourglassTop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isRunning) "Sestavuji APK přes GitHub Action..." else "🚀 Spustit sestavení APK (${selectedBuildType.uppercase()})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. Workflow YAML preview & Copy card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = CyanBright, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Konfigurace .github/workflows/build-apk.yml",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate100
                                )
                                Text("Aktivní v git repozitáři", fontSize = 10.sp, color = EmeraldSuccess)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val clip = ClipData.newPlainText("GitHub Actions Workflow", yamlContent)
                                    clipboard?.setPrimaryClip(clip)
                                    Toast.makeText(context, "YAML konfigurace zkopírována do schránky!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Kopírovat", tint = CyanBright, modifier = Modifier.size(16.dp))
                            }

                            IconButton(
                                onClick = { isYamlExpanded = !isYamlExpanded },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isYamlExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = Slate400,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = isYamlExpanded) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Slate950,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = yamlContent,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = Slate300,
                                    lineHeight = 13.sp,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Past Workflow Runs & Downloadable Artifacts
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Historie běhů & Artefakty APK (${workflowRuns.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate300
                )
                Text(
                    text = "Uloženo v GitHub Artifacts",
                    fontSize = 10.sp,
                    color = Slate500
                )
            }
        }

        items(workflowRuns) { run ->
            WorkflowRunItemCard(
                run = run,
                onDownload = {
                    Toast.makeText(
                        context,
                        "Stahování artefaktu: ${run.artifactName} (${run.artifactSizeMb} MB) zahájeno!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }
}

@Composable
fun WorkflowRunItemCard(
    run: GitHubActionRun,
    onDownload: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(EmeraldSuccess.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(14.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "#${run.runNumber} ${run.workflowName}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate100
                        )
                        Text(
                            text = "Větev: ${run.branch} • Commit: ${run.commitHash} • ${run.duration}",
                            fontSize = 10.sp,
                            color = Slate400
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = EmeraldSuccess.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "SUCCESS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldSuccess,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (!run.artifactName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Slate950, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Android, contentDescription = null, tint = EmeraldBright, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = run.artifactName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Velikost: ${run.artifactSizeMb ?: 28.0} MB • Připraveno k instalaci",
                                fontSize = 9.sp,
                                color = Slate400
                            )
                        }
                    }

                    Button(
                        onClick = onDownload,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldSuccess,
                            contentColor = Slate950
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stáhnout", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun WorkflowBadge(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Slate950,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Slate800)
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            color = color,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

