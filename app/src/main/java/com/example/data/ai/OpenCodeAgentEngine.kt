package com.example.data.ai

import android.content.Context
import com.example.data.local.dao.WorkspaceDao
import com.example.data.local.entities.WorkspaceFileEntity
import com.example.data.security.SecureKeyStorage
import com.example.data.update.AppUpdateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    private val webSearchService: WebSearchService? = null,
    private val onProviderKeyUpdated: ((providerId: String, key: String) -> Unit)? = null
) {

    /**
     * Creates a new file in the workspace database and writes to disk.
     */
    suspend fun createFile(
        path: String,
        content: String,
        projectId: String? = null
    ): AgentExecutionResult = withContext(Dispatchers.IO) {
        try {
            val cleanPath = path.trim().removePrefix("/")
            val fileName = cleanPath.substringAfterLast('/')
            val lang = detectLanguage(cleanPath)

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
                        projectId = projectId ?: "proj_opencode"
                    )
                )
            }

            try {
                val diskFile = File(context.filesDir, cleanPath)
                diskFile.parentFile?.mkdirs()
                diskFile.writeText(content)
            } catch (_: Exception) {}

            val linesCount = content.lines().size
            AgentExecutionResult(
                toolName = "create_file",
                args = cleanPath,
                output = "Vytvořen soubor '$cleanPath' ($linesCount řádků, $lang) a uložen do pracovního prostoru.",
                isSuccess = true,
                actionSummary = "Vytvořen soubor $cleanPath"
            )
        } catch (e: Exception) {
            AgentExecutionResult(
                toolName = "create_file",
                args = path,
                output = "Chyba při vytváření souboru '$path': ${e.message}",
                isSuccess = false,
                actionSummary = "Chyba vytvoření $path"
            )
        }
    }

    /**
     * Edits or updates an existing file in the workspace.
     */
    suspend fun editOrWriteCode(
        path: String,
        content: String,
        projectId: String? = null
    ): AgentExecutionResult = withContext(Dispatchers.IO) {
        try {
            val cleanPath = path.trim().removePrefix("/")
            val fileName = cleanPath.substringAfterLast('/')
            val lang = detectLanguage(cleanPath)

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
                        projectId = projectId ?: "proj_opencode"
                    )
                )
            }

            try {
                val diskFile = File(context.filesDir, cleanPath)
                diskFile.parentFile?.mkdirs()
                diskFile.writeText(content)
            } catch (_: Exception) {}

            val linesCount = content.lines().size
            AgentExecutionResult(
                toolName = "edit_code",
                args = cleanPath,
                output = "Soubor '$cleanPath' ($linesCount řádků) byl úspěšně upraven a uložen.",
                isSuccess = true,
                actionSummary = "Upraven soubor $cleanPath"
            )
        } catch (e: Exception) {
            AgentExecutionResult(
                toolName = "edit_code",
                args = path,
                output = "Chyba při zápisu kódu do '$path': ${e.message}",
                isSuccess = false,
                actionSummary = "Chyba zápisu do $path"
            )
        }
    }

    /**
     * Reads a file from the workspace.
     */
    suspend fun readFile(path: String): AgentExecutionResult = withContext(Dispatchers.IO) {
        val cleanPath = path.trim().removePrefix("/")
        val file = workspaceDao.getFileByPath(cleanPath)
        if (file != null) {
            AgentExecutionResult(
                toolName = "read_file",
                args = cleanPath,
                output = file.content,
                isSuccess = true,
                actionSummary = "Přečten soubor $cleanPath"
            )
        } else {
            AgentExecutionResult(
                toolName = "read_file",
                args = cleanPath,
                output = "Soubor '$cleanPath' nebyl nalezen v pracovním prostoru.",
                isSuccess = false,
                actionSummary = "Soubor $cleanPath nenalezen"
            )
        }
    }

    /**
     * Lists files in workspace.
     */
    suspend fun listFiles(path: String = ""): AgentExecutionResult = withContext(Dispatchers.IO) {
        val files = workspaceDao.getAllFilesList()
        val sb = StringBuilder()
        sb.append("Celkem ${files.size} souborů v projektu:\n")
        files.forEach { f ->
            val size = f.content.toByteArray().size
            val status = when (f.gitStatus) {
                "modified" -> " [modifikováno]"
                "new" -> " [nový]"
                else -> ""
            }
            sb.append("• `${f.path}` (${f.language}, $size B)$status\n")
        }
        AgentExecutionResult(
            toolName = "list_files",
            args = path.ifBlank { "/" },
            output = sb.toString().trimEnd(),
            isSuccess = true,
            actionSummary = "Vypsáno ${files.size} souborů"
        )
    }

    /**
     * Deletes a file from workspace.
     */
    suspend fun deleteFile(path: String): AgentExecutionResult = withContext(Dispatchers.IO) {
        val cleanPath = path.trim().removePrefix("/")
        workspaceDao.deleteFileByPath(cleanPath)
        try {
            File(context.filesDir, cleanPath).delete()
        } catch (_: Exception) {}
        AgentExecutionResult(
            toolName = "delete_file",
            args = cleanPath,
            output = "Soubor '$cleanPath' byl odstraněn z pracovního prostoru.",
            isSuccess = true,
            actionSummary = "Smazán soubor $cleanPath"
        )
    }

    /**
     * Performs a web search.
     */
    suspend fun searchWeb(query: String): AgentExecutionResult = withContext(Dispatchers.IO) {
        if (webSearchService == null) {
            return@withContext AgentExecutionResult(
                toolName = "web_search",
                args = query,
                output = "Webový vyhledávač není k dispozici.",
                isSuccess = false,
                actionSummary = "Chyba hledání na webu"
            )
        }
        val res = webSearchService.search(query)
        val sb = StringBuilder()
        sb.append(res.summary).append("\n\n")
        res.results.take(5).forEachIndexed { index, item ->
            sb.append("${index + 1}. **[${item.title}](${item.url})**\n   ${item.snippet}\n")
        }
        AgentExecutionResult(
            toolName = "web_search",
            args = query,
            output = sb.toString().trimEnd(),
            isSuccess = true,
            actionSummary = "Nalezeno ${res.results.size} výsledků pro '$query'"
        )
    }

    /**
     * Runs terminal command.
     */
    suspend fun runTerminal(command: String): AgentExecutionResult = withContext(Dispatchers.IO) {
        val res = terminalExecutor.execute(command)
        AgentExecutionResult(
            toolName = "run_command",
            args = command,
            output = res.output,
            isSuccess = res.exitCode == 0,
            actionSummary = "Příkaz '$command' (kód ${res.exitCode})"
        )
    }

    /**
     * Bumps the version, commits changes, creates a git tag, and pushes to trigger
     * the GitHub Actions APK build workflow.
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
                * **Aktualizace:** Kód aplikace byl upraven a synchronizován.
                * **Sestavení:** Automatický CI/CD build přes GitHub Actions.
                * **Instalace:** Podepsané APK je připraveno ke stažení.
                """.trimIndent()
            }

            terminalExecutor.execute("git add -A")
            terminalExecutor.execute("git commit -m \"feat(release): $finalMsg\"")
            terminalExecutor.execute("git tag -a v$cleanVer -m \"Release v$cleanVer\"")
            terminalExecutor.execute("git push origin master --tags")

            appUpdateManager.notifyNewReleaseAvailable(
                version = cleanVer,
                notes = finalNotes
            )

            val log = buildString {
                appendLine("🚀 **Byla vytvořena nová verze v$cleanVer**")
                appendLine("• Git commit: `feat(release): $finalMsg`")
                appendLine("• Git tag: `v$cleanVer` vytvořen a pushnut na origin")
                appendLine("• In-App Updater: Dialog aktualizace aktivován pro okamžitou instalaci")
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
                output = "Chyba při vytváření aktualizace: ${e.message}",
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
                        output = "Chybí hodnota pro secret '$cleanKey'.",
                        isSuccess = false,
                        actionSummary = "Chybějící hodnota"
                    )
                }

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
                val out = "Secret `$cleanKey` ($providerId) uložen: `$masked`"

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
                    "Secret `$cleanKey`: `$masked`"
                } else {
                    "Secret `$cleanKey` není nastaven."
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
                sb.append("Spravované API klíče:\n")
                keys.forEach { (name, provId) ->
                    val isSet = secureKeyStorage.hasApiKey(provId)
                    val masked = if (isSet) secureKeyStorage.getMaskedApiKey(provId) else "Nenastaveno"
                    val icon = if (isSet) "✓" else "–"
                    sb.append("• [$icon] $name: `$masked`\n")
                }

                AgentExecutionResult(
                    toolName = "manage_secret",
                    args = "list",
                    output = sb.toString().trimEnd(),
                    isSuccess = true,
                    actionSummary = "Seznam secretů"
                )
            }

            "delete" -> {
                val providerId = cleanKey.lowercase().removeSuffix("_api_key").removeSuffix("_token")
                secureKeyStorage.removeApiKey(providerId)
                AgentExecutionResult(
                    toolName = "manage_secret",
                    args = "delete $cleanKey",
                    output = "Secret `$cleanKey` byl odstraněn.",
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
     * Checks for updates.
     */
    suspend fun checkAndPromptUpdate(currentVersion: String = "1.2.2"): AgentExecutionResult = withContext(Dispatchers.IO) {
        val state = appUpdateManager.checkForUpdates(currentVersion)
        val out = when (state) {
            is com.example.data.update.UpdateCheckState.UpdateAvailable -> {
                "Nová verze ${state.release.versionName} je k dispozici. Dialog aktualizace byl otevřen."
            }
            is com.example.data.update.UpdateCheckState.UpToDate -> {
                "Aplikace je aktuální (verze $currentVersion)."
            }
            is com.example.data.update.UpdateCheckState.Error -> {
                "Kontrola aktualizací: ${state.message}"
            }
            else -> "Kontrola aktualizací dokončena."
        }

        AgentExecutionResult(
            toolName = "check_update",
            args = currentVersion,
            output = out,
            isSuccess = true,
            actionSummary = "Kontrola aktualizací"
        )
    }

    private fun detectLanguage(path: String): String {
        return when {
            path.endsWith(".kt") || path.endsWith(".kts") -> "kotlin"
            path.endsWith(".py") -> "python"
            path.endsWith(".js") -> "javascript"
            path.endsWith(".ts") || path.endsWith(".tsx") -> "typescript"
            path.endsWith(".json") -> "json"
            path.endsWith(".md") -> "markdown"
            path.endsWith(".sql") -> "sql"
            path.endsWith(".xml") -> "xml"
            path.endsWith(".html") -> "html"
            path.endsWith(".css") -> "css"
            path.endsWith(".sh") || path.endsWith(".bash") -> "bash"
            path.endsWith(".yml") || path.endsWith(".yaml") -> "yaml"
            else -> "text"
        }
    }
}
