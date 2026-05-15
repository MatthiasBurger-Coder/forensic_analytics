import { describe, expect, it } from "vitest";

import {
  mapBackendStatus,
  mapRepositoryAnalysisDto,
  mapWorkspaceDto,
  mapWorkspaceListDto,
  mapWorkspaceViewsFromAnalyses
} from "@/adapters/api/mappers";

describe("API DTO mapping", () => {
  it.each([
    ["COMPLETED", "SUCCESS", true],
    ["FAILED", "FAILED", true],
    ["DEAD_LETTERED", "FAILED", true],
    ["CLEANED", "CLEANED", true],
    ["CANCELED", "CANCELED", true],
    ["REGISTERED", "REGISTERED", false],
    ["ACCEPTED", "ACCEPTED", false],
    ["DISPATCHABLE", "DISPATCHABLE", false],
    ["RUNNING", "RUNNING", false],
    ["RETRYABLE", "RETRYABLE", false]
  ])(
    "maps backend status %s into UI lifecycle %s",
    (backendStatus, lifecycle, terminal) => {
      expect(mapBackendStatus(backendStatus)).toEqual({
        backendStatus,
        lifecycle,
        terminal
      });
    }
  );

  it("keeps createdAt null unless the backend provides it", () => {
    const analysis = mapRepositoryAnalysisDto({
      analysisRunId: "run-1",
      workspaceId: "workspace-1",
      repositoryUrl: "https://example.invalid/project.git",
      status: "REGISTERED"
    });

    expect(analysis.createdAt).toBeNull();
    expect(analysis.status.backendStatus).toBe("REGISTERED");
    expect(analysis.status.lifecycle).toBe("REGISTERED");
  });

  it("sanitizes diagnostic text during mapping", () => {
    const analysis = mapRepositoryAnalysisDto({
      analysisRunId: "run-1",
      repositoryUrl: "https://example.invalid/project.git",
      status: "FAILED",
      diagnostics: [
        {
          severity: "ERROR",
          message:
            "token=secret-value\n    at example.Stack(C:\\Users\\private\\File.java:1)"
        }
      ]
    });

    expect(analysis.diagnostics[0].message).toContain("token=[redacted]");
    expect(analysis.diagnostics[0].message).toContain("[stack-frame-redacted]");
    expect(analysis.diagnostics[0].message).not.toContain("secret-value");
  });

  it("keeps REST string diagnostics visible", () => {
    const analysis = mapRepositoryAnalysisDto({
      analysisRunId: "run-1",
      repositoryUrl: "https://example.invalid/project.git",
      status: "REGISTERED",
      diagnostics: ["checkout completed"]
    });

    expect(analysis.diagnostics[0].message).toBe("checkout completed");
  });

  it("maps backend workspace views from REST", () => {
    const workspaces = mapWorkspaceListDto({
      items: [
        {
          workspaceId: "workspace-1",
          name: null,
          status: null,
          createdAt: null,
          updatedAt: null,
          repositoryAnalyses: [
            {
              analysisRunId: "run-1",
              workspaceId: "workspace-1",
              repositoryUrl: "https://example.invalid/project.git",
              branch: "main",
              status: "REGISTERED",
              diagnostics: ["checkout completed"]
            }
          ]
        }
      ]
    });

    expect(workspaces[0].workspaceId).toBe("workspace-1");
    expect(workspaces[0].status).toBeNull();
    expect(workspaces[0].repositoryAnalyses[0].analysisRunId).toBe("run-1");
    expect(workspaces[0].diagnostics[0].message).toBe("checkout completed");
    expect(mapWorkspaceDto({ workspaceId: "workspace-2" }).status).toBeNull();
  });

  it("derives workspace views only from repository analyses with workspace evidence", () => {
    const withWorkspace = mapRepositoryAnalysisDto({
      analysisRunId: "run-1",
      workspaceId: "workspace-1",
      repositoryUrl: "https://example.invalid/project.git",
      status: "REGISTERED"
    });
    const withoutWorkspace = mapRepositoryAnalysisDto({
      analysisRunId: "run-2",
      repositoryUrl: "https://example.invalid/other.git",
      status: "REGISTERED"
    });

    const workspaces = mapWorkspaceViewsFromAnalyses([
      withWorkspace,
      withoutWorkspace
    ]);

    expect(workspaces).toHaveLength(1);
    expect(workspaces[0].workspaceId).toBe("workspace-1");
    expect(workspaces[0].repositoryAnalyses).toHaveLength(1);
    expect(workspaces[0]).not.toHaveProperty("analysisRunIds");
  });
});
