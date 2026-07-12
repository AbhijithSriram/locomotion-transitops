package com.transitops.driver.data.ai

import com.transitops.driver.ui.trip.TripViewModel
import org.json.JSONObject

object McpToolDispatcher {

    fun dispatch(llmResponse: String, viewModel: TripViewModel): String {
        try {
            // Because on-device 2B models can sometimes hallucinate JSON syntax (missing braces, 
            // missing quotes, trailing commas), we use resilient Regex extraction instead of strict JSON parsing.
            
            val toolMatch = Regex("tool[\"\\s]*:[\"\\s]*([A-Za-z_]+)", RegexOption.IGNORE_CASE).find(llmResponse)
            val tool = toolMatch?.groupValues?.get(1)?.uppercase() ?: "UNKNOWN"

            return when (tool) {
                "FUEL_LOG" -> {
                    val litersMatch = Regex("lit[er]+s[\"\\s]*:[\"\\s]*([0-9.]+)", RegexOption.IGNORE_CASE).find(llmResponse)
                    val costMatch = Regex("cost[\"\\s]*:[\"\\s]*([0-9.]+)", RegexOption.IGNORE_CASE).find(llmResponse)
                    val odometerMatch = Regex("odometer[\"\\s]*:[\"\\s]*([0-9.]+)", RegexOption.IGNORE_CASE).find(llmResponse)
                    
                    val liters = litersMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: Double.NaN
                    val cost = costMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: Double.NaN
                    val odometer = odometerMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: Double.NaN
                    
                    if (liters.isNaN() || cost.isNaN()) {
                        return "I need the liters and cost to log fuel."
                    }
                    
                    viewModel.logFuel(liters, cost)
                    if (!odometer.isNaN()) {
                        viewModel.updateOdometer(odometer)
                    }
                    "✓ Fuel log saved — ${liters}L at ₹${cost.toInt()}. Queued for sync."
                }
                "TRIP_COMPLETE" -> {
                    val odometerMatch = Regex("finalOdometer[\"\\s]*:[\"\\s]*([0-9.]+)", RegexOption.IGNORE_CASE).find(llmResponse)
                    val finalOdometer = odometerMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: viewModel.activeTrip.value?.vehicle?.odometer ?: 0.0
                    viewModel.completeTrip(finalOdometer)
                    "✓ Trip marked complete! Queued for sync. Well done."
                }
                "INCIDENT_REPORT" -> {
                    val descriptionMatch = Regex("description[\"\\s]*:[\"\\s]*([^\"}]+)", RegexOption.IGNORE_CASE).find(llmResponse)
                    val severityMatch = Regex("severity[\"\\s]*:[\"\\s]*([A-Za-z]+)", RegexOption.IGNORE_CASE).find(llmResponse)
                    
                    val description = descriptionMatch?.groupValues?.get(1)?.trim() ?: ""
                    val severity = severityMatch?.groupValues?.get(1)?.uppercase() ?: "MEDIUM"
                    
                    if (description.isBlank()) {
                        return "Please describe the incident so I can report it."
                    }
                    
                    viewModel.reportIncident(description)
                    "✓ Incident reported. Queued for sync."
                }
                "ODOMETER_UPDATE" -> {
                    val odometerMatch = Regex("odometer[\"\\s]*:[\"\\s]*([0-9.]+)", RegexOption.IGNORE_CASE).find(llmResponse)
                    val odometer = odometerMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: Double.NaN
                    if (odometer.isNaN()) {
                        return "I need the odometer reading."
                    }
                    viewModel.updateOdometer(odometer)
                    "✓ Odometer updated to ${odometer.toInt()} km. Queued for sync."
                }
                "UNKNOWN" -> "I didn't understand that. Try: 'Log 45 liters fuel', 'Complete trip', or 'Report incident'."
                else -> "Unknown tool: $tool"
            }
        } catch (e: Exception) {
            android.util.Log.e("McpToolDispatcher", "Error parsing LLM response. Raw response was: $llmResponse", e)
            return "I had trouble understanding that. Please try rephrasing.\n\n(Debug: $llmResponse)"
        }
    }
}
