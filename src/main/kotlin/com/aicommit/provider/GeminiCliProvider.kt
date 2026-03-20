package com.aicommit.provider

class GeminiCliProvider(
    cliPath: String = ""
) : CliBaseProvider(cliPath) {

    override val id = "gemini-cli"
    override val displayName = "Gemini CLI"
    override val defaultCommand = "gemini"

    override fun buildCommand(prompt: String): List<String> {
        return listOf(defaultCommand, "-p", prompt)
    }
}
