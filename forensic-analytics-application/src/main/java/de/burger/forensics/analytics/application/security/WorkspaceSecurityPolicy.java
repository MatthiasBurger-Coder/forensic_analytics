package de.burger.forensics.analytics.application.security;

import de.burger.forensics.analytics.application.project.ProjectNotFoundException;
import de.burger.forensics.analytics.application.project.port.ProjectMembershipRepository;
import de.burger.forensics.analytics.application.project.port.ProjectRepository;
import de.burger.forensics.analytics.application.workspace.WorkspaceAccessDeniedException;
import de.burger.forensics.analytics.application.workspace.WorkspaceNotFoundException;
import de.burger.forensics.analytics.application.workspace.port.WorkspaceRepository;
import de.burger.forensics.analytics.domain.workspace.ProjectId;
import de.burger.forensics.analytics.domain.workspace.UserId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceMembership;
import de.burger.forensics.analytics.domain.workspace.WorkspacePermission;
import de.burger.forensics.analytics.domain.workspace.WorkspaceProject;

import java.util.Objects;

public final class WorkspaceSecurityPolicy {
    private final WorkspaceRepository workspaceRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMembershipRepository projectMembershipRepository;

    public WorkspaceSecurityPolicy(
        WorkspaceRepository workspaceRepository,
        ProjectRepository projectRepository,
        ProjectMembershipRepository projectMembershipRepository
    ) {
        this.workspaceRepository = Objects.requireNonNull(workspaceRepository, "workspaceRepository must not be null");
        this.projectRepository = Objects.requireNonNull(projectRepository, "projectRepository must not be null");
        this.projectMembershipRepository = Objects.requireNonNull(projectMembershipRepository, "projectMembershipRepository must not be null");
    }

    public WorkspaceMembership requireWorkspacePermission(
        WorkspaceId workspaceId,
        UserId actorUserId,
        WorkspacePermission permission
    ) {
        Objects.requireNonNull(permission, "permission must not be null");
        workspaceRepository.findById(Objects.requireNonNull(workspaceId, "workspaceId must not be null"))
            .orElseThrow(() -> new WorkspaceNotFoundException("workspace not found: " + workspaceId.value()));
        var membership = workspaceRepository.findMembership(workspaceId, Objects.requireNonNull(actorUserId, "actorUserId must not be null"))
            .orElseThrow(() -> new WorkspaceAccessDeniedException("workspace membership is required"));
        if (!permission.isGrantedTo(membership.role())) {
            throw new WorkspaceAccessDeniedException("workspace role is not allowed to " + permission.name());
        }
        return membership;
    }

    public WorkspaceProject requireProjectInWorkspace(WorkspaceId workspaceId, ProjectId projectId) {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        Objects.requireNonNull(projectId, "projectId must not be null");
        return projectRepository.findById(projectId)
            .filter(project -> project.belongsTo(workspaceId))
            .orElseThrow(() -> new ProjectNotFoundException("project not found in workspace: " + projectId.value()));
    }

    public WorkspaceProject requireProjectAccess(WorkspaceId workspaceId, ProjectId projectId, UserId actorUserId) {
        var project = requireProjectInWorkspace(workspaceId, projectId);
        var workspaceMembership = requireWorkspaceMembership(workspaceId, actorUserId);
        var workspaceRoleAllowsAllProjects = WorkspacePermission.READ_PROJECT.isGrantedTo(workspaceMembership.role());
        var assignedProjectMember = projectMembershipRepository.findMembership(workspaceId, projectId, actorUserId)
            .isPresent();
        if (!workspaceRoleAllowsAllProjects && !assignedProjectMember) {
            throw new WorkspaceAccessDeniedException("project membership is required");
        }
        return project;
    }

    public WorkspaceMembership requireWorkspaceMembership(WorkspaceId workspaceId, UserId actorUserId) {
        workspaceRepository.findById(Objects.requireNonNull(workspaceId, "workspaceId must not be null"))
            .orElseThrow(() -> new WorkspaceNotFoundException("workspace not found: " + workspaceId.value()));
        return workspaceRepository.findMembership(workspaceId, Objects.requireNonNull(actorUserId, "actorUserId must not be null"))
            .orElseThrow(() -> new WorkspaceAccessDeniedException("workspace membership is required"));
    }
}
