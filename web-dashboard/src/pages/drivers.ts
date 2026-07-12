// Drivers registry with full driver recruitment (CRUD) and suspend/reinstate controls.

import { api } from '../api';
import type { Driver } from '../types/api';
import { badge, DRIVER_BADGE, fmtDate, loadingRow, exportToCsv } from './format';

export function renderDrivers(el: HTMLElement): void {
  el.innerHTML = `
    <div class="panel">
      <div class="panel-head">
        <h2>Drivers &amp; Safety Profiles</h2>
        <div style="display: flex; gap: 12px; align-items: center;">
          <button class="btn btn-ghost" id="btn-export-csv">Export CSV</button>
          <button class="btn btn-primary" id="btn-add-driver">+ Add Driver</button>
        </div>
      </div>
      <table class="table">
        <thead><tr>
          <th>Name</th><th>License No</th><th>License Expiry</th>
          <th>Safety Score</th><th>Status</th><th>Assignment</th><th>Actions</th>
        </tr></thead>
        <tbody id="drv-body">${loadingRow(7)}</tbody>
      </table>
      <p class="rule-note">Suspended or expired-license drivers are automatically blocked from trip assignments.</p>
    </div>
  `;

  const body = el.querySelector<HTMLTableSectionElement>('#drv-body')!;
  const addBtn = el.querySelector<HTMLButtonElement>('#btn-add-driver')!;
  let all: Driver[] = [];

  const draw = () => {
    const now = Date.now();
    body.innerHTML = all
      .map((d) => {
        const expired = d.licenseExpiry < now;
        const blocked = expired || d.status === 'SUSPENDED';
        const scoreCls = d.safetyScore >= 90 ? 'ok' : d.safetyScore >= 75 ? 'warn' : 'bad';
        
        let actionBtn = '';
        if (d.status === 'SUSPENDED') {
          actionBtn = `<button class="btn-action btn-action-primary btn-reinstate" data-id="${d.id}">Reinstate</button>`;
        } else if (d.status !== 'ON_TRIP') {
          actionBtn = `<button class="btn-action btn-action-danger btn-suspend" data-id="${d.id}">Suspend</button>`;
        } else {
          actionBtn = `<span class="muted small">—</span>`;
        }

        return `
          <tr>
            <td>${d.name}</td>
            <td class="mono">${d.licenseNumber}</td>
            <td class="${expired ? 'cell-bad' : ''}">${fmtDate(d.licenseExpiry)}${expired ? ' (expired)' : ''}</td>
            <td><span class="risk risk-${scoreCls}">${d.safetyScore}</span></td>
            <td>${badge(d.status, DRIVER_BADGE[d.status])}</td>
            <td>${blocked ? badge('BLOCKED', 'bad') : badge('ELIGIBLE', 'ok')}</td>
            <td>${actionBtn}</td>
          </tr>`;
      })
      .join('');

    // Attach listeners
    body.querySelectorAll('.btn-suspend').forEach((btn) => {
      btn.addEventListener('click', (e) => {
        const id = (e.target as HTMLButtonElement).dataset.id!;
        void api.suspendDriver(id).then(refresh);
      });
    });

    body.querySelectorAll('.btn-reinstate').forEach((btn) => {
      btn.addEventListener('click', (e) => {
        const id = (e.target as HTMLButtonElement).dataset.id!;
        void api.reinstateDriver(id).then(refresh);
      });
    });
  };

  const refresh = () => {
    void api.getDrivers().then((drivers) => {
      all = drivers;
      draw();
    });
  };

  refresh();

  el.querySelector<HTMLButtonElement>('#btn-export-csv')!.onclick = () => {
    const headers = ['ID', 'Name', 'License Number', 'License Category', 'Contact', 'License Expiry', 'Safety Score', 'Status'];
    const rows = all.map(d => [
      d.id, d.name, d.licenseNumber, d.licenseCategory, d.contact, new Date(d.licenseExpiry).toISOString(), d.safetyScore, d.status
    ]);
    exportToCsv('transitops-drivers.csv', headers, rows);
  };

  // Add driver modal setup
  addBtn.onclick = () => {
    const overlay = document.createElement('div');
    overlay.className = 'modal-overlay';
    overlay.innerHTML = `
      <div class="modal">
        <div class="modal-head">
          <h3>Add Driver</h3>
          <button class="btn btn-ghost" id="modal-close">Close</button>
        </div>
        <form id="drv-form">
          <label>Full Name
            <input type="text" name="name" placeholder="Alex John" required />
          </label>
          <label>License Number
            <input type="text" name="licenseNumber" placeholder="e.g. DL-88213" required />
          </label>
          <label>License Expiry Date
            <input type="date" name="licenseExpiry" required />
          </label>
          <div class="modal-foot">
            <button type="submit" class="btn btn-primary">Save Driver</button>
          </div>
        </form>
      </div>
    `;

    document.body.appendChild(overlay);

    overlay.querySelector('#modal-close')!.addEventListener('click', () => overlay.remove());

    const form = overlay.querySelector<HTMLFormElement>('#drv-form')!;
    form.onsubmit = (e) => {
      e.preventDefault();
      const fd = new FormData(form);
      const name = fd.get('name') as string;
      const licenseNumber = fd.get('licenseNumber') as string;
      const expiryStr = fd.get('licenseExpiry') as string;
      const licenseExpiry = new Date(expiryStr).getTime();

      void api.createDriver({ name, licenseNumber, licenseExpiry }).then(() => {
        overlay.remove();
        refresh();
      }).catch((err) => {
        alert(err.message || 'Error creating driver');
      });
    };
  };
}
