import type { DiagnosticMessage } from "@/domain/diagnostic";
import type { RepositoryAnalysisSummary } from "@/domain/repositoryAnalysis";

export interface Workspace {
  workspaceId: string;
  name: string | null;
  status: string | null;
  repositoryUrl: string | null;
  branch: string | null;
  createdAt: string | null;
  updatedAt: string | null;
  diagnostics: DiagnosticMessage[];
  repositoryAnalyses: RepositoryAnalysisSummary[];
}
