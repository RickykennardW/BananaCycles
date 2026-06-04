package com.ky.bananacycles.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ky.bananacycles.model.WasteItem

private val MarketplacePriceColor = Color(0xFF0B3D91)

@Composable
fun WasteCard(
    wasteItem: WasteItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            ListingImage(
                imageUrl = wasteItem.imageUrl,
                listingId = wasteItem.id,
                sellerId = wasteItem.sellerId,
                height = 112.dp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = wasteItem.wasteName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Rp ${wasteItem.pricePerKg.toRupiah()} / kg",
                style = MaterialTheme.typography.titleMedium,
                color = MarketplacePriceColor,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${wasteItem.stockKg.toStockText()} kg available",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

        }

    }

}

private fun Int.toRupiah(): String {
    return "%,d".format(this).replace(",", ".")
}

private fun Double.toStockText(): String {
    return if (this % 1.0 == 0.0) {
        toInt().toString()
    } else {
        toString()
    }
}
