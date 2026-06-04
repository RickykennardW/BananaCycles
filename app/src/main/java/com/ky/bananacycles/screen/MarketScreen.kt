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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ky.bananacycles.component.WasteCard
import com.ky.bananacycles.model.WasteItem
import com.ky.bananacycles.viewmodel.WasteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScreen(
    viewModel: WasteViewModel,
    onWasteClick: (WasteItem) -> Unit
) {
    var searchQuery by remember {
        mutableStateOf("")
    }

    var selectedCategory by remember {
        mutableStateOf("All")
    }

    val uiState = viewModel.uiState

    LaunchedEffect(Unit) {
        viewModel.loadMarketListings()
    }

    val filteredWasteList = uiState.marketListings.filter { waste ->

        val matchesSearch = waste.wasteName.contains(
            searchQuery,
            ignoreCase = true
        )

        val matchesCategory = when (selectedCategory) {

            "Organic" -> {
                waste.category.isOrganicCategory()
            }

            "Inorganic" -> {
                waste.category.isInorganicCategory()
            }

            else -> true
        }

        matchesSearch && matchesCategory
    }

    PullToRefreshBox(
        isRefreshing = uiState.isMarketRefreshing,
        onRefresh = {
            viewModel.loadMarketListings(forceRefresh = true)
        }
    ) {
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
                    Text("Search Waste")
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
                    selected = selectedCategory == "All",
                    onClick = {
                        selectedCategory = "All"
                    },
                    label = {
                        Text("All")
                    }
                )

                FilterChip(
                    selected = selectedCategory == "Organic",
                    onClick = {
                        selectedCategory = "Organic"
                    },
                    label = {
                        Text("Organic")
                    }
                )

                FilterChip(
                    selected = selectedCategory == "Inorganic",
                    onClick = {
                        selectedCategory = "Inorganic"
                    },
                    label = {
                        Text("Inorganic")
                    }
                )

            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            uiState.errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )
            }

            if (uiState.isMarketLoading) {

                CircularProgressIndicator()

            } else if (filteredWasteList.isEmpty()) {

                Text(
                    text = "No waste listings are available.",
                    style = MaterialTheme.typography.bodyLarge
                )

            } else {

                LazyColumn {

                    items(
                        items = filteredWasteList,
                        key = { waste ->
                            "${waste.id}-${waste.imageUrl}"
                        }
                    ) { waste ->

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
    }

}

private fun String.isOrganicCategory(): Boolean {
    return equals("Organic", ignoreCase = true) ||
        equals("Organik", ignoreCase = true)
}

private fun String.isInorganicCategory(): Boolean {
    return equals("Inorganic", ignoreCase = true) ||
        equals("Anorganik", ignoreCase = true)
}
