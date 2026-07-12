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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    tripId: String,
    vehicleId: String,
    viewModel: TripViewModel,
    onNavigateBack: () -> Unit
) {
    var isFuelLog by remember { mutableStateOf(true) }

    // Fuel State
    var liters by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var odometer by remember { mutableStateOf("") }

    // Incident State
    var description by remember { mutableStateOf("") }
    var severity by remember { mutableStateOf("LOW") }
    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Report Issue or Log Fuel") },
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
            // Tab row to select Fuel vs Incident
            TabRow(selectedTabIndex = if (isFuelLog) 0 else 1) {
                Tab(
                    selected = isFuelLog,
                    onClick = { isFuelLog = true },
                    text = { Text("Fuel Log") }
                )
                Tab(
                    selected = !isFuelLog,
                    onClick = { isFuelLog = false },
                    text = { Text("Incident Report") }
                )
            }

            if (isFuelLog) {
                // Fuel Log Form
                OutlinedTextField(
                    value = liters,
                    onValueChange = { liters = it },
                    label = { Text("Liters Filled") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = cost,
                    onValueChange = { cost = it },
                    label = { Text("Total Cost") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = odometer,
                    onValueChange = { odometer = it },
                    label = { Text("Odometer Reading") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // Incident Form
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Incident Description") },
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
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        listOf("LOW", "MEDIUM", "HIGH", "CRITICAL").forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    severity = selectionOption
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (isFuelLog) {
                        viewModel.logFuel(
                            vehicleId = vehicleId,
                            liters = liters.toDoubleOrNull() ?: 0.0,
                            cost = cost.toDoubleOrNull() ?: 0.0,
                            odometer = odometer.toDoubleOrNull() ?: 0.0
                        )
                    } else {
                        viewModel.reportIncident(
                            tripId = tripId,
                            vehicleId = vehicleId,
                            description = description,
                            severity = severity
                        )
                    }
                    onNavigateBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(if (isFuelLog) "Save Fuel Log" else "Report Incident")
            }
        }
    }
}
