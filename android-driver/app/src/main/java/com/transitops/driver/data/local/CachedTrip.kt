package com.transitops.driver.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local representation of the ActiveTripResponse.
 * This ensures the driver can view their currently assigned trip even without an internet connection.
 * We flatten out some nested objects for simplicity in Room, or we can use TypeConverters.
 * Here, we will use plain fields.
 */
@Entity(tableName = "cached_trips")
data class CachedTrip(
    @PrimaryKey val tripId: String,
    val status: String,
    
    // Flattened Source Location
    val sourceName: String,
    val sourceLat: Double,
    val sourceLng: Double,
    
    // Flattened Destination Location
    val destName: String,
    val destLat: Double,
    val destLng: Double,
    
    val cargoWeightKg: Double,
    
    // Flattened Vehicle details
    val vehicleId: String,
    val vehicleRegNumber: String,
    
    // The Google encoded polyline string for offline mapping/navigation
    val routePolyline: String?,
    
    val dispatchedAt: Long?
)
