package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.youtube.*
import com.example.ui.OpenCodeViewModel
import com.example.ui.theme.*

enum class VideoFilterCategory(val label: String) {
    ALL("Všechna videa"),
    LONG("Dlouhá videa"),
    SHORTS("Shorts"),
    LOW_CTR("⚠️ Slabé CTR (< 5%)"),
    TOP_PERFORMING("🚀 Top výkon")
}

@Composable
fun YouTubeChannelContentView(
    viewModel: OpenCodeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val connectedChannel by viewModel.connectedYouTubeChannel.collectAsStateWithLifecycle()
    val videos by viewModel.channelVideos.collectAsStateWithLifecycle()
    val isConnecting by viewModel.isConnectingYouTubeChannel.collectAsStateWithLifecycle()
    val connectionError by viewModel.youtubeConnectionError.collectAsStateWithLifecycle()
    val selectedVideoForAudit by viewModel.selectedVideoForAudit.collectAsStateWithLifecycle()
    val auditResult by viewModel.videoAuditResult.collectAsStateWithLifecycle()
    val isAuditing by viewModel.isAuditingVideo.collectAsStateWithLifecycle()

    var showConnectDialog by remember { mutableStateOf(false) }
    var showAddVideoDialog by remember { mutableStateOf(false) }
    var showEditChannelDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(VideoFilterCategory.ALL) }

    val youtubeRed = Color(0xFFFF0033)

    // Filter videos based on selection
    val filteredVideos = remember(videos, selectedFilter) {
        when (selectedFilter) {
            VideoFilterCategory.ALL -> videos
            VideoFilterCategory.LONG -> videos.filter { !it.isShort }
            VideoFilterCategory.SHORTS -> videos.filter { it.isShort }
            VideoFilterCategory.LOW_CTR -> videos.filter { it.ctrPercent < 5.0f }
            VideoFilterCategory.TOP_PERFORMING -> videos.sortedByDescending { it.viewCount }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("youtube_channel_content_view")
    ) {
        if (connectedChannel == null) {
            // NOT CONNECTED STATE: Dedicated Connect Card
            NotConnectedOnboardingCard(
                onConnectClick = { showConnectDialog = true },
                userEmail = "p.p.lukes892@gmail.com"
            )
        } else {
            val channel = connectedChannel!!

            // CONNECTED STATE: Channel Profile Header
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = BorderStroke(1.dp, Slate800),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            // Avatar
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Slate800)
                                    .border(2.dp, youtubeRed, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = channel.avatarUrl,
                                    contentDescription = "Channel Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = channel.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate100
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Ověřený účet",
                                        tint = CyanBright,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Text(
                                    text = "${channel.handle} • ${channel.channelEmail ?: "Google Account"}",
                                    fontSize = 11.sp,
                                    color = Slate400
                                )

                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = EmeraldSuccess.copy(alpha = 0.15f),
                                    border = BorderStroke(0.5.dp, EmeraldSuccess),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text(
                                        text = "● ${channel.authType.label}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldBright,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Edit Channel Info
                            IconButton(
                                onClick = { showEditChannelDialog = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Slate800)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Upravit údaje kanálu",
                                    tint = Slate200,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Disconnect button
                            IconButton(
                                onClick = {
                                    viewModel.disconnectYouTubeChannel()
                                    Toast.makeText(context, "Kanál byl odpojen", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Slate800)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = "Odpojit kanál",
                                    tint = Color(0xFFFF6B6B),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Channel Statistics Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatTile(
                            label = "Odběratelé",
                            value = "${channel.subscriberCount}",
                            sub = "Aktivní komunita",
                            subColor = EmeraldBright,
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            label = "Celková zhlédnutí",
                            value = "${channel.viewCount}",
                            sub = "${videos.size} videí v appce",
                            subColor = Slate400,
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            label = "Zdraví kanálu",
                            value = "94%",
                            sub = "Připraveno pro růst",
                            subColor = CyanBright,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action buttons (Add Video, Refresh, Edit)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showAddVideoDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = youtubeRed),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("➕ Přidat mé video", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.refreshYouTubeVideos() },
                            enabled = !isConnecting,
                            colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp), tint = Slate200)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Obnovit", fontSize = 11.sp, color = Slate200)
                        }

                        OutlinedButton(
                            onClick = { showConnectDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Slate700),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(15.dp), tint = Slate300)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Změnit", fontSize = 11.sp, color = Slate300)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section: My Content & Videos Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = youtubeRed, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Váš nahraný obsah (${videos.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate100
                    )
                }

                TextButton(onClick = { showAddVideoDialog = true }) {
                    Text("+ Přidat video", color = CyanBright, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(VideoFilterCategory.values()) { category ->
                    FilterChip(
                        selected = selectedFilter == category,
                        onClick = { selectedFilter = category },
                        label = { Text(category.label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (category == VideoFilterCategory.LOW_CTR) AmberWarning.copy(alpha = 0.25f) else Slate800,
                            selectedLabelColor = Color.White,
                            containerColor = Slate900,
                            labelColor = Slate400
                        ),
                        border = if (selectedFilter == category) BorderStroke(1.dp, Slate600) else null
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredVideos.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Slate900,
                    border = BorderStroke(1.dp, Slate800),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.PlayCircleOutline, contentDescription = null, tint = Slate500, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Zatím zde nejsou žádná videa pro tento filtr",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate200
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Klikněte na „Přidat mé video“ pro vložení odkazu nebo názvu vašeho videa z YouTube.",
                            fontSize = 11.sp,
                            color = Slate400,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { showAddVideoDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = youtubeRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Přidat mé video z YouTube")
                        }
                    }
                }
            } else {
                // Videos List
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredVideos, key = { it.id }) { video ->
                        VideoItemCard(
                            video = video,
                            onAuditClick = { viewModel.startVideoAudit(video) },
                            onDeleteClick = {
                                viewModel.deleteCustomVideo(video.id)
                                Toast.makeText(context, "Video odstraněno ze seznamu", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }

    // CONNECT CHANNEL DIALOG
    if (showConnectDialog) {
        ConnectChannelDialog(
            currentEmail = "p.p.lukes892@gmail.com",
            onDismiss = { showConnectDialog = false },
            onConnect = { query, authType, apiKey, customTitle, customSubscribers, customCategory ->
                viewModel.connectYouTubeChannel(
                    query = query,
                    authType = authType,
                    apiKey = apiKey,
                    userEmail = "p.p.lukes892@gmail.com",
                    customTitle = customTitle,
                    customSubscribers = customSubscribers,
                    customCategory = customCategory
                )
                showConnectDialog = false
                Toast.makeText(context, "Kanál úspěšně připojen!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // ADD CUSTOM VIDEO DIALOG
    if (showAddVideoDialog) {
        AddCustomVideoDialog(
            onDismiss = { showAddVideoDialog = false },
            onAdd = { title, url, duration, isShort, views, likes, ctr, tags ->
                viewModel.addCustomVideo(
                    title = title,
                    urlOrId = url,
                    duration = duration,
                    isShort = isShort,
                    views = views,
                    likes = likes,
                    ctr = ctr,
                    tags = tags
                )
                showAddVideoDialog = false
                Toast.makeText(context, "Video „$title“ bylo přidáno!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // EDIT CHANNEL DIALOG
    if (showEditChannelDialog && connectedChannel != null) {
        EditChannelDialog(
            currentChannel = connectedChannel!!,
            onDismiss = { showEditChannelDialog = false },
            onSave = { title, handle, subscribers, description ->
                viewModel.updateChannelProfile(
                    title = title,
                    handle = handle,
                    subscriberCount = subscribers,
                    description = description
                )
                showEditChannelDialog = false
                Toast.makeText(context, "Profil kanálu byl upraven", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // AI VIDEO AUDIT DETAIL DIALOG
    if (selectedVideoForAudit != null) {
        val video = selectedVideoForAudit!!
        VideoAuditDetailDialog(
            video = video,
            audit = auditResult,
            isLoading = isAuditing,
            onDismiss = { viewModel.closeVideoAudit() },
            onCopy = { text, label ->
                clipboardManager.setText(AnnotatedString(text))
                Toast.makeText(context, "Zkopírováno: $label", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun NotConnectedOnboardingCard(
    onConnectClick: () -> Unit,
    userEmail: String
) {
    val youtubeRed = Color(0xFFFF0033)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = BorderStroke(1.dp, Slate800),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(youtubeRed.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = youtubeRed,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Připojte svůj YouTube kanál",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Slate100,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Propojte aplikaci s vaším skutečným účtem. Zadejte svůj kanál, spravujte svá videa, sledujte reálná čísla a nechte AI agenta provést detailní audit CTR, miniatur a titulků pro maximální růst.",
                style = MaterialTheme.typography.bodyMedium,
                color = Slate400,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // User email badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Slate800,
                border = BorderStroke(1.dp, Slate700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = CyanBright, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Přihlášený Google účet:", fontSize = 10.sp, color = Slate400)
                        Text(userEmail, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate100)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onConnectClick,
                colors = ButtonDefaults.buttonColors(containerColor = youtubeRed),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("🚀 Připojit můj YouTube kanál", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun VideoItemCard(
    video: YouTubeVideoItem,
    onAuditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val youtubeRed = Color(0xFFFF0033)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = BorderStroke(1.dp, if (video.ctrPercent < 5.0f) AmberWarning.copy(alpha = 0.5f) else Slate800),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // Thumbnail
                Box(
                    modifier = Modifier
                        .size(width = 114.dp, height = 68.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Slate950)
                ) {
                    AsyncImage(
                        model = video.thumbnailUrl,
                        contentDescription = video.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Duration Badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Black.copy(alpha = 0.8f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                    ) {
                        Text(
                            text = video.duration,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }

                    if (video.isShort) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = youtubeRed,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(4.dp)
                        ) {
                            Text(
                                text = "SHORTS",
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title and Publication
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate100,
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${video.publishedAt} • ${video.privacyStatus.uppercase()}",
                        fontSize = 10.sp,
                        color = Slate400
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Performance Metrics Pill Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Visibility, contentDescription = null, tint = Slate400, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("${video.viewCount}", fontSize = 10.sp, color = Slate300)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ThumbUp, contentDescription = null, tint = Slate400, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("${video.likeCount}", fontSize = 10.sp, color = Slate300)
                        }

                        // CTR Badge
                        val ctrColor = if (video.ctrPercent >= 7.0f) EmeraldBright else if (video.ctrPercent >= 5.0f) AmberWarning else Color(0xFFFF4D4D)
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = ctrColor.copy(alpha = 0.15f),
                            border = BorderStroke(0.5.dp, ctrColor)
                        ) {
                            Text(
                                text = "CTR: ${video.ctrPercent}%",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = ctrColor,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val healthColor = if (video.aiHealthScore >= 80) EmeraldBright else AmberWarning
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(healthColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Skóre: ${video.aiHealthScore}/100",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = healthColor
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Smazat video ze seznamu",
                            tint = Slate500,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Button(
                        onClick = onAuditClick,
                        colors = ButtonDefaults.buttonColors(containerColor = youtubeRed),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("AI Audit", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    sub: String,
    subColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Slate950,
        border = BorderStroke(1.dp, Slate800),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(label, fontSize = 9.sp, color = Slate400)
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Slate100,
                fontFamily = FontFamily.Monospace
            )
            Text(sub, fontSize = 8.sp, color = subColor)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConnectChannelDialog(
    currentEmail: String,
    onDismiss: () -> Unit,
    onConnect: (
        query: String,
        authType: YouTubeAuthType,
        apiKey: String?,
        customTitle: String?,
        customSubscribers: Long?,
        customCategory: String?
    ) -> Unit
) {
    var selectedMethod by remember { mutableStateOf(YouTubeAuthType.GOOGLE_OAUTH) }
    var channelNameInput by remember { mutableStateOf("Petr Lukeš") }
    var channelHandleInput by remember { mutableStateOf("@pplukes892") }
    var subscriberCountInput by remember { mutableStateOf("250") }
    var selectedCategory by remember { mutableStateOf("Tech & IT") }
    var apiKeyInput by remember { mutableStateOf("") }

    val categories = listOf("Tech & IT", "Gaming", "Vlogy", "Vzdělávání", "Finance & Byznys", "Hudba", "Zábava", "Ostatní")
    val youtubeRed = Color(0xFFFF0033)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Link, contentDescription = null, tint = youtubeRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Připojit váš YouTube kanál", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Způsob ověření účtu:",
                    fontSize = 12.sp,
                    color = Slate300,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Method Selector Tabs
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    YouTubeAuthType.values().forEach { type ->
                        FilterChip(
                            selected = selectedMethod == type,
                            onClick = { selectedMethod = type },
                            label = { Text(type.label.split(" ").first(), fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = youtubeRed,
                                selectedLabelColor = Color.White,
                                containerColor = Slate800,
                                labelColor = Slate300
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedMethod == YouTubeAuthType.GOOGLE_OAUTH) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Slate800,
                        border = BorderStroke(1.dp, Slate700),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Přihlášený Google účet:", fontSize = 10.sp, color = Slate400)
                            Text(currentEmail, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate100)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                OutlinedTextField(
                    value = channelNameInput,
                    onValueChange = { channelNameInput = it },
                    label = { Text("Název vašeho kanálu") },
                    placeholder = { Text("např. Petr Lukeš nebo Tech Vlog") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = channelHandleInput,
                    onValueChange = { channelHandleInput = it },
                    label = { Text("Váš @handle nebo link kanálu") },
                    placeholder = { Text("@pplukes892 nebo youtube.com/@lukes") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = subscriberCountInput,
                    onValueChange = { subscriberCountInput = it },
                    label = { Text("Počet odběratelů") },
                    placeholder = { Text("např. 250") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("Kategorie tvorby:", fontSize = 11.sp, color = Slate400)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Slate700,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                if (selectedMethod == YouTubeAuthType.YOUTUBE_API_KEY) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("YouTube Data API v3 Klíč (volitelné)") },
                        placeholder = { Text("AIzaSy...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalQuery = channelHandleInput.trim().ifBlank {
                        "@" + channelNameInput.trim().lowercase().replace(" ", "")
                    }
                    val subs = subscriberCountInput.trim().toLongOrNull() ?: 0L
                    onConnect(
                        finalQuery,
                        selectedMethod,
                        apiKeyInput.trim().ifBlank { null },
                        channelNameInput.trim().ifBlank { null },
                        subs,
                        selectedCategory
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = youtubeRed)
            ) {
                Text("Připojit můj kanál")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zrušit")
            }
        }
    )
}

@Composable
private fun AddCustomVideoDialog(
    onDismiss: () -> Unit,
    onAdd: (
        title: String,
        urlOrId: String,
        duration: String,
        isShort: Boolean,
        views: Long,
        likes: Long,
        ctr: Float,
        tags: List<String>
    ) -> Unit
) {
    var titleInput by remember { mutableStateOf("") }
    var urlInput by remember { mutableStateOf("") }
    var durationInput by remember { mutableStateOf("10:15") }
    var isShort by remember { mutableStateOf(false) }
    var viewsInput by remember { mutableStateOf("120") }
    var likesInput by remember { mutableStateOf("15") }
    var ctrInput by remember { mutableStateOf("6.2") }
    var tagsInput by remember { mutableStateOf("youtube, vlog, tutorial") }

    val youtubeRed = Color(0xFFFF0033)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VideoCall, contentDescription = null, tint = youtubeRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Přidat mé video z YouTube", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Vložte název nebo odkaz vašeho reálného videa pro okamžitý AI audit.",
                    fontSize = 11.sp,
                    color = Slate400
                )

                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("Název vašeho videa *") },
                    placeholder = { Text("např. Můj první projekt v Androidu") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("Odkaz na video nebo ID") },
                    placeholder = { Text("https://youtu.be/... nebo dQw4w9WgXcQ") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isShort,
                        onClick = { isShort = false },
                        label = { Text("Dlouhé video") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = isShort,
                        onClick = { isShort = true },
                        label = { Text("Shorts") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = durationInput,
                        onValueChange = { durationInput = it },
                        label = { Text("Délka") },
                        placeholder = { Text(if (isShort) "0:45" else "12:30") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = ctrInput,
                        onValueChange = { ctrInput = it },
                        label = { Text("CTR v %") },
                        placeholder = { Text("6.5") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = viewsInput,
                        onValueChange = { viewsInput = it },
                        label = { Text("Zhlédnutí") },
                        placeholder = { Text("500") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = likesInput,
                        onValueChange = { likesInput = it },
                        label = { Text("Lajky") },
                        placeholder = { Text("40") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = tagsInput,
                    onValueChange = { tagsInput = it },
                    label = { Text("Klíčová slova / tagy (oddělené čárkou)") },
                    placeholder = { Text("recenze, tipy, zivot") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (titleInput.isNotBlank()) {
                        val views = viewsInput.toLongOrNull() ?: 100L
                        val likes = likesInput.toLongOrNull() ?: 10L
                        val ctr = ctrInput.toFloatOrNull() ?: 5.0f
                        val tags = tagsInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        onAdd(
                            titleInput.trim(),
                            urlInput.trim(),
                            durationInput.trim(),
                            isShort,
                            views,
                            likes,
                            ctr,
                            tags
                        )
                    }
                },
                enabled = titleInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = youtubeRed)
            ) {
                Text("Přidat a uložit video")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zrušit")
            }
        }
    )
}

@Composable
private fun EditChannelDialog(
    currentChannel: YouTubeChannelAccount,
    onDismiss: () -> Unit,
    onSave: (title: String, handle: String, subscriberCount: Long, description: String) -> Unit
) {
    var title by remember { mutableStateOf(currentChannel.title) }
    var handle by remember { mutableStateOf(currentChannel.handle) }
    var subscribers by remember { mutableStateOf(currentChannel.subscriberCount.toString()) }
    var description by remember { mutableStateOf(currentChannel.description) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = CyanBright)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Upravit informace o kanálu", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Název kanálu") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = handle,
                    onValueChange = { handle = it },
                    label = { Text("@handle") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = subscribers,
                    onValueChange = { subscribers = it },
                    label = { Text("Počet odběratelů") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Popis kanálu") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val subs = subscribers.toLongOrNull() ?: currentChannel.subscriberCount
                    onSave(title, handle, subs, description)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Slate700)
            ) {
                Text("Uložit změny")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zrušit")
            }
        }
    )
}

@Composable
private fun VideoAuditDetailDialog(
    video: YouTubeVideoItem,
    audit: VideoAiAuditResult?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onCopy: (text: String, label: String) -> Unit
) {
    val youtubeRed = Color(0xFFFF0033)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = youtubeRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI Audit videa", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            if (isLoading || audit == null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = youtubeRed)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Analyzuji video a metriky algoritmu...", fontSize = 12.sp, color = Slate300)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Video Header Summary
                    Text(
                        text = "„${video.title}“",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate100
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Zhlédnutí: ${video.viewCount}", fontSize = 10.sp, color = Slate400)
                        Text("CTR: ${video.ctrPercent}%", fontSize = 10.sp, color = if (video.ctrPercent < 5.0f) Color(0xFFFF4D4D) else EmeraldBright)
                        Text("Dopad: ${audit.estimatedCtrBoost}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EmeraldBright)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Title Critique
                    AuditSectionCard(title = "🔍 Diagnóza současného názvu") {
                        Text(audit.titleCritique, fontSize = 11.sp, color = Slate300)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Hook Critique
                    AuditSectionCard(title = "⏱️ Doporučení pro úvodních 30 sekund") {
                        Text(audit.hookCritique, fontSize = 11.sp, color = Slate300)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 5 High-CTR Recommended Titles
                    AuditSectionCard(title = "🔥 5 doporučených High-CTR titulků") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            audit.recommendedTitles.forEachIndexed { idx, title ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Slate950, RoundedCornerShape(6.dp))
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${idx + 1}. $title",
                                        fontSize = 11.sp,
                                        color = Slate100,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { onCopy(title, "Titulek #${idx + 1}") },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = CyanBright, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Recommended Tags
                    AuditSectionCard(title = "🏷️ Doporučené tagy pro YouTube Studio") {
                        val tagsString = audit.recommendedTags.joinToString(", ")
                        Text(tagsString, fontSize = 10.sp, color = Slate400, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = { onCopy(tagsString, "Klíčové tagy") },
                            colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Zkopírovat všechny tagy", fontSize = 10.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Pinned Comment
                    AuditSectionCard(title = "💬 Návrh připnutého komentáře (Pinned Comment)") {
                        Text(audit.pinnedCommentSuggestion, fontSize = 11.sp, color = Slate300)
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = { onCopy(audit.pinnedCommentSuggestion, "Připnutý komentář") },
                            colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Zkopírovat komentář", fontSize = 10.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Slate800)
            ) {
                Text("Zavřít audit")
            }
        }
    )
}

@Composable
private fun AuditSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Slate900,
        border = BorderStroke(1.dp, Slate800),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate200)
            Spacer(modifier = Modifier.height(6.dp))
            content()
        }
    }
}
