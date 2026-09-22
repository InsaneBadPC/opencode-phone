package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.youtube.*
import com.example.ui.OpenCodeViewModel
import com.example.ui.theme.*

@Composable
fun YouTubeContentCalendarView(
    viewModel: OpenCodeViewModel,
    channelNiche: String = "AI & Programování",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var drafts by remember { mutableStateOf(YouTubeHistoricalData.getInitialDrafts()) }
    var selectedFilter by remember { mutableStateOf("Vše") }
    var showNewDraftDialog by remember { mutableStateOf(false) }
    var detailedSlotDraft by remember { mutableStateOf<VideoDraft?>(null) }

    val heatmaps = remember { YouTubeHistoricalData.defaultEngagementHeatmaps }

    val youtubeRed = Color(0xFFFF0033)

    // Filter logic
    val filteredDrafts = remember(drafts, selectedFilter) {
        when (selectedFilter) {
            "K publikaci" -> drafts.filter { it.status == VideoDraftStatus.READY || it.status == VideoDraftStatus.SCHEDULED }
            "Ve výrobě" -> drafts.filter { it.status == VideoDraftStatus.SCRIPTING || it.status == VideoDraftStatus.RECORDING || it.status == VideoDraftStatus.EDITING }
            "Shorts" -> drafts.filter { it.format == VideoDraftFormat.SHORTS }
            "Dlouhá videa" -> drafts.filter { it.format == VideoDraftFormat.LONG_FORM }
            else -> drafts
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("youtube_content_calendar_view")
    ) {
        // TOP METRICS STRIP: Pipeline Overview
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = BorderStroke(1.dp, Slate800),
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
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(youtubeRed.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = youtubeRed, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "YouTube Content Kalendář",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate100
                            )
                            Text(
                                text = "Naplánované koncepty & historická peak engagement okna",
                                fontSize = 10.sp,
                                color = Slate400
                            )
                        }
                    }

                    Button(
                        onClick = { showNewDraftDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = youtubeRed, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_new_video_draft")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Nový koncept", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Pipeline Quick Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PipelineMiniStat(
                        label = "Celkem v plánu",
                        value = "${drafts.size}",
                        icon = Icons.Default.FolderOpen,
                        color = CyanBright,
                        modifier = Modifier.weight(1f)
                    )
                    PipelineMiniStat(
                        label = "Připraveno",
                        value = "${drafts.count { it.status == VideoDraftStatus.READY }}",
                        icon = Icons.Default.CheckCircle,
                        color = EmeraldBright,
                        modifier = Modifier.weight(1f)
                    )
                    PipelineMiniStat(
                        label = "S naplánovaným slotem",
                        value = "${drafts.count { it.scheduledSlot != null }}",
                        icon = Icons.Default.Schedule,
                        color = AmberWarning,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // HISTORICAL ENGAGEMENT HEATMAP STRIP
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Whatshot, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Historické špičky aktivity publika (Peak Engagement)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Slate100
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Slate800
                    ) {
                        Text(
                            text = "Velocity 120m",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberWarning,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Algoritmus YouTube sleduje rychlost prokliků (Velocity) v prvních 2 hodinách od publikace. Publikujte v tyto špičky pro okamžité doporučování:",
                    fontSize = 10.sp,
                    color = Slate300,
                    lineHeight = 14.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 7 Days Heatmap Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    heatmaps.forEach { day ->
                        DayHeatmapPill(day = day)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // FILTER CHIPS ROW
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val filterOptions = listOf("Vše", "K publikaci", "Ve výrobě", "Dlouhá videa", "Shorts")
            filterOptions.forEach { option ->
                FilterChip(
                    selected = selectedFilter == option,
                    onClick = { selectedFilter = option },
                    label = { Text(option, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = youtubeRed,
                        selectedLabelColor = Color.White,
                        containerColor = Slate900,
                        labelColor = Slate300
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // DRAFTS LIST
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (filteredDrafts.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Slate900,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = Slate600, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Žádné koncepty v této kategorii", color = Slate400, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { showNewDraftDialog = true },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Vytvořit nový koncept")
                            }
                        }
                    }
                }
            } else {
                items(filteredDrafts, key = { it.id }) { draft ->
                    VideoDraftCard(
                        draft = draft,
                        onSuggestOptimalSlot = {
                            val recommended = YouTubeHistoricalData.recommendBestSlotForDraft(draft)
                            drafts = drafts.map { if (it.id == draft.id) it.copy(scheduledSlot = recommended) else it }
                            Toast.makeText(
                                context,
                                "Přiřazen optimální slot: ${recommended.dayOfWeek} ${recommended.timeString} (${recommended.engagementLevel})",
                                Toast.LENGTH_LONG
                            ).show()
                        },
                        onOpenSlotDetails = { detailedSlotDraft = draft },
                        onSendToAiChat = {
                            val prompt = "Napiš kompletní produkční plán a scénář pro YouTube video na téma '${draft.title}' (${draft.format.displayName}). " +
                                    "Cílová délka: ${draft.expectedDurationMinutes} minut. Cílové CTR: ${draft.targetCtrGoal} %. " +
                                    "Poznámky k úvodu (hooku): '${draft.hookNotes}'. " +
                                    "Navrhni: 1) 3 virální titulky, 2) Prvních 30 sekund slovo od slova, 3) 5 časových kapitol a B-roll doporučení pro udržení retence."
                            viewModel.updateChatInput(prompt)
                            viewModel.selectTab(0)
                            Toast.makeText(context, "Produkční plán vložen do AI Chatu!", Toast.LENGTH_SHORT).show()
                        },
                        onCycleStatus = {
                            val nextStatus = when (draft.status) {
                                VideoDraftStatus.IDEA -> VideoDraftStatus.SCRIPTING
                                VideoDraftStatus.SCRIPTING -> VideoDraftStatus.RECORDING
                                VideoDraftStatus.RECORDING -> VideoDraftStatus.EDITING
                                VideoDraftStatus.EDITING -> VideoDraftStatus.READY
                                VideoDraftStatus.READY -> VideoDraftStatus.SCHEDULED
                                VideoDraftStatus.SCHEDULED -> VideoDraftStatus.PUBLISHED
                                VideoDraftStatus.PUBLISHED -> VideoDraftStatus.IDEA
                            }
                            drafts = drafts.map { if (it.id == draft.id) it.copy(status = nextStatus) else it }
                        },
                        onDeleteDraft = {
                            drafts = drafts.filter { it.id != draft.id }
                            Toast.makeText(context, "Koncept odstraněn", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    // DIALOG: ADD NEW VIDEO DRAFT
    if (showNewDraftDialog) {
        NewVideoDraftDialog(
            channelNiche = channelNiche,
            onDismiss = { showNewDraftDialog = false },
            onSaveDraft = { newDraft ->
                drafts = listOf(newDraft) + drafts
                showNewDraftDialog = false
                Toast.makeText(context, "Nový koncept byl přidán do kalendáře!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // DIALOG: DETAILED SLOT RATIONALE
    if (detailedSlotDraft != null) {
        val draft = detailedSlotDraft!!
        val slot = draft.scheduledSlot ?: YouTubeHistoricalData.recommendBestSlotForDraft(draft)

        AlertDialog(
            onDismissRequest = { detailedSlotDraft = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Analytics, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Algoritmická analýza slotu", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = draft.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate100)
                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Slate950,
                        border = BorderStroke(0.5.dp, AmberWarning),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${slot.dayOfWeek} v ${slot.timeString}", fontWeight = FontWeight.Bold, color = AmberWarning, fontSize = 14.sp)
                                Text("Skóre: ${slot.historicalScore} / 100", fontWeight = FontWeight.Bold, color = EmeraldBright, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(slot.engagementLevel, fontSize = 11.sp, color = Slate300)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Proč algoritmus doporučuje tento čas:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Slate200)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(slot.rationale, fontSize = 11.sp, color = Slate300, lineHeight = 16.sp)

                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(shape = RoundedCornerShape(6.dp), color = Slate800, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = CyanBright, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Doporučený neveřejný upload: ${slot.unlistedUploadTime}",
                                fontSize = 10.sp,
                                color = Slate200
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { detailedSlotDraft = null },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanBright, contentColor = Slate950)
                ) {
                    Text("Rozumím")
                }
            },
            containerColor = Slate900
        )
    }
}

// ==============================================================================
// SUB-COMPONENTS
// ==============================================================================

@Composable
private fun PipelineMiniStat(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Slate950,
        border = BorderStroke(0.5.dp, Slate800),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate100)
                Text(text = label, fontSize = 9.sp, color = Slate400)
            }
        }
    }
}

@Composable
private fun DayHeatmapPill(day: DayEngagementHeatmap) {
    val barColor = when {
        day.heatScore >= 90 -> Color(0xFFFF0033) // Peak red
        day.heatScore >= 80 -> AmberWarning
        else -> CyanBright
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (day.isRecommendedPrimary) Slate800 else Slate950,
        border = if (day.isRecommendedPrimary) BorderStroke(1.dp, barColor) else BorderStroke(0.5.dp, Slate800),
        modifier = Modifier.width(96.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = day.dayShort,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (day.isRecommendedPrimary) Slate100 else Slate300
                )
                Text(
                    text = "${day.heatScore}%",
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = barColor
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = { day.heatScore / 100f },
                color = barColor,
                trackColor = Slate800,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = day.peakHours,
                fontSize = 8.sp,
                color = Slate400,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun VideoDraftCard(
    draft: VideoDraft,
    onSuggestOptimalSlot: () -> Unit,
    onOpenSlotDetails: () -> Unit,
    onSendToAiChat: () -> Unit,
    onCycleStatus: () -> Unit,
    onDeleteDraft: () -> Unit
) {
    val youtubeRed = Color(0xFFFF0033)
    val hasSlot = draft.scheduledSlot != null

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = BorderStroke(
            1.dp,
            if (hasSlot) youtubeRed.copy(alpha = 0.35f) else Slate800
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top row: Format Badge + Status Tag + Duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Format Pill
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (draft.format == VideoDraftFormat.SHORTS) Color(0xFFFF0033).copy(alpha = 0.2f) else Slate800,
                        border = if (draft.format == VideoDraftFormat.SHORTS) BorderStroke(0.5.dp, Color(0xFFFF0033)) else null
                    ) {
                        Text(
                            text = if (draft.format == VideoDraftFormat.SHORTS) "⚡ Shorts (9:16)" else "🎬 Video (16:9)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (draft.format == VideoDraftFormat.SHORTS) Color(0xFFFF4D4D) else CyanBright,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Status Pill (Clickable to advance status)
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Slate800,
                        modifier = Modifier.clickable { onCycleStatus() }
                    ) {
                        Text(
                            text = draft.status.label,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate200,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "⏱️ ${draft.expectedDurationMinutes} min  •  CTR cíl: ${draft.targetCtrGoal}%",
                        fontSize = 9.sp,
                        color = Slate400
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Video Title
            Text(
                text = draft.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Slate100,
                lineHeight = 18.sp
            )

            if (draft.hookNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "💡 Hook nápad: ${draft.hookNotes}",
                    fontSize = 10.sp,
                    color = Slate300,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // PUBLISHING SLOT SECTION
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Slate950,
                border = BorderStroke(0.5.dp, if (hasSlot) AmberWarning.copy(alpha = 0.5f) else Slate800),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (hasSlot) {
                    val slot = draft.scheduledSlot!!
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenSlotDetails() }
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EventAvailable, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${slot.dayOfWeek} v ${slot.timeString}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AmberWarning
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "• ${slot.historicalScore}% engagement",
                                        fontSize = 10.sp,
                                        color = EmeraldBright,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = slot.unlistedUploadTime,
                                    fontSize = 9.sp,
                                    color = Slate400
                                )
                            }
                        }

                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Detail", tint = Slate500, modifier = Modifier.size(14.dp))
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Zatím nepřiřazen optimální publikační slot",
                            fontSize = 10.sp,
                            color = Slate400
                        )

                        FilledTonalButton(
                            onClick = onSuggestOptimalSlot,
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = AmberWarning.copy(alpha = 0.2f), contentColor = AmberWarning),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Doporučit slot ⚡", fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                // Change status / Re-suggest button
                if (hasSlot) {
                    TextButton(
                        onClick = onSuggestOptimalSlot,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(13.dp), tint = Slate400)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Přepočítat slot", fontSize = 10.sp, color = Slate400)
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = onDeleteDraft,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Smazat", tint = Slate500, modifier = Modifier.size(15.dp))
                    }

                    // Send to AI chat for full script / strategy
                    Button(
                        onClick = onSendToAiChat,
                        colors = ButtonDefaults.buttonColors(containerColor = youtubeRed, contentColor = Color.White),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Scénář v AI Chatu", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==============================================================================
// DIALOG: CREATE NEW VIDEO DRAFT
// ==============================================================================

@Composable
private fun NewVideoDraftDialog(
    channelNiche: String,
    onDismiss: () -> Unit,
    onSaveDraft: (VideoDraft) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedFormat by remember { mutableStateOf(VideoDraftFormat.LONG_FORM) }
    var expectedMinutes by remember { mutableStateOf("12") }
    var hookIdea by remember { mutableStateOf("") }
    var autoAssignSlot by remember { mutableStateOf(true) }

    val youtubeRed = Color(0xFFFF0033)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VideoCall, contentDescription = null, tint = youtubeRed, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Nový koncept videa", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Název nebo pracovní téma videa:", fontSize = 11.sp, color = Slate400)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("např. 5 chyb, které ničí retenci na YouTube...", color = Slate500, fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = youtubeRed,
                        unfocusedBorderColor = Slate700,
                        focusedTextColor = Slate100,
                        unfocusedTextColor = Slate100
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Format selector
                Text("Formát obsahu:", fontSize = 11.sp, color = Slate400)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFormat == VideoDraftFormat.LONG_FORM,
                        onClick = { selectedFormat = VideoDraftFormat.LONG_FORM },
                        label = { Text("🎬 Dlouhé video (16:9)", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = youtubeRed,
                            selectedLabelColor = Color.White
                        )
                    )

                    FilterChip(
                        selected = selectedFormat == VideoDraftFormat.SHORTS,
                        onClick = { selectedFormat = VideoDraftFormat.SHORTS },
                        label = { Text("⚡ Shorts (9:16)", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = youtubeRed,
                            selectedLabelColor = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Duration field
                Text("Předpokládaná délka videa (v minutách):", fontSize = 11.sp, color = Slate400)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = expectedMinutes,
                    onValueChange = { expectedMinutes = it.filter { c -> c.isDigit() } },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanBright,
                        unfocusedBorderColor = Slate700,
                        focusedTextColor = Slate100,
                        unfocusedTextColor = Slate100
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Hook Idea
                Text("Nápad na úvodní hook (prvních 30 sekund):", fontSize = 11.sp, color = Slate400)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = hookIdea,
                    onValueChange = { hookIdea = it },
                    placeholder = { Text("Čím okamžitě zaujmout diváka bez intra a loga...", color = Slate500, fontSize = 11.sp) },
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AmberWarning,
                        unfocusedBorderColor = Slate700,
                        focusedTextColor = Slate100,
                        unfocusedTextColor = Slate100
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Auto-assign slot toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = autoAssignSlot,
                        onCheckedChange = { autoAssignSlot = it },
                        colors = CheckboxDefaults.colors(checkedColor = youtubeRed)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Automaticky přiřadit nejlepší historický slot (Čtvrtek 17:30 / Neděle 16:00)",
                        fontSize = 11.sp,
                        color = Slate200
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) return@Button
                    val duration = expectedMinutes.toIntOrNull() ?: 10
                    val draft = VideoDraft(
                        title = title.trim(),
                        format = selectedFormat,
                        status = VideoDraftStatus.IDEA,
                        topicNiche = channelNiche,
                        expectedDurationMinutes = duration,
                        hookNotes = hookIdea.trim(),
                        scheduledSlot = if (autoAssignSlot) {
                            YouTubeHistoricalData.recommendBestSlotForDraft(
                                VideoDraft(title = title, format = selectedFormat, expectedDurationMinutes = duration)
                            )
                        } else null
                    )
                    onSaveDraft(draft)
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = youtubeRed, contentColor = Color.White),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Uložit do kalendáře")
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
