package de.burger.forensics.analytics.application.canvas.command;

import de.burger.forensics.analytics.domain.workspace.UserId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;

import java.util.Objects;

public record GetWorkspaceCanvasCommand(
    WorkspaceId workspaceId,
    UserId actorUserId
) {
    public GetWorkspaceCanvasCommand {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        Objects.requireNonNull(actorUserId, "actorUserId must not be null");
    }
}
