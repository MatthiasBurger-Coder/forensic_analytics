package de.burger.forensics.analytics.application.retention;

import de.burger.forensics.analytics.application.retention.command.ConfigureWorkspaceRetentionCommand;
import de.burger.forensics.analytics.application.retention.port.RetentionPolicyRepository;
import de.burger.forensics.analytics.application.workspace.WorkspaceAccessDeniedException;
import de.burger.forensics.analytics.application.workspace.WorkspaceArchivedException;
import de.burger.forensics.analytics.application.workspace.WorkspaceNotFoundException;
import de.burger.forensics.analytics.application.workspace.port.WorkspaceAuditPort;
import de.burger.forensics.analytics.application.workspace.port.WorkspaceRepository;
import de.burger.forensics.analytics.domain.audit.AuditEvent;
import de.burger.forensics.analytics.domain.workspace.RetentionPolicy;
import de.burger.forensics.analytics.domain.workspace.UserId;
import de.burger.forensics.analytics.domain.workspace.Workspace;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceMembership;
import de.burger.forensics.analytics.domain.workspace.WorkspaceRole;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultRetentionPolicyUseCaseTest {
    private static final WorkspaceId WORKSPACE_A = new WorkspaceId("workspace-a");
    private static final UserId OWNER_ID = new UserId("owner-a");
    private static final UserId ADMIN_ID = new UserId("admin-a");
    private static final UserId VIEWER_ID = new UserId("viewer-a");
    private static final UserId OUTSIDER_ID = new UserId("outsider-a");
    private static final Instant NOW = Instant.parse("2026-05-14T15:15:30Z");

    private final RecordingWorkspaceRepository workspaceRepository = new RecordingWorkspaceRepository();
    private final RecordingRetentionPolicyRepository retentionPolicyRepository = new RecordingRetentionPolicyRepository();
    private final RecordingWorkspaceAuditPort auditPort = new RecordingWorkspaceAuditPort();
    private final DefaultRetentionPolicyUseCase useCase = new DefaultRetentionPolicyUseCase(
        workspaceRepository,
        retentionPolicyRepository,
        auditPort,
        Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void ownerAndAdminCanConfigureWorkspaceRetention() {
        activeWorkspaceWithMembers();

        var ownerPolicy = useCase.configure(new ConfigureWorkspaceRetentionCommand(WORKSPACE_A, 30, OWNER_ID));
        var adminPolicy = useCase.configure(new ConfigureWorkspaceRetentionCommand(WORKSPACE_A, 60, ADMIN_ID));

        assertEquals(new RetentionPolicy(30), ownerPolicy);
        assertEquals(new RetentionPolicy(60), adminPolicy);
        assertEquals(Optional.of(new RetentionPolicy(60)), retentionPolicyRepository.findByWorkspace(WORKSPACE_A));
        assertEquals("workspace.retention.updated", auditPort.events.getFirst().action());
        assertEquals("30", auditPort.events.getFirst().metadata().get("retentionDays"));
        assertEquals(NOW, auditPort.events.getFirst().occurredAt());
    }

    @Test
    void viewerAndOutsiderCannotConfigureRetention() {
        activeWorkspaceWithMembers();

        assertThrows(
            WorkspaceAccessDeniedException.class,
            () -> useCase.configure(new ConfigureWorkspaceRetentionCommand(WORKSPACE_A, 30, VIEWER_ID))
        );
        assertThrows(
            WorkspaceAccessDeniedException.class,
            () -> useCase.configure(new ConfigureWorkspaceRetentionCommand(WORKSPACE_A, 30, OUTSIDER_ID))
        );
    }

    @Test
    void archivedWorkspaceRejectsRetentionChanges() {
        activeWorkspaceWithMembers();
        workspaceRepository.update(Workspace.active(WORKSPACE_A, "Workspace A").archive());

        assertThrows(
            WorkspaceArchivedException.class,
            () -> useCase.configure(new ConfigureWorkspaceRetentionCommand(WORKSPACE_A, 30, OWNER_ID))
        );
    }

    @Test
    void unknownWorkspaceFailsExplicitly() {
        assertThrows(
            WorkspaceNotFoundException.class,
            () -> useCase.configure(new ConfigureWorkspaceRetentionCommand(WORKSPACE_A, 30, OWNER_ID))
        );
    }

    @Test
    void dependenciesAndCommandsAreRequired() {
        assertThrows(NullPointerException.class, () -> new DefaultRetentionPolicyUseCase(null, retentionPolicyRepository, auditPort, Clock.systemUTC()));
        assertThrows(NullPointerException.class, () -> new DefaultRetentionPolicyUseCase(workspaceRepository, null, auditPort, Clock.systemUTC()));
        assertThrows(NullPointerException.class, () -> new DefaultRetentionPolicyUseCase(workspaceRepository, retentionPolicyRepository, null, Clock.systemUTC()));
        assertThrows(NullPointerException.class, () -> new DefaultRetentionPolicyUseCase(workspaceRepository, retentionPolicyRepository, auditPort, null));
        assertThrows(NullPointerException.class, () -> useCase.configure(null));
        assertThrows(NullPointerException.class, () -> new ConfigureWorkspaceRetentionCommand(null, 30, OWNER_ID));
        assertThrows(IllegalArgumentException.class, () -> new ConfigureWorkspaceRetentionCommand(WORKSPACE_A, 0, OWNER_ID));
        assertThrows(NullPointerException.class, () -> new ConfigureWorkspaceRetentionCommand(WORKSPACE_A, 30, null));
    }

    private void activeWorkspaceWithMembers() {
        workspaceRepository.save(Workspace.active(WORKSPACE_A, "Workspace A"));
        workspaceRepository.saveMembership(new WorkspaceMembership(WORKSPACE_A, OWNER_ID, WorkspaceRole.OWNER));
        workspaceRepository.saveMembership(new WorkspaceMembership(WORKSPACE_A, ADMIN_ID, WorkspaceRole.ADMIN));
        workspaceRepository.saveMembership(new WorkspaceMembership(WORKSPACE_A, VIEWER_ID, WorkspaceRole.VIEWER));
    }

    private static final class RecordingWorkspaceRepository implements WorkspaceRepository {
        private final List<Workspace> workspaces = new ArrayList<>();
        private final List<WorkspaceMembership> memberships = new ArrayList<>();

        @Override
        public void save(Workspace workspace) {
            workspaces.add(workspace);
        }

        @Override
        public void update(Workspace workspace) {
            workspaces.removeIf(existing -> existing.id().equals(workspace.id()));
            workspaces.add(workspace);
        }

        @Override
        public Optional<Workspace> findById(WorkspaceId workspaceId) {
            return workspaces.stream().filter(workspace -> workspace.id().equals(workspaceId)).findFirst();
        }

        @Override
        public List<Workspace> findByMember(UserId userId) {
            var workspaceIds = memberships.stream()
                .filter(membership -> membership.userId().equals(userId))
                .map(WorkspaceMembership::workspaceId)
                .toList();
            return workspaces.stream()
                .filter(workspace -> workspaceIds.contains(workspace.id()))
                .sorted(Comparator.comparing(workspace -> workspace.id().value()))
                .toList();
        }

        @Override
        public void saveMembership(WorkspaceMembership membership) {
            removeMembership(membership.workspaceId(), membership.userId());
            memberships.add(membership);
        }

        @Override
        public Optional<WorkspaceMembership> findMembership(WorkspaceId workspaceId, UserId userId) {
            return memberships.stream()
                .filter(membership -> membership.workspaceId().equals(workspaceId))
                .filter(membership -> membership.userId().equals(userId))
                .findFirst();
        }

        @Override
        public List<WorkspaceMembership> findMemberships(WorkspaceId workspaceId) {
            return memberships.stream()
                .filter(membership -> membership.workspaceId().equals(workspaceId))
                .sorted(Comparator.comparing(membership -> membership.userId().value()))
                .toList();
        }

        @Override
        public void removeMembership(WorkspaceId workspaceId, UserId userId) {
            memberships.removeIf(membership ->
                membership.workspaceId().equals(workspaceId)
                    && membership.userId().equals(userId)
            );
        }
    }

    private static final class RecordingRetentionPolicyRepository implements RetentionPolicyRepository {
        private final List<StoredPolicy> policies = new ArrayList<>();

        @Override
        public void save(WorkspaceId workspaceId, RetentionPolicy policy) {
            policies.removeIf(stored -> stored.workspaceId.equals(workspaceId));
            policies.add(new StoredPolicy(workspaceId, policy));
        }

        @Override
        public Optional<RetentionPolicy> findByWorkspace(WorkspaceId workspaceId) {
            return policies.stream()
                .filter(stored -> stored.workspaceId.equals(workspaceId))
                .map(StoredPolicy::policy)
                .findFirst();
        }

        private record StoredPolicy(WorkspaceId workspaceId, RetentionPolicy policy) {
        }
    }

    private static final class RecordingWorkspaceAuditPort implements WorkspaceAuditPort {
        private final List<AuditEvent> events = new ArrayList<>();

        @Override
        public void publish(AuditEvent event) {
            events.add(event);
        }
    }
}
