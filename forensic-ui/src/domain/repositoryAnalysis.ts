import type { AnalysisStatusState } from "@/domain/analysisStatus";
import type { DiagnosticMessage } from "@/domain/diagnostic";

export interface RepositoryAnalysisSummary {
  analysisRunId: string;
  workspaceId: string | null;
  repositoryUrl: string;
  branch: string | null;
  commit: string | null;
  resolvedCommit: string | null;
  checkoutStatus: string | null;
  status: AnalysisStatusState;
  createdAt: string | null;
  startedAt: string | null;
  diagnostics: DiagnosticMessage[];
}

export interface RepositoryAnalysis extends RepositoryAnalysisSummary {
  sourceRoots: string[];
}

export interface AnalysisJob extends RepositoryAnalysis {
  lastUpdatedAt: string | null;
}

export interface BuildContext {
  buildTool: string;
  buildId: string;
  rootProjectName: string | null;
  declaredModules: string[];
  attributes: Record<string, string>;
}

export interface WorkspacePolicy {
  ephemeral: boolean;
  allowShallowClone: boolean;
  allowPartialClone: boolean;
  allowSparseCheckout: boolean;
  timeoutSeconds: number;
  maxWorkspaceBytes: number;
}

export interface StartRepositoryAnalysisCommand {
  requestId: string;
  schemaVersion: string;
  repositoryUrl: string;
  provider: string | null;
  branch: string | null;
  commit: string | null;
  buildContext: BuildContext;
  workspacePolicy: WorkspacePolicy;
}

export const REPOSITORY_ANALYSIS_SCHEMA_VERSION = "schema-v1";
