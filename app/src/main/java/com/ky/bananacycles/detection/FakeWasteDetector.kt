package com.ky.bananacycles.detection

import android.net.Uri
import com.ky.bananacycles.model.WasteScanResult
import kotlinx.coroutines.delay
import java.util.Locale

class FakeWasteDetector {

    suspend fun detectWaste(imageUri: Uri): WasteScanResult {
        delay(900)

        val source = imageUri.toString().lowercase(Locale.US)
        val result = when {
            source.contains("glass") -> WasteScanResult(
                wasteName = "Glass Bottle",
                category = "Inorganic",
                materialType = "Glass Bottle",
                cleanliness = "Clean",
                reuseSuggestion = "Recycle or reuse as container",
                recyclability = "High",
                materialQuality = "Good",
                suggestedPricePerKg = "Rp 500 - Rp 1.500/kg",
                priceExplanation = "Glass has lower resale value but remains recyclable when sorted.",
                confidence = 0.88f
            )
            source.contains("paper") -> WasteScanResult(
                wasteName = "Paper",
                category = "Inorganic",
                materialType = "Paper",
                cleanliness = "Clean",
                reuseSuggestion = "Recycle into paper pulp",
                recyclability = "High",
                materialQuality = "Medium",
                suggestedPricePerKg = "Rp 1.000 - Rp 2.500/kg",
                priceExplanation = "Paper price depends heavily on dryness and contamination level.",
                confidence = 0.86f
            )
            source.contains("cardboard") || source.contains("carton") || source.contains("box") -> WasteScanResult(
                wasteName = "Cardboard",
                category = "Inorganic",
                materialType = "Cardboard",
                cleanliness = "Clean",
                reuseSuggestion = "Reuse as packaging",
                recyclability = "High",
                materialQuality = "Good",
                suggestedPricePerKg = "Rp 1.500 - Rp 3.000/kg",
                priceExplanation = "Dry cardboard is easy to sort and commonly accepted by recyclers.",
                confidence = 0.87f
            )
            source.contains("aluminum") || source.contains("aluminium") || source.contains("can") -> WasteScanResult(
                wasteName = "Aluminum Can",
                category = "Inorganic",
                materialType = "Aluminum Can",
                cleanliness = "Clean",
                reuseSuggestion = "Recycle as metal scrap",
                recyclability = "High",
                materialQuality = "High",
                suggestedPricePerKg = "Rp 10.000 - Rp 20.000/kg",
                priceExplanation = "Aluminum has higher scrap value than most household recyclables.",
                confidence = 0.9f
            )
            source.contains("leaf") || source.contains("leaves") -> WasteScanResult(
                wasteName = "Leaves",
                category = "Organic",
                materialType = "Leaves",
                cleanliness = "Clean",
                reuseSuggestion = "Compost",
                recyclability = "Medium",
                materialQuality = "Medium",
                suggestedPricePerKg = "Rp 0 - Rp 1.000/kg",
                priceExplanation = "Leaves are useful for composting but usually have low marketplace price.",
                confidence = 0.82f
            )
            source.contains("organic") || source.contains("food") -> WasteScanResult(
                wasteName = "Food Waste",
                category = "Organic",
                materialType = "Food Waste",
                cleanliness = "Needs Sorting",
                reuseSuggestion = "Compost",
                recyclability = "Medium",
                materialQuality = "Medium",
                suggestedPricePerKg = "Rp 0 - Rp 1.000/kg",
                priceExplanation = "Food waste has low direct resale value but can become compost.",
                confidence = 0.84f
            )
            source.contains("unknown") || source.contains("mixed") -> WasteScanResult(
                wasteName = "Unknown Material",
                category = "Inorganic",
                materialType = "Unknown Material",
                cleanliness = "Unknown",
                reuseSuggestion = "Please classify manually",
                recyclability = "Unknown",
                materialQuality = "Unknown",
                suggestedPricePerKg = "No price suggestion",
                priceExplanation = "The material is outside the focused recyclable material set.",
                confidence = 0.36f
            )
            else -> WasteScanResult(
                wasteName = "Plastic Bottle",
                category = "Inorganic",
                materialType = "Plastic Bottle",
                cleanliness = "Clean",
                reuseSuggestion = "Recycle",
                recyclability = "High",
                materialQuality = "Good",
                suggestedPricePerKg = "Rp 2.500 - Rp 4.000/kg",
                priceExplanation = "Clean PET bottles usually have stable recycling demand.",
                confidence = 0.92f
            )
        }

        return result
    }
}
