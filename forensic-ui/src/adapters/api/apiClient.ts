import type { ApplicationPorts } from "@/application/createApplicationServices";
import { ApplicationError } from "@/application/errors";
import type { RepositoryAnalysisPort } from "@/application/ports/repositoryAnalysisPort";
import type { WorkspacePort } from "@/application/ports/workspacePort";
import type { DiagnosticsPort } from "@/application/ports/diagnosticsPort";
import type {
  RepositoryAnalysis,
  StartRepositoryAnalysisCommand
} from "@/domain/repositoryAnalysis";

import { resolveApiConfig, type ApiConfig } from "./config";
import type { RepositoryAnalysisDto, WorkspaceDto } from "./dtos";
import { HttpClient, type Delay, type Fetcher } from "./httpClient";
import {
  mapAnalysisJobDto,
  mapRepositoryAnalysisDto,
  mapRepositoryAnalysisListDto,
  mapWorkspaceDto,
  mapWorkspaceListDto
} from "./mappers";

export interface ApiClientOptions extends Partial<ApiConfig> {
  fetcher?: Fetcher;
  delay?: Delay;
  random?: () => number;
}

export const createApiClient = (
  options: ApiClientOptions = {}
): ApplicationPorts => {
  const config = resolveApiConfig(options);
  const http = new HttpClient({
    ...config,
    fetcher: options.fetcher,
    delay: options.delay,
    random: options.random
  });

  const repositoryAnalysis: RepositoryAnalysisPort = {
    async listRepositoryAnalyses(signal) {
      const response = await http.requestJson<unknown>("/repository-analyses", {
        signal
      });

      return mapRepositoryAnalysisListDto(response);
    },
    async getAnalysisJob(analysisRunId, signal) {
      const response = await http.requestJson<RepositoryAnalysisDto>(
        `/repository-analyses/${encodeURIComponent(analysisRunId)}`,
        { signal }
      );

      return mapAnalysisJobDto(response);
    },
    async startRepositoryAnalysis(command, signal) {
      validateStartCommand(command);
      const response = await http.requestJson<RepositoryAnalysisDto>(
        "/repository-analyses",
        {
          method: "POST",
          body: toStartRequest(command),
          signal
        }
      );

      return mapRepositoryAnalysisDto(response);
    }
  };

  const workspaces: WorkspacePort = {
    async listWorkspaces(signal) {
      const response = await http.requestJson<unknown>("/workspaces", {
        signal
      });

      return mapWorkspaceListDto(response);
    },
    async getWorkspace(workspaceId, signal) {
      const response = await http.requestJson<WorkspaceDto>(
        `/workspaces/${encodeURIComponent(workspaceId)}`,
        { signal }
      );

      return mapWorkspaceDto(response);
    }
  };

  const diagnostics: DiagnosticsPort = {
    async collectDiagnostics(signal) {
      const analyses = await repositoryAnalysis.listRepositoryAnalyses(signal);
      return analyses.flatMap((analysis) => analysis.diagnostics);
    }
  };

  return {
    repositoryAnalysis,
    workspaces,
    diagnostics
  };
};

const toStartRequest = (command: StartRepositoryAnalysisCommand) => ({
  requestId: command.requestId,
  schemaVersion: command.schemaVersion,
  repositoryUrl: command.repositoryUrl,
  provider: command.provider,
  branch: command.branch,
  commit: command.commit,
  buildContext: {
    buildTool: command.buildContext.buildTool,
    buildId: command.buildContext.buildId,
    rootProjectName: command.buildContext.rootProjectName,
    declaredModules: command.buildContext.declaredModules,
    attributes: command.buildContext.attributes
  },
  workspacePolicy: {
    ephemeral: command.workspacePolicy.ephemeral,
    allowShallowClone: command.workspacePolicy.allowShallowClone,
    allowPartialClone: command.workspacePolicy.allowPartialClone,
    allowSparseCheckout: command.workspacePolicy.allowSparseCheckout,
    timeoutSeconds: command.workspacePolicy.timeoutSeconds,
    maxWorkspaceBytes: command.workspacePolicy.maxWorkspaceBytes
  }
});

const validateStartCommand = (command: StartRepositoryAnalysisCommand): void => {
  const missingText = [
    ["repositoryUrl", command.repositoryUrl],
    ["requestId", command.requestId],
    ["schemaVersion", command.schemaVersion],
    ["buildContext.buildTool", command.buildContext.buildTool],
    ["buildContext.buildId", command.buildContext.buildId]
  ].find(([, value]) => typeof value !== "string" || !value.trim());

  if (missingText) {
    throw new ApplicationError(
      "VALIDATION_ERROR",
      `${missingText[0]} is required before registering a repository analysis session.`
    );
  }

  if (!command.branch?.trim() && !command.commit?.trim()) {
    throw new ApplicationError(
      "VALIDATION_ERROR",
      "Branch or commit is required before registering a repository analysis session."
    );
  }

  if (
    command.workspacePolicy.timeoutSeconds < 0 ||
    command.workspacePolicy.maxWorkspaceBytes < 0
  ) {
    throw new ApplicationError(
      "VALIDATION_ERROR",
      "Workspace timeout and byte limits must not be negative."
    );
  }
};

export const collectAnalysisDiagnostics = (
  analyses: RepositoryAnalysis[]
) => analyses.flatMap((analysis) => analysis.diagnostics);
