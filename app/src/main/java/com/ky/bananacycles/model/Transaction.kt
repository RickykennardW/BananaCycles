package com.ky.bananacycles.model

data class Order(
    val orderId: String = "",
    val listingId: String = "",
    val buyerId: String = "",
    val buyerName: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val productName: String = "",
    val productImage: String = "",
    val quantityPurchased: Double = 0.0,
    val pricePerKg: Int = 0,
    val totalPrice: Int = 0,
    val status: String = OrderStatus.PACKING.name,
    val createdAt: Long = 0L,
    val completedAt: Long = 0L,
    val cancelledAt: Long = 0L
)

enum class OrderStatus {
    PACKING,
    SENDING,
    MEET,
    FINISHED,
    CANCELLED
}

data class UserStats(
    val userId: String = "",
    val totalSalesCompleted: Int = 0,
    val totalPurchasesCompleted: Int = 0,
    val aiAssistedListings: Int = 0
)

data class Transaction(
    val id: String,
    val wasteName: String,
    val price: Int,
    var status: String
)
