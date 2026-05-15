import { RadioTower, RefreshCcw } from "lucide-react";
import { Link } from "react-router-dom";

export const BackendUnavailablePage = () => (
  <section className="page">
    <header className="page-header">
      <div>
        <span className="eyebrow">Dependency</span>
        <h1>Backend unavailable</h1>
      </div>
    </header>

    <div className="panel state-panel danger">
      <RadioTower size={28} aria-hidden="true" />
      <h2>REST backend is not reachable</h2>
      <p>
        Data-loading views preserve stale data when available and require manual
        retry after the retry budget is exhausted.
      </p>
      <Link className="button secondary" to="/">
        <RefreshCcw size={16} aria-hidden="true" />
        Return to dashboard
      </Link>
    </div>
  </section>
);
