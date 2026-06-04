package com.ky.bananacycles.repository

import com.google.firebase.firestore.ListenerRegistration
import com.ky.bananacycles.model.Order
import com.ky.bananacycles.service.HistoryFirestoreService

class HistoryRepository(
    private val service: HistoryFirestoreService = HistoryFirestoreService()
) {
    fun listenPurchaseHistory(
        buyerId: String,
        onDataChanged: (List<Order>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return service.listenPurchaseHistory(
            buyerId = buyerId,
            onDataChanged = onDataChanged,
            onError = onError
        )
    }

    fun listenSalesHistory(
        sellerId: String,
        onDataChanged: (List<Order>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return service.listenSalesHistory(
            sellerId = sellerId,
            onDataChanged = onDataChanged,
            onError = onError
        )
    }
}
