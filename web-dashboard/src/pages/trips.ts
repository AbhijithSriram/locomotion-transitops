// Trip board: create, dispatch, cancel, and complete trips with real-time UI updates.

import { api } from '../api';
import type { Driver, Trip, Vehicle, Location } from '../types/api';
import { badge, emptyRow, fmtDateTime, fmtNum, loadingRow, TRIP_BADGE, fmtStatus, exportToCsv } from './format';

const STATUSES = ['ALL', 'DRAFT', 'DISPATCHED', 'COMPLETED', 'CANCELLED', 'INTERRUPTED'];

const DEPOTS: Location[] = [
  { name: 'Gandhinagar Depot', lat: 23.2156, lng: 72.6369 },
  { name: 'Ahmedabad Hub', lat: 23.0225, lng: 72.5714 },
  { name: 'Vatva Industrial Area', lat: 22.9676, lng: 72.6469 },
  { name: 'Sanand Warehouse', lat: 22.9922, lng: 72.3822 },
  { name: 'Kalol Depot', lat: 23.2422, lng: 72.4946 },
  { name: 'Mansa Yard', lat: 23.4256, lng: 72.6567 },
  { name: 'Chennai Depot', lat: 13.0827, lng: 80.2707 },
  { name: 'Bangalore Hub', lat: 12.9716, lng: 77.5946 },
];

export function renderTrips(el: HTMLElement): void {
  el.innerHTML = `
    <div class="panel">
      <div class="panel-head">
        <h2>Trip Dispatcher</h2>
        <div style="display: flex; gap: 12px; align-items: center;">
          <div class="filters">
            <select id="f-status">${STATUSES.map((s) => `<option value="${s}">${s === 'ALL' ? 'All statuses' : s}</option>`).join('')}</select>
          </div>
          <button class="btn btn-ghost" id="btn-export-csv">Export CSV</button>
          <button class="btn btn-primary" id="btn-create-trip">+ Create Trip</button>
        </div>
      </div>
      <table class="table">
        <thead><tr>
          <th>Route</th><th>Vehicle</th><th>Driver</th><th>Cargo</th>
          <th>Status</th><th>Dispatched</th><th>Completed</th><th>Actions</th>
        </tr></thead>
        <tbody id="trip-body">${loadingRow(8)}</tbody>
      </table>
      <p class="rule-note">Lifecycle: Draft → Dispatched → Completed / Cancelled (+ Interrupted on breakdown).</p>
    </div>
  `;

  const body = el.querySelector<HTMLTableSectionElement>('#trip-body')!;
  const statusSel = el.querySelector<HTMLSelectElement>('#f-status')!;
  const createBtn = el.querySelector<HTMLButtonElement>('#btn-create-trip')!;
  let all: Trip[] = [];
  let vehiclesById = new Map<string, Vehicle>();
  let driversById = new Map<string, Driver>();

  const draw = () => {
    const rows = all
      .filter((t) => statusSel.value === 'ALL' || t.status === statusSel.value)
      .sort((a, b) => b.createdAt - a.createdAt)
      .map((t) => {
        let actionButtons = '';
        if (t.status === 'DRAFT') {
          actionButtons = `
            <button class="btn-action btn-action-primary btn-dispatch" data-id="${t.id}">Dispatch</button>
            <button class="btn-action btn-action-danger btn-cancel" data-id="${t.id}">Cancel</button>
          `;
        } else if (t.status === 'DISPATCHED') {
          actionButtons = `
            <button class="btn-action btn-action-primary btn-complete" data-id="${t.id}">Complete</button>
            <button class="btn-action btn-action-danger btn-cancel" data-id="${t.id}">Cancel</button>
          `;
        } else {
          actionButtons = `<span class="muted small">—</span>`;
        }

        return `
          <tr>
            <td>${t.source.name} <span class="muted">→</span> ${t.destination.name}</td>
            <td class="mono">${vehiclesById.get(t.vehicleId)?.regNumber ?? '—'}</td>
            <td>${driversById.get(t.driverId)?.name ?? '—'}</td>
            <td>${fmtNum(t.cargoWeightKg)} kg</td>
            <td>${badge(t.status, TRIP_BADGE[t.status])}</td>
            <td class="muted">${fmtDateTime(t.dispatchedAt)}</td>
            <td class="muted">${fmtDateTime(t.completedAt)}</td>
            <td><div style="display: flex; gap: 4px;">${actionButtons}</div></td>
          </tr>`;
      });
    body.innerHTML = rows.join('') || emptyRow(8, 'No trips with this status.');

    // Attach Action Listeners
    body.querySelectorAll('.btn-dispatch').forEach((btn) => {
      btn.addEventListener('click', (e) => {
        const id = (e.target as HTMLButtonElement).dataset.id!;
        void api.dispatchTrip(id).then(refresh).catch((err) => alert(err.message || 'Dispatch failed'));
      });
    });

    body.querySelectorAll('.btn-cancel').forEach((btn) => {
      btn.addEventListener('click', (e) => {
        const id = (e.target as HTMLButtonElement).dataset.id!;
        if (confirm('Are you sure you want to cancel this trip?')) {
          void api.cancelTrip(id).then(refresh).catch((err) => alert(err.message || 'Cancel failed'));
        }
      });
    });

    body.querySelectorAll('.btn-complete').forEach((btn) => {
      btn.addEventListener('click', (e) => {
        const id = (e.target as HTMLButtonElement).dataset.id!;
        const trip = all.find((x) => x.id === id);
        const veh = trip ? vehiclesById.get(trip.vehicleId) : null;
        const currentOdo = veh ? veh.odometer : 0;
        
        const odoInput = prompt(`Enter final odometer reading (must be > ${Math.round(currentOdo)} km):`, String(Math.round(currentOdo) + 10));
        if (odoInput !== null) {
          const finalOdo = Number(odoInput);
          if (isNaN(finalOdo) || finalOdo <= currentOdo) {
            alert('Invalid odometer reading');
            return;
          }
          void api.completeTrip(id, finalOdo).then(refresh).catch((err) => alert(err.message || 'Completion failed'));
        }
      });
    });
  };

  statusSel.onchange = draw;

  const refresh = () => {
    void Promise.all([api.getTrips(), api.getVehicles(), api.getDrivers()]).then(([trips, vehicles, drivers]) => {
      all = trips;
      vehiclesById = new Map(vehicles.map((v) => [v.id, v]));
      driversById = new Map(drivers.map((d) => [d.id, d]));
      draw();
    });
  };

  refresh();

  el.querySelector<HTMLButtonElement>('#btn-export-csv')!.onclick = () => {
    const headers = ['ID', 'Source', 'Destination', 'Vehicle ID', 'Driver ID', 'Cargo Weight (kg)', 'Status', 'Created At', 'Dispatched At', 'Completed At'];
    const rows = all.map(t => [
      t.id, t.source.name, t.destination.name, t.vehicleId, t.driverId, t.cargoWeightKg, t.status, 
      new Date(t.createdAt).toISOString(), 
      t.dispatchedAt ? new Date(t.dispatchedAt).toISOString() : '', 
      t.completedAt ? new Date(t.completedAt).toISOString() : ''
    ]);
    exportToCsv('transitops-trips.csv', headers, rows);
  };

  // Create Trip modal
  createBtn.onclick = () => {
    // Filter available vehicles and drivers
    const availVeh = [...vehiclesById.values()].filter((v) => v.status === 'AVAILABLE');
    const availDrv = [...driversById.values()].filter((d) => d.status === 'AVAILABLE' && d.licenseExpiry > Date.now());

    if (availVeh.length === 0 || availDrv.length === 0) {
      alert('Cannot create trip: Require at least one available vehicle and eligible driver.');
      return;
    }

    const overlay = document.createElement('div');
    overlay.className = 'modal-overlay';
    overlay.innerHTML = `
      <div class="modal">
        <div class="modal-head">
          <h3>Create Trip</h3>
          <button class="btn btn-ghost" id="modal-close">Close</button>
        </div>
        <form id="trip-form">
          <label>Source Depot
            <select name="sourceIdx">
              ${DEPOTS.map((d, i) => `<option value="${i}">${d.name}</option>`).join('')}
            </select>
          </label>
          <label>Destination Depot
            <select name="destIdx">
              ${DEPOTS.map((d, i) => `<option value="${i}">${d.name}</option>`).join('')}
            </select>
          </label>
          <label>Assigned Vehicle
            <select name="vehicleId">
              ${availVeh.map((v) => `<option value="${v.id}">${v.regNumber} (${fmtStatus(v.type)} - max ${v.maxLoadKg}kg)</option>`).join('')}
            </select>
          </label>
          <label>Assigned Driver
            <select name="driverId">
              ${availDrv.map((d) => `<option value="${d.id}">${d.name} (Safety: ${d.safetyScore})</option>`).join('')}
            </select>
          </label>
          <label>Cargo Weight (kg)
            <input type="number" name="cargoWeightKg" placeholder="1200" required />
          </label>
          <div class="modal-foot">
            <button type="submit" class="btn btn-primary">Create Draft</button>
          </div>
        </form>
      </div>
    `;

    document.body.appendChild(overlay);

    overlay.querySelector('#modal-close')!.addEventListener('click', () => overlay.remove());

    const form = overlay.querySelector<HTMLFormElement>('#trip-form')!;
    form.onsubmit = (e) => {
      e.preventDefault();
      const fd = new FormData(form);
      
      const sourceIdx = Number(fd.get('sourceIdx'));
      const destIdx = Number(fd.get('destIdx'));
      if (sourceIdx === destIdx) {
        alert('Source and destination cannot be the same depot.');
        return;
      }

      const source = DEPOTS[sourceIdx];
      const destination = DEPOTS[destIdx];
      const vehicleId = fd.get('vehicleId') as string;
      const driverId = fd.get('driverId') as string;
      const cargoWeightKg = Number(fd.get('cargoWeightKg'));

      // Validate load limit client-side for immediate UX
      const selectedVeh = vehiclesById.get(vehicleId);
      if (selectedVeh && cargoWeightKg > selectedVeh.maxLoadKg) {
        alert(`Warning: Cargo weight (${cargoWeightKg}kg) exceeds vehicle capacity (${selectedVeh.maxLoadKg}kg). Dispatch will fail.`);
      }

      void api.createTrip({ source, destination, vehicleId, driverId, cargoWeightKg }).then(() => {
        overlay.remove();
        refresh();
      }).catch((err) => {
        alert(err.message || 'Error creating trip');
      });
    };
  };
}
