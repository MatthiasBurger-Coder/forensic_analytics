package de.burger.forensics.analytics.persistence;

import de.burger.forensics.analytics.application.ingestion.port.IngestionSessionRepository;
import de.burger.forensics.analytics.domain.ingestion.IngestionPayload;
import de.burger.forensics.analytics.domain.ingestion.IngestionSession;
import de.burger.forensics.analytics.observability.OperationLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryIngestionSessionRepository implements IngestionSessionRepository {
    private final Map<String, IngestionSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, List<IngestionPayload>> payloads = new ConcurrentHashMap<>();
    private final OperationLogger operationLogger;

    public InMemoryIngestionSessionRepository() {
        this(OperationLogger.system(InMemoryIngestionSessionRepository.class));
    }

    InMemoryIngestionSessionRepository(OperationLogger operationLogger) {
        this.operationLogger = Objects.requireNonNull(operationLogger, "operationLogger must not be null");
    }

    @Override
    public void save(IngestionSession session) {
        var verifiedSession = Objects.requireNonNull(session, "session must not be null");
        operationLogger.logged("persistence.ingestion-session.save", () -> {
            sessions.put(verifiedSession.sessionId(), verifiedSession);
            payloads.putIfAbsent(verifiedSession.sessionId(), new ArrayList<>());
            return null;
        });
    }

    @Override
    public Optional<IngestionSession> findById(String sessionId) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        return Optional.ofNullable(sessions.get(sessionId));
    }

    @Override
    public void update(IngestionSession session) {
        var verifiedSession = Objects.requireNonNull(session, "session must not be null");
        operationLogger.logged(
            "persistence.ingestion-session.update",
            () -> {
                sessions.put(verifiedSession.sessionId(), verifiedSession);
                return null;
            }
        );
    }

    @Override
    public long appendPayload(IngestionPayload payload) {
        var verifiedPayload = Objects.requireNonNull(payload, "payload must not be null");
        return operationLogger.logged("persistence.ingestion-payload.append", () -> appendVerifiedPayload(verifiedPayload));
    }

    private long appendVerifiedPayload(IngestionPayload payload) {
        var sessionPayloads = payloads.computeIfAbsent(payload.sessionId(), ignored -> new ArrayList<>());
        synchronized (sessionPayloads) {
            sessionPayloads.add(payload);
            return sessionPayloads.size();
        }
    }
}
