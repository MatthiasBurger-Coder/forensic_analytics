package de.burger.forensics.analytics.application.asset;

import de.burger.forensics.analytics.application.asset.command.ListProjectAssetsCommand;
import de.burger.forensics.analytics.application.asset.command.ListSharedAssetsCommand;
import de.burger.forensics.analytics.application.asset.command.RegisterProjectAssetCommand;
import de.burger.forensics.analytics.application.asset.command.RegisterSharedAssetCommand;
import de.burger.forensics.analytics.application.asset.port.AssetRepository;
import de.burger.forensics.analytics.application.project.ProjectArchivedException;
import de.burger.forensics.analytics.application.project.ProjectNotFoundException;
import de.burger.forensics.analytics.application.project.port.ProjectMembershipRepository;
import de.burger.forensics.analytics.application.project.port.ProjectRepository;
import de.burger.forensics.analytics.application.workspace.WorkspaceAccessDeniedException;
import de.burger.forensics.analytics.application.workspace.WorkspaceArchivedException;
import de.burger.forensics.analytics.application.workspace.port.WorkspaceAuditPort;
import de.burger.forensics.analytics.application.workspace.port.WorkspaceRepository;
import de.burger.forensics.analytics.domain.audit.AuditEvent;
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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultAssetCatalogUseCaseTest {
    private static final WorkspaceId WORKSPACE_A = new WorkspaceId("workspace-a");
    private static final ProjectId PROJECT_A = new ProjectId("project-a");
    private static final AssetId ASSET_A = new AssetId("asset-a");
    private static final AssetId ASSET_B = new AssetId("asset-b");
    private static final UserId OWNER_ID = new UserId("owner-a");
    private static final UserId VIEWER_ID = new UserId("viewer-a");
    private static final UserId ANALYST_ID = new UserId("analyst-a");
    private static final UserId OUTSIDER_ID = new UserId("outsider-a");
    private static final Instant NOW = Instant.parse("2026-05-14T14:15:30Z");

    private final RecordingWorkspaceRepository workspaceRepository = new RecordingWorkspaceRepository();
    private final RecordingProjectRepository projectRepository = new RecordingProjectRepository();
    private final RecordingProjectMembershipRepository projectMembershipRepository = new RecordingProjectMembershipRepository();
    private final RecordingAssetRepository assetRepository = new RecordingAssetRepository();
    private final RecordingWorkspaceAuditPort auditPort = new RecordingWorkspaceAuditPort();
    private final DefaultAssetCatalogUseCase useCase = new DefaultAssetCatalogUseCase(
        workspaceRepository,
        projectRepository,
        projectMembershipRepository,
        assetRepository,
        auditPort,
        Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void ownerRegistersSharedAssetAndWorkspaceMemberListsIt() {
        activeWorkspaceWithMembers();

        var asset = useCase.registerShared(sharedCommand(ASSET_A, OWNER_ID));
        var listed = useCase.listShared(new ListSharedAssetsCommand(WORKSPACE_A, VIEWER_ID));

        assertEquals(new WorkspaceAsset(ASSET_A, new SharedWorkspaceAssetScope(WORKSPACE_A), "asset-a.bin", "sha256:asset-a", 42L), asset);
        assertEquals(List.of(asset), listed);
        assertEquals("asset.uploaded", auditPort.events.getFirst().action());
        assertEquals("shared", auditPort.events.getFirst().metadata().get("scope"));
        assertEquals("sha256:asset-a", auditPort.events.getFirst().metadata().get("sha256"));
        assertEquals(NOW, auditPort.events.getFirst().occurredAt());
    }

    @Test
    void assignedProjectMemberRegistersAndListsProjectAsset() {
        var project = activeProjectWithAssignedAnalyst();

        var asset = useCase.registerProject(projectCommand(ASSET_B, ANALYST_ID));
        var listed = useCase.listProject(new ListProjectAssetsCommand(WORKSPACE_A, PROJECT_A, ANALYST_ID));

        assertEquals(new WorkspaceAsset(ASSET_B, new ProjectAssetScope(WORKSPACE_A, PROJECT_A), "asset-b.bin", "sha256:asset-b", 42L), asset);
        assertEquals(List.of(asset), listed);
        assertEquals(project, projectMembershipRepository.findMembership(WORKSPACE_A, PROJECT_A, ANALYST_ID).orElseThrow().project());
        assertEquals("project", auditPort.events.getFirst().metadata().get("scope"));
    }

    @Test
    void viewerCannotRegisterSharedAssetAndUnassignedMemberCannotRegisterProjectAsset() {
        activeProjectWithAssignedAnalyst();

        assertThrows(WorkspaceAccessDeniedException.class, () -> useCase.registerShared(sharedCommand(ASSET_A, VIEWER_ID)));
        assertThrows(WorkspaceAccessDeniedException.class, () -> useCase.registerProject(projectCommand(ASSET_A, VIEWER_ID)));
    }

    @Test
    void outsiderCannotListSharedOrProjectAssets() {
        activeProjectWithAssignedAnalyst();

        assertThrows(
            WorkspaceAccessDeniedException.class,
            () -> useCase.listShared(new ListSharedAssetsCommand(WORKSPACE_A, OUTSIDER_ID))
        );
        assertThrows(
            WorkspaceAccessDeniedException.class,
            () -> useCase.listProject(new ListProjectAssetsCommand(WORKSPACE_A, PROJECT_A, OUTSIDER_ID))
        );
    }

    @Test
    void archivedWorkspaceOrProjectRejectsAssetRegistration() {
        var project = activeProjectWithAssignedAnalyst();
        projectRepository.update(project.archive());

        assertThrows(ProjectArchivedException.class, () -> useCase.registerProject(projectCommand(ASSET_A, ANALYST_ID)));

        projectRepository.update(project);
        workspaceRepository.update(Workspace.active(WORKSPACE_A, "Workspace A").archive());

        assertThrows(WorkspaceArchivedException.class, () -> useCase.registerShared(sharedCommand(ASSET_A, OWNER_ID)));
        assertThrows(WorkspaceArchivedException.class, () -> useCase.registerProject(projectCommand(ASSET_A, ANALYST_ID)));
    }

    @Test
    void unknownProjectFailsExplicitly() {
        activeWorkspaceWithMembers();

        assertThrows(ProjectNotFoundException.class, () -> useCase.registerProject(projectCommand(ASSET_A, ANALYST_ID)));
    }

    @Test
    void dependenciesAndCommandsAreRequired() {
        assertThrows(NullPointerException.class, () -> new DefaultAssetCatalogUseCase(null, projectRepository, projectMembershipRepository, assetRepository, auditPort, Clock.systemUTC()));
        assertThrows(NullPointerException.class, () -> new DefaultAssetCatalogUseCase(workspaceRepository, null, projectMembershipRepository, assetRepository, auditPort, Clock.systemUTC()));
        assertThrows(NullPointerException.class, () -> new DefaultAssetCatalogUseCase(workspaceRepository, projectRepository, null, assetRepository, auditPort, Clock.systemUTC()));
        assertThrows(NullPointerException.class, () -> new DefaultAssetCatalogUseCase(workspaceRepository, projectRepository, projectMembershipRepository, null, auditPort, Clock.systemUTC()));
        assertThrows(NullPointerException.class, () -> new DefaultAssetCatalogUseCase(workspaceRepository, projectRepository, projectMembershipRepository, assetRepository, null, Clock.systemUTC()));
        assertThrows(NullPointerException.class, () -> new DefaultAssetCatalogUseCase(workspaceRepository, projectRepository, projectMembershipRepository, assetRepository, auditPort, null));
        assertThrows(NullPointerException.class, () -> useCase.registerShared(null));
        assertThrows(NullPointerException.class, () -> useCase.registerProject(null));
        assertThrows(NullPointerException.class, () -> useCase.listShared(null));
        assertThrows(NullPointerException.class, () -> useCase.listProject(null));
        assertThrows(NullPointerException.class, () -> new RegisterSharedAssetCommand(null, ASSET_A, "file", "sha", 1L, OWNER_ID));
        assertThrows(NullPointerException.class, () -> new RegisterSharedAssetCommand(WORKSPACE_A, null, "file", "sha", 1L, OWNER_ID));
        assertThrows(IllegalArgumentException.class, () -> new RegisterSharedAssetCommand(WORKSPACE_A, ASSET_A, " ", "sha", 1L, OWNER_ID));
        assertThrows(IllegalArgumentException.class, () -> new RegisterSharedAssetCommand(WORKSPACE_A, ASSET_A, "file", " ", 1L, OWNER_ID));
        assertThrows(IllegalArgumentException.class, () -> new RegisterSharedAssetCommand(WORKSPACE_A, ASSET_A, "file", "sha", -1L, OWNER_ID));
        assertThrows(NullPointerException.class, () -> new RegisterSharedAssetCommand(WORKSPACE_A, ASSET_A, "file", "sha", 1L, null));
        assertThrows(NullPointerException.class, () -> new RegisterProjectAssetCommand(null, PROJECT_A, ASSET_A, "file", "sha", 1L, OWNER_ID));
        assertThrows(NullPointerException.class, () -> new RegisterProjectAssetCommand(WORKSPACE_A, null, ASSET_A, "file", "sha", 1L, OWNER_ID));
        assertThrows(NullPointerException.class, () -> new RegisterProjectAssetCommand(WORKSPACE_A, PROJECT_A, null, "file", "sha", 1L, OWNER_ID));
        assertThrows(IllegalArgumentException.class, () -> new RegisterProjectAssetCommand(WORKSPACE_A, PROJECT_A, ASSET_A, " ", "sha", 1L, OWNER_ID));
        assertThrows(IllegalArgumentException.class, () -> new RegisterProjectAssetCommand(WORKSPACE_A, PROJECT_A, ASSET_A, "file", " ", 1L, OWNER_ID));
        assertThrows(IllegalArgumentException.class, () -> new RegisterProjectAssetCommand(WORKSPACE_A, PROJECT_A, ASSET_A, "file", "sha", -1L, OWNER_ID));
        assertThrows(NullPointerException.class, () -> new RegisterProjectAssetCommand(WORKSPACE_A, PROJECT_A, ASSET_A, "file", "sha", 1L, null));
        assertThrows(NullPointerException.class, () -> new ListSharedAssetsCommand(null, OWNER_ID));
        assertThrows(NullPointerException.class, () -> new ListSharedAssetsCommand(WORKSPACE_A, null));
        assertThrows(NullPointerException.class, () -> new ListProjectAssetsCommand(null, PROJECT_A, OWNER_ID));
        assertThrows(NullPointerException.class, () -> new ListProjectAssetsCommand(WORKSPACE_A, null, OWNER_ID));
        assertThrows(NullPointerException.class, () -> new ListProjectAssetsCommand(WORKSPACE_A, PROJECT_A, null));
    }

    private void activeWorkspaceWithMembers() {
        workspaceRepository.save(Workspace.active(WORKSPACE_A, "Workspace A"));
        workspaceRepository.saveMembership(new WorkspaceMembership(WORKSPACE_A, OWNER_ID, WorkspaceRole.OWNER));
        workspaceRepository.saveMembership(new WorkspaceMembership(WORKSPACE_A, VIEWER_ID, WorkspaceRole.VIEWER));
        workspaceRepository.saveMembership(new WorkspaceMembership(WORKSPACE_A, ANALYST_ID, WorkspaceRole.ANALYST));
    }

    private WorkspaceProject activeProjectWithAssignedAnalyst() {
        activeWorkspaceWithMembers();
        var project = WorkspaceProject.active(PROJECT_A, WORKSPACE_A, "Project A");
        projectRepository.save(project);
        projectMembershipRepository.save(new ProjectMembership(project, ANALYST_ID, WorkspaceRole.ANALYST));
        return project;
    }

    private static RegisterSharedAssetCommand sharedCommand(AssetId assetId, UserId actorUserId) {
        return new RegisterSharedAssetCommand(WORKSPACE_A, assetId, assetId.value() + ".bin", "sha256:" + assetId.value(), 42L, actorUserId);
    }

    private static RegisterProjectAssetCommand projectCommand(AssetId assetId, UserId actorUserId) {
        return new RegisterProjectAssetCommand(WORKSPACE_A, PROJECT_A, assetId, assetId.value() + ".bin", "sha256:" + assetId.value(), 42L, actorUserId);
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
            assets.removeIf(existing -> existing.id().equals(asset.id()));
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

    private static final class RecordingWorkspaceAuditPort implements WorkspaceAuditPort {
        private final List<AuditEvent> events = new ArrayList<>();

        @Override
        public void publish(AuditEvent event) {
            events.add(event);
        }
    }
}
