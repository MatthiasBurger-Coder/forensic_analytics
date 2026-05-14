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

public final class DefaultProjectManagementUseCase implements ProjectManagementUseCase {
    private static final String TARGET_TYPE_PROJECT = "project";
    private static final String PROJECT_CREATED = "project.created";
    private static final String PROJECT_UPDATED = "project.updated";
    private static final String PROJECT_ARCHIVED = "project.archived";

    private final WorkspaceRepository workspaceRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMembershipRepository projectMembershipRepository;
    private final WorkspaceAuditPort auditPort;
    private final Clock clock;

    public DefaultProjectManagementUseCase(
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
    public WorkspaceProject create(CreateProjectCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        var workspace = activeWorkspace(command.workspaceId());
        requireWorkspacePermission(command.workspaceId(), command.actorUserId(), WorkspacePermission.CREATE_PROJECT);
        var project = WorkspaceProject.active(command.projectId(), workspace.id(), command.name());
        projectRepository.save(project);
        publishAuditEvent(PROJECT_CREATED, project, command.actorUserId());
        return project;
    }

    @Override
    public WorkspaceProject get(GetProjectCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        existingWorkspace(command.workspaceId());
        var project = existingProject(command.workspaceId(), command.projectId());
        requireProjectReadAccess(project, command.actorUserId());
        return project;
    }

    @Override
    public List<WorkspaceProject> list(ListProjectsCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        existingWorkspace(command.workspaceId());
        var membership = requireWorkspaceMembership(command.workspaceId(), command.actorUserId());
        if (WorkspacePermission.READ_PROJECT.isGrantedTo(membership.role())) {
            return projectRepository.findByWorkspace(command.workspaceId()).stream()
                .sorted(Comparator.comparing(project -> project.id().value()))
                .toList();
        }
        return projectMembershipRepository.findByUser(command.actorUserId()).stream()
            .filter(projectMembership -> projectMembership.workspaceId().equals(command.workspaceId()))
            .map(de.burger.forensics.analytics.domain.workspace.ProjectMembership::project)
            .sorted(Comparator.comparing(project -> project.id().value()))
            .toList();
    }

    @Override
    public WorkspaceProject rename(RenameProjectCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        activeWorkspace(command.workspaceId());
        requireWorkspacePermission(command.workspaceId(), command.actorUserId(), WorkspacePermission.UPDATE_PROJECT);
        var project = activeProject(command.workspaceId(), command.projectId());
        var renamed = project.rename(command.name());
        projectRepository.update(renamed);
        publishAuditEvent(PROJECT_UPDATED, renamed, command.actorUserId());
        return renamed;
    }

    @Override
    public WorkspaceProject archive(ArchiveProjectCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        activeWorkspace(command.workspaceId());
        requireWorkspacePermission(command.workspaceId(), command.actorUserId(), WorkspacePermission.ARCHIVE_PROJECT);
        var project = activeProject(command.workspaceId(), command.projectId());
        var archived = project.archive();
        projectRepository.update(archived);
        publishAuditEvent(PROJECT_ARCHIVED, archived, command.actorUserId());
        return archived;
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
        var membership = requireWorkspaceMembership(workspaceId, actorUserId);
        if (!permission.isGrantedTo(membership.role())) {
            throw new WorkspaceAccessDeniedException("workspace role is not allowed to " + permission.name());
        }
    }

    private de.burger.forensics.analytics.domain.workspace.WorkspaceMembership requireWorkspaceMembership(
        WorkspaceId workspaceId,
        UserId actorUserId
    ) {
        return workspaceRepository.findMembership(workspaceId, actorUserId)
            .orElseThrow(() -> new WorkspaceAccessDeniedException("workspace membership is required"));
    }

    private void requireProjectReadAccess(WorkspaceProject project, UserId actorUserId) {
        var workspaceMembership = requireWorkspaceMembership(project.workspaceId(), actorUserId);
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

    private void publishAuditEvent(String action, WorkspaceProject project, UserId actorUserId) {
        auditPort.publish(new AuditEvent(
            project.workspaceId(),
            actorUserId,
            action,
            TARGET_TYPE_PROJECT,
            project.id().value(),
            clock.instant(),
            Map.of("status", project.status().name())
        ));
    }
}
