package com.aicommit.provider

interface AiProvider {
    val id: String
    val displayName: String

    suspend fun generate(prompt: String): String
}
