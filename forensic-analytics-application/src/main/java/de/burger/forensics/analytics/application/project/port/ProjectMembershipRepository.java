package de.burger.forensics.analytics.application.project.port;

import de.burger.forensics.analytics.domain.workspace.ProjectId;
import de.burger.forensics.analytics.domain.workspace.ProjectMembership;
import de.burger.forensics.analytics.domain.workspace.UserId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;

import java.util.List;
import java.util.Optional;

public interface ProjectMembershipRepository {
    void save(ProjectMembership membership);

    Optional<ProjectMembership> findMembership(WorkspaceId workspaceId, ProjectId projectId, UserId userId);

    List<ProjectMembership> findByProject(WorkspaceId workspaceId, ProjectId projectId);

    List<ProjectMembership> findByUser(UserId userId);

    void removeMembership(WorkspaceId workspaceId, ProjectId projectId, UserId userId);
}
