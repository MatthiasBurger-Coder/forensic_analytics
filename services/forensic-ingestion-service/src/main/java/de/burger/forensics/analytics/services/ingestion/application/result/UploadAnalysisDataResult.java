package de.burger.forensics.analytics.services.ingestion.application.result;

import de.burger.forensics.analytics.services.ingestion.domain.IngestionStatus;

public record UploadAnalysisDataResult(
    String sessionId,
    IngestionStatus status,
    long receivedItems,
    boolean acceptedNewPayload,
    String message
) {
}
