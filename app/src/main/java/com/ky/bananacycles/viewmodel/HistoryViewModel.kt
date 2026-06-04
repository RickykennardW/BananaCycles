package com.ky.bananacycles.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.ky.bananacycles.model.Order
import com.ky.bananacycles.repository.HistoryRepository

data class HistoryUiState(
    val purchaseHistory: List<Order> = emptyList(),
    val salesHistory: List<Order> = emptyList(),
    val isPurchaseHistoryLoading: Boolean = false,
    val isSalesHistoryLoading: Boolean = false,
    val errorMessage: String? = null
)

class HistoryViewModel(
    private val repository: HistoryRepository = HistoryRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {
    var uiState by mutableStateOf(HistoryUiState())
        private set

    private var purchaseHistoryListener: ListenerRegistration? = null
    private var salesHistoryListener: ListenerRegistration? = null

    fun listenHistory() {
        val currentUserId = auth.currentUser?.uid

        if (currentUserId.isNullOrBlank()) {
            uiState = uiState.copy(errorMessage = "User is not signed in.")
            return
        }

        purchaseHistoryListener?.remove()
        salesHistoryListener?.remove()
        uiState = uiState.copy(
            isPurchaseHistoryLoading = true,
            isSalesHistoryLoading = true,
            errorMessage = null
        )

        purchaseHistoryListener = repository.listenPurchaseHistory(
            buyerId = currentUserId,
            onDataChanged = { history ->
                uiState = uiState.copy(
                    purchaseHistory = history,
                    isPurchaseHistoryLoading = false
                )
            },
            onError = { error ->
                uiState = uiState.copy(
                    isPurchaseHistoryLoading = false,
                    errorMessage = error.localizedMessage ?: "Failed to load purchase history."
                )
            }
        )

        salesHistoryListener = repository.listenSalesHistory(
            sellerId = currentUserId,
            onDataChanged = { history ->
                uiState = uiState.copy(
                    salesHistory = history,
                    isSalesHistoryLoading = false
                )
            },
            onError = { error ->
                uiState = uiState.copy(
                    isSalesHistoryLoading = false,
                    errorMessage = error.localizedMessage ?: "Failed to load sales history."
                )
            }
        )
    }

    fun clearHistory() {
        purchaseHistoryListener?.remove()
        salesHistoryListener?.remove()
        purchaseHistoryListener = null
        salesHistoryListener = null
        uiState = HistoryUiState()
    }

    override fun onCleared() {
        purchaseHistoryListener?.remove()
        salesHistoryListener?.remove()
        super.onCleared()
    }
}
