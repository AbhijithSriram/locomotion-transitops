package com.transitops.driver.ui.trip

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.transitops.driver.data.local.DatabaseProvider
import com.transitops.driver.data.local.OutboxAction
import com.transitops.driver.data.remote.OutboxActionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class TripViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DatabaseProvider.getDatabase(application)

    fun logFuel(vehicleId: String, liters: Double, cost: Double, odometer: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val payload = """
                {
                  "vehicleId": "$vehicleId",
                  "liters": $liters,
                  "cost": $cost,
                  "odometer": $odometer
                }
            """.trimIndent()
            
            insertOutboxAction(OutboxActionType.FUEL_LOG, payload)
        }
    }

    fun reportIncident(tripId: String, vehicleId: String, description: String, severity: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val payload = """
                {
                  "tripId": "$tripId",
                  "vehicleId": "$vehicleId",
                  "description": "$description",
                  "severity": "$severity"
                }
            """.trimIndent()
            
            insertOutboxAction(OutboxActionType.INCIDENT_REPORT, payload)
        }
    }

    fun completeTrip(tripId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val payload = """
                {
                  "tripId": "$tripId"
                }
            """.trimIndent()
            
            insertOutboxAction(OutboxActionType.TRIP_COMPLETE, payload)
            
            // In a real app we would also trigger the SyncWorker here
        }
    }

    private suspend fun insertOutboxAction(type: OutboxActionType, payload: String) {
        val action = OutboxAction(
            id = UUID.randomUUID().toString(),
            type = type,
            payloadJson = payload,
            createdAt = System.currentTimeMillis()
        )
        db.outboxDao().insert(action)
    }
}
