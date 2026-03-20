package com.aicommit.provider

class ClaudeCodeCliProvider(
    cliPath: String = ""
) : CliBaseProvider(cliPath) {

    override val id = "claude-code"
    override val displayName = "Claude Code CLI"
    override val defaultCommand = "claude"

    override fun buildCommand(prompt: String): List<String> {
        return listOf(defaultCommand, "-p", prompt, "--output-format", "text")
    }
}
