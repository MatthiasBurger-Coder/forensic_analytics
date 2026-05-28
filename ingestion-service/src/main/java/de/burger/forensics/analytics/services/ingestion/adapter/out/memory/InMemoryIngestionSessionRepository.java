package de.burger.forensics.analytics.services.ingestion.adapter.out.memory;

import de.burger.forensics.analytics.services.ingestion.application.port.IngestionSessionRepository;
import de.burger.forensics.analytics.services.ingestion.domain.IngestionSession;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

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

    @Override
    public Optional<IngestionSession> update(String sessionId, UnaryOperator<IngestionSession> mutation) {
        var current = sessions.computeIfPresent(sessionId, (ignored, session) -> mutation.apply(session));
        return Optional.ofNullable(current);
    }
}
