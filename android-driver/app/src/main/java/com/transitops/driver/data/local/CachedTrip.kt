package com.transitops.driver.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.transitops.driver.data.remote.ActiveTripResponse
import com.transitops.driver.data.remote.LocationDto
import com.transitops.driver.data.remote.TripStatus
import com.transitops.driver.data.remote.VehicleDto
import com.transitops.driver.data.remote.VehicleType

/**
 * Local Room representation of the active trip from GET /driver/me/active-trip.
 * All nested objects are flattened into columns so no TypeConverters are needed.
 * This ensures the driver can view their trip even without an internet connection.
 */
@Entity(tableName = "cached_trips")
data class CachedTrip(
    @PrimaryKey val tripId: Long,
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
    val vehicleId: Long,
    val vehicleRegNumber: String,
    val vehicleType: String,

    // The Google encoded polyline string for offline navigation
    val routePolyline: String?,

    val dispatchedAt: Long?
)

/** Maps the network response to the flat Room entity. */
fun ActiveTripResponse.toCachedTrip() = CachedTrip(
    tripId = tripId,
    status = status.name,
    sourceName = source.name,
    sourceLat = source.lat,
    sourceLng = source.lng,
    destName = destination.name,
    destLat = destination.lat,
    destLng = destination.lng,
    cargoWeightKg = cargoWeightKg,
    vehicleId = vehicle.id,
    vehicleRegNumber = vehicle.regNumber,
    vehicleType = vehicle.type.name,
    routePolyline = routePolyline,
    dispatchedAt = dispatchedAt
)

/** Maps the flat Room entity back to the network response shape for UI consumption. */
fun CachedTrip.toResponse() = ActiveTripResponse(
    tripId = tripId,
    status = TripStatus.valueOf(status),
    source = LocationDto(sourceName, sourceLat, sourceLng),
    destination = LocationDto(destName, destLat, destLng),
    cargoWeightKg = cargoWeightKg,
    vehicle = VehicleDto(
        id = vehicleId,
        regNumber = vehicleRegNumber,
        type = VehicleType.valueOf(vehicleType),
        maxLoadKg = 0.0,   // not stored locally — not needed for UI
        odometer = 0.0
    ),
    routePolyline = routePolyline,
    dispatchedAt = dispatchedAt
)
