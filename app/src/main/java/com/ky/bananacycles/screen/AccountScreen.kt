package com.ky.bananacycles.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ky.bananacycles.ui.theme.BananaCyclesTheme

@Composable
fun AccountScreen() {

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = "Account Screen",
            style = MaterialTheme.typography.headlineMedium
        )

    }

}

@Preview(showBackground = true)
@Composable
fun AccountScreenPreview() {
    BananaCyclesTheme {
        AccountScreen()
    }
}