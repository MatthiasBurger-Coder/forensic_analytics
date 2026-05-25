import { render, screen } from "@testing-library/react";
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
          name: "Repository checkout workspaces"
        })
      ).toBeInTheDocument();
      expect(
        screen.getByRole("link", { name: /^workspaces$/i })
      ).toBeInTheDocument();
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
