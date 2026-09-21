package com.example.data.assistant

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class AssistantChatStore(context: Context) {
    private val preferences = context.getSharedPreferences("vocab_ai_chat", Context.MODE_PRIVATE)

    fun loadConversations(): List<AssistantConversation> = runCatching {
        val array = JSONArray(preferences.getString(KEY_CONVERSATIONS, "[]"))
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    AssistantConversation(
                        id = item.optString("id"),
                        title = item.optString("title", "新對話"),
                        createdAt = item.optLong("createdAt"),
                        updatedAt = item.optLong("updatedAt"),
                        incognito = false
                    )
                )
            }
        }.filter { it.id.isNotBlank() }
            .distinctBy { it.id }
            .sortedByDescending { it.updatedAt }
            .take(MAX_CONVERSATIONS)
    }.getOrDefault(emptyList())

    fun loadMessages(): List<AssistantMessage> = runCatching {
        val array = JSONArray(preferences.getString(KEY_MESSAGES, "[]"))
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    AssistantMessage(
                        id = item.optString("id"),
                        conversationId = item.optString("conversationId"),
                        role = item.optString("role"),
                        content = item.optString("content").take(MAX_MESSAGE_CONTENT),
                        createdAt = item.optLong("createdAt"),
                        kind = item.optString("kind", "TEXT"),
                        payload = item.optString("payload").take(MAX_MESSAGE_PAYLOAD)
                    )
                )
            }
        }.filter { it.id.isNotBlank() && it.conversationId.isNotBlank() }
            .distinctBy { it.id }
            .sortedByDescending { it.createdAt }
            .take(MAX_MESSAGES)
            .sortedBy { it.createdAt }
    }.getOrDefault(emptyList())

    @Synchronized
    fun save(conversations: List<AssistantConversation>, messages: List<AssistantMessage>) {
        val persistedConversations = conversations.filterNot { it.incognito }.take(MAX_CONVERSATIONS)
        val ids = persistedConversations.mapTo(hashSetOf()) { it.id }
        val persistedMessages = messages
            .filter { it.conversationId in ids }
            .sortedByDescending { it.createdAt }
            .take(MAX_MESSAGES)
            .sortedBy { it.createdAt }
        val conversationJson = JSONArray().apply {
            persistedConversations.forEach { conversation ->
                put(
                    JSONObject()
                        .put("id", conversation.id)
                        .put("title", conversation.title)
                        .put("createdAt", conversation.createdAt)
                        .put("updatedAt", conversation.updatedAt)
                )
            }
        }
        val messageJson = JSONArray().apply {
            persistedMessages.forEach { message ->
                put(
                    JSONObject()
                        .put("id", message.id)
                        .put("conversationId", message.conversationId)
                        .put("role", message.role)
                        .put("content", message.content)
                        .put("createdAt", message.createdAt)
                        .put("kind", message.kind)
                        .put("payload", message.payload)
                )
            }
        }
        preferences.edit()
            .putString(KEY_CONVERSATIONS, conversationJson.toString())
            .putString(KEY_MESSAGES, messageJson.toString())
            .apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    companion object {
        private const val KEY_CONVERSATIONS = "conversations"
        private const val KEY_MESSAGES = "messages"
        private const val MAX_CONVERSATIONS = 50
        private const val MAX_MESSAGES = 2_000
        private const val MAX_MESSAGE_CONTENT = 40_000
        private const val MAX_MESSAGE_PAYLOAD = 500_000
    }
}
