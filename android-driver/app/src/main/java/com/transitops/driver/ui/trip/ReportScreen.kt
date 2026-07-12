package com.transitops.driver.ui.trip

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Screen for logging fuel or reporting an incident.
 * Uses TripViewModel to write to the local outbox (offline-safe).
 * vehicleId and tripId are read from the ViewModel's active trip state —
 * no hardcoded IDs here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    viewModel: TripViewModel,
    onNavigateBack: () -> Unit
) {
    var isFuelLog by remember { mutableStateOf(true) }
    var submitted by remember { mutableStateOf(false) }

    // Fuel state
    var liters by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var odometer by remember { mutableStateOf("") }

    // Incident state
    var description by remember { mutableStateOf("") }
    var severity by remember { mutableStateOf("LOW") }
    var expanded by remember { mutableStateOf(false) }

    if (submitted) {
        LaunchedEffect(Unit) { onNavigateBack() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log Fuel / Report Incident") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tab selector
            TabRow(selectedTabIndex = if (isFuelLog) 0 else 1) {
                Tab(selected = isFuelLog, onClick = { isFuelLog = true }, text = { Text("Fuel Log") })
                Tab(selected = !isFuelLog, onClick = { isFuelLog = false }, text = { Text("Incident Report") })
            }

            if (isFuelLog) {
                // Fuel Log Form
                OutlinedTextField(
                    value = liters,
                    onValueChange = { liters = it },
                    label = { Text("Liters Filled") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = cost,
                    onValueChange = { cost = it },
                    label = { Text("Total Cost (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = odometer,
                    onValueChange = { odometer = it },
                    label = { Text("Current Odometer (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // Incident Report Form
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Describe the incident") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = severity,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Severity") },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        listOf("LOW", "MEDIUM", "HIGH").forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = { severity = option; expanded = false }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            val isSubmitEnabled = if (isFuelLog) {
                liters.toDoubleOrNull() != null && cost.toDoubleOrNull() != null && odometer.toDoubleOrNull() != null
            } else {
                description.isNotBlank()
            }

            Button(
                onClick = {
                    if (isFuelLog) {
                        viewModel.logFuel(
                            liters = liters.toDouble(),
                            cost = cost.toDouble()
                        )
                        val odoValue = odometer.toDoubleOrNull()
                        if (odoValue != null) {
                            viewModel.updateOdometer(odoValue)
                        }
                    } else {
                        viewModel.reportIncident(
                            description = description
                        )
                    }
                    submitted = true
                },
                enabled = isSubmitEnabled,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(if (isFuelLog) "Save Fuel Log" else "Report Incident")
            }
        }
    }
}
