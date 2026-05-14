package de.burger.forensics.analytics.application.workspace.command;

import de.burger.forensics.analytics.domain.workspace.UserId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;

import java.util.Objects;

public record GetWorkspaceCommand(
    WorkspaceId workspaceId,
    UserId actorUserId
) {
    public GetWorkspaceCommand {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        Objects.requireNonNull(actorUserId, "actorUserId must not be null");
    }
}
