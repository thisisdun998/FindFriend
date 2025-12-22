package com.github.thisisdun998.findfriend.services

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.annotations.XCollection
import java.util.Collections

@Service(Service.Level.APP)
@State(
    name = "ChatHistoryService",
    storages = [Storage("FindFriendChatHistory.xml")]
)
class ChatHistoryService : PersistentStateComponent<ChatHistoryService.State> {

    data class ChatMessage(
        var sender: String = "",
        var content: String = "",
        var timestamp: Long = 0,
        var isMe: Boolean = false,
        var isSystem: Boolean = false
    )

    data class Conversation(
        var partnerId: String = "",
        var nickname: String = "",
        @XCollection(style = XCollection.Style.v2)
        var messages: MutableList<ChatMessage> = mutableListOf()
    )

    class State {
        @XCollection(style = XCollection.Style.v2)
        var conversations: MutableList<Conversation> = mutableListOf()
    }

    private var myState = State()

    override fun getState(): State {
        return myState
    }

    override fun loadState(state: State) {
        myState = state
    }

    fun addMessage(partnerId: String, content: String, isMe: Boolean) {
        synchronized(myState.conversations) {
            val conversation = myState.conversations.find { it.partnerId == partnerId }
                ?: Conversation(partnerId).also { myState.conversations.add(it) }
            
            conversation.messages.add(
                ChatMessage(
                    sender = if (isMe) "Me" else partnerId,
                    content = content,
                    timestamp = System.currentTimeMillis(),
                    isMe = isMe
                )
            )
        }
    }

    fun addSystemMessage(partnerId: String, content: String) {
        synchronized(myState.conversations) {
            val conversation = myState.conversations.find { it.partnerId == partnerId }
                ?: Conversation(partnerId).also { myState.conversations.add(it) }
            
            conversation.messages.add(
                ChatMessage(
                    sender = "System",
                    content = content,
                    timestamp = System.currentTimeMillis(),
                    isMe = false,
                    isSystem = true
                )
            )
        }
    }

    fun updateNickname(partnerId: String, nickname: String) {
        synchronized(myState.conversations) {
            val conversation = myState.conversations.find { it.partnerId == partnerId }
            if (conversation != null) {
                conversation.nickname = nickname
            }
        }
    }

    fun getConversations(): List<Conversation> {
        synchronized(myState.conversations) {
            // Return a copy to avoid concurrent modification issues if iterated
            return ArrayList(myState.conversations)
        }
    }

    fun getMessages(partnerId: String): List<ChatMessage> {
        synchronized(myState.conversations) {
            return myState.conversations.find { it.partnerId == partnerId }?.messages?.toList() ?: emptyList()
        }
    }
}
