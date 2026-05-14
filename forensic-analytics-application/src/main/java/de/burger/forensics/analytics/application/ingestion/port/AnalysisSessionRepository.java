package de.burger.forensics.analytics.application.ingestion.port;

import de.burger.forensics.analytics.domain.analysis.AnalysisRunId;
import de.burger.forensics.analytics.domain.analysis.AnalysisSession;

import java.util.Optional;

public interface AnalysisSessionRepository {
    void save(AnalysisSession session);

    Optional<AnalysisSession> findById(AnalysisRunId sessionId);
}
