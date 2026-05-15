import { useDiagnostics } from "@/application/hooks/useDiagnostics";
import { DiagnosticList } from "@/widgets/DiagnosticList";
import {
  EmptyPanel,
  ErrorPanel,
  LoadingPanel,
  StaleNotice
} from "@/widgets/StatePanels";

export const DiagnosticsPage = () => {
  const diagnostics = useDiagnostics();
  const data = diagnostics.data ?? [];

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <span className="eyebrow">Sanitized</span>
          <h1>Diagnostics</h1>
        </div>
      </header>

      {diagnostics.stale ? <StaleNotice onRetry={diagnostics.reload} /> : null}

      {diagnostics.loading ? (
        <LoadingPanel label="Loading diagnostics." />
      ) : diagnostics.error && !diagnostics.data ? (
        <ErrorPanel error={diagnostics.error} onRetry={diagnostics.reload} />
      ) : diagnostics.empty ? (
        <EmptyPanel
          title="No diagnostics"
          body="No repository analysis sessions currently expose diagnostics."
        />
      ) : (
        <section className="panel">
          <div className="panel-header">
            <div>
              <span className="eyebrow">Current</span>
              <h2>{data.length} diagnostic messages</h2>
            </div>
          </div>
          <DiagnosticList diagnostics={data} />
        </section>
      )}
    </section>
  );
};
