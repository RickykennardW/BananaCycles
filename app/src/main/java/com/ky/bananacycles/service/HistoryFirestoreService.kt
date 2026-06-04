package com.ky.bananacycles.service

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.ky.bananacycles.model.Order
import com.ky.bananacycles.model.OrderStatus

class HistoryFirestoreService(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val historyCollection = firestore.collection("OrderHistory")

    fun listenPurchaseHistory(
        buyerId: String,
        onDataChanged: (List<Order>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return historyCollection
            .whereEqualTo("buyerId", buyerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                onDataChanged(
                    snapshot
                        ?.toOrders()
                        ?.sortedByDescending { order -> order.completedAt }
                        .orEmpty()
                )
            }
    }

    fun listenSalesHistory(
        sellerId: String,
        onDataChanged: (List<Order>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return historyCollection
            .whereEqualTo("sellerId", sellerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                onDataChanged(
                    snapshot
                        ?.toOrders()
                        ?.sortedByDescending { order -> order.completedAt }
                        .orEmpty()
                )
            }
    }

    private fun QuerySnapshot.toOrders(): List<Order> {
        return documents.map { document ->
            Order(
                orderId = document.getString("orderId") ?: document.id,
                listingId = document.getString("listingId").orEmpty(),
                buyerId = document.getString("buyerId").orEmpty(),
                buyerName = document.getString("buyerName").orEmpty(),
                sellerId = document.getString("sellerId").orEmpty(),
                sellerName = document.getString("sellerName").orEmpty(),
                productName = document.getString("productName").orEmpty(),
                productImage = document.getString("productImage").orEmpty(),
                quantityPurchased = document.getDouble("quantityPurchased") ?: 0.0,
                pricePerKg = document.getLong("pricePerKg")?.toInt() ?: 0,
                totalPrice = document.getLong("totalPrice")?.toInt() ?: 0,
                status = document.getString("status") ?: OrderStatus.FINISHED.name,
                createdAt = document.getLong("createdAt") ?: document.getTimestamp("createdAt").toMillisOrZero(),
                completedAt = document.getTimestamp("completedAt").toMillisOrZero()
            )
        }
    }

    private fun Timestamp?.toMillisOrZero(): Long {
        return this?.toDate()?.time ?: 0L
    }
}
