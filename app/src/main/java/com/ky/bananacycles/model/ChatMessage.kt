package com.ky.bananacycles.model

data class ChatRoom(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val visibleTo: List<String> = emptyList(),
    val deletedForUsers: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val participantPhotoUrls: Map<String, String> = emptyMap(),
    val buyerId: String = "",
    val sellerId: String = "",
    val listingId: String = "",
    val listingName: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val unreadCounts: Map<String, Int> = emptyMap(),
    val createdAt: Long = 0L
)

data class ChatMessage(

    val id: String = "",

    val senderId: String = "",

    val receiverId: String = "",

    val message: String = "",

    val timestamp: Long = System.currentTimeMillis(),

    val status: String = MessageStatus.DELIVERED.name,

    val readAt: Long = 0L,

    val isDeleted: Boolean = false

)

enum class MessageStatus {
    SENT,
    DELIVERED,
    READ
}
