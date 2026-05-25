import type { ApplicationPorts } from "@/application/createApplicationServices";
import { ApplicationError } from "@/application/errors";
import type { RepositoryAnalysisPort } from "@/application/ports/repositoryAnalysisPort";
import type { WorkspacePort } from "@/application/ports/workspacePort";
import type { DiagnosticsPort } from "@/application/ports/diagnosticsPort";
import type {
  RepositoryAnalysis,
  StartRepositoryAnalysisCommand
} from "@/domain/repositoryAnalysis";
import type {
  CreateWorkspaceCommand,
  DeleteWorkspaceCommand,
  GetWorkspaceCommand,
  PreviewWorkspaceMetadataCommand,
  RefreshWorkspaceBranchCommand
} from "@/domain/workspace";

import { resolveApiConfig, type ApiConfig } from "./config";
import type {
  BranchRefreshResponseDto,
  RepositoryAnalysisDto,
  WorkspaceCleanupResponseDto,
  WorkspaceDto,
  WorkspaceListDto,
  WorkspaceMetadataResponseDto
} from "./dtos";
import { HttpClient, type Delay, type Fetcher } from "./httpClient";
import {
  mapAnalysisJobDto,
  mapBranchRefreshDto,
  mapRepositoryAnalysisDto,
  mapRepositoryAnalysisListDto,
  mapWorkspaceCleanupDto,
  mapWorkspaceDto,
  mapWorkspaceListDto,
  mapWorkspaceMetadataDto
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
      void signal;

      return mapRepositoryAnalysisListDto([]);
    },
    async getAnalysisJob(analysisRunId, signal) {
      const response = await http.requestJson<RepositoryAnalysisDto>(
        `/repository-analyses/${encodeURIComponent(analysisRunId)}`,
        {
          headers: {
            "X-Correlation-Id": createGatewayMetadata("status").correlationId
          },
          signal
        }
      );

      return mapAnalysisJobDto(response);
    },
    async startRepositoryAnalysis(command, signal) {
      validateStartCommand(command);
      const response = await http.requestJson<RepositoryAnalysisDto>(
        "/repository-analyses",
        {
          method: "POST",
          headers: {
            "X-Correlation-Id": command.correlationId,
            "Idempotency-Key": command.idempotencyKey
          },
          body: toStartRequest(command),
          signal
        }
      );

      return withSubmittedCommand(mapRepositoryAnalysisDto(response), command);
    }
  };

  const workspaces: WorkspacePort = {
    async previewMetadata(command, signal) {
      validateWorkspaceMetadataCommand(command);
      const response = await http.requestJson<WorkspaceMetadataResponseDto>(
        "/workspace-metadata",
        {
          method: "POST",
          headers: mutationHeaders(command),
          body: {
            repositoryUrl: command.repositoryUrl
          },
          signal
        }
      );

      return mapWorkspaceMetadataDto(response);
    },
    async createWorkspace(command, signal) {
      validateCreateWorkspaceCommand(command);
      const response = await http.requestJson<WorkspaceDto>("/workspaces", {
        method: "POST",
        headers: mutationHeaders(command),
        body: {
          repositoryUrl: command.repositoryUrl,
          selectedBranch: command.selectedBranch,
          workspacePolicy: command.workspacePolicy
        },
        signal
      });

      return mapWorkspaceDto(response);
    },
    async refreshBranch(command, signal) {
      validateRefreshWorkspaceBranchCommand(command);
      const response = await http.requestJson<BranchRefreshResponseDto>(
        `/workspaces/${encodeURIComponent(command.workspaceId)}/branches/${encodeURIComponent(command.workspaceBranchId)}/refresh`,
        {
          method: "POST",
          headers: mutationHeaders(command),
          signal
        }
      );

      return mapBranchRefreshDto(response);
    },
    async listWorkspaces(signal) {
      const response = await http.requestJson<WorkspaceListDto>("/workspaces", {
        headers: {
          "X-Correlation-Id": createGatewayMetadata("workspace-list").correlationId
        },
        signal
      });

      return mapWorkspaceListDto(response);
    },
    async deleteWorkspace(command, signal) {
      validateDeleteWorkspaceCommand(command);
      const response = await http.requestJson<WorkspaceCleanupResponseDto>(
        `/workspaces/${encodeURIComponent(command.workspaceId)}`,
        {
          method: "DELETE",
          headers: mutationHeaders(command),
          signal
        }
      );

      return mapWorkspaceCleanupDto(response);
    },
    async getWorkspace(command, signal) {
      const request =
        typeof command === "string"
          ? { workspaceId: command, correlationId: createGatewayMetadata("workspace").correlationId }
          : command;
      validateGetWorkspaceCommand(request);
      const response = await http.requestJson<WorkspaceDto>(
        `/workspaces/${encodeURIComponent(request.workspaceId)}`,
        {
          headers: {
            "X-Correlation-Id": request.correlationId
          },
          signal
        }
      );

      return mapWorkspaceDto(response);
    },
    async waitForWorkspaceCheckout(command, signal) {
      validateGetWorkspaceCommand(command);
      const response = await http.requestJson<WorkspaceDto>(
        `/workspaces/${encodeURIComponent(command.workspaceId)}/checkout-result`,
        {
          headers: {
            "X-Correlation-Id": command.correlationId
          },
          signal,
          timeoutMs: null
        }
      );

      return mapWorkspaceDto(response);
    }
  };

  const diagnostics: DiagnosticsPort = {
    async collectDiagnostics(signal) {
      void signal;
      return [];
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
  requestedOutputs: command.requestedOutputs,
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
    ["correlationId", command.correlationId],
    ["idempotencyKey", command.idempotencyKey],
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
    command.workspacePolicy.timeoutSeconds < 1 ||
    command.workspacePolicy.maxWorkspaceBytes < 1
  ) {
    throw new ApplicationError(
      "VALIDATION_ERROR",
      "Workspace timeout and byte limits must be positive."
    );
  }

  if (!command.requestedOutputs.includes("BTM_RULES")) {
    throw new ApplicationError(
      "VALIDATION_ERROR",
      "BTM_RULES must be requested before starting repository-to-BTM analysis."
    );
  }
};

const validateWorkspaceMetadataCommand = (
  command: PreviewWorkspaceMetadataCommand
): void => {
  validateRequiredText(
    [
      ["repositoryUrl", command.repositoryUrl],
      ["correlationId", command.correlationId],
      ["idempotencyKey", command.idempotencyKey]
    ],
    "previewing repository workspace metadata"
  );
};

const validateCreateWorkspaceCommand = (
  command: CreateWorkspaceCommand
): void => {
  validateRequiredText(
    [
      ["repositoryUrl", command.repositoryUrl],
      ["correlationId", command.correlationId],
      ["idempotencyKey", command.idempotencyKey]
    ],
    "creating a repository workspace"
  );

  if (
    command.workspacePolicy.timeoutSeconds < 1 ||
    command.workspacePolicy.maxWorkspaceBytes < 1
  ) {
    throw new ApplicationError(
      "VALIDATION_ERROR",
      "Workspace timeout and byte limits must be positive."
    );
  }
};

const validateGetWorkspaceCommand = (command: GetWorkspaceCommand): void => {
  validateRequiredText(
    [
      ["workspaceId", command.workspaceId],
      ["correlationId", command.correlationId]
    ],
    "loading a repository workspace"
  );
};

const validateRefreshWorkspaceBranchCommand = (
  command: RefreshWorkspaceBranchCommand
): void => {
  validateRequiredText(
    [
      ["workspaceId", command.workspaceId],
      ["workspaceBranchId", command.workspaceBranchId],
      ["correlationId", command.correlationId],
      ["idempotencyKey", command.idempotencyKey]
    ],
    "refreshing a repository workspace branch"
  );
};

const validateDeleteWorkspaceCommand = (
  command: DeleteWorkspaceCommand
): void => {
  validateRequiredText(
    [
      ["workspaceId", command.workspaceId],
      ["correlationId", command.correlationId],
      ["idempotencyKey", command.idempotencyKey]
    ],
    "deleting a repository workspace"
  );
  validateWorkspaceRouteId(command.workspaceId, "workspaceId");
};

const validateWorkspaceRouteId = (value: string, fieldName: string): void => {
  if (!/^workspace-[A-Za-z0-9_-]+$/.test(value)) {
    throw new ApplicationError(
      "VALIDATION_ERROR",
      `${fieldName} must be an opaque workspace identifier before deleting a repository workspace.`
    );
  }
};

const validateRequiredText = (
  fields: Array<[string, string]>,
  action: string
): void => {
  const missingText = fields.find(
    ([, value]) => typeof value !== "string" || !value.trim()
  );

  if (missingText) {
    throw new ApplicationError(
      "VALIDATION_ERROR",
      `${missingText[0]} is required before ${action}.`
    );
  }
};

const mutationHeaders = (command: {
  correlationId: string;
  idempotencyKey: string;
}): Record<string, string> => ({
  "X-Correlation-Id": command.correlationId,
  "Idempotency-Key": command.idempotencyKey
});

const withSubmittedCommand = (
  analysis: RepositoryAnalysis,
  command: StartRepositoryAnalysisCommand
): RepositoryAnalysis => ({
  ...analysis,
  repositoryUrl: analysis.repositoryUrl || command.repositoryUrl,
  branch: analysis.branch ?? command.branch,
  commit: analysis.commit ?? command.commit,
  correlationId: analysis.correlationId ?? command.correlationId
});

const createGatewayMetadata = (purpose: string): { correlationId: string } => ({
  correlationId: `ui-${purpose}-${createPublicId()}`
});

const createPublicId = (): string => {
  if (globalThis.crypto?.randomUUID) {
    return globalThis.crypto.randomUUID();
  }

  return Date.now().toString(36);
};

export const collectAnalysisDiagnostics = (
  analyses: RepositoryAnalysis[]
) => analyses.flatMap((analysis) => analysis.diagnostics);
