package com.ky.bananacycles.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ky.bananacycles.component.WasteCard
import com.ky.bananacycles.model.WasteItem

@Composable
fun MarketScreen(
    onWasteClick: (WasteItem) -> Unit
) {

    var searchQuery by remember {
        mutableStateOf("")
    }

    var selectedCategory by remember {
        mutableStateOf("Semua")
    }

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

    val filteredWasteList = wasteList.filter { waste ->

        val matchesSearch = waste.wasteName.contains(
            searchQuery,
            ignoreCase = true
        )

        val matchesCategory = when (selectedCategory) {

            "Organik" -> waste.category == "Organik"

            "Anorganik" -> waste.category == "Anorganik"

            else -> true
        }

        matchesSearch && matchesCategory
    }

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

        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
            },
            label = {
                Text("Cari Limbah")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            FilterChip(
                selected = selectedCategory == "Semua",
                onClick = {
                    selectedCategory = "Semua"
                },
                label = {
                    Text("Semua")
                }
            )

            FilterChip(
                selected = selectedCategory == "Organik",
                onClick = {
                    selectedCategory = "Organik"
                },
                label = {
                    Text("Organik")
                }
            )

            FilterChip(
                selected = selectedCategory == "Anorganik",
                onClick = {
                    selectedCategory = "Anorganik"
                },
                label = {
                    Text("Anorganik")
                }
            )

        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        LazyColumn {

            items(filteredWasteList) { waste ->

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