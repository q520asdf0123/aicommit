package com.aicommit.service

import org.junit.Assert.*
import org.junit.Test

class CommitMessageGeneratorTest {

    @Test
    fun `buildPrompt replaces diff placeholder in template`() {
        val template = "生成 commit message:\n{{diff}}"
        val diff = "+ new code"
        val result = CommitMessageGenerator.buildPrompt(template, diff)
        assertEquals("生成 commit message:\n+ new code", result)
    }

    @Test
    fun `buildPrompt uses default template when template is empty`() {
        val result = CommitMessageGenerator.buildPrompt("", "some diff")
        assertTrue(result.contains("Conventional Commits"))
        assertTrue(result.contains("some diff"))
    }

    @Test
    fun `cleanResponse strips markdown code fences`() {
        val raw = "```\nfeat(auth): 添加登录\n```"
        assertEquals("feat(auth): 添加登录", CommitMessageGenerator.cleanResponse(raw))
    }

    @Test
    fun `cleanResponse preserves plain text`() {
        val raw = "fix(gate): 修复超时"
        assertEquals("fix(gate): 修复超时", CommitMessageGenerator.cleanResponse(raw))
    }
}
