package com.transitops.driver.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Data Access Object for handling the outbox sync queue.
 */
@Dao
interface OutboxDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAction(action: OutboxAction)

    // Fetch all actions that haven't been successfully synced yet, ordered by oldest first
    @Query("SELECT * FROM outbox_actions WHERE synced = 0 ORDER BY createdAt ASC")
    suspend fun getUnsyncedActions(): List<OutboxAction>

    // Observe all outbox actions reactively for the UI (latest first)
    @Query("SELECT * FROM outbox_actions ORDER BY createdAt DESC")
    fun getActionsFlow(): kotlinx.coroutines.flow.Flow<List<OutboxAction>>

    // Mark specific actions as synced so they aren't sent again
    @Query("UPDATE outbox_actions SET synced = 1 WHERE idempotencyKey IN (:keys)")
    suspend fun markAsSynced(keys: List<String>)
    
    // Optional: Clean up old synced actions to save space
    @Query("DELETE FROM outbox_actions WHERE synced = 1")
    suspend fun deleteSyncedActions()
}

/**
 * Data Access Object for caching the currently active trip.
 */
@Dao
interface TripDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: CachedTrip)

    @Query("SELECT * FROM cached_trips LIMIT 1")
    suspend fun getActiveTrip(): CachedTrip?

    @Query("DELETE FROM cached_trips")
    suspend fun clearTrips()
}
