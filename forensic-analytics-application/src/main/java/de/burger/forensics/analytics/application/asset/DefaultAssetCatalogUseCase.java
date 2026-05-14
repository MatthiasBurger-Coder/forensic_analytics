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
import de.burger.forensics.analytics.application.workspace.WorkspaceNotFoundException;
import de.burger.forensics.analytics.application.workspace.port.WorkspaceAuditPort;
import de.burger.forensics.analytics.application.workspace.port.WorkspaceRepository;
import de.burger.forensics.analytics.domain.audit.AuditEvent;
import de.burger.forensics.analytics.domain.workspace.AssetScope;
import de.burger.forensics.analytics.domain.workspace.ProjectAssetScope;
import de.burger.forensics.analytics.domain.workspace.ProjectId;
import de.burger.forensics.analytics.domain.workspace.SharedWorkspaceAssetScope;
import de.burger.forensics.analytics.domain.workspace.UserId;
import de.burger.forensics.analytics.domain.workspace.Workspace;
import de.burger.forensics.analytics.domain.workspace.WorkspaceAsset;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;
import de.burger.forensics.analytics.domain.workspace.WorkspacePermission;
import de.burger.forensics.analytics.domain.workspace.WorkspaceProject;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class DefaultAssetCatalogUseCase implements AssetCatalogUseCase {
    private static final String ASSET_UPLOADED = "asset.uploaded";
    private static final String TARGET_TYPE_ASSET = "asset";

    private final WorkspaceRepository workspaceRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMembershipRepository projectMembershipRepository;
    private final AssetRepository assetRepository;
    private final WorkspaceAuditPort auditPort;
    private final Clock clock;

    public DefaultAssetCatalogUseCase(
        WorkspaceRepository workspaceRepository,
        ProjectRepository projectRepository,
        ProjectMembershipRepository projectMembershipRepository,
        AssetRepository assetRepository,
        WorkspaceAuditPort auditPort,
        Clock clock
    ) {
        this.workspaceRepository = Objects.requireNonNull(workspaceRepository, "workspaceRepository must not be null");
        this.projectRepository = Objects.requireNonNull(projectRepository, "projectRepository must not be null");
        this.projectMembershipRepository = Objects.requireNonNull(projectMembershipRepository, "projectMembershipRepository must not be null");
        this.assetRepository = Objects.requireNonNull(assetRepository, "assetRepository must not be null");
        this.auditPort = Objects.requireNonNull(auditPort, "auditPort must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public WorkspaceAsset registerShared(RegisterSharedAssetCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        var workspace = activeWorkspace(command.workspaceId());
        requireWorkspacePermission(workspace.id(), command.actorUserId(), WorkspacePermission.MANAGE_SHARED_ASSETS);
        var asset = asset(command, new SharedWorkspaceAssetScope(command.workspaceId()));
        assetRepository.save(asset);
        publishUploaded(asset, command.actorUserId(), "shared");
        return asset;
    }

    @Override
    public WorkspaceAsset registerProject(RegisterProjectAssetCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        activeWorkspace(command.workspaceId());
        var project = activeProject(command.workspaceId(), command.projectId());
        requireProjectAccess(project, command.actorUserId());
        var asset = asset(command, new ProjectAssetScope(command.workspaceId(), command.projectId()));
        assetRepository.save(asset);
        publishUploaded(asset, command.actorUserId(), "project");
        return asset;
    }

    @Override
    public List<WorkspaceAsset> listShared(ListSharedAssetsCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        existingWorkspace(command.workspaceId());
        requireWorkspaceMember(command.workspaceId(), command.actorUserId());
        return assetRepository.findSharedByWorkspace(command.workspaceId()).stream()
            .sorted(Comparator.comparing(asset -> asset.id().value()))
            .toList();
    }

    @Override
    public List<WorkspaceAsset> listProject(ListProjectAssetsCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        existingWorkspace(command.workspaceId());
        var project = existingProject(command.workspaceId(), command.projectId());
        requireProjectAccess(project, command.actorUserId());
        return assetRepository.findByProject(command.workspaceId(), command.projectId()).stream()
            .sorted(Comparator.comparing(asset -> asset.id().value()))
            .toList();
    }

    private WorkspaceAsset asset(RegisterSharedAssetCommand command, AssetScope scope) {
        return new WorkspaceAsset(
            command.assetId(),
            scope,
            command.storedFileName(),
            command.sha256(),
            command.sizeBytes()
        );
    }

    private WorkspaceAsset asset(RegisterProjectAssetCommand command, AssetScope scope) {
        return new WorkspaceAsset(
            command.assetId(),
            scope,
            command.storedFileName(),
            command.sha256(),
            command.sizeBytes()
        );
    }

    private Workspace activeWorkspace(WorkspaceId workspaceId) {
        var workspace = existingWorkspace(workspaceId);
        if (!workspace.acceptsChanges()) {
            throw new WorkspaceArchivedException("archived workspace is read-only: " + workspace.id().value());
        }
        return workspace;
    }

    private Workspace existingWorkspace(WorkspaceId workspaceId) {
        return workspaceRepository.findById(workspaceId)
            .orElseThrow(() -> new WorkspaceNotFoundException("workspace not found: " + workspaceId.value()));
    }

    private WorkspaceProject activeProject(WorkspaceId workspaceId, ProjectId projectId) {
        var project = existingProject(workspaceId, projectId);
        if (!project.acceptsChanges()) {
            throw new ProjectArchivedException("archived project is read-only: " + project.id().value());
        }
        return project;
    }

    private WorkspaceProject existingProject(WorkspaceId workspaceId, ProjectId projectId) {
        return projectRepository.findById(projectId)
            .filter(project -> project.belongsTo(workspaceId))
            .orElseThrow(() -> new ProjectNotFoundException("project not found in workspace: " + projectId.value()));
    }

    private void requireWorkspacePermission(WorkspaceId workspaceId, UserId actorUserId, WorkspacePermission permission) {
        var membership = requireWorkspaceMember(workspaceId, actorUserId);
        if (!permission.isGrantedTo(membership.role())) {
            throw new WorkspaceAccessDeniedException("workspace role is not allowed to " + permission.name());
        }
    }

    private de.burger.forensics.analytics.domain.workspace.WorkspaceMembership requireWorkspaceMember(
        WorkspaceId workspaceId,
        UserId actorUserId
    ) {
        return workspaceRepository.findMembership(workspaceId, actorUserId)
            .orElseThrow(() -> new WorkspaceAccessDeniedException("workspace membership is required"));
    }

    private void requireProjectAccess(WorkspaceProject project, UserId actorUserId) {
        var workspaceMembership = requireWorkspaceMember(project.workspaceId(), actorUserId);
        var workspaceRoleAllowsAllProjects = WorkspacePermission.READ_PROJECT.isGrantedTo(workspaceMembership.role());
        var assignedProjectMember = projectMembershipRepository.findMembership(
            project.workspaceId(),
            project.id(),
            actorUserId
        ).isPresent();
        if (!workspaceRoleAllowsAllProjects && !assignedProjectMember) {
            throw new WorkspaceAccessDeniedException("project membership is required");
        }
    }

    private void publishUploaded(WorkspaceAsset asset, UserId actorUserId, String scope) {
        auditPort.publish(new AuditEvent(
            asset.workspaceId(),
            actorUserId,
            ASSET_UPLOADED,
            TARGET_TYPE_ASSET,
            asset.id().value(),
            clock.instant(),
            Map.of(
                "scope", scope,
                "sha256", asset.sha256(),
                "sizeBytes", Long.toString(asset.sizeBytes())
            )
        ));
    }
}
