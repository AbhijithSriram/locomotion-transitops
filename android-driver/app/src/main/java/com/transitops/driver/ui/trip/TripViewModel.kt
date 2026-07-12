package com.transitops.driver.ui.trip

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.transitops.driver.data.local.AppDatabase
import com.transitops.driver.data.local.OutboxAction
import com.transitops.driver.data.local.toCachedTrip
import com.transitops.driver.data.remote.ActiveTripResponse
import com.transitops.driver.data.remote.NetworkProvider
import com.transitops.driver.data.remote.OutboxActionType
import com.transitops.driver.data.remote.TripStatus
import com.transitops.driver.data.local.toResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.transitops.driver.data.sync.SyncWorker

private const val ACTIVE_TRIP_POLL_INTERVAL_MS = 30_000L

class TripViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)

    // --- Active Trip State ---
    private val _activeTrip = MutableStateFlow<ActiveTripResponse?>(null)
    val activeTrip: StateFlow<ActiveTripResponse?> = _activeTrip.asStateFlow()

    private val _isTripLoading = MutableStateFlow(false)
    val isTripLoading: StateFlow<Boolean> = _isTripLoading.asStateFlow()

    // --- Conflict messages from sync (one-shot events) ---
    private val _conflictMessage = MutableSharedFlow<String>()
    val conflictMessage: SharedFlow<String> = _conflictMessage.asSharedFlow()

    // --- Outbox actions (live, for activity history UI) ---
    val outboxActions = db.outboxDao().getActionsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        loadActiveTrip()
        startPolling()
    }

    /**
     * Fetches the active trip — first shows cached Room data, then refreshes from network.
     * Safe to call offline: will show cached data without crashing.
     */
    fun loadActiveTrip() {
        viewModelScope.launch(Dispatchers.IO) {
            _isTripLoading.value = true
            try {
                // 1. Show cached data first for instant display
                val cached = db.tripDao().getActiveTrip()
                if (cached != null) {
                    _activeTrip.value = cached.toResponse()
                }

                // 2. Refresh from network
                val response = NetworkProvider.api.getActiveTrip()
                when {
                    response.isSuccessful -> {
                        val trip = response.body()
                        if (trip != null) {
                            _activeTrip.value = trip
                            db.tripDao().upsertTrip(trip.toCachedTrip())
                        }
                    }
                    response.code() == 404 -> {
                        // NO_ACTIVE_TRIP — driver is idle, clear the cache
                        _activeTrip.value = null
                        db.tripDao().clearTrips()
                    }
                    else -> Log.w("TripViewModel", "Unexpected response: ${response.code()}")
                }
            } catch (e: Exception) {
                // Network unavailable — cached data (set above) is still shown
                Log.d("TripViewModel", "Offline — showing cached trip data")
            } finally {
                _isTripLoading.value = false
            }
        }
    }

    /** Polls the active-trip endpoint every 30s to detect new dispatch assignments. */
    private fun startPolling() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(ACTIVE_TRIP_POLL_INTERVAL_MS)
                loadActiveTrip()
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Outbox actions — write locally first, trigger sync immediately when online
    // ---------------------------------------------------------------------------

    fun logFuel(liters: Double, cost: Double, odometer: Double) {
        val vehicleId = _activeTrip.value?.vehicle?.id ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val payload = """{"vehicleId":$vehicleId,"liters":$liters,"cost":$cost,"odometer":$odometer}"""
            insertOutboxAction(OutboxActionType.FUEL_LOG, payload)
        }
    }

    fun reportIncident(description: String, severity: String) {
        val trip = _activeTrip.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val payload = """{"tripId":${trip.tripId},"vehicleId":${trip.vehicle.id},"description":"$description","severity":"$severity"}"""
            insertOutboxAction(OutboxActionType.INCIDENT_REPORT, payload)
        }
    }

    fun completeTrip() {
        val tripId = _activeTrip.value?.tripId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val payload = """{"tripId":$tripId}"""
            insertOutboxAction(OutboxActionType.TRIP_COMPLETE, payload)
            // Optimistically clear the local trip so UI shows "waiting for dispatch"
            _activeTrip.value = null
            db.tripDao().clearTrips()
        }
    }

    fun updateOdometer(odometer: Double) {
        val vehicleId = _activeTrip.value?.vehicle?.id ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val payload = """{"vehicleId":$vehicleId,"odometer":$odometer}"""
            insertOutboxAction(OutboxActionType.ODOMETER_UPDATE, payload)
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

        // Trigger an immediate sync attempt (will only run if network is available)
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

    /** Called by SyncWorker (via shared prefs or WorkManager output) when a conflict arrives. */
    fun emitConflict(message: String) {
        viewModelScope.launch {
            _conflictMessage.emit(message)
        }
    }
}
