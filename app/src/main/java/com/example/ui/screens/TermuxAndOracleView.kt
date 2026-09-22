package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.OracleCloudHost
import com.example.data.models.TerminalExecutionTarget
import com.example.data.models.TermuxInstalledPackage
import com.example.data.models.TermuxSyncState
import com.example.ui.OpenCodeViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TermuxAndOracleView(
    viewModel: OpenCodeViewModel,
    modifier: Modifier = Modifier
) {
    val executionTarget by viewModel.terminalTarget.collectAsState()
    val packages by viewModel.termuxPackages.collectAsState()
    val oracleHost by viewModel.oracleCloudHost.collectAsState()
    val terminalHistory by viewModel.terminalHistory.collectAsState()
    val terminalCmd by viewModel.terminalInput.collectAsState()
    val context = LocalContext.current

    var selectedTab by remember { mutableStateOf(0) } // 0 = Terminál, 1 = Správce balíčků (pkg/apt/pip), 2 = Oracle Cloud SSH

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .padding(14.dp)
    ) {
        // Mode & Environment Switcher
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Slate900,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cílové prostředí vykonávání:",
                        style = MaterialTheme.typography.titleSmall,
                        color = Slate300,
                        fontSize = 11.sp
                    )
                    Badge(
                        containerColor = when (executionTarget) {
                            TerminalExecutionTarget.LOCAL_TERMUX -> EmeraldSuccess
                            TerminalExecutionTarget.ORACLE_CLOUD_OCI -> AmberWarning
                            TerminalExecutionTarget.DOCKER_CONTAINER -> CyanBright
                        }
                    ) {
                        Text(
                            text = when (executionTarget) {
                                TerminalExecutionTarget.LOCAL_TERMUX -> "LOKÁLNÍ TERMUX"
                                TerminalExecutionTarget.ORACLE_CLOUD_OCI -> "ORACLE CLOUD VM"
                                TerminalExecutionTarget.DOCKER_CONTAINER -> "DEV DOCKER"
                            },
                            color = Slate950,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = executionTarget == TerminalExecutionTarget.LOCAL_TERMUX,
                        onClick = { viewModel.setTerminalExecutionTarget(TerminalExecutionTarget.LOCAL_TERMUX) },
                        label = { Text("📱 Lokální Termux", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = executionTarget == TerminalExecutionTarget.ORACLE_CLOUD_OCI,
                        onClick = { viewModel.setTerminalExecutionTarget(TerminalExecutionTarget.ORACLE_CLOUD_OCI) },
                        label = { Text("☁️ Oracle Cloud", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = executionTarget == TerminalExecutionTarget.DOCKER_CONTAINER,
                        onClick = { viewModel.setTerminalExecutionTarget(TerminalExecutionTarget.DOCKER_CONTAINER) },
                        label = { Text("🐳 Docker", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Subtabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Slate900,
            contentColor = CyanBright
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("💻 Konzole", fontSize = 11.sp) },
                icon = { Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("📦 Balíčky (pkg/apt)", fontSize = 11.sp) },
                icon = { Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("⚡ OCI Instance", fontSize = 11.sp) },
                icon = { Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text("🔄 CLI Sync", fontSize = 11.sp) },
                icon = { Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        when (selectedTab) {
            0 -> {
                // Interactive Shell
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF070B14)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .border(1.dp, Slate800, RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(terminalHistory) { line ->
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = when (executionTarget) {
                                                TerminalExecutionTarget.LOCAL_TERMUX -> "termux@localhost:~$ "
                                                TerminalExecutionTarget.ORACLE_CLOUD_OCI -> "opc@oracle-vm-frankfurt:~$ "
                                                TerminalExecutionTarget.DOCKER_CONTAINER -> "root@docker-container:/# "
                                            },
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldBright
                                        )
                                        Text(
                                            text = line.command,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = Slate100
                                        )
                                    }
                                    if (line.output.isNotBlank()) {
                                        Text(
                                            text = line.output,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = if (line.exitCode == 0) Slate300 else RoseError,
                                            modifier = Modifier.padding(start = 12.dp, top = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = terminalCmd,
                                onValueChange = { viewModel.updateTerminalInput(it) },
                                placeholder = { Text("Zadejte příkaz (např. pkg install htop, oci compute...)", color = Slate600, fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Slate100),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Slate900,
                                    unfocusedContainerColor = Slate900,
                                    focusedBorderColor = CyanBright,
                                    unfocusedBorderColor = Slate700
                                )
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            Button(
                                onClick = { viewModel.runTerminalCommand() },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanBright, contentColor = Slate950),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Spustit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            1 -> {
                // Package Manager (Termux pkg / pip / npm)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Dostupné a nainstalované balíčky prostředí", style = MaterialTheme.typography.titleMedium, color = Slate100)
                    Text("Instalujte kompilátory (clang, rust), runtime prostředí (python, nodejs) nebo nástroje přímo do prostředí.", fontSize = 12.sp, color = Slate400)

                    packages.forEach { pkg ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Slate900),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(pkg.name, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = CyanBright, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Badge(containerColor = Slate800) {
                                            Text(pkg.category, fontSize = 9.sp, color = Slate300)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Verze: ${pkg.version} • Velikost: ${pkg.sizeMb} MB", fontSize = 11.sp, color = Slate400)
                                }

                                if (pkg.isInstalled) {
                                    AssistChip(
                                        onClick = {},
                                        label = { Text("Nainstalováno", fontSize = 10.sp, color = EmeraldSuccess) },
                                        leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(14.dp)) }
                                    )
                                } else {
                                    Button(
                                        onClick = {
                                            viewModel.installTermuxPackage(pkg.name) { msg ->
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = Slate950),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Instalovat", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Oracle Cloud Host Configuration
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Slate900),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Oracle Cloud Infrastructure (OCI)", style = MaterialTheme.typography.titleMedium, color = Slate100)
                                    Text("Propojeno s Always Free Compute instancí", style = MaterialTheme.typography.bodySmall, color = Slate400)
                                }
                                Badge(containerColor = EmeraldSuccess) {
                                    Text("SSH PŘIPOJENO", color = Slate950, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text("Veřejná IP adresa: ${oracleHost.hostIp}", fontFamily = FontFamily.Monospace, color = CyanBright, fontSize = 12.sp)
                            Text("Region: ${oracleHost.region}", fontSize = 12.sp, color = Slate300)
                            Text("Uživatelské jméno: ${oracleHost.username}", fontSize = 12.sp, color = Slate300)
                            Text("Typ instance: ${oracleHost.instanceType}", fontSize = 12.sp, color = Slate300)

                            Spacer(modifier = Modifier.height(14.dp))

                            FilledTonalButton(
                                onClick = {
                                    viewModel.setTerminalExecutionTarget(TerminalExecutionTarget.ORACLE_CLOUD_OCI)
                                    viewModel.updateTerminalInput("uname -a && free -h")
                                    viewModel.runTerminalCommand()
                                    selectedTab = 0
                                },
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Slate800, contentColor = CyanBright),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Otevřít vzdálenou SSH relaci")
                            }
                        }
                    }
                }
            }
            3 -> {
                TermuxCliSyncSubView(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun TermuxCliSyncSubView(
    viewModel: OpenCodeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val syncConfig by viewModel.termuxSyncConfig.collectAsState()
    val syncStatus by viewModel.termuxSyncStatus.collectAsState()
    val syncLogs by viewModel.termuxSyncLogs.collectAsState()
    val allSessions by viewModel.sessions.collectAsState()

    val termuxSessions = remember(allSessions) {
        allSessions.filter { it.projectId == "proj_termux_cli" || it.title.contains("Termux", ignoreCase = true) }
    }

    var portInput by remember { mutableStateOf(syncConfig.port.toString()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("termux_cli_sync_view"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Status & Overview Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (syncConfig.isAutoSyncEnabled) EmeraldSuccess.copy(alpha = 0.5f) else Slate800
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (syncConfig.isAutoSyncEnabled) EmeraldSuccess else Slate500)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "OpenCode CLI v Termuxu",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate100
                            )
                        }

                        Badge(
                            containerColor = when (syncStatus.state) {
                                TermuxSyncState.SYNCING -> AmberWarning
                                TermuxSyncState.CONNECTED -> EmeraldSuccess
                                else -> Slate700
                            }
                        ) {
                            Text(
                                text = when (syncStatus.state) {
                                    TermuxSyncState.SYNCING -> "SYNCHRONIZUJI..."
                                    TermuxSyncState.CONNECTED -> "PŘIPOJENO"
                                    TermuxSyncState.OFFLINE -> "OFFLINE"
                                    TermuxSyncState.ERROR -> "CHYBA"
                                    TermuxSyncState.IDLE -> "PŘIPRAVENO"
                                },
                                color = Slate950,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Automatická obousměrná synchronizace relací mezi terminálem Termux a Android grafickým IDE. Zprávy a příkazy zadané v CLI se okamžitě zobrazují zde a naopak.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate300,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Metrics Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Slate950,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Slate800),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("DAEMON PORT", fontSize = 9.sp, color = Slate400, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("127.0.0.1:${syncConfig.port}", fontSize = 11.sp, color = CyanBright, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Slate950,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Slate800),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("TERMUX SESSIONS", fontSize = 9.sp, color = Slate400, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("${termuxSessions.size} relací", fontSize = 11.sp, color = EmeraldBright, fontWeight = FontWeight.Bold)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Slate950,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Slate800),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("INTERVAL", fontSize = 9.sp, color = Slate400, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("${syncConfig.syncIntervalSeconds} s", fontSize = 11.sp, color = AmberWarning, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.triggerTermuxSyncNow()
                                Toast.makeText(context, "Spuštěna synchronizace z Termuxu...", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = Slate950),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_trigger_termux_sync")
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Synchronizovat nyní", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.exportCurrentSessionToTermux()
                                Toast.makeText(context, "Exportováno do Termux CLI (~/.opencode)", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate200),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export chatu do CLI", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Auto-Sync Configuration Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Konfigurace automatického zrcadlení",
                        fontWeight = FontWeight.Bold,
                        color = Slate100,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Automatická synchronizace na pozadí", fontSize = 12.sp, color = Slate200, fontWeight = FontWeight.Medium)
                            Text("Pravidelně dotazuje lokální Termux daemon na nové relace a zprávy", fontSize = 10.sp, color = Slate400)
                        }
                        Switch(
                            checked = syncConfig.isAutoSyncEnabled,
                            onCheckedChange = { viewModel.toggleTermuxAutoSync(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Frekvence automatické kontroly:", fontSize = 11.sp, color = Slate300)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(10, 30, 60, 120).forEach { seconds ->
                            FilterChip(
                                selected = syncConfig.syncIntervalSeconds == seconds,
                                onClick = { viewModel.setTermuxSyncInterval(seconds) },
                                label = { Text("${seconds}s", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = portInput,
                            onValueChange = {
                                portInput = it
                                it.toIntOrNull()?.let { p -> viewModel.setTermuxSyncPort(p) }
                            },
                            label = { Text("Port daemonu", fontSize = 10.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanBright,
                                unfocusedBorderColor = Slate700,
                                focusedTextColor = Slate100,
                                unfocusedTextColor = Slate100
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )

                        FilledTonalButton(
                            onClick = {
                                portInput = "4096"
                                viewModel.setTermuxSyncPort(4096)
                                Toast.makeText(context, "Nastaven výchozí port 4096", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = Slate800, contentColor = CyanBright),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Výchozí (4096)", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Setup Commands for Termux Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
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
                            Icon(Icons.Default.Terminal, contentDescription = null, tint = EmeraldBright, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Příkaz pro spuštění v Termuxu", fontWeight = FontWeight.Bold, color = Slate100, fontSize = 12.sp)
                        }

                        val termuxCommand = "pkg install nodejs -y && npm install -g opencode-cli && opencode daemon --sync --port ${syncConfig.port}"
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(termuxCommand))
                                Toast.makeText(context, "Příkaz zkopírován do schránky", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Kopírovat", tint = CyanBright, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF070B14),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Slate800),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "pkg install nodejs -y\nnpm install -g opencode-cli\nopencode daemon --sync --port ${syncConfig.port}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = EmeraldBright,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }

        // Real-time Sync Event Logs
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF070B14)),
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
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = CyanBright, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Žurnál synchronizace v reálném čase", fontWeight = FontWeight.Bold, color = Slate200, fontSize = 12.sp)
                        }
                        Text("${syncLogs.size} záznamů", fontSize = 10.sp, color = Slate400)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        syncLogs.forEach { logLine ->
                            Text(
                                text = logLine,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = if (logLine.contains("✓")) EmeraldBright
                                else if (logLine.contains("ℹ")) CyanBright
                                else Slate400
                            )
                        }
                    }
                }
            }
        }

        // Synced Termux Sessions List
        if (termuxSessions.isNotEmpty()) {
            item {
                Text(
                    text = "Synchronizované relace z Termuxu (${termuxSessions.size}):",
                    fontWeight = FontWeight.Bold,
                    color = Slate200,
                    fontSize = 12.sp
                )
            }

            items(termuxSessions, key = { it.id }) { session ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Slate900,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.selectSession(session.id)
                            viewModel.selectTab(0) // Switch to chat
                            Toast.makeText(context, "Otevřena relace: ${session.title}", Toast.LENGTH_SHORT).show()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Terminal, contentDescription = null, tint = EmeraldBright, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(session.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate100)
                                Text("Termux: ~/workspace • Model: ${session.modelName}", fontSize = 10.sp, color = Slate400)
                            }
                        }

                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Slate500, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
