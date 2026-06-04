package com.ky.bananacycles.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ky.bananacycles.component.ListingImage
import com.ky.bananacycles.model.Order
import com.ky.bananacycles.model.OrderStatus
import com.ky.bananacycles.ui.theme.BananaCyclesTheme
import com.ky.bananacycles.viewmodel.OrderViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class OrderTab(
    val label: String,
    val subtitle: String
) {
    PURCHASES("Purchases", "Orders I have placed"),
    SALES("Sales", "Orders placed for my listings")
}

@Composable
fun TransactionScreen(
    viewModel: OrderViewModel
) {
    val context = LocalContext.current
    val uiState = viewModel.uiState
    var selectedTab by remember {
        mutableStateOf(OrderTab.PURCHASES)
    }
    var orderPendingCancel by remember {
        mutableStateOf<Order?>(null)
    }

    orderPendingCancel?.let { order ->
        AlertDialog(
            onDismissRequest = {
                orderPendingCancel = null
            },
            title = {
                Text("Cancel Order")
            },
            text = {
                Text("Are you sure you want to cancel this order? This action will return the reserved stock back to your inventory.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.cancelOrder(
                            order = order,
                            onFailure = { message ->
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        )
                        orderPendingCancel = null
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        orderPendingCancel = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        viewModel.listenOrders()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Orders",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OrderTab.entries.forEach { tab ->
                    val selected = selectedTab == tab
                    if (selected) {
                        Button(
                            onClick = {
                                selectedTab = tab
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(tab.label)
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                selectedTab = tab
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(tab.label)
                        }
                    }
                }
            }

            Text(
                text = selectedTab.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp)
            )

            uiState.errorMessage?.let { message ->
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val orders = if (selectedTab == OrderTab.PURCHASES) {
                uiState.purchases
            } else {
                uiState.sales
            }

            val isLoading = if (selectedTab == OrderTab.PURCHASES) {
                uiState.isPurchasesLoading
            } else {
                uiState.isSalesLoading
            }

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                orders.isEmpty() -> {
                    EmptyOrdersState(
                        text = if (selectedTab == OrderTab.PURCHASES) {
                            "No purchases yet."
                        } else {
                            "No sales yet."
                        }
                    )
                }

                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = orders,
                            key = { order -> order.orderId }
                        ) { order ->
                            OrderCard(
                                order = order,
                                isSalesMode = selectedTab == OrderTab.SALES,
                                isUpdating = uiState.isUpdatingStatus,
                                onUpdateStatus = {
                                    viewModel.updateStatus(
                                        order = order,
                                        onFailure = { message ->
                                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                },
                                onCancelOrder = {
                                    orderPendingCancel = order
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderCard(
    order: Order,
    isSalesMode: Boolean,
    isUpdating: Boolean,
    onUpdateStatus: () -> Unit,
    onCancelOrder: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                ListingImage(
                    imageUrl = order.productImage,
                    listingId = order.listingId,
                    sellerId = order.sellerId,
                    modifier = Modifier.size(78.dp),
                    height = 78.dp
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = order.productName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("${order.quantityPurchased.toQuantityText()} kg")
                    Text(
                        text = "Rp ${order.totalPrice.toRupiah()}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isSalesMode) {
                            "Buyer: ${order.buyerName}"
                        } else {
                            "Seller: ${order.sellerName}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = order.createdAt.toOrderDate(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (order.status == OrderStatus.CANCELLED.name) {
                        Text(
                            text = "Order Cancelled",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (isSalesMode && order.canBeCancelled()) {
                    IconButton(
                        onClick = onCancelOrder
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel Order",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (order.status == OrderStatus.CANCELLED.name) {
                CancelledStatus()
            } else {
                OrderProgressTracker(status = order.status)
            }

            if (isSalesMode && order.status !in listOf(OrderStatus.FINISHED.name, OrderStatus.CANCELLED.name)) {
                Button(
                    onClick = onUpdateStatus,
                    enabled = !isUpdating,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Update Status")
                }
            }
        }
    }
}

@Composable
private fun CancelledStatus() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Text(
            text = "Order Cancelled",
            modifier = Modifier.padding(12.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun OrderProgressTracker(
    status: String
) {
    val steps = OrderStatus.entries
    val currentIndex = steps.indexOfFirst { step -> step.name == status }.coerceAtLeast(0)

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.forEachIndexed { index, _ ->
                val active = index <= currentIndex
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(
                            if (active) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                )

                if (index < steps.lastIndex) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .background(
                                if (index < currentIndex) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            steps.forEach { step ->
                Text(
                    text = step.name.toOrderLabel(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (steps.indexOf(step) <= currentIndex) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyOrdersState(
    text: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.AutoMirrored.Filled.List,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

private fun Int.toRupiah(): String {
    return "%,d".format(this).replace(",", ".")
}

private fun Double.toQuantityText(): String {
    return if (this % 1.0 == 0.0) toInt().toString() else toString()
}

private fun Long.toOrderDate(): String {
    if (this <= 0L) return ""
    return SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(this))
}

private fun String.toOrderLabel(): String {
    return when (this) {
        OrderStatus.PACKING.name -> "Packing"
        OrderStatus.SENDING.name -> "Sending"
        OrderStatus.MEET.name -> "Meet"
        OrderStatus.FINISHED.name -> "Finish"
        OrderStatus.CANCELLED.name -> "Cancelled"
        else -> this
    }
}

private fun Order.canBeCancelled(): Boolean {
    return status in listOf(
        OrderStatus.PACKING.name,
        OrderStatus.SENDING.name,
        OrderStatus.MEET.name
    )
}

@Preview(showBackground = true)
@Composable
private fun TransactionScreenPreview() {
    BananaCyclesTheme {
        EmptyOrdersState(text = "No purchases yet.")
    }
}
