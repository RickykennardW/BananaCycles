package com.ky.bananacycles.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.ky.bananacycles.model.UserProfile
import com.ky.bananacycles.model.UserStats
import com.ky.bananacycles.repository.ProfileRepository

data class ProfileUiState(
    val profile: UserProfile = UserProfile(),
    val stats: UserStats = UserStats(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

class ProfileViewModel(
    private val repository: ProfileRepository = ProfileRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {
    var uiState by mutableStateOf(ProfileUiState())
        private set

    private var statsListener: ListenerRegistration? = null
    private var profileListener: ListenerRegistration? = null

    fun listenProfileStats() {
        val userId = auth.currentUser?.uid

        if (userId.isNullOrBlank()) {
            uiState = uiState.copy(errorMessage = "User is not signed in.")
            return
        }

        profileListener?.remove()
        profileListener = repository.listenUserProfile(
            userId = userId,
            onDataChanged = { profile ->
                uiState = uiState.copy(
                    profile = profile,
                    errorMessage = null
                )
            },
            onError = { error ->
                uiState = uiState.copy(
                    errorMessage = error.localizedMessage ?: "Failed to load profile."
                )
            }
        )

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

    fun updateProfile(
        displayName: String,
        imageUri: Uri?,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val userId = auth.currentUser?.uid
        if (userId.isNullOrBlank()) {
            onFailure("User is not signed in.")
            return
        }

        if (displayName.isBlank()) {
            onFailure("Display name is required.")
            return
        }

        uiState = uiState.copy(
            isSaving = true,
            errorMessage = null
        )

        repository.updateProfile(
            userId = userId,
            displayName = displayName.trim(),
            imageUri = imageUri,
            onSuccess = {
                uiState = uiState.copy(isSaving = false)
                onSuccess()
            },
            onFailure = { error ->
                val message = error.localizedMessage ?: "Failed to update profile."
                uiState = uiState.copy(
                    isSaving = false,
                    errorMessage = message
                )
                onFailure(message)
            }
        )
    }

    fun clearProfile() {
        statsListener?.remove()
        profileListener?.remove()
        statsListener = null
        profileListener = null
        uiState = ProfileUiState()
    }

    override fun onCleared() {
        statsListener?.remove()
        profileListener?.remove()
        super.onCleared()
    }
}
