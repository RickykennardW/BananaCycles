package com.ky.bananacycles.repository

import android.net.Uri
import com.ky.bananacycles.detection.FakeWasteDetector
import com.ky.bananacycles.detection.HeuristicWasteDetector
import com.ky.bananacycles.detection.WasteDetector
import com.ky.bananacycles.model.SelectedImage
import com.ky.bananacycles.model.WastePrediction
import com.ky.bananacycles.model.WasteScanResult

class WasteDetectionRepository(
    private val detector: WasteDetector = HeuristicWasteDetector(),
    private val scanDetector: FakeWasteDetector = FakeWasteDetector()
) {
    fun detectWaste(image: SelectedImage): WastePrediction {
        return detector.detect(image)
    }

    suspend fun detectWaste(imageUri: Uri): WasteScanResult {
        return scanDetector.detectWaste(imageUri)
    }
}
