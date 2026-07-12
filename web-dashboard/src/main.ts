import './styles.css';
import { session } from './auth';
import { startApp } from './layout';
import { renderLogin } from './pages/login';
import { ALL_OFFICE, registerRoutes } from './router';
import { renderDashboard } from './pages/dashboard';
import { renderVehicles } from './pages/vehicles';
import { renderDrivers } from './pages/drivers';
import { renderTrips } from './pages/trips';
import { renderMaintenance } from './pages/maintenance';
import { renderFuel } from './pages/fuel';
import { renderAnalytics } from './pages/analytics';
import { renderSettings } from './pages/stubs';

registerRoutes([
  { path: 'dashboard', label: 'Dashboard', icon: 'dashboard', roles: ALL_OFFICE, render: renderDashboard },
  { path: 'vehicles', label: 'Fleet', icon: 'fleet', roles: ['FLEET_MANAGER', 'SAFETY_OFFICER'], render: renderVehicles },
  { path: 'drivers', label: 'Drivers', icon: 'drivers', roles: ['FLEET_MANAGER', 'SAFETY_OFFICER'], render: renderDrivers },
  { path: 'trips', label: 'Trips', icon: 'trips', roles: ['FLEET_MANAGER'], render: renderTrips },
  { path: 'maintenance', label: 'Maintenance', icon: 'maintenance', roles: ['FLEET_MANAGER', 'SAFETY_OFFICER'], render: renderMaintenance },
  { path: 'fuel', label: 'Fuel & Expenses', icon: 'fuel', roles: ['FLEET_MANAGER', 'FINANCIAL_ANALYST'], render: renderFuel },
  { path: 'analytics', label: 'Reports', icon: 'analytics', roles: ['FLEET_MANAGER', 'FINANCIAL_ANALYST'], render: renderAnalytics },
  { path: 'settings', label: 'Settings', icon: 'settings', roles: ['FLEET_MANAGER'], render: renderSettings },
]);

if (session.user) startApp();
else renderLogin();
