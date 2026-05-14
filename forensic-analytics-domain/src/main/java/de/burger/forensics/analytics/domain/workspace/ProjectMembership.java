package de.burger.forensics.analytics.domain.workspace;

import java.util.Objects;

public record ProjectMembership(
    WorkspaceProject project,
    UserId userId,
    WorkspaceRole role
) {
    public ProjectMembership {
        Objects.requireNonNull(project, "project must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(role, "role must not be null");
    }

    public WorkspaceId workspaceId() {
        return project.workspaceId();
    }

    public ProjectId projectId() {
        return project.id();
    }
}
