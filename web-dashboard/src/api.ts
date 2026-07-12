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

// Access tokens from Person A's backend expire after 15 minutes. On the first
// 401/403 we refresh once (deduplicated across concurrent calls) and retry;
// if the refresh fails the session is dead, so drop back to the login page.
let refreshInFlight: Promise<boolean> | null = null;

async function refreshTokens(): Promise<boolean> {
  const refreshToken = session.refreshToken;
  if (!refreshToken) return false;
  try {
    const res = await fetch(`${API_BASE}/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    });
    if (!res.ok) return false;
    const body = await res.json();
    session.updateTokens(body.accessToken, body.refreshToken);
    return true;
  } catch {
    return false;
  }
}

function tryRefresh(): Promise<boolean> {
  refreshInFlight ??= refreshTokens().finally(() => {
    refreshInFlight = null;
  });
  return refreshInFlight;
}

async function http<T>(path: string, init: RequestInit = {}, retried = false): Promise<T> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  if (session.accessToken) headers.Authorization = `Bearer ${session.accessToken}`;
  const res = await fetch(`${API_BASE}${path}`, { ...init, headers });
  if (!res.ok) {
    // The backend answers expired/invalid tokens with 401 or 403 (no auth entry
    // point configured, so 403 is the common case).
    if ((res.status === 401 || res.status === 403) && !retried && !path.startsWith('/auth/') && session.refreshToken) {
      if (await tryRefresh()) return http<T>(path, init, true);
      session.clear();
      location.reload();
    }
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
  // Some endpoints (e.g. /sim/spawn-trip) return 200/202 with an empty body.
  const text = await res.text();
  return text ? (JSON.parse(text) as T) : (undefined as T);
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
  downloadReportCsv(): Promise<void>;
  spawnSimTrip(): Promise<void>;
  triggerSimBreakdown(vehicleId: string): Promise<void>;
  setSimSpeed(multiplier: number): Promise<{ multiplier: number }>;
}

// Person A's backend serializes dates as ISO strings; the dashboard uses epoch millis.
function ts(value: string | null | undefined): number | null {
  return value ? Date.parse(value) : null;
}

function isoDate(millis: number): string {
  return new Date(millis).toISOString().slice(0, 10);
}

function notFound(err: unknown): boolean {
  return err instanceof HttpError && err.status === 404;
}

function haversineKm(a: Location, b: Location): number {
  const rad = (d: number) => (d * Math.PI) / 180;
  const dLat = rad(b.lat - a.lat);
  const dLng = rad(b.lng - a.lng);
  const h = Math.sin(dLat / 2) ** 2 + Math.cos(rad(a.lat)) * Math.cos(rad(b.lat)) * Math.sin(dLng / 2) ** 2;
  return 6371 * 2 * Math.asin(Math.sqrt(h));
}

function mapDriver(d: any): Driver {
  return {
    id: d.id,
    name: d.name,
    licenseNumber: d.licenseNumber,
    licenseExpiry: ts(d.licenseExpiry) ?? 0,
    safetyScore: d.safetyScore,
    status: d.status,
  };
}

function mapTrip(t: any): Trip {
  return {
    id: t.id,
    // Backend stores locations as plain names; only `.name` is rendered.
    source: { name: t.source, lat: 0, lng: 0 },
    destination: { name: t.destination, lat: 0, lng: 0 },
    vehicleId: t.vehicleId,
    driverId: t.driverId,
    cargoWeightKg: t.cargoWeightKg,
    status: t.status,
    routePolyline: t.routePolyline ?? null,
    createdAt: ts(t.createdAt) ?? 0,
    dispatchedAt: ts(t.dispatchedAt),
    completedAt: ts(t.completedAt),
  };
}

// Person A's maintenance API uses `type` for the description, `createdAt` for
// the opened timestamp, and lowercase "active"/"closed" statuses.
function mapMaintenance(m: any): MaintenanceLog {
  return {
    id: m.id,
    vehicleId: m.vehicleId,
    description: m.type ?? m.description ?? '',
    cost: m.cost,
    status: String(m.status).toLowerCase() === 'closed' ? 'CLOSED' : 'OPEN',
    openedAt: ts(m.createdAt ?? m.openedAt) ?? 0,
    closedAt: ts(m.closedAt),
  };
}

function mapFuelLog(f: any): FuelLog {
  return { ...f, tripId: f.tripId ?? null, loggedAt: ts(f.loggedAt) ?? 0 };
}

function mapExpense(e: any): Expense {
  return { ...e, vehicleId: e.vehicleId ?? null, tripId: e.tripId ?? null, incurredAt: ts(e.incurredAt) ?? 0 };
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
    let res: any;
    try {
      res = await http<any>('/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) });
    } catch (err) {
      // Real auth failures (bad credentials → 401/403) must reach the login form.
      // Fall back to a dev session only when the endpoint doesn't exist yet.
      if (err instanceof HttpError && err.status !== 404) {
        throw new HttpError(err.status, err.code, err.message || 'Invalid email or password');
      }
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
    if (res.user) return res as LoginResponse;
    // Person A's backend returns a flat { accessToken, refreshToken, role, email }
    // instead of the contract's { ..., expiresInMs, user }. Build the user object.
    const name = String(res.email ?? email).split('@')[0];
    return {
      accessToken: res.accessToken,
      refreshToken: res.refreshToken,
      expiresInMs: res.expiresInMs ?? 12 * 60 * 60 * 1000,
      user: { id: res.email ?? email, name, email: res.email ?? email, role: res.role, driverId: null },
    };
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
  getDrivers: async () => (await http<any[]>('/drivers')).map(mapDriver),
  createDriver: async (d) => {
    // Person A's DriverRequest needs licenseCategory (non-blank) and an ISO date.
    const body = {
      name: d.name,
      licenseNumber: d.licenseNumber,
      licenseCategory: 'LMV',
      licenseExpiry: isoDate(d.licenseExpiry),
    };
    return mapDriver(await http<any>('/drivers', { method: 'POST', body: JSON.stringify(body) }));
  },
  // Person A's backend has no suspend/reinstate endpoints; PATCH the status instead.
  suspendDriver: async (id) => mapDriver(await http<any>(`/drivers/${id}`, { method: 'PATCH', body: JSON.stringify({ status: 'SUSPENDED' }) })),
  reinstateDriver: async (id) => mapDriver(await http<any>(`/drivers/${id}`, { method: 'PATCH', body: JSON.stringify({ status: 'AVAILABLE' }) })),
  getTrips: async () => (await http<any[]>('/trips')).map(mapTrip),
  createTrip: async (t) => {
    // Person A's TripCreateRequest wants location names plus a positive plannedDistanceKm.
    const body = {
      source: t.source.name,
      destination: t.destination.name,
      vehicleId: t.vehicleId,
      driverId: t.driverId,
      cargoWeightKg: t.cargoWeightKg,
      plannedDistanceKm: Math.max(1, Math.round(haversineKm(t.source, t.destination))),
    };
    return mapTrip(await http<any>('/trips', { method: 'POST', body: JSON.stringify(body) }));
  },
  // Trip actions are PATCH (not POST) on Person A's backend; complete requires a body.
  dispatchTrip: async (id) => mapTrip(await http<any>(`/trips/${id}/dispatch`, { method: 'PATCH' })),
  completeTrip: async (id, finalOdometer) =>
    mapTrip(await http<any>(`/trips/${id}/complete`, { method: 'PATCH', body: JSON.stringify({ finalOdometer: finalOdometer ?? 0, actualDistanceKm: null }) })),
  cancelTrip: async (id) => mapTrip(await http<any>(`/trips/${id}/cancel`, { method: 'PATCH' })),
  // Maintenance / fuel / expenses / reports don't exist on Person A's backend yet.
  // Reads degrade to empty data so the pages stay usable; writes surface the 404.
  getMaintenance: async () => {
    try {
      return (await http<any[]>('/maintenance')).map(mapMaintenance);
    } catch (err) {
      if (notFound(err)) return [];
      throw err;
    }
  },
  createMaintenance: async (m) =>
    mapMaintenance(await http<any>('/maintenance', {
      method: 'POST',
      body: JSON.stringify({ vehicleId: m.vehicleId, type: m.description, cost: m.cost }),
    })),
  closeMaintenance: async (id) => mapMaintenance(await http<any>(`/maintenance/${id}/close`, { method: 'PATCH' })),
  getFuelLogs: async () => {
    try {
      return (await http<any[]>('/fuel-logs')).map(mapFuelLog);
    } catch (err) {
      if (notFound(err)) return [];
      throw err;
    }
  },
  createFuelLog: async (f) => mapFuelLog(await http<any>('/fuel-logs', { method: 'POST', body: JSON.stringify(f) })),
  getExpenses: async () => {
    try {
      return (await http<any[]>('/expenses')).map(mapExpense);
    } catch (err) {
      if (notFound(err)) return [];
      throw err;
    }
  },
  createExpense: async (e) => mapExpense(await http<any>('/expenses', { method: 'POST', body: JSON.stringify(e) })),
  getReportSummary: async () => {
    try {
      const raw = await http<any>('/reports/summary');
      return {
        totalVehicles: raw.totalVehicles || 0,
        activeTrips: (raw.tripsByStatus?.['DISPATCHED'] || 0) + (raw.tripsByStatus?.['IN_PROGRESS'] || 0),
        completedTrips: raw.tripsByStatus?.['COMPLETED'] || 0,
        totalFuelCost: raw.totalFuelCost || 0,
        totalMaintenanceCost: raw.totalMaintenanceCost || 0,
        totalExpenses: raw.totalExpenseCost || 0,
        avgFuelEfficiencyKmPerL: 0,
        fleetUtilizationPct: raw.totalVehicles ? Math.round(((raw.vehiclesByStatus?.['ON_TRIP'] || 0) / raw.totalVehicles) * 100) : 0,
      };
    } catch (err) {
      if (!notFound(err)) throw err;
      // No reports endpoint yet — compute what we can from vehicles + trips.
      const [vehicles, trips] = await Promise.all([api.getVehicles(), api.getTrips()]);
      const onTrip = vehicles.filter((v) => v.status === 'ON_TRIP').length;
      return {
        totalVehicles: vehicles.length,
        activeTrips: trips.filter((t) => t.status === 'DISPATCHED').length,
        completedTrips: trips.filter((t) => t.status === 'COMPLETED').length,
        totalFuelCost: 0,
        totalMaintenanceCost: 0,
        totalExpenses: 0,
        avgFuelEfficiencyKmPerL: 0,
        fleetUtilizationPct: vehicles.length ? Math.round((onTrip / vehicles.length) * 100) : 0,
      };
    }
  },
  downloadReportCsv: async () => {
    const res = await fetch(`${API_BASE}/reports/export.csv`, {
      headers: { 'Authorization': `Bearer ${session.token}` },
    });
    if (!res.ok) throw new Error('Failed to download CSV');
    const blob = await res.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'transitops-report.csv';
    a.click();
    URL.revokeObjectURL(url);
  },
  spawnSimTrip: () => http<void>('/sim/spawn-trip', { method: 'POST' }),
  triggerSimBreakdown: (vehicleId) => http<void>(`/sim/trigger-breakdown/${vehicleId}`, { method: 'POST' }),
  setSimSpeed: (multiplier) => http<{ multiplier: number }>('/sim/speed', { method: 'POST', body: JSON.stringify({ multiplier }) }),
};

export const api: Api = USE_MOCK ? mockApi : realApi;
