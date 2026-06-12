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
        val analysisBytes = image.bytes.preprocessForAnalysis()
        val source = image.sourceUri.lowercase(Locale.US)
        val mime = image.mimeType.lowercase(Locale.US)
        val visualHint = analysisBytes.toVisualHint()
        val evidence = "$source $mime $visualHint"

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
                materialQuality = "Unknown",
                suggestedPricePerKg = "No price suggestion",
                priceExplanation = "The image does not match the focused recyclable materials. Please verify manually.",
                confidence = if (analysisBytes.isNotEmpty()) 0.36f else 0.24f,
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
            materialQuality = match.materialQuality,
            suggestedPricePerKg = match.suggestedPricePerKg,
            priceExplanation = match.priceExplanation,
            confidence = match.confidence,
            aiGenerated = true
        )
    }

    override suspend fun detectWaste(imageUri: Uri): WasteScanResult {
        return FakeWasteDetector().detectWaste(imageUri)
    }

    private fun ByteArray.preprocessForAnalysis(): ByteArray {
        if (isEmpty()) return this

        return runCatching {
            val bitmap = BitmapFactory.decodeByteArray(this, 0, size) ?: return this
            val cropped = bitmap.centerCropSquare()
            val resized = Bitmap.createScaledBitmap(cropped, 224, 224, true)
            val enhanced = resized.normalizeBrightnessAndContrast()
            val output = ByteArrayOutputStream()
            enhanced.compress(Bitmap.CompressFormat.JPEG, 82, output)
            output.toByteArray()
        }.getOrDefault(this)
    }

    private fun Bitmap.centerCropSquare(): Bitmap {
        val size = minOf(width, height)
        val left = (width - size) / 2
        val top = (height - size) / 2
        return Bitmap.createBitmap(this, left, top, size, size)
    }

    private fun Bitmap.normalizeBrightnessAndContrast(): Bitmap {
        val output = copy(Bitmap.Config.ARGB_8888, true)
        var totalBrightness = 0L
        val sampleStep = 8
        var sampleCount = 0

        for (y in 0 until height step sampleStep) {
            for (x in 0 until width step sampleStep) {
                val pixel = getPixel(x, y)
                val red = android.graphics.Color.red(pixel)
                val green = android.graphics.Color.green(pixel)
                val blue = android.graphics.Color.blue(pixel)
                totalBrightness += (red + green + blue) / 3
                sampleCount += 1
            }
        }

        val averageBrightness = if (sampleCount == 0) 128f else totalBrightness.toFloat() / sampleCount
        val brightnessShift = 128f - averageBrightness
        val contrast = 1.15f

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = getPixel(x, y)
                val red = ((android.graphics.Color.red(pixel) + brightnessShift - 128f) * contrast + 128f).toInt().coerceIn(0, 255)
                val green = ((android.graphics.Color.green(pixel) + brightnessShift - 128f) * contrast + 128f).toInt().coerceIn(0, 255)
                val blue = ((android.graphics.Color.blue(pixel) + brightnessShift - 128f) * contrast + 128f).toInt().coerceIn(0, 255)
                output.setPixel(x, y, android.graphics.Color.rgb(red, green, blue))
            }
        }

        return output
    }

    private fun ByteArray.toVisualHint(): String {
        val bitmap = BitmapFactory.decodeByteArray(this, 0, size) ?: return ""
        var greenDominant = 0
        var brownDominant = 0
        var whiteDominant = 0
        var metallicDominant = 0
        val sampleStep = 16

        for (y in 0 until bitmap.height step sampleStep) {
            for (x in 0 until bitmap.width step sampleStep) {
                val pixel = bitmap.getPixel(x, y)
                val red = android.graphics.Color.red(pixel)
                val green = android.graphics.Color.green(pixel)
                val blue = android.graphics.Color.blue(pixel)
                when {
                    green > red + 20 && green > blue + 20 -> greenDominant += 1
                    red > 95 && green in 55..145 && blue < 100 -> brownDominant += 1
                    red > 190 && green > 190 && blue > 180 -> whiteDominant += 1
                    kotlin.math.abs(red - green) < 18 && kotlin.math.abs(green - blue) < 18 && red in 105..210 -> metallicDominant += 1
                }
            }
        }

        val strongestHint = maxOf(greenDominant, brownDominant, whiteDominant, metallicDominant)
        if (strongestHint == 0) {
            return ""
        }

        return when (strongestHint) {
            greenDominant -> "leaf leaves"
            brownDominant -> "cardboard food"
            whiteDominant -> "paper plastic"
            metallicDominant -> "aluminum can metal"
            else -> ""
        }
    }

    private data class DetectionRule(
        val keywords: List<String>,
        val category: String,
        val materialType: String,
        val cleanliness: String,
        val contamination: String,
        val reuseSuggestion: String,
        val recyclability: String,
        val materialQuality: String,
        val suggestedPricePerKg: String,
        val priceExplanation: String,
        val confidence: Float
    )

    companion object {
        private val DETECTION_RULES = listOf(
            DetectionRule(listOf("plastic", "pet", "plastic bottle"), "Inorganic", "Plastic Bottle", "Clean", "Low", "Recycle", "High", "Good", "Rp 2.500 - Rp 4.000/kg", "Clean PET bottles usually have stable recycling demand.", 0.92f),
            DetectionRule(listOf("glass", "jar", "glass bottle"), "Inorganic", "Glass Bottle", "Clean", "Low", "Recycle or reuse as container", "High", "Good", "Rp 500 - Rp 1.500/kg", "Glass has lower resale value but remains recyclable when sorted.", 0.88f),
            DetectionRule(listOf("aluminum", "aluminium", "can", "metal can"), "Inorganic", "Aluminum Can", "Clean", "Low", "Recycle as metal scrap", "High", "High", "Rp 10.000 - Rp 20.000/kg", "Aluminum has higher scrap value than most household recyclables.", 0.9f),
            DetectionRule(listOf("cardboard", "box", "carton"), "Inorganic", "Cardboard", "Clean", "Low", "Reuse as packaging", "High", "Good", "Rp 1.500 - Rp 3.000/kg", "Dry cardboard is easy to sort and commonly accepted by recyclers.", 0.87f),
            DetectionRule(listOf("paper", "newspaper", "document"), "Inorganic", "Paper", "Clean", "Low", "Recycle into paper pulp", "Medium", "Medium", "Rp 1.000 - Rp 2.500/kg", "Paper price depends heavily on dryness and contamination level.", 0.86f),
            DetectionRule(listOf("food", "food waste", "banana", "leftover"), "Organic", "Food Waste", "Needs Sorting", "Medium", "Compost", "Medium", "Medium", "Rp 0 - Rp 1.000/kg", "Food waste has low direct resale value but can become compost.", 0.84f),
            DetectionRule(listOf("leaf", "leaves", "plant"), "Organic", "Leaves", "Clean", "Low", "Compost", "Medium", "Medium", "Rp 0 - Rp 1.000/kg", "Leaves are useful for composting but usually have low marketplace price.", 0.82f)
        )
    }
}
