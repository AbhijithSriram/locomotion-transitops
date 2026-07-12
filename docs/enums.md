# TransitOps — Enums (Source of Truth)

> Exact string values. Must match character-for-character across Java (backend),
> TypeScript (web dashboard), and Kotlin (Android driver app).
> **Frozen at H0. Any change requires all three people to sign off.**

## VehicleStatus

```
AVAILABLE, ON_TRIP, IN_SHOP, RETIRED, BROKEN_DOWN
```

## VehicleType

```
TRUCK, MINI_TRUCK, VAN, BIKE
```

## DriverStatus

```
AVAILABLE, ON_TRIP, OFF_DUTY, SUSPENDED
```

## TripStatus

```
DRAFT, DISPATCHED, COMPLETED, CANCELLED, INTERRUPTED
```

- `INTERRUPTED` = trip was aborted mid-route (e.g., vehicle breakdown). The Android
  app must render this state; a rescue dispatch may follow.

## Role

```
FLEET_MANAGER, SAFETY_OFFICER, FINANCIAL_ANALYST, DRIVER
```

## OutboxActionType (Android offline sync)

```
TRIP_COMPLETE, FUEL_LOG, ODOMETER_UPDATE, INCIDENT_REPORT
```

## SyncResultStatus (`POST /sync/actions` response)

```
APPLIED, CONFLICT, ERROR
```

- Re-sending an already-applied `idempotencyKey` returns `APPLIED` again (idempotent).

## MaintenanceStatus

```
OPEN, CLOSED
```

## ExpenseCategory

```
FUEL, MAINTENANCE, TOLL, INSURANCE, OTHER
```

## IncidentSeverity (payload of `INCIDENT_REPORT`)

```
LOW, MEDIUM, HIGH
```

## AlertSeverity (`/topic/alerts` WebSocket payloads)

```
INFO, WARN, CRITICAL
```
