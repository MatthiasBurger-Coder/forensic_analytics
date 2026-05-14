package de.burger.forensics.analytics.domain.workspace;

import java.util.Objects;

public record WorkspaceMembership(
    WorkspaceId workspaceId,
    UserId userId,
    WorkspaceRole role
) {
    public WorkspaceMembership {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(role, "role must not be null");
    }
}
