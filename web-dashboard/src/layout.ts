import { session } from './auth';
import { renderCurrent, visibleRoutes } from './router';
import { realtime } from './ws';
import { renderLogin } from './pages/login';
import { USE_MOCK } from './config';
import { icons } from './icons';

const ROLE_LABEL: Record<string, string> = {
  FLEET_MANAGER: 'Fleet Manager',
  SAFETY_OFFICER: 'Safety Officer',
  FINANCIAL_ANALYST: 'Financial Analyst',
  DRIVER: 'Driver',
};

export function startApp(): void {
  const user = session.user;
  if (!user) {
    renderLogin();
    return;
  }

  const app = document.getElementById('app')!;
  app.innerHTML = `
    <div class="shell">
      <aside class="sidebar">
        <div class="brand">
          <span class="brand-mark">${icons.logo}</span>
          <div>
            <div class="brand-name">Loco<em>Motion</em></div>
            <div class="brand-sub">Transit Ops Control</div>
          </div>
        </div>
        <nav class="nav"></nav>
        <div class="sidebar-foot">
          ${USE_MOCK ? '<span class="badge badge-mock">MOCK DATA</span>' : '<span class="badge badge-live">LIVE</span>'}
        </div>
      </aside>
      <div class="body">
        <header class="topbar">
          <div class="topbar-title" id="page-title"></div>
          <div class="topbar-user">
            <span class="user-name">${user.name}</span>
            <span class="user-role">${ROLE_LABEL[user.role] ?? user.role}</span>
            <button class="btn btn-ghost" id="logout">Logout</button>
          </div>
        </header>
        <main class="page" id="outlet"></main>
      </div>
    </div>
  `;

  const nav = app.querySelector('.nav')!;
  for (const route of visibleRoutes(user.role)) {
    const a = document.createElement('a');
    a.href = `#/${route.path}`;
    a.dataset.path = route.path;
    a.innerHTML = `${icons[route.icon]}${route.label}`;
    nav.appendChild(a);
  }

  app.querySelector('#logout')!.addEventListener('click', () => {
    session.clear();
    realtime().disconnect();
    location.hash = '';
    renderLogin();
  });

  const outlet = document.getElementById('outlet')!;
  const rerender = () => {
    renderCurrent(outlet);
    const active = document.querySelector<HTMLAnchorElement>('.nav a.active');
    document.getElementById('page-title')!.textContent = active?.textContent?.trim() ?? '';
  };
  window.onhashchange = rerender;
  rerender();

  realtime().connect();
}
