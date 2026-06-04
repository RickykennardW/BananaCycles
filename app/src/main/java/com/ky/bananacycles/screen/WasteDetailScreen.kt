package com.ky.bananacycles.screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ky.bananacycles.model.Transaction
import com.ky.bananacycles.model.WasteItem
import com.ky.bananacycles.repository.TransactionRepository

@Composable
fun WasteDetailScreen(
    wasteItem: WasteItem,
    onBack: () -> Unit
) {

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {

        Text(
            text = wasteItem.wasteName,
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Text(
            text = "Category: ${wasteItem.category.toDisplayCategory()}"
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Weight: ${wasteItem.weight} kg"
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Estimated Price: IDR ${wasteItem.estimatedPrice}"
        )

        Spacer(
            modifier = Modifier.height(32.dp)
        )

        Button(
            onClick = {

                TransactionRepository.addTransaction(

                    Transaction(
                        id = System.currentTimeMillis().toString(),
                        wasteName = wasteItem.wasteName,
                        price = wasteItem.estimatedPrice,
                        status = "Pending"
                    )

                )

                Toast.makeText(
                    context,
                    "COD order created successfully.",
                    Toast.LENGTH_SHORT
                ).show()

            },
            modifier = Modifier.fillMaxWidth()
        ) {

            Text("Buy (COD)")

        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Button(
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
