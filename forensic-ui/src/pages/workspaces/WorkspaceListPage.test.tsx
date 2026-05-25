import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";

import { ApplicationServicesProvider } from "@/application/ApplicationServicesContext";
import type { ApplicationServices } from "@/application/createApplicationServices";
import { ApplicationError } from "@/application/errors";
import type {
  BranchRefreshResult,
  Workspace,
  WorkspaceCleanupResult
} from "@/domain/workspace";

import { WorkspaceListPage } from "./WorkspaceListPage";

describe("WorkspaceListPage", () => {
  it("renders repository checkout workspaces with required list fields", async () => {
    const services = servicesForWorkspaceList([workspace()]);

    renderPage(services);

    expect(
      await screen.findByRole("heading", {
        name: "Repository checkout workspaces"
      })
    ).toBeInTheDocument();
    expect(screen.getByRole("columnheader", { name: "Workspace_ID" }))
      .toBeInTheDocument();
    expect(screen.getByRole("columnheader", { name: "Workspace" }))
      .toBeInTheDocument();
    expect(screen.getByRole("columnheader", { name: "Selected Branch" }))
      .toBeInTheDocument();
    expect(screen.getByText("workspace-1")).toBeInTheDocument();
    expect(screen.getByText("wildfly")).toBeInTheDocument();
    expect(screen.getByText("main")).toBeInTheDocument();
    expect(screen.getByText("READY")).toBeInTheDocument();
    expect(screen.getByText("CHECKED_OUT")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /create workspace/i }))
      .toHaveAttribute("href", "/workspaces/new");
  });

  it("renders loading, empty and initial error states", async () => {
    const pending = deferred<Workspace[]>();
    const loadingServices = servicesForWorkspaceList(pending.promise);

    renderPage(loadingServices);

    expect(
      screen.getByText("Loading repository checkout workspace views.")
    ).toBeInTheDocument();

    pending.resolve([]);
    expect(await screen.findByText("No workspaces yet")).toBeInTheDocument();

    const failedServices = servicesForWorkspaceList([
      new ApplicationError("BACKEND_UNAVAILABLE", "Backend unavailable")
    ]);

    renderPage(failedServices);

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Backend unavailable"
    );
  });

  it("refreshes the selected branch, reloads the list and blocks duplicate submits", async () => {
    const user = userEvent.setup();
    const refresh = deferred<BranchRefreshResult>();
    const services = servicesForWorkspaceList([workspace()]);
    vi.mocked(services.workspaces.refreshBranch).mockReturnValue(refresh.promise);

    renderPage(services);

    const refreshButton = await screen.findByRole("button", {
      name: /update branch main for workspace workspace-1/i
    });

    await user.dblClick(refreshButton);

    expect(services.workspaces.refreshBranch).toHaveBeenCalledTimes(1);
    expect(refreshButton).toBeDisabled();
    expect(services.workspaces.refreshBranch).toHaveBeenCalledWith(
      expect.objectContaining({
        workspaceId: "workspace-1",
        workspaceBranchId: "workspace-branch-1",
        idempotencyKey: expect.stringMatching(/^ui-list-branch-refresh-/)
      })
    );

    refresh.resolve(refreshUnchanged());

    expect(
      await screen.findByText("Branch main is up to date.")
    ).toBeInTheDocument();
    await waitFor(() =>
      expect(services.workspaces.listWorkspaces).toHaveBeenCalledTimes(2)
    );
  });

  it("deletes a workspace, reloads the list and blocks duplicate submits", async () => {
    const user = userEvent.setup();
    const cleanup = deferred<WorkspaceCleanupResult>();
    const services = servicesForWorkspaceList([workspace()]);
    vi.mocked(services.workspaces.deleteWorkspace).mockReturnValue(cleanup.promise);

    renderPage(services);

    const deleteButton = await screen.findByRole("button", {
      name: /delete workspace workspace-1/i
    });

    await user.dblClick(deleteButton);

    expect(services.workspaces.deleteWorkspace).toHaveBeenCalledTimes(1);
    expect(deleteButton).toBeDisabled();
    expect(services.workspaces.deleteWorkspace).toHaveBeenCalledWith(
      expect.objectContaining({
        workspaceId: "workspace-1",
        idempotencyKey: expect.stringMatching(/^ui-workspace-delete-/)
      })
    );

    cleanup.resolve({
      workspaceId: "workspace-1",
      status: "CLEANED",
      diagnostics: []
    });

    expect(
      await screen.findByText("Workspace workspace-1 cleaned.")
    ).toBeInTheDocument();
    await waitFor(() =>
      expect(services.workspaces.listWorkspaces).toHaveBeenCalledTimes(2)
    );
  });

  it("keeps prior workspace rows visible with a stale notice when reload loses the backend", async () => {
    const user = userEvent.setup();
    const services = servicesForWorkspaceList([
      [workspace()],
      new ApplicationError("BACKEND_UNAVAILABLE", "Backend unavailable", {
        retryable: true
      })
    ]);

    renderPage(services);

    await screen.findByText("workspace-1");
    await user.click(
      screen.getByRole("button", {
        name: /update branch main for workspace workspace-1/i
      })
    );

    expect(
      await screen.findByText(
        "Showing stale data because the backend is currently unreachable."
      )
    ).toBeInTheDocument();
    expect(screen.getByText("workspace-1")).toBeInTheDocument();
  });

  it("renders action errors without dropping the current workspace list", async () => {
    const user = userEvent.setup();
    const services = servicesForWorkspaceList([workspace()]);
    vi.mocked(services.workspaces.deleteWorkspace).mockRejectedValueOnce(
      new ApplicationError("BACKEND_UNAVAILABLE", "Backend unavailable")
    );

    renderPage(services);

    await user.click(
      await screen.findByRole("button", {
        name: /delete workspace workspace-1/i
      })
    );

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Backend unavailable"
    );
    expect(screen.getByText("workspace-1")).toBeInTheDocument();
  });
});

const renderPage = (services: ApplicationServices) =>
  render(
    <MemoryRouter>
      <ApplicationServicesProvider services={services}>
        <WorkspaceListPage />
      </ApplicationServicesProvider>
    </MemoryRouter>
  );

const servicesForWorkspaceList = (
  source:
    | Workspace[]
    | Promise<Workspace[]>
    | Array<Workspace[] | ApplicationError>
): ApplicationServices => {
  const sequence =
    Array.isArray(source) &&
    (source.length === 0 ||
      Array.isArray(source[0]) ||
      source[0] instanceof ApplicationError)
      ? ([...source] as Array<Workspace[] | ApplicationError>)
      : null;
  const promised = source instanceof Promise ? source : null;
  const staticWorkspaces =
    sequence === null && promised === null ? (source as Workspace[]) : null;

  const listWorkspaces = vi.fn(async () => {
    if (sequence) {
      const next = sequence.shift();
      if (next instanceof ApplicationError) {
        throw next;
      }
      return next ?? [];
    }
    if (promised) {
      return promised;
    }
    return staticWorkspaces ?? [];
  });

  return {
    repositoryAnalysis: {
      listRepositoryAnalyses: vi.fn(),
      getAnalysisJob: vi.fn(),
      startRepositoryAnalysis: vi.fn()
    },
    workspaces: {
      previewMetadata: vi.fn(),
      createWorkspace: vi.fn(),
      refreshBranch: vi.fn().mockResolvedValue(refreshUnchanged()),
      listWorkspaces,
      deleteWorkspace: vi.fn().mockResolvedValue({
        workspaceId: "workspace-1",
        status: "CLEANED",
        diagnostics: []
      }),
      getWorkspace: vi.fn(),
      waitForWorkspaceCheckout: vi.fn()
    },
    diagnostics: {
      collectDiagnostics: vi.fn()
    }
  };
};

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

const refreshUnchanged = (): BranchRefreshResult => ({
  workspaceBranchId: "workspace-branch-1",
  repositoryBranch: "main",
  status: "UP_TO_DATE",
  changed: false,
  previousCommit: "abc1234",
  resolvedCommit: "abc1234",
  sourceSnapshotId: "source-snapshot-1",
  diagnostics: []
});

const deferred = <T,>() => {
  let resolve!: (value: T) => void;
  let reject!: (reason: unknown) => void;
  const promise = new Promise<T>((innerResolve, innerReject) => {
    resolve = innerResolve;
    reject = innerReject;
  });

  return { promise, resolve, reject };
};
