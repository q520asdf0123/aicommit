package com.aicommit.provider

import com.google.gson.Gson
import com.google.gson.JsonParser
import java.net.HttpURLConnection
import java.net.URI

class ClaudeProvider(
    private val apiKey: String,
    private val model: String = "claude-sonnet-4-20250514"
) : AiProvider {

    override val id = "claude"
    override val displayName = "Claude"

    private val gson = Gson()

    override suspend fun generate(prompt: String): String {
        val url = URI("https://api.anthropic.com/v1/messages").toURL()
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("x-api-key", apiKey)
        conn.setRequestProperty("anthropic-version", "2023-06-01")
        conn.connectTimeout = 30_000
        conn.readTimeout = 30_000
        conn.doOutput = true

        conn.outputStream.use { it.write(buildRequestBody(prompt).toByteArray()) }

        val status = conn.responseCode
        if (status != 200) {
            val error = conn.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
            throw RuntimeException("Claude API error ($status): $error")
        }

        val response = conn.inputStream.bufferedReader().readText()
        return parseResponse(response)
    }

    fun buildRequestBody(prompt: String): String {
        val body = mapOf(
            "model" to model,
            "max_tokens" to 1024,
            "messages" to listOf(mapOf("role" to "user", "content" to prompt))
        )
        return gson.toJson(body)
    }

    fun parseResponse(json: String): String {
        val root = JsonParser.parseString(json).asJsonObject
        val content = root.getAsJsonArray("content")
        if (content == null || content.size() == 0) {
            throw RuntimeException("Claude 返回空结果")
        }
        return content[0].asJsonObject.get("text").asString.trim()
    }
}
