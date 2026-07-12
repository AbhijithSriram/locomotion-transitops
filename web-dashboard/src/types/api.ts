// Mirrors docs/api-contract.md + docs/enums.md — the frozen source of truth.
// All ids are server-generated UUID strings (opaque). All timestamps epoch millis.

export type VehicleStatus = 'AVAILABLE' | 'ON_TRIP' | 'IN_SHOP' | 'RETIRED' | 'BROKEN_DOWN';
export type VehicleType = 'TRUCK' | 'MINI_TRUCK' | 'VAN' | 'BIKE';
export type DriverStatus = 'AVAILABLE' | 'ON_TRIP' | 'OFF_DUTY' | 'SUSPENDED';
export type TripStatus = 'DRAFT' | 'DISPATCHED' | 'COMPLETED' | 'CANCELLED' | 'INTERRUPTED';
export type Role = 'FLEET_MANAGER' | 'SAFETY_OFFICER' | 'FINANCIAL_ANALYST' | 'DRIVER';
export type MaintenanceStatus = 'OPEN' | 'CLOSED';
export type ExpenseCategory = 'FUEL' | 'MAINTENANCE' | 'TOLL' | 'INSURANCE' | 'OTHER';

export interface Location {
  name: string;
  lat: number;
  lng: number;
}

export interface VehicleHealth {
  tyres: number; // 0–100
  engine: number;
  brakes: number;
  riskScore: number; // 0–100, higher = worse
}

export interface Vehicle {
  id: string;
  regNumber: string;
  type: VehicleType;
  maxLoadKg: number;
  odometer: number;
  acquisitionCost: number;
  status: VehicleStatus;
  health: VehicleHealth;
}

export interface Driver {
  id: string;
  name: string;
  licenseNumber: string;
  licenseExpiry: number;
  safetyScore: number;
  status: DriverStatus;
}

export interface Trip {
  id: string;
  source: Location;
  destination: Location;
  vehicleId: string;
  driverId: string;
  cargoWeightKg: number;
  status: TripStatus;
  routePolyline: string | null;
  createdAt: number;
  dispatchedAt: number | null;
  completedAt: number | null;
}

export interface MaintenanceLog {
  id: string;
  vehicleId: string;
  description: string;
  cost: number;
  status: MaintenanceStatus;
  openedAt: number;
  closedAt: number | null;
}

export interface FuelLog {
  id: string;
  vehicleId: string;
  tripId: string | null;
  liters: number;
  cost: number;
  odometer: number;
  loggedAt: number;
}

export interface Expense {
  id: string;
  vehicleId: string | null;
  tripId: string | null;
  category: ExpenseCategory;
  amount: number;
  description: string;
  incurredAt: number;
}

export interface User {
  id: string;
  name: string;
  email: string;
  role: Role;
  driverId?: string | null;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  expiresInMs: number;
  user: User;
}

export interface ReportSummary {
  totalVehicles: number;
  activeTrips: number;
  completedTrips: number;
  totalFuelCost: number;
  totalMaintenanceCost: number;
  totalExpenses: number;
  avgFuelEfficiencyKmPerL: number;
  fleetUtilizationPct: number;
}

export interface ApiError {
  error: { code: string; message: string };
}
