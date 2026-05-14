package de.burger.forensics.analytics.application.asset.command;

import de.burger.forensics.analytics.domain.workspace.ProjectId;
import de.burger.forensics.analytics.domain.workspace.UserId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;

import java.util.Objects;

public record ListProjectAssetsCommand(
    WorkspaceId workspaceId,
    ProjectId projectId,
    UserId actorUserId
) {
    public ListProjectAssetsCommand {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        Objects.requireNonNull(projectId, "projectId must not be null");
        Objects.requireNonNull(actorUserId, "actorUserId must not be null");
    }
}
