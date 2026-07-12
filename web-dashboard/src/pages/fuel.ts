// Fuel logs & Expense trackers with form entry overlays to capture operational spending.

import { api } from '../api';
import type { Vehicle, ExpenseCategory } from '../types/api';
import { badge, fmtDate, fmtMoney, fmtNum, loadingRow } from './format';

export function renderFuel(el: HTMLElement): void {
  el.innerHTML = `
    <section class="kpi-strip" id="fuel-kpis"></section>
    <section class="two-col">
      <div class="panel">
        <div class="panel-head">
          <h2>Fuel Logs</h2>
          <button class="btn btn-primary" id="btn-add-fuel">+ Log Fuel</button>
        </div>
        <table class="table">
          <thead><tr><th>Vehicle</th><th>Liters</th><th>Cost</th><th>₹/L</th><th>Odometer</th><th>Date</th></tr></thead>
          <tbody id="fuel-body">${loadingRow(6)}</tbody>
        </table>
        <p class="rule-note">Depot refuels have no linked trip — the contract allows a null tripId.</p>
      </div>
      <div class="panel">
        <div class="panel-head">
          <h2>Other Expenses</h2>
          <button class="btn btn-primary" id="btn-add-expense">+ Log Expense</button>
        </div>
        <table class="table">
          <thead><tr><th>Category</th><th>Vehicle</th><th>Amount</th><th>Description</th><th>Date</th></tr></thead>
          <tbody id="exp-body">${loadingRow(5)}</tbody>
        </table>
      </div>
    </section>
  `;

  const kpis = el.querySelector<HTMLElement>('#fuel-kpis')!;
  const fuelBody = el.querySelector<HTMLTableSectionElement>('#fuel-body')!;
  const expBody = el.querySelector<HTMLTableSectionElement>('#exp-body')!;
  const addFuelBtn = el.querySelector<HTMLButtonElement>('#btn-add-fuel')!;
  const addExpBtn = el.querySelector<HTMLButtonElement>('#btn-add-expense')!;
  
  let vehiclesById = new Map<string, string>();
  let allVehicles: Vehicle[] = [];

  const refresh = () => {
    void Promise.all([api.getFuelLogs(), api.getExpenses(), api.getVehicles()]).then(
      ([fuelLogs, expenses, vehicles]) => {
        allVehicles = vehicles;
        vehiclesById = new Map(vehicles.map((v) => [v.id, v.regNumber]));
        const fuelTotal = fuelLogs.reduce((acc, f) => acc + f.cost, 0);
        const expTotal = expenses.reduce((acc, e) => acc + e.amount, 0);

        kpis.innerHTML = [
          { label: 'Fuel Cost', value: fmtMoney(fuelTotal) },
          { label: 'Other Expenses', value: fmtMoney(expTotal) },
          { label: 'Total Operational', value: fmtMoney(fuelTotal + expTotal) },
          { label: 'Fuel Entries', value: String(fuelLogs.length) },
        ]
          .map((k) => `<div class="kpi-card"><div class="kpi-value">${k.value}</div><div class="kpi-label">${k.label}</div></div>`)
          .join('');

        fuelBody.innerHTML = fuelLogs
          .sort((a, b) => b.loggedAt - a.loggedAt)
          .map((f) => `
            <tr>
              <td class="mono">${vehiclesById.get(f.vehicleId) ?? '—'}</td>
              <td>${Math.round(f.liters * 10) / 10} L</td>
              <td>${fmtMoney(Math.round(f.cost))}</td>
              <td class="muted">${f.liters > 0 ? (f.cost / f.liters).toFixed(0) : '—'}</td>
              <td class="muted">${fmtNum(Math.round(f.odometer))} km</td>
              <td class="muted">${fmtDate(f.loggedAt)}</td>
            </tr>`)
          .join('');

        expBody.innerHTML = expenses
          .sort((a, b) => b.incurredAt - a.incurredAt)
          .map((e) => `
            <tr>
              <td>${badge(e.category, 'muted')}</td>
              <td class="mono">${e.vehicleId ? vehiclesById.get(e.vehicleId) ?? '—' : '—'}</td>
              <td>${fmtMoney(e.amount)}</td>
              <td>${e.description}</td>
              <td class="muted">${fmtDate(e.incurredAt)}</td>
            </tr>`)
          .join('');
      },
    );
  };

  refresh();

  // Log Fuel Modal
  addFuelBtn.onclick = () => {
    if (allVehicles.length === 0) return;
    const overlay = document.createElement('div');
    overlay.className = 'modal-overlay';
    overlay.innerHTML = `
      <div class="modal">
        <div class="modal-head">
          <h3>Log Fuel Refueling</h3>
          <button class="btn btn-ghost" id="modal-close">Close</button>
        </div>
        <form id="fuel-form">
          <label>Select Vehicle
            <select name="vehicleId">
              ${allVehicles.map((v) => `<option value="${v.id}">${v.regNumber} (Current Odo: ${Math.round(v.odometer)}km)</option>`).join('')}
            </select>
          </label>
          <label>Liters Refueled
            <input type="number" step="0.1" name="liters" placeholder="45.5" required />
          </label>
          <label>Total Cost (₹)
            <input type="number" name="cost" placeholder="4200" required />
          </label>
          <label>Odometer at Refuel (km)
            <input type="number" name="odometer" placeholder="15020" required />
          </label>
          <div class="modal-foot">
            <button type="submit" class="btn btn-primary">Log Fuel</button>
          </div>
        </form>
      </div>
    `;

    document.body.appendChild(overlay);

    overlay.querySelector('#modal-close')!.addEventListener('click', () => overlay.remove());

    const form = overlay.querySelector<HTMLFormElement>('#fuel-form')!;

    // Pre-fill the odometer with the selected vehicle's live reading so logs
    // stay consistent with the simulator; the user can still adjust it.
    const vehicleSelect = form.querySelector<HTMLSelectElement>('select[name="vehicleId"]')!;
    const odoInput = form.querySelector<HTMLInputElement>('input[name="odometer"]')!;
    const syncOdometer = () => {
      const veh = allVehicles.find((v) => v.id === vehicleSelect.value);
      if (veh) odoInput.value = String(Math.round(veh.odometer));
    };
    syncOdometer();
    vehicleSelect.addEventListener('change', syncOdometer);

    form.onsubmit = (e) => {
      e.preventDefault();
      const fd = new FormData(form);
      const vehicleId = fd.get('vehicleId') as string;
      const liters = Number(fd.get('liters'));
      const cost = Number(fd.get('cost'));
      const odometer = Number(fd.get('odometer'));

      const selectedVeh = allVehicles.find(v => v.id === vehicleId);
      if (selectedVeh && odometer < Math.floor(selectedVeh.odometer)) {
        alert(`Odometer reading cannot be lower than current vehicle odometer (${Math.round(selectedVeh.odometer)}km).`);
        return;
      }

      void api.createFuelLog({ vehicleId, liters, cost, odometer }).then(() => {
        overlay.remove();
        refresh();
      }).catch((err) => {
        alert(err.message || 'Error logging fuel');
      });
    };
  };

  // Log Expense Modal
  addExpBtn.onclick = () => {
    const overlay = document.createElement('div');
    overlay.className = 'modal-overlay';
    overlay.innerHTML = `
      <div class="modal">
        <div class="modal-head">
          <h3>Log Other Expense</h3>
          <button class="btn btn-ghost" id="modal-close">Close</button>
        </div>
        <form id="exp-form">
          <label>Category
            <select name="category">
              <option value="TOLL">Toll</option>
              <option value="MAINTENANCE">Maintenance</option>
              <option value="INSURANCE">Insurance</option>
              <option value="OTHER">Other</option>
            </select>
          </label>
          <label>Linked Vehicle (Optional)
            <select name="vehicleId">
              <option value="">None / Fleet-wide</option>
              ${allVehicles.map((v) => `<option value="${v.id}">${v.regNumber}</option>`).join('')}
            </select>
          </label>
          <label>Expense Amount (₹)
            <input type="number" name="amount" placeholder="350" required />
          </label>
          <label>Description / Notes
            <input type="text" name="description" placeholder="NH44 toll plaza fee" required />
          </label>
          <div class="modal-foot">
            <button type="submit" class="btn btn-primary">Log Expense</button>
          </div>
        </form>
      </div>
    `;

    document.body.appendChild(overlay);

    overlay.querySelector('#modal-close')!.addEventListener('click', () => overlay.remove());

    const form = overlay.querySelector<HTMLFormElement>('#exp-form')!;
    form.onsubmit = (e) => {
      e.preventDefault();
      const fd = new FormData(form);
      const category = fd.get('category') as ExpenseCategory;
      const vehIdRaw = fd.get('vehicleId') as string;
      const vehicleId = vehIdRaw ? vehIdRaw : null;
      const amount = Number(fd.get('amount'));
      const description = fd.get('description') as string;

      void api.createExpense({ category, vehicleId, amount, description }).then(() => {
        overlay.remove();
        refresh();
      }).catch((err) => {
        alert(err.message || 'Error logging expense');
      });
    };
  };
}
