package de.burger.forensics.analytics.application.workspace.command;

import de.burger.forensics.analytics.domain.workspace.UserId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;

import java.util.Objects;

public record ListWorkspaceMembersCommand(
    WorkspaceId workspaceId,
    UserId actorUserId
) {
    public ListWorkspaceMembersCommand {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        Objects.requireNonNull(actorUserId, "actorUserId must not be null");
    }
}
