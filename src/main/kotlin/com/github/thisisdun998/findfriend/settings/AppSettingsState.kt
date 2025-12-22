package com.github.thisisdun998.findfriend.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "com.github.thisisdun998.findfriend.settings.AppSettingsState",
    storages = [Storage("FindFriendSettings.xml")]
)
class AppSettingsState : PersistentStateComponent<AppSettingsState> {
    var userId: String = ""

    init {
        if (userId.isEmpty()) {
            userId = java.util.UUID.randomUUID().toString()
        }
    }

    companion object {
        val instance: AppSettingsState
            get() = ApplicationManager.getApplication().getService(AppSettingsState::class.java)
    }

    override fun getState(): AppSettingsState {
        return this
    }

    override fun loadState(state: AppSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
        // Ensure ID exists even after load if somehow missing
        if (userId.isEmpty()) {
             userId = java.util.UUID.randomUUID().toString()
        }
    }
}
