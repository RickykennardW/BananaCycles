package com.ky.bananacycles.service

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.ky.bananacycles.model.SelectedImage
import com.ky.bananacycles.model.UserProfile
import com.ky.bananacycles.model.UserStats
import java.util.Locale

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
        selectedImage: SelectedImage?,
        onProgress: (Float?) -> Unit,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (selectedImage == null) {
            updateProfileData(
                userId = userId,
                displayName = displayName,
                photoUrl = auth.currentUser?.photoUrl?.toString().orEmpty(),
                onProgress = onProgress,
                onSuccess = onSuccess,
                onFailure = onFailure
            )
            return
        }

        val imageUri = Uri.parse(selectedImage.sourceUri)
        android.util.Log.d(
            "IMAGE_DEBUG",
            "Selected profile URI=${selectedImage.sourceUri}, mimeType=${selectedImage.mimeType}, bytes=${selectedImage.bytes.size}, userId=$userId"
        )

        if (imageUri.isRemoteImageUrl()) {
            updateProfileData(
                userId = userId,
                displayName = displayName,
                photoUrl = imageUri.toString(),
                onProgress = onProgress,
                onSuccess = onSuccess,
                onFailure = onFailure
            )
            return
        }

        if (!imageUri.isLocalUploadableImage()) {
            onFailure(IllegalArgumentException("Unsupported image source. Please choose an image from your gallery."))
            return
        }

        onProgress(0f)
        val uploadId = System.currentTimeMillis()
        val profileImageRef = storage.reference
            .child("profile_images/$userId/profile.jpg")
        val metadata = StorageMetadata.Builder()
            .setContentType(selectedImage.mimeType.toSupportedImageContentType() ?: "image/jpeg")
            .setCustomMetadata("updatedAt", uploadId.toString())
            .build()

        android.util.Log.d(
            "IMAGE_DEBUG",
            "Upload Started profile storagePath=${profileImageRef.path}, bucket=${storage.reference.bucket}, contentType=${metadata.contentType}, bytes=${selectedImage.bytes.size}"
        )

        profileImageRef.uploadImageWithRetry(
            selectedImage = selectedImage,
            metadata = metadata,
            attemptsRemaining = 2,
            onProgress = onProgress,
            onSuccess = { downloadUrl ->
                val freshPhotoUrl = downloadUrl.withCacheBust(uploadId)
                cleanupOldProfileImages(
                    userId = userId,
                    activePath = profileImageRef.path,
                    onComplete = {
                        updateProfileData(
                            userId = userId,
                            displayName = displayName,
                            photoUrl = freshPhotoUrl,
                            onProgress = onProgress,
                            onSuccess = onSuccess,
                            onFailure = onFailure
                        )
                    }
                )
            },
            onFailure = onFailure
        )
    }

    private fun cleanupOldProfileImages(
        userId: String,
        activePath: String,
        onComplete: () -> Unit
    ) {
        val profileFolder = storage.reference.child("profile_images/$userId")
        android.util.Log.d("IMAGE_DEBUG", "Profile cleanup start userId=$userId activePath=$activePath")
        profileFolder
            .listAll()
            .addOnSuccessListener { result ->
                val staleItems = result.items.filter { item -> item.path != activePath }
                if (staleItems.isEmpty()) {
                    android.util.Log.d("IMAGE_DEBUG", "Profile cleanup no stale images userId=$userId")
                    onComplete()
                    return@addOnSuccessListener
                }

                var pendingDeletes = staleItems.size
                staleItems.forEach { item ->
                    item.delete()
                        .addOnSuccessListener {
                            android.util.Log.d("IMAGE_DEBUG", "Profile cleanup deleted stale image path=${item.path}")
                            pendingDeletes -= 1
                            if (pendingDeletes == 0) {
                                onComplete()
                            }
                        }
                        .addOnFailureListener { error ->
                            android.util.Log.e("IMAGE_DEBUG", "Profile cleanup failed path=${item.path}", error)
                            pendingDeletes -= 1
                            if (pendingDeletes == 0) {
                                onComplete()
                            }
                        }
                }
            }
            .addOnFailureListener { error ->
                android.util.Log.e("IMAGE_DEBUG", "Profile cleanup list failed userId=$userId", error)
                onComplete()
            }
    }

    private fun String.withCacheBust(version: Long): String {
        return if (contains("?")) {
            "$this&v=$version"
        } else {
            "$this?v=$version"
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
                        totalPurchasesCompleted = snapshot?.getLong("totalPurchasesCompleted")?.toInt() ?: 0,
                        aiAssistedListings = snapshot?.getLong("aiAssistedListings")?.toInt() ?: 0
                    )
                )
            }
    }

    private fun updateProfileData(
        userId: String,
        displayName: String,
        photoUrl: String,
        onProgress: (Float?) -> Unit,
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
                    "uid" to userId,
                    "userId" to userId,
                    "displayName" to displayName,
                    "email" to currentUser.email.orEmpty(),
                    "photoUrl" to photoUrl
                )

                android.util.Log.d("IMAGE_DEBUG", "Firestore Update start profile userId=$userId, photoUrl=$photoUrl")
                profilesCollection
                    .document(userId)
                    .set(data, SetOptions.merge())
                    .addOnSuccessListener {
                        android.util.Log.d("IMAGE_DEBUG", "Firestore Update success profile userId=$userId")
                        propagateProfileChanges(
                            userId = userId,
                            displayName = displayName,
                            photoUrl = photoUrl,
                            onSuccess = {
                                onProgress(null)
                                onSuccess()
                            },
                            onFailure = onFailure
                        )
                    }
                    .addOnFailureListener { error ->
                        android.util.Log.e("IMAGE_DEBUG", "Firestore Update failure profile userId=$userId", error)
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
                                                            .addOnSuccessListener {
                                                                onSuccess()
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
                    .addOnFailureListener { error -> onFailure(error) }
            }
            .addOnFailureListener { error ->
                onFailure(error)
            }
    }

    private fun com.google.firebase.storage.StorageReference.uploadImageWithRetry(
        selectedImage: SelectedImage,
        metadata: StorageMetadata,
        attemptsRemaining: Int,
        onProgress: (Float?) -> Unit,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        putBytes(selectedImage.bytes, metadata)
            .addOnProgressListener { snapshot ->
                val totalBytes = snapshot.totalByteCount
                if (totalBytes > 0L) {
                    onProgress(snapshot.bytesTransferred.toFloat() / totalBytes.toFloat())
                }
            }
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    android.util.Log.e("IMAGE_DEBUG", "Upload Failure profile storagePath=$path", task.exception)
                    task.exception?.let { throw it }
                }
                android.util.Log.d("IMAGE_DEBUG", "Upload Success profile storagePath=$path; verifying metadata")
                this.metadata
            }
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    android.util.Log.e("IMAGE_DEBUG", "Storage metadata verification failed profile storagePath=$path", task.exception)
                    task.exception?.let { throw it }
                }
                android.util.Log.d("IMAGE_DEBUG", "Storage metadata verified profile storagePath=$path size=${task.result.sizeBytes} contentType=${task.result.contentType}")
                android.util.Log.d("IMAGE_DEBUG", "Download URL Request profile storagePath=$path")
                downloadUrl
            }
            .addOnSuccessListener { downloadUrl ->
                android.util.Log.d("IMAGE_DEBUG", "Download URL Success profile storagePath=$path url=$downloadUrl")
                onProgress(1f)
                onSuccess(downloadUrl.toString())
            }
            .addOnFailureListener { error ->
                android.util.Log.e("IMAGE_DEBUG", "Download URL Failure or upload chain failure profile storagePath=$path attemptsRemaining=$attemptsRemaining", error)
                if (attemptsRemaining > 0) {
                    uploadImageWithRetry(
                        selectedImage = selectedImage,
                        metadata = metadata,
                        attemptsRemaining = attemptsRemaining - 1,
                        onProgress = onProgress,
                        onSuccess = onSuccess,
                        onFailure = onFailure
                    )
                } else {
                    onProgress(null)
                    onFailure(error)
                }
            }
    }

    private fun Uri.isRemoteImageUrl(): Boolean {
        val normalizedScheme = scheme?.lowercase(Locale.US)
        return normalizedScheme == "http" || normalizedScheme == "https"
    }

    private fun Uri.isLocalUploadableImage(): Boolean {
        val normalizedScheme = scheme?.lowercase(Locale.US)
        return normalizedScheme == "content" || normalizedScheme == "file"
    }

    private fun String.supportedImageExtension(): String {
        return when (toSupportedImageContentType()) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
    }

    private fun String?.toSupportedImageContentType(): String? {
        return when (this?.lowercase(Locale.US)) {
            "image/jpg",
            "image/jpeg" -> "image/jpeg"
            "image/png" -> "image/png"
            "image/webp" -> "image/webp"
            else -> null
        }
    }

    private fun String.toImageContentType(): String {
        return when (this) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> "image/jpeg"
        }
    }
}
