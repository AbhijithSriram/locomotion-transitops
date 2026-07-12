// Vehicle registry with full CRUD registration & retirement actions.
// Updates both mock & real endpoints seamlessly.

import { api } from '../api';
import type { Vehicle, VehicleType } from '../types/api';
import { badge, emptyRow, exportToCsv, fmtMoney, fmtNum, fmtStatus, loadingRow, riskCls, VEHICLE_BADGE } from './format';

const TYPES = ['ALL', 'TRUCK', 'MINI_TRUCK', 'VAN', 'BIKE'];
const STATUSES = ['ALL', 'AVAILABLE', 'ON_TRIP', 'IN_SHOP', 'RETIRED', 'BROKEN_DOWN'];

const options = (values: string[]) =>
  values.map((v) => `<option value="${v}">${v === 'ALL' ? 'All' : fmtStatus(v)}</option>`).join('');

export function renderVehicles(el: HTMLElement): void {
  el.innerHTML = `
    <div class="panel">
      <div class="panel-head">
        <h2>Vehicle Registry</h2>
        <div style="display: flex; gap: 12px; align-items: center;">
          <div class="filters">
            <select id="f-type">${options(TYPES)}</select>
            <select id="f-status">${options(STATUSES)}</select>
          </div>
          <button class="btn btn-ghost" id="btn-export-csv">Export CSV</button>
          <button class="btn btn-primary" id="btn-add-vehicle">+ Register Vehicle</button>
        </div>
      </div>
      <table class="table">
        <thead><tr>
          <th>Reg No</th><th>Type</th><th>Max Load</th><th>Odometer</th>
          <th>Acquisition</th><th>Status</th><th>Risk</th><th>Actions</th>
        </tr></thead>
        <tbody id="veh-body">${loadingRow(8)}</tbody>
      </table>
      <p class="rule-note">Retired / In-Shop vehicles never appear in the trip dispatcher.</p>
    </div>
  `;

  const body = el.querySelector<HTMLTableSectionElement>('#veh-body')!;
  const typeSel = el.querySelector<HTMLSelectElement>('#f-type')!;
  const statusSel = el.querySelector<HTMLSelectElement>('#f-status')!;
  const addBtn = el.querySelector<HTMLButtonElement>('#btn-add-vehicle')!;
  let all: Vehicle[] = [];

  const draw = () => {
    const rows = all
      .filter((v) => typeSel.value === 'ALL' || v.type === typeSel.value)
      .filter((v) => statusSel.value === 'ALL' || v.status === statusSel.value)
      .map((v) => {
        const canRetire = v.status !== 'RETIRED' && v.status !== 'ON_TRIP';
        const actionBtn = canRetire
          ? `<button class="btn-action btn-action-danger btn-retire" data-id="${v.id}">Retire</button>`
          : `<span class="muted small">—</span>`;
        return `
          <tr>
            <td class="mono">${v.regNumber}</td>
            <td>${fmtStatus(v.type)}</td>
            <td>${fmtNum(v.maxLoadKg)} kg</td>
            <td>${fmtNum(v.odometer)} km</td>
            <td>${fmtMoney(v.acquisitionCost)}</td>
            <td>${badge(v.status, VEHICLE_BADGE[v.status])}</td>
            <td><span class="risk risk-${riskCls(v.health.riskScore)}">${v.health.riskScore}</span></td>
            <td>${actionBtn}</td>
          </tr>`;
      });
    body.innerHTML = rows.join('') || emptyRow(8, 'No vehicles match the filters.');

    // Attach retire listeners
    body.querySelectorAll('.btn-retire').forEach((btn) => {
      btn.addEventListener('click', (e) => {
        const id = (e.target as HTMLButtonElement).dataset.id!;
        if (confirm('Are you sure you want to retire this vehicle? This action is permanent.')) {
          void api.retireVehicle(id).then(() => {
            void api.getVehicles().then((v) => {
              all = v;
              draw();
            });
          });
        }
      });
    });
  };

  typeSel.onchange = draw;
  statusSel.onchange = draw;
  
  const refresh = () => {
    void api.getVehicles().then((v) => {
      all = v;
      draw();
    });
  };

  refresh();

  el.querySelector<HTMLButtonElement>('#btn-export-csv')!.onclick = () => {
    const headers = ['ID', 'Reg Number', 'Type', 'Max Load (kg)', 'Odometer (km)', 'Acquisition Cost', 'Status', 'Risk Score'];
    const rows = all.map(v => [
      v.id, v.regNumber, v.type, v.maxLoadKg, v.odometer, v.acquisitionCost, v.status, v.health.riskScore
    ]);
    exportToCsv('transitops-vehicles.csv', headers, rows);
  };

  // Modal registration setup
  addBtn.onclick = () => {
    const overlay = document.createElement('div');
    overlay.className = 'modal-overlay';
    overlay.innerHTML = `
      <div class="modal">
        <div class="modal-head">
          <h3>Register Vehicle</h3>
          <button class="btn btn-ghost" id="modal-close">Close</button>
        </div>
        <form id="veh-form">
          <label>Reg Number
            <input type="text" name="regNumber" placeholder="e.g. GJ01AB1531" required />
          </label>
          <label>Vehicle Type
            <select name="type">
              <option value="VAN">Van</option>
              <option value="MINI_TRUCK">Mini Truck</option>
              <option value="TRUCK">Truck</option>
              <option value="BIKE">Bike</option>
            </select>
          </label>
          <label>Max Load (kg)
            <input type="number" name="maxLoadKg" placeholder="800" required />
          </label>
          <label>Odometer (km)
            <input type="number" name="odometer" placeholder="0" required />
          </label>
          <label>Acquisition Cost (₹)
            <input type="number" name="acquisitionCost" placeholder="600000" required />
          </label>
          <div class="modal-foot">
            <button type="submit" class="btn btn-primary">Save Vehicle</button>
          </div>
        </form>
      </div>
    `;

    document.body.appendChild(overlay);

    overlay.querySelector('#modal-close')!.addEventListener('click', () => overlay.remove());

    const form = overlay.querySelector<HTMLFormElement>('#veh-form')!;
    form.onsubmit = (e) => {
      e.preventDefault();
      const fd = new FormData(form);
      const regNumber = fd.get('regNumber') as string;
      const type = fd.get('type') as VehicleType;
      const maxLoadKg = Number(fd.get('maxLoadKg'));
      const odometer = Number(fd.get('odometer'));
      const acquisitionCost = Number(fd.get('acquisitionCost'));

      void api.createVehicle({ regNumber, type, maxLoadKg, odometer, acquisitionCost }).then(() => {
        overlay.remove();
        refresh();
      }).catch((err) => {
        alert(err.message || 'Error creating vehicle');
      });
    };
  };
}
