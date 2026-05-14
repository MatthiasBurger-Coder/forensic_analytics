package de.burger.forensics.analytics.application.retention;

import de.burger.forensics.analytics.application.retention.command.ConfigureWorkspaceRetentionCommand;
import de.burger.forensics.analytics.application.retention.port.RetentionPolicyRepository;
import de.burger.forensics.analytics.application.workspace.WorkspaceAccessDeniedException;
import de.burger.forensics.analytics.application.workspace.WorkspaceArchivedException;
import de.burger.forensics.analytics.application.workspace.WorkspaceNotFoundException;
import de.burger.forensics.analytics.application.workspace.port.WorkspaceAuditPort;
import de.burger.forensics.analytics.application.workspace.port.WorkspaceRepository;
import de.burger.forensics.analytics.domain.audit.AuditEvent;
import de.burger.forensics.analytics.domain.workspace.RetentionPolicy;
import de.burger.forensics.analytics.domain.workspace.UserId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;
import de.burger.forensics.analytics.domain.workspace.WorkspacePermission;

import java.time.Clock;
import java.util.Map;
import java.util.Objects;

public final class DefaultRetentionPolicyUseCase implements RetentionPolicyUseCase {
    private static final String RETENTION_UPDATED = "workspace.retention.updated";
    private static final String TARGET_TYPE_WORKSPACE_RETENTION = "workspace-retention";

    private final WorkspaceRepository workspaceRepository;
    private final RetentionPolicyRepository retentionPolicyRepository;
    private final WorkspaceAuditPort auditPort;
    private final Clock clock;

    public DefaultRetentionPolicyUseCase(
        WorkspaceRepository workspaceRepository,
        RetentionPolicyRepository retentionPolicyRepository,
        WorkspaceAuditPort auditPort,
        Clock clock
    ) {
        this.workspaceRepository = Objects.requireNonNull(workspaceRepository, "workspaceRepository must not be null");
        this.retentionPolicyRepository = Objects.requireNonNull(retentionPolicyRepository, "retentionPolicyRepository must not be null");
        this.auditPort = Objects.requireNonNull(auditPort, "auditPort must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public RetentionPolicy configure(ConfigureWorkspaceRetentionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        var workspace = workspaceRepository.findById(command.workspaceId())
            .orElseThrow(() -> new WorkspaceNotFoundException("workspace not found: " + command.workspaceId().value()));
        if (!workspace.acceptsChanges()) {
            throw new WorkspaceArchivedException("archived workspace is read-only: " + workspace.id().value());
        }
        requireOwnerOrAdmin(command.workspaceId(), command.actorUserId());
        var policy = new RetentionPolicy(command.retentionDays());
        retentionPolicyRepository.save(command.workspaceId(), policy);
        auditPort.publish(new AuditEvent(
            command.workspaceId(),
            command.actorUserId(),
            RETENTION_UPDATED,
            TARGET_TYPE_WORKSPACE_RETENTION,
            command.workspaceId().value(),
            clock.instant(),
            Map.of("retentionDays", Integer.toString(policy.retentionDays()))
        ));
        return policy;
    }

    private void requireOwnerOrAdmin(WorkspaceId workspaceId, UserId actorUserId) {
        var membership = workspaceRepository.findMembership(workspaceId, actorUserId)
            .orElseThrow(() -> new WorkspaceAccessDeniedException("workspace membership is required"));
        if (!WorkspacePermission.UPDATE_WORKSPACE.isGrantedTo(membership.role())) {
            throw new WorkspaceAccessDeniedException("workspace role is not allowed to configure retention");
        }
    }
}
