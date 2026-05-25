import { describe, expect, it } from "vitest";

import {
  mapBranchRefreshDto,
  mapBackendStatus,
  mapRepositoryAnalysisDto,
  mapWorkspaceCleanupDto,
  mapWorkspaceDto,
  mapWorkspaceListDto,
  mapWorkspaceMetadataDto
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

  it("maps Gateway repository-to-BTM status metadata", () => {
    const analysis = mapRepositoryAnalysisDto({
      analysisRunId: "run-1",
      status: "ACCEPTED",
      sourceSnapshotStatus: "AVAILABLE",
      workflow: "repository-to-btm",
      statusUrl: "/repository-analyses/run-1",
      jobsUrl: "/repository-analyses/run-1/jobs",
      btmDeliveryStatus: "BTM_DELIVERY_NOT_READY",
      btmDeliveryService: "BtmArtifactDeliveryService",
      correlationId: "correlation-1"
    });

    expect(analysis.sourceSnapshotStatus).toBe("AVAILABLE");
    expect(analysis.workflow).toBe("repository-to-btm");
    expect(analysis.statusUrl).toBe("/repository-analyses/run-1");
    expect(analysis.jobsUrl).toBe("/repository-analyses/run-1/jobs");
    expect(analysis.btmDeliveryStatus).toBe("BTM_DELIVERY_NOT_READY");
    expect(analysis.btmDeliveryService).toBe("BtmArtifactDeliveryService");
    expect(analysis.correlationId).toBe("correlation-1");
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

  it("maps repository workspace metadata from REST without deriving it locally", () => {
    const metadata = mapWorkspaceMetadataDto({
      repositoryKey: "github.com/wildfly/wildfly",
      repositoryHost: "github.com",
      repositoryOwner: "wildfly",
      repositoryName: "wildfly",
      workspaceTitle: "wildfly",
      defaultBranch: "main",
      diagnostics: []
    });

    expect(metadata).toMatchObject({
      repositoryKey: "github.com/wildfly/wildfly",
      repositoryHost: "github.com",
      repositoryOwner: "wildfly",
      repositoryName: "wildfly",
      workspaceTitle: "wildfly",
      defaultBranch: "main"
    });
  });

  it("maps repository checkout workspaces and sanitizes branch diagnostics", () => {
    const workspaces = mapWorkspaceListDto({
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
              sourceRoots: ["src/main/java", "/var/lib/forensic-analytics/repository-workspaces/workspace-1"],
              diagnostics: [
                {
                  severity: "ERROR",
                  message:
                    "raw stdout token=secret https://github.com/wildfly/wildfly.git jdbc:h2:file:/var/lib/forensic-analytics/repository-source-data/repository-source",
                  source: "C:\\Users\\private\\File.java"
                }
              ]
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
        },
        {
          workspaceId: "workspace-2",
          workspaceTitle: "empty",
          repository: {
            repositoryKey: "github.com/acme/empty",
            repositoryHost: "github.com",
            repositoryOwner: "acme",
            repositoryName: "empty",
            repositoryUrl: "https://github.com/acme/empty.git",
            defaultBranch: "main"
          },
          status: "READY",
          branches: [],
          diagnostics: []
        }
      ]
    });

    expect(workspaces[0].workspaceId).toBe("workspace-1");
    expect(workspaces[0].workspaceTitle).toBe("wildfly");
    expect(workspaces[0].status).toBe("READY");
    expect(workspaces[0].repository.repositoryKey).toBe("github.com/wildfly/wildfly");
    expect(workspaces[0].repository.repositoryUrl).toBe("");
    expect(workspaces[0].repository.defaultBranch).toBeNull();
    expect(workspaces[0].branches.map((branch) => branch.workspaceBranchId))
      .toEqual(["workspace-branch-1", "workspace-branch-2"]);
    expect(workspaces[0].branches.map((branch) => branch.repositoryBranch))
      .toEqual(["main", "release/1.0"]);
    expect(workspaces[0].branches[0].status).toBe("CHECKED_OUT");
    expect(workspaces[0].branches[0].sourceRoots[1]).toContain("[local-path]");
    expect(workspaces[0].branches[0].diagnostics[0].message).toContain(
      "[stream-redacted]"
    );
    expect(workspaces[0].branches[0].diagnostics[0].message).toContain(
      "[url-redacted]"
    );
    expect(workspaces[0].branches[0].diagnostics[0].message).not.toContain(
      "secret"
    );
    expect(workspaces[0].branches[0].diagnostics[0].message).not.toContain(
      "https://github.com/wildfly/wildfly.git"
    );
    expect(workspaces[0].branches[0].diagnostics[0].source).toBe("[local-path]");
    expect(workspaces[1].branches).toEqual([]);
    expect(workspaces[1].repository.repositoryUrl).toBe("");
    expect(workspaces[1].repository.defaultBranch).toBeNull();
    expect(mapWorkspaceDto({ workspaceId: "workspace-2" }).status).toBe(
      "UNKNOWN"
    );
  });

  it("maps repository checkout cleanup without confirming unknown states", () => {
    const cleanup = mapWorkspaceCleanupDto({
      workspaceId: "workspace-1",
      status: "CLEANED",
      diagnostics: [
        {
          severity: "INFO",
          message: "cleaned raw stdout token=secret https://github.com/acme/private.git",
          source: "/var/lib/forensic-analytics/repository-workspaces/workspace-1"
        }
      ]
    });
    const unknown = mapWorkspaceCleanupDto({
      workspaceId: "workspace-2",
      status: "READY",
      diagnostics: []
    });

    expect(cleanup.workspaceId).toBe("workspace-1");
    expect(cleanup.status).toBe("CLEANED");
    expect(cleanup.diagnostics[0].message).toContain("[url-redacted]");
    expect(cleanup.diagnostics[0].message).not.toContain("secret");
    expect(cleanup.diagnostics[0].source).toBe("[local-path]");
    expect(unknown.status).toBe("UNKNOWN");
    expect(mapWorkspaceCleanupDto({ workspaceId: "workspace-3" }).status).toBe(
      "UNKNOWN"
    );
  });

  it("maps repository checkout branch refresh results", () => {
    const refresh = mapBranchRefreshDto({
      workspaceBranchId: "workspace-branch-1",
      repositoryBranch: "main",
      status: "UP_TO_DATE",
      changed: false,
      previousCommit: "abc1234",
      resolvedCommit: "abc1234",
      sourceSnapshotId: "source-snapshot-1",
      diagnostics: []
    });

    expect(refresh.changed).toBe(false);
    expect(refresh.status).toBe("UP_TO_DATE");
    expect(refresh.resolvedCommit).toBe("abc1234");
  });

  it("maps changed repository checkout branch refresh results", () => {
    const refresh = mapBranchRefreshDto({
      workspaceBranchId: "workspace-branch-1",
      repositoryBranch: "main",
      status: "UPDATED",
      changed: true,
      previousCommit: "abc1234",
      resolvedCommit: "def4567",
      sourceSnapshotId: "source-snapshot-2",
      diagnostics: []
    });

    expect(refresh.changed).toBe(true);
    expect(refresh.status).toBe("UPDATED");
    expect(refresh.previousCommit).toBe("abc1234");
    expect(refresh.resolvedCommit).toBe("def4567");
    expect(refresh.sourceSnapshotId).toBe("source-snapshot-2");
  });
});
