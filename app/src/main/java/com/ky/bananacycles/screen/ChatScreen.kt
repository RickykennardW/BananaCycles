package com.ky.bananacycles.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Chat",
            style = MaterialTheme.typography.headlineMedium
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            items(chatMessages) { chat ->

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {

                        Text(
                            text = chat.senderName,
                            style = MaterialTheme.typography.titleSmall
                        )

                        Text(
                            text = chat.message
                        )

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

                        val newMessage = ChatMessage(
                            id = System.currentTimeMillis().toString(),
                            senderId = "local_user",
                            senderName = "You",
                            message = messageText
                        )

                        chatMessages =
                            chatMessages + newMessage

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