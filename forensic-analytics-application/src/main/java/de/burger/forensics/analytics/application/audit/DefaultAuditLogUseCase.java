package de.burger.forensics.analytics.application.audit;

import de.burger.forensics.analytics.application.audit.command.ListWorkspaceAuditEventsCommand;
import de.burger.forensics.analytics.application.audit.port.AuditEventRepository;
import de.burger.forensics.analytics.application.workspace.WorkspaceAccessDeniedException;
import de.burger.forensics.analytics.application.workspace.WorkspaceNotFoundException;
import de.burger.forensics.analytics.application.workspace.port.WorkspaceRepository;
import de.burger.forensics.analytics.domain.audit.AuditEvent;
import de.burger.forensics.analytics.domain.workspace.WorkspacePermission;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class DefaultAuditLogUseCase implements AuditLogUseCase {
    private final WorkspaceRepository workspaceRepository;
    private final AuditEventRepository auditEventRepository;

    public DefaultAuditLogUseCase(
        WorkspaceRepository workspaceRepository,
        AuditEventRepository auditEventRepository
    ) {
        this.workspaceRepository = Objects.requireNonNull(workspaceRepository, "workspaceRepository must not be null");
        this.auditEventRepository = Objects.requireNonNull(auditEventRepository, "auditEventRepository must not be null");
    }

    @Override
    public List<AuditEvent> listWorkspaceEvents(ListWorkspaceAuditEventsCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        workspaceRepository.findById(command.workspaceId())
            .orElseThrow(() -> new WorkspaceNotFoundException("workspace not found: " + command.workspaceId().value()));
        var membership = workspaceRepository.findMembership(command.workspaceId(), command.actorUserId())
            .orElseThrow(() -> new WorkspaceAccessDeniedException("workspace membership is required"));
        if (!WorkspacePermission.READ_WORKSPACE_AUDIT.isGrantedTo(membership.role())) {
            throw new WorkspaceAccessDeniedException("workspace role is not allowed to READ_WORKSPACE_AUDIT");
        }
        return auditEventRepository.findByWorkspace(command.workspaceId()).stream()
            .sorted(Comparator
                .comparing(AuditEvent::occurredAt)
                .thenComparing(AuditEvent::action)
                .thenComparing(AuditEvent::targetType)
                .thenComparing(AuditEvent::targetId))
            .toList();
    }
}
