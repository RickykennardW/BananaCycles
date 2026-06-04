package com.ky.bananacycles.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.ky.bananacycles.model.Order
import com.ky.bananacycles.repository.OrderRepository

data class OrderUiState(
    val purchases: List<Order> = emptyList(),
    val sales: List<Order> = emptyList(),
    val isPurchasesLoading: Boolean = false,
    val isSalesLoading: Boolean = false,
    val isUpdatingStatus: Boolean = false,
    val errorMessage: String? = null
)

class OrderViewModel(
    private val repository: OrderRepository = OrderRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    var uiState by mutableStateOf(OrderUiState())
        private set

    private var purchasesListener: ListenerRegistration? = null
    private var salesListener: ListenerRegistration? = null

    fun listenOrders() {
        val currentUserId = auth.currentUser?.uid

        if (currentUserId.isNullOrBlank()) {
            uiState = uiState.copy(
                purchases = emptyList(),
                sales = emptyList(),
                isPurchasesLoading = false,
                isSalesLoading = false,
                errorMessage = "User is not signed in."
            )
            return
        }

        purchasesListener?.remove()
        salesListener?.remove()

        uiState = uiState.copy(
            isPurchasesLoading = true,
            isSalesLoading = true,
            errorMessage = null
        )

        purchasesListener = repository.listenPurchases(
            buyerId = currentUserId,
            onDataChanged = { orders ->
                uiState = uiState.copy(
                    purchases = orders,
                    isPurchasesLoading = false,
                    errorMessage = null
                )
            },
            onError = { error ->
                uiState = uiState.copy(
                    isPurchasesLoading = false,
                    errorMessage = error.localizedMessage ?: "Failed to load purchases."
                )
            }
        )

        salesListener = repository.listenSales(
            sellerId = currentUserId,
            onDataChanged = { orders ->
                uiState = uiState.copy(
                    sales = orders,
                    isSalesLoading = false,
                    errorMessage = null
                )
            },
            onError = { error ->
                uiState = uiState.copy(
                    isSalesLoading = false,
                    errorMessage = error.localizedMessage ?: "Failed to load sales."
                )
            }
        )
    }

    fun updateStatus(
        order: Order,
        onFailure: (String) -> Unit
    ) {
        uiState = uiState.copy(
            isUpdatingStatus = true,
            errorMessage = null
        )

        repository.updateOrderStatus(
            order = order,
            onSuccess = {
                uiState = uiState.copy(isUpdatingStatus = false)
            },
            onFailure = { error ->
                val message = error.localizedMessage ?: "Failed to update order status."
                uiState = uiState.copy(
                    isUpdatingStatus = false,
                    errorMessage = message
                )
                onFailure(message)
            }
        )
    }

    fun cancelOrder(
        order: Order,
        onFailure: (String) -> Unit
    ) {
        uiState = uiState.copy(
            isUpdatingStatus = true,
            errorMessage = null
        )

        repository.cancelOrder(
            order = order,
            onSuccess = {
                uiState = uiState.copy(isUpdatingStatus = false)
            },
            onFailure = { error ->
                val message = error.localizedMessage ?: "Failed to cancel order."
                uiState = uiState.copy(
                    isUpdatingStatus = false,
                    errorMessage = message
                )
                onFailure(message)
            }
        )
    }

    fun clearOrders() {
        purchasesListener?.remove()
        salesListener?.remove()
        purchasesListener = null
        salesListener = null
        uiState = OrderUiState()
    }

    override fun onCleared() {
        purchasesListener?.remove()
        salesListener?.remove()
        super.onCleared()
    }
}
