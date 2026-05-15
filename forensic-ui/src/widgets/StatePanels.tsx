import { AlertTriangle, RefreshCcw } from "lucide-react";
import { Link } from "react-router-dom";

import { toUserMessage } from "@/application/errors";

export const LoadingPanel = ({ label }: { label: string }) => (
  <div className="panel state-panel" aria-live="polite">
    <span className="spinner" aria-hidden="true" />
    <p>{label}</p>
  </div>
);

export const EmptyPanel = ({
  title,
  body
}: {
  title: string;
  body: string;
}) => (
  <div className="panel state-panel">
    <span className="eyebrow">No records</span>
    <h2>{title}</h2>
    <p>{body}</p>
  </div>
);

export const ErrorPanel = ({
  error,
  onRetry
}: {
  error: unknown;
  onRetry?: () => void;
}) => (
  <div className="panel state-panel danger" role="alert">
    <AlertTriangle size={22} aria-hidden="true" />
    <h2>Request failed</h2>
    <p>{toUserMessage(error)}</p>
    <div className="button-row">
      {onRetry ? (
        <button className="button secondary" type="button" onClick={onRetry}>
          <RefreshCcw size={16} aria-hidden="true" />
          Retry
        </button>
      ) : null}
      <Link className="button ghost" to="/backend-unavailable">
        Backend state
      </Link>
    </div>
  </div>
);

export const StaleNotice = ({ onRetry }: { onRetry: () => void }) => (
  <div className="notice warning" role="status">
    <AlertTriangle size={18} aria-hidden="true" />
    <span>Showing stale data because the backend is currently unreachable.</span>
    <button className="button ghost compact" type="button" onClick={onRetry}>
      <RefreshCcw size={15} aria-hidden="true" />
      Refresh
    </button>
  </div>
);
