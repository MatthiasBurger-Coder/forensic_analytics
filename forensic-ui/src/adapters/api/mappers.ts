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
import type {
  BranchRefreshResult,
  RepositoryIdentity,
  Workspace,
  WorkspaceBranch,
  WorkspaceBranchStatus,
  WorkspaceCleanupResult,
  WorkspaceMetadata,
  WorkspaceStatus
} from "@/domain/workspace";
import { sanitizeDiagnosticText } from "@/shared/safeText";

import type {
  BranchRefreshResponseDto,
  DatabaseSettingsPublicViewDto,
  DatabaseSettingsStatusDto,
  DatabaseSettingsValidationResponseDto,
  DiagnosticDto,
  PublicRepositoryIdentityDto,
  RepositoryAnalysisDto,
  RepositoryIdentityDto,
  WorkspaceBranchDto,
  WorkspaceCleanupResponseDto,
  WorkspaceDto,
  WorkspaceListItemDto,
  WorkspaceListDto,
  WorkspaceMetadataResponseDto
} from "./dtos";
import type {
  DatabaseSettingsAvailability,
  DatabaseSettingsValidationResult,
  DatabaseSettingsValidationStatus,
  DatabaseSettingsView,
  DatabaseSettingsStatus
} from "@/domain/settings";

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
  sourceSnapshotStatus: textOrNull(dto.sourceSnapshotStatus),
  workflow: textOrNull(dto.workflow),
  statusUrl: textOrNull(dto.statusUrl),
  jobsUrl: textOrNull(dto.jobsUrl),
  btmDeliveryStatus: textOrNull(dto.btmDeliveryStatus),
  btmDeliveryService: textOrNull(dto.btmDeliveryService),
  correlationId: textOrNull(dto.correlationId),
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
    sourceSnapshotStatus: analysis.sourceSnapshotStatus,
    workflow: analysis.workflow,
    statusUrl: analysis.statusUrl,
    jobsUrl: analysis.jobsUrl,
    btmDeliveryStatus: analysis.btmDeliveryStatus,
    btmDeliveryService: analysis.btmDeliveryService,
    correlationId: analysis.correlationId,
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
  return {
    workspaceId: textOrEmpty(dto.workspaceId),
    workspaceTitle: textOrEmpty(dto.workspaceTitle),
    repository: mapRepositoryIdentityDto(
      isRecord(dto.repository) ? (dto.repository as RepositoryIdentityDto) : {}
    ),
    branches: workspaceBranchArray(dto.branches),
    status: workspaceStatus(dto.status),
    diagnostics: diagnostics(dto.diagnostics)
  };
};

export const mapWorkspaceListDto = (value: WorkspaceListDto | unknown): Workspace[] => {
  const items = Array.isArray(value)
    ? value
    : isRecord(value) && Array.isArray(value.items)
      ? value.items
      : [];

  return items
    .filter(isRecord)
    .map((item) => mapWorkspaceListItemDto(item as WorkspaceListItemDto));
};

export const mapWorkspaceMetadataDto = (
  dto: WorkspaceMetadataResponseDto
): WorkspaceMetadata => ({
  repositoryKey: textOrEmpty(dto.repositoryKey),
  repositoryHost: textOrEmpty(dto.repositoryHost),
  repositoryOwner: textOrNull(dto.repositoryOwner),
  repositoryName: textOrEmpty(dto.repositoryName),
  workspaceTitle: textOrEmpty(dto.workspaceTitle),
  defaultBranch: textOrNull(dto.defaultBranch),
  repositoryBranches: stringArray(dto.repositoryBranches),
  diagnostics: diagnostics(dto.diagnostics)
});

export const mapBranchRefreshDto = (
  dto: BranchRefreshResponseDto
): BranchRefreshResult => ({
  workspaceBranchId: textOrEmpty(dto.workspaceBranchId),
  repositoryBranch: textOrEmpty(dto.repositoryBranch),
  status: branchStatus(dto.status),
  changed: dto.changed === true,
  previousCommit: textOrNull(dto.previousCommit),
  resolvedCommit: textOrNull(dto.resolvedCommit),
  sourceSnapshotId: textOrNull(dto.sourceSnapshotId),
  diagnostics: diagnostics(dto.diagnostics)
});

export const mapWorkspaceCleanupDto = (
  dto: WorkspaceCleanupResponseDto
): WorkspaceCleanupResult => ({
  workspaceId: textOrEmpty(dto.workspaceId),
  status: cleanupStatus(dto.status),
  diagnostics: publicDiagnostics(dto.diagnostics)
});

export const mapDatabaseSettingsStatusDto = (
  dto: DatabaseSettingsStatusDto
): DatabaseSettingsStatus => ({
  settings: mapDatabaseSettingsViewDto(
    isRecord(dto.settings) ? (dto.settings as DatabaseSettingsPublicViewDto) : {}
  ),
  status: databaseSettingsAvailability(dto.status),
  diagnostics: publicDiagnostics(dto.diagnostics)
});

export const mapDatabaseSettingsValidationResponseDto = (
  dto: DatabaseSettingsValidationResponseDto
): DatabaseSettingsValidationResult => ({
  settings: mapDatabaseSettingsViewDto(
    isRecord(dto.settings) ? (dto.settings as DatabaseSettingsPublicViewDto) : {}
  ),
  validationStatus: databaseSettingsValidationStatus(dto.validationStatus),
  applyMode: textOrEmpty(dto.applyMode),
  hotApplySupported: dto.hotApplySupported === true,
  diagnostics: publicDiagnostics(dto.diagnostics)
});

export const mapDiagnosticDto = (
  dto: DiagnosticDto,
  index: number
): DiagnosticMessage => ({
  id: textOrNull(dto.id) ?? `diagnostic-${index}`,
  severity: severity(dto.severity),
  code: textOrNull(dto.code),
  message: sanitizePublicDiagnosticText(dto.message),
  source: sanitizedOptionalText(dto.source),
  observedAt: textOrNull(dto.observedAt) ?? textOrNull(dto.timestamp)
});

const diagnostics = (value: unknown): DiagnosticMessage[] =>
  Array.isArray(value)
    ? value.map((item, index) =>
        isRecord(item)
          ? mapDiagnosticDto(item as DiagnosticDto, index)
          : mapDiagnosticDto({ message: item }, index)
      )
    : [];

const publicDiagnostics = (value: unknown): DiagnosticMessage[] =>
  Array.isArray(value)
    ? value.map((item, index) =>
        isRecord(item)
          ? mapPublicDiagnosticDto(item as DiagnosticDto, index)
          : mapPublicDiagnosticDto({ message: item }, index)
      )
    : [];

const mapPublicDiagnosticDto = (
  dto: DiagnosticDto,
  index: number
): DiagnosticMessage => ({
  id: textOrNull(dto.id) ?? `diagnostic-${index}`,
  severity: severity(dto.severity),
  code: textOrNull(dto.code),
  message: sanitizePublicDiagnosticText(dto.message),
  source: publicSanitizedOptionalText(dto.source),
  observedAt: textOrNull(dto.observedAt) ?? textOrNull(dto.timestamp)
});

const mapRepositoryIdentityDto = (
  dto: RepositoryIdentityDto
): RepositoryIdentity => ({
  repositoryKey: textOrEmpty(dto.repositoryKey),
  repositoryUrl: textOrEmpty(dto.repositoryUrl),
  repositoryHost: textOrEmpty(dto.repositoryHost),
  repositoryOwner: textOrNull(dto.repositoryOwner),
  repositoryName: textOrEmpty(dto.repositoryName),
  defaultBranch: textOrNull(dto.defaultBranch)
});

const mapPublicRepositoryIdentityDto = (
  dto: PublicRepositoryIdentityDto
): RepositoryIdentity => ({
  repositoryKey: textOrEmpty(dto.repositoryKey),
  repositoryUrl: "",
  repositoryHost: textOrEmpty(dto.repositoryHost),
  repositoryOwner: textOrNull(dto.repositoryOwner),
  repositoryName: textOrEmpty(dto.repositoryName),
  defaultBranch: null
});

const mapWorkspaceListItemDto = (dto: WorkspaceListItemDto): Workspace => ({
  workspaceId: textOrEmpty(dto.workspaceId),
  workspaceTitle: textOrEmpty(dto.workspaceTitle),
  repository: mapPublicRepositoryIdentityDto(
    isRecord(dto.repository) ? (dto.repository as PublicRepositoryIdentityDto) : {}
  ),
  branches: publicWorkspaceBranchArray(dto.branches),
  status: workspaceStatus(dto.status),
  diagnostics: publicDiagnostics(dto.diagnostics)
});

const workspaceBranchArray = (value: unknown): WorkspaceBranch[] =>
  Array.isArray(value)
    ? value
        .filter(isRecord)
        .map((item) => mapWorkspaceBranchDto(item as WorkspaceBranchDto))
    : [];

const publicWorkspaceBranchArray = (value: unknown): WorkspaceBranch[] =>
  Array.isArray(value)
    ? value
        .filter(isRecord)
        .map((item) => mapPublicWorkspaceBranchDto(item as WorkspaceBranchDto))
    : [];

const mapWorkspaceBranchDto = (dto: WorkspaceBranchDto): WorkspaceBranch => ({
  workspaceBranchId: textOrEmpty(dto.workspaceBranchId),
  repositoryBranch: textOrEmpty(dto.repositoryBranch),
  status: branchStatus(dto.status),
  resolvedCommit: textOrNull(dto.resolvedCommit),
  sourceSnapshotId: textOrNull(dto.sourceSnapshotId),
  sourceRoots: stringArray(dto.sourceRoots).map(sanitizeDiagnosticText),
  diagnostics: diagnostics(dto.diagnostics)
});

const mapPublicWorkspaceBranchDto = (dto: WorkspaceBranchDto): WorkspaceBranch => ({
  workspaceBranchId: textOrEmpty(dto.workspaceBranchId),
  repositoryBranch: textOrEmpty(dto.repositoryBranch),
  status: branchStatus(dto.status),
  resolvedCommit: textOrNull(dto.resolvedCommit),
  sourceSnapshotId: textOrNull(dto.sourceSnapshotId),
  sourceRoots: stringArray(dto.sourceRoots).map(sanitizePublicDiagnosticText),
  diagnostics: publicDiagnostics(dto.diagnostics)
});

const mapDatabaseSettingsViewDto = (
  dto: DatabaseSettingsPublicViewDto
): DatabaseSettingsView => ({
  engine: textOrEmpty(dto.engine),
  host: textOrEmpty(dto.host),
  port: numberOrDefault(dto.port, 0),
  databaseName: textOrEmpty(dto.databaseName),
  username: textOrEmpty(dto.username),
  authenticationConfigured: dto.authenticationConfigured === true,
  schema: textOrEmpty(dto.schema),
  sslMode: textOrEmpty(dto.sslMode),
  configurationSource: textOrEmpty(dto.configurationSource),
  applyMode: textOrEmpty(dto.applyMode),
  hotApplySupported: dto.hotApplySupported === true
});

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

const numberOrDefault = (value: unknown, fallback: number): number =>
  typeof value === "number" && Number.isFinite(value) ? value : fallback;

const sanitizedOptionalText = (value: unknown): string | null => {
  const text = textOrNull(value);

  return text === null ? null : sanitizePublicDiagnosticText(text);
};

const publicSanitizedOptionalText = (value: unknown): string | null => {
  const text = textOrNull(value);

  return text === null ? null : sanitizePublicDiagnosticText(text);
};

const sanitizePublicDiagnosticText = (value: unknown): string =>
  sanitizeDiagnosticText(value).replace(/\bhttps?:\/\/[^\s<>"']+/gi, "[url-redacted]");

const workspaceStatus = (value: unknown): WorkspaceStatus => {
  const normalized = textOrNull(value)?.toUpperCase();

  if (
    normalized === "NEW" ||
    normalized === "CHECKING_OUT" ||
    normalized === "READY" ||
    normalized === "CHECKED_OUT" ||
    normalized === "CLEANED" ||
    normalized === "FAILED"
  ) {
    return normalized;
  }

  return "UNKNOWN";
};

const cleanupStatus = (value: unknown): WorkspaceStatus =>
  workspaceStatus(value) === "CLEANED" ? "CLEANED" : "UNKNOWN";

const databaseSettingsAvailability = (
  value: unknown
): DatabaseSettingsAvailability => {
  const normalized = textOrNull(value)?.toUpperCase();

  return normalized === "AVAILABLE" || normalized === "UNAVAILABLE"
    ? normalized
    : "UNKNOWN";
};

const databaseSettingsValidationStatus = (
  value: unknown
): DatabaseSettingsValidationStatus => {
  const normalized = textOrNull(value)?.toUpperCase();

  if (
    normalized === "VALID" ||
    normalized === "INVALID" ||
    normalized === "UNREACHABLE" ||
    normalized === "AUTHENTICATION_FAILED" ||
    normalized === "UNSUPPORTED"
  ) {
    return normalized;
  }

  return "UNKNOWN";
};

const branchStatus = (value: unknown): WorkspaceBranchStatus => {
  const normalized = textOrNull(value)?.toUpperCase();

  if (
    normalized === "CHECKING_OUT" ||
    normalized === "CHECKED_OUT" ||
    normalized === "UP_TO_DATE" ||
    normalized === "UPDATING" ||
    normalized === "UPDATED" ||
    normalized === "FAILED"
  ) {
    return normalized;
  }

  return "UNKNOWN";
};

const isRecord = (value: unknown): value is Record<string, unknown> =>
  typeof value === "object" && value !== null && !Array.isArray(value);
