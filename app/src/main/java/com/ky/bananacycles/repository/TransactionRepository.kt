package com.ky.bananacycles.repository

import androidx.compose.runtime.mutableStateListOf
import com.ky.bananacycles.model.Transaction

object TransactionRepository {

    val transactionList = mutableStateListOf<Transaction>()

    fun addTransaction(
        transaction: Transaction
    ) {
        transactionList.add(transaction)
    }

    fun updateStatus(
        transaction: Transaction
    ) {

        transaction.status = when (
            transaction.status
        ) {

            "Pending" -> "Dijemput"

            "Dijemput" -> "Selesai"

            else -> "Selesai"
        }

        val index =
            transactionList.indexOf(transaction)

        if (index != -1) {

            transactionList[index] =
                transaction.copy(
                    status = transaction.status
                )

        }

    }

}