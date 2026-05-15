import type { AnalysisStatusState } from "@/domain/analysisStatus";

export const StatusBadge = ({ status }: { status: AnalysisStatusState }) => (
  <span
    className={`status-badge status-${status.lifecycle.toLowerCase()}`}
    title={
      status.backendStatus
        ? `Backend status: ${status.backendStatus}`
        : "Backend status unavailable"
    }
  >
    <span aria-hidden="true" />
    {status.lifecycle}
  </span>
);
