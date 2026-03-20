package com.aicommit.service

import com.aicommit.provider.AiProvider

object CommitMessageGenerator {

    val DEFAULT_TEMPLATE = """
根据以下 git diff 生成一条 Conventional Commits 格式的 commit message。

规则：
- 格式：<type>(<scope>): <description>
- type 可选：feat, fix, refactor, docs, style, test, chore, perf
- scope 从变更的模块/目录推断
- description 用简洁的中文描述变更内容
- 如果涉及多个文件或模块的变更，必须在 body 中逐条列出每个文件/模块的主要变更内容
- body 使用 "- " 开头的列表格式，每条说明一个具体变更
- 变更文件越多，body 描述应越详细，确保每个重要变更都被提及
- 只输出 commit message 本身，不要其他解释

示例（多文件变更时）：
feat(provider): 新增多个 AI 提供商支持

- 新增 CliBaseProvider 抽象基类，封装 CLI 进程执行逻辑
- 新增 ClaudeCodeCliProvider，支持 claude 命令行调用
- 修改 AiCommitSettings，新增 CLI 路径配置字段
- 修改 AiCommitConfigurable，新增设置 UI 分组

Diff:
{{diff}}
    """.trimIndent()

    suspend fun generate(provider: AiProvider, diff: String, template: String = ""): String {
        if (diff.isBlank()) {
            throw IllegalArgumentException("没有选中要提交的文件")
        }
        val prompt = buildPrompt(template, diff)
        val raw = provider.generate(prompt)
        return cleanResponse(raw)
    }

    fun buildPrompt(template: String, diff: String): String {
        val t = template.ifBlank { DEFAULT_TEMPLATE }
        return t.replace("{{diff}}", diff)
    }

    fun cleanResponse(raw: String): String {
        var result = raw.trim()
        // 去除 markdown code fence
        if (result.startsWith("```")) {
            result = result.removePrefix("```").trimStart()
            // 去除可能的语言标记（如 ```text）
            val firstNewline = result.indexOf('\n')
            if (firstNewline >= 0 && !result.substring(0, firstNewline).contains(' ')) {
                result = result.substring(firstNewline + 1)
            }
        }
        if (result.endsWith("```")) {
            result = result.removeSuffix("```").trimEnd()
        }
        return result
    }
}
