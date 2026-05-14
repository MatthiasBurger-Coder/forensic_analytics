package de.burger.forensics.analytics.domain.workspace;

import java.time.Duration;
import java.util.Objects;

public record WorkspacePolicy(
    boolean ephemeral,
    boolean allowShallowClone,
    boolean allowPartialClone,
    boolean allowSparseCheckout,
    Duration timeout,
    long maxWorkspaceBytes,
    WorkspaceCleanupPolicy cleanupPolicy
) {
    public WorkspacePolicy {
        timeout = Objects.requireNonNull(timeout, "timeout must not be null");
        if (timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must not be negative");
        }
        if (maxWorkspaceBytes < 0) {
            throw new IllegalArgumentException("maxWorkspaceBytes must not be negative");
        }
        cleanupPolicy = Objects.requireNonNull(cleanupPolicy, "cleanupPolicy must not be null");
    }
}
