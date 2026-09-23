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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
            // NOT CONNECTED STATE: High-impact Login / Connect Card
            NotConnectedOnboardingCard(
                onConnectClick = { showConnectDialog = true },
                onQuickDemoConnect = {
                    viewModel.connectYouTubeChannel(
                        query = "@pepa_dev",
                        authType = YouTubeAuthType.GOOGLE_OAUTH,
                        userEmail = "p.p.lukes892@gmail.com"
                    )
                }
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
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

                        // Disconnect / Switch button
                        IconButton(
                            onClick = { viewModel.disconnectYouTubeChannel() },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Slate800)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Odpojit kanál",
                                tint = Slate300,
                                modifier = Modifier.size(18.dp)
                            )
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
                            sub = "+180 tento týden",
                            subColor = EmeraldBright,
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            label = "Celková zhlédnutí",
                            value = "${channel.viewCount}",
                            sub = "42 videí online",
                            subColor = Slate400,
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            label = "Stav kanálu",
                            value = "Zdravý 92%",
                            sub = "Top 10% nisy",
                            subColor = CyanBright,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action buttons (Refresh & Connect another)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.refreshYouTubeVideos() },
                            enabled = !isConnecting,
                            colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = Slate200)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Synchronizovat", fontSize = 11.sp, color = Slate200)
                        }

                        OutlinedButton(
                            onClick = { showConnectDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Slate700),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp), tint = Slate300)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Přepnout kanál", fontSize = 11.sp, color = Slate300)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section: My Content & Videos
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
                Text(
                    text = "Klikněte pro AI Audit",
                    fontSize = 11.sp,
                    color = Slate400
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

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

            // Videos List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredVideos) { video ->
                    VideoItemCard(
                        video = video,
                        onAuditClick = { viewModel.startVideoAudit(video) }
                    )
                }
            }
        }
    }

    // CONNECT CHANNEL DIALOG
    if (showConnectDialog) {
        ConnectChannelDialog(
            currentEmail = "p.p.lukes892@gmail.com",
            onDismiss = { showConnectDialog = false },
            onConnect = { query, authType, apiKey ->
                viewModel.connectYouTubeChannel(
                    query = query,
                    authType = authType,
                    apiKey = apiKey,
                    userEmail = "p.p.lukes892@gmail.com"
                )
                showConnectDialog = false
                Toast.makeText(context, "Připojování ke kanálu...", Toast.LENGTH_SHORT).show()
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
    onQuickDemoConnect: () -> Unit
) {
    val youtubeRed = Color(0xFFFF0033)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = BorderStroke(1.dp, Slate800),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(youtubeRed.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = youtubeRed,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Připojte svůj YouTube kanál",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Slate100
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Propojte YouTube Agent s vaším reálným účtem. Agent načte vaše nahraná videa, zhlédnutí, prokliky (CTR) a navrhne přesné úpravy titulků a miniatur pro okamžitý růst.",
                style = MaterialTheme.typography.bodySmall,
                color = Slate400,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onConnectClick,
                colors = ButtonDefaults.buttonColors(containerColor = youtubeRed),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Přihlásit se přes Google / YouTube", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onQuickDemoConnect,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Slate700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayCircle, contentDescription = null, tint = CyanBright, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Vyzkoušet s ukázkovým kanálem (@pepa_dev)", color = Slate200)
            }
        }
    }
}

@Composable
private fun VideoItemCard(
    video: YouTubeVideoItem,
    onAuditClick: () -> Unit
) {
    val youtubeRed = Color(0xFFFF0033)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = BorderStroke(1.dp, if (video.ctrPercent < 5.0f) AmberWarning.copy(alpha = 0.5f) else Slate800),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAuditClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // Thumbnail
                Box(
                    modifier = Modifier
                        .size(width = 110.dp, height = 66.dp)
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

            // Action Buttons
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
                        text = "Skóre algoritmu: ${video.aiHealthScore}/100",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = healthColor
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
    onConnect: (query: String, authType: YouTubeAuthType, apiKey: String?) -> Unit
) {
    var selectedMethod by remember { mutableStateOf(YouTubeAuthType.GOOGLE_OAUTH) }
    var channelQueryInput by remember { mutableStateOf("@pepa_dev") }
    var apiKeyInput by remember { mutableStateOf("") }

    val youtubeRed = Color(0xFFFF0033)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Link, contentDescription = null, tint = youtubeRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Připojit YouTube kanál", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Vyberte způsob propojení s vaším kanálem:",
                    fontSize = 12.sp,
                    color = Slate400
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Method Selector Tabs
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    YouTubeAuthType.values().forEach { type ->
                        FilterChip(
                            selected = selectedMethod == type,
                            onClick = { selectedMethod = type },
                            label = { Text(type.label.split(" ").first(), fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Slate800,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                when (selectedMethod) {
                    YouTubeAuthType.GOOGLE_OAUTH -> {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Slate800,
                            border = BorderStroke(1.dp, Slate700),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = CyanBright, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Zjištěný Google účet:", fontSize = 11.sp, color = Slate300)
                                }
                                Text(
                                    text = currentEmail,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                Text(
                                    text = "Oprávnění: Čtení YouTube videí & YouTube Analytics (pouze pro čtení)",
                                    fontSize = 9.sp,
                                    color = Slate400
                                )
                            }
                        }
                    }

                    YouTubeAuthType.HANDLE_SYNC -> {
                        OutlinedTextField(
                            value = channelQueryInput,
                            onValueChange = { channelQueryInput = it },
                            label = { Text("YouTube Handle nebo URL") },
                            placeholder = { Text("@vas_kanal") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    YouTubeAuthType.YOUTUBE_API_KEY -> {
                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = { apiKeyInput = it },
                            label = { Text("YouTube Data API v3 Klíč") },
                            placeholder = { Text("AIzaSy...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = channelQueryInput,
                            onValueChange = { channelQueryInput = it },
                            label = { Text("Váš Channel ID nebo Handle") },
                            placeholder = { Text("UC... nebo @kanál") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalQuery = if (selectedMethod == YouTubeAuthType.GOOGLE_OAUTH) "@pepa_dev" else channelQueryInput
                    onConnect(finalQuery, selectedMethod, apiKeyInput.ifBlank { null })
                },
                colors = ButtonDefaults.buttonColors(containerColor = youtubeRed)
            ) {
                Text("Autorizovat & Propojit")
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
                            onClick = { onCopy(tagsString, "SEO Tagy") },
                            colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Zkopírovat všechny tagy do schránky", fontSize = 10.sp)
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
