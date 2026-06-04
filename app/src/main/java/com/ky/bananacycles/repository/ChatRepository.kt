package com.ky.bananacycles.repository

import com.google.firebase.firestore.ListenerRegistration
import com.ky.bananacycles.model.ChatMessage
import com.ky.bananacycles.model.ChatRoom
import com.ky.bananacycles.model.WasteItem
import com.ky.bananacycles.service.ChatFirestoreService

class ChatRepository(
    private val service: ChatFirestoreService = ChatFirestoreService()
) {
    fun findOrCreateChatRoom(
        listing: WasteItem,
        buyerId: String,
        buyerName: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        service.findOrCreateChatRoom(
            listing = listing,
            buyerId = buyerId,
            buyerName = buyerName,
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }

    fun listenChatRooms(
        currentUserId: String,
        onDataChanged: (List<ChatRoom>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return service.listenChatRooms(
            currentUserId = currentUserId,
            onDataChanged = onDataChanged,
            onError = onError
        )
    }

    fun listenMessages(
        chatId: String,
        onDataChanged: (List<ChatMessage>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return service.listenMessages(
            chatId = chatId,
            onDataChanged = onDataChanged,
            onError = onError
        )
    }

    fun sendMessage(
        chatId: String,
        senderId: String,
        receiverId: String,
        message: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        service.sendMessage(
            chatId = chatId,
            senderId = senderId,
            receiverId = receiverId,
            message = message,
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }

    fun markChatAsRead(
        chatId: String,
        currentUserId: String
    ) {
        service.markChatAsRead(
            chatId = chatId,
            currentUserId = currentUserId
        )
    }
}
