package com.ky.bananacycles.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.ky.bananacycles.model.UserStats
import com.ky.bananacycles.repository.ProfileRepository

data class ProfileUiState(
    val stats: UserStats = UserStats(),
    val errorMessage: String? = null
)

class ProfileViewModel(
    private val repository: ProfileRepository = ProfileRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {
    var uiState by mutableStateOf(ProfileUiState())
        private set

    private var statsListener: ListenerRegistration? = null

    fun listenProfileStats() {
        val userId = auth.currentUser?.uid

        if (userId.isNullOrBlank()) {
            uiState = uiState.copy(errorMessage = "User is not signed in.")
            return
        }

        statsListener?.remove()
        statsListener = repository.listenUserStats(
            userId = userId,
            onDataChanged = { stats ->
                uiState = uiState.copy(
                    stats = stats,
                    errorMessage = null
                )
            },
            onError = { error ->
                uiState = uiState.copy(
                    errorMessage = error.localizedMessage ?: "Failed to load profile statistics."
                )
            }
        )
    }

    fun clearProfile() {
        statsListener?.remove()
        statsListener = null
        uiState = ProfileUiState()
    }

    override fun onCleared() {
        statsListener?.remove()
        super.onCleared()
    }
}
