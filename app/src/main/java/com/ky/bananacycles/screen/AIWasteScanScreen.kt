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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
                        onUseResult = {
                            viewModel.acceptAiScanResult()
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

@Composable
private fun ScanResultCard(
    result: WasteScanResult,
    onUseResult: () -> Unit
) {
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
                text = "Detected:",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = result.wasteName.ifBlank { result.materialType },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text("Category: ${result.category}")
            Text("Confidence: ${result.confidencePercent}%")

            if (!result.isConfident) {
                Text(
                    text = "AI is not confident. Please check the data manually.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Button(
                onClick = onUseResult,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Use This Result")
            }
        }
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
