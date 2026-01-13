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
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.swing.SwingUtilities

@Service(Service.Level.APP)
class WebSocketService {
    private val logger = Logger.getInstance(WebSocketService::class.java)
    private var webSocket: WebSocket? = null
    private val client = HttpClient.newHttpClient()
    private val reconnectDelayMs = 5000L
    private val heartbeatExecutor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    
    // 连接状态管理
    private val isConnecting = AtomicBoolean(false)
    private val isConnected = AtomicBoolean(false)
    
    // 心跳检测
    private val lastPongTime = AtomicLong(System.currentTimeMillis())
    private val heartbeatIntervalSeconds = 30L
    private val heartbeatTimeoutMs = 90000L // 3次心跳无响应则认为断开
    
    // 消息队列：断开时缓存消息，重连后发送
    private val pendingMessages = ConcurrentLinkedQueue<Pair<String, String>>()

    init {
        connect()
        startHeartbeat()
    }

    fun reconnect() {
        logger.info("Manual reconnect triggered")
        closeConnection()
        connect()
    }
    
    private fun closeConnection() {
        try {
            webSocket?.sendClose(WebSocket.NORMAL_CLOSURE, "Reconnecting")
        } catch (e: Exception) {
            logger.warn("Error closing WebSocket: ${e.message}")
        }
        webSocket = null
        isConnected.set(false)
    }

    private fun connect() {
        // 防止重复连接
        if (!isConnecting.compareAndSet(false, true)) {
            logger.info("Connection already in progress, skipping")
            return
        }
        
        val userId = AppSettingsState.instance.userId
        if (userId.isEmpty()) {
            logger.warn("User ID not set, skipping connection")
            isConnecting.set(false)
            return
        }

        val uri = URI.create("ws://106.12.14.8:9001/chat/$userId")
        try {
            val builder = client.newWebSocketBuilder()
            builder.buildAsync(uri, WebSocketListener())
                .thenAccept { ws ->
                    webSocket = ws
                    isConnected.set(true)
                    isConnecting.set(false)
                    lastPongTime.set(System.currentTimeMillis())
                    logger.info("WebSocket connected to $uri")
                    
                    // 发送队列中的待发消息
                    flushPendingMessages()
                }
                .exceptionally { e ->
                    logger.warn("WebSocket connection failed: ${e.message}")
                    isConnecting.set(false)
                    isConnected.set(false)
                    scheduleReconnect()
                    null
                }
        } catch (e: Exception) {
            logger.error("Error creating WebSocket connection", e)
            isConnecting.set(false)
            isConnected.set(false)
            scheduleReconnect()
        }
    }
    
    private fun flushPendingMessages() {
        while (pendingMessages.isNotEmpty() && isConnected.get()) {
            val (toId, content) = pendingMessages.poll() ?: break
            doSendMessage(toId, content)
        }
    }

    private fun startHeartbeat() {
        heartbeatExecutor.scheduleAtFixedRate({
            try {
                val currentTime = System.currentTimeMillis()
                val timeSinceLastPong = currentTime - lastPongTime.get()
                
                // 检查心跳超时
                if (isConnected.get() && timeSinceLastPong > heartbeatTimeoutMs) {
                    logger.warn("Heartbeat timeout detected, reconnecting...")
                    closeConnection()
                    connect()
                    return@scheduleAtFixedRate
                }
                
                // 发送心跳
                if (isConnected.get() && webSocket != null) {
                    val heartbeatJson = """{"type":"HEARTBEAT"}"""
                    webSocket?.sendText(heartbeatJson, true)
                    logger.debug("Heartbeat sent")
                } else if (!isConnected.get() && !isConnecting.get()) {
                    // 连接断开且没有正在重连，尝试重连
                    logger.info("Connection lost, attempting to reconnect...")
                    connect()
                }
            } catch (e: Exception) {
                logger.warn("Heartbeat error: ${e.message}")
            }
        }, heartbeatIntervalSeconds, heartbeatIntervalSeconds, TimeUnit.SECONDS)
    }

    private fun scheduleReconnect() {
        val app = ApplicationManager.getApplication()
        if (app != null && !app.isDisposed) {
             app.executeOnPooledThread {
                 try {
                     Thread.sleep(reconnectDelayMs)
                     if (!isConnected.get() && !isConnecting.get()) {
                         connect()
                     }
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
            
            // 更新最后收到消息的时间（作为心跳响应）
            lastPongTime.set(System.currentTimeMillis())

            // Parse JSON manually to avoid dependencies
            val type = extractJsonField(message, "type")
            val content = extractJsonField(message, "content")
            val fromId = extractJsonField(message, "fromId")
            
            // 过滤心跳响应消息，不显示弹窗
            if (type == "HEARTBEAT" || type == "PONG" || type == "heartbeat" || type == "pong") {
                logger.debug("Heartbeat response received")
                webSocket.request(1)
                return super.onText(webSocket, data, last)
            }

            if (type == "ERROR" && content != null) {
                // Handle error message (e.g. User offline)
                // Assume fromId carries the ID of the user we tried to contact, or use "System"
                val targetId = if (!fromId.isNullOrEmpty()) fromId else "System"
                
                ApplicationManager.getApplication().service<ChatHistoryService>().addSystemMessage(targetId, content)
                ApplicationManager.getApplication().messageBus.syncPublisher(ChatListener.TOPIC).onMessageReceived(targetId)
            } else if (content != null && content.isNotEmpty() && fromId != null && fromId.isNotEmpty()) {
                // 只有当 content 和 fromId 都存在且非空时才处理为聊天消息
                val sender = fromId
                
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
            isConnected.set(false)
            this@WebSocketService.webSocket = null
            
            // 总是尝试重连，除非是正常关闭
            if (statusCode != WebSocket.NORMAL_CLOSURE) {
                 scheduleReconnect()
            }
            return super.onClose(webSocket, statusCode, reason)
        }

        override fun onError(webSocket: WebSocket, error: Throwable) {
            logger.error("WebSocket error", error)
            isConnected.set(false)
            this@WebSocketService.webSocket = null
            scheduleReconnect()
            super.onError(webSocket, error)
        }
    }

    fun sendMessage(toId: String, content: String) {
        // 先存储消息到本地历史
        ApplicationManager.getApplication().service<ChatHistoryService>().addMessage(toId, content, true)
        ApplicationManager.getApplication().messageBus.syncPublisher(ChatListener.TOPIC).onMessageReceived(toId)
        
        if (!isConnected.get() || webSocket == null) {
            // 连接断开时，将消息加入队列，重连后发送
            logger.info("Connection not available, queueing message for later delivery")
            pendingMessages.offer(Pair(toId, content))
            
            // 触发重连
            if (!isConnecting.get()) {
                connect()
            }
            return
        }
        
        doSendMessage(toId, content)
    }
    
    private fun doSendMessage(toId: String, content: String) {
        // Construct JSON manually
        val json = """
            {
                "type": "CHAT",
                "toId": "$toId",
                "content": "$content"
            }
        """.trimIndent()
        
        try {
            webSocket?.sendText(json, true)
            logger.info("Message sent to $toId")
        } catch (e: Exception) {
            logger.error("Failed to send message: ${e.message}")
            // 发送失败时加入队列
            pendingMessages.offer(Pair(toId, content))
        }
    }

    private fun extractJsonField(json: String, fieldName: String): String? {
        // Simple regex to extract string value of a field
        // Matches "fieldName": "value" or "fieldName":"value"
        val regex = "\"$fieldName\"\\s*:\\s*\"(.*?)\"".toRegex()
        val matchResult = regex.find(json)
        return matchResult?.groupValues?.get(1)
    }
}
