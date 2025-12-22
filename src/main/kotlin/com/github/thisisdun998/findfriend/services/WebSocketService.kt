package com.github.thisisdun998.findfriend.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.github.thisisdun998.findfriend.settings.AppSettingsState
import com.github.thisisdun998.findfriend.ui.NotificationDialog
import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.util.concurrent.CompletionStage
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import javax.swing.SwingUtilities

@Service(Service.Level.APP)
class WebSocketService {
    private val logger = Logger.getInstance(WebSocketService::class.java)
    private var webSocket: WebSocket? = null
    private val client = HttpClient.newHttpClient()
    private val reconnectDelayMs = 5000L
    private val heartbeatExecutor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    init {
        connect()
        startHeartbeat()
    }

    fun reconnect() {
        webSocket?.sendClose(WebSocket.NORMAL_CLOSURE, "Reconnecting")
        webSocket = null
        connect()
    }

    private fun connect() {
        val userId = AppSettingsState.instance.userId
        if (userId.isEmpty()) {
            logger.warn("User ID not set, skipping connection")
            return
        }

        val uri = URI.create("ws://106.12.14.8:9001/chat/$userId")
        try {
            val builder = client.newWebSocketBuilder()
            builder.buildAsync(uri, WebSocketListener())
                .thenAccept { ws ->
                    webSocket = ws
                    logger.info("WebSocket connected to $uri")
                }
                .exceptionally { e ->
                    logger.warn("WebSocket connection failed: ${e.message}")
                    scheduleReconnect()
                    null
                }
        } catch (e: Exception) {
            logger.error("Error creating WebSocket connection", e)
            scheduleReconnect()
        }
    }

    private fun startHeartbeat() {
        heartbeatExecutor.scheduleAtFixedRate({
            if (webSocket != null) {
                // Send heartbeat "empty" message as requested
                val heartbeatJson = """{"type":"HEARTBEAT"}"""
                webSocket?.sendText(heartbeatJson, true)
            }
        }, 30, 30, TimeUnit.SECONDS)
    }

    private fun scheduleReconnect() {
        val app = ApplicationManager.getApplication()
        if (app != null && !app.isDisposed) {
             app.executeOnPooledThread {
                 try {
                     Thread.sleep(reconnectDelayMs)
                     connect()
                 } catch (e: InterruptedException) {
                     Thread.currentThread().interrupt()
                 }
             }
        }
    }

    private inner class WebSocketListener : WebSocket.Listener {
        override fun onOpen(webSocket: WebSocket) {
            logger.info("WebSocket opened")
            webSocket.request(1)
            super.onOpen(webSocket)
        }

        override fun onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*>? {
            val message = data.toString()
            logger.info("Received message: $message")

            // Parse JSON manually to avoid dependencies
            val type = extractJsonField(message, "type")
            val content = extractJsonField(message, "content")
            val fromId = extractJsonField(message, "fromId")

            if (type == "ERROR" && content != null) {
                // Handle error message (e.g. User offline)
                // Assume fromId carries the ID of the user we tried to contact, or use "System"
                val targetId = if (!fromId.isNullOrEmpty()) fromId else "System"
                
                ApplicationManager.getApplication().service<ChatHistoryService>().addSystemMessage(targetId, content)
                ApplicationManager.getApplication().messageBus.syncPublisher(ChatListener.TOPIC).onMessageReceived(targetId)
            } else if (content != null && content.isNotEmpty()) {
                val sender = fromId ?: "Unknown"
                
                // Store message
                ApplicationManager.getApplication().service<ChatHistoryService>().addMessage(sender, content, false)
                
                // Notify UI update via MessageBus
                ApplicationManager.getApplication().messageBus.syncPublisher(ChatListener.TOPIC).onMessageReceived(sender)

                // Show UI notification on EDT
                SwingUtilities.invokeLater {
                    NotificationDialog(sender, content).show()
                }
            }
            
            webSocket.request(1)
            return super.onText(webSocket, data, last)
        }

        override fun onClose(webSocket: WebSocket, statusCode: Int, reason: String): CompletionStage<*>? {
            logger.info("WebSocket closed: $statusCode $reason")
            // Only reconnect if not intentionally closed for reconfiguration
            // But here we can just schedule reconnect, if it was intentional connect() will be called immediately after
            if (statusCode != WebSocket.NORMAL_CLOSURE) {
                 scheduleReconnect()
            }
            return super.onClose(webSocket, statusCode, reason)
        }

        override fun onError(webSocket: WebSocket, error: Throwable) {
            logger.error("WebSocket error", error)
            scheduleReconnect()
            super.onError(webSocket, error)
        }
    }

    fun sendMessage(toId: String, content: String) {
        if (webSocket == null) {
            // NotificationDialog needs senderId and content, here we just show error
            // Using a simple message dialog would be better but reusing NotificationDialog for consistency if needed
            // Or just logging/ignoring for now as this is edge case
            return
        }
        
        // Store sent message
        ApplicationManager.getApplication().service<ChatHistoryService>().addMessage(toId, content, true)
        ApplicationManager.getApplication().messageBus.syncPublisher(ChatListener.TOPIC).onMessageReceived(toId)
        
        // Construct JSON manually
        val json = """
            {
                "type": "CHAT",
                "toId": "$toId",
                "content": "$content"
            }
        """.trimIndent()
        
        webSocket?.sendText(json, true)
    }

    private fun extractJsonField(json: String, fieldName: String): String? {
        // Simple regex to extract string value of a field
        // Matches "fieldName": "value" or "fieldName":"value"
        val regex = "\"$fieldName\"\\s*:\\s*\"(.*?)\"".toRegex()
        val matchResult = regex.find(json)
        return matchResult?.groupValues?.get(1)
    }
}
