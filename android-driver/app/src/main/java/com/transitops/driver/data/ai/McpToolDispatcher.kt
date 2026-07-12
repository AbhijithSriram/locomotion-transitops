package com.transitops.driver.data.ai

import com.transitops.driver.ui.trip.TripViewModel
import org.json.JSONObject

object McpToolDispatcher {

    fun dispatch(llmResponse: String, viewModel: TripViewModel): String {
        try {
            // The LLM might output markdown like ```json ... ```, so let's extract just the JSON part
            val jsonString = extractJson(llmResponse)
            val json = JSONObject(jsonString)

            if (json.has("error")) {
                return json.getString("error")
            }

            val tool = json.optString("tool", "UNKNOWN")
            val params = json.optJSONObject("params") ?: JSONObject()

            return when (tool) {
                "FUEL_LOG" -> {
                    val liters = params.optDouble("liters", Double.NaN)
                    val cost = params.optDouble("cost", Double.NaN)
                    val odometer = params.optDouble("odometer", Double.NaN)
                    
                    if (liters.isNaN() || cost.isNaN() || odometer.isNaN()) {
                        return "I need the liters, cost, and odometer reading to log fuel."
                    }
                    
                    viewModel.logFuel(liters, cost, odometer)
                    "✓ Fuel log saved — ${liters}L at ₹${cost.toInt()}, odometer ${odometer.toInt()} km. Queued for sync."
                }
                "TRIP_COMPLETE" -> {
                    viewModel.completeTrip()
                    "✓ Trip marked complete! Queued for sync. Well done."
                }
                "INCIDENT_REPORT" -> {
                    val description = params.optString("description", "")
                    val severity = params.optString("severity", "MEDIUM")
                    
                    if (description.isBlank()) {
                        return "Please describe the incident so I can report it."
                    }
                    
                    viewModel.reportIncident(description, severity)
                    "✓ Incident reported (Severity: $severity). Queued for sync."
                }
                "ODOMETER_UPDATE" -> {
                    val odometer = params.optDouble("odometer", Double.NaN)
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
            return "I had trouble understanding that. Please try rephrasing."
        }
    }

    private fun extractJson(text: String): String {
        val startIndex = text.indexOf('{')
        val endIndex = text.lastIndexOf('}')
        if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
            return text.substring(startIndex, endIndex + 1)
        }
        return "{}"
    }
}
