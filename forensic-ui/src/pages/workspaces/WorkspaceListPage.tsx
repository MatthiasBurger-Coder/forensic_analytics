import { Link } from "react-router-dom";

import { useWorkspaces } from "@/application/hooks/useWorkspaces";
import { AnalysisTable } from "@/widgets/AnalysisTable";
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
          <span className="eyebrow">Sessions</span>
          <h1>Repository-analysis workspaces</h1>
        </div>
        <Link className="button primary" to="/repository-analyses/new">
          Register session
        </Link>
      </header>

      {workspaces.stale ? <StaleNotice onRetry={workspaces.reload} /> : null}

      {workspaces.loading ? (
        <LoadingPanel label="Loading repository-analysis workspace views." />
      ) : workspaces.error && !workspaces.data ? (
        <ErrorPanel error={workspaces.error} onRetry={workspaces.reload} />
      ) : workspaces.empty ? (
        <EmptyPanel
          title="No repository-analysis workspace views"
          body="Workspace views appear only when repository analysis sessions include workspace evidence."
        />
      ) : (
        <div className="stack">
          {data.map((workspace) => (
            <section className="panel" key={workspace.workspaceId}>
              <div className="panel-header">
                <div>
                  <span className="eyebrow">Workspace</span>
                  <h2>{workspace.workspaceId}</h2>
                </div>
                <span className="pill">
                  {workspace.repositoryAnalyses.length} session
                  {workspace.repositoryAnalyses.length === 1 ? "" : "s"}
                </span>
              </div>
              <dl className="detail-grid">
                <div>
                  <dt>Repository</dt>
                  <dd>{workspace.repositoryUrl ?? "Unavailable"}</dd>
                </div>
                <div>
                  <dt>Branch</dt>
                  <dd>{workspace.branch ?? "Absent"}</dd>
                </div>
                <div>
                  <dt>Created</dt>
                  <dd>{workspace.createdAt ?? "Unavailable"}</dd>
                </div>
              </dl>
              <AnalysisTable analyses={workspace.repositoryAnalyses} />
            </section>
          ))}
        </div>
      )}
    </section>
  );
};
