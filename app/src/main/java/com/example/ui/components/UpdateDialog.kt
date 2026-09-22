package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.update.AppReleaseInfo
import com.example.data.update.UpdateCheckState
import com.example.ui.OpenCodeViewModel
import com.example.ui.theme.*

@Composable
fun UpdateDialog(
    viewModel: OpenCodeViewModel,
    modifier: Modifier = Modifier
) {
    val updateState by viewModel.updateState.collectAsState()

    when (val state = updateState) {
        is UpdateCheckState.UpdateAvailable -> {
            UpdateAvailableDialogContent(
                release = state.release,
                currentVersion = viewModel.appVersionName,
                onUpdate = { viewModel.downloadAndInstallUpdate(state.release) },
                onOpenGitHub = { viewModel.openGitHubReleaseUrl(state.release.htmlUrl) },
                onDismiss = { viewModel.dismissUpdate() }
            )
        }
        is UpdateCheckState.Downloading -> {
            DownloadingDialogContent(
                percent = state.progressPercent,
                downloadedBytes = state.downloadedBytes,
                totalBytes = state.totalBytes,
                onDismiss = { viewModel.dismissUpdate() }
            )
        }
        is UpdateCheckState.ReadyToInstall -> {
            ReadyToInstallDialogContent(
                release = state.release,
                onInstallAgain = { viewModel.launchInstaller(state.apkFile) },
                onDismiss = { viewModel.dismissUpdate() }
            )
        }
        is UpdateCheckState.Error -> {
            UpdateErrorDialogContent(
                message = state.message,
                onRetry = { viewModel.checkForUpdates() },
                onDismiss = { viewModel.dismissUpdate() }
            )
        }
        else -> {
            // Idle or Checking - handled unobtrusively
        }
    }
}

@Composable
private fun UpdateAvailableDialogContent(
    release: AppReleaseInfo,
    currentVersion: String,
    onUpdate: () -> Unit,
    onOpenGitHub: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Slate900,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, CyanBright),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp)
                .testTag("dialog_update_available")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CyanBright.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = CyanBright,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "K dispozici je nová verze!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Slate100
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Version Tag comparison
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Slate800
                    ) {
                        Text(
                            text = "v$currentVersion",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Slate400,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = Slate400,
                        modifier = Modifier.size(14.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = EmeraldSuccess.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess)
                    ) {
                        Text(
                            text = release.tagName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = EmeraldBright,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Release Title & Changelog Box
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF070B14),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Slate800),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = release.releaseTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Slate200
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = release.releaseNotes,
                            fontSize = 11.sp,
                            color = Slate300,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Buttons
                Button(
                    onClick = onUpdate,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanBright, contentColor = Slate950),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("btn_confirm_download_update")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stáhnout a instalovat APK", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenGitHub,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate300),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("GitHub", fontSize = 11.sp)
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Později", color = Slate400, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadingDialogContent(
    percent: Int,
    downloadedBytes: Long,
    totalBytes: Long,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Slate900,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    progress = { percent / 100f },
                    color = CyanBright,
                    modifier = Modifier.size(50.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Stahování aktualizace...",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Slate100
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "$percent % hotovo",
                    fontSize = 12.sp,
                    color = CyanBright,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = { percent / 100f },
                    color = CyanBright,
                    trackColor = Slate800,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                TextButton(onClick = onDismiss) {
                    Text("Zrušit", color = Slate400, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ReadyToInstallDialogContent(
    release: AppReleaseInfo,
    onInstallAgain: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Slate900,
            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = EmeraldBright,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Aktualizace stažena",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Slate100
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Spuštěn nativní instalátor balíčků Android pro verzi ${release.tagName}.",
                    fontSize = 11.sp,
                    color = Slate300,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onInstallAgain,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = Slate950),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Spustit instalátor znovu", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(6.dp))

                TextButton(onClick = onDismiss) {
                    Text("Zavřít", color = Slate400)
                }
            }
        }
    }
}

@Composable
private fun UpdateErrorDialogContent(
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aktualizace", color = Slate100, fontWeight = FontWeight.Bold) },
        text = { Text(message, color = Slate300, fontSize = 12.sp) },
        confirmButton = {
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = CyanBright, contentColor = Slate950)
            ) {
                Text("Zkusit znovu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zavřít", color = Slate400)
            }
        },
        containerColor = Slate900
    )
}
