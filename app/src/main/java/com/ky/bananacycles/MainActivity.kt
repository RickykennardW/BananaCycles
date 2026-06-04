package com.ky.bananacycles

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.ky.bananacycles.auth.LoginScreen
import com.ky.bananacycles.auth.RegisterScreen
import com.ky.bananacycles.model.WasteItem
import com.ky.bananacycles.screen.ChatDetailScreen
import com.ky.bananacycles.screen.ChatScreen
import com.ky.bananacycles.screen.MarketScreen
import com.ky.bananacycles.screen.ProfileScreen
import com.ky.bananacycles.screen.TransactionScreen
import com.ky.bananacycles.screen.UploadWasteScreen
import com.ky.bananacycles.screen.WasteDetailScreen
import com.ky.bananacycles.ui.theme.BananaCyclesTheme
import com.ky.bananacycles.viewmodel.ChatViewModel
import com.ky.bananacycles.viewmodel.WasteViewModel

private object BottomRoutes {
    const val MARKET = "market"
    const val SELL = "sell"
    const val CHAT = "chat"
    const val TRANSACTION = "transaction"
    const val PROFILE = "profile"
}

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
                    mutableStateOf(false)
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
                    val chatViewModel: ChatViewModel = viewModel()
                    val context = LocalContext.current

                    LaunchedEffect(Unit) {
                        chatViewModel.listenInbox()
                    }

                    // Bottom navigation uses explicit routes for the five primary app tabs.
                    var selectedRoute by remember {
                        mutableStateOf(BottomRoutes.MARKET)
                    }

                    var selectedWaste by remember {
                        mutableStateOf<WasteItem?>(null)
                    }

                    var selectedChatId by remember {
                        mutableStateOf<String?>(null)
                    }

                    val totalUnreadChats = chatViewModel.uiState.totalUnreadFor(
                        chatViewModel.currentUserId
                    )

                    Scaffold(
                        bottomBar = {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = selectedRoute == BottomRoutes.MARKET,
                                    onClick = {
                                        selectedRoute = BottomRoutes.MARKET
                                        selectedWaste = null
                                        selectedChatId = null
                                    },
                                    icon = {
                                        Icon(
                                            Icons.Default.Home,
                                            contentDescription = null
                                        )
                                    },
                                    label = {
                                        BottomNavigationLabel("Market")
                                    }
                                )

                                NavigationBarItem(
                                    selected = selectedRoute == BottomRoutes.SELL,
                                    onClick = {
                                        selectedRoute = BottomRoutes.SELL
                                        selectedWaste = null
                                        selectedChatId = null
                                    },
                                    icon = {
                                        Icon(
                                            Icons.Default.AddCircle,
                                            contentDescription = null
                                        )
                                    },
                                    label = {
                                        BottomNavigationLabel("Sell")
                                    }
                                )

                                NavigationBarItem(
                                    selected = selectedRoute == BottomRoutes.CHAT,
                                    onClick = {
                                        selectedRoute = BottomRoutes.CHAT
                                        selectedWaste = null
                                        selectedChatId = null
                                    },
                                    icon = {
                                        ChatNavigationIcon(
                                            hasUnreadMessages = totalUnreadChats > 0
                                        )
                                    },
                                    label = {
                                        BottomNavigationLabel("Chat")
                                    }
                                )

                                NavigationBarItem(
                                    selected = selectedRoute == BottomRoutes.TRANSACTION,
                                    onClick = {
                                        selectedRoute = BottomRoutes.TRANSACTION
                                        selectedWaste = null
                                        selectedChatId = null
                                    },
                                    icon = {
                                        Icon(
                                            Icons.AutoMirrored.Filled.List,
                                            contentDescription = null
                                        )
                                    },
                                    label = {
                                        BottomNavigationLabel("Orders")
                                    }
                                )

                                NavigationBarItem(
                                    selected = selectedRoute == BottomRoutes.PROFILE,
                                    onClick = {
                                        selectedRoute = BottomRoutes.PROFILE
                                        selectedWaste = null
                                        selectedChatId = null
                                    },
                                    icon = {
                                        Icon(
                                            Icons.Default.AccountCircle,
                                            contentDescription = null
                                        )
                                    },
                                    label = {
                                        BottomNavigationLabel("Profile")
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
                                        viewModel = wasteViewModel,
                                        onBack = {
                                            selectedWaste = null
                                        },
                                        onChatSeller = { listing ->
                                            chatViewModel.startChatWithSeller(
                                                listing = listing,
                                                onSuccess = { chatId ->
                                                    selectedRoute = BottomRoutes.CHAT
                                                    selectedWaste = null
                                                    selectedChatId = chatId
                                                },
                                                onFailure = { message ->
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        message,
                                                        android.widget.Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            )
                                        }
                                    )
                                }

                                selectedChatId != null -> {
                                    val chatRoom = chatViewModel.uiState.chatRooms.firstOrNull { room ->
                                        room.id == selectedChatId
                                    }

                                    ChatDetailScreen(
                                        chatId = selectedChatId!!,
                                        chatRoom = chatRoom,
                                        viewModel = chatViewModel,
                                        onBack = {
                                            selectedChatId = null
                                        }
                                    )
                                }

                                selectedRoute == BottomRoutes.MARKET -> {
                                    MarketScreen(
                                        viewModel = wasteViewModel,
                                        onWasteClick = { waste ->
                                            selectedWaste = waste
                                        }
                                    )
                                }

                                selectedRoute == BottomRoutes.CHAT -> {
                                    ChatScreen(
                                        viewModel = chatViewModel,
                                        onChatClick = { chatId ->
                                            selectedChatId = chatId
                                        }
                                    )
                                }

                                selectedRoute == BottomRoutes.SELL -> {
                                    UploadWasteScreen(
                                        viewModel = wasteViewModel
                                    )
                                }

                                selectedRoute == BottomRoutes.TRANSACTION -> {
                                    TransactionScreen()
                                }

                                selectedRoute == BottomRoutes.PROFILE -> {
                                    ProfileScreen()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatNavigationIcon(
    hasUnreadMessages: Boolean
) {
    if (hasUnreadMessages) {
        BadgedBox(
            badge = {
                Badge(
                    modifier = Modifier.size(8.dp),
                    containerColor = Color(0xFF0B7DFF)
                )
            }
        ) {
            Icon(
                Icons.Default.Email,
                contentDescription = null
            )
        }
    } else {
        Icon(
            Icons.Default.Email,
            contentDescription = null
        )
    }
}

@Composable
private fun BottomNavigationLabel(text: String) {
    Text(
        text = text,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip
    )
}
