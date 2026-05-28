package de.burger.forensics.analytics.services.analysisstore.application.port;

import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisJob;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisJobId;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisJobState;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisRunId;
import de.burger.forensics.analytics.services.analysisstore.domain.AnalysisWorkerKind;

import java.util.List;
import java.util.Optional;

public interface AnalysisJobRepository {
    void save(AnalysisJob job);

    Optional<AnalysisJob> findById(AnalysisJobId jobId);

    List<AnalysisJob> list(AnalysisRunId runId, AnalysisWorkerKind workerKind, AnalysisJobState state);
}
