package com.aicommit.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import javax.swing.JComponent
import javax.swing.JPanel

class AiCommitConfigurable : Configurable {

    private val settings = AiCommitSettings.getInstance()

    private val providerCombo = javax.swing.JComboBox(arrayOf("claude", "openai", "custom"))
    private val claudeKeyField = JBPasswordField()
    private val claudeModelField = JBTextField()
    private val openaiKeyField = JBPasswordField()
    private val openaiModelField = JBTextField()
    private val customKeyField = JBPasswordField()
    private val customModelField = JBTextField()
    private val customUrlField = JBTextField()
    private val promptField = JBTextArea(5, 40)

    override fun getDisplayName() = "AI Commit"

    override fun createComponent(): JComponent {
        return panel {
            group("通用") {
                row("默认提供商:") { cell(providerCombo) }
                row("Prompt 模板:") {
                    cell(promptField)
                        .comment("使用 {{diff}} 作为 diff 内容占位符。留空使用默认模板。")
                }
            }
            group("Claude") {
                row("API Key:") { cell(claudeKeyField).columns(COLUMNS_LARGE) }
                row("模型:") { cell(claudeModelField).columns(COLUMNS_MEDIUM) }
            }
            group("OpenAI") {
                row("API Key:") { cell(openaiKeyField).columns(COLUMNS_LARGE) }
                row("模型:") { cell(openaiModelField).columns(COLUMNS_MEDIUM) }
            }
            group("自定义") {
                row("Base URL:") {
                    cell(customUrlField).columns(COLUMNS_LARGE)
                        .comment("如 http://localhost:11434/v1，无需包含 /chat/completions")
                }
                row("API Key:") { cell(customKeyField).columns(COLUMNS_LARGE) }
                row("模型:") { cell(customModelField).columns(COLUMNS_MEDIUM) }
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
                promptField.text != state.promptTemplate
    }

    override fun apply() {
        val state = settings.state
        state.defaultProvider = providerCombo.selectedItem as String
        state.claudeModel = claudeModelField.text
        state.openaiModel = openaiModelField.text
        state.customModel = customModelField.text
        state.customBaseUrl = customUrlField.text
        state.promptTemplate = promptField.text

        settings.setApiKey("claude", String(claudeKeyField.password))
        settings.setApiKey("openai", String(openaiKeyField.password))
        settings.setApiKey("custom", String(customKeyField.password))
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
        promptField.text = state.promptTemplate
    }
}
