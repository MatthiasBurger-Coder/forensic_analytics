package de.burger.forensics.analytics.application.workspace;

import de.burger.forensics.analytics.application.workspace.command.AddWorkspaceMemberCommand;
import de.burger.forensics.analytics.application.workspace.command.ChangeWorkspaceMemberRoleCommand;
import de.burger.forensics.analytics.application.workspace.command.ListWorkspaceMembersCommand;
import de.burger.forensics.analytics.application.workspace.command.RemoveWorkspaceMemberCommand;
import de.burger.forensics.analytics.application.workspace.port.WorkspaceAuditPort;
import de.burger.forensics.analytics.application.workspace.port.WorkspaceRepository;
import de.burger.forensics.analytics.domain.audit.AuditEvent;
import de.burger.forensics.analytics.domain.workspace.UserId;
import de.burger.forensics.analytics.domain.workspace.Workspace;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceMembership;
import de.burger.forensics.analytics.domain.workspace.WorkspacePermission;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class DefaultWorkspaceMemberManagementUseCase implements WorkspaceMemberManagementUseCase {
    private static final String TARGET_TYPE_WORKSPACE_MEMBER = "workspace-member";
    private static final String MEMBER_ADDED = "workspace.member.added";
    private static final String MEMBER_ROLE_CHANGED = "workspace.member.role_changed";
    private static final String MEMBER_REMOVED = "workspace.member.removed";

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceAuditPort auditPort;
    private final Clock clock;

    public DefaultWorkspaceMemberManagementUseCase(
        WorkspaceRepository workspaceRepository,
        WorkspaceAuditPort auditPort,
        Clock clock
    ) {
        this.workspaceRepository = Objects.requireNonNull(workspaceRepository, "workspaceRepository must not be null");
        this.auditPort = Objects.requireNonNull(auditPort, "auditPort must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public WorkspaceMembership add(AddWorkspaceMemberCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        var workspace = activeWorkspace(command.workspaceId());
        requireManageMembers(workspace.id(), command.actorUserId());
        workspaceRepository.findMembership(command.workspaceId(), command.memberUserId()).ifPresent(existing -> {
            throw new WorkspaceMembershipAlreadyExistsException(
                "workspace membership already exists for user: " + existing.userId().value()
            );
        });
        var membership = new WorkspaceMembership(command.workspaceId(), command.memberUserId(), command.role());
        workspaceRepository.saveMembership(membership);
        publishAuditEvent(MEMBER_ADDED, membership, command.actorUserId(), Map.of("role", command.role().name()));
        return membership;
    }

    @Override
    public List<WorkspaceMembership> list(ListWorkspaceMembersCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        var workspace = existingWorkspace(command.workspaceId());
        requirePermission(workspace.id(), command.actorUserId(), WorkspacePermission.READ_WORKSPACE);
        return workspaceRepository.findMemberships(command.workspaceId()).stream()
            .sorted(Comparator.comparing(membership -> membership.userId().value()))
            .toList();
    }

    @Override
    public WorkspaceMembership changeRole(ChangeWorkspaceMemberRoleCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        var workspace = activeWorkspace(command.workspaceId());
        requireManageMembers(workspace.id(), command.actorUserId());
        var existing = existingMembership(command.workspaceId(), command.memberUserId());
        var updated = new WorkspaceMembership(command.workspaceId(), command.memberUserId(), command.role());
        workspaceRepository.saveMembership(updated);
        publishAuditEvent(MEMBER_ROLE_CHANGED, updated, command.actorUserId(), Map.of(
            "previousRole", existing.role().name(),
            "role", command.role().name()
        ));
        return updated;
    }

    @Override
    public void remove(RemoveWorkspaceMemberCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        var workspace = activeWorkspace(command.workspaceId());
        requireManageMembers(workspace.id(), command.actorUserId());
        var existing = existingMembership(command.workspaceId(), command.memberUserId());
        workspaceRepository.removeMembership(command.workspaceId(), command.memberUserId());
        publishAuditEvent(MEMBER_REMOVED, existing, command.actorUserId(), Map.of("role", existing.role().name()));
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

    private WorkspaceMembership existingMembership(WorkspaceId workspaceId, UserId userId) {
        return workspaceRepository.findMembership(workspaceId, userId)
            .orElseThrow(() -> new WorkspaceMemberNotFoundException("workspace member not found: " + userId.value()));
    }

    private void requireManageMembers(WorkspaceId workspaceId, UserId actorUserId) {
        requirePermission(workspaceId, actorUserId, WorkspacePermission.MANAGE_WORKSPACE_MEMBERS);
    }

    private void requirePermission(WorkspaceId workspaceId, UserId actorUserId, WorkspacePermission permission) {
        var actorMembership = workspaceRepository.findMembership(workspaceId, actorUserId)
            .orElseThrow(() -> new WorkspaceAccessDeniedException("workspace membership is required"));
        if (!permission.isGrantedTo(actorMembership.role())) {
            throw new WorkspaceAccessDeniedException("workspace role is not allowed to " + permission.name());
        }
    }

    private void publishAuditEvent(
        String action,
        WorkspaceMembership membership,
        UserId actorUserId,
        Map<String, String> metadata
    ) {
        auditPort.publish(new AuditEvent(
            membership.workspaceId(),
            actorUserId,
            action,
            TARGET_TYPE_WORKSPACE_MEMBER,
            membership.userId().value(),
            clock.instant(),
            metadata
        ));
    }
}
