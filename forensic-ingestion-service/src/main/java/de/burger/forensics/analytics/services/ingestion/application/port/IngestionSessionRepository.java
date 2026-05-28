package de.burger.forensics.analytics.services.ingestion.application.port;

import de.burger.forensics.analytics.services.ingestion.domain.IngestionSession;

import java.util.Optional;

public interface IngestionSessionRepository {
    void save(IngestionSession session);

    Optional<IngestionSession> findById(String sessionId);
}
