import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";

import { ApplicationServicesProvider } from "@/application/ApplicationServicesContext";
import type { ApplicationServices } from "@/application/createApplicationServices";
import { createStatusState } from "@/domain/analysisStatus";
import type {
  RepositoryAnalysis,
  StartRepositoryAnalysisCommand
} from "@/domain/repositoryAnalysis";

import { CreateRepositoryAnalysisPage } from "./CreateRepositoryAnalysisPage";

const WILDFLY_REPOSITORY_URL = "https://github.com/wildfly/wildfly.git";

describe("CreateRepositoryAnalysisPage", () => {
  it("submits the WildFly repository command through the frontend boundary", async () => {
    const user = userEvent.setup();
    const services = servicesWithStartResult(analysis());

    render(
      <MemoryRouter
        future={{ v7_relativeSplatPath: true, v7_startTransition: true }}
      >
        <ApplicationServicesProvider services={services}>
          <CreateRepositoryAnalysisPage />
        </ApplicationServicesProvider>
      </MemoryRouter>
    );

    await user.type(
      screen.getByLabelText("Repository URL"),
      WILDFLY_REPOSITORY_URL
    );
    await user.type(screen.getByLabelText("Provider"), "github");
    await user.clear(screen.getByLabelText("Build tool"));
    await user.type(screen.getByLabelText("Build tool"), "maven");
    await user.clear(screen.getByLabelText("Build ID"));
    await user.type(screen.getByLabelText("Build ID"), "manual-wildfly");
    await user.type(screen.getByLabelText("Root project name"), "wildfly");
    await user.clear(screen.getByLabelText("Timeout seconds"));
    await user.type(screen.getByLabelText("Timeout seconds"), "1200");

    await user.click(screen.getByRole("button", { name: /register session/i }));

    await waitFor(() =>
      expect(
        services.repositoryAnalysis.startRepositoryAnalysis
      ).toHaveBeenCalledTimes(1)
    );

    const [command] = vi.mocked(
      services.repositoryAnalysis.startRepositoryAnalysis
    ).mock.calls[0] as [StartRepositoryAnalysisCommand, AbortSignal];
    expect(command).toMatchObject({
      repositoryUrl: WILDFLY_REPOSITORY_URL,
      provider: "github",
      branch: "main",
      commit: null,
      buildContext: {
        buildTool: "maven",
        buildId: "manual-wildfly",
        rootProjectName: "wildfly",
        declaredModules: [],
        attributes: {}
      },
      workspacePolicy: {
        ephemeral: false,
        allowShallowClone: true,
        allowPartialClone: false,
        allowSparseCheckout: false,
        timeoutSeconds: 1200,
        maxWorkspaceBytes: 0
      }
    });
  });
});

const servicesWithStartResult = (
  result: RepositoryAnalysis
): ApplicationServices => ({
  repositoryAnalysis: {
    listRepositoryAnalyses: vi.fn(),
    getAnalysisJob: vi.fn(),
    startRepositoryAnalysis: vi.fn().mockResolvedValue(result)
  },
  workspaces: {
    listWorkspaces: vi.fn(),
    getWorkspace: vi.fn()
  },
  diagnostics: {
    collectDiagnostics: vi.fn()
  }
});

const analysis = (): RepositoryAnalysis => ({
  analysisRunId: "wildfly-run-1",
  workspaceId: "wildfly-workspace-1",
  repositoryUrl: WILDFLY_REPOSITORY_URL,
  branch: "main",
  commit: null,
  resolvedCommit: null,
  checkoutStatus: "CHECKED_OUT",
  status: createStatusState("REGISTERED", "REGISTERED"),
  sourceRoots: [],
  diagnostics: [],
  createdAt: null,
  startedAt: null
});
