import type { RepositoryAnalysisPort } from "@/application/ports/repositoryAnalysisPort";
import type {
  RepositoryAnalysis,
  StartRepositoryAnalysisCommand
} from "@/domain/repositoryAnalysis";

export type StartRepositoryAnalysisUseCase = (
  command: StartRepositoryAnalysisCommand,
  signal?: AbortSignal
) => Promise<RepositoryAnalysis>;

export const createStartRepositoryAnalysisUseCase = (
  port: RepositoryAnalysisPort
): StartRepositoryAnalysisUseCase => {
  let inFlight: Promise<RepositoryAnalysis> | null = null;

  return (command, signal) => {
    if (inFlight) {
      return inFlight;
    }

    inFlight = port
      .startRepositoryAnalysis(command, signal)
      .finally(() => {
        inFlight = null;
      });

    return inFlight;
  };
};
