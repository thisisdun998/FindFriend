package com.github.thisisdun998.findfriend.toolWindow

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.FormBuilder
import com.github.thisisdun998.findfriend.services.WebSocketService
import javax.swing.JButton
import javax.swing.JPanel

class MyToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = MyToolWindow(toolWindow)
        val content = ContentFactory.getInstance().createContent(myToolWindow.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class MyToolWindow(toolWindow: ToolWindow) {

        fun getContent(): JPanel {
            val toIdField = JBTextField("1002")
            val messageField = JBTextField("Hello!")
            val statusLabel = JBLabel("Ready")

            return FormBuilder.createFormBuilder()
                .addLabeledComponent(JBLabel("To User ID:"), toIdField, 1, false)
                .addLabeledComponent(JBLabel("Message:"), messageField, 1, false)
                .addComponent(JButton("Send").apply {
                    addActionListener {
                        val toId = toIdField.text
                        val msg = messageField.text
                        if (toId.isNotBlank() && msg.isNotBlank()) {
                            ApplicationManager.getApplication().getService(WebSocketService::class.java)
                                .sendMessage(toId, msg)
                            statusLabel.text = "Sent to $toId"
                        } else {
                            statusLabel.text = "Fields cannot be empty"
                        }
                    }
                })
                .addComponent(statusLabel)
                .addComponentFillVertically(JPanel(), 0)
                .panel
        }
    }
}
