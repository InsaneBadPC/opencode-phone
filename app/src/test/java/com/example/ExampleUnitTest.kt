package com.example

import com.example.data.models.AiProviderCatalog
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testAiProviderCatalog_hasFreeTierProviders() {
        val providers = AiProviderCatalog.getDefaultProviders()
        assertTrue("Provider catalog must not be empty", providers.isNotEmpty())

        // Check key free tier providers exist
        val providerIds = providers.map { it.id }.toSet()
        assertTrue("Google Gemini must exist", providerIds.contains("google_gemini"))
        assertTrue("Groq must exist", providerIds.contains("groq"))
        assertTrue("OpenRouter must exist", providerIds.contains("openrouter"))
        assertTrue("GitHub Models must exist", providerIds.contains("github_models"))
        assertTrue("Cerebras must exist", providerIds.contains("cerebras"))
        assertTrue("Ollama must exist", providerIds.contains("ollama"))

        // Default availability: Gemini (built-in) and Ollama (local/no key) are available immediately
        val gemini = providers.first { it.id == "google_gemini" }
        assertTrue("Gemini should be available by default", gemini.isAvailable)

        val ollama = providers.first { it.id == "ollama" }
        assertTrue("Ollama without key should be available", ollama.isAvailable)

        // Groq without key should NOT be available in model picker
        val groq = providers.first { it.id == "groq" }
        assertFalse("Groq without key should not be available", groq.isAvailable)

        // Groq with key configured should be available
        val groqWithKey = groq.copy(apiKey = "gsk_test_key_123")
        assertTrue("Groq with key should be available", groqWithKey.isAvailable)
    }

    @Test
    fun testAvailableModelsFiltering() {
        val providers = AiProviderCatalog.getDefaultProviders()
        val availableProviders = providers.filter { it.isAvailable }
        val availableModels = availableProviders.flatMap { it.models }

        // At least Gemini and Ollama models should be available by default
        assertTrue("Available models list should not be empty", availableModels.isNotEmpty())
        assertTrue("Available models must only come from available providers", 
            availableModels.all { model -> availableProviders.any { it.id == model.providerId } }
        )
    }

    @Test
    fun testFileExplorerTreeBuilding() {
        val files = listOf(
            com.example.data.local.entities.WorkspaceFileEntity(
                path = "app/src/MainActivity.kt",
                name = "MainActivity.kt",
                content = "class MainActivity",
                language = "kotlin"
            ),
            com.example.data.local.entities.WorkspaceFileEntity(
                path = "app/build.gradle.kts",
                name = "build.gradle.kts",
                content = "plugins {}",
                language = "gradle"
            ),
            com.example.data.local.entities.WorkspaceFileEntity(
                path = "README.md",
                name = "README.md",
                content = "# OpenCode",
                language = "markdown"
            )
        )

        val treeNodes = com.example.ui.components.buildFileTree(files)
        assertTrue("Tree nodes should not be empty", treeNodes.isNotEmpty())
        
        // Root should have "app" folder and "README.md" file
        val appFolder = treeNodes.find { it.name == "app" }
        assertNotNull("App folder should exist in root tree", appFolder)
        assertTrue("App should be a directory", appFolder!!.isDirectory)
        
        val readme = treeNodes.find { it.name == "README.md" }
        assertNotNull("README.md should exist in root tree", readme)
        assertFalse("README.md should be a file", readme!!.isDirectory)
    }
}

