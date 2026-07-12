// Settings / god-mode panel with simulator controls for live demo manipulation.

import { api } from '../api';
import type { Vehicle } from '../types/api';

export function renderSettings(el: HTMLElement): () => void {
  el.innerHTML = `
    <div class="panel" style="margin-bottom: 20px;">
      <div class="panel-head">
        <h2>God Mode &amp; Simulation Control</h2>
        <span class="badge badge-warn">Demo Controller</span>
      </div>
      <div style="display: flex; flex-direction: column; gap: 20px; margin-top: 10px;">
        <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 16px;">
          
          <div class="panel" style="background: var(--panel-2); display: flex; flex-direction: column; gap: 12px;">
            <h3>Simulation Speed</h3>
            <div style="display: flex; align-items: center; gap: 12px;">
              <input type="range" id="sim-speed-range" min="1" max="60" value="1" style="flex: 1;" />
              <span id="sim-speed-val" class="mono" style="font-weight: bold; width: 36px;">1x</span>
            </div>
            <p class="muted small" style="margin: 0;">Speeds up movement, health wear, and trip completions.</p>
          </div>

          <div class="panel" style="background: var(--panel-2); display: flex; flex-direction: column; gap: 12px;">
            <h3>Spawn Active Trip</h3>
            <button class="btn btn-primary" id="btn-spawn-trip">Spawn Simulated Trip</button>
            <p class="muted small" style="margin: 0;">Automatically matches an available vehicle and driver to start a live trip.</p>
          </div>

          <div class="panel" style="background: var(--panel-2); display: flex; flex-direction: column; gap: 12px;">
            <h3>Force Breakdown</h3>
            <div style="display: flex; gap: 8px;">
              <select id="sel-breakdown-veh" style="flex: 1; padding: 6px 10px; border-radius: 8px; background: var(--panel); border: 1px solid var(--line); color: var(--ink);">
                <option value="">No active vehicles</option>
              </select>
              <button class="btn btn-danger" id="btn-force-breakdown" style="padding: 6px 12px; font-size: 13px;">Break</button>
            </div>
            <p class="muted small" style="margin: 0;">Forces breakdown, halts vehicle on map, alerts fleet, and queues a rescue vehicle.</p>
          </div>

        </div>
      </div>
    </div>

    <div class="panel">
      <div class="panel-head">
        <h2>Office RBAC Permission Matrix</h2>
        <span class="badge badge-muted">Reference only</span>
      </div>
      <table class="table" style="margin-top: 10px;">
        <thead>
          <tr>
            <th>Role</th>
            <th>Read Data</th>
            <th>Operational CRUD</th>
            <th>Financial Reports</th>
            <th>Simulator Control</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td><strong>Fleet Manager</strong></td>
            <td>${check(true)}</td>
            <td>${check(true)}</td>
            <td>${check(true)}</td>
            <td>${check(true)}</td>
          </tr>
          <tr>
            <td><strong>Safety Officer</strong></td>
            <td>${check(true)}</td>
            <td>${check(true)} (Restricted)</td>
            <td>${check(false)}</td>
            <td>${check(false)}</td>
          </tr>
          <tr>
            <td><strong>Financial Analyst</strong></td>
            <td>${check(true)}</td>
            <td>${check(false)}</td>
            <td>${check(true)}</td>
            <td>${check(false)}</td>
          </tr>
        </tbody>
      </table>
    </div>
  `;

  function check(val: boolean): string {
    return val
      ? `<span style="color: var(--ok); font-weight: bold;">✔ Yes</span>`
      : `<span style="color: var(--bad);">✘ No</span>`;
  }

  const speedRange = el.querySelector<HTMLInputElement>('#sim-speed-range')!;
  const speedVal = el.querySelector<HTMLElement>('#sim-speed-val')!;
  const spawnBtn = el.querySelector<HTMLButtonElement>('#btn-spawn-trip')!;
  const breakdownSel = el.querySelector<HTMLSelectElement>('#sel-breakdown-veh')!;
  const breakdownBtn = el.querySelector<HTMLButtonElement>('#btn-force-breakdown')!;

  let activeVehicles: Vehicle[] = [];

  const updateActiveVehicles = () => {
    void api.getVehicles().then((vehicles) => {
      activeVehicles = vehicles.filter((v) => v.status === 'ON_TRIP');
      if (activeVehicles.length === 0) {
        breakdownSel.innerHTML = `<option value="">No active vehicles</option>`;
        breakdownBtn.disabled = true;
      } else {
        breakdownSel.innerHTML = activeVehicles
          .map((v) => `<option value="${v.id}">${v.regNumber}</option>`)
          .join('');
        breakdownBtn.disabled = false;
      }
    });
  };

  updateActiveVehicles();
  const pollInterval = window.setInterval(updateActiveVehicles, 5000);

  speedRange.oninput = () => {
    speedVal.textContent = `${speedRange.value}x`;
  };

  speedRange.onchange = () => {
    const val = Number(speedRange.value);
    void api.setSimSpeed(val).catch((err) => alert(err.message || 'Failed to set speed'));
  };

  spawnBtn.onclick = () => {
    spawnBtn.disabled = true;
    void api.spawnSimTrip()
      .then(() => {
        setTimeout(() => {
          spawnBtn.disabled = false;
          updateActiveVehicles();
        }, 500);
      })
      .catch((err) => {
        spawnBtn.disabled = false;
        alert(err.message || 'Spawn failed');
      });
  };

  breakdownBtn.onclick = () => {
    const vehId = breakdownSel.value;
    if (!vehId) return;
    if (confirm(`Trigger a mechanical breakdown on vehicle ${activeVehicles.find(x => x.id === vehId)?.regNumber}?`)) {
      void api.triggerSimBreakdown(vehId)
        .then(() => {
          setTimeout(updateActiveVehicles, 500);
        })
        .catch((err) => alert(err.message || 'Breakdown trigger failed'));
    }
  };

  return () => {
    window.clearInterval(pollInterval);
  };
}
