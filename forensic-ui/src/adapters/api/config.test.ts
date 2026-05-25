import { afterEach, describe, expect, it, vi } from "vitest";

import { DEFAULT_API_CONFIG, resolveApiConfig } from "./config";

describe("API config", () => {
  afterEach(() => {
    vi.unstubAllEnvs();
  });

  it("keeps the default request timeout aligned with checkout operations", () => {
    expect(DEFAULT_API_CONFIG.timeoutMs).toBe(120000);
    expect(resolveApiConfig().timeoutMs).toBe(120000);
  });

  it("allows runtime API timeout configuration", () => {
    vi.stubEnv("VITE_API_TIMEOUT_MS", "180000");

    expect(resolveApiConfig().timeoutMs).toBe(180000);
  });

  it("keeps explicit timeout overrides authoritative", () => {
    vi.stubEnv("VITE_API_TIMEOUT_MS", "180000");

    expect(resolveApiConfig({ timeoutMs: 1000 }).timeoutMs).toBe(1000);
  });

  it("ignores invalid runtime API timeout values", () => {
    vi.stubEnv("VITE_API_TIMEOUT_MS", "nope");

    expect(resolveApiConfig().timeoutMs).toBe(DEFAULT_API_CONFIG.timeoutMs);
  });
});
