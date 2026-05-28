package de.burger.forensics.analytics.services.ingestion.adapter.out.memory;

import de.burger.forensics.analytics.services.ingestion.application.port.IngestionSessionRepository;
import de.burger.forensics.analytics.services.ingestion.domain.IngestionSession;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryIngestionSessionRepository implements IngestionSessionRepository {
    private final ConcurrentHashMap<String, IngestionSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void save(IngestionSession session) {
        sessions.put(session.sessionId(), session);
    }

    @Override
    public Optional<IngestionSession> findById(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }
}
