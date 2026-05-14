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
import de.burger.forensics.analytics.domain.workspace.WorkspacePermission;
import de.burger.forensics.analytics.domain.workspace.WorkspaceProject;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class DefaultProjectMemberManagementUseCase implements ProjectMemberManagementUseCase {
    private static final String TARGET_TYPE_PROJECT_MEMBER = "project-member";
    private static final String MEMBER_ADDED = "project.member.added";
    private static final String MEMBER_ROLE_CHANGED = "project.member.role_changed";
    private static final String MEMBER_REMOVED = "project.member.removed";

    private final WorkspaceRepository workspaceRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMembershipRepository projectMembershipRepository;
    private final WorkspaceAuditPort auditPort;
    private final Clock clock;

    public DefaultProjectMemberManagementUseCase(
        WorkspaceRepository workspaceRepository,
        ProjectRepository projectRepository,
        ProjectMembershipRepository projectMembershipRepository,
        WorkspaceAuditPort auditPort,
        Clock clock
    ) {
        this.workspaceRepository = Objects.requireNonNull(workspaceRepository, "workspaceRepository must not be null");
        this.projectRepository = Objects.requireNonNull(projectRepository, "projectRepository must not be null");
        this.projectMembershipRepository = Objects.requireNonNull(projectMembershipRepository, "projectMembershipRepository must not be null");
        this.auditPort = Objects.requireNonNull(auditPort, "auditPort must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public ProjectMembership add(AddProjectMemberCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        var project = activeProject(command.workspaceId(), command.projectId());
        requireManageProjectMembers(command.workspaceId(), command.actorUserId());
        requireWorkspaceMember(command.workspaceId(), command.memberUserId());
        projectMembershipRepository.findMembership(
            command.workspaceId(),
            command.projectId(),
            command.memberUserId()
        ).ifPresent(existing -> {
            throw new ProjectMembershipAlreadyExistsException(
                "project membership already exists for user: " + existing.userId().value()
            );
        });
        var membership = new ProjectMembership(project, command.memberUserId(), command.role());
        projectMembershipRepository.save(membership);
        publishAuditEvent(MEMBER_ADDED, membership, command.actorUserId(), Map.of("role", command.role().name()));
        return membership;
    }

    @Override
    public List<ProjectMembership> list(ListProjectMembersCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        var project = existingProject(command.workspaceId(), command.projectId());
        requireProjectAccess(project, command.actorUserId());
        return projectMembershipRepository.findByProject(command.workspaceId(), command.projectId()).stream()
            .sorted(Comparator.comparing(membership -> membership.userId().value()))
            .toList();
    }

    @Override
    public ProjectMembership changeRole(ChangeProjectMemberRoleCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        activeProject(command.workspaceId(), command.projectId());
        requireManageProjectMembers(command.workspaceId(), command.actorUserId());
        var existing = existingMembership(command.workspaceId(), command.projectId(), command.memberUserId());
        var updated = new ProjectMembership(existing.project(), command.memberUserId(), command.role());
        projectMembershipRepository.save(updated);
        publishAuditEvent(MEMBER_ROLE_CHANGED, updated, command.actorUserId(), Map.of(
            "previousRole", existing.role().name(),
            "role", command.role().name()
        ));
        return updated;
    }

    @Override
    public void remove(RemoveProjectMemberCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        activeProject(command.workspaceId(), command.projectId());
        requireManageProjectMembers(command.workspaceId(), command.actorUserId());
        var existing = existingMembership(command.workspaceId(), command.projectId(), command.memberUserId());
        projectMembershipRepository.removeMembership(command.workspaceId(), command.projectId(), command.memberUserId());
        publishAuditEvent(MEMBER_REMOVED, existing, command.actorUserId(), Map.of("role", existing.role().name()));
    }

    private WorkspaceProject activeProject(WorkspaceId workspaceId, ProjectId projectId) {
        var workspace = existingWorkspace(workspaceId);
        if (!workspace.acceptsChanges()) {
            throw new WorkspaceArchivedException("archived workspace is read-only: " + workspace.id().value());
        }
        var project = existingProject(workspaceId, projectId);
        if (!project.acceptsChanges()) {
            throw new ProjectArchivedException("archived project is read-only: " + project.id().value());
        }
        return project;
    }

    private Workspace existingWorkspace(WorkspaceId workspaceId) {
        return workspaceRepository.findById(workspaceId)
            .orElseThrow(() -> new WorkspaceNotFoundException("workspace not found: " + workspaceId.value()));
    }

    private WorkspaceProject existingProject(WorkspaceId workspaceId, ProjectId projectId) {
        return projectRepository.findById(projectId)
            .filter(project -> project.belongsTo(workspaceId))
            .orElseThrow(() -> new ProjectNotFoundException("project not found in workspace: " + projectId.value()));
    }

    private ProjectMembership existingMembership(WorkspaceId workspaceId, ProjectId projectId, UserId userId) {
        return projectMembershipRepository.findMembership(workspaceId, projectId, userId)
            .orElseThrow(() -> new ProjectMemberNotFoundException("project member not found: " + userId.value()));
    }

    private void requireManageProjectMembers(WorkspaceId workspaceId, UserId actorUserId) {
        var membership = requireWorkspaceMember(workspaceId, actorUserId);
        if (!WorkspacePermission.MANAGE_PROJECT_MEMBERS.isGrantedTo(membership.role())) {
            throw new WorkspaceAccessDeniedException("workspace role is not allowed to MANAGE_PROJECT_MEMBERS");
        }
    }

    private de.burger.forensics.analytics.domain.workspace.WorkspaceMembership requireWorkspaceMember(
        WorkspaceId workspaceId,
        UserId userId
    ) {
        return workspaceRepository.findMembership(workspaceId, userId)
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

    private void publishAuditEvent(
        String action,
        ProjectMembership membership,
        UserId actorUserId,
        Map<String, String> metadata
    ) {
        auditPort.publish(new AuditEvent(
            membership.workspaceId(),
            actorUserId,
            action,
            TARGET_TYPE_PROJECT_MEMBER,
            membership.projectId().value() + ":" + membership.userId().value(),
            clock.instant(),
            metadata
        ));
    }
}
