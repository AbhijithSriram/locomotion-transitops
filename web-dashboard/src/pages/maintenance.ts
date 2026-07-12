// Maintenance records panel with log entry (IN_SHOP) and closure (AVAILABLE) controls.

import { api } from '../api';
import type { Vehicle, MaintenanceLog } from '../types/api';
import { badge, fmtDate, fmtMoney, loadingRow, MAINT_BADGE, fmtStatus } from './format';

export function renderMaintenance(el: HTMLElement): void {
  el.innerHTML = `
    <div class="panel">
      <div class="panel-head">
        <h2>Maintenance Log</h2>
        <div style="display: flex; gap: 12px; align-items: center;">
          <span class="muted small" id="mnt-summary"></span>
          <button class="btn btn-primary" id="btn-add-maint">+ Log Maintenance</button>
        </div>
      </div>
      <table class="table">
        <thead><tr>
          <th>Vehicle</th><th>Description</th><th>Cost</th><th>Status</th><th>Opened</th><th>Closed</th><th>Actions</th>
        </tr></thead>
        <tbody id="mnt-body">${loadingRow(7)}</tbody>
      </table>
      <p class="rule-note">Opening a record moves the vehicle to In Shop; closing it returns the vehicle to Available.</p>
    </div>
  `;

  const body = el.querySelector<HTMLTableSectionElement>('#mnt-body')!;
  const summary = el.querySelector<HTMLElement>('#mnt-summary')!;
  const addBtn = el.querySelector<HTMLButtonElement>('#btn-add-maint')!;
  let vehiclesById = new Map<string, Vehicle>();
  let all: MaintenanceLog[] = [];

  const draw = () => {
    const open = all.filter((l) => l.status === 'OPEN').length;
    const total = all.reduce((acc, l) => acc + l.cost, 0);
    summary.textContent = `${open} open · ${fmtMoney(total)} total cost`;
    body.innerHTML = all
      .sort((a, b) => b.openedAt - a.openedAt)
      .map((l) => {
        const actionBtn = l.status === 'OPEN'
          ? `<button class="btn-action btn-action-primary btn-close-maint" data-id="${l.id}">Close Repair</button>`
          : `<span class="muted small">—</span>`;
        return `
          <tr>
            <td class="mono">${vehiclesById.get(l.vehicleId)?.regNumber ?? '—'}</td>
            <td>${l.description}</td>
            <td>${fmtMoney(l.cost)}</td>
            <td>${badge(l.status, MAINT_BADGE[l.status])}</td>
            <td class="muted">${fmtDate(l.openedAt)}</td>
            <td class="muted">${fmtDate(l.closedAt)}</td>
            <td>${actionBtn}</td>
          </tr>`;
      })
      .join('');

    // Attach listeners
    body.querySelectorAll('.btn-close-maint').forEach((btn) => {
      btn.addEventListener('click', (e) => {
        const id = (e.target as HTMLButtonElement).dataset.id!;
        void api.closeMaintenance(id).then(refresh).catch((err) => alert(err.message || 'Close failed'));
      });
    });
  };

  const refresh = () => {
    void Promise.all([api.getMaintenance(), api.getVehicles()]).then(([logs, vehicles]) => {
      all = logs;
      vehiclesById = new Map(vehicles.map((v) => [v.id, v]));
      draw();
    });
  };

  refresh();

  // Add Maintenance modal
  addBtn.onclick = () => {
    const availVeh = [...vehiclesById.values()].filter((v) => v.status === 'AVAILABLE' || v.status === 'BROKEN_DOWN');
    if (availVeh.length === 0) {
      alert('No available or broken down vehicles to send to maintenance.');
      return;
    }

    const overlay = document.createElement('div');
    overlay.className = 'modal-overlay';
    overlay.innerHTML = `
      <div class="modal">
        <div class="modal-head">
          <h3>Log Maintenance</h3>
          <button class="btn btn-ghost" id="modal-close">Close</button>
        </div>
        <form id="maint-form">
          <label>Select Vehicle
            <select name="vehicleId">
              ${availVeh.map((v) => `<option value="${v.id}">${v.regNumber} (${fmtStatus(v.type)} - Status: ${fmtStatus(v.status)})</option>`).join('')}
            </select>
          </label>
          <label>Repair Description
            <input type="text" name="description" placeholder="Brake pad replacement" required />
          </label>
          <label>Estimated Cost (₹)
            <input type="number" name="cost" placeholder="4500" required />
          </label>
          <div class="modal-foot">
            <button type="submit" class="btn btn-primary">Open Maintenance</button>
          </div>
        </form>
      </div>
    `;

    document.body.appendChild(overlay);

    overlay.querySelector('#modal-close')!.addEventListener('click', () => overlay.remove());

    const form = overlay.querySelector<HTMLFormElement>('#maint-form')!;
    form.onsubmit = (e) => {
      e.preventDefault();
      const fd = new FormData(form);
      const vehicleId = fd.get('vehicleId') as string;
      const description = fd.get('description') as string;
      const cost = Number(fd.get('cost'));

      void api.createMaintenance({ vehicleId, description, cost }).then(() => {
        overlay.remove();
        refresh();
      }).catch((err) => {
        alert(err.message || 'Error opening maintenance log');
      });
    };
  };
}
