package com.ky.bananacycles.repository

import com.google.firebase.firestore.ListenerRegistration
import com.ky.bananacycles.model.WasteItem
import com.ky.bananacycles.service.WasteFirestoreService

class WasteRepository(
    private val service: WasteFirestoreService = WasteFirestoreService()
) {

    // Repository keeps Firebase details out of composables and ViewModel callers.
    fun addListing(
        wasteName: String,
        category: String,
        weight: Double,
        estimatedPrice: Int,
        sellerId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        service.addListing(
            wasteName = wasteName,
            category = category,
            weight = weight,
            estimatedPrice = estimatedPrice,
            sellerId = sellerId,
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }

    fun listenMarketListings(
        currentUserId: String,
        onDataChanged: (List<WasteItem>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return service.listenMarketListings(
            currentUserId = currentUserId,
            onDataChanged = onDataChanged,
            onError = onError
        )
    }

    fun listenUserListings(
        currentUserId: String,
        onDataChanged: (List<WasteItem>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return service.listenUserListings(
            currentUserId = currentUserId,
            onDataChanged = onDataChanged,
            onError = onError
        )
    }
}
