package com.aicommit.provider

import org.junit.Assert.*
import org.junit.Test

class CustomProviderTest {

    @Test
    fun `uses OpenAI format with custom baseUrl`() {
        val provider = CustomProvider(
            apiKey = "",
            model = "llama3",
            baseUrl = "http://localhost:11434/v1"
        )
        val body = provider.buildRequestBody("test")
        assertTrue(body.contains("\"model\":\"llama3\""))
    }

    @Test
    fun `parseResponse works with OpenAI format`() {
        val provider = CustomProvider(
            apiKey = "",
            model = "llama3",
            baseUrl = "http://localhost:11434/v1"
        )
        val json = """{"choices":[{"message":{"content":"chore: 更新配置"}}]}"""
        assertEquals("chore: 更新配置", provider.parseResponse(json))
    }
}
