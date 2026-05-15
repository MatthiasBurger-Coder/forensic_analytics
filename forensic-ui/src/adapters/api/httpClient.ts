import { ApplicationError, type FailureCategory } from "@/application/errors";
import { sanitizeDiagnosticText } from "@/shared/safeText";

import type { ApiConfig } from "./config";
import type { DiagnosticDto, ErrorEnvelopeDto } from "./dtos";
import { mapDiagnosticDto } from "./mappers";

export type Fetcher = typeof fetch;
export type Delay = (milliseconds: number, signal?: AbortSignal) => Promise<void>;

export interface HttpClientOptions extends ApiConfig {
  fetcher?: Fetcher;
  delay?: Delay;
  random?: () => number;
}

interface RequestOptions {
  method?: "GET" | "POST";
  body?: unknown;
  signal?: AbortSignal;
}

export class HttpClient {
  private readonly baseUrl: string;
  private readonly timeoutMs: number;
  private readonly maxGetAttempts: number;
  private readonly baseRetryDelayMs: number;
  private readonly fetcher: Fetcher;
  private readonly delay: Delay;
  private readonly random: () => number;

  constructor(options: HttpClientOptions) {
    this.baseUrl = options.baseUrl.replace(/\/+$/, "");
    this.timeoutMs = options.timeoutMs;
    this.maxGetAttempts = Math.max(1, options.maxGetAttempts);
    this.baseRetryDelayMs = options.baseRetryDelayMs;
    this.fetcher = options.fetcher ?? fetch.bind(globalThis);
    this.delay = options.delay ?? sleep;
    this.random = options.random ?? Math.random;
  }

  async requestJson<T>(
    path: string,
    options: RequestOptions = {}
  ): Promise<T> {
    const method = options.method ?? "GET";
    const attempts = method === "GET" ? this.maxGetAttempts : 1;
    let lastError: ApplicationError | null = null;

    for (let attempt = 1; attempt <= attempts; attempt += 1) {
      try {
        return await this.performRequest<T>(path, method, options);
      } catch (error) {
        const appError = this.toApplicationError(error);
        lastError = appError;

        if (
          method !== "GET" ||
          !appError.retryable ||
          options.signal?.aborted ||
          attempt === attempts
        ) {
          break;
        }

        await this.delay(this.retryDelay(attempt), options.signal);
      }
    }

    if (method === "GET" && attempts > 1 && lastError?.retryable) {
      throw new ApplicationError(
        "RETRY_EXHAUSTED",
        "The backend did not respond successfully before the retry budget was exhausted.",
        {
          retryable: true,
          correlationId: lastError.correlationId,
          diagnostics: lastError.diagnostics,
          statusCode: lastError.statusCode,
          cause: lastError
        }
      );
    }

    throw lastError ?? new ApplicationError("UNEXPECTED_ERROR", "Request failed.");
  }

  private async performRequest<T>(
    path: string,
    method: "GET" | "POST",
    options: RequestOptions
  ): Promise<T> {
    const timeout = composeAbortSignal(options.signal, this.timeoutMs);

    try {
      const response = await this.fetcher(this.url(path), {
        method,
        headers:
          options.body === undefined
            ? { Accept: "application/json" }
            : {
                Accept: "application/json",
                "Content-Type": "application/json"
              },
        body: options.body === undefined ? undefined : JSON.stringify(options.body),
        signal: timeout.signal
      });

      if (!response.ok) {
        throw await this.errorFromResponse(response);
      }

      if (response.status === 204) {
        return null as T;
      }

      return (await response.json()) as T;
    } catch (error) {
      if (timeout.timedOut()) {
        throw new ApplicationError(
          "TIMEOUT",
          "The backend request exceeded the configured timeout.",
          { retryable: true, cause: error }
        );
      }

      if (options.signal?.aborted) {
        throw new ApplicationError("UNEXPECTED_ERROR", "The request was canceled.", {
          cause: error
        });
      }

      throw error;
    } finally {
      timeout.dispose();
    }
  }

  private async errorFromResponse(response: Response): Promise<ApplicationError> {
    const envelope = await readErrorEnvelope(response);
    const category = categoryFromEnvelope(envelope) ?? categoryFromStatus(response.status);
    const retryable =
      typeof envelope?.retryable === "boolean"
        ? envelope.retryable
        : response.status === 408 || response.status === 429 || response.status >= 500;

    return new ApplicationError(
      category,
      safeEnvelopeMessage(envelope) ?? messageForCategory(category),
      {
        retryable,
        correlationId:
          typeof envelope?.correlationId === "string"
            ? envelope.correlationId
            : response.headers.get("x-correlation-id"),
        diagnostics: Array.isArray(envelope?.diagnostics)
          ? envelope.diagnostics.map((item, index) =>
              isRecord(item)
                ? mapDiagnosticDto(item as DiagnosticDto, index)
                : mapDiagnosticDto({ message: item }, index)
            )
          : [],
        statusCode: response.status
      }
    );
  }

  private toApplicationError(error: unknown): ApplicationError {
    if (error instanceof ApplicationError) {
      return error;
    }

    if (error instanceof TypeError) {
      return new ApplicationError(
        "BACKEND_UNAVAILABLE",
        "The backend is unavailable or the browser could not reach it.",
        { retryable: true, cause: error }
      );
    }

    return new ApplicationError(
      "UNEXPECTED_ERROR",
      "The backend response could not be processed safely.",
      { cause: error }
    );
  }

  private retryDelay(attempt: number): number {
    const exponential = this.baseRetryDelayMs * 2 ** (attempt - 1);
    const jitter = Math.floor(exponential * 0.35 * this.random());

    return exponential + jitter;
  }

  private url(path: string): string {
    const normalizedPath = path.startsWith("/") ? path : `/${path}`;

    return `${this.baseUrl}${normalizedPath}`;
  }
}

export const sleep: Delay = (milliseconds, signal) =>
  new Promise((resolve, reject) => {
    if (signal?.aborted) {
      reject(new DOMException("Delay canceled", "AbortError"));
      return;
    }

    const timeoutId = window.setTimeout(resolve, milliseconds);
    signal?.addEventListener(
      "abort",
      () => {
        window.clearTimeout(timeoutId);
        reject(new DOMException("Delay canceled", "AbortError"));
      },
      { once: true }
    );
  });

const composeAbortSignal = (
  externalSignal: AbortSignal | undefined,
  timeoutMs: number
): {
  signal: AbortSignal;
  timedOut: () => boolean;
  dispose: () => void;
} => {
  const controller = new AbortController();
  let timedOut = false;

  const timeoutId = window.setTimeout(() => {
    timedOut = true;
    controller.abort(new DOMException("Request timeout", "TimeoutError"));
  }, timeoutMs);

  const abortFromExternal = () => {
    controller.abort(
      externalSignal?.reason ?? new DOMException("Request canceled", "AbortError")
    );
  };

  externalSignal?.addEventListener("abort", abortFromExternal, { once: true });

  return {
    signal: controller.signal,
    timedOut: () => timedOut,
    dispose: () => {
      window.clearTimeout(timeoutId);
      externalSignal?.removeEventListener("abort", abortFromExternal);
    }
  };
};

const readErrorEnvelope = async (
  response: Response
): Promise<ErrorEnvelopeDto | null> => {
  try {
    const value = (await response.json()) as unknown;
    return isRecord(value) ? value : null;
  } catch {
    return null;
  }
};

const categoryFromEnvelope = (
  envelope: ErrorEnvelopeDto | null
): FailureCategory | null => {
  const code = typeof envelope?.code === "string" ? envelope.code : "";

  if (
    code === "VALIDATION_ERROR" ||
    code === "NOT_FOUND" ||
    code === "BACKEND_UNAVAILABLE" ||
    code === "TIMEOUT" ||
    code === "RETRY_EXHAUSTED" ||
    code === "UNEXPECTED_ERROR"
  ) {
    return code;
  }

  return null;
};

const categoryFromStatus = (status: number): FailureCategory => {
  if (status === 400 || status === 422) {
    return "VALIDATION_ERROR";
  }

  if (status === 404) {
    return "NOT_FOUND";
  }

  if (status === 408) {
    return "TIMEOUT";
  }

  if (status === 429 || status >= 500) {
    return "BACKEND_UNAVAILABLE";
  }

  return "UNEXPECTED_ERROR";
};

const safeEnvelopeMessage = (envelope: ErrorEnvelopeDto | null): string | null =>
  typeof envelope?.message === "string"
    ? sanitizeDiagnosticText(envelope.message)
    : null;

const messageForCategory = (category: FailureCategory): string => {
  switch (category) {
    case "VALIDATION_ERROR":
      return "The request was rejected by backend validation.";
    case "NOT_FOUND":
      return "The requested analysis resource was not found.";
    case "BACKEND_UNAVAILABLE":
      return "The backend is unavailable.";
    case "TIMEOUT":
      return "The backend request timed out.";
    case "RETRY_EXHAUSTED":
      return "The retry budget was exhausted.";
    case "UNEXPECTED_ERROR":
      return "The backend returned an unexpected response.";
  }
};

const isRecord = (value: unknown): value is Record<string, unknown> =>
  typeof value === "object" && value !== null && !Array.isArray(value);
