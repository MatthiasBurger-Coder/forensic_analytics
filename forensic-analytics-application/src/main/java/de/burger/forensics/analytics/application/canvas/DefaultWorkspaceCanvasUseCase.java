package de.burger.forensics.analytics.application.canvas;

import de.burger.forensics.analytics.application.asset.port.AssetRepository;
import de.burger.forensics.analytics.application.canvas.command.GetWorkspaceCanvasCommand;
import de.burger.forensics.analytics.application.canvas.result.WorkspaceCanvasView;
import de.burger.forensics.analytics.application.project.port.ProjectMembershipRepository;
import de.burger.forensics.analytics.application.project.port.ProjectRepository;
import de.burger.forensics.analytics.application.workspace.WorkspaceAccessDeniedException;
import de.burger.forensics.analytics.application.workspace.WorkspaceNotFoundException;
import de.burger.forensics.analytics.application.workspace.port.WorkspaceRepository;
import de.burger.forensics.analytics.domain.workspace.WorkspacePermission;
import de.burger.forensics.analytics.domain.workspace.WorkspaceProject;

import java.util.Comparator;
import java.util.Objects;

public final class DefaultWorkspaceCanvasUseCase implements WorkspaceCanvasUseCase {
    private final WorkspaceRepository workspaceRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMembershipRepository projectMembershipRepository;
    private final AssetRepository assetRepository;

    public DefaultWorkspaceCanvasUseCase(
        WorkspaceRepository workspaceRepository,
        ProjectRepository projectRepository,
        ProjectMembershipRepository projectMembershipRepository,
        AssetRepository assetRepository
    ) {
        this.workspaceRepository = Objects.requireNonNull(workspaceRepository, "workspaceRepository must not be null");
        this.projectRepository = Objects.requireNonNull(projectRepository, "projectRepository must not be null");
        this.projectMembershipRepository = Objects.requireNonNull(projectMembershipRepository, "projectMembershipRepository must not be null");
        this.assetRepository = Objects.requireNonNull(assetRepository, "assetRepository must not be null");
    }

    @Override
    public WorkspaceCanvasView get(GetWorkspaceCanvasCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        var workspace = workspaceRepository.findById(command.workspaceId())
            .orElseThrow(() -> new WorkspaceNotFoundException("workspace not found: " + command.workspaceId().value()));
        var membership = workspaceRepository.findMembership(command.workspaceId(), command.actorUserId())
            .orElseThrow(() -> new WorkspaceAccessDeniedException("workspace membership is required"));
        var canReadAllProjects = WorkspacePermission.READ_PROJECT.isGrantedTo(membership.role());
        var visibleProjects = canReadAllProjects
            ? projectRepository.findByWorkspace(command.workspaceId())
            : projectMembershipRepository.findByUser(command.actorUserId()).stream()
                .filter(projectMembership -> projectMembership.workspaceId().equals(command.workspaceId()))
                .map(de.burger.forensics.analytics.domain.workspace.ProjectMembership::project)
                .toList();
        var sortedProjects = visibleProjects.stream()
            .sorted(Comparator.comparing(project -> project.id().value()))
            .toList();

        return new WorkspaceCanvasView(
            workspace,
            membership.role(),
            sortedProjects,
            assetRepository.findSharedByWorkspace(command.workspaceId()),
            WorkspacePermission.UPDATE_WORKSPACE.isGrantedTo(membership.role()),
            WorkspacePermission.MANAGE_SHARED_ASSETS.isGrantedTo(membership.role()),
            WorkspacePermission.READ_WORKSPACE_AUDIT.isGrantedTo(membership.role())
        );
    }
}
