package com.ky.bananacycles.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ky.bananacycles.model.WasteItem

@Composable
fun WasteDetailScreen(
    wasteItem: WasteItem
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {

        Text(
            text = wasteItem.wasteName,
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Kategori : ${wasteItem.category}"
        )

        Text(
            text = "Berat : ${wasteItem.weight} Kg"
        )

        Text(
            text = "Estimasi Harga : Rp ${wasteItem.estimatedPrice}"
        )
    }
}