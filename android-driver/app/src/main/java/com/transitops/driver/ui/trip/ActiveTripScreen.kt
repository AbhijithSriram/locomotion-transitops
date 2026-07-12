package com.transitops.driver.ui.trip

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.Alignment
import java.util.UUID
import org.json.JSONObject

/**
 * The Active Trip Screen.
 * Displays the current trip assignment to the driver. 
 * Allows the driver to mark the trip as "Complete".
 * 
 * In a fully wired app, this Compose screen would observe a ViewModel that holds
 * StateFlows reflecting the CachedTrip from Room.
 */
@Composable
fun ActiveTripScreen(
    viewModel: TripViewModel,
    onLogout: () -> Unit,
    onNavigateToReport: () -> Unit
) {
    // For this UI mockup, we are simulating a trip being present
    var isTripCompleted by remember { mutableStateOf(false) }
    
    val outboxActions by viewModel.outboxActions.collectAsState()

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Active Trip") },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text("Logout", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isTripCompleted) {
                Text(
                    text = "Trip Completed! Synced to Outbox.",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text("Waiting for next dispatch...")
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "Trip #42", style = MaterialTheme.typography.headlineSmall)
                        Divider()
                        Text("From: Chennai Depot")
                        Text("To: Bangalore Hub")
                        Text("Cargo: 1200 Kg")
                        Text("Vehicle: TN-01-AB-1234 (VAN)")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Recent Activity", 
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                if (outboxActions.isEmpty()) {
                    Text("No offline actions recorded yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(outboxActions) { action ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (action.type == "FUEL_LOG") Icons.Default.Info else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (action.type == "FUEL_LOG") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = if (action.type == "FUEL_LOG") "Fuel Logged" else if (action.type == "INCIDENT_REPORT") "Incident Reported" else "Trip Complete", 
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        
                                        // Simple manual JSON parse for display
                                        val detailText = try {
                                            val json = JSONObject(action.payloadJson)
                                            if (action.type == "FUEL_LOG") {
                                                "${json.optDouble("liters", 0.0)} Liters • ODO: ${json.optDouble("odometer", 0.0)} KM"
                                            } else if (action.type == "INCIDENT_REPORT") {
                                                "Severity: ${json.optString("severity", "N/A")}"
                                            } else {
                                                "ID: ${json.optString("tripId", "")}"
                                            }
                                        } catch (e: Exception) {
                                            "Pending Sync..."
                                        }
                                        
                                        Text(text = detailText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.completeTrip(tripId = "trip-42")
                        isTripCompleted = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Complete Trip")
                }
                
                OutlinedButton(
                    onClick = {
                        onNavigateToReport()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Report Incident / Fuel")
                }
            }
        }
    }
}
