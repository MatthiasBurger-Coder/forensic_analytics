import type { DiagnosticMessage } from "@/domain/diagnostic";

export interface DiagnosticsPort {
  collectDiagnostics(signal?: AbortSignal): Promise<DiagnosticMessage[]>;
}
