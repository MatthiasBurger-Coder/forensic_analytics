import type {
  AnalysisJob,
  RepositoryAnalysis,
  StartRepositoryAnalysisCommand
} from "@/domain/repositoryAnalysis";

export interface RepositoryAnalysisPort {
  listRepositoryAnalyses(signal?: AbortSignal): Promise<RepositoryAnalysis[]>;
  getAnalysisJob(
    analysisRunId: string,
    signal?: AbortSignal
  ): Promise<AnalysisJob>;
  startRepositoryAnalysis(
    command: StartRepositoryAnalysisCommand,
    signal?: AbortSignal
  ): Promise<RepositoryAnalysis>;
}
