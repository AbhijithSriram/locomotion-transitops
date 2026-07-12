package com.transitops.driver.data.remote

import com.squareup.moshi.JsonClass

/**
 * Data models for the REST API contract defined in docs/api-contract.md.
 * We use Moshi for JSON serialization/deserialization.
 */

// --- Authentication ---

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val email: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class RefreshRequest(
    val refreshToken: String
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresInMs: Long,
    val user: UserDto
)

@JsonClass(generateAdapter = true)
data class UserDto(
    val id: String,
    val name: String,
    val email: String,
    val role: Role,
    val driverId: String? // Only present if role == DRIVER
)

// --- Shared Entity DTOs ---

@JsonClass(generateAdapter = true)
data class LocationDto(
    val name: String,
    val lat: Double,
    val lng: Double
)

@JsonClass(generateAdapter = true)
data class VehicleDto(
    val id: String,
    val regNumber: String,
    val type: VehicleType,
    val maxLoadKg: Double,
    val odometer: Double,
    // Note: status and health are omitted here as per the active-trip payload shape, 
    // but can be added if needed for other endpoints.
)

// --- Active Trip ---

/**
 * Response for GET /driver/me/active-trip
 */
@JsonClass(generateAdapter = true)
data class ActiveTripResponse(
    val tripId: String,
    val status: TripStatus,
    val source: LocationDto,
    val destination: LocationDto,
    val cargoWeightKg: Double,
    val vehicle: VehicleDto,
    val routePolyline: String?,
    val dispatchedAt: Long?
)

// --- Offline Sync (Outbox) ---

/**
 * Request payload for POST /sync/actions
 */
@JsonClass(generateAdapter = true)
data class SyncActionRequest(
    val actions: List<SyncActionItem>
)

@JsonClass(generateAdapter = true)
data class SyncActionItem(
    val idempotencyKey: String,
    val type: OutboxActionType,
    val performedAt: Long,
    // The payload is passed as a raw JSON string or dynamic map, but Moshi handles Maps well.
    // For simplicity, we represent it as a generic map which Moshi serializes to JSON objects.
    val payload: Map<String, Any>
)

/**
 * Response payload from POST /sync/actions
 */
@JsonClass(generateAdapter = true)
data class SyncResultResponse(
    val results: List<SyncResultItem>
)

@JsonClass(generateAdapter = true)
data class SyncResultItem(
    val idempotencyKey: String,
    val status: SyncResultStatus,
    val message: String?
)

// --- Standard Error Envelope ---

@JsonClass(generateAdapter = true)
data class ErrorEnvelope(
    val error: ErrorDetails
)

@JsonClass(generateAdapter = true)
data class ErrorDetails(
    val code: String,
    val message: String
)
