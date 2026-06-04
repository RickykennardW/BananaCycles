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

            "Pending" -> "Picked Up"

            "Picked Up" -> "Completed"

            else -> "Completed"
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

    fun getTotalTransaction(): Int {

        return transactionList.size

    }

    fun getTotalIncome(): Int {

        return transactionList.sumOf {
            it.price
        }

    }

    fun getCompletedTransaction(): Int {

        return transactionList.count {
            it.status == "Completed"
        }

    }

}
