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

}