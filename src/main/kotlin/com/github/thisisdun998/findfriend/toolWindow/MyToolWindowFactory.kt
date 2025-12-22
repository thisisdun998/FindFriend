package com.github.thisisdun998.findfriend.toolWindow

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.table.JBTable
import com.github.thisisdun998.findfriend.services.ChatHistoryService
import com.github.thisisdun998.findfriend.services.ChatListener
import com.github.thisisdun998.findfriend.services.WebSocketService
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.text.SimpleDateFormat
import java.util.Date
import javax.swing.*
import javax.swing.event.TableModelEvent
import javax.swing.table.DefaultTableModel

class MyToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = MyToolWindow(toolWindow)
        val content = ContentFactory.getInstance().createContent(myToolWindow.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class MyToolWindow(toolWindow: ToolWindow) {
        private val cardLayout = CardLayout()
        private val mainPanel = JPanel(cardLayout)
        
        // Chat List View
        private val tableModel = object : DefaultTableModel(arrayOf("User ID", "Last Time", "Nickname"), 0) {
            override fun isCellEditable(row: Int, column: Int): Boolean {
                return column == 2
            }
        }
        private val chatTable = JBTable(tableModel)
        
        // Chat Detail View
        private val chatHistoryArea = JBTextArea()
        private val messageInput = JTextField()
        private val sendButton = JButton("Send")
        private val backButton = JButton("Back")
        private val detailTitle = JLabel("Chat")
        private var currentPartnerId: String? = null

        init {
            setupChatListPanel()
            setupChatDetailPanel()
            
            // Subscribe to Chat Events
            ApplicationManager.getApplication().messageBus.connect().subscribe(ChatListener.TOPIC, object : ChatListener {
                override fun onMessageReceived(partnerId: String) {
                    SwingUtilities.invokeLater {
                        refreshChatList()
                        if (currentPartnerId == partnerId) {
                            refreshChatDetail(partnerId)
                        }
                    }
                }
            })
            
            refreshChatList()
        }

        fun getContent(): JPanel = mainPanel

        private fun setupChatListPanel() {
            val panel = JPanel(BorderLayout())
            
            // Table
            chatTable.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
            chatTable.addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    if (e.clickCount == 2) {
                        val row = chatTable.selectedRow
                        val col = chatTable.selectedColumn
                        if (row != -1 && col != 2) { // Open chat if not clicking nickname column
                            val partnerId = chatTable.getValueAt(row, 0) as String
                            openChat(partnerId)
                        }
                    }
                }
            })

            // Listen for nickname changes
            tableModel.addTableModelListener { e ->
                if (e.type == TableModelEvent.UPDATE && e.column == 2) {
                    val row = e.firstRow
                    if (row >= 0 && row < tableModel.rowCount) {
                        val partnerId = tableModel.getValueAt(row, 0) as String
                        val newNickname = tableModel.getValueAt(row, 2) as String
                        ApplicationManager.getApplication().service<ChatHistoryService>()
                            .updateNickname(partnerId, newNickname)
                    }
                }
            }

            panel.add(JBScrollPane(chatTable), BorderLayout.CENTER)
            
            // New Chat Button
            val newChatButton = JButton("New Chat")
            newChatButton.addActionListener {
                val partnerId = Messages.showInputDialog(
                    "Enter Friend's User ID:",
                    "New Chat",
                    Messages.getQuestionIcon()
                )
                if (!partnerId.isNullOrBlank()) {
                    openChat(partnerId)
                }
            }
            panel.add(newChatButton, BorderLayout.SOUTH)
            
            mainPanel.add(panel, "LIST")
        }

        private fun setupChatDetailPanel() {
            val panel = JPanel(BorderLayout())
            
            // Header
            val header = JPanel(BorderLayout())
            header.add(backButton, BorderLayout.WEST)
            detailTitle.horizontalAlignment = SwingConstants.CENTER
            header.add(detailTitle, BorderLayout.CENTER)
            panel.add(header, BorderLayout.NORTH)
            
            // History
            chatHistoryArea.isEditable = false
            chatHistoryArea.lineWrap = true
            chatHistoryArea.wrapStyleWord = true
            panel.add(JBScrollPane(chatHistoryArea), BorderLayout.CENTER)
            
            // Input
            val inputPanel = JPanel(BorderLayout())
            inputPanel.add(messageInput, BorderLayout.CENTER)
            inputPanel.add(sendButton, BorderLayout.EAST)
            panel.add(inputPanel, BorderLayout.SOUTH)
            
            // Add keyboard listener for Enter key to send message
            messageInput.addActionListener { e ->
                val content = messageInput.text.trim()
                val partnerId = currentPartnerId
                if (content.isNotEmpty() && partnerId != null) {
                    ApplicationManager.getApplication().getService(WebSocketService::class.java)
                        .sendMessage(partnerId, content)
                    messageInput.text = ""
                }
            }
            
            // Actions
            backButton.addActionListener {
                currentPartnerId = null
                cardLayout.show(mainPanel, "LIST")
            }
            
            sendButton.addActionListener {
                val content = messageInput.text.trim()
                val partnerId = currentPartnerId
                if (content.isNotEmpty() && partnerId != null) {
                    ApplicationManager.getApplication().getService(WebSocketService::class.java)
                        .sendMessage(partnerId, content)
                    messageInput.text = ""
                }
            }
            
            mainPanel.add(panel, "DETAIL")
        }

        private fun openChat(partnerId: String) {
            currentPartnerId = partnerId
            refreshChatDetail(partnerId) // This will set the title correctly
            cardLayout.show(mainPanel, "DETAIL")
        }

        private fun refreshChatList() {
            val historyService = ApplicationManager.getApplication().service<ChatHistoryService>()
            val conversations = historyService.getConversations()
            
            // If editing, maybe don't refresh? Or try to restore selection?
            // For now, simple refresh.
            val isEditing = chatTable.isEditing
            if (isEditing) return // Don't interrupt editing

            tableModel.rowCount = 0
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm")
            
            conversations.forEach { conv ->
                // Skip "System" user in the chat list
                if (conv.partnerId != "System") {
                    val lastMsg = conv.messages.maxByOrNull { it.timestamp }
                    val lastTime = if (lastMsg != null) sdf.format(Date(lastMsg.timestamp)) else ""
                    tableModel.addRow(arrayOf(conv.partnerId, lastTime, conv.nickname))
                }
            }
        }

        private fun refreshChatDetail(partnerId: String) {
            val historyService = ApplicationManager.getApplication().service<ChatHistoryService>()
            // Find conversation to get nickname
            val conversations = historyService.getConversations()
            val conversation = conversations.find { it.partnerId == partnerId }
            val nickname = conversation?.nickname ?: ""
            
            val displayName = if (nickname.isNotEmpty()) "$nickname ($partnerId)" else partnerId
            detailTitle.text = "Chat with $displayName"

            val messages = historyService.getMessages(partnerId)
            
            val sb = StringBuilder()
            val sdf = SimpleDateFormat("HH:mm:ss")
            messages.forEach { msg ->
                val time = sdf.format(Date(msg.timestamp))
                if (msg.isSystem) {
                    sb.append("[$time] SYSTEM: ${msg.content}\n\n")
                } else {
                    val sender = if (msg.isMe) "Me" else (if (nickname.isNotEmpty()) nickname else msg.sender)
                    sb.append("[$time] $sender: ${msg.content}\n\n")
                }
            }
            chatHistoryArea.text = sb.toString()
            // Scroll to bottom
            chatHistoryArea.caretPosition = chatHistoryArea.document.length
        }
    }
}
