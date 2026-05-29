package com.ky.bananacycles.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ky.bananacycles.model.WasteItem

@Composable
fun WasteCard(
    wasteItem: WasteItem,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
                onClick()
            }
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = wasteItem.wasteName,
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = "Kategori: ${wasteItem.category}"
            )

            Text(
                text = "Berat: ${wasteItem.weight} Kg"
            )

            Text(
                text = "Estimasi Harga: Rp ${wasteItem.estimatedPrice}"
            )
        }
    }
}