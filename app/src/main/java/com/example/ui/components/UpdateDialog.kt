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
                onOpenGitHub = { viewModel.openGitHubReleaseUrl(state.release.htmlUrl) },
                onDismiss = { viewModel.dismissUpdate() }
            )
        }
        is UpdateCheckState.Checking -> {
            CheckingDialogContent(
                onDismiss = { viewModel.dismissUpdate() }
            )
        }
        is UpdateCheckState.UpToDate -> {
            UpToDateDialogContent(
                version = state.currentVersion,
                onTestUpdate = { viewModel.forceOfferUpdate() },
                onOpenGitHub = { viewModel.openGitHubReleases() },
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
        is UpdateCheckState.Idle -> {
            // Nothing to display
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
    onOpenGitHub: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Slate900,
            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = EmeraldBright,
                    modifier = Modifier.size(44.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Instalační balíček připraven",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Slate100
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Verze ${release.tagName} byla stažena do paměti zařízení.",
                    fontSize = 11.sp,
                    color = Slate400,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Signature Conflict Guide Card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AmberWarning.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberWarning.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = AmberWarning,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Hlásí Android konflikt balíčků?",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberWarning
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Chyba 'Balíček je v konfliktu se stávajícím balíčkem' vzniká, když má nová verze jiný podpisový certifikát (Keystore) než aktuálně nainstalovaná aplikace.\n\n" +
                                    "Řešení ve 2 krocích:\n" +
                                    "1. Původní aplikaci odinstalujte z telefonu (dlouhý stisk ikony -> Odinstalovat).\n" +
                                    "2. Klikněte níže na 'Spustit instalaci' nebo nainstalujte APK z GitHubu.",
                            fontSize = 10.sp,
                            color = Slate200,
                            lineHeight = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onInstallAgain,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = Slate950),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Spustit instalaci APK", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onOpenGitHub,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanBright),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Otevřít vydání na GitHubu", fontSize = 11.sp)
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

@Composable
private fun CheckingDialogContent(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = CyanBright,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Kontrola aktualizací", color = Slate100, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Text(
                "Ověřuji nejnovější vydání v repozitáři InsaneBadPC/opencode-phone na GitHubu...",
                color = Slate300,
                fontSize = 12.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Zrušit", color = Slate400)
            }
        },
        containerColor = Slate900
    )
}

@Composable
private fun UpToDateDialogContent(
    version: String,
    onTestUpdate: () -> Unit,
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
            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(EmeraldSuccess.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = EmeraldBright,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Aplikace je aktuální",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Slate100
                )

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = EmeraldSuccess.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "v$version • Nejnovější verze",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = EmeraldBright,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Máte nainstalovanou nejnovější dostupnou verzi OpenCode. Žádné novější aktualizace nebyly na GitHubu nalezeny.",
                    fontSize = 12.sp,
                    color = Slate300,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanBright, contentColor = Slate950),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Rozumím", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onTestUpdate,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate300),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Vynutit dialog", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = onOpenGitHub,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate300),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Releases", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
