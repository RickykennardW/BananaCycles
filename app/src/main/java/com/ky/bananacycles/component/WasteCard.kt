package com.ky.bananacycles.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
            .padding(bottom = 12.dp)
            .clickable {
                onClick()
            },
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            ListingImage(
                imageUrl = wasteItem.imageUrl
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = wasteItem.wasteName,
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Category: ${wasteItem.category.toDisplayCategory()}"
            )

            Text(
                text = "Stock: ${wasteItem.stockKg} kg"
            )

            Text(
                text = "IDR ${wasteItem.pricePerKg} / kg"
            )

            Text(
                text = "Status: ${wasteItem.status.toDisplayStatus()}"
            )

        }

    }

}

private fun String.toDisplayCategory(): String {
    return when {
        equals("Organik", ignoreCase = true) -> "Organic"
        equals("Anorganik", ignoreCase = true) -> "Inorganic"
        else -> this
    }
}

private fun String.toDisplayStatus(): String {
    return when (this) {
        "ACTIVE" -> "Active"
        "SOLD_OUT" -> "Sold Out"
        "PENDING" -> "Pending"
        else -> this
    }
}
