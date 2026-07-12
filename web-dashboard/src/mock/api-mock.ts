import type { Api } from '../api';
import type { LoginResponse, Role, Vehicle, Driver, Trip, MaintenanceLog, FuelLog, Expense, ReportSummary } from '../types/api';
import { drivers, expenses, fuelLogs, maintenanceLogs, trips, uuid, vehicles } from './data';

const delay = <T>(value: T, ms = 250): Promise<T> =>
  new Promise((resolve) => setTimeout(() => resolve(value), ms));

function roleFor(email: string): { role: Role; name: string } {
  const prefix = email.split('@')[0].toLowerCase();
  if (prefix.startsWith('safety')) return { role: 'SAFETY_OFFICER', name: 'Sana' };
  if (prefix.startsWith('finance')) return { role: 'FINANCIAL_ANALYST', name: 'Farid' };
  return { role: 'FLEET_MANAGER', name: 'Rakesh' };
}

const sum = (ns: number[]): number => ns.reduce((a, b) => a + b, 0);

export const mockApi: Api = {
  login(email: string, _password: string): Promise<LoginResponse> {
    const { role, name } = roleFor(email);
    return delay({
      accessToken: `mock-access-${uuid()}`,
      refreshToken: `mock-refresh-${uuid()}`,
      expiresInMs: 12 * 60 * 60 * 1000,
      user: { id: uuid(), name, email, role, driverId: null },
    });
  },
  getVehicles: () => delay([...vehicles]),
  createVehicle(v): Promise<Vehicle> {
    const newVeh: Vehicle = {
      id: uuid(),
      health: { tyres: 100, engine: 100, brakes: 100, riskScore: 0 },
      status: 'AVAILABLE',
      ...v,
    };
    vehicles.push(newVeh);
    return delay(newVeh);
  },
  retireVehicle(id): Promise<Vehicle> {
    const veh = vehicles.find((x) => x.id === id);
    if (veh) veh.status = 'RETIRED';
    return delay(veh!);
  },
  getDrivers: () => delay([...drivers]),
  createDriver(d): Promise<Driver> {
    const newDrv: Driver = {
      id: uuid(),
      safetyScore: 100,
      status: 'AVAILABLE',
      ...d,
    };
    drivers.push(newDrv);
    return delay(newDrv);
  },
  suspendDriver(id): Promise<Driver> {
    const drv = drivers.find((x) => x.id === id);
    if (drv) drv.status = 'SUSPENDED';
    return delay(drv!);
  },
  reinstateDriver(id): Promise<Driver> {
    const drv = drivers.find((x) => x.id === id);
    if (drv) drv.status = 'AVAILABLE';
    return delay(drv!);
  },
  getTrips: () => delay([...trips]),
  createTrip(t): Promise<Trip> {
    const newTrip: Trip = {
      id: uuid(),
      routePolyline: 'gfo}EtohhU_~@~|@_~@~|@', // dummy line
      status: 'DRAFT',
      createdAt: Date.now(),
      dispatchedAt: null,
      completedAt: null,
      ...t,
    };
    trips.push(newTrip);
    return delay(newTrip);
  },
  dispatchTrip(id): Promise<Trip> {
    const trip = trips.find((x) => x.id === id);
    if (trip) {
      trip.status = 'DISPATCHED';
      trip.dispatchedAt = Date.now();
      const veh = vehicles.find((x) => x.id === trip.vehicleId);
      if (veh) veh.status = 'ON_TRIP';
      const drv = drivers.find((x) => x.id === trip.driverId);
      if (drv) drv.status = 'ON_TRIP';
    }
    return delay(trip!);
  },
  completeTrip(id, finalOdometer): Promise<Trip> {
    const trip = trips.find((x) => x.id === id);
    if (trip) {
      trip.status = 'COMPLETED';
      trip.completedAt = Date.now();
      const veh = vehicles.find((x) => x.id === trip.vehicleId);
      if (veh) {
        veh.status = 'AVAILABLE';
        if (finalOdometer) veh.odometer = finalOdometer;
      }
      const drv = drivers.find((x) => x.id === trip.driverId);
      if (drv) drv.status = 'AVAILABLE';
    }
    return delay(trip!);
  },
  cancelTrip(id): Promise<Trip> {
    const trip = trips.find((x) => x.id === id);
    if (trip) {
      trip.status = 'CANCELLED';
      const veh = vehicles.find((x) => x.id === trip.vehicleId);
      if (veh && veh.status === 'ON_TRIP') veh.status = 'AVAILABLE';
      const drv = drivers.find((x) => x.id === trip.driverId);
      if (drv && drv.status === 'ON_TRIP') drv.status = 'AVAILABLE';
    }
    return delay(trip!);
  },
  getMaintenance: () => delay([...maintenanceLogs]),
  createMaintenance(m): Promise<MaintenanceLog> {
    const log: MaintenanceLog = {
      id: uuid(),
      status: 'OPEN',
      openedAt: Date.now(),
      closedAt: null,
      ...m,
    };
    maintenanceLogs.push(log);
    const veh = vehicles.find((x) => x.id === m.vehicleId);
    if (veh) veh.status = 'IN_SHOP';
    return delay(log);
  },
  closeMaintenance(id): Promise<MaintenanceLog> {
    const log = maintenanceLogs.find((x) => x.id === id);
    if (log) {
      log.status = 'CLOSED';
      log.closedAt = Date.now();
      const veh = vehicles.find((x) => x.id === log.vehicleId);
      if (veh) veh.status = 'AVAILABLE';
    }
    return delay(log!);
  },
  getFuelLogs: () => delay([...fuelLogs]),
  createFuelLog(f): Promise<FuelLog> {
    const log: FuelLog = {
      id: uuid(),
      loggedAt: Date.now(),
      vehicleId: f.vehicleId,
      tripId: f.tripId ?? null,
      liters: f.liters,
      cost: f.cost,
      odometer: f.odometer,
    };
    fuelLogs.push(log);
    const veh = vehicles.find((x) => x.id === f.vehicleId);
    if (veh) veh.odometer = f.odometer;
    return delay(log);
  },
  getExpenses: () => delay([...expenses]),
  createExpense(e): Promise<Expense> {
    const exp: Expense = {
      id: uuid(),
      incurredAt: Date.now(),
      vehicleId: e.vehicleId ?? null,
      tripId: e.tripId ?? null,
      category: e.category,
      amount: e.amount,
      description: e.description,
    };
    expenses.push(exp);
    return delay(exp);
  },
  getReportSummary(): Promise<ReportSummary> {
    const totalFuelCost = sum(fuelLogs.map((f) => f.cost));
    const totalMaintenanceCost = sum(maintenanceLogs.map((m) => m.cost));
    const activeFleet = vehicles.filter((v) => v.status !== 'RETIRED');
    const onTrip = activeFleet.filter((v) => v.status === 'ON_TRIP').length;
    return delay({
      totalVehicles: vehicles.length,
      activeTrips: trips.filter((t) => t.status === 'DISPATCHED').length,
      completedTrips: trips.filter((t) => t.status === 'COMPLETED').length,
      totalFuelCost,
      totalMaintenanceCost,
      totalExpenses: totalFuelCost + totalMaintenanceCost + sum(expenses.map((e) => e.amount)),
      avgFuelEfficiencyKmPerL: 8.4,
      fleetUtilizationPct: activeFleet.length === 0 ? 0 : Math.round((onTrip / activeFleet.length) * 100),
    });
  },
  spawnSimTrip(): Promise<void> {
    const avVeh = vehicles.filter((v) => v.status === 'AVAILABLE');
    const avDrv = drivers.filter((d) => d.status === 'AVAILABLE');
    if (avVeh.length && avDrv.length) {
      const v = avVeh[0];
      const d = avDrv[0];
      v.status = 'ON_TRIP';
      d.status = 'ON_TRIP';
      trips.push({
        id: uuid(),
        source: { name: 'Ahmedabad Hub', lat: 23.0225, lng: 72.5714 },
        destination: { name: 'Gandhinagar Depot', lat: 23.2156, lng: 72.6369 },
        vehicleId: v.id,
        driverId: d.id,
        cargoWeightKg: 450,
        status: 'DISPATCHED',
        routePolyline: 'gfo}EtohhU_~@~|@_~@~|@',
        createdAt: Date.now(),
        dispatchedAt: Date.now(),
        completedAt: null,
      });
    }
    return delay(undefined);
  },
  triggerSimBreakdown(vehicleId): Promise<void> {
    const veh = vehicles.find((x) => x.id === vehicleId);
    if (veh) {
      veh.status = 'BROKEN_DOWN';
      veh.health = { tyres: 50, engine: 10, brakes: 40, riskScore: 95 };
      const trip = trips.find((t) => t.vehicleId === vehicleId && t.status === 'DISPATCHED');
      if (trip) trip.status = 'INTERRUPTED';
    }
    return delay(undefined);
  },
  setSimSpeed(_multiplier): Promise<{ multiplier: number }> {
    return delay({ multiplier: _multiplier });
  },
};
