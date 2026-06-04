package com.ky.bananacycles.service

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.ky.bananacycles.model.WasteItem

class WasteFirestoreService(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val listingsCollection = firestore.collection("Listings")

    // Writes a new listing with the authenticated user's uid as sellerId.
    fun addListing(
        wasteName: String,
        category: String,
        weight: Double,
        estimatedPrice: Int,
        sellerId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val listingDocument = listingsCollection.document()

        val data = hashMapOf(
            "listingId" to listingDocument.id,
            "sellerId" to sellerId,
            "wasteName" to wasteName,
            "category" to category,
            "weight" to weight,
            "estimatedPrice" to estimatedPrice,
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

    fun listenMarketListings(
        currentUserId: String,
        onDataChanged: (List<WasteItem>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return listingsCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val listings = snapshot
                    ?.toWasteItems()
                    ?.filter { listing ->
                        listing.sellerId != currentUserId
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

            WasteItem(
                id = document.getString("listingId") ?: document.id,
                sellerId = document.getString("sellerId").orEmpty(),
                wasteName = document.getString("wasteName").orEmpty(),
                category = document.getString("category").orEmpty(),
                weight = document.getDouble("weight") ?: 0.0,
                estimatedPrice = document.getLong("estimatedPrice")?.toInt() ?: 0,
                createdAt = createdAtTimestamp.toMillisOrZero()
            )
        }
    }

    private fun Timestamp?.toMillisOrZero(): Long {
        return this?.toDate()?.time ?: 0L
    }
}
