package com.ky.bananacycles

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.ky.bananacycles.auth.LoginScreen
import com.ky.bananacycles.auth.RegisterScreen
import com.ky.bananacycles.model.WasteItem
import com.ky.bananacycles.screen.AccountScreen
import com.ky.bananacycles.screen.MarketScreen
import com.ky.bananacycles.screen.TransactionScreen
import com.ky.bananacycles.screen.UploadWasteScreen
import com.ky.bananacycles.screen.WasteDetailScreen
import com.ky.bananacycles.ui.theme.BananaCyclesTheme
import com.ky.bananacycles.viewmodel.WasteViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BananaCyclesTheme {
                val auth = remember {
                    FirebaseAuth.getInstance()
                }

                var isLoggedIn by remember {
                    mutableStateOf(auth.currentUser != null)
                }

                var showRegister by remember {
                    mutableStateOf(false)
                }

                if (!isLoggedIn) {
                    if (showRegister) {
                        RegisterScreen(
                            onRegisterSuccess = {
                                isLoggedIn = true
                                showRegister = false
                            },
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
                    val wasteViewModel: WasteViewModel = viewModel()

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
                                    onClick = {
                                        selectedTab = 0
                                        selectedWaste = null
                                    },
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
                                    onClick = {
                                        selectedTab = 1
                                        selectedWaste = null
                                    },
                                    icon = {
                                        Icon(
                                            Icons.Default.ShoppingCart,
                                            contentDescription = null
                                        )
                                    },
                                    label = {
                                        Text("My Listings")
                                    }
                                )

                                NavigationBarItem(
                                    selected = selectedTab == 2,
                                    onClick = {
                                        selectedTab = 2
                                        selectedWaste = null
                                    },
                                    icon = {
                                        Icon(
                                            Icons.AutoMirrored.Filled.List,
                                            contentDescription = null
                                        )
                                    },
                                    label = {
                                        Text("Transaksi")
                                    }
                                )

                                NavigationBarItem(
                                    selected = selectedTab == 3,
                                    onClick = {
                                        selectedTab = 3
                                        selectedWaste = null
                                    },
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
                            modifier = Modifier.padding(paddingValues)
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
                                        viewModel = wasteViewModel,
                                        onWasteClick = { waste ->
                                            selectedWaste = waste
                                        }
                                    )
                                }

                                selectedTab == 1 -> {
                                    UploadWasteScreen(
                                        viewModel = wasteViewModel
                                    )
                                }

                                selectedTab == 2 -> {
                                    TransactionScreen()
                                }

                                selectedTab == 3 -> {
                                    AccountScreen(
                                        onLogout = {
                                            wasteViewModel.clearListings()
                                            auth.signOut()
                                            selectedTab = 0
                                            selectedWaste = null
                                            isLoggedIn = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
