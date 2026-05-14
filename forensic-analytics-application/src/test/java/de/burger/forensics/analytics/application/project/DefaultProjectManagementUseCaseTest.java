package de.burger.forensics.analytics.application.project;

import de.burger.forensics.analytics.application.project.command.ArchiveProjectCommand;
import de.burger.forensics.analytics.application.project.command.CreateProjectCommand;
import de.burger.forensics.analytics.application.project.command.GetProjectCommand;
import de.burger.forensics.analytics.application.project.command.ListProjectsCommand;
import de.burger.forensics.analytics.application.project.command.RenameProjectCommand;
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

class DefaultProjectManagementUseCaseTest {
    private static final WorkspaceId WORKSPACE_A = new WorkspaceId("workspace-a");
    private static final WorkspaceId WORKSPACE_B = new WorkspaceId("workspace-b");
    private static final ProjectId PROJECT_A = new ProjectId("project-a");
    private static final ProjectId PROJECT_B = new ProjectId("project-b");
    private static final UserId OWNER_ID = new UserId("owner-a");
    private static final UserId ADMIN_ID = new UserId("admin-a");
    private static final UserId VIEWER_ID = new UserId("viewer-a");
    private static final UserId OUTSIDER_ID = new UserId("outsider-a");
    private static final Instant NOW = Instant.parse("2026-05-14T12:15:30Z");

    private final RecordingWorkspaceRepository workspaceRepository = new RecordingWorkspaceRepository();
    private final RecordingProjectRepository projectRepository = new RecordingProjectRepository();
    private final RecordingProjectMembershipRepository projectMembershipRepository = new RecordingProjectMembershipRepository();
    private final RecordingWorkspaceAuditPort auditPort = new RecordingWorkspaceAuditPort();
    private final DefaultProjectManagementUseCase useCase = new DefaultProjectManagementUseCase(
        workspaceRepository,
        projectRepository,
        projectMembershipRepository,
        auditPort,
        Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void ownerCanCreateProjectInWorkspace() {
        activeWorkspaceWithOwner();

        var project = useCase.create(new CreateProjectCommand(WORKSPACE_A, PROJECT_A, "Project A", OWNER_ID));

        assertEquals(WorkspaceProject.active(PROJECT_A, WORKSPACE_A, "Project A"), project);
        assertEquals(Optional.of(project), projectRepository.findById(PROJECT_A));
        assertEquals("project.created", auditPort.events.getFirst().action());
        assertEquals(PROJECT_A.value(), auditPort.events.getFirst().targetId());
        assertEquals(NOW, auditPort.events.getFirst().occurredAt());
    }

    @Test
    void ownerCanReadAndListProjectsInWorkspace() {
        activeWorkspaceWithOwner();
        var projectA = savedProject(PROJECT_A, WORKSPACE_A, "Project A");
        var projectB = savedProject(PROJECT_B, WORKSPACE_A, "Project B");
        savedProject(new ProjectId("project-c"), WORKSPACE_B, "Project C");

        var read = useCase.get(new GetProjectCommand(WORKSPACE_A, PROJECT_A, OWNER_ID));
        var listed = useCase.list(new ListProjectsCommand(WORKSPACE_A, OWNER_ID));

        assertEquals(projectA, read);
        assertEquals(List.of(projectA, projectB), listed);
    }

    @Test
    void assignedMemberCanReadAndListAssignedProjectOnly() {
        activeWorkspaceWithOwner();
        workspaceRepository.saveMembership(new WorkspaceMembership(WORKSPACE_A, VIEWER_ID, WorkspaceRole.VIEWER));
        var projectA = savedProject(PROJECT_A, WORKSPACE_A, "Project A");
        savedProject(PROJECT_B, WORKSPACE_A, "Project B");
        projectMembershipRepository.save(new ProjectMembership(projectA, VIEWER_ID, WorkspaceRole.VIEWER));

        var read = useCase.get(new GetProjectCommand(WORKSPACE_A, PROJECT_A, VIEWER_ID));
        var listed = useCase.list(new ListProjectsCommand(WORKSPACE_A, VIEWER_ID));

        assertEquals(projectA, read);
        assertEquals(List.of(projectA), listed);
    }

    @Test
    void unassignedMemberCannotReadProject() {
        activeWorkspaceWithOwner();
        workspaceRepository.saveMembership(new WorkspaceMembership(WORKSPACE_A, VIEWER_ID, WorkspaceRole.VIEWER));
        savedProject(PROJECT_A, WORKSPACE_A, "Project A");

        assertThrows(
            WorkspaceAccessDeniedException.class,
            () -> useCase.get(new GetProjectCommand(WORKSPACE_A, PROJECT_A, VIEWER_ID))
        );
    }

    @Test
    void projectReadRejectsCrossWorkspaceId() {
        activeWorkspaceWithOwner();
        workspaceRepository.save(Workspace.active(WORKSPACE_B, "Workspace B"));
        workspaceRepository.saveMembership(new WorkspaceMembership(WORKSPACE_B, OWNER_ID, WorkspaceRole.OWNER));
        savedProject(PROJECT_A, WORKSPACE_B, "Project B");

        assertThrows(
            ProjectNotFoundException.class,
            () -> useCase.get(new GetProjectCommand(WORKSPACE_A, PROJECT_A, OWNER_ID))
        );
    }

    @Test
    void ownerAndAdminCanRenameProject() {
        activeWorkspaceWithOwner();
        workspaceRepository.saveMembership(new WorkspaceMembership(WORKSPACE_A, ADMIN_ID, WorkspaceRole.ADMIN));
        savedProject(PROJECT_A, WORKSPACE_A, "Project A");

        var ownerRenamed = useCase.rename(new RenameProjectCommand(WORKSPACE_A, PROJECT_A, "Owner Name", OWNER_ID));
        var adminRenamed = useCase.rename(new RenameProjectCommand(WORKSPACE_A, PROJECT_A, "Admin Name", ADMIN_ID));

        assertEquals("Owner Name", ownerRenamed.name());
        assertEquals("Admin Name", adminRenamed.name());
        assertEquals("project.updated", auditPort.events.get(0).action());
        assertEquals("project.updated", auditPort.events.get(1).action());
    }

    @Test
    void viewerAndOutsiderCannotCreateOrRenameProjects() {
        activeWorkspaceWithOwner();
        workspaceRepository.saveMembership(new WorkspaceMembership(WORKSPACE_A, VIEWER_ID, WorkspaceRole.VIEWER));
        savedProject(PROJECT_A, WORKSPACE_A, "Project A");

        assertThrows(
            WorkspaceAccessDeniedException.class,
            () -> useCase.create(new CreateProjectCommand(WORKSPACE_A, PROJECT_B, "Denied", VIEWER_ID))
        );
        assertThrows(
            WorkspaceAccessDeniedException.class,
            () -> useCase.rename(new RenameProjectCommand(WORKSPACE_A, PROJECT_A, "Denied", OUTSIDER_ID))
        );
    }

    @Test
    void adminCanArchiveProjectAndArchivedProjectIsReadOnly() {
        activeWorkspaceWithOwner();
        workspaceRepository.saveMembership(new WorkspaceMembership(WORKSPACE_A, ADMIN_ID, WorkspaceRole.ADMIN));
        savedProject(PROJECT_A, WORKSPACE_A, "Project A");

        var archived = useCase.archive(new ArchiveProjectCommand(WORKSPACE_A, PROJECT_A, ADMIN_ID));

        assertEquals("project.archived", auditPort.events.getFirst().action());
        assertEquals(PROJECT_A, archived.id());
        assertThrows(
            ProjectArchivedException.class,
            () -> useCase.rename(new RenameProjectCommand(WORKSPACE_A, PROJECT_A, "Denied", OWNER_ID))
        );
        assertThrows(
            ProjectArchivedException.class,
            () -> useCase.archive(new ArchiveProjectCommand(WORKSPACE_A, PROJECT_A, OWNER_ID))
        );
    }

    @Test
    void archivedWorkspaceRejectsProjectChanges() {
        activeWorkspaceWithOwner();
        workspaceRepository.update(Workspace.active(WORKSPACE_A, "Workspace A").archive());

        assertThrows(
            WorkspaceArchivedException.class,
            () -> useCase.create(new CreateProjectCommand(WORKSPACE_A, PROJECT_A, "Denied", OWNER_ID))
        );
    }

    @Test
    void unknownWorkspaceOrProjectFailsExplicitly() {
        assertThrows(
            WorkspaceNotFoundException.class,
            () -> useCase.create(new CreateProjectCommand(WORKSPACE_A, PROJECT_A, "Project A", OWNER_ID))
        );
        activeWorkspaceWithOwner();
        assertThrows(
            ProjectNotFoundException.class,
            () -> useCase.get(new GetProjectCommand(WORKSPACE_A, PROJECT_A, OWNER_ID))
        );
    }

    @Test
    void dependenciesAndCommandsAreRequired() {
        assertThrows(NullPointerException.class, () -> new DefaultProjectManagementUseCase(null, projectRepository, projectMembershipRepository, auditPort, Clock.systemUTC()));
        assertThrows(NullPointerException.class, () -> new DefaultProjectManagementUseCase(workspaceRepository, null, projectMembershipRepository, auditPort, Clock.systemUTC()));
        assertThrows(NullPointerException.class, () -> new DefaultProjectManagementUseCase(workspaceRepository, projectRepository, null, auditPort, Clock.systemUTC()));
        assertThrows(NullPointerException.class, () -> new DefaultProjectManagementUseCase(workspaceRepository, projectRepository, projectMembershipRepository, null, Clock.systemUTC()));
        assertThrows(NullPointerException.class, () -> new DefaultProjectManagementUseCase(workspaceRepository, projectRepository, projectMembershipRepository, auditPort, null));
        assertThrows(NullPointerException.class, () -> useCase.create(null));
        assertThrows(NullPointerException.class, () -> useCase.get(null));
        assertThrows(NullPointerException.class, () -> useCase.list(null));
        assertThrows(NullPointerException.class, () -> useCase.rename(null));
        assertThrows(NullPointerException.class, () -> useCase.archive(null));
        assertThrows(NullPointerException.class, () -> new CreateProjectCommand(null, PROJECT_A, "Project", OWNER_ID));
        assertThrows(NullPointerException.class, () -> new CreateProjectCommand(WORKSPACE_A, null, "Project", OWNER_ID));
        assertThrows(IllegalArgumentException.class, () -> new CreateProjectCommand(WORKSPACE_A, PROJECT_A, null, OWNER_ID));
        assertThrows(IllegalArgumentException.class, () -> new CreateProjectCommand(WORKSPACE_A, PROJECT_A, " ", OWNER_ID));
        assertThrows(NullPointerException.class, () -> new CreateProjectCommand(WORKSPACE_A, PROJECT_A, "Project", null));
        assertThrows(NullPointerException.class, () -> new GetProjectCommand(null, PROJECT_A, OWNER_ID));
        assertThrows(NullPointerException.class, () -> new GetProjectCommand(WORKSPACE_A, null, OWNER_ID));
        assertThrows(NullPointerException.class, () -> new GetProjectCommand(WORKSPACE_A, PROJECT_A, null));
        assertThrows(NullPointerException.class, () -> new ListProjectsCommand(null, OWNER_ID));
        assertThrows(NullPointerException.class, () -> new ListProjectsCommand(WORKSPACE_A, null));
        assertThrows(NullPointerException.class, () -> new RenameProjectCommand(null, PROJECT_A, "Project", OWNER_ID));
        assertThrows(NullPointerException.class, () -> new RenameProjectCommand(WORKSPACE_A, null, "Project", OWNER_ID));
        assertThrows(IllegalArgumentException.class, () -> new RenameProjectCommand(WORKSPACE_A, PROJECT_A, null, OWNER_ID));
        assertThrows(IllegalArgumentException.class, () -> new RenameProjectCommand(WORKSPACE_A, PROJECT_A, " ", OWNER_ID));
        assertThrows(NullPointerException.class, () -> new RenameProjectCommand(WORKSPACE_A, PROJECT_A, "Project", null));
        assertThrows(NullPointerException.class, () -> new ArchiveProjectCommand(null, PROJECT_A, OWNER_ID));
        assertThrows(NullPointerException.class, () -> new ArchiveProjectCommand(WORKSPACE_A, null, OWNER_ID));
        assertThrows(NullPointerException.class, () -> new ArchiveProjectCommand(WORKSPACE_A, PROJECT_A, null));
    }

    private void activeWorkspaceWithOwner() {
        workspaceRepository.save(Workspace.active(WORKSPACE_A, "Workspace A"));
        workspaceRepository.saveMembership(new WorkspaceMembership(WORKSPACE_A, OWNER_ID, WorkspaceRole.OWNER));
    }

    private WorkspaceProject savedProject(ProjectId projectId, WorkspaceId workspaceId, String name) {
        var project = WorkspaceProject.active(projectId, workspaceId, name);
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
