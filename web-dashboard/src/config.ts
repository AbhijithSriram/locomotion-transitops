// VITE_USE_MOCK=false switches every data source (REST + realtime) to the real
// backend. Default is mock so the dashboard runs with zero backend.
export const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false';

// Same-origin in dev thanks to the Vite proxy (see vite.config.ts).
export const API_BASE = '';
export const WS_URL = `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/ws`;
