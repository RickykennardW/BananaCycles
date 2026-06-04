package com.ky.bananacycles.component

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest

private const val IMAGE_DEBUG_TAG = "IMAGE_DEBUG"

@Composable
fun ListingImage(
    imageUrl: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    height: Dp = 120.dp
) {
    val normalizedUrl = imageUrl.trim()
    var imageState by remember(normalizedUrl) {
        mutableStateOf(ImageLoadState.Idle)
    }

    LaunchedEffect(normalizedUrl) {
        Log.d(IMAGE_DEBUG_TAG, "Listing imageUrl: $normalizedUrl")
    }

    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (normalizedUrl.isBlank()) {
            ImagePlaceholder("No image")
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(normalizedUrl)
                    .crossfade(true)
                    .listener(
                        onError = { _, result ->
                            Log.e(
                                IMAGE_DEBUG_TAG,
                                "Coil failed to load imageUrl=$normalizedUrl",
                                result.throwable
                            )
                        },
                        onSuccess = { _, _ ->
                            Log.d(IMAGE_DEBUG_TAG, "Coil loaded imageUrl=$normalizedUrl")
                        }
                    )
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onState = { state ->
                    imageState = when (state) {
                        is AsyncImagePainter.State.Success -> ImageLoadState.Success
                        is AsyncImagePainter.State.Error -> {
                            Log.e(
                                IMAGE_DEBUG_TAG,
                                "AsyncImage error state for imageUrl=$normalizedUrl",
                                state.result.throwable
                            )
                            ImageLoadState.Error
                        }
                        is AsyncImagePainter.State.Loading -> ImageLoadState.Loading
                        else -> ImageLoadState.Idle
                    }
                }
            )

            when (imageState) {
                ImageLoadState.Error -> ImagePlaceholder("Image unavailable")
                ImageLoadState.Loading -> ImagePlaceholder("Loading image...")
                ImageLoadState.Idle,
                ImageLoadState.Success -> Unit
            }
        }
    }
}

private enum class ImageLoadState {
    Idle,
    Loading,
    Success,
    Error
}

@Composable
private fun ImagePlaceholder(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(16.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
