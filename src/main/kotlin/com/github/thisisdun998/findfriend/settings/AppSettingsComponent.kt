package com.github.thisisdun998.findfriend.settings

import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JPanel

class AppSettingsComponent {
    val panel: JPanel
    private val userIdField = JBTextField()

    init {
        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("User ID:"), userIdField, 1, false)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    fun getPreferredFocusedComponent() = userIdField

    var userIdText: String
        get() = userIdField.text
        set(newText) {
            userIdField.text = newText
        }
}
