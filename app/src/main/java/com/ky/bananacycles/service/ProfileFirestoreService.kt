package com.ky.bananacycles.service

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.ky.bananacycles.model.UserProfile
import com.ky.bananacycles.model.UserStats

class ProfileFirestoreService(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    private val profilesCollection = firestore.collection("UserProfiles")

    fun listenUserProfile(
        userId: String,
        onDataChanged: (UserProfile) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return profilesCollection
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val currentUser = auth.currentUser
                onDataChanged(
                    UserProfile(
                        userId = userId,
                        displayName = snapshot?.getString("displayName")
                            ?: currentUser?.displayName
                            ?: currentUser?.email?.substringBefore("@")
                            ?: "BananaCycles User",
                        email = snapshot?.getString("email") ?: currentUser?.email.orEmpty(),
                        photoUrl = snapshot?.getString("photoUrl") ?: currentUser?.photoUrl?.toString().orEmpty()
                    )
                )
            }
    }

    fun updateProfile(
        userId: String,
        displayName: String,
        imageUri: Uri?,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (imageUri == null) {
            updateProfileData(
                userId = userId,
                displayName = displayName,
                photoUrl = auth.currentUser?.photoUrl?.toString().orEmpty(),
                onSuccess = onSuccess,
                onFailure = onFailure
            )
            return
        }

        val profileImageRef = storage.reference.child("profile_images/$userId.jpg")
        profileImageRef
            .putFile(imageUri)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                profileImageRef.downloadUrl
            }
            .addOnSuccessListener { downloadUrl ->
                updateProfileData(
                    userId = userId,
                    displayName = displayName,
                    photoUrl = downloadUrl.toString(),
                    onSuccess = onSuccess,
                    onFailure = onFailure
                )
            }
            .addOnFailureListener { error ->
                onFailure(error)
            }
    }

    fun listenUserStats(
        userId: String,
        onDataChanged: (UserStats) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return firestore
            .collection("UserStats")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                onDataChanged(
                    UserStats(
                        userId = snapshot?.getString("userId") ?: userId,
                        totalSalesCompleted = snapshot?.getLong("totalSalesCompleted")?.toInt() ?: 0,
                        totalPurchasesCompleted = snapshot?.getLong("totalPurchasesCompleted")?.toInt() ?: 0
                    )
                )
            }
    }

    private fun updateProfileData(
        userId: String,
        displayName: String,
        photoUrl: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onFailure(IllegalStateException("User is not signed in."))
            return
        }

        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(displayName)
            .setPhotoUri(photoUrl.takeIf { it.isNotBlank() }?.let { Uri.parse(it) })
            .build()

        currentUser
            .updateProfile(profileUpdates)
            .addOnSuccessListener {
                val data = mapOf(
                    "userId" to userId,
                    "displayName" to displayName,
                    "email" to currentUser.email.orEmpty(),
                    "photoUrl" to photoUrl
                )

                profilesCollection
                    .document(userId)
                    .set(data, SetOptions.merge())
                    .addOnSuccessListener {
                        propagateProfileChanges(
                            userId = userId,
                            displayName = displayName,
                            photoUrl = photoUrl,
                            onSuccess = onSuccess,
                            onFailure = onFailure
                        )
                    }
                    .addOnFailureListener { error ->
                        onFailure(error)
                    }
            }
            .addOnFailureListener { error ->
                onFailure(error)
            }
    }

    private fun propagateProfileChanges(
        userId: String,
        displayName: String,
        photoUrl: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val batch = firestore.batch()

        firestore.collection("Listings")
            .whereEqualTo("sellerId", userId)
            .get()
            .addOnSuccessListener { listingSnapshot ->
                listingSnapshot.documents.forEach { document ->
                    batch.update(
                        document.reference,
                        mapOf(
                            "sellerName" to displayName,
                            "sellerPhotoUrl" to photoUrl
                        )
                    )
                }

                firestore.collection("Orders")
                    .whereEqualTo("sellerId", userId)
                    .get()
                    .addOnSuccessListener { sellerOrders ->
                        sellerOrders.documents.forEach { document ->
                            batch.update(
                                document.reference,
                                mapOf(
                                    "sellerName" to displayName,
                                    "sellerPhotoUrl" to photoUrl
                                )
                            )
                        }

                        firestore.collection("Orders")
                            .whereEqualTo("buyerId", userId)
                            .get()
                            .addOnSuccessListener { buyerOrders ->
                                buyerOrders.documents.forEach { document ->
                                    batch.update(
                                        document.reference,
                                        mapOf(
                                            "buyerName" to displayName,
                                            "buyerPhotoUrl" to photoUrl
                                        )
                                    )
                                }

                                firestore.collection("Chats")
                                    .whereArrayContains("participants", userId)
                                    .get()
                                    .addOnSuccessListener { chatRooms ->
                                        chatRooms.documents.forEach { document ->
                                            batch.update(
                                                document.reference,
                                                mapOf(
                                                    "participantNames.$userId" to displayName,
                                                    "participantPhotoUrls.$userId" to photoUrl
                                                )
                                            )
                                        }

                                        firestore.collection("OrderHistory")
                                            .whereEqualTo("sellerId", userId)
                                            .get()
                                            .addOnSuccessListener { sellerHistory ->
                                                sellerHistory.documents.forEach { document ->
                                                    batch.update(
                                                        document.reference,
                                                        mapOf(
                                                            "sellerName" to displayName,
                                                            "sellerPhotoUrl" to photoUrl
                                                        )
                                                    )
                                                }

                                                firestore.collection("OrderHistory")
                                                    .whereEqualTo("buyerId", userId)
                                                    .get()
                                                    .addOnSuccessListener { buyerHistory ->
                                                        buyerHistory.documents.forEach { document ->
                                                            batch.update(
                                                                document.reference,
                                                                mapOf(
                                                                    "buyerName" to displayName,
                                                                    "buyerPhotoUrl" to photoUrl
                                                                )
                                                            )
                                                        }

                                                        batch.commit()
                                                            .addOnSuccessListener { onSuccess() }
                                                            .addOnFailureListener { error -> onFailure(error) }
                                                    }
                                                    .addOnFailureListener { error -> onFailure(error) }
                                            }
                                            .addOnFailureListener { error -> onFailure(error) }
                                    }
                                    .addOnFailureListener { error -> onFailure(error) }
                            }
                            .addOnFailureListener { error -> onFailure(error) }
                    }
                    .addOnFailureListener { error -> onFailure(error) }
            }
            .addOnFailureListener { error ->
                onFailure(error)
            }
    }
}
