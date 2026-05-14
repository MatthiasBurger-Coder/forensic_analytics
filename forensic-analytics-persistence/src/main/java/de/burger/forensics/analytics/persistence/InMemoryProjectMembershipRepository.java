package de.burger.forensics.analytics.persistence;

import de.burger.forensics.analytics.application.project.port.ProjectMembershipRepository;
import de.burger.forensics.analytics.domain.workspace.ProjectId;
import de.burger.forensics.analytics.domain.workspace.ProjectMembership;
import de.burger.forensics.analytics.domain.workspace.UserId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryProjectMembershipRepository implements ProjectMembershipRepository {
    private final Map<ProjectMembershipKey, ProjectMembership> memberships = new ConcurrentHashMap<>();

    @Override
    public void save(ProjectMembership membership) {
        Objects.requireNonNull(membership, "membership must not be null");
        memberships.put(ProjectMembershipKey.from(membership), membership);
    }

    @Override
    public Optional<ProjectMembership> findMembership(WorkspaceId workspaceId, ProjectId projectId, UserId userId) {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        Objects.requireNonNull(projectId, "projectId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        return Optional.ofNullable(memberships.get(new ProjectMembershipKey(workspaceId, projectId, userId)));
    }

    @Override
    public List<ProjectMembership> findByProject(WorkspaceId workspaceId, ProjectId projectId) {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        Objects.requireNonNull(projectId, "projectId must not be null");
        return memberships.values().stream()
            .filter(membership -> membership.workspaceId().equals(workspaceId))
            .filter(membership -> membership.projectId().equals(projectId))
            .sorted(Comparator.comparing(membership -> membership.userId().value()))
            .toList();
    }

    @Override
    public List<ProjectMembership> findByUser(UserId userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        return memberships.values().stream()
            .filter(membership -> membership.userId().equals(userId))
            .sorted(Comparator
                .comparing((ProjectMembership membership) -> membership.workspaceId().value())
                .thenComparing(membership -> membership.projectId().value()))
            .toList();
    }

    @Override
    public void removeMembership(WorkspaceId workspaceId, ProjectId projectId, UserId userId) {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        Objects.requireNonNull(projectId, "projectId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        memberships.remove(new ProjectMembershipKey(workspaceId, projectId, userId));
    }

    private record ProjectMembershipKey(WorkspaceId workspaceId, ProjectId projectId, UserId userId) {
        private ProjectMembershipKey {
            Objects.requireNonNull(workspaceId, "workspaceId must not be null");
            Objects.requireNonNull(projectId, "projectId must not be null");
            Objects.requireNonNull(userId, "userId must not be null");
        }

        static ProjectMembershipKey from(ProjectMembership membership) {
            return new ProjectMembershipKey(membership.workspaceId(), membership.projectId(), membership.userId());
        }
    }
}
