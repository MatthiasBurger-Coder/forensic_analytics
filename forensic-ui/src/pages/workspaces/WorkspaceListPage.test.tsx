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
  WorkspaceBranch,
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
    expect(
      screen.getByRole("combobox", {
        name: /select branch for workspace workspace-1/i
      })
    ).toHaveValue("workspace-branch-1");
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

  it("selects another public branch record and refreshes by its branch id", async () => {
    const user = userEvent.setup();
    const refresh = deferred<BranchRefreshResult>();
    const services = servicesForWorkspaceList([
      workspace({
        branches: [
          branch(),
          branch({
            workspaceBranchId: "workspace-branch-2",
            repositoryBranch: "release/1.0",
            status: "UP_TO_DATE",
            resolvedCommit: "def5678",
            sourceSnapshotId: "source-snapshot-2"
          })
        ]
      })
    ]);
    vi.mocked(services.workspaces.refreshBranch).mockReturnValue(refresh.promise);

    renderPage(services);

    const branchSelect = await screen.findByRole("combobox", {
      name: /select branch for workspace workspace-1/i
    });
    await user.selectOptions(branchSelect, "workspace-branch-2");
    await user.click(
      screen.getByRole("button", {
        name: /update branch release\/1\.0 for workspace workspace-1/i
      })
    );

    expect(services.workspaces.refreshBranch).toHaveBeenCalledWith(
      expect.objectContaining({
        workspaceId: "workspace-1",
        workspaceBranchId: "workspace-branch-2"
      })
    );

    refresh.resolve(
      refreshUnchanged({
        workspaceBranchId: "workspace-branch-2",
        repositoryBranch: "release/1.0",
        resolvedCommit: "def5678",
        sourceSnapshotId: "source-snapshot-2"
      })
    );

    expect(
      await screen.findByText("Branch release/1.0 is up to date.")
    ).toBeInTheDocument();
  });

  it("keeps branch selection scoped to each workspace row", async () => {
    const user = userEvent.setup();
    const services = servicesForWorkspaceList([
      workspace({
        branches: [
          branch(),
          branch({
            workspaceBranchId: "workspace-branch-2",
            repositoryBranch: "release/1.0"
          })
        ]
      }),
      workspace({
        workspaceId: "workspace-2",
        workspaceTitle: "netty",
        repository: {
          ...workspace().repository,
          repositoryKey: "github.com/netty/netty",
          repositoryName: "netty"
        },
        branches: [
          branch({
            workspaceBranchId: "workspace-branch-3",
            repositoryBranch: "develop"
          }),
          branch({
            workspaceBranchId: "workspace-branch-4",
            repositoryBranch: "hotfix"
          })
        ]
      })
    ]);

    renderPage(services);

    const firstWorkspaceSelect = await screen.findByRole("combobox", {
      name: /select branch for workspace workspace-1/i
    });
    const secondWorkspaceSelect = screen.getByRole("combobox", {
      name: /select branch for workspace workspace-2/i
    });

    await user.selectOptions(firstWorkspaceSelect, "workspace-branch-2");
    await user.selectOptions(secondWorkspaceSelect, "workspace-branch-4");

    expect(firstWorkspaceSelect).toHaveValue("workspace-branch-2");
    expect(secondWorkspaceSelect).toHaveValue("workspace-branch-4");
  });

  it("falls back to the first public branch when a selected branch disappears", async () => {
    const user = userEvent.setup();
    const services = servicesForWorkspaceList([
      [
        workspace({
          branches: [
            branch(),
            branch({
              workspaceBranchId: "workspace-branch-2",
              repositoryBranch: "release/1.0"
            })
          ]
        })
      ],
      [workspace()]
    ]);
    vi.mocked(services.workspaces.refreshBranch).mockResolvedValueOnce(
      refreshUnchanged({
        workspaceBranchId: "workspace-branch-2",
        repositoryBranch: "release/1.0"
      })
    );

    renderPage(services);

    const branchSelect = await screen.findByRole("combobox", {
      name: /select branch for workspace workspace-1/i
    });
    await user.selectOptions(branchSelect, "workspace-branch-2");
    await user.click(
      screen.getByRole("button", {
        name: /update branch release\/1\.0 for workspace workspace-1/i
      })
    );

    await waitFor(() =>
      expect(services.workspaces.listWorkspaces).toHaveBeenCalledTimes(2)
    );
    expect(
      screen.getByRole("combobox", {
        name: /select branch for workspace workspace-1/i
      })
    ).toHaveValue("workspace-branch-1");
  });

  it("shows no-branch workspaces as unavailable and disables branch refresh", async () => {
    const services = servicesForWorkspaceList([
      workspace({
        workspaceId: "workspace-empty",
        workspaceTitle: "empty",
        branches: []
      })
    ]);

    renderPage(services);

    expect(await screen.findByText("workspace-empty")).toBeInTheDocument();
    expect(screen.getByText("Unavailable")).toBeInTheDocument();
    expect(
      screen.queryByRole("combobox", {
        name: /select branch for workspace workspace-empty/i
      })
    ).not.toBeInTheDocument();
    expect(
      screen.getByRole("button", {
        name: /update branch for workspace workspace-empty/i
      })
    ).toBeDisabled();
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

const workspace = (overrides: Partial<Workspace> = {}): Workspace => {
  const base: Workspace = {
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
    branches: [branch()],
    diagnostics: []
  };

  return {
    ...base,
    ...overrides,
    repository: {
      ...base.repository,
      ...overrides.repository
    }
  };
};

const branch = (overrides: Partial<WorkspaceBranch> = {}): WorkspaceBranch => ({
  workspaceBranchId: "workspace-branch-1",
  repositoryBranch: "main",
  status: "CHECKED_OUT",
  resolvedCommit: "abc1234",
  sourceSnapshotId: "source-snapshot-1",
  sourceRoots: ["src/main/java"],
  diagnostics: [],
  ...overrides
});

const refreshUnchanged = (
  overrides: Partial<BranchRefreshResult> = {}
): BranchRefreshResult => ({
  workspaceBranchId: "workspace-branch-1",
  repositoryBranch: "main",
  status: "UP_TO_DATE",
  changed: false,
  previousCommit: "abc1234",
  resolvedCommit: "abc1234",
  sourceSnapshotId: "source-snapshot-1",
  diagnostics: [],
  ...overrides
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
