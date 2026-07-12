package com.transitops.driver.data.remote

/**
 * Enums representing the exact string values expected by the backend API.
 * These must match the backend contract (enums.md) perfectly.
 */

enum class VehicleStatus {
    AVAILABLE, ON_TRIP, IN_SHOP, RETIRED, BROKEN_DOWN
}

enum class VehicleType {
    TRUCK, MINI_TRUCK, VAN, BIKE
}

enum class DriverStatus {
    AVAILABLE, ON_TRIP, OFF_DUTY, SUSPENDED
}

enum class TripStatus {
    DRAFT, DISPATCHED, COMPLETED, CANCELLED, INTERRUPTED
}

enum class Role {
    FLEET_MANAGER, SAFETY_OFFICER, FINANCIAL_ANALYST, DRIVER
}

enum class OutboxActionType {
    TRIP_COMPLETE, FUEL_LOG, ODOMETER_UPDATE, INCIDENT_REPORT
}

enum class SyncResultStatus {
    APPLIED, CONFLICT, ERROR
}

enum class MaintenanceStatus {
    OPEN, CLOSED
}

enum class ExpenseCategory {
    FUEL, MAINTENANCE, TOLL, INSURANCE, OTHER
}

enum class IncidentSeverity {
    LOW, MEDIUM, HIGH
}
