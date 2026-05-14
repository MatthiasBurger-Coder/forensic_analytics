package de.burger.forensics.analytics.domain.workspace;

import java.util.Objects;

public record WorkspaceProject(
    ProjectId id,
    WorkspaceId workspaceId,
    String name,
    ProjectStatus status
) {
    public WorkspaceProject {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        RequiredWorkspaceText.requireText(name, "project name");
        Objects.requireNonNull(status, "status must not be null");
    }

    public static WorkspaceProject active(ProjectId id, WorkspaceId workspaceId, String name) {
        return new WorkspaceProject(id, workspaceId, name, ProjectStatus.ACTIVE);
    }

    public WorkspaceProject rename(String newName) {
        return new WorkspaceProject(id, workspaceId, newName, status);
    }

    public WorkspaceProject archive() {
        return new WorkspaceProject(id, workspaceId, name, ProjectStatus.ARCHIVED);
    }

    public boolean acceptsChanges() {
        return status.acceptsChanges();
    }

    public boolean belongsTo(WorkspaceId candidateWorkspaceId) {
        return workspaceId.equals(Objects.requireNonNull(candidateWorkspaceId, "candidateWorkspaceId must not be null"));
    }
}
