package com.ky.bananacycles

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ky.bananacycles.screen.UploadWasteScreen
import com.ky.bananacycles.ui.theme.BananaCyclesTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            BananaCyclesTheme {
                UploadWasteScreen()
            }
        }
    }
}