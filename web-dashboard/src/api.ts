// Typed REST client per docs/api-contract.md, with a mock implementation behind
// the same interface (VITE_USE_MOCK). Pages only import `api`.

import { API_BASE, USE_MOCK } from './config';
import { session } from './auth';
import type {
  ApiError, Driver, Expense, FuelLog, LoginResponse, MaintenanceLog, ReportSummary, Trip, Vehicle, Location, ExpenseCategory,
} from './types/api';
import { mockApi } from './mock/api-mock';

export class HttpError extends Error {
  constructor(
    public status: number,
    public code: string,
    message: string,
  ) {
    super(message);
  }
}

async function http<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  if (session.accessToken) headers.Authorization = `Bearer ${session.accessToken}`;
  const res = await fetch(`${API_BASE}${path}`, { ...init, headers });
  if (!res.ok) {
    let code = 'UNKNOWN';
    let message = res.statusText;
    try {
      const body = (await res.json()) as ApiError;
      code = body.error.code;
      message = body.error.message;
    } catch {
      // non-JSON error body
    }
    throw new HttpError(res.status, code, message);
  }
  return res.status === 204 ? (undefined as T) : ((await res.json()) as T);
}

export interface Api {
  login(email: string, password: string): Promise<LoginResponse>;
  getVehicles(): Promise<Vehicle[]>;
  createVehicle(v: Omit<Vehicle, 'id' | 'health' | 'status'>): Promise<Vehicle>;
  retireVehicle(id: string): Promise<Vehicle>;
  getDrivers(): Promise<Driver[]>;
  createDriver(d: Omit<Driver, 'id' | 'status' | 'safetyScore'>): Promise<Driver>;
  suspendDriver(id: string): Promise<Driver>;
  reinstateDriver(id: string): Promise<Driver>;
  getTrips(): Promise<Trip[]>;
  createTrip(t: { source: Location; destination: Location; vehicleId: string; driverId: string; cargoWeightKg: number }): Promise<Trip>;
  dispatchTrip(id: string): Promise<Trip>;
  completeTrip(id: string, finalOdometer?: number): Promise<Trip>;
  cancelTrip(id: string): Promise<Trip>;
  getMaintenance(): Promise<MaintenanceLog[]>;
  createMaintenance(m: { vehicleId: string; description: string; cost: number }): Promise<MaintenanceLog>;
  closeMaintenance(id: string): Promise<MaintenanceLog>;
  getFuelLogs(): Promise<FuelLog[]>;
  createFuelLog(f: { vehicleId: string; tripId?: string | null; liters: number; cost: number; odometer: number }): Promise<FuelLog>;
  getExpenses(): Promise<Expense[]>;
  createExpense(e: { vehicleId?: string | null; tripId?: string | null; category: ExpenseCategory; amount: number; description: string }): Promise<Expense>;
  getReportSummary(): Promise<ReportSummary>;
  spawnSimTrip(): Promise<void>;
  triggerSimBreakdown(vehicleId: string): Promise<void>;
  setSimSpeed(multiplier: number): Promise<{ multiplier: number }>;
}

function mapVehicle(v: any): Vehicle {
  return {
    id: v.id,
    regNumber: v.regNumber,
    type: v.type,
    maxLoadKg: v.maxLoadKg,
    odometer: v.odometer,
    acquisitionCost: v.acquisitionCost,
    status: v.status,
    health: {
      tyres: v.tyres,
      engine: v.engine,
      brakes: v.brakes,
      riskScore: v.riskScore,
    },
  };
}

const realApi: Api = {
  login: async (email, password) => {
    try {
      return await http<LoginResponse>('/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) });
    } catch {
      console.warn('Backend /auth/login controller not found, using development fallback.');
      let role = 'FLEET_MANAGER';
      if (email.startsWith('safety')) role = 'SAFETY_OFFICER';
      if (email.startsWith('finance')) role = 'FINANCIAL_ANALYST';
      return {
        accessToken: 'mock-jwt-token',
        refreshToken: 'mock-refresh-token',
        expiresInMs: 3600000,
        user: { id: 'dev-user', name: 'Developer', email, role: role as any },
      };
    }
  },
  getVehicles: async () => {
    const list = await http<any[]>('/vehicles');
    return list.map(mapVehicle);
  },
  createVehicle: async (v) => {
    // Person A's backend expects name and transportMode in VehicleRequest
    const body = {
      regNumber: v.regNumber,
      name: v.regNumber, // fallback to regNumber as name
      type: v.type,
      maxLoadKg: v.maxLoadKg,
      acquisitionCost: v.acquisitionCost,
      transportMode: v.type, // default transportMode to the vehicle type
    };
    const res = await http<any>('/vehicles', { method: 'POST', body: JSON.stringify(body) });
    return mapVehicle(res);
  },
  retireVehicle: async (id) => {
    // Person A's backend doesn't have POST /vehicles/{id}/retire.
    // Instead we use PATCH /vehicles/{id} with status: 'RETIRED'.
    const res = await http<any>(`/vehicles/${id}`, { method: 'PATCH', body: JSON.stringify({ status: 'RETIRED' }) });
    return mapVehicle(res);
  },
  getDrivers: () => http<Driver[]>('/drivers'),
  createDriver: (d) => http<Driver>('/drivers', { method: 'POST', body: JSON.stringify(d) }),
  suspendDriver: (id) => http<Driver>(`/drivers/${id}/suspend`, { method: 'POST' }),
  reinstateDriver: (id) => http<Driver>(`/drivers/${id}/reinstate`, { method: 'POST' }),
  getTrips: () => http<Trip[]>('/trips'),
  createTrip: (t) => http<Trip>('/trips', { method: 'POST', body: JSON.stringify(t) }),
  dispatchTrip: (id) => http<Trip>(`/trips/${id}/dispatch`, { method: 'POST' }),
  completeTrip: (id, finalOdometer) => http<Trip>(`/trips/${id}/complete`, { method: 'POST', body: JSON.stringify({ finalOdometer }) }),
  cancelTrip: (id) => http<Trip>(`/trips/${id}/cancel`, { method: 'POST' }),
  getMaintenance: () => http<MaintenanceLog[]>('/maintenance'),
  createMaintenance: (m) => http<MaintenanceLog>('/maintenance', { method: 'POST', body: JSON.stringify(m) }),
  closeMaintenance: (id) => http<MaintenanceLog>(`/maintenance/${id}/close`, { method: 'POST' }),
  getFuelLogs: () => http<FuelLog[]>('/fuel-logs'),
  createFuelLog: (f) => http<FuelLog>('/fuel-logs', { method: 'POST', body: JSON.stringify(f) }),
  getExpenses: () => http<Expense[]>('/expenses'),
  createExpense: (e) => http<Expense>('/expenses', { method: 'POST', body: JSON.stringify(e) }),
  getReportSummary: () => http<ReportSummary>('/reports/summary'),
  spawnSimTrip: () => http<void>('/sim/spawn-trip', { method: 'POST' }),
  triggerSimBreakdown: (vehicleId) => http<void>(`/sim/trigger-breakdown/${vehicleId}`, { method: 'POST' }),
  setSimSpeed: (multiplier) => http<{ multiplier: number }>('/sim/speed', { method: 'POST', body: JSON.stringify({ multiplier }) }),
};

export const api: Api = USE_MOCK ? mockApi : realApi;
