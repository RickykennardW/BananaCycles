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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.sp
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
    var reviewedResult by remember {
        mutableStateOf<WasteScanResult?>(null)
    }

    LaunchedEffect(uiState.aiScanPreviewResult) {
        reviewedResult = uiState.aiScanPreviewResult?.normalizedForMaterial()
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            uri.toSelectedImage(context)
                .onSuccess { image ->
                    previewUri = image.sourceUri
                    reviewedResult = null
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
                    reviewedResult = null
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
        },
        bottomBar = {
            val resultToUse = reviewedResult
            AnimatedVisibility(visible = resultToUse != null) {
                Surface(
                    tonalElevation = 3.dp,
                    shadowElevation = 6.dp
                ) {
                    Button(
                        onClick = {
                            val finalResult = resultToUse ?: return@Button
                            viewModel.acceptAiScanResult(finalResult)
                            Toast.makeText(
                                context,
                                "AI successfully filled your listing.",
                                Toast.LENGTH_SHORT
                            ).show()
                            onUseResult()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Use This Result")
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .navigationBarsPadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                top = 8.dp,
                end = 16.dp,
                bottom = if (reviewedResult != null) 96.dp else 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Scan a waste item or upload an image to automatically fill your sell form.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Open Camera")
                }
            }

            item {
                OutlinedButton(
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Upload From Gallery")
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (previewUri.isNullOrBlank()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Image preview will appear here.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
                                )
                            }
                        } else {
                            ListingImage(
                                imageUrl = previewUri.orEmpty(),
                                listingId = "ai-scan-preview",
                                modifier = Modifier.fillMaxWidth(),
                                height = 160.dp
                            )
                        }
                    }
                }
            }

            item {
                AnimatedVisibility(visible = uiState.isScanningWaste) {
                    ScanningCard()
                }
            }

            item {
                AnimatedVisibility(visible = uiState.aiScanPreviewResult != null) {
                    reviewedResult?.let { result ->
                        ScanResultCard(
                            result = result,
                            onResultChange = { updated ->
                                reviewedResult = updated
                            },
                            onRetakePhoto = {
                                if (ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.CAMERA
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    viewModel.clearAiScanPreview()
                                    reviewedResult = null
                                    previewUri = null
                                    cameraLauncher.launch(null)
                                } else {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }
                        )
                    }
                }
            }

            item {
                uiState.aiScanErrorMessage?.let { message ->
                    Text(
                        text = message,
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
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp)
                )
            }
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ScanResultCard(
    result: WasteScanResult,
    onResultChange: (WasteScanResult) -> Unit,
    onRetakePhoto: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Detected",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = result.materialType.ifBlank { "Unknown Material" },
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                fontWeight = FontWeight.Bold
            )
            CompactResultRow("Confidence", "${result.confidencePercent}%")
            CompactResultRow("Category", result.category)
            CompactResultRow("Reuse", result.reuseSuggestion.ifBlank { "-" })
            CompactResultRow("Price", result.suggestedPricePerKg.ifBlank { "No suggestion" })

            if (!result.isConfident) {
                Text(
                    text = "AI is not confident. Please verify manually.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            OutlinedTextField(
                value = result.wasteName.ifBlank { result.materialType },
                onValueChange = {
                    onResultChange(
                        result.copy(
                            wasteName = it,
                            materialType = it
                        ).normalizedForMaterial()
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Waste Name")
                },
                singleLine = true
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Organic", "Inorganic").forEach { option ->
                    FilterChip(
                        selected = result.category == option,
                        onClick = {
                            onResultChange(result.copy(category = option))
                        },
                        label = {
                            Text(option)
                        }
                    )
                }
            }

            OutlinedButton(
                onClick = onRetakePhoto,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Retake Photo")
            }
        }
    }
}

@Composable
private fun CompactResultRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            fontWeight = FontWeight.Medium
        )
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
