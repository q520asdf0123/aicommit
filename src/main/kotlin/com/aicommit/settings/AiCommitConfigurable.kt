package com.aicommit.settings

import com.aicommit.service.CommitMessageGenerator
import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import java.awt.Color
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JScrollPane

class AiCommitConfigurable : Configurable {

    private val settings = AiCommitSettings.getInstance()

    private val providerCombo = javax.swing.JComboBox(arrayOf(
        "claude", "openai", "custom", "gemini",
        "claude-code", "codex", "gemini-cli"
    ))
    private val claudeKeyField = JBPasswordField()
    private val claudeModelField = JBTextField()
    private val openaiKeyField = JBPasswordField()
    private val openaiModelField = JBTextField()
    private val customKeyField = JBPasswordField()
    private val customModelField = JBTextField()
    private val customUrlField = JBTextField()
    private val geminiKeyField = JBPasswordField()
    private val geminiModelField = JBTextField()
    private val geminiBaseUrlField = JBTextField()
    private val claudeCodePathField = JBTextField()
    private val codexPathField = JBTextField()
    private val geminiCliPathField = JBTextField()
    private val promptField = JBTextArea(10, 60)
    private val resetPromptButton = JButton("恢复默认模板")

    override fun getDisplayName() = "AI Commit"

    override fun createComponent(): JComponent {
        setupPromptField()
        return panel {
            group("通用") {
                row("默认提供商:") { cell(providerCombo) }
                row("Prompt 模板:") {
                    cell(JScrollPane(promptField))
                        .comment("使用 {{diff}} 作为 diff 内容占位符。留空使用默认模板。")
                }
                row("") {
                    cell(resetPromptButton)
                        .comment("点击将模板恢复为内置默认值")
                }
            }
            group("Claude API") {
                row("API Key:") { cell(claudeKeyField).columns(COLUMNS_LARGE) }
                row("模型:") { cell(claudeModelField).columns(COLUMNS_MEDIUM) }
            }
            group("OpenAI API") {
                row("API Key:") { cell(openaiKeyField).columns(COLUMNS_LARGE) }
                row("模型:") { cell(openaiModelField).columns(COLUMNS_MEDIUM) }
            }
            group("自定义 API") {
                row("Base URL:") {
                    cell(customUrlField).columns(COLUMNS_LARGE)
                        .comment("如 http://localhost:11434/v1，无需包含 /chat/completions")
                }
                row("API Key:") { cell(customKeyField).columns(COLUMNS_LARGE) }
                row("模型:") { cell(customModelField).columns(COLUMNS_MEDIUM) }
            }
            group("Gemini API") {
                row("API Key:") { cell(geminiKeyField).columns(COLUMNS_LARGE) }
                row("模型:") { cell(geminiModelField).columns(COLUMNS_MEDIUM) }
                row("Base URL:") {
                    cell(geminiBaseUrlField).columns(COLUMNS_LARGE)
                        .comment("默认 https://generativelanguage.googleapis.com")
                }
            }
            group("Claude Code CLI") {
                row("CLI 路径:") {
                    cell(claudeCodePathField).columns(COLUMNS_LARGE)
                        .comment("留空则使用系统 PATH 中的 claude 命令")
                }
            }
            group("Codex CLI") {
                row("CLI 路径:") {
                    cell(codexPathField).columns(COLUMNS_LARGE)
                        .comment("留空则使用系统 PATH 中的 codex 命令")
                }
            }
            group("Gemini CLI") {
                row("CLI 路径:") {
                    cell(geminiCliPathField).columns(COLUMNS_LARGE)
                        .comment("留空则使用系统 PATH 中的 gemini 命令")
                }
            }
        }
    }

    override fun isModified(): Boolean {
        val state = settings.state
        return providerCombo.selectedItem != state.defaultProvider ||
                String(claudeKeyField.password) != settings.getApiKey("claude") ||
                claudeModelField.text != state.claudeModel ||
                String(openaiKeyField.password) != settings.getApiKey("openai") ||
                openaiModelField.text != state.openaiModel ||
                String(customKeyField.password) != settings.getApiKey("custom") ||
                customModelField.text != state.customModel ||
                customUrlField.text != state.customBaseUrl ||
                String(geminiKeyField.password) != settings.getApiKey("gemini") ||
                geminiModelField.text != state.geminiModel ||
                geminiBaseUrlField.text != state.geminiBaseUrl ||
                claudeCodePathField.text != state.claudeCodePath ||
                codexPathField.text != state.codexPath ||
                geminiCliPathField.text != state.geminiCliPath ||
                getPromptText() != state.promptTemplate
    }

    override fun apply() {
        val state = settings.state
        state.defaultProvider = providerCombo.selectedItem as String
        state.claudeModel = claudeModelField.text
        state.openaiModel = openaiModelField.text
        state.customModel = customModelField.text
        state.customBaseUrl = customUrlField.text
        state.geminiModel = geminiModelField.text
        state.geminiBaseUrl = geminiBaseUrlField.text
        state.claudeCodePath = claudeCodePathField.text
        state.codexPath = codexPathField.text
        state.geminiCliPath = geminiCliPathField.text
        state.promptTemplate = getPromptText()

        settings.setApiKey("claude", String(claudeKeyField.password))
        settings.setApiKey("openai", String(openaiKeyField.password))
        settings.setApiKey("custom", String(customKeyField.password))
        settings.setApiKey("gemini", String(geminiKeyField.password))
    }

    override fun reset() {
        val state = settings.state
        providerCombo.selectedItem = state.defaultProvider
        claudeKeyField.text = settings.getApiKey("claude")
        claudeModelField.text = state.claudeModel
        openaiKeyField.text = settings.getApiKey("openai")
        openaiModelField.text = state.openaiModel
        customKeyField.text = settings.getApiKey("custom")
        customModelField.text = state.customModel
        customUrlField.text = state.customBaseUrl
        geminiKeyField.text = settings.getApiKey("gemini")
        geminiModelField.text = state.geminiModel
        geminiBaseUrlField.text = state.geminiBaseUrl
        claudeCodePathField.text = state.claudeCodePath
        codexPathField.text = state.codexPath
        geminiCliPathField.text = state.geminiCliPath
        promptField.text = state.promptTemplate
        updatePlaceholder()
    }

    private fun getPromptText(): String {
        return if (isShowingPlaceholder) "" else promptField.text
    }

    private fun setupPromptField() {
        promptField.lineWrap = true
        promptField.wrapStyleWord = true

        resetPromptButton.addActionListener {
            promptField.text = CommitMessageGenerator.DEFAULT_TEMPLATE
            promptField.foreground = null
        }

        promptField.addFocusListener(object : FocusAdapter() {
            override fun focusGained(e: FocusEvent?) {
                if (isShowingPlaceholder) {
                    promptField.text = ""
                    promptField.foreground = null
                    isShowingPlaceholder = false
                }
            }

            override fun focusLost(e: FocusEvent?) {
                updatePlaceholder()
            }
        })
    }

    private var isShowingPlaceholder = false

    private fun updatePlaceholder() {
        if (promptField.text.isBlank()) {
            promptField.foreground = Color.GRAY
            promptField.text = CommitMessageGenerator.DEFAULT_TEMPLATE
            isShowingPlaceholder = true
        } else {
            isShowingPlaceholder = false
        }
    }
}
