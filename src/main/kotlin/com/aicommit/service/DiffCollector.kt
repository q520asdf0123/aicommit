package com.aicommit.service

import com.intellij.openapi.vcs.changes.Change

data class FileDiff(
    val path: String,
    val diff: String,
    val changedLines: Int
)

object DiffCollector {

    private const val DEFAULT_MAX_LENGTH = 8000

    fun collectFromChanges(changes: Collection<Change>): String {
        val fileDiffs = changes.mapNotNull { change ->
            val path = (change.afterRevision ?: change.beforeRevision)?.file?.path ?: return@mapNotNull null
            val diff = buildDiffForChange(change)
            val lines = diff.lines().count { it.startsWith("+") || it.startsWith("-") }
            FileDiff(path, diff, lines)
        }

        if (fileDiffs.isEmpty()) return ""
        return truncateDiff(fileDiffs, DEFAULT_MAX_LENGTH)
    }

    private fun buildDiffForChange(change: Change): String {
        val before = change.beforeRevision?.content ?: ""
        val after = change.afterRevision?.content ?: ""

        return when {
            before.isEmpty() && after.isNotEmpty() -> "[新增文件]\n$after"
            before.isNotEmpty() && after.isEmpty() -> "[删除文件]"
            else -> buildUnifiedDiff(before.lines(), after.lines())
        }
    }

    /**
     * 生成简化的 unified diff，只输出变更行及周围 3 行上下文。
     * 使用 Myers diff 的简化版（逐行比对 LCS）。
     */
    fun buildUnifiedDiff(beforeLines: List<String>, afterLines: List<String>, contextLines: Int = 3): String {
        // 计算 LCS 来找到真正的差异
        val lcs = computeLcs(beforeLines, afterLines)
        val allLines = mutableListOf<Pair<String, String>>() // (prefix, content)

        var bi = 0  // before index
        var ai = 0  // after index
        var li = 0  // lcs index

        while (bi < beforeLines.size || ai < afterLines.size) {
            if (li < lcs.size && bi < beforeLines.size && ai < afterLines.size
                && beforeLines[bi] == lcs[li] && afterLines[ai] == lcs[li]) {
                allLines.add(" " to beforeLines[bi])
                bi++; ai++; li++
            } else {
                if (bi < beforeLines.size && (li >= lcs.size || beforeLines[bi] != lcs[li])) {
                    allLines.add("-" to beforeLines[bi])
                    bi++
                }
                if (ai < afterLines.size && (li >= lcs.size || afterLines[ai] != lcs[li])) {
                    allLines.add("+" to afterLines[ai])
                    ai++
                }
            }
        }

        // 只保留变更行及周围 contextLines 行上下文
        val changed = BooleanArray(allLines.size) { allLines[it].first != " " }
        val keep = BooleanArray(allLines.size)
        for (i in allLines.indices) {
            if (changed[i]) {
                for (j in maxOf(0, i - contextLines)..minOf(allLines.lastIndex, i + contextLines)) {
                    keep[j] = true
                }
            }
        }

        val result = StringBuilder()
        var inHunk = false
        for (i in allLines.indices) {
            if (keep[i]) {
                result.appendLine("${allLines[i].first}${allLines[i].second}")
                inHunk = true
            } else if (inHunk) {
                result.appendLine("...")
                inHunk = false
            }
        }

        return result.toString().trimEnd()
    }

    private fun computeLcs(a: List<String>, b: List<String>): List<String> {
        val m = a.size
        val n = b.size
        val dp = Array(m + 1) { IntArray(n + 1) }
        for (i in 1..m) {
            for (j in 1..n) {
                dp[i][j] = if (a[i - 1] == b[j - 1]) dp[i - 1][j - 1] + 1
                           else maxOf(dp[i - 1][j], dp[i][j - 1])
            }
        }
        val result = mutableListOf<String>()
        var i = m; var j = n
        while (i > 0 && j > 0) {
            when {
                a[i - 1] == b[j - 1] -> { result.add(a[i - 1]); i--; j-- }
                dp[i - 1][j] > dp[i][j - 1] -> i--
                else -> j--
            }
        }
        return result.reversed()
    }

    fun truncateDiff(files: List<FileDiff>, maxLength: Int = DEFAULT_MAX_LENGTH): String {
        val sorted = files.sortedByDescending { it.changedLines }
        val result = StringBuilder()
        var truncated = false

        for (file in sorted) {
            val entry = formatFileDiff(file)
            if (result.length + entry.length > maxLength) {
                truncated = true
                // 若结果为空，至少加入第一个文件（即使超出限制）
                if (result.isEmpty()) {
                    result.append(entry).append("\n")
                }
                break
            }
            result.append(entry).append("\n")
        }

        if (truncated) {
            result.append("\n(部分文件因长度限制已省略)")
        }

        return result.toString().trim()
    }

    fun formatFileDiff(diff: FileDiff): String {
        return "--- ${diff.path} ---\n${diff.diff}"
    }
}
