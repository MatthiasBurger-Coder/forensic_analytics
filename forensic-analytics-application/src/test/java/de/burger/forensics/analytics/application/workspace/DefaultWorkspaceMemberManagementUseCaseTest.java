package de.burger.forensics.analytics.application.workspace;

import de.burger.forensics.analytics.application.workspace.command.AddWorkspaceMemberCommand;
import de.burger.forensics.analytics.application.workspace.command.ChangeWorkspaceMemberRoleCommand;
import de.burger.forensics.analytics.application.workspace.command.ListWorkspaceMembersCommand;
import de.burger.forensics.analytics.application.workspace.command.RemoveWorkspaceMemberCommand;
import de.burger.forensics.analytics.application.workspace.port.WorkspaceAuditPort;
import de.burger.forensics.analytics.application.workspace.port.WorkspaceRepository;
import de.burger.forensics.analytics.domain.audit.AuditEvent;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultWorkspaceMemberManagementUseCaseTest {
    private static final WorkspaceId WORKSPACE_ID = new WorkspaceId("workspace-a");
    private static final UserId OWNER_ID = new UserId("owner-a");
    private static final UserId ADMIN_ID = new UserId("admin-a");
    private static final UserId VIEWER_ID = new UserId("viewer-a");
    private static final UserId ANALYST_ID = new UserId("analyst-a");
    private static final UserId OUTSIDER_ID = new UserId("outsider-a");
    private static final Instant NOW = Instant.parse("2026-05-14T11:15:30Z");

    private final RecordingWorkspaceRepository workspaceRepository = new RecordingWorkspaceRepository();
    private final RecordingWorkspaceAuditPort auditPort = new RecordingWorkspaceAuditPort();
    private final DefaultWorkspaceMemberManagementUseCase useCase = new DefaultWorkspaceMemberManagementUseCase(
        workspaceRepository,
        auditPort,
        Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void ownerCanAddWorkspaceMember() {
        activeWorkspaceWithOwner();

        var membership = useCase.add(new AddWorkspaceMemberCommand(
            WORKSPACE_ID,
            ANALYST_ID,
            WorkspaceRole.ANALYST,
            OWNER_ID
        ));

        assertEquals(new WorkspaceMembership(WORKSPACE_ID, ANALYST_ID, WorkspaceRole.ANALYST), membership);
        assertEquals(Optional.of(membership), workspaceRepository.findMembership(WORKSPACE_ID, ANALYST_ID));
        assertEquals("workspace.member.added", auditPort.events.getFirst().action());
        assertEquals("ANALYST", auditPort.events.getFirst().metadata().get("role"));
        assertEquals(NOW, auditPort.events.getFirst().occurredAt());
    }

    @Test
    void adminCanChangeWorkspaceMemberRole() {
        activeWorkspaceWithOwner();
        workspaceRepository.saveMembership(new WorkspaceMembership(WORKSPACE_ID, ADMIN_ID, WorkspaceRole.ADMIN));
        workspaceRepository.saveMembership(new WorkspaceMembership(WORKSPACE_ID, ANALYST_ID, WorkspaceRole.ANALYST));

        var changed = useCase.changeRole(new ChangeWorkspaceMemberRoleCommand(
            WORKSPACE_ID,
            ANALYST_ID,
            WorkspaceRole.REVIEWER,
            ADMIN_ID
        ));

        assertEquals(WorkspaceRole.REVIEWER, changed.role());
        assertEquals("workspace.member.role_changed", auditPort.events.getFirst().action());
        assertEquals("ANALYST", auditPort.events.getFirst().metadata().get("previousRole"));
        assertEquals("REVIEWER", auditPort.events.getFirst().metadata().get("role"));
    }

    @Test
    void adminCanRemoveWorkspaceMember() {
        activeWorkspaceWithOwner();
        workspaceRepository.saveMembership(new WorkspaceMembership(WORKSPACE_ID, ADMIN_ID, WorkspaceRole.ADMIN));
        workspaceRepository.saveMembership(new WorkspaceMembership(WORKSPACE_ID, ANALYST_ID, WorkspaceRole.ANALYST));

        useCase.remove(new RemoveWorkspaceMemberCommand(WORKSPACE_ID, ANALYST_ID, ADMIN_ID));

        assertTrue(workspaceRepository.findMembership(WORKSPACE_ID, ANALYST_ID).isEmpty());
        assertEquals("workspace.member.removed", auditPort.events.getFirst().action());
        assertEquals("ANALYST", auditPort.events.getFirst().metadata().get("role"));
    }

    @Test
    void memberCanListWorkspaceMembersDeterministically() {
        activeWorkspaceWithOwner();
        workspaceRepository.saveMembership(new WorkspaceMembership(WORKSPACE_ID, VIEWER_ID, WorkspaceRole.VIEWER));
        workspaceRepository.saveMembership(new WorkspaceMembership(WORKSPACE_ID, ANALYST_ID, WorkspaceRole.ANALYST));

        var memberships = useCase.list(new ListWorkspaceMembersCommand(WORKSPACE_ID, VIEWER_ID));

        assertEquals(
            List.of(
                new WorkspaceMembership(WORKSPACE_ID, ANALYST_ID, WorkspaceRole.ANALYST),
                new WorkspaceMembership(WORKSPACE_ID, OWNER_ID, WorkspaceRole.OWNER),
                new WorkspaceMembership(WORKSPACE_ID, VIEWER_ID, WorkspaceRole.VIEWER)
            ),
            memberships
        );
    }

    @Test
    void viewerAndOutsiderCannotManageWorkspaceMembers() {
        activeWorkspaceWithOwner();
        workspaceRepository.saveMembership(new WorkspaceMembership(WORKSPACE_ID, VIEWER_ID, WorkspaceRole.VIEWER));

        assertThrows(
            WorkspaceAccessDeniedException.class,
            () -> useCase.add(new AddWorkspaceMemberCommand(WORKSPACE_ID, ANALYST_ID, WorkspaceRole.ANALYST, VIEWER_ID))
        );
        assertThrows(
            WorkspaceAccessDeniedException.class,
            () -> useCase.add(new AddWorkspaceMemberCommand(WORKSPACE_ID, ANALYST_ID, WorkspaceRole.ANALYST, OUTSIDER_ID))
        );
    }

    @Test
    void rejectsDuplicateOrMissingMembers() {
        activeWorkspaceWithOwner();

        assertThrows(
            WorkspaceMembershipAlreadyExistsException.class,
            () -> useCase.add(new AddWorkspaceMemberCommand(WORKSPACE_ID, OWNER_ID, WorkspaceRole.ADMIN, OWNER_ID))
        );
        assertThrows(
            WorkspaceMemberNotFoundException.class,
            () -> useCase.changeRole(new ChangeWorkspaceMemberRoleCommand(WORKSPACE_ID, ANALYST_ID, WorkspaceRole.ANALYST, OWNER_ID))
        );
        assertThrows(
            WorkspaceMemberNotFoundException.class,
            () -> useCase.remove(new RemoveWorkspaceMemberCommand(WORKSPACE_ID, ANALYST_ID, OWNER_ID))
        );
    }

    @Test
    void archivedWorkspaceRejectsMemberChanges() {
        activeWorkspaceWithOwner();
        workspaceRepository.update(Workspace.active(WORKSPACE_ID, "Workspace A").archive());

        assertThrows(
            WorkspaceArchivedException.class,
            () -> useCase.add(new AddWorkspaceMemberCommand(WORKSPACE_ID, ANALYST_ID, WorkspaceRole.ANALYST, OWNER_ID))
        );
    }

    @Test
    void unknownWorkspaceFailsExplicitly() {
        assertThrows(
            WorkspaceNotFoundException.class,
            () -> useCase.add(new AddWorkspaceMemberCommand(WORKSPACE_ID, ANALYST_ID, WorkspaceRole.ANALYST, OWNER_ID))
        );
    }

    @Test
    void dependenciesAndCommandsAreRequired() {
        assertThrows(NullPointerException.class, () -> new DefaultWorkspaceMemberManagementUseCase(null, auditPort, Clock.systemUTC()));
        assertThrows(NullPointerException.class, () -> new DefaultWorkspaceMemberManagementUseCase(workspaceRepository, null, Clock.systemUTC()));
        assertThrows(NullPointerException.class, () -> new DefaultWorkspaceMemberManagementUseCase(workspaceRepository, auditPort, null));
        assertThrows(NullPointerException.class, () -> useCase.add(null));
        assertThrows(NullPointerException.class, () -> useCase.list(null));
        assertThrows(NullPointerException.class, () -> useCase.changeRole(null));
        assertThrows(NullPointerException.class, () -> useCase.remove(null));
        assertThrows(NullPointerException.class, () -> new AddWorkspaceMemberCommand(null, ANALYST_ID, WorkspaceRole.ANALYST, OWNER_ID));
        assertThrows(NullPointerException.class, () -> new AddWorkspaceMemberCommand(WORKSPACE_ID, null, WorkspaceRole.ANALYST, OWNER_ID));
        assertThrows(NullPointerException.class, () -> new AddWorkspaceMemberCommand(WORKSPACE_ID, ANALYST_ID, null, OWNER_ID));
        assertThrows(NullPointerException.class, () -> new AddWorkspaceMemberCommand(WORKSPACE_ID, ANALYST_ID, WorkspaceRole.ANALYST, null));
        assertThrows(NullPointerException.class, () -> new ChangeWorkspaceMemberRoleCommand(null, ANALYST_ID, WorkspaceRole.ANALYST, OWNER_ID));
        assertThrows(NullPointerException.class, () -> new ChangeWorkspaceMemberRoleCommand(WORKSPACE_ID, null, WorkspaceRole.ANALYST, OWNER_ID));
        assertThrows(NullPointerException.class, () -> new ChangeWorkspaceMemberRoleCommand(WORKSPACE_ID, ANALYST_ID, null, OWNER_ID));
        assertThrows(NullPointerException.class, () -> new ChangeWorkspaceMemberRoleCommand(WORKSPACE_ID, ANALYST_ID, WorkspaceRole.ANALYST, null));
        assertThrows(NullPointerException.class, () -> new ListWorkspaceMembersCommand(null, OWNER_ID));
        assertThrows(NullPointerException.class, () -> new ListWorkspaceMembersCommand(WORKSPACE_ID, null));
        assertThrows(NullPointerException.class, () -> new RemoveWorkspaceMemberCommand(null, ANALYST_ID, OWNER_ID));
        assertThrows(NullPointerException.class, () -> new RemoveWorkspaceMemberCommand(WORKSPACE_ID, null, OWNER_ID));
        assertThrows(NullPointerException.class, () -> new RemoveWorkspaceMemberCommand(WORKSPACE_ID, ANALYST_ID, null));
    }

    private void activeWorkspaceWithOwner() {
        workspaceRepository.save(Workspace.active(WORKSPACE_ID, "Workspace A"));
        workspaceRepository.saveMembership(new WorkspaceMembership(WORKSPACE_ID, OWNER_ID, WorkspaceRole.OWNER));
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
            return workspaces.stream()
                .filter(workspace -> workspace.id().equals(workspaceId))
                .findFirst();
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

    private static final class RecordingWorkspaceAuditPort implements WorkspaceAuditPort {
        private final List<AuditEvent> events = new ArrayList<>();

        @Override
        public void publish(AuditEvent event) {
            events.add(event);
        }
    }
}
