package com.ky.bananacycles.screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ky.bananacycles.model.WasteItem
import com.ky.bananacycles.ui.theme.BananaCyclesTheme
import com.ky.bananacycles.repository.WasteRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadWasteScreen() {

    val context = LocalContext.current

    var wasteName by remember {
        mutableStateOf("")
    }

    val categories = listOf(
        "Organik",
        "Anorganik"
    )

    var selectedCategory by remember {
        mutableStateOf("Organik")
    }

    var expanded by remember {
        mutableStateOf(false)
    }

    var weight by remember {
        mutableStateOf("")
    }

    var showErrorDialog by remember {
        mutableStateOf(false)
    }

    val estimatedPrice = remember(
        selectedCategory,
        weight
    ) {

        val weightValue = weight.toDoubleOrNull() ?: 0.0

        val pricePerKg = when (selectedCategory) {
            "Organik" -> 2000
            "Anorganik" -> 5000
            else -> 0
        }

        (weightValue * pricePerKg).toInt()
    }

    if (showErrorDialog) {

        AlertDialog(
            onDismissRequest = {
                showErrorDialog = false
            },
            confirmButton = {

                TextButton(
                    onClick = {
                        showErrorDialog = false
                    }
                ) {
                    Text("OK")
                }

            },
            title = {
                Text("Input Tidak Valid")
            },
            text = {
                Text(
                    "Nama limbah tidak boleh kosong dan berat harus lebih dari 0 Kg."
                )
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 50.dp)
        ) {

            Text(
                text = "♻ Upload Limbah",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = "Jual limbah Anda dengan mudah",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(
                modifier = Modifier.height(28.dp)
            )

            OutlinedTextField(
                value = wasteName,
                onValueChange = {
                    wasteName = it
                },
                label = {
                    Text("Nama Limbah")
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = {
                    expanded = !expanded
                }
            ) {

                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = {},
                    readOnly = true,
                    label = {
                        Text("Kategori")
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = expanded
                        )
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = {
                        expanded = false
                    }
                ) {

                    categories.forEach { category ->

                        DropdownMenuItem(
                            text = {
                                Text(category)
                            },
                            onClick = {

                                selectedCategory = category
                                expanded = false

                            }
                        )

                    }

                }

            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            OutlinedTextField(
                value = weight,
                onValueChange = { input ->

                    if (
                        input.all {
                            it.isDigit() || it == '.'
                        }
                    ) {
                        weight = input
                    }

                },
                label = {
                    Text("Berat (Kg)")
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                )
            )

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 6.dp
                )
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "Estimasi Harga"
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = "Rp $estimatedPrice",
                        style = MaterialTheme.typography.headlineSmall
                    )

                }

            }

            Spacer(
                modifier = Modifier.height(28.dp)
            )

            Button(
                onClick = {

                    val weightValue =
                        weight.toDoubleOrNull() ?: 0.0

                    if (
                        wasteName.isBlank() ||
                        weightValue <= 0
                    ) {

                        showErrorDialog = true

                    } else {

                        WasteRepository.addWaste(

                            WasteItem(
                                id = System.currentTimeMillis().toString(),
                                wasteName = wasteName,
                                category = selectedCategory,
                                weight = weightValue,
                                estimatedPrice = estimatedPrice
                            )

                        )

                        Toast.makeText(
                            context,
                            "Limbah berhasil diupload",
                            Toast.LENGTH_SHORT
                        ).show()

                        wasteName = ""
                        weight = ""

                    }

                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {

                Text(
                    text = "Upload Limbah"
                )

            }

        }

    }

}

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
fun UploadWasteScreenPreview() {

    BananaCyclesTheme {

        UploadWasteScreen()

    }

}