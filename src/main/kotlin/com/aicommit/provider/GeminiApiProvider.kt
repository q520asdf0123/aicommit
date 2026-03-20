package com.aicommit.provider

import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URI

class GeminiApiProvider(
    private val apiKey: String,
    private val model: String = "gemini-2.0-flash",
    private val baseUrl: String = "https://generativelanguage.googleapis.com"
) : AiProvider {

    override val id = "gemini"
    override val displayName = "Gemini API"

    private val gson = Gson()

    override suspend fun generate(prompt: String): String = withContext(Dispatchers.IO) {
        val normalizedBase = baseUrl.trimEnd('/')
        val url = URI("$normalizedBase/v1beta/models/$model:generateContent?key=$apiKey").toURL()
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 30_000
            conn.readTimeout = 30_000
            conn.doOutput = true

            conn.outputStream.use { it.write(buildRequestBody(prompt).toByteArray()) }

            val status = conn.responseCode
            if (status != 200) {
                val error = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                throw RuntimeException("Gemini API error ($status): $error")
            }

            val response = conn.inputStream.bufferedReader().use { it.readText() }
            parseResponse(response)
        } finally {
            conn.disconnect()
        }
    }

    fun buildRequestBody(prompt: String): String {
        val body = mapOf(
            "contents" to listOf(
                mapOf(
                    "parts" to listOf(mapOf("text" to prompt))
                )
            )
        )
        return gson.toJson(body)
    }

    fun parseResponse(json: String): String {
        val root = JsonParser.parseString(json).asJsonObject
        val candidates = root.getAsJsonArray("candidates")
        if (candidates == null || candidates.size() == 0) {
            throw RuntimeException("Gemini 返回空结果")
        }
        return candidates[0].asJsonObject
            .getAsJsonObject("content")
            .getAsJsonArray("parts")[0].asJsonObject
            .get("text").asString.trim()
    }
}
