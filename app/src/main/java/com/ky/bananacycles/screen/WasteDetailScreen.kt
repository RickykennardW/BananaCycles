package com.ky.bananacycles.screen

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ky.bananacycles.component.ListingImage
import com.ky.bananacycles.component.UserAvatar
import com.ky.bananacycles.model.WasteItem
import com.ky.bananacycles.viewmodel.WasteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WasteDetailScreen(
    wasteItem: WasteItem,
    viewModel: WasteViewModel,
    onBack: () -> Unit,
    onChatSeller: (WasteItem) -> Unit,
    onSellerClick: (String) -> Unit
) {
    val context = LocalContext.current
    val uiState = viewModel.uiState
    var quantity by remember {
        mutableStateOf("")
    }
    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = wasteItem.wasteName,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = 20.dp,
                end = 20.dp,
                bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = wasteItem.wasteName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                SellerProfileCard(
                    wasteItem = wasteItem,
                    onClick = {
                        onSellerClick(wasteItem.sellerId)
                    }
                )
            }

            item {
                ListingImage(
                    imageUrl = wasteItem.imageUrl,
                    listingId = wasteItem.id,
                    sellerId = wasteItem.sellerId,
                    height = 220.dp
                )
            }

            item {
                ListingInfoCard(
                    title = "Remaining Stock",
                    value = "${wasteItem.stockKg.toStockText()} kg"
                )
            }

            item {
                ListingInfoCard(
                    title = "Price",
                    value = "Rp ${wasteItem.pricePerKg.toRupiah()} / kg",
                    emphasizeValue = true
                )
            }

            item {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() || it == '.' }) {
                            quantity = input
                            errorMessage = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Quantity (kg)")
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            errorMessage?.let { message ->
                item {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        val quantityValue = quantity.toDoubleOrNull() ?: 0.0

                        when {
                            quantityValue <= 0.0 -> {
                                errorMessage = "Please enter a purchase quantity greater than 0 kg."
                            }

                            quantityValue > wasteItem.stockKg -> {
                                errorMessage = "Requested quantity exceeds available stock."
                            }

                            else -> {
                                viewModel.purchaseListing(
                                    listing = wasteItem,
                                    quantityKg = quantityValue,
                                    onSuccess = {
                                        Toast.makeText(
                                            context,
                                            "Purchase completed successfully.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        onBack()
                                    },
                                    onFailure = { message ->
                                        errorMessage = message
                                    }
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !uiState.isPurchasing
                ) {
                    if (uiState.isPurchasing) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Purchase")
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = {
                        onChatSeller(wasteItem)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !uiState.isPurchasing
                ) {
                    Text("Chat Seller")
                }
            }
        }
    }
}

@Composable
private fun ListingInfoCard(
    title: String,
    value: String,
    emphasizeValue: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = if (emphasizeValue) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                },
                color = if (emphasizeValue) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
private fun SellerProfileCard(
    wasteItem: WasteItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            UserAvatar(
                photoUrl = wasteItem.sellerPhotoUrl,
                size = 46.dp
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = wasteItem.sellerName.ifBlank { "BananaCycles Seller" },
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "View Seller Profile",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.List,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun Double.toStockText(): String {
    return if (this % 1.0 == 0.0) {
        toInt().toString()
    } else {
        toString()
    }
}

private fun Int.toRupiah(): String {
    return "%,d".format(this).replace(",", ".")
}
