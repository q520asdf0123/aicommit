package com.aicommit.service

import org.junit.Assert.*
import org.junit.Test

class DiffCollectorTest {

    @Test
    fun `truncateDiff respects maxLength and truncates by file`() {
        val files = listOf(
            FileDiff("a.kt", "+ line1\n+ line2\n+ line3\n+ line4\n+ line5", 5),
            FileDiff("b.kt", "+ short", 1)
        )
        // 设一个小限制来触发截断
        val result = DiffCollector.truncateDiff(files, maxLength = 30)
        // 应该优先保留变更行数多的文件（a.kt）
        assertTrue(result.contains("a.kt"))
        assertTrue(result.contains("部分文件因长度限制已省略"))
    }

    @Test
    fun `truncateDiff returns all files when under limit`() {
        val files = listOf(
            FileDiff("a.kt", "+ line1", 1),
            FileDiff("b.kt", "+ line2", 1)
        )
        val result = DiffCollector.truncateDiff(files, maxLength = 8000)
        assertTrue(result.contains("a.kt"))
        assertTrue(result.contains("b.kt"))
        assertFalse(result.contains("已省略"))
    }

    @Test
    fun `formatFileDiff formats correctly`() {
        val diff = FileDiff("src/Main.kt", "+ new line\n- old line", 2)
        val result = DiffCollector.formatFileDiff(diff)
        assertTrue(result.startsWith("--- src/Main.kt ---"))
        assertTrue(result.contains("+ new line"))
    }

    @Test
    fun `buildUnifiedDiff only shows changed lines with context`() {
        val before = listOf("line1", "line2", "line3", "line4", "line5", "line6", "line7", "line8", "line9", "line10")
        val after = listOf("line1", "line2", "line3", "line4", "CHANGED", "line6", "line7", "line8", "line9", "line10")
        val result = DiffCollector.buildUnifiedDiff(before, after, contextLines = 2)
        // 应包含变更行
        assertTrue(result.contains("-line5"))
        assertTrue(result.contains("+CHANGED"))
        // 应包含上下文
        assertTrue(result.contains(" line3"))
        assertTrue(result.contains(" line7"))
        // 不应包含远离变更的行（line1 距离变更 >2 行）
        assertFalse(result.contains(" line1"))
    }

    @Test
    fun `buildUnifiedDiff handles new file content`() {
        val before = emptyList<String>()
        val after = listOf("new line 1", "new line 2")
        val result = DiffCollector.buildUnifiedDiff(before, after)
        assertTrue(result.contains("+new line 1"))
        assertTrue(result.contains("+new line 2"))
    }
}
