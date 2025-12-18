package com.github.thisisdun998.findfriend.ui

import com.intellij.openapi.ui.DialogWrapper
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import java.awt.BorderLayout
import javax.swing.BorderFactory

class NotificationDialog(private val message: String) : DialogWrapper(true) {
    init {
        title = "New Friend Message"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val dialogPanel = JPanel(BorderLayout())
        val label = JLabel("<html><body>${message.replace("\n", "<br>")}</body></html>")
        label.border = BorderFactory.createEmptyBorder(20, 20, 20, 20)
        dialogPanel.add(label, BorderLayout.CENTER)
        return dialogPanel
    }
}
