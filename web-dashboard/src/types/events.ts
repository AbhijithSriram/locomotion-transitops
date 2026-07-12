// Mirrors docs/events-contract.md — STOMP topic payloads. All timestamps epoch millis.

import type { TripStatus, VehicleStatus } from './api';

export type AlertType =
  | 'BREAKDOWN'
  | 'HIGH_RISK'
  | 'RESCUE_DISPATCHED'
  | 'TRIP_COMPLETED'
  | 'LICENSE_EXPIRING'
  | 'MAINTENANCE_DUE';

export type AlertSeverity = 'INFO' | 'WARN' | 'CRITICAL';

export interface VehiclePositionEvent {
  vehicleId: string;
  lat: number;
  lng: number;
  speedKmh: number;
  heading: number; // degrees clockwise from north
  tripId: string;
  ts: number;
}

export interface VehicleStatusEvent {
  vehicleId: string;
  status: VehicleStatus;
  ts: number;
}

export interface VehicleHealthEvent {
  vehicleId: string;
  tyres: number;
  engine: number;
  brakes: number;
  riskScore: number;
  ts: number;
}

export interface TripStatusEvent {
  tripId: string;
  status: TripStatus;
  vehicleId: string;
  driverId: string;
  ts: number;
}

export interface AlertEvent {
  type: AlertType;
  severity: AlertSeverity;
  message: string;
  vehicleId: string | null;
  tripId: string | null;
  ts: number;
}

export interface KpiEvent {
  activeVehicles: number;
  availableVehicles: number;
  inMaintenance: number;
  activeTrips: number;
  pendingTrips: number;
  driversOnDuty: number;
  utilizationPct: number;
  ts: number;
}

/** Topic name → payload type. Topic string on the wire is `/topic/${Topic}`. */
export interface TopicPayloads {
  'vehicle-position': VehiclePositionEvent;
  'vehicle-status': VehicleStatusEvent;
  'vehicle-health': VehicleHealthEvent;
  'trip-status': TripStatusEvent;
  alerts: AlertEvent;
  kpi: KpiEvent;
}

export type Topic = keyof TopicPayloads;
