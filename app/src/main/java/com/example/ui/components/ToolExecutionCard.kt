package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun ToolExecutionCard(
    toolName: String,
    toolArgs: String?,
    toolResult: String?,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val (icon, label, tintColor) = resolveToolMetadata(toolName)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tool_card_${toolName}"),
        shape = RoundedCornerShape(8.dp),
        color = Slate900,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Slate800)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(tintColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = tintColor,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate200
                    )
                    if (!toolArgs.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = toolArgs.take(30) + if (toolArgs.length > 30) "..." else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = tintColor,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(EmeraldBright, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Slate400,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Slate950)
                        .padding(10.dp)
                ) {
                    if (!toolArgs.isNullOrBlank()) {
                        Text(
                            text = "Parametry:",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate400
                        )
                        Text(
                            text = toolArgs,
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate200,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    if (!toolResult.isNullOrBlank()) {
                        Text(
                            text = "Výstup:",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldBright
                        )
                        Text(
                            text = toolResult,
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate200,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

private fun resolveToolMetadata(toolName: String): Triple<ImageVector, String, androidx.compose.ui.graphics.Color> {
    return when (toolName.lowercase()) {
        "create_file" -> Triple(Icons.Default.CreateNewFolder, "Vytvoření souboru", EmeraldBright)
        "edit_code", "edit_file" -> Triple(Icons.Default.Code, "Úprava kódu", CyanBright)
        "read_file" -> Triple(Icons.Default.Article, "Čtení souboru", Slate200)
        "list_files" -> Triple(Icons.Default.FolderOpen, "Seznam souborů", AmberWarning)
        "delete_file" -> Triple(Icons.Default.Delete, "Smazání souboru", RoseError)
        "web_search", "websearch" -> Triple(Icons.Default.TravelExplore, "Web vyhledávání", CyanBright)
        "run_command", "runterminal", "terminal" -> Triple(Icons.Default.Terminal, "Terminál", EmeraldBright)
        "bump_version_and_push" -> Triple(Icons.Default.RocketLaunch, "Vydání verze & Build", VioletPurple)
        "manage_secret" -> Triple(Icons.Default.Key, "Správa klíčů", AmberWarning)
        "check_update" -> Triple(Icons.Default.SystemUpdate, "Aktualizace", CyanBright)
        else -> Triple(Icons.Default.SmartToy, "Nástroj: $toolName", CyanBright)
    }
}
