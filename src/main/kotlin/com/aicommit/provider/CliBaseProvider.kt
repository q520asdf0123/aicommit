package com.aicommit.provider

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

abstract class CliBaseProvider(
    protected val cliPath: String = ""
) : AiProvider {

    protected abstract val defaultCommand: String
    protected abstract fun buildCommand(prompt: String): List<String>

    open fun parseOutput(output: String): String = output.trim()

    override suspend fun generate(prompt: String): String = withContext(Dispatchers.IO) {
        val command = buildCommand(prompt)
        val resolvedCommand = command.toMutableList().apply {
            if (this[0] == defaultCommand && cliPath.isNotBlank()) {
                this[0] = cliPath
            }
        }

        val processBuilder = ProcessBuilder(resolvedCommand)
            .redirectErrorStream(false)

        val process = processBuilder.start()

        val stdout = process.inputStream.bufferedReader().use { it.readText() }
        val stderr = process.errorStream.bufferedReader().use { it.readText() }

        val finished = process.waitFor(120, TimeUnit.SECONDS)
        if (!finished) {
            process.destroyForcibly()
            throw RuntimeException("CLI 命令执行超时（120秒）")
        }

        val exitCode = process.exitValue()
        if (exitCode != 0) {
            val errorMsg = stderr.ifBlank { stdout }
            throw RuntimeException("${displayName} CLI 执行失败 (exit code $exitCode): ${errorMsg.take(500)}")
        }

        val result = parseOutput(stdout)
        if (result.isBlank()) {
            throw RuntimeException("${displayName} CLI 返回空结果")
        }
        result
    }
}
