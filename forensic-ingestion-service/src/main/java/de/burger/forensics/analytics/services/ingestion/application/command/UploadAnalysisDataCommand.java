package de.burger.forensics.analytics.services.ingestion.application.command;

import de.burger.forensics.analytics.services.ingestion.domain.RawIngestionPayload;

import java.util.Objects;

public record UploadAnalysisDataCommand(String sessionId, RawIngestionPayload payload) {
    public UploadAnalysisDataCommand {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId must not be blank");
        }
        sessionId = sessionId.strip();
        Objects.requireNonNull(payload, "payload must not be null");
    }
}
