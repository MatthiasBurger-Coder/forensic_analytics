package de.burger.forensics.analytics.persistence;

import de.burger.forensics.analytics.application.workspace.port.WorkspaceRepository;
import de.burger.forensics.analytics.domain.workspace.UserId;
import de.burger.forensics.analytics.domain.workspace.Workspace;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceMembership;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryWorkspaceRepository implements WorkspaceRepository {
    private final Map<WorkspaceId, Workspace> workspaces = new ConcurrentHashMap<>();
    private final Map<MembershipKey, WorkspaceMembership> memberships = new ConcurrentHashMap<>();

    @Override
    public void save(Workspace workspace) {
        Objects.requireNonNull(workspace, "workspace must not be null");
        workspaces.put(workspace.id(), workspace);
    }

    @Override
    public void update(Workspace workspace) {
        save(workspace);
    }

    @Override
    public Optional<Workspace> findById(WorkspaceId workspaceId) {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        return Optional.ofNullable(workspaces.get(workspaceId));
    }

    @Override
    public List<Workspace> findByMember(UserId userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        return memberships.values().stream()
            .filter(membership -> membership.userId().equals(userId))
            .map(WorkspaceMembership::workspaceId)
            .distinct()
            .map(workspaces::get)
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(workspace -> workspace.id().value()))
            .toList();
    }

    @Override
    public void saveMembership(WorkspaceMembership membership) {
        Objects.requireNonNull(membership, "membership must not be null");
        memberships.put(MembershipKey.from(membership), membership);
    }

    @Override
    public Optional<WorkspaceMembership> findMembership(WorkspaceId workspaceId, UserId userId) {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        return Optional.ofNullable(memberships.get(new MembershipKey(workspaceId, userId)));
    }

    @Override
    public List<WorkspaceMembership> findMemberships(WorkspaceId workspaceId) {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        return memberships.values().stream()
            .filter(membership -> membership.workspaceId().equals(workspaceId))
            .sorted(Comparator.comparing(membership -> membership.userId().value()))
            .toList();
    }

    @Override
    public void removeMembership(WorkspaceId workspaceId, UserId userId) {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        memberships.remove(new MembershipKey(workspaceId, userId));
    }

    private record MembershipKey(WorkspaceId workspaceId, UserId userId) {
        private MembershipKey {
            Objects.requireNonNull(workspaceId, "workspaceId must not be null");
            Objects.requireNonNull(userId, "userId must not be null");
        }

        static MembershipKey from(WorkspaceMembership membership) {
            return new MembershipKey(membership.workspaceId(), membership.userId());
        }
    }
}
