package com.aicommit.provider

import com.google.gson.Gson
import com.google.gson.JsonParser
import java.net.HttpURLConnection
import java.net.URI

open class OpenAiProvider(
    private val apiKey: String,
    private val model: String = "gpt-4o",
    private val baseUrl: String = "https://api.openai.com/v1"
) : AiProvider {

    override val id = "openai"
    override val displayName = "OpenAI"

    private val gson = Gson()

    override suspend fun generate(prompt: String): String {
        val url = URI("$baseUrl/chat/completions").toURL()
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Authorization", "Bearer $apiKey")
        conn.connectTimeout = 30_000
        conn.readTimeout = 30_000
        conn.doOutput = true

        conn.outputStream.use { it.write(buildRequestBody(prompt).toByteArray()) }

        val status = conn.responseCode
        if (status != 200) {
            val error = conn.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
            throw RuntimeException("OpenAI API error ($status): $error")
        }

        val response = conn.inputStream.bufferedReader().readText()
        return parseResponse(response)
    }

    fun buildRequestBody(prompt: String): String {
        val body = mapOf(
            "model" to model,
            "messages" to listOf(mapOf("role" to "user", "content" to prompt)),
            "temperature" to 0.3
        )
        return gson.toJson(body)
    }

    fun parseResponse(json: String): String {
        val root = JsonParser.parseString(json).asJsonObject
        val choices = root.getAsJsonArray("choices")
        if (choices == null || choices.size() == 0) {
            throw RuntimeException("OpenAI 返回空结果")
        }
        return choices[0].asJsonObject
            .getAsJsonObject("message")
            .get("content").asString.trim()
    }
}
