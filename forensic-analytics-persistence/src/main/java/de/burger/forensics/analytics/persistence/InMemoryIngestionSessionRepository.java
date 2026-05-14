package de.burger.forensics.analytics.persistence;

import de.burger.forensics.analytics.application.ingestion.port.IngestionSessionRepository;
import de.burger.forensics.analytics.domain.ingestion.IngestionPayload;
import de.burger.forensics.analytics.domain.ingestion.IngestionSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryIngestionSessionRepository implements IngestionSessionRepository {
    private final Map<String, IngestionSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, List<IngestionPayload>> payloads = new ConcurrentHashMap<>();

    @Override
    public void save(IngestionSession session) {
        sessions.put(session.sessionId(), session);
        payloads.putIfAbsent(session.sessionId(), new ArrayList<>());
    }

    @Override
    public Optional<IngestionSession> findById(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    @Override
    public void update(IngestionSession session) {
        sessions.put(session.sessionId(), session);
    }

    @Override
    public long appendPayload(IngestionPayload payload) {
        var sessionPayloads = payloads.computeIfAbsent(payload.sessionId(), ignored -> new ArrayList<>());
        synchronized (sessionPayloads) {
            sessionPayloads.add(payload);
            return sessionPayloads.size();
        }
    }
}
