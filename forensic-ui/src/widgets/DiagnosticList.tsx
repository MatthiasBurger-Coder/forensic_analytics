import type { DiagnosticMessage } from "@/domain/diagnostic";
import { sanitizeDiagnosticText } from "@/shared/safeText";

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
        <pre>{sanitizeDiagnosticText(diagnostic.message)}</pre>
        {diagnostic.source ? (
          <footer>{sanitizeDiagnosticText(diagnostic.source)}</footer>
        ) : null}
      </article>
    ))}
  </div>
);
