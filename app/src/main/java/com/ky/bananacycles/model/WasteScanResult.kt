package com.ky.bananacycles.model

data class WasteScanResult(
    val wasteName: String = "",
    val category: String = "Inorganic",
    val materialType: String = "",
    val cleanliness: String = "",
    val reuseSuggestion: String = "",
    val recyclability: String = "",
    val materialQuality: String = "",
    val suggestedPricePerKg: String = "",
    val priceExplanation: String = "",
    val confidence: Float = 0f
) {
    val confidencePercent: Int
        get() = (confidence.coerceIn(0f, 1f) * 100).toInt()

    val isConfident: Boolean
        get() = confidence >= 0.7f
}
