package de.burger.forensics.analytics.application.ingestion.port;

import de.burger.forensics.analytics.domain.ingestion.IngestionPayload;
import de.burger.forensics.analytics.domain.ingestion.IngestionSession;

import java.util.Optional;

public interface IngestionSessionRepository {
    void save(IngestionSession session);

    Optional<IngestionSession> findById(String sessionId);

    void update(IngestionSession session);

    long appendPayload(IngestionPayload payload);
}
