package de.burger.forensics.analytics.application.workspace.command;

import de.burger.forensics.analytics.domain.workspace.UserId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;

import java.util.Objects;

public record CreateWorkspaceCommand(
    WorkspaceId workspaceId,
    String name,
    UserId ownerUserId
) {
    public CreateWorkspaceCommand {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        Objects.requireNonNull(ownerUserId, "ownerUserId must not be null");
    }
}
