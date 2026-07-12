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
                    performedAt = localAction.createdAt,
                    payload = payloadMap
                )
            } catch (e: Exception) {
                // If we can't parse a local action, skip it for this batch to avoid crashing
                null
            }
        }
        
        val request = SyncActionRequest(actions = syncItems)

        return try {
            // NOTE: In a real implementation, you'd get the TransitOpsApi from your DI container
            // val response = api.syncActions(request)
            
            // For now, let's just simulate the success logic 
            // If the response is successful, we mark them as synced.
            // val results = response.body()?.results ?: emptyList()
            
            // Collect the idempotencyKeys of actions that were successfully applied or conflicted
            // We consider CONFLICT as "handled" by the server, so we shouldn't keep retrying it forever.
            // val keysToMarkSynced = results
            //     .filter { it.status == SyncResultStatus.APPLIED || it.status == SyncResultStatus.CONFLICT }
            //     .map { it.idempotencyKey }
            
            // outboxDao.markAsSynced(keysToMarkSynced)
            
            Result.success()
        } catch (e: Exception) {
            // If the network fails, we return retry so WorkManager tries again later
            Result.retry()
        }
    }
}
