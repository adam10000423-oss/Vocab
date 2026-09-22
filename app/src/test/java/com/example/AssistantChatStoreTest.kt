package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.assistant.AssistantChatStore
import com.example.data.assistant.AssistantConversation
import com.example.data.assistant.AssistantMessage
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AssistantChatStoreTest {
    private lateinit var store: AssistantChatStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        store = AssistantChatStore(context)
        store.clear()
    }

    @Test
    fun imageAttachmentsSurviveChatReload() {
        val conversation = AssistantConversation("conversation", "圖片", 1L, 1L)
        val message = AssistantMessage(
            id = "message",
            conversationId = conversation.id,
            role = "USER",
            content = "請分析圖片",
            createdAt = 2L,
            attachmentUris = listOf("file:///data/user/0/example/files/assistant_images/image.jpg")
        )

        store.save(listOf(conversation), listOf(message))

        assertEquals(message.attachmentUris, store.loadMessages().single().attachmentUris)
    }
}
