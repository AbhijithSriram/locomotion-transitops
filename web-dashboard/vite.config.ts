import { defineConfig } from 'vite';

// REST + WebSocket proxy to Person A's Spring Boot backend (port 8080).
// Only used when VITE_USE_MOCK=false; the mock feed needs no backend at all.
const BACKEND = 'http://localhost:8080';

export default defineConfig({
  server: {
    port: 5173,
    allowedHosts: ['locomotion-transitops.abhijith-sriram.in'],
    proxy: {
      '/auth': BACKEND,
      '/vehicles': BACKEND,
      '/drivers': BACKEND,
      '/trips': BACKEND,
      '/maintenance': BACKEND,
      '/fuel-logs': BACKEND,
      '/expenses': BACKEND,
      '/reports': BACKEND,
      '/sim': BACKEND,
      '/ws': { target: BACKEND, ws: true },
    },
  },
});
