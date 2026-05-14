package de.burger.forensics.analytics.application.retention.command;

import de.burger.forensics.analytics.domain.workspace.UserId;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;

import java.util.Objects;

public record ConfigureWorkspaceRetentionCommand(
    WorkspaceId workspaceId,
    int retentionDays,
    UserId actorUserId
) {
    public ConfigureWorkspaceRetentionCommand {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        if (retentionDays < 1) {
            throw new IllegalArgumentException("retentionDays must be positive");
        }
        Objects.requireNonNull(actorUserId, "actorUserId must not be null");
    }
}
