import type { DiagnosticMessage } from "@/domain/diagnostic";

export type FailureCategory =
  | "VALIDATION_ERROR"
  | "NOT_FOUND"
  | "BACKEND_UNAVAILABLE"
  | "TIMEOUT"
  | "RETRY_EXHAUSTED"
  | "UNEXPECTED_ERROR";

export class ApplicationError extends Error {
  readonly category: FailureCategory;
  readonly retryable: boolean;
  readonly correlationId: string | null;
  readonly diagnostics: DiagnosticMessage[];
  readonly statusCode: number | null;

  constructor(
    category: FailureCategory,
    message: string,
    options: {
      retryable?: boolean;
      correlationId?: string | null;
      diagnostics?: DiagnosticMessage[];
      statusCode?: number | null;
      cause?: unknown;
    } = {}
  ) {
    super(message, { cause: options.cause });
    this.name = "ApplicationError";
    this.category = category;
    this.retryable = options.retryable ?? false;
    this.correlationId = options.correlationId ?? null;
    this.diagnostics = options.diagnostics ?? [];
    this.statusCode = options.statusCode ?? null;
  }
}

export const isBackendUnavailableError = (error: unknown): boolean =>
  error instanceof ApplicationError &&
  (error.category === "BACKEND_UNAVAILABLE" ||
    error.category === "TIMEOUT" ||
    error.category === "RETRY_EXHAUSTED");

export const toUserMessage = (error: unknown): string => {
  if (error instanceof ApplicationError) {
    return error.message;
  }

  return "The requested operation failed before a safe diagnostic response was available.";
};
