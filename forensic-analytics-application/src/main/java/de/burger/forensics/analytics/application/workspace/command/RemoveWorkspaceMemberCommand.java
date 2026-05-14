package de.burger.forensics.analytics.application.workspace.command;

import de.burger.forensics.analytics.domain.workspace.UserId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;

import java.util.Objects;

public record RemoveWorkspaceMemberCommand(
    WorkspaceId workspaceId,
    UserId memberUserId,
    UserId actorUserId
) {
    public RemoveWorkspaceMemberCommand {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        Objects.requireNonNull(memberUserId, "memberUserId must not be null");
        Objects.requireNonNull(actorUserId, "actorUserId must not be null");
    }
}
