package com.aicommit.provider

import org.junit.Assert.*
import org.junit.Test

class ClaudeProviderTest {

    @Test
    fun `buildRequestBody creates correct Anthropic API format`() {
        val provider = ClaudeProvider(apiKey = "test-key", model = "claude-sonnet-4-20250514")
        val body = provider.buildRequestBody("test prompt")
        assertTrue(body.contains("\"model\":\"claude-sonnet-4-20250514\""))
        assertTrue(body.contains("test prompt"))
        assertTrue(body.contains("\"role\":\"user\""))
    }

    @Test
    fun `parseResponse extracts text from content blocks`() {
        val provider = ClaudeProvider(apiKey = "test-key")
        val json = """
            {"content":[{"type":"text","text":"fix(gate): 修复网关超时问题"}]}
        """.trimIndent()
        val result = provider.parseResponse(json)
        assertEquals("fix(gate): 修复网关超时问题", result)
    }

    @Test(expected = RuntimeException::class)
    fun `parseResponse throws on empty content`() {
        val provider = ClaudeProvider(apiKey = "test-key")
        provider.parseResponse("""{"content":[]}""")
    }
}
