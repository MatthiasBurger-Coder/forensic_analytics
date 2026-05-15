export type DiagnosticSeverity = "INFO" | "WARNING" | "ERROR" | "UNKNOWN";

export interface DiagnosticMessage {
  id: string;
  severity: DiagnosticSeverity;
  code: string | null;
  message: string;
  source: string | null;
  observedAt: string | null;
}
