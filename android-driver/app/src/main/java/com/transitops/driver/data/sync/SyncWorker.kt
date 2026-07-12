package com.transitops.driver.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.transitops.driver.data.local.AppDatabase
import com.transitops.driver.data.remote.OutboxActionType
import com.transitops.driver.data.remote.SyncActionItem
import com.transitops.driver.data.remote.SyncActionRequest
import com.transitops.driver.data.remote.SyncResultStatus
import com.transitops.driver.data.remote.TransitOpsApi
import com.transitops.driver.data.remote.NetworkProvider
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

/**
 * WorkManager job that flushes the local Outbox to the remote server.
 * It is triggered when network connectivity is restored.
 */
class SyncWorker(
    context: Context,
    workerParams: WorkerParameters,
    // In a real app with DI, these would be injected. 
    // For now, we will assume they are passed or fetched inside doWork.
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // Note: For hackathon speed, we instantiate DB/API here if not using DI.
        // We assume ServiceLocator or similar will provide the Api instance.
        // For demonstration, we'll assume we have a singleton provider.
        
        // Ensure TokenProvider is initialized so we have the JWT for the network call
        com.transitops.driver.data.auth.TokenProvider.init(applicationContext)
        
        val database = AppDatabase.getDatabase(applicationContext)
        val outboxDao = database.outboxDao()
        
        // Fetch unsynced actions from the local SQLite database
        val unsyncedActions = outboxDao.getUnsyncedActions()
        
        if (unsyncedActions.isEmpty()) {
            return Result.success() // Nothing to do!
        }

        // We need Moshi to parse the payloadJson string back into a Map so it fits our SyncActionItem
        val moshi = Moshi.Builder().build()
        val mapType = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        val jsonAdapter = moshi.adapter<Map<String, Any>>(mapType)

        // Convert Room entities to our API payload shape
        val syncItems = unsyncedActions.mapNotNull { localAction ->
            try {
                val payloadMap = jsonAdapter.fromJson(localAction.payloadJson) ?: emptyMap()
                SyncActionItem(
                    idempotencyKey = localAction.idempotencyKey,
                    type = OutboxActionType.valueOf(localAction.type),
                    driverId = localAction.driverId,
                    payload = payloadMap
                )
            } catch (e: Exception) {
                // If we can't parse a local action, skip it for this batch to avoid crashing
                null
            }
        }
        
        val request = SyncActionRequest(actions = syncItems)

        return try {
            val response = NetworkProvider.api.syncActions(request)
            
            if (response.isSuccessful) {
                val results = response.body()?.results ?: emptyList()
                
                // Collect the idempotencyKeys of actions that were successfully applied or conflicted
                // We consider CONFLICT as "handled" by the server, so we shouldn't keep retrying it forever.
                val keysToMarkSynced = results
                    .filter { it.result == "applied" || it.result == "conflict" }
                    .map { it.idempotencyKey }
                
                if (keysToMarkSynced.isNotEmpty()) {
                    outboxDao.markAsSynced(keysToMarkSynced)
                }
                
                Result.success()
            } else {
                // E.g. 404, 500, etc.
                android.util.Log.e("SyncWorker", "API error: ${response.code()} ${response.errorBody()?.string()}")
                Result.retry()
            }
        } catch (e: Exception) {
            // E.g. timeout, no network
            android.util.Log.e("SyncWorker", "Network exception during sync", e)
            Result.retry()
        }
    }
}
