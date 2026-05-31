import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import { ApplicationServicesProvider } from "@/application/ApplicationServicesContext";
import type { ApplicationServices } from "@/application/createApplicationServices";
import type {
  DatabaseSettingsStatus,
  DatabaseSettingsValidationStatus,
  DatabaseSettingsValidationResult
} from "@/domain/settings";

import { SettingsPage } from "./SettingsPage";

describe("SettingsPage", () => {
  it("loads sanitized database settings and validates candidates without rendering passwords", async () => {
    window.localStorage.clear();
    window.sessionStorage.clear();
    const user = userEvent.setup();
    const services = servicesForSettings();

    renderPage(services);

    await user.type(screen.getByLabelText(/operator token/i), "operator-token");
    await user.click(screen.getByRole("button", { name: /refresh/i }));

    expect(
      await screen.findByText("postgres.example.test")
    ).toBeInTheDocument();
    expect(services.settings!.getRepositorySourceDatabaseSettings).toHaveBeenCalledWith(
      {
        operatorToken: "operator-token"
      }
    );

    await user.type(screen.getByLabelText(/password/i), "candidate-secret");
    await user.click(screen.getByRole("button", { name: /validate/i }));

    expect(
      services.settings!.validateRepositorySourceDatabaseSettings
    ).toHaveBeenCalledWith({
      operatorToken: "operator-token",
      host: "postgres.example.test",
      port: 5432,
      databaseName: "forensic_analytics",
      username: "forensic",
      password: "candidate-secret",
      schema: "repository_source",
      sslMode: "require"
    });
    expect(await screen.findByText("Validation UNREACHABLE")).toBeInTheDocument();
    expect(screen.queryByText("candidate-secret")).not.toBeInTheDocument();
    expect(document.body.textContent).not.toContain("candidate-secret");
    expect(document.body.textContent).not.toContain("operator-token");
    expect(window.location.href).not.toContain("candidate-secret");
    expect(window.location.href).not.toContain("operator-token");
    expect(screen.getByLabelText(/password/i)).toHaveValue("");
    expect(window.localStorage.length).toBe(0);
    expect(window.sessionStorage.length).toBe(0);
    expect(screen.getAllByText("RESTART_REQUIRED").length).toBeGreaterThan(0);
    expect(screen.getByText("Not supported")).toBeInTheDocument();
  });

  it.each<DatabaseSettingsValidationStatus>([
    "VALID",
    "INVALID",
    "UNREACHABLE",
    "AUTHENTICATION_FAILED",
    "UNSUPPORTED"
  ])("renders validation status %s distinctly", async (validationStatus) => {
    const user = userEvent.setup();
    const services = servicesForSettings(validationResult(validationStatus));

    renderPage(services);

    await user.type(screen.getByLabelText(/operator token/i), "operator-token");
    await user.click(screen.getByRole("button", { name: /refresh/i }));
    await user.type(screen.getByLabelText(/password/i), "candidate-secret");
    await user.click(screen.getByRole("button", { name: /validate/i }));

    expect(
      await screen.findByText(`Validation ${validationStatus}`)
    ).toBeInTheDocument();
    expect(screen.getAllByText("RESTART_REQUIRED").length).toBeGreaterThan(0);
    expect(screen.getByText("Not supported")).toBeInTheDocument();
  });
});

const renderPage = (services: ApplicationServices) =>
  render(
    <ApplicationServicesProvider services={services}>
      <SettingsPage />
    </ApplicationServicesProvider>
  );

const servicesForSettings = (
  validation: DatabaseSettingsValidationResult = validationResult()
): ApplicationServices => ({
  repositoryAnalysis: {
    listRepositoryAnalyses: vi.fn(),
    getAnalysisJob: vi.fn(),
    startRepositoryAnalysis: vi.fn()
  },
  workspaces: {
    previewMetadata: vi.fn(),
    createWorkspace: vi.fn(),
    refreshBranch: vi.fn(),
    listWorkspaces: vi.fn(),
    deleteWorkspace: vi.fn(),
    getWorkspace: vi.fn(),
    waitForWorkspaceCheckout: vi.fn()
  },
  diagnostics: {
    collectDiagnostics: vi.fn()
  },
  settings: {
    getRepositorySourceDatabaseSettings: vi.fn().mockResolvedValue(settingsStatus()),
    validateRepositorySourceDatabaseSettings: vi.fn().mockResolvedValue(validation)
  }
});

const settingsStatus = (): DatabaseSettingsStatus => ({
  settings: settingsView("REPOSITORY_SOURCE_RUNTIME"),
  status: "AVAILABLE",
  diagnostics: []
});

const validationResult = (
  validationStatus: DatabaseSettingsValidationStatus = "UNREACHABLE"
): DatabaseSettingsValidationResult => ({
  settings: settingsView("VALIDATION_REQUEST"),
  validationStatus,
  applyMode: "RESTART_REQUIRED",
  hotApplySupported: false,
  diagnostics: [
    {
      id: "settings-diagnostic-1",
      severity: "ERROR",
      code: "DATABASE_SETTINGS_UNREACHABLE",
      message: "PostgreSQL is not reachable",
      source: null,
      observedAt: null
    }
  ]
});

const settingsView = (configurationSource: string) => ({
  engine: "POSTGRESQL",
  host: "postgres.example.test",
  port: 5432,
  databaseName: "forensic_analytics",
  username: "forensic",
  authenticationConfigured: true,
  schema: "repository_source",
  sslMode: "require",
  configurationSource,
  applyMode: "RESTART_REQUIRED",
  hotApplySupported: false
});
