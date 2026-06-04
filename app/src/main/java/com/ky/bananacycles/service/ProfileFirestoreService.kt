package com.ky.bananacycles.service

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.ky.bananacycles.model.UserStats

class ProfileFirestoreService(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
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
}
