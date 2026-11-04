package com.photoedit.app.ai

import com.photoedit.app.domain.ChatMessage
import com.photoedit.app.domain.ChatProvider

/** Groq remains available; shared HTTP transport avoids duplicating network code. */
class GroqChatClient {
    private val client = ChatClient()
    suspend fun reply(token: String, messages: List<ChatMessage>,
        model: String = ChatProvider.GROQ.defaultModel): String =
        client.reply(ChatProvider.GROQ, token, model, messages)
}
