package com.transitops.driver.ui.ai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.transitops.driver.data.ai.GemmaEngine
import com.transitops.driver.data.ai.McpToolDispatcher
import com.transitops.driver.data.remote.ActiveTripResponse
import com.transitops.driver.ui.trip.TripViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isFromUser: Boolean
)

class GemmaViewModel(application: Application) : AndroidViewModel(application) {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    init {
        viewModelScope.launch {
            GemmaEngine.init(application)
            _isReady.value = GemmaEngine.isReady
            
            // Initial greeting
            if (GemmaEngine.isReady) {
                _messages.value = listOf(
                    ChatMessage(text = "Hello! I'm your on-device AI assistant. I can log fuel, report incidents, or complete your trip. What do you need?", isFromUser = false)
                )
            } else {
                 _messages.value = listOf(
                    ChatMessage(text = "Failed to extract or load the AI model from the app bundle.", isFromUser = false)
                )
            }
        }
    }

    fun sendMessage(userMessage: String, tripViewModel: TripViewModel) {
        if (userMessage.isBlank() || _isProcessing.value) return

        val currentMessages = _messages.value.toMutableList()
        currentMessages.add(ChatMessage(text = userMessage, isFromUser = true))
        _messages.value = currentMessages

        viewModelScope.launch {
            _isProcessing.value = true
            
            // Build the prompt with context
            val activeTrip = tripViewModel.activeTrip.value
            val prompt = buildPrompt(userMessage, activeTrip)
            
            val llmResponse = GemmaEngine.generate(prompt)
            
            // Dispatch to tools
            val responseText = McpToolDispatcher.dispatch(llmResponse, tripViewModel)
            
            val updatedMessages = _messages.value.toMutableList()
            updatedMessages.add(ChatMessage(text = responseText, isFromUser = false))
            _messages.value = updatedMessages
            
            _isProcessing.value = false
        }
    }

    private fun buildPrompt(userMessage: String, activeTrip: ActiveTripResponse?): String {
        val tripContext = if (activeTrip != null) {
            "Trip #${activeTrip.tripId}, ${activeTrip.source.name} -> ${activeTrip.destination.name}, Vehicle ${activeTrip.vehicle.regNumber}."
        } else {
            "No active trip."
        }

        return """
            You are a driver assistant. Extract actions from driver messages and respond ONLY with valid JSON.
            CRITICAL: For numbers (like liters, cost, odometer), output ONLY raw digits. Do NOT include units like "liters", "kms", or "rupees" inside the JSON values.
            Available tools: FUEL_LOG, TRIP_COMPLETE, INCIDENT_REPORT, ODOMETER_UPDATE.
            Current context: $tripContext
            
            Examples:
            User: "filled 45 liters cost 4200 odometer 15420"
            Response: {"tool":"FUEL_LOG","params":{"liters":45,"cost":4200,"odometer":15420}}
            
            User: "trip done"  
            Response: {"tool":"TRIP_COMPLETE","params":{}}
            
            User: "tyre burst on highway"
            Response: {"tool":"INCIDENT_REPORT","params":{"description":"tyre burst on highway","severity":"HIGH"}}
            
            User: "filled 60 litres of diesel for 6000 rupees. odometer shows 60000 kms"
            Response: {"tool":"FUEL_LOG","params":{"liters":60,"cost":6000,"odometer":60000}}
            
            User: "$userMessage"
            Response: {
        """.trimIndent()
    }
}
