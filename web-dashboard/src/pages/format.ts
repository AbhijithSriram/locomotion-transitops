// Shared formatting + enum→badge-class maps for the data pages. Enum keys track
// docs/enums.md exactly.

import type { DriverStatus, MaintenanceStatus, TripStatus, VehicleStatus } from '../types/api';

export const fmtStatus = (s: string): string => s.replaceAll('_', ' ');
export const fmtMoney = (n: number): string => `₹${n.toLocaleString('en-IN')}`;
export const fmtNum = (n: number): string => n.toLocaleString('en-IN');

export const fmtDate = (ts: number | null): string =>
  ts == null ? '—' : new Date(ts).toLocaleDateString(undefined, { day: 'numeric', month: 'short' });

export const fmtDateTime = (ts: number | null): string =>
  ts == null
    ? '—'
    : new Date(ts).toLocaleString(undefined, { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' });

export const VEHICLE_BADGE: Record<VehicleStatus, string> = {
  AVAILABLE: 'ok',
  ON_TRIP: 'info',
  IN_SHOP: 'warn',
  RETIRED: 'muted',
  BROKEN_DOWN: 'bad',
};

export const DRIVER_BADGE: Record<DriverStatus, string> = {
  AVAILABLE: 'ok',
  ON_TRIP: 'info',
  OFF_DUTY: 'muted',
  SUSPENDED: 'bad',
};

export const TRIP_BADGE: Record<TripStatus, string> = {
  DRAFT: 'muted',
  DISPATCHED: 'info',
  COMPLETED: 'ok',
  CANCELLED: 'muted',
  INTERRUPTED: 'bad',
};

export const MAINT_BADGE: Record<MaintenanceStatus, string> = {
  OPEN: 'warn',
  CLOSED: 'ok',
};

export const badge = (text: string, cls: string): string =>
  `<span class="badge badge-${cls}">${fmtStatus(text)}</span>`;

export const riskCls = (score: number): string => (score > 70 ? 'bad' : score > 40 ? 'warn' : 'ok');

export const loadingRow = (cols: number): string =>
  `<tr><td colspan="${cols}" class="muted">Loading…</td></tr>`;

export const emptyRow = (cols: number, msg: string): string =>
  `<tr><td colspan="${cols}" class="muted">${msg}</td></tr>`;
