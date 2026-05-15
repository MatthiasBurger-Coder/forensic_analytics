import {
  Activity,
  AlertTriangle,
  BarChart3,
  Database,
  FilePlus2,
  RadioTower,
  Settings,
  ShieldCheck
} from "lucide-react";
import { NavLink, Outlet } from "react-router-dom";

const navItems = [
  { to: "/", label: "Dashboard", icon: BarChart3, end: true },
  { to: "/workspaces", label: "Workspaces", icon: Database },
  { to: "/repository-analyses/new", label: "Register session", icon: FilePlus2 },
  { to: "/diagnostics", label: "Diagnostics", icon: AlertTriangle },
  { to: "/backend-unavailable", label: "Backend", icon: RadioTower },
  { to: "/settings", label: "Settings", icon: Settings }
];

export const AppShell = () => (
  <div className="app-shell">
    <aside className="sidebar" aria-label="Primary navigation">
      <div className="brand">
        <ShieldCheck size={24} aria-hidden="true" />
        <div>
          <strong>Forensic Analytics</strong>
          <span>Operator Console</span>
        </div>
      </div>
      <nav className="nav-list">
        {navItems.map((item) => {
          const Icon = item.icon;

          return (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) =>
                isActive ? "nav-link active" : "nav-link"
              }
            >
              <Icon size={18} aria-hidden="true" />
              <span>{item.label}</span>
            </NavLink>
          );
        })}
      </nav>
    </aside>
    <div className="main-column">
      <header className="topbar">
        <div className="topbar-status">
          <Activity size={16} aria-hidden="true" />
          <span>REST API</span>
          <strong>{import.meta.env.VITE_API_BASE_URL || "/api"}</strong>
        </div>
        <div className="topbar-status muted">
          <span>Transport</span>
          <strong>HTTP</strong>
        </div>
      </header>
      <main className="content">
        <Outlet />
      </main>
    </div>
  </div>
);
