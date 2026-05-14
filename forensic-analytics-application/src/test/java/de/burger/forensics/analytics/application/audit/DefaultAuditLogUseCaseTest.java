package de.burger.forensics.analytics.application.audit;

import de.burger.forensics.analytics.application.audit.command.ListWorkspaceAuditEventsCommand;
import de.burger.forensics.analytics.application.audit.port.AuditEventRepository;
import de.burger.forensics.analytics.application.workspace.WorkspaceAccessDeniedException;
import de.burger.forensics.analytics.application.workspace.WorkspaceNotFoundException;
import de.burger.forensics.analytics.application.workspace.port.WorkspaceRepository;
import de.burger.forensics.analytics.domain.audit.AuditEvent;
import de.burger.forensics.analytics.domain.workspace.UserId;
import de.burger.forensics.analytics.domain.workspace.Workspace;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceMembership;
import de.burger.forensics.analytics.domain.workspace.WorkspaceRole;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultAuditLogUseCaseTest {
    private static final WorkspaceId WORKSPACE_A = new WorkspaceId("workspace-a");
    private static final WorkspaceId WORKSPACE_B = new WorkspaceId("workspace-b");
    private static final UserId OWNER_ID = new UserId("owner-a");
    private static final UserId ADMIN_ID = new UserId("admin-a");
    private static final UserId AUDITOR_ID = new UserId("auditor-a");
    private static final UserId VIEWER_ID = new UserId("viewer-a");
    private static final UserId OUTSIDER_ID = new UserId("outsider-a");

    private final RecordingWorkspaceRepository workspaceRepository = new RecordingWorkspaceRepository();
    private final RecordingAuditEventRepository auditEventRepository = new RecordingAuditEventRepository();
    private final DefaultAuditLogUseCase useCase = new DefaultAuditLogUseCase(workspaceRepository, auditEventRepository);

    @Test
    void ownerAdminAndAuditorCanReadWorkspaceAuditLog() {
        activeWorkspaceWithAuditReaders();
        var event = event(WORKSPACE_A, "workspace.created", "workspace", "workspace-a", "2026-05-14T10:00:00Z");
        auditEventRepository.append(event);

        assertEquals(List.of(event), useCase.listWorkspaceEvents(new ListWorkspaceAuditEventsCommand(WORKSPACE_A, OWNER_ID)));
        assertEquals(List.of(event), useCase.listWorkspaceEvents(new ListWorkspaceAuditEventsCommand(WORKSPACE_A, ADMIN_ID)));
        assertEquals(List.of(event), useCase.listWorkspaceEvents(new ListWorkspaceAuditEventsCommand(WORKSPACE_A, AUDITOR_ID)));
    }

    @Test
    void auditLogReadIsDeterministicAndWorkspaceScoped() {
        activeWorkspaceWithAuditReaders();
        var later = event(WORKSPACE_A, "workspace.updated", "workspace", "workspace-a", "2026-05-14T10:01:00Z");
        var earlier = event(WORKSPACE_A, "project.created", "project", "project-a", "2026-05-14T10:00:00Z");
        var sameTime = event(WORKSPACE_A, "project.archived", "project", "project-a", "2026-05-14T10:00:00Z");
        auditEventRepository.append(later);
        auditEventRepository.append(event(WORKSPACE_B, "workspace.created", "workspace", "workspace-b", "2026-05-14T09:00:00Z"));
        auditEventRepository.append(sameTime);
        auditEventRepository.append(earlier);

        var events = useCase.listWorkspaceEvents(new ListWorkspaceAuditEventsCommand(WORKSPACE_A, AUDITOR_ID));

        assertEquals(List.of(sameTime, earlier, later), events);
    }

    @Test
    void viewerAndOutsiderCannotReadAuditLog() {
        activeWorkspaceWithAuditReaders();

        assertThrows(
            WorkspaceAccessDeniedException.class,
            () -> useCase.listWorkspaceEvents(new ListWorkspaceAuditEventsCommand(WORKSPACE_A, VIEWER_ID))
        );
        assertThrows(
            WorkspaceAccessDeniedException.class,
            () -> useCase.listWorkspaceEvents(new ListWorkspaceAuditEventsCommand(WORKSPACE_A, OUTSIDER_ID))
        );
    }

    @Test
    void unknownWorkspaceFailsExplicitly() {
        assertThrows(
            WorkspaceNotFoundException.class,
            () -> useCase.listWorkspaceEvents(new ListWorkspaceAuditEventsCommand(WORKSPACE_A, OWNER_ID))
        );
    }

    @Test
    void dependenciesAndCommandsAreRequired() {
        assertThrows(NullPointerException.class, () -> new DefaultAuditLogUseCase(null, auditEventRepository));
        assertThrows(NullPointerException.class, () -> new DefaultAuditLogUseCase(workspaceRepository, null));
        assertThrows(NullPointerException.class, () -> useCase.listWorkspaceEvents(null));
        assertThrows(NullPointerException.class, () -> new ListWorkspaceAuditEventsCommand(null, OWNER_ID));
        assertThrows(NullPointerException.class, () -> new ListWorkspaceAuditEventsCommand(WORKSPACE_A, null));
    }

    private void activeWorkspaceWithAuditReaders() {
        workspaceRepository.save(Workspace.active(WORKSPACE_A, "Workspace A"));
        workspaceRepository.saveMembership(new WorkspaceMembership(WORKSPACE_A, OWNER_ID, WorkspaceRole.OWNER));
        workspaceRepository.saveMembership(new WorkspaceMembership(WORKSPACE_A, ADMIN_ID, WorkspaceRole.ADMIN));
        workspaceRepository.saveMembership(new WorkspaceMembership(WORKSPACE_A, AUDITOR_ID, WorkspaceRole.AUDITOR));
        workspaceRepository.saveMembership(new WorkspaceMembership(WORKSPACE_A, VIEWER_ID, WorkspaceRole.VIEWER));
    }

    private static AuditEvent event(
        WorkspaceId workspaceId,
        String action,
        String targetType,
        String targetId,
        String occurredAt
    ) {
        return new AuditEvent(
            workspaceId,
            OWNER_ID,
            action,
            targetType,
            targetId,
            Instant.parse(occurredAt),
            Map.of()
        );
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

    private static final class RecordingAuditEventRepository implements AuditEventRepository {
        private final List<AuditEvent> events = new ArrayList<>();

        @Override
        public void append(AuditEvent event) {
            events.add(event);
        }

        @Override
        public List<AuditEvent> findByWorkspace(WorkspaceId workspaceId) {
            return events.stream()
                .filter(event -> event.workspaceId().equals(workspaceId))
                .sorted(Comparator
                    .comparing(AuditEvent::occurredAt)
                    .thenComparing(AuditEvent::action)
                    .thenComparing(AuditEvent::targetType)
                    .thenComparing(AuditEvent::targetId))
                .toList();
        }
    }
}
