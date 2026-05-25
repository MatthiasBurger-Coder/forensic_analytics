export interface ApiConfig {
  baseUrl: string;
  timeoutMs: number;
  maxGetAttempts: number;
  baseRetryDelayMs: number;
}

export const DEFAULT_API_CONFIG: ApiConfig = {
  baseUrl: "/api",
  timeoutMs: 120000,
  maxGetAttempts: 3,
  baseRetryDelayMs: 150
};

export const resolveApiConfig = (
  overrides: Partial<ApiConfig> = {}
): ApiConfig => {
  const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL;
  const configuredTimeoutMs = positiveInteger(
    import.meta.env.VITE_API_TIMEOUT_MS
  );

  return {
    ...DEFAULT_API_CONFIG,
    baseUrl:
      overrides.baseUrl ??
      (configuredBaseUrl && configuredBaseUrl.trim()
        ? configuredBaseUrl.trim()
        : DEFAULT_API_CONFIG.baseUrl),
    timeoutMs:
      overrides.timeoutMs ?? configuredTimeoutMs ?? DEFAULT_API_CONFIG.timeoutMs,
    maxGetAttempts:
      overrides.maxGetAttempts ?? DEFAULT_API_CONFIG.maxGetAttempts,
    baseRetryDelayMs:
      overrides.baseRetryDelayMs ?? DEFAULT_API_CONFIG.baseRetryDelayMs
  };
};

const positiveInteger = (value: unknown): number | null => {
  if (typeof value !== "string" || !value.trim()) {
    return null;
  }
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? Math.trunc(parsed) : null;
};
