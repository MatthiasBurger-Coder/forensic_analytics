import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import type { ApplicationServices } from "@/application/createApplicationServices";
import type { Workspace } from "@/domain/workspace";

import { App } from "./App";

describe("App routing", () => {
  it.each([
    "/",
    "/workspaces",
    "/repository-analyses/new",
    "/diagnostics"
  ])(
    "routes %s to the Workspaces list",
    async (path) => {
      window.history.pushState({}, "", path);

      render(<App services={services()} />);

      expect(
        await screen.findByRole("heading", {
          name: "Workspaces"
        })
      ).toBeInTheDocument();
      expect(screen.queryByText("REST API")).not.toBeInTheDocument();
      expect(screen.queryByText("Transport")).not.toBeInTheDocument();
      expect(screen.queryByText("HTTP")).not.toBeInTheDocument();
      expect(
        screen.getByRole("button", { name: /^workspace$/i })
      ).toHaveAttribute("aria-expanded", "true");
      expect(
        screen.queryByRole("link", { name: /register session/i })
      ).not.toBeInTheDocument();
    }
  );

  it("keeps the create flow on /workspaces/new", async () => {
    window.history.pushState({}, "", "/workspaces/new");

    render(<App services={services()} />);

    expect(
      await screen.findByRole("heading", {
        name: "Create repository workspace"
      })
    ).toBeInTheDocument();
  });

  it("renders workspace actions as a submenu below the Workspace navigation item", async () => {
    const user = userEvent.setup();
    window.history.pushState({}, "", "/workspaces");

    render(<App services={services()} />);

    const workspaceSubmenu = await screen.findByLabelText("Workspace submenu");
    const workspaceToggle = screen.getByRole("button", { name: /^workspace$/i });
    expect(workspaceToggle).toHaveAttribute("aria-expanded", "true");
    expect(
      screen.queryByLabelText("Workspace navigation")
    ).not.toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: /new workspace/i })
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: /^list$/i })
    ).toBeInTheDocument();
    expect(workspaceSubmenu).toHaveTextContent("Select a workspace from the list");
    expect(workspaceSubmenu).not.toHaveTextContent("WildFly Investigation");
    expect(workspaceSubmenu).not.toHaveTextContent("WS-1001");

    await user.click(screen.getByRole("checkbox", { name: "Show workspace WS-1001 in sidebar" }));
    expect(workspaceSubmenu).not.toHaveTextContent("Select a workspace from the list");
    expect(workspaceSubmenu).toHaveTextContent("WildFly Investigation");
    expect(workspaceSubmenu).not.toHaveTextContent("WS-1001");

    await user.click(screen.getByRole("button", { name: /new workspace/i }));
    expect(
      await screen.findByRole("heading", { name: "Create workspace" })
    ).toBeInTheDocument();

    await user.click(workspaceToggle);
    expect(screen.queryByLabelText("Workspace submenu")).not.toBeInTheDocument();
    expect(workspaceToggle).toHaveAttribute("aria-expanded", "false");
  });

  it("routes /settings to the operator Settings page", async () => {
    window.history.pushState({}, "", "/settings");

    render(<App services={services()} />);

    expect(
      await screen.findByRole("heading", {
        name: "Settings"
      })
    ).toBeInTheDocument();
    expect(
      screen.getByRole("heading", {
        name: "Database status"
      })
    ).toBeInTheDocument();
  });
});

const services = (): ApplicationServices => ({
  repositoryAnalysis: {
    listRepositoryAnalyses: vi.fn(),
    getAnalysisJob: vi.fn(),
    startRepositoryAnalysis: vi.fn()
  },
  workspaces: {
    previewMetadata: vi.fn(),
    createWorkspace: vi.fn(),
    refreshBranch: vi.fn(),
    listWorkspaces: vi.fn().mockResolvedValue([workspace()]),
    deleteWorkspace: vi.fn(),
    getWorkspace: vi.fn(),
    waitForWorkspaceCheckout: vi.fn()
  },
  diagnostics: {
    collectDiagnostics: vi.fn()
  },
  settings: {
    getRepositorySourceDatabaseSettings: vi.fn(),
    validateRepositorySourceDatabaseSettings: vi.fn()
  }
});

const workspace = (): Workspace => ({
  workspaceId: "workspace-1",
  workspaceTitle: "wildfly",
  repository: {
    repositoryKey: "github.com/wildfly/wildfly",
    repositoryUrl: "",
    repositoryHost: "github.com",
    repositoryOwner: "wildfly",
    repositoryName: "wildfly",
    defaultBranch: null
  },
  status: "READY",
  branches: [
    {
      workspaceBranchId: "workspace-branch-1",
      repositoryBranch: "main",
      status: "CHECKED_OUT",
      resolvedCommit: "abc1234",
      sourceSnapshotId: "source-snapshot-1",
      sourceRoots: ["src/main/java"],
      diagnostics: []
    }
  ],
  diagnostics: []
});
