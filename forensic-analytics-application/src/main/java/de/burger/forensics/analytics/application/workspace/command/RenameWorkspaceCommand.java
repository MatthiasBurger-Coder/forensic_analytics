package de.burger.forensics.analytics.application.workspace.command;

import de.burger.forensics.analytics.domain.workspace.UserId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;

import java.util.Objects;

public record RenameWorkspaceCommand(
    WorkspaceId workspaceId,
    String name,
    UserId actorUserId
) {
    public RenameWorkspaceCommand {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        Objects.requireNonNull(actorUserId, "actorUserId must not be null");
    }
}
