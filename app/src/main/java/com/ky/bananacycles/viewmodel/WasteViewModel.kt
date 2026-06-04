package com.ky.bananacycles.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.ky.bananacycles.model.WasteItem
import com.ky.bananacycles.repository.WasteRepository

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
    val errorMessage: String? = null
)

class WasteViewModel(
    private val repository: WasteRepository = WasteRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    var uiState by mutableStateOf(WasteUiState())
        private set

    private var marketListener: ListenerRegistration? = null
    private var myListingsListener: ListenerRegistration? = null

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
        category: String,
        stockKg: Double,
        pricePerKg: Int,
        imageUrl: String,
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
            errorMessage = null
        )

        repository.addListing(
            sellerId = currentUserId,
            sellerName = currentUser.displayName
                ?: currentUser.email
                ?: "BananaCycles Seller",
            wasteName = wasteName,
            category = category,
            stockKg = stockKg,
            pricePerKg = pricePerKg,
            imageUrl = imageUrl,
            onSuccess = {
                uiState = uiState.copy(isSaving = false)
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
        category: String,
        pricePerKg: Int,
        imageUrl: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        uiState = uiState.copy(
            isSaving = true,
            errorMessage = null
        )

        repository.updateListing(
            listingId = listingId,
            wasteName = wasteName,
            category = category,
            pricePerKg = pricePerKg,
            imageUrl = imageUrl,
            onSuccess = {
                uiState = uiState.copy(isSaving = false)
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
        val message = error.localizedMessage ?: fallbackMessage

        uiState = if (isPurchaseFailure) {
            uiState.copy(
                isPurchasing = false,
                errorMessage = message
            )
        } else {
            uiState.copy(
                isSaving = false,
                errorMessage = message
            )
        }

        onFailure(message)
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
        super.onCleared()
    }
}
