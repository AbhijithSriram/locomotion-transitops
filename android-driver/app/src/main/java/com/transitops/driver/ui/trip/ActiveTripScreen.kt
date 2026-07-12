package com.transitops.driver.ui.trip

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.UUID

/**
 * The Active Trip Screen.
 * Displays the current trip assignment to the driver. 
 * Allows the driver to mark the trip as "Complete".
 * 
 * In a fully wired app, this Compose screen would observe a ViewModel that holds
 * StateFlows reflecting the CachedTrip from Room.
 */
@Composable
fun ActiveTripScreen(onLogout: () -> Unit) {
    // For this UI mockup, we are simulating a trip being present
    var isTripCompleted by remember { mutableStateOf(false) }

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

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        // In reality, this onClick would call viewModel.completeTrip(finalOdometer = 15020)
                        // which would write a TRIP_COMPLETE OutboxAction to Room with a UUID
                        // val uuid = UUID.randomUUID().toString()
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
                        // This would open the Incident Reporting dialog
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
