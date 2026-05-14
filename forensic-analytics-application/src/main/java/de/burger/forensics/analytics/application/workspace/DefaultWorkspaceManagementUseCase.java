package de.burger.forensics.analytics.application.workspace;

import de.burger.forensics.analytics.application.workspace.command.ArchiveWorkspaceCommand;
import de.burger.forensics.analytics.application.workspace.command.CreateWorkspaceCommand;
import de.burger.forensics.analytics.application.workspace.command.GetWorkspaceCommand;
import de.burger.forensics.analytics.application.workspace.command.ListWorkspacesCommand;
import de.burger.forensics.analytics.application.workspace.command.RenameWorkspaceCommand;
import de.burger.forensics.analytics.application.workspace.port.WorkspaceAuditPort;
import de.burger.forensics.analytics.application.workspace.port.WorkspaceRepository;
import de.burger.forensics.analytics.domain.audit.AuditEvent;
import de.burger.forensics.analytics.domain.workspace.Workspace;
import de.burger.forensics.analytics.domain.workspace.WorkspaceMembership;
import de.burger.forensics.analytics.domain.workspace.WorkspacePermission;
import de.burger.forensics.analytics.domain.workspace.WorkspaceRole;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class DefaultWorkspaceManagementUseCase implements WorkspaceManagementUseCase {
    private static final String TARGET_TYPE_WORKSPACE = "workspace";
    private static final String WORKSPACE_CREATED = "workspace.created";
    private static final String WORKSPACE_UPDATED = "workspace.updated";
    private static final String WORKSPACE_ARCHIVED = "workspace.archived";

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceAuditPort auditPort;
    private final Clock clock;

    public DefaultWorkspaceManagementUseCase(
        WorkspaceRepository workspaceRepository,
        WorkspaceAuditPort auditPort,
        Clock clock
    ) {
        this.workspaceRepository = Objects.requireNonNull(workspaceRepository, "workspaceRepository must not be null");
        this.auditPort = Objects.requireNonNull(auditPort, "auditPort must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public Workspace create(CreateWorkspaceCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        var workspace = Workspace.active(command.workspaceId(), command.name());
        workspaceRepository.save(workspace);
        workspaceRepository.saveMembership(new WorkspaceMembership(
            command.workspaceId(),
            command.ownerUserId(),
            WorkspaceRole.OWNER
        ));
        publishAuditEvent(WORKSPACE_CREATED, workspace, command.ownerUserId());
        return workspace;
    }

    @Override
    public Workspace get(GetWorkspaceCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        var workspace = existingWorkspace(command.workspaceId().value());
        requirePermission(workspace, command.actorUserId().value(), WorkspacePermission.READ_WORKSPACE);
        return workspace;
    }

    @Override
    public List<Workspace> list(ListWorkspacesCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        return workspaceRepository.findByMember(command.actorUserId());
    }

    @Override
    public Workspace rename(RenameWorkspaceCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        var workspace = existingWorkspace(command.workspaceId().value());
        requireActive(workspace);
        requirePermission(workspace, command.actorUserId().value(), WorkspacePermission.UPDATE_WORKSPACE);
        var renamed = workspace.rename(command.name());
        workspaceRepository.update(renamed);
        publishAuditEvent(WORKSPACE_UPDATED, renamed, command.actorUserId());
        return renamed;
    }

    @Override
    public Workspace archive(ArchiveWorkspaceCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        var workspace = existingWorkspace(command.workspaceId().value());
        requireActive(workspace);
        requirePermission(workspace, command.actorUserId().value(), WorkspacePermission.ARCHIVE_WORKSPACE);
        var archived = workspace.archive();
        workspaceRepository.update(archived);
        publishAuditEvent(WORKSPACE_ARCHIVED, archived, command.actorUserId());
        return archived;
    }

    private Workspace existingWorkspace(String workspaceId) {
        return workspaceRepository.findById(new de.burger.forensics.analytics.domain.workspace.WorkspaceId(workspaceId))
            .orElseThrow(() -> new WorkspaceNotFoundException("workspace not found: " + workspaceId));
    }

    private void requirePermission(Workspace workspace, String actorUserId, WorkspacePermission permission) {
        var membership = workspaceRepository.findMembership(
            workspace.id(),
            new de.burger.forensics.analytics.domain.workspace.UserId(actorUserId)
        ).orElseThrow(() -> new WorkspaceAccessDeniedException("workspace membership is required"));
        if (!permission.isGrantedTo(membership.role())) {
            throw new WorkspaceAccessDeniedException("workspace role is not allowed to " + permission.name());
        }
    }

    private static void requireActive(Workspace workspace) {
        if (!workspace.acceptsChanges()) {
            throw new WorkspaceArchivedException("archived workspace is read-only: " + workspace.id().value());
        }
    }

    private void publishAuditEvent(String action, Workspace workspace, de.burger.forensics.analytics.domain.workspace.UserId actorUserId) {
        auditPort.publish(new AuditEvent(
            workspace.id(),
            actorUserId,
            action,
            TARGET_TYPE_WORKSPACE,
            workspace.id().value(),
            clock.instant(),
            Map.of("status", workspace.status().name())
        ));
    }
}
