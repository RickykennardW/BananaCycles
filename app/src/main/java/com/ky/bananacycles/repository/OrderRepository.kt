package com.ky.bananacycles.repository

import com.google.firebase.firestore.ListenerRegistration
import com.ky.bananacycles.model.Order
import com.ky.bananacycles.service.OrderFirestoreService

class OrderRepository(
    private val service: OrderFirestoreService = OrderFirestoreService()
) {
    fun listenPurchases(
        buyerId: String,
        onDataChanged: (List<Order>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return service.listenPurchases(
            buyerId = buyerId,
            onDataChanged = onDataChanged,
            onError = onError
        )
    }

    fun listenSales(
        sellerId: String,
        onDataChanged: (List<Order>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return service.listenSales(
            sellerId = sellerId,
            onDataChanged = onDataChanged,
            onError = onError
        )
    }

    fun updateOrderStatus(
        order: Order,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        service.updateOrderStatus(
            order = order,
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }

    fun cancelOrder(
        order: Order,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        service.cancelOrder(
            order = order,
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }
}
