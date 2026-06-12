package com.ky.bananacycles.detection

import android.net.Uri
import com.ky.bananacycles.model.SelectedImage
import com.ky.bananacycles.model.WastePrediction
import com.ky.bananacycles.model.WasteScanResult

interface WasteDetector {
    fun detect(image: SelectedImage): WastePrediction

    suspend fun detectWaste(imageUri: Uri): WasteScanResult
}
