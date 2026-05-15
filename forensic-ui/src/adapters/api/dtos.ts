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

export interface WorkspaceDto {
  workspaceId?: unknown;
  name?: unknown;
  status?: unknown;
  createdAt?: unknown;
  updatedAt?: unknown;
  repositoryAnalyses?: unknown;
}

export interface WorkspaceListDto {
  items?: unknown;
}
