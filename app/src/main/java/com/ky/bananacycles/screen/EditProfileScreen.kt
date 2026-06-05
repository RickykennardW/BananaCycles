package com.ky.bananacycles.screen

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ky.bananacycles.component.UserAvatar
import com.ky.bananacycles.model.SelectedImage
import com.ky.bananacycles.viewmodel.ProfileViewModel
import java.net.URI
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState = viewModel.uiState
    var displayName by remember {
        mutableStateOf("")
    }
    var selectedImage by remember {
        mutableStateOf<SelectedImage?>(null)
    }
    var imageUrl by remember {
        mutableStateOf("")
    }
    var imageInputError by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(uiState.profile.displayName) {
        if (displayName.isBlank()) {
            displayName = uiState.profile.displayName
        }
    }

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
                    "Selected profile URI=${image.sourceUri}, mimeType=${image.mimeType}, bytes=${image.bytes.size}"
                )
                selectedImage = image
                imageUrl = ""
                imageInputError = null
            }.onFailure { error ->
                Log.e("IMAGE_DEBUG", "Selected profile image could not be read uri=$uri", error)
                Toast.makeText(
                    context,
                    error.localizedMessage ?: "Unable to read the selected image.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Edit Profile")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                    .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    UserAvatar(
                        photoUrl = imageUrl.takeIf { it.isNotBlank() }
                            ?: selectedImage?.sourceUri
                            ?: uiState.profile.photoUrl,
                        size = 104.dp
                    )

                    Text(
                        text = "Choose From Gallery",
                        style = MaterialTheme.typography.labelLarge
                    )

                    OutlinedButton(
                        onClick = {
                            imagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Text("Change Profile Picture")
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
                            imageInputError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Paste Image URL")
                        },
                        singleLine = true,
                        isError = imageInputError != null
                    )

                    imageInputError?.let { message ->
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    uiState.imageUploadProgress?.let { progress ->
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
            }

            OutlinedTextField(
                value = displayName,
                onValueChange = {
                    displayName = it
                },
                label = {
                    Text("Display Name")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            uiState.errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
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
                            imageInputError = "Please enter a valid image URL ending in jpg, jpeg, png, webp, or gif."
                            return@Button
                        }
                        else -> selectedImage
                    }

                    viewModel.updateProfile(
                        displayName = displayName,
                        selectedImage = finalImage,
                        onSuccess = {
                            Toast.makeText(context, "Profile updated.", Toast.LENGTH_SHORT).show()
                            onBack()
                        },
                        onFailure = { message ->
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                enabled = !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(if (uiState.errorMessage == null) "Save Changes" else "Retry Upload")
                }
            }
        }
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
