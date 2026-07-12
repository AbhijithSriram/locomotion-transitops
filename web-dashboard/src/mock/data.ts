// Seed fleet for mock mode. Locations are real places around Ahmedabad/Gandhinagar
// (same geography as the hackathon mockup's depot names) so the H3 map drops in
// with no data changes.

import type { Driver, Expense, FuelLog, Location, MaintenanceLog, Trip, Vehicle } from '../types/api';

export const uuid = (): string => crypto.randomUUID();

const now = Date.now();
const days = (n: number) => n * 24 * 60 * 60 * 1000;

export const PLACES: Location[] = [
  { name: 'Gandhinagar Depot', lat: 23.2156, lng: 72.6369 },
  { name: 'Ahmedabad Hub', lat: 23.0225, lng: 72.5714 },
  { name: 'Vatva Industrial Area', lat: 22.9676, lng: 72.6469 },
  { name: 'Sanand Warehouse', lat: 22.9922, lng: 72.3822 },
  { name: 'Kalol Depot', lat: 23.2422, lng: 72.4946 },
  { name: 'Mansa Yard', lat: 23.4256, lng: 72.6567 },
];

function vehicle(partial: Omit<Vehicle, 'id' | 'health'> & { health?: Vehicle['health'] }): Vehicle {
  return {
    id: uuid(),
    health: partial.health ?? { tyres: 90, engine: 92, brakes: 94, riskScore: 10 },
    ...partial,
  };
}

export const vehicles: Vehicle[] = [
  vehicle({ regNumber: 'GJ01AB1531', type: 'VAN', maxLoadKg: 800, odometer: 74000, acquisitionCost: 620000, status: 'ON_TRIP' }),
  vehicle({ regNumber: 'GJ01B9991', type: 'TRUCK', maxLoadKg: 5000, odometer: 182000, acquisitionCost: 2450000, status: 'ON_TRIP' }),
  vehicle({ regNumber: 'GJ01B1120', type: 'MINI_TRUCK', maxLoadKg: 1000, odometer: 66000, acquisitionCost: 410000, status: 'ON_TRIP' }),
  vehicle({ regNumber: 'GJ01B0097', type: 'VAN', maxLoadKg: 750, odometer: 211400, acquisitionCost: 540000, status: 'AVAILABLE' }),
  vehicle({ regNumber: 'GJ01C4410', type: 'TRUCK', maxLoadKg: 6000, odometer: 98000, acquisitionCost: 2800000, status: 'AVAILABLE' }),
  vehicle({ regNumber: 'GJ01D2205', type: 'BIKE', maxLoadKg: 40, odometer: 21000, acquisitionCost: 95000, status: 'AVAILABLE' }),
  vehicle({
    regNumber: 'GJ01E7782', type: 'MINI_TRUCK', maxLoadKg: 1200, odometer: 143000, acquisitionCost: 520000, status: 'IN_SHOP',
    health: { tyres: 55, engine: 40, brakes: 62, riskScore: 68 },
  }),
  vehicle({ regNumber: 'GJ01F0009', type: 'VAN', maxLoadKg: 700, odometer: 305000, acquisitionCost: 480000, status: 'RETIRED' }),
];

function driver(partial: Omit<Driver, 'id'>): Driver {
  return { id: uuid(), ...partial };
}

export const drivers: Driver[] = [
  driver({ name: 'Alex', licenseNumber: 'DL-88213', licenseExpiry: now + days(500), safetyScore: 96, status: 'ON_TRIP' }),
  driver({ name: 'Jane', licenseNumber: 'DL-44120', licenseExpiry: now - days(30), safetyScore: 81, status: 'SUSPENDED' }),
  driver({ name: 'Priya', licenseNumber: 'DL-77031', licenseExpiry: now + days(380), safetyScore: 99, status: 'ON_TRIP' }),
  driver({ name: 'Suresh', licenseNumber: 'DL-90045', licenseExpiry: now + days(200), safetyScore: 88, status: 'ON_TRIP' }),
  driver({ name: 'Meera', licenseNumber: 'DL-12988', licenseExpiry: now + days(700), safetyScore: 93, status: 'AVAILABLE' }),
  driver({ name: 'Ravi', licenseNumber: 'DL-33417', licenseExpiry: now + days(90), safetyScore: 77, status: 'OFF_DUTY' }),
];

function trip(source: Location, destination: Location, vehicleIdx: number, driverIdx: number, cargoWeightKg: number, status: Trip['status']): Trip {
  return {
    id: uuid(),
    source,
    destination,
    vehicleId: vehicles[vehicleIdx].id,
    driverId: drivers[driverIdx].id,
    cargoWeightKg,
    status,
    routePolyline: null,
    createdAt: now - 30 * 60 * 1000,
    dispatchedAt: status === 'DISPATCHED' ? now - 15 * 60 * 1000 : null,
    completedAt: null,
  };
}

function pastTrip(
  source: Location, destination: Location, vehicleIdx: number, driverIdx: number,
  cargoWeightKg: number, endedDaysAgo: number, status: Trip['status'] = 'COMPLETED',
): Trip {
  const ended = now - days(endedDaysAgo);
  return {
    id: uuid(),
    source,
    destination,
    vehicleId: vehicles[vehicleIdx].id,
    driverId: drivers[driverIdx].id,
    cargoWeightKg,
    status,
    routePolyline: null,
    createdAt: ended - 5 * 60 * 60 * 1000,
    // cancelled-from-draft trips were never dispatched
    dispatchedAt: status === 'CANCELLED' ? null : ended - 4 * 60 * 60 * 1000,
    completedAt: status === 'COMPLETED' ? ended : null,
  };
}

export const trips: Trip[] = [
  trip(PLACES[0], PLACES[1], 0, 0, 500, 'DISPATCHED'), // Gandhinagar → Ahmedabad, VAN, Alex
  trip(PLACES[2], PLACES[3], 1, 2, 3800, 'DISPATCHED'), // Vatva → Sanand, TRUCK, Priya
  trip(PLACES[4], PLACES[5], 2, 3, 750, 'DISPATCHED'), // Kalol → Mansa, MINI_TRUCK, Suresh
  trip(PLACES[1], PLACES[4], 4, 4, 2000, 'DRAFT'), // awaiting dispatch
  // history — feeds trips table, fuel logs, and analytics roll-ups
  pastTrip(PLACES[1], PLACES[0], 0, 0, 450, 1),
  pastTrip(PLACES[3], PLACES[1], 1, 2, 4200, 2),
  pastTrip(PLACES[0], PLACES[2], 2, 3, 800, 3),
  pastTrip(PLACES[2], PLACES[1], 4, 4, 2500, 4),
  pastTrip(PLACES[5], PLACES[0], 0, 4, 300, 6),
  pastTrip(PLACES[1], PLACES[3], 4, 0, 1800, 5, 'CANCELLED'),
  pastTrip(PLACES[4], PLACES[1], 6, 3, 900, 8, 'INTERRUPTED'), // breakdown → vehicle now IN_SHOP
];

export const maintenanceLogs: MaintenanceLog[] = [
  {
    id: uuid(), vehicleId: vehicles[6].id, cost: 18500, status: 'OPEN',
    description: 'Coolant system overhaul after mid-route breakdown',
    openedAt: now - days(8), closedAt: null,
  },
  {
    id: uuid(), vehicleId: vehicles[4].id, cost: 12800, status: 'CLOSED',
    description: 'Tyre replacement (front axle)',
    openedAt: now - days(15), closedAt: now - days(14),
  },
  {
    id: uuid(), vehicleId: vehicles[0].id, cost: 3200, status: 'CLOSED',
    description: 'Scheduled service — oil + filters',
    openedAt: now - days(25), closedAt: now - days(24),
  },
  {
    id: uuid(), vehicleId: vehicles[1].id, cost: 4500, status: 'CLOSED',
    description: 'Brake pad replacement',
    openedAt: now - days(40), closedAt: now - days(38),
  },
  {
    id: uuid(), vehicleId: vehicles[7].id, cost: 7000, status: 'CLOSED',
    description: 'Pre-retirement inspection',
    openedAt: now - days(60), closedAt: now - days(59),
  },
];

function fuel(vehicleIdx: number, tripIdx: number | null, liters: number, cost: number, odometer: number, daysAgo: number): FuelLog {
  return {
    id: uuid(),
    vehicleId: vehicles[vehicleIdx].id,
    tripId: tripIdx === null ? null : trips[tripIdx].id,
    liters,
    cost,
    odometer,
    loggedAt: now - days(daysAgo),
  };
}

export const fuelLogs: FuelLog[] = [
  fuel(0, 4, 32, 3360, 73400, 1), // VAN, yesterday's Ahmedabad → Gandhinagar run
  fuel(1, 5, 110, 11550, 181200, 2), // TRUCK, Sanand → Ahmedabad
  fuel(2, 6, 41, 4305, 65300, 3), // MINI_TRUCK, Gandhinagar → Vatva
  fuel(4, 7, 95, 9975, 97300, 4), // TRUCK, Vatva → Ahmedabad
  fuel(0, 8, 24, 2520, 72800, 6), // VAN, Mansa → Gandhinagar
  fuel(6, 10, 36, 3780, 142600, 8), // MINI_TRUCK, partial fill before breakdown
  fuel(1, null, 120, 12600, 179800, 12), // depot refuel, no trip
  fuel(4, null, 100, 10500, 96100, 18), // depot refuel
  fuel(5, null, 6, 630, 20800, 9), // BIKE top-up
];

export const expenses: Expense[] = [
  {
    id: uuid(), vehicleId: vehicles[0].id, tripId: trips[4].id, category: 'TOLL',
    amount: 120, description: 'Sarkhej toll plaza', incurredAt: now - days(1),
  },
  {
    id: uuid(), vehicleId: vehicles[1].id, tripId: trips[5].id, category: 'TOLL',
    amount: 350, description: 'SG Highway toll', incurredAt: now - days(2),
  },
  {
    id: uuid(), vehicleId: vehicles[6].id, tripId: trips[10].id, category: 'OTHER',
    amount: 2500, description: 'Towing charge after breakdown', incurredAt: now - days(8),
  },
  {
    id: uuid(), vehicleId: vehicles[4].id, tripId: null, category: 'INSURANCE',
    amount: 24000, description: 'Annual fleet insurance premium', incurredAt: now - days(20),
  },
  {
    id: uuid(), vehicleId: null, tripId: null, category: 'OTHER',
    amount: 5500, description: 'Depot parking dues', incurredAt: now - days(10),
  },
];
