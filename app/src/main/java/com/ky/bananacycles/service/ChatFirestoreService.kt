package com.ky.bananacycles.service

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.ky.bananacycles.model.ChatMessage
import com.ky.bananacycles.model.ChatRoom
import com.ky.bananacycles.model.MessageStatus
import com.ky.bananacycles.model.WasteItem

class ChatFirestoreService(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val chatsCollection = firestore.collection("Chats")

    fun findOrCreateChatRoom(
        listing: WasteItem,
        buyerId: String,
        buyerName: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        chatsCollection
            .whereArrayContains("participants", buyerId)
            .get()
            .addOnSuccessListener { snapshot ->
                val visibleChat = snapshot
                    .toChatRooms()
                    .firstOrNull { room ->
                        room.listingId == listing.id &&
                            room.sellerId == listing.sellerId &&
                            !room.isDeletedForUser(buyerId)
                    }

                if (visibleChat != null) {
                    onSuccess(visibleChat.id)
                    return@addOnSuccessListener
                }

                createChatRoom(
                    chatDocument = chatsCollection.document(),
                    listing = listing,
                    buyerId = buyerId,
                    buyerName = buyerName,
                    onSuccess = onSuccess,
                    onFailure = onFailure
                )
            }
            .addOnFailureListener { error ->
                onFailure(error)
            }
    }

    fun listenChatRooms(
        currentUserId: String,
        onDataChanged: (List<ChatRoom>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return chatsCollection
            .whereArrayContains("participants", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val rooms = snapshot
                    ?.toChatRooms()
                    ?.filter { room ->
                        !room.isDeletedForUser(currentUserId)
                    }
                    ?.sortedByDescending { room -> room.lastMessageTime.takeIf { it > 0L } ?: room.createdAt }
                    .orEmpty()

                onDataChanged(rooms)
            }
    }

    fun listenMessages(
        chatId: String,
        onDataChanged: (List<ChatMessage>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return chatsCollection
            .document(chatId)
            .collection("Messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                onDataChanged(snapshot?.toChatMessages().orEmpty())
            }
    }

    fun sendMessage(
        chatId: String,
        senderId: String,
        receiverId: String,
        message: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val chatDocument = chatsCollection.document(chatId)
        val messageDocument = chatDocument.collection("Messages").document()
        val trimmedMessage = message.trim()

        // Message creation and room preview/unread metadata must stay in sync.
        firestore
            .runBatch { batch ->
                batch.set(
                    messageDocument,
                    mapOf(
                        "messageId" to messageDocument.id,
                        "senderId" to senderId,
                        "receiverId" to receiverId,
                        "message" to trimmedMessage,
                        "isDeleted" to false,
                        "status" to MessageStatus.DELIVERED.name,
                        "readAt" to null,
                        "timestamp" to FieldValue.serverTimestamp()
                    )
                )

                batch.update(
                    chatDocument,
                    mapOf(
                        "lastMessage" to trimmedMessage,
                        "lastMessageTime" to FieldValue.serverTimestamp(),
                        "unreadCounts.$receiverId" to FieldValue.increment(1)
                    )
                )
            }
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->
                onFailure(error)
            }
    }

    fun markChatAsRead(
        chatId: String,
        currentUserId: String
    ) {
        val chatDocument = chatsCollection.document(chatId)

        chatDocument
            .collection("Messages")
            .whereEqualTo("receiverId", currentUserId)
            .get()
            .addOnSuccessListener { snapshot ->
                firestore.runBatch { batch ->
                    batch.update(chatDocument, "unreadCounts.$currentUserId", 0)

                    snapshot.documents
                        .filter { document ->
                            document.getString("status") != MessageStatus.READ.name
                        }
                        .forEach { document ->
                            batch.update(
                                document.reference,
                                mapOf(
                                    "status" to MessageStatus.READ.name,
                                    "readAt" to FieldValue.serverTimestamp()
                                )
                            )
                        }
                }
            }
    }

    fun deleteChatForUser(
        chatId: String,
        currentUserId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        chatsCollection
            .document(chatId)
            .update(
                mapOf(
                    "visibleTo" to FieldValue.arrayRemove(currentUserId),
                    "deletedForUsers" to FieldValue.arrayUnion(currentUserId),
                    "unreadCounts.$currentUserId" to 0
                )
            )
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->
                onFailure(error)
            }
    }

    fun deleteMessageForEveryone(
        chatId: String,
        messageId: String,
        currentUserId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val messageDocument = chatsCollection
            .document(chatId)
            .collection("Messages")
            .document(messageId)

        messageDocument
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.getString("senderId") != currentUserId) {
                    onFailure(IllegalStateException("You can only delete your own messages."))
                    return@addOnSuccessListener
                }

                messageDocument
                    .update(
                        mapOf(
                            "message" to DELETED_MESSAGE_TEXT,
                            "isDeleted" to true
                        )
                    )
                    .addOnSuccessListener {
                        onSuccess()
                    }
                    .addOnFailureListener { error ->
                        onFailure(error)
                    }
            }
            .addOnFailureListener { error ->
                onFailure(error)
            }
    }

    private fun createChatRoom(
        chatDocument: DocumentReference,
        listing: WasteItem,
        buyerId: String,
        buyerName: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val sellerName = listing.sellerName.ifBlank { "Seller" }
        val data = mapOf(
            "chatId" to chatDocument.id,
            "participants" to listOf(buyerId, listing.sellerId),
            "visibleTo" to listOf(buyerId, listing.sellerId),
            "deletedForUsers" to emptyList<String>(),
            "participantNames" to mapOf(
                buyerId to buyerName,
                listing.sellerId to sellerName
            ),
            "participantPhotoUrls" to mapOf(
                buyerId to "",
                listing.sellerId to listing.sellerPhotoUrl
            ),
            "buyerId" to buyerId,
            "sellerId" to listing.sellerId,
            "listingId" to listing.id,
            "listingName" to listing.wasteName,
            "lastMessage" to "",
            "lastMessageTime" to null,
            "unreadCounts" to mapOf(
                buyerId to 0,
                listing.sellerId to 0
            ),
            "createdAt" to FieldValue.serverTimestamp()
        )

        chatDocument
            .set(data)
            .addOnSuccessListener {
                onSuccess(chatDocument.id)
            }
            .addOnFailureListener { error ->
                onFailure(error)
            }
    }

    private fun QuerySnapshot.toChatRooms(): List<ChatRoom> {
        return documents.map { document ->
            val participants = document.get("participants") as? List<*>
            val visibleTo = document.get("visibleTo") as? List<*>
            val deletedForUsers = document.get("deletedForUsers") as? List<*>
            val participantNames = document.get("participantNames") as? Map<*, *>
            val unreadCounts = document.get("unreadCounts") as? Map<*, *>

            ChatRoom(
                id = document.getString("chatId") ?: document.id,
                participants = participants
                    ?.mapNotNull { value -> value as? String }
                    .orEmpty(),
                visibleTo = visibleTo
                    ?.mapNotNull { value -> value as? String }
                    .orEmpty(),
                deletedForUsers = deletedForUsers
                    ?.mapNotNull { value -> value as? String }
                    .orEmpty(),
                participantNames = participantNames
                    ?.mapNotNull { (key, value) ->
                        val userId = key as? String
                        val name = value as? String
                        if (userId != null && name != null) userId to name else null
                    }
                    ?.toMap()
                    .orEmpty(),
                buyerId = document.getString("buyerId").orEmpty(),
                sellerId = document.getString("sellerId").orEmpty(),
                listingId = document.getString("listingId").orEmpty(),
                listingName = document.getString("listingName").orEmpty(),
                lastMessage = document.getString("lastMessage").orEmpty(),
                lastMessageTime = document.getTimestamp("lastMessageTime").toMillisOrZero(),
                unreadCounts = unreadCounts
                    ?.mapNotNull { (key, value) ->
                        val userId = key as? String
                        val count = (value as? Number)?.toInt()
                        if (userId != null && count != null) userId to count else null
                    }
                    ?.toMap()
                    .orEmpty(),
                createdAt = document.getTimestamp("createdAt").toMillisOrZero()
            )
        }
    }

    private fun QuerySnapshot.toChatMessages(): List<ChatMessage> {
        return documents.map { document ->
            ChatMessage(
                id = document.getString("messageId") ?: document.id,
                senderId = document.getString("senderId").orEmpty(),
                receiverId = document.getString("receiverId").orEmpty(),
                message = document.getString("message").orEmpty(),
                timestamp = document.getTimestamp("timestamp").toMillisOrZero(),
                status = document.getString("status") ?: MessageStatus.DELIVERED.name,
                readAt = document.getTimestamp("readAt").toMillisOrZero(),
                isDeleted = document.getBoolean("isDeleted") ?: false
            )
        }
    }

    private fun Timestamp?.toMillisOrZero(): Long {
        return this?.toDate()?.time ?: 0L
    }

    private fun ChatRoom.isDeletedForUser(userId: String): Boolean {
        return deletedForUsers.contains(userId) ||
            (visibleTo.isNotEmpty() && !visibleTo.contains(userId))
    }

    private companion object {
        const val DELETED_MESSAGE_TEXT = "This message was deleted"
    }
}
