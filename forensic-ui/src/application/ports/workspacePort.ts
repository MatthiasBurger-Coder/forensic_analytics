import type {
  BranchRefreshResult,
  CreateWorkspaceCommand,
  GetWorkspaceCommand,
  PreviewWorkspaceMetadataCommand,
  RefreshWorkspaceBranchCommand,
  WaitForWorkspaceCheckoutCommand,
  Workspace,
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
  getWorkspace(
    command: GetWorkspaceCommand | string,
    signal?: AbortSignal
  ): Promise<Workspace>;
  waitForWorkspaceCheckout(
    command: WaitForWorkspaceCheckoutCommand,
    signal?: AbortSignal
  ): Promise<Workspace>;
}
