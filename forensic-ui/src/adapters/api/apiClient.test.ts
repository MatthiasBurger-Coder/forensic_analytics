import { describe, expect, it, vi } from "vitest";

import { ApplicationError } from "@/application/errors";
import { createApiClient } from "@/adapters/api/apiClient";
import type { StartRepositoryAnalysisCommand } from "@/domain/repositoryAnalysis";

describe("API client resilience", () => {
  it("retries idempotent GET requests with a bounded budget", async () => {
    const fetcher = vi
      .fn()
      .mockResolvedValueOnce(
        jsonResponse(
          {
            code: "BACKEND_UNAVAILABLE",
            message: "Backend unavailable",
            retryable: true
          },
          503
        )
      )
      .mockResolvedValueOnce(
        jsonResponse([
          {
            analysisRunId: "run-1",
            workspaceId: "workspace-1",
            repositoryUrl: "https://example.invalid/project.git",
            status: "REGISTERED"
          }
        ])
      );

    const client = createApiClient({
      baseUrl: "https://backend.invalid/api",
      timeoutMs: 1000,
      maxGetAttempts: 3,
      baseRetryDelayMs: 1,
      delay: async () => undefined,
      random: () => 0,
      fetcher
    });

    const analyses =
      await client.repositoryAnalysis.listRepositoryAnalyses();

    expect(fetcher).toHaveBeenCalledTimes(2);
    expect(analyses[0].status.backendStatus).toBe("REGISTERED");
  });

  it("times out a backend request", async () => {
    vi.useFakeTimers();
    const fetcher = vi.fn((_url, init) => {
      const signal = (init as RequestInit).signal;
      return new Promise<Response>((_resolve, reject) => {
        signal?.addEventListener("abort", () => {
          reject(new DOMException("aborted", "AbortError"));
        });
      });
    });

    const client = createApiClient({
      baseUrl: "https://backend.invalid/api",
      timeoutMs: 5,
      maxGetAttempts: 1,
      baseRetryDelayMs: 1,
      fetcher
    });

    const request = client.repositoryAnalysis.listRepositoryAnalyses();
    const assertion = expect(request).rejects.toMatchObject({
      category: "TIMEOUT"
    });

    await vi.advanceTimersByTimeAsync(6);
    await assertion;
    vi.useRealTimers();
  });

  it("does not retry repository-analysis POST and sends the full command contract", async () => {
    const fetcher = vi
      .fn()
      .mockResolvedValue(
        jsonResponse(
          {
            code: "BACKEND_UNAVAILABLE",
            message: "Backend unavailable",
            retryable: true
          },
          503
        )
      );
    const client = createApiClient({
      baseUrl: "https://backend.invalid/api",
      timeoutMs: 1000,
      maxGetAttempts: 3,
      baseRetryDelayMs: 1,
      delay: async () => undefined,
      fetcher
    });

    await expect(
      client.repositoryAnalysis.startRepositoryAnalysis(command())
    ).rejects.toBeInstanceOf(ApplicationError);

    expect(fetcher).toHaveBeenCalledTimes(1);
    const [, init] = fetcher.mock.calls[0];
    expect((init as RequestInit).method).toBe("POST");
    expect(JSON.parse(String((init as RequestInit).body))).toEqual({
      requestId: "request-1",
      schemaVersion: "schema-v1",
      repositoryUrl: "https://example.invalid/project.git",
      provider: "git",
      branch: "main",
      commit: null,
      buildContext: {
        buildTool: "gradle",
        buildId: "build-1",
        rootProjectName: null,
        declaredModules: [":app"],
        attributes: { origin: "ui" }
      },
      workspacePolicy: {
        ephemeral: false,
        allowShallowClone: false,
        allowPartialClone: false,
        allowSparseCheckout: false,
        timeoutSeconds: 60,
        maxWorkspaceBytes: 0
      }
    });
  });
});

const jsonResponse = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json"
    }
  });

const command = (): StartRepositoryAnalysisCommand => ({
  requestId: "request-1",
  schemaVersion: "schema-v1",
  repositoryUrl: "https://example.invalid/project.git",
  provider: "git",
  branch: "main",
  commit: null,
  buildContext: {
    buildTool: "gradle",
    buildId: "build-1",
    rootProjectName: null,
    declaredModules: [":app"],
    attributes: { origin: "ui" }
  },
  workspacePolicy: {
    ephemeral: false,
    allowShallowClone: false,
    allowPartialClone: false,
    allowSparseCheckout: false,
    timeoutSeconds: 60,
    maxWorkspaceBytes: 0
  }
});
