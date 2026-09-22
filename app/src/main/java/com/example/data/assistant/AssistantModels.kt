package com.example.data.assistant

data class AssistantConversation(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val incognito: Boolean = false
)

data class AssistantMessage(
    val id: String,
    val conversationId: String,
    val role: String,
    val content: String,
    val createdAt: Long,
    val kind: String = "TEXT",
    val payload: String = "",
    val attachmentUris: List<String> = emptyList()
)

data class AssistantPendingAction(
    val id: String,
    val type: String,
    val deckId: Long,
    val title: String,
    val description: String,
    val payload: String = ""
)

data class AssistantQuizOption(
    val text: String,
    val correct: Boolean
)

data class AssistantQuizQuestion(
    val prompt: String,
    val explanation: String,
    val options: List<AssistantQuizOption>
)
