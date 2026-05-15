import type { DiagnosticMessage } from "@/domain/diagnostic";

export const DiagnosticList = ({
  diagnostics
}: {
  diagnostics: DiagnosticMessage[];
}) => (
  <div className="diagnostic-list">
    {diagnostics.map((diagnostic) => (
      <article
        className={`diagnostic diagnostic-${diagnostic.severity.toLowerCase()}`}
        key={diagnostic.id}
      >
        <header>
          <strong>{diagnostic.severity}</strong>
          <span>{diagnostic.code ?? "NO_CODE"}</span>
          <span>{diagnostic.observedAt ?? "No timestamp"}</span>
        </header>
        <pre>{diagnostic.message}</pre>
        {diagnostic.source ? <footer>{diagnostic.source}</footer> : null}
      </article>
    ))}
  </div>
);
