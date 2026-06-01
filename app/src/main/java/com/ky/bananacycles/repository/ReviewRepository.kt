package com.ky.bananacycles.repository

import androidx.compose.runtime.mutableStateListOf
import com.ky.bananacycles.model.Review

object ReviewRepository {

    val reviewList = mutableStateListOf<Review>()

    fun addReview(
        review: Review
    ) {
        reviewList.add(review)
    }

    fun getTotalReview(): Int {

        return reviewList.size

    }

}