import { useState } from "react";
import {
  ChevronDown,
  ChevronRight,
  FolderOpen,
  List,
  Plus,
  RadioTower,
  Settings,
  ShieldCheck
} from "lucide-react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";

import { useWorkspaceUi } from "@/pages/workspaces/WorkspaceUiContext";

const navItems = [
  { to: "/backend-unavailable", label: "Backend", icon: RadioTower },
  { to: "/settings", label: "Settings", icon: Settings }
];

export const AppShell = () => {
  const [workspacesExpanded, setWorkspacesExpanded] = useState(true);
  const workspaceUi = useWorkspaceUi();
  const navigate = useNavigate();

  const openWorkspaceRoute = () => {
    navigate("/workspaces");
  };

  return (
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
          <div className="nav-group">
            <button
              aria-expanded={workspacesExpanded}
              className="nav-link nav-toggle active"
              onClick={() => {
                setWorkspacesExpanded((expanded) => !expanded);
                openWorkspaceRoute();
              }}
              type="button"
            >
              <FolderOpen size={18} aria-hidden="true" />
              <span>Workspace</span>
              {workspacesExpanded ? (
                <ChevronDown className="nav-toggle-icon" size={16} aria-hidden="true" />
              ) : (
                <ChevronRight className="nav-toggle-icon" size={16} aria-hidden="true" />
              )}
            </button>
            {workspacesExpanded ? (
              <div className="nav-submenu" aria-label="Workspace submenu">
                <button
                  className={workspaceUi.view === "create" ? "nav-subitem active" : "nav-subitem"}
                  onClick={() => {
                    workspaceUi.showCreate();
                    openWorkspaceRoute();
                  }}
                  type="button"
                >
                  <Plus size={15} aria-hidden="true" />
                  New Workspace
                </button>
                <button
                  className={workspaceUi.view === "list" ? "nav-subitem active" : "nav-subitem"}
                  onClick={() => {
                    workspaceUi.showList();
                    openWorkspaceRoute();
                  }}
                  type="button"
                >
                  <List size={15} aria-hidden="true" />
                  List
                </button>
                <div className="nav-subworkspace-list">
                  {workspaceUi.sidebarWorkspaces.length === 0 ? (
                    <span className="nav-subhint">
                      Select a workspace from the list to open Overview and Repositories.
                    </span>
                  ) : (
                    workspaceUi.sidebarWorkspaces.map((workspace) => (
                      <button
                        className={
                          workspaceUi.selectedWorkspaceId === workspace.workspaceId &&
                          (workspaceUi.view === "detail" || workspaceUi.view === "edit")
                            ? "nav-workspace active"
                            : "nav-workspace"
                        }
                        key={workspace.workspaceId}
                        onClick={() => {
                          workspaceUi.showDetail(workspace);
                          openWorkspaceRoute();
                        }}
                        type="button"
                      >
                        {workspace.title}
                      </button>
                    ))
                  )}
                </div>
              </div>
            ) : null}
          </div>

          {navItems.map((item) => {
            const Icon = item.icon;

            return (
              <div className="nav-group" key={item.to}>
                <NavLink
                  to={item.to}
                  className={({ isActive }) =>
                    isActive ? "nav-link active" : "nav-link"
                  }
                >
                  <Icon size={18} aria-hidden="true" />
                  <span>{item.label}</span>
                </NavLink>
              </div>
            );
          })}
        </nav>
      </aside>
      <div className="main-column">
        <main className="content">
          <Outlet />
        </main>
      </div>
    </div>
  );
};
