import { useRef, useState } from "react";
import {
  CheckCircle2,
  Plus,
  RefreshCcw,
  Trash2
} from "lucide-react";
import { Link } from "react-router-dom";

import { useWorkspaces } from "@/application/hooks/useWorkspaces";
import { useApplicationServices } from "@/application/ApplicationServicesContext";
import { toUserMessage } from "@/application/errors";
import type { Workspace, WorkspaceBranch } from "@/domain/workspace";
import {
  EmptyPanel,
  ErrorPanel,
  LoadingPanel,
  StaleNotice
} from "@/widgets/StatePanels";

interface WorkspaceRow {
  workspace: Workspace;
  branch: WorkspaceBranch | null;
  rowId: string;
}

export const WorkspaceListPage = () => {
  const services = useApplicationServices();
  const workspaces = useWorkspaces();
  const refreshInFlight = useRef<Promise<void> | null>(null);
  const deleteInFlight = useRef<Promise<void> | null>(null);
  const [refreshingBranchId, setRefreshingBranchId] = useState<string | null>(
    null
  );
  const [deletingWorkspaceId, setDeletingWorkspaceId] = useState<string | null>(
    null
  );
  const [actionMessage, setActionMessage] = useState<string | null>(null);
  const [actionError, setActionError] = useState<unknown>(null);
  const data = workspaces.data ?? [];
  const rows = data.flatMap(workspaceRows);
  const actionBusy = refreshingBranchId !== null || deletingWorkspaceId !== null;

  const refreshBranch = async (workspace: Workspace, branch: WorkspaceBranch) => {
    if (refreshInFlight.current) {
      await refreshInFlight.current;
      return;
    }

    setRefreshingBranchId(branch.workspaceBranchId);
    setActionMessage(null);
    setActionError(null);

    refreshInFlight.current = services.workspaces
      .refreshBranch({
        workspaceId: workspace.workspaceId,
        workspaceBranchId: branch.workspaceBranchId,
        correlationId: createPublicId("ui-list-branch-refresh-correlation"),
        idempotencyKey: createPublicId("ui-list-branch-refresh")
      })
      .then((result) => {
        setActionMessage(
          result.changed
            ? `Branch ${result.repositoryBranch} updated.`
            : `Branch ${result.repositoryBranch} is up to date.`
        );
        workspaces.reload();
      })
      .catch((caught) => {
        setActionError(caught);
      })
      .finally(() => {
        refreshInFlight.current = null;
        setRefreshingBranchId(null);
      });

    await refreshInFlight.current;
  };

  const deleteWorkspace = async (workspace: Workspace) => {
    if (deleteInFlight.current) {
      await deleteInFlight.current;
      return;
    }

    setDeletingWorkspaceId(workspace.workspaceId);
    setActionMessage(null);
    setActionError(null);

    deleteInFlight.current = services.workspaces
      .deleteWorkspace({
        workspaceId: workspace.workspaceId,
        correlationId: createPublicId("ui-workspace-delete-correlation"),
        idempotencyKey: createPublicId("ui-workspace-delete")
      })
      .then((result) => {
        setActionMessage(`Workspace ${result.workspaceId} cleaned.`);
        workspaces.reload();
      })
      .catch((caught) => {
        setActionError(caught);
      })
      .finally(() => {
        deleteInFlight.current = null;
        setDeletingWorkspaceId(null);
      });

    await deleteInFlight.current;
  };

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <span className="eyebrow">Workspaces</span>
          <h1>Repository checkout workspaces</h1>
        </div>
        <Link className="button primary" to="/workspaces/new">
          <Plus size={16} aria-hidden="true" />
          Create workspace
        </Link>
      </header>

      {workspaces.stale ? <StaleNotice onRetry={workspaces.reload} /> : null}
      {actionMessage ? (
        <div className="notice" role="status">
          <CheckCircle2 size={18} aria-hidden="true" />
          {actionMessage}
        </div>
      ) : null}
      {actionError ? (
        <div className="notice danger" role="alert">
          {toUserMessage(actionError)}
        </div>
      ) : null}

      {workspaces.loading ? (
        <LoadingPanel label="Loading repository checkout workspace views." />
      ) : workspaces.error && !workspaces.data ? (
        <ErrorPanel error={workspaces.error} onRetry={workspaces.reload} />
      ) : workspaces.empty ? (
        <EmptyPanel
          title="No workspaces yet"
          body="Create a repository checkout workspace to make it available in this list."
        />
      ) : (
        <div className="panel table-wrap">
          <table className="workspace-table">
            <thead>
              <tr>
                <th>Workspace_ID</th>
                <th>Workspace</th>
                <th>Selected Branch</th>
                <th>Status</th>
                <th>Repository</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {rows.map(({ workspace, branch, rowId }) => (
                <tr key={rowId}>
                  <td className="mono-cell">{workspace.workspaceId}</td>
                  <td>
                    <strong>{workspace.workspaceTitle || "Unavailable"}</strong>
                  </td>
                  <td>
                    <div className="workspace-branch-cell">
                      <span>{branch?.repositoryBranch ?? "Unavailable"}</span>
                      {branch ? (
                        <small>{branch.workspaceBranchId}</small>
                      ) : null}
                    </div>
                  </td>
                  <td>
                    <div className="status-stack">
                      <StatusBadge label={workspace.status} />
                      {branch ? <StatusBadge label={branch.status} /> : null}
                    </div>
                  </td>
                  <td className="truncate">
                    {workspace.repository.repositoryKey || "Unavailable"}
                  </td>
                  <td>
                    <div className="table-actions">
                      <button
                        aria-label={
                          branch
                            ? `Update branch ${branch.repositoryBranch} for workspace ${workspace.workspaceId}`
                            : `Update branch for workspace ${workspace.workspaceId}`
                        }
                        className="icon-button"
                        disabled={
                          actionBusy || !branch || !canRefreshBranch(branch)
                        }
                        onClick={() => {
                          if (branch) {
                            void refreshBranch(workspace, branch);
                          }
                        }}
                        title="Update branch"
                        type="button"
                      >
                        <RefreshCcw
                          size={16}
                          aria-hidden="true"
                          className={
                            refreshingBranchId === branch?.workspaceBranchId
                              ? "spin-icon"
                              : undefined
                          }
                        />
                      </button>
                      <button
                        aria-label={`Delete workspace ${workspace.workspaceId}`}
                        className="icon-button danger-action"
                        disabled={actionBusy || workspace.status === "CLEANED"}
                        onClick={() => {
                          void deleteWorkspace(workspace);
                        }}
                        title="Delete workspace"
                        type="button"
                      >
                        <Trash2 size={16} aria-hidden="true" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
};

const workspaceRows = (workspace: Workspace): WorkspaceRow[] =>
  workspace.branches.length > 0
    ? workspace.branches.map((branch) => ({
        workspace,
        branch,
        rowId: `${workspace.workspaceId}:${branch.workspaceBranchId}`
      }))
    : [
        {
          workspace,
          branch: null,
          rowId: `${workspace.workspaceId}:no-branch`
        }
      ];

const StatusBadge = ({ label }: { label: string }) => (
  <span className={`status-badge status-${label.toLowerCase()}`}>
    <span aria-hidden="true" />
    {label}
  </span>
);

const canRefreshBranch = (branch: WorkspaceBranch): boolean =>
  branch.status === "CHECKED_OUT" ||
  branch.status === "UP_TO_DATE" ||
  branch.status === "UPDATED";

const createPublicId = (prefix: string): string => {
  if (globalThis.crypto?.randomUUID) {
    return `${prefix}-${globalThis.crypto.randomUUID()}`;
  }

  return `${prefix}-${Date.now().toString(36)}`;
};
