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

  it("does not call planned Gateway analysis list and diagnostics routes", async () => {
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
    await expect(client.diagnostics.collectDiagnostics()).resolves.toEqual([]);

    expect(fetcher).not.toHaveBeenCalled();
  });

  it("uses only public workspace REST routes for metadata, create, get and refresh", async () => {
    const fetcher = vi
      .fn()
      .mockResolvedValueOnce(
        jsonResponse({
          repositoryKey: "github.com/wildfly/wildfly",
          repositoryHost: "github.com",
          repositoryOwner: "wildfly",
          repositoryName: "wildfly",
          workspaceTitle: "wildfly",
          defaultBranch: "main",
          diagnostics: []
        })
      )
      .mockResolvedValueOnce(jsonResponse(workspaceResponse()))
      .mockResolvedValueOnce(jsonResponse(workspaceResponse()))
      .mockResolvedValueOnce(
        jsonResponse({
          workspaceBranchId: "workspace-branch-1",
          repositoryBranch: "main",
          status: "UP_TO_DATE",
          changed: false,
          resolvedCommit: "abc1234",
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

    await client.workspaces.previewMetadata({
      repositoryUrl: "https://github.com/wildfly/wildfly.git",
      correlationId: "correlation-metadata",
      idempotencyKey: "idem-metadata"
    });
    await client.workspaces.createWorkspace({
      repositoryUrl: "https://github.com/wildfly/wildfly.git",
      selectedBranch: "main",
      workspacePolicy: {
        ephemeral: false,
        allowShallowClone: true,
        allowPartialClone: false,
        allowSparseCheckout: false,
        timeoutSeconds: 60,
        maxWorkspaceBytes: 1073741824
      },
      correlationId: "correlation-create",
      idempotencyKey: "idem-create"
    });
    await client.workspaces.getWorkspace({
      workspaceId: "workspace-1",
      correlationId: "correlation-get"
    });
    await client.workspaces.refreshBranch({
      workspaceId: "workspace-1",
      workspaceBranchId: "workspace-branch-1",
      correlationId: "correlation-refresh",
      idempotencyKey: "idem-refresh"
    });

    expect(fetcher).toHaveBeenCalledTimes(4);
    expect(fetcher.mock.calls.map(([url]) => url)).toEqual([
      "https://backend.invalid/api/workspace-metadata",
      "https://backend.invalid/api/workspaces",
      "https://backend.invalid/api/workspaces/workspace-1",
      "https://backend.invalid/api/workspaces/workspace-1/branches/workspace-branch-1/refresh"
    ]);
    expect(fetcher.mock.calls.map(([url]) => url)).not.toContain(
      "https://github.com/wildfly/wildfly.git"
    );
    expect((fetcher.mock.calls[0][1] as RequestInit).headers).toMatchObject({
      "X-Correlation-Id": "correlation-metadata",
      "Idempotency-Key": "idem-metadata"
    });
    expect(JSON.parse(String((fetcher.mock.calls[1][1] as RequestInit).body))).toMatchObject({
      repositoryUrl: "https://github.com/wildfly/wildfly.git",
      selectedBranch: "main"
    });
    expect((fetcher.mock.calls[2][1] as RequestInit).headers).toMatchObject({
      "X-Correlation-Id": "correlation-get"
    });
    expect((fetcher.mock.calls[3][1] as RequestInit).headers).toMatchObject({
      "X-Correlation-Id": "correlation-refresh",
      "Idempotency-Key": "idem-refresh"
    });
    expect((fetcher.mock.calls[3][1] as RequestInit).body).toBeUndefined();
  });

  it("uses the public workspace list route with generated correlation metadata", async () => {
    const fetcher = vi.fn().mockResolvedValue(jsonResponse(workspaceListResponse()));
    const client = createApiClient({
      baseUrl: "https://backend.invalid/api",
      timeoutMs: 1000,
      maxGetAttempts: 1,
      baseRetryDelayMs: 1,
      fetcher
    });

    const workspaces = await client.workspaces.listWorkspaces();

    expect(fetcher).toHaveBeenCalledTimes(1);
    expect(fetcher.mock.calls[0][0]).toBe("https://backend.invalid/api/workspaces");
    expect((fetcher.mock.calls[0][1] as RequestInit).method).toBe("GET");
    expect((fetcher.mock.calls[0][1] as RequestInit).headers).toMatchObject({
      "X-Correlation-Id": expect.stringMatching(/^ui-workspace-list-/)
    });
    expect((fetcher.mock.calls[0][1] as RequestInit).headers).not.toMatchObject({
      "Idempotency-Key": expect.any(String)
    });
    expect((fetcher.mock.calls[0][1] as RequestInit).body).toBeUndefined();
    expect(workspaces[0].workspaceId).toBe("workspace-1");
    expect(workspaces[0].branches.map((branch) => branch.workspaceBranchId))
      .toEqual(["workspace-branch-1", "workspace-branch-2"]);
    expect(workspaces[0].branches.map((branch) => branch.repositoryBranch))
      .toEqual(["main", "release/1.0"]);
    expect(workspaces[0].repository.repositoryUrl).toBe("");
    expect(workspaces[0].repository.defaultBranch).toBeNull();
  });

  it("uses the public workspace delete route with mutation metadata", async () => {
    const fetcher = vi.fn().mockResolvedValue(jsonResponse(cleanupResponse()));
    const client = createApiClient({
      baseUrl: "https://backend.invalid/api",
      timeoutMs: 1000,
      maxGetAttempts: 1,
      baseRetryDelayMs: 1,
      fetcher
    });

    const cleanup = await client.workspaces.deleteWorkspace({
      workspaceId: "workspace-1",
      correlationId: "correlation-delete",
      idempotencyKey: "idem-delete"
    });

    expect(fetcher).toHaveBeenCalledTimes(1);
    expect(fetcher.mock.calls[0][0]).toBe(
      "https://backend.invalid/api/workspaces/workspace-1"
    );
    expect((fetcher.mock.calls[0][1] as RequestInit).method).toBe("DELETE");
    expect((fetcher.mock.calls[0][1] as RequestInit).headers).toMatchObject({
      "X-Correlation-Id": "correlation-delete",
      "Idempotency-Key": "idem-delete"
    });
    expect((fetcher.mock.calls[0][1] as RequestInit).body).toBeUndefined();
    expect(cleanup.workspaceId).toBe("workspace-1");
    expect(cleanup.status).toBe("CLEANED");
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
    [
      "metadata preview",
      (client: ReturnType<typeof createApiClient>) =>
        client.workspaces.previewMetadata({
          repositoryUrl: "https://github.com/wildfly/wildfly.git",
          correlationId: "correlation-metadata",
          idempotencyKey: "idem-metadata"
        })
    ],
    [
      "workspace create",
      (client: ReturnType<typeof createApiClient>) =>
        client.workspaces.createWorkspace({
          repositoryUrl: "https://github.com/wildfly/wildfly.git",
          selectedBranch: "main",
          workspacePolicy: {
            ephemeral: false,
            allowShallowClone: true,
            allowPartialClone: false,
            allowSparseCheckout: false,
            timeoutSeconds: 60,
            maxWorkspaceBytes: 1073741824
          },
          correlationId: "correlation-create",
          idempotencyKey: "idem-create"
        })
    ],
    [
      "branch refresh",
      (client: ReturnType<typeof createApiClient>) =>
        client.workspaces.refreshBranch({
          workspaceId: "workspace-1",
          workspaceBranchId: "workspace-branch-1",
          correlationId: "correlation-refresh",
          idempotencyKey: "idem-refresh"
        })
    ],
    [
      "workspace delete",
      (client: ReturnType<typeof createApiClient>) =>
        client.workspaces.deleteWorkspace({
          workspaceId: "workspace-1",
          correlationId: "correlation-delete",
          idempotencyKey: "idem-delete"
        })
    ]
  ])("does not retry workspace mutation %s", async (_name, action) => {
    const fetcher = vi.fn().mockResolvedValue(
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

    await expect(action(client)).rejects.toBeInstanceOf(ApplicationError);

    expect(fetcher).toHaveBeenCalledTimes(1);
  });

  it("maps public idempotency conflicts from workspace routes", async () => {
    const fetcher = vi.fn().mockImplementation(() =>
      Promise.resolve(jsonResponse(
        {
          code: "IDEMPOTENCY_CONFLICT",
          message: "The idempotency key was already used with different input.",
          retryable: false,
          correlationId: "correlation-1",
          diagnostics: []
        },
        409
      ))
    );
    const client = createApiClient({
      baseUrl: "https://backend.invalid/api",
      timeoutMs: 1000,
      maxGetAttempts: 1,
      baseRetryDelayMs: 1,
      fetcher
    });

    await expect(
      client.workspaces.previewMetadata({
        repositoryUrl: "https://github.com/wildfly/wildfly.git",
        correlationId: "correlation-1",
        idempotencyKey: "idem-1"
      })
    ).rejects.toMatchObject({
      category: "IDEMPOTENCY_CONFLICT",
      correlationId: "correlation-1"
    });
    await expect(
      client.workspaces.deleteWorkspace({
        workspaceId: "workspace-1",
        correlationId: "correlation-1",
        idempotencyKey: "idem-1"
      })
    ).rejects.toMatchObject({
      category: "IDEMPOTENCY_CONFLICT",
      correlationId: "correlation-1"
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

  it.each([
    ["workspaceId", { workspaceId: " " }],
    ["correlationId", { correlationId: " " }],
    ["idempotencyKey", { idempotencyKey: " " }]
  ])("rejects missing Gateway %s metadata before workspace delete", async (_field, patch) => {
    const fetcher = vi.fn();
    const client = createApiClient({
      baseUrl: "https://backend.invalid/api",
      timeoutMs: 1000,
      maxGetAttempts: 1,
      baseRetryDelayMs: 1,
      fetcher
    });

    await expect(
      client.workspaces.deleteWorkspace({
        workspaceId: "workspace-1",
        correlationId: "correlation-delete",
        idempotencyKey: "idem-delete",
        ...patch
      })
    ).rejects.toMatchObject({
      category: "VALIDATION_ERROR"
    });

    expect(fetcher).not.toHaveBeenCalled();
  });

  it.each([
    "workspace-1/branch",
    "workspace-1\\branch",
    "../workspace-1",
    "https://backend.invalid/workspace-1",
    "token-workspace-1",
    "project-1"
  ])("rejects unsafe workspace delete id %s before fetch", async (workspaceId) => {
    const fetcher = vi.fn();
    const client = createApiClient({
      baseUrl: "https://backend.invalid/api",
      timeoutMs: 1000,
      maxGetAttempts: 1,
      baseRetryDelayMs: 1,
      fetcher
    });

    await expect(
      client.workspaces.deleteWorkspace({
        workspaceId,
        correlationId: "correlation-delete",
        idempotencyKey: "idem-delete"
      })
    ).rejects.toMatchObject({
      category: "VALIDATION_ERROR"
    });

    expect(fetcher).not.toHaveBeenCalled();
  });

  it("sanitizes workspace list and delete backend diagnostics", async () => {
    const fetcher = vi
      .fn()
      .mockResolvedValueOnce(
        jsonResponse({
          code: "BACKEND_UNAVAILABLE",
          message:
            "raw stdout token=secret https://github.com/wildfly/wildfly.git C:\\Users\\private\\repo",
          retryable: false,
          correlationId: "correlation-list",
          diagnostics: [
            {
              severity: "ERROR",
              message:
                "raw stderr password=secret https://github.com/acme/private.git /var/lib/forensic-analytics/repository-workspaces/workspace-1",
              source: "C:\\Users\\private\\File.ts"
            }
          ]
        }, 503)
      )
      .mockResolvedValueOnce(
        jsonResponse({
          code: "BACKEND_UNAVAILABLE",
          message:
            "raw stdout token=secret https://github.com/wildfly/wildfly.git C:\\Users\\private\\repo",
          retryable: false,
          correlationId: "correlation-delete",
          diagnostics: [
            {
              severity: "ERROR",
              message:
                "raw stderr password=secret https://github.com/acme/private.git /var/lib/forensic-analytics/repository-workspaces/workspace-1",
              source: "C:\\Users\\private\\File.ts"
            }
          ]
        }, 503)
      );
    const client = createApiClient({
      baseUrl: "https://backend.invalid/api",
      timeoutMs: 1000,
      maxGetAttempts: 1,
      baseRetryDelayMs: 1,
      fetcher
    });

    await expect(client.workspaces.listWorkspaces()).rejects.toMatchObject({
      message: expect.stringContaining("[url-redacted]"),
      diagnostics: [
        expect.objectContaining({
          message: expect.stringContaining("[url-redacted]"),
          source: "[local-path]"
        })
      ]
    });
    await expect(
      client.workspaces.deleteWorkspace({
        workspaceId: "workspace-1",
        correlationId: "correlation-delete",
        idempotencyKey: "idem-delete"
      })
    ).rejects.toMatchObject({
      message: expect.stringContaining("[url-redacted]"),
      diagnostics: [
        expect.objectContaining({
          message: expect.stringContaining("[url-redacted]"),
          source: "[local-path]"
        })
      ]
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

const workspaceListResponse = () => ({
  items: [
    {
      workspaceId: "workspace-1",
      workspaceTitle: "wildfly",
      repository: {
        repositoryKey: "github.com/wildfly/wildfly",
        repositoryHost: "github.com",
        repositoryOwner: "wildfly",
        repositoryName: "wildfly"
      },
      status: "READY",
      branches: [
        {
          workspaceBranchId: "workspace-branch-1",
          repositoryBranch: "main",
          status: "CHECKED_OUT",
          resolvedCommit: "abc1234",
          sourceSnapshotId: "source-snapshot-1",
          sourceRoots: ["src/main/java"],
          diagnostics: []
        },
        {
          workspaceBranchId: "workspace-branch-2",
          repositoryBranch: "release/1.0",
          status: "UP_TO_DATE",
          resolvedCommit: "def5678",
          sourceSnapshotId: "source-snapshot-2",
          sourceRoots: [],
          diagnostics: []
        }
      ],
      diagnostics: []
    }
  ],
  diagnostics: []
});

const cleanupResponse = () => ({
  workspaceId: "workspace-1",
  status: "CLEANED",
  diagnostics: []
});

const workspaceResponse = () => ({
  workspaceId: "workspace-1",
  workspaceTitle: "wildfly",
  repository: {
    repositoryKey: "github.com/wildfly/wildfly",
    repositoryUrl: "https://github.com/wildfly/wildfly.git",
    repositoryHost: "github.com",
    repositoryOwner: "wildfly",
    repositoryName: "wildfly",
    defaultBranch: "main"
  },
  status: "READY",
  branches: [
    {
      workspaceBranchId: "workspace-branch-1",
      repositoryBranch: "main",
      status: "CHECKED_OUT",
      resolvedCommit: "abc1234",
      sourceSnapshotId: "source-snapshot-1",
      sourceRoots: ["src/main/java"],
      diagnostics: []
    }
  ],
  diagnostics: []
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
