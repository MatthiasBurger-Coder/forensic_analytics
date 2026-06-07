import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";

import { ApplicationServicesProvider } from "@/application/ApplicationServicesContext";
import type { ApplicationServices } from "@/application/createApplicationServices";
import { WorkspaceUiProvider } from "@/pages/workspaces/WorkspaceUiContext";

import { WorkspaceListPage } from "./WorkspaceListPage";

describe("WorkspaceListPage", () => {
  it("renders the workspace list content without the old in-page submenu", async () => {
    renderPage(servicesForWorkspaceList());

    expect(await screen.findByRole("heading", { name: "Workspaces" }))
      .toBeInTheDocument();
    expect(screen.queryByLabelText("Workspace navigation")).not.toBeInTheDocument();
    expect(screen.queryByText("Existing implementation")).not.toBeInTheDocument();
    expect(screen.queryByText("Repository checkout workspaces"))
      .not.toBeInTheDocument();
    expect(screen.getByRole("columnheader", { name: "ID" }))
      .toBeInTheDocument();
    expect(screen.queryByRole("columnheader", { name: "Title" }))
      .not.toBeInTheDocument();
  });

  it("switches between list, detail, create and edit views", async () => {
    const user = userEvent.setup();

    renderPage(servicesForWorkspaceList());

    await user.click(screen.getByRole("button", { name: "WS-1001" }));
    expect(screen.getByRole("tab", { name: "Overview" }))
      .toHaveAttribute("aria-selected", "true");
    expect(screen.getByRole("tab", { name: "Repositories" }))
      .toHaveAttribute("aria-selected", "false");
    expect(screen.getByRole("heading", { name: "WildFly Investigation", level: 1 }))
      .toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "WildFly Investigation", level: 3 }))
      .not.toBeInTheDocument();
    expect(screen.queryByRole("table")).not.toBeInTheDocument();

    await user.click(screen.getByRole("tab", { name: "Repositories" }));
    expect(screen.getByRole("tab", { name: "Repositories" }))
      .toHaveAttribute("aria-selected", "true");
    expect(screen.getByRole("heading", { name: "Repositories" }))
      .toBeInTheDocument();
    expect(screen.getByText("source-analysis-service")).toBeInTheDocument();

    await user.click(screen.getByRole("tab", { name: "Overview" }));
    await user.click(screen.getByRole("button", { name: /^edit$/i }));
    expect(screen.getByRole("heading", { name: "Edit workspace" }))
      .toBeInTheDocument();
    expect(screen.queryByRole("table")).not.toBeInTheDocument();
    const titleInput = screen.getByLabelText("Title");
    await user.clear(titleInput);
    await user.type(titleInput, "Updated WildFly Investigation");
    await user.click(screen.getByRole("button", { name: "Update" }));
    expect(await screen.findByText("Workspace WS-1001 updated."))
      .toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Updated WildFly Investigation" }))
      .toBeInTheDocument();
  });

  it("creates, deletes and renders sidebar selection checkboxes", async () => {
    const user = userEvent.setup();

    renderPage(servicesForWorkspaceList());

    expect(screen.queryByRole("button", { name: "Columns" }))
      .not.toBeInTheDocument();
    expect(screen.getByRole("columnheader", { name: "Show" }))
      .toBeInTheDocument();
    expect(screen.getByRole("checkbox", { name: "Show workspace WS-1001 in sidebar" }))
      .toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Create Workspace" }));
    expect(screen.getByRole("heading", { name: "Create workspace" }))
      .toBeInTheDocument();
    expect(screen.queryByRole("table")).not.toBeInTheDocument();
    await user.type(screen.getByLabelText("Title"), "Incident Review");
    await user.type(screen.getByLabelText("Description"), "Optional click-dummy description");
    await user.click(screen.getByRole("button", { name: "Create" }));

    expect(await screen.findByText(/created\./i)).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Incident Review" }))
      .toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /^delete$/i }));
    expect(await screen.findByText(/deleted with cascade cleanup\./i))
      .toBeInTheDocument();
    expect(screen.getByRole("table")).toBeInTheDocument();
  });
});

const renderPage = (services: ApplicationServices) =>
  render(
    <MemoryRouter>
      <ApplicationServicesProvider services={services}>
        <WorkspaceUiProvider>
          <WorkspaceListPage />
        </WorkspaceUiProvider>
      </ApplicationServicesProvider>
    </MemoryRouter>
  );

const servicesForWorkspaceList = (): ApplicationServices => ({
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
  }
});
