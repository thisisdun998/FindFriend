package com.github.thisisdun998.findfriend.services

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.annotations.XCollection

@Service(Service.Level.APP)
@State(
    name = "ChatHistoryService",
    storages = [Storage("FindFriendChatHistory.xml")]
)
class ChatHistoryService : PersistentStateComponent<ChatHistoryService.State> {

    data class ChatMessage(
        var sender: String = "",
        var content: String = "",
        var timestamp: Long = 0
    )

    class State {
        @XCollection(style = XCollection.Style.v2)
        var messages: MutableList<ChatMessage> = mutableListOf()
    }

    private var myState = State()

    override fun getState(): State {
        return myState
    }

    override fun loadState(state: State) {
        myState = state
    }

    fun addMessage(sender: String, content: String) {
        synchronized(myState.messages) {
            myState.messages.add(ChatMessage(sender, content, System.currentTimeMillis()))
        }
    }

    fun getMessages(): List<ChatMessage> {
        synchronized(myState.messages) {
            return ArrayList(myState.messages)
        }
    }
}
