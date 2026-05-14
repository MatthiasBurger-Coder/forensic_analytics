package de.burger.forensics.analytics.persistence;

import de.burger.forensics.analytics.application.retention.port.RetentionPolicyRepository;
import de.burger.forensics.analytics.domain.workspace.RetentionPolicy;
import de.burger.forensics.analytics.domain.workspace.WorkspaceId;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryRetentionPolicyRepository implements RetentionPolicyRepository {
    private final Map<WorkspaceId, RetentionPolicy> policies = new ConcurrentHashMap<>();

    @Override
    public void save(WorkspaceId workspaceId, RetentionPolicy policy) {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        Objects.requireNonNull(policy, "policy must not be null");
        policies.put(workspaceId, policy);
    }

    @Override
    public Optional<RetentionPolicy> findByWorkspace(WorkspaceId workspaceId) {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        return Optional.ofNullable(policies.get(workspaceId));
    }
}
