// Live operations dashboard: KPI strip + fleet board + alerts feed, all driven
// by the realtime client (fake feed in mock mode, STOMP once the backend is up).

import { api } from '../api';
import { realtime, type Unsubscribe } from '../ws';
import type { AlertEvent, KpiEvent } from '../types/events';
import type { Vehicle } from '../types/api';
import { fmtStatus, VEHICLE_BADGE } from './format';

const L = (window as any).L;

const KPI_DEFS: { key: keyof Omit<KpiEvent, 'ts'>; label: string; fmt?: (v: number) => string }[] = [
  { key: 'activeVehicles', label: 'Active Vehicles' },
  { key: 'availableVehicles', label: 'Available' },
  { key: 'inMaintenance', label: 'In Maintenance' },
  { key: 'activeTrips', label: 'Active Trips' },
  { key: 'pendingTrips', label: 'Pending Trips' },
  { key: 'driversOnDuty', label: 'Drivers On Duty' },
  { key: 'utilizationPct', label: 'Fleet Utilization', fmt: (v) => `${v}%` },
];

function decodePolyline(encoded: string): [number, number][] {
  const points: [number, number][] = [];
  let index = 0, len = encoded.length;
  let lat = 0, lng = 0;
  while (index < len) {
    let b, shift = 0, result = 0;
    do {
      b = encoded.charCodeAt(index++) - 63;
      result |= (b & 0x1f) << shift;
      shift += 5;
    } while (b >= 0x20);
    const dlat = ((result & 1) ? ~(result >> 1) : (result >> 1));
    lat += dlat;

    shift = 0;
    result = 0;
    do {
      b = encoded.charCodeAt(index++) - 63;
      result |= (b & 0x1f) << shift;
      shift += 5;
    } while (b >= 0x20);
    const dlng = ((result & 1) ? ~(result >> 1) : (result >> 1));
    lng += dlng;

    points.push([lat / 1e5, lng / 1e5]);
  }
  return points;
}

function getRiskColor(score: number): string {
  if (score > 70) return '#ff5c5c'; // var(--bad)
  if (score > 40) return '#ffb020'; // var(--warn)
  return '#3fce7a'; // var(--ok)
}

function createMarkerIcon(color: string, heading: number) {
  return L.divIcon({
    html: `<div style="transform: rotate(${heading}deg); transition: transform 0.2s ease; width: 24px; height: 24px; display: flex; align-items: center; justify-content: center;">
             <svg width="18" height="18" viewBox="0 0 24 24" fill="${color}" stroke="#0b0e14" stroke-width="2">
               <path d="M12 2L2 22l10-6 10 6L12 2z"/>
             </svg>
           </div>`,
    className: 'custom-marker',
    iconSize: [24, 24],
    iconAnchor: [12, 12],
  });
}

export function renderDashboard(el: HTMLElement): () => void {
  el.innerHTML = `
    <section class="kpi-strip">
      ${KPI_DEFS.map((k) => `
        <div class="kpi-card">
          <div class="kpi-value" data-kpi="${k.key}">—</div>
          <div class="kpi-label">${k.label}</div>
        </div>`).join('')}
    </section>
    <section class="dash-grid">
      <div class="panel">
        <div class="panel-head">
          <h2>Fleet Board</h2>
          <span class="muted small">live positions map</span>
        </div>
        <div id="map-container" style="height: 380px; border-radius: 8px; margin-bottom: 16px; border: 1px solid var(--line); z-index: 1;"></div>
        <table class="table" id="fleet-table">
          <thead><tr><th>Vehicle</th><th>Type</th><th>Status</th><th>Speed</th><th>Risk</th><th>Position</th></tr></thead>
          <tbody></tbody>
        </table>
      </div>
      <div class="panel">
        <div class="panel-head"><h2>Alerts</h2></div>
        <ul class="alerts" id="alerts-list">
          <li class="muted small">Waiting for events…</li>
        </ul>
      </div>
    </section>
  `;

  // Initialize Map
  let map: any = null;
  const markers = new Map<string, any>();
  let activePolyline: any = null;

  try {
    map = L.map('map-container').setView([23.08, 72.6], 11);
    L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
      maxZoom: 19,
      attribution: '© OpenStreetMap, © CartoDB',
    }).addTo(map);
  } catch (err) {
    console.error('Leaflet initialization failed: ', err);
  }

  const rows = new Map<string, HTMLTableRowElement>();
  const tbody = el.querySelector<HTMLTableSectionElement>('#fleet-table tbody')!;

  const handleRowClick = (v: Vehicle) => {
    // Zoom and pan to vehicle marker if exists
    const marker = markers.get(v.id);
    if (marker && map) {
      map.setView(marker.getLatLng(), 13);
    }

    // Highlight route if on trip
    void api.getTrips().then((trips) => {
      const activeTrip = trips.find((t) => t.vehicleId === v.id && t.status === 'DISPATCHED');
      if (activePolyline && map) {
        map.removeLayer(activePolyline);
        activePolyline = null;
      }
      if (activeTrip && activeTrip.routePolyline && map) {
        const coords = decodePolyline(activeTrip.routePolyline);
        activePolyline = L.polyline(coords, { color: '#2fd6c3', weight: 4, opacity: 0.8 }).addTo(map);
        map.fitBounds(activePolyline.getBounds(), { padding: [30, 30] });
      }
    });
  };

  const upsertRow = (v: Vehicle) => {
    let tr = rows.get(v.id);
    if (!tr) {
      tr = document.createElement('tr');
      tr.style.cursor = 'pointer';
      tr.innerHTML = `
        <td class="mono">${v.regNumber}</td>
        <td>${fmtStatus(v.type)}</td>
        <td><span class="badge"></span></td>
        <td class="cell-speed muted">—</td>
        <td class="cell-risk">—</td>
        <td class="cell-pos muted small">—</td>`;
      tr.addEventListener('click', () => handleRowClick(v));
      rows.set(v.id, tr);
      tbody.appendChild(tr);
    }
    const badge = tr.querySelector<HTMLElement>('.badge')!;
    badge.textContent = fmtStatus(v.status);
    badge.className = `badge badge-${VEHICLE_BADGE[v.status]}`;

    // Add marker to map
    if (map && !markers.has(v.id)) {
      // Default placeholder location
      const color = getRiskColor(v.health.riskScore);
      const marker = L.marker([23.08 + RandomLat(), 72.6], {
        icon: createMarkerIcon(color, 0),
      }).addTo(map);
      marker.bindPopup(`<b>${v.regNumber}</b> (${fmtStatus(v.type)})<br>Status: ${fmtStatus(v.status)}`);
      markers.set(v.id, marker);
    }
  };

  // Helper for small spread of default points
  function RandomLat() {
    return (Math.random() - 0.5) * 0.1;
  }

  void api.getVehicles().then((vehicles) => vehicles.forEach(upsertRow));

  const rt = realtime();
  const subs: Unsubscribe[] = [];

  subs.push(
    rt.on('kpi', (kpi) => {
      for (const def of KPI_DEFS) {
        const cell = el.querySelector(`[data-kpi="${def.key}"]`);
        if (cell) cell.textContent = def.fmt ? def.fmt(kpi[def.key]) : String(kpi[def.key]);
      }
    }),
    rt.on('vehicle-position', (e) => {
      const tr = rows.get(e.vehicleId);
      if (tr) {
        tr.querySelector('.cell-speed')!.textContent = `${Math.round(e.speedKmh)} km/h`;
        tr.querySelector('.cell-pos')!.textContent = `${e.lat.toFixed(4)}, ${e.lng.toFixed(4)}`;
      }

      // Update marker on map
      const marker = markers.get(e.vehicleId);
      if (marker && map) {
        marker.setLatLng([e.lat, e.lng]);
        // Update rotation
        const color = marker.options.riskColor || '#3fce7a';
        marker.setIcon(createMarkerIcon(color, e.heading));
      }
    }),
    rt.on('vehicle-status', (e) => {
      const tr = rows.get(e.vehicleId);
      if (tr) {
        const badge = tr.querySelector<HTMLElement>('.badge')!;
        badge.textContent = fmtStatus(e.status);
        badge.className = `badge badge-${VEHICLE_BADGE[e.status]}`;
        if (e.status !== 'ON_TRIP') {
          tr.querySelector('.cell-speed')!.textContent = '—';
        }
      }

      // Update marker popup
      const marker = markers.get(e.vehicleId);
      if (marker) {
        marker.setPopupContent(`<b>Status Changed</b>: ${fmtStatus(e.status)}`);
      }
    }),
    rt.on('vehicle-health', (e) => {
      const tr = rows.get(e.vehicleId);
      const color = getRiskColor(e.riskScore);
      if (tr) {
        const cls = e.riskScore > 70 ? 'bad' : e.riskScore > 40 ? 'warn' : 'ok';
        tr.querySelector('.cell-risk')!.innerHTML = `<span class="risk risk-${cls}">${e.riskScore}</span>`;
      }

      // Update marker color/icon
      const marker = markers.get(e.vehicleId);
      if (marker) {
        marker.options.riskColor = color;
        marker.setIcon(createMarkerIcon(color, marker.options.heading || 0));
      }
    }),
    rt.on('alerts', (a) => addAlert(el, a)),
  );

  return () => {
    subs.forEach((u) => u());
    if (activePolyline && map) {
      map.removeLayer(activePolyline);
    }
    markers.forEach((m) => m.remove());
    if (map) {
      map.remove();
    }
  };
}

function addAlert(el: HTMLElement, a: AlertEvent): void {
  const list = el.querySelector('#alerts-list');
  if (!list) return;
  list.querySelector('.muted')?.remove();
  const li = document.createElement('li');
  li.className = `alert alert-${a.severity.toLowerCase()}`;
  const time = new Date(a.ts).toLocaleTimeString();
  li.innerHTML = `<span class="alert-sev">${a.severity}</span><div><div>${a.message}</div><div class="muted small">${time}</div></div>`;
  list.prepend(li);
  while (list.children.length > 20) list.lastElementChild?.remove();
}
