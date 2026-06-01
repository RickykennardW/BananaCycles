package com.ky.bananacycles.repository

import androidx.compose.runtime.mutableStateListOf
import com.ky.bananacycles.model.WasteItem

object WasteRepository {

    val wasteList = mutableStateListOf(
// Ini adalah comment
        WasteItem(
            id = "1",
            wasteName = "Botol Plastik",
            category = "Anorganik",
            weight = 5.0,
            estimatedPrice = 25000
        ),

        WasteItem(
            id = "2",
            wasteName = "Kardus Bekas",
            category = "Anorganik",
            weight = 10.0,
            estimatedPrice = 50000
        ),

        WasteItem(
            id = "3",
            wasteName = "Daun Kering",
            category = "Organik",
            weight = 3.0,
            estimatedPrice = 6000
        )
    )

    fun addWaste(waste: WasteItem) {
        wasteList.add(waste)
    }
}