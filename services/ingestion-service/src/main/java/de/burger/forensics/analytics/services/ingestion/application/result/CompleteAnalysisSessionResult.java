package de.burger.forensics.analytics.services.ingestion.application.result;

import de.burger.forensics.analytics.services.ingestion.domain.IngestionStatus;

public record CompleteAnalysisSessionResult(String sessionId, IngestionStatus status, String message) {
}
