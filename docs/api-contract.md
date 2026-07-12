# TransitOps — REST API Contract (Source of Truth)

> **Frozen at H0.** Person A implements this; Person B (dashboard) and Person C
> (Android) mock against it. Any change requires all three people to sign off.
> Enum string values live in [enums.md](enums.md). WebSocket topics live in
> [events-contract.md](events-contract.md).

## Global conventions

- Base URL: `http://<host>:8080` (no `/api` prefix — matches backend bootstrap)
- **IDs are server-generated UUID strings** (e.g. `"550e8400-e29b-41d4-a716-446655440000"`).
  Examples below use short placeholders (`"veh-5"`, `"drv-3"`, `"trip-42"`, `"usr-7"`) for readability;
  on the wire they are full UUIDs. Clients treat every id as an opaque string.
- **All timestamps are epoch milliseconds** (`1752300000000`) — plain numbers,
  no ISO parsing/adapters needed in Gson/Moshi, Jackson, or JS.
- Auth: `Authorization: Bearer <accessToken>` header on everything except
  `/auth/login` and `/auth/refresh`.
- Lists return plain JSON arrays. No pagination (hackathon scope).
- Locations are always `{ "name": string, "lat": number, "lng": number }`.
- Every non-2xx response uses the standard error envelope:

```json
{ "error": { "code": "OVERLOADED_VEHICLE", "message": "Cargo 2500kg exceeds max load 2000kg" } }
```

### Error codes

| Code | Meaning | HTTP |
|---|---|---|
| `VALIDATION_ERROR` | bad/missing field | 400 |
| `UNAUTHORIZED` | missing/expired token | 401 |
| `FORBIDDEN` | role not allowed | 403 |
| `NOT_FOUND` | entity id doesn't exist | 404 |
| `NO_ACTIVE_TRIP` | driver has no dispatched trip | 404 |
| `DUPLICATE_REG_NUMBER` | vehicle regNumber exists | 409 |
| `VEHICLE_NOT_AVAILABLE` | vehicle not `AVAILABLE` (retired / in shop / on trip / broken down) | 409 |
| `DRIVER_NOT_AVAILABLE` | driver not `AVAILABLE` (on trip / off duty) | 409 |
| `DRIVER_SUSPENDED` | driver is suspended | 409 |
| `LICENSE_EXPIRED` | driver license expired | 409 |
| `OVERLOADED_VEHICLE` | cargoWeightKg > vehicle maxLoadKg | 409 |
| `INVALID_STATE_TRANSITION` | e.g., completing a DRAFT trip | 409 |

---

## 1. Auth

### `POST /auth/login`

Request:
```json
{ "email": "alex@transitops.io", "password": "secret" }
```

Response `200`:
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "expiresInMs": 43200000,
  "user": { "id": "usr-7", "name": "Alex", "email": "alex@transitops.io", "role": "DRIVER", "driverId": "drv-3" }
}
```

- `driverId` present **only** when `role == "DRIVER"` (links auth user → Driver entity); `null`/absent otherwise.
- Access token lifetime 12h (long enough that expiry can never hit mid-demo).

### `POST /auth/refresh`

Request: `{ "refreshToken": "eyJ..." }` → Response `200`: **same shape as login**.

---

## 2. Entity shapes

### Vehicle
```json
{
  "id": "veh-5",
  "regNumber": "TN-01-AB-1234",
  "type": "VAN",
  "maxLoadKg": 2000,
  "odometer": 15000,
  "acquisitionCost": 1200000,
  "status": "AVAILABLE",
  "health": { "tyres": 92, "engine": 88, "brakes": 95, "riskScore": 12 }
}
```
- `health` fields are 0–100 integers; `riskScore` 0–100 (higher = worse). Health is
  written by the simulation engine; treat as read-only through REST.

### Driver
```json
{
  "id": "drv-3",
  "name": "Alex",
  "licenseNumber": "DL-2020-998877",
  "licenseExpiry": 1785000000000,
  "safetyScore": 87,
  "status": "AVAILABLE"
}
```

### Trip
```json
{
  "id": "trip-42",
  "source":      { "name": "Chennai Depot", "lat": 13.0827, "lng": 80.2707 },
  "destination": { "name": "Bangalore Hub", "lat": 12.9716, "lng": 77.5946 },
  "vehicleId": "veh-5",
  "driverId": "drv-3",
  "cargoWeightKg": 1200,
  "status": "DISPATCHED",
  "routePolyline": "gfo}EtohhU...",
  "createdAt": 1752299000000,
  "dispatchedAt": 1752300000000,
  "completedAt": null
}
```
- `routePolyline`: Google **encoded polyline (precision 5)**, or `null` before dispatch.
  The simulation engine fetches and stores it at dispatch time; the fallback (no Google
  key) encodes a straight source→destination line in the same format — clients never care.

### MaintenanceLog
```json
{ "id": "mnt-9", "vehicleId": "veh-5", "description": "Brake pad replacement", "cost": 4500,
  "status": "OPEN", "openedAt": 1752300000000, "closedAt": null }
```

### FuelLog
```json
{ "id": "fuel-21", "vehicleId": "veh-5", "tripId": "trip-42", "liters": 45.5, "cost": 4200,
  "odometer": 15020, "loggedAt": 1752301000000 }
```
- `tripId` nullable (depot refuels aren't tied to a trip).

### Expense
```json
{ "id": "exp-33", "vehicleId": "veh-5", "tripId": null, "category": "TOLL", "amount": 350,
  "description": "NH44 toll", "incurredAt": 1752301000000 }
```

---

## 3. CRUD + actions

### Vehicles
- `GET /vehicles` → `Vehicle[]` — optional `?status=AVAILABLE` filter
- `GET /vehicles/{id}` → `Vehicle`
- `POST /vehicles` (body = Vehicle without `id`/`status`/`health`) → `201 Vehicle` — `409 DUPLICATE_REG_NUMBER`
- `PUT /vehicles/{id}` → `Vehicle`
- `POST /vehicles/{id}/retire` → `Vehicle` (status → `RETIRED`; terminal)

### Drivers
- `GET /drivers` / `GET /drivers/{id}` / `POST /drivers` / `PUT /drivers/{id}` — same pattern
- `POST /drivers/{id}/suspend` and `POST /drivers/{id}/reinstate` → `Driver`

### Trips
- `GET /trips` → `Trip[]` — optional `?status=` filter
- `GET /trips/{id}` → `Trip`
- `POST /trips` → `201 Trip` (status `DRAFT`). Body:
  ```json
  { "source": {...}, "destination": {...}, "vehicleId": "veh-5", "driverId": "drv-3", "cargoWeightKg": 1200 }
  ```
  Validation (`OVERLOADED_VEHICLE` etc.) runs at **dispatch**, not draft creation.
- `POST /trips/{id}/dispatch` → `Trip` — runs the full business-rule state machine
  (vehicle available, driver available + license valid + not suspended, no double
  booking, cargo ≤ max load). On success vehicle & driver → `ON_TRIP`.
- `POST /trips/{id}/complete` → `Trip` — body optional: `{ "finalOdometer": 15020 }`.
  Vehicle & driver → `AVAILABLE`.
- `POST /trips/{id}/cancel` → `Trip` — allowed from `DRAFT` or `DISPATCHED`.
  Vehicle & driver → `AVAILABLE`.

### Maintenance
- `GET /maintenance` → `MaintenanceLog[]` — optional `?vehicleId=` / `?status=OPEN`
- `POST /maintenance` `{ "vehicleId": "veh-5", "description": "...", "cost": 4500 }` → `201`
  — vehicle → `IN_SHOP` (`409 VEHICLE_NOT_AVAILABLE` if it's on a trip)
- `POST /maintenance/{id}/close` → `MaintenanceLog` — vehicle → `AVAILABLE` (unless `RETIRED`)

### Fuel & expenses
- `GET /fuel-logs` (optional `?vehicleId=`) / `POST /fuel-logs` → `201`
- `GET /expenses` (optional `?vehicleId=`, `?category=`) / `POST /expenses` → `201`

### Reports
- `GET /reports/summary` →
  ```json
  {
    "totalVehicles": 12, "activeTrips": 4, "completedTrips": 38,
    "totalFuelCost": 182000, "totalMaintenanceCost": 46000, "totalExpenses": 251000,
    "avgFuelEfficiencyKmPerL": 9.4, "fleetUtilizationPct": 66.7
  }
  ```
- `GET /reports/export.csv` → `text/csv` download of trips + costs.

---

## 4. Driver app endpoints (Person C)

### `GET /driver/me/active-trip`

The trip in `DISPATCHED` status for the driver identified by the bearer token.

Response `200`:
```json
{
  "tripId": "trip-42",
  "status": "DISPATCHED",
  "source":      { "name": "Chennai Depot", "lat": 13.0827, "lng": 80.2707 },
  "destination": { "name": "Bangalore Hub", "lat": 12.9716, "lng": 77.5946 },
  "cargoWeightKg": 1200,
  "vehicle": { "id": "veh-5", "regNumber": "TN-01-AB-1234", "type": "VAN", "maxLoadKg": 2000, "odometer": 15000 },
  "routePolyline": "gfo}EtohhU...",
  "dispatchedAt": 1752300000000
}
```

`404` with code `NO_ACTIVE_TRIP` when none.

**New-trip discovery:** the app polls this endpoint every ~30s while online. No push
infrastructure.

### `POST /sync/actions`

Batched offline actions. Server processes the array **in order, each item
independently** (no all-or-nothing) — each action gets its own transaction,
so one bad item doesn't roll back the others.

Request:
```json
{
  "actions": [
    {
      "idempotencyKey": "550e8400-e29b-41d4-a716-446655440000",
      "type": "TRIP_COMPLETE",
      "driverId": "driver-9",
      "payload": { "tripId": "trip-42", "finalOdometer": 15020, "actualDistanceKm": 95.0 }
    }
  ]
}
```

- `payload` is a **raw JSON object** (backend binds to `JsonNode`, stores as text) — not an escaped string.
- `idempotencyKey` = UUID generated on-device, persisted in the Room outbox. **Required.**
- `driverId` — **required on every action**, not just trip/incident ones. Used as the actor reference for `FUEL_LOG` and `INCIDENT_REPORT`, and stored on the `SyncedAction` dedupe record regardless of type.
- No `performedAt` field currently — the server stamps `processedAt` itself on receipt. If the app needs the original device timestamp preserved (e.g. for accurate fuel/incident ordering), that's not wired in yet — flag it and we'll add it as an optional payload field per type rather than a top-level one.

Per-type payloads (as currently implemented):

| `type` | `payload` |
|---|---|
| `TRIP_COMPLETE` | `{ "tripId": "...", "finalOdometer": 15020, "actualDistanceKm": 95.0 }` — `actualDistanceKm` optional, falls back to trip's `plannedDistanceKm` if omitted. **No `fuelUsedLiters` here** — log fuel separately via a `FUEL_LOG` action. |
| `FUEL_LOG` | `{ "vehicleId": "...", "liters": 45.5, "cost": 4200, "date": "2026-07-12T10:00:00Z" }` — `date` optional (ISO-8601 string), defaults to server receipt time if omitted. **No `odometer` field on this payload** — use a separate `ODOMETER_UPDATE` action if the odometer needs updating alongside a fuel log. |
| `ODOMETER_UPDATE` | `{ "vehicleId": "...", "odometer": 15100 }` |
| `INCIDENT_REPORT` | `{ "vehicleId": "...", "tripId": "...", "description": "..." }` — `tripId` optional. **No `severity` field currently** — not modeled on `IncidentReport` yet. If you need it for dashboard triage, tell us and we'll add it. |

Response `200` (always 200; per-item outcomes inside):
```json
{
  "results": [
    { "idempotencyKey": "550e8400-...", "result": "applied", "message": null }
  ]
}
```

- Field is `result`, not `status`.
- Values are **lowercase**: `applied | conflict | error` (not `APPLIED`/`CONFLICT`/`ERROR`).
- Duplicate `idempotencyKey` (already applied earlier) → `applied` again, no-op, no duplicate row written.
- `conflict` example: `TRIP_COMPLETE` arrives for a trip the dispatcher meanwhile moved out of `DISPATCHED` (e.g. cancelled) → server leaves it as-is, returns `{ "result": "conflict", "message": "Trip is CANCELLED, expected DISPATCHED" }`. Same pattern for `ODOMETER_UPDATE` if the submitted value is behind the server's current odometer.
- `error` = malformed/missing payload field or unknown `type`. `message` carries the exception text. App may drop the action after showing it — these are **not** persisted as `SyncedAction`, so an `error` result is safe to retry after the app fixes the payload (won't be treated as a duplicate).

**Append-only vs server-authoritative** (unchanged from original design):
- `FUEL_LOG`, `INCIDENT_REPORT` — always merge, always `applied` (barring malformed payload).
- `TRIP_COMPLETE`, `ODOMETER_UPDATE` — server-authoritative, can return `conflict`.

---
## 5. Simulation god-mode (Person B; used by dashboard demo controls)

- `POST /sim/spawn-trip` — body optional `{ "vehicleId": "veh-5", "driverId": "drv-3" }`; omitted →
  random available pair. Creates + dispatches a trip with a real route. → `Trip`
- `POST /sim/trigger-breakdown/{vehicleId}` → `202` — vehicle → `BROKEN_DOWN`, trip →
  `INTERRUPTED`, alert emitted, rescue dispatch logic kicks in
- `POST /sim/speed` — `{ "multiplier": 5 }` (1 = real time; clamp 1–60) → `200 { "multiplier": 5 }`

Roles: `FLEET_MANAGER` can call everything above; `SAFETY_OFFICER` and
`FINANCIAL_ANALYST` are read-only (GETs + reports); `DRIVER` can only call section 4.
