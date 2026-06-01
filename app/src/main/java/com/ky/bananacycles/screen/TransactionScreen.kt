package com.ky.bananacycles.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ky.bananacycles.repository.TransactionRepository

@Composable
fun TransactionScreen() {

    val transactions =
        TransactionRepository.transactionList

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Transaksi Saya",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        if (transactions.isEmpty()) {

            Text(
                text = "Belum ada transaksi"
            )

        } else {

            LazyColumn {

                items(transactions) { transaction ->

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 4.dp
                        )
                    ) {

                        Column(
                            modifier = Modifier.padding(
                                16.dp
                            )
                        ) {

                            Text(
                                text = transaction.wasteName
                            )

                            Spacer(
                                modifier = Modifier.height(
                                    4.dp
                                )
                            )

                            Text(
                                text =
                                    "Rp ${transaction.price}"
                            )

                            Spacer(
                                modifier = Modifier.height(
                                    4.dp
                                )
                            )

                            Text(
                                text =
                                    "Status: ${transaction.status}"
                            )

                            Spacer(
                                modifier = Modifier.height(
                                    12.dp
                                )
                            )

                            if (
                                transaction.status != "Selesai"
                            ) {

                                Button(
                                    onClick = {

                                        TransactionRepository
                                            .updateStatus(
                                                transaction
                                            )

                                    }
                                ) {

                                    Text(
                                        "Update Status"
                                    )

                                }

                            }

                        }

                    }

                }

            }

        }

    }

}