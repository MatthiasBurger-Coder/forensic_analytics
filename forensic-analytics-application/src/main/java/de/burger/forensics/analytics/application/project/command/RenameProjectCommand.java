package de.burger.forensics.analytics.application.project.command;

import de.burger.forensics.analytics.domain.workspace.ProjectId;
import de.burger.forensics.analytics.domain.workspace.UserId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;

import java.util.Objects;

public record RenameProjectCommand(
    WorkspaceId workspaceId,
    ProjectId projectId,
    String name,
    UserId actorUserId
) {
    public RenameProjectCommand {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        Objects.requireNonNull(projectId, "projectId must not be null");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        Objects.requireNonNull(actorUserId, "actorUserId must not be null");
    }
}
