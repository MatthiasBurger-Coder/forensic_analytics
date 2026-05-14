package de.burger.forensics.analytics.application.asset.command;

import de.burger.forensics.analytics.domain.workspace.UserId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;

import java.util.Objects;

public record ListSharedAssetsCommand(
    WorkspaceId workspaceId,
    UserId actorUserId
) {
    public ListSharedAssetsCommand {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        Objects.requireNonNull(actorUserId, "actorUserId must not be null");
    }
}
