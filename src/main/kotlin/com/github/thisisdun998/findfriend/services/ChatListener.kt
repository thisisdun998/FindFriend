package com.github.thisisdun998.findfriend.services

import com.intellij.util.messages.Topic

interface ChatListener {
    companion object {
        val TOPIC = Topic.create("FindFriend Chat", ChatListener::class.java)
    }

    fun onMessageReceived(partnerId: String)
}
