import type { Role } from './types/api';
import { session } from './auth';
import type { IconName } from './icons';

export interface Route {
  path: string; // hash without '#/'
  label: string;
  icon: IconName;
  roles: Role[]; // who sees it in the nav / may open it
  render(el: HTMLElement): void | (() => void); // may return a cleanup fn
}

const ALL_OFFICE: Role[] = ['FLEET_MANAGER', 'SAFETY_OFFICER', 'FINANCIAL_ANALYST'];

export const routes: Route[] = [];
let cleanup: (() => void) | null = null;

export function registerRoutes(defs: Route[]): void {
  routes.push(...defs);
}

export function visibleRoutes(role: Role): Route[] {
  return routes.filter((r) => r.roles.includes(role));
}

export function currentPath(): string {
  return location.hash.replace(/^#\//, '') || 'dashboard';
}

export function navigate(path: string): void {
  location.hash = `#/${path}`;
}

export function renderCurrent(outlet: HTMLElement): void {
  const role = session.user?.role;
  if (!role) return;
  const path = currentPath();
  const route = routes.find((r) => r.path === path && r.roles.includes(role)) ?? visibleRoutes(role)[0];
  cleanup?.();
  cleanup = null;
  outlet.innerHTML = '';
  const result = route.render(outlet);
  if (typeof result === 'function') cleanup = result;
  document.querySelectorAll<HTMLAnchorElement>('.nav a').forEach((a) => {
    a.classList.toggle('active', a.dataset.path === route.path);
  });
}

export { ALL_OFFICE };
