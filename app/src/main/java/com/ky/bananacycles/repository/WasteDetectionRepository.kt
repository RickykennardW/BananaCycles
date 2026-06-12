package com.ky.bananacycles.repository

import com.ky.bananacycles.detection.HeuristicWasteDetector
import com.ky.bananacycles.detection.WasteDetector
import com.ky.bananacycles.model.SelectedImage
import com.ky.bananacycles.model.WastePrediction
import com.ky.bananacycles.model.WasteScanResult

class WasteDetectionRepository(
    private val detector: WasteDetector = HeuristicWasteDetector()
) {
    fun detectWaste(image: SelectedImage): WastePrediction {
        return detector.detect(image)
    }

    fun scanWasteForSell(image: SelectedImage): WasteScanResult {
        return detector.detect(image).toScanResult()
    }

    private fun WastePrediction.toScanResult(): WasteScanResult {
        return WasteScanResult(
            wasteName = materialType,
            category = wasteCategory,
            materialType = materialType,
            cleanliness = cleanliness,
            reuseSuggestion = reuseSuggestion,
            recyclability = recyclability,
            materialQuality = materialQuality,
            suggestedPricePerKg = suggestedPricePerKg,
            priceExplanation = priceExplanation,
            confidence = confidence
        )
    }
}
