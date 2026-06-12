package com.ky.bananacycles.viewmodel

import android.util.Log
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.StorageException
import com.ky.bananacycles.model.SelectedImage
import com.ky.bananacycles.model.WasteItem
import com.ky.bananacycles.model.WastePrediction
import com.ky.bananacycles.model.WasteScanResult
import com.ky.bananacycles.repository.WasteDetectionRepository
import com.ky.bananacycles.repository.WasteRepository
import java.util.concurrent.Executors

private const val IMAGE_DEBUG_TAG = "IMAGE_DEBUG"

data class WasteUiState(
    val marketListings: List<WasteItem> = emptyList(),
    val myListings: List<WasteItem> = emptyList(),
    val isMarketLoading: Boolean = false,
    val isMyListingsLoading: Boolean = false,
    val isMarketRefreshing: Boolean = false,
    val isMyListingsRefreshing: Boolean = false,
    val isSaving: Boolean = false,
    val isPurchasing: Boolean = false,
    val isAnalyzingWaste: Boolean = false,
    val wastePrediction: WastePrediction? = null,
    val wasteDetectionMessage: String? = null,
    val isScanningWaste: Boolean = false,
    val aiScanPreviewResult: WasteScanResult? = null,
    val aiScanResult: WasteScanResult? = null,
    val aiScanErrorMessage: String? = null,
    val imageUploadProgress: Float? = null,
    val errorMessage: String? = null
)

class WasteViewModel(
    private val repository: WasteRepository = WasteRepository(),
    private val detectionRepository: WasteDetectionRepository = WasteDetectionRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    var uiState by mutableStateOf(WasteUiState())
        private set

    private var marketListener: ListenerRegistration? = null
    private var myListingsListener: ListenerRegistration? = null
    private val detectionExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var detectionRequestId = 0

    fun loadMarketListings(
        forceRefresh: Boolean = false
    ) {
        if (forceRefresh && uiState.isMarketRefreshing) {
            return
        }

        val currentUserId = auth.currentUser?.uid

        if (currentUserId.isNullOrBlank()) {
            uiState = uiState.copy(
                marketListings = emptyList(),
                isMarketLoading = false,
                isMarketRefreshing = false,
                errorMessage = "User is not signed in."
            )
            return
        }

        marketListener?.remove()
        uiState = uiState.copy(
            isMarketLoading = !forceRefresh,
            isMarketRefreshing = forceRefresh,
            errorMessage = null
        )

        marketListener = repository.listenMarketListings(
            currentUserId = currentUserId,
            onDataChanged = { listings ->
                listings.forEach { listing ->
                    Log.d(
                        IMAGE_DEBUG_TAG,
                        "ViewModel market listingId=${listing.id}, sellerId=${listing.sellerId}, imageUrl=${listing.imageUrl}"
                    )
                }
                uiState = uiState.copy(
                    marketListings = listings,
                    isMarketLoading = false,
                    isMarketRefreshing = false,
                    errorMessage = null
                )
            },
            onError = { error ->
                uiState = uiState.copy(
                    isMarketLoading = false,
                    isMarketRefreshing = false,
                    errorMessage = error.localizedMessage
                        ?: "Failed to load the marketplace."
                )
            }
        )
    }

    fun loadMyListings(
        forceRefresh: Boolean = false
    ) {
        if (forceRefresh && uiState.isMyListingsRefreshing) {
            return
        }

        val currentUserId = auth.currentUser?.uid

        if (currentUserId.isNullOrBlank()) {
            uiState = uiState.copy(
                myListings = emptyList(),
                isMyListingsLoading = false,
                isMyListingsRefreshing = false,
                errorMessage = "User is not signed in."
            )
            return
        }

        myListingsListener?.remove()
        uiState = uiState.copy(
            isMyListingsLoading = !forceRefresh,
            isMyListingsRefreshing = forceRefresh,
            errorMessage = null
        )

        myListingsListener = repository.listenUserListings(
            currentUserId = currentUserId,
            onDataChanged = { listings ->
                listings.forEach { listing ->
                    Log.d(
                        IMAGE_DEBUG_TAG,
                        "ViewModel seller listingId=${listing.id}, sellerId=${listing.sellerId}, imageUrl=${listing.imageUrl}"
                    )
                }
                uiState = uiState.copy(
                    myListings = listings,
                    isMyListingsLoading = false,
                    isMyListingsRefreshing = false,
                    errorMessage = null
                )
            },
            onError = { error ->
                uiState = uiState.copy(
                    isMyListingsLoading = false,
                    isMyListingsRefreshing = false,
                    errorMessage = error.localizedMessage
                        ?: "Failed to load your waste listings."
                )
            }
        )
    }

    fun addListing(
        wasteName: String,
        description: String,
        category: String,
        stockKg: Double,
        pricePerKg: Int,
        selectedImage: SelectedImage?,
        existingImageUrl: String = "",
        wastePrediction: WastePrediction? = null,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val currentUser = auth.currentUser
        val currentUserId = currentUser?.uid

        if (currentUserId.isNullOrBlank()) {
            onFailure("User is not signed in.")
            return
        }

        uiState = uiState.copy(
            isSaving = true,
            imageUploadProgress = null,
            errorMessage = null
        )

        repository.addListing(
            sellerId = currentUserId,
        sellerName = currentUser.displayName
                ?: currentUser.email
                ?: "BananaCycles Seller",
            sellerPhotoUrl = currentUser.photoUrl?.toString().orEmpty(),
            wasteName = wasteName,
            description = description,
            category = category,
            stockKg = stockKg,
            pricePerKg = pricePerKg,
            selectedImage = selectedImage,
            existingImageUrl = existingImageUrl,
            wastePrediction = wastePrediction,
            onProgress = { progress ->
                uiState = uiState.copy(imageUploadProgress = progress)
            },
            onSuccess = {
                uiState = uiState.copy(
                    isSaving = false,
                    imageUploadProgress = null
                )
                onSuccess()
            },
            onFailure = { error ->
                handleFailure(
                    fallbackMessage = "Failed to create the waste listing.",
                    error = error,
                    onFailure = onFailure
                )
            }
        )
    }

    fun updateListing(
        listingId: String,
        wasteName: String,
        description: String,
        category: String,
        pricePerKg: Int,
        sellerId: String,
        selectedImage: SelectedImage?,
        existingImageUrl: String,
        wastePrediction: WastePrediction? = null,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        uiState = uiState.copy(
            isSaving = true,
            imageUploadProgress = null,
            errorMessage = null
        )

        repository.updateListing(
            listingId = listingId,
            wasteName = wasteName,
            description = description,
            category = category,
            pricePerKg = pricePerKg,
            sellerId = sellerId,
            selectedImage = selectedImage,
            existingImageUrl = existingImageUrl,
            wastePrediction = wastePrediction,
            onProgress = { progress ->
                uiState = uiState.copy(imageUploadProgress = progress)
            },
            onSuccess = {
                uiState = uiState.copy(
                    isSaving = false,
                    imageUploadProgress = null
                )
                onSuccess()
            },
            onFailure = { error ->
                handleFailure(
                    fallbackMessage = "Failed to update the waste listing.",
                    error = error,
                    onFailure = onFailure
                )
            }
        )
    }

    fun analyzeWasteImage(selectedImage: SelectedImage) {
        val requestId = ++detectionRequestId
        uiState = uiState.copy(
            isAnalyzingWaste = true,
            wastePrediction = null,
            wasteDetectionMessage = null,
            errorMessage = null
        )

        // Detection runs away from the Compose thread so image compression and
        // future ML inference engines cannot block typing, scrolling, or preview rendering.
        detectionExecutor.execute {
            runCatching {
                detectionRepository.detectWaste(selectedImage)
            }.onSuccess { prediction ->
                if (requestId != detectionRequestId) {
                    return@onSuccess
                }

                val message = if (prediction.isConfident) {
                    null
                } else {
                    "We are not confident. Please classify manually."
                }

                mainHandler.post {
                    if (requestId == detectionRequestId) {
                        uiState = uiState.copy(
                            isAnalyzingWaste = false,
                            wastePrediction = prediction,
                            wasteDetectionMessage = message
                        )
                    }
                }
            }.onFailure { error ->
                if (requestId != detectionRequestId) {
                    return@onFailure
                }

                mainHandler.post {
                    if (requestId == detectionRequestId) {
                        uiState = uiState.copy(
                            isAnalyzingWaste = false,
                            wastePrediction = null,
                            wasteDetectionMessage = error.localizedMessage
                                ?: "AI waste analysis failed. Please classify manually."
                        )
                    }
                }
            }
        }
    }

    fun clearWasteDetection() {
        detectionRequestId += 1
        uiState = uiState.copy(
            isAnalyzingWaste = false,
            wastePrediction = null,
            wasteDetectionMessage = null
        )
    }

    fun scanWasteImageForSell(selectedImage: SelectedImage) {
        val requestId = ++detectionRequestId
        uiState = uiState.copy(
            isScanningWaste = true,
            aiScanPreviewResult = null,
            aiScanErrorMessage = null
        )

        // The scan screen uses the same repository boundary as the form analyzer.
        // This keeps fake/demo AI replaceable with TFLite, Firebase ML, or Gemini Vision later.
        detectionExecutor.execute {
            runCatching {
                detectionRepository.scanWasteForSell(selectedImage)
            }.onSuccess { result ->
                mainHandler.post {
                    if (requestId == detectionRequestId) {
                        uiState = uiState.copy(
                            isScanningWaste = false,
                            aiScanPreviewResult = result,
                            aiScanErrorMessage = null
                        )
                    }
                }
            }.onFailure { error ->
                mainHandler.post {
                    if (requestId == detectionRequestId) {
                        uiState = uiState.copy(
                            isScanningWaste = false,
                            aiScanPreviewResult = null,
                            aiScanErrorMessage = error.localizedMessage
                                ?: "AI scan failed. Please try again or fill the form manually."
                        )
                    }
                }
            }
        }
    }

    fun acceptAiScanResult(correctedResult: WasteScanResult? = null) {
        val result = correctedResult ?: uiState.aiScanPreviewResult ?: return
        uiState = uiState.copy(
            aiScanResult = result,
            aiScanErrorMessage = if (result.isConfident) {
                null
            } else {
                "AI is not confident. Please verify manually."
            }
        )
    }

    fun clearAiScanPreview() {
        detectionRequestId += 1
        uiState = uiState.copy(
            isScanningWaste = false,
            aiScanPreviewResult = null,
            aiScanErrorMessage = null
        )
    }

    fun clearAiScanResult() {
        uiState = uiState.copy(
            aiScanResult = null,
            aiScanErrorMessage = null
        )
    }

    fun addStock(
        listingId: String,
        quantityKg: Double,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (quantityKg <= 0.0) {
            onFailure("Please enter a stock quantity greater than 0 kg.")
            return
        }

        updateStockByDelta(
            listingId = listingId,
            stockDeltaKg = quantityKg,
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }

    fun removeStock(
        listing: WasteItem,
        quantityKg: Double,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (quantityKg <= 0.0) {
            onFailure("Please enter a stock quantity greater than 0 kg.")
            return
        }

        if (quantityKg > listing.stockKg) {
            onFailure("Insufficient stock available.")
            return
        }

        updateStockByDelta(
            listingId = listing.id,
            stockDeltaKg = -quantityKg,
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }

    fun deleteListing(
        listingId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        uiState = uiState.copy(
            isSaving = true,
            errorMessage = null
        )

        repository.deleteListing(
            listingId = listingId,
            onSuccess = {
                uiState = uiState.copy(isSaving = false)
                onSuccess()
            },
            onFailure = { error ->
                handleFailure(
                    fallbackMessage = "Failed to delete the waste listing.",
                    error = error,
                    onFailure = onFailure
                )
            }
        )
    }

    fun purchaseListing(
        listing: WasteItem,
        quantityKg: Double,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (quantityKg <= 0.0) {
            onFailure("Please enter a purchase quantity greater than 0 kg.")
            return
        }

        if (quantityKg > listing.stockKg) {
            onFailure("Requested quantity exceeds available stock.")
            return
        }

        uiState = uiState.copy(
            isPurchasing = true,
            errorMessage = null
        )

        val currentUser = auth.currentUser
        val buyerId = currentUser?.uid.orEmpty()

        if (buyerId.isBlank()) {
            onFailure("User is not signed in.")
            uiState = uiState.copy(isPurchasing = false)
            return
        }

        repository.purchaseListing(
            listing = listing,
            buyerId = buyerId,
            buyerName = currentUser?.displayName
                ?: currentUser?.email
                ?: "Buyer",
            quantityKg = quantityKg,
            onSuccess = {
                uiState = uiState.copy(isPurchasing = false)
                onSuccess()
            },
            onFailure = { error ->
                handleFailure(
                    fallbackMessage = "Failed to complete the purchase.",
                    error = error,
                    onFailure = onFailure,
                    isPurchaseFailure = true
                )
            }
        )
    }

    fun clearListings() {
        marketListener?.remove()
        myListingsListener?.remove()
        marketListener = null
        myListingsListener = null
        uiState = WasteUiState()
    }

    private fun handleFailure(
        fallbackMessage: String,
        error: Exception,
        onFailure: (String) -> Unit,
        isPurchaseFailure: Boolean = false
    ) {
        val message = error.toUserFacingMessage(fallbackMessage)

        uiState = if (isPurchaseFailure) {
            uiState.copy(
                isPurchasing = false,
                errorMessage = message
            )
        } else {
            uiState.copy(
                isSaving = false,
                imageUploadProgress = null,
                errorMessage = message
            )
        }

        onFailure(message)
    }

    private fun Exception.toUserFacingMessage(fallbackMessage: String): String {
        return if (this is StorageException) {
            val detail = "Firebase Storage errorCode=$errorCode, message=${localizedMessage ?: fallbackMessage}"
            when (errorCode) {
                StorageException.ERROR_OBJECT_NOT_FOUND -> {
                    "Image upload failed: uploaded object was not found. $detail"
                }
                StorageException.ERROR_NOT_AUTHENTICATED -> {
                    "Image upload failed: please log in again. $detail"
                }
                StorageException.ERROR_NOT_AUTHORIZED -> {
                    "Image upload failed: blocked by Firebase Storage rules. $detail"
                }
                StorageException.ERROR_RETRY_LIMIT_EXCEEDED -> {
                    "Image upload failed: network retry limit exceeded. $detail"
                }
                else -> detail
            }
        } else {
            localizedMessage ?: fallbackMessage
        }
    }

    private fun updateStockByDelta(
        listingId: String,
        stockDeltaKg: Double,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        uiState = uiState.copy(
            isSaving = true,
            errorMessage = null
        )

        repository.adjustStock(
            listingId = listingId,
            stockDeltaKg = stockDeltaKg,
            onSuccess = {
                uiState = uiState.copy(isSaving = false)
                onSuccess()
            },
            onFailure = { error ->
                handleFailure(
                    fallbackMessage = "Failed to update stock.",
                    error = error,
                    onFailure = onFailure
                )
            }
        )
    }

    override fun onCleared() {
        marketListener?.remove()
        myListingsListener?.remove()
        detectionExecutor.shutdownNow()
        super.onCleared()
    }
}
