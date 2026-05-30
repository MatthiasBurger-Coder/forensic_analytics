package de.burger.forensics.analytics.services.ingestion.adapter.in.file;

import de.burger.forensics.analytics.services.ingestion.domain.IngestionStatus;

import java.util.Objects;

public record EngineIngestionImportResult(
    String sessionId,
    IngestionStatus completionStatus,
    int uploadedPayloads
) {
    public EngineIngestionImportResult {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(completionStatus, "completionStatus must not be null");
        if (uploadedPayloads < 0) {
            throw new IllegalArgumentException("uploadedPayloads must not be negative");
        }
    }
}
