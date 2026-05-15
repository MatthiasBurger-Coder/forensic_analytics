import type { DiagnosticsPort } from "@/application/ports/diagnosticsPort";
import type { RepositoryAnalysisPort } from "@/application/ports/repositoryAnalysisPort";
import type { WorkspacePort } from "@/application/ports/workspacePort";
import { createStartRepositoryAnalysisUseCase } from "@/application/usecases/startRepositoryAnalysis";

export interface ApplicationPorts {
  repositoryAnalysis: RepositoryAnalysisPort;
  workspaces: WorkspacePort;
  diagnostics: DiagnosticsPort;
}

export interface ApplicationServices {
  repositoryAnalysis: RepositoryAnalysisPort;
  workspaces: WorkspacePort;
  diagnostics: DiagnosticsPort;
}

export const createApplicationServices = (
  ports: ApplicationPorts
): ApplicationServices => ({
  repositoryAnalysis: {
    ...ports.repositoryAnalysis,
    startRepositoryAnalysis: createStartRepositoryAnalysisUseCase(
      ports.repositoryAnalysis
    )
  },
  workspaces: ports.workspaces,
  diagnostics: ports.diagnostics
});
