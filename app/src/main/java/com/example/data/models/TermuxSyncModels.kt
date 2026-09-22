package com.example.data.models

enum class TermuxSyncMode(val displayName: String) {
    HTTP_BRIDGE("HTTP Daemon (127.0.0.1:4096)"),
    SHARED_STORAGE("Sdcard / Storage (~/.opencode)"),
    BIDIRECTIONAL("Plně obousměrná (Live Mirror)")
}

enum class TermuxSyncState {
    IDLE,
    SYNCING,
    CONNECTED,
    OFFLINE,
    ERROR
}

data class TermuxSyncConfig(
    val isAutoSyncEnabled: Boolean = true,
    val host: String = "127.0.0.1",
    val port: Int = 4096,
    val syncIntervalSeconds: Int = 30,
    val syncMode: TermuxSyncMode = TermuxSyncMode.HTTP_BRIDGE,
    val termuxWorkingDir: String = "/data/data/com.termux/files/home/.opencode/sessions",
    val autoCreateProjectForTermux: Boolean = true
)

data class TermuxSyncStatus(
    val state: TermuxSyncState = TermuxSyncState.CONNECTED,
    val lastSyncTimestamp: Long = System.currentTimeMillis(),
    val syncedSessionsCount: Int = 0,
    val totalTermuxSessionsFound: Int = 0,
    val lastErrorMessage: String? = null,
    val activeDaemonPort: Int = 4096,
    val daemonVersion: String = "opencode-cli v1.8.4",
    val isLiveWatching: Boolean = true
)

data class TermuxSessionDto(
    val id: String,
    val title: String,
    val createdAt: Long,
    val modelName: String,
    val projectId: String? = null,
    val workingDirectory: String = "~/workspace",
    val messages: List<TermuxMessageDto> = emptyList(),
    val activeSkillIds: String = ""
)

data class TermuxMessageDto(
    val role: String, // "user", "assistant", "system", "tool"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val toolName: String? = null,
    val toolArgs: String? = null,
    val toolResult: String? = null
)
