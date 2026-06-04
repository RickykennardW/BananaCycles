package com.ky.bananacycles.service

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.ky.bananacycles.model.ListingStatus
import com.ky.bananacycles.model.OrderStatus
import com.ky.bananacycles.model.WasteItem

private const val IMAGE_DEBUG_TAG = "IMAGE_DEBUG"

class WasteFirestoreService(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val listingsCollection = firestore.collection("Listings")

    fun addListing(
        sellerId: String,
        sellerName: String,
        wasteName: String,
        category: String,
        stockKg: Double,
        pricePerKg: Int,
        imageUrl: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val listingDocument = listingsCollection.document()
        val data = hashMapOf(
            "listingId" to listingDocument.id,
            "sellerId" to sellerId,
            "sellerName" to sellerName,
            "wasteName" to wasteName,
            "category" to category,
            "stockKg" to stockKg,
            "pricePerKg" to pricePerKg,
            "imageUrl" to imageUrl,
            "status" to ListingStatus.ACTIVE.name,
            "createdAt" to FieldValue.serverTimestamp()
        )

        listingDocument
            .set(data)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->
                onFailure(error)
            }
    }

    fun updateListing(
        listingId: String,
        wasteName: String,
        category: String,
        pricePerKg: Int,
        imageUrl: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val data = mapOf(
            "wasteName" to wasteName,
            "category" to category,
            "pricePerKg" to pricePerKg,
            "imageUrl" to imageUrl
        )

        listingsCollection
            .document(listingId)
            .update(data)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->
                onFailure(error)
            }
    }

    fun adjustStock(
        listingId: String,
        stockDeltaKg: Double,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val listingDocument = listingsCollection.document(listingId)

        // Stock changes are transactional so add/remove actions cannot overwrite each other.
        firestore
            .runTransaction { transaction ->
                val snapshot = transaction.get(listingDocument)
                val currentStock = snapshot.getDouble("stockKg")
                    ?: snapshot.getDouble("weight")
                    ?: 0.0
                val nextStock = currentStock + stockDeltaKg

                if (nextStock < 0.0) {
                    throw IllegalArgumentException("Insufficient stock available.")
                }

                val nextStatus = if (nextStock > 0.0) {
                    ListingStatus.ACTIVE.name
                } else {
                    ListingStatus.SOLD_OUT.name
                }

                transaction.update(
                    listingDocument,
                    mapOf(
                        "stockKg" to nextStock,
                        "status" to nextStatus
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

    fun deleteListing(
        listingId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        listingsCollection
            .document(listingId)
            .delete()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->
                onFailure(error)
            }
    }

    fun purchaseListing(
        listing: WasteItem,
        buyerId: String,
        buyerName: String,
        quantityKg: Double,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val listingDocument = listingsCollection.document(listing.id)
        val orderDocument = firestore.collection("Orders").document()

        firestore
            .runTransaction { transaction ->
                val snapshot = transaction.get(listingDocument)
                val currentStock = snapshot.getDouble("stockKg")
                    ?: snapshot.getDouble("weight")
                    ?: 0.0
                val currentStatus = snapshot.getString("status")
                    ?: ListingStatus.ACTIVE.name

                if (currentStatus != ListingStatus.ACTIVE.name || currentStock <= 0.0) {
                    throw IllegalStateException("This listing is sold out.")
                }

                if (quantityKg <= 0.0) {
                    throw IllegalArgumentException("Please enter a purchase quantity greater than 0 kg.")
                }

                if (quantityKg > currentStock) {
                    throw IllegalArgumentException("Requested quantity exceeds available stock.")
                }

                val remainingStock = currentStock - quantityKg
                val nextStatus = if (remainingStock <= 0.0) {
                    ListingStatus.SOLD_OUT.name
                } else {
                    ListingStatus.ACTIVE.name
                }

                transaction.update(
                    listingDocument,
                    mapOf(
                        "stockKg" to remainingStock.coerceAtLeast(0.0),
                        "status" to nextStatus
                    )
                )

                val pricePerKg = snapshot.getLong("pricePerKg")?.toInt()
                    ?: listing.pricePerKg
                val totalPrice = (quantityKg * pricePerKg).toInt()

                transaction.set(
                    orderDocument,
                    mapOf(
                        "orderId" to orderDocument.id,
                        "listingId" to listing.id,
                        "buyerId" to buyerId,
                        "buyerName" to buyerName,
                        "sellerId" to (snapshot.getString("sellerId") ?: listing.sellerId),
                        "sellerName" to (snapshot.getString("sellerName") ?: listing.sellerName),
                        "productName" to (snapshot.getString("wasteName") ?: listing.wasteName),
                        "productImage" to (snapshot.getString("imageUrl") ?: listing.imageUrl),
                        "quantityPurchased" to quantityKg,
                        "pricePerKg" to pricePerKg,
                        "totalPrice" to totalPrice,
                        "status" to OrderStatus.PACKING.name,
                        "createdAt" to FieldValue.serverTimestamp()
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

    fun listenMarketListings(
        currentUserId: String,
        onDataChanged: (List<WasteItem>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return listingsCollection
            .whereNotEqualTo("sellerId", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val listings = snapshot
                    ?.toWasteItems()
                    ?.filter { listing ->
                        listing.status == ListingStatus.ACTIVE.name &&
                            listing.stockKg > 0.0
                    }
                    ?.sortedByDescending { listing ->
                        listing.createdAt
                    }
                    .orEmpty()

                onDataChanged(listings)
            }
    }

    fun listenUserListings(
        currentUserId: String,
        onDataChanged: (List<WasteItem>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return listingsCollection
            .whereEqualTo("sellerId", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val listings = snapshot
                    ?.toWasteItems()
                    ?.sortedByDescending { listing ->
                        listing.createdAt
                    }
                    .orEmpty()

                onDataChanged(listings)
            }
    }

    private fun QuerySnapshot.toWasteItems(): List<WasteItem> {
        return documents.map { document ->
            val createdAtTimestamp = document.getTimestamp("createdAt")
            val stockKg = document.getDouble("stockKg")
                ?: document.getDouble("weight")
                ?: 0.0
            val pricePerKg = document.getLong("pricePerKg")?.toInt()
                ?: document.getLong("estimatedPrice")?.toInt()
                ?: 0
            val status = document.getString("status")
                ?: if (stockKg > 0.0) ListingStatus.ACTIVE.name else ListingStatus.SOLD_OUT.name
            val imageUrl = document.getString("imageUrl").orEmpty()
            val sellerId = document.getString("sellerId").orEmpty()

            Log.d(
                IMAGE_DEBUG_TAG,
                "Firestore listingId=${document.id}, sellerId=$sellerId, imageUrl=$imageUrl"
            )

            WasteItem(
                id = document.getString("listingId") ?: document.id,
                sellerId = sellerId,
                sellerName = document.getString("sellerName").orEmpty(),
                wasteName = document.getString("wasteName").orEmpty(),
                category = document.getString("category").orEmpty(),
                stockKg = stockKg,
                pricePerKg = pricePerKg,
                imageUrl = imageUrl,
                status = status,
                createdAt = createdAtTimestamp.toMillisOrZero()
            )
        }
    }

    private fun Timestamp?.toMillisOrZero(): Long {
        return this?.toDate()?.time ?: 0L
    }
}
