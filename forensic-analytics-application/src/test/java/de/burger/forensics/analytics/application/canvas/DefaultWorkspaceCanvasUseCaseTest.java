package de.burger.forensics.analytics.application.canvas;

import de.burger.forensics.analytics.application.asset.port.AssetRepository;
import de.burger.forensics.analytics.application.canvas.command.GetWorkspaceCanvasCommand;
import de.burger.forensics.analytics.application.canvas.result.WorkspaceCanvasView;
import de.burger.forensics.analytics.application.project.port.ProjectMembershipRepository;
import de.burger.forensics.analytics.application.project.port.ProjectRepository;
import de.burger.forensics.analytics.application.workspace.WorkspaceAccessDeniedException;
import de.burger.forensics.analytics.application.workspace.WorkspaceNotFoundException;
import de.burger.forensics.analytics.application.workspace.port.WorkspaceRepository;
import de.burger.forensics.analytics.domain.workspace.AssetId;
import de.burger.forensics.analytics.domain.workspace.ProjectAssetScope;
import de.burger.forensics.analytics.domain.workspace.ProjectId;
import de.burger.forensics.analytics.domain.workspace.ProjectMembership;
import de.burger.forensics.analytics.domain.workspace.SharedWorkspaceAssetScope;
import de.burger.forensics.analytics.domain.workspace.UserId;
import de.burger.forensics.analytics.domain.workspace.Workspace;
import de.burger.forensics.analytics.domain.workspace.WorkspaceAsset;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceMembership;
import de.burger.forensics.analytics.domain.workspace.WorkspaceProject;
import de.burger.forensics.analytics.domain.workspace.WorkspaceRole;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultWorkspaceCanvasUseCaseTest {
    private static final WorkspaceId WORKSPACE_A = new WorkspaceId("workspace-a");
    private static final ProjectId PROJECT_A = new ProjectId("project-a");
    private static final ProjectId PROJECT_B = new ProjectId("project-b");
    private static final UserId OWNER_ID = new UserId("owner-a");
    private static final UserId ANALYST_ID = new UserId("analyst-a");
    private static final UserId OUTSIDER_ID = new UserId("outsider-a");

    private final RecordingWorkspaceRepository workspaceRepository = new RecordingWorkspaceRepository();
    private final RecordingProjectRepository projectRepository = new RecordingProjectRepository();
    private final RecordingProjectMembershipRepository projectMembershipRepository = new RecordingProjectMembershipRepository();
    private final RecordingAssetRepository assetRepository = new RecordingAssetRepository();
    private final DefaultWorkspaceCanvasUseCase useCase = new DefaultWorkspaceCanvasUseCase(
        workspaceRepository,
        projectRepository,
        projectMembershipRepository,
        assetRepository
    );

    @Test
    void ownerCanvasShowsAllProjectsAndManagementActions() {
        var projectA = setupWorkspaceWithProjectsAndAssets();
        var projectB = projectRepository.findById(PROJECT_B).orElseThrow();

        var view = useCase.get(new GetWorkspaceCanvasCommand(WORKSPACE_A, OWNER_ID));

        assertEquals(WorkspaceRole.OWNER, view.actorRole());
        assertEquals(List.of(projectA, projectB), view.visibleProjects());
        assertEquals(List.of(sharedAsset()), view.sharedAssets());
        assertTrue(view.canManageWorkspace());
        assertTrue(view.canManageSharedAssets());
        assertTrue(view.canReadAuditLog());
    }

    @Test
    void assignedAnalystCanvasShowsOnlyAssignedProjectsAndNoManagementActions() {
        var projectA = setupWorkspaceWithProjectsAndAssets();
        projectMembershipRepository.save(new ProjectMembership(projectA, ANALYST_ID, WorkspaceRole.ANALYST));

        var view = useCase.get(new GetWorkspaceCanvasCommand(WORKSPACE_A, ANALYST_ID));

        assertEquals(WorkspaceRole.ANALYST, view.actorRole());
        assertEquals(List.of(projectA), view.visibleProjects());
        assertEquals(List.of(sharedAsset()), view.sharedAssets());
        assertFalse(view.canManageWorkspace());
        assertFalse(view.canManageSharedAssets());
        assertFalse(view.canReadAuditLog());
    }

    @Test
    void outsiderCannotOpenWorkspaceCanvas() {
        setupWorkspaceWithProjectsAndAssets();

        assertThrows(
            WorkspaceAccessDeniedException.class,
            () -> useCase.get(new GetWorkspaceCanvasCommand(WORKSPACE_A, OUTSIDER_ID))
        );
    }

    @Test
    void unknownWorkspaceFailsExplicitly() {
        assertThrows(
            WorkspaceNotFoundException.class,
            () -> useCase.get(new GetWorkspaceCanvasCommand(WORKSPACE_A, OWNER_ID))
        );
    }

    @Test
    void viewDefensivelyCopiesLists() {
        var projects = new ArrayList<WorkspaceProject>();
        projects.add(WorkspaceProject.active(PROJECT_A, WORKSPACE_A, "Project A"));
        var assets = new ArrayList<WorkspaceAsset>();
        assets.add(sharedAsset());

        var view = new WorkspaceCanvasView(
            Workspace.active(WORKSPACE_A, "Workspace A"),
            WorkspaceRole.OWNER,
            projects,
            assets,
            true,
            true,
            true
        );
        projects.clear();
        assets.clear();

        assertEquals(1, view.visibleProjects().size());
        assertEquals(1, view.sharedAssets().size());
        assertThrows(UnsupportedOperationException.class, () -> view.visibleProjects().add(WorkspaceProject.active(PROJECT_B, WORKSPACE_A, "Project B")));
        assertThrows(UnsupportedOperationException.class, () -> view.sharedAssets().add(sharedAsset()));
    }

    @Test
    void dependenciesAndCommandAreRequired() {
        assertThrows(NullPointerException.class, () -> new DefaultWorkspaceCanvasUseCase(null, projectRepository, projectMembershipRepository, assetRepository));
        assertThrows(NullPointerException.class, () -> new DefaultWorkspaceCanvasUseCase(workspaceRepository, null, projectMembershipRepository, assetRepository));
        assertThrows(NullPointerException.class, () -> new DefaultWorkspaceCanvasUseCase(workspaceRepository, projectRepository, null, assetRepository));
        assertThrows(NullPointerException.class, () -> new DefaultWorkspaceCanvasUseCase(workspaceRepository, projectRepository, projectMembershipRepository, null));
        assertThrows(NullPointerException.class, () -> useCase.get(null));
        assertThrows(NullPointerException.class, () -> new GetWorkspaceCanvasCommand(null, OWNER_ID));
        assertThrows(NullPointerException.class, () -> new GetWorkspaceCanvasCommand(WORKSPACE_A, null));
        assertThrows(NullPointerException.class, () -> new WorkspaceCanvasView(null, WorkspaceRole.OWNER, List.of(), List.of(), true, true, true));
        assertThrows(NullPointerException.class, () -> new WorkspaceCanvasView(Workspace.active(WORKSPACE_A, "Workspace"), null, List.of(), List.of(), true, true, true));
        assertThrows(NullPointerException.class, () -> new WorkspaceCanvasView(Workspace.active(WORKSPACE_A, "Workspace"), WorkspaceRole.OWNER, null, List.of(), true, true, true));
        assertThrows(NullPointerException.class, () -> new WorkspaceCanvasView(Workspace.active(WORKSPACE_A, "Workspace"), WorkspaceRole.OWNER, List.of(), null, true, true, true));
    }

    private WorkspaceProject setupWorkspaceWithProjectsAndAssets() {
        workspaceRepository.save(Workspace.active(WORKSPACE_A, "Workspace A"));
        workspaceRepository.saveMembership(new WorkspaceMembership(WORKSPACE_A, OWNER_ID, WorkspaceRole.OWNER));
        workspaceRepository.saveMembership(new WorkspaceMembership(WORKSPACE_A, ANALYST_ID, WorkspaceRole.ANALYST));
        var projectA = WorkspaceProject.active(PROJECT_A, WORKSPACE_A, "Project A");
        var projectB = WorkspaceProject.active(PROJECT_B, WORKSPACE_A, "Project B");
        projectRepository.save(projectB);
        projectRepository.save(projectA);
        assetRepository.save(sharedAsset());
        assetRepository.save(new WorkspaceAsset(
            new AssetId("project-asset"),
            new ProjectAssetScope(WORKSPACE_A, PROJECT_A),
            "project.bin",
            "sha256:project",
            1L
        ));
        return projectA;
    }

    private static WorkspaceAsset sharedAsset() {
        return new WorkspaceAsset(
            new AssetId("shared-asset"),
            new SharedWorkspaceAssetScope(WORKSPACE_A),
            "shared.bin",
            "sha256:shared",
            1L
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
            return memberships.stream()
                .filter(membership -> membership.userId().equals(userId))
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

    private static final class RecordingAssetRepository implements AssetRepository {
        private final List<WorkspaceAsset> assets = new ArrayList<>();

        @Override
        public void save(WorkspaceAsset asset) {
            assets.add(asset);
        }

        @Override
        public Optional<WorkspaceAsset> findById(AssetId assetId) {
            return assets.stream().filter(asset -> asset.id().equals(assetId)).findFirst();
        }

        @Override
        public List<WorkspaceAsset> findSharedByWorkspace(WorkspaceId workspaceId) {
            return assets.stream()
                .filter(asset -> asset.workspaceId().equals(workspaceId))
                .filter(WorkspaceAsset::isShared)
                .sorted(Comparator.comparing(asset -> asset.id().value()))
                .toList();
        }

        @Override
        public List<WorkspaceAsset> findByProject(WorkspaceId workspaceId, ProjectId projectId) {
            return assets.stream()
                .filter(asset -> asset.workspaceId().equals(workspaceId))
                .filter(asset -> asset.scope().projectId().filter(projectId::equals).isPresent())
                .sorted(Comparator.comparing(asset -> asset.id().value()))
                .toList();
        }
    }
}
