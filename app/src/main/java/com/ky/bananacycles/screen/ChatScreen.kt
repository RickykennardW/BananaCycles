package com.ky.bananacycles.screen

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ky.bananacycles.component.UserAvatar
import com.ky.bananacycles.model.ChatMessage
import com.ky.bananacycles.model.ChatRoom
import com.ky.bananacycles.model.MessageStatus
import com.ky.bananacycles.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onChatClick: (String) -> Unit
) {
    val uiState = viewModel.uiState
    val currentUserId = viewModel.currentUserId
    val context = LocalContext.current
    var searchQuery by remember {
        mutableStateOf("")
    }
    var chatPendingDelete by remember {
        mutableStateOf<ChatRoom?>(null)
    }

    chatPendingDelete?.let { room ->
        AlertDialog(
            onDismissRequest = {
                chatPendingDelete = null
            },
            title = {
                Text("Delete Chat")
            },
            text = {
                Text("This chat will be removed from your chat list only.")
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        viewModel.deleteChatForCurrentUser(
                            chatId = room.id,
                            onFailure = { message ->
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        )
                        chatPendingDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        chatPendingDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        viewModel.listenInbox()
    }

    val filteredRooms = uiState.chatRooms.filter { room ->
        room.otherParticipantName(currentUserId).contains(searchQuery, ignoreCase = true) ||
            room.listingName.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Chats",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Search chats")
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        uiState.errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        when {
            uiState.isInboxLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            filteredRooms.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Avatar()
                        Text(
                            text = "No chats yet",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Open a listing and start a conversation with the seller.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = filteredRooms,
                        key = { room -> room.id }
                    ) { room ->
                        ChatRoomCard(
                            room = room,
                            currentUserId = currentUserId,
                            onClick = {
                                viewModel.markChatAsRead(room.id)
                                onChatClick(room.id)
                            },
                            onDelete = {
                                chatPendingDelete = room
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    chatId: String,
    chatRoom: ChatRoom?,
    viewModel: ChatViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState = viewModel.uiState
    val currentUserId = viewModel.currentUserId
    val listState = rememberLazyListState()
    var messageText by remember {
        mutableStateOf("")
    }
    var messagePendingDelete by remember {
        mutableStateOf<ChatMessage?>(null)
    }

    messagePendingDelete?.let { message ->
        AlertDialog(
            onDismissRequest = {
                messagePendingDelete = null
            },
            title = {
                Text("Delete Message")
            },
            text = {
                Text("This message will be replaced with \"This message was deleted\" for everyone.")
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        viewModel.deleteOwnMessage(
                            chatId = chatId,
                            message = message,
                            onFailure = { error ->
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            }
                        )
                        messagePendingDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        messagePendingDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    LaunchedEffect(chatId) {
        viewModel.listenMessages(chatId)
    }

    LaunchedEffect(uiState.selectedMessages.size) {
        if (uiState.selectedMessages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.selectedMessages.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = chatRoom?.otherParticipantName(currentUserId) ?: "Chat",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        chatRoom?.listingName?.takeIf { it.isNotBlank() }?.let { listingName ->
                            Text(
                                text = listingName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.clearMessages()
                            onBack()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (uiState.isMessagesLoading || chatRoom == null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = uiState.selectedMessages,
                        key = { _, message -> message.id }
                    ) { index, message ->
                        val previousMessage = uiState.selectedMessages.getOrNull(index - 1)
                        val isMine = message.senderId == currentUserId
                        val startsNewSenderGroup = previousMessage?.senderId != message.senderId

                        MessageBubble(
                            message = message,
                            isMine = isMine,
                            showSenderHeader = !isMine && startsNewSenderGroup,
                            senderName = chatRoom.participantNames[message.senderId]
                                ?: chatRoom.otherParticipantName(currentUserId),
                            senderPhotoUrl = chatRoom.participantPhotoUrls[message.senderId].orEmpty(),
                            onDeleteRequest = {
                                messagePendingDelete = message
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = {
                        messageText = it
                    },
                    modifier = Modifier.weight(1f),
                    label = {
                        Text("Type message")
                    },
                    singleLine = true
                )

                IconButton(
                    onClick = {
                        val room = chatRoom ?: return@IconButton
                        viewModel.sendMessage(
                            chatRoom = room,
                            message = messageText,
                            onFailure = { message ->
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        )
                        messageText = ""
                    },
                    enabled = messageText.isNotBlank() && !uiState.isSending && chatRoom != null
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send"
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun ChatRoomCard(
    room: ChatRoom,
    currentUserId: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val unreadCount = room.unreadCounts[currentUserId] ?: 0
    var isMenuOpen by remember {
        mutableStateOf(false)
    }

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        isMenuOpen = true
                    }
                ),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Avatar(photoUrl = room.otherParticipantPhotoUrl(currentUserId))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = room.otherParticipantName(currentUserId),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = room.lastMessage.ifBlank { "Chat about ${room.listingName}" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = room.lastMessageTime.toChatTime(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (unreadCount > 0) {
                        UnreadBadge(count = unreadCount)
                    }
                }
            }
        }

        DropdownMenu(
            expanded = isMenuOpen,
            onDismissRequest = {
                isMenuOpen = false
            }
        ) {
            DropdownMenuItem(
                text = {
                    Text("Delete Chat")
                },
                onClick = {
                    isMenuOpen = false
                    onDelete()
                }
            )
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun MessageBubble(
    message: ChatMessage,
    isMine: Boolean,
    showSenderHeader: Boolean,
    senderName: String,
    senderPhotoUrl: String,
    onDeleteRequest: () -> Unit
) {
    var isMenuOpen by remember {
        mutableStateOf(false)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = {
                    if (isMine && !message.isDeleted) {
                        isMenuOpen = true
                    }
                }
            ),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        if (!isMine) {
            if (showSenderHeader) {
                Avatar(
                    photoUrl = senderPhotoUrl,
                    size = 36.dp
                )
            } else {
                Spacer(modifier = Modifier.width(36.dp))
            }

            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 292.dp)
        ) {
            if (!isMine && showSenderHeader) {
                Text(
                    text = senderName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isMine) 18.dp else 4.dp,
                            bottomEnd = if (isMine) 4.dp else 18.dp
                        )
                    )
                    .background(
                        if (message.isDeleted) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else if (isMine) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
                ) {
                    Text(
                        text = message.message,
                        color = if (message.isDeleted) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                        } else if (isMine) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = if (message.isDeleted) FontStyle.Italic else FontStyle.Normal
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = message.timestamp.toChatTime(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isMine) {
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                            }
                        )

                        if (isMine && !message.isDeleted) {
                            ReadReceipt(
                                message = message
                            )
                        }
                    }
                }
            }

            DropdownMenu(
                expanded = isMenuOpen,
                onDismissRequest = {
                    isMenuOpen = false
                }
            ) {
                DropdownMenuItem(
                    text = {
                        Text("Delete Message")
                    },
                    onClick = {
                        isMenuOpen = false
                        onDeleteRequest()
                    }
                )
            }
        }
    }
}

@Composable
private fun Avatar(
    photoUrl: String = "",
    size: Dp = 48.dp
) {
    UserAvatar(
        photoUrl = photoUrl,
        size = size
    )
}

@Composable
private fun ReadReceipt(
    message: ChatMessage
) {
    val isRead = message.status == MessageStatus.READ.name || message.readAt > 0L
    val receiptText = when {
        isRead -> "✓✓"
        message.status == MessageStatus.DELIVERED.name || message.timestamp > 0L -> "✓✓"
        else -> "✓"
    }

    Text(
        text = receiptText,
        style = MaterialTheme.typography.labelSmall,
        color = if (isRead) {
            Color(0xFF40A9FF)
        } else {
            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f)
        },
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun UnreadBadge(count: Int) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = count.coerceAtMost(99).toString(),
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun ChatRoom.otherParticipantName(currentUserId: String): String {
    val otherUserId = participants.firstOrNull { userId -> userId != currentUserId }.orEmpty()
    return participantNames[otherUserId].orEmpty().ifBlank { "BananaCycles User" }
}

private fun ChatRoom.otherParticipantPhotoUrl(currentUserId: String): String {
    val otherUserId = participants.firstOrNull { userId -> userId != currentUserId }.orEmpty()
    return participantPhotoUrls[otherUserId].orEmpty()
}

private fun Long.toChatTime(): String {
    if (this <= 0L) {
        return ""
    }

    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(this))
}
