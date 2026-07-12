// Reports & analytics with summary reports, CSV downloads, and custom styled high-fidelity cost breakdowns.

import { api } from '../api';
import { USE_MOCK } from '../config';
import type { Driver, Trip, Vehicle } from '../types/api';
import { emptyRow, fmtMoney, loadingRow } from './format';

export function renderAnalytics(el: HTMLElement): void {
  el.innerHTML = `
    <section class="kpi-strip" id="rep-kpis"></section>
    
    <section class="two-col" style="margin-bottom: 20px;">
      <div class="panel">
        <div class="panel-head"><h2>Operational Spend by Category</h2></div>
        <div id="cost-category-chart" style="display: flex; flex-direction: column; gap: 14px; padding: 10px 0;">
          <div class="muted small">Loading spend data...</div>
        </div>
      </div>

      <div class="panel">
        <div class="panel-head"><h2>Driver Safety Leaderboard</h2></div>
        <div id="safety-leaderboard-chart" style="display: flex; flex-direction: column; gap: 12px; padding: 10px 0;">
          <div class="muted small">Loading safety profiles...</div>
        </div>
      </div>
    </section>

    <div class="panel">
      <div class="panel-head">
        <h2>Costliest Vehicles</h2>
        <button class="btn btn-ghost" id="export-csv" disabled>Export CSV</button>
      </div>
      <table class="table">
        <thead><tr><th>Vehicle</th><th>Fuel</th><th>Maintenance</th><th>Other</th><th>Total</th></tr></thead>
        <tbody id="cost-body">${loadingRow(5)}</tbody>
      </table>
      <p class="rule-note">Computed client-side from fuel logs, maintenance records, and expenses.</p>
    </div>
  `;

  const kpis = el.querySelector<HTMLElement>('#rep-kpis')!;
  const costBody = el.querySelector<HTMLTableSectionElement>('#cost-body')!;
  const exportBtn = el.querySelector<HTMLButtonElement>('#export-csv')!;
  const categoryChart = el.querySelector<HTMLElement>('#cost-category-chart')!;
  const safetyChart = el.querySelector<HTMLElement>('#safety-leaderboard-chart')!;

  void Promise.all([
    api.getReportSummary(),
    api.getVehicles(),
    api.getDrivers(),
    api.getTrips(),
    api.getFuelLogs(),
    api.getMaintenance(),
    api.getExpenses(),
  ]).then(([summary, vehicles, drivers, trips, fuelLogs, maintenance, expenses]) => {
    kpis.innerHTML = [
      { label: 'Total Vehicles', value: String(summary.totalVehicles) },
      { label: 'Active Trips', value: String(summary.activeTrips) },
      { label: 'Completed Trips', value: String(summary.completedTrips) },
      { label: 'Fuel Cost', value: fmtMoney(summary.totalFuelCost) },
      { label: 'Maintenance Cost', value: fmtMoney(summary.totalMaintenanceCost) },
      { label: 'Total Expenses', value: fmtMoney(summary.totalExpenses) },
      { label: 'Avg Efficiency', value: `${summary.avgFuelEfficiencyKmPerL} km/L` },
      { label: 'Fleet Utilization', value: `${summary.fleetUtilizationPct}%` },
    ]
      .map((k) => `<div class="kpi-card"><div class="kpi-value">${k.value}</div><div class="kpi-label">${k.label}</div></div>`)
      .join('');

    // 1. Cost Category Chart
    const fuelTotal = summary.totalFuelCost;
    const maintTotal = summary.totalMaintenanceCost;
    const tollTotal = expenses.filter(e => e.category === 'TOLL').reduce((acc, e) => acc + e.amount, 0);
    const insTotal = expenses.filter(e => e.category === 'INSURANCE').reduce((acc, e) => acc + e.amount, 0);
    const otherTotal = expenses.filter(e => e.category === 'OTHER').reduce((acc, e) => acc + e.amount, 0);
    const totalOperational = fuelTotal + maintTotal + tollTotal + insTotal + otherTotal;

    const cats = [
      { name: 'Fuel Refueling', amount: fuelTotal, color: '#3fce7a' },
      { name: 'Repairs & Maintenance', amount: maintTotal, color: '#ffb020' },
      { name: 'Highway Tolls', amount: tollTotal, color: '#5aa2ff' },
      { name: 'Fleet Insurance', amount: insTotal, color: '#90045c' },
      { name: 'Miscellaneous Expenses', amount: otherTotal, color: '#8b94a9' },
    ].sort((a, b) => b.amount - a.amount);

    categoryChart.innerHTML = cats.map(c => {
      const pct = totalOperational > 0 ? (c.amount / totalOperational) * 100 : 0;
      return `
        <div>
          <div style="display: flex; justify-content: space-between; font-size: 13px; margin-bottom: 4px;">
            <span>${c.name} <span class="muted">(${pct.toFixed(0)}%)</span></span>
            <strong>${fmtMoney(c.amount)}</strong>
          </div>
          <div style="background: var(--line); height: 8px; border-radius: 4px; overflow: hidden;">
            <div style="background: ${c.color}; height: 100%; width: ${pct}%; transition: width 0.5s ease;"></div>
          </div>
        </div>
      `;
    }).join('');

    // 2. Driver Safety Scores
    const sortedDrivers = [...drivers].sort((a, b) => b.safetyScore - a.safetyScore).slice(0, 5);
    safetyChart.innerHTML = sortedDrivers.map(d => {
      const color = d.safetyScore >= 90 ? 'var(--ok)' : d.safetyScore >= 75 ? 'var(--warn)' : 'var(--bad)';
      return `
        <div>
          <div style="display: flex; justify-content: space-between; font-size: 13px; margin-bottom: 4px;">
            <span>${d.name} <span class="muted">(${d.status})</span></span>
            <strong style="color: ${color};">${d.safetyScore} pts</strong>
          </div>
          <div style="background: var(--line); height: 6px; border-radius: 3px; overflow: hidden;">
            <div style="background: ${color}; height: 100%; width: ${d.safetyScore}%; transition: width 0.5s ease;"></div>
          </div>
        </div>
      `;
    }).join('');

    // 3. Costliest Vehicles Table calculation
    const costs = new Map<string, { fuel: number; maint: number; other: number }>();
    const bucket = (vehicleId: string) => {
      let c = costs.get(vehicleId);
      if (!c) {
        c = { fuel: 0, maint: 0, other: 0 };
        costs.set(vehicleId, c);
      }
      return c;
    };
    fuelLogs.forEach((f) => (bucket(f.vehicleId).fuel += f.cost));
    maintenance.forEach((m) => (bucket(m.vehicleId).maint += m.cost));
    expenses.forEach((e) => e.vehicleId && (bucket(e.vehicleId).other += e.amount));

    const regById = new Map(vehicles.map((v) => [v.id, v.regNumber]));
    const rows = [...costs.entries()]
      .map(([id, c]) => ({ reg: regById.get(id) ?? '—', ...c, total: c.fuel + c.maint + c.other }))
      .sort((a, b) => b.total - a.total)
      .slice(0, 5)
      .map((r) => `
        <tr>
          <td class="mono">${r.reg}</td>
          <td>${fmtMoney(r.fuel)}</td>
          <td>${fmtMoney(r.maint)}</td>
          <td>${fmtMoney(r.other)}</td>
          <td><strong>${fmtMoney(r.total)}</strong></td>
        </tr>`);
    costBody.innerHTML = rows.join('') || emptyRow(5, 'No cost data yet.');

    exportBtn.disabled = false;
    exportBtn.onclick = () => {
      if (!USE_MOCK) {
        // live backend serves the authoritative export
        void api.downloadReportCsv();
        return;
      }
      downloadCsv(trips, vehicles, drivers, fuelLogs);
    };
  });
}

function downloadCsv(trips: Trip[], vehicles: Vehicle[], drivers: Driver[], fuelLogs: { tripId: string | null; cost: number }[]): void {
  const regById = new Map(vehicles.map((v) => [v.id, v.regNumber]));
  const nameById = new Map(drivers.map((d) => [d.id, d.name]));
  const fuelByTrip = new Map<string, number>();
  fuelLogs.forEach((f) => f.tripId && fuelByTrip.set(f.tripId, (fuelByTrip.get(f.tripId) ?? 0) + f.cost));

  const header = ['tripId', 'source', 'destination', 'vehicle', 'driver', 'status', 'cargoWeightKg', 'createdAt', 'completedAt', 'fuelCost'];
  const lines = trips.map((t) =>
    [
      t.id,
      t.source.name,
      t.destination.name,
      regById.get(t.vehicleId) ?? '',
      nameById.get(t.driverId) ?? '',
      t.status,
      t.cargoWeightKg,
      t.createdAt,
      t.completedAt ?? '',
      fuelByTrip.get(t.id) ?? 0,
    ]
      .map((v) => `"${String(v).replaceAll('"', '""')}"`)
      .join(','),
  );
  const blob = new Blob([[header.join(','), ...lines].join('\n')], { type: 'text/csv' });
  const a = document.createElement('a');
  a.href = URL.createObjectURL(blob);
  a.download = 'transitops-trips.csv';
  a.click();
  URL.revokeObjectURL(a.href);
}
