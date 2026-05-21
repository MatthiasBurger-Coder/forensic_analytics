package de.burger.forensics.analytics.services.ingestion.application.port;

import de.burger.forensics.analytics.services.ingestion.domain.RawIngestionPayload;

public interface AcceptedIngestionHandoffPort {
    void accepted(String sessionId, RawIngestionPayload payload);
}
