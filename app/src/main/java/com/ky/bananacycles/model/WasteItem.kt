package com.ky.bananacycles.model

data class WasteItem(

    val id: String = "",

    val sellerId: String = "",

    val wasteName: String = "",

    val category: String = "",

    val weight: Double = 0.0,

    val estimatedPrice: Int = 0,

    val createdAt: Long = 0L

)
