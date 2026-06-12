package com.ky.bananacycles.model

data class WastePrediction(
    val wasteCategory: String = "Inorganic",
    val materialType: String = "Unknown Material",
    val cleanliness: String = "Unknown",
    val contamination: String = "Unknown",
    val reuseSuggestion: String = "Classify manually",
    val recyclability: String = "Unknown",
    val confidence: Float = 0f,
    val aiGenerated: Boolean = false
) {
    val confidencePercent: Int
        get() = (confidence.coerceIn(0f, 1f) * 100).toInt()

    val isConfident: Boolean
        get() = confidence >= 0.5f
}
