package com.ky.bananacycles.repository

import com.google.firebase.firestore.ListenerRegistration
import com.ky.bananacycles.model.SelectedImage
import com.ky.bananacycles.model.WasteItem
import com.ky.bananacycles.model.WastePrediction
import com.ky.bananacycles.service.WasteFirestoreService

class WasteRepository(
    private val service: WasteFirestoreService = WasteFirestoreService()
) {

    fun addListing(
        sellerId: String,
        sellerName: String,
        sellerPhotoUrl: String,
        wasteName: String,
        description: String,
        category: String,
        stockKg: Double,
        pricePerKg: Int,
        selectedImage: SelectedImage?,
        existingImageUrl: String,
        wastePrediction: WastePrediction?,
        onProgress: (Float?) -> Unit,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        service.addListing(
            sellerId = sellerId,
            sellerName = sellerName,
            sellerPhotoUrl = sellerPhotoUrl,
            wasteName = wasteName,
            description = description,
            category = category,
            stockKg = stockKg,
            pricePerKg = pricePerKg,
            selectedImage = selectedImage,
            existingImageUrl = existingImageUrl,
            wastePrediction = wastePrediction,
            onProgress = onProgress,
            onSuccess = onSuccess,
            onFailure = onFailure
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
        wastePrediction: WastePrediction?,
        onProgress: (Float?) -> Unit,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        service.updateListing(
            listingId = listingId,
            wasteName = wasteName,
            description = description,
            category = category,
            pricePerKg = pricePerKg,
            sellerId = sellerId,
            selectedImage = selectedImage,
            existingImageUrl = existingImageUrl,
            wastePrediction = wastePrediction,
            onProgress = onProgress,
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }

    fun adjustStock(
        listingId: String,
        stockDeltaKg: Double,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        service.adjustStock(
            listingId = listingId,
            stockDeltaKg = stockDeltaKg,
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }

    fun deleteListing(
        listingId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        service.deleteListing(
            listingId = listingId,
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }

    fun purchaseListing(
        listing: WasteItem,
        buyerId: String,
        buyerName: String,
        quantityKg: Double,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        service.purchaseListing(
            listing = listing,
            buyerId = buyerId,
            buyerName = buyerName,
            quantityKg = quantityKg,
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

    fun listenSellerActiveListings(
        sellerId: String,
        onDataChanged: (List<WasteItem>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return service.listenSellerActiveListings(
            sellerId = sellerId,
            onDataChanged = onDataChanged,
            onError = onError
        )
    }
}
