// In-browser fake telemetry generator implementing the RealtimeClient interface.
// Emits the exact payloads from docs/events-contract.md so every widget built
// against it works unchanged when the real STOMP feed replaces it.

import { Listeners, type RealtimeClient, type Unsubscribe } from '../ws';
import type { Topic, TopicPayloads } from '../types/events';
import type { Trip, Vehicle } from '../types/api';
import { drivers, trips, vehicles } from './data';

interface MovingTrip {
  trip: Trip;
  vehicle: Vehicle;
  progress: number; // 0 → 1
  durationTicks: number;
}

const TICK_MS = 1000;

function heading(from: { lat: number; lng: number }, to: { lat: number; lng: number }): number {
  const deg = (Math.atan2(to.lng - from.lng, to.lat - from.lat) * 180) / Math.PI;
  return (deg + 360) % 360;
}

export class FakeFeed implements RealtimeClient {
  private listeners = new Listeners();
  private timer: number | null = null;
  private tick = 0;
  private moving: MovingTrip[] = [];
  private brokenDownOnce = false;

  connect(): void {
    if (this.timer !== null) return;
    this.moving = trips
      .filter((t) => t.status === 'DISPATCHED')
      .map((trip, i) => ({
        trip,
        vehicle: vehicles.find((v) => v.id === trip.vehicleId)!,
        progress: 0.15 * (i + 1),
        durationTicks: 180 + i * 60,
      }));
    this.timer = window.setInterval(() => this.step(), TICK_MS);
  }

  disconnect(): void {
    if (this.timer !== null) window.clearInterval(this.timer);
    this.timer = null;
  }

  on<T extends Topic>(topic: T, cb: (event: TopicPayloads[T]) => void): Unsubscribe {
    return this.listeners.add(topic, cb as (e: unknown) => void);
  }

  private step(): void {
    this.tick++;
    const ts = Date.now();

    for (const m of this.moving) {
      if (m.trip.status !== 'DISPATCHED') continue;
      m.progress = Math.min(1, m.progress + 1 / m.durationTicks);
      const { source: s, destination: d } = m.trip;
      const jitter = () => (Math.random() - 0.5) * 0.0006;
      const lat = s.lat + (d.lat - s.lat) * m.progress + jitter();
      const lng = s.lng + (d.lng - s.lng) * m.progress + jitter();
      this.listeners.emit('vehicle-position', {
        vehicleId: m.vehicle.id,
        lat,
        lng,
        speedKmh: 40 + Math.random() * 30,
        heading: heading(s, d),
        tripId: m.trip.id,
        ts,
      });
      if (m.progress >= 1) this.completeTrip(m, ts);
    }

    // Scripted breakdown at ~45s: proves alerts + status pipeline visually.
    if (this.tick === 45 && !this.brokenDownOnce) this.breakdown(ts);

    if (this.tick % 2 === 0) this.emitKpi(ts);
    if (this.tick % 5 === 0) this.emitHealth(ts);
  }

  private completeTrip(m: MovingTrip, ts: number): void {
    m.trip.status = 'COMPLETED';
    m.trip.completedAt = ts;
    m.vehicle.status = 'AVAILABLE';
    const driver = drivers.find((dr) => dr.id === m.trip.driverId);
    if (driver) driver.status = 'AVAILABLE';
    this.listeners.emit('trip-status', {
      tripId: m.trip.id, status: 'COMPLETED', vehicleId: m.vehicle.id, driverId: m.trip.driverId, ts,
    });
    this.listeners.emit('vehicle-status', { vehicleId: m.vehicle.id, status: 'AVAILABLE', ts });
    this.listeners.emit('alerts', {
      type: 'TRIP_COMPLETED', severity: 'INFO',
      message: `${m.vehicle.regNumber} completed ${m.trip.source.name} → ${m.trip.destination.name}`,
      vehicleId: m.vehicle.id, tripId: m.trip.id, ts,
    });
  }

  private breakdown(ts: number): void {
    const m = this.moving.find((x) => x.trip.status === 'DISPATCHED');
    if (!m) return;
    this.brokenDownOnce = true;
    m.trip.status = 'INTERRUPTED';
    m.vehicle.status = 'BROKEN_DOWN';
    m.vehicle.health = { tyres: 62, engine: 18, brakes: 70, riskScore: 91 };
    this.listeners.emit('vehicle-status', { vehicleId: m.vehicle.id, status: 'BROKEN_DOWN', ts });
    this.listeners.emit('trip-status', {
      tripId: m.trip.id, status: 'INTERRUPTED', vehicleId: m.vehicle.id, driverId: m.trip.driverId, ts,
    });
    this.listeners.emit('vehicle-health', { vehicleId: m.vehicle.id, ...m.vehicle.health, ts });
    this.listeners.emit('alerts', {
      type: 'BREAKDOWN', severity: 'CRITICAL',
      message: `${m.vehicle.regNumber} broke down en route to ${m.trip.destination.name}`,
      vehicleId: m.vehicle.id, tripId: m.trip.id, ts,
    });
    window.setTimeout(() => {
      this.listeners.emit('alerts', {
        type: 'RESCUE_DISPATCHED', severity: 'WARN',
        message: `Rescue vehicle dispatched for ${m.vehicle.regNumber}`,
        vehicleId: m.vehicle.id, tripId: m.trip.id, ts: Date.now(),
      });
    }, 8000);
  }

  private emitKpi(ts: number): void {
    const count = (s: Vehicle['status']) => vehicles.filter((v) => v.status === s).length;
    const nonRetired = vehicles.length - count('RETIRED');
    this.listeners.emit('kpi', {
      activeVehicles: count('ON_TRIP'),
      availableVehicles: count('AVAILABLE'),
      inMaintenance: count('IN_SHOP'),
      activeTrips: trips.filter((t) => t.status === 'DISPATCHED').length,
      pendingTrips: trips.filter((t) => t.status === 'DRAFT').length,
      driversOnDuty: drivers.filter((d) => d.status === 'ON_TRIP' || d.status === 'AVAILABLE').length,
      utilizationPct: Math.round((count('ON_TRIP') / nonRetired) * 1000) / 10,
      ts,
    });
  }

  private emitHealth(ts: number): void {
    for (const m of this.moving) {
      if (m.trip.status !== 'DISPATCHED') continue;
      const h = m.vehicle.health;
      h.tyres = Math.max(20, h.tyres - Math.random() * 0.6);
      h.engine = Math.max(20, h.engine - Math.random() * 0.4);
      h.brakes = Math.max(20, h.brakes - Math.random() * 0.5);
      h.riskScore = Math.min(95, Math.round(100 - (h.tyres + h.engine + h.brakes) / 3));
      this.listeners.emit('vehicle-health', {
        vehicleId: m.vehicle.id,
        tyres: Math.round(h.tyres),
        engine: Math.round(h.engine),
        brakes: Math.round(h.brakes),
        riskScore: h.riskScore,
        ts,
      });
    }
  }
}
