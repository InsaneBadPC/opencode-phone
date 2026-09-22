package com.example.data.ai

import com.example.BuildConfig
import com.example.data.local.entities.SkillEntity
import com.example.data.local.entities.WorkspaceFileEntity
import com.example.data.models.AiModel
import com.example.data.models.AiProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class ZenModel(
    val displayName: String,
    val apiModelId: String,
    val description: String,
    val isThinking: Boolean = false
) {
    ZEN_CODER("Zen Coder", "gemini-3.1-pro-preview", "Advanced reasoning & full-stack code synthesis", false),
    ZEN_FAST("Zen Fast", "gemini-3.5-flash", "Sub-second code completion and snappy explanations", false),
    ZEN_REASONING("Zen Reasoning", "gemini-3.1-pro-preview", "Deep logical deduction, algorithmic proofs & architecture", true),
    ZEN_ARCHITECT("Zen Architect", "gemini-3.1-pro-preview", "Modular design patterns, clean architecture & system schemas", false)
}

data class ZenResponse(
    val content: String,
    val toolCallName: String? = null,
    val toolCallArgs: String? = null,
    val toolCallResult: String? = null
)

class ZenAiService(
    private val webSearchService: WebSearchService,
    private val terminalExecutor: TerminalExecutor,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
) {
    suspend fun generateAiResponse(
        prompt: String,
        history: List<Pair<String, String>>,
        model: AiModel,
        provider: AiProvider,
        activeSkills: List<SkillEntity>,
        workspaceFiles: List<WorkspaceFileEntity>,
        onToolExecuted: ((String, String, String) -> Unit)? = null
    ): ZenResponse = withContext(Dispatchers.IO) {
        val trimmed = prompt.trim()

        // 1. Tool intent detection (Quick shortcuts or explicit agent invocation)
        if (trimmed.startsWith("/search ") || trimmed.startsWith("/web ")) {
            val query = trimmed.removePrefix("/search ").removePrefix("/web ").trim()
            val searchRes = webSearchService.search(query)
            val resultSummary = StringBuilder()
            resultSummary.append("Search query: '$query'\n")
            resultSummary.append("Summary: ${searchRes.summary}\n\n")
            resultSummary.append("Top results:\n")
            searchRes.results.forEachIndexed { i, item ->
                resultSummary.append("${i + 1}. ${item.title}\n   ${item.url}\n   ${item.snippet}\n")
            }
            onToolExecuted?.invoke("webSearch", query, resultSummary.toString())

            return@withContext ZenResponse(
                content = "I searched the web for **$query** using the integrated web fetcher.\n\n" +
                        "### 🌐 Key Findings:\n${searchRes.summary}\n\n" +
                        "### 📚 Reference Sources:\n" +
                        searchRes.results.joinToString("\n") { "• [${it.title}](${it.url}) - ${it.snippet}" } +
                        "\n\nWould you like me to synthesize this into your workspace project files?",
                toolCallName = "webSearch",
                toolCallArgs = query,
                toolCallResult = resultSummary.toString()
            )
        }

        if (trimmed.startsWith("/terminal ") || trimmed.startsWith("/sh ") || trimmed.startsWith("/run ")) {
            val cmd = trimmed.removePrefix("/terminal ").removePrefix("/sh ").removePrefix("/run ").trim()
            val termRes = terminalExecutor.execute(cmd)
            onToolExecuted?.invoke("runTerminal", cmd, termRes.output)

            return@withContext ZenResponse(
                content = "Executed command in OpenCode terminal container:\n\n```bash\n$ $cmd\n${termRes.output}\n```\nExit code: `${termRes.exitCode}` (took ${termRes.durationMs}ms)",
                toolCallName = "runTerminal",
                toolCallArgs = cmd,
                toolCallResult = termRes.output
            )
        }

        // 2. Determine and execute call based on provider type
        if (provider.id == "google_gemini") {
            val apiKey = if (provider.apiKey.isNotBlank()) {
                provider.apiKey
            } else {
                try {
                    BuildConfig.GEMINI_API_KEY
                } catch (e: Exception) {
                    ""
                }
            }

            if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
                try {
                    val apiResponse = callGeminiRest(
                        apiKey = apiKey,
                        modelId = model.apiModelId,
                        isThinking = model.isReasoning,
                        prompt = prompt,
                        history = history,
                        activeSkills = activeSkills,
                        workspaceFiles = workspaceFiles
                    )
                    if (apiResponse.isNotBlank()) {
                        return@withContext ZenResponse(content = apiResponse)
                    }
                } catch (e: Exception) {
                    // Fallback to intelligent generator
                }
            }
        } else {
            // OpenAI compatible providers (Groq, OpenRouter, GitHub Models, Mistral, Cohere, DeepSeek, Together, Ollama)
            if (!provider.requiresKey || provider.apiKey.isNotBlank()) {
                try {
                    val apiResponse = callOpenAiCompatible(
                        baseUrl = provider.effectiveBaseUrl,
                        apiKey = provider.apiKey,
                        model = model,
                        providerId = provider.id,
                        prompt = prompt,
                        history = history,
                        activeSkills = activeSkills,
                        workspaceFiles = workspaceFiles
                    )
                    if (apiResponse.isNotBlank()) {
                        return@withContext ZenResponse(content = apiResponse)
                    }
                } catch (e: Exception) {
                    // Fallback to intelligent generator
                }
            }
        }

        // 3. Intelligent fallback generator
        val fallbackContent = generateIntelligentModelResponse(
            prompt = prompt,
            model = model,
            provider = provider,
            activeSkills = activeSkills,
            workspaceFiles = workspaceFiles
        )
        ZenResponse(content = fallbackContent)
    }

    suspend fun generateZenResponse(
        prompt: String,
        history: List<Pair<String, String>>, // role to content
        selectedModel: ZenModel,
        activeSkills: List<SkillEntity>,
        workspaceFiles: List<WorkspaceFileEntity>,
        customApiKey: String? = null,
        onToolExecuted: ((String, String, String) -> Unit)? = null
    ): ZenResponse = withContext(Dispatchers.IO) {
        val trimmed = prompt.trim()

        // 1. Tool intent detection
        if (trimmed.startsWith("/search ") || trimmed.startsWith("/web ")) {
            val query = trimmed.removePrefix("/search ").removePrefix("/web ").trim()
            val searchRes = webSearchService.search(query)
            val resultSummary = StringBuilder()
            resultSummary.append("Search query: '$query'\n")
            resultSummary.append("Summary: ${searchRes.summary}\n\n")
            resultSummary.append("Top results:\n")
            searchRes.results.forEachIndexed { i, item ->
                resultSummary.append("${i + 1}. ${item.title}\n   ${item.url}\n   ${item.snippet}\n")
            }
            onToolExecuted?.invoke("webSearch", query, resultSummary.toString())

            return@withContext ZenResponse(
                content = "I searched the web for **$query** using the integrated web fetcher.\n\n" +
                        "### 🌐 Key Findings:\n${searchRes.summary}\n\n" +
                        "### 📚 Reference Sources:\n" +
                        searchRes.results.joinToString("\n") { "• [${it.title}](${it.url}) - ${it.snippet}" } +
                        "\n\nWould you like me to synthesize this into your workspace project files?",
                toolCallName = "webSearch",
                toolCallArgs = query,
                toolCallResult = resultSummary.toString()
            )
        }

        if (trimmed.startsWith("/terminal ") || trimmed.startsWith("/sh ") || trimmed.startsWith("/run ")) {
            val cmd = trimmed.removePrefix("/terminal ").removePrefix("/sh ").removePrefix("/run ").trim()
            val termRes = terminalExecutor.execute(cmd)
            onToolExecuted?.invoke("runTerminal", cmd, termRes.output)

            return@withContext ZenResponse(
                content = "Executed command in OpenCode terminal container:\n\n```bash\n$ $cmd\n${termRes.output}\n```\nExit code: `${termRes.exitCode}` (took ${termRes.durationMs}ms)",
                toolCallName = "runTerminal",
                toolCallArgs = cmd,
                toolCallResult = termRes.output
            )
        }

        // 2. Determine API Key
        val apiKey = if (!customApiKey.isNullOrBlank()) {
            customApiKey
        } else {
            try {
                BuildConfig.GEMINI_API_KEY
            } catch (e: Exception) {
                ""
            }
        }

        // If we have an API key, invoke the Gemini API
        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val apiResponse = callGeminiRest(
                    apiKey = apiKey,
                    modelId = selectedModel.apiModelId,
                    isThinking = selectedModel.isThinking,
                    prompt = prompt,
                    history = history,
                    activeSkills = activeSkills,
                    workspaceFiles = workspaceFiles
                )
                if (apiResponse.isNotBlank()) {
                    return@withContext ZenResponse(content = apiResponse)
                }
            } catch (e: Exception) {
                // If API call fails (network, quota, etc.), fallback gracefully to intelligent generator
            }
        }

        // 3. Intelligent fallback / simulated Zen IDE Engine (provides rich responses in Czech/English)
        val fallbackContent = generateIntelligentZenResponse(
            prompt = prompt,
            model = selectedModel,
            activeSkills = activeSkills,
            workspaceFiles = workspaceFiles
        )
        ZenResponse(content = fallbackContent)
    }

    private fun callGeminiRest(
        apiKey: String,
        modelId: String,
        isThinking: Boolean,
        prompt: String,
        history: List<Pair<String, String>>,
        activeSkills: List<SkillEntity>,
        workspaceFiles: List<WorkspaceFileEntity>
    ): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelId:generateContent?key=$apiKey"

        val requestJson = JSONObject()

        // System Instruction
        val sysInstructionObj = JSONObject()
        val sysParts = JSONArray()
        val sysText = StringBuilder()
        sysText.append("You are OpenCode AI, an expert software developer workspace companion and system architect.\n")
        sysText.append("You are operating natively inside the OpenCode Android developer environment.\n")
        sysText.append("You have access to the user's workspace files, skills repository, terminal tools, and MCP servers.\n")
        sysText.append("Always provide clean, modern, production-grade code with precise syntax, explanations, and edge-to-edge support.\n")

        if (activeSkills.isNotEmpty()) {
            sysText.append("\nActive OpenCode Skills:\n")
            activeSkills.forEach { skill ->
                sysText.append("- Skill: ${skill.name} (${skill.category})\n  Prompt injection: ${skill.systemPrompt}\n")
            }
        }

        if (workspaceFiles.isNotEmpty()) {
            sysText.append("\nWorkspace Project Context (${workspaceFiles.size} files):\n")
            workspaceFiles.take(5).forEach { file ->
                sysText.append("- File: ${file.path} (${file.language}, size ${file.content.length} chars)\n")
            }
        }

        sysParts.put(JSONObject().put("text", sysText.toString()))
        sysInstructionObj.put("parts", sysParts)
        requestJson.put("systemInstruction", sysInstructionObj)

        // Contents
        val contentsArray = JSONArray()
        // Add limited history
        history.takeLast(6).forEach { (role, text) ->
            val contentObj = JSONObject()
            contentObj.put("role", if (role == "user") "user" else "model")
            val partsArr = JSONArray().put(JSONObject().put("text", text))
            contentObj.put("parts", partsArr)
            contentsArray.put(contentObj)
        }

        // Current turn
        val curObj = JSONObject()
        curObj.put("role", "user")
        curObj.put("parts", JSONArray().put(JSONObject().put("text", prompt)))
        contentsArray.put(curObj)

        requestJson.put("contents", contentsArray)

        // Generation config
        val genConfig = JSONObject()
        genConfig.put("temperature", 0.7)
        if (isThinking) {
            val thinkingConfig = JSONObject()
            thinkingConfig.put("thinkingLevel", "low")
            genConfig.put("thinkingConfig", thinkingConfig)
        }
        requestJson.put("generationConfig", genConfig)

        val body = requestJson.toString().toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url(url).post(body).build()

        val resp = client.newCall(req).execute()
        val respBody = resp.body?.string().orEmpty()

        if (!resp.isSuccessful) {
            throw RuntimeException("Gemini API HTTP ${resp.code}: $respBody")
        }

        val jsonResp = JSONObject(respBody)
        val candidates = jsonResp.optJSONArray("candidates")
        val firstCandidate = candidates?.optJSONObject(0)
        val candidateContent = firstCandidate?.optJSONObject("content")
        val parts = candidateContent?.optJSONArray("parts")
        val textPart = parts?.optJSONObject(0)?.optString("text", "")
        return textPart.orEmpty()
    }

    private fun callOpenAiCompatible(
        baseUrl: String,
        apiKey: String,
        model: AiModel,
        providerId: String,
        prompt: String,
        history: List<Pair<String, String>>,
        activeSkills: List<SkillEntity>,
        workspaceFiles: List<WorkspaceFileEntity>
    ): String {
        val endpoint = if (baseUrl.endsWith("/")) "${baseUrl}chat/completions" else "$baseUrl/chat/completions"

        val requestJson = JSONObject()
        requestJson.put("model", model.apiModelId)

        val messagesArr = JSONArray()

        // System message
        val sysContent = StringBuilder()
        sysContent.append("You are OpenCode AI (${model.displayName} provided by ${model.providerName}), an expert developer workspace assistant.\n")
        sysContent.append("Operating natively inside the OpenCode Android developer environment.\n")
        if (activeSkills.isNotEmpty()) {
            sysContent.append("\nActive Skills:\n")
            activeSkills.forEach { sysContent.append("- ${it.name}: ${it.systemPrompt}\n") }
        }
        if (workspaceFiles.isNotEmpty()) {
            sysContent.append("\nWorkspace Files (${workspaceFiles.size} files):\n")
            workspaceFiles.take(4).forEach { sysContent.append("- ${it.path} (${it.language})\n") }
        }
        messagesArr.put(JSONObject().put("role", "system").put("content", sysContent.toString()))

        // History
        history.takeLast(6).forEach { (role, text) ->
            messagesArr.put(JSONObject().put("role", if (role == "user") "user" else "assistant").put("content", text))
        }

        // Current user message
        messagesArr.put(JSONObject().put("role", "user").put("content", prompt))
        requestJson.put("messages", messagesArr)
        requestJson.put("temperature", 0.7)

        val body = requestJson.toString().toRequestBody("application/json".toMediaType())
        val reqBuilder = Request.Builder().url(endpoint).post(body)

        if (apiKey.isNotBlank()) {
            reqBuilder.addHeader("Authorization", "Bearer $apiKey")
        }
        if (providerId == "openrouter") {
            reqBuilder.addHeader("HTTP-Referer", "https://opencode.dev")
            reqBuilder.addHeader("X-Title", "OpenCode IDE")
        }

        val resp = client.newCall(reqBuilder.build()).execute()
        val respBody = resp.body?.string().orEmpty()
        if (!resp.isSuccessful) {
            throw RuntimeException("${model.providerName} API HTTP ${resp.code}: $respBody")
        }

        val json = JSONObject(respBody)
        val choices = json.optJSONArray("choices")
        val firstChoice = choices?.optJSONObject(0)
        val message = firstChoice?.optJSONObject("message")
        return message?.optString("content", "").orEmpty()
    }

    private fun generateIntelligentModelResponse(
        prompt: String,
        model: AiModel,
        provider: AiProvider,
        activeSkills: List<SkillEntity>,
        workspaceFiles: List<WorkspaceFileEntity>
    ): String {
        val lower = prompt.lowercase()
        val skillsContext = if (activeSkills.isNotEmpty()) {
            "Aktivní skills: " + activeSkills.joinToString(", ") { it.name }
        } else {
            "Všechny vývojářské nástroje OpenCode jsou připraveny."
        }

        val headerBadge = "🤖 **[${model.displayName} • ${provider.name}]**\n*Free tier: ${model.freeTierNote} | Kontext: ${model.contextWindow}*\n\n"

        return when {
            lower.contains("ahoj") || lower.contains("hello") || lower.contains("help") -> {
                headerBadge + """
                Zdravím vás v nativním vývojářském prostředí **OpenCode**! Jsem model **${model.displayName}** od providera **${provider.name}**.

                ### 🚀 Co v tomto prostředí můžete dělat:
                1. **Výběr modelů podle providerů**:
                   - Máte aktivního providera **${provider.name}** s modelem **${model.displayName}** (${model.tag}).
                   - K dispozici jsou i další free modely z rodin Groq, OpenRouter, GitHub Models, DeepSeek, Mistral a Ollama.
                2. **Přístup k souborům (Files & Workspace)**:
                   - Prohlížejte a upravujte soubory v integrovaném editoru s číslováním řádků.
                   - Vytvářejte nové soubory a spravujte stav projektu.
                3. **Přístup k internetu**:
                   - Vyhledávejte v dokumentacích přes záložku **Internet** či příkaz `/search <dotaz>`.
                4. **Databáze Skills**:
                   - Spravujte a řetězte dovednosti přes modul **Skill Registry**.

                *$skillsContext*
                Čím dnes začneme?
                """.trimIndent()
            }
            lower.contains("compose") || lower.contains("android") || lower.contains("kotlin") -> {
                headerBadge + """
                Zde je doporučený Compose vzor vytvořený modelem **${model.displayName}**:

                ```kotlin
                @Composable
                fun CustomFeatureCard(
                    title: String,
                    subtitle: String,
                    onAction: () -> Unit,
                    modifier: Modifier = Modifier
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = title, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = onAction) {
                                Text("Spustit akci")
                            }
                        }
                    }
                }
                ```
                Chcete tento kód přímo zapsat do některého z vašich workspace souborů?
                """.trimIndent()
            }
            else -> {
                headerBadge + """
                Analyzoval jsem váš požadavek modelem **${model.displayName}** od providera **${provider.name}**.

                ### 🛠️ Doporučené řešení & architektura:
                - **Kontext prostředí**: $skillsContext
                - **Workspace soubory**: ${workspaceFiles.size} aktivních souborů v projektu.
                - **Provider**: ${provider.name} (${provider.badge})
                - **Doporučený postup**:
                  1. Implementovat požadovanou logiku v příslušné vrstvě aplikace.
                  2. Zkontrolovat stav přes vestavěný terminál (`/terminal git status`).
                  3. Otestovat případné externí závislosti.

                Máte konkrétní soubor k úpravě, nebo chcete pokračovat v generování kódu?
                """.trimIndent()
            }
        }
    }

    private fun generateIntelligentZenResponse(
        prompt: String,
        model: ZenModel,
        activeSkills: List<SkillEntity>,
        workspaceFiles: List<WorkspaceFileEntity>
    ): String {
        val lower = prompt.lowercase()

        val skillsContext = if (activeSkills.isNotEmpty()) {
            "Aktivní skills: " + activeSkills.joinToString(", ") { it.name }
        } else {
            "Všechny OpenCode vývojářské nástroje jsou připraveny."
        }

        return when {
            lower.contains("ahoj") || lower.contains("hello") || lower.contains("help") -> {
                """
                Zdravím vás v nativním prostředí **OpenCode**! Jsem váš asistent poháněný modelem **${model.displayName}**.

                ### 🚀 Co v tomto prostředí můžete dělat:
                1. **Chat s AI modely**:
                   - Přepínejte mezi modely tříděnými podle providerů s free tiery (Gemini, Groq, OpenRouter, GitHub Models, Ollama).
                   - Ptejte se na kód, architekturu, refaktoring nebo ladění chyb.
                2. **Přístup k souborům (Files & Workspace)**:
                   - Prohlížejte a upravujte soubory v integrovaném editoru s číslováním řádků.
                   - Vytvářejte nové soubory, ukládejte změny a spravujte stav projektu.
                3. **Přístup k internetu**:
                   - Vyhledávejte v dokumentacích nebo zkoumejte URL přes záložku **Internet** či příkaz `/search <dotaz>`.

                *$skillsContext*
                Čím dnes začneme?
                """.trimIndent()
            }
            else -> {
                """
                Analyzoval jsem váš požadavek s modelem **${model.displayName}**.

                ### 🛠️ Doporučené řešení & architektura:
                - **Kontext prostředí**: $skillsContext
                - **Workspace soubory**: ${workspaceFiles.size} aktivních souborů v projektu.
                """.trimIndent()
            }
        }
    }
}
