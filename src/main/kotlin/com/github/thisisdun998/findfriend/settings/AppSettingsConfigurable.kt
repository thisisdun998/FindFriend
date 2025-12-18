package com.github.thisisdun998.findfriend.settings

import com.intellij.openapi.options.Configurable
import com.github.thisisdun998.findfriend.services.WebSocketService
import com.intellij.openapi.application.ApplicationManager
import javax.swing.JComponent

class AppSettingsConfigurable : Configurable {
    private var mySettingsComponent: AppSettingsComponent? = null

    override fun getDisplayName(): String = "FindFriend Settings"

    override fun createComponent(): JComponent {
        mySettingsComponent = AppSettingsComponent()
        return mySettingsComponent!!.panel
    }

    override fun isModified(): Boolean {
        val settings = AppSettingsState.instance
        return mySettingsComponent!!.userIdText != settings.userId
    }

    override fun apply() {
        val settings = AppSettingsState.instance
        val oldUserId = settings.userId
        settings.userId = mySettingsComponent!!.userIdText
        
        if (oldUserId != settings.userId) {
            // Reconnect if userId changed
            ApplicationManager.getApplication().getService(WebSocketService::class.java).reconnect()
        }
    }

    override fun reset() {
        val settings = AppSettingsState.instance
        mySettingsComponent!!.userIdText = settings.userId
    }

    override fun disposeUIResources() {
        mySettingsComponent = null
    }
}
