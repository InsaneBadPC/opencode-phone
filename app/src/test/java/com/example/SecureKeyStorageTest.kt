package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.security.SecureKeyStorage
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SecureKeyStorageTest {

    private lateinit var context: Context
    private lateinit var storage: SecureKeyStorage

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        storage = SecureKeyStorage(context)
    }

    @Test
    fun testSaveAndRetrieveApiKey() {
        val providerId = "groq"
        val testKey = "gsk_test1234567890abcdef"

        storage.saveApiKey(providerId, testKey)

        assertTrue(storage.hasApiKey(providerId))
        assertEquals(testKey, storage.getApiKey(providerId))
    }

    @Test
    fun testMaskedApiKey() {
        val providerId = "openrouter"
        val testKey = "sk-or-v1-abcdef1234567890"

        storage.saveApiKey(providerId, testKey)

        val masked = storage.getMaskedApiKey(providerId)
        assertTrue(masked.startsWith("sk-o"))
        assertTrue(masked.endsWith("7890"))
        assertTrue(masked.contains("•"))
    }

    @Test
    fun testRemoveApiKey() {
        val providerId = "gemini"
        val testKey = "AIzaSyTestApiKey123"

        storage.saveApiKey(providerId, testKey)
        assertTrue(storage.hasApiKey(providerId))

        storage.removeApiKey(providerId)
        assertFalse(storage.hasApiKey(providerId))
        assertEquals("", storage.getApiKey(providerId))
    }

    @Test
    fun testCustomBaseUrl() {
        val providerId = "ollama_local"
        val testUrl = "http://192.168.1.50:11434/v1"

        storage.saveCustomBaseUrl(providerId, testUrl)
        assertEquals(testUrl, storage.getCustomBaseUrl(providerId))
    }

    @Test
    fun testProviderEnabledState() {
        val providerId = "github_models"

        storage.saveProviderEnabled(providerId, false)
        assertFalse(storage.getProviderEnabled(providerId))

        storage.saveProviderEnabled(providerId, true)
        assertTrue(storage.getProviderEnabled(providerId))
    }
}
