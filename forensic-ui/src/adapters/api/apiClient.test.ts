import { describe, expect, it, vi } from "vitest";

import { ApplicationError } from "@/application/errors";
import { createApiClient } from "@/adapters/api/apiClient";
import type { StartRepositoryAnalysisCommand } from "@/domain/repositoryAnalysis";

describe("API client resilience", () => {
  it("retries idempotent Gateway status reads with a bounded budget", async () => {
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
        jsonResponse(
          {
            analysisRunId: "run-1",
            status: "REGISTERED"
          }
        )
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

    const analysis = await client.repositoryAnalysis.getAnalysisJob("run-1");

    expect(fetcher).toHaveBeenCalledTimes(2);
    expect(analysis.status.backendStatus).toBe("REGISTERED");
  });

  it("does not call planned Gateway list and workspace routes", async () => {
    const fetcher = vi.fn();
    const client = createApiClient({
      baseUrl: "https://backend.invalid/api",
      timeoutMs: 1000,
      maxGetAttempts: 1,
      baseRetryDelayMs: 1,
      fetcher
    });

    await expect(
      client.repositoryAnalysis.listRepositoryAnalyses()
    ).resolves.toEqual([]);
    await expect(client.workspaces.listWorkspaces()).resolves.toEqual([]);
    await expect(client.diagnostics.collectDiagnostics()).resolves.toEqual([]);
    await expect(client.workspaces.getWorkspace("workspace-1")).rejects.toMatchObject({
      category: "VALIDATION_ERROR"
    });

    expect(fetcher).not.toHaveBeenCalled();
  });

  it("sends required Gateway correlation metadata for status reads", async () => {
    const fetcher = vi.fn().mockResolvedValue(
      jsonResponse({
        analysisRunId: "run-1",
        status: "ACCEPTED",
        diagnostics: []
      })
    );
    const client = createApiClient({
      baseUrl: "https://backend.invalid/api",
      timeoutMs: 1000,
      maxGetAttempts: 1,
      baseRetryDelayMs: 1,
      fetcher
    });

    await client.repositoryAnalysis.getAnalysisJob("run-1");

    expect((fetcher.mock.calls[0][1] as RequestInit).headers).toMatchObject({
      "X-Correlation-Id": expect.stringMatching(/^ui-status-/)
    });
  });

  it("keeps submitted repository context when Gateway accepts a BTM request", async () => {
    const fetcher = vi.fn().mockResolvedValue(
      jsonResponse(
        {
          analysisRunId: "run-1",
          status: "ACCEPTED",
          btmDeliveryStatus: "BTM_DELIVERY_NOT_READY",
          correlationId: "correlation-1",
          diagnostics: []
        },
        202
      )
    );
    const client = createApiClient({
      baseUrl: "https://backend.invalid/api",
      timeoutMs: 1000,
      maxGetAttempts: 1,
      baseRetryDelayMs: 1,
      fetcher
    });

    const analysis = await client.repositoryAnalysis.startRepositoryAnalysis(
      command()
    );

    expect(analysis.repositoryUrl).toBe("https://example.invalid/project.git");
    expect(analysis.branch).toBe("main");
    expect(analysis.btmDeliveryStatus).toBe("BTM_DELIVERY_NOT_READY");
    expect(analysis.correlationId).toBe("correlation-1");
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

    const request = client.repositoryAnalysis.getAnalysisJob("run-1");
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
    expect((init as RequestInit).headers).toMatchObject({
      "X-Correlation-Id": "correlation-1",
      "Idempotency-Key": "idem-1"
    });
    expect(JSON.parse(String((init as RequestInit).body))).toEqual({
      requestId: "request-1",
      schemaVersion: "schema-v1",
      requestedOutputs: ["BTM_RULES"],
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
        maxWorkspaceBytes: 100000
      }
    });
  });

  it.each([
    ["correlationId", { correlationId: " " }],
    ["idempotencyKey", { idempotencyKey: " " }]
  ])("rejects missing Gateway %s metadata before POST", async (_field, patch) => {
    const fetcher = vi.fn();
    const client = createApiClient({
      baseUrl: "https://backend.invalid/api",
      timeoutMs: 1000,
      maxGetAttempts: 1,
      baseRetryDelayMs: 1,
      fetcher
    });

    await expect(
      client.repositoryAnalysis.startRepositoryAnalysis({
        ...command(),
        ...patch
      })
    ).rejects.toMatchObject({
      category: "VALIDATION_ERROR"
    });

    expect(fetcher).not.toHaveBeenCalled();
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
  correlationId: "correlation-1",
  idempotencyKey: "idem-1",
  schemaVersion: "schema-v1",
  requestedOutputs: ["BTM_RULES"],
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
    maxWorkspaceBytes: 100000
  }
});
