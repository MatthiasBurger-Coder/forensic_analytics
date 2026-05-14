package de.burger.forensics.analytics.domain.workspace;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record WorkspaceLease(
    String owner,
    Instant startedAt,
    Optional<Instant> expiresAt,
    WorkspacePreparationStatus status
) {
    public WorkspaceLease {
        RequiredWorkspaceText.requireText(owner, "lease owner");
        startedAt = Objects.requireNonNull(startedAt, "startedAt must not be null");
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        if (expiresAt.isPresent()) {
            requireExpiryAfterStart(startedAt, expiresAt.orElseThrow());
        }
        status = Objects.requireNonNull(status, "status must not be null");
    }

    private static void requireExpiryAfterStart(Instant startedAt, Instant expiresAt) {
        if (!expiresAt.isAfter(startedAt)) {
            throw new IllegalArgumentException("lease expiry must be after lease start");
        }
    }
}
