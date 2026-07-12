package com.transitops.driver.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The core of our offline-first architecture. 
 * Every action the driver takes (completing a trip, logging fuel) is saved here first.
 * A background worker later batches and sends these to the server.
 */
@Entity(tableName = "outbox_actions")
data class OutboxAction(
    // Local auto-generated ID for Room
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    
    // A UUID generated on the device. The server uses this to prevent applying the same action twice (Idempotency).
    val idempotencyKey: String,
    
    // What kind of action this is (e.g., TRIP_COMPLETE, FUEL_LOG)
    val type: String,
    
    // The actual JSON payload to send to the server
    val payloadJson: String,
    
    // When the action occurred on the device
    val createdAt: Long,
    
    // True if this action has been successfully acknowledged by the server
    val synced: Boolean = false
)
