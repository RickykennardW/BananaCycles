package com.ky.bananacycles.repository

import com.google.firebase.firestore.ListenerRegistration
import com.ky.bananacycles.model.UserStats
import com.ky.bananacycles.service.ProfileFirestoreService

class ProfileRepository(
    private val service: ProfileFirestoreService = ProfileFirestoreService()
) {
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
}
