package de.burger.forensics.analytics.persistence;

import de.burger.forensics.analytics.application.ingestion.port.AnalysisSessionRepository;
import de.burger.forensics.analytics.domain.analysis.AnalysisRunId;
import de.burger.forensics.analytics.domain.analysis.AnalysisSession;
import de.burger.forensics.analytics.observability.OperationLogger;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryAnalysisSessionRepository implements AnalysisSessionRepository {
    private final Map<AnalysisRunId, AnalysisSession> sessions = new ConcurrentHashMap<>();
    private final OperationLogger operationLogger;

    public InMemoryAnalysisSessionRepository() {
        this(OperationLogger.system(InMemoryAnalysisSessionRepository.class));
    }

    InMemoryAnalysisSessionRepository(OperationLogger operationLogger) {
        this.operationLogger = Objects.requireNonNull(operationLogger, "operationLogger must not be null");
    }

    @Override
    public void save(AnalysisSession session) {
        var verifiedSession = Objects.requireNonNull(session, "session must not be null");
        operationLogger.logged("persistence.analysis-session.save", () -> {
            sessions.put(verifiedSession.id(), verifiedSession);
            return null;
        });
    }

    @Override
    public Optional<AnalysisSession> findById(AnalysisRunId sessionId) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        return Optional.ofNullable(sessions.get(sessionId));
    }

    @Override
    public List<AnalysisSession> findAll() {
        return sessions.values().stream()
            .sorted(Comparator.comparing(session -> session.id().value()))
            .toList();
    }
}
