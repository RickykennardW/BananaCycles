package com.ky.bananacycles.service

import com.google.firebase.firestore.FieldValue
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.ky.bananacycles.model.Order
import com.ky.bananacycles.model.OrderStatus

class OrderFirestoreService(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val ordersCollection = firestore.collection("Orders")
    private val listingsCollection = firestore.collection("Listings")
    private val historyCollection = firestore.collection("OrderHistory")
    private val statsCollection = firestore.collection("UserStats")

    fun listenPurchases(
        buyerId: String,
        onDataChanged: (List<Order>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return ordersCollection
            .whereEqualTo("buyerId", buyerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                onDataChanged(
                    snapshot
                        ?.toOrders()
                        ?.filterNot { order -> order.status == OrderStatus.FINISHED.name }
                        ?.sortedByDescending { order -> order.createdAt }
                        .orEmpty()
                )
            }
    }

    fun listenSales(
        sellerId: String,
        onDataChanged: (List<Order>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return ordersCollection
            .whereEqualTo("sellerId", sellerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                onDataChanged(
                    snapshot
                        ?.toOrders()
                        ?.filterNot { order -> order.status == OrderStatus.FINISHED.name }
                        ?.sortedByDescending { order -> order.createdAt }
                        .orEmpty()
                )
            }
    }

    fun updateOrderStatus(
        order: Order,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val nextStatus = order.status.nextOrderStatus()

        if (nextStatus == null) {
            onSuccess()
            return
        }

        val orderDocument = ordersCollection.document(order.orderId)

        firestore
            .runTransaction { transaction ->
                transaction.update(orderDocument, "status", nextStatus.name)

                if (nextStatus == OrderStatus.FINISHED) {
                    transaction.update(orderDocument, "completedAt", FieldValue.serverTimestamp())
                    transaction.set(
                        historyCollection.document(order.orderId),
                        order.toHistoryData(nextStatus.name)
                    )
                    transaction.set(
                        statsCollection.document(order.sellerId),
                        mapOf(
                            "userId" to order.sellerId,
                            "totalSalesCompleted" to FieldValue.increment(1)
                        ),
                        com.google.firebase.firestore.SetOptions.merge()
                    )
                    transaction.set(
                        statsCollection.document(order.buyerId),
                        mapOf(
                            "userId" to order.buyerId,
                            "totalPurchasesCompleted" to FieldValue.increment(1)
                        ),
                        com.google.firebase.firestore.SetOptions.merge()
                    )
                }
            }
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->
                onFailure(error)
            }
    }

    fun cancelOrder(
        order: Order,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (order.status == OrderStatus.FINISHED.name || order.status == OrderStatus.CANCELLED.name) {
            onSuccess()
            return
        }

        val orderDocument = ordersCollection.document(order.orderId)
        val listingDocument = listingsCollection.document(order.listingId)

        firestore
            .runTransaction { transaction ->
                val listingSnapshot = transaction.get(listingDocument)
                val currentStock = listingSnapshot.getDouble("stockKg") ?: 0.0
                val restoredStock = currentStock + order.quantityPurchased

                transaction.update(
                    listingDocument,
                    mapOf(
                        "stockKg" to restoredStock,
                        "status" to com.ky.bananacycles.model.ListingStatus.ACTIVE.name
                    )
                )

                transaction.update(
                    orderDocument,
                    mapOf(
                        "status" to OrderStatus.CANCELLED.name,
                        "cancelledAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->
                onFailure(error)
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
                status = document.getString("status") ?: OrderStatus.PACKING.name,
                createdAt = document.getTimestamp("createdAt").toMillisOrZero(),
                completedAt = document.getTimestamp("completedAt").toMillisOrZero(),
                cancelledAt = document.getTimestamp("cancelledAt").toMillisOrZero()
            )
        }
    }

    private fun String.nextOrderStatus(): OrderStatus? {
        return when (this) {
            OrderStatus.PACKING.name -> OrderStatus.SENDING
            OrderStatus.SENDING.name -> OrderStatus.MEET
            OrderStatus.MEET.name -> OrderStatus.FINISHED
            else -> null
        }
    }

    private fun Order.toHistoryData(status: String): Map<String, Any?> {
        return mapOf(
            "historyId" to orderId,
            "orderId" to orderId,
            "listingId" to listingId,
            "buyerId" to buyerId,
            "buyerName" to buyerName,
            "sellerId" to sellerId,
            "sellerName" to sellerName,
            "productName" to productName,
            "productImage" to productImage,
            "quantityPurchased" to quantityPurchased,
            "pricePerKg" to pricePerKg,
            "totalPrice" to totalPrice,
            "status" to status,
            "createdAt" to createdAt,
            "completedAt" to FieldValue.serverTimestamp()
        )
    }

    private fun Timestamp?.toMillisOrZero(): Long {
        return this?.toDate()?.time ?: 0L
    }
}
