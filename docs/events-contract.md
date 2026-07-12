# TransitOps — WebSocket Events Contract (Source of Truth)

> **Frozen at H0.** Emitted by Person B's simulation engine inside the Spring Boot
> app; consumed only by the web dashboard. The Android app never uses WebSockets
> (offline-first → REST + sync only). Enum values in [enums.md](enums.md).

## Connection

- Endpoint: `ws://<host>:8080/ws` — STOMP over SockJS (SockJS fallback enabled).
- Auth: `Authorization: Bearer <accessToken>` header on the STOMP CONNECT frame.
  (Demo fallback: server may permit anonymous subscribes — decided at H3 integration
  check if auth fights us.)
- All payloads are JSON. All timestamps epoch millis. Every message carries `ts`
  (server emit time) so the dashboard can drop stale updates.

## Topics

### `/topic/vehicle-position` — every sim tick (~1s real time) per moving vehicle
```json
{ "vehicleId": 5, "lat": 13.0512, "lng": 80.1123, "speedKmh": 62.5, "heading": 214.0, "tripId": 42, "ts": 1752300001000 }
```
- `heading` = degrees clockwise from north (dashboard rotates the marker icon).

### `/topic/vehicle-status` — on change only
```json
{ "vehicleId": 5, "status": "BROKEN_DOWN", "ts": 1752300055000 }
```

### `/topic/vehicle-health` — every ~5s per active vehicle, and immediately on breakdown
```json
{ "vehicleId": 5, "tyres": 78, "engine": 64, "brakes": 81, "riskScore": 43, "ts": 1752300005000 }
```
- All values 0–100 integers. `riskScore`: higher = worse; dashboard colors: <40 green, 40–70 amber, >70 red.

### `/topic/trip-status` — on change only
```json
{ "tripId": 42, "status": "INTERRUPTED", "vehicleId": 5, "driverId": 3, "ts": 1752300055000 }
```

### `/topic/alerts` — on event
```json
{ "type": "BREAKDOWN", "severity": "CRITICAL", "message": "Van-05 broke down 12km from Bangalore Hub", "vehicleId": 5, "tripId": 42, "ts": 1752300055000 }
```
- `type` ∈ `BREAKDOWN | HIGH_RISK | RESCUE_DISPATCHED | TRIP_COMPLETED | LICENSE_EXPIRING | MAINTENANCE_DUE`
- `severity` ∈ `INFO | WARN | CRITICAL` (see enums.md AlertSeverity)
- `vehicleId` / `tripId` nullable depending on type.

### `/topic/kpi` — every ~2s (or on change)
```json
{
  "activeVehicles": 4, "availableVehicles": 6, "inMaintenance": 2,
  "activeTrips": 4, "pendingTrips": 1, "driversOnDuty": 7,
  "utilizationPct": 66.7, "ts": 1752300002000
}
```
- `activeVehicles` = ON_TRIP count; `pendingTrips` = DRAFT count;
  `utilizationPct` = ON_TRIP / (total − RETIRED) × 100, one decimal.

## Dashboard fallback (from risk plan)

If STOMP integration fights us at H3: dashboard flips to polling `GET /vehicles` +
`GET /reports/summary` every 2s behind the same client interface. Topic payload
shapes above still define the TypeScript types either way.
