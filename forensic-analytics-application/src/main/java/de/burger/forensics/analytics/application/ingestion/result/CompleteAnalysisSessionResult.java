package de.burger.forensics.analytics.application.ingestion.result;

import java.util.Objects;

public record CompleteAnalysisSessionResult(String sessionId, IngestionStatus status, String message) {
    public CompleteAnalysisSessionResult {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(message, "message must not be null");
    }
}
