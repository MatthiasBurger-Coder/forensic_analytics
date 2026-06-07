import {
  BrowserRouter,
  Navigate,
  Route,
  Routes
} from "react-router-dom";

import { ApplicationServicesProvider } from "@/application/ApplicationServicesContext";
import type { ApplicationServices } from "@/application/createApplicationServices";
import { ErrorBoundary, RouteBoundary } from "@/app/ErrorBoundary";
import { AppShell } from "@/layouts/AppShell";
import { AnalysisJobDetailPage } from "@/pages/analysis-jobs/AnalysisJobDetailPage";
import { BackendUnavailablePage } from "@/pages/backend-unavailable/BackendUnavailablePage";
import { SettingsPage } from "@/pages/settings/SettingsPage";
import { CreateWorkspacePage } from "@/pages/workspaces/CreateWorkspacePage";
import { WorkspaceListPage } from "@/pages/workspaces/WorkspaceListPage";
import { WorkspaceUiProvider } from "@/pages/workspaces/WorkspaceUiContext";

export const App = ({ services }: { services: ApplicationServices }) => (
  <ApplicationServicesProvider services={services}>
    <ErrorBoundary>
      <BrowserRouter>
        <WorkspaceUiProvider>
          <Routes>
            <Route element={<AppShell />}>
              <Route index element={<Navigate to="/workspaces" replace />} />
              <Route
                path="workspaces"
                element={
                  <RouteBoundary>
                    <WorkspaceListPage />
                  </RouteBoundary>
                }
              />
              <Route
                path="workspaces/new"
                element={
                  <RouteBoundary>
                    <CreateWorkspacePage />
                  </RouteBoundary>
                }
              />
              <Route
                path="repository-analyses/new"
                element={<Navigate to="/workspaces" replace />}
              />
              <Route
                path="analysis-jobs/:analysisRunId"
                element={
                  <RouteBoundary>
                    <AnalysisJobDetailPage />
                  </RouteBoundary>
                }
              />
              <Route
                path="diagnostics"
                element={<Navigate to="/workspaces" replace />}
              />
              <Route
                path="backend-unavailable"
                element={
                  <RouteBoundary>
                    <BackendUnavailablePage />
                  </RouteBoundary>
                }
              />
              <Route
                path="settings"
                element={
                  <RouteBoundary>
                    <SettingsPage />
                  </RouteBoundary>
                }
              />
            </Route>
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </WorkspaceUiProvider>
      </BrowserRouter>
    </ErrorBoundary>
  </ApplicationServicesProvider>
);
