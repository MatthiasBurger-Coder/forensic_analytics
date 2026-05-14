package de.burger.forensics.analytics.application.project;

import de.burger.forensics.analytics.application.project.command.AddProjectMemberCommand;
import de.burger.forensics.analytics.application.project.command.ChangeProjectMemberRoleCommand;
import de.burger.forensics.analytics.application.project.command.ListProjectMembersCommand;
import de.burger.forensics.analytics.application.project.command.RemoveProjectMemberCommand;
import de.burger.forensics.analytics.application.project.port.ProjectMembershipRepository;
import de.burger.forensics.analytics.application.project.port.ProjectRepository;
import de.burger.forensics.analytics.application.workspace.WorkspaceAccessDeniedException;
import de.burger.forensics.analytics.application.workspace.WorkspaceArchivedException;
import de.burger.forensics.analytics.application.workspace.WorkspaceNotFoundException;
import de.burger.forensics.analytics.application.workspace.port.WorkspaceAuditPort;
import de.burger.forensics.analytics.application.workspace.port.WorkspaceRepository;
import de.burger.forensics.analytics.domain.audit.AuditEvent;
import de.burger.forensics.analytics.domain.workspace.ProjectId;
import de.burger.forensics.analytics.domain.workspace.ProjectMembership;
import de.burger.forensics.analytics.domain.workspace.UserId;
import de.burger.forensics.analytics.domain.workspace.Workspace;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceMembership;
import de.burger.forensics.analytics.domain.workspace.WorkspaceProject;
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

class DefaultProjectMemberManagementUseCaseTest {
    private static final WorkspaceId WORKSPACE_A = new WorkspaceId("workspace-a");
    private static final WorkspaceId WORKSPACE_B = new WorkspaceId("workspace-b");
    private static final ProjectId PROJECT_A = new ProjectId("project-a");
    private static final UserId OWNER_ID = new UserId("owner-a");
    private static final UserId ADMIN_ID = new UserId("admin-a");
    private static final UserId VIEWER_ID = new UserId("viewer-a");
    private static final UserId ANALYST_ID = new UserId("analyst-a");
    private static final UserId OUTSIDER_ID = new UserId("outsider-a");
    private static final Instant NOW = Instant.parse("2026-05-14T13:15:30Z");

    private final RecordingWorkspaceRepository workspaceRepository = new RecordingWorkspaceRepository();
    private final RecordingProjectRepository projectRepository = new RecordingProjectRepository();
    private final RecordingProjectMembershipRepository projectMembershipRepository = new RecordingProjectMembershipRepository();
    private final RecordingWorkspaceAuditPort auditPort = new RecordingWorkspaceAuditPort();
    private final DefaultProjectMemberManagementUseCase useCase = new DefaultProjectMemberManagementUseCase(
        workspaceRepository,
        projectRepository,
        projectMembershipRepository,
        auditPort,
        Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void ownerCanAddProjectMemberWhoBelongsToWorkspace() {
        var project = activeProjectWithOwnerAndMember();

        var membership = useCase.add(new AddProjectMemberCommand(
            WORKSPACE_A,
            PROJECT_A,
            ANALYST_ID,
            WorkspaceRole.ANALYST,
            OWNER_ID
        ));

        assertEquals(new ProjectMembership(project, ANALYST_ID, WorkspaceRole.ANALYST), membership);
        assertEquals(Optional.of(membership), projectMembershipRepository.findMembership(WORKSPACE_A, PROJECT_A, ANALYST_ID));
        assertEquals("project.member.added", auditPort.events.getFirst().action());
        assertEquals("ANALYST", auditPort.events.getFirst().metadata().get("role"));
        assertEquals(NOW, auditPort.events.getFirst().occurredAt());
    }

    @Test
    void projectMemberCanListProjectMembers() {
        var project = activeProjectWithOwnerAndMember();
        projectMembershipRepository.save(new ProjectMembership(project, VIEWER_ID, WorkspaceRole.VIEWER));

        var memberships = useCase.list(new ListProjectMembersCommand(WORKSPACE_A, PROJECT_A, VIEWER_ID));

        assertEquals(
            List.of(new ProjectMembership(project, VIEWER_ID, WorkspaceRole.VIEWER)),
            memberships
        );
    }

    @Test
    void adminCanChangeAndRemoveProjectMember() {
        var project = activeProjectWithOwnerAndMember();
        workspaceRepository.saveMembership(new WorkspaceMembership(WORKSPACE_A, ADMIN_ID, WorkspaceRole.ADMIN));
        projectMembershipRepository.save(new ProjectMembership(project, ANALYST_ID, WorkspaceRole.ANALYST));

        var changed = useCase.changeRole(new ChangeProjectMemberRoleCommand(
            WORKSPACE_A,
            PROJECT_A,
            ANALYST_ID,
            WorkspaceRole.REVIEWER,
            ADMIN_ID
        ));
        useCase.remove(new RemoveProjectMemberCommand(WORKSPACE_A, PROJECT_A, ANALYST_ID, ADMIN_ID));

        assertEquals(WorkspaceRole.REVIEWER, changed.role());
        assertTrue(projectMembershipRepository.findMembership(WORKSPACE_A, PROJECT_A, ANALYST_ID).isEmpty());
        assertEquals("project.member.role_changed", auditPort.events.get(0).action());
        assertEquals("project.member.removed", auditPort.events.get(1).action());
    }

    @Test
    void viewerAndOutsiderCannotManageProjectMembers() {
        activeProjectWithOwnerAndMember();

        assertThrows(
            WorkspaceAccessDeniedException.class,
            () -> useCase.add(new AddProjectMemberCommand(WORKSPACE_A, PROJECT_A, ANALYST_ID, WorkspaceRole.ANALYST, VIEWER_ID))
        );
        assertThrows(
            WorkspaceAccessDeniedException.class,
            () -> useCase.add(new AddProjectMemberCommand(WORKSPACE_A, PROJECT_A, ANALYST_ID, WorkspaceRole.ANALYST, OUTSIDER_ID))
        );
    }

    @Test
    void userFromOtherWorkspaceCannotBeAddedToProject() {
        activeProjectWithOwnerAndMember();
        workspaceRepository.save(Workspace.active(WORKSPACE_B, "Workspace B"));
        workspaceRepository.saveMembership(new WorkspaceMembership(WORKSPACE_B, OUTSIDER_ID, WorkspaceRole.ANALYST));

        assertThrows(
            WorkspaceAccessDeniedException.class,
            () -> useCase.add(new AddProjectMemberCommand(WORKSPACE_A, PROJECT_A, OUTSIDER_ID, WorkspaceRole.ANALYST, OWNER_ID))
        );
    }

    @Test
    void rejectsDuplicateOrMissingProjectMembers() {
        var project = activeProjectWithOwnerAndMember();
        projectMembershipRepository.save(new ProjectMembership(project, ANALYST_ID, WorkspaceRole.ANALYST));

        assertThrows(
            ProjectMembershipAlreadyExistsException.class,
            () -> useCase.add(new AddProjectMemberCommand(WORKSPACE_A, PROJECT_A, ANALYST_ID, WorkspaceRole.ANALYST, OWNER_ID))
        );
        assertThrows(
            ProjectMemberNotFoundException.class,
            () -> useCase.changeRole(new ChangeProjectMemberRoleCommand(WORKSPACE_A, PROJECT_A, OUTSIDER_ID, WorkspaceRole.VIEWER, OWNER_ID))
        );
        assertThrows(
            ProjectMemberNotFoundException.class,
            () -> useCase.remove(new RemoveProjectMemberCommand(WORKSPACE_A, PROJECT_A, OUTSIDER_ID, OWNER_ID))
        );
    }

    @Test
    void archivedWorkspaceOrProjectRejectsProjectMemberChanges() {
        var project = activeProjectWithOwnerAndMember();
        projectRepository.update(project.archive());

        assertThrows(
            ProjectArchivedException.class,
            () -> useCase.add(new AddProjectMemberCommand(WORKSPACE_A, PROJECT_A, ANALYST_ID, WorkspaceRole.ANALYST, OWNER_ID))
        );

        projectRepository.update(project);
        workspaceRepository.update(Workspace.active(WORKSPACE_A, "Workspace A").archive());

        assertThrows(
            WorkspaceArchivedException.class,
            () -> useCase.add(new AddProjectMemberCommand(WORKSPACE_A, PROJECT_A, ANALYST_ID, WorkspaceRole.ANALYST, OWNER_ID))
        );
    }

    @Test
    void unknownWorkspaceOrProjectFailsExplicitly() {
        assertThrows(
            WorkspaceNotFoundException.class,
            () -> useCase.add(new AddProjectMemberCommand(WORKSPACE_A, PROJECT_A, ANALYST_ID, WorkspaceRole.ANALYST, OWNER_ID))
        );

        workspaceRepository.save(Workspace.active(WORKSPACE_A, "Workspace A"));
        workspaceRepository.saveMembership(new WorkspaceMembership(WORKSPACE_A, OWNER_ID, WorkspaceRole.OWNER));

        assertThrows(
            ProjectNotFoundException.class,
            () -> useCase.add(new AddProjectMemberCommand(WORKSPACE_A, PROJECT_A, ANALYST_ID, WorkspaceRole.ANALYST, OWNER_ID))
        );
    }

    @Test
    void dependenciesAndCommandsAreRequired() {
        assertThrows(NullPointerException.class, () -> new DefaultProjectMemberManagementUseCase(null, projectRepository, projectMembershipRepository, auditPort, Clock.systemUTC()));
        assertThrows(NullPointerException.class, () -> new DefaultProjectMemberManagementUseCase(workspaceRepository, null, projectMembershipRepository, auditPort, Clock.systemUTC()));
        assertThrows(NullPointerException.class, () -> new DefaultProjectMemberManagementUseCase(workspaceRepository, projectRepository, null, auditPort, Clock.systemUTC()));
        assertThrows(NullPointerException.class, () -> new DefaultProjectMemberManagementUseCase(workspaceRepository, projectRepository, projectMembershipRepository, null, Clock.systemUTC()));
        assertThrows(NullPointerException.class, () -> new DefaultProjectMemberManagementUseCase(workspaceRepository, projectRepository, projectMembershipRepository, auditPort, null));
        assertThrows(NullPointerException.class, () -> useCase.add(null));
        assertThrows(NullPointerException.class, () -> useCase.list(null));
        assertThrows(NullPointerException.class, () -> useCase.changeRole(null));
        assertThrows(NullPointerException.class, () -> useCase.remove(null));
        assertThrows(NullPointerException.class, () -> new AddProjectMemberCommand(null, PROJECT_A, ANALYST_ID, WorkspaceRole.ANALYST, OWNER_ID));
        assertThrows(NullPointerException.class, () -> new AddProjectMemberCommand(WORKSPACE_A, null, ANALYST_ID, WorkspaceRole.ANALYST, OWNER_ID));
        assertThrows(NullPointerException.class, () -> new AddProjectMemberCommand(WORKSPACE_A, PROJECT_A, null, WorkspaceRole.ANALYST, OWNER_ID));
        assertThrows(NullPointerException.class, () -> new AddProjectMemberCommand(WORKSPACE_A, PROJECT_A, ANALYST_ID, null, OWNER_ID));
        assertThrows(NullPointerException.class, () -> new AddProjectMemberCommand(WORKSPACE_A, PROJECT_A, ANALYST_ID, WorkspaceRole.ANALYST, null));
        assertThrows(NullPointerException.class, () -> new ListProjectMembersCommand(null, PROJECT_A, OWNER_ID));
        assertThrows(NullPointerException.class, () -> new ListProjectMembersCommand(WORKSPACE_A, null, OWNER_ID));
        assertThrows(NullPointerException.class, () -> new ListProjectMembersCommand(WORKSPACE_A, PROJECT_A, null));
        assertThrows(NullPointerException.class, () -> new ChangeProjectMemberRoleCommand(null, PROJECT_A, ANALYST_ID, WorkspaceRole.ANALYST, OWNER_ID));
        assertThrows(NullPointerException.class, () -> new ChangeProjectMemberRoleCommand(WORKSPACE_A, null, ANALYST_ID, WorkspaceRole.ANALYST, OWNER_ID));
        assertThrows(NullPointerException.class, () -> new ChangeProjectMemberRoleCommand(WORKSPACE_A, PROJECT_A, null, WorkspaceRole.ANALYST, OWNER_ID));
        assertThrows(NullPointerException.class, () -> new ChangeProjectMemberRoleCommand(WORKSPACE_A, PROJECT_A, ANALYST_ID, null, OWNER_ID));
        assertThrows(NullPointerException.class, () -> new ChangeProjectMemberRoleCommand(WORKSPACE_A, PROJECT_A, ANALYST_ID, WorkspaceRole.ANALYST, null));
        assertThrows(NullPointerException.class, () -> new RemoveProjectMemberCommand(null, PROJECT_A, ANALYST_ID, OWNER_ID));
        assertThrows(NullPointerException.class, () -> new RemoveProjectMemberCommand(WORKSPACE_A, null, ANALYST_ID, OWNER_ID));
        assertThrows(NullPointerException.class, () -> new RemoveProjectMemberCommand(WORKSPACE_A, PROJECT_A, null, OWNER_ID));
        assertThrows(NullPointerException.class, () -> new RemoveProjectMemberCommand(WORKSPACE_A, PROJECT_A, ANALYST_ID, null));
    }

    private WorkspaceProject activeProjectWithOwnerAndMember() {
        workspaceRepository.save(Workspace.active(WORKSPACE_A, "Workspace A"));
        workspaceRepository.saveMembership(new WorkspaceMembership(WORKSPACE_A, OWNER_ID, WorkspaceRole.OWNER));
        workspaceRepository.saveMembership(new WorkspaceMembership(WORKSPACE_A, VIEWER_ID, WorkspaceRole.VIEWER));
        workspaceRepository.saveMembership(new WorkspaceMembership(WORKSPACE_A, ANALYST_ID, WorkspaceRole.ANALYST));
        var project = WorkspaceProject.active(PROJECT_A, WORKSPACE_A, "Project A");
        projectRepository.save(project);
        return project;
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

    private static final class RecordingProjectRepository implements ProjectRepository {
        private final List<WorkspaceProject> projects = new ArrayList<>();

        @Override
        public void save(WorkspaceProject project) {
            update(project);
        }

        @Override
        public void update(WorkspaceProject project) {
            projects.removeIf(existing -> existing.id().equals(project.id()));
            projects.add(project);
        }

        @Override
        public Optional<WorkspaceProject> findById(ProjectId projectId) {
            return projects.stream()
                .filter(project -> project.id().equals(projectId))
                .findFirst();
        }

        @Override
        public List<WorkspaceProject> findByWorkspace(WorkspaceId workspaceId) {
            return projects.stream()
                .filter(project -> project.workspaceId().equals(workspaceId))
                .sorted(Comparator.comparing(project -> project.id().value()))
                .toList();
        }
    }

    private static final class RecordingProjectMembershipRepository implements ProjectMembershipRepository {
        private final List<ProjectMembership> memberships = new ArrayList<>();

        @Override
        public void save(ProjectMembership membership) {
            removeMembership(membership.workspaceId(), membership.projectId(), membership.userId());
            memberships.add(membership);
        }

        @Override
        public Optional<ProjectMembership> findMembership(WorkspaceId workspaceId, ProjectId projectId, UserId userId) {
            return memberships.stream()
                .filter(membership -> membership.workspaceId().equals(workspaceId))
                .filter(membership -> membership.projectId().equals(projectId))
                .filter(membership -> membership.userId().equals(userId))
                .findFirst();
        }

        @Override
        public List<ProjectMembership> findByProject(WorkspaceId workspaceId, ProjectId projectId) {
            return memberships.stream()
                .filter(membership -> membership.workspaceId().equals(workspaceId))
                .filter(membership -> membership.projectId().equals(projectId))
                .sorted(Comparator.comparing(membership -> membership.userId().value()))
                .toList();
        }

        @Override
        public List<ProjectMembership> findByUser(UserId userId) {
            return memberships.stream()
                .filter(membership -> membership.userId().equals(userId))
                .sorted(Comparator
                    .comparing((ProjectMembership membership) -> membership.workspaceId().value())
                    .thenComparing(membership -> membership.projectId().value()))
                .toList();
        }

        @Override
        public void removeMembership(WorkspaceId workspaceId, ProjectId projectId, UserId userId) {
            memberships.removeIf(membership ->
                membership.workspaceId().equals(workspaceId)
                    && membership.projectId().equals(projectId)
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
