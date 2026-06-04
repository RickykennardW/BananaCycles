package com.ky.bananacycles.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.ky.bananacycles.model.ChatMessage
import com.ky.bananacycles.model.ChatRoom
import com.ky.bananacycles.model.WasteItem
import com.ky.bananacycles.repository.ChatRepository

data class ChatUiState(
    val chatRooms: List<ChatRoom> = emptyList(),
    val selectedMessages: List<ChatMessage> = emptyList(),
    val isInboxLoading: Boolean = false,
    val isMessagesLoading: Boolean = false,
    val isCreatingChat: Boolean = false,
    val isSending: Boolean = false,
    val errorMessage: String? = null
) {
    fun totalUnreadFor(userId: String): Int {
        return chatRooms.sumOf { room -> room.unreadCounts[userId] ?: 0 }
    }
}

class ChatViewModel(
    private val repository: ChatRepository = ChatRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    var uiState by mutableStateOf(ChatUiState())
        private set

    private var inboxListener: ListenerRegistration? = null
    private var messagesListener: ListenerRegistration? = null

    val currentUserId: String
        get() = auth.currentUser?.uid.orEmpty()

    fun listenInbox() {
        val userId = currentUserId
        if (userId.isBlank()) {
            uiState = uiState.copy(
                chatRooms = emptyList(),
                isInboxLoading = false,
                errorMessage = "User is not signed in."
            )
            return
        }

        inboxListener?.remove()
        uiState = uiState.copy(
            isInboxLoading = true,
            errorMessage = null
        )

        inboxListener = repository.listenChatRooms(
            currentUserId = userId,
            onDataChanged = { rooms ->
                uiState = uiState.copy(
                    chatRooms = rooms,
                    isInboxLoading = false,
                    errorMessage = null
                )
            },
            onError = { error ->
                uiState = uiState.copy(
                    isInboxLoading = false,
                    errorMessage = error.localizedMessage ?: "Failed to load chats."
                )
            }
        )
    }

    fun startChatWithSeller(
        listing: WasteItem,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val buyerId = currentUserId
        val currentUser = auth.currentUser

        if (buyerId.isBlank()) {
            onFailure("User is not signed in.")
            return
        }

        if (listing.sellerId == buyerId) {
            onFailure("You cannot chat with yourself for your own listing.")
            return
        }

        uiState = uiState.copy(
            isCreatingChat = true,
            errorMessage = null
        )

        repository.findOrCreateChatRoom(
            listing = listing,
            buyerId = buyerId,
            buyerName = currentUser?.displayName
                ?: currentUser?.email
                ?: "Buyer",
            onSuccess = { chatId ->
                uiState = uiState.copy(isCreatingChat = false)
                onSuccess(chatId)
            },
            onFailure = { error ->
                val message = error.localizedMessage ?: "Failed to open chat."
                uiState = uiState.copy(
                    isCreatingChat = false,
                    errorMessage = message
                )
                onFailure(message)
            }
        )
    }

    fun listenMessages(chatId: String) {
        if (chatId.isBlank()) {
            return
        }

        messagesListener?.remove()
        uiState = uiState.copy(
            selectedMessages = emptyList(),
            isMessagesLoading = true,
            errorMessage = null
        )

        messagesListener = repository.listenMessages(
            chatId = chatId,
            onDataChanged = { messages ->
                uiState = uiState.copy(
                    selectedMessages = messages,
                    isMessagesLoading = false,
                    errorMessage = null
                )
                markChatAsRead(chatId)
            },
            onError = { error ->
                uiState = uiState.copy(
                    isMessagesLoading = false,
                    errorMessage = error.localizedMessage ?: "Failed to load messages."
                )
            }
        )
    }

    fun sendMessage(
        chatRoom: ChatRoom,
        message: String,
        onFailure: (String) -> Unit
    ) {
        val senderId = currentUserId
        val receiverId = chatRoom.participants.firstOrNull { participantId ->
            participantId != senderId
        }.orEmpty()

        if (senderId.isBlank() || receiverId.isBlank() || message.isBlank()) {
            return
        }

        uiState = uiState.copy(isSending = true)

        repository.sendMessage(
            chatId = chatRoom.id,
            senderId = senderId,
            receiverId = receiverId,
            message = message,
            onSuccess = {
                uiState = uiState.copy(isSending = false)
            },
            onFailure = { error ->
                val errorMessage = error.localizedMessage ?: "Failed to send message."
                uiState = uiState.copy(
                    isSending = false,
                    errorMessage = errorMessage
                )
                onFailure(errorMessage)
            }
        )
    }

    fun markChatAsRead(chatId: String) {
        val userId = currentUserId
        if (chatId.isNotBlank() && userId.isNotBlank()) {
            repository.markChatAsRead(
                chatId = chatId,
                currentUserId = userId
            )
        }
    }

    fun deleteChatForCurrentUser(
        chatId: String,
        onFailure: (String) -> Unit
    ) {
        val userId = currentUserId
        if (chatId.isBlank() || userId.isBlank()) {
            return
        }

        val previousRooms = uiState.chatRooms
        uiState = uiState.copy(
            chatRooms = uiState.chatRooms.filterNot { room -> room.id == chatId }
        )

        repository.deleteChatForUser(
            chatId = chatId,
            currentUserId = userId,
            onSuccess = {},
            onFailure = { error ->
                val message = error.localizedMessage ?: "Failed to delete chat."
                uiState = uiState.copy(
                    chatRooms = previousRooms,
                    errorMessage = message
                )
                onFailure(message)
            }
        )
    }

    fun deleteOwnMessage(
        chatId: String,
        message: ChatMessage,
        onFailure: (String) -> Unit
    ) {
        val userId = currentUserId
        if (
            chatId.isBlank() ||
            userId.isBlank() ||
            message.senderId != userId ||
            message.isDeleted
        ) {
            return
        }

        repository.deleteMessageForEveryone(
            chatId = chatId,
            messageId = message.id,
            currentUserId = userId,
            onSuccess = {},
            onFailure = { error ->
                val errorMessage = error.localizedMessage ?: "Failed to delete message."
                uiState = uiState.copy(errorMessage = errorMessage)
                onFailure(errorMessage)
            }
        )
    }

    fun clearMessages() {
        messagesListener?.remove()
        messagesListener = null
        uiState = uiState.copy(selectedMessages = emptyList())
    }

    override fun onCleared() {
        inboxListener?.remove()
        messagesListener?.remove()
        super.onCleared()
    }
}
