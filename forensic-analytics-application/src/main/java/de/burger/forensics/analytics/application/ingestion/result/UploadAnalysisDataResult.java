package de.burger.forensics.analytics.application.ingestion.result;

import java.util.Objects;

public record UploadAnalysisDataResult(
    String sessionId,
    IngestionStatus status,
    long receivedItems,
    String message
) {
    public UploadAnalysisDataResult {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(message, "message must not be null");
        if (receivedItems < 0) {
            throw new IllegalArgumentException("receivedItems must not be negative");
        }
    }
}
