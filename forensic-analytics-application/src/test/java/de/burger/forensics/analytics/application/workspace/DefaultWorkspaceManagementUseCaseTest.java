package de.burger.forensics.analytics.application.workspace;

import de.burger.forensics.analytics.application.workspace.command.ArchiveWorkspaceCommand;
import de.burger.forensics.analytics.application.workspace.command.CreateWorkspaceCommand;
import de.burger.forensics.analytics.application.workspace.command.GetWorkspaceCommand;
import de.burger.forensics.analytics.application.workspace.command.ListWorkspacesCommand;
import de.burger.forensics.analytics.application.workspace.command.RenameWorkspaceCommand;
import de.burger.forensics.analytics.application.workspace.port.WorkspaceAuditPort;
import de.burger.forensics.analytics.application.workspace.port.WorkspaceRepository;
import de.burger.forensics.analytics.domain.audit.AuditEvent;
import de.burger.forensics.analytics.domain.workspace.UserId;
import de.burger.forensics.analytics.domain.workspace.Workspace;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceMembership;
import de.burger.forensics.analytics.domain.workspace.WorkspaceRole;
import de.burger.forensics.analytics.domain.workspace.WorkspaceStatus;
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

class DefaultWorkspaceManagementUseCaseTest {
    private static final WorkspaceId WORKSPACE_ID = new WorkspaceId("workspace-a");
    private static final UserId OWNER_ID = new UserId("owner-a");
    private static final UserId ADMIN_ID = new UserId("admin-a");
    private static final UserId VIEWER_ID = new UserId("viewer-a");
    private static final UserId OUTSIDER_ID = new UserId("outsider-a");
    private static final Instant NOW = Instant.parse("2026-05-14T10:15:30Z");

    private final RecordingWorkspaceRepository workspaceRepository = new RecordingWorkspaceRepository();
    private final RecordingWorkspaceAuditPort auditPort = new RecordingWorkspaceAuditPort();
    private final DefaultWorkspaceManagementUseCase useCase = new DefaultWorkspaceManagementUseCase(
        workspaceRepository,
        auditPort,
        Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void createWorkspaceStoresOwnerMembershipAndAuditEvent() {
        var workspace = useCase.create(new CreateWorkspaceCommand(WORKSPACE_ID, "Workspace A", OWNER_ID));

        assertEquals(Workspace.active(WORKSPACE_ID, "Workspace A"), workspace);
        assertEquals(Optional.of(workspace), workspaceRepository.findById(WORKSPACE_ID));
        assertEquals(
            Optional.of(new WorkspaceMembership(WORKSPACE_ID, OWNER_ID, WorkspaceRole.OWNER)),
            workspaceRepository.findMembership(WORKSPACE_ID, OWNER_ID)
        );
        assertEquals("workspace.created", auditPort.events.getFirst().action());
        assertEquals(WORKSPACE_ID, auditPort.events.getFirst().workspaceId());
        assertEquals(OWNER_ID, auditPort.events.getFirst().actorUserId());
        assertEquals(NOW, auditPort.events.getFirst().occurredAt());
    }

    @Test
    void memberCanReadAndListWorkspace() {
        useCase.create(new CreateWorkspaceCommand(WORKSPACE_ID, "Workspace A", OWNER_ID));
        workspaceRepository.saveMembership(new WorkspaceMembership(WORKSPACE_ID, VIEWER_ID, WorkspaceRole.VIEWER));

        var read = useCase.get(new GetWorkspaceCommand(WORKSPACE_ID, VIEWER_ID));
        var listed = useCase.list(new ListWorkspacesCommand(VIEWER_ID));

        assertEquals(WORKSPACE_ID, read.id());
        assertEquals(List.of(read), listed);
    }

    @Test
    void outsiderCannotReadWorkspace() {
        useCase.create(new CreateWorkspaceCommand(WORKSPACE_ID, "Workspace A", OWNER_ID));

        assertThrows(
            WorkspaceAccessDeniedException.class,
            () -> useCase.get(new GetWorkspaceCommand(WORKSPACE_ID, OUTSIDER_ID))
        );
    }

    @Test
    void ownerAndAdminCanRenameWorkspace() {
        useCase.create(new CreateWorkspaceCommand(WORKSPACE_ID, "Workspace A", OWNER_ID));
        workspaceRepository.saveMembership(new WorkspaceMembership(WORKSPACE_ID, ADMIN_ID, WorkspaceRole.ADMIN));

        var ownerRenamed = useCase.rename(new RenameWorkspaceCommand(WORKSPACE_ID, "Owner Name", OWNER_ID));
        var adminRenamed = useCase.rename(new RenameWorkspaceCommand(WORKSPACE_ID, "Admin Name", ADMIN_ID));

        assertEquals("Owner Name", ownerRenamed.name());
        assertEquals("Admin Name", adminRenamed.name());
        assertEquals("workspace.updated", auditPort.events.get(1).action());
        assertEquals("workspace.updated", auditPort.events.get(2).action());
    }

    @Test
    void viewerCannotRenameWorkspace() {
        useCase.create(new CreateWorkspaceCommand(WORKSPACE_ID, "Workspace A", OWNER_ID));
        workspaceRepository.saveMembership(new WorkspaceMembership(WORKSPACE_ID, VIEWER_ID, WorkspaceRole.VIEWER));

        assertThrows(
            WorkspaceAccessDeniedException.class,
            () -> useCase.rename(new RenameWorkspaceCommand(WORKSPACE_ID, "Denied", VIEWER_ID))
        );
    }

    @Test
    void adminCanArchiveWorkspaceAndArchivedWorkspaceIsReadOnly() {
        useCase.create(new CreateWorkspaceCommand(WORKSPACE_ID, "Workspace A", OWNER_ID));
        workspaceRepository.saveMembership(new WorkspaceMembership(WORKSPACE_ID, ADMIN_ID, WorkspaceRole.ADMIN));

        var archived = useCase.archive(new ArchiveWorkspaceCommand(WORKSPACE_ID, ADMIN_ID));

        assertEquals(WorkspaceStatus.ARCHIVED, archived.status());
        assertEquals("workspace.archived", auditPort.events.get(1).action());
        assertThrows(
            WorkspaceArchivedException.class,
            () -> useCase.rename(new RenameWorkspaceCommand(WORKSPACE_ID, "Denied", OWNER_ID))
        );
        assertThrows(
            WorkspaceArchivedException.class,
            () -> useCase.archive(new ArchiveWorkspaceCommand(WORKSPACE_ID, OWNER_ID))
        );
    }

    @Test
    void unknownWorkspaceFailsExplicitly() {
        assertThrows(
            WorkspaceNotFoundException.class,
            () -> useCase.get(new GetWorkspaceCommand(WORKSPACE_ID, OWNER_ID))
        );
    }

    @Test
    void dependenciesAndCommandsAreRequired() {
        assertThrows(NullPointerException.class, () -> new DefaultWorkspaceManagementUseCase(null, auditPort, Clock.systemUTC()));
        assertThrows(NullPointerException.class, () -> new DefaultWorkspaceManagementUseCase(workspaceRepository, null, Clock.systemUTC()));
        assertThrows(NullPointerException.class, () -> new DefaultWorkspaceManagementUseCase(workspaceRepository, auditPort, null));
        assertThrows(NullPointerException.class, () -> useCase.create(null));
        assertThrows(NullPointerException.class, () -> useCase.get(null));
        assertThrows(NullPointerException.class, () -> useCase.list(null));
        assertThrows(NullPointerException.class, () -> useCase.rename(null));
        assertThrows(NullPointerException.class, () -> useCase.archive(null));
        assertThrows(NullPointerException.class, () -> new CreateWorkspaceCommand(null, "Workspace", OWNER_ID));
        assertThrows(IllegalArgumentException.class, () -> new CreateWorkspaceCommand(WORKSPACE_ID, null, OWNER_ID));
        assertThrows(IllegalArgumentException.class, () -> new CreateWorkspaceCommand(WORKSPACE_ID, " ", OWNER_ID));
        assertThrows(NullPointerException.class, () -> new CreateWorkspaceCommand(WORKSPACE_ID, "Workspace", null));
        assertThrows(NullPointerException.class, () -> new GetWorkspaceCommand(null, OWNER_ID));
        assertThrows(NullPointerException.class, () -> new GetWorkspaceCommand(WORKSPACE_ID, null));
        assertThrows(NullPointerException.class, () -> new ListWorkspacesCommand(null));
        assertThrows(NullPointerException.class, () -> new RenameWorkspaceCommand(null, "Workspace", OWNER_ID));
        assertThrows(IllegalArgumentException.class, () -> new RenameWorkspaceCommand(WORKSPACE_ID, null, OWNER_ID));
        assertThrows(IllegalArgumentException.class, () -> new RenameWorkspaceCommand(WORKSPACE_ID, " ", OWNER_ID));
        assertThrows(NullPointerException.class, () -> new RenameWorkspaceCommand(WORKSPACE_ID, "Workspace", null));
        assertThrows(NullPointerException.class, () -> new ArchiveWorkspaceCommand(null, OWNER_ID));
        assertThrows(NullPointerException.class, () -> new ArchiveWorkspaceCommand(WORKSPACE_ID, null));
    }

    @Test
    void auditEventKeepsStableMetadata() {
        var event = new AuditEvent(
            WORKSPACE_ID,
            OWNER_ID,
            "workspace.created",
            "workspace",
            WORKSPACE_ID.value(),
            NOW,
            java.util.Map.of("status", "ACTIVE")
        );

        assertEquals(java.util.Map.of("status", "ACTIVE"), event.metadata());
        assertThrows(UnsupportedOperationException.class, () -> event.metadata().put("x", "y"));
        assertThrows(NullPointerException.class, () -> new AuditEvent(null, OWNER_ID, "action", "target", "id", NOW, java.util.Map.of()));
        assertThrows(NullPointerException.class, () -> new AuditEvent(WORKSPACE_ID, null, "action", "target", "id", NOW, java.util.Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new AuditEvent(WORKSPACE_ID, OWNER_ID, " ", "target", "id", NOW, java.util.Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new AuditEvent(WORKSPACE_ID, OWNER_ID, "action", " ", "id", NOW, java.util.Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new AuditEvent(WORKSPACE_ID, OWNER_ID, "action", "target", " ", NOW, java.util.Map.of()));
        assertThrows(NullPointerException.class, () -> new AuditEvent(WORKSPACE_ID, OWNER_ID, "action", "target", "id", null, java.util.Map.of()));
        assertThrows(NullPointerException.class, () -> new AuditEvent(WORKSPACE_ID, OWNER_ID, "action", "target", "id", NOW, null));
        assertThrows(IllegalArgumentException.class, () -> new AuditEvent(WORKSPACE_ID, OWNER_ID, "action", "target", "id", NOW, java.util.Map.of(" ", "value")));
        assertThrows(IllegalArgumentException.class, () -> new AuditEvent(WORKSPACE_ID, OWNER_ID, "action", "target", "id", NOW, java.util.Map.of("key", " ")));
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
            memberships.removeIf(existing ->
                existing.workspaceId().equals(membership.workspaceId())
                    && existing.userId().equals(membership.userId())
            );
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
