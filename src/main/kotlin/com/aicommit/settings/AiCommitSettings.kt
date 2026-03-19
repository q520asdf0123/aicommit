package com.aicommit.settings

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.APP)
@State(name = "AiCommitSettings", storages = [Storage("ai-commit.xml")])
class AiCommitSettings : PersistentStateComponent<AiCommitSettings.State> {

    data class State(
        var defaultProvider: String = "claude",
        var claudeModel: String = "claude-sonnet-4-20250514",
        var openaiModel: String = "gpt-4o",
        var customModel: String = "",
        var customBaseUrl: String = "",
        var promptTemplate: String = ""
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    fun getApiKey(providerId: String): String {
        val attr = credentialAttributes(providerId)
        return PasswordSafe.instance.getPassword(attr) ?: ""
    }

    fun setApiKey(providerId: String, key: String) {
        val attr = credentialAttributes(providerId)
        PasswordSafe.instance.setPassword(attr, key)
    }

    private fun credentialAttributes(providerId: String): CredentialAttributes {
        return CredentialAttributes(generateServiceName("AiCommit", providerId))
    }

    companion object {
        fun getInstance(): AiCommitSettings {
            return ApplicationManager.getApplication().getService(AiCommitSettings::class.java)
        }
    }
}
