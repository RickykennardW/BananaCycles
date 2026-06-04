package com.ky.bananacycles.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ky.bananacycles.repository.ReviewRepository

@Composable
fun ReviewScreen() {

    val reviews =
        ReviewRepository.reviewList

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Reviews & Ratings",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        if (reviews.isEmpty()) {

            Text(
                text = "No reviews yet."
            )

        } else {

            LazyColumn {

                items(reviews) { review ->

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 4.dp
                        )
                    ) {

                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {

                            Text(
                                text = review.wasteName
                            )

                            Text(
                                text = "⭐ ${review.rating}/5"
                            )

                            Text(
                                text = review.comment
                            )

                        }

                    }

                }

            }

        }

    }

}
