package com.ky.bananacycles.screen

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Scaffold
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
import com.ky.bananacycles.component.ListingImage
import com.ky.bananacycles.model.ListingStatus
import com.ky.bananacycles.model.SelectedImage
import com.ky.bananacycles.model.WasteItem
import com.ky.bananacycles.viewmodel.WasteViewModel
import java.net.URI
import java.util.Locale

private enum class SellFilter(
    val label: String
) {
    ALL("All"),
    ACTIVE("Active"),
    SOLD_OUT("Sold Out"),
    PENDING("Pending")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadWasteScreen(
    viewModel: WasteViewModel
) {
    val context = LocalContext.current
    val uiState = viewModel.uiState
    var selectedFilter by remember {
        mutableStateOf(SellFilter.ALL)
    }
    var editingListing by remember {
        mutableStateOf<WasteItem?>(null)
    }
    var isFormOpen by remember {
        mutableStateOf(false)
    }
    var stockAction by remember {
        mutableStateOf<StockAction?>(null)
    }

    LaunchedEffect(Unit) {
        viewModel.loadMyListings()
    }

    val filteredListings = uiState.myListings.filter { listing ->
        when (selectedFilter) {
            SellFilter.ALL -> true
            SellFilter.ACTIVE -> listing.status == ListingStatus.ACTIVE.name
            SellFilter.SOLD_OUT -> listing.status == ListingStatus.SOLD_OUT.name
            SellFilter.PENDING -> listing.status == ListingStatus.PENDING.name
        }
    }

    if (isFormOpen) {
        ListingFormDialog(
            initialListing = editingListing,
            isSaving = uiState.isSaving,
            uploadProgress = uiState.imageUploadProgress,
            externalErrorMessage = uiState.errorMessage,
            onDismiss = {
                isFormOpen = false
                editingListing = null
            },
            onSave = { form ->
                val onSuccess = {
                    Toast.makeText(
                        context,
                        if (editingListing == null) {
                            "Waste listing created successfully."
                        } else {
                            "Waste listing updated successfully."
                        },
                        Toast.LENGTH_SHORT
                    ).show()
                    isFormOpen = false
                    editingListing = null
                }

                val onFailure: (String) -> Unit = { message ->
                    Toast.makeText(
                        context,
                        message,
                        Toast.LENGTH_SHORT
                    ).show()
                }

                val listing = editingListing
                if (listing == null) {
                    viewModel.addListing(
                        wasteName = form.wasteName,
                        category = form.category,
                        stockKg = form.stockKg,
                        pricePerKg = form.pricePerKg,
                        selectedImage = form.selectedImage,
                        existingImageUrl = form.existingImageUrl,
                        onSuccess = onSuccess,
                        onFailure = onFailure
                    )
                } else {
                    viewModel.updateListing(
                        listingId = listing.id,
                        wasteName = form.wasteName,
                        category = form.category,
                        pricePerKg = form.pricePerKg,
                        sellerId = listing.sellerId,
                        selectedImage = form.selectedImage,
                        existingImageUrl = form.existingImageUrl,
                        onSuccess = onSuccess,
                        onFailure = onFailure
                    )
                }
            }
        )
    }

    stockAction?.let { action ->
        StockActionDialog(
            action = action,
            isSaving = uiState.isSaving,
            onDismiss = {
                stockAction = null
            },
            onConfirm = { quantity ->
                val onSuccess = {
                    Toast.makeText(
                        context,
                        "Stock updated successfully.",
                        Toast.LENGTH_SHORT
                    ).show()
                    stockAction = null
                }
                val onFailure: (String) -> Unit = { message ->
                    Toast.makeText(
                        context,
                        message,
                        Toast.LENGTH_SHORT
                    ).show()
                }

                when (action.type) {
                    StockActionType.ADD -> {
                        viewModel.addStock(
                            listingId = action.listing.id,
                            quantityKg = quantity,
                            onSuccess = onSuccess,
                            onFailure = onFailure
                        )
                    }

                    StockActionType.REMOVE -> {
                        viewModel.removeStock(
                            listing = action.listing,
                            quantityKg = quantity,
                            onSuccess = onSuccess,
                            onFailure = onFailure
                        )
                    }
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingListing = null
                    isFormOpen = true
                }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Create New Listing")
                }
            }
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "My Waste Listings",
                    style = MaterialTheme.typography.headlineMedium
                )

                Text(
                    text = "Manage stock, prices, and availability",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SellFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = {
                                selectedFilter = filter
                            },
                            label = {
                                Text(filter.label)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                PullToRefreshBox(
                    isRefreshing = uiState.isMyListingsRefreshing,
                    onRefresh = {
                        viewModel.loadMyListings(forceRefresh = true)
                    }
                ) {
                    if (uiState.isMyListingsLoading) {
                        CircularProgressIndicator()
                    } else if (filteredListings.isEmpty()) {
                        Text(
                            text = "No listings match this filter.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = filteredListings,
                                key = { listing ->
                                    "${listing.id}-${listing.imageUrl}"
                                }
                            ) { listing ->
                                SellerListingCard(
                                    listing = listing,
                                    isSaving = uiState.isSaving,
                                    onEdit = {
                                        editingListing = listing
                                        isFormOpen = true
                                    },
                                    onAddStock = {
                                        stockAction = StockAction(
                                            listing = listing,
                                            type = StockActionType.ADD
                                        )
                                    },
                                    onRemoveStock = {
                                        stockAction = StockAction(
                                            listing = listing,
                                            type = StockActionType.REMOVE
                                        )
                                    },
                                    onDelete = {
                                        viewModel.deleteListing(
                                            listingId = listing.id,
                                            onSuccess = {
                                                Toast.makeText(
                                                    context,
                                                    "Waste listing deleted.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
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
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SellerListingCard(
    listing: WasteItem,
    isSaving: Boolean,
    onEdit: () -> Unit,
    onAddStock: () -> Unit,
    onRemoveStock: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ListingImage(
                imageUrl = listing.imageUrl,
                listingId = listing.id,
                sellerId = listing.sellerId,
                modifier = Modifier
                    .size(76.dp),
                height = 76.dp
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = listing.wasteName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text("Stock: ${listing.stockKg} kg")
                Text("IDR ${listing.pricePerKg} / kg")
                Text("Status: ${listing.status.toDisplayStatus()}")

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onAddStock,
                        enabled = !isSaving
                    ) {
                        Text("+ Add")
                    }

                    OutlinedButton(
                        onClick = onRemoveStock,
                        enabled = !isSaving
                    ) {
                        Text("- Remove")
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onEdit,
                        enabled = !isSaving
                    ) {
                        Text("Edit")
                    }

                    TextButton(
                        onClick = onDelete,
                        enabled = !isSaving
                    ) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListingFormDialog(
    initialListing: WasteItem?,
    isSaving: Boolean,
    uploadProgress: Float?,
    externalErrorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (ListingForm) -> Unit
) {
    val categories = listOf("Organic", "Inorganic")
    var wasteName by remember(initialListing) {
        mutableStateOf(initialListing?.wasteName.orEmpty())
    }
    var category by remember(initialListing) {
        mutableStateOf(initialListing?.category?.toDisplayCategory() ?: "Organic")
    }
    var stock by remember(initialListing) {
        mutableStateOf(initialListing?.stockKg?.takeIf { it > 0.0 }?.toString().orEmpty())
    }
    var pricePerKg by remember(initialListing) {
        mutableStateOf(initialListing?.pricePerKg?.takeIf { it > 0 }?.toString().orEmpty())
    }
    var selectedImage by remember(initialListing) {
        mutableStateOf<SelectedImage?>(null)
    }
    var imageUrl by remember(initialListing) {
        mutableStateOf("")
    }
    var expanded by remember {
        mutableStateOf(false)
    }
    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }
    val context = LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.readBytes()
                } ?: throw IllegalArgumentException("Unable to read the selected image.")

                SelectedImage(
                    sourceUri = uri.toString(),
                    mimeType = mimeType,
                    bytes = bytes
                )
            }.onSuccess { image ->
                Log.d(
                    "IMAGE_DEBUG",
                    "Selected product URI=${image.sourceUri}, mimeType=${image.mimeType}, bytes=${image.bytes.size}"
                )
                selectedImage = image
                imageUrl = ""
                errorMessage = null
            }.onFailure { error ->
                Log.e("IMAGE_DEBUG", "Selected product image could not be read uri=$uri", error)
                errorMessage = error.localizedMessage ?: "Unable to read the selected image."
            }
        }
    }
    val previewImage = imageUrl.takeIf { it.isNotBlank() }
        ?: selectedImage?.sourceUri
        ?: initialListing?.imageUrl.orEmpty()

    AlertDialog(
        onDismissRequest = {
            if (!isSaving) {
                onDismiss()
            }
        },
        title = {
            Text(
                text = if (initialListing == null) {
                    "Create New Listing"
                } else {
                    "Edit Listing"
                }
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = wasteName,
                    onValueChange = {
                        wasteName = it
                        errorMessage = null
                    },
                    label = {
                        Text("Waste Name")
                    },
                    singleLine = true
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = {
                        expanded = !expanded
                    }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = {
                            Text("Category")
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = {
                            expanded = false
                        }
                    ) {
                        categories.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(option)
                                },
                                onClick = {
                                    category = option
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = stock,
                    onValueChange = { input ->
                        if (initialListing == null && input.all { it.isDigit() || it == '.' }) {
                            stock = input
                            errorMessage = null
                        }
                    },
                    label = {
                        Text(
                            if (initialListing == null) {
                                "Initial Stock (kg)"
                            } else {
                                "Current Stock (kg)"
                            }
                        )
                    },
                    readOnly = initialListing != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = pricePerKg,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) {
                            pricePerKg = input
                            errorMessage = null
                        }
                    },
                    label = {
                        Text("Price Per Kg")
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (previewImage.isNotBlank()) {
                            ListingImage(
                                imageUrl = previewImage,
                                listingId = initialListing?.id ?: "new-listing-preview",
                                sellerId = initialListing?.sellerId.orEmpty(),
                                modifier = Modifier
                                    .fillMaxWidth(),
                                height = 150.dp
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                imagePicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            enabled = !isSaving
                        ) {
                            Text(
                                if (previewImage.isBlank()) {
                                    "Choose Image From Gallery"
                                } else {
                                    "Change Image"
                                }
                            )
                        }

                        Text(
                            text = "OR",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = imageUrl,
                            onValueChange = { input ->
                                imageUrl = input
                                selectedImage = null
                                errorMessage = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text("Paste Image URL")
                            },
                            singleLine = true
                        )

                        Text(
                            text = "Use a gallery image or paste an image URL.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                uploadProgress?.let { progress ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Uploading image ${(progress.coerceIn(0f, 1f) * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                (errorMessage ?: externalErrorMessage)?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val stockValue = stock.toDoubleOrNull() ?: 0.0
                    val priceValue = pricePerKg.toIntOrNull() ?: 0
                    val remoteImage = imageUrl.trim()
                    val finalImage = when {
                        remoteImage.isNotBlank() && remoteImage.isValidImageUrl() -> {
                            SelectedImage(
                                sourceUri = remoteImage,
                                mimeType = remoteImage.inferImageMimeType(),
                                bytes = byteArrayOf()
                            )
                        }
                        remoteImage.isNotBlank() -> {
                            errorMessage = "Please enter a valid image URL ending in jpg, jpeg, png, webp, or gif."
                            return@Button
                        }
                        else -> selectedImage
                    }

                    when {
                        wasteName.isBlank() -> errorMessage = "Waste name is required."
                        initialListing == null && stockValue <= 0.0 -> {
                            errorMessage = "Initial stock must be greater than 0 kg."
                        }
                        priceValue <= 0 -> errorMessage = "Price per kg must be greater than 0."
                        else -> onSave(
                            ListingForm(
                                wasteName = wasteName.trim(),
                                category = category,
                                stockKg = stockValue,
                                pricePerKg = priceValue,
                                selectedImage = finalImage,
                                existingImageUrl = initialListing?.imageUrl.orEmpty()
                            )
                        )
                    }
                },
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(if (externalErrorMessage == null) "Save" else "Retry Upload")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text("Cancel")
            }
        }
    )
}

private data class ListingForm(
    val wasteName: String,
    val category: String,
    val stockKg: Double,
    val pricePerKg: Int,
    val selectedImage: SelectedImage?,
    val existingImageUrl: String
)

private enum class StockActionType {
    ADD,
    REMOVE
}

private data class StockAction(
    val listing: WasteItem,
    val type: StockActionType
)

@Composable
private fun StockActionDialog(
    action: StockAction,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var quantity by remember(action) {
        mutableStateOf("")
    }
    var errorMessage by remember(action) {
        mutableStateOf<String?>(null)
    }

    val isAdd = action.type == StockActionType.ADD

    AlertDialog(
        onDismissRequest = {
            if (!isSaving) {
                onDismiss()
            }
        },
        title = {
            Text(if (isAdd) "Add Stock" else "Remove Stock")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isAdd) {
                        "How many kilograms would you like to add?"
                    } else {
                        "How many kilograms would you like to remove?"
                    }
                )

                Text("Current Stock: ${action.listing.stockKg} kg")

                OutlinedTextField(
                    value = quantity,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() || it == '.' }) {
                            quantity = input
                            errorMessage = null
                        }
                    },
                    label = {
                        Text("Quantity (kg)")
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val quantityValue = quantity.toDoubleOrNull() ?: 0.0
                    when {
                        quantityValue <= 0.0 -> {
                            errorMessage = "Please enter a stock quantity greater than 0 kg."
                        }

                        !isAdd && quantityValue > action.listing.stockKg -> {
                            errorMessage = "Insufficient stock available."
                        }

                        else -> onConfirm(quantityValue)
                    }
                },
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Confirm")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text("Cancel")
            }
        }
    )
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
        ListingStatus.ACTIVE.name -> "Active"
        ListingStatus.SOLD_OUT.name -> "Sold Out"
        ListingStatus.PENDING.name -> "Pending"
        else -> this
    }
}

private fun String.isValidImageUrl(): Boolean {
    return runCatching {
        val uri = URI(trim())
        val scheme = uri.scheme?.lowercase(Locale.US)
        val host = uri.host.orEmpty()
        val path = uri.path.orEmpty().lowercase(Locale.US)

        (scheme == "http" || scheme == "https") &&
            host.isNotBlank() &&
            (
                path.endsWith(".jpg") ||
                    path.endsWith(".jpeg") ||
                    path.endsWith(".png") ||
                    path.endsWith(".webp") ||
                    path.endsWith(".gif") ||
                    host.contains("images.unsplash.com", ignoreCase = true)
                )
    }.getOrDefault(false)
}

private fun String.inferImageMimeType(): String {
    val normalized = lowercase(Locale.US).substringBefore("?")
    return when {
        normalized.endsWith(".png") -> "image/png"
        normalized.endsWith(".webp") -> "image/webp"
        normalized.endsWith(".gif") -> "image/gif"
        else -> "image/jpeg"
    }
}
