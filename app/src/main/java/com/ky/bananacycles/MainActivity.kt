package com.ky.bananacycles

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.ky.bananacycles.auth.LoginScreen
import com.ky.bananacycles.auth.RegisterScreen
import com.ky.bananacycles.model.WasteItem
import com.ky.bananacycles.screen.AccountScreen
import com.ky.bananacycles.screen.MarketScreen
import com.ky.bananacycles.screen.UploadWasteScreen
import com.ky.bananacycles.screen.WasteDetailScreen
import com.ky.bananacycles.ui.theme.BananaCyclesTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {

            BananaCyclesTheme {

                var isLoggedIn by remember {
                    mutableStateOf(false)
                }

                var showRegister by remember {
                    mutableStateOf(false)
                }

                if (!isLoggedIn) {

                    if (showRegister) {

                        RegisterScreen(
                            onBackToLogin = {
                                showRegister = false
                            }
                        )

                    } else {

                        LoginScreen(
                            onLoginSuccess = {
                                isLoggedIn = true
                            },
                            onRegisterClick = {
                                showRegister = true
                            }
                        )

                    }

                } else {

                    var selectedTab by remember {
                        mutableStateOf(0)
                    }

                    var selectedWaste by remember {
                        mutableStateOf<WasteItem?>(null)
                    }

                    Scaffold(

                        bottomBar = {

                            NavigationBar {

                                NavigationBarItem(
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 },
                                    icon = {
                                        Icon(
                                            Icons.Default.Home,
                                            contentDescription = null
                                        )
                                    },
                                    label = {
                                        Text("Market")
                                    }
                                )

                                NavigationBarItem(
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 },
                                    icon = {
                                        Icon(
                                            Icons.Default.ShoppingCart,
                                            contentDescription = null
                                        )
                                    },
                                    label = {
                                        Text("Sell")
                                    }
                                )

                                NavigationBarItem(
                                    selected = selectedTab == 2,
                                    onClick = { selectedTab = 2 },
                                    icon = {
                                        Icon(
                                            Icons.Default.AccountCircle,
                                            contentDescription = null
                                        )
                                    },
                                    label = {
                                        Text("Account")
                                    }
                                )

                            }

                        }

                    ) { paddingValues ->

                        Surface(
                            modifier = Modifier.padding(
                                paddingValues
                            )
                        ) {

                            when {

                                selectedWaste != null -> {

                                    WasteDetailScreen(
                                        wasteItem = selectedWaste!!,
                                        onBack = {
                                            selectedWaste = null
                                        }
                                    )

                                }

                                selectedTab == 0 -> {

                                    MarketScreen(
                                        onWasteClick = { waste ->
                                            selectedWaste = waste
                                        }
                                    )

                                }

                                selectedTab == 1 -> {

                                    UploadWasteScreen()

                                }

                                selectedTab == 2 -> {

                                    AccountScreen()

                                }

                            }

                        }

                    }

                }

            }

        }

    }

}