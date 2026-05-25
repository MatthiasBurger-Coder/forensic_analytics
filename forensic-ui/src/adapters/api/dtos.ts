export interface ErrorEnvelopeDto {
  code?: unknown;
  message?: unknown;
  retryable?: unknown;
  correlationId?: unknown;
  diagnostics?: unknown;
}

export interface DiagnosticDto {
  id?: unknown;
  severity?: unknown;
  code?: unknown;
  message?: unknown;
  source?: unknown;
  observedAt?: unknown;
  timestamp?: unknown;
}

export interface RepositoryAnalysisDto {
  analysisRunId?: unknown;
  workspaceId?: unknown;
  repositoryUrl?: unknown;
  provider?: unknown;
  branch?: unknown;
  commit?: unknown;
  resolvedCommit?: unknown;
  checkoutStatus?: unknown;
  sourceSnapshotStatus?: unknown;
  workflow?: unknown;
  statusUrl?: unknown;
  jobsUrl?: unknown;
  btmDeliveryStatus?: unknown;
  btmDeliveryService?: unknown;
  correlationId?: unknown;
  status?: unknown;
  sourceRoots?: unknown;
  diagnostics?: unknown;
  createdAt?: unknown;
  startedAt?: unknown;
  lastUpdatedAt?: unknown;
}

export interface RepositoryAnalysisListDto {
  items?: unknown;
}

export interface WorkspacePolicyDto {
  ephemeral?: unknown;
  allowShallowClone?: unknown;
  allowPartialClone?: unknown;
  allowSparseCheckout?: unknown;
  timeoutSeconds?: unknown;
  maxWorkspaceBytes?: unknown;
}

export interface WorkspaceMetadataRequestDto {
  repositoryUrl: string;
}

export interface WorkspaceMetadataResponseDto {
  repositoryKey?: unknown;
  repositoryHost?: unknown;
  repositoryOwner?: unknown;
  repositoryName?: unknown;
  workspaceTitle?: unknown;
  defaultBranch?: unknown;
  diagnostics?: unknown;
}

export interface CreateWorkspaceRequestDto {
  repositoryUrl: string;
  selectedBranch: string | null;
  workspacePolicy: WorkspacePolicyDto;
}

export interface RepositoryIdentityDto {
  repositoryKey?: unknown;
  repositoryUrl?: unknown;
  repositoryHost?: unknown;
  repositoryOwner?: unknown;
  repositoryName?: unknown;
  defaultBranch?: unknown;
}

export interface PublicRepositoryIdentityDto {
  repositoryKey?: unknown;
  repositoryHost?: unknown;
  repositoryOwner?: unknown;
  repositoryName?: unknown;
}

export interface WorkspaceBranchDto {
  workspaceBranchId?: unknown;
  repositoryBranch?: unknown;
  status?: unknown;
  resolvedCommit?: unknown;
  sourceSnapshotId?: unknown;
  sourceRoots?: unknown;
  diagnostics?: unknown;
}

export interface WorkspaceDto {
  workspaceId?: unknown;
  workspaceTitle?: unknown;
  repository?: unknown;
  branches?: unknown;
  status?: unknown;
  diagnostics?: unknown;
}

export interface WorkspaceListItemDto {
  workspaceId?: unknown;
  workspaceTitle?: unknown;
  repository?: unknown;
  branches?: unknown;
  status?: unknown;
  diagnostics?: unknown;
}

export interface WorkspaceListDto {
  items?: unknown;
  diagnostics?: unknown;
}

export interface WorkspaceCleanupResponseDto {
  workspaceId?: unknown;
  status?: unknown;
  diagnostics?: unknown;
}

export interface BranchRefreshResponseDto {
  workspaceBranchId?: unknown;
  repositoryBranch?: unknown;
  status?: unknown;
  changed?: unknown;
  previousCommit?: unknown;
  resolvedCommit?: unknown;
  sourceSnapshotId?: unknown;
  diagnostics?: unknown;
}
