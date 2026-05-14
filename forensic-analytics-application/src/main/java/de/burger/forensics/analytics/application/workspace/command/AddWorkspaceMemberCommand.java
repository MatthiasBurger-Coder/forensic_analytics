package de.burger.forensics.analytics.application.workspace.command;

import de.burger.forensics.analytics.domain.workspace.UserId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceRole;

import java.util.Objects;

public record AddWorkspaceMemberCommand(
    WorkspaceId workspaceId,
    UserId memberUserId,
    WorkspaceRole role,
    UserId actorUserId
) {
    public AddWorkspaceMemberCommand {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        Objects.requireNonNull(memberUserId, "memberUserId must not be null");
        Objects.requireNonNull(role, "role must not be null");
        Objects.requireNonNull(actorUserId, "actorUserId must not be null");
    }
}
