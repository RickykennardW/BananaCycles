package com.ky.bananacycles.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ky.bananacycles.model.WasteItem
import com.ky.bananacycles.viewmodel.WasteViewModel

@Composable
fun WasteDetailScreen(
    wasteItem: WasteItem,
    viewModel: WasteViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState = viewModel.uiState
    var quantity by remember {
        mutableStateOf("")
    }
    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Image placeholder",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = wasteItem.wasteName,
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Seller: ${wasteItem.sellerName.ifBlank { "Unknown Seller" }}")
                Text("Category: ${wasteItem.category.toDisplayCategory()}")
                Text("Available Stock: ${wasteItem.stockKg} kg")
                Text("Price Per Kg: IDR ${wasteItem.pricePerKg}")
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

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
                Text("Purchase Quantity (kg)")
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = {
                val quantityValue = quantity.toDoubleOrNull() ?: 0.0

                when {
                    quantityValue <= 0.0 -> {
                        errorMessage = "Please enter a purchase quantity greater than 0 kg."
                    }

                    quantityValue > wasteItem.stockKg -> {
                        errorMessage = "Purchase quantity cannot exceed available stock."
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
            modifier = Modifier.fillMaxWidth(),
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

        Spacer(modifier = Modifier.height(10.dp))

        TextButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
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
