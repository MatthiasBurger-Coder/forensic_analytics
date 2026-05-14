package de.burger.forensics.analytics.application.ingestion.result;

import java.util.Objects;

public record AbortAnalysisSessionResult(String sessionId, IngestionStatus status, String message) {
    public AbortAnalysisSessionResult {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(message, "message must not be null");
    }
}
