package com.aicommit.action

import com.aicommit.provider.AiProvider
import com.aicommit.provider.ClaudeProvider
import com.aicommit.provider.CustomProvider
import com.aicommit.provider.OpenAiProvider
import com.aicommit.service.CommitMessageGenerator
import com.aicommit.service.DiffCollector
import com.aicommit.settings.AiCommitSettings
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.ui.CommitMessage
import com.intellij.vcs.commit.AbstractCommitWorkflowHandler

class GenerateCommitMessageAction : AnAction() {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL) != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val commitMessage = e.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL) as? CommitMessage ?: return

        // 尝试多种方式获取变更列表
        val changes: List<Change> = getChanges(e, project)

        if (changes.isEmpty()) {
            notify(project, "请先选中要提交的文件", NotificationType.WARNING)
            return
        }

        val settings = AiCommitSettings.getInstance()
        val providerId = settings.state.defaultProvider
        val apiKey = settings.getApiKey(providerId)

        if (apiKey.isBlank() && providerId != "custom") {
            notify(project, "请先在 Settings → Tools → AI Commit 中配置 ${providerId} 的 API Key", NotificationType.WARNING)
            return
        }

        val provider = createProvider(providerId, settings)

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "正在生成 Commit Message...", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    val diff = DiffCollector.collectFromChanges(changes)
                    if (diff.isBlank()) {
                        notify(project, "无法获取文件变更内容", NotificationType.WARNING)
                        return
                    }

                    indicator.text = "正在调用 ${provider.displayName} API..."

                    val message = kotlinx.coroutines.runBlocking {
                        CommitMessageGenerator.generate(
                            provider = provider,
                            diff = diff,
                            template = settings.state.promptTemplate
                        )
                    }

                    // 回到 EDT 设置 commit message
                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                        commitMessage.setCommitMessage(message)
                    }
                } catch (ex: Exception) {
                    if (indicator.isCanceled) return
                    val errorMsg = when {
                        ex.message?.contains("401") == true -> "API Key 无效，请检查配置"
                        ex.message?.contains("429") == true -> "请求频率过高，请稍后重试"
                        ex.message?.contains("timeout") == true -> "请求超时，请重试"
                        else -> "生成失败: ${ex.message}"
                    }
                    notify(project, errorMsg, NotificationType.ERROR)
                }
            }
        })
    }

    private fun getChanges(e: AnActionEvent, project: com.intellij.openapi.project.Project): List<Change> {
        // 方式1：从 ActionEvent DataContext 获取（某些 IDEA 版本可用）
        val eventChanges = e.getData(VcsDataKeys.CHANGES)
        if (eventChanges != null && eventChanges.isNotEmpty()) {
            return eventChanges.toList()
        }

        // 方式2：从 CommitWorkflowHandler 获取（新版 IDEA commit 面板）
        try {
            val workflowHandler = e.getData(VcsDataKeys.COMMIT_WORKFLOW_HANDLER)
            if (workflowHandler is AbstractCommitWorkflowHandler<*, *>) {
                val ui = workflowHandler.ui
                val includedChanges = ui.getIncludedChanges()
                if (includedChanges.isNotEmpty()) {
                    return includedChanges
                }
            }
        } catch (_: Exception) {
            // 忽略，尝试下一种方式
        }

        // 方式3：回退到 ChangeListManager 获取默认 changelist
        val clm = ChangeListManager.getInstance(project)
        return clm.defaultChangeList.changes.toList()
    }

    private fun createProvider(id: String, settings: AiCommitSettings): AiProvider {
        val state = settings.state
        return when (id) {
            "claude" -> ClaudeProvider(
                apiKey = settings.getApiKey("claude"),
                model = state.claudeModel
            )
            "openai" -> OpenAiProvider(
                apiKey = settings.getApiKey("openai"),
                model = state.openaiModel
            )
            "custom" -> CustomProvider(
                apiKey = settings.getApiKey("custom"),
                model = state.customModel,
                baseUrl = state.customBaseUrl
            )
            else -> throw IllegalArgumentException("未知的 AI 提供商: $id")
        }
    }

    private fun notify(project: com.intellij.openapi.project.Project, content: String, type: NotificationType) {
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("AiCommit.Notification")
                .createNotification("AI Commit", content, type)
                .notify(project)
        }
    }
}
