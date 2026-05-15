import type { Workspace } from "@/domain/workspace";

export interface WorkspacePort {
  listWorkspaces(signal?: AbortSignal): Promise<Workspace[]>;
  getWorkspace(workspaceId: string, signal?: AbortSignal): Promise<Workspace>;
}
