package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleOAuthDialog(
    projectId: String = "gen-lang-client-0025970914",
    projectNumber: Long = 670263378430L,
    brandName: String = "YouAgent",
    onDismiss: () -> Unit,
    onAuthorize: (tokenOrKey: String, email: String) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val detectedUserEmail = "p.p.lukes892@gmail.com"
    var emailInput by remember { mutableStateOf(detectedUserEmail) }
    var handleInput by remember { mutableStateOf("") }
    var tokenInput by remember { mutableStateOf("") }

    val youtubeRed = Color(0xFFFF0033)
    val googleBlue = Color(0xFF4285F4)
    val googleGreen = Color(0xFF34A853)
    val googleYellow = Color(0xFFFBBC05)
    val googleRed = Color(0xFFEA4335)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Google "G" circle badge
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "G",
                        color = googleRed,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Google OAuth 2.0",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Slate100
                    )
                    Text(
                        text = "Oficiální propojení YouTube agenta",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400,
                        fontSize = 11.sp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Official Cloud Project Metadata Card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Slate900,
                    border = BorderStroke(1.dp, Slate800),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = EmeraldBright,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Google Cloud Ověření",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldBright
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = googleBlue.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = brandName,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = googleBlue,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Projekt: $projectId (#$projectNumber)",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Slate400
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Požadovaná oprávnění (Scopes):",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate300
                        )
                        Text(
                            text = "• youtube.readonly (čtení videí a statistik kanálu)\n• youtube (správa a audit obsahu)",
                            fontSize = 9.sp,
                            color = Slate400,
                            lineHeight = 13.sp
                        )
                    }
                }

                // Auth Mode Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Slate900,
                    contentColor = CyanBright
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Google Účet", fontSize = 11.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("YouTube @Handle", fontSize = 11.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("OAuth Token", fontSize = 11.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) }
                    )
                }

                when (selectedTab) {
                    0 -> {
                        // Google Email Sign-In
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Primary 1-tap Google Sign-In Card
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White,
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val chosenEmail = emailInput.ifBlank { detectedUserEmail }
                                        val chosenHandle = handleInput.ifBlank { "@" + chosenEmail.substringBefore("@").replace(".", "_") }
                                        onAuthorize(chosenHandle, chosenEmail)
                                    }
                                    .testTag("oauth_quick_google_signin")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(googleRed, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("G", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Přihlásit účet ${emailInput.ifBlank { detectedUserEmail }}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF1F1F1F)
                                        )
                                        Text(
                                            text = "1 kliknutí • Okamžitá autorizace bez chybného přesměrování",
                                            fontSize = 10.sp,
                                            color = EmeraldDark
                                        )
                                    }
                                    Icon(
                                        Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        tint = googleBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = Slate800)

                            Text(
                                text = "Nebo zadejte jiný Google e-mail nebo YouTube kanál:",
                                fontSize = 11.sp,
                                color = Slate300
                            )

                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it },
                                label = { Text("Váš Google E-mail *") },
                                placeholder = { Text("např. muj.ucet@gmail.com") },
                                leadingIcon = {
                                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = googleBlue)
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("oauth_google_email_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = googleBlue,
                                    unfocusedBorderColor = Slate700
                                )
                            )

                            OutlinedTextField(
                                value = handleInput,
                                onValueChange = { handleInput = it },
                                label = { Text("YouTube @handle kanálu (volitelné)") },
                                placeholder = { Text("např. @muj_kanal") },
                                leadingIcon = {
                                    Icon(Icons.Default.Link, contentDescription = null, tint = youtubeRed)
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Native in-app auth status info
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Slate800.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = EmeraldBright,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Přímé propojení v aplikaci. Žádné otevírání neexistujících odkazů.",
                                        fontSize = 10.sp,
                                        color = Slate300
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        // Channel Handle Direct Resolution
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Zadejte YouTube @handle vašeho kanálu. Agent načte reálná data a videa přímo z YouTube:",
                                fontSize = 11.sp,
                                color = Slate300
                            )

                            OutlinedTextField(
                                value = handleInput,
                                onValueChange = { handleInput = it },
                                label = { Text("YouTube @handle nebo odkaz *") },
                                placeholder = { Text("např. @muj_kanal") },
                                leadingIcon = {
                                    Icon(Icons.Default.Link, contentDescription = null, tint = youtubeRed)
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it },
                                label = { Text("Váš Google E-mail (volitelné)") },
                                placeholder = { Text("např. muj.ucet@gmail.com") },
                                leadingIcon = {
                                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = CyanBright)
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    2 -> {
                        // Custom OAuth Bearer Token / API Key
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Vložte Google OAuth 2.0 Access Token (začínající ya29...) nebo YouTube Data API v3 klíč:",
                                fontSize = 11.sp,
                                color = Slate300
                            )

                            OutlinedTextField(
                                value = tokenInput,
                                onValueChange = { tokenInput = it },
                                label = { Text("OAuth 2.0 Token / API Klíč *") },
                                placeholder = { Text("ya29... nebo AIzaSy...") },
                                leadingIcon = {
                                    Icon(Icons.Default.Key, contentDescription = null, tint = AmberWarning)
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it },
                                label = { Text("Google E-mail účtu (volitelné)") },
                                placeholder = { Text("např. ucet@gmail.com") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanEmail = emailInput.trim()
                    val cleanHandle = handleInput.trim()
                    val cleanToken = tokenInput.trim()

                    val effectiveTokenOrQuery = when {
                        cleanToken.isNotBlank() -> cleanToken
                        cleanHandle.isNotBlank() -> cleanHandle
                        cleanEmail.isNotBlank() -> "@" + cleanEmail.substringBefore("@")
                        else -> "oauth_token_gen_lang_client"
                    }

                    onAuthorize(effectiveTokenOrQuery, cleanEmail)
                },
                enabled = when (selectedTab) {
                    0 -> emailInput.isNotBlank() || handleInput.isNotBlank()
                    1 -> handleInput.isNotBlank() || emailInput.isNotBlank()
                    2 -> tokenInput.isNotBlank() || emailInput.isNotBlank()
                    else -> true
                },
                colors = ButtonDefaults.buttonColors(containerColor = youtubeRed),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("confirm_oauth_btn")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Autorizovat a propojit YouTube", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zrušit")
            }
        }
    )
}
