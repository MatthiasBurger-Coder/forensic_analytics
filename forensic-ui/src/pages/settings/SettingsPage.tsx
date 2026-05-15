import { Settings } from "lucide-react";

export const SettingsPage = () => (
  <section className="page">
    <header className="page-header">
      <div>
        <span className="eyebrow">Console</span>
        <h1>Settings</h1>
      </div>
    </header>

    <div className="panel state-panel">
      <Settings size={28} aria-hidden="true" />
      <h2>Settings placeholder</h2>
      <p>Runtime configuration controls are not wired in this slice.</p>
    </div>
  </section>
);
