package com.ky.bananacycles.repository

import android.net.Uri
import com.google.firebase.firestore.ListenerRegistration
import com.ky.bananacycles.model.UserProfile
import com.ky.bananacycles.model.UserStats
import com.ky.bananacycles.service.ProfileFirestoreService

class ProfileRepository(
    private val service: ProfileFirestoreService = ProfileFirestoreService()
) {
    fun listenUserProfile(
        userId: String,
        onDataChanged: (UserProfile) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return service.listenUserProfile(
            userId = userId,
            onDataChanged = onDataChanged,
            onError = onError
        )
    }

    fun listenUserStats(
        userId: String,
        onDataChanged: (UserStats) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return service.listenUserStats(
            userId = userId,
            onDataChanged = onDataChanged,
            onError = onError
        )
    }

    fun updateProfile(
        userId: String,
        displayName: String,
        imageUri: Uri?,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        service.updateProfile(
            userId = userId,
            displayName = displayName,
            imageUri = imageUri,
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }
}
