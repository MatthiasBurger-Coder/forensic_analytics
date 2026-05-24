import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import type { ApplicationServices } from "@/application/createApplicationServices";

import { App } from "./App";

describe("App routing", () => {
  it.each(["/", "/workspaces", "/workspaces/new"])(
    "routes %s to the Create Workspace flow",
    async (path) => {
      window.history.pushState({}, "", path);

      render(<App services={services()} />);

      expect(
        await screen.findByRole("heading", {
          name: "Create repository workspace"
        })
      ).toBeInTheDocument();
      expect(
        screen.getByRole("link", { name: /create workspace/i })
      ).toBeInTheDocument();
    }
  );
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
    listWorkspaces: vi.fn(),
    getWorkspace: vi.fn()
  },
  diagnostics: {
    collectDiagnostics: vi.fn()
  }
});
