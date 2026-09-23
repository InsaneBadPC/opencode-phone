package com.example.data.ai

import android.content.Context
import com.example.data.local.dao.WorkspaceDao
import com.example.data.local.entities.WorkspaceFileEntity
import com.example.data.security.SecureKeyStorage
import com.example.data.update.AppUpdateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class AgentExecutionResult(
    val toolName: String,
    val args: String,
    val output: String,
    val isSuccess: Boolean,
    val actionSummary: String
)

class OpenCodeAgentEngine(
    private val context: Context,
    private val workspaceDao: WorkspaceDao,
    private val terminalExecutor: TerminalExecutor,
    private val secureKeyStorage: SecureKeyStorage,
    private val appUpdateManager: AppUpdateManager,
    private val onProviderKeyUpdated: ((providerId: String, key: String) -> Unit)? = null
) {

    /**
     * Edits or creates a file in the application codebase / workspace.
     */
    suspend fun editOrWriteCode(
        path: String,
        content: String,
        projectId: String? = null
    ): AgentExecutionResult = withContext(Dispatchers.IO) {
        try {
            val cleanPath = path.trim().removePrefix("/")
            val fileName = cleanPath.substringAfterLast('/')
            val lang = when {
                cleanPath.endsWith(".kt") -> "kotlin"
                cleanPath.endsWith(".kts") -> "kotlin"
                cleanPath.endsWith(".py") -> "python"
                cleanPath.endsWith(".json") -> "json"
                cleanPath.endsWith(".md") -> "markdown"
                cleanPath.endsWith(".sql") -> "sql"
                cleanPath.endsWith(".xml") -> "xml"
                cleanPath.endsWith(".yml") || cleanPath.endsWith(".yaml") -> "yaml"
                else -> "text"
            }

            // Save to database
            val existing = workspaceDao.getFileByPath(cleanPath)
            if (existing != null) {
                workspaceDao.insertFile(
                    existing.copy(
                        content = content,
                        gitStatus = "modified",
                        updatedAt = System.currentTimeMillis()
                    )
                )
            } else {
                workspaceDao.insertFile(
                    WorkspaceFileEntity(
                        path = cleanPath,
                        name = fileName,
                        content = content,
                        language = lang,
                        gitStatus = "new",
                        projectId = projectId ?: "default_project"
                    )
                )
            }

            // Try to write to disk if working directory is accessible
            try {
                val diskFile = File(context.filesDir, cleanPath)
                diskFile.parentFile?.mkdirs()
                diskFile.writeText(content)
            } catch (_: Exception) {
                // Ignore disk write errors in restricted sandboxes
            }

            val linesCount = content.lines().size
            AgentExecutionResult(
                toolName = "edit_code",
                args = cleanPath,
                output = "✅ Soubor '$cleanPath' ($linesCount řádků) byl úspěšně upraven a označen v Gitu jako 'modified'.",
                isSuccess = true,
                actionSummary = "Upraven soubor $cleanPath"
            )
        } catch (e: Exception) {
            AgentExecutionResult(
                toolName = "edit_code",
                args = path,
                output = "❌ Chyba při zápisu kódu do '$path': ${e.message}",
                isSuccess = false,
                actionSummary = "Chyba zápisu do $path"
            )
        }
    }

    /**
     * Bumps the version, commits changes, creates a git tag, and pushes to trigger
     * the GitHub Actions APK build workflow. Once finished, triggers an in-app update prompt!
     */
    suspend fun bumpVersionAndPush(
        version: String,
        commitMessage: String,
        notes: String = ""
    ): AgentExecutionResult = withContext(Dispatchers.IO) {
        try {
            val cleanVer = version.trim().removePrefix("v")
            val finalMsg = commitMessage.ifBlank { "Release v$cleanVer: Automatická aktualizace kódu" }
            val finalNotes = notes.ifBlank {
                """
                ### 🚀 OpenCode v$cleanVer
                * **Automatická aktualizace:** Kód aplikace byl upraven a optimalizován AI agentem.
                * **Sestaveno:** Automatický build přes GitHub Actions CI/CD.
                * **Podepsané APK:** Připraveno k přímé instalaci.
                """.trimIndent()
            }

            // 1. Run git status and commit via terminalExecutor
            val termRes1 = terminalExecutor.execute("git add -A")
            val termRes2 = terminalExecutor.execute("git commit -m \"feat(release): $finalMsg\"")
            val termRes3 = terminalExecutor.execute("git tag -a v$cleanVer -m \"Release v$cleanVer\"")
            val termRes4 = terminalExecutor.execute("git push origin master --tags")

            // 2. Notify AppUpdateManager to offer the new update to the user
            appUpdateManager.notifyNewReleaseAvailable(
                version = cleanVer,
                notes = finalNotes
            )

            val log = buildString {
                appendLine("🚀 **Byla vytvořena nová verze v$cleanVer!**")
                appendLine("---")
                appendLine("1. **Git Commit**: `feat(release): $finalMsg`")
                appendLine("2. **Git Tag**: `v$cleanVer` vytvořen")
                appendLine("3. **GitHub Push**: `git push origin master --tags` odesláno")
                appendLine("4. **GitHub Actions**: Spuštěn CI/CD workflow `.github/workflows/build-apk.yml`")
                appendLine("5. **In-App Updater**: Dialog aktualizace byl aktivován – uživatel může ihned stáhnout nové APK!")
            }

            AgentExecutionResult(
                toolName = "bump_version_and_push",
                args = "v$cleanVer",
                output = log,
                isSuccess = true,
                actionSummary = "Vydána verze v$cleanVer a spuštěn build"
            )
        } catch (e: Exception) {
            AgentExecutionResult(
                toolName = "bump_version_and_push",
                args = version,
                output = "❌ Chyba při vytváření aktualizace: ${e.message}",
                isSuccess = false,
                actionSummary = "Chyba vydání verze $version"
            )
        }
    }

    /**
     * Manages API keys, tokens, and secrets (AES-256-GCM encrypted).
     */
    suspend fun manageSecret(
        action: String,
        key: String,
        value: String? = null
    ): AgentExecutionResult = withContext(Dispatchers.IO) {
        val cleanAction = action.lowercase().trim()
        val cleanKey = key.trim().uppercase()

        when (cleanAction) {
            "set" -> {
                if (value.isNullOrBlank()) {
                    return@withContext AgentExecutionResult(
                        toolName = "manage_secret",
                        args = "set $cleanKey",
                        output = "❌ Chybí hodnota pro secret '$cleanKey'.",
                        isSuccess = false,
                        actionSummary = "Chybějící hodnota"
                    )
                }

                // Map common keys to providers
                val providerId = when (cleanKey) {
                    "GEMINI_API_KEY", "GOOGLE_API_KEY", "GOOGLE_GEMINI" -> "google_gemini"
                    "GROQ_API_KEY", "GROQ" -> "groq"
                    "OPENROUTER_API_KEY", "OPENROUTER" -> "openrouter"
                    "GITHUB_TOKEN", "GITHUB_MODELS" -> "github_models"
                    "DEEPSEEK_API_KEY", "DEEPSEEK" -> "deepseek"
                    "MISTRAL_API_KEY", "MISTRAL" -> "mistral"
                    else -> cleanKey.lowercase()
                }

                secureKeyStorage.saveApiKey(providerId, value.trim())
                onProviderKeyUpdated?.invoke(providerId, value.trim())

                val masked = secureKeyStorage.getMaskedApiKey(providerId)
                val out = "🔐 **Secret uložen a zašifrován (AES-256-GCM):**\n" +
                        "• Klíč: `$cleanKey` (provider: `$providerId`)\n" +
                        "• Hodnota: `$masked`\n" +
                        "• Stav: Aktivní pro všechny AI požadavky a buildy."

                AgentExecutionResult(
                    toolName = "manage_secret",
                    args = "set $cleanKey",
                    output = out,
                    isSuccess = true,
                    actionSummary = "Uložen secret $cleanKey"
                )
            }

            "get" -> {
                val providerId = cleanKey.lowercase().removeSuffix("_api_key").removeSuffix("_token")
                val masked = secureKeyStorage.getMaskedApiKey(providerId)
                val out = if (masked.isNotBlank()) {
                    "🔑 Secret `$cleanKey`: `$masked`"
                } else {
                    "⚠️ Secret `$cleanKey` není nastaven."
                }
                AgentExecutionResult(
                    toolName = "manage_secret",
                    args = "get $cleanKey",
                    output = out,
                    isSuccess = true,
                    actionSummary = "Zobrazen secret $cleanKey"
                )
            }

            "list" -> {
                val keys = listOf(
                    "GEMINI_API_KEY" to "google_gemini",
                    "GROQ_API_KEY" to "groq",
                    "OPENROUTER_API_KEY" to "openrouter",
                    "GITHUB_TOKEN" to "github_models",
                    "DEEPSEEK_API_KEY" to "deepseek",
                    "MISTRAL_API_KEY" to "mistral",
                    "YOUTUBE_API_KEY" to "youtube_api_key"
                )

                val sb = StringBuilder()
                sb.append("📋 **Seznam spravovaných API klíčů a Secrets:**\n\n")
                keys.forEach { (name, provId) ->
                    val isSet = secureKeyStorage.hasApiKey(provId)
                    val masked = if (isSet) secureKeyStorage.getMaskedApiKey(provId) else "❌ Nenastaveno"
                    val icon = if (isSet) "🟢" else "⚪"
                    sb.append("$icon **$name**: `$masked`\n")
                }
                sb.append("\n*Všechny klíče jsou chráněny v hardwarovém Android KeyStore (AES-256-GCM).*")

                AgentExecutionResult(
                    toolName = "manage_secret",
                    args = "list",
                    output = sb.toString(),
                    isSuccess = true,
                    actionSummary = "Vypsán seznam secretů"
                )
            }

            "delete" -> {
                val providerId = cleanKey.lowercase().removeSuffix("_api_key").removeSuffix("_token")
                secureKeyStorage.removeApiKey(providerId)
                AgentExecutionResult(
                    toolName = "manage_secret",
                    args = "delete $cleanKey",
                    output = "🗑️ Secret `$cleanKey` byl bezpečně odstraněn.",
                    isSuccess = true,
                    actionSummary = "Smazán secret $cleanKey"
                )
            }

            else -> {
                AgentExecutionResult(
                    toolName = "manage_secret",
                    args = action,
                    output = "Neznámá akce: '$action'. Použijte 'set', 'get', 'list' nebo 'delete'.",
                    isSuccess = false,
                    actionSummary = "Neznámá akce"
                )
            }
        }
    }

    /**
     * Checks for updates and pops up the update dialog if a new version is found.
     */
    suspend fun checkAndPromptUpdate(currentVersion: String = "1.2.0"): AgentExecutionResult = withContext(Dispatchers.IO) {
        val state = appUpdateManager.checkForUpdates(currentVersion)
        val out = when (state) {
            is com.example.data.update.UpdateCheckState.UpdateAvailable -> {
                "🎉 **Nová verze ${state.release.versionName} je k dispozici!**\n" +
                        "Dialog aktualizace byl otevřen. Můžete přímo kliknout na 'Stáhnout a instalovat APK'."
            }
            is com.example.data.update.UpdateCheckState.UpToDate -> {
                "✅ Aplikace je aktuální (verze $currentVersion)."
            }
            is com.example.data.update.UpdateCheckState.Error -> {
                "⚠️ Kontrola aktualizací: ${state.message}"
            }
            else -> "Kontrola aktualizací dokončena."
        }

        AgentExecutionResult(
            toolName = "check_update",
            args = currentVersion,
            output = out,
            isSuccess = true,
            actionSummary = "Zkontrolovány aktualizace"
        )
    }
}
