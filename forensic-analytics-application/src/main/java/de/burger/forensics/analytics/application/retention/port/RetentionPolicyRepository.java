package de.burger.forensics.analytics.application.retention.port;

import de.burger.forensics.analytics.domain.workspace.RetentionPolicy;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;

import java.util.Optional;

public interface RetentionPolicyRepository {
    void save(WorkspaceId workspaceId, RetentionPolicy policy);

    Optional<RetentionPolicy> findByWorkspace(WorkspaceId workspaceId);
}
