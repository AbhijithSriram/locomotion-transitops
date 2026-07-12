package com.transitops.driver.ui.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.transitops.driver.ui.trip.TripViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(
    gemmaViewModel: GemmaViewModel,
    tripViewModel: TripViewModel,
    onNavigateBack: () -> Unit
) {
    val messages by gemmaViewModel.messages.collectAsState()
    val isProcessing by gemmaViewModel.isProcessing.collectAsState()
    val isReady by gemmaViewModel.isReady.collectAsState()
    val activeTrip by tripViewModel.activeTrip.collectAsState()

    var inputText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("AI Assistant")
                        if (activeTrip != null) {
                            Text("Trip #${activeTrip?.tripId}", style = MaterialTheme.typography.labelSmall)
                        } else {
                            Text("No active trip", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Status banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isReady) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    if (isReady) "🟢 Gemma 2B · On-device" else "🔴 Model not loaded",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isReady) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
            }

            // Message list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(message)
                }
                if (isProcessing) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                            Card(
                                shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                PaddingValues(12.dp)
                                CircularProgressIndicator(
                                    modifier = Modifier.padding(12.dp).size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                }
            }

            // Input field
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...") },
                    enabled = isReady && !isProcessing
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        gemmaViewModel.sendMessage(inputText, tripViewModel)
                        inputText = ""
                    },
                    enabled = inputText.isNotBlank() && isReady && !isProcessing,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send")
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            shape = if (message.isFromUser)
                RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp)
            else
                RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isFromUser)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.secondaryContainer,
                contentColor = if (message.isFromUser)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
