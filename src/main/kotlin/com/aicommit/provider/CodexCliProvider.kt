package com.aicommit.provider

class CodexCliProvider(
    cliPath: String = ""
) : CliBaseProvider(cliPath) {

    override val id = "codex"
    override val displayName = "Codex CLI"
    override val defaultCommand = "codex"

    override fun buildCommand(prompt: String): List<String> {
        return listOf(defaultCommand, "-q", prompt)
    }
}
