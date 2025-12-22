package com.github.thisisdun998.findfriend.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import com.github.thisisdun998.findfriend.services.WebSocketService
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.BorderFactory

class NotificationDialog(
    private val senderId: String,
    private val messageContent: String
) : DialogWrapper(true) {

    private val replyArea = JBTextArea(5, 20)

    init {
        title = "Message from $senderId"
        init()
        // Double the typical size or set a reasonable large size
        setSize(600, 400)
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout(0, 10))
        panel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

        // Message Display
        val messageLabel = JBLabel("<html><body style='width: 400px'><b>$senderId says:</b><br><br>${messageContent.replace("\n", "<br>")}</body></html>")
        panel.add(JBScrollPane(messageLabel), BorderLayout.CENTER)

        // Reply Section
        val replyPanel = JPanel(BorderLayout(0, 5))
        replyPanel.add(JBLabel("Reply:"), BorderLayout.NORTH)
        
        replyArea.lineWrap = true
        replyArea.wrapStyleWord = true
        replyPanel.add(JBScrollPane(replyArea), BorderLayout.CENTER)
        
        panel.add(replyPanel, BorderLayout.SOUTH)

        return panel
    }

    override fun createActions(): Array<Action> {
        val replyAction = object : DialogWrapperAction("Reply") {
            override fun doAction(e: java.awt.event.ActionEvent?) {
                val replyText = replyArea.text.trim()
                if (replyText.isNotEmpty()) {
                    ApplicationManager.getApplication().getService(WebSocketService::class.java)
                        .sendMessage(senderId, replyText)
                    close(OK_EXIT_CODE)
                }
            }
        }
        return arrayOf(replyAction, cancelAction) // Cancel acts as "Close" or "Ignore"
    }
}
