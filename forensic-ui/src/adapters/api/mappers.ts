import {
  createStatusState,
  type AnalysisLifecycle,
  type AnalysisStatusState
} from "@/domain/analysisStatus";
import type {
  AnalysisJob,
  RepositoryAnalysis,
  RepositoryAnalysisSummary
} from "@/domain/repositoryAnalysis";
import type { DiagnosticMessage, DiagnosticSeverity } from "@/domain/diagnostic";
import type { Workspace } from "@/domain/workspace";
import { sanitizeDiagnosticText } from "@/shared/safeText";

import type { DiagnosticDto, RepositoryAnalysisDto, WorkspaceDto } from "./dtos";

const STATUS_MAPPING: Record<string, AnalysisLifecycle> = {
  COMPLETED: "SUCCESS",
  FAILED: "FAILED",
  DEAD_LETTERED: "FAILED",
  CLEANED: "CLEANED",
  REGISTERED: "REGISTERED",
  ACCEPTED: "ACCEPTED",
  DISPATCHABLE: "DISPATCHABLE",
  RUNNING: "RUNNING",
  RETRYABLE: "RETRYABLE",
  CANCELED: "CANCELED"
};

export const mapBackendStatus = (status: unknown): AnalysisStatusState => {
  const backendStatus = textOrNull(status);
  const lifecycle =
    backendStatus === null
      ? "UNKNOWN"
      : STATUS_MAPPING[backendStatus.toUpperCase()] ?? "UNKNOWN";

  return createStatusState(backendStatus, lifecycle);
};

export const mapRepositoryAnalysisDto = (
  dto: RepositoryAnalysisDto
): RepositoryAnalysis => ({
  analysisRunId: textOrEmpty(dto.analysisRunId),
  workspaceId: textOrNull(dto.workspaceId),
  repositoryUrl: textOrEmpty(dto.repositoryUrl),
  branch: textOrNull(dto.branch),
  commit: textOrNull(dto.commit),
  resolvedCommit: textOrNull(dto.resolvedCommit),
  checkoutStatus: textOrNull(dto.checkoutStatus),
  status: mapBackendStatus(dto.status),
  sourceRoots: stringArray(dto.sourceRoots),
  diagnostics: diagnostics(dto.diagnostics),
  createdAt: textOrNull(dto.createdAt),
  startedAt: textOrNull(dto.startedAt)
});

export const mapAnalysisJobDto = (dto: RepositoryAnalysisDto): AnalysisJob => ({
  ...mapRepositoryAnalysisDto(dto),
  lastUpdatedAt: textOrNull(dto.lastUpdatedAt)
});

export const mapRepositoryAnalysisSummary = (
  dto: RepositoryAnalysisDto
): RepositoryAnalysisSummary => {
  const analysis = mapRepositoryAnalysisDto(dto);

  return {
    analysisRunId: analysis.analysisRunId,
    workspaceId: analysis.workspaceId,
    repositoryUrl: analysis.repositoryUrl,
    branch: analysis.branch,
    commit: analysis.commit,
    resolvedCommit: analysis.resolvedCommit,
    checkoutStatus: analysis.checkoutStatus,
    status: analysis.status,
    createdAt: analysis.createdAt,
    startedAt: analysis.startedAt,
    diagnostics: analysis.diagnostics
  };
};

export const mapRepositoryAnalysisListDto = (
  value: unknown
): RepositoryAnalysis[] => {
  const items = Array.isArray(value)
    ? value
    : isRecord(value) && Array.isArray(value.items)
      ? value.items
      : [];

  return items
    .filter(isRecord)
    .map((item) => mapRepositoryAnalysisDto(item as RepositoryAnalysisDto));
};

export const mapWorkspaceDto = (dto: WorkspaceDto): Workspace => {
  const analyses = repositoryAnalysisArray(dto.repositoryAnalyses);
  const first = analyses[0];

  return {
    workspaceId: textOrEmpty(dto.workspaceId),
    name: textOrNull(dto.name),
    status: textOrNull(dto.status),
    repositoryUrl: first?.repositoryUrl ?? null,
    branch: first?.branch ?? null,
    createdAt: textOrNull(dto.createdAt),
    updatedAt: textOrNull(dto.updatedAt),
    diagnostics: analyses.flatMap((analysis) => analysis.diagnostics),
    repositoryAnalyses: analyses.map(toSummary)
  };
};

export const mapWorkspaceListDto = (value: unknown): Workspace[] => {
  const items = Array.isArray(value)
    ? value
    : isRecord(value) && Array.isArray(value.items)
      ? value.items
      : [];

  return items
    .filter(isRecord)
    .map((item) => mapWorkspaceDto(item as WorkspaceDto));
};

export const mapWorkspaceViewsFromAnalyses = (
  analyses: RepositoryAnalysis[]
): Workspace[] => {
  const grouped = new Map<string, RepositoryAnalysisSummary[]>();

  analyses.forEach((analysis) => {
    if (analysis.workspaceId === null) {
      return;
    }

    const current = grouped.get(analysis.workspaceId) ?? [];
    current.push(toSummary(analysis));
    grouped.set(analysis.workspaceId, current);
  });

  return Array.from(grouped.entries()).map(([workspaceId, summaries]) => {
    const first = summaries[0];

    return {
      workspaceId,
      name: null,
      status: null,
      repositoryUrl: first?.repositoryUrl ?? null,
      branch: first?.branch ?? null,
      createdAt: null,
      updatedAt: null,
      diagnostics: summaries.flatMap((summary) => summary.diagnostics),
      repositoryAnalyses: summaries
    };
  });
};

export const mapDiagnosticDto = (
  dto: DiagnosticDto,
  index: number
): DiagnosticMessage => ({
  id: textOrNull(dto.id) ?? `diagnostic-${index}`,
  severity: severity(dto.severity),
  code: textOrNull(dto.code),
  message: sanitizeDiagnosticText(dto.message),
  source: textOrNull(dto.source),
  observedAt: textOrNull(dto.observedAt) ?? textOrNull(dto.timestamp)
});

const toSummary = (analysis: RepositoryAnalysis): RepositoryAnalysisSummary => ({
  analysisRunId: analysis.analysisRunId,
  workspaceId: analysis.workspaceId,
  repositoryUrl: analysis.repositoryUrl,
  branch: analysis.branch,
  commit: analysis.commit,
  resolvedCommit: analysis.resolvedCommit,
  checkoutStatus: analysis.checkoutStatus,
  status: analysis.status,
  createdAt: analysis.createdAt,
  startedAt: analysis.startedAt,
  diagnostics: analysis.diagnostics
});

const diagnostics = (value: unknown): DiagnosticMessage[] =>
  Array.isArray(value)
    ? value.map((item, index) =>
        isRecord(item)
          ? mapDiagnosticDto(item as DiagnosticDto, index)
          : mapDiagnosticDto({ message: item }, index)
      )
    : [];

const repositoryAnalysisArray = (value: unknown): RepositoryAnalysis[] =>
  Array.isArray(value)
    ? value
        .filter(isRecord)
        .map((item) => mapRepositoryAnalysisDto(item as RepositoryAnalysisDto))
    : [];

const severity = (value: unknown): DiagnosticSeverity => {
  const normalized = textOrNull(value)?.toUpperCase();

  if (
    normalized === "INFO" ||
    normalized === "WARNING" ||
    normalized === "ERROR"
  ) {
    return normalized;
  }

  return "UNKNOWN";
};

const stringArray = (value: unknown): string[] =>
  Array.isArray(value)
    ? value.map(textOrNull).filter((item): item is string => item !== null)
    : [];

const textOrEmpty = (value: unknown): string => textOrNull(value) ?? "";

const textOrNull = (value: unknown): string | null =>
  typeof value === "string" && value.trim() ? value.trim() : null;

const isRecord = (value: unknown): value is Record<string, unknown> =>
  typeof value === "object" && value !== null && !Array.isArray(value);
