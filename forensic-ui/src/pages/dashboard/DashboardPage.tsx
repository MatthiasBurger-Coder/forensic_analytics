import { AlertCircle, CheckCircle2, Clock3, Database } from "lucide-react";

import { useRepositoryAnalyses } from "@/application/hooks/useRepositoryAnalyses";
import { AnalysisTable } from "@/widgets/AnalysisTable";
import {
  EmptyPanel,
  ErrorPanel,
  LoadingPanel,
  StaleNotice
} from "@/widgets/StatePanels";

export const DashboardPage = () => {
  const analyses = useRepositoryAnalyses();
  const data = analyses.data ?? [];
  const running = data.filter((item) => !item.status.terminal).length;
  const failed = data.filter((item) => item.status.lifecycle === "FAILED").length;
  const succeeded = data.filter(
    (item) => item.status.lifecycle === "SUCCESS"
  ).length;
  const diagnostics = data.reduce(
    (total, item) => total + item.diagnostics.length,
    0
  );

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <span className="eyebrow">Operations</span>
          <h1>Dashboard</h1>
        </div>
      </header>

      {analyses.stale ? <StaleNotice onRetry={analyses.reload} /> : null}

      <div className="metric-grid">
        <Metric icon={Database} label="Sessions" value={data.length} />
        <Metric icon={Clock3} label="In progress" value={running} />
        <Metric icon={CheckCircle2} label="Successful" value={succeeded} />
        <Metric icon={AlertCircle} label="Diagnostics" value={diagnostics + failed} />
      </div>

      {analyses.loading ? (
        <LoadingPanel label="Loading repository analysis sessions." />
      ) : analyses.error && !analyses.data ? (
        <ErrorPanel error={analyses.error} onRetry={analyses.reload} />
      ) : analyses.empty ? (
        <EmptyPanel
          title="No repository analysis sessions"
          body="Register a repository analysis session to populate the dashboard."
        />
      ) : (
        <section className="panel">
          <div className="panel-header">
            <div>
              <span className="eyebrow">Recent</span>
              <h2>Repository analysis sessions</h2>
            </div>
          </div>
          <AnalysisTable analyses={data} />
        </section>
      )}
    </section>
  );
};

const Metric = ({
  icon: Icon,
  label,
  value
}: {
  icon: typeof Database;
  label: string;
  value: number;
}) => (
  <div className="metric">
    <Icon size={19} aria-hidden="true" />
    <span>{label}</span>
    <strong>{value}</strong>
  </div>
);
