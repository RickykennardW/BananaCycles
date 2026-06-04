package com.ky.bananacycles.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.ky.bananacycles.model.WasteItem
import com.ky.bananacycles.repository.WasteRepository

data class WasteUiState(
    val marketListings: List<WasteItem> = emptyList(),
    val myListings: List<WasteItem> = emptyList(),
    val isMarketLoading: Boolean = false,
    val isMyListingsLoading: Boolean = false,
    val isUploading: Boolean = false,
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

    // Market listens to all Listings, then excludes listings owned by the current user.
    fun loadMarketListings() {
        val currentUserId = auth.currentUser?.uid

        if (currentUserId.isNullOrBlank()) {
            uiState = uiState.copy(
                marketListings = emptyList(),
                isMarketLoading = false,
                errorMessage = "User is not signed in."
            )
            return
        }

        marketListener?.remove()
        uiState = uiState.copy(
            isMarketLoading = true,
            errorMessage = null
        )

        marketListener = repository.listenMarketListings(
            currentUserId = currentUserId,
            onDataChanged = { listings ->
                uiState = uiState.copy(
                    marketListings = listings,
                    isMarketLoading = false,
                    errorMessage = null
                )
            },
            onError = { error ->
                uiState = uiState.copy(
                    isMarketLoading = false,
                    errorMessage = error.localizedMessage
                        ?: "Failed to load the marketplace."
                )
            }
        )
    }

    // My Listings uses a sellerId query so users only see their own uploads here.
    fun loadMyListings() {
        val currentUserId = auth.currentUser?.uid

        if (currentUserId.isNullOrBlank()) {
            uiState = uiState.copy(
                myListings = emptyList(),
                isMyListingsLoading = false,
                errorMessage = "User is not signed in."
            )
            return
        }

        myListingsListener?.remove()
        uiState = uiState.copy(
            isMyListingsLoading = true,
            errorMessage = null
        )

        myListingsListener = repository.listenUserListings(
            currentUserId = currentUserId,
            onDataChanged = { listings ->
                uiState = uiState.copy(
                    myListings = listings,
                    isMyListingsLoading = false,
                    errorMessage = null
                )
            },
            onError = { error ->
                uiState = uiState.copy(
                    isMyListingsLoading = false,
                    errorMessage = error.localizedMessage
                        ?: "Failed to load your waste listings."
                )
            }
        )
    }

    fun addListing(
        wasteName: String,
        category: String,
        weight: Double,
        estimatedPrice: Int,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val currentUserId = auth.currentUser?.uid

        if (currentUserId.isNullOrBlank()) {
            onFailure("User is not signed in.")
            return
        }

        uiState = uiState.copy(
            isUploading = true,
            errorMessage = null
        )

        repository.addListing(
            wasteName = wasteName,
            category = category,
            weight = weight,
            estimatedPrice = estimatedPrice,
            sellerId = currentUserId,
            onSuccess = {
                uiState = uiState.copy(isUploading = false)
                onSuccess()
            },
            onFailure = { error ->
                val message = error.localizedMessage
                    ?: "Failed to create the waste listing."

                uiState = uiState.copy(
                    isUploading = false,
                    errorMessage = message
                )
                onFailure(message)
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

    override fun onCleared() {
        marketListener?.remove()
        myListingsListener?.remove()
        super.onCleared()
    }
}
