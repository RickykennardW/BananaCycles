package com.ky.bananacycles.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ky.bananacycles.component.ListingImage
import com.ky.bananacycles.model.SelectedImage
import com.ky.bananacycles.model.WasteScanResult
import com.ky.bananacycles.viewmodel.WasteViewModel
import java.io.ByteArrayOutputStream
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIWasteScanScreen(
    viewModel: WasteViewModel,
    onBack: () -> Unit,
    onUseResult: () -> Unit
) {
    val context = LocalContext.current
    val uiState = viewModel.uiState
    var previewUri by remember {
        mutableStateOf<String?>(null)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            uri.toSelectedImage(context)
                .onSuccess { image ->
                    previewUri = image.sourceUri
                    viewModel.scanWasteImageForSell(image)
                }
                .onFailure { error ->
                    Toast.makeText(
                        context,
                        error.localizedMessage ?: "AI scan failed. Please try again or fill the form manually.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            bitmap.toSelectedImage(context)
                .onSuccess { image ->
                    previewUri = image.sourceUri
                    viewModel.scanWasteImageForSell(image)
                }
                .onFailure { error ->
                    Toast.makeText(
                        context,
                        error.localizedMessage ?: "AI scan failed. Please try again or fill the form manually.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraLauncher.launch(null)
        } else {
            Toast.makeText(
                context,
                "Camera permission is required to scan waste.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("AI Waste Scanner")
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.clearAiScanPreview()
                            onBack()
                        }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Scan waste item or upload image to auto-fill your sell form.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            cameraLauncher.launch(null)
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Open Camera")
                }

                OutlinedButton(
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Upload From Gallery")
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (previewUri.isNullOrBlank()) {
                        Text(
                            text = "Image preview will appear here.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        ListingImage(
                            imageUrl = previewUri.orEmpty(),
                            listingId = "ai-scan-preview",
                            modifier = Modifier.fillMaxWidth(),
                            height = 196.dp
                        )
                    }
                }
            }

            when {
                uiState.isScanningWaste -> {
                    ScanningCard()
                }

                uiState.aiScanPreviewResult != null -> {
                    ScanResultCard(
                        result = uiState.aiScanPreviewResult,
                        onRetakePhoto = {
                            if (ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                viewModel.clearAiScanPreview()
                                previewUri = null
                                cameraLauncher.launch(null)
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        onUseResult = { correctedResult ->
                            viewModel.acceptAiScanResult(correctedResult)
                            onUseResult()
                        }
                    )
                }

                uiState.aiScanErrorMessage != null -> {
                    Text(
                        text = uiState.aiScanErrorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun ScanningCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
                Text(
                    text = "Scanning waste item...",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScanResultCard(
    result: WasteScanResult,
    onRetakePhoto: () -> Unit,
    onUseResult: (WasteScanResult) -> Unit
) {
    var materialType by remember {
        mutableStateOf(result.materialType.ifBlank { result.wasteName })
    }
    var category by remember {
        mutableStateOf(result.category)
    }
    var wasteName by remember {
        mutableStateOf(result.wasteName.ifBlank { result.materialType })
    }
    var suggestedPricePerKg by remember {
        mutableStateOf(result.suggestedPricePerKg)
    }
    var priceExplanation by remember {
        mutableStateOf(result.priceExplanation)
    }
    var materialQuality by remember {
        mutableStateOf(result.materialQuality)
    }

    LaunchedEffect(result) {
        val normalized = result.normalizedForMaterial()
        materialType = normalized.materialType
        category = normalized.category
        wasteName = normalized.wasteName
        suggestedPricePerKg = normalized.suggestedPricePerKg
        priceExplanation = normalized.priceExplanation
        materialQuality = normalized.materialQuality
    }

    val correctedResult = result.copy(
        wasteName = wasteName.ifBlank { materialType },
        materialType = materialType,
        category = category.toOrganicOrInorganic(),
        suggestedPricePerKg = suggestedPricePerKg,
        priceExplanation = priceExplanation,
        materialQuality = materialQuality
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Scan Result Review",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = materialType.ifBlank { "Unknown Material" },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text("Confidence: ${result.confidencePercent}%")

            if (!result.isConfident) {
                Text(
                    text = "AI is not confident. Please verify manually.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            OutlinedTextField(
                value = wasteName,
                onValueChange = {
                    wasteName = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Waste Name")
                },
                singleLine = true
            )

            Text(
                text = "Detected Material",
                style = MaterialTheme.typography.labelLarge
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                focusedMaterials.forEach { material ->
                    FilterChip(
                        selected = materialType == material,
                        onClick = {
                            val normalized = result.copy(materialType = material).normalizedForMaterial()
                            materialType = normalized.materialType
                            category = normalized.category
                            wasteName = normalized.wasteName
                            suggestedPricePerKg = normalized.suggestedPricePerKg
                            priceExplanation = normalized.priceExplanation
                            materialQuality = normalized.materialQuality
                        },
                        label = {
                            Text(material)
                        }
                    )
                }
            }

            Text(
                text = "Category",
                style = MaterialTheme.typography.labelLarge
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Organic", "Inorganic").forEach { option ->
                    FilterChip(
                        selected = category == option,
                        onClick = {
                            category = option
                        },
                        label = {
                            Text(option)
                        }
                    )
                }
            }

            Text("Estimated Quality: ${materialQuality.ifBlank { "Unknown" }}")
            Text("Suggested Price: ${suggestedPricePerKg.ifBlank { "No price suggestion" }}")
            Text(
                text = priceExplanation.ifBlank {
                    "Price is a recommendation only. Seller can still set their own price."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedButton(
                onClick = onRetakePhoto,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Retake Photo")
            }

            Button(
                onClick = {
                    onUseResult(correctedResult)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Use This Result")
            }
        }
    }
}

private val focusedMaterials = listOf(
    "Plastic Bottle",
    "Glass Bottle",
    "Aluminum Can",
    "Cardboard",
    "Paper",
    "Food Waste",
    "Leaves"
)

private fun WasteScanResult.normalizedForMaterial(): WasteScanResult {
    val material = materialType.ifBlank { wasteName }
    val normalizedMaterial = focusedMaterials.firstOrNull { option ->
        option.equals(material, ignoreCase = true)
    } ?: material

    val category = normalizedMaterial.toOrganicOrInorganic()
    val priceInfo = normalizedMaterial.priceSuggestion()

    return copy(
        wasteName = normalizedMaterial,
        materialType = normalizedMaterial,
        category = category,
        materialQuality = priceInfo.quality,
        suggestedPricePerKg = priceInfo.range,
        priceExplanation = priceInfo.explanation
    )
}

private fun String.toOrganicOrInorganic(): String {
    return when (this) {
        "Food Waste", "Leaves", "Organic" -> "Organic"
        else -> "Inorganic"
    }
}

private data class PriceSuggestion(
    val quality: String,
    val range: String,
    val explanation: String
)

private fun String.priceSuggestion(): PriceSuggestion {
    return when (this) {
        "Plastic Bottle" -> PriceSuggestion(
            "Good",
            "Rp 2.500 - Rp 4.000/kg",
            "Clean PET bottles usually have stable recycling demand."
        )
        "Glass Bottle" -> PriceSuggestion(
            "Good",
            "Rp 500 - Rp 1.500/kg",
            "Glass has lower resale value but remains recyclable when sorted."
        )
        "Aluminum Can" -> PriceSuggestion(
            "High",
            "Rp 10.000 - Rp 20.000/kg",
            "Aluminum has higher scrap value than most household recyclables."
        )
        "Cardboard" -> PriceSuggestion(
            "Good",
            "Rp 1.500 - Rp 3.000/kg",
            "Dry cardboard is easy to sort and commonly accepted by recyclers."
        )
        "Paper" -> PriceSuggestion(
            "Medium",
            "Rp 1.000 - Rp 2.500/kg",
            "Paper price depends heavily on dryness and contamination level."
        )
        "Food Waste" -> PriceSuggestion(
            "Medium",
            "Rp 0 - Rp 1.000/kg",
            "Food waste has low direct resale value but can become compost."
        )
        "Leaves" -> PriceSuggestion(
            "Medium",
            "Rp 0 - Rp 1.000/kg",
            "Leaves are useful for composting but usually have low marketplace price."
        )
        else -> PriceSuggestion(
            "Unknown",
            "No price suggestion",
            "The material is outside the focused recyclable material set."
        )
    }
}

private fun Uri.toSelectedImage(context: Context): Result<SelectedImage> {
    return runCatching {
        val mimeType = context.contentResolver.getType(this) ?: "image/jpeg"
        val bytes = context.contentResolver.openInputStream(this)?.use { input ->
            input.readBytes()
        } ?: throw IllegalArgumentException("Unable to read the selected image.")

        SelectedImage(
            sourceUri = toString(),
            mimeType = mimeType,
            bytes = bytes
        )
    }
}

private fun Bitmap.toSelectedImage(context: Context): Result<SelectedImage> {
    return runCatching {
        val output = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 86, output)
        val bytes = output.toByteArray()
        val file = File(context.cacheDir, "ai_waste_scan_${System.currentTimeMillis()}.jpg")
        file.writeBytes(bytes)

        SelectedImage(
            sourceUri = Uri.fromFile(file).toString(),
            mimeType = "image/jpeg",
            bytes = bytes
        )
    }
}
