package com.transitops.driver.ui.trip

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.transitops.driver.data.local.DatabaseProvider
import com.transitops.driver.data.local.OutboxAction
import com.transitops.driver.data.remote.OutboxActionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.transitops.driver.data.sync.SyncWorker

class TripViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DatabaseProvider.getDatabase(application)
    
    val outboxActions: StateFlow<List<OutboxAction>> = db.outboxDao().getActionsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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
        }
    }

    private suspend fun insertOutboxAction(type: OutboxActionType, payload: String) {
        val action = OutboxAction(
            idempotencyKey = UUID.randomUUID().toString(),
            type = type.name,
            payloadJson = payload,
            createdAt = System.currentTimeMillis()
        )
        db.outboxDao().insertAction(action)
        
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
            
        WorkManager.getInstance(getApplication()).enqueueUniqueWork(
            "immediate_sync",
            androidx.work.ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }
}
