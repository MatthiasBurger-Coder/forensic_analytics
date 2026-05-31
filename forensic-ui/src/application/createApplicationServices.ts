import type { DiagnosticsPort } from "@/application/ports/diagnosticsPort";
import type { RepositoryAnalysisPort } from "@/application/ports/repositoryAnalysisPort";
import type { SettingsPort } from "@/application/ports/settingsPort";
import type { WorkspacePort } from "@/application/ports/workspacePort";
import { createStartRepositoryAnalysisUseCase } from "@/application/usecases/startRepositoryAnalysis";

export interface ApplicationPorts {
  repositoryAnalysis: RepositoryAnalysisPort;
  workspaces: WorkspacePort;
  diagnostics: DiagnosticsPort;
  settings: SettingsPort;
}

export interface ApplicationServices {
  repositoryAnalysis: RepositoryAnalysisPort;
  workspaces: WorkspacePort;
  diagnostics: DiagnosticsPort;
  settings?: SettingsPort;
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
  diagnostics: ports.diagnostics,
  settings: ports.settings
});
