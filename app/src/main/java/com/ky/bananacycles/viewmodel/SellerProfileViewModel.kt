package com.ky.bananacycles.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ListenerRegistration
import com.ky.bananacycles.model.UserProfile
import com.ky.bananacycles.model.UserStats
import com.ky.bananacycles.model.WasteItem
import com.ky.bananacycles.repository.ProfileRepository
import com.ky.bananacycles.repository.WasteRepository

data class SellerProfileUiState(
    val profile: UserProfile = UserProfile(),
    val stats: UserStats = UserStats(),
    val listings: List<WasteItem> = emptyList(),
    val isLoadingListings: Boolean = false,
    val errorMessage: String? = null
)

class SellerProfileViewModel(
    private val profileRepository: ProfileRepository = ProfileRepository(),
    private val wasteRepository: WasteRepository = WasteRepository()
) : ViewModel() {
    var uiState by mutableStateOf(SellerProfileUiState())
        private set

    private var profileListener: ListenerRegistration? = null
    private var statsListener: ListenerRegistration? = null
    private var listingsListener: ListenerRegistration? = null

    fun listenSeller(sellerId: String) {
        if (sellerId.isBlank()) {
            return
        }

        profileListener?.remove()
        statsListener?.remove()
        listingsListener?.remove()

        uiState = uiState.copy(isLoadingListings = true)

        profileListener = profileRepository.listenUserProfile(
            userId = sellerId,
            onDataChanged = { profile ->
                uiState = uiState.copy(profile = profile)
            },
            onError = { error ->
                uiState = uiState.copy(errorMessage = error.localizedMessage)
            }
        )

        statsListener = profileRepository.listenUserStats(
            userId = sellerId,
            onDataChanged = { stats ->
                uiState = uiState.copy(stats = stats)
            },
            onError = { error ->
                uiState = uiState.copy(errorMessage = error.localizedMessage)
            }
        )

        listingsListener = wasteRepository.listenSellerActiveListings(
            sellerId = sellerId,
            onDataChanged = { listings ->
                uiState = uiState.copy(
                    listings = listings,
                    isLoadingListings = false
                )
            },
            onError = { error ->
                uiState = uiState.copy(
                    isLoadingListings = false,
                    errorMessage = error.localizedMessage
                )
            }
        )
    }

    fun clearSeller() {
        profileListener?.remove()
        statsListener?.remove()
        listingsListener?.remove()
        profileListener = null
        statsListener = null
        listingsListener = null
        uiState = SellerProfileUiState()
    }

    override fun onCleared() {
        clearSeller()
        super.onCleared()
    }
}
