import { Link } from "react-router-dom";

import { useWorkspaces } from "@/application/hooks/useWorkspaces";
import {
  EmptyPanel,
  ErrorPanel,
  LoadingPanel,
  StaleNotice
} from "@/widgets/StatePanels";

export const WorkspaceListPage = () => {
  const workspaces = useWorkspaces();
  const data = workspaces.data ?? [];

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <span className="eyebrow">Workspaces</span>
          <h1>Repository checkout workspaces</h1>
        </div>
        <Link className="button primary" to="/workspaces/new">
          Create workspace
        </Link>
      </header>

      {workspaces.stale ? <StaleNotice onRetry={workspaces.reload} /> : null}

      {workspaces.loading ? (
        <LoadingPanel label="Loading repository checkout workspace views." />
      ) : workspaces.error && !workspaces.data ? (
        <ErrorPanel error={workspaces.error} onRetry={workspaces.reload} />
      ) : workspaces.empty ? (
        <EmptyPanel
          title="No workspace list route"
          body="Create or open a workspace through the verified workspace routes."
        />
      ) : (
        <div className="stack">
          {data.map((workspace) => (
            <section className="panel" key={workspace.workspaceId}>
              <div className="panel-header">
                <div>
                  <span className="eyebrow">Workspace</span>
                  <h2>{workspace.workspaceTitle || workspace.workspaceId}</h2>
                </div>
                <span className="pill">
                  {workspace.status}
                </span>
              </div>
              <dl className="detail-grid">
                <div>
                  <dt>Repository</dt>
                  <dd>{workspace.repository.repositoryKey || "Unavailable"}</dd>
                </div>
                <div>
                  <dt>Branches</dt>
                  <dd>{workspace.branches.length}</dd>
                </div>
                <div>
                  <dt>Default branch</dt>
                  <dd>{workspace.repository.defaultBranch ?? "Unavailable"}</dd>
                </div>
              </dl>
            </section>
          ))}
        </div>
      )}
    </section>
  );
};
