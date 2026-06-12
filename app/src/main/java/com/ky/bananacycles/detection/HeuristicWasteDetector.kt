package com.ky.bananacycles.detection

import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.ky.bananacycles.model.SelectedImage
import com.ky.bananacycles.model.WastePrediction
import com.ky.bananacycles.model.WasteScanResult
import java.io.ByteArrayOutputStream
import java.util.Locale

class HeuristicWasteDetector : WasteDetector {

    override fun detect(image: SelectedImage): WastePrediction {
        // Keep this engine lightweight and swappable: the UI talks to WasteDetector,
        // while this default implementation uses local heuristics until a real ML model is added.
        val analysisBytes = image.bytes.compressForAnalysis()
        val source = image.sourceUri.lowercase(Locale.US)
        val mime = image.mimeType.lowercase(Locale.US)
        val evidence = "$source $mime"

        val match = DETECTION_RULES.firstOrNull { rule ->
            rule.keywords.any { keyword -> evidence.contains(keyword) }
        }

        if (match == null) {
            return WastePrediction(
                wasteCategory = "Inorganic",
                materialType = "Unknown Material",
                cleanliness = "Unknown",
                contamination = "Unknown",
                reuseSuggestion = "Please classify manually",
                recyclability = "Unknown",
                confidence = if (analysisBytes.isNotEmpty()) 0.42f else 0.28f,
                aiGenerated = false
            )
        }

        val contamination = when {
            evidence.contains("dirty") || evidence.contains("food") || evidence.contains("stain") -> "Medium"
            evidence.contains("mixed") || evidence.contains("trash") -> "High"
            else -> match.contamination
        }

        val cleanliness = when (contamination) {
            "Low" -> "Clean"
            "Medium" -> "Needs Sorting"
            "High" -> "Contaminated"
            else -> match.cleanliness
        }

        return WastePrediction(
            wasteCategory = match.category,
            materialType = match.materialType,
            cleanliness = cleanliness,
            contamination = contamination,
            reuseSuggestion = match.reuseSuggestion,
            recyclability = match.recyclability,
            confidence = match.confidence,
            aiGenerated = true
        )
    }

    override suspend fun detectWaste(imageUri: Uri): WasteScanResult {
        return FakeWasteDetector().detectWaste(imageUri)
    }

    private fun ByteArray.compressForAnalysis(): ByteArray {
        if (isEmpty()) return this

        return runCatching {
            val bitmap = BitmapFactory.decodeByteArray(this, 0, size) ?: return this
            val output = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, output)
            output.toByteArray()
        }.getOrDefault(this)
    }

    private data class DetectionRule(
        val keywords: List<String>,
        val category: String,
        val materialType: String,
        val cleanliness: String,
        val contamination: String,
        val reuseSuggestion: String,
        val recyclability: String,
        val confidence: Float
    )

    companion object {
        private val DETECTION_RULES = listOf(
            DetectionRule(listOf("bottle", "plastic", "pet"), "Inorganic", "Plastic Bottle", "Clean", "Low", "Recycle", "High", 0.96f),
            DetectionRule(listOf("paper", "newspaper", "document"), "Inorganic", "Paper", "Clean", "Low", "Recycle into paper pulp", "High", 0.91f),
            DetectionRule(listOf("cardboard", "box", "carton"), "Inorganic", "Cardboard Box", "Clean", "Low", "Reuse as packaging", "High", 0.9f),
            DetectionRule(listOf("glass", "jar"), "Inorganic", "Glass Container", "Clean", "Low", "Recycle or reuse as container", "High", 0.88f),
            DetectionRule(listOf("metal", "can", "aluminum", "steel"), "Inorganic", "Metal Can", "Clean", "Low", "Recycle as metal scrap", "High", 0.89f),
            DetectionRule(listOf("organic", "banana", "food", "leaf", "compost"), "Organic", "Organic Waste", "Needs Sorting", "Medium", "Compost", "Medium", 0.86f),
            DetectionRule(listOf("electronic", "ewaste", "battery", "cable", "phone"), "Inorganic", "Electronic Waste", "Needs Sorting", "Medium", "Send to certified e-waste recycler", "Medium", 0.84f),
            DetectionRule(listOf("fabric", "cloth", "textile", "shirt"), "Inorganic", "Fabric/Textile", "Clean", "Low", "Upcycle or donate", "Medium", 0.82f),
            DetectionRule(listOf("rubber", "tire", "tyre"), "Inorganic", "Rubber Material", "Needs Sorting", "Medium", "Recycle into rubber material", "Medium", 0.81f),
            DetectionRule(listOf("wood", "timber", "pallet"), "Inorganic", "Wood Scrap", "Clean", "Low", "Reuse or upcycle", "Medium", 0.8f),
            DetectionRule(listOf("mixed", "trash", "garbage"), "Inorganic", "Mixed Waste", "Contaminated", "High", "Sort before recycling", "Low", 0.72f)
        )
    }
}
