import { describe, expect, it, vi } from "vitest";

import { createStartRepositoryAnalysisUseCase } from "@/application/usecases/startRepositoryAnalysis";
import type { RepositoryAnalysisPort } from "@/application/ports/repositoryAnalysisPort";
import type {
  RepositoryAnalysis,
  StartRepositoryAnalysisCommand
} from "@/domain/repositoryAnalysis";
import { createStatusState } from "@/domain/analysisStatus";

describe("start repository analysis use case", () => {
  it("deduplicates concurrent submissions", async () => {
    const result = analysis();
    const port: RepositoryAnalysisPort = {
      listRepositoryAnalyses: vi.fn(),
      getAnalysisJob: vi.fn(),
      startRepositoryAnalysis: vi.fn().mockResolvedValue(result)
    };
    const useCase = createStartRepositoryAnalysisUseCase(port);

    const first = useCase(command());
    const second = useCase(command());

    await expect(Promise.all([first, second])).resolves.toEqual([
      result,
      result
    ]);
    expect(port.startRepositoryAnalysis).toHaveBeenCalledTimes(1);
  });
});

const command = (): StartRepositoryAnalysisCommand => ({
  requestId: "request-1",
  schemaVersion: "schema-v1",
  repositoryUrl: "https://example.invalid/project.git",
  provider: null,
  branch: "main",
  commit: null,
  buildContext: {
    buildTool: "gradle",
    buildId: "build-1",
    rootProjectName: null,
    declaredModules: [],
    attributes: {}
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

const analysis = (): RepositoryAnalysis => ({
  analysisRunId: "run-1",
  workspaceId: "workspace-1",
  repositoryUrl: "https://example.invalid/project.git",
  branch: "main",
  commit: null,
  resolvedCommit: null,
  checkoutStatus: "CHECKED_OUT",
  status: createStatusState("REGISTERED", "REGISTERED"),
  sourceRoots: [],
  diagnostics: [],
  createdAt: null,
  startedAt: null
});
