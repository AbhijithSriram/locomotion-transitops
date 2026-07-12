package com.transitops.driver.ui.trip

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.transitops.driver.data.auth.TokenProvider
import com.transitops.driver.data.remote.TripStatus
import org.json.JSONObject

/**
 * The Active Trip Screen.
 * Observes real data from TripViewModel:
 *  - Cached + live trip via GET /driver/me/active-trip (polled every 30s)
 *  - Outbox queue for the activity history section
 *  - Conflict messages surfaced as Snackbars
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveTripScreen(
    viewModel: TripViewModel,
    onLogout: () -> Unit,
    onNavigateToReport: () -> Unit,
    onNavigateToAssistant: () -> Unit
) {
    val activeTrip by viewModel.activeTrip.collectAsState()
    val isTripLoading by viewModel.isTripLoading.collectAsState()
    val outboxActions by viewModel.outboxActions.collectAsState()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Check network connectivity for the offline banner
    val isOnline by produceState(initialValue = true) {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val network = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(network)
        value = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    // Show conflict messages as Snackbars (one-shot events from SyncWorker)
    LaunchedEffect(Unit) {
        viewModel.conflictMessage.collect { message ->
            snackbarHostState.showSnackbar(
                message = "⚠ Sync conflict: $message",
                duration = SnackbarDuration.Long
            )
        }
    }

    // Refresh trip on first composition
    LaunchedEffect(Unit) {
        viewModel.loadActiveTrip()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("TransitOps Driver")
                        TokenProvider.driverName?.let {
                            Text(it, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                },
                actions = {
                    TextButton(onClick = {
                        viewModel // logout is handled by LoginViewModel
                        onLogout()
                    }) {
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
        ) {
            // ── Offline indicator banner ──────────────────────────────────────
            if (!isOnline) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFF3CD))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFF856404),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "Offline — actions are queued and will sync automatically",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF856404)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // ── Trip card ─────────────────────────────────────────────────
                when {
                    isTripLoading && activeTrip == null -> {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }

                    activeTrip == null -> {
                        // No active trip
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("No Trip Assigned", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    "Waiting for dispatch. The app checks every 30 seconds.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    activeTrip?.status == TripStatus.INTERRUPTED -> {
                        // Trip was interrupted (e.g., vehicle breakdown)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "⚠ Trip Interrupted",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                HorizontalDivider()
                                Text(
                                    "Trip #${activeTrip!!.tripId} has been interrupted. Please contact dispatch.",
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    "From: ${activeTrip!!.source.name}",
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    "To: ${activeTrip!!.destination.name}",
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    activeTrip?.status == TripStatus.COMPLETED || activeTrip?.status == TripStatus.CANCELLED -> {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    if (activeTrip?.status == TripStatus.COMPLETED) "✓ Trip Completed!" else "Trip Cancelled",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text("Waiting for next dispatch...")
                            }
                        }
                    }

                    else -> {
                        // DISPATCHED — main trip card with real data
                        val trip = activeTrip!!
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Trip #${trip.tripId}",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                HorizontalDivider()
                                InfoRow("From", trip.source.name)
                                InfoRow("To", trip.destination.name)
                                InfoRow("Cargo", "${trip.cargoWeightKg.toInt()} kg")
                                InfoRow("Vehicle", "${trip.vehicle.regNumber} (${trip.vehicle.type.name})")
                            }
                        }

                        // ── Outbox / Activity History ─────────────────────────
                        Text(
                            "Activity History",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (outboxActions.isEmpty()) {
                            Text(
                                "No offline actions recorded yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(outboxActions) { action ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (action.synced)
                                                MaterialTheme.colorScheme.surfaceVariant
                                            else
                                                MaterialTheme.colorScheme.secondaryContainer
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (action.type == "FUEL_LOG") Icons.Default.Info else Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = if (action.type == "FUEL_LOG")
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.error
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                val label = when (action.type) {
                                                    "FUEL_LOG" -> "Fuel Logged"
                                                    "INCIDENT_REPORT" -> "Incident Reported"
                                                    "TRIP_COMPLETE" -> "Trip Completed"
                                                    "ODOMETER_UPDATE" -> "Odometer Updated"
                                                    else -> action.type
                                                }
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(label, style = MaterialTheme.typography.titleSmall)
                                                    if (!action.synced) {
                                                        Text(
                                                            "• Pending sync",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.secondary,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }

                                                val detail = try {
                                                    val json = JSONObject(action.payloadJson)
                                                    when (action.type) {
                                                        "FUEL_LOG" -> "${json.optDouble("liters")} L - ₹${json.optDouble("cost").toInt()}"
                                                        "INCIDENT_REPORT" -> json.optString("description").take(30) + "..."
                                                        "TRIP_COMPLETE" -> "Trip #${json.optString("tripId")}"
                                                        "ODOMETER_UPDATE" -> "ODO: ${json.optDouble("odometer").toInt()} km"
                                                        else -> ""
                                                    }
                                                } catch (e: Exception) { "" }

                                                if (detail.isNotEmpty()) {
                                                    Text(
                                                        detail,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // ── Action buttons ────────────────────────────────────
                        Button(
                            onClick = { viewModel.completeTrip(trip.vehicle.odometer) },
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Text("Complete Trip")
                        }

                        OutlinedButton(
                            onClick = onNavigateToReport,
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Text("Report Incident / Log Fuel")
                        }

                        Button(
                            onClick = { onNavigateToAssistant() },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Text("AI Assistant (Gemma)")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
