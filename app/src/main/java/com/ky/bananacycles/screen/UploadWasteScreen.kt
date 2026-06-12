package com.ky.bananacycles.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.AssistChip
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
import com.ky.bananacycles.model.WastePrediction
import com.ky.bananacycles.model.WasteScanResult
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
    viewModel: WasteViewModel,
    onAiScanClick: () -> Unit = {}
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

    LaunchedEffect(uiState.aiScanResult) {
        if (uiState.aiScanResult != null) {
            editingListing = null
            isFormOpen = true
        }
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
            isAnalyzingWaste = uiState.isAnalyzingWaste,
            wastePrediction = uiState.wastePrediction,
            wasteDetectionMessage = uiState.wasteDetectionMessage,
            aiScanResult = uiState.aiScanResult,
            aiScanMessage = uiState.aiScanErrorMessage,
            externalErrorMessage = uiState.errorMessage,
            onClearWasteAnalysis = {
                viewModel.clearWasteDetection()
            },
            onDismiss = {
                viewModel.clearWasteDetection()
                viewModel.clearAiScanResult()
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
                    viewModel.clearAiScanResult()
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
                        description = form.description,
                        category = form.category,
                        stockKg = form.stockKg,
                        pricePerKg = form.pricePerKg,
                        selectedImage = form.selectedImage,
                        existingImageUrl = form.existingImageUrl,
                        wastePrediction = form.wastePrediction,
                        onSuccess = onSuccess,
                        onFailure = onFailure
                    )
                } else {
                    viewModel.updateListing(
                        listingId = listing.id,
                        wasteName = form.wasteName,
                        description = form.description,
                        category = form.category,
                        pricePerKg = form.pricePerKg,
                        sellerId = listing.sellerId,
                        selectedImage = form.selectedImage,
                        existingImageUrl = form.existingImageUrl,
                        wastePrediction = form.wastePrediction,
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

                AiScannerEntryCard(
                    onAiScanClick = onAiScanClick
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
private fun AiScannerEntryCard(
    onAiScanClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "AI Waste Scanner",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "Scan or upload a waste image. BananaCycles will help fill this form automatically.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Button(
                onClick = onAiScanClick,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Scan with AI")
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
    isAnalyzingWaste: Boolean,
    wastePrediction: WastePrediction?,
    wasteDetectionMessage: String?,
    aiScanResult: WasteScanResult?,
    aiScanMessage: String?,
    externalErrorMessage: String?,
    onClearWasteAnalysis: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (ListingForm) -> Unit
) {
    val categories = listOf(
        "Organic",
        "Inorganic"
    )
    var wasteName by remember(initialListing) {
        mutableStateOf(initialListing?.wasteName.orEmpty())
    }
    var description by remember(initialListing, aiScanResult) {
        mutableStateOf(
            aiScanResult?.toGeneratedDescription()
                ?: initialListing?.description.orEmpty()
        )
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
    var materialType by remember(initialListing) {
        mutableStateOf(initialListing?.materialType.orEmpty())
    }
    var cleanliness by remember(initialListing) {
        mutableStateOf(initialListing?.cleanliness.orEmpty())
    }
    var contamination by remember(initialListing) {
        mutableStateOf(initialListing?.contamination.orEmpty())
    }
    var reuseSuggestion by remember(initialListing) {
        mutableStateOf(initialListing?.reuseSuggestion.orEmpty())
    }
    var recyclability by remember(initialListing) {
        mutableStateOf(initialListing?.recyclability.orEmpty())
    }
    var acceptedPrediction by remember(initialListing) {
        mutableStateOf<WastePrediction?>(null)
    }
    var isAiCardVisible by remember(initialListing) {
        mutableStateOf(initialListing?.aiGenerated == true)
    }
    var expanded by remember {
        mutableStateOf(false)
    }
    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }
    val previewImage = imageUrl.takeIf { it.isNotBlank() }
        ?: initialListing?.imageUrl.orEmpty()

    LaunchedEffect(aiScanResult) {
        val result = aiScanResult ?: return@LaunchedEffect
        wasteName = result.wasteName.ifBlank { result.materialType }
        category = result.category.ifBlank { category }
        description = result.toGeneratedDescription()
        materialType = result.materialType
        cleanliness = result.cleanliness
        contamination = if (result.recyclability.equals("High", ignoreCase = true)) {
            "Low"
        } else {
            "Needs Review"
        }
        reuseSuggestion = result.reuseSuggestion
        recyclability = result.recyclability
        acceptedPrediction = result.toWastePrediction()
        isAiCardVisible = true
    }

    LaunchedEffect(wastePrediction) {
        val prediction = wastePrediction
        if (prediction != null && prediction.isConfident) {
            // Auto-fill is intentionally limited to local form state. The seller
            // still controls the final submission and may edit every value before saving.
            acceptedPrediction = prediction.copy(aiGenerated = true)
            isAiCardVisible = true
            category = prediction.wasteCategory
            materialType = prediction.materialType
            cleanliness = prediction.cleanliness
            contamination = prediction.contamination
            reuseSuggestion = prediction.reuseSuggestion
            recyclability = prediction.recyclability
            if (wasteName.isBlank()) {
                wasteName = prediction.materialType
            }
        }
    }

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

                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Description / Note")
                    },
                    minLines = 3
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

                        OutlinedTextField(
                            value = imageUrl,
                            onValueChange = { input ->
                                imageUrl = input
                                selectedImage = null
                                acceptedPrediction = null
                                isAiCardVisible = false
                                onClearWasteAnalysis()
                                errorMessage = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text("Paste Image URL")
                            },
                            singleLine = true
                        )

                        Text(
                            text = "Enter an image URL manually. AI scan images are not used as product images.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                AiWasteAnalysisSection(
                    isAnalyzingWaste = isAnalyzingWaste,
                    prediction = wastePrediction,
                    message = aiScanMessage ?: wasteDetectionMessage,
                    isVisible = isAiCardVisible || isAnalyzingWaste || wasteDetectionMessage != null || aiScanMessage != null,
                    onAcceptSuggestion = { prediction ->
                        acceptedPrediction = prediction.copy(aiGenerated = true)
                        isAiCardVisible = true
                        category = prediction.wasteCategory
                        materialType = prediction.materialType
                        cleanliness = prediction.cleanliness
                        contamination = prediction.contamination
                        reuseSuggestion = prediction.reuseSuggestion
                        recyclability = prediction.recyclability
                        if (wasteName.isBlank()) {
                            wasteName = prediction.materialType
                        }
                    },
                    onEditManually = {
                        acceptedPrediction = null
                        isAiCardVisible = false
                        onClearWasteAnalysis()
                    }
                )

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
                            onClearWasteAnalysis()
                            acceptedPrediction = null
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
                                description = description.trim(),
                                category = category,
                                stockKg = stockValue,
                                pricePerKg = priceValue,
                                selectedImage = finalImage,
                                existingImageUrl = initialListing?.imageUrl.orEmpty(),
                                wastePrediction = acceptedPrediction?.copy(
                                    wasteCategory = category,
                                    materialType = materialType,
                                    cleanliness = cleanliness,
                                    contamination = contamination,
                                    reuseSuggestion = reuseSuggestion,
                                    recyclability = recyclability,
                                    aiGenerated = true
                                )
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

@Composable
private fun AiWasteAnalysisSection(
    isAnalyzingWaste: Boolean,
    prediction: WastePrediction?,
    message: String?,
    isVisible: Boolean,
    onAcceptSuggestion: (WastePrediction) -> Unit,
    onEditManually: () -> Unit
) {
    if (!isVisible) {
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "AI Waste Analysis",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            if (isAnalyzingWaste) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Analyzing waste...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
                return@Column
            }

            if (prediction == null || !prediction.isConfident) {
                Text(
                    text = message ?: "We are not confident. Please classify manually.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                TextButton(
                    onClick = onEditManually
                ) {
                    Text("Edit Manually")
                }
                return@Column
            }

            Text(
                text = prediction.materialType,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            AssistChip(
                onClick = {},
                label = {
                    Text("Confidence ${prediction.confidencePercent}%")
                }
            )

            AiPredictionRow("Category", prediction.wasteCategory)
            AiPredictionRow("Condition", prediction.cleanliness)
            AiPredictionRow("Contamination", prediction.contamination)
            AiPredictionRow("Reuse", prediction.reuseSuggestion)
            AiPredictionRow("Recyclability", prediction.recyclability)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        onAcceptSuggestion(prediction)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Accept Suggestion")
                }

                OutlinedButton(
                    onClick = onEditManually,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Edit Manually")
                }
            }
        }
    }
}

@Composable
private fun AiPredictionRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value.ifBlank { "-" },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

private data class ListingForm(
    val wasteName: String,
    val description: String,
    val category: String,
    val stockKg: Double,
    val pricePerKg: Int,
    val selectedImage: SelectedImage?,
    val existingImageUrl: String,
    val wastePrediction: WastePrediction?
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
        equals("Organic", ignoreCase = true) -> "Organic"
        equals("Organik", ignoreCase = true) -> "Organic"
        else -> "Inorganic"
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

private fun WasteScanResult.toGeneratedDescription(): String {
    return listOf(
        "Material: ${materialType.ifBlank { "-" }}",
        "Condition: ${cleanliness.ifBlank { "-" }}",
        "Recyclability: ${recyclability.ifBlank { "-" }}",
        "Suggested reuse: ${reuseSuggestion.ifBlank { "-" }}",
        "Suggested price: ${suggestedPricePerKg.ifBlank { "-" }}",
        "Price note: ${priceExplanation.ifBlank { "-" }}",
        "Detected by AI with $confidencePercent% confidence."
    ).joinToString(separator = "\n")
}

private fun WasteScanResult.toWastePrediction(): WastePrediction {
    return WastePrediction(
        wasteCategory = category,
        materialType = materialType,
        cleanliness = cleanliness,
        contamination = if (recyclability.equals("High", ignoreCase = true)) "Low" else "Needs Review",
        reuseSuggestion = reuseSuggestion,
        recyclability = recyclability,
        materialQuality = materialQuality,
        suggestedPricePerKg = suggestedPricePerKg,
        priceExplanation = priceExplanation,
        confidence = confidence,
        aiGenerated = true
    )
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
