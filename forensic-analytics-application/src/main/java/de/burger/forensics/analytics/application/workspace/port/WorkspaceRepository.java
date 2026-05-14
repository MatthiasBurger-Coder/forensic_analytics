package de.burger.forensics.analytics.application.workspace.port;

import de.burger.forensics.analytics.domain.workspace.UserId;
import de.burger.forensics.analytics.domain.workspace.Workspace;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceMembership;

import java.util.List;
import java.util.Optional;

public interface WorkspaceRepository {
    void save(Workspace workspace);

    void update(Workspace workspace);

    Optional<Workspace> findById(WorkspaceId workspaceId);

    List<Workspace> findByMember(UserId userId);

    void saveMembership(WorkspaceMembership membership);

    Optional<WorkspaceMembership> findMembership(WorkspaceId workspaceId, UserId userId);

    List<WorkspaceMembership> findMemberships(WorkspaceId workspaceId);

    void removeMembership(WorkspaceId workspaceId, UserId userId);
}
