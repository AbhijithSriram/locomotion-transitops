import { api, HttpError } from '../api';
import { session } from '../auth';
import { USE_MOCK } from '../config';
import { startApp } from '../layout';
import { icons } from '../icons';

export function renderLogin(): void {
  const app = document.getElementById('app')!;
  app.innerHTML = `
    <div class="login-wrap">
      <div class="login-hero">
        <div class="brand brand-lg">
          <span class="brand-mark">${icons.logo}</span>
          <div>
            <div class="brand-name">Loco<em>Motion</em></div>
            <div class="brand-sub">Transit Operations Control</div>
          </div>
        </div>
        <p class="hero-note">One login, three roles:</p>
        <ul class="hero-roles">
          <li>Fleet Manager — fleet, trips, dispatch, god-mode</li>
          <li>Safety Officer — drivers, vehicle health, alerts</li>
          <li>Financial Analyst — fuel, expenses, analytics</li>
        </ul>
        ${USE_MOCK ? '<p class="hero-mock">Mock mode: any password works.<br/>Try <code>manager@</code>, <code>safety@</code> or <code>finance@locomotion.io</code></p>' : ''}
      </div>
      <div class="login-card">
        <h1>Sign in to your account</h1>
        <p class="muted">Enter your credentials to continue</p>
        <form id="login-form">
          <label>Email
            <input type="email" name="email" placeholder="you@transitops.com" required value="${USE_MOCK ? 'manager@locomotion.io' : ''}" />
          </label>
          <label>Password
            <input type="password" name="password" placeholder="••••••••" required value="${USE_MOCK ? 'demo' : ''}" />
          </label>
          <div class="form-error" id="login-error" hidden></div>
          <button class="btn btn-primary" type="submit">Sign In</button>
        </form>
      </div>
    </div>
  `;

  const form = document.getElementById('login-form') as HTMLFormElement;
  const errorEl = document.getElementById('login-error')!;
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const data = new FormData(form);
    const btn = form.querySelector('button')!;
    btn.disabled = true;
    btn.textContent = 'Signing in…';
    try {
      const res = await api.login(String(data.get('email')), String(data.get('password')));
      session.save(res);
      startApp();
    } catch (err) {
      errorEl.hidden = false;
      errorEl.textContent = err instanceof HttpError ? err.message : 'Could not reach the server';
      btn.disabled = false;
      btn.textContent = 'Sign In';
    }
  });
}
