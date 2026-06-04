package com.ky.bananacycles.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ky.bananacycles.repository.ReviewRepository
import com.ky.bananacycles.repository.TransactionRepository
import com.ky.bananacycles.ui.theme.BananaCyclesTheme

@Composable
fun AccountScreen(
    onLogout: () -> Unit = {}
) {

    val totalTransaction =
        TransactionRepository.getTotalTransaction()

    val totalIncome =
        TransactionRepository.getTotalIncome()

    val completedTransaction =
        TransactionRepository.getCompletedTransaction()

    val totalReview =
        ReviewRepository.getTotalReview()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),

        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text = "Dashboard Saya",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        DashboardCard(
            title = "Total Transaksi",
            value = totalTransaction.toString()
        )

        DashboardCard(
            title = "Total Pendapatan",
            value = "Rp $totalIncome"
        )

        DashboardCard(
            title = "Transaksi Selesai",
            value = completedTransaction.toString()
        )

        DashboardCard(
            title = "Total Review",
            value = totalReview.toString()
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Logout")
        }

    }

}

@Composable
fun DashboardCard(
    title: String,
    value: String
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall
            )

        }

    }

}

@Preview(showBackground = true)
@Composable
fun AccountScreenPreview() {

    BananaCyclesTheme {

        AccountScreen()

    }

}
