package de.burger.forensics.analytics.application.project.command;

import de.burger.forensics.analytics.domain.workspace.ProjectId;
import de.burger.forensics.analytics.domain.workspace.UserId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceRole;

import java.util.Objects;

public record ChangeProjectMemberRoleCommand(
    WorkspaceId workspaceId,
    ProjectId projectId,
    UserId memberUserId,
    WorkspaceRole role,
    UserId actorUserId
) {
    public ChangeProjectMemberRoleCommand {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        Objects.requireNonNull(projectId, "projectId must not be null");
        Objects.requireNonNull(memberUserId, "memberUserId must not be null");
        Objects.requireNonNull(role, "role must not be null");
        Objects.requireNonNull(actorUserId, "actorUserId must not be null");
    }
}
