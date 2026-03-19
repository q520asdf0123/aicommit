package com.aicommit.service

import com.aicommit.provider.AiProvider

object CommitMessageGenerator {

    private val DEFAULT_TEMPLATE = """
根据以下 git diff 生成一条 Conventional Commits 格式的 commit message。

规则：
- 格式：<type>(<scope>): <description>
- type 可选：feat, fix, refactor, docs, style, test, chore, perf
- scope 从变更的模块/目录推断
- description 用简洁的中文描述变更内容
- 如有多个不相关变更，用换行分隔 body 说明
- 只输出 commit message 本身，不要其他解释

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
