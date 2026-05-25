package de.burger.forensics.analytics.services.repositorysource.application.port;

import java.time.Instant;
import java.util.Objects;

import static de.burger.forensics.analytics.services.repositorysource.domain.RepositorySourceDomain.requireText;

public record RepositorySourceIdempotencyRecord(
    String idempotencyKey,
    String operation,
    String fingerprint,
    String resultType,
    String resultReference,
    String resultPayload,
    String status,
    Instant createdAt,
    Instant expiresAt
) {
    public RepositorySourceIdempotencyRecord {
        idempotencyKey = requireText(idempotencyKey, "idempotency key");
        operation = requireText(operation, "idempotency operation");
        fingerprint = requireText(fingerprint, "idempotency fingerprint");
        resultType = requireText(resultType, "idempotency result type");
        resultReference = requireText(resultReference, "idempotency result reference");
        resultPayload = resultPayload == null ? "" : resultPayload;
        status = requireText(status, "idempotency status");
        createdAt = Objects.requireNonNull(createdAt, "created at must not be null");
    }
}
