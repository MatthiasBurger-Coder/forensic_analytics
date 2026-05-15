export type AnalysisLifecycle =
  | "REGISTERED"
  | "ACCEPTED"
  | "DISPATCHABLE"
  | "RUNNING"
  | "RETRYABLE"
  | "SUCCESS"
  | "FAILED"
  | "CLEANED"
  | "CANCELED"
  | "UNKNOWN";

export interface AnalysisStatusState {
  backendStatus: string | null;
  lifecycle: AnalysisLifecycle;
  terminal: boolean;
}

const TERMINAL_LIFECYCLES: ReadonlySet<AnalysisLifecycle> = new Set([
  "SUCCESS",
  "FAILED",
  "CLEANED",
  "CANCELED"
]);

export const isTerminalLifecycle = (lifecycle: AnalysisLifecycle): boolean =>
  TERMINAL_LIFECYCLES.has(lifecycle);

export const createStatusState = (
  backendStatus: string | null,
  lifecycle: AnalysisLifecycle
): AnalysisStatusState => ({
  backendStatus,
  lifecycle,
  terminal: isTerminalLifecycle(lifecycle)
});
