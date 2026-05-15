export interface ApiConfig {
  baseUrl: string;
  timeoutMs: number;
  maxGetAttempts: number;
  baseRetryDelayMs: number;
}

export const DEFAULT_API_CONFIG: ApiConfig = {
  baseUrl: "/api",
  timeoutMs: 8000,
  maxGetAttempts: 3,
  baseRetryDelayMs: 150
};

export const resolveApiConfig = (
  overrides: Partial<ApiConfig> = {}
): ApiConfig => {
  const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL;

  return {
    ...DEFAULT_API_CONFIG,
    baseUrl:
      overrides.baseUrl ??
      (configuredBaseUrl && configuredBaseUrl.trim()
        ? configuredBaseUrl.trim()
        : DEFAULT_API_CONFIG.baseUrl),
    timeoutMs: overrides.timeoutMs ?? DEFAULT_API_CONFIG.timeoutMs,
    maxGetAttempts:
      overrides.maxGetAttempts ?? DEFAULT_API_CONFIG.maxGetAttempts,
    baseRetryDelayMs:
      overrides.baseRetryDelayMs ?? DEFAULT_API_CONFIG.baseRetryDelayMs
  };
};
