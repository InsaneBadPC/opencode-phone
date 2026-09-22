package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ProjectsDialog
import com.example.ui.components.SessionsDialog
import com.example.ui.components.UpdateDialog
import com.example.ui.screens.*
import com.example.ui.theme.*

data class NavigationTab(
    val title: String,
    val icon: ImageVector,
    val testTag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: OpenCodeViewModel,
    modifier: Modifier = Modifier
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val activeProject by viewModel.activeProject.collectAsState()
    val allSessions by viewModel.sessions.collectAsState()

    var showProjectsDialog by remember { mutableStateOf(false) }
    var showSessionsDialog by remember { mutableStateOf(false) }

    if (showProjectsDialog) {
        ProjectsDialog(
            viewModel = viewModel,
            onDismissRequest = { showProjectsDialog = false }
        )
    }

    if (showSessionsDialog) {
        SessionsDialog(
            viewModel = viewModel,
            onDismissRequest = { showSessionsDialog = false }
        )
    }

    UpdateDialog(viewModel = viewModel)

    val tabs = listOf(
        NavigationTab("Chat", Icons.Default.ChatBubble, "nav_tab_chat"),
        NavigationTab("Soubory", Icons.Default.Folder, "nav_tab_files"),
        NavigationTab("Tržiště", Icons.Default.Storefront, "nav_tab_market"),
        NavigationTab("Internet", Icons.Default.Public, "nav_tab_internet"),
        NavigationTab("YouTube", Icons.Default.PlayCircle, "nav_tab_youtube"),
        NavigationTab("Dovednosti", Icons.Default.Extension, "nav_tab_skills"),
        NavigationTab("Nástroje", Icons.Default.Handyman, "nav_tab_plugins")
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = null,
                                tint = CyanBright,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "OpenCode",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate100
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Slate800,
                                modifier = Modifier
                                    .clickable { viewModel.checkForUpdates() }
                                    .testTag("top_version_badge")
                            ) {
                                Text(
                                    text = "v${viewModel.appVersionName}",
                                    fontSize = 9.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = CyanBright,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Project Selector Chip
                            FilledTonalButton(
                                onClick = { showProjectsDialog = true },
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Slate800, contentColor = Slate200),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("top_project_chip")
                            ) {
                                Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = CyanBright, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = activeProject?.name?.take(10) ?: "Projekt",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Slate100
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Slate400, modifier = Modifier.size(14.dp))
                            }

                            // Sessions Selector Chip
                            FilledTonalButton(
                                onClick = { showSessionsDialog = true },
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Slate800, contentColor = Slate200),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("top_sessions_chip")
                            ) {
                                Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = CyanBright, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${allSessions.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate100
                                )
                            }

                            // YouTube Agent Button
                            FilledTonalButton(
                                onClick = { viewModel.selectTab(4) },
                                colors = if (currentTab == 4) {
                                    ButtonDefaults.filledTonalButtonColors(containerColor = androidx.compose.ui.graphics.Color(0xFFFF0033), contentColor = androidx.compose.ui.graphics.Color.White)
                                } else {
                                    ButtonDefaults.filledTonalButtonColors(containerColor = Slate800, contentColor = androidx.compose.ui.graphics.Color(0xFFFF4D4D))
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("top_youtube_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("YT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Marketplace Button
                            FilledTonalButton(
                                onClick = { viewModel.openMarketplace("Vše") },
                                colors = if (currentTab == 2) {
                                    ButtonDefaults.filledTonalButtonColors(containerColor = CyanBright, contentColor = Slate950)
                                } else {
                                    ButtonDefaults.filledTonalButtonColors(containerColor = Slate800, contentColor = CyanBright)
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("top_market_button")
                            ) {
                                Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Trh", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Slate900,
                    titleContentColor = Slate100
                ),
                windowInsets = WindowInsets.statusBars
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Slate900,
                tonalElevation = 6.dp,
                windowInsets = WindowInsets.navigationBars,
                modifier = Modifier.testTag("main_bottom_nav")
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = currentTab == index,
                        onClick = { viewModel.selectTab(index) },
                        alwaysShowLabel = false,
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 9.sp,
                                maxLines = 1,
                                fontWeight = if (currentTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Slate950,
                            selectedTextColor = if (index == 4) androidx.compose.ui.graphics.Color(0xFFFF0033) else CyanBright,
                            indicatorColor = if (index == 4) androidx.compose.ui.graphics.Color(0xFFFF4D4D) else CyanBright,
                            unselectedIconColor = Slate400,
                            unselectedTextColor = Slate400
                        ),
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> ChatScreen(viewModel = viewModel)
                1 -> FilesScreen(viewModel = viewModel)
                2 -> MarketplaceScreen(viewModel = viewModel)
                3 -> InternetScreen(viewModel = viewModel)
                4 -> YouTubeAgentScreen(viewModel = viewModel)
                5 -> SkillsMcpScreen(viewModel = viewModel)
                6 -> PluginsSettingsScreen(viewModel = viewModel)
            }
        }
    }
}
