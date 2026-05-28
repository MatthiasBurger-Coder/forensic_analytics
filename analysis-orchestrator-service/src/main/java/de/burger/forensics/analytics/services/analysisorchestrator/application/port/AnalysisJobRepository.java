package de.burger.forensics.analytics.services.analysisorchestrator.application.port;

import de.burger.forensics.analytics.services.analysisorchestrator.domain.AnalysisJob;
import de.burger.forensics.analytics.services.analysisorchestrator.domain.AnalysisJobId;
import de.burger.forensics.analytics.services.analysisorchestrator.domain.AnalysisJobState;
import de.burger.forensics.analytics.services.analysisorchestrator.domain.AnalysisRunId;
import de.burger.forensics.analytics.services.analysisorchestrator.domain.AnalysisWorkerKind;

import java.util.List;
import java.util.Optional;

public interface AnalysisJobRepository {
    void save(AnalysisJob job);

    Optional<AnalysisJob> findById(AnalysisJobId jobId);

    List<AnalysisJob> list(AnalysisRunId runId, AnalysisWorkerKind workerKind, AnalysisJobState state);
}
