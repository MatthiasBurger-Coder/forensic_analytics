import { RefreshCcw } from "lucide-react";
import { useParams } from "react-router-dom";

import { useAnalysisJob } from "@/application/hooks/useAnalysisJob";
import { DiagnosticList } from "@/widgets/DiagnosticList";
import {
  EvidenceClassGrid,
  type EvidenceClassItem
} from "@/widgets/EvidenceClassGrid";
import { StatusBadge } from "@/widgets/StatusBadge";
import { ErrorPanel, LoadingPanel, StaleNotice } from "@/widgets/StatePanels";

export const AnalysisJobDetailPage = () => {
  const { analysisRunId = "" } = useParams();
  const job = useAnalysisJob(analysisRunId);
  const data = job.data;

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <span className="eyebrow">Analysis job</span>
          <h1>{analysisRunId}</h1>
        </div>
        <button className="button secondary" type="button" onClick={job.reload}>
          <RefreshCcw size={16} aria-hidden="true" />
          Refresh
        </button>
      </header>

      {job.stale ? <StaleNotice onRetry={job.reload} /> : null}

      {job.loading && !data ? (
        <LoadingPanel label="Loading analysis job detail." />
      ) : job.error && !data ? (
        <ErrorPanel error={job.error} onRetry={job.reload} />
      ) : data ? (
        <div className="stack">
          <section className="panel">
            <div className="panel-header">
              <div>
                <span className="eyebrow">Lifecycle</span>
                <h2>Session state</h2>
              </div>
              <StatusBadge status={data.status} />
            </div>
            <dl className="detail-grid">
              <div>
                <dt>Backend status</dt>
                <dd>{data.status.backendStatus ?? "Unavailable"}</dd>
              </div>
              <div>
                <dt>Polling</dt>
                <dd>{job.polling ? "Active" : "Stopped"}</dd>
              </div>
              <div>
                <dt>Workspace</dt>
                <dd>{data.workspaceId ?? "Unavailable"}</dd>
              </div>
              <div>
                <dt>Checkout</dt>
                <dd>{data.checkoutStatus ?? "Unavailable"}</dd>
              </div>
              <div>
                <dt>Created</dt>
                <dd>{data.createdAt ?? "Unavailable"}</dd>
              </div>
              <div>
                <dt>Updated</dt>
                <dd>{data.lastUpdatedAt ?? "Unavailable"}</dd>
              </div>
            </dl>
          </section>

          <section className="panel">
            <div className="panel-header">
              <div>
                <span className="eyebrow">Repository</span>
                <h2>{data.repositoryUrl || "Unavailable repository"}</h2>
              </div>
            </div>
            <dl className="detail-grid">
              <div>
                <dt>Branch</dt>
                <dd>{data.branch ?? "Absent"}</dd>
              </div>
              <div>
                <dt>Commit</dt>
                <dd>{data.commit ?? "Absent"}</dd>
              </div>
              <div>
                <dt>Resolved commit</dt>
                <dd>{data.resolvedCommit ?? "Unresolved"}</dd>
              </div>
              <div>
                <dt>Source roots</dt>
                <dd>
                  {data.sourceRoots.length
                    ? data.sourceRoots.join(", ")
                    : "Unavailable"}
                </dd>
              </div>
            </dl>
          </section>

          <section className="panel">
            <div className="panel-header">
              <div>
                <span className="eyebrow">Evidence classes</span>
                <h2>Gateway public status</h2>
              </div>
            </div>
            <EvidenceClassGrid items={evidenceClassItems(data)} />
          </section>

          <section className="panel">
            <div className="panel-header">
              <div>
                <span className="eyebrow">Evidence gaps</span>
                <h2>Diagnostics</h2>
              </div>
            </div>
            {data.diagnostics.length ? (
              <DiagnosticList diagnostics={data.diagnostics} />
            ) : (
              <p className="muted-text">No diagnostics were returned.</p>
            )}
          </section>
        </div>
      ) : null}
    </section>
  );
};

const evidenceClassItems = (data: {
  sourceSnapshotStatus: string | null;
  resolvedCommit: string | null;
  sourceRoots: string[];
  btmDeliveryStatus: string | null;
  btmDeliveryService: string | null;
  diagnostics: unknown[];
  workflow: string | null;
}): EvidenceClassItem[] => [
  {
    label: "Confirmed evidence",
    value: data.resolvedCommit ?? data.sourceSnapshotStatus ?? "Unavailable"
  },
  {
    label: "Derived analysis",
    value: data.sourceRoots.length
      ? `${data.sourceRoots.length} source roots`
      : data.workflow ?? "Unavailable"
  },
  {
    label: "Generated BTM artifacts",
    value: data.btmDeliveryStatus ?? "Unavailable"
  },
  {
    label: "Diagnostics",
    value: `${data.diagnostics.length} messages`
  },
  {
    label: "Unresolved gaps",
    value: data.diagnostics.length ? "See diagnostics" : "Not reported"
  },
  {
    label: "Hypotheses",
    value: "Not provided"
  },
  {
    label: "Suggested fixes",
    value: data.btmDeliveryService ?? "Not provided"
  }
];
