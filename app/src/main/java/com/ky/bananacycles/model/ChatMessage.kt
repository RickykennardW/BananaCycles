package com.ky.bananacycles.model

data class ChatMessage(

    val id: String = "",

    val senderId: String = "",

    val senderName: String = "",

    val message: String = "",

    val timestamp: Long = System.currentTimeMillis()

)