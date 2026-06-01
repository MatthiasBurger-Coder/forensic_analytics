import type { DiagnosticMessage } from "@/domain/diagnostic";

export type WorkspaceStatus =
  | "NEW"
  | "CHECKING_OUT"
  | "READY"
  | "CHECKED_OUT"
  | "CLEANED"
  | "FAILED"
  | "UNKNOWN";

export type WorkspaceBranchStatus =
  | "CHECKING_OUT"
  | "CHECKED_OUT"
  | "UP_TO_DATE"
  | "UPDATING"
  | "UPDATED"
  | "FAILED"
  | "UNKNOWN";

export interface RepositoryIdentity {
  repositoryKey: string;
  repositoryUrl: string;
  repositoryHost: string;
  repositoryOwner: string | null;
  repositoryName: string;
  defaultBranch: string | null;
}

export interface WorkspaceMetadata {
  repositoryKey: string;
  repositoryHost: string;
  repositoryOwner: string | null;
  repositoryName: string;
  workspaceTitle: string;
  defaultBranch: string | null;
  repositoryBranches: string[];
  diagnostics: DiagnosticMessage[];
}

export interface WorkspacePolicy {
  ephemeral: false;
  allowShallowClone: boolean;
  allowPartialClone: false;
  allowSparseCheckout: false;
  timeoutSeconds: number;
  maxWorkspaceBytes: number;
}

export interface WorkspaceBranch {
  workspaceBranchId: string;
  repositoryBranch: string;
  status: WorkspaceBranchStatus;
  resolvedCommit: string | null;
  sourceSnapshotId: string | null;
  sourceRoots: string[];
  diagnostics: DiagnosticMessage[];
}

export interface Workspace {
  workspaceId: string;
  workspaceTitle: string;
  repository: RepositoryIdentity;
  branches: WorkspaceBranch[];
  status: WorkspaceStatus;
  diagnostics: DiagnosticMessage[];
}

export interface BranchRefreshResult {
  workspaceBranchId: string;
  repositoryBranch: string;
  status: WorkspaceBranchStatus;
  changed: boolean;
  previousCommit: string | null;
  resolvedCommit: string | null;
  sourceSnapshotId: string | null;
  diagnostics: DiagnosticMessage[];
}

export interface WorkspaceCleanupResult {
  workspaceId: string;
  status: WorkspaceStatus;
  diagnostics: DiagnosticMessage[];
}

export interface PreviewWorkspaceMetadataCommand {
  repositoryUrl: string;
  correlationId: string;
  idempotencyKey: string;
}

export interface CreateWorkspaceCommand {
  repositoryUrl: string;
  selectedBranch: string | null;
  workspacePolicy: WorkspacePolicy;
  correlationId: string;
  idempotencyKey: string;
}

export interface GetWorkspaceCommand {
  workspaceId: string;
  correlationId: string;
}

export interface WaitForWorkspaceCheckoutCommand {
  workspaceId: string;
  correlationId: string;
}

export interface RefreshWorkspaceBranchCommand {
  workspaceId: string;
  workspaceBranchId: string;
  correlationId: string;
  idempotencyKey: string;
}

export interface DeleteWorkspaceCommand {
  workspaceId: string;
  correlationId: string;
  idempotencyKey: string;
}
