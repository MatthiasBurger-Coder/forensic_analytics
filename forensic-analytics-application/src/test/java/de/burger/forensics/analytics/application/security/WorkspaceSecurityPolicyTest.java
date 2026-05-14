package de.burger.forensics.analytics.application.security;

import de.burger.forensics.analytics.application.project.ProjectNotFoundException;
import de.burger.forensics.analytics.application.project.port.ProjectMembershipRepository;
import de.burger.forensics.analytics.application.project.port.ProjectRepository;
import de.burger.forensics.analytics.application.workspace.WorkspaceAccessDeniedException;
import de.burger.forensics.analytics.application.workspace.WorkspaceNotFoundException;
import de.burger.forensics.analytics.application.workspace.port.WorkspaceRepository;
import de.burger.forensics.analytics.domain.workspace.ProjectId;
import de.burger.forensics.analytics.domain.workspace.ProjectMembership;
import de.burger.forensics.analytics.domain.workspace.UserId;
import de.burger.forensics.analytics.domain.workspace.Workspace;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceMembership;
import de.burger.forensics.analytics.domain.workspace.WorkspacePermission;
import de.burger.forensics.analytics.domain.workspace.WorkspaceProject;
import de.burger.forensics.analytics.domain.workspace.WorkspaceRole;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorkspaceSecurityPolicyTest {
    private static final WorkspaceId WORKSPACE_A = new WorkspaceId("workspace-a");
    private static final WorkspaceId WORKSPACE_B = new WorkspaceId("workspace-b");
    private static final ProjectId PROJECT_A = new ProjectId("project-a");
    private static final UserId OWNER_ID = new UserId("owner-a");
    private static final UserId VIEWER_ID = new UserId("viewer-a");
    private static final UserId ANALYST_ID = new UserId("analyst-a");
    private static final UserId OUTSIDER_ID = new UserId("outsider-a");

    private final RecordingWorkspaceRepository workspaceRepository = new RecordingWorkspaceRepository();
    private final RecordingProjectRepository projectRepository = new RecordingProjectRepository();
    private final RecordingProjectMembershipRepository projectMembershipRepository = new RecordingProjectMembershipRepository();
    private final WorkspaceSecurityPolicy policy = new WorkspaceSecurityPolicy(
        workspaceRepository,
        projectRepository,
        projectMembershipRepository
    );

    @Test
    void requiresWorkspaceMembershipAndPermission() {
        setupWorkspace();

        assertEquals(
            new WorkspaceMembership(WORKSPACE_A, OWNER_ID, WorkspaceRole.OWNER),
            policy.requireWorkspacePermission(WORKSPACE_A, OWNER_ID, WorkspacePermission.UPDATE_WORKSPACE)
        );
        assertThrows(
            WorkspaceAccessDeniedException.class,
            () -> policy.requireWorkspacePermission(WORKSPACE_A, VIEWER_ID, WorkspacePermission.UPDATE_WORKSPACE)
        );
        assertThrows(
            WorkspaceAccessDeniedException.class,
            () -> policy.requireWorkspacePermission(WORKSPACE_A, OUTSIDER_ID, WorkspacePermission.READ_WORKSPACE)
        );
    }

    @Test
    void ownerCanAccessAnyProjectInWorkspace() {
        var project = setupProject();

        assertEquals(project, policy.requireProjectAccess(WORKSPACE_A, PROJECT_A, OWNER_ID));
    }

    @Test
    void assignedProjectMemberCanAccessProject() {
        var project = setupProject();
        projectMembershipRepository.save(new ProjectMembership(project, ANALYST_ID, WorkspaceRole.ANALYST));

        assertEquals(project, policy.requireProjectAccess(WORKSPACE_A, PROJECT_A, ANALYST_ID));
    }

    @Test
    void unassignedWorkspaceMemberCannotAccessProject() {
        setupProject();

        assertThrows(
            WorkspaceAccessDeniedException.class,
            () -> policy.requireProjectAccess(WORKSPACE_A, PROJECT_A, VIEWER_ID)
        );
    }

    @Test
    void crossWorkspaceProjectIdIsRejectedAsNotFound() {
        setupWorkspace();
        workspaceRepository.save(Workspace.active(WORKSPACE_B, "Workspace B"));
        workspaceRepository.saveMembership(new WorkspaceMembership(WORKSPACE_B, OWNER_ID, WorkspaceRole.OWNER));
        projectRepository.save(WorkspaceProject.active(PROJECT_A, WORKSPACE_B, "Project B"));

        assertThrows(
            ProjectNotFoundException.class,
            () -> policy.requireProjectInWorkspace(WORKSPACE_A, PROJECT_A)
        );
    }

    @Test
    void unknownWorkspaceFailsExplicitly() {
        assertThrows(
            WorkspaceNotFoundException.class,
            () -> policy.requireWorkspaceMembership(WORKSPACE_A, OWNER_ID)
        );
    }

    @Test
    void dependenciesAndArgumentsAreRequired() {
        setupWorkspace();

        assertThrows(NullPointerException.class, () -> new WorkspaceSecurityPolicy(null, projectRepository, projectMembershipRepository));
        assertThrows(NullPointerException.class, () -> new WorkspaceSecurityPolicy(workspaceRepository, null, projectMembershipRepository));
        assertThrows(NullPointerException.class, () -> new WorkspaceSecurityPolicy(workspaceRepository, projectRepository, null));
        assertThrows(NullPointerException.class, () -> policy.requireWorkspacePermission(null, OWNER_ID, WorkspacePermission.READ_WORKSPACE));
        assertThrows(NullPointerException.class, () -> policy.requireWorkspacePermission(WORKSPACE_A, null, WorkspacePermission.READ_WORKSPACE));
        assertThrows(NullPointerException.class, () -> policy.requireWorkspacePermission(WORKSPACE_A, OWNER_ID, null));
        assertThrows(NullPointerException.class, () -> policy.requireProjectInWorkspace(null, PROJECT_A));
        assertThrows(NullPointerException.class, () -> policy.requireProjectInWorkspace(WORKSPACE_A, null));
        assertThrows(NullPointerException.class, () -> policy.requireWorkspaceMembership(null, OWNER_ID));
        assertThrows(NullPointerException.class, () -> policy.requireWorkspaceMembership(WORKSPACE_A, null));
    }

    private void setupWorkspace() {
        workspaceRepository.save(Workspace.active(WORKSPACE_A, "Workspace A"));
        workspaceRepository.saveMembership(new WorkspaceMembership(WORKSPACE_A, OWNER_ID, WorkspaceRole.OWNER));
        workspaceRepository.saveMembership(new WorkspaceMembership(WORKSPACE_A, VIEWER_ID, WorkspaceRole.VIEWER));
        workspaceRepository.saveMembership(new WorkspaceMembership(WORKSPACE_A, ANALYST_ID, WorkspaceRole.ANALYST));
    }

    private WorkspaceProject setupProject() {
        setupWorkspace();
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
            return projects.stream().filter(project -> project.id().equals(projectId)).findFirst();
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
                .toList();
        }

        @Override
        public List<ProjectMembership> findByUser(UserId userId) {
            return memberships.stream().filter(membership -> membership.userId().equals(userId)).toList();
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
}
