package com.ky.bananacycles.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ky.bananacycles.component.WasteCard
import com.ky.bananacycles.viewmodel.WasteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadWasteScreen(
    viewModel: WasteViewModel
) {
    val context = LocalContext.current
    val uiState = viewModel.uiState

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

    LaunchedEffect(Unit) {
        viewModel.loadMyListings()
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
                Text("Nama limbah tidak boleh kosong dan berat harus lebih dari 0 Kg.")
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 50.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "My Listings",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    Text(
                        text = "Upload dan kelola limbah yang Anda jual",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
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
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
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

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 4.dp
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

                    Button(
                        onClick = {
                            val weightValue = weight.toDoubleOrNull() ?: 0.0

                            if (
                                wasteName.isBlank() ||
                                weightValue <= 0
                            ) {
                                showErrorDialog = true
                            } else {
                                viewModel.addListing(
                                    wasteName = wasteName.trim(),
                                    category = selectedCategory,
                                    weight = weightValue,
                                    estimatedPrice = estimatedPrice,
                                    onSuccess = {
                                        Toast.makeText(
                                            context,
                                            "Limbah berhasil diupload",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        wasteName = ""
                                        weight = ""
                                    },
                                    onFailure = { message ->
                                        Toast.makeText(
                                            context,
                                            message,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !uiState.isUploading,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (uiState.isUploading) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Upload Limbah"
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Listing Saya",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            if (uiState.isMyListingsLoading) {
                item {
                    CircularProgressIndicator()
                }
            } else if (uiState.myListings.isEmpty()) {
                item {
                    Text(
                        text = "Anda belum mengupload listing.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                items(
                    items = uiState.myListings,
                    key = { waste ->
                        waste.id
                    }
                ) { waste ->
                    WasteCard(
                        wasteItem = waste,
                        onClick = {}
                    )
                }
            }
        }
    }
}
