import type {
  BranchRefreshResult,
  CreateWorkspaceCommand,
  DeleteWorkspaceCommand,
  GetWorkspaceCommand,
  PreviewWorkspaceMetadataCommand,
  RefreshWorkspaceBranchCommand,
  WaitForWorkspaceCheckoutCommand,
  Workspace,
  WorkspaceCleanupResult,
  WorkspaceMetadata
} from "@/domain/workspace";

export interface WorkspacePort {
  previewMetadata(
    command: PreviewWorkspaceMetadataCommand,
    signal?: AbortSignal
  ): Promise<WorkspaceMetadata>;
  createWorkspace(
    command: CreateWorkspaceCommand,
    signal?: AbortSignal
  ): Promise<Workspace>;
  refreshBranch(
    command: RefreshWorkspaceBranchCommand,
    signal?: AbortSignal
  ): Promise<BranchRefreshResult>;
  listWorkspaces(signal?: AbortSignal): Promise<Workspace[]>;
  deleteWorkspace(
    command: DeleteWorkspaceCommand,
    signal?: AbortSignal
  ): Promise<WorkspaceCleanupResult>;
  getWorkspace(
    command: GetWorkspaceCommand | string,
    signal?: AbortSignal
  ): Promise<Workspace>;
  waitForWorkspaceCheckout(
    command: WaitForWorkspaceCheckoutCommand,
    signal?: AbortSignal
  ): Promise<Workspace>;
}
