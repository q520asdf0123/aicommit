package com.aicommit.provider

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class OpenAiProviderTest {

    @Test
    fun `buildRequestBody creates correct JSON`() {
        val provider = OpenAiProvider(
            apiKey = "test-key",
            model = "gpt-4o",
            baseUrl = "https://api.openai.com/v1"
        )
        val body = provider.buildRequestBody("test prompt")
        assertTrue(body.contains("\"model\":\"gpt-4o\""))
        assertTrue(body.contains("test prompt"))
    }

    @Test
    fun `parseResponse extracts content from valid response`() {
        val provider = OpenAiProvider(
            apiKey = "test-key",
            model = "gpt-4o",
            baseUrl = "https://api.openai.com/v1"
        )
        val json = """
            {"choices":[{"message":{"content":"feat(auth): 添加登录功能"}}]}
        """.trimIndent()
        val result = provider.parseResponse(json)
        assertEquals("feat(auth): 添加登录功能", result)
    }

    @Test(expected = RuntimeException::class)
    fun `parseResponse throws on empty choices`() {
        val provider = OpenAiProvider(
            apiKey = "test-key",
            model = "gpt-4o",
            baseUrl = "https://api.openai.com/v1"
        )
        provider.parseResponse("""{"choices":[]}""")
    }
}
