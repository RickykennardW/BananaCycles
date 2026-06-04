package com.ky.bananacycles.model

data class UserProfile(
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val totalSalesCompleted: Int = 0,
    val totalPurchasesCompleted: Int = 0
)
