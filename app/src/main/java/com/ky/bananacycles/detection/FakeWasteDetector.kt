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
            source.contains("paper") -> WasteScanResult(
                wasteName = "Paper Waste",
                category = "Inorganic",
                materialType = "Paper",
                cleanliness = "Clean",
                reuseSuggestion = "Recycle into paper pulp",
                recyclability = "High",
                confidence = 0.91f
            )
            source.contains("metal") || source.contains("can") -> WasteScanResult(
                wasteName = "Metal Can",
                category = "Inorganic",
                materialType = "Aluminum Can",
                cleanliness = "Clean",
                reuseSuggestion = "Recycle as metal scrap",
                recyclability = "High",
                confidence = 0.89f
            )
            source.contains("organic") || source.contains("food") -> WasteScanResult(
                wasteName = "Organic Waste",
                category = "Organic",
                materialType = "Food Waste",
                cleanliness = "Needs Sorting",
                reuseSuggestion = "Compost",
                recyclability = "Medium",
                confidence = 0.82f
            )
            source.contains("unknown") || source.contains("mixed") -> WasteScanResult(
                wasteName = "Mixed Waste",
                category = "Inorganic",
                materialType = "Mixed Material",
                cleanliness = "Needs Sorting",
                reuseSuggestion = "Sort before recycling",
                recyclability = "Low",
                confidence = 0.42f
            )
            else -> WasteScanResult(
                wasteName = "Plastic Bottle",
                category = "Inorganic",
                materialType = "Plastic Bottle",
                cleanliness = "Clean",
                reuseSuggestion = "Recycle",
                recyclability = "High",
                confidence = 0.94f
            )
        }

        return result
    }
}
