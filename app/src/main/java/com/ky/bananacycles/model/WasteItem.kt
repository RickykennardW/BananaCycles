package com.ky.bananacycles.model

data class WasteItem(

    val id: String = "",

    val sellerId: String = "",

    val sellerName: String = "",

    val wasteName: String = "",

    val category: String = "",

    val stockKg: Double = 0.0,

    val pricePerKg: Int = 0,

    val imageUrl: String = "",

    val status: String = ListingStatus.ACTIVE.name,

    val createdAt: Long = 0L

)

enum class ListingStatus {
    ACTIVE,
    SOLD_OUT,
    PENDING
}
