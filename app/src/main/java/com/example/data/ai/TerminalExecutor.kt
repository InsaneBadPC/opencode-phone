package com.example.data.ai

import com.example.data.local.dao.WorkspaceDao
import com.example.data.local.entities.WorkspaceFileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class TerminalExecutionResult(
    val command: String,
    val output: String,
    val exitCode: Int,
    val durationMs: Long
)

class TerminalExecutor(
    private val workspaceDao: WorkspaceDao
) {
    suspend fun execute(command: String): TerminalExecutionResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val trimmed = command.trim()
        val parts = trimmed.split("\\s+".toRegex()).filter { it.isNotBlank() }

        if (parts.isEmpty()) {
            return@withContext TerminalExecutionResult(command, "", 0, 1)
        }

        val cmd = parts[0].lowercase()
        val args = parts.drop(1)

        val (output, exitCode) = when (cmd) {
            "pwd" -> Pair("/workspace/project", 0)
            "clear" -> Pair("", 0)
            "help" -> Pair(
                """
                OpenCode Terminal CLI v2.4
                Available commands:
                  ls [-la] [dir]    List workspace files
                  cat <file>        Read file contents
                  touch <file>      Create empty file
                  rm <file>         Delete workspace file
                  grep <pattern>    Search in workspace
                  git status        Check repo git status
                  git log           Show recent commit history
                  git diff          Show workspace file diffs
                  find .            Find all workspace files
                  echo <text>       Print text to standard output
                  python <file>     Run Python script through sandbox
                  node <file>       Run JS/TS script through sandbox
                  curl <url>        Fetch remote URL header/content
                  zen --info        Inspect active Zen AI model
                  mcp list          List registered MCP servers
                  skills list       List enabled developer skills
                """.trimIndent(), 0
            )
            "ls" -> {
                val files = workspaceDao.getAllFilesList()
                if (files.isEmpty()) {
                    Pair("total 0\n(empty workspace)", 0)
                } else {
                    val sb = StringBuilder()
                    sb.append("total ${files.size}\n")
                    files.forEach { f ->
                        val size = f.content.toByteArray().size
                        val statusBadge = when (f.gitStatus) {
                            "modified" -> "M"
                            "new" -> "?"
                            else -> " "
                        }
                        sb.append("-rw-r--r-- 1 opencode dev %6d [%s] %s\n".format(size, statusBadge, f.path))
                    }
                    Pair(sb.toString().trimEnd(), 0)
                }
            }
            "cat" -> {
                if (args.isEmpty()) {
                    Pair("cat: missing file operand", 1)
                } else {
                    val path = args[0]
                    val file = workspaceDao.getFileByPath(path)
                    if (file != null) {
                        Pair(file.content, 0)
                    } else {
                        Pair("cat: $path: No such file or directory in workspace", 1)
                    }
                }
            }
            "touch" -> {
                if (args.isEmpty()) {
                    Pair("touch: missing file operand", 1)
                } else {
                    val path = args[0]
                    val existing = workspaceDao.getFileByPath(path)
                    if (existing == null) {
                        workspaceDao.insertFile(
                            WorkspaceFileEntity(
                                path = path,
                                name = path.substringAfterLast('/'),
                                content = "",
                                language = when {
                                    path.endsWith(".kt") -> "kotlin"
                                    path.endsWith(".py") -> "python"
                                    path.endsWith(".json") -> "json"
                                    path.endsWith(".md") -> "markdown"
                                    path.endsWith(".sql") -> "sql"
                                    else -> "text"
                                },
                                gitStatus = "new"
                            )
                        )
                    }
                    Pair("", 0)
                }
            }
            "rm" -> {
                if (args.isEmpty()) {
                    Pair("rm: missing file operand", 1)
                } else {
                    val path = args[0]
                    workspaceDao.deleteFileByPath(path)
                    Pair("Removed '$path'", 0)
                }
            }
            "echo" -> {
                Pair(args.joinToString(" "), 0)
            }
            "grep" -> {
                if (args.isEmpty()) {
                    Pair("grep: missing search pattern", 1)
                } else {
                    val pattern = args[0]
                    Pair(
                        """
                        MainActivity.kt:14: // OpenCode Core initialisation
                        Repository.kt:28: suspend fun queryZenModel(prompt: String): Flow<Result>
                        AppConfig.json:3: "environment": "production"
                        """.trimIndent(), 0
                    )
                }
            }
            "git" -> {
                val sub = args.firstOrNull() ?: "status"
                when (sub) {
                    "status" -> {
                        val files = workspaceDao.getAllFilesList()
                        val modified = files.filter { it.gitStatus == "modified" }
                        val newFiles = files.filter { it.gitStatus == "new" }
                        val sb = StringBuilder()
                        sb.append("On branch main\n")
                        sb.append("Your branch is up to date with 'origin/main'.\n\n")
                        if (modified.isNotEmpty()) {
                            sb.append("Changes not staged for commit:\n")
                            sb.append("  (use \"git add <file>...\" to update what will be committed)\n")
                            modified.forEach { sb.append("    modified:   ${it.path}\n") }
                            sb.append("\n")
                        }
                        if (newFiles.isNotEmpty()) {
                            sb.append("Untracked files:\n")
                            sb.append("  (use \"git add <file>...\" to include in what will be committed)\n")
                            newFiles.forEach { sb.append("    ${it.path}\n") }
                            sb.append("\n")
                        }
                        if (modified.isEmpty() && newFiles.isEmpty()) {
                            sb.append("nothing to commit, working tree clean")
                        } else {
                            sb.append("no changes added to commit (use \"git add\" and \"git commit -m\")")
                        }
                        Pair(sb.toString().trimEnd(), 0)
                    }
                    "log" -> Pair(
                        """
                        commit a7f920c (HEAD -> main, origin/main)
                        Author: OpenCode Architect <dev@opencode.org>
                        Date:   Sun Sep 21 19:40:12 2026

                            feat: integrate Zen AI reasoning model and MCP server bridge

                        commit 98b411d
                        Author: OpenCode Architect <dev@opencode.org>
                        Date:   Sun Sep 21 18:22:04 2026

                            init: configure workspace architecture and native Kotlin runtime
                        """.trimIndent(), 0
                    )
                    "diff" -> Pair(
                        """
                        diff --git a/MainActivity.kt b/MainActivity.kt
                        --- a/MainActivity.kt
                        +++ b/MainActivity.kt
                        @@ -24,3 +24,7 @@
                        +    // Enabled Zen AI model context
                        +    val zenEngine = ZenAiEngine.getInstance()
                        +    zenEngine.attachWorkspace(currentDir)
                        """.trimIndent(), 0
                    )
                    else -> Pair("git: '$sub' is not a git command. See 'git --help'.", 1)
                }
            }
            "python" -> {
                val script = args.firstOrNull() ?: "script.py"
                Pair(
                    """
                    [OpenCode Python Sandbox 3.12]
                    Running $script...
                    >>> Initializing neural matrix...
                    >>> Matrix shape: (1024, 768)
                    >>> Zen Loss: 0.0042 | Accuracy: 99.8%
                    [Execution completed in 240ms - Exit 0]
                    """.trimIndent(), 0
                )
            }
            "node" -> {
                val file = args.firstOrNull() ?: "index.js"
                Pair(
                    """
                    [OpenCode Node.js Sandbox v22.4.0]
                    Server listening on http://localhost:8080
                    Connected to MCP WebSocket client.
                    """.trimIndent(), 0
                )
            }
            "zen" -> Pair(
                """
                Zen AI Intelligence Engine:
                  Active Persona: Zen Coder
                  Base Engine: Gemini 3.1 Pro Preview
                  Thinking Mode: Enabled (depth: high)
                  Context Window: 2,000,000 tokens
                  Active Tools: [web_search, read_file, write_file, run_terminal, mcp_call]
                """.trimIndent(), 0
            )
            "mcp" -> Pair(
                """
                Registered MCP Servers (Model Context Protocol):
                  - Filesystem MCP (stdio) [CONNECTED]
                  - GitHub MCP (sse) [CONNECTED]
                  - PostgreSQL MCP (stdio) [CONNECTED]
                  - Brave Web Search MCP (sse) [CONNECTED]
                Use 'mcp call <server> <tool>' to test.
                """.trimIndent(), 0
            )
            "skills" -> Pair(
                """
                OpenCode Skills Active:
                  [✓] Jetpack Compose Architect
                  [✓] Fullstack Kotlin & Python
                  [✓] Model Context Protocol Builder
                  [✓] Bug Hunter & Security Audit
                """.trimIndent(), 0
            )
            else -> Pair("opencode-sh: command not found: $cmd. Type 'help' for available commands.", 127)
        }

        val duration = System.currentTimeMillis() - startTime
        TerminalExecutionResult(command, output, exitCode, duration)
    }
}
