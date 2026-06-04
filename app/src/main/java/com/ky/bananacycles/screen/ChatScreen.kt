package com.ky.bananacycles.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ky.bananacycles.model.ChatMessage

@Composable
fun ChatScreen() {

    var messageText by remember {
        mutableStateOf("")
    }

    var chatMessages by remember {
        mutableStateOf(
            listOf<ChatMessage>()
        )
    }

    val firestore = remember {
        FirebaseFirestore.getInstance()
    }

    val auth = remember {
        FirebaseAuth.getInstance()
    }

    LaunchedEffect(Unit) {

        firestore
            .collection("chats")
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null) {

                    val loadedMessages =
                        snapshot.toObjects(
                            ChatMessage::class.java
                        )

                    chatMessages =
                        loadedMessages.sortedBy {
                            it.timestamp
                        }

                }

            }

    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Chat",
            style = MaterialTheme.typography.headlineMedium
        )

        if (chatMessages.isEmpty()) {

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "No messages yet.",
                    style = MaterialTheme.typography.headlineSmall
                )

                Text(
                    text = "Start your first conversation."
                )

            }

        } else {

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                items(chatMessages) { chat ->

                    val currentUserId =
                        auth.currentUser?.uid ?: ""

                    val isMyMessage =
                        chat.senderId == currentUserId

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            if (isMyMessage)
                                Arrangement.End
                            else
                                Arrangement.Start
                    ) {

                        Column(
                            horizontalAlignment =
                                if (isMyMessage)
                                    Alignment.End
                                else
                                    Alignment.Start
                        ) {

                            Text(
                                text = chat.senderName,
                                style = MaterialTheme.typography.labelMedium
                            )

                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor =
                                        if (isMyMessage)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else
                                            MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {

                                Text(
                                    text = chat.message,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodyLarge
                                )

                            }

                        }

                    }

                }

            }

        }

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {

            OutlinedTextField(
                value = messageText,
                onValueChange = {
                    messageText = it
                },
                label = {
                    Text("Type a message")
                },
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = {

                    if (messageText.isNotBlank()) {

                        val currentUser =
                            auth.currentUser

                        val newMessage = ChatMessage(
                            id = System.currentTimeMillis().toString(),
                            senderId = currentUser?.uid ?: "",
                            senderName =
                                currentUser?.displayName
                                    ?: currentUser?.email
                                    ?: "Anonymous",
                            message = messageText
                        )

                        firestore
                            .collection("chats")
                            .document(newMessage.id)
                            .set(newMessage)

                        messageText = ""

                    }

                },
                modifier = Modifier.padding(start = 8.dp)
            ) {

                Text("Send")

            }

        }

    }

}