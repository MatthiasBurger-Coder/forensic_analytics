import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import { ApplicationServicesProvider } from "@/application/ApplicationServicesContext";
import { ApplicationError } from "@/application/errors";
import type { ApplicationServices } from "@/application/createApplicationServices";
import type {
  BranchRefreshResult,
  CreateWorkspaceCommand,
  PreviewWorkspaceMetadataCommand,
  Workspace,
  WorkspaceMetadata
} from "@/domain/workspace";

import { CreateWorkspacePage } from "./CreateWorkspacePage";

const WILDFLY_REPOSITORY_URL = "https://github.com/wildfly/wildfly.git";

describe("CreateWorkspacePage", () => {
  it("renders metadata from the public preview response and keeps workspaceTitle read-only", async () => {
    const user = userEvent.setup();
    const services = servicesForWorkspaceFlow();

    renderPage(services);

    const title = screen.getByLabelText("Workspace title");
    expect(title).toHaveAttribute("readonly");
    expect(title).toHaveValue("");

    await user.type(screen.getByLabelText("Repository URL"), WILDFLY_REPOSITORY_URL);
    await user.click(screen.getByRole("button", { name: /preview/i }));

    await waitFor(() => expect(title).toHaveValue("wildfly"));
    expect(screen.getByLabelText("Selected branch")).toHaveValue("main");
    expect(screen.getByText("github.com/wildfly/wildfly")).toBeInTheDocument();

    const [command] = vi.mocked(services.workspaces.previewMetadata).mock
      .calls[0] as [PreviewWorkspaceMetadataCommand, AbortSignal | undefined];
    expect(command.repositoryUrl).toBe(WILDFLY_REPOSITORY_URL);
    expect(command.idempotencyKey).toMatch(/^ui-workspace-metadata-/);
    expect(screen.queryByText("Checkout limits")).not.toBeInTheDocument();
  });

  it("leaves branch blank when the public preview has no default branch", async () => {
    const user = userEvent.setup();
    const services = servicesForWorkspaceFlow();
    vi.mocked(services.workspaces.previewMetadata).mockResolvedValueOnce({
      repositoryKey: "github.com/wildfly/wildfly",
      repositoryHost: "github.com",
      repositoryOwner: "wildfly",
      repositoryName: "wildfly",
      workspaceTitle: "server-title",
      defaultBranch: null,
      diagnostics: []
    });

    renderPage(services);

    await user.type(screen.getByLabelText("Repository URL"), WILDFLY_REPOSITORY_URL);
    await user.click(screen.getByRole("button", { name: /preview/i }));

    await waitFor(() =>
      expect(screen.getByLabelText("Workspace title")).toHaveValue("server-title")
    );
    expect(screen.getByLabelText("Selected branch")).toHaveValue("");

    await user.click(screen.getByRole("button", { name: /^save$/i }));

    const [command] = vi.mocked(services.workspaces.createWorkspace).mock
      .calls[0] as [CreateWorkspaceCommand];
    expect(command.selectedBranch).toBeNull();
    expect(command.workspacePolicy).toEqual({
      ephemeral: false,
      allowShallowClone: true,
      allowPartialClone: false,
      allowSparseCheckout: false,
      timeoutSeconds: 300,
      maxWorkspaceBytes: 1073741824
    });
  });

  it("clears stale branch input when the repository URL changes", async () => {
    const user = userEvent.setup();
    const services = servicesForWorkspaceFlow();

    renderPage(services);

    await user.type(screen.getByLabelText("Repository URL"), WILDFLY_REPOSITORY_URL);
    await user.click(screen.getByRole("button", { name: /preview/i }));
    await screen.findByDisplayValue("wildfly");
    expect(screen.getByLabelText("Selected branch")).toHaveValue("main");

    await user.clear(screen.getByLabelText("Repository URL"));
    await user.type(
      screen.getByLabelText("Repository URL"),
      "https://github.com/example/project.git"
    );

    expect(screen.getByLabelText("Selected branch")).toHaveValue("");
  });

  it("ignores stale metadata responses after the repository URL changes", async () => {
    const user = userEvent.setup();
    const services = servicesForWorkspaceFlow();
    const pending = deferred<WorkspaceMetadata>();
    vi.mocked(services.workspaces.previewMetadata).mockReturnValueOnce(
      pending.promise
    );

    renderPage(services);

    await user.type(screen.getByLabelText("Repository URL"), WILDFLY_REPOSITORY_URL);
    await user.click(screen.getByRole("button", { name: /preview/i }));
    await user.clear(screen.getByLabelText("Repository URL"));
    await user.type(
      screen.getByLabelText("Repository URL"),
      "https://github.com/example/project.git"
    );

    pending.resolve({
      repositoryKey: "github.com/wildfly/wildfly",
      repositoryHost: "github.com",
      repositoryOwner: "wildfly",
      repositoryName: "wildfly",
      workspaceTitle: "wildfly",
      defaultBranch: "main",
      diagnostics: []
    });

    await waitFor(() =>
      expect(screen.getByRole("button", { name: /preview/i })).toBeEnabled()
    );
    expect(screen.getByLabelText("Workspace title")).toHaveValue("");
    expect(screen.getByLabelText("Selected branch")).toHaveValue("");
    expect(screen.queryByText("github.com/wildfly/wildfly")).not.toBeInTheDocument();
  });

  it("does not announce metadata work before the operator starts preview", () => {
    const services = servicesForWorkspaceFlow();

    renderPage(services);

    expect(
      screen.getByRole("heading", { name: "Awaiting repository URL" })
    ).toBeInTheDocument();
    expect(screen.queryByText("Checking out repository...")).not.toBeInTheDocument();
    expect(screen.queryByText("Resolving source roots...")).not.toBeInTheDocument();
  });

  it("reuses the save idempotency key when the same failed save is retried", async () => {
    const user = userEvent.setup();
    const services = servicesForWorkspaceFlow();
    vi.mocked(services.workspaces.createWorkspace)
      .mockRejectedValueOnce(
        new ApplicationError("BACKEND_UNAVAILABLE", "Backend unavailable")
      )
      .mockResolvedValueOnce(workspace());

    renderPage(services);

    await user.type(screen.getByLabelText("Repository URL"), WILDFLY_REPOSITORY_URL);
    await user.click(screen.getByRole("button", { name: /preview/i }));
    await screen.findByDisplayValue("wildfly");

    await user.click(screen.getByRole("button", { name: /^save$/i }));
    await screen.findByRole("alert");

    await user.click(screen.getByRole("button", { name: /^save$/i }));
    await screen.findByRole("heading", { name: "Workspace ready" });

    const first = vi.mocked(services.workspaces.createWorkspace).mock
      .calls[0][0] as CreateWorkspaceCommand;
    const second = vi.mocked(services.workspaces.createWorkspace).mock
      .calls[1][0] as CreateWorkspaceCommand;
    expect(second.idempotencyKey).toBe(first.idempotencyKey);
  });

  it("generates a new save idempotency key after the selected branch changes", async () => {
    const user = userEvent.setup();
    const services = servicesForWorkspaceFlow();

    renderPage(services);

    await user.type(screen.getByLabelText("Repository URL"), WILDFLY_REPOSITORY_URL);
    await user.click(screen.getByRole("button", { name: /preview/i }));
    await screen.findByDisplayValue("wildfly");

    await user.click(screen.getByRole("button", { name: /^save$/i }));
    await screen.findByRole("heading", { name: "Workspace ready" });

    await user.clear(screen.getByLabelText("Selected branch"));
    await user.type(screen.getByLabelText("Selected branch"), "release/1.0");
    await user.click(screen.getByRole("button", { name: /^save$/i }));

    const first = vi.mocked(services.workspaces.createWorkspace).mock
      .calls[0][0] as CreateWorkspaceCommand;
    const second = vi.mocked(services.workspaces.createWorkspace).mock
      .calls[1][0] as CreateWorkspaceCommand;
    expect(second.idempotencyKey).not.toBe(first.idempotencyKey);
    expect(second.selectedBranch).toBe("release/1.0");
  });

  it("does not create duplicate save operations while a save is already running", async () => {
    const user = userEvent.setup();
    const services = servicesForWorkspaceFlow();
    const pending = deferred<Workspace>();
    vi.mocked(services.workspaces.createWorkspace).mockReturnValue(pending.promise);

    renderPage(services);

    await user.type(screen.getByLabelText("Repository URL"), WILDFLY_REPOSITORY_URL);
    await user.click(screen.getByRole("button", { name: /preview/i }));
    await screen.findByDisplayValue("wildfly");

    await user.dblClick(screen.getByRole("button", { name: /^save$/i }));

    expect(services.workspaces.createWorkspace).toHaveBeenCalledTimes(1);
    pending.resolve(workspace());
    await screen.findByRole("heading", { name: "Workspace ready" });
  });

  it("waits for workspace checkout result while checkout is running", async () => {
    const user = userEvent.setup();
    const services = servicesForWorkspaceFlow();
    const checkoutResult = deferred<Workspace>();
    vi.mocked(services.workspaces.createWorkspace).mockResolvedValueOnce(
      checkingOutWorkspace()
    );
    vi.mocked(services.workspaces.waitForWorkspaceCheckout).mockReturnValueOnce(
      checkoutResult.promise
    );

    renderPage(services);

    await user.type(screen.getByLabelText("Repository URL"), WILDFLY_REPOSITORY_URL);
    await user.click(screen.getByRole("button", { name: /preview/i }));
    await screen.findByDisplayValue("wildfly");
    await user.click(screen.getByRole("button", { name: /^save$/i }));

    await screen.findByRole("heading", { name: "Checking out repository..." });
    expect(screen.getByRole("button", { name: /update branch/i })).toBeDisabled();

    checkoutResult.resolve(workspace());

    await screen.findByRole("heading", { name: "Workspace ready" });
    expect(services.workspaces.waitForWorkspaceCheckout).toHaveBeenCalledWith(
      expect.objectContaining({
        workspaceId: "workspace-1",
        correlationId: expect.stringMatching(/^ui-workspace-status-correlation-/)
      }),
      expect.any(AbortSignal)
    );
  });

  it("refreshes a checked-out branch and displays the public refresh result", async () => {
    const user = userEvent.setup();
    const services = servicesForWorkspaceFlow();

    renderPage(services);

    await user.type(screen.getByLabelText("Repository URL"), WILDFLY_REPOSITORY_URL);
    await user.click(screen.getByRole("button", { name: /preview/i }));
    await screen.findByDisplayValue("wildfly");
    await user.click(screen.getByRole("button", { name: /^save$/i }));
    await screen.findByRole("heading", { name: "Workspace ready" });

    await user.click(screen.getByRole("button", { name: /update branch/i }));
    await screen.findByRole("heading", { name: "Branch up to date" });

    expect(services.workspaces.refreshBranch).toHaveBeenCalledWith(
      expect.objectContaining({
        workspaceId: "workspace-1",
        workspaceBranchId: "workspace-branch-1",
        idempotencyKey: expect.stringMatching(/^ui-branch-refresh-/)
      })
    );
  });

  it("displays changed branch refresh state with updated snapshot metadata", async () => {
    const user = userEvent.setup();
    const services = servicesForWorkspaceFlow();
    vi.mocked(services.workspaces.refreshBranch).mockResolvedValueOnce({
      workspaceBranchId: "workspace-branch-1",
      repositoryBranch: "main",
      status: "UPDATED",
      changed: true,
      previousCommit: "abc1234",
      resolvedCommit: "def4567",
      sourceSnapshotId: "source-snapshot-2",
      diagnostics: []
    });

    renderPage(services);

    await user.type(screen.getByLabelText("Repository URL"), WILDFLY_REPOSITORY_URL);
    await user.click(screen.getByRole("button", { name: /preview/i }));
    await screen.findByDisplayValue("wildfly");
    await user.click(screen.getByRole("button", { name: /^save$/i }));
    await screen.findByRole("heading", { name: "Workspace ready" });

    await user.click(screen.getByRole("button", { name: /update branch/i }));

    await screen.findByRole("heading", { name: "Branch updated" });
    expect(screen.getByText("def4567")).toBeInTheDocument();
    expect(screen.getByText("source-snapshot-2")).toBeInTheDocument();
  });

  it("sanitizes diagnostics before rendering them", async () => {
    const user = userEvent.setup();
    const services = servicesForWorkspaceFlow();
    vi.mocked(services.workspaces.previewMetadata).mockResolvedValueOnce({
      repositoryKey: "github.com/wildfly/wildfly",
      repositoryHost: "github.com",
      repositoryOwner: "wildfly",
      repositoryName: "wildfly",
      workspaceTitle: "wildfly",
      defaultBranch: "main",
      diagnostics: [
        {
          id: "unsafe-diagnostic",
          severity: "ERROR",
          code: "UNSAFE",
          message:
            "raw stdout token=secret credential=value jdbc:h2:file:/var/lib/forensic-analytics/repository-source-data/repository-source https://user:secret@example.com/repo.git",
          source: "C:\\Users\\private\\File.java",
          observedAt: null
        }
      ]
    });

    renderPage(services);

    await user.type(screen.getByLabelText("Repository URL"), WILDFLY_REPOSITORY_URL);
    await user.click(screen.getByRole("button", { name: /preview/i }));

    await screen.findByText("UNSAFE");
    expect(screen.queryByText(/raw stdout/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/secret/)).not.toBeInTheDocument();
    expect(screen.queryByText(/jdbc:h2/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/C:\\Users/)).not.toBeInTheDocument();
    expect(screen.getByText(/\[stream-redacted]/)).toBeInTheDocument();
    expect(screen.getAllByText(/\[local-path]/).length).toBeGreaterThan(0);
  });
});

const renderPage = (services: ApplicationServices) =>
  render(
    <ApplicationServicesProvider services={services}>
      <CreateWorkspacePage />
    </ApplicationServicesProvider>
  );

const servicesForWorkspaceFlow = (): ApplicationServices => ({
  repositoryAnalysis: {
    listRepositoryAnalyses: vi.fn(),
    getAnalysisJob: vi.fn(),
    startRepositoryAnalysis: vi.fn()
  },
  workspaces: {
    previewMetadata: vi.fn().mockResolvedValue({
      repositoryKey: "github.com/wildfly/wildfly",
      repositoryHost: "github.com",
      repositoryOwner: "wildfly",
      repositoryName: "wildfly",
      workspaceTitle: "wildfly",
      defaultBranch: "main",
      diagnostics: []
    }),
    createWorkspace: vi.fn().mockResolvedValue(workspace()),
    refreshBranch: vi.fn().mockResolvedValue(refreshUnchanged()),
    listWorkspaces: vi.fn(),
    deleteWorkspace: vi.fn(),
    getWorkspace: vi.fn(),
    waitForWorkspaceCheckout: vi.fn().mockResolvedValue(workspace())
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
    repositoryUrl: WILDFLY_REPOSITORY_URL,
    repositoryHost: "github.com",
    repositoryOwner: "wildfly",
    repositoryName: "wildfly",
    defaultBranch: "main"
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

const checkingOutWorkspace = (): Workspace => ({
  ...workspace(),
  branches: [
    {
      ...workspace().branches[0],
      status: "CHECKING_OUT",
      resolvedCommit: null,
      sourceSnapshotId: null,
      sourceRoots: []
    }
  ]
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
