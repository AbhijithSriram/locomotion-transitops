// Inline SVG icon set (stroke style, currentColor) — no emoji, no font deps.

const svg = (paths: string): string =>
  `<svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${paths}</svg>`;

export const icons = {
  logo: svg('<path d="M4 6l6 6-6 6"/><path d="M12 6l6 6-6 6"/>'),
  dashboard: svg('<rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/>'),
  fleet: svg('<path d="M2 8h11v8H2z"/><path d="M13 11h4l3 3v2h-7"/><circle cx="6.5" cy="18" r="1.6"/><circle cx="16.5" cy="18" r="1.6"/>'),
  drivers: svg('<circle cx="12" cy="8" r="3.5"/><path d="M5 20c1.5-3.5 4-5 7-5s5.5 1.5 7 5"/>'),
  trips: svg('<circle cx="5.5" cy="18.5" r="2"/><circle cx="18.5" cy="5.5" r="2"/><path d="M7 17L17 7"/>'),
  maintenance: svg('<path d="M20.3 6.3a5 5 0 0 1-6.6 6.6L7 19.6 4.4 17l6.7-6.7a5 5 0 0 1 6.6-6.6l-3.2 3.2 2.6 2.6 3.2-3.2z"/>'),
  fuel: svg('<path d="M12 3s6 6.8 6 11a6 6 0 0 1-12 0c0-4.2 6-11 6-11z"/>'),
  analytics: svg('<path d="M5 20v-9"/><path d="M12 20V5"/><path d="M19 20v-6"/>'),
  settings: svg('<path d="M4 7h8"/><path d="M18 7h2"/><circle cx="15" cy="7" r="2"/><path d="M4 17h2"/><path d="M12 17h8"/><circle cx="9" cy="17" r="2"/>'),
};

export type IconName = keyof typeof icons;
