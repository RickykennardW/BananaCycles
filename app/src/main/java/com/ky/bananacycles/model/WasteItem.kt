package com.ky.bananacycles.model

data class WasteItem(

    val id: String = "",

    val sellerId: String = "",

    val sellerName: String = "",

    val sellerPhotoUrl: String = "",

    val wasteName: String = "",

    val description: String = "",

    val category: String = "",

    val stockKg: Double = 0.0,

    val pricePerKg: Int = 0,

    val imageUrl: String = "",

    val status: String = ListingStatus.ACTIVE.name,

    val createdAt: Long = 0L,

    val aiDetectedCategory: String = "",

    val aiConfidence: Double = 0.0,

    val materialType: String = "",

    val cleanliness: String = "",

    val contamination: String = "",

    val reuseSuggestion: String = "",

    val recyclability: String = "",

    val materialQuality: String = "",

    val suggestedPricePerKg: String = "",

    val priceExplanation: String = "",

    val aiGenerated: Boolean = false

)

enum class ListingStatus {
    ACTIVE,
    SOLD_OUT,
    PENDING
}
