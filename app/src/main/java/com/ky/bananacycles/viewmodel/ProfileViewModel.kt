package com.ky.bananacycles.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.StorageException
import com.ky.bananacycles.model.UserProfile
import com.ky.bananacycles.model.UserStats
import com.ky.bananacycles.repository.ProfileRepository

data class ProfileUiState(
    val profile: UserProfile = UserProfile(),
    val stats: UserStats = UserStats(),
    val isSaving: Boolean = false,
    val imageUploadProgress: Float? = null,
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
        imageMimeType: String?,
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
            imageUploadProgress = null,
            errorMessage = null
        )

        repository.updateProfile(
            userId = userId,
            displayName = displayName.trim(),
            imageUri = imageUri,
            imageMimeType = imageMimeType,
            onProgress = { progress ->
                uiState = uiState.copy(imageUploadProgress = progress)
            },
            onSuccess = {
                uiState = uiState.copy(
                    isSaving = false,
                    imageUploadProgress = null
                )
                onSuccess()
            },
            onFailure = { error ->
                val message = error.toUserFacingMessage("Failed to update profile.")
                uiState = uiState.copy(
                    isSaving = false,
                    imageUploadProgress = null,
                    errorMessage = message
                )
                onFailure(message)
            }
        )
    }

    private fun Exception.toUserFacingMessage(fallbackMessage: String): String {
        return if (this is StorageException) {
            when (errorCode) {
                StorageException.ERROR_OBJECT_NOT_FOUND -> {
                    "Profile image upload failed because Firebase Storage could not find the uploaded object. Please retry."
                }
                StorageException.ERROR_NOT_AUTHENTICATED -> {
                    "Please log in again before uploading a profile image."
                }
                StorageException.ERROR_NOT_AUTHORIZED -> {
                    "Profile image upload is blocked by Firebase Storage rules."
                }
                StorageException.ERROR_RETRY_LIMIT_EXCEEDED -> {
                    "Profile image upload timed out. Please check your connection and retry."
                }
                else -> localizedMessage ?: fallbackMessage
            }
        } else {
            localizedMessage ?: fallbackMessage
        }
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
