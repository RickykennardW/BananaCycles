package com.ky.bananacycles.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ky.bananacycles.component.WasteCard
import com.ky.bananacycles.model.WasteItem

@Composable
fun MarketScreen(
    onWasteClick: (WasteItem) -> Unit
) {

    val wasteList = listOf(
        WasteItem(
            wasteName = "Botol Plastik",
            category = "Anorganik",
            weight = 5.0,
            estimatedPrice = 25000
        ),
        WasteItem(
            wasteName = "Kardus Bekas",
            category = "Anorganik",
            weight = 10.0,
            estimatedPrice = 50000
        ),
        WasteItem(
            wasteName = "Daun Kering",
            category = "Organik",
            weight = 3.0,
            estimatedPrice = 6000
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Marketplace",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        LazyColumn {

            items(wasteList) { waste ->

                WasteCard(
                    wasteItem = waste,
                    onClick = {
                        onWasteClick(waste)
                    }
                )

            }

        }

    }

}